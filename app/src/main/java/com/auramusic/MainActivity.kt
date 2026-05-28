package com.auramusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.ui.components.SharedViewModel
import com.auramusic.ui.navigation.AppNavigation
import com.auramusic.ui.theme.AuraMusicTheme
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()

    private var _hasAudioPermission = false
    val hasAudioPermission: Boolean get() = _hasAudioPermission

    private var permissionRequestInProgress = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequestInProgress = false
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val audioGranted = permissions[audioPermission] ?: false
        _hasAudioPermission = audioGranted
        Log.d("MainActivity", "Permiso audio concedido: $audioGranted")

        if (audioGranted) {
            Log.d("MainActivity", "Permiso audio concedido - scan handled by splash screen")
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Log.w("MainActivity", "Permisos denegados: $deniedPermissions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        _hasAudioPermission = checkAudioPermissions()
        
        setContent {
            var permissionsChecked by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                if (!permissionsChecked) {
                    permissionsChecked = true
                    requestAudioPermissions()
                }
            }

            val themeMode by sharedViewModel.preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = AppPreferences.THEME_AMOLED)
            val accentColor by sharedViewModel.preferences.accentColor
                .collectAsStateWithLifecycle(initialValue = 0xFF8B5CF6L)

            AuraMusicTheme(themeMode = themeMode, accentColor = Color(accentColor)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(sharedViewModel = sharedViewModel)
                }
            }
        }
    }

    private fun checkAudioPermissions(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
            false
        }
    }

    private fun requestAudioPermissions() {
        if (permissionRequestInProgress) return
        try {
            val permissions = mutableListOf<String>()
            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(audioPermission)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                        Log.i("MainActivity", "RECORD_AUDIO needed for audio visualizer (FFT) - showing rationale")
                    }
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                }
            }

            if (permissions.isNotEmpty()) {
                permissionRequestInProgress = true
                Log.d("MainActivity", "Solicitando permisos: $permissions")
                try {
                    requestPermissionLauncher.launch(permissions.toTypedArray())
                } catch (e: Exception) {
                    permissionRequestInProgress = false
                    Log.e("MainActivity", "Error lanzando solicitud de permisos", e)
                }
            } else {
                _hasAudioPermission = true
                Log.d("MainActivity", "Permisos ya concedidos")
            }
        } catch (e: Exception) {
            permissionRequestInProgress = false
            Log.e("MainActivity", "Error requesting permissions", e)
        }
    }
}
