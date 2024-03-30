package com.example.cmpe277_app3

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class CallOpenAIService : Service() {
    fun extractMessageContent(jsonString: String): String {
        val jsonObject = JSONObject(jsonString)
        val firstChoice = jsonObject.getJSONArray("choices").optJSONObject(0)
        val messageContent = firstChoice?.optJSONObject("message")?.optString("content")
        if (!messageContent.isNullOrBlank()) {
            return messageContent
        }
        return "Unknown"
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val httpURLConnection = url.openConnection() as HttpURLConnection

            val prompt = intent?.extras?.getString("prompt").toString()

            val additionalPrompt = "You are about to answer questions related to the following information: The summaries of the budget speech documents from 2017-18 to 2023-24 corresponding to your prompts are presented below. Each point refers to the themes and objectives outlined in the respective budget speeches, highlighting the Indian government's focus and initiatives over these years." +
                    "Economic and Social Sustainability Statements, Social Security, and Rural Development:" +
                    "2017-18: The government emphasized agricultural growth, targeted financial inclusion, infrastructure development, and support for the vulnerable sectors, aiming at a transformative shift towards a more transparent, objective decision-making, and formal economy ." +
                    "2018-19: Initiatives focused on improving rural livelihoods through extensive infrastructure development, enhancing farmers' incomes, and promoting rural employment. Significant attention was given to the health sector, education, and social welfare to support the underprivileged ." +
                    "2019-20: Prioritized water security and sanitation under the Jal Jeevan Mission, aimed at sustainable water supply. Continued emphasis on agricultural infrastructure, support for private entrepreneurship in agro-based industries, and rural development through various schemes ." +
                    "2020-21: Discussed the potential of GIFT city as a center for international finance and data processing. Announced the IPO of LIC as part of disinvestment strategies and stressed fiscal management and the efficient allocation of resources ." +
                    "2021-22: Highlighted infrastructure projects like highways and economic corridors, aimed at improving connectivity and supporting economic activities. Discussed policies for disinvestment and strategic investment to foster economic growth ." +
                    "2022-23: Focused on financial inclusion, infrastructure development, and support for rural and underprivileged communities through various government schemes. The budget aimed at economic empowerment of women, support for traditional artisans, and promotion of tourism ." +
                    "Farmers' Benefits, Economic Conditions, and Poverty Reduction Strategies" +
                    "Farmers' Benefits: Across years, the budgets have focused on increasing farmers' incomes, improving access to credit, enhancing agricultural productivity, and providing support for post-harvest infrastructure ." +
                    "Economic Conditions: The budgets have aimed at macroeconomic stability, fiscal consolidation, and leveraging technology for economic growth. Efforts to formalize the economy and improve governance were emphasized to create a more inclusive and sustainable economic environment ." +
                    "Poverty Reduction Strategies: The strategies include enhancing rural livelihoods, improving infrastructure and access to basic services, and targeting social welfare schemes towards the needy. Emphasis on education, health care, and social security underlines the approach to reducing poverty and improving the quality of life for the underprivileged ." +
                    "Benefits to Marginalized Communities, Women, Children, and Overall Economic Impact" +
                    "Benefits to Marginalized Communities: Initiatives to support SC/STs, women, and the disabled through targeted welfare schemes, financial inclusion measures, and infrastructure development in rural areas were highlighted. The focus has been on ensuring equitable growth and access to resources ." +
                    "Women and Children: Specific schemes aimed at the economic empowerment of women, health and nutrition programs for children, and educational initiatives to support youth and skill development reflect the government's commitment to empowering these vulnerable groups ." +
                    "Economic Impact: The budgets reflect a comprehensive approach to fostering sustainable economic growth, addressing socio-economic disparities, and ensuring environmental sustainability. Efforts to promote digital economy, transparency in governance, and fiscal prudence are aimed at strengthening the economic foundation and ensuring inclusive growth ." +
                    "These summaries capture the essence of the Indian government's budgetary priorities and initiatives over the years, focusing on sustainable economic and social development, poverty reduction, and support for the agriculture sector, rural development, and marginalized communities." +
                    "You are a Copilot for the achievement of UN SDGs. You will answer in short. The current prompt: "

            Log.d("openai", "prompt = $additionalPrompt $prompt")

            try {
                httpURLConnection.requestMethod = "POST"
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                httpURLConnection.setRequestProperty("Authorization", "Bearer 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")
                httpURLConnection.doOutput = true
                httpURLConnection.doInput = true

                val jsonInputString = """
                      {
                           "model": "gpt-3.5-turbo",
                            "messages": [
                                {
                                    "role": "user",
                                    "content": "$additionalPrompt $prompt"
                                }
                            ],
                            "temperature": 1,
                            "max_tokens": 256,
                            "top_p": 1,
                            "frequency_penalty": 0,
                            "presence_penalty": 0
                      }
                    """.trimIndent()

                httpURLConnection.outputStream.use { os ->
                    BufferedWriter(OutputStreamWriter(os, "UTF-8")).use { writer ->
                        writer.write(jsonInputString)
                        writer.flush()
                    }
                }

                val responseCode = httpURLConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = httpURLConnection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("openai", "Response: $response")
                    Log.d("openai", "extractMessageContent: ${extractMessageContent(response)}")
                    Intent("cmpe277.app3.UPDATE_ACTIVITY").also {
                        it.putExtra("response", extractMessageContent(response))
                        sendBroadcast(it)
                    }
                } else {
                    Log.d("openai", "Response: ERROR $responseCode")

                }
            } finally {
                httpURLConnection.disconnect()
            }
        }.start()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}