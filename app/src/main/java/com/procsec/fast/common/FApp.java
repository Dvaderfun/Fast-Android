package com.procsec.fast.common;

import android.app.*;
import android.content.*;
import android.database.sqlite.*;
import com.procsec.fast.db.*;
import java.util.*;
import com.procsec.fast.service.*;

public class FApp extends Application {

	public static volatile Context context;
	public static volatile SQLiteDatabase database;
	public static volatile Locale locale;
	
	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		database = DBHelper.getInstance().getWritableDatabase();
		locale = Locale.getDefault();
	}
	
	public static boolean isDebug() {
		return false;
	}
}
