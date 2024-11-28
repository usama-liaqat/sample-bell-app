package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack



data class VideoItem(
    val name: String,
    val videoTrack: VideoTrack? = null,
    val mirror: Boolean = false

)

class VideoViewAdapter(
    private val items: MutableList<VideoItem>,
    private val rootEglBase:EglBase = EglBase.create()
) : RecyclerView.Adapter<VideoViewAdapter.ViewHolder>() {

    private val mutex = Mutex()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val surfaceViewRenderer: SurfaceViewRenderer = view.findViewById(R.id.surfaceViewRenderer)
        val overlayName: TextView = view.findViewById(R.id.overlayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.overlayName.text = item.name
        val videoTrack = item.videoTrack
        
        val renderer = holder.surfaceViewRenderer
        if (videoTrack != null) {
            renderer.init(rootEglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            renderer.setMirror(item.mirror)
            renderer.setZOrderMediaOverlay(true)
            videoTrack.addSink(renderer)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.surfaceViewRenderer.release()
    }

    override fun getItemCount(): Int = items.size

    private suspend fun addItem(item: VideoItem) {
        mutex.withLock {
            items.add(item)
            notifyItemInserted(items.size - 1)
        }
    }

    private suspend fun removeItem(position: Int) {
        mutex.withLock {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private suspend fun replaceItem(position: Int, item: VideoItem) {
        mutex.withLock {
            items[position] = items[position].copy(videoTrack = item.videoTrack)
            notifyItemChanged(position)
        }
    }

    fun findAndRemoveItemByName(name: String) {
        val index = items.indexOfFirst { it.name == name }
        if (index != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    removeItem(index)
                }
            }
        }
    }

    fun addOrUpdateItem(item: VideoItem) {
        val index = items.indexOfFirst { it.name == item.name }
        if (index != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    replaceItem(index, item)
                }
            }
        } else {

            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    addItem(item)
                }
            }
        }
    }
}