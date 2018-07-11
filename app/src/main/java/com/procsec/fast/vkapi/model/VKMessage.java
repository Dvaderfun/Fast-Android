package com.procsec.fast.vkapi.model;

import com.procsec.fast.vkapi.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class VKMessage implements Serializable {
    public static final int UNREAD = 1;        //сообщение не прочитано
    public static final int OUTBOX = 2;        //исходящее сообщение
    public static final int REPLIED = 4;        //на сообщение был создан ответ
    public static final int IMPORTANT = 8;    //помеченное сообщение
    public static final int CHAT = 16;        //сообщение отправлено через диалог
    public static final int FRIENDS = 32;        //сообщение отправлено другом
    public static final int SPAM = 64;        //сообщение помечено как "Спам"
    public static final int DELETED = 128;    //сообщение удалено (в корзине)
    public static final int FIXED = 256;        //сообщение проверено пользователем на спам
    public static final int MEDIA = 512;        //сообщение содержит медиаконтент
    public static final int BESEDA = 8192;    //беседа
    private static final long serialVersionUID = 1L;

    public static final String ACTION_CHAT_CREATE = "chat_create";
    public static final String ACTION_CHAT_INVITE_USER = "chat_invite_user";
    public static final String ACTION_CHAT_KICK_USER = "chat_kick_user";

    public static final String ACTION_CHAT_TITLE_UPDATE = "chat_title_update";
    public static final String ACTION_CHAT_PHOTO_UPDATE = "chat_photo_update";
    public static final String ACTION_CHAT_PHOTO_REMOVE = "chat_photo_remove";

    public boolean is_new = false;
    public boolean is_sending = false;

    public String action = null;
    public int action_mid = 0;
	
	public static ArrayList<VKMessage> fws;
    
    public int id;
    
    public int user_id;
    
    public long date;
    
    public String title;
    
    public String body;
    
    public boolean read_state;
    public boolean is_out;
    
    public ArrayList<VKAttachment> attachments = new ArrayList<>();
    public ArrayList<VKMessage> fws_messages;
	
    public int chat_id;
    
    public ArrayList<Long> chat_members;
    public Boolean push_enabled = true;
    
    public Long admin_id;
    
    public Long users_count;
    
    public boolean is_deleted;
    public boolean is_important;
    
    public String photo_50;
    public String photo_100;
    public String photo_200;
	
    public long unread;
    public long count;
    public int flag;
    public String action_text;

    public static boolean isUnread(int flags) {
        return (flags & UNREAD) != 0;
    }

    public static VKMessage parse(JSONObject source) throws JSONException {
        VKMessage message = new VKMessage();
        message.id = source.optInt("id");
        message.user_id = source.optInt("user_id");
        message.chat_id = source.optInt("chat_id");
        message.date = source.optLong("date");
        message.is_out = source.optLong("out") == 1;
        message.read_state = source.optLong("read_state") == 1;
        message.title = VKApi.unescape(source.optString("title"));
        message.body = source.optString("body");
        message.users_count = source.optLong("users_count");
        message.is_deleted = source.optLong("deleted") == 1;
        message.is_important = source.optLong("important") == 1;
        message.photo_50 = source.optString("photo_50");
        message.photo_100 = source.optString("photo_100");
        message.photo_200 = source.optString("photo_200");

        if (source.has("action")) {
            message.action = source.optString("action");
            message.action_text = source.optString("action_text");
            message.action_mid = source.optInt("action_mid");
        }

        JSONArray atts = source.optJSONArray("attachments");
        if (atts != null) {
            message.attachments = VKAttachment.parseArray(atts);
        }
		
		JSONArray messages = source.optJSONArray("fwd_messages");
        if (messages != null) {
            message.fws_messages = new ArrayList<>(messages.length());
            for (int i = 0; i < messages.length(); i++) {
                message.fws_messages.add(new VKMessage().parse(messages.optJSONObject(i)));
            }
        }

        JSONArray fwdMessages = source.optJSONArray("fwd_messages");
        if (fwdMessages != null) {
            for (int i = 0; i < fwdMessages.length(); i++) {
                VKMessage fwd_msg = VKMessage.parse(fwdMessages.optJSONObject(i));
                VKAttachment att = new VKAttachment();
                att.type = VKAttachment.TYPE_MESSAGE;
                att.message = fwd_msg;
                message.attachments.add(att);
            }
        }

        JSONArray chat_active = source.optJSONArray("chat_active");
        if (chat_active != null) {
            for (int i = 0; i < chat_active.length(); i++) {
                message.chat_members = new ArrayList<>();
                message.chat_members.add(chat_active.optLong(i));
            }
        }

        // TODO: from_id возврвщается только тогда, когда получаем историю
        int from_id = source.optInt("from_id", -1);
        if (from_id != -1 && message.chat_id != 0) {
            message.user_id = from_id;
        }

        return message;
    }

    public static ArrayList<VKMessage> parseArray(JSONArray array) throws JSONException {
        ArrayList<VKMessage> vkMessages = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            VKMessage message;
            if (item.has("message")) {
                message = VKMessage.parse(item.optJSONObject("message"));
                message.unread = item.optInt("unread");
                message.count = item.optInt("count");
            } else {
                message = VKMessage.parse(item);
            }

            vkMessages.add(message);
        }

        return vkMessages;
    }

    // parse from long poll (update[])
    public static VKMessage parse(JSONArray a) throws JSONException {
        VKMessage m = new VKMessage();
        m.id = a.optInt(1);
        m.flag = a.optInt(2);
        m.user_id = a.optInt(3);
        m.date = a.optLong(4);
        m.title = VKApi.unescape(a.optString(5));
        m.body = (a.optString(6));
        m.read_state = ((m.flag & UNREAD) == 0);
        m.is_out = (m.flag & OUTBOX) != 0;
        if ((m.flag & BESEDA) != 0) {
            m.chat_id = a.optInt(3) - 2000000000;// & 63;//cut 6 last digits
            JSONObject o = a.optJSONObject(7);
            m.user_id = o.optInt("from");
        }


        // m.attachment = a.getJSONArray(7); TODO
        m.attachments = VKAttachment.parseArray(a.optJSONArray(7));
        return m;
    }

    public static VKMessage parseReadMessages(JSONArray a) throws JSONException {
        VKMessage m = new VKMessage();
        m.id = a.optInt(2);
        return m;
    }

    public boolean isChat() {
        return chat_id != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VKMessage message = (VKMessage) o;

        if (chat_id != message.chat_id) return false;
        if (date != message.date) return false;
        if (is_deleted != message.is_deleted) return false;
        if (is_important != message.is_important) return false;
        if (is_out != message.is_out) return false;
        if (id != message.id) return false;
        if (read_state != message.read_state) return false;
        if (user_id != message.user_id) return false;
        if (users_count != message.users_count) return false;
        if (admin_id != null ? !admin_id.equals(message.admin_id) : message.admin_id != null)
            return false;
        if (attachments != null ? !attachments.equals(message.attachments) : message.attachments != null)
            return false;
        if (body != null ? !body.equals(message.body) : message.body != null) return false;
        if (chat_members != null ? !chat_members.equals(message.chat_members) : message.chat_members != null)
            return false;
        if (photo_100 != null ? !photo_100.equals(message.photo_100) : message.photo_100 != null)
            return false;
        if (photo_200 != null ? !photo_200.equals(message.photo_200) : message.photo_200 != null)
            return false;
        if (photo_50 != null ? !photo_50.equals(message.photo_50) : message.photo_50 != null)
            return false;
        return !(title != null ? !title.equals(message.title) : message.title != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (user_id ^ (user_id >>> 32));
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (read_state ? 1 : 0);
        result = 31 * result + (is_out ? 1 : 0);
        result = 31 * result + (attachments != null ? attachments.hashCode() : 0);
        result = 31 * result + (int) (chat_id ^ (chat_id >>> 32));
        result = 31 * result + (chat_members != null ? chat_members.hashCode() : 0);
        result = 31 * result + (admin_id != null ? admin_id.hashCode() : 0);
        result = 31 * result + (int) (users_count ^ (users_count >>> 32));
        result = 31 * result + (is_deleted ? 1 : 0);
        result = 31 * result + (is_important ? 1 : 0);
        result = 31 * result + (photo_50 != null ? photo_50.hashCode() : 0);
        result = 31 * result + (photo_100 != null ? photo_100.hashCode() : 0);
        result = 31 * result + (photo_200 != null ? photo_200.hashCode() : 0);
        result = 31 * result + flag;
        return result;
    }
}
