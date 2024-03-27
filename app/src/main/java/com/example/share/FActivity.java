package com.example.share;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.DActivityBinding;
import com.example.share.databinding.EActivityBinding;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EActivity extends AppCompatActivity {
    EActivityBinding binding;

    ExecutorService executorService = Executors.newSingleThreadExecutor();
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
        executorService.submit(() -> {
            UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            Log.i("TAG", "client: "+device);
            BluetoothDevice btDevice = bluetoothAdapter.getRemoteDevice(device); // Get the BTAddress after scan
            try {
                BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(serialUUID);
                btSocket.connect();
                OutputStream oStream = btSocket.getOutputStream();
                byte[] data = EncryptFile(path, key);
                ByteArrayInputStream bufferedInputStream = new ByteArrayInputStream(data);
                byte[] bytes = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(bytes)) != -1) {
                    oStream.write(bytes, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private byte[] EncryptFile(String path, String key) throws IOException {
        File file = new File(path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        bufferedInputStream.read(bytes, 0, bytes.length);
        bufferedInputStream.close();
        return encode(bytes, key);
    }


    private byte[] encode(byte[] data, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
