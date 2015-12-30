package neo.Game;

import java.nio.ByteBuffer;
import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_SetAngularVelocity;
import static neo.Game.Entity.EV_SetLinearVelocity;
import static neo.Game.Entity.EV_SetOwner;
import static neo.Game.Entity.TH_THINK;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.GameSys.Class;
import static neo.Game.GameSys.Class.EV_Remove;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.BURNEXPIRED;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.BURNING;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.EXPLODING;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.NORMAL;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Player.idPlayer;
import static neo.Game.Projectile.EV_Explode;
import neo.Game.Projectile.idDebris;
import neo.Game.Script.Script_Thread.idThread;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_SOLID;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Curve.idCurve_Spline;
import static neo.idlib.math.Math_h.MS2SEC;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Moveable {

    /*
     ===============================================================================

     Entity using rigid body physics.

     ===============================================================================
     */
    /*
     ===============================================================================

     idMoveable
	
     ===============================================================================
     */
    public static final idEventDef EV_BecomeNonSolid = new idEventDef("becomeNonSolid");
    public static final idEventDef EV_SetOwnerFromSpawnArgs = new idEventDef("<setOwnerFromSpawnArgs>");
    public static final idEventDef EV_IsAtRest = new idEventDef("isAtRest", null, 'd');
    public static final idEventDef EV_EnableDamage = new idEventDef("enableDamage", "f");
//    
    public static final float BOUNCE_SOUND_MIN_VELOCITY = 80.0f;
    public static final float BOUNCE_SOUND_MAX_VELOCITY = 200.0f;
//

    public static class idMoveable extends idEntity {
        // CLASS_PROTOTYPE( idMoveable );

        protected idPhysics_RigidBody physicsObj;	// physics object
        protected idStr brokenModel;			// model set when health drops down to or below zero
        protected idStr damage;				// if > 0 apply damage to hit entities
        protected idStr fxCollide;			// fx system to start when collides with something
        protected int nextCollideFxTime;		// next time it is ok to spawn collision fx
        protected float minDamageVelocity;		// minimum velocity before moveable applies damage
        protected float maxDamageVelocity;		// velocity at which the maximum damage is applied
        protected idCurve_Spline<idVec3> initialSpline;	// initial spline path the moveable follows
        protected idVec3 initialSplineDir;		// initial relative direction along the spline path
        protected boolean explode;			// entity explodes when health drops down to or below zero
        protected boolean unbindOnDeath;		// unbind from master when health drops down to or below zero
        protected boolean allowStep;			// allow monsters to step on the object
        protected boolean canDamage;			// only apply damage when this is set
        protected int nextDamageTime;			// next time the movable can hurt the player
        protected int nextSoundTime;			// next time the moveable can make a sound
        //
        //

        public idMoveable() {
            physicsObj = new idPhysics_RigidBody();
            brokenModel = new idStr();
            minDamageVelocity = 100.0f;
            maxDamageVelocity = 200.0f;
            nextCollideFxTime = 0;
            nextDamageTime = 0;
            nextSoundTime = 0;
            initialSpline = null;
            initialSplineDir = getVec3_zero();
            explode = false;
            unbindOnDeath = false;
            allowStep = false;
            canDamage = false;
        }
        // ~idMoveable( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            idTraceModel trm = new idTraceModel();
            float[] density = {0}, friction = {0}, bouncyness = {0}, mass = {0};
            int clipShrink;
            idStr clipModelName = new idStr();

            // check if a clip model is set
            spawnArgs.GetString("clipmodel", "", clipModelName);
            if (!isNotNullOrEmpty(clipModelName)) {
                clipModelName.oSet(spawnArgs.GetString("model"));		// use the visual model
            }

            if (!CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm)) {
                gameLocal.Error("idMoveable '%s': cannot load collision model %s", name, clipModelName);
                return;
            }

            // if the model should be shrinked
            clipShrink = spawnArgs.GetInt("clipshrink");
            if (clipShrink != 0) {
                trm.Shrink(clipShrink * CM_CLIP_EPSILON);
            }

            // get rigid body properties
            spawnArgs.GetFloat("density", "0.5", density);
            density[0] = idMath.ClampFloat(0.001f, 1000.0f, density[0]);
            spawnArgs.GetFloat("friction", "0.05", friction);
            friction[0] = idMath.ClampFloat(0.0f, 1.0f, friction[0]);
            spawnArgs.GetFloat("bouncyness", "0.6", bouncyness);
            bouncyness[0] = idMath.ClampFloat(0.0f, 1.0f, bouncyness[0]);
            explode = spawnArgs.GetBool("explode");
            unbindOnDeath = spawnArgs.GetBool("unbindondeath");

            fxCollide = new idStr(spawnArgs.GetString("fx_collide"));
            nextCollideFxTime = 0;

            fl.takedamage = true;
            damage = new idStr(spawnArgs.GetString("def_damage", ""));
            canDamage = !spawnArgs.GetBool("damageWhenActive");
            minDamageVelocity = spawnArgs.GetFloat("minDamageVelocity", "100");
            maxDamageVelocity = spawnArgs.GetFloat("maxDamageVelocity", "200");
            nextDamageTime = 0;
            nextSoundTime = 0;

            health = spawnArgs.GetInt("health", "0");
            spawnArgs.GetString("broken", "", brokenModel);

            if (health != 0) {
                if (!brokenModel.IsEmpty() && NOT(renderModelManager.CheckModel(brokenModel.toString()))) {
                    gameLocal.Error("idMoveable '%s' at (%s): cannot load broken model '%s'", name, GetPhysics().GetOrigin().ToString(0), brokenModel);
                }
            }

            // setup the physics
            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(trm), density[0]);
            physicsObj.GetClipModel().SetMaterial(GetRenderModelMaterial());
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetBouncyness(bouncyness[0]);
            physicsObj.SetFriction(0.6f, 0.6f, friction[0]);
            physicsObj.SetGravity(gameLocal.GetGravity());
            physicsObj.SetContents(CONTENTS_SOLID);
            physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            SetPhysics(physicsObj);

            if (spawnArgs.GetFloat("mass", "10", mass)) {
                physicsObj.SetMass(mass[0]);
            }

            if (spawnArgs.GetBool("nodrop")) {
                physicsObj.PutToRest();
            } else {
                physicsObj.DropToFloor();
            }

            if (spawnArgs.GetBool("noimpact") || spawnArgs.GetBool("notPushable")) {
                physicsObj.DisableImpact();
            }

            if (spawnArgs.GetBool("nonsolid")) {
                BecomeNonSolid();
            }

            allowStep = spawnArgs.GetBool("allowStep", "1");

            PostEventMS(EV_SetOwnerFromSpawnArgs, 0);
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteString(brokenModel);
            savefile.WriteString(damage);
            savefile.WriteString(fxCollide);
            savefile.WriteInt(nextCollideFxTime);
            savefile.WriteFloat(minDamageVelocity);
            savefile.WriteFloat(maxDamageVelocity);
            savefile.WriteBool(explode);
            savefile.WriteBool(unbindOnDeath);
            savefile.WriteBool(allowStep);
            savefile.WriteBool(canDamage);
            savefile.WriteInt(nextDamageTime);
            savefile.WriteInt(nextSoundTime);
            savefile.WriteInt((int) (initialSpline != null ? initialSpline.GetTime(0) : -1));
            savefile.WriteVec3(initialSplineDir);

            savefile.WriteStaticObject(physicsObj);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int[] initialSplineTime = {0};

            savefile.ReadString(brokenModel);
            savefile.ReadString(damage);
            savefile.ReadString(fxCollide);
            nextCollideFxTime = savefile.ReadInt();
            minDamageVelocity = savefile.ReadFloat();
            maxDamageVelocity = savefile.ReadFloat();
            explode = savefile.ReadBool();
            unbindOnDeath = savefile.ReadBool();
            allowStep = savefile.ReadBool();
            canDamage = savefile.ReadBool();
            nextDamageTime = savefile.ReadInt();
            nextSoundTime = savefile.ReadInt();
            savefile.ReadInt(initialSplineTime);
            savefile.ReadVec3(initialSplineDir);

            if (initialSplineTime[0] != -1) {
                InitInitialSpline(initialSplineTime[0]);
            } else {
                initialSpline = null;
            }

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);
        }

        @Override
        public void Think() {
            if ((thinkFlags & TH_THINK) != 0) {
                if (!FollowInitialSplinePath()) {
                    BecomeInactive(TH_THINK);
                }
            }
            super.Think();
        }

        @Override
        public void Hide() {
            super.Hide();
            physicsObj.SetContents(0);
        }

        @Override
        public void Show() {
            super.Show();
            if (!spawnArgs.GetBool("nonsolid")) {
                physicsObj.SetContents(CONTENTS_SOLID);
            }
        }

        public boolean AllowStep() {
            return allowStep;
        }

        public void EnableDamage(boolean enable, float duration) {
            canDamage = enable;
            if (duration != 0) {
                PostEventSec(EV_EnableDamage, duration, (!enable) ? 0.0f : 1.0f);
            }
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            float v, f;
            idVec3 dir;
            idEntity ent;

            v = -(velocity.oMultiply(collision.c.normal));
            if (v > BOUNCE_SOUND_MIN_VELOCITY && gameLocal.time > nextSoundTime) {
                f = v > BOUNCE_SOUND_MAX_VELOCITY ? 1.0f : idMath.Sqrt(v - BOUNCE_SOUND_MIN_VELOCITY) * (1.0f / idMath.Sqrt(BOUNCE_SOUND_MAX_VELOCITY - BOUNCE_SOUND_MIN_VELOCITY));
                if (StartSound("snd_bounce", SND_CHANNEL_ANY, 0, false, null)) {
                    // don't set the volume unless there is a bounce sound as it overrides the entire channel
                    // which causes footsteps on ai's to not honor their shader parms
                    SetSoundVolume(f);
                }
                nextSoundTime = gameLocal.time + 500;
            }

            if (canDamage && damage.Length() != 0 && gameLocal.time > nextDamageTime) {
                ent = gameLocal.entities[ collision.c.entityNum];
                if (ent != null && v > minDamageVelocity) {
                    f = v > maxDamageVelocity ? 1.0f : idMath.Sqrt(v - minDamageVelocity) * (1.0f / idMath.Sqrt(maxDamageVelocity - minDamageVelocity));
                    dir = velocity;
                    dir.NormalizeFast();
                    ent.Damage(this, GetPhysics().GetClipModel().GetOwner(), dir, damage.toString(), f, INVALID_JOINT);
                    nextDamageTime = gameLocal.time + 1000;
                }
            }

            if (fxCollide.Length() != 0 && gameLocal.time > nextCollideFxTime) {
                idEntityFx.StartFx(fxCollide, collision.c.point, null, this, false);
                nextCollideFxTime = gameLocal.time + 3500;
            }

            return false;
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (unbindOnDeath) {
                Unbind();
            }

            if (!brokenModel.IsEmpty()) {
                SetModel(brokenModel.toString());
            }

            if (explode) {
                if (brokenModel.IsEmpty()) {
                    PostEventMS(EV_Remove, 1000);
                }
            }

            if (renderEntity.gui[ 0] != null) {
                renderEntity.gui[ 0] = null;
            }

            ActivateTargets(this);

            fl.takedamage = false;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            physicsObj.WriteToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            physicsObj.ReadFromSnapshot(msg);
            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        protected idMaterial GetRenderModelMaterial() {
            if (renderEntity.customShader != null) {
                return renderEntity.customShader;
            }
            if (renderEntity.hModel != null && renderEntity.hModel.NumSurfaces() != 0) {
                return renderEntity.hModel.Surface(0).shader;
            }
            return null;
        }

        protected void BecomeNonSolid() {
            // set CONTENTS_RENDERMODEL so bullets still collide with the moveable
            physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_RENDERMODEL);
            physicsObj.SetClipMask(MASK_SOLID | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
        }

        protected void InitInitialSpline(int startTime) {
            int initialSplineTime;

            initialSpline = GetSpline();
            initialSplineTime = spawnArgs.GetInt("initialSplineTime", "300");

            if (initialSpline != null) {
                initialSpline.MakeUniform(initialSplineTime);
                initialSpline.ShiftTime(startTime - initialSpline.GetTime(0));
                initialSplineDir = initialSpline.GetCurrentFirstDerivative(startTime);
                initialSplineDir.oMulSet(physicsObj.GetAxis().Transpose());
                initialSplineDir.Normalize();
                BecomeActive(TH_THINK);
            }
        }

        protected boolean FollowInitialSplinePath() {
            if (initialSpline != null) {
                if (gameLocal.time < initialSpline.GetTime(initialSpline.GetNumValues() - 1)) {
                    idVec3 splinePos = initialSpline.GetCurrentValue(gameLocal.time);
                    idVec3 linearVelocity = (splinePos.oMinus(physicsObj.GetOrigin())).oMultiply(USERCMD_HZ);
                    physicsObj.SetLinearVelocity(linearVelocity);

                    idVec3 splineDir = initialSpline.GetCurrentFirstDerivative(gameLocal.time);
                    idVec3 dir = initialSplineDir.oMultiply(physicsObj.GetAxis());
                    idVec3 angularVelocity = dir.Cross(splineDir);
                    angularVelocity.Normalize();
                    angularVelocity.oMulSet(idMath.ACos16(dir.oMultiply(splineDir) / splineDir.Length()) * USERCMD_HZ);//TODO:back reference from ACos16
                    physicsObj.SetAngularVelocity(angularVelocity);
                    return true;
                } else {
//			delete initialSpline;
                    initialSpline = null;
                }
            }
            return false;
        }

        protected void Event_Activate(idEntity activator) {
            float delay;
            idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            if (0 == spawnArgs.GetInt("notPushable")) {
                physicsObj.EnableImpact();
            }

            physicsObj.Activate();

            spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0.0f) {
                physicsObj.SetLinearVelocity(init_velocity);
            } else {
                PostEventSec(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0.0f) {
                physicsObj.SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }

            InitInitialSpline(gameLocal.time);
        }

        protected void Event_BecomeNonSolid() {
            BecomeNonSolid();
        }

        protected void Event_SetOwnerFromSpawnArgs() {
            String[] owner = {null};

            if (spawnArgs.GetString("owner", "", owner)) {
                ProcessEvent(EV_SetOwner, gameLocal.FindEntity(owner[0]));
            }
        }

        protected void Event_IsAtRest() {
            idThread.ReturnInt(physicsObj.IsAtRest());
        }

        protected void Event_EnableDamage(float enable) {
            canDamage = (enable != 0.0f);
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         *
         */
        public void idEntity_Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            super.Damage(inflictor, attacker, dir, damageDefName, damageScale, location);
        }
    };

    /*
     ===============================================================================

     A barrel using rigid body physics. The barrel has special handling of
     the view model orientation to make it look like it rolls instead of slides.

     ===============================================================================
     */
    /*
     ===============================================================================

     idBarrel
	
     ===============================================================================
     */
    public static class idBarrel extends idMoveable {
        // CLASS_PROTOTYPE( idBarrel );

        private float radius;				// radius of barrel
        private int barrelAxis;				// one of the coordinate axes the barrel cylinder is parallel to
        private idVec3 lastOrigin;			// origin of the barrel the last think frame
        private idMat3 lastAxis;			// axis of the barrel the last think frame
        private float additionalRotation;		// additional rotation of the barrel about it's axis
        private idMat3 additionalAxis;			// additional rotation axis
        //
        //

        public idBarrel() {
            radius = 1.0f;
            barrelAxis = 0;
            lastOrigin.Zero();
            lastAxis.Identity();
            additionalRotation = 0;
            additionalAxis.Identity();
            fl.networkSync = true;
        }

        @Override
        public void Spawn() {
            final idBounds bounds = GetPhysics().GetBounds();

            // radius of the barrel cylinder
            radius = (bounds.oGet(1, 0) - bounds.oGet(0, 0)) * 0.5f;

            // always a vertical barrel with cylinder axis parallel to the z-axis
            barrelAxis = 2;

            lastOrigin = GetPhysics().GetOrigin();
            lastAxis = GetPhysics().GetAxis();

            additionalRotation = 0.0f;
            additionalAxis.Identity();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(radius);
            savefile.WriteInt(barrelAxis);
            savefile.WriteVec3(lastOrigin);
            savefile.WriteMat3(lastAxis);
            savefile.WriteFloat(additionalRotation);
            savefile.WriteMat3(additionalAxis);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            radius = savefile.ReadFloat();
            barrelAxis = savefile.ReadInt();
            savefile.ReadVec3(lastOrigin);
            savefile.ReadMat3(lastAxis);
            additionalRotation = savefile.ReadFloat();
            savefile.ReadMat3(additionalAxis);
        }

        public void BarrelThink() {
            boolean wasAtRest, onGround;
            float movedDistance, rotatedDistance, angle;
            idVec3 curOrigin, gravityNormal, dir;
            idMat3 curAxis, axis;

            wasAtRest = IsAtRest();

            // run physics
            RunPhysics();

            // only need to give the visual model an additional rotation if the physics were run
            if (!wasAtRest) {

                // current physics state
                onGround = GetPhysics().HasGroundContacts();
                curOrigin = GetPhysics().GetOrigin();
                curAxis = GetPhysics().GetAxis();

                // if the barrel is on the ground
                if (onGround) {
                    gravityNormal = GetPhysics().GetGravityNormal();

                    dir = curOrigin.oMinus(lastOrigin);
                    dir.oMinSet(gravityNormal.oMultiply(dir.oMultiply(gravityNormal)));
                    movedDistance = dir.LengthSqr();

                    // if the barrel moved and the barrel is not aligned with the gravity direction
                    if (movedDistance > 0.0f && idMath.Fabs(gravityNormal.oMultiply(curAxis.oGet(barrelAxis))) < 0.7f) {

                        // barrel movement since last think frame orthogonal to the barrel axis
                        movedDistance = idMath.Sqrt(movedDistance);
                        dir.oMulSet(1.0f / movedDistance);
                        movedDistance = (1.0f - idMath.Fabs(dir.oMultiply(curAxis.oGet(barrelAxis)))) * movedDistance;

                        // get rotation about barrel axis since last think frame
                        angle = lastAxis.oGet((barrelAxis + 1) % 3).oMultiply(curAxis.oGet((barrelAxis + 1) % 3));
                        angle = idMath.ACos(angle);
                        // distance along cylinder hull
                        rotatedDistance = angle * radius;

                        // if the barrel moved further than it rotated about it's axis
                        if (movedDistance > rotatedDistance) {

                            // additional rotation of the visual model to make it look
                            // like the barrel rolls instead of slides
                            angle = 180.0f * (movedDistance - rotatedDistance) / (radius * idMath.PI);
                            if (gravityNormal.Cross(curAxis.oGet(barrelAxis)).oMultiply(dir) < 0.0f) {
                                additionalRotation += angle;
                            } else {
                                additionalRotation -= angle;
                            }
                            dir = getVec3_origin();
                            dir.oSet(barrelAxis, 1.0f);
                            additionalAxis = new idRotation(getVec3_origin(), dir, additionalRotation).ToMat3();
                        }
                    }
                }

                // save state for next think
                lastOrigin = curOrigin;
                lastAxis = curAxis;
            }

            Present();
        }

        @Override
        public void Think() {
            if ((thinkFlags & TH_THINK) != 0) {
                if (!FollowInitialSplinePath()) {
                    BecomeInactive(TH_THINK);
                }
            }

            BarrelThink();
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            origin.oSet(getVec3_origin());
            axis.oSet(additionalAxis);
            return true;
        }

        @Override
        public void ClientPredictionThink() {
            Think();
        }

    };

    /*
     ===============================================================================

     A barrel using rigid body physics and special handling of the view model
     orientation to make it look like it rolls instead of slides. The barrel
     can burn and explode when damaged.

     ===============================================================================
     */
    public static final idEventDef EV_Respawn = new idEventDef("<respawn>");
    public static final idEventDef EV_TriggerTargets = new idEventDef("<triggertargets>");

    /*
     ===============================================================================

     idExplodingBarrel

     ===============================================================================
     */
    public static class idExplodingBarrel extends idBarrel {
        // CLASS_PROTOTYPE( idExplodingBarrel );

        // enum {
        public static final int EVENT_EXPLODE = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_MAXEVENTS = EVENT_EXPLODE + 1;
        // };
//
//

        protected enum explode_state_t {

            NORMAL,//= 0,
            BURNING,
            BURNEXPIRED,
            EXPLODING
        };
        private explode_state_t state;
        private idVec3 spawnOrigin;
        private idMat3 spawnAxis;
        private int/*qhandle_t*/ particleModelDefHandle;
        private int/*qhandle_t*/ lightDefHandle;
        private renderEntity_s particleRenderEntity;
        private renderLight_s light;
        private int particleTime;
        private int lightTime;
        private float time;
        //
        //

        public idExplodingBarrel() {
            spawnOrigin.Zero();
            spawnAxis.Zero();
            state = NORMAL;
            particleModelDefHandle = -1;
            lightDefHandle = -1;
//	memset( &particleRenderEntity, 0, sizeof( particleRenderEntity ) );
            particleRenderEntity = new renderEntity_s();
//	memset( &light, 0, sizeof( light ) );
            light = new renderLight_s();
            particleTime = 0;
            lightTime = 0;
            time = 0.0f;
        }
        // ~idExplodingBarrel();

        @Override
        public void Spawn() {
            health = spawnArgs.GetInt("health", "5");
            fl.takedamage = true;
            spawnOrigin = GetPhysics().GetOrigin();
            spawnAxis = GetPhysics().GetAxis();
            state = NORMAL;
            particleModelDefHandle = -1;
            lightDefHandle = -1;
            lightTime = 0;
            particleTime = 0;
            time = spawnArgs.GetFloat("time");
            //	memset( &particleRenderEntity, 0, sizeof( particleRenderEntity ) );
            particleRenderEntity = new renderEntity_s();
//	memset( &light, 0, sizeof( light ) );
            light = new renderLight_s();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(spawnOrigin);
            savefile.WriteMat3(spawnAxis);

            savefile.WriteInt(etoi(state));
            savefile.WriteInt(particleModelDefHandle);
            savefile.WriteInt(lightDefHandle);

            savefile.WriteRenderEntity(particleRenderEntity);
            savefile.WriteRenderLight(light);

            savefile.WriteInt(particleTime);
            savefile.WriteInt(lightTime);
            savefile.WriteFloat(time);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(spawnOrigin);
            savefile.ReadMat3(spawnAxis);

            state = explode_state_t.values()[savefile.ReadInt()];
            particleModelDefHandle = savefile.ReadInt();
            lightDefHandle = savefile.ReadInt();

            savefile.ReadRenderEntity(particleRenderEntity);
            savefile.ReadRenderLight(light);

            particleTime = savefile.ReadInt();
            lightTime = savefile.ReadInt();
            time = savefile.ReadFloat();
        }

        @Override
        public void Think() {
            super.BarrelThink();

            if (lightDefHandle >= 0) {
                if (state == BURNING) {
                    // ramp the color up over 250 ms
                    float pct = (gameLocal.time - lightTime) / 250.f;
                    if (pct > 1.0f) {
                        pct = 1.0f;
                    }
                    light.origin = physicsObj.GetAbsBounds().GetCenter();
                    light.axis = getMat3_identity();
                    light.shaderParms[ SHADERPARM_RED] = pct;
                    light.shaderParms[ SHADERPARM_GREEN] = pct;
                    light.shaderParms[ SHADERPARM_BLUE] = pct;
                    light.shaderParms[ SHADERPARM_ALPHA] = pct;
                    gameRenderWorld.UpdateLightDef(lightDefHandle, light);
                } else {
                    if (gameLocal.time - lightTime > 250) {
                        gameRenderWorld.FreeLightDef(lightDefHandle);
                        lightDefHandle = -1;
                    }
                    return;
                }
            }

            if (!gameLocal.isClient && state != BURNING && state != EXPLODING) {
                BecomeInactive(TH_THINK);
                return;
            }

            if (particleModelDefHandle >= 0) {
                particleRenderEntity.origin = physicsObj.GetAbsBounds().GetCenter();
                particleRenderEntity.axis = getMat3_identity();
                gameRenderWorld.UpdateEntityDef(particleModelDefHandle, particleRenderEntity);
            }
        }

        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir,
                final String damageDefName, final float damageScale, final int location) {

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'\n", damageDefName);
            }
            if (damageDef.FindKey("radius") != null && GetPhysics().GetContents() != 0 && GetBindMaster() == null) {
                PostEventMS(EV_Explode, 400);
            } else {
                idEntity_Damage(inflictor, attacker, dir, damageDefName, damageScale, location);
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {

            if (IsHidden() || state == EXPLODING || state == BURNING) {
                return;
            }

            float f = spawnArgs.GetFloat("burn");
            if (f > 0.0f && state == NORMAL) {
                state = BURNING;
                PostEventSec(EV_Explode, f);
                StartSound("snd_burn", SND_CHANNEL_ANY, 0, false, null);
                AddParticles(spawnArgs.GetString("model_burn", ""), true);
                return;
            } else {
                state = EXPLODING;
                if (gameLocal.isServer) {
                    idBitMsg msg = new idBitMsg();
                    ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                    msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                    msg.WriteLong(gameLocal.time);
                    ServerSendEvent(EVENT_EXPLODE, msg, false, -1);
                }
            }

            // do this before applying radius damage so the ent can trace to any damagable ents nearby
            Hide();
            physicsObj.SetContents(0);

            final String splash = spawnArgs.GetString("def_splash_damage", "damage_explosion");
            if (splash != null && !splash.isEmpty()) {
                gameLocal.RadiusDamage(GetPhysics().GetOrigin(), this, attacker, this, this, splash);
            }

            ExplodingEffects();

            //FIXME: need to precache all the debris stuff here and in the projectiles
            idKeyValue kv = spawnArgs.MatchPrefix("def_debris");
            // bool first = true;
            while (kv != null) {
                final idDict debris_args = gameLocal.FindEntityDefDict(kv.GetValue().toString(), false);
                if (debris_args != null) {
                    idEntity[] ent = {null};
                    idVec3 dir2;
                    idDebris debris;
                    //if ( first ) {
                    dir2 = physicsObj.GetAxis().oGet(1);
                    //	first = false;
                    //} else {
                    dir2.x += gameLocal.random.CRandomFloat() * 4.0f;
                    dir2.y += gameLocal.random.CRandomFloat() * 4.0f;
                    //dir.z = gameLocal.random.RandomFloat() * 8.0f;
                    //}
                    dir2.Normalize();

                    gameLocal.SpawnEntityDef(debris_args, ent, false);
                    if (null == ent[0] || !ent[0].IsType(idDebris.class)) {
                        gameLocal.Error("'projectile_debris' is not an idDebris");
                    }

                    debris = (idDebris) ent[0];
                    debris.Create(this, physicsObj.GetOrigin(), dir2.ToMat3());
                    debris.Launch();
                    debris.GetRenderEntity().shaderParms[ SHADERPARM_TIME_OF_DEATH] = (gameLocal.time + 1500) * 0.001f;
                    debris.UpdateVisuals();

                }
                kv = spawnArgs.MatchPrefix("def_debris", kv);
            }

            physicsObj.PutToRest();
            CancelEvents(EV_Explode);
            CancelEvents(EV_Activate);

            f = spawnArgs.GetFloat("respawn");
            if (f > 0.0f) {
                PostEventSec(EV_Respawn, f);
            } else {
                PostEventMS(EV_Remove, 5000);
            }

            if (spawnArgs.GetBool("triggerTargets")) {
                ActivateTargets(this);
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            super.WriteToSnapshot(msg);
            msg.WriteBits(btoi(IsHidden()), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {

            super.ReadFromSnapshot(msg);
            if (msg.ReadBits(1) != 0) {
                Hide();
            } else {
                Show();
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {

            switch (event) {
                case EVENT_EXPLODE: {
                    if (gameLocal.realClientTime - msg.ReadLong() < spawnArgs.GetInt("explode_lapse", "1000")) {
                        ExplodingEffects();
                    }
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        private void AddParticles(final String name, boolean burn) {
            if (name != null && !name.isEmpty()) {
                if (particleModelDefHandle >= 0) {
                    gameRenderWorld.FreeEntityDef(particleModelDefHandle);
                }
//		memset( &particleRenderEntity, 0, sizeof ( particleRenderEntity ) );
                particleRenderEntity = new renderEntity_s();//TODO:remove memset0 function from whatever fucking class got it!!!
                final idDeclModelDef modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, name);
                if (modelDef != null) {
                    particleRenderEntity.origin = physicsObj.GetAbsBounds().GetCenter();
                    particleRenderEntity.axis = getMat3_identity();
                    particleRenderEntity.hModel = modelDef.ModelHandle();
                    float rgb = (burn) ? 0.0f : 1.0f;
                    particleRenderEntity.shaderParms[ SHADERPARM_RED] = rgb;
                    particleRenderEntity.shaderParms[ SHADERPARM_GREEN] = rgb;
                    particleRenderEntity.shaderParms[ SHADERPARM_BLUE] = rgb;
                    particleRenderEntity.shaderParms[ SHADERPARM_ALPHA] = rgb;
                    particleRenderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.realClientTime);
                    particleRenderEntity.shaderParms[ SHADERPARM_DIVERSITY] = (burn) ? 1.0f : gameLocal.random.RandomInt(90);
                    if (null == particleRenderEntity.hModel) {
                        particleRenderEntity.hModel = renderModelManager.FindModel(name);
                    }
                    particleModelDefHandle = gameRenderWorld.AddEntityDef(particleRenderEntity);
                    if (burn) {
                        BecomeActive(TH_THINK);
                    }
                    particleTime = gameLocal.realClientTime;
                }
            }
        }

        private void AddLight(final String name, boolean burn) {
            if (lightDefHandle >= 0) {
                gameRenderWorld.FreeLightDef(lightDefHandle);
            }
//	memset( &light, 0, sizeof ( light ) );
            light = new renderLight_s();
            light.axis = getMat3_identity();
            light.lightRadius.x = spawnArgs.GetFloat("light_radius");
            light.lightRadius.y = light.lightRadius.z = light.lightRadius.x;
            light.origin = physicsObj.GetOrigin();
            light.origin.z += 128;
            light.pointLight = true;
            light.shader = declManager.FindMaterial(name);
            light.shaderParms[ SHADERPARM_RED] = 2.0f;
            light.shaderParms[ SHADERPARM_GREEN] = 2.0f;
            light.shaderParms[ SHADERPARM_BLUE] = 2.0f;
            light.shaderParms[ SHADERPARM_ALPHA] = 2.0f;
            lightDefHandle = gameRenderWorld.AddLightDef(light);
            lightTime = gameLocal.realClientTime;
            BecomeActive(TH_THINK);
        }

        private void ExplodingEffects() {
            String temp;

            StartSound("snd_explode", SND_CHANNEL_ANY, 0, false, null);

            temp = spawnArgs.GetString("model_damage");
            if (!temp.isEmpty()) {// != '\0' ) {
                SetModel(temp);
                Show();
            }

            temp = spawnArgs.GetString("model_detonate");
            if (!temp.isEmpty()) {// != '\0' ) {
                AddParticles(temp, false);
            }

            temp = spawnArgs.GetString("mtr_lightexplode");
            if (!temp.isEmpty()) {// != '\0' ) {
                AddLight(temp, false);
            }

            temp = spawnArgs.GetString("mtr_burnmark");
            if (!temp.isEmpty()) {// != '\0' ) {
                gameLocal.ProjectDecal(GetPhysics().GetOrigin(), GetPhysics().GetGravity(), 128.0f, true, 96.0f, temp);
            }
        }

        @Override
        public void Event_Activate(idEntity activator) {
            Killed(activator, activator, 0, getVec3_origin(), 0);
        }

        private void Event_Respawn() {
            int i;
            int minRespawnDist = spawnArgs.GetInt("respawn_range", "256");
            if (minRespawnDist != 0) {
                float minDist = -1;
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (NOT(gameLocal.entities[i]) || !gameLocal.entities[i].IsType(idPlayer.class)) {
                        continue;
                    }
                    idVec3 v = gameLocal.entities[i].GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                    float dist = v.Length();
                    if (minDist < 0 || dist < minDist) {
                        minDist = dist;
                    }
                }
                if (minDist < minRespawnDist) {
                    PostEventSec(EV_Respawn, spawnArgs.GetInt("respawn_again", "10"));
                    return;
                }
            }
            final String temp = spawnArgs.GetString("model");
            if (temp != null && !temp.isEmpty()) {
                SetModel(temp);
            }
            health = spawnArgs.GetInt("health", "5");
            fl.takedamage = true;
            physicsObj.SetOrigin(spawnOrigin);
            physicsObj.SetAxis(spawnAxis);
            physicsObj.SetContents(CONTENTS_SOLID);
            physicsObj.DropToFloor();
            state = NORMAL;
            Show();
            UpdateVisuals();
        }

        private void Event_Explode() {
            if (state == NORMAL || state == BURNING) {
                state = BURNEXPIRED;
                Killed(null, null, 0, getVec3_zero(), 0);
            }
        }

        private void Event_TriggerTargets() {
            ActivateTargets(this);
        }
    };
}
