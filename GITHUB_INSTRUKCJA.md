# Translator PL–EN–RU — budowanie APK na GitHubie

Ten projekt jest **prototypem**. Dodano workflow `.github/workflows/build-apk.yml`, który automatycznie instaluje Java 17, Android SDK 35, Gradle 8.9 i uruchamia budowanie `app-debug.apk`. Nie przeprowadzono jeszcze kompilacji w GitHub Actions ani testu logowania na telefonie.

## Pierwsze uruchomienie — bez Android Studio

1. Zaloguj się na https://github.com i załóż nowe repozytorium, np. `TranslatorPLENRU` (publiczne lub prywatne).
2. Rozpakuj ZIP na komputerze. **Otwórz katalog `TranslatorPLENRU_GitHub`** i wybierz **całą zawartość** do przesłania, czyli m.in. `app`, `.github`, `build.gradle.kts`, `settings.gradle.kts` — pliki muszą znaleźć się **w katalogu głównym repozytorium**, a nie w podfolderze.
3. W repozytorium wybierz **Add file → Upload files** i przeciągnij zawartość rozpakowanego katalogu. Zatwierdź przesłanie przez **Commit changes**.
4. Przejdź do zakładki **Actions** i wybierz workflow **Translator PL EN RU - build APK**. Na głównej gałęzi kliknij **Run workflow**. Workflow uruchamia się też automatycznie po przesłaniu kodu na gałąź `main` lub `master`.
5. Po zakończeniu przebiegu zielonym znacznikiem, otwórz konkretny przebieg i znajdź sekcję **Artifacts** → **Translator-PL-EN-RU-APK-debug**. Pobierz plik ZIP z artefaktem i rozpakuj `app-debug.apk`.
6. Przenieś APK na telefon, uruchom plik i zaakceptuj systemowe zezwolenie na instalację z tego źródła (jeśli Android o nie poprosi).

## Gdy kompilacja nie przejdzie

Wejdź w **Actions → nieudany przebieg → Zbuduj instalator Android → Skompiluj APK debug**, skopiuj błąd lub przekaż zrzut ekranu. Jest to pierwsza kompilacja projektu; możliwe, że potrzebne będą poprawki kodu.

## Dodatkowe informacje

- Artefakt w Actions to ZIP zawierający APK, nie APK bezpośrednio.
- `app-debug.apk` jest podpisany standardowym kluczem debug dla tego przebiegu CI. **Nie jest to produkcyjny podpis**, a kolejna kompilacja na nowym runnerze może mieć inny klucz. Do trwałych aktualizacji bez odinstalowywania aplikacji konieczny będzie osobny, bezpiecznie przechowywany klucz podpisu release.
- Github Actions kompiluje kod, ale **nie potwierdza, że logowanie OpenAI i tłumaczenie faktycznie zadziałają** na urządzeniu. Tego wymagają testy.
- Nie umieszczaj haseł, kluczy API ani tokenów konta w repozytorium. Aplikacja nie wymaga ich na etapie kompilacji.
- Nie wszystkie widoczne teksty z Instagrama da się odczytać przez Dostępność bez OCR.
