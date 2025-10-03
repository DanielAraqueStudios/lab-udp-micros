# ⚡ Setup Rápido - ESP32 UDP Lab

## 🎯 Configuración en 5 Minutos

### 📋 **Checklist de Materiales**
- [ ] ESP32 o ESP32-S3
- [ ] DHT11 (temperatura/humedad)
- [ ] LDR (sensor de luz)
- [ ] 4x LEDs + resistencias 220Ω
- [ ] Resistencias para divisor de voltaje
- [ ] Protoboard y cables jumper
- [ ] Cable USB para programación

### 🔌 **1. Conexiones Hardware (5 min)**

```
DHT11:
├── VCC → 3.3V ESP32
├── GND → GND ESP32  
└── Data → GPIO 4

LDR + Resistencia (divisor de voltaje):
├── LDR → 3.3V
├── Resistencia 10kΩ → GND
└── Unión → GPIO 3

LEDs + Resistencias 220Ω:
├── LED1 → GPIO 5 → Resistencia → GND
├── LED2 → GPIO 18 → Resistencia → GND
├── LED3 → GPIO 2 → Resistencia → GND
└── LED4 → GPIO 21 → Resistencia → GND
```

### 💻 **2. Software ESP32 (3 min)**

#### Instalar Librerías Arduino IDE:
```
1. Tools → Manage Libraries
2. Buscar "DHT sensor library" by Adafruit → Install
3. Buscar "ArduinoJson" by Benoit Blanchon → Install
```

#### Configurar WiFi:
```cpp
// En main.ino líneas 7-8:
const char* ssid = "TU_WIFI_AQUI";
const char* password = "TU_PASSWORD_AQUI";
```

#### Cargar Código:
```
1. Conectar ESP32 via USB
2. Tools → Board → ESP32 Dev Module (o ESP32-S3)
3. Tools → Port → (seleccionar puerto COM)
4. Upload ↑
```

### 📱 **3. Android App (2 min)**

#### Opción A: APK Pre-compilado
```
1. Transferir app_2/app/build/outputs/apk/debug/app-debug.apk al teléfono
2. Instalar APK (permitir fuentes desconocidas)
3. Abrir app "ESP32 UDP Control"
```

#### Opción B: Compilar desde código
```bash
cd app_2/
./gradlew assembleDebug
# Instalar APK generado
```

### 🖥️ **4. Monitor PyQt6 (Opcional)**

```bash
# Instalar dependencias
pip install PyQt6 pyqtgraph pyserial

# Ejecutar monitor
python esp32_serial_monitor.py
```

### 🌐 **5. Web App (Opcional)**

```
Abrir esp32_mobile_web.html en navegador móvil
```

## 🚀 **Configuración de Red**

### **Paso 1: Obtener IP del ESP32**
1. Abrir Serial Monitor (115200 baud)
2. Resetear ESP32
3. Buscar línea: `🏠 IP Local: 192.168.43.XXX`
4. **Anotar esta IP** ⬅️ IMPORTANTE

### **Paso 2: Configurar Cliente**

#### Android App:
1. Abrir app
2. En campo "IP ESP32", escribir IP obtenida (ej: `192.168.43.101`)
3. Presionar "Conectar"

#### Web App:
```javascript
// Editar línea ~30 en esp32_mobile_web.html:
const ESP32_IP = "192.168.43.101";  // ⬅️ IP del ESP32
```

## ✅ **Verificación Funcionamiento**

### **Test 1: Sensores (ESP32)**
```
Serial Monitor debe mostrar cada 250ms:
📤 [X] UDP → 192.168.43.XXX:4211
📋 TEXTO: 23.0;33.3;100;0;0;0;0;0;1
```

### **Test 2: LEDs Hardware**
```
En Serial Monitor escribir:
test1    # ¿LED 1 enciende?
test2    # ¿LED 2 enciende?
allon    # ¿Todos encienden?
alloff   # ¿Todos apagan?
```

### **Test 3: Control Remoto**
```
En app Android:
1. Presionar botón LED 1
2. En Serial Monitor debe aparecer:
   📥 [1] COMANDO RECIBIDO:
   🌐 Desde: 192.168.43.XXX:XXXX
   📋 TEXTO: 1
   💡 LED 1: 🟢 ENCENDIDO
```

## 🐛 **Solución Rápida de Problemas**

### **LEDs no responden desde app:**
```bash
# Verificar en Serial Monitor:
status   # Ver IP y estadísticas
red      # Ver configuración de red

# Común: IP incorrecta en app Android
```

### **No se ven datos de sensores:**
```bash
# Verificar en main.ino línea 14:
IPAddress phoneIP(192,168,43,XXX);  # ⬅️ IP del teléfono que ejecuta app
```

### **WiFi no conecta:**
```bash
# Verificar credenciales en main.ino líneas 7-8
# Verificar que red WiFi esté disponible
# Resetear ESP32
```

### **LEDs no encienden físicamente:**
```bash
# En Serial Monitor probar:
test1, test2, test3, test4
# Si no funcionan = problema de hardware/conexiones
```

## 📊 **Comando de Diagnóstico**

```bash
# En Serial Monitor ESP32:
status    # Estado completo del sistema
red       # Información detallada de red  
help      # Lista de comandos disponibles
udptest   # Prueba comunicación UDP
```

## 🎯 **Configuración de Red Típica**

```
Router WiFi: "TU_WIFI"
├── ESP32: 192.168.43.101 (automático)
├── Teléfono Android: 192.168.43.XXX (automático)
└── PC (PyQt6): 192.168.43.XXX (automático)

Puertos UDP:
├── ESP32 escucha comandos: 4210
└── Clientes reciben datos: 4211
```

## ⏱️ **Tiempo Total de Setup**

- ⚡ **Hardware**: 5 minutos
- ⚡ **Software ESP32**: 3 minutos  
- ⚡ **Android App**: 2 minutos
- ⚡ **Configuración red**: 2 minutos
- ⚡ **Testing**: 3 minutos

**🎉 Total: ~15 minutos para sistema completo funcionando**

---

### 🆘 **Ayuda Rápida**

```bash
# ¿Problemas? Ejecutar diagnóstico:
# Serial Monitor:
status && red && help

# ¿Todo verde pero LEDs no responden?
# Verificar IP en app Android = IP ESP32 del Serial Monitor

# ¿Sensores no se ven en app?  
# Verificar phoneIP en main.ino = IP real del teléfono Android
```

**📞 Soporte**: [GitHub Issues](https://github.com/DanielAraqueStudios/lab-udp-micros/issues)