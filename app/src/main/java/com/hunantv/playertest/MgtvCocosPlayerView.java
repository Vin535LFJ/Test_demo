package com.hunantv.playertest;

import android.app.Activity;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.cocos.lib.IVideoPlayerView;
import com.hunantv.imgo.BaseApplication;
import com.hunantv.media.alpha.MgtvAlphaVideoView;
import com.hunantv.media.player.utils.FileUtil;
import com.hunantv.media.report.ReportParams;
import com.hunantv.media.widget.IVideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Author : kangxuebin
 * Time : 2024/6/16 16:21
 * Describe: cocos专用的透明播放器
 */
public class MgtvCocosPlayerView implements IVideoPlayerView {
//    private String url = "https://video.da.mgtv.com/new_video/2022/06/25/1038/AA95C79FB79D07E9FB9CC7F2D3210645_20220625_1_1_2418.mp4";
    private MgtvAlphaVideoView mMgtvVideoView;
    private Activity mActivity;
    private String TAG = MgtvCocosPlayerView.class.getSimpleName();

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    protected int mViewLeft = 0;
    protected int mViewTop = 0;
    protected int mViewWidth = 0;
    protected int mViewHeight = 0;

    protected int mVisibleLeft = 0;
    protected int mVisibleTop = 0;
    protected int mVisibleWidth = 0;
    protected int mVisibleHeight = 0;

    protected boolean mFullScreenEnabled = false;

    private boolean mKeepRatio = false;
    private boolean mMetaUpdated = false;
    private boolean mLoop = false;
    private String mLoopUrl;

    private OnVideoEventListener mOnVideoEventListener;
    private int mViewTag = 0;

    public MgtvCocosPlayerView(Activity activity, int tag, boolean isSurfaceView) {
        this.mActivity = activity;
        this.mViewTag = tag;
        this.mLoop = false;
        if (isSurfaceView){
            mMgtvVideoView = new MgtvAlphaVideoView(activity, MgtvAlphaVideoView.MGTV_RENDER_SURFACE_VIEW);
            mMgtvVideoView.setPlayerHardwareMode(false);
            //默认调一次，否则MgtvAlphaVideoView内部会调一次setZOrderOnTop
            mMgtvVideoView.setZOrderMediaOverlay(true);
        }else {
            mMgtvVideoView = new MgtvAlphaVideoView(activity, MgtvAlphaVideoView.MGTV_RENDER_TEXTURE_VIEW);
        }
        mMgtvVideoView.setOnPreparedListener(() -> {
//            Log.d(TAG, "onPrepared mLoop " + mLoop);
            if (mLoop){
                mMgtvVideoView.start();
            }else{
                sendEvent(EVENT_META_LOADED);
                sendEvent(EVENT_READY_TO_PLAY);
            }
        });
        // 设置场景类型
        ReportParams reportParams = new ReportParams();
        reportParams.setVideoType(ReportParams.VideoType.ALPHA_PLAY);
        mMgtvVideoView.setReportParams(reportParams);
        mMgtvVideoView.setOnCompletionListener(new IVideoView.OnCompletionListener() {
            @Override
            public void onCompletion(int i, int i1) {
                Log.d(TAG, "onCompletion mLoop " + mLoop);
                if (mLoop) {
                    //重播不需要seek
                   // mMgtvVideoView.seekTo(0);
                  //  mMgtvVideoView.start();
                    reStart();
                } else {
                    sendEvent(EVENT_COMPLETED);
                }
            }
        });

        mMgtvVideoView.setOnErrorListener(new IVideoView.OnErrorListener() {
            @Override
            public boolean onError(int what, int extra) {
                Log.d(TAG, "cocos player onError what " + what + " , extra : " + extra);
                return false;
            }
        });
    }

    /**
     * 重播
     * */
    private void reStart() {
        Log.d(TAG, "reStart " + mLoop);
        if(!TextUtils.isEmpty(mLoopUrl) && mLoop){
            String filePath = mLoopUrl;
            if (filePath.startsWith(AssetResourceRoot)) {
                filePath = filePath.substring(AssetResourceRoot.length());
            }
            if (filePath.startsWith("/")) {
                mIsAssetResource = false;
                mMgtvVideoView.resetVideoPath(filePath);
            } else {
                mMgtvVideoView.resetVideoPath(urlToPath("/" + filePath));
            }
        }
    }

    @Override
    public View getView() {
        return mMgtvVideoView;
    }

    @Override
    public void setVideoURL(String url) {
        Log.d(TAG, "setVideoURL " + url);
        mIsAssetResource = false;
      //  mMgtvVideoView.setLoop(mLoop);
        mMgtvVideoView.setVideoPath(urlToPath(url));
    }

    private boolean mIsAssetResource = false;
    private String mVideoFilePath = null;

    @Override
    public void setVideoFileName(String filePath) {
        Log.d(TAG, "setVideoFileName " + filePath);
        this.mLoopUrl = filePath;
        if (filePath.startsWith(AssetResourceRoot)) {
            filePath = filePath.substring(AssetResourceRoot.length());
        }
      //  mMgtvVideoView.setLoop(mLoop);
        if (filePath.startsWith("/")) {
            mIsAssetResource = false;
            mMgtvVideoView.setVideoPath(filePath);
        } else {
            mMgtvVideoView.setVideoPath(urlToPath("/" + filePath));
        }
    }

    private String urlToPath(String filePath){
        filePath = filePath.substring(1);
        final String path = mActivity.getExternalCacheDir().getAbsolutePath() +  filePath;
        try {
            InputStream in = null;
            OutputStream out = null;
            try {
                // 打开assets目录下的文件输入流
                in = BaseApplication.getContext().getAssets().open(filePath);
                // 创建输出文件，位于应用内部存储目录下
                String outDirPath = path;
                File outFile = new File(outDirPath);
                // 确保输出目录存在
                outFile.getParentFile().mkdirs();
                // 创建文件输出流
                out = new FileOutputStream(outFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public void start() {
        Log.d(TAG, "start" );
        mMgtvVideoView.start();
        sendEvent(EVENT_PLAYING);
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop" );
        mMgtvVideoView.stop();
        sendEvent(EVENT_STOPPED);
    }

    @Override
    public void stopPlayback() {
        Log.d(TAG, "stopPlayback" );
        mMgtvVideoView.release();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause ");
        mMgtvVideoView.pause();
        sendEvent(EVENT_PAUSED);
    }

    @Override
    public void resume() {
        Log.d(TAG, "resume " );
        mMgtvVideoView.start();
        sendEvent(EVENT_PLAYING);
    }

    @Override
    public void seekTo(int ms) {
        Log.d(TAG, "seekTo : "+ms );
        mMgtvVideoView.seekTo(ms);
    }

    @Override
    public int getDuration() {
        return mMgtvVideoView.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mMgtvVideoView.getCurrentPositionUnsafe();
    }

    @Override
    public void setVolume(float volume) {
        if (mMgtvVideoView != null) {
            mMgtvVideoView.setVolume(volume, volume);
        }
    }

    @Override
    public void setLoop(boolean enable) {
        mLoop = enable;
        Log.d(TAG, "setLoop " + enable);
    }

    @Override
    public void setKeepRatio(boolean enabled) {
        mKeepRatio = enabled;
        fixSize();
    }

    @Override
    public void setVideoRect(int left, int top, int maxWidth, int maxHeight) {
        Log.d(TAG, "setVideoRect left : " + left +" top ; " + top + " ; maxWidth : " + maxWidth + " ; maxHeight : " + maxHeight);
        if (mViewLeft == left && mViewTop == top && mViewWidth == maxWidth && mViewHeight == maxHeight){
            return;
        }

        mViewLeft = left;
        mViewTop = top;
        mViewWidth = maxWidth;
        mViewHeight = maxHeight;
        fixSize(mViewLeft, mViewTop, mViewWidth, mViewHeight);
    }

    public void fixSize(int left, int top, int width, int height) {
        if (mVideoWidth == 0 || mVideoHeight == 0) {
            mVisibleLeft = left;
            mVisibleTop = top;
            mVisibleWidth = width;
            mVisibleHeight = height;
        } else if (width != 0 && height != 0) {
            if (mKeepRatio && !mFullScreenEnabled) {
                if ( mVideoWidth * height  > width * mVideoHeight ) {
                    mVisibleWidth = width;
                    mVisibleHeight = width * mVideoHeight / mVideoWidth;
                } else if ( mVideoWidth * height  < width * mVideoHeight ) {
                    mVisibleWidth = height * mVideoWidth / mVideoHeight;
                    mVisibleHeight = height;
                }
                mVisibleLeft = left + (width - mVisibleWidth) / 2;
                mVisibleTop = top + (height - mVisibleHeight) / 2;
            } else {
                mVisibleLeft = left;
                mVisibleTop = top;
                mVisibleWidth = width;
                mVisibleHeight = height;
            }
        } else {
            mVisibleLeft = left;
            mVisibleTop = top;
            mVisibleWidth = mVideoWidth;
            mVisibleHeight = mVideoHeight;
        }

//        getHolder().setFixedSize(mVisibleWidth, mVisibleHeight);

        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(mVisibleWidth, mVisibleHeight);
        lParams.leftMargin = mVisibleLeft;
        lParams.topMargin = mVisibleTop;
        if(this.mMgtvVideoView != null) {
            this.mMgtvVideoView.setLayoutParams(lParams);
        }
    }

    @Override
    public void setFullScreenEnabled(boolean enabled) {
        if (mFullScreenEnabled != enabled) {
            mFullScreenEnabled = enabled;
            fixSize();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        mMgtvVideoView.setVisibility(visibility);
    }

    @Override
    public void fixSize() {
        if (mFullScreenEnabled) {
            Point screenSize = new Point();
            mActivity.getWindowManager().getDefaultDisplay().getSize(screenSize);
            fixSize(0, 0, screenSize.x, screenSize.y);
        } else {
            fixSize(mViewLeft, mViewTop, mViewWidth, mViewHeight);
        }
    }

    @Override
    public void setVideoViewEventListener(OnVideoEventListener l) {
        mOnVideoEventListener = l;
    }

    @Override
    public void setViewTag(int viewTag) {
        this.mViewTag = viewTag;
    }

    public void setStayOnBottom(boolean enable) {
        Log.d(TAG, "setStayOnBottom " + enable);
        mMgtvVideoView.setZOrderMediaOverlay(!enable);
    }

    private void sendEvent(int event) {
        if (this.mOnVideoEventListener != null) {
            this.mOnVideoEventListener.onVideoEvent(this.mViewTag, event);
        }
    }


//    @WithTryCatchRuntime
    public void setVideoVisibility(boolean visibility){
        if (visibility){
            mMgtvVideoView.setVisibility(View.VISIBLE);
        }else{
            mMgtvVideoView.setVisibility(View.GONE);
        }
    }

}
