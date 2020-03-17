package neo.framework;

import static neo.Game.Game_local.game;
import static neo.framework.Common.EDITOR_AAS;
import static neo.framework.Common.EDITOR_RADIANT;
import static neo.framework.Common.com_editors;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.idlib.Text.Token.TT_STRING;

import neo.framework.DeclManager.idDecl;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class DeclEntityDef {

    /*
     ===============================================================================

     idDeclEntityDef

     ===============================================================================
     */
    public static class idDeclEntityDef extends idDecl {

        public idDict dict = new idDict();

        @Override
        public long Size() {
//            return sizeof( idDeclEntityDef ) + dict.Allocated();
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "\"DEFAULTED\"\t\"1\"\n"
                    + "}";
        }

        @Override
        public boolean Parse(String text, int textLength) throws Lib.idException {
            idLexer src = new idLexer();
            idToken token = new idToken(), token2 = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (0 == token.Icmp("}")) {
                    break;
                }
                if (token.type != TT_STRING) {
                    src.Warning("Expected quoted string, but found '%s'", token.getData());
                    MakeDefault();
                    return false;
                }

                if (!src.ReadToken(token2)) {
                    src.Warning("Unexpected end of file");
                    MakeDefault();
                    return false;
                }

                if (dict.FindKey(token.getData()) != null) {
                    src.Warning("'%s' already defined", token.getData());
                }
                dict.Set(token, token2);
            }

            // we always automatically set a "classname" key to our name
            dict.Set("classname", GetName());

            // "inherit" keys will cause all values from another entityDef to be copied into this one
            // if they don't conflict.  We can't have circular recursions, because each entityDef will
            // never be parsed mroe than once
            // find all of the dicts first, because copying inherited values will modify the dict
            idList<idDeclEntityDef> defList = new idList<>();

            while (true) {
                final idKeyValue kv;
                kv = dict.MatchPrefix("inherit", null);
                if (null == kv) {
                    break;
                }

                final idDeclEntityDef copy = /*static_cast<const idDeclEntityDef *>*/ (idDeclEntityDef) declManager.FindType(DECL_ENTITYDEF, kv.GetValue(), false);
                if (null == copy) {
                    src.Warning("Unknown entityDef '%s' inherited by '%s'", kv.GetValue(), GetName());
                } else {
                    defList.Append(copy);
                }

                // delete this key/value pair
                dict.Delete(kv.GetKey().getData());
            }

            // now copy over the inherited key / value pairs
            for (int i = 0; i < defList.Num(); i++) {
                dict.SetDefaults(defList.oGet(i).dict);
            }

            // precache all referenced media
            // do this as long as we arent in modview
            if (0 == (com_editors & (EDITOR_RADIANT | EDITOR_AAS))) {
                game.CacheDictionaryMedia(dict);
            }

            return true;
        }

        @Override
        public void FreeData() {
            dict.Clear();
        }

        /*
         ================
         idDeclEntityDef::Print

         Dumps all key/value pairs, including inherited ones
         ================
         */
        @Override
        public void Print() throws idException {
            dict.Print();
        }
    };
}
