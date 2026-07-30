# Feature-First Modular Architecture — Analiz ve Geçiş Raporu

Tarih: 2026-07-30 · Analiz raporu. **Beş fazın tamamı uygulandı — bkz. [§6 Uygulama durumu](#6-uygulama-durumu).**

## 1. Önemli tespit: Proje zaten feature-first

Modül grafiği şu an şöyle:

```
androidApp ─┐
            ├─► shared (composition root: Koin + NavDisplay)
iosApp ─────┘        │
        ┌────────────┼────────────────┐
        ▼            ▼                ▼
  feature:home  feature:category  feature:player     (her birinin :navigation alt modülü var)
        │            │                │
        └────────────┴───────┬────────┘
                             ▼
   core:domain (port + use case) · core:model · core:designsystem · core:navigation
   core:audio-content · core:favorites · core:playback · core:playback-engine · core:datastore
```

Yapı doğru kurulmuş:

- Feature'lar birbirinin **implementasyonuna değil, yalnızca `:navigation` API modülüne** bağımlı
  (ör. `feature:home` → `feature:player:navigation`). Bu, feature-first'ün en kritik kuralıdır ve
  zaten uygulanıyor.
- Ekranlar route (`NavKey`), Koin `navigation<Route> {}` entry'si ve ViewModel'iyle kendi
  modülünde yaşıyor.
- `core` katmanı port/adapter ayrımıyla bölünmüş; `core:domain` saf Kotlin.

**Sonuç:** "Feature-first'e geçiş" değil, "feature ekleme maliyetini düşürme" problemi var.
Sorun mimari şablonda değil, aşağıdaki sürtünme noktalarında.

## 2. Asıl sorun: Yeni bir feature eklemek bugün 8 noktaya dokunmayı gerektiriyor

Bugün `feature:foo` eklemek için yapılması gerekenler:

| # | Dokunulan yer | İçerik |
|---|---------------|--------|
| 1 | `settings.gradle.kts` | 2 satır `include` (`:feature:foo`, `:feature:foo:navigation`) |
| 2 | `feature/foo/build.gradle.kts` | ~50 satır, mevcut bir feature'dan kopyala-yapıştır |
| 3 | `feature/foo/navigation/build.gradle.kts` | ~30 satır, yine kopyala-yapıştır |
| 4 | `shared/build.gradle.kts` | 2 bağımlılık satırı |
| 5 | `shared/.../di/AppModules.kt` | Koin modül kaydı |
| 6 | `shared/.../navigation/AppNavigation.kt` | `SerializersModule` include'u |
| 7 | `core/domain/.../ObserveContentUseCases.kt` | Ekrana özel use case + content data class |
| 8 | `shared/.../di/DomainModule.kt` | Use case Koin factory kaydı |

7 ve 8 en pahalıları: ekrana özel iş mantığı `core:domain`'de yaşadığı için her yeni ekran
core modülünü değiştiriyor, bu da tüm feature'ların yeniden derlenmesine ve core'un sürekli
büyümesine yol açıyor.

## 3. Yapılacaklar listesi (fazlara bölünmüş)

### Faz 1 — Convention plugin'ler (`build-logic`) · en yüksek getiri

15 modülün build dosyası neredeyse aynı ~35 satırı tekrarlıyor (iOS guard, `compileSdk`,
`jvmTarget`, `withHostTest`, Compose plugin seti). Yapılacaklar:

1. Kök dizine `build-logic/` composite build oluştur (`settings.gradle.kts`'e
   `includeBuild("build-logic")`).
2. Şu convention plugin'leri yaz:
   - `xwab.kmp.library` — KMP hedefleri (android + iOS guard'ı), `JvmTarget.JVM_11`,
     `compileSdk`/`minSdk`, `withHostTest`. (`gradle.extra["enableIos"]` mantığı buraya taşınır.)
   - `xwab.kmp.compose` — Compose Multiplatform + compiler plugin'i ve ortak Compose bağımlılıkları.
   - `xwab.kmp.feature` — `xwab.kmp.library` + `xwab.kmp.compose` + her feature'ın zaten aldığı
     ortak bağımlılıklar (`core:model`, `core:domain`, `core:designsystem`, `core:navigation`,
     lifecycle-viewmodel, koin-compose-viewmodel, koin-compose-navigation3).
   - `xwab.kmp.feature.navigation` — navigation API modülleri için (navigation3-runtime +
     kotlinx-serialization).
3. Mevcut 15 modülü bu plugin'lere geçir. Feature build dosyası ~50 satırdan ~10 satıra iner:

```kotlin
plugins { id("xwab.kmp.feature") }
kotlin {
    android { namespace = "com.xwab.app.feature.foo" }
    sourceSets.commonMain.dependencies {
        implementation(projects.feature.foo.navigation)
        implementation(projects.feature.player.navigation) // sadece gerçek cross-feature ihtiyaçlar
    }
}
```

Riski düşük, davranış değiştirmez; tabloda 2–3 numaralı maliyeti ortadan kaldırır.

### Faz 2 — Ekrana özel use case'leri feature'lara taşı

`core:domain` şu an iki şeyi karıştırıyor: gerçek paylaşılan sözleşmeler (portlar:
`MusicCatalogRepository`, `FavoritesRepository`, `PlaybackCoordinator`) ve ekrana özel
orkestrasyon (`ObserveHomeContentUseCase`, `HomeContent`, `ObserveCategoryContentUseCase`, …).

1. `ObserveHomeContentUseCase` + `HomeContent` → `feature:home` içine taşı; aynısını
   category ve player için yap.
2. Birden fazla feature'ın paylaştığı use case'ler (`ToggleFavoriteUseCase`,
   `ToggleMusicPlaybackUseCase` home+category+player'da ortak) `core:domain`'de kalır.
3. Her feature kendi use case'ini kendi Koin modülünde `factory {}` ile bağlar;
   `shared/DomainModule.kt` yalnızca ortak use case'lerle kalır (veya tamamen erir).
4. Testleri taşı (`ObserveContentUseCasesTest` ilgili feature'ların `commonTest`'ine bölünür).

Sonuç: yeni ekran = `core:domain`'e sıfır dokunuş (yeni port gerekmediği sürece).
Tablodaki 7–8 numaralı maliyet feature'ın kendi içine iner.

### Faz 3 — Feature kaydını tek noktaya indir

5 ve 6 numaralı dokunuşları tek satıra düşürmek için `core:navigation`'a küçük bir sözleşme ekle:

```kotlin
// core:navigation
data class FeatureEntry(
    val koinModule: Module,
    val serializers: SerializersModule = EmptySerializersModule(),
)
```

Her feature tek bir `val fooFeature = FeatureEntry(fooModule, fooNavigationSerializers)` dışa
açar. `shared` tarafında:

```kotlin
private val features = listOf(homeFeature, categoryFeature, playerFeature)
// appModules() feature'ların koinModule'lerini, AppNavigation serializers'ları buradan toplar
```

Yeni feature kaydı = `shared`'da tek liste elemanı. (Koin `navigation<Route>` entry'leri zaten
modül içinden `koinEntryProvider()` ile otomatik toplanıyor; bu mekanizma korunur.)

### Faz 4 — Feature iskeleti üretimi (scaffolding)

- `tools/new-feature.ps1` (veya Gradle task): `feature/foo` + `feature/foo/navigation`
  klasörlerini, 10 satırlık build dosyalarını, Route/Screen/ViewModel/State/Module iskeletini ve
  `settings.gradle.kts` include'larını otomatik oluşturan bir şablon.
- Alternatif (daha az sihirli): `settings.gradle.kts`'te `feature/` klasörünü tarayıp modülleri
  otomatik `include` eden döngü — include satırlarını da sıfırlar.

### Faz 5 — Kural koruması ve dokümantasyon

- README'deki mimari bölümünü güncelle (feature ekleme adımları artık: "iskeleti üret, listeye
  ekle, bitti" olmalı).
- Mimari kuralları test ile sabitle (öneri: [Konsist](https://docs.konsist.lemonappdev.com/)):
  - Bir feature başka bir feature'ın yalnızca `:navigation` modülüne bağımlı olabilir.
  - `core:*` modülleri `feature:*` modüllerine bağımlı olamaz.
  - Ekrana özel use case'ler `core:domain`'e geri sızamaz.
- Eksik dilim: README "feature slices: home, category, **favorites**, player" diyor ama
  `feature:favorites` modülü yok (favoriler home içinde). Bilinçliyse README düzeltilmeli,
  değilse Faz 4'teki iskeletle ilk aday feature olarak ayrıştırılabilir.

## 4. Önerilen sıra ve beklenen kazanım

| Faz | Eforu | Kazanım |
|-----|-------|---------|
| 1. Convention plugin'ler | Orta (1 oturum) | Feature başına ~80 satır build boilerplate'i → ~10 satır |
| 2. Use case taşıma | Orta | `core:domain` dokunuşu sıfırlanır, derleme izolasyonu |
| 3. FeatureEntry kaydı | Düşük | `shared`'da 3 dokunuş → 1 satır |
| 4. Scaffolding | Düşük | Yeni feature ~1 dakikada iskelet |
| 5. Konsist + docs | Düşük | Yapının zamanla bozulmasını engeller |

Geçiş sonrası "yeni feature ekleme" akışı: iskelet script'ini çalıştır → `shared`'daki feature
listesine 1 satır ekle → ekranı yaz. (Bugünkü 8 dokunuşa karşılık 2 dokunuş.)

## 5. Riskler ve notlar

- **Gradle bu oturumda çalıştırılamıyor** (loopback engeli); her fazın doğrulaması kendi
  makinende `./gradlew :shared:testAndroidHostTest` + iOS CI ile yapılmalı.
- `core:playback-engine` yakın zamanda tamamlanmış ve tam doğrulanmış bir migrasyondan çıktı
  (bkz. hafıza notu / PLAYBACK_COORDINATOR_PLAN.md); Faz 1'de build dosyası convention plugin'e
  geçirilirken **kaynak koduna dokunulmamalı**.
- Faz 2'de `DomainModule.kt`'deki tasarım notu geçerliliğini korur: use case'ler DI bilmez,
  yalnızca kayıt yeri feature'ın Koin modülüne taşınır.
- Fazlar bağımsızdır; her biri ayrı PR olarak, aralarda yeşil test koşusuyla ilerlenmeli.

## 6. Uygulama durumu

Beş faz da uygulandı. Planın öngördüğünden sapan noktalar:

| # | Sapma | Gerekçe |
|---|-------|---------|
| 1 | `shared` convention plugin'e geçirilmedi | Tek iOS framework binary'si onda ve host testleri Android resource istiyor; `withHostTest` ikinci kez çağrılamaz. Kalan 14 modül geçirildi. |
| 1 | Convention plugin'ler `Plugin<Project>` sınıfı, precompiled script (`*.gradle.kts`) değil | Precompiled script derlemek Gradle'ın `kotlin-dsl` plugin'ini gerektiriyor; o da **yalnızca plugins.gradle.org'da** yayınlanıyor. Bu makine hiçbir zaman o host'tan indirme yapmadı (yalnızca dl.google.com, repo.maven.apache.org, jetbrains.space) ve ilk denemede tam olarak orada patladı. `build-logic` artık portala hiç bakmıyor. |
| 2 | `SetPlaybackLooping/Volume`, `Start/CancelSleepTimer` de `feature:player`'a taşındı | Planın kendi kuralı: yalnızca birden fazla feature'ın kullandığı use case `core:domain`'de kalır. Bunları yalnızca player kullanıyordu. |
| 2 | Yeni `core:testing` modülü | Üç feature de aynı üç port fake'ine ihtiyaç duyuyor; `xwab.kmp.feature` bunu her feature'ın `commonTest`'ine otomatik veriyor. `core:domain` kendi fake'lerini yerel tutuyor (aksi halde `core:testing` → `core:domain` döngüsü). |
| 2 | `ToggleFavoriteUseCase` bırakılmadı, silindi | Gövdesi tek delege çağrısıydı (`favoritesRepository.toggle`), kendi testi de delege edenin delege ettiğini doğruluyordu. `category` ve `player` artık `FavoritesRepository` portunu doğrudan alıyor. `core:domain`'de tek use case kaldı: `ToggleMusicPlaybackUseCase` — o gerçekten iki portu birleştiriyor ve bilinmeyen id kararını taşıyor. |
| 5 | Konsist yerine `checkArchitecture` Gradle task'i | Yeni bir dış bağımlılık gerekmiyor, offline çalışıyor ve modül grafiğini gerçek Gradle dependency modelinden okuyor. Üç kural da uygulanıyor. |

Doğrulama durumu:

| Koşu | Sonuç |
|------|-------|
| `./gradlew checkArchitecture` | ✅ |
| `./gradlew testAndroidHostTest` | ✅ (core + üç feature + shared) |
| iOS CI: `iosSimulatorArm64Test` + `:shared:linkDebugFrameworkIosArm64` | ✅ ([koşu 30533555035](https://github.com/SezerUzunca/xwab/actions/runs/30533555035)) |
| `./gradlew :androidApp:assembleDebug` | ✅ |

Yol boyunca migrasyondan bağımsız iki şey çıktı ve ayrı commit'lerde düzeltildi:

- `.gitignore`'daki `**/build/` kuralı convention plugin'lerin Kotlin paket klasörünü
  (`com/xwab/build/`) yuttu; kaynaklar hiç commit edilmedi ve CI descriptor'ları olan boş bir jar
  aldı. Paket `com.xwab.convention` oldu. **Bu repoda `src` altında `build` adlı klasör açma.**
- `main` `e151e80`'den beri iOS'ta kırmızıydı (`core:audio-content`'in `IosAudioFileStore.kt`'si).
  İki Foundation çağrısı Objective-C kategorisi üyesi; Kotlin'e extension olarak geliyorlar ve
  adlarıyla import edilmeleri gerekiyor. `MAX_DOWNLOAD_BYTES` de `const` olamıyor.

`build-logic`'in bağımlılık kuralı: **plugins.gradle.org kullanılmaz.** Hem `pluginManagement` hem
`dependencyResolutionManagement` repo'ları `build-logic/settings.gradle.kts`'te açıkça yazılıdır
(google + Maven Central). Buraya portal-only bir artefakt eklemek build'i bu makinede kırar.
