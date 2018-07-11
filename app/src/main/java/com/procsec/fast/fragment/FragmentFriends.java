package com.procsec.fast.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.procsec.fast.MessagesActivity;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.util.Utils;
import com.procsec.fast.vkapi.model.VKUser;

import java.util.ArrayList;

import ru.lischenko_dev.fastmessenger.R;

public class FragmentFriends extends Fragment {

    private ListView lv;

    private ArrayList<VKUser> apiProfiles;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progress;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        lv = view.findViewById(R.id.lv);
        progress = view.findViewById(R.id.progress);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VKUser user = (VKUser) parent.getItemAtPosition(position);
                Intent intent = new Intent();
                intent.setClass(getActivity(), MessagesActivity.class);
                intent.putExtra("uid", user.id);
                intent.putExtra("title", user.toString());
                startActivity(intent);
            }
        });


        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Utils.hasConnection(getActivity()))
                    new FriendsLoader().execute();
                else
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
        swipeRefreshLayout.setColorSchemeColors(ThemeManager.color);

        new FriendsLoader().execute();
        return view;
    }

    private class FriendsLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            lv.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //CacheHelper.saveFriends();
            lv.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            super.onPostExecute(aVoid);
        }
    }
}
