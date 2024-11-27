package com.example.myapplication

import android.app.Activity
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
import com.example.myapplication.webrtc.WHEPPeer
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.PeerConnectionFactory

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

    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory by lazy { buildPeerConnectionFactory() }


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
        }
    }

    private fun connectBell(bellId: String, activity: Activity) {
        Log.e(TAG, "Joining: $bellId")
        if (whepPeer !== null) {
            whepPeer!!.close()
        }

        whepExchange = WHEPExchange(Config.LIVE_BASE_URL, bellId)
        whepPeer = WHEPPeer(activity, videoViewAdapter, whepExchange!!, peerConnectionFactory)
        whepPeer!!.connect()
    }

    private fun connection(sid: String) {
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

    private fun onSocketAnswer(data: JSONObject) {}
    private fun onSocketCandidate(data: JSONObject) {}

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        initPeerConnectionFactory()
        val videoEncoderFactory = HardwareVideoEncoderFactory(
            rootEglBase.eglBaseContext, true, true
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        return PeerConnectionFactory.builder().setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val activity = requireActivity()
        val options = PeerConnectionFactory.InitializationOptions.builder(activity.application)
            .setEnableInternalTracer(true).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
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