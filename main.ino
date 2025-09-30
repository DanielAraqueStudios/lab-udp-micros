#include <WiFi.h>
#include <WiFiUdp.h>
#include <DHT.h>
#include <ArduinoJson.h>

// 🔹 Configura tu red WiFi
const char* ssid = "Velasco";
const char* password = "txcb1540";

// 🔹 Configuración UDP (IPs en red 10.175.23.x)
WiFiUDP udp;
const int localUdpPort = 4210;  // Puerto local para escuchar comandos
IPAddress phoneIP(10, 175, 23, 159);  // IP del teléfono Android (CORREGIDA)
const int phoneUdpPort = 4211;  // Puerto del teléfono para recibir datos
char incomingPacket[255];  // Buffer para paquetes entrantes

// 🔹 Configuración sensores
#define DHT_PIN 4
#define DHT_TYPE DHT11
#define LDR_PIN 3  // Pin analógico para LDR
DHT dht(DHT_PIN, DHT_TYPE);

// 🔹 Pines de los LEDs (actuadores)
const int led1 = 5;
const int led2 = 18;
const int led3 = 2;   // Cambiado de 36 (input-only) a 2 (válido output)
const int led4 = 21;

// 🔹 Estados de los LEDs (actuadores)
bool estado1 = false;
bool estado2 = false;
bool estado3 = false;
bool estado4 = false;

// 🔹 Variables para control de tiempo
unsigned long ultimoEnvio = 0;
const unsigned long intervaloEnvio = 250;  // 4Hz = 250ms entre envíos

// 🔹 Variables para datos de sensores
float temperatura = 0.0;
float humedad = 0.0;
int luminosidad = 0;
bool errorDHT = false;
bool wifiConectado = true;

// 🔹 Variables para logging y estadísticas
unsigned long contadorEnvios = 0;
unsigned long contadorComandos = 0;
unsigned long ultimaActualizacion = 0;
unsigned long tiempoInicioSistema = 0;

// ===============================
//  Funciones de logging
// ===============================
void mostrarEstadoSistema() {
  Serial.println("\n═══════════════════════════════════════");
  Serial.println("📊 ESTADO ACTUAL DEL SISTEMA");
  Serial.println("═══════════════════════════════════════");
  
  // Tiempo de funcionamiento
  unsigned long tiempoFuncionamiento = (millis() - tiempoInicioSistema) / 1000;
  Serial.println("⏱️  Tiempo funcionamiento: " + String(tiempoFuncionamiento) + " segundos");
  
  // Estado de red
  Serial.println("🌐 RED:");
  Serial.println("   📶 WiFi: " + String(wifiConectado ? "CONECTADO" : "DESCONECTADO"));
  Serial.println("   🏠 IP ESP32: " + WiFi.localIP().toString());
  Serial.println("   📱 IP Teléfono: " + phoneIP.toString());
  Serial.println("   🔧 RSSI: " + String(WiFi.RSSI()) + " dBm");
  
  // Estado de sensores
  Serial.println("🌡️  SENSORES:");
  Serial.println("   🌡️  Temperatura: " + String(temperatura, 1) + "°C");
  Serial.println("   💧 Humedad: " + String(humedad, 1) + "%");
  Serial.println("   ☀️  Luminosidad: " + String(luminosidad) + "%");
  Serial.println("   ⚠️  Error DHT11: " + String(errorDHT ? "SÍ" : "NO"));
  
  // Estado de actuadores
  Serial.println("💡 ACTUADORES:");
  Serial.println("   LED 1 (GPIO 5): " + String(estado1 ? "🟢 ON" : "🔴 OFF"));
  Serial.println("   LED 2 (GPIO 18): " + String(estado2 ? "🟢 ON" : "🔴 OFF"));
  Serial.println("   LED 3 (GPIO 2): " + String(estado3 ? "🟢 ON" : "🔴 OFF"));
  Serial.println("   LED 4 (GPIO 21): " + String(estado4 ? "🟢 ON" : "🔴 OFF"));
  
  // Estadísticas de comunicación
  Serial.println("📊 ESTADÍSTICAS UDP:");
  Serial.println("   📤 Mensajes enviados: " + String(contadorEnvios));
  Serial.println("   📥 Comandos recibidos: " + String(contadorComandos));
  Serial.println("   🚀 Frecuencia: 4Hz (cada 250ms)");
  
  Serial.println("═══════════════════════════════════════\n");
}

void mostrarInformacionRed() {
  Serial.println("🌐 INFORMACIÓN DE RED DETALLADA:");
  Serial.println("   📡 SSID: " + String(ssid));
  Serial.println("   🔐 Estado: " + String(WiFi.status() == WL_CONNECTED ? "CONECTADO" : "DESCONECTADO"));
  Serial.println("   🏠 IP Local: " + WiFi.localIP().toString());
  Serial.println("   🌍 Gateway: " + WiFi.gatewayIP().toString());
  Serial.println("   🏢 Subnet: " + WiFi.subnetMask().toString());
  Serial.println("   📶 RSSI: " + String(WiFi.RSSI()) + " dBm");
  Serial.println("   📋 MAC: " + WiFi.macAddress());
  Serial.println("   📱 Teléfono destino: " + phoneIP.toString());
  Serial.println("   🔌 Puerto local (escucha): " + String(localUdpPort));
  Serial.println("   📤 Puerto remoto (envío): " + String(phoneUdpPort));
}

// ===============================
//  Funciones para sensores
// ===============================
void leerSensores() {
  Serial.print("🔄 Leyendo sensores... ");
  
  // Leer DHT11
  float tempTemp = dht.readTemperature();
  float tempHum = dht.readHumidity();
  
  if (isnan(tempTemp) || isnan(tempHum)) {
    errorDHT = true;
    Serial.println("❌ Error leyendo DHT11");
  } else {
    errorDHT = false;
    temperatura = tempTemp;
    humedad = tempHum;
    Serial.print("🌡️ " + String(temperatura, 1) + "°C, 💧 " + String(humedad, 1) + "% ");
  }
  
  // Leer LDR (0-4095 en ESP32, convertir a 0-100%)
  int valorLDR = analogRead(LDR_PIN);
  luminosidad = map(valorLDR, 0, 4095, 0, 100);
  
  Serial.println("☀️ " + String(luminosidad) + "% (raw:" + String(valorLDR) + ")");
}

// ===============================
//  Funciones UDP
// ===============================
void enviarDatosSensores() {
  contadorEnvios++;
  
  // Crear mensaje en formato texto simple separado por ";"
  // Formato: temp;hum;luz;led1;led2;led3;led4;error_dht;wifi_ok
  String mensaje = String(temperatura, 1) + ";" +           // Temperatura con 1 decimal
                   String(humedad, 1) + ";" +               // Humedad con 1 decimal
                   String(luminosidad) + ";" +              // Luminosidad entero
                   String(estado1 ? "1" : "0") + ";" +      // LED 1 (1=ON, 0=OFF)
                   String(estado2 ? "1" : "0") + ";" +      // LED 2
                   String(estado3 ? "1" : "0") + ";" +      // LED 3
                   String(estado4 ? "1" : "0") + ";" +      // LED 4
                   String(errorDHT ? "1" : "0") + ";" +     // Error DHT (1=error, 0=ok)
                   String(wifiConectado ? "1" : "0");       // WiFi (1=conectado, 0=desconectado)
  
  // Enviar UDP
  udp.beginPacket(phoneIP, phoneUdpPort);
  udp.print(mensaje);
  udp.endPacket();
  
  Serial.println("📤 [" + String(contadorEnvios) + "] UDP → " + phoneIP.toString() + ":" + String(phoneUdpPort));
  Serial.println("   📋 TEXTO: " + mensaje);
  Serial.println("   📊 Formato: temp;hum;luz;led1;led2;led3;led4;error_dht;wifi_ok");
  Serial.println("   ⏱️  Timestamp: " + String(millis()));
}

void procesarComandoUDP() {
  int packetSize = udp.parsePacket();
  if (packetSize) {
    contadorComandos++;
    
    int len = udp.read(incomingPacket, 255);
    if (len > 0) {
      incomingPacket[len] = 0;
    }
    
    Serial.println("\n📥 [" + String(contadorComandos) + "] COMANDO RECIBIDO:");
    Serial.println("   🌐 Desde: " + udp.remoteIP().toString() + ":" + String(udp.remotePort()));
    Serial.println("   📦 Tamaño: " + String(packetSize) + " bytes");
    Serial.println("   📋 TEXTO: " + String(incomingPacket));
    
    // Procesar comando en formato texto
    // MIT App Inventor envía comandos simples, vamos a soportar múltiples formatos
    String comando = String(incomingPacket);
    comando.trim();
    comando.toUpperCase(); // Convertir a mayúsculas para mayor compatibilidad
    
    Serial.println("   ✅ Procesando comando: '" + comando + "'");
    
    bool comandoReconocido = false;
    
    // ===== COMANDO GET_DATA =====
    if (comando == "GET_DATA" || comando == "GETDATA") {
      Serial.println("   📊 Comando GET_DATA - Enviando datos inmediatos");
      leerSensores();
      enviarDatosSensores();
      comandoReconocido = true;
    }
    
    // ===== COMANDOS DE ENCENDIDO/APAGADO INDIVIDUAL =====
    // "LED1_ON", "LED1_OFF", "1_ON", "1_OFF", etc.
    else if (comando.indexOf("LED") >= 0 || (comando.length() >= 3 && (comando.indexOf("_ON") >= 0 || comando.indexOf("_OFF") >= 0))) {
      if (comando.startsWith("LED1") || comando.startsWith("1")) {
        bool encender = comando.indexOf("ON") >= 0 || comando.indexOf("1") >= 0;
        if (estado1 != encender) {
          estado1 = encender;
          digitalWrite(led1, estado1 ? HIGH : LOW);
          Serial.println("   💡 LED 1 (GPIO 5): " + String(estado1 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED2") || comando.startsWith("2")) {
        bool encender = comando.indexOf("ON") >= 0 || comando.indexOf("1") >= 0;
        if (estado2 != encender) {
          estado2 = encender;
          digitalWrite(led2, estado2 ? HIGH : LOW);
          Serial.println("   💡 LED 2 (GPIO 18): " + String(estado2 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED3") || comando.startsWith("3")) {
        bool encender = comando.indexOf("ON") >= 0 || comando.indexOf("1") >= 0;
        if (estado3 != encender) {
          estado3 = encender;
          digitalWrite(led3, estado3 ? HIGH : LOW);
          Serial.println("   💡 LED 3 (GPIO 2): " + String(estado3 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED4") || comando.startsWith("4")) {
        bool encender = comando.indexOf("ON") >= 0 || comando.indexOf("1") >= 0;
        if (estado4 != encender) {
          estado4 = encender;
          digitalWrite(led4, estado4 ? HIGH : LOW);
          Serial.println("   � LED 4 (GPIO 21): " + String(estado4 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
    }
    
    // ===== COMANDOS SIMPLES DE NÚMEROS =====
    // "1", "2", "3", "4" = Encender LED correspondiente
    // "0" = Apagar todos
    else if (comando.length() == 1 && comando >= "0" && comando <= "4") {
      if (comando == "0") {
        // Apagar todos los LEDs
        if (estado1) { estado1 = false; digitalWrite(led1, LOW); Serial.println("   💡 LED 1 (GPIO 5): 🔴 APAGADO"); }
        if (estado2) { estado2 = false; digitalWrite(led2, LOW); Serial.println("   💡 LED 2 (GPIO 18): 🔴 APAGADO"); }
        if (estado3) { estado3 = false; digitalWrite(led3, LOW); Serial.println("   💡 LED 3 (GPIO 2): 🔴 APAGADO"); }
        if (estado4) { estado4 = false; digitalWrite(led4, LOW); Serial.println("   💡 LED 4 (GPIO 21): 🔴 APAGADO"); }
        comandoReconocido = true;
      }
      else if (comando == "1") {
        estado1 = !estado1; // Toggle
        digitalWrite(led1, estado1 ? HIGH : LOW);
        Serial.println("   💡 LED 1 (GPIO 5): " + String(estado1 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        comandoReconocido = true;
      }
      else if (comando == "2") {
        estado2 = !estado2; // Toggle
        digitalWrite(led2, estado2 ? HIGH : LOW);
        Serial.println("   💡 LED 2 (GPIO 18): " + String(estado2 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        comandoReconocido = true;
      }
      else if (comando == "3") {
        estado3 = !estado3; // Toggle
        digitalWrite(led3, estado3 ? HIGH : LOW);
        Serial.println("   💡 LED 3 (GPIO 2): " + String(estado3 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        comandoReconocido = true;
      }
      else if (comando == "4") {
        estado4 = !estado4; // Toggle
        digitalWrite(led4, estado4 ? HIGH : LOW);
        Serial.println("   💡 LED 4 (GPIO 21): " + String(estado4 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        comandoReconocido = true;
      }
    }
    
    // ===== FORMATO LEDx:estado (formato original) =====
    else if (comando.indexOf(":") >= 0) {
      if (comando.startsWith("LED1:")) {
        bool nuevoEstado = comando.substring(5).toInt() == 1;
        if (estado1 != nuevoEstado) {
          estado1 = nuevoEstado;
          digitalWrite(led1, estado1 ? HIGH : LOW);
          Serial.println("   💡 LED 1 (GPIO 5): " + String(estado1 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED2:")) {
        bool nuevoEstado = comando.substring(5).toInt() == 1;
        if (estado2 != nuevoEstado) {
          estado2 = nuevoEstado;
          digitalWrite(led2, estado2 ? HIGH : LOW);
          Serial.println("   💡 LED 2 (GPIO 18): " + String(estado2 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED3:")) {
        bool nuevoEstado = comando.substring(5).toInt() == 1;
        if (estado3 != nuevoEstado) {
          estado3 = nuevoEstado;
          digitalWrite(led3, estado3 ? HIGH : LOW);
          Serial.println("   💡 LED 3 (GPIO 2): " + String(estado3 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
      else if (comando.startsWith("LED4:")) {
        bool nuevoEstado = comando.substring(5).toInt() == 1;
        if (estado4 != nuevoEstado) {
          estado4 = nuevoEstado;
          digitalWrite(led4, estado4 ? HIGH : LOW);
          Serial.println("   💡 LED 4 (GPIO 21): " + String(estado4 ? "🟢 ENCENDIDO" : "🔴 APAGADO"));
        }
        comandoReconocido = true;
      }
    }
    
    // ===== FORMATO 1;0;1;0 (estados completos) =====
    else if (comando.indexOf(";") >= 0) {
      Serial.println("   🔄 Procesando estados completos: " + comando);
      // Dividir por ";"
      int pos1 = comando.indexOf(';');
      int pos2 = comando.indexOf(';', pos1 + 1);
      int pos3 = comando.indexOf(';', pos2 + 1);
      
      if (pos1 > 0 && pos2 > pos1 && pos3 > pos2) {
        bool nuevoEstado1 = comando.substring(0, pos1).toInt() == 1;
        bool nuevoEstado2 = comando.substring(pos1 + 1, pos2).toInt() == 1;
        bool nuevoEstado3 = comando.substring(pos2 + 1, pos3).toInt() == 1;
        bool nuevoEstado4 = comando.substring(pos3 + 1).toInt() == 1;
        
        if (estado1 != nuevoEstado1) { estado1 = nuevoEstado1; digitalWrite(led1, estado1 ? HIGH : LOW); Serial.println("   💡 LED 1 (GPIO 5): " + String(estado1 ? "🟢 ENCENDIDO" : "🔴 APAGADO")); }
        if (estado2 != nuevoEstado2) { estado2 = nuevoEstado2; digitalWrite(led2, estado2 ? HIGH : LOW); Serial.println("   💡 LED 2 (GPIO 18): " + String(estado2 ? "🟢 ENCENDIDO" : "🔴 APAGADO")); }
        if (estado3 != nuevoEstado3) { estado3 = nuevoEstado3; digitalWrite(led3, estado3 ? HIGH : LOW); Serial.println("   💡 LED 3 (GPIO 2): " + String(estado3 ? "🟢 ENCENDIDO" : "🔴 APAGADO")); }
        if (estado4 != nuevoEstado4) { estado4 = nuevoEstado4; digitalWrite(led4, estado4 ? HIGH : LOW); Serial.println("   💡 LED 4 (GPIO 21): " + String(estado4 ? "🟢 ENCENDIDO" : "🔴 APAGADO")); }
        
        comandoReconocido = true;
      }
    }
    
    // ===== MENSAJE DE ERROR SI NO SE RECONOCE =====
    if (!comandoReconocido) {
      Serial.println("   ❌ Comando no reconocido: '" + comando + "'");
      Serial.println("   📋 Formatos válidos para MIT App Inventor:");
      Serial.println("      '1', '2', '3', '4' (toggle LED correspondiente)");
      Serial.println("      '0' (apagar todos los LEDs)");
      Serial.println("      'LED1_ON', 'LED2_OFF' (encender/apagar específico)");
      Serial.println("      'LED1:1', 'LED2:0' (formato clásico)");
      Serial.println("      '1;0;1;0' (todos los estados a la vez)");
      Serial.println("      'GET_DATA' (solicitar datos de sensores)");
    }
    
    Serial.println("   ✅ Comando procesado\n");
  }
}

void verificarConexionWiFi() {
  static bool estadoAnterior = true;
  static unsigned long ultimaVerificacion = 0;
  
  // Verificar cada 5 segundos
  if (millis() - ultimaVerificacion > 5000) {
    bool estadoActual = (WiFi.status() == WL_CONNECTED);
    
    if (estadoActual != estadoAnterior) {
      if (!estadoActual) {
        wifiConectado = false;
        Serial.println("\n❌ WIFI DESCONECTADO - Intentando reconectar...");
        Serial.println("   🔄 Estado: " + String(WiFi.status()));
        WiFi.begin(ssid, password);
      } else {
        wifiConectado = true;
        Serial.println("\n✅ WIFI RECONECTADO!");
        mostrarInformacionRed();
      }
      estadoAnterior = estadoActual;
    }
    
    wifiConectado = estadoActual;
    ultimaVerificacion = millis();
  }
}

// ===============================
//  Setup
// ===============================
void setup() {
  Serial.begin(115200);
  delay(1000); // Esperar estabilización del serial
  
  Serial.println("\n\n🚀🚀🚀 ESP32 UDP MICROCONTROLLER LAB 🚀🚀🚀");
  Serial.println("════════════════════════════════════════════════");
  Serial.println("📅 Fecha de compilación: " + String(__DATE__) + " " + String(__TIME__));
  Serial.println("🔧 Versión ESP32: " + String(ESP.getChipModel()));
  Serial.println("💾 Memoria libre: " + String(ESP.getFreeHeap()) + " bytes");
  Serial.println("⚡ Frecuencia CPU: " + String(ESP.getCpuFreqMHz()) + " MHz");
  Serial.println("════════════════════════════════════════════════\n");
  
  tiempoInicioSistema = millis();

  Serial.println("🔧 CONFIGURANDO HARDWARE...");
  
  // Configurar pines como salida (actuadores)
  pinMode(led1, OUTPUT);
  pinMode(led2, OUTPUT);
  pinMode(led3, OUTPUT);
  pinMode(led4, OUTPUT);
  Serial.println("   💡 LEDs configurados en GPIO: 5, 18, 2, 21");
  
  // Configurar pin analógico para LDR
  pinMode(LDR_PIN, INPUT);
  Serial.println("   ☀️  LDR configurado en GPIO: " + String(LDR_PIN));
  
  // Inicializar sensor DHT11
  dht.begin();
  Serial.println("   🌡️  DHT11 inicializado en GPIO: " + String(DHT_PIN));
  
  Serial.println("✅ Hardware configurado correctamente\n");

  // Conectar al WiFi
  Serial.println("🌐 CONECTANDO A WIFI...");
  Serial.println("   📡 SSID: " + String(ssid));
  Serial.print("   � Conectando");
  
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\n✅ CONECTADO AL WIFI EXITOSAMENTE!");
  mostrarInformacionRed();

  // Inicializar UDP
  Serial.println("\n📡 CONFIGURANDO SERVIDOR UDP...");
  udp.begin(localUdpPort);
  Serial.println("   ✅ Servidor UDP iniciado en puerto " + String(localUdpPort));
  Serial.println("   📤 Enviando datos a " + phoneIP.toString() + ":" + String(phoneUdpPort));
  Serial.println("   ⚡ Frecuencia de envío: 4Hz (cada 250ms)");
  Serial.println("   📋 Formato de mensaje: TEXTO separado por ';'");
  
  Serial.println("\n🎯 SISTEMA COMPLETAMENTE OPERATIVO!");
  Serial.println("═══════════════════════════════════════════════");
  Serial.println("💡 Monitoreando sensores y escuchando comandos...");
  Serial.println("📺 Para ver estado completo, escriba 'status' en monitor serial");
  Serial.println("═══════════════════════════════════════════════\n");
  
  // Mostrar estado inicial
  mostrarEstadoSistema();
}

// ===============================
//  Loop Principal
// ===============================
void loop() {
  // Verificar conexión WiFi periódicamente
  verificarConexionWiFi();
  
  // Escuchar comandos UDP entrantes
  procesarComandoUDP();
  
  // Enviar datos de sensores a 4Hz (cada 250ms)
  unsigned long tiempoActual = millis();
  if (tiempoActual - ultimoEnvio >= intervaloEnvio) {
    leerSensores();
    enviarDatosSensores();
    ultimoEnvio = tiempoActual;
  }
  
  // Mostrar estado completo cada 30 segundos
  if (tiempoActual - ultimaActualizacion >= 30000) {
    mostrarEstadoSistema();
    ultimaActualizacion = tiempoActual;
  }
  
  // Verificar si hay comandos en Serial Monitor
  if (Serial.available()) {
    String comando = Serial.readString();
    comando.trim();
    if (comando == "status" || comando == "estado") {
      mostrarEstadoSistema();
    } else if (comando == "red" || comando == "network") {
      mostrarInformacionRed();
    } else if (comando == "help" || comando == "ayuda") {
      Serial.println("📚 COMANDOS DISPONIBLES:");
      Serial.println("   'status' o 'estado' - Mostrar estado completo del sistema");
      Serial.println("   'red' o 'network' - Mostrar información detallada de red");
      Serial.println("   'help' o 'ayuda' - Mostrar esta ayuda");
    }
  }
  
  // Pequeña pausa para evitar saturar el procesador
  delay(10);
}