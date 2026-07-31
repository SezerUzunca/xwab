# PlaybackCoordinator — ne yapıldı, ne kaldı?

> 2026-07-28 güncellemesi: Bu belge ilk playback ayrımının tarihsel karar kaydıdır. O tarihte
> kapsam dışı bırakılan resource/katalog ayrımı, hibrit katalog ihtiyacı doğunca
> Güncel yapıda `core:data` içinde uygulanmıştır; `Music.audioResource` kaldırılmış, paketli MP3'ler ve
> uzak HTTPS kaynakları aynı manifestin sahibi olmuştur. Aşağıdaki “yapılmayacak” ifadeleri ilk
> çalışmanın kapsamını anlatır, mevcut mimariyi değil.
>
> 2026-07-30 güncellemesi: Ayrı `core:domain` modülü kaldırıldı. `AudioContentResolver`
> `core:data`ya, `PlaybackCoordinator` `core:playback`a ve ortak saf `PlaybackSummary`
> `core:model`a taşındı.
>
> 2026-07-31 güncellemesi: Paketli MP3'ler tamamen kaldırıldı. Aşağıda geçen
> `files/audio/*.mp3` composeResources ve üretilen `Res` sınıfı artık yok; her parça HTTPS'ten
> çalınıp yerele indiriliyor. Katalog `core:audio-content` içinde.

Kapsam: port [PlaybackCoordinator.kt](core/playback/src/commonMain/kotlin/com/xwab/app/core/playback/PlaybackCoordinator.kt),
adaptör [DefaultPlaybackCoordinator.kt](core/playback/src/commonMain/kotlin/com/xwab/app/core/playback/DefaultPlaybackCoordinator.kt)
ve testi [DefaultPlaybackCoordinatorTest.kt](core/playback/src/commonTest/kotlin/com/xwab/app/core/playback/DefaultPlaybackCoordinatorTest.kt).

| Adım | Konu | Durum |
|---|---|---|
| 1 | Davranışı kilitleyen testler | **Yapıldı** |
| 2 | State sahipliğini tekilleştir | **Yapıldı** |
| 6 | İsimlendirme + ölü esneklik | **Yapıldı** |
| 4 | Modül taşıması + bağımlılık yönü | **Yapıldı** (Seçenek A) — bkz. §5 |
| 5 | Domain sızıntısını kapat | **Yapıldı** — bkz. §5.3 |
| 3 | Resource eşlemesini ayır | **Yapılmayacak** — bkz. §4 |
| 7 | Dış inceleme bulguları | **Yapıldı** — bkz. §8 |

---

## 1. Teyit edilen tespitler

| # | Tespit | Durum |
|---|---|---|
| 1 | Coordinator bir repository değil; session façade. `PlaybackCoordinator` adı doğru. | Doğrulandı |
| 2 | `core:data` içinde bulunması katman sorumluluğunu bozuyor. | Doğrulandı — Adım 4 |
| 3 | `core:domain → core:data` bağımlılığı Clean Architecture'a ters. | Doğrulandı — Adım 4 |
| 4 | `loopMode`/`volume` cache'i ikinci bir state sahibi yaratıyor. | Doğrulandı — düzeltildi, §2 |
| 5 | `toPlaybackRequest(autoplay)` yalnız `true` ile çağrılıyor. | Düzeltildi |
| 6 | `playbackSettingsAreRetainedWhenAnotherSoundIsLoaded` adının iddia ettiğini test etmiyor. | Düzeltildi |
| 7 | Testte coordinator nesneleri `repository` diye adlandırılmış. | Düzeltildi |

---

## 2. Çift state sahipliği — bulgu ve düzeltme

Coordinator, controller state'ini yayımlarken ayrıca kendi `loopMode`/`volume` alanlarını
tutuyordu ve yeni istek hazırlarken hangisini kullanacağına `current.source != null` ile karar
veriyordu. `core:playback-engine` okunduğunda bu gate'in **yanlış sinyale** baktığı görüldü:

- [PlaybackReducer.kt:125](core/playback-engine/src/commonMain/kotlin/com/xwab/app/core/media/PlaybackReducer.kt:125)
  — `reduceSetLooping`/`reduceSetVolume` `desired.isLooping`/`desired.volume`'u **koşulsuz**
  günceller; yani kaynak yüklenmeden yapılan seçimi medya katmanı zaten hatırlıyor.
- [PlaybackReducer.kt:440](core/playback-engine/src/commonMain/kotlin/com/xwab/app/core/media/PlaybackReducer.kt:440)
  — bağlantı koptuğunda yalnızca `observed.source` null'lanıyor; `desired` korunuyor.
- [PlaybackProjection.kt:33](core/playback-engine/src/commonMain/kotlin/com/xwab/app/core/media/PlaybackProjection.kt:33)
  — yayımlanan `isLooping` **`desired.isLooping`**, disconnect'te hayatta kalıyor.
- [AndroidPlaybackFacade.kt:379](core/playback-engine/src/androidMain/kotlin/com/xwab/app/core/media/AndroidPlaybackFacade.kt:379)
  — controller yokken yayımlanan `volume` da `desired.volume`'a düşüyor.

Yani `source == null` olduğu anda `desired` değerleri hâlâ doğruydu; coordinator gereksiz yere
bayat kendi kopyasına düşüyordu. Uygulanan düzeltme:

- `private var loopMode` ve `private var volume` **silindi**.
- Gate `current.source != null` → `current.activeSource != null` oldu (`activeSource` =
  `source ?: requestedSource`, disconnect'te dolu kalır).
- İstek artık `current.isLooping` ve `current.volume`'u taşıyor.
- Yeni `settingsChangedOutsideTheAppSurviveALostServiceConnection` testi bu senaryoyu kilitliyor;
  eski kodda kırmızı olurdu.

**Cache tamamen silinemedi — bilerek.** Geriye tek bir `loopPreferenceEstablished: Boolean` kaldı.
Sebebi: `DesiredPlayback.isLooping` varsayılanı `false`, ürün varsayılanı ise `LoopMode.One`
(uyku sesleri döngüde başlar). Yayımlanan state "hiç dokunulmadı" ile "kullanıcı kapattı"yı
ayırt edemiyor; bu bayrak tam olarak o ayrımı yapıyor ve ilk yüklemeden sonra bir daha okunmuyor.

Denenip **reddedilen** alternatifler:
- `DesiredPlayback.isLooping` varsayılanını `true` yapmak → ürün politikası altyapı katmanına sızar.
- Kurulumda `SetLooping(true)` göndermek → Android'de `ensureConnectedOrApply` controller yoksa
  `connection.connect()` çağırıyor, yani uygulama açılışında PlaybackService'e bağlanırdı.

`core:playback-engine` bu yüzden **hiç değiştirilmedi**; düzeltme tamamen `core:playback` içinde kaldı.

---

## 3. Yapılan isimlendirme değişiklikleri

- `PlaybackCoordinator.toggle(music)` → `togglePlayback(music)`
  (tarihsel çağıran: `PlaybackUseCases.kt`).
- `toPlaybackRequest(autoplay: Boolean)` parametresi kaldırıldı; her çağrı `true` idi.
- Testteki `val repository = DefaultPlaybackCoordinator(...)` → `coordinator`; test adları
  `togglePlayback...` ile hizalandı.
- `PlaybackCoordinator` → `MusicPlaybackSession` yeniden adlandırması **yapılmadı**: mevcut ad
  rolü doğru anlatıyor, değişiklik `core:domain` + DI + testlerde saf churn olurdu.

Test fake'i (`FakePlaybackController`) artık gönderilen komutları state'e yansıtıyor. Bu gerçek
davranışın modeli: her iki facade da `submit` içinde, dönmeden önce publish ediyor.

---

## 4. Adım 3 kapatıldı — yapılmayacak

İki parçası vardı, ikisi de gerekçesiyle düşürüldü.

**`Music → AudioSource` eşlemesini ayrı bir `AudioSourceFactory`'ye çıkarmak.** Yalnız dosya
sayısını artırır: `audioUriForResource` zaten enjekte edilebilir bir fonksiyon parametresi,
yani test edilebilirlik için ayırmaya gerek yok. Coordinator Adım 2 ve 5'ten sonra zaten
neredeyse tamamen politika; eşleme dört satır.

**`Music.audioResource`'u modelden kaldırıp resource'u `Music.id`'den çözmek.** Rapor bunu
"altyapı yolunun üst katmanlara taşınması" diye işaretledi ve ilk planda kabul etmiştim; somut
uygulamaya bakınca ikna edici değil:

- Alan bir `String`. Hiçbir tip bağımlılığı yaratmıyor — Adım 5'ten sonra `core:model` ve
  `core:domain`'in `core:playback-engine`'e bağımlılığı zaten sıfır.
- Kaldırmanın iki yolu var, ikisi de daha kötü: (a) `core:playback` içinde ikinci bir
  `id → dosya` tablosu — katalogla sessizce ayrışabilecek bir kopya; (b) isim kuralına
  güvenmek (`gentle-rain` → `gentle_rain.mp3`) — bugün beş girdinin beşi uyuyor ama kural
  derleyici tarafından zorlanmıyor, uymayan bir id çalışma zamanında patlar.
- Bugün katalog ile dosyalar tek yerde eşleşiyor ve `BundledAudioAssetsTest` bu eşleşmeyi
  doğruluyor. Her iki alternatif de bu tek doğrulama noktasını bölerdi.

Kazanç kozmetik, bedel gerçek. Yapılmadı.

---

## 5. Adım 4 — yapılan modül taşıması (Seçenek A)

Kontrat sahipliğini düzeltmenin ön koşulu coordinator'ı `core:data` dışına çıkarmaktı: yalnız
repository arayüzlerini taşımak `core:data → core:domain → core:data` Gradle döngüsü yaratırdı.
İkisi birlikte yapıldı.

### 5.1 Yeni yapı

```text
core:model    (bağımsız)
core:playback-engine (bağımsız)
core:data     → core:model                  # repository/resolver portları + implementasyonları
core:playback → core:data, core:playback-engine, core:model # playback portu + oturumu
shared        → hepsi (composition root)
feature:*     → core:data, core:playback, core:model, core:designsystem
```

Bağımlılık yönü artık `data/playback → domain`. Gradle döngüsü yok.

### 5.2 Somut değişiklikler

- Yeni `core:playback` modülü: `DefaultPlaybackCoordinator`, `playbackCoordinatorModule` ve
  `files/audio/*.mp3` composeResources. Generated `Res` sınıfı
  `xwab.core.playback.generated.resources` oldu.
- Repository/resolver arayüzleri `com.xwab.app.core.data`, playback arayüzleri
  `com.xwab.app.core.playback` altında kendi implementasyonlarıyla birlikte tutuluyor.
- Ayrı `core:domain` modülü kalmadı; capability sözleşmeleri sahip oldukları modüllere taşındı.
- `core:data` paketli ses kaynakları için Compose eklentisini koruyor fakat playback engine
  tiplerini görmüyor. Public yüzeyi repository portları ile Koin modülleri.
- DI `dataModule` + `dataPlatformModule` + `playbackCoordinatorModule` olarak ayrıldı. Son adın
  `playbackModule` olmamasının sebebi `core:playback-engine`'in o adı zaten kullanması.

### 5.3 Adım 5 — domain sızıntısı kapatıldı

Port artık yalnız domain tipleri yayımlıyor:

```kotlin
val playback: Flow<PlaybackSummary>
val sleepTimerRemainingMs: Flow<Long?>
```

- `PlaybackSummary`, üç feature'ın paylaştığı engine-bağımsız model olarak `core:model`a taşındı.
- `AudioPlayerState.toPlaybackSummary()` eşlemesi `core:playback` adaptörüne indi.
- `core:domain` `api(projects.core.playbackEngine)`'ı bıraktı. `core:playback-engine` artık yalnız kendi,
  `core:playback` ve `shared` içinde görünüyor; `core:domain`, `core:data`, `core:model` ve
  bütün `feature:*` modülleri temiz.
- `StateFlow` → `Flow` dönüşümü davranışı değiştirmiyor: use case'ler bu akışları yalnız
  `combine` içinde kullanıyor, hiç `.value` okumuyor ve `StateFlow.map` her yeni collector'a
  mevcut değeri veriyor.

Yeni testler: özet eşlemesi, reconnect sırasında `activeSourceId`'nin requested source'a
düşmesi ve sleep timer'ın ham milisaniye olarak yayımlanması.

### 5.4 Son bağımlılık grafiği

```text
core:model    (bağımsız)
core:playback-engine (bağımsız)
core:data     → core:model # repository/resolver portları + implementasyonları
core:playback → core:data, core:playback-engine, core:model
shared        → hepsi (composition root)
feature:*     → core:data, core:playback, core:model, core:designsystem, core:navigation
```

---

## 6. Doğrulama

Bütün adımlar (1–8) Android/common tarafında **yeşil** doğrulandı.

```bash
./gradlew :shared:allTests :core:data:allTests :core:playback:allTests
```

**Önceki yapı iOS'ta yeşildi.** [PR #2](https://github.com/SezerUzunca/xwab/pull/2) üzerinde
[run 30340856100](https://github.com/SezerUzunca/xwab/actions/runs/30340856100): yedi modülün
simülatör testleri koştu (`core:data`, `core:playback-engine`, `core:model`,
`core:navigation`, `core:playback`, `shared`), on beş modülün test binary'si linklendi ve
`:shared:linkDebugFrameworkIosArm64` ile cihaz framework'ü de bağlandı.

Bu koşu, 2026-07-30 tarihli paket ve modül taşımalarından önce yapıldığı için mevcut çalışma
ağacının iOS doğrulaması sayılmaz. Güncel yapı Windows'ta Android/common tarafında doğrulandı;
`audiocontent` → `data` paket değişikliği dahil iOS simülatör testleri ve framework linkleme
macOS CI'da yeniden çalıştırılmalıdır.

⚠️ **iOS CI `macos-26` gerektiriyor.** `macos-15` (Xcode 16.4, iOS 18.5 SDK) Compose'a bağlı bir
modülün test binary'sini linkleyemiyor: `compose.ui:ui-uikit` içindeki `CMPLayoutRegion`,
yalnız iOS 26 SDK'sında bulunan `UIViewLayoutRegion`'a referans veriyor. Bu, workflow yalnız
`core:playback-engine`'i derlerken görünmüyordu — o modülün Compose bağımlılığı yok.

Adım 4 DI kayıtlarını böldüğü için uygulamanın gerçekten açılması da kontrol edilmeli
(Koin çözümlemesi çalışma zamanında patlar, derlemede değil). `core:playback-engine` instrumentation
paketi disconnect/reconnect senaryolarını kapsıyor:

```bash
./gradlew :core:playback-engine:connectedAndroidTest
```

macOS'ta ayrıca `-PenableIos=true` ile iOS derlemesi.

---

## 7. Bilerek yapılmayacaklar

- **`PlaybackRepository` adı verilmeyecek.** Coordinator CRUD/kalıcılık yapmıyor.
- **Ayrı bir playback repository eklenmeyecek.** Playback canlı bir oturum kontrolü. Yalnız
  loop/volume uygulama yeniden açıldığında korunacaksa `PlaybackPreferencesRepository` anlamlı
  olur — bu bir ürün kararı, mimari eksiklik değil.
- **Tek satırlık playback use case'leri silinmeyecek.** Davranışları az ama ViewModel'lerin
  data/media katmanına doğrudan erişmesini engelliyorlar.

---

## 8. Dış incelemeden gelen düzeltmeler

Bağımsız bir inceleme dört borç bildirdi; dördü de koda karşı doğrulandı ve kapatıldı.

**8.1 DI testi production listesini doğrulamıyordu.** `AppModulesTest` modülleri elle sıralıyor,
`appModules()`'ı çağırmıyordu; yani `playbackCoordinatorModule` production listesinden düşse test
yine geçerdi. Artık iki yarım var ve ikisi de gerekli: `theApplicationShipsTheModulesUnderTest`
gerçek `appModules()` içeriğini sabitliyor, `everyUseCaseResolvesFromTheAssembledGraph` o listenin
çözüldüğünü kanıtlıyor. Tek başına her biri, uygulama açılışta çökerken de yeşil kalabilir.

**8.2 Favorites geçici hatadan toparlanmıyordu.** `catch` fallback'ini yayımladıktan sonra akışı
sonlandırıyor, dolayısıyla favoriler o abonelik boyunca ölü kalıyordu. Öncesine sınırlı bir
`retryWhen` eklendi (3 deneme, 150 ms ara); kalıcı hatada eski boş-set davranışı korunuyor.
Yeni test geçici hatadan sonra gerçekten toparlanmayı doğruluyor.

**8.3 `core:domain` Koin'e bağlıydı.** `domainModule` composition root'a (`shared`) taşındı ve
`core:domain` Koin bağımlılığını bıraktı — artık plain Kotlin. Adaptör modülleri (`core:data`,
`core:playback`) kendi Koin modüllerini tutmaya devam ediyor: bağladıkları implementasyonlar
`internal` ve sırf `shared`'dan kaydedebilmek için public yapmak, saflık uğruna API yüzeyini
genişletmek olurdu.

**8.4 Doküman bayattı.** Bu dosyanın başındaki iki link silinmiş dosyaları gösteriyordu; DI
testinin derlenmediği yazıyordu; asset testinin yorumu ise §4'ün yapılmayacağını söylediği
`Music.id` resolver'ına atıf yapıyordu. Üçü de düzeltildi.

**Kapsam dışı bırakılanlar.** İncelemenin online katalog bölümü (offline-first hibrit, Room KMP +
Ktor, `CatalogSync`, `AudioDownloadRepository`, manifest'ten katalog üretimi, arama/paging) ürün
kararıyla kapsam dışı: katalog ve ses dosyaları değişmeyecek. `Music.audioResource` → `assetId`
önerisinin gerekçesi de remote/downloaded kaynaklardı; §4 kararı bu kısıt altında geçerliliğini
koruyor. `PlaybackSummary` daha sonra üç feature'ın ortak engine-bağımsız modeli olduğu için
`core:model`a taşındı.
