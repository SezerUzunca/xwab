# UI ve UX düzeltmeleri

28 Eylül 2026. Bu belge, aynı tarihli analizdeki 12 bulgu için uygulanan değişiklikleri kaydeder. Kullanıcının sonraki kararı doğrultusunda ilerleme çubuğu ve seek/±15 saniye kontrolleri kaldırılmıştır.

29 Eylül mimari güncellemesi: Oynatıcı artık tam ekran `NowPlayingRoute` hedefidir.
Güncel navigation yapısı ve test sonuçları [Navigation 3 mimarisi](NAVIGATION3_MIMARISI.md)
belgesindedir. Aşağıdaki 28 Eylül test kayıtları o günkü sürüme aittir.

29 Eylül inceleme düzeltmeleri:

- **Zamanlayıcı:** Oynatmadan önce kurulabilir. Oynatıcı ekranındaki zamanlayıcı hiçbir şey
  çalmıyorken de etkindir. Ses ve hikâye detaylarındaki `Set sleep timer` düğmesi oynatıcıyı açar.
  Hiçbir şey istenmemişken çalışan bir zamanlayıcı varsa mini oynatıcı "Sleep timer" çubuğu olarak
  görünür kalır, böylece iptal edilebilir.
- **Undo bildirimi:** Ekrandan ayrılıp dönünce bildirim yalnız kaldırmadan sonraki 10 saniye içinde
  ve ses hâlâ favori dışındaysa yeniden gösterilir. Başarısız geri almada bildirim tekrar çıkmaz;
  yeniden deneme düğmesi kalır.
- **Mini oynatıcı:** Yükleme sırasında durum satırı `Loading…` der; dokunma etiketi görünür metin
  olarak gösterilmez.
- **Çok panel:** Bir paneldeki geri oku o paneli ve ondan açılanı kapatır. Ebeveyn paneli
  görünürken geri oku gizlense bile ses detayındaki favori düğmesi sağda kalır.
- **Erişilebilirlik:** Favori düğmesinin adı durumdan bağımsızdır (`Favorite <ad>`); açık/kapalı
  durumu ayrıca duyurulur. Hikâyeler başlığı başlık semantiği taşır.
- **Temizlik:** Kullanılmayan `Track.playbackTitle` kaldırıldı. Hikâye detayı ses detayıyla aynı
  hizalamayı kullanır.

29 Eylül emülatör incelemesi düzeltmeleri:

- **Zamanlayıcı:** Oynatıcıda oynatma düğmesinin hemen altında, kendi kartında duruyor. Süreler
  çerçeveli düğmeler; çalışan zamanlayıcı vurgulu kalan süre ile yanındaki iptal düğmesinden oluşuyor.
- **Detaylar:** Zamanlayıcı çalışırken detaylardaki düğme `Sleep timer · 28:01` gösteriyor.
  Çalan öğenin kendi detayındayken mini oynatıcı gizleniyor; aynı düğmeler iki kez görünmüyor.
- **Bildirim:** Undo bildirimi açık renk yerine uygulamanın koyu yüzey renginde.
- **Süre:** Seslerde süre `0:12 loop` olarak yazılıyor; hikâyelerde düz süre kalıyor.
- **Oynatıcı düzeni:** Başlık, oynat düğmesi ve `View details` ortalı; ses seviyesi ve tekrar
  zamanlayıcının altında.
- **Tutarlılık:** Üç sekmenin başlıkları aynı boyutta. Mini oynatıcı `Playing · Timer off` diyor.

## Uygulanan değişiklikler

| Analiz bulgusu | Yeni davranış |
|---|---|
| 1. Detaydaki ayarlar başka içeriği etkiliyor | Ses ve hikâye detayları yalnız kendi içerik eylemlerini sunar. Ses seviyesi, tekrar ve zamanlayıcı, aktif içeriğin başlığını gösteren ortak oynatıcı panelindedir. |
| 2. Hikâyede tekrar tercihi görünmüyor | `Repeat playback` aynı panelde sesler ve hikâyeler için görünür. Kullanıcının mevcut global tercihi korunur. |
| 3. Benzer satırların dokunma davranışı farklı | Ses, favori ve hikâye satırları detay açar; ayrı oynatma düğmesi oynatır/duraklatır. |
| 4. Mini oynatıcı hikâyeyi buldurmuyor | Mini oynatıcı her içerik için aynı paneli açar. `View details`, çalan sesin veya belirli hikâyenin detayına gider. |
| 5. Favorilerden kaldırma eksik | Satırda kaldırma, süreli ve kapatılabilir Undo bildirimi, başarısız geri almada yeniden deneme ve boş listede `Explore sounds` eklendi. Kalıcı değişiklik atomik ve idempotenttir. |
| 6. Zamanlayıcı kapsamı belirsiz | Aktif zamanlayıcının kalan süresi tüm sekmelerde mini oynatıcıda görünür. Panel, sürenin hemen başlayacağını veya mevcut sayacı yeniden başlatacağını açıklar. İptal aynı yerde bulunur. |
| 7. Hikâye bilgisi eksik | Hikâye detayında açıklama, yazar, anlatıcı, süre ve internet gereksinimi sunulur. Ürün kararı gereği ortak oynatıcıda ilerleme çubuğu veya ileri/geri atlama bulunmaz. |
| 8. Dekorasyon temel eylemleri geriye itiyor | Ses detayı görseli 112 dp'ye indirildi; ekranların dikey boşlukları azaltıldı. İçerik eylemleri daha erken görünür. |
| 9. Tipografi okunabilirliği | İşlevsel metinler 13–16 sp aralığına getirildi; aşırı ince ağırlık ve geniş harf aralıkları kaldırıldı. Kart başlıkları iki satıra izin verir. |
| 10. Erişilebilirlik bağlamı eksik | Oynatma/favori eylemleri içerik adı taşır; favori kontrolü seçili durumunu bildirir. Detay açma ve zamanlayıcı seçeneklerinin anlamlı adları vardır. Başlık semantiği ve hata/yükleme duyuruları eklendi. |
| 11. Çevrimdışı durum belirsiz | Ses detayındaki durum tamamlanmış, boş olmayan gerçek önbellek dosyasından türetilir. İndirme tamamlanınca güncellenir. Hikâyelerde internet gereksinimi açıkça belirtilir. |
| 12. Adlandırma tutarsız | `Browse` sekmesi `Sounds`, `White Noise` kategorisi `Noise`, favoriler başlığı `Favorite sounds` oldu. Sistem medya başlığı katalogdaki kullanıcıya görünen ses adıyla eşitlendi. |

## Bileşen ve gezinme düzeni

- `SoundScreen` → `SoundDetailScreen`: görevi artık açıkça içerik detayıdır.
- `NowPlayingMiniPlayer` ve `NowPlayingScreen`: küçük çubuk ile tam ekran oynatıcı ayrı bileşenlerdir; sheet kullanılmaz.
- Navigation bar ve rail Material `NavigationSuiteScaffold` tarafından seçilir; ortak mini oynatıcı bu iskeletin içinde, `NavDisplay`'in altında tek örnek olarak durur. Eski `AppPlayer`, `AppNowPlayingSceneDecorator` ve 29 Eylül'de `NavigationChromeScene` kaldırılmıştır.
- `StoryRoute` ve `StoryDetailScreen`: hikâye listesi ile belirli hikâyenin detayı ayrı hedeflerdir.
- `PlayPauseButton` / `PlayableRow` içindeki `isPlaying` parametresi `playRequested` oldu: düğmenin yükleme sırasında da duraklatma/iptal niyetini temsil ettiği anlaşılır.
- Mevcut route kimlikleri, favori namespace'leri ve kategori kimlikleri korunur. Serializer kayıtları aynı wire biçimini koruyan resmi overload ile yapılır.

Uygulamanın mevcut İngilizce dili korundu. Türkçe yerelleştirme, hikâye favorileri, özel süre girişi ve hikâye sonunda durma bu düzeltmenin kapsamında değildir.

## Doğrulama

Çalıştırılan komut:

```text
.\gradlew.bat :androidApp:assembleDebug testAndroidHostTest checkArchitecture :androidApp:lintDebug :check staticAnalysis --no-daemon --continue
```

- Seek kaldırıldıktan sonra 54 test paketi, 372 test geçti: sıfır başarısızlık, sıfır hata, sıfır atlanan test. İlk sürümdeki 13 seek/progress testi özelliğin kaldırılmasıyla temizlendi.
- Android debug APK üretildi; mimari kurallar 19 modülde geçti.
- Statik analiz geçti. Detekt'in serialization tarafından üretilen `.serializer()` üyelerini çözememe uyarıları eşdeğer serializer kayıt çağrılarıyla giderildi; ayrıca 32 shared testi geçti.
- Android lint: 0 hata, 5 uyarı. Uyarılar değişmeyen target SDK bildirimi (2), medya servisi bildirimi (1), launcher monochrome ikonları (2) üzerindedir.
- `git diff --check` kontrol edildi.

Pixel_4 Android emülatöründe dokunma ve ekran görüntüsüyle doğrulananlar:

1. Kategoriden doğrudan ses başlatma ve tek mini oynatıcı.
2. Panel açma, kapatma ve oynatmayı durdurmadan geri dönme.
3. Zamanlayıcının sekme değişince görünür kalması.
4. Hikâye satırının, çalan sesi değiştirmeden hikâye detayını açması.
5. Hikâye yükleme hatasında açıklama ve yeniden deneme eylemi.
6. Favoriye ekleme, listeden kaldırma, boş durum, Undo ile geri getirme ve bildirimin kapanması.
7. Daha önce indirilen sesin detayında `Available offline` gösterimi.
8. Yüzde 200 yazı boyutunda oynatıcı panelini kaydırarak tüm zamanlayıcı seçeneklerine ulaşma.

Ekran kayıtları yerel `tmp/ui-ux/` dizinindedir. Bu görüntüler seek kaldırılmadan önceki sürümdendir. Geçici emülatör salt okunur AVD oturumunda kullanılmıştır.

## Doğrulamanın sınırları

- Windows üzerinde iOS native derlemesi, iOS UI testleri ve VoiceOver çalıştırılmadı. Ortak kodun Android host testleri geçti. Sonradan eklenen native seek/progress entegrasyonu da kaldırıldı.
- Hikâye uzak kaynağı emülatörde erişim hatası verdi. Hata akışı doğrulandı; başarılı hikâye streaming davranışı gerçek kaynakla uçtan uca doğrulanamadı.
- TalkBack ile sesli kullanım, fiziksel cihaz dokunma hissi ve animasyon performansı ölçülmedi. Bunlar kullanıcı testiyle eşdeğer kabul edilmemelidir.
- Önbellek etiketi mevcut dosyayı kontrol eder; işletim sisteminin dosyayı sonradan temizlemeyeceği garantisini vermez. Katalogda gösterilen süre ile gerçek medya süresi farklı olabilir.
