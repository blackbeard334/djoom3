package neo.framework;

import static neo.framework.DeclManager.DECL_LEXER_FLAGS;

import neo.framework.DeclManager.idDecl;
import neo.idlib.Lib;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class DeclTable {

    /*
     ===============================================================================

     tables are used to map a floating point input value to a floating point
     output value, with optional wrap / clamp and interpolation

     ===============================================================================
     */
    public static class idDeclTable extends idDecl {

        private boolean clamp;
        private boolean snap;
        private idList<Float> values = new idList<>();
        //
        //

        @Override
        public long Size() {
            return /*sizeof(idDeclTable) +*/ values.Allocated();
        }

        @Override
        public String DefaultDefinition() {
            return "{ { 0 } }";
        }

        @Override
        public boolean Parse(String text, int textLength) throws Lib.idException {
            idLexer src = new idLexer();
            idToken token = new idToken();
            float v;

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            snap = false;
            clamp = false;
            values.Clear();

            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (token.Icmp("snap") == 0) {
                    snap = true;
                } else if (token.Icmp("clamp") == 0) {
                    clamp = true;
                } else if (token.Icmp("{") == 0) {

                    while (true) {
                        boolean[] errorFlag = new boolean[1];

                        v = src.ParseFloat(errorFlag);
                        if (errorFlag[0]) {
                            // we got something non-numeric
                            MakeDefault();
                            return false;
                        }

                        values.Append(v);

                        src.ReadToken(token);
                        if (token.equals("}")) {
                            break;
                        }
                        if (token.equals(",")) {
                            continue;
                        }
                        src.Warning("expected comma or brace");
                        MakeDefault();
                        return false;
                    }

                } else {
                    src.Warning("unknown token '%s'", token.toString());
                    MakeDefault();
                    return false;
                }
            }

            // copy the 0 element to the end, so lerping doesn't
            // need to worry about the wrap case
            float val = values.oGet(0);		// template bug requires this to not be in the Append()?
            values.Append(val);

            return true;
        }

        @Override
        public void FreeData() {
            snap = false;
            clamp = false;
            values.Clear();
        }

        public float TableLookup(float index) {
            int iIndex;
            float iFrac;

            int domain = values.Num() - 1;

            if (domain <= 1) {
                return 1.0f;
            }

            if (clamp) {
                index *= (domain - 1);
                if (index >= domain - 1) {
                    return values.oGet(domain - 1);
                } else if (index <= 0) {
                    return values.oGet(0);
                }
                iIndex = idMath.Ftoi(index);
                iFrac = index - iIndex;
            } else {
                index *= domain;

                if (index < 0) {
                    index += domain * idMath.Ceil(-index / domain);
                }

                iIndex = idMath.FtoiFast(idMath.Floor(index));
                iFrac = index - iIndex;
                iIndex = iIndex % domain;
            }

            if (!snap) {
                // we duplicated the 0 index at the end at creation time, so we
                // don't need to worry about wrapping the filter
                return values.oGet(iIndex) * (1.0f - iFrac) + values.oGet(iIndex + 1) * iFrac;
            }

            return values.oGet(iIndex);
        }

    };
}
