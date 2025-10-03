# 🚀 ESP32 UDP Microcontroller Lab

![ESP32](https://img.shields.io/badge/ESP32-E7352C?style=for-the-badge&logo=espressif&logoColor=white)
![Arduino](https://img.shields.io/badge/Arduino-00979D?style=for-the-badge&logo=Arduino&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)

Un sistema IoT completo que implementa comunicación UDP bidireccional entre un microcontrolador ESP32 y múltiples interfaces cliente para monitoreo de sensores y control remoto de actuadores.

## 📋 Características

- 🌡️ **Monitoreo de sensores en tiempo real** (DHT11 temperatura/humedad, LDR luminosidad)
- 💡 **Control remoto de LEDs** via UDP
- 📱 **App Android nativa** con Material Design 3
- 🖥️ **Monitor PyQt6** con gráficas en tiempo real
- 🌐 **Aplicación web móvil** responsive
- 📡 **Comunicación UDP a 4Hz** (250ms)
- 🔄 **Protocolo de texto simple** compatible con múltiples plataformas

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    UDP 4211    ┌──────────────────┐
│     ESP32       │───────────────▶│   Clientes       │
│  📡 WiFi        │                │  📱 Android      │
│  🌡️ DHT11       │                │  🖥️ PyQt6       │
│  ☀️ LDR         │                │  🌐 Web App     │
│  💡 4x LEDs     │◀───────────────│                  │
└─────────────────┘    UDP 4210    └──────────────────┘
```

## 🔧 Hardware Requerido

### Componentes Principales
- **ESP32 o ESP32-S3** microcontrolador
- **DHT11** sensor de temperatura y humedad
- **LDR** sensor de luminosidad
- **4x LEDs** con resistencias limitadoras
- **Resistencias** para divisor de voltaje (LDR)
- **Protoboard y cables** de conexión

### Conexiones
```
DHT11:  Data → GPIO 4,  VCC → 3.3V,  GND → GND
LDR:    Signal → GPIO 3 (con divisor de voltaje)
LEDs:   Ánodos → GPIO 5, 18, 2, 21 (con resistencias)
```

## 🚀 Inicio Rápido

### 1. Configuración del ESP32

1. **Instalar librerías en Arduino IDE:**
   ```
   - DHT sensor library (Adafruit)
   - ArduinoJson (Benoit Blanchon)
   ```

2. **Configurar WiFi en `main.ino`:**
   ```cpp
   const char* ssid = "TU_WIFI";
   const char* password = "TU_PASSWORD";
   ```

3. **Cargar código al ESP32** y obtener IP desde Serial Monitor

### 2. Configuración de Clientes

#### Android App
```bash
cd app_2/
./gradlew assembleDebug
# Instalar APK en dispositivo
```

#### Monitor PyQt6
```bash
pip install PyQt6 pyqtgraph pyserial
python esp32_serial_monitor.py
```

#### Web App
Abrir `esp32_mobile_web.html` en navegador móvil

### Conexiones

| Componente | Pin ESP32 | Descripción |
|------------|-----------|-------------|
| DHT11 Data | GPIO 4 | Señal digital del sensor |
| LDR Signal | GPIO 34 | Entrada analógica (0-3.3V) |
| LED 1 | GPIO 5 | Actuador digital ON/OFF |
| LED 2 | GPIO 18 | Actuador digital ON/OFF |
| LED 3 | GPIO 36 | Actuador digital ON/OFF |
| LED 4 | GPIO 21 | Actuador digital ON/OFF |

## 📦 Dependencias

### Librerías Arduino IDE
```cpp
#include <WiFi.h>          // Conectividad WiFi (incluida en ESP32 Core)
#include <WiFiUdp.h>       // Protocolo UDP (incluida en ESP32 Core)
#include <DHT.h>           // Sensor DHT11 (by Adafruit)
#include <ArduinoJson.h>   // Manejo de JSON (by Benoit Blanchon)
```

### Instalación de Librerías
1. Abrir Arduino IDE
2. Ir a **Tools > Manage Libraries**
3. Buscar e instalar:
   - "DHT sensor library" by Adafruit
   - "ArduinoJson" by Benoit Blanchon

## 🚀 Configuración y Uso

### 1. Configuración de Red
```cpp
// Editar en main.ino
const char* ssid = "TU_NOMBRE_WIFI";           // Nombre de tu red WiFi
const char* password = "TU_PASSWORD_WIFI";      // Contraseña de tu red
IPAddress phoneIP(192, 168, 1, 100);           // IP de tu teléfono Android
```

### 2. Compilación y Carga
1. Conectar ESP32 por USB
2. Seleccionar la placa correcta en Arduino IDE
3. Compilar y cargar el código
4. Abrir Serial Monitor a 115200 baudios

### 3. Obtener IP del ESP32
Una vez conectado, el ESP32 mostrará en el Serial Monitor:
```
✅ Conectado al WiFi!
🌐 IP ESP32: 192.168.1.150
📡 Servidor UDP iniciado en puerto 4210
```

### 4. Configurar App MIT App Inventor
- **IP destino**: Usar la IP mostrada por el ESP32
- **Puerto para recibir datos**: 4211
- **Puerto para enviar comandos**: 4210

## 📡 Protocolo de Comunicación

### Datos de Sensores (ESP32 → Teléfono)
**Frecuencia**: 4Hz (cada 250ms)  
**Puerto**: 4211  
**Formato**: JSON

```json
{
  "temp": 25.6,           // Temperatura en °C
  "hum": 60.2,            // Humedad en %
  "luz": 75,              // Luminosidad en % (0-100)
  "led1": true,           // Estado LED 1
  "led2": false,          // Estado LED 2  
  "led3": true,           // Estado LED 3
  "led4": false,          // Estado LED 4
  "error_dht": false,     // Error en sensor DHT11
  "wifi_ok": true,        // Estado de conexión WiFi
  "timestamp": 123456     // Marca de tiempo
}
```

### Comandos de Control (Teléfono → ESP32)
**Puerto**: 4210  
**Formato**: JSON

```json
{
  "led1": true,    // Encender LED 1
  "led2": false,   // Apagar LED 2
  "led3": true,    // Encender LED 3
  "led4": false    // Apagar LED 4
}
```

## 🎛️ MIT App Inventor - Guía de Implementación

### Componentes Necesarios
- **Clock**: Timer para recibir datos cada 250ms
- **Web**: Para comunicación UDP
- **Labels**: Mostrar valores de sensores
- **Buttons/Switches**: Controlar LEDs
- **Canvas**: Opcional para gráficos

### Bloques Clave
```
Cuando Clock1.Timer:
  - Web1.Get de "IP_ESP32:4211"
  - Procesar respuesta JSON
  - Actualizar labels de sensores

Cuando Button_LED1.Click:
  - Crear JSON {"led1": true}
  - Web1.PostText a "IP_ESP32:4210"
```

## 🔍 Debugging y Monitoreo

### Serial Monitor
El ESP32 proporciona información detallada:
```
🚀 Iniciando ESP32 UDP Lab...
🌡️ Sensor DHT11 inicializado
✅ Conectado al WiFi!
🌐 IP ESP32: 192.168.1.150
📡 Servidor UDP iniciado en puerto 4210
📤 Enviado: {"temp":24.5,"hum":58.3,...}
📥 Comando recibido: {"led1":true}
💡 LED 1: ENCENDIDO
```

### Indicadores de Estado
- 🔹 **Configuración**: Inicialización del sistema
- ✅ **Éxito**: Operaciones completadas
- 🌐 **Red**: Estado de conectividad WiFi
- 📡 **UDP**: Comunicación de datos
- 📤 **Envío**: Datos transmitidos al teléfono
- 📥 **Recepción**: Comandos recibidos del teléfono
- 💡 **Actuadores**: Estado de LEDs
- ❌ **Errores**: Fallos detectados

## 🛠️ Resolución de Problemas

### Problemas Comunes

**1. Error de conexión WiFi**
```
❌ WiFi desconectado - Intentando reconectar...
```
- Verificar credenciales WiFi
- Confirmar que la red esté disponible
- Revisar intensidad de señal

**2. Error del sensor DHT11**
```
❌ Error leyendo DHT11
```
- Verificar conexiones del sensor
- Confirmar alimentación 3.3V/5V
- Revisar integridad del sensor

**3. No se reciben comandos UDP**
- Verificar que ambos dispositivos estén en la misma red
- Confirmar puertos UDP (4210/4211)
- Revisar IP del teléfono en el código
- Verificar firewall del router

**4. Compilación fallida**
- Instalar librerías faltantes
- Verificar versión del ESP32 Core
- Revisar sintaxis del código

## 📊 Especificaciones Técnicas

### Rendimiento
- **Frecuencia de muestreo**: ≥4Hz (250ms entre lecturas)
- **Latencia UDP**: <10ms en red local
- **Precisión DHT11**: ±2°C, ±5% HR
- **Resolución LDR**: 12 bits (0-4095)

### Consumo
- **ESP32 activo**: ~240mA
- **LEDs**: ~20mA c/u (con resistencia 220Ω)
- **DHT11**: ~2.5mA
- **Total estimado**: ~320mA

### Rangos de Operación
- **Temperatura**: -40°C a +80°C (DHT11: 0-50°C)
- **Humedad**: 20-90% RH
- **Luminosidad**: 0-100% (relativo)
- **Voltaje**: 3.3V (ESP32), 5V tolerante en algunos pines

## 🤝 Contribución

1. Fork el repositorio
2. Crear una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👨‍💻 Autor

**Daniel Araque Studios**
- GitHub: [@DanielAraqueStudios](https://github.com/DanielAraqueStudios)

## 🙏 Agradecimientos

- Universidad Militar Nueva Granada - Facultad de Ingeniería
- Comunidad Arduino y ESP32
- Adafruit por las librerías de sensores
- MIT App Inventor Team

---

> **Nota**: Este proyecto fue desarrollado como parte del laboratorio de Conectividad WiFi en sistemas embebidos. El objetivo es demostrar la implementación de sistemas distribuidos utilizando protocolos UDP para IoT.