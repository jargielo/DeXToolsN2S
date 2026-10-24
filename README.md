# DeX Tools n2s

Natywna aplikacja Android w Kotlinie. Autor: **n2s**. Pakiet: `pl.n2s.dextools`.
Polski interfejs AMOLED, zmienny rozmiar okna, sterowanie dotykiem, myszą i klawiaturą.
Minimalny Android 11 (API 30), compile/target SDK 36. Docelowy sprzęt użytkownika:
Samsung Galaxy S24 Ultra w DeX, Android 17 / One UI 9 — wymaga testów na fizycznym urządzeniu.

## Instalacja i użycie

1. Pobierz `app-debug.apk` z artefaktu workflow **Android APK** (zakładka Actions) lub z Releases, jeśli opublikowano wydanie.
2. Zezwól na instalowanie z wybranego źródła. APK jest podpisanym buildem debug, nie wydaniem sklepowym.
3. Otwórz aplikację. Zrzut pełnego ekranu działa po zgodzie systemowej MediaProjection.
4. Dla panelu i prostokąta włącz „Wyświetlanie nad innymi aplikacjami”, następnie „Włącz pływający panel”.
5. Dla odczytu tekstu świadomie włącz usługę **DeX Tools n2s — tekst i skrót** w ustawieniach dostępności. Android może wymagać zezwolenia na ustawienia z ograniczonym dostępem dla APK spoza sklepu.
6. Nad aplikacją źródłową kliknij **T**. W osobnym oknie zaznacz tekst myszą i skopiuj przyciskiem lub Ctrl+C. Długie kliknięcie T próbuje zaznaczyć i skopiować całe aktywne pole tekstowe przez systemowe akcje dostępności.

Panel: **▣** pełny ekran, **⌗** fragment, **T** tekst, **×** wyłączenie. Uchwyt **⋮** przenosi panel.
Nakładka ukrywa się na czas zrzutu. Prostokąt wybiera się na zamrożonym obrazie ekranu;
Esc lub Wstecz anuluje wybór. Zamknięcie panelu lub przycisk Zatrzymaj w powiadomieniu kończy usługę.

PNG jest zapisywany przez MediaStore w `Pictures/Screenshots` bez uprawnienia do całego magazynu.
Do schowka trafia **URI obrazu z MIME image/png**, a nie ścieżka lub tekst base64.
Wklejanie obrazu zależy od obsługi obrazów przez aplikację docelową. Zapis i schowek raportują błędy oddzielnie.

## Ograniczenia systemowe

- Każdy zrzut wymaga nowej zgody MediaProjection. Nie obchodzimy zgód ani FLAG_SECURE, DRM i zabezpieczeń aplikacji.
- Android może pozwolić wyłącznie na udostępnianie okna zamiast całego ekranu; żądamy domyślnego wyświetlacza. Zewnętrzny wyświetlacz DeX i mapowanie współrzędnych trzeba sprawdzić na Samsungu. Brak gwarancji przechwytywania dowolnego monitora.
- Ctrl+Shift+S działa w głównym oknie aplikacji. Usługa dostępności próbuje obsłużyć ten sam skrót poza aplikacją. Android, One UI lub inna usługa mogą go przejąć. Panel jest alternatywą.
- Nie ma globalnego zaznaczania dowolnego tekstu we wszystkich aplikacjach. Pobieramy tekst i opisy udostępnione w aktywnym drzewie Accessibility; pomijamy hasła. Canvas, obrazy, część WebView, zabezpieczone pola i aplikacje blokujące dostęp mogą nie udostępniać treści. Brak OCR.
- Mysz zaznacza tekst w oddzielnym, edytowalnym oknie, które obsługuje również Android SEND i PROCESS_TEXT. Bezpośrednia próba zaznaczenia/kopiowania dotyczy całego aktywnego pola, jeśli źródło obsługuje ACTION_SET_SELECTION i ACTION_COPY. Nie modyfikujemy treści pola.
- Odczyt jest wyłącznie na kliknięcie, bez historii i transmisji sieciowej. Dla bezpieczeństwa ograniczamy odczyt do 2500 węzłów / 100 tys. znaków. Kolejność i duplikaty zależą od aplikacji źródłowej.
- Android 17 / One UI 9 nie został zweryfikowany fizycznie w środowisku chmurowym. Kompilacja na SDK 36 nie potwierdza zgodności funkcjonalnej z nowszym systemem.
- Zainstalowany debug APK wymaga ręcznego przyznania uprawnień. W razie agresywnego usypiania One UI należy sprawdzić ustawienia baterii. Usługa nie uruchamia się samoczynnie po restarcie.

## Budowanie

Wymagane: JDK 17 lub 21, Android SDK Platform 36, Build Tools 36.0.0.
Gradle Wrapper 8.13, Android Gradle Plugin 8.11.1, Kotlin 2.1.20.

```sh
# Android Studio: otwórz katalog repozytorium i pozwól na synchronizację Gradle.
# CLI: skonfiguruj ANDROID_HOME lub ignorowany local.properties (sdk.dir=...).
./gradlew assembleDebug testDebugUnitTest lintDebug
# wynik: app/build/outputs/apk/debug/app-debug.apk
```

Workflow `.github/workflows/android.yml` buduje, testuje, wykonuje Lint i sprawdza podpis APK.
Publikuje APK i SHA-256 jako artefakt przez 30 dni. Tag `v*` dodatkowo publikuje plik w GitHub Releases.
Debug keystore jest lokalny dla maszyny/runnera: kolejne buildy mogą mieć inny podpis,
co wymaga odinstalowania poprzedniej wersji. Do stałych aktualizacji potrzebny będzie prywatny klucz wydania.

## Testy na urządzeniu (do wykonania)

- MediaProjection: zgoda i odmowa, zrzut pełny, porównanie wymiarów z ekranem DeX.
- Fragment: przeciąganie w obu kierunkach, granice ekranu, Esc, obrót/zmiana monitora.
- Galeria: PNG w Screenshots; schowek: wklejenie do aplikacji obsługującej obrazy.
- Accessibility: zwykłe pole, hasło, przeglądarka, aplikacja bez udostępnionej treści; długie kliknięcie T.
- Okno tekstu: zaznaczanie myszą, Ctrl+A/C, wklejanie, SEND i PROCESS_TEXT.
- Panel: przenoszenie, wyłączenie, odebranie uprawnień, zatrzymanie przez powiadomienie.
- Skrót w aplikacji i poza nią; ponowne uruchomienie po ubiciu procesu.

Wyniki chmurowej walidacji znajdują się w `VALIDATION.md`; testy jednostkowe nie zastępują tych testów sprzętowych.
