# Translator PL / EN / RU — prototyp Android (RMX2193)

Stan: **kod źródłowy prototypu 0.1.0, nieprzetestowany na fizycznym telefonie**.

## Co działa w kodzie

- Edytor tekstu z językiem źródłowym automatycznym lub ręcznym; wynik PL, EN lub RU.
- Naturalny, dosłowny i literacki styl tłumaczenia.
- `Intent.ACTION_SEND` — pojawia się w systemowym Udostępnij dla `text/plain`.
- `Intent.ACTION_PROCESS_TEXT` — rejestruje systemowy sposób tłumaczenia zaznaczenia (jeśli dana aplikacja go obsługuje).
- Schowek, kopiowanie, historia 30 tłumaczeń w pamięci aplikacji.
- Opcjonalny pływający przycisk Dostępności, który **dopiero po dotknięciu** sprawdza tekst dostępny na ekranie i pokazuje jego podgląd. Przed wysłaniem do modelu trzeba nacisnąć „Tłumacz”. Hasła są pomijane, inne dane prywatne mogą znaleźć się w odczycie — przejrzyj podgląd!
- Logowanie Sign in with ChatGPT: OAuth PKCE, callback na `127.0.0.1` (uruchomiony na urządzeniu), walidacja podpisu ID tokenu (RS256/JWKS), sprawdzanie nonce/state/iss/aud/exp, AES/GCM w Android Keystore dla tokenów, odświeżanie tokenów.
- Wywołanie oficjalnego `POST https://api.openai.com/v1/responses`, wymagane `store: false`, `stream: true`, SSE z weryfikacją `response.completed`.

**Nie** korzysta ze skryptów wstrzykiwanych do `chatgpt.com/translate` ani z ciasteczek przeglądarki. Funkcjonalnie dąży do podobnego wyniku, ale nie jest kopią oficjalnego interfejsu ChatGPT Translate.

## Instalacja — źródła

1. Otwórz folder `TranslatorPLENRU` w **Android Studio** na komputerze z internetem.
2. Gdy Android Studio poprosi o SDK, zainstaluj Android SDK Platform 35 i Build Tools.
3. Projekt wymaga JDK 17; Gradle 8.9 / Android Gradle Plugin 8.7.3 / Kotlin 2.0.21.
4. W Android Studio wybierz **Build → Build APK(s)**.
5. Przenieś `app/build/outputs/apk/debug/app-debug.apk` na Realme RMX2193 i zainstaluj (zezwól na instalację ze wskazanego źródła, gdy Android o to poprosi).
6. Otwórz aplikację, wybierz **Połącz z ChatGPT**, zaloguj się w systemowej przeglądarce, potwierdź użycie planu ChatGPT.
7. Wybierz docelowy język i wypróbuj zwykłe tłumaczenie, potem systemowe „Udostępnij”.
8. Opcjonalnie uruchom usługę Dostępności „Przycisk tłumaczenia na ekranie”, aby zobaczyć pływającą literę **T**. Można ją wyłączyć w ustawieniach Dostępności.

> Zależnie od wersji Android Studio może brakować pliku gradle-wrapper.jar. W takiej sytuacji otwórz folder w Android Studio i wskaż lokalną instalację Gradle 8.9 lub wygeneruj standardowy wrapper poleceniem `gradle wrapper --gradle-version 8.9` na komputerze posiadającym Gradle.

## Ograniczenia prototypu

- Nie jest to podpisany APK; w środowisku wytworzenia brak zainstalowanego Android SDK i Gradle. Nie wykonano synchronizacji, kompilacji ani testów fizycznych i UI. Potencjalne błędy dopiero wyjdą podczas budowania.
- SIWC jest stosunkowo nową funkcją. Prawidłowość logowania po zbudowaniu aplikacji należy zweryfikować na urządzeniu. Konto musi mieć uprawnienia udostępniania planu.
- Pływająca usługa może nie odczytać tekstu z niektórych elementów Instagrama, obrazów, filmów i niestandardowych kontrolek. Nie implementuje OCR, nie odczytuje haseł.
- Pływający przycisk to usługa Dostępności uruchamiana po Twoim zezwoleniu; na urządzeniach Realme systemowe oszczędzanie energii może ją zatrzymać.
- Pływająca nakładka przetłumaczy *zebrany widoczny tekst*, niekoniecznie jeden wskazany fragment. Użytkownik widzi podgląd i decyduje, co wysłać.
- Ze względu na zgodność z dokumentacją SIWC zapytania mogą pobierać dostępny limit użycia planu ChatGPT; nie ma gwarancji identycznych limitów co w chatgpt.com/translate.
- Wstępna wersja obsługuje jedno konto ChatGPT na raz, a wylogowanie czyści lokalne tokeny. Aby również odebrać zgodę po stronie OpenAI, rozłącz aplikację w ustawieniach ChatGPT.
- Tłumaczenia w historii zapisują się lokalnie w SharedPreferences (nie są szyfrowane). Nie przechowuj tam szczególnie wrażliwych tekstów; historię można skasować w aplikacji.

## Oficjalna dokumentacja

- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://openai.com/policies/sign-in-with-chatgpt-terms/

## Licencja

Kod przykładowy w tym archiwum udostępniony na zasadach MIT — plik `LICENSE`.


## Kompilacja w GitHub Actions

Wersja GitHub-ready zawiera `.github/workflows/build-apk.yml`. Instrukcje w pliku `GITHUB_INSTRUKCJA.md`.

## Audyt 0.3 (2026-10-14)

- Trwałe zachowanie `client_id` po rejestracji SIWC także wtedy, gdy wymiana kodu OAuth na token nie powiedzie się.
- Aplikacja nie pokazuje już „połączono” jedynie na podstawie nadanego `client_id`; wymagany jest token i udzielony zakres `chatgpt.tokens.use.direct`.
- Weryfikacja niespodziewanego `client_id` w powracającym OAuth i znacznika `nbf` w ID tokenie.
- Czytelniejsze komunikaty limitów i błędów odpowiedzi strumieniowych.
- Wyczyszczenie poprzedniego tłumaczenia podczas nowego zlecenia i po odebraniu tekstu z innej aplikacji; blokada wielokrotnego równoczesnego rozpoczynania logowania.

Nie udało się jeszcze przetestować rzeczywistego logowania na telefonie. Debug APK jest podpisywany kluczem generowanym na runnerze GitHub Actions, więc aktualizacja wcześniej zainstalowanego testowego APK może wymagać jego odinstalowania.
