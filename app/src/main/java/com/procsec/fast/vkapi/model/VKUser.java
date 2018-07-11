package com.procsec.fast.vkapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Igor on 16.07.15.
 */
public class VKUser implements Serializable {

    /** Empty user object */
    public static final VKUser EMPTY = new VKUser() {
        @Override
        public String toString() {
            return "";
        }
    };

    public static final String FIELDS_DEFAULT = "photo_id,verified,sex,photo_50,photo_100,photo_200_orig,photo_200,photo_400_orig,photo_max,photo_max_orig,online,domain,status,last_seen,nickname,can_write_private_message,screen_name,is_friend";
    /**
     * User ID.
     */
    public int id;
	
	public long last_seen;
    /**
     * First name of user.
     */
    public String first_name = "DELETED";
    /**
     * Last name of user.
     */
    public String last_name = "";
    /**
     * Status of User
     */
	public String full_name = "";
    public String status = "";
    /**
     * User page's screen name (subdomain)
     */
    public String screen_name;
    /**
     * Information whether the user is online.
     */
    public boolean online;

    /**
     * If user utilizes a mobile application or site mobile version,it returns online_mobile as additional.
     */
    public boolean online_mobile;

    /**
     * ID of mobile application,if user is online
     */
    public int online_app;

    /**
     * URL of default square photo of the user with 50 pixels in width.
     */
    public String photo_50 = "http://vk.com/images/camera_c.gif";

    /**
     * URL of default square photo of the user with 100 pixels in width.
     */
    public String photo_100 = "http://vk.com/images/camera_b.gif";

    /**
     * URL of default square photo of the user with 200 pixels in width.
     */
    public String photo_200 = "http://vk.com/images/camera_a.gif";
   
	public int sex;

    public static VKUser parse(JSONObject source) {
        VKUser user = new VKUser();
        user.id = source.optInt("id");
		user.sex = source.optInt("sex");
		user.last_seen = source.optLong("last_seen");
        user.first_name = source.optString("first_name");
        user.last_name = source.optString("last_name");
		user.full_name = user.first_name + " " + user.last_name;
        user.photo_50 = source.optString("photo_50", "http://vk.com/images/camera_c.gif");
        user.photo_100 = source.optString("photo_100","http://vk.com/images/camera_b.gif");
        user.photo_200 = source.optString("photo_200","http://vk.com/images/camera_a.gif");
        user.screen_name = source.optString("screen_name");
        user.online = source.optInt("online") == 1;
        user.status = source.optString("status");
        user.online_mobile = source.optInt("online_mobile") == 1;
        if (user.online_mobile) {
            user.online_app = source.optInt("online_app");
        }
//        Log.w("VKApi",String.format("name: %s,online: %s,online mobile: %s,online app %s",user.toString(),user.online,user.online_mobile,user.online_app));

        return user;
    }

    public static ArrayList<VKUser> parseUsers(JSONArray array) {
        ArrayList<VKUser> vkUsers = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            VKUser user = VKUser.parse(array.optJSONObject(i));
            vkUsers.add(user);
        }
        return vkUsers;
    }

    @Override
    public String toString() {
        if (full_name == null) {
            full_name = first_name.concat(" ").concat(last_name);
        }
        return full_name;
    }
}
