package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.Renderer.RenderSystem.GLYPH_END;
import static neo.Renderer.RenderSystem.GLYPH_START;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_GUI;
import static neo.framework.Common.com_ticNumber;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.idlib.Text.Str.C_COLOR_DEFAULT;
import static neo.idlib.Text.Str.C_COLOR_ESCAPE;
import static neo.idlib.Text.Str.va;
import static neo.ui.DeviceContext.idDeviceContext.ALIGN.ALIGN_CENTER;
import static neo.ui.DeviceContext.idDeviceContext.ALIGN.ALIGN_RIGHT;
import static neo.ui.DeviceContext.idDeviceContext.CURSOR.CURSOR_ARROW;
import static neo.ui.DeviceContext.idDeviceContext.CURSOR.CURSOR_COUNT;
import static neo.ui.DeviceContext.idDeviceContext.CURSOR.CURSOR_HAND;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_COUNT;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_DOWN;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_HBACK;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_LEFT;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_RIGHT;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_THUMB;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_UP;
import static neo.ui.DeviceContext.idDeviceContext.SCROLLBAR.SCROLLBAR_VBACK;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderSystem.fontInfoEx_t;
import neo.Renderer.RenderSystem.fontInfo_t;
import neo.Renderer.RenderSystem.glyphInfo_t;
import neo.framework.CVarSystem.idCVar;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;
import neo.ui.Rectangle.idRectangle;
import neo.ui.Rectangle.idRegion;

/**
 *
 */
public class DeviceContext {

    public static final int VIRTUAL_WIDTH = 640;
    public static final int VIRTUAL_HEIGHT = 480;
    public static final int BLINK_DIVISOR = 200;

    static final idCVar gui_smallFontLimit = new idCVar("gui_smallFontLimit", "0.30", CVAR_GUI | CVAR_ARCHIVE, "");
    static final idCVar gui_mediumFontLimit = new idCVar("gui_mediumFontLimit", "0.60", CVAR_GUI | CVAR_ARCHIVE, "");

    public static class idDeviceContext {

        private final idMaterial[] cursorImages = new idMaterial[CURSOR_COUNT.ordinal()];
        private final idMaterial[] scrollBarImages = new idMaterial[SCROLLBAR_COUNT.ordinal()];
        private idMaterial whiteImage;
        private fontInfoEx_t activeFont;
        private fontInfo_t useFont;
        private idStr fontName = new idStr();
        private float xScale;
        private float yScale;
        //
        private float vidHeight;
        private float vidWidth;
        //
        private CURSOR cursor;
        //
        private idList<idRectangle> clipRects = new idList<>();
        //	
        private static idList<fontInfoEx_t> fonts = new idList<>();
        private idStr fontLang;
        //
        private boolean enableClipping;
        //
        private boolean overStrikeMode;
        //
        private idMat3 mat;
        private idVec3 origin;
        private boolean initialized;
        //
        private boolean mbcs;
        //
        //

        public idDeviceContext() {
            this.fontLang = new idStr();
            this.mat = new idMat3();
            this.origin = new idVec3();
            Clear();
        }
        // ~idDeviceContext() { }

        public void Init() {
            xScale = 0;
            SetSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            whiteImage = declManager.FindMaterial("guis/assets/white.tga");
            whiteImage.SetSort(SS_GUI);
            mbcs = false;
            SetupFonts();
            activeFont = fonts.oGet(0);
            colorPurple = new idVec4(1, 0, 1, 1);
            colorOrange = new idVec4(1, 1, 0, 1);
            colorYellow = new idVec4(0, 1, 1, 1);
            colorGreen = new idVec4(0, 1, 0, 1);
            colorBlue = new idVec4(0, 0, 1, 1);
            colorRed = new idVec4(1, 0, 0, 1);
            colorWhite = new idVec4(1, 1, 1, 1);
            colorBlack = new idVec4(0, 0, 0, 1);
            colorNone = new idVec4(0, 0, 0, 0);
            cursorImages[CURSOR_ARROW.ordinal()] = declManager.FindMaterial("ui/assets/guicursor_arrow.tga");
            cursorImages[CURSOR_HAND.ordinal()] = declManager.FindMaterial("ui/assets/guicursor_hand.tga");
            scrollBarImages[SCROLLBAR_HBACK.ordinal()] = declManager.FindMaterial("ui/assets/scrollbarh.tga");
            scrollBarImages[SCROLLBAR_VBACK.ordinal()] = declManager.FindMaterial("ui/assets/scrollbarv.tga");
            scrollBarImages[SCROLLBAR_THUMB.ordinal()] = declManager.FindMaterial("ui/assets/scrollbar_thumb.tga");
            scrollBarImages[SCROLLBAR_RIGHT.ordinal()] = declManager.FindMaterial("ui/assets/scrollbar_right.tga");
            scrollBarImages[SCROLLBAR_LEFT.ordinal()] = declManager.FindMaterial("ui/assets/scrollbar_left.tga");
            scrollBarImages[SCROLLBAR_UP.ordinal()] = declManager.FindMaterial("ui/assets/scrollbar_up.tga");
            scrollBarImages[SCROLLBAR_DOWN.ordinal()] = declManager.FindMaterial("ui/assets/scrollbar_down.tga");
            cursorImages[CURSOR_ARROW.ordinal()].SetSort(SS_GUI);
            cursorImages[CURSOR_HAND.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_HBACK.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_VBACK.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_THUMB.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_RIGHT.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_LEFT.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_UP.ordinal()].SetSort(SS_GUI);
            scrollBarImages[SCROLLBAR_DOWN.ordinal()].SetSort(SS_GUI);
            cursor = CURSOR_ARROW;
            enableClipping = true;
            overStrikeMode = true;
            mat.Identity();
            origin.Zero();
            initialized = true;
        }

        public void Shutdown() {
            fontName.Clear();
            clipRects.Clear();
            fonts.Clear();
            Clear();
        }

        public boolean Initialized() {
            return initialized;
        }
//
//        public void EnableLocalization();
//

        public void GetTransformInfo(idVec3 origin, idMat3 mat) {
            mat.oSet(this.mat);
            origin.oSet(this.origin);
        }

        public void SetTransformInfo(final idVec3 origin, final idMat3 mat) {
            this.origin = origin;
            this.mat = mat;
        }

        public void DrawMaterial(float x, float y, float w, float h, final idMaterial mat, final idVec4 color) {
            DrawMaterial(x, y, w, h, mat, color, 1.0f);

        }

        public void DrawMaterial(float x, float y, float w, float h, final idMaterial mat, final idVec4 color, float scalex /*= 1.0f*/) {
            DrawMaterial(x, y, w, h, mat, color, scalex, 1.0f);
        }

        public void DrawMaterial(float x, float y, float w, float h, final idMaterial mat, final idVec4 color, float scaleX /*= 1.0f*/, float scaleY /*= 1.0f*/) {

            renderSystem.SetColor(color);

            float[] s0 = {0}, s1 = {0}, t0 = {0}, t1 = {0};
            float[] x1 = {x}, y1 = {y}, w1 = {w}, h1 = {h};
// 
//  handle negative scales as well	
            if (scaleX < 0) {
                w1[0] *= -1;
                scaleX *= -1;
            }
            if (scaleY < 0) {
                h1[0] *= -1;
                scaleY *= -1;
            }
// 
            if (w1[0] < 0) {	// flip about vertical
                w1[0] = -w1[0];
                s0[0] = 1 * scaleX;
                s1[0] = 0;
            } else {
                s0[0] = 0;
                s1[0] = 1 * scaleX;
            }

            if (h1[0] < 0) {	// flip about horizontal
                h1[0] = -h1[0];
                t0[0] = 1 * scaleY;
                t1[0] = 0;
            } else {
                t0[0] = 0;
                t1[0] = 1 * scaleY;
            }

            if (ClippedCoords(x1, y1, w1, h1, s0, t0, s1, t1)) {
                return;
            }

            AdjustCoords(x1, y1, w1, h1);
            
            DrawStretchPic(x1[0], y1[0], w1[0], h1[0], s0[0], t0[0], s1[0], t1[0], mat);
            bla99++;
        }
        static int bla99 = 0;

        public void DrawRect(float x, float y, float width, float height, float size, final idVec4 color) {

            float[] x1 = {x}, y1 = {y}, w1 = {width}, h1 = {height};

            if (color.w == 0) {
                return;
            }

            renderSystem.SetColor(color);

            if (ClippedCoords(x1, y1, w1, h1, null, null, null, null)) {
                return;
            }

            AdjustCoords(x1, y1, w1, h1);
            DrawStretchPic(x1[0], y1[0], size, h1[0], 0, 0, 0, 0, whiteImage);
            DrawStretchPic(x1[0] + w1[0] - size, y1[0], size, h1[0], 0, 0, 0, 0, whiteImage);
            DrawStretchPic(x1[0], y1[0], w1[0], size, 0, 0, 0, 0, whiteImage);
            DrawStretchPic(x1[0], y1[0] + h1[0] - size, w1[0], size, 0, 0, 0, 0, whiteImage);
        }

        public void DrawFilledRect(float x, float y, float width, float height, final idVec4 color) {

            float[] x1 = {x}, y1 = {y}, w1 = {width}, h1 = {height};

            if (color.w == 0) {
                return;
            }

            renderSystem.SetColor(color);

            if (ClippedCoords(x1, y1, w1, h1, null, null, null, null)) {
                return;
            }

            AdjustCoords(x1, y1, w1, h1);
            DrawStretchPic(x1[0], y1[0], w1[0], h1[0], 0, 0, 0, 0, whiteImage);
            aaaa++;
        }
        static int aaaa = 0;

        public int DrawText(String text, float textScale, int textAlign, idVec4 color, idRectangle rectDraw, boolean wrap, int cursor /*= -1*/, boolean calcOnly /*= false*/, idList<Integer> breaks /*= NULL*/, int limit /*= 0*/) {
            char p, textPtr;
            int p_i, newLinePtr = 0;
            final char[] buff = new char[1024];
            int len, newLine, newLineWidth, count;
            float y;
            float textWidth;

            float charSkip = MaxCharWidth(textScale) + 1;
            float lineSkip = MaxCharHeight(textScale);

            float cursorSkip = (cursor >= 0 ? charSkip : 0);

            boolean lineBreak, wordBreak;

            SetFontByScale(textScale);

//            textWidth = 0;
//	newLinePtr = NULL;
            if (!calcOnly && !(text != null && !text.isEmpty())) {
                if (cursor == 0) {
                    renderSystem.SetColor(color);
                    DrawEditCursor(rectDraw.x, lineSkip + rectDraw.y, textScale);
                }
                return idMath.FtoiFast(rectDraw.w / charSkip);
            }

            if (!text.contains("\\0")) {
                text += '\0';//TODO:we temporarily append a '\0' here, but we should refactor the code.
            }

            y = lineSkip + rectDraw.y;
            len = 0;
            buff[0] = '\0';
            newLine = 0;
            newLineWidth = 0;
            p_i = 0;

            if (breaks != null) {
                breaks.Append(0);
            }
            count = 0;
            textWidth = 0;
            lineBreak = false;
            wordBreak = false;
            while (p_i < text.length()) {
                p = text.charAt(p_i);

                if (p == '\n' || p == '\r' || p == '\0') {
                    lineBreak = true;
                    if ((p == '\n' && text.charAt(p_i + 1) == '\r') || (p == '\r' && text.charAt(p_i + 1) == '\n')) {
                        p = text.charAt(p_i++);
                    }
                }

                int nextCharWidth = (int) (idStr.CharIsPrintable(p) ? CharWidth(p, textScale) : cursorSkip);
                // FIXME: this is a temp hack until the guis can be fixed not not overflow the bounding rectangles
                //	      the side-effect is that list boxes and edit boxes will draw over their scroll bars
                //  The following line and the !linebreak in the if statement below should be removed
                nextCharWidth = 0;

                if (!lineBreak && (textWidth + nextCharWidth) > rectDraw.w) {
                    // The next character will cause us to overflow, if we haven't yet found a suitable
                    // break spot, set it to be this character
                    if (len > 0 && newLine == 0) {
                        newLine = len;
                        newLinePtr = p_i;
                        newLineWidth = (int) textWidth;
                    }
                    wordBreak = true;
                } else if (lineBreak || (wrap && (p == ' ' || p == '\t'))) {
                    // The next character is in view, so if we are a break character, store our position
                    newLine = len;
                    newLinePtr = p_i + 1;
                    newLineWidth = (int) textWidth;
                }

                if (lineBreak || wordBreak) {
                    float x = rectDraw.x;

                    if (textAlign == etoi(ALIGN_RIGHT)) {
                        x = rectDraw.x + rectDraw.w - newLineWidth;
                    } else if (textAlign == etoi(ALIGN_CENTER)) {
                        x = rectDraw.x + (rectDraw.w - newLineWidth) / 2;
                    }

                    if (wrap || newLine > 0) {
                        buff[newLine] = '\0';

                        // This is a special case to handle breaking in the middle of a word.
                        // if we didn't do this, the cursor would appear on the end of this line
                        // and the beginning of the next.
                        if (wordBreak && cursor >= newLine && newLine == len) {
                            cursor++;
                        }
                    }

                    if (!calcOnly) {
                        count += DrawText(x, y, textScale, color, ctos(buff), 0, 0, 0, cursor);
                    }

                    if (cursor < newLine) {
                        cursor = -1;
                    } else if (cursor >= 0) {
                        cursor -= (newLine + 1);
                    }

                    if (!wrap) {
                        return newLine;
                    }

                    if ((limit != 0 && count > limit) || p == '\0') {
                        break;
                    }

                    y += lineSkip + 5;

                    if (!calcOnly && y > rectDraw.Bottom()) {
                        break;
                    }

                    p_i = newLinePtr;//TODO:check if any of the pointers are actually incremented.

                    if (breaks != null) {
                        breaks.Append(p_i);
                    }

                    len = 0;
                    newLine = 0;
                    newLineWidth = 0;
                    textWidth = 0;
                    lineBreak = false;
                    wordBreak = false;
                    continue;
                }

                buff[len++] = p;
                p_i++;
                buff[len] = '\0';
                // update the width
                bla++;
                if (buff[len - 1] != C_COLOR_ESCAPE && (len <= 1 || buff[len - 2] != C_COLOR_ESCAPE)) {
                    textWidth += textScale * useFont.glyphScale * useFont.glyphs[ buff[len - 1]].xSkip;
                    // Jim Dosé, I don't know who you are..but I hate you.
                }
            }

            return idMath.FtoiFast(rectDraw.w / charSkip);
        }
        static int bla = 0;

        public int DrawText(final idStr text, float textScale, int textAlign, idVec4 color, idRectangle rectDraw, boolean wrap, int cursor /*= -1*/, boolean calcOnly /*= false*/, idList<Integer> breaks /*= NULL*/) {
            return DrawText(text.toString(), textScale, textAlign, color, rectDraw, wrap, cursor, calcOnly, breaks, 0);
        }

        public int DrawText(final String text, float textScale, int textAlign, idVec4 color, idRectangle rectDraw, boolean wrap, int cursor) {
            return DrawText(text, textScale, textAlign, color, rectDraw, wrap, cursor, false, null, 0);
        }

        public int DrawText(final String text, float textScale, int textAlign, idVec4 color, idRectangle rectDraw, boolean wrap) {
            return DrawText(text, textScale, textAlign, color, rectDraw, wrap, -1);
        }

        public int DrawText(final idStr text, float textScale, int textAlign, idVec4 color, idRectangle rectDraw, boolean wrap, int cursor) {
            return DrawText(text.toString(), textScale, textAlign, color, rectDraw, wrap, cursor);
        }

        public void DrawMaterialRect(float x, float y, float w, float h, float size, final idMaterial mat, final idVec4 color) {

            if (color.w == 0) {
                return;
            }

            renderSystem.SetColor(color);
            DrawMaterial(x, y, size, h, mat, color);
            DrawMaterial(x + w - size, y, size, h, mat, color);
            DrawMaterial(x, y, w, size, mat, color);
            DrawMaterial(x, y + h - size, w, size, mat, color);
        }

        public void DrawStretchPic(float x, float y, float w, float h, float s0, float t0, float s1, float t1, final idMaterial shader) {
            final idDrawVert[] verts = {new idDrawVert(), new idDrawVert(), new idDrawVert(), new idDrawVert()};
            final int/*glIndex_t*/[] indexes = new int[6];
            indexes[0] = 3;
            indexes[1] = 0;
            indexes[2] = 2;
            indexes[3] = 2;
            indexes[4] = 0;
            indexes[5] = 1;
            verts[0].xyz.oSet(0, x);
            verts[0].xyz.oSet(1, y);
            verts[0].xyz.oSet(2, 0);
            verts[0].st.oSet(0, s0);
            verts[0].st.oSet(1, t0);
            verts[0].normal.oSet(0, 0);
            verts[0].normal.oSet(1, 0);
            verts[0].normal.oSet(2, 1);
            verts[0].tangents[0].oSet(0, 1);
            verts[0].tangents[0].oSet(1, 0);
            verts[0].tangents[0].oSet(2, 0);
            verts[0].tangents[1].oSet(0, 0);
            verts[0].tangents[1].oSet(1, 1);
            verts[0].tangents[1].oSet(2, 0);
            verts[1].xyz.oSet(0, x + w);
            verts[1].xyz.oSet(1, y);
            verts[1].xyz.oSet(2, 0);
            verts[1].st.oSet(0, s1);
            verts[1].st.oSet(1, t0);
            verts[1].normal.oSet(0, 0);
            verts[1].normal.oSet(1, 0);
            verts[1].normal.oSet(2, 1);
            verts[1].tangents[0].oSet(0, 1);
            verts[1].tangents[0].oSet(1, 0);
            verts[1].tangents[0].oSet(2, 0);
            verts[1].tangents[1].oSet(0, 0);
            verts[1].tangents[1].oSet(1, 1);
            verts[1].tangents[1].oSet(2, 0);
            verts[2].xyz.oSet(0, x + w);
            verts[2].xyz.oSet(1, y + h);
            verts[2].xyz.oSet(2, 0);
            verts[2].st.oSet(0, s1);
            verts[2].st.oSet(1, t1);
            verts[2].normal.oSet(0, 0);
            verts[2].normal.oSet(1, 0);
            verts[2].normal.oSet(2, 1);
            verts[2].tangents[0].oSet(0, 1);
            verts[2].tangents[0].oSet(1, 0);
            verts[2].tangents[0].oSet(2, 0);
            verts[2].tangents[1].oSet(0, 0);
            verts[2].tangents[1].oSet(1, 1);
            verts[2].tangents[1].oSet(2, 0);
            verts[3].xyz.oSet(0, x);
            verts[3].xyz.oSet(1, y + h);
            verts[3].xyz.oSet(2, 0);
            verts[3].st.oSet(0, s0);
            verts[3].st.oSet(1, t1);
            verts[3].normal.oSet(0, 0);
            verts[3].normal.oSet(1, 0);
            verts[3].normal.oSet(2, 1);
            verts[3].tangents[0].oSet(0, 1);
            verts[3].tangents[0].oSet(1, 0);
            verts[3].tangents[0].oSet(2, 0);
            verts[3].tangents[1].oSet(0, 0);
            verts[3].tangents[1].oSet(1, 1);
            verts[3].tangents[1].oSet(2, 0);

            boolean identity = !mat.IsIdentity();
            if (identity) {
                verts[0].xyz.oMinSet(origin);
                verts[0].xyz.oMulSet(mat);
                verts[0].xyz.oPluSet(origin);
                verts[1].xyz.oMinSet(origin);
                verts[1].xyz.oMulSet(mat);
                verts[1].xyz.oPluSet(origin);
                verts[2].xyz.oMinSet(origin);
                verts[2].xyz.oMulSet(mat);
                verts[2].xyz.oPluSet(origin);
                verts[3].xyz.oMinSet(origin);
                verts[3].xyz.oMulSet(mat);
                verts[3].xyz.oPluSet(origin);
            }

            renderSystem.DrawStretchPic(verts, indexes, 4, 6, shader, identity);
        }

        public void DrawMaterialRotated(float x, float y, float w, float h, final idMaterial mat, final idVec4 color, float scalex /*= 1.0*/, float scaley /*= 1.0*/, float angle /*= 0.0f*/) {

            renderSystem.SetColor(color);

            float[] s0 = new float[1], s1 = new float[1], t0 = new float[1], t1 = new float[1];
            float[] x1 = {x}, y1 = {y}, w1 = {w}, h1 = {h};
            // 
            //  handle negative scales as well	
            if (scalex < 0) {
                w1[0] *= -1;
                scalex *= -1;
            }
            if (scaley < 0) {
                h1[0] *= -1;
                scaley *= -1;
            }
            // 
            if (w1[0] < 0) {	// flip about vertical
                w1[0] = -w1[0];
                s0[0] = 1 * scalex;
                s1[0] = 0;
            } else {
                s0[0] = 0;
                s1[0] = 1 * scalex;
            }

            if (h1[0] < 0) {	// flip about horizontal
                h1[0] = -h1[0];
                t0[0] = 1 * scaley;
                t1[0] = 0;
            } else {
                t0[0] = 0;
                t1[0] = 1 * scaley;
            }

            if (angle == 0 && ClippedCoords(x1, y1, w1, h1, s0, t0, s1, t1)) {
                return;
            }

            AdjustCoords(x1, y1, w1, h1);

            DrawStretchPicRotated(x1[0], y1[0], w1[0], h1[0], s0[0], t0[0], s1[0], t1[0], mat, angle);
        }

        public void DrawStretchPicRotated(float x, float y, float w, float h, float s0, float t0, float s1, float t1, final idMaterial shader, float angle /*= 0.0f*/) {
            final idDrawVert[] verts = new idDrawVert[4];
            final int/*glIndex_t*/[] indexes = new int[6];
            indexes[0] = 3;
            indexes[1] = 0;
            indexes[2] = 2;
            indexes[3] = 2;
            indexes[4] = 0;
            indexes[5] = 1;
            verts[0].xyz.oSet(0, x);
            verts[0].xyz.oSet(1, y);
            verts[0].xyz.oSet(2, 0);
            verts[0].st.oSet(0, s0);
            verts[0].st.oSet(1, t0);
            verts[0].normal.oSet(0, 0);
            verts[0].normal.oSet(1, 0);
            verts[0].normal.oSet(2, 1);
            verts[0].tangents[0].oSet(0, 1);
            verts[0].tangents[0].oSet(1, 0);
            verts[0].tangents[0].oSet(2, 0);
            verts[0].tangents[1].oSet(0, 0);
            verts[0].tangents[1].oSet(1, 1);
            verts[0].tangents[1].oSet(2, 0);
            verts[1].xyz.oSet(0, x + w);
            verts[1].xyz.oSet(1, y);
            verts[1].xyz.oSet(2, 0);
            verts[1].st.oSet(0, s1);
            verts[1].st.oSet(1, t0);
            verts[1].normal.oSet(0, 0);
            verts[1].normal.oSet(1, 0);
            verts[1].normal.oSet(2, 1);
            verts[1].tangents[0].oSet(0, 1);
            verts[1].tangents[0].oSet(1, 0);
            verts[1].tangents[0].oSet(2, 0);
            verts[1].tangents[1].oSet(0, 0);
            verts[1].tangents[1].oSet(1, 1);
            verts[1].tangents[1].oSet(2, 0);
            verts[2].xyz.oSet(0, x + w);
            verts[2].xyz.oSet(1, y + h);
            verts[2].xyz.oSet(2, 0);
            verts[2].st.oSet(0, s1);
            verts[2].st.oSet(1, t1);
            verts[2].normal.oSet(0, 0);
            verts[2].normal.oSet(1, 0);
            verts[2].normal.oSet(2, 1);
            verts[2].tangents[0].oSet(0, 1);
            verts[2].tangents[0].oSet(1, 0);
            verts[2].tangents[0].oSet(2, 0);
            verts[2].tangents[1].oSet(0, 0);
            verts[2].tangents[1].oSet(1, 1);
            verts[2].tangents[1].oSet(2, 0);
            verts[3].xyz.oSet(0, x);
            verts[3].xyz.oSet(1, y + h);
            verts[3].xyz.oSet(2, 0);
            verts[3].st.oSet(0, s0);
            verts[3].st.oSet(1, t1);
            verts[3].normal.oSet(0, 0);
            verts[3].normal.oSet(1, 0);
            verts[3].normal.oSet(2, 1);
            verts[3].tangents[0].oSet(0, 1);
            verts[3].tangents[0].oSet(1, 0);
            verts[3].tangents[0].oSet(2, 0);
            verts[3].tangents[1].oSet(0, 0);
            verts[3].tangents[1].oSet(1, 1);
            verts[3].tangents[1].oSet(2, 0);

            boolean ident = !mat.IsIdentity();
            if (ident) {
                verts[0].xyz.oMinSet(origin);
                verts[0].xyz.oMulSet(mat);
                verts[0].xyz.oPluSet(origin);
                verts[1].xyz.oMinSet(origin);
                verts[1].xyz.oMulSet(mat);
                verts[1].xyz.oPluSet(origin);
                verts[2].xyz.oMinSet(origin);
                verts[2].xyz.oMulSet(mat);
                verts[2].xyz.oPluSet(origin);
                verts[3].xyz.oMinSet(origin);
                verts[3].xyz.oMulSet(mat);
                verts[3].xyz.oPluSet(origin);
            }

            //Generate a translation so we can translate to the center of the image rotate and draw
            idVec3 origTrans = new idVec3();
            origTrans.x = x + (w / 2);
            origTrans.y = y + (h / 2);
            origTrans.z = 0;

            //Rotate the verts about the z axis before drawing them
            idMat4 rotz = new idMat4();
            rotz.Identity();
            final float sinAng = idMath.Sin(angle);
            final float cosAng = idMath.Cos(angle);
            rotz.oSet(0, 0, cosAng);
            rotz.oSet(0, 1, sinAng);
            rotz.oSet(1, 0, -sinAng);
            rotz.oSet(1, 1, cosAng);
            for (int i = 0; i < 4; i++) {
                //Translate to origin
                verts[i].xyz.oMinSet(origTrans);

                //Rotate
                verts[i].xyz = rotz.oMultiply(verts[i].xyz);

                //Translate back
                verts[i].xyz.oPluSet(origTrans);
            }

            renderSystem.DrawStretchPic(verts, indexes, 4, 6, shader, (angle != 0));
        }

        public int CharWidth(final char c, float scale) {
            glyphInfo_t glyph;
            float useScale;
            SetFontByScale(scale);
            fontInfo_t font = useFont;
            useScale = scale * font.glyphScale;
            glyph = font.glyphs[c];
            return idMath.FtoiFast(glyph.xSkip * useScale);
        }

        public int TextWidth(final String text, float scale, int limit) {
            int i, width;

            SetFontByScale(scale);
            final glyphInfo_t[] glyphs = useFont.glyphs;

            if (text == null) {
                return 0;
            }

            width = 0;
            if (limit > 0) {
                for (i = 0; text.charAt(i) != '\0' && i < limit; i++) {
                    if (idStr.IsColor(text.substring(i))) {
                        i++;
                    } else {
                        width += glyphs[text.charAt(i)].xSkip;
                    }
                }
            } else {
                for (i = 0; text.charAt(i) != '\0'; i++) {
                    if (idStr.IsColor(text.substring(i))) {
                        i++;
                    } else {
                        width += glyphs[text.charAt(i)].xSkip;
                    }
                }
            }
            return idMath.FtoiFast(scale * useFont.glyphScale * width);
        }

        public int TextWidth(final idStr text, float scale, int limit) {
            return TextWidth(text.toString(), scale, limit);
        }

        public int TextHeight(final String text, float scale, int limit) {
            int len, count;
            float max;
            glyphInfo_t glyph;
            float useScale;
            int s = 0;//text;
            SetFontByScale(scale);
            fontInfo_t font = useFont;

            useScale = scale * font.glyphScale;
            max = 0;
            if (text != null) {
                len = text.length();
                if (limit > 0 && len > limit) {
                    len = limit;
                }

                count = 0;
                while (count < len) {
                    if (idStr.IsColor(text.substring(s))) {
                        s += 2;
//                        continue;
                    } else {
                        glyph = font.glyphs[text.charAt(s)];
                        if (max < glyph.height) {
                            max = glyph.height;
                        }

                        s++;
                        count++;
                    }
                }
            }

            return idMath.FtoiFast(max * useScale);
        }

        public int MaxCharHeight(float scale) {
            SetFontByScale(scale);
            float useScale = scale * useFont.glyphScale;
            return idMath.FtoiFast(activeFont.maxHeight * useScale);
        }

        public int MaxCharWidth(float scale) {
            SetFontByScale(scale);
            float useScale = scale * useFont.glyphScale;
            return idMath.FtoiFast(activeFont.maxWidth * useScale);
        }

        public int FindFont(final String name) {
            int c = fonts.Num();
            for (int i = 0; i < c; i++) {
                if (idStr.Icmp(name, fonts.oGet(i).name) == 0) {
                    return i;
                }
            }

            // If the font was not found, try to register it
            idStr fileName = new idStr(name);
            fileName.Replace("fonts", va("fonts/%s", fontLang));

            fontInfoEx_t fontInfo = new fontInfoEx_t();
            int index = fonts.Append(fontInfo);
            if (renderSystem.RegisterFont(fileName.toString(), fonts.oGet(index))) {
                fonts.oGet(index).name = name;//idStr.Copynz(fonts.oGet(index).name, name, fonts.oGet(index).name.length());
                return index;
            } else {
                common.Printf("Could not register font %s [%s]\n", name, fileName);
                return -1;
            }
        }

        public void SetupFonts() {
            fonts.SetGranularity(1);

            this.fontLang.oSet(cvarSystem.GetCVarString("sys_lang"));
            String font = this.fontLang.toString();

            // western european languages can use the english font
            if ("french".equals(font) || "german".equals(font) || "spanish".equals(font) || "italian".equals(font)) {
                this.fontLang.oSet("english");
            }

            // Default font has to be added first
            FindFont("fonts");
        }

        public idRegion GetTextRegion(final String text, float textScale, idRectangle rectDraw, float xStart, float yStart) {
// if (false){
            // const char	*p, *textPtr, *newLinePtr;
            // char		buff[1024];
            // int			len, textWidth, newLine, newLineWidth;
            // float		y;

            // float charSkip = MaxCharWidth(textScale) + 1;
            // float lineSkip = MaxCharHeight(textScale);
            // textWidth = 0;
            // newLinePtr = NULL;
// }
            return null;
            /*
             if (text == NULL) {
             return;
             }

             textPtr = text;
             if (*textPtr == '\0') {
             return;
             }

             y = lineSkip + rectDraw.y + yStart; 
             len = 0;
             buff[0] = '\0';
             newLine = 0;
             newLineWidth = 0;
             p = textPtr;

             textWidth = 0;
             while (p) {
             if (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\0') {
             newLine = len;
             newLinePtr = p + 1;
             newLineWidth = textWidth;
             }

             if ((newLine && textWidth > rectDraw.w) || *p == '\n' || *p == '\0') {
             if (len) {

             float x = rectDraw.x ;
				
             buff[newLine] = '\0';
             DrawText(x, y, textScale, color, buff, 0, 0, 0);
             if (!wrap) {
             return;
             }
             }

             if (*p == '\0') {
             break;
             }

             y += lineSkip + 5;
             p = newLinePtr;
             len = 0;
             newLine = 0;
             newLineWidth = 0;
             continue;
             }

             buff[len++] = *p++;
             buff[len] = '\0';
             textWidth = TextWidth( buff, textScale, -1 );
             }
             */
        }

        public void SetSize(float width, float height) {
            vidWidth = VIRTUAL_WIDTH;
            vidHeight = VIRTUAL_HEIGHT;
            xScale = yScale = 0;
            if (width != 0 && height != 0) {
                xScale = vidWidth * (1.0f / width);
                yScale = vidHeight * (1.0f / height);
            }
        }

        public idMaterial GetScrollBarImage(int index) {
//if (false){
//	const char	*p, *textPtr, *newLinePtr;
//	char		buff[1024];
//	int			len, textWidth, newLine, newLineWidth;
//	float		y;
//
//	float charSkip = MaxCharWidth(textScale) + 1;
//	float lineSkip = MaxCharHeight(textScale);
//
//	textWidth = 0;
//	newLinePtr = NULL;
//}
            return null;
            /*
             if (text == NULL) {
             return;
             }

             textPtr = text;
             if (*textPtr == '\0') {
             return;
             }

             y = lineSkip + rectDraw.y + yStart; 
             len = 0;
             buff[0] = '\0';
             newLine = 0;
             newLineWidth = 0;
             p = textPtr;

             textWidth = 0;
             while (p) {
             if (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\0') {
             newLine = len;
             newLinePtr = p + 1;
             newLineWidth = textWidth;
             }

             if ((newLine && textWidth > rectDraw.w) || *p == '\n' || *p == '\0') {
             if (len) {

             float x = rectDraw.x ;
				
             buff[newLine] = '\0';
             DrawText(x, y, textScale, color, buff, 0, 0, 0);
             if (!wrap) {
             return;
             }
             }

             if (*p == '\0') {
             break;
             }

             y += lineSkip + 5;
             p = newLinePtr;
             len = 0;
             newLine = 0;
             newLineWidth = 0;
             continue;
             }

             buff[len++] = *p++;
             buff[len] = '\0';
             textWidth = TextWidth( buff, textScale, -1 );
             }
             */
        }

        public void DrawCursor(float[] x, float[] y, float size) {
            float[] s = {size};

            if (x[0] < 0) {
                x[0] = 0;
            }

            if (x[0] >= vidWidth) {
                x[0] = vidWidth;
            }

            if (y[0] < 0) {
                y[0] = 0;
            }

            if (y[0] >= vidHeight) {
                y[0] = vidHeight;
            }

            renderSystem.SetColor(colorWhite);
            AdjustCoords(x, y, s, s);
            DrawStretchPic(x[0], y[0], s[0], s[0], 0, 0, 1, 1, cursorImages[cursor.ordinal()]);
        }

        public void SetCursor(int n) {
            cursor = (n < CURSOR_ARROW.ordinal() || n >= CURSOR_COUNT.ordinal()) ? CURSOR_ARROW : CURSOR.values()[n];
        }

        public void AdjustCoords(float[] x, float[] y, float[] w, float[] h) {
            if (x != null) {
                x[0] *= xScale;
            }
            if (y != null) {
                y[0] *= yScale;
            }
            if (w != null) {
                w[0] *= xScale;
            }
            if (h != null) {
                h[0] *= yScale;
            }
        }

        public boolean ClippedCoords(float[] x, float[] y, float[] w, float[] h) {
            return ClippedCoords(x, y, w, h, null, null, null, null);
        }

        public boolean ClippedCoords(float[] x, float[] y, float[] w, float[] h, float[] s1, float[] t1, float[] s2, float[] t2) {

            if (enableClipping == false || clipRects.Num() == 0) {
                return false;
            }

            int c = clipRects.Num();
            while (--c > 0) {
                idRectangle clipRect = clipRects.oGet(c);

                float ox = x[0];
                float oy = y[0];
                float ow = w[0];
                float oh = h[0];

                if (ow <= 0.0f || oh <= 0.0f) {
                    break;
                }

                if (x[0] < clipRect.x) {
                    w[0] -= clipRect.x - x[0];
                    x[0] = clipRect.x;
                } else if (x[0] > clipRect.x + clipRect.w) {
                    x[0] = w[0] = y[0] = h[0] = 0;
                }
                if (y[0] < clipRect.y) {
                    h[0] -= clipRect.y - y[0];
                    y[0] = clipRect.y;
                } else if (y[0] > clipRect.y + clipRect.h) {
                    x[0] = w[0] = y[0] = h[0] = 0;
                }
                if (w[0] > clipRect.w) {
                    w[0] = clipRect.w - x[0] + clipRect.x;
                } else if (x[0] + w[0] > clipRect.x + clipRect.w) {
                    w[0] = clipRect.Right() - x[0];
                }
                if (h[0] > clipRect.h) {
                    h[0] = clipRect.h - y[0] + clipRect.y;
                } else if (y[0] + h[0] > clipRect.y + clipRect.h) {
                    h[0] = clipRect.Bottom() - y[0];
                }

                if (s1 != null && s2 != null && t1 != null && t2 != null && ow > 0) {
                    float ns1, ns2, nt1, nt2;
                    // upper left
                    float u = (x[0] - ox) / ow;
                    ns1 = s1[0] * (1.0f - u) + s2[0] * (u);

                    // upper right
                    u = (x[0] + w[0] - ox) / ow;
                    ns2 = s1[0] * (1.0f - u) + s2[0] * (u);

                    // lower left
                    u = (y[0] - oy) / oh;
                    nt1 = t1[0] * (1.0f - u) + t2[0] * (u);

                    // lower right
                    u = (y[0] + h[0] - oy) / oh;
                    nt2 = t1[0] * (1.0f - u) + t2[0] * (u);

                    // set values
                    s1[0] = ns1;
                    s2[0] = ns2;
                    t1[0] = nt1;
                    t2[0] = nt2;
                }
            }

            return (w[0] == 0 || h[0] == 0);
        }

        public void PushClipRect(float x, float y, float w, float h) {
            clipRects.Append(new idRectangle(x, y, w, h));
        }

        public void PushClipRect(idRectangle r) {
            clipRects.Append(r);
        }

        public void PopClipRect() {
            if (clipRects.Num() != 0) {
                clipRects.RemoveIndex(clipRects.Num() - 1);
            }
        }

        public void EnableClipping(boolean b) {
            enableClipping = b;
        }

        public void SetFont(int num) {
            if (num >= 0 && num < fonts.Num()) {
                activeFont = fonts.oGet(num);
            } else {
                activeFont = fonts.oGet(0);
            }
        }

        public void SetOverStrike(boolean b) {
            overStrikeMode = b;
        }

        public boolean GetOverStrike() {
            return overStrikeMode;
        }

        public void DrawEditCursor(float x, float y, float scale) {
            if (((com_ticNumber >> 4) & 1) != 0) {
                return;
            }
            SetFontByScale(scale);
            float useScale = scale * useFont.glyphScale;
            final glyphInfo_t glyph2 = useFont.glyphs[(overStrikeMode) ? '_' : '|'];
            float yadj = useScale * glyph2.top;
            PaintChar(x, y - yadj, glyph2.imageWidth, glyph2.imageHeight, useScale, glyph2.s, glyph2.t, glyph2.s2, glyph2.t2, glyph2.glyph);
        }

        public enum CURSOR {

            CURSOR_ARROW,
            CURSOR_HAND,
            CURSOR_COUNT
        };

        public enum ALIGN {

            ALIGN_LEFT,
            ALIGN_CENTER,
            ALIGN_RIGHT
        };

        public enum SCROLLBAR {

            SCROLLBAR_HBACK,
            SCROLLBAR_VBACK,
            SCROLLBAR_THUMB,
            SCROLLBAR_RIGHT,
            SCROLLBAR_LEFT,
            SCROLLBAR_UP,
            SCROLLBAR_DOWN,
            SCROLLBAR_COUNT
        };
        public static idVec4 colorPurple;
        public static idVec4 colorOrange;
        public static idVec4 colorYellow;
        public static idVec4 colorGreen;
        public static idVec4 colorBlue;
        public static idVec4 colorRed;
        public static idVec4 colorWhite;
        public static idVec4 colorBlack;
        public static idVec4 colorNone;

        static int d1 = 0, d2 = 0;

        private int DrawText(float x, float y, float scale, idVec4 color, final String text, float adjust, int limit, int style, int cursor /*= -1*/) {
            int len, count;
            idVec4 newColor;
            glyphInfo_t glyph;
            float useScale;
            SetFontByScale(scale);
            useScale = scale * useFont.glyphScale;
            count = 0;
            if (isNotNullOrEmpty(text) && color.w != 0.0f) {
                char s = text.charAt(0);//(const unsigned char*)text;
                int s_i = 0;
                renderSystem.SetColor(color);
//		memcpy(newColor[0], color[0], sizeof(idVec4));
                newColor = new idVec4(color);
                len = text.length();
                if (limit > 0 && len > limit) {
                    len = limit;
                }

                while (s_i < len && (s = text.charAt(s_i)) != 0 && count < len) {
                    if (s < GLYPH_START || s > GLYPH_END) {
                        s_i++;
                        continue;
                    }
                    glyph = useFont.glyphs[s = text.charAt(s_i)];

                    //
                    // int yadj = Assets.textFont.glyphs[text[i]].bottom +
                    // Assets.textFont.glyphs[text[i]].top; float yadj = scale *
                    // (Assets.textFont.glyphs[text[i]].imageHeight -
                    // Assets.textFont.glyphs[text[i]].height);
                    //
                    if (idStr.IsColor(ctos(s))) {
                        d1++;
                        if (text.charAt(s_i + 1) == C_COLOR_DEFAULT) {
                            newColor = color;
                        } else {
                            newColor = idStr.ColorForIndex(text.charAt(s_i + 1));
                            newColor.oSet(3, color.oGet(3));
                        }
                        if (cursor == count || cursor == count + 1) {
                            float partialSkip = ((glyph.xSkip * useScale) + adjust) / 5.0f;
                            if (cursor == count) {
                                partialSkip *= 2.0f;
                            } else {
                                renderSystem.SetColor(newColor);
                            }
                            DrawEditCursor(x - partialSkip, y, scale);
                        }
                        renderSystem.SetColor(newColor);
                        s_i += 2;
                        count += 2;
                        continue;
                    } else {
                        d2++;
                        float yadj = useScale * glyph.top;
                        PaintChar(x, y - yadj, glyph.imageWidth, glyph.imageHeight, useScale, glyph.s, glyph.t, glyph.s2, glyph.t2, glyph.glyph);

                        if (cursor == count) {
                            DrawEditCursor(x, y, scale);
                        }
                        x += (glyph.xSkip * useScale) + adjust;
                        s_i++;
                        count++;
                    }
                }
                if (cursor == len) {
                    DrawEditCursor(x, y, scale);
                }
            }
            return count;
        }

        private void PaintChar(float x, float y, float width, float height, float scale, float s, float t, float s2, float t2, final idMaterial hShader) {
            float[] w = {width * scale}, h = {height * scale};
            float[] x1 = {x}, y1 = {y};
            float[] s1 = {s}, t1 = {t};
            float[] s3 = {s2}, t3 = {t2};

            if (ClippedCoords(x1, y1, w, h, s1, t1, s3, t3)) {
                return;
            }

            AdjustCoords(x1, y1, w, h);
            DrawStretchPic(x1[0], y1[0], w[0], h[0], s1[0], t1[0], s3[0], t3[0], hShader);
            asdasdasd++;
        }
        static int asdasdasd = 0;

        private void SetFontByScale(float scale) {
            if (scale <= gui_smallFontLimit.GetFloat()) {
                useFont = activeFont.fontInfoSmall;
                activeFont.maxHeight = activeFont.maxHeightSmall;
                activeFont.maxWidth = activeFont.maxWidthSmall;
            } else if (scale <= gui_mediumFontLimit.GetFloat()) {
                useFont = activeFont.fontInfoMedium;
                activeFont.maxHeight = activeFont.maxHeightMedium;
                activeFont.maxWidth = activeFont.maxWidthMedium;
            } else {
                useFont = activeFont.fontInfoLarge;
                activeFont.maxHeight = activeFont.maxHeightLarge;
                activeFont.maxWidth = activeFont.maxWidthLarge;
            }
        }

        private void Clear() {
            initialized = false;
            useFont = null;
            activeFont = null;
            mbcs = false;
        }
    };
}
