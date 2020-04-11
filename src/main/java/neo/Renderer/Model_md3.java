package neo.Renderer;

import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import static neo.Renderer.RenderWorld.SHADERPARM_MD3_BACKLERP;
import static neo.Renderer.RenderWorld.SHADERPARM_MD3_FRAME;
import static neo.Renderer.RenderWorld.SHADERPARM_MD3_LASTFRAME;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.LittleFloat;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleShort;

import java.nio.ByteBuffer;

import neo.TempDump.SERiAL;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.dynamicModel_t;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_md3 {

    /*
     ========================================================================

     .MD3 triangle model file format

     Private structures used by the MD3 loader.

     ========================================================================
     */
    static final int MD3_IDENT = (('3' << 24) + ('P' << 16) + ('D' << 8) + 'I');
    static final int MD3_VERSION = 15;
//
// surface geometry should not exceed these limits
    static final int SHADER_MAX_VERTEXES = 1000;
    static final int SHADER_MAX_INDEXES = (6 * SHADER_MAX_VERTEXES);
//
// limits
    static final int MD3_MAX_LODS = 4;
    static final int MD3_MAX_TRIANGLES = 8192;// per surface
    static final int MD3_MAX_VERTS = 4096;	// per surface
    static final int MD3_MAX_SHADERS = 256;	// per surface
    static final int MD3_MAX_FRAMES = 1024;	// per model
    static final int MD3_MAX_SURFACES = 32;	// per model
    static final int MD3_MAX_TAGS = 16;	// per frame
    static final int MAX_MD3PATH = 64;		// from quake3
//
// vertex scales
    static final double MD3_XYZ_SCALE = (1.0 / 64);

    static class md3Frame_s {

        idVec3[] bounds = new idVec3[2];
        idVec3 localOrigin;
        float radius;
//	char		name[16];
        String name;
    }

    static class md3Tag_s {
//	char		name[MAX_MD3PATH];	// tag name

        String name;	// tag name
        idVec3 origin;
        idVec3[] axis = new idVec3[3];
    }

    /*
     ** md3Surface_t
     **
     ** CHUNK			SIZE
     ** header			sizeof( md3Surface_t )
     ** shaders			sizeof( md3Shader_t ) * numShaders
     ** triangles[0]		sizeof( md3Triangle_t ) * numTriangles
     ** st				sizeof( md3St_t ) * numVerts
     ** XyzNormals		sizeof( md3XyzNormal_t ) * numVerts * numFrames
     */
    static class md3Surface_s {

        int ident;				// 
//
//	char		name[MAX_MD3PATH];	// polyset name
        String name;                            // polyset name
//
        int flags;
        int numFrames;                          // all surfaces in a model should have the same
//
        int numShaders;                         // all surfaces in a model should have the same
        int numVerts;
//
        int numTriangles;
        int ofsTriangles;
//
        int ofsShaders;                         // offset from start of md3Surface_t
        int ofsSt;				// texture coords are common for all frames
        int ofsXyzNormals;                      // numVerts * numFrames
//
        //
        md3Triangle_t[] triangles;
        //
        md3Shader_t[] shaders;
        md3St_t[] verts;
        md3XyzNormal_t[] normals;
        //

        int ofsEnd;				// next surface follows
    }

    static class md3Shader_t {
//	char				name[MAX_MD3PATH];

        String name;
        idMaterial shader;			// for in-game use
    }

    static class md3Triangle_t {

        int[] indexes = new int[3];
    }

    static class md3St_t {

        float[] st = new float[2];
    }

    static class md3XyzNormal_t {

        short[] xyz = new short[3];
        short normal;
    }

    static class md3Header_s implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		int ident;
        int version;
//
//	char		name[MAX_MD3PATH];	// model name
        String name;                            // model name
//
        int flags;
//
        int numFrames;
        int numTags;
        int numSurfaces;
//
        int numSkins;
//
        int ofsFrames;                          // offset for first frame
        int ofsTags;                            // numFrames * numTags
        int ofsSurfaces;                        // first surface, others follow
//
        // 
        md3Frame_s[] frames;
        md3Tag_s[] tags;
        md3Surface_s[] surfaces;
        //
        int ofsEnd;				// end of file

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
    }

    static int LL(int x) {
        return LittleLong(x);
    }

    /*
     ===============================================================================

     MD3 animated model

     ===============================================================================
     */
    static class idRenderModelMD3 extends idRenderModelStatic {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;		// model = tr.models[model->index]
        private int dataSize;		// just for listing purposes
        private md3Header_s md3;	// only if type == MOD_MESH
        private int numLods;
        //
        //

        @Override
        public void InitFromFile(String fileName) {
            int i, j;
            md3Header_s pinmodel;
            md3Frame_s frame;
            md3Surface_s surf;
            md3Shader_t shader;
            md3Triangle_t tri;
            md3St_t st;
            md3XyzNormal_t xyz;
            md3Tag_s tag;
            final ByteBuffer[] buffer = {null};
            int version;
            int size;

            this.name.oSet(fileName);

            size = fileSystem.ReadFile(fileName, buffer, null);
            if ((0 == size) || (size < 0)) {
                return;
            }

            pinmodel = new md3Header_s();
            pinmodel.Read(buffer[0]);

            version = LittleLong(pinmodel.version);
            if (version != MD3_VERSION) {
                fileSystem.FreeFile(buffer);
                common.Warning("InitFromFile: %s has wrong version (%d should be %d)",
                        fileName, version, MD3_VERSION);
                return;
            }

            size = LittleLong(pinmodel.ofsEnd);
            this.dataSize += size;
//            md3 = new md3Header_s[size];// Mem_Alloc(size);

//            memcpy(md3, buffer, LittleLong(pinmodel.ofsEnd));
//            for (int h = 0; h < size; h++) {
            this.md3 = new md3Header_s();
            this.md3.Read(buffer[0]);

            this.md3.ident = LL(this.md3.ident);
            this.md3.version = LL(this.md3.version);
            this.md3.numFrames = LL(this.md3.numFrames);
            this.md3.numTags = LL(this.md3.numTags);
            this.md3.numSurfaces = LL(this.md3.numSurfaces);
            this.md3.ofsFrames = LL(this.md3.ofsFrames);
            this.md3.ofsTags = LL(this.md3.ofsTags);
            this.md3.ofsSurfaces = LL(this.md3.ofsSurfaces);
            this.md3.ofsEnd = LL(this.md3.ofsEnd);

            if (this.md3.numFrames < 1) {
                common.Warning("InitFromFile: %s has no frames", fileName);
                fileSystem.FreeFile(buffer);
                return;
            }

            // swap all the frames
//            frame = (md3Frame_s) ((byte[]) md3[md3.ofsFrames]);
            this.md3.frames = new md3Frame_s[this.md3.numFrames];
            for (i = 0; i < this.md3.numFrames; i++) {
                frame = new md3Frame_s();
                frame.radius = LittleFloat(frame.radius);
                for (j = 0; j < 3; j++) {
                    frame.bounds[0].oSet(j, LittleFloat(frame.bounds[0].oGet(j)));
                    frame.bounds[1].oSet(j, LittleFloat(frame.bounds[1].oGet(j)));
                    frame.localOrigin.oSet(j, LittleFloat(frame.localOrigin.oGet(j)));
                }
                this.md3.frames[i] = frame;
            }

            // swap all the tags
//                tag = (md3Tag_s) ((byte[]) md3[md3.ofsTags]);
            this.md3.tags = new md3Tag_s[this.md3.numTags * this.md3.numFrames];
            for (i = 0; i < (this.md3.numTags * this.md3.numFrames); i++) {
                tag = new md3Tag_s();
                for (j = 0; j < 3; j++) {
                    tag.origin.oSet(j, LittleFloat(tag.origin.oGet(j)));
                    tag.axis[0].oSet(j, LittleFloat(tag.axis[0].oGet(j)));
                    tag.axis[1].oSet(j, LittleFloat(tag.axis[1].oGet(j)));
                    tag.axis[2].oSet(j, LittleFloat(tag.axis[2].oGet(j)));
                }
                this.md3.tags[i] = tag;
            }

            // swap all the surfaces
//                surf = (md3Surface_s) ((byte[]) md3[md3.ofsSurfaces]);
            this.md3.surfaces = new md3Surface_s[this.md3.numSurfaces];
            for (i = 0; i < this.md3.numSurfaces; i++) {

                surf = new md3Surface_s();

                surf.ident = LL(surf.ident);
                surf.flags = LL(surf.flags);
                surf.numFrames = LL(surf.numFrames);
                surf.numShaders = LL(surf.numShaders);
                surf.numTriangles = LL(surf.numTriangles);
                surf.ofsTriangles = LL(surf.ofsTriangles);
                surf.numVerts = LL(surf.numVerts);
                surf.ofsShaders = LL(surf.ofsShaders);
                surf.ofsSt = LL(surf.ofsSt);
                surf.ofsXyzNormals = LL(surf.ofsXyzNormals);
                surf.ofsEnd = LL(surf.ofsEnd);

                if (surf.numVerts > SHADER_MAX_VERTEXES) {
                    common.Error("InitFromFile: %s has more than %d verts on a surface (%d)",
                            fileName, SHADER_MAX_VERTEXES, surf.numVerts);
                }
                if ((surf.numTriangles * 3) > SHADER_MAX_INDEXES) {
                    common.Error("InitFromFile: %s has more than %d triangles on a surface (%d)",
                            fileName, SHADER_MAX_INDEXES / 3, surf.numTriangles);
                }

                // change to surface identifier
                surf.ident = 0;	//SF_MD3;

                // lowercase the surface name so skin compares are faster
//		int slen = (int)strlen( surf.name );
//		for( j = 0; j < slen; j++ ) {
//			surf.name[j] = tolower( surf.name[j] );
//		}
                surf.name = surf.name.toLowerCase();

                // strip off a trailing _1 or _2
                // this is a crutch for q3data being a mess
                j = surf.name.length();
                if ((j > 2) && (surf.name.charAt(j - 2) == '_')) {
                    surf.name = surf.name.substring(0, j - 2);
                }

                // register the shaders
//                    shader = (md3Shader_s) ((byte[]) surf[surf.ofsShaders]);
                surf.shaders = new md3Shader_t[surf.numShaders];
                for (j = 0; j < surf.numShaders; j++) {
                    shader = new md3Shader_t();
                    idMaterial sh;

                    sh = declManager.FindMaterial(shader.name);
                    shader.shader = sh;

                    surf.shaders[j] = shader;
                }

                // swap all the triangles
//                    tri = (md3Triangle_t) ((byte[]) surf[surf.ofsTriangles]);
                surf.triangles = new md3Triangle_t[surf.numTriangles];
                for (j = 0; j < surf.numTriangles; j++) {
                    tri = new md3Triangle_t();
                    tri.indexes[0] = LL(tri.indexes[0]);
                    tri.indexes[1] = LL(tri.indexes[1]);
                    tri.indexes[2] = LL(tri.indexes[2]);

                    surf.triangles[j] = tri;
                }

                // swap all the ST
//                    st = (md3St_t) ((byte[]) surf + surf.ofsSt);
                surf.verts = new md3St_t[surf.numVerts];
                for (j = 0; j < surf.numVerts; j++) {
                    st = new md3St_t();
                    st.st[0] = LittleFloat(st.st[0]);
                    st.st[1] = LittleFloat(st.st[1]);

                    surf.verts[j] = st;
                }

                // swap all the XyzNormals
//                    xyz = (md3XyzNormal_t) ((byte[]) surf + surf.ofsXyzNormals);
                surf.normals = new md3XyzNormal_t[surf.numVerts * surf.numFrames];
                for (j = 0; j < (surf.numVerts * surf.numFrames); j++) {
                    xyz = new md3XyzNormal_t();

                    xyz.xyz[0] = LittleShort(xyz.xyz[0]);
                    xyz.xyz[1] = LittleShort(xyz.xyz[1]);
                    xyz.xyz[2] = LittleShort(xyz.xyz[2]);

                    xyz.normal = LittleShort(xyz.normal);

                    surf.normals[j] = xyz;
                }

                // find the next surface
//                    surf = (md3Surface_t) ((byte[]) surf[surf.ofsEnd]);//TODO: make sure the offsets are mapped correctly with the serialization
//                    
                this.md3.surfaces[i] = surf;
            }
//            }

            fileSystem.FreeFile(buffer);
        }

        @Override
        public dynamicModel_t IsDynamicModel() {
            return DM_CACHED;
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s ent, viewDef_s view, idRenderModel cachedModel) {
            int i, j;
            float backlerp;
//            md3Triangle_t triangle;
//            float[] texCoords;
            int indexes;
            int numVerts;
            md3Surface_s surface;
            int frame, oldframe;
            idRenderModelStatic staticModel;

            if (cachedModel != null) {
//		delete cachedModel;
                cachedModel = null;
            }

            staticModel = new idRenderModelStatic();
            staticModel.bounds.Clear();

//            surface = (md3Surface_t) ((byte[]) md3[ md3.ofsSurfaces]);
            surface = this.md3.surfaces[0];

            // TODO: these need set by an entity
            frame = (int) ent.shaderParms[SHADERPARM_MD3_FRAME];			// probably want to keep frames < 1000 or so
            oldframe = (int) ent.shaderParms[SHADERPARM_MD3_LASTFRAME];
            backlerp = ent.shaderParms[SHADERPARM_MD3_BACKLERP];

            for (i = 0; i < this.md3.numSurfaces; /*i++*/) {

                final srfTriangles_s tri = R_AllocStaticTriSurf();
                R_AllocStaticTriSurfVerts(tri, surface.numVerts);
                R_AllocStaticTriSurfIndexes(tri, surface.numTriangles * 3);
                tri.bounds.Clear();

                final modelSurface_s surf = new modelSurface_s();

                surf.geometry = tri;

//                md3Shader_t shaders = (md3Shader_t) ((byte[]) surface[surface.ofsShaders]);
                final md3Shader_t shaders = surface.shaders[0];
                surf.shader = shaders.shader;

                LerpMeshVertexes(tri, surface, backlerp, frame, oldframe);

                indexes = surface.numTriangles * 3;
                j = 0;
                for (final md3Triangle_t triangle : surface.triangles) {
//                triangles = (int[]) ((byte[]) surface + surface.ofsTriangles);
                    for (/*j = 0*/; j < indexes; j++) {
                        tri.indexes[j] = triangle.indexes[j];
                    }
                    tri.numIndexes += indexes;
                }

                numVerts = surface.numVerts;
                j = 0;
                for (final md3St_t texCoords : surface.verts) {
//                texCoords = (float[]) ((byte[]) surface + surface.ofsSt);

                    for (/*j = 0*/; j < numVerts; j++) {
                        final idDrawVert stri = tri.verts[j];
                        stri.st.oSet(0, texCoords.st[(j * 2) + 0]);
                        stri.st.oSet(1, texCoords.st[(j * 2) + 1]);
                    }
                }

                R_BoundTriSurf(tri);

                staticModel.AddSurface(surf);
                staticModel.bounds.AddPoint(surf.geometry.bounds.oGet(0));
                staticModel.bounds.AddPoint(surf.geometry.bounds.oGet(1));

                // find the next surface
                surface = this.md3.surfaces[++i];
            }

            return staticModel;
        }

        @Override
        public idBounds Bounds(renderEntity_s ent) {
            final idBounds ret = new idBounds();

            ret.Clear();

            if ((null == ent) || (null == this.md3)) {
                // just give it the editor bounds
                ret.AddPoint(new idVec3(-10, -10, -10));
                ret.AddPoint(new idVec3(10, 10, 10));
                return ret;
            }

//            md3Frame_s frame = (md3Frame_t) ((byte[]) md3 + md3.ofsFrames);
            final md3Frame_s frame = this.md3.frames[0];

            ret.AddPoint(frame.bounds[0]);
            ret.AddPoint(frame.bounds[1]);

            return ret;
        }

        private void LerpMeshVertexes(srfTriangles_s tri, final md3Surface_s surf, final float backlerp, /*final*/ int frame, /*final*/ int oldframe) {
            md3XyzNormal_t oldXyz, newXyz;
            float oldXyzScale, newXyzScale;
            int vertNum;
            int numVerts;

//            newXyz = (short[]) ((byte[]) surf + surf.ofsXyzNormals) + (frame * surf.numVerts * 4);
            newXyz = surf.normals[frame];
            newXyzScale = (float) (MD3_XYZ_SCALE * (1.0 - backlerp));

            numVerts = surf.numVerts;

            if (backlerp == 0) {
                //
                // just copy the vertexes
                //
                for (vertNum = 0; vertNum < numVerts; newXyz = surf.normals[++frame], vertNum++) {

                    final idDrawVert outvert = tri.verts[tri.numVerts];

                    outvert.xyz.x = newXyz.xyz[0] * newXyzScale;
                    outvert.xyz.y = newXyz.xyz[1] * newXyzScale;
                    outvert.xyz.z = newXyz.xyz[2] * newXyzScale;

                    tri.numVerts++;
                }
            } else {
                //
                // interpolate and copy the vertexes
                //
//                oldXyz = (short[]) ((byte[]) surf + surf.ofsXyzNormals) + (oldframe * surf.numVerts * 4);
                oldXyz = surf.normals[oldframe];

                oldXyzScale = (float) (MD3_XYZ_SCALE * backlerp);

                for (vertNum = 0; vertNum < numVerts; vertNum++, oldXyz = surf.normals[++oldframe], newXyz = surf.normals[++frame]) {

                    final idDrawVert outvert = tri.verts[tri.numVerts];

                    // interpolate the xyz
                    outvert.xyz.x = (oldXyz.xyz[0] * oldXyzScale) + (newXyz.xyz[0] * newXyzScale);
                    outvert.xyz.y = (oldXyz.xyz[1] * oldXyzScale) + (newXyz.xyz[1] * newXyzScale);
                    outvert.xyz.z = (oldXyz.xyz[2] * oldXyzScale) + (newXyz.xyz[2] * newXyzScale);

                    tri.numVerts++;
                }
            }
        }
    }
}
