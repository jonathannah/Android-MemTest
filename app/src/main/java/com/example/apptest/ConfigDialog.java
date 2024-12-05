package com.example.apptest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;

public class ConfigDialog {

    private final Context context;
    private final Config config;
    private final ActivityResultLauncher<Intent> folderPickerLauncher;
    private final ConfigDialogListener listener;
    private View dialogView; // Store the dialog view for dynamic updates

    public ConfigDialog(Context context, Config config, ActivityResultLauncher<Intent> folderPickerLauncher, ConfigDialogListener listener) {
        this.context = context;
        this.config = config;
        this.folderPickerLauncher = folderPickerLauncher;
        this.listener = listener;
    }

    public interface ConfigDialogListener {
        void onConfigSaved();
        void onConfigCanceled();
    }

    public void show() {
        // Inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.app_config, null);

        // Initialize UI components
        Button btnSelectFolder = dialogView.findViewById(R.id.btn_select_folder);
        TextView tvSelectedFolder = dialogView.findViewById(R.id.tv_selected_folder);

        if (config.isFolderPathEmpty()) {
            launchFolderPicker();
        } else {
            tvSelectedFolder.setText("Selected Folder: " + config.folderPath);
        }

        Spinner spinnerWriteSize = dialogView.findViewById(R.id.spinner_write_size);
        Spinner spinnerTestMode = dialogView.findViewById(R.id.spinner_test_mode);
        TextView tvInputLabel = dialogView.findViewById(R.id.tv_input_label);
        EditText etInputValue = dialogView.findViewById(R.id.et_input_value);

        // Prepopulate folder path
        tvSelectedFolder.setText("Selected Folder: " + config.folderPath);

        // Populate Max Write Size Dropdown
        String[] writeSizes = {"1", "2", "4", "8", "16", "32"};
        ArrayAdapter<String> writeSizeAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, writeSizes);
        spinnerWriteSize.setAdapter(writeSizeAdapter);
        spinnerWriteSize.setSelection(java.util.Arrays.asList(writeSizes).indexOf(Integer.toString(config.maxWriteSize)));

        // Populate Test Mode Dropdown

        ArrayAdapter<String> testModeAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, Config.TestMode.testModes);
        spinnerTestMode.setAdapter(testModeAdapter);
        spinnerTestMode.setSelection(java.util.Arrays.asList(Config.TestMode.testModes).indexOf(config.testMode.label()));

        // Update label and input based on test mode
        spinnerTestMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || position == 1) { // Sweep or Max Loops
                    tvInputLabel.setText("Number of Loops:");
                    etInputValue.setHint("Enter number of loops");
                } else { // Max Time
                    tvInputLabel.setText("Max Time (ms):");
                    etInputValue.setHint("Enter max milliseconds");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Prepopulate input value
        etInputValue.setText(String.valueOf(config.loopsOrTime));

        // Folder Selector
        btnSelectFolder.setOnClickListener(v -> startFolderPicker());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Configuration");
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setPositiveButton("OK", (dialog, which) -> {
            config.folderPath = Uri.parse(tvSelectedFolder.getText().toString().replace("Selected Folder: ", "")).toString();

            String input = spinnerWriteSize.getSelectedItem().toString();
            config.maxWriteSize = input.isEmpty() ? 32 : Integer.parseInt(input);

            config.testMode = Config.TestMode.lookup(spinnerTestMode.getSelectedItem().toString());

            input = etInputValue.getText().toString();
            config.loopsOrTime = input.isEmpty() ? 0 : Integer.parseInt(input);

            // Save configuration persistently
            config.save(context);

            // Notify listener
            listener.onConfigSaved();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> listener.onConfigCanceled());
        builder.show();
    }

    private void launchFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void startFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    public void handleFolderPickerResult(Uri folderUri) {
        if (folderUri != null) {
            // Persist permissions for the selected folder
            context.getContentResolver().takePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            // Update the Config object
            config.folderPath = folderUri.toString();
            config.save(context);

            // Update the TextView in the dialog
            if (dialogView != null) {
                TextView tvSelectedFolder = dialogView.findViewById(R.id.tv_selected_folder);
                if (tvSelectedFolder != null) {
                    tvSelectedFolder.setText("Selected Folder: " + config.folderPath);
                }
            }
        }
    }
}
