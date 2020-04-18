package neo.Game;

import static neo.Game.AI.AI_Events.AI_RandomPath;
import static neo.Game.Actor.EV_Footstep;
import static neo.Game.Actor.EV_FootstepLeft;
import static neo.Game.Actor.EV_FootstepRight;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_FindTargets;
import static neo.Game.Entity.EV_PostSpawn;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_ALL;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEPARTICLES;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SaveGame.INITIAL_RELEASE_BUILD_NUMBER;
import static neo.Game.GameSys.SysCvar.ai_debugTrajectory;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_debugCinematic;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.GENTITYNUM_BITS;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_RADIO;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_WEAPON;
import static neo.Game.Physics.Force_Field.forceFieldApplyType.FORCEFIELD_APPLY_FORCE;
import static neo.Game.Physics.Force_Field.forceFieldApplyType.FORCEFIELD_APPLY_IMPULSE;
import static neo.Game.Physics.Force_Field.forceFieldApplyType.FORCEFIELD_APPLY_VELOCITY;
import static neo.Game.Player.EV_Player_ExitTeleporter;
import static neo.Game.Player.INFLUENCE_LEVEL3;
import static neo.Game.Player.INFLUENCE_NONE;
import static neo.Game.Sound.SSF_GLOBAL;
import static neo.Renderer.Material.CONTENTS_SOLID;
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
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.Renderer.RenderWorld.SHADERPARM_PARTICLE_STOPTIME;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_AIR;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_ALL;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_LOCATION;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_NONE;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_OBSTACLE;
import static neo.framework.Common.EDITOR_PARTICLE;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_DECELSINE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NONE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Vector.getVec3_origin;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFEntity_Gibbable;
import neo.Game.Actor.idActor;
import neo.Game.Camera.idCamera;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Game_local.idGameLocal;
import neo.Game.Moveable.idMoveable;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idProjectile;
import neo.Game.AI.AI.idAI;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t4;
import neo.Game.GameSys.Class.eventCallback_t6;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force_Field.idForce_Field;
import neo.Game.Physics.Force_Spring.idForce_Spring;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Model_liquid.idRenderModelLiquid;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclParticle.idDeclParticle;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Misc {

    /*
     ===============================================================================

     idSpawnableEntity

     A simple, spawnable entity with a model and no functionable ability of it's own.
     For example, it can be used as a placeholder during development, for marking
     locations on maps for script, or for simple placed models without any behavior
     that can be bound to other entities.  Should not be subclassed.
     ===============================================================================
     */
    public static class idSpawnableEntity extends idEntity {
        //public 	CLASS_PROTOTYPE( idSpawnableEntity );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void Spawn() {
            // this just holds dict information
            super.Spawn();
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     Potential spawning position for players.
     The first time a player enters the game, they will be at an 'initial' spot.
     Targets will be fired when someone spawns in on them.

     When triggered, will cause player to be teleported to spawn spot.

     ===============================================================================
     */
    public static final idEventDef EV_TeleportStage = new idEventDef("<TeleportStage>", "e");

    /*
     ===============================================================================

     idPlayerStart

     ===============================================================================
     */
    public static class idPlayerStart extends idEntity {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idPlayerStart );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idPlayerStart>) idPlayerStart::Event_TeleportPlayer);
            eventCallbacks.put(EV_TeleportStage, (eventCallback_t1<idPlayerStart>) idPlayerStart::Event_TeleportStage);
        }

        // enum {
        public static final int EVENT_TELEPORTPLAYER = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_MAXEVENTS      = EVENT_TELEPORTPLAYER + 1;
        // };
        private int teleportStage;
        //
        //

        public idPlayerStart() {
            this.teleportStage = 0;
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.teleportStage = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.teleportStage);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] teleportStage = {0};

            savefile.ReadInt(teleportStage);

            this.teleportStage = teleportStage[0];
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            int entityNumber;

            switch (event) {
                case EVENT_TELEPORTPLAYER: {
                    entityNumber = msg.ReadBits(GENTITYNUM_BITS);
                    final idPlayer player = (idPlayer) gameLocal.entities[entityNumber];
                    if ((player != null) && player.IsType(idPlayer.class)) {
                        Event_TeleportPlayer(player);
                    }
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        private void Event_TeleportPlayer(idEntity activator) {
            Event_TeleportPlayer(this, idEventArg.toArg(activator));
        }

        private static void Event_TeleportPlayer(idPlayerStart p, idEventArg<idEntity> activator) {
            idPlayer player;

            if (activator.value.IsType(idPlayer.class)) {
                player = (idPlayer) activator.value;
            } else {
                player = gameLocal.GetLocalPlayer();
            }
            if (player != null) {
                if (p.spawnArgs.GetBool("visualFx")) {

                    p.teleportStage = 0;
                    p.Event_TeleportStage(player);

                } else {

                    if (gameLocal.isServer) {
                        final idBitMsg msg = new idBitMsg();
                        final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                        msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                        msg.BeginWriting();
                        msg.WriteBits(player.entityNumber, GENTITYNUM_BITS);
                        p.ServerSendEvent(EVENT_TELEPORTPLAYER, msg, false, -1);
                    }

                    p.TeleportPlayer(player);
                }
            }
        }

        private void Event_TeleportStage(idEntity _player) {
            Event_TeleportStage(this, idEventArg.toArg(_player));
        }

        /*
         ===============
         idPlayerStart::Event_TeleportStage

         FIXME: add functionality to fx system ( could be done with player scripting too )
         ================
         */
        private static void Event_TeleportStage(idPlayerStart p, idEventArg<idEntity> _player) {
            idPlayer player;
            if (!_player.value.IsType(idPlayer.class)) {
                common.Warning("idPlayerStart::Event_TeleportStage: entity is not an idPlayer\n");
                return;
            }
            player = (idPlayer) _player.value;
            final float teleportDelay = p.spawnArgs.GetFloat("teleportDelay");
            switch (p.teleportStage) {
                case 0:
                    player.playerView.Flash(colorWhite, 125);
                    player.SetInfluenceLevel(INFLUENCE_LEVEL3);
                    player.SetInfluenceView(p.spawnArgs.GetString("mtr_teleportFx"), null, 0.0f, null);
                    gameSoundWorld.FadeSoundClasses(0, -20.0f, teleportDelay);
                    player.StartSound("snd_teleport_start", SND_CHANNEL_BODY2, 0, false, null);
                    p.teleportStage++;
                    p.PostEventSec(EV_TeleportStage, teleportDelay, player);
                    break;
                case 1:
                    gameSoundWorld.FadeSoundClasses(0, 0.0f, 0.25f);
                    p.teleportStage++;
                    p.PostEventSec(EV_TeleportStage, 0.25f, player);
                    break;
                case 2:
                    player.SetInfluenceView(null, null, 0.0f, null);
                    p.TeleportPlayer(player);
                    player.StopSound(etoi(SND_CHANNEL_BODY2), false);
                    player.SetInfluenceLevel(INFLUENCE_NONE);
                    p.teleportStage = 0;
                    break;
                default:
                    break;
            }
        }

        private void TeleportPlayer(idPlayer player) {
            final float pushVel = this.spawnArgs.GetFloat("push", "300");
            final float f = this.spawnArgs.GetFloat("visualEffect", "0");
            final String viewName = this.spawnArgs.GetString("visualView", "");
            final idEntity ent = viewName != null ? gameLocal.FindEntity(viewName) : null;//TODO:the standard C++ boolean checks if the bytes are switched on, which in the case of String means NOT NULL AND NOT EMPTY.

            if ((f != 0) && (ent != null)) {
                // place in private camera view for some time
                // the entity needs to teleport to where the camera view is to have the PVS right
                player.Teleport(ent.GetPhysics().GetOrigin(), getAng_zero(), this);
                player.StartSound("snd_teleport_enter", SND_CHANNEL_ANY, 0, false, null);
                player.SetPrivateCameraView((idCamera) ent);
                // the player entity knows where to spawn from the previous Teleport call
                if (!gameLocal.isClient) {
                    player.PostEventSec(EV_Player_ExitTeleporter, f);
                }
            } else {
                // direct to exit, Teleport will take care of the killbox
                player.Teleport(GetPhysics().GetOrigin(), GetPhysics().GetAxis().ToAngles(), null);

                // multiplayer hijacked this entity, so only push the player in multiplayer
                if (gameLocal.isMultiplayer) {
                    player.GetPhysics().SetLinearVelocity(GetPhysics().GetAxis().oGet(0).oMultiply(pushVel));
                }
            }
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

    /*
     ===============================================================================

     Non-displayed entity used to activate triggers when it touches them.
     Bind to a mover to have the mover activate a trigger as it moves.
     When target by triggers, activating the trigger will toggle the
     activator on and off. Check "start_off" to have it spawn disabled.
	
     ===============================================================================
     */
    public static class idActivator extends idEntity {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idActivator );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idActivator>) idActivator::Event_Activate);
        }

        private final boolean[] stay_on = new boolean[1];
        //
        //

        @Override
        public void Spawn() {
            final boolean[] start_off = new boolean[1];

            this.spawnArgs.GetBool("stay_on", "0", this.stay_on);
            this.spawnArgs.GetBool("start_off", "0", start_off);

            GetPhysics().SetClipBox(new idBounds(getVec3_origin()).Expand(4), 1.0f);
            GetPhysics().SetContents(0);

            if (!start_off[0]) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.stay_on[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadBool(this.stay_on);

            if (this.stay_on[0]) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Think() {
            RunPhysics();
            if ((this.thinkFlags & TH_THINK) != 0) {
                if (TouchTriggers()) {
                    if (!this.stay_on[0]) {
                        BecomeInactive(TH_THINK);
                    }
                }
            }
            Present();
        }

        private static void Event_Activate(idActivator a, idEventArg<idEntity> activator) {
            if ((a.thinkFlags & TH_THINK) != 0) {
                a.BecomeInactive(TH_THINK);
            } else {
                a.BecomeActive(TH_THINK);
            }
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

    /*
     ===============================================================================

     Path entities for monsters to follow.

     ===============================================================================
     */
    /*
     ===============================================================================

     idPathCorner

     ===============================================================================
     */
    public static class idPathCorner extends idEntity {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idPathCorner );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static{
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(AI_RandomPath, (eventCallback_t0<idPathCorner>) idPathCorner::Event_RandomPath);
        }

        public static void DrawDebugInfo() {
            idEntity ent;
            final idBounds bnds = new idBounds(new idVec3(-4.0f, -4.0f, -8.0f), new idVec3(4.0f, 4.0f, 64.0f));

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (!ent.IsType(idPathCorner.class)) {
                    continue;
                }

                final idVec3 org = ent.GetPhysics().GetOrigin();
                gameRenderWorld.DebugBounds(colorRed, bnds, org, 0);
            }
        }

        public static idPathCorner RandomPath(final idEntity source, final idEntity ignore) {
            int i;
            int num;
            int which;
            idEntity ent;
            final idPathCorner[] path = new idPathCorner[MAX_GENTITIES];

            num = 0;
            for (i = 0; i < source.targets.Num(); i++) {
                ent = source.targets.oGet(i).GetEntity();
                if ((ent != null) && (ent != ignore) && ent.IsType(idPathCorner.class)) {
                    path[ num++] = (idPathCorner) ent;
                    if (num >= MAX_GENTITIES) {
                        break;
                    }
                }
            }

            if (0 == num) {
                return null;
            }

            which = gameLocal.random.RandomInt(num);
            return path[ which];
        }

        private void Event_RandomPath() {
            idPathCorner path;

            path = RandomPath(this, null);
            idThread.ReturnEntity(path);
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

    /*
     ===============================================================================

     Object that fires targets and changes shader parms when damaged.

     ===============================================================================
     */
    public static final idEventDef EV_RestoreDamagable = new idEventDef("<RestoreDamagable>");
    /*
     ===============================================================================

     idDamagable
	
     ===============================================================================
     */

    public static class idDamagable extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idDamagable );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idDamagable>) idDamagable::Event_BecomeBroken);
            eventCallbacks.put(EV_RestoreDamagable, (eventCallback_t0<idDamagable>) idDamagable::Event_RestoreDamagable);
        }

        private final int[] count = {0};
        private final int[] nextTriggerTime = {0};
        //
        //

        public idDamagable() {
            this.count[0] = 0;
            this.nextTriggerTime[0] = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.count[0]);
            savefile.WriteInt(this.nextTriggerTime[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadInt(this.count);
            savefile.ReadInt(this.nextTriggerTime);
        }

        @Override
        public void Spawn() {
            final idStr broken = new idStr();

            this.health = this.spawnArgs.GetInt("health", "5");
            this.spawnArgs.GetInt("count", "1", this.count);
            this.nextTriggerTime[0] = 0;

            // make sure the model gets cached
            this.spawnArgs.GetString("broken", "", broken);
            if ((broken.Length() != 0) && NOT(renderModelManager.CheckModel(broken.getData()))) {
                idGameLocal.Error("idDamagable '%s' at (%s): cannot load broken model '%s'", this.name, GetPhysics().GetOrigin().ToString(0), broken);
            }

            this.fl.takedamage = true;
            GetPhysics().SetContents(CONTENTS_SOLID);
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (gameLocal.time < this.nextTriggerTime[0]) {
                this.health += damage;
                return;
            }

            BecomeBroken(attacker);
        }

        private void BecomeBroken(idEntity activator) {
            final float[] forceState = {0};
            final int[] numStates = {0};
            final int[] cycle = {0};
            final float[] wait = {0};

            if (gameLocal.time < this.nextTriggerTime[0]) {
                return;
            }

            this.spawnArgs.GetFloat("wait", "0.1", wait);
            this.nextTriggerTime[0] = (int) (gameLocal.time + SEC2MS(wait[0]));
            if (this.count[0] > 0) {
                this.count[0]--;
                if (0 == this.count[0]) {
                    this.fl.takedamage = false;
                } else {
                    this.health = this.spawnArgs.GetInt("health", "5");
                }
            }

            final idStr broken = new idStr();

            this.spawnArgs.GetString("broken", "", broken);
            if (broken.Length() != 0) {
                SetModel(broken.getData());
            }

            // offset the start time of the shader to sync it to the gameLocal time
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            this.spawnArgs.GetInt("numstates", "1", numStates);
            this.spawnArgs.GetInt("cycle", "0", cycle);
            this.spawnArgs.GetFloat("forcestate", "0", forceState);

            // set the state parm
            if (cycle[0] != 0) {
                this.renderEntity.shaderParms[ SHADERPARM_MODE]++;
                if (this.renderEntity.shaderParms[ SHADERPARM_MODE] > numStates[0]) {
                    this.renderEntity.shaderParms[ SHADERPARM_MODE] = 0;
                }
            } else if (forceState[0] != 0) {
                this.renderEntity.shaderParms[ SHADERPARM_MODE] = forceState[0];
            } else {
                this.renderEntity.shaderParms[ SHADERPARM_MODE] = gameLocal.random.RandomInt(numStates[0]) + 1;
            }

            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            ActivateTargets(activator);

            if (this.spawnArgs.GetBool("hideWhenBroken")) {
                Hide();
                PostEventMS(EV_RestoreDamagable, this.nextTriggerTime[0] - gameLocal.time);
                BecomeActive(TH_THINK);
            }
        }

        private static void Event_BecomeBroken(idDamagable d, idEventArg<idEntity> activator) {
            d.BecomeBroken(activator.value);
        }

        private void Event_RestoreDamagable() {
            this.health = this.spawnArgs.GetInt("health", "5");
            Show();
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

    /*
     ===============================================================================

     Hidden object that explodes when activated

     ===============================================================================
     */
    /*
     ===============================================================================

     idExplodable
	
     ===============================================================================
     */
    public static class idExplodable extends idEntity {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE( idExplodable );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static{
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idExplodable>) idExplodable::Event_Explode);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            Hide();
        }

        private static void Event_Explode(idExplodable e, idEventArg<idEntity> activator) {
            final String[] temp = {null};

            if (e.spawnArgs.GetString("def_damage", "damage_explosion", temp)) {
                gameLocal.RadiusDamage(e.GetPhysics().GetOrigin(), activator.value, activator.value, e, e, temp[0]);
            }

            e.StartSound("snd_explode", SND_CHANNEL_ANY, 0, false, null);

            // Show() calls UpdateVisuals, so we don't need to call it ourselves after setting the shaderParms
            e.renderEntity.shaderParms[SHADERPARM_RED] = 1.0f;
            e.renderEntity.shaderParms[SHADERPARM_GREEN] = 1.0f;
            e.renderEntity.shaderParms[SHADERPARM_BLUE] = 1.0f;
            e.renderEntity.shaderParms[SHADERPARM_ALPHA] = 1.0f;
            e.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            e.renderEntity.shaderParms[SHADERPARM_DIVERSITY] = 0.0f;
            e.Show();

            e.PostEventMS(EV_Remove, 2000);

            e.ActivateTargets(activator.value);
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

    /*
     ===============================================================================

     idSpring

     ===============================================================================
     */
    public static class idSpring extends idEntity {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE( idSpring );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_PostSpawn, (eventCallback_t0<idSpring>) idSpring::Event_LinkSpring);
        }

        private idEntity ent1;
        private idEntity ent2;
        private final int[] id1 = {0};
        private final int[] id2 = {0};
        private idVec3 p1;
        private idVec3 p2;
        private idForce_Spring spring;
        //
        //

        @Override
        public void Spawn() {
            final float[] Kstretch = {0}, damping = {0}, restLength = {0};

            this.spawnArgs.GetInt("id1", "0", this.id1);
            this.spawnArgs.GetInt("id2", "0", this.id2);
            this.spawnArgs.GetVector("point1", "0 0 0", this.p1);
            this.spawnArgs.GetVector("point2", "0 0 0", this.p2);
            this.spawnArgs.GetFloat("constant", "100.0f", Kstretch);
            this.spawnArgs.GetFloat("damping", "10.0f", damping);
            this.spawnArgs.GetFloat("restlength", "0.0f", restLength);

            this.spring.InitSpring(Kstretch[0], 0.0f, damping[0], restLength[0]);

            this.ent1 = this.ent2 = null;

            PostEventMS(EV_PostSpawn, 0);
        }

        @Override
        public void Think() {
            idVec3 start, end, origin;
            idMat3 axis;

            // run physics
            RunPhysics();

            if ((this.thinkFlags & TH_THINK) != 0) {
                // evaluate force
                this.spring.Evaluate(gameLocal.time);

                start = this.p1;
                if (this.ent1.GetPhysics() != null) {
                    axis = this.ent1.GetPhysics().GetAxis();
                    origin = this.ent1.GetPhysics().GetOrigin();
                    start = origin.oPlus(start.oMultiply(axis));
                }

                end = this.p2;
                if (this.ent2.GetPhysics() != null) {
                    axis = this.ent2.GetPhysics().GetAxis();
                    origin = this.ent2.GetPhysics().GetOrigin();
                    end = origin.oPlus(this.p2.oMultiply(axis));
                }

                gameRenderWorld.DebugLine(new idVec4(1, 1, 0, 1), start, end, 0, true);
            }

            Present();
        }

        private void Event_LinkSpring() {
            final idStr name1 = new idStr(), name2 = new idStr();

            this.spawnArgs.GetString("ent1", "", name1);
            this.spawnArgs.GetString("ent2", "", name2);

            if (name1.Length() != 0) {
                this.ent1 = gameLocal.FindEntity(name1.getData());
                if (null == this.ent1) {
                    idGameLocal.Error("idSpring '%s' at (%s): cannot find first entity '%s'", this.name, GetPhysics().GetOrigin().ToString(0), name1);
                }
            } else {
                this.ent1 = gameLocal.entities[ENTITYNUM_WORLD];
            }

            if (name2.Length() != 0) {
                this.ent2 = gameLocal.FindEntity(name2.getData());
                if (null == this.ent2) {
                    idGameLocal.Error("idSpring '%s' at (%s): cannot find second entity '%s'", this.name, GetPhysics().GetOrigin().ToString(0), name2);
                }
            } else {
                this.ent2 = gameLocal.entities[ENTITYNUM_WORLD];
            }
            this.spring.SetPosition(this.ent1.GetPhysics(), this.id1[0], this.p1, this.ent2.GetPhysics(), this.id2[0], this.p2);
            BecomeActive(TH_THINK);
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

    /*
     ===============================================================================

     idForceField

     ===============================================================================
     */
    public static final idEventDef EV_Toggle = new idEventDef("Toggle", null);

    public static class idForceField extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idForceField );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idForceField>) idForceField::Event_Activate);
            eventCallbacks.put(EV_Toggle, (eventCallback_t0<idForceField>) idForceField::Event_Toggle);
            eventCallbacks.put(EV_FindTargets, (eventCallback_t0<idForceField>) idForceField::Event_FindTargets );
        }

        private final idForce_Field forceField = new idForce_Field();
        //
        //

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteStaticObject(this.forceField);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadStaticObject(this.forceField);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            final idVec3 uniform = new idVec3();
            final float[] explosion = {0}, implosion = {0}, randomTorque = {0};

            if (this.spawnArgs.GetVector("uniform", "0 0 0", uniform)) {
                this.forceField.Uniform(uniform);
            } else if (this.spawnArgs.GetFloat("explosion", "0", explosion)) {
                this.forceField.Explosion(explosion[0]);
            } else if (this.spawnArgs.GetFloat("implosion", "0", implosion)) {
                this.forceField.Implosion(implosion[0]);
            }

            if (this.spawnArgs.GetFloat("randomTorque", "0", randomTorque)) {
                this.forceField.RandomTorque(randomTorque[0]);
            }

            if (this.spawnArgs.GetBool("applyForce", "0")) {
                this.forceField.SetApplyType(FORCEFIELD_APPLY_FORCE);
            } else if (this.spawnArgs.GetBool("applyImpulse", "0")) {
                this.forceField.SetApplyType(FORCEFIELD_APPLY_IMPULSE);
            } else {
                this.forceField.SetApplyType(FORCEFIELD_APPLY_VELOCITY);
            }

            this.forceField.SetPlayerOnly(this.spawnArgs.GetBool("playerOnly", "0"));
            this.forceField.SetMonsterOnly(this.spawnArgs.GetBool("monsterOnly", "0"));

            // set the collision model on the force field
            this.forceField.SetClipModel(new idClipModel(GetPhysics().GetClipModel()));

            // remove the collision model from the physics object
            GetPhysics().SetClipModel(null, 1.0f);

            if (this.spawnArgs.GetBool("start_on")) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                // evaluate force
                this.forceField.Evaluate(gameLocal.time);
            }
            Present();
        }

        private void Toggle() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                BecomeInactive(TH_THINK);
            } else {
                BecomeActive(TH_THINK);
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            final float[] wait = new float[1];

            Toggle();
            if (this.spawnArgs.GetFloat("wait", "0.01", wait)) {
                PostEventSec(EV_Toggle, wait[0]);
            }
        }

        private void Event_Toggle() {
            Toggle();
        }

        private void Event_FindTargets() {
            FindTargets();
            RemoveNullTargets();
            if (this.targets.Num() != 0) {
                this.forceField.Uniform(this.targets.oGet(0).GetEntity().GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin()));
            }
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
    /*
     ===============================================================================

     idAnimated

     ===============================================================================
     */
    public static final idEventDef EV_Animated_Start       = new idEventDef("<start>");
    public static final idEventDef EV_LaunchMissiles       = new idEventDef("launchMissiles", "ssssdf");
    public static final idEventDef EV_LaunchMissilesUpdate = new idEventDef("<launchMissiles>", "dddd");
    public static final idEventDef EV_AnimDone             = new idEventDef("<AnimDone>", "d");
    public static final idEventDef EV_StartRagdoll         = new idEventDef("startRagdoll");

    public static class idAnimated extends idAFEntity_Gibbable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idAnimated );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Gibbable.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idAnimated>) idAnimated::Event_Activate);
            eventCallbacks.put(EV_Animated_Start, (eventCallback_t0<idAnimated>) idAnimated::Event_Start);
            eventCallbacks.put(EV_StartRagdoll, (eventCallback_t0<idAnimated>) idAnimated::Event_StartRagdoll);
            eventCallbacks.put(EV_AnimDone, (eventCallback_t1<idAnimated>) idAnimated::Event_AnimDone);
            eventCallbacks.put(EV_Footstep, (eventCallback_t0<idAnimated>) idAnimated::Event_Footstep);
            eventCallbacks.put(EV_FootstepLeft, (eventCallback_t0<idAnimated>) idAnimated::Event_Footstep);
            eventCallbacks.put(EV_FootstepRight, (eventCallback_t0<idAnimated>) idAnimated::Event_Footstep);
            eventCallbacks.put(EV_LaunchMissiles, (eventCallback_t6<idAnimated>) idAnimated::Event_LaunchMissiles);
            eventCallbacks.put(EV_LaunchMissilesUpdate, (eventCallback_t4<idAnimated>) idAnimated::Event_LaunchMissilesUpdate);
        }

        private int                   num_anims;
        private int                   current_anim_index;
        private int                   anim;
        private int                   blendFrames;
        private int/*jointHandle_t*/  soundJoint;
        private final idEntityPtr<idEntity> activator;
        private boolean               activated;
        //
        //

        public idAnimated() {
            this.anim = 0;
            this.blendFrames = 0;
            this.soundJoint = INVALID_JOINT;
            this.activated = false;
            this.combatModel = null;
            this.activator = new idEntityPtr<>();
            this.current_anim_index = 0;
            this.num_anims = 0;

        }
        // ~idAnimated();

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.current_anim_index);
            savefile.WriteInt(this.num_anims);
            savefile.WriteInt(this.anim);
            savefile.WriteInt(this.blendFrames);
            savefile.WriteJoint(this.soundJoint);
            this.activator.Save(savefile);
            savefile.WriteBool(this.activated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] current_anim_index = {0}, num_anims = {0}, anim = {0}, blendFrames = {0}, soundJoint = {0};
            final boolean[] activated = {false};

            savefile.ReadInt(current_anim_index);
            savefile.ReadInt(num_anims);
            savefile.ReadInt(anim);
            savefile.ReadInt(blendFrames);
            savefile.ReadJoint(soundJoint);
            this.activator.Restore(savefile);
            savefile.ReadBool(activated);

            this.current_anim_index = current_anim_index[0];
            this.num_anims = num_anims[0];
            this.anim = anim[0];
            this.blendFrames = blendFrames[0];
            this.soundJoint = soundJoint[0];
            this.activated = activated[0];
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            final String[] animname = new String[1];
            int anim2;
            final float[] wait = {0};
            final String joint;
            final int[] num_anims2 = {0};

            joint = this.spawnArgs.GetString("sound_bone", "origin");
            this.soundJoint = this.animator.GetJointHandle(joint);
            if (this.soundJoint == INVALID_JOINT) {
                gameLocal.Warning("idAnimated '%s' at (%s): cannot find joint '%s' for sound playback", this.name, GetPhysics().GetOrigin().ToString(0), joint);
            }

            LoadAF();

            // allow bullets to collide with a combat model
            if (this.spawnArgs.GetBool("combatModel", "0")) {
                this.combatModel = new idClipModel(this.modelDefHandle);
            }

            // allow the entity to take damage
            if (this.spawnArgs.GetBool("takeDamage", "0")) {
                this.fl.takedamage = true;
            }

            this.blendFrames = 0;

            this.current_anim_index = 0;
            this.spawnArgs.GetInt("num_anims", "0", num_anims2);
            this.num_anims = num_anims2[0];

            this.blendFrames = this.spawnArgs.GetInt("blend_in");

            animname[0] = this.spawnArgs.GetString(this.num_anims != 0 ? "anim1" : "anim");
            if (0 == animname[0].length()) {
                this.anim = 0;
            } else {
                this.anim = this.animator.GetAnim(animname[0]);
                if (0 == this.anim) {
                    idGameLocal.Error("idAnimated '%s' at (%s): cannot find anim '%s'", this.name, GetPhysics().GetOrigin().ToString(0), animname[0]);
                }
            }

            if (this.spawnArgs.GetBool("hide")) {
                Hide();

                if (0 == this.num_anims) {
                    this.blendFrames = 0;
                }
            } else if (this.spawnArgs.GetString("start_anim", "", animname)) {
                anim2 = this.animator.GetAnim(animname[0]);
                if (0 == anim2) {
                    idGameLocal.Error("idAnimated '%s' at (%s): cannot find anim '%s'", this.name, GetPhysics().GetOrigin().ToString(0), animname[0]);
                }
                this.animator.CycleAnim(ANIMCHANNEL_ALL, anim2, gameLocal.time, 0);
            } else if (this.anim != 0) {
                // init joints to the first frame of the animation
                this.animator.SetFrame(ANIMCHANNEL_ALL, this.anim, 1, gameLocal.time, 0);

                if (0 == this.num_anims) {
                    this.blendFrames = 0;
                }
            }

            this.spawnArgs.GetFloat("wait", "-1", wait);

            if (wait[0] >= 0) {
                PostEventSec(EV_Activate, wait[0], this);
            }
        }

        @Override
        public boolean LoadAF() {
            final String[] fileName = new String[1];

            if (!this.spawnArgs.GetString("ragdoll", "*unknown*", fileName)) {
                return false;
            }
            this.af.SetAnimator(GetAnimator());
            return this.af.Load(this, fileName[0]);
        }

        public boolean StartRagdoll() {
            // if no AF loaded
            if (!this.af.IsLoaded()) {
                return false;
            }

            // if the AF is already active
            if (this.af.IsActive()) {
                return true;
            }

            // disable any collision model used
            GetPhysics().DisableClip();

            // start using the AF
            this.af.StartFromCurrentPose(this.spawnArgs.GetInt("velocityTime", "0"));

            return true;
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            this.animator.GetJointTransform(this.soundJoint, gameLocal.time, origin, axis);
            axis.oSet(this.renderEntity.axis);
            return true;
        }

        private void PlayNextAnim() {
            final String[] animName = new String[1];
            int len;
            final int[] cycle = new int[1];

            if (this.current_anim_index >= this.num_anims) {
                Hide();
                if (this.spawnArgs.GetBool("remove")) {
                    PostEventMS(EV_Remove, 0);
                } else {
                    this.current_anim_index = 0;
                }
                return;
            }

            Show();
            this.current_anim_index++;

            this.spawnArgs.GetString(va("anim%d", this.current_anim_index), null, animName);
            if (animName[0].isEmpty()) {
                this.anim = 0;
                this.animator.Clear(ANIMCHANNEL_ALL, gameLocal.time, FRAME2MS(this.blendFrames));
                return;
            }

            this.anim = this.animator.GetAnim(animName[0]);
            if (0 == this.anim) {
                gameLocal.Warning("missing anim '%s' on %s", animName[0], this.name);
                return;
            }

            if (g_debugCinematic.GetBool()) {
                gameLocal.Printf("%d: '%s' start anim '%s'\n", gameLocal.framenum, GetName(), animName[0]);
            }

            this.spawnArgs.GetInt("cycle", "1", cycle);
            if ((this.current_anim_index == this.num_anims) && this.spawnArgs.GetBool("loop_last_anim")) {
                cycle[0] = -1;
            }

            this.animator.CycleAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(this.blendFrames));
            this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(cycle[0]);

            len = this.animator.CurrentAnim(ANIMCHANNEL_ALL).PlayLength();
            if (len >= 0) {
                PostEventMS(EV_AnimDone, len, this.current_anim_index);
            }

            // offset the start time of the shader to sync it to the game time
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            this.animator.ForceUpdate();
            UpdateAnimation();
            UpdateVisuals();
            Present();
        }

        private void Event_Activate(idEventArg<idEntity> _activator) {
            if (this.num_anims != 0) {
                PlayNextAnim();
                this.activator.oSet(_activator.value);
                return;
            }

            if (this.activated) {
                // already activated
                return;
            }

            this.activated = true;
            this.activator.oSet(_activator.value);
            ProcessEvent(EV_Animated_Start);
        }

        private void Event_Start() {
            final int[] cycle = new int[1];
            int len;

            Show();

            if (this.num_anims != 0) {
                PlayNextAnim();
                return;
            }

            if (this.anim != 0) {
                if (g_debugCinematic.GetBool()) {
                    final idAnim animPtr = this.animator.GetAnim(this.anim);
                    gameLocal.Printf("%d: '%s' start anim '%s'\n", gameLocal.framenum, GetName(), animPtr != null ? animPtr.Name() : "");
                }
                this.spawnArgs.GetInt("cycle", "1", cycle);
                this.animator.CycleAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(this.blendFrames));
                this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(cycle[0]);

                len = this.animator.CurrentAnim(ANIMCHANNEL_ALL).PlayLength();
                if (len >= 0) {
                    PostEventMS(EV_AnimDone, len, 1);
                }
            }

            // offset the start time of the shader to sync it to the game time
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            this.animator.ForceUpdate();
            UpdateAnimation();
            UpdateVisuals();
            Present();
        }

        private void Event_StartRagdoll() {
            StartRagdoll();
        }

        private void Event_AnimDone(idEventArg<Integer> animIndex) {
            if (g_debugCinematic.GetBool()) {
                final idAnim animPtr = this.animator.GetAnim(this.anim);
                gameLocal.Printf("%d: '%s' end anim '%s'\n", gameLocal.framenum, GetName(), animPtr != null ? animPtr.Name() : "");
            }

            if ((animIndex.value >= this.num_anims) && this.spawnArgs.GetBool("remove")) {
                Hide();
                PostEventMS(EV_Remove, 0);
            } else if (this.spawnArgs.GetBool("auto_advance")) {
                PlayNextAnim();
            } else {
                this.activated = false;
            }

            ActivateTargets(this.activator.GetEntity());
        }

        private void Event_Footstep() {
            StartSound("snd_footstep", SND_CHANNEL_BODY, 0, false, null);
        }

        private void Event_LaunchMissiles(final idEventArg<String> projectilename, final idEventArg<String> sound, final idEventArg<String> launchjoint,
                                          final idEventArg<String> targetjoint, idEventArg<Integer> numshots, idEventArg<Integer> framedelay) {
            idDict projectileDef;
            int/*jointHandle_t*/ launch;
            int/*jointHandle_t*/ target;

            projectileDef = gameLocal.FindEntityDefDict(projectilename.value, false);
            if (null == projectileDef) {
                gameLocal.Warning("idAnimated '%s' at (%s): unknown projectile '%s'", this.name, GetPhysics().GetOrigin().ToString(0), projectilename.value);
                return;
            }

            launch = this.animator.GetJointHandle(launchjoint.value);
            if (launch == INVALID_JOINT) {
                gameLocal.Warning("idAnimated '%s' at (%s): unknown launch joint '%s'", this.name, GetPhysics().GetOrigin().ToString(0), launchjoint.value);
                idGameLocal.Error("Unknown joint '%s'", launchjoint.value);
            }

            target = this.animator.GetJointHandle(targetjoint.value);
            if (target == INVALID_JOINT) {
                gameLocal.Warning("idAnimated '%s' at (%s): unknown target joint '%s'", this.name, GetPhysics().GetOrigin().ToString(0), targetjoint.value);
            }

            this.spawnArgs.Set("projectilename", projectilename.value);
            this.spawnArgs.Set("missilesound", sound.value);

            CancelEvents(EV_LaunchMissilesUpdate);
            ProcessEvent(EV_LaunchMissilesUpdate, launch, target, numshots.value - 1, framedelay.value);
        }

        private void Event_LaunchMissilesUpdate(idEventArg<Integer> launchjoint, idEventArg<Integer> targetjoint, idEventArg<Integer> numshots, idEventArg<Integer> framedelay) {
            idVec3 launchPos = new idVec3();
            idVec3 targetPos = new idVec3();
            final idMat3 axis = new idMat3();
            idVec3 dir;
            final idEntity[] ent = {null};
            idProjectile projectile;
            idDict projectileDef;
            String projectilename;

            projectilename = this.spawnArgs.GetString("projectilename");
            projectileDef = gameLocal.FindEntityDefDict(projectilename, false);
            if (null == projectileDef) {
                gameLocal.Warning("idAnimated '%s' at (%s): 'launchMissiles' called with unknown projectile '%s'", this.name, GetPhysics().GetOrigin().ToString(0), projectilename);
                return;
            }

            StartSound("snd_missile", SND_CHANNEL_WEAPON, 0, false, null);

            this.animator.GetJointTransform(launchjoint.value, gameLocal.time, launchPos, axis);
            launchPos = this.renderEntity.origin.oPlus(launchPos.oMultiply(this.renderEntity.axis));

            this.animator.GetJointTransform(targetjoint.value, gameLocal.time, targetPos, axis);
            targetPos = this.renderEntity.origin.oPlus(targetPos.oMultiply(this.renderEntity.axis));

            dir = targetPos.oMinus(launchPos);
            dir.Normalize();

            gameLocal.SpawnEntityDef(projectileDef, ent, false);
            if ((null == ent[0]) || !ent[0].IsType(idProjectile.class)) {
                idGameLocal.Error("idAnimated '%s' at (%s): in 'launchMissiles' call '%s' is not an idProjectile", this.name, GetPhysics().GetOrigin().ToString(0), projectilename);
            }
            projectile = (idProjectile) ent[0];
            projectile.Create(this, launchPos, dir);
            projectile.Launch(launchPos, dir, getVec3_origin());

            if (numshots.value > 0) {
                PostEventMS(EV_LaunchMissilesUpdate, FRAME2MS(framedelay.value), launchjoint.value, targetjoint.value, numshots.value - 1, framedelay.value);
            }
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

     idStaticEntity

     Some static entities may be optimized into inline geometry by dmap

     ===============================================================================
     */
    public static class idStaticEntity extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idStaticEntity );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idStaticEntity>) idStaticEntity::Event_Activate);
        }

        private int     spawnTime;
        private boolean active;
        private final idVec4  fadeFrom;
        private final idVec4  fadeTo;
        private int     fadeStart;
        private int     fadeEnd;
        private boolean runGui;
        //
        //

        public idStaticEntity() {
            this.spawnTime = 0;
            this.active = false;
            this.fadeFrom = new idVec4(1, 1, 1, 1);
            this.fadeTo = new idVec4(1, 1, 1, 1);
            this.fadeStart = 0;
            this.fadeEnd = 0;
            this.runGui = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.spawnTime);
            savefile.WriteBool(this.active);
            savefile.WriteVec4(this.fadeFrom);
            savefile.WriteVec4(this.fadeTo);
            savefile.WriteInt(this.fadeStart);
            savefile.WriteInt(this.fadeEnd);
            savefile.WriteBool(this.runGui);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] spawnTime = {0}, fadeStart = {0}, fadeEnd = {0};//TODO:make sure the dumbass compiler doesn't decide that all {0}'s are the same
            final boolean[] active = {false}, runGui = {false};

            savefile.ReadInt(spawnTime);
            savefile.ReadBool(active);
            savefile.ReadVec4(this.fadeFrom);
            savefile.ReadVec4(this.fadeTo);
            savefile.ReadInt(fadeStart);
            savefile.ReadInt(fadeEnd);
            savefile.ReadBool(runGui);

            this.spawnTime = spawnTime[0];
            this.fadeStart = fadeStart[0];
            this.fadeEnd = fadeEnd[0];
            this.active = active[0];
            this.runGui = runGui[0];
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            boolean solid;
            boolean hidden;

            // an inline static model will not do anything at all
            if (this.spawnArgs.GetBool("inline") || gameLocal.world.spawnArgs.GetBool("inlineAllStatics")) {
                Hide();
                return;
            }

            solid = this.spawnArgs.GetBool("solid");
            hidden = this.spawnArgs.GetBool("hide");

            if (solid && !hidden) {
                GetPhysics().SetContents(CONTENTS_SOLID);
            } else {
                GetPhysics().SetContents(0);
            }

            this.spawnTime = gameLocal.time;
            this.active = false;

            final idStr model = new idStr(this.spawnArgs.GetString("model"));
            if (model.Find(".prt") >= 0) {
                // we want the parametric particles out of sync with each other
                this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = gameLocal.random.RandomInt(32767);
            }

            this.fadeFrom.Set(1, 1, 1, 1);
            this.fadeTo.Set(1, 1, 1, 1);
            this.fadeStart = 0;
            this.fadeEnd = 0;

            // NOTE: this should be used very rarely because it is expensive
            this.runGui = this.spawnArgs.GetBool("runGui");
            if (this.runGui) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void ShowEditingDialog() {
            common.InitTool(EDITOR_PARTICLE, this.spawnArgs);
        }

        @Override
        public void Hide() {
            super.Hide();
            GetPhysics().SetContents(0);
        }

        @Override
        public void Show() {
            super.Show();
            if (this.spawnArgs.GetBool("solid")) {
                GetPhysics().SetContents(CONTENTS_SOLID);
            }
        }

        public void Fade(final idVec4 to, float fadeTime) {
            GetColor(this.fadeFrom);
            this.fadeTo.oSet(to);
            this.fadeStart = gameLocal.time;
            this.fadeEnd = (int) (gameLocal.time + SEC2MS(fadeTime));
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            super.Think();
            if ((this.thinkFlags & TH_THINK) != 0) {
                if (this.runGui && (this.renderEntity.gui[0] != null)) {
                    final idPlayer player = gameLocal.GetLocalPlayer();
                    if (player != null) {
                        if (!player.objectiveSystemOpen) {
                            this.renderEntity.gui[0].StateChanged(gameLocal.time, true);
                            if (this.renderEntity.gui[1] != null) {
                                this.renderEntity.gui[1].StateChanged(gameLocal.time, true);
                            }
                            if (this.renderEntity.gui[2] != null) {
                                this.renderEntity.gui[2].StateChanged(gameLocal.time, true);
                            }
                        }
                    }
                }
                if (this.fadeEnd > 0) {
                    idVec4 color = new idVec4();
                    if (gameLocal.time < this.fadeEnd) {
                        color.Lerp(this.fadeFrom, this.fadeTo, (float) (gameLocal.time - this.fadeStart) / (float) (this.fadeEnd - this.fadeStart));
                    } else {
                        color = this.fadeTo;
                        this.fadeEnd = 0;
                        BecomeInactive(TH_THINK);
                    }
                    SetColor(color);
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            GetPhysics().WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
            WriteColorToSnapshot(msg);
            WriteGUIToSnapshot(msg);
            msg.WriteBits(IsHidden() ? 1 : 0, 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            boolean hidden;

            GetPhysics().ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);
            ReadColorFromSnapshot(msg);
            ReadGUIFromSnapshot(msg);
            hidden = msg.ReadBits(1) == 1;
            if (hidden != IsHidden()) {
                if (hidden) {
                    Hide();
                } else {
                    Show();
                }
            }
            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            final idStr activateGui;

            this.spawnTime = gameLocal.time;
            this.active = !this.active;

            final idKeyValue kv = this.spawnArgs.FindKey("hide");
            if (kv != null) {
                if (IsHidden()) {
                    Show();
                } else {
                    Hide();
                }
            }

            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(this.spawnTime);
            this.renderEntity.shaderParms[5] = this.active ? 1 : 0;
            // this change should be a good thing, it will automatically turn on 
            // lights etc.. when triggered so that does not have to be specifically done
            // with trigger parms.. it MIGHT break things so need to keep an eye on it
            this.renderEntity.shaderParms[ SHADERPARM_MODE] = (this.renderEntity.shaderParms[ SHADERPARM_MODE] != 0) ? 0.0f : 1.0f;
            BecomeActive(TH_UPDATEVISUALS);
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

    /*
     ===============================================================================

     idFuncEmitter

     ===============================================================================
     */
    public static class idFuncEmitter extends idStaticEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idFuncEmitter );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idStaticEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncEmitter>) idFuncEmitter::Event_Activate);
        }

        private final boolean[] hidden = {false};
        //
        //

        public idFuncEmitter() {
            this.hidden[0] = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.hidden[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadBool(this.hidden);
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            if (this.spawnArgs.GetBool("start_off")) {
                this.hidden[0] = true;
                this.renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] = MS2SEC(1);
                UpdateVisuals();
            } else {
                this.hidden[0] = false;
            }
        }

        public void Event_Activate(idEventArg<idEntity> activator) {
            if (this.hidden[0] || this.spawnArgs.GetBool("cycleTrigger")) {
                this.renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] = 0;
                this.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                this.hidden[0] = false;
            } else {
                this.renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] = MS2SEC(gameLocal.time);
                this.hidden[0] = true;
            }
            UpdateVisuals();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(this.hidden[0] ? 1 : 0, 1);
            msg.WriteFloat(this.renderEntity.shaderParms[ SHADERPARM_PARTICLE_STOPTIME]);
            msg.WriteFloat(this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET]);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.hidden[0] = msg.ReadBits(1) != 0;
            this.renderEntity.shaderParms[ SHADERPARM_PARTICLE_STOPTIME] = msg.ReadFloat();
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = msg.ReadFloat();
            if (msg.HasChanged()) {
                UpdateVisuals();
            }
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

     idFuncSmoke

     ===============================================================================
     */
    public static class idFuncSmoke extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idFuncSmoke );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncSmoke>) idFuncSmoke::Event_Activate);
        }

        private int smokeTime;
        private idDeclParticle smoke;
        private boolean restart;
        //
        //

        public idFuncSmoke() {
            this.smokeTime = 0;
            this.smoke = null;
            this.restart = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            final String smokeName = this.spawnArgs.GetString("smoke");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                this.smoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            } else {
                this.smoke = null;
            }
            if (this.spawnArgs.GetBool("start_off")) {
                this.smokeTime = 0;
                this.restart = false;
            } else if (this.smoke != null) {
                this.smokeTime = gameLocal.time;
                BecomeActive(TH_UPDATEPARTICLES);
                this.restart = true;
            }
            GetPhysics().SetContents(0);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.smokeTime);
            savefile.WriteParticle(this.smoke);
            savefile.WriteBool(this.restart);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] smokeTime = {0};
            final boolean[] restart = {false};

            savefile.ReadInt(smokeTime);
            savefile.ReadParticle(this.smoke);
            savefile.ReadBool(restart);

            this.smokeTime = smokeTime[0];
            this.restart = restart[0];
        }

        @Override
        public void Think() {

            // if we are completely closed off from the player, don't do anything at all
            if (CheckDormant() || (this.smoke == null) || (this.smokeTime == -1)) {
                return;
            }

            if (((this.thinkFlags & TH_UPDATEPARTICLES) != 0) && !IsHidden()) {
                if (!gameLocal.smokeParticles.EmitSmoke(this.smoke, this.smokeTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis())) {
                    if (this.restart) {
                        this.smokeTime = gameLocal.time;
                    } else {
                        this.smokeTime = 0;
                        BecomeInactive(TH_UPDATEPARTICLES);
                    }
                }
            }

        }

        public void Event_Activate(idEventArg<idEntity> activator) {
            if ((this.thinkFlags & TH_UPDATEPARTICLES) != 0) {
                this.restart = false;
//                return;
            } else {
                BecomeActive(TH_UPDATEPARTICLES);
                this.restart = true;
                this.smokeTime = gameLocal.time;
            }
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

    /*
     ===============================================================================

     idFuncSplat

     ===============================================================================
     */
    public static final idEventDef EV_Splat = new idEventDef("<Splat>");

    public static class idFuncSplat extends idFuncEmitter {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idFuncSplat );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idFuncEmitter.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncSplat>) idFuncSplat::Event_Activate);
            eventCallbacks.put(EV_Splat, (eventCallback_t0<idFuncSplat>) idFuncSplat::Event_Splat);
        }

        public idFuncSplat() {
        }

        @Override
        public void Spawn() {
        }

        @Override
        public void Event_Activate(idEventArg<idEntity> activator) {
            super.Event_Activate(activator);
            PostEventSec(EV_Splat, this.spawnArgs.GetFloat("splatDelay", "0.25"));
            StartSound("snd_spurt", SND_CHANNEL_ANY, 0, false, null);
        }

        private void Event_Splat() {
            String splat;
            final int count = this.spawnArgs.GetInt("splatCount", "1");
            for (int i = 0; i < count; i++) {
                splat = this.spawnArgs.RandomPrefix("mtr_splat", gameLocal.random);
                if ((splat != null) && !splat.isEmpty()) {
                    final float size = this.spawnArgs.GetFloat("splatSize", "128");
                    final float dist = this.spawnArgs.GetFloat("splatDistance", "128");
                    final float angle = this.spawnArgs.GetFloat("splatAngle", "0");
                    gameLocal.ProjectDecal(GetPhysics().GetOrigin(), GetPhysics().GetAxis().oGet(2), dist, true, size, splat, angle);
                }
            }
            StartSound("snd_splat", SND_CHANNEL_ANY, 0, false, null);
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

     idTextEntity

     ===============================================================================
     */
    public static class idTextEntity extends idEntity {
        // CLASS_PROTOTYPE( idTextEntity );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private idStr text;
        private boolean playerOriented;
        //
        //

        @Override
        public void Spawn() {
            // these are cached as the are used each frame
            this.text.oSet(this.spawnArgs.GetString("text"));
            this.playerOriented = this.spawnArgs.GetBool("playerOriented");
            final boolean force = this.spawnArgs.GetBool("force");
            if (developer.GetBool() || force) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteString(this.text);
            savefile.WriteBool(this.playerOriented);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] playerOriented = {false};

            savefile.ReadString(this.text);
            savefile.ReadBool(playerOriented);

            this.playerOriented = playerOriented[0];
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                gameRenderWorld.DrawText(this.text.getData(), GetPhysics().GetOrigin(), 0.25f, colorWhite, this.playerOriented ? gameLocal.GetLocalPlayer().viewAngles.ToMat3() : GetPhysics().GetAxis().Transpose(), 1);
                for (int i = 0; i < this.targets.Num(); i++) {
                    if (this.targets.oGet(i).GetEntity() != null) {
                        gameRenderWorld.DebugArrow(colorBlue, GetPhysics().GetOrigin(), this.targets.oGet(i).GetEntity().GetPhysics().GetOrigin(), 1);
                    }
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idLocationEntity

     ===============================================================================
     */
    public static class idLocationEntity extends idEntity {
        // CLASS_PROTOTYPE( idLocationEntity );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void Spawn() {
            super.Spawn();
            
            final String[] realName = new String[1];

            // this just holds dict information
            // if "location" not already set, use the entity name.
            if (!this.spawnArgs.GetString("location", "", realName)) {
                this.spawnArgs.Set("location", this.name);
            }
        }

        public String GetLocation() {
            return this.spawnArgs.GetString("location");
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idLocationSeparatorEntity

     ===============================================================================
     */
    public static class idLocationSeparatorEntity extends idEntity {
        // CLASS_PROTOTYPE( idLocationSeparatorEntity );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void Spawn() {
            super.Spawn();
            
            idBounds b;

            b = new idBounds(this.spawnArgs.GetVector("origin")).Expand(16);
            final int/*qhandle_t*/ portal = gameRenderWorld.FindPortal(b);
            if (0 == portal) {
                gameLocal.Warning("LocationSeparator '%s' didn't contact a portal", this.spawnArgs.GetString("name"));
            }
            gameLocal.SetPortalState(portal, etoi(PS_BLOCK_LOCATION));
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idVacuumSeperatorEntity

     Can be triggered to let vacuum through a portal (blown out window)

     ===============================================================================
     */
    public static class idVacuumSeparatorEntity extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idVacuumSeparatorEntity );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idVacuumSeparatorEntity>) idVacuumSeparatorEntity::Event_Activate);
        }

        public idVacuumSeparatorEntity() {
            this.portal = 0;
        }

        @Override
        public void Spawn() {
            idBounds b;

            b = new idBounds(this.spawnArgs.GetVector("origin")).Expand(16);
            this.portal = gameRenderWorld.FindPortal(b);
            if (0 == this.portal) {
                gameLocal.Warning("VacuumSeparator '%s' didn't contact a portal", this.spawnArgs.GetString("name"));
                return;
            }
            gameLocal.SetPortalState(this.portal, (etoi(PS_BLOCK_AIR) | etoi(PS_BLOCK_LOCATION)));
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.portal);
            savefile.WriteInt(gameRenderWorld.GetPortalState(this.portal));
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] state = {0}, portal = {0};

            savefile.ReadInt(portal);
            savefile.ReadInt(state);

            this.portal = portal[0];
            gameLocal.SetPortalState(portal[0], state[0]);
        }

        public void Event_Activate(idEventArg<idEntity> activator) {
            if (0 == this.portal) {
                return;
            }
            gameLocal.SetPortalState(this.portal, etoi(PS_BLOCK_NONE));
        }
//
//
        private int/*qhandle_t*/ portal;

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

    /*
     ===============================================================================

     idVacuumEntity

     Levels should only have a single vacuum entity.

     ===============================================================================
     */
    public static class idVacuumEntity extends idEntity {
// public:
        // CLASS_PROTOTYPE( idVacuumEntity );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void Spawn() {
            super.Spawn();

            if (gameLocal.vacuumAreaNum != -1) {
                gameLocal.Warning("idVacuumEntity::Spawn: multiple idVacuumEntity in level");
                return;
            }

            final idVec3 org = this.spawnArgs.GetVector("origin");

            gameLocal.vacuumAreaNum = gameRenderWorld.PointInArea(org);
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idBeam

     ===============================================================================
     */
    public static class idBeam extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idBeam );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_PostSpawn, (eventCallback_t0<idBeam>) idBeam::Event_MatchTarget);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idBeam>) idBeam::Event_Activate);
        }

        private final idEntityPtr<idBeam> target;
        private final idEntityPtr<idBeam> master;
        //
        //

        public idBeam() {
            this.target = new idEntityPtr<>();
            this.master = new idEntityPtr<>();
        }

        @Override
        public void Spawn() {
            super.Spawn();

            final float[] width = new float[1];

            if (this.spawnArgs.GetFloat("width", "0", width)) {
                this.renderEntity.shaderParms[ SHADERPARM_BEAM_WIDTH] = width[0];
            }

            SetModel("_BEAM");
            Hide();
            PostEventMS(EV_PostSpawn, 0);
        }

        @Override
        public void Save(idSaveGame savefile) {
            this.target.Save(savefile);
            this.master.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.target.Restore(savefile);
            this.master.Restore(savefile);
        }

        @Override
        public void Think() {
            idBeam masterEnt;

            if (!IsHidden() && (null == this.target.GetEntity())) {
                // hide if our target is removed
                Hide();
            }

            RunPhysics();

            masterEnt = this.master.GetEntity();
            if (masterEnt != null) {
                final idVec3 origin = GetPhysics().GetOrigin();
                masterEnt.SetBeamTarget(origin);
            }
            Present();
        }

        public void SetMaster(idBeam masterbeam) {
            this.master.oSet(masterbeam);
        }

        public void SetBeamTarget(final idVec3 origin) {
            if ((this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_X] != origin.x) || (this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_Y] != origin.y) || (this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_Z] != origin.z)) {
                this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_X] = origin.x;
                this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_Y] = origin.y;
                this.renderEntity.shaderParms[ SHADERPARM_BEAM_END_Z] = origin.z;
                UpdateVisuals();
            }
        }

        @Override
        public void Show() {
            idBeam targetEnt;

            super.Show();

            targetEnt = this.target.GetEntity();
            if (targetEnt != null) {
                final idVec3 origin = targetEnt.GetPhysics().GetOrigin();
                SetBeamTarget(origin);
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            GetPhysics().WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
            WriteColorToSnapshot(msg);
            msg.WriteFloat(this.renderEntity.shaderParms[SHADERPARM_BEAM_END_X]);
            msg.WriteFloat(this.renderEntity.shaderParms[SHADERPARM_BEAM_END_Y]);
            msg.WriteFloat(this.renderEntity.shaderParms[SHADERPARM_BEAM_END_Z]);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            GetPhysics().ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);
            ReadColorFromSnapshot(msg);
            this.renderEntity.shaderParms[SHADERPARM_BEAM_END_X] = msg.ReadFloat();
            this.renderEntity.shaderParms[SHADERPARM_BEAM_END_Y] = msg.ReadFloat();
            this.renderEntity.shaderParms[SHADERPARM_BEAM_END_Z] = msg.ReadFloat();
            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        private void Event_MatchTarget() {
            int i;
            idEntity targetEnt;
            idBeam targetBeam;

            if (0 == this.targets.Num()) {
                return;
            }

            targetBeam = null;
            for (i = 0; i < this.targets.Num(); i++) {
                targetEnt = this.targets.oGet(i).GetEntity();
                if ((targetEnt != null) && targetEnt.IsType(idBeam.class)) {
                    targetBeam = (idBeam) targetEnt;
                    break;
                }
            }

            if (null == targetBeam) {
                idGameLocal.Error("Could not find valid beam target for '%s'", this.name);
            }

            this.target.oSet(targetBeam);
            targetBeam.SetMaster(this);
            if (!this.spawnArgs.GetBool("start_off")) {
                Show();
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            if (IsHidden()) {
                Show();
            } else {
                Hide();
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idLiquid

     ===============================================================================
     */
    @Deprecated
    public static class idLiquid extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idLiquid );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idLiquid>) idLiquid::Event_Touch);
        }

        private idRenderModelLiquid model;
        //
        //

        @Override
        public void Spawn() {
            /*
             model = dynamic_cast<idRenderModelLiquid *>( renderEntity.hModel );
             if ( !model ) {
             gameLocal.Error( "Entity '%s' must have liquid model", name.c_str() );
             }
             model->Reset();
             GetPhysics()->SetContents( CONTENTS_TRIGGER );
             */
        }

        @Override
        public void Save(idSaveGame savefile) {
            // Nothing to save
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            //FIXME: NO!
            Spawn();
        }

        private void Event_Touch(idEventArg<idEntity> other, idEventArg<trace_s> trace) {
            // FIXME: for QuakeCon
/*
             idVec3 pos;

             pos = other->GetPhysics()->GetOrigin() - GetPhysics()->GetOrigin();
             model->IntersectBounds( other->GetPhysics()->GetBounds().Translate( pos ), -10.0f );
             */
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

    /*
     ===============================================================================

     idShaking

     ===============================================================================
     */
    public static class idShaking extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idShaking );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idShaking>) idShaking::Event_Activate);
        }

        private final idPhysics_Parametric physicsObj;
        private boolean active;
        //
        //

        public idShaking() {
            this.physicsObj = new idPhysics_Parametric();
            this.active = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            SetPhysics(this.physicsObj);

            this.active = false;
            if (!this.spawnArgs.GetBool("start_off")) {
                BeginShaking();
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.active);
            savefile.WriteStaticObject(this.physicsObj);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] active = {false};

            savefile.ReadBool(active);
            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            this.active = active[0];
        }

        private void BeginShaking() {
            int phase;
            idAngles shake;
            int period;

            this.active = true;
            phase = gameLocal.random.RandomInt(1000);
            shake = this.spawnArgs.GetAngles("shake", "0.5 0.5 0.5");
            period = (int) (this.spawnArgs.GetFloat("period", "0.05") * 1000);
            this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), phase, (int) (period * 0.25f), GetPhysics().GetAxis().ToAngles(), shake, getAng_zero());
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            if (!this.active) {
                BeginShaking();
            } else {
                this.active = false;
                this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.physicsObj.GetAxis().ToAngles(), getAng_zero(), getAng_zero());
            }
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

    /*
     ===============================================================================

     idEarthQuake

     ===============================================================================
     */
    public static class idEarthQuake extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = -5892291559148660706L;
		// CLASS_PROTOTYPE( idEarthQuake );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idEarthQuake>) idEarthQuake::Event_Activate);
        }

        private int     nextTriggerTime;
        private int     shakeStopTime;
        private float   wait;
        private float   random;
        private boolean triggered;
        private boolean playerOriented;
        private boolean disabled;
        private float   shakeTime;
        //
        //

        public idEarthQuake() {
            this.wait = 0.0f;
            this.random = 0.0f;
            this.nextTriggerTime = 0;
            this.shakeStopTime = 0;
            this.triggered = false;
            this.playerOriented = false;
            this.disabled = false;
            this.shakeTime = 0.0f;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.nextTriggerTime = 0;
            this.shakeStopTime = 0;
            this.wait = this.spawnArgs.GetFloat("wait", "15");
            this.random = this.spawnArgs.GetFloat("random", "5");
            this.triggered = this.spawnArgs.GetBool("triggered");
            this.playerOriented = this.spawnArgs.GetBool("playerOriented");
            this.disabled = false;
            this.shakeTime = this.spawnArgs.GetFloat("shakeTime", "0");

            if (!this.triggered) {
                PostEventSec(EV_Activate, this.spawnArgs.GetFloat("wait"), this);
            }
            BecomeInactive(TH_THINK);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.nextTriggerTime);
            savefile.WriteInt(this.shakeStopTime);
            savefile.WriteFloat(this.wait);
            savefile.WriteFloat(this.random);
            savefile.WriteBool(this.triggered);
            savefile.WriteBool(this.playerOriented);
            savefile.WriteBool(this.disabled);
            savefile.WriteFloat(this.shakeTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final int[] nextTriggerTime = {0}, shakeStopTime = {0};
            final float[] wait = {0}, random = {0}, shakeTime = {0};
            final boolean[] triggered = {false}, playerOriented = {false}, disabled = {false};

            savefile.ReadInt(nextTriggerTime);
            savefile.ReadInt(shakeStopTime);
            savefile.ReadFloat(wait);
            savefile.ReadFloat(random);
            savefile.ReadBool(triggered);
            savefile.ReadBool(playerOriented);
            savefile.ReadBool(disabled);
            savefile.ReadFloat(shakeTime);

            this.nextTriggerTime = nextTriggerTime[0];
            this.shakeStopTime = shakeStopTime[0];
            this.wait = wait[0];
            this.random = random[0];
            this.triggered = triggered[0];
            this.playerOriented = playerOriented[0];
            this.disabled = disabled[0];
            this.shakeTime = shakeTime[0];

            if (shakeStopTime[0] > gameLocal.time) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Think() {
        }

        private void Event_Activate(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;

            if (this.nextTriggerTime > gameLocal.time) {
                return;
            }

            if (this.disabled && (activator == this)) {
                return;
            }

            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player == null) {
                return;
            }

            this.nextTriggerTime = 0;

            if (!this.triggered && (activator != this)) {
                // if we are not triggered ( i.e. random ), disable or enable
                this.disabled ^= true;//1;
                if (this.disabled) {
                    return;
                } else {
                    PostEventSec(EV_Activate, this.wait + (this.random * gameLocal.random.CRandomFloat()), this);
                }
            }

            ActivateTargets(activator);

            final idSoundShader shader = declManager.FindSound(this.spawnArgs.GetString("snd_quake"));
            if (this.playerOriented) {
                player.StartSoundShader(shader, SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
            } else {
                StartSoundShader(shader, SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
            }

            if (this.shakeTime > 0.0f) {
                this.shakeStopTime = (int) (gameLocal.time + SEC2MS(this.shakeTime));
                BecomeActive(TH_THINK);
            }

            if (this.wait > 0.0f) {
                if (!this.triggered) {
                    PostEventSec(EV_Activate, this.wait + (this.random * gameLocal.random.CRandomFloat()), this);
                } else {
                    this.nextTriggerTime = (int) (gameLocal.time + SEC2MS(this.wait + (this.random * gameLocal.random.CRandomFloat())));
                }
            } else if (this.shakeTime == 0.0f) {
                PostEventMS(EV_Remove, 0);
            }
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


    /*
     ===============================================================================

     idFuncPortal

     ===============================================================================
     */
    public static class idFuncPortal extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 935062458871120297L;
		// CLASS_PROTOTYPE( idFuncPortal );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncPortal>) idFuncPortal::Event_Activate);
        }

        private final int[]/*qhandle_t*/ portal = {0};
        private final boolean[] state = {false};
        //
        //

        public idFuncPortal() {
            this.portal[0] = 0;
            this.state[0] = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.portal[0] = gameRenderWorld.FindPortal(GetPhysics().GetAbsBounds().Expand(32.0f));
            if (this.portal[0] > 0) {
                this.state[0] = this.spawnArgs.GetBool("start_on");
                gameLocal.SetPortalState(this.portal[0], (this.state[0] ? PS_BLOCK_ALL : PS_BLOCK_NONE).ordinal());
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.portal[0]);
            savefile.WriteBool(this.state[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadInt(this.portal);
            savefile.ReadBool(this.state);
            gameLocal.SetPortalState(this.portal[0], (this.state[0] ? PS_BLOCK_ALL : PS_BLOCK_NONE).ordinal());
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            if (this.portal[0] > 0) {
                this.state[0] = !this.state[0];
                gameLocal.SetPortalState(this.portal[0], (this.state[0] ? PS_BLOCK_ALL : PS_BLOCK_NONE).ordinal());
            }
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

    /*
     ===============================================================================

     idFuncAASPortal

     ===============================================================================
     */
    public static class idFuncAASPortal extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 2352468639457188273L;
		// CLASS_PROTOTYPE( idFuncAASPortal );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncAASPortal>) idFuncAASPortal::Event_Activate);
        }

        private boolean state;
        //
        //

        public idFuncAASPortal() {
            this.state = false;
        }

        @Override
        public void Spawn() {
            this.state = this.spawnArgs.GetBool("start_on");
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_CLUSTERPORTAL, this.state);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.state);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] state = {false};
            savefile.ReadBool(state);
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_CLUSTERPORTAL, this.state = state[0]);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            this.state ^= true;//1;
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_CLUSTERPORTAL, this.state);
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

    /*
     ===============================================================================

     idFuncAASObstacle

     ===============================================================================
     */
    public static class idFuncAASObstacle extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 2338709258716423473L;
		// CLASS_PROTOTYPE( idFuncAASObstacle );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncAASObstacle>) idFuncAASObstacle::Event_Activate);
        }

        private final boolean[] state = {false};
        //
        //

        public idFuncAASObstacle() {
            this.state[0] = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.state[0] = this.spawnArgs.GetBool("start_on");
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_OBSTACLE, this.state[0]);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.state[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadBool(this.state);
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_OBSTACLE, this.state[0]);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            this.state[0] ^= true;//1;
            gameLocal.SetAASAreaState(GetPhysics().GetAbsBounds(), AREACONTENTS_OBSTACLE, this.state[0]);
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
    /*
     ===============================================================================

     idFuncRadioChatter

     ===============================================================================
     */
    public static final idEventDef EV_ResetRadioHud = new idEventDef("<resetradiohud>", "e");

    public static class idFuncRadioChatter extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = -8168694844528207578L;
		// CLASS_PROTOTYPE( idFuncRadioChatter );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idFuncRadioChatter>) idFuncRadioChatter::Event_Activate);
            eventCallbacks.put(EV_ResetRadioHud, (eventCallback_t1<idFuncRadioChatter>) idFuncRadioChatter::Event_ResetRadioHud);
        }

        private float time;
        //
        //

        public idFuncRadioChatter() {
            this.time = 0;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.time = this.spawnArgs.GetFloat("time", "5.0");
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.time);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final float[] time = {0};

            savefile.ReadFloat(time);

            this.time = time[0];
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            idPlayer player;
            final String sound;
            idSoundShader shader;
            final int[] length = {0};

            if (activator.value.IsType(idPlayer.class)) {
                player = (idPlayer) activator.value;
            } else {
                player = gameLocal.GetLocalPlayer();
            }

            player.hud.HandleNamedEvent("radioChatterUp");

            sound = this.spawnArgs.GetString("snd_radiochatter", "");
            if ((sound != null) && !sound.isEmpty()) {
                shader = declManager.FindSound(sound);
                player.StartSoundShader(shader, SND_CHANNEL_RADIO, SSF_GLOBAL, false, length);
                this.time = MS2SEC(length[0] + 150);
            }
            // we still put the hud up because this is used with no sound on 
            // certain frame commands when the chatter is triggered
            PostEventSec(EV_ResetRadioHud, this.time, player);

        }

        private void Event_ResetRadioHud(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;
            final idPlayer player = (activator.IsType(idPlayer.class)) ? (idPlayer) activator : gameLocal.GetLocalPlayer();
            player.hud.HandleNamedEvent("radioChatterDown");
            ActivateTargets(activator);
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


    /*
     ===============================================================================

     idPhantomObjects

     ===============================================================================
     */
    public static class idPhantomObjects extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = -3576149907834991213L;
		// CLASS_PROTOTYPE( idPhantomObjects );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idPhantomObjects>) idPhantomObjects::Event_Activate);
        }

        private int                  end_time;
        private float                throw_time;
        private float                shake_time;
        private idVec3               shake_ang;
        private float                speed;
        private int                  min_wait;
        private int                  max_wait;
        private final idEntityPtr<idActor> target;
        private final idList<Integer>      targetTime;
        private final idList<idVec3>       lastTargetPos;
        //
        //

        public idPhantomObjects() {
            this.target = null;
            this.end_time = 0;
            this.throw_time = 0.0f;
            this.shake_time = 0.0f;
            this.shake_ang = new idVec3();
            this.speed = 0.0f;
            this.min_wait = 0;
            this.max_wait = 0;
            this.fl.neverDormant = false;
            this.targetTime = new idList<>();
            this.lastTargetPos = new idList<>();
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.throw_time = this.spawnArgs.GetFloat("time", "5");
            this.speed = this.spawnArgs.GetFloat("speed", "1200");
            this.shake_time = this.spawnArgs.GetFloat("shake_time", "1");
            this.throw_time -= this.shake_time;
            if (this.throw_time < 0.0f) {
                this.throw_time = 0.0f;
            }
            this.min_wait = (int) SEC2MS(this.spawnArgs.GetFloat("min_wait", "1"));
            this.max_wait = (int) SEC2MS(this.spawnArgs.GetFloat("max_wait", "3"));

            this.shake_ang = this.spawnArgs.GetVector("shake_ang", "65 65 65");
            Hide();
            GetPhysics().SetContents(0);
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(this.end_time);
            savefile.WriteFloat(this.throw_time);
            savefile.WriteFloat(this.shake_time);
            savefile.WriteVec3(this.shake_ang);
            savefile.WriteFloat(this.speed);
            savefile.WriteInt(this.min_wait);
            savefile.WriteInt(this.max_wait);
            this.target.Save(savefile);
            savefile.WriteInt(this.targetTime.Num());
            for (i = 0; i < this.targetTime.Num(); i++) {
                savefile.WriteInt(this.targetTime.oGet(i));
            }

            for (i = 0; i < this.lastTargetPos.Num(); i++) {
                savefile.WriteVec3(this.lastTargetPos.oGet(i));
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int num;
            int i;

            this.end_time = savefile.ReadInt();
            this.throw_time = savefile.ReadFloat();
            this.shake_time = savefile.ReadFloat();
            savefile.ReadVec3(this.shake_ang);
            this.speed = savefile.ReadFloat();
            this.min_wait = savefile.ReadInt();
            this.max_wait = savefile.ReadInt();
            this.target.Restore(savefile);

            num = savefile.ReadInt();
            this.targetTime.SetGranularity(1);
            this.targetTime.SetNum(num);
            this.lastTargetPos.SetGranularity(1);
            this.lastTargetPos.SetNum(num);

            for (i = 0; i < num; i++) {
                this.targetTime.oSet(i, savefile.ReadInt());
            }

            if (savefile.GetBuildNumber() == INITIAL_RELEASE_BUILD_NUMBER) {
                // these weren't saved out in the first release
                for (i = 0; i < num; i++) {
                    this.lastTargetPos.oGet(i).Zero();
                }
            } else {
                for (i = 0; i < num; i++) {
                    savefile.ReadVec3(this.lastTargetPos.oGet(i));
                }
            }
        }

        @Override
        public void Think() {
            int i;
            int num;
            float time;
            final idVec3 vel = new idVec3();
            final idVec3 ang = new idVec3();
            idEntity ent;
            idActor targetEnt;
            idPhysics entPhys;
            final trace_s[] tr = {null};

            // if we are completely closed off from the player, don't do anything at all
            if (CheckDormant()) {
                return;
            }

            if (0 == (this.thinkFlags & TH_THINK)) {
                BecomeInactive(this.thinkFlags & ~TH_THINK);
                return;
            }

            targetEnt = this.target.GetEntity();
            if ((null == targetEnt) || (targetEnt.health <= 0) || ((this.end_time != 0) && (gameLocal.time > this.end_time)) || gameLocal.inCinematic) {
                BecomeInactive(TH_THINK);
            }

            final idVec3 toPos = targetEnt.GetEyePosition();

            num = 0;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }

                if (ent.fl.hidden) {
                    // don't throw hidden objects
                    continue;
                }

                if (0 == this.targetTime.oGet(i)) {
                    // already threw this object
                    continue;
                }

                num++;

                time = MS2SEC(this.targetTime.oGet(i) - gameLocal.time);
                if (time > this.shake_time) {
                    continue;
                }

                entPhys = ent.GetPhysics();
                final idVec3 entOrg = entPhys.GetOrigin();

                gameLocal.clip.TracePoint(tr, entOrg, toPos, MASK_OPAQUE, ent);
                if ((tr[0].fraction >= 1.0f) || gameLocal.GetTraceEntity(tr[0]).equals(targetEnt)) {
                    this.lastTargetPos.oSet(i, toPos);
                }

                if (time < 0.0f) {
                    idAI.PredictTrajectory(entPhys.GetOrigin(), this.lastTargetPos.oGet(i), this.speed, entPhys.GetGravity(),
                            entPhys.GetClipModel(), entPhys.GetClipMask(), 256.0f, ent, targetEnt, ai_debugTrajectory.GetBool() ? 1 : 0, vel);
                    vel.oMulSet(this.speed);
                    entPhys.SetLinearVelocity(vel);
                    if (0 == this.end_time) {
                        this.targetTime.oSet(i, 0);
                    } else {
                        this.targetTime.oSet(i, gameLocal.time + gameLocal.random.RandomInt(this.max_wait - this.min_wait) + this.min_wait);
                    }
                    if (ent.IsType(idMoveable.class)) {
                        final idMoveable ment = (idMoveable) ent;
                        ment.EnableDamage(true, 2.5f);
                    }
                } else {
                    // this is not the right way to set the angular velocity, but the effect is nice, so I'm keeping it. :)
                    ang.Set(gameLocal.random.CRandomFloat() * this.shake_ang.x, gameLocal.random.CRandomFloat() * this.shake_ang.y, gameLocal.random.CRandomFloat() * this.shake_ang.z);
                    ang.oMulSet(1.0f - (time / this.shake_time));
                    entPhys.SetAngularVelocity(ang);
                }
            }

            if (0 == num) {
                BecomeInactive(TH_THINK);
            }
        }

        private void Event_Activate(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;
            int i;
            float time;
            float frac;
            float scale;

            if ((this.thinkFlags & TH_THINK) != 0) {
                BecomeInactive(TH_THINK);
                return;
            }

            RemoveNullTargets();
            if (0 == this.targets.Num()) {
                return;
            }

            if ((null == activator) || !activator.IsType(idActor.class)) {
                this.target.oSet(gameLocal.GetLocalPlayer());
            } else {
                this.target.oSet((idActor) activator);
            }

            this.end_time = (int) (gameLocal.time + SEC2MS(this.spawnArgs.GetFloat("end_time", "0")));

            this.targetTime.SetNum(this.targets.Num());
            this.lastTargetPos.SetNum(this.targets.Num());

            final idVec3 toPos = this.target.GetEntity().GetEyePosition();

            // calculate the relative times of all the objects
            time = 0.0f;
            for (i = 0; i < this.targetTime.Num(); i++) {
                this.targetTime.oSetType(i, SEC2MS(time));
                this.lastTargetPos.oSet(i, toPos);

                frac = 1.0f - ((float) i / (float) this.targetTime.Num());
                time += ((gameLocal.random.RandomFloat() + 1.0f) * 0.5f * frac) + 0.1f;
            }

            // scale up the times to fit within throw_time
            scale = this.throw_time / time;
            for (i = 0; i < this.targetTime.Num(); i++) {
                this.targetTime.oSetType(i, gameLocal.time + SEC2MS(this.shake_time) + (this.targetTime.oGet(i) * scale));
            }

            BecomeActive(TH_THINK);
        }

//        private void Event_Throw();
//
//        private void Event_ShakeObject(idEntity object, int starttime);
//        
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
