# ESP32 UDP Controller - Android App

## Overview
Native Android application to control ESP32 via UDP communication. This app provides real-time sensor monitoring and LED control for the ESP32 distributed IoT system.

## Features
- **Real-time Sensor Monitoring**: Temperature, humidity, and light level graphs
- **LED Control**: Individual control of 4 LEDs with visual feedback
- **UDP Communication**: Bidirectional communication with ESP32 on ports 4210/4211
- **Material 3 UI**: Modern Android design with dark theme support
- **Network Discovery**: Automatic ESP32 IP detection and manual configuration

## Technical Specifications
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 24 (Android 7.0)
- **Architecture**: MVVM with Kotlin Coroutines
- **UI Framework**: Jetpack Compose with Material 3
- **Charts**: MPAndroidChart for real-time data visualization
- **Networking**: Native Android UDP sockets with coroutines

## Communication Protocol
### Data Reception (ESP32 → Android)
**Port**: 4211  
**Format**: Text (semicolon-separated)  
**Frequency**: 4Hz  
```
"temp;hum;luz;led1;led2;led3;led4;error;wifi"
Example: "22.6;47.1;100;1;0;1;0;0;1"
```

### Control Commands (Android → ESP32)
**Port**: 4210  
**Supported Commands**:
- `"1"`, `"2"`, `"3"`, `"4"` - Toggle LEDs 1-4
- `"0"` - Turn off all LEDs
- `"LED1:1"`, `"LED2:0"` - Specific LED control
- `"GET_DATA"` - Request sensor data

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or higher
- Android SDK with API 34
- Gradle 8.4 (managed by wrapper)

### Build Steps
1. **Open Project**: Open android-app folder in Android Studio
2. **Sync Dependencies**: Android Studio will sync Gradle automatically
3. **Build APK**: Use `./gradlew assembleDebug` or Android Studio build
4. **Install**: Enable USB debugging and install via `./gradlew installDebug`

## Network Configuration
1. **WiFi Setup**: Ensure both devices on same network (e.g., "Velasco")
2. **ESP32 IP**: Typically `10.175.23.159` - configure in app if different
3. **Ports**: UDP 4210 (commands) and 4211 (sensor data)

## Project Structure
Complete Android project with:
- **MainActivity.kt**: Main UI with sensor cards and LED controls
- **UdpClient.kt**: Coroutine-based UDP communication
- **SensorData.kt**: Data models for parsing ESP32 messages
- **Material 3 UI**: Modern layouts with adaptive icons
- **Build Configuration**: Gradle 8.4 with modern Android dependencies

## Integration
This app integrates with:
- **ESP32 Firmware**: `main.ino` providing UDP server/client
- **PyQt6 Monitor**: Desktop debugging interface
- **Network**: 4Hz sensor data + real-time LED control

## Troubleshooting
1. **No Data**: Check WiFi connection and ESP32 IP configuration
2. **LED Control Issues**: Verify UDP command format and ESP32 response
3. **Build Errors**: Ensure Android Studio sync completed successfully

The app is ready for compilation in Android Studio with all dependencies configured.

### 🌐 **Comunicación UDP**
- **IP ESP32**: `10.175.23.159`
- **Puerto ESP32**: `4210` (escucha comandos)
- **Puerto App**: `4211` (recibe datos)
- **Protocolo**: Texto separado por `;`
- **Frecuencia**: 4Hz (datos cada 250ms)

## 🛠️ Configuración de Android Studio

### **1. Crear nuevo proyecto**
```bash
- Choose "Empty Activity"
- Name: "ESP32 UDP Control"
- Package: com.esp32.udpcontrol
- Language: Kotlin
- Minimum SDK: API 24 (Android 7.0)
```

### **2. Copiar archivos**
Copia toda la estructura de carpetas `android-app/` a tu proyecto Android Studio.

### **3. Sincronizar proyecto**
- File → Sync Project with Gradle Files
- Build → Rebuild Project

## 📋 Dependencias Principales

```kotlin
// Material Design y componentes básicos
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// ViewModel y LiveData
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'

// Gráficos (MPAndroidChart)
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

## 🔧 Configuración de Red

### **IP Addresses (Actualizar si es necesario)**
```kotlin
// En MainActivity.kt, líneas 22-24
val esp32IP = "10.175.23.159"    // IP de tu ESP32
val esp32Port = 4210             // Puerto ESP32 (comandos)
val localPort = 4211             // Puerto app (datos)
```

### **Permisos en AndroidManifest.xml**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

## 🎮 Uso de la App

### **Interfaz Principal**
1. **Switch de conexión** - Conectar/desconectar del ESP32
2. **Datos de sensores** - Valores en tiempo real
3. **Botones de LEDs** - Control individual (toggle)
4. **Botón "Apagar todos"** - Comando masivo
5. **Gráfico** - Historial visual de sensores
6. **Botón actualizar** - Solicitar datos inmediatos

### **Comandos UDP que envía la app**
```
"1" → Toggle LED 1
"2" → Toggle LED 2  
"3" → Toggle LED 3
"4" → Toggle LED 4
"0" → Apagar todos los LEDs
"GET_DATA" → Solicitar datos inmediatos
```

### **Formato de datos que recibe**
```
"22.9;44.4;84;0;0;0;0;0;1"
 │    │   │  │ │ │ │ │ │
 │    │   │  │ │ │ │ │ └─ WiFi OK (1=conectado)
 │    │   │  │ │ │ │ └─── Error DHT (0=ok, 1=error)  
 │    │   │  │ │ │ └───── LED 4 (0=off, 1=on)
 │    │   │  │ │ └─────── LED 3
 │    │   │  │ └───────── LED 2
 │    │   │  └─────────── LED 1
 │    │   └───────────── Luminosidad (%)
 │    └───────────────── Humedad (%)
 └────────────────────── Temperatura (°C)
```

## 🎨 Características de la UI

### **Material Design 3**
- 🎨 Colores modernos y accesibles
- 📱 Responsive design para diferentes pantallas
- 🌙 Soporte para tema claro/oscuro automático

### **Componentes Visuales**
- 📊 **CardViews** para organizar secciones
- 📈 **LineChart** para gráficos interactivos
- 🎛️ **Switches** y **Buttons** con feedback visual
- 📱 **ScrollView** para contenido completo

### **Estados de LEDs**
- 🔴 LED 1 - Color rojo cuando está encendido
- 🟢 LED 2 - Color verde cuando está encendido
- 🔵 LED 3 - Color azul cuando está encendido  
- 🟠 LED 4 - Color naranja cuando está encendido
- ⚫ Todos - Color gris cuando están apagados

## 🐛 Troubleshooting

### **Problemas de Conexión**
```
❌ "Error de conexión: socket creation failed"
→ Verificar permisos de red en AndroidManifest.xml
→ Asegurar que el dispositivo esté en la misma red WiFi

❌ "Timeout receiving data"  
→ Verificar IP del ESP32 (10.175.23.159)
→ Confirmar que ESP32 esté enviando datos cada 250ms

❌ "Command not sent"
→ Verificar puerto ESP32 (4210)
→ Revisar firewall del router
```

### **Problemas de Parsing**
```
❌ "Error parseando datos del sensor"
→ Verificar formato de datos: "temp;hum;luz;led1;led2;led3;led4;error;wifi"
→ Confirmar que ESP32 envíe exactamente 9 campos separados por ";"
```

### **Problemas de UI**
```
❌ "Gráfico no se actualiza"
→ Verificar que lleguen datos del ESP32
→ Revisar logs en Logcat para errores de parsing

❌ "Botones de LED no responden"  
→ Verificar que se envíen comandos UDP
→ Confirmar que ESP32 reciba y procese comandos
```

## 📱 Instalación

### **Desde Android Studio**
1. Abrir proyecto en Android Studio
2. Conectar dispositivo Android (USB debugging habilitado)
3. Build → Run 'app'

### **APK Release**
1. Build → Generate Signed Bundle/APK
2. Seguir wizard para crear keystore
3. Instalar APK en dispositivo

## 🔮 Futuras Mejoras

- 📊 **Exportar datos** a CSV/Excel
- 🔔 **Notificaciones** para alertas de sensores
- ⚙️ **Configuración** de IPs desde la app
- 📈 **Más gráficos** (barras, pie charts)
- 🌙 **Tema oscuro** manual
- 📱 **Widget** para pantalla principal
- 🔄 **Auto-reconnect** inteligente

## 📞 Soporte

Para problemas o mejoras:
1. Verificar logs en Android Studio (Logcat)
2. Confirmar comunicación UDP con ESP32
3. Revisar configuración de red WiFi
4. Contactar al desarrollador con logs específicos

---

**🎯 Proyecto completo listo para compilar y usar!** 🚀