/****************************************************************************
Copyright (c) 2014-2016 Chukong Technologies Inc.
Copyright (c) 2017-2020 Xiamen Yaji Software Co., Ltd.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/

package com.cocos.lib;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.app.Activity;

import java.lang.ref.WeakReference;

public class CocosVideoHelper {
    private enum AlphaLayoutPosition {
        Bottom,
        Right,
        Top,
        Left
    }

    private FrameLayout mLayout = null;
    private Activity mActivity = null;
    private static SparseArray<IVideoPlayerView> sVideoViews = null;
    static VideoHandler mVideoHandler = null;
    private static Handler sHandler = null;
    private AlphaLayoutPosition mAlphaLayoutPosition = AlphaLayoutPosition.Bottom;

    private IVideoPlayerView mViewPlayView;
    private boolean mIsAlphaVideo = false;

    public CocosVideoHelper(Activity activity, FrameLayout layout)
    {
        mActivity = activity;
        mLayout = layout;
        mVideoHandler = new VideoHandler(this);
        if (sVideoViews == null){
            sVideoViews = new SparseArray<IVideoPlayerView>();
        }
        sHandler = new Handler(Looper.myLooper());
    }

    private static int videoTag = 0;
    private final static int VideoTaskCreate = 0;
    private final static int VideoTaskRemove = 1;
    private final static int VideoTaskSetSource = 2;
    private final static int VideoTaskSetRect = 3;
    private final static int VideoTaskStart = 4;
    private final static int VideoTaskPause = 5;
    private final static int VideoTaskResume = 6;
    private final static int VideoTaskStop = 7;
    private final static int VideoTaskSeek = 8;
    private final static int VideoTaskSetVisible = 9;
    private final static int VideoTaskRestart = 10;
    private final static int VideoTaskKeepRatio = 11;
    private final static int VideoTaskFullScreen = 12;
    private final static int VideoTaskSetVolume = 13;
    private final static int VideoTaskSetMute = 14;
    private final static int VideoTaskSetLoop = 15;
    private final static int VideoTaskSetPlaybackRate = 16;
    private final static int VideoTaskSetStayOnBottom = 17;
    private final static int VideoTaskSetAlphaEnabled = 18;
    private final static int VideoTaskSetAlphaLayoutPosition = 19;

    final static int KeyEventBack = 1000;

    static class VideoHandler extends Handler{
        WeakReference<CocosVideoHelper> mReference;

        VideoHandler(CocosVideoHelper helper){
            mReference = new WeakReference<CocosVideoHelper>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            CocosVideoHelper helper = mReference.get();
            if (helper == null) {
                return;
            }
            switch (msg.what) {
            case VideoTaskCreate: {
                /// lazy create
//                helper._createVideoView(msg.arg1);
                break;
            }
            case VideoTaskRemove: {
                helper._removeVideoView(msg.arg1);
                break;
            }
            case VideoTaskSetSource: {
                helper._setVideoURL(msg.arg1, msg.arg2, (String)msg.obj);
                break;
            }
            case VideoTaskStart: {
                helper._startVideo(msg.arg1);
                break;
            }
            case VideoTaskSetRect: {
                Rect rect = (Rect)msg.obj;
                helper._setVideoRect(msg.arg1, rect.left, rect.top, rect.right, rect.bottom);
                break;
            }
            case VideoTaskFullScreen:{
                if (msg.arg2 == 1) {
                    helper._setFullScreenEnabled(msg.arg1, true);
                } else {
                    helper._setFullScreenEnabled(msg.arg1, false);
                }
                break;
            }
            case VideoTaskPause: {
                helper._pauseVideo(msg.arg1);
                break;
            }
            case VideoTaskResume: {
                helper._resumeVideo(msg.arg1);
                break;
            }
            case VideoTaskStop: {
                helper._stopVideo(msg.arg1);
                break;
            }
            case VideoTaskSeek: {
                helper._seekVideoTo(msg.arg1, msg.arg2);
                break;
            }
            case VideoTaskSetVisible: {
                if (msg.arg2 == 1) {
                    helper._setVideoVisible(msg.arg1, true);
                } else {
                    helper._setVideoVisible(msg.arg1, false);
                }
                break;
            }
            case VideoTaskKeepRatio: {
                if (msg.arg2 == 1) {
                    helper._setVideoKeepRatio(msg.arg1, true);
                } else {
                    helper._setVideoKeepRatio(msg.arg1, false);
                }
                break;
            }
            case KeyEventBack: {
                helper.onBackKeyEvent();
                break;
            }
            case VideoTaskSetVolume: {
                float volume = (float) msg.arg2 / 10;
                helper._setVolume(msg.arg1, volume);
                break;
            }
            case VideoTaskSetMute: {
                boolean enable = msg.arg2 == 1;
                helper._setMute(msg.arg1, enable);
                break;
            }
            case VideoTaskSetLoop: {
                boolean enable = msg.arg2 == 1;
                helper._setLoop(msg.arg1, enable);
                break;
            }
            case VideoTaskSetPlaybackRate: {
                float rate = (float) msg.arg2 / 10;
                helper._setPlaybackRate(msg.arg1, rate);
                break;
            }
            case VideoTaskSetStayOnBottom: {
                boolean enable = msg.arg2 == 1;
                helper._setStayOnBottom(msg.arg1, enable);
                break;
            }
            case VideoTaskSetAlphaEnabled: {
                boolean enable = msg.arg2 == 1;
                helper._setAlphaEnabled(msg.arg1, enable);
                break;
            }
            case VideoTaskSetAlphaLayoutPosition: {
                helper._setAlphaLayoutPosition(msg.arg1, msg.arg2);
                break;
            }
            default:
                break;
            }

            super.handleMessage(msg);
        }
    }

    public static native void nativeExecuteVideoCallback(int index,int event);

    IVideoPlayerView.OnVideoEventListener videoEventListener = new IVideoPlayerView.OnVideoEventListener() {

        @Override
        public void onVideoEvent(int tag,int event) {
            CocosHelper.runOnGameThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("[Mocos] videoHelper--", "onVideoEvent: " + event);
                    nativeExecuteVideoCallback(tag, event);
                }
            });
        }
    };

    public static int createVideoWidget() {
        Message msg = new Message();
        msg.what = VideoTaskCreate;
        msg.arg1 = videoTag;
        mVideoHandler.sendMessage(msg);

        return videoTag++;
    }

    public void setVideoPlayerView(IVideoPlayerView view){
        mViewPlayView = view;
    }

    private void _createVideoView(int index) {
        if (mIsAlphaVideo && mViewPlayView != null) {
            IVideoPlayerView videoView = mViewPlayView;
            videoView.setViewTag(index);
            sVideoViews.put(index, videoView);
            FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            mLayout.addView(videoView.getView(), lParams);
            videoView.setVideoViewEventListener(videoEventListener);
        } else {
            CocosVideoView videoView = new CocosVideoView(mActivity, index);
            sVideoViews.put(index, videoView);
            FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            mLayout.addView(videoView, lParams);

            videoView.setZOrderOnTop(false);
            videoView.setVideoViewEventListener(videoEventListener);
        }
    }

    public static void removeVideoWidget(int index){
        Message msg = new Message();
        msg.what = VideoTaskRemove;
        msg.arg1 = index;
        mVideoHandler.sendMessage(msg);
    }

    private void _removeVideoView(int index) {
        IVideoPlayerView view = sVideoViews.get(index);
        if (view != null) {
            view.stopPlayback();
            sVideoViews.remove(index);
            if(view.getView() != null){
                mLayout.removeView(view.getView());
            }
        }
    }

    public static void setVideoUrl(int index, int videoSource, String videoUrl) {
        Message msg = new Message();
        msg.what = VideoTaskSetSource;
        msg.arg1 = index;
        msg.arg2 = videoSource;
        msg.obj = videoUrl;
        mVideoHandler.sendMessage(msg);
    }

    private void _setVideoURL(int index, int videoSource, String videoUrl) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        Log.d("[Mocos] videoHelper--", "_setVideoURL: " + videoUrl);
        if (videoView == null) {
            _createVideoView(index);
            videoView = sVideoViews.get(index);
        }
        if (videoView != null) {
            switch (videoSource) {
            case 0:
                videoView.setVideoFileName(videoUrl);
                break;
            case 1:
                videoView.setVideoURL(videoUrl);
                break;
            default:
                break;
            }
        }
    }

    public static void setVideoRect(int index, int left, int top, int maxWidth, int maxHeight) {
        Message msg = new Message();
        msg.what = VideoTaskSetRect;
        msg.arg1 = index;
        msg.obj = new Rect(left, top, maxWidth, maxHeight);
        mVideoHandler.sendMessage(msg);
    }

    private void _setVideoRect(int index, int left, int top, int maxWidth, int maxHeight) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        Log.d("[Mocos] videoHelper--", "_setVideoRect: " + maxWidth);
        if (videoView != null) {
            videoView.setVideoRect(left, top, maxWidth, maxHeight);
        }
    }

    public static void setFullScreenEnabled(int index, boolean enabled) {
        Message msg = new Message();
        msg.what = VideoTaskFullScreen;
        msg.arg1 = index;
        if (enabled) {
            msg.arg2 = 1;
        } else {
            msg.arg2 = 0;
        }
        mVideoHandler.sendMessage(msg);
    }

    private void _setFullScreenEnabled(int index, boolean enabled) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.setFullScreenEnabled(enabled);
        }
    }

    private void onBackKeyEvent() {
        int viewCount = sVideoViews.size();
        for (int i = 0; i < viewCount; i++) {
            int key = sVideoViews.keyAt(i);
            IVideoPlayerView videoView = sVideoViews.get(key);
            if (videoView != null) {
                videoView.setFullScreenEnabled(false);
                CocosHelper.runOnGameThreadAtForeground(new Runnable() {
                    @Override
                    public void run() {
                        nativeExecuteVideoCallback(key, KeyEventBack);
                    }
                });
            }
        }
    }

    public static void setMute(int index, boolean enable) {
        Message msg = new Message();
        msg.what = VideoTaskSetMute;
        msg.arg1 = index;
        msg.arg2 = enable ? 1 : 0;
        mVideoHandler.sendMessage(msg);
    }

    private void _setMute(int index, boolean enable) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            /// todo: videoView 实现 setMute
            Log.d("[Mocos] videoHelper--", "_setMute: " + enable);
        }
    }

    public static void setLoop(int index, boolean enable) {
        Message msg = new Message();
        msg.what = VideoTaskSetLoop;
        msg.arg1 = index;
        msg.arg2 = enable ? 1 : 0;
        mVideoHandler.sendMessage(msg);
    }

    private void _setLoop(int index, boolean enable) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            Log.d("[Mocos] videoHelper--", "_setLoop: " + enable);
            videoView.setLoop(enable);
        }
    }

    public static void setPlaybackRate(int index, float value) {
        Message msg = new Message();
        msg.what = VideoTaskSetPlaybackRate;
        msg.arg1 = index;
        msg.arg2 = (int)(value * 10);
        mVideoHandler.sendMessage(msg);
    }

    private void _setPlaybackRate(int index, float value) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            /// todo: videoView 实现 setPlaybackRate
            Log.d("[Mocos] videoHelper--", "_setPlaybackRate: " + value);
        }
    }

    public static void setStayOnBottom(int index, boolean enable) {
        Message msg = new Message();
        msg.what = VideoTaskSetStayOnBottom;
        msg.arg1 = index;
        msg.arg2 = enable ? 1 : 0;
        mVideoHandler.sendMessage(msg);
    }

    private void _setStayOnBottom(int index, boolean enable) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.setStayOnBottom(enable);
            Log.d("[Mocos] videoHelper--", "_setStayOnBottom: " + enable);
        }
    }

    public static void startVideo(int index) {
        Message msg = new Message();
        msg.what = VideoTaskStart;
        msg.arg1 = index;
        mVideoHandler.sendMessage(msg);
    }

    private void _startVideo(int index) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        Log.d("[Mocos] videoHelper--", "_startVideo");
        if (videoView != null) {
            videoView.start();
        }
    }

    public static void pauseVideo(int index) {
        Message msg = new Message();
        msg.what = VideoTaskPause;
        msg.arg1 = index;
        mVideoHandler.sendMessage(msg);
    }

    private void _pauseVideo(int index) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.pause();
        }
    }

    public static void resumeVideo(int index) {
        Message msg = new Message();
        msg.what = VideoTaskResume;
        msg.arg1 = index;
        mVideoHandler.sendMessage(msg);
    }

    private void _resumeVideo(int index) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            Log.d("[Mocos] videoHelper--", "_resumeVideo");
            videoView.resume();
        }
    }

    public static void stopVideo(int index) {
        Message msg = new Message();
        msg.what = VideoTaskStop;
        msg.arg1 = index;
        mVideoHandler.sendMessage(msg);
    }

    private void _stopVideo(int index) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.stop();
        }
    }

    public static void seekVideoTo(int index,int msec) {
        Message msg = new Message();
        msg.what = VideoTaskSeek;
        msg.arg1 = index;
        msg.arg2 = msec;
        mVideoHandler.sendMessage(msg);
    }

    private void _seekVideoTo(int index,int msec) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.seekTo(msec);
        }
    }

    public static float getCurrentTime(final int index) {
        IVideoPlayerView video = sVideoViews.get(index);
        float currentPosition = -1;
        if (video != null) {
            currentPosition = video.getCurrentPosition() / 1000.0f;
        }
        return currentPosition;
    }

    public  static  float getDuration(final int index) {
        IVideoPlayerView video = sVideoViews.get(index);
        float duration = -1;
        if (video != null) {
            duration = video.getDuration() / 1000.0f;
        }
        if (duration <= 0) {
            Log.w("CocosVideoHelper", "Video player's duration is not ready to get now!");
        }
        return duration;
    }

    public static void setVideoVisible(int index, boolean visible) {
        Message msg = new Message();
        msg.what = VideoTaskSetVisible;
        msg.arg1 = index;
        if (visible) {
            msg.arg2 = 1;
        } else {
            msg.arg2 = 0;
        }

        mVideoHandler.sendMessage(msg);
    }

    private void _setVideoVisible(int index, boolean visible) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            if (visible) {
                videoView.fixSize();
                videoView.setVisibility(View.VISIBLE);
            } else {
                videoView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public static void setVideoKeepRatioEnabled(int index, boolean enable) {
        Message msg = new Message();
        msg.what = VideoTaskKeepRatio;
        msg.arg1 = index;
        if (enable) {
            msg.arg2 = 1;
        } else {
            msg.arg2 = 0;
        }
        mVideoHandler.sendMessage(msg);
    }

    private void _setVideoKeepRatio(int index, boolean enable) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.setKeepRatio(enable);
        }
    }

    private void _setVolume(final int index, final float volume) {
        IVideoPlayerView videoView = sVideoViews.get(index);
        if (videoView != null) {
            videoView.setVolume(volume);
        }
    }

    public static void setVolume(final int index, final float volume) {
        Message msg = new Message();
        msg.what = VideoTaskSetVolume;
        msg.arg1 = index;
        msg.arg2 = (int) (volume * 10);
        mVideoHandler.sendMessage(msg);
    }

    public static void setAlphaEnabled(int index, boolean enable) {
        Message msg = new Message();
        msg.what = VideoTaskSetAlphaEnabled;
        msg.arg1 = index;
        msg.arg2 = enable ? 1 : 0;
        mVideoHandler.sendMessage(msg);
    }

    private void _setAlphaEnabled(int index, boolean enable) {
        Log.d("[Mocos] videoHelper--", "_setAlphaEnabled: " + enable);
        mIsAlphaVideo = enable;
    }

    public static void setAlphaLayoutPosition(int index, int pos) {
        Message msg = new Message();
        msg.what = VideoTaskSetAlphaLayoutPosition;
        msg.arg1 = index;
        msg.arg2 = pos;
        mVideoHandler.sendMessage(msg);
    }

    private void _setAlphaLayoutPosition(int index, int pos) {
        Log.d("[Mocos] videoHelper--", "_setAlphaLayoutPosition: " + pos);
        switch (pos) {
            case 0: {
                mAlphaLayoutPosition = AlphaLayoutPosition.Bottom;
                break;
            }
            case 1: {
                mAlphaLayoutPosition = AlphaLayoutPosition.Right;
                break;
            }
            case 2: {
                mAlphaLayoutPosition = AlphaLayoutPosition.Top;
                break;
            }
            case 3: {
                mAlphaLayoutPosition = AlphaLayoutPosition.Left;
                break;
            }
            default:
                mAlphaLayoutPosition = AlphaLayoutPosition.Left;
        }
    }
}
