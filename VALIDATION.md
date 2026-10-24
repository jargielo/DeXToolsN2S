# Walidacja DeX Tools n2s

## Wersja 0.3.0 — przezroczystość i kropka

Data: 2026-10-24. versionCode 5, compile/target SDK 36.

- `assembleDebug testDebugUnitTest lintDebug`: **BUILD SUCCESSFUL**, 8/8 testów JVM, 0 pominiętych; Lint: 0 błędów, 13 ostrzeżeń.
- Lokalny APK: podpis v2 poprawny; instalacja / aktualizacja na AOSP API 36: **Success**, MainActivity **Status: ok**.
- Suwak ustawiono myszą na 40%; odczyt SharedPreferences potwierdził wartość, WindowManager potwierdził `alpha=0.6`.
- Minimalizacja głównego popupu do kropki: okno 48 × 48 px, widoczna kropka 32 dp przy density 160. Kliknięcie rozwija panel 56 × 328 dp.
- Przeciągnięcie kropki do dolnego prawego rogu zachowało stan zwinięcia. Po puszczeniu zapisano x=952, y=468; okno [952,596]–[1000,644].
- Rozwinięcie przy krawędzi: panel [944,316]–[1000,644], w całości w dostępnym obszarze. Przycisk − ponownie zwinął panel.
- Po force-stop i ponownym uruchomieniu pozostały przezroczystość 40%, pozycja i stan zwinięcia; popup odtworzył `alpha=0.6`.
- Test gestu wykrył niedokładne miejsce upuszczenia; poprawiono użycie końcowych współrzędnych ACTION_UP i ponowiono test z pozytywnym wynikiem.
- Dowody: [docs/evidence](docs/evidence/). Instrukcja nadania / cofnięcia zgody: [ADB.md](ADB.md).
- Fizyczny Samsung S24 Ultra / Android 17 / One UI 9 / DeX pozostają **nieprzetestowane**.

## Archiwum walidacji 0.2.0 i starszych

Data: 2026-10-24. Badane wydanie: **0.2.0**, versionCode **4**, pakiet `pl.n2s.dextools`.
JDK 17.0.21, Gradle 8.13, AGP 8.11.1, Kotlin 2.1.20, compile/target SDK 36, minSdk 30.

## Kompilacja i plik APK

- `./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug`: **BUILD SUCCESSFUL**.
- **8 testów JVM wykonanych, 8 zaliczonych, 0 błędów, 0 pominiętych**. Testują granice i kierunek zaznaczenia,
  odrzucanie pustych / nieprawidłowych danych oraz regułę skrótu klawiaturowego.
- Android Lint: **0 błędów, 9 ostrzeżeń**. Ostrzeżenia dotyczą m.in. obsługi gestów przez własne widoki,
  konstruktora RegionOverlay, alokacji podczas rysowania, referencji do żywej usługi i konfiguracji backupu.
- Finalny workflow: **Success**, **2m 26s**, publikacja APK i raportów jako dwóch artefaktów oraz APK w Releases:
  https://github.com/jargielo/DeXToolsN2S/actions/runs/40124006180
- Pobrano rzeczywisty plik z https://github.com/jargielo/DeXToolsN2S/releases/download/v0.2.0/app-debug.apk
- Rozmiar: **2 469 545 bajtów**. `apksigner verify --verbose`: poprawny podpis **v2**, jeden podpisujący, klucz debug.
- SHA-256 zgodne z opublikowanym plikiem kontrolnym:
  `902412a73110933005964004bbf84b1d0375f94859109827157c2f2320bbc46d`.
- Pierwsze dwa workflow nie przeszły. Uproszczono przygotowanie toolchainu i dodano publikowanie diagnostyki.
  Dokładnej pierwotnej przyczyny nie potwierdzono z pełnych logów. Kolejne buildy i publikacje zakończyły się sukcesem.

## Wykonane testy funkcjonalne

Środowisko: **Android 16 / API 36 AOSP x86_64**, 1000 × 700 px, 160 dpi, emulacja programowa bez KVM.
Testy to ręczne smoke testy przez ADB, rzeczywiste API Androida i inspekcja obrazów ekranu.
Nie jest to formalnie zaliczona suita UiAutomator: legacy `uiautomator dump` nie zwrócił drzewa / zakończył się błędem.

| Przypadek | Zaobserwowany wynik |
| --- | --- |
| Instalacja i uruchomienie | Finalny APK 0.2.0 pobrany z Releases: instalacja **Success**, uruchomienie MainActivity **Status: ok** |
| Małe okienka 0.2.0 | Menu 280 × 344 dp, edytor 320 × 440 dp; tło źródła pozostaje widoczne |
| Panel | Uruchomiona usługa foreground `specialUse`; w 0.2.0 wąski pionowy pasek 56 dp |
| Udostępnienie tekstu | `ACTION_SEND` otworzyło edytor z przekazanym tekstem |
| Klawiatura w edytorze | Ctrl+A, Ctrl+C, usunięcie zawartości i Ctrl+V przywróciły testowy tekst |
| Mysz w edytorze 0.2.0 | Przeciągnięcie zaznaczyło fragment `n2s_mou`; Ctrl+C, osobny odbiorca zwrócił `TEXT_OK n2s_mou` |
| Pełny zrzut | Ponownie w 0.2.0: z wąskiego panelu nad inną aplikacją → PNG 1000 × 700, odbiorca `IMAGE_OK 1000x700`, URI media/25 |
| Zrzut innej aplikacji | W 0.1.2 okno zgody zachowało aplikację źródłową; poprawny PNG zwykłego okna fixture, nie głównego okna narzędzi |
| Fragment myszą | `input mouse swipe` → PNG 400 × 327 oraz 400 × 273, poprawnie odczytane i zawierające wybrany obraz |
| Schowek obrazu | Osobny pakiet `pl.n2s.clipboardprobe`, inny UID, bez uprawnień do galerii: `IMAGE_OK 1000x700` i `IMAGE_OK 400x273`, MIME image/png, odczyt i dekodowanie URI |
| Aktywne pole przez Accessibility | Przytrzymanie T zaznaczyło i skopiowało pole z innej aplikacji; odbiorca: `TEXT_OK N2S_FIELD_2468` |
| Osobne okno odczytanego tekstu | Krótkie T otworzyło tekst fixture z tytułem, zwykłym polem i etykietami; w teście z hasłem węzeł hasłowy pominięty |
| Ctrl+Shift+S w aplikacji | Finalny APK 0.2.0: skrót w małym menu otworzył systemową zgodę MediaProjection; następnie anulowano |

Testy zrzutów, grantów URI i Accessibility wykonano podczas walidacji 0.1.1 / 0.1.2.
W 0.2.0 ponownie sprawdzono popupy, zaznaczanie myszą, pełny zrzut z panelu i obraz w schowku; silnik MediaProjection, MediaStore i ekstrakcji tekstu
pozostaje ten sam. Menu narzędzi jest zamykane przed przechwyceniem obrazu.

Dowody obrazowe: [docs/evidence](docs/evidence/).
Niezależny odbiorca jest udostępniony w [tools/clipboard-probe](tools/clipboard-probe/README.md).
Przy aktualizacji fixture wcześniejszy grant URI został unieważniony i odbiorca zgłosił `SecurityException`.
Nowo wykonany zrzut utworzył nowy schowek i pozytywne testy odbioru przeszły — starego URI po reinstalacji nie uznano za zaliczony test.

## Ograniczenia i wyniki niepotwierdzone

- **Oczekiwane zablokowanie:** Android 16 ukrył obraz okna fixture z polem hasłowym i pokazał
  „App content hidden from screen share for security”. Nie obchodzono zabezpieczeń.
  Pozytywne testy wykonano na wariancie fixture bez pola hasłowego.
- **Skrót globalny niepotwierdzony:** próba syntetycznego Ctrl+Shift+S przez ADB poza aplikacją nie otworzyła zgody.
  Nie ustalono, czy dotyczy to wstrzykniętych zdarzeń, polityki systemu czy filtra usługi.
  Obsługa jest zaimplementowana w Activity i AccessibilityService; nie obiecuje działania na każdym systemie.
- **Niewykonane:** fizyczny Samsung S24 Ultra, Android 17 / One UI 9, zewnętrzny monitor Samsung DeX,
  aplikacje z DRM, szeroki zestaw WebView / przeglądarek i wklejanie do konkretnych edytorów obrazów.
- Pierwszy emulator Google APIs miał ANR procesów systemowych i błąd `ApkAssets.finalize()`.
  Nie były to awarie kodu aplikacji; do testów użyto lżejszego AOSP i dostosowanych limitów emulatora.
- Kompilacja na SDK 36 i testy AOSP 16 nie potwierdzają pełnej zgodności z Android 17 / One UI 9.
- Zapis skryptów w konfiguracji środowiska nie oznacza publikacji nowego snapshotu ani sprawdzenia odtworzenia w nowym zadaniu.
