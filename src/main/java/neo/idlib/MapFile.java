package neo.idlib;

import neo.framework.File_h.idFile;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.MapFile.idMapBrush;
import neo.idlib.MapFile.idMapBrushSide;
import neo.idlib.MapFile.idMapPrimitive;
import static neo.idlib.MapFile.idMapPrimitive.TYPE_BRUSH;
import static neo.idlib.MapFile.idMapPrimitive.TYPE_INVALID;
import static neo.idlib.MapFile.idMapPrimitive.TYPE_PATCH;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_STRING;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class MapFile {
    /*
     ===============================================================================

     Reads or writes the contents of .map files into a standard internal
     format, which can then be moved into private formats for collision
     detection, map processing, or editor use.

     No validation (duplicate planes, null area brushes, etc) is performed.
     There are no limits to the number of any of the elements in maps.
     The order of entities, brushes, and sides is maintained.

     ===============================================================================
     */

    public static final int   OLD_MAP_VERSION             = 1;
    public static final int   CURRENT_MAP_VERSION         = 2;
    public static final int   DEFAULT_CURVE_SUBDIVISION   = 4;
    public static final float DEFAULT_CURVE_MAX_ERROR     = 4.0f;
    public static final float DEFAULT_CURVE_MAX_ERROR_CD  = 24.0f;
    public static final float DEFAULT_CURVE_MAX_LENGTH    = -1.0f;
    public static final float DEFAULT_CURVE_MAX_LENGTH_CD = -1.0f;

    public static class idMapPrimitive {

        protected int type;
        //
        //

        //	enum { TYPE_INVALID = -1, TYPE_BRUSH, TYPE_PATCH };
        public static final int TYPE_INVALID = -1, TYPE_BRUSH = 0, TYPE_PATCH = 1;
        public idDict epairs;

        public idMapPrimitive() {
            type = TYPE_INVALID;
        }
//public	virtual					~idMapPrimitive( void ) { }

        public int GetType() {
            return type;
        }
    };

    public static class idMapBrushSide {
//	friend class idMapBrush;

        protected idStr   material;
        protected idPlane plane;
        protected idVec3[] texMat = new idVec3[2];
        protected idVec3 origin;
        //
        //

        public idMapBrushSide() {
            plane.Zero();
            texMat[0].Zero();
            texMat[1].Zero();
            origin.Zero();
        }
//public							~idMapBrushSide( void ) { }

        public idStr GetMaterial() {
            return material;
        }

        public void SetMaterial(final String p) {
            material = new idStr(p);
        }

        public idPlane GetPlane() {
            return plane;
        }

        public void SetPlane(final idPlane p) {
            plane = p;
        }

        public void SetTextureMatrix(final idVec3[] mat) {
            texMat[0] = mat[0];
            texMat[1] = mat[1];
        }

        public void GetTextureMatrix(idVec3[] mat1, idVec3[] mat2) {
            mat1[0] = texMat[0];
            mat2[0] = texMat[1];
        }

        public void GetTextureVectors(idVec4[] v) {
            int i;
            idVec3 texX = new idVec3(), texY = new idVec3();

            ComputeAxisBase(plane.Normal(), texX, texY);
            for (i = 0; i < 2; i++) {
                v[i].oSet(0, texX.oGet(0) * texMat[i].oGet(0) + texY.oGet(0) * texMat[i].oGet(1));
                v[i].oSet(1, texX.oGet(1) * texMat[i].oGet(0) + texY.oGet(1) * texMat[i].oGet(1));
                v[i].oSet(2, texX.oGet(2) * texMat[i].oGet(0) + texY.oGet(2) * texMat[i].oGet(1));
                v[i].oSet(3, texMat[i].oGet(2) + (origin.oMultiply(v[i].ToVec3())));
            }
        }
    };

    /*
     =================
     ComputeAxisBase

     WARNING : special case behaviour of atan2(y,x) <-> atan(y/x) might not be the same everywhere when x == 0
     rotation by (0,RotY,RotZ) assigns X to normal
     =================
     */
    static void ComputeAxisBase(final idVec3 normal, idVec3 texS, idVec3 texT) {
        double RotY, RotZ;
        idVec3 n = new idVec3();

        // do some cleaning
        n.oSet(0, (idMath.Fabs(normal.oGet(0)) < 1e-6f) ? 0.0f : normal.oGet(0));
        n.oSet(1, (idMath.Fabs(normal.oGet(1)) < 1e-6f) ? 0.0f : normal.oGet(1));
        n.oSet(2, (idMath.Fabs(normal.oGet(2)) < 1e-6f) ? 0.0f : normal.oGet(2));

        RotY = -Math.atan2(n.oGet(2), idMath.Sqrt(n.oGet(1) * n.oGet(1) + n.oGet(0) * n.oGet(0)));
        RotZ = Math.atan2(n.oGet(1), n.oGet(0));
        // rotate (0,1,0) and (0,0,1) to compute texS and texT
        texS.oSet(0, (float) -Math.sin(RotZ));
        texS.oSet(1, (float) Math.cos(RotZ));
        texS.oSet(2, 0);
        // the texT vector is along -Z ( T texture coorinates axis )
        texT.oSet(0, (float) (-Math.sin(RotY) * Math.cos(RotZ)));
        texT.oSet(1, (float) (-Math.sin(RotY) * Math.sin(RotZ)));
        texT.oSet(2, (float) -Math.cos(RotY));
    }

    public static class idMapBrush extends idMapPrimitive {

        public idMapBrush() {
            type = TYPE_BRUSH;
            sides.Resize(8, 4);
        }
//public							~idMapBrush( void ) { sides.DeleteContents( true ); }

//public	static idMapBrush *		Parse( idLexer &src, const idVec3 &origin, bool newFormat = true, float version = CURRENT_MAP_VERSION );
        public static idMapBrush Parse(idLexer src, final idVec3 origin, boolean newFormat, float version) throws idException {
            int i;
            idVec3[] planepts = new idVec3[3];
            idToken token = new idToken();
            idList<idMapBrushSide> sides = new idList<>();
            idMapBrushSide side;
            idDict epairs = new idDict();

            if (src.ExpectTokenString("{")) {
                return null;
            }

            do {
                if (!src.ReadToken(token)) {
                    src.Error("idMapBrush::Parse: unexpected EOF");
                    sides.DeleteContents(true);
                    return null;
                }
                if (token.equals("}")) {
                    break;
                }

                // here we may have to jump over brush epairs ( only used in editor )
                do {
                    // if token is a brace
                    if (token.equals("(")) {
                        break;
                    }
                    // the token should be a key string for a key/value pair
                    if (token.type != TT_STRING) {
                        src.Error("idMapBrush::Parse: unexpected %s, expected ( or epair key string", token);
                        sides.DeleteContents(true);
                        return null;
                    }

                    idStr key = token;

                    if (!src.ReadTokenOnLine(token) || token.type != TT_STRING) {
                        src.Error("idMapBrush::Parse: expected epair value string not found");
                        sides.DeleteContents(true);
                        return null;
                    }

                    epairs.Set(key, token);

                    // try to read the next key
                    if (!src.ReadToken(token)) {
                        src.Error("idMapBrush::Parse: unexpected EOF");
                        sides.DeleteContents(true);
                        return null;
                    }
                } while (true);

                src.UnreadToken(token);

                side = new idMapBrushSide();
                sides.Append(side);

                if (newFormat) {
                    if (!src.Parse1DMatrix(4, side.plane.ToFloatPtr())) {
                        src.Error("idMapBrush::Parse: unable to read brush side plane definition");
                        sides.DeleteContents(true);
                        return null;
                    }
                } else {
                    // read the three point plane definition
                    if (!src.Parse1DMatrix(3, planepts[0].ToFloatPtr())
                            || !src.Parse1DMatrix(3, planepts[1].ToFloatPtr())
                            || !src.Parse1DMatrix(3, planepts[2].ToFloatPtr())) {
                        src.Error("idMapBrush::Parse: unable to read brush side plane definition");
                        sides.DeleteContents(true);
                        return null;
                    }

                    planepts[0].oMinSet(origin);
                    planepts[1].oMinSet(origin);
                    planepts[2].oMinSet(origin);

                    side.plane.FromPoints(planepts[0], planepts[1], planepts[2]);
                }

                // read the texture matrix
                // this is odd, because the texmat is 2D relative to default planar texture axis
                if (!src.Parse2DMatrix(2, 3, side.texMat[0].ToFloatPtr())) {
                    src.Error("idMapBrush::Parse: unable to read brush side texture matrix");
                    sides.DeleteContents(true);
                    return null;
                }
                side.origin = origin;

                // read the material
                if (!src.ReadTokenOnLine(token)) {
                    src.Error("idMapBrush::Parse: unable to read brush side material");
                    sides.DeleteContents(true);
                    return null;
                }

                // we had an implicit 'textures/' in the old format...
                if (version < 2.0f) {
                    side.material = new idStr("textures/" + token.toString());
                } else {
                    side.material = token;
                }

                // Q2 allowed override of default flags and values, but we don't any more
                if (src.ReadTokenOnLine(token)) {
                    if (src.ReadTokenOnLine(token)) {
                        if (src.ReadTokenOnLine(token)) {
                        }
                    }
                }
            } while (true);

            if (!src.ExpectTokenString("}")) {
                sides.DeleteContents(true);
                return null;
            }

            idMapBrush brush = new idMapBrush();
            for (i = 0; i < sides.Num(); i++) {
                brush.AddSide(sides.oGet(i));
            }

            brush.epairs = epairs;

            return brush;
        }

        public static idMapBrush ParseQ3(idLexer src, final idVec3 origin) throws idException {
            int i, rotate;
            int[] shift = new int[2];
            float[] scale = new float[2];
            idVec3[] planepts = new idVec3[3];
            idToken token = new idToken();
            idList<idMapBrushSide> sides = new idList<>();
            idMapBrushSide side;
            idDict epairs = new idDict();

            do {
                if (src.CheckTokenString("}")) {
                    break;
                }

                side = new idMapBrushSide();
                sides.Append(side);

                // read the three point plane definition
                if (!src.Parse1DMatrix(3, planepts[0].ToFloatPtr())
                        || !src.Parse1DMatrix(3, planepts[1].ToFloatPtr())
                        || !src.Parse1DMatrix(3, planepts[2].ToFloatPtr())) {
                    src.Error("idMapBrush::ParseQ3: unable to read brush side plane definition");
                    sides.DeleteContents(true);
                    return null;
                }

                planepts[0].oMinSet(origin);
                planepts[1].oMinSet(origin);
                planepts[2].oMinSet(origin);

                side.plane.FromPoints(planepts[0], planepts[1], planepts[2]);

                // read the material
                if (!src.ReadTokenOnLine(token)) {
                    src.Error("idMapBrush::ParseQ3: unable to read brush side material");
                    sides.DeleteContents(true);
                    return null;
                }

                // we have an implicit 'textures/' in the old format
                side.material = new idStr("textures/" + token.toString());

                // read the texture shift, rotate and scale
                shift[0] = src.ParseInt();
                shift[1] = src.ParseInt();
                rotate = src.ParseInt();
                scale[0] = src.ParseFloat();
                scale[1] = src.ParseFloat();
                side.texMat[0] = new idVec3(0.03125f, 0.0f, 0.0f);
                side.texMat[1] = new idVec3(0.0f, 0.03125f, 0.0f);
                side.origin = origin;

                // Q2 allowed override of default flags and values, but we don't any more
                if (src.ReadTokenOnLine(token)) {
                    if (src.ReadTokenOnLine(token)) {
                        if (src.ReadTokenOnLine(token)) {
                        }
                    }
                }
            } while (true);

            idMapBrush brush = new idMapBrush();
            for (i = 0; i < sides.Num(); i++) {
                brush.AddSide(sides.oGet(i));
            }

            brush.epairs = epairs;

            return brush;
        }

        public boolean Write(idFile fp, int primitiveNum, final idVec3 origin) {
            int i;
            idMapBrushSide side;

            fp.WriteFloatString("// primitive %d\n{\n brushDef3\n {\n", primitiveNum);

            // write brush epairs
            for (i = 0; i < epairs.GetNumKeyVals(); i++) {
                fp.WriteFloatString("  \"%s\" \"%s\"\n", epairs.GetKeyVal(i).GetKey(), epairs.GetKeyVal(i).GetValue());
            }

            // write brush sides
            for (i = 0; i < GetNumSides(); i++) {
                side = GetSide(i);
                fp.WriteFloatString("  ( %f %f %f %f ) ", side.plane.oGet(0), side.plane.oGet(1), side.plane.oGet(2), side.plane.oGet(3));
                fp.WriteFloatString("( ( %f %f %f ) ( %f %f %f ) ) \"%s\" 0 0 0\n",
                        side.texMat[0].oGet(0), side.texMat[0].oGet(1), side.texMat[0].oGet(2),
                        side.texMat[1].oGet(0), side.texMat[1].oGet(1), side.texMat[1].oGet(2),
                        side.material);
            }

            fp.WriteFloatString(" }\n}\n");

            return true;
        }

        public int GetNumSides() {
            return sides.Num();
        }

        public int AddSide(idMapBrushSide side) {
            return sides.Append(side);
        }

        public idMapBrushSide GetSide(int i) {
            return sides.oGet(i);
        }

        public int GetGeometryCRC() {
            int i, j;
            idMapBrushSide mapSide;
            int crc;

            crc = 0;
            for (i = 0; i < GetNumSides(); i++) {
                mapSide = GetSide(i);
                for (j = 0; j < 4; j++) {
                    crc ^= FloatCRC(mapSide.GetPlane().oGet(j));
                }
                crc ^= StringCRC(mapSide.GetMaterial().toString());
            }

            return crc;
        }
//
//
        protected int numSides;
        protected idList<idMapBrushSide> sides;
    };

    static int FloatCRC(float f) {
        return (int) f;
    }

    static int StringCRC(final String str) {
        int i, crc;

        crc = 0;
        for (i = 0; i < str.length(); i++) {
            crc ^= str.charAt(i) << (i & 3);
        }
        return crc;
    }

    public static class idMapPatch extends idMapPrimitive {

        protected idStr material;
        protected int horzSubdivisions;
        protected int vertSubdivisions;
        protected boolean explicitSubdivisions;
        //
        //

        public idMapPatch() {
            type = TYPE_PATCH;
            horzSubdivisions = vertSubdivisions = 0;
            explicitSubdivisions = false;
            width = height = 0;
            maxWidth = maxHeight = 0;
            expanded = false;
        }

        public idMapPatch(int maxPatchWidth, int maxPatchHeight) {
            type = TYPE_PATCH;
            horzSubdivisions = vertSubdivisions = 0;
            explicitSubdivisions = false;
            width = height = 0;
            maxWidth = maxPatchWidth;
            maxHeight = maxPatchHeight;
            verts.SetNum(maxWidth * maxHeight);
            expanded = false;
        }

        public idMapPatch(idMapPrimitive mapPrimitive) {
            this.epairs = mapPrimitive.epairs;
            this.type = mapPrimitive.type;

        }

//public							~idMapPatch( void ) { }
//public	static idMapPatch *		Parse( idLexer &src, const idVec3 &origin, bool patchDef3 = true, float version = CURRENT_MAP_VERSION );
        public static idMapPatch Parse(idLexer src, final idVec3 origin, boolean patchDef3, float version) throws idException {
            float[] info = new float[7];
            idDrawVert vert;
            idToken token = new idToken();
            int i, j;

            if (src.ExpectTokenString("{")) {
                return null;
            }

            // read the material (we had an implicit 'textures/' in the old format...)
            if (!src.ReadToken(token)) {
                src.Error("idMapPatch::Parse: unexpected EOF");
                return null;
            }

            // Parse it
            if (patchDef3) {
                if (!src.Parse1DMatrix(7, info)) {
                    src.Error("idMapPatch::Parse: unable to Parse patchDef3 info");
                    return null;
                }
            } else {
                if (!src.Parse1DMatrix(5, info)) {
                    src.Error("idMapPatch::Parse: unable to parse patchDef2 info");
                    return null;
                }
            }

            idMapPatch patch = new idMapPatch((int) info[0], (int) info[1]);
            patch.SetSize((int) info[0], (int) info[1]);
            if (version < 2.0f) {
                patch.SetMaterial("textures/" + token);
            } else {
                patch.SetMaterial("" + token);//TODO:accept Objects, and cast within
            }

            if (patchDef3) {
                patch.SetHorzSubdivisions((int) info[2]);
                patch.SetVertSubdivisions((int) info[3]);
                patch.SetExplicitlySubdivided(true);
            }

            if (patch.GetWidth() < 0 || patch.GetHeight() < 0) {
                src.Error("idMapPatch::Parse: bad size");
//		delete patch;
                return null;
            }

            // these were written out in the wrong order, IMHO
            if (src.ExpectTokenString("(")) {
                src.Error("idMapPatch::Parse: bad patch vertex data");
//		delete patch;
                return null;
            }
            for (j = 0; j < patch.GetWidth(); j++) {
                if (src.ExpectTokenString("(")) {
                    src.Error("idMapPatch::Parse: bad vertex row data");
//			delete patch;
                    return null;
                }
                for (i = 0; i < patch.GetHeight(); i++) {
                    float[] v = new float[5];

                    if (!src.Parse1DMatrix(5, v)) {
                        src.Error("idMapPatch::Parse: bad vertex column data");
//				delete patch;
                        return null;
                    }

//                    vert = patch.oGet(i * patch.GetWidth() + j);
                    vert = patch.verts.oGet(i * patch.GetWidth() + j);
                    vert.xyz.oSet(0, v[0] - origin.oGet(0));
                    vert.xyz.oSet(1, v[1] - origin.oGet(1));
                    vert.xyz.oSet(2, v[2] - origin.oGet(2));
                    vert.st.oSet(0, v[3]);
                    vert.st.oSet(1, v[4]);
                }
                if (src.ExpectTokenString(")")) {
//			delete patch;
                    src.Error("idMapPatch::Parse: unable to parse patch control points");
                    return null;
                }
            }
            if (src.ExpectTokenString(")")) {
                src.Error("idMapPatch::Parse: unable to parse patch control points, no closure");
//		delete patch;
                return null;
            }

            // read any key/value pairs
            while (src.ReadToken(token)) {
                if (token.equals("}")) {
                    src.ExpectTokenString("}");
                    break;
                }
                if (token.type == TT_STRING) {
                    idStr key = token;
                    src.ExpectTokenType(TT_STRING, 0, token);
                    patch.epairs.Set(key.toString(), token.toString());
                }
            }

            return patch;
        }

        public boolean Write(idFile fp, int primitiveNum, final idVec3 origin) {
            int i, j;
            idDrawVert v;

            if (GetExplicitlySubdivided()) {
                fp.WriteFloatString("// primitive %d\n{\n patchDef3\n {\n", primitiveNum);
                fp.WriteFloatString("  \"%s\"\n  ( %d %d %d %d 0 0 0 )\n", GetMaterial(), GetWidth(), GetHeight(), GetHorzSubdivisions(), GetVertSubdivisions());
            } else {
                fp.WriteFloatString("// primitive %d\n{\n patchDef2\n {\n", primitiveNum);
                fp.WriteFloatString("  \"%s\"\n  ( %d %d 0 0 0 )\n", GetMaterial(), GetWidth(), GetHeight());
            }

            fp.WriteFloatString("  (\n");
            for (i = 0; i < GetWidth(); i++) {
                fp.WriteFloatString("   ( ");
                for (j = 0; j < GetHeight(); j++) {
                    v = verts.oGet(j * GetWidth() + i);
                    fp.WriteFloatString(" ( %f %f %f %f %f )",
                            v.xyz.oGet(0) + origin.oGet(0),
                            v.xyz.oGet(1) + origin.oGet(1),
                            v.xyz.oGet(2) + origin.oGet(2),
                            v.st.oGet(0),
                            v.st.oGet(1));
                }
                fp.WriteFloatString(" )\n");
            }
            fp.WriteFloatString("  )\n }\n}\n");

            return true;
        }

        public idStr GetMaterial() {
            return material;
        }

        public void SetMaterial(final String p) {
            material = new idStr(p);
        }

        public int GetHorzSubdivisions() {
            return horzSubdivisions;
        }

        public int GetVertSubdivisions() {
            return vertSubdivisions;
        }

        public boolean GetExplicitlySubdivided() {
            return explicitSubdivisions;
        }

        public void SetHorzSubdivisions(int n) {
            horzSubdivisions = n;
        }

        public void SetVertSubdivisions(int n) {
            vertSubdivisions = n;
        }

        public void SetExplicitlySubdivided(boolean b) {
            explicitSubdivisions = b;
        }

        public int GetGeometryCRC() {
            int i, j;
            int crc;

            crc = GetHorzSubdivisions() ^ GetVertSubdivisions();
            for (i = 0; i < GetWidth(); i++) {
                for (j = 0; j < GetHeight(); j++) {
                    crc ^= FloatCRC(verts.oGet(j * GetWidth() + i).xyz.x);
                    crc ^= FloatCRC(verts.oGet(j * GetWidth() + i).xyz.y);
                    crc ^= FloatCRC(verts.oGet(j * GetWidth() + i).xyz.z);
                }
            }

            crc ^= StringCRC(GetMaterial().toString());

            return crc;
        }

        /**
         * i d S u r f a c e_-_P a t c h
         */
        protected int width;			// width of patch
        protected int height;			// height of patch
        protected int maxWidth;		// maximum width allocated for
        protected int maxHeight;		// maximum height allocated for
        protected boolean expanded;		// true if vertices are spaced out
        protected idList<idDrawVert> verts;			// vertices

        public int GetWidth() {
            return width;
        }

        public int GetHeight() {
            return height;
        }

        public void SetSize(int patchWidth, int patchHeight) throws idException {
            if (patchWidth < 1 || patchWidth > maxWidth) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchWidth");
            }
            if (patchHeight < 1 || patchHeight > maxHeight) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchHeight");
            }
            width = patchWidth;
            height = patchHeight;
            verts.SetNum(width * height, false);
        }
    };

    public static class idMapEntity {
//	friend class			idMapFile;
//
//

        public idDict epairs;
        //
        protected idList<idMapPrimitive> primitives;
        //
        //

        public idMapEntity() {
            epairs.SetHashSize(64);
        }
//public							~idMapEntity( void ) { primitives.DeleteContents( true ); }
//public	static idMapEntity *	Parse( idLexer &src, bool worldSpawn = false, float version = CURRENT_MAP_VERSION );

        public static idMapEntity Parse(idLexer src, boolean worldSpawn, float version) throws idException {
            idToken token = new idToken();
            idMapEntity mapEnt;
            idMapPatch mapPatch;
            idMapBrush mapBrush;
            boolean worldent;
            idVec3 origin = new idVec3();
            float v1, v2, v3;

            if (!src.ReadToken(token)) {
                return null;
            }

            if (!token.equals("{")) {
                src.Error("idMapEntity::Parse: { not found, found %s", token/*c_str()*/);
                return null;
            }

            mapEnt = new idMapEntity();

            if (worldSpawn) {
                mapEnt.primitives.Resize(1024, 256);
            }

            origin.Zero();
            worldent = false;
            do {
                if (!src.ReadToken(token)) {
                    src.Error("idMapEntity::Parse: EOF without closing brace");
                    return null;
                }
                if (token.equals("}")) {
                    break;
                }

                if (token.equals("{")) {
                    // parse a brush or patch
                    if (!src.ReadToken(token)) {
                        src.Error("idMapEntity::Parse: unexpected EOF");
                        return null;
                    }

                    if (worldent) {
                        origin.Zero();
                    }

                    // if is it a brush: brush, brushDef, brushDef2, brushDef3
                    if (token.Icmpn("brush", 5) == 0) {
                        mapBrush = idMapBrush.Parse(src, origin, (0 == token.Icmp("brushDef2") || 0 == token.Icmp("brushDef3")), version);
                        if (null == mapBrush) {
                            return null;
                        }
                        mapEnt.AddPrimitive(mapBrush);
                    } // if is it a patch: patchDef2, patchDef3
                    else if (token.Icmpn("patch", 5) == 0) {
                        mapPatch = idMapPatch.Parse(src, origin, 0 == token.Icmp("patchDef3"), version);
                        if (null == mapPatch) {
                            return null;
                        }
                        mapEnt.AddPrimitive(mapPatch);
                    } // assume it's a brush in Q3 or older style
                    else {
                        src.UnreadToken(token);
                        mapBrush = idMapBrush.ParseQ3(src, origin);
                        if (null == mapBrush) {
                            return null;
                        }
                        mapEnt.AddPrimitive(mapBrush);
                    }
                } else {
                    idStr key, value;

                    // parse a key / value pair
                    key = token;
                    src.ReadTokenOnLine(token);
                    value = token;

                    // strip trailing spaces that sometimes get accidentally
                    // added in the editor
                    value.StripTrailingWhitespace();
                    key.StripTrailingWhitespace();

                    mapEnt.epairs.Set(key, value);

                    if (0 == idStr.Icmp(key.toString(), "origin")) {
                        // scanf into doubles, then assign, so it is idVec size independent
                        v1 = v2 = v3 = 0;
//                        sscanf(value, "%lf %lf %lf",  & v1,  & v2,  & v3);
                        String[] values = value.toString().split(" ");
                        origin.x = v1 = Float.parseFloat(String.format("%lf", values[0]));
                        origin.y = v2 = Float.parseFloat(String.format("%lf", values[1]));
                        origin.z = v3 = Float.parseFloat(String.format("%lf", values[2]));
                    } else if (0 == idStr.Icmp(key.toString(), "classname") && 0 == idStr.Icmp(value.toString(), "worldspawn")) {
                        worldent = true;
                    }
                }
            } while (true);

            return mapEnt;
        }

        public boolean Write(idFile fp, int entityNum) throws idException {
            int i;
            idMapPrimitive mapPrim;
            idVec3 origin = new idVec3();

            fp.WriteFloatString("// entity %d\n{\n", entityNum);

            // write entity epairs
            for (i = 0; i < epairs.GetNumKeyVals(); i++) {
                fp.WriteFloatString("\"%s\" \"%s\"\n", epairs.GetKeyVal(i).GetKey(), epairs.GetKeyVal(i).GetValue());
            }

            epairs.GetVector("origin", "0 0 0", origin);

            // write pritimives
            for (i = 0; i < GetNumPrimitives(); i++) {
                mapPrim = GetPrimitive(i);

                switch (mapPrim.GetType()) {
                    case TYPE_BRUSH:
                        ((idMapBrush) mapPrim).Write(fp, i, origin);
                        break;
                    case TYPE_PATCH:
                        new idMapPatch(mapPrim).Write(fp, i, origin);
                        break;
                }
            }

            fp.WriteFloatString("}\n");

            return true;
        }

        public int GetNumPrimitives() {
            return primitives.Num();
        }

        public idMapPrimitive GetPrimitive(int i) {
            return primitives.oGet(i);
        }

        public void AddPrimitive(idMapPrimitive p) {
            primitives.Append(p);
        }

        public int GetGeometryCRC() {
            int i;
            int crc;
            idMapPrimitive mapPrim;

            crc = 0;
            for (i = 0; i < GetNumPrimitives(); i++) {
                mapPrim = GetPrimitive(i);

                switch (mapPrim.GetType()) {
                    case TYPE_BRUSH:
                        crc ^= ((idMapBrush) mapPrim).GetGeometryCRC();
                        break;
                    case TYPE_PATCH:
                        crc ^= new idMapPatch(mapPrim).GetGeometryCRC();
                        break;
                }
            }

            return crc;
        }

        public void RemovePrimitiveData() {
            primitives.DeleteContents(true);
        }

    };

    public static class idMapFile {

        protected float version;
        protected long fileTime;
        protected int geometryCRC;
        protected idList<idMapEntity> entities;
        protected idStr name;
        protected boolean hasPrimitiveData;
//
//

        public idMapFile() {
            version = CURRENT_MAP_VERSION;
            fileTime = 0;
            geometryCRC = 0;
            entities = new idList<>();
            entities.Resize(1024, 256);
            hasPrimitiveData = false;
        }
//public							~idMapFile( void ) { entities.DeleteContents( true ); }
//

        // filename does not require an extension
        // normally this will use a .reg file instead of a .map file if it exists,
        // which is what the game and dmap want, but the editor will want to always
        // load a .map file
        public boolean Parse(final String filename, boolean ignoreRegion/*= false*/, boolean osPath/*= false*/) throws idException {
            // no string concatenation for epairs and allow path names for materials
            idLexer src = new idLexer(LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
            idToken token = new idToken();
            idStr fullName;
            idMapEntity mapEnt;
            int i, j, k;

            name = new idStr(filename);
            name.StripFileExtension();
            fullName = name;
            hasPrimitiveData = false;

            if (!ignoreRegion) {
                // try loading a .reg file first
                fullName.SetFileExtension("reg");
                src.LoadFile(fullName.toString(), osPath);
            }

            if (!src.IsLoaded()) {
                // now try a .map file
                fullName.SetFileExtension("map");
                src.LoadFile(fullName.toString(), osPath);
                if (!src.IsLoaded()) {
                    // didn't get anything at all
                    return false;
                }
            }

            version = OLD_MAP_VERSION;
            fileTime = src.GetFileTime();
            entities.DeleteContents(true);

            if (src.CheckTokenString("Version")) {
                src.ReadTokenOnLine(token);
                version = token.GetFloatValue();
            }

            while (true) {
                mapEnt = idMapEntity.Parse(src, (entities.Num() == 0), version);
                if (null == mapEnt) {
                    break;
                }
                entities.Append(mapEnt);
            }

            SetGeometryCRC();

            // if the map has a worldspawn
            if (entities.Num() != 0) {

                // "removeEntities" "classname" can be set in the worldspawn to remove all entities with the given classname
                idKeyValue removeEntities = entities.oGet(0).epairs.MatchPrefix("removeEntities", null);
                while (removeEntities != null) {
                    RemoveEntities(removeEntities.GetValue().toString());
                    removeEntities = entities.oGet(0).epairs.MatchPrefix("removeEntities", removeEntities);
                }

                // "overrideMaterial" "material" can be set in the worldspawn to reset all materials
                idStr material = new idStr();
                if (entities.oGet(0).epairs.GetString("overrideMaterial", "", material)) {
                    for (i = 0; i < entities.Num(); i++) {
                        mapEnt = entities.oGet(i);
                        for (j = 0; j < mapEnt.GetNumPrimitives(); j++) {
                            idMapPrimitive mapPrimitive = mapEnt.GetPrimitive(j);
                            switch (mapPrimitive.GetType()) {
                                case TYPE_BRUSH: {
                                    idMapBrush mapBrush = (idMapBrush) mapPrimitive;
                                    for (k = 0; k < mapBrush.GetNumSides(); k++) {
                                        mapBrush.GetSide(k).SetMaterial(material.toString());
                                    }
                                    break;
                                }
                                case TYPE_PATCH: {
//							static_cast<idMapPatch *>(mapPrimitive).SetMaterial( material );//TODO:necessary?
                                    break;
                                }
                            }
                        }
                    }
                }

                // force all entities to have a name key/value pair
                if (entities.oGet(0).epairs.GetBool("forceEntityNames")) {
                    for (i = 1; i < entities.Num(); i++) {
                        mapEnt = entities.oGet(i);
                        if (null == mapEnt.epairs.FindKey("name")) {
                            mapEnt.epairs.Set("name", va("%s%d", mapEnt.epairs.GetString("classname", "forcedName"), i));
                        }
                    }
                }

                // move the primitives of any func_group entities to the worldspawn
                if (entities.oGet(0).epairs.GetBool("moveFuncGroups")) {
                    for (i = 1; i < entities.Num(); i++) {
                        mapEnt = entities.oGet(i);
                        if (idStr.Icmp(mapEnt.epairs.GetString("classname"), "func_group") == 0) {
                            entities.oGet(0).primitives.Append(mapEnt.primitives);
                            mapEnt.primitives.Clear();
                        }
                    }
                }
            }

            hasPrimitiveData = true;
            return true;
        }

        public boolean Parse(final String filename, boolean ignoreRegion/*= false*/) throws idException {
            return Parse(filename, ignoreRegion, false);
        }

        public boolean Parse(final String filename) throws idException {
            return Parse(filename, false);
        }

        public boolean Parse(final idStr filename) throws idException {
            return Parse(filename.toString());
        }

        public boolean Write(final String fileName, final String ext) throws idException {
            return Write(fileName, ext, true);
        }

        public boolean Write(final String fileName, final String ext, boolean fromBasePath) throws idException {
            int i;
            idStr qpath;
            idFile fp;

            qpath = new idStr(fileName);
            qpath.SetFileExtension(ext);

            idLib.common.Printf("writing %s...\n", qpath);

            if (fromBasePath) {
                fp = idLib.fileSystem.OpenFileWrite(qpath.toString(), "fs_devpath");
            } else {
                fp = idLib.fileSystem.OpenExplicitFileWrite(qpath.toString());
            }

            if (null == fp) {
                idLib.common.Warning("Couldn't open %s\n", qpath);
                return false;
            }

            fp.WriteFloatString("Version %f\n", CURRENT_MAP_VERSION);

            for (i = 0; i < entities.Num(); i++) {
                entities.oGet(i).Write(fp, i);
            }

            idLib.fileSystem.CloseFile(fp);

            return true;
        }
        // get the number of entities in the map

        public int GetNumEntities() {
            return entities.Num();
        }

        // get the specified entity
        public idMapEntity GetEntity(int i) {
            return entities.oGet(i);
        }

        // get the name without file extension
        public String GetName() {
            return name.toString();
        }

        public idStr GetNameStr() {
            return name;
        }

        // get the file time
        public long GetFileTime() {
            return fileTime;
        }

        // get CRC for the map geometry
        // texture coordinates and entity key/value pairs are not taken into account
        public int GetGeometryCRC() {
            return geometryCRC;
        }

        // returns true if the file on disk changed
        public boolean NeedsReload() {
            if (name.Length() != 0) {
//		ID_TIME_T time = (ID_TIME_T)-1;
                long[] time = {Long.MAX_VALUE};
                if (idLib.fileSystem.ReadFile(name.toString(), null, time) > 0) {
                    return (time[0] > fileTime);
                }
            }
            return true;
        }
//

        public int AddEntity(idMapEntity mapentity) {
            int ret = entities.Append(mapentity);
            return ret;
        }

        public idMapEntity FindEntity(final String name) throws idException {
            for (int i = 0; i < entities.Num(); i++) {
                idMapEntity ent = entities.oGet(i);
                if (idStr.Icmp(ent.epairs.GetString("name"), name) == 0) {
                    return ent;
                }
            }
            return null;
        }

        public idMapEntity FindEntity(final idStr name) throws idException {
            return this.FindEntity(name.toString());
        }

        public void RemoveEntity(idMapEntity mapEnt) {
            entities.Remove(mapEnt);
//	delete mapEnt;
        }

        public void RemoveEntities(final String classname) throws idException {
            for (int i = 0; i < entities.Num(); i++) {
                idMapEntity ent = entities.oGet(i);
                if (idStr.Icmp(ent.epairs.GetString("classname"), classname) == 0) {
//			delete entities[i];
                    entities.RemoveIndex(i);
                    i--;
                }
            }
        }

        public void RemoveAllEntities() {
            entities.DeleteContents(true);
            hasPrimitiveData = false;
        }

        public void RemovePrimitiveData() {
            for (int i = 0; i < entities.Num(); i++) {
                idMapEntity ent = entities.oGet(i);
                ent.RemovePrimitiveData();
            }
            hasPrimitiveData = false;
        }

        public boolean HasPrimitiveData() {
            return hasPrimitiveData;
        }

        private void SetGeometryCRC() {
            int i;

            geometryCRC = 0;
            for (i = 0; i < entities.Num(); i++) {
                geometryCRC ^= entities.oGet(i).GetGeometryCRC();
            }
        }
    };
}
