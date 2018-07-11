package com.procsec.fast.adapter;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.procsec.fast.MessagesActivity;
import com.procsec.fast.R;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.util.ArrayUtil;
import com.procsec.fast.util.Utils;
import com.procsec.fast.view.CircleImageView;
import com.procsec.fast.vkapi.model.VKMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;


public class MessageAdapter extends BaseAdapter<VKMessage, MessageAdapter.ViewHolder> {

    private int chatId;
    private int userId;

    private LayoutInflater inflater;

    public MessageAdapter(Context context, ArrayList<VKMessage> messages, int chatId, int userId) {
        super(context, messages);

        this.chatId = chatId;
        this.userId = userId;

        this.inflater = LayoutInflater.from(context);

        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessage(VKMessage message) {
        if (message.is_out) {
            return;
        }

        if (message.chat_id == chatId && message.isChat() || message.user_id == userId) {
            getValues().add(message);
            notifyDataSetChanged();
        }
        MessagesActivity root = (MessagesActivity) context;
        root.getRecycler().scrollToPosition(getMessagesCount());
    }

    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.activity_messages_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageAdapter.ViewHolder holder, int position) {

        final VKMessage item = getItem(position);

        holder.main_container.setGravity(item.is_out ? Gravity.END : Gravity.START);

        holder.body.setText(item.body + "        ");

        Drawable bg = context.getResources().getDrawable(R.drawable.msg_bg);
        bg.setTint(item.is_out ? ThemeManager.color : Color.LTGRAY);

        holder.body.setBackground(bg);

        holder.date.setText(Utils.parseDate(item.date * 1000));

    }

    public int getMessagesCount() {
        return getValues().size();
    }

    public void changeItems(ArrayList<VKMessage> messages) {
        if (!ArrayUtil.isEmpty(messages)) {
            this.getValues().clear();
            this.getValues().addAll(messages);
        }
    }

    public void add(VKMessage message, boolean anim) {
        getValues().add(message);
        if (anim) {
            notifyItemInserted(getValues().size() - 1);
        } else {
            notifyDataSetChanged();
        }
    }

    public void insert(ArrayList<VKMessage> messages) {
        this.getValues().addAll(0, messages);
    }

    public void change(VKMessage message) {
        for (int i = 0; i < getValues().size(); i++) {
            if (getValues().get(i).date == message.date) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatar;

        TextView body;
        TextView date;

        LinearLayout main_container;
        LinearLayout body_container;
        LinearLayout attach_container;
        LinearLayout bubble;

        public ViewHolder(View v) {
            super(v);

            body = v.findViewById(R.id.body);
            date = v.findViewById(R.id.date);

            main_container = v.findViewById(R.id.main_container);
            body_container = v.findViewById(R.id.body_container);
            attach_container = v.findViewById(R.id.attach_container);
            bubble = v.findViewById(R.id.bubble);
        }
    }

    public class SendStatus {
        public static final int ERROR = -1;
        public static final int SENDING = 0;
        public static final int SENT = 1;
    }

}
