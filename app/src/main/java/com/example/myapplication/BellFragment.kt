package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.exchange.SocketExchange
import com.example.myapplication.exchange.WHIPExchange
import com.example.myapplication.webrtc.PeerFactory
import com.example.myapplication.webrtc.WHIPPeer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.EglBase
import org.webrtc.VideoCapturer


class BellFragment : Fragment() {
    private val TAG = "BellFragment"

    private lateinit var videoViewAdapter: VideoViewAdapter
    private lateinit var recyclerView: RecyclerView

    private val videoItems = mutableListOf<VideoItem>()


    private lateinit var videoCapturer: VideoCapturer

    private lateinit var whipExchange: WHIPExchange
    private var whipPeer: WHIPPeer? = null

    private lateinit var socketExchange: SocketExchange
    private lateinit var publishButton: Button

    private lateinit var rootEglBase: EglBase
    private lateinit var peerFactory: PeerFactory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bell_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val context = requireContext()

        recyclerView = view.findViewById(R.id.recyclerView)
        videoViewAdapter = VideoViewAdapter(videoItems)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = videoViewAdapter


        rootEglBase = EglBase.create()
        peerFactory = PeerFactory(activity, rootEglBase)

        videoCapturer = getVideoCapturer(activity)
        publishButton = view.findViewById(R.id.bellPublishButton)

        val sid = arguments?.getString(ARG_INPUT_SID)
        if (sid != null) {
            connection(sid, activity)
        }
    }

    private fun connection(sid: String, activity: Activity) {
        lifecycleScope.launch(Dispatchers.IO) {
            peerFactory.getIceServers(sid)
        }
        whipExchange = WHIPExchange(Config.LIVE_BASE_URL, sid)
        socketExchange = SocketExchange(sid)
        socketExchange.connect()
        subscribeSocketEvents()
        publishButton.setOnClickListener {
            whipPeer?.let {
                it.close()
            }
            whipPeer = WHIPPeer(activity, videoCapturer, whipExchange, videoViewAdapter,peerFactory)
            whipPeer?.connect()
            whipPeer?.addVideoToView()
        }
    }

    private fun getVideoCapturer(context: Context) = Camera2Enumerator(context).run {
        createCapturer(deviceNames[1], null)
    }

    private fun subscribeSocketEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            socketExchange.events.collect { (eventName, data) ->
                // Handle the event here
                when (eventName) {
                    "offer" -> onSocketOffer(data)
                    "candidate" -> onSocketCandidate(data)
                }
            }
        }
    }

    private fun onSocketOffer(data: JSONObject) {
        logJson(data, "onSocketOffer")

    }
    private fun onSocketCandidate(data: JSONObject) {
        logJson(data, "onSocketCandidate")
    }

    private fun logJson(jsonObject: JSONObject, tag: String) {
        try {
            // Pretty print with indentation of 4 spaces
            val formattedJson = jsonObject.toString(4)
            android.util.Log.d(TAG, "$tag:\n$formattedJson")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error logging JSON: ${e.message}")
        }
    }

    companion object {
        private const val ARG_INPUT_SID = "sid"
        fun new(sid: String): BellFragment {
            val fragment = BellFragment()
            val args = Bundle()
            args.putString(ARG_INPUT_SID, sid)
            fragment.arguments = args
            return fragment
        }
    }
}