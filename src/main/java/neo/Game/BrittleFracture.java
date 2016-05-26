package neo.Game;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import neo.CM.CollisionModel.trace_s;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import static neo.Game.GameSys.Class.EV_Remove;

import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Physics.Physics_StaticMulti.idPhysics_StaticMulti;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import neo.Renderer.RenderWorld.deferredEntityCallback_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import static neo.framework.DeclManager.declManager;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsg;
import static neo.idlib.Lib.LittleBitField;
import static neo.idlib.Lib.PackColor;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.TraceModel.idTraceModel;
import static neo.idlib.geometry.Winding.MAX_POINTS_ON_WINDING;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Simd.SIMDProcessor;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

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

        idClipModel clipModel;
        idFixedWinding winding;
        idList<idFixedWinding> decals;
        idList<Boolean> edgeHasNeighbour;
        idList< shard_s> neighbours;
        idPhysics_RigidBody physicsObj;
        int droppedTime;
        boolean atEdge;
        int islandNum;
    };
//
    public static final int SHARD_ALIVE_TIME = 5000;
    public static final int SHARD_FADE_START = 2000;
//
    public static final String brittleFracture_SnapshotName = "_BrittleFracture_Snapshot_";
//

    public static class idBrittleFracture extends idEntity {
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
        public static final int EVENT_SHATTER = 1 + EVENT_PROJECT_DECAL;
        public static final int EVENT_MAXEVENTS = 2 + EVENT_PROJECT_DECAL;
        // };
        //        

        //
        // setttings
        private idMaterial material;
        private idMaterial decalMaterial;
        private float decalSize;
        private float maxShardArea;
        private float maxShatterRadius;
        private float minShatterRadius;
        private float linearVelocityScale;
        private float angularVelocityScale;
        private float shardMass;
        private float density;
        private float friction;
        private float bouncyness;
        private idStr fxFracture;
        //
        // state
        private idPhysics_StaticMulti physicsObj;
        private idList<shard_s> shards;
        private idBounds bounds;
        private boolean disableFracture;
        //
        // for rendering
        private int lastRenderEntityUpdate;
        private boolean changed;
        //
        //

        public idBrittleFracture() {
            material = null;
            decalMaterial = null;
            decalSize = 0;
            maxShardArea = 0;
            maxShatterRadius = 0;
            minShatterRadius = 0;
            linearVelocityScale = 0;
            angularVelocityScale = 0;
            shardMass = 0;
            density = 0;
            friction = 0;
            bouncyness = 0;
            fxFracture.Clear();

            bounds.Clear();
            disableFracture = false;

            lastRenderEntityUpdate = -1;
            changed = false;

            fl.networkSync = true;
        }
        // virtual						~idBrittleFracture( void );

        @Override
        public void Save(idSaveGame savefile) {
            int i, j;

            savefile.WriteInt(health);
            entityFlags_s flags = fl;
            LittleBitField(flags);
            savefile.Write(flags);

            // setttings
            savefile.WriteMaterial(material);
            savefile.WriteMaterial(decalMaterial);
            savefile.WriteFloat(decalSize);
            savefile.WriteFloat(maxShardArea);
            savefile.WriteFloat(maxShatterRadius);
            savefile.WriteFloat(minShatterRadius);
            savefile.WriteFloat(linearVelocityScale);
            savefile.WriteFloat(angularVelocityScale);
            savefile.WriteFloat(shardMass);
            savefile.WriteFloat(density);
            savefile.WriteFloat(friction);
            savefile.WriteFloat(bouncyness);
            savefile.WriteString(fxFracture);

            // state
            savefile.WriteBounds(bounds);
            savefile.WriteBool(disableFracture);

            savefile.WriteInt(lastRenderEntityUpdate);
            savefile.WriteBool(changed);

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteInt(shards.Num());
            for (i = 0; i < shards.Num(); i++) {
                savefile.WriteWinding(shards.oGet(i).winding);

                savefile.WriteInt(shards.oGet(i).decals.Num());
                for (j = 0; j < shards.oGet(i).decals.Num(); j++) {
                    savefile.WriteWinding(shards.oGet(i).decals.oGet(j));
                }

                savefile.WriteInt(shards.oGet(i).neighbours.Num());
                for (j = 0; j < shards.oGet(i).neighbours.Num(); j++) {
                    int index = shards.FindIndex(shards.oGet(i).neighbours.oGet(j));
                    assert (index != -1);
                    savefile.WriteInt(index);
                }

                savefile.WriteInt(shards.oGet(i).edgeHasNeighbour.Num());
                for (j = 0; j < shards.oGet(i).edgeHasNeighbour.Num(); j++) {
                    savefile.WriteBool(shards.oGet(i).edgeHasNeighbour.oGet(j));
                }

                savefile.WriteInt(shards.oGet(i).droppedTime);
                savefile.WriteInt(shards.oGet(i).islandNum);
                savefile.WriteBool(shards.oGet(i).atEdge);
                savefile.WriteStaticObject(shards.oGet(i).physicsObj);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i, j;
            int[] num = new int[1];

            renderEntity.hModel = renderModelManager.AllocModel();
            renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);
            renderEntity.callback = idBrittleFracture.ModelCallback.getInstance();
            renderEntity.noShadow = true;
            renderEntity.noSelfShadow = true;
            renderEntity.noDynamicInteractions = false;

            health = savefile.ReadInt();
            savefile.Read(fl);
            LittleBitField(fl);

            // setttings
            savefile.ReadMaterial(material);
            savefile.ReadMaterial(decalMaterial);
            decalSize = savefile.ReadFloat();
            maxShardArea = savefile.ReadFloat();
            maxShatterRadius = savefile.ReadFloat();
            minShatterRadius = savefile.ReadFloat();
            linearVelocityScale = savefile.ReadFloat();
            angularVelocityScale = savefile.ReadFloat();
            shardMass = savefile.ReadFloat();
            density = savefile.ReadFloat();
            friction = savefile.ReadFloat();
            bouncyness = savefile.ReadFloat();
            savefile.ReadString(fxFracture);

            // state
            savefile.ReadBounds(bounds);
            disableFracture = savefile.ReadBool();

            lastRenderEntityUpdate = savefile.ReadInt();
            changed = savefile.ReadBool();

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            savefile.ReadInt(num);
            shards.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                shards.oSet(i, new shard_s());
            }

            for (i = 0; i < num[0]; i++) {
                savefile.ReadWinding(shards.oGet(i).winding);

                j = savefile.ReadInt();
                shards.oGet(i).decals.SetNum(j);
                for (j = 0; j < shards.oGet(i).decals.Num(); j++) {
                    shards.oGet(i).decals.oSet(j, new idFixedWinding());
                    savefile.ReadWinding(shards.oGet(i).decals.oGet(j));//TODO:pointer of begin range?
                }

                j = savefile.ReadInt();
                shards.oGet(i).neighbours.SetNum(j);
                for (j = 0; j < shards.oGet(i).neighbours.Num(); j++) {
                    int[] index = new int[1];
                    savefile.ReadInt(index);
                    assert (index[0] != -1);
                    shards.oGet(i).neighbours.oSet(j, shards.oGet(index[0]));
                }

                j = savefile.ReadInt();
                shards.oGet(i).edgeHasNeighbour.SetNum(j);
                for (j = 0; j < shards.oGet(i).edgeHasNeighbour.Num(); j++) {
                    shards.oGet(i).edgeHasNeighbour.oSet(j, savefile.ReadBool());
                }

                shards.oGet(i).droppedTime = savefile.ReadInt();
                shards.oGet(i).islandNum = savefile.ReadInt();
                shards.oGet(i).atEdge = savefile.ReadBool();
                savefile.ReadStaticObject(shards.oGet(i).physicsObj);
                if (shards.oGet(i).droppedTime < 0) {
                    shards.oGet(i).clipModel = physicsObj.GetClipModel(i);
                } else {
                    shards.oGet(i).clipModel = shards.oGet(i).physicsObj.GetClipModel();
                }
            }
        }

        @Override
        public void Spawn() {
            float[] d = {0}, f = {0}, b = {0};

            // get shard properties
            decalMaterial = declManager.FindMaterial(spawnArgs.GetString("mtr_decal"));
            decalSize = spawnArgs.GetFloat("decalSize", "40");
            maxShardArea = spawnArgs.GetFloat("maxShardArea", "200");
            maxShardArea = idMath.ClampFloat(100, 10000, maxShardArea);
            maxShatterRadius = spawnArgs.GetFloat("maxShatterRadius", "40");
            minShatterRadius = spawnArgs.GetFloat("minShatterRadius", "10");
            linearVelocityScale = spawnArgs.GetFloat("linearVelocityScale", "0.1");
            angularVelocityScale = spawnArgs.GetFloat("angularVelocityScale", "40");
            fxFracture.oSet(spawnArgs.GetString("fx"));

            // get rigid body properties
            shardMass = spawnArgs.GetFloat("shardMass", "20");
            shardMass = idMath.ClampFloat(0.001f, 1000.0f, shardMass);
            spawnArgs.GetFloat("density", "0.1", d);
            density = idMath.ClampFloat(0.001f, 1000.0f, d[0]);
            spawnArgs.GetFloat("friction", "0.4", f);
            friction = idMath.ClampFloat(0.0f, 1.0f, f[0]);
            spawnArgs.GetFloat("bouncyness", "0.01", b);
            bouncyness = idMath.ClampFloat(0.0f, 1.0f, b[0]);

            disableFracture = spawnArgs.GetBool("disableFracture", "0");
            health = spawnArgs.GetInt("health", "40");
            fl.takedamage = true;

            // FIXME: set "bleed" so idProjectile calls AddDamageEffect
            spawnArgs.SetBool("bleed", true);

            CreateFractures(renderEntity.hModel);

            FindNeighbours();

            renderEntity.hModel = renderModelManager.AllocModel();
            renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);
            renderEntity.callback = idBrittleFracture.ModelCallback.getInstance();
            renderEntity.noShadow = true;
            renderEntity.noSelfShadow = true;
            renderEntity.noDynamicInteractions = false;
        }

        @Override
        public void Present() {

            // don't present to the renderer if the entity hasn't changed
            if (0 == (thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            renderEntity.bounds.oSet(bounds);
            renderEntity.origin.Zero();
            renderEntity.axis.Identity();

            // force an update because the bounds/origin/axis may stay the same while the model changes
            renderEntity.forceUpdate = 1;//true;

            // add to refresh list
            if (modelDefHandle == -1) {
                modelDefHandle = gameRenderWorld.AddEntityDef(renderEntity);
            } else {
                gameRenderWorld.UpdateEntityDef(modelDefHandle, renderEntity);
            }

            changed = true;
        }

        @Override
        public void Think() {
            int i, startTime, endTime, droppedTime;
            shard_s shard;
            boolean atRest = true, fading = false;

            // remove overdue shards
            for (i = 0; i < shards.Num(); i++) {
                droppedTime = shards.oGet(i).droppedTime;
                if (droppedTime != -1) {
                    if (gameLocal.time - droppedTime > SHARD_ALIVE_TIME) {
                        RemoveShard(i);
                        i--;
                    }
                    fading = true;
                }
            }

            // remove the entity when nothing is visible
            if (0 == shards.Num()) {
                PostEventMS(EV_Remove, 0);
                return;
            }

            if ((thinkFlags & TH_PHYSICS) != 0) {

                startTime = gameLocal.previousTime;
                endTime = gameLocal.time;

                // run physics on shards
                for (i = 0; i < shards.Num(); i++) {
                    shard = shards.oGet(i);

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

            if (!atRest || bounds.IsCleared()) {
                bounds.Clear();
                for (i = 0; i < shards.Num(); i++) {
                    bounds.AddBounds(shards.oGet(i).clipModel.GetAbsBounds());
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

            if (id < 0 || id >= shards.Num()) {
                return;
            }

            if (shards.oGet(id).droppedTime != -1) {
                shards.oGet(id).physicsObj.ApplyImpulse(0, point, impulse);
            } else if (health <= 0 && !disableFracture) {
                Shatter(point, impulse, gameLocal.time);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {

            if (id < 0 || id >= shards.Num()) {
                return;
            }

            if (shards.oGet(id).droppedTime != -1) {
                shards.oGet(id).physicsObj.AddForce(0, point, force);
            } else if (health <= 0 && !disableFracture) {
                Shatter(point, force, gameLocal.time);
            }
        }

        @Override
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            if (!disableFracture) {
                ProjectDecal(collision.c.point, collision.c.normal, gameLocal.time, damageDefName);
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (!disableFracture) {
                ActivateTargets(this);
                Break();
            }
        }

        public void ProjectDecal(final idVec3 point, final idVec3 dir, final int time, final String damageDefName) {
            int i, j, bits, clipBits;
            float a, c, s;
            idVec2[] st = new idVec2[MAX_POINTS_ON_WINDING];
            idVec3 origin;
            idMat3 axis = new idMat3(), axisTemp = new idMat3();
            idPlane[] textureAxis = new idPlane[2];

            if (gameLocal.isServer) {
                idBitMsg msg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

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

            textureAxis[0].oSet(axis.oGet(0).oMultiply(1.0f / decalSize));
            textureAxis[0].oSet(3, -(point.oMultiply(textureAxis[0].Normal())) + 0.5f);

            textureAxis[1].oSet(axis.oGet(1).oMultiply(1.0f / decalSize));
            textureAxis[1].oSet(3, -(point.oMultiply(textureAxis[1].Normal())) + 0.5f);

            for (i = 0; i < shards.Num(); i++) {
                idFixedWinding winding = shards.oGet(i).winding;
                origin = shards.oGet(i).clipModel.GetOrigin();
                axis = shards.oGet(i).clipModel.GetAxis();
                float d0, d1;

                clipBits = -1;
                for (j = 0; j < winding.GetNumPoints(); j++) {
                    idVec3 p = origin.oPlus(winding.oGet(j).ToVec3().oMultiply(axis));

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

                idFixedWinding decal = new idFixedWinding();
                shards.oGet(i).decals.Append(decal);

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
            return (fl.takedamage == false);
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
            idVec3 point = new idVec3(), dir = new idVec3();

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
            idPlane plane = new idPlane();
            idMat3 tangents;

            // this may be triggered by a model trace or other non-view related source,
            // to which we should look like an empty model
            if (null == renderView) {
                return false;
            }

            // don't regenerate it if it is current
            if (lastRenderEntityUpdate == gameLocal.time || !changed) {
                return false;
            }

            lastRenderEntityUpdate = gameLocal.time;
            changed = false;

            numTris = 0;
            numDecalTris = 0;
            for (i = 0; i < shards.Num(); i++) {
                n = shards.oGet(i).winding.GetNumPoints();
                if (n > 2) {
                    numTris += n - 2;
                }
                for (k = 0; k < shards.oGet(i).decals.Num(); k++) {
                    n = shards.oGet(i).decals.oGet(k).GetNumPoints();
                    if (n > 2) {
                        numDecalTris += n - 2;
                    }
                }
            }

            // FIXME: re-use model surfaces
            renderEntity.hModel.InitEmpty(brittleFracture_SnapshotName);

            // allocate triangle surfaces for the fractures and decals
            tris = renderEntity.hModel.AllocSurfaceTriangles(numTris * 3, material.ShouldCreateBackSides() ? numTris * 6 : numTris * 3);
            decalTris = renderEntity.hModel.AllocSurfaceTriangles(numDecalTris * 3, decalMaterial.ShouldCreateBackSides() ? numDecalTris * 6 : numDecalTris * 3);

            for (i = 0; i < shards.Num(); i++) {
                final idVec3 origin = shards.oGet(i).clipModel.GetOrigin();
                final idMat3 axis = shards.oGet(i).clipModel.GetAxis();

                fade = 1.0f;
                if (shards.oGet(i).droppedTime >= 0) {
                    msec = gameLocal.time - shards.oGet(i).droppedTime - SHARD_FADE_START;
                    if (msec > 0) {
                        fade = 1.0f - (float) msec / (SHARD_ALIVE_TIME - SHARD_FADE_START);
                    }
                }
                packedColor = (int) PackColor(new idVec4(renderEntity.shaderParms[SHADERPARM_RED] * fade,
                        renderEntity.shaderParms[ SHADERPARM_GREEN] * fade,
                        renderEntity.shaderParms[ SHADERPARM_BLUE] * fade,
                        fade));

                final idWinding winding = shards.oGet(i).winding;

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

                    tris.indexes[tris.numIndexes++] = tris.numVerts - 3;
                    tris.indexes[tris.numIndexes++] = tris.numVerts - 2;
                    tris.indexes[tris.numIndexes++] = tris.numVerts - 1;

                    if (material.ShouldCreateBackSides()) {

                        tris.indexes[tris.numIndexes++] = tris.numVerts - 2;
                        tris.indexes[tris.numIndexes++] = tris.numVerts - 3;
                        tris.indexes[tris.numIndexes++] = tris.numVerts - 1;
                    }
                }

                for (k = 0; k < shards.oGet(i).decals.Num(); k++) {
                    final idWinding decalWinding = shards.oGet(i).decals.oGet(k);

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

                        decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 3;
                        decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 2;
                        decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 1;

                        if (decalMaterial.ShouldCreateBackSides()) {

                            decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 2;
                            decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 3;
                            decalTris.indexes[decalTris.numIndexes++] = decalTris.numVerts - 1;
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
            surface.shader = material;
            surface.id = 0;
            surface.geometry = tris;
            renderEntity.hModel.AddSurface(surface);

//	memset( &surface, 0, sizeof( surface ) );
            surface = new modelSurface_s();
            surface.shader = decalMaterial;
            surface.id = 1;
            surface.geometry = decalTris;
            renderEntity.hModel.AddSurface(surface);

            return true;
        }

        public static class ModelCallback extends deferredEntityCallback_t {

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
                    gameLocal.Error("idBrittleFracture::ModelCallback: callback with NULL game entity");
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
        };

        private void AddShard(idClipModel clipModel, idFixedWinding w) {
            shard_s shard = new shard_s();
            shard.clipModel = clipModel;
            shard.droppedTime = -1;
            shard.winding = w;
            shard.decals.Clear();
            shard.edgeHasNeighbour.AssureSize(w.GetNumPoints(), false);
            shard.neighbours.Clear();
            shard.atEdge = false;
            shards.Append(shard);
        }

        private void RemoveShard(int index) {
            int i;

//	delete shards[index];
            shards.oSet(index, null);
            shards.RemoveIndex(index);
            physicsObj.RemoveIndex(index);

            for (i = index; i < shards.Num(); i++) {
                shards.oGet(i).clipModel.SetId(i);
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
            physicsObj.SetClipModel(null, 1.0f, clipModelId, false);

            origin = shard.clipModel.GetOrigin();
            axis = shard.clipModel.GetAxis();

            // set the dropped time for fading
            shard.droppedTime = time;

            dir2 = origin.oMinus(point);
            dist = dir2.Normalize();
            f = dist > maxShatterRadius ? 1.0f : idMath.Sqrt(dist - minShatterRadius) * (1.0f / idMath.Sqrt(maxShatterRadius - minShatterRadius));

            // setup the physics
            shard.physicsObj.SetSelf(this);
            shard.physicsObj.SetClipModel(shard.clipModel, density);
            shard.physicsObj.SetMass(shardMass);
            shard.physicsObj.SetOrigin(origin);
            shard.physicsObj.SetAxis(axis);
            shard.physicsObj.SetBouncyness(bouncyness);
            shard.physicsObj.SetFriction(0.6f, 0.6f, friction);
            shard.physicsObj.SetGravity(gameLocal.GetGravity());
            shard.physicsObj.SetContents(CONTENTS_RENDERMODEL);
            shard.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_MOVEABLECLIP);
            shard.physicsObj.ApplyImpulse(0, origin, dir.oMultiply(impulse * linearVelocityScale));
            shard.physicsObj.SetAngularVelocity(dir.Cross(dir2).oMultiply(f * angularVelocityScale));

            shard.clipModel.SetId(clipModelId);

            BecomeActive(TH_PHYSICS);
        }

        private void Shatter(final idVec3 point, final idVec3 impulse, final int time) {
            int i;
            idVec3 dir;
            shard_s shard;
            float m;

            if (gameLocal.isServer) {
                idBitMsg msg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

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

            if (fxFracture.Length() != 0) {
                idEntityFx.StartFx(fxFracture, point, GetPhysics().GetAxis(), this, true);
            }

            dir = impulse;
            m = dir.Normalize();

            for (i = 0; i < shards.Num(); i++) {
                shard = shards.oGet(i);

                if (shard.droppedTime != -1) {
                    continue;
                }

                if ((shard.clipModel.GetOrigin().oMinus(point)).LengthSqr() > Square(maxShatterRadius)) {
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
            queue = new shard_s[shards.Num()];
            for (i = 0; i < shards.Num(); i++) {
                shards.oGet(i).islandNum = 0;
            }

            for (i = 0; i < shards.Num(); i++) {

                if (shards.oGet(i).droppedTime != -1) {
                    continue;
                }

                if (shards.oGet(i).islandNum != 0) {
                    continue;
                }

                queueStart = 0;
                queueEnd = 1;
                queue[0] = shards.oGet(i);
                shards.oGet(i).islandNum = numIslands + 1;
                touchesEdge = false;

                if (shards.oGet(i).atEdge) {
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
            fl.takedamage = false;
            physicsObj.SetContents(CONTENTS_RENDERMODEL | CONTENTS_TRIGGER);
        }

        private void Fracture_r(idFixedWinding w) {
            int i, j, bestPlane;
            float a, c, s, dist, bestDist;
            idVec3 origin;
            idPlane windingPlane = new idPlane();
            idPlane[] splitPlanes = new idPlane[2];
            idMat3 axis = new idMat3(), axistemp = new idMat3();
            idFixedWinding back = new idFixedWinding();
            idTraceModel trm = new idTraceModel();
            idClipModel clipModel;

            while (true) {
                origin = w.GetCenter();
                w.GetPlane(windingPlane);

                if (w.GetArea() < maxShardArea) {
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

            physicsObj.SetClipModel(clipModel, 1.0f, shards.Num());
            physicsObj.SetOrigin(GetPhysics().GetOrigin().oPlus(origin), shards.Num());
            physicsObj.SetAxis(GetPhysics().GetAxis(), shards.Num());

            AddShard(clipModel, w);
        }

        private void CreateFractures(final idRenderModel renderModel) {
            int i, j, k;
            modelSurface_s surf;
            idDrawVert v;
            idFixedWinding w = new idFixedWinding();

            if (NOT(renderModel)) {
                return;
            }

            physicsObj.SetSelf(this);
            physicsObj.SetOrigin(GetPhysics().GetOrigin(), 0);
            physicsObj.SetAxis(GetPhysics().GetAxis(), 0);

            for (i = 0; i < 1 /*renderModel.NumSurfaces()*/; i++) {
                surf = renderModel.Surface(i);
                material = surf.shader;

                for (j = 0; j < surf.geometry.numIndexes; j += 3) {
                    w.Clear();
                    for (k = 0; k < 3; k++) {
                        v = surf.geometry.verts[ surf.geometry.indexes[ j + 2 - k]];
                        w.AddPoint(v.xyz);
                        w.oGet(k).s = v.st.oGet(0);
                        w.oGet(k).t = v.st.oGet(1);
                    }
                    Fracture_r(w);
                }
            }

            physicsObj.SetContents(material.GetContentFlags());
            SetPhysics(physicsObj);
        }

        private void FindNeighbours() {
            int i, j, k, l;
            idVec3 p1, p2, dir;
            idMat3 axis;
            idPlane[] plane = new idPlane[4];

            for (i = 0; i < shards.Num(); i++) {

                shard_s shard1 = shards.oGet(i);
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

                    for (j = 0; j < shards.Num(); j++) {

                        if (i == j) {
                            continue;
                        }

                        shard_s shard2 = shards.oGet(j);

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
                            if (plane[0].Side(p2, 0.1f) == SIDE_FRONT && plane[1].Side(p1, 0.1f) == SIDE_FRONT) {
                                if (plane[2].Side(p1, 0.1f) == SIDE_ON && plane[3].Side(p1, 0.1f) == SIDE_ON) {
                                    if (plane[2].Side(p2, 0.1f) == SIDE_ON && plane[3].Side(p2, 0.1f) == SIDE_ON) {
                                        shard1.neighbours.Append(shard2);
                                        shard1.edgeHasNeighbour.oSet(k, true);
                                        shard2.neighbours.Append(shard1);
                                        shard2.edgeHasNeighbour.oSet((l - 1 + w2.GetNumPoints()) % w2.GetNumPoints(), true);
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
            disableFracture = false;
            if (health <= 0) {
                Break();
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> _trace) {
            idEntity other = _other.value;
            trace_s trace = _trace.value;
            idVec3 point, impulse;

            if (!IsBroken()) {
                return;
            }

            if (trace.c.id < 0 || trace.c.id >= shards.Num()) {
                return;
            }

            point = shards.oGet(trace.c.id).clipModel.GetOrigin();
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

    };
}
