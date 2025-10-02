package com.esp32.udpcontrol

data class SensorData(
    val temperature: Float,
    val humidity: Float,
    val light: Int,
    val led1: Boolean,
    val led2: Boolean,
    val led3: Boolean,
    val led4: Boolean,
    val errorDHT: Boolean,
    val wifiOK: Boolean
) {
    override fun toString(): String {
        return "SensorData(temp=${temperature}°C, hum=${humidity}%, light=${light}%, " +
               "LEDs=[${led1.toInt()},${led2.toInt()},${led3.toInt()},${led4.toInt()}], " +
               "dhtError=$errorDHT, wifi=$wifiOK)"
    }
    
    private fun Boolean.toInt() = if (this) 1 else 0
}