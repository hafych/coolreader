# CoolReader Fork — мастер-план оставшихся работ

Актуально на 29 июля 2026 года. Источник истины — текущий код форка
`hafych/coolreader`, а не исторические планы upstream.

## Как вести этот файл

- Здесь находятся только подтверждённые незавершённые задачи.
- `[ ]` — задача ещё не начата.
- `[-]` — задача выполняется сейчас.
- После реализации задача временно помечается `~~зачёркнутой~~`.
- После прохождения относящихся к ней проверок зачёркнутая задача удаляется:
  завершённые работы не накапливаются в этом файле.
- Новые задачи добавляются только после проверки по коду, сборке или воспроизводимому
  дефекту.

## Граница работ этого агента

Этот агент выполняет только обычную разработку приложения, локальную сборку и
функциональные тесты на данных репозитория. Security-задачи, которые могут быть
похожи на offensive security, собраны в отдельном треке ниже и этому агенту не
поручаются.

Не входят в работу этого агента и не разрешены: сканирование внешних целей,
пентест чужих систем, создание или применение эксплойтов, обход авторизации,
DRM или ограничений доступа, подбор/получение секретов и отправка вредоносных
данных во внешние сервисы.

## Отдельный security-трек — передать другой нейронке

Все проверки этого трека должны выполняться только локально, на синтетических
данных без приватных книг, учётных данных и внешних целей.

- [ ] Добавить fuzz-targets для XML/HTML, EPUB/ZIP, FB2 и декодеров изображений,
  seed corpus и regression corpus.
- [ ] Добавить негативные тесты повреждённых документов, архивных лимитов,
  traversal/duplicate entries, чрезмерной вложенности и размеров.
- [ ] Добавить проверку известных уязвимостей зависимостей и правила обновления
  исключений.
- [ ] Добавить негативные SAF/provider-тесты для отозванных разрешений, циклов,
  symlink loops и недоступных или изменяющихся URI.
- [ ] До включения sync отдельно проверить threat model, шифрование,
  аутентификацию и key management.

## P0 — блокеры публичного релиза и Play Market

### Идентичность, подпись и публикация

- [ ] Выбрать постоянные имя приложения, `applicationId` и бренд форка. Это решение
  нельзя откладывать после первой публикации в Play.
- [ ] Создать и безопасно сохранить release keystore вне репозитория; описать
  восстановление, ротацию и доступ владельца.
- [ ] После создания постоянного ключа собрать production
  `:app:bundleSignedRelease`, проверить сертификат и установку/обновление
  release-сборки через APK set.
- [ ] После утверждения идентичности и контактов перенести подготовленный
  `docs/PLAY_LISTING.md` в Play Console и добавить проверенные иконку, feature
  graphic и скриншоты.
- [ ] Заполнить поля владельца в `docs/PRIVACY_POLICY.md`, опубликовать политику,
  добавить ссылку в приложение и заполнить Data safety по подготовленному
  `docs/DATA_SAFETY.md` для точного release AAB.
- [ ] После загрузки release AAB выполнить `docs/PLAY_RELEASE.md`: internal и
  closed test, разобрать pre-launch report и применить допустимый для данного
  релиза rollout с критериями остановки.
- [ ] Получить отдельное юридическое подтверждение имени/товарных знаков и
  закрыть пробелы происхождения и прав на иконки, шрифты и прочие ассеты из
  `docs/IDENTITY_AND_ASSETS.md`.

### Данные, SAF и Android permissions

- ~~После завершения SAF-перехода удалить legacy storage permissions,
  `requestLegacyExternalStorage` и прямое сканирование недоступных каталогов.~~

## P1 — воспроизводимость, качество и сопровождение форка

### Матрица сборок и CI

- [-] Clang job блокирует high-confidence diagnostics без frozen baseline.
  Все first-party предупреждения исправлены (0 при `-Wall -Wextra -Wpedantic`).
  Legacy GUI warning cleanup/current command mapping, document callbacks/battery,
  controls/property/font-menu/fullscreen key-label helpers, font property keys,
  CSS/image/document/history/settings/help/dictionary paths,
  dialog/menu/about/search/number-input/TOC/recent-book/bookmark/citation
  selection/search-result navigation text, dictionary/T9 word selection,
  on-screen keyboard layouts и T9 layout storage приведены к текущим
  `lString32`/UTF-32 контрактам.
  Неиспользуемые UTF-16 overloads number/TOC dialogs удалены после
  миграции всех внутренних consumers.
  Link-selection invalidation переведён с удалённого range geometry API
  на текущую геометрию экрана без device-specific размеров.
  Logo converter использует UTF-32 image/output paths, RAII stream и
  проверку полного write/flush вместо ручного `FILE *`.
  Shared GUI startup/font discovery и log properties переведены на
  `lString32Collection`/UTF-32; все platform call sites синхронизированы,
  а неиспользуемый unchecked file-read helper удалён.
  Jinke, NanoX, PocketBook, XCB и legacy Qt translation units проверены
  реальными SDK/header contracts; legacy Qt route переведён на Qt5 и добавлен
  в Linux Clang job вместе с `tinydict`. Modern Qt5/Qt6 link matrix использует
  imported targets на всех hosts без конфликтующего macOS fontconfig override.
  Границы массивов настроек modern Qt используют совместимые с Qt знаковые
  индексы; неиспользуемые параметры callback/event/slot реализаций удалены.
  Qt6 CI-сборка теперь также проходит общий Clang warning gate.
  Гейт расширен: `return-type`, `implicit-function-declaration`,
  `incompatible-pointer-types`, `uninitialized`, `delete-non-virtual-dtor`,
  `switch`, `non-c-typedef-for-linkage`, `misleading-indentation`,
  `reorder-ctor`, `sign-compare`, `unused-but-set-variable`,
  `unused-parameter`, `unused-private-field`.
  Осталось: подтвердить первый зелёный CI-прогон.
- [-] Проверить первый полный CI-прогон после закрепления Linux/Android jobs на
  Ubuntu 24.04 и macOS job на macOS 15; после зелёного прогона убрать этот пункт.

### Release engineering

- [-] Локальные double-clean Android rebuild и повторная desktop-упаковка
  воспроизводимы; double-runner gate и допустимая RSA-PSS разница APK
  документированы. После первого полного прогона release workflow подтвердить
  Linux/macOS rebuild на чистых runner и удалить пункт.

## P2 — развитие продукта после безопасного релиза

### Архитектура Android

- [-] Разделить монолитные `CoolReader`, `ReaderView` и сервисы на тестируемые
  lifecycle-aware компоненты без статического состояния Activity/Service.
  Выполнено: Activity-owned service graph и immutable dependency snapshot,
  generation-scoped callback cancellation, application-context service
  connectors, явные зависимости `CoolReader`/`ReaderView`/диалогов и bounded
  widened page-flip lookup geometry с отдельным JVM regression, method-scoped
  heap diagnostics, удаление мёртвого process-wide date formatter и atomic
  single-drain handoff process-dispatcher handlers с сохранением delay, а также
  immutable Engine process snapshot, generation-owned path corrector и
  synchronized/frozen HyphDict registry без backing-array escape и
  immutable/testable owner lookup-таблиц page-curl, плюс one-shot
  interrupt-preserving blocking result без зависимости dispatcher от UI и
  generation-scoped OptionsDialog resource/format state без shared title arrays,
  Activity-owned backlight timestamp/timer и pure timeout policy, а также
  thread-safe strict OPDS timestamp parser без shared formatter state и
  generation-owned immutable Nook EPD reflection bindings без mutable
  process-wide vendor cache, плюс ReaderView-owned bitmap pool/VMRuntime
  tracker с immutable reflection bindings, synchronized long accounting и
  widened row-stride/surface byte sizes, а также immutable ReaderAction
  metadata/catalog copies, Activity-owned themed icon snapshots и
  SettingsManager-owned device input defaults с корректным Nook override без
  перезаписи user mappings, ReaderView-owned immutable/overflow-safe gesture
  acceleration curve и единый immutable typed OptionsDialog style catalog
  вместо параллельных process-wide массивов, плюс immutable external-dictionary
  metadata и snapshot-only array API без process-wide catalog escape, а также
  final BaseActivity preference/debug constants и generation-owned system
  locale resolution, плюс ReaderView-owned animation timing с fractional scroll
  steps, validated rolling samples и widened autoscroll duration arithmetic, а
  также ReaderView-owned saturating reading-time tracker без getter mutation и
  double-count на повторных lifecycle signals, плюс locale-aware long reading
  time formatter без int narrowing для multi-day persisted durations.
  Также audio sibling lookup и filename transliteration вынесены в private
  immutable owners без process-wide array escape и зависимости от OPDS types.
  Interface theme definitions теперь immutable values в generation-owned
  catalog `BaseActivity`, с явным E-Ink snapshot и без public static array.
  Profile load/save filtering также вынесен из `SettingsManager` в единый
  immutable matcher без public rule-array и с JVM regression на wildcard rules.
  Синхронная запись настроек делегирована отдельному stateless
  `SettingsFileStore`: snapshot валидируется до открытия файла, output всегда
  закрывается, round-trip/truncation покрыты JVM-тестами, а мёртвый
  `saveSettingsTask` и его закомментированный callback удалены.
  Built-in background textures вынесены из `Engine` в immutable catalog;
  immutable metadata и `none → external → built-in` List snapshot JVM-tested.
  `DocumentFormat` extension/MIME metadata теперь clone-on-boundary, private
  final и используется через primary-extension API без backing-array escape.
  Tap action routing и highlight bounds `ReaderView` теперь используют одну
  pure JVM-tested 3×3 geometry с совпадающими границами для некратных размеров,
  clamped coordinates, widened arithmetic и safe empty invalid-surface bounds.
  Tap highlight visibility/bounds также переведены с non-atomic numeric
  generation и shared mutable `Rect` на synchronized identity-owned
  `TapHighlightState`: replacement redraws union старых/новых immutable bounds,
  delayed unhighlight принадлежит cancelable reader scheduler и действует
  только на latest request, page/reload/close invalidation и destroy исключают
  stale callbacks.
  Requested viewport и delayed native resize объединены в
  `ViewportResizeState`: immutable volatile size snapshot заменяет parallel
  width/height, exact latest request контролирует background apply и GUI done,
  invalid dimensions получают positive fallback, owned scheduler заменяет
  pending delay, а destroy закрывает state до native teardown.
  Surface created/visible/focused/closed объединены в synchronized
  `ReaderSurfaceState` вместо отдельного boolean: оба порядка visible/create
  дают один ready refresh, delayed E-Ink focus refresh принадлежит exact token
  и reader scheduler, focus loss/hide/surface destroy/replacement инвалидируют
  его, а draw и callback требуют тот же open surface/service generation.
  Delayed current-position save переведён с numeric generation/global Handler
  на exact `CloseableTaskGate` token и owned GUI scheduler: replacement,
  sync/pause/reload отменяют pending callback, destroy закрывает gate, а
  one-shot apply проверяет captured `BookInfo` identity и не может записать
  bookmark старой книги в текущую.
  Selection preview/end также используют один exact `CloseableTaskGate` token:
  каждый drag sample заменяет owner, stale terminal callback не открывает
  toolbar и не очищает selection нового жеста, clear/reload/close отменяют
  current update, а destroy permanently закрывает gate до native teardown.
  Временное отключение E-Ink full refresh вынесено в synchronized
  ReaderView-owned lease tracker: overlapping clients восстанавливают исходный
  interval только после последнего matching release, а duplicate/unmatched
  transitions и отрицательные vendor intervals обрабатываются без sentinel.
  Battery receiver/renderer boundary переведён с трёх параллельных mutable
  полей на immutable `BatteryStatus`: provider level нормализуется по scale
  widened arithmetic, Activity хранит один initial snapshot, а `ReaderView`
  публикует один volatile snapshot без mixed-generation native update.
  Основной load/format progress `ReaderView` также вынесен в synchronized
  snapshot owner: initial state явно hidden, zero-position остаётся active,
  duplicate show и hide идемпотентны, а renderer получает согласованные
  position/resource/title одного поколения.
  Horizontal `ProgressDialog` больше не создаёт пустой implicit-Looper Handler:
  number/percent views связаны явно и синхронно получают JVM-tested,
  locale-aware `ProgressDisplayState` с clamped progress и safe zero max.
  Engine show/hide/delayed progress переведён с non-atomic volatile int и
  parallel booleans на synchronized identity-owned `ProgressUiState`: latest
  request wins, delayed hide действует только на свой visible/pending token,
  cancel/show race закрыт, а detach permanently invalidates work и dismisses
  диалог через GUI owner.
  Общий `DelayedExecutor` переведён с nullable callback check на pure one-shot
  `ReplaceableTaskSlot`: replacement/cancel инвалидируют точный wrapper,
  successful claim очищает slot до delegate, stale generations и повторный
  запуск не проходят, reentrant reschedule сохраняет нового владельца.
  Animation/GC delayed executors теперь private final у одного `ReaderView`;
  animation handoff использует instance lock и volatile active reference вместо
  process-wide class monitor, а `destroy()` безусловно отменяет оба executor и
  очищает pending animation state до native teardown; неиспользуемый volatile
  animation serial без readers/writers удалён.
  Coalesced `DrawPageTask` теперь использует exact closeable token вместо
  numeric generation: только current render завершает GC lifecycle, replacement
  не поглощает независимый command completion, reentrant draw отменяет GC
  predecessor, а destroy закрывает render/callback owner до native teardown.
  Autoscroll теперь принадлежит synchronized identity-owned
  `AutoScrollSessionState` и отдельному cancelable GUI scheduler: background
  init публикует render-ready только точному owner, stop/destroy не допускают
  resurrection, re-init временно скрывает partial state, speed публикуется
  volatile, а close не запускает page cleanup для ещё не initialized session.
  Каждая animation дополнительно хранит immutable captured book+interaction:
  init, timer, image preparation, native page turn, render и stop completion
  повторно проверяют обе identity; stale session снимается без page cleanup,
  а redraw/position-save после stop возвращаются на GUI с той же exact-парой.
  Отложенный native swap-to-cache также переведён с volatile task pointer на
  exact `CloseableTaskGate` token и отдельный cancelable GUI scheduler:
  reload/close инвалидируют retry с возможностью следующей книги, terminal
  completion очищает только своего owner, а destroy закрывает gate до native
  teardown и не допускает swap уже уничтоженного `DocView`.
  Font-face navigation вынесена в stateless `FontFaceSwitcher`: empty native
  catalog даёт no-op, missing current начинает с directional edge, известные
  значения корректно wrap, magnitude направления нормализуется без overflow.
  `PositionProperties` теперь widened/clamped для scrollable range и 0–10000
  percent; scroll movement использует тот же range, а go-to-percent переиспользует
  единый контракт без int overflow и division by zero. Stateless
  `DocumentPositionPolicy` централизует one-based display page, percent text и
  0–100 → valid page mapping с точной 100% last-page boundary.
  Асинхронный startup TTS теперь использует begin-if-idle closeable token:
  повторный PLAY не дублирует bind/init, STOP инвалидирует exact request,
  destroy permanently закрывает startup, success/failure завершают только свой
  owner, а toolbar close очищает лишь собственную identity. `CoolReader`
  публикует captured accessor/engine только активному service generation;
  TTS connector держит registration/binder/pending callbacks под одним lock,
  сообщает bind failure и очищает очередь при unbind.
  Долгая инициализация audiobook word timings и периодический position poll
  `TTSToolbarDlg` теперь принадлежат closeable generation gate: повторная
  инициализация инвалидирует старую публикацию, закрытие очищает main-handler
  и завершает owned `HandlerThread`, а stale service/background callbacks не
  меняют selection или UI закрытого диалога.
  Motion watchdog того же диалога теперь действительно выполняется на Looper
  своего `HandlerThread`, повторный PLAYING заменяет старого owner, а
  pause/stop/close идемпотентно отписывает sensor, очищает messages,
  восстанавливает volume и завершает thread без UI `sleep`; fade state
  clamped и JVM-tested до точного нуля без underflow.
  Sentence timing cache вынесен из audiobook matcher в stateless immutable
  codec со strict finite/non-negative parsing и scoped IO; matcher принимает
  только complete one-to-one snapshot известных sentence positions и
  публикует его целиком после валидации, а raw timing reader и
  `MediaMetadataRetriever` закрываются на всех путях.
  Повторяемые touch-actions также используют exact one-shot generation:
  `UP`/`CANCEL`, новое нажатие и detach View отменяют pending wrapper, снимают
  pressed-state и не позволяют callback старого жеста действовать на новый.
  Key single/double-click decision вынесен из четырёх parallel полей в
  synchronized `KeyDoubleClickState`: immutable pending identity не позволяет
  stale timer очистить replacement, matching second press потребляет double,
  остальные/expired/regressed события flush только prior single с
  overflow-safe elapsed. Touch long/double timeouts используют отдельный
  closeable gate и reader scheduler; replacement/drag/`ACTION_CANCEL`/focus
  loss/book close/destroy отменяют exact owner, stale selection completion
  проверяет handler identity и active service generation, а delayed
  link/image/bookmark completion дополнительно проверяет captured book identity.
  Открытие документа теперь принадлежит одному Activity-owned
  `DocumentLoadLifecycle` от первого public request до `ReaderView`: ожидание
  DB service, SAF metadata/probe, non-seekable cache, descriptor transfer,
  history lookup, background/GUI handoff, native load и memory-stream
  reconciliation используют один exact token. Новый выбор, close и destroy
  сразу инвалидируют всю старую цепочку; переход в browser/root отменяет
  pending open, но не обрывает reconciliation уже опубликованной книги. Stale
  SAF result не открывает Reader и закрывает ещё не переданный descriptor.
  `LoadDocumentTask` хранит свой `BookInfo` вместо чтения mutable global book
  во время engine work; только current generation публикует UI, failure
  recovery и позднее stream-to-cache/fingerprint reconciliation. Закрытие
  старой книги сохраняет её позицию до публикации metadata новой, а native
  engine boundary закрывает документ, оставшийся от уже заменённой parse-задачи.
  Операции уже выбранного документа используют отдельную exact
  `DocumentLoadLifecycle.Interaction`, связанную с captured `BookInfo`:
  replacement, close, destroy и уход в browser/root сразу меняют generation.
  History/bookmark/engine navigation, scroll/go-to, запросы и публикация
  position status, TOC и go-to dialog callbacks, а также gesture/programmatic
  page-flip обязаны повторно подтвердить обе identity до native mutation и UI
  completion. TOC-диалог хранит узкий page callback вместо `ReaderView`;
  animation scheduler адресует конкретный animation instance, а teardown
  очищает pending update до закрытия native document. Поэтому старый callback,
  dialog или page-flip не может переместить, отрисовать или сохранить позицию
  книги, которая его заменила.
  Selection/search chain также сохраняет captured book+interaction от native
  gesture update до toolbar, search-history callback, двухпроходного
  forward/backward find, find-next popup, clear и bookmark highlight. Native
  work и GUI completion повторно проверяют обе identity; stale toolbar
  dismiss/click не очищает selection и не меняет view mode новой книги.
  `SearchDlg`, `FindNextDlg` и dictionary picker больше не хранят
  `ReaderView`, получая только узкие generation-aware callbacks.
  Bookmark list/editor chain теперь так же сохраняет captured
  book+interaction: list, edit, shortcut и selection entry points получают
  только узкий `BookmarkInteractionHandler`, а add/update/delete/go-to и
  асинхронное получение bookmark текущей страницы повторно проверяют обе
  identity перед mutation, DB effect и highlight. Отложенное сохранение
  позиции захватывает эту пару до GUI-hop и переносит её до exact apply, поэтому
  stale диалог или timer не может изменить либо сохранить replacement-книгу.
  Остальные обязанности монолитов выносятся отдельными bounded-пакетами.

### Библиотека и сканирование

- [x] Сделать сканер инкрементальным и отменяемым, с bounded parallelism и
  backpressure.
- [x] Отделить discovery, metadata extraction, persistence и UI progress.
- [x] Добавить fingerprint каталога/документа и не перечитывать неизменённые книги.
- [x] Обрабатывать огромные каталоги с bounded limits и понятным прогрессом.
- [x] Добавить corpus-тесты на десятки тысяч книг и измерять время/память.

### Нативное ядро и рендеринг

- [-] Зафиксировать политику владения нативными объектами и постепенно заменить
  опасные raw pointers/ручные пары acquire-release на RAII.
  Выполнено: ownership для hyphenation registry/method и TeX pattern
  bucket/chain graphs, text-language/font-manager; временные буферы INI,
  cache-file, SVG/GIF/XPM/image rows, bounded XPM header/palette/raster
  parsing и bounded GIF record/sub-block/LZW
  bitstream/table/output parsing, palette validation и interlaced-frame row
  mapping, normalized/clamped alpha-transform arithmetic,
  SVG/PNG/JPEG/GIF callback cancellation и exception-safe C-library teardown,
  alpha/stretch-transform callback borrow/lifecycle и stretch row
  storage/downscaling, dummy/draw-buffer cancellation и null-buffer factory,
  encoding autodetect/offline
  statistic input и parser format detection; parser selection, draw-buffer
  source и owned/borrowed
  color/gray pixel backing, ZIP decoder/entry
  factories and bounded stream fragments, cached-stream slots, transactional
  legacy C bitmap-buffer publication, parser
  read/charset buffers, debug compare-stream/tinyDOM fixture scratch, RTF
  text/destination
  ownership, TCR dictionary/index/decoded buffers, dictzip chunk-size/offset
  catalogs и scoped compressed/reusable unpacked/article buffers с корректным
  multi-chunk read, scoped dictionary FILE handles, value-owned names/words и
  explicit word/result/dictionary owner graphs с transactional reopen,
  unpacked 8/16/32-bit pixel storage, checked byte-count/format contracts и
  downstream cancellation,
  scoped stream-image decoder factory candidates, scaled-image
  overflow-safe maps/clipping, bounded source/destination RGBA workspaces
  and shared scaler allocation with mapped fallback,
  complete smooth-output/nine-patch lifecycle,
  guarded color/gray image draw entry
  points with success-only accounting and
  saturating 64-bit render statistics,
  nine-patch metadata cache and color-transform
  workspaces/cancellation/64-bit statistics, WOL TOC/image/LZSS buffers and
  reader result ownership, default stream-region buffers,
  owned/borrowed memory streams, block write-cache buffers/LRU, mapped-file
  regions/OS handles, file-stream FILE/HANDLE/owned/borrowed descriptor
  lifecycle, directory scan handles/item candidates, archive factory/item
  candidates, serialization-buffer storage и string-collection slots/hashed
  buckets, transactional COW string buffer allocation/growth, bounded custom
  string-chunk и DOM block-pool slice/size-class owners, DOM name/id
  owner/index storage, generic hash-table buckets/chains,
  generic value-array, transactional reference adoption/clone, reference-vector,
  transactional owning/borrowed pointer-vector adoption/storage and
  contiguous matrix-cell storage,
  pagination compact arrays/line-link lists, reference-owned formatted-text
  factory configuration, scoped live page/virtual-line candidates and bounded
  transactional page/page-list snapshots,
  Word/PDB transient import buffers/factory candidates, cache-file ZSTD/zlib
  contexts/chunk output/block scratch, scoped live-block candidates and
  bounded transactional index snapshots/publication, DOM blob
  payload/item/index storage,
  DOM text-storage chunk resident buffers/cache transitions, transactional
  chunk catalogs/runtime candidate adoption and persistent node-part
  catalogs/cache loading, CSS declaration buffers,
  selector/rule chains, stylesheet snapshots, transactional style-record
  restore и bounded sparse style-index publication,
  reference-cache
  buckets/index exports and bounded map slots, transactional document-cache
  directory index and render-header restore, longjmp-safe PNG rows/pixels,
  JPEG pool/error lifecycle, draw mark list, history XML/file/bookmark parse
  candidates, transactional snapshot publication и synchronization-record
  bookmark ownership, global i18n translator slots и GUI-side translation
  candidates, bounded properties input/candidate snapshots, transactional
  serialized-property restore, clone/item publication и exact-write settings
  output, XML/HTML document factories,
  OPC relation-table
  owner/index publication, FB3 description/parser state и CHM
  container/file/metadata/HTML parse ownership, а также EPUB encrypted-container,
  font-key snapshot, manifest-item и OPF/nav/NCX/page-map DOM ownership и
  lifecycle основного документа `LVDocView`, ODT metadata DOM и style/list
  parse-candidates, skin DOM и factory/icon/button parse-candidates, generic
  queue nodes, thread-executor monitor/thread/task ownership и font-cache
  registered/instance entry ownership, global/local glyph bitmap-item
  candidate/owner transfer, transactional bounded embedded-font
  definition/list restore и
  transactional cache deserialization, а также plain-text line-queue item
  ownership и non-null parser borrow, process-wide logger ownership и
  synchronized lifecycle/dispatch, persistent DOM CacheFile ownership и
  owner-backed storage/blob borrows, encoding double-character sparse-row и
  output ownership, а также GUI window-manager screen owner/borrow lifecycle,
  transactional canvas-generation resize, modern Qt view
  PIMPL/document/word-selector, generated-UI helper owners и fallback-font
  row/control graphs, scoped export dialogs/callback restoration и transient
  context-menu/search-feedback widgets, guarded live search-dialog observer,
  close-time teardown и transactional publication для всех modeless dialogs,
  explicit history bookmark borrow, guarded document-view borrows и
  method-scoped file-properties engine borrow, replace-safe
  scrollbar/property observers, shared property-adapter
  factories без implementation downcasts,
  GUI window/event queue ownership и
  scoped accelerator/command-event/main-menu/recent/bookmark item
  publication candidates и lifecycle `CRDocViewWindow`, scoped page-image
  render candidates,
  bookmark mutation/highlight-range candidates и scoped nested-render mark
  copies, value-owned rectangle clipping, scoped settings menu/item
  candidates, а также
  transactional bookmark-list publication, render-flow float/shift publication
  и teardown, table-cell
  page-context ownership, draw-time bookmark-range filters и full CCRTable
  row-group/row/column/cell graph publication, включая MathML table expansion,
  persistent/enumerated FreeType face owners and load candidates,
  transactional bounded color-glyph scaling workspace/metric publication,
  scoped
  bitmap/Win32 manager load candidates, transactional legacy bitmap-font file
  candidates и alternate-backend interface compile gate, portable RAII Win32
  glyph-cache graph и language-compatibility cache owners, transactional
  HarfBuzz font replacement/buffer lifecycle, process-level FreeType library
  и FontConfig query/result graph, а также bounded NanoSVG
  image/rasterizer/RGBA workspace ownership и transactional
  selection-range split/draw/text owner publication, включая batched
  word/filter/crop/extended-word owner-list factories, а также bounded
  transactional TOC/page-map graph deserialization и whole-snapshot DOM
  name/attribute/value/ID-node map publication, плюс RAII legacy HTML
  autoclose-rule tables, lazy FreeType glyph-metric page ownership и shared
  XPointer state ownership, scoped CSS pseudo-element style owners и
  vector-backed mutable DOM attributes, transactional mutable/persistent DOM
  node payload transitions и reserved child publication, а также central RAII
  ownership для parser element-writer/foster graphs, DOM-backed base64 stream
  candidates и temporary DOM serialization hyphenation flags.
  Остальные участки мигрируются отдельными bounded-пакетами.
- [x] Устранить глобальное изменяемое состояние в путях parser/render/cache либо
  явно ограничить его синхронизацией и временем жизни процесса.
  Выполнено: атомарный счётчик cacheable object ID, `std::call_once` для
  MathML-стилей; mutex для interning-таблиц cs8/cs32; IS_FIRST_BODY перенесён
  в документ; custom chunk/block/ref-count pools явно оставлены
  single-threaded lifecycle-компонентами с поддержкой повторной инициализации;
  ~15 read-only глобалов переведены в const; Antiword bridge использует
  per-import context и сериализованные entry points. Все паттерны закреплены
  в `native_state_policy.cmake`; legacy crconcurrent guards имеют встроенный
  recursive-mutex fallback, RAII ownership и quiescent setup/shutdown.
- [x] Добавить differential/regression tests для pagination, bookmarks, selection,
  search и восстановления позиции.
- [x] Ввести ограниченный cache manager с наблюдаемыми hit/miss/eviction counters
  для glyph, image, cover и parsed-document caches.
- [x] Проверить корректность Unicode: surrogate pairs, combining marks, bidi,
  grapheme boundaries, CJK и mixed scripts.

### EPUB/CSS/типографика

- [x] Расширить EPUB3 nav/landmarks, metadata, footnotes и media-overlay policy с
  тестовым corpus.
- [x] Определить поддерживаемое CSS-подмножество, добавить тесты cascade,
  specificity, inheritance, units и page-break.
- [x] Обновить hyphenation dictionaries, проверить их лицензии и добавить
  языковые golden tests.
- [x] Добавить variable-font/font-fallback tests и контролируемый выбор системных и
  встроенных шрифтов.

### UI, доступность и E-Ink

- [ ] Провести accessibility-аудит: TalkBack, focus order, touch targets, contrast,
  dynamic font size и content descriptions.
- [ ] Ввести явные профили LCD/E-Ink с capability detection и безопасным fallback.
- [ ] Проверить ghosting/full-refresh policy, аппаратные кнопки и задержку
  перелистывания на поддерживаемых E-Ink устройствах.
- [ ] Унифицировать темы/цвета/типографику и убрать неявные зависимости от
  конкретной Activity/ориентации.

### Синхронизация

- [ ] До включения sync определить формат данных, конфликтную модель и
  совместимость версий.
- [ ] Реализовать idempotent operation log и детерминированное разрешение
  конфликтов для позиции, закладок и аннотаций.
- [ ] Добавить offline/retry/backoff, bounded queues, account removal и полное
  удаление локальных/серверных данных.
- [ ] Покрыть sync contract tests с несовместимыми версиями, повторами, reorder,
  частичными ответами и потерей сети.

## Порядок исполнения

1. Завершить и проверить текущий рабочий пакет.
2. Зафиксировать идентичность приложения, подпись и privacy/Data safety.
3. Закрыть SAF library roots; security-тесты ведутся отдельным треком.
4. Выпустить внутренний AAB и пройти Play pre-launch report.
5. Настроить release automation и защиту ветки.
6. После стабильного публичного релиза переходить к задачам P2.
