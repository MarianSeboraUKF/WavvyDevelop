package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import sk.ukf.wavvy.model.Song;

public class MainActivity extends AppCompatActivity implements PlaybackManager.Listener {
    private BottomNavigationView bottomNav;
    private ConstraintLayout miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageButton btnMiniPrev, btnMiniPlay, btnMiniNext;
    private ProgressBar miniProgress;
    private float startY;
    private static final int SWIPE_THRESHOLD = 140;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        View root = findViewById(R.id.navHost);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottom = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            ).bottom;

            v.setPadding(0,0,0,bottom);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment fragment = null;

            if (item.getItemId()==R.id.nav_home)
                fragment=new HomeFragment();
            else if(item.getItemId()==R.id.nav_search)
                fragment=new SearchFragment();
            else if(item.getItemId()==R.id.nav_playlists)
                fragment=new PlaylistsFragment();

            if(fragment!=null){
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPrev = findViewById(R.id.btnMiniPrev);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        miniProgress = findViewById(R.id.miniProgress);

        miniPlayer.setOnTouchListener((v,event)->{

            if(event.getAction()==MotionEvent.ACTION_DOWN){
                startY=event.getRawY();
                return true;
            }

            if(event.getAction()==MotionEvent.ACTION_UP){
                v.performClick();
                float diff=startY-event.getRawY();

                if(Math.abs(diff)<20||diff>SWIPE_THRESHOLD){
                    openPlayerFromNowPlaying();
                }
                return true;
            }
            return false;
        });

        btnMiniPlay.setOnClickListener(v ->
                PlaybackManager.get(this).togglePlayPause());

        btnMiniPrev.setOnClickListener(v -> {
            PlaybackManager pm = PlaybackManager.get(this);

            if (pm.getPlayer() != null &&
                    pm.getPlayer().getCurrentPosition() > 3000) {
                pm.getPlayer().seekTo(0);
                return;
            }

            pm.playPrev(true);
        });

        btnMiniNext.setOnClickListener(v ->
                PlaybackManager.get(this).playNext(true));
    }
    private void loadFragment(Fragment f){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.navHost,f)
                .commit();
    }

    @Override protected void onStart(){
        super.onStart();
        PlaybackManager.get(this).addListener(this);
    }

    @Override protected void onStop(){
        super.onStop();
        PlaybackManager.get(this).removeListener(this);
    }

    @Override protected void onResume(){
        super.onResume();
        updateMiniPlayer();
    }
    @Override public void onNowPlayingChanged(int a,int[]b,int c){
        updateMiniPlayer();
    }
    @Override public void onIsPlayingChanged(boolean playing){
        btnMiniPlay.setImageResource(
                playing?R.drawable.ic_pause:R.drawable.ic_play);
    }
    @Override public void onProgress(long pos,long dur){
        if(dur>0){
            miniProgress.setMax((int)dur);
            miniProgress.setProgress((int)pos);
        }
    }
    private void updateMiniPlayer(){
        if(!NowPlayingRepository.hasNowPlaying(this)){
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        Song s = SongRepository.findByAudioResId(
                NowPlayingRepository.getAudioResId(this));

        if(s==null)return;

        miniPlayer.setVisibility(View.VISIBLE);
        ivMiniCover.setImageResource(s.getCoverResId());
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());

        PlaybackManager pm = PlaybackManager.get(this);

        long savedPos = pm.getSavedPosition();
        long savedDur = pm.getSavedDuration();

        if (savedDur > 0) {
            miniProgress.setMax((int) savedDur);
            miniProgress.setProgress((int) savedPos);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        PlaybackManager.get(this).saveCurrentPositionNow();
    }
    private void openPlayerFromNowPlaying(){

        PlaybackManager pm=PlaybackManager.get(this);
        int[] q=pm.getQueueIds();

        if(q==null||q.length==0)return;

        Intent i=new Intent(this,PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_QUEUE_AUDIO_IDS,q);
        i.putExtra(PlayerActivity.EXTRA_QUEUE_INDEX,pm.getQueueIndex());
        i.putExtra(PlayerActivity.EXTRA_OPEN_EXISTING,true);
        startActivity(i);
    }
}