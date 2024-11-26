package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.exchange.SocketExchange
import com.example.myapplication.exchange.WHIPExchange
import kotlinx.coroutines.launch
import org.json.JSONObject

class BellFragment: Fragment() {
    private lateinit var whipExchange: WHIPExchange
    private lateinit var socketExchange: SocketExchange
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bell_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sid = arguments?.getString(ARG_INPUT_SID)
        if (sid != null) {
            whipExchange = WHIPExchange(Config.liveBaseURI, sid)
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
                    "offer" -> onSocketOffer(data)
                    "candidate" -> onSocketCandidate(data)
                }
            }
        }
    }

    private fun onSocketOffer(data: JSONObject){}
    private fun onSocketCandidate(data: JSONObject){}
}