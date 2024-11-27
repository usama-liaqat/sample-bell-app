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
import com.example.myapplication.webrtc.PeerConnectionObserver
import com.example.myapplication.webrtc.WHIPPeer
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.RtpReceiver
import org.webrtc.VideoCapturer
import org.webrtc.Camera2Enumerator


class BellFragment: Fragment() {
    private lateinit var videoViewAdapter: VideoViewAdapter
    private lateinit var recyclerView: RecyclerView

    private val videoItems = mutableListOf<VideoItem>()


    private lateinit var videoCapturer: VideoCapturer

    private lateinit var whipExchange: WHIPExchange
    private lateinit var whipPeer: WHIPPeer
    private lateinit var socketExchange: SocketExchange
    private lateinit var publishButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

        videoCapturer =  getVideoCapturer(activity)
        publishButton = view.findViewById(R.id.bellPublishButton)

        val sid = arguments?.getString(ARG_INPUT_SID)
        if (sid != null) {
            connection(sid, activity, context)
        }
    }

    private fun connection(sid: String, activity:Activity, context:Context){
        whipExchange = WHIPExchange(Config.liveBaseURI, sid)
        socketExchange = SocketExchange(sid)
        socketExchange.connect()
        subscribeSocketEvents()
        publishButton.setOnClickListener {
            publish(activity, context)
        }
    }

    private fun publish(activity:Activity, context:Context){
        whipPeer = WHIPPeer(activity,videoCapturer, whipExchange, object: PeerConnectionObserver() {
            override fun onAddTrack(
                receiver: RtpReceiver?,
                mediaStreams: Array<out MediaStream>?
            ) {
                TODO("Not yet implemented")
            }

            override fun onRemoveStream(mediaStream: MediaStream?) {
                TODO("Not yet implemented")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                if(iceCandidate !== null){
                    whipPeer.addIceCandidate(iceCandidate)
                }
            }
        })
        whipPeer.startCapture()
        whipPeer.initPeerConnection()
        whipPeer.connect()
        val videoItem = whipPeer.getVideoItem()
        if(videoItem!== null) {
            videoViewAdapter.addItem(videoItem)
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

    private fun onSocketOffer(data: JSONObject){}
    private fun onSocketCandidate(data: JSONObject){}


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