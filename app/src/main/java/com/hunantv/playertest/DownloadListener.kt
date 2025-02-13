package com.hunantv.playertest

interface DownloadListener {
    fun callBack(taskId: Int, status: Int, progress: Int)
}