package com.esp32.udpcontrol

import kotlinx.coroutines.*
import java.net.*

class UdpClient(
    private val esp32IP: String,
    private val esp32Port: Int,
    private val localPort: Int,
    private val onDataReceived: (SensorData) -> Unit
) {
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private var listenerJob: Job? = null
    
    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            socket = DatagramSocket(localPort)
            isRunning = true
            
            // Iniciar listener en coroutine separada
            listenerJob = CoroutineScope(Dispatchers.IO).launch {
                listenForData()
            }
            
            // Solicitar datos iniciales
            delay(500)
            requestData()
            
        } catch (e: Exception) {
            throw Exception("Error iniciando UDP client: ${e.message}")
        }
    }
    
    fun stop() {
        isRunning = false
        listenerJob?.cancel()
        socket?.close()
        socket = null
    }
    
    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        try {
            val socket = socket ?: throw Exception("Socket no inicializado")
            
            val buffer = command.toByteArray()
            val address = InetAddress.getByName(esp32IP)
            val packet = DatagramPacket(buffer, buffer.size, address, esp32Port)
            
            socket.send(packet)
            
        } catch (e: Exception) {
            throw Exception("Error enviando comando: ${e.message}")
        }
    }
    
    suspend fun requestData() {
        sendCommand("GET_DATA")
    }
    
    private suspend fun listenForData() {
        val buffer = ByteArray(1024)
        
        while (isRunning) {
            try {
                val socket = socket ?: break
                
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                
                val receivedData = String(packet.data, 0, packet.length)
                
                // Parsear datos del sensor
                val sensorData = parseSensorData(receivedData)
                if (sensorData != null) {
                    onDataReceived(sensorData)
                }
                
            } catch (e: SocketTimeoutException) {
                // Timeout normal, continuar
                continue
            } catch (e: Exception) {
                if (isRunning) {
                    // Solo reportar error si aún estamos corriendo
                    println("Error recibiendo datos: ${e.message}")
                }
                break
            }
        }
    }
    
    private fun parseSensorData(data: String): SensorData? {
        try {
            // Formato esperado: temp;hum;luz;led1;led2;led3;led4;error_dht;wifi_ok
            // Ejemplo: "22.9;44.4;84;0;0;0;0;0;1"
            
            val parts = data.trim().split(";")
            if (parts.size >= 9) {
                return SensorData(
                    temperature = parts[0].toFloat(),
                    humidity = parts[1].toFloat(),
                    light = parts[2].toInt(),
                    led1 = parts[3] == "1",
                    led2 = parts[4] == "1",
                    led3 = parts[5] == "1",
                    led4 = parts[6] == "1",
                    errorDHT = parts[7] == "1",
                    wifiOK = parts[8] == "1"
                )
            }
        } catch (e: Exception) {
            println("Error parseando datos del sensor: ${e.message}")
            println("Datos recibidos: $data")
        }
        
        return null
    }
}