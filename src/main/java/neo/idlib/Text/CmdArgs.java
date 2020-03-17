package neo.idlib.Text;

import static neo.idlib.Text.Lexer.LEXFL_ALLOWIPADDRESSES;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Lexer.LEXFL_NOWARNINGS;
import static neo.idlib.Text.Lexer.LEXFL_ONLYSTRINGS;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.math.Lcp.clam;
import static neo.idlib.math.Lcp.unClam;

import java.util.Arrays;

import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;

/**
 *
 */
public class CmdArgs {
    /*
     ===============================================================================

     Command arguments.

     ===============================================================================
     */

    class idCmdArgs {

        private static final int MAX_COMMAND_ARGS = 64;
        private static final int MAX_COMMAND_STRING = 2 * Lib.MAX_STRING_CHARS;
        //
        private int argc;							// number of arguments
        private char[] argv = new char[MAX_COMMAND_ARGS];			// points into tokenized
        private char[] tokenized = new char[MAX_COMMAND_STRING];		// will have 0 bytes inserted
        //
        //

        public idCmdArgs() {
            argc = 0;
        }

        public idCmdArgs(final String text, boolean keepAsStrings) throws idException {
            TokenizeString(text, keepAsStrings);
        }
//

        public void oSet(final idCmdArgs args) {
            int i;

            argc = args.argc;
//	memcpy( tokenized, args.tokenized, MAX_COMMAND_STRING );
            System.arraycopy(args.tokenized, 0, tokenized, 0, MAX_COMMAND_STRING);
//            for (i = 0; i < argc; i++) {
//		argv[ i ] = tokenized + ( args.argv[ i ] - args.tokenized );
//            }
            System.arraycopy(args.argv, 0, argv, 0, argc);
        }
//

        // The functions that execute commands get their parameters with these functions.
        public int Argc() {
            return argc;
        }

        // Argv() will return an empty string, not NULL if arg >= argc.
        public String Argv(int arg) {
            return (String) ((arg >= 0 && arg < argc) ? argv[arg] : "");
        }

        // Returns a single string containing argv(start) to argv(end)
        // escapeArgs is a fugly way to put the string back into a state ready to tokenize again
//public	String			Args( int start = 1, int end = -1, bool escapeArgs = false ) const;
        public String Args(int start, int end, boolean escapeArgs) {
//	static char cmd_args[MAX_COMMAND_STRING];
            String cmd_args = "";
            int i;

            if (end < 0) {
                end = argc - 1;
            } else if (end >= argc) {
                end = argc - 1;
            }
            cmd_args += '\0';
            if (escapeArgs) {
//		strcat( cmd_args, "\"" );
                cmd_args += "\"";
            }
            for (i = start; i <= end; i++) {
                if (i > start) {
                    if (escapeArgs) {
                        cmd_args += "\" \"";
                    } else {
                        cmd_args += " ";
                    }
                }
                if (escapeArgs && Arrays.binarySearch(argv, i, argv.length, '\\') != 0) {
                    int p = i;
                    while (argv[p] != '\0') {
                        if (argv[p] == '\\') {
                            cmd_args += "\\\\";
                        } else {
                            int l = cmd_args.length();
                            cmd_args += argv[p];
                            cmd_args += '\0';
                        }
                        p++;
                    }
                } else {
                    cmd_args += argv[i];
                }
            }
            if (escapeArgs) {
                cmd_args += "\"";
            }

            return cmd_args;
        }
//

        /*
         ============
         idCmdArgs::TokenizeString

         Parses the given string into command line tokens.
         The text is copied to a separate buffer and 0 characters
         are inserted in the appropriate place. The argv array
         will point into this temporary buffer.
         ============
         */
        // Takes a null terminated string and breaks the string up into arg tokens.
        // Does not need to be /n terminated.
        // Set keepAsStrings to true to only seperate tokens from whitespace and comments, ignoring punctuation
        public void TokenizeString(final String text, boolean keepAsStrings) throws idException {
            idLexer lex = new idLexer();
            idToken token = new idToken();
            idToken number = new idToken();
            int len, totalLen;

            // clear previous args
            argc = 0;

            if (null == text) {
                return;
            }

            lex.LoadMemory(text, text.length(), "idCmdSystemLocal::TokenizeString");
            lex.SetFlags((int) (LEXFL_NOERRORS
                    | LEXFL_NOWARNINGS
                    | LEXFL_NOSTRINGCONCAT
                    | LEXFL_ALLOWPATHNAMES
                    | LEXFL_NOSTRINGESCAPECHARS
                    | LEXFL_ALLOWIPADDRESSES | (keepAsStrings ? LEXFL_ONLYSTRINGS : 0)));

            totalLen = 0;

            while (true) {
                if (argc == MAX_COMMAND_ARGS) {
                    return;			// this is usually something malicious
                }

                if (!lex.ReadToken(token)) {
                    return;
                }

                // check for negative numbers
                if (!keepAsStrings && (token.equals("-"))) {
                    if (lex.CheckTokenType(TT_NUMBER, 0, number) != 0) {
                        token.oSet("-" + number);
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

                if (totalLen + len + 1 > tokenized.length) {
                    return;			// this is usually something malicious
                }

                // regular token
                argv[argc] = tokenized[totalLen];
                argc++;

                char[] tokenizedClam = clam(tokenized, totalLen);
                idStr.Copynz(tokenizedClam, token.getData(), tokenized.length - totalLen);
                unClam(tokenized, tokenizedClam);

                totalLen += len + 1;
            }
        }
//

        public void AppendArg(final String text) {
            if (0 == argc) {
                argc = 1;
                argv[0] = tokenized[0];
                idStr.Copynz(tokenized, text, tokenized.length);
            } else {
                argv[argc] = argv[argc - 1 + (argv.length - argc - 1) + 1];
                char[] argvClam = clam(argv, argc);
                idStr.Copynz(argvClam, text, tokenized.length - (argv.length - argc - tokenized[0]));
                unClam(argv, argvClam);
                argc++;
            }
        }

        public void Clear() {
            argc = 0;
        }

        public char[] GetArgs(int[] _argc) {
            _argc[0] = argc;
            return argv;
        }
    };
}
