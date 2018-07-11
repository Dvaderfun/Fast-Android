package com.procsec.fast.adapter;

import android.content.*;
import android.graphics.drawable.*;
import android.support.v4.content.*;
import android.support.v7.widget.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.procsec.fast.*;
import com.procsec.fast.common.*;
import com.procsec.fast.db.*;
import com.procsec.fast.util.*;
import com.procsec.fast.vkapi.*;
import com.procsec.fast.vkapi.model.*;
import com.squareup.picasso.*;
import java.text.*;
import java.util.*;
import org.greenrobot.eventbus.*;
import android.graphics.*;

import ru.lischenko_dev.fastmessenger.R;

public class DialogAdapter extends RecyclerView.Adapter<DialogAdapter.ViewHolder> {

    public ArrayList<VKMessage> messages;

    private LayoutInflater inflater;
    private Context context;
	
	private Account account;

    private Comparator<VKMessage> comparator;
    private OnItemClickListener listener;
    private int position;
	
    public DialogAdapter(ArrayList<VKMessage> messages) {
        this.messages = messages;
		this.account = VKApi.getAccount();
        this.context = FApp.context;
        this.inflater = LayoutInflater.from(context);
		
        comparator = new Comparator<VKMessage>() {
            @Override
            public int compare(VKMessage o1, VKMessage o2) {
                long x = o1.date;
                long y = o2.date;

                return (x > y) ? -1 : ((x == y) ? 1 : 0);
            }
        };

        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessage(VKMessage message) {
        int index = searchMessageIndex(message.user_id, message.chat_id);
        if (index >= 0) {
            VKMessage current = messages.get(index);
            current.id = message.id;
            current.body = message.body;
            current.title = message.title;
            current.date = message.date;
            current.user_id = message.user_id;
            current.chat_id = message.chat_id;
            current.read_state = message.read_state;
            current.is_out = message.is_out;
            current.unread++;
            if (current.is_out) {
                current.unread = 0;
            }

            Collections.sort(messages, comparator);
            notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReadMessage(Integer id) {
        VKMessage message = searchMessage(id);
        if (message != null) {
            message.read_state = true;
            message.unread = 0;
			
            notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.fragment_messages_list, parent, false);
        return new ViewHolder(v);
    }
	
	@Override
	public void onBindViewHolder(DialogAdapter.ViewHolder holder, int pos) {
		this.position = pos;
		initListeners(holder.itemView, pos);
		
		VKMessage msg = messages.get(pos);
		VKUser user = searchUser(msg.user_id);
        VKGroup group = searchGroup(msg.user_id);
		
		holder.title.setText(getTitle(msg, user, group));
		
		if (msg.isChat() && group != null) {
			holder.title.setText(msg.title);
		}
		
		holder.date.setText(new SimpleDateFormat("HH:mm").format(msg.date * 1000));
		
		holder.body.setText(msg.body);
		
		String avatar_image = "";
		
		if (group == null) {
			if (msg.isChat()) {
				avatar_image = msg.photo_100;
			} else {
				avatar_image = user.photo_100;
			}
		} else if (msg.isChat() && group != null) {
			avatar_image = msg.photo_100;
		} else {
			avatar_image = group.photo_100;
		}
		
		String small_avatar_image = msg.is_out ? VKApi.getAccount().restore().photo_50 : user.photo_50;
		
		if (msg.isChat() && group != null && !msg.is_out) {
			small_avatar_image = group.photo_50;
		}
		
		if (avatar_image.length() == 0) {
			avatar_image = "http://vk.com/images/camera_a.gif";
		}
		
		if (small_avatar_image.length() == 0) {
			small_avatar_image = "http://vk.com/images/camera_c.gif";
		}
		
		try {
			Picasso.with(FApp.context).load(avatar_image).placeholder(R.drawable.camera_200).into(holder.avatar);
			Picasso.with(FApp.context).load(small_avatar_image).placeholder(R.drawable.camera_200).into(holder.avatar_small);
		} catch (Exception e) {
			Log.e("Load avat from messages", "Error:");
			e.printStackTrace();
		}
		
		if (!msg.is_out && !msg.isChat()) {
			holder.avatar_small.setVisibility(View.GONE);
		} else {
			holder.avatar_small.setVisibility(View.VISIBLE);
		}
		
		if (msg.is_out && !msg.read_state) {
			Drawable unread = FApp.context.getResources().getDrawable(R.drawable.ic_not_read_body);
			
			holder.container.setBackground(unread);
		} else {
			holder.container.setBackground(null);
		}
		
		if (TextUtils.isEmpty(msg.action)) {
            if ((!ArrayUtil.isEmpty(msg.attachments)
				|| !ArrayUtil.isEmpty(msg.fws_messages))
				&& TextUtils.isEmpty(msg.body)) {
                String body = getAttachmentBody(msg.attachments, msg.fws_messages);
                SpannableString span = new SpannableString(body);
                span.setSpan(new ForegroundColorSpan(ThemeManager.color), 0, body.length(), 0);

                holder.body.append(span);
            }
        } else {
            String body = getActionBody(msg);
            SpannableString span = new SpannableString(body);
            span.setSpan(new ForegroundColorSpan(ThemeManager.color), 0, body.length(), 0);

            holder.body.setText(span);
        }
		
		if ((user != null && user.online) && !msg.isChat()) {
            holder.online.setVisibility(View.VISIBLE);
            holder.online.setImageDrawable(getOnlineIndicator(user));
        } else {
            holder.online.setVisibility(View.GONE);
        }
		
		if (!msg.is_out && !msg.read_state) {
			holder.counter.setVisibility(View.VISIBLE);
			holder.counter.setText(msg.unread + "");
			
			GradientDrawable gd = new GradientDrawable();
			gd.setColor(ThemeManager.color);
			gd.setCornerRadius(60);
			
			holder.counter.setBackground(gd);
		} else {
			holder.counter.setVisibility(View.GONE);
		}
		
		if (msg.read_state && msg.unread == 0) {
			holder.container.setBackground(null);
			holder.container.setBackgroundColor(Color.TRANSPARENT);
		}
	}
	
	public int getCurrentPosition() {
        return position;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void add(ArrayList<VKMessage> messages) {
        this.messages.addAll(messages);
    }

    public void remove(int position) {
        messages.remove(position);
    }

    public String getTitle(VKMessage msg, VKUser user, VKGroup group) {
        return group != null
			? group.name : msg.isChat()
			? msg.title : user.first_name.concat(" ").concat(user.last_name);
    }

    public String getPhoto(VKMessage msg, VKUser user, VKGroup group) {
        if (msg.isChat() && !TextUtils.isEmpty(msg.photo_200)) {
            return msg.photo_200;
        }
        return group != null
			? group.photo_200 : user.photo_200;
    }

    public void changeItems(ArrayList<VKMessage> messages) {
        if (!ArrayUtil.isEmpty(messages)) {
            this.messages.clear();
            this.messages.addAll(messages);
        }
    }

    private String getActionBody(VKMessage msg) {
        switch (msg.action) {
            case VKMessage.ACTION_CHAT_KICK_USER:
                if (msg.user_id == msg.action_mid) {
                    return MemoryCache.getUser(msg.user_id) + " leaved from chat";
                } else return MemoryCache.getUser(msg.user_id) + " kicked " + MemoryCache.getUser(msg.action_mid) + " from chat";

            case VKMessage.ACTION_CHAT_INVITE_USER:
                VKUser owner = MemoryCache.getUser(msg.user_id);
                VKUser invited = MemoryCache.getUser(msg.action_mid);

                return owner + " invited " + invited + " in the chat";

            case VKMessage.ACTION_CHAT_PHOTO_UPDATE:
                return "updated chat photo";

            case VKMessage.ACTION_CHAT_PHOTO_REMOVE:
                return "removed chat photo";

            case VKMessage.ACTION_CHAT_TITLE_UPDATE:
                return "updated chat title to «" + msg.action_text + "»";

            case VKMessage.ACTION_CHAT_CREATE:
                return "created chat «" + msg.action_text + "»";
        }

        return "";
    }

    private String getAttachmentBody(ArrayList<VKAttachment> attachments, ArrayList<VKMessage> forwards) {
        if (ArrayUtil.isEmpty(attachments)) {
            return "";
        }
        VKAttachment attach = attachments.get(0);
        String s = attach.type;

        
        if (!ArrayUtil.isEmpty(forwards) && s.length() == 0) {
            s = forwards.size() > 1 ? "Forward messages"
				: "Forward message";
        }

        return (s);
    }

    private void initListeners(View v, final int position) {
        v.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (listener != null) {
						listener.onItemLongClick(v, position);
					}
					return true;
				}
			});
        v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onItemClick(v, position);
					}
				}
			});
    }

    public VKUser searchUser(int id) {
        VKUser user = MemoryCache.getUser(id);
        if (user == null) {
            user = VKUser.EMPTY;
        }
        return user;
    }

    public VKGroup searchGroup(int id) {
        if (!VKGroup.isGroupId(id)) {
            return null;
        }
        return MemoryCache.getGroup(VKGroup.toGroupId(id));
    }

    public int searchMessageIndex(int userId, int chatId) {
        for (int i = 0; i < messages.size(); i++) {
            VKMessage msg = messages.get(i);
            if (msg.chat_id == chatId && chatId > 0) {
                return i;
            }

            if (msg.user_id == userId && chatId == 0) {
                return i;
            }
        }
        return -1;
    }

    public VKMessage searchMessage(int id) {
        for (int i = 0; i < messages.size(); i++) {
            VKMessage msg = messages.get(i);
            if (msg.id == id) {
                return msg;
            }
        }
        return null;
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);

        messages.clear();
        listener = null;
    }

    private Drawable getOnlineIndicator(VKUser user) {
        return getOnlineIndicator(context, user);
    }

    public static Drawable getOnlineIndicator(Context context, VKUser user) {
       /*
		int resource = R.drawable.ic_vector_smartphone;
        if (user.online_mobile) {
            // online from mobile app
            switch (user.online_app) {
                case Identifiers.EUPHORIA:
                    resource = R.drawable.ic_vector_pets;
                    break;

                case Identifiers.ANDROID_OFFICIAL:
                    resource = R.drawable.ic_vector_android;
                    break;

                case Identifiers.WP_OFFICIAL:
                case Identifiers.WP_OFFICIAL_NEW:
                case Identifiers.WINDOWS_OFFICIAL:
                    resource = R.drawable.ic_vector_win;
                    break;

                case Identifiers.IPAD_OFFICIAL:
                case Identifiers.IPHONE_OFFICIAL:
                    resource = R.drawable.ic_vector_apple;
                    break;

                case Identifiers.KATE_MOBILE:
                    resource = R.drawable.ic_kate;
                    break;

                case Identifiers.ROCKET:
                    resource = R.drawable.ic_vector_rocket;
                    break;

                case Identifiers.LYNT:
//                    resource = R.drawable.ic_lynt;
                    break;

                case Identifiers.SWEET:
                    resource = R.drawable.ic_vector_sweet;
                    break;

                case Identifiers.PHOENIX:
                case Identifiers.MESSENGER:
                    resource = R.drawable.ic_phoenix;
                    break;

                default:
                    if (user.online_app > 0) {
                        // other unknown mobile app
                        resource = R.drawable.ic_vector_settings;
                    }
            }
        } else {
            // online from desktop (PC)
            resource = R.drawable.ic_vector_web;
        }*/

        return ContextCompat.getDrawable(context, R.drawable.ic_online_circle);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatar;
		private ImageView avatar_small;
        private ImageView online;

        private TextView title;
        private TextView body;
        private TextView date;
        private TextView counter;
		
		private LinearLayout container;

        public ViewHolder(View v) {
            super(v);
			
			this.avatar_small = v.findViewById(R.id.avatar_small);
            this.avatar = v.findViewById(R.id.avatar);
            this.online = v.findViewById(R.id.online);
            
            this.title = v.findViewById(R.id.title);
            this.body = v.findViewById(R.id.body);
            this.date = v.findViewById(R.id.date);
            this.counter = v.findViewById(R.id.counter);
			
			this.container = v.findViewById(R.id.container);
        }
    }
	
}
