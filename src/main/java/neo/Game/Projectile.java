package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Game.TEST_PARTICLE_IMPACT;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_projectileLights;
import static neo.Game.GameSys.SysCvar.g_testParticle;
import static neo.Game.GameSys.SysCvar.g_testParticleName;
import static neo.Game.Game_local.MASK_SHOT_RENDERMODEL;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ITEM;
import static neo.Game.Physics.Clip.CLIPMODEL_ID_TO_JOINT_HANDLE;
import static neo.Game.Physics.Physics_RigidBody.RB_VELOCITY_EXPONENT_BITS;
import static neo.Game.Physics.Physics_RigidBody.RB_VELOCITY_MANTISSA_BITS;
import static neo.Game.Player.PROJECTILE_DAMAGE;
import static neo.Game.Projectile.idProjectile.projectileState_t.CREATED;
import static neo.Game.Projectile.idProjectile.projectileState_t.EXPLODED;
import static neo.Game.Projectile.idProjectile.projectileState_t.FIZZLED;
import static neo.Game.Projectile.idProjectile.projectileState_t.LAUNCHED;
import static neo.Game.Projectile.idProjectile.projectileState_t.SPAWNED;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_PROJECTILE;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Material.SURF_NOIMPACT;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_METAL;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_NONE;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_RICOCHET;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_STONE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_END_X;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_END_Y;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_END_Z;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_WIDTH;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import static neo.idlib.Lib.LittleBitField;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.TempDump.SERiAL;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Mover.idDoor;
import neo.Game.Player.idPlayer;
import neo.Game.AI.AI.idAI;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force_Constant.idForce_Constant;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.surfTypes_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclParticle.idDeclParticle;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Projectile {

    /*
     ===============================================================================

     idProjectile
	
     ===============================================================================
     */
    public static final int        BFG_DAMAGE_FREQUENCY      = 333;
    public static final float      BOUNCE_SOUND_MIN_VELOCITY = 200.0f;
    public static final float      BOUNCE_SOUND_MAX_VELOCITY = 400.0f;
    //
    public static final idEventDef EV_Explode                = new idEventDef("<explode>", null);
    public static final idEventDef EV_Fizzle                 = new idEventDef("<fizzle>", null);
    public static final idEventDef EV_RadiusDamage           = new idEventDef("<radiusdmg>", "e");
    public static final idEventDef EV_GetProjectileState     = new idEventDef("getProjectileState", null, 'd');
    //
    public static final idEventDef EV_RemoveBeams            = new idEventDef("<removeBeams>", null);

    public static class idProjectile extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Explode, (eventCallback_t0<idProjectile>) idProjectile::Event_Explode);
            eventCallbacks.put(EV_Fizzle, (eventCallback_t0<idProjectile>) idProjectile::Event_Fizzle);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idProjectile>) idProjectile::Event_Touch);
            eventCallbacks.put(EV_RadiusDamage, (eventCallback_t1<idProjectile>) idProjectile::Event_RadiusDamage);
            eventCallbacks.put(EV_GetProjectileState, (eventCallback_t0<idProjectile>) idProjectile::Event_GetProjectileState);
        }


        protected idEntityPtr<idEntity> owner;
//

        public static class projectileFlags_s implements SERiAL {

            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public boolean detonate_on_world;//: 1;
            public boolean detonate_on_actor;//: 1;
            boolean randomShaderSpin;//: 1;
            public boolean isTracer;//: 1;
            public boolean noSplashDamage;//: 1;

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
        //
        protected projectileFlags_s   projectileFlags;
        //
        protected float               thrust;
        protected int                 thrust_end;
        protected float               damagePower;
        //
        protected renderLight_s       renderLight;
        protected int/*qhandle_t*/    lightDefHandle;                // handle to renderer light def
        protected idVec3              lightOffset;
        protected int                 lightStartTime;
        protected int                 lightEndTime;
        protected idVec3              lightColor;
        //
        protected idForce_Constant    thruster;
        protected idPhysics_RigidBody physicsObj;
        //
        protected idDeclParticle      smokeFly;
        protected int                 smokeFlyTime;
//

        protected enum projectileState_t {
            // must update these in script/doom_defs.script if changed

            SPAWNED,//= 0,
            CREATED,//= 1,
            LAUNCHED,//= 2,
            FIZZLED,//= 3,
            EXPLODED,//= 4
        }
        //
        protected projectileState_t state;
        //
        private   boolean           netSyncPhysics;
//
//

        // public :
        // CLASS_PROTOTYPE( idProjectile );
        public idProjectile() {
            this.owner = new idEntityPtr<>();
            this.lightDefHandle = -1;
            this.thrust = 0.0f;
            this.thrust_end = 0;
            this.smokeFly = null;
            this.smokeFlyTime = 0;
            this.state = SPAWNED;
            this.lightOffset = getVec3_zero();
            this.lightStartTime = 0;
            this.lightEndTime = 0;
            this.lightColor = getVec3_zero();
            this.state = SPAWNED;
            this.damagePower = 1.0f;
            this.projectileFlags = new projectileFlags_s();//memset( &projectileFlags, 0, sizeof( projectileFlags ) );
            this.renderLight = new renderLight_s();//memset( &renderLight, 0, sizeof( renderLight ) );
            
            // note: for net_instanthit projectiles, we will force this back to false at spawn time
            this.fl.networkSync = true;

            this.netSyncPhysics = false;
            
            this.physicsObj = new idPhysics_RigidBody();
            this.thruster = new idForce_Constant();
        }

        @Override
        protected void _deconstructor() {
            StopSound(SND_CHANNEL_ANY.ordinal(), false);
            FreeLightDef();

            super._deconstructor();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetContents(0);
            this.physicsObj.SetClipMask(0);
            this.physicsObj.PutToRest();
            SetPhysics(this.physicsObj);
        }

        @Override
        public void Save(idSaveGame savefile) {

            this.owner.Save(savefile);

            final projectileFlags_s flags = this.projectileFlags;
            LittleBitField(flags);
            savefile.Write(flags);

            savefile.WriteFloat(this.thrust);
            savefile.WriteInt(this.thrust_end);

            savefile.WriteRenderLight(this.renderLight);
            savefile.WriteInt(this.lightDefHandle);
            savefile.WriteVec3(this.lightOffset);
            savefile.WriteInt(this.lightStartTime);
            savefile.WriteInt(this.lightEndTime);
            savefile.WriteVec3(this.lightColor);

            savefile.WriteParticle(this.smokeFly);
            savefile.WriteInt(this.smokeFlyTime);

            savefile.WriteInt(etoi(this.state));

            savefile.WriteFloat(this.damagePower);

            savefile.WriteStaticObject(this.physicsObj);
            savefile.WriteStaticObject(this.thruster);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            this.owner.Restore(savefile);

            savefile.Read(this.projectileFlags);
            LittleBitField(this.projectileFlags);

            this.thrust = savefile.ReadFloat();
            this.thrust_end = savefile.ReadInt();

            savefile.ReadRenderLight(this.renderLight);
            this.lightDefHandle = savefile.ReadInt();
            savefile.ReadVec3(this.lightOffset);
            this.lightStartTime = savefile.ReadInt();
            this.lightEndTime = savefile.ReadInt();
            savefile.ReadVec3(this.lightColor);

            savefile.ReadParticle(this.smokeFly);
            this.smokeFlyTime = savefile.ReadInt();

            this.state = projectileState_t.values()[savefile.ReadInt()];

            this.damagePower = savefile.ReadFloat();

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            savefile.ReadStaticObject(this.thruster);
            this.thruster.SetPhysics(this.physicsObj);

            if (this.smokeFly != null) {
                idVec3 dir;
                dir = this.physicsObj.GetLinearVelocity();
                dir.NormalizeFast();
                gameLocal.smokeParticles.EmitSmoke(this.smokeFly, gameLocal.time, gameLocal.random.RandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }
        }

        public void Create(idEntity owner, final idVec3 start, final idVec3 dir) {
//            idDict args;
            String shaderName;
            final idVec3 light_color = new idVec3();
//            idVec3 light_offset;
            idVec3 tmp;
            idMat3 axis;

            Unbind();

            // align z-axis of model with the direction
            axis = dir.ToMat3();
            tmp = axis.oGet(2);
            axis.oSet(2, axis.oGet(0));
            axis.oSet(0, tmp.oNegative());

            this.physicsObj.SetOrigin(start);
            this.physicsObj.SetAxis(axis);

            this.physicsObj.GetClipModel().SetOwner(owner);

            this.owner.oSet(owner);

//	memset( &renderLight, 0, sizeof( renderLight ) );
            this.renderLight = new renderLight_s();
            shaderName = this.spawnArgs.GetString("mtr_light_shader");
            if (!shaderName.isEmpty()) {
                this.renderLight.shader = declManager.FindMaterial(shaderName, false);
                this.renderLight.pointLight = true;
                this.renderLight.lightRadius.oSet(0,
                        this.renderLight.lightRadius.oSet(1,
                                this.renderLight.lightRadius.oSet(2, this.spawnArgs.GetFloat("light_radius"))));
                this.spawnArgs.GetVector("light_color", "1 1 1", light_color);
                this.renderLight.shaderParms[0] = light_color.oGet(0);
                this.renderLight.shaderParms[1] = light_color.oGet(1);
                this.renderLight.shaderParms[2] = light_color.oGet(2);
                this.renderLight.shaderParms[3] = 1.0f;
            }

            this.spawnArgs.GetVector("light_offset", "0 0 0", this.lightOffset);

            this.lightStartTime = 0;
            this.lightEndTime = 0;
            this.smokeFlyTime = 0;

            this.damagePower = 1.0f;

            UpdateVisuals();

            this.state = CREATED;

            if (this.spawnArgs.GetBool("net_fullphysics")) {
                this.netSyncPhysics = true;
            }
        }

        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float launchPower /*= 1.0f*/, final float dmgPower /*= 1.0f*/) {
            float fuse;
            float startthrust;
            float endthrust;
            final idVec3 velocity = new idVec3();
            final idAngles angular_velocity = new idAngles();
            float linear_friction;
            float angular_friction;
            float contact_friction;
            float bounce;
            float mass;
            float speed;
            float gravity;
            idVec3 gravVec;
            idVec3 tmp;
            idMat3 axis;
            int thrust_start;
            int contents;
            int clipMask;

            // allow characters to throw projectiles during cinematics, but not the player
            if ((this.owner.GetEntity() != null) && !this.owner.GetEntity().IsType(idPlayer.class)) {
                this.cinematic = this.owner.GetEntity().cinematic;
            } else {
                this.cinematic = false;
            }

            this.thrust = this.spawnArgs.GetFloat("thrust");
            startthrust = this.spawnArgs.GetFloat("thrust_start");
            endthrust = this.spawnArgs.GetFloat("thrust_end");

            this.spawnArgs.GetVector("velocity", "0 0 0", velocity);

            speed = velocity.Length() * launchPower;

            this.damagePower = dmgPower;

            this.spawnArgs.GetAngles("angular_velocity", "0 0 0", angular_velocity);

            linear_friction = this.spawnArgs.GetFloat("linear_friction");
            angular_friction = this.spawnArgs.GetFloat("angular_friction");
            contact_friction = this.spawnArgs.GetFloat("contact_friction");
            bounce = this.spawnArgs.GetFloat("bounce");
            mass = this.spawnArgs.GetFloat("mass");
            gravity = this.spawnArgs.GetFloat("gravity");
            fuse = this.spawnArgs.GetFloat("fuse");

            this.projectileFlags.detonate_on_world = this.spawnArgs.GetBool("detonate_on_world");
            this.projectileFlags.detonate_on_actor = this.spawnArgs.GetBool("detonate_on_actor");
            this.projectileFlags.randomShaderSpin = this.spawnArgs.GetBool("random_shader_spin");

            if (mass <= 0) {
                gameLocal.Error("Invalid mass on '%s'\n", GetEntityDefName());
            }

            this.thrust *= mass;
            thrust_start = (int) (SEC2MS(startthrust) + gameLocal.time);
            this.thrust_end = (int) (SEC2MS(endthrust) + gameLocal.time);

            this.lightStartTime = 0;
            this.lightEndTime = 0;

            if (this.health != 0) {
                this.fl.takedamage = true;
            }

            gravVec = gameLocal.GetGravity();
            gravVec.NormalizeFast();

            Unbind();

            // align z-axis of model with the direction
            axis = dir.ToMat3();
            tmp = axis.oGet(2);
            axis.oSet(2, axis.oGet(0));
            axis.oSet(0, tmp.oNegative());

            contents = 0;
            clipMask = MASK_SHOT_RENDERMODEL;
            if (this.spawnArgs.GetBool("detonate_on_trigger")) {
                contents |= CONTENTS_TRIGGER;
            }
            if (!this.spawnArgs.GetBool("no_contents")) {
                contents |= CONTENTS_PROJECTILE;
                clipMask |= CONTENTS_PROJECTILE;
            }

            // don't do tracers on client, we don't know origin and direction
            if (this.spawnArgs.GetBool("tracers") && (gameLocal.random.RandomFloat() > 0.5f)) {
                SetModel(this.spawnArgs.GetString("model_tracer"));
                this.projectileFlags.isTracer = true;
            }

            this.physicsObj.SetMass(mass);
            this.physicsObj.SetFriction(linear_friction, angular_friction, contact_friction);
            if (contact_friction == 0.0f) {
                this.physicsObj.NoContact();
            }
            this.physicsObj.SetBouncyness(bounce);
            this.physicsObj.SetGravity(gravVec.oMultiply(gravity));
            this.physicsObj.SetContents(contents);
            this.physicsObj.SetClipMask(clipMask);
            this.physicsObj.SetLinearVelocity(pushVelocity.oPlus(axis.oGet(2).oMultiply(speed)));
            this.physicsObj.SetAngularVelocity(angular_velocity.ToAngularVelocity().oMultiply(axis));
            this.physicsObj.SetOrigin(start);
            this.physicsObj.SetAxis(axis);

            this.thruster.SetPosition(this.physicsObj, 0, new idVec3(GetPhysics().GetBounds().oGet(0).x, 0, 0));

            if (!gameLocal.isClient) {
                if (fuse <= 0) {
                    // run physics for 1 second
                    RunPhysics();
                    PostEventMS(EV_Remove, this.spawnArgs.GetInt("remove_time", "1500"));
                } else if (this.spawnArgs.GetBool("detonate_on_fuse")) {
                    fuse -= timeSinceFire;
                    if (fuse < 0.0f) {
                        fuse = 0.0f;
                    }
                    PostEventSec(EV_Explode, fuse);
                } else {
                    fuse -= timeSinceFire;
                    if (fuse < 0.0f) {
                        fuse = 0.0f;
                    }
                    PostEventSec(EV_Fizzle, fuse);
                }
            }

            if (this.projectileFlags.isTracer) {
                StartSound("snd_tracer", SND_CHANNEL_BODY, 0, false, null);
            } else {
                StartSound("snd_fly", SND_CHANNEL_BODY, 0, false, null);
            }

            this.smokeFlyTime = 0;
            final String smokeName = this.spawnArgs.GetString("smoke_fly");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                this.smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                this.smokeFlyTime = gameLocal.time;
            }

            // used for the plasma bolts but may have other uses as well
            if (this.projectileFlags.randomShaderSpin) {
                float f = gameLocal.random.RandomFloat();
                f *= 0.5f;
                this.renderEntity.shaderParms[SHADERPARM_DIVERSITY] = f;
            }

            UpdateVisuals();

            this.state = LAUNCHED;
        }

        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float launchPower /*= 1.0f*/) {
            Launch(start, dir, pushVelocity, timeSinceFire, launchPower, 0.0f);
        }

        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/) {
            Launch(start, dir, pushVelocity, timeSinceFire, 0.0f);
        }

        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity) {
            Launch(start, dir, pushVelocity, 0.0f);
        }

        @Override
        public void FreeLightDef() {
            if (this.lightDefHandle != -1) {
                gameRenderWorld.FreeLightDef(this.lightDefHandle);
                this.lightDefHandle = -1;
            }
        }

        public idEntity GetOwner() {
            return this.owner.GetEntity();
        }

        @Override
        public void Think() {

            if ((this.thinkFlags & TH_THINK) != 0) {
                if ((this.thrust != 0) && (gameLocal.time < this.thrust_end)) {
                    // evaluate force
                    this.thruster.SetForce(GetPhysics().GetAxis().oGet(0).oMultiply(this.thrust));
                    this.thruster.Evaluate(gameLocal.time);
                }
            }

            // run physics
            RunPhysics();

            Present();

            // add the particles
            if ((this.smokeFly != null) && (this.smokeFlyTime != 0) && !IsHidden()) {
                final idVec3 dir = GetPhysics().GetLinearVelocity().oNegative();
                dir.Normalize();
                if (!gameLocal.smokeParticles.EmitSmoke(this.smokeFly, this.smokeFlyTime, gameLocal.random.RandomFloat(), GetPhysics().GetOrigin(), dir.ToMat3())) {
                    this.smokeFlyTime = gameLocal.time;
                }
            }

            // add the light
            if ((this.renderLight.lightRadius.x > 0.0f) && g_projectileLights.GetBool()) {
                this.renderLight.origin.oSet(GetPhysics().GetOrigin().oPlus(GetPhysics().GetAxis().oMultiply(this.lightOffset)));
                this.renderLight.axis.oSet(GetPhysics().GetAxis());
                if ((this.lightDefHandle != -1)) {
                    if ((this.lightEndTime > 0) && (gameLocal.time <= (this.lightEndTime + gameLocal.GetMSec()))) {
                        final idVec3 color = new idVec3(0, 0, 0);//TODO:superfluous
                        if (gameLocal.time < this.lightEndTime) {
                            final float frac = (float) (gameLocal.time - this.lightStartTime) / (float) (this.lightEndTime - this.lightStartTime);
                            color.Lerp(this.lightColor, color, frac);
                        }
                        this.renderLight.shaderParms[SHADERPARM_RED] = color.x;
                        this.renderLight.shaderParms[SHADERPARM_GREEN] = color.y;
                        this.renderLight.shaderParms[SHADERPARM_BLUE] = color.z;
                    }
                    gameRenderWorld.UpdateLightDef(this.lightDefHandle, this.renderLight);
                } else {
                    this.lightDefHandle = gameRenderWorld.AddLightDef(this.renderLight);
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (this.spawnArgs.GetBool("detonate_on_death")) {
                trace_s collision;

//		memset( &collision, 0, sizeof( collision ) );
                collision = new trace_s();
                collision.endAxis.oSet(GetPhysics().GetAxis());
                collision.endpos.oSet(GetPhysics().GetOrigin());
                collision.c.point.oSet(GetPhysics().GetOrigin());
                collision.c.normal.Set(0, 0, 1);
                Explode(collision, null);
                this.physicsObj.ClearContacts();
                this.physicsObj.PutToRest();
            } else {
                Fizzle();
            }
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            idEntity ent;
            idEntity ignore;
            String damageDefName;
            idVec3 dir;
            final float[] push = {0};
            float damageScale;

            if ((this.state == EXPLODED) || (this.state == FIZZLED)) {
                return true;
            }

            // predict the explosion
            if (gameLocal.isClient) {
                if (ClientPredictionCollide(this, this.spawnArgs, collision, velocity, !this.spawnArgs.GetBool("net_instanthit"))) {
                    Explode(collision, null);
                    return true;
                }
                return false;
            }

            // remove projectile when a 'noimpact' surface is hit
            if ((collision.c.material != null) && ((collision.c.material.GetSurfaceFlags() & SURF_NOIMPACT) != 0)) {
                PostEventMS(EV_Remove, 0);
                common.DPrintf("Projectile collision no impact\n");
                return true;
            }

            // get the entity the projectile collided with
            ent = gameLocal.entities[ collision.c.entityNum];
            if (ent.equals(this.owner.GetEntity())) {
                assert (false);
                return true;
            }

            // just get rid of the projectile when it hits a player in noclip
            if (ent.IsType(idPlayer.class) && ((idPlayer) ent).noclip) {
                PostEventMS(EV_Remove, 0);
                return true;
            }

            // direction of projectile
            dir = velocity;
            dir.Normalize();

            // projectiles can apply an additional impulse next to the rigid body physics impulse
            if (this.spawnArgs.GetFloat("push", "0", push) && (push[0] > 0)) {
                ent.ApplyImpulse(this, collision.c.id, collision.c.point, dir.oMultiply(push[0]));
            }

            // MP: projectiles open doors
            if (gameLocal.isMultiplayer && ent.IsType(idDoor.class) && !((idDoor) ent).IsOpen() && !ent.spawnArgs.GetBool("no_touch")) {
                ent.ProcessEvent(EV_Activate, this);
            }

            if (ent.IsType(idActor.class) || (ent.IsType(idAFAttachment.class) && ((idAFAttachment) ent).GetBody().IsType(idActor.class))) {
                if (!this.projectileFlags.detonate_on_actor) {
                    return false;
                }
            } else {
                if (!this.projectileFlags.detonate_on_world) {
                    if (!StartSound("snd_ricochet", SND_CHANNEL_ITEM, 0, true, null)) {
                        final float len = velocity.Length();
                        if (len > BOUNCE_SOUND_MIN_VELOCITY) {
                            SetSoundVolume(len > BOUNCE_SOUND_MAX_VELOCITY ? 1.0f : idMath.Sqrt(len - BOUNCE_SOUND_MIN_VELOCITY) * (1.0f / idMath.Sqrt(BOUNCE_SOUND_MAX_VELOCITY - BOUNCE_SOUND_MIN_VELOCITY)));
                            StartSound("snd_bounce", SND_CHANNEL_ANY, 0, true, null);
                        }
                    }
                    return false;
                }
            }

            SetOrigin(collision.endpos);
            SetAxis(collision.endAxis);

            // unlink the clip model because we no longer need it
            GetPhysics().UnlinkClip();

            damageDefName = this.spawnArgs.GetString("def_damage");

            ignore = null;

            // if the hit entity takes damage
            if (ent.fl.takedamage) {
                if (this.damagePower != 0) {
                    damageScale = this.damagePower;
                } else {
                    damageScale = 1.0f;
                }

                // if the projectile owner is a player
                if ((this.owner.GetEntity() != null) && this.owner.GetEntity().IsType(idPlayer.class)) {
                    // if the projectile hit an actor
                    if (ent.IsType(idActor.class)) {
                        final idPlayer player = (idPlayer) this.owner.GetEntity();
                        player.AddProjectileHits(1);
                        damageScale *= player.PowerUpModifier(PROJECTILE_DAMAGE);
                    }
                }

                if (!damageDefName.isEmpty()) {//[0] != '\0') {
                    ent.Damage(this, this.owner.GetEntity(), dir, damageDefName, damageScale, CLIPMODEL_ID_TO_JOINT_HANDLE(collision.c.id));
                    ignore = ent;
                }
            }

            // if the projectile causes a damage effect
            if (this.spawnArgs.GetBool("impact_damage_effect")) {
                // if the hit entity has a special damage effect
                if (ent.spawnArgs.GetBool("bleed")) {
                    ent.AddDamageEffect(collision, velocity, damageDefName);
                } else {
                    AddDefaultDamageEffect(collision, velocity);
                }
            }

            Explode(collision, ignore);

            return true;
        }

        public void Explode(final trace_s collision, idEntity ignore) {
            String fxname, light_shader, sndExplode;
            float light_fadetime;
            final idVec3 normal = new idVec3();
            int removeTime;

            if ((this.state == EXPLODED) || (this.state == FIZZLED)) {
                return;
            }

            // stop sound
            StopSound(etoi(SND_CHANNEL_BODY2), false);

            // play explode sound
            switch ((int) this.damagePower) {
                case 2:
                    sndExplode = "snd_explode2";
                    break;
                case 3:
                    sndExplode = "snd_explode3";
                    break;
                case 4:
                    sndExplode = "snd_explode4";
                    break;
                default:
                    sndExplode = "snd_explode";
                    break;
            }
            StartSound(sndExplode, SND_CHANNEL_BODY, 0, true, null);

            // we need to work out how long the effects last and then remove them at that time
            // for example, bullets have no real effects
            if ((this.smokeFly != null) && (this.smokeFlyTime != 0)) {
                this.smokeFlyTime = 0;
            }

            Hide();
            FreeLightDef();

            if (this.spawnArgs.GetVector("detonation_axis", "", normal)) {
                GetPhysics().SetAxis(normal.ToMat3());
            }
            GetPhysics().SetOrigin(collision.endpos.oPlus(collision.c.normal.oMultiply(2.0f)));

            // default remove time
            removeTime = this.spawnArgs.GetInt("remove_time", "1500");

            // change the model, usually to a PRT
            if (g_testParticle.GetInteger() == TEST_PARTICLE_IMPACT) {
                fxname = g_testParticleName.GetString();
            } else {
                fxname = this.spawnArgs.GetString("model_detonate");
            }

            final int surfaceType = (collision.c.material != null ? collision.c.material.GetSurfaceType() : SURFTYPE_METAL).ordinal();
            if (!((fxname != null) && !fxname.isEmpty())) {
                if ((surfaceType == etoi(SURFTYPE_NONE))
                        || (surfaceType == etoi(SURFTYPE_METAL))
                        || (surfaceType == etoi(SURFTYPE_STONE))) {
                    fxname = this.spawnArgs.GetString("model_smokespark");
                } else if (surfaceType == etoi(SURFTYPE_RICOCHET)) {
                    fxname = this.spawnArgs.GetString("model_ricochet");
                } else {
                    fxname = this.spawnArgs.GetString("model_smoke");
                }
            }

            if ((fxname != null) && !fxname.isEmpty()) {
                SetModel(fxname);
                this.renderEntity.shaderParms[SHADERPARM_RED]
                        = this.renderEntity.shaderParms[SHADERPARM_GREEN]
                        = this.renderEntity.shaderParms[SHADERPARM_BLUE]
                        = this.renderEntity.shaderParms[SHADERPARM_ALPHA] = 1.0f;
                this.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                this.renderEntity.shaderParms[SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();
                Show();
                removeTime = (removeTime > 3000) ? removeTime : 3000;
            }

            // explosion light
            light_shader = this.spawnArgs.GetString("mtr_explode_light_shader");
            if (light_shader != null) {
                this.renderLight.shader = declManager.FindMaterial(light_shader, false);
                this.renderLight.pointLight = true;
                this.renderLight.lightRadius.oSet(1,
                        this.renderLight.lightRadius.oSet(2,
                                this.renderLight.lightRadius.oSet(2, this.spawnArgs.GetFloat("explode_light_radius"))));
                this.spawnArgs.GetVector("explode_light_color", "1 1 1", this.lightColor);
                this.renderLight.shaderParms[SHADERPARM_RED] = this.lightColor.x;
                this.renderLight.shaderParms[SHADERPARM_GREEN] = this.lightColor.y;
                this.renderLight.shaderParms[SHADERPARM_BLUE] = this.lightColor.z;
                this.renderLight.shaderParms[SHADERPARM_ALPHA] = 1.0f;
                this.renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                light_fadetime = this.spawnArgs.GetFloat("explode_light_fadetime", "0.5");
                this.lightStartTime = gameLocal.time;
                this.lightEndTime = (int) (gameLocal.time + SEC2MS(light_fadetime));
                BecomeActive(TH_THINK);
            }

            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.PutToRest();

            this.state = EXPLODED;

            if (gameLocal.isClient) {
                return;
            }

            // alert the ai
            gameLocal.AlertAI(this.owner.GetEntity());

            // bind the projectile to the impact entity if necesary
            if ((gameLocal.entities[collision.c.entityNum] != null) && this.spawnArgs.GetBool("bindOnImpact")) {
                Bind(gameLocal.entities[collision.c.entityNum], true);
            }

            // splash damage
            if (!this.projectileFlags.noSplashDamage) {
                final float delay = this.spawnArgs.GetFloat("delay_splash");
                if (delay != 0) {
                    if (removeTime < (delay * 1000)) {
                        removeTime = (int) ((delay + 0.10f) * 1000);
                    }
                    PostEventSec(EV_RadiusDamage, delay, ignore);
                } else {
                    Event_RadiusDamage(idEventArg.toArg(ignore));
                }
            }

            // spawn debris entities
            final int fxdebris = this.spawnArgs.GetInt("debris_count");
            if (fxdebris != 0) {
                idDict debris = gameLocal.FindEntityDefDict("projectile_debris", false);
                if (debris != null) {
                    final int amount = gameLocal.random.RandomInt(fxdebris);
                    for (int i = 0; i < amount; i++) {
                        final idEntity[] ent = {null};
                        final idVec3 dir = new idVec3();
                        dir.x = gameLocal.random.CRandomFloat() * 4.0f;
                        dir.y = gameLocal.random.CRandomFloat() * 4.0f;
                        dir.z = gameLocal.random.RandomFloat() * 8.0f;
                        dir.Normalize();

                        gameLocal.SpawnEntityDef(debris, ent, false);
                        if ((null == ent[0]) || !ent[0].IsType(idDebris.class)) {
                            gameLocal.Error("'projectile_debris' is not an idDebris");
                        }

                        final idDebris debris2 = (idDebris) ent[0];
                        debris2.Create(this.owner.GetEntity(), this.physicsObj.GetOrigin(), dir.ToMat3());
                        debris2.Launch();
                    }
                }

                debris = gameLocal.FindEntityDefDict("projectile_shrapnel", false);
                if (debris != null) {
                    final int amount = gameLocal.random.RandomInt(fxdebris);
                    for (int i = 0; i < amount; i++) {
                        final idEntity[] ent = {null};
                        final idVec3 dir = new idVec3();
                        dir.x = gameLocal.random.CRandomFloat() * 8.0f;
                        dir.y = gameLocal.random.CRandomFloat() * 8.0f;
                        dir.z = (gameLocal.random.RandomFloat() * 8.0f) + 8.0f;
                        dir.Normalize();

                        gameLocal.SpawnEntityDef(debris, ent, false);
                        if ((null == ent[0]) || !ent[0].IsType(idDebris.class)) {
                            gameLocal.Error("'projectile_shrapnel' is not an idDebris");
                        }

                        final idDebris debris2 = (idDebris) ent[0];
                        debris2.Create(this.owner.GetEntity(), this.physicsObj.GetOrigin(), dir.ToMat3());
                        debris2.Launch();
                    }
                }
            }

            CancelEvents(EV_Explode);
            PostEventMS(EV_Remove, removeTime);
        }

        public void Fizzle() {

            if ((this.state == EXPLODED) || (this.state == FIZZLED)) {
                return;
            }

            StopSound(etoi(SND_CHANNEL_BODY), false);
            StartSound("snd_fizzle", SND_CHANNEL_BODY, 0, false, null);

            // fizzle FX
            final String psystem = this.spawnArgs.GetString("smoke_fuse");
            if ((psystem != null) && !psystem.isEmpty()) {
//FIXME:SMOKE		gameLocal.particles.SpawnParticles( GetPhysics().GetOrigin(), vec3_origin, psystem );
            }

            // we need to work out how long the effects last and then remove them at that time
            // for example, bullets have no real effects
            if ((this.smokeFly != null) && (this.smokeFlyTime != 0)) {
                this.smokeFlyTime = 0;
            }

            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.GetClipModel().Unlink();
            this.physicsObj.PutToRest();

            Hide();
            FreeLightDef();

            this.state = FIZZLED;

            if (gameLocal.isClient) {
                return;
            }

            CancelEvents(EV_Fizzle);
            PostEventMS(EV_Remove, this.spawnArgs.GetInt("remove_time", "1500"));
        }

        public static idVec3 GetVelocity(final idDict projectile) {
            final idVec3 velocity = new idVec3();

            projectile.GetVector("velocity", "0 0 0", velocity);
            return velocity;
        }

        public static idVec3 GetGravity(final idDict projectile) {
            float gravity;

            gravity = projectile.GetFloat("gravity");
            return new idVec3(0, 0, -gravity);
        }
        // enum {
        public static final int EVENT_DAMAGE_EFFECT = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_MAXEVENTS = EVENT_DAMAGE_EFFECT;
        // };

        public static void DefaultDamageEffect(idEntity soundEnt, final idDict projectileDef, final trace_s collision, final idVec3 velocity) {
            String decal, sound, typeName;
            surfTypes_t materialType;

            if (collision.c.material != null) {
                materialType = collision.c.material.GetSurfaceType();
            } else {
                materialType = SURFTYPE_METAL;
            }

            // get material type name
            typeName = gameLocal.sufaceTypeNames[ materialType.ordinal()];

            // play impact sound
            sound = projectileDef.GetString(va("snd_%s", typeName));
            if (sound.isEmpty()) {// == '\0' ) {
                sound = projectileDef.GetString("snd_metal");
            }
            if (sound.isEmpty()) {// == '\0' ) {
                sound = projectileDef.GetString("snd_impact");
            }
            if (sound.isEmpty()) {// == '\0' ) {
                soundEnt.StartSoundShader(declManager.FindSound(sound), SND_CHANNEL_BODY, 0, false, null);
            }

            // project decal
            decal = projectileDef.GetString(va("mtr_detonate_%s", typeName));
            if (decal.isEmpty()) {// == '\0' ) {
                decal = projectileDef.GetString("mtr_detonate");
            }
            if (decal.isEmpty()) {// == '\0' ) {
                gameLocal.ProjectDecal(collision.c.point, collision.c.normal.oNegative(), 8.0f, true, projectileDef.GetFloat("decal_size", "6.0"), decal);
            }
        }

        public static boolean ClientPredictionCollide(idEntity soundEnt, final idDict projectileDef, final trace_s collision, final idVec3 velocity, boolean addDamageEffect) {
            idEntity ent;

            // remove projectile when a 'noimpact' surface is hit
            if ((collision.c.material != null) && ((collision.c.material.GetSurfaceFlags() & SURF_NOIMPACT) != 0)) {
                return false;
            }

            // get the entity the projectile collided with
            ent = gameLocal.entities[ collision.c.entityNum];
            if (ent == null) {
                return false;
            }

            // don't do anything if hitting a noclip player
            if (ent.IsType(idPlayer.class) && ((idPlayer) ent).noclip) {
                return false;
            }

            if (ent.IsType(idActor.class) || (ent.IsType(idAFAttachment.class) && ((idAFAttachment) ent).GetBody().IsType(idActor.class))) {
                if (!projectileDef.GetBool("detonate_on_actor")) {
                    return false;
                }
            } else {
                if (!projectileDef.GetBool("detonate_on_world")) {
                    return false;
                }
            }

            // if the projectile causes a damage effect
            if (addDamageEffect && projectileDef.GetBool("impact_damage_effect")) {
                // if the hit entity does not have a special damage effect
                if (!ent.spawnArgs.GetBool("bleed")) {
                    // predict damage effect
                    DefaultDamageEffect(soundEnt, projectileDef, collision, velocity);
                }
            }
            return true;
        }

        @Override
        public void ClientPredictionThink() {
            if (null == this.renderEntity.hModel) {
                return;
            }
            Think();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(this.owner.GetSpawnId(), 32);
            msg.WriteBits(etoi(this.state), 3);
            msg.WriteBits(btoi(this.fl.hidden), 1);
            if (this.netSyncPhysics) {
                msg.WriteBits(1, 1);
                this.physicsObj.WriteToSnapshot(msg);
            } else {
                msg.WriteBits(0, 1);
                final idVec3 origin = this.physicsObj.GetOrigin();
                final idVec3 velocity = this.physicsObj.GetLinearVelocity();

                msg.WriteFloat(origin.x);
                msg.WriteFloat(origin.y);
                msg.WriteFloat(origin.z);

                msg.WriteDeltaFloat(0.0f, velocity.oGet(0), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, velocity.oGet(1), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, velocity.oGet(2), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            }
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            projectileState_t newState;

            this.owner.SetSpawnId(msg.ReadBits(32));
            newState = projectileState_t.values()[msg.ReadBits(3)];
            if (msg.ReadBits(1) != 0) {
                Hide();
            } else {
                Show();
            }

            while (this.state != newState) {
                switch (this.state) {
                    case SPAWNED: {
                        Create(this.owner.GetEntity(), getVec3_origin(), new idVec3(1, 0, 0));
                        break;
                    }
                    case CREATED: {
                        // the right origin and direction are required if you want bullet traces
                        Launch(getVec3_origin(), new idVec3(1, 0, 0), getVec3_origin());
                        break;
                    }
                    case LAUNCHED: {
                        if (newState == FIZZLED) {
                            Fizzle();
                        } else {
                            trace_s collision;
//					memset( &collision, 0, sizeof( collision ) );
                            collision = new trace_s();
                            collision.endAxis.oSet(GetPhysics().GetAxis());
                            collision.endpos.oSet(GetPhysics().GetOrigin());
                            collision.c.point.oSet(GetPhysics().GetOrigin());
                            collision.c.normal.Set(0, 0, 1);
                            Explode(collision, null);
                        }
                        break;
                    }
                    case FIZZLED:
                    case EXPLODED: {
                        StopSound(etoi(SND_CHANNEL_BODY2), false);
                        GameEdit.gameEdit.ParseSpawnArgsToRenderEntity(this.spawnArgs, this.renderEntity);
                        this.state = SPAWNED;
                        break;
                    }
                }
            }

            if (msg.ReadBits(1) != 0) {
                this.physicsObj.ReadFromSnapshot(msg);
            } else {
                final idVec3 origin = new idVec3();
                final idVec3 velocity = new idVec3();
                idVec3 tmp;
                idMat3 axis;

                origin.x = msg.ReadFloat();
                origin.y = msg.ReadFloat();
                origin.z = msg.ReadFloat();

                velocity.x = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                velocity.y = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                velocity.z = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);

                this.physicsObj.SetOrigin(origin);
                this.physicsObj.SetLinearVelocity(velocity);

                // align z-axis of model with the direction
                velocity.NormalizeFast();
                axis = velocity.ToMat3();
                tmp = axis.oGet(2);
                axis.oSet(2, axis.oGet(0));
                axis.oSet(0, tmp.oNegative());
                this.physicsObj.SetAxis(axis);
            }

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            trace_s collision;
            final idVec3 velocity = new idVec3();

            switch (event) {
                case EVENT_DAMAGE_EFFECT: {
//			memset( &collision, 0, sizeof( collision ) );
                    collision = new trace_s();
                    collision.c.point.oSet(0, msg.ReadFloat());
                    collision.c.point.oSet(1, msg.ReadFloat());
                    collision.c.point.oSet(2, msg.ReadFloat());
                    collision.c.normal.oSet(msg.ReadDir(24));
                    final int index = gameLocal.ClientRemapDecl(DECL_MATERIAL, msg.ReadLong());
                    collision.c.material = (index != -1) ? (idMaterial) (declManager.DeclByIndex(DECL_MATERIAL, index)) : null;
                    velocity.oSet(0, msg.ReadFloat(5, 10));
                    velocity.oSet(1, msg.ReadFloat(5, 10));
                    velocity.oSet(2, msg.ReadFloat(5, 10));
                    DefaultDamageEffect(this, this.spawnArgs, collision, velocity);
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        private void AddDefaultDamageEffect(final trace_s collision, final idVec3 velocity) {

            DefaultDamageEffect(this, this.spawnArgs, collision, velocity);

            if (gameLocal.isServer && this.fl.networkSync) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);
                int excludeClient;

                if (this.spawnArgs.GetBool("net_instanthit")) {
                    excludeClient = this.owner.GetEntityNum();
                } else {
                    excludeClient = -1;
                }

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteFloat(collision.c.point.oGet(0));
                msg.WriteFloat(collision.c.point.oGet(1));
                msg.WriteFloat(collision.c.point.oGet(2));
                msg.WriteDir(collision.c.normal, 24);
                msg.WriteLong((collision.c.material != null) ? gameLocal.ServerRemapDecl(-1, DECL_MATERIAL, collision.c.material.Index()) : -1);
                msg.WriteFloat(velocity.oGet(0), 5, 10);
                msg.WriteFloat(velocity.oGet(1), 5, 10);
                msg.WriteFloat(velocity.oGet(2), 5, 10);
                ServerSendEvent(EVENT_DAMAGE_EFFECT, msg, false, excludeClient);
            }
        }

        private void Event_Explode() {
            trace_s collision;

//	memset( &collision, 0, sizeof( collision ) );
            collision = new trace_s();
            collision.endAxis.oSet(GetPhysics().GetAxis());
            collision.endpos.oSet(GetPhysics().GetOrigin());
            collision.c.point.oSet(GetPhysics().GetOrigin());
            collision.c.normal.Set(0, 0, 1);
            AddDefaultDamageEffect(collision, collision.c.normal);
            Explode(collision, null);
        }

        private void Event_Fizzle() {
            Fizzle();
        }

        private void Event_RadiusDamage(idEventArg<idEntity> ignore) {
            final String splash_damage = this.spawnArgs.GetString("def_splash_damage");
            if (!splash_damage.isEmpty()) {//[0] != '\0' ) {
                gameLocal.RadiusDamage(this.physicsObj.GetOrigin(), this, this.owner.GetEntity(), ignore.value, this, splash_damage, this.damagePower);
            }
        }

        private void Event_Touch(idEventArg<idEntity> other, idEventArg<trace_s> trace) {

            if (IsHidden()) {
                return;
            }

            if (!other.value.equals(this.owner.GetEntity())) {
                trace_s collision;

                collision = new trace_s();//memset( &collision, 0, sizeof( collision ) );
                collision.endAxis.oSet(GetPhysics().GetAxis());
                collision.endpos.oSet(GetPhysics().GetOrigin());
                collision.c.point.oSet(GetPhysics().GetOrigin());
                collision.c.normal.Set(0, 0, 1);
                AddDefaultDamageEffect(collision, collision.c.normal);
                Explode(collision, null);
            }
        }

        private void Event_GetProjectileState() {
            idThread.ReturnInt(etoi(this.state));
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

     idGuidedProjectile

     ===============================================================================
     */
    public static class idGuidedProjectile extends idProjectile {
        // CLASS_PROTOTYPE( idGuidedProjectile );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private   idAngles              rndScale;
        private final   idAngles              rndAng;
        private   idAngles              angles;
        private   int                   rndUpdateTime;
        private   float                 turn_max;
        private   float                 clamp_dist;
        private   boolean               burstMode;
        private   boolean               unGuided;
        private   float                 burstDist;
        private   float                 burstVelocity;
        //
        protected float                 speed;
        protected idEntityPtr<idEntity> enemy;
        //
        //

        public idGuidedProjectile() {
            this.enemy = new idEntityPtr<>();
            this.speed = 0.0f;
            this.turn_max = 0.0f;
            this.clamp_dist = 0.0f;
            this.rndScale = getAng_zero();
            this.rndAng = getAng_zero();
            this.rndUpdateTime = 0;
            this.angles = getAng_zero();
            this.burstMode = false;
            this.burstDist = 0;
            this.burstVelocity = 0.0f;
            this.unGuided = false;
        }
        // ~idGuidedProjectile( void );

        @Override
        public void Save(idSaveGame savefile) {
            this.enemy.Save(savefile);
            savefile.WriteFloat(this.speed);
            savefile.WriteAngles(this.rndScale);
            savefile.WriteAngles(this.rndAng);
            savefile.WriteInt(this.rndUpdateTime);
            savefile.WriteFloat(this.turn_max);
            savefile.WriteFloat(this.clamp_dist);
            savefile.WriteAngles(this.angles);
            savefile.WriteBool(this.burstMode);
            savefile.WriteBool(this.unGuided);
            savefile.WriteFloat(this.burstDist);
            savefile.WriteFloat(this.burstVelocity);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.enemy.Restore(savefile);
            this.speed = savefile.ReadFloat();
            savefile.ReadAngles(this.rndScale);
            savefile.ReadAngles(this.rndAng);
            this.rndUpdateTime = savefile.ReadInt();
            this.turn_max = savefile.ReadFloat();
            this.clamp_dist = savefile.ReadFloat();
            savefile.ReadAngles(this.angles);
            this.burstMode = savefile.ReadBool();
            this.unGuided = savefile.ReadBool();
            this.burstDist = savefile.ReadFloat();
            this.burstVelocity = savefile.ReadFloat();
        }

        @Override
        public void Spawn() {
            super.Spawn();
        }

        @Override
        public void Think() {
            idVec3 dir;
            final idVec3 seekPos = new idVec3();
            idVec3 velocity;
            idVec3 nose;
            idVec3 tmp;
            idMat3 axis;
            idAngles dirAng;
            idAngles diff;
            float dist;
            float frac;
            int i;

            if ((this.state == LAUNCHED) && !this.unGuided) {

                GetSeekPos(seekPos);

                if (this.rndUpdateTime < gameLocal.time) {
                    this.rndAng.oSet(0, this.rndScale.oGet(0) * gameLocal.random.CRandomFloat());
                    this.rndAng.oSet(1, this.rndScale.oGet(1) * gameLocal.random.CRandomFloat());
                    this.rndAng.oSet(2, this.rndScale.oGet(2) * gameLocal.random.CRandomFloat());
                    this.rndUpdateTime = gameLocal.time + 200;
                }

                nose = this.physicsObj.GetOrigin().oPlus(this.physicsObj.GetAxis().oGet(0).oMultiply(10.0f));

                dir = seekPos.oMinus(nose);
                dist = dir.Normalize();
                dirAng = dir.ToAngles();

                // make it more accurate as it gets closer
                frac = dist / this.clamp_dist;
                if (frac > 1.0f) {
                    frac = 1.0f;
                }

                diff = dirAng.oMinus(this.angles).oPlus(this.rndAng.oMultiply(frac));

                // clamp the to the max turn rate
                diff.Normalize180();
                for (i = 0; i < 3; i++) {
                    if (diff.oGet(i) > this.turn_max) {
                        diff.oSet(i, this.turn_max);
                    } else if (diff.oGet(i) < -this.turn_max) {
                        diff.oSet(i, -this.turn_max);
                    }
                }
                this.angles.oPluSet(diff);

                // make the visual model always points the dir we're traveling
                dir = this.angles.ToForward();
                velocity = dir.oMultiply(this.speed);

                if (this.burstMode && (dist < this.burstDist)) {
                    this.unGuided = true;
                    velocity.oMulSet(this.burstVelocity);
                }

                this.physicsObj.SetLinearVelocity(velocity);

                // align z-axis of model with the direction
                axis = dir.ToMat3();
                tmp = axis.oGet(2);
                axis.oSet(2, axis.oGet(0));
                axis.oSet(0, tmp.oNegative());

                GetPhysics().SetAxis(axis);
            }

            super.Think();
        }

        @Override
        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float launchPower /*= 1.0f*/, final float dmgPower /*= 1.0f*/) {
            super.Launch(start, dir, pushVelocity, timeSinceFire, launchPower, dmgPower);
            if (this.owner.GetEntity() != null) {
                if (this.owner.GetEntity().IsType(idAI.class)) {
                    this.enemy.oSet(((idAI) this.owner.GetEntity()).GetEnemy());
                } else if (this.owner.GetEntity().IsType(idPlayer.class)) {
                    final trace_s[] tr = {null};
                    final idPlayer player = (idPlayer) this.owner.GetEntity();
                    final idVec3 start2 = player.GetEyePosition();
                    final idVec3 end2 = start2.oPlus(player.viewAxis.oGet(0).oMultiply(1000.0f));
                    gameLocal.clip.TracePoint(tr, start2, end2, MASK_SHOT_RENDERMODEL | CONTENTS_BODY, this.owner.GetEntity());
                    if (tr[0].fraction < 1.0f) {
                        this.enemy.oSet(gameLocal.GetTraceEntity(tr[0]));
                    }
                    // ignore actors on the player's team
                    if ((this.enemy.GetEntity() == null) || !this.enemy.GetEntity().IsType(idActor.class) || (((idActor) this.enemy.GetEntity()).team == player.team)) {
                        this.enemy.oSet(player.EnemyWithMostHealth());
                    }
                }
            }
            final idVec3 vel = this.physicsObj.GetLinearVelocity();
            this.angles = vel.ToAngles();
            this.speed = vel.Length();
            this.rndScale = this.spawnArgs.GetAngles("random", "15 15 0");
            this.turn_max = this.spawnArgs.GetFloat("turn_max", "180") / USERCMD_HZ;
            this.clamp_dist = this.spawnArgs.GetFloat("clamp_dist", "256");
            this.burstMode = this.spawnArgs.GetBool("burstMode");
            this.unGuided = false;
            this.burstDist = this.spawnArgs.GetFloat("burstDist", "64");
            this.burstVelocity = this.spawnArgs.GetFloat("burstVelocity", "1.25");
            UpdateVisuals();
        }

        protected void GetSeekPos(idVec3 out) {
            final idEntity enemyEnt = this.enemy.GetEntity();
            if (enemyEnt != null) {
                if (enemyEnt.IsType(idActor.class)) {
                    out.oSet(((idActor) enemyEnt).GetEyePosition());
                    out.z -= 12.0f;
                } else {
                    out.oSet(enemyEnt.GetPhysics().GetOrigin());
                }
            } else {
                out.oSet(GetPhysics().GetOrigin().oPlus(this.physicsObj.GetLinearVelocity().oMultiply(2.0f)));
            }
        }
    }

    /*
     ===============================================================================

     idSoulCubeMissile

     ===============================================================================
     */
    public static class idSoulCubeMissile extends idGuidedProjectile {
        // CLASS_PROTOTYPE ( idSoulCubeMissile );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private idVec3 startingVelocity;
        private idVec3 endingVelocity;
        private float accelTime;
        private int launchTime;
        private boolean killPhase;
        private boolean returnPhase;
        private idVec3 destOrg;
        private idVec3 orbitOrg;
        private int orbitTime;
        private int smokeKillTime;
        private idDeclParticle smokeKill;
        //
        //

        // ~idSoulCubeMissile();
        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.startingVelocity);
            savefile.WriteVec3(this.endingVelocity);
            savefile.WriteFloat(this.accelTime);
            savefile.WriteInt(this.launchTime);
            savefile.WriteBool(this.killPhase);
            savefile.WriteBool(this.returnPhase);
            savefile.WriteVec3(this.destOrg);
            savefile.WriteInt(this.orbitTime);
            savefile.WriteVec3(this.orbitOrg);
            savefile.WriteInt(this.smokeKillTime);
            savefile.WriteParticle(this.smokeKill);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.startingVelocity);
            savefile.ReadVec3(this.endingVelocity);
            this.accelTime = savefile.ReadFloat();
            this.launchTime = savefile.ReadInt();
            this.killPhase = savefile.ReadBool();
            this.returnPhase = savefile.ReadBool();
            savefile.ReadVec3(this.destOrg);
            this.orbitTime = savefile.ReadInt();
            savefile.ReadVec3(this.orbitOrg);
            this.smokeKillTime = savefile.ReadInt();
            savefile.ReadParticle(this.smokeKill);
        }

        @Override
        public void Spawn() {
            this.startingVelocity.Zero();
            this.endingVelocity.Zero();
            this.accelTime = 0.0f;
            this.launchTime = 0;
            this.killPhase = false;
            this.returnPhase = false;
            this.smokeKillTime = 0;
            this.smokeKill = null;
        }

        @Override
        public void Think() {
            float pct;
            final idVec3 seekPos = new idVec3();
            idEntity ownerEnt;

            if (this.state == LAUNCHED) {
                if (this.killPhase) {
                    // orbit the mob, cascading down
                    if (gameLocal.time < (this.orbitTime + 1500)) {
                        if (!gameLocal.smokeParticles.EmitSmoke(this.smokeKill, this.smokeKillTime, gameLocal.random.CRandomFloat(), this.orbitOrg, getMat3_identity())) {
                            this.smokeKillTime = gameLocal.time;
                        }
                    }
                } else {
                    if ((this.accelTime != 0) && (gameLocal.time < (this.launchTime + (this.accelTime * 1000)))) {
                        pct = (gameLocal.time - this.launchTime) / (this.accelTime * 1000);
                        this.speed = (this.startingVelocity.oPlus((this.startingVelocity.oPlus(this.endingVelocity)).oMultiply(pct))).Length();
                    }
                }
                super.Think();
                GetSeekPos(seekPos);
                if ((seekPos.oMinus(this.physicsObj.GetOrigin())).Length() < 32.0f) {
                    if (this.returnPhase) {
                        StopSound(etoi(SND_CHANNEL_ANY), false);
                        StartSound("snd_return", SND_CHANNEL_BODY2, 0, false, null);
                        Hide();
                        PostEventSec(EV_Remove, 2.0f);

                        ownerEnt = this.owner.GetEntity();
                        if ((ownerEnt != null) && ownerEnt.IsType(idPlayer.class)) {
                            ((idPlayer) ownerEnt).SetSoulCubeProjectile(null);
                        }

                        this.state = FIZZLED;
                    } else if (!this.killPhase) {
                        KillTarget(this.physicsObj.GetAxis().oGet(0));
                    }
                }
            }
        }

        @Override
        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float launchPower /*= 1.0f*/, final float dmgPower /*= 1.0f*/) {
            idVec3 newStart;
            idVec3 offs;
            idEntity ownerEnt;

            // push it out a little
            newStart = start.oPlus(dir.oMultiply(this.spawnArgs.GetFloat("launchDist")));
            offs = this.spawnArgs.GetVector("launchOffset", "0 0 -4");
            newStart.oPluSet(offs);
            super.Launch(newStart, dir, pushVelocity, timeSinceFire, launchPower, dmgPower);
            if ((this.enemy.GetEntity() == null) || !this.enemy.GetEntity().IsType(idActor.class)) {
                this.destOrg = start.oPlus(dir.oMultiply(256.0f));
            } else {
                this.destOrg.Zero();
            }
            this.physicsObj.SetClipMask(0); // never collide.. think routine will decide when to detonate
            this.startingVelocity = this.spawnArgs.GetVector("startingVelocity", "15 0 0");
            this.endingVelocity = this.spawnArgs.GetVector("endingVelocity", "1500 0 0");
            this.accelTime = this.spawnArgs.GetFloat("accelTime", "5");
            this.physicsObj.SetLinearVelocity(this.physicsObj.GetAxis().oGet(2).oMultiply(this.startingVelocity.Length()));
            this.launchTime = gameLocal.time;
            this.killPhase = false;
            UpdateVisuals();

            ownerEnt = this.owner.GetEntity();
            if ((ownerEnt != null) && ownerEnt.IsType(idPlayer.class)) {
                ((idPlayer) ownerEnt).SetSoulCubeProjectile(this);
            }

        }

        @Override
        protected void GetSeekPos(idVec3 out) {
            if (this.returnPhase && (this.owner.GetEntity() != null) && this.owner.GetEntity().IsType(idActor.class)) {
                final idActor act = (idActor) this.owner.GetEntity();
                out.oSet(act.GetEyePosition());
                return;
            }
            if (!this.destOrg.equals(getVec3_zero())) {
                out.oSet(this.destOrg);
                return;
            }
            super.GetSeekPos(out);
        }

        protected void ReturnToOwner() {
            this.speed *= 0.65f;
            this.killPhase = false;
            this.returnPhase = true;
            this.smokeFlyTime = 0;
        }

        protected void KillTarget(final idVec3 dir) {
            idEntity ownerEnt;
            String smokeName;
            idActor act;

            ReturnToOwner();
            if ((this.enemy.GetEntity() != null) && this.enemy.GetEntity().IsType(idActor.class)) {
                act = (idActor) this.enemy.GetEntity();
                this.killPhase = true;
                this.orbitOrg = act.GetPhysics().GetAbsBounds().GetCenter();
                this.orbitTime = gameLocal.time;
                this.smokeKillTime = 0;
                smokeName = this.spawnArgs.GetString("smoke_kill");
                if (!smokeName.isEmpty()) {// != '\0' ) {
                    this.smokeKill = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                    this.smokeKillTime = gameLocal.time;
                }
                ownerEnt = this.owner.GetEntity();
                if ((act.health > 0) && (ownerEnt != null) && ownerEnt.IsType(idPlayer.class) && (ownerEnt.health > 0) && !act.spawnArgs.GetBool("boss")) {
                    ((idPlayer) ownerEnt).GiveHealthPool(act.health);
                }
                act.Damage(this, this.owner.GetEntity(), dir, this.spawnArgs.GetString("def_damage"), 1.0f, INVALID_JOINT);
                act.GetAFPhysics().SetTimeScale(0.25f);
                StartSound("snd_explode", SND_CHANNEL_BODY, 0, false, null);
            }
        }
    }

    public static class beamTarget_t {

        idEntityPtr<idEntity> target;
        renderEntity_s renderEntity;
        int/*qhandle_t*/ modelDefHandle;
    }

    /*
     ===============================================================================

     idBFGProjectile

     ===============================================================================
     */
    public static class idBFGProjectile extends idProjectile {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idBFGProjectile );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idProjectile.getEventCallBacks());
            eventCallbacks.put(EV_RemoveBeams, (eventCallback_t0<idBFGProjectile>) idBFGProjectile::Event_RemoveBeams);
        }

        private final idList<beamTarget_t> beamTargets;
        private renderEntity_s       secondModel;
        private int/*qhandle_t*/     secondModelDefHandle;
        private int                  nextDamageTime;
        private idStr                damageFreq;
        //
        //

        public idBFGProjectile() {
            this.beamTargets = new idList<>();
            this.secondModel = new renderEntity_s();
            this.secondModelDefHandle = -1;
            this.nextDamageTime = 0;
            this.damageFreq = new idStr();
        }

        @Override
        protected void _deconstructor() {
            FreeBeams();

            if (this.secondModelDefHandle >= 0) {
                gameRenderWorld.FreeEntityDef(this.secondModelDefHandle);
                this.secondModelDefHandle = -1;
            }

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(this.beamTargets.Num());
            for (i = 0; i < this.beamTargets.Num(); i++) {
                this.beamTargets.oGet(i).target.Save(savefile);
                savefile.WriteRenderEntity(this.beamTargets.oGet(i).renderEntity);
                savefile.WriteInt(this.beamTargets.oGet(i).modelDefHandle);
            }

            savefile.WriteRenderEntity(this.secondModel);
            savefile.WriteInt(this.secondModelDefHandle);
            savefile.WriteInt(this.nextDamageTime);
            savefile.WriteString(this.damageFreq);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            final int[] num = new int[1];

            savefile.ReadInt(num);
            this.beamTargets.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.beamTargets.oGet(i).target.Restore(savefile);
                savefile.ReadRenderEntity(this.beamTargets.oGet(i).renderEntity);
                this.beamTargets.oGet(i).modelDefHandle = savefile.ReadInt();

                if (this.beamTargets.oGet(i).modelDefHandle >= 0) {
                    this.beamTargets.oGet(i).modelDefHandle = gameRenderWorld.AddEntityDef(this.beamTargets.oGet(i).renderEntity);
                }
            }

            savefile.ReadRenderEntity(this.secondModel);
            this.secondModelDefHandle = savefile.ReadInt();
            this.nextDamageTime = savefile.ReadInt();
            savefile.ReadString(this.damageFreq);

            if (this.secondModelDefHandle >= 0) {
                this.secondModelDefHandle = gameRenderWorld.AddEntityDef(this.secondModel);
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.beamTargets.Clear();
            this.secondModel = new renderEntity_s();//memset( &secondModel, 0, sizeof( secondModel ) );
            this.secondModelDefHandle = -1;
            final String temp = this.spawnArgs.GetString("model_two");
            if ((temp != null) && !temp.isEmpty()) {
                this.secondModel.hModel = renderModelManager.FindModel(temp);
                this.secondModel.bounds.oSet(this.secondModel.hModel.Bounds(this.secondModel));
                this.secondModel.shaderParms[ SHADERPARM_RED]
                        = this.secondModel.shaderParms[ SHADERPARM_GREEN]
                        = this.secondModel.shaderParms[ SHADERPARM_BLUE]
                        = this.secondModel.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                this.secondModel.noSelfShadow = true;
                this.secondModel.noShadow = true;
            }
            this.nextDamageTime = 0;
            this.damageFreq = null;
        }

        @Override
        public void Think() {
            if (this.state == LAUNCHED) {

                // update beam targets
                for (int i = 0; i < this.beamTargets.Num(); i++) {
                    if (this.beamTargets.oGet(i).target.GetEntity() == null) {
                        continue;
                    }
                    final idPlayer player = (this.beamTargets.oGet(i).target.GetEntity().IsType(idPlayer.class)) ? (idPlayer) this.beamTargets.oGet(i).target.GetEntity() : null;
                    idVec3 org = this.beamTargets.oGet(i).target.GetEntity().GetPhysics().GetAbsBounds().GetCenter();
                    this.beamTargets.oGet(i).renderEntity.origin.oSet(GetPhysics().GetOrigin());
                    this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_X] = org.x;
                    this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_Y] = org.y;
                    this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_Z] = org.z;
                    this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_RED]
                            = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_GREEN]
                            = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BLUE]
                            = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                    if (gameLocal.time > this.nextDamageTime) {
                        boolean bfgVision = true;
                        if ((this.damageFreq != null) && /*(const char *)*/ !this.damageFreq.IsEmpty() && (this.beamTargets.oGet(i).target.GetEntity() != null) && this.beamTargets.oGet(i).target.GetEntity().CanDamage(GetPhysics().GetOrigin(), org)) {
                            org = this.beamTargets.oGet(i).target.GetEntity().GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                            org.Normalize();
                            this.beamTargets.oGet(i).target.GetEntity().Damage(this, this.owner.GetEntity(), org, this.damageFreq.getData(), (this.damagePower != 0) ? this.damagePower : 1.0f, INVALID_JOINT);
                        } else {
                            this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_RED]
                                    = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_GREEN]
                                    = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BLUE]
                                    = this.beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_ALPHA] = 0.0f;
                            bfgVision = false;
                        }
                        if (player != null) {
                            player.playerView.EnableBFGVision(bfgVision);
                        }
                        this.nextDamageTime = gameLocal.time + BFG_DAMAGE_FREQUENCY;
                    }
                    gameRenderWorld.UpdateEntityDef(this.beamTargets.oGet(i).modelDefHandle, this.beamTargets.oGet(i).renderEntity);
                }

                if (this.secondModelDefHandle >= 0) {
                    this.secondModel.origin.oSet(GetPhysics().GetOrigin());
                    gameRenderWorld.UpdateEntityDef(this.secondModelDefHandle, this.secondModel);
                }

                final idAngles ang = new idAngles();

                ang.pitch = ((gameLocal.time & 4095) * 360.0f) / -4096.0f;
                ang.yaw = ang.pitch;
                ang.roll = 0.0f;
                SetAngles(ang);

                ang.pitch = ((gameLocal.time & 2047) * 360.0f) / -2048.0f;
                ang.yaw = ang.pitch;
                ang.roll = 0.0f;
                this.secondModel.axis.oSet(ang.ToMat3());

                UpdateVisuals();
            }

            super.Think();
        }

        @Override
        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float power /*= 1.0f*/, final float dmgPower /*= 1.0f*/) {
            super.Launch(start, dir, pushVelocity, 0.0f, power, dmgPower);

            // dmgPower * radius is the target acquisition area
            // acquisition should make sure that monsters are not dormant 
            // which will cut down on hitting monsters not actively fighting
            // but saves on the traces making sure they are visible
            // damage is not applied until the projectile explodes
            idEntity ent;
            final idEntity[] entityList = new idEntity[MAX_GENTITIES];
            int numListedEntities;
            idBounds bounds;
            final idVec3 damagePoint = new idVec3();

            final float[] radius = {0};
            this.spawnArgs.GetFloat("damageRadius", "512", radius);
            bounds = new idBounds(GetPhysics().GetOrigin()).Expand(radius[0]);

            final float beamWidth = this.spawnArgs.GetFloat("beam_WidthFly");
            final String skin = this.spawnArgs.GetString("skin_beam");

//	memset( &secondModel, 0, sizeof( secondModel ) );
            this.secondModel = new renderEntity_s();
            this.secondModelDefHandle = -1;
            final String temp = this.spawnArgs.GetString("model_two");
            if ((temp != null) && !temp.isEmpty()) {
                this.secondModel.hModel = renderModelManager.FindModel(temp);
                this.secondModel.bounds.oSet(this.secondModel.hModel.Bounds(this.secondModel));
                this.secondModel.shaderParms[ SHADERPARM_RED]
                        = this.secondModel.shaderParms[ SHADERPARM_GREEN]
                        = this.secondModel.shaderParms[ SHADERPARM_BLUE]
                        = this.secondModel.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                this.secondModel.noSelfShadow = true;
                this.secondModel.noShadow = true;
                this.secondModel.origin.oSet(GetPhysics().GetOrigin());
                this.secondModel.axis.oSet(GetPhysics().GetAxis());
                this.secondModelDefHandle = gameRenderWorld.AddEntityDef(this.secondModel);
            }

            final idVec3 delta = new idVec3(15.0f, 15.0f, 15.0f);
            //physicsObj.SetAngularExtrapolation( extrapolation_t(EXTRAPOLATION_LINEAR|EXTRAPOLATION_NOSTOP), gameLocal.time, 0, physicsObj.GetAxis().ToAngles(), delta, ang_zero );

            // get all entities touching the bounds
            numListedEntities = gameLocal.clip.EntitiesTouchingBounds(bounds, CONTENTS_BODY, entityList, MAX_GENTITIES);
            for (int e = 0; e < numListedEntities; e++) {
                ent = entityList[ e];
                assert (ent != null);

                if ((ent == this) || (ent == this.owner.GetEntity()) || ent.IsHidden() || !ent.IsActive() || !ent.fl.takedamage || (ent.health <= 0) || !ent.IsType(idActor.class)) {
                    continue;
                }

                if (!ent.CanDamage(GetPhysics().GetOrigin(), damagePoint)) {
                    continue;
                }

                if (ent.IsType(idPlayer.class)) {
                    final idPlayer player = (idPlayer) ent;
                    player.playerView.EnableBFGVision(true);
                }

                final beamTarget_t bt = new beamTarget_t();//memset( &bt.renderEntity, 0, sizeof( renderEntity_t ) );
                this.renderEntity = new renderEntity_s();
                bt.renderEntity.origin.oSet(GetPhysics().GetOrigin());
                bt.renderEntity.axis.oSet(GetPhysics().GetAxis());
                bt.renderEntity.shaderParms[ SHADERPARM_BEAM_WIDTH] = beamWidth;
                bt.renderEntity.shaderParms[ SHADERPARM_RED] = 1.0f;
                bt.renderEntity.shaderParms[ SHADERPARM_GREEN] = 1.0f;
                bt.renderEntity.shaderParms[ SHADERPARM_BLUE] = 1.0f;
                bt.renderEntity.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                bt.renderEntity.shaderParms[ SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat() * 0.75f;
                bt.renderEntity.hModel = renderModelManager.FindModel("_beam");
                bt.renderEntity.callback = null;
                bt.renderEntity.numJoints = 0;
                bt.renderEntity.joints = null;
                bt.renderEntity.bounds.Clear();
                bt.renderEntity.customSkin = declManager.FindSkin(skin);
                bt.target.oSet(ent);
                bt.modelDefHandle = gameRenderWorld.AddEntityDef(bt.renderEntity);
                this.beamTargets.Append(bt);
            }
            if (numListedEntities != 0) {
                StartSound("snd_beam", SND_CHANNEL_BODY2, 0, false, null);
            }
            this.damageFreq.oSet(this.spawnArgs.GetString("def_damageFreq"));
            this.nextDamageTime = gameLocal.time + BFG_DAMAGE_FREQUENCY;
            UpdateVisuals();
        }

        @Override
        public void Explode(final trace_s collision, idEntity ignore) {
            int i;
            final idVec3 dmgPoint = new idVec3();
            idVec3 dir;
            float beamWidth;
            float damageScale;
            String damage;
            idPlayer player;
            idEntity ownerEnt;

            ownerEnt = this.owner.GetEntity();
            if ((ownerEnt != null) && ownerEnt.IsType(idPlayer.class)) {
                player = (idPlayer) ownerEnt;
            } else {
                player = null;
            }

            beamWidth = this.spawnArgs.GetFloat("beam_WidthExplode");
            damage = this.spawnArgs.GetString("def_damage");

            for (i = 0; i < this.beamTargets.Num(); i++) {
                if ((this.beamTargets.oGet(i).target.GetEntity() == null) || (ownerEnt == null)) {
                    continue;
                }

                if (!this.beamTargets.oGet(i).target.GetEntity().CanDamage(GetPhysics().GetOrigin(), dmgPoint)) {
                    continue;
                }

                this.beamTargets.oGet(i).renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] = beamWidth;

                // if the hit entity takes damage
                if (this.damagePower != 0) {
                    damageScale = this.damagePower;
                } else {
                    damageScale = 1.0f;
                }

                // if the projectile owner is a player
                if (player != null) {
                    // if the projectile hit an actor
                    if (this.beamTargets.oGet(i).target.GetEntity().IsType(idActor.class)) {
                        player.SetLastHitTime(gameLocal.time);
                        player.AddProjectileHits(1);
                        damageScale *= player.PowerUpModifier(PROJECTILE_DAMAGE);
                    }
                }

                if (!damage.isEmpty() && (this.beamTargets.oGet(i).target.GetEntity().entityNumber > (gameLocal.numClients - 1))) {
                    dir = this.beamTargets.oGet(i).target.GetEntity().GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                    dir.Normalize();
                    this.beamTargets.oGet(i).target.GetEntity().Damage(this, ownerEnt, dir, damage, damageScale, (collision.c.id < 0) ? CLIPMODEL_ID_TO_JOINT_HANDLE(collision.c.id) : INVALID_JOINT);
                }
            }

            if (this.secondModelDefHandle >= 0) {
                gameRenderWorld.FreeEntityDef(this.secondModelDefHandle);
                this.secondModelDefHandle = -1;
            }

            if (ignore == null) {
                this.projectileFlags.noSplashDamage = true;
            }

            if (!gameLocal.isClient) {
                if (ignore != null) {
                    PostEventMS(EV_RemoveBeams, 750);
                } else {
                    PostEventMS(EV_RemoveBeams, 0);
                }
            }

            super.Explode(collision, ignore);
        }

        private void FreeBeams() {
            for (int i = 0; i < this.beamTargets.Num(); i++) {
                if (this.beamTargets.oGet(i).modelDefHandle >= 0) {
                    gameRenderWorld.FreeEntityDef(this.beamTargets.oGet(i).modelDefHandle);
                    this.beamTargets.oGet(i).modelDefHandle = -1;
                }
            }

            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                player.playerView.EnableBFGVision(false);
            }
        }

        private void Event_RemoveBeams() {
            FreeBeams();
            UpdateVisuals();
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }


//        private void ApplyDamage();
    }

    /*
     ===============================================================================

     idDebris
	
     ===============================================================================
     */
    public static class idDebris extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idDebris );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Explode, (eventCallback_t0<idDebris>) idDebris::Event_Explode);
            eventCallbacks.put(EV_Fizzle, (eventCallback_t0<idDebris>) idDebris::Event_Fizzle);
        }


        private idEntityPtr<idEntity> owner;
        private final idPhysics_RigidBody   physicsObj;
        private idDeclParticle        smokeFly;
        private int                   smokeFlyTime;
        private idSoundShader         sndBounce;
        //
        //

        public idDebris() {
            this.owner = null;
            this.physicsObj = new idPhysics_RigidBody();
            this.smokeFly = null;
            this.smokeFlyTime = 0;
            this.sndBounce = null;
        }

        // ~idDebris();
        // save games
        @Override
        public void Save(idSaveGame savefile) {					// archives object for save game file 
            this.owner.Save(savefile);

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteParticle(this.smokeFly);
            savefile.WriteInt(this.smokeFlyTime);
            savefile.WriteSoundShader(this.sndBounce);
        }

        @Override
        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file
            this.owner.Restore(savefile);

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            savefile.ReadParticle(this.smokeFly);
            this.smokeFlyTime = savefile.ReadInt();
            savefile.ReadSoundShader(this.sndBounce);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.owner = null;
            this.smokeFly = null;
            this.smokeFlyTime = 0;
        }

        public void Create(idEntity owner, final idVec3 start, final idMat3 axis) {
            Unbind();
            GetPhysics().SetOrigin(start);
            GetPhysics().SetAxis(axis);
            GetPhysics().SetContents(0);
            this.owner = new idEntityPtr<>(owner);
            this.smokeFly = null;
            this.smokeFlyTime = 0;
            this.sndBounce = null;
            UpdateVisuals();
        }

        public void Launch() {
            float fuse;
            final idVec3 velocity = new idVec3();
            final idAngles angular_velocity = new idAngles();
            float linear_friction;
            float angular_friction;
            float contact_friction;
            float bounce;
            float mass;
            float gravity;
            idVec3 gravVec;
            boolean randomVelocity;
            idMat3 axis;

            this.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            this.spawnArgs.GetVector("velocity", "0 0 0", velocity);
            this.spawnArgs.GetAngles("angular_velocity", "0 0 0", angular_velocity);

            linear_friction = this.spawnArgs.GetFloat("linear_friction");
            angular_friction = this.spawnArgs.GetFloat("angular_friction");
            contact_friction = this.spawnArgs.GetFloat("contact_friction");
            bounce = this.spawnArgs.GetFloat("bounce");
            mass = this.spawnArgs.GetFloat("mass");
            gravity = this.spawnArgs.GetFloat("gravity");
            fuse = this.spawnArgs.GetFloat("fuse");
            randomVelocity = this.spawnArgs.GetBool("random_velocity");

            if (mass <= 0) {
                gameLocal.Error("Invalid mass on '%s'\n", GetEntityDefName());
            }

            if (randomVelocity) {
                velocity.x *= gameLocal.random.RandomFloat() + 0.5f;
                velocity.y *= gameLocal.random.RandomFloat() + 0.5f;
                velocity.z *= gameLocal.random.RandomFloat() + 0.5f;
            }

            if (this.health != 0) {
                this.fl.takedamage = true;
            }

            gravVec = gameLocal.GetGravity();
            gravVec.NormalizeFast();
            axis = GetPhysics().GetAxis();

            Unbind();

            this.physicsObj.SetSelf(this);

            // check if a clip model is set
            final idStr clipModelName = new idStr();
            final idTraceModel trm = new idTraceModel();
            this.spawnArgs.GetString("clipmodel", "", clipModelName);
            if (clipModelName.IsEmpty()) {
                clipModelName.oSet(this.spawnArgs.GetString("model"));		// use the visual model
            }

            // load the trace model
            if (!CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm)) {
                // default to a box
                this.physicsObj.SetClipBox(this.renderEntity.bounds, 1.0f);
            } else {
                this.physicsObj.SetClipModel(new idClipModel(trm), 1.0f);
            }

            this.physicsObj.GetClipModel().SetOwner(this.owner.GetEntity());
            this.physicsObj.SetMass(mass);
            this.physicsObj.SetFriction(linear_friction, angular_friction, contact_friction);
            if (contact_friction == 0.0f) {
                this.physicsObj.NoContact();
            }
            this.physicsObj.SetBouncyness(bounce);
            this.physicsObj.SetGravity(gravVec.oMultiply(gravity));
            this.physicsObj.SetContents(0);
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_MOVEABLECLIP);
            this.physicsObj.SetLinearVelocity(axis.oGet(0).oMultiply(velocity.oGet(0)).oPlus(axis.oGet(1).oMultiply(velocity.oGet(1)).oPlus(axis.oGet(2).oMultiply(velocity.oGet(2)))));
            this.physicsObj.SetAngularVelocity(angular_velocity.ToAngularVelocity().oMultiply(axis));
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(axis);
            SetPhysics(this.physicsObj);

            if (!gameLocal.isClient) {
                if (fuse <= 0) {
                    // run physics for 1 second
                    RunPhysics();
                    PostEventMS(EV_Remove, 0);
                } else if (this.spawnArgs.GetBool("detonate_on_fuse")) {
                    if (fuse < 0.0f) {
                        fuse = 0.0f;
                    }
                    RunPhysics();
                    PostEventSec(EV_Explode, fuse);
                } else {
                    if (fuse < 0.0f) {
                        fuse = 0.0f;
                    }
                    PostEventSec(EV_Fizzle, fuse);
                }
            }

            StartSound("snd_fly", SND_CHANNEL_BODY, 0, false, null);

            this.smokeFly = null;
            this.smokeFlyTime = 0;
            final String smokeName = this.spawnArgs.GetString("smoke_fly");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                this.smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                this.smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(this.smokeFly, this.smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            final String sndName = this.spawnArgs.GetString("snd_bounce");
            if (isNotNullOrEmpty(sndName)) {//sndName != '\0' ) {
                this.sndBounce = declManager.FindSound(sndName);
            }

            UpdateVisuals();
        }

        @Override
        public void Think() {

            // run physics
            RunPhysics();
            Present();

            if ((this.smokeFly != null) && (this.smokeFlyTime != 0)) {
                if (!gameLocal.smokeParticles.EmitSmoke(this.smokeFly, this.smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis())) {
                    this.smokeFlyTime = 0;
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (this.spawnArgs.GetBool("detonate_on_death")) {
                Explode();
            } else {
                Fizzle();
            }
        }

        public void Explode() {
            if (IsHidden()) {
                // already exploded
                return;
            }

            StopSound(etoi(SND_CHANNEL_ANY), false);
            StartSound("snd_explode", SND_CHANNEL_BODY, 0, false, null);

            Hide();

            // these must not be "live forever" particle systems
            this.smokeFly = null;
            this.smokeFlyTime = 0;
            final String smokeName = this.spawnArgs.GetString("smoke_detonate");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                this.smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                this.smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(this.smokeFly, this.smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.PutToRest();

            CancelEvents(EV_Explode);
            PostEventMS(EV_Remove, 0);
        }

        public void Fizzle() {
            if (IsHidden()) {
                // already exploded
                return;
            }

            StopSound(etoi(SND_CHANNEL_ANY), false);
            StartSound("snd_fizzle", SND_CHANNEL_BODY, 0, false, null);

            // fizzle FX
            final String smokeName = this.spawnArgs.GetString("smoke_fuse");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                this.smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                this.smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(this.smokeFly, this.smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.PutToRest();

            Hide();

            if (gameLocal.isClient) {
                return;
            }

            CancelEvents(EV_Fizzle);
            PostEventMS(EV_Remove, 0);
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            if (this.sndBounce != null) {
                StartSoundShader(this.sndBounce, SND_CHANNEL_BODY, 0, false, null);
            }
            this.sndBounce = null;
            return false;
        }

        private void Event_Explode() {
            Explode();
        }

        private void Event_Fizzle() {
            Fizzle();
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
