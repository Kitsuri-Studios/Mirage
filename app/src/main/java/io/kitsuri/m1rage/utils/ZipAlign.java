package io.kitsuri.m1rage.utils;

import android.util.Log;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import io.kitsuri.m1rage.model.PatcherViewModel;

public class ZipAlign {

    private static final String TAG = "ZipAlign";
    private static final int maxEOCDLookup = 0xffff + 22;
    private static PatcherViewModel viewModel;

    public static void setViewModel(PatcherViewModel vm) {
        viewModel = vm;
    }

    private static void addLog(int level, String message) {
        if (viewModel != null) {
            viewModel.addLog(level, message);
        } else {
            Log.println(level, TAG, message);
        }
    }

    public static void alignZip(RandomAccessFile file, OutputStream out) throws IOException {
        alignZip(file, out, 4, 16384);
    }

    public static void alignZip(RandomAccessFile file, OutputStream out, int alignment, int soFileAlignment)
            throws IOException {

        addLog(Log.INFO, "Starting zip alignment");
        addLog(Log.DEBUG, "Alignment: " + alignment + ", SO alignment: " + soFileAlignment);

        long seekStart;
        int readAmount;
        final long fileLength = file.length();

        if (fileLength > maxEOCDLookup) {
            seekStart = fileLength - maxEOCDLookup;
            readAmount = maxEOCDLookup;
        } else {
            seekStart = 0;
            readAmount = (int) fileLength;
        }

        file.seek(seekStart);

        int i;
        for (i = readAmount - 4; i >= 0; i--) {
            if (file.readByte() != 0x50) continue;
            file.seek(file.getFilePointer() - 1);
            if (file.readInt() == 0x504b0506) break;
        }

        if (i < 0) {
            addLog(Log.ERROR, "No end-of-central-directory found");
            throw new IOException("No end-of-central-directory found");
        }

        long eocdPosition = file.getFilePointer() - 4;
        addLog(Log.DEBUG, "Found EOCD at position: " + eocdPosition);

        file.seek(eocdPosition + 10);

        byte[] buf = new byte[10];
        file.read(buf);
        ByteBuffer eocdBuffer = ByteBuffer.wrap(buf)
                .order(ByteOrder.LITTLE_ENDIAN);

        short totalEntries = eocdBuffer.getShort();
        int centralDirOffset = eocdBuffer.getInt();

        addLog(Log.DEBUG, "Total entries: " + totalEntries);

        ArrayList<Alignment> neededAlignments = new ArrayList<>();
        ArrayList<FileOffsetShift> shifts = new ArrayList<>();

        int shiftAmount = 0;

        file.seek(centralDirOffset);
        byte[] entry = new byte[46];
        ByteBuffer entryBuffer = ByteBuffer.wrap(entry)
                .order(ByteOrder.LITTLE_ENDIAN);

        int alignedFiles = 0;
        for (int ei = 0; ei < totalEntries; ei++) {
            final long entryStart = file.getFilePointer();
            file.read(entry);

            if (entryBuffer.getInt(0) != 0x02014b50)
                throw new IOException(
                        "assumed central directory entry at " + entryStart + " doesn't start with a signature"
                );

            short entry_fileNameLen = entryBuffer.getShort(28);
            short entry_extraFieldLen = entryBuffer.getShort(30);
            short entry_commentLen = entryBuffer.getShort(32);
            int fileOffset = entryBuffer.getInt(42);

            if (shiftAmount != 0)
                shifts.add(new FileOffsetShift(entryStart + 42, fileOffset + shiftAmount));

            boolean soAligned = false;

            if (soFileAlignment != 0) {
                byte[] filenameBuffer = new byte[entry_fileNameLen];
                file.read(filenameBuffer);

                String filename = new String(filenameBuffer, StandardCharsets.UTF_8);
                if (filename.endsWith(".so")) {
                    file.seek(fileOffset + 26);

                    ByteBuffer lengths = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                    file.read(lengths.array());
                    short fileNameLen = lengths.getShort();
                    short extraFieldLen = lengths.getShort();

                    long dataPos = fileOffset + 30 + fileNameLen + extraFieldLen + shiftAmount;
                    int wrongOffset = (int) (dataPos % soFileAlignment);
                    int alignAmount = wrongOffset == 0 ? 0 : (soFileAlignment - wrongOffset);
                    shiftAmount += alignAmount;

                    if (alignAmount != 0) {
                        neededAlignments.add(new Alignment(
                                alignAmount,
                                fileOffset + 28,
                                (short) (extraFieldLen + alignAmount),
                                fileNameLen + extraFieldLen
                        ));
                        alignedFiles++;
                    }

                    soAligned = true;
                }
            }

            if (entryBuffer.getShort(10) == 0 && !soAligned) {
                file.seek(fileOffset + 26);

                ByteBuffer lengths = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                file.read(lengths.array());
                short fileNameLen = lengths.getShort();
                short extraFieldLen = lengths.getShort();

                long dataPos = fileOffset + 30 + fileNameLen + extraFieldLen + shiftAmount;
                int wrongOffset = (int) (dataPos % alignment);
                int alignAmount = wrongOffset == 0 ? 0 : (alignment - wrongOffset);
                shiftAmount += alignAmount;

                if (alignAmount != 0) {
                    neededAlignments.add(new Alignment(
                            alignAmount,
                            fileOffset + 28,
                            (short) (extraFieldLen + alignAmount),
                            fileNameLen + extraFieldLen
                    ));
                    alignedFiles++;
                }
            }

            file.seek(entryStart + 46 + entry_fileNameLen + entry_extraFieldLen + entry_commentLen);
        }

        if (alignedFiles > 0) {
            addLog(Log.INFO, "Aligning " + alignedFiles + " files");
        }

        file.seek(0);
        if (neededAlignments.isEmpty()) {
            addLog(Log.INFO, "No alignment needed, copying as-is");
            byte[] buffer = new byte[8192];
            int len;
            while (-1 != (len = file.read(buffer))) {
                out.write(buffer, 0, len);
            }
            return;
        }

        for (Alignment al : neededAlignments) {
            if (al.extraFieldLenOffset != 0) {
                passBytes(file, out, al.extraFieldLenOffset - file.getFilePointer());
            }

            out.write(al.extraFieldLenValue & 0xFF);
            out.write((al.extraFieldLenValue >>> 8) & 0xFF);
            file.readShort();

            passBytes(file, out, al.extraFieldExtensionOffset);

            byte[] padding = new byte[al.alignAmount];
            out.write(padding);
            out.flush();
        }

        for (FileOffsetShift shift : shifts) {
            passBytes(file, out, shift.eocdhPosition - file.getFilePointer());

            out.write(shift.shiftedFileOffset & 0xFF);
            out.write((shift.shiftedFileOffset >>> 8) & 0xFF);
            out.write((shift.shiftedFileOffset >>> 16) & 0xFF);
            out.write((shift.shiftedFileOffset >>> 24) & 0xFF);
            file.readInt();
        }

        passBytes(file, out, eocdPosition + 0x10 - file.getFilePointer());
        int shiftedCDOffset = centralDirOffset + shiftAmount;

        out.write(shiftedCDOffset & 0xFF);
        out.write((shiftedCDOffset >>> 8) & 0xFF);
        out.write((shiftedCDOffset >>> 16) & 0xFF);
        out.write((shiftedCDOffset >>> 24) & 0xFF);
        file.readInt();

        passBytes(file, out, file.length() - file.getFilePointer());

        addLog(Log.INFO, "Zip alignment complete");
    }

    private static class Alignment {
        public int alignAmount;
        public long extraFieldLenOffset;
        public short extraFieldLenValue;
        public int extraFieldExtensionOffset;

        public Alignment(int alignAmount, long extraFieldLenOffset, short extraFieldLenValue,
                         int extraFieldExtensionOffset) {
            this.alignAmount = alignAmount;
            this.extraFieldLenOffset = extraFieldLenOffset;
            this.extraFieldLenValue = extraFieldLenValue;
            this.extraFieldExtensionOffset = extraFieldExtensionOffset;
        }
    }

    private static class FileOffsetShift {
        public long eocdhPosition;
        public int shiftedFileOffset;

        public FileOffsetShift(long eocdhPosition, int shiftedFileOffset) {
            this.eocdhPosition = eocdhPosition;
            this.shiftedFileOffset = shiftedFileOffset;
        }
    }

    private static void passBytes(RandomAccessFile raf, OutputStream out, long len) throws IOException {
        byte[] buffer = new byte[8162];

        long left;
        for (left = len; left > 8162; left -= 8162) {
            raf.read(buffer);
            out.write(buffer);
        }

        if (left != 0) {
            buffer = new byte[(int) left];
            raf.read(buffer);
            out.write(buffer);
        }

        out.flush();
    }
}