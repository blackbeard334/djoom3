package neo.Game.Physics;

import java.util.Arrays;
import static neo.CM.CollisionModel.CM_BOX_EPSILON;
import static neo.CM.CollisionModel.CM_MAX_TRACE_DIST;
import neo.CM.CollisionModel.contactInfo_t;
import static neo.CM.CollisionModel.contactType_t.CONTACT_TRMVERTEX;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;

import static neo.CM.CollisionModel_local.collisionModelManager;
import static neo.Game.Game_local.ENTITYNUM_NONE;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Model.INVALID_JOINT;
import neo.Renderer.RenderWorld.modelTrace_s;
import neo.Renderer.RenderWorld.renderEntity_s;
import static neo.TempDump.sizeof;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorWhite;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.Winding.idFixedWinding;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_default;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;

/**
 *
 */
public class Clip {

    /*
     ===============================================================================

     Handles collision detection with the world and between physics objects.

     ===============================================================================
     */
    public static int CLIPMODEL_ID_TO_JOINT_HANDLE(final int id) {
        return (id >= 0 ? INVALID_JOINT : (-1 - id));
    }

    public static int JOINT_HANDLE_TO_CLIPMODEL_ID(final int id) {
        return (-1 - id);
    }
    public static final int MAX_SECTOR_DEPTH = 12;
    public static final int MAX_SECTORS      = ((1 << (MAX_SECTOR_DEPTH + 1)) - 1);

    public static class clipSector_s {

        int axis;		// -1 = leaf node
        float dist;
        clipSector_s[] children = new clipSector_s[2];
        clipLink_s clipLinks;

//        private void oSet(clipSector_s clip) {
//            this.axis = clip.axis;
//            this.dist = clip.dist;
//            this.children = clip.children;
//            this.clipLinks = clip.clipLinks;
//        }
    };

    public static class clipLink_s {

        idClipModel clipModel;
        clipSector_s sector;
        clipLink_s prevInSector;
        clipLink_s nextInSector;
        clipLink_s nextLink;
    };

    public static class trmCache_s {

        idTraceModel trm;
        int          refCount;
        float        volume;
        idVec3       centerOfMass;
        idMat3       inertiaTensor;

        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public trmCache_s() {
            centerOfMass = new idVec3();
            inertiaTensor = new idMat3();
        }
    };
    public static final idVec3 vec3_boxEpsilon = new idVec3(CM_BOX_EPSILON, CM_BOX_EPSILON, CM_BOX_EPSILON);
//    public static final idBlockAlloc<clipLink_s> clipLinkAllocator = new idBlockAlloc<>(1024);
    /*
     ===============================================================

     idClipModel trace model cache

     ===============================================================
     */
    static final idList<trmCache_s> traceModelCache = new idList<>();
    static final idHashIndex        traceModelHash  = new idHashIndex();

    public static class idClipModel {

        private boolean  enabled;                       // true if this clip model is used for clipping
        private idEntity entity;                        // entity using this clip model
        private int      id;                            // id for entities that use multiple clip models
        private idEntity owner;                         // owner of the entity that owns this clip model
        private idVec3   origin    = new idVec3();      // origin of clip model
        private idMat3   axis      = new idMat3();      // orientation of clip model
        private idBounds bounds    = new idBounds();    // bounds
        private idBounds absBounds = new idBounds();    // absolute bounds
        private idMaterial        material;             // material for trace models
        private int               contents;             // all contents ored together
        private int/*cmHandle_t*/ collisionModelHandle; // handle to collision model
        private int               traceModelIndex;      // trace model used for collision detection
        private int               renderModelHandle;    // render model def handle
        //
        private clipLink_s        clipLinks;            // links into sectors
        private int               touchCount;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        // friend class idClip;
        public idClipModel() {
            Init();
        }

        public idClipModel(final String name) {
            Init();
            LoadModel(name);
        }

        public idClipModel(final idTraceModel trm) {
            Init();
            LoadModel(trm);
        }

        public idClipModel(final int renderModelHandle) {
            Init();
            contents = CONTENTS_RENDERMODEL;
            LoadModel(renderModelHandle);
        }

        public idClipModel(final idClipModel model) {
            enabled = model.enabled;
            entity = model.entity;
            id = model.id;
            owner = model.owner;
            origin.oSet(model.origin);
            if (Float.isNaN(origin.oGet(0))) {
                int a = 0;
            }
            axis.oSet(model.axis);
            bounds.oSet(model.bounds);
            absBounds.oSet(model.absBounds);
            material = model.material;
            contents = model.contents;
            collisionModelHandle = model.collisionModelHandle;
            traceModelIndex = -1;
            if (model.traceModelIndex != -1) {
                LoadModel(GetCachedTraceModel(model.traceModelIndex));
            }
            renderModelHandle = model.renderModelHandle;
            clipLinks = null;
            touchCount = -1;
        }
        // ~idClipModel( void );

        public final boolean LoadModel(final String name) {
            renderModelHandle = -1;
            if (traceModelIndex != -1) {
                FreeTraceModel(traceModelIndex);
                traceModelIndex = -1;
            }
            collisionModelHandle = collisionModelManager.LoadModel(name, false);
            if (collisionModelHandle != 0) {
                collisionModelManager.GetModelBounds(collisionModelHandle, bounds);
                {
                    int[] contents = {0};
                    collisionModelManager.GetModelContents(collisionModelHandle, contents);
                    this.contents = contents[0];
                }
                return true;
            } else {
                bounds.Zero();
                return false;
            }
        }

        public final void LoadModel(final idTraceModel trm) {
            collisionModelHandle = 0;
            renderModelHandle = -1;
            if (traceModelIndex != -1) {
                FreeTraceModel(traceModelIndex);
            }
            traceModelIndex = AllocTraceModel(trm);
            bounds.oSet(trm.bounds);
        }

        public final void LoadModel(final int renderModelHandle) {
            collisionModelHandle = 0;
            this.renderModelHandle = renderModelHandle;
            if (renderModelHandle != -1) {
                final renderEntity_s renderEntity = gameRenderWorld.GetRenderEntity(renderModelHandle);
                if (renderEntity != null) {
                    bounds.oSet(renderEntity.bounds);
                }
            }
            if (traceModelIndex != -1) {
                FreeTraceModel(traceModelIndex);
                traceModelIndex = -1;
            }
        }

        public void Save(idSaveGame savefile) {
            savefile.WriteBool(enabled);
            savefile.WriteObject(entity);
            savefile.WriteInt(id);
            savefile.WriteObject(owner);
            savefile.WriteVec3(origin);
            savefile.WriteMat3(axis);
            savefile.WriteBounds(bounds);
            savefile.WriteBounds(absBounds);
            savefile.WriteMaterial(material);
            savefile.WriteInt(contents);
            if (collisionModelHandle >= 0) {
                savefile.WriteString(collisionModelManager.GetModelName(collisionModelHandle));
            } else {
                savefile.WriteString("");
            }
            savefile.WriteInt(traceModelIndex);
            savefile.WriteInt(renderModelHandle);
            savefile.WriteBool(clipLinks != null);
            savefile.WriteInt(touchCount);
        }

        public void Restore(idRestoreGame savefile) {
            idStr collisionModelName = new idStr();
            boolean[] linked = {false};

            enabled = savefile.ReadBool();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/entity);
            id = savefile.ReadInt();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/owner);
            savefile.ReadVec3(origin);
            savefile.ReadMat3(axis);
            savefile.ReadBounds(bounds);
            savefile.ReadBounds(absBounds);
            savefile.ReadMaterial(material);
            contents = savefile.ReadInt();
            savefile.ReadString(collisionModelName);
            if (collisionModelName.Length() != 0) {
                collisionModelHandle = collisionModelManager.LoadModel(collisionModelName.toString(), false);
            } else {
                collisionModelHandle = -1;
            }
            traceModelIndex = savefile.ReadInt();
            if (traceModelIndex >= 0) {
                traceModelCache.oGet(traceModelIndex).refCount++;
            }
            renderModelHandle = savefile.ReadInt();
            savefile.ReadBool(linked);
            touchCount = savefile.ReadInt();

            // the render model will be set when the clip model is linked
            renderModelHandle = -1;
            clipLinks = null;
            touchCount = -1;

            if (linked[0]) {
                Link(gameLocal.clip, entity, id, origin, axis, renderModelHandle);
            }
        }

        private static int DBG_Link = 0;
        public void Link(idClip clp) {				// must have been linked with an entity and id before
            DBG_Link++;
            assert (this.entity != null);
            if (null == this.entity) {
                return;
            }

            if (clipLinks != null) {
                Unlink();	// unlink from old position
            }

            if (bounds.IsCleared()) {
                return;
            }

            // set the abs box
            if (axis.IsRotated()) {
                // expand for rotation
                absBounds.FromTransformedBounds(bounds, origin, axis);
            } else {
                // normal
                absBounds.oSet(0, bounds.oGet(0).oPlus(origin));
                absBounds.oSet(1, bounds.oGet(1).oPlus(origin));
            }

            // because movement is clipped an epsilon away from an actual edge,
            // we must fully check even when bounding boxes don't quite touch
            absBounds.oMinSet(0, vec3_boxEpsilon);
            absBounds.oPluSet(1, vec3_boxEpsilon);

            Link_r(clp.clipSectors[0]);//TODO:check if [0] is good enough.
        }

        public void Link(idClip clp, idEntity ent, int newId, final idVec3 newOrigin, final idMat3 newAxis) {
            Link(clp, ent, newId, newOrigin, newAxis, -1);
        }

        public void Link(idClip clp, idEntity ent, int newId, final idVec3 newOrigin, final idMat3 newAxis, int renderModelHandle /*= -1*/) {

            this.entity = ent;
            this.id = newId;
            this.origin.oSet(newOrigin);
            if (Float.isNaN(origin.z)) {
                int a = 0;
            }
            this.axis.oSet(newAxis);
            if (renderModelHandle != -1) {
                this.renderModelHandle = renderModelHandle;
                final renderEntity_s renderEntity = gameRenderWorld.GetRenderEntity(renderModelHandle);
                if (renderEntity != null) {
                    this.bounds.oSet(renderEntity.bounds);
                }
            }
            this.Link(clp);
        }

        public void Unlink() {						// unlink from sectors
            clipLink_s link;

            for (link = clipLinks; link != null; link = clipLinks) {
                clipLinks = link.nextLink;
                if (link.prevInSector != null) {
                    link.prevInSector.nextInSector = link.nextInSector;
                } else {
                    link.sector.clipLinks = link.nextInSector;
                }
                if (link.nextInSector != null) {
                    link.nextInSector.prevInSector = link.prevInSector;
                }
//                clipLinkAllocator.Free(link);
            }
        }

        public void SetPosition(final idVec3 newOrigin, final idMat3 newAxis) {	// unlinks the clip model
            if (clipLinks != null) {
                Unlink();	// unlink from old position
            }
            origin.oSet(newOrigin);
            if (Float.isNaN(origin.z)) {
                int a = 0;
            }
            axis.oSet(newAxis);
        }

        public void Translate(final idVec3 translation) {							// unlinks the clip model
            Unlink();
            origin.oPluSet(translation);
            if (Float.isNaN(origin.z)) {
                int a = 0;
            }
        }

        public void Rotate(final idRotation rotation) {							// unlinks the clip model
            Unlink();
            origin.oMulSet(rotation);
            if (Float.isNaN(origin.z)) {
                int a = 0;
            }
            axis.oMulSet(rotation.ToMat3());
        }

        public void Enable() {						// enable for clipping
            enabled = true;
        }

        public void Disable() {					// keep linked but disable for clipping
            enabled = false;
        }

        public void SetMaterial(final idMaterial m) {
            material = m;
        }

        public idMaterial GetMaterial() {
            return material;
        }

        public void SetContents(int newContents) {		// override contents
            contents = newContents;
        }

        public int GetContents() {
            return contents;
        }

        public void SetEntity(idEntity newEntity) {
            entity = newEntity;
        }

        public idEntity GetEntity() {
            return entity;
        }

        public void SetId(int newId) {
            id = newId;
        }

        public int GetId() {
            return id;
        }

        public void SetOwner(idEntity newOwner) {
            owner = newOwner;
        }

        public idEntity GetOwner() {
            return owner;
        }

        public idBounds GetBounds() {
            return new idBounds(bounds);
        }

        public idBounds GetAbsBounds() {
            return new idBounds(absBounds);
        }

        public idVec3 GetOrigin() {
            return new idVec3(origin);
        }

        public idMat3 GetAxis() {
            return new idMat3(axis);
        }

        public boolean IsTraceModel() {			// returns true if this is a trace model
            return (traceModelIndex != -1);
        }

        public boolean IsRenderModel() {		// returns true if this is a render model
            return (renderModelHandle != -1);
        }

        public boolean IsLinked() {				// returns true if the clip model is linked
            return (clipLinks != null);
        }

        public boolean IsEnabled() {			// returns true if enabled for collision detection
            return enabled;
        }

        public boolean IsEqual(final idTraceModel trm) {
            return (traceModelIndex != -1 && GetCachedTraceModel(traceModelIndex) == trm);
        }

        public int/*cmHandle_t*/ Handle() {				// returns handle used to collide vs this model
            assert (renderModelHandle == -1);
            if (collisionModelHandle != 0) {
                return collisionModelHandle;
            } else if (traceModelIndex != -1) {
                return collisionModelManager.SetupTrmModel(GetCachedTraceModel(traceModelIndex), material);
            } else {
                // this happens in multiplayer on the combat models
                gameLocal.Warning("idClipModel::Handle: clip model %d on '%s' (%x) is not a collision or trace model", id, entity.name, entity.entityNumber);
                return 0;
            }
        }

        public idTraceModel GetTraceModel() {
            if (!IsTraceModel()) {
                return null;
            }
            return idClipModel.GetCachedTraceModel(traceModelIndex);
        }

        public void GetMassProperties(final float density, float[] mass, idVec3 centerOfMass, idMat3 inertiaTensor) {
            if (traceModelIndex == -1) {
                gameLocal.Error("idClipModel::GetMassProperties: clip model %d on '%s' is not a trace model\n", id, entity.name);
            }

            trmCache_s entry = traceModelCache.oGet(traceModelIndex);
            mass[0] = entry.volume * density;
            centerOfMass.oSet(entry.centerOfMass);
            inertiaTensor.oSet(entry.inertiaTensor.oMultiply(density));
        }

        public static int/*cmHandle_t*/ CheckModel(final String name) {
            return collisionModelManager.LoadModel(name, false);
        }

        public static int/*cmHandle_t*/ CheckModel(final idStr name) {
            return CheckModel(name.toString());
        }

        public static void ClearTraceModelCache() {
            traceModelCache.DeleteContents(true);
            traceModelHash.Free();
        }

        public static int TraceModelCacheSize() {
            return traceModelCache.Num() * sizeof(idTraceModel.class);
        }

        public static void SaveTraceModels(idSaveGame savefile) {
            int i;

            savefile.WriteInt(traceModelCache.Num());
            for (i = 0; i < traceModelCache.Num(); i++) {
                trmCache_s entry = traceModelCache.oGet(i);

                savefile.WriteTraceModel(entry.trm);
                savefile.WriteFloat(entry.volume);
                savefile.WriteVec3(entry.centerOfMass);
                savefile.WriteMat3(entry.inertiaTensor);
            }
        }

        public static void RestoreTraceModels(idRestoreGame savefile) {
            int i;
            int[] num = new int[1];

            ClearTraceModelCache();

            savefile.ReadInt(num);
            traceModelCache.SetNum(num[0]);

            for (i = 0; i < num[0]; i++) {
                trmCache_s entry = new trmCache_s();

                savefile.ReadTraceModel(entry.trm);

                entry.volume = savefile.ReadFloat();
                savefile.ReadVec3(entry.centerOfMass);
                savefile.ReadMat3(entry.inertiaTensor);
                entry.refCount = 0;

                traceModelCache.oSet(i, entry);
                traceModelHash.Add(GetTraceModelHashKey(entry.trm), i);
            }
        }

        // initialize(or does it?)
        private void Init() {
            enabled = true;
            entity = null;
            id = 0;
            owner = null;
            origin.Zero();
            axis.Identity();
            bounds.Zero();
            absBounds.Zero();
            material = null;
            contents = CONTENTS_BODY;
            collisionModelHandle = 0;
            renderModelHandle = -1;
            traceModelIndex = -1;
            clipLinks = null;
            touchCount = -1;
        }

        private void Link_r(clipSector_s node) {
            clipLink_s link;

            while (node.axis != -1) {
                if (absBounds.oGet(0, node.axis) > node.dist) {
                    node = node.children[0];
                } else if (absBounds.oGet(1, node.axis) < node.dist) {
                    node = node.children[1];
                } else {
                    Link_r(node.children[0]);
                    node = node.children[1];
                }
            }

            link = new clipLink_s();//clipLinkAllocator.Alloc();
            link.clipModel = this;
            link.sector = node;
            link.nextInSector = node.clipLinks;
            link.prevInSector = null;
            if (link.clipModel.entity.name.equals("env_gibs_leftleg_1")) {
                int a = 0;
                float x = origin.oGet(0);
                if(Float.isNaN(x) || Float.isInfinite(x))
                System.out.println("~~~~~" + this.origin);
            }
            if (node.clipLinks != null) {
                node.clipLinks.prevInSector = link;
            }
            if (this.entity.name.equals("marscity_cinematic_player_1") &&
                    node.clipLinks.clipModel.entity.name.equals("env_gibs_leftleg_1")) {
                int a = 0;
            }
            node.clipLinks = link;
            link.nextLink = clipLinks;
            clipLinks = link;
        }

        private static int DBG_AllocTraceModel = 0;
        private static int AllocTraceModel(final idTraceModel trm) {DBG_AllocTraceModel++;
            int i, hashKey, traceModelIndex;
            trmCache_s entry;

            hashKey = GetTraceModelHashKey(trm);
            for (i = traceModelHash.First(hashKey); i >= 0; i = traceModelHash.Next(i)) {
                if (traceModelCache.oGet(i).trm.equals(trm)) {
                    traceModelCache.oGet(i).refCount++;
                    return i;
                }
            }

            entry = new trmCache_s();
            entry.trm = trm;
            {
                float[] volume = {0};
                entry.trm.GetMassProperties(1.0f, volume, entry.centerOfMass, entry.inertiaTensor);
                entry.volume = volume[0];
            }
            entry.refCount = 1;

            traceModelIndex = traceModelCache.Append(entry);
            traceModelHash.Add(hashKey, traceModelIndex);
            return traceModelIndex;
        }

        private static void FreeTraceModel(int traceModelIndex) {
            if (traceModelIndex < 0 || traceModelIndex >= traceModelCache.Num() || traceModelCache.oGet(traceModelIndex).refCount <= 0) {
                gameLocal.Warning("idClipModel::FreeTraceModel: tried to free uncached trace model");
                return;
            }
            traceModelCache.oGet(traceModelIndex).refCount--;
        }

        private static idTraceModel GetCachedTraceModel(int traceModelIndex) {
            return traceModelCache.oGet(traceModelIndex).trm;
        }

        private static int GetTraceModelHashKey(final idTraceModel trm) {
            final idVec3 v = trm.bounds.oGet(0);
            return (trm.type.ordinal() << 8) ^ (trm.numVerts << 4) ^ (trm.numEdges << 2) ^ (trm.numPolys << 0) ^ idMath.FloatHash(v.ToFloatPtr(), v.GetDimension());
        }

        public void oSet(idClipModel idClipModel) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        protected void _deconstructor() {
            // make sure the clip model is no longer linked
            Unlink();
            if (traceModelIndex != -1) {
                FreeTraceModel(traceModelIndex);
            }
        }

        public static void delete(idClipModel clipModel) {
            clipModel._deconstructor();
        }
    };

    //===============================================================
    //
    //	idClip
    //
    //===============================================================
    public static class idClip {
        // friend class idClipModel;

        private int numClipSectors;
        private clipSector_s[] clipSectors;
        private idBounds worldBounds;
        private idClipModel temporaryClipModel = new idClipModel();
        private idClipModel defaultClipModel = new idClipModel();
        private int touchCount;
        // statistics
        private int numTranslations;
        private int numRotations;
        private int numMotions;
        private int numRenderModelTraces;
        private int numContents;
        private int numContacts;
        //
        //

        public idClip() {
            numClipSectors = 0;
            clipSectors = null;
            this.worldBounds = new idBounds();//worldBounds.Zero();
            numRotations = numTranslations = numMotions = numRenderModelTraces = numContents = numContacts = 0;
        }

        public void Init() {
            int/*cmHandle_t*/ h;
            idVec3 size, maxSector = getVec3_origin();

            // clear clip sectors
            clipSectors = new clipSector_s[MAX_SECTORS];
//	memset( clipSectors, 0, MAX_SECTORS * sizeof( clipSector_t ) );
            numClipSectors = 0;
            touchCount = -1;
            // get world map bounds
            h = collisionModelManager.LoadModel("worldMap", false);
            collisionModelManager.GetModelBounds(h, worldBounds);
            // create world sectors
            CreateClipSectors_r(0, worldBounds, maxSector);

            size = worldBounds.oGet(1).oMinus(worldBounds.oGet(0));
            gameLocal.Printf("map bounds are (%1.1f, %1.1f, %1.1f)\n", size.oGet(0), size.oGet(1), size.oGet(2));
            gameLocal.Printf("max clip sector is (%1.1f, %1.1f, %1.1f)\n", maxSector.oGet(0), maxSector.oGet(1), maxSector.oGet(2));

            // initialize a default clip model
            defaultClipModel.LoadModel(new idTraceModel(new idBounds(new idVec3(0, 0, 0)).Expand(8)));

            // set counters to zero
            numRotations = numTranslations = numMotions = numRenderModelTraces = numContents = numContacts = 0;
        }

        public void Shutdown() {
//	delete[] clipSectors;
            clipSectors = null;

            // free the trace model used for the temporaryClipModel
            if (temporaryClipModel.traceModelIndex != -1) {
                idClipModel.FreeTraceModel(temporaryClipModel.traceModelIndex);
                temporaryClipModel.traceModelIndex = -1;
            }

            // free the trace model used for the defaultClipModel
            if (defaultClipModel.traceModelIndex != -1) {
                idClipModel.FreeTraceModel(defaultClipModel.traceModelIndex);
                defaultClipModel.traceModelIndex = -1;
            }
//
//            clipLinkAllocator.Shutdown();
        }

        // clip versus the rest of the world
        public boolean Translation(trace_s[] results, final idVec3 start, final idVec3 end,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, num;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idBounds traceBounds = new idBounds();
            float radius;
            trace_s[] trace = {new trace_s()};
            idTraceModel trm;

            if (TestHugeTranslation(results[0], mdl, start, end, trmAxis)) {
                return true;
            }

            trm = TraceModelForClipModel(mdl);

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // test world
                this.numTranslations++;
                collisionModelManager.Translation(results, start, end, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
                results[0].c.entityNum = results[0].fraction != 1.0f ? ENTITYNUM_WORLD : ENTITYNUM_NONE;
                if (results[0].fraction == 0.0f) {
                    return true;		// blocked immediately by the world
                }
            } else {
//		memset( &results, 0, sizeof( results ) );
                results[0] = new trace_s();
                results[0].fraction = 1.0f;
                results[0].endpos.oSet(end);
                results[0].endAxis.oSet(trmAxis);
            }

            if (null == trm) {
                traceBounds.FromPointTranslation(start, results[0].endpos.oMinus(start));
                radius = 0.0f;
            } else {
                traceBounds.FromBoundsTranslation(trm.bounds, start, trmAxis, results[0].endpos.oMinus(start));
                radius = trm.bounds.GetRadius();
            }

            num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);

            for (i = 0; i < num; i++) {
                touch = clipModelList[i];

                if (null == touch) {
                    continue;
                }

                if (touch.renderModelHandle != -1) {
                    this.numRenderModelTraces++;
                    TraceRenderModel(trace[0], start, end, radius, trmAxis, touch);
                } else {
                    this.numTranslations++;
                    collisionModelManager.Translation(trace, start, end, trm, trmAxis, contentMask, touch.Handle(), touch.origin, touch.axis);
                }

                if (trace[0].fraction < results[0].fraction) {
                    results[0] = trace[0];
                    results[0].c.entityNum = touch.entity.entityNumber;
                    results[0].c.id = touch.id;
                    if (results[0].fraction == 0.0f) {
                        break;
                    }
                }
            }

            return (results[0].fraction < 1.0f);
        }

        public boolean Rotation(trace_s[] results, final idVec3 start, final idRotation rotation,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, num;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idBounds traceBounds = new idBounds();
            trace_s[] trace = new trace_s[1];
            idTraceModel trm;

            trm = TraceModelForClipModel(mdl);

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // test world
                this.numRotations++;
                collisionModelManager.Rotation(results, start, rotation, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
                results[0].c.entityNum = results[0].fraction != 1.0f ? ENTITYNUM_WORLD : ENTITYNUM_NONE;
                if (results[0].fraction == 0.0f) {
                    return true;		// blocked immediately by the world
                }
            } else {
//		memset( &results, 0, sizeof( results ) );
                results[0] = new trace_s();
                results[0].fraction = 1.0f;
                results[0].endpos.oSet(start);
                results[0].endAxis.oSet(trmAxis.oMultiply(rotation.ToMat3()));
            }

            if (null == trm) {
                traceBounds.FromPointRotation(start, rotation);
            } else {
                traceBounds.FromBoundsRotation(trm.bounds, start, trmAxis, rotation);
            }

            num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);

            for (i = 0; i < num; i++) {
                touch = clipModelList[i];

                if (null == touch) {
                    continue;
                }

                // no rotational collision with render models
                if (touch.renderModelHandle != -1) {
                    continue;
                }

                this.numRotations++;
                collisionModelManager.Rotation(trace, start, rotation, trm, trmAxis, contentMask, touch.Handle(), touch.origin, touch.axis);

                if (trace[0].fraction < results[0].fraction) {
                    results[0] = trace[0];
                    results[0].c.entityNum = touch.entity.entityNumber;
                    results[0].c.id = touch.id;
                    if (results[0].fraction == 0.0f) {
                        break;
                    }
                }
            }

            return (results[0].fraction < 1.0f);
        }

        public boolean Motion(trace_s[] results, final idVec3 start, final idVec3 end, final idRotation rotation,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, num;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idVec3 dir, endPosition;
            idBounds traceBounds = new idBounds();
            float radius;
            trace_s[] translationalTrace = {null}, rotationalTrace = {null};
            trace_s[] trace = new trace_s[1];
            idRotation endRotation;
            idTraceModel trm;

            assert (rotation.GetOrigin() == start);

            if (TestHugeTranslation(results[0], mdl, start, end, trmAxis)) {
                return true;
            }

            if (mdl != null && rotation.GetAngle() != 0.0f && !rotation.GetVec().equals(getVec3_origin())) {
                // if no translation
                if (start == end) {
                    // pure rotation
                    return Rotation(results, start, rotation, mdl, trmAxis, contentMask, passEntity);
                }
            } else if (start != end) {
                // pure translation
                return Translation(results, start, end, mdl, trmAxis, contentMask, passEntity);
            } else {
                // no motion
                results[0].fraction = 1.0f;
                results[0].endpos.oSet(start);
                results[0].endAxis.oSet(trmAxis);
                return false;
            }

            trm = TraceModelForClipModel(mdl);

            radius = trm.bounds.GetRadius();

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // translational collision with world
                this.numTranslations++;
                collisionModelManager.Translation(translationalTrace, start, end, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
                translationalTrace[0].c.entityNum = translationalTrace[0].fraction != 1.0f ? ENTITYNUM_WORLD : ENTITYNUM_NONE;
            } else {
//		memset( &translationalTrace, 0, sizeof( translationalTrace ) );
                translationalTrace[0] = new trace_s();
                translationalTrace[0].fraction = 1.0f;
                translationalTrace[0].endpos.oSet(end);
                translationalTrace[0].endAxis.oSet(trmAxis);
            }

            if (translationalTrace[0].fraction != 0.0f) {

                traceBounds.FromBoundsRotation(trm.bounds, start, trmAxis, rotation);
                dir = translationalTrace[0].endpos.oMinus(start);
                for (i = 0; i < 3; i++) {
                    if (dir.oGet(i) < 0.0f) {
                        traceBounds.oGet(0).oPluSet(i, dir.oGet(i));
                    } else {
                        traceBounds.oGet(1).oPluSet(i, dir.oGet(i));
                    }
                }

                num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);

                for (i = 0; i < num; i++) {
                    touch = clipModelList[i];

                    if (null == touch) {
                        continue;
                    }

                    if (touch.renderModelHandle != -1) {
                        this.numRenderModelTraces++;
                        TraceRenderModel(trace[0], start, end, radius, trmAxis, touch);
                    } else {
                        this.numTranslations++;
                        collisionModelManager.Translation(trace, start, end, trm, trmAxis, contentMask, touch.Handle(), touch.origin, touch.axis);
                    }

                    if (trace[0].fraction < translationalTrace[0].fraction) {
                        translationalTrace[0] = trace[0];
                        translationalTrace[0].c.entityNum = touch.entity.entityNumber;
                        translationalTrace[0].c.id = touch.id;
                        if (translationalTrace[0].fraction == 0.0f) {
                            break;
                        }
                    }
                }
            } else {
                num = -1;
            }

            endPosition = translationalTrace[0].endpos;
            endRotation = new idRotation(rotation);
            endRotation.SetOrigin(endPosition);

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // rotational collision with world
                this.numRotations++;
                collisionModelManager.Rotation(rotationalTrace, endPosition, endRotation, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
                rotationalTrace[0].c.entityNum = rotationalTrace[0].fraction != 1.0f ? ENTITYNUM_WORLD : ENTITYNUM_NONE;
            } else {
//		memset( &rotationalTrace, 0, sizeof( rotationalTrace ) );
                rotationalTrace[0] = new trace_s();
                rotationalTrace[0].fraction = 1.0f;
                rotationalTrace[0].endpos.oSet(endPosition);
                rotationalTrace[0].endAxis.oSet(trmAxis.oMultiply(rotation.ToMat3()));
            }

            if (rotationalTrace[0].fraction != 0.0f) {

                if (num == -1) {
                    traceBounds.FromBoundsRotation(trm.bounds, endPosition, trmAxis, endRotation);
                    num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);
                }

                for (i = 0; i < num; i++) {
                    touch = clipModelList[i];

                    if (null == touch) {
                        continue;
                    }

                    // no rotational collision detection with render models
                    if (touch.renderModelHandle != -1) {
                        continue;
                    }

                    this.numRotations++;
                    collisionModelManager.Rotation(trace, endPosition, endRotation, trm, trmAxis, contentMask, touch.Handle(), touch.origin, touch.axis);

                    if (trace[0].fraction < rotationalTrace[0].fraction) {
                        rotationalTrace[0].oSet(trace[0]);
                        rotationalTrace[0].c.entityNum = touch.entity.entityNumber;
                        rotationalTrace[0].c.id = touch.id;
                        if (rotationalTrace[0].fraction == 0.0f) {
                            break;
                        }
                    }
                }
            }

            if (rotationalTrace[0].fraction < 1.0f) {
                results[0].oSet(rotationalTrace[0]);
            } else {
                results[0].oSet(translationalTrace[0]);
                results[0].endAxis.oSet(rotationalTrace[0].endAxis);
            }

            results[0].fraction = (float) Max(translationalTrace[0].fraction, rotationalTrace[0].fraction);

            return (translationalTrace[0].fraction < 1.0f || rotationalTrace[0].fraction < 1.0f);
        }

        public int Contacts(contactInfo_t[] contacts, final int maxContacts, final idVec3 start, final idVec6 dir, final float depth,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, j, num, n, numContacts;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idBounds traceBounds = new idBounds();
            idTraceModel trm;

            trm = TraceModelForClipModel(mdl);

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // test world
                this.numContacts++;
                numContacts = collisionModelManager.Contacts(contacts, maxContacts, start, dir, depth, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
            } else {
                numContacts = 0;
            }

            for (i = 0; i < numContacts; i++) {
                contacts[i].entityNum = ENTITYNUM_WORLD;
                contacts[i].id = 0;
            }

            if (numContacts >= maxContacts) {
                return numContacts;
            }

            if (null == trm) {
                traceBounds = new idBounds(start).Expand(depth);
            } else {
                traceBounds.FromTransformedBounds(trm.bounds, start, trmAxis);
                traceBounds.ExpandSelf(depth);
            }

            num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);

            for (i = 0; i < num; i++) {
                touch = clipModelList[i];

                if (null == touch) {
                    continue;
                }

                // no contacts with render models
                if (touch.renderModelHandle != -1) {
                    continue;
                }

                this.numContacts++;
                contactInfo_t[] contactz = Arrays.copyOfRange(contacts, numContacts, contacts.length);
                n = collisionModelManager.Contacts(
                        contactz, maxContacts - numContacts,
                        start, dir, depth, trm, trmAxis, contentMask,
                        touch.Handle(), touch.origin, touch.axis);

                for (j = 0; j < n; j++) {
                    contacts[numContacts] = contactz[j];
                    contacts[numContacts].entityNum = touch.entity.entityNumber;
                    contacts[numContacts].id = touch.id;
                    numContacts++;
                }

                if (numContacts >= maxContacts) {
                    break;
                }
            }

            return numContacts;
        }

        public int Contents(final idVec3 start, final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, num, contents;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idBounds traceBounds = new idBounds();
            idTraceModel trm;

            trm = TraceModelForClipModel(mdl);

            if (null == passEntity || passEntity.entityNumber != ENTITYNUM_WORLD) {
                // test world
                this.numContents++;
                contents = collisionModelManager.Contents(start, trm, trmAxis, contentMask, 0, getVec3_origin(), getMat3_default());
            } else {
                contents = 0;
            }

            if (null == trm) {
                traceBounds.oSet(0, start);
                traceBounds.oSet(1, start);
            } else if (trmAxis.IsRotated()) {
                traceBounds.FromTransformedBounds(trm.bounds, start, trmAxis);
            } else {
                traceBounds.oSet(0, trm.bounds.oGet(0).oPlus(start));
                traceBounds.oSet(1, trm.bounds.oGet(1).oPlus(start));
            }

            num = GetTraceClipModels(traceBounds, -1, passEntity, clipModelList);

            for (i = 0; i < num; i++) {
                touch = clipModelList[i];

                if (null == touch) {
                    continue;
                }

                // no contents test with render models
                if (touch.renderModelHandle != -1) {
                    continue;
                }

                // if the entity does not have any contents we are looking for
                if ((touch.contents & contentMask) == 0) {
                    continue;
                }

                // if the entity has no new contents flags
                if ((touch.contents & contents) == touch.contents) {
                    continue;
                }

                this.numContents++;
                if (collisionModelManager.Contents(start, trm, trmAxis, contentMask, touch.Handle(), touch.origin, touch.axis) != 0) {
                    contents |= (touch.contents & contentMask);
                }
            }

            return contents;
        }

        // special case translations versus the rest of the world
        public boolean TracePoint(trace_s[] results, final idVec3 start, final idVec3 end, int contentMask, final idEntity passEntity) {
            Translation(results, start, end, null, getMat3_identity(), contentMask, passEntity);
            return (results[0].fraction < 1.0f);
        }

        public boolean TraceBounds(trace_s[] results, final idVec3 start, final idVec3 end, final idBounds bounds, int contentMask, final idEntity passEntity) {
            temporaryClipModel.LoadModel(new idTraceModel(bounds));
            Translation(results, start, end, temporaryClipModel, getMat3_identity(), contentMask, passEntity);
            return (results[0].fraction < 1.0f);
        }

        // clip versus a specific model
        public void TranslationModel(trace_s[] results, final idVec3 start, final idVec3 end,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, int/*cmHandle_t*/ model, final idVec3 modelOrigin, final idMat3 modelAxis) {
            final idTraceModel trm = TraceModelForClipModel(mdl);
            this.numTranslations++;
            collisionModelManager.Translation(results, start, end, trm, trmAxis, contentMask, model, modelOrigin, modelAxis);
        }

        public void RotationModel(trace_s[] results, final idVec3 start, final idRotation rotation,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, int/*cmHandle_t*/ model, final idVec3 modelOrigin, final idMat3 modelAxis) {
            final idTraceModel trm = TraceModelForClipModel(mdl);
            this.numRotations++;
            collisionModelManager.Rotation(results, start, rotation, trm, trmAxis, contentMask, model, modelOrigin, modelAxis);
        }

        public int ContactsModel(contactInfo_t[] contacts, final int maxContacts, final idVec3 start, final idVec6 dir, final float depth,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, int/*cmHandle_t*/ model, final idVec3 modelOrigin, final idMat3 modelAxis) {
            final idTraceModel trm = TraceModelForClipModel(mdl);
            this.numContacts++;
            return collisionModelManager.Contacts(contacts, maxContacts, start, dir, depth, trm, trmAxis, contentMask, model, modelOrigin, modelAxis);
        }

        public int ContentsModel(final idVec3 start, final idClipModel mdl, final idMat3 trmAxis, int contentMask,
                int/*cmHandle_t*/ model, final idVec3 modelOrigin, final idMat3 modelAxis) {
            final idTraceModel trm = TraceModelForClipModel(mdl);
            this.numContents++;
            return collisionModelManager.Contents(start, trm, trmAxis, contentMask, model, modelOrigin, modelAxis);
        }

        // clip versus all entities but not the world
        public void TranslationEntities(trace_s results, final idVec3 start, final idVec3 end,
                final idClipModel mdl, final idMat3 trmAxis, int contentMask, final idEntity passEntity) {
            int i, num;
            idClipModel touch;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idBounds traceBounds = new idBounds();
            float radius;
            trace_s[] trace = {null};
            idTraceModel trm;

            if (TestHugeTranslation(results, mdl, start, end, trmAxis)) {
                return;
            }

            trm = TraceModelForClipModel(mdl);

            results.fraction = 1.0f;
            results.endpos.oSet(end);
            results.endAxis.oSet(trmAxis);

            if (null == trm) {
                traceBounds.FromPointTranslation(start, end.oMinus(start));
                radius = 0.0f;
            } else {
                traceBounds.FromBoundsTranslation(trm.bounds, start, trmAxis, end.oMinus(start));
                radius = trm.bounds.GetRadius();
            }

            num = GetTraceClipModels(traceBounds, contentMask, passEntity, clipModelList);

            for (i = 0; i < num; i++) {
                touch = clipModelList[i];

                if (null == touch) {
                    continue;
                }

                if (touch.renderModelHandle != -1) {
                    this.numRenderModelTraces++;
                    TraceRenderModel(trace[0], start, end, radius, trmAxis, touch);
                } else {
                    this.numTranslations++;
                    collisionModelManager.Translation(trace, start, end, trm, trmAxis, contentMask,
                            touch.Handle(), touch.origin, touch.axis);
                }

                if (trace[0].fraction < results.fraction) {
                    results = trace[0];
                    results.c.entityNum = touch.entity.entityNumber;
                    results.c.id = touch.id;
                    if (results.fraction == 0.0f) {
                        break;
                    }
                }
            }
        }

        // get a contact feature
        public boolean GetModelContactFeature(final contactInfo_t contact, final idClipModel clipModel, idFixedWinding winding) {
            int i;
            int/*cmHandle_t*/ handle;
            idVec3 start = new idVec3(), end = new idVec3();

            handle = -1;
            winding.Clear();

            if (clipModel == null) {
                handle = 0;
            } else {
                if (clipModel.renderModelHandle != -1) {
                    winding.oPluSet(contact.point);
                    return true;
                } else if (clipModel.traceModelIndex != -1) {
                    handle = collisionModelManager.SetupTrmModel(idClipModel.GetCachedTraceModel(clipModel.traceModelIndex), clipModel.material);
                } else {
                    handle = clipModel.collisionModelHandle;
                }
            }

            // if contact with a collision model
            if (handle != -1) {
                switch (contact.type) {
                    case CONTACT_EDGE: {
                        // the model contact feature is a collision model edge
                        collisionModelManager.GetModelEdge(handle, contact.modelFeature, start, end);
                        winding.oPluSet(start);
                        winding.oPluSet(end);
                        break;
                    }
                    case CONTACT_MODELVERTEX: {
                        // the model contact feature is a collision model vertex
                        collisionModelManager.GetModelVertex(handle, contact.modelFeature, start);
                        winding.oPluSet(start);
                        break;
                    }
                    case CONTACT_TRMVERTEX: {
                        // the model contact feature is a collision model polygon
//                        collisionModelManager.GetModelPolygon(handle, contact.modelFeature, winding);//TODO:is this function necessary?
                        break;
                    }
                }
            }

            // transform the winding to world space
            if (clipModel != null) {
                for (i = 0; i < winding.GetNumPoints(); i++) {
                    winding.oGet(i).ToVec3_oMulSet(clipModel.axis);
                    winding.oGet(i).ToVec3_oPluSet(clipModel.origin);
                }
            }

            return true;
        }

        // get entities/clip models within or touching the given bounds
        public int EntitiesTouchingBounds(final idBounds bounds, int contentMask, idEntity[] entityList, int maxCount) {
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            int i, j, count, entCount;

            count = this.ClipModelsTouchingBounds(bounds, contentMask, clipModelList, MAX_GENTITIES);
            entCount = 0;
            for (i = 0; i < count; i++) {
                // entity could already be in the list because an entity can use multiple clip models
                for (j = 0; j < entCount; j++) {
                    if (entityList[j] == clipModelList[i].entity) {
                        break;
                    }
                }
                if (j >= entCount) {
                    if (entCount >= maxCount) {
                        gameLocal.Warning("idClip::EntitiesTouchingBounds: max count");
                        return entCount;
                    }
                    entityList[entCount] = clipModelList[i].entity;
                    entCount++;
                }
            }

            return entCount;
        }

        public int ClipModelsTouchingBounds(final idBounds bounds, int contentMask, idClipModel[] clipModelList, int maxCount) {
            listParms_s parms = new listParms_s();

            if (bounds.oGet(0, 0) > bounds.oGet(1, 0)
                    || bounds.oGet(0, 1) > bounds.oGet(1, 1)
                    || bounds.oGet(0, 2) > bounds.oGet(1, 2)) {
                // we should not go through the tree for degenerate or backwards bounds
                assert (false);
                return 0;
            }

            parms.bounds.oSet(0, bounds.oGet(0).oMinus(vec3_boxEpsilon));
            parms.bounds.oSet(1, bounds.oGet(1).oPlus(vec3_boxEpsilon));
            parms.contentMask = contentMask;
            parms.list = clipModelList;
            parms.count = 0;
            parms.maxCount = maxCount;

            touchCount++;
            ClipModelsTouchingBounds_r(clipSectors[0], parms);//TODO:check if [0] is good enough.

            return parms.count;
        }

        public idBounds GetWorldBounds() {
            return worldBounds;
        }

        public idClipModel DefaultClipModel() {
            return defaultClipModel;
        }

        // stats and debug drawing
        public void PrintStatistics() {
            gameLocal.Printf("t = %-3d, r = %-3d, m = %-3d, render = %-3d, contents = %-3d, contacts = %-3d\n",
                    numTranslations, numRotations, numMotions, numRenderModelTraces, numContents, numContacts);
            numRotations = numTranslations = numMotions = numRenderModelTraces = numContents = numContacts = 0;
        }

        public void DrawClipModels(final idVec3 eye, final float radius, final idEntity passEntity) {
            int i, num;
            idBounds bounds;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idClipModel clipModel;

            bounds = new idBounds(eye).Expand(radius);

            num = this.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            for (i = 0; i < num; i++) {
                clipModel = clipModelList[i];
                if (clipModel.GetEntity() == passEntity) {
                    continue;
                }
                if (clipModel.renderModelHandle != -1) {
                    gameRenderWorld.DebugBounds(colorCyan, clipModel.GetAbsBounds());
                } else {
                    collisionModelManager.DrawModel(clipModel.Handle(), clipModel.GetOrigin(), clipModel.GetAxis(), eye, radius);
                }
            }
        }

        public boolean DrawModelContactFeature(final contactInfo_t contact, final idClipModel clipModel, int lifetime) {
            int i;
            idMat3 axis;
            idFixedWinding winding = new idFixedWinding();

            if (!GetModelContactFeature(contact, clipModel, winding)) {
                return false;
            }

            axis = contact.normal.ToMat3();

            if (winding.GetNumPoints() == 1) {
                gameRenderWorld.DebugLine(colorCyan, winding.oGet(0).ToVec3(), winding.oGet(0).ToVec3().oPlus(axis.oGet(0).oMultiply(2.0f)), lifetime);
                gameRenderWorld.DebugLine(colorWhite, winding.oGet(0).ToVec3().oMinus(/*- 1.0f * */axis.oGet(1)), winding.oGet(0).ToVec3().oPlus(/*+ 1.0f */axis.oGet(1)), lifetime);
                gameRenderWorld.DebugLine(colorWhite, winding.oGet(0).ToVec3().oMinus(/*- 1.0f * */axis.oGet(2)), winding.oGet(0).ToVec3().oPlus(/*+ 1.0f */axis.oGet(2)), lifetime);
            } else {
                for (i = 0; i < winding.GetNumPoints(); i++) {
                    gameRenderWorld.DebugLine(colorCyan, winding.oGet(i).ToVec3(), winding.oGet((i + 1) % winding.GetNumPoints()).ToVec3(), lifetime);
                }
            }

            axis.oSet(0, axis.oGet(0).oNegative());
            axis.oSet(2, axis.oGet(2).oNegative());
            gameRenderWorld.DrawText(contact.material.GetName(), winding.GetCenter().oMinus(axis.oGet(2).oMultiply(4.0f)), 0.1f, colorWhite, axis, 1, 5000);

            return true;
        }

        /*
         ===============
         idClip::CreateClipSectors_r

         Builds a uniformly subdivided tree for the given world size
         ===============
         */
        private clipSector_s CreateClipSectors_r(final int depth, final idBounds bounds, idVec3 maxSector) {
            int i;
            clipSector_s anode;
            idVec3 size;
            idBounds front, back;

            anode = clipSectors[this.numClipSectors++] = new clipSector_s();

            if (depth == MAX_SECTOR_DEPTH) {
                anode.axis = -1;
                anode.children[0] = anode.children[1] = null;

                for (i = 0; i < 3; i++) {
                    if (bounds.oGet(1, i) - bounds.oGet(0, i) > maxSector.oGet(i)) {
                        maxSector.oSet(i, bounds.oGet(1, i) - bounds.oGet(0, i));
                    }
                }
                return anode;
            }

            size = bounds.oGet(1).oMinus(bounds.oGet(0));
            if (size.oGet(0) >= size.oGet(1) && size.oGet(0) >= size.oGet(2)) {
                anode.axis = 0;
            } else if (size.oGet(1) >= size.oGet(0) && size.oGet(1) >= size.oGet(2)) {
                anode.axis = 1;
            } else {
                anode.axis = 2;
            }

            anode.dist = 0.5f * (bounds.oGet(1, anode.axis) + bounds.oGet(0, anode.axis));

            front = new idBounds(bounds);
            back = new idBounds(bounds);

            front.oSet(0, anode.axis, back.oSet(1, anode.axis, anode.dist));

            anode.children[0] = CreateClipSectors_r(depth + 1, front, maxSector);
            anode.children[1] = CreateClipSectors_r(depth + 1, back, maxSector);

            if(anode.children[1] != null &&
                anode.children[1].clipLinks != null &&
                anode.children[1].clipLinks.clipModel.entity.name.equals("env_gibs_leftleg_1")) {
                int b = 0;
            }
            return anode;
        }

        /*
         ====================
         idClip::ClipModelsTouchingBounds_r
         ====================
         */
        private static class listParms_s {

            idBounds bounds;
            int contentMask;
            idClipModel[] list;
            int count;
            int maxCount;

            public listParms_s() {
                bounds = new idBounds();
            }
        };

        private void ClipModelsTouchingBounds_r(clipSector_s node, listParms_s parms) {

            while (node.axis != -1) {
                if (parms.bounds.oGet(0, node.axis) > node.dist) {
                    node = node.children[0];
                } else if (parms.bounds.oGet(1, node.axis) < node.dist) {
                    node = node.children[1];
                } else {
                    ClipModelsTouchingBounds_r(node.children[0], parms);
                    node = node.children[1];
                }
            }

            int i = 0;
            for (clipLink_s link = node.clipLinks; link != null; link = link.nextInSector) {
                idClipModel check = link.clipModel;
                i++;

                // if the clip model is enabled
                if (!check.enabled) {
                    continue;
                }

                // avoid duplicates in the list
                if (check.touchCount == touchCount) {
                    continue;
                }

                // if the clip model does not have any contents we are looking for
                if (0 == (check.contents & parms.contentMask)) {
                    continue;
                }

                // if the bounds really do overlap
                if (check.absBounds.oGet(0, 0) > parms.bounds.oGet(1, 0)
                        || check.absBounds.oGet(1, 0) < parms.bounds.oGet(0, 0)
                        || check.absBounds.oGet(0, 1) > parms.bounds.oGet(1, 1)
                        || check.absBounds.oGet(1, 1) < parms.bounds.oGet(0, 1)
                        || check.absBounds.oGet(0, 2) > parms.bounds.oGet(1, 2)
                        || check.absBounds.oGet(1, 2) < parms.bounds.oGet(0, 2)) {
                    continue;
                }

                if (parms.count >= parms.maxCount) {
                    gameLocal.Warning("idClip::ClipModelsTouchingBounds_r: max count");
                    return;
                }

                check.touchCount = touchCount;
                parms.list[parms.count] = check;
                parms.count++;
            }
        }

        private idTraceModel TraceModelForClipModel(final idClipModel mdl) {
            if (null == mdl) {
                return null;
            } else {
                if (!mdl.IsTraceModel()) {
                    if (mdl.GetEntity() != null) {
                        gameLocal.Error("TraceModelForClipModel: clip model %d on '%s' is not a trace model\n", mdl.GetId(), mdl.GetEntity().name);
                    } else {
                        gameLocal.Error("TraceModelForClipModel: clip model %d is not a trace model\n", mdl.GetId());
                    }
                }
                return idClipModel.GetCachedTraceModel(mdl.traceModelIndex);
            }
        }

        /*
         ====================
         idClip::GetTraceClipModels

         an ent will be excluded from testing if:
         cm->entity == passEntity ( don't clip against the pass entity )
         cm->entity == passOwner ( missiles don't clip with owner )
         cm->owner == passEntity ( don't interact with your own missiles )
         cm->owner == passOwner ( don't interact with other missiles from same owner )
         ====================
         */
        private int GetTraceClipModels(final idBounds bounds, int contentMask, final idEntity passEntity, idClipModel[] clipModelList) {
            int i, num;
            idClipModel cm;
            idEntity passOwner;

            num = ClipModelsTouchingBounds(bounds, contentMask, clipModelList, MAX_GENTITIES);

            if (null == passEntity) {
                return num;
            }

            if (passEntity.GetPhysics().GetNumClipModels() > 0) {
                passOwner = passEntity.GetPhysics().GetClipModel().GetOwner();
            } else {
                passOwner = null;
            }

            for (i = 0; i < num; i++) {

                cm = clipModelList[i];

                // check if we should ignore this entity
                if (cm.entity == passEntity) {
                    clipModelList[i] = null;			// don't clip against the pass entity
                } else if (cm.entity == passOwner) {
                    clipModelList[i] = null;			// missiles don't clip with their owner
                } else if (cm.owner != null) {
                    if (cm.owner == passEntity) {
                        clipModelList[i] = null;		// don't clip against own missiles
                    } else if (cm.owner == passOwner) {
                        clipModelList[i] = null;		// don't clip against other missiles from same owner
                    }
                }
            }

            return num;
        }

        private void TraceRenderModel(trace_s trace, final idVec3 start, final idVec3 end, final float radius, final idMat3 axis, idClipModel touch) {
            trace.fraction = 1.0f;

            // if the trace is passing through the bounds
            if (touch.absBounds.Expand(radius).LineIntersection(start, end)) {
                modelTrace_s modelTrace = new modelTrace_s();

                // test with exact render model and modify trace_t structure accordingly
                if (gameRenderWorld.ModelTrace(modelTrace, touch.renderModelHandle, start, end, radius)) {
                    trace.fraction = modelTrace.fraction;
                    trace.endAxis.oSet(axis);
                    trace.endpos.oSet(modelTrace.point);
                    trace.c.normal.oSet(modelTrace.normal);
                    trace.c.dist = modelTrace.point.oMultiply(modelTrace.normal);
                    trace.c.point.oSet(modelTrace.point);
                    trace.c.type = CONTACT_TRMVERTEX;
                    trace.c.modelFeature = 0;
                    trace.c.trmFeature = 0;
                    trace.c.contents = modelTrace.material.GetContentFlags();
                    trace.c.material = modelTrace.material;
                    // NOTE: trace.c.id will be the joint number
                    touch.id = JOINT_HANDLE_TO_CLIPMODEL_ID(modelTrace.jointNumber);
                }
            }
        }
    };

    /*
     ============
     idClip::TestHugeTranslation
     ============
     */
    public static boolean TestHugeTranslation(trace_s results, final idClipModel mdl, final idVec3 start, final idVec3 end, final idMat3 trmAxis) {
        if (mdl != null && (end.oMinus(start)).LengthSqr() > Square(CM_MAX_TRACE_DIST)) {
            assert (false);

            results.fraction = 0.0f;
            results.endpos.oSet(start);
            results.endAxis.oSet(trmAxis);
            results.c = new contactInfo_t();//memset( results.c, 0, sizeof( results.c ) );
            results.c.point.oSet(start);
            results.c.entityNum = ENTITYNUM_WORLD;

            if (mdl.GetEntity() != null) {
                gameLocal.Printf("huge translation for clip model %d on entity %d '%s'\n", mdl.GetId(), mdl.GetEntity().entityNumber, mdl.GetEntity().GetName());
            } else {
                gameLocal.Printf("huge translation for clip model %d\n", mdl.GetId());
            }
            return true;
        }
        return false;
    }
}
