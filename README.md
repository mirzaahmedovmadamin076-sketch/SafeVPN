package com.safevpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

public class VpnService extends android.net.VpnService {

    private static final String TAG = "SafeVPN";
    private static final String CHANNEL_ID = "vpn_channel";
    private static final int NOTIFICATION_ID = 1;

    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;
    private boolean running = false;
    private String serverIp = "104.28.11.52";
    private String serverName = "Germaniya";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            serverIp   = intent.getStringExtra("server_ip");
            serverName = intent.getStringExtra("server_name");
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        startVpnTunnel();
        return START_STICKY;
    }

    private void startVpnTunnel() {
        running = true;
        vpnThread = new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("split_tunnel", MODE_PRIVATE);
                String appsJson = prefs.getString("apps", "[]");
                List<String> bypassPackages = getBypassPackages(appsJson);

                Builder builder = new Builder()
                    .setSession("SafeVPN")
                    .addAddress("10.0.0.2", 24)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500);

                for (String pkg : bypassPackages) {
                    try {
                        builder.addDisallowedApplication(pkg);
                        Log.d(TAG, "Bypass: " + pkg);
                    } catch (Exception e) {
                        Log.w(TAG, "Package not found: " + pkg);
                    }
                }

                vpnInterface = builder.establish();

                if (vpnInterface == null) {
                    Log.e(TAG, "VPN interfeysi yaratilmadi");
                    return;
                }

                FileInputStream  in  = new FileInputStream(vpnInterface.getFileDescriptor());
                FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

                byte[] packet = new byte[32767];
                while (running) {
                    int len = in.read(packet);
                    if (len > 0) {
                        out.write(packet, 0, len);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "VPN xatosi: " + e.getMessage());
            }
        });
        vpnThread.start();
    }

    private List<String> getBypassPackages(String json) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optBoolean("vpn", true)) {
                    list.add(obj.optString("packageName", ""));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parse xatosi: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (vpnThread != null) vpnThread.interrupt();
        try {
            if (vpnInterface != null) vpnInterface.close();
        } catch (IOException e) {
            Log.e(TAG, "Close error: " + e.getMessage());
        }
        super.onDestroy();
    }

    private Notification buildNotification() {
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeVPN Faol")
            .setContentText("Server: " + serverName + " | Himoyalangan")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "VPN Holati",
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
