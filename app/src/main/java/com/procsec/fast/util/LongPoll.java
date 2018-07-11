package com.procsec.fast.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.procsec.fast.vkapi.KException;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKMessage;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class LongPoll {

    public static final String TAG = "VKLongPollHelper";
    private static LongPoll instance = null;
    public boolean isRunning;
    String key;
    String server;
    Long ts;
    Long pts;
    private VKApi mApi;
    private Context mContext;
    private ArrayList<Listener> mListeners = null;
    private Object[] pollServer;

    private LongPoll(Context context, VKApi api) {
        this.mApi = api;
        this.mContext = context;
    }

    public static LongPoll get(Context context, VKApi api) {
        if (instance == null) {
            instance = new LongPoll(context, api);
        }
        return instance;
    }

    public synchronized void start() {
        if (!isRunning) {
            new VKLongPollTask().start();
            isRunning = true;
        }
    }

    public void stop() {
        isRunning = false;
        Log.w(TAG, "Stopped...");
    }

    @Deprecated
    public void setListener(Listener listener) {
        addListener(listener);
    }


    public void addListener(Listener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    private String getResponse() throws IOException {
        String request = "https://" + server
                + "?act=a_check&key=" + key
                + "&ts=" + ts +
                "&wait=25&mode=2";
        return VKApi.sendRequestInternal(request, "", false);
    }

    private void getServer() throws IOException, JSONException, KException {
        this.pollServer = mApi.getLongPollServer(null, null);
        this.key = (String) pollServer[0];
        this.server = (String) pollServer[1];
        this.ts = (Long) pollServer[2];
    }

    private void process(JSONArray updates) throws JSONException {
        for (int i = 0; i < updates.length(); i++) {
            JSONArray item = updates.optJSONArray(i);
            int type = item.optInt(0);

            switch (type) {
                case 3:
                    int id = item.optInt(1);
                    int mask = item.optInt(2);
                    messageClearFlags(id, mask);
                    break;
                case 4:
                    messageEvent(item);
                    break;

            }
        }
    }

    private void messageEvent(JSONArray item) throws JSONException {
        VKMessage message = VKMessage.parse(item);
        EventBus.getDefault().postSticky(message);
        Log.d(TAG, message.body);
    }

    private void messageClearFlags(int id, int mask) {
        if (VKMessage.isUnread(mask)) {
            EventBus.getDefault().post(id);
        }
    }

    private void processResponse(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            try {
                final JSONArray arrayItem = (JSONArray) array.get(i);
                int type = (Integer) arrayItem.get(0);

                if (!isRunning) break;
                /*

                 0,$message_id,0 — удаление сообщения с указанным local_id
                 1,$message_id,$flags — замена флагов сообщения (FLAGS:=$flags)
                 2,$message_id,$mask[,$user_id] — установка флагов сообщения (FLAGS|=$mask)
                 3,$message_id,$mask[,$user_id] — сброс флагов сообщения (FLAGS&=~$mask)
                 4,$message_id,$flags,$from_id,$timestamp,$subject,$text,$attachments — добавление нового сообщения
                 6,$peer_id,$local_id — прочтение всех входящих сообщений с $peer_id вплоть до $local_id включительно
                 7,$peer_id,$local_id — прочтение всех исходящих сообщений с $peer_id вплоть до $local_id включительно
                 8,-$user_id,$extra — друг $user_id стал онлайн, $extra не равен 0, если в mode был передан флаг 64, в младшем байте (остаток от деления на 256) числа $extra лежит идентификатор платформы (таблица ниже)
                 9,-$user_id,$flags — друг $user_id стал оффлайн ($flags равен 0, если пользователь покинул сайт (например, нажал выход) и 1, если оффлайн по таймауту (например, статус away))

                 51,$chat_id,$self — один из параметров (состав, тема) беседы $chat_id были изменены. $self - были ли изменения вызваны самим пользователем

                 61,$user_id,$flags — пользователь $user_id начал набирать текст в диалоге. событие должно приходить раз в ~5 секунд при постоянном наборе текста. $flags = 1
                 62,$user_id,$chat_id — пользователь $user_id начал набирать текст в беседе $chat_id.

                 70,$user_id,$call_id — пользователь $user_id совершил звонок имеющий идентификатор $call_id.
                 80,$count,0 — новый счетчик непрочитанных в левом меню стал равен $count.

                */

                switch (type) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        final VKMessage message = VKMessage.parse(arrayItem);

                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                if (mListeners != null)
                                    for (Listener l : mListeners) {
                                        l.onNewMessage(message);
                                    }
                            }
                        });
                        break;
                    case 8:

                        break;
                    case 9:

                        break;
                    case 51:
                        break;
                    case 61:
                        final long user_id = arrayItem.optLong(1);
                        final long flag = arrayItem.optLong(2);

                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                if (mListeners != null)
                                    for (Listener l : mListeners) {
                                        l.onUserTyping(user_id, flag);
                                    }
                            }
                        });
                        break;
                    case 62:
                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                if (mListeners != null)
                                    for (Listener l : mListeners) {
                                        l.onUserTypingInChat(arrayItem.optLong(1), arrayItem.optLong(2));
                                    }

                            }
                        });
                        break;
                    case 70:
                        break;

                    case 80:
                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                if (mListeners != null)
                                    for (Listener l : mListeners) {
                                        l.onChangeMessagesCount(arrayItem.optLong(1));
                                        ;
                                    }

                            }
                        });
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void runOnUi(Runnable action) {
        ((Activity) mContext).runOnUiThread(action);
    }

    public interface Listener {

        /**
         * Пришло новое сообщение
         */
        void onNewMessage(VKMessage message);

        /**
         * Пользователь начал набирать текст в диалоге
         */
        void onUserTyping(long user_id, long flag);

        /**
         * Пользователь начал набирать текст в чате
         */
        void onUserTypingInChat(long user_id, long chat_id);

        /**
         * Новое число счетчика сообщений
         */
        void onChangeMessagesCount(long newCount);


    }

    private class VKLongPollTask extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                if (!Utils.hasConnection(mContext)) {
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                try {
                    if (pollServer == null) {
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
                    e.printStackTrace();
                    pollServer = null;
                }

            }
        }
    }

}
