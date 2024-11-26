package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import android.view.ViewGroup



data class SurfaceItem(
    val name: String,
    val renderer: SurfaceViewRenderer? = null
)

class VideoViewAdapter(private val items: MutableList<SurfaceItem>) :
    RecyclerView.Adapter<VideoViewAdapter.SurfaceViewHolder>() {

    inner class SurfaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val surfaceViewRenderer: SurfaceViewRenderer = view.findViewById(R.id.surfaceViewRenderer)
        val overlayName: TextView = view.findViewById(R.id.overlayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurfaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_card, parent, false)
        return SurfaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SurfaceViewHolder, position: Int) {
        val item = items[position]
        holder.overlayName.text = item.name

        val renderer = item.renderer
        if (renderer != null) {
            holder.surfaceViewRenderer.init(EglBase.create().eglBaseContext, null)
            holder.surfaceViewRenderer.setMirror(false)

            // You can bind a video track here if available
            // For example: renderer.setVideoTrack(videoTrack)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: SurfaceItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        items[position].renderer?.release()
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun findAndRemoveItemByName(name: String) {
        val index = items.indexOfFirst { it.name == name }
        if (index != -1) {
            removeItem(index)
        }
    }

    fun updateRendererForItem(position: Int, newRenderer: SurfaceViewRenderer?) {
        items[position] = items[position].copy(renderer = newRenderer)
        notifyItemChanged(position)
    }
}