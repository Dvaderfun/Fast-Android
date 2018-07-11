package com.procsec.fast.util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class JSONParser {

    public static JSONObject parseObject(String url, String charset) {
        BufferedReader reader;
        HttpURLConnection connection;
        StringBuilder builder;
        JSONObject result = null;
        try {
            connection = (HttpURLConnection) new java.net.URL(url).openConnection();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
            builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            connection.disconnect();
            reader.close();
            result = new JSONObject(builder.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

