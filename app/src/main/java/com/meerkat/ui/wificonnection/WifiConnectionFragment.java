package com.meerkat.ui.wificonnection;

import static com.meerkat.Settings.port;
import static com.meerkat.Settings.wifiName;
import static com.meerkat.log.Log.useLogWriter;
import static com.meerkat.log.Log.viewLogWriter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meerkat.MainActivity;
import com.meerkat.PingComms;
import com.meerkat.R;
import com.meerkat.Settings;
import com.meerkat.WifiConnection;
import com.meerkat.databinding.FragmentWifiConnectionBinding;
import com.meerkat.log.Log;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class WifiConnectionFragment extends Fragment implements ApListAdapter.ScanResultClickListener, MaterialButton.OnClickListener {

    private static ApListAdapter myAdapter;
    ArrayList<String> accessPoints;
    private Context context;
    private FragmentWifiConnectionBinding binding;

    public WifiConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        useLogWriter(viewLogWriter, false);
        // Inflate the layout for this fragment
        binding = FragmentWifiConnectionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        this.context = getContext();
        accessPoints = new ArrayList<>();
        Button b = binding.scanWifiButton;
        b.setOnClickListener(this::onClick);
        b.setVisibility(View.VISIBLE);
        context.registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        myAdapter = new ApListAdapter(accessPoints, this);
        RecyclerView apListView = binding.aplistview;
        apListView.setVisibility(View.INVISIBLE);
        // Improve performance if you know that changes in content do not change the layout size of the RecyclerView
        apListView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(apListView.getContext());
        apListView.setLayoutManager(layoutManager);
        apListView.setAdapter(myAdapter);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("WifiConnectionFragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("WifiConnectionFragment paused");
    }

    public static WifiConnection findWifiConnection() {
        Log.i("New connection");

        WifiConnection result = new WifiConnection();
        return result;
    }

    @Override
    public void onClick(View v) {
        Button b = binding.scanWifiButton;
        b.setVisibility(View.INVISIBLE);
        RecyclerView apList = binding.aplistview;
        apList.setVisibility(View.VISIBLE);
        Log.i("Scanning WiFi");
        wifiName = null;
        if (PingComms.pingComms != null)
            PingComms.pingComms.stop();
        //noinspection deprecation
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();

    }

    private class WifiScanReceiver extends BroadcastReceiver {
        // This is checked via mLocationPermissionApproved boolean
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            @SuppressLint("MissingPermission") List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.i(scanResults.size() + " APs discovered.");
            // User to select from available Wifi networks
            for (ScanResult scanResult : scanResults) {
                if (scanResult.SSID.isEmpty()) continue;
                accessPoints.add(scanResult.SSID);
                if (accessPoints.size() >= RangingRequest.getMaxPeers()) break;
            }

            myAdapter.notifyDataSetChanged();
            Log.i("Select the ADS-B device WiFi network");
        }
    }

    @Override
    public void onScanResultItemClick(String scanResult) {
        wifiName = scanResult;
        Log.d("onScanResultItemClick(): ssid: " + wifiName);

        // User has selected a Wifi SSID -- save it for future use, and connect to it
        Settings.save();
        binding.aplistview.setVisibility(View.INVISIBLE);
        Log.i("Connecting to " + wifiName);
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiConnection wifiConnection = new WifiConnection(connMgr, wifiName, "");
        Log.i("Starting Ping comms");
        PingComms.pingComms = new PingComms(port);
        // Configured, but not connected to Wifi, or connected to the wrong Wifi
        PingComms.pingComms.start();
    }

}
