# ESP32 UDP Microcontroller Lab - AI Coding Agent Instructions

## Project Overview
This is a complete ESP32 Arduino project that implements a distributed IoT system using UDP protocol for bidirectional communication between an ESP32 microcontroller and multiple client interfaces. The system reads two sensors (DHT11 and LDR) and controls four digital actuators (LEDs) remotely through WiFi networking.

## Architecture & Design Patterns

### Hardware Configuration
- **Target Platform**: ESP32/ESP32-S3 microcontroller
- **Sensors**: DHT11 (temp/humidity) on GPIO 4, LDR (light) on GPIO 3
- **Actuators**: 4 LEDs on GPIO 5, 18, 2, 21 (GPIO 36 replaced with GPIO 2 due to input-only limitation)
- **Communication**: WiFi + UDP protocol (bidirectional)
- **Serial**: 115200 baud for debugging and manual testing

### Code Structure
- **Single File Architecture**: All code resides in `main.ino` following Arduino IDE conventions
- **UDP Server/Client**: ESP32 acts as both UDP server (listening) and client (sending)
- **Real-time Data**: Sensors read at 4Hz minimum (250ms intervals)
- **Text Protocol**: Simple semicolon-separated format for broad compatibility

### Network Configuration
```cpp
// WiFi and UDP settings in main.ino
const char* ssid = "paisanet";         // WiFi network name
const char* password = "paisanet";     // WiFi password
IPAddress phoneIP(192,168,43,8);       // Target device IP for sensor data
const int localUdpPort = 4210;         // ESP32 listening port for commands
const int phoneUdpPort = 4211;         // Target port for sending sensor data
```

## Communication Protocol

### Sensor Data (ESP32 → Client)
**Frequency**: 4Hz (every 250ms)  
**Port**: 4211  
**Format**: Text (semicolon-separated)
```
23.0;33.3;100;0;0;0;0;0;1
```
**Fields**: `temp;humidity;light;led1;led2;led3;led4;error_dht;wifi_ok`

### Control Commands (Client → ESP32)
**Port**: 4210  
**Format**: Text commands
```
Examples:
"1" - Toggle LED 1
"2" - Toggle LED 2  
"3" - Toggle LED 3
"4" - Toggle LED 4
"0" - Turn off all LEDs
"LED1_ON" - Turn on LED 1
"LED2_OFF" - Turn off LED 2
"1;0;1;0" - Set all LED states at once
"GET_DATA" - Request immediate sensor data
```

## Client Applications

### 1. Android App (Primary Interface)
- **Path**: `app_2/` directory
- **Language**: Kotlin with Material 3 design
- **Features**: Real-time sensor monitoring, LED control, dynamic IP configuration
- **Architecture**: MVVM with coroutines and StateFlow
- **Build**: `./gradlew assembleDebug` generates APK

### 2. PyQt6 Desktop Monitor
- **Path**: `esp32_serial_monitor.py`
- **Features**: Serial monitoring, real-time graphs, LED control via serial commands
- **Requirements**: PyQt6, pyqtgraph, pyserial
- **Usage**: Direct ESP32 debugging and visualization

### 3. Mobile Web App
- **Path**: `esp32_mobile_web.html`
- **Features**: Responsive design, UDP communication, cross-platform compatibility
- **Usage**: Direct browser access for instant control

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
5. Configure client apps with ESP32 IP address

### Hardware Connections
- **DHT11**: Data pin → GPIO 4, VCC → 3.3V, GND → GND
- **LDR**: Signal → GPIO 3 (with voltage divider), VCC → 3.3V, GND → GND  
- **LEDs**: Anodes → GPIO 5,18,2,21 (with current-limiting resistors), Cathodes → GND

### Network Setup
1. **WiFi Configuration**: ESP32 connects to specified network
2. **IP Discovery**: ESP32 prints its IP address via serial monitor
3. **Client Configuration**: Update client apps with ESP32 IP address
4. **Port Configuration**: ESP32 listens on 4210, sends to 4211

### Debugging Patterns
- **Serial Monitor**: Primary debugging with emoji-coded messages (🔹, ✅, 🌐, 📡)
- **Network Discovery**: ESP32 prints IP addresses and port configuration
- **Real-time Logging**: Sensor readings and command processing logged
- **Error Detection**: WiFi disconnection, DHT sensor failures tracked
- **Manual Testing**: Serial commands (test1, test2, allon, alloff, status, udptest)

## Key Implementation Details

### Error Handling
- **WiFi Reconnection**: Automatic reconnection on WiFi loss
- **Sensor Validation**: DHT11 NaN detection and error flagging
- **UDP Communication**: Graceful handling of malformed packets

### Timing and Performance
- **Non-blocking Loop**: Main loop optimized for real-time performance
- **4Hz Guaranteed**: Timer-based sensor reading ensures minimum frequency
- **Low Latency**: UDP protocol provides minimal communication delay

### Extension Patterns
When adding new sensors or actuators:
1. Add pin constants after existing definitions
2. Update `leerSensores()` function for new sensor readings
3. Modify text format in `enviarDatosSensores()`
4. Add command processing in `procesarComandoUDP()`
5. Update error handling variables as needed

### Client Integration
- **Android App**: Connect using ESP32 IP on port 4210, listen on port 4211
- **PyQt6 Monitor**: Serial connection for debugging and LED control
- **Web App**: Direct UDP communication via JavaScript WebRTC/Socket APIs
- **Protocol Compatibility**: Text format ensures broad device support

## Testing Strategy
- **Network Connectivity**: Verify WiFi connection and IP assignment
- **Sensor Validation**: Check DHT11 and LDR readings via serial monitor
- **LED Hardware**: Test individual LEDs with serial commands (test1, test2, etc.)
- **UDP Communication**: Test bidirectional data flow with client apps
- **Error Recovery**: Test WiFi disconnection and sensor failure scenarios

## Deployment Notes
- **Network Requirements**: ESP32 and clients must be on same WiFi network
- **IP Configuration**: Update client apps with ESP32 IP address from serial monitor
- **Firewall**: Ensure UDP ports 4210/4211 are not blocked
- **Power Supply**: Stable 3.3V/5V supply recommended for sensor accuracy
- **Hardware Verification**: Use serial commands to verify LED connectivity before UDP testing