package com.example.share;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.CActivityBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class CActivity extends AppCompatActivity {
    CActivityBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = CActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(binding.getRoot());

        String path = getIntent().getStringExtra("path");

        String key = getRandomKey();

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix result = multiFormatWriter.encode(key, BarcodeFormat.QR_CODE, 600, 600);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(result);
            binding.img.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        binding.next.setOnClickListener(v -> {
            Intent intent = new Intent(CActivity.this, DActivity.class);
            intent.putExtra("path",path);
            intent.putExtra("key",key);
            startActivity(intent);
        });


    }


    private String getRandomKey() {
        String words = "0123456789qwertyuiopasdfghjklzxcvbnm";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; ++i)
            sb.append(words.charAt(random.nextInt(words.length())));
        return sb.toString();
    }



}
