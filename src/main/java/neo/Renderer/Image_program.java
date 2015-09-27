package neo.Renderer;

import java.nio.ByteBuffer;
import static neo.Renderer.Image.MAX_IMAGE_NAME;
import neo.Renderer.Image.textureDepth_t;
import static neo.Renderer.Image.textureDepth_t.TD_BUMP;
import static neo.Renderer.Image_files.R_LoadImage;
import static neo.Renderer.Image_process.R_Dropsample;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Token.idToken;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_origin;

/**
 *
 */
public class Image_program {

    // we build a canonical token form of the image program here
    final static StringBuffer parseBuffer = new StringBuffer(MAX_IMAGE_NAME);

    private static final float[][] factors = {
        {1, 1, 1},
        {1, 1, 1},
        {1, 1, 1}
    };

    /*
     ===================
     R_LoadImageProgram
     ===================
     */
    static void R_LoadImageProgram(final String name, ByteBuffer[] pic, int[] width, int[] height, /*ID_TIME_T */ long[] timestamps, textureDepth_t[] depth) {
        idLexer src = new idLexer();

        src.LoadMemory(name, name.length(), name);
        src.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);

        parseBuffer.delete(0, parseBuffer.capacity());
        if (timestamps != null) {
            timestamps[0] = 0;
        }

        R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

        src.FreeSource();
    }

    static void R_LoadImageProgram(final String name, ByteBuffer[] pic, int[] width, int[] height, /*ID_TIME_T */ long[] timestamps) {
        R_LoadImageProgram(name, pic, width, height, timestamps, null);
    }

    /*
     ===================
     R_ParseImageProgram_r

     If pic is NULL, the timestamps will be filled in, but no image will be generated
     If both pic and timestamps are NULL, it will just advance past it, which can be
     used to parse an image program from a text stream.
     ===================
     */
    static boolean R_ParseImageProgram_r(idLexer src, ByteBuffer[] pic, int[] width, int[] height, long[] timestamps, textureDepth_t[] depth) {
        idToken token = new idToken();
        float scale;
        long[] timestamp = {0};

        src.ReadToken(token);
        AppendToken(token);

        if (0 == token.Icmp("heightmap")) {
            MatchAndAppendToken(src, "(");

            if (!R_ParseImageProgram_r(src, pic, width, height, timestamps, depth)) {
                return false;
            }

            MatchAndAppendToken(src, ",");

            src.ReadToken(token);
            AppendToken(token);
            scale = token.GetFloatValue();

            // process it
            if (pic != null) {
                R_HeightmapToNormalMap(pic[0], width[0], height[0], scale);
                if (depth != null) {
                    depth[0] = TD_BUMP;
                }
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("addnormals")) {
            ByteBuffer[] pic2 = {null};
            int[] width2 = {0}, height2 = {0};

            MatchAndAppendToken(src, "(");

            if (!R_ParseImageProgram_r(src, pic, width, height, timestamps, depth)) {
                return false;
            }

            MatchAndAppendToken(src, ",");

            if (!R_ParseImageProgram_r(src, pic != null ? pic2 : null, width2, height2, timestamps, depth)) {
                if (pic != null) {
                    pic[0] = null;//R_StaticFree(pic);
                }
                return false;
            }

            // process it
            if (pic != null) {
                R_AddNormalMaps(pic[0], width[0], height[0], pic2[0], width2[0], height2[0]);
//                R_StaticFree(pic2);
                if (depth != null) {
                    depth[0] = TD_BUMP;
                }
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("smoothnormals")) {
            MatchAndAppendToken(src, "(");

            if (!R_ParseImageProgram_r(src, pic, width, height, timestamps, depth)) {
                return false;
            }

            if (pic != null) {
                R_SmoothNormalMap(pic[0], width[0], height[0]);
                if (depth != null) {
                    depth[0] = TD_BUMP;
                }
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("add")) {
            ByteBuffer[] pic2 = {null};
            int[] width2 = {0}, height2 = {0};

            MatchAndAppendToken(src, "(");

            if (!R_ParseImageProgram_r(src, pic, width, height, timestamps, depth)) {
                return false;
            }

            MatchAndAppendToken(src, ",");

            if (!R_ParseImageProgram_r(src, pic[0] != null ? pic2 : null, width2, height2, timestamps, depth)) {
                if (pic != null) {
                    pic[0] = null;//R_StaticFree(pic[0]);
                }
                return false;
            }

            // process it
            if (pic != null) {
                R_ImageAdd(pic[0], width[0], height[0], pic2[0], width2[0], height2[0]);
//                R_StaticFree(pic2);
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("scale")) {
            final float[] scale2 = new float[4];
            int i;

            MatchAndAppendToken(src, "(");

            R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

            for (i = 0; i < 4; i++) {
                MatchAndAppendToken(src, ",");
                src.ReadToken(token);
                AppendToken(token);
                scale2[i] = token.GetFloatValue();
            }

            // process it
            if (pic != null) {
                R_ImageScale(pic[0], width[0], height[0], scale2);
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("invertAlpha")) {
            MatchAndAppendToken(src, "(");

            R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

            // process it
            if (pic != null) {
                R_InvertAlpha(pic[0], width[0], height[0]);
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("invertColor")) {
            MatchAndAppendToken(src, "(");

            R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

            // process it
            if (pic != null) {
                R_InvertColor(pic[0], width[0], height[0]);
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("makeIntensity")) {
            int i;

            MatchAndAppendToken(src, "(");

            R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

            // copy red to green, blue, and alpha
            if (pic != null) {
                int c;
                c = width[0] * height[0] * 4;
                pic[0].position(0);
                for (i = 0; i < c; i += 4) {
                    final byte r = pic[0].get(i);
                    final byte[] rgba = {r, r, r, r};
                    pic[0].put(rgba);
                }
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        if (0 == token.Icmp("makeAlpha")) {
            int i;

            MatchAndAppendToken(src, "(");

            R_ParseImageProgram_r(src, pic, width, height, timestamps, depth);

            // average RGB into alpha, then set RGB to white
            if (pic != null) {
                int c;
                pic[0].position(0);
                c = width[0] * height[0] * 4;
                for (i = 0; i < c; i += 4) {
                    final byte[] rgb = {(byte) 255, (byte) 255, (byte) 255};
                    pic[0].put(i + 3, (byte) ((pic[0].get(i + 0) + pic[0].get(i + 1) + pic[0].get(i + 2)) / 3));
                    pic[0].put(rgb);
                }
            }

            MatchAndAppendToken(src, ")");
            return true;
        }

        // if we are just parsing instead of loading or checking,
        // don't do the R_LoadImage
        if (null == timestamps && null == pic) {
            return true;
        }

        // load it as an image
        R_LoadImage(token.toString(), pic, width, height, timestamp, true);

        if (timestamp[0] == -1) {
            return false;
        }

        // add this to the timestamp
        if (timestamps != null) {
            if (timestamp[0] > timestamps[0]) {
                timestamps[0] = timestamp[0];
            }
        }

        return true;
    }

    /*
     ===================
     AppendToken
     ===================
     */
    static void AppendToken(idToken token) {
        // add a leading space if not at the beginning
        if (parseBuffer.length() > 0) {
//            idStr.Append(parseBuffer, MAX_IMAGE_NAME, " ");
            parseBuffer.append(" ");
        }
//        idStr.Append(parseBuffer, MAX_IMAGE_NAME, token.toString());
        parseBuffer.append(token.toString());
    }

    /*
     ===================
     MatchAndAppendToken
     ===================
     */
    static void MatchAndAppendToken(idLexer src, final String match) {
        if (!src.ExpectTokenString(match)) {
            return;
        }
        // a matched token won't need a leading space
//        idStr.Append(parseBuffer, MAX_IMAGE_NAME, match);
        parseBuffer.append(match);
    }

    /*
     =================
     R_HeightmapToNormalMap

     it is not possible to convert a heightmap into a normal map
     properly without knowing the texture coordinate stretching.
     We can assume constant and equal ST vectors for walls, but not for characters.
     =================
     */
    static void R_HeightmapToNormalMap(ByteBuffer data, int width, int height, float scale) {
        int i, j;
        byte[] depth;

        scale = scale / 256;

        // copy and convert to grey scale
        j = width * height;
        depth = new byte[j];//R_StaticAlloc(j);
        for (i = 0; i < j; i++) {
            depth[i] = (byte) ((data.get(i * 4) + data.get(i * 4 + 1) + data.get(i * 4 + 2)) / 3);
        }

        idVec3 dir = new idVec3(), dir2 = new idVec3();
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                int d1, d2, d3, d4;
                int a1, a2, a3, a4;

                // FIXME: look at five points?
                // look at three points to estimate the gradient
                a1 = d1 = depth[ (i * width + j)];
                a2 = d2 = depth[ (i * width + ((j + 1) & (width - 1)))];
                a3 = d3 = depth[ (((i + 1) & (height - 1)) * width + j)];
                a4 = d4 = depth[ (((i + 1) & (height - 1)) * width + ((j + 1) & (width - 1)))];

                d2 -= d1;
                d3 -= d1;

                dir.oSet(0, -d2 * scale);
                dir.oSet(1, -d3 * scale);
                dir.oSet(2, 1);
                dir.NormalizeFast();

                a1 -= a3;
                a4 -= a3;

                dir2.oSet(0, -a4 * scale);
                dir2.oSet(1, a1 * scale);
                dir2.oSet(2, 1);
                dir2.NormalizeFast();

                dir.oPluSet(dir2);
                dir.NormalizeFast();

                a1 = (i * width + j) * 4;
                data.put(a1 + 0, (byte) (dir.oGet(0) * 127 + 128));
                data.put(a1 + 1, (byte) (dir.oGet(1) * 127 + 128));
                data.put(a1 + 2, (byte) (dir.oGet(2) * 127 + 128));
                data.put(a1 + 3, (byte) 255);
            }
        }

//        R_StaticFree(depth);
    }

    /*
     ================
     R_SmoothNormalMap
     ================
     */
    static void R_SmoothNormalMap(ByteBuffer data, int width, int height) {
        byte[] orig;
        int i, j, k, l;
        idVec3 normal;
        int out;

        orig = new byte[width * height * 4];// R_StaticAlloc(width * height * 4);
//	memcpy( orig, data, width * height * 4 );
        System.arraycopy(data.array(), 0, orig, 0, width * height * 4);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {
                normal = vec3_origin;
                for (k = -1; k < 2; k++) {
                    for (l = -1; l < 2; l++) {
                        int in;

                        in = /*orig +*/ (((j + l) & (height - 1)) * width + ((i + k) & (width - 1))) * 4;

                        // ignore 000 and -1 -1 -1
                        if (orig[in + 0] == 0 && orig[in + 1] == 0 && orig[in + 2] == 0) {
                            continue;
                        }
                        if (orig[in + 0] == 128 && orig[in + 1] == 128 && orig[in + 2] == 128) {
                            continue;
                        }

                        normal.oPluSet(0, factors[k + 1][l + 1] * (orig[in + 0] - 128));
                        normal.oPluSet(1, factors[k + 1][l + 1] * (orig[in + 1] - 128));
                        normal.oPluSet(2, factors[k + 1][l + 1] * (orig[in + 2] - 128));
                    }
                }
                normal.Normalize();
                out = /*data +*/ (j * width + i) * 4;
                data.put(out + 0, (byte) (128 + 127 * normal.oGet(0)));
                data.put(out + 1, (byte) (128 + 127 * normal.oGet(1)));
                data.put(out + 2, (byte) (128 + 127 * normal.oGet(2)));
            }
        }

//        R_StaticFree(orig);
    }

    /*
     ===================
     R_ImageAdd

     ===================
     */
    static void R_ImageAdd(ByteBuffer data1, int width1, int height1, ByteBuffer data2, int width2, int height2) {
        int i, j;
        final int c;
        byte[] newMap;

        // resample pic2 to the same size as pic1
        if (width2 != width1 || height2 != height1) {
            newMap = R_Dropsample(data2, width2, height2, width1, height1);
            data2.put(newMap);//TODO:not overwrite reference. EDIT:is this enough?
        } else {
            newMap = null;
        }

        c = width1 * height1 * 4;

        for (i = 0; i < c; i++) {
            j = data1.get(i) + data2.get(i);
            if (j > 255) {
                j = 255;
            }
            data1.put(i, (byte) j);
        }

//        if (newMap != null) {
//            R_StaticFree(newMap);
//        }
    }

    /*
     =================
     R_ImageScale
     =================
     */
    static void R_ImageScale(ByteBuffer data, int width, int height, float[] scale/*[4]*/) {
        int i, j;
        final int c;

        c = width * height * 4;

        for (i = 0; i < c; i++) {
            j = (byte) (data.get(i) * scale[i & 3]);
            if (j < 0) {
                j = 0;
            } else if (j > 255) {
                j = 255;
            }
            data.put(i, (byte) j);
        }
    }

    /*
     =================
     R_InvertAlpha
     =================
     */
    static void R_InvertAlpha(ByteBuffer data, int width, int height) {
        int i;
        final int c;

        c = width * height * 4;

        for (i = 0; i < c; i += 4) {
            data.put(i + 3, (byte) (255 - data.get(i + 3)));
        }
    }

    /*
     =================
     R_InvertColor
     =================
     */
    static void R_InvertColor(ByteBuffer data, int width, int height) {
        int i;
        final int c;

        c = width * height * 4;

        for (i = 0; i < c; i += 4) {
            data.put(i + 0, (byte) (255 - data.get(i + 0)));
            data.put(i + 1, (byte) (255 - data.get(i + 1)));
            data.put(i + 2, (byte) (255 - data.get(i + 2)));
        }
    }

    /*
     ===================
     R_AddNormalMaps

     ===================
     */
    static void R_AddNormalMaps(ByteBuffer data1, int width1, int height1, ByteBuffer data2, int width2, int height2) {
        int i, j;
        byte[] newMap;

        // resample pic2 to the same size as pic1
        if (width2 != width1 || height2 != height1) {
            newMap = R_Dropsample(data2, width2, height2, width1, height1);
            data2.put(newMap);
        } else {
            newMap = null;
        }

        // add the normal change from the second and renormalize
        for (i = 0; i < height1; i++) {
            for (j = 0; j < width1; j++) {
                int d1, d2;
                idVec3 n = new idVec3();
                float len;

                d1 =/* data1 + */ (i * width1 + j) * 4;
                d2 = /*data2 + */ (i * width1 + j) * 4;

                n.oSet(0, (data1.get(d1 + 0) - 128) / 127.0f);
                n.oSet(1, (data1.get(d1 + 1) - 128) / 127.0f);
                n.oSet(2, (data1.get(d1 + 2) - 128) / 127.0f);

                // There are some normal maps that blend to 0,0,0 at the edges
                // this screws up compression, so we try to correct that here by instead fading it to 0,0,1
                len = n.LengthFast();
                if (len < 1.0f) {
                    n.oSet(2, idMath.Sqrt(1.0f - (n.oGet(0) * n.oGet(0)) - (n.oGet(1) * n.oGet(1))));
                }

                n.oPluSet(0, (data2.get(d2 + 0) - 128) / 127.0f);
                n.oPluSet(1, (data2.get(d2 + 1) - 128) / 127.0f);
                n.Normalize();

                data1.put(d1 + 0, (byte) (n.oGet(0) * 127 + 128));
                data1.put(d1 + 1, (byte) (n.oGet(1) * 127 + 128));
                data1.put(d1 + 2, (byte) (n.oGet(2) * 127 + 128));
                data1.put(d1 + 3, (byte) 255);
            }
        }

//        if (newMap != null) {
//            R_StaticFree(newMap);
//        }
    }

    /*
     ===================
     R_ParsePastImageProgram
     ===================
     */
    static String R_ParsePastImageProgram(idLexer src) {
        parseBuffer.delete(0, parseBuffer.capacity());
        R_ParseImageProgram_r(src, null, null, null, null, null);
        return new String(parseBuffer);
    }
}
