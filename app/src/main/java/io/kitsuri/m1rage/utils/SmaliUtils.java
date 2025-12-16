package io.kitsuri.m1rage.utils;

import android.util.Log;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.kitsuri.m1rage.model.PatcherViewModel;

public class SmaliUtils {

    private static final String TAG = "SmaliUtils";
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

    public static boolean smaliToDex(File smaliRootDir, File outputDex, int apiLevel) {
        if (!smaliRootDir.isDirectory()) {
            addLog(Log.ERROR, "Smali directory not found: " + smaliRootDir);
            return false;
        }

        addLog(Log.INFO, "Compiling smali files to DEX...");
        addLog(Log.DEBUG, "API Level: " + apiLevel);
        logMemoryUsage("Before compilation");

        DexBuilder dexBuilder = apiLevel > 0
                ? new DexBuilder(Opcodes.forApi(apiLevel))
                : new DexBuilder(Opcodes.getDefault());

        List<File> smaliFiles = collectSmaliFiles(smaliRootDir);
        addLog(Log.INFO, "Found " + smaliFiles.size() + " smali files");

        int compiled = 0;
        for (File smaliFile : smaliFiles) {
            if (!compileSmaliFile(smaliFile, dexBuilder, apiLevel)) {
                addLog(Log.ERROR, "Failed to compile: " + smaliFile.getName());
                return false;
            }
            compiled++;

            if (compiled % 100 == 0) {
                System.gc();
            }

        }
        smaliFiles.clear();
        System.gc();

        try {
            addLog(Log.INFO, "Writing DEX file...");
            logMemoryUsage("Before writing DEX");
            dexBuilder.writeTo(new FileDataStore(outputDex));
            addLog(Log.INFO, "DEX written to: " + outputDex.getName());
            logMemoryUsage("After writing DEX");
            return true;
        } catch (Exception e) {
            addLog(Log.ERROR, "Failed to write DEX file: " + e.getMessage());
            return false;
        }
    }

    private static List<File> collectSmaliFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectSmaliFilesRecursive(dir, files);
        return files;
    }

    private static void collectSmaliFilesRecursive(File dir, List<File> list) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                collectSmaliFilesRecursive(f, list);
            } else if (f.getName().endsWith(".smali")) {
                list.add(f);
            }
        }
    }

    private static boolean compileSmaliFile(File file, DexBuilder builder, int apiLevel) {
        FileInputStream fis = null;
        InputStreamReader reader = null;
        smaliFlexLexer lexer = null;
        smaliParser parser = null;
        CommonTree tree = null;
        CommonTreeNodeStream nodes = null;
        smaliTreeWalker walker = null;

        try {
            fis = new FileInputStream(file);
            reader = new InputStreamReader(fis, StandardCharsets.UTF_8);

            lexer = new smaliFlexLexer(reader, apiLevel);
            lexer.setSourceFile(file);

            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            parser = new smaliParser(tokenStream);
            parser.setApiLevel(apiLevel);
            parser.setAllowOdex(false);
            parser.setVerboseErrors(false);

            smaliParser.smali_file_return result = parser.smali_file();

            if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                addLog(Log.ERROR, "Syntax errors in: " + file.getName());
                return false;
            }

            tree = (CommonTree) result.getTree();
            nodes = new CommonTreeNodeStream(tree);
            nodes.setTokenStream(tokenStream);

            walker = new smaliTreeWalker(nodes);
            walker.setApiLevel(apiLevel);
            walker.setDexBuilder(builder);
            walker.smali_file();

            boolean success = walker.getNumberOfSyntaxErrors() == 0;

            // Explicitly null out large objects to help GC
            walker = null;
            nodes = null;
            tree = null;
            parser = null;
            lexer = null;

            return success;

        } catch (OutOfMemoryError oom) {
            walker = null;
            nodes = null;
            tree = null;
            parser = null;
            lexer = null;
            System.gc();

            return false;
        } catch (Exception e) {
            addLog(Log.ERROR, "Error compiling: " + file.getName() + " - " + e.getMessage());
            return false;
        } finally {
            // Ensure streams are closed
            try {
                if (reader != null) reader.close();
                if (fis != null) fis.close();
            } catch (Exception ignored) {}

            // Final cleanup
            walker = null;
            nodes = null;
            tree = null;
            parser = null;
            lexer = null;
        }
    }

    private static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        addLog(Log.DEBUG, String.format(
                "[%s] Memory: %dMB used / %dMB max",
                context, usedMemory, maxMemory
        ));
    }
}