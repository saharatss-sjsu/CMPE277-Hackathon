package com.example.cmpe277_hackathon.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cmpe277_hackathon.R
import com.example.cmpe277_hackathon.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.color.MaterialColors
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

val YearChoices: List<String> = listOf(
    "1960", "1961", "1962", "1963", "1964", "1965", "1966", "1967", "1968", "1969",
    "1970", "1971", "1972", "1973", "1974", "1975", "1976", "1977", "1978", "1979",
    "1980", "1981", "1982", "1983", "1984", "1985", "1986", "1987", "1988", "1989",
    "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999",
    "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009",
    "2010", "2011", "2012", "2013", "2014", "2015", "2016", "2017", "2018", "2019",
    "2020", "2021", "2022", "2023", "2024"
)

val CountryChoices: Map<String, String> = mapOf(
    "cn" to "China" ,
    "in" to "India",
    "usa" to "USA",
)

val IndicatorChoices: Map<String, String> = mapOf(
    "NY.GDP.MKTP.KD.ZG" to "GDP growth (annual %)" ,
    "NY.GDP.MKTP.CD" to "GDP (current US\$)",
    "BX.KLT.DINV.WD.GD.ZS" to "FDI, net inflows (% of GDP)",
    "BM.KLT.DINV.WD.GD.ZS" to "FDI, net outflows (% of GDP)",
)

class CustomSpinnerAdapter(context: Context, resource: Int, objects: List<String>, private val keys: List<String>) : ArrayAdapter<String>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.tag = keys[position]
        return view
    }
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        view.tag = keys[position]
        return view
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private fun fetchData(countryCode:String, indicators: MutableList<String>, yearStart:String, yearEnd:String){
        Log.d("main", "fetching $countryCode, $indicators, $yearStart-$yearEnd")

        CoroutineScope(Dispatchers.IO).launch {
            val dataEntries = mutableMapOf<String, MutableList<Entry>>()

            indicators.forEach { indicatorId ->
                var connection: HttpURLConnection? = null
                try {
                    val url =
                        URL("https://api.worldbank.org/v2/country/${countryCode}/indicator/${indicatorId}?format=json&date=${yearStart}:${yearEnd}")
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
                            val value = dataObject.optDouble("value").toFloat()
                            val indicatorDisplayStr = dataObject.getJSONObject("indicator").getString("value")
                            if (!value.isNaN()){
                                if (dataEntries[indicatorDisplayStr].isNullOrEmpty()) {
                                    dataEntries[indicatorDisplayStr] = mutableListOf()
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
                Log.d("main", "data-raw:  $dataEntries")
                val lineData = LineData()
                var index = 0
                dataEntries.forEach { (indicator, entries) ->
                    val lineDataSet = LineDataSet(
                        entries.sortedBy { it.x },
                        indicator).apply {
                            val color = IndicatorColorList[index]
                            setColor(color)
                            setCircleColor(color)
                            setDrawValues(false)
                            if (entries.first().y > 100000) {
                                axisDependency = YAxis.AxisDependency.RIGHT
                            }
                        }
                    lineData.addDataSet(lineDataSet)
                    index += 1
                }
                if (dataEntries.keys.isEmpty()){
                    binding.lineChart.clear()
                }else{
                    binding.lineChart.data = lineData
                    binding.lineChart.invalidate()
                }
//                binding.lineChart.resetZoom()
                binding.lineChart.description = Description().apply { text = "${CountryChoices[countryCode]}, $yearStart-$yearEnd" }
            }


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val lineChart = binding.lineChart
        lineChart.contentDescription = ""

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val spinnerCountry: Spinner = binding.spinnerCountry
        val spinnerYearStart: Spinner = binding.spinnerYearStart
        val spinnerYearEnd: Spinner = binding.spinnerYearEnd

        var selectingYearStart = YearChoices.first()
        var selectingYearEnd   = YearChoices.last()
        var selectingCountry   = CountryChoices.keys.first()
        val selectingIndicators = mutableListOf<String>()

        CustomSpinnerAdapter(requireContext(), android.R.layout.simple_spinner_item, CountryChoices.values.toList(), CountryChoices.keys.toList()).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCountry.adapter = adapter
        }

        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectingCountry = view?.tag.toString()
                fetchData(selectingCountry, selectingIndicators, selectingYearStart, selectingYearEnd)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {  }
        }

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, YearChoices).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerYearStart.adapter = adapter
            spinnerYearStart.setSelection(0)
        }
        spinnerYearStart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val newValue = parent.getItemAtPosition(position).toString()
                if (newValue.toInt() >= selectingYearEnd.toInt()){
                    spinnerYearStart.setSelection(0)
                    return
                }
                selectingYearStart = newValue
                fetchData(selectingCountry, selectingIndicators, selectingYearStart, selectingYearEnd)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {  }
        }

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, YearChoices).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerYearEnd.adapter = adapter
            spinnerYearEnd.setSelection(YearChoices.count()-1)
        }
        spinnerYearEnd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val newValue = parent.getItemAtPosition(position).toString()
                if (newValue.toInt() <= selectingYearStart.toInt()){
                    spinnerYearEnd.setSelection(YearChoices.size-1)
                    return
                }
                selectingYearEnd = newValue
                fetchData(selectingCountry, selectingIndicators, selectingYearStart, selectingYearEnd)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {  }
        }

        IndicatorChoices.forEach { indicatorCode, indicatorDisplay ->
            val checkBox = CheckBox(requireContext()).apply {
                text = indicatorDisplay
                buttonTintList = ColorStateList.valueOf(requireContext().getColor(R.color.purple_500))
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked){
                        selectingIndicators.add(indicatorCode)
                    }else{
                        selectingIndicators.remove(indicatorCode)
                    }
                    fetchData(selectingCountry, selectingIndicators, selectingYearStart, selectingYearEnd)
                }
            }
            binding.indicators.addView(checkBox)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}