package neo.idlib.Text;

import static neo.TempDump.NOT;
import static neo.TempDump.atocb;
import static neo.TempDump.bbtocb;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_BINARY;
import static neo.idlib.Text.Token.TT_DECIMAL;
import static neo.idlib.Text.Token.TT_DOUBLE_PRECISION;
import static neo.idlib.Text.Token.TT_EXTENDED_PRECISION;
import static neo.idlib.Text.Token.TT_FLOAT;
import static neo.idlib.Text.Token.TT_HEX;
import static neo.idlib.Text.Token.TT_INDEFINITE;
import static neo.idlib.Text.Token.TT_INFINITE;
import static neo.idlib.Text.Token.TT_INTEGER;
import static neo.idlib.Text.Token.TT_IPADDRESS;
import static neo.idlib.Text.Token.TT_IPPORT;
import static neo.idlib.Text.Token.TT_LITERAL;
import static neo.idlib.Text.Token.TT_LONG;
import static neo.idlib.Text.Token.TT_NAME;
import static neo.idlib.Text.Token.TT_NAN;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.Text.Token.TT_OCTAL;
import static neo.idlib.Text.Token.TT_PUNCTUATION;
import static neo.idlib.Text.Token.TT_SINGLE_PRECISION;
import static neo.idlib.Text.Token.TT_STRING;
import static neo.idlib.Text.Token.TT_UNSIGNED;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import neo.framework.File_h.idFile;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Vector.idVec;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Lexer {

    static final boolean PUNCTABLE = true;

    /**
     * ===============================================================================
     *
     * Lexicographical parser
     *
     * Does not use memory allocation during parsing. The lexer uses no memory
     * allocation if a source is loaded with LoadMemory(). However, idToken may
     * still allocate memory for large strings.
     *
     * A number directly following the escape character '\' in a string is
     * assumed to be in decimal format instead of octal. Binary numbers of the
     * form 0b.. or 0B.. can also be used.
     *
     * ===============================================================================
     */
    // lexer flags
    public static final int             LEXFL_NOERRORS                   = BIT(0);    // don't print any errors
    public static final int             LEXFL_NOWARNINGS                 = BIT(1);    // don't print any warnings
    public static final int             LEXFL_NOFATALERRORS              = BIT(2);    // errors aren't fatal
    public static final int             LEXFL_NOSTRINGCONCAT             = BIT(3);    // multiple strings seperated by whitespaces are not concatenated
    public static final int             LEXFL_NOSTRINGESCAPECHARS        = BIT(4);    // no escape characters inside strings
    public static final int             LEXFL_NODOLLARPRECOMPILE         = BIT(5);    // don't use the $ sign for precompilation
    public static final int             LEXFL_NOBASEINCLUDES             = BIT(6);    // don't include files embraced with < >
    public static final int             LEXFL_ALLOWPATHNAMES             = BIT(7);    // allow path seperators in names
    public static final int             LEXFL_ALLOWNUMBERNAMES           = BIT(8);    // allow names to start with a number
    public static final int             LEXFL_ALLOWIPADDRESSES           = BIT(9);    // allow ip addresses to be parsed as numbers
    public static final int             LEXFL_ALLOWFLOATEXCEPTIONS       = BIT(10);   // allow float exceptions like 1.#INF or 1.#IND to be parsed
    public static final int             LEXFL_ALLOWMULTICHARLITERALS     = BIT(11);   // allow multi character literals
    public static final int             LEXFL_ALLOWBACKSLASHSTRINGCONCAT = BIT(12);   // allow multiple strings seperated by '\' to be concatenated
    public static final int             LEXFL_ONLYSTRINGS                = BIT(13);   // parse as whitespace deliminated strings (quoted strings keep quotes)
    //
//    
    // punctuation ids
    static final        int             P_RSHIFT_ASSIGN                  = 1;
    static final        int             P_LSHIFT_ASSIGN                  = 2;
    static final        int             P_PARMS                          = 3;
    static final        int             P_PRECOMPMERGE                   = 4;
    static final        int             P_LOGIC_AND                      = 5;
    static final        int             P_LOGIC_OR                       = 6;
    static final        int             P_LOGIC_GEQ                      = 7;
    static final        int             P_LOGIC_LEQ                      = 8;
    static final        int             P_LOGIC_EQ                       = 9;
    static final        int             P_LOGIC_UNEQ                     = 10;
    static final        int             P_MUL_ASSIGN                     = 11;
    static final        int             P_DIV_ASSIGN                     = 12;
    static final        int             P_MOD_ASSIGN                     = 13;
    static final        int             P_ADD_ASSIGN                     = 14;
    static final        int             P_SUB_ASSIGN                     = 15;
    static final        int             P_INC                            = 16;
    static final        int             P_DEC                            = 17;
    static final        int             P_BIN_AND_ASSIGN                 = 18;
    static final        int             P_BIN_OR_ASSIGN                  = 19;
    static final        int             P_BIN_XOR_ASSIGN                 = 20;
    static final        int             P_RSHIFT                         = 21;
    static final        int             P_LSHIFT                         = 22;
    static final        int             P_POINTERREF                     = 23;
    static final        int             P_CPP1                           = 24;
    static final        int             P_CPP2                           = 25;
    static final        int             P_MUL                            = 26;
    static final        int             P_DIV                            = 27;
    static final        int             P_MOD                            = 28;
    static final        int             P_ADD                            = 29;
    static final        int             P_SUB                            = 30;
    static final        int             P_ASSIGN                         = 31;
    static final        int             P_BIN_AND                        = 32;
    static final        int             P_BIN_OR                         = 33;
    static final        int             P_BIN_XOR                        = 34;
    static final        int             P_BIN_NOT                        = 35;
    static final        int             P_LOGIC_NOT                      = 36;
    static final        int             P_LOGIC_GREATER                  = 37;
    static final        int             P_LOGIC_LESS                     = 38;
    static final        int             P_REF                            = 39;
    static final        int             P_COMMA                          = 40;
    static final        int             P_SEMICOLON                      = 41;
    static final        int             P_COLON                          = 42;
    static final        int             P_QUESTIONMARK                   = 43;
    static final        int             P_PARENTHESESOPEN                = 44;
    static final        int             P_PARENTHESESCLOSE               = 45;
    static final        int             P_BRACEOPEN                      = 46;
    static final        int             P_BRACECLOSE                     = 47;
    static final        int             P_SQBRACKETOPEN                  = 48;
    static final        int             P_SQBRACKETCLOSE                 = 49;
    static final        int             P_BACKSLASH                      = 50;
    public static final int             P_PRECOMP                        = 51;
    static final        int             P_DOLLAR                         = 52;
    //
//  
    //longer punctuations first
    static final punctuation_t[] default_punctuations = {
        //binary operators
        new punctuation_t(">>=", P_RSHIFT_ASSIGN),
        new punctuation_t("<<=", P_LSHIFT_ASSIGN),
        //
        new punctuation_t("...", P_PARMS),
        //define merge operator
        new punctuation_t("##", P_PRECOMPMERGE), // pre-compiler
        //logic operators
        new punctuation_t("&&", P_LOGIC_AND), // pre-compiler
        new punctuation_t("||", P_LOGIC_OR), // pre-compiler
        new punctuation_t(">=", P_LOGIC_GEQ), // pre-compiler
        new punctuation_t("<=", P_LOGIC_LEQ), // pre-compiler
        new punctuation_t("==", P_LOGIC_EQ), // pre-compiler
        new punctuation_t("!=", P_LOGIC_UNEQ), // pre-compiler
        //arithmatic operators
        new punctuation_t("*=", P_MUL_ASSIGN),
        new punctuation_t("/=", P_DIV_ASSIGN),
        new punctuation_t("%=", P_MOD_ASSIGN),
        new punctuation_t("+=", P_ADD_ASSIGN),
        new punctuation_t("-=", P_SUB_ASSIGN),
        new punctuation_t("++", P_INC),
        new punctuation_t("--", P_DEC),
        //binary operators
        new punctuation_t("&=", P_BIN_AND_ASSIGN),
        new punctuation_t("|=", P_BIN_OR_ASSIGN),
        new punctuation_t("^=", P_BIN_XOR_ASSIGN),
        new punctuation_t(">>", P_RSHIFT), // pre-compiler
        new punctuation_t("<<", P_LSHIFT), // pre-compiler
        //reference operators
        new punctuation_t("->", P_POINTERREF),
        //C++
        new punctuation_t("::", P_CPP1),
        new punctuation_t(".*", P_CPP2),
        //arithmatic operators
        new punctuation_t("*", P_MUL), // pre-compiler
        new punctuation_t("/", P_DIV), // pre-compiler
        new punctuation_t("%", P_MOD), // pre-compiler
        new punctuation_t("+", P_ADD), // pre-compiler
        new punctuation_t("-", P_SUB), // pre-compiler
        new punctuation_t("=", P_ASSIGN),
        //binary operators
        new punctuation_t("&", P_BIN_AND), // pre-compiler
        new punctuation_t("|", P_BIN_OR), // pre-compiler
        new punctuation_t("^", P_BIN_XOR), // pre-compiler
        new punctuation_t("~", P_BIN_NOT), // pre-compiler
        //logic operators
        new punctuation_t("!", P_LOGIC_NOT), // pre-compiler
        new punctuation_t(">", P_LOGIC_GREATER), // pre-compiler
        new punctuation_t("<", P_LOGIC_LESS), // pre-compiler
        //reference operator
        new punctuation_t(".", P_REF),
        //seperators
        new punctuation_t(",", P_COMMA), // pre-compiler
        new punctuation_t(";", P_SEMICOLON),
        //label indication
        new punctuation_t(":", P_COLON), // pre-compiler
        //if statement
        new punctuation_t("?", P_QUESTIONMARK), // pre-compiler
        //embracements
        new punctuation_t("(", P_PARENTHESESOPEN), // pre-compiler
        new punctuation_t(")", P_PARENTHESESCLOSE), // pre-compiler
        new punctuation_t("{", P_BRACEOPEN), // pre-compiler
        new punctuation_t("}", P_BRACECLOSE), // pre-compiler
        new punctuation_t("[", P_SQBRACKETOPEN),
        new punctuation_t("]", P_SQBRACKETCLOSE),
        //
        new punctuation_t("\\", P_BACKSLASH),
        //precompiler operator
        new punctuation_t("#", P_PRECOMP), // pre-compiler
        new punctuation_t("$", P_DOLLAR),
        new punctuation_t(null, 0)
    };
    static final        int[]           default_punctuationtable         = new int[256];
    static final        int[]           default_nextpunctuation          = new int[default_punctuations.length];
    static boolean default_setup;

    // punctuation
    static class punctuation_t {

        String p;                 // punctuation character(s)
        int    n;                 // punctuation id

        public punctuation_t(String p, int n) {
            this.p = p;
            this.n = n;
        }
    }

    public static class idLexer {

        private boolean loaded;                                 // set when a script file is loaded from file or memory
        private idStr   filename;                               // file name of the script
        private boolean allocated;                              // true if buffer memory was allocated
        CharBuffer buffer;                                      // buffer containing the script
        int        script_p;                                    // current pointer in the script
        private               int             end_p;            // pointer to the end of the script
        private               int             lastScript_p;     // script pointer before reading token
        private               int             whiteSpaceStart_p;// start of last white space
        private               int             whiteSpaceEnd_p;  // end of last white space
        private /*ID_TIME_T*/ long            fileTime;         // file time
        private               int             length;           // length of the script in bytes
        private               int             line;             // current line in script
        private               int             lastline;         // line before reading token
        private               boolean         tokenAvailable;   // set by unreadToken
        private               long            flags;            // several script flags
        private               punctuation_t[] punctuations;     // the punctuations used in the script
        private               int[]           punctuationTable; // ASCII table with punctuations
        private               int[]           nextPunctuation;  // next punctuation in chain
        private idToken token;			                        // available token
        protected idLexer next;			                        // next script in a chain
        private boolean hadError;		                        // set by idLexer::Error, even if the error is suppressed
        //
        // base folder to load files from
        private static final StringBuilder baseFolder = new StringBuilder(256);
        //
        //

        // constructor
        public idLexer() {
            this.loaded = false;
            this.filename = new idStr("");
            this.flags = 0;
            this.SetPunctuations(null);
            this.allocated = false;
            this.fileTime = 0;
            this.length = 0;
            this.line = 0;
            this.lastline = 0;
            this.tokenAvailable = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
        }

        public idLexer(long flags) {
            this.loaded = false;
            this.filename = new idStr("");
            this.flags = flags;
            this.SetPunctuations(null);
            this.allocated = false;
            this.fileTime = 0;
            this.length = 0;
            this.line = 0;
            this.lastline = 0;
            this.tokenAvailable = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
        }

        public idLexer(final String filename) throws idException {
            this.loaded = false;
            this.flags = 0;
            this.SetPunctuations(null);
            this.allocated = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
            this.LoadFile(filename, false);
        }

        public idLexer(final String filename, int flags) throws idException {
            this.loaded = false;
            this.flags = flags;
            this.SetPunctuations(null);
            this.allocated = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
            this.LoadFile(filename, false);
        }

        public idLexer(final String filename, int flags, boolean OSPath) throws idException {
            this.loaded = false;
            this.flags = flags;
            this.SetPunctuations(null);
            this.allocated = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
            this.LoadFile(filename, OSPath);
        }

        public idLexer(final CharBuffer ptr, int length, final String name) throws idException {
            this.loaded = false;
            this.flags = 0;
            this.SetPunctuations(null);
            this.allocated = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
            this.LoadMemory(ptr, length, name);
        }

        public idLexer(final String ptr, int length, final String name) throws idException {
            this(atocb(ptr), length, name);
        }

        public idLexer(final String ptr, int length, final String name, int flags) throws idException {
            this.loaded = false;
            this.flags = flags;
            this.SetPunctuations(null);
            this.allocated = false;
            this.token = new idToken();
            this.next = null;
            this.hadError = false;
            this.LoadMemory(ptr, length, name);
        }
//					// destructor
//public					~idLexer();

        public boolean LoadFile(final String filename) throws idException {
            return this.LoadFile(filename, false);
        }

        public boolean LoadFile(final idStr filename) throws idException {
            return this.LoadFile(filename.getData());
        }

        // load a script from the given file at the given offset with the given length
        public boolean LoadFile(final String filename, boolean OSPath /*= false*/) throws idException {
//        TODO:NIO
            idFile fp;
            String pathname;
            int length;
            ByteBuffer buf;

            if (this.loaded) {
                idLib.common.Error("this.LoadFile: another script already loaded");
                return false;
            }

            if (!OSPath && (baseFolder.length() > 0 && baseFolder.charAt(0) != '\0')) {//TODO: use length isntead?
                pathname = va("%s/%s", baseFolder, filename);
            } else {
                pathname = filename;
            }
            if (OSPath) {
                fp = idLib.fileSystem.OpenExplicitFileRead(pathname);
            } else {
                fp = idLib.fileSystem.OpenFileRead(pathname);
            }
            if (null == fp) {
                return false;
            }
            length = fp.Length();
            buf = ByteBuffer.allocate(length + 1);
            fp.Read(buf, length);
            buf.put(length, (byte) 0);//[length] = '\0';
            this.fileTime = fp.Timestamp();
            this.filename = new idStr(fp.GetFullPath());
            idLib.fileSystem.CloseFile(fp);

            this.buffer = bbtocb(buf);
            this.length = length;
            // pointer in script buffer
//            this.script_p = this.buffer;
            this.script_p = 0;
            // pointer in script buffer before reading token
            this.lastScript_p = 0;//this.buffer;
            // pointer to end of script buffer
            this.end_p = this.buffer.length();//(this.buffer[length]);

            this.tokenAvailable = false;//0;
            this.line = 1;
            this.lastline = 1;
            this.allocated = true;
            this.loaded = true;

            return true;
        }

        // load a script from the given memory with the given length and a specified line offset,
        // so source strings extracted from a file can still refer to proper line numbers in the file
        // NOTE: the ptr is expected to point at a valid C string: ptr[length] == '\0'
        public boolean LoadMemory(final idStr ptr, int length, final idStr name/*= 1*/) throws idException {
            return LoadMemory(CharBuffer.wrap(ptr.c_str()), length, name.getData());
        }

        public boolean LoadMemory(final String ptr, int length, final String name/*= 1*/) throws idException {
            return LoadMemory(atocb(ptr), length, name, 1);
        }

        public boolean LoadMemory(final CharBuffer ptr, int length, final String name) throws idException {
            return LoadMemory(ptr, length, name, 1);
        }

        public boolean LoadMemory(final CharBuffer ptr, int length, final String name, int startLine) throws idException {
            if (this.loaded) {
                idLib.common.Error("this.LoadMemory: another script already loaded");
                return false;
            }
            this.filename = new idStr(name);
            this.buffer = CharBuffer.wrap(ptr.toString() + '\0');///TODO:should ptr and name be the same?
            this.fileTime = 0;
            this.length = length;
            // pointer in script buffer
            this.script_p = 0;//this.buffer;
            // pointer in script buffer before reading token
            this.lastScript_p = 0;//this.buffer;
            // pointer to end of script buffer
            this.end_p = this.buffer.length();//(this.buffer[length]);

            this.tokenAvailable = false;//0;
            this.line = startLine;
            this.lastline = startLine;
            this.allocated = false;
            this.loaded = true;

            return true;
        }

        public boolean LoadMemory(final String ptr, int length, final String name, int startLine) throws idException {
            return LoadMemory(CharBuffer.wrap(ptr), length, name, startLine);//the \0 is needed for the parsing loops.
        }

        public boolean LoadMemory(final idStr ptr, int length, final String name) throws idException {
            return LoadMemory(ptr.getData(), length, name);
        }

        // free the script
        public void FreeSource() {
//#ifdef PUNCTABLE
            if (this.punctuationTable != null && this.punctuationTable != default_punctuationtable) {
//                Mem_Free((void *) this.punctuationtable);
                this.punctuationTable = null;
            }
            if (this.nextPunctuation != null && this.nextPunctuation != default_nextpunctuation) {
//                Mem_Free((void *) this.nextpunctuation);
                this.nextPunctuation = null;
            }
//#endif //PUNCTABLE
            if (this.allocated) {
//                Mem_Free((void *) this.buffer);
                this.buffer = null;
                this.allocated = false;
            }
            this.tokenAvailable = false;//0;
            this.token = null;
            this.loaded = false;
        }

        // returns true if a script is loaded
        public boolean IsLoaded() {
            return this.loaded;
        }

        // read a token
        public boolean ReadToken(idToken token) throws idException {
            char c, c2;

            if (!loaded) {
                idLib.common.Error("idLexer::ReadToken: no file loaded");
                return false;
            }

            // if there is a token available (from unreadToken)
            if (tokenAvailable) {
                tokenAvailable = false;
                token.oSet(this.token);
                return true;
            }
            // save script pointer
            lastScript_p = script_p;
            // save line counter
            lastline = line;
            // clear the token stuff
            token.setData("");
            // token.setLen(0);
            // start of the white space
            whiteSpaceStart_p = token.whiteSpaceStart_p = script_p;
            // read white space before token
            if (!ReadWhiteSpace()) {
                return false;
            }
            // end of the white space
            this.whiteSpaceEnd_p = token.whiteSpaceEnd_p = script_p;
            // line the token is on
            token.line = line;
            // number of lines crossed before token
            token.linesCrossed = line - lastline;
            // clear token flags
            token.flags = 0;

            c = this.buffer.get(this.script_p);
            c2 = this.buffer.get(this.script_p + 1);

            // if we're keeping everything as whitespace deliminated strings
            if ((this.flags & LEXFL_ONLYSTRINGS) != 0) {
                // if there is a leading quote
                if (c == '\"' || c == '\'') {
                    if (!this.ReadString(token, c)) {
                        return false;
                    }
                } else if (!this.ReadName(token)) {
                    return false;
                }
            } // if there is a number
            else if ((Character.isDigit(c))
                    || (c == '.' && Character.isDigit(c2))) {
                if (!this.ReadNumber(token)) {
                    return false;
                }
                // if names are allowed to start with a number
                if ((this.flags & LEXFL_ALLOWNUMBERNAMES) != 0) {
                    c = this.buffer.get(this.script_p);
                    if (Character.isLetter(c) || c == '_') {
                        if (!this.ReadName(token)) {
                            return false;
                        }
                    }
                }
            } // if there is a leading quote
            else if (c == '\"' || c == '\'') {
                if (!this.ReadString(token, c)) {
                    return false;
                }
            } // if there is a name
            else if (Character.isLetter(c) || c == '_') {
                if (!this.ReadName(token)) {
                    return false;
                }
            } // names may also start with a slash when pathnames are allowed
            else if ((this.flags & LEXFL_ALLOWPATHNAMES) != 0 && ((c == '/' || c == '\\') || c == '.')) {
                if (!this.ReadName(token)) {
                    return false;
                }
            } // check for punctuations
            else if (!this.ReadPunctuation(token)) {
                this.Error("unknown punctuation %c", c);
                return false;
            }
            // succesfully read a token
            return true;
        }

        // expect a certain token, reads the token when available
        public boolean ExpectTokenString(final String string) throws idException {
            idToken token = new idToken();

            if (!this.ReadToken(token)) {
                this.Error("couldn't find expected '%s'", string);
                return false;
            }
            if (!token.equals(string)) {
                this.Error("expected '%s' but found '%s'", string, token);
                return false;
            }
            return true;
        }

        // expect a certain token type
        public int ExpectTokenType(int type, int subtype, idToken token) throws idException {
            idStr str = new idStr();

            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected token");
                return 0;
            }

            if (token.type != type) {
                switch (type) {
                    case TT_STRING:
                        str.oSet("string");
                        break;
                    case TT_LITERAL:
                        str.oSet("literal");
                        break;
                    case TT_NUMBER:
                        str.oSet("number");
                        break;
                    case TT_NAME:
                        str.oSet("name");
                        break;
                    case TT_PUNCTUATION:
                        str.oSet("punctuation");
                        break;
                    default:
                        str.oSet("unknown type");
                        break;
                }
                this.Error("expected a %s but found '%s'", str.getData(), token.getData());
                return 0;
            }
            if (token.type == TT_NUMBER) {
                if ((token.subtype & subtype) != subtype) {
                    str.Clear();
                    if ((subtype & TT_DECIMAL) != 0) {
                        str.oSet("decimal ");
                    }
                    if ((subtype & TT_HEX) != 0) {
                        str.oSet("hex ");
                    }
                    if ((subtype & TT_OCTAL) != 0) {
                        str.oSet("octal ");
                    }
                    if ((subtype & TT_BINARY) != 0) {
                        str.oSet("binary ");
                    }
                    if ((subtype & TT_UNSIGNED) != 0) {
                        str.Append("unsigned ");
                    }
                    if ((subtype & TT_LONG) != 0) {
                        str.Append("long ");
                    }
                    if ((subtype & TT_FLOAT) != 0) {
                        str.Append("float ");
                    }
                    if ((subtype & TT_INTEGER) != 0) {
                        str.Append("integer ");
                    }
                    str.StripTrailing(' ');
                    this.Error("expected %s but found '%s'", str.getData(), token.getData());
                    return 0;
                }
            } else if (token.type == TT_PUNCTUATION) {
                if (subtype < 0) {
                    this.Error("BUG: wrong punctuation subtype");
                    return 0;
                }
                if (token.subtype != subtype) {
                    this.Error("expected '%s' but found '%s'", GetPunctuationFromId(subtype), token.getData());
                    return 0;
                }
            }
            return 1;
        }

        // expect a token
        public boolean ExpectAnyToken(idToken token) throws idException {
            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected token");
                return false;
            } else {
                return true;
            }
        }

        // returns true when the token is available
        public boolean CheckTokenString(final String string) throws idException {
            idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }
            // if the given string is available
            if (tok.Cmp(string) == 0) {
                return true;
            }
            // unread token
            script_p = lastScript_p;
            line = lastline;
            return false;
        }

        // returns true an reads the token when a token with the given type is available
        public int CheckTokenType(int type, int subtype, idToken token) throws idException {
            idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return 0;
            }
            // if the type matches
            if (tok.type == type && (tok.subtype & subtype) == subtype) {
                token.oSet(tok);
                return 1;
            }
            // unread token
            script_p = lastScript_p;
            line = lastline;
            return 0;
        }

        // returns true if the next token equals the given string but does not remove the token from the source
        public boolean PeekTokenString(final String string) throws idException {
            idToken token = new idToken();

            if (!ReadToken(token)) {
                return false;
            }

            // unread token
            script_p = lastScript_p;
            line = lastline;

            return (token.getData().equals(string));
        }

        // returns true if the next token equals the given type but does not remove the token from the source
        public boolean PeekTokenType(int type, int subtype, idToken[] token) throws idException {
            idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }

            // unread token
            script_p = lastScript_p;
            line = lastline;

            // if the type matches
            if (tok.type == type && (tok.subtype & subtype) == subtype) {
                token[0] = tok;
                return true;
            }
            return false;
        }

        // skip tokens until the given token string is read
        public boolean SkipUntilString(final String string) throws idException {
            idToken token = new idToken();

            while (this.ReadToken(token)) {
                if (token.getData().equals(string)) {
                    return true;
                }
            }
            return false;
        }

        // skip the rest of the current line
        public int SkipRestOfLine() throws idException {
            idToken token = new idToken();

            while (this.ReadToken(token)) {
                if (token.linesCrossed != 0) {
                    this.script_p = lastScript_p;
                    this.line = lastline;
                    return 1;
                }
            }
            return 0;
        }

        // skip the braced section
        public boolean SkipBracedSection() throws idException {
            return SkipBracedSection(true);
        }

        /*
         =================
         idLexer::SkipBracedSection

         Skips until a matching close brace is found.
         Internal brace depths are properly skipped.
         =================
         */
        public boolean SkipBracedSection(boolean parseFirstBrace) throws idException {
            idToken token = new idToken();
            int depth;

            depth = parseFirstBrace ? 0 : 1;
            do {
                if (!ReadToken(token)) {
                    return false;
                }
                if (token.type == TT_PUNCTUATION) {
                    if (token.equals("{")) {
                        depth++;
                    } else if (token.equals("}")) {
                        depth--;
                    }
                }
            } while (depth != 0);
            return true;
        }

        // unread the given token
        public void UnreadToken(final idToken token) throws idException {
            if (this.tokenAvailable) {
                idLib.common.FatalError("idLexer::unreadToken, unread token twice\n");
            }
            this.token = token;
            this.tokenAvailable = true;
        }

        // read a token only if on the same line
        public boolean ReadTokenOnLine(idToken token) throws idException {
            idToken tok = new idToken();

            if (!this.ReadToken(tok)) {
                this.script_p = lastScript_p;
                this.line = lastline;
                return false;
            }
            // if no lines were crossed before this token
            if (0 == tok.linesCrossed) {
                token.oSet(tok);
                return true;
            }
            // restore our position
            this.script_p = lastScript_p;
            this.line = lastline;
            token.Clear();
            return false;
        }
//		

        //Returns the rest of the current line
        public String ReadRestOfLine(idStr out) {
            while (true) {

                if (this.buffer.get(this.script_p) == '\n') {
                    this.line++;
                    break;
                }

                if (0 == this.buffer.get(this.script_p)) {
                    break;
                }

                if (this.buffer.get(this.script_p) <= ' ') {
                    out.Append(" ");
                } else {
                    out.Append(this.buffer.get(this.script_p));
                }
                this.script_p++;

            }

            out.Strip(' ');
            return out.getData();
        }
//

        // read a signed integer
        public int ParseInt() throws idException {
            idToken token = new idToken();

            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected integer");
                return 0;
            }
            if (token.type == TT_PUNCTUATION && token.equals("-")) {
                this.ExpectTokenType(TT_NUMBER, TT_INTEGER, token);
                return -token.GetIntValue();
            } else if (token.type != TT_NUMBER || token.subtype == TT_FLOAT) {
                this.Error("expected integer value, found '%s'", token);
            }
            return token.GetIntValue();
        }

        // read a Boolean
        public boolean ParseBool() throws idException {
            idToken token = new idToken();

            if (0 == this.ExpectTokenType(TT_NUMBER, 0, token)) {
                this.Error("couldn't read expected boolean");
                return false;
            }
            return (token.GetIntValue() != 0);
        }

        public float ParseFloat() throws idException {
            return ParseFloat(null);
        }

        // read a floating point number.  If errorFlag is NULL, a non-numeric token will
        // issue an Error().  If it isn't NULL, it will issue a Warning() and set *errorFlag = true
        public float ParseFloat(boolean[] errorFlag/*= NULL*/) throws idException {
            idToken token = new idToken();

            if (errorFlag != null) {
                errorFlag[0] = false;
            }

            if (!this.ReadToken(token)) {
                if (errorFlag != null) {
                    this.Warning("couldn't read expected floating point number");
                    errorFlag[0] = true;
                } else {
                    this.Error("couldn't read expected floating point number");
                }
                return 0;
            }
            if (token.type == TT_PUNCTUATION && token.equals("-")) {
                this.ExpectTokenType(TT_NUMBER, 0, token);
                return -token.GetFloatValue();
            } else if (token.type != TT_NUMBER) {
                if (errorFlag != null) {
                    this.Warning("expected float value, found '%s'", token);
                    errorFlag[0] = true;
                } else {
                    this.Error("expected float value, found '%s'", token);
                }
            }
            return token.GetFloatValue();
        }

        public boolean Parse1DMatrix(int x, idVec v) throws idException {
            float[] m = new float[x];
            boolean result = Parse1DMatrix(x, m);
            for (int i = 0; i < x; i++) {
                v.oSet(i, m[i]);
            }

            return result;
        }
        
        public boolean Parse1DMatrix(int x, idPlane p) throws idException {
            float[] m = new float[x];
            boolean result = Parse1DMatrix(x, m);
            for (int i = 0; i < x; i++) {
                p.oSet(i, m[i]);
            }

            return result;
        }
        
        public boolean Parse1DMatrix(int x, idMat3 m) throws idException {
            float[] n = new float[x];
            boolean result = Parse1DMatrix(x, n);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    m.oSet(i, j, n[i * 3 + j]);
                }
            }

            return result;
        }
        
        public boolean Parse1DMatrix(int x, idQuat q) throws idException {
            float[] m = new float[x];
            boolean result = Parse1DMatrix(x, m);
            for (int i = 0; i < x; i++) {
                q.oSet(i, m[i]);
            }

            return result;
        }
        
        public boolean Parse1DMatrix(int x, idCQuat q) throws idException {
            float[] m = new float[x];
            boolean result = Parse1DMatrix(x, m);
            for (int i = 0; i < x; i++) {
                q.oSet(i, m[i]);
            }

            return result;
        }

        public boolean Parse1DMatrix(int x, float[] m) throws idException {
            return this.Parse1DMatrix(x, m, 0);
        }

        // parse matrices with floats
        private boolean Parse1DMatrix(int x, float[] m, final int offset) throws idException {
            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (int i = 0; i < x; i++) {
                m[offset + i] = this.ParseFloat();
            }

            return (this.ExpectTokenString(")"));
        }

        public boolean Parse2DMatrix(int y, int x, idVec3[] m) throws idException {
            if (!this.ExpectTokenString("(")) {
                return false;
            }
            
            for (int i = 0; i < y; i++) {
                if (!Parse1DMatrix(x, m[i])) {
                    return false;
                }
            }

            return (this.ExpectTokenString(")"));
        }

        public boolean Parse2DMatrix(int y, int x, float[] m) throws idException {
            return this.Parse2DMatrix(y, x, m, 0);
        }

        private boolean Parse2DMatrix(int y, int x, float[] m, final int offset) throws idException {
            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (int i = 0; i < y; i++) {
                if (!this.Parse1DMatrix(x, m, offset + (i * x))) {
                    return false;
                }
            }

            return (this.ExpectTokenString(")"));
        }

        public boolean Parse3DMatrix(int z, int y, int x, float[] m) throws idException {
            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (int i = 0; i < z; i++) {
                if (!this.Parse2DMatrix(y, x, m, i * x * y)) {
                    return false;
                }
            }

            return (this.ExpectTokenString(")"));
        }

        /*
         =================
         idLexer::ParseBracedSection

         The next token should be an open brace.
         Parses until a matching close brace is found.
         Internal brace depths are properly skipped.
         =================
         */
        // parse a braced section into a string
        public String ParseBracedSection(idStr out) throws idException {
            idToken token = new idToken();
            int i, depth;

            out.Empty();
            if (!this.ExpectTokenString("{")) {
                return out.getData();
            }
            out.oSet("{");
            depth = 1;
            do {
                if (!this.ReadToken(token)) {
                    Error("missing closing brace");
                    return out.getData();
                }

                // if the token is on a new line
                for (i = 0; i < token.linesCrossed; i++) {
                    out.oPluSet("\r\n");
                }

                if (token.type == TT_PUNCTUATION) {
                    if (token.equals('{')) {
                        depth++;
                    } else if (token.equals('}')) {
                        depth--;
                    }
                }

                if (token.type == TT_STRING) {
                    out.oPluSet("\"" + token + "\"");
                } else {
                    out.oPluSet(token);
                }
                out.oPluSet(" ");
            } while (depth != 0);

            return out.getData();
        }

        /*
         =================
         idParser::ParseBracedSection

         The next token should be an open brace.
         Parses until a matching close brace is found.
         Maintains exact characters between braces.

         FIXME: this should use ReadToken and replace the token white space with correct indents and newlines
         =================
         */
        public String ParseBracedSection(idStr out, int tabs/*= -1*/) throws idException {
            int depth;
            boolean doTabs;
            boolean skipWhite;

            out.Empty();

            if (!this.ExpectTokenString("{")) {
                return out.getData();
            }

            out.oSet("{");
            depth = 1;
            skipWhite = false;
            doTabs = tabs >= 0;

            while (depth != 0 && this.buffer.get(this.script_p) != 0) {
                char c = this.buffer.get(this.script_p++);

                switch (c) {
                    case '\t':
                    case ' ': {
                        if (skipWhite) {
                            continue;
                        }
                        break;
                    }
                    case '\n': {
                        if (doTabs) {
                            skipWhite = true;
                            out.oPluSet(c);
                            continue;
                        }
                        break;
                    }
                    case '{': {
                        depth++;
                        tabs++;
                        break;
                    }
                    case '}': {
                        depth--;
                        tabs--;
                        break;
                    }
                }

                if (skipWhite) {
                    int i = tabs;
                    if (c == '{') {
                        i--;
                    }
                    skipWhite = false;
                    for (; i > 0; i--) {
                        out.oPluSet('\t');
                    }
                }
                out.oPluSet(c);
            }
            return out.getData();
        }

        /*
         =================
         idParser::ParseBracedSection

         The next token should be an open brace.
         Parses until a matching close brace is found.
         Maintains exact characters between braces.

         FIXME: this should use ReadToken and replace the token white space with correct indents and newlines
         =================
         */
        // parse a braced section into a string, maintaining indents and newlines
//public	String	ParseBracedSectionExact ( idStr &out, int tabs = -1 );
        public String ParseBracedSectionExact(idStr out, int tabs) throws idException {
            int depth;
            boolean doTabs;
            boolean skipWhite;

            out.Empty();

            if (!this.ExpectTokenString("{")) {
                return out.getData();
            }

            out.oSet("{");
            depth = 1;
            skipWhite = false;
            doTabs = tabs >= 0;

            while (depth != 0 && this.buffer.get(this.script_p) != 0) {
                char c = this.buffer.get(this.script_p++);

                switch (c) {
                    case '\t':
                    case ' ': {
                        if (skipWhite) {
                            continue;
                        }
                        break;
                    }
                    case '\n': {
                        if (doTabs) {
                            skipWhite = true;
                            out.oPluSet(c);
                            continue;
                        }
                        break;
                    }
                    case '{': {
                        depth++;
                        tabs++;
                        break;
                    }
                    case '}': {
                        depth--;
                        tabs--;
                        break;
                    }
                }

                if (skipWhite) {
                    int i = tabs;
                    if (c == '{') {
                        i--;
                    }
                    skipWhite = false;
                    for (; i > 0; i--) {
                        out.oPluSet('\t');
                    }
                }
                out.oPluSet(c);
            }
            return out.getData();
        }

        // parse the rest of the line
        public String ParseRestOfLine(idStr out) throws idException {
            idToken token = new idToken();

            out.Empty();
            while (this.ReadToken(token)) {
                if (token.linesCrossed != 0) {
                    this.script_p = lastScript_p;
                    this.line = lastline;
                    break;
                }
                if (out.Length() != 0) {
                    out.oPluSet(" ");
                }
                out.oPluSet(token);
            }
            return out.getData();
        }

        // retrieves the white space characters before the last read token
        public int GetLastWhiteSpace(idStr whiteSpace) {
            whiteSpace.Clear();
            for (int p = whiteSpaceStart_p; p < whiteSpaceEnd_p; p++) {
                whiteSpace.Append(buffer.get(p));
            }
            return whiteSpace.Length();
        }

        // returns start index into text buffer of last white space
        public int GetLastWhiteSpaceStart() {
            return whiteSpaceStart_p;// - buffer;
        }

        // returns end index into text buffer of last white space
        public int GetLastWhiteSpaceEnd() {
            return whiteSpaceEnd_p;// - buffer;
        }

        // set an array with punctuations, NULL restores default C/C++ set, see default_punctuations for an example
        public void SetPunctuations(final punctuation_t[] p) {
            if (PUNCTABLE) {
                if (p != null) {
                    this.CreatePunctuationTable(p);
                } else {
                    this.CreatePunctuationTable(default_punctuations);
                }
            } //PUNCTABLE
            if (p != null) {
                this.punctuations = p;
            } else {
                this.punctuations = default_punctuations;
            }
        }

        // returns a pointer to the punctuation with the given id
        public String GetPunctuationFromId(int id) {
            int i;

            for (i = 0; this.punctuations[i].p != null; i++) {
                if (this.punctuations[i].n == id) {
                    return this.punctuations[i].p;
                }
            }
            return "unkown punctuation";
        }

        // get the id for the given punctuation
        public int GetPunctuationId(final String p) {
            int i;

            for (i = 0; this.punctuations[i].p != null; i++) {
                if (this.punctuations[i].p.equals(p)) {
                    return this.punctuations[i].n;
                }
            }
            return 0;
        }
        // set lexer flags

        public void SetFlags(int flags) {
            this.flags = flags;
        }

        // get lexer flags
        public long GetFlags() {
            return this.flags;
        }

        // reset the lexer
        public void Reset() {
            // pointer in script buffer
//            this.script_p = this.buffer;
            this.script_p = 0;
            // pointer in script buffer before reading token
//            this.lastScript_p = this.buffer;
            this.lastScript_p = 0;
            // begin of white space
            this.whiteSpaceStart_p = 0;
            // end of white space
            this.whiteSpaceEnd_p = 0;
            // set if there's a token available in this.token
            this.tokenAvailable = false;

            this.line = 1;
            this.lastline = 1;
            // clear the saved token
            this.token = new idToken();
        }

        // returns true if at the end of the file
        public boolean EndOfFile() {
            return this.script_p >= this.end_p;
        }

        // returns the current filename
        public idStr GetFileName() {
            return this.filename;
        }

        // get offset in script
        public int GetFileOffset() {
            return this.script_p;//- this.buffer;
        }

        // get file time
        public long/*ID_TIME_T*/ GetFileTime() {
            return this.fileTime;
        }

        // returns the current line number
        public int GetLineNum() {
            return this.line;
        }

        // print an error message
        public void Error(final String fmt, final Object... str) throws idException {//id_attribute((format(printf,2,3)));
            String text;//[MAX_STRING_CHARS];
//            va_list ap;

            hadError = true;

            if ((this.flags & LEXFL_NOERRORS) != 0) {
                return;
            }

            text = String.format(fmt, str);
//            va_start(ap, str);
//            vsprintf(text, str, ap);
//            va_end(ap);

            if ((this.flags & LEXFL_NOFATALERRORS) != 0) {
                idLib.common.Warning("file %s, line %d: %s", this.filename.getData(), this.line, text);
            } else {
                idLib.common.Error("file %s, line %d: %s", this.filename.getData(), this.line, text);
            }
        }

        // print a warning message
        public void Warning(final String fmt, final Object... str) throws idException {//id_attribute((format(printf,2,3)));
            String text;//[MAX_STRING_CHARS];
//	va_list ap;

            if ((this.flags & LEXFL_NOWARNINGS) != 0) {
                return;
            }

            text = String.format(fmt, str);
//	va_start( ap, str );
//	vsprintf( text, str, ap );
//	va_end( ap );
            idLib.common.Warning("file %s, line %d: %s", this.filename.getData(), this.line, text);
        }

        // returns true if Error() was called with LEXFL_NOFATALERRORS or LEXFL_NOERRORS set
        public boolean HadError() {
            return hadError;
        }

        // set the base folder to load files from
        public static void SetBaseFolder(final String path) {
            idStr.Copynz(baseFolder, path);//TODO:length?
        }

        private void CreatePunctuationTable(final punctuation_t[] punctuations) {
            int i, n, lastp;
            punctuation_t p, newp;

            //get memory for the table
            if (Arrays.equals(punctuations, default_punctuations)) {
                this.punctuationTable = default_punctuationtable;
                this.nextPunctuation = default_nextpunctuation;
                if (default_setup) {
                    return;
                }
                default_setup = true;
                i = default_punctuations.length;
            } else {
                if (NOT(this.punctuationTable) || Arrays.equals(this.punctuationTable, default_punctuationtable)) {
                    this.punctuationTable = new int[256];// (int *) Mem_Alloc(256 * sizeof(int));
                }
                if (this.nextPunctuation != null && !Arrays.equals(this.nextPunctuation, default_nextpunctuation)) {
//			Mem_Free( this.nextPunctuation );
                    this.nextPunctuation = null;
                }
                for (i = 0; punctuations[i].p != null; i++) {
                }
                this.nextPunctuation = new int[i];//(int *) Mem_Alloc(i * sizeof(int));
            }

            Arrays.fill(this.punctuationTable, 0, 256, -1);//memset(this.punctuationTable, 0xFF, 256 * sizeof(int));
            Arrays.fill(this.nextPunctuation, 0, i, -1);//memset(this.nextPunctuation, 0xFF, i * sizeof(int));
            //add the punctuations in the list to the punctuation table
            for (i = 0; punctuations[i].p != null; i++) {
                newp = punctuations[i];
                lastp = -1;
                //sort the punctuations in this table entry on length (longer punctuations first)
                for (n = this.punctuationTable[newp.p.charAt(0)]; n >= 0; n = this.nextPunctuation[n]) {
                    p = punctuations[n];
                    if (p.p.length() < newp.p.length()) {
                        this.nextPunctuation[i] = n;
                        if (lastp >= 0) {
                            this.nextPunctuation[lastp] = i;
                        } else {
                            this.punctuationTable[newp.p.charAt(0)] = i;
                        }
                        break;
                    }
                    lastp = n;
                }
                if (n < 0) {
                    this.nextPunctuation[i] = -1;
                    if (lastp >= 0) {
                        this.nextPunctuation[lastp] = i;
                    } else {
                        this.punctuationTable[newp.p.charAt(0)] = i;
                    }
                }
            }
        }

        /*
         ================
         idLexer::ReadWhiteSpace

         Reads spaces, tabs, C-like comments etc.
         When a newline character is found the scripts line counter is increased.
         ================
         */
        private boolean ReadWhiteSpace() throws idException {
            try {
                while (true) {
                    // skip white space
                    while (this.buffer.get(this.script_p) <= ' ') {
                        if (0 == this.buffer.get(this.script_p)) {
                            return false;
                        }
                        if (this.buffer.get(this.script_p) == '\n') {
                            this.line++;
                        }
                        this.script_p++;
                    }
                    // skip comments
                    if (this.buffer.get(this.script_p) == '/') {
                        // comments //
                        if (this.buffer.get(this.script_p + 1) == '/') {
                            this.script_p++;
                            do {
                                this.script_p++;
                                if (0 == this.buffer.get(this.script_p)) {
                                    return false;
                                }
                            } while (this.buffer.get(this.script_p) != '\n');
                            this.line++;
                            this.script_p++;
                            if (0 == this.buffer.get(this.script_p)) {
                                return false;
                            }
                            continue;
                        } // comments /* */
                        else if (this.buffer.get(this.script_p + 1) == '*') {
                            this.script_p++;
                            while (true) {
                                this.script_p++;
                                if (0 == this.buffer.get(this.script_p)) {
                                    return false;//0;
                                }
                                if (this.buffer.get(this.script_p) == '\n') {
                                    this.line++;
                                } else if (this.buffer.get(this.script_p) == '/') {
                                    if (this.buffer.get(this.script_p - 1) == '*') {
                                        break;
                                    }
                                    if (this.buffer.get(this.script_p + 1) == '*') {
                                        this.Warning("nested comment");
                                    }
                                }
                            }
                            this.script_p++;
                            if (0 == this.buffer.get(this.script_p)) {
                                return false;
                            }
                            this.script_p++;
                            if (0 == this.buffer.get(this.script_p)) {
                                return false;
                            }
                            continue;
                        }
                    }
                    break;
                }
                return true;
            } catch (IndexOutOfBoundsException e) {//TODO:think of a more elegant solution you lout!
                return false;
            }
        }

        private boolean ReadEscapeCharacter(char[] ch) throws idException {
            int c, val, i;

            // step over the leading '\\'
            this.script_p++;
            // determine the escape character
            switch (this.buffer.get(this.script_p)) {
                case '\\':
                    c = '\\';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 'r':
                    c = '\r';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'v':
                    c = '\u000B';//'\v';
                    break;
                case 'b':
                    c = '\b';
                    break;
                case 'f':
                    c = '\f';
                    break;
                case 'a':
                    c = '\u0007';//'\a';
                    break;
                case '\'':
                    c = '\'';
                    break;
                case '\"':
                    c = '\"';
                    break;
                case '?':
                    c = '?';
                    break;
                case 'x': {
                    this.script_p++;
                    for (i = 0, val = 0;; i++, this.script_p++) {
                        c = this.buffer.get(this.script_p);
                        if (Character.isDigit(c)) {
                            c = c - '0';
                        } else if (Character.isUpperCase(c)) {
                            c = c - 'A' + 10;
                        } else if (Character.isLowerCase(c)) {
                            c = c - 'a' + 10;
                        } else {
                            break;
                        }
                        val = (val << 4) + c;
                    }
                    this.script_p--;
                    if (val > 0xFF) {
                        this.Warning("too large value in escape character");
                        val = 0xFF;
                    }
                    c = val;
                    break;
                }
                default: //NOTE: decimal ASCII code, NOT octal
                {
                    if (this.buffer.get(this.script_p) < '0' || this.buffer.get(this.script_p) > '9') {
                        this.Error("unknown escape char");
                    }
                    for (i = 0, val = 0;; i++, this.script_p++) {
                        c = this.buffer.get(this.script_p);
                        if (Character.isDigit(c)) {
                            c = c - '0';
                        } else {
                            break;
                        }
                        val = val * 10 + c;
                    }
                    this.script_p--;
                    if (val > 0xFF) {
                        this.Warning("too large value in escape character");
                        val = 0xFF;
                    }
                    c = val;
                    break;
                }
            }
            // step over the escape character or the last digit of the number
            this.script_p++;
            // store the escape character
            ch[0] = (char) c;
            // succesfully read escape character
            return true;
        }

        /*
         ================
         idLexer::ReadString

         Escape characters are interpretted.
         Reads two strings with only a white space between them as one string.
         ================
         */
        private boolean ReadString(idToken token, int quote) throws idException {
            int tmpline;
            int tmpscript_p;
            char[] ch = new char[1];

            if (quote == '\"') {
                token.type = TT_STRING;
            } else {
                token.type = TT_LITERAL;
            }

            // leading quote
            this.script_p++;

            while (true) {
                // if there is an escape character and escape characters are allowed
                if (this.buffer.get(this.script_p) == '\\' && 0 == (this.flags & LEXFL_NOSTRINGESCAPECHARS)) {
                    if (!this.ReadEscapeCharacter(ch)) {
                        return false;
                    }
                    token.AppendDirty(ch[0]);
                } // if a trailing quote
                else if (this.buffer.get(this.script_p) == quote) {
                    // step over the quote
                    this.script_p++;
                    // if consecutive strings should not be concatenated
                    if (((this.flags & LEXFL_NOSTRINGCONCAT) != 0)
                            && (0 == (this.flags & LEXFL_ALLOWBACKSLASHSTRINGCONCAT) || (quote != '\"'))) {
                        break;
                    }

                    tmpscript_p = this.script_p;
                    tmpline = this.line;
                    // read white space between possible two consecutive strings
                    if (!this.ReadWhiteSpace()) {
                        this.script_p = tmpscript_p;
                        this.line = tmpline;
                        break;
                    }

                    if ((this.flags & LEXFL_NOSTRINGCONCAT) != 0) {
                        if (this.buffer.get(this.script_p) != '\\') {
                            this.script_p = tmpscript_p;
                            this.line = tmpline;
                            break;
                        }
                        // step over the '\\'
                        this.script_p++;
                        if (!this.ReadWhiteSpace() || (this.buffer.get(this.script_p) != quote)) {
                            this.Error("expecting string after '\' terminated line");
                            return false;
                        }
                    }

                    // if there's no leading qoute
                    if (this.buffer.get(this.script_p) != quote) {
                        this.script_p = tmpscript_p;
                        this.line = tmpline;
                        break;
                    }
                    // step over the new leading quote
                    this.script_p++;
                } else {
                    if (this.buffer.get(this.script_p) == '\0') {
                        this.Error("missing trailing quote");
                        return false;
                    }
                    if (this.buffer.get(this.script_p) == '\n') {
                        this.Error("newline inside string");
                        return false;
                    }
                    token.AppendDirty(this.buffer.get(this.script_p++));
                }
            }
//            token.oSet(token.len, '\0');

            if (token.type == TT_LITERAL) {
                if (0 == (this.flags & LEXFL_ALLOWMULTICHARLITERALS)) {
                    if (token.Length() != 1) {
                        this.Warning("literal is not one character long");
                    }
                }
                token.subtype = token.oGet(0);
            } else {
                // the sub type is the length of the string
                token.subtype = token.Length();
            }
            return true;
        }

        private boolean ReadName(idToken token) {
            char c;

            token.type = TT_NAME;
            do {
                token.AppendDirty(this.buffer.get(this.script_p));
            } while (this.script_p++ + 1 < this.buffer.capacity()
                    && (Character.isLowerCase(c = this.buffer.get(this.script_p))
                    || (Character.isUpperCase(c))
                    || (Character.isDigit(c))
                    || c == '_'
                    || // if treating all tokens as strings, don't parse '-' as a seperate token
                    (((this.flags & LEXFL_ONLYSTRINGS) != 0) && (c == '-'))
                    || // if special path name characters are allowed
                    (((this.flags & LEXFL_ALLOWPATHNAMES) != 0) && (c == '/' || c == '\\' || c == ':' || c == '.'))));
//            token.oSet(token.len, '\0');
            //the sub type is the length of the name
            token.subtype = token.Length();
            return true;
        }

        private boolean ReadNumber(idToken token) throws idException {
            int i;
            int dot;
            char c, c2;

            token.type = TT_NUMBER;
            token.subtype = 0;
            token.intValue = 0;
            token.floatValue = 0;

            c = this.buffer.get(this.script_p);
            c2 = this.buffer.get(this.script_p + 1);

            if (c == '0' && c2 != '.') {
                // check for a hexadecimal number
                if (c2 == 'x' || c2 == 'X') {
                    token.AppendDirty(this.buffer.get(this.script_p++));
                    token.AppendDirty(this.buffer.get(this.script_p++));
                    c = this.buffer.get(this.script_p);
                    while ((Character.isDigit(c))
                            || (c >= 'a' && c <= 'f')
                            || (c >= 'A' && c <= 'F')) {
                        token.AppendDirty(c);
                        c = this.buffer.get(++this.script_p);
                    }
                    token.subtype = TT_HEX | TT_INTEGER;
                } // check for a binary number
                else if (c2 == 'b' || c2 == 'B') {
                    token.AppendDirty(this.buffer.get(this.script_p++));
                    token.AppendDirty(this.buffer.get(this.script_p++));
                    c = this.buffer.get(this.script_p);
                    while (c == '0' || c == '1') {
                        token.AppendDirty(c);
                        c = this.buffer.get(++this.script_p);
                    }
                    token.subtype = TT_BINARY | TT_INTEGER;
                } // its an octal number
                else {
                    token.AppendDirty(this.buffer.get(this.script_p++));
                    c = this.buffer.get(this.script_p);
                    while (c >= '0' && c <= '7') {
                        token.AppendDirty(c);
                        c = this.buffer.get(++this.script_p);
                    }
                    token.subtype = TT_OCTAL | TT_INTEGER;
                }
            } else {
                // decimal integer or floating point number or ip address
                dot = 0;
                while (true) {
                    if (Character.isDigit(c)) {
                        // if (c >= '0' && c <= '9') {
                    } else if (c == '.') {
                        dot++;
                    } else {
                        break;
                    }
                    token.AppendDirty(c);
                    c = this.buffer.get(++this.script_p);
                }
                if (c == 'e' && dot == 0) {
                    //We have scientific notation without a decimal point
                    dot++;
                }
                // if a floating point number
                if (dot == 1) {
                    token.subtype = TT_DECIMAL | TT_FLOAT;
                    // check for floating point exponent
                    if (c == 'e') {
                        //Append the e so that GetFloatValue code works
                        token.AppendDirty(c);
                        c = this.buffer.get(++this.script_p);
                        if (c == '-') {
                            token.AppendDirty(c);
                            c = this.buffer.get(++this.script_p);
                        } else if (c == '+') {
                            token.AppendDirty(c);
                            c = this.buffer.get(++this.script_p);
                        }
                        while (Character.isDigit(c)) {//c >= '0' && c <= '9') {
                            token.AppendDirty(c);
                            c = this.buffer.get(++this.script_p);
                        }
                    } // check for floating point exception infinite 1.#INF or indefinite 1.#IND or NaN
                    else if (c == '#') {
                        c2 = 4;
                        if (CheckString("INF")) {
                            token.subtype |= TT_INFINITE;
                        } else if (CheckString("IND")) {
                            token.subtype |= TT_INDEFINITE;
                        } else if (CheckString("NAN")) {
                            token.subtype |= TT_NAN;
                        } else if (CheckString("QNAN")) {
                            token.subtype |= TT_NAN;
                            c2++;
                        } else if (CheckString("SNAN")) {
                            token.subtype |= TT_NAN;
                            c2++;
                        }
                        for (i = 0; i < c2; i++) {
                            token.AppendDirty(c);
                            c = this.buffer.get(++this.script_p);
                        }
                        while (Character.isDigit(c)) {
                            token.AppendDirty(c);
                            c = this.buffer.get(++this.script_p);
                        }
                        if (0 == (this.flags & LEXFL_ALLOWFLOATEXCEPTIONS)) {
//                            token.AppendDirty('\0');	// zero terminate for c_str
                            this.Error("parsed %s", token.getData());
                        }
                    }
                } else if (dot > 1) {
                    if (0 == (this.flags & LEXFL_ALLOWIPADDRESSES)) {
                        this.Error("more than one dot in number");
                        return false;
                    }
                    if (dot != 3) {
                        this.Error("ip address should have three dots");
                        return false;
                    }
                    token.subtype = TT_IPADDRESS;
                } else {
                    token.subtype = TT_DECIMAL | TT_INTEGER;
                }
            }

            if ((token.subtype & TT_FLOAT) != 0) {
                if (c > ' ') {
                    // single-precision: float
                    if (c == 'f' || c == 'F') {
                        token.subtype |= TT_SINGLE_PRECISION;
                        this.script_p++;
                    } // extended-precision: long double
                    else if (c == 'l' || c == 'L') {
                        token.subtype |= TT_EXTENDED_PRECISION;
                        this.script_p++;
                    } // default is double-precision: double
                    else {
                        token.subtype |= TT_DOUBLE_PRECISION;
                    }
                } else {
                    token.subtype |= TT_DOUBLE_PRECISION;
                }
            } else if ((token.subtype & TT_INTEGER) != 0) {
                if (c > ' ') {
                    // default: signed long
                    for (i = 0; i < 2; i++) {
                        // long integer
                        if (c == 'l' || c == 'L') {
                            token.subtype |= TT_LONG;
                        } // unsigned integer
                        else if (c == 'u' || c == 'U') {
                            token.subtype |= TT_UNSIGNED;
                        } else {
                            break;
                        }
                        c = this.buffer.get(++this.script_p);
                    }
                }
            } else if ((token.subtype & TT_IPADDRESS) != 0) {
                if (c == ':') {
                    token.AppendDirty(c);
                    c = this.buffer.get(++this.script_p);
                    while (Character.isDigit(c)) {
                        token.AppendDirty(c);
                        c = this.buffer.get(++this.script_p);
                    }
                    token.subtype |= TT_IPPORT;
                }
            }
//            token.oSet(token.len, '\0');
            return true;
        }

        private boolean ReadPunctuation(idToken token) {
            int l, n, i;
            char[] p;
            punctuation_t punc;

// #ifdef PUNCTABLE
            for (n = this.punctuationTable[(int) this.buffer.get(this.script_p)]; n >= 0; n = this.nextPunctuation[n]) {
                punc = (this.punctuations[n]);
// #else
//	int i;
//
//	for (i = 0; idLexer::punctuations[i].p; i++) {
//		punc = &idLexer::punctuations[i];
//#endif
                p = punc.p.toCharArray();
                // check for this punctuation in the script
                for (l = 0; l < p.length && this.buffer.get(this.script_p + l) != 0; l++) {
                    if (this.buffer.get(this.script_p + l) != p[l]) {
                        break;
                    }
                }
                if (l >= p.length) {
                    //
                    token.EnsureAlloced(l + 1, false);
                    for (i = 0; i < l; i++) {
//                        token.data[i] = p[i];
                        token.oSet(i, p[i]);
                    }
                    // token.setLen(l);
                    //
                    this.script_p += l;
                    token.type = TT_PUNCTUATION;
                    // sub type is the punctuation id
                    token.subtype = punc.n;
                    return true;
                }
            }
            return false;
        }

//        private boolean ReadPrimitive(idToken token);
        private boolean CheckString(final String str) {
            int i;

            for (i = 0; str.charAt(i) != 0; i++) {
                if (this.buffer.get(i + script_p) != str.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        private int NumLinesCrossed() {
            return this.line - this.lastline;
        }
    };
}
