package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.exchange.SocketExchange
import com.example.myapplication.exchange.WHEPExchange
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

class AdminFragment: Fragment() {
    val rootEglBase: EglBase = EglBase.create()
    private lateinit var whepExchange: WHEPExchange
    private lateinit var socketExchange: SocketExchange
    private lateinit var peerConnectionFactory:PeerConnectionFactory

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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.admin_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sid = arguments?.getString(AdminFragment.ARG_INPUT_SID)
        if (sid != null) {
            whepExchange = WHEPExchange(Config.liveBaseURI, sid)
            socketExchange = SocketExchange(sid)
            socketExchange.connect()
            subscribeSocketEvents()
        }
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

    private fun onSocketAnswer(data: JSONObject){}
    private fun onSocketCandidate(data: JSONObject){}
}