# ESP32 UDP Microcontroller Lab - AI Coding Agent Instructions

## Project Overview
This is an ESP32 Arduino project that implements a distributed IoT system using UDP protocol for bidirectional communication between an ESP32 microcontroller and an Android phone. The system reads two sensors (DHT11 and LDR) and controls four digital actuators (LEDs) remotely.

## Architecture & Design Patterns

### Hardware Configuration
- **Target Platform**: ESP32/ESP32-S3 microcontroller
- **Sensors**: DHT11 (temp/humidity) on GPIO 4, LDR (light) on GPIO 34
- **Actuators**: 4 LEDs on GPIO 5, 18, 36, 21
- **Communication**: WiFi + UDP protocol (bidirectional)
- **Serial**: 115200 baud for debugging

### Code Structure
- **Single File Architecture**: All code resides in `main.ino` following Arduino IDE conventions
- **UDP Server/Client**: ESP32 acts as both UDP server (listening) and client (sending)
- **Real-time Data**: Sensors read at 4Hz minimum (250ms intervals)
- **JSON Protocol**: Structured messages for sensor data and control commands

### Network Configuration
```cpp
// WiFi and UDP settings in main.ino
const char* ssid = "TU_SSID";
const char* password = "TU_PASSWORD";
IPAddress phoneIP(192, 168, 1, 100);  // Android phone IP
const int localUdpPort = 4210;        // ESP32 listening port
const int phoneUdpPort = 4211;        // Phone listening port
```

## Communication Protocol

### Sensor Data (ESP32 → Phone)
**Frequency**: 4Hz (every 250ms)  
**Port**: 4211  
**Format**: JSON
```json
{
  "temp": 25.6,
  "hum": 60.2, 
  "luz": 75,
  "led1": true,
  "led2": false,
  "led3": true,
  "led4": false,
  "error_dht": false,
  "wifi_ok": true,
  "timestamp": 123456
}
```

### Control Commands (Phone → ESP32)
**Port**: 4210  
**Format**: JSON
```json
{"led1": true, "led2": false, "led3": true, "led4": false}
```

## Development Workflow

### Dependencies Installation
Required libraries in Arduino IDE:
```
- DHT sensor library (by Adafruit)
- ArduinoJson (by Benoit Blanchon)  
- WiFi (included in ESP32 Core)
- WiFiUdp (included in ESP32 Core)
```

### Build & Upload Process
1. Install required libraries in Arduino IDE
2. Configure WiFi credentials and phone IP in `main.ino`
3. Upload to ESP32/ESP32-S3
4. Monitor serial output at 115200 baud for IP and status
5. Configure MIT App Inventor app with ESP32 IP

### Hardware Connections
- **DHT11**: Data pin → GPIO 4, VCC → 3.3V, GND → GND
- **LDR**: Signal → GPIO 34 (with voltage divider), VCC → 3.3V, GND → GND  
- **LEDs**: Anodes → GPIO 5,18,36,21 (with resistors), Cathodes → GND

### Debugging Patterns
- **Serial Monitor**: Primary debugging with emoji-coded messages (🔹, ✅, 🌐, 📡)
- **Network Discovery**: ESP32 prints IP addresses and port configuration
- **Real-time Logging**: Sensor readings and command processing logged
- **Error Detection**: WiFi disconnection, DHT sensor failures tracked

## Key Implementation Details

### Error Handling
- **WiFi Reconnection**: Automatic reconnection on WiFi loss
- **Sensor Validation**: DHT11 NaN detection and error flagging
- **UDP Communication**: Graceful handling of malformed JSON packets

### Timing and Performance
- **Non-blocking Loop**: Main loop optimized for real-time performance
- **4Hz Guaranteed**: Timer-based sensor reading ensures minimum frequency
- **Low Latency**: UDP protocol provides minimal communication delay

### Extension Patterns
When adding new sensors or actuators:
1. Add pin constants after existing definitions
2. Update `leerSensores()` function for new sensor readings
3. Modify JSON structure in `enviarDatosSensores()`
4. Add command processing in `procesarComandoUDP()`
5. Update error handling variables as needed

### MIT App Inventor Integration
- **Receive**: Listen on port 4211 for sensor data JSON
- **Send**: Send commands to ESP32 IP on port 4210
- **Parse**: Decode JSON for temperature, humidity, light, LED states
- **Display**: Real-time updates of sensor values and actuator states

## Testing Strategy
- **Network Connectivity**: Verify WiFi connection and IP assignment
- **Sensor Validation**: Check DHT11 and LDR readings via serial monitor
- **UDP Communication**: Test bidirectional data flow with network tools
- **App Integration**: Validate complete system with MIT App Inventor app

## Deployment Notes
- **Network Requirements**: ESP32 and phone must be on same WiFi network
- **IP Configuration**: Update `phoneIP` to match Android device IP
- **Firewall**: Ensure UDP ports 4210/4211 are not blocked
- **Power Supply**: Stable 3.3V/5V supply recommended for sensor accuracy