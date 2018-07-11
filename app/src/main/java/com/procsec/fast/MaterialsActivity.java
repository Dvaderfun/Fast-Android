package com.procsec.fast;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.procsec.fast.adapter.MaterialsFragmentAdapter;
import com.procsec.fast.common.ThemeManager;
import com.procsec.fast.fragment.material.FragmentAudio;
import com.procsec.fast.fragment.material.FragmentDocuments;
import com.procsec.fast.fragment.material.FragmentPhoto;
import com.procsec.fast.fragment.material.FragmentVideo;

import ru.lischenko_dev.fastmessenger.R;


public class MaterialsActivity extends AppCompatActivity {

    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager viewPager;
    long uid, cid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materials);

        uid = getIntent().getExtras().getLong("uid");
        cid = getIntent().getExtras().getLong("cid");
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ThemeManager.applyToolbarStyles(toolbar);

        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setBackgroundColor(Color.WHITE);
        tabLayout.setTabTextColors(Color.GRAY, ThemeManager.color);
    }

    private void setupViewPager(ViewPager viewPager) {
        MaterialsFragmentAdapter adapter = new MaterialsFragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentPhoto(), getString(R.string.materials_photo));
        adapter.addFragment(new FragmentAudio(), getString(R.string.materials_audio));
        adapter.addFragment(new FragmentVideo(), getString(R.string.materials_video));
        adapter.addFragment(new FragmentDocuments(), getString(R.string.materials_doc));
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
