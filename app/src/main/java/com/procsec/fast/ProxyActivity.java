package com.procsec.fast;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ProxyActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.proxy);
    }
}
