package com.example.share;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.share.databinding.ActivityMainBinding;
import com.example.share.databinding.BActivityBinding;

import java.io.File;
import java.io.IOException;

public class BActivity extends AppCompatActivity implements ProgressView.OnFinishRecordTime {
    BActivityBinding binding;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = BActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(binding.getRoot());

        binding.progressView.setOnTouchListener((v, event) -> {
            Log.i("TAG", "onCreate: " + event.getAction());
            if (event.getAction() == ACTION_DOWN) {
                req.launch(Manifest.permission.RECORD_AUDIO);
            } else if (event.getAction() == ACTION_UP) {
                binding.progressView.cancelProgress();
                finishRecord();
            }

            return true;

        });

    }


    ActivityResultLauncher<String> req = registerForActivityResult(new ActivityResultContracts.RequestPermission(), o -> {
        if (o) {
            try {
                startRecord();
            } catch (IOException e) {
                mediaRecorder = null;
            }
        }
    });


    MediaRecorder mediaRecorder;

    private void startRecord() throws IOException {


        mediaRecorder = new MediaRecorder();

        File folder = new File(getCacheDir() + File.separator + "caches");
        if (!folder.exists())
            folder.mkdirs();

        File path = new File(folder, System.currentTimeMillis() + ".3gp");

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaRecorder.setOutputFile(path);
        }
        mediaRecorder.prepare();
        mediaRecorder.start();
        binding.progressView.startProgress(this);

    }


    private void finishRecord() {
        if (mediaRecorder == null)
            return;
        binding.send.setEnabled(true);

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception e) {
        }
        mediaRecorder = null;
    }

    @Override
    public void onFinishTime() {
        finishRecord();
    }
}
