package com.procsec.fast.db;

import android.util.SparseArray;

import com.procsec.fast.vkapi.model.VKGroup;
import com.procsec.fast.vkapi.model.VKUser;

import java.util.ArrayList;

public class MemoryCache {

    private static VKUser u;

    private static SparseArray<VKUser> users = new SparseArray<>(20);
    private static SparseArray<VKGroup> groups = new SparseArray<>(20);

    public static VKUser getUser(int id) {
        VKUser user = users.get(id);
        if (user == null) {
            user = CacheStorage.getUser(id);
            if (user != null) {
                append(user);
            }
        }
        return user;
    }

    public static VKGroup getGroup(int id) {
        VKGroup group = groups.get(id);
        if (group == null) {
            group = CacheStorage.getGroup(id);
            if (group != null) {
                append(group);
            }
        }
        return group;
    }

    public static void update(ArrayList<VKUser> users) {
        for (VKUser user : users) {
            append(user);
        }
    }

    public static void append(VKGroup value) {
        groups.append(value.id, value);
    }

    public static void append(VKUser value) {
        users.append(value.id, value);
    }

    public static void clear() {
        users.clear();
        groups.clear();
    }
}
