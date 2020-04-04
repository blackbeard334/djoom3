package neo.idlib;

import static neo.idlib.Text.Lexer.LEXFL_ALLOWIPADDRESSES;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Lexer.LEXFL_NOWARNINGS;
import static neo.idlib.Text.Lexer.LEXFL_ONLYSTRINGS;
import static neo.idlib.Text.Token.TT_NUMBER;

import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Token.idToken;

/**
 *
 */
public class CmdArgs {

    public static class idCmdArgs {

        private static final int MAX_COMMAND_ARGS   = 64;
        private static final int MAX_COMMAND_STRING = 2 * Lib.MAX_STRING_CHARS;
        private int argc;                            // number of arguments
        private final String[]      argv      = new String[MAX_COMMAND_ARGS];         // points into tokenized
        private       StringBuilder tokenized = new StringBuilder(MAX_COMMAND_STRING);// will have 0 bytes inserted
        //
        //

        public idCmdArgs() {
        }

        public idCmdArgs(final String text, boolean keepAsStrings) throws idException {
            TokenizeString(text, keepAsStrings);
        }

        //operator=( final idCmdArgs &args );
        public void oSet(final idCmdArgs args) {
            int i;

            this.argc = args.argc;
//	memcpy( tokenized, args.tokenized, MAX_COMMAND_STRING );
            this.tokenized = new StringBuilder(args.tokenized);
            for (i = 0; i < this.argc; i++) {
//		argv[ i ] = tokenized + ( args.argv[ i ] - args.tokenized );//TODO:what the hell does this do??????
                this.argv[i] = args.argv[i];
            }
        }

        public void oSet(final String text) {
            TokenizeString(text, false);
        }

        // The functions that execute commands get their parameters with these functions.
        public final int Argc() {
            return this.argc;
        }

        // Argv() will return an empty string, not NULL if arg >= argc.
        public String Argv(int arg) {
            return ((arg >= 0) && (arg < this.argc)) ? this.argv[arg] : "";
        }

        // Returns a single string containing argv(start) to argv(end)
        // escapeArgs is a fugly way to put the string back into a state ready to tokenize again
        public String Args() {
            return Args(1, -1, false);
        }

        public String Args(int start) {
            return Args(start, -1, false);
        }

        public String Args(int start, int end) {
            return Args(start, end, false);
        }

        public String Args(int start, int end, boolean escapeArgs) {
//	char []cmd_args=new char[MAX_COMMAND_STRING];
            String cmd_args = "";
            int i;

            if (end < 0) {
                end = this.argc - 1;
            } else if (end >= this.argc) {
                end = this.argc - 1;
            }
//	cmd_args[0] = '\0';
            if (escapeArgs) {
//		strcat( cmd_args, "\"" );
                cmd_args += "\"";
            }
            for (i = start; i <= end; i++) {
                if (i > start) {
                    if (escapeArgs) {
//				strcat( cmd_args, "\" \"" );
                        cmd_args += "\" \"";
                    } else {
//				strcat( cmd_args, " " );
                        cmd_args += " ";
                    }
                }
//		if ( escapeArgs && strchr( argv[i], '\\' ) ) {
                if (escapeArgs && this.argv[i].contains("\\")) {
//			char *p = argv[i];
                    int p = i;
                    while (p < this.argv[i].length()) {
                        if (this.argv[i].charAt(p) == '\\') {
//					strcat( cmd_args, "\\\\" );
                            cmd_args += "\\\\";
                        } else {
                            final int l = cmd_args.length();
                            cmd_args += this.argv[i].charAt(p);
//					cmd_args[ l ] = *p;
//					cmd_args[ l+1 ] = '\0';
                        }
                        p++;
                    }
                } else {
//			strcat( cmd_args, argv[i] );
                    cmd_args += this.argv[i];
                }
            }
            if (escapeArgs) {
//		strcat( cmd_args, "\"" );
                cmd_args += "\"";
            }

            return cmd_args;
        }

        /*
         ============
         idCmdArgs::TokenizeString

         Parses the given string into command line tokens.
         The text is copied to a separate buffer and 0 characters
         are inserted in the appropriate place. The argv array
         will point into this temporary buffer.
         ============
         // Takes a null terminated string and breaks the string up into arg tokens.
         // Does not need to be /n terminated.
         // Set keepAsStrings to true to only seperate tokens from whitespace and comments, ignoring punctuation
         */
        public void TokenizeString(final String text, boolean keepAsStrings) throws idException {
            final idLexer lex = new idLexer();
            final idToken token = new idToken();
            final idToken number = new idToken();
            int len, totalLen;

            // clear previous args
            this.argc = 0;

            if (null == text) {
                return;
            }

            lex.LoadMemory(text, text.length(), "idCmdSystemLocal::TokenizeString");
            lex.SetFlags((LEXFL_NOERRORS
                    | LEXFL_NOWARNINGS
                    | LEXFL_NOSTRINGCONCAT
                    | LEXFL_ALLOWPATHNAMES
                    | LEXFL_NOSTRINGESCAPECHARS
                    | LEXFL_ALLOWIPADDRESSES | (keepAsStrings ? LEXFL_ONLYSTRINGS : 0)));

            totalLen = 0;

            while (true) {
                if (this.argc == MAX_COMMAND_ARGS) {
                    return;			// this is usually something malicious
                }

                if (!lex.ReadToken(token)) {
                    return;
                }

                // check for negative numbers
                if (!keepAsStrings && (token.equals("-"))) {
                    if (lex.CheckTokenType(TT_NUMBER, 0, number) != 0) {
                        token.oSet("-" + number.getData());
                    }
                }

                // check for cvar expansion
                if (token.equals("$")) {
                    if (!lex.ReadToken(token)) {
                        return;
                    }
                    if (idLib.cvarSystem != null) {
                        token.oSet(idLib.cvarSystem.GetCVarString(token.getData()));
                    } else {
                        token.oSet("<unknown>");
                    }
                }

                len = token.Length();

                if ((totalLen + len + 1) > /*sizeof(*/ this.tokenized.capacity()) {
                    return;			// this is usually something malicious
                }

//                tokenized.append(token);//damn pointers!
                // regular token
                this.argv[this.argc] = this.tokenized.replace(totalLen, this.tokenized.capacity(), token.getData()).substring(totalLen);
                this.argc++;

//                idStr::Copynz( tokenized + totalLen, token.c_str(), sizeof( tokenized ) - totalLen );
//                tokenized.replace(totalLen, tokenized.capacity() - token.Length(), token.toString());
                totalLen += len;// + 1;//we don't need the '\0'.
            }
        }

        public void AppendArg(final String text) {
            if (0 == this.argc) {
                this.argc = 1;
                this.argv[0] = text;
//		idStr::Copynz( tokenized, text, sizeof( tokenized ) );
                this.tokenized = new StringBuilder(this.tokenized.capacity()).append(text);
            } else {
//              argv[ argc ] = argv[ argc-1 ] + strlen( argv[ argc-1 ] ) + 1;
//              idStr::Copynz( argv[ argc ], text, sizeof( tokenized ) - ( argv[ argc ] - tokenized ) );
                this.argv[this.argc++] = text;
            }
        }

        public void Clear() {
            this.argc = 0;
        }

        public final String[] GetArgs(int[] _argc) {
            _argc[0] = this.argc;
            return this.argv;
        }
    }
}
