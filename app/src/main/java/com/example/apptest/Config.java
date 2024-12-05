package com.example.apptest;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;

public class Config {
    private static final String PREFS_NAME = "ConfigPrefs";
    private static final String KEY_FOLDER_PATH = "folderPath";
    private static final String KEY_MAX_WRITE_SIZE = "maxWriteSize";
    private static final String KEY_TEST_MODE = "testMode";
    private static final String KEY_LOOPS_OR_TIME = "loopsOrTime";

    public String folderPath;
    public int maxWriteSize;
    public String testMode;       // Default test mode
    public int loopsOrTime;             // Default loops or time

    // Save configuration to SharedPreferences
    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_FOLDER_PATH, folderPath);
        editor.putInt(KEY_MAX_WRITE_SIZE, maxWriteSize);
        editor.putString(KEY_TEST_MODE, testMode);
        editor.putInt(KEY_LOOPS_OR_TIME, loopsOrTime);
        editor.apply();
    }

    // Load configuration from SharedPreferences
    public void load(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        folderPath = prefs.getString(KEY_FOLDER_PATH, null);

        maxWriteSize = prefs.getInt(KEY_MAX_WRITE_SIZE, 32);
        testMode = prefs.getString(KEY_TEST_MODE, "Sweep");
        loopsOrTime = prefs.getInt(KEY_LOOPS_OR_TIME, 1000);
    }

    public boolean isFolderPathEmpty() {
        return folderPath == null || folderPath.isEmpty();
    }
}
