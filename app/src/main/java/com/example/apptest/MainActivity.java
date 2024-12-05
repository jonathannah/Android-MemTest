package com.example.apptest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.TextView;
import android.content.Context;
import android.net.Uri;

import com.example.apptest.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    public static class JMemTestData {
        int memSize;
        double deltaRunTime;
        double deltaUserCPU;


        double deltaSysCPU;
        int sum1;
        int sum2;

        public int getMemSize(){return this.memSize;}
        public double getDeltaRunTime(){return this.deltaRunTime;}
        public double getDeltaUserCPU(){return this.deltaUserCPU;}
        public double getDeltaSysCPU() {return deltaSysCPU;}
        public int getSum1(){return this.sum1;}
        public int getSum2(){return this.sum2;}

        public JMemTestData(int _size, double _deltaTime, double _deltaUserCPU, double deltaSysCPU, int _sum1, int _sum2) {

            this.memSize = _size;
            this.deltaRunTime = _deltaTime;
            this.deltaUserCPU = _deltaUserCPU;
            this.deltaSysCPU = deltaSysCPU;
            this.sum1 = _sum1;
            this.sum2 = _sum2;
        }

        private ActivityResultLauncher<Intent> folderPickerLauncher;
        private ConfigDialog configDialog;

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public java.lang.String toString() {

            return String.format(
                    "%d, %f, %f, %f, %d, %d",
                    this.memSize,
                    this.deltaRunTime,
                    this.deltaUserCPU,
                    this.deltaSysCPU,
                    this.sum1,
                    this.sum2
            );

        }
    };


    // Used to load the 'apptest' library on application startup.
    static {
        System.loadLibrary("apptest");
    }

    private ActivityMainBinding binding;

    ConfigDialog configDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load saved configuration
        Config config = new Config();
        config.load(this);

        // Register the ActivityResultLauncher
        ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri folderUri = result.getData().getData();
                        if (configDialog != null) {
                            configDialog.handleFolderPickerResult(folderUri);
                        }
                    }
                }
        );


        // Show configuration dialog
        configDialog = new ConfigDialog(this, config, folderPickerLauncher, new ConfigDialog.ConfigDialogListener() {
            @Override
            public void onConfigSaved() {
                runTest(config);
            }

            @Override
            public void onConfigCanceled() {
                finish();
            }
        });

        configDialog.show();

    }

    void runTest(Config config){

        StringBuilder results = new StringBuilder();
        results.append(getCPUFamilyName());
        results.append('\n');
        results.append(GetCoreInfo());
        results.append('\n');
        results.append(getNeonInfo());
        results.append('\n');
        results.append("     ").append(getCPUFeatures());
        results.append('\n');
        results.append("     ").append(getSoCName());
        results.append('\n');
        results.append(getSoCInfo());
        results.append('\n');
        results.append('\n');
        results.append(getComprehensiveSoCInfo("     "));
        results.append('\n');
        results.append(startMemTest(config));
        TextView tv = binding.sampleText;
        tv.setText(results.toString());

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        //File file = new File(dir, "memtest_results_" + timeStamp + ".txt");

        String filePath = getSanitisedModelName() + "_sysscan_results_" + timeStamp + ".txt";

        FileUtils.writeFileToConfigFolder(this, config, filePath, results.toString());
    }


    public static String getComprehensiveSoCInfo(String indent) {
        StringBuilder info = new StringBuilder();

        // Get info from Build class
        info.append(indent).append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        info.append(indent).append("Model: ").append(Build.MODEL).append("\n");
        info.append(indent).append("Hardware: ").append(Build.HARDWARE).append("\n");
        info.append(indent).append("Board: ").append(Build.BOARD).append("\n");

        // Get additional properties using SystemProperties
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getMethod("get", String.class);

            String socName = (String) get.invoke(null, "ro.board.platform");
            info.append(indent).append("SoC Name: ").append(socName).append("\n");

            String productBoard = (String) get.invoke(null, "ro.product.board");
            info.append(indent).append("Product Board: ").append(productBoard).append("\n");

            String productManufacturer = (String) get.invoke(null, "ro.product.manufacturer");
            info.append(indent).append("Product Manufacturer: ").append(productManufacturer).append("\n");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get info from /proc/cpuinfo
        info.append(indent).append("CPU Info:\n").append(getCpuInfo(indent+"     "));

        return info.toString();
    }

    private String getCPUFeatures()
    {
        StringBuilder featuresStr = new StringBuilder();
        ArrayList<String> features = getArmFeatures();

        for(String curFeature : features){
            if(featuresStr.length() > 0){
                featuresStr.append(", ");
            }
            featuresStr.append(curFeature);
        }

        return "Features: " + featuresStr.toString();
    }

    private String getNeonInfo(){
        StringBuilder info = new StringBuilder();

        // Check CPU ABI
        String cpuAbi = Build.SUPPORTED_ABIS[0];
        info.append("CPU ABI: ").append(cpuAbi).append("\n");

        // Check NEON support
        boolean neonSupported1 = false;
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.contains("neon")) {
                neonSupported1 = true;
                break;
            }
        }

        boolean neonSupported2 = isNeonSupported();

        info.append("NEON Supported: ").append(neonSupported2 ? "Yes" : "No").append("\n");
        //info.append("NEON Supported2: ").append(neonSupported2 ? "Yes" : "No").append("\n");
        return info.toString();
    }

    private String GetCoreInfo(){return GetCoreInfo("     ");}
    private String GetCoreInfo(String indent){
        StringBuilder info = new StringBuilder();

        // Get number of cores
        int cores = Runtime.getRuntime().availableProcessors();
        info.append(indent).append("Number of Cores: ").append(cores).append("\n");

        // Get clock speed of each core
        for (int i = 0; i < cores; i++) {
            String path = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line = br.readLine();
                if (line != null) {
                    int maxFreqKHz = Integer.parseInt(line.trim());
                    info.append(indent).append("Core ").append(i).append(" Max Frequency: ").append(maxFreqKHz / 1000).append(" MHz\n");
                }
            } catch (IOException e) {
                info.append(indent).append(e.toString());
            }
        }
        return info.toString();
    }

    private String startMemTest(Config cfg){

        Context context = this;


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        //File file = new File(dir, "memtest_results_" + timeStamp + ".txt");
        String filePath = getSanitisedModelName() + "_memtest_results_" + timeStamp + ".csv";

        StringBuilder sb = new StringBuilder(getSoCInfo());
        if(cfg.testMode == Config.TestMode.MaxTime || cfg.testMode == Config.TestMode.All) {
            int blockSize = cfg.testMode == Config.TestMode.All ? 1024*1024 : cfg.maxWriteSize;
            int loops = cfg.testMode == Config.TestMode.All ? 1000 : cfg.loopsOrTime;

            double exeTime = runLoopMemTest(loops, blockSize);

            sb.append("\n\nLoop Test");
            sb.append("\nLoops,Time (s)");
            sb.append(String.format(Locale.getDefault(),"\n%d,%f", blockSize, exeTime));
        }

        if(cfg.testMode == Config.TestMode.MaxTime || cfg.testMode == Config.TestMode.All) {
            int timeMS = cfg.testMode == Config.TestMode.All ? 1000 : cfg.loopsOrTime;
            int blockSize = cfg.testMode == Config.TestMode.All ? 32*1024*1024 : cfg.maxWriteSize;

            long bytesWritten = runTimedMemTest(timeMS, blockSize);

            sb.append("\n\nTime Test");
            sb.append("\nTime (ms),Bytes");
            sb.append(String.format(Locale.getDefault(),"\n%d,%d", timeMS, bytesWritten));
            sb.append('\n');
        }

        if(cfg.testMode == Config.TestMode.Sweep || cfg.testMode == Config.TestMode.All) {

            final int LOOP_ITERATIONS = cfg.loopsOrTime;
            final int MAX_BLOCK_SIZE = 1024 * 1024 * cfg.maxWriteSize;
            final int INITIAL_BLOCK = 1024;//* 500;

            JMemTestData[] resultsData = runSweepMemTest(LOOP_ITERATIONS, INITIAL_BLOCK, MAX_BLOCK_SIZE);
            sb.append("\n\nSweep Test");
            sb.append(String.format(Locale.getDefault(), "\nLoop size: %d Block size: %d", LOOP_ITERATIONS, MAX_BLOCK_SIZE));

            sb.append("\nsize, deltaRunTime, deltaUserCPU, deltaSysCPU, sum1, sum2\n");

            for (JMemTestData cur : resultsData) {
                sb.append(cur.toString()).append('\n');
            }
            sb.append("\n\n");
        }

        String results = sb.toString();
        FileUtils.writeFileToConfigFolder(this, cfg, filePath, results);
        return "";//results;

    }

    public static String getCpuInfo(String indent) {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                cpuInfo.append(indent).append(line).append('\n');
            }
            br.close();
        } catch (IOException e) {
            return e.toString();
        }
        return cpuInfo.toString();
    }

    public String getSanitisedModelName() {
        // Define a regular expression to match invalid characters
        // The regex includes characters that are typically not allowed in file names
        String modelName = getModel();
        String sanitized = modelName.replaceAll("[<>:\"/\\\\|?*\\[\\]()]", "_");

        sanitized = sanitized.replaceAll(" +", "_");
        // Optionally, replace multiple underscores with a single underscore
        sanitized = sanitized.replaceAll("_+", "_");

        // Trim leading and trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        return sanitized;
    }


    /**
     * A native method that is implemented by the 'apptest' native library,
     * which is packaged with this application.
     */
    public native JMemTestData[] runSweepMemTest(int loopIterations, int initialBlockSize, long maxBlockSize);
    public native double runLoopMemTest(int loopIterations, int blockSize);
    public native long runTimedMemTest(int runtimeMSecs, int blockSize);
    public native boolean isNeonSupported();
    public native String getCPUFamilyName();
    public native ArrayList<String> getArmFeatures();
    public native String getSoCName();
    public native String getSoCInfo();
    public native String getModel();


}