package com.safevpn;

import android.content.SharedPreferences;
import android.content.pm.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;
import java.util.*;

public class SplitTunnelActivity extends AppCompatActivity {

    private LinearLayout appListContainer;
    private SharedPreferences prefs;
    private List<AppItem> appItems = new ArrayList<>();

    static class AppItem {
        String name, packageName;
        boolean useVpn;
        AppItem(String n, String p, boolean v) { name=n; packageName=p; useVpn=v; }
    }

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_tunnel);

        prefs = getSharedPreferences("split_tunnel", MODE_PRIVATE);
        appListContainer = findViewById(R.id.appListContainer);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Split Tunneling");

        loadApps();
        renderList();
    }

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_tunnel);

        prefs = getSharedPreferences("split_tunnel", MODE_PRIVATE);
        appListContainer = findViewById(R.id.appListContainer);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Split Tunneling");

        loadApps();
        renderList();
    }

appItems.clear();
        for (ApplicationInfo info : installedApps) {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String name = pm.getApplicationLabel(info).toString();
                boolean useVpn = savedSettings.getOrDefault(info.packageName, true);
                appItems.add(new AppItem(name, info.packageName, useVpn));
            }
        }

        Collections.sort(appItems, (a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private void renderList() {
        appListContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < appItems.size(); i++) {
            final int idx = i;
            AppItem app = appItems.get(i);


      View row = inflater.inflate(R.layout.item_split_app, appListContainer, false);
            TextView tvName    = row.findViewById(R.id.tvAppName);
            TextView tvPackage = row.findViewById(R.id.tvPackageName);
            Switch   swVpn     = row.findViewById(R.id.swVpn);
            TextView tvBadge   = row.findViewById(R.id.tvBadge);

            tvName.setText(app.name);
            tvPackage.setText(app.packageName);
            swVpn.setChecked(app.useVpn);
            updateBadge(tvBadge, app.useVpn);

            swVpn.setOnCheckedChangeListener((btn, checked) -> {
                appItems.get(idx).useVpn = checked;
                updateBadge(tvBadge, checked);
                saveSettings();
            });

            appListContainer.addView(row);

      }
    }

    private void updateBadge(TextView tv, boolean vpn) {
        if (vpn) {
            tv.setText("VPN orqali");
            tv.setTextColor(0xFF185FA5);
            tv.setBackgroundColor(0xFFE6F1FB);
        } else {
            tv.setText("To'g'ridan");
            tv.setTextColor(0xFF854F0B);
            tv.setBackgroundColor(0xFFFAEEDA);
        }
    }

private void saveSettings() {
        JSONArray arr = new JSONArray();
        try {
            for (AppItem item : appItems) {
                JSONObject o = new JSONObject();
                o.put("name", item.name);
                o.put("packageName", item.packageName);
                o.put("vpn", item.useVpn);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        prefs.edit().putString("apps", arr.toString()).apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
