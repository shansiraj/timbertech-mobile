package com.shan.timbertech

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ResultActivity : ComponentActivity() {

    private lateinit var barChart: BarChart
    private lateinit var gradeTextView: TextView
    private lateinit var descriptionTextView: TextView

    // Example JSON data from backend
    private val response = mapOf(
        "final_grade" to "A",
        "price" to "233 LKR",
        "description" to "High-quality teak wood with minimal defects, suitable for premium furniture.",
        "grade_probability" to mapOf(
            "A" to 0.6f,
            "B" to 0.2f,
            "C" to 0.1f,
            "D" to 0.1f
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        barChart = findViewById(R.id.barChart)
        gradeTextView = findViewById(R.id.gradeTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)

        val backBtn: ImageButton = findViewById(R.id.backButton)

        backBtn.setOnClickListener {
            finish()
        }


        showResults()
    }

    private fun showResults() {
        val grade = response["final_grade"] as String
        val price = response["price"] as String
        val description = response["description"] as String
        val probs = response["grade_probability"] as Map<String, Float>

        gradeTextView.text = "Final Grade : $grade\nPrice : $price"
        descriptionTextView.text = description

        val labels = probs.keys.toList()
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), probs[label] ?: 0f)
        }

        val dataSet = BarDataSet(entries, "Grades")
        val barData = BarData(dataSet)
        dataSet.valueTextSize = 12f

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 0f

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        barChart.animateY(1000)

        barChart.invalidate()
    }
}
