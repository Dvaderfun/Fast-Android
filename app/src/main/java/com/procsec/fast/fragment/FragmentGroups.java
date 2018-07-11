package com.procsec.fast.fragment;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.util.Utils;
import com.procsec.fast.vkapi.VKApi;
import com.procsec.fast.vkapi.model.VKGroup;

import java.util.ArrayList;

import ru.lischenko_dev.fastmessenger.R;

public class FragmentGroups extends Fragment {

    private ListView lv;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        lv = view.findViewById(R.id.lv);
        progressBar = view.findViewById(R.id.progress);


        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Utils.hasConnection(getActivity()))
                    new GroupsLoader().execute();
                else
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
        swipeRefreshLayout.setColorSchemeColors(ThemeManager.color);


        if (Utils.hasConnection(getActivity()))
            new GroupsLoader().execute();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                VKGroup group = (VKGroup) adapterView.getItemAtPosition(i);
                showDialog(group);
            }
        });
        return view;
    }

    private void showDialog(final VKGroup group) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VKGroup gr = VKApi.getGroup(group.id);
                    final String info = "Участников: " + String.valueOf(gr.members_count) + "\nScreen Name: " + gr.screen_name + "\nСтатус: " + (gr.status.length() > 0 ? gr.status : "отсутствует");

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                            adb.setTitle(group.name);
                            adb.setMessage(info);
                            adb.setPositiveButton(R.string.ok, null);
                            AlertDialog a = adb.create();
                            a.show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private class GroupsLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            lv.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ArrayList<VKGroup> apiGroups = VKApi.getGroups(VKApi.getAccount().id);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //   GroupsAdapter adapter = new GroupsAdapter(getActivity(), apiGroups);
                                // lv.setAdapter(adapter);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            lv.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            super.onPostExecute(aVoid);
        }
    }


}
