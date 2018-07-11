package com.procsec.fast.vkapi.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKAttachment implements Serializable
{
    private static final long serialVersionUID = 1L;
    public long id;//used only for wall post attached to message
    public String type; //photo,posted_photo,video,audio,link,note,app,poll,doc,geo,message,page,album
    public VKPhoto photo;
    //public Photo posted_photo; 
    public VKVideo video;
    public VKAudio audio;
    public VKGraffiti graffiti;
    public VKDocument document;
    public VKMessage message;
    
    public VKGift gift;
    public VKSticker sticker;

    public static final String TYPE_LINK = "link";
    public static final String TYPE_GRAFITY = "graffiti";
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_POLL = "poll";
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_DOC = "doc";
    public static final String TYPE_WALL = "wall";
    public static final String TYPE_PADE = "page";
    public static final String TYPE_GIFT = "gift";
    public static final String TYPE_STICKER = "sticker";
    public static final String TYPE_ALUMB = "album";
    public static final String TYPE_GEO = "geo";


    public static ArrayList<VKAttachment> parseAttachments(JSONArray attachments, long from_id, long copy_owner_id, JSONObject geo_json) throws JSONException {
        ArrayList<VKAttachment> attachments_arr = new ArrayList<>();
        if(attachments != null)
		{
            int size = attachments.length();
            for(int j = 0; j < size; ++j)
			{
                Object att = attachments.opt(j);
                if(!(att instanceof JSONObject))
                    continue;
                JSONObject json_attachment = (JSONObject) att;
                VKAttachment attachment = new VKAttachment();
                attachment.type = json_attachment.optString("type");
                switch(attachment.type)
				{
                    case "photo":
                    case "posted_photo":
                        JSONObject x = json_attachment.optJSONObject("photo");
                        if(x != null)
                            attachment.photo = new VKPhoto(x);
                        break;
                    case "graffiti":
                        attachment.graffiti = VKGraffiti.parse(json_attachment.optJSONObject("graffiti"));
                        break;
                    
                    case "audio":
                        attachment.audio = VKAudio.parse(json_attachment.optJSONObject("audio"));
                        break;
                    
                    case "video":
                        attachment.video = VKVideo.parseForAttachments(json_attachment.optJSONObject("video"));
                        break;
                    
                    case "doc":
                        attachment.document = VKDocument.parse(json_attachment.optJSONObject("doc"));
                        break;
					case "message":
						attachment.message = VKMessage.parse(json_attachment.optJSONObject("message"));
						break;
                    
                    case "gift":
                        attachment.gift = VKGift.parse(json_attachment.optJSONObject("gift"));
                        break;
                    
                    case "sticker":
                        attachment.sticker = VKSticker.parse(json_attachment.optJSONObject("sticker"));
                        break;

                }
                attachments_arr.add(attachment);
            }
        }
        return attachments_arr;
    }

	public String getStringAttachment() {
		switch(type) {
			case "photo":
            case "posted_photo":
				return "Фото";
			case "message":
				return "Пересланное сообщение";
			case "graffiti":
				return "Граффити";
			case "geo":
				return "Координаты";
			case "link":
				return "Ссылка";
			case "audio":
				return "Аудио";
			case "note":
				return "Заметка";
			case "video":
				return "Видео";
			case "poll":
				return "Опрос";
			case "doc":
				return "Документ";
			case "wall":
				return "Пост";
			case "page":
				return "Страница";
			case "gift":
				return "Подарок";
			case "album":
				return "Альбом";
			case "sticker":
				return "Стикер";
		}

		return null;
	}

    public static ArrayList<VKAttachment> parseArray(JSONArray sourse) throws JSONException {
        return parseAttachments(sourse, 0, 0, null);
    }
}
