package com.procsec.fast;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.procsec.fast.common.ThemeManager;

public class ProxyActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.proxy);
    }
}
