package com.procsec.fast.vkapi;

import android.util.Log;

import java.net.URLEncoder;

public class Auth {

    private static final String TAG = "Kate.Auth";

    public static String redirect_url = "https://oauth.vk.com/blank.html";


    public static String getUrl(String api_id, String settings) {
        return "https://" +
                VKApi.OAUTH +
                "/authorize?client_id=" +
                api_id +
                "&display=mobile&scope=" +
                settings +
                "&redirect_uri=" +
                URLEncoder.encode(redirect_url) +
                "&response_type=token" +
                "&v=" +
                URLEncoder.encode(VKApi.API_VERSION);
    }

    public static String getSettings() {
        return "notify,friends,photos,audio,video,docs,status,notes,pages,wall,groups,messages,offline,notifications";
    }

    public static String[] parseRedirectUrl(String url) throws Exception {
        String access_token = VKUtils.extractPattern(url, "access_token=(.*?)&");
        Log.i(TAG, "access_token=" + access_token);
        String user_id = VKUtils.extractPattern(url, "user_id=(\\d*)");
        Log.i(TAG, "user_id=" + user_id);
        if (user_id == null || user_id.length() == 0 || access_token == null || access_token.length() == 0)
            throw new Exception("Failed to parse redirect url " + url);
        return new String[]{access_token, user_id};
    }
}
