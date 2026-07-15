# Aura Music — Plan de Revisión y Correcciones

## Goal
Revisar y corregir la aplicación Aura Music parte por parte para asegurar su correcto funcionamiento.

## Constraints
- Sistema: Android (minSdk 26, targetSdk 36)
- Stack: Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, Hilt, Gradle KTS
- **No hay JDK instalado** — no se puede compilar para verificar

---

## Progreso General

| Parte | Módulo | Estado | Archivos |
|-------|--------|--------|----------|
| 1 | Escaneo y Carga | ✅ Completado | MusicScanner, SongEntity, SongDao, AuraDatabase, MusicRepositoryImpl, MusicScanWorker, ScanMusicUseCase, LibraryViewModel |
| 2 | Reproducción | ✅ Completado | MusicPlayer, SleepTimerManager, EqualizerManager, AudioFocusManager, FftVisualizer, AuraColorManager, MusicPlaybackService, BluetoothA2DPReceiver, NotificationReceiver, MusicWidgetProvider |
| 3 | UI / ViewModels | ✅ Completado | PlayerViewModel, NowPlayingScreen |
| 4 | Navegación y Tema | ✅ Corregido | AppNavigation.kt, MiniPlayer.kt, CommonComponents.kt |
| 5 | Pantallas restantes | ⬜ Pendiente | HomeScreen, LibraryScreen, SearchScreen, SettingsScreen, SongListScreen, PlaylistDetailScreen, HistoryScreen, etc. |
| 6 | Tests | ⬜ Pendiente | — |

---

## Parte 1 — Escaneo y Carga de Música (`core` + `data`)
### Hallazgos (15 encontrados, 8 corregidos)

| # | Archivo | Issue | Severidad | Corregido |
|---|---------|-------|-----------|-----------|
| 1 | MusicScanner.kt | Path hardcoded `/storage/emulated/0/` | 🔴 Crítico | ✅ Usa `Environment.getExternalStorageDirectory()` |
| 2 | MusicScanner.kt | Filtro `duration <= 0` elimina canciones válidas | 🟡 Medio | ✅ Eliminado |
| 3 | MusicScanner.kt | Sin refresh en API 33+ con grupo READ_MEDIA_AUDIO | 🔵 Info | ⬜ |
| 4 | MusicScanner.kt | No escanea `folderPaths` si `contentUri` falla | 🔵 Info | ⬜ |
| 5 | MusicScanner.kt | Sin filtro `BUCKET_DISPLAY_NAME` | 🔵 Info | ⬜ |
| 6 | SongEntity.kt | Sin índices en columnas de búsqueda frecuente | 🟠 Alto | ✅ +9 índices (`artist`, `album`, `album_id`, `genre`, `path`, `is_favorite`, `play_count`, `last_played`, `date_added`) |
| 7 | AuraDatabase.kt | No usa índices (no migración) | 🟠 Alto | ✅ Migración v1→v2 con `CREATE INDEX IF NOT EXISTS` |
| 8 | AuraDatabase.kt | Usa `Log.i` en vez de `Timber` | 🟡 Medio | ✅ `Timber.i` |
| 9 | SongDao.kt | Falta `getAllSongIds()`, `deleteSongsByIds()` | 🟠 Alto | ✅ Agregados |
| 10 | PlaylistDao.kt | Falta `deletePlaylistCascade()`, `removeSongsFromAllPlaylists()` | 🟠 Alto | ✅ Agregados |
| 11 | MusicRepositoryImpl.kt | `deletePlaylist` no transaccional, no cascade | 🟠 Alto | ✅ Usa `deletePlaylistCascade` |
| 12 | MusicRepositoryImpl.kt | `scanAndCacheMusic` no limpia canciones eliminadas del dispositivo | 🟠 Alto | ✅ `deleteSongsNotInIds()` |
| 13 | MusicScanWorker.kt | No limpia canciones eliminadas después del scan | 🟠 Alto | ✅ Llama `deleteSongsNotInIds` |
| 14 | ScanMusicUseCase.kt | Sin issues | — | ⬜ |
| 15 | LibraryViewModel.kt | `scanMusic()` bloquea el hilo principal | 🔴 Crítico | ✅ `withTimeoutOrNull(3000)` |

---

## Parte 2 — Reproducción (`player`)
### Hallazgos (7 encontrados, 7 corregidos)

| # | Archivo | Issue | Severidad | Corregido |
|---|---------|-------|-----------|-----------|
| 1 | MusicPlayer.kt | `STATE_ENDED` con `setNextMediaItem` rompe repeat modes nativos | 🔴 Crítico | ✅ Delega en ExoPlayer; solo interviene si no hay más items y repeat off |
| 2 | MusicPlayer.kt | `play()` no llama `prepare()` cuando está en `STATE_ENDED` | 🔴 Crítico | ✅ `prepare()` + manejo de `STATE_ENDED` |
| 3 | MusicPlayer.kt | `crossfadeJob` y `fadeJob` pueden correr simultáneos | 🟡 Medio | ✅ `crossfadeJob` cancela `fadeJob` al empezar |
| 4 | MusicPlayer.kt | `pause()` no cancela `crossfadeJob` | 🟡 Medio | ✅ Cancela ambos jobs |
| 5 | MusicPlayer.kt | `persistQueue()` se llama en cada `moveInQueue()` sin debounce | 🟡 Medio | ✅ Debounce 300ms con `persistJob` |
| 6 | SleepTimerManager.kt | Código duplicado en `start()`/`restore()` | 🟡 Medio | ✅ Extraído a `runTimer()` |
| 7 | AudioFocusManager, FftVisualizer, AuraColorManager, BluetoothA2DPReceiver, NotificationReceiver, MusicWidgetProvider | Usan `android.util.Log` en vez de `Timber` | 🟡 Medio | ✅ Todos a `Timber` |

---

## Parte 3 — UI / ViewModels
### Hallazgos (2 encontrados, 2 corregidos)

| # | Archivo | Issue | Severidad | Corregido |
|---|---------|-------|-----------|-----------|
| 1 | PlayerViewModel.kt | `restoreState()` se cuelga si la DB nunca emite | 🔴 Crítico | ✅ `withTimeoutOrNull(5000)` |
| 2 | NowPlayingScreen.kt | Bloque visualizer/waveform/seekbar/controls duplicado entre portrait y landscape | 🟡 Medio | ✅ Extraído `NowPlayingControls` |

---

## Parte 4 — Navegación, Tema y Componentes
### Hallazgos (5 encontrados, 5 corregidos)

| # | Archivo | Issue | Severidad | Corregido |
|---|---------|-------|-----------|-----------|
| 1 | AppNavigation.kt | `safePopBackStack()` recreada en cada recomposición | 🔴 Crítico | ✅ Envuelta en `remember` (sin keys, `navController` es estable) |
| 2 | AppNavigation.kt | `playAllSongs()`/`shufflePlaySongs` recreadas cada recomposición | 🔴 Crítico | ✅ Envueltas en `remember` |
| 3 | AppNavigation.kt | 4 rutas SongListScreen casi idénticas (Artist/Album/Genre/Folder) | 🟡 Medio | ✅ Extraído `SongListContent()` local composable; cada ruta llama con sus parámetros específicos (~70 líneas ahorradas) |
| 4 | AppNavigation.kt | `playAll`/`shufflePlay` duplicados inline en PlaylistDetailScreen | 🟡 Medio | ✅ Reusa `playAllSongs`/`shufflePlaySongs` |
| 5 | CommonComponents.kt | `SectionHeader` texto usa color hardcoded `Color.hsl(0f,0f,1f,0.9f)` en modo animated | 🟡 Medio | ✅ Usa `MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)` |
| — | MiniPlayer.kt | Rotación de disco usa `animateFloatAsState` (solo gira una vez) | 🟡 Medio | ✅ Usa `InfiniteTransition` con `infiniteRepeatable(RepeatMode.Restart)` y aplica condicional con `isPlaying`/`gamerMode` |

---

## Próximos Pasos (Parte 5+)

### Parte 5 — Pantallas restantes
- [ ] `HomeScreen.kt` — verificar colecciones, shimmer, navegación
- [ ] `LibraryScreen.kt` — verificar secciones (artistas, álbumes, géneros, playlists)
- [ ] `SearchScreen.kt` — verificar búsqueda, filtros, historial
- [ ] `SettingsScreen.kt` — verificar equalizador, temas, sleep timer
- [ ] `SongListScreen.kt` — verificar filtros, ordenamiento
- [ ] `PlaylistDetailScreen.kt` — verificar reordenamiento, export/import
- [ ] `NowPlayingScreen.kt` — verificar layout portrait/landscape, visualizer, lyrics
- [ ] `HistoryScreen.kt`, `StatisticsScreen.kt`, `OnboardingScreen.kt`, `SplashScreen.kt`, `CreatePlaylistScreen.kt`

### Parte 6 — Tests
- [ ] Corregir `LibraryViewModelTest.kt` si es necesario
- [ ] Verificar cobertura de tests existentes

### Deuda Técnica (no bloqueante)
- Limpiar imports no usados en todos los archivos
- Revisar si `SongListContent` captura correctamente `songToDelete` (MutableState vía delegado `by`)
- Evaluar si conviene extraer `playAllSongs`/`shufflePlaySongs` a `PlayerViewModel`
- Verificar navegación con argumentos que contienen caracteres especiales (`Uri.encode`)

---

## Archivos Modificados

| Archivo | Cambio |
|---------|--------|
| `core/.../util/MusicScanner.kt` | Path dinámico, duración mínima eliminada |
| `data/.../entity/SongEntity.kt` | +9 índices Room |
| `data/.../database/AuraDatabase.kt` | Migración v1→v2, Timber |
| `data/.../dao/SongDao.kt` | getAllSongIds, deleteSongsByIds |
| `data/.../dao/PlaylistDao.kt` | deletePlaylistCascade, removeSongsFromAllPlaylists |
| `data/.../repository/MusicRepositoryImpl.kt` | deleteSongsNotInIds, deletePlaylist cascade |
| `data/.../worker/MusicScanWorker.kt` | Cleanup post-scan |
| `app/.../components/LibraryViewModel.kt` | scanMusic con timeout |
| `player/.../player/MusicPlayer.kt` | STATE_ENDED, crossfade, persistQueue debounce |
| `player/.../player/SleepTimerManager.kt` | runTimer() extraído |
| `player/.../player/AudioFocusManager.kt` | Timber |
| `player/.../player/FftVisualizer.kt` | Timber |
| `player/.../player/AuraColorManager.kt` | Timber |
| `player/.../service/BluetoothA2DPReceiver.kt` | Timber |
| `player/.../service/NotificationReceiver.kt` | Timber |
| `player/.../widget/MusicWidgetProvider.kt` | Timber |
| `app/.../components/PlayerViewModel.kt` | restoreState con timeout |
| `app/.../screens/nowplaying/NowPlayingScreen.kt` | NowPlayingControls extraído |
| `app/.../navigation/AppNavigation.kt` | remember lambdas, SongListContent, refactor |
| `app/.../components/MiniPlayer.kt` | InfiniteTransition rotación |
| `app/.../components/CommonComponents.kt` | SectionHeader color del tema |
