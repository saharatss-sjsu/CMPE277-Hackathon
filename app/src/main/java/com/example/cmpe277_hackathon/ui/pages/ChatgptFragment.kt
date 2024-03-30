package com.example.cmpe277_hackathon.ui.pages

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import com.example.cmpe277_app3.CallOpenAIService
import com.example.cmpe277_hackathon.databinding.FragmentChatgptBinding
import org.json.JSONObject
import org.w3c.dom.Text

class ChatgptFragment : Fragment() {

    private var _binding: FragmentChatgptBinding? = null

    private val binding get() = _binding!!

    private val messages = mutableListOf<String>()

    private fun extractMessageContent(jsonString: String): String {
        val jsonObject = JSONObject(jsonString)
        val firstChoice = jsonObject.getJSONArray("choices").optJSONObject(0)
        val messageContent = firstChoice?.optJSONObject("message")?.optString("content")
        if (!messageContent.isNullOrBlank()) {
            return messageContent
        }
        return "Unknown"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatgptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textInput = binding.textInput

        val context = requireContext()

        binding.buttonSend.apply {
            setOnClickListener {
                val message = textInput.text.toString()
                textInput.setText("")
                messages.add("Me: $message")
                val intent = Intent(context, CallOpenAIService::class.java)
                intent.putExtra("prompt", message)
                context.startService(intent)
                updateListView()

                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
            }
        }

        val filter = IntentFilter("cmpe277.app3.UPDATE_ACTIVITY")
        context.registerReceiver(updateReceiver, filter, RECEIVER_EXPORTED)

        updateListView()

        return  root
    }

    private fun updateListView(){
        val listView = binding.listView
        listView.removeAllViews()

        messages.forEach { message ->
            listView.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0,0,0,32)
                }
                setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Medium)
                text = message
            })
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("response")
            Log.d("main", "OpenAI: response = $message")
            messages.add("AI: $message")
            updateListView()
        }
    }
}