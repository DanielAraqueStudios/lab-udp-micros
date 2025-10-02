package com.esp32.udpcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel principal para la aplicación ESP32 UDP Controller
 * Implementa patrón MVVM y maneja toda la lógica de negocio
 */
class ESP32ViewModel : ViewModel() {
    
    private val stateManager = ESP32StateManager()
    private var udpClient: UdpClient? = null
    
    // Estados observables
    val esp32State: StateFlow<ESP32State> = stateManager.state
    val sensorHistory: StateFlow<List<SensorData>> = stateManager.sensorHistory
    
    // Estado de la UI
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<LedCommand?>(null)
    val lastCommand: StateFlow<LedCommand?> = _lastCommand.asStateFlow()
    
    init {
        // Configuración inicial
        stateManager.updateNetworkConfig(NetworkConfig())
    }
    
    /**
     * Conecta al ESP32
     */
    fun connect() {
        if (_isConnecting.value) return
        
        viewModelScope.launch {
            _isConnecting.value = true
            stateManager.updateConnectionState(ConnectionState.CONNECTING)
            stateManager.clearError()
            
            try {
                // Crear cliente UDP
                udpClient = UdpClientFactory.create(
                    esp32State.value.networkConfig,
                    viewModelScope
                )
                
                // Configurar listeners
                setupUdpListeners()
                
                // Iniciar conexión
                val success = udpClient?.start() ?: false
                
                if (success) {
                    stateManager.updateConnectionState(ConnectionState.CONNECTED)
                    
                    // Enviar ping inicial
                    udpClient?.ping()
                    
                    // Iniciar monitoreo de conexión
                    startConnectionMonitoring()
                } else {
                    stateManager.setError("No se pudo establecer conexión UDP")
                }
                
            } catch (e: Exception) {
                stateManager.setError("Error de conexión: ${e.message}")
            } finally {
                _isConnecting.value = false
            }
        }
    }
    
    /**
     * Desconecta del ESP32
     */
    fun disconnect() {
        viewModelScope.launch {
            udpClient?.stop()
            udpClient = null
            stateManager.updateConnectionState(ConnectionState.DISCONNECTED)
        }
    }
    
    /**
     * Envía comando para controlar LEDs
     */
    fun sendLedCommand(led1: Boolean, led2: Boolean, led3: Boolean, led4: Boolean) {
        val command = LedCommand(led1, led2, led3, led4)
        
        viewModelScope.launch {
            try {
                val success = udpClient?.sendLedCommand(command) ?: false
                if (success) {
                    _lastCommand.value = command
                } else {
                    stateManager.setError("Error enviando comando LED")
                }
            } catch (e: Exception) {
                stateManager.setError("Error enviando comando: ${e.message}")
            }
        }
    }
    
    /**
     * Controla un LED individual
     */
    fun toggleLed(ledNumber: Int) {
        val currentData = esp32State.value.lastSensorData
        if (currentData != null) {
            when (ledNumber) {
                1 -> sendLedCommand(!currentData.led1, currentData.led2, currentData.led3, currentData.led4)
                2 -> sendLedCommand(currentData.led1, !currentData.led2, currentData.led3, currentData.led4)
                3 -> sendLedCommand(currentData.led1, currentData.led2, !currentData.led3, currentData.led4)
                4 -> sendLedCommand(currentData.led1, currentData.led2, currentData.led3, !currentData.led4)
            }
        } else {
            // Si no hay datos, encender solo el LED seleccionado
            sendLedCommand(
                ledNumber == 1,
                ledNumber == 2,
                ledNumber == 3,
                ledNumber == 4
            )
        }
    }
    
    /**
     * Apaga todos los LEDs
     */
    fun turnOffAllLeds() {
        sendLedCommand(false, false, false, false)
    }
    
    /**
     * Enciende todos los LEDs
     */
    fun turnOnAllLeds() {
        sendLedCommand(true, true, true, true)
    }
    
    /**
     * Actualiza la configuración de red
     */
    fun updateNetworkConfig(newConfig: NetworkConfig) {
        stateManager.updateNetworkConfig(newConfig)
    }
    
    /**
     * Limpia errores
     */
    fun clearError() {
        stateManager.clearError()
    }
    
    /**
     * Envía ping al ESP32
     */
    fun ping() {
        viewModelScope.launch {
            udpClient?.ping()
        }
    }
    
    /**
     * Configura los listeners para el cliente UDP
     */
    private fun setupUdpListeners() {
        udpClient?.let { client ->
            // Listener para datos recibidos
            viewModelScope.launch {
                client.receivedData.collect { sensorData ->
                    stateManager.updateSensorData(sensorData)
                }
            }
            
            // Listener para errores
            viewModelScope.launch {
                client.connectionErrors.collect { error ->
                    stateManager.setError(error)
                }
            }
        }
    }
    
    /**
     * Inicia el monitoreo de conexión
     */
    private fun startConnectionMonitoring() {
        viewModelScope.launch {
            while (esp32State.value.connectionState == ConnectionState.CONNECTED) {
                delay(5000) // Verificar cada 5 segundos
                
                if (udpClient?.isConnected() != true) {
                    stateManager.setError("Conexión perdida")
                    break
                }
            }
        }
    }
    
    /**
     * Obtiene estadísticas de los sensores
     */
    fun getSensorStats(): SensorStats? {
        val history = sensorHistory.value
        if (history.isEmpty()) return null
        
        return SensorStats(
            avgTemperature = history.map { it.temperature }.average().toFloat(),
            avgHumidity = history.map { it.humidity }.average().toFloat(),
            avgLight = history.map { it.light }.average().toInt(),
            dataPoints = history.size,
            timeSpan = if (history.size > 1) {
                history.last().timestamp - history.first().timestamp
            } else 0L
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

/**
 * Estadísticas de sensores
 */
data class SensorStats(
    val avgTemperature: Float,
    val avgHumidity: Float,
    val avgLight: Int,
    val dataPoints: Int,
    val timeSpan: Long
)

/**
 * Factory para el ViewModel
 */
class ESP32ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ESP32ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ESP32ViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}