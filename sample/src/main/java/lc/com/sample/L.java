package lc.com.sample;

import android.util.Log;

/**
 * Created by LiangCheng on 2017/11/22.
 */

public class L {

    private static final String TAG = "OKHTTP";
    private static final boolean debug = true;//开关

    public static void e(String msg) {
        if (debug) {
            Log.e(TAG, msg);
        }
    }
}
