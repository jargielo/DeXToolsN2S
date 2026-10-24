# Zgoda na przechwytywanie ekranu przez ADB

Okno „Udostępnić ekran aplikacji DeX Tools n2s?” pochodzi z systemowego MediaProjection.
To inne ustawienie niż „Wyświetlanie nad innymi aplikacjami”. Aplikacja sama nie może nadać sobie tej zgody.

Na swoim telefonie włącz Opcje programisty → Debugowanie USB, połącz go z komputerem
z Android Platform Tools i zaakceptuj klucz komputera. Sprawdź `adb devices`: urządzenie ma mieć stan `device`.
Przy wielu urządzeniach dodaj `-s NUMER_URZĄDZENIA` po `adb`.

## Nadanie zgody tylko DeX Tools n2s

W terminalu komputera:

```sh
adb shell cmd appops get pl.n2s.dextools PROJECT_MEDIA
adb shell cmd appops set pl.n2s.dextools PROJECT_MEDIA allow
adb shell cmd appops get pl.n2s.dextools PROJECT_MEDIA
```

Ostatnia komenda powinna pokazać `PROJECT_MEDIA: allow`. Uruchom ręcznie kolejny zrzut z panelu.
Na AOSP Android 16 ta operacja pozwala pominąć pytanie o zgodę dla wskazanego pakietu, bez roota.
Nie usuwa wskaźników aktywnego przechwytywania, powiadomień usługi, ochrony `FLAG_SECURE` ani DRM.
DeX Tools nadal tworzy nową, jednorazową sesję MediaProjection na każdy ręcznie wywołany zrzut.

**Skutek:** ten pakiet otrzymuje stałą zgodę na przechwytywanie bez osobnego potwierdzenia każdego zrzutu.
Nie nadawaj jej innym, niezaufanym aplikacjom. Nie ma potrzeby wyłączania zabezpieczeń całego systemu.

## Przywrócenie pytania

```sh
adb shell cmd appops set pl.n2s.dextools PROJECT_MEDIA ignore
adb shell cmd appops get pl.n2s.dextools PROJECT_MEDIA
```

Ta zmiana dotyczy kolejnych żądań zgody; nie służy do kończenia już trwającej sesji.
DeX Tools kończy sesję po zrzucie. Aby natychmiast przerwać pracę, użyj systemowego wskaźnika
udostępniania / zatrzymania aplikacji. Nie używaj `appops reset`, które zmienia także inne operacje.

## Samsung / Android 17 / One UI 9

Nie mamy fizycznego telefonu użytkownika. Działanie na tej konfiguracji jest **niezweryfikowane**.
Jeśli pojawi się `Unknown operation`, `SecurityException`, `Permission denied` albo mimo `allow` nadal będzie dialog,
nie uznawaj zmiany za działającą. Zachowaj wynik komendy oraz wersję systemu i wróć do standardowego potwierdzania.
One UI może modyfikować zachowanie SystemUI; nie ma zweryfikowanej, uniwersalnej komendy `settings put`, która
wyłącza ten dialog na wszystkich urządzeniach. Nie proponujemy automatycznego klikania „Udostępnij ekran”.

Po odinstalowaniu aplikacji, zmianie użytkownika/profilu lub aktualizacji systemu sprawdź zgodę ponownie.
Komendy dotyczą bieżącego użytkownika ADB; profil służbowy wymaga właściwego `--user` i polityka administratora może go ograniczać.
Możesz wyłączyć debugowanie USB po konfiguracji. Cofnięcie autoryzacji komputera ADB samo nie cofa `PROJECT_MEDIA`.

## Podstawa w kodzie Androida

- [AOSP Android 16: MediaProjectionManagerService.hasProjectionPermission](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/core/java/com/android/server/media/projection/MediaProjectionManagerService.java#L756): sprawdza `OP_PROJECT_MEDIA == MODE_ALLOWED`.
- [AOSP Android 16: MediaProjectionPermissionActivity](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/packages/SystemUI/src/com/android/systemui/mediaprojection/permission/MediaProjectionPermissionActivity.java#L156): przy wcześniej nadanej zgodzie zwraca wynik bez pokazywania okna wyboru.

Wyniki wykonanych testów i zakres weryfikacji: [VALIDATION.md](VALIDATION.md).
