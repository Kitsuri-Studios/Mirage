package io.kitsuri.m1rage.utils;

import android.util.Log;

import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.File;

import io.kitsuri.m1rage.model.PatcherViewModel;

public class DexToSmali {

    private static final String TAG = "DexToSmali";
    private static PatcherViewModel viewModel;

    private final boolean mDebugInfo;
    private final File mInputFile, mOutDir;
    private final int mAPI;
    private final String mDEXName;

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

    public DexToSmali(boolean debugInfo, File inputFile, File outDir, int api, String dexName) {
        this.mDebugInfo = debugInfo;
        this.mInputFile = inputFile;
        this.mOutDir = outDir;
        this.mAPI = api;
        this.mDEXName = dexName;
    }

    public void execute() {
        try {
            addLog(Log.INFO, "Disassembling DEX file: " + mInputFile.getName());
            addLog(Log.DEBUG, "API Level: " + mAPI);

            final BaksmaliOptions options = new BaksmaliOptions();
            options.deodex = false;
            options.implicitReferences = false;
            options.parameterRegisters = true;
            options.localsDirective = true;
            options.sequentialLabels = true;
            options.debugInfo = mDebugInfo;
            options.codeOffsets = false;
            options.accessorComments = false;
            options.registerInfo = 0;
            options.inlineResolver = null;

            int jobs = Runtime.getRuntime().availableProcessors();
            if (jobs > 6) {
                jobs = 6;
            }
            addLog(Log.DEBUG, "Using " + jobs + " threads");

            MultiDexContainer<? extends DexBackedDexFile> container =
                    DexFileFactory.loadDexContainer(mInputFile, Opcodes.forApi(mAPI));

            MultiDexContainer.DexEntry<? extends DexBackedDexFile> dexEntry;
            DexBackedDexFile dexFile;

            if (container.getDexEntryNames().size() == 1) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            } else {
                dexEntry = container.getEntry(mDEXName);
            }

            if (dexEntry == null) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            }

            assert dexEntry != null;
            dexFile = dexEntry.getDexFile();

            if (dexFile.supportsOptimizedOpcodes()) {
                addLog(Log.WARN, "Odex file detected - deodexing may be required");
            }

            if (dexFile instanceof DexBackedOdexFile) {
                options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(
                        ((DexBackedOdexFile)dexFile).getOdexVersion()
                );
            }

            addLog(Log.INFO, "Disassembling classes...");
            Baksmali.disassembleDexFile(dexFile, mOutDir, jobs, options);
            addLog(Log.INFO, "Disassembly complete");

        } catch (Exception e) {
            addLog(Log.ERROR, "Disassembly failed: " + e.getMessage());
        }
    }
}