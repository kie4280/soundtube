package kie.com.soundtube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener,
        VideoFragment.OnFragmentInteractionListener, SettingFragment.OnFragmentInteractionListener
        , PlaylistFragment.OnFragmentInteractionListener {

    public Handler workHandler;
    public HandlerThread workThread;
    public static boolean servicebound = false;
    private Intent serviceIntent;
    public TabLayout tabLayout;
    public CustomSlideUpPanel slidePanel;
    public static boolean netConncted = false;
    VideoFragment videoFragment;
    SearchFragment searchFragment;
    SettingFragment settingFragment;
    PlaylistFragment playlistFragment;
    FragmentManager fragmentManager;
    MediaPlayerService mediaService;
    Context context;
    ConnectivityManager connectmgr;
    int tabHeight;
    ArrayList<Fragment> fragments = new ArrayList<>(3);
    CustomViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            Thread.UncaughtExceptionHandler defualtexception = Thread.getDefaultUncaughtExceptionHandler();

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
//                throwable.printStackTrace();
                Intent intent = new Intent(context, LogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                String stack = Log.getStackTraceString(throwable);
                intent.putExtra("onError message", stack);
                Log.d("Exception", "caught");
                startActivity(intent);
//                continue as normal
                defualtexception.uncaughtException(thread, throwable);
//                System.exit(1);
//                finish();

            }
        });
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabHeight = Tools.convertDpToPixel(50, context);
        slidePanel = (CustomSlideUpPanel) findViewById(R.id.slidePanel);
        viewPager = (CustomViewPager) findViewById(R.id.fragmentPager);
        slidePanel.addPanelSlideListener(panelSlideListener);
//        slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);           //should not be commented when building app
        serviceIntent = new Intent(this, MediaPlayerService.class);
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);


        workThread = new HandlerThread("WorkThread");
        workThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
        videoFragment = new VideoFragment();
        searchFragment = new SearchFragment();
        settingFragment = new SettingFragment();
        playlistFragment = new PlaylistFragment();
        fragments.add(searchFragment);
        fragments.add(playlistFragment);
        fragments.add(settingFragment);

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction
                .add(R.id.videoPanel, videoFragment, "videoFragment")
                .commit();

        tabLayout.addOnTabSelectedListener(tabSelectedListener);
        viewPager.setAdapter(new CustomPagerAdapter(fragmentManager, fragments));

    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
        Log.d("activity", "onStart");

    }

    @Override
    protected void onResume() {

        super.onResume();
        NetworkInfo info = connectmgr.getActiveNetworkInfo();
        if (info != null && info.isAvailable() && info.isConnected()) {
            netConncted = true;
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.needNetwork), Toast.LENGTH_SHORT);
            toast.show();
            netConncted = false;
        }

        Log.d("activity", "onResume");

    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnect();
        Log.d("activity", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("activity", "onDestroy");
        mediaService = null;
        workThread.quitSafely();

    }

    private SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
//            Log.d("panel", Float.toString(slideOffset));
            videoFragment.setHeaderPos(slideOffset);
            tabLayout.setTranslationY(slideOffset * tabHeight);

        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                videoFragment.setHeaderVisible(false);
                videoFragment.setHeaderPos(1);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                Log.d("Panel", "expanded");

            } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                videoFragment.setHeaderVisible(true);
                videoFragment.setHeaderPos(0);
                Log.d("Panel", "collapsed");

            } else if (newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                Log.d("Panel", "dragging");
            }
        }
    };

    private TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {

            viewPager.setCurrentItem(tab.getPosition(), false);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new NavigationView.OnNavigationItemSelectedListener() {


                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int i = item.getItemId();

                    switch (i) {
                        case R.id.playlists:
                            Intent playlistintent = new Intent(context, PlaylistActivity.class);
                            playlistintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(playlistintent);
                            break;
                        case R.id.settings:

                            Intent settingintent = new Intent(context, SettingActivity.class);
                            settingintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(settingintent);

                            break;

                        case R.id.sign_in_action:
                            Intent signInintent = new Intent(context, SignInActivity.class);
                            signInintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(signInintent);
                            break;
                        default:
                            break;
                    }

                    return true;
                }
            };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MusicBinder musicBinder = (MediaPlayerService.MusicBinder) service;
            mediaService = musicBinder.getService();
            servicebound = true;
            mediaService.videoFragment = videoFragment;
            videoFragment.mediaService = mediaService;
            videoFragment.serviceConnected();
            Log.d("activity", "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;
            mediaService.videoFragment = null;
            videoFragment.mediaService = null;
            mediaService = null;
            Log.w("activity", "service unbind due to onError");
        }
    };

    public void connect() {
        startService(serviceIntent);
        if (mediaService == null || !servicebound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    public void disconnect() {
        if (servicebound) {
            unbindService(serviceConnection);
            servicebound = false;
            Log.d("activity", "service disconnected");
        }
    }


    @Override
    public void onReturnSearchVideo(final DataHolder dataHolder) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });
        mediaService.watchedQueue.clear();
        mediaService.currentData = null;
        videoFragment.start(dataHolder);
    }

    @Override
    public void onStartVideo(DataHolder dataHolder) {

    }

    @Override
    public void onBackPressed() {
        Log.d("SearchActivity", "activity back");
        switch (slidePanel.getPanelState()) {
            case HIDDEN:
            case COLLAPSED:
                stopService(serviceIntent);
                finish();
                break;
            case EXPANDED:
                videoFragment.previousVideo();
                break;
            default:
                break;
        }

//        moveTaskToBack(true);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                videoFragment.changeToLandscape();
                slidePanel.setTouchEnabled(false);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                videoFragment.changeToPortrait();
                slidePanel.setTouchEnabled(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
