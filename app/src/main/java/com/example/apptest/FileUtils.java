package com.example.apptest;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    /**
     * Writes a string to a file in the folder specified in the config.
     *
     * @param context  The context of the calling activity.
     * @param config   The configuration object containing the folder path.
     * @param fileName The name of the file to create.
     * @param data     The string data to write to the file.
     * @return true if the file was written successfully, false otherwise.
     */
    public static boolean writeFileToConfigFolder(Context context, Config config, String fileName, String data) {
        try {
            // Parse the folder URI from the config
            Uri folderUri = Uri.parse(config.folderPath);

            // Create a DocumentFile instance for the folder
            DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
            if (folder == null || !folder.isDirectory()) {
                return false; // Invalid folder
            }

            // Check if the file already exists
            DocumentFile file = folder.findFile(fileName);
            if (file == null) {
                // Create a new file
                file = folder.createFile("text/plain", fileName);
            }

            if (file == null) {
                return false; // Failed to create or locate the file
            }

            // Write the data to the file
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
                if (outputStream == null) {
                    return false; // Failed to open stream
                }

                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            return true; // File written successfully
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Failed to write file
        }
    }
}


