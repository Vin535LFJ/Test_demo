/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2014-2016 Chukong Technologies Inc.
 * Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hunantv.playertest;

import android.app.Activity;
import android.graphics.Point;
import android.view.View;
import android.widget.Toast;

import com.cocos.lib.IVideoPlayerView;
import com.hunantv.media.alpha.MgtvAlphaVideoView;
import com.hunantv.media.player.pragma.DebugLog;
import com.hunantv.media.player.utils.FileUtil;
import com.hunantv.media.widget.IVideoView;

import java.io.IOException;

public class MgtvVideoPlayerView implements IVideoPlayerView {
    private String url = "https://video.da.mgtv.com/new_video/2022/06/25/1038/AA95C79FB79D07E9FB9CC7F2D3210645_20220625_1_1_2418.mp4";
    private MgtvAlphaVideoView mMgtvVideoView;
    private Activity mActivity;

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

    public MgtvVideoPlayerView(Activity activity, int tag) {
        this.mActivity = activity;
        mViewTag = tag;

        mMgtvVideoView = new MgtvAlphaVideoView(activity, MgtvAlphaVideoView.MGTV_RENDER_TEXTURE_VIEW);
        mMgtvVideoView.setOnPreparedListener(new IVideoView.OnPreparedListener() {
            @Override
            public void onPrepared() {
                mMgtvVideoView.start();
                sendEvent(EVENT_META_LOADED);
                sendEvent(EVENT_READY_TO_PLAY);
            }
        });

        mMgtvVideoView.setOnCompletionListener(new IVideoView.OnCompletionListener() {
            @Override
            public void onCompletion(int i, int i1) {
                sendEvent(EVENT_COMPLETED);
            }
        });
        mMgtvVideoView.setOnErrorListener(new IVideoView.OnErrorListener() {
            @Override
            public boolean onError(int what, int extra) {
                DebugLog.e("test", "onError what: " + what + ", extra:" + extra + " 包名：" + activity.getPackageName());
                Toast.makeText(activity, "onError what: " + what + ", extra:" + extra, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @Override
    public View getView() {
        return mMgtvVideoView;
    }

    @Override
    public void setVideoURL(String url) {
        mIsAssetResource = false;
        mMgtvVideoView.setVideoPath(urlToPath(url));
    }

    private boolean mIsAssetResource = false;
    private String mVideoFilePath = null;                                                                   
    @Override
    public void setVideoFileName(String filePath) {
        if (filePath.startsWith(AssetResourceRoot)) {
            filePath = filePath.substring(AssetResourceRoot.length());
        }
        if (filePath.startsWith("/")) {
            mIsAssetResource = false;
            mMgtvVideoView.setVideoPath(filePath);
        } else {
            mMgtvVideoView.setVideoPath(urlToPath(filePath));
        }
    }

    private String urlToPath(String filePath){
        final String path = mActivity.getExternalCacheDir().getAbsolutePath() + filePath;
        try {
            FileUtil.copyAssertThrow(mActivity, filePath, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public void start() {
        mMgtvVideoView.start();
        this.sendEvent(EVENT_PLAYING);

    }

    @Override
    public void stop() {
        mMgtvVideoView.stop();
        this.sendEvent(EVENT_STOPPED);
    }

    @Override
    public void stopPlayback() {
        mMgtvVideoView.release();

    }

    @Override
    public void pause() {
        mMgtvVideoView.pause();
        this.sendEvent(EVENT_PAUSED);

    }

    @Override
    public void resume() {

    }

    @Override
    public void seekTo(int ms) {
        mMgtvVideoView.seekTo(ms);
    }

    @Override
    public int getDuration() {
        return mMgtvVideoView.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mMgtvVideoView.getCurrentPosition();
    }

    @Override
    public void setVolume(float volume) {
        if (mMgtvVideoView != null) {
            mMgtvVideoView.setVolume(volume, volume);
        }
    }

    @Override
    public void setLoop(boolean enable) {

    }

    @Override
    public void setKeepRatio(boolean enabled) {
        mKeepRatio = enabled;
        fixSize();
    }

    @Override
    public void setVideoRect(int left, int top, int maxWidth, int maxHeight) {
        if (mViewLeft == left && mViewTop == top && mViewWidth == maxWidth && mViewHeight == maxHeight)
            return;

        mViewLeft = left;
        mViewTop = top;
        mViewWidth = maxWidth;
        mViewHeight = maxHeight;
        mLoop = true;

        fixSize(mViewLeft, mViewTop, mViewWidth, mViewHeight);
    }

    public void fixSize(int left, int top, int width, int height) {
        if (mVideoWidth == 0 || mVideoHeight == 0) {
            mVisibleLeft = left;
            mVisibleTop = top;
            mVisibleWidth = width;
            mVisibleHeight = height;
        }
        else if (width != 0 && height != 0) {
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
        }
        else {
            mVisibleLeft = left;
            mVisibleTop = top;
            mVisibleWidth = mVideoWidth;
            mVisibleHeight = mVideoHeight;
        }

//        getHolder().setFixedSize(mVisibleWidth, mVisibleHeight);
//
//        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT);
//        lParams.leftMargin = mVisibleLeft;
//        lParams.topMargin = mVisibleTop;
//        setLayoutParams(lParams);
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

    private OnVideoEventListener mOnVideoEventListener;
    @Override
    public void setVideoViewEventListener(OnVideoEventListener l) {
        mOnVideoEventListener = l;
    }

    @Override
    public void setViewTag(int viewTag) {

    }

    @Override
    public void setStayOnBottom(boolean enable) {

    }

    private int mViewTag = 0;
    private void sendEvent(int event) {
        if (this.mOnVideoEventListener != null) {
            this.mOnVideoEventListener.onVideoEvent(this.mViewTag, event);
        }
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

}
