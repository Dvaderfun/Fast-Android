package com.procsec.fast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKChat;
import com.procsec.fast.vkapi.model.VKUser;
import com.squareup.picasso.Picasso;

public class ChatActivityInfo extends AppCompatActivity implements EditText.OnEditorActionListener {

    private long cid;

    private ListView lv;
    private EditText et;
    private ImageView ivAva;

    private String title;

    private VKChat chat;
    private VKUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar tb = findViewById(R.id.toolbar);

        ThemeManager.applyToolbarStyles(tb);

        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cid = getIntent().getExtras().getLong("cid");
        title = getIntent().getExtras().getString("title");
        lv = findViewById(R.id.lv);
        et = findViewById(R.id.et);
        ivAva = findViewById(R.id.ivAva);

        et.setText(title);
        et.setOnEditorActionListener(this);
        getSupportActionBar().setTitle(title);

        // new ChatInfoGetter().execute();
        getChatById();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    public void getChatById() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chat = VKApi.getChat(cid);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            et.setText(chat.title);
                            et.setSelection(et.getText().length());
                            Picasso.with(getApplicationContext()).load(chat.photo_100).placeholder(R.drawable.community_200).into(ivAva);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showChangeTitleDialog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!et.getText().toString().equals(chat.title))
                        VKApi.editChat(chat.id, et.getText().toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getChatById();
                            if (!et.getText().toString().equals(chat.title))
                                Toast.makeText(ChatActivityInfo.this, getString(R.string.title_changed), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_GO) {
            showChangeTitleDialog();
            return true;
        }
        return false;
    }
}
