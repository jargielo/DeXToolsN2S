# Niezależna aplikacja kontrolna

Fixture do testów na własnym emulatorze. Pakiet `pl.n2s.clipboardprobe` ma inny UID niż DeX Tools
oraz **nie deklaruje uprawnień do galerii / magazynu**. Po uzyskaniu fokusu lub naciśnięciu
„Sprawdź schowek” czyta schowek standardowym API i próbuje zdekodować PNG z przekazanego URI.
To sprawdza faktyczne udostępnienie obrazu między aplikacjami, a nie tylko wywołanie setPrimaryClip.

```sh
./gradlew -p tools/clipboard-probe --no-daemon --max-workers=2 assembleDebug
adb -s SERIAL install -r tools/clipboard-probe/build/outputs/apk/debug/clipboard-probe-debug.apk
adb -s SERIAL shell am start -W -n pl.n2s.clipboardprobe/.ProbeActivity
```

Wynik jest widoczny w oknie i w prywatnym `files/clipboard-result.txt`:

- `IMAGE_OK WxH mime=image/png uri=...` — obraz odczytany i zdekodowany przez odbiorcę bez uprawnienia do galerii.
- `TEXT_OK ...` — odczytany tekst. Pole źródłowe zawiera `N2S_FIELD_2468` i pozwala sprawdzić kopiowanie przez Accessibility.
- `ERROR ...` — próba nie powiodła się; nie traktować jej jako zaliczonej.

Na debug emulatorze plik można odczytać przez `adb shell run-as pl.n2s.clipboardprobe cat files/clipboard-result.txt`.
Przed oceną nowego schowka poczekaj na zakończenie CaptureService i ponownie kliknij „Sprawdź schowek”.
Aktualizacja / reinstalacja odbiorcy może unieważnić wcześniejsze granty URI; wtedy wykonaj nowy zrzut.

Negatywny przypadek i test pomijania haseł: po zamknięciu fixture uruchom ją z `--ez password true`:

```sh
adb -s SERIAL shell am force-stop pl.n2s.clipboardprobe
adb -s SERIAL shell am start -W -n pl.n2s.clipboardprobe/.ProbeActivity --ez password true
```

Dodaje to sztuczne pole hasłowe. Android 16 może zablokować obraz takiego okna podczas MediaProjection.
Ten przypadek jest oczekiwanym ograniczeniem, nie pozytywnym testem zrzutu. Podstawowy wariant bez tego parametru
nie zawiera pola hasłowego. DeX Tools powinien pomijać węzeł hasła przy odczycie tekstu.
