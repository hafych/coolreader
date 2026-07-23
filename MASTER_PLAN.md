# CoolReader Next — расширенный мастер-план развития

> Рабочая спецификация модернизации всего репозитория: сборки и поставки,
> Android-приложения, JNI, `crengine`, данных пользователя, desktop/legacy
> фронтендов и внешних зависимостей.
>
> Базовый upstream: [`buggins/coolreader`](https://github.com/buggins/coolreader).
> Проект развивается как совместимый downstream-fork, а не как независимая
> перепись.
>
> Состояние кода проверено: 23 июля 2026 года.
>
> Оценки ниже предполагают команду из 2–3 разработчиков. Для одного разработчика
> календарные сроки следует увеличить примерно в 2–3 раза.
>
> Главный вывод полного аудита: новый UI, Flexbox и облачная синхронизация нельзя
> начинать как первые крупные инициативы. Сначала должны быть закрыты stop-ship
> проблемы TLS/учётных данных, восстановлена сборка, создан тестовый baseline и
> укреплены хранение данных и разбор недоверенных документов.

---

## 1. Цели программы

1. Вернуть все поддерживаемые сборки в воспроизводимое «зелёное» состояние.
2. Исключить отключение TLS-проверок, передачу паролей по HTTP и хранение секретов
   открытым текстом.
3. Обеспечить корректную работу Android с Scoped Storage и Storage Access Framework
   без загрузки всей книги в Java heap.
4. Сделать миграции БД, историю, настройки, backup/restore и идентичность книг
   проверяемыми и совместимыми между релизами.
5. Укрепить обработку недоверенных EPUB/ZIP/XML/изображений лимитами, fuzzing и
   sanitizers.
6. Устранить подтверждённые ошибки потокобезопасности, не усложняя модель блокировок
   без измеримой необходимости.
7. Модернизировать Android-интерфейс и E-Ink-интеграции без одновременной переписи
   ядра.
8. Постепенно уменьшать технический долг `crengine`, сохраняя совместимость форматов,
   производительность и компактное представление DOM.
9. Ввести управляемый жизненный цикл зависимостей, release artifacts, символов,
   SBOM и документации.
10. Расширять EPUB 3, CSS и синхронизацию отдельными, измеримыми инициативами.
11. Сохранять возможность регулярно подтягивать upstream и возвращать общие
    исправления в `buggins/coolreader`.

## 2. Что не входит в базовый объём

- Массовая замена `ldomNode *` на `std::shared_ptr` или `std::unique_ptr`.
- Одновременная миграция всех контейнеров и строк `crengine` на STL.
- Полная реализация CSS Grid в первой версии CoolReader Next.
- Одновременный выпуск WebDAV, Google Drive и нескольких независимых облачных
  провайдеров.
- Переход на C++20 до стабилизации C++17 на всех целевых платформах.
- Переписывание Android UI, файлового слоя и C++ ядра в одном релизе.
- Обещание одинакового уровня поддержки для Android, Qt, wxWidgets и всех
  исторических E-Ink платформ без матрицы владельцев и устройств.
- Автоматическое исправление каждого `TODO` или разбиение больших файлов только
  ради метрики размера.
- Собственный универсальный HTTP/OAuth/crypto stack вместо проверяемых
  платформенных компонентов.
- Бессрочное накопление fork-only патчей без owner, причины и плана удаления.
- Переписывание истории upstream или массовое форматирование файлов, создающее
  искусственные merge conflicts.

---

## 3. Текущее состояние проекта

### 3.1. Компоненты

| Компонент | Текущее состояние | Роль |
| --- | --- | --- |
| `crengine` | C++ с собственными строками, контейнерами, DOM и подсистемой потоков | Парсинг, CSS, верстка, разбиение страниц, форматы документов |
| `android` | Java + JNI + C++; Kotlin и AndroidX фактически не используются | Мобильный клиент, библиотека, TTS, OPDS, словари, E-Ink |
| `cr3qt` | C++ / Qt5 и Qt6 | Десктопный клиент |
| `cr3gui` | C++ | PocketBook, Jinke, XCB, Nano-X и другие legacy-фронтенды |
| `cr3wx` | C++ / wxWidgets | Legacy-десктопный клиент |
| `fb2props` | C++ библиотека и тестовая утилита | Извлечение метаданных FB2 |
| `tinydict` | C++ | Работа со словарями StarDict |

### 3.2. Масштаб и архитектурные hotspots

- В проверенных C/C++/Java каталогах около 324 тысяч строк, включая большие
  сгенерированные locale/encoding tables. Это не проект для безопасного
  «переписывания целиком».
- Главные production hotspots:
  - [`lvtinydom.cpp`](crengine/src/lvtinydom.cpp) — около 20 тысяч строк;
  - [`lvrend.cpp`](crengine/src/lvrend.cpp) — около 11 тысяч;
  - [`lvdocview.cpp`](crengine/src/lvdocview.cpp) — около 7,5 тысячи;
  - [`ReaderView.java`](android/src/org/coolreader/crengine/ReaderView.java) —
    около 6,7 тысячи;
  - `OptionsDialog`, `docview.cpp`, `CoolReader`, `BaseActivity`, `MainDB`,
    `Synchronizer` и `TTSControlService` — от 1,7 до 3,2 тысячи строк каждый.
- Основной Android-поток данных сейчас выглядит так:

  `Intent / Scanner / OPDS → FileInfo / MainDB → ReaderView → DocView / JNI → LVDocView → importer / DOM / layout / cache`.

  Изменение пути, идентификатора книги или формата cache затрагивает сразу
  несколько слоёв, поэтому такие задачи требуют вертикального интеграционного
  теста, а не только unit-теста одного класса.
- Android построен вокруг одной `Activity`, собственных `View`/`Dialog`, статических
  сервисов и одного `BackgroundThread`. Миграция UI без выделения use-case и data
  boundaries будет переносить старую связанность в новый toolkit.
- Root CMake объединяет Qt4/5/6, wxWidgets, XCB, Nano-X, PocketBook, Jinke, Win32 и
  служебные targets. Поддержку следует разделить на уровни, иначе любой
  «обязательный» CI становится практически невыполнимым.

### 3.3. Подтверждённые проблемы и ограничения

#### Сборка и CI

- Корневой [`CMakeLists.txt`](CMakeLists.txt) содержит лишнюю обратную кавычку
  после значения `CMAKE_CXX_STANDARD 17` и поэтому не проходит генерацию CMake.
- Для GNU-компиляторов одновременно добавляется `-std=c++11`, что конфликтует с
  целевым C++17.
- В GitHub Actions сейчас проверяется только `clang-format`; сборки Android,
  `crengine`, Qt и `fb2props` не проверяются.
- Android использует Gradle 8.7, AGP 8.6.1, `compileSdkVersion 35` и
  `targetSdkVersion 35` в
  [`android/app/build.gradle`](android/app/build.gradle); повторная «миграция на
  Gradle 8 / SDK 35» не нужна.
- `testInstrumentationRunner` всё ещё указывает на legacy
  `android.support.test.runner.AndroidJUnitRunner`, при этом в модуле не видно
  настроенного современного test stack.
- Версия NDK не закреплена в Gradle-конфигурации.

#### Android Storage Access Framework

- `content://` URI уже открываются через `ContentResolver.openInputStream()`.
- Текущая реализация в
  [`ReaderView.java`](android/src/org/coolreader/crengine/ReaderView.java)
  копирует весь поток в `ByteArrayOutputStream`, затем создаёт ещё один `byte[]` и
  передаёт книгу в JNI. Это создаёт риск `OutOfMemoryError` и не подходит для
  больших EPUB/FB2-архивов.
- POSIX-реализация `LVFileStream` в
  [`lvfilestream.cpp`](crengine/src/lvstream/lvfilestream.cpp) уже хранит
  `int m_fd` и работает через `read`, `write`, `lseek` и `fstat`. Для SAF не нужен
  `fdopen`; нужен безопасный конструктор/фабрика из дублированного file descriptor.
- Не каждый `ContentProvider` возвращает seekable descriptor. Для таких
  провайдеров нужен fallback с потоковым копированием во временный cache-файл.
- Для повторного открытия книги после перезапуска необходимо сохранять URI,
  persistable permission, отображаемое имя, MIME type и стабильный идентификатор
  записи истории.

#### Сетевая безопасность и приватность

- [`OPDSUtil.java`](android/src/org/coolreader/crengine/OPDSUtil.java) создаёт
  `X509TrustManager`, принимающий любой сертификат, меняет default
  `SSLSocketFactory` всего процесса и разрешает любое имя хоста. Это stop-ship
  уязвимость, а не технический долг.
- Там же используется глобальный `Authenticator.setDefault()`, вручную собирается
  заголовок авторизации и автоматически выполняются redirect. Учётные данные
  нельзя передавать на другой origin или оставлять глобальным состоянием процесса.
- [`LitresConnection.java`](android/src/org/coolreader/plugins/litres/LitresConnection.java)
  использует HTTP для авторизации, регистрации, покупки и загрузки, а HTTPS явно
  отклоняет. Логин и пароль сохраняются открытым текстом в `SharedPreferences`.
- Таблица `opds_catalog` в
  [`MainDB.java`](android/src/org/coolreader/db/MainDB.java) также хранит username
  и password открытым текстом.
- Manifest одновременно разрешает cleartext traffic, включает backup приложения,
  запрашивает legacy storage и `READ_PHONE_STATE`. Пока секреты не удалены из
  backup surface, это повышает последствия компрометации.
- OPDS и LitRes создают SAX parser без явно заданного безопасного набора XML
  features. Для сетевого XML нужны запрет DTD/external entities, лимиты ответа и
  тесты hostile payloads.
- OPDS-загрузка с неизвестным `Content-Length` пишет до EOF без общего byte/time
  budget. Одного connect/read timeout недостаточно для ограничения диска и трафика.

#### База данных, история и настройки

- `MainDB` имеет schema version 34 и большой линейный `upgradeSchema()`. Часть
  шагов выполняется через `execSQLIgnoreErrors()`, а при версии БД выше
  поддерживаемой код принудительно трактует её как version 26. Без fixture-based
  тестов это создаёт риск частичной миграции и потери данных.
- Миграция жанров временно выключает foreign keys и выполняет серию DDL/DML
  операций без явного атомарного migration contract.
- Идентичность книги основана прежде всего на `pathname`; `FileInfo` и таблица
  `book` не моделируют `content://` URI как отдельный тип источника.
- Для сравнения книг и sync всё ещё используется CRC32. Он пригоден как быстрый
  change hint, но не как устойчивый глобальный `BookKey`.
- Настройки, история, закладки, cache и база имеют разные механизмы сохранения и
  восстановления. Для них нет общей документированной политики backup, rollback,
  privacy и retention.

#### Недоверенные документы и native boundary

- ZIP reader ограничивает длину имени и extra field, но в просмотренном пути нет
  общего бюджета на количество entries, суммарный uncompressed size, compression
  ratio и стоимость вложенного разбора.
- На JNI-границе десятки ручных методов и signal-based `coffeecatch`. Нужны
  contract tests для null/exception/local-reference handling и отдельное решение,
  допустимо ли продолжать работу процесса после фатального native signal.
- В production-коде остаются `sprintf`/`strcpy` и большие ручные буферы. Это не
  доказывает наличие exploit, но требует compiler warnings, sanitizers и
  прицельного перевода внешних данных на bounded APIs.
- Текущие тесты — преимущественно старые tools и smoke-утилиты. Нет постоянного
  fuzzing для ZIP, XML/HTML/FB2, EPUB/DOCX/ODT, изображений и cache deserialization.

#### Потокобезопасность

- Java-обёртка [`DocView`](android/src/org/coolreader/crengine/DocView.java)
  сериализует JNI-вызовы через общий `mutex`.
- `LVDocView` уже содержит `LVMutex`, а многие C++ методы используют `LVLock`.
- В pthread-реализации
  [`LVMutex`](crengine/include/lvthread.h) результат `pthread_mutex_init()`
  интерпретируется наоборот: успешная инициализация делает `_valid == false`. Это
  необходимо исправить до проектирования новой модели блокировок.
- Наличие конкретной гонки между версткой, поиском и выделением текста должно быть
  подтверждено воспроизводимым тестом или ThreadSanitizer, а не предположением.
- `std::shared_mutex` допустим только после классификации операций на действительно
  read-only и mutating: многие «читающие» операции лениво обновляют кэши и layout.

#### E-Ink

- Поддержка Onyx в
  [`EinkScreenOnyx.java`](android/src/org/coolreader/crengine/EinkScreenOnyx.java),
  `DeviceInfo.java` и `BaseActivity.java` отключена комментариями из-за вопроса
  совместимости лицензии SDK с GPL.
- Отключённый Onyx-код уже ориентирован на API SDK, а не на
  `android.os.EinkManager`.
- Рефлексия над приватными `android.hardware.EpdController` находится в
  Nook-адаптере `N2EpdController`, поэтому её нельзя удалять как часть Onyx-задачи
  без проверки старых Nook-устройств.

#### Android lifecycle, permissions и поставка

- `minSdkVersion 4` конфликтует с фактическим native minimum API 21, ошибка которого
  подавлена через `android.ndk.suppressMinSdkVersionError=21`.
- Release lint не блокирует сборку, основной release не minify/strip, а
  `doNotStrip "**/*.so"` упаковывает native symbols внутрь APK. Символы нужны для
  диагностики, но должны поставляться отдельным артефактом.
- `TTSControlService` объявлен как `mediaPlayback` foreground service, но manifest
  содержит только общую `FOREGROUND_SERVICE`, без обязательной для target 35
  [`FOREGROUND_SERVICE_MEDIA_PLAYBACK`](https://developer.android.com/about/versions/14/changes/fgs-types-required).
- `PhoneStateReceiver` экспортирован, держит static callback, регистрирует
  `PhoneStateListener` без надёжного unregister и полагается на `finalize()`.
  При уже реализованном Audio Focus это отдельный кандидат на удаление
  `READ_PHONE_STATE`.
- `TTSControlService`, `ReaderView`, `BaseActivity` и статический `Services`
  смешивают lifecycle, состояние, threading и UI. Перед UI-миграцией нужны
  lifecycle tests и узкие interfaces.
- TTS и sync notifications создают `PendingIntent` с flags `0`, местами для
  неявных broadcast actions. Начиная с target API 31
  [mutability должна быть указана явно](https://developer.android.com/about/versions/12/behavior-changes-12);
  для target 35 нужны также безопасные identity и package-scoped control intents.
- Donations используют встроенный legacy In-App Billing v3 AIDL. Нужно решить:
  мигрировать на поддерживаемый billing client либо удалить этот путь из
  соответствующего flavor; purchase response нельзя считать доверенным без
  проверки.
- В manifest присутствуют очень широкие VIEW intent filters, включая
  `application/octet-stream`; входящие URI и MIME нельзя считать доверенными.

#### Строки, контейнеры и DOM

- `lString32` использует reference counting и copy-on-write. Переход на
  `std::u32string` не гарантирует ускорение и может увеличить количество копирований
  и потребление памяти.
- `LVPtrVector<T>` поддерживает владеющий и невладеющий режим
  (`LVPtrVector<T, false>`). Невладеющие коллекции нельзя механически заменить на
  `std::vector<std::unique_ptr<T>>`.
- [`ldomNode`](crengine/include/lvtinydom.h) — компактный handle, выделяемый
  собственным пулом `tinyNodeCollection`; оператор `delete` намеренно отключён.
  Владение жизненным циклом находится у документа/пула, а не у каждого указателя.
- Любая модернизация базовых типов имеет большой blast radius и должна выполняться
  небольшими сериями изменений с тестами и замерами.

#### EPUB, CSS и синхронизация

- `epub:type` — семантический XML-атрибут EPUB, а не CSS-свойство. Его обработка
  относится к EPUB importer / DOM mapping и стандартным стилям.
- В движке уже есть механизм `-cr-hint` для `footnote`, `noteref` и
  `footnote-inpage`; новую поддержку сносок следует строить поверх него.
- Flexbox и Grid требуют изменений не только CSS-парсера, но и модели стилей,
  layout, пагинации, hit-testing и сериализации кэша.
- В Android уже есть каркас [`sync2`](android/src/org/coolreader/sync2)
  (`Synchronizer`, `RemoteAccess`, `SyncService`), но нет рабочей свободной
  реализации удалённого провайдера.
- Google Drive-код отключён из-за зависимости от проприетарных Google Play Services.

#### Зависимости, документация и сопровождение

- `thirdparty_repo/*.meta.sh` фиксирует версии и SHA-512, но часть download URL
  остаётся HTTP.
- Одновременно в дереве находятся metadata новых версий и Android-vendored
  конфигурации старых версий, например HarfBuzz 2.8.0 и libpng 1.6.37. Нужен один
  генерируемый source of truth, показывающий, какие именно исходники вошли в
  конкретный artifact.
- Нет CI jobs для build/test/lint/security; единственный workflow проверяет
  `clang-format` изменённых C/C++ файлов.
- README содержит актуальные разделы рядом с инструкциями Qt SDK, Visual Studio
  2008/2010 и OpenInkpot, помеченными obsolete. `CONTRIBUTING.md` не описывает
  настройку среды, тестовые команды, release flow и support tiers.
- Changelog верхнего уровня не отражает современные Android/engine релизы.

---

## 4. Инженерные принципы

1. **Сначала baseline, затем рефакторинг.** Любое изменение ядра сравнивается с
   зафиксированным набором книг и метрик.
2. **Малые миграции.** Один тип, подсистема или формат за серию небольших PR.
3. **Совместимость важнее чистоты API.** Сохранение форматов кэша, позиций чтения,
   закладок и поведения верстки проверяется автоматически.
4. **Нет неподтверждённых оптимизаций.** Цели производительности задаются после
   измерения baseline.
5. **Явное владение.** Для каждой коллекции документируется owner, observer и
   время жизни объектов.
6. **Раздельные релизы.** Storage, UI, engine и sync могут выпускаться независимо.
7. **Недоверенный вход по умолчанию.** URI, OPDS/XML, ZIP entries, книги, изображения,
   шрифты, cache и sync payloads проходят лимиты и не управляют глобальной
   конфигурацией процесса.
8. **Секреты не являются metadata.** Password/token не хранятся в основной БД,
   обычных preferences, логах или backup; credential store имеет отдельный lifecycle.
9. **Миграции — часть продукта.** Каждое изменение schema/settings/cache имеет
   fixtures предыдущих версий, atomicity rule и recovery path.
10. **Уровни поддержки вместо иллюзии паритета.** Tier 1 targets блокируют merge,
    Tier 2 проверяются регулярно, archived targets собираются только владельцами.
11. **Большие файлы делятся по seams, а не по строкам.** Сначала выделяются
    проверяемые boundaries — transport, repository, parser, layout stage,
    lifecycle controller — затем переносится код.

### 4.1. Операционная модель форка

#### Remotes и ветки

- Сейчас локальный `origin` указывает непосредственно на
  `https://github.com/buggins/coolreader.git`. После создания GitHub fork:
  - `origin` должен указывать на репозиторий форка с правом push;
  - `upstream` — на `https://github.com/buggins/coolreader.git`;
  - URL форка не следует угадывать: его нужно подставить после создания
    репозитория владельцем.
- `upstream/master` остаётся неизменяемой ссылкой на канонический проект.
- `origin/master` — защищённая integration/release ветка форка без force-push.
- Рабочие изменения идут короткими feature branches; rebase допустим до merge,
  но не для опубликованных shared/release веток.

#### Классификация изменений

Каждый PR получает один из трёх типов:

1. `upstreamable` — общая ошибка, тест, parser hardening, совместимое улучшение
   сборки или документации; сначала готовится в форме, пригодной для PR upstream.
2. `fork-only` — продуктовая политика, новый UI/branding/provider или изменение,
   которое upstream явно не принимает; требуется owner и ADR.
3. `temporary-delta` — workaround до появления upstream fix; обязательно содержит
   ссылку на upstream issue/PR и условие удаления.

#### Правила минимизации расхождений

- Не смешивать upstreamable bugfix с fork-only UI/schema изменением в одном PR.
- Новые функции по возможности изолировать adapter, flavor, feature flag или
  отдельным модулем, сохраняя интерфейсы `crengine`.
- Не переименовывать и не форматировать большие upstream-файлы без функциональной
  необходимости.
- Общие security/parser/build fixes предлагать upstream без секретов, приватного
  corpus и данных пользователей.
- Вести `FORK_DELTA.md`: commit/PR, категория, затронутые файлы, owner, причина,
  upstream link и removal condition.

#### Синхронизация и версии

- Автоматически выполнять `fetch upstream` и пробную сборку/тест merge результата
  по расписанию; реальное обновление integration branch проходит обычный PR.
- При каждом upstream release оценивать commits по security, formats, cache/schema,
  dependencies и Android platform отдельно, а не делать слепой merge.
- Версия форка сохраняет upstream base и добавляет однозначный fork suffix/build
  metadata; Android `versionCode` остаётся монотонным.
- Release notes разделяют: upstream changes, backports, fork-only changes и
  временные отклонения.
- Security fixes с общим воздействием координируются с upstream до публикации,
  когда это не задерживает защиту пользователей форка.

---

## 5. Дорожная карта

### Фаза 0A. Security containment и stop-ship gate

**Срок:** 3–7 рабочих дней на containment, 2–3 недели на полную миграцию

**Приоритет:** P0 / блокирует публичный релиз

**Зависимости:** нет; идёт параллельно с Фазой 0

#### Немедленное containment

- [x] Удалить trust-all `X509TrustManager`, allow-all `HostnameVerifier` и
  process-wide `setDefaultSSLSocketFactory()` из OPDS.
- [x] Удалить global `Authenticator.setDefault()`; передавать credentials только
  конкретному запросу и только после same-origin redirect policy.
- [ ] Добавить regression tests, доказывающие отказ для self-signed, expired и
  hostname-mismatch сертификатов.
- [x] Временно отключить authenticated LitRes actions и удаление/покупку через
  HTTP. Не «исправлять» это разрешением cleartext в Network Security Config.
- [x] По умолчанию установить `usesCleartextTraffic=false`; исключения допустимы
  только для явного пользовательского локального каталога и отдельного opt-in
  debug/compatibility режима без credentials.
- [x] Запретить DTD и external entities во всех SAX parsers сетевого слоя.
- [x] Ввести лимиты OPDS feed/download: bytes, redirects, items, wall time,
  destination space и cancellation.

#### Миграция секретов и приватности

- [x] Инвентаризировать `opds_catalog.password`, `litres.password`, SID/token и
  возможные fork-specific поля.
- [x] Перенести поддерживаемые credentials в Android Keystore-backed storage либо
  потребовать повторный ввод, если надёжная миграция невозможна.
- [x] Очистить plaintext credentials из SQLite и SharedPreferences после
  успешной миграции; миграция должна быть идемпотентной.
- [ ] Добавить `dataExtractionRules`/backup rules: не экспортировать credentials,
  временные загрузки, cache, логи и незавершённые sync payloads.
- [x] Проверить, что URL, query, headers, login, SID/token и заметки не попадают в
  production logs или crash reports.
- [ ] Добавить `SECURITY.md` с каналом disclosure и поддерживаемыми версиями.

#### Критерии приёмки

- В production-коде нет trust-all TLS и allow-all hostname verifier.
- Авторизация не отправляется по HTTP и не переживает cross-origin redirect.
- Ни один пароль/token не находится в обычной БД, preferences, backup или логах.
- Hostile XML, redirect loop и бесконечный/слишком большой ответ завершаются
  контролируемой ошибкой.
- До выполнения gate новый публичный APK не считается release candidate.

### Фаза 0. Восстановление сборки и определение baseline

**Срок:** 1–2 недели

**Приоритет:** P0

**Зависимости:** нет

#### Задачи

- [x] Удалить лишнюю обратную кавычку после `CMAKE_CXX_STANDARD 17` и
  конфликтующий `-std=c++11`.
- [ ] После создания fork настроить `origin`/`upstream`, protection rules и
  документированную команду обновления без переписывания shared history.
- [ ] Добавить `FORK_DELTA.md` и PR labels `upstreamable`, `fork-only`,
  `temporary-delta`.
- [x] Сделать C++17 единственным обязательным стандартом первой программы
  модернизации.
- [ ] Зафиксировать минимальные версии CMake, JDK и NDK.
- [x] Согласовать Android `minSdk` с фактическим native API и удалить
  `android.ndk.suppressMinSdkVersionError`.
- [ ] Зафиксировать support tiers:
  - Tier 1: Android + `crengine` на Linux;
  - Tier 2: Qt6 на выбранных desktop OS;
  - maintenance/archived: wxWidgets и legacy E-Ink фронтенды без активного owner.
- [ ] Добавить CI-сборки:
  - `crengine` + `fb2props` на Linux;
  - Android debug APK для `arm64-v8a`, `armeabi-v7a` и `x86_64`;
  - Qt6 smoke build сначала на Linux, затем на macOS и Windows.
- [x] Добавить обязательные Android lint, Java compile warnings, CTest и smoke run;
  убрать глобальный `abortOnError false`.
- [ ] Сохранять логи и артефакты сборки.
- [x] Добавить ASan/UBSan job для поддерживаемого desktop-конфига.
- [ ] Добавить отдельный build с Clang warnings; новые warnings блокируют merge,
  старые фиксируются baseline-файлом и уменьшаются.
- [ ] Закрепить Gradle wrapper checksum, NDK revision и container/toolchain image.
- [ ] Добавить scheduled upstream-sync dry run: fetch, test merge, Tier 1 build,
  dependency/schema/cache change report без автоматического push.
- [ ] Задокументировать реально поддерживаемые фронтенды и платформы.

#### Критерии приёмки

- Чистая конфигурация CMake и сборка проходят в CI.
- Android debug APK собирается из чистого checkout.
- В коде нет конкурирующих флагов `-std=`.
- Lint/test failures действительно останавливают CI.
- Матрица платформ опубликована в README/CONTRIBUTING.
- Upstream update воспроизводится через PR, а fork delta имеет владельцев и
  upstream/removal links.

### Фаза 1. Тестовый корпус и измерения

**Срок:** 2–4 недели

**Приоритет:** P0

**Зависимости:** Фаза 0; security fixtures проектируются параллельно с Фазой 0A

#### Задачи

- [ ] Создать легально распространяемый corpus: FB2, EPUB 2/3, TXT, HTML, DOCX,
  ODT, CHM, PDB, архивы, RTL, CJK, изображения, шрифты, сноски, очень большие и
  повреждённые документы.
- [ ] Добавить smoke-тесты открытия, определения формата, metadata и page count.
- [ ] Добавить layout regression tests с контролируемыми шрифтами и настройками.
- [ ] Разделить corpus на:
  - golden fixtures с ожидаемой metadata/layout;
  - malformed/security fixtures;
  - performance fixtures, которые не хранятся в обычном unit-test репозитории;
  - private compatibility corpus с хешами и правилами доступа.
- [ ] Зафиксировать benchmark:
  - время парсинга;
  - время первой верстки;
  - перелистывание и поиск;
  - peak RSS / Java heap;
  - размер и время загрузки DOM cache.
- [ ] Добавить stress-сценарий: открыть → искать → выделить → сменить размер →
  перелистнуть → закрыть, многократно.
- [ ] Подготовить ThreadSanitizer job для desktop-сценариев, где это возможно.
- [ ] Добавить libFuzzer/AFL++ harnesses минимум для:
  - format detection и `LVOpenArchive`;
  - ZIP central/local headers и decompression stream;
  - XML/HTML/FB2 parser;
  - EPUB/DOCX/ODT package metadata;
  - PNG/JPEG/SVG front door;
  - DOM cache deserialize.
- [ ] Запускать короткий fuzz smoke в PR, длительный sanitizer/fuzz job ночью и
  сохранять минимизированный regression input.
- [ ] Добавить сетевые contract tests на локальном HTTP(S) server: TLS failures,
  redirect, auth origin, gzip, chunked/unknown length, cancellation и disk-full.
- [ ] Добавить Android instrumentation scenarios для Activity recreation,
  process death, service rebind и входящего `content://` intent.

#### Критерии приёмки

- Все последующие фазы имеют воспроизводимый функциональный и performance baseline.
- Регрессии верстки и памяти видны до merge.
- KPI измеряются одной задокументированной командой.
- Каждый исправленный parser crash остаётся минимизированным regression test.

### Фаза 2. Android SAF без полной буферизации

**Срок:** 3–5 недель

**Приоритет:** P0

**Зависимости:** Фазы 0A–1

#### Архитектура

1. Java получает `ParcelFileDescriptor` через `ContentResolver`.
2. JNI немедленно вызывает `dup(fd)`; жизненный цикл Java descriptor после этого
   не влияет на C++ stream.
3. `LVFileStream` принимает owned descriptor, определяет размер через `fstat` и
   закрывает только собственную копию.
4. Если descriptor не поддерживает `lseek`, Java или native storage adapter
   потоково копирует данные в cache-файл с проверкой свободного места и отменой.
5. История хранит URI и metadata отдельно от псевдопути, используемого внутри
   форматов и архивов.

#### Задачи

- [x] Добавить `LVOpenFileDescriptorStream(int fd, const lString32 &name)` и тесты
  владения descriptor.
- [x] Добавить JNI API открытия из file descriptor.
- [x] Сохранить текущий memory-stream путь только для небольших ресурсов с явным
  лимитом размера.
- [x] Реализовать seekability check и cache fallback.
- [x] Гарантировать закрытие `InputStream`/`ParcelFileDescriptor` во всех success,
  cancel, error и process recreation paths.
- [ ] Сохранять persistable URI permission после `ACTION_OPEN_DOCUMENT` и
  `ACTION_OPEN_DOCUMENT_TREE`.
- [ ] Для входящего `ACTION_VIEW` корректно обрабатывать временный grant:
  предлагать импорт/копию либо явно объяснять, что повторное открытие невозможно.
- [ ] Обновить `FileInfo`, историю и scanner для URI-backed документов.
- [x] Корректно обрабатывать EPUB/FB2 внутри ZIP, MIME type и отображаемое имя.
- [ ] Добавить очистку временных файлов по LRU/лимиту.

#### Критерии приёмки

- Книга размером не менее 250 МБ открывается без копирования всего файла в Java
  heap.
- Seekable и non-seekable `ContentProvider` проходят интеграционные тесты.
- Книга повторно открывается после перезапуска приложения.
- Проверены Android API 30, 34 и 35, внутренняя память и съёмная SD-карта.
- Нет утечек descriptor и временных файлов.

### Фаза 2B. Persistence, schema и стабильная идентичность книги

**Срок:** 3–5 недель

**Приоритет:** P0

**Зависимости:** Фазы 0A–1; schema URI координируется с Фазой 2

#### Модель данных

- [ ] Ввести `DocumentSource`: local path, archive entry, persisted URI,
  temporary URI/import и remote acquisition. Не перегружать одно поле `pathname`.
- [ ] Ввести versioned `BookKey`: криптографический digest контента при наличии,
  размер и нормализованные metadata как hints; CRC32 оставить только быстрым hint.
- [x] Разделить public catalog metadata и credentials. В SQLite допустим
  credential reference/alias, но не пароль.
- [ ] Определить каноническую модель archive entry, чтобы `archive@/entry` не
  конфликтовал с URI encoding и обычными путями.

#### Миграции и восстановление

- [ ] Перевести schema upgrade в последовательность именованных migrations с одной
  транзакцией на migration и проверкой postconditions.
- [x] Не понижать неизвестную будущую version до 26. Открывать такую БД read-only
  или завершаться понятной ошибкой с сохранением оригинала.
- [ ] Убрать `execSQLIgnoreErrors()` из обязательных migration steps; допустимые
  recovery cases проверять явно через schema inspection.
- [ ] Создать anonymized DB fixtures для version 0, 6, 23, 28, 32, 33, 34,
  corrupted/partial migration и future version.
- [ ] Проверять foreign keys, indexes, uniqueness, bookmark ownership и orphan
  cleanup после каждой migration.
- [ ] Сделать backup/restore атомарным: temp file, `fsync`, integrity check,
  rename; хранить последнюю известную исправную копию.
- [ ] Задокументировать retention/delete/export для истории, закладок, search
  history, OPDS credentials, sync и cache.

#### Scanner и библиотека

- [ ] Сделать scan job отменяемым и resumable, с ограниченной очередью metadata/
  cover parsing и явным progress model.
- [ ] Не пересканировать неизменённые архивы/деревья; сравнивать source identity,
  размер, modification metadata и fingerprint по определённой политике.
- [ ] Сохранять batch результатов одной транзакцией и не показывать UI частично
  обновлённое дерево как завершённое.
- [ ] Добавить benchmark библиотек на 1 000 и 10 000 книг, глубокие каталоги,
  большие ZIP и медленный provider.
- [ ] Проверить duplicate detection при переименовании, переносе, новом mount path
  и смене URI permission.

#### Критерии приёмки

- Все migration fixtures обновляются до текущей schema без потери книги,
  закладок, позиции и времени чтения.
- Interrupted migration либо полностью откатывается, либо детерминированно
  восстанавливается из backup.
- Local path и persisted URI одной книги корректно объединяются или показываются
  отдельно по явно выбранной политике.
- Future-version DB никогда не переписывается как старая schema.
- Прерванный scan безопасно возобновляется и не создаёт дубликаты/осиротевшие
  связи.

### Фаза 3. Потокобезопасность и стабильность ядра

**Срок:** 2–4 недели

**Приоритет:** P0

**Зависимости:** Фазы 0–1

#### Задачи

- [x] Исправить инвертированную проверку `pthread_mutex_init()`.
- [x] Добавить unit-тесты `LVMutex`, `trylock` и RAII `LVLock`.
- [ ] Построить карту потоков Java → JNI → `LVDocView`.
- [ ] Зафиксировать и автоматизировать сценарий предполагаемой гонки.
- [ ] Проверить все обходы Java `DocView.mutex` и прямые обращения к native view.
- [ ] Разделить методы `LVDocView` на mutating, logically-read-only и cache-mutating.
- [ ] Рассматривать `std::shared_mutex` только если обычная корректная блокировка
  создаёт измеримый bottleneck.

#### Критерии приёмки

- Stress-тест не выявляет race/use-after-free под TSan/ASan в поддерживаемой
  конфигурации.
- Android-сценарии поиска, выделения, изменения настроек и перелистывания проходят
  1 000 последовательных итераций без сбоя.
- Модель блокировок и порядок захвата документированы.

### Фаза 4. Android platform и UI

**Срок:** 6–10 недель

**Приоритет:** P1

**Зависимости:** Фазы 0A, 0, 2 и 2B; может идти параллельно с Фазой 3

#### Сначала — platform hygiene

- [x] Закрепить NDK и JDK в репозитории/CI.
- [ ] Привести manifest к least privilege: удалить legacy external-storage
  permissions после SAF, пересмотреть `READ_PHONE_STATE`, exported components,
  broad MIME filters и backup rules.
- [ ] Удалить устаревшие SDK declarations и legacy manifest/config, которые не
  входят в активный source set.
- [x] Принять отдельное продуктовое решение о новом `minSdk`; текущее значение 4
  не соответствует фактическому native minimum API 21.
- [ ] Мигрировать на AndroidX без одновременной переделки экранов.
- [x] Добавить lint как обязательную CI-проверку; не отключать все release errors.
- [ ] Проверить foreground-service/media-playback и notification behavior на всех
  поддерживаемых API уровнях; добавить
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` и manifest/runtime tests.
- [ ] Сделать все `PendingIntent` explicit, задать корректный immutable/mutable
  flag и уникальный identity; ограничить TTS/sync control broadcasts приложением.
- [ ] Мигрировать или удалить legacy In-App Billing v3 donation flow; добавить
  purchase verification и billing lifecycle tests, если он остаётся.
- [ ] Удалить lifecycle через `finalize()`: регистрировать и снимать telephony,
  noisy receiver, timers, MediaSession, TTS и service binding симметрично.
- [ ] Предпочесть Audio Focus остановке TTS через глобальный
  `PhoneStateReceiver`; если receiver остаётся, сделать его минимальным,
  неэкспортированным где возможно и покрыть permission tests.
- [ ] Перейти с deprecated `onActivityResult` на Activity Result APIs после
  стабилизации storage flows.

#### Architecture seams перед сменой toolkit

- [ ] Вынести из `ReaderView` контроллер загрузки/закрытия документа, reader state
  и animation policy.
- [ ] Вынести из `CoolReader` intent routing, permission/document picker и
  navigation state.
- [ ] Разделить `OptionsDialog` на typed settings schema и presentation.
- [ ] Поставить repository interfaces перед `MainDB`/`CRDBService`, не переписывая
  всю БД одновременно.
- [ ] Заменить глобальный service locator `Services` явным application scope и
  тестовыми fakes поэтапно.

#### Затем — UI

- [ ] Зафиксировать выбор: Material 3 Views или Compose. Kotlin/Compose не считать
  обязательными без отдельного ADR.
- [ ] Мигрировать по экранам: библиотека → файловый браузер → reader controls →
  настройки.
- [ ] Сохранить быстрый E-Ink режим без тяжёлых анимаций.
- [ ] Добавить accessibility, scalable text, landscape/tablet layouts.

#### E-Ink decision gate

- [ ] Проверить актуальную лицензию и способ распространения Onyx SDK.
- [ ] Если лицензия совместима — подключить SDK через отдельный адаптер/flavor.
- [ ] Если несовместима — оставить нейтральный публичный API или документированный
  optional integration, не добавляя проприетарный код в GPL-сборку.
- [ ] Не менять Nook/Tolino adapters без тестовой матрицы устройств.

#### Критерии приёмки

- Основные reader-сценарии работают на phone, tablet и E-Ink профилях.
- UI-миграция не меняет формат настроек и истории без мигратора.
- Activity recreation и process death не оставляют native view, service binding,
  receiver, timer или descriptor.
- Onyx-решение имеет зафиксированный юридический и технический ADR.

### Фаза 5. Инкрементальная модернизация `crengine`

**Срок:** 8–16 недель сериями небольших PR

**Приоритет:** P1

**Зависимости:** Фазы 0A–3

#### Строки

- [ ] Добавить безопасную совместимость с `std::u32string_view` на границах API,
  не меняя сразу внутреннее хранение.
- [ ] Сравнить COW `lString32` и альтернативы на реальных workload.
- [ ] Менять внутреннее представление только при доказанном выигрыше или
  устранении конкретного класса ошибок.

#### Контейнеры

- [ ] Классифицировать использования `LVArray` и `LVPtrVector` по владению.
- [ ] Локально заменять value collections на `std::vector<T>`.
- [ ] Владеющие коллекции переводить на `std::vector<std::unique_ptr<T>>`.
- [ ] Невладеющие коллекции переводить на `std::vector<T *>` или
  специализированный observer view/container, совместимый с C++17.
- [ ] Не проводить глобальную замену typedef/макросом.

#### DOM

- [ ] Сохранить pool ownership и компактный `ldomNode`.
- [ ] Документировать валидность raw handles и правила инвалидирования.
- [ ] При необходимости добавить debug generation/cookie для обнаружения stale
  handles.
- [ ] Изменение layout DOM или формата persistent cache выполнять через версию и
  миграцию/инвалидацию кэша.

#### Проверяемые границы

- [ ] Разделять `lvtinydom.cpp`, `lvrend.cpp`, `lvdocview.cpp` и `lvstsheet.cpp`
  только вокруг уже покрытых тестами стадий: parse, computed style, layout,
  pagination, navigation и cache.
- [ ] Ввести typed result/error на parser/cache boundaries вместо неразличимых
  `false`/`null` и silent ignore.
- [ ] Сгенерировать/проверять JNI registration table из одного описания либо
  добавить compile-time/signature contract test, чтобы Java и C++ declarations не
  расходились.
- [ ] Заменять небезопасные форматирующие/copy API сначала там, где размер зависит
  от документа, пути, языка или внешнего сервиса.
- [ ] Отделить crash reporting от recovery: после SIGSEGV/SIGBUS native state не
  считать пригодным для продолжения чтения без доказанной стратегии.

#### Критерии приёмки

- Каждая серия миграций проходит functional corpus, ASan/UBSan и benchmark.
- Допустимая регрессия median performance — не более 5% без отдельного решения.
- Peak memory не растёт более чем на 10% без объяснённого компромисса.
- Публичные API и форматы кэша либо совместимы, либо имеют версионированную
  миграцию.

### Фаза 5B. Hardening форматов, архивов и cache

**Срок:** 4–8 недель, затем постоянно

**Приоритет:** P0 для лимитов и crash fixes, P1 для глубокой переработки

**Зависимости:** Фазы 0–1; может идти параллельно с 2–5

#### Resource budgets

- [ ] Ввести единый `ParseBudget`: максимальный размер входа, entries, суммарный
  uncompressed size, compression ratio, nesting/depth, XML nodes/attributes,
  image pixels, font bytes, wall time и cancellation token.
- [ ] Применить budget к ZIP/EPUB/FB3/DOCX/ODT, встроенным изображениям,
  stylesheets, base64 blobs и сетевым загрузкам.
- [ ] Проверять integer overflow до сложения offset/size и до allocation.
- [ ] Валидировать archive path normalization, даже когда запись читается как
  stream и не извлекается напрямую на диск.
- [ ] Для слишком сложного документа возвращать диагностируемую ошибку, не OOM,
  hang или частично повреждённый cache.

#### Parser и cache lifecycle

- [ ] Проверять cache header/version/options/input fingerprint до deserialization.
- [ ] Делать запись cache атомарной и не загружать partial file после process kill.
- [ ] Разделить «unsupported», «corrupted», «budget exceeded» и internal error.
- [ ] Добавить differential tests: чтение напрямую и из cache даёт одинаковую
  структуру/позиции на golden corpus.
- [ ] Минимизировать каждый sanitizer/fuzzer crash и привязывать regression fixture
  к issue/CVE, если применимо.

#### Критерии приёмки

- ZIP bomb, oversized image/XML tree и повреждённый cache прекращаются в
  установленном budget без crash/OOM и без сохранения partial результата.
- 24-часовой fuzz run ключевых harnesses не имеет воспроизводимых crash,
  sanitizer finding или uncontrolled allocation.
- Ошибка формата содержит безопасный reason code, но не раскрывает секреты или
  полный пользовательский путь в telemetry.

### Фаза 6. EPUB 3, CSS и переносы

**Срок:** 6–12 недель на выбранный набор функций

**Приоритет:** P2

**Зависимости:** Фазы 1, 5 и 5B

#### 6A. EPUB 3 semantics

- [ ] Составить compatibility matrix по EPUB 3.
- [ ] Нормализовать `epub:type` в importer/DOM mapping.
- [ ] Связать `noteref`/`footnote` с существующими `-cr-hint`.
- [ ] Добавить fixtures для popup и in-page footnotes, pagebreak и navigation.

#### 6B. CSS layout

- [ ] Подготовить отдельный RFC для Flexbox с точным подмножеством свойств.
- [ ] Реализовать parser → computed style → layout → pagination → hit-testing
  вертикальными срезами.
- [ ] Не включать Grid в этот milestone; сначала собрать реальные EPUB fixtures и
  оценить отдельный проект.

#### 6C. Hyphenation

- [ ] Обновить словари и правила их версионирования.
- [ ] Добавить языковые regression fixtures.
- [ ] Измерять качество переносов отдельно от скорости layout.

#### Критерии приёмки

- Каждая новая возможность имеет список поддерживаемых и неподдерживаемых случаев.
- Старые документы сохраняют layout в пределах согласованных изменений.
- Изменения DOM/cache version отражены в миграции.

### Фаза 7. Синхронизация

**Срок:** 6–10 недель для первого провайдера

**Приоритет:** P2

**Зависимости:** Фазы 0A, 1, 2 и 2B

#### MVP

- [ ] Переиспользовать интерфейсы `sync2`, не создавать параллельный sync framework.
- [ ] Первым провайдером реализовать WebDAV; Nextcloud поддерживать через WebDAV,
  если не требуется отдельное API.
- [ ] Синхронизировать только позицию чтения и закладки.
- [ ] Ввести версионированный schema format.
- [ ] Определять книгу по устойчивому `BookKey` (hash + нормализованные metadata),
  а не по локальному пути.
- [ ] Реализовать conflict policy, idempotency, offline queue и atomic writes.
- [ ] Использовать TLS platform trust, OAuth/token storage через credential store,
  redacted logging и явный logout/revoke.
- [ ] Заменить CRC32 в remote identity/content verification на современный digest.
- [ ] Ограничить размер и lifetime буферов: `Synchronizer` сейчас в нескольких
  путях собирает данные в `ByteArrayOutputStream`.
- [ ] Проверять remote payload теми же schema/parser budgets, что и локальный.
- [ ] Не логировать токены и содержимое пользовательских заметок.

#### Следующие провайдеры

- [ ] Google Drive рассматривать отдельным milestone после проверки лицензии,
  OAuth-модели и варианта без обязательных проприетарных библиотек.
- [ ] Синхронизацию тела книги добавлять только после оценки квот, шифрования,
  авторских прав и стоимости трафика.

#### Критерии приёмки

- Два устройства корректно разрешают изменения позиции и закладок.
- Повтор запроса не создаёт дубликаты.
- Повреждённый/частичный upload не уничтожает последнюю корректную версию.
- Экспорт и удаление синхронизированных данных доступны пользователю.
- Logout удаляет локальные токены и queued operations, но не пользовательские
  закладки без отдельного подтверждения.

### Фаза 8. Beta, выпуск и сопровождение

**Срок:** 3–5 недель

**Приоритет:** P1

**Зависимости:** выбранные продуктовые фазы

- [ ] Ввести feature flags для SAF, нового UI, E-Ink adapters и sync.
- [ ] Провести staged beta на реальном наборе устройств.
- [ ] Подготовить rollback/migration strategy для настроек, истории и DOM cache.
- [ ] Генерировать SBOM для каждого artifact и проверять лицензии всех встроенных
  компонентов, не только новых.
- [ ] Генерировать dependency manifest из `thirdparty_repo` и Android build:
  version, source URL, SHA-512, patches, license и фактически скомпилированный
  commit/source directory.
- [ ] Перевести доступные third-party URL на HTTPS и проверять checksum до
  распаковки; добавить CI dry-run deploy.
- [ ] Устранить расхождение между deployed `thirdparty/` и vendored Android
  sources либо генерировать оба дерева одним процессом.
- [ ] Публиковать stripped APK/AAB и отдельные native debug symbols/mapping;
  проверить, что release artifact не содержит лишние логи и debug assets.
- [ ] Добавить dependency/CVE update cadence и owner для antiword/chmlib и других
  unmanaged libraries.
- [ ] Переписать README вокруг support matrix и трёх проверенных build commands;
  obsolete инструкции вынести в `docs/legacy/`.
- [ ] Расширить CONTRIBUTING: environment bootstrap, tests, corpus policy,
  security disclosure, ADR, changelog и release checklist.
- [ ] Ввести актуальный `CHANGELOG.md` и автоматическую проверку versionCode /
  versionName / release tag.
- [ ] Перед release обновить `FORK_DELTA.md`, зафиксировать upstream base commit и
  проверить, какие temporary deltas уже можно удалить.
- [ ] Публиковать fork source tag/commit вместе с бинарными artifacts и сохранять
  notices/исходники в соответствии с GPL и лицензиями компонентов.
- [ ] Публиковать release notes с известными ограничениями.

#### Release gate

- [ ] Security gate Фазы 0A закрыт.
- [ ] Все schema migration и SAF process-death tests зелёные.
- [ ] Критические parser harnesses прошли длительный sanitizer/fuzz run.
- [ ] Есть smoke results для каждого заявленного Tier 1 target и подписанный
  перечень неподдерживаемых targets.
- [ ] Upstream base commit и полный fork delta известны; нет временного патча без
  owner/removal condition.
- [ ] Rollback был фактически отрепетирован на копии пользовательских данных.

---

## 6. Вехи и ориентировочный календарь

| Веха | Состав | Ориентир |
| --- | --- | --- |
| M-1 — Security Containment | Stop-ship часть Фазы 0A | Недели 1–2 |
| M0 — Green & Measured Baseline | Фазы 0–1 | Недели 1–5 |
| M1 — Storage & Data Safety | Фазы 2, 2B и 3 | Недели 5–12 |
| M2 — Android Platform Beta | Основная часть Фазы 4 | Недели 12–22 |
| M3 — Engine Hardening | Фазы 5 и 5B, итеративно | Недели 12–30 |
| M4 — EPUB/Sync Extensions | Выбранные части Фаз 6–7 | Недели 23–40 |
| M5 — Stable Release | Фаза 8 | После выполнения release gate |

Срок M4 зависит от выбранного объёма. Flexbox, Google Drive и синхронизация файлов
книг не должны автоматически входить в один релиз.

Фаза 0A не добавляется «после» продуктовой разработки: containment начинается
сразу, а полный security/privacy gate закрывается до beta. Фазы 2B и 5B можно
вести параллельно только при наличии разных владельцев; один разработчик выполняет
их последовательно.

---

## 7. KPI и release gates

### Безопасность и приватность

- Нет trust-all TLS, allow-all hostname verifier, global network authenticator и
  cleartext credentials.
- Password/token отсутствуют в SQLite, обычных preferences, backup, логах и crash
  attachments.
- Network redirect и auth origin покрыты отрицательными тестами.
- Parser/decompression budgets применяются ко всем заявленным внешним форматам.
- На момент релиза нет открытых P0/P1 findings с доказанным exploit/crash/OOM path.

### Сборка

- 100% обязательных CI jobs проходят на чистом checkout.
- Android и desktop artifacts воспроизводимо собираются задокументированными
  командами.
- Artifact имеет SBOM, dependency manifest, license notices и отдельные debug
  symbols.

### Совместимость

- Corpus открывается без crash и необъяснённых layout regressions.
- Книги из SAF повторно открываются после перезапуска на Android API 30/34/35.
- История, позиции и закладки сохраняются после обновления приложения.
- Fixtures всех поддерживаемых schema/settings/cache версий проходят migration и
  recovery tests.

### Производительность

- Время первой верстки и peak memory сравниваются с baseline на фиксированных
  устройствах/машинах.
- По умолчанию допускается не более 5% регрессии времени и 10% памяти.
- Цели ускорения задаются только после профилирования; произвольный KPI `+15–20%`
  не используется.

### Стабильность

- Нет новых ASan/UBSan/TSan ошибок в поддерживаемых тестах.
- Android stress-сценарий проходит 1 000 итераций.
- Нет утечек file descriptor и бесконтрольного роста cache.
- Fuzz smoke проходит в каждом PR; длительный run перед release не оставляет
  воспроизводимых crash/OOM/hang.
- Activity recreation, process death и service rebind проходят без leaked
  receiver/timer/thread/native handle.

### Качество выпуска

- Каждая фаза имеет owner, issue list, ADR для архитектурных решений и changelog.
- Новые зависимости проходят проверку лицензии.
- Для миграций данных существует rollback или безопасная инвалидизация.
- Каждый заявленный target имеет Tier, owner, последнюю проверенную конфигурацию и
  дату smoke test.
- Каждый fork-only/temporary commit отражён в `FORK_DELTA.md`; scheduled
  upstream-sync dry run зелёный либо имеет заведённые conflict issues.

---

## 8. Основные риски

| Риск | Вероятность / влияние | Митигация |
| --- | --- | --- |
| Компрометация OPDS/LitRes credentials | Высокая / критическая | Немедленно убрать TLS bypass и HTTP auth, credential migration, release gate |
| Потеря данных при schema migration | Средняя / критическая | Version fixtures, транзакции, integrity check, atomic backup и rollback rehearsal |
| ZIP/XML/image bomb или parser crash | Высокая / высокая | Общий parse budget, fuzzing, ASan/UBSan, минимизированные fixtures |
| Истёкший `content://` grant ломает историю | Высокая / высокая | Persisted permission, import fallback, typed document source |
| Несогласованные версии third-party | Средняя / высокая | Один generated manifest/source of truth, SBOM и reproducible deploy |
| Форк расходится с upstream и перестаёт обновляться | Высокая / высокая | Три категории delta, scheduled sync dry run, upstream-first bugfixes, запрет шумовых rewrites |
| Утечки receiver/service/native handle | Средняя / высокая | Lifecycle ownership, recreation/process-death tests, отказ от `finalize()` |
| Потеря производительности при замене COW-строк | Высокая / высокая | Benchmark до изменения, adapters вместо глобальной замены |
| Рост DOM из-за smart pointers | Высокая / высокая | Сохранить pool ownership и компактные handles |
| Deadlock после усложнения блокировок | Средняя / высокая | Сначала исправить `LVMutex`, документировать lock order, stress/TSan |
| Non-seekable SAF provider | Высокая / средняя | Cache fallback, лимит места, отмена и очистка |
| Несовместимая лицензия Onyx/Google SDK | Средняя / высокая | License gate и optional adapters |
| UI rewrite блокирует функциональные исправления | Высокая / высокая | Раздельные ветки поставки и feature flags |
| Flex/Grid ломают пагинацию и cache | Высокая / высокая | Отдельный RFC, fixtures, vertical slices и cache versioning |
| Конфликты синхронизации теряют данные | Средняя / высокая | Versioned schema, atomic writes, idempotency и conflict tests |
| CI пытается поддерживать все legacy targets | Высокая / средняя | Tier model, owners и архивирование без ложных гарантий |

---

## 9. Ближайший исполнимый backlog

### Первые 72 часа

1. Удалить trust-all TLS/global authenticator из OPDS и добавить отрицательные TLS
   tests.
2. Отключить LitRes authenticated HTTP operations; подготовить уведомление и
   решение о повторном вводе/миграции пароля.
3. Запретить cleartext по умолчанию, закрыть XML external entities и ввести
   минимальный download byte/redirect budget.
4. Зафиксировать security advisory/release decision, если уязвимая версия уже
   распространялась.

### Первая–вторая недели

5. После создания GitHub fork настроить `origin`/`upstream`, protection rules,
   `FORK_DELTA.md` и категории PR.
6. Исправить CMake C++ standard и добиться чистой сборки `fb2props`/`crengine`.
7. Добавить Linux C++ CI, Android debug/lint CI и ASan/UBSan smoke.
8. Исправить и протестировать pthread `LVMutex`.
9. Создать минимальный golden + malformed corpus и первые ZIP/XML fuzz harnesses.
10. Добавить migration fixtures для MainDB 23/28/32/34 и прекратить downgrade
   future-version DB до 26.
11. Исправить target-35 runtime blockers: media-playback FGS permission,
    `PendingIntent` mutability/implicit intents и notification/service smoke tests.
12. Спроектировать `DocumentSource`, `BookKey` и credential reference до
    изменения schema.

### Следующий инкремент

13. Реализовать `LVOpenFileDescriptorStream` и seekable SAF path.
14. Добавить non-seekable cache fallback, persisted URI permission и URI-backed
    историю.
15. Ввести archive/parser budgets и atomic cache validation.
16. Привести manifest к least privilege и закрыть lifecycle TTS/receiver.
17. Зафиксировать platform support tiers, dependency manifest и актуальные build
    инструкции.
18. После выполнения пунктов 1–17 пересчитать сроки UI, engine modernization,
    EPUB/CSS и sync по фактической скорости команды.

## 10. Definition of Ready для крупных инициатив

### Новый Android UI

- Security gate закрыт.
- Storage/data contracts стабилизированы.
- Есть lifecycle instrumentation tests.
- Выбран toolkit ADR и первый вертикальный экран без изменения schema.

### Изменение DOM/layout/CSS

- Есть golden corpus, layout diff и benchmark.
- Описаны cache version/invalidation и допустимые изменения позиций.
- Определён небольшой набор свойств и unsupported cases.

### Новый sync provider

- Готовы `BookKey`, schema, conflict policy и credential store.
- Локальная export/import модель работает без сети.
- Provider имеет logout/revoke, retry budget и end-to-end tests на двух клиентах.

### Возврат legacy/E-Ink интеграции

- Есть совместимая лицензия, owner и минимум одно доступное тестовое устройство.
- Интеграция изолирована adapter/flavor и не снижает security/SDK baseline основной
  сборки.
