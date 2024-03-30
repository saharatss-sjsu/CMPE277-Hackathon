package com.example.cmpe277_hackathon.ui.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cmpe277_hackathon.databinding.FragmentChatgptBinding

class ChatgptFragment : Fragment() {

    private var _binding: FragmentChatgptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatgptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return  root
    }
}