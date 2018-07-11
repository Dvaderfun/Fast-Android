package com.procsec.fast.vkapi;

import android.util.Log;

import com.procsec.fast.util.Account;
import com.procsec.fast.util.Constants;
import com.procsec.fast.vkapi.model.VKAudio;
import com.procsec.fast.vkapi.model.VKChat;
import com.procsec.fast.vkapi.model.VKDocument;
import com.procsec.fast.vkapi.model.VKGroup;
import com.procsec.fast.vkapi.model.VKMessage;
import com.procsec.fast.vkapi.model.VKMessageAttachment;
import com.procsec.fast.vkapi.model.VKPhoto;
import com.procsec.fast.vkapi.model.VKStatus;
import com.procsec.fast.vkapi.model.VKUser;
import com.procsec.fast.vkapi.model.VKVideo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class VKApi {

    public static final String TAG = "Kate.Api";
    public static final String BASE_URL = "https://api.vk.com/method/";
    public static final String API_VERSION = "5.80";
    private static final int MAX_TRIES = 3;
    public static String access_token;
    public static String api_id;
    static String language = Locale.getDefault().getLanguage();
    static boolean enable_compression = true;
    private static VKApi mSingleton;
    private static Account account;

    public VKApi(String access_token, String api_id) {
        this.access_token = access_token;
        this.api_id = api_id;
    }

    public static VKApi init(Account n_account) {
        account = n_account;
        return new VKApi(n_account.token, Constants.API_ID);
    }

    public static Account getAccount() {
        if (account == null) account = new Account();
        account.restore();
        return account;
    }

    public static synchronized VKApi init(String access_token, String api_id) {
        if (mSingleton == null) {
            mSingleton = new VKApi(access_token, Constants.API_ID);
        }
        return mSingleton;
    }

    private static void checkError(JSONObject root, String url) throws JSONException, KException {
        if (!root.isNull("error")) {
            JSONObject error = root.getJSONObject("error");
            int code = error.getInt("error_code");
            String message = error.getString("error_msg");
            KException e = new KException(code, message, url);
            if (code == 14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            if (code == 17)
                e.redirect_uri = error.optString("redirect_uri");
            throw e;
        }
        if (!root.isNull("execute_errors")) {
            JSONArray errors = root.getJSONArray("execute_errors");
            if (errors.length() == 0)
                return;
            //only first error is processed if there are multiple
            JSONObject error = errors.getJSONObject(0);
            int code = error.getInt("error_code");
            String message = error.getString("error_msg");
            KException e = new KException(code, message, url);
            if (code == 14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            if (code == 17)
                e.redirect_uri = error.optString("redirect_uri");
            throw e;
        }
    }

    public static JSONObject sendRequest(VKParams params) throws IOException, JSONException, KException {
        return sendRequest(params, false);
    }

    private static JSONObject sendRequest(VKParams params, boolean is_post) throws IOException, JSONException, KException {
        String url = getSignedUrl(params, is_post);
        String body = "";
        if (is_post)
            body = params.getParamsString();
        Log.i(TAG, "url=" + url);
        if (body.length() != 0)
            Log.i(TAG, "body=" + body);
        String response = "";
        for (int i = 1; i <= MAX_TRIES; ++i) {
            try {
                if (i != 1)
                    Log.i(TAG, "try " + i);
                response = sendRequestInternal(url, body, is_post);
                break;
            } catch (IOException ex) {
                processNetworkException(i, ex);
            }
        }
        Log.i(TAG, "response=" + response);
        JSONObject root = new JSONObject(response);
        checkError(root, url);
        return root;
    }


    private static void processNetworkException(int i, IOException ex) throws IOException {
        ex.printStackTrace();
        if (i == MAX_TRIES)
            throw ex;
    }

    public static String sendRequestInternal(String url, String body, boolean is_post) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setDoOutput(is_post);
            connection.setDoInput(true);
            connection.setRequestMethod(is_post ? "POST" : "GET");
            // connection.setRequestMethod("POST");
            if (enable_compression)
                connection.setRequestProperty("Accept-Encoding", "gzip");
            if (is_post)
                connection.getOutputStream().write(body.getBytes("UTF-8"));
            int code = connection.getResponseCode();
            Log.i(TAG, "code=" + code);
            //It may happen due to keep-alive problem http://stackoverflow.com/questions/1440957/httpurlconnection-getresponsecode-returns-1-on-second-invocation
            if (code == -1)
                throw new WrongResponseCodeException("Network error");
            //может стоит проверить на код 200
            //on error can also read error stream from connection.
            InputStream is = new BufferedInputStream(connection.getInputStream(), 8192);
            String enc = connection.getHeaderField("Content-Encoding");
            if (enc != null && enc.equalsIgnoreCase("gzip"))
                is = new GZIPInputStream(is);
            return VKUtils.convertStreamToString(is);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    private static String getSignedUrl(VKParams params, boolean is_post) {
        params.put("access_token", getAccount().token);
        if (!params.contains("v"))
            params.put("v", API_VERSION);
        if (!params.contains("lang"))
            params.put("lang", language);

        String args = "";
        if (!is_post)
            args = params.getParamsString();

        return BASE_URL + params.method_name + "?" + args;
    }

    public static String unescape(String text) {
        if (text == null)
            return null;
        return text.replace("&amp;", "&").replace("&quot;", "\"").replace("<br>", "\n").replace("&gt;", ">").replace("&lt;", "<")
                .replace("<br/>", "\n").replace("&ndash;", "-").trim();
        //Баг в API
        //amp встречается в сообщении, br в Ответах тип comment_photo, gt lt на стене - баг API, ndash в статусе когда аудио транслируется
        //quot в тексте сообщения из LongPoll - то есть в уведомлении
    }

    public static VKApi get() {
        return mSingleton;
    }


    private static void addCaptchaParams(String captcha_key, String captcha_sid, VKParams params) {
        params.put("captcha_sid", captcha_sid);
        params.put("captcha_key", captcha_key);
    }

    static String idsToString(Integer[] ids) {
        String result = "";

        for (int i = 0; i < ids.length; i++) {
            result += ids[i] + ((i + 1) < ids.length ? "," : "");
        }

        return result;
    }

    static <T> String arrayToString(Collection<T> items) {
        if (items == null)
            return null;
        String str_cids = "";
        for (Object item : items) {
            if (str_cids.length() != 0)
                str_cids += ',';
            str_cids += item;
        }
        return str_cids;
    }

    public static ArrayList<VKUser> getProfiles(Collection<Integer> ids, String fields) throws IOException, JSONException, KException {
        VKParams params = new VKParams("users.get");
        params.put("user_ids", arrayToString(ids));
        params.put("fields", fields);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return VKUser.parseUsers(array);
    }

    public static VKUser getProfile(int user_id) throws KException, JSONException, IOException {
        ArrayList<Integer> id = new ArrayList<>();
        id.add(user_id);
        return getProfiles(id, VKUser.FIELDS_DEFAULT).get(0);
    }

    public static ArrayList<VKUser> getFriendsFull(int user_id, String fields, String order, Integer lid, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.get");
        params.put("fields", fields);
        params.put("user_id", user_id);
        params.put("list_id", lid);
        params.put("order", order);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKUser> users = new ArrayList<>();

        if (array == null)
            return users;
        int category_count = array.length();
        for (int i = 0; i < category_count; ++i) {
            JSONObject o = (JSONObject) array.get(i);
            VKUser u = VKUser.parse(o);
            users.add(u);
        }
        return users;
    }

    public static ArrayList<VKUser> getFriends(long user_id, String order, Integer lid, String captcha_key, String captcha_sid) throws JSONException, IOException, KException {
        VKParams params = new VKParams("friends.get");
        params.put("user_id", user_id);
        params.put("fields", VKUser.FIELDS_DEFAULT);
        params.put("list_id", lid);
        params.put("order", order);

        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");

        return VKUser.parseUsers(array);
    }

    public static ArrayList<Long> getOnlineFriends(Long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.getOnline");
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        ArrayList<Long> users = new ArrayList<Long>();
        if (array != null) {
            int category_count = array.length();
            for (int i = 0; i < category_count; ++i) {
                Long id = array.optLong(i, -1);
                if (id != -1) users.add(id);
            }
        }
        return users;
    }

    public static ArrayList<Long> getMutual(Long target_uid, Long source_uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.getMutual");
        params.put("target_uid", target_uid);
        params.put("source_uid", source_uid);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        ArrayList<Long> users = new ArrayList<Long>();
        if (array != null) {
            int category_count = array.length();
            for (int i = 0; i < category_count; ++i) {
                Long id = array.optLong(i, -1);
                if (id != -1)
                    users.add(id);
            }
        }
        return users;
    }

    public static ArrayList<VKMessageAttachment> getHistoryAttachments(Long peer_id, String media_type, Integer offset, Integer count, Boolean photo_sizes) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getHistoryAttachments");
        params.put("peer_id", peer_id);
        params.put("media_type", media_type);
        params.put("offset", offset);
        params.put("count", count);
        params.put("count", count);
        params.put("photo_sizes", photo_sizes);

        JSONObject request = sendRequest(params);
        JSONObject response = request.optJSONObject("response");
        return VKMessageAttachment.parseArray(response.optJSONArray("items"), media_type, response.optInt("next_from"));
    }

    public static ArrayList<VKPhoto> getPhotos(Long uid, Long aid, Integer offset, Integer count, boolean rev) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.get");
        params.put("owner_id", uid);
        params.put("album_id", aid);
        params.put("extended", "1");
        params.put("offset", offset);
        params.put("limit", count);
        if (rev)
            params.put("rev", 1);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        if (array == null)
            return new ArrayList<VKPhoto>();
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/photos.getUserPhotos
    public static ArrayList<VKPhoto> getUserPhotos(Long uid, Integer offset, Integer count) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getUserPhotos");
        params.put("user_id", uid);
        params.put("sort", "0");
        params.put("count", count);
        params.put("offset", offset);
        params.put("extended", 1);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        if (array == null)
            return new ArrayList<VKPhoto>();
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/photos.getAll
    public static ArrayList<VKPhoto> getAllPhotos(Long owner_id, Integer offset, Integer count, boolean extended) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getAll");
        params.put("owner_id", owner_id);
        params.put("offset", offset);
        params.put("count", count);
        params.put("extended", extended ? 1 : 0);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        if (array == null)
            return new ArrayList<VKPhoto>();
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    public static ArrayList<VKMessage> getMessages(long time_offset, int filters, boolean is_out, int count) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.get");
        if (is_out)
            params.put("out", "1");
        if (time_offset != 0)
            params.put("time_offset", time_offset);
        if (count != 0)
            params.put("count", count);
        if (filters != 0)
            params.put("filters", filters);

        params.put("preview_length", "0");
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKMessage> messages = parseMessages(array, false, 0, false, 0);
        return messages;
    }

    public static ArrayList<VKMessage> getMessagesHistory(long peer_id, boolean rev, int offset, int count) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getHistory");
        params.put("peer_id", peer_id);
        params.put("offset", offset);
        params.put("rev", rev ? 1 : 0);
        if (count > 0)
            params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return VKMessage.parseArray(array);
    }

    //http://vk.com/dev/messages.getDialogs
    public static ArrayList<VKMessage> getMessagesDialogs(long offset, int count, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getDialogs");
        //params.put("access_token", getAccount().access_token);
        if (offset != 0)
            params.put("offset", offset);
        if (count != 0)
            params.put("count", count);
        params.put("preview_length", "50");
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, false);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKMessage> messages = parseMessages(array, false, 0, false, 0);
        return messages;
    }

    //http://vk.com/dev/messages.getLongPollServer
    public static Object[] getLongPollServer(String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getLongPollServer");
        params.put("need_pts", true);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        String key = response.getString("key");
        String server = response.getString("server");
        Long ts = response.getLong("ts");
        Long pts = response.getLong("pts");
        return new Object[]{key, server, ts, pts};
    }

    // https://vk.com/dev/messages.getLongPollHistory
    public static ArrayList<VKMessage> getLongPollHistory(long ts, Long pts, long preview_length, boolean onlines, Long events_limit, Long msgs_limit, Long max_msg_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("messages.getLongPollHistory");
        params.put("ts", ts);
        params.put("pts", pts);
        params.put("preview_length", preview_length);
        params.put("onlines", onlines);
        params.put("events_limit", events_limit);
        params.put("msgs_limit", msgs_limit);
        params.put("max_msg_id", max_msg_id);

        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONObject messages = response.optJSONObject("messages");

        return VKMessage.parseArray(messages.optJSONArray("items"));
    }

    //http://vk.com/dev/messages.setActivity
    public static Integer setMessageActivity(Long uid, Long chat_id, boolean typing) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.setActivity");
        params.put("user_id", uid);
        params.put("chat_id", chat_id);
        if (typing)
            params.put("type", "typing");
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    private static ArrayList<VKMessage> parseMessages(JSONArray array, boolean from_history, long history_uid, boolean from_chat, long me) throws JSONException {
//        ArrayList<VKMessage> messages = new ArrayList<VKMessage>();
//        if (array != null) {
//            int category_count = array.length();
//            for (int i = 0; i < category_count; ++i) {
//                JSONObject o = array.getJSONObject(i);
////                VKMessage m = VKMessage.parseArray(o, from_history, history_uid, from_chat, me);
//                VKMessage m = VKMessage.parseArray(o);
//                messages.add(m);
//            }
//        }
        return VKMessage.parseArray(array);
    }

    //http://vk.com/dev/messages.send
    public static int sendMessage(int uid, int chat_id, String message, String title, String type, Collection<String> attachments, ArrayList<Long> forward_messages, String lat, String lon, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.send");
        if (chat_id <= 0)
            params.put("user_id", uid);
        else
            params.put("chat_id", chat_id);
        params.put("message", message);
        params.put("title", title);
        params.put("type", type);
        params.put("attachment", arrayToString(attachments));
        params.put("forward_messages", arrayToString(forward_messages));
        params.put("lat", lat);
        params.put("long", lon);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, true);
        return root.optInt("response");
    }

    public static Long markAsRead(ArrayList<Long> mids, Long peer_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("messages.markAsRead");
        params.put("message_ids", arrayToString(mids));
        params.put("peer_id", peer_id);
        JSONObject root = sendRequest(params);
        Long response = root.optLong("response");
        return response;
    }

    public static ArrayList<Long> markAsImportant(ArrayList<Long> mids, boolean important) throws JSONException, IOException, KException {
        VKParams params = new VKParams("messages.markAsImportant");
        params.put("message_ids", arrayToString(mids));
        params.put("important", important);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        ArrayList<Long> message_ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            message_ids.add(array.optLong(i));
        }
        return message_ids;
    }

    //http://vk.com/dev/messages.delete
    public static Integer deleteMessage(Collection<Integer> message_ids, boolean every) throws IOException, JSONException, KException {
        if (message_ids.isEmpty()) return null;

        VKParams params = new VKParams("messages.delete");
        params.put("message_ids", arrayToString(message_ids));
        JSONObject root = sendRequest(params);
        //не парсим ответ - там приходят отдельные флаги для каждого удалённого сообщения
        // TODO: Может быть, это так, но у меня возвращает 1
        return root.optInt("response", -1);
    }

    // Получение 1.5к сообщений в истории перписки
    public static ArrayList<VKMessage> getMessagesHistoryWithExecute(Long user_id, Long chat_id, long me, long offset) throws JSONException, IOException, KException {
        String var;
        if (chat_id != 0) {
            var = "\"chat_id\":" + chat_id + ",\n" +
                    "\"user_id\":" + 0 + ",\n";
        } else var = "\"user_id\":" + user_id + ",\n";

        String code =
                "var offset = " + offset + ";\n" +
                        "var v = " + API_VERSION + ";\n" +
                        "var a = API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset, \n" +
                        "\"count\":200, \n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var b =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 200, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var c =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 400, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var d =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 600, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var e =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 800, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var k =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 1000, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var j =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 1200, \n" +
                        "\"count\":100,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "var i =  API.messages.getHistory({\n" +
                        var +
                        "\"offset\":offset + 1300, \n" +
                        "\"count\":200,\n" +
                        "\"v\":v\n" +
                        "});\n" +
                        "\n" +
                        "return a.items + b.items + c.items + d.items + e.items + k.items + j.items + i.items;";

        VKParams params = new VKParams("execute");
        params.put("code", code);
        JSONObject root = sendRequest(params);
        JSONArray response = root.optJSONArray("response");
        return parseMessages(response, chat_id <= 0, user_id, chat_id > 0, me);
    }

    /**
     * for status**
     */
    //http://vk.com/dev/status.get
    public static VKStatus getStatus(int uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("status.get");
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        JSONObject obj = root.optJSONObject("response");
        VKStatus status = new VKStatus();
        if (obj != null) {
            status.text = unescape(obj.getString("text"));
            JSONObject jaudio = obj.optJSONObject("audio");
            if (jaudio != null)
                status.audio = VKAudio.parse(jaudio);
        }
        return status;
    }

    //http://vk.com/dev/status.set
    public static String setStatus(String status_text) throws IOException, JSONException, KException {
        VKParams params = new VKParams("status.set");
        params.put("text", status_text);
        JSONObject root = sendRequest(params);
        Object response_id = root.opt("response");
        if (response_id != null)
            return String.valueOf(response_id);
        return null;
    }

    public static ArrayList<VKAudio> getAudio(Long owner_id, Long album_id, long count, long offset, Collection<Long> aids, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.get");
        if (owner_id != null)
            params.put("owner_id", owner_id);
        params.put("audio_ids", arrayToString(aids));//не документировано - возможно уже не работает - возможно нужно использовать audio.getById
        params.put("album_id", album_id);
        params.put("count", count);
        params.put("offset", offset);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return parseAudioList(array);
    }

    // http://vk.com/dev/audio.getCount
    public static Long getAudioCount(Long owner_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("audio.getCount");
        params.put("owner_id", owner_id);

        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    //http://vk.com/dev/audio.getById
    public static ArrayList<VKAudio> getAudioById(String audios, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.getById");
        params.put("audios", audios);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array);
    }

    //http://vk.com/dev/audiou.getLyrics
    public static String getLyricsAudio(Long id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.getLyrics");
        params.put("lyrics_id", id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return response.optString("text");
    }

    //http://vk.com/dev/audio.search
    public static ArrayList<VKAudio> searchAudio(String q, String sort, String lyrics, Long count, Long offset, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.search");
        params.put("q", q);
        params.put("sort", sort);
        params.put("lyrics", lyrics);
        params.put("count", count);
        params.put("offset", offset);
        params.put("auto_complete", "1");
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return parseAudioList(array);
    }

    //http://vk.com/dev/audio.delete
    public static String deleteAudio(Long aid, Long oid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.delete");
        params.put("audio_id", aid);
        params.put("album_id", aid);//Баг в Api - это лишний параметр
        params.put("owner_id", oid);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    //http://vk.com/dev/audio.add
    public static String addAudio(Long aid, Long oid, Long gid, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.add");
        params.put("audio_id", aid);
        params.put("owner_id", oid);
        params.put("group_id", gid);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    // http://vk.com/dev/audio.restore
    // TODO: Если время хранения удаленной аудиозаписи истекло (обычно это 20 минут), сервер вернет ошибку 202 (Cache expired).
    public static VKAudio restoreAudio(Long audio_id, Long owner_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("audio.restore");
        params.put("audio_id", audio_id);
        params.put("owner_id", owner_id);

        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return VKAudio.parse(response);
    }

    private static ArrayList<VKAudio> parseAudioList(JSONArray array) throws JSONException {
        ArrayList<VKAudio> audios = new ArrayList<VKAudio>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) { //get(0) is integer, it is audio count
                JSONObject o = (JSONObject) array.get(i);
                audios.add(VKAudio.parse(o));
            }
        }
        return audios;
    }

    /**
     * for video **
     */
    //http://vk.com/dev/video.get
    public static ArrayList<VKVideo> getVideo(String videos, Long owner_id, Long album_id, String width, Long count, Long offset, String access_key) throws IOException, JSONException, KException {
        VKParams params = new VKParams("video.get");
        params.put("videos", videos);
        params.put("owner_id", owner_id);
        params.put("width", width);
        params.put("count", count);
        params.put("offset", offset);
        params.put("album_id", album_id);
        params.put("access_key", access_key);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        ArrayList<VKVideo> videoss = new ArrayList<VKVideo>();
        if (response == null)
            return videoss;
        JSONArray array = response.optJSONArray("items");
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject o = (JSONObject) array.get(i);
                VKVideo video = VKVideo.parse(o);
                videoss.add(video);
            }
        }
        return videoss;
    }

    //http://vk.com/dev/video.getUserVideos
    public static ArrayList<VKVideo> getUserVideo(Long user_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("video.getUserVideos");
        params.put("user_id", user_id);
        params.put("count", "50");
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKVideo> videos = new ArrayList<VKVideo>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject o = (JSONObject) array.get(i);
                videos.add(VKVideo.parse(o));
            }
        }
        return videos;
    }

    //http://vk.com/dev/photos.getUploadServer
    public static String getPhotoUploadServer(long album_id, Long group_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getUploadServer");
        params.put("album_id", album_id);
        params.put("group_id", group_id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/photos.getWallUploadServer
    public static String getWallUploadServer(Long user_id, Long group_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getWallUploadServer");
        params.put("user_id", user_id);
        params.put("group_id", group_id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/audio.getUploadServer
    public static String getAudioUploadServer() throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.getUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/photos.getMessagesUploadServer
    public static String getPhotoMessageUploadServer() throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getMessagesUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/photos.getProfileUploadServer
    public static String getPhotoProfileUploadServer() throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getProfileUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/photos.save
    public static ArrayList<VKPhoto> savePhoto(String server, String photos_list, Long aid, Long group_id, String hash, String caption) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.save");
        params.put("server", server);
        params.put("photos_list", photos_list);
        params.put("album_id", aid);
        params.put("group_id", group_id);
        params.put("hash", hash);
        params.put("caption", caption);
        JSONObject root = sendRequest(params);
        JSONArray array = root.getJSONArray("response");
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/photos.saveWallPhoto
    public static ArrayList<VKPhoto> saveWallPhoto(String server, String photo, String hash, Long user_id, Long group_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.saveWallPhoto");
        params.put("server", server);
        params.put("photo", photo);
        params.put("hash", hash);
        params.put("user_id", user_id);
        params.put("group_id", group_id);
        JSONObject root = sendRequest(params);
        JSONArray array = root.getJSONArray("response");
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/audio.save
    public static VKAudio saveAudio(String server, String audio, String hash, String artist, String title) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.save");
        params.put("server", server);
        params.put("audio", audio);
        params.put("hash", hash);
        params.put("artist", artist);
        params.put("title", title);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return VKAudio.parse(response);
    }

    //http://vk.com/dev/photos.saveMessagesPhoto
    public static ArrayList<VKPhoto> saveMessagesPhoto(String server, String photo, String hash) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.saveMessagesPhoto");
        params.put("server", server);
        params.put("photo", photo);
        params.put("hash", hash);
        JSONObject root = sendRequest(params);
        JSONArray array = root.getJSONArray("response");
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/photos.saveProfilePhoto
    public static String[] saveProfilePhoto(String server, String photo, String hash) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.saveProfilePhoto");
        params.put("server", server);
        params.put("photo", photo);
        params.put("hash", hash);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        String src = response.optString("photo_src");
        String hash1 = response.optString("photo_hash");
        String[] res = new String[]{src, hash1};
        return res;
    }

    private static ArrayList<VKPhoto> parsePhotos(JSONArray array) throws JSONException {
        ArrayList<VKPhoto> photos = new ArrayList<VKPhoto>();
        int category_count = array.length();
        for (int i = 0; i < category_count; ++i) {
            JSONObject o = (JSONObject) array.get(i);
            VKPhoto p = new VKPhoto(o);
            photos.add(p);
        }
        return photos;
    }

    public static ArrayList<VKPhoto> getPhotosById(String photos, Integer extended, Integer photo_sizes) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.getById");
        params.put("photos", photos);
        params.put("extended", extended);
        params.put("photo_sizes", photo_sizes);
        JSONObject root = sendRequest(params);
        JSONArray response = root.optJSONArray("response");
        if (response == null)
            return new ArrayList<VKPhoto>();
        ArrayList<VKPhoto> photos1 = parsePhotos(response);
        return photos1;
    }

    //http://vk.com/dev/groups.get
    public static ArrayList<VKGroup> getGroups(int user_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.get");
        params.put("extended", "1");
        params.put("user_id", user_id);
        JSONObject root = sendRequest(params);
        ArrayList<VKGroup> groups = new ArrayList<VKGroup>();
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        //if there are no groups "response" will not be array
        if (array == null)
            return groups;
        //   groups = VKGroup.(array);//TODO: make array parse method
        return groups;
    }

    public static VKGroup getGroup(int id) throws JSONException, IOException, KException {
        ArrayList uid = new ArrayList();
        uid.add(id);
        ArrayList<VKGroup> apiGroup = getGroupsById(uid, null, "screen_name,members_count,status");
        for (VKGroup group : apiGroup) {
            return group;
        }
        return null;
    }

    //http://vk.com/dev/video.search
    public static ArrayList<VKVideo> searchVideo(String q, String sort, String hd, Long count, Long offset, Integer adult, String filters) throws IOException, JSONException, KException {
        VKParams params = new VKParams("video.search");
        params.put("q", q);
        params.put("sort", sort);
        params.put("hd", hd);
        params.put("count", count);
        params.put("offset", offset);
        params.put("adult", adult);     //safe search: 1 - disabled, 0 - enabled
        params.put("filters", filters); //mp4, youtube, vimeo, short, long
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKVideo> videoss = new ArrayList<VKVideo>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject o = (JSONObject) array.get(i);
                VKVideo video = VKVideo.parse(o);
                videoss.add(video);
            }
        }
        return videoss;
    }

    //http://vk.com/dev/account.setOnline
    public static void setOnline(String captcha_key, String captcha_sid) {
        VKParams params = new VKParams("account.setOnline");
        addCaptchaParams(captcha_key, captcha_sid, params);
        try {
            sendRequest(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //http://vk.com/dev/friends.add
    public static long addFriend(Long uid, String text, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.add");
        params.put("user_id", uid);
        params.put("text", text);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    //http://vk.com/dev/friends.delete
    public static long deleteFriend(Long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.delete");
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    //http://vk.com/dev/friends.getRequests
    public static ArrayList<Object[]> getRequestsFriends(Integer out) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.getRequests");
        params.put("need_messages", "1");
        params.put("out", out);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<Object[]> users = new ArrayList<Object[]>();
        if (array != null) {
            int category_count = array.length();
            for (int i = 0; i < category_count; ++i) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    Long id = item.optLong("user_id", -1);
                    if (id != -1) {
                        Object[] u = new Object[2];
                        u[0] = id;
                        u[1] = item.optString("message");
                        users.add(u);
                    }
                }
            }
        }
        return users;
    }

    //http://vk.com/dev/users.getSubscriptions
    public static ArrayList<Long> getSubscriptions(Long uid, int offset, int count, Integer extended) throws IOException, JSONException, KException {
        VKParams params = new VKParams("users.getSubscriptions");
        params.put("user_id", uid);
        //params.put("extended", extended); //TODO
        if (offset > 0)
            params.put("offset", offset);
        if (count > 0)
            params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        JSONObject jusers = response.optJSONObject("users");
        JSONArray array = jusers.optJSONArray("items");
        ArrayList<Long> users = new ArrayList<Long>();
        if (array != null) {
            int category_count = array.length();
            for (int i = 0; i < category_count; ++i) {
                Long id = array.optLong(i, -1);
                if (id != -1)
                    users.add(id);
            }
        }
        return users;
    }

    //http://vk.com/dev/users.getFollowers
    public static ArrayList<VKUser> getFollowers(Long uid, int offset, int count, String fields, String name_case) throws IOException, JSONException, KException {
        VKParams params = new VKParams("users.getFollowers");
        params.put("user_id", uid);
        if (offset > 0)
            params.put("offset", offset);
        if (count > 0)
            params.put("count", count);

        // if this method is called without fields it will return just ids in wrong format
        if (fields == null)
            fields = "first_name,last_name, photo_100, online";
        params.put("fields", fields);
        params.put("name_case", name_case);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return VKUser.parseUsers(array);
    }

    //http://vk.com/dev/messages.deleteDialog
    public static int deleteMessageDialog(int uid, long chatId) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.deleteDialog");
        params.put("user_id", uid);
        params.put("chat_id", chatId);
        JSONObject root = sendRequest(params);
        return root.getInt("response");
    }

    //http://vk.com/dev/execute
    public static void execute(String code) throws IOException, JSONException, KException {
        VKParams params = new VKParams("execute");
        params.put("code", code);
        sendRequest(params);
    }

    //http://vk.com/dev/photos.delete
    public static boolean deletePhoto(Long owner_id, Long photo_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.delete");
        params.put("owner_id", owner_id);
        params.put("photo_id", photo_id);
        JSONObject root = sendRequest(params);
        long response = root.optLong("response", -1);
        return response == 1;
    }

    //http://vk.com/dev/polls.addVote
    public static int addPollVote(long poll_id, long answer_id, long owner_id, long topic_id, String captcha_key, String captcha_sid) throws JSONException, IOException, KException {
        VKParams params = new VKParams("polls.addVote");
        params.put("owner_id", owner_id);
        params.put("poll_id", poll_id);
        if (topic_id != 0)
            params.put("board", topic_id);
        params.put("answer_id", answer_id);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        return root.getInt("response");
    }

    //http://vk.com/dev/polls.getById

    //http://vk.com/dev/polls.deleteVote
    public static int deletePollVote(long poll_id, long answer_id, long owner_id, long topic_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("polls.deleteVote");
        params.put("owner_id", owner_id);
        params.put("poll_id", poll_id);
        if (topic_id != 0)
            params.put("board", topic_id);
        params.put("answer_id", answer_id);
        JSONObject root = sendRequest(params);
        return root.getInt("response");
    }

    //http://vk.com/dev/polls.getVoters
    public static ArrayList<VKUser> getPollVoters(long poll_id, long owner_id, Collection<Long> answer_ids, Long count, Long offset, String fields, long topic_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("polls.getVoters");
        params.put("owner_id", owner_id);
        params.put("poll_id", poll_id);
        if (topic_id != 0)
            params.put("board", topic_id);
        params.put("answer_ids", arrayToString(answer_ids));
        params.put("count", count);
        params.put("offset", offset);
        params.put("fields", fields);
        JSONObject root = sendRequest(params);
        JSONArray response = root.optJSONArray("response");//массив ответов
        JSONObject object = (JSONObject) response.get(0);
        JSONObject array2 = object.optJSONObject("users");
        JSONArray array = array2.optJSONArray("items");
        //TODO for others answer_ids
        return VKUser.parseUsers(array);
    }

    //http://vk.com/dev/video.save
    public static String saveVideo(String name, String description, Long gid, int privacy_view, int privacy_comment) throws IOException, JSONException, KException {
        VKParams params = new VKParams("video.save");
        params.put("name", name);
        params.put("description", description);
        params.put("group_id", gid);
        if (privacy_view > 0)
            params.put("privacy_view", privacy_view);
        if (privacy_comment > 0)
            params.put("privacy_comment", privacy_comment);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/friends.getLists

    //http://vk.com/dev/photos.deleteAlbum
    public static String deleteAlbum(Long aid, Long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.deleteAlbum");
        params.put("album_id", aid);
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    /**
     * topics region **
     */
    //http://vk.com/dev/board.getTopics


    //http://vk.com/dev/board.getComments


    //http://vk.com/dev/board.addComment
    public static long createGroupTopicComment(long gid, long tid, String text, Collection<String> attachments, boolean from_group, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("board.addComment");
        params.put("group_id", gid);
        params.put("topic_id", tid);
        params.put("text", text);
        params.put("attachments", arrayToString(attachments));
        if (from_group)
            params.put("from_group", "1");
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, true);
        long message_id = root.optLong("response");
        return message_id;
    }

    //http://vk.com/dev/photos.getTags


    //http://vk.com/dev/photos.putTag

    //http://vk.com/dev/board.editComment
    public static boolean editGroupTopicComment(long cid, long gid, long tid, String text, Collection<String> attachments, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("board.editComment");
        params.put("comment_id", cid);
        params.put("group_id", gid);
        params.put("topic_id", tid);
        params.put("text", text);
        params.put("attachments", arrayToString(attachments));
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, true);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/board.deleteComment
    public static Boolean deleteGroupTopicComment(long gid, long tid, long cid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("board.deleteComment");
        params.put("group_id", gid);
        params.put("topic_id", tid);
        params.put("comment_id", cid);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/board.addTopic
    public static long createGroupTopic(long gid, String title, String text, boolean from_group, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("board.addTopic");
        params.put("group_id", gid);
        params.put("title", title);
        params.put("text", text);
        if (from_group)
            params.put("from_group", "1");
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        long topic_id = root.optLong("response");
        return topic_id;
    }

    //http://vk.com/dev/board.deleteTopic
    public static Boolean deleteGroupTopic(long gid, long tid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("board.deleteTopic");
        params.put("group_id", gid);
        params.put("topic_id", tid);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    /**
     * end topics region **
     */

    //http://vk.com/dev/groups.getById
    public static ArrayList<VKGroup> getGroupsById(Collection<Integer> uids, String domain, String fields) throws IOException, JSONException, KException {
        if (uids == null && domain == null)
            return null;
        if (uids.isEmpty() && domain == null)
            return null;
        VKParams params = new VKParams("groups.getById");
        String str_uids;
        if (uids != null && uids.size() > 0)
            str_uids = arrayToString(uids);
        else
            str_uids = domain;
        params.put("group_ids", str_uids);
        params.put("fields", fields);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return VKGroup.parseGroups(array);
    }

    //http://vk.com/dev/groups.join
    public static String joinGroup(long gid, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.join");
        params.put("group_id", gid);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    //http://vk.com/dev/groups.leave
    public static String leaveGroup(long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.leave");
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    // http://vk.com/dev/groups.isMember
    public static Boolean isGroupMember(long group_id, long user_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("groups.isMember");
        params.put("group_id", group_id);
        params.put("user_id", user_id);
        JSONObject root = sendRequest(params);
        Long response = root.optLong("response");

        return response == 1;
    }

    //http://vk.com/dev/groups.search
    public static ArrayList<VKGroup> searchGroup(String q, Long count, Long offset) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.search");
        params.put("q", q);
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKGroup> groups = new ArrayList<VKGroup>();
        //if there are no groups "response" will not be array
        if (array == null)
            return groups;
        groups = VKGroup.parseGroups(array);
        return groups;
    }

    //http://vk.com/dev/account.registerDevice
    public static String registerDevice(String token, String device_model, String system_version, Integer no_text, String subscribe)
            throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.registerDevice");
        params.put("token", token);
        params.put("device_model", device_model);
        params.put("system_version", system_version);
        params.put("no_text", no_text);
        params.put("subscribe", subscribe);
        //params.put("gcm", 1);
        JSONObject root = sendRequest(params);
        return root.getString("response");
    }

    //http://vk.com/dev/account.unregisterDevice
    public static String unregisterDevice(String token) throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.unregisterDevice");
        params.put("token", token);
        JSONObject root = sendRequest(params);
        return root.getString("response");
    }

    //http://vk.com/dev/messages.getById
    public static ArrayList<VKMessage> getMessagesById(ArrayList<Long> message_ids) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getById");
        params.put("message_ids", arrayToString(message_ids));
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKMessage> messages = parseMessages(array, false, 0, false, 0);
        return messages;
    }

    //http://vk.com/dev/notifications.get

    /**
     * faves **
     */


    //http://vk.com/dev/fave.getPhotos
    public static ArrayList<VKPhoto> getFavePhotos(Integer count, Integer offset) throws IOException, JSONException, KException {
        VKParams params = new VKParams("fave.getPhotos");
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        if (array == null)
            return new ArrayList<VKPhoto>();
        ArrayList<VKPhoto> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/account.getCounters

    //http://vk.com/dev/fave.getVideos
    public static ArrayList<VKVideo> getFaveVideos(Integer count, Integer offset) throws IOException, JSONException, KException {
        VKParams params = new VKParams("fave.getVideos");
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKVideo> videos = new ArrayList<VKVideo>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JSONObject o = (JSONObject) array.get(i);
                VKVideo video = VKVideo.parse(o);
                videos.add(video);
            }
        }
        return videos;
    }

    /**
     * chat methods **
     */
    //http://vk.com/dev/messages.createChat
    public static Long сreateChat(ArrayList<Long> uids, String title) throws IOException, JSONException, KException {
        if (uids == null || uids.size() == 0)
            return null;
        VKParams params = new VKParams("messages.createChat");
        String str_uids = String.valueOf(uids.get(0));
        for (int i = 1; i < uids.size(); i++)
            str_uids += "," + String.valueOf(uids.get(i));
        params.put("user_ids", str_uids);
        params.put("title", title);
        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    //http://vk.com/dev/fave.getPosts


    //http://vk.com/dev/fave.getLinks

    /*** end faves  ***/

    //http://vk.com/dev/messages.editChat
    public static Integer editChat(long chat_id, String title) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.editChat");
        params.put("chat_id", chat_id);
        params.put("title", title);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/messages.getChatUsers
    public static ArrayList<VKUser> getChatUsers(long chat_id, String fields) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.getChatUsers");
        params.put("chat_id", chat_id);
        params.put("fields", fields);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return VKUser.parseUsers(array);
    }

    // https://vk.com/dev/messages.getChat
    public static VKChat getChat(long chat_id) throws JSONException, IOException, KException {
        VKParams params = new VKParams("messages.getChat");
        params.put("chat_id", chat_id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return VKChat.parse(response);
    }

    //http://vk.com/dev/messages.addChatUser
    public static Integer addUserToChat(long chat_id, long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.addChatUser");
        params.put("chat_id", chat_id);
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/messages.removeChatUser
    public static Integer removeUserFromChat(long chat_id, long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.removeChatUser");
        params.put("chat_id", chat_id);
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    /**
     * end chat methods **
     */

    //http://vk.com/dev/friends.getSuggestions
    public static ArrayList<VKUser> getSuggestions(String filter, String fields) throws IOException, JSONException, KException {
        VKParams params = new VKParams("friends.getSuggestions");
        params.put("filter", filter);   //mutual, contacts, mutual_contacts
        params.put("fields", fields);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return VKUser.parseUsers(array);
    }

    //http://vk.com/dev/account.importContacts
    @Deprecated
    public static Integer importContacts(Collection<String> contacts) throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.importContacts");
        params.put("contacts", arrayToString(contacts));
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    /**
     * methods for messages search **
     */
    //http://vk.com/dev/messages.search
    public static ArrayList<VKMessage> searchMessages(String q, int offset, int count, Integer preview_length) throws IOException, JSONException, KException {
        VKParams params = new VKParams("messages.search");
        params.put("q", q);
        params.put("count", count);
        params.put("offset", offset);
        params.put("preview_length", preview_length);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<VKMessage> messages = parseMessages(array, false, 0, false, 0);
        return messages;
    }

    //http://vk.com/dev/friends.getByPhones

    //http://vk.com/dev/groups.getMembers
    public static ArrayList<Long> getGroupsMembers(long gid, Integer count, Integer offset, String sort) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.getMembers");
        params.put("group_id", gid);
        params.put("count", count);
        params.put("offset", offset);
        params.put("sort", sort); //id_asc, id_desc, time_asc, time_desc
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        ArrayList<Long> users = new ArrayList<Long>();
        if (array != null) {
            int category_count = array.length();
            for (int i = 0; i < category_count; i++) {
                Long id = array.optLong(i, -1);
                if (id != -1)
                    users.add(id);
            }
        }
        return users;
    }

    //http://vk.com/dev/messages.searchDialogs

    public static ArrayList<VKUser> getGroupsMembersWithExecute(long gid, Integer count, Integer offset, String sort, String fields) throws IOException, JSONException, KException {
        //String code = "return API.users.get({\"user_ids\":API.groups.getMembers({\"gid\":" + String.valueOf(gid) + ",\"count\":" + String.valueOf(count) + ",\"offset\":" + String.valueOf(offset) + ",\"sort\":\"id_asc\"}),\"fields\":\"" + fields + "\"});";
        String code = "var members=API.groups.getMembers({\"gid\":" + gid + "}); var u=members[1]; return API.users.get({\"user_ids\":u,\"fields\":\"" + fields + "\"});";
        VKParams params = new VKParams("execute");
        params.put("code", code);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return VKUser.parseUsers(array);
    }

    //http://vk.com/dev/utils.getServerTime
    public static long getServerTime() throws IOException, JSONException, KException {
        VKParams params = new VKParams("utils.getServerTime");
        JSONObject root = sendRequest(params);
        return root.getLong("response");
    }

    //http://vk.com/dev/audio.getRecommendations
    public static ArrayList<VKAudio> getAudioRecommendations() throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.getRecommendations");
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return parseAudioList(array);
    }

    //http://vk.com/dev/audio.getAlbums

    //http://vk.com/dev/audio.getPopular
    public static ArrayList<VKAudio> getAudioPopular() throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.getPopular");
        //params.put("only_eng", only_eng);
        //params.put("genre_id", genre_id);
        //params.put("count", count);
        //params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array);
    }

    //http://vk.com/dev/wall.edit
    public static int editWallPost(long owner_id, long post_id, String text, Collection<String> attachments, String lat, String lon, long place_id, Long publish_date, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("wall.edit");
        params.put("owner_id", owner_id);
        params.put("post_id", post_id);
        params.put("message", text);
        params.put("attachments", arrayToString(attachments));
        params.put("lat", lat);
        params.put("long", lon);
        params.put("place_id", place_id);
        params.put("publish_date", publish_date);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, true);
        return root.optInt("response");
    }

    //http://vk.com/dev/photos.edit
    public static Integer photoEdit(Long owner_id, long pid, String caption) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.edit");
        params.put("owner_id", owner_id);
        params.put("photo_id", pid);
        params.put("caption", caption);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/docs.get
    public static ArrayList<VKDocument> getDocs(Long owner_id, Long count, Long offset) throws IOException, JSONException, KException {
        VKParams params = new VKParams("docs.get");
        params.put("owner_id", owner_id);
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return VKDocument.parseDocs(array);
    }

    //http://vk.com/dev/docs.getUploadServer
    public static String getDocsUploadServer() throws IOException, JSONException, KException {
        VKParams params = new VKParams("docs.getUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/docs.save
    public static VKDocument saveDoc(String file) throws IOException, JSONException, KException {
        VKParams params = new VKParams("docs.save");
        params.put("file", file);
        JSONObject root = sendRequest(params);
        JSONArray array = root.getJSONArray("response");
        ArrayList<VKDocument> docs = VKDocument.parseDocs(array);
        return docs.get(0);
    }

    //http://vk.com/dev/docs.delete
    public static Boolean deleteDoc(Long doc_id, long owner_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("docs.delete");
        params.put("owner_id", owner_id);
        params.put("doc_id", doc_id);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/notifications.markAsViewed
    public static Boolean markNotificationsAsViewed() throws IOException, JSONException, KException {
        VKParams params = new VKParams("notifications.markAsViewed");
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/newsfeed.addBan
    public static Boolean addBan(Collection<Long> uids, Collection<Long> gids) throws IOException, JSONException, KException {
        VKParams params = new VKParams("newsfeed.addBan");
        if (uids != null && uids.size() > 0)
            params.put("uids", arrayToString(uids));
        if (gids != null && gids.size() > 0)
            params.put("gids", arrayToString(gids));
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/newsfeed.getBanned

    //http://vk.com/dev/newsfeed.deleteBan
    public static Boolean deleteBan(Collection<Long> uids, Collection<Long> gids) throws IOException, JSONException, KException {
        VKParams params = new VKParams("newsfeed.deleteBan");
        if (uids != null && uids.size() > 0)
            params.put("uids", arrayToString(uids));
        if (gids != null && gids.size() > 0)
            params.put("gids", arrayToString(gids));
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/audio.setBroadcast
    public static boolean audioSetBroadcast(String audio, String target_ids) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.setBroadcast");
        params.put("audio", audio);
        params.put("target_ids", target_ids);
        sendRequest(params);
        //В случае успешного выполнения возвращает массив идентификаторов сообществ и пользователя, которым был установлен или удален аудиостатус.
        //response: [1661530]
        //нет необходимости парсить пока
        return true;
    }

    //http://vk.com/dev/audio.getBroadcast
    //gets status of broadcasting user current audio to his page

    //http://vk.com/dev/audio.addAlbum
    public static Long addAudioAlbum(String title, Long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.addAlbum");
        params.put("title", title);
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        JSONObject obj = root.getJSONObject("response");
        return obj.optLong("album_id");
    }

    //http://vk.com/dev/audio.editAlbum
    public static Integer editAudioAlbum(String title, long album_id, Long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.editAlbum");
        params.put("title", title);
        params.put("album_id", album_id);
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/audio.deleteAlbum
    public static Integer deleteAudioAlbum(long album_id, Long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.deleteAlbum");
        params.put("album_id", album_id);
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/audio.moveToAlbum
    public static Integer moveToAudioAlbum(Collection<Long> aids, long album_id, Long gid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.moveToAlbum");
        params.put("audio_ids", arrayToString(aids));
        params.put("album_ids", arrayToString(aids));//album_ids instead audio_ids - Баг в API
        params.put("album_id", album_id);
        params.put("group_id", gid);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/newsfeed.unsubscribe
    public static Integer unsubscribeMewsfeed(String type, Long owner_id, Long item_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("newsfeed.unsubscribe");
        params.put("type", type);
        params.put("owner_id", owner_id);
        params.put("item_id", item_id);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }

    //http://vk.com/dev/wall.getById

    //http://vk.com/dev/account.getBanned
    public static ArrayList<VKUser> getBlackList(Long offset, Long count) throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.getBanned");
        String fields = "first_name,last_name,photo_100,online";
        params.put("fields", fields);
        params.put("offset", offset);
        params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        return VKUser.parseUsers(array);
    }

    //http://vk.com/dev/account.banUser
    public static Boolean addToBlackList(long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.banUser");
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/account.unbanUser
    public static Boolean deleteFromBlackList(long uid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("account.unbanUser");
        params.put("user_id", uid);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/docs.add
    public static Long addDoc(long owner_id, long document_id, String access_key) throws IOException, JSONException, KException {
        VKParams params = new VKParams("docs.add");
        params.put("doc_id", document_id);
        params.put("owner_id", owner_id);
        params.put("access_key", access_key);
        JSONObject root = sendRequest(params);
        //returns new document_id
        return root.optLong("response");
    }

    //http://vk.com/dev/groups.banUser
    public static Boolean addGroupBanUser(long group_id, long user_id, Long end_date, Integer reason, String comment, boolean comment_visible) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.banUser");
        params.put("group_id", group_id);
        params.put("user_id", user_id);
        params.put("end_date", end_date);
        params.put("reason", reason);
        params.put("comment", comment);
        if (comment_visible)
            params.put("comment_visible", "1");
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/groups.unbanUser
    public static Boolean deleteGroupBanUser(long group_id, long user_id) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.unbanUser");
        params.put("group_id", group_id);
        params.put("user_id", user_id);
        JSONObject root = sendRequest(params);
        int response = root.optInt("response");
        return response == 1;
    }

    //http://vk.com/dev/photos.copy
    public static Long copyPhoto(long owner_id, long photo_id, String access_key) throws IOException, JSONException, KException {
        VKParams params = new VKParams("photos.copy");
        params.put("owner_id", owner_id);
        params.put("photo_id", photo_id);
        params.put("access_key", access_key);
        JSONObject root = sendRequest(params);
        Long response = root.optLong("response");
        return response;
    }

    //http://vk.com/dev/account.setOffline
    public static Long setOffline() {
        try {
            VKParams params = new VKParams("account.setOffline");
            JSONObject root = sendRequest(params);
            return root.optLong("response");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    //http://vk.com/dev/groups.getInvites
    public static ArrayList<VKGroup> getGroupsInvites(Long offset, Long count) throws IOException, JSONException, KException {
        VKParams params = new VKParams("groups.getInvites");
        params.put("offset", offset);
        params.put("count", count);
        JSONObject root = sendRequest(params);
        ArrayList<VKGroup> groups = new ArrayList<VKGroup>();
        JSONObject response = root.optJSONObject("response");
        JSONArray array = response.optJSONArray("items");
        //if there are no groups "response" will not be array
        if (array == null)
            return groups;
        groups = VKGroup.parseGroups(array);
        return groups;
    }

    //http://vk.com/dev/audio.edit
    public static Long editAudio(long owner_id, long audio_id, String artist, String title, String text, Integer genre_id, Integer no_search, String captcha_key, String captcha_sid) throws IOException, JSONException, KException {
        VKParams params = new VKParams("audio.edit");
        params.put("owner_id", owner_id);
        params.put("audio_id", audio_id);
        params.put("artist", artist);
        params.put("title", title);
        params.put("text", text);
        params.put("genre_id", genre_id);
        params.put("no_search", no_search);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params, true);
        Long lyrics_id = root.optLong("response");
        return lyrics_id;
    }

    // http://vk.com/dev/stats.trackVisitor
    public static Long trackStatsVisitor() throws JSONException, IOException, KException {
        VKParams params = new VKParams("stats.trackVisitor");
        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    // https://vk.com/dev/storage.set
    public static Long setStorage(String key, String value) throws JSONException, IOException, KException {
        VKParams params = new VKParams("storage.set");
        params.put("key", key);
        params.put("value", value);
        params.put("global", "1");

        JSONObject root = sendRequest(params);
        return root.optLong("response");
    }

    // http://vk.com/dev/storage.get
    public static String getStorage(String key) throws JSONException, IOException, KException {
        VKParams params = new VKParams("storage.get");
        params.put("key", key);
        params.put("global", "1");

        JSONObject root = sendRequest(params);
        return root.optString("response");
    }

    public static class Indentefiers {
        /**
         * Official clients
         */
        public static final int ANDROID_OFFICIAL = 2274003;
        public static final int IPHONE_OFFICIAL = 3140623;
        public static final int IPAD_OFFICIAL = 3682744;
        public static final int WP_OFFICIAL = 3502557;
        public static final int WINDOWS_OFFICIAL = 3697615;

        /**
         * Unofficial client, mods and messengers
         */
        public static final int FAST_MESSENGER = 5462895;

        public static final int KATE_MOBILE = 2685278;
        public static final int EUPHORIA = 4510232;
        public static final int LYNT = 3469984;
        public static final int SWEET = 4856309;
        public static final int AMBERFOG = 4445970;
        public static final int PHOENIX = 4994316;
        public static final int MESSENGER = 4894723;
        public static final int ZEUS = 4831060;
        public static final int ROCKET = 4757672;
        public static final int VK_MD = 4967124;
    }


}
