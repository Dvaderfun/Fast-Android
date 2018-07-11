package com.procsec.fast.common;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.procsec.fast.db.DBHelper;
import com.procsec.fast.vkapi.VKApi;

import java.util.Locale;

public class FApp extends Application {

    public static volatile Context context;
    public static volatile SQLiteDatabase database;
    public static volatile Locale locale;

    public static boolean isDebug() {
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        database = DBHelper.getInstance().getWritableDatabase();
        locale = Locale.getDefault();

        VKApi.initBaseUrl();
    }
}
