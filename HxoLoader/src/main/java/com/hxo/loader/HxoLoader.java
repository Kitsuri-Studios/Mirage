package com.hxo.loader;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.content.Context;

public final class HxoLoader extends ContentProvider {

    static {
        try {
            System.loadLibrary("hxo");
        } catch (Throwable t) {
            //incase it fails  to load
            // bahiya me kasi ke saiya
            // marela kacha kach kach kach
            // those  who know :trollface:
            // you are cooked lol fix it yourself
        }
    }

    @Override
    public boolean onCreate() {
        Context app = getContext();
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues v) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] a) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) {
        return 0;
    }
}
