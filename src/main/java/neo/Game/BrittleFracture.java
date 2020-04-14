package neo.Game;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.LittleBitField;
import static neo.idlib.Lib.PackColor;
import static neo.idlib.geometry.Winding.MAX_POINTS_ON_WINDING;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import static neo.idlib.math.Simd.SIMDProcessor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Physics.Physics_StaticMulti.idPhysics_StaticMulti;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld.deferredEntityCallback_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class BrittleFracture {
    /*
     ===============================================================================

     B-rep Brittle Fracture - Static entity using the boundary representation
     of the render model which can fracture.

     ===============================================================================
     */

    public static class shard_s {

        idClipModel            clipModel;
        idFixedWinding         winding;
        idList<idFixedWinding> decals;
        idList<Boolean>        edgeHasNeighbour;
        idList<shard_s>        neighbours;
        idPhysics_RigidBody    physicsObj;
        int                    droppedTime;
        boolean                atEdge;
        int                    islandNum;
    }
//
    public static final int SHARD_ALIVE_TIME = 5000;
    public static final int SHARD_FADE_START = 2000;
//
    public static final String brittleFracture_SnapshotName = "_BrittleFracture_Snapshot_";
//

    public static class idBrittleFracture extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public CLASS_PROTOTYPE( idBrittleFracture );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idBrittleFracture>) idBrittleFracture::Event_Activate);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idBrittleFracture>) idBrittleFracture::Event_Touch);
        }


        //        
        // enum {
        public static final int EVENT_PROJECT_DECAL = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_SHATTER       = 1 + EVENT_PROJECT_DECAL;
        public static final int EVENT_MAXEVENTS     = 2 + EVENT_PROJECT_DECAL;
        // };
        //        

        //
        // setttings
        private idMaterial            material;
        private idMaterial            decalMaterial;
        private float                 decalSize;
        private float                 maxShardArea;
        private float                 maxShatterRadius;
        private float                 minShatterRadius;
        private float                 linearVelocityScale;
        private float                 angularVelocityScale;
        private float                 shardMass;
        private float                 density;
        private float                 friction;
        private float                 bouncyness;
        private final idStr                 fxFracture;
        //
        // state
        private idPhysics_StaticMulti physicsObj;
        private final idList<shard_s>       shards;
        private final idBounds              bounds;
        private boolean               disableFracture;
        //
        // for rendering
        private int                   lastRenderEntityUpdate;
        private boolean               changed;
        //
        //

        public idBrittleFracture() {
            this.material = null;
            this.decalMaterial = null;
            this.decalSize = 0;
            this.maxShardArea = 0;
            this.maxShatterRadius = 0;
            this.minShatterRadius = 0;
            this.linearVelocityScale = 0;
            this.angularVelocityScale = 0;
            this.shardMass = 0;
            this.density = 0;
            this.friction = 0;
            this.bouncyness = 0;
            this.fxFracture = new idStr();

            this.shards = new idList<>();
            this.bounds = idBounds.ClearBounds();
            this.disableFracture = false;

            this.lastRenderEntityUpdate = -1;
            this.changed = false;

            this.fl.networkSync = true;
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i, j;

            savefile.WriteInt(this.health);
            final entityFlags_s flags = this.fl;
            LittleBitField(flags);
            savefile.Write(flags);

            // setttings
            savefile.WriteMaterial(this.material);
            savefile.WriteMaterial(this.decalMaterial);
            savefile.WriteFloat(this.decalSize);
            savefile.WriteFloat(this.maxShardArea);
            savefile.WriteFloat(this.maxShatterRadius);
            savefile.WriteFloat(this.minShatterRadius);
            savefile.WriteFloat(this.linearVelocityScale);
            savefile.WriteFloat(this.angularVelocityScale);
            savefile.WriteFloat(this.shardMass);
            savefile.WriteFloat(this.density);
            savefile.WriteFloat(this.friction);
            savefile.WriteFloat(this.bouncyness);
            savefile.WriteString(this.fxFracture);

            // state
            savefile.WriteBounds(this.bounds);
            savefile.WriteBool(this.disableFracture);

            savefile.WriteInt(this.lastRenderEntityUpdate);
            savefile.WriteBool(this.changed);

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteInt(this.shards.Num());
            for (i = 0; i < this.shards.Num(); i++) {
                savefile.WriteWinding(this.shards.oGet(i).winding);

                savefile.WriteInt(this.shards.oGet(i).decals.Num());
                for (j = 0; j < this.shards.oGet(i).decals.Num(); j++) {
                    savefile.WriteWinding(this.shards.oGet(i).decals.oGet(j));
                }

                savefile.WriteInt(this.shards.oGet(i).neighbours.Num());
                for (j = 0; j < this.shards.oGet(i).neighbours.Num(); j++) {
                    final int index = this.shards.FindIndex(this.shards.oGet(i).neighbours.oGet(j));
                    assert (index != -1);
                    savefile.WriteInt(index);
                }

                savefile.WriteInt(this.shards.oGet(i).edgeHasNeighbour.Num());
                for (j = 0; j < this.shards.oGet(i).edgeHasNeighbour.Num(); j++) {
                    savefile.WriteBool(this.shards.oGet(i).edgeHasNeighbour.oGet(j));
                }

                savefile.WriteInt(this.shards.oGet(i).droppedTime);
                savefile.WriteInt(this.shards.oGet(i).islandNum);
                savefile.WriteBool(this.shards.oGet(i).atEdge);
                savefile.WriteStaticObject(this.shards.oGet(i).physicsObj);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i, j;
            final int[] num = new int[1];

            this.renderEntity.hModel = renderModelManager.AllocModel();
            this.renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);
            this.renderEntity.callback = idBrittleFracture.ModelCallback.getInstance();
            this.renderEntity.noShadow = true;
            this.renderEntity.noSelfShadow = true;
            this.renderEntity.noDynamicInteractions = false;

            this.health = savefile.ReadInt();
            savefile.Read(this.fl);
            LittleBitField(this.fl);

            // setttings
            savefile.ReadMaterial(this.material);
            savefile.ReadMaterial(this.decalMaterial);
            this.decalSize = savefile.ReadFloat();
            this.maxShardArea = savefile.ReadFloat();
            this.maxShatterRadius = savefile.ReadFloat();
            this.minShatterRadius = savefile.ReadFloat();
            this.linearVelocityScale = savefile.ReadFloat();
            this.angularVelocityScale = savefile.ReadFloat();
            this.shardMass = savefile.ReadFloat();
            this.density = savefile.ReadFloat();
            this.friction = savefile.ReadFloat();
            this.bouncyness = savefile.ReadFloat();
            savefile.ReadString(this.fxFracture);

            // state
            savefile.ReadBounds(this.bounds);
            this.disableFracture = savefile.ReadBool();

            this.lastRenderEntityUpdate = savefile.ReadInt();
            this.changed = savefile.ReadBool();

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            savefile.ReadInt(num);
            this.shards.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.shards.oSet(i, new shard_s());
            }

            for (i = 0; i < num[0]; i++) {
                savefile.ReadWinding(this.shards.oGet(i).winding);

                j = savefile.ReadInt();
                this.shards.oGet(i).decals.SetNum(j);
                for (j = 0; j < this.shards.oGet(i).decals.Num(); j++) {
                    this.shards.oGet(i).decals.oSet(j, new idFixedWinding());
                    savefile.ReadWinding(this.shards.oGet(i).decals.oGet(j));//TODO:pointer of begin range?
                }

                j = savefile.ReadInt();
                this.shards.oGet(i).neighbours.SetNum(j);
                for (j = 0; j < this.shards.oGet(i).neighbours.Num(); j++) {
                    final int[] index = new int[1];
                    savefile.ReadInt(index);
                    assert (index[0] != -1);
                    this.shards.oGet(i).neighbours.oSet(j, this.shards.oGet(index[0]));
                }

                j = savefile.ReadInt();
                this.shards.oGet(i).edgeHasNeighbour.SetNum(j);
                for (j = 0; j < this.shards.oGet(i).edgeHasNeighbour.Num(); j++) {
                    this.shards.oGet(i).edgeHasNeighbour.oSet(j, savefile.ReadBool());
                }

                this.shards.oGet(i).droppedTime = savefile.ReadInt();
                this.shards.oGet(i).islandNum = savefile.ReadInt();
                this.shards.oGet(i).atEdge = savefile.ReadBool();
                savefile.ReadStaticObject(this.shards.oGet(i).physicsObj);
                if (this.shards.oGet(i).droppedTime < 0) {
                    this.shards.oGet(i).clipModel = this.physicsObj.GetClipModel(i);
                } else {
                    this.shards.oGet(i).clipModel = this.shards.oGet(i).physicsObj.GetClipModel();
                }
            }
        }

        @Override
        public void Spawn() {
            final float[] d = {0}, f = {0}, b = {0};

            // get shard properties
            this.decalMaterial = declManager.FindMaterial(this.spawnArgs.GetString("mtr_decal"));
            this.decalSize = this.spawnArgs.GetFloat("decalSize", "40");
            this.maxShardArea = this.spawnArgs.GetFloat("maxShardArea", "200");
            this.maxShardArea = idMath.ClampFloat(100, 10000, this.maxShardArea);
            this.maxShatterRadius = this.spawnArgs.GetFloat("maxShatterRadius", "40");
            this.minShatterRadius = this.spawnArgs.GetFloat("minShatterRadius", "10");
            this.linearVelocityScale = this.spawnArgs.GetFloat("linearVelocityScale", "0.1");
            this.angularVelocityScale = this.spawnArgs.GetFloat("angularVelocityScale", "40");
            this.fxFracture.oSet(this.spawnArgs.GetString("fx"));

            // get rigid body properties
            this.shardMass = this.spawnArgs.GetFloat("shardMass", "20");
            this.shardMass = idMath.ClampFloat(0.001f, 1000.0f, this.shardMass);
            this.spawnArgs.GetFloat("density", "0.1", d);
            this.density = idMath.ClampFloat(0.001f, 1000.0f, d[0]);
            this.spawnArgs.GetFloat("friction", "0.4", f);
            this.friction = idMath.ClampFloat(0.0f, 1.0f, f[0]);
            this.spawnArgs.GetFloat("bouncyness", "0.01", b);
            this.bouncyness = idMath.ClampFloat(0.0f, 1.0f, b[0]);

            this.disableFracture = this.spawnArgs.GetBool("disableFracture", "0");
            this.health = this.spawnArgs.GetInt("health", "40");
            this.fl.takedamage = true;

            // FIXME: set "bleed" so idProjectile calls AddDamageEffect
            this.spawnArgs.SetBool("bleed", true);

            CreateFractures(this.renderEntity.hModel);

            FindNeighbours();

            this.renderEntity.hModel = renderModelManager.AllocModel();
            this.renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);
            this.renderEntity.callback = idBrittleFracture.ModelCallback.getInstance();
            this.renderEntity.noShadow = true;
            this.renderEntity.noSelfShadow = true;
            this.renderEntity.noDynamicInteractions = false;
        }

        @Override
        public void Present() {

            // don't present to the renderer if the entity hasn't changed
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            this.renderEntity.bounds.oSet(this.bounds);
            this.renderEntity.origin.Zero();
            this.renderEntity.axis.Identity();

            // force an update because the bounds/origin/axis may stay the same while the model changes
            this.renderEntity.forceUpdate = 1;//true;

            // add to refresh list
            if (this.modelDefHandle == -1) {
                this.modelDefHandle = gameRenderWorld.AddEntityDef(this.renderEntity);
            } else {
                gameRenderWorld.UpdateEntityDef(this.modelDefHandle, this.renderEntity);
            }

            this.changed = true;
        }

        @Override
        public void Think() {
            int i, startTime, endTime, droppedTime;
            shard_s shard;
            boolean atRest = true, fading = false;

            // remove overdue shards
            for (i = 0; i < this.shards.Num(); i++) {
                droppedTime = this.shards.oGet(i).droppedTime;
                if (droppedTime != -1) {
                    if ((gameLocal.time - droppedTime) > SHARD_ALIVE_TIME) {
                        RemoveShard(i);
                        i--;
                    }
                    fading = true;
                }
            }

            // remove the entity when nothing is visible
            if (0 == this.shards.Num()) {
                PostEventMS(EV_Remove, 0);
                return;
            }

            if ((this.thinkFlags & TH_PHYSICS) != 0) {

                startTime = gameLocal.previousTime;
                endTime = gameLocal.time;

                // run physics on shards
                for (i = 0; i < this.shards.Num(); i++) {
                    shard = this.shards.oGet(i);

                    if (shard.droppedTime == -1) {
                        continue;
                    }

                    shard.physicsObj.Evaluate(endTime - startTime, endTime);

                    if (!shard.physicsObj.IsAtRest()) {
                        atRest = false;
                    }
                }

                if (atRest) {
                    BecomeInactive(TH_PHYSICS);
                } else {
                    BecomeActive(TH_PHYSICS);
                }
            }

            if (!atRest || this.bounds.IsCleared()) {
                this.bounds.Clear();
                for (i = 0; i < this.shards.Num(); i++) {
                    this.bounds.AddBounds(this.shards.oGet(i).clipModel.GetAbsBounds());
                }
            }

            if (fading) {
                BecomeActive(TH_UPDATEVISUALS | TH_THINK);
            } else {
                BecomeInactive(TH_THINK);
            }

            RunPhysics();
            Present();
        }

        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {

            if ((id < 0) || (id >= this.shards.Num())) {
                return;
            }

            if (this.shards.oGet(id).droppedTime != -1) {
                this.shards.oGet(id).physicsObj.ApplyImpulse(0, point, impulse);
            } else if ((this.health <= 0) && !this.disableFracture) {
                Shatter(point, impulse, gameLocal.time);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {

            if ((id < 0) || (id >= this.shards.Num())) {
                return;
            }

            if (this.shards.oGet(id).droppedTime != -1) {
                this.shards.oGet(id).physicsObj.AddForce(0, point, force);
            } else if ((this.health <= 0) && !this.disableFracture) {
                Shatter(point, force, gameLocal.time);
            }
        }

        @Override
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            if (!this.disableFracture) {
                ProjectDecal(collision.c.point, collision.c.normal, gameLocal.time, damageDefName);
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (!this.disableFracture) {
                ActivateTargets(this);
                Break();
            }
        }

        public void ProjectDecal(final idVec3 point, final idVec3 dir, final int time, final String damageDefName) {
            int i, j, bits, clipBits;
            float a, c, s;
            final idVec2[] st = new idVec2[MAX_POINTS_ON_WINDING];
            idVec3 origin;
            idMat3 axis = new idMat3();
			final idMat3 axisTemp = new idMat3();
            final idPlane[] textureAxis = new idPlane[2];

            if (gameLocal.isServer) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteFloat(point.oGet(0));
                msg.WriteFloat(point.oGet(1));
                msg.WriteFloat(point.oGet(2));
                msg.WriteFloat(dir.oGet(0));
                msg.WriteFloat(dir.oGet(1));
                msg.WriteFloat(dir.oGet(2));
                ServerSendEvent(EVENT_PROJECT_DECAL, msg, true, -1);
            }

            if (time >= gameLocal.time) {
                // try to get the sound from the damage def
                idDeclEntityDef damageDef;
                idSoundShader sndShader = null;
                if (damageDefName != null) {
                    damageDef = gameLocal.FindEntityDef(damageDefName, false);
                    if (damageDef != null) {
                        sndShader = declManager.FindSound(damageDef.dict.GetString("snd_shatter", ""));
                    }
                }

                if (sndShader != null) {
                    StartSoundShader(sndShader, etoi(SND_CHANNEL_ANY), 0, false, null);
                } else {
                    StartSound("snd_bullethole", SND_CHANNEL_ANY, 0, false, null);
                }
            }

            a = gameLocal.random.RandomFloat() * idMath.TWO_PI;
            c = (float) cos(a);
            s = (float) -sin(a);

            axis.oSet(2, dir.oNegative());
            axis.oGet(2).Normalize();
            axis.oGet(2).NormalVectors(axisTemp.oGet(0), axisTemp.oGet(1));
            axis.oSet(0, axisTemp.oGet(0).oMultiply(c).oPlus(axisTemp.oGet(1).oMultiply(s)));
            axis.oSet(1, axisTemp.oGet(0).oMultiply(s).oPlus(axisTemp.oGet(1).oMultiply(-c)));

            textureAxis[0].oSet(axis.oGet(0).oMultiply(1.0f / this.decalSize));
            textureAxis[0].oSet(3, -(point.oMultiply(textureAxis[0].Normal())) + 0.5f);

            textureAxis[1].oSet(axis.oGet(1).oMultiply(1.0f / this.decalSize));
            textureAxis[1].oSet(3, -(point.oMultiply(textureAxis[1].Normal())) + 0.5f);

            for (i = 0; i < this.shards.Num(); i++) {
                final idFixedWinding winding = this.shards.oGet(i).winding;
                origin = this.shards.oGet(i).clipModel.GetOrigin();
                axis = this.shards.oGet(i).clipModel.GetAxis();
                float d0, d1;

                clipBits = -1;
                for (j = 0; j < winding.GetNumPoints(); j++) {
                    final idVec3 p = origin.oPlus(winding.oGet(j).ToVec3().oMultiply(axis));

                    st[j].x = d0 = textureAxis[0].Distance(p);
                    st[j].y = d1 = textureAxis[1].Distance(p);

                    bits = FLOATSIGNBITSET(d0);
                    d0 = 1.0f - d0;
                    bits |= FLOATSIGNBITSET(d1) << 2;
                    d1 = 1.0f - d1;
                    bits |= FLOATSIGNBITSET(d0) << 1;
                    bits |= FLOATSIGNBITSET(d1) << 3;

                    clipBits &= bits;
                }

                if (clipBits != 0) {
                    continue;
                }

                final idFixedWinding decal = new idFixedWinding();
                this.shards.oGet(i).decals.Append(decal);

                decal.SetNumPoints(winding.GetNumPoints());
                for (j = 0; j < winding.GetNumPoints(); j++) {
                    decal.oGet(j).oSet(winding.oGet(j).ToVec3());//TODO:double check this.
                    decal.oGet(j).s = st[j].x;
                    decal.oGet(j).t = st[j].y;
                }
            }

            BecomeActive(TH_UPDATEVISUALS);
        }

        public boolean IsBroken() {
            return (this.fl.takedamage == false);
        }

        @Override
        public void ClientPredictionThink() {
            // only think forward because the state is not synced through snapshots
            if (!gameLocal.isNewFrame) {
                return;
            }

            Think();
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            final idVec3 point = new idVec3(), dir = new idVec3();

            switch (event) {
                case EVENT_PROJECT_DECAL: {
                    point.oSet(0, msg.ReadFloat());
                    point.oSet(1, msg.ReadFloat());
                    point.oSet(2, msg.ReadFloat());
                    dir.oSet(0, msg.ReadFloat());
                    dir.oSet(1, msg.ReadFloat());
                    dir.oSet(2, msg.ReadFloat());
                    ProjectDecal(point, dir, time, null);
                    return true;
                }
                case EVENT_SHATTER: {
                    point.oSet(0, msg.ReadFloat());
                    point.oSet(1, msg.ReadFloat());
                    point.oSet(2, msg.ReadFloat());
                    dir.oSet(0, msg.ReadFloat());
                    dir.oSet(1, msg.ReadFloat());
                    dir.oSet(2, msg.ReadFloat());
                    Shatter(point, dir, time);
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        @Override
        public boolean UpdateRenderEntity(renderEntity_s renderEntity, final renderView_s renderView) {
            int i, j, k, n, msec, numTris, numDecalTris;
            float fade;
            int/*dword*/ packedColor;
            srfTriangles_s tris, decalTris;
            modelSurface_s surface;
            idDrawVert v;
            final idPlane plane = new idPlane();
            idMat3 tangents;

            // this may be triggered by a model trace or other non-view related source,
            // to which we should look like an empty model
            if (null == renderView) {
                return false;
            }

            // don't regenerate it if it is current
            if ((this.lastRenderEntityUpdate == gameLocal.time) || !this.changed) {
                return false;
            }

            this.lastRenderEntityUpdate = gameLocal.time;
            this.changed = false;

            numTris = 0;
            numDecalTris = 0;
            for (i = 0; i < this.shards.Num(); i++) {
                n = this.shards.oGet(i).winding.GetNumPoints();
                if (n > 2) {
                    numTris += n - 2;
                }
                for (k = 0; k < this.shards.oGet(i).decals.Num(); k++) {
                    n = this.shards.oGet(i).decals.oGet(k).GetNumPoints();
                    if (n > 2) {
                        numDecalTris += n - 2;
                    }
                }
            }

            // FIXME: re-use model surfaces
            renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);

            // allocate triangle surfaces for the fractures and decals
            tris = renderEntity.hModel.AllocSurfaceTriangles(numTris * 3, this.material.ShouldCreateBackSides() ? numTris * 6 : numTris * 3);
            decalTris = renderEntity.hModel.AllocSurfaceTriangles(numDecalTris * 3, this.decalMaterial.ShouldCreateBackSides() ? numDecalTris * 6 : numDecalTris * 3);

            for (i = 0; i < this.shards.Num(); i++) {
                final idVec3 origin = this.shards.oGet(i).clipModel.GetOrigin();
                final idMat3 axis = this.shards.oGet(i).clipModel.GetAxis();

                fade = 1.0f;
                if (this.shards.oGet(i).droppedTime >= 0) {
                    msec = gameLocal.time - this.shards.oGet(i).droppedTime - SHARD_FADE_START;
                    if (msec > 0) {
                        fade = 1.0f - ((float) msec / (SHARD_ALIVE_TIME - SHARD_FADE_START));
                    }
                }
                packedColor = (int) PackColor(new idVec4(renderEntity.shaderParms[SHADERPARM_RED] * fade,
                        renderEntity.shaderParms[ SHADERPARM_GREEN] * fade,
                        renderEntity.shaderParms[ SHADERPARM_BLUE] * fade,
                        fade));

                final idWinding winding = this.shards.oGet(i).winding;

                winding.GetPlane(plane);
                tangents = (plane.Normal().oMultiply(axis)).ToMat3();

                for (j = 2; j < winding.GetNumPoints(); j++) {

                    v = tris.verts[tris.numVerts++];
                    v.Clear();
                    v.xyz = origin.oPlus(winding.oGet(0).ToVec3().oMultiply(axis));
                    v.st.oSet(0, winding.oGet(0).s);
                    v.st.oSet(1, winding.oGet(0).t);
                    v.normal = tangents.oGet(0);
                    v.tangents[0] = tangents.oGet(1);
                    v.tangents[1] = tangents.oGet(2);
                    v.SetColor(packedColor);

                    v = tris.verts[tris.numVerts++];
                    v.Clear();
                    v.xyz = origin.oPlus(winding.oGet(j - 1).ToVec3().oMultiply(axis));
                    v.st.oSet(0, winding.oGet(j - 1).s);
                    v.st.oSet(1, winding.oGet(j - 1).t);
                    v.normal = tangents.oGet(0);
                    v.tangents[0] = tangents.oGet(1);
                    v.tangents[1] = tangents.oGet(2);
                    v.SetColor(packedColor);

                    v = tris.verts[tris.numVerts++];
                    v.Clear();
                    v.xyz = origin.oPlus(winding.oGet(j).ToVec3().oMultiply(axis));
                    v.st.oSet(0, winding.oGet(j).s);
                    v.st.oSet(1, winding.oGet(j).t);
                    v.normal = tangents.oGet(0);
                    v.tangents[0] = tangents.oGet(1);
                    v.tangents[1] = tangents.oGet(2);
                    v.SetColor(packedColor);

                    tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 3;
                    tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 2;
                    tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 1;

                    if (this.material.ShouldCreateBackSides()) {

                        tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 2;
                        tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 3;
                        tris.getIndexes()[tris.incNumIndexes()] = tris.numVerts - 1;
                    }
                }

                for (k = 0; k < this.shards.oGet(i).decals.Num(); k++) {
                    final idWinding decalWinding = this.shards.oGet(i).decals.oGet(k);

                    for (j = 2; j < decalWinding.GetNumPoints(); j++) {

                        v = decalTris.verts[decalTris.numVerts++];
                        v.Clear();
                        v.xyz = origin.oPlus(decalWinding.oGet(0).ToVec3().oMultiply(axis));
                        v.st.oSet(0, decalWinding.oGet(0).s);
                        v.st.oSet(1, decalWinding.oGet(0).t);
                        v.normal = tangents.oGet(0);
                        v.tangents[0] = tangents.oGet(1);
                        v.tangents[1] = tangents.oGet(2);
                        v.SetColor(packedColor);

                        v = decalTris.verts[decalTris.numVerts++];
                        v.Clear();
                        v.xyz = origin.oPlus(decalWinding.oGet(j - 1).ToVec3().oMultiply(axis));
                        v.st.oSet(0, decalWinding.oGet(j - 1).s);
                        v.st.oSet(1, decalWinding.oGet(j - 1).t);
                        v.normal = tangents.oGet(0);
                        v.tangents[0] = tangents.oGet(1);
                        v.tangents[1] = tangents.oGet(2);
                        v.SetColor(packedColor);

                        v = decalTris.verts[decalTris.numVerts++];
                        v.Clear();
                        v.xyz = origin.oPlus(decalWinding.oGet(j).ToVec3().oMultiply(axis));
                        v.st.oSet(0, decalWinding.oGet(j).s);
                        v.st.oSet(1, decalWinding.oGet(j).t);
                        v.normal = tangents.oGet(0);
                        v.tangents[0] = tangents.oGet(1);
                        v.tangents[1] = tangents.oGet(2);
                        v.SetColor(packedColor);

                        decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 3;
                        decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 2;
                        decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 1;

                        if (this.decalMaterial.ShouldCreateBackSides()) {

                            decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 2;
                            decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 3;
                            decalTris.getIndexes()[decalTris.incNumIndexes()] = decalTris.numVerts - 1;
                        }
                    }
                }
            }

            tris.tangentsCalculated = true;
            decalTris.tangentsCalculated = true;

            SIMDProcessor.MinMax(tris.bounds.oGet(0), tris.bounds.oGet(1), tris.verts, tris.numVerts);
            SIMDProcessor.MinMax(decalTris.bounds.oGet(0), decalTris.bounds.oGet(1), decalTris.verts, decalTris.numVerts);

//	memset( &surface, 0, sizeof( surface ) );
            surface = new modelSurface_s();
            surface.shader = this.material;
            surface.id = 0;
            surface.geometry = tris;
            renderEntity.hModel.AddSurface(surface);

//	memset( &surface, 0, sizeof( surface ) );
            surface = new modelSurface_s();
            surface.shader = this.decalMaterial;
            surface.id = 1;
            surface.geometry = decalTris;
            renderEntity.hModel.AddSurface(surface);

            return true;
        }

        public static class ModelCallback extends deferredEntityCallback_t {

            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public static final deferredEntityCallback_t instance = new ModelCallback();

            private ModelCallback() {
            }

            public static deferredEntityCallback_t getInstance() {
                return instance;
            }

            @Override
            public boolean run(renderEntity_s e, renderView_s v) {
                final idBrittleFracture ent;

                ent = (idBrittleFracture) gameLocal.entities[e.entityNum];
                if (null == ent) {
                    idGameLocal.Error("idBrittleFracture::ModelCallback: callback with NULL game entity");
                }

                return ent.UpdateRenderEntity(e, v);
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
        }

        private void AddShard(idClipModel clipModel, idFixedWinding w) {
            final shard_s shard = new shard_s();
            shard.clipModel = clipModel;
            shard.droppedTime = -1;
            shard.winding = w;
            shard.decals.Clear();
            shard.edgeHasNeighbour.AssureSize(w.GetNumPoints(), false);
            shard.neighbours.Clear();
            shard.atEdge = false;
            this.shards.Append(shard);
        }

        private void RemoveShard(int index) {
            int i;

//	delete shards[index];
            this.shards.oSet(index, null);
            this.shards.RemoveIndex(index);
            this.physicsObj.RemoveIndex(index);

            for (i = index; i < this.shards.Num(); i++) {
                this.shards.oGet(i).clipModel.SetId(i);
            }
        }

        private void DropShard(shard_s shard, final idVec3 point, final idVec3 dir, final float impulse, final int time) {
            int i, j, clipModelId;
            float dist, f;
            idVec3 dir2, origin;
            idMat3 axis;
            shard_s neighbour;

            // don't display decals on dropped shards
            shard.decals.DeleteContents(true);

            // remove neighbour pointers of neighbours pointing to this shard
            for (i = 0; i < shard.neighbours.Num(); i++) {
                neighbour = shard.neighbours.oGet(i);
                for (j = 0; j < neighbour.neighbours.Num(); j++) {
                    if (neighbour.neighbours.oGet(j).equals(shard)) {
                        neighbour.neighbours.RemoveIndex(j);
                        break;
                    }
                }
            }

            // remove neighbour pointers
            shard.neighbours.Clear();

            // remove the clip model from the static physics object
            clipModelId = shard.clipModel.GetId();
            this.physicsObj.SetClipModel(null, 1.0f, clipModelId, false);

            origin = shard.clipModel.GetOrigin();
            axis = shard.clipModel.GetAxis();

            // set the dropped time for fading
            shard.droppedTime = time;

            dir2 = origin.oMinus(point);
            dist = dir2.Normalize();
            f = dist > this.maxShatterRadius ? 1.0f : idMath.Sqrt(dist - this.minShatterRadius) * (1.0f / idMath.Sqrt(this.maxShatterRadius - this.minShatterRadius));

            // setup the physics
            shard.physicsObj.SetSelf(this);
            shard.physicsObj.SetClipModel(shard.clipModel, this.density);
            shard.physicsObj.SetMass(this.shardMass);
            shard.physicsObj.SetOrigin(origin);
            shard.physicsObj.SetAxis(axis);
            shard.physicsObj.SetBouncyness(this.bouncyness);
            shard.physicsObj.SetFriction(0.6f, 0.6f, this.friction);
            shard.physicsObj.SetGravity(gameLocal.GetGravity());
            shard.physicsObj.SetContents(CONTENTS_RENDERMODEL);
            shard.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_MOVEABLECLIP);
            shard.physicsObj.ApplyImpulse(0, origin, dir.oMultiply(impulse * this.linearVelocityScale));
            shard.physicsObj.SetAngularVelocity(dir.Cross(dir2).oMultiply(f * this.angularVelocityScale));

            shard.clipModel.SetId(clipModelId);

            BecomeActive(TH_PHYSICS);
        }

        private void Shatter(final idVec3 point, final idVec3 impulse, final int time) {
            int i;
            idVec3 dir;
            shard_s shard;
            float m;

            if (gameLocal.isServer) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteFloat(point.oGet(0));
                msg.WriteFloat(point.oGet(1));
                msg.WriteFloat(point.oGet(2));
                msg.WriteFloat(impulse.oGet(0));
                msg.WriteFloat(impulse.oGet(1));
                msg.WriteFloat(impulse.oGet(2));
                ServerSendEvent(EVENT_SHATTER, msg, true, -1);
            }

            if (time > (gameLocal.time - SHARD_ALIVE_TIME)) {
                StartSound("snd_shatter", SND_CHANNEL_ANY, 0, false, null);
            }

            if (!IsBroken()) {
                Break();
            }

            if (this.fxFracture.Length() != 0) {
                idEntityFx.StartFx(this.fxFracture, point, GetPhysics().GetAxis(), this, true);
            }

            dir = impulse;
            m = dir.Normalize();

            for (i = 0; i < this.shards.Num(); i++) {
                shard = this.shards.oGet(i);

                if (shard.droppedTime != -1) {
                    continue;
                }

                if ((shard.clipModel.GetOrigin().oMinus(point)).LengthSqr() > Square(this.maxShatterRadius)) {
                    continue;
                }

                DropShard(shard, point, dir, m, time);
            }

            DropFloatingIslands(point, impulse, time);
        }

        private void DropFloatingIslands(final idVec3 point, final idVec3 impulse, final int time) {
            int i, j, numIslands;
            int queueStart, queueEnd;
            shard_s curShard, nextShard;
            shard_s[] queue;
            boolean touchesEdge;
            idVec3 dir;

            dir = impulse;
            dir.Normalize();

            numIslands = 0;
            queue = new shard_s[this.shards.Num()];
            for (i = 0; i < this.shards.Num(); i++) {
                this.shards.oGet(i).islandNum = 0;
            }

            for (i = 0; i < this.shards.Num(); i++) {

                if (this.shards.oGet(i).droppedTime != -1) {
                    continue;
                }

                if (this.shards.oGet(i).islandNum != 0) {
                    continue;
                }

                queueStart = 0;
                queueEnd = 1;
                queue[0] = this.shards.oGet(i);
                this.shards.oGet(i).islandNum = numIslands + 1;
                touchesEdge = false;

                if (this.shards.oGet(i).atEdge) {
                    touchesEdge = true;
                }

                for (curShard = queue[queueStart]; queueStart < queueEnd; curShard = queue[++queueStart]) {

                    for (j = 0; j < curShard.neighbours.Num(); j++) {

                        nextShard = curShard.neighbours.oGet(j);

                        if (nextShard.droppedTime != -1) {
                            continue;
                        }

                        if (nextShard.islandNum != 0) {
                            continue;
                        }

                        queue[queueEnd++] = nextShard;
                        nextShard.islandNum = numIslands + 1;

                        if (nextShard.atEdge) {
                            touchesEdge = true;
                        }
                    }
                }
                numIslands++;

                // if the island is not connected to the world at any edges
                if (!touchesEdge) {
                    for (j = 0; j < queueEnd; j++) {
                        DropShard(queue[j], point, dir, 0.0f, time);
                    }
                }
            }
        }

        private void Break() {
            this.fl.takedamage = false;
            this.physicsObj.SetContents(CONTENTS_RENDERMODEL | CONTENTS_TRIGGER);
        }

        private void Fracture_r(idFixedWinding w) {
            int i, j, bestPlane;
            float a, c, s, dist, bestDist;
            idVec3 origin;
            final idPlane windingPlane = new idPlane();
            final idPlane[] splitPlanes = new idPlane[2];
            final idMat3 axis = new idMat3(), axistemp = new idMat3();
            final idFixedWinding back = new idFixedWinding();
            final idTraceModel trm = new idTraceModel();
            idClipModel clipModel;

            while (true) {
                origin = w.GetCenter();
                w.GetPlane(windingPlane);

                if (w.GetArea() < this.maxShardArea) {
                    break;
                }

                // randomly create a split plane
                a = gameLocal.random.RandomFloat() * idMath.TWO_PI;
                c = (float) cos(a);
                s = (float) -sin(a);
                axis.oSet(2, windingPlane.Normal());
                axis.oGet(2).NormalVectors(axistemp.oGet(0), axistemp.oGet(1));
                axis.oSet(0, axistemp.oGet(0).oMultiply(c).oPlus(axistemp.oGet(1).oMultiply(s)));
                axis.oSet(1, axistemp.oGet(0).oMultiply(s).oPlus(axistemp.oGet(1).oMultiply(-c)));

                // get the best split plane
                bestDist = 0.0f;
                bestPlane = 0;
                for (i = 0; i < 2; i++) {
                    splitPlanes[i].SetNormal(axis.oGet(i));
                    splitPlanes[i].FitThroughPoint(origin);
                    for (j = 0; j < w.GetNumPoints(); j++) {
                        dist = splitPlanes[i].Distance(w.oGet(j).ToVec3());
                        if (dist > bestDist) {
                            bestDist = dist;
                            bestPlane = i;
                        }
                    }
                }

                // split the winding
                if (0 == w.Split(back, splitPlanes[bestPlane])) {
                    break;
                }

                // recursively create shards for the back winding
                Fracture_r(back);
            }

            // translate the winding to it's center
            origin = w.GetCenter();
            for (j = 0; j < w.GetNumPoints(); j++) {
                w.oGet(j).ToVec3().oMinSet(origin);
            }
            w.RemoveEqualPoints();

            trm.SetupPolygon(w);
            trm.Shrink(CM_CLIP_EPSILON);
            clipModel = new idClipModel(trm);

            this.physicsObj.SetClipModel(clipModel, 1.0f, this.shards.Num());
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin().oPlus(origin), this.shards.Num());
            this.physicsObj.SetAxis(GetPhysics().GetAxis(), this.shards.Num());

            AddShard(clipModel, w);
        }

        private void CreateFractures(final idRenderModel renderModel) {
            int i, j, k;
            modelSurface_s surf;
            idDrawVert v;
            final idFixedWinding w = new idFixedWinding();

            if (NOT(renderModel)) {
                return;
            }

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin(), 0);
            this.physicsObj.SetAxis(GetPhysics().GetAxis(), 0);

            for (i = 0; i < 1 /*renderModel.NumSurfaces()*/; i++) {
                surf = renderModel.Surface(i);
                this.material = surf.shader;

                for (j = 0; j < surf.geometry.getNumIndexes(); j += 3) {
                    w.Clear();
                    for (k = 0; k < 3; k++) {
                        v = surf.geometry.verts[ surf.geometry.getIndexes()[ (j + 2) - k]];
                        w.AddPoint(v.xyz);
                        w.oGet(k).s = v.st.oGet(0);
                        w.oGet(k).t = v.st.oGet(1);
                    }
                    Fracture_r(w);
                }
            }

            this.physicsObj.SetContents(this.material.GetContentFlags());
            SetPhysics(this.physicsObj);
        }

        private void FindNeighbours() {
            int i, j, k, l;
            idVec3 p1, p2, dir;
            idMat3 axis;
            final idPlane[] plane = new idPlane[4];

            for (i = 0; i < this.shards.Num(); i++) {

                final shard_s shard1 = this.shards.oGet(i);
                final idWinding w1 = shard1.winding;
                final idVec3 origin1 = shard1.clipModel.GetOrigin();
                final idMat3 axis1 = shard1.clipModel.GetAxis();

                for (k = 0; k < w1.GetNumPoints(); k++) {

                    p1 = origin1.oPlus(w1.oGet(k).ToVec3().oMultiply(axis1));
                    p2 = origin1.oPlus(w1.oGet((k + 1) % w1.GetNumPoints()).ToVec3().oMultiply(axis1));
                    dir = p2.oMinus(p1);
                    dir.Normalize();
                    axis = dir.ToMat3();

                    plane[0].SetNormal(dir);
                    plane[0].FitThroughPoint(p1);
                    plane[1].SetNormal(dir.oNegative());
                    plane[1].FitThroughPoint(p2);
                    plane[2].SetNormal(axis.oGet(1));
                    plane[2].FitThroughPoint(p1);
                    plane[3].SetNormal(axis.oGet(2));
                    plane[3].FitThroughPoint(p1);

                    for (j = 0; j < this.shards.Num(); j++) {

                        if (i == j) {
                            continue;
                        }

                        final shard_s shard2 = this.shards.oGet(j);

                        for (l = 0; l < shard1.neighbours.Num(); l++) {
                            if (shard1.neighbours.oGet(l).equals(shard2)) {
                                break;
                            }
                        }
                        if (l < shard1.neighbours.Num()) {
                            continue;
                        }

                        final idWinding w2 = shard2.winding;
                        final idVec3 origin2 = shard2.clipModel.GetOrigin();
                        final idMat3 axis2 = shard2.clipModel.GetAxis();

                        for (l = w2.GetNumPoints() - 1; l >= 0; l--) {
                            p1 = origin1.oPlus(w2.oGet(l).ToVec3().oMultiply(axis2));
                            p2 = origin1.oPlus(w2.oGet((l - 1) % w2.GetNumPoints()).ToVec3().oMultiply(axis2));
                            if ((plane[0].Side(p2, 0.1f) == SIDE_FRONT) && (plane[1].Side(p1, 0.1f) == SIDE_FRONT)) {
                                if ((plane[2].Side(p1, 0.1f) == SIDE_ON) && (plane[3].Side(p1, 0.1f) == SIDE_ON)) {
                                    if ((plane[2].Side(p2, 0.1f) == SIDE_ON) && (plane[3].Side(p2, 0.1f) == SIDE_ON)) {
                                        shard1.neighbours.Append(shard2);
                                        shard1.edgeHasNeighbour.oSet(k, true);
                                        shard2.neighbours.Append(shard1);
                                        shard2.edgeHasNeighbour.oSet(((l - 1) + w2.GetNumPoints()) % w2.GetNumPoints(), true);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                for (k = 0; k < w1.GetNumPoints(); k++) {
                    if (!shard1.edgeHasNeighbour.oGet(k)) {
                        break;
                    }
                }
                if (k < w1.GetNumPoints()) {
                    shard1.atEdge = true;
                } else {
                    shard1.atEdge = false;
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            this.disableFracture = false;
            if (this.health <= 0) {
                Break();
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> _trace) {
            final idEntity other = _other.value;
            final trace_s trace = _trace.value;
            idVec3 point, impulse;

            if (!IsBroken()) {
                return;
            }

            if ((trace.c.id < 0) || (trace.c.id >= this.shards.Num())) {
                return;
            }

            point = this.shards.oGet(trace.c.id).clipModel.GetOrigin();
            impulse = other.GetPhysics().GetLinearVelocity().oMultiply(other.GetPhysics().GetMass());

            Shatter(point, impulse, gameLocal.time);
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

        // virtual						~idBrittleFracture( void );
        @Override
        protected void _deconstructor() {
            int i;

            for (i = 0; i < this.shards.Num(); i++) {
                this.shards.oGet(i).decals.DeleteContents(true);
//                delete shards[i];
            }

            // make sure the render entity is freed before the model is freed
            FreeModelDef();
            renderModelManager.FreeModel(this.renderEntity.hModel);

            super._deconstructor();
        }
    }
}
