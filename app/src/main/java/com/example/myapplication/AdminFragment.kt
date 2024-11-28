package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.exchange.SocketExchange
import com.example.myapplication.exchange.WHEPExchange
import com.example.myapplication.webrtc.Peer
import com.example.myapplication.webrtc.PeerFactory
import com.example.myapplication.webrtc.WHEPPeer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer

class AdminFragment : Fragment() {
    private val TAG = "AdminFragment"


    private lateinit var videoViewAdapter: VideoViewAdapter
    private lateinit var recyclerView: RecyclerView

    private val videoItems = mutableListOf<VideoItem>()

    private var whepExchange: WHEPExchange? = null
    private var whepPeer: WHEPPeer? = null

    private lateinit var socketExchange: SocketExchange

    private lateinit var callButton: Button
    private lateinit var callInput: EditText

    private lateinit var videoCapturer: VideoCapturer


    private lateinit var peerFactory: PeerFactory
    private val peers = mutableMapOf<String, Peer>()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.admin_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val context = requireContext()



        recyclerView = view.findViewById(R.id.recyclerView)
        videoViewAdapter = VideoViewAdapter(videoItems)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = videoViewAdapter
        videoCapturer = getVideoCapturer(activity)


        peerFactory = PeerFactory(activity)

        callInput = view.findViewById(R.id.callInput)
        callButton = view.findViewById(R.id.callButton)

        val sid = arguments?.getString(ARG_INPUT_SID)
        if (sid != null) {
            connection(sid)
        }
        callButton.setOnClickListener {

            val inputText = callInput.text.toString()

            if (inputText.isEmpty()) {
                callInput.error = "Input cannot be empty" // Show an error message
                return@setOnClickListener // Stop the method execution if input is empty
            }
            connectBell(inputText, activity)
            connectPeer(inputText, activity)
        }
    }

    private fun connectBell(bellId: String, activity: Activity) {
        Log.e(TAG, "Joining: $bellId")

        if (whepPeer !== null) {
            whepPeer!!.close()
        }

        whepExchange = WHEPExchange(Config.LIVE_BASE_URL, bellId)
        whepPeer = WHEPPeer(activity, videoViewAdapter, whepExchange!!, peerFactory)
        whepPeer!!.connect()
    }

    private fun connectPeer(bellId: String, activity: Activity) {
        peers[bellId].let { it?.close() }
        val peer = Peer(bellId,true,activity,videoCapturer, peerFactory, socketExchange, videoViewAdapter)
        peer.createOffer()
        peers[bellId] = peer
    }

    private fun connection(sid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            peerFactory.getIceServers(sid)
        }
        socketExchange = SocketExchange(sid)
        socketExchange.connect()
        subscribeSocketEvents()
    }


    private fun subscribeSocketEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            socketExchange.events.collect { (eventName, data) ->
                // Handle the event here
                when (eventName) {
                    "answer" -> onSocketAnswer(data)
                    "candidate" -> onSocketCandidate(data)
                }
            }
        }
    }

    private fun onSocketAnswer(data: JSONObject) {
        val sdp = data.getString("sdp")
        val from = data.getString("from")
        val answer  = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peers[from].let { it?.addRemoteAnswer(answer) }
    }

    private fun onSocketCandidate(data: JSONObject) {
        val from = data.getString("from")
        val candidate = data.getString("candidate")
        val sdpMLineIndex = data.getInt("sdpMLineIndex")
        val sdpMid = data.getString("sdpMid")

        val iceCandidate = IceCandidate(
            sdpMid,
            sdpMLineIndex,
            candidate
        )
        peers[from].let { it?.addIceCandidate(iceCandidate) }
    }
    private fun getVideoCapturer(context: Context) = Camera2Enumerator(context).run {
        createCapturer(deviceNames[1], null)
    }


    companion object {
        private const val ARG_INPUT_SID = "sid"
        fun new(sid: String): AdminFragment {
            val fragment = AdminFragment()
            val args = Bundle()
            args.putString(ARG_INPUT_SID, sid)
            fragment.arguments = args
            return fragment
        }
    }
}