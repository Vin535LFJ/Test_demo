package com.cocos.lib;

import android.view.View;

/**
 * @author dengwenbo
 * @brief 视频播放view相关接口
 * */
public interface IVideoPlayerView {
    static final String AssetResourceRoot = "@assets/";
    static final int EVENT_PLAYING = 0;
    static final int EVENT_PAUSED = 1;
    static final int EVENT_STOPPED = 2;
    static final int EVENT_COMPLETED = 3;
    static final int EVENT_META_LOADED = 4;
    static final int EVENT_CLICKED = 5;
    static final int EVENT_READY_TO_PLAY = 6;

    View getView();
    void setVideoURL(String url);
    void setVideoFileName(String filePath);
    void start();
    void stop();
    void stopPlayback();
    void pause();
    void resume();
    void seekTo(int ms);
    int getDuration();
    int getCurrentPosition();
    void setVolume(float volume);
    void setLoop(boolean enable);

    void setKeepRatio(boolean enabled);
    void setVideoRect(int left, int top, int maxWidth, int maxHeight);
    void setFullScreenEnabled(boolean enabled);
    void setVisibility(int visibility);
    void fixSize();

    void setVideoViewEventListener(OnVideoEventListener l);
    void setViewTag(int viewTag);
    void setStayOnBottom(boolean enable);

    public interface OnVideoEventListener {
        void onVideoEvent(int tag,int event);
    }
}
