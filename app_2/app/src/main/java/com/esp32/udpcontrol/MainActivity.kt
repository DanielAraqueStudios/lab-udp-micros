package com.esp32.udpcontrol

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.esp32.udpcontrol.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ESP32ViewModel by viewModels { ESP32ViewModelFactory() }
    
    // Launcher para permisos de red
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permisos de red necesarios para la comunicación UDP", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Verificar permisos de red
        checkNetworkPermissions()
        
        // Observar errores globales
        observeErrors()

        binding.fab.setOnClickListener { view ->
            // Conectar/desconectar ESP32
            when (viewModel.esp32State.value.connectionState) {
                ConnectionState.DISCONNECTED -> {
                    viewModel.connect()
                    Toast.makeText(this, "Conectando a ESP32...", Toast.LENGTH_SHORT).show()
                }
                ConnectionState.CONNECTED -> {
                    viewModel.disconnect()
                    Toast.makeText(this, "Desconectando...", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Conexión en proceso...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkNetworkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permisos ya concedidos
            }
            else -> {
                // Solicitar permisos
                requestPermissionLauncher.launch(Manifest.permission.INTERNET)
            }
        }
    }
    
    private fun observeErrors() {
        lifecycleScope.launch {
            viewModel.esp32State.collect { state ->
                state.errorMessage?.let { error ->
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }
}