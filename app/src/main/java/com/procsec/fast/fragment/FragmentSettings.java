package com.procsec.fast.fragment;


import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.procsec.fast.R;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.util.Utils;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static final String KEY_COLOR = "color";
    public static final String KEY_QUICKMOON = "quickmoon";
    public static final String KEY_YELLOW_MOON = "light_moon";
    private Preference quickmoon, yellow_moon, color;
    private String key;

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        key = rootKey;
        init();
    }

    private void init() {
        setPreferencesFromResource(R.xml.prefs, key);

        color = findPreference(KEY_COLOR);
        quickmoon = findPreference(KEY_QUICKMOON);
        yellow_moon = findPreference(KEY_YELLOW_MOON);

        color.setOnPreferenceChangeListener(this);
        quickmoon.setOnPreferenceChangeListener(this);

        yellow_moon.setVisible(Utils.getPrefs().getBoolean(KEY_QUICKMOON, false));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden)
            init();
        super.onHiddenChanged(hidden);
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object newVal) {
        switch (p.getKey()) {
            case KEY_COLOR:
                ThemeManager.putTheme((String) newVal);
                getActivity().recreate();
                break;
            case KEY_QUICKMOON:
                yellow_moon.setVisible((boolean) newVal);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference p1) {
        // TODO: Implement this method
        return true;
    }

}
