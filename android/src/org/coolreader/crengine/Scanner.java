/*
 * CoolReader for Android
 * Copyright (C) 2010-2012,2015 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2013 Alexey Kabelitskiy <akabelytskyi@hmstn.com>
 * Copyright (C) 2018-2021 Aleksey Chernov <valexlin@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.crengine;


import org.coolreader.R;
import org.coolreader.db.CRDBService;
import org.coolreader.plugins.OnlineStorePluginManager;
import org.coolreader.plugins.OnlineStoreWrapper;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

public class Scanner extends FileInfoChangeSource {
	
	public static final Logger log = L.create("sc");
	private static final int METADATA_BATCH_SIZE = 64;
	private static final int DEFAULT_MAX_DISCOVERY_ENTRIES = 100_000;
	private static final int DEFAULT_MAX_DISCOVERY_DEPTH = 256;
	
	HashMap<String, FileInfo> mFileList = new HashMap<>();
//	ArrayList<FileInfo> mFilesForParsing = new ArrayList<FileInfo>();
	FileInfo mRoot;
	
	boolean mHideEmptyDirs = true;
	
	public void setHideEmptyDirs( boolean flgHide ) {
		mHideEmptyDirs = flgHide;
	}

	private boolean dirScanEnabled = true;
	public boolean getDirScanEnabled()
	{
		return dirScanEnabled;
	}
	
	public void setDirScanEnabled(boolean dirScanEnabled)
	{
		this.dirScanEnabled = dirScanEnabled;
	}
	
	private FileInfo scanZip(FileInfo zip, ScanControl control)
	{
		try {
			File zf = new File(zip.pathname);
			long arcsize = zf.length();
			//ZipFile file = new ZipFile(zf);
			ArrayList<ZipEntry> entries = Engine.getArchiveItems(zip.pathname);
			ArrayList<FileInfo> items = new ArrayList<FileInfo>();
			//for ( Enumeration<?> e = file.entries(); e.hasMoreElements(); ) {
			for ( ZipEntry entry : entries ) {
				if (isScanStopped(control))
					return null;
				if (!recordDiscoveryEntry(
						control, entry.isDirectory()))
					return null;
				if ( entry.isDirectory() )
					continue;
				String name = entry.getName();
				FileInfo item = new FileInfo();
				item.format = DocumentFormat.byExtension(name);
				if ( item.format==null )
					continue;
				File f = new File(name);
				item.filename = f.getName();
				item.path = f.getPath();
				item.pathname = entry.getName();
				item.size = entry.getSize();
				//item.createTime = entry.getTime();
				item.createTime = zf.lastModified();
				item.arcname = zip.pathname;
				//item.arcsize = entry.getCompressedSize();
				item.arcsize = zip.size;
				item.isArchive = true;
				item.scanFingerprint =
						LibrarySourceFingerprint.forDocument(item);
				items.add(item);
			}
			if ( items.size()==0 ) {
				L.i("Supported files not found in " + zip.pathname);
				return null;
			} else if ( items.size()==1 ) {
				// single supported file in archive
				FileInfo item = items.get(0);
				item.isArchive = true;
				item.isDirectory = false;
				return item;
			} else {
				zip.isArchive = true;
				zip.isDirectory = true;
				zip.isListed = true;
				for ( FileInfo item : items ) {
					item.parent = zip;
					zip.addFile(item);
				}
				zip.scanFingerprint =
						LibrarySourceFingerprint.forDirectory(zip);
				return zip;
			}
		} catch ( Exception e ) {
			L.e("IOException while opening " + zip.pathname + " " + e.getMessage());
		}
		return null;
	}

	public boolean listDirectory(FileInfo baseDir) {
		return listDirectory(baseDir, true, true);
	}

	public boolean listDirectory(FileInfo baseDir, boolean onlySupportedFormats, boolean scanzip) {
		return listDirectory(baseDir, onlySupportedFormats, scanzip, false);
	}

	/**
	 * Adds dir and file children to directory FileInfo item.
	 * @param baseDir is directory to list files and dirs for
	 * @param onlySupportedFormats list only supported files
	 * @param scanzip scan zip-files
	 * @param rescan full directory rescan
	 * @return true if successful.
	 */
	public boolean listDirectory(FileInfo baseDir, boolean onlySupportedFormats, boolean scanzip, boolean rescan)
	{
		return listDirectory(
				baseDir, onlySupportedFormats, scanzip, rescan, null);
	}

	private boolean listDirectory(FileInfo baseDir,
			boolean onlySupportedFormats, boolean scanzip, boolean rescan,
			ScanControl control)
	{
		if (cancelDirectoryListing(baseDir, control))
			return false;
		Set<String> knownItems = null;
		if ( baseDir.isListed ) {
			if (rescan) {
				baseDir.clear();
			} else {
				knownItems = new HashSet<String>();
				for (int i = baseDir.itemCount() - 1; i >= 0; i--) {
					if (cancelDirectoryListing(baseDir, control))
						return false;
					FileInfo item = baseDir.getItem(i);
					if (!item.exists()) {
						// remove item from list
						baseDir.removeChild(item);
					} else {
						knownItems.add(item.getBasePath());
					}
				}
			}
		}
		try {
			File dir = new File(baseDir.pathname);
			//File[] items = dir.listFiles();
			// To resolve unhandled exception
			// 'JNI DETECTED ERROR IN APPLICATION: input is not valid Modified UTF-8: illegal continuation byte 0'
			// that can be produced by invalid filename (broken sdcard, etc)
			// or 'JNI WARNING: input is not valid Modified UTF-8: illegal start byte 0xf0'
			// that can be generated if 4-byte UTF-8 sequence found in the filename,
			// we implement own directory listing method instead of File.listFiles().
			// TODO: replace other occurrences of the method File.listFiles().
			File[] items = Engine.listFiles(dir);
			// process normal files
			if ( items!=null ) {
				for ( File f : items ) {
					if (cancelDirectoryListing(baseDir, control))
						return false;
					if (!f.isDirectory()
							&& !recordDiscoveryEntry(
									control, false)) {
						cancelDirectoryListing(
								baseDir, control);
						return false;
					}
					// check whether file is a link
					if (Engine.isLink(f.getAbsolutePath()) != null) {
						log.w("skipping " + f + " because it's a link");
						continue;
					}
					if (!f.isDirectory()) {
						// regular file
						if (f.getName().startsWith("."))
							continue; // treat files beginning with '.' as hidden
						if (f.getName().equalsIgnoreCase("LOST.DIR"))
							continue; // system directory
						String pathName = f.getAbsolutePath();
						if ( knownItems!=null && knownItems.contains(pathName) )
							continue;
						if (engine.isRootsMountPoint(pathName)) {
							// skip mount root
							continue;
						}
						boolean isArc = Engine.isArchive(pathName);
						FileInfo item = !rescan ? mFileList.get(pathName) : null;
						boolean isNew = false;
						if ( item==null ) {
							item = new FileInfo( f );
							if ( scanzip && isArc ) {
								item = scanZip(item, control);
								if (cancelDirectoryListing(
										baseDir, control))
									return false;
								if ( item==null )
									continue;
								if ( item.isDirectory ) {
									// many supported files in ZIP
									item.parent = baseDir;
									baseDir.addDir(item);
									for ( int i=0; i<item.fileCount(); i++ ) {
										FileInfo file = item.getFile(i);
										mFileList.put(file.getPathName(), file);
									}
								} else {
									item.parent = baseDir;
									baseDir.addFile(item);
									mFileList.put(pathName, item);
								}
								continue;
							}
							isNew = true;
						}
						if ( !onlySupportedFormats || item.format!=null ) {
							item.parent = baseDir;
							baseDir.addFile(item);
							if ( isNew )
								mFileList.put(pathName, item);
						}
					}
				}
				// process directories 
				for ( File f : items ) {
					if (cancelDirectoryListing(baseDir, control))
						return false;
					if ( f.isDirectory() ) {
						if (!recordDiscoveryEntry(
								control, true)) {
							cancelDirectoryListing(
									baseDir, control);
							return false;
						}
						if ( f.getName().startsWith(".") )
							continue; // treat dirs beginning with '.' as hidden
						FileInfo item = new FileInfo( f );
						if ( knownItems!=null && knownItems.contains(item.getPathName()) )
							continue;
						item.parent = baseDir;
						baseDir.addDir(item);					
					}
				}
			}
			baseDir.isListed = true;
			updateSourceFingerprints(baseDir);
			return true;
		} catch ( Exception e ) {
			if (cancelDirectoryListing(baseDir, control))
				return false;
			L.e("Exception while listing directory " + baseDir.pathname, e);
			baseDir.isListed = true;
			return false;
		}
	}

	private static void updateSourceFingerprints(
			FileInfo directory) {
		for (int i = 0; i < directory.fileCount(); i++) {
			FileInfo file = directory.getFile(i);
			file.scanFingerprint =
					LibrarySourceFingerprint.forDocument(file);
		}
		directory.scanFingerprint =
				LibrarySourceFingerprint.forDirectory(directory);
	}

	private static boolean isScanStopped(ScanControl control) {
		return control != null && control.isStopped();
	}

	private static boolean recordDiscoveryEntry(
			ScanControl control, boolean directory) {
		return control == null
				|| control.recordDiscoveryEntry(directory);
	}

	private static boolean cancelDirectoryListing(
			FileInfo directory, ScanControl control) {
		if (!isScanStopped(control))
			return false;
		directory.isListed = false;
		directory.isScanned = false;
		return true;
	}
	
	public static class ScanControl {
		private final LibraryScanState state;

		public ScanControl() {
			this(DEFAULT_MAX_DISCOVERY_ENTRIES,
					DEFAULT_MAX_DISCOVERY_DEPTH);
		}

		public ScanControl(int maxEntries, int maxDepth) {
			state = new LibraryScanState(maxEntries, maxDepth);
		}

		public boolean isStopped() {
			return state.isStopped();
		}

		public void stop() {
			state.stopByUser();
		}

		public ScanStopReason getStopReason() {
			return state.getStopReason();
		}

		public int getDiscoveredEntryCount() {
			return state.getDiscoveredEntries();
		}

		public int getMaxEntries() {
			return state.getMaxEntries();
		}

		public int getMaxDepth() {
			return state.getMaxDepth();
		}

		private boolean recordDiscoveryEntry(boolean directory) {
			return state.recordEntry(directory);
		}

		private void startRootDirectory() {
			state.startRootDirectory();
		}

		private void completeDirectory() {
			state.completeDirectory();
		}

		private int discoveryProgress() {
			return state.discoveryProgress(
					ScanProgressTracker.DISCOVERY_MAX);
		}

		private void stopAtDepthLimit() {
			state.stopAtDepthLimit();
		}
	}

	public interface ScanCompleteListener {
		void onComplete(ScanControl scanControl);
	}

	/**
	 * Call this method (in GUI thread) to update views if directory content is changed outside.
	 * @param dir is directory with changed content
	 */
	public void onDirectoryContentChanged(FileInfo dir) {
		log.v("onDirectoryContentChanged(" + dir.getPathName() + ")");
		onChange(dir, false);
	}
	
	/**
	 * For all files in directory, retrieve metadata from DB or scan and save into DB.
	 * Call in GUI thread only!
	 * @param baseDir is directory with files to lookup/scan; file items will be updated with info from file metadata or DB
	 * @param readyCallback is Runable to call when operation is finished or stopped (will be called in GUI thread)
	 * @param control allows to stop long operation
	 */
	private void scanDirectoryFiles(
			final LibraryMetadataStore metadataStore,
			final LibraryMetadataExtractor metadataExtractor,
			final FileInfo baseDir, final ScanControl control,
			final ScanProgressTracker progress,
			final boolean directoryUnchanged,
			final MetadataScanCompleteListener readyCallback) {
		// GUI thread
		BackgroundThread.ensureGUI();
		log.d("scanDirectoryFiles(" + baseDir.getPathName() + ") ");

		if (baseDir.fileCount() == 0 || control.isStopped()) {
			readyCallback.onComplete(!control.isStopped());
			return;
		}

		new MetadataScanSession(
				metadataStore, metadataExtractor,
				baseDir, control,
				progress, directoryUnchanged,
				readyCallback).scanNextBatch();
	}

	private interface MetadataScanCompleteListener {
		void onComplete(boolean complete);
	}

	private final class MetadataScanSession {
		private final LibraryMetadataStore metadataStore;
		private final LibraryMetadataExtractor metadataExtractor;
		private final FileInfo baseDir;
		private final ScanControl control;
		private final ScanProgressTracker progress;
		private final boolean directoryUnchanged;
		private final MetadataScanCompleteListener readyCallback;
		private final ScanBatchCursor batches;
		private boolean finished;
		private boolean complete = true;

		MetadataScanSession(
				LibraryMetadataStore metadataStore,
				LibraryMetadataExtractor metadataExtractor,
				FileInfo baseDir, ScanControl control,
				ScanProgressTracker progress,
				boolean directoryUnchanged,
				MetadataScanCompleteListener readyCallback) {
			this.metadataStore = metadataStore;
			this.metadataExtractor = metadataExtractor;
			this.baseDir = baseDir;
			this.control = control;
			this.progress = progress;
			this.directoryUnchanged = directoryUnchanged;
			this.readyCallback = readyCallback;
			batches = new ScanBatchCursor(
					baseDir.fileCount(), METADATA_BATCH_SIZE,
					control::isStopped);
		}

		void scanNextBatch() {
			BackgroundThread.ensureGUI();
			ScanBatchCursor.Range batch = batches.next();
			if (batch == null) {
				finish();
				return;
			}
			ArrayList<String> pathNames =
					new ArrayList<>(batch.size());
			for (int i = batch.start; i < batch.end; i++)
				pathNames.add(baseDir.getFile(i).getPathName());
			metadataStore.load(
					pathNames, control,
					list -> onFileInfoListLoaded(batch, list));
		}

		private void onFileInfoListLoaded(
				ScanBatchCursor.Range batch, ArrayList<FileInfo> list) {
			BackgroundThread.ensureGUI();
			log.v("onFileInfoListLoaded("
					+ batch.start + ", " + batch.end + ")");
			if (control.isStopped()) {
				complete = false;
				finish();
				return;
			}
			final ArrayList<FileInfo> filesForParsing = new ArrayList<>();
			final ArrayList<FileInfo> filesForCRC32Update = new ArrayList<>();
			Map<String, FileInfo> mapOfFilesFoundInDb = new HashMap<>();
			for (FileInfo f : list)
				mapOfFilesFoundInDb.put(f.getPathName(), f);

			if (directoryUnchanged
					&& restoreUnchangedBatch(
							batch, mapOfFilesFoundInDb)) {
				setProgress(batch.end);
				scanNextBatch();
				return;
			}

			for (int i = batch.start; i < batch.end; i++) {
				FileInfo item = baseDir.getFile(i);
				FileInfo fromDB = mapOfFilesFoundInDb.get(item.getPathName());
				// check the relevance of data in the database
				if (fromDB != null) {
					if (fromDB.crc32 == 0
							|| item.scanFingerprint == null
							|| !item.scanFingerprint.equals(
									fromDB.scanFingerprint)) {
						// to force rescan and update data in DB
						log.v("The found entry in the database has an " +
								"outdated source fingerprint, need to rescan " +
								fromDB.toString());
						fromDB = null;
					}
					if (null != fromDB && DocumentFormat.FB2 == fromDB.format && null == fromDB.genres) {
						// to force rescan and update data in DB
						log.v("The found entry in the database is outdated (genres=null), need to rescan " + fromDB.toString());
						fromDB = null;
					}
				} else {
					// not found in DB
					// for new files set latest DOM level and max block rendering flags
					item.domVersion = Engine.DOM_VERSION_CURRENT;
					item.blockRenderingFlags = Engine.BLOCK_RENDERING_FLAGS_WEB;
				}
				if (fromDB != null) {
					// use DB value
					baseDir.setFile(i, fromDB);
				} else {
					if (item.format != null && item.format.canParseProperties()) {
						filesForParsing.add(new FileInfo(item));
					} else {
						filesForCRC32Update.add(new FileInfo(item));
					}
				}
			}
			if (control.isStopped()) {
				complete = false;
				finish();
				return;
			}
			if (filesForParsing.isEmpty()
					&& filesForCRC32Update.isEmpty()) {
				setProgress(batch.end);
				scanNextBatch();
				return;
			}

			BackgroundThread.instance().postBackground(() -> {
				// Background thread
				final ArrayList<FileInfo> filesForSave = new ArrayList<>();
				int completed = batch.start;
				try {
					int count1 = filesForParsing.size();
					int count2 = filesForCRC32Update.size();
					for (int i = 0; i < count1; i++) {
						if (control.isStopped())
							break;
						FileInfo item = filesForParsing.get(i);
						if (metadataExtractor
								.extractProperties(item))
							filesForSave.add(item);
						else
							complete = false;
						setProgress(++completed);
					}
					for (int i = 0; i < count2; i++) {
						if (control.isStopped())
							break;
						FileInfo item = filesForCRC32Update.get(i);
						if (metadataExtractor
								.updateFileFingerprint(item))
							filesForSave.add(item);
						else
							complete = false;
						setProgress(++completed);
					}
				} catch (Exception e) {
					complete = false;
					L.e("Exception while scanning", e);
				}
				BackgroundThread.instance().postGUI(
						() -> saveBatchAndContinue(batch, filesForSave));
			});
		}

		private void saveBatchAndContinue(ScanBatchCursor.Range batch,
				ArrayList<FileInfo> filesForSave) {
			BackgroundThread.ensureGUI();
			try {
				if (!filesForSave.isEmpty())
					metadataStore.save(filesForSave);
				for (FileInfo file : filesForSave)
					baseDir.setFile(file);
			} catch (Exception e) {
				L.e("Exception while saving scan batch", e);
			}
			if (control.isStopped()) {
				complete = false;
				finish();
				return;
			}
			setProgress(batch.end);
			scanNextBatch();
		}

		private void setProgress(int completedCount) {
			int totalCount = batches.getTotalCount();
			progress.setMetadataProgress(
					completedCount, totalCount);
		}

		private void finish() {
			if (finished)
				return;
			finished = true;
			readyCallback.onComplete(complete);
		}

		private boolean restoreUnchangedBatch(
				ScanBatchCursor.Range batch,
				Map<String, FileInfo> filesByPath) {
			for (int i = batch.start; i < batch.end; i++) {
				FileInfo current = baseDir.getFile(i);
				if (!filesByPath.containsKey(
						current.getPathName()))
					return false;
			}
			for (int i = batch.start; i < batch.end; i++) {
				FileInfo current = baseDir.getFile(i);
				baseDir.setFile(
						i, filesByPath.get(
								current.getPathName()));
			}
			return true;
		}
	}

	private final class DirectoryMetadataIterator {
		private final LibraryMetadataStore metadataStore;
		private final LibraryMetadataExtractor metadataExtractor;
		private final ScanControl control;
		private final ScanProgressTracker progress;
		private final ScanCompleteListener readyListener;
		private final boolean recursiveScan;
		private final ArrayDeque<FileInfo> pending = new ArrayDeque<>();
		private boolean finished;

		DirectoryMetadataIterator(
				LibraryMetadataStore metadataStore,
				LibraryMetadataExtractor metadataExtractor,
				FileInfo baseDir, ScanControl control,
				ScanProgressTracker progress,
				ScanCompleteListener readyListener,
				boolean recursiveScan) {
			this.metadataStore = metadataStore;
			this.metadataExtractor = metadataExtractor;
			this.control = control;
			this.progress = progress;
			this.readyListener = readyListener;
			this.recursiveScan = recursiveScan;
			pending.addLast(baseDir);
		}

		void scanNextDirectory() {
			BackgroundThread.ensureGUI();
			if (control.isStopped() || pending.isEmpty()) {
				finish();
				return;
			}
			FileInfo directory = pending.removeFirst();
			String currentFingerprint =
					directory.scanFingerprint;
			boolean stableFingerprint =
					hasStableDocumentFingerprints(directory);
			metadataStore.loadDirectoryFingerprint(
					directory.getPathName(), storedFingerprint -> {
				if (control.isStopped()) {
					finish();
					return;
				}
				boolean directoryUnchanged =
						stableFingerprint
								&& currentFingerprint != null
								&& currentFingerprint.equals(
										storedFingerprint);
				scanDirectoryFiles(
						metadataStore, metadataExtractor, directory,
						control, progress, directoryUnchanged,
						metadataComplete -> {
						onDirectoryContentChanged(directory);
						if (control.isStopped()) {
							finish();
							return;
						}
						directory.isScanned = true;
						if (metadataComplete
								&& stableFingerprint
								&& currentFingerprint != null) {
							metadataStore.saveDirectoryFingerprint(
									directory.getPathName(),
									currentFingerprint);
						}
						if (recursiveScan)
							enqueueChildren(directory);
						scanNextDirectory();
					});
			});
		}

		private boolean hasStableDocumentFingerprints(
				FileInfo directory) {
			for (int i = 0; i < directory.fileCount(); i++) {
				if (directory.getFile(i).scanFingerprint == null)
					return false;
			}
			return true;
		}

		private void enqueueChildren(FileInfo directory) {
			for (int i = directory.dirCount() - 1; i >= 0; i--) {
				FileInfo child = directory.getDir(i);
				File path = new File(child.getPathName());
				if (!engine.getPathCorrector()
						.isRecursivePath(path))
					pending.addLast(child);
			}
		}

		private void finish() {
			if (finished)
				return;
			finished = true;
			progress.hide();
			readyListener.onComplete(control);
		}
	}
	
	/**
	 * Scan single directory for dir and file properties in background thread.
	 * @param db database instance to fetch/update metadata
	 * @param baseDir is directory to scan
	 * @param readyListener is called on completion
	 * @param recursiveScan is true to scan subdirectories recursively, false to scan current directory only
	 * @param scanControl is to stop long scanning
	 */
	public void scanDirectory(final CRDBService.LocalBinder db, final FileInfo baseDir, final Runnable initialUpdateCallback, final ScanCompleteListener readyListener, final boolean recursiveScan, final ScanControl scanControl) {
		// Call in GUI thread only!
		BackgroundThread.ensureGUI();

		log.d("scanDirectory(" + baseDir.getPathName() + ") " + (recursiveScan ? "recursive" : ""));
		Engine.ProgressControl engineProgress = engine.createProgress(
				recursiveScan ? 0 : R.string.progress_scanning,
				scanControl);
		ScanProgressTracker progress = new ScanProgressTracker(
				new ScanProgressTracker.Sink() {
					@Override
					public void setProgress(int value) {
						engineProgress.setProgress(value);
					}

					@Override
					public void hide() {
						engineProgress.hide();
					}
				});
		LibraryMetadataStore metadataStore =
				new CrdbLibraryMetadataStore(db);
		LibraryMetadataExtractor metadataExtractor =
				new EngineLibraryMetadataExtractor(engine);
		boolean fullDiscovery = recursiveScan || mHideEmptyDirs;

		listSubtreeBg(
				baseDir,
				fullDiscovery ? scanControl.getMaxDepth() : 2,
				scanControl, progress, fullDiscovery,
				initialUpdateCallback, () -> {
			if ( (!getDirScanEnabled() || baseDir.isScanned) && !recursiveScan || scanControl.isStopped() ) {
				progress.hide();
				readyListener.onComplete(scanControl);
				return;
			}
			new DirectoryMetadataIterator(
					metadataStore, metadataExtractor,
					baseDir, scanControl, progress,
					readyListener, recursiveScan)
					.scanNextDirectory();
		});
	}
	
	private boolean addRoot( String pathname, int resourceId, boolean listIt) {
		return addRoot( pathname, mActivity.getResources().getString(resourceId), listIt);
	}

	private FileInfo findRoot(String pathname) {
		String normalized = engine.getPathCorrector().normalizeIfPossible(pathname);
		for (int i = 0; i<mRoot.dirCount(); i++) {
			FileInfo dir = mRoot.getDir(i);
			if (normalized.equals(engine.getPathCorrector().normalizeIfPossible(dir.getPathName())))
				return dir;
		}
		return null;
	}

	private boolean addRoot( String pathname, String filename, boolean listIt) {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = pathname;
		dir.filename = filename;
		dir.title = filename;
		if (findRoot(pathname) != null) {
			log.w("skipping duplicate root " + pathname);
			return false; // exclude duplicates
		}
		if (listIt) {
			log.i("Checking FS root " + pathname);
			if (!dir.isReadableDirectory()) { // isWritableDirectory
				log.w("Skipping " + pathname + " - it's not a readable directory");
				return false;
			}
			if (!listDirectory(dir, true, false)) {
				log.w("Skipping " + pathname + " - listing failed");
				return false;
			}
			log.i("Adding FS root: " + pathname + "  " + filename);
		}
		mRoot.addDir(dir);
		dir.parent = mRoot;
		if (!listIt) {
			dir.isListed = true;
			dir.isScanned = true;
		}
		return true;
	}
	
	public FileInfo pathToFileInfo(String path) {
		if (path == null || path.length() == 0)
			return null;
		if (FileInfo.OPDS_LIST_TAG.equals(path))
			return createOPDSRoot();
		else if (FileInfo.SEARCH_SHORTCUT_TAG.equals(path))
			return createSearchRoot();
		else if (FileInfo.RECENT_DIR_TAG.equals(path))
			return getRecentDir();
		else if (FileInfo.GENRES_TAG.equals(path))
			return createGenresRoot();
		else if (FileInfo.AUTHORS_TAG.equals(path))
			return createAuthorsRoot();
		else if (FileInfo.TITLE_TAG.equals(path))
			return createTitleRoot();
		else if (FileInfo.SERIES_TAG.equals(path))
			return createSeriesRoot();
		else if (FileInfo.RATING_TAG.equals(path))
			return createBooksByRatingRoot();
		else if (FileInfo.STATE_READING_TAG.equals(path))
			return createBooksByStateReadingRoot();
		else if (FileInfo.STATE_TO_READ_TAG.equals(path))
			return createBooksByStateToReadRoot();
		else if (FileInfo.STATE_FINISHED_TAG.equals(path))
			return createBooksByStateFinishedRoot();
		else if (path.startsWith(FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX)) {
			OnlineStoreWrapper w = OnlineStorePluginManager.getPlugin(mActivity, path);
			if (w != null)
				return w.createRootDirectory();
			return null;
		} else if (path.startsWith(FileInfo.OPDS_DIR_PREFIX))
			return createOPDSDir(path);
		else
			return new FileInfo(path);
	}
	
	public FileInfo createOPDSRoot() {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.OPDS_LIST_TAG;
		dir.filename = mActivity.getString(R.string.mi_book_opds_root);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public static FileInfo createOnlineLibraryPluginItem(String packageName, String label) {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		if (packageName.startsWith(FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX))
			dir.pathname = packageName;
		else
			dir.pathname = FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX + packageName;
		dir.filename = label;
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	private void addRoot(FileInfo dir) {
		dir.parent = mRoot;
		mRoot.addDir(dir);
	}

	public FileInfo createRecentRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.RECENT_DIR_TAG;
		dir.filename = mActivity.getString(R.string.dir_recent_books);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addOPDSRoot() {
		addRoot(createOPDSRoot());
	}
	
	public FileInfo createSearchRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.SEARCH_SHORTCUT_TAG;
		dir.filename = mActivity.getString(R.string.mi_book_search);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addSearchRoot() {
		addRoot(createSearchRoot());
	}

	public FileInfo createGenresRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.GENRES_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_genre);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createAuthorsRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.AUTHORS_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_author);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addAuthorsRoot() {
		addRoot(createAuthorsRoot());
	}
	
	public FileInfo createOPDSDir(String path) {
		FileInfo opds = mRoot.findItemByPathName(FileInfo.OPDS_LIST_TAG);
		if (opds == null)
			return null;
		return opds.findItemByPathName(path);
	}
	
	public FileInfo createSeriesRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.SERIES_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_series);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByRatingRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.RATING_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_rating);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateToReadRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_TO_READ_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_state_to_read);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateReadingRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_READING_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_state_reading);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateFinishedRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_FINISHED_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_state_finished);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addSeriesRoot() {
		addRoot(createSeriesRoot());
	}
	
	public FileInfo createTitleRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.TITLE_TAG;
		dir.filename = mActivity.getString(R.string.folder_name_books_by_title);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addTitleRoot() {
		addRoot(createTitleRoot());
	}
	
	/**
	 * Lists all directories from root to directory of specified file, returns found directory.
	 * @param file
	 * @param root
	 * @return
	 */
	private FileInfo findParentInternal(FileInfo file, FileInfo root)	{
		if ( root==null || file==null || root.isRecentDir() )
			return null;
		if (!root.isRootDir() &&
				!(file.getPathName().startsWith(root.getPathName()) ||
						root.isOnSDCard() && file.getPathName().toLowerCase().startsWith( root.getPathName().toLowerCase() ) ) )
			return null;
		// to list all directories starting root dir
		if ( root.isDirectory && !root.isSpecialDir() )
				listDirectory(root);
		for ( int i=0; i<root.dirCount(); i++ ) {
			FileInfo found = findParentInternal( file, root.getDir(i));
			if ( found!=null )
				return found;
		}
		for ( int i=0; i<root.fileCount(); i++ ) {
			if ( root.getFile(i).getPathName().equals(file.getPathName()) ||
					root.isOnSDCard() && root.getFile(i).getPathName().equalsIgnoreCase(file.getPathName()) )
				return root;
			if ( root.getFile(i).getPathName().startsWith(file.getPathName() + "@/") ||
					root.isOnSDCard() && root.getFile(i).getPathName().toLowerCase().startsWith(file.getPathName().toLowerCase() + "@/") )
				return root;
		}
		return null;
	}
	
	public final static int MAX_DIR_LIST_TIME = 500; // 0.5 seconds
	
	/**
	 * Lists all directories from root to directory of specified file, returns found directory.
	 * @param file
	 * @param root
	 * @return
	 */
	public FileInfo findParent(FileInfo file, FileInfo root) {
		FileInfo parent = findParentInternal(file, root);
		if ( parent==null ) {
			autoAddRootForFile(new File(file.pathname) );
			parent = findParentInternal(file, root);
			if ( parent==null )
				parent = findParentInternal(file, new FileInfo(mActivity.getFilesDir()));
			if ( parent==null )
				parent = findParentInternal(file, new FileInfo(mActivity.getCacheDir()));
			if ( parent==null ) {
				L.e("Cannot find root directory for file " + file.pathname);
				return null;
			}
		}
		return parent;
	}
	
	public FileInfo findFileInTree(FileInfo f) {
		FileInfo parent = findParent(f, getRoot());
		if (parent == null)
			return null;
		FileInfo item = parent.findItemByPathName(f.getPathName());
		return item;
	}

	/**
	 * List directories in subtree (in background thread), remove empty branches (w/o books).
	 * @param root is directory to start with
	 * @param maxDepth is maximum depth
	 * @param scanControl is to stop long scanning
	 * @param readyCallback ready callback, can be null
	 */
	private void listSubtreeBg(FileInfo root, int maxDepth,
			ScanControl scanControl, ScanProgressTracker progress,
			boolean enforceDepthLimit, Runnable initialUpdateCallback,
			Runnable readyCallback) {
		BackgroundThread.instance().postBackground(() -> {
			// make a copy to scan in background
			FileInfo dir = copyDirectorySnapshot(root);
			dir.parent = root.parent;
			scanControl.startRootDirectory();
			boolean rootListed = listDirectory(
					dir, true, true,
					!dir.isSpecialDir() && !dir.isArchive,
					scanControl);
			progress.setDiscoveryProgress(
					scanControl.discoveryProgress());
			boolean initialUpdatePosted =
					rootListed && initialUpdateCallback != null;
			if (initialUpdatePosted) {
				FileInfo initialSnapshot =
						copyDirectorySnapshot(dir);
				BackgroundThread.instance().postGUI(() -> {
					root.setItems(initialSnapshot);
					root.scanFingerprint =
							initialSnapshot.scanFingerprint;
					onDirectoryContentChanged(root);
					initialUpdateCallback.run();
				});
			}
			if (rootListed && !scanControl.isStopped())
				listSubtreeBg_impl(
						dir, maxDepth, scanControl, progress,
						true, enforceDepthLimit);
			BackgroundThread.instance().postGUI(() -> {
				// transfer scanned items from background copy to update in GUI
				root.setItems(dir);
				root.scanFingerprint = dir.scanFingerprint;
				onDirectoryContentChanged(root);
				if (!initialUpdatePosted
						&& initialUpdateCallback != null)
					initialUpdateCallback.run();
				if (null != readyCallback) {
					readyCallback.run();
				}
			});
		});
	}

	private FileInfo copyDirectorySnapshot(FileInfo directory) {
		FileInfo copy = new FileInfo(directory);
		copy.parent = directory.parent;
		for (int i = 0; i < directory.fileCount(); i++) {
			FileInfo file = new FileInfo(directory.getFile(i));
			file.parent = copy;
			copy.addFile(file);
		}
		for (int i = 0; i < directory.dirCount(); i++) {
			FileInfo child = new FileInfo(directory.getDir(i));
			child.parent = copy;
			copy.addDir(child);
		}
		return copy;
	}

	private boolean listSubtreeBg_impl(FileInfo dir, int maxDepth,
			ScanControl scanControl, ScanProgressTracker progress,
			boolean rootAlreadyDiscovered,
			boolean enforceDepthLimit) {
		BackgroundThread.ensureBackground();
		return IterativeScanTraversal.traverse(
				dir, maxDepth, scanControl::isStopped,
				enforceDepthLimit
						? scanControl::stopAtDepthLimit
						: () -> {
						},
				new IterativeScanTraversal.Adapter<FileInfo>() {
					@Override
					public boolean discover(FileInfo directory) {
						if (rootAlreadyDiscovered
								&& directory == dir)
							return true;
						return listDirectory(
								directory, true, true,
								!directory.isSpecialDir()
										&& !directory.isArchive,
								scanControl);
					}

					@Override
					public int childCount(FileInfo directory) {
						return directory.dirCount();
					}

					@Override
					public FileInfo childAt(
							FileInfo directory, int index) {
						return directory.getDir(index);
					}

					@Override
					public void onCompleted(
							FileInfo directory, boolean fullDepth) {
						if (fullDepth && mHideEmptyDirs)
							directory.removeEmptyDirs();
						scanControl.completeDirectory();
						progress.setDiscoveryProgress(
								scanControl.discoveryProgress());
					}
				});
	}

	public FileInfo setSearchResults( FileInfo[] results ) {
		FileInfo existingResults = null;
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo dir = mRoot.getDir(i);
			if ( dir.isSearchDir() ) {
				existingResults = dir;
				dir.clear();
				break;
			}
		}
		if ( existingResults==null ) {
			FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = FileInfo.SEARCH_RESULT_DIR_TAG;
			dir.filename = mActivity.getResources().getString(R.string.dir_search_results);
			dir.parent = mRoot;
			dir.isListed = true;
			dir.isScanned = true;
			mRoot.addDir(dir);
			existingResults = dir;
		}
		for ( FileInfo item : results )
			existingResults.addFile(item);
		return existingResults;
	}
	
	public void initRoots(Map<String, String> fsRoots, Map<String, String> privateDirs) {
		Log.d("cr3", "Scanner.initRoots(" + fsRoots + ")");
		mRoot.clear();
		// create recent books dir
		addRoot( FileInfo.RECENT_DIR_TAG, R.string.dir_recent_books, false);

		// create system dirs
		for (Map.Entry<String, String> entry : fsRoots.entrySet())
			addRoot( entry.getKey(), entry.getValue(), true);

		// App private dirs
		for (Map.Entry<String, String> entry : privateDirs.entrySet())
			addRoot( entry.getKey(), entry.getValue(), true);

		// create OPDS dir
		addOPDSRoot();
		
		// create search dir
		addSearchRoot();
		
		// create books by author root
		addAuthorsRoot();
		// create books by series root
		addSeriesRoot();
		// create books by title root
		addTitleRoot();
	}

	public boolean autoAddRootForFile( File f ) {
		File p = f.getParentFile();
		while ( p!=null ) {
			if ( p.getParentFile()==null || p.getParentFile().getParentFile()==null )
				break;
			p = p.getParentFile();
		}
		if ( p!=null ) {
			L.i("Found possible mount point " + p.getAbsolutePath());
			return addRoot(p.getAbsolutePath(), p.getAbsolutePath(), true);
		}
		return false;
	}
	
//	public boolean scan()
//	{
//		L.i("Started scanning");
//		long start = System.currentTimeMillis();
//		mFileList.clear();
//		mFilesForParsing.clear();
//		mRoot.clear();
//		// create recent books dir
//		FileInfo recentDir = new FileInfo();
//		recentDir.isDirectory = true;
//		recentDir.pathname = "@recent";
//		recentDir.filename = "Recent Books";
//		mRoot.addDir(recentDir);
//		recentDir.parent = mRoot;
//		// scan directories
//		lastPercent = -1;
//		lastProgressUpdate = System.currentTimeMillis() - 500;
//		boolean res = scanDirectories( mRoot );
//		// process found files
//		lookupDB();
//		parseBookProperties();
//		updateProgress(9999);
//		L.i("Finished scanning (" + (System.currentTimeMillis()-start)+ " ms)");
//		return res;
//	}
	
	
	public ArrayList<FileInfo> getLibraryItems() {
		ArrayList<FileInfo> result = new ArrayList<FileInfo>();
		result.add(pathToFileInfo(FileInfo.SEARCH_SHORTCUT_TAG));
		result.add(pathToFileInfo(FileInfo.GENRES_TAG));
		result.add(pathToFileInfo(FileInfo.AUTHORS_TAG));
		result.add(pathToFileInfo(FileInfo.TITLE_TAG));
		result.add(pathToFileInfo(FileInfo.SERIES_TAG));
		result.add(pathToFileInfo(FileInfo.RATING_TAG));
		result.add(pathToFileInfo(FileInfo.STATE_TO_READ_TAG));
		result.add(pathToFileInfo(FileInfo.STATE_READING_TAG));
		result.add(pathToFileInfo(FileInfo.STATE_FINISHED_TAG));
		return result;
	}
	
	public FileInfo getDownloadDirectory() {
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo item = mRoot.getDir(i);
			if (!item.isWritableDirectory())
				continue;
			if ( !item.isSpecialDir() && !item.isArchive ) {
				if (!item.isListed)
					listDirectory(item, false, false);
				FileInfo books = item.findItemByPathName(item.pathname + "/Books");
				if (books == null)
					books = item.findItemByPathName(item.pathname + "/books");
				if (books != null && books.exists())
					return books;
				File dir = new File(item.getPathName());
				if (dir.isDirectory()) {
					if (!dir.canWrite())
						Log.w("cr3", "Directory " + dir + " is readonly");
					File f = new File( dir, "Books" );
					if ( f.mkdirs() || f.isDirectory() ) {
						books = new FileInfo(f);
						books.parent = item;
						item.addDir(books);
						books.isScanned = true;
						books.isListed = true;
						return books;
					}
				}
			}
		}
		File fd = mActivity.getFilesDir();
		File downloadDir = new File(fd, "downloads");
		if (downloadDir.isDirectory() || downloadDir.mkdirs()) {
			Log.d("cr3", "download dir: " + downloadDir);
			FileInfo books = null;
			books = new FileInfo(downloadDir);
			//books.parent = item;
			//item.addDir(books);
			books.isScanned = true;
			books.isListed = true;
			return books;
		}
		try {
			throw new Exception("download directory not found and cannot be created");
		} catch (Exception e) {
			Log.e("cr3", "download directory is not found!!!", e);
		}
		return null;
	}

	public FileInfo getSharedDownloadDirectory() {
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo item = mRoot.getDir(i);
			if (!item.isWritableDirectory())
				continue;
			if ( !item.isSpecialDir() && !item.isArchive ) {
				if (!item.isListed)
					listDirectory(item, false, false);
				FileInfo download = item.findItemByPathName(item.pathname + "/Download");
				if (download == null)
					download = item.findItemByPathName(item.pathname + "/download");
				if (download != null && download.exists())
					return download;
			}
		}
		Log.e("cr3", "shared download directory is not found!!!");
		return null;
	}

	public boolean isValidFolder(FileInfo info){
        File dir = new File( info.pathname );
        return dir.isDirectory();
    }

	public FileInfo getRoot() 
	{
		return mRoot;
	}

	public FileInfo getOPDSRoot() 
	{
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			if ( mRoot.getDir(i).isOPDSRoot() )
				return mRoot.getDir(i);
		}
		L.w("OPDS root directory not found!");
		return null;
	}
	
	public FileInfo getRecentDir() 
	{
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			if ( mRoot.getDir(i).isRecentDir())
				return mRoot.getDir(i);
		}
		L.w("Recent books directory not found!");
		return null;
	}
	
	public Scanner( BaseActivity coolReader, Engine engine )
	{
		this.engine = engine;
		this.mActivity = coolReader;
		mRoot = new FileInfo();
		mRoot.path = FileInfo.ROOT_DIR_TAG;	
		mRoot.filename = "File Manager";
		mRoot.pathname = FileInfo.ROOT_DIR_TAG;
		mRoot.isListed = true;
		mRoot.isScanned = true;
		mRoot.isDirectory = true;
	}

	private final Engine engine;
	private final BaseActivity mActivity;
}
