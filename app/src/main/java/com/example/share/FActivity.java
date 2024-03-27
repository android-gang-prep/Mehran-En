package com.example.share;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.EActivityBinding;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class FActivity extends AppCompatActivity {
    EActivityBinding binding;

    String device;
    BluetoothAdapter bluetoothAdapter;
    String path;
    String key;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = EActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(binding.getRoot());

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        key = getIntent().getStringExtra("key");

        DISCOVERABLE = false;

        binding.play.setOnClickListener(v -> play(f));
    }


    ActivityResultLauncher<Intent> reqBlue = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        if (o.getResultCode() == RESULT_OK) {
            server();
            return;
        }
        finish();
    });

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetooth();
    }

    public void locationCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("enable your GPS")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> finish());
        builder.show();
    }

    ActivityResultLauncher<String[]> rqPer = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), o -> {
        for (Map.Entry<String, Boolean> stringBooleanEntry : o.entrySet()) {
            if (!stringBooleanEntry.getValue())
                return;
        }

        if (!bluetoothAdapter.isEnabled())
            if (!bluetoothAdapter.enable()) {
                reqBlue.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                return;
            }


        server();


    });


    File f;

    boolean DISCOVERABLE = false;
    ActivityResultLauncher<Intent> reqDISCOVERABLE = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        if (o.getResultCode() == 300) {
            DISCOVERABLE = true;
        }
    });

    private void server() {

        locationCheck();
        File file = new File(Environment.getExternalStorageDirectory(), "Pictures");
        if (!file.exists())
            file.mkdirs();
        f = new File(file, System.currentTimeMillis() + ".3gp");
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (!DISCOVERABLE)
            reqDISCOVERABLE.launch(intent);
        new Handler().postDelayed(() -> new BluetoothConnectionService().startServer(f, key, file1 -> runOnUiThread(() -> {
            f = file1;
            binding.progress.setProgressCompat(100, true);
            binding.play.setVisibility(View.VISIBLE);
        })), 1000);


    }

    MediaPlayer mediaPlayer;

    private void play(File file) {
        if (mediaPlayer != null)
            mediaPlayer.pause();
        mediaPlayer = MediaPlayer.create(FActivity.this, Uri.fromFile(file));
        mediaPlayer.start();
    }


    private void checkBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean hasBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!hasBluetooth) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rqPer.launch(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION});
        } else {
            rqPer.launch(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION});

        }
    }
}
