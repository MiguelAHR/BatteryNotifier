# 🔋 BatteryNotifier

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue?style=flat-square)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-orange?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

**Alertas inteligentes de batería con 3 niveles de invasividad, sonidos personalizables y monitoreo optimizado.**

> 🇬🇧 *Smart battery alerts with 3 invasiveness levels, customizable sounds and battery-optimized monitoring. Full English docs below in [English](#-english).*

---

## ✨ Características

| Función | Descripción |
|---|---|
| 🔔 **Umbrales configurables** | Notifica al llegar a cualquier % (ej. 30 / 20 / 10 / 5). Sin límite duro, recomendado ≤ 3 activos |
| 🟢 **Alerta Normal** | Solo barra de estado, silenciosa o con sonido |
| 🟡 **Alerta Invasiva 1** | Notificación + **pantalla completa** (`full-screen intent`), con sonido y vibración |
| 🔴 **Alerta Invasiva 3** | Igual que Invasiva 1 + **notificaciones continuas cada ~1%** hasta conectar el cargador |
| 🎵 **Sonidos** | Tonos del sistema + archivos externos (SAF con permiso persistible) + vista previa |
| ⚙️ **Accesos directos** | Botones a ajustes del sistema: notificaciones, sonido, ahorro de batería, acceso a notificaciones |
| 🔐 **Permisos guiados** | Solicitud en tiempo de ejecución + accesos directos a ajustes |
| 🔄 **Auto-arranque** | Opcional: reanuda la monitorización tras reiniciar el dispositivo |
| 🌍 **Multi-idioma** | Español + Inglés (`values-es/`, `values-en/`) |
| 🎨 **UI neón** | Tema oscuro con animaciones básicas (`AnimatedVisibility`, pulso, `animateContentSize`) |
| 🔋 **Ligera** | Sin widgets pesados, `AlarmManager` inexacto + `WorkManager`, `minify + shrink` en release |

---

## 📱 Capturas

> _Agrega tus capturas en `docs/screenshots/` y actualiza estas rutas._

| Dashboard | Ajustes | Alerta full-screen |
|---|---|---|
| `docs/screenshots/dashboard.png` | `docs/screenshots/settings.png` | `docs/screenshots/fullscreen.png` |

---

## 🛠️ Stack técnico

| Componente | Versión |
|---|---|
| Kotlin | 1.9.24 |
| AGP | 8.7.3 |
| Gradle | 8.9 |
| Compile / Target SDK | 36 (Android 16) |
| Min SDK | 26 (Android 8.0) |
| Compose BOM | 2024.06.00 |
| Compose Compiler | 1.5.14 |
| kotlinx-datetime | 0.6.1 |
| DataStore Preferences | 1.1.1 |
| WorkManager | 2.9.0 |
| Lifecycle / Activity Compose | 2.8.3 / 1.9.0 |
| Material Icons Extended | 1.6.8 |
| Java | 17 (`jvmTarget = 17`) |

**Arquitectura:** MVVM + Clean Architecture
`ui/` (Compose) → `domain/model/` (lógica pura) → `data/` (`settings/` DataStore, `notification/`, `alarm/`, `battery/`, `sound/`, `work/`)

---

## 🔐 Permisos

| Permiso | Uso | Obligatorio |
|---|---|---|
| `POST_NOTIFICATIONS` | Mostrar alertas (runtime, API 33+) | ✅ Sí |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Alarmas en el % exacto | ⚠️ Recomendado |
| `RECEIVE_BOOT_COMPLETED` | Auto-arranque tras reinicio | Solo si activas auto-arranque |
| `VIBRATE` | Vibración en Invasiva 1/3 | Solo si activas vibración |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Sonidos externos | Solo si usas archivo propio |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Monitoreo continuo (Invasiva 3) | Solo Invasiva 3 |

---

## 🚀 Instalación y compilación

**Requisitos:** Android Studio Ladybug+, JDK 17, SDK con **Android 16 (API 36)** instalado.

```bash
# Clonar
git clone https://github.com/TU_USUARIO/BatteryNotifier.git
cd BatteryNotifier

# Compilar debug
./gradlew assembleDebug

# APK firmado release (minify + shrink activados)
./gradlew assembleRelease

# Tests unitarios
./gradlew :app:testDebugUnitTest
```

El APK debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

> ⚠️ `local.properties` (con tu `sdk.dir`) **no** se versiona. Android Studio lo genera solo.

---

## 📖 Uso

1. Abre la app y **concede los permisos** (notificaciones como mínimo).
2. En **Ajustes**, activa cada umbral y elige su **tipo de alerta**:
   - **Normal** → discreta, barra de estado.
   - **Invasiva 1** → pantalla completa + sonido/vibración.
   - **Invasiva 3** → como Invasiva 1 + repite cada ~1% hasta que conectes el cargador.
3. Elige **sonido** por umbral (sistema o archivo propio) y pulsa **Probar notificación**.
4. (Opcional) Activa **Auto-arranque** para que sobreviva reinicios.
5. Usa **Accesos rápidos** para ir directo a ajustes del sistema.

---

## 📁 Estructura del proyecto

```
BatteryNotifier/
├── app/src/main/java/com/batterynotify/battery/
│   ├── MainActivity.kt
│   ├── domain/model/BatteryModels.kt        # AlertType, ThresholdConfig, BatterySettings
│   ├── data/
│   │   ├── settings/SettingsRepository.kt   # DataStore
│   │   ├── notification/                    # Notifications, NotificationChannels
│   │   ├── alarm/                           # Engine, ThresholdReceiver, BootReceiver
│   │   ├── battery/                         # BatteryManagerWrapper, MonitoringService
│   │   ├── sound/SoundPickerHelper.kt       # Tonos sistema + SAF
│   │   └── work/ContinuousAlertWorker.kt    # Invasiva 3 (WorkManager)
│   └── ui/
│       ├── main/                            # Dashboard + BatteryViewModel
│       ├── settings/                        # Settings + SettingsViewModel
│       ├── alert/FullScreenAlertActivity.kt # Pantalla completa
│       ├── components/                      # AlertTypePicker, SoundPicker, NeonCard…
│       ├── permissions/PermissionsHelper.kt
│       └── theme/                           # Tema neón
└── app/src/main/res/
    ├── values/strings.xml                   # Base (ES)
    ├── values-en/strings.xml                # English
    └── values-es/strings.xml                # Español
```

---

## 🗺️ Roadmap

- [x] v1.0.0 — 3 tipos de alerta, full-screen, sonidos sistema/externos, auto-arranque, ES/EN
- [ ] v1.1.0 — `ContinuousAlertWorker` con WorkManager periódico + detección fina cada 1%
- [ ] v1.2.0 — Tests unitarios (`BatteryThresholdEngine`, `Notifications`) + tests en Moto G75 Android 16
- [ ] v1.3.0 — Historial de alertas + estadísticas de descarga
- [ ] v2.0.0 — Umbrales ilimitados con UI de arrastrar, temas claro/oscuro

---

## 🤝 Contribuir

1. Haz fork del repo.
2. Crea una rama: `git checkout -b feat/mi-mejora`.
3. Commitea con mensajes claros (español o inglés).
4. Abre un Pull Request describiendo el cambio y cómo probarlo.

---

## 📄 Licencia

MIT — ver [LICENSE](LICENSE) (agrégalo si aún no existe).

---

## 🇬🇧 English

**BatteryNotifier** — smart low-battery alerts for Android with 3 invasiveness levels:

- **Normal** — status-bar only notification.
- **Invasive 1** — notification + **full-screen intent** with sound & vibration.
- **Invasive 3** — like Invasive 1 + **repeating alerts roughly every 1%** until the charger is connected.

Per-threshold sound picker (system tones + external files via SAF), quick links to system settings (notifications, sound, battery saver, notification access), runtime permission flows, optional auto-start on boot (`RECEIVE_BOOT_COMPLETED`), Spanish + English strings, neon dark Compose UI with basic animations, and battery-friendly scheduling (`AlarmManager` + `WorkManager`, minify/shrink in release).

**Build:** `./gradlew assembleDebug` (JDK 17, Android Studio Ladybug+, API 36 SDK). See tables above for the full tech stack and permissions.
