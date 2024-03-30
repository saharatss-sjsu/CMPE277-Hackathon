package com.example.cmpe277_hackathon.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cmpe277_hackathon.databinding.FragmentHomeBinding
import com.google.android.gms.fitness.data.DataPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textView
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val lineChart = binding.lineChart

        val entries = ArrayList<Entry>().apply {
            add(Entry(1f, 2f))
            add(Entry(2f, 3f))
            add(Entry(3f, 5f))
        }

        val dataSet = LineDataSet(entries, "Label")

        val lineData = LineData(dataSet)
        lineChart.data = lineData


        // Refresh the chart
        lineChart.invalidate()

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Options: BOTTOM, TOP, BOTH_SIDED, TOP_INSIDE, BOTTOM_INSIDE

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}