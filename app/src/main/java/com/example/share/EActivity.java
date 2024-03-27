package com.example.share;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.EActivityBinding;

public class EActivity extends AppCompatActivity {
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
        path = getIntent().getStringExtra("path");

        key = getIntent().getStringExtra("key");
        device = getIntent().getStringExtra("device");


        client();

    }

    private void client() {
        new Handler().postDelayed(() -> new BluetoothConnectionService().startClient(device, path, key, p -> runOnUiThread(() -> binding.progress.setProgressCompat(100,true))), 1000);
    }


}
