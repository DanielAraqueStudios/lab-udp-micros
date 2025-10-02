package com.esp32.udpcontrol

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.*
import java.io.IOException

/**
 * Cliente UDP para comunicación bidireccional con ESP32
 * Maneja tanto el envío de comandos como la recepción de datos de sensores
 */
class UdpClient(
    private val config: NetworkConfig,
    private val coroutineScope: CoroutineScope
) {
    private var listeningSocket: DatagramSocket? = null
    private var sendingSocket: DatagramSocket? = null
    private var listeningJob: Job? = null
    private var isRunning = false
    
    private val _receivedData = MutableSharedFlow<SensorData>()
    val receivedData: SharedFlow<SensorData> = _receivedData.asSharedFlow()
    
    private val _connectionErrors = MutableSharedFlow<String>()
    val connectionErrors: SharedFlow<String> = _connectionErrors.asSharedFlow()
    
    /**
     * Inicia la comunicación UDP con el ESP32
     */
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext true
        
        try {
            // Socket para recibir datos del ESP32
            listeningSocket = DatagramSocket(config.localPort)
            listeningSocket?.soTimeout = 1000 // Timeout de 1 segundo
            
            // Socket para enviar comandos al ESP32
            sendingSocket = DatagramSocket()
            
            isRunning = true
            
            // Iniciar hilo de escucha
            startListening()
            
            true
        } catch (e: Exception) {
            _connectionErrors.emit("Error al iniciar UDP: ${e.message}")
            false
        }
    }
    
    /**
     * Detiene la comunicación UDP
     */
    fun stop() {
        isRunning = false
        listeningJob?.cancel()
        
        try {
            listeningSocket?.close()
            sendingSocket?.close()
        } catch (e: Exception) {
            // Ignorar errores al cerrar sockets
        }
        
        listeningSocket = null
        sendingSocket = null
    }
    
    /**
     * Envía un comando de LEDs al ESP32
     */
    suspend fun sendLedCommand(command: LedCommand): Boolean = withContext(Dispatchers.IO) {
        try {
            val commandString = command.toCommandString()
            val buffer = commandString.toByteArray()
            
            val address = InetAddress.getByName(config.esp32Ip)
            val packet = DatagramPacket(
                buffer,
                buffer.size,
                address,
                config.esp32Port
            )
            
            sendingSocket?.send(packet)
            true
        } catch (e: Exception) {
            _connectionErrors.emit("Error enviando comando: ${e.message}")
            false
        }
    }
    
    /**
     * Envía un ping al ESP32 para verificar conexión
     */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pingMessage = "ping"
            val buffer = pingMessage.toByteArray()
            
            val address = InetAddress.getByName(config.esp32Ip)
            val packet = DatagramPacket(
                buffer,
                buffer.size,
                address,
                config.esp32Port
            )
            
            sendingSocket?.send(packet)
            true
        } catch (e: Exception) {
            _connectionErrors.emit("Error en ping: ${e.message}")
            false
        }
    }
    
    /**
     * Inicia el hilo de escucha para recibir datos del ESP32
     */
    private fun startListening() {
        listeningJob = coroutineScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            
            while (isRunning && !currentCoroutineContext().isActive.not()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    listeningSocket?.receive(packet)
                    
                    val receivedString = String(packet.data, 0, packet.length)
                    
                    // Parsear datos del sensor
                    val sensorData = SensorData.fromString(receivedString)
                    if (sensorData != null) {
                        _receivedData.emit(sensorData)
                    }
                    
                } catch (e: SocketTimeoutException) {
                    // Timeout normal, continuar
                    continue
                } catch (e: IOException) {
                    if (isRunning) {
                        _connectionErrors.emit("Error de conexión: ${e.message}")
                    }
                    break
                } catch (e: Exception) {
                    if (isRunning) {
                        _connectionErrors.emit("Error inesperado: ${e.message}")
                    }
                    break
                }
            }
        }
    }
    
    /**
     * Verifica si el cliente está funcionando
     */
    fun isConnected(): Boolean = isRunning && listeningSocket?.isBound == true
    
    /**
     * Obtiene información del estado de la conexión
     */
    fun getConnectionInfo(): String {
        return if (isConnected()) {
            "Conectado - Escuchando en puerto ${config.localPort}, enviando a ${config.esp32Ip}:${config.esp32Port}"
        } else {
            "Desconectado"
        }
    }
}

/**
 * Factory para crear instancias de UdpClient
 */
object UdpClientFactory {
    fun create(
        config: NetworkConfig,
        coroutineScope: CoroutineScope
    ): UdpClient {
        return UdpClient(config, coroutineScope)
    }
}