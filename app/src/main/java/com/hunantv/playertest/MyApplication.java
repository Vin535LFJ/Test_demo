package com.hunantv.playertest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.hunantv.imgo.BaseApplication;
import com.hunantv.media.player.MgtvPlayerLogger;

/**
 * @author 徐斌
 * @since 2017/11/7 下午5:26
 * 这个是代码层的application,为了符合android的加载机制,同时又防止过多的代码与其耦合导致无法使用补丁修复
 */

public class MyApplication extends Application {
    public MyApplication() {
        super();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BaseApplication.setContext(this);
        initLog();
    }

    private void initLog() {
        MgtvPlayerLogger.openDebugMode(true);
        MgtvPlayerLogger.setLogCallback(new MgtvPlayerLogger.OnLogCallback() {

            @Override
            public void onLogCb(int level, String modelId, String tag, String msg) {
                switch (level) {
                    case MgtvPlayerLogger.REPORT_LOG_LEVEL_DEBUG:
                        if (true) {
                            Log.d(tag, msg);
                        }
                        break;
                    case MgtvPlayerLogger.REPORT_LOG_LEVEL_INFO:
                        Log.i(tag, msg);
                        break;
                    case MgtvPlayerLogger.REPORT_LOG_LEVEL_WARN:
                        Log.w(tag, msg);
                        break;
                    case MgtvPlayerLogger.REPORT_LOG_LEVEL_ERROR:
                        Log.e(tag, msg);
                        break;
                }
            }
        });
    }
}
