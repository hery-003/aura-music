package com.auramusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import timber.log.Timber
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.auramusic.ui.components.LibraryViewModel
import com.auramusic.ui.components.PlayerViewModel
import com.auramusic.ui.components.SettingsViewModel
import com.auramusic.ui.navigation.AppNavigation
import com.auramusic.ui.theme.AuraMusicTheme
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

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
        Timber.d("Permiso audio concedido: $audioGranted")

        if (audioGranted) {
            Timber.d("Permiso audio concedido - scan handled by splash screen")
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Timber.w("Permisos denegados: $deniedPermissions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        _hasAudioPermission = checkAudioPermissions()
        
        setContent {
            var permissionsChecked by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                if (!permissionsChecked) {
                    permissionsChecked = true
                    requestAudioPermissions()
                }
            }

            val themeMode by settingsViewModel.preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = AppPreferences.THEME_AMOLED)
            val accentColor by settingsViewModel.preferences.accentColor
                .collectAsStateWithLifecycle(initialValue = 0xFF8B5CF6.toInt())

            AuraMusicTheme(themeMode = themeMode, accentColor = Color(accentColor)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        playerViewModel = playerViewModel,
                        libraryViewModel = libraryViewModel,
                        settingsViewModel = settingsViewModel
                    )
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
            Timber.e(e, "Error checking permissions")
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
                        Timber.i("RECORD_AUDIO needed for audio visualizer (FFT) - showing rationale")
                    }
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                }
            }

            if (permissions.isNotEmpty()) {
                permissionRequestInProgress = true
                Timber.d("Solicitando permisos: $permissions")
                try {
                    requestPermissionLauncher.launch(permissions.toTypedArray())
                } catch (e: Exception) {
                    permissionRequestInProgress = false
                    Timber.e(e, "Error lanzando solicitud de permisos")
                }
            } else {
                _hasAudioPermission = true
                Timber.d("Permisos ya concedidos")
            }
        } catch (e: Exception) {
            permissionRequestInProgress = false
            Timber.e(e, "Error requesting permissions")
        }
    }
}
