package com.procsec.fast.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.procsec.fast.MessagesActivity;
import com.procsec.fast.R;
import com.procsec.fast.adapter.DialogAdapter;
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

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;

public class FragmentDialogs extends Fragment implements SwipeRefreshLayout.OnRefreshListener, DialogAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progress;
    private DialogAdapter adapter;
    private boolean loading;

    @Override
    public void onItemClick(View view, int position) {
        openChat(position, false);
    }

    @Override
    public void onItemLongClick(View view, int position) {
        showDialog(adapter.messages.get(position));
    }

    @Override
    public void onRefresh() {
        getDialogs(0, 30);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        recyclerView = view.findViewById(R.id.lv);
        progress = view.findViewById(R.id.progress);

        refreshLayout = view.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.color);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);

        getCachedDialogs(0, 30);
        getDialogs(0, 30);
        return view;
    }

    private void getCachedDialogs(int offset, int count) {
        ArrayList<VKMessage> dialogs = CacheStorage.getDialogs();
        if (ArrayUtil.isEmpty(dialogs)) {
            return;
        }

        createAdapter(dialogs, 0);
    }

    private void getDialogs(final int offset, final int count) {
        if (!Utils.hasConnection(getActivity())) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
            private ArrayList<VKMessage> messages;

            @Override
            public void ready() throws Exception {
                messages = new ArrayList<>();

                messages = VKApi.getMessagesDialogs(offset, count, null, null);

                if (messages.isEmpty()) {
                    loading = true;
                }

                if (offset == 0) {
                    CacheStorage.delete(DBHelper.DIALOGS_TABLE);
                    CacheStorage.insert(DBHelper.DIALOGS_TABLE, messages);
                }

                HashSet<Integer> userIds = new HashSet<>();
                HashSet<Integer> groupIds = new HashSet<>();

                if (offset == 0) {
                    userIds.add(VKApi.getAccount().id);
                }

                boolean hasGroups = false;
                for (VKMessage item : messages) {
                    if (VKGroup.isGroupId(item.user_id)) {
                        groupIds.add(VKGroup.toGroupId(item.user_id));
                        hasGroups = true;
                    } else {
                        userIds.add(item.user_id);
                        if (item.isChat() && !TextUtils.isEmpty(item.action)) {
                            userIds.add(item.action_mid);
                        }
                    }
                }

                final ArrayList<VKUser> users = VKApi.getProfiles(userIds, VKUser.FIELDS_DEFAULT);

                CacheStorage.insert(DBHelper.USERS_TABLE, users);

                if (hasGroups) {
                    ArrayList<VKGroup> groups = VKApi.getGroupsById(groupIds, "", "");
                    CacheStorage.insert(DBHelper.GROUPS_TABLE, groups);
                }
            }

            @Override
            public void done() {
                if (!isAdded()) {
                    return;
                }

                EventBus.getDefault().postSticky(MemoryCache.getUser(VKApi.getAccount().restore().id));
                createAdapter(messages, offset);
                refreshLayout.setRefreshing(false);

                if (!messages.isEmpty()) {
                    loading = false;
                }
            }

            @Override
            public void error(Exception e) {
                super.error(e);

                refreshLayout.setRefreshing(false);
            }
        });
    }

    private void createAdapter(ArrayList<VKMessage> messages, int offset) {
        if (ArrayUtil.isEmpty(messages)) {
            return;
        }
        if (offset != 0) {
            adapter.add(messages);
            adapter.notifyDataSetChanged();
            return;
        }

        if (adapter != null) {
            adapter.changeItems(messages);
            adapter.notifyDataSetChanged();
            return;
        }
        adapter = new DialogAdapter(getActivity(), messages);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void openChat(int position, boolean fromStart) {
        VKMessage item = adapter.messages.get(position);
        VKUser user = adapter.searchUser(item.user_id);
        VKGroup group = adapter.searchGroup(item.user_id);

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", adapter.getTitle(item, user, group));
        intent.putExtra("photo", adapter.getPhoto(item, user, group));
        intent.putExtra("user_id", user.id);
        intent.putExtra("chat_id", item.chat_id);
        intent.putExtra("group_id", group != null ? group.id : -1);
        if (false) {
            intent.putExtra("members_count", item.isChat() ? item.users_count : -1);
            intent.putExtra("from_start", fromStart);
        }
        startActivity(intent);
    }


    private void showDialog(final VKMessage item) {
        String[] items = new String[]{
                getString(R.string.clean_history)
        };
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                        adb.setMessage(R.string.confirm_delete_dialog);
                        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteMessages(item);
                            }
                        });
                        adb.setNegativeButton(R.string.no, null);
                        AlertDialog a = adb.create();
                        a.show();
                        break;
                }
            }
        });
        AlertDialog a = adb.create();
        a.show();
    }


    private void deleteMessages(final VKMessage item) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VKApi.deleteMessageDialog(item.user_id, item.chat_id);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.message_history_deleted, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("error delete msg", e.toString());
                    e.printStackTrace();
                }
            }
        }).start();
    }
}


