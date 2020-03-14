package neo.Game.Animation;

import static neo.Game.GameSys.SysCvar.g_exportMask;
import static neo.Game.Game_local.gameLocal;
import static neo.Renderer.Model.MD5_ANIM_EXT;
import static neo.Renderer.Model.MD5_CAMERA_EXT;
import static neo.Renderer.Model.MD5_MESH_EXT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Licensee.BASE_GAMEDIR;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Str.va;

import neo.TempDump.TODO_Exception;
import neo.framework.FileSystem_h.idFileList;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Parser;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token;
import neo.idlib.Text.Token.idToken;

/**
 *
 */
public class Anim_Import {

    /**
     * *********************************************************************
     *
     * Maya conversion functions
     *
     **********************************************************************
     */
    public static idStr Maya_Error;
//
//    public static exporterInterface_t Maya_ConvertModel = null;
//    public static exporterShutdown_t Maya_Shutdown = null;
    public static int importDLL = 0;
//
//bool idModelExport::initialized = false;

    /*
     ==============================================================================================

     idModelExport

     ==============================================================================================
     */
    public static class idModelExport {

        //private static boolean initialized;
        //
        public         idStr   commandLine;
        public         idStr   src;
        public         idStr   dest;
        public         boolean force;
        //
        //

        private void Reset() {
            force = false;
            commandLine.oSet("");
            src.oSet("");
            dest.oSet("");
        }

        private boolean ParseOptions(Lexer.idLexer lex) throws idException {
            Token.idToken token = new Token.idToken();
            idStr destdir = new idStr();
            idStr sourcedir = new idStr();

            if (!lex.ReadToken(token)) {
                lex.Error("Expected filename");
                return false;
            }

            src = token;
            dest = token;

            while (lex.ReadToken(token)) {
                if (token.equals("-")) {
                    if (!lex.ReadToken(token)) {
                        lex.Error("Expecting option");
                        return false;
                    }
                    if (token.equals("sourcedir")) {
                        if (!lex.ReadToken(token)) {
                            lex.Error("Missing pathname after -sourcedir");
                            return false;
                        }
                        sourcedir = token;
                    } else if (token.equals("destdir")) {
                        if (!lex.ReadToken(token)) {
                            lex.Error("Missing pathname after -destdir");
                            return false;
                        }
                        destdir = token;
                    } else if (token.equals("dest")) {
                        if (!lex.ReadToken(token)) {
                            lex.Error("Missing filename after -dest");
                            return false;
                        }
                        dest = token;
                    } else {
                        commandLine.oPluSet(va(" -%s", token.toString()));
                    }
                } else {
                    commandLine.oPluSet(va(" %s", token.toString()));
                }
            }

            if (sourcedir.Length() != 0) {
                src.StripPath();
                sourcedir.BackSlashesToSlashes();
                src.oSet(String.format("%s/%s", sourcedir.toString(), src.toString()));
            }

            if (destdir.Length() != 0) {
                dest.StripPath();
                destdir.BackSlashesToSlashes();
                dest.oSet(String.format("%s/%s", destdir.toString(), dest.toString()));
            }

            return true;
        }

        private int ParseExportSection(Parser.idParser parser) {
            Token.idToken command = new idToken();
            Token.idToken token = new idToken();
            idStr defaultCommands = new idStr();
            Lexer.idLexer lex = new idLexer();
            idStr temp = new idStr();
            idStr parms = new idStr();
            int count;

            // only export sections that match our export mask
            if (isNotNullOrEmpty(g_exportMask.GetString())) {
                if (parser.CheckTokenString("{")) {
                    parser.SkipBracedSection(false);
                    return 0;
                }

                parser.ReadToken(token);
                if (token.Icmp(g_exportMask.GetString()) != 0) {
                    parser.SkipBracedSection();
                    return 0;
                }
                parser.ExpectTokenString("{");
            } else if (!parser.CheckTokenString("{")) {
                // skip the export mask
                parser.ReadToken(token);
                parser.ExpectTokenString("{");
            }

            count = 0;

            lex.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);

            while (true) {

                if (!parser.ReadToken(command)) {
                    parser.Error("Unexpoected end-of-file");
                    break;
                }

                if (command.equals("}")) {
                    break;
                }

                if (command.equals("options")) {
                    parser.ParseRestOfLine(defaultCommands);
                } else if (command.equals("addoptions")) {
                    parser.ParseRestOfLine(temp);
                    defaultCommands.oPluSet(" ");
                    defaultCommands.oPluSet(temp);
                } else if ((command.equals("mesh")) || (command.equals("anim")) || (command.equals("camera"))) {
                    if (!parser.ReadToken(token)) {
                        parser.Error("Expected filename");
                    }

                    temp = token;
                    parser.ParseRestOfLine(parms);

                    if (defaultCommands.Length() != 0) {
                        temp.oSet(String.format("%s %s", temp.toString(), defaultCommands));
                    }

                    if (parms.Length() != 0) {
                        temp.oSet(String.format("%s %s", temp, parms));
                    }

                    lex.LoadMemory(temp, temp.Length(), parser.GetFileName());

                    Reset();
                    if (ParseOptions(lex)) {
                        String game = cvarSystem.GetCVarString("fs_game");
                        if (game.length() == 0) {
                            game = BASE_GAMEDIR;
                        }

                        if (command.equals("mesh")) {
                            dest.SetFileExtension(MD5_MESH_EXT);
                        } else if (command.equals("anim")) {
                            dest.SetFileExtension(MD5_ANIM_EXT);
                        } else if (command.equals("camera")) {
                            dest.SetFileExtension(MD5_CAMERA_EXT);
                        } else {
                            dest.SetFileExtension(command.toString());
                        }
//				idStr back = commandLine;
                        commandLine.oSet(String.format("%s %s -dest %s -game %s%s", command.toString(), src.toString(), dest.toString(), game, commandLine.toString()));
                        if (ConvertMayaToMD5()) {
                            count++;
                        } else {
                            parser.Warning("Failed to export '%s' : %s", src, Maya_Error);
                        }
                    }
                    lex.FreeSource();
                } else {
                    parser.Error("Unknown token: %s", command);
                    parser.SkipBracedSection(false);
                    break;
                }
            }

            return count;
        }

        /*
         =====================
         idModelExport::CheckMayaInstall

         Determines if Maya is installed on the user's machine
         =====================
         */
        private static boolean CheckMayaInstall() {//TODO:is this necessary?
////if( _WIN32){
////	return false;
////}else if(false){
////	HKEY	hKey;
////	long	lres, lType;
////
////	lres = RegOpenKey( HKEY_LOCAL_MACHINE, "SOFTWARE\\Alias|Wavefront\\Maya\\4.5\\Setup\\InstallPath", &hKey );
////
////	if ( lres != ERROR_SUCCESS ) {
////		return false;
////	}
////
////	lres = RegQueryValueEx( hKey, "MAYA_INSTALL_LOCATION", NULL, (unsigned long*)&lType, (unsigned char*)NULL, (unsigned long*)NULL );
////
////	RegCloseKey( hKey );
////
////	if ( lres != ERROR_SUCCESS ) {
////		return false;
////	}
////	return true;
////}else{
//            HKEY hKey;
//            long lres;
//
//            // only check the non-version specific key so that we only have to update the maya dll when new versions are released
//            lres = RegOpenKey(HKEY_LOCAL_MACHINE, "SOFTWARE\\Alias|Wavefront\\Maya", hKey);
//            RegCloseKey(hKey);
//
//            return lres == ERROR_SUCCESS;
            throw new TODO_Exception();
        }

        /*
         =====================
         idModelExport::LoadMayaDll

         Checks to see if we can load the Maya export dll
         =====================
         */
        private static void LoadMayaDll() {
//            exporterDLLEntry_t dllEntry;
//            char[] dllPath = new char[MAX_OSPATH];
//
//            fileSystem.FindDLL("MayaImport", dllPath, false);
//            if (0 == dllPath[ 0]) {
//                return;
//            }
//            importDLL = sys.DLL_Load(dllPath);
//            if (0 == importDLL) {
//                return;
//            }
//
//            // look up the dll interface functions
//            dllEntry = (exporterDLLEntry_t) sys.DLL_GetProcAddress(importDLL, "dllEntry");
//            Maya_ConvertModel = (exporterInterface_t) sys.DLL_GetProcAddress(importDLL, "Maya_ConvertModel");
//            Maya_Shutdown = (exporterShutdown_t) sys.DLL_GetProcAddress(importDLL, "Maya_Shutdown");
//            if (!Maya_ConvertModel || !dllEntry || !Maya_Shutdown) {
//                Maya_ConvertModel = null;
//                Maya_Shutdown = null;
//                sys.DLL_Unload(importDLL);
//                importDLL = 0;
//                gameLocal.Error("Invalid interface on export DLL.");
//                return;
//            }
//
//            // initialize the DLL
//            if (!dllEntry(MD5_VERSION, common, sys)) {
//                // init failed
//                Maya_ConvertModel = null;
//                Maya_Shutdown = null;
//                sys.DLL_Unload(importDLL);
//                importDLL = 0;
//                gameLocal.Error("Export DLL init failed.");
//                return;
//            }
            throw new TODO_Exception();
        }

        /*
         =====================
         idModelExport::ConvertMayaToMD5

         Checks if a Maya model should be converted to an MD5, and converts if if the time/date or 
         version number has changed.
         =====================
         */
        private boolean ConvertMayaToMD5() {
//            long[] sourceTime = {0};
//            long[] destTime = {0};
//            int version;
//            idToken cmdLine = new idToken();
//            idStr path = new StrPool.idPoolStr();
//
//            // check if our DLL got loaded
//            if (initialized && !Maya_ConvertModel) {
//                Maya_Error.oSet("MayaImport dll not loaded.");
//                return false;
//            }
//
//            // if idAnimManager::forceExport is set then we always reexport Maya models
//            if (idAnimManager.forceExport) {
//                force = true;
//            }
//
//            // get the source file's time
//            if (fileSystem.ReadFile(src, null, sourceTime) < 0) {
//                // source file doesn't exist
//                return true;
//            }
//
//            // get the destination file's time
//            if (!force && (fileSystem.ReadFile(dest, null, destTime) >= 0)) {
//                idParser parser = new idParser(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS);
//
//                parser.LoadFile(dest);
//
//                // read the file version
//                if (parser.CheckTokenString(MD5_VERSION_STRING)) {
//                    version = parser.ParseInt();
//
//                    // check the command line
//                    if (parser.CheckTokenString("commandline")) {
//                        parser.ReadToken(cmdLine);
//
//                        // check the file time, scale, and version
//                        if ((destTime[0] >= sourceTime[0]) && (version == MD5_VERSION) && (cmdLine == commandLine)) {
//                            // don't convert it
//                            return true;
//                        }
//                    }
//                }
//            }
//
//            // if this is the first time we've been run, check if Maya is installed and load our DLL
//            if (!initialized) {
//                initialized = true;
//
//                if (!CheckMayaInstall()) {
//                    Maya_Error.oSet("Maya not installed in registry.");
//                    return false;
//                }
//
//                LoadMayaDll();
//
//                // check if our DLL got loaded
//                if (!Maya_ConvertModel) {
//                    Maya_Error.oSet("Could not load MayaImport dll.");
//                    return false;
//                }
//            }
//
//            // we need to make sure we have a full path, so convert the filename to an OS path
//            src.oSet(fileSystem.RelativePathToOSPath(src));
//            dest.oSet(fileSystem.RelativePathToOSPath(dest));
//
//            dest.ExtractFilePath(path);
//            if (path.Length() != 0) {
//                fileSystem.CreateOSPath(path);
//            }
//
//            // get the os path in case it needs to create one
//            path.oSet(fileSystem.RelativePathToOSPath(""));
//
//            common.SetRefreshOnPrint(true);
//            Maya_Error = Maya_ConvertModel(path, commandLine);
//            common.SetRefreshOnPrint(false);
//            if (!Maya_Error.equals("Ok")) {
//                return false;
//            }
//
//            // conversion succeded
//            return true;
            throw new TODO_Exception();
        }

        public idModelExport() {
            Reset();
        }

        public static void Shutdown() {
//            if (Maya_Shutdown) {
//                Maya_Shutdown.run();
//            }
//
//            if (importDLL != 0) {
//                sys.DLL_Unload(importDLL);
//            }
//
//            importDLL = 0;
//            Maya_Shutdown = null;
//            Maya_ConvertModel = null;
//            Maya_Error.Clear();
//            initialized = false;
        }

        public int ExportDefFile(final String filename) {
            idParser parser = new idParser(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
            idToken token = new idToken();
            int count;

            count = 0;

            if (!parser.LoadFile(filename)) {
                gameLocal.Printf("Could not load '%s'\n", filename);
                return 0;
            }

            while (parser.ReadToken(token)) {
                if (token.equals("export")) {
                    count += ParseExportSection(parser);
                } else {
                    parser.ReadToken(token);
                    parser.SkipBracedSection();
                }
            }

            return count;
        }

        public boolean ExportModel(final String model) {
            String game = cvarSystem.GetCVarString("fs_game");
            if (isNotNullOrEmpty(game)) {
                game = BASE_GAMEDIR;
            }

            Reset();
            src.oSet(model);
            dest.oSet(model);
            dest.SetFileExtension(MD5_MESH_EXT);

            commandLine.oSet(String.format("mesh %s -dest %s -game %s", src.toString(), dest.toString(), game));
            if (!ConvertMayaToMD5()) {
                gameLocal.Printf("Failed to export '%s' : %s", src, Maya_Error);
                return false;
            }

            return true;
        }

        public boolean ExportAnim(final String anim) {
            String game = cvarSystem.GetCVarString("fs_game");
            if (isNotNullOrEmpty(game)) {
                game = BASE_GAMEDIR;
            }

            Reset();
            src.oSet(anim);
            dest.oSet(anim);
            dest.SetFileExtension(MD5_ANIM_EXT);

            commandLine.oSet(String.format("anim %s -dest %s -game %s", src, dest, game));
            if (!ConvertMayaToMD5()) {
                gameLocal.Printf("Failed to export '%s' : %s", src, Maya_Error);
                return false;
            }

            return true;
        }

        public int ExportModels(final String pathname, final String extension) {
            int count;

//	count = 0;
            idFileList files;
            int i;

            if (!CheckMayaInstall()) {
                // if Maya isn't installed, don't bother checking if we have anims to export
                return 0;
            }

            gameLocal.Printf("--------- Exporting models --------\n");
            if (isNotNullOrEmpty(g_exportMask.GetString())) {
                gameLocal.Printf("  Export mask: '%s'\n", g_exportMask.GetString());
            }

            count = 0;

            files = fileSystem.ListFiles(pathname, extension);
            for (i = 0; i < files.GetNumFiles(); i++) {
                count += ExportDefFile(va("%s/%s", pathname, files.GetFile(i)));
            }
            fileSystem.FreeFileList(files);

            gameLocal.Printf("...%d models exported.\n", count);
            gameLocal.Printf("-----------------------------------\n");

            return count;
        }
    };
}
