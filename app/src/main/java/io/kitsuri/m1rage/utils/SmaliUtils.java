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

        DexBuilder dexBuilder = apiLevel > 0
                ? new DexBuilder(Opcodes.forApi(apiLevel))
                : new DexBuilder(Opcodes.getDefault());

        List<File> smaliFiles = collectSmaliFiles(smaliRootDir);
        addLog(Log.DEBUG, "Found " + smaliFiles.size() + " smali files");

        int compiled = 0;
        for (File smaliFile : smaliFiles) {
            if (!compileSmaliFile(smaliFile, dexBuilder, apiLevel)) {
                addLog(Log.ERROR, "Failed to compile: " + smaliFile.getName());
                return false;
            }
            compiled++;
            if (compiled % 100 == 0) {
                addLog(Log.DEBUG, "Compiled " + compiled + "/" + smaliFiles.size() + " files");
            }
        }

        try {
            addLog(Log.INFO, "Writing DEX file...");
            dexBuilder.writeTo(new FileDataStore(outputDex));
            addLog(Log.INFO, "DEX written to: " + outputDex.getName());
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
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            smaliFlexLexer lexer = new smaliFlexLexer(reader, apiLevel);
            lexer.setSourceFile(file);

            smaliParser parser = new smaliParser(new CommonTokenStream(lexer));
            parser.setApiLevel(apiLevel);
            parser.setAllowOdex(false);
            parser.setVerboseErrors(false);

            smaliParser.smali_file_return result = parser.smali_file();

            if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                addLog(Log.ERROR, "Syntax errors in: " + file.getName());
                return false;
            }

            CommonTree tree = (CommonTree) result.getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
            nodes.setTokenStream(parser.getTokenStream());

            smaliTreeWalker walker = new smaliTreeWalker(nodes);
            walker.setApiLevel(apiLevel);
            walker.setDexBuilder(builder);
            walker.smali_file();

            return walker.getNumberOfSyntaxErrors() == 0;

        } catch (Exception e) {
            addLog(Log.ERROR, "Error compiling: " + file.getName() + " - " + e.getMessage());
            return false;
        }
    }
}