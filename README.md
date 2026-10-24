# DeX Tools n2s

Polska aplikacja Android w Kotlinie, autor **n2s**, pakiet **`pl.n2s.dextools`**.
Mały, wąski popup AMOLED: menu 280 × 344 dp, okno tekstu 320 × 440 dp.
Pływające przyciski tworzą pionowy pasek o szerokości 56 dp, zwijany przyciskiem **−** do kropki 32 dp (obszar kliknięcia 48 dp).
Kliknięcie kropki rozwija pasek, przeciągnięcie przenosi go po ekranie.
**Ustawienia → Przezroczystość** lub **⚙** na panelu: suwak 0–80%, zapisywany między uruchomieniami. Dotyczy popupów, panelu i kropki. Obsługa myszy i klawiatury.
Rozmiary są ograniczane do dostępnego obszaru ekranu; aplikacja nie otwiera pełnoekranowego pulpitu narzędzi.

**[Pobierz APK 0.3.0](https://github.com/jargielo/DeXToolsN2S/releases/download/v0.3.0/app-debug.apk)** ·
[Wydanie i SHA-256](https://github.com/jargielo/DeXToolsN2S/releases/tag/v0.3.0) ·
[Buildy Actions](https://github.com/jargielo/DeXToolsN2S/actions) ·
[Wyniki testów](VALIDATION.md)

APK jest podpisanym buildem **debug**. Android 11+ (minSdk 30), compile/target SDK 36.
Docelowy sprzęt użytkownika: Samsung Galaxy S24 Ultra, Android 17 / One UI 9, Samsung DeX.
Testy chmurowe wykonano na emulatorze Android 16 / API 36. **Nie przeprowadzono testów na fizycznym Samsungu ani zewnętrznym monitorze DeX.**

## Screenshot

- Zrzut całego domyślnego wyświetlacza przez MediaProjection.
- Fragment wybierany przeciągnięciem myszy na zamrożonym obrazie ekranu. Esc / Wstecz anuluje.
- PNG w `Pictures/Screenshots`, zapis przez MediaStore bez dostępu do całego magazynu.
- Automatyczne kopiowanie obrazu jako **URI `image/png`** do schowka. Odbiorca musi obsługiwać obrazy.
- Panel pływający z przyciskiem pełnego zrzutu i fragmentu.
- Ctrl+Shift+S w głównym oknie; usługa dostępności próbuje obsłużyć ten skrót również poza aplikacją.

Domyślnie system pyta o zgodę MediaProjection przy każdym zrzucie. Opcjonalną zgodę przez ADB opisuje [instrukcja](ADB.md). Okno zgody ma osobne, przezroczyste zadanie,
aby wywołanie z panelu nad inną aplikacją nie przywoływało głównego okna DeX Tools.
Podgląd fragmentu dopasowuje się do obszaru nakładki; współrzędne są przeliczane do rozdzielczości obrazu źródłowego.

## Text Selection

- **T** odczytuje na żądanie tekst udostępniony w aktywnym drzewie Accessibility i otwiera go w oddzielnym oknie.
- W oknie tekstu działa zaznaczanie, kopiowanie, edycja oraz Ctrl+A/C/V.
- Długie kliknięcie **T** próbuje zaznaczyć i skopiować całe aktywne pole tekstowe przez `ACTION_SET_SELECTION` i `ACTION_COPY`.
- Android **Udostępnij** (`SEND`) i akcja zaznaczonego tekstu (`PROCESS_TEXT`) otwierają to samo okno.
- Gdy źródło nie udostępnia tekstu, można użyć Udostępnij albo wkleić tekst ręcznie. Brak OCR.

Nie jest to globalne zaznaczanie dowolnego tekstu w dowolnej aplikacji.
Źródło musi udostępniać tekst lub obsługiwać systemowe akcje zaznaczenia / kopiowania.
Pola oznaczone jako hasła są pomijane. Odczyt ma limit 2500 węzłów / 100 tys. znaków.
Kolejność i ewentualne duplikaty wynikają ze struktury aplikacji źródłowej.

## Instalacja i uprawnienia

1. Pobierz APK z wydania i zezwól Androidowi na instalację z wybranego źródła.
2. Otwórz aplikację. W **Ustawienia → Zgoda na nakładkę** włącz wyświetlanie nad innymi aplikacjami, następnie wybierz **Pływające przyciski**.
3. Dla odczytu tekstu świadomie włącz usługę **DeX Tools n2s — tekst i skrót** w ustawieniach dostępności.
   APK spoza sklepu może wymagać zezwolenia na ustawienia z ograniczonym dostępem w szczegółach aplikacji.
4. Potwierdzaj systemową zgodę na zrzut albo świadomie nadaj zgodę dla tego pakietu przez [ADB](ADB.md). Zgoda może być cofnięta.

Panel: **▣** pełny ekran, **⌗** fragment, **T** tekst, **−** kropka, **⚙** przezroczystość, **×** wyłączenie.
Przycisk **−** w nagłówku głównego popupu również minimalizuje go do pływającej kropki.
Panel i kropka pamiętają pozycję oraz stan zwinięcia. Rozwinięcie przy krawędzi mieści panel na ekranie. Uchwyt **⋮** pozwala przenieść panel.
Panel ukrywa się podczas przechwytywania. Usługę można zatrzymać również z powiadomienia.
Panel nie uruchamia się samoczynnie po restarcie telefonu.

## Ograniczenia Androida i DeX

- `FLAG_SECURE`, DRM i ochrona treści wrażliwych mogą dać czarny / ukryty obraz. Android 16 zablokował w teście okno z polem hasłowym.
  Zabezpieczeń nie obchodzimy.
- MediaProjection żąda domyślnego wyświetlacza. Wybór udostępnianego okna / ekranu zależy od systemu.
  Przechwytywanie zewnętrznego monitora DeX i mapowanie jego współrzędnych wymagają testu na Samsungu.
- Android, One UI lub inna usługa mogą przejąć skrót klawiaturowy. Panel jest alternatywnym sposobem wywołania.
- Canvas, obrazy, część WebView i aplikacje blokujące Accessibility mogą nie udostępniać tekstu.
- Wklejanie obrazu zależy od aplikacji docelowej; sam tekstowy edytor nie staje się edytorem obrazów.
- Wersje debug z różnych maszyn / runnerów mają różne klucze podpisujące. Aktualizacja może wymagać odinstalowania poprzedniego debug APK.
  Stały klucz wydania należy skonfigurować osobno, poza repozytorium.

Aplikacja nie żąda uprawnienia INTERNET, nie ma analityki ani historii tekstu. Zrzuty pozostają w galerii.
Usługa dostępności nie zbiera tekstu w tle; odczyt wykonuje dopiero kliknięcie użytkownika.

## Budowanie w Android Studio lub CLI

Wymagane: JDK 17, Android SDK Platform 36, Build Tools 36.0.0.
Gradle Wrapper 8.13 (z SHA-256 dystrybucji), AGP 8.11.1, Kotlin 2.1.20.

```sh
# Otwórz katalog repozytorium w Android Studio i zsynchronizuj Gradle.
# CLI: ustaw ANDROID_HOME lub ignorowany local.properties z sdk.dir=...
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

W przygotowanym środowisku chmurowym:

```sh
source /workspace/toolchains/activate.sh
cd /workspace/DeXToolsN2S
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
# Odtworzenie / odświeżenie narzędzi i zależności:
bash tools/cloud-setup.sh
```

Workflow `.github/workflows/android.yml` buduje APK, uruchamia testy i Lint, sprawdza podpis oraz publikuje APK i SHA-256
jako artefakt przez 30 dni. Tag `v*` uruchamia też publikację w GitHub Releases.
`tools/ci-build.sh` zawiera sprawdzony skrypt CI; błędy trafiają do adnotacji, a pełny log do artefaktu raportów.
Kopia w `downloads/` jest lokalnym buildem debug; plik z Releases pochodzi z osobnego builda runnera.

## Testy urządzeniowe

Wyniki i rozróżnienie testów zaliczonych, ograniczeń systemowych oraz niewykonanych prób: [VALIDATION.md](VALIDATION.md).
Niezależna aplikacja kontrolna schowka i pól tekstowych: [tools/clipboard-probe](tools/clipboard-probe/README.md).
Nie jest częścią produktu ani jego APK i nie jest dołączana do standardowego builda aplikacji.
