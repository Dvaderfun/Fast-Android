package com.procsec.fast.common;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.procsec.fast.util.JSONParser;

import org.json.JSONObject;

import ru.lischenko_dev.fastmessenger.R;

public class OTAManager {

    public static final String VERSION_NAME = "DP 5";

    private static final String look_link = "https://stwtforever.000webhostapp.com/fast.json";

    public static void checkUpdate(final AppCompatActivity a) {
        if (false)
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        JSONObject root = JSONParser.parseObject(look_link, "utf-8");
                        String version_name = root.optString("version_name");

                        if (version_name.contains(VERSION_NAME)) {
                            return;
                        } else {
                            showUpdateDialog(version_name, a);
                        }
                    } catch (Exception e) {
                        Log.e("Ща будет крашлог", "");
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    private static void showUpdateDialog(final String vers, final AppCompatActivity a) {
        AlertDialog.Builder adb = new AlertDialog.Builder(a);

        View layout = LayoutInflater.from(a).inflate(R.layout.update_screen, null, false);
        adb.setView(layout);

        final AlertDialog ad = adb.create();
        ad.show();

        TextView version = layout.findViewById(R.id.version);

        Button download = layout.findViewById(R.id.download);
        Button cancel = layout.findViewById(R.id.cancel);

        download.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://stwtforever.000webhostapp.com/" + vers + ".apk"));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                a.startActivity(i);
            }

        });

        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                ad.dismiss();
            }

        });

        String ver = a.getString(R.string.version_new);
        version.setText(ver + " " + vers);

    }
}
