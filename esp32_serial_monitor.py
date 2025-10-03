"""
ESP32 UDP Lab - Serial Monitor
Interfaz moderna en PyQt6 para monitorear el ESP32 UDP Lab
Autor: Daniel Araque Studios
"""

import sys
import serial
import serial.tools.list_ports
import json
import re
from datetime import datetime
from PyQt6.QtWidgets import (QApplication, QMainWindow, QWidget, QVBoxLayout, 
                            QHBoxLayout, QGridLayout, QLabel, QPushButton, 
                            QComboBox, QTextEdit, QGroupBox, QFrame, QSplitter,
                            QStatusBar, QMenuBar, QMenu, QScrollArea, QProgressBar)
from PyQt6.QtCore import QThread, pyqtSignal, QTimer, Qt, QPropertyAnimation, QEasingCurve
from PyQt6.QtGui import QFont, QPixmap, QIcon, QPalette, QColor, QAction
import pyqtgraph as pg


class SerialThread(QThread):
    """Hilo para manejar la comunicación serial sin bloquear la UI"""
    data_received = pyqtSignal(str)
    connection_status = pyqtSignal(bool)
    
    def __init__(self):
        super().__init__()
        self.serial_port = None
        self.is_running = False
        
    def connect_serial(self, port, baudrate=115200):
        """Conectar al puerto serial"""
        try:
            if self.serial_port and self.serial_port.is_open:
                self.serial_port.close()
                
            self.serial_port = serial.Serial(port, baudrate, timeout=1)
            self.is_running = True
            self.connection_status.emit(True)
            return True
        except Exception as e:
            self.connection_status.emit(False)
            return False
    
    def disconnect_serial(self):
        """Desconectar del puerto serial"""
        self.is_running = False
        if self.serial_port and self.serial_port.is_open:
            self.serial_port.close()
        self.connection_status.emit(False)
    
    def send_command(self, command):
        """Enviar comando al ESP32"""
        try:
            if self.serial_port and self.serial_port.is_open:
                self.serial_port.write(f"{command}\n".encode('utf-8'))
                return True
        except Exception as e:
            return False
        return False
    
    def run(self):
        """Ejecutar el hilo de lectura serial"""
        while self.is_running:
            try:
                if self.serial_port and self.serial_port.is_open:
                    if self.serial_port.in_waiting > 0:
                        data = self.serial_port.readline().decode('utf-8', errors='ignore').strip()
                        if data:
                            self.data_received.emit(data)
                self.msleep(10)  # Pausa pequeña para evitar saturar CPU
            except Exception as e:
                self.connection_status.emit(False)
                break


class SensorCard(QFrame):
    """Widget personalizado para mostrar datos de sensores"""
    
    def __init__(self, title, unit, icon="", color="#3498db"):
        super().__init__()
        self.setFrameStyle(QFrame.Shape.Box)
        self.setStyleSheet(f"""
            QFrame {{
                background-color: rgba(255, 255, 255, 0.05);
                border: 2px solid {color};
                border-radius: 15px;
                margin: 5px;
                padding: 10px;
            }}
        """)
        
        layout = QVBoxLayout()
        
        # Título con icono
        title_layout = QHBoxLayout()
        icon_label = QLabel(icon)
        icon_label.setStyleSheet("font-size: 24px;")
        title_label = QLabel(title)
        title_label.setStyleSheet("font-weight: bold; color: #ecf0f1; font-size: 14px;")
        
        title_layout.addWidget(icon_label)
        title_layout.addWidget(title_label)
        title_layout.addStretch()
        
        # Valor principal
        self.value_label = QLabel("--")
        self.value_label.setStyleSheet(f"""
            font-size: 28px; 
            font-weight: bold; 
            color: {color};
            margin: 10px 0;
        """)
        self.value_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Unidad
        unit_label = QLabel(unit)
        unit_label.setStyleSheet("color: #bdc3c7; font-size: 12px;")
        unit_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        layout.addLayout(title_layout)
        layout.addWidget(self.value_label)
        layout.addWidget(unit_label)
        
        self.setLayout(layout)
        self.setFixedHeight(120)
    
    def update_value(self, value, status="normal"):
        """Actualizar el valor mostrado"""
        self.value_label.setText(str(value))
        
        # Cambiar color según estado
        colors = {
            "normal": "#3498db",
            "warning": "#f39c12",
            "error": "#e74c3c",
            "success": "#27ae60"
        }
        
        color = colors.get(status, "#3498db")
        self.value_label.setStyleSheet(f"""
            font-size: 28px; 
            font-weight: bold; 
            color: {color};
            margin: 10px 0;
        """)


class LEDControl(QFrame):
    """Widget para controlar y mostrar estado de LEDs"""
    
    def __init__(self, led_number, gpio_pin, serial_thread=None):
        super().__init__()
        self.led_number = led_number
        self.gpio_pin = gpio_pin
        self.is_on = False
        self.serial_thread = serial_thread
        
        self.setFrameStyle(QFrame.Shape.Box)
        self.setStyleSheet("""
            QFrame {
                background-color: rgba(255, 255, 255, 0.05);
                border: 2px solid #34495e;
                border-radius: 10px;
                margin: 5px;
                padding: 10px;
            }
        """)
        
        layout = QVBoxLayout()
        
        # Título
        title = QLabel(f"LED {led_number}")
        title.setStyleSheet("font-weight: bold; color: #ecf0f1; font-size: 14px;")
        title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Pin info
        pin_info = QLabel(f"GPIO {gpio_pin}")
        pin_info.setStyleSheet("color: #bdc3c7; font-size: 10px;")
        pin_info.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # LED visual
        self.led_visual = QLabel("⚫")
        self.led_visual.setStyleSheet("font-size: 30px;")
        self.led_visual.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Botón de control
        self.control_btn = QPushButton("OFF")
        self.control_btn.setStyleSheet("""
            QPushButton {
                background-color: #e74c3c;
                color: white;
                border: none;
                border-radius: 5px;
                padding: 5px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #c0392b;
            }
        """)
        self.control_btn.clicked.connect(self.toggle_led)
        
        # Estado
        self.status_label = QLabel("APAGADO")
        self.status_label.setStyleSheet("color: #e74c3c; font-size: 10px;")
        self.status_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        layout.addWidget(title)
        layout.addWidget(pin_info)
        layout.addWidget(self.led_visual)
        layout.addWidget(self.control_btn)
        layout.addWidget(self.status_label)
        
        self.setLayout(layout)
        self.setFixedWidth(120)
        self.setFixedHeight(160)
    
    def toggle_led(self):
        """Alternar estado del LED"""
        if self.serial_thread:
            command = f"test{self.led_number}"
            if self.serial_thread.send_command(command):
                self.is_on = not self.is_on
                self.update_visual()
    
    def set_state(self, state):
        """Establecer estado del LED desde datos recibidos"""
        self.is_on = state
        self.update_visual()
    
    def update_visual(self):
        """Actualizar visualización del LED"""
        if self.is_on:
            self.led_visual.setText("🟢")
            self.control_btn.setText("ON")
            self.control_btn.setStyleSheet("""
                QPushButton {
                    background-color: #27ae60;
                    color: white;
                    border: none;
                    border-radius: 5px;
                    padding: 5px;
                    font-weight: bold;
                }
                QPushButton:hover {
                    background-color: #229954;
                }
            """)
            self.status_label.setText("ENCENDIDO")
            self.status_label.setStyleSheet("color: #27ae60; font-size: 10px;")
        else:
            self.led_visual.setText("⚫")
            self.control_btn.setText("OFF")
            self.control_btn.setStyleSheet("""
                QPushButton {
                    background-color: #e74c3c;
                    color: white;
                    border: none;
                    border-radius: 5px;
                    padding: 5px;
                    font-weight: bold;
                }
                QPushButton:hover {
                    background-color: #c0392b;
                }
            """)
            self.status_label.setText("APAGADO")
            self.status_label.setStyleSheet("color: #e74c3c; font-size: 10px;")


class DiagnosticPanel(QFrame):
    """Panel de diagnósticos y comandos de prueba"""
    
    def __init__(self, serial_thread=None):
        super().__init__()
        self.serial_thread = serial_thread
        
        self.setFrameStyle(QFrame.Shape.Box)
        self.setStyleSheet("""
            QFrame {
                background-color: rgba(255, 255, 255, 0.05);
                border: 2px solid #9b59b6;
                border-radius: 10px;
                margin: 5px;
                padding: 15px;
            }
        """)
        
        layout = QVBoxLayout()
        
        # Título
        title = QLabel("🔧 Diagnósticos y Pruebas")
        title.setStyleSheet("font-weight: bold; color: #ecf0f1; font-size: 16px; margin-bottom: 10px;")
        
        # Información de conexión
        self.connection_info = QLabel("📡 Estado: Desconectado")
        self.connection_info.setStyleSheet("color: #e74c3c; font-size: 12px;")
        
        # Estadísticas UDP
        self.udp_stats = QLabel("📊 Mensajes UDP: 0 enviados, 0 recibidos")
        self.udp_stats.setStyleSheet("color: #3498db; font-size: 12px;")
        
        # IP Information
        self.ip_info = QLabel("🌐 ESP32: --.--.--.-- | Teléfono: --.--.--.--")
        self.ip_info.setStyleSheet("color: #f39c12; font-size: 12px;")
        
        # Botones de prueba
        test_layout = QGridLayout()
        
        # Botones individuales
        self.test_buttons = []
        for i in range(1, 5):
            btn = QPushButton(f"Test LED {i}")
            btn.setStyleSheet("""
                QPushButton {
                    background-color: #3498db;
                    color: white;
                    border: none;
                    border-radius: 5px;
                    padding: 8px;
                    font-weight: bold;
                }
                QPushButton:hover {
                    background-color: #2980b9;
                }
            """)
            btn.clicked.connect(lambda checked, x=i: self.send_test_command(f"test{x}"))
            test_layout.addWidget(btn, 0, i-1)
            self.test_buttons.append(btn)
        
        # Botones globales
        all_on_btn = QPushButton("🟢 Todos ON")
        all_on_btn.setStyleSheet("""
            QPushButton {
                background-color: #27ae60;
                color: white;
                border: none;
                border-radius: 5px;
                padding: 8px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #229954;
            }
        """)
        all_on_btn.clicked.connect(lambda: self.send_test_command("allon"))
        
        all_off_btn = QPushButton("🔴 Todos OFF")
        all_off_btn.setStyleSheet("""
            QPushButton {
                background-color: #e74c3c;
                color: white;
                border: none;
                border-radius: 5px;
                padding: 8px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #c0392b;
            }
        """)
        all_off_btn.clicked.connect(lambda: self.send_test_command("alloff"))
        
        status_btn = QPushButton("📊 Estado Sistema")
        status_btn.setStyleSheet("""
            QPushButton {
                background-color: #9b59b6;
                color: white;
                border: none;
                border-radius: 5px;
                padding: 8px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #8e44ad;
            }
        """)
        status_btn.clicked.connect(lambda: self.send_test_command("status"))
        
        test_layout.addWidget(all_on_btn, 1, 0)
        test_layout.addWidget(all_off_btn, 1, 1)
        test_layout.addWidget(status_btn, 1, 2, 1, 2)
        
        layout.addWidget(title)
        layout.addWidget(self.connection_info)
        layout.addWidget(self.udp_stats)
        layout.addWidget(self.ip_info)
        layout.addLayout(test_layout)
        
        self.setLayout(layout)
    
    def send_test_command(self, command):
        """Enviar comando de prueba"""
        if self.serial_thread:
            success = self.serial_thread.send_command(command)
            if success:
                print(f"📤 Comando enviado: {command}")
            else:
                print(f"❌ Error enviando comando: {command}")
    
    def update_connection_status(self, connected, esp32_ip="", phone_ip=""):
        """Actualizar estado de conexión"""
        if connected:
            self.connection_info.setText("📡 Estado: ✅ Conectado")
            self.connection_info.setStyleSheet("color: #27ae60; font-size: 12px;")
            if esp32_ip and phone_ip:
                self.ip_info.setText(f"🌐 ESP32: {esp32_ip} | Teléfono: {phone_ip}")
        else:
            self.connection_info.setText("📡 Estado: ❌ Desconectado")
            self.connection_info.setStyleSheet("color: #e74c3c; font-size: 12px;")
            self.ip_info.setText("🌐 ESP32: --.--.--.-- | Teléfono: --.--.--.--")
    
    def update_udp_stats(self, sent=0, received=0):
        """Actualizar estadísticas UDP"""
        self.udp_stats.setText(f"📊 Mensajes UDP: {sent} enviados, {received} recibidos")


class NetworkInfo(QFrame):
    """Panel de información de red"""
    
    def __init__(self):
        super().__init__()
        
        self.setFrameStyle(QFrame.Shape.Box)
        self.setStyleSheet("""
            QFrame {
                background-color: rgba(255, 255, 255, 0.05);
                border: 2px solid #f39c12;
                border-radius: 10px;
                margin: 5px;
                padding: 15px;
            }
        """)
        
        layout = QVBoxLayout()
        
        # Título
        title = QLabel("🌐 Información de Red")
        title.setStyleSheet("font-weight: bold; color: #ecf0f1; font-size: 16px; margin-bottom: 10px;")
        
        # WiFi Info
        self.wifi_ssid = QLabel("📡 SSID: --")
        self.wifi_ssid.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.wifi_status = QLabel("🔐 Estado: --")
        self.wifi_status.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.esp32_ip = QLabel("🏠 IP ESP32: --")
        self.esp32_ip.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.gateway_ip = QLabel("🌍 Gateway: --")
        self.gateway_ip.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.phone_ip = QLabel("📱 IP Teléfono: --")
        self.phone_ip.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.rssi = QLabel("📶 RSSI: -- dBm")
        self.rssi.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        self.ports = QLabel("🔌 Puertos: Local -- | Remoto --")
        self.ports.setStyleSheet("color: #ecf0f1; font-size: 12px;")
        
        layout.addWidget(title)
        layout.addWidget(self.wifi_ssid)
        layout.addWidget(self.wifi_status)
        layout.addWidget(self.esp32_ip)
        layout.addWidget(self.gateway_ip)
        layout.addWidget(self.phone_ip)
        layout.addWidget(self.rssi)
        layout.addWidget(self.ports)
        
        self.setLayout(layout)
    
    def update_network_info(self, data):
        """Actualizar información de red"""
        if "SSID:" in data:
            ssid = data.split("SSID:")[1].strip()
            self.wifi_ssid.setText(f"📡 SSID: {ssid}")
        
        if "IP Local:" in data:
            ip = data.split("IP Local:")[1].strip()
            self.esp32_ip.setText(f"🏠 IP ESP32: {ip}")
        
        if "Gateway:" in data:
            gateway = data.split("Gateway:")[1].strip()
            self.gateway_ip.setText(f"🌍 Gateway: {gateway}")
        
        if "Teléfono destino:" in data:
            phone = data.split("Teléfono destino:")[1].strip()
            self.phone_ip.setText(f"📱 IP Teléfono: {phone}")
        
        if "RSSI:" in data:
            rssi_value = data.split("RSSI:")[1].strip()
            self.rssi.setText(f"📶 RSSI: {rssi_value}")
            
            # Cambiar color según intensidad de señal
            try:
                rssi_num = int(rssi_value.split()[0])
                if rssi_num > -50:
                    color = "#27ae60"  # Excelente
                elif rssi_num > -70:
                    color = "#f39c12"  # Buena
                else:
                    color = "#e74c3c"  # Débil
                self.rssi.setStyleSheet(f"color: {color}; font-size: 12px;")
            except:
                pass
        
        if "Puerto local (escucha):" in data and "Puerto remoto (envío):" in data:
            local_port = data.split("Puerto local (escucha):")[1].split()[0]
            remote_port = data.split("Puerto remoto (envío):")[1].split()[0]
            self.ports.setText(f"🔌 Puertos: Local {local_port} | Remoto {remote_port}")


class ESP32Monitor(QMainWindow):
            QFrame {
                background-color: rgba(255, 255, 255, 0.05);
                border: 2px solid #95a5a6;
                border-radius: 10px;
                margin: 3px;
                padding: 8px;
            }
        """)
        
        layout = QVBoxLayout()
        
        # Título
        title = QLabel(f"LED {led_number}")
        title.setStyleSheet("font-weight: bold; color: #ecf0f1; font-size: 12px;")
        title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # GPIO Info
        gpio_info = QLabel(f"GPIO {gpio_pin}")
        gpio_info.setStyleSheet("color: #95a5a6; font-size: 10px;")
        gpio_info.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Indicador visual
        self.indicator = QLabel("●")
        self.indicator.setStyleSheet("font-size: 24px; color: #7f8c8d;")
        self.indicator.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Estado texto
        self.status_label = QLabel("OFF")
        self.status_label.setStyleSheet("color: #95a5a6; font-size: 10px; font-weight: bold;")
        self.status_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        layout.addWidget(title)
        layout.addWidget(gpio_info)
        layout.addWidget(self.indicator)
        layout.addWidget(self.status_label)
        
        self.setLayout(layout)
        self.setFixedHeight(100)
    
    def update_status(self, is_on):
        """Actualizar estado del LED"""
        self.is_on = is_on
        
        if is_on:
            self.indicator.setStyleSheet("font-size: 24px; color: #27ae60;")
            self.status_label.setText("ON")
            self.status_label.setStyleSheet("color: #27ae60; font-size: 10px; font-weight: bold;")
            self.setStyleSheet("""
                QFrame {
                    background-color: rgba(39, 174, 96, 0.1);
                    border: 2px solid #27ae60;
                    border-radius: 10px;
                    margin: 3px;
                    padding: 8px;
                }
            """)
        else:
            self.indicator.setStyleSheet("font-size: 24px; color: #7f8c8d;")
            self.status_label.setText("OFF")
            self.status_label.setStyleSheet("color: #95a5a6; font-size: 10px; font-weight: bold;")
            self.setStyleSheet("""
                QFrame {
                    background-color: rgba(255, 255, 255, 0.05);
                    border: 2px solid #95a5a6;
                    border-radius: 10px;
                    margin: 3px;
                    padding: 8px;
                }
            """)


class ESP32SerialMonitor(QMainWindow):
    """Ventana principal de la aplicación"""
    
    def __init__(self):
        super().__init__()
        self.serial_thread = SerialThread()
        self.is_dark_mode = True
        self.sensor_data = {}
        self.led_states = {}
        
        self.init_ui()
        self.setup_connections()
        self.apply_dark_theme()
        
        # Timer para actualizar información
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_ui)
        self.update_timer.start(100)  # Actualizar cada 100ms
    
    def init_ui(self):
        """Inicializar la interfaz de usuario"""
        self.setWindowTitle("ESP32 UDP Lab - Serial Monitor v1.0")
        self.setGeometry(100, 100, 1400, 900)
        
        # Widget central
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Layout principal
        main_layout = QHBoxLayout()
        
        # Panel izquierdo - Controles
        left_panel = self.create_control_panel()
        
        # Panel central - Dashboard
        center_panel = self.create_dashboard_panel()
        
        # Panel derecho - Console
        right_panel = self.create_console_panel()
        
        # Splitter para dividir paneles
        splitter = QSplitter(Qt.Orientation.Horizontal)
        splitter.addWidget(left_panel)
        splitter.addWidget(center_panel)
        splitter.addWidget(right_panel)
        splitter.setSizes([300, 600, 500])  # Tamaños proporcionales
        
        main_layout.addWidget(splitter)
        central_widget.setLayout(main_layout)
        
        # Barra de estado
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Desconectado")
        
        # Menú
        self.create_menu()
    
    def create_control_panel(self):
        """Crear panel de controles"""
        panel = QFrame()
        panel.setFrameStyle(QFrame.Shape.Box)
        layout = QVBoxLayout()
        
        # Grupo de conexión
        connection_group = QGroupBox("🔌 Conexión Serial")
        connection_layout = QVBoxLayout()
        
        # Selector de puerto COM
        port_layout = QHBoxLayout()
        port_layout.addWidget(QLabel("Puerto:"))
        self.port_combo = QComboBox()
        self.refresh_ports()
        port_layout.addWidget(self.port_combo)
        
        refresh_btn = QPushButton("🔄")
        refresh_btn.clicked.connect(self.refresh_ports)
        refresh_btn.setMaximumWidth(40)
        port_layout.addWidget(refresh_btn)
        
        # Botones de conexión
        self.connect_btn = QPushButton("🔗 Conectar")
        self.connect_btn.clicked.connect(self.toggle_connection)
        
        connection_layout.addLayout(port_layout)
        connection_layout.addWidget(self.connect_btn)
        connection_group.setLayout(connection_layout)
        
        # Grupo de información del sistema
        info_group = QGroupBox("📊 Información del Sistema")
        info_layout = QVBoxLayout()
        
        self.uptime_label = QLabel("Tiempo: --")
        self.wifi_status_label = QLabel("WiFi: --")
        self.ip_label = QLabel("IP ESP32: --")
        self.phone_ip_label = QLabel("IP Teléfono: --")
        self.messages_sent_label = QLabel("Enviados: --")
        self.messages_received_label = QLabel("Recibidos: --")
        
        for label in [self.uptime_label, self.wifi_status_label, self.ip_label, 
                     self.phone_ip_label, self.messages_sent_label, self.messages_received_label]:
            label.setStyleSheet("color: #ecf0f1; margin: 2px;")
            info_layout.addWidget(label)
        
        info_group.setLayout(info_layout)
        
        # Grupo de controles
        controls_group = QGroupBox("🎮 Controles")
        controls_layout = QVBoxLayout()
        
        clear_btn = QPushButton("🗑️ Limpiar Console")
        clear_btn.clicked.connect(self.clear_console)
        
        save_btn = QPushButton("💾 Guardar Log")
        save_btn.clicked.connect(self.save_log)
        
        theme_btn = QPushButton("🌙 Cambiar Tema")
        theme_btn.clicked.connect(self.toggle_theme)
        
        controls_layout.addWidget(clear_btn)
        controls_layout.addWidget(save_btn)
        controls_layout.addWidget(theme_btn)
        controls_group.setLayout(controls_layout)
        
        layout.addWidget(connection_group)
        layout.addWidget(info_group)
        layout.addWidget(controls_group)
        layout.addStretch()
        
        panel.setLayout(layout)
        panel.setMaximumWidth(320)
        return panel
    
    def create_dashboard_panel(self):
        """Crear panel del dashboard"""
        panel = QFrame()
        panel.setFrameStyle(QFrame.Shape.Box)
        layout = QVBoxLayout()
        
        # Título del dashboard
        title = QLabel("📊 ESP32 UDP Lab Dashboard")
        title.setStyleSheet("font-size: 18px; font-weight: bold; color: #3498db; margin: 10px;")
        title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        # Sensores
        sensors_group = QGroupBox("🌡️ Sensores")
        sensors_layout = QHBoxLayout()
        
        self.temp_card = SensorCard("Temperatura", "°C", "🌡️", "#e74c3c")
        self.humidity_card = SensorCard("Humedad", "%", "💧", "#3498db")
        self.light_card = SensorCard("Luminosidad", "%", "☀️", "#f39c12")
        
        sensors_layout.addWidget(self.temp_card)
        sensors_layout.addWidget(self.humidity_card)
        sensors_layout.addWidget(self.light_card)
        sensors_group.setLayout(sensors_layout)
        
        # LEDs
        leds_group = QGroupBox("💡 Estado de LEDs")
        leds_layout = QHBoxLayout()
        
        self.led_controls = {}
        led_pins = [5, 18, 36, 21]
        for i, pin in enumerate(led_pins, 1):
            led_control = LEDControl(i, pin)
            self.led_controls[f"led{i}"] = led_control
            leds_layout.addWidget(led_control)
        
        leds_group.setLayout(leds_layout)
        
        # Gráfico de temperatura (opcional)
        graph_group = QGroupBox("📈 Gráfico de Temperatura")
        graph_layout = QVBoxLayout()
        
        self.temperature_plot = pg.PlotWidget()
        self.temperature_plot.setBackground('transparent')
        self.temperature_plot.setLabel('left', 'Temperatura (°C)')
        self.temperature_plot.setLabel('bottom', 'Tiempo (s)')
        self.temperature_plot.showGrid(x=True, y=True, alpha=0.3)
        
        # Datos para el gráfico
        self.temp_data = []
        self.temp_times = []
        self.temp_curve = self.temperature_plot.plot(pen='r', width=2)
        
        graph_layout.addWidget(self.temperature_plot)
        graph_group.setLayout(graph_layout)
        
        layout.addWidget(title)
        layout.addWidget(sensors_group)
        layout.addWidget(leds_group)
        layout.addWidget(graph_group)
        
        panel.setLayout(layout)
        return panel
    
    def create_console_panel(self):
        """Crear panel de la consola"""
        panel = QFrame()
        panel.setFrameStyle(QFrame.Shape.Box)
        layout = QVBoxLayout()
        
        # Título
        title = QLabel("📺 Consola Serial")
        title.setStyleSheet("font-size: 16px; font-weight: bold; color: #2ecc71; margin: 5px;")
        
        # Área de texto para la consola
        self.console_text = QTextEdit()
        self.console_text.setReadOnly(True)
        self.console_text.setStyleSheet("""
            QTextEdit {
                background-color: #1e1e1e;
                color: #ffffff;
                font-family: 'Consolas', 'Monaco', monospace;
                font-size: 11px;
                border: 1px solid #555;
                border-radius: 5px;
                padding: 5px;
            }
        """)
        
        layout.addWidget(title)
        layout.addWidget(self.console_text)
        
        panel.setLayout(layout)
        panel.setMaximumWidth(520)
        return panel
    
    def create_menu(self):
        """Crear menú de la aplicación"""
        menubar = self.menuBar()
        
        # Menú Archivo
        file_menu = menubar.addMenu('📁 Archivo')
        
        save_action = QAction('💾 Guardar Log', self)
        save_action.triggered.connect(self.save_log)
        file_menu.addAction(save_action)
        
        exit_action = QAction('❌ Salir', self)
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # Menú Ver
        view_menu = menubar.addMenu('👁️ Ver')
        
        theme_action = QAction('🌙 Cambiar Tema', self)
        theme_action.triggered.connect(self.toggle_theme)
        view_menu.addAction(theme_action)
        
        clear_action = QAction('🗑️ Limpiar Consola', self)
        clear_action.triggered.connect(self.clear_console)
        view_menu.addAction(clear_action)
    
    def setup_connections(self):
        """Configurar conexiones de señales"""
        self.serial_thread.data_received.connect(self.process_serial_data)
        self.serial_thread.connection_status.connect(self.update_connection_status)
    
    def apply_dark_theme(self):
        """Aplicar tema oscuro"""
        if self.is_dark_mode:
            self.setStyleSheet("""
                QMainWindow {
                    background-color: #2c3e50;
                    color: #ecf0f1;
                }
                QGroupBox {
                    font-weight: bold;
                    border: 2px solid #34495e;
                    border-radius: 8px;
                    margin-top: 1ex;
                    padding-top: 8px;
                    background-color: rgba(52, 73, 94, 0.3);
                    color: #ecf0f1;
                }
                QGroupBox::title {
                    subcontrol-origin: margin;
                    left: 10px;
                    padding: 0 5px 0 5px;
                }
                QPushButton {
                    background-color: #3498db;
                    border: none;
                    color: white;
                    padding: 8px 16px;
                    text-align: center;
                    font-size: 12px;
                    font-weight: bold;
                    border-radius: 6px;
                    margin: 2px;
                }
                QPushButton:hover {
                    background-color: #2980b9;
                }
                QPushButton:pressed {
                    background-color: #21618c;
                }
                QComboBox {
                    background-color: #34495e;
                    border: 1px solid #555;
                    border-radius: 4px;
                    padding: 4px;
                    color: #ecf0f1;
                    font-size: 11px;
                }
                QComboBox::drop-down {
                    border: none;
                }
                QComboBox::down-arrow {
                    width: 12px;
                    height: 12px;
                }
                QLabel {
                    color: #ecf0f1;
                }
                QStatusBar {
                    background-color: #34495e;
                    color: #ecf0f1;
                    border-top: 1px solid #555;
                }
                QFrame {
                    background-color: rgba(52, 73, 94, 0.2);
                    border: 1px solid #34495e;
                    border-radius: 8px;
                    margin: 2px;
                }
            """)
    
    def refresh_ports(self):
        """Actualizar lista de puertos COM"""
        self.port_combo.clear()
        ports = serial.tools.list_ports.comports()
        for port in ports:
            self.port_combo.addItem(f"{port.device} - {port.description}")
    
    def toggle_connection(self):
        """Alternar conexión serial"""
        if not self.serial_thread.is_running:
            # Conectar
            selected_port = self.port_combo.currentText().split(" - ")[0]
            if self.serial_thread.connect_serial(selected_port):
                self.serial_thread.start()
                self.connect_btn.setText("🔌 Desconectar")
                self.connect_btn.setStyleSheet("background-color: #e74c3c;")
            else:
                self.console_text.append(f"❌ Error al conectar al puerto {selected_port}")
        else:
            # Desconectar
            self.serial_thread.disconnect_serial()
            self.connect_btn.setText("🔗 Conectar")
            self.connect_btn.setStyleSheet("background-color: #3498db;")
    
    def update_connection_status(self, connected):
        """Actualizar estado de conexión"""
        if connected:
            self.status_bar.showMessage("✅ Conectado")
            self.status_bar.setStyleSheet("background-color: #27ae60;")
        else:
            self.status_bar.showMessage("❌ Desconectado")
            self.status_bar.setStyleSheet("background-color: #e74c3c;")
    
    def process_serial_data(self, data):
        """Procesar datos recibidos del serial"""
        # Mostrar en consola con timestamp
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        self.console_text.append(f"[{timestamp}] {data}")
        
        # Autodesplazar hacia abajo
        scrollbar = self.console_text.verticalScrollBar()
        scrollbar.setValue(scrollbar.maximum())
        
        # Parsear datos específicos del ESP32
        self.parse_esp32_data(data)
    
    def parse_esp32_data(self, data):
        """Parsear datos específicos del ESP32"""
        try:
            # Buscar información de red
            if "IP ESP32:" in data:
                ip = data.split("IP ESP32:")[1].strip()
                self.ip_label.setText(f"IP ESP32: {ip}")
            
            elif "IP Teléfono:" in data:
                phone_ip = data.split("IP Teléfono:")[1].strip()
                self.phone_ip_label.setText(f"IP Teléfono: {phone_ip}")
            
            elif "WiFi:" in data and "CONECTADO" in data:
                self.wifi_status_label.setText("WiFi: ✅ CONECTADO")
            
            elif "Tiempo funcionamiento:" in data:
                time_match = re.search(r"(\d+) segundos", data)
                if time_match:
                    seconds = int(time_match.group(1))
                    hours, remainder = divmod(seconds, 3600)
                    minutes, seconds = divmod(remainder, 60)
                    self.uptime_label.setText(f"Tiempo: {hours:02d}:{minutes:02d}:{seconds:02d}")
            
            # Buscar datos de sensores
            elif "Temperatura:" in data:
                temp_match = re.search(r"Temperatura:\s*([0-9.-]+)", data)
                if temp_match:
                    temp = float(temp_match.group(1))
                    self.sensor_data['temperature'] = temp
                    self.temp_card.update_value(f"{temp:.1f}")
                    
                    # Agregar al gráfico
                    current_time = len(self.temp_data)
                    self.temp_data.append(temp)
                    self.temp_times.append(current_time)
                    
                    # Mantener solo los últimos 100 puntos
                    if len(self.temp_data) > 100:
                        self.temp_data.pop(0)
                        self.temp_times.pop(0)
                        self.temp_times = list(range(len(self.temp_data)))
                    
                    self.temp_curve.setData(self.temp_times, self.temp_data)
            
            elif "Humedad:" in data:
                hum_match = re.search(r"Humedad:\s*([0-9.-]+)", data)
                if hum_match:
                    humidity = float(hum_match.group(1))
                    self.sensor_data['humidity'] = humidity
                    self.humidity_card.update_value(f"{humidity:.1f}")
            
            elif "Luminosidad:" in data:
                light_match = re.search(r"Luminosidad:\s*([0-9.-]+)", data)
                if light_match:
                    light = float(light_match.group(1))
                    self.sensor_data['light'] = light
                    self.light_card.update_value(f"{light:.0f}")
            
            # Buscar estados de LEDs
            led_match = re.search(r"LED (\d+).*?GPIO (\d+).*?(ENCENDIDO|APAGADO|ON|OFF)", data)
            if led_match:
                led_num = led_match.group(1)
                led_key = f"led{led_num}"
                is_on = led_match.group(3) in ["ENCENDIDO", "ON"]
                
                if led_key in self.led_controls:
                    self.led_controls[led_key].update_status(is_on)
            
            # Buscar contadores de mensajes
            sent_match = re.search(r"Mensajes enviados:\s*(\d+)", data)
            if sent_match:
                count = sent_match.group(1)
                self.messages_sent_label.setText(f"Enviados: {count}")
            
            received_match = re.search(r"Comandos recibidos:\s*(\d+)", data)
            if received_match:
                count = received_match.group(1)
                self.messages_received_label.setText(f"Recibidos: {count}")
                
        except Exception as e:
            # Error en parsing, ignorar silenciosamente
            pass
    
    def update_ui(self):
        """Actualizar interfaz periódicamente"""
        # Aquí puedes agregar actualizaciones periódicas si es necesario
        pass
    
    def clear_console(self):
        """Limpiar la consola"""
        self.console_text.clear()
    
    def save_log(self):
        """Guardar log de la consola"""
        from PyQt6.QtWidgets import QFileDialog
        
        filename, _ = QFileDialog.getSaveFileName(
            self, 
            "Guardar Log", 
            f"esp32_log_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt",
            "Text Files (*.txt);;All Files (*)"
        )
        
        if filename:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(self.console_text.toPlainText())
            self.console_text.append(f"✅ Log guardado en: {filename}")
    
    def toggle_theme(self):
        """Cambiar entre tema claro y oscuro"""
        self.is_dark_mode = not self.is_dark_mode
        if self.is_dark_mode:
            self.apply_dark_theme()
        else:
            self.setStyleSheet("")  # Tema claro por defecto
    
    def closeEvent(self, event):
        """Manejar cierre de la aplicación"""
        self.serial_thread.disconnect_serial()
        self.serial_thread.quit()
        self.serial_thread.wait()
        event.accept()


def main():
    """Función principal"""
    app = QApplication(sys.argv)
    
    # Configurar aplicación
    app.setApplicationName("ESP32 UDP Lab Serial Monitor")
    app.setApplicationVersion("1.0")
    app.setOrganizationName("Daniel Araque Studios")
    
    # Crear y mostrar ventana principal
    window = ESP32SerialMonitor()
    window.show()
    
    # Ejecutar aplicación
    sys.exit(app.exec())


if __name__ == "__main__":
    main()