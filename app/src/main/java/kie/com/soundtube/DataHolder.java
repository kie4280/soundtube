package kie.com.soundtube;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kieChang on 2017/5/18.
 */
public class DataHolder {
    Bitmap thumbnail;
    String title;
    String publishdate;
    String videoID;
    String videolength;
    HashMap<Integer, String> onlineUris = null;
    HashMap<ArrayList<Integer>, String> localUris = null;
}
