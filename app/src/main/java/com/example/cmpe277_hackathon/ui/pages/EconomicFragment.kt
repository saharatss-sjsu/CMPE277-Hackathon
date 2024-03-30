package com.example.cmpe277_hackathon.ui.pages

import AnnotationTableRowView
import OnTextDialogListener
import TextDialogFragment
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.cmpe277_hackathon.annotationrecord.AnnotationRecord
import com.example.cmpe277_hackathon.R
import com.example.cmpe277_hackathon.ui.CustomSpinnerAdapter
import com.example.cmpe277_hackathon.ui.LoginFragment
import com.example.cmpe277_hackathon.ui.SharedLoginRepository
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import kotlin.random.Random

import com.example.cmpe277_hackathon.annotationrecord.AnnotationDbHelper as AnnotationDbHelper
import com.example.cmpe277_hackathon.databinding.FragmentEconomicBinding as FragmentBinding

private fun randomColor(): Int {
    val random = Random.Default
    val red = random.nextInt(256)
    val green = random.nextInt(256)
    val blue = random.nextInt(256)
    return Color.rgb(red, green, blue)
}

private val YearChoices: List<String> = listOf(
    "1960", "1961", "1962", "1963", "1964", "1965", "1966", "1967", "1968", "1969",
    "1970", "1971", "1972", "1973", "1974", "1975", "1976", "1977", "1978", "1979",
    "1980", "1981", "1982", "1983", "1984", "1985", "1986", "1987", "1988", "1989",
    "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999",
    "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009",
    "2010", "2011", "2012", "2013", "2014", "2015", "2016", "2017", "2018", "2019",
    "2020", "2021", "2022", "2023", "2024"
)

private val CountryChoices: Map<String, String> = mapOf(
    "cn" to "China" ,
    "in" to "India",
    "usa" to "USA",
)

private val IndicatorChoices: Map<String, String> = mapOf(
    "NY.GDP.MKTP.KD.ZG" to "GDP growth (annual %)" ,
    "NY.GDP.MKTP.CD" to "GDP (current US\$)",
    "BX.KLT.DINV.WD.GD.ZS" to "FDI, net inflows (% of GDP)",
    "BM.KLT.DINV.WD.GD.ZS" to "FDI, net outflows (% of GDP)",
)

class EconomicFragment : Fragment(), OnChartValueSelectedListener, OnChartGestureListener, OnTextDialogListener {

    private var _binding: FragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var indicatorColorList = mutableListOf<Int>()

    private var selectingYearStart = YearChoices.first()
    private var selectingYearEnd   = YearChoices.last()
    private var selectingCountry   = CountryChoices.keys.first()
    private val selectingIndicators = mutableListOf<String>(IndicatorChoices.keys.first())

    private var editingAnnotation: AnnotationRecord? = null
    private var annotations = mutableListOf<AnnotationRecord>()

    @SuppressLint("SetTextI18n")
    private fun fetchData(countryCode:String, indicators: MutableList<String>, yearStart:String, yearEnd:String){
        Log.d("main", "fetching $countryCode, $indicators, $yearStart-$yearEnd")

        binding.textSelecting.apply {
            visibility = View.VISIBLE
            setTextColor(Color.GRAY)
            text = "Loading..."
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val dataEntries = mutableMapOf<String, MutableList<Entry>>()
                indicators.forEach { indicatorId ->
                    var connection: HttpURLConnection? = null
                    val url = URL("https://api.worldbank.org/v2/country/${countryCode}/indicator/${indicatorId}?format=json&date=${yearStart}:${yearEnd}")
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
                            val indicatorDisplayStr = dataObject.getJSONObject("indicator").getString("id")
                            if (!value.isNaN()) {
                                if (dataEntries[indicatorDisplayStr].isNullOrEmpty()) {
                                    dataEntries[indicatorDisplayStr] = mutableListOf()
                                }
                                dataEntries[indicatorDisplayStr]?.add(Entry(date, value))
                            }
                        }
                    }
                    connection.disconnect()
                }

                withContext(Dispatchers.Main) {
                    Log.d("main", "data-raw:  $dataEntries")
                    val lineData = LineData()
                    dataEntries.forEach { (indicatorCode, entries) ->
                        val lineDataSet = LineDataSet(
                            entries.sortedBy { it.x },
                            IndicatorChoices[indicatorCode]
                        ).apply {
                            val color = indicatorColorList[IndicatorChoices.keys.indexOf(indicatorCode)]
                            setColor(color)
                            setCircleColor(color)
                            setDrawValues(false)
                            lineWidth = 2f
                            if (entries.first().y > 100000) {
                                axisDependency = YAxis.AxisDependency.RIGHT
                            }
                        }
                        lineData.addDataSet(lineDataSet)
                    }
                    if (dataEntries.keys.isEmpty()) {
                        binding.lineChart.clear()
                    } else {
                        binding.lineChart.data = lineData
                        binding.lineChart.invalidate()
                    }
                    binding.lineChart.description = Description().apply {
                        text = "${CountryChoices[countryCode]}, $yearStart-$yearEnd"
                    }
                    binding.textSelecting.visibility = View.GONE
                    binding.buttonAnnotation.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("main", "fetch: $e")
            binding.textSelecting.apply {
                visibility = View.VISIBLE
                setTextColor(Color.GRAY)
                text = "Load Failed"
            }
        }
    }

    private fun databaseLoad() {
//        requireContext().deleteDatabase("AnnotationDb.db")
        val dbHelper = AnnotationDbHelper(requireContext())
        annotations = dbHelper.readAllRecords().filter { annotationRecord -> IndicatorChoices.containsKey(annotationRecord.indicator) }.toMutableList()
        Log.d("main", "SQLite read: $annotations")
        val tableAnnotation = binding.tableAnnotations
        tableAnnotation.removeAllViews()
        annotations.forEach {annotationRecord ->
            tableAnnotation.addView(AnnotationTableRowView(requireContext()).apply {
                indicatorChoices = IndicatorChoices
                countryChoices = CountryChoices
                annotation = annotationRecord
                onDelete = {
                    dbHelper.deleteRecord(annotationRecord)
                    databaseLoad()
                }
            })
        }
        if(annotations.isEmpty()){
            binding.textNoAnnotation.visibility = View.VISIBLE
        }else{
            binding.textNoAnnotation.visibility = View.GONE
        }
    }
    private fun databaseSave(annotationRecord: AnnotationRecord) {
        AnnotationDbHelper(requireContext()).writeRecord(annotationRecord)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        indicatorColorList = mutableListOf<Int>().apply { repeat(20) { add(randomColor()) }}

        val lineChart = binding.lineChart
        lineChart.contentDescription = ""
        lineChart.setOnChartValueSelectedListener(this)
        lineChart.onChartGestureListener = this
        lineChart.setNoDataTextColor(requireContext().getColor(R.color.purple_500))

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val spinnerCountry: Spinner = binding.spinnerCountry
        val spinnerYearStart: Spinner = binding.spinnerYearStart
        val spinnerYearEnd: Spinner = binding.spinnerYearEnd

        binding.textSelecting.visibility = View.GONE

        binding.buttonZoomReset.apply {
            visibility = View.GONE
            setOnClickListener {
                lineChart.fitScreen()
                visibility = View.GONE
            }
        }

        binding.buttonAnnotation.apply {
            visibility = View.GONE
            setOnClickListener {
                val dialogFragment = TextDialogFragment("Annotation")
                dialogFragment.textDialogListener = this@EconomicFragment
                dialogFragment.show(childFragmentManager, "annotation_dialog")
            }
        }

        CustomSpinnerAdapter(requireContext(), android.R.layout.simple_spinner_item, CountryChoices.values.toList(), CountryChoices.keys.toList()).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCountry.adapter = adapter
        }
        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (view != null) selectingCountry = view.tag.toString()
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

        IndicatorChoices.entries.forEachIndexed { index, indicatorEntry ->
            val checkBox = CheckBox(requireContext()).apply {
                text = indicatorEntry.value
                buttonTintList = ColorStateList.valueOf(indicatorColorList[index])
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked){
                        selectingIndicators.add(indicatorEntry.key)
                    }else{
                        selectingIndicators.removeAll(listOf(indicatorEntry.key))
                    }
                    fetchData(selectingCountry, selectingIndicators, selectingYearStart, selectingYearEnd)
                }
            }
            checkBox.isChecked = selectingIndicators.contains(indicatorEntry.key)
            binding.indicators.addView(checkBox)
        }

        databaseLoad()

//        val transaction = requireActivity().supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.navigation_economic, LoginFragment())
//        transaction.commit()

        if(SharedLoginRepository.isResearcher == null) showLoginDialog()
        Log.d("main", "Login: isResearcher = ${SharedLoginRepository.isResearcher}")
        if(SharedLoginRepository.isResearcher == false) binding.cardAnnotations.visibility = View.GONE

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    override fun onValueSelected(dataEntry: Entry?, h: Highlight?) {
        Log.d("main", "$dataEntry $h")
        if(dataEntry == null || h == null) return
        val indicatorDisplay = binding.lineChart.data.dataSets[h.dataSetIndex].label.toString()
        val indicatorCode    = IndicatorChoices.keys.toList()[IndicatorChoices.values.indexOf(indicatorDisplay)]
        binding.textSelecting.apply {
            visibility = View.VISIBLE
            setTextColor(indicatorColorList[IndicatorChoices.values.indexOf(indicatorDisplay)])
            text = "${dataEntry.x.toInt()} $indicatorDisplay: ${DecimalFormat("#,##0.00").format(dataEntry.y)}"
        }
        if(SharedLoginRepository.isResearcher == true) binding.buttonAnnotation.visibility = View.VISIBLE
        editingAnnotation = AnnotationRecord(country = selectingCountry, year = dataEntry.x.toInt().toString(), indicator = indicatorCode, content = "")
    }

    @SuppressLint("SetTextI18n")
    override fun onNothingSelected() {
        Log.d("main", "No Selection")
        binding.textSelecting.visibility = View.GONE
    }

    override fun onTextDialogDataReceived(textData: String) {
        val annotation = editingAnnotation!!
        Log.d("main", "onTextDialogDataReceived ${annotation.year} ${annotation.indicator}: ${textData}")
        if(editingAnnotation == null) return
        annotation.content = textData
        databaseSave(annotation)
        databaseLoad()
    }

    private fun showLoginDialog() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("Welcome to Macroeconomic Food Security App")
        builder.setMessage("Please identify yourself?")

        builder.setNegativeButton("Government") { _, _ ->
            SharedLoginRepository.isResearcher = false
            Log.d("main", "Login: isResearcher = ${SharedLoginRepository.isResearcher}")
            if(SharedLoginRepository.isResearcher == false) binding.cardAnnotations.visibility = View.GONE
        }
        builder.setPositiveButton("Researcher") { _, _ ->
            SharedLoginRepository.isResearcher = true
            Log.d("main", "Login: isResearcher = ${SharedLoginRepository.isResearcher}")
            if(SharedLoginRepository.isResearcher == false) binding.cardAnnotations.visibility = View.GONE
        }

        val dialog = builder.create()
        dialog.show()
    }


    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
    override fun onChartLongPressed(me: MotionEvent?) { }
    override fun onChartDoubleTapped(me: MotionEvent?) {
        binding.buttonZoomReset.visibility = View.VISIBLE
    }
    override fun onChartSingleTapped(me: MotionEvent?) { }
    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) { }

}