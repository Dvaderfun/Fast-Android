package com.procsec.fast.adapter;

import android.content.*;
import android.support.v7.widget.*;
import android.view.*;
import java.util.*;

public class BaseAdapterr<T, VH extends RecyclerView.ViewHolder>
extends RecyclerView.Adapter<VH> {
    private ArrayList<T> values;
    private ArrayList<T> cleanValues;

    protected Context context;
    protected LayoutInflater inflater;

    public BaseAdapterr(Context context, ArrayList<T> values) {
        this.context = context;
        this.values = values;

        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {

    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public T getItem(int position) {
        return values.get(position);
    }

    public void filter(String query) {
        String lowerQuery = query.toLowerCase();

        if (cleanValues == null) {
            cleanValues = new ArrayList<>(values);
        }
        values.clear();

        if (query.isEmpty()) {
            values.addAll(cleanValues);
        } else {
            for (T value : cleanValues) {
                if (onQueryItem(value, lowerQuery)) {
                    values.add(value);
                }
            }
        }

        notifyDataSetChanged();
    }

    public boolean onQueryItem(T item, String lowerQuery) {
        return false;
    }

    public ArrayList<T> getValues() {
        return values;
    }
}