package me.wcy.ponymusic.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.Bind;
import me.wcy.ponymusic.R;
import me.wcy.ponymusic.adapter.FragmentAdapter;
import me.wcy.ponymusic.fragment.LocalMusicFragment;
import me.wcy.ponymusic.fragment.OnlineMusicFragment;
import me.wcy.ponymusic.fragment.PlayFragment;
import me.wcy.ponymusic.model.LocalMusic;
import me.wcy.ponymusic.service.OnPlayerEventListener;
import me.wcy.ponymusic.service.PlayService;
import me.wcy.ponymusic.utils.CoverLoader;
import me.wcy.ponymusic.utils.MusicUtils;

public class MusicActivity extends BaseActivity implements View.OnClickListener, OnPlayerEventListener, NavigationView.OnNavigationItemSelectedListener {
    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @Bind(R.id.navigation_view)
    NavigationView navigationView;
    @Bind(R.id.tabs)
    TabLayout mTabLayout;
    @Bind(R.id.viewpager)
    ViewPager mViewPager;
    @Bind(R.id.fl_play_bar)
    FrameLayout flPlayBar;
    @Bind(R.id.iv_play_bar_cover)
    ImageView ivPlayBarCover;
    @Bind(R.id.tv_play_bar_title)
    TextView tvPlayBarTitle;
    @Bind(R.id.tv_play_bar_artist)
    TextView tvPlayBarArtist;
    @Bind(R.id.iv_play_bar_play)
    ImageView ivPlayBarPlay;
    @Bind(R.id.iv_play_bar_next)
    ImageView ivPlayBarNext;
    @Bind(R.id.pb_play_bar)
    ProgressBar mProgressBar;
    private View navigationHeader;
    private LocalMusicFragment mLocalMusicFragment;
    private OnlineMusicFragment mOnlineMusicFragment;
    private PlayFragment mPlayFragment;
    private PlayService mPlayService;
    private PlayServiceConnection mPlayServiceConnection;
    private ProgressDialog mProgressDialog;
    private boolean mIsPlayFragmentShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.show();
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClass(this, PlayService.class);
        mPlayServiceConnection = new PlayServiceConnection();
        bindService(intent, mPlayServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private class PlayServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayService = ((PlayService.PlayBinder) service).getService();
            mPlayService.setOnPlayEventListener(MusicActivity.this);
            init();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private void init() {
        setupView();
        onChange(mPlayService.getPlayingPosition());
        mProgressDialog.cancel();
    }

    @Override
    protected void setListener() {
        flPlayBar.setOnClickListener(this);
        ivPlayBarPlay.setOnClickListener(this);
        ivPlayBarNext.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupView() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        // add navigation header
        navigationHeader = LayoutInflater.from(this).inflate(R.layout.navigation_header, null);
        navigationView.addHeaderView(navigationHeader);

        // setup view pager
        mLocalMusicFragment = new LocalMusicFragment();
        mOnlineMusicFragment = new OnlineMusicFragment();
        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(mLocalMusicFragment, getString(R.string.local_music));
        adapter.addFragment(mOnlineMusicFragment, getString(R.string.online_music));
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    /**
     * 更新播放进度
     */
    @Override
    public void onPublish(int progress) {
        mProgressBar.setProgress(progress);
        if (mPlayFragment != null && mPlayFragment.isResumed()) {
            mPlayFragment.onPublish(progress);
        }
    }

    @Override
    public void onChange(int position) {
        onPlay(position);
        if (mPlayFragment != null && mPlayFragment.isResumed()) {
            mPlayFragment.onChange(position);
        }
    }

    @Override
    public void onPlayerPause() {
        ivPlayBarPlay.setImageResource(R.drawable.ic_playbar_btn_play);
        if (mPlayFragment != null && mPlayFragment.isResumed()) {
            mPlayFragment.onPlayerPause();
        }
    }

    @Override
    public void onPlayerResume() {
        ivPlayBarPlay.setImageResource(R.drawable.ic_playbar_btn_pause);
        if (mPlayFragment != null && mPlayFragment.isResumed()) {
            mPlayFragment.onPlayerResume();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fl_play_bar:
                showPlayingFragment();
                break;
            case R.id.iv_play_bar_play:
                play();
                break;
            case R.id.iv_play_bar_next:
                next();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        drawerLayout.closeDrawers();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                item.setChecked(false);
            }
        }, 500);
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            case R.id.action_setting:
                return true;
            case R.id.action_share:
                return true;
            case R.id.action_about:
                return true;
        }
        return false;
    }

    public void onPlay(int position) {
        if (MusicUtils.getMusicList().isEmpty()) {
            return;
        }

        LocalMusic localMusic = MusicUtils.getMusicList().get(position);
        Bitmap cover = CoverLoader.getInstance().loadThumbnail(localMusic.getCoverUri());
        ivPlayBarCover.setImageBitmap(cover);
        tvPlayBarTitle.setText(localMusic.getTitle());
        tvPlayBarArtist.setText(localMusic.getArtist());
        if (getPlayService().isPlaying()) {
            ivPlayBarPlay.setImageResource(R.drawable.ic_playbar_btn_pause);
        } else {
            ivPlayBarPlay.setImageResource(R.drawable.ic_playbar_btn_play);
        }
        mProgressBar.setMax((int) localMusic.getDuration());
        mProgressBar.setProgress(0);

        if (mLocalMusicFragment != null && mLocalMusicFragment.isResumed()) {
            mLocalMusicFragment.onItemPlay(position);
        }
    }

    private void play() {
        if (getPlayService().isPlaying()) {//正在播放
            getPlayService().pause();
        } else {
            if (getPlayService().isPause()) {//暂停
                getPlayService().resume();
            } else {//还未开始播放
                getPlayService().play(getPlayService().getPlayingPosition());
            }
        }
    }

    private void next() {
        getPlayService().next();
    }

    public PlayService getPlayService() {
        return mPlayService;
    }

    private void showPlayingFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_slide_up, 0);
        if (mPlayFragment == null) {
            mPlayFragment = new PlayFragment();
            ft.replace(android.R.id.content, mPlayFragment);
        } else {
            ft.show(mPlayFragment);
        }
        ft.commit();
        mIsPlayFragmentShow = true;
    }

    private void hidePlayingFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(0, R.anim.fragment_slide_down);
        ft.hide(mPlayFragment);
        ft.commit();
        mIsPlayFragmentShow = false;
    }

    @Override
    public void onBackPressed() {
        if (mPlayFragment != null && mIsPlayFragmentShow) {
            hidePlayingFragment();
            return;
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }
        //moveTaskToBack(false);
        finish();
    }

    @Override
    protected void onDestroy() {
        unbindService(mPlayServiceConnection);
        super.onDestroy();
    }
}
