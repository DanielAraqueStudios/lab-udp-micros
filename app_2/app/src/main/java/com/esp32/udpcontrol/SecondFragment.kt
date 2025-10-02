package com.esp32.udpcontrol

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.esp32.udpcontrol.databinding.FragmentSecondBinding
import kotlinx.coroutines.launch

/**
 * Fragment secundario para configuración y estadísticas
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ESP32ViewModel by activityViewModels { ESP32ViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Botón para regresar
        binding.buttonSecond?.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        
        // Otros controles de configuración aquí
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.esp32State.collect { state ->
                // Actualizar UI con estado del ESP32
                updateStatistics(state)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sensorHistory.collect { history ->
                // Actualizar gráficos o historial
                updateHistory(history)
            }
        }
    }
    
    private fun updateStatistics(state: ESP32State) {
        val stats = viewModel.getSensorStats()
        if (stats != null) {
            // Mostrar estadísticas si hay elementos en el layout
            // binding.tvAvgTemp?.text = "Temp. promedio: ${stats.avgTemperature}°C"
            // binding.tvAvgHum?.text = "Humedad promedio: ${stats.avgHumidity}%"
            // binding.tvDataPoints?.text = "Puntos de datos: ${stats.dataPoints}"
        }
    }
    
    private fun updateHistory(history: List<SensorData>) {
        // Aquí se pueden actualizar gráficos o listas de historial
        // Por ahora solo mostrar el conteo
        // binding.tvHistoryCount?.text = "Registros: ${history.size}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}