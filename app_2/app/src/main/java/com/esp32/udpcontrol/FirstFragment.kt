package com.esp32.udpcontrol

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.esp32.udpcontrol.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch

/**
 * Fragment principal para el control del ESP32
 * Muestra datos de sensores y controles de LEDs
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ESP32ViewModel by activityViewModels { ESP32ViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Configurar listeners de botones
        binding.btnConnect.setOnClickListener {
            when (viewModel.esp32State.value.connectionState) {
                ConnectionState.DISCONNECTED -> {
                    // Obtener la IP del campo de entrada
                    val ipAddress = binding.etEsp32Ip.text.toString().trim()
                    if (ipAddress.isNotEmpty() && isValidIPAddress(ipAddress)) {
                        // Actualizar la configuración de red con la nueva IP
                        val newConfig = NetworkConfig(
                            esp32Ip = ipAddress,
                            esp32Port = 4210,
                            localPort = 4211
                        )
                        viewModel.updateNetworkConfig(newConfig)
                        viewModel.connect()
                    } else {
                        // Mostrar error si la IP no es válida
                        binding.tvConnectionStatus.text = "Error: IP inválida"
                        binding.tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                    }
                }
                ConnectionState.CONNECTED -> viewModel.disconnect()
                else -> {} // No hacer nada si está conectando
            }
        }
        
        binding.btnPing.setOnClickListener {
            viewModel.ping()
        }
        
        // Controles de LEDs
        binding.btnLed1.setOnClickListener { viewModel.toggleLed(1) }
        binding.btnLed2.setOnClickListener { viewModel.toggleLed(2) }
        binding.btnLed3.setOnClickListener { viewModel.toggleLed(3) }
        binding.btnLed4.setOnClickListener { viewModel.toggleLed(4) }
        
        binding.btnAllOn.setOnClickListener { viewModel.turnOnAllLeds() }
        binding.btnAllOff.setOnClickListener { viewModel.turnOffAllLeds() }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.esp32State.collect { state ->
                updateConnectionState(state.connectionState)
                state.lastSensorData?.let { updateSensorData(it) }
            }
        }
    }
    
    private fun updateConnectionState(state: ConnectionState) {
        val currentIp = viewModel.esp32State.value.networkConfig.esp32Ip
        
        when (state) {
            ConnectionState.DISCONNECTED -> {
                binding.tvConnectionStatus.text = "Desconectado"
                binding.tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                binding.btnConnect.text = "Conectar"
                binding.btnConnect.isEnabled = true
                enableLedControls(false)
            }
            ConnectionState.CONNECTING -> {
                binding.tvConnectionStatus.text = "Conectando a $currentIp..."
                binding.tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                binding.btnConnect.text = "Conectando..."
                binding.btnConnect.isEnabled = false
                enableLedControls(false)
            }
            ConnectionState.CONNECTED -> {
                binding.tvConnectionStatus.text = "Conectado a $currentIp"
                binding.tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                binding.btnConnect.text = "Desconectar"
                binding.btnConnect.isEnabled = true
                enableLedControls(true)
            }
            ConnectionState.ERROR -> {
                binding.tvConnectionStatus.text = "Error conectando a $currentIp"
                binding.tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                binding.btnConnect.text = "Reconectar"
                binding.btnConnect.isEnabled = true
                enableLedControls(false)
            }
        }
    }
    
    private fun enableLedControls(enabled: Boolean) {
        binding.btnLed1.isEnabled = enabled
        binding.btnLed2.isEnabled = enabled
        binding.btnLed3.isEnabled = enabled
        binding.btnLed4.isEnabled = enabled
        binding.btnAllOn.isEnabled = enabled
        binding.btnAllOff.isEnabled = enabled
        binding.btnPing.isEnabled = enabled
    }
    
    private fun updateSensorData(data: SensorData) {
        binding.tvTemperature.text = "${data.temperature}°C"
        binding.tvHumidity.text = "${data.humidity}%"
        binding.tvLight.text = "${data.light}"
        
        // Actualizar estados de LEDs
        updateLedButton(binding.btnLed1, data.led1)
        updateLedButton(binding.btnLed2, data.led2)
        updateLedButton(binding.btnLed3, data.led3)
        updateLedButton(binding.btnLed4, data.led4)
    }
    
    private fun updateLedButton(button: View, isOn: Boolean) {
        val color = if (isOn) {
            requireContext().getColor(android.R.color.holo_green_light)
        } else {
            requireContext().getColor(android.R.color.darker_gray)
        }
        button.setBackgroundColor(color)
    }
    
    /**
     * Valida si una dirección IP es válida
     */
    private fun isValidIPAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            
            for (part in parts) {
                val num = part.toInt()
                if (num < 0 || num > 255) return false
            }
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}