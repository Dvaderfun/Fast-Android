package com.procsec.fast.db;

import android.content.ContentValues;
import android.database.Cursor;

import com.procsec.fast.common.FApp;
import com.procsec.fast.util.ArrayUtil;
import com.procsec.fast.util.Utils;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKAudio;
import com.procsec.fast.vkapi.model.VKGroup;
import com.procsec.fast.vkapi.model.VKMessage;
import com.procsec.fast.vkapi.model.VKPhoto;
import com.procsec.fast.vkapi.model.VKUser;

import java.util.ArrayList;
import java.util.Locale;

import static com.procsec.fast.common.FApp.database;
import static com.procsec.fast.db.DBHelper.ADMIN_LEVER;
import static com.procsec.fast.db.DBHelper.ALBUM_ID;
import static com.procsec.fast.db.DBHelper.ARTIST;
import static com.procsec.fast.db.DBHelper.ATTACHMENTS;
import static com.procsec.fast.db.DBHelper.AUDIO_ID;
import static com.procsec.fast.db.DBHelper.BODY;
import static com.procsec.fast.db.DBHelper.CHAT_ID;
import static com.procsec.fast.db.DBHelper.DATE;
import static com.procsec.fast.db.DBHelper.DESCRIPTION;
import static com.procsec.fast.db.DBHelper.DIALOGS_TABLE;
import static com.procsec.fast.db.DBHelper.DURATION;
import static com.procsec.fast.db.DBHelper.FIRST_NAME;
import static com.procsec.fast.db.DBHelper.FRIENDS_TABLE;
import static com.procsec.fast.db.DBHelper.FRIEND_ID;
import static com.procsec.fast.db.DBHelper.FWD_MESSAGES;
import static com.procsec.fast.db.DBHelper.GROUPS_TABLE;
import static com.procsec.fast.db.DBHelper.GROUP_ID;
import static com.procsec.fast.db.DBHelper.HEIGHT;
import static com.procsec.fast.db.DBHelper.IMPORTANT;
import static com.procsec.fast.db.DBHelper.IS_ADMIN;
import static com.procsec.fast.db.DBHelper.IS_CLOSED;
import static com.procsec.fast.db.DBHelper.IS_OUT;
import static com.procsec.fast.db.DBHelper.LAST_NAME;
import static com.procsec.fast.db.DBHelper.LAST_SEEN;
import static com.procsec.fast.db.DBHelper.MEMBERS_COUNT;
import static com.procsec.fast.db.DBHelper.MESSAGES_TABLE;
import static com.procsec.fast.db.DBHelper.MESSAGE_ID;
import static com.procsec.fast.db.DBHelper.NAME;
import static com.procsec.fast.db.DBHelper.ONLINE;
import static com.procsec.fast.db.DBHelper.ONLINE_APP;
import static com.procsec.fast.db.DBHelper.ONLINE_MOBILE;
import static com.procsec.fast.db.DBHelper.OWNER_ID;
import static com.procsec.fast.db.DBHelper.PHOTOS_TABLE;
import static com.procsec.fast.db.DBHelper.PHOTO_100;
import static com.procsec.fast.db.DBHelper.PHOTO_1280;
import static com.procsec.fast.db.DBHelper.PHOTO_130;
import static com.procsec.fast.db.DBHelper.PHOTO_200;
import static com.procsec.fast.db.DBHelper.PHOTO_2560;
import static com.procsec.fast.db.DBHelper.PHOTO_50;
import static com.procsec.fast.db.DBHelper.PHOTO_604;
import static com.procsec.fast.db.DBHelper.PHOTO_75;
import static com.procsec.fast.db.DBHelper.PHOTO_807;
import static com.procsec.fast.db.DBHelper.READ_STATE;
import static com.procsec.fast.db.DBHelper.SCREEN_NAME;
import static com.procsec.fast.db.DBHelper.SEX;
import static com.procsec.fast.db.DBHelper.STATUS;
import static com.procsec.fast.db.DBHelper.TEXT;
import static com.procsec.fast.db.DBHelper.TITLE;
import static com.procsec.fast.db.DBHelper.TYPE;
import static com.procsec.fast.db.DBHelper.UNREAD_COUNT;
import static com.procsec.fast.db.DBHelper.URL;
import static com.procsec.fast.db.DBHelper.USERS_COUNT;
import static com.procsec.fast.db.DBHelper.USERS_TABLE;
import static com.procsec.fast.db.DBHelper.USER_ID;
import static com.procsec.fast.db.DBHelper.WIDTH;
import static com.procsec.fast.db.DBHelper._ID;

public class CacheStorage {
    public static void checkOpen() {
        if (!database.isOpen()) {
            database = DBHelper.getInstance().getWritableDatabase();
        }
    }

    private static Cursor selectCursor(String table, String column, Object value) {
        return QueryBuilder.query()
                .select("*").from(table)
                .where(column.concat(" = ").concat(String.valueOf(value)))
                .asCursor(database);
    }

    private static Cursor selectCursor(String table, String column, int... ids) {
        StringBuilder where = new StringBuilder(5 * ids.length);

        where.append(column);
        where.append(" = ");
        where.append(ids[0]);
        for (int i = 1; i < ids.length; i++) {
            where.append(" OR ");
            where.append(column);
            where.append(" = ");
            where.append(ids[i]);
        }
        return selectCursor(table, where.toString());
    }

    private static Cursor selectCursor(String table, String where) {
        return QueryBuilder.query()
                .select("*").from(table).where(where)
                .asCursor(database);
    }

    private static Cursor selectCursor(String table) {
        return QueryBuilder.query()
                .select("*").from(table)
                .asCursor(database);
    }

    private static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    private static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private static byte[] getBlob(Cursor cursor, String columnName) {
        return cursor.getBlob(cursor.getColumnIndex(columnName));
    }

    public static VKUser getUser(int id) {
        Cursor cursor = selectCursor(USERS_TABLE, USER_ID, id);
        if (cursor.moveToFirst()) {
            return parseUser(cursor);
        }
        cursor.close();
        return null;
    }

    public static VKGroup getGroup(int id) {
        Cursor cursor = selectCursor(GROUPS_TABLE, GROUP_ID, id);
        if (cursor.moveToFirst()) {
            return parseGroup(cursor);
        }
        cursor.close();
        return null;
    }

    public static VKPhoto getPhoto(int id) {
        Cursor cursor = selectCursor(PHOTOS_TABLE, _ID, id);
        if (cursor.moveToFirst()) {
            return parsePhoto(cursor);
        }

        cursor.close();
        return null;
    }

    public static ArrayList<VKUser> getUsers(int ids) {
        Cursor cursor = selectCursor(USERS_TABLE, USER_ID, ids);
        ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            users.add(parseUser(cursor));
        }

        cursor.close();
        return users;
    }

    public static ArrayList<VKUser> getFriends(int userId, boolean onlyOnline) {
        Cursor cursor = QueryBuilder.query()
                .select("*")
                .from(FRIENDS_TABLE)
                .leftJoin(USERS_TABLE)
                .on("friends.friend_id = users.user_id")
                .where("friends.user_id = " + userId)
                .asCursor(database);

        ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            boolean userOnline = getInt(cursor, ONLINE) == 1;
            if (onlyOnline && !userOnline) {
                continue;
            }

            VKUser user = parseUser(cursor);
            users.add(user);
        }
        cursor.close();

        return users;
    }

    public static ArrayList<VKMessage> getDialogs() {
        Cursor cursor = selectCursor(DIALOGS_TABLE);
        if (cursor.getCount() <= 0) {
            return null;
        }

        ArrayList<VKMessage> messages = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            messages.add(parseDialog(cursor));
        }

        cursor.close();
        return messages;
    }

    public static ArrayList<VKGroup> getGroups() {
        Cursor cursor = selectCursor(GROUPS_TABLE);
        if (cursor.getCount() <= 0) {
            return null;
        }

        ArrayList<VKGroup> groups = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            groups.add(parseGroup(cursor));
        }

        cursor.close();
        return groups;
    }

    public static ArrayList<VKMessage> getMessages(int userId, int chatId) {
        Cursor cursor;
        if (chatId > 0) {
            cursor = selectCursor(MESSAGES_TABLE, CHAT_ID, chatId);
        } else {
            cursor = selectCursor(MESSAGES_TABLE, String.format(FApp.locale, "%s = 0 AND %s = %d", CHAT_ID, USER_ID, userId));
        }

        ArrayList<VKMessage> messages = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            messages.add(parseMessage(cursor));
        }
        cursor.close();
        return messages;
    }

    private static String dialogWhere(int userId, int chatId) {
        String where;
        if (chatId > 0) {
            where = String.format(Locale.US, "%s = %d", CHAT_ID, chatId);
        } else {
            where = String.format(Locale.US, "%s = 0 AND %s = %d", CHAT_ID, USER_ID, userId);
        }
        return where;
    }

    public static void deleteMessages(int userId, int chatId) {
        String where = dialogWhere(userId, chatId);
        delete(MESSAGES_TABLE, where);
    }

    public static void deleteDialog(int userId, int chatId) {
        String where = dialogWhere(userId, chatId);
        delete(DIALOGS_TABLE, where);
    }


    public static void insert(String table, ArrayList values) {
        database.beginTransaction();

        ContentValues cv = new ContentValues();
        for (int i = 0; i < values.size(); i++) {
            switch (table) {
                case USERS_TABLE:
                    putValues(cv, (VKUser) values.get(i), false);
                    break;

                case FRIENDS_TABLE:
                    putValues(cv, (VKUser) values.get(i), true);
                    break;

                case DIALOGS_TABLE:
                    putValues(cv, (VKMessage) values.get(i), true);
                    break;

                case MESSAGES_TABLE:
                    putValues(cv, (VKMessage) values.get(i), false);
                    break;

                case GROUPS_TABLE:
                    putValues(cv, (VKGroup) values.get(i));
                    break;

                case PHOTOS_TABLE:
                    putValues(cv, (VKPhoto) values.get(i));
                    break;
            }

            database.insert(table, null, cv);
            cv.clear();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public static void delete(String table, String where) {
        database.delete(table, where, null);
    }

    public static void delete(String table) {
        database.delete(table, null, null);
    }

    private static VKUser parseUser(Cursor cursor) {
        VKUser user = new VKUser();

        user.id = getInt(cursor, USER_ID);
        user.first_name = getString(cursor, FIRST_NAME);
        user.last_name = getString(cursor, LAST_NAME);
        user.last_seen = getInt(cursor, LAST_SEEN);
        user.screen_name = getString(cursor, SCREEN_NAME);
        user.status = getString(cursor, STATUS);
        user.photo_50 = getString(cursor, PHOTO_50);
        user.photo_100 = getString(cursor, PHOTO_100);
        user.photo_200 = getString(cursor, PHOTO_200);

        user.online = getInt(cursor, ONLINE) == 1;
        user.online_mobile = getInt(cursor, ONLINE_MOBILE) == 1;
        user.online_app = getInt(cursor, ONLINE_APP);
        // user.deactivated = getString(cursor, DEACTIVATED);
        user.sex = getInt(cursor, SEX);
        return user;
    }

    public static VKMessage parseDialog(Cursor cursor) {
        VKMessage message = new VKMessage();

        message.id = getInt(cursor, MESSAGE_ID);
        message.user_id = getInt(cursor, USER_ID);
        message.chat_id = getInt(cursor, CHAT_ID);
        message.title = getString(cursor, TITLE);
        message.body = getString(cursor, BODY);
        message.is_out = getInt(cursor, IS_OUT) == 1;
        message.read_state = getInt(cursor, READ_STATE) == 1;
        // message.users_count = getInt(cursor, USERS_COUNT);
        message.unread = getInt(cursor, UNREAD_COUNT);
        message.date = getInt(cursor, DATE);

        message.photo_50 = getString(cursor, PHOTO_50);
        message.photo_100 = getString(cursor, PHOTO_100);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static VKMessage parseMessage(Cursor cursor) {
        VKMessage message = new VKMessage();

        message.id = getInt(cursor, MESSAGE_ID);
        message.user_id = getInt(cursor, USER_ID);
        message.chat_id = getInt(cursor, CHAT_ID);
        message.date = getInt(cursor, DATE);
        message.body = getString(cursor, BODY);
        message.read_state = getInt(cursor, READ_STATE) == 1;
        message.is_out = getInt(cursor, IS_OUT) == 1;
        message.is_important = getInt(cursor, IMPORTANT) == 1;
        message.attachments = (ArrayList) Utils.deserialize(getBlob(cursor, ATTACHMENTS));
        message.fws_messages = (ArrayList) Utils.deserialize(getBlob(cursor, FWD_MESSAGES));
        return message;
    }

    public static VKGroup parseGroup(Cursor cursor) {
        VKGroup group = new VKGroup();

        group.id = getInt(cursor, GROUP_ID);
        group.name = getString(cursor, NAME);
        group.screen_name = getString(cursor, SCREEN_NAME);
        group.description = getString(cursor, DESCRIPTION);
        group.status = getString(cursor, STATUS);
        group.type = getInt(cursor, TYPE);
        group.is_closed = getInt(cursor, IS_CLOSED);
        group.admin_level = getInt(cursor, ADMIN_LEVER);
        group.is_admin = getInt(cursor, IS_ADMIN) == 1;
        group.photo_50 = getString(cursor, PHOTO_50);
        group.photo_100 = getString(cursor, PHOTO_100);
        group.members_count = getInt(cursor, MEMBERS_COUNT);
        return group;
    }

    public static VKPhoto parsePhoto(Cursor cursor) {
        VKPhoto photo = new VKPhoto();

        photo.id = getInt(cursor, _ID);
        photo.album_id = getInt(cursor, ALBUM_ID);
        photo.owner_id = getInt(cursor, OWNER_ID);
        photo.text = getString(cursor, TEXT);
        photo.date = getInt(cursor, DATE);
        photo.photo_75 = getString(cursor, PHOTO_75);
        photo.photo_130 = getString(cursor, PHOTO_130);
        photo.photo_604 = getString(cursor, PHOTO_604);
        photo.photo_807 = getString(cursor, PHOTO_807);
        photo.photo_1280 = getString(cursor, PHOTO_1280);
        photo.photo_2560 = getString(cursor, PHOTO_2560);
        photo.width = getInt(cursor, WIDTH);
        photo.height = getInt(cursor, HEIGHT);
        return photo;
    }

    private static void putValues(ContentValues values, VKUser user, boolean friends) {
        if (friends) {
            values.put(USER_ID, VKApi.getAccount().id);
            values.put(FRIEND_ID, user.id);
            return;
        }

        values.put(USER_ID, user.id);
        values.put(FIRST_NAME, user.first_name);
        values.put(LAST_NAME, user.last_name);
        values.put(SCREEN_NAME, user.screen_name);
        values.put(LAST_SEEN, user.last_seen);
        values.put(ONLINE, user.online);
        values.put(ONLINE_MOBILE, user.online_mobile);
        values.put(ONLINE_APP, user.online_app);
        values.put(STATUS, user.status);
        values.put(PHOTO_50, user.photo_50);
        values.put(PHOTO_100, user.photo_100);
        values.put(PHOTO_200, user.photo_200);
        values.put(PHOTO_200, user.photo_200);
        //values.put(DEACTIVATED, user.deactivated);
        values.put(SEX, user.sex);
    }

    private static void putValues(ContentValues values, VKMessage dialog, boolean isDialog) {
        values.put(MESSAGE_ID, dialog.id);
        values.put(USER_ID, dialog.user_id);
        values.put(CHAT_ID, dialog.chat_id);
        values.put(BODY, dialog.body);
        values.put(DATE, dialog.date);
        values.put(IS_OUT, dialog.is_out);
        values.put(READ_STATE, dialog.read_state);

        if (isDialog) {
            values.put(TITLE, dialog.title);
            values.put(USERS_COUNT, dialog.users_count);
            values.put(UNREAD_COUNT, dialog.unread);
            values.put(PHOTO_50, dialog.photo_50);
            values.put(PHOTO_100, dialog.photo_100);
        } else {
            values.put(IMPORTANT, dialog.is_important);
            if (!ArrayUtil.isEmpty(dialog.attachments)) {
                values.put(ATTACHMENTS, Utils.serialize(dialog.attachments));
            }
            if (!ArrayUtil.isEmpty(dialog.fws_messages)) {
                values.put(FWD_MESSAGES, Utils.serialize(dialog.fws_messages));
            }
        }

    }

    private static void putValues(ContentValues values, VKGroup group) {
        values.put(GROUP_ID, group.id);
        values.put(NAME, group.name);
        values.put(SCREEN_NAME, group.screen_name);
        values.put(DESCRIPTION, group.description);
        values.put(STATUS, group.status);
        values.put(TYPE, group.type);
        values.put(IS_CLOSED, group.is_closed);
        values.put(ADMIN_LEVER, group.admin_level);
        values.put(IS_ADMIN, group.is_admin);
        values.put(PHOTO_50, group.photo_50);
        values.put(PHOTO_100, group.photo_100);
        values.put(MEMBERS_COUNT, group.members_count);
    }

    private static void putValues(ContentValues values, VKPhoto photo) {
        values.put(_ID, photo.id);
        values.put(ALBUM_ID, photo.album_id);
        values.put(OWNER_ID, photo.owner_id);
        values.put(WIDTH, photo.width);
        values.put(HEIGHT, photo.height);
        values.put(TEXT, photo.text);
        values.put(DATE, photo.date);
        values.put(PHOTO_75, photo.photo_75);
        values.put(PHOTO_130, photo.photo_130);
        values.put(PHOTO_604, photo.photo_604);
        values.put(PHOTO_807, photo.photo_807);
        values.put(PHOTO_1280, photo.photo_1280);
        values.put(PHOTO_2560, photo.photo_2560);
    }

    private static void putValues(ContentValues values, VKAudio audio) {
        values.put(AUDIO_ID, audio.id);
        values.put(OWNER_ID, audio.owner_id);
        values.put(ARTIST, audio.artist);
        values.put(TITLE, audio.title);
        values.put(DURATION, audio.duration);
        values.put(URL, audio.url);
    }
}
