package com.procsec.fast.service;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import com.procsec.fast.util.*;
import com.procsec.fast.vkapi.*;
import com.procsec.fast.vkapi.model.*;
import java.io.*;
import org.greenrobot.eventbus.*;
import org.json.*;

public class LongPollService extends Service {

    public boolean isRunning;
    private Thread updateThread;
    public static final String TAG = "VKLongPollHelper";
    private String key;
    private String server;
    private Long ts;
    private Object[] pollServer;
	private boolean error;

    public LongPollService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        updateThread = new Thread(new MessageUpdater());
        updateThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class MessageUpdater implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
				if (VKApi.getAccount().id == 0) {
					try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
				}
                if (!Utils.hasConnection(LongPollService.this)) {
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
				if (error) {
					try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
					error = false;
                    continue;
				}
                try {
                    if (server == null) {
                        getServer();
                    }
                    String response = getResponse();
                    JSONObject root = new JSONObject(response);

                    Long tsResponse = root.optLong("ts");
                    JSONArray updates = root.getJSONArray("updates");

                    ts = tsResponse;
                    if (updates.length() != 0) {
                        process(updates);
                    }
                } catch (Exception e) {
					Log.e("Fast LongPoll", "Error:");
                    e.printStackTrace();
                    server = null;
					error = true;
					continue;
                }

            }
        }

        private String getResponse() throws IOException {
            String request = "https://" + server
                    + "?act=a_check&key=" + key
                    + "&ts=" + ts +
                    "&wait=25&mode=2";
            return VKApi.sendRequestInternal(request, "", false);
        }


        private void messageEvent(JSONArray item) throws JSONException {
            VKMessage message = VKMessage.parse(item);
            EventBus.getDefault().postSticky(message);
        }

        private void messageClearFlags(long id, int mask) {
            if (VKMessage.isUnread(mask)) {
                EventBus.getDefault().post(id);
            }
        }

        private void getServer() throws IOException, JSONException, KException {
            pollServer = VKApi.getLongPollServer(null, null);
            key = (String) pollServer[0];
            server = (String) pollServer[1];
            ts = (Long) pollServer[2];
        }

        private void process(JSONArray updates) throws JSONException {
            if (updates.length() == 0) {
                return;
            }

            for (int i = 0; i < updates.length(); i++) {
                JSONArray item = updates.optJSONArray(i);
                int type = item.optInt(0);

                switch (type) {
                    case 3:
                        long id = item.optInt(1);
                        int mask = item.optInt(2);
                        messageClearFlags(id, mask);
                        break;

                    case 4:
                        messageEvent(item);
                        break;
                }
            }
        }

    }
}
