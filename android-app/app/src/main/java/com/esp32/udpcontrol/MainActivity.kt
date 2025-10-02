package com.esp32.udpcontrol

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.esp32.udpcontrol.databinding.ActivityMainBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var udpClient: UdpClient
    
    // Datos para gráficos
    private val temperatureEntries = mutableListOf<Entry>()
    private val humidityEntries = mutableListOf<Entry>()
    private val lightEntries = mutableListOf<Entry>()
    private var entryIndex = 0f
    
    // Estados de LEDs
    private var ledStates = booleanArrayOf(false, false, false, false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar ESP32 IP y puertos
        val esp32IP = "10.175.23.159"
        val esp32Port = 4210
        val localPort = 4211
        
        // Inicializar cliente UDP
        udpClient = UdpClient(esp32IP, esp32Port, localPort) { sensorData ->
            runOnUiThread {
                updateUI(sensorData)
            }
        }
        
        setupUI()
        setupChart()
        startUdpClient()
    }
    
    private fun setupUI() {
        // Configurar botones de LEDs
        binding.btnLed1.setOnClickListener { toggleLED(1) }
        binding.btnLed2.setOnClickListener { toggleLED(2) }
        binding.btnLed3.setOnClickListener { toggleLED(3) }
        binding.btnLed4.setOnClickListener { toggleLED(4) }
        binding.btnAllOff.setOnClickListener { allLEDsOff() }
        
        // Configurar switch de conexión
        binding.switchConnection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startUdpClient()
            } else {
                stopUdpClient()
            }
        }
        
        // Botón de actualizar datos
        binding.btnRefresh.setOnClickListener {
            lifecycleScope.launch {
                udpClient.requestData()
            }
        }
        
        // Actualizar estado inicial
        updateConnectionStatus(false)
    }
    
    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true
            
            xAxis.apply {
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }
            
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }
        }
    }
    
    private fun toggleLED(ledNumber: Int) {
        lifecycleScope.launch {
            val command = ledNumber.toString()
            udpClient.sendCommand(command)
            
            // Actualizar estado local (se confirmará con datos del sensor)
            ledStates[ledNumber - 1] = !ledStates[ledNumber - 1]
            updateLEDButtons()
            
            showToast("Comando LED $ledNumber enviado")
        }
    }
    
    private fun allLEDsOff() {
        lifecycleScope.launch {
            udpClient.sendCommand("0")
            
            // Actualizar estados locales
            ledStates.fill(false)
            updateLEDButtons()
            
            showToast("Todos los LEDs apagados")
        }
    }
    
    private fun updateUI(sensorData: SensorData) {
        // Actualizar valores de sensores
        binding.txtTemperature.text = "${sensorData.temperature}°C"
        binding.txtHumidity.text = "${sensorData.humidity}%"
        binding.txtLight.text = "${sensorData.light}%"
        
        // Actualizar estados de LEDs
        ledStates[0] = sensorData.led1
        ledStates[1] = sensorData.led2
        ledStates[2] = sensorData.led3
        ledStates[3] = sensorData.led4
        updateLEDButtons()
        
        // Actualizar indicadores de estado
        binding.txtDhtStatus.text = if (sensorData.errorDHT) "❌ Error" else "✅ OK"
        binding.txtWifiStatus.text = if (sensorData.wifiOK) "🟢 Conectado" else "🔴 Desconectado"
        
        // Actualizar timestamp
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        binding.txtLastUpdate.text = "Última actualización: $timestamp"
        
        // Actualizar gráfico
        updateChart(sensorData)
    }
    
    private fun updateLEDButtons() {
        val buttons = arrayOf(binding.btnLed1, binding.btnLed2, binding.btnLed3, binding.btnLed4)
        val colors = arrayOf(
            android.R.color.holo_red_light,
            android.R.color.holo_green_light,
            android.R.color.holo_blue_light,
            android.R.color.holo_orange_light
        )
        
        buttons.forEachIndexed { index, button ->
            if (ledStates[index]) {
                button.setBackgroundColor(getColor(colors[index]))
                button.text = "LED ${index + 1} ON"
            } else {
                button.setBackgroundColor(getColor(android.R.color.darker_gray))
                button.text = "LED ${index + 1} OFF"
            }
        }
    }
    
    private fun updateChart(sensorData: SensorData) {
        // Añadir nuevos puntos de datos
        temperatureEntries.add(Entry(entryIndex, sensorData.temperature))
        humidityEntries.add(Entry(entryIndex, sensorData.humidity))
        lightEntries.add(Entry(entryIndex, sensorData.light.toFloat()))
        
        // Mantener solo los últimos 50 puntos
        if (temperatureEntries.size > 50) {
            temperatureEntries.removeAt(0)
            humidityEntries.removeAt(0)
            lightEntries.removeAt(0)
        }
        
        entryIndex++
        
        // Crear datasets
        val tempDataSet = LineDataSet(temperatureEntries, "Temperatura (°C)").apply {
            color = getColor(android.R.color.holo_red_light)
            setCircleColor(getColor(android.R.color.holo_red_light))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 0f
        }
        
        val humDataSet = LineDataSet(humidityEntries, "Humedad (%)").apply {
            color = getColor(android.R.color.holo_blue_light)
            setCircleColor(getColor(android.R.color.holo_blue_light))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 0f
        }
        
        val lightDataSet = LineDataSet(lightEntries, "Luz (%)").apply {
            color = getColor(android.R.color.holo_orange_light)
            setCircleColor(getColor(android.R.color.holo_orange_light))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 0f
        }
        
        // Actualizar gráfico
        val lineData = LineData(tempDataSet, humDataSet, lightDataSet)
        binding.chart.data = lineData
        binding.chart.notifyDataSetChanged()
        binding.chart.invalidate()
    }
    
    private fun startUdpClient() {
        lifecycleScope.launch {
            try {
                udpClient.start()
                updateConnectionStatus(true)
                showToast("Conectado al ESP32")
            } catch (e: Exception) {
                updateConnectionStatus(false)
                showToast("Error de conexión: ${e.message}")
            }
        }
    }
    
    private fun stopUdpClient() {
        udpClient.stop()
        updateConnectionStatus(false)
        showToast("Desconectado del ESP32")
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        binding.txtConnectionStatus.text = if (connected) {
            "🟢 Conectado a ESP32"
        } else {
            "🔴 Desconectado"
        }
        
        binding.switchConnection.isChecked = connected
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        udpClient.stop()
    }
}