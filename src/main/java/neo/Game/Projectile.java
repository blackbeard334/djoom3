package neo.Game;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AI.AI.idAI;
import neo.Game.Actor.idActor;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_THINK;
import neo.Game.Entity.idEntity;
import static neo.Game.Game.TEST_PARTICLE_IMPACT;
import neo.Game.GameSys.Class;
import static neo.Game.GameSys.Class.EV_Remove;

import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
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
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Mover.idDoor;
import static neo.Game.Physics.Clip.CLIPMODEL_ID_TO_JOINT_HANDLE;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force_Constant.idForce_Constant;
import static neo.Game.Physics.Physics_RigidBody.RB_VELOCITY_EXPONENT_BITS;
import static neo.Game.Physics.Physics_RigidBody.RB_VELOCITY_MANTISSA_BITS;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import static neo.Game.Player.PROJECTILE_DAMAGE;
import neo.Game.Player.idPlayer;
import static neo.Game.Projectile.idProjectile.projectileState_t.CREATED;
import static neo.Game.Projectile.idProjectile.projectileState_t.EXPLODED;
import static neo.Game.Projectile.idProjectile.projectileState_t.FIZZLED;
import static neo.Game.Projectile.idProjectile.projectileState_t.LAUNCHED;
import static neo.Game.Projectile.idProjectile.projectileState_t.SPAWNED;
import neo.Game.Script.Script_Thread.idThread;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_PROJECTILE;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Material.SURF_NOIMPACT;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.surfTypes_t;
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
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.TempDump.SERiAL;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import neo.framework.DeclParticle.idDeclParticle;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import static neo.idlib.Lib.LittleBitField;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import static neo.idlib.math.Angles.getAng_zero;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

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
        };
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
        };
        //
        protected projectileState_t state;
        //
        private   boolean           netSyncPhysics;
//
//

        // public :
        // CLASS_PROTOTYPE( idProjectile );
        public idProjectile() {
            owner = new idEntityPtr<>();
            lightDefHandle = -1;
            thrust = 0.0f;
            thrust_end = 0;
            smokeFly = null;
            smokeFlyTime = 0;
            state = SPAWNED;
            lightOffset = getVec3_zero();
            lightStartTime = 0;
            lightEndTime = 0;
            lightColor = getVec3_zero();
            state = SPAWNED;
            damagePower = 1.0f;
            projectileFlags = new projectileFlags_s();//memset( &projectileFlags, 0, sizeof( projectileFlags ) );
            renderLight = new renderLight_s();//memset( &renderLight, 0, sizeof( renderLight ) );
            
            // note: for net_instanthit projectiles, we will force this back to false at spawn time
            fl.networkSync = true;

            netSyncPhysics = false;
            
            physicsObj = new idPhysics_RigidBody();
            thruster = new idForce_Constant();
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
            
            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetContents(0);
            physicsObj.SetClipMask(0);
            physicsObj.PutToRest();
            SetPhysics(physicsObj);
        }

        @Override
        public void Save(idSaveGame savefile) {

            owner.Save(savefile);

            projectileFlags_s flags = projectileFlags;
            LittleBitField(flags);
            savefile.Write(flags);

            savefile.WriteFloat(thrust);
            savefile.WriteInt(thrust_end);

            savefile.WriteRenderLight(renderLight);
            savefile.WriteInt(lightDefHandle);
            savefile.WriteVec3(lightOffset);
            savefile.WriteInt(lightStartTime);
            savefile.WriteInt(lightEndTime);
            savefile.WriteVec3(lightColor);

            savefile.WriteParticle(smokeFly);
            savefile.WriteInt(smokeFlyTime);

            savefile.WriteInt(etoi(state));

            savefile.WriteFloat(damagePower);

            savefile.WriteStaticObject(physicsObj);
            savefile.WriteStaticObject(thruster);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            owner.Restore(savefile);

            savefile.Read(projectileFlags);
            LittleBitField(projectileFlags);

            thrust = savefile.ReadFloat();
            thrust_end = savefile.ReadInt();

            savefile.ReadRenderLight(renderLight);
            lightDefHandle = savefile.ReadInt();
            savefile.ReadVec3(lightOffset);
            lightStartTime = savefile.ReadInt();
            lightEndTime = savefile.ReadInt();
            savefile.ReadVec3(lightColor);

            savefile.ReadParticle(smokeFly);
            smokeFlyTime = savefile.ReadInt();

            state = projectileState_t.values()[savefile.ReadInt()];

            damagePower = savefile.ReadFloat();

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            savefile.ReadStaticObject(thruster);
            thruster.SetPhysics(physicsObj);

            if (smokeFly != null) {
                idVec3 dir;
                dir = physicsObj.GetLinearVelocity();
                dir.NormalizeFast();
                gameLocal.smokeParticles.EmitSmoke(smokeFly, gameLocal.time, gameLocal.random.RandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }
        }

        public void Create(idEntity owner, final idVec3 start, final idVec3 dir) {
//            idDict args;
            String shaderName;
            idVec3 light_color = new idVec3();
//            idVec3 light_offset;
            idVec3 tmp;
            idMat3 axis;

            Unbind();

            // align z-axis of model with the direction
            axis = dir.ToMat3();
            tmp = axis.oGet(2);
            axis.oSet(2, axis.oGet(0));
            axis.oSet(0, tmp.oNegative());

            physicsObj.SetOrigin(start);
            physicsObj.SetAxis(axis);

            physicsObj.GetClipModel().SetOwner(owner);

            this.owner.oSet(owner);

//	memset( &renderLight, 0, sizeof( renderLight ) );
            renderLight = new renderLight_s();
            shaderName = spawnArgs.GetString("mtr_light_shader");
            if (!shaderName.isEmpty()) {
                renderLight.shader = declManager.FindMaterial(shaderName, false);
                renderLight.pointLight = true;
                renderLight.lightRadius.oSet(0,
                        renderLight.lightRadius.oSet(1,
                                renderLight.lightRadius.oSet(2, spawnArgs.GetFloat("light_radius"))));
                spawnArgs.GetVector("light_color", "1 1 1", light_color);
                renderLight.shaderParms[0] = light_color.oGet(0);
                renderLight.shaderParms[1] = light_color.oGet(1);
                renderLight.shaderParms[2] = light_color.oGet(2);
                renderLight.shaderParms[3] = 1.0f;
            }

            spawnArgs.GetVector("light_offset", "0 0 0", lightOffset);

            lightStartTime = 0;
            lightEndTime = 0;
            smokeFlyTime = 0;

            damagePower = 1.0f;

            UpdateVisuals();

            state = CREATED;

            if (spawnArgs.GetBool("net_fullphysics")) {
                netSyncPhysics = true;
            }
        }

        public void Launch(final idVec3 start, final idVec3 dir, final idVec3 pushVelocity, final float timeSinceFire /*= 0.0f*/, final float launchPower /*= 1.0f*/, final float dmgPower /*= 1.0f*/) {
            float fuse;
            float startthrust;
            float endthrust;
            idVec3 velocity = new idVec3();
            idAngles angular_velocity = new idAngles();
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
            if (owner.GetEntity() != null && !owner.GetEntity().IsType(idPlayer.class)) {
                cinematic = owner.GetEntity().cinematic;
            } else {
                cinematic = false;
            }

            thrust = spawnArgs.GetFloat("thrust");
            startthrust = spawnArgs.GetFloat("thrust_start");
            endthrust = spawnArgs.GetFloat("thrust_end");

            spawnArgs.GetVector("velocity", "0 0 0", velocity);

            speed = velocity.Length() * launchPower;

            damagePower = dmgPower;

            spawnArgs.GetAngles("angular_velocity", "0 0 0", angular_velocity);

            linear_friction = spawnArgs.GetFloat("linear_friction");
            angular_friction = spawnArgs.GetFloat("angular_friction");
            contact_friction = spawnArgs.GetFloat("contact_friction");
            bounce = spawnArgs.GetFloat("bounce");
            mass = spawnArgs.GetFloat("mass");
            gravity = spawnArgs.GetFloat("gravity");
            fuse = spawnArgs.GetFloat("fuse");

            projectileFlags.detonate_on_world = spawnArgs.GetBool("detonate_on_world");
            projectileFlags.detonate_on_actor = spawnArgs.GetBool("detonate_on_actor");
            projectileFlags.randomShaderSpin = spawnArgs.GetBool("random_shader_spin");

            if (mass <= 0) {
                gameLocal.Error("Invalid mass on '%s'\n", GetEntityDefName());
            }

            thrust *= mass;
            thrust_start = (int) (SEC2MS(startthrust) + gameLocal.time);
            thrust_end = (int) (SEC2MS(endthrust) + gameLocal.time);

            lightStartTime = 0;
            lightEndTime = 0;

            if (health != 0) {
                fl.takedamage = true;
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
            if (spawnArgs.GetBool("detonate_on_trigger")) {
                contents |= CONTENTS_TRIGGER;
            }
            if (!spawnArgs.GetBool("no_contents")) {
                contents |= CONTENTS_PROJECTILE;
                clipMask |= CONTENTS_PROJECTILE;
            }

            // don't do tracers on client, we don't know origin and direction
            if (spawnArgs.GetBool("tracers") && gameLocal.random.RandomFloat() > 0.5f) {
                SetModel(spawnArgs.GetString("model_tracer"));
                projectileFlags.isTracer = true;
            }

            physicsObj.SetMass(mass);
            physicsObj.SetFriction(linear_friction, angular_friction, contact_friction);
            if (contact_friction == 0.0f) {
                physicsObj.NoContact();
            }
            physicsObj.SetBouncyness(bounce);
            physicsObj.SetGravity(gravVec.oMultiply(gravity));
            physicsObj.SetContents(contents);
            physicsObj.SetClipMask(clipMask);
            physicsObj.SetLinearVelocity(pushVelocity.oPlus(axis.oGet(2).oMultiply(speed)));
            physicsObj.SetAngularVelocity(angular_velocity.ToAngularVelocity().oMultiply(axis));
            physicsObj.SetOrigin(start);
            physicsObj.SetAxis(axis);

            thruster.SetPosition(physicsObj, 0, new idVec3(GetPhysics().GetBounds().oGet(0).x, 0, 0));

            if (!gameLocal.isClient) {
                if (fuse <= 0) {
                    // run physics for 1 second
                    RunPhysics();
                    PostEventMS(EV_Remove, spawnArgs.GetInt("remove_time", "1500"));
                } else if (spawnArgs.GetBool("detonate_on_fuse")) {
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

            if (projectileFlags.isTracer) {
                StartSound("snd_tracer", SND_CHANNEL_BODY, 0, false, null);
            } else {
                StartSound("snd_fly", SND_CHANNEL_BODY, 0, false, null);
            }

            smokeFlyTime = 0;
            final String smokeName = spawnArgs.GetString("smoke_fly");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                smokeFlyTime = gameLocal.time;
            }

            // used for the plasma bolts but may have other uses as well
            if (projectileFlags.randomShaderSpin) {
                float f = gameLocal.random.RandomFloat();
                f *= 0.5f;
                renderEntity.shaderParms[SHADERPARM_DIVERSITY] = f;
            }

            UpdateVisuals();

            state = LAUNCHED;
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
            if (lightDefHandle != -1) {
                gameRenderWorld.FreeLightDef(lightDefHandle);
                lightDefHandle = -1;
            }
        }

        public idEntity GetOwner() {
            return owner.GetEntity();
        }

        @Override
        public void Think() {

            if ((thinkFlags & TH_THINK) != 0) {
                if (thrust != 0 && (gameLocal.time < thrust_end)) {
                    // evaluate force
                    thruster.SetForce(GetPhysics().GetAxis().oGet(0).oMultiply(thrust));
                    thruster.Evaluate(gameLocal.time);
                }
            }

            // run physics
            RunPhysics();

            Present();

            // add the particles
            if (smokeFly != null && smokeFlyTime != 0 && !IsHidden()) {
                idVec3 dir = GetPhysics().GetLinearVelocity().oNegative();
                dir.Normalize();
                if (!gameLocal.smokeParticles.EmitSmoke(smokeFly, smokeFlyTime, gameLocal.random.RandomFloat(), GetPhysics().GetOrigin(), dir.ToMat3())) {
                    smokeFlyTime = gameLocal.time;
                }
            }

            // add the light
            if (renderLight.lightRadius.x > 0.0f && g_projectileLights.GetBool()) {
                renderLight.origin.oSet(GetPhysics().GetOrigin().oPlus(GetPhysics().GetAxis().oMultiply(lightOffset)));
                renderLight.axis.oSet(GetPhysics().GetAxis());
                if ((lightDefHandle != -1)) {
                    if (lightEndTime > 0 && gameLocal.time <= lightEndTime + gameLocal.GetMSec()) {
                        idVec3 color = new idVec3(0, 0, 0);//TODO:superfluous
                        if (gameLocal.time < lightEndTime) {
                            float frac = (float) (gameLocal.time - lightStartTime) / (float) (lightEndTime - lightStartTime);
                            color.Lerp(lightColor, color, frac);
                        }
                        renderLight.shaderParms[SHADERPARM_RED] = color.x;
                        renderLight.shaderParms[SHADERPARM_GREEN] = color.y;
                        renderLight.shaderParms[SHADERPARM_BLUE] = color.z;
                    }
                    gameRenderWorld.UpdateLightDef(lightDefHandle, renderLight);
                } else {
                    lightDefHandle = gameRenderWorld.AddLightDef(renderLight);
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (spawnArgs.GetBool("detonate_on_death")) {
                trace_s collision;

//		memset( &collision, 0, sizeof( collision ) );
                collision = new trace_s();
                collision.endAxis.oSet(GetPhysics().GetAxis());
                collision.endpos.oSet(GetPhysics().GetOrigin());
                collision.c.point.oSet(GetPhysics().GetOrigin());
                collision.c.normal.Set(0, 0, 1);
                Explode(collision, null);
                physicsObj.ClearContacts();
                physicsObj.PutToRest();
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
            float[] push = {0};
            float damageScale;

            if (state == EXPLODED || state == FIZZLED) {
                return true;
            }

            // predict the explosion
            if (gameLocal.isClient) {
                if (ClientPredictionCollide(this, spawnArgs, collision, velocity, !spawnArgs.GetBool("net_instanthit"))) {
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
            if (ent.equals(owner.GetEntity())) {
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
            if (spawnArgs.GetFloat("push", "0", push) && push[0] > 0) {
                ent.ApplyImpulse(this, collision.c.id, collision.c.point, dir.oMultiply(push[0]));
            }

            // MP: projectiles open doors
            if (gameLocal.isMultiplayer && ent.IsType(idDoor.class) && !((idDoor) ent).IsOpen() && !ent.spawnArgs.GetBool("no_touch")) {
                ent.ProcessEvent(EV_Activate, this);
            }

            if (ent.IsType(idActor.class) || (ent.IsType(idAFAttachment.class) && ((idAFAttachment) ent).GetBody().IsType(idActor.class))) {
                if (!projectileFlags.detonate_on_actor) {
                    return false;
                }
            } else {
                if (!projectileFlags.detonate_on_world) {
                    if (!StartSound("snd_ricochet", SND_CHANNEL_ITEM, 0, true, null)) {
                        float len = velocity.Length();
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

            damageDefName = spawnArgs.GetString("def_damage");

            ignore = null;

            // if the hit entity takes damage
            if (ent.fl.takedamage) {
                if (damagePower != 0) {
                    damageScale = damagePower;
                } else {
                    damageScale = 1.0f;
                }

                // if the projectile owner is a player
                if (owner.GetEntity() != null && owner.GetEntity().IsType(idPlayer.class)) {
                    // if the projectile hit an actor
                    if (ent.IsType(idActor.class)) {
                        idPlayer player = (idPlayer) owner.GetEntity();
                        player.AddProjectileHits(1);
                        damageScale *= player.PowerUpModifier(PROJECTILE_DAMAGE);
                    }
                }

                if (!damageDefName.isEmpty()) {//[0] != '\0') {
                    ent.Damage(this, owner.GetEntity(), dir, damageDefName, damageScale, CLIPMODEL_ID_TO_JOINT_HANDLE(collision.c.id));
                    ignore = ent;
                }
            }

            // if the projectile causes a damage effect
            if (spawnArgs.GetBool("impact_damage_effect")) {
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
            idVec3 normal = new idVec3();
            int removeTime;

            if (state == EXPLODED || state == FIZZLED) {
                return;
            }

            // stop sound
            StopSound(etoi(SND_CHANNEL_BODY2), false);

            // play explode sound
            switch ((int) damagePower) {
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
            if (smokeFly != null && smokeFlyTime != 0) {
                smokeFlyTime = 0;
            }

            Hide();
            FreeLightDef();

            if (spawnArgs.GetVector("detonation_axis", "", normal)) {
                GetPhysics().SetAxis(normal.ToMat3());
            }
            GetPhysics().SetOrigin(collision.endpos.oPlus(collision.c.normal.oMultiply(2.0f)));

            // default remove time
            removeTime = spawnArgs.GetInt("remove_time", "1500");

            // change the model, usually to a PRT
            if (g_testParticle.GetInteger() == TEST_PARTICLE_IMPACT) {
                fxname = g_testParticleName.GetString();
            } else {
                fxname = spawnArgs.GetString("model_detonate");
            }

            int surfaceType = (collision.c.material != null ? collision.c.material.GetSurfaceType() : SURFTYPE_METAL).ordinal();
            if (!(fxname != null && !fxname.isEmpty())) {
                if ((surfaceType == etoi(SURFTYPE_NONE))
                        || (surfaceType == etoi(SURFTYPE_METAL))
                        || (surfaceType == etoi(SURFTYPE_STONE))) {
                    fxname = spawnArgs.GetString("model_smokespark");
                } else if (surfaceType == etoi(SURFTYPE_RICOCHET)) {
                    fxname = spawnArgs.GetString("model_ricochet");
                } else {
                    fxname = spawnArgs.GetString("model_smoke");
                }
            }

            if (fxname != null && !fxname.isEmpty()) {
                SetModel(fxname);
                renderEntity.shaderParms[SHADERPARM_RED]
                        = renderEntity.shaderParms[SHADERPARM_GREEN]
                        = renderEntity.shaderParms[SHADERPARM_BLUE]
                        = renderEntity.shaderParms[SHADERPARM_ALPHA] = 1.0f;
                renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                renderEntity.shaderParms[SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();
                Show();
                removeTime = (removeTime > 3000) ? removeTime : 3000;
            }

            // explosion light
            light_shader = spawnArgs.GetString("mtr_explode_light_shader");
            if (light_shader != null) {
                renderLight.shader = declManager.FindMaterial(light_shader, false);
                renderLight.pointLight = true;
                renderLight.lightRadius.oSet(1,
                        renderLight.lightRadius.oSet(2,
                                renderLight.lightRadius.oSet(2, spawnArgs.GetFloat("explode_light_radius"))));
                spawnArgs.GetVector("explode_light_color", "1 1 1", lightColor);
                renderLight.shaderParms[SHADERPARM_RED] = lightColor.x;
                renderLight.shaderParms[SHADERPARM_GREEN] = lightColor.y;
                renderLight.shaderParms[SHADERPARM_BLUE] = lightColor.z;
                renderLight.shaderParms[SHADERPARM_ALPHA] = 1.0f;
                renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                light_fadetime = spawnArgs.GetFloat("explode_light_fadetime", "0.5");
                lightStartTime = gameLocal.time;
                lightEndTime = (int) (gameLocal.time + SEC2MS(light_fadetime));
                BecomeActive(TH_THINK);
            }

            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.PutToRest();

            state = EXPLODED;

            if (gameLocal.isClient) {
                return;
            }

            // alert the ai
            gameLocal.AlertAI(owner.GetEntity());

            // bind the projectile to the impact entity if necesary
            if (gameLocal.entities[collision.c.entityNum] != null && spawnArgs.GetBool("bindOnImpact")) {
                Bind(gameLocal.entities[collision.c.entityNum], true);
            }

            // splash damage
            if (!projectileFlags.noSplashDamage) {
                float delay = spawnArgs.GetFloat("delay_splash");
                if (delay != 0) {
                    if (removeTime < delay * 1000) {
                        removeTime = (int) ((delay + 0.10f) * 1000);
                    }
                    PostEventSec(EV_RadiusDamage, delay, ignore);
                } else {
                    Event_RadiusDamage(idEventArg.toArg(ignore));
                }
            }

            // spawn debris entities
            int fxdebris = spawnArgs.GetInt("debris_count");
            if (fxdebris != 0) {
                idDict debris = gameLocal.FindEntityDefDict("projectile_debris", false);
                if (debris != null) {
                    int amount = gameLocal.random.RandomInt(fxdebris);
                    for (int i = 0; i < amount; i++) {
                        idEntity[] ent = {null};
                        idVec3 dir = new idVec3();
                        dir.x = gameLocal.random.CRandomFloat() * 4.0f;
                        dir.y = gameLocal.random.CRandomFloat() * 4.0f;
                        dir.z = gameLocal.random.RandomFloat() * 8.0f;
                        dir.Normalize();

                        gameLocal.SpawnEntityDef(debris, ent, false);
                        if (null == ent[0] || !ent[0].IsType(idDebris.class)) {
                            gameLocal.Error("'projectile_debris' is not an idDebris");
                        }

                        idDebris debris2 = (idDebris) ent[0];
                        debris2.Create(owner.GetEntity(), physicsObj.GetOrigin(), dir.ToMat3());
                        debris2.Launch();
                    }
                }

                debris = gameLocal.FindEntityDefDict("projectile_shrapnel", false);
                if (debris != null) {
                    int amount = gameLocal.random.RandomInt(fxdebris);
                    for (int i = 0; i < amount; i++) {
                        idEntity[] ent = {null};
                        idVec3 dir = new idVec3();
                        dir.x = gameLocal.random.CRandomFloat() * 8.0f;
                        dir.y = gameLocal.random.CRandomFloat() * 8.0f;
                        dir.z = gameLocal.random.RandomFloat() * 8.0f + 8.0f;
                        dir.Normalize();

                        gameLocal.SpawnEntityDef(debris, ent, false);
                        if (null == ent[0] || !ent[0].IsType(idDebris.class)) {
                            gameLocal.Error("'projectile_shrapnel' is not an idDebris");
                        }

                        idDebris debris2 = (idDebris) ent[0];
                        debris2.Create(owner.GetEntity(), physicsObj.GetOrigin(), dir.ToMat3());
                        debris2.Launch();
                    }
                }
            }

            CancelEvents(EV_Explode);
            PostEventMS(EV_Remove, removeTime);
        }

        public void Fizzle() {

            if (state == EXPLODED || state == FIZZLED) {
                return;
            }

            StopSound(etoi(SND_CHANNEL_BODY), false);
            StartSound("snd_fizzle", SND_CHANNEL_BODY, 0, false, null);

            // fizzle FX
            final String psystem = spawnArgs.GetString("smoke_fuse");
            if (psystem != null && !psystem.isEmpty()) {
//FIXME:SMOKE		gameLocal.particles.SpawnParticles( GetPhysics().GetOrigin(), vec3_origin, psystem );
            }

            // we need to work out how long the effects last and then remove them at that time
            // for example, bullets have no real effects
            if (smokeFly != null && smokeFlyTime != 0) {
                smokeFlyTime = 0;
            }

            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.GetClipModel().Unlink();
            physicsObj.PutToRest();

            Hide();
            FreeLightDef();

            state = FIZZLED;

            if (gameLocal.isClient) {
                return;
            }

            CancelEvents(EV_Fizzle);
            PostEventMS(EV_Remove, spawnArgs.GetInt("remove_time", "1500"));
        }

        public static idVec3 GetVelocity(final idDict projectile) {
            idVec3 velocity = new idVec3();

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
            if (collision.c.material != null && (collision.c.material.GetSurfaceFlags() & SURF_NOIMPACT) != 0) {
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
            if (null == renderEntity.hModel) {
                return;
            }
            Think();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(owner.GetSpawnId(), 32);
            msg.WriteBits(etoi(state), 3);
            msg.WriteBits(btoi(fl.hidden), 1);
            if (netSyncPhysics) {
                msg.WriteBits(1, 1);
                physicsObj.WriteToSnapshot(msg);
            } else {
                msg.WriteBits(0, 1);
                final idVec3 origin = physicsObj.GetOrigin();
                final idVec3 velocity = physicsObj.GetLinearVelocity();

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

            owner.SetSpawnId(msg.ReadBits(32));
            newState = projectileState_t.values()[msg.ReadBits(3)];
            if (msg.ReadBits(1) != 0) {
                Hide();
            } else {
                Show();
            }

            while (state != newState) {
                switch (state) {
                    case SPAWNED: {
                        Create(owner.GetEntity(), getVec3_origin(), new idVec3(1, 0, 0));
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
                        GameEdit.gameEdit.ParseSpawnArgsToRenderEntity(spawnArgs, renderEntity);
                        state = SPAWNED;
                        break;
                    }
                }
            }

            if (msg.ReadBits(1) != 0) {
                physicsObj.ReadFromSnapshot(msg);
            } else {
                idVec3 origin = new idVec3();
                idVec3 velocity = new idVec3();
                idVec3 tmp;
                idMat3 axis;

                origin.x = msg.ReadFloat();
                origin.y = msg.ReadFloat();
                origin.z = msg.ReadFloat();

                velocity.x = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                velocity.y = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
                velocity.z = msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);

                physicsObj.SetOrigin(origin);
                physicsObj.SetLinearVelocity(velocity);

                // align z-axis of model with the direction
                velocity.NormalizeFast();
                axis = velocity.ToMat3();
                tmp = axis.oGet(2);
                axis.oSet(2, axis.oGet(0));
                axis.oSet(0, tmp.oNegative());
                physicsObj.SetAxis(axis);
            }

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            trace_s collision;
            idVec3 velocity = new idVec3();

            switch (event) {
                case EVENT_DAMAGE_EFFECT: {
//			memset( &collision, 0, sizeof( collision ) );
                    collision = new trace_s();
                    collision.c.point.oSet(0, msg.ReadFloat());
                    collision.c.point.oSet(1, msg.ReadFloat());
                    collision.c.point.oSet(2, msg.ReadFloat());
                    collision.c.normal.oSet(msg.ReadDir(24));
                    int index = gameLocal.ClientRemapDecl(DECL_MATERIAL, msg.ReadLong());
                    collision.c.material = (index != -1) ? (idMaterial) (declManager.DeclByIndex(DECL_MATERIAL, index)) : null;
                    velocity.oSet(0, msg.ReadFloat(5, 10));
                    velocity.oSet(1, msg.ReadFloat(5, 10));
                    velocity.oSet(2, msg.ReadFloat(5, 10));
                    DefaultDamageEffect(this, spawnArgs, collision, velocity);
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        private void AddDefaultDamageEffect(final trace_s collision, final idVec3 velocity) {

            DefaultDamageEffect(this, spawnArgs, collision, velocity);

            if (gameLocal.isServer && fl.networkSync) {
                idBitMsg msg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);
                int excludeClient;

                if (spawnArgs.GetBool("net_instanthit")) {
                    excludeClient = owner.GetEntityNum();
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
            final String splash_damage = spawnArgs.GetString("def_splash_damage");
            if (!splash_damage.isEmpty()) {//[0] != '\0' ) {
                gameLocal.RadiusDamage(physicsObj.GetOrigin(), this, owner.GetEntity(), ignore.value, this, splash_damage, damagePower);
            }
        }

        private void Event_Touch(idEventArg<idEntity> other, idEventArg<trace_s> trace) {

            if (IsHidden()) {
                return;
            }

            if (!other.value.equals(owner.GetEntity())) {
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
            idThread.ReturnInt(etoi(state));
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };

    /*
     ===============================================================================

     idGuidedProjectile

     ===============================================================================
     */
    public static class idGuidedProjectile extends idProjectile {
        // CLASS_PROTOTYPE( idGuidedProjectile );

        private   idAngles              rndScale;
        private   idAngles              rndAng;
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
            enemy = new idEntityPtr<>();
            speed = 0.0f;
            turn_max = 0.0f;
            clamp_dist = 0.0f;
            rndScale = getAng_zero();
            rndAng = getAng_zero();
            rndUpdateTime = 0;
            angles = getAng_zero();
            burstMode = false;
            burstDist = 0;
            burstVelocity = 0.0f;
            unGuided = false;
        }
        // ~idGuidedProjectile( void );

        @Override
        public void Save(idSaveGame savefile) {
            enemy.Save(savefile);
            savefile.WriteFloat(speed);
            savefile.WriteAngles(rndScale);
            savefile.WriteAngles(rndAng);
            savefile.WriteInt(rndUpdateTime);
            savefile.WriteFloat(turn_max);
            savefile.WriteFloat(clamp_dist);
            savefile.WriteAngles(angles);
            savefile.WriteBool(burstMode);
            savefile.WriteBool(unGuided);
            savefile.WriteFloat(burstDist);
            savefile.WriteFloat(burstVelocity);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            enemy.Restore(savefile);
            speed = savefile.ReadFloat();
            savefile.ReadAngles(rndScale);
            savefile.ReadAngles(rndAng);
            rndUpdateTime = savefile.ReadInt();
            turn_max = savefile.ReadFloat();
            clamp_dist = savefile.ReadFloat();
            savefile.ReadAngles(angles);
            burstMode = savefile.ReadBool();
            unGuided = savefile.ReadBool();
            burstDist = savefile.ReadFloat();
            burstVelocity = savefile.ReadFloat();
        }

        @Override
        public void Spawn() {
            super.Spawn();
        }

        @Override
        public void Think() {
            idVec3 dir;
            idVec3 seekPos = new idVec3();
            idVec3 velocity;
            idVec3 nose;
            idVec3 tmp;
            idMat3 axis;
            idAngles dirAng;
            idAngles diff;
            float dist;
            float frac;
            int i;

            if (state == LAUNCHED && !unGuided) {

                GetSeekPos(seekPos);

                if (rndUpdateTime < gameLocal.time) {
                    rndAng.oSet(0, rndScale.oGet(0) * gameLocal.random.CRandomFloat());
                    rndAng.oSet(1, rndScale.oGet(1) * gameLocal.random.CRandomFloat());
                    rndAng.oSet(2, rndScale.oGet(2) * gameLocal.random.CRandomFloat());
                    rndUpdateTime = gameLocal.time + 200;
                }

                nose = physicsObj.GetOrigin().oPlus(physicsObj.GetAxis().oGet(0).oMultiply(10.0f));

                dir = seekPos.oMinus(nose);
                dist = dir.Normalize();
                dirAng = dir.ToAngles();

                // make it more accurate as it gets closer
                frac = dist / clamp_dist;
                if (frac > 1.0f) {
                    frac = 1.0f;
                }

                diff = dirAng.oMinus(angles).oPlus(rndAng.oMultiply(frac));

                // clamp the to the max turn rate
                diff.Normalize180();
                for (i = 0; i < 3; i++) {
                    if (diff.oGet(i) > turn_max) {
                        diff.oSet(i, turn_max);
                    } else if (diff.oGet(i) < -turn_max) {
                        diff.oSet(i, -turn_max);
                    }
                }
                angles.oPluSet(diff);

                // make the visual model always points the dir we're traveling
                dir = angles.ToForward();
                velocity = dir.oMultiply(speed);

                if (burstMode && dist < burstDist) {
                    unGuided = true;
                    velocity.oMulSet(burstVelocity);
                }

                physicsObj.SetLinearVelocity(velocity);

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
            if (owner.GetEntity() != null) {
                if (owner.GetEntity().IsType(idAI.class)) {
                    enemy.oSet(((idAI) owner.GetEntity()).GetEnemy());
                } else if (owner.GetEntity().IsType(idPlayer.class)) {
                    trace_s[] tr = {null};
                    idPlayer player = (idPlayer) owner.GetEntity();
                    idVec3 start2 = player.GetEyePosition();
                    idVec3 end2 = start2.oPlus(player.viewAxis.oGet(0).oMultiply(1000.0f));
                    gameLocal.clip.TracePoint(tr, start2, end2, MASK_SHOT_RENDERMODEL | CONTENTS_BODY, owner.GetEntity());
                    if (tr[0].fraction < 1.0f) {
                        enemy.oSet(gameLocal.GetTraceEntity(tr[0]));
                    }
                    // ignore actors on the player's team
                    if (enemy.GetEntity() == null || !enemy.GetEntity().IsType(idActor.class) || (((idActor) enemy.GetEntity()).team == player.team)) {
                        enemy.oSet(player.EnemyWithMostHealth());
                    }
                }
            }
            final idVec3 vel = physicsObj.GetLinearVelocity();
            angles = vel.ToAngles();
            speed = vel.Length();
            rndScale = spawnArgs.GetAngles("random", "15 15 0");
            turn_max = spawnArgs.GetFloat("turn_max", "180") / (float) USERCMD_HZ;
            clamp_dist = spawnArgs.GetFloat("clamp_dist", "256");
            burstMode = spawnArgs.GetBool("burstMode");
            unGuided = false;
            burstDist = spawnArgs.GetFloat("burstDist", "64");
            burstVelocity = spawnArgs.GetFloat("burstVelocity", "1.25");
            UpdateVisuals();
        }

        protected void GetSeekPos(idVec3 out) {
            idEntity enemyEnt = enemy.GetEntity();
            if (enemyEnt != null) {
                if (enemyEnt.IsType(idActor.class)) {
                    out.oSet(((idActor) enemyEnt).GetEyePosition());
                    out.z -= 12.0f;
                } else {
                    out.oSet(enemyEnt.GetPhysics().GetOrigin());
                }
            } else {
                out.oSet(GetPhysics().GetOrigin().oPlus(physicsObj.GetLinearVelocity().oMultiply(2.0f)));
            }
        }
    };

    /*
     ===============================================================================

     idSoulCubeMissile

     ===============================================================================
     */
    public static class idSoulCubeMissile extends idGuidedProjectile {
        // CLASS_PROTOTYPE ( idSoulCubeMissile );

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
            savefile.WriteVec3(startingVelocity);
            savefile.WriteVec3(endingVelocity);
            savefile.WriteFloat(accelTime);
            savefile.WriteInt(launchTime);
            savefile.WriteBool(killPhase);
            savefile.WriteBool(returnPhase);
            savefile.WriteVec3(destOrg);
            savefile.WriteInt(orbitTime);
            savefile.WriteVec3(orbitOrg);
            savefile.WriteInt(smokeKillTime);
            savefile.WriteParticle(smokeKill);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(startingVelocity);
            savefile.ReadVec3(endingVelocity);
            accelTime = savefile.ReadFloat();
            launchTime = savefile.ReadInt();
            killPhase = savefile.ReadBool();
            returnPhase = savefile.ReadBool();
            savefile.ReadVec3(destOrg);
            orbitTime = savefile.ReadInt();
            savefile.ReadVec3(orbitOrg);
            smokeKillTime = savefile.ReadInt();
            savefile.ReadParticle(smokeKill);
        }

        @Override
        public void Spawn() {
            startingVelocity.Zero();
            endingVelocity.Zero();
            accelTime = 0.0f;
            launchTime = 0;
            killPhase = false;
            returnPhase = false;
            smokeKillTime = 0;
            smokeKill = null;
        }

        @Override
        public void Think() {
            float pct;
            idVec3 seekPos = new idVec3();
            idEntity ownerEnt;

            if (state == LAUNCHED) {
                if (killPhase) {
                    // orbit the mob, cascading down
                    if (gameLocal.time < orbitTime + 1500) {
                        if (!gameLocal.smokeParticles.EmitSmoke(smokeKill, smokeKillTime, gameLocal.random.CRandomFloat(), orbitOrg, getMat3_identity())) {
                            smokeKillTime = gameLocal.time;
                        }
                    }
                } else {
                    if (accelTime != 0 && gameLocal.time < launchTime + accelTime * 1000) {
                        pct = (gameLocal.time - launchTime) / (accelTime * 1000);
                        speed = (startingVelocity.oPlus((startingVelocity.oPlus(endingVelocity)).oMultiply(pct))).Length();
                    }
                }
                super.Think();
                GetSeekPos(seekPos);
                if ((seekPos.oMinus(physicsObj.GetOrigin())).Length() < 32.0f) {
                    if (returnPhase) {
                        StopSound(etoi(SND_CHANNEL_ANY), false);
                        StartSound("snd_return", SND_CHANNEL_BODY2, 0, false, null);
                        Hide();
                        PostEventSec(EV_Remove, 2.0f);

                        ownerEnt = owner.GetEntity();
                        if (ownerEnt != null && ownerEnt.IsType(idPlayer.class)) {
                            ((idPlayer) ownerEnt).SetSoulCubeProjectile(null);
                        }

                        state = FIZZLED;
                    } else if (!killPhase) {
                        KillTarget(physicsObj.GetAxis().oGet(0));
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
            newStart = start.oPlus(dir.oMultiply(spawnArgs.GetFloat("launchDist")));
            offs = spawnArgs.GetVector("launchOffset", "0 0 -4");
            newStart.oPluSet(offs);
            super.Launch(newStart, dir, pushVelocity, timeSinceFire, launchPower, dmgPower);
            if (enemy.GetEntity() == null || !enemy.GetEntity().IsType(idActor.class)) {
                destOrg = start.oPlus(dir.oMultiply(256.0f));
            } else {
                destOrg.Zero();
            }
            physicsObj.SetClipMask(0); // never collide.. think routine will decide when to detonate
            startingVelocity = spawnArgs.GetVector("startingVelocity", "15 0 0");
            endingVelocity = spawnArgs.GetVector("endingVelocity", "1500 0 0");
            accelTime = spawnArgs.GetFloat("accelTime", "5");
            physicsObj.SetLinearVelocity(physicsObj.GetAxis().oGet(2).oMultiply(startingVelocity.Length()));
            launchTime = gameLocal.time;
            killPhase = false;
            UpdateVisuals();

            ownerEnt = owner.GetEntity();
            if (ownerEnt != null && ownerEnt.IsType(idPlayer.class)) {
                ((idPlayer) ownerEnt).SetSoulCubeProjectile(this);
            }

        }

        @Override
        protected void GetSeekPos(idVec3 out) {
            if (returnPhase && owner.GetEntity() != null && owner.GetEntity().IsType(idActor.class)) {
                idActor act = (idActor) owner.GetEntity();
                out.oSet(act.GetEyePosition());
                return;
            }
            if (!destOrg.equals(getVec3_zero())) {
                out.oSet(destOrg);
                return;
            }
            super.GetSeekPos(out);
        }

        protected void ReturnToOwner() {
            speed *= 0.65f;
            killPhase = false;
            returnPhase = true;
            smokeFlyTime = 0;
        }

        protected void KillTarget(final idVec3 dir) {
            idEntity ownerEnt;
            String smokeName;
            idActor act;

            ReturnToOwner();
            if (enemy.GetEntity() != null && enemy.GetEntity().IsType(idActor.class)) {
                act = (idActor) enemy.GetEntity();
                killPhase = true;
                orbitOrg = act.GetPhysics().GetAbsBounds().GetCenter();
                orbitTime = gameLocal.time;
                smokeKillTime = 0;
                smokeName = spawnArgs.GetString("smoke_kill");
                if (!smokeName.isEmpty()) {// != '\0' ) {
                    smokeKill = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                    smokeKillTime = gameLocal.time;
                }
                ownerEnt = owner.GetEntity();
                if ((act.health > 0) && ownerEnt != null && ownerEnt.IsType(idPlayer.class) && (ownerEnt.health > 0) && !act.spawnArgs.GetBool("boss")) {
                    ((idPlayer) ownerEnt).GiveHealthPool(act.health);
                }
                act.Damage(this, owner.GetEntity(), dir, spawnArgs.GetString("def_damage"), 1.0f, INVALID_JOINT);
                act.GetAFPhysics().SetTimeScale(0.25f);
                StartSound("snd_explode", SND_CHANNEL_BODY, 0, false, null);
            }
        }
    };

    public static class beamTarget_t {

        idEntityPtr<idEntity> target;
        renderEntity_s renderEntity;
        int/*qhandle_t*/ modelDefHandle;
    };

    /*
     ===============================================================================

     idBFGProjectile

     ===============================================================================
     */
    public static class idBFGProjectile extends idProjectile {
        // CLASS_PROTOTYPE( idBFGProjectile );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idProjectile.getEventCallBacks());
            eventCallbacks.put(EV_RemoveBeams, (eventCallback_t0<idBFGProjectile>) idBFGProjectile::Event_RemoveBeams);
        }

        private idList<beamTarget_t> beamTargets;
        private renderEntity_s       secondModel;
        private int/*qhandle_t*/     secondModelDefHandle;
        private int                  nextDamageTime;
        private idStr                damageFreq;
        //
        //

        public idBFGProjectile() {
            beamTargets = new idList<>();
            secondModel = new renderEntity_s();
            secondModelDefHandle = -1;
            nextDamageTime = 0;
            damageFreq = new idStr();
        }

        @Override
        protected void _deconstructor() {
            FreeBeams();

            if (secondModelDefHandle >= 0) {
                gameRenderWorld.FreeEntityDef(secondModelDefHandle);
                secondModelDefHandle = -1;
            }

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(beamTargets.Num());
            for (i = 0; i < beamTargets.Num(); i++) {
                beamTargets.oGet(i).target.Save(savefile);
                savefile.WriteRenderEntity(beamTargets.oGet(i).renderEntity);
                savefile.WriteInt(beamTargets.oGet(i).modelDefHandle);
            }

            savefile.WriteRenderEntity(secondModel);
            savefile.WriteInt(secondModelDefHandle);
            savefile.WriteInt(nextDamageTime);
            savefile.WriteString(damageFreq);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = new int[1];

            savefile.ReadInt(num);
            beamTargets.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                beamTargets.oGet(i).target.Restore(savefile);
                savefile.ReadRenderEntity(beamTargets.oGet(i).renderEntity);
                beamTargets.oGet(i).modelDefHandle = savefile.ReadInt();

                if (beamTargets.oGet(i).modelDefHandle >= 0) {
                    beamTargets.oGet(i).modelDefHandle = gameRenderWorld.AddEntityDef(beamTargets.oGet(i).renderEntity);
                }
            }

            savefile.ReadRenderEntity(secondModel);
            secondModelDefHandle = savefile.ReadInt();
            nextDamageTime = savefile.ReadInt();
            savefile.ReadString(damageFreq);

            if (secondModelDefHandle >= 0) {
                secondModelDefHandle = gameRenderWorld.AddEntityDef(secondModel);
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            beamTargets.Clear();
            secondModel = new renderEntity_s();//memset( &secondModel, 0, sizeof( secondModel ) );
            secondModelDefHandle = -1;
            final String temp = spawnArgs.GetString("model_two");
            if (temp != null && !temp.isEmpty()) {
                secondModel.hModel = renderModelManager.FindModel(temp);
                secondModel.bounds.oSet(secondModel.hModel.Bounds(secondModel));
                secondModel.shaderParms[ SHADERPARM_RED]
                        = secondModel.shaderParms[ SHADERPARM_GREEN]
                        = secondModel.shaderParms[ SHADERPARM_BLUE]
                        = secondModel.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                secondModel.noSelfShadow = true;
                secondModel.noShadow = true;
            }
            nextDamageTime = 0;
            damageFreq = null;
        }

        @Override
        public void Think() {
            if (state == LAUNCHED) {

                // update beam targets
                for (int i = 0; i < beamTargets.Num(); i++) {
                    if (beamTargets.oGet(i).target.GetEntity() == null) {
                        continue;
                    }
                    idPlayer player = (beamTargets.oGet(i).target.GetEntity().IsType(idPlayer.class)) ? (idPlayer) beamTargets.oGet(i).target.GetEntity() : null;
                    idVec3 org = beamTargets.oGet(i).target.GetEntity().GetPhysics().GetAbsBounds().GetCenter();
                    beamTargets.oGet(i).renderEntity.origin.oSet(GetPhysics().GetOrigin());
                    beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_X] = org.x;
                    beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_Y] = org.y;
                    beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BEAM_END_Z] = org.z;
                    beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_RED]
                            = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_GREEN]
                            = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BLUE]
                            = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                    if (gameLocal.time > nextDamageTime) {
                        boolean bfgVision = true;
                        if (damageFreq != null && /*(const char *)*/ !damageFreq.IsEmpty() && beamTargets.oGet(i).target.GetEntity() != null && beamTargets.oGet(i).target.GetEntity().CanDamage(GetPhysics().GetOrigin(), org)) {
                            org = beamTargets.oGet(i).target.GetEntity().GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                            org.Normalize();
                            beamTargets.oGet(i).target.GetEntity().Damage(this, owner.GetEntity(), org, damageFreq.toString(), (damagePower != 0) ? damagePower : 1.0f, INVALID_JOINT);
                        } else {
                            beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_RED]
                                    = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_GREEN]
                                    = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_BLUE]
                                    = beamTargets.oGet(i).renderEntity.shaderParms[ SHADERPARM_ALPHA] = 0.0f;
                            bfgVision = false;
                        }
                        if (player != null) {
                            player.playerView.EnableBFGVision(bfgVision);
                        }
                        nextDamageTime = gameLocal.time + BFG_DAMAGE_FREQUENCY;
                    }
                    gameRenderWorld.UpdateEntityDef(beamTargets.oGet(i).modelDefHandle, beamTargets.oGet(i).renderEntity);
                }

                if (secondModelDefHandle >= 0) {
                    secondModel.origin.oSet(GetPhysics().GetOrigin());
                    gameRenderWorld.UpdateEntityDef(secondModelDefHandle, secondModel);
                }

                idAngles ang = new idAngles();

                ang.pitch = (gameLocal.time & 4095) * 360.0f / -4096.0f;
                ang.yaw = ang.pitch;
                ang.roll = 0.0f;
                SetAngles(ang);

                ang.pitch = (gameLocal.time & 2047) * 360.0f / -2048.0f;
                ang.yaw = ang.pitch;
                ang.roll = 0.0f;
                secondModel.axis.oSet(ang.ToMat3());

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
            idEntity[] entityList = new idEntity[MAX_GENTITIES];
            int numListedEntities;
            idBounds bounds;
            idVec3 damagePoint = new idVec3();

            float[] radius = {0};
            spawnArgs.GetFloat("damageRadius", "512", radius);
            bounds = new idBounds(GetPhysics().GetOrigin()).Expand(radius[0]);

            float beamWidth = spawnArgs.GetFloat("beam_WidthFly");
            final String skin = spawnArgs.GetString("skin_beam");

//	memset( &secondModel, 0, sizeof( secondModel ) );
            secondModel = new renderEntity_s();
            secondModelDefHandle = -1;
            final String temp = spawnArgs.GetString("model_two");
            if (temp != null && !temp.isEmpty()) {
                secondModel.hModel = renderModelManager.FindModel(temp);
                secondModel.bounds.oSet(secondModel.hModel.Bounds(secondModel));
                secondModel.shaderParms[ SHADERPARM_RED]
                        = secondModel.shaderParms[ SHADERPARM_GREEN]
                        = secondModel.shaderParms[ SHADERPARM_BLUE]
                        = secondModel.shaderParms[ SHADERPARM_ALPHA] = 1.0f;
                secondModel.noSelfShadow = true;
                secondModel.noShadow = true;
                secondModel.origin.oSet(GetPhysics().GetOrigin());
                secondModel.axis.oSet(GetPhysics().GetAxis());
                secondModelDefHandle = gameRenderWorld.AddEntityDef(secondModel);
            }

            idVec3 delta = new idVec3(15.0f, 15.0f, 15.0f);
            //physicsObj.SetAngularExtrapolation( extrapolation_t(EXTRAPOLATION_LINEAR|EXTRAPOLATION_NOSTOP), gameLocal.time, 0, physicsObj.GetAxis().ToAngles(), delta, ang_zero );

            // get all entities touching the bounds
            numListedEntities = gameLocal.clip.EntitiesTouchingBounds(bounds, CONTENTS_BODY, entityList, MAX_GENTITIES);
            for (int e = 0; e < numListedEntities; e++) {
                ent = entityList[ e];
                assert (ent != null);

                if (ent == this || ent == owner.GetEntity() || ent.IsHidden() || !ent.IsActive() || !ent.fl.takedamage || ent.health <= 0 || !ent.IsType(idActor.class)) {
                    continue;
                }

                if (!ent.CanDamage(GetPhysics().GetOrigin(), damagePoint)) {
                    continue;
                }

                if (ent.IsType(idPlayer.class)) {
                    idPlayer player = (idPlayer) ent;
                    player.playerView.EnableBFGVision(true);
                }

                beamTarget_t bt = new beamTarget_t();//memset( &bt.renderEntity, 0, sizeof( renderEntity_t ) );
                renderEntity = new renderEntity_s();
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
                beamTargets.Append(bt);
            }
            if (numListedEntities != 0) {
                StartSound("snd_beam", SND_CHANNEL_BODY2, 0, false, null);
            }
            damageFreq.oSet(spawnArgs.GetString("def_damageFreq"));
            nextDamageTime = gameLocal.time + BFG_DAMAGE_FREQUENCY;
            UpdateVisuals();
        }

        @Override
        public void Explode(final trace_s collision, idEntity ignore) {
            int i;
            idVec3 dmgPoint = new idVec3();
            idVec3 dir;
            float beamWidth;
            float damageScale;
            String damage;
            idPlayer player;
            idEntity ownerEnt;

            ownerEnt = owner.GetEntity();
            if (ownerEnt != null && ownerEnt.IsType(idPlayer.class)) {
                player = (idPlayer) ownerEnt;
            } else {
                player = null;
            }

            beamWidth = spawnArgs.GetFloat("beam_WidthExplode");
            damage = spawnArgs.GetString("def_damage");

            for (i = 0; i < beamTargets.Num(); i++) {
                if ((beamTargets.oGet(i).target.GetEntity() == null) || (ownerEnt == null)) {
                    continue;
                }

                if (!beamTargets.oGet(i).target.GetEntity().CanDamage(GetPhysics().GetOrigin(), dmgPoint)) {
                    continue;
                }

                beamTargets.oGet(i).renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] = beamWidth;

                // if the hit entity takes damage
                if (damagePower != 0) {
                    damageScale = damagePower;
                } else {
                    damageScale = 1.0f;
                }

                // if the projectile owner is a player
                if (player != null) {
                    // if the projectile hit an actor
                    if (beamTargets.oGet(i).target.GetEntity().IsType(idActor.class)) {
                        player.SetLastHitTime(gameLocal.time);
                        player.AddProjectileHits(1);
                        damageScale *= player.PowerUpModifier(PROJECTILE_DAMAGE);
                    }
                }

                if (!damage.isEmpty() && (beamTargets.oGet(i).target.GetEntity().entityNumber > gameLocal.numClients - 1)) {
                    dir = beamTargets.oGet(i).target.GetEntity().GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                    dir.Normalize();
                    beamTargets.oGet(i).target.GetEntity().Damage(this, ownerEnt, dir, damage, damageScale, (collision.c.id < 0) ? CLIPMODEL_ID_TO_JOINT_HANDLE(collision.c.id) : INVALID_JOINT);
                }
            }

            if (secondModelDefHandle >= 0) {
                gameRenderWorld.FreeEntityDef(secondModelDefHandle);
                secondModelDefHandle = -1;
            }

            if (ignore == null) {
                projectileFlags.noSplashDamage = true;
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
            for (int i = 0; i < beamTargets.Num(); i++) {
                if (beamTargets.oGet(i).modelDefHandle >= 0) {
                    gameRenderWorld.FreeEntityDef(beamTargets.oGet(i).modelDefHandle);
                    beamTargets.oGet(i).modelDefHandle = -1;
                }
            }

            idPlayer player = gameLocal.GetLocalPlayer();
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
    };

    /*
     ===============================================================================

     idDebris
	
     ===============================================================================
     */
    public static class idDebris extends idEntity {
        // CLASS_PROTOTYPE( idDebris );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Explode, (eventCallback_t0<idDebris>) idDebris::Event_Explode);
            eventCallbacks.put(EV_Fizzle, (eventCallback_t0<idDebris>) idDebris::Event_Fizzle);
        }


        private idEntityPtr<idEntity> owner;
        private idPhysics_RigidBody   physicsObj;
        private idDeclParticle        smokeFly;
        private int                   smokeFlyTime;
        private idSoundShader         sndBounce;
        //
        //

        public idDebris() {
            owner = null;
            physicsObj = new idPhysics_RigidBody();
            smokeFly = null;
            smokeFlyTime = 0;
            sndBounce = null;
        }

        // ~idDebris();
        // save games
        @Override
        public void Save(idSaveGame savefile) {					// archives object for save game file 
            owner.Save(savefile);

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteParticle(smokeFly);
            savefile.WriteInt(smokeFlyTime);
            savefile.WriteSoundShader(sndBounce);
        }

        @Override
        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file
            owner.Restore(savefile);

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            savefile.ReadParticle(smokeFly);
            smokeFlyTime = savefile.ReadInt();
            savefile.ReadSoundShader(sndBounce);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            owner = null;
            smokeFly = null;
            smokeFlyTime = 0;
        }

        public void Create(idEntity owner, final idVec3 start, final idMat3 axis) {
            Unbind();
            GetPhysics().SetOrigin(start);
            GetPhysics().SetAxis(axis);
            GetPhysics().SetContents(0);
            this.owner = new idEntityPtr<>(owner);
            smokeFly = null;
            smokeFlyTime = 0;
            sndBounce = null;
            UpdateVisuals();
        }

        public void Launch() {
            float fuse;
            idVec3 velocity = new idVec3();
            idAngles angular_velocity = new idAngles();
            float linear_friction;
            float angular_friction;
            float contact_friction;
            float bounce;
            float mass;
            float gravity;
            idVec3 gravVec;
            boolean randomVelocity;
            idMat3 axis;

            renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            spawnArgs.GetVector("velocity", "0 0 0", velocity);
            spawnArgs.GetAngles("angular_velocity", "0 0 0", angular_velocity);

            linear_friction = spawnArgs.GetFloat("linear_friction");
            angular_friction = spawnArgs.GetFloat("angular_friction");
            contact_friction = spawnArgs.GetFloat("contact_friction");
            bounce = spawnArgs.GetFloat("bounce");
            mass = spawnArgs.GetFloat("mass");
            gravity = spawnArgs.GetFloat("gravity");
            fuse = spawnArgs.GetFloat("fuse");
            randomVelocity = spawnArgs.GetBool("random_velocity");

            if (mass <= 0) {
                gameLocal.Error("Invalid mass on '%s'\n", GetEntityDefName());
            }

            if (randomVelocity) {
                velocity.x *= gameLocal.random.RandomFloat() + 0.5f;
                velocity.y *= gameLocal.random.RandomFloat() + 0.5f;
                velocity.z *= gameLocal.random.RandomFloat() + 0.5f;
            }

            if (health != 0) {
                fl.takedamage = true;
            }

            gravVec = gameLocal.GetGravity();
            gravVec.NormalizeFast();
            axis = GetPhysics().GetAxis();

            Unbind();

            physicsObj.SetSelf(this);

            // check if a clip model is set
            idStr clipModelName = new idStr();
            idTraceModel trm = new idTraceModel();
            spawnArgs.GetString("clipmodel", "", clipModelName);
            if (clipModelName.IsEmpty()) {
                clipModelName.oSet(spawnArgs.GetString("model"));		// use the visual model
            }

            // load the trace model
            if (!CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm)) {
                // default to a box
                physicsObj.SetClipBox(renderEntity.bounds, 1.0f);
            } else {
                physicsObj.SetClipModel(new idClipModel(trm), 1.0f);
            }

            physicsObj.GetClipModel().SetOwner(owner.GetEntity());
            physicsObj.SetMass(mass);
            physicsObj.SetFriction(linear_friction, angular_friction, contact_friction);
            if (contact_friction == 0.0f) {
                physicsObj.NoContact();
            }
            physicsObj.SetBouncyness(bounce);
            physicsObj.SetGravity(gravVec.oMultiply(gravity));
            physicsObj.SetContents(0);
            physicsObj.SetClipMask(MASK_SOLID | CONTENTS_MOVEABLECLIP);
            physicsObj.SetLinearVelocity(axis.oGet(0).oMultiply(velocity.oGet(0)).oPlus(axis.oGet(1).oMultiply(velocity.oGet(1)).oPlus(axis.oGet(2).oMultiply(velocity.oGet(2)))));
            physicsObj.SetAngularVelocity(angular_velocity.ToAngularVelocity().oMultiply(axis));
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(axis);
            SetPhysics(physicsObj);

            if (!gameLocal.isClient) {
                if (fuse <= 0) {
                    // run physics for 1 second
                    RunPhysics();
                    PostEventMS(EV_Remove, 0);
                } else if (spawnArgs.GetBool("detonate_on_fuse")) {
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

            smokeFly = null;
            smokeFlyTime = 0;
            final String smokeName = spawnArgs.GetString("smoke_fly");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(smokeFly, smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            final String sndName = spawnArgs.GetString("snd_bounce");
            if (isNotNullOrEmpty(sndName)) {//sndName != '\0' ) {
                sndBounce = declManager.FindSound(sndName);
            }

            UpdateVisuals();
        }

        @Override
        public void Think() {

            // run physics
            RunPhysics();
            Present();

            if (smokeFly != null && smokeFlyTime != 0) {
                if (!gameLocal.smokeParticles.EmitSmoke(smokeFly, smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis())) {
                    smokeFlyTime = 0;
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (spawnArgs.GetBool("detonate_on_death")) {
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
            smokeFly = null;
            smokeFlyTime = 0;
            final String smokeName = spawnArgs.GetString("smoke_detonate");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(smokeFly, smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.PutToRest();

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
            final String smokeName = spawnArgs.GetString("smoke_fuse");
            if (isNotNullOrEmpty(smokeName)) {//smokeName != '\0' ) {
                smokeFly = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                smokeFlyTime = gameLocal.time;
                gameLocal.smokeParticles.EmitSmoke(smokeFly, smokeFlyTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            }

            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.PutToRest();

            Hide();

            if (gameLocal.isClient) {
                return;
            }

            CancelEvents(EV_Fizzle);
            PostEventMS(EV_Remove, 0);
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            if (sndBounce != null) {
                StartSoundShader(sndBounce, SND_CHANNEL_BODY, 0, false, null);
            }
            sndBounce = null;
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

    };
}
