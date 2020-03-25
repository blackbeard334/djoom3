package neo.framework;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.fopenOptions;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.BuildDefines.ID_ALLOW_D3XP;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.BuildDefines.ID_FAKE_PURE;
import static neo.framework.BuildDefines.ID_PURE_ALLOWDDS;
import static neo.framework.BuildDefines.WIN32;
import static neo.framework.BuildDefines._DEBUG;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.com_developer;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MAPDEF;
import static neo.framework.DemoChecksum.DEMO_PAK_CHECKSUM;
import static neo.framework.EventLoop.eventLoop;
import static neo.framework.FileSystem_h.binaryStatus_t.BINARY_NO;
import static neo.framework.FileSystem_h.binaryStatus_t.BINARY_UNKNOWN;
import static neo.framework.FileSystem_h.binaryStatus_t.BINARY_YES;
import static neo.framework.FileSystem_h.dlStatus_t.DL_ABORTING;
import static neo.framework.FileSystem_h.dlType_t.DLTYPE_FILE;
import static neo.framework.FileSystem_h.findFile_t.FIND_ADDON;
import static neo.framework.FileSystem_h.findFile_t.FIND_NO;
import static neo.framework.FileSystem_h.findFile_t.FIND_YES;
import static neo.framework.FileSystem_h.fsMode_t.FS_APPEND;
import static neo.framework.FileSystem_h.fsMode_t.FS_READ;
import static neo.framework.FileSystem_h.fsMode_t.FS_WRITE;
import static neo.framework.FileSystem_h.fsPureReply_t.PURE_MISSING;
import static neo.framework.FileSystem_h.fsPureReply_t.PURE_NODLL;
import static neo.framework.FileSystem_h.fsPureReply_t.PURE_OK;
import static neo.framework.FileSystem_h.fsPureReply_t.PURE_RESTART;
import static neo.framework.FileSystem_h.pureStatus_t.PURE_ALWAYS;
import static neo.framework.FileSystem_h.pureStatus_t.PURE_NEUTRAL;
import static neo.framework.FileSystem_h.pureStatus_t.PURE_NEVER;
import static neo.framework.FileSystem_h.pureStatus_t.PURE_UNKNOWN;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_END;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import static neo.framework.Licensee.BASE_GAMEDIR;
import static neo.framework.Licensee.CDKEY_FILE;
import static neo.framework.Licensee.XPKEY_FILE;
import static neo.framework.Session.session;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Lib.idLib.sys;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_STRING;
import static neo.idlib.containers.StrList.idStrList.idStrListSortPaths;
import static neo.idlib.hashing.MD4.MD4_BlockChecksum;
import static neo.sys.sys_public.BUILD_OS_ID;
import static neo.sys.sys_public.PATHSEPERATOR_CHAR;
import static neo.sys.sys_public.PATHSEPERATOR_STR;
import static neo.sys.sys_public.Sys_ListFiles;
import static neo.sys.win_main.Sys_DefaultBasePath;
import static neo.sys.win_main.Sys_DefaultCDPath;
import static neo.sys.win_main.Sys_DefaultSavePath;
import static neo.sys.win_main.Sys_EXEPath;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_FileTimeStamp;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_main.Sys_Mkdir;
import static neo.sys.win_main.Sys_TriggerEvent;
import static neo.sys.win_main.remove;
import static neo.sys.win_main.tmpfile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import neo.TempDump.CPP_class;
import neo.TempDump.CPP_class.Bool;
import neo.TempDump.CPP_class.Char;
import neo.TempDump.CPP_class.Pointer;
import neo.TempDump.TODO_Exception;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclManager.idDecl;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_InZip;
import neo.framework.File_h.idFile_Permanent;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.sys.sys_public.xthreadInfo;
import neo.sys.sys_public.xthread_t;

/**
 *
 */
public class FileSystem_h {
    /*
     ===============================================================================

     File System

     No stdio calls should be used by any part of the game, because of all sorts
     of directory and separator char issues. Throughout the game a forward slash
     should be used as a separator. The file system takes care of the conversion
     to an OS specific separator. The file system treats all file and directory
     names as case insensitive.

     The following cvars store paths used by the file system:

     "fs_basepath"		path to local install, read-only
     "fs_savepath"		path to config, save game, etc. files, read & write
     "fs_cdpath"			path to cd, read-only
     "fs_devpath"		path to files created during development, read & write

     The base path for file saving can be set to "fs_savepath" or "fs_devpath".

     ===============================================================================
     */

    private static idFileSystemLocal fileSystemLocal = new idFileSystemLocal();
    public static  idFileSystem      fileSystem      = fileSystemLocal;//TODO:make a [] pointer of this??

    public static final int FILE_NOT_FOUND_TIMESTAMP = 0xFFFFFFFF;
    public static final int MAX_PURE_PAKS            = 128;
    public static final int MAX_OSPATH               = 256;

    // modes for OpenFileByMode. used as bit mask internally
    public enum fsMode_t {

        FS_READ,
        FS_WRITE,
        FS_APPEND
    }

    public enum fsPureReply_t {

        PURE_OK,      // we are good to connect as-is
        PURE_RESTART, // restart required
        PURE_MISSING, // pak files missing on the client
        PURE_NODLL	  // no DLL could be extracted
    }

    public enum dlType_t {

        DLTYPE_URL,
        DLTYPE_FILE
    }

    public enum dlStatus_t {

        DL_WAIT,        // waiting in the list for beginning of the download
        DL_INPROGRESS,  // in progress
        DL_DONE,        // download completed, success
        DL_ABORTING,    // this one can be set during a download, it will force the next progress callback to abort - then will go to DL_FAILED
        DL_FAILED
    }

    public enum dlMime_t {

        FILE_EXEC,
        FILE_OPEN
    }

    public enum findFile_t {

        FIND_NO,
        FIND_YES,
        FIND_ADDON
    }

    public static class urlDownload_s {

        public static final transient int SIZE
                = idStr.SIZE
                + (Char.SIZE * MAX_STRING_CHARS)
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + CPP_class.Enum.SIZE;

        public idStr url = new idStr();
        public String     dlerror;//[ MAX_STRING_CHARS ];
        public int        dltotal;
        public int        dlnow;
        public int        dlstatus;
        public dlStatus_t status;

        public urlDownload_s() {
        }

        /**
         * copy constructor
         *
         * @param url
         */
        private urlDownload_s(urlDownload_s url) {
            this.url = new idStr(url.url);
            this.dlerror = url.dlerror;
            this.dlnow = url.dlnow;
            this.dlstatus = url.dlstatus;
            this.status = url.status;
        }
    }

    public static class fileDownload_s {

        public static final transient int SIZE
                = Integer.SIZE
                + Integer.SIZE
                + Pointer.SIZE;//void * buffer

        public int        position;
        public int        length;
        public ByteBuffer buffer;

        public fileDownload_s() {
        }

        /**
         * copy constructor
         *
         * @param file
         */
        private fileDownload_s(fileDownload_s file) {
            this.position = file.position;
            this.length = file.length;
            this.buffer = file.buffer.duplicate();
        }
    }

    public static class backgroundDownload_s {

        public static final transient int SIZE
                = Pointer.SIZE//backgroundDownload_s next
                + CPP_class.Enum.SIZE
                + Pointer.SIZE//idFile f
                + fileDownload_s.SIZE
                + urlDownload_s.SIZE
                + Bool.SIZE;//TODO:volatile?

        public backgroundDownload_s next;    // set by the fileSystem
        public dlType_t             opcode;
        public idFile               f;
        public fileDownload_s       file;
        public urlDownload_s url = new urlDownload_s();
        public volatile boolean completed;

        public backgroundDownload_s() {
        }

        /**
         * cop constructor
         *
         * @param bgl
         */
        public backgroundDownload_s(backgroundDownload_s bgl) {
            this.next = bgl.next;//pointer
            this.opcode = bgl.opcode;
            this.f = bgl.f;//pointer
            this.file = new fileDownload_s(bgl.file);
            this.url = new urlDownload_s(bgl.url);
            this.completed = bgl.completed;
        }
    }

    // file list for directory listings
    public static class idFileList {
//	friend class idFileSystemLocal;
//

        private idStr     basePath;
        private final idStrList list;
        //
        //

        public idFileList() {
            this.list = new idStrList();
            this.basePath = new idStr();
        }

        public String GetBasePath() {
            return this.basePath.toString();
        }

        public int GetNumFiles() {
            return this.list.Num();
        }

        public String GetFile(int index) {
            return this.list.oGet(index).toString();
        }

        public idStrList GetList() {
            return this.list;
        }
    }

    // mod list
    static class idModList {

        private final idStrList mods;
        private final idStrList descriptions;
        //
        //

        idModList() {
            this.mods = new idStrList();
            this.descriptions = new idStrList();
        }

        public int GetNumMods() {
            return this.mods.Num();
        }

        public String GetMod(int index) {
            return this.mods.oGet(index).toString();
        }

        public String GetDescription(int index) {
            return this.descriptions.oGet(index).toString();
        }
    }

    public static abstract class idFileSystem {
//	public abstract					~idFileSystem() {}

        // Initializes the file system.
        public abstract void Init();

        // Restarts the file system.
        public abstract void Restart();

        // Shutdown the file system.
        public abstract void Shutdown(boolean reloading);

        // Returns true if the file system is initialized.
        public abstract boolean IsInitialized();

        // Returns true if we are doing an fs_copyfiles.
        public abstract boolean PerformingCopyFiles();

        // Returns a list of mods found along with descriptions
        // 'mods' contains the directory names to be passed to fs_game
        // 'descriptions' contains a free form string to be used in the UI
        public abstract idModList ListMods();

        // Frees the given mod list
        public abstract void FreeModList(idModList modList);

        // Lists files with the given extension in the given directory.
        // Directory should not have either a leading or trailing '/'
        // The returned files will not include any directories or '/' unless fullRelativePath is set.
        // The extension must include a leading dot and may not contain wildcards.
        // If extension is "/", only subdirectories will be returned.
        public abstract idFileList ListFiles(final String relativePath, final String extension);

        public abstract idFileList ListFiles(final String relativePath, final String extension, boolean sort);

        public abstract idFileList ListFiles(final String relativePath, final String extension, boolean sort, boolean fullRelativePath);

        public abstract idFileList ListFiles(final String relativePath, final String extension, boolean sort, boolean fullRelativePath, final String gamedir);

        // Lists files in the given directory and all subdirectories with the given extension.
        // Directory should not have either a leading or trailing '/'
        // The returned files include a full relative path.
        // The extension must include a leading dot and may not contain wildcards.
        public abstract idFileList ListFilesTree(final String relativePath, final String extension);

        public abstract idFileList ListFilesTree(final String relativePath, final String extension, boolean sort);

        public abstract idFileList ListFilesTree(final String relativePath, final String extension, boolean sort, final String gamedir);

        // Frees the given file list.
        public abstract void FreeFileList(idFileList fileList);

        // Converts a relative path to a full OS path.
        public abstract String OSPathToRelativePath(final String OSPath);

        // Converts a full OS path to a relative path.
        public abstract String RelativePathToOSPath(final String relativePath, final String basePath/* = "fs_devpath"*/);

        public String RelativePathToOSPath(final String relativePath) {//TODO:return Path
            return RelativePathToOSPath(relativePath, "fs_devpath");
        }

        public String RelativePathToOSPath(final idStr relativePath) {
            return RelativePathToOSPath(relativePath.toString());
        }

        // Builds a full OS path from the given components.
        public abstract String BuildOSPath(final String base, final String game, final String relativePath);

        // Creates the given OS path for as far as it doesn't exist already.
        public abstract void CreateOSPath(final String OSPath);

        public void CreateOSPath(final idStr OSPath) {
            CreateOSPath(OSPath.toString());
        }

        // Returns true if a file is in a pak file.
        public abstract boolean FileIsInPAK(final String relativePath);

        // Returns a space separated string containing the checksums of all referenced pak files.
        // will call SetPureServerChecksums internally to restrict itself
        public abstract void UpdatePureServerChecksums();

        // setup the mapping of OS -> game pak checksum
        public abstract boolean UpdateGamePakChecksums();

        // 0-terminated list of pak checksums
        // if pureChecksums[ 0 ] == 0, all data sources will be allowed
        // otherwise, only pak files that match one of the checksums will be checked for files
        // with the sole exception of .cfg files.
        // the function tries to configure pure mode from the paks already referenced and this new list
        // it returns wether the switch was successfull, and sets the missing checksums
        // the process is verbosive when fs_debug 1
        public abstract fsPureReply_t SetPureServerChecksums(int pureChecksums[], int gamePakChecksum, int missingChecksums[], int[] missingGamePakChecksum);

        // fills a 0-terminated list of pak checksums for a client
        // if OS is -1, give the current game pak checksum. if >= 0, lookup the game pak table (server only)
        public abstract void GetPureServerChecksums(int checksums[], int OS, int[] gamePakChecksum);

        // before doing a restart, force the pure list and the search order
        // if the given checksum list can't be completely processed and set, will error out
        public abstract void SetRestartChecksums(int pureChecksums[], int gamePakChecksum);

        // equivalent to calling SetPureServerChecksums with an empty list
        public abstract void ClearPureChecksums();

        // get a mask of supported OSes. if not pure, returns -1
        public abstract int GetOSMask();

        // Reads a complete file.
        // Returns the length of the file, or -1 on failure.
        // A null buffer will just return the file length without loading.
        // A null timestamp will be ignored.
        // As a quick check for existance. -1 length == not present.
        // A 0 byte will always be appended at the end, so string ops are safe.
        // The buffer should be considered read-only, because it may be cached for other uses.
        public abstract int ReadFile(final String relativePath, ByteBuffer[] buffer, long[] timestamp);

        public abstract int ReadFile(final String relativePath, ByteBuffer[] buffer);

        public int ReadFile(idStr name, ByteBuffer[] buffer, long[] timeStamp) {
            return ReadFile(name.toString(), buffer, timeStamp);
        }

        public int ReadFile(idStr name, ByteBuffer[] buffer) {
            return ReadFile(name.toString(), buffer);
        }

        // Frees the memory allocated by ReadFile.
        @Deprecated
        public abstract void FreeFile(Object[] buffer);

        // Writes a complete file, will create any needed subdirectories.
        // Returns the length of the file, or -1 on failure.
        public abstract int WriteFile(final String relativePath, final ByteBuffer buffer, int size/*, final String basePath = "fs_savepath" */);

        public abstract int WriteFile(final String relativePath, final ByteBuffer buffer, int size, final String basePath/* = "fs_savepath" */);

        // Removes the given file.
        public abstract void RemoveFile(final String relativePath);

        public abstract idFile OpenFileRead(final String relativePath);

        public abstract idFile OpenFileRead(final String relativePath, boolean allowCopyFiles);

        // Opens a file for reading.
        public abstract idFile OpenFileRead(final String relativePath, boolean allowCopyFiles, final String gamedir);

        public idFile OpenFileWrite(final String relativePath) {
            return OpenFileWrite(relativePath, "fs_savepath");
        }

        // Opens a file for writing, will create any needed subdirectories.
        public abstract idFile OpenFileWrite(final String relativePath, final String basePath);

        // Opens a file for writing at the end.
        public abstract idFile OpenFileAppend(final String filename, boolean sync, final String basePath/* = "fs_basepath"*/);

        public abstract idFile OpenFileAppend(final String filename, boolean sync/* = "fs_basepath"*/);

        public abstract idFile OpenFileAppend(final String filename/*, boolean sync*/);

        // Opens a file for reading, writing, or appending depending on the value of mode.
        public abstract idFile OpenFileByMode(final String relativePath, fsMode_t mode);

        // Opens a file for reading from a full OS path.
        public abstract idFile OpenExplicitFileRead(final String OSPath);

        // Opens a file for writing to a full OS path.
        public abstract idFile OpenExplicitFileWrite(final String OSPath);

        // Closes a file.
        public abstract void CloseFile(idFile f);

        // Returns immediately, performing the read from a background thread.
        public abstract void BackgroundDownload(backgroundDownload_s bgl);

        // resets the bytes read counter
        public abstract void ResetReadCount();

        // retrieves the current read count
        public abstract int GetReadCount();

        // adds to the read count
        public abstract void AddToReadCount(int c);

        // look for a dynamic module
        public abstract void FindDLL(final String basename, char dllPath[], boolean updateChecksum);

        // case sensitive filesystems use an internal directory cache
        // the cache is cleared when calling OpenFileWrite and RemoveFile
        // in some cases you may need to use this directly
        public abstract void ClearDirCache();

        // is D3XP installed? even if not running it atm
        public abstract boolean HasD3XP();

        // are we using D3XP content ( through a real d3xp run or through a double mod )
        public abstract boolean RunningD3XP();

        // don't use for large copies - allocates a single memory block for the copy
        public abstract void CopyFile(final String fromOSPath, final String toOSPath);

        // lookup a relative path, return the size or 0 if not found
        public abstract int ValidateDownloadPakForChecksum(int checksum, char path[], boolean isGamePak);

        public abstract idFile MakeTemporaryFile();

        // make downloaded pak files known so pure negociation works next time
        public abstract int AddZipFile(final String path);

        // look for a file in the loaded paks or the addon paks
        // if the file is found in addons, FS's internal structures are ready for a reloadEngine
        public abstract findFile_t FindFile(final String path, boolean scheduleAddons);

        // get map/addon decls and take into account addon paks that are not on the search list
        // the decl 'name' is in the "path" entry of the dict
        public abstract int GetNumMaps();

        public abstract idDict GetMapDecl(int i);

        public abstract void FindMapScreenshot(final String path, String[] buf, int len);

        // ignore case and seperator char distinctions
        public abstract boolean FilenameCompare(final String s1, final String s2);

        public boolean FilenameCompare(final idStr s1, final idStr s2) {
            return FilenameCompare(s1.toString(), s2.toString());
        }

    }
    /*
     =============================================================================

     DOOM FILESYSTEM

     All of Doom's data access is through a hierarchical file system, but the contents of
     the file system can be transparently merged from several sources.

     A "relativePath" is a reference to game file data, which must include a terminating zero.
     "..", "\\", and ":" are explicitly illegal in qpaths to prevent any references
     outside the Doom directory system.

     The "base path" is the path to the directory holding all the game directories and
     usually the executable. It defaults to the current directory, but can be overridden
     with "+set fs_basepath c:\doom" on the command line. The base path cannot be modified
     at all after startup.

     The "save path" is the path to the directory where game files will be saved. It defaults
     to the base path, but can be overridden with a "+set fs_savepath c:\doom" on the
     command line. Any files that are created during the game (demos, screenshots, etc.) will
     be created reletive to the save path.

     The "cd path" is the path to an alternate hierarchy that will be searched if a file
     is not located in the base path. A user can do a partial install that copies some
     data to a base path created on their hard drive and leave the rest on the cd. It defaults
     to the current directory, but it can be overridden with "+set fs_cdpath g:\doom" on the
     command line.

     The "dev path" is the path to an alternate hierarchy where the editors and tools used
     during development (Radiant, AF editor, dmap, runAAS) will write files to. It defaults to
     the cd path, but can be overridden with a "+set fs_devpath c:\doom" on the command line.

     If a user runs the game directly from a CD, the base path would be on the CD. This
     should still function correctly, but all file writes will fail (harmlessly).

     The "base game" is the directory under the paths where data comes from by default, and
     can be either "base" or "demo".

     The "current game" may be the same as the base game, or it may be the name of another
     directory under the paths that should be searched for files before looking in the base
     game. The game directory is set with "+set fs_game myaddon" on the command line. This is
     the basis for addons.

     No other directories outside of the base game and current game will ever be referenced by
     filesystem functions.

     To save disk space and speed up file loading, directory trees can be collapsed into zip
     files. The files use a ".pk4" extension to prevent users from unzipping them accidentally,
     but otherwise they are simply normal zip files. A game directory can have multiple zip
     files of the form "pak0.pk4", "pak1.pk4", etc. Zip files are searched in decending order
     from the highest number to the lowest, and will always take precedence over the filesystem.
     This allows a pk4 distributed as a patch to override all existing data.

     Because we will have updated executables freely available online, there is no point to
     trying to restrict demo / oem versions of the game with code changes. Demo / oem versions
     should be exactly the same executables as release versions, but with different data that
     automatically restricts where game media can come from to prevent add-ons from working.

     After the paths are initialized, Doom will look for the product.txt file. If not found
     and verified, the game will run in restricted mode. In restricted mode, only files
     contained in demo/pak0.pk4 will be available for loading, and only if the zip header is
     verified to not have been modified. A single exception is made for DoomConfig.cfg. Files
     can still be written out in restricted mode, so screenshots and demos are allowed.
     Restricted mode can be tested by setting "+set fs_restrict 1" on the command line, even
     if there is a valid product.txt under the basepath or cdpath.

     If the "fs_copyfiles" cvar is set to 1, then every time a file is sourced from the cd
     path, it will be copied over to the save path. This is a development aid to help build
     test releases and to copy working sets of files.

     If the "fs_copyfiles" cvar is set to 2, any file found in fs_cdpath that is newer than
     it's fs_savepath version will be copied to fs_savepath (in addition to the fs_copyfiles 1
     behaviour).

     If the "fs_copyfiles" cvar is set to 3, files from both basepath and cdpath will be copied
     over to the save path. This is useful when copying working sets of files mainly from base
     path with an additional cd path (which can be a slower network drive for instance).

     If the "fs_copyfiles" cvar is set to 4, files that exist in the cd path but NOT the base path
     will be copied to the save path

     NOTE: fs_copyfiles and case sensitivity. On fs_caseSensitiveOS 0 filesystems ( win32 ), the
     copied files may change casing when copied over.

     The relative path "sound/newstuff/test.wav" would be searched for in the following places:

     for save path, dev path, base path, cd path:
     for current game, base game:
     search directory
     search zip files

     downloaded files, to be written to save path + current game's directory

     The filesystem can be safely shutdown and reinitialized with different
     basedir / cddir / game combinations, but all other subsystems that rely on it
     (sound, video) must also be forced to restart.


     "fs_caseSensitiveOS":
     This cvar is set on operating systems that use case sensitive filesystems (Linux and OSX)
     It is a common situation to have the media reference filenames, whereas the file on disc
     only matches in a case-insensitive way. When "fs_caseSensitiveOS" is set, the filesystem
     will always do a case insensitive search.
     IMPORTANT: This only applies to files, and not to directories. There is no case-insensitive
     matching of directories. All directory names should be lowercase, when "com_developer" is 1,
     the filesystem will warn when it catches bad directory situations (regardless of the
     "fs_caseSensitiveOS" setting)
     When bad casing in directories happen and "fs_caseSensitiveOS" is set, BuildOSPath will
     attempt to correct the situation by forcing the path to lowercase. This assumes the media
     is stored all lowercase.

     "additional mod path search":
     fs_game_base can be used to set an additional search path
     in search order, fs_game, fs_game_base, BASEGAME
     for instance to base a mod of D3 + D3XP assets, fs_game mymod, fs_game_base d3xp

     =============================================================================
     */
    // define to fix special-cases for GetPackStatus so that files that shipped in 
    // the wrong place for Doom 3 don't break pure servers.
    static final boolean DOOM3_PURE_SPECIAL_CASES = true;

    static abstract class pureExclusionFunc_t {

        public abstract boolean run(final pureExclusion_s excl, int l, final idStr name);
    }

    static class pureExclusion_s {

        int nameLen;
        int extLen;
        String name;
        String ext;
        pureExclusionFunc_t func;

        public pureExclusion_s(int nameLen, int extLen, String name, String ext, pureExclusionFunc_t func) {
            this.nameLen = nameLen;
            this.extLen = extLen;
            this.name = name;
            this.ext = ext;
            this.func = func;
        }
    }

    static class excludeExtension extends pureExclusionFunc_t {

        private static final pureExclusionFunc_t instance = new excludeExtension();

        public static pureExclusionFunc_t getInstance() {
            return instance;
        }

        @Override
        public boolean run(pureExclusion_s excl, int l, idStr name) {
            if ((l > excl.extLen) && (0 == idStr.Icmp(name.toString().substring(l - excl.extLen), excl.ext))) {
                return true;
            }
            return false;
        }
    }

    static class excludePathPrefixAndExtension extends pureExclusionFunc_t {

        private static final pureExclusionFunc_t instance = new excludePathPrefixAndExtension();

        public static pureExclusionFunc_t getInstance() {
            return instance;
        }

        @Override
        public boolean run(pureExclusion_s excl, int l, idStr name) {
            if ((l > excl.extLen) && (0 == idStr.Icmp(name.toString().substring(l - excl.extLen), excl.ext)) && (0 == name.IcmpPrefixPath(excl.name))) {
                return true;
            }
            return false;
        }
    }

    static class excludeFullName extends pureExclusionFunc_t {

        private static final pureExclusionFunc_t instance = new excludeFullName();

        public static pureExclusionFunc_t getInstance() {
            return instance;
        }

        @Override
        public boolean run(pureExclusion_s excl, int l, idStr name) {
            if ((l == excl.nameLen) && (0 == name.Icmp(excl.name))) {
                return true;
            }
            return false;
        }
    }
    static final pureExclusion_s[] pureExclusions1 = {
        new pureExclusion_s(0, 0, null, "/", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, "\\", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".pda", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".gui", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".pd", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".lang", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/VO", ".ogg", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/VO", ".wav", excludePathPrefixAndExtension.getInstance()),
        // add any special-case files or paths for pure servers here
        new pureExclusion_s(0, 0, "sound/ed/marscity/vo_intro_cutscene.ogg", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "sound/weapons/soulcube/energize_01.ogg", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "sound/xian/creepy/vocal_fx", ".ogg", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/xian/creepy/vocal_fx", ".wav", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/feedback", ".ogg", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/feedback", ".wav", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "guis/assets/mainmenu/chnote.tga", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "sound/levels/alphalabs2/uac_better_place.ogg", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "textures/bigchars.tga", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "dds/textures/bigchars.dds", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, "fonts", ".tga", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "dds/fonts", ".dds", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "default.cfg", null, excludeFullName.getInstance()),
        // russian zpak001.pk4
        new pureExclusion_s(0, 0, "fonts", ".dat", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "guis/temp.guied", null, excludeFullName.getInstance()),
        new pureExclusion_s(0, 0, null, null, null)
    };
    static final pureExclusion_s[] pureExclusions2 = {
        new pureExclusion_s(0, 0, null, "/", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, "\\", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".pda", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".gui", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".pd", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, null, ".lang", excludeExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/VO", ".ogg", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, "sound/VO", ".wav", excludePathPrefixAndExtension.getInstance()),
        new pureExclusion_s(0, 0, null, null, null)
    };
    static final pureExclusion_s[] pureExclusions = DOOM3_PURE_SPECIAL_CASES ? pureExclusions1 : pureExclusions2;

    // ensures that lengths for pure exclusions are correct
    class idInitExclusions {

        public idInitExclusions() {
            for (int i = 0; pureExclusions[i].func != null; i++) {
                if (pureExclusions[i].name != null) {
                    pureExclusions[i].nameLen = pureExclusions[i].name.length();
                }
                if (pureExclusions[i].ext != null) {
                    pureExclusions[i].extLen = pureExclusions[i].ext.length();
                }
            }
        }
    }
    static idInitExclusions initExclusions;
//    
    static final int MAX_ZIPPED_FILE_NAME = 2048;
    static final int FILE_HASH_SIZE = 1024;

    static class fileInPack_s {

        idStr name;					// name of the file
        int pos;					// file info position in zip
        fileInPack_s next;				// next file in the hash
        ZipEntry entry;                                 //
    }

    enum binaryStatus_t {

        BINARY_UNKNOWN,
        BINARY_YES,
        BINARY_NO
    }

    enum pureStatus_t {

        PURE_UNKNOWN, // need to run the pak through GetPackStatus
        PURE_NEUTRAL, // neutral regarding pureness. gets in the pure list if referenced
        PURE_ALWAYS, // always referenced - for pak* named files, unless NEVER
        PURE_NEVER		// VO paks. may be referenced, won't be in the pure lists
    }

    static class addonInfo_t {

        idList<Integer> depends;
        idList<idDict> mapDecls;
    }

    static class pack_t {

        idStr pakFilename;				// c:\doom\base\pak0.pk4
        ZipFile/*unzFile*/ handle;
        int checksum;
        int numfiles;
        int length;
        boolean referenced;
        binaryStatus_t binary;
        boolean addon;					// this is an addon pack - addon_search tells if it's 'active'
        boolean addon_search;				// is in the search list
        addonInfo_t addon_info;
        pureStatus_t pureStatus;
        boolean isNew;					// for downloaded paks
        fileInPack_s[] hashTable = new fileInPack_s[FILE_HASH_SIZE];
        fileInPack_s[] buildBuffer;
    }

    static class directory_t {

        idStr path;					// c:\doom
        idStr gamedir;					// base
    }

    static class searchpath_s {

        pack_t pack;					// only one of pack / dir will be non NULL
        directory_t dir;
        searchpath_s next;
    }

//    
// search flags when opening a file
    static final int FSFLAG_SEARCH_DIRS = (1 << 0);
    static final int FSFLAG_SEARCH_PAKS = (1 << 1);
    static final int FSFLAG_PURE_NOREF = (1 << 2);
    static final int FSFLAG_BINARY_ONLY = (1 << 3);
    static final int FSFLAG_SEARCH_ADDONS = (1 << 4);
//
    // 3 search path (fs_savepath fs_basepath fs_cdpath)
    // + .jpg and .tga
    static final int MAX_CACHED_DIRS = 6;
//    
    // how many OSes to handle game paks for ( we don't have to know them precisely )
    static final int MAX_GAME_OS = 6;
    static final String BINARY_CONFIG = "binary.conf";
    static final String ADDON_CONFIG = "addon.conf";

    static class idDEntry extends idStrList {

        private idStr directory;
        private idStr extension;
        //
        //

        public idDEntry() {
            this.directory = new idStr();
            this.extension = new idStr();
        }
//public	virtual				~idDEntry() {}
//

        public boolean Matches(final String directory, final String extension) {
            if ((0 == this.directory.Icmp(directory))
                    && (0 == this.extension.Icmp(extension))) {
                return true;
            }
            return false;
        }

        public void Init(final String directory, final String extension, final idStrList list) {
            this.directory = new idStr(directory);
            this.extension = new idStr(extension);
            super.oSet(list);
        }

        @Override
        public void Clear() {
            this.directory.Clear();
            this.extension.Clear();
            super.Clear();
        }
    }

    static class idFileSystemLocal extends idFileSystem {

        private searchpath_s searchPaths;
        private int          readCount;                     // total bytes read
        private int          loadCount;                     // total files read
        private int          loadStack;                     // total files in memory
        private idStr        gameFolder;                    // this will be a single name without separators
        //
        private searchpath_s addonPaks;                     // not loaded up, but we saw them
        private idDict       mapDict;                       // for GetMapDecl
        //
        private static final idCVar fs_debug           = new idCVar("fs_debug", "0", CVAR_SYSTEM | CVAR_INTEGER, "", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        private static final idCVar fs_restrict        = new idCVar("fs_restrict", "", CVAR_SYSTEM | CVAR_INIT | CVAR_BOOL, "");
        private static final idCVar fs_copyfiles       = new idCVar("fs_copyfiles", "0", CVAR_SYSTEM | CVAR_INIT | CVAR_INTEGER, "", 0, 4, new idCmdSystem.ArgCompletion_Integer(0, 3));
        private static final idCVar fs_basepath        = new idCVar("fs_basepath", "", CVAR_SYSTEM | CVAR_INIT, "");
        private static final idCVar fs_savepath        = new idCVar("fs_savepath", "", CVAR_SYSTEM | CVAR_INIT, "");
        private static final idCVar fs_cdpath          = new idCVar("fs_cdpath", "", CVAR_SYSTEM | CVAR_INIT, "");
        private static final idCVar fs_devpath         = new idCVar("fs_devpath", "", CVAR_SYSTEM | CVAR_INIT, "");
        private static final idCVar fs_game            = new idCVar("fs_game", "", CVAR_SYSTEM | CVAR_INIT | CVAR_SERVERINFO, "mod path");
        private static final idCVar fs_game_base       = new idCVar("fs_game_base", "", CVAR_SYSTEM | CVAR_INIT | CVAR_SERVERINFO, "alternate mod path, searched after the main fs_game path, before the basedir");
        private static final idCVar fs_caseSensitiveOS = new idCVar("fs_caseSensitiveOS", (WIN32 ? "0" : "1"), CVAR_SYSTEM | CVAR_BOOL, "");
        private static final idCVar fs_searchAddons    = new idCVar("fs_searchAddons", "0", CVAR_SYSTEM | CVAR_BOOL, "search all addon pk4s ( disables addon functionality )");
        //
        private backgroundDownload_s backgroundDownloads;
        private backgroundDownload_s defaultBackgroundDownload;
        private final xthreadInfo          backgroundThread;
        //
        private final idList<pack_t>       serverPaks;
        private boolean              loadedFileFromDir;     // set to true once a file was loaded from a directory - can't switch to pure anymore
        private final idList<Integer>      restartChecksums;      // used during a restart to set things in right order
        private final idList<Integer>      addonChecksums;        // list of checksums that should go to the search list directly ( for restarts )
        private int                  restartGamePakChecksum;
        private int                  gameDLLChecksum;       // the checksum of the last loaded game DLL
        private int                  gamePakChecksum;       // the checksum of the pak holding the loaded game DLL
        //
        private final int[] gamePakForOS = new int[MAX_GAME_OS];
        //
        private final idDEntry[] dir_cache; // fifo
        private       int        dir_cache_index;
        private       int        dir_cache_count;
        //
        private       int        d3xp;                      // 0: didn't check, -1: not installed, 1: installed
        //
        //

        public idFileSystemLocal() {
            this.searchPaths = null;
            this.readCount = 0;
            this.loadCount = 0;
            this.loadStack = 0;
            this.gameFolder = new idStr();
            this.dir_cache_index = 0;
            this.dir_cache_count = 0;
            this.d3xp = 0;
            this.loadedFileFromDir = false;
            this.restartGamePakChecksum = 0;
            this.backgroundThread = new xthreadInfo();//memset( &backgroundThread, 0, sizeof( backgroundThread ) );
            this.serverPaks = new idList<>();
            this.addonPaks = null;
            this.mapDict = new idDict();

            this.restartChecksums = new idList<>();
            this.addonChecksums = new idList<>();

            this.dir_cache = new idDEntry[MAX_CACHED_DIRS];
            for (int s = 0; s < MAX_CACHED_DIRS; s++) {
                this.dir_cache[s] = new idDEntry();
            }
        }


        /*
         ================
         idFileSystemLocal::Init

         Called only at inital startup, not when the filesystem
         is resetting due to a game change
         ================
         */
        @Override
        public void Init() {
            // allow command line parms to override our defaults
            // we have to specially handle this, because normal command
            // line variable sets don't happen until after the filesystem
            // has already been initialized
            common.StartupVariable("fs_basepath", false);
            common.StartupVariable("fs_savepath", false);
            common.StartupVariable("fs_cdpath", false);
            common.StartupVariable("fs_devpath", false);
            common.StartupVariable("fs_game", false);
            common.StartupVariable("fs_game_base", false);
            common.StartupVariable("fs_copyfiles", false);
            common.StartupVariable("fs_restrict", false);
            common.StartupVariable("fs_searchAddons", false);

            if (!ID_ALLOW_D3XP) {
                if ((fs_game.GetString() != null) && (0 == idStr.Icmp(fs_game.GetString(), "d3xp"))) {
                    fs_game.SetString(null);
                }
                if ((fs_game_base.GetString() != null) && (0 == idStr.Icmp(fs_game_base.GetString(), "d3xp"))) {
                    fs_game_base.SetString(null);
                }
            }

            if (fs_basepath.GetString().isEmpty()) {
                fs_basepath.SetString(Sys_DefaultBasePath());
            }
            if (fs_savepath.GetString().isEmpty()) {
                fs_savepath.SetString(Sys_DefaultSavePath());
            }
            if (fs_cdpath.GetString().isEmpty()) {
                fs_cdpath.SetString(Sys_DefaultCDPath());
            }

            if (fs_devpath.GetString().isEmpty()) {
                if (WIN32) {
                    fs_devpath.SetString(!fs_cdpath.GetString().isEmpty() ? fs_cdpath.GetString() : fs_basepath.GetString());
                } else {
                    fs_devpath.SetString(fs_savepath.GetString());
                }
            }

            // try to start up normally
            Startup();

            // see if we are going to allow add-ons
            SetRestrictions();

            // spawn a thread to handle background file reads
            StartBackgroundDownloadThread();

            // if we can't find default.cfg, assume that the paths are
            // busted and error out now, rather than getting an unreadable
            // graphics screen when the font fails to load
            // Dedicated servers can run with no outside files at all
            if (ReadFile("default.cfg", null, null) <= 0) {
                common.FatalError("Couldn't load default.cfg");
            }
        }

        public void StartBackgroundDownloadThread() {
            if (NOT(this.backgroundThread.threadHandle)) {//TODO:enable this.
//                Sys_CreateThread(BackgroundDownloadThread.INSTANCE, null, THREAD_NORMAL, backgroundThread, "backgroundDownload", g_threads, g_thread_count);
                if (NOT(this.backgroundThread.threadHandle)) {
                    common.Warning("idFileSystemLocal::StartBackgroundDownloadThread: failed");
                }
            } else {
                common.Printf("background thread already running\n");
            }
        }

        @Override
        public void Restart() {
            // free anything we currently have loaded
            Shutdown(true);

            Startup();

            // see if we are going to allow add-ons
            SetRestrictions();

            // if we can't find default.cfg, assume that the paths are
            // busted and error out now, rather than getting an unreadable
            // graphics screen when the font fails to load
            if (ReadFile("default.cfg", null, null) <= 0) {
                common.FatalError("Couldn't load default.cfg");
            }
        }

        /*
         ================
         idFileSystemLocal::Shutdown

         Frees all resources and closes all files
         ================
         */
        @Override
        public void Shutdown(boolean reloading) {
            searchpath_s sp;
            searchpath_s next, loop;

            this.gameFolder.Clear();

            this.serverPaks.Clear();
            if (!reloading) {
                this.restartChecksums.Clear();
                this.addonChecksums.Clear();
            }
            this.loadedFileFromDir = false;
            this.gameDLLChecksum = 0;
            this.gamePakChecksum = 0;

            ClearDirCache();

            // free everything - loop through searchPaths and addonPaks
            for (loop = this.searchPaths; loop != null; loop = (loop == this.searchPaths ? this.addonPaks : null)) {
                for (sp = loop; sp != null; sp = next) {
                    next = sp.next;

                    if (sp.pack != null) {
                        try {
                            //                        unzClose(sp.pack.handle);
                            sp.pack.handle.close();
                        } catch (final IOException ex) {
                            Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
                        }
//				delete [] sp.pack.buildBuffer;
                        if (sp.pack.addon_info != null) {
                            sp.pack.addon_info.mapDecls.DeleteContents(true);
//					delete sp.pack.addon_info;
                            sp.pack.addon_info = null;
                        }
//				delete sp.pack;
                        sp.pack = null;
                    }
                    if (sp.dir != null) {
//				delete sp.dir;
                        sp.dir = null;
                    }
//			delete sp;
                }
            }

            // any FS_ calls will now be an error until reinitialized
            this.searchPaths = null;
            this.addonPaks = null;

            cmdSystem.RemoveCommand("path");
            cmdSystem.RemoveCommand("dir");
            cmdSystem.RemoveCommand("dirtree");
            cmdSystem.RemoveCommand("touchFile");

            this.mapDict.Clear();
        }

        @Override
        public boolean IsInitialized() {
            return (this.searchPaths != null);
        }

        @Override
        public boolean PerformingCopyFiles() {
            return fs_copyfiles.GetInteger() > 0;
        }

        static final int MAX_DESCRIPTION = 256;

        @Override
        public idModList ListMods() {
            int i;
            final ByteBuffer desc = ByteBuffer.allocate(MAX_DESCRIPTION);

            final idStrList dirs = new idStrList();
            final idStrList pk4s = new idStrList();

            final idModList list = new idModList();

            final String[] search = new String[4];
            int isearch;

            search[0] = fs_savepath.GetString();
            search[1] = fs_devpath.GetString();
            search[2] = fs_basepath.GetString();
            search[3] = fs_cdpath.GetString();

            for (isearch = 0; isearch < 4; isearch++) {

                dirs.Clear();
                pk4s.Clear();

                // scan for directories
                ListOSFiles(search[isearch], "/", dirs);

                dirs.Remove(new idStr("."));
                dirs.Remove(new idStr(".."));
                dirs.Remove(new idStr("base"));
                dirs.Remove(new idStr("pb"));

                // see if there are any pk4 files in each directory
                for (i = 0; i < dirs.Num(); i++) {
                    final idStr gamepath = new idStr(BuildOSPath(search[isearch], dirs.oGet(i).toString(), ""));
                    ListOSFiles(gamepath.toString(), ".pk4", pk4s);
                    if (pk4s.Num() != 0) {
                        if (0 == list.mods.Find(dirs.oGet(i))) {
                            // D3 1.3 #31, only list d3xp if the pak is present
                            if ((dirs.oGet(i).Icmp("d3xp") != 0) || HasD3XP()) {
                                list.mods.Append(dirs.oGet(i));
                            }
                        }
                    }
                }
            }

            list.mods.Sort();

            // read the descriptions for each mod - search all paths
            for (i = 0; i < list.mods.Num(); i++) {

                for (isearch = 0; isearch < 4; isearch++) {

                    final idStr descfile = new idStr(BuildOSPath(search[isearch], list.mods.oGet(i).toString(), "description.txt"));
                    final FileChannel f = OpenOSFile(descfile.toString(), "r");
                    if (f != null) {
                        try {
                            if (f.read(desc) > 0) {
                                list.descriptions.Append(new idStr(new String(desc.array())));
                                f.close();
                                break;
                            } else {
                                common.DWarning("Error reading %s", descfile.toString());
                                f.close();
                                continue;
                            }
                        } catch (final IOException ex) {
                            Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                if (isearch == 4) {
                    list.descriptions.Append(list.mods.oGet(i));
                }
            }

            list.mods.Insert(new idStr(""));
            list.descriptions.Insert(new idStr("Doom 3"));

            assert (list.mods.Num() == list.descriptions.Num());

            return list;
        }

        @Override
        public void FreeModList(idModList modList) {
//	delete modList;
        }

        @Override
        public idFileList ListFiles(String relativePath, String extension) {
            return ListFiles(relativePath, extension, false);
        }

        @Override
        public idFileList ListFiles(String relativePath, String extension, boolean sort) {
            return ListFiles(relativePath, extension, sort, false);
        }

        @Override
        public idFileList ListFiles(String relativePath, String extension, boolean sort, boolean fullRelativePath) {
            return ListFiles(relativePath, extension, sort, fullRelativePath, null);
        }

        @Override
        public idFileList ListFiles(String relativePath, String extension, boolean sort, boolean fullRelativePath, String gamedir) {
            final idHashIndex hashIndex = new HashIndex.idHashIndex(4096, 4096);
            final idStrList extensionList = new idStrList();

            final idFileList fileList = new idFileList();
            fileList.basePath = new idStr(relativePath);

            GetExtensionList(extension, extensionList);

            GetFileList(relativePath, extensionList, fileList.list, hashIndex, fullRelativePath, gamedir);

            if (sort) {
                idStrListSortPaths(fileList.list);
            }

            return fileList;
        }

        @Override
        public idFileList ListFilesTree(String relativePath, String extension) {
            return ListFilesTree(relativePath, extension, false);
        }

        @Override
        public idFileList ListFilesTree(String relativePath, String extension, boolean sort) {
            return ListFilesTree(relativePath, extension, sort, null);
        }

        @Override
        public idFileList ListFilesTree(String relativePath, String extension, boolean sort, String gamedir) {
            final idHashIndex hashIndex = new idHashIndex(4096, 4096);
            final idStrList extensionList = new idStrList();

            final idFileList fileList = new idFileList();
            fileList.basePath = new idStr(relativePath);
            fileList.list.SetGranularity(4096);

            GetExtensionList(extension, extensionList);

            GetFileListTree(relativePath, extensionList, fileList.list, hashIndex, gamedir);

            if (sort) {
                idStrListSortPaths(fileList.list);
            }

            return fileList;
        }

        @Override
        public void FreeFileList(idFileList fileList) {
//            delete fileList;
        }

        /*
         ================
         idFileSystemLocal::OSPathToRelativePath

         takes a full OS path, as might be found in data from a media creation
         program, and converts it to a relativePath by stripping off directories

         Returns false if the osPath tree doesn't match any of the existing
         search paths.

         ================
         */
        @Override
        public String OSPathToRelativePath(String OSPath) {
            String relativePath;//=new char[MAX_STRING_CHARS];
            int s, base;

            // skip a drive letter?
            // search for anything with "base" in it
            // Ase files from max may have the form of:
            // "//Purgatory/purgatory/doom/base/models/mapobjects/bitch/hologirl.tga"
            // which won't match any of our drive letter based search paths
            boolean ignoreWarning = false;
            if (ID_DEMO_BUILD) {
                base = OSPath.indexOf(BASE_GAMEDIR);
                String tempStr = OSPath;
                tempStr = tempStr.toLowerCase();
                if ((tempStr.contains("//") || tempStr.contains("w:"))
                        && tempStr.contains("/doom/base/")) {
                    // will cause a warning but will load the file. ase models have
                    // hard coded doom/base/ in the material names
                    base = OSPath.indexOf("base");
                    ignoreWarning = true;
                }
            } else {
                // look for the first complete directory name
                base = OSPath.indexOf(BASE_GAMEDIR);
                while (base != -1) {
                    char c1 = '\0', c2;
                    if (base > 0) {
                        c1 = OSPath.charAt(base - 1);
                    }
                    c2 = OSPath.charAt(base + BASE_GAMEDIR.length());
                    if (((c1 == '/') || (c1 == '\\')) && ((c2 == '/') || (c2 == '\\'))) {
                        break;
                    }
                    base = OSPath.indexOf(BASE_GAMEDIR, base + 1);
                }
            }
            // fs_game and fs_game_base support - look for first complete name with a mod path
            // ( fs_game searched before fs_game_base )
            String fsgame = null;
            int iGame;
            for (iGame = 0; iGame < 2; iGame++) {
                if (iGame == 0) {
                    fsgame = fs_game.GetString();
                } else if (iGame == 1) {
                    fsgame = fs_game_base.GetString();
                }
                if ((-1 == base) && isNotNullOrEmpty(fsgame)) {
                    base = OSPath.indexOf(fsgame);
                    while (base != -1) {
                        char c1 = '\0', c2;
                        if (base > 0) {
                            c1 = OSPath.charAt(base - 1);
                        }
                        c2 = OSPath.charAt(base + fsgame.length());
                        if (((c1 == '/') || (c1 == '\\')) && ((c2 == '/') || (c2 == '\\'))) {
                            break;
                        }
                        base = OSPath.indexOf(fsgame, base + 1);
                    }
                }
            }

            if (base > 0) {
                s = OSPath.indexOf('/', base);
                if (s < 0) {
                    s = OSPath.indexOf('\\', base);
                }
                if (s != -1) {
//                    strcpy(relativePath, s + 1);
                    relativePath = OSPath.substring(s + 1);
                    if (fs_debug.GetInteger() > 1) {
                        common.Printf("idFileSystem::OSPathToRelativePath: %s becomes %s\n", OSPath, relativePath);
                    }
                    return relativePath;
                }
            }

            if (!ignoreWarning) {
                common.Warning("idFileSystem::OSPathToRelativePath failed on %s", OSPath);
            }
//            strcpy(relativePath, "");
            return "";
        }

        /*
         =====================
         idFileSystemLocal::RelativePathToOSPath

         Returns a fully qualified path that can be used with stdio libraries
         =====================
         */
        @Override
        public String RelativePathToOSPath(String relativePath, String basePath) {
            String path = cvarSystem.GetCVarString(basePath);
            if (path.isEmpty()) {
                path = fs_savepath.GetString();
            }
            return BuildOSPath(path, this.gameFolder.toString(), relativePath);
        }

        @Override
        public String BuildOSPath(String base, String game, String relativePath) {
            final StringBuilder OSPath = new StringBuilder(MAX_STRING_CHARS);
            idStr newPath;

            if (fs_caseSensitiveOS.GetBool() || com_developer.GetBool()) {
                // extract the path, make sure it's all lowercase
                idStr testPath, fileName;

//		sprintf( testPath, "%s/%s", game , relativePath );
                testPath = new idStr(String.format("%s/%s", game, relativePath));
                testPath.StripFilename();

                if (testPath.HasUpper()) {

                    common.Warning("Non-portable: path contains uppercase characters: %s", testPath);

                    // attempt a fixup on the fly
                    if (fs_caseSensitiveOS.GetBool()) {
                        testPath.ToLower();
                        fileName = new idStr(relativePath);
                        fileName.StripPath();
//				sprintf( newPath, "%s/%s/%s", base, testPath.c_str(), fileName.c_str() );
                        newPath = new idStr(String.format("%s/%s/%s", base, testPath, fileName));
                        ReplaceSeparators(newPath);
                        common.DPrintf("Fixed up to %s\n", newPath);
                        idStr.Copynz(OSPath, newPath.toString());
                        return OSPath.toString();
                    }
                }
            }

            final idStr strBase = new idStr(base);
            strBase.StripTrailing('/');
            strBase.StripTrailing('\\');
//	sprintf( newPath, "%s/%s/%s", strBase.c_str(), game, relativePath );
            newPath = new idStr(String.format("%s/%s/%s", strBase, game, relativePath));
            ReplaceSeparators(newPath);
            idStr.Copynz(OSPath, newPath.toString());
            return OSPath.toString();
        }

        /*
         ============
         idFileSystemLocal::CreateOSPath

         Creates any directories needed to store the given filename
         ============
         */
        @Override
        public void CreateOSPath(String OSPath) {
            int ofs;

            // make absolutely sure that it can't back up the path
            // FIXME: what about c: ?
            if (OSPath.contains("..") || OSPath.contains("::")) {
                if (_DEBUG) {
                    common.DPrintf("refusing to create relative path \"%s\"\n", OSPath);
                }
                return;
            }

            final idStr path = new idStr(OSPath);
            for (ofs = 1; ofs < path.Length(); ofs++) {
                if (path.oGet(ofs) == PATHSEPERATOR_CHAR) {
                    // create the directory
                    path.oSet(ofs, '0');
                    Sys_Mkdir(path);
                    path.oSet(ofs, PATHSEPERATOR_CHAR);
                }
            }
        }

        @Override
        public boolean FileIsInPAK(String relativePath) {
            searchpath_s search;
            pack_t pak;
            fileInPack_s pakFile;
            long hash;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (null == relativePath) {
                common.FatalError("idFileSystemLocal::FileIsInPAK: NULL 'relativePath' parameter passed\n");
            }

            // qpaths are not supposed to have a leading slash
            if ((relativePath.charAt(0) == '/') || (relativePath.charAt(0) == '\\')) {
//		relativePath++;
                relativePath = relativePath.substring(1);
            }

            // make absolutely sure that it can't back up the path.
            // The searchpaths do guarantee that something will always
            // be prepended, so we don't need to worry about "c:" or "//limbo" 
            if (relativePath.contains("..") || relativePath.contains("::")) {
                return false;
            }

            //
            // search through the path, one element at a time
            //
            hash = HashFileName(relativePath);

            for (search = this.searchPaths; search != null; search = search.next) {
                // is the element a pak file?
                if ((search.pack != null) && (search.pack.hashTable[(int) hash] != null)) {

                    // disregard if it doesn't match one of the allowed pure pak files - or is a localization file
                    if (this.serverPaks.Num() != 0) {
                        GetPackStatus(search.pack);
                        if ((search.pack.pureStatus != PURE_NEVER) && (null == this.serverPaks.Find(search.pack))) {
                            continue; // not on the pure server pak list
                        }
                    }

                    // look through all the pak file elements
                    pak = search.pack;
                    pakFile = pak.hashTable[(int) hash];
                    do {
                        // case and separator insensitive comparisons
                        if (FilenameCompare(pakFile.name.toString(), relativePath)) {
                            return true;
                        }
                        pakFile = pakFile.next;
                    } while (pakFile != null);
                }
            }
            return false;
        }

        @Override
        public void UpdatePureServerChecksums() {
            searchpath_s search;
            int i;
            pureStatus_t status;

            this.serverPaks.Clear();
            for (search = this.searchPaths; search != null; search = search.next) {
                // is the element a referenced pak file?
                if (null == search.pack) {
                    continue;
                }
                status = GetPackStatus(search.pack);
                if (status == PURE_NEVER) {
                    continue;
                }
                if ((status == PURE_NEUTRAL) && !search.pack.referenced) {
                    continue;
                }
                this.serverPaks.Append(search.pack);
                if (this.serverPaks.Num() >= MAX_PURE_PAKS) {
                    common.FatalError("MAX_PURE_PAKS ( %d ) exceeded\n", MAX_PURE_PAKS);
                }
            }
            if (fs_debug.GetBool()) {
                String checks = "";
                for (i = 0; i < this.serverPaks.Num(); i++) {
                    checks += va("%x ", this.serverPaks.oGet(i).checksum);
                }
                common.Printf("set pure list - %d paks ( %s)\n", this.serverPaks.Num(), checks);
            }
        }

        @Override
        public boolean UpdateGamePakChecksums() {
            searchpath_s search;
            fileInPack_s pakFile;
            int confHash;
            idFile confFile;
            ByteBuffer buf;
            idLexer lexConf;
            final idToken token = new idToken();
            int id;

            confHash = (int) HashFileName(BINARY_CONFIG);

//	memset( gamePakForOS, 0, sizeof( gamePakForOS ) );
            for (search = this.searchPaths; search != null; search = search.next) {
                if (null == search.pack) {
                    continue;
                }
                search.pack.binary = BINARY_NO;
                for (pakFile = search.pack.hashTable[confHash]; pakFile != null; pakFile = pakFile.next) {
                    if (FilenameCompare(pakFile.name.toString(), BINARY_CONFIG)) {
                        search.pack.binary = BINARY_YES;
                        confFile = ReadFileFromZip(search.pack, pakFile, BINARY_CONFIG);
//				buf = new char[ confFile.Length() + 1 ];
                        confFile.Read(buf = ByteBuffer.allocate(confFile.Length()), confFile.Length());
//				buf[ confFile.Length() ] = '\0';
                        lexConf = new idLexer(new String(buf.array()), confFile.Length(), confFile.GetFullPath());
                        while (lexConf.ReadToken(token)) {
                            if (token.IsNumeric()) {
                                id = Integer.parseInt(token.toString());
                                if ((id < MAX_GAME_OS) && (0 == this.gamePakForOS[id])) {
                                    if (fs_debug.GetBool()) {
                                        common.Printf("Adding game pak checksum for OS %d: %s 0x%x\n", id, confFile.GetFullPath(), search.pack.checksum);
                                    }
                                    this.gamePakForOS[id] = search.pack.checksum;
                                }
                            }
                        }
                        CloseFile(confFile);
//				delete lexConf;
//				delete[] buf;
                    }
                }
            }

            // some sanity checks on the game code references
            // make sure that at least the local OS got a pure reference
            if (0 == this.gamePakForOS[BUILD_OS_ID]) {
                common.Warning("No game code pak reference found for the local OS");
                return false;
            }

            if (!cvarSystem.GetCVarBool("net_serverAllowServerMod")
                    && (this.gamePakChecksum != this.gamePakForOS[BUILD_OS_ID])) {
                common.Warning("The current game code doesn't match pak files (net_serverAllowServerMod is off)");
                return false;
            }

            return true;
        }

        /*
         =====================
         idFileSystemLocal::SetPureServerChecksums
         set the pure paks according to what the server asks
         if that's not possible, identify why and build an answer
         can be:
         loadedFileFromDir - some files were loaded from directories instead of paks (a restart in pure pak-only is required)
         missing/wrong checksums - some pak files would need to be installed/updated (downloaded for instance)
         some pak files currently referenced are not referenced by the server
         wrong order - if the pak order doesn't match, means some stuff could have been loaded from somewhere else
         server referenced files are prepended to the list if possible ( that doesn't break pureness )
         DLL:
         the checksum of the pak containing the DLL is maintained seperately, the server can send different replies by OS
         =====================
         */
        @Override
        public fsPureReply_t SetPureServerChecksums(int[] pureChecksums, int _gamePakChecksum, int[] missingChecksums, int[] missingGamePakChecksum) {
            pack_t pack;
            int i, j, imissing;
            boolean success = true;
            boolean canPrepend = true;
            final String[] dllName = {null};
            int dllHash;
            fileInPack_s pakFile;

            sys.DLL_GetFileName("game", dllName, MAX_OSPATH);
            dllHash = (int) HashFileName(dllName[0]);

            imissing = 0;
            missingChecksums[0] = 0;
            assert (missingGamePakChecksum[0] != 0);
            missingGamePakChecksum[0] = 0;

            if (pureChecksums[0] == 0) {
                ClearPureChecksums();
                return PURE_OK;
            }

            if (0 == this.serverPaks.Num()) {
                // there was no pure lockdown yet - lock to what we already have
                UpdatePureServerChecksums();
            }
            i = 0;
            j = 0;
            while (pureChecksums[i] != 0) {
                if ((j < this.serverPaks.Num()) && (this.serverPaks.oGet(j).checksum == pureChecksums[i])) {
                    canPrepend = false; // once you start matching into the list there is no prepending anymore
                    i++;
                    j++; // the pak is matched, is in the right order, continue..
                } else {
                    pack = GetPackForChecksum(pureChecksums[i], true);
                    if ((pack != null) && pack.addon && !pack.addon_search) {
                        // this is an addon pack, and it's not on our current search list
                        // setting success to false meaning that a restart including this addon is required
                        if (fs_debug.GetBool()) {
                            common.Printf("pak %s checksumed 0x%x is on addon list. Restart required.\n", pack.pakFilename.toString(), pack.checksum);
                        }
                        success = false;
                    }
                    if ((pack != null) && pack.isNew) {
                        // that's a downloaded pack, we will need to restart
                        if (fs_debug.GetBool()) {
                            common.Printf("pak %s checksumed 0x%x is a newly downloaded file. Restart required.\n", pack.pakFilename.toString(), pack.checksum);
                        }
                        success = false;
                    }
                    if (pack != null) {
                        if (canPrepend) {
                            // we still have a chance
                            if (fs_debug.GetBool()) {
                                common.Printf("prepend pak %s checksumed 0x%x at index %d\n", pack.pakFilename.toString(), pack.checksum, j);
                            }
                            // NOTE: there is a light possibility this adds at the end of the list if UpdatePureServerChecksums didn't set anything
                            this.serverPaks.Insert(pack, j);
                            i++;
                            j++; // continue..
                        } else {
                            success = false;
                            if (fs_debug.GetBool()) {
                                // verbose the situation
                                if (this.serverPaks.Find(pack) != null) {
                                    common.Printf("pak %s checksumed 0x%x is in the pure list at wrong index. Current index is %d, found at %d\n", pack.pakFilename.toString(), pack.checksum, j, this.serverPaks.FindIndex(pack));
                                } else {
                                    common.Printf("pak %s checksumed 0x%x can't be added to pure list because of search order\n", pack.pakFilename.toString(), pack.checksum);
                                }
                            }
                            i++; // advance server checksums only
                        }
                    } else {
                        // didn't find a matching checksum
                        success = false;
                        missingChecksums[imissing++] = pureChecksums[i];
                        missingChecksums[imissing] = 0;
                        if (fs_debug.GetBool()) {
                            common.Printf("checksum not found - 0x%x\n", pureChecksums[i]);
                        }
                        i++; // advance the server checksums only
                    }
                }
            }
            while (j < this.serverPaks.Num()) {
                success = false; // just in case some extra pak files are referenced at the end of our local list
                if (fs_debug.GetBool()) {
                    common.Printf("pak %s checksumed 0x%x is an extra reference at the end of local pure list\n", this.serverPaks.oGet(j).pakFilename.toString(), this.serverPaks.oGet(j).checksum);
                }
                j++;
            }

            // DLL checksuming
            if (0 == _gamePakChecksum) {
                // server doesn't have knowledge of code we can use ( OS issue )
                return PURE_NODLL;
            }
            assert (this.gameDLLChecksum != 0);
            if (ID_FAKE_PURE) {
                this.gamePakChecksum = _gamePakChecksum;
            }
            if (_gamePakChecksum != this.gamePakChecksum) {
                // current DLL is wrong, search for a pak with the approriate checksum
                // ( search all paks, the pure list is not relevant here )
                pack = GetPackForChecksum(_gamePakChecksum);
                if (null == pack) {
                    if (fs_debug.GetBool()) {
                        common.Printf("missing the game code pak ( 0x%x )\n", _gamePakChecksum);
                    }
                    // if there are other paks missing they have also been marked above
                    missingGamePakChecksum[0] = _gamePakChecksum;
                    return PURE_MISSING;
                }
                // if assets paks are missing, don't try any of the DLL restart / NODLL
                if (imissing != 0) {
                    return PURE_MISSING;
                }
                // we have a matching pak
                if (fs_debug.GetBool()) {
                    common.Printf("server's game code pak candidate is '%s' ( 0x%x )\n", pack.pakFilename.toString(), pack.checksum);
                }
                // make sure there is a valid DLL for us
                if (pack.hashTable[dllHash] != null) {
                    for (pakFile = pack.hashTable[dllHash]; pakFile != null; pakFile = pakFile.next) {
                        if (FilenameCompare(pakFile.name.toString(), dllName[0])) {
                            this.gamePakChecksum = _gamePakChecksum;        // this will be used to extract the DLL in pure mode FindDLL
                            return PURE_RESTART;
                        }
                    }
                }
                common.Warning("media is misconfigured. server claims pak '%s' ( 0x%x ) has media for us, but '%s' is not found\n", pack.pakFilename.toString(), pack.checksum, dllName[0]);
                return PURE_NODLL;
            }

            // we reply to missing after DLL check so it can be part of the list
            if (imissing != 0) {
                return PURE_MISSING;
            }

            // one last check
            if (this.loadedFileFromDir) {
                success = false;
                if (fs_debug.GetBool()) {
                    common.Printf("SetPureServerChecksums: there are files loaded from dir\n");
                }
            }
            return (success ? PURE_OK : PURE_RESTART);
        }

        @Override
        public void GetPureServerChecksums(int[] checksums, int OS, int[] _gamePakChecksum) {
            int i;

            for (i = 0; i < this.serverPaks.Num(); i++) {
                checksums[i] = this.serverPaks.oGet(i).checksum;
            }
            checksums[i] = 0;
            if (_gamePakChecksum != null) {
                if (OS >= 0) {
                    _gamePakChecksum[0] = this.gamePakForOS[OS];
                } else {
                    _gamePakChecksum[0] = this.gamePakChecksum;
                }
            }
        }

        @Override
        public void SetRestartChecksums(int[] pureChecksums, int gamePakChecksum) {
            int i;
            pack_t pack;

            this.restartChecksums.Clear();
            i = 0;
            while (i < pureChecksums.length) {
                pack = GetPackForChecksum(pureChecksums[i], true);
                if (null == pack) {
                    common.FatalError("SetRestartChecksums failed: no pak for checksum 0x%x\n", pureChecksums[i]);
                }
                if (pack.addon && (this.addonChecksums.FindIndex(pack.checksum) < 0)) {
                    // can't mark it pure if we're not even gonna search it :-)
                    this.addonChecksums.Append(pack.checksum);
                }
                this.restartChecksums.Append(pureChecksums[i]);
                i++;
            }
            this.restartGamePakChecksum = gamePakChecksum;
        }

        @Override
        public void ClearPureChecksums() {
            common.DPrintf("Cleared pure server lock\n");
            this.serverPaks.Clear();
        }

        @Override
        public int GetOSMask() {
            int i, ret = 0;
            for (i = 0; i < MAX_GAME_OS; i++) {
                if (fileSystemLocal.gamePakForOS[i] != 0) {
                    ret |= (1 << i);
                }
            }
            if (0 == ret) {
                return -1;
            }
            return ret;
        }

        /*
         ============
         idFileSystemLocal::ReadFile

         Filename are relative to the search path
         a null buffer will just return the file length and time without loading
         timestamp can be NULL if not required
         ============
         */
        @Override
        public int ReadFile(String relativePath, ByteBuffer[] buffer, long[] timestamp) {
            idFile f;
            ByteBuffer buf;
            final int[] len = {0};
            boolean isConfig;

            if (NOT(this.searchPaths)) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (NOT(relativePath) || relativePath.isEmpty()) {
                common.FatalError("idFileSystemLocal::ReadFile with empty name\n");
            }

            if (timestamp != null) {
                timestamp[0] = FILE_NOT_FOUND_TIMESTAMP;
            }

            if (buffer != null) {
                buffer[0] = null;//TODO:
            }

//            buf = null;	// quiet compiler warning
            // if this is a .cfg file and we are playing back a journal, read
            // it from the journal file
            if (relativePath.endsWith(".cfg")) {
//            if (relativePath.indexOf(".cfg") == relativePath.length() - 4) {
                isConfig = true;
                if ((eventLoop != null) && (eventLoop.JournalLevel() == 2)) {
                    int r;

                    this.loadCount++;
                    this.loadStack++;

                    common.DPrintf("Loading %s from journal file.\n", relativePath);
                    len[0] = 0;
                    r = eventLoop.com_journalDataFile.ReadInt(len);
                    final int r_bits = r * 8;
                    if (r_bits != Integer.SIZE) {
                        buffer[0] = null;
                        return -1;
                    }
                    buf = ByteBuffer.allocate(len[0] + 1);// Heap.Mem_ClearedAlloc(len + 1);
                    buffer[0] = buf;
                    r = eventLoop.com_journalDataFile.Read(buf, len[0]);
                    if (r != len[0]) {
                        common.FatalError("Read from journalDataFile failed");
                    }

                    // guarantee that it will have a trailing 0 for string operations
                    buf.put(len[0], (byte) 0);

                    return len[0];
                }
            } else {
                isConfig = false;
            }

            // look for it in the filesystem or pack files
            f = OpenFileRead(relativePath, (buffer != null));
            if (f == null) {
                if (buffer != null) {
                    buffer[0] = null;
                }
                return -1;
            }
            len[0] = f.Length();

            if (timestamp != null) {
                timestamp[0] = f.Timestamp();
            }

            if (null == buffer) {
                CloseFile(f);
                return len[0];
            }

            this.loadCount++;
            this.loadStack++;

            buf = ByteBuffer.allocate(len[0] + 1);// Heap.Mem_ClearedAlloc(len + 1);
            buffer[0] = buf;

            f.Read(buf, len[0]);

            // guarantee that it will have a trailing 0 for string operations
//            buf.put(len[0], (byte) 0);
            CloseFile(f);

            // if we are journalling and it is a config file, write it to the journal file
            if (isConfig && (eventLoop != null) && (eventLoop.JournalLevel() == 1)) {
                common.DPrintf("Writing %s to journal file.\n", relativePath);
                eventLoop.com_journalDataFile.WriteInt(len[0]);
                eventLoop.com_journalDataFile.Write(buf, len[0]);
                eventLoop.com_journalDataFile.Flush();
            }

            return len[0];
        }

        @Override
        public int ReadFile(String relativePath, ByteBuffer[] buffer) {
            return ReadFile(relativePath, buffer, null);

        }

        @Override
        public void FreeFile(Object[] buffer) {
            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }
            if (null == buffer) {
                common.FatalError("idFileSystemLocal::FreeFile( null )");
            }
            this.loadStack--;

//            Heap.Mem_Free(buffer);
            buffer[0] = null;
        }

        /*
         ============
         idFileSystemLocal::WriteFile

         Filenames are relative to the search path
         ============
         */
        @Override
        public int WriteFile(String relativePath, ByteBuffer buffer, int size) {
            return WriteFile(relativePath, buffer, size, "fs_savepath");
        }

        @Override
        public int WriteFile(String relativePath, ByteBuffer buffer, int size, String basePath /*"fs_savepath"*/) {
            idFile f;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if ((null == relativePath) || (null == buffer)) {
                common.FatalError("idFileSystemLocal::WriteFile: NULL parameter");
            }

            f = this.OpenFileWrite(relativePath, basePath);
            if (null == f) {
                common.Printf("Failed to open %s\n", relativePath);
                return -1;
            }

            size = f.Write(buffer, size);

            CloseFile(f);

            return size;
        }

        @Override
        public void RemoveFile(String relativePath) {
            idStr OSPath;

            if (!fs_devpath.GetString().isEmpty()) {
                OSPath = new idStr(BuildOSPath(fs_devpath.GetString(), this.gameFolder.toString(), relativePath));
                remove(OSPath);
            }

            OSPath = new idStr(BuildOSPath(fs_savepath.GetString(), this.gameFolder.toString(), relativePath));
            remove(OSPath);

            ClearDirCache();
        }

        /*
         ===========
         idFileSystemLocal::OpenFileReadFlags

         Finds the file in the search path, following search flag recommendations
         Returns filesize and an open FILE pointer.
         Used for streaming data out of either a
         separate file or a ZIP file.
         ===========
         */
        public idFile OpenFileReadFlags(String relativePath, int searchFlags) {
            return OpenFileReadFlags(relativePath, searchFlags, null);
        }

        public idFile OpenFileReadFlags(String relativePath, int searchFlags, pack_t[] foundInPak /*= NULL*/) {
            return OpenFileReadFlags(relativePath, searchFlags, foundInPak, true);
        }

        public idFile OpenFileReadFlags(String relativePath, int searchFlags, pack_t[] foundInPak /*= NULL*/, boolean allowCopyFiles /*= true*/) {
            return OpenFileReadFlags(relativePath, searchFlags, foundInPak, allowCopyFiles, null);
        }

        public idFile OpenFileReadFlags(String relativePath, int searchFlags, pack_t[] foundInPak /*= NULL*/, boolean allowCopyFiles /*= true*/, final String gamedir /*= NULL*/) {
            searchpath_s search;
            idStr netpath;
            pack_t pak;
            fileInPack_s pakFile;
            directory_t dir;
            long hash;
            FileChannel fp;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (null == relativePath) {
                common.FatalError("idFileSystemLocal::OpenFileRead: null 'relativePath' parameter passed\n");
            }

            if (foundInPak != null) {
                foundInPak[0] = null;
            }

            // qpaths are not supposed to have a leading slash
            if ((relativePath.charAt(0) == '/') || (relativePath.charAt(0) == '\\')) {//TODO: regex
                relativePath = relativePath.substring(1);
            }

            // make absolutely sure that it can't back up the path.
            // The searchpaths do guarantee that something will always
            // be prepended, so we don't need to worry about "c:" or "//limbo" 
            if (relativePath.contains("..") || relativePath.contains("::")) {//TODO: regex
                return null;
            }

            // edge case
            if (relativePath.isEmpty()/*[0] == '\0'*/) {
                return null;
            }

            // make sure the doomkey file is only readable by game at initialization
            // any other time the key should only be accessed in memory using the provided functions
            if (common.IsInitialized() && ((idStr.Icmp(relativePath, CDKEY_FILE) == 0) || (idStr.Icmp(relativePath, XPKEY_FILE) == 0))) {
                return null;
            }

            //
            // search through the path, one element at a time
            //
            hash = HashFileName(relativePath);

            for (search = this.searchPaths; search != null; search = search.next) {
                if ((search.dir != null) && ((searchFlags & FSFLAG_SEARCH_DIRS) != 0)) {
                    // check a file in the directory tree

                    // if we are running restricted, the only files we
                    // will allow to come from the directory are .cfg files
                    if (fs_restrict.GetBool() || (this.serverPaks.Num() != 0)) {
                        if (!FileAllowedFromDir(relativePath)) {
                            continue;
                        }
                    }

                    dir = search.dir;

                    if ((gamedir != null) && !gamedir.isEmpty()) {
                        if (!dir.gamedir.equals(gamedir)) {
                            continue;
                        }
                    }

                    netpath = new idStr(BuildOSPath(dir.path.toString(), dir.gamedir.toString(), relativePath));
                    fp = OpenOSFileCorrectName(netpath, "rb");
                    if (NOT(fp)) {
                        continue;
                    }

                    final idFile_Permanent file = new idFile_Permanent();
                    file.o = fp;
                    file.name.oSet(relativePath);
                    file.fullPath = netpath;
                    file.mode = (1 << etoi(FS_READ));
                    file.fileSize = (int) DirectFileLength(file.o);
                    if (fs_debug.GetInteger() != 0) {
                        common.Printf("idFileSystem::OpenFileRead: %s (found in '%s/%s')\n", relativePath, dir.path.toString(), dir.gamedir.toString());
                    }

                    if (!this.loadedFileFromDir && !FileAllowedFromDir(relativePath)) {
                        if (this.restartChecksums.Num() != 0) {
                            common.FatalError("'%s' loaded from directory: Failed to restart with pure mode restrictions for server connect", relativePath);
                        }
                        common.DPrintf("filesystem: switching to pure mode will require a restart. '%s' loaded from directory.\n", relativePath);
                        this.loadedFileFromDir = true;
                    }

                    // if fs_copyfiles is set
                    if (allowCopyFiles && (fs_copyfiles.GetInteger() != 0)) {

                        idStr copypath;
                        final idStr name = new idStr();
                        copypath = new idStr(BuildOSPath(fs_savepath.GetString(), dir.gamedir.toString(), relativePath));
                        netpath.ExtractFileName(name);
                        copypath.StripFilename();
                        copypath.Append(PATHSEPERATOR_STR);
                        copypath.Append(name);

                        final boolean isFromCDPath = (0 == dir.path.Cmp(fs_cdpath.GetString()));
                        final boolean isFromSavePath = (0 == dir.path.Cmp(fs_savepath.GetString()));
                        final boolean isFromBasePath = (0 == dir.path.Cmp(fs_basepath.GetString()));

                        switch (fs_copyfiles.GetInteger()) {
                            case 1:
                                // copy from cd path only
                                if (isFromCDPath) {
                                    CopyFile(netpath.toString(), copypath.toString());
                                }
                                break;
                            case 2:
                                // from cd path + timestamps
                                if (isFromCDPath) {
                                    CopyFile(netpath.toString(), copypath.toString());
                                } else if (isFromSavePath || isFromBasePath) {
                                    idStr sourcepath;
                                    sourcepath = new idStr(BuildOSPath(fs_cdpath.GetString(), dir.gamedir.toString(), relativePath));
                                    final long t1 = Sys_FileTimeStamp(sourcepath.toString());
                                    final long t2 = Sys_FileTimeStamp(copypath.toString());
                                    if (t1 > t2) {
                                        CopyFile(sourcepath.toString(), copypath.toString());
                                    }
                                }
                                break;
                            case 3:
                                if (isFromCDPath || isFromBasePath) {
                                    CopyFile(netpath.toString(), copypath.toString());
                                }
                                break;
                            case 4:
                                if (isFromCDPath && !isFromBasePath) {
                                    CopyFile(netpath.toString(), copypath.toString());
                                }
                                break;
                        }
                    }

                    return file;
                } else if ((search.pack != null) && ((searchFlags & FSFLAG_SEARCH_PAKS) != 0)) {

                    if (null == search.pack.hashTable[(int) hash]) {
                        continue;
                    }

                    // disregard if it doesn't match one of the allowed pure pak files
                    if (this.serverPaks.Num() != 0) {
                        GetPackStatus(search.pack);
                        if ((search.pack.pureStatus != PURE_NEVER) && (null == this.serverPaks.Find(search.pack))) {
                            continue; // not on the pure server pak list
                        }
                    }

                    // look through all the pak file elements
                    pak = search.pack;

                    if ((searchFlags & FSFLAG_BINARY_ONLY) != 0) {
                        // make sure this pak is tagged as a binary file
                        if (pak.binary == BINARY_UNKNOWN) {
                            int confHash;
//					fileInPack_s	pakFile;
                            confHash = (int) HashFileName(BINARY_CONFIG);
                            pak.binary = BINARY_NO;
                            for (pakFile = search.pack.hashTable[confHash]; pakFile != null; pakFile = pakFile.next) {
                                if (FilenameCompare(pakFile.name.toString(), BINARY_CONFIG)) {
                                    pak.binary = BINARY_YES;
                                    break;
                                }
                            }
                        }
                        if (pak.binary == BINARY_NO) {
                            continue; // not a binary pak, skip
                        }
                    }

                    for (pakFile = pak.hashTable[(int) hash]; pakFile != null; pakFile = pakFile.next) {
                        // case and separator insensitive comparisons
                        if (FilenameCompare(pakFile.name.toString(), relativePath)) {
                            final idFile_InZip file = ReadFileFromZip(pak, pakFile, relativePath);

                            if (foundInPak != null) {
                                foundInPak[0] = pak;
                            }

                            if (!pak.referenced && (0 == (searchFlags & FSFLAG_PURE_NOREF))) {
                                // mark this pak referenced
                                if (fs_debug.GetInteger() != 0) {
                                    common.Printf("idFileSystem::OpenFileRead: %s . adding %s to referenced paks\n", relativePath, pak.pakFilename.toString());
                                }
                                pak.referenced = true;
                            }

                            if (fs_debug.GetInteger() != 0) {
                                common.Printf("idFileSystem::OpenFileRead: %s (found in '%s')\n", relativePath, pak.pakFilename.toString());
                            }
                            return file;
                        }
                    }
                }
            }

            if ((searchFlags & FSFLAG_SEARCH_ADDONS) != 0) {
                for (search = this.addonPaks; search != null; search = search.next) {
                    assert (search.pack != null);
//			fileInPack_s	pakFile;
                    pak = search.pack;
                    for (pakFile = pak.hashTable[(int) hash]; pakFile != null; pakFile = pakFile.next) {
                        if (FilenameCompare(pakFile.name.toString(), relativePath)) {
                            final idFile_InZip file = ReadFileFromZip(pak, pakFile, relativePath);
                            if (foundInPak != null) {
                                foundInPak[0] = pak;
                            }
                            // we don't toggle pure on paks found in addons - they can't be used without a reloadEngine anyway
                            if (fs_debug.GetInteger() != 0) {
                                common.Printf("idFileSystem::OpenFileRead: %s (found in addon pk4 '%s')\n", relativePath, search.pack.pakFilename.toString());
                            }
                            return file;
                        }
                    }
                }
            }

            if (fs_debug.GetInteger() != 0) {
                common.Printf("Can't find %s\n", relativePath);
            }

            return null;
        }

        @Override
        public idFile OpenFileRead(String relativePath) {
            return OpenFileRead(relativePath, true);
        }

        @Override
        public idFile OpenFileRead(String relativePath, boolean allowCopyFiles) {
            return OpenFileRead(relativePath, allowCopyFiles, null);
        }

        @Override
        public idFile OpenFileRead(String relativePath, boolean allowCopyFiles, String gamedir) {
            return OpenFileReadFlags(relativePath, FSFLAG_SEARCH_DIRS | FSFLAG_SEARCH_PAKS, null, allowCopyFiles, gamedir);
        }

        @Override
        public idFile OpenFileWrite(String relativePath, String basePath) {
            String path;
            String OSpath;
            idFile_Permanent f;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            path = cvarSystem.GetCVarString(basePath);
            if (path.isEmpty()) {//TODO:check null
                path = fs_savepath.GetString();
            }

            OSpath = BuildOSPath(path, this.gameFolder.toString(), relativePath);

            if (fs_debug.GetInteger() != 0) {
                common.Printf("idFileSystem::OpenFileWrite: %s\n", OSpath);
            }

            // if the dir we are writing to is in our current list, it will be outdated
            // so just flush everything
            ClearDirCache();

            common.DPrintf("writing to: %s\n", OSpath);
            CreateOSPath(OSpath);

            f = new idFile_Permanent();
            f.o = OpenOSFile(OSpath, "wb");
            if (NOT(f.o)) {
//		delete f;
                return null;
            }
            f.name.oSet(relativePath);
            f.fullPath.oSet(OSpath);
            f.mode = (1 << FS_WRITE.ordinal());
            f.handleSync = false;
            f.fileSize = 0;

            return f;
        }

        @Override
        public idFile OpenFileAppend(String filename) {
            return OpenFileAppend(filename, false);

        }

        @Override
        public idFile OpenFileAppend(String filename, boolean sync) {
            return OpenFileAppend(filename, sync, "fs_basepath");

        }

        @Override
        public idFile OpenFileAppend(String filename, boolean sync, String basePath /*= "fs_basepath"*/) {
            String path;
            String OSpath;
            idFile_Permanent f;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            path = cvarSystem.GetCVarString(basePath);
            if (!path.isEmpty()) {
                path = fs_savepath.GetString();
            }

            OSpath = BuildOSPath(path, this.gameFolder.toString(), filename);
            CreateOSPath(OSpath);

            if (fs_debug.GetInteger() != 0) {
                common.Printf("idFileSystem::OpenFileAppend: %s\n", OSpath);
            }

            f = new idFile_Permanent();
            f.o = OpenOSFile(OSpath, "ab");
            if (NOT(f.o)) {
//		delete f;
                return null;
            }
            f.name.oSet(filename);
            f.fullPath.oSet(OSpath);
            f.mode = (1 << etoi(FS_WRITE)) + (1 << etoi(FS_APPEND));
            f.handleSync = sync;
            f.fileSize = (int) DirectFileLength(f.o);

            return f;
        }

        @Override
        public idFile OpenFileByMode(String relativePath, fsMode_t mode) {
            if (mode == FS_READ) {
                return OpenFileRead(relativePath);
            }
            if (mode == FS_WRITE) {
                return OpenFileWrite(relativePath);
            }
            if (mode == FS_APPEND) {
                return OpenFileAppend(relativePath, true);
            }
            common.FatalError("idFileSystemLocal::OpenFileByMode: bad mode");
            return null;
        }

        @Override
        public idFile OpenExplicitFileRead(String OSPath) {
            idFile_Permanent f;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (fs_debug.GetInteger() != 0) {
                common.Printf("idFileSystem::OpenExplicitFileRead: %s\n", OSPath);
            }

            common.DPrintf("idFileSystem::OpenExplicitFileRead - reading from: %s\n", OSPath);

            f = new idFile_Permanent();
            f.o = OpenOSFile(OSPath, "rb");
            if (NOT(f.o)) {
//		delete f;
                return null;
            }
            f.name.oSet(OSPath);
            f.fullPath = new idStr(OSPath);
            f.mode = (1 << etoi(FS_READ));
            f.handleSync = false;
            f.fileSize = (int) DirectFileLength(f.o);

            return f;
        }

        @Override
        public idFile OpenExplicitFileWrite(String OSPath) {
            idFile_Permanent f;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (fs_debug.GetInteger() != 0) {
                common.Printf("idFileSystem::OpenExplicitFileWrite: %s\n", OSPath);
            }

            common.DPrintf("writing to: %s\n", OSPath);
            CreateOSPath(OSPath);

            f = new idFile_Permanent();
            f.o = OpenOSFile(OSPath, "wb");
            if (NOT(f.o)) {
//		delete f;
                return null;
            }
            f.name.oSet(OSPath);
            f.fullPath.oSet(OSPath);
            f.mode = (1 << etoi(FS_WRITE));
            f.handleSync = false;
            f.fileSize = 0;

            return f;
        }

        @Override
        public void CloseFile(idFile f) {
            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }
//	delete f;
        }

        @Override
        public void BackgroundDownload(backgroundDownload_s bgl) {
            if (bgl.opcode == DLTYPE_FILE) {
                if ( /*dynamic_cast<idFile_Permanent *>*/(bgl.f) != null) {
                    // add the bgl to the background download list
                    Sys_EnterCriticalSection();
                    bgl.next = this.backgroundDownloads;
                    this.backgroundDownloads = bgl;
                    Sys_TriggerEvent();
                    Sys_LeaveCriticalSection();
                } else {
                    // read zipped file directly
                    bgl.f.Seek(bgl.file.position, FS_SEEK_SET);
                    bgl.f.Read(bgl.file.buffer, bgl.file.length);
                    bgl.completed = true;
                }
            } else {
                Sys_EnterCriticalSection();
                bgl.next = this.backgroundDownloads;
                this.backgroundDownloads = bgl;
                Sys_TriggerEvent();
                Sys_LeaveCriticalSection();
            }
        }

        @Override
        public void ResetReadCount() {
            this.readCount = 0;
        }

        @Override
        public int GetReadCount() {
            return this.readCount;
        }

        @Override
        public void AddToReadCount(int c) {
            this.readCount += c;
        }

        @Override
        public void FindDLL(String basename, char[] _dllPath, boolean updateChecksum) {
            idFile dllFile = null;
//            char[] __dllName = new char[MAX_OSPATH];
            final String[] __dllName = {null};
            idStr dllPath = new idStr();
            long dllHash;
            final pack_t[] inPak = new pack_t[1];
            pack_t pak;
            fileInPack_s pakFile;
            String dllName = null;

            sys.DLL_GetFileName("" + basename, __dllName, MAX_OSPATH);
            dllHash = HashFileName(__dllName[0]);

// #if ID_FAKE_PURE
            // if ( 1 ) {
// #else
            if (0 == this.serverPaks.Num()) {
// #endif
                // from executable directory first - this is handy for developement
                dllName = __dllName[0];
                dllPath.oSet(Sys_EXEPath());
                dllPath.StripFilename();
                dllPath.AppendPath(dllName);
                dllFile = OpenExplicitFileRead(dllPath.toString());
            }
            if (null == dllFile) {
                if (0 == this.serverPaks.Num()) {
                    // not running in pure mode, try to extract from a pak file first
                    dllFile = OpenFileReadFlags(dllName, FSFLAG_SEARCH_PAKS | FSFLAG_PURE_NOREF | FSFLAG_BINARY_ONLY, inPak);
                    if (dllFile != null) {
                        common.Printf("found DLL in pak file: %s\n", dllFile.GetFullPath());
                        dllPath = new idStr(RelativePathToOSPath(dllName, "fs_savepath"));
                        CopyFile(dllFile, dllPath.toString());
                        CloseFile(dllFile);
                        dllFile = OpenFileReadFlags(dllName, FSFLAG_SEARCH_DIRS);
                        if (null == dllFile) {
                            common.Error("DLL extraction to fs_savepath failed\n");
                        } else if (updateChecksum) {
                            this.gameDLLChecksum = GetFileChecksum(dllFile);
                            this.gamePakChecksum = inPak[0].checksum;
                            updateChecksum = false;	// don't try again below
                        }
                    } else {
                        // didn't find a source in a pak file, try in the directory
                        dllFile = OpenFileReadFlags(dllName, FSFLAG_SEARCH_DIRS);
                        if (dllFile != null) {
                            if (updateChecksum) {
                                final int[] gameDLLChecksum = {this.gameDLLChecksum = GetFileChecksum(dllFile)};
                                // see if we can mark a pak file
                                pak = FindPakForFileChecksum(dllName, gameDLLChecksum, false);
                                this.gameDLLChecksum = gameDLLChecksum[0];
                                this.gamePakChecksum = pak != null ? pak.checksum : 0;
                                updateChecksum = false;
                            }
                        }
                    }
                } else {
                    // we are in pure mode. this path to be reached only for game DLL situations
                    // with a code pak checksum given by server
                    assert (this.gamePakChecksum != 0);
                    assert (updateChecksum);
                    pak = GetPackForChecksum(this.gamePakChecksum);
                    if (null == pak) {
                        // not supposed to happen, bug in pure code?
                        common.Warning("FindDLL in pure mode: game pak not found ( 0x%x )\n", this.gamePakChecksum);
                    } else {
                        // extract and copy
                        for (pakFile = pak.hashTable[(int) dllHash]; pakFile != null; pakFile = pakFile.next) {
                            if (FilenameCompare(pakFile.name.toString(), dllName)) {
                                dllFile = ReadFileFromZip(pak, pakFile, dllName);
                                common.Printf("found DLL in game pak file: %s\n", pak.pakFilename.toString());
                                dllPath = new idStr(RelativePathToOSPath(dllName, "fs_savepath"));
                                CopyFile(dllFile, dllPath.toString());
                                CloseFile(dllFile);
                                dllFile = OpenFileReadFlags(dllName, FSFLAG_SEARCH_DIRS);
                                if (null == dllFile) {
                                    common.Error("DLL extraction to fs_savepath failed\n");
                                } else {
                                    this.gameDLLChecksum = GetFileChecksum(dllFile);
                                    updateChecksum = false;	// don't try again below
                                }
                            }
                        }
                    }
                }
            }
            if (updateChecksum) {
                if (dllFile != null) {
                    this.gameDLLChecksum = GetFileChecksum(dllFile);
                } else {
                    this.gameDLLChecksum = 0;
                }
                this.gamePakChecksum = 0;
            }
            if (dllFile != null) {
                dllPath = new idStr(dllFile.GetFullPath());
                CloseFile(dllFile);
//                dllFile = null;
            } else {
                dllPath = new idStr();
            }
            idStr.snPrintf(_dllPath, MAX_OSPATH, dllPath.toString());
        }

        @Override
        public void ClearDirCache() {
            int i;

            this.dir_cache_index = 0;
            this.dir_cache_count = 0;
            for (i = 0; i < MAX_CACHED_DIRS; i++) {
                this.dir_cache[i].Clear();
            }
        }

        @Override
        public boolean HasD3XP() {
            int i;
            final idStrList dirs = new idStrList()/*, pk4s*/;
            String gamepath;

            if (this.d3xp == -1) {
                return false;
            } else if (this.d3xp == 1) {
                return true;
            }
//	
//#if 0
            /*// check for a d3xp directory with a pk4 file
             * // copied over from ListMods - only looks in basepath
             * ListOSFiles( fs_basepath.GetString(), "/", dirs );
             * for ( i = 0; i < dirs.Num(); i++ ) {
             * if ( dirs[i].Icmp( "d3xp" ) == 0 ) {
             * gamepath = BuildOSPath( fs_basepath.GetString(), dirs[ i ], "" );
             * ListOSFiles( gamepath, ".pk4", pk4s );
             * if ( pk4s.Num() ) {
             * d3xp = 1;
             * return true;
             * }
             * }
             * }*/
//#elif ID_ALLOW_D3XP
            // check for d3xp's d3xp/pak000.pk4 in any search path
            // checking wether the pak is loaded by checksum wouldn't be enough:
            // we may have a different fs_game right now but still need to reply that it's installed
            final String[] search = new String[4];
            idFile pakfile;
            search[0] = fs_savepath.GetString();
            search[1] = fs_devpath.GetString();
            search[2] = fs_basepath.GetString();
            search[3] = fs_cdpath.GetString();
            for (i = 0; i < 4; i++) {
                pakfile = OpenExplicitFileRead(BuildOSPath(search[i], "d3xp", "pak000.pk4"));
                if (pakfile != null) {
                    CloseFile(pakfile);
                    this.d3xp = 1;
                    return true;
                }
            }
//#endif
//
//#if ID_ALLOW_D3XP
            // if we didn't find a pk4 file then the user might have unpacked so look for default.cfg file
            // that's the old way mostly used during developement. don't think it hurts to leave it there
            ListOSFiles(fs_basepath.GetString(), "/", dirs);
            for (i = 0; i < dirs.Num(); i++) {
                if (dirs.oGet(i).Icmp("d3xp") == 0) {

                    gamepath = BuildOSPath(fs_savepath.GetString(), dirs.oGet(i).toString(), "default.cfg");
                    final idFile cfg = OpenExplicitFileRead(gamepath);
                    if (cfg != null) {
                        CloseFile(cfg);
                        this.d3xp = 1;
                        return true;
                    }
                }
            }
//#endif
            this.d3xp = -1;
            return false;
        }

        @Override
        public boolean RunningD3XP() {
            // TODO: mark the checksum of the gold XP and check for it being referenced ( for double mod support )
            // a simple fs_game check should be enough for now..
            if ((0 == idStr.Icmp(fs_game.GetString(), "d3xp"))
                    || (0 == idStr.Icmp(fs_game_base.GetString(), "d3xp"))) {
                return true;
            }
            return false;
        }

        /*
         =================
         idFileSystemLocal::CopyFile

         Copy a fully specified file from one place to another
         =================
         */
        @Override
        public void CopyFile(String fromOSPath, String toOSPath) {
            FileChannel f;
            long len;
            ByteBuffer buf;

            common.Printf("copy %s to %s\n", fromOSPath, toOSPath);
            f = OpenOSFile(fromOSPath, "rb");
            if (NOT(f)) {
                return;
            }
            try {
//            fseek(f, 0, SEEK_END);
//            len = ftell(f);
//            fseek(f, 0, SEEK_SET);
                len = f.size();

                buf = ByteBuffer.allocate((int) len);
                if (f.read(buf) != len) {
//            if (fread(buf, 1, len, f) != len) {
                    common.FatalError("short read in idFileSystemLocal::CopyFile()\n");
                }
                f.close();

                CreateOSPath(toOSPath);
                f = OpenOSFile(toOSPath, "wb");
                if (NOT(f)) {
                    common.Printf("could not create destination file\n");
//                Heap.Mem_Free(buf);
                    return;
                }
                if (f.write(buf) != len) {
//            if (fwrite(buf, 1, len, f) != len) {
                    common.FatalError("short write in idFileSystemLocal::CopyFile()\n");
                }
                f.close();
//            Heap.Mem_Free(buf);
            } catch (final IOException ex) {
                Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public int ValidateDownloadPakForChecksum(int checksum, char[] path, boolean isBinary) {
            int i;
            final idStrList testList = new idStrList();
            idStr name;
            final idStr relativePath = new idStr();
            boolean pakBinary;
            final pack_t pak = GetPackForChecksum(checksum);

            if (NOT(pak)) {
                return 0;
            }

            // validate this pak for a potential download
            // ignore pak*.pk4 for download. those are reserved to distribution and cannot be downloaded
            name = pak.pakFilename;
            name.StripPath();
            if (name.toString().startsWith("pak")) {
                common.DPrintf("%s is not a donwloadable pak\n", pak.pakFilename.toString());
                return 0;
            }
            // check the binary
            // a pure server sets the binary flag when starting the game
            assert (pak.binary != BINARY_UNKNOWN);
            pakBinary = (pak.binary == BINARY_YES);
            if (isBinary != pakBinary) {
                common.DPrintf("%s binary flag mismatch\n", pak.pakFilename.toString());
                return 0;
            }

            // extract a path that includes the fs_game: != OSPathToRelativePath
            testList.Append(fs_savepath.GetString());
            testList.Append(fs_devpath.GetString());
            testList.Append(fs_basepath.GetString());
            testList.Append(fs_cdpath.GetString());
            for (i = 0; i < testList.Num(); i++) {
                if ((testList.oGet(i).Length() != 0)
                        && NOT(testList.oGet(i).Icmpn(pak.pakFilename.toString(), testList.oGet(i).Length()))) {
                    relativePath.oSet(pak.pakFilename.toString().substring(testList.oGet(i).Length() + 1));
                    break;
                }
            }
            if (i == testList.Num()) {
                common.Warning("idFileSystem::ValidateDownloadPak: failed to extract relative path for %s", pak.pakFilename.toString());
                return 0;
            }
            idStr.Copynz(path, relativePath.c_str(), MAX_STRING_CHARS);
            return pak.length;
        }

        @Override
        public idFile MakeTemporaryFile() {
            FileChannel f;

            try {
                f = tmpfile();
//            if (NOT(f)) {
            } catch (final IOException e) {
                common.Warning("idFileSystem::MakeTemporaryFile failed: %s", e.getMessage());// strerror(System.err));
                return null;
            }
            final idFile_Permanent file = new idFile_Permanent();
            file.o = f;
            file.name.oSet("<tempfile>");
            file.fullPath.oSet("<tempfile>");
            file.mode = (1 << etoi(FS_READ)) + (1 << etoi(FS_WRITE));
            file.fileSize = 0;
            return file;
        }

        /*
         ===============
         idFileSystemLocal::AddZipFile
         adds a downloaded pak file to the list so we can work out what we have and what we still need
         the isNew flag is set to true, indicating that we cannot add this pak to the search lists without a restart
         ===============
         */
        @Override
        public int AddZipFile(String path) {
            final idStr fullpath = new idStr(fs_savepath.GetString());
            pack_t pak;
            searchpath_s search, last;

            fullpath.AppendPath(path);
            pak = LoadZipFile(fullpath.toString());
            if (null == pak) {
                common.Warning("AddZipFile %s failed\n", path);
                return 0;
            }
            // insert the pak at the end of the search list - temporary until we restart
            pak.isNew = true;
            search = new searchpath_s();
            search.dir = null;
            search.pack = pak;
            search.next = null;
            last = this.searchPaths;
            while (last.next != null) {
                last = last.next;
            }
            last.next = search;
            common.Printf("Appended pk4 %s with checksum 0x%x\n", pak.pakFilename.toString() + pak.checksum);
            return pak.checksum;
        }

        @Override
        public findFile_t FindFile(String path, boolean scheduleAddons) {
            final pack_t[] pak = new pack_t[1];
            final idFile f = OpenFileReadFlags(path, FSFLAG_SEARCH_DIRS | FSFLAG_SEARCH_PAKS | FSFLAG_SEARCH_ADDONS, pak);
            if (null == f) {
                return FIND_NO;
            }
            if (null == pak[0]) {
                // found in FS, not even in paks
                return FIND_YES;
            }
            // marking addons for inclusion on reload - may need to do that even when already in the search path
            if (scheduleAddons && pak[0].addon && (this.addonChecksums.FindIndex(pak[0].checksum) < 0)) {
                this.addonChecksums.Append(pak[0].checksum);
            }
            // an addon that's not on search list yet? that will require a restart
            if (pak[0].addon && !pak[0].addon_search) {
//		delete f;
                return FIND_ADDON;
            }
//	delete f;
            return FIND_YES;
        }

        /*
         ===============
         idFileSystemLocal::GetNumMaps
         account for actual decls and for addon maps
         ===============
         */
        @Override
        public int GetNumMaps() {
            int i;
            searchpath_s search = null;
            int ret = declManager.GetNumDecls(DECL_MAPDEF);

            // add to this all addon decls - coming from all addon packs ( searched or not )
            for (i = 0; i < 2; i++) {
                if (i == 0) {
                    search = this.searchPaths;
                } else if (i == 1) {
                    search = this.addonPaks;
                }
                for (; search != null; search = search.next) {
                    if ((null == search.pack) || !search.pack.addon || (null == search.pack.addon_info)) {
                        continue;
                    }
                    ret += search.pack.addon_info.mapDecls.Num();
                }
            }
            return ret;
        }

        /*
         ===============
         idFileSystemLocal::GetMapDecl
         retrieve the decl dictionary, add a 'path' value
         ===============
         */
        @Override
        public idDict GetMapDecl(int idecl) {
            int i;
            idDecl mapDecl;
            idDeclEntityDef mapDef;
            final int numdecls = declManager.GetNumDecls(DECL_MAPDEF);
            searchpath_s search = null;

            if (idecl < numdecls) {
                mapDecl = declManager.DeclByIndex(DECL_MAPDEF, idecl);
                mapDef = (idDeclEntityDef) mapDecl;
                if (NOT(mapDef)) {
                    common.Error("idFileSystemLocal::GetMapDecl %d: not found\n", idecl);
                }
                this.mapDict = mapDef.dict;
                this.mapDict.Set("path", mapDef.GetName());
                return this.mapDict;
            }
            idecl -= numdecls;
            for (i = 0; i < 2; i++) {
                if (i == 0) {
                    search = this.searchPaths;
                } else if (i == 1) {
                    search = this.addonPaks;
                }
                for (; search != null; search = search.next) {
                    if ((null == search.pack) || !search.pack.addon || (null == search.pack.addon_info)) {
                        continue;
                    }
                    // each addon may have a bunch of map decls
                    if (idecl < search.pack.addon_info.mapDecls.Num()) {
                        this.mapDict = search.pack.addon_info.mapDecls.oGet(idecl);
                        return this.mapDict;
                    }
                    idecl -= search.pack.addon_info.mapDecls.Num();
                    assert (idecl >= 0);
                }
            }
            return null;
        }

        @Override
        public void FindMapScreenshot(String path, String[] buf, int len) {
            idFile file;
            final idStr mapname = new idStr(path);

            mapname.StripPath();
            mapname.StripFileExtension();

            idStr.snPrintf(buf, len, "guis/assets/splash/%s.tga", mapname.toString());
            if (ReadFile(buf[0], null, null) == -1) {
                // try to extract from an addon
                file = OpenFileReadFlags(buf[0], FSFLAG_SEARCH_ADDONS);
                if (file != null) {
                    // save it out to an addon splash directory
                    final int dlen = file.Length();
                    final ByteBuffer data = ByteBuffer.allocate(dlen);
                    file.Read(data, dlen);
                    CloseFile(file);
                    idStr.snPrintf(buf, len, "guis/assets/splash/addon/%s.tga", mapname.toString());
                    WriteFile(buf[0], data, dlen);
//			delete[] data;
                } else {
                    idStr.Copynz(buf, "guis/assets/splash/pdtempa", len);
                }
            }
        }

        /*
         ===========
         idFileSystemLocal::FilenameCompare

         Ignore case and separator char distinctions
         ===========
         */
        @Override
        public boolean FilenameCompare(String s1, String s2) {
            return Paths.get(s1).equals(Paths.get(s2));
        }

        public static class Dir_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Dir_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            private Dir_f() {
            }

            @Override
            public void run(idCmdArgs args) {
                idStr relativePath;
                idStr extension;
                idFileList fileList;
                int i;

                if ((args.Argc() < 2) || (args.Argc() > 3)) {
                    common.Printf("usage: dir <directory> [extension]\n");
                    return;
                }

                if (args.Argc() == 2) {
                    relativePath = new idStr(args.Argv(1));
                    extension = new idStr();
                } else {
                    relativePath = new idStr(args.Argv(1));
                    extension = new idStr(args.Argv(2));
                    if (extension.oGet(0) != '.') {
                        common.Warning("extension should have a leading dot");
                    }
                }
                relativePath.BackSlashesToSlashes();
                relativePath.StripTrailing('/');

                common.Printf("Listing of %s/*%s\n", relativePath.toString(), extension.toString());
                common.Printf("---------------\n");

                fileList = fileSystemLocal.ListFiles(relativePath.toString(), extension.toString());

                for (i = 0; i < fileList.GetNumFiles(); i++) {
                    common.Printf("%s\n", fileList.GetFile(i));
                }
                common.Printf("%d files\n", fileList.list.Num());

                fileSystemLocal.FreeFileList(fileList);
            }
        }

        public static class DirTree_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new DirTree_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            private DirTree_f() {
            }

            @Override
            public void run(idCmdArgs args) {
                idStr relativePath;
                idStr extension;
                idFileList fileList;
                int i;

                if ((args.Argc() < 2) || (args.Argc() > 3)) {
                    common.Printf("usage: dirtree <directory> [extension]\n");
                    return;
                }

                if (args.Argc() == 2) {
                    relativePath = new idStr(args.Argv(1));
                    extension = new idStr();
                } else {
                    relativePath = new idStr(args.Argv(1));
                    extension = new idStr(args.Argv(2));
                    if (extension.oGet(0) != '.') {
                        common.Warning("extension should have a leading dot");
                    }
                }
                relativePath.BackSlashesToSlashes();
                relativePath.StripTrailing('/');

                common.Printf("Listing of %s/*%s /s\n", relativePath.toString(), extension.toString());
                common.Printf("---------------\n");

                fileList = fileSystemLocal.ListFilesTree(relativePath.toString(), extension.toString());

                for (i = 0; i < fileList.GetNumFiles(); i++) {
                    common.Printf("%s\n", fileList.GetFile(i));
                }
                common.Printf("%d files\n" + fileList.list.Num());

                fileSystemLocal.FreeFileList(fileList);
            }
        }

        public static class Path_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Path_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            private Path_f() {
            }

            @Override
            public void run(idCmdArgs args) {
                searchpath_s sp;
                int i;
                String status;// = "";

                common.Printf("Current search path:\n");
                for (sp = fileSystemLocal.searchPaths; sp != null; sp = sp.next) {
                    if (sp.pack != null) {
                        if (com_developer.GetBool()) {
                            status = String.format("%s (%d files - 0x%x %s", sp.pack.pakFilename, sp.pack.numfiles, sp.pack.checksum, sp.pack.referenced ? "referenced" : "not referenced");
                            if (sp.pack.addon) {
                                status += " - addon)\n";
                            } else {
                                status += ")\n";
                            }
                            common.Printf(status);
                        } else {
                            common.Printf("%s (%d files)\n", sp.pack.pakFilename, sp.pack.numfiles);
                        }
                        if (fileSystemLocal.serverPaks.Num() != 0) {
                            if (fileSystemLocal.serverPaks.Find(sp.pack) != 0) {
                                common.Printf("    on the pure list\n");
                            } else {
                                common.Printf("    not on the pure list\n");
                            }
                        }
                    } else {
                        common.Printf("%s/%s\n", sp.dir.path, sp.dir.gamedir);
                    }
                }
                common.Printf("game DLL: 0x%x in pak: 0x%x\n", fileSystemLocal.gameDLLChecksum, fileSystemLocal.gamePakChecksum);
//#if ID_FAKE_PURE
//	common.Printf( "Note: ID_FAKE_PURE is enabled\n" );
//#endif
                for (i = 0; i < MAX_GAME_OS; i++) {
                    if (fileSystemLocal.gamePakForOS[i] != 0) {
                        common.Printf("OS %d - pak 0x%x\n", i, fileSystemLocal.gamePakForOS[i]);
                    }
                }
                // show addon packs that are *not* in the search lists
                common.Printf("Addon pk4s:\n");
                for (sp = fileSystemLocal.addonPaks; sp != null; sp = sp.next) {
                    if (com_developer.GetBool()) {
                        common.Printf("%s (%d files - 0x%x)\n", sp.pack.pakFilename, sp.pack.numfiles, sp.pack.checksum);
                    } else {
                        common.Printf("%s (%d files)\n", sp.pack.pakFilename, sp.pack.numfiles);
                    }
                }
            }
        }

        /*
         ============
         idFileSystemLocal::TouchFile_f

         The only purpose of this function is to allow game script files to copy
         arbitrary files furing an "fs_copyfiles 1" run.
         ============
         */
        public static class TouchFile_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TouchFile_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            private TouchFile_f() {
            }

            @Override
            public void run(idCmdArgs args) {
                idFile f;

                if (args.Argc() != 2) {
                    common.Printf("Usage: touchFile <file>\n");
                    return;
                }

                f = fileSystemLocal.OpenFileRead(args.Argv(1));
                if (f != null) {
                    fileSystemLocal.CloseFile(f);
                }
            }
        }

        /*
         ============
         idFileSystemLocal::TouchFileList_f

         Takes a text file and touches every file in it, use one file per line.
         ============
         */
        public static class TouchFileList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TouchFileList_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            private TouchFileList_f() {
            }

            @Override
            public void run(idCmdArgs args) {

                if (args.Argc() != 2) {
                    common.Printf("Usage: touchFileList <filename>\n");
                    return;
                }

                final ByteBuffer[] buffer = {null};
                final idParser src = new idParser(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                if ((fileSystem.ReadFile(args.Argv(1), buffer, null) != 0) && (buffer[0] != null)) {
                    src.LoadMemory(new String(buffer[0].array()), buffer[0].capacity(), args.Argv(1));
                    if (src.IsLoaded()) {
                        final idToken token = new idToken();
                        while (src.ReadToken(token)) {
                            common.Printf("%s\n", token.toString());
                            session.UpdateScreen();
                            final idFile f = fileSystemLocal.OpenFileRead(token.toString());
                            if (f != null) {
                                fileSystemLocal.CloseFile(f);
                            }
                        }
                    }
                }

            }
        }
//
//

        /*
         ===================
         BackgroundDownload

         Reads part of a file from a background thread.
         ===================
         */
        private static class BackgroundDownloadThread extends xthread_t {

            public static final xthread_t INSTANCE = new BackgroundDownloadThread();

            private BackgroundDownloadThread() {
            }

            @Override
            public void/*int*/ run(/*Object... parms*/) {
                throw new TODO_Exception();
//                while (true) {
//                    Sys_EnterCriticalSection();
//                    backgroundDownload_t bgl = fileSystemLocal.backgroundDownloads;
//                    if (null == bgl) {
//                        Sys_LeaveCriticalSection();
//                        Sys_WaitForEvent();
//                        continue;
//                    }
//                    // remove this from the list
//                    fileSystemLocal.backgroundDownloads = (backgroundDownload_t) bgl.next;
//                    Sys_LeaveCriticalSection();
//
//                    bgl.next = null;
//
//                    if (bgl.opcode == DLTYPE_FILE) {
//                        // use the low level read function, because fread may allocate memory
////                    if (WIN32) {
////                        _read(((idFile_Permanent) bgl.f).GetFilePtr()._file, bgl.file.buffer, bgl.file.length);
////                    } else {
//                        ((idFile_Permanent) bgl.f).GetFilePtr().read(bgl.file.buffer = ByteBuffer.allocate(bgl.file.length));
////                        fread(bgl.file.buffer, bgl.file.length, 1, ((idFile_Permanent) bgl.f).GetFilePtr());
////                    }
//                        bgl.completed = true;
//                    } else {
//                        if (ID_ENABLE_CURL) {
//                            // DLTYPE_URL
//                            // use a local buffer for curl error since the size define is local
//                            char[] error_buf = new char[CURL_ERROR_SIZE];
//                            bgl.url.dlerror = '\0' + bgl.url.dlerror.substring(1);
//                            CURL session = curl_easy_init();
//                            CURLcode ret;
//                            if (!session) {
//                                bgl.url.dlstatus = CURLE_FAILED_INIT;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_ERRORBUFFER, error_buf);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_URL, bgl.url.url.c_str());
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_FAILONERROR, 1);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_WRITEFUNCTION, idFileSystemLocal.CurlWriteFunction);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_WRITEDATA, bgl);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_NOPROGRESS, 0);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_PROGRESSFUNCTION, idFileSystemLocal.CurlProgressFunction);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            ret = curl_easy_setopt(session, CURLOPT_PROGRESSDATA, bgl);
//                            if (ret) {
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            bgl.url.dlnow = 0;
//                            bgl.url.dltotal = 0;
//                            bgl.url.status = DL_INPROGRESS;
//                            ret = curl_easy_perform(session);
//                            if (ret) {
//                                Sys_Printf("curl_easy_perform failed: %s\n", error_buf);
////				idStr.Copynz( bgl.url.dlerror, error_buf, MAX_STRING_CHARS );
//                                bgl.url.dlerror = new String(error_buf);
//                                bgl.url.dlstatus = ret;
//                                bgl.url.status = DL_FAILED;
//                                bgl.completed = true;
//                                continue;
//                            }
//                            bgl.url.status = DL_DONE;
//                            bgl.completed = true;
//                        } else {
//                            bgl.url.status = DL_FAILED;
//                            bgl.completed = true;
//                        }
//                    }
//                }
//                return 0;
            }
        }

        private void ReplaceSeparators(idStr path) {
            ReplaceSeparators(path, PATHSEPERATOR_CHAR);
        }

        /*
         ====================
         idFileSystemLocal::ReplaceSeparators

         Fix things up differently for win/unix/mac
         ====================
         */
        private void ReplaceSeparators(idStr path, char sep) {
            char[] s;
            int i;

            s = path.c_str();

            for (i = 0; i < s.length; i++) {
                if ((s[i] == '/') || (s[i] == '\\')) {
                    s[i] = sep;
                }
            }
        }

        /*
         ================
         idFileSystemLocal::HashFileName

         return a hash value for the filename
         ================
         */
        private long HashFileName(final String fname) {
            int i;
            long hash;
            char letter;

            hash = 0;
            i = 0;
            while (i < fname.length()) {
                letter = idStr.ToLower(fname.charAt(i));
                if (letter == '.') {
                    break;				// don't include extension
                }
                if (letter == '\\') {
                    letter = '/';		// damn path names
                }
                hash += (long) (letter) * (i + 119);
                i++;
            }
            hash &= (FILE_HASH_SIZE - 1);
            return hash;
        }

        /*
         ===============
         idFileSystemLocal::ListOSFiles

         call to the OS for a listing of files in an OS directory
         optionally, perform some caching of the entries
         ===============
         */
        private int ListOSFiles(final String directory, String extension, idStrList list) {
            int i, j, ret;

            if (null == extension) {
                extension = "";
            }

            if (!fs_caseSensitiveOS.GetBool()) {
                return Sys_ListFiles(directory, extension, list);
            }

            // try in cache
            i = this.dir_cache_index - 1;
            while (i >= (this.dir_cache_index - this.dir_cache_count)) {
                j = (i + MAX_CACHED_DIRS) % MAX_CACHED_DIRS;
                if (this.dir_cache[j].Matches(directory, extension)) {
                    if (fs_debug.GetInteger() != 0) {
                        //common.Printf( "idFileSystemLocal::ListOSFiles: cache hit: %s\n", directory );
                    }
                    list = this.dir_cache[j];
                    return list.Num();
                }
                i--;
            }

            if (fs_debug.GetInteger() != 0) {
                //common.Printf( "idFileSystemLocal::ListOSFiles: cache miss: %s\n", directory );
            }

            ret = Sys_ListFiles(directory, extension, list);

            if (ret == -1) {
                return -1;
            }

            // push a new entry
            this.dir_cache[this.dir_cache_index].Init(directory, extension, list);
            this.dir_cache_index = (++this.dir_cache_index) % MAX_CACHED_DIRS;
            if (this.dir_cache_count < MAX_CACHED_DIRS) {
                this.dir_cache_count++;
            }

            return ret;
        }

        /*
         ================
         idFileSystemLocal::OpenOSFile
         optional caseSensitiveName is set to case sensitive file name as found on disc (fs_caseSensitiveOS only)
         ================
         */
        private FileChannel OpenOSFile(final String fileName, final String mode) {
            return OpenOSFile(fileName, mode, null);

        }

        private FileChannel OpenOSFile(final String fileName, final String mode, idStr caseSensitiveName /*= NULL*/) {
            int i;
            Path fp;
            idStr fpath, entry;
            final idStrList list = new idStrList();

//if( __MWERKS__&&
// WIN32 ){
//	// some systems will let you fopen a directory
//	struct stat buf;
//	if ( stat( fileName, &buf ) != -1 && !S_ISREG(buf.st_mode) ) {
//		return NULL;
//	}
//}
            try {
                fp = Paths.get(fileName);//fp = fopen(fileName, mode);
            } catch (final InvalidPathException e) {
                //log something.
                fp = Paths.get("/" + UUID.randomUUID());
            }

            if (Files.notExists(fp, NOFOLLOW_LINKS)
                    && fs_caseSensitiveOS.GetBool()) {
                fpath = new idStr(fileName);
                fpath.StripFilename();
                fpath.StripTrailing(PATHSEPERATOR_CHAR);
                if (ListOSFiles(fpath.toString(), null, list) == -1) {
                    return null;
                }

                for (i = 0; i < list.Num(); i++) {
                    entry = new idStr(fpath.toString() + PATHSEPERATOR_CHAR + list.oGet(i).toString());
                    if (0 == entry.Icmp(fileName)) {
                        fp = Paths.get(entry.toString());//fp = fopen(entry, mode);
                        if (Files.exists(fp, NOFOLLOW_LINKS)) {
                            if (caseSensitiveName != null) {
                                caseSensitiveName.oSet(entry);
                                caseSensitiveName.StripPath();
                            }
                            if (fs_debug.GetInteger() != 0) {
                                common.Printf("idFileSystemLocal::OpenFileRead: changed %s to %s\n", fileName, entry);
                            }
                            break;
                        } else {
                            // not supposed to happen if ListOSFiles is doing it's job correctly
                            common.Warning("idFileSystemLocal::OpenFileRead: fs_caseSensitiveOS 1 could not open %s", entry);
                        }
                    }
                }
            } else if (caseSensitiveName != null) {
                caseSensitiveName.oSet(fileName);
                caseSensitiveName.StripPath();
            }
            try {
                //                return new FileInputStream(fp.toFile()).getChannel();
                return FileChannel.open(fp, fopenOptions(mode));
            } catch (final NoSuchFileException ex) {//TODO:turn exceptions back on.
//                Logger.getLogger(FileSystem_h.class.getName()).log(Level.WARNING, null, ex);
            } catch (final IOException ex) {
                Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }

        private FileChannel OpenOSFileCorrectName(idStr path, final String mode) {
            final idStr caseName = new idStr();
            final FileChannel f = OpenOSFile(path.toString(), mode, caseName);
            if (f != null) {
                path.StripFilename();
                path.Append(PATHSEPERATOR_STR);
                path.Append(caseName);
            }
            return f;
        }

        private long DirectFileLength(FileChannel o) {
            try {
                //            int pos;
//            int end;
//
//            pos = ftell(o);
//            fseek(o, 0, SEEK_END);
//            end = ftell(o);
//            fseek(o, pos, SEEK_SET);
                return o.size();
            } catch (final IOException ex) {
                Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
            }

            return -1;
        }

        public void CopyFile(idFile src, String toOSPath) {
            FileChannel f;
            int len;
            ByteBuffer buf;

            common.Printf("copy %s to %s\n", src.GetName(), toOSPath);
            src.Seek(0, FS_SEEK_END);
            len = src.Tell();
            src.Seek(0, FS_SEEK_SET);

            buf = ByteBuffer.allocate(len);//Mem_Alloc(len);
            if (src.Read(buf, len) != len) {
                common.FatalError("Short read in idFileSystemLocal::CopyFile()\n");
            }

            CreateOSPath(toOSPath);
            f = OpenOSFile(toOSPath, "wb");
            if (NOT(f)) {
                common.Printf("could not create destination file\n");
//                Heap.Mem_Free(buf);
                return;
            }

            try {
                if (f.write(buf) != len) {
//            if (fwrite(buf, 1, len, f) != len) {
                    common.FatalError("Short write in idFileSystemLocal::CopyFile()\n");
                }
                f.close();
//            Heap.Mem_Free(buf);
            } catch (final IOException ex) {
                Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private int AddUnique(final String name, idStrList list, idHashIndex hashIndex) {
            int i, hashKey;

            hashKey = hashIndex.GenerateKey(name.toCharArray());
            for (i = hashIndex.First(hashKey); i >= 0; i = hashIndex.Next(i)) {
                if (list.oGet(i).Icmp(name) == 0) {
                    return i;
                }
            }
            i = list.Append(new idStr(name));
            hashIndex.Add(hashKey, i);
            return i;
        }

        private void GetExtensionList(final String extension, idStrList extensionList) {
            int s, e, l;

            l = extension.length();
            s = 0;
            while (true) {
                e = idStr.FindChar(extension, '|', s, l);
                if (e != -1) {
                    extensionList.Append(new idStr(extension, s, e));
                    s = e + 1;
                } else {
                    extensionList.Append(new idStr(extension, s, l));
                    break;
                }
            }
        }

        /*
         ===============
         idFileSystemLocal::GetFileList

         Does not clear the list first so this can be used to progressively build a file list.
         When 'sort' is true only the new files added to the list are sorted.
         ===============
         */
        private int GetFileList(final String relativePath, final idStrList extensions, idStrList list, idHashIndex hashIndex, boolean fullRelativePath, final String gamedir /*= NULL*/) {
            searchpath_s search;
            fileInPack_s[] buildBuffer;
            int i, j;
            int pathLength;
            int length;
            String name;
            pack_t pak;
            idStr work;

            if (null == this.searchPaths) {
                common.FatalError("Filesystem call made without initialization\n");
            }

            if (0 == extensions.Num()) {
                return 0;
            }

            if (null == relativePath) {
                return 0;
            }
            pathLength = relativePath.length();
            if (pathLength != 0) {
                pathLength++;	// for the trailing '/'
            }

            // search through the path, one element at a time, adding to list
            for (search = this.searchPaths; search != null; search = search.next) {
                if (search.dir != null) {
                    if ((gamedir != null) && !gamedir.isEmpty()) {
                        if (!search.dir.gamedir.toString().equals(gamedir)) {
                            continue;
                        }
                    }

                    final idStrList sysFiles = new idStrList();
                    idStr netpath;

                    netpath = new idStr(BuildOSPath(search.dir.path.toString(), search.dir.gamedir.toString(), relativePath));

                    for (i = 0; i < extensions.Num(); i++) {

                        // scan for files in the filesystem
                        ListOSFiles(netpath.toString(), extensions.oGet(i).toString(), sysFiles);

                        // if we are searching for directories, remove . and ..
                        if (extensions.oGet(i).equals("/")) {// && extensions.oGet(i).oGet(1) == 0) {//TODO:==0?????
                            sysFiles.Remove(new idStr("."));
                            sysFiles.Remove(new idStr(".."));
                        }

                        for (j = 0; j < sysFiles.Num(); j++) {
                            // unique the match
                            if (fullRelativePath) {
                                work = new idStr(relativePath);
                                work.Append("/");
                                work.Append(sysFiles.oGet(j));
                                AddUnique(work.toString(), list, hashIndex);
                            } else {
                                AddUnique(sysFiles.oGet(j).toString(), list, hashIndex);
                            }
                        }
                    }
                } else if (search.pack != null) {
                    // look through all the pak file elements

                    // exclude any extra packs if we have server paks to search
                    if (this.serverPaks.Num() != 0) {
                        GetPackStatus(search.pack);
                        if ((search.pack.pureStatus != PURE_NEVER) && (0 == this.serverPaks.Find(search.pack))) {
                            continue; // not on the pure server pak list
                        }
                    }

                    pak = search.pack;
                    buildBuffer = pak.buildBuffer;
                    for (i = 0; i < pak.numfiles; i++) {

                        length = buildBuffer[i].name.Length();

                        // if the name is not long anough to at least contain the path
                        if (length <= pathLength) {
                            continue;
                        }

                        name = buildBuffer[i].name.toString();

                        // check for a path match without the trailing '/'
                        if ((pathLength > 0) && (idStr.Icmpn(name, relativePath, pathLength - 1) != 0)) {
                            continue;
                        }

                        // ensure we have a path, and not just a filename containing the path
                        if ((name.length() == pathLength) || (name.charAt(pathLength - 1) != '/')) {
                            continue;
                        }

                        // make sure the file is not in a subdirectory
                        for (j = pathLength; /*name.[j+1] != '\0'*/ j < name.length(); j++) {
                            if (name.charAt(j) == '/') {
                                break;
                            }
                        }
                        if ((j + 1) < name.length()) {
                            continue;
                        }

                        // check for extension match
                        for (j = 0; j < extensions.Num(); j++) {
                            if ((length >= extensions.oGet(j).Length()) && (extensions.oGet(j).Icmp(name.substring(length - extensions.oGet(j).Length())) == 0)) {
                                break;
                            }
                        }
                        if (j >= extensions.Num()) {
                            continue;
                        }

                        // unique the match
                        if (fullRelativePath) {
                            work = new idStr(relativePath);
                            work.Append("/");
                            work.Append(name.substring(pathLength));
                            work.StripTrailing('/');
                            AddUnique(work.toString(), list, hashIndex);
                        } else {
                            work = new idStr(name.substring(pathLength));
                            work.StripTrailing('/');
                            AddUnique(work.toString(), list, hashIndex);
                        }
                    }
                }
            }

            return list.Num();
        }

        private int GetFileListTree(final String relativePath, final idStrList extensions, idStrList list, idHashIndex hashIndex, final String gamedir /*= NULL*/) {
            int i;
            final idStrList slash = new idStrList(), folders = new idStrList(128);
            final idHashIndex folderHashIndex = new idHashIndex(1024, 128);

            // recurse through the subdirectories
            slash.Append(new idStr("/"));
            GetFileList(relativePath, slash, folders, folderHashIndex, true, gamedir);
            for (i = 0; i < folders.Num(); i++) {
                if (folders.oGet(i).oGet(0) == '.') {
                    continue;
                }
                if (folders.oGet(i).Icmp(relativePath) == 0) {
                    continue;
                }
                GetFileListTree(folders.oGet(i).toString(), extensions, list, hashIndex, gamedir);
            }

            // list files in the current directory
            GetFileList(relativePath, extensions, list, hashIndex, true, gamedir);

            return list.Num();
        }

        /*
         ================
         idFileSystemLocal::AddGameDirectory

         Sets gameFolder, adds the directory to the head of the search paths, then loads any pk4 files.
         ================
         */
        private void AddGameDirectory(final String path, final String dir) {
            int i;
            searchpath_s search;
            pack_t pak;
            idStr pakfile;
            final idStrList pakfiles = new idStrList();

            // check if the search path already exists
            for (search = this.searchPaths; search != null; search = search.next) {
                // if this element is a pak file
                if (null == search.dir) {
                    continue;
                }
                if ((search.dir.path.Cmp(path) == 0) && (search.dir.gamedir.Cmp(dir) == 0)) {
                    return;
                }
            }

            this.gameFolder = new idStr(dir);

            //
            // add the directory to the search path
            //
            search = new searchpath_s();
            search.dir = new directory_t();
            search.pack = null;

            search.dir.path = new idStr(path);
            search.dir.gamedir = new idStr(dir);
            search.next = this.searchPaths;
            this.searchPaths = search;

            // find all pak files in this directory
            pakfile = new idStr(BuildOSPath(path, dir, ""));
//            pakfile.oSet(pakfile.Length() - 1, (char) 0);	// strip the trailing slash

            ListOSFiles(pakfile.toString(), ".pk4", pakfiles);

            // sort them so that later alphabetic matches override
            // earlier ones. This makes pak1.pk4 override pak0.pk4
            pakfiles.Sort();

            for (i = 0; i < pakfiles.Num(); i++) {
                pakfile = new idStr(BuildOSPath(path, dir, pakfiles.oGet(i).toString()));
                pak = LoadZipFile(pakfile.toString());
                if (null == pak) {
                    continue;
                }
                // insert the pak after the directory it comes from
                search = new searchpath_s();
                search.dir = null;
                search.pack = pak;
                search.next = this.searchPaths.next;
                this.searchPaths.next = search;
                common.Printf("Loaded pk4 %s with checksum 0x%x\n", pakfile.toString(), pak.checksum);
            }
        }

        /*
         ================
         idFileSystemLocal::SetupGameDirectories

         Takes care of the correct search order.
         ================
         */
        private void SetupGameDirectories(final String gameName) {
            // setup cdpath
            if (!fs_cdpath.GetString().isEmpty()) {
                AddGameDirectory(fs_cdpath.GetString(), gameName);
            }

            // setup basepath
            if (!fs_basepath.GetString().isEmpty()) {
                AddGameDirectory(fs_basepath.GetString(), gameName);
            }

            // setup devpath
            if (!fs_devpath.GetString().isEmpty()) {
                AddGameDirectory(fs_devpath.GetString(), gameName);
            }

            // setup savepath
            if (!fs_savepath.GetString().isEmpty()) {
                AddGameDirectory(fs_savepath.GetString(), gameName);
            }
        }

        private void Startup() {
            searchpath_s search;
            int i;
            pack_t pak;
            int addon_index;

            common.Printf("------ Initializing File System ------\n");

            if (this.restartChecksums.Num() != 0) {
                common.Printf("restarting in pure mode with %d pak files\n", this.restartChecksums.Num());
            }
            if (this.addonChecksums.Num() != 0) {
                common.Printf("restarting filesystem with %d addon pak file(s) to include\n", this.addonChecksums.Num());
            }

            SetupGameDirectories(BASE_GAMEDIR);

            // fs_game_base override
            if (!fs_game_base.GetString().isEmpty() && (idStr.Icmp(fs_game_base.GetString(), BASE_GAMEDIR) != 0)) {
                SetupGameDirectories(fs_game_base.GetString());
            }

            // fs_game override
            if (!fs_game.GetString().isEmpty() && (idStr.Icmp(fs_game.GetString(), BASE_GAMEDIR) != 0) && (idStr.Icmp(fs_game.GetString(), fs_game_base.GetString()) != 0)) {
                SetupGameDirectories(fs_game.GetString());
            }

            // currently all addons are in the search list - deal with filtering out and dependencies now
            // scan through and deal with dependencies
            search = this.searchPaths;
            while (search != null) {
                if ((null == search.pack) || !search.pack.addon) {
                    search = search.next;
                    continue;
                }
                pak = search.pack;
                if (fs_searchAddons.GetBool()) {
                    // when we have fs_searchAddons on we should never have addonChecksums
                    assert (0 == this.addonChecksums.Num());
                    pak.addon_search = true;
                    search = search.next;
                    continue;
                }
                addon_index = this.addonChecksums.FindIndex(pak.checksum);
                if (addon_index >= 0) {
                    assert (!pak.addon_search);	// any pak getting flagged as addon_search should also have been removed from addonChecksums already
                    pak.addon_search = true;
                    this.addonChecksums.RemoveIndex(addon_index);
                    FollowAddonDependencies(pak);
                }
                search = search.next;
            }

            // now scan to filter out addons not marked addon_search
            search = this.searchPaths;
            while (search != null) {
                if ((null == search.pack) || !search.pack.addon) {
                    search = search.next;
                    continue;
                }
                assert (null == search.dir);
                pak = search.pack;
                if (pak.addon_search) {
                    common.Printf("Addon pk4 %s with checksum 0x%x is on the search list\n", pak.pakFilename.toString(), pak.checksum);
                    search = search.next;
                } else {
                    // remove from search list, put in addons list
                    final searchpath_s paksearch = search;
                    search = search.next;
                    paksearch.next = this.addonPaks;
                    this.addonPaks = paksearch;
                    common.Printf("Addon pk4 %s with checksum 0x%x is on addon list\n", pak.pakFilename.toString(), pak.checksum);
                }
            }

            // all addon paks found and accounted for
            assert (0 == this.addonChecksums.Num());
            this.addonChecksums.Clear();	// just in case

            if (this.restartChecksums.Num() != 0) {
                search = this.searchPaths;
                while (search != null) {
                    if (null == search.pack) {
                        search = search.next;
                        continue;
                    }
                    if ((i = this.restartChecksums.FindIndex(search.pack.checksum)) != -1) {
                        if (i == 0) {
                            // this pak is the next one in the pure search order
                            this.serverPaks.Append(search.pack);
                            this.restartChecksums.RemoveIndex(0);
                            if (0 == this.restartChecksums.Num()) {
                                break; // early out, we're done
                            }
                            search = search.next;
                            continue;
                        } else {
                            // this pak will be on the pure list, but order is not right yet
                            searchpath_s aux;
                            aux = search.next;
                            if (null == aux) {
                                // last of the list can't be swapped back
                                if (fs_debug.GetBool()) {
                                    common.Printf("found pure checksum %x at index %d, but the end of search path is reached\n", search.pack.checksum, i);
                                    final idStr checks = new idStr();
                                    checks.Clear();
                                    for (i = 0; i < this.serverPaks.Num(); i++) {
                                        checks.Append(va("%p ", this.serverPaks.oGet(i)));
                                    }
                                    common.Printf("%d pure paks - %s \n", this.serverPaks.Num(), checks.toString());
                                    checks.Clear();
                                    for (i = 0; i < this.restartChecksums.Num(); i++) {
                                        checks.Append(va("%x ", this.restartChecksums.oGet(i)));
                                    }
                                    common.Printf("%d paks left - %s\n", this.restartChecksums.Num(), checks.toString());
                                }
                                common.FatalError("Failed to restart with pure mode restrictions for server connect");
                            }
                            // put this search path at the end of the list
                            searchpath_s search_end;
                            search_end = search.next;
                            while (search_end.next != null) {
                                search_end = search_end.next;
                            }
                            search_end.next = search;
                            search = search.next;
                            search_end.next.next = null;
                            continue;
                        }
                    }
                    // this pak is not on the pure list
                    search = search.next;
                }
                // the list must be empty
                if (this.restartChecksums.Num() != 0) {
                    if (fs_debug.GetBool()) {
                        final idStr checks = new idStr();
                        checks.Clear();
                        for (i = 0; i < this.serverPaks.Num(); i++) {
                            checks.Append(va("%p ", this.serverPaks.oGet(i)));
                        }
                        common.Printf("%d pure paks - %s \n", this.serverPaks.Num(), checks);
                        checks.Clear();
                        for (i = 0; i < this.restartChecksums.Num(); i++) {
                            checks.Append(va("%x ", this.restartChecksums.oGet(i)));
                        }
                        common.Printf("%d paks left - %s\n", this.restartChecksums.Num(), checks);
                    }
                    common.FatalError("Failed to restart with pure mode restrictions for server connect");
                }
                // also the game pak checksum
                // we could check if the game pak is actually present, but we would not be restarting if there wasn't one @ first pure check
                this.gamePakChecksum = this.restartGamePakChecksum;
            }

            // add our commands
            cmdSystem.AddCommand("dir", Dir_f.getInstance(), CMD_FL_SYSTEM, "lists a folder", idCmdSystem.ArgCompletion_FileName.getInstance());
            cmdSystem.AddCommand("dirtree", DirTree_f.getInstance(), CMD_FL_SYSTEM, "lists a folder with subfolders");
            cmdSystem.AddCommand("path", Path_f.getInstance(), CMD_FL_SYSTEM, "lists search paths");
            cmdSystem.AddCommand("touchFile", TouchFile_f.getInstance(), CMD_FL_SYSTEM, "touches a file");
            cmdSystem.AddCommand("touchFileList", TouchFileList_f.getInstance(), CMD_FL_SYSTEM, "touches a list of files");

            // print the current search paths
            Path_f.getInstance().run(new idCmdArgs());

            common.Printf("file syastem initialized.\n");
            common.Printf("--------------------------------------\n");
        }

        /*
         ===================
         idFileSystemLocal::SetRestrictions

         Looks for product keys and restricts media add on ability
         if the full version is not found
         ===================
         */
        private void SetRestrictions() {
            if (ID_DEMO_BUILD) {
                common.Printf("\nRunning in restricted demo mode.\n\n");
                // make sure that the pak file has the header checksum we expect
                searchpath_s search;
                for (search = this.searchPaths; search != null; search = search.next) {
                    if (search.pack != null) {
                        // a tiny attempt to keep the checksum from being scannable from the exe
                        if ((search.pack.checksum ^ 0x84268436) != (DEMO_PAK_CHECKSUM ^ 0x84268436)) {
                            common.FatalError("Corrupted %s: 0x%x", search.pack.pakFilename.toString(), search.pack.checksum);
                        }
                    }
                }
                cvarSystem.SetCVarBool("fs_restrict", true);
            }
        }
//							// some files can be obtained from directories without compromising si_pure

        private boolean FileAllowedFromDir(final String path) {
            int l;

            l = path.length();

            if ((".cfg".equals(path.substring(l - 4)) // for config files
                    || ".dat".equals(path.substring(l - 4)) // for journal files
                    || "dll".equals(path.substring(l - 4)) // dynamic modules are handled a different way for pure
                    || ".so".equals(path.substring(l - 3))
                    || ((l > 6) && ".dylib".equals(path.substring(l - 6)))
                    || ((l > 10) && ".scriptcfg".equals(path.substring(l - 10))))// configuration script, such as map cycle
                    || (ID_PURE_ALLOWDDS && "dds".equals(path.substring(l - 4)))) {
                // note: cd and xp keys, as well as config.spec are opened through an explicit OS path and don't hit this
                return true;
            }
            // savegames
            if (path.startsWith("savegames")
                    && (".tga".equals(path.substring(l - 4)) || ".txt".equals(path.substring(l - 4)) || ".save".equals(path.substring(l - 5)))) {
                return true;
            }
            // screen shots
            if (path.startsWith("screenshots") && ".tga".equals(path.substring(l - 4))) {
                return true;
            }
            // objective tgas
            if (path.startsWith("maps/game")
                    && ".tga".equals(path.substring(l - 4))) {
                return true;
            }
            // splash screens extracted from addons
            if (path.startsWith("guis/assets/splash/addon")
                    && ".tga".equals(path.substring(l - 4))) {
                return true;
            }

            return false;
        }
        // searches all the paks, no pure check

        private pack_t GetPackForChecksum(int checksum) {
            return GetPackForChecksum(checksum, false);
        }

        private pack_t GetPackForChecksum(int checksum, boolean searchAddons /*= false*/) {
            searchpath_s search;
            for (search = this.searchPaths; search != null; search = search.next) {
                if (null == search.pack) {
                    continue;
                }
                if (search.pack.checksum == checksum) {
                    return search.pack;
                }
            }
            if (searchAddons) {
                for (search = this.addonPaks; search != null; search = search.next) {
                    assert ((search.pack != null) && search.pack.addon);
                    if (search.pack.checksum == checksum) {
                        return search.pack;
                    }
                }
            }
            return null;
        }
        // searches all the paks, no pure check

        private pack_t FindPakForFileChecksum(final String relativePath, int[] findChecksum, boolean bReference) {
            searchpath_s search;
            pack_t pak;
            fileInPack_s pakFile;
            int hash;
            assert (0 == this.serverPaks.Num());
            hash = (int) HashFileName(relativePath);
            for (search = this.searchPaths; search != null; search = search.next) {
                if ((search.pack != null) && (search.pack.hashTable[hash] != null)) {
                    pak = search.pack;
                    for (pakFile = pak.hashTable[hash]; pakFile != null; pakFile = pakFile.next) {
                        if (FilenameCompare(pakFile.name.toString(), relativePath)) {
                            final idFile_InZip file = ReadFileFromZip(pak, pakFile, relativePath);
                            if (findChecksum[0] == GetFileChecksum(file)) {
                                if (fs_debug.GetBool()) {
                                    common.Printf("found '%s' with checksum 0x%x in pak '%s'\n", relativePath, findChecksum[0], pak.pakFilename.toString());
                                }
                                if (bReference) {
                                    pak.referenced = true;
                                    // FIXME: use dependencies for pak references
                                }
                                CloseFile(file);
                                return pak;
                            } else if (fs_debug.GetBool()) {
                                common.Printf("'%s' in pak '%s' has != checksum %x\n", relativePath, pak.pakFilename, GetFileChecksum(file));
                            }
                            CloseFile(file);
                        }
                    }
                }
            }
            if (fs_debug.GetBool()) {
                common.Printf("no pak file found for '%s' checksumed %x\n", relativePath, findChecksum[0]);
            }
            return null;
        }

        private pack_t LoadZipFile(final String zipfile) {
            fileInPack_s[] buildBuffer;
            pack_t pack;
            ZipFile uf;
//            int err;
//            unz_global_info gi;
            String filename_inzip;//= new char[MAX_ZIPPED_FILE_NAME];
//            unz_file_info file_info;
            int i;
            long hash;
            int fs_numHeaderLongs;
            int[] fs_headerLongs;
            FileChannel f;
            int len;
            int confHash;
            fileInPack_s pakFile;

            f = OpenOSFile(zipfile, "rb");
            if (NOT(f)) {
                return null;
            }

            try {
                //            fseek(f, 0, SEEK_END);
                len = (int) f.size();
                f.close();

                fs_numHeaderLongs = 0;

                uf = new ZipFile(zipfile);

//            err = unzGetGlobalInfo(uf, gi);
//
//            if (err != UNZ_OK) {
//                return null;
//            }
//
                buildBuffer = new fileInPack_s[uf.size()];//int) gi.number_entry];
                pack = new pack_t();
                for (i = 0; i < FILE_HASH_SIZE; i++) {
                    pack.hashTable[i] = null;
                }

                pack.pakFilename = new idStr(zipfile);
                pack.handle = uf;
                pack.numfiles = uf.size();//gi.number_entry;
                pack.buildBuffer = buildBuffer;
                pack.referenced = false;
                pack.binary = BINARY_UNKNOWN;
                pack.addon = false;
                pack.addon_search = false;
                pack.addon_info = null;
                pack.pureStatus = PURE_UNKNOWN;
                pack.isNew = false;

                pack.length = len;

//            unzGoToFirstFile(uf);
                fs_headerLongs = new int[uf.size()];// gi.number_entry];//Mem_ClearedAlloc(gi.number_entry sizeof(int));
                final Enumeration<? extends ZipEntry> entries = uf.entries();
                for (i = 0; i < uf.size() /*gi.number_entry*/; i++) {
                    // go to the next file in the zip
                    final ZipEntry entry = entries.nextElement();
//                err = unzGetCurrentFileInfo(uf, file_info, filename_inzip, sizeof(filename_inzip), null, 0, null, 0);
//                if (err != UNZ_OK) {
//                    break;
//                }
//                if (file_info.uncompressed_size > 0) {
//                    fs_headerLongs[fs_numHeaderLongs++] = LittleLong(file_info.crc);
//                }
                    filename_inzip = entry.getName();
                    if (entry.getSize() > 0) {
                        fs_headerLongs[fs_numHeaderLongs++] = LittleLong(entry.getCrc());
                    }
                    hash = HashFileName(filename_inzip);
                    buildBuffer[i] = new fileInPack_s();
                    buildBuffer[i].name = new idStr(filename_inzip);
                    buildBuffer[i].name.ToLower();
                    buildBuffer[i].name.BackSlashesToSlashes();
                    // store the file position in the zip
//                unzGetCurrentFileInfoPosition(uf, buildBuffer[i].pos);
                    buildBuffer[i].pos = i;
                    // add the file to the hash
                    buildBuffer[i].next = pack.hashTable[(int) hash];
                    pack.hashTable[(int) hash] = buildBuffer[i];

                    buildBuffer[i].entry = entry;//TODO:remove all the other shit
//                // go to the next file in the zip
//                unzGoToNextFile(uf);
                }

                // check if this is an addon pak
                pack.addon = false;
                confHash = (int) HashFileName(ADDON_CONFIG);
                for (pakFile = pack.hashTable[confHash]; pakFile != null; pakFile = pakFile.next) {
                    if (FilenameCompare(pakFile.name.toString(), ADDON_CONFIG)) {
                        pack.addon = true;
                        final idFile_InZip file = ReadFileFromZip(pack, pakFile, ADDON_CONFIG);
                        // may be just an empty file if you don't bother about the mapDef
                        if ((file != null) && (file.Length() != 0)) {
                            ByteBuffer buf;
                            buf = ByteBuffer.allocate(file.Length() + 1);
                            file.Read( /*(void *)*/buf, file.Length());
                            buf.put(file.Length(), (byte) '\0');
                            pack.addon_info = ParseAddonDef(new String(buf.array()), file.Length());
//				delete[] buf;
                        }
                        if (file != null) {
                            CloseFile(file);
                        }
                        break;
                    }
                }

                pack.checksum = new BigInteger(MD4_BlockChecksum(fs_headerLongs, fs_numHeaderLongs)).intValue();
                pack.checksum = LittleLong(pack.checksum);

//            Mem_Free(fs_headerLongs);
                return pack;

            } catch (final IOException ex) {
                Logger.getLogger(FileSystem_h.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }

        private idFile_InZip ReadFileFromZip(pack_t pak, fileInPack_s pakFile, final String relativePath) {
            File fp;
            final idFile_InZip file = new idFile_InZip();

            // open a new file on the pakfile
            fp = new File(pak.pakFilename.toString());//TODO: check this shit
            if (!fp.exists()) {
                common.FatalError("Couldn't reopen %s", pak.pakFilename.toString());
            }
            file.z = pakFile.entry;
            file.name.oSet(relativePath);
            file.fullPath.oSet(pak.pakFilename);
            file.zipFilePos = pakFile.pos;
            file.fileSize = (int) pakFile.entry.getSize();
            return file;
        }

        private int GetFileChecksum(idFile file) {
            int len, ret;
            ByteBuffer buf;

            file.Seek(0, FS_SEEK_END);
            len = file.Tell();
            file.Seek(0, FS_SEEK_SET);
            buf = ByteBuffer.allocate(len);
            if (file.Read(buf, len) != len) {
                common.FatalError("Short read in idFileSystemLocal::GetFileChecksum()\n");
            }
            ret = new BigInteger(MD4_BlockChecksum(buf, len)).intValue();
//            Mem_Free(buf);
            return ret;
        }

        private pureStatus_t GetPackStatus(pack_t pak) {
            int i, l, hashindex;
            fileInPack_s file;
            boolean abrt;
            final idStr name = new idStr();

            if (pak.pureStatus != PURE_UNKNOWN) {
                return pak.pureStatus;
            }

            // check content for PURE_NEVER
            i = 0;
            for (hashindex = 0; hashindex < FILE_HASH_SIZE; hashindex++) {
                abrt = false;
                file = pak.buildBuffer[hashindex] = pak.hashTable[hashindex];
                while (file != null) {
                    abrt = true;
                    l = file.name.Length();
                    for (int j = 0; pureExclusions[j].func != null; j++) {
                        if (pureExclusions[j].func.run(pureExclusions[j], l, file.name)) {
                            abrt = false;
                            break;
                        }
                    }
                    if (abrt) {
                        common.DPrintf("pak '%s' candidate for pure: '%s'\n", pak.pakFilename.toString(), file.name.toString());
                        break;
                    }
                    file = file.next;//TODO:check this assignment.
                    i++;
                }
                if (abrt) {
                    break;
                }
            }
            if (i == pak.numfiles) {
                pak.pureStatus = PURE_NEVER;
                return PURE_NEVER;
            }

            // check pak name for PURE_ALWAYS
            pak.pakFilename.ExtractFileName(name);
            if (0 == name.IcmpPrefixPath("pak")) {
                pak.pureStatus = PURE_ALWAYS;
                return PURE_ALWAYS;
            }

            pak.pureStatus = PURE_NEUTRAL;
            return PURE_NEUTRAL;
        }

        private addonInfo_t ParseAddonDef(final String buf, final int len) {
            final idLexer src = new idLexer();
            final idToken token = new idToken(), token2 = new idToken();
            addonInfo_t info;

            src.LoadMemory(buf, len, "<addon.conf>");
            src.SetFlags(DECL_LEXER_FLAGS);
            if (!src.SkipUntilString("addonDef")) {
                src.Warning("ParseAddonDef: no addonDef");
                return null;
            }
            if (!src.ReadToken(token)) {
                src.Warning("Expected {");
                return null;
            }
            info = new addonInfo_t();
            // read addonDef
            while (true) {
                if (!src.ReadToken(token)) {
//			delete info;
                    return null;
                }
                if (!token.equals("}")) {
                    break;
                }
                if (token.type != TT_STRING) {
                    src.Warning("Expected quoted string, but found '%s'", token.toString());
//			delete info;
                    return null;
                }
                int checksum;

//		if ( sscanf( token.c_str(), "0x%x", checksum ) != 1 && sscanf( token.c_str(), "%x", checksum ) != 1 ) {
                if ((checksum = Integer.parseInt(String.format("%x", token.toString()))) != 0) {
                    src.Warning("Could not parse checksum '%s'", token.toString());
//			delete info;
                    return null;
                }
                info.depends.Append(checksum);
            }
            // read any number of mapDef entries
            while (true) {
                if (!src.SkipUntilString("mapDef")) {
                    return info;
                }
                if (!src.ReadToken(token)) {
                    src.Warning("Expected map path");
                    info.mapDecls.DeleteContents(true);
//			delete info;
                    return null;
                }
                final idDict dict = new idDict();
                dict.Set("path", token.toString());
                if (!src.ReadToken(token)) {
                    src.Warning("Expected {");
                    info.mapDecls.DeleteContents(true);
//			delete dict;
//			delete info;
                    return null;
                }
                while (true) {
                    if (!src.ReadToken(token)) {
                        break;
                    }
                    if (!token.equals("}")) {
                        break;
                    }
                    if (token.type != TT_STRING) {
                        src.Warning("Expected quoted string, but found '%s'", token.toString());
                        info.mapDecls.DeleteContents(true);
//				delete dict;
//				delete info;
                        return null;
                    }

                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        info.mapDecls.DeleteContents(true);
//				delete dict;
//				delete info;
                        return null;
                    }

                    if (dict.FindKey(token.toString()) != null) {
                        src.Warning("'%s' already defined", token.toString());
                    }
                    dict.Set(token, token2);
                }
                info.mapDecls.Append(dict);
            }
//            assert (false);
//            return null;
        }

        private void FollowAddonDependencies(pack_t pak) {
            assert (pak != null);
            if ((null == pak.addon_info) || (0 == pak.addon_info.depends.Num())) {
                return;
            }
            int i;
			final int num = pak.addon_info.depends.Num();
            for (i = 0; i < num; i++) {
                final pack_t deppak = GetPackForChecksum(pak.addon_info.depends.oGet(i), true);
                if (deppak != null) {
                    // make sure it hasn't been marked for search already
                    if (!deppak.addon_search) {
                        // must clean addonChecksums as we go
                        final int addon_index = this.addonChecksums.FindIndex(deppak.checksum);
                        if (addon_index >= 0) {
                            this.addonChecksums.RemoveIndex(addon_index);
                        }
                        deppak.addon_search = true;
                        common.Printf("Addon pk4 %s 0x%x depends on pak %s 0x%x, will be searched\n", pak.pakFilename.toString(), pak.checksum, deppak.pakFilename.toString(), deppak.checksum);
                        FollowAddonDependencies(deppak);
                    }
                } else {
                    common.Printf("Addon pk4 %s 0x%x depends on unknown pak 0x%x\n", pak.pakFilename.toString(), pak.checksum, pak.addon_info.depends.oGet(i));
                }
            }
        }
//

        private static /*size_t*/ int CurlWriteFunction(ByteBuffer ptr, int /*size_t*/ size, /*size_t*/ int nmemb, Object[] stream) {
            throw new TODO_Exception();
//            backgroundDownload_t bgl = (backgroundDownload_t) stream[0];
//            if (null == bgl.f) {
//                return size * nmemb;
//            }
////            if (_WIN32) {
////                return _write(((idFile_Permanent) bgl.f).GetFilePtr()._file, ptr, size * nmemb);
////            } else {
//            return ((idFile_Permanent) bgl.f).GetFilePtr().write(ptr);
////                return fwrite(ptr, size, nmemb, ((idFile_Permanent) bgl.f).GetFilePtr());
////            }
        }
//							// curl_progress_callback in curl.h

        private static int CurlProgressFunction(Object[] clientp, double dltotal, double dlnow, double ultotal, double ulnow) {
            final backgroundDownload_s bgl = (backgroundDownload_s) clientp[0];
            if (bgl.url.status == DL_ABORTING) {
                return 1;
            }
            bgl.url.dltotal = (int) dltotal;
            bgl.url.dlnow = (int) dlnow;
            return 0;
        }
    }

    public static void setFileSystem(idFileSystem fileSystem) {
        FileSystem_h.fileSystem = FileSystem_h.fileSystemLocal = (idFileSystemLocal) fileSystem;
    }
}
