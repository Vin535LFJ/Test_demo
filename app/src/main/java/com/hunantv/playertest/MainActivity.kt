package com.hunantv.playertest

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.cocos.bridge.CocosBridgeHelper
import com.cocos.bridge.CocosDataListener
import com.cocos.helper.CocosDBHelper
import com.cocos.lib.CocosHelper
import com.cocos.lib.CocosPlayer
import com.cocos.lib.CocosVideoHelper
import com.hunantv.effect.R
import org.json.JSONArray

class MainActivity : AppCompatActivity() {


    var btnDownload1: Button? = null
    var btnDownload2: Button? = null


    private var mVideoHelper: CocosVideoHelper? = null

    private val cocosListenerInMain: CocosDataListener = CocosDataListener { action, argument, callbackId ->
        CocosBridgeHelper.log("接收InMain", action)
        if (action == "action_appVersion") {
            CocosBridgeHelper.getInstance().main2Cocos(action, packageManager.getPackageInfo(packageName, 0).versionName, callbackId)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 20)

//        DownloadUtil.instance.addListener(downloadListener)
        CocosBridgeHelper.getInstance().addMainListener(cocosListenerInMain)

        val outPathString =
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath

        val cocosLayout = findViewById<FrameLayout>(R.id.cocos)

        val cocosSurfaceView = CocosDanmakuSurfaceView(this, CocosPlayer.getInstance())
        cocosLayout.addView(cocosSurfaceView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        cocosSurfaceView.setZOrderMediaOverlay(true)

        val path = ArrayList<String>()
        path.add("${outPathString}/assets")
        val pathStr = JSONArray(path)
        CocosDBHelper(this).set("HotUpdateSearchPaths", pathStr.toString())
        CocosHelper.setDebugEnabled(true)
        val rootLayout = findViewById<FrameLayout>(R.id.myframe)
        if (mVideoHelper == null) {
            mVideoHelper = CocosVideoHelper(this, rootLayout)

            mVideoHelper?.setVideoPlayerView(MgtvCocosPlayerView(this , 0, true))
        }

        CocosPlayer.getInstance().loadEngine(this, applicationContext)
        CocosPlayer.getInstance().registerSurfaceViewForRendering(cocosSurfaceView)
        CocosPlayer.getInstance().startCocosEngine()
        btnDownload1 = findViewById<AppCompatButton>(R.id.btn_download1).apply {

        }
        btnDownload2 = findViewById<AppCompatButton>(R.id.btn_download2).apply {

        }

        CocosBridgeHelper.log("主进程", "---")

    }

    override fun onResume() {
        super.onResume()

        CocosPlayer.getInstance().onLifecycleResume()
    }

    override fun onStop() {
        super.onStop()

        CocosPlayer.getInstance().onLifecycleStop()
    }

    override fun onStart() {
        super.onStart()

        CocosPlayer.getInstance().onLifecycleStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        val state: ByteArray = CocosPlayer.getInstance().onSaveInstanceStateNative()
//        if (state != null) {
//            outState.putByteArray("android:native_state", state)
//        }
    }

    override fun onPause() {
        super.onPause()

        CocosPlayer.getInstance().onLifecyclePause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        CocosPlayer.getInstance().onConfigRationChanged()
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        CocosPlayer.getInstance().onWindowFocusChanged(hasFocus)
    }


    private inner class CocosDanmakuSurfaceView(
        context: Context,
        private val mCocosPlayer: CocosPlayer?
    ) : SurfaceView(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {

            if (mCocosPlayer != null) {
                val touch = mCocosPlayer.onTouchEvent(event)

                return touch
            }
            return super.onTouchEvent(event)
        }
    }
}