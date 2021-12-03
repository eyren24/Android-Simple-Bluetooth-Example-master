package com.mcuhq.simplebluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity2 extends AppCompatActivity {
    private final String TAG = MainActivity2.class.getSimpleName();
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private BluetoothAdapter mBTAdapter;
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    public Handler mHandler;
    private TextView mReadBuffer;
    private TextView mBluetoothStatus;

    public MainActivity mainActivity = new MainActivity();
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main2);
        Button mDisconnect = (Button) findViewById(R.id.disconnectBtn);
        Button mAvanti = (Button) findViewById(R.id.avanti);
        Button mIndietro = (Button) findViewById(R.id.indietro);
        Button mSinistra = (Button) findViewById(R.id.sinistra);
        Button mDestra = (Button) findViewById(R.id.destra);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetooth_status);
        mReadBuffer = (TextView) findViewById(R.id.read_buffer_motore);
        TextView mReaderEnergia = (TextView) findViewById(R.id.read_buffer_energia);
        TextView mVelocitaMotoreValue = (TextView) findViewById(R.id.velocitaMotoreValue);
        TextView mBatteria1 = (TextView) findViewById(R.id.read_buffer_batteria_1);
        TextView mBatteria2 = (TextView) findViewById(R.id.read_buffer_batteria_2);
        TextView mPIstantanea = (TextView) findViewById(R.id.read_buffer_p_istantanea);
        SeekBar mVelocitaMotore = (SeekBar) findViewById(R.id.velocitaMotore);
        ImageView mBussola = findViewById(R.id.bussola);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        String info = MainActivity.getInfoDevice();
        final String address = info.substring(info.length() - 17);
        final String name = info.substring(0,info.length() - 17);
        // Legge i messaggi in arrivo
        mHandler = new Handler(Looper.getMainLooper()){
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    String prefix = readMessage.substring(0, readMessage.indexOf(' '));
                    String value = readMessage.substring(readMessage.indexOf(' '));
                    System.out.println(prefix + " " + value);
                    if(prefix.equalsIgnoreCase("motore")){
                        mReadBuffer.setText(value);
                    }
                    if(prefix.equalsIgnoreCase("energia")){
                        mReaderEnergia.setText(value);
                    }
                    if(prefix.equalsIgnoreCase("bussola")){
                        float getValue = Float.parseFloat(value);
                        mBussola.setRotation(getValue);
                    }
                    if(prefix.equalsIgnoreCase("batteria1")){
                        mBatteria1.setText(value);
                    }
                    if(prefix.equalsIgnoreCase("batteria2")){
                        mBatteria2.setText(value);
                    }
                    if(prefix.equalsIgnoreCase("pIstantanea")){
                        mPIstantanea.setText(value);
                    }
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + msg.obj);
                    else
                        mBluetoothStatus.setText("Connection Failed");
                        //mainActivity.switchActivitiesWithData(MainActivity.class);
                }
            }
        };

        // Spawn a new thread to avoid blocking the GUI one
        new Thread()
        {
            @Override
            public void run() {
                boolean fail = false;

                BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                try {
                    mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();

                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();

                    }
                }
                if(!fail) {
                    mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget();
                }
            }
        }.start();

        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.cancel();
                switchActivitiesWithData();
            }
        });

        mVelocitaMotore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            String progressChangedValue = null;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = Integer.toString(progress);
                mVelocitaMotoreValue.setText(progressChangedValue);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mVelocitaMotoreValue.setText(progressChangedValue);
                if (progressChangedValue.equals("0")){
                    mVelocitaMotoreValue.setText("Motore Spento!");
                }
            }
        });

        mAvanti.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mAvanti.setRotationX(20F);
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 100);
                        break;
                    case MotionEvent.ACTION_UP:
                        mAvanti.setRotationX(0F);
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            final Runnable mAction = new Runnable() {
                @Override public void run() {
                    mConnectedThread.write("w");
                    mHandler.postDelayed(this, 100);
                }
            };

        });

        mIndietro.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIndietro.setRotationX(-20F);
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 100);
                        break;
                    case MotionEvent.ACTION_UP:
                        mIndietro.setRotationX(0F);
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            final Runnable mAction = new Runnable() {
                @Override public void run() {
                    mConnectedThread.write("s");
                    mHandler.postDelayed(this, 100);
                }
            };

        });
        mSinistra.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 100);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            final Runnable mAction = new Runnable() {
                @Override public void run() {
                    mConnectedThread.write("a");
                    mHandler.postDelayed(this, 100);
                }
            };

        });

        mDestra.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 100);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            final Runnable mAction = new Runnable() {
                @Override public void run() {
                    mConnectedThread.write("d");
                    mHandler.postDelayed(this, 100);
                }
            };

        });
    }

    public void switchActivitiesWithData() {
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        startActivity(switchActivityIntent);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

}