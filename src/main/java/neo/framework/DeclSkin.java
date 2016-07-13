package neo.framework;

import java.nio.ByteBuffer;
import neo.Renderer.Material.idMaterial;
import static neo.TempDump.NOT;
import neo.TempDump.SERiAL;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import neo.framework.DeclManager.idDecl;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;

/**
 *
 */
public class DeclSkin {

    /*
     ===============================================================================

     idDeclSkin

     ===============================================================================
     */
    static class skinMapping_t {

        idMaterial from;			// 0 == any unmatched shader
        idMaterial to;
    };

    public static class idDeclSkin extends idDecl implements SERiAL {

        private idList<skinMapping_t> mappings = new idList<>();
        private idStrList associatedModels = new idStrList();
        //
        //

        @Override
        public long Size() {
//            return sizeof( idDeclSkin );
            return super.Size();
        }

        @Override
        public boolean SetDefaultText() throws idException {
            // if there exists a material with the same name
            if (declManager.FindType(DECL_MATERIAL, GetName(), false) != null) {
                StringBuilder generated = new StringBuilder(2048);

                idStr.snPrintf(generated, generated.capacity(),
                        "skin %s // IMPLICITLY GENERATED\n"
                        + "{\n"
                        + "_default %s\n"
                        + "}\n", GetName(), GetName());
                SetText(generated.toString());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "\"*\"\t\"_default\"\n"
                    + "}";
        }

        @Override
        public boolean Parse(String text, int textLength) throws idException {
            idLexer src = new idLexer();
            idToken token = new idToken(), token2 = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            associatedModels.Clear();

            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (0 == token.Icmp("}")) {
                    break;
                }
                if (!src.ReadToken(token2)) {
                    src.Warning("Unexpected end of file");
                    MakeDefault();
                    return false;
                }

                if (0 == token.Icmp("model")) {
                    associatedModels.Append(token2.toString());
                    continue;
                }

                skinMapping_t map = new skinMapping_t();

                if (0 == token.Icmp("*")) {
                    // wildcard
                    map.from = null;
                } else {
                    map.from = declManager.FindMaterial(token);
                }

                map.to = declManager.FindMaterial(token2);

                mappings.Append(map);
            }

            return false;
        }

        @Override
        public void FreeData() {
            mappings.Clear();
        }

        public idMaterial RemapShaderBySkin(final idMaterial shader) {
            int i;

            if (NOT(shader)) {
                return null;
            }

            // never remap surfaces that were originally nodraw, like collision hulls
            if (!shader.IsDrawn()) {
                return shader;
            }

            for (i = 0; i < mappings.Num(); i++) {
                final skinMapping_t map = mappings.oGet(i);

                // null = wildcard match
                if (NOT(map.from) || map.from.equals(shader)) {
                    return map.to;
                }
            }

            // didn't find a match or wildcard, so stay the same
            return shader;
        }

        // model associations are just for the preview dialog in the editor
        public int GetNumModelAssociations() {
            return associatedModels.Num();
        }

        public String GetAssociatedModel(int index) {
            if (index >= 0 && index < associatedModels.Num()) {
                return associatedModels.oGet(index).toString();
            }
            return "";
        }

        public void oSet(idDeclSkin skin) {
            this.mappings.oSet(skin.mappings);
            this.associatedModels.oSet(skin.associatedModels);
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
}
