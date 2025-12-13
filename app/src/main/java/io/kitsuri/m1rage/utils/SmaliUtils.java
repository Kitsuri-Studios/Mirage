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

public class SmaliUtils {

    private static final String TAG = "SmaliUtils";

    public static boolean smaliToDex(File smaliRootDir, File outputDex, int apiLevel) {
        if (!smaliRootDir.isDirectory()) {
            Log.e(TAG, "Smali directory not found: " + smaliRootDir);
            return false;
        }

        DexBuilder dexBuilder = apiLevel > 0
                ? new DexBuilder(Opcodes.forApi(apiLevel))
                : new DexBuilder(Opcodes.getDefault());

        List<File> smaliFiles = collectSmaliFiles(smaliRootDir);

        for (File smaliFile : smaliFiles) {
            if (!compileSmaliFile(smaliFile, dexBuilder, apiLevel)) {
                Log.e(TAG, "Failed to compile: " + smaliFile.getName());
                return false;
            }
        }

        try {
            dexBuilder.writeTo(new FileDataStore(outputDex));
            Log.i(TAG, "DEX written to: " + outputDex);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write DEX file", e);
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
            Log.e(TAG, "Error compiling smali: " + file.getName(), e);
            return false;
        }
    }
}
