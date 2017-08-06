package kie.com.soundtube;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.HashMap;
import java.util.List;


public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    private float scaleFactor = 1f;
    private int HeaderDP = 70;
    private float headersize = 0;
    public boolean prepared = false;
    boolean connected = false;
    boolean controlshow = true;
    boolean seekbarUpdating = false;
    private Button playbutton;
    private SurfaceView surfaceView;
    private RelativeLayout vrelativeLayout;
    private RelativeLayout drelativeLayout;
    private RelativeLayout header;
    private View videoFragmentView;
    private SeekBar seekBar;
    private ProgressBar progressBar;
    private TextView currentTime;
    private TextView totalTime;
    private ListView listView;
    private DisplayMetrics displayMetrics;
    private Context context;
    private RelativeLayout.LayoutParams portraitlayout;
    private RelativeLayout.LayoutParams landscapelayout;
    private Activity activity;
    private Handler seekHandler, workHandler;
    private HandlerThread thread;
    private RecyclerView recyclerView;
    private ViewPager viewPager;
    public VideoRetriver videoRetriver;
    public SurfaceHolder surfaceHolder;

    MediaPlayerService mediaService;
    MainActivity mainActivity;
    VideoRecyclerAdapter adapter;
    Searcher searcher = null;
    ScaleGestureDetector scaleGestureDetector;

    public VideoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
        displayMetrics = context.getResources().getDisplayMetrics();
        thread = new HandlerThread("seek");
        thread.start();
        seekHandler = new Handler(thread.getLooper());
        videoRetriver = new VideoRetriver(thread);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        videoFragmentView = inflater.inflate(R.layout.fragment_video, container, false);
        surfaceView = (SurfaceView) videoFragmentView.findViewById(R.id.surfaceView);
        seekBar = (SeekBar) videoFragmentView.findViewById(R.id.seekBar);

        currentTime = (TextView) videoFragmentView.findViewById(R.id.currentTime);
        totalTime = (TextView) videoFragmentView.findViewById(R.id.totalTime);
        header = (RelativeLayout) videoFragmentView.findViewById(R.id.headerView);
//        recyclerView = (RecyclerView) videoFragmentView.findViewById(R.id.videorecyclerView);
        recyclerView = new RecyclerView(context);
        viewPager = (ViewPager) videoFragmentView.findViewById(R.id.searchViewPager);
        headersize = Tools.convertDpToPixel(HeaderDP, context);

        listView = (ListView) videoFragmentView.findViewById(R.id.recoList);
        playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
        progressBar = (ProgressBar) videoFragmentView.findViewById(R.id.progressBar1);
        progressBar.setMax(100);
        progressBar.setIndeterminate(false);
        mainActivity.slidePanel.setScrollableView(recyclerView);
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter();
        pagerAdapter.addView(recyclerView);
        pagerAdapter.count = 1;
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1);

        vrelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.videoRelativeLayout);
        drelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.descriptionRelativeLayout);
        RelativeLayout.LayoutParams orig = (RelativeLayout.LayoutParams) vrelativeLayout.getLayoutParams();
        orig.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        landscapelayout = new RelativeLayout.LayoutParams(orig);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int w = (int) (displayMetrics.heightPixels / Displayratio);
            orig.height = w;
            portraitlayout = new RelativeLayout.LayoutParams(orig);
            changeToLandscape();

        } else {
            int w = (int) (displayMetrics.widthPixels / Displayratio);
            orig.height = w;
            portraitlayout = new RelativeLayout.LayoutParams(orig);
            changeToPortrait();
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                prepared = true;
                if (mediaService != null) {
                    mediaService.setDisplay(holder);
                }

                Log.d("video", "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                System.out.println("width " + width + " height " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                prepared = false;
                Log.d("video", "surfaceDestroyed");
                if (mediaService != null) {
                    mediaService.setDisplay(null);
                }

            }
        });

        playbutton.setOnTouchListener(new View.OnTouchListener() {
            float prevX = 0;
            float prevY = 0;
            float thresholdX = 15f;
            float thresholdY = 15f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                if (action == MotionEvent.ACTION_DOWN) {
//                    mainActivity.viewPager.setSwipingEnabled(false);
                    Log.d("playbutton", "down");
                    prevX = event.getX();
                    prevY = event.getY();
                } else if (action == MotionEvent.ACTION_UP) {
                    if (Math.abs(event.getX() - prevX) <= thresholdX &&
                            Math.abs(event.getY() - prevY) <= thresholdY && mediaService != null) {
                        if (mediaService.mediaPlayer.isPlaying()) {
                            mediaService.pause();
                            playbutton.setBackgroundResource(R.drawable.play);

                        } else {
                            mediaService.play();
                            playbutton.setBackgroundResource(R.drawable.pause);
                        }
//                    mainActivity.viewPager.setSwipingEnabled(true);
                    }

                }
                return false;
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (mediaService.prepared && fromUser) {
                    mediaService.seekTo(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        scaleGestureDetector = new ScaleGestureDetector(vrelativeLayout.getContext(),
                new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.d("scale", Float.toString(scaleFactor));
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
        vrelativeLayout.setOnTouchListener(touchListener);


        Log.d("video", "createView");

        return videoFragmentView;
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        float prevX = 0;
        float prevY = 0;
        float thresholdX = 15f;
        float thresholdY = 15f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);
            int pointerIndex = event.getActionIndex();
            int pointerID = event.getPointerId(pointerIndex);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    prevX = event.getX();
                    prevY = event.getY();
                case MotionEvent.ACTION_POINTER_DOWN:
                    Log.d("surface", "touchdown");
                case MotionEvent.ACTION_MOVE:
//                    mainActivity.viewPager.setSwipingEnabled(false);

//                    Log.d("surface", "move");
                    scaleGestureDetector.onTouchEvent(event);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getX() - prevX) <= thresholdX &&
                            Math.abs(event.getY() - prevY) <= thresholdY) {
                        if (controlshow) {
                            showcontrols(false);
                        } else {
                            showcontrols(true);
                        }
                    }

//                    mainActivity.viewPager.setSwipingEnabled(true);
                    Log.d("surface", "touchup");
                    break;
            }


            return true;
        }
    };


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        System.out.println("fragment back");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
//        if (mediaService.updateSeekBar) {
//            updateSeekBar();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
        connected = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quit();
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    public void start(final DataHolder dataHolder) {

        ConnectivityManager connectmgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectmgr.getActiveNetworkInfo();
        if (info.isAvailable() && info.isConnected()) {

            if (mediaService != null) {
                mediaService.reset();
                mediaService.prepare(dataHolder);
                mediaService.setDisplay(surfaceHolder);
                mediaService.play();
                playbutton.setBackgroundResource(R.drawable.pause);

                if (searcher != null) {
                    searcher.loadRelatedVideos(dataHolder.videoID, new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(final List<DataHolder> data) {
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateListView(dataHolder.title, data);
                                }
                            });
                        }
                    });
                }


            } else {
                Toast toast = Toast.makeText(context, "Error!!!", Toast.LENGTH_LONG);
                toast.show();
            }


        } else {
            Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public void resume() {


    }

    public void updateListView(final String text, final List<DataHolder> data) {
        if (adapter == null) {
            createListView(data);
        }
//        recyclerView.setTranslationY(mainActivity.toolbar.getHeight());
        adapter.dataHolders = data;
        adapter.text = text;
        adapter.notifyDataSetChanged();

    }

    public void createListView(final List<DataHolder> data) {

        //        Log.d("createlist", "create");
        adapter = new VideoRecyclerAdapter(data);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
            @Override
            public void onClick(View view, int position) {
                if (position >= 1) {
                    System.out.println("clicked" + (position - 1));
                    Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                    toast.show();
                    final DataHolder dataHolder = adapter.dataHolders.get(position - 1);
                    videoRetriver.startExtracting("https://www.youtube" +
                            ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;
                            start(dataHolder);
                            //Log.d("search", ))
                        }

                        @Override
                        public void onFailure(Error error) {
                            Log.d("search", "error extracting");

                        }
                    });
                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        });
        recyclerView.addOnItemTouchListener(listener);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {


            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                Log.d("recyclerView dx", Integer.toString(dx));
                mainActivity.setToolbar(dy);

            }
        });
//        ListAdapter listAdapter = new ListAdapter() {
//
//
//            @Override
//            public boolean areAllItemsEnabled() {
//                return true;
//            }
//
//            @Override
//            public boolean isEnabled(int position) {
//                return true;
//            }
//
//            @Override
//            public void registerDataSetObserver(DataSetObserver observer) {
//
//            }
//
//            @Override
//            public void unregisterDataSetObserver(DataSetObserver observer) {
//
//            }
//
//            @Override
//            public int getCount() {
//                return data.size() + 1;
//            }
//
//            @Override
//            public DataHolder getItem(int position) {
//                return null;
//            }
//
//            @Override
//            public long getItemId(int position) {
//                return 0;
//            }
//
//            @Override
//            public boolean hasStableIds() {
//                return false;
//            }
//
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//
//                int viewtype = getItemViewType(position);
//                if (convertView == null) {
//
//                    if (viewtype == 0) {
//                        convertView = titleView;
//                    } else {
//
//                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
//                                Context.LAYOUT_INFLATER_SERVICE);
//                        View inflatedView = inflater.inflate(R.layout.video_layout, parent, false);
//                        ViewHolder viewHolder = new ViewHolder();
//                        viewHolder.imageView = (ImageView) inflatedView.findViewById(R.id.imageView);
//                        viewHolder.titleview = (TextView) inflatedView.findViewById(R.id.titleview);
//                        viewHolder.durationview = (TextView) inflatedView.findViewById(R.id.durationview);
//                        DataHolder dataHolder = data.get(position - 1);
//                        viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
//                        viewHolder.titleview.setText(dataHolder.title);
//                        viewHolder.durationview.setText(dataHolder.videolength);
//                        convertView = inflatedView;
//                        convertView.setTag(viewHolder);
//                    }
//                } else {
//
//                    if (viewtype == 0) {
//                        convertView = titleView;
//                    } else {
//                        DataHolder dataHolder = data.get(position - 1);
//                        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
//                        viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
//                        viewHolder.titleview.setText(dataHolder.title);
//                        viewHolder.durationview.setText(dataHolder.videolength);
//                    }
//                }
//                return convertView;
//            }
//
//            @Override
//            public int getItemViewType(int position) {
//                if (position == 0) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//
//            }
//
//            @Override
//            public int getViewTypeCount() {
//                return 2;
//            }
//
//            @Override
//            public boolean isEmpty() {
//                return false;
//            }
//        };
//        listView.setAdapter(listAdapter);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                System.out.println("clicked" + position);
//                Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
//                toast.show();
//                if (position != 0) {
//                    final DataHolder dataHolder = data.get(position - 1);
//                    videoRetriver.startExtracting("https://www.youtube" +
//                            ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
//                        @Override
//                        public void onSuccess(HashMap<Integer, String> result) {
//                            dataHolder.videoUris = result;
//                            start(dataHolder);
//                            //Log.d("search", ))
//                        }
//
//                        @Override
//                        public void onFailure(Error error) {
//                            Log.d("search", "errorextracting");
//
//                        }
//                    });
//                }
//
//
//            }
//        });

    }

    public void buffering(final boolean buff) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (buff) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }

            }
        });

    }

    public void setSeekBarMax(final int max) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seekBar.setMax(max);
//                totalTime.setText(max);
            }
        });
    }

    public void updateSeekBar() {
        if (activity != null && !seekbarUpdating) {
            seekbarUpdating = true;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mediaService.updateSeekBar) {
                        int pos = mediaService.mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(pos);
//                        currentTime.setText(pos);
                        seekHandler.postDelayed(this, 1000);
                    } else {
                        seekbarUpdating = false;
                    }
                }
            });
        }
    }

    public void onComplete() {
        seekHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playbutton.setBackgroundResource(R.drawable.play);
                        showcontrols(true);
                        seekBar.setProgress(0);

                    }
                });
            }
        }, 200);

    }

    public void setSearchWorker(Handler handler) {
        this.workHandler = handler;
        searcher = new Searcher(context, workHandler);
    }

    public void changeToPortrait() {
        if (vrelativeLayout != null && drelativeLayout != null && activity != null) {
            vrelativeLayout.setLayoutParams(portraitlayout);
            drelativeLayout.setVisibility(View.VISIBLE);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    public void changeToLandscape() {
        if (vrelativeLayout != null && drelativeLayout != null && activity != null) {
            vrelativeLayout.setLayoutParams(landscapelayout);
            drelativeLayout.setVisibility(View.GONE);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

    }

    public void showcontrols(final boolean show) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    seekBar.setVisibility(View.VISIBLE);
                    playbutton.setVisibility(View.VISIBLE);
                    controlshow = true;
                } else {
                    seekBar.setVisibility(View.GONE);
                    playbutton.setVisibility(View.GONE);
                    controlshow = false;
                }
            }
        });
    }

    public void setHeaderPos(float alpha) {
        header.setAlpha(1 - alpha);
        float offset = headersize * (1 - alpha);
//        header.setTranslationY(offset);
        vrelativeLayout.setTranslationY(offset);
        drelativeLayout.setTranslationY(offset);
    }

    public void setHeaderVisible(boolean visible) {
        if (header != null) {
            if (visible) {
                header.setVisibility(View.VISIBLE);
            } else {
                header.setVisibility(View.GONE);
            }

        }
    }


    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

}


