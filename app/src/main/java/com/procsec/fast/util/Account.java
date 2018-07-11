package com.procsec.fast.util;

import android.content.*;
import android.content.SharedPreferences.*;
import android.preference.*;
import com.procsec.fast.common.*;

public class Account {

    public String token;
    public int id;

    public String photo_200,
	full_name, 
	name, 
	surname,
	status, 
	photo_50;

	public void makeFullName() {
		this.full_name = this.name + " " + this.surname;
		save();
	}

    public void save() {
        SharedPreferences.Editor editor = Utils.getPrefs().edit();
        editor.putString("token", token);
        editor.putInt("id", id);
		editor.putString("name", name);
		editor.putString("surname", surname);
        editor.putString("photo_200", photo_200);
        editor.putString("photo_50", photo_50);
        editor.putString("full_name", full_name);
        editor.putString("status", status);

        editor.apply();
    }

    public Account restore() {
        SharedPreferences prefs = Utils.getPrefs();
        token = prefs.getString("token", "");
        id = prefs.getInt("id", 0);
        photo_200 = prefs.getString("photo_200", "");
        photo_50 = prefs.getString("photo_50", "");
        full_name = prefs.getString("full_name", "");
		surname = prefs.getString("surname", "");
		name = prefs.getString("name", "");
        status = prefs.getString("status", "");
		return this;
    }

    public void clear() {
        SharedPreferences.Editor editor = Utils.getPrefs().edit();
		editor.remove("token");
		editor.remove("id");
		editor.remove("photo_200");
		editor.remove("photo_50");
		editor.remove("full_name");
		editor.remove("status");
		editor.remove("name");
		editor.remove("surname");
		editor.apply();
    }

    @Override
    public String toString() {
        return full_name;
    }
}
