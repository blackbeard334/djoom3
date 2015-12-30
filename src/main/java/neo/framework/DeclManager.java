package neo.framework;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import neo.Renderer.Material.idMaterial;
import neo.Sound.snd_shader.idSoundShader;
import static neo.Sound.snd_system.soundSystem;
import neo.TempDump.CPP_class.Pointer;
import static neo.TempDump.atobb;
import static neo.TempDump.bbtocb;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import static neo.framework.Common.common;
import neo.framework.DeclAF.idDeclAF;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclFX.idDeclFX;
import static neo.framework.DeclManager.declState_t.DS_DEFAULTED;
import static neo.framework.DeclManager.declState_t.DS_PARSED;
import static neo.framework.DeclManager.declState_t.DS_UNPARSED;
import static neo.framework.DeclManager.declType_t.DECL_AF;
import static neo.framework.DeclManager.declType_t.DECL_AUDIO;
import static neo.framework.DeclManager.declType_t.DECL_EMAIL;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_FX;
import static neo.framework.DeclManager.declType_t.DECL_MAPDEF;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.DeclManager.declType_t.DECL_MAX_TYPES;
import static neo.framework.DeclManager.declType_t.DECL_MODELEXPORT;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_PDA;
import static neo.framework.DeclManager.declType_t.DECL_SKIN;
import static neo.framework.DeclManager.declType_t.DECL_SOUND;
import static neo.framework.DeclManager.declType_t.DECL_TABLE;
import static neo.framework.DeclManager.declType_t.DECL_VIDEO;
import neo.framework.DeclManager.huffmanNode_s;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclManager.idDeclFile;
import neo.framework.DeclManager.idDeclLocal;
import neo.framework.DeclManager.idDeclType;
import neo.framework.DeclPDA.idDeclAudio;
import neo.framework.DeclPDA.idDeclEmail;
import neo.framework.DeclPDA.idDeclPDA;
import neo.framework.DeclPDA.idDeclVideo;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.framework.DeclTable.idDeclTable;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.FileSystem_h.idFileList;
import neo.framework.File_h.idFile;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Max;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import static neo.idlib.hashing.MD5.MD5_BlockChecksum;

/**
 *
 */
public class DeclManager {

    private static idDeclManagerLocal declManagerLocal = new idDeclManagerLocal();
    public static idDeclManager declManager = declManagerLocal;

    static final boolean USE_COMPRESSED_DECLS = true;
    static final boolean GET_HUFFMAN_FREQUENCIES = false;

    /*
     ===============================================================================

     Declaration Manager

     All "small text" data types, like materials, sound shaders, fx files,
     entity defs, etc. are managed uniformly, allowing reloading, purging,
     listing, printing, etc. All "large text" data types that never have more
     than one declaration in a given file, like maps, models, AAS files, etc.
     are not handled here.

     A decl will never, ever go away once it is created. The manager is
     guaranteed to always return the same decl pointer for a decl type/name
     combination. The index of a decl in the per type list also stays the
     same throughout the lifetime of the engine. Although the pointer to
     a decl always stays the same, one should never maintain pointers to
     data inside decls. The data stored in a decl is not garranteed to stay
     the same for more than one engine frame.

     The decl indexes of explicitely defined decls are garrenteed to be
     consistent based on the parsed decl files. However, the indexes of
     implicit decls may be different based on the order in which levels
     are loaded.

     The decl namespaces are separate for each type. Comments for decls go
     above the text definition to keep them associated with the proper decl.

     During decl parsing, errors should never be issued, only warnings
     followed by a call to MakeDefault().

     ===============================================================================
     */
    public enum declType_t {

        DECL_TABLE,//0
        DECL_MATERIAL,
        DECL_SKIN,
        DECL_SOUND,
        DECL_ENTITYDEF,
        DECL_MODELDEF,
        DECL_FX,
        DECL_PARTICLE,
        DECL_AF,
        DECL_PDA,
        DECL_VIDEO,
        DECL_AUDIO,
        DECL_EMAIL,
        DECL_MODELEXPORT,
        DECL_MAPDEF,//14

        // new decl types can be added here
        _15_, _16_, _17_, _18_, _19_, _20_, _21_, _22_, _23_, _24_,
        _25_, _26_, _27_, _28_, _29_, _30_, _31_,
        DECL_MAX_TYPES//32
    };
//    

    public enum declState_t {

        DS_UNPARSED,
        DS_DEFAULTED, // set if a parse failed due to an error, or the lack of any source
        DS_PARSED
    };
//    
    public static final int DECL_LEXER_FLAGS
            = LEXFL_NOSTRINGCONCAT | // multiple strings seperated by whitespaces are not concatenated
            LEXFL_NOSTRINGESCAPECHARS | // no escape characters inside strings
            LEXFL_ALLOWPATHNAMES | // allow path seperators in names
            LEXFL_ALLOWMULTICHARLITERALS | // allow multi character literals
            LEXFL_ALLOWBACKSLASHSTRINGCONCAT | // allow multiple strings seperated by '\' to be concatenated
            LEXFL_NOFATALERRORS;		// just set a flag instead of fatal erroring

    public static final String[] listDeclStrings = {"current", "all", "ever", null};

    public static abstract class idDeclBase {

// public	abstract 				~idDeclBase() {};
        public abstract String GetName();

        public abstract declType_t GetType();

        public abstract declState_t GetState();

        public abstract boolean IsImplicit();

        public abstract boolean IsValid();

        public abstract void Invalidate();

        public abstract void Reload() throws idException;

        public abstract void EnsureNotPurged() throws idException;

        public abstract int Index();

        public abstract int GetLineNum();

        public abstract String GetFileName();

        public abstract void GetText(String[] text);

        public abstract int GetTextLength();

        public abstract void SetText(String text);

        public abstract boolean ReplaceSourceFileText() throws idException;

        public abstract boolean SourceFileChanged();

        public abstract void MakeDefault() throws idException;

        public abstract boolean EverReferenced();

        protected abstract boolean SetDefaultText();

        protected abstract String DefaultDefinition();

        protected abstract boolean Parse(String text, final int textLength) throws idException;

        protected abstract void FreeData();

        public abstract /*size_t*/ long Size();

        protected abstract void List() throws idException;

        protected abstract void Print();
    };

    public static class idDecl {

        public static final transient int SIZE = Pointer.SIZE;//base is an abstract class.

        public idDeclBase base;
        //
        //

        // The constructor should initialize variables such that
        // an immediate call to FreeData() does no harm.
        public idDecl() {
            base = null;
        }
        // public /*abstract*/ 				~idDecl() {};

        // Returns the name of the decl.
        public String GetName() {
            return base.GetName();
        }

        // Returns the decl type.
        public declType_t GetType() {
            return base.GetType();
        }

        // Returns the decl state which is usefull for finding out if a decl defaulted.
        public declState_t GetState() {
            return base.GetState();
        }

        // Returns true if the decl was defaulted or the text was created with a call to SetDefaultText.
        public boolean IsImplicit() {
            return base.IsImplicit();
        }

        // The only way non-manager code can have an invalid decl is if the *ByIndex()
        // call was used with forceParse = false to walk the lists to look at names
        // without touching the media.
        public boolean IsValid() {
            return base.IsValid();
        }

        // Sets state back to unparsed.
        // Used by decl editors to undo any changes to the decl.
        public void Invalidate() {
            base.Invalidate();
        }

        // if a pointer might possible be stale from a previous level,
        // call this to have it re-parsed
        public void EnsureNotPurged() throws idException {
            base.EnsureNotPurged();
        }

        // Returns the index in the per-type list.
        public int Index() {
            return base.Index();
        }

        // Returns the line number the decl starts.
        public int GetLineNum() {
            return base.GetLineNum();
        }

        // Returns the name of the file in which the decl is defined.
        public String GetFileName() {
            return base.GetFileName();
        }

        // Returns the decl text.
        public void GetText(String[] text) {
            base.GetText(text);
        }

        // Returns the length of the decl text.
        public int GetTextLength() {
            return base.GetTextLength();
        }

        // Sets new decl text.
        public void SetText(String text) {
            base.SetText(text);
        }

        // Saves out new text for the decl.
        // Used by decl editors to replace the decl text in the source file.
        public boolean ReplaceSourceFileText() throws idException {
            return base.ReplaceSourceFileText();
        }

        // Returns true if the source file changed since it was loaded and parsed.
        public boolean SourceFileChanged() {
            return base.SourceFileChanged();
        }

        // Frees data and makes the decl a default.
        public void MakeDefault() throws idException {
            base.MakeDefault();
        }

        // Returns true if the decl was ever referenced.
        public boolean EverReferenced() {
            return base.EverReferenced();
        }

        // Sets textSource to a default text if necessary.
        // This may be overridden to provide a default definition based on the
        // decl name. For instance materials may default to an implicit definition
        // using a texture with the same name as the decl.
        public /*abstract*/ boolean SetDefaultText() throws idException {
            return base.SetDefaultText();
        }

        // Each declaration type must have a default string that it is guaranteed
        // to parse acceptably. When a decl is not explicitly found, is purged, or
        // has an error while parsing, MakeDefault() will do a FreeData(), then a
        // Parse() with DefaultDefinition(). The defaultDefintion should start with
        // an open brace and end with a close brace.
        public /*abstract*/ String DefaultDefinition() {
            return base.DefaultDefinition();
        }

        // The manager will have already parsed past the type, name and opening brace.
        // All necessary media will be touched before return.
        // The manager will have called FreeData() before issuing a Parse().
        // The subclass can call MakeDefault() internally at any point if
        // there are parse errors.
        public /*abstract*/ boolean Parse(String text, final int textLength) throws idException {
            return base.Parse(text, textLength);
        }

        // Frees any pointers held by the subclass. This may be called before
        // any Parse(), so the constructor must have set sane values. The decl will be
        // invalid after issuing this call, but it will always be immediately followed
        // by a Parse()
        public /*abstract*/ void FreeData() {
            base.FreeData();
        }

        // Returns the size of the decl in memory.
        public /*abstract*/ /*size_t*/ long Size() {
            return base.Size();
        }

        // If this isn't overridden, it will just print the decl name.
        // The manager will have printed 7 characters on the line already,
        // containing the reference state and index number.
        public /*abstract*/ void List() throws idException {
            base.List();
        }

        // The print function will already have dumped the text source
        // and common data, subclasses can override this to dump more
        // explicit data.
        public /*abstract*/ void Print() throws idException {
            base.Print();
        }
    };

    public static Constructor<idDecl> idDeclAllocator(Class/*<idDecl>*/ theMobRules) {
        //TODO:use reflection. EDIT:cross fingers.
        try {
            return theMobRules.getConstructor();
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(DeclManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static abstract class idDeclManager {

        // virtual 					~idDeclManager() {}
//
        public abstract void Init() throws idException;

        public abstract void Shutdown();

        public abstract void Reload(boolean force) throws idException;

        public abstract void BeginLevelLoad() throws idException;

        public abstract void EndLevelLoad();

        // Registers a new decl type.
        public abstract void RegisterDeclType(final String typeName, declType_t type, Constructor<idDecl> allocator /* *(*allocator)()*/) throws idException;

        // Registers a new folder with decl files.
        public abstract void RegisterDeclFolder(final String folder, final String extension, declType_t defaultType) throws idException;

        // Returns a checksum for all loaded decl text.
        public abstract BigInteger GetChecksum();

        // Returns the number of decl types.
        public abstract int GetNumDeclTypes();

        // Returns the type name for a decl type.
        public abstract String GetDeclNameFromType(declType_t type) throws idException;

        // Returns the decl type for a type name.
        public abstract declType_t GetDeclTypeFromName(final String typeName);

        // If makeDefault is true, a default decl of appropriate type will be created
        // if an explicit one isn't found. If makeDefault is false, NULL will be returned
        // if the decl wasn't explcitly defined.
        public abstract idDecl FindType(declType_t type, idStr name, boolean makeDefault /*= true*/) throws idException;

        public idDecl FindType(declType_t type, idStr name) throws idException {
            return FindType(type, name, true);
        }

        @Deprecated//name could have a back reference.
        public idDecl FindType(declType_t type, String name, boolean makeDefault) {
            return FindType(type, new idStr(name), makeDefault);
        }

        @Deprecated//name could have a back reference.
        public idDecl FindType(declType_t type, String name) {
            return FindType(type, new idStr(name));
        }

        public abstract idDecl FindDeclWithoutParsing(declType_t type, final String name, boolean makeDefault /*= true*/) throws idException;

        public idDecl FindDeclWithoutParsing(declType_t type, final String name) throws idException {
            return FindDeclWithoutParsing(type, name, true);
        }

        public abstract void ReloadFile(final String filename, boolean force) throws idException;

        // Returns the number of decls of the given type.
        public abstract int GetNumDecls(int type) throws idException;

        public int GetNumDecls(declType_t type) throws idException {
            return GetNumDecls(etoi(type));
        }

        // The complete lists of decls can be walked to populate editor browsers.
        // If forceParse is set false, you can get the decl to check name / filename / etc.
        // without causing it to parse the source and load media.
        public abstract idDecl DeclByIndex(declType_t type, int index, boolean forceParse /*= true*/) throws idException;

        public abstract idDecl DeclByIndex(declType_t type, int index) throws idException;

        // List and print decls.
        public abstract void ListType(final idCmdArgs args, declType_t type) throws idException;

        public abstract void PrintType(final idCmdArgs args, declType_t type) throws idException;

        // Creates a new default decl of the given type with the given name in
        // the given file used by editors to create a new decls.
        public abstract idDecl CreateNewDecl(declType_t type, final String name, final String fileName) throws idException;

        // BSM - Added for the material editors rename capabilities
        public abstract boolean RenameDecl(declType_t type, final String oldName, final String newName);

        // When media files are loaded, a reference line can be printed at a
        // proper indentation if decl_show is set
        public abstract void MediaPrint(final String fmt, final Object... arg) /*id_attribute((format(printf,2,3))) */ throws idException;

        public abstract void WritePrecacheCommands(idFile f);

        // Convenience functions for specific types.
        public abstract idMaterial FindMaterial(idStr name, boolean makeDefault /*= true*/) throws idException;

        public idMaterial FindMaterial(idStr name) throws idException {
            return FindMaterial(name, true);
        }

        @Deprecated//name could have a back reference.
        public idMaterial FindMaterial(String name, boolean makeDefault) {
            return FindMaterial(new idStr(name), makeDefault);
        }

        @Deprecated//name could have a back reference.
        public idMaterial FindMaterial(String name) {
            return FindMaterial(new idStr(name));
        }

        public abstract idDeclSkin FindSkin(idStr name, boolean makeDefault/* = true*/) throws idException;

        public idDeclSkin FindSkin(idStr name) throws idException {
            return FindSkin(name, true);
        }

        @Deprecated//name could have a back reference.
        public idDeclSkin FindSkin(String name, boolean makeDefault) {
            return FindSkin(new idStr(name), makeDefault);
        }

        @Deprecated//name could have a back reference.
        public idDeclSkin FindSkin(String name) {
            return FindSkin(new idStr(name));
        }

        public abstract idSoundShader FindSound(idStr name, boolean makeDefault/* = true*/) throws idException;

        public idSoundShader FindSound(idStr name) throws idException {
            return FindSound(name, true);
        }

        @Deprecated//name could have a back reference.
        public idSoundShader FindSound(String name, boolean makeDefault) {
            return FindSound(new idStr(name), makeDefault);
        }

        @Deprecated//name could have a back reference.
        public idSoundShader FindSound(String name) {
            return FindSound(new idStr(name));
        }

        public abstract idMaterial MaterialByIndex(int index, boolean forceParse /*= true*/) throws idException;

        public idMaterial MaterialByIndex(int index) throws idException {
            return MaterialByIndex(index, true);
        }

        public abstract idDeclSkin SkinByIndex(int index, boolean forceParse /*= true */) throws idException;

        public idDeclSkin SkinByIndex(int index) throws idException {
            return SkinByIndex(index, true);
        }

        public abstract idSoundShader SoundByIndex(int index, boolean forceParse /*= true*/) throws idException;

        public idSoundShader SoundByIndex(int index) throws idException {
            return SoundByIndex(index, true);
        }

    };

    public static class idListDecls_f extends cmdFunction_t {

        private final declType_t type;

        public idListDecls_f(declType_t type) {
            this.type = type;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            declManager.ListType(args, type);
        }
    };

    public static class idPrintDecls_f extends cmdFunction_t {

        private final declType_t type;

        public idPrintDecls_f(declType_t type) {
            this.type = type;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            declManager.PrintType(args, type);
        }
    };

    static class idDeclType {

        public idStr typeName;
        public declType_t type;
        public Constructor<idDecl> allocator;//(*allocator)( void );
    };

    static class idDeclFolder {

        public idStr folder;
        public idStr extension;
        public declType_t defaultType;
    };

    static class idDeclLocal extends idDeclBase {

        private idDecl self;
        //
        private idStr name;			// name of the decl
        private ByteBuffer textSource;		// decl text definition
        private int textLength;			// length of textSource
        private int compressedLength;		// compressed length
        private idDeclFile sourceFile;		// source file in which the decl was defined
        private int sourceTextOffset;		// offset in source file to decl text
        private int sourceTextLength;		// length of decl text in source file
        private int sourceLine;			// this is where the actual declaration token starts
        private BigInteger checksum;		// checksum of the decl text
        private declType_t type;		// decl type
        private declState_t declState;		// decl state
        private int index;			// index in the per-type list
        //
        private boolean parsedOutsideLevelLoad;	// these decls will never be purged
        private boolean everReferenced;		// set to true if the decl was ever used
        private boolean referencedThisLevel;	// set to true when the decl is used for the current level
        private boolean redefinedInReload;	// used during file reloading to make sure a decl that has its source removed will be defaulted
        //
        private idDeclLocal nextInFile;		// next decl in the decl file
        //
        //

        public idDeclLocal() {
            name = new idStr("unnamed");
            textSource = null;
            textLength = 0;
            compressedLength = 0;
            sourceFile = null;
            sourceTextOffset = 0;
            sourceTextLength = 0;
            sourceLine = 0;
            checksum = BigInteger.ZERO;
            type = DECL_ENTITYDEF;
            index = 0;
            declState = DS_UNPARSED;
            parsedOutsideLevelLoad = false;
            referencedThisLevel = false;
            everReferenced = false;
            redefinedInReload = false;
            nextInFile = null;
        }

        @Override
        public String GetName() {
            return name.toString();
        }

        @Override
        public declType_t GetType() {
            return type;
        }

        @Override
        public declState_t GetState() {
            return declState;
        }

        @Override
        public boolean IsImplicit() {
            return (sourceFile == declManagerLocal.GetImplicitDeclFile());
        }

        @Override
        public boolean IsValid() {
            return (declState != DS_UNPARSED);
        }

        @Override
        public void Invalidate() {
            declState = DS_UNPARSED;
        }

        @Override
        public void Reload() throws idException {
            this.sourceFile.Reload(false);
        }

        @Override
        public void EnsureNotPurged() throws idException {
            if (declState == DS_UNPARSED) {
                ParseLocal();
            }
        }

        @Override
        public int Index() {
            return index;
        }

        @Override
        public int GetLineNum() {
            return sourceLine;
        }

        @Override
        public String GetFileName() {
            return (sourceFile != null) ? sourceFile.fileName.toString() : "*invalid*";
        }

        @Override
        public void GetText(String[] text) {
            if (USE_COMPRESSED_DECLS) {
                HuffmanDecompressText(text, textLength, textSource, compressedLength);
            } else {
                // memcpy( text, textSource, textLength+1 );
            }
        }

        @Override
        public int GetTextLength() {
            return textLength;
        }

        @Override
        public void SetText(String text) {
            SetTextLocal(text, text.length());
        }

        @Override
        public boolean ReplaceSourceFileText() throws idException {
            int oldFileLength, newFileLength;
            byte[] buffer;
            idFile file;

            common.Printf("Writing \'%s\' to \'%s\'...\n", GetName(), GetFileName());

            if (sourceFile == declManagerLocal.implicitDecls) {
                common.Warning("Can't save implicit declaration %s.", GetName());
                return false;
            }

            // get length and allocate buffer to hold the file
            oldFileLength = sourceFile.fileSize;
            newFileLength = oldFileLength - sourceTextLength + textLength;
//            buffer = (char[]) Mem_Alloc(Max(newFileLength, oldFileLength));
            buffer = new byte[Max(newFileLength, oldFileLength)];

            // read original file
            if (sourceFile.fileSize != 0) {

                file = fileSystem.OpenFileRead(GetFileName());
                if (null == file) {
//                    Mem_Free(buffer);
                    common.Warning("Couldn't open %s for reading.", GetFileName());
                    return false;
                }

                if (file.Length() != sourceFile.fileSize || file.Timestamp() != sourceFile.timestamp[0]) {
//                    Mem_Free(buffer);
                    common.Warning("The file %s has been modified outside of the engine.", GetFileName());
                    return false;
                }

                file.Read(ByteBuffer.wrap(buffer), oldFileLength);
                fileSystem.CloseFile(file);

                if (!MD5_BlockChecksum(buffer, oldFileLength).equals(sourceFile.checksum)) {
//                    Mem_Free(buffer);
                    common.Warning("The file %s has been modified outside of the engine.", GetFileName());
                    return false;
                }
            }

            // insert new text
            char[] declText;//= new char[textLength + 1];
            String[] declString = new String[1];
            GetText(declString);
            declText = declString[0].toCharArray();
//	memmove( buffer + sourceTextOffset + textLength, buffer + sourceTextOffset + sourceTextLength, oldFileLength - sourceTextOffset - sourceTextLength );
            System.arraycopy(buffer, sourceTextOffset + sourceTextLength, buffer, sourceTextOffset + textLength, oldFileLength - sourceTextOffset - sourceTextLength);
//	memcpy( buffer + sourceTextOffset, declText, textLength );
            System.arraycopy(declText, 0, buffer, sourceTextOffset, textLength);

            // write out new file
            file = fileSystem.OpenFileWrite(GetFileName(), "fs_devpath");
            if (null == file) {
//                Mem_Free(buffer);
                common.Warning("Couldn't open %s for writing.", GetFileName());
                return false;
            }
            file.Write(ByteBuffer.wrap(buffer), newFileLength);
            fileSystem.CloseFile(file);

            // set new file size, checksum and timestamp
            sourceFile.fileSize = newFileLength;
            sourceFile.checksum = new BigInteger(MD5_BlockChecksum(buffer, newFileLength));
            fileSystem.ReadFile(GetFileName(), null, sourceFile.timestamp);

            // free buffer
//            Mem_Free(buffer);
            // move all decls in the same file
            for (idDeclLocal decl = sourceFile.decls; decl != null; decl = decl.nextInFile) {
                if (decl.sourceTextOffset > sourceTextOffset) {
                    decl.sourceTextOffset += textLength - sourceTextLength;
                }
            }

            // set new size of text in source file
            sourceTextLength = textLength;

            return true;
        }

        @Override
        public boolean SourceFileChanged() {
            int newLength;
            /*ID_TIME_T*/ long[] newTimestamp = new long[1];

            if (sourceFile.fileSize <= 0) {
                return false;
            }

            newLength = fileSystem.ReadFile(GetFileName(), null, newTimestamp);

            if (newLength != sourceFile.fileSize || newTimestamp != sourceFile.timestamp) {
                return true;
            }

            return false;
        }
        private static int recursionLevel;

        @Override
        public void MakeDefault() throws idException {
            final String defaultText;

            declManagerLocal.MediaPrint("DEFAULTED\n");
            declState = DS_DEFAULTED;

            AllocateSelf();

            defaultText = self.DefaultDefinition();

            // a parse error inside a DefaultDefinition() string could
            // cause an infinite loop, but normal default definitions could
            // still reference other default definitions, so we can't
            // just dump out on the first recursion
            if (++recursionLevel > 100) {
                common.FatalError("idDecl::MakeDefault: bad DefaultDefinition(): %s", defaultText);
            }

            // always free data before parsing
            self.FreeData();

            // parse
            self.Parse(defaultText, defaultText.length());

            // we could still eventually hit the recursion if we have enough Error() calls inside Parse...
            --recursionLevel;
        }

        @Override
        public boolean EverReferenced() {
            return everReferenced;
        }

        @Override
        public long Size() {
            return /*sizeof(idDecl) +*/ name.Allocated();
        }

        @Override
        protected boolean SetDefaultText() {
            return false;
        }

        @Override
        protected String DefaultDefinition() {
            return "{ }";
        }

        @Override
        protected boolean Parse(String text, int textLength) throws idException {
            idLexer src = new idLexer();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");
            src.SkipBracedSection(false);
            return true;
        }

        @Override
        protected void FreeData() {
        }

        @Override
        protected void List() throws idException {
            common.Printf("%s\n", GetName());
        }

        @Override
        protected void Print() {
        }

        private static int DBG_AllocateSelf = 0;
        protected void AllocateSelf() {
            if (null == self) {
                try {
                    DBG_AllocateSelf++;
                    self = declManagerLocal.GetDeclType(etoi(type)).allocator.newInstance();
                    self.base = this;
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(DeclManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        // Parses the decl definition.
        // After calling parse, a decl will be guaranteed usable.
        protected void ParseLocal() throws idException {
            boolean generatedDefaultText = false;

            AllocateSelf();

            // always free data before parsing
            self.FreeData();

            declManagerLocal.MediaPrint("parsing %s %s\n", declManagerLocal.declTypes.oGet(type.ordinal()).typeName, name);

            // if no text source try to generate default text
            if (textSource == null) {
                generatedDefaultText = self.SetDefaultText();
            }

            // indent for DEFAULTED or media file references
            declManagerLocal.indent++;

            // no text immediately causes a MakeDefault()
            if (textSource == null) {
                MakeDefault();
                declManagerLocal.indent--;
                return;
            }

            declState = DS_PARSED;

            // parse
            String[] declText = {null};/*(char *) _alloca( ( GetTextLength() + 1 ) * sizeof( char ) )*/;
            GetText(declText);
            self.Parse(declText[0], GetTextLength());

            // free generated text
            if (generatedDefaultText) {
//                Mem_Free(textSource);
                textSource = null;
                textLength = 0;
            }

            declManagerLocal.indent--;
        }

        // Does a MakeDefualt, but flags the decl so that it
        // will Parse() the next time the decl is found.
        protected void Purge() throws idException {
            // never purge things that were referenced outside level load,
            // like the console and menu graphics
            if (parsedOutsideLevelLoad) {
                return;
            }

            referencedThisLevel = false;
            MakeDefault();

            // the next Find() for this will re-parse the real data
            declState = DS_UNPARSED;
        }

        // Set textSource possible with compression.
        protected void SetTextLocal(final String text, final int length) {

//            Mem_Free(textSource);
            textSource = null;

            checksum = new BigInteger(MD5_BlockChecksum(text, length));

            if (GET_HUFFMAN_FREQUENCIES) {
                for (int i = 0; i < length; i++) {
//		huffmanFrequencies[((const unsigned char *)text)[i]]++;
                    huffmanFrequencies[text.charAt(i) & 0xff]++;
                }
            }

            if (USE_COMPRESSED_DECLS) {
                int maxBytesPerCode = (maxHuffmanBits + 7) >> 3;
                ByteBuffer compressed = ByteBuffer.allocate(length * maxBytesPerCode);
                compressedLength = HuffmanCompressText(text, length, compressed, length * maxBytesPerCode);
                compressed.rewind();
                textSource = compressed;//(char *)Mem_Alloc( compressedLength );
//	memcpy( textSource, compressed, compressedLength );
            } else {
                compressedLength = length;
                textSource = atobb(text);//(char *) Mem_Alloc( length + 1 );
//	memcpy( textSource, text, length );
//	textSource[length] = '\0';
            }
            textLength = length;
        }
    };

    static class idDeclFile {

        public idStr fileName;
        public declType_t defaultType;
        //
        public /*ID_TIME_T*/ long[] timestamp = new long[1];
        public BigInteger checksum;
        public int fileSize;
        public int numLines;
        //
        public idDeclLocal decls;
        //
        //

        public idDeclFile() {
            this.fileName = new idStr("<implicit file>");
            this.defaultType = DECL_MAX_TYPES;
            this.timestamp[0] = 0;
            this.checksum = BigInteger.ZERO;
            this.fileSize = 0;
            this.numLines = 0;
            this.decls = null;
        }

        public idDeclFile(final String fileName, declType_t defaultType) {
            this.fileName = new idStr(fileName);
            this.defaultType = defaultType;
            this.timestamp[0] = 0;
            this.checksum = BigInteger.ZERO;
            this.fileSize = 0;
            this.numLines = 0;
            this.decls = null;
        }
//

        /*
         ================
         idDeclFile::Reload

         ForceReload will cause it to reload even if the timestamp hasn't changed
         ================
         */
        public void Reload(boolean force) throws idException {
            // check for an unchanged timestamp
            if (!force && timestamp[0] != 0) {
                /*ID_TIME_T*/ long[] testTimeStamp = new long[1];
                fileSystem.ReadFile(fileName.toString(), null, testTimeStamp);

                if (testTimeStamp == timestamp) {
                    return;
                }
            }

            // parse the text
            LoadAndParse();
        }

        /*
         ================
         idDeclFile::LoadAndParse

         This is used during both the initial load, and any reloads
         ================
         */
        int c_savedMemory = 0;

        public BigInteger LoadAndParse() throws idException {
            int i, numTypes;
            idLexer src = new idLexer();
            idToken token = new idToken();
            int startMarker;
            ByteBuffer[] buffer = {null};
            int length, size;
            int sourceLine;
            String name;
            idDeclLocal newDecl;
            boolean reparse;

            // load the text
            common.DPrintf("...loading '%s'\n", fileName.toString());
            length = fileSystem.ReadFile(fileName.toString(), buffer, timestamp);
            if (length == -1) {
                common.FatalError("couldn't load %s", fileName.toString());
                return BigInteger.ZERO;
            }

            if (!src.LoadMemory(bbtocb(buffer[0]), length, fileName.toString())) {
                common.Error("Couldn't parse %s", fileName.toString());
//                Mem_Free(buffer);
                return BigInteger.ZERO;
            }

            // mark all the defs that were from the last reload of this file
            for (idDeclLocal decl = decls; decl != null; decl = decl.nextInFile) {
                decl.redefinedInReload = false;
            }

            src.SetFlags(DECL_LEXER_FLAGS);

            checksum = new BigInteger(MD5_BlockChecksum(buffer[0].array(), length));

            fileSize = length;

            // scan through, identifying each individual declaration
            while (true) {

                startMarker = src.GetFileOffset();
                sourceLine = src.GetLineNum();

                // parse the decl type name
                if (!src.ReadToken(token)) {
                    break;
                }

                declType_t identifiedType = DECL_MAX_TYPES;

                // get the decl type from the type name
                numTypes = declManagerLocal.GetNumDeclTypes();
                for (i = 0; i < numTypes; i++) {
                    idDeclType typeInfo = declManagerLocal.GetDeclType(i);
                    if (typeInfo != null && typeInfo.typeName.Icmp(token.toString()) == 0) {
                        identifiedType = (declType_t) typeInfo.type;
                        break;
                    }
                }

                if (i >= numTypes) {

                    if (token.Icmp("{") == 0) {

                        // if we ever see an open brace, we somehow missed the [type] <name> prefix
                        src.Warning("Missing decl name");
                        src.SkipBracedSection(false);
                        continue;

                    } else {

                        if (defaultType == DECL_MAX_TYPES) {
                            src.Warning("No type");
                            continue;
                        }
                        src.UnreadToken(token);
                        // use the default type
                        identifiedType = defaultType;
                    }
                }

                // now parse the name
                if (!src.ReadToken(token)) {
                    src.Warning("Type without definition at end of file");
                    break;
                }

                if (0 == token.Icmp("{")) {
                    // if we ever see an open brace, we somehow missed the [type] <name> prefix
                    src.Warning("Missing decl name");
                    src.SkipBracedSection(false);
                    continue;
                }

                // FIXME: export decls are only used by the model exporter, they are skipped here for now
                if (identifiedType == DECL_MODELEXPORT) {
                    src.SkipBracedSection();
                    continue;
                }

                name = token.toString();

                // make sure there's a '{'
                if (!src.ReadToken(token)) {
                    src.Warning("Type without definition at end of file");
                    break;
                }
                if (!token.equals("{")) {
                    src.Warning("Expecting '{' but found '%s'", token);
                    continue;
                }
                src.UnreadToken(token);

                // now take everything until a matched closing brace
                src.SkipBracedSection();
                size = src.GetFileOffset() - startMarker;

                // look it up, possibly getting a newly created default decl
                reparse = false;
                newDecl = declManagerLocal.FindTypeWithoutParsing(identifiedType, name, false);
                if (newDecl != null) {
                    // update the existing copy
                    if (newDecl.sourceFile != this || newDecl.redefinedInReload) {
                        src.Warning("%s '%s' previously defined at %s:%d", declManagerLocal.GetDeclNameFromType(identifiedType), name, newDecl.sourceFile.fileName.toString(), newDecl.sourceLine);
                        continue;
                    }
                    if (newDecl.declState != DS_UNPARSED) {
                        reparse = true;
                    }
                } else {
                    // allow it to be created as a default, then add it to the per-file list
                    newDecl = declManagerLocal.FindTypeWithoutParsing(identifiedType, name, true);
                    newDecl.nextInFile = this.decls;
                    this.decls = newDecl;
                }

                newDecl.redefinedInReload = true;

                if (newDecl.textSource != null) {
//                    Mem_Free(newDecl.textSource);
                    newDecl.textSource = null;
                }

                newDecl.SetTextLocal(new String(buffer[0].array()).substring(startMarker), size);
                newDecl.sourceFile = this;
                newDecl.sourceTextOffset = startMarker;
                newDecl.sourceTextLength = size;
                newDecl.sourceLine = sourceLine;
                newDecl.declState = DS_UNPARSED;

                // if it is currently in use, reparse it immedaitely
                if (reparse) {
                    newDecl.ParseLocal();
                }
            }

            numLines = src.GetLineNum();

//            Mem_Free(buffer);
            // any defs that weren't redefinedInReload should now be defaulted
            for (idDeclLocal decl = decls; decl != null; decl = decl.nextInFile) {
                if (decl.redefinedInReload == false) {
                    decl.MakeDefault();
                    decl.sourceTextOffset = decl.sourceFile.fileSize;
                    decl.sourceTextLength = 0;
                    decl.sourceLine = decl.sourceFile.numLines;
                }
            }

            return checksum;
        }
    };

    static class idDeclManagerLocal extends idDeclManager {

        private idList<idDeclType> declTypes;
        private idList<idDeclFolder> declFolders;
        //
        private idList<idDeclFile> loadedFiles;
        private final idHashIndex[] hashTables;
        private final idList<idDeclLocal>[] linearLists;
        private idDeclFile implicitDecls;// this holds all the decls that were created because explicit
        //                               // text definitions were not found. Decls that became default
        //                               // because of a parse error are not in this list.
        private BigInteger checksum;     // checksum of all loaded decl text
        private int indent;		 // for MediaPrint
        private boolean insideLevelLoad;
        //
        private static final idCVar decl_show = new idCVar("decl_show", "0", CVAR_SYSTEM, "set to 1 to print parses, 2 to also print references", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        //
        //

        idDeclManagerLocal() {
            this.declTypes = new idList<>();
            this.declFolders = new idList<>();

            this.loadedFiles = new idList<>();
            this.hashTables = new idHashIndex[etoi(DECL_MAX_TYPES)];
            this.linearLists = new idList[etoi(DECL_MAX_TYPES)];

            for (int d = 0; d < etoi(DECL_MAX_TYPES); d++) {
                hashTables[d] = new idHashIndex();
                linearLists[d] = new idList<>();
            }
        }

        @Override
        public void Init() throws idException {

            common.Printf("----- Initializing Decls -----\n");

            checksum = BigInteger.ZERO;

            if (USE_COMPRESSED_DECLS) {
                SetupHuffman();
            }

            if (GET_HUFFMAN_FREQUENCIES) {
                ClearHuffmanFrequencies();
            }

            // decls used throughout the engine
            RegisterDeclType("table", DECL_TABLE, idDeclAllocator(idDeclTable.class));
            RegisterDeclType("material", DECL_MATERIAL, idDeclAllocator(idMaterial.class));
            RegisterDeclType("skin", DECL_SKIN, idDeclAllocator(idDeclSkin.class));
            RegisterDeclType("sound", DECL_SOUND, idDeclAllocator(idSoundShader.class));

            RegisterDeclType("entityDef", DECL_ENTITYDEF, idDeclAllocator(idDeclEntityDef.class));
            RegisterDeclType("mapDef", DECL_MAPDEF, idDeclAllocator(idDeclEntityDef.class));
            RegisterDeclType("fx", DECL_FX, idDeclAllocator(idDeclFX.class));
            RegisterDeclType("particle", DECL_PARTICLE, idDeclAllocator(idDeclParticle.class));
            RegisterDeclType("articulatedFigure", DECL_AF, idDeclAllocator(idDeclAF.class));
            RegisterDeclType("pda", DECL_PDA, idDeclAllocator(idDeclPDA.class));
            RegisterDeclType("email", DECL_EMAIL, idDeclAllocator(idDeclEmail.class));
            RegisterDeclType("video", DECL_VIDEO, idDeclAllocator(idDeclVideo.class));
            RegisterDeclType("audio", DECL_AUDIO, idDeclAllocator(idDeclAudio.class));

            RegisterDeclFolder("materials", ".mtr", DECL_MATERIAL);
            RegisterDeclFolder("skins", ".skin", DECL_SKIN);
            RegisterDeclFolder("sound", ".sndshd", DECL_SOUND);

            // add console commands
            cmdSystem.AddCommand("listDecls", ListDecls_f.getInstance(), CMD_FL_SYSTEM, "lists all decls");

            cmdSystem.AddCommand("reloadDecls", ReloadDecls_f.getInstance(), CMD_FL_SYSTEM, "reloads decls");
            cmdSystem.AddCommand("touch", TouchDecl_f.getInstance(), CMD_FL_SYSTEM, "touches a decl");

            cmdSystem.AddCommand("listTables", new idListDecls_f(DECL_TABLE), CMD_FL_SYSTEM, "lists tables", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listMaterials", new idListDecls_f(DECL_MATERIAL), CMD_FL_SYSTEM, "lists materials", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listSkins", new idListDecls_f(DECL_SKIN), CMD_FL_SYSTEM, "lists skins", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listSoundShaders", new idListDecls_f(DECL_SOUND), CMD_FL_SYSTEM, "lists sound shaders", new idCmdSystem.ArgCompletion_String(listDeclStrings));

            cmdSystem.AddCommand("listEntityDefs", new idListDecls_f(DECL_ENTITYDEF), CMD_FL_SYSTEM, "lists entity defs", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listFX", new idListDecls_f(DECL_FX), CMD_FL_SYSTEM, "lists FX systems", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listParticles", new idListDecls_f(DECL_PARTICLE), CMD_FL_SYSTEM, "lists particle systems", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listAF", new idListDecls_f(DECL_AF), CMD_FL_SYSTEM, "lists articulated figures", new idCmdSystem.ArgCompletion_String(listDeclStrings));

            cmdSystem.AddCommand("listPDAs", new idListDecls_f(DECL_PDA), CMD_FL_SYSTEM, "lists PDAs", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listEmails", new idListDecls_f(DECL_EMAIL), CMD_FL_SYSTEM, "lists Emails", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listVideos", new idListDecls_f(DECL_VIDEO), CMD_FL_SYSTEM, "lists Videos", new idCmdSystem.ArgCompletion_String(listDeclStrings));
            cmdSystem.AddCommand("listAudios", new idListDecls_f(DECL_AUDIO), CMD_FL_SYSTEM, "lists Audios", new idCmdSystem.ArgCompletion_String(listDeclStrings));

            cmdSystem.AddCommand("printTable", new idPrintDecls_f(DECL_TABLE), CMD_FL_SYSTEM, "prints a table", new idCmdSystem.ArgCompletion_Decl(DECL_TABLE));
            cmdSystem.AddCommand("printMaterial", new idPrintDecls_f(DECL_MATERIAL), CMD_FL_SYSTEM, "prints a material", new idCmdSystem.ArgCompletion_Decl(DECL_MATERIAL));
            cmdSystem.AddCommand("printSkin", new idPrintDecls_f(DECL_SKIN), CMD_FL_SYSTEM, "prints a skin", new idCmdSystem.ArgCompletion_Decl(DECL_SKIN));
            cmdSystem.AddCommand("printSoundShader", new idPrintDecls_f(DECL_SOUND), CMD_FL_SYSTEM, "prints a sound shader", new idCmdSystem.ArgCompletion_Decl(DECL_SOUND));

            cmdSystem.AddCommand("printEntityDef", new idPrintDecls_f(DECL_ENTITYDEF), CMD_FL_SYSTEM, "prints an entity def", new idCmdSystem.ArgCompletion_Decl(DECL_ENTITYDEF));
            cmdSystem.AddCommand("printFX", new idPrintDecls_f(DECL_FX), CMD_FL_SYSTEM, "prints an FX system", new idCmdSystem.ArgCompletion_Decl(DECL_FX));
            cmdSystem.AddCommand("printParticle", new idPrintDecls_f(DECL_PARTICLE), CMD_FL_SYSTEM, "prints a particle system", new idCmdSystem.ArgCompletion_Decl(DECL_PARTICLE));
            cmdSystem.AddCommand("printAF", new idPrintDecls_f(DECL_AF), CMD_FL_SYSTEM, "prints an articulated figure", new idCmdSystem.ArgCompletion_Decl(DECL_AF));

            cmdSystem.AddCommand("printPDA", new idPrintDecls_f(DECL_PDA), CMD_FL_SYSTEM, "prints an PDA", new idCmdSystem.ArgCompletion_Decl(DECL_PDA));
            cmdSystem.AddCommand("printEmail", new idPrintDecls_f(DECL_EMAIL), CMD_FL_SYSTEM, "prints an Email", new idCmdSystem.ArgCompletion_Decl(DECL_EMAIL));
            cmdSystem.AddCommand("printVideo", new idPrintDecls_f(DECL_VIDEO), CMD_FL_SYSTEM, "prints a Audio", new idCmdSystem.ArgCompletion_Decl(DECL_VIDEO));
            cmdSystem.AddCommand("printAudio", new idPrintDecls_f(DECL_AUDIO), CMD_FL_SYSTEM, "prints an Video", new idCmdSystem.ArgCompletion_Decl(DECL_AUDIO));

            cmdSystem.AddCommand("listHuffmanFrequencies", ListHuffmanFrequencies_f.getInstance(), CMD_FL_SYSTEM, "lists decl text character frequencies");

            common.Printf("------------------------------\n");
        }

        @Override
        public void Shutdown() {
            int i, j;
            idDeclLocal decl;

            // free decls
            for (i = 0; i < DECL_MAX_TYPES.ordinal(); i++) {
                for (j = 0; j < linearLists[i].Num(); j++) {
                    decl = linearLists[i].oGet(j);
                    if (decl.self != null) {
                        decl.self.FreeData();
//				delete decl.self;
                    }
                    if (decl.textSource != null) {
//                        Mem_Free(decl.textSource);
                        decl.textSource = null;
                    }
//			delete decl;
                }
                linearLists[i].Clear();
                hashTables[i].Free();
            }

            // free decl files
            loadedFiles.DeleteContents(true);

            // free the decl types and folders
            declTypes.DeleteContents(true);
            declFolders.DeleteContents(true);

            if (USE_COMPRESSED_DECLS) {
                ShutdownHuffman();
            }
        }

        @Override
        public void Reload(boolean force) throws idException {
            for (int i = 0; i < loadedFiles.Num(); i++) {
                loadedFiles.oGet(i).Reload(force);
            }
        }

        @Override
        public void BeginLevelLoad() throws idException {
            insideLevelLoad = true;

            // clear all the referencedThisLevel flags and purge all the data
            // so the next reference will cause a reparse
            for (int i = 0; i < DECL_MAX_TYPES.ordinal(); i++) {
                int num = linearLists[i].Num();
                for (int j = 0; j < num; j++) {
                    idDeclLocal decl = linearLists[i].oGet(j);
                    decl.Purge();
                }
            }
        }

        @Override
        public void EndLevelLoad() {
            insideLevelLoad = false;

            // we don't need to do anything here, but the image manager, model manager,
            // and sound sample manager will need to free media that was not referenced
        }

        @Override
        public void RegisterDeclType(String typeName, declType_t type, Constructor<idDecl> allocator) throws idException {
            idDeclType declType;

            if (type.ordinal() < declTypes.Num() && declTypes.oGet(type.ordinal()) != null) {
                common.Warning("idDeclManager::RegisterDeclType: type '%s' already exists", typeName);
                return;
            }

            declType = new idDeclType();
            declType.typeName = new idStr(typeName);
            declType.type = type;
            declType.allocator = allocator;

            if (type.ordinal() + 1 > declTypes.Num()) {
                declTypes.AssureSize(type.ordinal() + 1, null);
            }
            declTypes.oSet(type.ordinal(), declType);
        }

        @Override
        public void RegisterDeclFolder(String folder, String extension, declType_t defaultType) throws idException {
            int i, j;
            idStr fileName;
            idDeclFolder declFolder;
            idFileList fileList;
            idDeclFile df;

            // check whether this folder / extension combination already exists
            for (i = 0; i < declFolders.Num(); i++) {
                if (declFolders.oGet(i).folder.Icmp(folder) == 0 && declFolders.oGet(i).extension.Icmp(extension) == 0) {
                    break;
                }
            }
            if (i < declFolders.Num()) {
                declFolder = declFolders.oGet(i);
            } else {
                declFolder = new idDeclFolder();
                declFolder.folder = new idStr(folder);
                declFolder.extension = new idStr(extension);
                declFolder.defaultType = defaultType;
                declFolders.Append(declFolder);
            }

            // scan for decl files
            fileList = fileSystem.ListFiles(declFolder.folder.toString(), declFolder.extension.toString(), true);

            // load and parse decl files
            for (i = 0; i < fileList.GetNumFiles(); i++) {
                fileName = new idStr(declFolder.folder + "/" + fileList.GetFile(i));

                // check whether this file has already been loaded
                for (j = 0; j < loadedFiles.Num(); j++) {
                    if (fileName.Icmp(loadedFiles.oGet(j).fileName.toString()) == 0) {
                        break;
                    }
                }
                if (j < loadedFiles.Num()) {
                    df = loadedFiles.oGet(j);
                } else {
                    df = new idDeclFile(fileName.toString(), defaultType);
                    loadedFiles.Append(df);
                }
                df.LoadAndParse();
            }

            fileSystem.FreeFileList(fileList);
        }

        @Override
        public BigInteger GetChecksum() {
            throw new UnsupportedOperationException();
//            int i, j, total, num;
//            BigInteger[] checksumData;
//
//            // get the total number of decls
//            total = 0;
//            for (i = 0; i < DECL_MAX_TYPES.ordinal(); i++) {
//                total += linearLists[i].Num();
//            }
//
//            checksumData = new BigInteger[total * 2];
//
//            total = 0;
//            for (i = 0; i < DECL_MAX_TYPES.ordinal(); i++) {
//                declType_t type = declType_t.values()[i];
//
//                // FIXME: not particularly pretty but PDAs and associated decls are localized and should not be checksummed
//                if (type == DECL_PDA || type == DECL_VIDEO || type == DECL_AUDIO || type == DECL_EMAIL) {
//                    continue;
//                }
//
//                num = linearLists[i].Num();
//                for (j = 0; j < num; j++) {
//                    idDeclLocal decl = linearLists[i].oGet(j);
//
//                    if (decl.sourceFile == implicitDecls) {
//                        continue;
//                    }
//
//                    checksumData[total * 2 + 0] = total;
//                    checksumData[total * 2 + 1] = decl.checksum;
//                    total++;
//                }
//            }
//
//            Lib.LittleRevBytes(checksumData, total * 2);
//            return MD5_BlockChecksum(checksumData, total * 2 /* sizeof(int)*/);
        }

        @Override
        public int GetNumDeclTypes() {
            return declTypes.Num();
        }

        @Override
        public String GetDeclNameFromType(declType_t type) throws idException {
            int typeIndex = type.ordinal();

            if (typeIndex < 0 || typeIndex >= declTypes.Num() || declTypes.oGet(typeIndex) == null) {
                common.FatalError("idDeclManager::GetDeclNameFromType: bad type: %d", typeIndex);
            }
            return declTypes.oGet(typeIndex).typeName.toString();
        }

        @Override
        public declType_t GetDeclTypeFromName(String typeName) {
            int i;

            for (i = 0; i < declTypes.Num(); i++) {
                if (declTypes.oGet(i) != null && declTypes.oGet(i).typeName.Icmp(typeName) == 0) {
                    return declTypes.oGet(i).type;
                }
            }
            return DECL_MAX_TYPES;
        }

        /*
         =================
         idDeclManagerLocal::FindType

         External users will always cause the decl to be parsed before returning
         =================
         */static int DEBUG_FindType=0;
        @Override 
        public idDecl FindType(declType_t type, idStr name, boolean makeDefault) throws idException {
            idDeclLocal decl;

//            TempDump.printCallStack("--------------"+ DEBUG_FindType);
            DEBUG_FindType++;
            if (name.IsEmpty()) {
                name.oSet("_emptyName");
                //common.Warning( "idDeclManager::FindType: empty %s name", GetDeclType( (int)type ).typeName.c_str() );
            }

            decl = FindTypeWithoutParsing(type, name.toString(), makeDefault);
            if (null == decl) {
                return null;
            }

            decl.AllocateSelf();

            // if it hasn't been parsed yet, parse it now
            if (decl.declState == DS_UNPARSED) {
                decl.ParseLocal();
            }

            // mark it as referenced
            decl.referencedThisLevel = true;
            decl.everReferenced = true;
            if (insideLevelLoad) {
                decl.parsedOutsideLevelLoad = false;
            }

            return decl.self;
        }

        @Override
        public idDecl FindDeclWithoutParsing(declType_t type, String name, boolean makeDefault) throws idException {
            idDeclLocal decl;
            decl = FindTypeWithoutParsing(type, name, makeDefault);
            if (decl != null) {
                return decl.self;
            }
            return null;
        }

        @Override
        public idDecl FindDeclWithoutParsing(declType_t type, String name) throws idException {
            return FindDeclWithoutParsing(type, name, true);
        }

        @Override
        public void ReloadFile(String filename, boolean force) throws idException {
            for (int i = 0; i < loadedFiles.Num(); i++) {
                if (0 == loadedFiles.oGet(i).fileName.Icmp(filename)) {
                    checksum = checksum.xor(loadedFiles.oGet(i).checksum);
                    loadedFiles.oGet(i).Reload(force);
                    checksum = checksum.xor(loadedFiles.oGet(i).checksum);
                }
            }
        }

        @Override
        public int GetNumDecls(int typeIndex) throws idException {
//            int typeIndex = typeIndex;

            if (typeIndex < 0 || typeIndex >= declTypes.Num() || declTypes.oGet(typeIndex) == null) {
                common.FatalError("idDeclManager::GetNumDecls: bad type: %d", typeIndex);
            }
            return linearLists[typeIndex].Num();
        }

        @Override
        public idDecl DeclByIndex(declType_t type, int index, boolean forceParse) throws idException {
            int typeIndex = type.ordinal();

            if (typeIndex < 0 || typeIndex >= declTypes.Num() || declTypes.oGet(typeIndex) == null) {
                common.FatalError("idDeclManager::DeclByIndex: bad type: %d", typeIndex);
            }
            if (index < 0 || index >= linearLists[typeIndex].Num()) {
                common.Error("idDeclManager::DeclByIndex: out of range");
            }
            idDeclLocal decl = linearLists[typeIndex].oGet(index);

            decl.AllocateSelf();

            if (forceParse && decl.declState == DS_UNPARSED) {
                decl.ParseLocal();
            }

            return decl.self;
        }

        @Override
        public idDecl DeclByIndex(declType_t type, int index) throws idException {
            return DeclByIndex(type, index, true);
        }

        /*
         ===================
         idDeclManagerLocal::ListType

         list*
         Lists decls currently referenced

         list* ever
         Lists decls that have been referenced at least once since app launched

         list* all
         Lists every decl declared, even if it hasn't been referenced or parsed

         FIXME: alphabetized, wildcards?
         ===================
         */
        @Override
        public void ListType(idCmdArgs args, declType_t type) throws idException {
            boolean all, ever;

            if (0 == idStr.Icmp(args.Argv(1), "all")) {
                all = true;
            } else {
                all = false;
            }
            if (0 == idStr.Icmp(args.Argv(1), "ever")) {
                ever = true;
            } else {
                ever = false;
            }

            common.Printf("--------------------\n");
            int printed = 0;
            int count = linearLists[type.ordinal()].Num();
            for (int i = 0; i < count; i++) {
                idDeclLocal decl = linearLists[type.ordinal()].oGet(i);

                if (!all && decl.declState == DS_UNPARSED) {
                    continue;
                }

                if (!all && !ever && !decl.referencedThisLevel) {
                    continue;
                }

                if (decl.referencedThisLevel) {
                    common.Printf("*");
                } else if (decl.everReferenced) {
                    common.Printf(".");
                } else {
                    common.Printf(" ");
                }
                if (decl.declState == DS_DEFAULTED) {
                    common.Printf("D");
                } else {
                    common.Printf(" ");
                }
                common.Printf("%4i: ", decl.index);
                printed++;
                if (decl.declState == DS_UNPARSED) {
                    // doesn't have any type specific data yet
                    common.Printf("%s\n", decl.GetName());
                } else {
                    decl.self.List();
                }
            }

            common.Printf("--------------------\n");
            common.Printf("%d of %d %s\n", printed, count, declTypes.oGet(type.ordinal()).typeName.toString());
        }

        @Override
        public void PrintType(idCmdArgs args, declType_t type) throws idException {
            // individual decl types may use additional command parameters
            if (args.Argc() < 2) {
                common.Printf("USAGE: Print<decl type> <decl name> [type specific parms]\n");
                return;
            }

            // look it up, skipping the public path so it won't parse or reference
            idDeclLocal decl = FindTypeWithoutParsing(type, args.Argv(1), false);
            if (null == decl) {
                common.Printf("%s '%s' not found.\n", declTypes.oGet(type.ordinal()).typeName.toString(), args.Argv(1));
                return;
            }

            // print information common to all decls
            common.Printf("%s %s:\n", declTypes.oGet(type.ordinal()).typeName.toString(), decl.name.toString());
            common.Printf("source: %s:%d\n", decl.sourceFile.fileName.toString(), decl.sourceLine);
            common.Printf("----------\n");
            if (decl.textSource != null) {
                String[] declText = new String[1];//[decl.textLength + 1 ];
                decl.GetText(declText);
                common.Printf("%s\n", declText[0]);
            } else {
                common.Printf("NO SOURCE\n");
            }
            common.Printf("----------\n");
            switch (decl.declState) {
                case DS_UNPARSED:
                    common.Printf("Unparsed.\n");
                    break;
                case DS_DEFAULTED:
                    common.Printf("<DEFAULTED>\n");
                    break;
                case DS_PARSED:
                    common.Printf("Parsed.\n");
                    break;
            }

            if (decl.referencedThisLevel) {
                common.Printf("Currently referenced this level.\n");
            } else if (decl.everReferenced) {
                common.Printf("Referenced in a previous level.\n");
            } else {
                common.Printf("Never referenced.\n");
            }

            // allow type-specific data to be printed
            if (decl.self != null) {
                decl.self.Print();
            }
        }

        @Override
        public idDecl CreateNewDecl(declType_t type, String name, String _fileName) throws idException {
            int typeIndex = type.ordinal();
            int i, hash;

            if (typeIndex < 0 || typeIndex >= declTypes.Num() || declTypes.oGet(typeIndex) == null) {
                common.FatalError("idDeclManager::CreateNewDecl: bad type: %d", typeIndex);
            }

            char[] canonicalName = new char[MAX_STRING_CHARS];

            MakeNameCanonical(name, canonicalName, MAX_STRING_CHARS);

            idStr fileName = new idStr(_fileName);
            fileName.BackSlashesToSlashes();

            // see if it already exists
            hash = hashTables[typeIndex].GenerateKey(canonicalName, false);
            for (i = hashTables[typeIndex].First(hash); i >= 0; i = hashTables[typeIndex].Next(i)) {
                if (linearLists[typeIndex].oGet(i).name.Icmp(ctos(canonicalName)) == 0) {
                    linearLists[typeIndex].oGet(i).AllocateSelf();
                    return linearLists[typeIndex].oGet(i).self;
                }
            }

            idDeclFile sourceFile;

            // find existing source file or create a new one
            for (i = 0; i < loadedFiles.Num(); i++) {
                if (loadedFiles.oGet(i).fileName.Icmp(fileName.toString()) == 0) {
                    break;
                }
            }
            if (i < loadedFiles.Num()) {
                sourceFile = loadedFiles.oGet(i);
            } else {
                sourceFile = new idDeclFile(fileName.toString(), type);
                loadedFiles.Append(sourceFile);
            }

            idDeclLocal decl = new idDeclLocal();
            decl.name = new idStr(ctos(canonicalName));
            decl.type = type;
            decl.declState = DS_UNPARSED;
            decl.AllocateSelf();
            idStr header = declTypes.oGet(typeIndex).typeName;
            idStr defaultText = new idStr(decl.self.DefaultDefinition());

            int size = header.Length() + 1 + idStr.Length(canonicalName) + 1 + defaultText.Length();
            char[] declText = new char[size + 1];

//	memcpy( declText, header, header.Length() );
            System.arraycopy(header.c_str(), 0, declText, 0, header.Length());
            declText[header.Length()] = ' ';
//	memcpy( declText + header.Length() + 1, canonicalName, idStr::Length( canonicalName ) );
            System.arraycopy(canonicalName, 0, declText, header.Length() + 1, idStr.Length(canonicalName));
            declText[header.Length() + 1 + idStr.Length(canonicalName)] = ' ';
//	memcpy( declText + header.Length() + 1 + idStr::Length( canonicalName ) + 1, defaultText, defaultText.Length() + 1 );
            System.arraycopy(defaultText.c_str(), 0, declText, header.Length() + 1 + idStr.Length(canonicalName) + 1, defaultText.Length() + 1);

            final String declString = ctos(declText);
            decl.SetTextLocal(declString, declString.length());
            decl.sourceFile = sourceFile;
            decl.sourceTextOffset = sourceFile.fileSize;
            decl.sourceTextLength = 0;
            decl.sourceLine = sourceFile.numLines;

            decl.ParseLocal();

            // add this decl to the source file list
            decl.nextInFile = sourceFile.decls;
            sourceFile.decls = decl;

            // add it to the hash table and linear list
            decl.index = linearLists[typeIndex].Num();
            hashTables[typeIndex].Add(hash, linearLists[typeIndex].Append(decl));

            return decl.self;
        }

        //BSM Added for the material editors rename capabilities
        @Override
        public boolean RenameDecl(declType_t type, String oldName, String newName) {

            char[] canonicalOldName = new char[MAX_STRING_CHARS];
            MakeNameCanonical(oldName, canonicalOldName, MAX_STRING_CHARS);

            char[] canonicalNewName = new char[MAX_STRING_CHARS];
            MakeNameCanonical(newName, canonicalNewName, MAX_STRING_CHARS);

            idDeclLocal decl = null;

            // make sure it already exists
            int typeIndex = type.ordinal();
            int i, hash;
            hash = hashTables[typeIndex].GenerateKey(canonicalOldName, false);
            for (i = hashTables[typeIndex].First(hash); i >= 0; i = hashTables[typeIndex].Next(i)) {
                if (linearLists[typeIndex].oGet(i).name.Icmp(ctos(canonicalOldName)) == 0) {
                    decl = linearLists[typeIndex].oGet(i);
                    break;
                }
            }
            if (null == decl) {
                return false;
            }

            //if ( !hashTables[(int)type].Get( canonicalOldName, &declPtr ) )
            //	return false;
            //decl = *declPtr;
            //Change the name
            decl.name = new idStr(ctos(canonicalNewName));

            // add it to the hash table
            //hashTables[(int)decl.type].Set( decl.name, decl );
            int newhash = hashTables[typeIndex].GenerateKey(ctos(canonicalNewName), false);
            hashTables[typeIndex].Add(newhash, decl.index);

            //Remove the old hash item
            hashTables[typeIndex].Remove(hash, decl.index);

            return true;
        }


        /*
         ===================
         idDeclManagerLocal::MediaPrint

         This is just used to nicely indent media caching prints
         ===================
         */
        @Override
        public void MediaPrint(String fmt, Object... arg) throws idException {
            if (0 == decl_show.GetInteger()) {
                return;
            }
            for (int i = 0; i < indent; i++) {
                common.Printf("    ");
            }
//	va_list		argptr;
            String[] buffer = {null};//new char[1024];
//	va_start (argptr,fmt);
            idStr.vsnPrintf(buffer, 1024, fmt, arg);
//	va_end (argptr);
//            buffer[1024 - 1] = '\0';

            common.Printf("%s", buffer[0]);
        }

        @Override
        public void WritePrecacheCommands(idFile f) {
            for (int i = 0; i < declTypes.Num(); i++) {
                int num;

                if (declTypes.oGet(i) == null) {
                    continue;
                }

                num = linearLists[i].Num();

                for (int j = 0; j < num; j++) {
                    idDeclLocal decl = linearLists[i].oGet(j);

                    if (!decl.referencedThisLevel) {
                        continue;
                    }

                    String str;//[1024];
                    str = String.format("touch %s %s\n", declTypes.oGet(i).typeName.toString(), decl.GetName());
                    common.Printf("%s", str);
                    f.Printf("%s", str);
                }
            }
        }

        /* *******************************************************************/
        @Override
        public idMaterial FindMaterial(idStr name, boolean makeDefault) throws idException {
            return (idMaterial) FindType(DECL_MATERIAL, name, makeDefault);
        }

        @Override
        public idMaterial MaterialByIndex(int index, boolean forceParse) throws idException {
            return (idMaterial) DeclByIndex(DECL_MATERIAL, index, forceParse);
        }

        @Override
        public idMaterial MaterialByIndex(int index) throws idException {
            return MaterialByIndex(index, true);
        }

        /* *******************************************************************/
        @Override
        public idDeclSkin FindSkin(idStr name, boolean makeDefault) throws idException {
            return (idDeclSkin) FindType(DECL_SKIN, name, makeDefault);
        }

        @Override
        public idDeclSkin SkinByIndex(int index, boolean forceParse) throws idException {
            return (idDeclSkin) DeclByIndex(DECL_SKIN, index, forceParse);
        }

        @Override
        public idDeclSkin SkinByIndex(int index) throws idException {
            return SkinByIndex(index, true);
        }

        /* *******************************************************************/
        @Override
        public idSoundShader FindSound(idStr name, boolean makeDefault) throws idException {
            return (idSoundShader) FindType(DECL_SOUND, name, makeDefault);
        }

        @Override
        public idSoundShader SoundByIndex(int index, boolean forceParse) throws idException {
            return (idSoundShader) DeclByIndex(DECL_SOUND, index, forceParse);
        }

        @Override
        public idSoundShader SoundByIndex(int index) throws idException {
            return SoundByIndex(index, true);
        }
        /* *******************************************************************/
        //

        public static void MakeNameCanonical(final String name, char[] result, int maxLength) {//TODO:maxlength???
            int i, lastDot;

            lastDot = -1;
            for (i = 0; i < maxLength && i < name.length(); i++) {
                int c = name.charAt(i);
                if (c == '\\') {
                    result[i] = '/';
                } else if (c == '.') {
                    lastDot = i;
                    result[i] = (char) c;
                } else {
                    result[i] = idStr.ToLower((char) c);
                }
            }
            if (lastDot != -1) {
                result[lastDot] = '\0';
            } else {
                result[i] = '\0';
            }
        }

        /*
         ===================
         idDeclManagerLocal::FindTypeWithoutParsing

         This finds or creats the decl, but does not cause a parse.  This is only used internally.
         ===================
         */
        public idDeclLocal FindTypeWithoutParsing(declType_t type, final String name, boolean makeDefault/*= true*/) throws idException {
            final int typeIndex = type.ordinal();
            int i, hash;

            if (typeIndex < 0 || typeIndex >= declTypes.Num() || declTypes.oGet(typeIndex) == null) {
                common.FatalError("idDeclManager.FindTypeWithoutParsing: bad type: %d", typeIndex);
            }

            char[] canonicalName = new char[MAX_STRING_CHARS];

            MakeNameCanonical(name, canonicalName, MAX_STRING_CHARS);

            // see if it already exists
            hash = hashTables[typeIndex].GenerateKey(canonicalName, false);
            for (i = hashTables[typeIndex].First(hash); i >= 0; i = hashTables[typeIndex].Next(i)) {
                if (linearLists[typeIndex].oGet(i).name.Icmp(ctos(canonicalName)) == 0) {
                    // only print these when decl_show is set to 2, because it can be a lot of clutter
                    if (decl_show.GetInteger() > 1) {
                        MediaPrint("referencing %s %s\n", declTypes.oGet(type.ordinal()).typeName.toString(), name);
                    }
                    return linearLists[typeIndex].oGet(i);
                }
            }

            if (!makeDefault) {
                return null;
            }

            idDeclLocal decl = new idDeclLocal();
            decl.self = null;
            decl.name = new idStr(ctos(canonicalName));
            decl.type = type;
            decl.declState = DS_UNPARSED;
            decl.textSource = null;
            decl.textLength = 0;
            decl.sourceFile = implicitDecls;
            decl.referencedThisLevel = false;
            decl.everReferenced = false;
            decl.parsedOutsideLevelLoad = !insideLevelLoad;

            // add it to the linear list and hash table
            decl.index = linearLists[typeIndex].Num();
            hashTables[typeIndex].Add(hash, linearLists[typeIndex].Append(decl));

            return decl;
        }

        public idDeclType GetDeclType(int type) {
            return declTypes.oGet(type);
        }

        public idDeclFile GetImplicitDeclFile() {
            return implicitDecls;
        }


        /*
         ================
         idDeclManagerLocal.ListDecls_f
         ================
         */
        static class ListDecls_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListDecls_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i, j;
                int totalDecls = 0;
                int totalText = 0;
                int totalStructs = 0;

                for (i = 0; i < declManagerLocal.declTypes.Num(); i++) {
                    int size, num;

                    if (declManagerLocal.declTypes.oGet(i) == null) {
                        continue;
                    }

                    num = declManagerLocal.linearLists[i].Num();
                    totalDecls += num;

                    size = 0;
                    for (j = 0; j < num; j++) {
                        size += declManagerLocal.linearLists[i].oGet(j).Size();
                        if (declManagerLocal.linearLists[i].oGet(j).self != null) {
                            size += declManagerLocal.linearLists[i].oGet(j).self.Size();
                        }
                    }
                    totalStructs += size;

                    common.Printf("%4ik %4i %s\n", size >> 10, num, declManagerLocal.declTypes.oGet(i).typeName.toString());
                }

                for (i = 0; i < declManagerLocal.loadedFiles.Num(); i++) {
                    idDeclFile df = declManagerLocal.loadedFiles.oGet(i);
                    totalText += df.fileSize;
                }

                common.Printf("%d total decls is %d decl files\n", totalDecls, declManagerLocal.loadedFiles.Num());
                common.Printf("%dKB in text, %dKB in structures\n", totalText >> 10, totalStructs >> 10);
            }
        };

        /*
         ===================
         idDeclManagerLocal.ReloadDecls_f

         Reload will not find any new files created in the directories, it
         will only reload existing files.

         A reload will never cause anything to be purged.
         ===================
         */
        static class ReloadDecls_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ReloadDecls_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                boolean force;

                if (0 == idStr.Icmp(args.Argv(1), "all")) {
                    force = true;
                    common.Printf("reloading all decl files:\n");
                } else {
                    force = false;
                    common.Printf("reloading changed decl files:\n");
                }

                soundSystem.SetMute(true);

                declManagerLocal.Reload(force);

                soundSystem.SetMute(false);
            }
        };

        /*
         ===================
         idDeclManagerLocal.TouchDecl_f
         ===================
         */
        static class TouchDecl_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TouchDecl_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i;

                if (args.Argc() != 3) {
                    common.Printf("usage: touch <type> <name>\n");
                    common.Printf("valid types: ");
                    for (i = 0; i < declManagerLocal.declTypes.Num(); i++) {
                        if (declManagerLocal.declTypes.oGet(i) != null) {
                            common.Printf("%s ", declManagerLocal.declTypes.oGet(i).typeName.toString());
                        }
                    }
                    common.Printf("\n");
                    return;
                }

                for (i = 0; i < declManagerLocal.declTypes.Num(); i++) {
                    if (declManagerLocal.declTypes.oGet(i) != null && declManagerLocal.declTypes.oGet(i).typeName.Icmp(args.Argv(1)) == 0) {
                        break;
                    }
                }
                if (i >= declManagerLocal.declTypes.Num()) {
                    common.Printf("unknown decl type '%s'\n", args.Argv(1));
                    return;
                }

                final declType_t[] values = declType_t.values();
                if (i < values.length) {
                    final idDecl decl = declManagerLocal.FindType(values[i], new idStr(args.Argv(2)), false);
                    if (null == decl) {
                        common.Printf("%s '%s' not found\n", declManagerLocal.declTypes.oGet(i).typeName.toString(), args.Argv(2));
                    }
                }
            }
        };
    };
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    

//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
    /*
     ====================================================================================

     decl text huffman compression

     ====================================================================================
     */
    static final int MAX_HUFFMAN_SYMBOLS = 256;

    static class huffmanNode_s {

        int symbol;
        int frequency;
        huffmanNode_s next;
        huffmanNode_s[] children = new huffmanNode_s[2];
    };

    static class huffmanCode_s {

        long[] bits = new long[8];
        int numBits;

        private huffmanCode_s() {
        }

        public huffmanCode_s(huffmanCode_s code) {
            this.numBits = code.numBits;
            this.bits[0] = code.bits[0];
            this.bits[1] = code.bits[1];
            this.bits[2] = code.bits[2];
            this.bits[3] = code.bits[3];
            this.bits[4] = code.bits[4];
            this.bits[5] = code.bits[5];
            this.bits[6] = code.bits[6];
            this.bits[7] = code.bits[7];
        }

    };

    // compression ratio = 64%
    static final int huffmanFrequencies[] = {
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00078fb6, 0x000352a7, 0x00000002, 0x00000001, 0x0002795e, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00049600, 0x000000dd, 0x00018732, 0x0000005a, 0x00000007, 0x00000092, 0x0000000a, 0x00000919,
        0x00002dcf, 0x00002dda, 0x00004dfc, 0x0000039a, 0x000058be, 0x00002d13, 0x00014d8c, 0x00023c60,
        0x0002ddb0, 0x0000d1fc, 0x000078c4, 0x00003ec7, 0x00003113, 0x00006b59, 0x00002499, 0x0000184a,
        0x0000250b, 0x00004e38, 0x000001ca, 0x00000011, 0x00000020, 0x000023da, 0x00000012, 0x00000091,
        0x0000000b, 0x00000b14, 0x0000035d, 0x0000137e, 0x000020c9, 0x00000e11, 0x000004b4, 0x00000737,
        0x000006b8, 0x00001110, 0x000006b3, 0x000000fe, 0x00000f02, 0x00000d73, 0x000005f6, 0x00000be4,
        0x00000d86, 0x0000014d, 0x00000d89, 0x0000129b, 0x00000db3, 0x0000015a, 0x00000167, 0x00000375,
        0x00000028, 0x00000112, 0x00000018, 0x00000678, 0x0000081a, 0x00000677, 0x00000003, 0x00018112,
        0x00000001, 0x000441ee, 0x000124b0, 0x0001fa3f, 0x00026125, 0x0005a411, 0x0000e50f, 0x00011820,
        0x00010f13, 0x0002e723, 0x00003518, 0x00005738, 0x0002cc26, 0x0002a9b7, 0x0002db81, 0x0003b5fa,
        0x000185d2, 0x00001299, 0x00030773, 0x0003920d, 0x000411cd, 0x00018751, 0x00005fbd, 0x000099b0,
        0x00009242, 0x00007cf2, 0x00002809, 0x00005a1d, 0x00000001, 0x00005a1d, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,
        0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001, 0x00000001,};
    static huffmanCode_s[] huffmanCodes = new huffmanCode_s[MAX_HUFFMAN_SYMBOLS];
    static huffmanNode_s huffmanTree = null;
    static int totalUncompressedLength = 0;
    static int totalCompressedLength = 0;
    static int maxHuffmanBits = 0;

    /*
     ================
     ClearHuffmanFrequencies
     ================
     */
    static void ClearHuffmanFrequencies() {
        int i;

        for (i = 0; i < MAX_HUFFMAN_SYMBOLS; i++) {
            huffmanFrequencies[i] = 1;
        }
    }

    /*
     ================
     InsertHuffmanNode
     ================
     */
    static huffmanNode_s InsertHuffmanNode(huffmanNode_s firstNode, huffmanNode_s node) {
        huffmanNode_s n, lastNode;

        lastNode = null;
        for (n = firstNode; n != null; n = n.next) {
            if (node.frequency <= n.frequency) {
                break;
            }
            lastNode = n;
        }
        if (lastNode != null) {
            node.next = lastNode.next;
            lastNode.next = node;
        } else {
            node.next = firstNode;
            firstNode = node;
        }
        return firstNode;
    }

    /*
     ================
     BuildHuffmanCode_r
     ================
     */
    static void BuildHuffmanCode_r(huffmanNode_s node, final huffmanCode_s code, huffmanCode_s[] codes/*[MAX_HUFFMAN_SYMBOLS]*/) {
        if (node.symbol == -1) {
            huffmanCode_s newCode = new huffmanCode_s(code);
            assert (code.numBits < codes[0].bits.length * 8);
            newCode.numBits++;
            if (code.numBits > maxHuffmanBits) {
                maxHuffmanBits = newCode.numBits;
            }
            BuildHuffmanCode_r(node.children[0], newCode, codes);
            newCode.bits[code.numBits >> 5] |= 1 << (code.numBits & 31);
            BuildHuffmanCode_r(node.children[1], newCode, codes);
        } else {
            assert (code.numBits <= codes[0].bits.length * 8);
            codes[node.symbol] = new huffmanCode_s(code);
        }
    }

    /*
     ================
     FreeHuffmanTree_r
     ================
     */
    static void FreeHuffmanTree_r(huffmanNode_s node) {
        if (node.symbol == -1) {
            FreeHuffmanTree_r(node.children[0]);
            FreeHuffmanTree_r(node.children[1]);
        }
//	delete node;
    }

    /*
     ================
     HuffmanHeight_r
     ================
     */
    static int HuffmanHeight_r(huffmanNode_s node) {
        if (node == null) {
            return -1;
        }
        int left = HuffmanHeight_r(node.children[0]);
        int right = HuffmanHeight_r(node.children[1]);
        if (left > right) {
            return left + 1;
        }
        return right + 1;
    }

    /*
     ================
     SetupHuffman
     ================
     */
    static void SetupHuffman() {
        int i, height;
        huffmanNode_s firstNode, node;
        huffmanCode_s code;

        firstNode = null;
        for (i = 0; i < MAX_HUFFMAN_SYMBOLS; i++) {
            node = new huffmanNode_s();
            node.symbol = i;
            node.frequency = huffmanFrequencies[i];
            node.next = null;
            node.children[0] = null;
            node.children[1] = null;
            firstNode = InsertHuffmanNode(firstNode, node);
        }

        for (i = 1; i < MAX_HUFFMAN_SYMBOLS; i++) {
            node = new huffmanNode_s();
            node.symbol = -1;
            node.frequency = firstNode.frequency + firstNode.next.frequency;
            node.next = null;
            node.children[0] = firstNode;
            node.children[1] = firstNode.next;
            firstNode = InsertHuffmanNode(firstNode.next.next, node);
        }

        maxHuffmanBits = 0;
        code = new huffmanCode_s();//memset( &code, 0, sizeof( code ) );
        BuildHuffmanCode_r(firstNode, code, huffmanCodes);

        huffmanTree = firstNode;

        height = HuffmanHeight_r(firstNode);
        assert (maxHuffmanBits == height);
    }

    /*
     ================
     ShutdownHuffman
     ================
     */
    static void ShutdownHuffman() {
        if (huffmanTree != null) {
            FreeHuffmanTree_r(huffmanTree);
        }
    }

    /*
     ================
     HuffmanCompressText
     ================
     */
    private static int HuffmanCompressText(final String text, int textLength, ByteBuffer compressed, int maxCompressedSize) {
        int i, j;
        idBitMsg msg = new idBitMsg();

        totalUncompressedLength += textLength;

        msg.Init(compressed, maxCompressedSize);
        msg.BeginWriting();
        for (i = 0; i < textLength; i++) {
            final huffmanCode_s code = huffmanCodes[text.charAt(i)];
            for (j = 0; j < (code.numBits >> 5); j++) {
                msg.WriteBits((int) code.bits[j], 32);
            }
            if ((code.numBits & 31) != 0) {
                msg.WriteBits((int) code.bits[j], code.numBits & 31);
            }
        }

        totalCompressedLength += msg.GetSize();

        return msg.GetSize();
    }

    /*
     ================
     HuffmanDecompressText
     ================
     */
    static int HuffmanDecompressText(String[] text, int textLength, final ByteBuffer compressed, int compressedSize) {
        int i, bit;
        idBitMsg msg = new idBitMsg();
        huffmanNode_s node;

        msg.Init(compressed, compressedSize);
        msg.SetSize(compressedSize);
        msg.BeginReading();
        text[0] = "";
        for (i = 0; i < textLength; i++) {
            node = huffmanTree;
            do {
                bit = msg.ReadBits(1);
                node = node.children[bit];
//                System.out.println(bit + ":" + node.symbol);
            } while (node.symbol == -1);
            text[0] += (char) node.symbol;
        }
//        text[0] += '\0';
        return msg.GetReadCount();
    }

    /*
     ================
     ListHuffmanFrequencies_f
     ================
     */
    static class ListHuffmanFrequencies_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new ListHuffmanFrequencies_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i;
            float compression;
            compression = (0 == totalUncompressedLength ? 100 : 100 * totalCompressedLength / totalUncompressedLength);
            common.Printf("// compression ratio = %d%%\n", (int) compression);
            common.Printf("static int huffmanFrequencies[] = {\n");
            for (i = 0; i < MAX_HUFFMAN_SYMBOLS; i += 8) {
                common.Printf("\t0x%08x, 0x%08x, 0x%08x, 0x%08x, 0x%08x, 0x%08x, 0x%08x, 0x%08x,\n",
                        huffmanFrequencies[i + 0], huffmanFrequencies[i + 1],
                        huffmanFrequencies[i + 2], huffmanFrequencies[i + 3],
                        huffmanFrequencies[i + 4], huffmanFrequencies[i + 5],
                        huffmanFrequencies[i + 6], huffmanFrequencies[i + 7]);
            }
            common.Printf("}\n");
        }
    };

    public static void setDeclManager(idDeclManager declManager) {
        DeclManager.declManager = DeclManager.declManagerLocal = (idDeclManagerLocal) declManager;
    }
}
