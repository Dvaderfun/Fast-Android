package com.procsec.fast.helper;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.procsec.fast.fragment.FragmentDialogs;
import com.procsec.fast.fragment.FragmentFriends;
import com.procsec.fast.fragment.FragmentGroups;
import com.procsec.fast.fragment.FragmentSettings;

import java.util.ArrayList;

import ru.lischenko_dev.fastmessenger.R;

public class FragmentTransHelper {

    private static Fragment current_fragment;
    private static Integer layout_id;

    public static FragmentDialogs fragmentDialogs;
    public static FragmentFriends fragmentFriends;
    public static FragmentGroups fragmentGroups;
    public static FragmentSettings fragmentSettings;

    private static FragmentTransaction transaction;
    private static FragmentManager manager;

    private static ArrayList<Fragment> fragments;

    public static void init() {
        layout_id = R.id.container;
        current_fragment = new FragmentDialogs();

        fragmentDialogs = (FragmentDialogs) current_fragment;
        fragmentFriends = new FragmentFriends();
        fragmentGroups = new FragmentGroups();
        fragmentSettings = new FragmentSettings();

        fragments = new ArrayList<>();

        fragments.add(fragmentDialogs);
        fragments.add(fragmentFriends);
        fragments.add(fragmentGroups);
        fragments.add(fragmentSettings);
    }

    public static void addFragmentsToStack(AppCompatActivity a) {
        manager = a.getSupportFragmentManager();
        FragmentTransaction tr = manager.beginTransaction();
        for (Fragment f : fragments) {
            tr.add(R.id.container, f);
            tr.hide(f);
        }

        tr.commit();
    }

    public static void replaceFragment(FragmentManager fm, Fragment f) {
        setCurrentFragment(f);
        transaction = fm.beginTransaction();

        for (Fragment fr : fragments) {
            if (!fr.equals(f)) {
                transaction.hide(fr);
            } else {
                transaction.show(fr);
            }
        }

        transaction.commit();
    }

    public static Fragment getCurrentFragment() {
        return current_fragment;
    }

    public static void setCurrentFragment(Fragment f) {
        current_fragment = f;
    }
}

