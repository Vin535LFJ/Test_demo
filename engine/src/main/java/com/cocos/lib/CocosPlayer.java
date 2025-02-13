package com.cocos.lib;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

/**
 * @Description: cocos player
 * @Author: starzhang
 * @Date: 2024/3/18 14:47
 */
public class CocosPlayer implements SurfaceHolder.Callback2 {

    private static volatile CocosPlayer instance;
    private int curSurface;
    private boolean destroyed = true;
    private long nativeHandle;
    private CocosSensorHandler sensorHandler;

    static {
        onLoadNativeLibraries();
    }

    private RenderSurface mRenderSurface;

    private CocosPlayer() {
    }

    private static void onLoadNativeLibraries() {
        try {
            System.loadLibrary("cocos");
        } catch (Exception e16) {
            e16.printStackTrace();
        }
    }

    private void initHelper(Context context) {
        this.sensorHandler = new CocosSensorHandler(context);
    }

    private static String getAbsolutePath(File file) {
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }
    //region public functions
    public static CocosPlayer getInstance() {
        if (instance == null) {
            synchronized (CocosPlayer.class) {
                if (instance == null) {
                    instance = new CocosPlayer();
                }
            }
        }
        return instance;
    }


    public void loadEngine(Activity activity, Context context) {
        if (!this.destroyed || activity == null) {
            return;
        }

        initHelper(context);
        initJNI(activity);
        GlobalObject.init(context, activity);
//        GlobalObject.context = context;
        CocosHelper.registerBatteryLevelReceiver(context);
        CocosHelper.init();
        CanvasRenderingContext2DImpl.init(context);
        activity.setVolumeControlStream(3);
        String findLibrary = ((BaseDexClassLoader) context.getClassLoader()).findLibrary("cocos");
        if (findLibrary == null) {
            throw new IllegalArgumentException("Unable to find native library " + "cocos" + " using classloader: " + context.getClassLoader().toString());
        } else {
            this.nativeHandle = this.loadNativeCode(activity, findLibrary, "GameActivity_onCreate", getAbsolutePath(context.getFilesDir()), getAbsolutePath(context.getObbDir()), getAbsolutePath(context.getExternalFilesDir((String)null)), context.getAssets(), null);
            if (this.nativeHandle == 0L) {
                throw new UnsatisfiedLinkError("Unable to load native library \"" + "cocos" + "\": " + this.getDlError());
            }
        }
        this.destroyed = false;
    }

    public void destroyEngine() {
        this.destroyed = true;
        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
//        releaseJavaRef(j16);
        onSurfaceDestroyedNative(this.nativeHandle);
        unloadNativeCode(this.nativeHandle);
//        destroyJNI();
        CocosHelper.destroy();
//        CocosHelper.unregisterBatteryLevelReceiver(GlobalObject.context);
        CanvasRenderingContext2DImpl.destroy();
        GlobalObject.destroy();
    }

    public void resumeCocosEngine() {
        if (destroyed) {
            return;
        }
        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onResumeNative(j16);
        this.sensorHandler.onResume();
    }

    public void startCocosEngine() {
        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onStartNative(j16);
    }

    public void stopCocosEngine() {
        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onStopNative(j16);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (destroyed) {
            return false;
        }
        int action = Build.VERSION.SDK_INT >= 23 ? event.getActionButton() : 0;
        int cls = Build.VERSION.SDK_INT >= 29 ? event.getClassification() : 0;
        return this.onTouchEventNative(this.nativeHandle, event, event.getPointerCount(), event.getHistorySize(), event.getDeviceId(), event.getSource(), event.getAction(), event.getEventTime(), event.getDownTime(), event.getFlags(), event.getMetaState(), action, event.getButtonState(), cls, event.getEdgeFlags(), event.getXPrecision(), event.getYPrecision());
    }

    public void onWindowFocusChanged(boolean focus) {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onWindowFocusChangedNative(j16, focus);
    }

    public void createSurfaceViewForRendering(Context context) {
        registerSurfaceViewForRendering(new SurfaceView(context));
    }

    public void registerSurfaceViewForRendering(SurfaceView surfaceView) {
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setFormat(-3);
    }

    public void registerTextureViewForRendering(TextureView textureView) {
        attach(textureView);
    }

    private void attach(TextureView textureView) {
        textureView.setOpaque(false);

        mRenderSurface = new TextureViewHandler(textureView);
        TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                if (destroyed) {
                    return;
                }

                Surface surface = new Surface(surfaceTexture);

                TextureViewHandler textureViewHandler = (TextureViewHandler) mRenderSurface;
                textureViewHandler.setSurface(surface);

                onSurfaceCreatedNative(nativeHandle, surface);
                curSurface = surface.hashCode();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                if (destroyed) {
                    return;
                }

                if (mRenderSurface != null && mRenderSurface.getSurface() != null) {
                    onSurfaceChangedNative(nativeHandle, mRenderSurface.getSurface(), 0, width, height);
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {

                if (destroyed) {
                    return true;
                }
                onSurfaceDestroyedNative(nativeHandle);
                detach();
                curSurface = 0;
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        };

        textureView.setSurfaceTextureListener(listener);
    }

    private void detach() {
        if (mRenderSurface != null) {
            mRenderSurface.detach();
        }
    }

    public void onLifecycleStart() {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onStartNative(j16);
    }

    public void onLifecyclePause() {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onPauseNative(j16);
    }

    public void onLifecycleResume() {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onResumeNative(j16);
    }

    public void onLifecycleStop() {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }
        onStopNative(j16);
    }

    public void onConfigRationChanged() {
        if (destroyed) {
            return;
        }

        long j16 = this.nativeHandle;
        if (j16 == 0) {
            return;
        }

        onConfigurationChangedNative(j16);
    }
    //endregion

    //region native code
    private native void initJNI(Activity activity);

    protected native long loadNativeCode(Activity activity, String var1, String var2, String var3, String var4, String var5, AssetManager var6, byte[] var7);

    protected native String getDlError();

    protected native void unloadNativeCode(long var1);

    protected native void onStartNative(long var1);

    protected native void onResumeNative(long var1);

    protected native byte[] onSaveInstanceStateNative(long var1);

    protected native void onPauseNative(long var1);

    protected native void onStopNative(long var1);

    protected native void onConfigurationChangedNative(long var1);

    protected native void onTrimMemoryNative(long var1, int var3);

    protected native void onWindowFocusChangedNative(long var1, boolean var3);

    protected native void onSurfaceCreatedNative(long var1, Surface var3);

    protected native void onSurfaceChangedNative(long var1, Surface var3, int var4, int var5, int var6);

    protected native void onSurfaceRedrawNeededNative(long var1, Surface var3);

    protected native void onSurfaceDestroyedNative(long var1);

    protected native boolean onTouchEventNative(long var1, MotionEvent var3, int var4, int var5, int var6, int var7, int var8, long var9, long var11, int var13, int var14, int var15, int var16, int var17, int var18, float var19, float var20);

    protected native boolean onKeyDownNative(long var1, KeyEvent var3);

    protected native boolean onKeyUpNative(long var1, KeyEvent var3);

    protected native void onWindowInsetsChangedNative(long var1);

    //endregion

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i16, int i17, int i18) {
        if (this.destroyed) {
            return;
        }
        onSurfaceChangedNative(this.nativeHandle, surfaceHolder.getSurface(), i16, i17, i18);
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (this.destroyed) {
            return;
        }
        onSurfaceCreatedNative(this.nativeHandle, surfaceHolder.getSurface());
        this.curSurface = surfaceHolder.getSurface().hashCode();
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (this.destroyed) {
            return;
        }
        onSurfaceDestroyedNative(this.nativeHandle);
        this.curSurface = 0;
    }

    @Override // android.view.SurfaceHolder.Callback2
    public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
        if (this.destroyed) {
            return;
        }
        onSurfaceRedrawNeededNative(this.nativeHandle, surfaceHolder.getSurface());
    }

    private interface RenderSurface {
        void resize(int width, int height);
        void detach();
        Surface getSurface();
    }

    private class TextureViewHandler implements RenderSurface {
        private TextureView mTextureView;
        private Surface mSurface;

        TextureViewHandler(TextureView surface) { mTextureView = surface; }

        @Override
        public void resize(int width, int height) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                mTextureView.getSurfaceTexture().setDefaultBufferSize(width, height);
            }
            // the call above won't cause TextureView.onSurfaceTextureSizeChanged()
        }

        @Override
        public void detach() {
            setSurface(null);
        }

        @Override
        public Surface getSurface() {
            return mSurface;
        }

        void setSurface(Surface surface) {
            if (surface == null) {
                if (mSurface != null) {
                    mSurface.release();
                }
            }
            mSurface = surface;
        }
    }
}
