package neo.CM;

import java.nio.ByteBuffer;
import neo.CM.CollisionModel_local.cm_polygon_s;
import neo.Renderer.Material.idMaterial;
import neo.TempDump.SERiAL;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.idStr.parseStr;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;

/**
 *
 */
public class CollisionModel {

    /*
     ===============================================================================

     Trace model vs. polygonal model collision detection.

     Short translations are the least expensive. Retrieving contact points is
     about as cheap as a short translation. Position tests are more expensive
     and rotations are most expensive.

     There is no position test at the start of a translation or rotation. In other
     words if a translation with start != end or a rotation with angle != 0 starts
     in solid, this goes unnoticed and the collision result is undefined.

     A translation with start == end or a rotation with angle == 0 performs
     a position test and fills in the trace_t structure accordingly.

     ===============================================================================
     */
    // contact type
    public enum contactType_t {

        CONTACT_NONE, // no contact
        CONTACT_EDGE, // trace model edge hits model edge
        CONTACT_MODELVERTEX, // model vertex hits trace model polygon
        CONTACT_TRMVERTEX	// trace model vertex hits model polygon
    };

    // contact info
    public static class contactInfo_t {

        public contactType_t type;        // contact type
        public idVec3        point;       // point of contact
        public idVec3        normal;      // contact plane normal
        public float         dist;        // contact plane distance
        public int           contents;    // contents at other side of surface
        public idMaterial    material;    // surface material
        public int           modelFeature;// contact feature on model
        public int           trmFeature;  // contact feature on trace model
        public int           entityNum;   // entity the contact surface is a part of
        public int           id;          // id of clip model the contact surface is part of

        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;

        public contactInfo_t() {
            point = new idVec3();
            normal = new idVec3();
//            TempDump.printCallStack("contactInfo_t:" + DBG_count);
        }

        public contactInfo_t(contactInfo_t c) {
            this();
            this.type = c.type;
            this.point.oSet(c.point);
            this.normal.oSet(c.normal);
            this.dist = c.dist;
            this.contents = c.contents;
            this.material = c.material;
            this.modelFeature = c.modelFeature;
            this.trmFeature = c.trmFeature;
            this.entityNum = c.entityNum;
            this.id = c.id;
        }
    };

    // trace result
    public static class trace_s implements SERiAL {

        public float         fraction;     // fraction of movement completed, 1.0 = didn't hit anything
        public idVec3        endpos;       // final position of trace model
        public idMat3        endAxis;      // final axis of trace model
        public contactInfo_t c;            // contact information, only valid if fraction < 1.0

        public trace_s() {
            endpos = new idVec3();
            endAxis = new idMat3();
            this.c = new contactInfo_t();
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

        @Deprecated
        public void oSet(trace_s trace_s) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
//
//typedef int cmHandle_t;
    public static final float CM_CLIP_EPSILON   = 0.25f;        // always stay this distance away from any model
    public static final float CM_BOX_EPSILON    = 1.0f;         // should always be larger than clip epsilon
    public static final float CM_MAX_TRACE_DIST = 4096.0f;      // maximum distance a trace model may be traced, point traces are unlimited

    public static abstract class idCollisionModelManager {

        // virtual 					~idCollisionModelManager( void ) {}
        // Loads collision models from a map file.
        public abstract void LoadMap(final idMapFile mapFile);

        // Frees all the collision models.
        public abstract void FreeMap();

        // Gets the clip handle for a model.
        public abstract int LoadModel(final idStr modelName, final boolean precache);

        public int LoadModel(final String modelName, final boolean precache) {
            return LoadModel(parseStr(modelName), precache);
        }

        // Sets up a trace model for collision with other trace models.
        public abstract int SetupTrmModel(final idTraceModel trm, final idMaterial material);

        // Creates a trace model from a collision model, returns true if succesfull.
        public abstract boolean TrmFromModel(final idStr modelName, idTraceModel trm);

        // Gets the name of a model.
        public abstract String GetModelName(int model);

        // Gets the bounds of a model.
        public abstract boolean GetModelBounds(int model, idBounds bounds);

        // Gets all contents flags of brushes and polygons of a model ored together.
        public abstract boolean GetModelContents(int model, int[] contents);

        // Gets a vertex of a model.
        public abstract boolean GetModelVertex(int model, int vertexNum, idVec3 vertex);

        // Gets an edge of a model.
        public abstract boolean GetModelEdge(int model, int edgeNum, idVec3 start, idVec3 end);

        // Gets a polygon of a model.
        public abstract boolean GetModelPolygon(int model, /*int*/ cm_polygon_s polygonNum, idFixedWinding winding);

        // Translates a trace model and reports the first collision if any.
        public abstract void Translation(trace_s[] results, final idVec3 start, final idVec3 end,
                final idTraceModel trm, final idMat3 trmAxis, int contentMask,
                int model, final idVec3 modelOrigin, final idMat3 modelAxis);

        // Rotates a trace model and reports the first collision if any.
        public abstract void Rotation(trace_s[] results, final idVec3 start, final idRotation rotation,
                final idTraceModel trm, final idMat3 trmAxis, int contentMask,
                int model, final idVec3 modelOrigin, final idMat3 modelAxis);

        // Returns the contents touched by the trace model or 0 if the trace model is in free space.
        public abstract int Contents(final idVec3 start,
                final idTraceModel trm, final idMat3 trmAxis, int contentMask,
                int model, final idVec3 modelOrigin, final idMat3 modelAxis);

        // Stores all contact points of the trace model with the model, returns the number of contacts.
        public abstract int Contacts(contactInfo_t[] contacts, final int maxContacts, final idVec3 start, final idVec6 dir, final float depth,
                final idTraceModel trm, final idMat3 trmAxis, int contentMask,
                int model, final idVec3 modelOrigin, final idMat3 modelAxis);

        // Tests collision detection.
        public abstract void DebugOutput(final idVec3 origin);
        // Draws a model.

        public abstract void DrawModel(int model, final idVec3 modelOrigin, final idMat3 modelAxis, final idVec3 viewOrigin, final float radius);

        // Prints model information, use -1 handle for accumulated model info.
        public abstract void ModelInfo(int model);

        // Lists all loaded models.
        public abstract void ListModels();

        // Writes a collision model file for the given map entity.
        public abstract boolean WriteCollisionModelForMapEntity(final idMapEntity mapEnt, final String filename, final boolean testTraceModel/* = true*/);

        public abstract boolean WriteCollisionModelForMapEntity(final idMapEntity mapEnt, final String filename);
    };
}
