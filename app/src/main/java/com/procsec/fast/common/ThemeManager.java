package com.procsec.fast.common;

import android.graphics.Color;
import android.support.v7.widget.Toolbar;

import com.procsec.fast.R;
import com.procsec.fast.fragment.FragmentSettings;
import com.procsec.fast.util.Utils;

public class ThemeManager {

    public static final String THEME_BLUE = "blue";
    public static final String THEME_TEAL = "teal";
    public static final String THEME_RED = "red";
    public static final String THEME_LIGHTBLUE = "lightblue";
    public static final String THEME_ORANGE = "orange";
    public static final String THEME_PURPLE = "purple";
    public static final String THEME_GREEN = "green";
    public static String current_theme_name;
    public static int current_theme;
    public static int color;

    public static ThemeManager init() {
        ThemeManager manager = new ThemeManager();
        current_theme_name = Utils.getPrefs().getString(FragmentSettings.KEY_COLOR, "blue");
        current_theme = getCurrentTheme();
        color = getColor();
        return manager;
    }

    public static void putTheme(String theme) {
        Utils.getPrefs().edit().putString(FragmentSettings.KEY_COLOR, theme).apply();
    }

    private static int getColor() {
        switch (current_theme_name) {
            case THEME_BLUE:
                return getColor(R.color.colorPrimary);
            case THEME_TEAL:
                return getColor(R.color.colorPrimaryTeal);
            case THEME_RED:
                return getColor(R.color.colorPrimaryRed);
            case THEME_LIGHTBLUE:
                return getColor(R.color.colorPrimaryLightblue);
            case THEME_ORANGE:
                return getColor(R.color.colorPrimaryOrange);
            case THEME_PURPLE:
                return getColor(R.color.colorPrimaryPurple);
            case THEME_GREEN:
                return getColor(R.color.colorPrimaryGreen);
            default:
                return getColor(R.color.colorPrimary);
        }
    }

    public static int getCurrentTheme() {
        switch (current_theme_name) {
            case THEME_BLUE:
                return R.style.BlueTheme;
            case THEME_TEAL:
                return R.style.TealTheme;
            case THEME_RED:
                return R.style.RedTheme;
            case THEME_LIGHTBLUE:
                return R.style.LightBlueTheme;
            case THEME_ORANGE:
                return R.style.OrangeTheme;
            case THEME_PURPLE:
                return R.style.PurpleTheme;
            case THEME_GREEN:
                return R.style.GreenTheme;
            default:
                return R.style.BlueTheme;
        }
    }

    private static int getColor(int c) {
        return FApp.context.getResources().getColor(c);
    }

    public static void applyToolbarStyles(Toolbar toolbar) {
        if (toolbar != null) {
            toolbar.setBackgroundColor(Color.WHITE);
            toolbar.setTitleTextColor(color);
            if (toolbar.getNavigationIcon() != null)
                toolbar.getNavigationIcon().setTint(color);
            if (toolbar.getOverflowIcon() != null)
                toolbar.getOverflowIcon().setTint(color);
        }
    }

}
