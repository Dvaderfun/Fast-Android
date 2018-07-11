package com.procsec.fast;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.procsec.fast.common.OTAManager;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.db.MemoryCache;
import com.procsec.fast.helper.FragmentTransHelper;
import com.procsec.fast.service.LongPollService;
import com.procsec.fast.util.Account;
import com.procsec.fast.util.Requests;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKUser;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView drawer_view;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;

    private Account account;

    public static void setTitle(ActionBar ab, String title) {
        ab.setTitle(title);
    }

    public static void setTitle(Toolbar tb, String title) {
        tb.setTitle(title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        account = VKApi.getAccount();

        FragmentTransHelper.init();
        ThemeManager.init();
        OTAManager.checkUpdate(this);

        startService(new Intent(this, LongPollService.class));

        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(0);

        setContentView(R.layout.activity_main);
        initItems();
        checkLogin();

        EventBus.getDefault().register(this);

        ThemeManager.applyToolbarStyles(toolbar);

        setSupportActionBar(toolbar);

        drawer.addDrawerListener(toggle);
        toggle.getDrawerArrowDrawable().setColor(ThemeManager.color);
        toggle.syncState();

        drawer_view.setNavigationItemSelectedListener(this);

        drawer_view.setCheckedItem(R.id.nav_messages);
        changeTitle("Messages");
    }

    @Subscribe(sticky = true)
    public void onUpdateUser(VKUser u) {
        if (u != null) {
            account.photo_200 = u.photo_200;
            account.photo_50 = u.photo_50;
            account.status = "@id" + account.id;
            account.name = u.first_name;
            account.surname = u.last_name;
            account.makeFullName();
            account.save();

            applyUserData();
        }
    }

    private void applyUserData() {
        View header = drawer_view.getHeaderView(0);

        VKUser u = MemoryCache.getUser(account.id);

        ImageView avatar = header.findViewById(R.id.avatar);
        TextView name = header.findViewById(R.id.name);
        TextView status = header.findViewById(R.id.status);

        if (u != null) {
            account.photo_200 = u.photo_200;
            account.photo_50 = u.photo_50;
            account.status = "@id" + account.id;
            account.name = u.first_name;
            account.surname = u.last_name;
            account.makeFullName();
            account.save();

            name.setText(account.full_name);
            status.setText(account.status);

            try {
                Picasso.with(this).load(account.photo_200).placeholder(R.drawable.camera_200).into(avatar);
            } catch (Exception ignored) {
            }
        }
    }

    private void checkLogin() {
        account.restore();
        if (account.id == 0) {
            startActivityForResult(new Intent(this, LoginActivity.class), Requests.REQUEST_LOGIN);
        } else {
            FragmentTransHelper.addFragmentsToStack(this);
            onItemSelected(R.id.nav_messages);
            applyUserData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Requests.REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                account.id = data.getIntExtra("id", 0);
                account.token = data.getStringExtra("token");
                account.save();
                new Handler().post(new Runnable() {

                    @Override
                    public void run() {
                        FragmentTransHelper.addFragmentsToStack(MainActivity.this);
                        onItemSelected(R.id.nav_messages);
                    }
                });
                applyUserData();
            } else {
                finish();
            }
        }
    }

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.warning));
        adb.setMessage(getString(R.string.exit_message));
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                VKApi.getAccount().clear();
                finishAffinity();
            }
        });
        adb.setNegativeButton(R.string.no, null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        stopService(new Intent(this, LongPollService.class));
    }

    private void replaceFragment(Fragment f) {
        FragmentTransHelper.replaceFragment(getSupportFragmentManager(), f);
    }

    private void initItems() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawer_view = (NavigationView) findViewById(R.id.nav_view);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        onItemSelected(item.getItemId());
        return true;
    }

    private void onItemSelected(int id) {
        String title = null;
        switch (id) {
            case R.id.nav_messages:
                title = "Messages";
                replaceFragment(FragmentTransHelper.fragmentDialogs);
                break;
            case R.id.nav_friends:
                title = "Friends";
                replaceFragment(FragmentTransHelper.fragmentFriends);
                break;
            case R.id.nav_groups:
                title = "Groups";
                replaceFragment(FragmentTransHelper.fragmentGroups);
                break;
            case R.id.nav_settings:
                title = "Settings";
                replaceFragment(FragmentTransHelper.fragmentSettings);
                break;
            case R.id.nav_exit:
                showExitDialog();
                break;
        }

        changeTitle(title);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
    }

    public void changeTitle(String title) {
        ActionBar ab = getSupportActionBar();

        if (ab != null) {
            ab.setTitle(title);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(drawer_view)) {
            drawer.closeDrawer(drawer_view);
        } else {
            super.onBackPressed();
        }
    }
}
