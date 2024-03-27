package com.example.share;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.CActivityBinding;
import com.example.share.databinding.DActivityBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DActivity extends AppCompatActivity {
    DActivityBinding binding;
    BluetoothAdapter mBluetoothAdapter;

    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = DActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(binding.getRoot());

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        binding.rec.setAdapter(arrayAdapter);
        String path = getIntent().getStringExtra("path");

        String key = getIntent().getStringExtra("key");
        binding.rec.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(DActivity.this, EActivity.class);
            intent.putExtra("path",path);
            intent.putExtra("key",key);
            intent.putExtra("device",arrayAdapter.getItem(position).split("\n")[1].trim());
            startActivity(intent);
        });

    }

    private void bluetoothError() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(DActivity.this);
        dialog.setTitle("Error");
        dialog.setMessage("Bluetooth not available");
        dialog.setNeutralButton("OK", (dialog1, which) -> finish());
        dialog.setCancelable(false);
        dialog.show();
    }

    ActivityResultLauncher<Intent> reqBlue = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        if (o.getResultCode() == RESULT_OK) {
            bluetooth();
            return;
        }
        finish();
    });

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

        if (!mBluetoothAdapter.isEnabled())
            if (!mBluetoothAdapter.enable()) {
                reqBlue.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                return;
            }


        bluetooth();

    });


    private void bluetooth() {
        locationCheck();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);


    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetooth();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("TAG", "onReceive: " + action);
            //Finding devices
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDevice(device);
            }
        }
    };

    private void addDevice(BluetoothDevice device) {
        for (int i = 0; i < arrayAdapter.getCount(); i++) {
            if (arrayAdapter.getItem(i).contains(device.getAddress()))
                return;
        }

        if (device.getName() != null && !device.getName().isEmpty()) {
            arrayAdapter.add(device.getName() + " \n " + device.getAddress());
        }
        arrayAdapter.notifyDataSetChanged();

    }

    private void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean hasBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!hasBluetooth) {
            bluetoothError();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rqPer.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION});
        } else {
            rqPer.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION});

        }
    }


}
