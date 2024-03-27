package com.example.share;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class BluetoothConnectionService {

    UUID uuid = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b");


    String fileURI = "";
    String key = "";

    CallBackProgress clientProgress;
    Boolean success = false;

    public interface CallBackOnReceive {
        void onReceive(File file);
    }

    public interface CallBackProgress {
        void progress(int p);
    }

    public void startServer(File file, String key, CallBackOnReceive callBackOnReceive) {
        new BluetoothServerController(file, key, callBackOnReceive).start();
    }

    public boolean startClient(String device, String uri, String key, CallBackProgress callBackProgress) {
        fileURI = uri;
        this.key = key;
        clientProgress = callBackProgress;
        BluetoothClient btClient = new BluetoothClient(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device));
        btClient.start();
        try {
            btClient.join();
        } catch (InterruptedException e) {
        }

        return success;
    }


    class BluetoothClient extends Thread {
        BluetoothSocket socket;

        public BluetoothClient(BluetoothDevice device) {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {

            }
        }


        @Override
        public void run() {
            super.run();
            try {
                this.socket.connect();
            } catch (IOException e) {
                return;
            }

            OutputStream outputStream = null;
            try {
                outputStream = this.socket.getOutputStream();
            } catch (IOException e) {
            }
            InputStream inputStream = null;
            try {
                inputStream = this.socket.getInputStream();
            } catch (IOException e) {
            }

            byte[] fileBytes;
            try {
                fileBytes = EncryptFile(fileURI, key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            ByteBuffer fileSize = ByteBuffer.allocate(4);
            fileSize.putInt(fileBytes.length);


            try {
                outputStream.write(fileSize.array());
                outputStream.write(fileBytes);
                success = true;

                sleep(1000);
                outputStream.close();
                inputStream.close();
                clientProgress.progress(0);

                this.socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }


    }


    class BluetoothServerController extends Thread {

        private boolean cancelled;

        private File file;
        private CallBackOnReceive callBackOnReceive;
        private String key;
        private BluetoothServerSocket serverSocket;

        public BluetoothServerController(File file, String key, CallBackOnReceive callBackOnReceive) {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            this.file = file;
            this.callBackOnReceive = callBackOnReceive;
            this.key = key;
            if (btAdapter != null) {
                try {
                    this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("Mehran", uuid);
                } catch (IOException e) {

                }
                this.cancelled = false;
            } else {
                this.serverSocket = null;
                this.cancelled = true;
            }
        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket;

            while (true) {
                if (this.cancelled) {
                    break;
                }

                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (!this.cancelled && socket != null) {
                    new BluetoothServer(socket, file, key,this, callBackOnReceive).start();
                }

            }
        }


        public void cancel() {
            this.cancelled = true;
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    class BluetoothServer extends Thread {
        BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        CallBackOnReceive callBackOnReceive;
        private File file;
        private String key;
        private BluetoothServerController bluetoothServerController;

        public BluetoothServer(BluetoothSocket socket, File file, String key,BluetoothServerController bluetoothServerController, CallBackOnReceive callBackOnReceive) {
            this.socket = socket;
            this.file = file;
            this.bluetoothServerController = bluetoothServerController;
            this.callBackOnReceive = callBackOnReceive;
            this.key = key;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
            }
        }


        @Override
        public void run() {
            super.run();
            try {

                int totalFileSizeInBytes;


                byte[] fileSizebuffer = new byte[4];
                inputStream.read(fileSizebuffer, 0, 4);
                ByteBuffer fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer);
                totalFileSizeInBytes = fileSizeBuffer.getInt();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                int totalBytesRead = 0;
                read = inputStream.read(buffer, 0, buffer.length);

                while (read != -1) {
                    baos.write(buffer, 0, read);
                    totalBytesRead += read;
                    if (totalBytesRead == totalFileSizeInBytes) {
                        break;
                    }
                    read = inputStream.read(buffer, 0, buffer.length);
                }
                baos.flush();

                if (file.exists()) {
                    file.delete();
                }
                byte[] data = baos.toByteArray();
                FileOutputStream fos = new FileOutputStream(file.getPath());
                fos.write(decrypt(data, key));
                fos.close();
                sleep(1000);
                inputStream.close();
                outputStream.close();
                callBackOnReceive.onReceive(file);

                socket.close();
                bluetoothServerController.cancel();

            } catch (IOException e) {

            } catch (InterruptedException e) {
            }
        }


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

    private byte[] decrypt(byte[] data, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}