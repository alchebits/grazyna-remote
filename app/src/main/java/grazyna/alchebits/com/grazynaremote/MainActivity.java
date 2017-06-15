package grazyna.alchebits.com.grazynaremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    protected static final String APP_NAME = "GrazynaRemote";
    protected static final int REQUEST_ENABLE_BT = 100; // must be grater than 0
    protected BluetoothAdapter m_bluetoothAdapter = null;
    protected Context m_context = null;

    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver m_receiver = createBroadcastReciever();
    private boolean m_recevierIsOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_context = this;
        enableBluetooth();
        setOnClickListeners();
        addPairedDeviceToList();
        //startDiscoverBluetoothDevices();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT)
        {
            switch(resultCode)
            {
                case RESULT_OK: {
                    Log.d(APP_NAME, "Good choice !!!");
                    break;
                }
                case RESULT_CANCELED:
                default:
                {
                    Log.d(APP_NAME, "So app will not work faggot !");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(m_recevierIsOn)
            unregisterReceiver(m_receiver);
        m_recevierIsOn = false;
    }


    protected void enableBluetooth()
    {
        if(m_bluetoothAdapter == null)
            m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!m_bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(MainActivity.APP_NAME, "next paired name: "+deviceName);
            }
        }
    }

    protected void addPairedDeviceToList()
    {
        list = (ListView)findViewById(R.id.paired_list_view);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                return view;
            }
        } ;
        list.setAdapter(adapter);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                arrayList.add(deviceName+"@"+deviceHardwareAddress);
                adapter.notifyDataSetChanged();
                Log.d(MainActivity.APP_NAME, "next paired name: "+deviceName);
                Log.d(MainActivity.APP_NAME, "next paired MAC: "+deviceHardwareAddress);
            }
        }
    }

    protected void setOnClickListeners()
    {
        final Context ctx = this;
        ListView list = (ListView)findViewById(R.id.paired_list_view);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView txtView = (TextView)view;
                Log.d(APP_NAME, txtView.getText().toString());

                String[] splitedStrs = txtView.getText().toString().split("@");

                Intent intent = new Intent(ctx, MenuActivity.class);
                intent.putExtra(Extra.EXTRA_BT_NAME, splitedStrs[0]);
                intent.putExtra(Extra.EXTRA_BT_MAC, splitedStrs[1]);
                ctx.startActivity(intent);
            }
        });
    }

    protected BroadcastReceiver createBroadcastReciever()
    {
        return  new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    Log.d(MainActivity.APP_NAME, "Founded next device name: "+ deviceName);
                    Log.d(MainActivity.APP_NAME, "Founded next device MAC: "+ deviceHardwareAddress);
                }
            }
        };
    }

    protected void startDiscoverBluetoothDevices()
    {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(m_receiver, filter);
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        m_recevierIsOn = true;
    };
}
