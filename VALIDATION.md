# Walidacja DeX Tools n2s

Data: 2026-10-24. Środowisko: Linux x86_64, Temurin JDK 17.0.21,
Gradle Wrapper 8.13, AGP 8.11.1, Kotlin 2.1.20, Android SDK 36.

- `./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
- Testy JVM: **8 wykonanych, 8 zaliczonych, 0 błędów, 0 pominiętych**.
  Sprawdzają normalizację i ograniczanie prostokąta, puste i nieprawidłowe dane oraz regułę skrótu klawiaturowego.
- Android Lint: **0 błędów, 8 ostrzeżeń** (alokacje podczas rysowania, konstruktor własnego widoku, dostępność gestu, statyczna referencja usługi, reguły backupu).
- APK istnieje: `app/build/outputs/apk/debug/app-debug.apk`, około 2,4 MB.
- `apksigner verify --verbose`: poprawny podpis APK v2, jeden podpisujący, klucz debug RSA 2048.
- `aapt dump badging`: pakiet `pl.n2s.dextools`, wersja `0.1.0` / 1, minSdk 30, targetSdk 36, nazwa `DeX Tools n2s`.
- `git diff --check`: brak błędów białych znaków.

Testy na emulatorze i wynik zdalnego GitHub Actions: weryfikacja w toku.
Brak fizycznego S24 Ultra w środowisku. Nie potwierdzono funkcji na Android 17 / One UI 9 / Samsung DeX.
Nie traktować kompilacji ani testów JVM jako dowodu działania MediaProjection, schowka,
AccessibilityService, zaznaczania myszą lub globalnego skrótu na telefonie.
