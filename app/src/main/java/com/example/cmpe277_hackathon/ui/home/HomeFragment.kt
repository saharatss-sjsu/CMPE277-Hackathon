package com.example.cmpe277_hackathon.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cmpe277_hackathon.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

private fun randomColor(): Int {
    val random = Random.Default
    val red = random.nextInt(256)
    val green = random.nextInt(256)
    val blue = random.nextInt(256)
    return android.graphics.Color.rgb(red, green, blue)
}

private val IndicatorColorList = mutableListOf<Int>().apply { repeat(3) {
    add(randomColor())
}}
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private fun fetchData(country:String, indicators: MutableList<String>){
        CoroutineScope(Dispatchers.IO).launch {
            var dataEntries = mutableMapOf<String, MutableList<Entry>>()

            indicators.forEach { indicatorId ->
                var connection: HttpURLConnection? = null
                try {
                    val url =
                        URL("https://api.worldbank.org/v2/country/${country}/indicator/${indicatorId}?format=json")
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                        val response = bufferedReader.readText()
                        bufferedReader.close()

                        val jsonArray = JSONArray(response)
                        val dataJsonArray = jsonArray.getJSONArray(1)
                        for (i in 0 until dataJsonArray.length()) {
                            val dataObject = dataJsonArray.getJSONObject(i)
                            val date = dataObject.getString("date").toFloatOrNull() ?: continue
                            var value = dataObject.optDouble("value").toFloat()
                            val indicatorDisplayStr = dataObject.getJSONObject("indicator").getString("value")
                            if (!value.isNaN()){
                                if (dataEntries[indicatorDisplayStr].isNullOrEmpty()) {
                                    dataEntries[indicatorDisplayStr] = mutableListOf<Entry>()
                                }
                                dataEntries[indicatorDisplayStr]?.add(Entry(date, value))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("main", e.toString())
                    e.printStackTrace()
                } finally {
                    Log.d("main", "disconnect")
                    connection?.disconnect()
                }
            }

            withContext(Dispatchers.Main) {
                Log.d("main", "data-raw:  ${dataEntries.toString()}")
                val linedata = LineData()
                var index = 0
                dataEntries.forEach { indicator, entries ->
                    val lineDataSet = LineDataSet(
                        entries.sortedBy { it.x },
                        indicator).apply {
                            val _color = IndicatorColorList[index]
                            color = _color
                            setCircleColor(_color)
                            setDrawValues(false)
                        }
                    linedata.addDataSet(lineDataSet)
                    index += 1
                }
                if (dataEntries.keys.count() == 0){
                    binding.lineChart.clear()
                }else{
                    binding.lineChart.data = linedata
                    binding.lineChart.invalidate()
                }
            }


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val lineChart = binding.lineChart
        lineChart.contentDescription = ""

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val spinner: Spinner = binding.spinnerCountry

        val countryChoices: Map<String, String> = mapOf(
            "China" to "cn",
            "India" to "in",
            "USA" to "usa",
        )

        var selectingCountry = "cn"
        val selectingIndicators = mutableListOf<String>("NY.GDP.MKTP.KD.ZG")

        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            countryChoices.keys.toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val item = parent.getItemAtPosition(position).toString()
                selectingCountry = countryChoices[item].toString()
                fetchData(selectingCountry, selectingIndicators)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        val checkBoxes = listOf(
            binding.indicator1,
            binding.indicator2,
        )
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val indicator = checkBox.tag.toString()
                if (isChecked){
                    selectingIndicators.add(indicator)
                }else{
                    selectingIndicators.remove(indicator)
                }
                fetchData(selectingCountry, selectingIndicators)
            }
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}