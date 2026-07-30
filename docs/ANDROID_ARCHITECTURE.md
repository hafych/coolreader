# Android architecture

This document records the ownership and lifecycle rules for the Android
application while the remaining `CoolReader` and `ReaderView` decomposition is
in progress.

## Activity service graph

`BaseActivity` owns one `Services` instance. Starting that instance creates an
immutable `ServiceDependencies` snapshot containing the Engine, Scanner,
History, cover manager, filesystem folders, genre collection, document cache
and one `ServiceLifecycle`.

Components receive either the snapshot or the specific dependency they need.
They must not resolve Activity-owned services through static fields or a global
service locator.

Stopping an Activity closes only its `ServiceLifecycle`, clears that instance's
graph and detaches its Engine. Queued work that captures the lifecycle must
discard callbacks after the lifecycle closes. A stale Activity therefore
cannot clear or stop a graph owned by a newer Activity.

Activity UI helpers follow the same rule. For example, each `BaseActivity` owns
its custom E-Ink toast queue, main-thread handler and popup window, then cancels
and dismisses them during `onDestroy`. Diagnostic heap snapshots are created
per invocation rather than being shared by every Activity generation, and
unused formatter objects are not retained at process scope. Preference names
and debug switches are immutable constants; the system-locale snapshot belongs
to one Activity generation and is resolved through the pure
`AppLocaleSelection` value instead of being captured at process scope.

## Process-scoped infrastructure

`BackgroundThread` remains a process-scoped dispatcher. Activity teardown does
not quit it because a newer Activity generation may already use the same
dispatcher. Activity-owned work must use `ServiceLifecycle` for cancellation
instead. Its singleton and handler references use explicit cross-thread
publication. GUI and background tasks submitted before their handler exists
pass through one atomic deferred-queue handoff that preserves delay and removes
delivered entries before a later Activity generation can attach.
Synchronous cross-thread calls use a one-shot `BlockingResult` owned by the
dispatcher layer, not a nested `ReaderView` type. It releases every waiter,
completes failure paths, and restores an interrupted waiter's flag after
delivery.
Replaceable delayed callbacks use a pure `ReplaceableTaskSlot`. Replacement or
cancellation invalidates the exact old wrapper, only the current generation can
claim execution, and a successful claim clears the slot before invoking user
code. Reentrant scheduling therefore cannot be cleared by its predecessor, and
an already-removed callback cannot run merely because a newer callback exists.
Reader TTS startup uses the gate's begin-if-idle mode: repeated play commands
share one pending initialization, stop cancels its exact token, and reader
destruction closes startup permanently. Success and failure complete only that
request; a stale success cannot open a toolbar, and each toolbar close callback
clears only its own identity. `CoolReader` captures the exact service accessor
and engine package in an Activity-owned `TtsInitializationSession`, then
delivers results on the GUI thread only after claiming that exact request while
its captured service generation is active. Replacement transfers the prior
failure callback as one-shot cancellation so the reader gate cannot remain
pending; stop, engine change and Activity teardown detach all terminal
callbacks. Binder-connect and engine-result listeners are static weak-Activity
adapters, so a service queue cannot retain destroyed UI. The
application-context TTS connector
serializes binder registration, binder publication and pending callbacks under
one lock, snapshots callbacks before delivery, reports bind failure and clears
queued work on unbind.
The service itself owns engine replacement through `TtsInitializationState`.
Every attempt has its own `TextToSpeech` candidate and daemon timeout; callback
and timeout completion are serialized onto the service queue and must claim the
same request. Replacement cancels and shuts down only the previous attempt, so
its late callback cannot cancel the current timeout or publish the old engine.
Service destruction closes the state before draining its queue, suppresses
later callback posts, and releases TTS resources only after queued work stops.
The toolbar itself receives an immutable `TtsDocumentSnapshot` and a narrow
`TtsDocumentHandler` for the captured book/interaction; it never retains
`ReaderView`. Selection movement, temporary view mode, cover publication,
audiobook sentence extraction and stop/save cleanup all revalidate that exact
handler. Native sentence extraction is serialized on the Engine queue.
Document replace, pending-open cancellation, close and Activity teardown stop
and clean up TTS before rotating the interaction, while late service callbacks
are gated by both toolbar and document ownership. They therefore cannot move,
clear, restore or save a replacement book. The foreground service receives the
captured book metadata at startup, and bind failure follows the same idempotent
close-completion path.
Long-lived dialog work uses a closeable generation gate when cancellation must
also be permanent after teardown. `TTSToolbarDlg` assigns audiobook timing
initialization and its periodic position poll to the current gate token.
Reinitialization invalidates older results; close removes owned handler
callbacks and quits the dialog's timing thread. Completion is published on the
GUI thread only while its exact token remains active, so stale work cannot
restore controls or move selection after dismissal.
The TTS motion watchdog is also owned by that dialog. Its `Handler` is bound to
the Looper of the `HandlerThread` created for it; timeout and volume-fade work
never sleeps or runs on the GUI Looper. Replacement, pause, stop and close use
one idempotent cleanup path that unregisters the sensor, removes messages,
restores the original volume and quits the thread. Its pure fade state clamps
malformed provider values and reaches exactly zero without underflow.
Audiobook sentence timing persistence is delegated to the stateless
`AudiobookTimingCache` codec. It closes readers and writers, returns immutable
entries, and rejects malformed, non-finite or negative timing data.
`WordTimingAudiobookMatcher` indexes a complete one-to-one cache snapshot
before replacing any `SentenceTiming`; unknown, duplicate or missing sentence
positions therefore cannot leave a partially restored book. Raw word-timing
input and `MediaMetadataRetriever` resources are also closed on every path.
Repeated touch actions use the same exact-wrapper principle. Each press owns a
`ReplaceableTaskSlot` callback and one pressed View. Release, cancellation, a
new press or View detachment invalidates the pending wrapper and clears the
pressed state. A callback already removed from the Handler queue therefore
cannot observe or act on a replacement gesture.
Reader input timeouts follow the same ownership rule. Key single/double-click
resolution lives in a synchronized `KeyDoubleClickState` instead of four
parallel fields. A scheduled single-click claims its exact immutable pending
decision, so an old timer cannot clear or execute a replacement; a matching
second press consumes the double action, while another, expired or
clock-regressed press flushes only the prior single action with overflow-safe
elapsed arithmetic. Hardware-key repeat and long-press state likewise belongs
to one synchronized `KeyRepeatState`, replacing the retained `KeyEvent`,
repeat action, in-flight flag and unbounded wall-clock timestamp map. Each
command completion releases only its exact press/repeat token; release,
cancellation, focus loss, book close and destruction invalidate it. Long-press
duration uses monotonic `KeyEvent` time with a bounded device down-time
tolerance, so clock jumps or a stale completion cannot trigger or unblock a
replacement press.
Temporary scroll mode for selection adjustment and TTS is owned by one
synchronized `ReaderViewModeState`. Each consumer receives an exact identity
lease; the first lease queues the transition to scroll and only the final
matching release queues restoration of the configured mode. Settings apply
captures the effective FIFO snapshot, native settings readback republishes the
configured value, and document replacement, close or reader destruction
invalidates outstanding leases. Duplicate dismiss and stale cleanup therefore
cannot restore another consumer's mode or persist a temporary override.
Non-animated page up/down in scroll mode uses the same serialized command
boundary. `ReaderView` captures the book and interaction before queueing a
single operation that reads native position and executes `GO_POS`; it no
longer calls `DocView.getPositionProps()` on the GUI thread. The pure
`ReaderScrollPageCommand` computes seven eighths of the viewport with widened
arithmetic and clamps both document boundaries. The shared Engine executor
revalidates the exact request before and after the operation and again before
render/save completion.
Android minute ticks use an independent latest-only `CloseableTaskGate` and
the captured `ReaderRenderRequest`. The broadcast callback only queues work;
`DocView.isTimeChanged()` runs on the Engine thread after native-lifecycle,
service, document and token checks. Pause, autoscroll, replacement and close
invalidate
the exact owner, while reader destruction closes it permanently. Completion
revalidates the same request and draws through its captured identity instead
of a generic redraw that could recapture a replacement book.
The full-screen image viewer also captures its exact book and interaction.
Mutable image geometry belongs to a synchronized `ReaderImageViewerState`;
gesture updates and Engine renders cross the thread boundary only as copied
`ImageInfo` snapshots, and replacement or finish can affect only the matching
session. Native image close remains serialized on the Engine queue, while a
stale render recycles its candidate instead of publishing it. Document
replacement closes the viewer before rotating the interaction, and reader
destruction restores the prior orientation, permanently closes the state, and
queues native image cleanup before `DocView` teardown.
Touch long-press and double-tap timeouts use a separate
closeable gate plus reader-owned scheduler. Gesture replacement, drag-mode
entry, `ACTION_CANCEL`, focus loss, book close and reader destruction cancel
the exact timeout, and late selection inspection verifies both its handler
identity and active service generation before publishing UI. Link/image/
bookmark inspection also captures book identity, so its delayed GUI result
cannot act on a replacement document.

Document opening is one Activity-owned, identity-owned generation from the
public `CoolReader` request through `ReaderView` engine work and any
memory-stream fingerprint/cache reconciliation. `CoolReader` creates the
`DocumentLoadLifecycle` and injects the same instance into its reader.
DB-service readiness, SAF metadata/probing, non-seekable provider caching,
file/buffer/descriptor handoff and history lookup all carry the same token
across background and GUI queues. A newer selection invalidates native work
immediately instead of waiting for SAF preprocessing to reach the reader.
Browser/root navigation cancels a still-pending open, but preserves the token
after the reader has atomically marked the document published so its exact
stream reconciliation can finish off-screen. When cancellation claims an
unpublished request, `ReaderView` queues a serialized native/cache close after
any parse already in flight, so that parse cannot leave an ownerless document
open. Reader close and Activity destruction reject every phase. An unaccepted
or replaced descriptor is closed by the layer that still owns it, including
cached descriptors produced after teardown.
Adding or reselecting a library root is owned by an atomic
`LibraryRootRequestState`. Pending identity is stored separately from the
nullable previous URI, so an add request survives Bundle restore, an
overlapping launch cannot replace a reselect target, an ownerless result is
ignored, and launch failure or Activity destruction clears only the exact
request.
`ACTION_OPEN_DOCUMENT` launched from a library root has an independent
`LibraryDocumentRequestState`. Its initial root survives Bundle restore,
overlapping pickers cannot replace the owner, cancel or launch failure clears
the request atomically, and only an owned result from an active generation may
hand its URI to the shared `DocumentLoadLifecycle`.
The shared `OPEN_DOCUMENT_TREE` launcher owns one typed
`DocumentTreeRequestState` instead of parallel command and argument fields.
Delete-file, delete-folder and save-logcat launches atomically capture their
target, reject overlap, persist the paired snapshot through the Activity bundle,
and take the exact owner before dispatching a result. Invalid restore, an
ownerless result and launcher failure cannot reuse or overwrite another
operation's target. Launch and result dispatch also require an active service
generation. Activity teardown permanently closes the state, releases its
pending `FileInfo` graph and rejects every later request.
Book deletion and remove-from-recent capture a clone-on-boundary
`DeletionSnapshot` of the target and parent before showing confirmation.
Direct-file and SAF success enter the same history effect. Database readiness is
nullable-safe and tied to the captured service generation; delayed directory or
recent-shelf refresh checks that lifecycle again and cannot update a destroyed
Activity.
Recursive folder deletion extends the same clone-on-boundary contract across
direct I/O and DocumentsContract fallback. Successfully removed child books
cross back to the UI as one copied batch for a lifecycle-checked database
effect, and only the captured parent may be refreshed. The SAF retry attempt is
stored atomically with the picker command and target and survives Bundle
restore; concurrent operations share no mutable retry counter. Picker
cancellation refreshes only the captured parent after any partial direct
deletion.
Logcat export is owned by an Activity-scoped `LogcatExportSession`. Its
immutable filename and time boundary admit only one active request; direct-file
and SAF document creation, stream opening and the bounded `logcat` process all
run off the UI thread. The output owner is always closed, while preferences and
completion UI are published only by the exact request of an active service
generation. Activity destruction closes the session and rejects late process
completion.
Asynchronous preparation of a non-reader `OptionsDialog` is latest-only through
an Activity-owned `OptionsDialogRequestSession`. Replacement or switching to
reader mode invalidates an older font result, destruction closes the owner, and
only the exact request of an active service generation may create a dialog. The
font catalog is copied across the background/UI boundary and cloned again by
the dialog, so no caller-owned array escapes into its generation.
Each `LoadDocumentTask` retains its own `BookInfo`, settings and completion
state rather than consulting a book pointer that another request can replace
while native parsing is running. The old book is saved and marked closed before
the new metadata is published, and the serialized native boundary closes any
document left by a parse that became stale mid-flight. Only the current token
may publish reader UI, run failure recovery, or replace the stream book with a
late database/cache result.

Operations against the selected document carry a second exact
`DocumentLoadLifecycle.Interaction` identity together with their captured
`BookInfo`. Replacement, close, destruction and navigation to browser/root
rotate that identity even when a published load request itself must remain
alive for stream reconciliation. History and bookmark navigation, position
engine commands, scroll/go-to work, position queries/status publication, TOC
and go-to dialog callbacks, and gesture or programmatic page flips validate
both identities before native mutation and again before GUI completion.
`TOCDlg` retains only a narrow page-selection callback, not `ReaderView`.
Each animation owns the exact book/interaction pair, scheduled frames address
that same animation instance, and document teardown clears queued updates
before native close. A callback or page flip from an older book therefore
cannot move, draw, report, or persist a position for its replacement.

The reader book-info popup follows the same captured book/interaction boundary
and adds a latest-only `CloseableTaskGate`. Replacement or close cancels a
pending native lookup, destruction permanently closes the owner, and GUI
completion must claim the exact request. File, system and book metadata are
copied before the background handoff into an immutable
`ReaderBookInfoSnapshot`; bookmark and position data are read only from the
same document generation. Repeated requests, book replacement and teardown
therefore cannot open a stale dialog or combine one book's metadata with
another book's position.

Native viewer-settings readback after zoom and font commands follows the same
captured book/interaction boundary and a latest-only `CloseableTaskGate`. An
immutable `ReaderSettingsSyncSnapshot` performs an optimistic per-key merge: a
native value is applied only while the current GUI value and key presence still
match the request baseline, preserving newer same-key and unrelated settings.
`updateSettings`, replacement and close cancel the request, destruction closes
the owner, and a late callback cannot publish settings to a destroyed Activity.
Forward settings application from startup, GUI updates and document loading
uses an immutable `ReaderSettingsApplyRequest`. It captures the interaction and
book-language string without retaining mutable `BookInfo`; stream
reconciliation may therefore rebind the book identity while keeping the same
native document owner. Background application no longer reads mutable book
metadata or recaptures a replacement document through an ownerless redraw.
After native settings apply it derives one exact `ReaderRenderRequest` for the
still-current interaction, while the cross-thread current-book identity itself
is published through a volatile reference.
Page-background rendering now uses one synchronized
`ReaderBackgroundState` instead of separate texture, bitmap, tiling and color
fields. Surface and toolbar drawing consume one immutable snapshot while
holding its render boundary; replacement can return the previous bitmap for
recycling only after every in-flight Canvas draw has left that boundary.
Unchanged or post-close publication returns only its unused candidate. Reader
destruction closes the background owner immediately after Surface callbacks,
then recycles its final bitmap; the redundant pre-settings texture write and
the destroy-time bitmap leak are removed.

Selection and search preserve the same captured book/interaction pair from a
native gesture update through selection-toolbar actions, asynchronous search
history, both forward/backward native search passes, the find-next popup,
selection clearing and bookmark-highlight rendering. Every native phase and
GUI completion revalidates that pair. A stale toolbar cannot restore view mode
or clear selection on a replacement document. `SelectionToolbarDlg`,
`SearchDlg`, `FindNextDlg` and the dictionary picker retain narrow
generation-aware handlers instead of `ReaderView`. Toolbar mode adjustment,
selection-bound movement, copy/persistence decisions, bookmark/search actions,
quotation metadata and delayed overlap scrolling all return through the exact
handler; the UI cannot query or carry document ownership itself. A delayed
dialog callback therefore cannot recapture the new document as though it owned
the original selection.

Bookmark list and editor actions follow the same ownership boundary. They
retain a captured `BookInfo` plus a narrow `BookmarkInteractionHandler`, not
`ReaderView`; add, update, delete, shortcut and go-to effects revalidate the
original interaction before touching the model, database or native document.
Current-page bookmark lookup also carries that pair through engine completion.
Position-save scheduling captures the pair before its GUI handoff and preserves
it through delayed apply, so an older dialog or timer cannot mutate or persist
the replacement book.

Library book search has a separate dialog-owned boundary. `BookSearchDialog`
captures each field set in an immutable query and submits it through a narrow
database backend instead of retaining `CoolReader` as its search provider. A
replaceable GUI scheduler removes the previous debounce callback, while an
exact `BookSearchSession` preview token rejects a database result after another
query or dialog close. Submit and cancel share a single terminal transition, so
`BaseDialog.onClose()` cannot turn a confirmed search into a second
cancellation. The backend also checks the captured service generation before
delivering results to the dialog or browser.
Reader document search has the same dialog boundary. Search-history loading
must claim the dialog's exact `CloseableTaskGate` token while both its document
interaction and service generation remain active. Dismiss closes that owner
and detaches its one-shot database-bind wrapper before base-dialog cleanup.
The pending connector queue therefore releases the dialog reference, and a late
database result cannot attach Views to a closed dialog. Search submission
persists a copied `BookInfo` through a temporary database disconnect without
retaining the dialog itself. The find-next popup folds outside touch, close,
Back and platform dismiss into one terminal selection cleanup instead of
posting duplicate native work.
Book-info editing has two exact ownership levels. `CoolReader` gives each
pending database lookup a latest-only `BookInfoDialogSession` request, so a
second selection or Activity teardown rejects the earlier result before a
dialog is constructed. Each visible `BookInfoEditDialog` owns another session
for its cover bind/render callback and detaches the pending bind wrapper on
every terminal action or dismiss. Back uses the same save-and-close path as the
visible back button. Edited metadata updates the captured book and browser
snapshot once, while persistence uses a copied `BookInfo`; a temporary database
disconnect queues only that persistence callback and cannot retain the dialog
or dereference a null binder.
OPDS catalog editing serializes warning confirmation and save/cancel/delete
through a `CatalogEditSession`. Only the first terminal action can win, so
`BaseDialog` dismiss bookkeeping cannot repeat a save or close path. The
blacklist warning is entered only for a matching URL. Save and confirmed delete
capture immutable IDs/text, recheck the service generation, and queue through a
temporary database disconnect without dereferencing a missing binder or
retaining the editor dialog.

Database and TTS service connectors use application context for their
process/service lifetime. UI callbacks and Activity references remain
generation-scoped and detachable.
The database connector owns each platform registration through a pure
`ServiceBindingState`. Concurrent waiters join only that registration's
callback queue. Bind failure, null binding and binding death discard the queue
and permit a fresh retry; Activity unbind invalidates the exact registration so
its late `onServiceConnected` cannot publish a binder or run callbacks.
Temporary platform disconnect keeps the registration and accepts a system
reconnect. The nullable `BaseActivity.getDB()` boundary represents both initial
binding and temporary disconnect without throwing.

The native Engine's SAF-era process snapshot contains only immutable empty
legacy-root collections, a one-time font initialization candidate and the
final native DOM version. Each Engine generation owns its own
`MountPathCorrector`; no potentially mutable path graph is shared between
Activities.
Built-in background metadata lives in one immutable
`BackgroundTextureCatalog`. Texture values have private final fields, external
files are merged into an unmodifiable `none → external → built-in` snapshot,
and lookup no longer shares mutable elements or an Engine-owned backing array
with reader/options UI.

Packaged and pre-init file hyphenation definitions build through one
synchronized process registry. JNI receives a frozen array snapshot exactly
once; Java callers receive independent arrays, and definitions arriving after
native initialization are rejected instead of appearing only on the Java side.

Audio sibling selection and filename-safe transliteration are private,
immutable policies owned by `Utils`. Audio extension priority is copied into an
unmodifiable list, transliteration tables copy their replacement arrays, and
callers use narrow lookup methods instead of receiving process-wide arrays.
The filename policy is independent of OPDS download types.

Scanner filesystem/resource access and cached online-store plugins also retain
only application context. Cached process objects must never capture the
Activity that first requested them.

External document validation is isolated from `CoolReader`: local inputs are
probed by `ExternalDocumentValidator`, while resolver-owned content URIs remain
in the Activity's ContentResolver flow. Storage helpers such as
`DocumentFileCache` depend on `Context`, not on an Activity subtype.
`DocumentFormat` metadata is immutable as well: constructor and public
extension/MIME array boundaries clone storage, and internal callers use the
narrow primary-extension accessor instead of indexing an exposed backing
array.

## Testable view geometry

`ReaderView` delegates natural page-flip lookup indexing to the pure
`PageFlipGeometry` component. It widens multiplication before division and
clamps every lookup to the table's last valid index. Boundary and integer-limit
behavior is covered by a local JVM test, without constructing an Activity,
Surface or native document. The matching sine/arcsine page-curl curves are
built once by `PageCurveTables`; its arrays are private, final, instance-owned
storage and the legacy numeric samples are locked by a pure JVM regression.
Tap actions and their highlight rectangles likewise share the pure
`TapZoneGeometry` boundary. One row-major 3×3 partition now handles
non-divisible surface sizes, clamps coordinates, widens boundary arithmetic and
returns an empty immutable rectangle for an invalid surface, so action routing
and visual feedback cannot drift or divide by zero.
Tap-highlight visibility and bounds are published through one synchronized
`TapHighlightState` instead of a non-atomic numeric generation plus a shared
mutable `Rect`. Show/hide transitions carry both old and new immutable bounds
so replacement redraws the complete dirty union. Delayed unhighlight uses a
reader-owned cancelable scheduler and can hide only its latest request;
animation/page/book invalidation clears the same owner, while `destroy()`
closes it before native teardown.
Requested viewport dimensions and delayed native resize ownership live in one
`ViewportResizeState`. Its immutable volatile size snapshot replaces parallel
width/height fields, while identity requests make latest-size-wins apply to
both background resize and GUI completion. Invalid dimensions use a positive
fallback, one reader-owned scheduler replaces pending delays, and destroy
closes the state and removes the callback before native teardown.
Surface creation, window visibility/focus and permanent close are combined in
one synchronized `ReaderSurfaceState` instead of a standalone created flag.
Either ordering of visible-window and surface-created callbacks produces one
ready refresh. The delayed E-Ink focus refresh carries an exact state token and
uses a reader-owned scheduler; focus loss, hiding, surface destruction,
replacement and reader destruction invalidate it. Execution additionally
requires the same visible surface and active service generation, while every
draw checks the same closeable surface state before locking a canvas.
Reader destruction closes that state before every other reader owner, removes
the touch, key and focus listeners, and unregisters the
`SurfaceHolder.Callback`. Inner `SurfaceView` visibility, focus, size and draw
callbacks, holder changes, input/focus handlers and delayed redraw all reject
closed state before touching the Activity or scheduling more work.
Native `DocView` creation, configuration and destruction belong to one
synchronized `ReaderNativeLifecycle`; the parallel `mInitialized` flag is
gone. Reader destruction permanently closes that owner and always appends one
cleanup task to the same FIFO background queue behind any already posted
create/configure work. Closing before create suppresses creation, while
closing during create records the completed native object, rejects late
configuration/publication and destroys it exactly once. Activity teardown
therefore cannot resurrect or leak a `DocView`.
Delayed current-position persistence is owned by a `CloseableTaskGate`, a
dedicated GUI scheduler and an immutable `ReaderPositionSnapshot` rather than a
numeric generation left in the global Handler. The exact token owns both the
native capture and its delayed apply. `DocView` position is read only on the
shared serialized background FIFO; the GUI receives an independent bookmark
copy only after the captured `BookInfo` and interaction have been revalidated.
Document replacement cancels the token before rotating the interaction, and
destroy closes the gate. Pause, close, reload and TTS save synchronously obtain
their final exact snapshot through the same FIFO before persistence, preserving
fresh-position semantics without a GUI-to-`DocView` call. A stale capture can
therefore neither publish into the model nor save a bookmark into its
replacement.
Duplicate suppression after that capture belongs to synchronized
`ReaderPositionPersistenceState`, not to a mutable retained `Bookmark`. Each
database write has an exact request tied to the current `BookInfo` identity and
an immutable position string, including a valid null value. A write is recorded
only after the captured nullable-safe binder accepts save and flush; failure,
replacement and a newer request invalidate only their matching pending token.
Document load replaces the state with its exact book, stale animation cleanup
can invalidate only that same identity, and reader destruction closes the owner
permanently. Stream-to-database reconciliation rebinds the owner when it
publishes the resolved `BookInfo` identity.
Interactive selection previews and their terminal update share one
`CloseableTaskGate`. Every drag sample replaces the prior identity token, and
both native work and GUI completion must still own that token. A stale
gesture-end callback therefore cannot open a toolbar or clear selection after
a newer gesture begins. Clear, reload and book close cancel the current update;
reader destruction permanently closes the gate before native teardown.
`PositionProperties` widens scrollable-height subtraction and percentage
multiplication before clamping to its 0–10000 contract. Scroll movement uses the
same widened range. The stateless `DocumentPositionPolicy` converts zero-based
page indices for display, formats one-decimal percentages, and maps 0–100 input
back to an in-range page with widened multiplication and an exact last-page
boundary. Popups, book info and go-to dialogs use these shared contracts rather
than dividing directly by an unchecked document height.
Gesture animation uses the same boundary: each `ReaderView` owns an immutable
`GestureAcceleration` curve, input is clamped, and interpolation widens before
arithmetic so the full signed-integer range cannot overflow.
Each reader also owns an `AnimationTiming` sample window. It validates persisted
draw averages, computes fractional scroll frames without integer truncation,
and widens autoscroll character-duration arithmetic before clamping progress.
These timing rules are pure JVM-tested rather than embedded in rendering code.
Reading-session duration is similarly owned by one `ReadingTimeTracker`.
Visibility/focus start and stop signals are idempotent, elapsed reads never
mutate persisted data, clock regressions cannot subtract time, and accumulation
saturates instead of overflowing. `ReadingTimeFormatter` carries that `long`
contract through last-position UI: it clamps invalid negative values and never
narrows multi-day or saturated durations before locale-aware formatting.

Options UI state is scoped to each `OptionsDialog` generation. Resource-backed
motion and gesture choices, format capability flags, and icon visibility no
longer live in process-wide fields. Shared backlight values are exposed only
through the pure `BacklightOptions` owner; callers receive copies and localized
titles are built per Activity instead of mutating a global array. Document
style codes and label resources are paired in one immutable
`StyleOptionCatalog`, preventing the former parallel arrays from drifting.
The nested font picker gives each language-filter scan an exact
`FontFilterSession` request and a deep-copied candidate snapshot. Replacing or
unchecking a request and dismissing the picker physically stop its
`Scanner.ScanControl`; completion may update the adapter only after claiming
the same request. The picker performs cleanup through an overridden
`BaseDialog.onClose()` rather than replacing the base dismiss listener, so
Activity dialog bookkeeping is preserved and a closed picker cannot publish
late font results.
TTS options use independent latest-only `TtsOptionsSession` channels for
engines, locales, voices and engine initialization. A newer locale rejects a
late voice list from its predecessor, engine replacement invalidates both
dependent catalogs, and every service outcome returns to the GUI owner before
touching option Views. Closing the dialog permanently invalidates all channels.
Online-store UI uses an equivalent multi-channel
`OnlineStoreDialogSession`. Browser directory loads, book-info loads, cover
publication, downloads and authentication each own an exact request. Replacing
a request, navigating elsewhere, dismissing its dialog or tearing down the
browser closes that owner and invokes the attached `AsyncOperationControl`
cancellation. `OnlineStoreWrapper` guards its public callback boundary as well,
so a plugin that delivers after cancellation cannot reach Activity UI.
Login stays visible and disables its action while authentication is pending;
only the exact GUI completion may restore controls, dismiss on success and
notify its parent. Book-info reload, cover and download callbacks, and browser
directory/detail callbacks additionally require the captured service
generation before touching Views or opening another dialog.
`BaseActivity` owns nested `BaseDialog` instances as a stack instead of one
replaceable pointer. Closing a child restores its parent as the current dialog,
and Activity destruction takes the stack children-first and dismisses every
showing dialog. Each dialog's normal `onClose()` path therefore cancels its
owned work during teardown without losing the parent's bookkeeping.
File-browser navigation has one closeable exact owner as well.
`FileBrowserNavigationSession` spans CRDB genre/author/series/state queries,
filesystem scans and OPDS catalog or book transfers. Every public directory
selection replaces the prior request. Replacement, leaving the browser and
teardown invoke the cancellation attached to that request, so the exact
`Scanner.ScanControl` or OPDS `DownloadTask` is stopped and stale progress is
cleared. CRDB results and scan initial/final callbacks require both the captured
service generation and navigation identity before changing the current
directory.
OPDS pagination may publish multiple partial pages while its request remains
active; finish, error or book-download completion must claim that request
exactly once. Direct clicks and context-menu OPDS actions enter through the same
navigation boundary, so a late feed cannot pull the browser back to an abandoned
catalog. Browser close permanently closes the owner and unregisters the View
from `History` and `Scanner` change sources.
The home screen owns its refresh and listener lifecycle independently.
`RootViewRefreshSession` gives recent books, online catalogs, filesystem
folders and library shortcuts separate latest-only channels. Rebuilding the
view for a theme change invalidates every request from the old `mView`, and a
result must claim its exact channel request while the captured service
generation is active before updating a shelf.
`CRRootView` stores one `FileSystemFolders` listener for its whole lifetime
instead of adding an anonymous listener on every rebuild, and removes it on
close. The root view also owns idempotent cover-listener registration across
resume, pause and close; `CoolReader` only forwards those lifecycle events.
Activity destruction closes all refresh channels, while a delayed initial
database bind checks service and Activity ownership before constructing home
UI.
Reader-mode options additionally capture the exact `BookInfo` and document
interaction before fetching the native font catalog. The lookup belongs to a
latest-only `CloseableTaskGate`: a repeated request replaces its predecessor,
document replacement or close cancels it, and destruction permanently closes
the owner. The native font array is cloned before the background-to-GUI
handoff. The dialog receives an immutable `ReaderDocumentOptions` snapshot
plus a narrow generation-aware handler, never a mutable `ReaderView`. Applying
the dialog batches reflow, document styles, embedded fonts, DOM version and
block-rendering flags against that exact book, persists the book once, and
schedules at most one reload or render. A reload consumes all five captured
per-book settings, including the embedded-font command. If the interaction has
been replaced, the handler rejects every document/native/DB effect; unrelated
Activity settings selected in the same dialog may still be applied normally.
Activity-level dictionary lookup is owned by `DictionaryLookupSession` and a
cancelable GUI scheduler. A newer lookup, `showDictionary()` or Activity
destruction physically removes its predecessor, and only the exact active
request may launch an external dictionary intent. Pure code-point-aware query
normalization accepts single and supplementary Unicode letters, retains
trailing combining marks and removes only outer punctuation.
Profile selection follows the stricter document boundary. A
`SwitchProfileDialog` retains only a `ProfileSwitchHandler` created for the
captured `BookInfo` and interaction. The handler validates both the profile
range and exact document generation before changing the book's profile ID,
persisting it, or loading the corresponding Activity settings. A click from a
dialog left open across document replacement is therefore a complete no-op for
the replacement book and its settings.
Interface themes follow the same boundary: each `BaseActivity` owns an
immutable `InterfaceThemeCatalog` built from its E-Ink snapshot. Theme visual
metadata is final, the ordered catalog is unmodifiable, and `OptionsDialog`
enumerates that Activity-owned view instead of a public process-wide array.
Numbered-profile selection is delegated by `SettingsManager` to one immutable
`ProfileSettingsFilter`. Exact, wildcard-prefix and `styles.` rules are
pure JVM-tested, both load and save use the same matcher, and `Settings` no
longer exposes the rule backing array.
The final settings snapshot is written through a stateless
`SettingsFileStore` owned by `SettingsManager`. It validates the target and
snapshot before opening the file, scopes the output stream with
try-with-resources, and preserves the existing synchronous save semantics.
The unreachable delayed-save executor and callback graph have been removed.

Screen-backlight user-activity timestamps and scheduled timer tasks belong to
the `ScreenBacklightControl` of one `BaseActivity` generation. Threshold and
reschedule decisions are delegated to the pure, overflow-safe
`BacklightTimeoutPolicy`; activity from a replacement screen cannot extend an
older screen's WakeLock.

OPDS feed timestamps are parsed by the pure `FeedTimestampParser`. Formatter
instances are method-scoped, UTC and locale handling are explicit, parsing is
strict, and both compact and colon timezone offsets are normalized without
shared `SimpleDateFormat` state.

Each Nook screen-controller generation owns an immutable
`NookEpdControllerBindings` reflection graph. Disabled devices do not resolve
proprietary classes, legacy devices preserve their static vendor call, and
Nook 1.2 creates its controller only from the current View host. Vendor
methods, constructors, enum arrays and failure diagnostics are not retained in
mutable process-wide fields.

Temporary E-Ink full-refresh suppression is owned by one `ReaderView` through
`EinkRefreshLeaseTracker`. The first unique client saves the controller
interval, overlapping clients share the lease, and only the last matching
release restores it. Duplicate or unmatched transitions are no-ops, and every
integer interval—including negative vendor values—is preserved as data rather
than overloaded as lifecycle state.

Battery broadcasts cross the Activity/reader boundary as one immutable
`BatteryStatus`. Android's raw level is normalized against its advertised scale
with widened arithmetic and clamped to 0–100. `CoolReader` retains one initial
snapshot before view creation, while `ReaderView` publishes a single volatile
snapshot to render work and captures it once before updating the native
document, avoiding mixed state/charger/level generations.

Load/format progress is similarly owned by one `ReaderProgressState`. Its
initial state is explicitly hidden, `show` publishes an atomic position,
resource and resolved-title snapshot, and `hide` is idempotent. A zero position
remains a valid visible start state; duplicate callbacks do not redraw or
reacquire E-Ink refresh suppression, and the renderer cannot combine fields
from different progress generations.
The horizontal modal `ProgressDialog` is updated directly by the Engine's
existing GUI-serialized progress path. Its former Handler consumed empty
messages and owned no work, so it has been removed. Number and percentage
views now use the stateless, locale-aware `ProgressDisplayState`, which clamps
out-of-range values and defines a stable zero-maximum display.
Engine progress request ownership is tracked separately by the synchronized
`ProgressUiState`. Identity tokens replace the former non-atomic numeric
generation and parallel visibility flag. Latest show/hide wins; a
`DelayedProgress` can cancel a pending show or dismiss exactly its own visible
token, never a replacement request. Activity detach permanently closes the
state before the dialog is dismissed through the GUI dispatcher, so queued
work cannot recreate UI for a stale generation.
Animation and GC delayed executors are private final members of one
`ReaderView`. Animation handoff uses that reader's instance lock and a volatile
active-animation reference rather than the nested class monitor shared by all
Activities. `destroy()` always cancels both executors and clears pending
animation state before native document teardown, including repeated or
partially initialized teardown. The unused volatile animation serial, which
never participated in ordering or cancellation, has been removed.
Coalesced `DrawPageTask` work has a separate exact `CloseableTaskGate` owner
instead of a numeric generation, plus an immutable `ReaderRenderRequest` that
captures exact book and interaction identity, including the initial null-book
generation. Only the current token and document request may enter rendering or
publish its terminal GC schedule. A newly rendered bitmap is revalidated after
native resize, position lookup and page rendering before it enters the shared
page cache. Command completion remains independent of latest-render ownership,
so a redundant draw does not swallow repeat-action or engine-command
completion for the same document. Replacement and close cancel the draw gate;
stale completion or failure cannot call the handler, schedule GC, or hide the
next load's progress. `destroy()` closes the gate before native teardown and
permanently rejects late render, callback and GC work.
Page preparation itself now has no nullable/ownerless overload. Selection,
tap-highlight show/hide, autoscroll, gesture animations, document load,
error-document rendering and position restore all pass their captured
`ReaderRenderRequest`. Current and offset bitmaps are revalidated after native
movement/render and published through one lifecycle-locked helper only after
the native position has been restored, so interaction rotation cannot occur
between the final check and cache-slot assignment. A stale candidate is
recycled without replacing either shared slot. Invalidation claim and cache
clearing use the same lock, and an existing current bitmap remains available
until its validated replacement is published.
Page-cache invalidation is a separate synchronized
`ReaderPageInvalidationState`, replacing the cross-thread `invalidImages`
boolean. GUI settings, native callbacks and Engine operations install a fresh
identity; page preparation claims exactly the identity it is about to apply.
Repeated requests coalesce, while an invalidation arriving during slot
recycling remains distinct and pending for the next preparation instead of
being overwritten by a trailing boolean reset. Reader destruction closes this
owner after the render gate.
Asynchronous native commands use one `ReaderEngineCommandPolicy`. Every
document command, including zoom and render configuration, validates its
captured book and interaction before and after native mutation and again before
GUI completion, so an old command cannot affect or invalidate a replacement
document. Rotation metadata for font antialiasing is the sole reader-scoped
command: it still requires the active native/service lifecycle, survives a book
replacement, and captures the then-current render owner on completion.
Movement and position-save behavior comes from the same exhaustive JVM-tested
policy instead of a second switch in `ReaderView`.
Asynchronous document close owns an exact `ReaderPageCacheClose`. Current cache
identities are captured before queueing; the shared slots are captured again
and detached at the serialized Engine boundary after native close and before
the next `LoadDocumentTask`. Late GUI success or failure releases only those
four captured identities with one-shot deduplication; it never nulls or
recycles a replacement book's shared cache. Close also queues this native
boundary when browser/root navigation claims an unpublished in-flight load,
while leaving image-viewer mode during replacement does not submit a new draw
request.
Autoscroll has a separate synchronized `AutoScrollSessionState` and cancelable
GUI scheduler. A session is renderable only after its exact owner completes
background initialization; initialization temporarily suppresses drawing and
cannot republish after stop or destroy. Timer rescheduling and cancellation
share the same owner lock, speed publication is volatile, book close finishes
only an initialized session, and `ReaderView.destroy()` permanently rejects
late initialization and timer callbacks before native teardown. Every
animation also captures the exact `BookInfo` and document interaction.
Initialization, page-image preparation, timer work, native page turns and
rendering revalidate that pair. Stop completion returns to the GUI with the
same pair before redraw or position persistence; replacement instead abandons
the stale session without moving or saving the new document.
Deferred native swap-to-cache retries use a `CloseableTaskGate` identity token
and their own cancelable GUI scheduler instead of a volatile task pointer.
Loading or closing a book invalidates the exact retry generation while keeping
the reader reusable; terminal success/failure clears only its matching owner.
`ReaderView.destroy()` closes the gate and removes the pending delay before
native teardown, so a retained callback cannot swap an already destroyed
`DocView`.
Font-face next/previous commands delegate catalog navigation to the stateless
`FontFaceSwitcher`. Empty native catalogs are a no-op, an unavailable current
face starts at the directional edge, and known faces wrap safely after
normalizing direction magnitude.

Each `ReaderView` also owns its bitmap pool and `VMRuntimeHack`. The optional
legacy VM reflection bindings are final, accounting is synchronized and uses a
`long`, and failed vendor calls do not corrupt the local total. Bitmap memory
uses the actual row stride; the legacy surface estimate widens before
multiplication. Replacement reader generations therefore cannot share pool or
diagnostic accounting state.

`ReaderAction` definitions are immutable command metadata. The assignable
catalog is exposed only as copies, and its device-specific addition is selected
by an explicit accessor rather than during class initialization. Theme
drawable overrides live in an immutable `ActionIconSet` snapshot owned by each
`BaseActivity`; toolbars resolve icons through their current Activity instead
of reading a process-wide mutable action field.

Key and tap defaults are built as one immutable `DefaultInputActions` snapshot
per `SettingsManager`. Device flags are captured at construction. Nook
navigation mappings may replace conflicting generated defaults, while an
explicit user mapping remains authoritative; menu-access fallback logic is
covered without constructing an Activity.

External dictionary intent definitions live in one immutable
`DictionaryCatalog`. Every `DictInfo`, including its intent data key, is final;
legacy array callers receive independent snapshots, so one dialog cannot
rewrite integrations observed by another Activity generation.

## Migration rule

New Android components should:

1. take dependencies in a constructor or explicit factory method;
2. take `ServiceLifecycle` when work can outlive its caller;
3. avoid static mutable Activity, View or Service references;
4. make teardown idempotent and scoped to the owning generation;
5. expose narrow interfaces so lifecycle and background behavior can be tested
   without launching the complete reader.
