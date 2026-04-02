package sk.ukf.wavvy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import sk.ukf.wavvy.model.Song;

public class MainActivity extends AppCompatActivity implements PlaybackManager.Listener {
    private BottomNavigationView bottomNav;
    private ConstraintLayout miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageButton btnMiniPrev, btnMiniPlay, btnMiniNext;
    private ProgressBar miniProgress;
    private View navIndicator;
    private Fragment homeFragment;
    private Fragment searchFragment;
    private Fragment playlistsFragment;
    private Fragment activeFragment;
    private float startX;
    private float startY;
    private static final int SWIPE_THRESHOLD = 140;
    private PlaybackManager pm;
    private int lastAnimatedAudioId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        applyEdgeToEdge();

        bottomNav = findViewById(R.id.bottomNav);
        navIndicator = findViewById(R.id.navIndicator);
        pm = PlaybackManager.get(this);

        if (savedInstanceState == null) {

            homeFragment = new HomeFragment();
            searchFragment = new SearchFragment();
            playlistsFragment = new LibraryFragment();

            activeFragment = homeFragment;

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.navHost, playlistsFragment, "playlists").hide(playlistsFragment)
                    .add(R.id.navHost, searchFragment, "search").hide(searchFragment)
                    .add(R.id.navHost, homeFragment, "home")
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            preloadFragments();
        }
        setupBottomNavigation();
        setupMiniPlayer();

        bottomNav.post(() -> {
            moveIndicator(0);
            animateLabels(0);
        });
    }
    private void applyEdgeToEdge() {

        View bottomContainer = findViewById(R.id.bottomNavContainer);

        ViewCompat.setOnApplyWindowInsetsListener(
                bottomContainer,
                (v, insets) -> {

                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.navigationBars()
                    );
                    ViewGroup.MarginLayoutParams lp =
                            (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    v.setTranslationY(-dp(6));
                    return insets;
                }
        );
    }
    private int dp(int value){
        return (int)(value * getResources().getDisplayMetrics().density);
    }
    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target = null;
            int index = 0;

            if (item.getItemId() == R.id.nav_home) {
                target = homeFragment;
                index = 0;
            }
            else if (item.getItemId() == R.id.nav_search) {
                target = searchFragment;
                index = 1;
            }
            else if (item.getItemId() == R.id.nav_playlists) {
                target = playlistsFragment;
                index = 2;
            }

            if (target != null) {
                switchFragment(target);
                moveIndicator(index);
                animateLabels(index);
                return true;
            }

            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        ft.hide(activeFragment);
        ft.show(target);
        ft.commit();
        activeFragment = target;
    }
    private void preloadFragments() {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            getSupportFragmentManager()
                    .beginTransaction()
                    .show(searchFragment)
                    .commitNow();

            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(searchFragment)
                    .commitNow();

            getSupportFragmentManager()
                    .beginTransaction()
                    .show(playlistsFragment)
                    .commitNow();

            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(playlistsFragment)
                    .commitNow();

        }, 120);
    }
    private void moveIndicator(int position) {

        bottomNav.post(() -> {

            ViewGroup menuView = (ViewGroup) bottomNav.getChildAt(0);
            if (menuView == null) return;

            View itemView = menuView.getChildAt(position);
            if (itemView == null) return;

            float targetX =
                    itemView.getLeft()
                            + itemView.getWidth()/2f
                            - navIndicator.getWidth()/2f;

            navIndicator.animate()
                    .translationX(targetX)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        });
    }
    private void animateLabels(int selectedIndex) {

        ViewGroup menuView = (ViewGroup) bottomNav.getChildAt(0);
        if (menuView == null) return;

        for (int i = 0; i < menuView.getChildCount(); i++) {

            View item = menuView.getChildAt(i);

            TextView label =
                    item.findViewById(
                            com.google.android.material.R.id.navigation_bar_item_small_label_view
                    );

            if (label == null) continue;

            if (i == selectedIndex) {
                label.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(220).start();
            } else {
                label.animate().alpha(0.7f).scaleX(1f).scaleY(1f).setDuration(220).start();
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setupMiniPlayer() {
        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPrev = findViewById(R.id.btnMiniPrev);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        miniProgress = findViewById(R.id.miniProgress);


        miniPlayer.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    float endX = event.getRawX();
                    float endY = event.getRawY();
                    float deltaX = endX - startX;
                    float deltaY = endY - startY;

                    if (Math.abs(deltaX) > 120 && Math.abs(deltaX) > Math.abs(deltaY)) {
                        if (deltaX > 0) {
                            lastAnimatedAudioId = -1;
                            PlaybackManager.get(this).playPrev(true);
                        } else {
                            lastAnimatedAudioId = -1;
                            PlaybackManager.get(this).playNext(true);
                        }

                        float move = deltaX > 0 ? 40f : -40f;
                        View[] views = {tvMiniTitle, tvMiniArtist};

                        for (View view : views) {
                            view.animate().cancel();
                            view.animate()
                                    .translationX(move)
                                    .setDuration(120)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .withEndAction(() ->
                                            view.animate()
                                                    .translationX(0f)
                                                    .setDuration(140)
                                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                                    .start()
                                    )
                                    .start();
                        }
                        return true;
                    }
                    if (Math.abs(deltaY) < 20 || -deltaY > SWIPE_THRESHOLD) {
                        openPlayerFromNowPlaying();
                    }
                    v.performClick();
                    return true;
            }
            return false;
        });
        btnMiniPrev.setOnClickListener(v -> {

            haptic(v);

            PlaybackManager pm = PlaybackManager.get(this);
            long pos = pm.getPlayer().getCurrentPosition();

            if (pos > 3000) {
                pm.getPlayer().seekTo(0);
            } else {
                pm.playPrev(true);
            }
        });

        btnMiniPlay.setOnClickListener(v -> {
            haptic(v);
            v.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(80)
                    .withEndAction(() ->
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(120)
                                    .start()
                    )
                    .start();
            PlaybackManager.get(this).togglePlayPause();
        });
        btnMiniNext.setOnClickListener(v -> {
            haptic(v);
            PlaybackManager.get(this).playNext(true);
        });
    }

    @Override protected void onStart(){
        super.onStart();
        PlaybackManager.get(this).addListener(this);
    }

    @Override protected void onStop(){
        super.onStop();
        PlaybackManager.get(this).removeListener(this);
    }
    @Override public void onIsPlayingChanged(boolean playing){
        btnMiniPlay.setImageResource(
                playing?R.drawable.ic_pause:R.drawable.ic_play);
    }
    @Override public void onNowPlayingChanged(int a,int[]b,int c){
        updateMiniPlayer();
    }
    @Override
    public void onProgress(long pos, long dur) {
        if (dur <= 0) return;
        miniProgress.setMax((int) dur);

        miniProgress.animate()
                .setDuration(200)
                .withStartAction(() ->
                        miniProgress.setProgress((int) pos)
                )
                .start();
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
        int currentId = pm.getCurrentAudioResId();

        if (currentId == lastAnimatedAudioId) {
            return;
        }

        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());

        ivMiniCover.animate().cancel();
        ivMiniCover.setAlpha(1f);
        ivMiniCover.setTranslationX(0f);
        ivMiniCover.setImageResource(s.getCoverResId());
        ivMiniCover.setAlpha(0.85f);
        ivMiniCover.animate()
                .alpha(1f)
                .setDuration(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        lastAnimatedAudioId = currentId;
    }
    private void haptic(View v){
        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
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