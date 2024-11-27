package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import android.view.ViewGroup
import org.webrtc.VideoTrack



data class VideoItem(
    val name: String,
    val videoTrack: VideoTrack? = null,
    val mirror: Boolean = false

)

class VideoViewAdapter(private val items: MutableList<VideoItem>) :
    RecyclerView.Adapter<VideoViewAdapter.ViewHolder>() {

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
        if (videoTrack != null) {
            holder.surfaceViewRenderer.init(EglBase.create().eglBaseContext, null)
            holder.surfaceViewRenderer.setMirror(item.mirror)
            holder.surfaceViewRenderer.setZOrderMediaOverlay(true)
            videoTrack.addSink(holder.surfaceViewRenderer)

        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: VideoItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun findAndRemoveItemByName(name: String) {
        val index = items.indexOfFirst { it.name == name }
        if (index != -1) {
            removeItem(index)
        }
    }

    fun addOrUpdateItem(item: VideoItem) {
        val index = items.indexOfFirst { it.name == item.name }
        if (index != -1) {
            items[index] = items[index].copy(videoTrack = item.videoTrack)
            notifyItemChanged(index)
        } else {
            addItem(item)
        }

    }
}