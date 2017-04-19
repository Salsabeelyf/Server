package com.example.salsabeel.server;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements WifiP2pManager.PeerListListener,WifiP2pManager.ConnectionInfoListener {


    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog = null;
    private WifiP2pManager manager;
    private WifiP2pInfo info;
    private WifiP2pDevice device;
    private boolean isWifiP2pEnabled = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    NewSpinner sp;
    ArrayAdapter<String> adapter;
    ArrayList<String> list = new ArrayList<String>();
    Button btnSearch;
    int i,j;

    ServerSocket ss;
    Socket someSocket;
    InputStream is;
    DataInputStream in ;
    TextView tv;
    Thread th;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);


        sp = (NewSpinner) findViewById(R.id.spinner);
        adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        sp.setAdapter(adapter);

        i = 0;


        btnSearch = (Button) findViewById(R.id.searchBtn);
        //et = (EditText) findViewById(R.id.editText);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                j = 0;
                if (!isWifiP2pEnabled) {
                    Toast.makeText(MainActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(MainActivity.this, "Press back to cancel", "finding peers", true, true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (j == 0) {
                    j++;
                    return;
                }
                WifiP2pDevice device = peers.get(position);
                showDetails(device);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }


    /**
     * register the BroadcastReceiver with the intent values to be matched
     */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());


        if (peers.size() == 0) {
            Log.d("on peers available: ", "No devices found");
            list.clear();
            adapter.clear();
            adapter.addAll(list);
            adapter.notifyDataSetChanged();
            return;
        }
        list.clear();
        // add peers to spinner
        for (int i = 0; i < peers.size(); i++) {
            list.add(i, peers.get(i).deviceName);
        }
        adapter.clear();
        adapter.addAll(list);
        adapter.notifyDataSetChanged();

    }
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;

        // The owner IP is now known.
        TextView view = (TextView) findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());


        // if connection is established, then start receiving data
        if (info.groupFormed && info.isGroupOwner) {
                receiveData();
            }
        }


    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    public void receiveData()
    {
        try {
            ss = new ServerSocket(8988);
            someSocket = ss.accept();
            is = someSocket.getInputStream();
            in = new DataInputStream(is);
            tv = (TextView) findViewById(R.id.dataTextView);
            th = new Thread(){
                @Override
                public void run()
                {
                    while (true) {
                        try {
                            tv.setText(in.readUTF());
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(),"Error receiving data",Toast.LENGTH_LONG);
                            try {
                                someSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                }

            };

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
