package com.procsec.fast.adapter;

import com.procsec.fast.vkapi.model.*;


public class MessageItem {
    public VKMessage message;
    public VKUser user;

    public MessageItem(VKMessage message, VKUser user) {
        this.user = user;
        this.message = message;
    }

}
