<div align="center">
  <img src="logo.png" alt="KuroAnime" width="120" height="120" style="border-radius: 28px;">
  <h1 align="center">KuroAnime</h1>
  <p align="center">Aplicación Android para ver anime subtitulado y latino<br>sin anuncios, con diseño OLED y sistema de extensiones.</p>

  <p>
    <a href="https://github.com/KiritoAPT2/KuroAnime/releases">
      <img src="https://img.shields.io/badge/APK-Descargar-EF5350?style=for-the-badge&logo=android" alt="Download APK">
    </a>
    <a href="https://kiritoapt2.github.io/KuroAnime-Web/">
      <img src="https://img.shields.io/badge/Web-Landing%20Page-FF8A65?style=for-the-badge&logo=google-chrome" alt="Landing Page">
    </a>
    <a href="LICENSE">
      <img src="https://img.shields.io/badge/Licencia-AGPL--3.0-FFB74D?style=for-the-badge" alt="License">
    </a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android" alt="minSdk 24">
    <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin" alt="Kotlin 2.1.0">
    <img src="https://img.shields.io/badge/Compose-1.9.4-4285F4?logo=jetpackcompose" alt="Compose 1.9.4">
    <img src="https://img.shields.io/badge/SDK-35-FF6F00?logo=android" alt="compileSdk 35">
    <img src="https://img.shields.io/badge/License-AGPL--3.0-red" alt="License">
  </p>
</div>

---

## 🚀 Características

<table>
  <tr>
    <td align="center" width="25%"><b>🎬 Anime Latino</b><br><small>Animes doblados al español latino actualizados semanalmente</small></td>
    <td align="center" width="25%"><b>📡 Múltiples Fuentes</b><br><small>Extensiones para AnimeFLV, Latanime, TioAnime y más</small></td>
    <td align="center" width="25%"><b>🌙 Diseño OLED</b><br><small>Modo oscuro con negro puro para pantallas AMOLED</small></td>
    <td align="center" width="25%"><b>▶️ 2do Plano</b><br><small>Reproducción en segundo plano con audio persistente</small></td>
  </tr>
  <tr>
    <td align="center" width="25%"><b>🧩 Extensiones JS</b><br><small>Sistema modular con motor QuickJS embebido</small></td>
    <td align="center" width="25%"><b>🔍 Búsqueda Global</b><br><small>Busca en todas las fuentes simultáneamente</small></td>
    <td align="center" width="25%"><b>📱 Sin Anuncios</b><br><small>Experiencia limpia, sin rastreo ni publicidad</small></td>
    <td align="center" width="25%"><b>🎨 Personalizable</b><br><small>Tema claro/oscuro, múltiples servidores de video</small></td>
  </tr>
</table>

---

## 📥 Instalación

### Desde GitHub Releases

Descarga el APK más reciente desde [Releases](https://github.com/KiritoAPT2/KuroAnime/releases) e instálalo en tu dispositivo Android (permite *Orígenes desconocidos*).

### Compilar desde Código Fuente

```bash
git clone https://github.com/KiritoAPT2/KuroAnime.git
cd KuroAnime
./gradlew assembleDebug
```

El APK se genera en:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧩 Sistema de Extensiones

KuroAnime usa un sistema modular de **extensiones** que pueden estar escritas en **JavaScript** (ejecutado con QuickJS) o **Kotlin** nativo. Las extensiones JS tienen prioridad sobre las de Kotlin.

### Extensiones Incluidas

| Extensión | Tipo | Motor | Idioma | Sitio |
|-----------|------|-------|--------|-------|
| **AnimeFLV** | Kotlin | Jsoup | Sub | animeflv.net |
| **Latanime** | Kotlin | Jsoup | Latino | latanime.org |
| **TioAnime** | Kotlin | Ktor | Sub | tioanime.com |
| **Aniyae** | Kotlin | Ktor | Sub | open.aniyae.net |
| **AnimeOnlineNinja** | JS | QuickJS | Sub | animeonline.ninja |
| **Cuevana3** | JS | QuickJS | Latino | cuevana3.nu |
| **MonosChinos** | JS | QuickJS | Sub | monoschinos.st |

### API de Extensiones

Cada extensión implementa esta interfaz:

```kotlin
interface AnimeExtension {
    val name: String
    val baseUrl: String
    val lang: String            // "sub" o "latino"

    suspend fun search(query: String): List<Anime>
    suspend fun getAnimeInfo(url: String): Anime
    suspend fun getEpisodes(url: String): List<Episode>
    suspend fun getVideoSources(episodeUrl: String): List<VideoSource>
    suspend fun getLatest(page: Int): List<Anime>
    suspend fun getByGenre(genre: String, page: Int): List<Anime>
}
```

---

## 🛠️ Stack Tecnológico

| Librería | Versión | Uso |
|----------|---------|-----|
| Jetpack Compose | 1.9.4 | UI declarativa |
| Material 3 | 1.5.0-alpha12 | Componentes de diseño |
| Navigation Compose | 2.9.6 | Navegación entre pantallas |
| ExoPlayer (Media3) | 1.8.0 | Reproducción de video + HLS |
| Ktor | 3.0.3 | Cliente HTTP (OkHttp engine) |
| Jsoup | 1.22.2 | Parseo de HTML (scraping) |
| Coil | 2.7.0 | Carga de imágenes |
| QuickJS Android | 1.4.6 | Motor JavaScript embebido |
| Kotlinx Serialization | 1.8.0 | JSON parsing |
| Kotlin | 2.1.0 | Lenguaje |
| AGP | 8.10.1 | Build system |

---

## 📂 Estructura del Proyecto

```
app/
├── src/main/
│   ├── assets/extensions/       # Archivos .js para QuickJS
│   ├── java/com/kuroanime/
│   │   ├── data/
│   │   │   ├── HttpClient.kt    # OkHttp compartido + User-Agent
│   │   │   ├── SettingsManager.kt
│   │   │   ├── ResultCache.kt
│   │   │   └── model/Models.kt  # Anime, Episode, VideoSource
│   │   ├── extension/
│   │   │   ├── AnimeExtension.kt        # Interfaz
│   │   │   ├── ExtensionManager.kt      # Registro central
│   │   │   ├── JsExtensionEngine.kt     # Motor QuickJS
│   │   │   ├── JsExtension.kt           # Wrapper JS
│   │   │   ├── JsoupBasedExtension.kt   # Base para scraping
│   │   │   ├── AnimeFlvExtension.kt
│   │   │   ├── LatanimeExtension.kt
│   │   │   ├── TioAnimeExtension.kt
│   │   │   └── AniyaeExtension.kt
│   │   ├── player/
│   │   │   ├── ExoPlayerUtils.kt
│   │   │   ├── EmbedResolver.kt
│   │   │   ├── WebViewResolver.kt
│   │   │   └── ImmersiveUtils.kt
│   │   └── ui/
│   │       ├── components/
│   │       ├── navigation/
│   │       ├── screens/
│   │       │   ├── home/
│   │       │   ├── search/
│   │       │   ├── animeinfo/
│   │       │   ├── player/
│   │       │   └── settings/
│   │       └── theme/
│   └── res/
├── gradle/
│   └── libs.versions.toml       # Versiones centralizadas
├── build.gradle.kts
└── settings.gradle.kts
```

---

## ⚙️ Compilación

| Comando | Resultado |
|---------|-----------|
| `./gradlew assembleDebug` | APK debug (`app/build/outputs/apk/debug/`) |
| `./gradlew bundleRelease` | AAB release (`app/build/outputs/bundle/release/`) |
| `./gradlew :app:compileDebugKotlin` | Solo compilar Kotlin |
| `./gradlew clean` | Limpiar build |

---

## 📸 Capturas

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="Icon"><br><sub>Inicio</sub></td>
      <td align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="Icon"><br><sub>Categorías</sub></td>
      <td align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="Icon"><br><sub>Reproductor</sub></td>
      <td align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="Icon"><br><sub>Ajustes</sub></td>
    </tr>
  </table>
  <p><em>Próximamente capturas reales de la app.</em></p>
</div>

---

## 🤝 Contribuir

Las contribuciones son bienvenidas. Podés ayudar con:

- 🐛 **Reportar bugs** — abrí un [Issue](https://github.com/KiritoAPT2/KuroAnime/issues)
- 💡 **Sugerir funciones** — nuevas fuentes, mejoras en el reproducción, etc.
- 🧩 **Crear extensiones** — escribí un .js o un .kt y hacé un Pull Request
- 📖 **Mejorar documentación** — este README, wiki, etc.

---

## 📄 Licencia

**KuroAnime** no almacena ni distribuye contenido con derechos de autor. Solo enlaza a fuentes públicas de terceros.

```
AGPL-3.0 License
Copyright (c) 2026 KiritoAPT2
```

---

<div align="center">
  <p>Hecho con ❤️ · <a href="https://github.com/KiritoAPT2">KiritoAPT2</a> · 2026</p>
  <p>
    <a href="https://kiritoapt2.github.io/KuroAnime-Web/">Landing Page</a> ·
    <a href="https://github.com/KiritoAPT2/KuroAnime/issues">Issues</a> ·
    <a href="https://github.com/KiritoAPT2/KuroAnime/releases">Releases</a>
  </p>
</div>
