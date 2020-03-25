package neo.Game;

import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_SetAngularVelocity;
import static neo.Game.Entity.EV_SetLinearVelocity;
import static neo.Game.Entity.EV_SetOwner;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.BURNEXPIRED;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.BURNING;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.EXPLODING;
import static neo.Game.Moveable.idExplodingBarrel.explode_state_t.NORMAL;
import static neo.Game.Projectile.EV_Explode;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idDebris;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idMoveable );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idMoveable>) idMoveable::Event_Activate);
            eventCallbacks.put(EV_BecomeNonSolid, (eventCallback_t0<idMoveable>) idMoveable::Event_BecomeNonSolid);
            eventCallbacks.put(EV_SetOwnerFromSpawnArgs, (eventCallback_t0<idMoveable>) idMoveable::Event_SetOwnerFromSpawnArgs);
            eventCallbacks.put(EV_IsAtRest, (eventCallback_t0<idMoveable>) idMoveable::Event_IsAtRest);
            eventCallbacks.put(EV_EnableDamage, (eventCallback_t1<idMoveable>) idMoveable::Event_EnableDamage);
        }


        protected idPhysics_RigidBody    physicsObj;       // physics object
        protected idStr                  brokenModel;      // model set when health drops down to or below zero
        protected idStr                  damage;           // if > 0 apply damage to hit entities
        protected idStr                  fxCollide;        // fx system to start when collides with something
        protected int                    nextCollideFxTime;// next time it is ok to spawn collision fx
        protected float                  minDamageVelocity;// minimum velocity before moveable applies damage
        protected float                  maxDamageVelocity;// velocity at which the maximum damage is applied
        protected idCurve_Spline<idVec3> initialSpline;    // initial spline path the moveable follows
        protected idVec3                 initialSplineDir; // initial relative direction along the spline path
        protected boolean                explode;          // entity explodes when health drops down to or below zero
        protected boolean                unbindOnDeath;    // unbind from master when health drops down to or below zero
        protected boolean                allowStep;        // allow monsters to step on the object
        protected boolean                canDamage;        // only apply damage when this is set
        protected int                    nextDamageTime;   // next time the movable can hurt the player
        protected int                    nextSoundTime;    // next time the moveable can make a sound
        //
        //

        public idMoveable() {
            this.physicsObj = new idPhysics_RigidBody();
            this.brokenModel = new idStr();
            this.minDamageVelocity = 100.0f;
            this.maxDamageVelocity = 200.0f;
            this.nextCollideFxTime = 0;
            this.nextDamageTime = 0;
            this.nextSoundTime = 0;
            this.initialSpline = null;
            this.initialSplineDir = getVec3_zero();
            this.explode = false;
            this.unbindOnDeath = false;
            this.allowStep = false;
            this.canDamage = false;
        }
        // ~idMoveable( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            final idTraceModel trm = new idTraceModel();
            final float[] density = {0}, friction = {0}, bouncyness = {0}, mass = {0};
            int clipShrink;
            final idStr clipModelName = new idStr();

            // check if a clip model is set
            this.spawnArgs.GetString("clipmodel", "", clipModelName);
            if (!isNotNullOrEmpty(clipModelName)) {
                clipModelName.oSet(this.spawnArgs.GetString("model"));		// use the visual model
            }

            if (!CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm)) {
                gameLocal.Error("idMoveable '%s': cannot load collision model %s", this.name, clipModelName);
                return;
            }

            // if the model should be shrinked
            clipShrink = this.spawnArgs.GetInt("clipshrink");
            if (clipShrink != 0) {
                trm.Shrink(clipShrink * CM_CLIP_EPSILON);
            }

            // get rigid body properties
            this.spawnArgs.GetFloat("density", "0.5", density);
            density[0] = idMath.ClampFloat(0.001f, 1000.0f, density[0]);
            this.spawnArgs.GetFloat("friction", "0.05", friction);
            friction[0] = idMath.ClampFloat(0.0f, 1.0f, friction[0]);
            this.spawnArgs.GetFloat("bouncyness", "0.6", bouncyness);
            bouncyness[0] = idMath.ClampFloat(0.0f, 1.0f, bouncyness[0]);
            this.explode = this.spawnArgs.GetBool("explode");
            this.unbindOnDeath = this.spawnArgs.GetBool("unbindondeath");

            this.fxCollide = new idStr(this.spawnArgs.GetString("fx_collide"));
            this.nextCollideFxTime = 0;

            this.fl.takedamage = true;
            this.damage = new idStr(this.spawnArgs.GetString("def_damage", ""));
            this.canDamage = !this.spawnArgs.GetBool("damageWhenActive");
            this.minDamageVelocity = this.spawnArgs.GetFloat("minDamageVelocity", "100");
            this.maxDamageVelocity = this.spawnArgs.GetFloat("maxDamageVelocity", "200");
            this.nextDamageTime = 0;
            this.nextSoundTime = 0;

            this.health = this.spawnArgs.GetInt("health", "0");
            this.spawnArgs.GetString("broken", "", this.brokenModel);

            if (this.health != 0) {
                if (!this.brokenModel.IsEmpty() && NOT(renderModelManager.CheckModel(this.brokenModel.toString()))) {
                    gameLocal.Error("idMoveable '%s' at (%s): cannot load broken model '%s'", this.name, GetPhysics().GetOrigin().ToString(0), this.brokenModel);
                }
            }

            // setup the physics
            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(trm), density[0]);
            this.physicsObj.GetClipModel().SetMaterial(GetRenderModelMaterial());
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetBouncyness(bouncyness[0]);
            this.physicsObj.SetFriction(0.6f, 0.6f, friction[0]);
            this.physicsObj.SetGravity(gameLocal.GetGravity());
            this.physicsObj.SetContents(CONTENTS_SOLID);
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            SetPhysics(this.physicsObj);

            if (this.spawnArgs.GetFloat("mass", "10", mass)) {
                this.physicsObj.SetMass(mass[0]);
            }

            if (this.spawnArgs.GetBool("nodrop")) {
                this.physicsObj.PutToRest();
            } else {
                this.physicsObj.DropToFloor();
            }

            if (this.spawnArgs.GetBool("noimpact") || this.spawnArgs.GetBool("notPushable")) {
                this.physicsObj.DisableImpact();
            }

            if (this.spawnArgs.GetBool("nonsolid")) {
                BecomeNonSolid();
            }

            this.allowStep = this.spawnArgs.GetBool("allowStep", "1");

            PostEventMS(EV_SetOwnerFromSpawnArgs, 0);
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteString(this.brokenModel);
            savefile.WriteString(this.damage);
            savefile.WriteString(this.fxCollide);
            savefile.WriteInt(this.nextCollideFxTime);
            savefile.WriteFloat(this.minDamageVelocity);
            savefile.WriteFloat(this.maxDamageVelocity);
            savefile.WriteBool(this.explode);
            savefile.WriteBool(this.unbindOnDeath);
            savefile.WriteBool(this.allowStep);
            savefile.WriteBool(this.canDamage);
            savefile.WriteInt(this.nextDamageTime);
            savefile.WriteInt(this.nextSoundTime);
            savefile.WriteInt((int) (this.initialSpline != null ? this.initialSpline.GetTime(0) : -1));
            savefile.WriteVec3(this.initialSplineDir);

            savefile.WriteStaticObject(this.physicsObj);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] initialSplineTime = {0};

            savefile.ReadString(this.brokenModel);
            savefile.ReadString(this.damage);
            savefile.ReadString(this.fxCollide);
            this.nextCollideFxTime = savefile.ReadInt();
            this.minDamageVelocity = savefile.ReadFloat();
            this.maxDamageVelocity = savefile.ReadFloat();
            this.explode = savefile.ReadBool();
            this.unbindOnDeath = savefile.ReadBool();
            this.allowStep = savefile.ReadBool();
            this.canDamage = savefile.ReadBool();
            this.nextDamageTime = savefile.ReadInt();
            this.nextSoundTime = savefile.ReadInt();
            savefile.ReadInt(initialSplineTime);
            savefile.ReadVec3(this.initialSplineDir);

            if (initialSplineTime[0] != -1) {
                InitInitialSpline(initialSplineTime[0]);
            } else {
                this.initialSpline = null;
            }

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                if (!FollowInitialSplinePath()) {
                    BecomeInactive(TH_THINK);
                }
            }
            super.Think();
        }

        @Override
        public void Hide() {
            super.Hide();
            this.physicsObj.SetContents(0);
        }

        @Override
        public void Show() {
            super.Show();
            if (!this.spawnArgs.GetBool("nonsolid")) {
                this.physicsObj.SetContents(CONTENTS_SOLID);
            }
        }

        public boolean AllowStep() {
            return this.allowStep;
        }

        public void EnableDamage(boolean enable, float duration) {
            this.canDamage = enable;
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
            if ((v > BOUNCE_SOUND_MIN_VELOCITY) && (gameLocal.time > this.nextSoundTime)) {
                f = v > BOUNCE_SOUND_MAX_VELOCITY ? 1.0f : idMath.Sqrt(v - BOUNCE_SOUND_MIN_VELOCITY) * (1.0f / idMath.Sqrt(BOUNCE_SOUND_MAX_VELOCITY - BOUNCE_SOUND_MIN_VELOCITY));
                if (StartSound("snd_bounce", SND_CHANNEL_ANY, 0, false, null)) {
                    // don't set the volume unless there is a bounce sound as it overrides the entire channel
                    // which causes footsteps on ai's to not honor their shader parms
                    SetSoundVolume(f);
                }
                this.nextSoundTime = gameLocal.time + 500;
            }

            if (this.canDamage && (this.damage.Length() != 0) && (gameLocal.time > this.nextDamageTime)) {
                ent = gameLocal.entities[ collision.c.entityNum];
                if ((ent != null) && (v > this.minDamageVelocity)) {
                    f = v > this.maxDamageVelocity ? 1.0f : idMath.Sqrt(v - this.minDamageVelocity) * (1.0f / idMath.Sqrt(this.maxDamageVelocity - this.minDamageVelocity));
                    dir = velocity;
                    dir.NormalizeFast();
                    ent.Damage(this, GetPhysics().GetClipModel().GetOwner(), dir, this.damage.toString(), f, INVALID_JOINT);
                    this.nextDamageTime = gameLocal.time + 1000;
                }
            }

            if ((this.fxCollide.Length() != 0) && (gameLocal.time > this.nextCollideFxTime)) {
                idEntityFx.StartFx(this.fxCollide, collision.c.point, null, this, false);
                this.nextCollideFxTime = gameLocal.time + 3500;
            }

            return false;
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (this.unbindOnDeath) {
                Unbind();
            }

            if (!this.brokenModel.IsEmpty()) {
                SetModel(this.brokenModel.toString());
            }

            if (this.explode) {
                if (this.brokenModel.IsEmpty()) {
                    PostEventMS(EV_Remove, 1000);
                }
            }

            if (this.renderEntity.gui[ 0] != null) {
                this.renderEntity.gui[ 0] = null;
            }

            ActivateTargets(this);

            this.fl.takedamage = false;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            this.physicsObj.WriteToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.physicsObj.ReadFromSnapshot(msg);
            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        protected idMaterial GetRenderModelMaterial() {
            if (this.renderEntity.customShader != null) {
                return this.renderEntity.customShader;
            }
            if ((this.renderEntity.hModel != null) && (this.renderEntity.hModel.NumSurfaces() != 0)) {
                return this.renderEntity.hModel.Surface(0).shader;
            }
            return null;
        }

        protected void BecomeNonSolid() {
            // set CONTENTS_RENDERMODEL so bullets still collide with the moveable
            this.physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_RENDERMODEL);
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
        }

        protected void InitInitialSpline(int startTime) {
            int initialSplineTime;

            this.initialSpline = GetSpline();
            initialSplineTime = this.spawnArgs.GetInt("initialSplineTime", "300");

            if (this.initialSpline != null) {
                this.initialSpline.MakeUniform(initialSplineTime);
                this.initialSpline.ShiftTime(startTime - this.initialSpline.GetTime(0));
                this.initialSplineDir = this.initialSpline.GetCurrentFirstDerivative(startTime);
                this.initialSplineDir.oMulSet(this.physicsObj.GetAxis().Transpose());
                this.initialSplineDir.Normalize();
                BecomeActive(TH_THINK);
            }
        }

        protected boolean FollowInitialSplinePath() {
            if (this.initialSpline != null) {
                if (gameLocal.time < this.initialSpline.GetTime(this.initialSpline.GetNumValues() - 1)) {
                    final idVec3 splinePos = this.initialSpline.GetCurrentValue(gameLocal.time);
                    final idVec3 linearVelocity = (splinePos.oMinus(this.physicsObj.GetOrigin())).oMultiply(USERCMD_HZ);
                    this.physicsObj.SetLinearVelocity(linearVelocity);

                    final idVec3 splineDir = this.initialSpline.GetCurrentFirstDerivative(gameLocal.time);
                    final idVec3 dir = this.initialSplineDir.oMultiply(this.physicsObj.GetAxis());
                    final idVec3 angularVelocity = dir.Cross(splineDir);
                    angularVelocity.Normalize();
                    angularVelocity.oMulSet(idMath.ACos16(dir.oMultiply(splineDir) / splineDir.Length()) * USERCMD_HZ);//TODO:back reference from ACos16
                    this.physicsObj.SetAngularVelocity(angularVelocity);
                    return true;
                } else {
//			delete initialSpline;
                    this.initialSpline = null;
                }
            }
            return false;
        }

        protected void Event_Activate(idEventArg<idEntity> activator) {
            float delay;
            final idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            if (0 == this.spawnArgs.GetInt("notPushable")) {
                this.physicsObj.EnableImpact();
            }

            this.physicsObj.Activate();

            this.spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            this.spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = this.spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0.0f) {
                this.physicsObj.SetLinearVelocity(init_velocity);
            } else {
                PostEventSec(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = this.spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0.0f) {
                this.physicsObj.SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }

            InitInitialSpline(gameLocal.time);
        }

        protected void Event_BecomeNonSolid() {
            BecomeNonSolid();
        }

        protected void Event_SetOwnerFromSpawnArgs() {
            final String[] owner = {null};

            if (this.spawnArgs.GetString("owner", "", owner)) {
                ProcessEvent(EV_SetOwner, gameLocal.FindEntity(owner[0]));
            }
        }

        protected void Event_IsAtRest() {
            idThread.ReturnInt(this.physicsObj.IsAtRest());
        }

        protected void Event_EnableDamage(idEventArg<Float> enable) {
            this.canDamage = (enable.value != 0.0f);
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

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

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

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private float  radius;              // radius of barrel
        private int    barrelAxis;          // one of the coordinate axes the barrel cylinder is parallel to
        private idVec3 lastOrigin;          // origin of the barrel the last think frame
        private idMat3 lastAxis;            // axis of the barrel the last think frame
        private float  additionalRotation;  // additional rotation of the barrel about it's axis
        private idMat3 additionalAxis;      // additional rotation axis
        //
        //

        public idBarrel() {
            this.radius = 1.0f;
            this.barrelAxis = 0;
            this.lastOrigin = new idVec3();
            this.lastAxis = idMat3.getMat3_identity();
            this.additionalRotation = 0;
            this.additionalAxis = idMat3.getMat3_identity();
            this.fl.networkSync = true;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            final idBounds bounds = GetPhysics().GetBounds();

            // radius of the barrel cylinder
            this.radius = (bounds.oGet(1, 0) - bounds.oGet(0, 0)) * 0.5f;

            // always a vertical barrel with cylinder axis parallel to the z-axis
            this.barrelAxis = 2;

            this.lastOrigin = GetPhysics().GetOrigin();
            this.lastAxis = GetPhysics().GetAxis();

            this.additionalRotation = 0.0f;
            this.additionalAxis.Identity();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.radius);
            savefile.WriteInt(this.barrelAxis);
            savefile.WriteVec3(this.lastOrigin);
            savefile.WriteMat3(this.lastAxis);
            savefile.WriteFloat(this.additionalRotation);
            savefile.WriteMat3(this.additionalAxis);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.radius = savefile.ReadFloat();
            this.barrelAxis = savefile.ReadInt();
            savefile.ReadVec3(this.lastOrigin);
            savefile.ReadMat3(this.lastAxis);
            this.additionalRotation = savefile.ReadFloat();
            savefile.ReadMat3(this.additionalAxis);
        }

        public void BarrelThink() {
            boolean wasAtRest, onGround;
            float movedDistance, rotatedDistance, angle;
            idVec3 curOrigin, gravityNormal, dir;
            idMat3 curAxis;
			final idMat3 axis;

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

                    dir = curOrigin.oMinus(this.lastOrigin);
                    dir.oMinSet(gravityNormal.oMultiply(dir.oMultiply(gravityNormal)));
                    movedDistance = dir.LengthSqr();

                    // if the barrel moved and the barrel is not aligned with the gravity direction
                    if ((movedDistance > 0.0f) && (idMath.Fabs(gravityNormal.oMultiply(curAxis.oGet(this.barrelAxis))) < 0.7f)) {

                        // barrel movement since last think frame orthogonal to the barrel axis
                        movedDistance = idMath.Sqrt(movedDistance);
                        dir.oMulSet(1.0f / movedDistance);
                        movedDistance = (1.0f - idMath.Fabs(dir.oMultiply(curAxis.oGet(this.barrelAxis)))) * movedDistance;

                        // get rotation about barrel axis since last think frame
                        angle = this.lastAxis.oGet((this.barrelAxis + 1) % 3).oMultiply(curAxis.oGet((this.barrelAxis + 1) % 3));
                        angle = idMath.ACos(angle);
                        // distance along cylinder hull
                        rotatedDistance = angle * this.radius;

                        // if the barrel moved further than it rotated about it's axis
                        if (movedDistance > rotatedDistance) {

                            // additional rotation of the visual model to make it look
                            // like the barrel rolls instead of slides
                            angle = (180.0f * (movedDistance - rotatedDistance)) / (this.radius * idMath.PI);
                            if (gravityNormal.Cross(curAxis.oGet(this.barrelAxis)).oMultiply(dir) < 0.0f) {
                                this.additionalRotation += angle;
                            } else {
                                this.additionalRotation -= angle;
                            }
                            dir = getVec3_origin();
                            dir.oSet(this.barrelAxis, 1.0f);
                            this.additionalAxis = new idRotation(getVec3_origin(), dir, this.additionalRotation).ToMat3();
                        }
                    }
                }

                // save state for next think
                this.lastOrigin = curOrigin;
                this.lastAxis = curAxis;
            }

            Present();
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                if (!FollowInitialSplinePath()) {
                    BecomeInactive(TH_THINK);
                }
            }

            BarrelThink();
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            origin.oSet(getVec3_origin());
            axis.oSet(this.additionalAxis);
            return true;
        }

        @Override
        public void ClientPredictionThink() {
            Think();
        }

    }

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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idExplodingBarrel );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idBarrel.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idExplodingBarrel>) idExplodingBarrel::Event_Activate);
            eventCallbacks.put(EV_Respawn, (eventCallback_t0<idExplodingBarrel>) idExplodingBarrel::Event_Respawn);
            eventCallbacks.put(EV_Explode, (eventCallback_t0<idExplodingBarrel>) idExplodingBarrel::Event_Explode);
            eventCallbacks.put(EV_TriggerTargets, (eventCallback_t0<idExplodingBarrel>) idExplodingBarrel::Event_TriggerTargets);
        }

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
        }
        private explode_state_t  state;
        private idVec3           spawnOrigin;
        private idMat3           spawnAxis;
        private int/*qhandle_t*/ particleModelDefHandle;
        private int/*qhandle_t*/ lightDefHandle;
        private renderEntity_s   particleRenderEntity;
        private renderLight_s    light;
        private int              particleTime;
        private int              lightTime;
        private float            time;
        //
        //

        public idExplodingBarrel() {
            this.spawnOrigin = new idVec3();
            this.spawnAxis = new idMat3();
            this.state = NORMAL;
            this.particleModelDefHandle = -1;
            this.lightDefHandle = -1;
//	memset( &particleRenderEntity, 0, sizeof( particleRenderEntity ) );
            this.particleRenderEntity = new renderEntity_s();
//	memset( &light, 0, sizeof( light ) );
            this.light = new renderLight_s();
            this.particleTime = 0;
            this.lightTime = 0;
            this.time = 0.0f;
        }

        // ~idExplodingBarrel();
        @Override
        protected void _deconstructor() {
            if (this.particleModelDefHandle >= 0) {
                gameRenderWorld.FreeEntityDef(this.particleModelDefHandle);
            }
            if (this.lightDefHandle >= 0) {
                gameRenderWorld.FreeLightDef(this.lightDefHandle);
            }
            super._deconstructor();
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.health = this.spawnArgs.GetInt("health", "5");
            this.fl.takedamage = true;
            this.spawnOrigin = GetPhysics().GetOrigin();
            this.spawnAxis = GetPhysics().GetAxis();
            this.state = NORMAL;
            this.particleModelDefHandle = -1;
            this.lightDefHandle = -1;
            this.lightTime = 0;
            this.particleTime = 0;
            this.time = this.spawnArgs.GetFloat("time");
            this.particleRenderEntity = new renderEntity_s();//	memset( &particleRenderEntity, 0, sizeof( particleRenderEntity ) );
            this.light = new renderLight_s();//	memset( &light, 0, sizeof( light ) );
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.spawnOrigin);
            savefile.WriteMat3(this.spawnAxis);

            savefile.WriteInt(etoi(this.state));
            savefile.WriteInt(this.particleModelDefHandle);
            savefile.WriteInt(this.lightDefHandle);

            savefile.WriteRenderEntity(this.particleRenderEntity);
            savefile.WriteRenderLight(this.light);

            savefile.WriteInt(this.particleTime);
            savefile.WriteInt(this.lightTime);
            savefile.WriteFloat(this.time);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.spawnOrigin);
            savefile.ReadMat3(this.spawnAxis);

            this.state = explode_state_t.values()[savefile.ReadInt()];
            this.particleModelDefHandle = savefile.ReadInt();
            this.lightDefHandle = savefile.ReadInt();

            savefile.ReadRenderEntity(this.particleRenderEntity);
            savefile.ReadRenderLight(this.light);

            this.particleTime = savefile.ReadInt();
            this.lightTime = savefile.ReadInt();
            this.time = savefile.ReadFloat();
        }

        @Override
        public void Think() {
            super.BarrelThink();

            if (this.lightDefHandle >= 0) {
                if (this.state == BURNING) {
                    // ramp the color up over 250 ms
                    float pct = (gameLocal.time - this.lightTime) / 250.f;
                    if (pct > 1.0f) {
                        pct = 1.0f;
                    }
                    this.light.origin = this.physicsObj.GetAbsBounds().GetCenter();
                    this.light.axis = getMat3_identity();
                    this.light.shaderParms[ SHADERPARM_RED] = pct;
                    this.light.shaderParms[ SHADERPARM_GREEN] = pct;
                    this.light.shaderParms[ SHADERPARM_BLUE] = pct;
                    this.light.shaderParms[ SHADERPARM_ALPHA] = pct;
                    gameRenderWorld.UpdateLightDef(this.lightDefHandle, this.light);
                } else {
                    if ((gameLocal.time - this.lightTime) > 250) {
                        gameRenderWorld.FreeLightDef(this.lightDefHandle);
                        this.lightDefHandle = -1;
                    }
                    return;
                }
            }

            if (!gameLocal.isClient && (this.state != BURNING) && (this.state != EXPLODING)) {
                BecomeInactive(TH_THINK);
                return;
            }

            if (this.particleModelDefHandle >= 0) {
                this.particleRenderEntity.origin.oSet(this.physicsObj.GetAbsBounds().GetCenter());
                this.particleRenderEntity.axis.oSet(getMat3_identity());
                gameRenderWorld.UpdateEntityDef(this.particleModelDefHandle, this.particleRenderEntity);
            }
        }

        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir,
                final String damageDefName, final float damageScale, final int location) {

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'\n", damageDefName);
            }
            if ((damageDef.FindKey("radius") != null) && (GetPhysics().GetContents() != 0) && (GetBindMaster() == null)) {
                PostEventMS(EV_Explode, 400);
            } else {
                idEntity_Damage(inflictor, attacker, dir, damageDefName, damageScale, location);
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {

            if (IsHidden() || (this.state == EXPLODING) || (this.state == BURNING)) {
                return;
            }

            float f = this.spawnArgs.GetFloat("burn");
            if ((f > 0.0f) && (this.state == NORMAL)) {
                this.state = BURNING;
                PostEventSec(EV_Explode, f);
                StartSound("snd_burn", SND_CHANNEL_ANY, 0, false, null);
                AddParticles(this.spawnArgs.GetString("model_burn", ""), true);
                return;
            } else {
                this.state = EXPLODING;
                if (gameLocal.isServer) {
                    final idBitMsg msg = new idBitMsg();
                    final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                    msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                    msg.WriteLong(gameLocal.time);
                    ServerSendEvent(EVENT_EXPLODE, msg, false, -1);
                }
            }

            // do this before applying radius damage so the ent can trace to any damagable ents nearby
            Hide();
            this.physicsObj.SetContents(0);

            final String splash = this.spawnArgs.GetString("def_splash_damage", "damage_explosion");
            if ((splash != null) && !splash.isEmpty()) {
                gameLocal.RadiusDamage(GetPhysics().GetOrigin(), this, attacker, this, this, splash);
            }

            ExplodingEffects();

            //FIXME: need to precache all the debris stuff here and in the projectiles
            idKeyValue kv = this.spawnArgs.MatchPrefix("def_debris");
            // bool first = true;
            while (kv != null) {
                final idDict debris_args = gameLocal.FindEntityDefDict(kv.GetValue().toString(), false);
                if (debris_args != null) {
                    final idEntity[] ent = {null};
                    idVec3 dir2;
                    idDebris debris;
                    //if ( first ) {
                    dir2 = this.physicsObj.GetAxis().oGet(1);
                    //	first = false;
                    //} else {
                    dir2.x += gameLocal.random.CRandomFloat() * 4.0f;
                    dir2.y += gameLocal.random.CRandomFloat() * 4.0f;
                    //dir.z = gameLocal.random.RandomFloat() * 8.0f;
                    //}
                    dir2.Normalize();

                    gameLocal.SpawnEntityDef(debris_args, ent, false);
                    if ((null == ent[0]) || !ent[0].IsType(idDebris.class)) {
                        gameLocal.Error("'projectile_debris' is not an idDebris");
                    }

                    debris = (idDebris) ent[0];
                    debris.Create(this, this.physicsObj.GetOrigin(), dir2.ToMat3());
                    debris.Launch();
                    debris.GetRenderEntity().shaderParms[ SHADERPARM_TIME_OF_DEATH] = (gameLocal.time + 1500) * 0.001f;
                    debris.UpdateVisuals();

                }
                kv = this.spawnArgs.MatchPrefix("def_debris", kv);
            }

            this.physicsObj.PutToRest();
            CancelEvents(EV_Explode);
            CancelEvents(EV_Activate);

            f = this.spawnArgs.GetFloat("respawn");
            if (f > 0.0f) {
                PostEventSec(EV_Respawn, f);
            } else {
                PostEventMS(EV_Remove, 5000);
            }

            if (this.spawnArgs.GetBool("triggerTargets")) {
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
                    if ((gameLocal.realClientTime - msg.ReadLong()) < this.spawnArgs.GetInt("explode_lapse", "1000")) {
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
            if ((name != null) && !name.isEmpty()) {
                if (this.particleModelDefHandle >= 0) {
                    gameRenderWorld.FreeEntityDef(this.particleModelDefHandle);
                }
//		memset( &particleRenderEntity, 0, sizeof ( particleRenderEntity ) );
                this.particleRenderEntity = new renderEntity_s();//TODO:remove memset0 function from whatever fucking class got it!!!
                final idDeclModelDef modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, name);
                if (modelDef != null) {
                    this.particleRenderEntity.origin.oSet(this.physicsObj.GetAbsBounds().GetCenter());
                    this.particleRenderEntity.axis.oSet(getMat3_identity());
                    this.particleRenderEntity.hModel = modelDef.ModelHandle();
                    final float rgb = (burn) ? 0.0f : 1.0f;
                    this.particleRenderEntity.shaderParms[ SHADERPARM_RED] = rgb;
                    this.particleRenderEntity.shaderParms[ SHADERPARM_GREEN] = rgb;
                    this.particleRenderEntity.shaderParms[ SHADERPARM_BLUE] = rgb;
                    this.particleRenderEntity.shaderParms[ SHADERPARM_ALPHA] = rgb;
                    this.particleRenderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.realClientTime);
                    this.particleRenderEntity.shaderParms[ SHADERPARM_DIVERSITY] = (burn) ? 1.0f : gameLocal.random.RandomInt(90);
                    if (null == this.particleRenderEntity.hModel) {
                        this.particleRenderEntity.hModel = renderModelManager.FindModel(name);
                    }
                    this.particleModelDefHandle = gameRenderWorld.AddEntityDef(this.particleRenderEntity);
                    if (burn) {
                        BecomeActive(TH_THINK);
                    }
                    this.particleTime = gameLocal.realClientTime;
                }
            }
        }

        private void AddLight(final String name, boolean burn) {
            if (this.lightDefHandle >= 0) {
                gameRenderWorld.FreeLightDef(this.lightDefHandle);
            }
//	memset( &light, 0, sizeof ( light ) );
            this.light = new renderLight_s();
            this.light.axis = getMat3_identity();
            this.light.lightRadius.x = this.spawnArgs.GetFloat("light_radius");
            this.light.lightRadius.y = this.light.lightRadius.z = this.light.lightRadius.x;
            this.light.origin = this.physicsObj.GetOrigin();
            this.light.origin.z += 128;
            this.light.pointLight = true;
            this.light.shader = declManager.FindMaterial(name);
            this.light.shaderParms[ SHADERPARM_RED] = 2.0f;
            this.light.shaderParms[ SHADERPARM_GREEN] = 2.0f;
            this.light.shaderParms[ SHADERPARM_BLUE] = 2.0f;
            this.light.shaderParms[ SHADERPARM_ALPHA] = 2.0f;
            this.lightDefHandle = gameRenderWorld.AddLightDef(this.light);
            this.lightTime = gameLocal.realClientTime;
            BecomeActive(TH_THINK);
        }

        private void ExplodingEffects() {
            String temp;

            StartSound("snd_explode", SND_CHANNEL_ANY, 0, false, null);

            temp = this.spawnArgs.GetString("model_damage");
            if (!temp.isEmpty()) {// != '\0' ) {
                SetModel(temp);
                Show();
            }

            temp = this.spawnArgs.GetString("model_detonate");
            if (!temp.isEmpty()) {// != '\0' ) {
                AddParticles(temp, false);
            }

            temp = this.spawnArgs.GetString("mtr_lightexplode");
            if (!temp.isEmpty()) {// != '\0' ) {
                AddLight(temp, false);
            }

            temp = this.spawnArgs.GetString("mtr_burnmark");
            if (!temp.isEmpty()) {// != '\0' ) {
                gameLocal.ProjectDecal(GetPhysics().GetOrigin(), GetPhysics().GetGravity(), 128.0f, true, 96.0f, temp);
            }
        }

        @Override
        public void Event_Activate(idEventArg<idEntity> activator) {
            Killed(activator.value, activator.value, 0, getVec3_origin(), 0);
        }

        private void Event_Respawn() {
            int i;
            final int minRespawnDist = this.spawnArgs.GetInt("respawn_range", "256");
            if (minRespawnDist != 0) {
                float minDist = -1;
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (NOT(gameLocal.entities[i]) || !gameLocal.entities[i].IsType(idPlayer.class)) {
                        continue;
                    }
                    final idVec3 v = gameLocal.entities[i].GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                    final float dist = v.Length();
                    if ((minDist < 0) || (dist < minDist)) {
                        minDist = dist;
                    }
                }
                if (minDist < minRespawnDist) {
                    PostEventSec(EV_Respawn, this.spawnArgs.GetInt("respawn_again", "10"));
                    return;
                }
            }
            final String temp = this.spawnArgs.GetString("model");
            if ((temp != null) && !temp.isEmpty()) {
                SetModel(temp);
            }
            this.health = this.spawnArgs.GetInt("health", "5");
            this.fl.takedamage = true;
            this.physicsObj.SetOrigin(this.spawnOrigin);
            this.physicsObj.SetAxis(this.spawnAxis);
            this.physicsObj.SetContents(CONTENTS_SOLID);
            this.physicsObj.DropToFloor();
            this.state = NORMAL;
            Show();
            UpdateVisuals();
        }

        private void Event_Explode() {
            if ((this.state == NORMAL) || (this.state == BURNING)) {
                this.state = BURNEXPIRED;
                Killed(null, null, 0, getVec3_zero(), 0);
            }
        }

        private void Event_TriggerTargets() {
            ActivateTargets(this);
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }
}
