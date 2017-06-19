package grazyna.alchebits.com.grazynaremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class CalibrateActivity extends AppCompatActivity {

    private String m_btDevName = "";
    private String m_btMacAddress = "";

    final int RECIEVE_MESSAGE = 1;
    Handler m_handler;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // some random

    private TextView m_kValuesTxtView;
    private TextView m_termValuesTxtView;

    private int m_seriesCounter = 0;
    protected XYSeries m_seriesP;
    protected XYSeries m_seriesI;
    protected XYSeries m_seriesD;
    protected XYMultipleSeriesDataset m_dataset;
    protected GraphicalView m_chartView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        setBttnsListeners();
        createChart();

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            m_btDevName = extras.getString(Extra.EXTRA_BT_NAME);
            m_btMacAddress = extras.getString(Extra.EXTRA_BT_MAC);

            Log.d(MainActivity.APP_NAME, "taken bt name and mac: "+m_btDevName+"@"+m_btMacAddress);
        }

        m_kValuesTxtView = (TextView) findViewById(R.id.k_values_txtview);
        m_termValuesTxtView = (TextView) findViewById(R.id.term_values_txtview);


        final CalibrateActivity thisActivity = this;
        m_handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;

                        for(int i = 0 ; i < msg.arg1 ; i++)
                        {
                            if(i+1 < msg.arg1)
                            {
                                if(readBuf[i+1] == GrazynaArduino.BT_COMM_DELIMETER.charAt(1))
                                {
                                    if(readBuf[i] == GrazynaArduino.BT_COMM_DELIMETER.charAt(0))
                                    {
                                        i+=1;
                                        parseCommand(sb.toString());
                                        sb = new StringBuilder();
                                        continue;
                                    }
                                }
                            }
                            sb.append((char)readBuf[i]);
                        }
                        break;
                }
            };
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(MainActivity.APP_NAME, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(MainActivity.APP_NAME, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(m_btMacAddress);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(MainActivity.APP_NAME, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(MainActivity.APP_NAME, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(MainActivity.APP_NAME, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(MainActivity.APP_NAME, "...In onPause()...");
        mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_OFF_MSG+GrazynaArduino.BT_COMM_DELIMETER);
        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(MainActivity.APP_NAME, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    void setBttnsListeners()
    {
        Button calibOnBttn = (Button)findViewById(R.id.calib_on_bttn);
        Button calibOffBttn = (Button)findViewById(R.id.calib_off_bttn);
        Button kpPlusBttn = (Button) findViewById(R.id.kp_pp_bttn);
        Button kiPlusBttn = (Button) findViewById(R.id.ki_pp_bttn);
        Button kdPlusBttn = (Button) findViewById(R.id.kd_pp_bttn);
        Button divPlusBttn = (Button) findViewById(R.id.div_pp_bttn);
        Button kpMinusBttn = (Button) findViewById(R.id.kp_mm_bttn);
        Button kiMinusBttn = (Button) findViewById(R.id.ki_mm_bttn);
        Button kdMinusBttn = (Button) findViewById(R.id.kd_mm_bttn);
        Button divMinusBttn = (Button) findViewById(R.id.div_mm_bttn);

        calibOnBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_ON_MSG+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        calibOffBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_OFF_MSG+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kpPlusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KP_PP+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kiPlusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KI_PP+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kdPlusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KD_PP+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        divPlusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_DIV_PP+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kpMinusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KP_MM+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kiMinusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KI_MM+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        kdMinusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_KD_MM+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });

        divMinusBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write(GrazynaArduino.BT_CALIBRATION_DIV_MM+GrazynaArduino.BT_COMM_DELIMETER);
            }
        });


    }

    public void parseCommand(String btCommand)
    {
        if(btCommand.contains(GrazynaArduino.BT_REC_CALIB_DATA_CMD))
        {

            Log.d(MainActivity.APP_NAME, btCommand);
            String[] cmmndParams = btCommand.split("="); // cmd=param,param2,param3
            String[] params = new String[10];

            if(cmmndParams.length > 1)
                params = cmmndParams[1].split(",");

            if(params.length >= 7) {
                m_kValuesTxtView.setText("kp=" + params[0] + ", ki=" + params[1] + ", kd=" + params[2] + ", divider=" + params[3]);
                m_termValuesTxtView.setText("pTerm=" + params[4] + ", iTerm=" + params[5] + ", dTerm=" + params[6]);
                try{
                    int kp = Integer.parseInt(params[0]);
                    int ki = Integer.parseInt(params[1]);
                    int kd = Integer.parseInt(params[2]);
                    int divider = Integer.parseInt(params[3]);
                    int pTerm = Integer.parseInt(params[4]);
                    int iTerm = Integer.parseInt(params[5]);
                    int dTerm = Integer.parseInt(params[6]);

                    addToChart(Integer.parseInt(params[4]), Integer.parseInt(params[5]), Integer.parseInt(params[6]));
                }catch(Exception exc){
                }
            }
        }
    }

    protected XYSeriesRenderer getRendererInColor(int color)
    {
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setLineWidth(2);
        renderer.setColor(color);
        // Include low and max value
        renderer.setDisplayBoundingPoints(true);
        // we add point markers
        renderer.setPointStyle(PointStyle.CIRCLE);
        renderer.setPointStrokeWidth(3);
        return renderer;
    }

    protected void createChart()
    {
        m_seriesP = new XYSeries("P TERM VALUES");
        m_seriesI = new XYSeries("I TERM VALUES");
        m_seriesD = new XYSeries("D TERM VALUES");

        m_dataset = new XYMultipleSeriesDataset();
        m_dataset.addSeries(m_seriesP);
        m_dataset.addSeries(m_seriesI);
        m_dataset.addSeries(m_seriesD);

        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        mRenderer.addSeriesRenderer(getRendererInColor(Color.RED));
        mRenderer.addSeriesRenderer(getRendererInColor(Color.GREEN));
        mRenderer.addSeriesRenderer(getRendererInColor(Color.BLUE));

        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        mRenderer.setPanEnabled(false, false);
        mRenderer.setYAxisMax(500);
        mRenderer.setYAxisMin(-500);
        mRenderer.setShowGrid(true);

        m_chartView = ChartFactory.getLineChartView(this, m_dataset, mRenderer);
        LinearLayout chartLayout = (LinearLayout)findViewById(R.id.chart_layout);
        chartLayout.addView(m_chartView);
    }

    protected void addToChart(int pTerm, int iTerm, int dTerm) {
//        m_seriesP.add(pTerm, m_seriesCounter);
//        m_seriesI.add(iTerm, m_seriesCounter);
//        m_seriesD.add(dTerm, m_seriesCounter);

        m_seriesP.add(m_seriesCounter, pTerm);
        m_seriesI.add(m_seriesCounter, iTerm);
        m_seriesD.add(m_seriesCounter, dTerm);
        if (m_seriesCounter > 100)
        {
            m_seriesP.remove(0);
            m_seriesI.remove(0);
            m_seriesD.remove(0);
        }
        m_seriesCounter++;

        m_chartView.invalidate();
        m_chartView.repaint();
    }

    // SUBCLASS:
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                    try {
                        if (mmInStream.available() > 0) {
                            //Log.d(MainActivity.APP_NAME,"available: "+mmInStream.available());
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                            m_handler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                        }
                    } catch (IOException e) {
                        break;
                    }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            synchronized(this) {
                Log.d(MainActivity.APP_NAME, "...Data to send: " + message + "...");
                byte[] msgBuffer = message.getBytes();
                try {
                    mmOutStream.write(msgBuffer);
                } catch (IOException e) {
                    Log.d(MainActivity.APP_NAME, "...Error data send: " + e.getMessage() + "...");
                }
            }
        }
    }
}
