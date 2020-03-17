package neo.idlib;

import static neo.TempDump.bbtocb;
import static neo.framework.Common.STRTABLE_ID;
import static neo.framework.Common.STRTABLE_ID_LENGTH;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;

import java.nio.ByteBuffer;

import neo.framework.File_h.idFile;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class LangDict {

    /*
     ===============================================================================

     Simple dictionary specifically for the localized string tables.

     ===============================================================================
     */
    public static class idLangKeyValue {

        public idStr key;
        public idStr value;
    };

    public static class idLangDict {

        public idList<idLangKeyValue> args = new idList<>();
        private idHashIndex hash = new idHashIndex();
        //
        private int baseID;
        //
        //

        public idLangDict() {
            args.SetGranularity(256);
            hash.SetGranularity(256);
            hash.Clear(4096, 8192);
            baseID = 0;
        }
//public							~idLangDict( void );
//

        public void Clear() {
            args.Clear();
            hash.Clear();
        }

        public boolean Load(final String fileName) throws idException {
            return Load(fileName, true);
        }

        public boolean Load(final String fileName, boolean clear) throws idException {

            if (clear) {
                Clear();
            }

            ByteBuffer[] buffer = {null};
            idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);

            int len = idLib.fileSystem.ReadFile(fileName, buffer);
            if (len <= 0) {
                // let whoever called us deal with the failure (so sys_lang can be reset)
                return false;
            }
            src.LoadMemory(bbtocb(buffer[0]), bbtocb(buffer[0]).capacity(), fileName);
            if (!src.IsLoaded()) {
                return false;
            }

            idToken tok, tok2;
            src.ExpectTokenString("{");
            while (src.ReadToken(tok = new idToken())) {
                if (tok.equals("}")) {
                    break;
                }
                if (src.ReadToken(tok2 = new idToken())) {
                    if (tok2.equals("}")) {
                        break;
                    }
                    idLangKeyValue kv = new idLangKeyValue();
                    kv.key = tok;
                    kv.value = tok2;
                    assert (kv.key.Cmpn(STRTABLE_ID, STRTABLE_ID_LENGTH) == 0);
//                    if (tok.equals("#str_07184")) {
//                        tok2.oSet("006");
//                    }
                    hash.Add(GetHashKey(kv.key), args.Append(kv));
                }
            }
            idLib.common.Printf("%d strings read from %s\n", args.Num(), fileName);
            idLib.fileSystem.FreeFile(buffer);

            return true;
        }

        public void Save(String fileName) {
            idFile outFile = idLib.fileSystem.OpenFileWrite(fileName);
            outFile.WriteFloatString("// string table\n// english\n//\n\n{\n");
            for (int j = 0; j < args.Num(); j++) {
                outFile.WriteFloatString("\t\"%s\"\t\"", args.oGet(j).key);
                int l = args.oGet(j).value.Length();
                char slash = '\\';
                char tab = 't';
                char nl = 'n';
                for (int k = 0; k < l; k++) {
                    char ch = args.oGet(j).value.getData().charAt(k);
                    if (ch == '\t') {
                        outFile.WriteChar(slash);
                        outFile.WriteChar(tab);
                    } else if (ch == '\n' || ch == '\r') {
                        outFile.WriteChar(slash);
                        outFile.WriteChar(nl);
                    } else {
                        outFile.WriteChar(ch);
                    }
                }
                outFile.WriteFloatString("\"\n");
            }
            outFile.WriteFloatString("\n}\n");
            idLib.fileSystem.CloseFile(outFile);
        }

        public String AddString(String str) {

            if (ExcludeString(str)) {
                return str;
            }

            int c = args.Num();
            for (int j = 0; j < c; j++) {
                if (idStr.Cmp(args.oGet(j).value.getData(), str) == 0) {
                    return args.oGet(j).key.getData();
                }
            }

            int id = GetNextId();
            idLangKeyValue kv = new idLangKeyValue();
            // _D3XP
            kv.key = new idStr(Str.va("#str_%08i", id));
            // kv.key = va( "#str_%05i", id );
            kv.value = new idStr(str);
            c = args.Append(kv);
            assert (kv.key.Cmpn(STRTABLE_ID, STRTABLE_ID_LENGTH) == 0);
            hash.Add(GetHashKey(kv.key), c);
            return args.oGet(c).key.getData();
        }

        private static int DBG_GetString = 1;
        public String GetString(final String str) throws idException {
            if ("#str_07184".equals(str)) {
//                System.out.printf("GetString#%d\n", DBG_GetString);
//                return (DBG_GetString++) + "bnlaaaaaaaaaaa";
            }

            if (str == null || str.isEmpty()) {
                return "";
            }            

            if (idStr.Cmpn(str, STRTABLE_ID, STRTABLE_ID_LENGTH) != 0) {
                return str;
            }

            int hashKey = GetHashKey(str);
            for (int i = hash.First(hashKey); i != -1; i = hash.Next(i)) {
                if (args.oGet(i).key.Cmp(str) == 0) {
                    return args.oGet(i).value.getData();
                }
            }

            idLib.common.Warning("Unknown string id %s", str);
            return str;
        }

        public String GetString(final idStr str) throws idException {
            return GetString(str.getData());
        }

        // adds the value and key as passed (doesn't generate a "#str_xxxxx" key or ensure the key/value pair is unique)
        public void AddKeyVal(final String key, final String val) {
            idLangKeyValue kv = new idLangKeyValue();
            kv.key = new idStr(key);
            kv.value = new idStr(val);
            assert (kv.key.Cmpn(STRTABLE_ID, STRTABLE_ID_LENGTH) == 0);
            hash.Add(GetHashKey(kv.key), args.Append(kv));
        }

        public int GetNumKeyVals() {
            return args.Num();
        }

        public idLangKeyValue GetKeyVal(int i) {
            return args.oGet(i);
        }

        public void SetBaseID(int id) {
            baseID = id;
        }

        private boolean ExcludeString(final String str) {
            if (str == null) {
                return true;
            }

            int c = str.length();
            if (c <= 1) {
                return true;
            }

            if (idStr.Cmpn(str, STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                return true;
            }

            if (idStr.Icmpn(str, "gui::", "gui::".length()) == 0) {
                return true;
            }

            if (str.charAt(0) == '$') {
                return true;
            }

            int i;
            for (i = 0; i < c; i++) {
                if (Character.isAlphabetic(str.charAt(i))) {
                    break;
                }
            }

            return (i == c);
        }

        private int GetNextId() {
            int c = args.Num();

            //Let and external user supply the base id for this dictionary
            int id = baseID;

            if (c == 0) {
                return id;
            }

            idStr work;
            for (int j = 0; j < c; j++) {
                work = args.oGet(j).key;
                work.StripLeading(STRTABLE_ID);
                int test = Integer.parseInt(work.getData());
                if (test > id) {
                    id = test;
                }
            }
            return id + 1;
        }

        private int GetHashKey(final idStr str) {
            return GetHashKey(str.getData());
        }

        private int GetHashKey(final String str) {
            int hashKey = 0;
            int i;
            char c;

            for (i = STRTABLE_ID_LENGTH; i < str.length(); i++) {
                c = str.charAt(i);
                assert (Character.isDigit(c));

                hashKey = hashKey * 10 + c - '0';
            }
            return hashKey;
        }

    };
}
