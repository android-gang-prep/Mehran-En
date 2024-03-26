package com.example.share;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

public class PictureService {
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Activity context;
    private WindowManager windowManager;
    private CameraManager manager;
    String currentCameraId;

    String DirectoryName;


    public void startCapturing(final Activity activity) {

        this.context = activity;

        createFolder();
        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.windowManager = context.getWindowManager();

        try {
            final String[] cameraIdList = manager.getCameraIdList();
            for (String s : cameraIdList) {
                if (manager.getCameraCharacteristics(s).get(CameraCharacteristics.LENS_FACING) == LENS_FACING_FRONT) {
                    this.currentCameraId = s;
                    Log.i("Camera Mehran", s);
                }
            }

            if (!currentCameraId.isEmpty()) {
                openCameraAndTakePicture();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createFolder() {

        DateFormat df = new SimpleDateFormat("dd-MM-yy");
        Date currentDt = new Date();

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "Pictures");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {

            DirectoryName = folder.getPath() + File.separator + df.format(currentDt);

            folder = new File(DirectoryName);
            if (!folder.exists()) {
                if (!folder.mkdirs())
                    return;
            }
            DirectoryName = DirectoryName + File.separator + "Image-1";

            folder = new File(DirectoryName);
            if (!folder.exists()) {
                folder.mkdirs();
            }

        } else {
            this.context.runOnUiThread(() -> Toast.makeText(this.context, "Not create folder", Toast.LENGTH_SHORT).show());
        }
    }

    private void openCameraAndTakePicture() {
        startBackgroundThread();
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(currentCameraId, stateCallback, null);
            }
        } catch (CameraAccessException e) {
        }
    }


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            takePicture();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (cameraDevice != null) {
                cameraDevice.close();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            stopBackgroundThread();
        }


        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (cameraDevice != null) {
                cameraDevice.close();
            } else {
                cameraDevice = null;
            }
        }
    };


    private void takePicture() {
        if (null == cameraDevice) {
            return;
        }


        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                if (characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) != null) {
                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(ImageFormat.JPEG);
                }
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            int rotation = windowManager.getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            ImageReader.OnImageAvailableListener readerListener = (ImageReader readerL) -> {
                Image image = readerL.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                saveImageToDisk(bytes);
                image.close();
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void saveImageToDisk(final byte[] bytes) {

        final File file = new File(DirectoryName
                + File.separator + System.currentTimeMillis() + "_pic.jpg");

        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            try {
                output.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
        }

    }

    public String getVersion() {
        String v = "";
        try {
            v = String.valueOf(this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return v;
    }


    private void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("Camera Background" + currentCameraId);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
        }
    }


    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }


    final private CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            closeCamera();
        }
    };


}