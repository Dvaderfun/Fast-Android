package com.procsec.fast;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.procsec.fast.adapter.MessageAdapter;
import com.procsec.fast.common.FApp;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.concurrent.AsyncCallback;
import com.procsec.fast.concurrent.ThreadExecutor;
import com.procsec.fast.db.CacheStorage;
import com.procsec.fast.db.DBHelper;
import com.procsec.fast.db.MemoryCache;
import com.procsec.fast.util.ArrayUtil;
import com.procsec.fast.util.Utils;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKGroup;
import com.procsec.fast.vkapi.model.VKMessage;
import com.procsec.fast.vkapi.model.VKUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

public class MessagesActivity extends AppCompatActivity implements TextWatcher {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ImageButton send, attach, smiles;
    private EditText message;
    private ProgressBar progress;
    private LinearLayoutManager layoutManager;
    private MessageAdapter adapter;
    private String title, photo;
    private int userId, chatId, groupId, membersCount;
    private boolean loading;
    private boolean chronologyOrder;

    @Override
    public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
    }

    @Override
    public void afterTextChanged(Editable p1) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        getIntentData();
        initViews();

        Toast.makeText(this, userId + "", 0).show();

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle(getSubtitleStatus());

        ThemeManager.applyToolbarStyles(toolbar);

        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(layoutManager);

        message.addTextChangedListener(this);

        getCachedMessages();

        if (Utils.hasConnection(this)) {
            getMessages(0);
        }
    }

    @Override
    public void onTextChanged(CharSequence cs, int p2, int p3, int p4) {
        send.getDrawable().setTint(cs.length() == 0 ? Color.GRAY : ThemeManager.color);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat_history, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menuUpdate:
                getMessages(0);
                break;
            case R.id.menuMaterials:
                break;
            case R.id.menuFind:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.list);
        send = findViewById(R.id.send);
        attach = findViewById(R.id.attach);
        smiles = findViewById(R.id.smile);
        message = findViewById(R.id.message);
        progress = findViewById(R.id.progress);
    }


    public RecyclerView getRecycler() {
        return recyclerView;
    }

    private void getIntentData() {
        Intent intent = getIntent();

        this.title = intent.getStringExtra("title");
        this.userId = intent.getIntExtra("user_id", -1);
        this.chatId = intent.getIntExtra("chat_id", -1);
        this.groupId = intent.getIntExtra("group_id", -1);
        this.membersCount = intent.getIntExtra("members_count", -1);
        this.photo = intent.getStringExtra("photo");
        this.chronologyOrder = intent.getBooleanExtra("from_start", false);
    }

    private void createAdapter(ArrayList<VKMessage> messages) {
        if (adapter != null) {
            adapter.changeItems(messages);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new MessageAdapter(this, messages, chatId, userId);
            recyclerView.setAdapter(adapter);
            if (chronologyOrder) {
                recyclerView.scrollToPosition(0);
            } else {
                recyclerView.scrollToPosition(adapter.getMessagesCount());
            }
        }
    }

    private void insertMessages(ArrayList<VKMessage> messages) {
        if (adapter != null) {
            if (!chronologyOrder) {
                adapter.insert(messages);
                adapter.notifyItemRangeInserted(0, messages.size());
            } else {
                adapter.getValues().addAll(messages);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void getCachedMessages() {
        ArrayList<VKMessage> messages = CacheStorage.getMessages(userId, chatId);
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages);
        }
    }

    private String getSubtitleStatus() {
        if (groupId > 0) {
            return "Community";
        }

        Locale locale = FApp.locale;

        VKUser user = MemoryCache.getUser(userId);
        if (user == null) {
            return "";
        }
        if (user.online) {
            String status = "Online";
            if (user.online_mobile) {
                status += " from mobile";
            }
            return status;
        }

        return String.format(locale, "Last seen at ",
                Utils.parseDate(user.last_seen * 1000));
    }

    private long getPeerId() {
        return Utils.getPeerId(userId, chatId, groupId);
    }

    private void getUserIds(HashSet<Integer> ids, ArrayList<VKMessage> messages) {
        for (VKMessage msg : messages) {
            if (!VKGroup.isGroupId(msg.user_id)) {
                ids.add(msg.user_id);
            }

            if (!ArrayUtil.isEmpty(msg.fws_messages)) {
                getUserIds(ids, msg.fws_messages);
            }
        }
    }

    private void getUsers(ArrayList<VKMessage> messages) {
        final HashSet<Integer> ids = new HashSet<>();
        getUserIds(ids, messages);

        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = VKApi.getProfiles(ids, VKUser.FIELDS_DEFAULT);
            }

            @Override
            public void done() {
                if (ArrayUtil.isEmpty(users)) {
                    return;
                }

                CacheStorage.insert(DBHelper.USERS_TABLE, users);
                MemoryCache.update(users);
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(FApp.context, e.toString(), Toast.LENGTH_LONG).show();
                Log.e("Load users", "Error:");
                e.printStackTrace();
            }
        });
    }

    private void getMessages(final int offset) {
        loading = true;
        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKMessage> messages;

            @Override
            public void ready() throws Exception {
                messages = VKApi.getMessagesHistory(getPeerId(), chronologyOrder, 0, 30);
            }

            @Override
            public void done() {
                if (!chronologyOrder) {
                    Collections.reverse(messages);
                }
                if (offset == 0) {
                    CacheStorage.deleteMessages(userId, chatId);
                    CacheStorage.insert(DBHelper.MESSAGES_TABLE, messages);
                    createAdapter(messages);
                } else {
                    insertMessages(messages);
                }
                loading = messages.isEmpty();
                if (!messages.isEmpty()) {
                    getUsers(messages);
                }
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(FApp.context, e.toString(), Toast.LENGTH_LONG).show();
                Log.e("Load messages", "Error:");
                e.printStackTrace();
            }

        });

        if (offset != 0) {
            return;
        }

        if (chatId > 0 || groupId > 0) {
            return;
        }

        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = new ArrayList<>();

                ArrayList<Integer> id = new ArrayList<>();
                id.add(userId);

                VKUser user = VKApi.getProfiles(id, VKUser.FIELDS_DEFAULT).get(0);
                users.add(user);
            }

            @Override
            public void done() {
                CacheStorage.insert(DBHelper.USERS_TABLE, users);
                getSupportActionBar().setSubtitle(getSubtitleStatus());
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(FApp.context, e.toString(), Toast.LENGTH_LONG).show();
                Log.e("Load user", "Error:");
                e.printStackTrace();
            }

        });
    }
}
