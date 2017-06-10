package kie.com.soundtube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import kie.com.soundtube.MediaPlayerService.*;

public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    public MediaPlayerService mediaService;
    private boolean servicebound = false;
    private Intent playIntent;
    private SearchView searchView;
    public Toolbar toolbar;
    public static boolean netConncted = false;
    public CustomViewPager viewPager;

    VideoFragment videoFragment;
    SearchFragment searchFragment;
    Context context;

    ConnectivityManager connectmgr;
    Fragment[] fragments = new Fragment[2];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        UiHandler = new Handler(Looper.getMainLooper());
        videoFragment = new VideoFragment();
        videoFragment.setActivity(this);
        searchFragment = new SearchFragment();
        viewPager = (CustomViewPager) findViewById(R.id.viewpager);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        viewPager.setAdapter(fragmentPagerAdapter);
        fragments[0] = searchFragment;
        fragments[1] = videoFragment;

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ActionBar actionBar = getSupportActionBar();
                if(position == 0) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    if(actionBar != null) {
                        getSupportActionBar().show();
                    }

                } else if(position == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    if(actionBar != null) {
                        getSupportActionBar().hide();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                System.out.println("submit");
                if (query != null) {
                    if (MainActivity.netConncted) {
                        searchFragment.search(query);
                    } else {
                        Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    searchView.clearFocus();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
//                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                if(!hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                }
            }
        });

    }

    private FragmentPagerAdapter fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 2;
        }
    };



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MediaPlayerService.MusicBinder)service;
            mediaService = musicBinder.getService();
            servicebound = true;
            connect();
            if(mediaService.currentData != null) {
                videoFragment.resume();
            } else {

            }
            Log.d("activity", "service connected");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;

            Log.d("activity", "service unbind");

        }
    };

    public void connect() {
        mediaService.videoFragment = videoFragment;
        mediaService.connected = true;
        videoFragment.mediaService = mediaService;
        videoFragment.mediaPlayer = mediaService.mediaPlayer;
        videoFragment.connected = true;
    }

    @Override
    protected void onStart() {

        super.onStart();
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService.class);
            startService(playIntent);
        }

        bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        if(servicebound) {
            unbindService(serviceConnection);
        }

        Log.d("activity", "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("activity", "onPause");
    }

    @Override
    protected void onDestroy() {
        Log.d("activity", "onDestroy");
        stopService(playIntent);
        mediaService = null;
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void onreturnVideo(DataHolder dataHolder) {
        viewPager.setCurrentItem(1, true);
        videoFragment.start(dataHolder);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        moveTaskToBack(true);
    }
}