package net.maribat.zebrabluethprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    Button button,print,disconnect;
    private Connection connection;
    ZebraPrinter printer;

    private static int PICTURE_FROM_GALLERY = 2;
    private static File file = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        print = findViewById(R.id.print);
        disconnect = findViewById(R.id.disconnect);
        button.setOnClickListener(v -> {
            EditText adress = findViewById(R.id.macAdress);
            String macStr = adress.getText().toString();
             printer =  connectToPrinter(macStr);
        });

        disconnect.setOnClickListener(v -> {
            disconnect();
        });

        print.setOnClickListener(v -> {
            if (printer != null){
                printText();
            }
        });
    }

    private void printText() {

    }


    // ============ Connect to printer by mac adress
    private ZebraPrinter connectToPrinter(String macStr) {

        Log.i(TAG, "connectToPrinter: Connecting...");
            connection = null;
                connection = new BluetoothConnection(macStr);
            try {
                connection.open();
                Log.i(TAG, "connectToPrinter: Connected");
            } catch (ConnectionException e) {
                Log.i(TAG, "connectToPrinter: Disconnecting");
                //disconnect();
            }
            ZebraPrinter printer = null;
            if (connection.isConnected()) {
                try {
                    printer = ZebraPrinterFactory.getInstance(connection);
                    Log.i(TAG, "connectToPrinter: Determining Printer Language");

                    String pl = SGD.GET("device.languages", connection);
                    Log.i(TAG, "connectToPrinter: Printer Language " + pl);
                } catch (ConnectionException | ZebraPrinterLanguageUnknownException e) {
                    Log.i(TAG, "connectToPrinter: Unknown Printer Language");
                   printer = null;
                 //   DemoSleeper.sleep(1000);
                  //  disconnect();
                }
            }

            return printer;

    }
    // ============ Disconnect function
    public void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (ConnectionException e) {
            Log.i(TAG, "disconnect: COMM Error! Disconnected");
        } finally {
            Log.i(TAG, "disconnect: ");

        }
    }
    // ============ Print image

    private void getPhotosFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE_FROM_GALLERY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri imgPath = data.getData();
            Bitmap myBitmap = null;
            try {
                myBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imgPath);
            } catch (FileNotFoundException e) {
                Log.i(TAG, "onActivityResult: " +e.getMessage());
            } catch (IOException e) {
                Log.i(TAG, "onActivityResult: " +e.getMessage());
            }
            printPhotoFromExternal(myBitmap);
        }
    }

    private void printPhotoFromExternal(final Bitmap bitmap) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Looper.prepare();
                    Log.i(TAG, "run: Sending image to printer");
                        printer.printImage(new ZebraImageAndroid(bitmap), 0, 0, 550, 412, false);


                    if (file != null) {
                        file.delete();
                        file = null;
                    }
                } catch (ConnectionException e) {
                    Log.i(TAG, "run: " + e.getMessage());
                } finally {
                    bitmap.recycle();
                    Looper.myLooper().quit();
                }
            }
        }).start();

    }



}