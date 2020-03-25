package neo.idlib.Text;

import static neo.TempDump.NOT;
import static neo.TempDump.atocb;
import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.idlib.Text.Lexer.LEXFL_NOBASEINCLUDES;
import static neo.idlib.Text.Lexer.LEXFL_NODOLLARPRECOMPILE;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.P_ADD;
import static neo.idlib.Text.Lexer.P_BIN_AND;
import static neo.idlib.Text.Lexer.P_BIN_NOT;
import static neo.idlib.Text.Lexer.P_BIN_OR;
import static neo.idlib.Text.Lexer.P_BIN_XOR;
import static neo.idlib.Text.Lexer.P_COLON;
import static neo.idlib.Text.Lexer.P_DEC;
import static neo.idlib.Text.Lexer.P_DIV;
import static neo.idlib.Text.Lexer.P_INC;
import static neo.idlib.Text.Lexer.P_LOGIC_AND;
import static neo.idlib.Text.Lexer.P_LOGIC_EQ;
import static neo.idlib.Text.Lexer.P_LOGIC_GEQ;
import static neo.idlib.Text.Lexer.P_LOGIC_GREATER;
import static neo.idlib.Text.Lexer.P_LOGIC_LEQ;
import static neo.idlib.Text.Lexer.P_LOGIC_LESS;
import static neo.idlib.Text.Lexer.P_LOGIC_NOT;
import static neo.idlib.Text.Lexer.P_LOGIC_OR;
import static neo.idlib.Text.Lexer.P_LOGIC_UNEQ;
import static neo.idlib.Text.Lexer.P_LSHIFT;
import static neo.idlib.Text.Lexer.P_MOD;
import static neo.idlib.Text.Lexer.P_MUL;
import static neo.idlib.Text.Lexer.P_PARENTHESESCLOSE;
import static neo.idlib.Text.Lexer.P_PARENTHESESOPEN;
import static neo.idlib.Text.Lexer.P_QUESTIONMARK;
import static neo.idlib.Text.Lexer.P_RSHIFT;
import static neo.idlib.Text.Lexer.P_SUB;
import static neo.idlib.Text.Token.TT_BINARY;
import static neo.idlib.Text.Token.TT_DECIMAL;
import static neo.idlib.Text.Token.TT_FLOAT;
import static neo.idlib.Text.Token.TT_HEX;
import static neo.idlib.Text.Token.TT_INTEGER;
import static neo.idlib.Text.Token.TT_LITERAL;
import static neo.idlib.Text.Token.TT_LONG;
import static neo.idlib.Text.Token.TT_NAME;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.Text.Token.TT_OCTAL;
import static neo.idlib.Text.Token.TT_PUNCTUATION;
import static neo.idlib.Text.Token.TT_STRING;
import static neo.idlib.Text.Token.TT_UNSIGNED;
import static neo.idlib.Text.Token.TT_VALUESVALID;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Date;

import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Lexer.punctuation_t;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.math.Math_h.idMath;
import neo.sys.sys_public;

/**
 *
 */
public class Parser {

    static final int DEFINE_FIXED              = 0x0001;
    //
    static final int BUILTIN_LINE              = 1;
    static final int BUILTIN_FILE              = 2;
    static final int BUILTIN_DATE              = 3;
    static final int BUILTIN_TIME              = 4;
    static final int BUILTIN_STDC              = 5;
    //
    static final int INDENT_IF                 = 0x0001;
    static final int INDENT_ELSE               = 0x0002;
    static final int INDENT_ELIF               = 0x0004;
    static final int INDENT_IFDEF              = 0x0008;
    static final int INDENT_IFNDEF             = 0x0010;
    //
//    
//    
    static final int MAX_DEFINEPARMS           = 128;
    static final int DEFINEHASHSIZE            = 2048;
    //
    static final int TOKEN_FL_RECURSIVE_DEFINE = 1;
//
//    static define_t[] globaldefines;
//    
//    

    // macro definitions
    static class define_s {

        String   name;                        // define name
        int      flags;                       // define flags
        int      builtin;                     // > 0 if builtin define
        int      numparms;                    // number of define parameters
        idToken  parms;                       // define parameters
        idToken  tokens;                      // macro tokens (possibly containing parm tokens)
        define_s next;                        // next defined macro in a list
        define_s hashnext;                    // next define in the hash chain
    }

    // indents used for conditional compilation directives:
// #if, #else, #elif, #ifdef, #ifndef
    static class indent_s {

        int      type;                        // indent type
        int      skip;                        // true if skipping current indent
        idLexer  script;                      // script the indent was in
        indent_s next;                        // next indent on the indent stack
    }

    public static class idParser {

        private        boolean         loaded;                  // set when a source file is loaded from file or memory
        private        idStr           filename;                // file name of the script
        private        idStr           includepath;             // path to include files
        private        boolean         OSPath;                  // true if the file was loaded from an OS path
        private        punctuation_t[] punctuations;            // punctuations to use
        private        int             flags;                   // flags used for script parsing
        private        idLexer         scriptstack;             // stack with scripts of the source
        private        idToken         tokens;                  // tokens to read first
        private        define_s[]      defines;                 // list with macro definitions
        private        define_s[]      definehash;              // hash chain with defines
        private        indent_s        indentstack;             // stack with indents
        private        int             skip;                    // > 0 if skipping conditional code
        private        String          marker_p;
        //
        private static define_s        globaldefines;           // list with global defines added to every source loaded
        //
        //

        // constructor
        public idParser() {
            this.loaded = false;
            this.OSPath = false;
            this.punctuations = null;
            this.flags = 0;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
        }

        public idParser(int flags) {
            this.loaded = false;
            this.OSPath = false;
            this.punctuations = null;
            this.flags = flags;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
        }

        public idParser(final String filename) throws idException {
            this.loaded = false;
            this.OSPath = true;
            this.punctuations = null;
            this.flags = 0;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
            LoadFile(filename, false);
        }

        public idParser(final String filename, int flags) throws idException {
            this.loaded = false;
            this.OSPath = true;
            this.punctuations = null;
            this.flags = flags;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
            LoadFile(filename, false);
        }

        public idParser(final String filename, int flags, boolean OSPath) throws idException {
            this.loaded = false;
            this.OSPath = true;
            this.punctuations = null;
            this.flags = flags;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
            LoadFile(filename, OSPath);
        }

        public idParser(final String ptr, int length, final String name) throws idException {
            this.loaded = false;
            this.OSPath = true;
            this.punctuations = null;
            this.flags = 0;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
            LoadMemory(ptr, length, name);
        }

        public idParser(final String ptr, int length, final String name, int flags) throws idException {
            this.loaded = false;
            this.OSPath = true;
            this.punctuations = null;
            this.flags = flags;
            this.scriptstack = null;
            this.indentstack = null;
            this.definehash = null;
            this.defines = null;
            this.tokens = null;
            this.marker_p = null;
            LoadMemory(ptr, length, name);
        }
//					// destructor
//public					~idParser();

        public boolean LoadFile(final String filename) throws idException {
            return LoadFile(filename, false);
        }

        public boolean LoadFile(final idStr filename) throws idException {
            return LoadFile(filename.getData());
        }

        // load a source file
        public boolean LoadFile(final String filename, boolean OSPath) throws idException {
            idLexer script;

            if (this.loaded) {
                idLib.common.FatalError("idParser::loadFile: another source already loaded");
                return false;
            }
            script = new idLexer(filename, 0, OSPath);
            if (!script.IsLoaded()) {
//		delete script;
                script = null;
                return false;
            }
            script.SetFlags(this.flags);
            script.SetPunctuations(this.punctuations);
            script.next = null;
            this.OSPath = OSPath;
            this.filename = new idStr(filename);
            this.scriptstack = script;
            this.tokens = null;
            this.indentstack = null;
            this.skip = 0;
            this.loaded = true;

            if (null == this.definehash) {
                this.defines = null;
                this.definehash = new define_s[DEFINEHASHSIZE];// Mem_ClearedAlloc(DEFINEHASHSIZE);
                this.AddGlobalDefinesToSource();
            }
            return true;
        }

        // load a source from the given memory with the given length
        // NOTE: the ptr is expected to point at a valid C string: ptr[length] == '\0'
        public boolean LoadMemory(final CharBuffer ptr, int length, final String name) throws idException {
            idLexer script;

            if (this.loaded) {
                idLib.common.FatalError("idParser.loadMemory: another source already loaded");
                return false;
            }
            script = new idLexer(ptr, length, name);
            if (!script.IsLoaded()) {
//		delete script;
                return false;
            }
            script.SetFlags(this.flags);
            script.SetPunctuations(this.punctuations);
            script.next = null;
            this.filename = new idStr(name);
            this.scriptstack = script;
            this.tokens = null;
            this.indentstack = null;
            this.skip = 0;
            this.loaded = true;

            if (null == this.definehash) {
                this.defines = null;
                this.definehash = new define_s[DEFINEHASHSIZE];// Mem_ClearedAlloc(DEFINEHASHSIZE);
                this.AddGlobalDefinesToSource();
            }
            return true;
        }

        public boolean LoadMemory(final String ptr, int length, final String name) throws idException {
            return LoadMemory(atocb(ptr), length, name);
        }

        // free the current source
        public void FreeSource() {
            this.FreeSource(false);
        }

        public void FreeSource(boolean keepDefines /*= false*/) {
            idLexer script;
            idToken token;
            define_s define;
            indent_s indent;
            int i;

            // free all the scripts
            while (this.scriptstack != null) {
                script = this.scriptstack;
                this.scriptstack = this.scriptstack.next;
//		delete script;
            }
            // free all the tokens
            while (this.tokens != null) {
                token = this.tokens;
                this.tokens = this.tokens.next;
//		delete token;
            }
            // free all indents
            while (this.indentstack != null) {
                indent = this.indentstack;
                this.indentstack = this.indentstack.next;
//                Mem_Free(indent);
            }
            if (!keepDefines) {
                // free hash table
                if (this.definehash != null) {
                    // free defines
                    for (i = 0; i < DEFINEHASHSIZE; i++) {
                        while (this.definehash[i] != null) {
                            define = this.definehash[i];
                            this.definehash[i] = this.definehash[i].hashnext;
                            FreeDefine(define);
                        }
                    }
                    this.defines = null;
//                    Mem_Free(this.definehash);
                    this.definehash = null;
                }
            }
            this.loaded = false;
        }

        // returns true if a source is loaded
        public boolean IsLoaded() {
            return this.loaded;
        }

        // read a token from the source
        public boolean ReadToken(idToken token) throws idException {
            define_s define;

            while (true) {
                if (!this.ReadSourceToken(token)) {
                    return false;
                }
                // check for precompiler directives
                if ((token.type == TT_PUNCTUATION)
                        && ((token.oGet(0) == '#') && ((token.Length() == 1) || (token.oGet(1) == '\0')))) {
                    // read the precompiler directive
                    if (!this.ReadDirective()) {
                        return false;
                    }
                    continue;
                }
                // if skipping source because of conditional compilation
                if (this.skip != 0) {
                    continue;
                }
                // recursively concatenate strings that are behind each other still resolving defines
                if ((token.type == TT_STRING) && (NOT(this.scriptstack.GetFlags() & LEXFL_NOSTRINGCONCAT))) {
                    final idToken newtoken = new idToken();
                    if (this.ReadToken(newtoken)) {
                        if (newtoken.type == TT_STRING) {
                            token.Append(newtoken.c_str());
                        } else {
                            this.UnreadSourceToken(newtoken);
                        }
                    }
                }
                //
                if (0 == (this.scriptstack.GetFlags() & LEXFL_NODOLLARPRECOMPILE)) {
                    // check for special precompiler directives
                    if ((token.type == TT_PUNCTUATION)
                            && ((token.oGet(0) == '$') && ((token.Length() == 1) || (token.oGet(1) == '\0')))) {
                        // read the precompiler directive
                        if (this.ReadDollarDirective()) {
                            continue;
                        }
                    }
                }
                // if the token is a name
                if ((token.type == TT_NAME) && (0 == (token.flags & TOKEN_FL_RECURSIVE_DEFINE))) {
                    // check if the name is a define macro
                    define = FindHashedDefine(this.definehash, token.getData());
                    // if it is a define macro
                    if (define != null) {
                        // expand the defined macro
                        if (!this.ExpandDefineIntoSource(token, define)) {
                            return false;
                        }
                        continue;
                    }
                }
                // found a token
                return true;
            }
        }

        // expect a certain token, reads the token when available
        public boolean ExpectTokenString(final String string) throws idException {
            final idToken token = new idToken();

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
        public boolean ExpectTokenType(int type, int subtype, idToken token) throws idException {
            String str;

            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected token");
                return false;
            }

            if (token.type != type) {
                switch (type) {
                    case TT_STRING:
                        str = "string";
                        break;
                    case TT_LITERAL:
                        str = "literal";
                        break;
                    case TT_NUMBER:
                        str = "number";
                        break;
                    case TT_NAME:
                        str = "name";
                        break;
                    case TT_PUNCTUATION:
                        str = "punctuation";
                        break;
                    default:
                        str = "unknown type";
                        break;
                }
                this.Error("expected a %s but found '%s'", str, token);
                return false;
            }
            if (token.type == TT_NUMBER) {
                if ((token.subtype & subtype) != subtype) {
//                    str.Clear();
                    str = "";
                    if ((subtype & TT_DECIMAL) != 0) {
                        str = "decimal ";
                    }
                    if ((subtype & TT_HEX) != 0) {
                        str = "hex ";
                    }
                    if ((subtype & TT_OCTAL) != 0) {
                        str = "octal ";
                    }
                    if ((subtype & TT_BINARY) != 0) {
                        str = "binary ";
                    }
                    if ((subtype & TT_UNSIGNED) != 0) {
                        str += "unsigned ";
                    }
                    if ((subtype & TT_LONG) != 0) {
                        str += "long ";
                    }
                    if ((subtype & TT_FLOAT) != 0) {
                        str += "float ";
                    }
                    if ((subtype & TT_INTEGER) != 0) {
                        str += "integer ";
                    }
                    str.trim();//StripTrailing(' ');
                    this.Error("expected %s but found '%s'", str.toCharArray(), token);
                    return false;
                }
            } else if (token.type == TT_PUNCTUATION) {
                if (subtype < 0) {
                    this.Error("BUG: wrong punctuation subtype");
                    return false;
                }
                if (token.subtype != subtype) {
                    this.Error("expected '%s' but found '%s'", this.scriptstack.GetPunctuationFromId(subtype), token.getData());
                    return false;
                }
            }
            return true;
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

        // returns true if the next token equals the given string and removes the token from the source
        public boolean CheckTokenString(final String string) throws idException {
            final idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }
            //if the token is available
            if (tok.equals(string)) {
                return true;
            }

            UnreadSourceToken(tok);
            return false;
        }

        // returns true if the next token equals the given type and removes the token from the source
        public boolean CheckTokenType(int type, int subtype, idToken token) throws idException {
            final idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }
            //if the type matches
            if ((tok.type == type) && ((tok.subtype & subtype) == subtype)) {
                token.oSet(tok);
                return true;
            }

            UnreadSourceToken(tok);
            return false;
        }

        // returns true if the next token equals the given string but does not remove the token from the source
        public boolean PeekTokenString(final String string) throws idException {
            final idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }

            UnreadSourceToken(tok);

            // if the token is available
            if (tok.equals(string)) {
                return true;
            }
            return false;
        }

        // returns true if the next token equals the given type but does not remove the token from the source
        public boolean PeekTokenType(int type, int subtype, idToken token) throws idException {
            final idToken tok = new idToken();

            if (!ReadToken(tok)) {
                return false;
            }

            UnreadSourceToken(tok);

            // if the type matches
            if ((tok.type == type) && ((tok.subtype & subtype) == subtype)) {
                token.oSet(tok);
                return true;
            }
            return false;
        }

        // skip tokens until the given token string is read
        public boolean SkipUntilString(final String string) throws idException {
            final idToken token = new idToken();

            while (this.ReadToken(token)) {
                if (token.equals(string)) {
                    return true;
                }
            }
            return false;
        }

        // skip the rest of the current line
        public boolean SkipRestOfLine() throws idException {
            final idToken token = new idToken();

            while (this.ReadToken(token)) {
                if (token.linesCrossed != 0) {
                    this.UnreadSourceToken(token);
                    return true;
                }
            }
            return false;
        }

        /*
         =================
         idParser::SkipBracedSection

         Skips until a matching close brace is found.
         Internal brace depths are properly skipped.
         =================
         */
        // skip the braced section
        public boolean SkipBracedSection(boolean parseFirstBrace/*= true*/) throws idException {
            final idToken token = new idToken();
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

        public boolean SkipBracedSection() throws idException {
            return SkipBracedSection(true);
        }

        /*
         =================
         idParser::ParseBracedSection

         The next token should be an open brace.
         Parses until a matching close brace is found.
         Internal brace depths are properly skipped.
         =================
         */
        // parse a braced section into a string
        public String ParseBracedSection(idStr out, int tabs/*= -1*/) throws idException {
            final idToken token = new idToken();
            int i, depth;
            boolean doTabs = false;
            if (tabs >= 0) {
                doTabs = true;
            }

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
                    out.Append("\r\n");
                }

                if (doTabs && (token.linesCrossed != 0)) {
                    i = tabs;
                    if (token.equals("}") && (i > 0)) {
                        i--;
                    }
                    while (i-- > 0) {
                        out.Append("\t");
                    }
                }
                if (token.type == TT_PUNCTUATION) {
                    if (token.equals("{")) {
                        depth++;
                        if (doTabs) {
                            tabs++;
                        }
                    } else if (token.equals("}")) {
                        depth--;
                        if (doTabs) {
                            tabs--;
                        }
                    }
                }

                if (token.type == TT_STRING) {
                    out.Append("\"" + token.getData() + "\"");
                } else {
                    out.Append(token);
                }
                out.Append(" ");
            } while (depth != 0);

            return out.getData();
        }

        /*
         =================
         idParser::ParseBracedSectionExact

         The next token should be an open brace.
         Parses until a matching close brace is found.
         Maintains the exact formating of the braced section

         * TODO:FIXME: what about precompilation ?
         =================
         */
        // parse a braced section into a string, maintaining indents and newlines
        public String ParseBracedSectionExact(idStr out, int tabs/*= -1*/) throws idException {
            return this.scriptstack.ParseBracedSectionExact(out, tabs);
        }

        // parse the rest of the line
        public String ParseRestOfLine(idStr out) throws idException {
            final idToken token = new idToken();

            out.Empty();
            while (this.ReadToken(token)) {
                if (token.linesCrossed != 0) {
                    this.UnreadSourceToken(token);
                    break;
                }
                if (out.Length() != 0) {
                    out.Append(" ");
                }
                out.Append(token);
            }
            return out.getData();
        }

        // unread the given token
        public void UnreadToken(idToken token) {
            this.UnreadSourceToken(token);
        }

        // read a token only if on the current line
        public boolean ReadTokenOnLine(idToken token) throws idException {
            final idToken tok = new idToken();

            if (!this.ReadToken(tok)) {
                return false;
            }
            // if no lines were crossed before this token
            if (0 == tok.linesCrossed) {
                token.oSet(tok);
                return true;
            }
            //
            this.UnreadSourceToken(tok);
            return false;
        }

        // read a signed integer
        public int ParseInt() throws idException {
            final idToken token = new idToken();

            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected integer");
                return 0;
            }
            if ((token.type == TT_PUNCTUATION) && token.equals("-")) {
                this.ExpectTokenType(TT_NUMBER, TT_INTEGER, token);
                return -token.GetIntValue();
            } else if ((token.type != TT_NUMBER) || (token.subtype == TT_FLOAT)) {
                this.Error("expected integer value, found '%s'", token);
            }
            return token.GetIntValue();
        }

        // read a boolean
        public boolean ParseBool() throws idException {
            final idToken token = new idToken();

            if (!this.ExpectTokenType(TT_NUMBER, 0, token)) {
                this.Error("couldn't read expected boolean");
                return false;
            }
            return (token.GetIntValue() != 0);
        }

        // read a floating point number
        public float ParseFloat() throws idException {
            final idToken token = new idToken();

            if (!this.ReadToken(token)) {
                this.Error("couldn't read expected floating point number");
                return 0.0f;
            }
            if ((token.type == TT_PUNCTUATION) && token.equals("-")) {
                this.ExpectTokenType(TT_NUMBER, 0, token);
                return -token.GetFloatValue();
            } else if (token.type != TT_NUMBER) {
                this.Error("expected float value, found '%s'", token);
            }
            return token.GetFloatValue();
        }

        // parse matrices with floats
        public boolean Parse1DMatrix(int x, float[] m) throws idException {
            int i;

            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (i = 0; i < x; i++) {
                m[i] = this.ParseFloat();
            }

            if (!this.ExpectTokenString(")")) {
                return false;
            }
            return true;
        }

        public boolean Parse2DMatrix(int y, int x, float[] m) throws idException {
            int i;

            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (i = 0; i < y; i++) {
                final float[] tempM = new float[m.length - (i * x)];
                System.arraycopy(m, i * x, tempM, 0, tempM.length);
                if (!this.Parse1DMatrix(x, tempM)) {
                    System.arraycopy(tempM, 0, m, i * x, tempM.length);
                    return false;
                }
                System.arraycopy(tempM, 0, m, i * x, tempM.length);
            }

            if (!this.ExpectTokenString(")")) {
                return false;
            }
            return true;
        }

        public boolean Parse3DMatrix(int z, int y, int x, float[] m) throws idException {
            int i;

            if (!this.ExpectTokenString("(")) {
                return false;
            }

            for (i = 0; i < z; i++) {
                final float[] tempM = new float[m.length - (i * x * y)];
                System.arraycopy(m, i * x * y, tempM, 0, tempM.length);
                if (!this.Parse2DMatrix(y, x, tempM)) {
                    System.arraycopy(tempM, 0, m, i * x * y, tempM.length);
                    return false;
                }
                System.arraycopy(tempM, 0, m, i * x * y, tempM.length);
            }

            if (!this.ExpectTokenString(")")) {
                return false;
            }
            return true;
        }

        // get the white space before the last read token
        public int GetLastWhiteSpace(idStr whiteSpace) {
            if (this.scriptstack != null) {
                this.scriptstack.GetLastWhiteSpace(whiteSpace);
            } else {
                whiteSpace.Clear();
            }
            return whiteSpace.Length();
        }

        // Set a marker in the source file (there is only one marker)
        public void SetMarker() {
            this.marker_p = null;
        }

        /*
         ================
         idParser::GetStringFromMarker

         * TODO:FIXME: this is very bad code, the script isn't even garrenteed to still be around
         ================
         */
        // Get the string from the marker to the current position
        public void GetStringFromMarker(idStr out, boolean clean/*= false*/) throws idException {
            final int p;//marker
//            int save;

            if (this.marker_p == null) {
                this.marker_p = this.scriptstack.buffer.toString();
            }

            if (this.tokens != null) {
                p = this.tokens.whiteSpaceStart_p;
            } else {
                p = this.scriptstack.script_p;
            }

            // Set the end character to NULL to give us a complete string
//            save = p;
//            p = 0;
            // If cleaning then reparse
            if (clean) {
                final idParser temp = new idParser(this.marker_p, p, "temp", this.flags);//TODO:check whether this substringing works
                final idToken token = new idToken();
                while (temp.ReadToken(token)) {
                    out.oPluSet(token);
                }
            } else {
                out.oSet(this.marker_p);
            }

            // restore the character we set to NULL
//            p = save;
        }

        // add a define to the source
        public boolean AddDefine(final String string) throws idException {
            define_s define;

            define = DefineFromString(string);
            if (null == define) {
                return false;
            }
            AddDefineToHash(define, this.definehash);
            return true;
        }

        // add builtin defines
        public void AddBuiltinDefines() {
            int i;
            define_s define;
            class builtin {

                builtin(String string, int id) {
                    this.string = string;
                    this.id = id;
                }
                String string;
                int id;
            }
            final builtin[] builtin = {
                new builtin("__LINE__", BUILTIN_LINE),
                new builtin("__FILE__", BUILTIN_DATE),
                new builtin("__TIME__", BUILTIN_TIME),
                new builtin("__STDC__", BUILTIN_STDC),
                new builtin(null, 0)
            };

            for (i = 0; builtin[i].string != null; i++) {
//		define = (define_t *) Mem_Alloc(sizeof(define_t) + strlen(builtin[i].string) + 1);
                define = new define_s();
                define.name = builtin[i].string;
//		strcpy(define.name, builtin[i].string);
                define.flags = DEFINE_FIXED;
                define.builtin = builtin[i].id;
                define.numparms = 0;
                define.parms = null;
                define.tokens = null;
                // add the define to the source
                AddDefineToHash(define, this.definehash);
            }
        }

        // set the source include path
        public void SetIncludePath(final String path) {
            this.includepath = new idStr(path);
            // add trailing path seperator
            if ((this.includepath.oGet(this.includepath.Length() - 1) != '\\')
                    && (this.includepath.oGet(this.includepath.Length() - 1) != '/')) {
                this.includepath.Append(sys_public.PATHSEPERATOR_STR);
            }
        }

        // set the punctuation set
        public void SetPunctuations(final punctuation_t[] p) {
            this.punctuations = p;
        }

        // returns a pointer to the punctuation with the given id
        public String GetPunctuationFromId(int id) {
            int i;

            if (null == this.punctuations) {
                final idLexer lex = new idLexer();
                return lex.GetPunctuationFromId(id);
            }

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

            if (null == this.punctuations) {
                final idLexer lex = new idLexer();
                return lex.GetPunctuationId(p);
            }

            for (i = 0; this.punctuations[i].p != null; i++) {
                if (this.punctuations[i].p.equals(p)) {
                    return this.punctuations[i].n;
                }
            }
            return 0;
        }

        // set lexer flags
        public void SetFlags(int flags) {
            idLexer s;

            this.flags = flags;
            for (s = this.scriptstack; s != null; s = s.next) {
                s.SetFlags(flags);
            }
        }

        // get lexer flags
        public int GetFlags() {
            return this.flags;
        }

        // returns the current filename
        public idStr GetFileName() {
            if (this.scriptstack != null) {
                return this.scriptstack.GetFileName();
            } else {
                return null;
            }
        }

        // get current offset in current script
        public int GetFileOffset() {
            if (this.scriptstack != null) {
                return this.scriptstack.GetFileOffset();
            } else {
                return 0;
            }
        }

        // get file time for current script
        public long/*ID_TIME_T*/ GetFileTime() {
            if (this.scriptstack != null) {
                return this.scriptstack.GetFileTime();
            } else {
                return 0;
            }
        }

        // returns the current line number
        public int GetLineNum() {
            if (this.scriptstack != null) {
                return this.scriptstack.GetLineNum();
            } else {
                return 0;
            }
        }

        // print an error message
        public void Error(final String fmt, Object... args) throws idException {
//	char text[MAX_STRING_CHARS];
//            char text[MAX_STRING_CHARS];
//            va_list ap;
//
//            va_start(ap, str);
//            vsprintf(text, str, ap);
//            va_end(ap);

            if (this.scriptstack != null) {
                final String text = String.format(fmt, args);
                this.scriptstack.Error(text);
            }
        }

        @Deprecated
        public void Error(final String str, final char[] chr, final char[]   ... chrs) throws idException {
            this.Error(str);
            this.Error(ctos(chr));
            for (final char[] charoal : chrs) {
                this.Error(ctos(charoal));
            }
        }

        // print a warning message
        public void Warning(final String fmt, Object... args) throws idException {
//            char text[MAX_STRING_CHARS];
//            va_list ap;
//
//            va_start(ap, str);
//            vsprintf(text, str, ap);
//            va_end(ap);
            if (this.scriptstack != null) {
                final String text = String.format(fmt, args);
                this.scriptstack.Warning(text);
            }
        }

        @Deprecated
        public void Warning(final String str, final char[] chr, final char[]   ... chrs) throws idException {
            this.Warning(str);
            this.Warning(ctos(chr));
            for (final char[] charoal : chrs) {
                this.Warning(ctos(charoal));
            }
        }

        // add a global define that will be added to all opened sources
        public static boolean AddGlobalDefine(final String string) throws idException {
            define_s define;

            define = idParser.DefineFromString(string);
            if (null == define) {
                return false;
            }
            define.next = globaldefines;//TODO:check if [0] is correcto.
            globaldefines = define;
            return true;
        }

        // remove the given global define
        public static boolean RemoveGlobalDefine(final String name) {
            define_s d, prev;

            for (prev = null, d = globaldefines; d != null; prev = d, d = d.next) {
                if (d.name.equals(name)) {
                    break;
                }
            }
            if (d != null) {
                if (prev != null) {
                    prev.next = d.next;
                } else {
                    globaldefines = d.next;
                }
                idParser.FreeDefine(d);
                return true;
            }
            return false;
        }

        // remove all global defines
        public static void RemoveAllGlobalDefines() {
            define_s define;

            for (define = globaldefines; define != null; define = globaldefines) {
                globaldefines = globaldefines.next;//TODO:ptr
                idParser.FreeDefine(define);
            }
        }

        // set the base folder to load files from
        public static void SetBaseFolder(final String path) {
            idLexer.SetBaseFolder(path);
        }

        private void PushIndent(int type, int skip) {
            indent_s indent;

//	indent = (indent_t *) Mem_Alloc(sizeof(indent_t));
            indent = new indent_s();
            indent.type = type;
            indent.script = this.scriptstack;
            indent.skip = (skip != 0) ? 1 : 0;//TODO:booleanize?
            this.skip += indent.skip;
            indent.next = this.indentstack;
            this.indentstack = indent;
        }

        private void PopIndent(int[] type, int[] skip) {
            indent_s indent;

            type[0] = 0;
            skip[0] = 0;

            indent = this.indentstack;
            if (null == indent) {
                return;
            }

            // must be an indent from the current script
            if (this.indentstack.script != this.scriptstack) {
                return;
            }

            type[0] = indent.type;
            skip[0] = indent.skip;
            this.indentstack = this.indentstack.next;
            this.skip -= indent.skip;
//	Mem_Free( indent );
        }

        private void PushScript(idLexer script) throws idException {
            idLexer s;

            for (s = this.scriptstack; s != null; s = s.next) {
                if (0 == idStr.Icmp(s.GetFileName(), script.GetFileName())) {
                    this.Warning("'%s' recursively included", script.GetFileName());
                    return;
                }
            }
            //push the script on the script stack
            script.next = this.scriptstack;
            this.scriptstack = script;
        }

        private boolean ReadSourceToken(idToken token) throws idException {
            idToken t;
            idLexer script;
            final int[] type = {0}, skip = {0};
            int changedScript;

            if (NOT(this.scriptstack)) {
                idLib.common.FatalError("idParser::ReadSourceToken: not loaded");
                return false;
            }
            changedScript = 0;
            // if there's no token already available
            while (NOT(this.tokens)) {
                // if there's a token to read from the script
                if (this.scriptstack.ReadToken(token)) {
                    token.linesCrossed += changedScript;

                    // set the marker based on the start of the token read in
                    if (isNotNullOrEmpty(this.marker_p)) {
                        this.marker_p = "";//token.whiteSpaceEnd_p;//TODO:does marker_p do anythning???
                    }
                    return true;
                }
                // if at the end of the script
                if (this.scriptstack.EndOfFile()) {
                    // remove all indents of the script
                    while ((this.indentstack != null) && (this.indentstack.script == this.scriptstack)) {
                        this.Warning("missing #endif");
                        this.PopIndent(type, skip);
                    }
                    changedScript = 1;
                }
                // if this was the initial script
                if (NOT(this.scriptstack.next)) {
                    return false;
                }
                // remove the script and return to the previous one
                script = this.scriptstack;
                this.scriptstack = this.scriptstack.next;
//		delete script;
            }
            // copy the already available token
            token.oSet(this.tokens);
            // remove the token from the source
            t = this.tokens;
            this.tokens = this.tokens.next;
//	delete t;
            return true;
        }

        /*
         ================
         idParser::ReadLine

         reads a token from the current line, continues reading on the next
         line only if a backslash '\' is found
         ================
         */
        private boolean ReadLine(idToken token) throws idException {
            int crossline;

            crossline = 0;
            do {
                if (!this.ReadSourceToken(token)) {
                    return false;
                }

                if (token.linesCrossed > crossline) {
                    this.UnreadSourceToken(token);
                    return false;
                }
                crossline = 1;
            } while (token.equals("\\"));
            return true;
        }

        private boolean UnreadSourceToken(idToken token) {
            idToken t;

            t = new idToken(token);
            t.next = this.tokens;
            this.tokens = t;
            return true;
        }

        private boolean ReadDefineParms(define_s define, idToken[] parms, int maxparms) throws idException {
            define_s newdefine;
            final idToken token = new idToken();
            idToken t, last;
            int i, done, lastcomma, numparms, indent;

            if (!this.ReadSourceToken(token)) {
                this.Error("define '%s' missing parameters", define.name);
                return false;
            }

            if (define.numparms > maxparms) {
                this.Error("define with more than %d parameters", "" + maxparms);
                return false;
            }

            for (i = 0; i < define.numparms; i++) {
                parms[i] = null;
            }
            // if no leading "("
            if (!token.equals("(")) {
                this.UnreadSourceToken(token);
                this.Error("define '%s' missing parameters", define.name);
                return false;
            }
            // read the define parameters
            for (done = 0, numparms = 0, indent = 1; 0 == done;) {
                if (numparms >= maxparms) {
                    this.Error("define '%s' with too many parameters", define.name);
                    return false;
                }
                parms[numparms] = null;
                lastcomma = 1;
                last = null;
                while (0 == done) {

                    if (!this.ReadSourceToken(token)) {
                        this.Error("define '%s' incomplete", define.name);
                        return false;
                    }

                    if (token.equals(",")) {
                        if (indent <= 1) {
                            if (lastcomma != 0) {
                                this.Warning("too many comma's");
                            }
                            if (numparms >= define.numparms) {
                                this.Warning("too many define parameters");
                            }
                            lastcomma = 1;
                            break;
                        }
                    } else if (token.equals("(")) {
                        indent++;
                    } else if (token.equals(")")) {
                        indent--;
                        if (indent <= 0) {
                            if (null == parms[define.numparms - 1]) {
                                this.Warning("too few define parameters");
                            }
                            done = 1;
                            break;
                        }
                    } else if (token.type == TT_NAME) {
                        newdefine = FindHashedDefine(this.definehash, token.getData());
                        if (newdefine != null) {
                            if (!this.ExpandDefineIntoSource(token, newdefine)) {
                                return false;
                            }
                            continue;
                        }
                    }

                    lastcomma = 0;

                    if (numparms < define.numparms) {

                        t = new idToken(token);
                        t.next = null;
                        if (last != null) {
                            last.next = t;
                        } else {
                            parms[numparms] = t;
                        }
                        last = t;
                    }
                }
                numparms++;
            }
            return true;
        }

        private boolean StringizeTokens(idToken[] tokens, idToken token) {
            idToken t;

            token.type = TT_STRING;
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
//	(*token) = "";
            for (t = tokens[0]; t != null; t = t.next) {//TODO:check if tokens[0] should be used.
                token.Append(t.getData());
            }
            return true;
        }

        private boolean MergeTokens(idToken t1, idToken t2) {
            // merging of a name with a name or number
            if ((t1.type == TT_NAME) && ((t2.type == TT_NAME) || ((t2.type == TT_NUMBER) && ((t2.subtype & TT_FLOAT) == 0)))) {
                t1.Append(t2.c_str());
                return true;
            }
            // merging of two strings
            if ((t1.type == TT_STRING) && (t2.type == TT_STRING)) {
                t1.Append(t2.c_str());
                return true;
            }
            // merging of two numbers
            if ((t1.type == TT_NUMBER) && (t2.type == TT_NUMBER)
                    && ((t1.subtype & (TT_HEX | TT_BINARY)) == 0)
                    && ((t2.subtype & (TT_HEX | TT_BINARY)) == 0)
                    && (((t1.subtype & TT_FLOAT) == 0)
                    || ((t2.subtype & TT_FLOAT) == 0))) {
                t1.Append(t2.c_str());
                return true;
            }

            return false;
        }

        private boolean ExpandBuiltinDefine(idToken defToken, define_s define, idToken[] firstToken, idToken[] lastToken) throws idException {
            idToken token;
            /*ID_TIME_T*/ final long t;
            String curtime;
            String buf;//[MAX_STRING_CHARS];

            token = new idToken(defToken);
            switch (define.builtin) {
                case BUILTIN_LINE: {
                    buf = String.format("%d", defToken.line);
                    token.oSet(buf);
                    token.intValue = defToken.line;
                    token.floatValue = defToken.line;
                    token.type = TT_NUMBER;
                    token.subtype = TT_DECIMAL | TT_INTEGER | TT_VALUESVALID;
                    token.line = defToken.line;
                    token.linesCrossed = defToken.linesCrossed;
                    token.flags = 0;
                    firstToken[0] = token;
                    lastToken[0] = token;
                    break;
                }
                case BUILTIN_FILE: {
                    token.oSet(this.scriptstack.GetFileName());
                    token.type = TT_NAME;
                    token.subtype = token.Length();
                    token.line = defToken.line;
                    token.linesCrossed = defToken.linesCrossed;
                    token.flags = 0;
                    firstToken[0] = token;
                    lastToken[0] = token;
                    break;
                }
                case BUILTIN_DATE: {
//                    t = System.currentTimeMillis();
//                    curtime = ctime( & t);
                    curtime = new Date().toString();
                    token.oSet("\"");
                    token.Append(curtime + 4);
                    token.oSet(7, '\0');
                    token.Append(curtime + 20);
                    token.oSet(10, '\0');
                    token.Append("\"");
//			free(curtime);
                    token.type = TT_STRING;
                    token.subtype = token.Length();
                    token.line = defToken.line;
                    token.linesCrossed = defToken.linesCrossed;
                    token.flags = 0;
                    firstToken[0] = token;
                    lastToken[0] = token;
                    break;
                }
                case BUILTIN_TIME: {
//                    t = System.currentTimeMillis();
//                    curtime = ctime( & t);
                    curtime = new Date().toString();
                    token.oSet("\"");
                    token.Append(curtime + 11);
                    token.oSet(8, '\0');
                    token.Append("\"");
//			free(curtime);
                    token.type = TT_STRING;
                    token.subtype = token.Length();
                    token.line = defToken.line;
                    token.linesCrossed = defToken.linesCrossed;
                    token.flags = 0;
                    firstToken[0] = token;
                    lastToken[0] = token;
                    break;
                }
                case BUILTIN_STDC: {
                    this.Warning("__STDC__ not supported\n");
//			firsttoken = null;
//			lasttoken = null;
//			break;
                }
                default: {
                    firstToken[0] = null;
                    lastToken[0] = null;
                    break;
                }
            }
            return true;
        }

        private boolean ExpandDefine(idToken deftoken, define_s define, idToken[] firstToken, idToken[] lastToken) throws idException {
            final idToken[] parms = new idToken[MAX_DEFINEPARMS];
            idToken dt, pt, t;
            idToken t1, t2, first, last, nextpt;
			final idToken token = new idToken();
            int parmnum, i;

            // if it is a builtin define
            if (define.builtin != 0) {
                return this.ExpandBuiltinDefine(deftoken, define, firstToken, lastToken);
            }
            // if the define has parameters
            if (define.numparms != 0) {
                if (!this.ReadDefineParms(define, parms, MAX_DEFINEPARMS)) {
                    return false;
                }
//#ifdef DEBUG_EVAL
//		for ( i = 0; i < define.numparms; i++ ) {
//			Log_Write("define parms %d:", i);
//			for ( pt = parms[i]; pt; pt = pt.next ) {
//				Log_Write( "%s", pt.c_str() );
//			}
//		}
//#endif //DEBUG_EVAL
            }
            // empty list at first
            first = null;
            last = null;
            // create a list with tokens of the expanded define
            for (dt = define.tokens; dt != null; dt = dt.next) {
                parmnum = -1;
                // if the token is a name, it could be a define parameter
                if (dt.type == TT_NAME) {
                    parmnum = FindDefineParm(define, dt.getData());
                }
                // if it is a define parameter
                if (parmnum >= 0) {
                    for (pt = parms[parmnum]; pt != null; pt = pt.next) {
                        t = new idToken(pt);
                        //add the token to the list
                        t.next = null;
                        if (last != null) {
                            last.next = t;
                        } else {
                            first = t;
                        }
                        last = t;
                    }
                } else {
                    // if stringizing operator
                    if (dt.equals("#")) {
                        // the stringizing operator must be followed by a define parameter
                        if (dt.next != null) {
                            parmnum = FindDefineParm(define, dt.next.getData());
                        } else {
                            parmnum = -1;
                        }

                        if (parmnum >= 0) {
                            // step over the stringizing operator
                            dt = dt.next;
                            // stringize the define parameter tokens
                            if (!this.StringizeTokens(Arrays.copyOfRange(parms, parmnum, parms.length), token)) {
                                this.Error("can't stringize tokens");
                                return false;
                            }
                            t = new idToken(token);
                            t.line = deftoken.line;
                        } else {
                            this.Warning("stringizing operator without define parameter");
                            continue;
                        }
                    } else {
                        t = new idToken(dt);
                        t.line = deftoken.line;
                    }
                    // add the token to the list
                    t.next = null;
// the token being read from the define list should use the line number of
// the original file, not the header file			
                    t.line = deftoken.line;

                    if (last != null) {
                        last.next = t;
                    } else {
                        first = t;
                    }
                    last = t;
                }
            }
            // check for the merging operator
            for (t = first; t != null;) {
                if (t.next != null) {
                    // if the merging operator
                    if (t.next.equals("##")) {
                        t1 = t;
                        t2 = t.next.next;
                        if (t2 != null) {
                            if (!this.MergeTokens(t1, t2)) {
                                this.Error("can't merge '%s' with '%s'", t1.getData(), t2.getData());
                                return false;
                            }
//					delete t1.next;
                            t1.next = t2.next;
                            if (t2 == last) {
                                last = t1;
                            }
//					delete t2;
                            continue;
                        }
                    }
                }
                t = t.next;
            }
            // store the first and last token of the list
            firstToken[0] = first;
            lastToken[0] = last;
            // free all the parameter tokens
            for (i = 0; i < define.numparms; i++) {
                for (pt = parms[i]; pt != null; pt = nextpt) {
                    nextpt = pt.next;
//			delete pt;
                }
            }

            return true;
        }

        private boolean ExpandDefineIntoSource(idToken deftoken, define_s define) throws idException {
            final idToken[] firstToken = {null}, lastToken = {null};

            if (!this.ExpandDefine(deftoken, define, firstToken, lastToken)) {
                return false;
            }
            // if the define is not empty
            if ((firstToken[0] != null) && (lastToken[0] != null)) {
                firstToken[0].linesCrossed += deftoken.linesCrossed;
                lastToken[0].next = this.tokens;
                this.tokens = firstToken[0];
            }
            return true;
        }

        private void AddGlobalDefinesToSource() {
            define_s define, newdefine;

            for (define = globaldefines; define != null; define = define.next) {//TODO:check if "define = globaldefines" is correct.
                newdefine = CopyDefine(define);
                AddDefineToHash(newdefine, this.definehash);
            }
        }

        private define_s CopyDefine(define_s define) {
            define_s newdefine;
            idToken token, newtoken, lasttoken;

//	newdefine = (define_t *) Mem_Alloc(sizeof(define_t) + strlen(define.name) + 1);
            newdefine = new define_s();
            //copy the define name
//	newdefine.name = (char *) newdefine + sizeof(define_t);
            newdefine.name = define.name;
            newdefine.flags = define.flags;
            newdefine.builtin = define.builtin;
            newdefine.numparms = define.numparms;
            //the define is not linked
            newdefine.next = null;
            newdefine.hashnext = null;
            //copy the define tokens
            newdefine.tokens = null;
            for (lasttoken = null, token = define.tokens; token != null; token = token.next) {
                newtoken = new idToken(token);
                newtoken.next = null;
                if (lasttoken != null) {
                    lasttoken.next = newtoken;
                } else {
                    newdefine.tokens = newtoken;
                }
                lasttoken = newtoken;
            }
            //copy the define parameters
            newdefine.parms = null;
            for (lasttoken = null, token = define.parms; token != null; token = token.next) {
                newtoken = new idToken(token);
                newtoken.next = null;
                if (lasttoken != null) {
                    lasttoken.next = newtoken;
                } else {
                    newdefine.parms = newtoken;
                }
                lasttoken = newtoken;
            }
            return newdefine;
        }

        private define_s FindHashedDefine(define_s[] definehash, final String name) {
            define_s d;
            int hash;

            hash = PC_NameHash(name);
            for (d = definehash[hash]; d != null; d = d.hashnext) {
                if (d.name.equals(name)) {
                    return d;
                }
            }
            return null;
        }

        private int FindDefineParm(define_s define, final String name) {
            idToken p;
            int i;

            i = 0;
            for (p = define.parms; p != null; p = p.next) {
                if (p.equals(name)) {
                    return i;
                }
                i++;
            }
            return -1;
        }

        private void AddDefineToHash(define_s define, define_s[] definehash) {
            int hash;

            hash = PC_NameHash(define.name);
            define.hashnext = definehash[hash];
            definehash[hash] = define;
        }

        private static void PrintDefine(define_s define) throws idException {
            idLib.common.Printf("define->name = %s\n", define.name);
            idLib.common.Printf("define->flags = %d\n", define.flags);
            idLib.common.Printf("define->builtin = %d\n", define.builtin);
            idLib.common.Printf("define->numparms = %d\n", define.numparms);
        }

        private static void FreeDefine(define_s define) {
            final idToken t, next;

            //free the define parameters
//            for (t = define.parms; t; t = next) {
//                next = t.next;
//		delete t;
//            }
            //free the define tokens
//            for (t = define.tokens; t; t = next) {
//                next = t.next;
//		delete t;
//            }
            define.parms = define.tokens = null;//TODO:check if nullifying doesn't break nothing.
            //free the define
//            Mem_Free(define);
        }

        private define_s FindDefine(define_s defines, final String name) {
            define_s d;

            for (d = defines; d != null; d = d.next) {
                if (d.name.equals(name)) {
                    return d;
                }
            }
            return null;
        }

        private static define_s DefineFromString(final String string) throws idException {
            final idParser src = new idParser();
            define_s def;

            if (!src.LoadMemory(string, string.length(), "*defineString")) {
                return null;
            }
            // create a define from the source
            if (!src.Directive_define()) {
                src.FreeSource();
                return null;
            }
            def = src.CopyFirstDefine();
            src.FreeSource();
            //if the define was created succesfully
            return def;
        }

        private define_s CopyFirstDefine() {
            int i;

            for (i = 0; i < DEFINEHASHSIZE; i++) {
                if (this.definehash[i] != null) {
                    return CopyDefine(this.definehash[i]);
                }
            }
            return null;
        }

        private boolean Directive_include() throws Lib.idException {
            idLexer script;
            final idToken token = new idToken();
            final idStr path = new idStr();

            if (!this.ReadSourceToken(token)) {
                this.Error("#include without file name");
                return false;
            }
            if (token.linesCrossed > 0) {
                this.Error("#include without file name");
                return false;
            }
            if (token.type == TT_STRING) {
                script = new idLexer();
                // try relative to the current file
                path.oSet(this.scriptstack.GetFileName());
                path.StripFilename();
                path.oPluSet("/");
                path.oPluSet(token);
                if (!script.LoadFile(path.getData(), this.OSPath)) {
                    // try absolute path
                    path.oSet(token);
                    if (!script.LoadFile(path.getData(), this.OSPath)) {
                        // try from the include path
                        path.oSet(this.includepath.oPlus(token));
                        if (!script.LoadFile(path.getData(), this.OSPath)) {
//					delete script;
                            script = null;
                        }
                    }
                }
            } else if ((token.type == TT_PUNCTUATION) && token.equals("<")) {
                path.oSet(this.includepath);
                while (this.ReadSourceToken(token)) {
                    if (token.linesCrossed > 0) {
                        this.UnreadSourceToken(token);
                        break;
                    }
                    if ((token.type == TT_PUNCTUATION) && token.equals(">")) {
                        break;
                    }
                    path.oPluSet(token);
                }
                if (!token.equals(">")) {
                    this.Warning("#include missing trailing >");
                }
                if (0 == path.Length()) {
                    this.Error("#include without file name between < >");
                    return false;
                }
                if ((this.flags & LEXFL_NOBASEINCLUDES) != 0) {
                    return true;
                }
                script = new idLexer();
                if (!script.LoadFile(this.includepath.oPlus(path).getData(), this.OSPath)) {
//			delete script;
                    script = null;
                }
            } else {
                this.Error("#include without file name");
                return false;
            }
            if (null == script) {
                this.Error("file '%s' not found", path);
                return false;
            }
            script.SetFlags(this.flags);
            script.SetPunctuations(this.punctuations);
            this.PushScript(script);
            return true;
        }

        private boolean Directive_undef() throws idException {
            final idToken token = new idToken();
            define_s define, lastdefine;
            int hash;

            //
            if (!this.ReadLine(token)) {
                this.Error("undef without name");
                return false;
            }
            if (token.type != TT_NAME) {
                this.UnreadSourceToken(token);
                this.Error("expected name but found '%s'", token);
                return false;
            }

            hash = PC_NameHash(token.c_str());
            for (lastdefine = null, define = this.definehash[hash]; define != null; define = define.hashnext) {
                if (token.equals(define.name)) {
                    if ((define.flags & DEFINE_FIXED) != 0) {
                        this.Warning("can't undef '%s'", token);
                    } else {
                        if (lastdefine != null) {
                            lastdefine.hashnext = define.hashnext;
                        } else {
                            this.definehash[hash] = define.hashnext;
                        }
                        FreeDefine(define);
                    }
                    break;
                }
                lastdefine = define;
            }
            return true;
        }

        private boolean Directive_if_def(int type) throws idException {
            final idToken token = new idToken();
            define_s d;
            int skip;

            if (!this.ReadLine(token)) {
                this.Error("#ifdef without name");
                return false;
            }
            if (token.type != TT_NAME) {
                this.UnreadSourceToken(token);
                this.Error("expected name after #ifdef, found '%s'", token);
                return false;
            }
            d = FindHashedDefine(this.definehash, token.getData());
            skip = ((type == INDENT_IFDEF) == (d == null)) ? 1 : 0;
            this.PushIndent(type, skip);
            return true;
        }

        private boolean Directive_ifdef() throws idException {
            return this.Directive_if_def(INDENT_IFDEF);
        }

        private boolean Directive_ifndef() throws idException {
            return this.Directive_if_def(INDENT_IFNDEF);
        }

        private boolean Directive_else() throws idException {
            final int[] type = new int[1], skip = new int[1];

            this.PopIndent(type, skip);
            if (0 == type[0]) {
                this.Error("misplaced #else");
                return false;
            }
            if (type[0] == INDENT_ELSE) {
                this.Error("#else after #else");
                return false;
            }
            this.PushIndent(INDENT_ELSE, skip[0] == 0 ? 1 : 0);
            return true;
        }

        private boolean Directive_endif() throws idException {
            final int[] type = new int[1], skip = new int[1];

            this.PopIndent(type, skip);
            if (0 == type[0]) {
                this.Error("misplaced #endif");
                return false;
            }
            return true;
        }

        /*
         ================
         idParser::EvaluateTokens
         ================
         */
        class operator_s {

            int op;
            int priority;
            int parentheses;
            operator_s prev, next;
        }

        class value_s {

            long intValue;
            double floatValue;
            int parentheses;
            value_s prev, next;
        }

        int PC_OperatorPriority(int op) {
            switch (op) {
                case P_MUL:
                    return 15;
                case P_DIV:
                    return 15;
                case P_MOD:
                    return 15;
                case P_ADD:
                    return 14;
                case P_SUB:
                    return 14;

                case P_LOGIC_AND:
                    return 7;
                case P_LOGIC_OR:
                    return 6;
                case P_LOGIC_GEQ:
                    return 12;
                case P_LOGIC_LEQ:
                    return 12;
                case P_LOGIC_EQ:
                    return 11;
                case P_LOGIC_UNEQ:
                    return 11;

                case P_LOGIC_NOT:
                    return 16;
                case P_LOGIC_GREATER:
                    return 12;
                case P_LOGIC_LESS:
                    return 12;

                case P_RSHIFT:
                    return 13;
                case P_LSHIFT:
                    return 13;

                case P_BIN_AND:
                    return 10;
                case P_BIN_OR:
                    return 8;
                case P_BIN_XOR:
                    return 9;
                case P_BIN_NOT:
                    return 16;

                case P_COLON:
                    return 5;
                case P_QUESTIONMARK:
                    return 5;
            }
            return 0;
        }
        static final int MAX_VALUES = 64;
        static final int MAX_OPERATORS = 64;

        boolean AllocValue(value_s val, value_s[] value_heap, int[] numvalues) throws idException {
            boolean error = false;
            if (numvalues[0] >= MAX_VALUES) {
                this.Error("out of value space\n");
                error = true;
            } else {
                val = value_heap[numvalues[0]++];
            }
            return error;
        }

        boolean AllocOperator(operator_s op, operator_s[] operator_heap, int[] numoperators) throws idException {
            boolean error = false;
            if (numoperators[0] >= MAX_OPERATORS) {
                this.Error("out of operator space\n");
                error = true;
            } else {
                op = operator_heap[numoperators[0]++];
            }
            return error;
        }

        private boolean EvaluateTokens(idToken tokens, long[] intValue, double[] floatValue, int integer) throws idException {
            operator_s o = new operator_s(), firstOperator, lastOperator;
            value_s v = new value_s(), firstValue, lastValue, v1, v2;
            idToken t;
            boolean brace = false;
            int parentheses = 0;
            boolean error = false;
            boolean lastwasvalue = false;
            boolean negativevalue = false;
            boolean questmarkintvalue = false;
            double questmarkfloatvalue = 0;
            boolean gotquestmarkvalue = false;
            final boolean lastoperatortype = false;
            //
            final operator_s[] operator_heap = new operator_s[MAX_OPERATORS];
            final int[] numoperators = new int[1];
            final value_s[] value_heap = new value_s[MAX_VALUES];
            final int[] numvalues = new int[1];

            firstOperator = lastOperator = null;
            firstValue = lastValue = null;
            if (intValue[0] != 0) {
                intValue[0] = 0;
            }
            if (floatValue[0] != 0) {
                floatValue[0] = 0;
            }
            for (t = tokens; t != null; t = t.next) {
                switch (t.type) {
                    case TT_NAME: {
                        if (lastwasvalue || negativevalue) {
                            this.Error("syntax error in #if/#elif");
                            error = true;
                            break;
                        }
                        if (!t.equals("defined")) {
                            this.Error("undefined name '%s' in #if/#elif", t);
                            error = true;
                            break;
                        }
                        t = t.next;
                        if (t.equals("(")) {
                            brace = true;
                            t = t.next;
                        }
                        if ((null == t) || (t.type != TT_NAME)) {
                            this.Error("defined() without name in #if/#elif");
                            error = true;
                            break;
                        }
                        //v = (value_t *) GetClearedMemory(sizeof(value_t));
                        error = AllocValue(v, value_heap, numvalues);
                        if (FindHashedDefine(this.definehash, t.getData()) != null) {
                            v.intValue = 1;
                            v.floatValue = 1;
                        } else {
                            v.intValue = 0;
                            v.floatValue = 0;
                        }
                        v.parentheses = parentheses;
                        v.next = null;
                        v.prev = lastValue;
                        if (lastValue != null) {
                            lastValue.next = v;
                        } else {
                            firstValue = v;
                        }
                        lastValue = v;
                        if (brace) {
                            t = t.next;
                            if ((null == t) || !t.equals(")")) {
                                this.Error("defined missing ) in #if/#elif");
                                error = true;
                                break;
                            }
                        }
                        brace = false;
                        // defined() creates a value
                        lastwasvalue = true;
                        break;
                    }
                    case TT_NUMBER: {
                        if (lastwasvalue) {
                            this.Error("syntax error in #if/#elif");
                            error = true;
                            break;
                        }
                        //v = (value_t *) GetClearedMemory(sizeof(value_t));
                        error = AllocValue(v, value_heap, numvalues);
                        if (negativevalue) {
                            v.intValue = -t.GetIntValue();
                            v.floatValue = -t.GetFloatValue();
                        } else {
                            v.intValue = t.GetIntValue();
                            v.floatValue = t.GetFloatValue();
                        }
                        v.parentheses = parentheses;
                        v.next = null;
                        v.prev = lastValue;
                        if (lastValue != null) {
                            lastValue.next = v;
                        } else {
                            firstValue = v;
                        }
                        lastValue = v;
                        //last token was a value
                        lastwasvalue = true;
                        //
                        negativevalue = false;
                        break;
                    }
                    case TT_PUNCTUATION: {
                        if (negativevalue) {
                            this.Error("misplaced minus sign in #if/#elif");
                            error = true;
                            break;
                        }
                        if (t.subtype == P_PARENTHESESOPEN) {
                            parentheses++;
                            break;
                        } else if (t.subtype == P_PARENTHESESCLOSE) {
                            parentheses--;
                            if (parentheses < 0) {
                                this.Error("too many ) in #if/#elsif");
                                error = true;
                            }
                            break;
                        }
                        //check for invalid operators on floating point values
                        if (0 == integer) {
                            if ((t.subtype == P_BIN_NOT) || (t.subtype == P_MOD)
                                    || (t.subtype == P_RSHIFT) || (t.subtype == P_LSHIFT)
                                    || (t.subtype == P_BIN_AND) || (t.subtype == P_BIN_OR)
                                    || (t.subtype == P_BIN_XOR)) {
                                this.Error("illigal operator '%s' on floating point operands\n", t);
                                error = true;
                                break;
                            }
                        }
                        switch (t.subtype) {
                            case P_LOGIC_NOT:
                            case P_BIN_NOT: {
                                if (lastwasvalue) {
                                    this.Error("! or ~ after value in #if/#elif");
                                    error = true;
                                    break;
                                }
                                break;
                            }
                            case P_INC:
                            case P_DEC: {
                                this.Error("++ or -- used in #if/#elif");
                                break;
                            }
                            case P_SUB: {
                                if (!lastwasvalue) {
                                    negativevalue = true;
                                    break;
                                }
                            }

                            case P_MUL:
                            case P_DIV:
                            case P_MOD:
                            case P_ADD:

                            case P_LOGIC_AND:
                            case P_LOGIC_OR:
                            case P_LOGIC_GEQ:
                            case P_LOGIC_LEQ:
                            case P_LOGIC_EQ:
                            case P_LOGIC_UNEQ:

                            case P_LOGIC_GREATER:
                            case P_LOGIC_LESS:

                            case P_RSHIFT:
                            case P_LSHIFT:

                            case P_BIN_AND:
                            case P_BIN_OR:
                            case P_BIN_XOR:

                            case P_COLON:
                            case P_QUESTIONMARK: {
                                if (!lastwasvalue) {
                                    this.Error("operator '%s' after operator in #if/#elif", t);
                                    error = true;
                                    break;
                                }
                                break;
                            }
                            default: {
                                this.Error("invalid operator '%s' in #if/#elif", t);
                                error = true;
                                break;
                            }
                        }
                        if (!error && !negativevalue) {
                            //o = (operator_t *) GetClearedMemory(sizeof(operator_t));
                            error = AllocOperator(o, operator_heap, numoperators);
                            o.op = t.subtype;
                            o.priority = PC_OperatorPriority(t.subtype);
                            o.parentheses = parentheses;
                            o.next = null;
                            o.prev = lastOperator;
                            if (lastOperator != null) {
                                lastOperator.next = o;
                            } else {
                                firstOperator = o;
                            }
                            lastOperator = o;
                            lastwasvalue = false;
                        }
                        break;
                    }
                    default: {
                        this.Error("unknown '%s' in #if/#elif", t);
                        error = true;
                        break;
                    }
                }
                if (error) {
                    break;
                }
            }
            if (!error) {
                if (!lastwasvalue) {
                    this.Error("trailing operator in #if/#elif");
                    error = true;
                } else if (parentheses != 0) {
                    this.Error("too many ( in #if/#elif");
                    error = true;
                }
            }
            //
            gotquestmarkvalue = false;
            questmarkintvalue = false;
            questmarkfloatvalue = 0;
            //while there are operators
            while (!error && (firstOperator != null)) {
                v = firstValue;
                for (o = firstOperator; o.next != null; o = o.next) {
                    //if the current operator is nested deeper in parentheses
                    //than the next operator
                    if (o.parentheses > o.next.parentheses) {
                        break;
                    }
                    //if the current and next operator are nested equally deep in parentheses
                    if (o.parentheses == o.next.parentheses) {
                        //if the priority of the current operator is equal or higher
                        //than the priority of the next operator
                        if (o.priority >= o.next.priority) {
                            break;
                        }
                    }
                    //if the arity of the operator isn't equal to 1
                    if ((o.op != P_LOGIC_NOT) && (o.op != P_BIN_NOT)) {
                        v = v.next;
                    }
                    //if there's no value or no next value
                    if (null == v) {
                        this.Error("mising values in #if/#elif");
                        error = true;
                        break;
                    }
                }
                if (error) {
                    break;
                }
                v1 = v;
                v2 = v.next;
// #ifdef DEBUG_EVAL
                // if (integer) {
                // Log_Write("operator %s, value1 = %d", this.scriptstack.getPunctuationFromId(o.op), v1.intvalue);
                // if (v2) Log_Write("value2 = %d", v2.intvalue);
                // }
                // else {
                // Log_Write("operator %s, value1 = %f", this.scriptstack.getPunctuationFromId(o.op), v1.floatvalue);
                // if (v2) Log_Write("value2 = %f", v2.floatvalue);
                // }
// #endif //DEBUG_EVAL
                switch (o.op) {
                    case P_LOGIC_NOT:
                        v1.intValue = (0 == v1.intValue ? 1 : 0);
                        v1.floatValue = (0 == v1.floatValue ? 1 : 0);
                        break;
                    case P_BIN_NOT:
                        v1.intValue = ~v1.intValue;
                        break;
                    case P_MUL:
                        v1.intValue *= v2.intValue;
                        v1.floatValue *= v2.floatValue;
                        break;
                    case P_DIV:
                        if ((0 == v2.intValue) || (0 == v2.floatValue)) {
                            this.Error("divide by zero in #if/#elif\n");
                            error = true;
                            break;
                        }
                        v1.intValue /= v2.intValue;
                        v1.floatValue /= v2.floatValue;
                        break;
                    case P_MOD:
                        if (0 == v2.intValue) {
                            this.Error("divide by zero in #if/#elif\n");
                            error = true;
                            break;
                        }
                        v1.intValue %= v2.intValue;
                        break;
                    case P_ADD:
                        v1.intValue += v2.intValue;
                        v1.floatValue += v2.floatValue;
                        break;
                    case P_SUB:
                        v1.intValue -= v2.intValue;
                        v1.floatValue -= v2.floatValue;
                        break;
                    case P_LOGIC_AND:
                        v1.intValue = ((v1.intValue != 0) && (v2.intValue != 0)) ? 1 : 0;
                        v1.floatValue = ((v1.floatValue != 0) && (v2.floatValue != 0)) ? 1 : 0;
                        break;
                    case P_LOGIC_OR:
                        v1.intValue = ((v1.intValue != 0) || (v2.intValue != 0)) ? 1 : 0;
                        v1.floatValue = ((v1.floatValue != 0) || (v2.floatValue != 0)) ? 1 : 0;
                        break;
                    case P_LOGIC_GEQ:
                        v1.intValue = (v1.intValue >= v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue >= v2.floatValue) ? 1 : 0;
                        break;
                    case P_LOGIC_LEQ:
                        v1.intValue = (v1.intValue <= v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue <= v2.floatValue) ? 1 : 0;
                        break;
                    case P_LOGIC_EQ:
                        v1.intValue = (v1.intValue == v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue == v2.floatValue) ? 1 : 0;
                        break;
                    case P_LOGIC_UNEQ:
                        v1.intValue = (v1.intValue != v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue != v2.floatValue) ? 1 : 0;
                        break;
                    case P_LOGIC_GREATER:
                        v1.intValue = (v1.intValue > v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue > v2.floatValue) ? 1 : 0;
                        break;
                    case P_LOGIC_LESS:
                        v1.intValue = (v1.intValue < v2.intValue) ? 1 : 0;
                        v1.floatValue = (v1.floatValue < v2.floatValue) ? 1 : 0;
                        break;
                    case P_RSHIFT:
                        v1.intValue >>= v2.intValue;
                        break;
                    case P_LSHIFT:
                        v1.intValue <<= v2.intValue;
                        break;
                    case P_BIN_AND:
                        v1.intValue &= v2.intValue;
                        break;
                    case P_BIN_OR:
                        v1.intValue |= v2.intValue;
                        break;
                    case P_BIN_XOR:
                        v1.intValue ^= v2.intValue;
                        break;
                    case P_COLON: {
                        if (!gotquestmarkvalue) {
                            this.Error(": without ? in #if/#elif");
                            error = true;
                            break;
                        }
                        if (integer != 0) {
                            if (!questmarkintvalue) {
                                v1.intValue = v2.intValue;
                            }
                        } else {
                            if (0 == questmarkfloatvalue) {
                                v1.floatValue = v2.floatValue;
                            }
                        }
                        gotquestmarkvalue = false;
                        break;
                    }
                    case P_QUESTIONMARK: {
                        if (gotquestmarkvalue) {
                            this.Error("? after ? in #if/#elif");
                            error = true;
                            break;
                        }
                        questmarkintvalue = (v1.intValue != 0);
                        questmarkfloatvalue = v1.floatValue;
                        gotquestmarkvalue = true;
                        break;
                    }
                }
// #ifdef DEBUG_EVAL
                // if (integer) Log_Write("result value = %d", v1.intvalue);
                // else Log_Write("result value = %f", v1.floatvalue);
// #endif //DEBUG_EVAL
                if (error) {
                    break;
                }
//                lastoperatortype = o.op;
                //if not an operator with arity 1
                if ((o.op != P_LOGIC_NOT) && (o.op != P_BIN_NOT)) {
                    //remove the second value if not question mark operator
                    if (o.op != P_QUESTIONMARK) {
                        v = v.next;
                    }
                    //
                    if (v.prev != null) {
                        v.prev.next = v.next;
                    } else {
                        firstValue = v.next;
                    }
                    if (v.next != null) {
                        v.next.prev = v.prev;
                    } else {
                        lastValue = v.prev;
                    }
                    //FreeMemory(v);
//                    FreeValue(v);//TODO:does this macro do anytihng?
                }
                //remove the operator
                if (o.prev != null) {
                    o.prev.next = o.next;
                } else {
                    firstOperator = o.next;
                }
                if (o.next != null) {
                    o.next.prev = o.prev;
                } else {
                    lastOperator = o.prev;
                }
                //FreeMemory(o);
//                FreeOperator(o);//TODO:see above
            }
            if (firstValue != null) {
                if (intValue[0] != 0) {
                    intValue[0] = firstValue.intValue;
                }
                if (floatValue[0] != 0) {
                    floatValue[0] = firstValue.floatValue;
                }
            }
            for (o = firstOperator; o != null; o = lastOperator) {
                lastOperator = o.next;
                //FreeMemory(o);
//                FreeOperator(o);//TODO:see 2 up.
            }
            for (v = firstValue; v != null; v = lastValue) {
                lastValue = v.next;
                //FreeMemory(v);
//                FreeValue(v);//TODO:see 3 up
            }
            if (!error) {
                return true;
            }
            if (intValue[0] != 0) {
                intValue[0] = 0;
            }
            if (floatValue[0] != 0) {
                floatValue[0] = 0;
            }
            return false;
        }

        private boolean Evaluate(long[] intvalue, double[] floatvalue, int integer) throws idException {
            final idToken token = new idToken();
            idToken firstToken, lastToken;
            idToken t;
			final idToken nextToken;
            define_s define;
            boolean defined = false;

            if (intvalue != null) {
                intvalue[0] = 0;
            }
            if (floatvalue != null) {
                floatvalue[0] = 0;
            }
            //
            if (!this.ReadLine(token)) {
                this.Error("no value after #if/#elif");
                return false;
            }
            firstToken = null;
            lastToken = null;
            do {
                //if the token is a name
                if (token.type == TT_NAME) {
                    if (defined) {
                        defined = false;
                        t = new idToken(token);
                        t.next = null;
                        if (lastToken != null) {
                            lastToken.next = t;
                        } else {
                            firstToken = t;
                        }
                        lastToken = t;
                    } else if (token.equals("defined")) {
                        defined = true;
                        t = new idToken(token);
                        t.next = null;
                        if (lastToken != null) {
                            lastToken.next = t;
                        } else {
                            firstToken = t;
                        }
                        lastToken = t;
                    } else {
                        //then it must be a define
                        define = FindHashedDefine(this.definehash, token.getData());
                        if (null == define) {
                            this.Error("can't Evaluate '%s', not defined", token);
                            return false;
                        }
                        if (!this.ExpandDefineIntoSource(token, define)) {
                            return false;
                        }
                    }
                } //if the token is a number or a punctuation
                else if ((token.type == TT_NUMBER) || (token.type == TT_PUNCTUATION)) {
                    t = new idToken(token);
                    t.next = null;
                    if (lastToken != null) {
                        lastToken.next = t;
                    } else {
                        firstToken = t;
                    }
                    lastToken = t;
                } else {
                    this.Error("can't Evaluate '%s'", token);
                    return false;
                }
            } while (this.ReadLine(token));
            //
            if (!this.EvaluateTokens(firstToken, intvalue, floatvalue, integer)) {
                return false;
            }
//            //
//// #ifdef DEBUG_EVAL
//            // Log_Write("eval:");
//// #endif //DEBUG_EVAL
//            for (t = firsttoken; t != null; t = nexttoken) {
//// #ifdef DEBUG_EVAL
//                // Log_Write(" %s", t.c_str());
//// #endif //DEBUG_EVAL
//                nexttoken = t.next;
////		delete t;
//            } //end for
//// #ifdef DEBUG_EVAL
//            // if (integer) Log_Write("eval result: %d", *intvalue);
//            // else Log_Write("eval result: %f", *floatvalue);
//// #endif //DEBUG_EVAL
//            //
            return true;
        }

        private boolean DollarEvaluate(long[] intValue, double[] floatValue, int integer) throws idException {
            int indent;
            boolean defined = false;
            final idToken token = new idToken();
            idToken firstToken, lasttoken;
            idToken t;
			final idToken nexttoken;
            define_s define;

            if (intValue != null) {
                intValue[0] = 0;
            }
            if (floatValue != null) {
                floatValue[0] = 0;
            }
            //
            if (!this.ReadSourceToken(token)) {
                this.Error("no leading ( after $evalint/$evalfloat");
                return false;
            }
            if (!this.ReadSourceToken(token)) {
                this.Error("nothing to Evaluate");
                return false;
            }
            indent = 1;
            firstToken = null;
            lasttoken = null;
            do {
                //if the token is a name
                if (token.type == TT_NAME) {
                    if (defined) {
                        defined = false;
                        t = new idToken(token);
                        t.next = null;
                        if (lasttoken != null) {
                            lasttoken.next = t;
                        } else {
                            firstToken = t;
                        }
                        lasttoken = t;
                    } else if (token.equals("defined")) {
                        defined = true;
                        t = new idToken(token);
                        t.next = null;
                        if (lasttoken != null) {
                            lasttoken.next = t;
                        } else {
                            firstToken = t;
                        }
                        lasttoken = t;
                    } else {
                        //then it must be a define
                        define = FindHashedDefine(this.definehash, token.getData());
                        if (null == define) {
                            this.Warning("can't Evaluate '%s', not defined", token);
                            return false;
                        }
                        if (!this.ExpandDefineIntoSource(token, define)) {
                            return false;
                        }
                    }
                } //if the token is a number or a punctuation
                else if ((token.type == TT_NUMBER) || (token.type == TT_PUNCTUATION)) {
                    if (token.oGet(0) == '(') {
                        indent++;
                    } else if (token.oGet(0) == ')') {
                        indent--;
                    }
                    if (indent <= 0) {
                        break;
                    }
                    t = new idToken(token);
                    t.next = null;
                    if (lasttoken != null) {
                        lasttoken.next = t;
                    } else {
                        firstToken = t;
                    }
                    lasttoken = t;
                } else {
                    this.Error("can't Evaluate '%s'", token);
                    return false;
                }
            } while (this.ReadSourceToken(token));
            //
            if (!this.EvaluateTokens(firstToken, intValue, floatValue, integer)) {
                return false;
            }
            // //
// // #ifdef DEBUG_EVAL
            // // Log_Write("$eval:");
// // #endif //DEBUG_EVAL
            // for (t = firsttoken; t; t = nexttoken) {
// // #ifdef DEBUG_EVAL
            // // Log_Write(" %s", t.c_str());
// // #endif //DEBUG_EVAL
            // nexttoken = t.next;
            // delete t;
            // } //end for
// // #ifdef DEBUG_EVAL
            // // if (integer) Log_Write("$eval result: %d", *intvalue);
            // // else Log_Write("$eval result: %f", *floatvalue);
// // #endif //DEBUG_EVAL
            // //
            return true;
        }

        private boolean Directive_define() throws idException {
            final idToken token = new idToken();
            idToken t, last;
            define_s define;

            if (!this.ReadLine(token)) {
                this.Error("#define without name");
                return false;
            }
            if (token.type != TT_NAME) {
                this.UnreadSourceToken(token);
                this.Error("expected name after #define, found '%s'", token);
                return false;
            }
            // check if the define already exists
            define = FindHashedDefine(this.definehash, token.getData());
            if (define != null) {
                if ((define.flags & DEFINE_FIXED) != 0) {
                    this.Error("can't redefine '%s'", token);
                    return false;
                }
                this.Warning("redefinition of '%s'", token);
                // unread the define name before executing the #undef directive
                this.UnreadSourceToken(token);
                if (!this.Directive_undef()) {
                    return false;
                }
                // if the define was not removed (define.flags & DEFINE_FIXED)
                define = FindHashedDefine(this.definehash, token.getData());
            }
            // allocate define
//	define = (define_t *) Mem_ClearedAlloc(sizeof(define_t) + token.Length() + 1);
            define = new define_s();
//	define.name = (char *) define + sizeof(define_t);
            define.name = String.copyValueOf(token.c_str());
            // add the define to the source
            AddDefineToHash(define, this.definehash);
            // if nothing is defined, just return
            if (!this.ReadLine(token)) {
                return true;
            }
            // if it is a define with parameters
            if (!token.WhiteSpaceBeforeToken() && token.equals("(")) {
                // read the define parameters
                last = null;
                if (!this.CheckTokenString(")")) {
                    while (true) {
                        if (!this.ReadLine(token)) {
                            this.Error("expected define parameter");
                            return false;
                        }
                        // if it isn't a name
                        if (token.type != TT_NAME) {
                            this.Error("invalid define parameter");
                            return false;
                        }

                        if (FindDefineParm(define, token.getData()) >= 0) {
                            this.Error("two the same define parameters");
                            return false;
                        }
                        // add the define parm
                        t = new idToken(token);
                        t.ClearTokenWhiteSpace();
                        t.next = null;
                        if (last != null) {
                            last.next = t;
                        } else {
                            define.parms = t;
                        }
                        last = t;
                        define.numparms++;
                        // read next token
                        if (!this.ReadLine(token)) {
                            this.Error("define parameters not terminated");
                            return false;
                        }

                        if (token.equals(")")) {
                            break;
                        }
                        // then it must be a comma
                        if (!token.equals(",")) {
                            this.Error("define not terminated");
                            return false;
                        }
                    }
                }
                if (!this.ReadLine(token)) {
                    return true;
                }
            }
            // read the defined stuff
            last = null;
            do {
                t = new idToken(token);
                if ((t.type == TT_NAME) && t.getData().equals(define.name)) {
                    t.flags |= TOKEN_FL_RECURSIVE_DEFINE;
                    this.Warning("recursive define (removed recursion)");
                }
                t.ClearTokenWhiteSpace();
                t.next = null;
                if (last != null) {
                    last.next = t;
                } else {
                    define.tokens = t;
                }
                last = t;
            } while (this.ReadLine(token));

            if (last != null) {
                // check for merge operators at the beginning or end
                if (define.tokens.equals("##") || last.equals("##")) {
                    this.Error("define with misplaced ##");
                    return false;
                }
            }
            return true;
        }

        private boolean Directive_elif() throws idException {
            final long[] value = new long[1];
            final int[] type = new int[1], skip = new int[1];

            this.PopIndent(type, skip);
            if (type[0] == INDENT_ELSE) {
                this.Error("misplaced #elif");
                return false;
            }
            if (!this.Evaluate(value, null, 1)) {
                return false;
            }
            skip[0] = (value[0] == 0) ? 1 : 0;
            this.PushIndent(INDENT_ELIF, skip[0]);
            return true;
        }

        private boolean Directive_if() throws idException {
            final long[] value = new long[1];
            final int[] skip = new int[1];

            if (!this.Evaluate(value, null, 1)) {
                return false;
            }
            skip[0] = (value[0] == 0) ? 1 : 0;
            this.PushIndent(INDENT_IF, skip[0]);
            return true;
        }

        private boolean Directive_line() throws idException {
            final idToken token = new idToken();

            this.Error("#line directive not supported");
            while (this.ReadLine(token)) {
                //TODO:??
            }
            return true;
        }

        private boolean Directive_error() throws idException {
            final idToken token = new idToken();

            if (!this.ReadLine(token) || (token.type != TT_STRING)) {
                this.Error("#error without string");
                return false;
            }
            this.Error("#error: %s", token);
            return true;
        }

        private boolean Directive_warning() throws idException {
            final idToken token = new idToken();

            if (!this.ReadLine(token) || (token.type != TT_STRING)) {
                this.Error("#warning without string");
                return false;
            }
            this.Error("#warning: %s", token);
            return true;
        }

        private boolean Directive_pragma() throws idException {
            final idToken token = new idToken();

            this.Warning("#pragma directive not supported");
            while (this.ReadLine(token)) {
                //TODO::???
            }
            return true;
        }

        private void UnreadSignToken() {
            final idToken token = new idToken();

            token.line = this.scriptstack.GetLineNum();
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
            token.linesCrossed = 0;
            token.flags = 0;
            token.oSet("-");
            token.type = TT_PUNCTUATION;
            token.subtype = P_SUB;
            this.UnreadSourceToken(token);
        }

        private boolean Directive_eval() throws idException {
            final long[] value = new long[1];
            final idToken token = new idToken();
            String buf;//[128];

            if (!this.Evaluate(value, null, 1)) {
                return false;
            }

            token.line = this.scriptstack.GetLineNum();
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
            token.linesCrossed = 0;
            token.flags = 0;
            buf = String.format("%d", Math.abs(value[0]));
            token.oSet(buf);
            token.type = TT_NUMBER;
            token.subtype = TT_INTEGER | TT_LONG | TT_DECIMAL;
            this.UnreadSourceToken(token);
            if (value[0] < 0) {
                this.UnreadSignToken();
            }
            return true;
        }

        private boolean Directive_evalfloat() throws idException {
            final double[] value = new double[1];
            final idToken token = new idToken();
            String buf;//[128];

            if (!this.Evaluate(null, value, 1)) {
                return false;
            }

            token.line = this.scriptstack.GetLineNum();
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
            token.linesCrossed = 0;
            token.flags = 0;
            buf = String.format("%1.2f", idMath.Fabs((float) value[0]));
            token.oSet(buf);
            token.type = TT_NUMBER;
            token.subtype = TT_FLOAT | TT_LONG | TT_DECIMAL;
            this.UnreadSourceToken(token);
            if (value[0] < 0) {
                this.UnreadSignToken();
            }
            return true;
        }

        private boolean ReadDirective() throws idException {
            final idToken token = new idToken();

            //read the directive name
            if (!this.ReadSourceToken(token)) {
                this.Error("found '#' without name");
                return false;
            }
            //directive name must be on the same line
            if (token.linesCrossed > 0) {
                this.UnreadSourceToken(token);
                this.Error("found '#' at end of line");
                return false;
            }
            //if if is a name
            if (token.type == TT_NAME) {
                if (token.equals("ifdef")) {
                    return this.Directive_ifdef();
                } else if (token.equals("ifndef")) {
                    return this.Directive_ifndef();
                } else if (token.equals("if")) {//token.equals() is overriden to startsWith.
                    return this.Directive_if();
                } else if (token.equals("elif")) {
                    return this.Directive_elif();
                } else if (token.equals("else")) {
                    return this.Directive_else();
                } else if (token.equals("endif")) {
                    return this.Directive_endif();
                } else if (this.skip > 0) {
                    // skip the rest of the line
                    while (this.ReadLine(token)) {
                    }
                    return true;
                } else {
                    switch (token.getData()) {
                        case "include":
                            return this.Directive_include();
                        case "define":
                            return this.Directive_define();
                        case "undef":
                            return this.Directive_undef();
                        case "line":
                            return this.Directive_line();
                        case "error":
                            return this.Directive_error();
                        case "warning":
                            return this.Directive_warning();
                        case "pragma":
                            return this.Directive_pragma();
                        case "eval":
                            return this.Directive_eval();
                        case "evalfloat":
                            return this.Directive_evalfloat();
                    }
                }
            }
            this.Error("unknown precompiler directive '%s'", token);
            return false;
        }

        private boolean DollarDirective_evalint() throws idException {
            final long[] value = new long[1];
            final idToken token = new idToken();
            String buf;//[128];

            if (!this.DollarEvaluate(value, null, 1)) {
                return false;
            }

            token.line = this.scriptstack.GetLineNum();
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
            token.linesCrossed = 0;
            token.flags = 0;
            buf = String.format("%d", Math.abs(value[0]));
            token.oSet(buf);
            token.type = TT_NUMBER;
            token.subtype = TT_INTEGER | TT_LONG | TT_DECIMAL | TT_VALUESVALID;
            token.intValue = Math.abs(value[0]);
            token.floatValue = Math.abs(value[0]);
            this.UnreadSourceToken(token);
            if (value[0] < 0) {
                this.UnreadSignToken();
            }
            return true;
        }

        private boolean DollarDirective_evalfloat() throws idException {
            final double[] value = new double[1];
            final idToken token = new idToken();
            String buf;//[128];

            if (!this.DollarEvaluate(null, value, 1)) {
                return false;
            }

            token.line = this.scriptstack.GetLineNum();
            token.whiteSpaceStart_p = 0;
            token.whiteSpaceEnd_p = 0;
            token.linesCrossed = 0;
            token.flags = 0;
            buf = String.format("%1.2f", idMath.Fabs((float) value[0]));
            token.oSet(buf);
            token.type = TT_NUMBER;
            token.subtype = TT_FLOAT | TT_LONG | TT_DECIMAL | TT_VALUESVALID;
            token.intValue = (long) idMath.Fabs((float) value[0]);
            token.floatValue = idMath.Fabs((float) value[0]);
            this.UnreadSourceToken(token);
            if (value[0] < 0) {
                this.UnreadSignToken();
            }
            return true;
        }

        private boolean ReadDollarDirective() throws idException {
            final idToken token = new idToken();

            // read the directive name
            if (!this.ReadSourceToken(token)) {
                this.Error("found '$' without name");
                return false;
            }
            // directive name must be on the same line
            if (token.linesCrossed > 0) {
                this.UnreadSourceToken(token);
                this.Error("found '$' at end of line");
                return false;
            }
            // if if is a name
            if (token.type == TT_NAME) {
                if (token.equals("evalint")) {
                    return this.DollarDirective_evalint();
                } else if (token.equals("evalfloat")) {
                    return this.DollarDirective_evalfloat();
                }
            }
            this.UnreadSourceToken(token);
            return false;
        }
    }

    /*
     ================
     PC_NameHash
     ================
     */
    static int PC_NameHash(final String name) {
        return PC_NameHash(name.toCharArray());
    }

    static int PC_NameHash(final char[] name) {
        int hash, i;

        hash = 0;
        for (i = 0; (i < name.length) && (name[i] != '\0'); i++) {
            hash += name[i] * (119 + i);
        }
        hash = (hash ^ (hash >> 10) ^ (hash >> 20)) & (DEFINEHASHSIZE - 1);
        return hash;
    }
}
