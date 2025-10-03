package com.esp32.udpcontrol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modelo de datos para la información recibida del ESP32
 * Protocolo: "temp;hum;luz;led1;led2;led3;led4;error;wifi"
 */
data class SensorData(
    val temperature: Float = 0.0f,
    val humidity: Float = 0.0f,
    val light: Int = 0,
    val led1: Boolean = false,
    val led2: Boolean = false,
    val led3: Boolean = false,
    val led4: Boolean = false,
    val errorDht: Boolean = false,
    val wifiOk: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Parsea el string de datos recibido del ESP32
         * Formato: "temp;hum;luz;led1;led2;led3;led4;error;wifi"
         */
        fun fromString(data: String): SensorData? {
            return try {
                val parts = data.trim().split(";")
                if (parts.size >= 9) {
                    SensorData(
                        temperature = parts[0].toFloat(),
                        humidity = parts[1].toFloat(),
                        light = parts[2].toInt(),
                        led1 = parts[3] == "1",
                        led2 = parts[4] == "1",
                        led3 = parts[5] == "1",
                        led4 = parts[6] == "1",
                        errorDht = parts[7] == "1",
                        wifiOk = parts[8] == "1"
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Estado de conexión con el ESP32
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Comando para controlar LEDs
 */
data class LedCommand(
    val led1: Boolean = false,
    val led2: Boolean = false,
    val led3: Boolean = false,
    val led4: Boolean = false
) {
    /**
     * Convierte el comando a string para enviar al ESP32
     * Formato: "led1;led2;led3;led4"
     */
    fun toCommandString(): String {
        return "${if (led1) 1 else 0};${if (led2) 1 else 0};${if (led3) 1 else 0};${if (led4) 1 else 0}"
    }
}

/**
 * Configuración de red para ESP32
 */
data class NetworkConfig(
    val esp32Ip: String = "192.168.43.101",  // IP actual del ESP32 en red paisanet
    val esp32Port: Int = 4210,        // Puerto donde escucha el ESP32
    val localPort: Int = 4211,        // Puerto local para recibir datos
    val connectionTimeout: Long = 5000L,
    val updateInterval: Long = 250L    // 4Hz
)

/**
 * Estado del sistema ESP32
 */
data class ESP32State(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastSensorData: SensorData? = null,
    val networkConfig: NetworkConfig = NetworkConfig(),
    val errorMessage: String? = null,
    val isReceivingData: Boolean = false
)

/**
 * Manager para el estado de la aplicación
 */
class ESP32StateManager {
    private val _state = MutableStateFlow(ESP32State())
    val state: StateFlow<ESP32State> = _state.asStateFlow()
    
    private val _sensorHistory = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorHistory: StateFlow<List<SensorData>> = _sensorHistory.asStateFlow()
    
    fun updateConnectionState(newState: ConnectionState) {
        _state.value = _state.value.copy(connectionState = newState)
    }
    
    fun updateSensorData(data: SensorData) {
        _state.value = _state.value.copy(
            lastSensorData = data,
            isReceivingData = true,
            errorMessage = null
        )
        
        // Mantener solo los últimos 100 registros para los gráficos
        val currentHistory = _sensorHistory.value.toMutableList()
        currentHistory.add(data)
        if (currentHistory.size > 100) {
            currentHistory.removeAt(0)
        }
        _sensorHistory.value = currentHistory
    }
    
    fun updateNetworkConfig(config: NetworkConfig) {
        _state.value = _state.value.copy(networkConfig = config)
    }
    
    fun setError(message: String) {
        _state.value = _state.value.copy(
            connectionState = ConnectionState.ERROR,
            errorMessage = message,
            isReceivingData = false
        )
    }
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}