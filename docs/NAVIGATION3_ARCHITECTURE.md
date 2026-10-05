# Navigation 3 mimarisi ve kapsamı

29 Eylül 2026. Bu revizyon shared navigation katmanını resmi Navigation 3 ve Compose
Multiplatform çözümleriyle düzenler. Feature modülleri tek modül olarak kalır; bir feature
başka feature'ı veya shared'i tanımaz. Seek ve oynatma ilerleme çubuğu eklenmez.

## Sürümler

- Google `navigation3-runtime`: 1.1.7.
- JetBrains `navigation3-ui`: 1.1.2. Google ve JetBrains sürüm numaraları farklıdır;
  bu UI sürümü runtime 1.1.7 ile eşleşir.
- JetBrains `adaptive-navigation3`: 1.3.0-rc01. Material Adaptive API'si deneysel olarak
  işaretlidir; opt-in yalnızca bunu kullanan shared dosyalarındadır.
- Lifecycle ViewModel Navigation 3: 2.11.0; mevcut Metro DI korunur.

Tarif deposunun main dalı alpha/snapshot API kullanabilir. Buradaki kod, projede çözümlenen
sürümlerle derlenir; örneklerdeki sürüm numaraları doğrudan kopyalanmaz.

## Sorumluluklar

| Parça | Sorumluluk |
|---|---|
| `App` | Tema |
| `composition/AppNavigationHost` | Feature entry sözleşmelerini, callback'leri ve now-playing şeridini bağlama |
| `composition/AppEntryMetadata` | Feature'lar arası sunum politikası; resmi pane metadata'sı |
| `navigation/RememberNavigationState` | Her sekmenin `rememberNavBackStack` geçmişi ve seçili sekmenin seri hale getirilmesi |
| `navigation/Navigator` | Push, pop-to-existing, sekme seçimi, yeniden seçim, öğeyi kendi sekmesinde açma ve panel geri oku politikası |
| `navigation/TopLevelDestination`, `FeatureSerializers`, `RetiredRoute` | Sekme listesi, kayıtlı route'ların serileştirilmesi ve kaldırılmış route'ların güvenle atılması |
| `ui/TabEntries` | Aktif olmayan sekmeler dahil saveable-state ve ViewModel dekoratörlerini yaşatma |
| `ui/TabEntryProvider` | Sekmeye özgü saveable content key ve metadata (sekme, panel rolü, hedefin kendisi) |
| `ui/AppNavigationDisplay` | Resmi `NavigationSuiteScaffold` (bar/rail), içinde `NavDisplay`, Material list–detail sahne stratejisi ve now-playing şeridi yuvası |
| `ui/NavigationTransitions`, `ui/EntryMetadataKeys` | Sekme ve ileri/geri geçişleri; sekme ve ebeveyn panel metadata anahtarları |
| `ui/AdaptiveBackControl` | Resmi `NavEntryDecorator` (saveable-state ve ViewModel dekoratörlerinden sonra): gerçek ebeveyn paneli görünüyorsa tekrarlı geri düğmesini gizleme; birden fazla panel görünürken geri okunu kendi panelini kapatan `Navigator.goUp`'a bağlama. `NavEntry` anahtarı dekoratöre kapalı olduğu için hedef `DestinationKey` metadata'sıyla taşınır |

`navigation` durumu ve kuralları tutar, Compose çizimi içermez; `ui` bu durumu ekrana taşır.
Bağımlılık tek yönlüdür: `ui` → `navigation`. Mimari kural gereği `ui` feature'lara dokunamaz;
feature bilen tek paketler `composition`, `navigation` ve `di`'dır.

`NavController` ve feature'lar arası route bağımlılığı yoktur. Saved-state formatındaki mevcut
`@SerialName` değerleri ve route argümanları değiştirilmez. KMP için açık serializer modülü
kullanılır; Android'e özgü reflection çözümü common koduna taşınmaz.

## Projedeki karşılıklar

| Navigation 3 yeteneği | Projede kullanımı |
|---|---|
| `NavKey` ve entry-provider DSL | Her feature kendi navigation paketinden yayınlar |
| Kalıcı back stack | Her sekme ayrı `rememberNavBackStack` taşır |
| Seçili sekmeyi saklama | `rememberSerializable` ve aynı polymorphic serializer konfigürasyonu |
| Çoklu geçmiş | Browse, Favorites, Stories; diğer sekmeden geri başlangıç sekmesine döner |
| Saveable entry state | `rememberSaveableStateHolderNavEntryDecorator` |
| Entry ViewModel ömrü | Saveable dekoratörden sonra `rememberViewModelStoreNavEntryDecorator`; pop store'u temizler |
| Route argümanları | Category, Sound ve Story kimlikleri entry içinde ViewModel oluşturulurken verilir |
| Ortak uygulama durumu | Entry ve root ViewModel'leri aynı app-scoped playback portunu gözlemler |
| Uygulama chrome'u | Resmi `NavigationSuiteScaffold` pencere boyutu sınıfına göre kısa bar veya geniş ray seçer; chrome her ekranda aynı olduğu için `NavDisplay` dışında durur ve tek örnektir |
| Küçük ekran | `SinglePaneScene` fallback; tam ekran hedefler |
| Adaptif list–detail | Resmi Material `rememberListDetailSceneStrategy` |
| Extra pane | Browse listesinin yanında Category detail, ardından Sound extra pane. Altında Category olmayan ses (now-playing şeridinden kökten açılan) tek panel gösterilir. Şerit sesi her zaman kökten açtığı için altındaki kategori her zaman sesin kendi kategorisidir |
| Boş detail | Resmi `listPane(detailPlaceholder=...)` ile kategori/ses/hikâye seçim mesajı |
| İleri/geri animasyon | `transitionSpec` ve `popTransitionSpec`; RTL yönü korunur, sekmeler fade kullanır |
| Predictive Back | `predictivePopTransitionSpec`; Material sahne içindeki geri hareketini kendi işler |
| Yaşam döngüsüne bağlı dokunma | Feature navigation kontrollerinde `dropUnlessResumed` |
| Sekme yeniden seçimi | Root'a dönme, root'ta yeniden seçim event'iyle listeyi başa kaydırma |
| Şeritten öğe açma | Now-playing şeridi çalan öğeyi kendi sekmesinde (ses → Sounds, hikâye → Stories) sekmenin kökünden yeni seçim olarak açar; öğe o stack'te zaten açıksa oraya döner. Bulunulan sekmenin stack'i değişmez |
| Seçim değiştirme | Resmi tariflerdeki gibi yeni hedef eklenir (Rain açıkken Ocean). Aynı paneldeki eski seçimleri Material'in `PopUntilCurrentDestinationChange` geri davranışı tek geri işlemiyle atlar |
| Sürüm geçişi | Kaldırılan route'ların güvenli okunması ve stack'ten çıkarılması korunur |

Material sahne anahtarı sekmeye özgüdür. Browse içindeki Sound ile Favorites içindeki aynı
Sound'un content key'leri de farklıdır. Uygun pane grubuna ait olmayan hedef tek ekran olarak
gösterilir. Böylece bir ses listesiyle hikâye
detayının veya iki sekmenin sahnelerinin yanlış eşleşmesi engellenir.

Geri davranışı Material'in resmi `BackNavigationBehavior.PopUntilCurrentDestinationChange`
seçeneğidir: geri işlemi en son hedefin paneli değişene kadar geçmişi atlar. Liste yanında
Rain, Ocean ve Forest sırayla seçildiyse tek geri işlemi listeye döner; kategori yanında seçilen
sesler de tek geri işlemiyle kategoriye döner. 30 Eylül'de üç seçenek gerçek stack'lerle
ölçüldü:

- Varsayılan `PopUntilScaffoldValueChange`: liste boş detay yer tutucusunu gösterdiğinde
  scaffold değeri değişmediği için geri hedefi bulunamaz; Favorites'teki bir sesten geri,
  listeyi atlayıp başlangıç sekmesine geçer. Kullanılmaz.
- `PopLatest`: her seferinde tek hedef kaldırır; eski seçimler tek tek dolaşılır. Bu yüzden
  önceden `Navigator` aynı türden hedefi değiştiriyordu; bu özel kural kaldırıldı.
- `PopUntilCurrentDestinationChange`: yukarıdaki davranış. Kullanılır.

Telefonda liste ile detay aynı anda görünmediği için seçimler birikmez. Now-playing şeridi
öğeyi kendi sekmesinin kökünden açar; geri o sekmenin listesine döner.

Bu kurallardaki özel kısım yalnızca politikadır; mekanizmalar resmidir. Sekme kimliği resmi
`NavEntry.contentKey`, geri oku resmi `NavEntryDecorator`, iOS dahil kayıt resmi
`SavedStateConfiguration` ve `rememberSerializable` ile kurulur. Aynı stack'te aynı içerik
anahtarı iki kez bulunamaz (saveable state holder reddeder); var olan hedefe dönmek bunun
çözümlerinden biridir ve uygulamanın seçimidir. Metadata birden fazla değer taşıyan bir haritadır;
uygulamanın seçtiği şey, bir hedefin o geçmişte hangi panel rolünü alacağıdır.
[Resmi API açıklaması](https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigation/BackNavigationBehavior)
geniş ekranda aynı panelde farklı içeriklere gidildikten sonra pencere daraltılırsa
geçmişin kullanıcı için beklenmedik olabileceğini belirtir; her iki düzende aynı ekranların
görüneceği iddiası yapılmaz. Katlanır cihaz düzenleme ve pane hareketi Material
kütüphanesine bırakılır; ayrıca el yapımı list–detail sahnesi tutulmaz.

## Ürün akışı gerektiren diğer tarifler

Resmi tarifler bir zorunluluk listesi değildir. Birbirinin alternatifi olan çözümleri aynı
uygulamaya eklemek veya kullanılmayan route/dekoratör üretmek kapsamı tamamlamaz.

| Tarif / seçenek | Bu projedeki durum |
|---|---|
| DialogScene / BottomSheet OverlayScene | Back stack'e ait dialog/sheet hedefi yok. Tam ekran oynatıcı yoktur; zamanlayıcı ses/hikâye detayındaki ekran içi karttır. Böyle bir hedef eklenirse resmi overlay çözümü kullanılmalıdır. |
| Supporting pane / el yapımı iki panel | Mevcut hiyerarşi resmi list–detail–extra ile karşılanır; paralel alternatif sahne eklenmez. |
| Pane genişliği için sürükleme | Adaptif sahnenin isteğe bağlı drag handle'ı etkin değildir; otomatik yerleşim kullanılır. |
| Deep link ve sentetik geçmiş | Mevcut uygulamada dış URL/intent navigation sözleşmesi yoktur; bu revizyon deep link yayınlamaz. URL biçimi ve platform girişleri tanımlandığında resmi deep-link tarifine göre ayrıca uygulanmalıdır. |
| Conditional / auth navigation | Hesap, giriş ve onboarding akışı yoktur. |
| Entry'den sonuç döndürme | Seçim/edit sonucu döndüren bir hedef yoktur; favori ve playback değişimleri ortak port state'inden gözlemlenir. |
| Shared ViewModel / `retain` dekoratörü | Entry dışı retained nesne veya feature ViewModel paylaşma ihtiyacı yoktur. Playback oturumu zaten uygulama ömründedir. |
| Hilt / Koin / api–impl split | Metro ve projenin düz feature kuralının alternatifleridir; projeye eklenmez. |
| Dynamic Feature navigation | İndirilebilir feature modülü yoktur. |
| Parcelable stack | Common Android/iOS route'ları Kotlin serialization kullanır. |
| Fragment / View interop | Uygulama Compose Multiplatform ekranlarından oluşur. |

## Resmi belgelerle karşılaştırma

29 Eylül 2026 tarihinde resmi belgeler tekrar kontrol edildi. Uygulamanın kullandığı
karşılıklar aşağıdadır; tablo uygulama politikası ile kütüphane API'sini ayırır.

| Kontrol | Uygulama | Resmi dayanak |
|---|---|---|
| Uygulama chrome'u | Chrome her sahnede aynı olduğundan `NavDisplay` dışında, Material `NavigationSuiteScaffold` içindedir (resmi `commonui` tarifinin yaklaşımı). Sahne dekoratörü, chrome sahneye göre değiştiğinde gerekir; burada gerekmez. | [Common navigation UI tarifi](https://github.com/android/nav3-recipes), [NavigationSuiteScaffold](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation) |
| Adaptif ekranlar | Resmi `rememberListDetailSceneStrategy`, `listPane/detailPane/extraPane` ve `detailPlaceholder` kullanılır. Özel list–detail sahnesi yoktur. | [Material Adaptive sahneleri](https://developer.android.com/guide/navigation/navigation-3/scenes) |
| Animasyon | `NavDisplay` üç transition spec'i kullanır; hedefe özel metadata geçişi yoktur. | [Navigation animasyonları](https://developer.android.com/guide/navigation/navigation-3/animate-destinations) |
| State ve ViewModel | Her sekmenin decorator sırası saveable-state, ardından ViewModel store'dur. Aktif olmayan sekmelerin decorator'ları composition'da tutulur. | [State ve ViewModel](https://developer.android.com/guide/navigation/navigation-3/save-state), [Çoklu geçmiş](https://developer.android.com/guide/navigation/navigation-3/recipes/multiple-backstacks) |
| KMP serialization | Feature serializer modülleri birleşir; `rememberNavBackStack` açık `SavedStateConfiguration` alır. Android reflection overload'u common kodda kullanılmaz. | [KMP route serialization](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html) |

Panel içi geri oku ile sistem geri işlemi ayrı kurallardır. Sistem geri işlemi en sağdaki
paneli kapatır. Kategori ve ses yan yana görünürken bu panel sestir. Kategori panelindeki
ok ise kategori panelini ve ondan açılan sesleri kapatır; o panelde daha önce seçilmiş
kategoriler de gider (`Navigator.goUp`). Tek panelde ok, hedefin kendi geri eylemini kullanır.

`Navigator`'ın pop-to-existing, reselect, replacement ve panel oku kuralları, `TabKey` ile
sekme kimliği ve `ParentPaneKey` ile Up görünürlüğü uygulama politikasıdır; bunlar resmi
API gibi sunulmaz. `RetiredRoute` de eski sürümden gelen tanınmayan kayıtlı hedefleri
temizlemek için kullanılan aktif migration kodudur. Resmi kütüphane bu ürün politikasını
kendiliğinden uygulamaz; kullanım ve regresyon testleri olduğu için kaldırılmamıştır.

## Doğrulama

- `NavigatorTest`: tab seçimi, reselect, tekrar dokunma, pop-to-existing, entry replacement,
  root koruması ve geçersiz stack'in reddi.
- `AppEntryMetadataTest`: resmi Material stratejisiyle compact fallback, list/detail/extra,
  placeholder, sekme izolasyonu ve `PopUntilCurrentDestinationChange` ile eski seçimlerin tek
  geri işlemiyle atlanması.
- Serializer, retired-route ve content-key regresyon testleri korunur.
- `src/composeTest/.../NavigationCompositionTest`: gerçek `AppNavigationDisplay` üzerinden
  entry store ayrılığı, sekme değiştirme, recreation, saveable state, pop temizliği ve tek
  root chrome ViewModel'i. ViewModel'ler üretimdeki gibi MetroX ile alınır; entry ViewModel'inin
  `SavedStateHandle`'ı recreation'dan sonra kendi entry'sine döner, bu da dekoratör sırasını
  sınar. Compact/adaptif düzen değişiminde entry state'inin korunması ve
  görünür ebeveyn panelinde gereksiz geri kontrolünün gizlenmesi de sınanır. Sistem geri işlemi
  `Navigator` çağrılmadan, `NavDisplay`'in dinlediği navigation event dispatcher'ına gerçek
  geri olayı (tamamlanan ve predictive) gönderilerek sınanır: telefonda tek ekran kapanır,
  başlangıç sekmesine düşülür; liste yanında tek geri işlemi paneldeki eski seçimleri de atlar
  (`PopLatest` ile bu test düşer). Android cihaz ve iOS simulator source set'leri aynı testleri
  kullanır; CI bunları iOS simulator'da koşar, Android'de yerel cihazda koşulur.
- `src/composeTest/.../RetiredRouteRestoreTest`: eski bir sürümün kaydettiği, argüman taşıyan ve
  bu sürümde olmayan bir route içeren back stack, `rememberNavBackStack`'in kullandığı saved-state
  biçiminde gerçek platformda çözülür; route `RetiredRoute` olarak okunur ve temizlenir.
- İçerik anahtarı ve sekme kimliği `toString()` yerine route'un kayıtlı biçiminden
  (`@SerialName` ve argüman değerleri, `savedIdentity`) türetilir; sınıf adı değişse de kayıtlı
  arayüz durumu korunur. Feature'ın kendi seçtiği içerik anahtarı korunur.
- `checkArchitecture` kural 23: feature'da tanımlanan her route kendi serializer modülüne kayıtlı,
  her feature'ın serializer modülü de shell'deki `FEATURE_SERIALIZERS`'a eklenmiş olmalıdır;
  aksi durumda eksik yalnızca bir sonraki açılıştaki geri yüklemede ortaya çıkardı.
- Statik analiz, mimari kontrol, Android lint ve APK derlemesi.

Windows üzerinde iOS framework derlemesi ve simulator testleri çalıştırılamaz. Katlanır cihaz
donanımı ve gerçek iOS geri hareketi bu ortamın test kapsamının dışındadır.

## Resmi kaynaklar

- [Navigation 3 tarif kataloğu](https://github.com/android/nav3-recipes)
- [Adaptif navigasyon (NavigationSuiteScaffold)](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- [Material Adaptive sahneleri](https://developer.android.com/guide/navigation/navigation-3/scenes)
- [İleri, geri, predictive Back ve shared transition](https://developer.android.com/guide/navigation/navigation-3/animate-destinations)
- [Çoklu back stack](https://developer.android.com/guide/navigation/navigation-3/recipes/multiple-backstacks)
- [State saklama ve entry dekoratörleri](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [Compose Multiplatform Navigation 3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [Compose Multiplatform adaptif düzenler](https://kotlinlang.org/docs/multiplatform/compose-adaptive-layouts.html)
- [Adaptif geri davranışları](https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigation/BackNavigationBehavior)
- [AndroidX Test sürüm notları](https://developer.android.com/jetpack/androidx/releases/test)
