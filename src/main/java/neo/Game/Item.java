package neo.Game;

import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.AFEntity.EV_Gib;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEPARTICLES;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_dropItemRotation;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ITEM;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Player.idPlayer;
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
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.deferredEntityCallback_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
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
public class Item {

    static final idEventDef EV_DropToFloor   = new idEventDef("<dropToFloor>");
    static final idEventDef EV_RespawnItem   = new idEventDef("respawn");
    static final idEventDef EV_RespawnFx     = new idEventDef("<respawnFx>");
    static final idEventDef EV_GetPlayerPos  = new idEventDef("<getplayerpos>");
    static final idEventDef EV_HideObjective = new idEventDef("<hideobjective>", "e");
    static final idEventDef EV_CamShot       = new idEventDef("<camshot>");


    /*
     ===============================================================================

     Items the player can pick up or use.

     ===============================================================================
     */
    public static class idItem extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public	CLASS_PROTOTYPE( idItem );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_DropToFloor, (eventCallback_t0<idItem>) idItem::Event_DropToFloor);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idItem>) idItem::Event_Touch);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idItem>) idItem::Event_Trigger);
            eventCallbacks.put(EV_RespawnItem, (eventCallback_t0<idItem>) idItem::Event_Respawn);
            eventCallbacks.put(EV_RespawnFx, (eventCallback_t0<idItem>) idItem::Event_RespawnFx);
        }


        // enum {
        public static final int EVENT_PICKUP    = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_RESPAWN   = EVENT_PICKUP + 1;
        public static final int EVENT_RESPAWNFX = EVENT_PICKUP + 2;
        public static final int EVENT_MAXEVENTS = EVENT_PICKUP + 3;
        // };
        private idVec3     orgOrigin;
        private boolean    spin;
        private boolean    pulse;
        private boolean    canPickUp;
        //
        // for item pulse effect
        private int        itemShellHandle;
        private idMaterial shellMaterial;
        //
        // used to update the item pulse effect
        private boolean    inView;
        private int        inViewTime;
        private int        lastCycle;
        private int        lastRenderViewTime;
        //
        //

        public idItem() {
            this.spin = false;
            this.inView = false;
            this.inViewTime = 0;
            this.lastCycle = 0;
            this.lastRenderViewTime = -1;
            this.itemShellHandle = -1;
            this.shellMaterial = null;
            this.orgOrigin = new idVec3();
            this.canPickUp = true;
            this.fl.networkSync = true;
        }
        // virtual					~idItem();

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteVec3(this.orgOrigin);
            savefile.WriteBool(this.spin);
            savefile.WriteBool(this.pulse);
            savefile.WriteBool(this.canPickUp);

            savefile.WriteMaterial(this.shellMaterial);

            savefile.WriteBool(this.inView);
            savefile.WriteInt(this.inViewTime);
            savefile.WriteInt(this.lastCycle);
            savefile.WriteInt(this.lastRenderViewTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.orgOrigin);
            this.spin = savefile.ReadBool();
            this.spin = savefile.ReadBool();
            this.canPickUp = savefile.ReadBool();

            savefile.ReadMaterial(this.shellMaterial);

            this.inView = savefile.ReadBool();
            this.inViewTime = savefile.ReadInt();
            this.lastCycle = savefile.ReadInt();
            this.lastRenderViewTime = savefile.ReadInt();

            this.itemShellHandle = -1;
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            String giveTo;
            idEntity ent;
            final float[] tsize = {0};

            if (this.spawnArgs.GetBool("dropToFloor")) {
                PostEventMS(EV_DropToFloor, 0);
            }

            if (this.spawnArgs.GetFloat("triggersize", "0", tsize)) {
                GetPhysics().GetClipModel().LoadModel(new idTraceModel(new idBounds(getVec3_origin()).Expand(tsize[0])));
                GetPhysics().GetClipModel().Link(gameLocal.clip);
            }

            if (this.spawnArgs.GetBool("start_off")) {
                GetPhysics().SetContents(0);
                Hide();
            } else {
                GetPhysics().SetContents(CONTENTS_TRIGGER);
            }

            giveTo = this.spawnArgs.GetString("owner");
            if (giveTo.length() != 0) {
                ent = gameLocal.FindEntity(giveTo);
                if (NOT(ent)) {
                    gameLocal.Error("Item couldn't find owner '%s'", giveTo);
                }
                PostEventMS(EV_Touch, 0, ent, null);
            }

            if (this.spawnArgs.GetBool("spin") || gameLocal.isMultiplayer) {
                this.spin = true;
                BecomeActive(TH_THINK);
            }

            //pulse = !spawnArgs.GetBool( "nopulse" );
            //temp hack for tim
            this.pulse = false;
            this.orgOrigin = GetPhysics().GetOrigin();

            this.canPickUp = !(this.spawnArgs.GetBool("triggerFirst") || this.spawnArgs.GetBool("no_touch"));

            this.inViewTime = -1000;
            this.lastCycle = -1;
            this.itemShellHandle = -1;
            this.shellMaterial = declManager.FindMaterial("itemHighlightShell");
        }

        public void GetAttributes(idDict attributes) {
            int i;
            idKeyValue arg;

            for (i = 0; i < this.spawnArgs.GetNumKeyVals(); i++) {
                arg = this.spawnArgs.GetKeyVal(i);
                if (arg.GetKey().Left(4).equals("inv_")) {
                    attributes.Set(arg.GetKey().Right(arg.GetKey().Length() - 4), arg.GetValue());
                }
            }
        }

        public boolean GiveToPlayer(idPlayer player) {
            if (player == null) {
                return false;
            }

            if (this.spawnArgs.GetBool("inv_carry")) {
                return player.GiveInventoryItem(this.spawnArgs);
            }

            return player.GiveItem(this);
        }

        public boolean Pickup(idPlayer player) {

            if (!GiveToPlayer(player)) {
                return false;
            }

            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_PICKUP, null, false, -1);
            }

            // play pickup sound
            StartSound("snd_acquire", SND_CHANNEL_ITEM, 0, false, null);

            // trigger our targets
            ActivateTargets(player);

            // clear our contents so the object isn't picked up twice
            GetPhysics().SetContents(0);

            // hide the model
            Hide();

            // add the highlight shell
            if (this.itemShellHandle != -1) {
                gameRenderWorld.FreeEntityDef(this.itemShellHandle);
                this.itemShellHandle = -1;
            }

            float respawn = this.spawnArgs.GetFloat("respawn");
            final boolean dropped = this.spawnArgs.GetBool("dropped");
            final boolean no_respawn = this.spawnArgs.GetBool("no_respawn");

            if (gameLocal.isMultiplayer && (respawn == 0.0f)) {
                respawn = 20.0f;
            }

            if ((respawn != 0) && !dropped && !no_respawn) {
                final String sfx = this.spawnArgs.GetString("fxRespawn");
                if ((sfx != null) && !sfx.isEmpty()) {
                    PostEventSec(EV_RespawnFx, respawn - 0.5f);
                }
                PostEventSec(EV_RespawnItem, respawn);
            } else if (!this.spawnArgs.GetBool("inv_objective") && !no_respawn) {
                // give some time for the pickup sound to play
                // FIXME: Play on the owner
                if (!this.spawnArgs.GetBool("inv_carry")) {
                    PostEventMS(EV_Remove, 5000);
                }
            }

            BecomeInactive(TH_THINK);
            return true;
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                if (this.spin) {
                    final idAngles ang = new idAngles();
                    idVec3 org;

                    ang.pitch = ang.roll = 0.0f;
                    ang.yaw = ((gameLocal.time & 4095) * 360.0f) / -4096.0f;
                    SetAngles(ang);

                    final float scale = 0.005f + (this.entityNumber * 0.00001f);

                    org = this.orgOrigin;
                    org.z += 4.0f + (cos((gameLocal.time + 2000) * scale) * 4.0f);
                    SetOrigin(org);
                }
            }

            Present();
        }

        @Override
        public void Present() {
            super.Present();

            if (!this.fl.hidden && this.pulse) {
                // also add a highlight shell model
                renderEntity_s shell;

                shell = this.renderEntity;

                // we will mess with shader parms when the item is in view
                // to give the "item pulse" effect
                shell.callback = idItem.ModelCallback.getInstance();
                shell.entityNum = this.entityNumber;
                shell.customShader = this.shellMaterial;
                if (this.itemShellHandle == -1) {
                    this.itemShellHandle = gameRenderWorld.AddEntityDef(shell);
                } else {
                    gameRenderWorld.UpdateEntityDef(this.itemShellHandle, shell);
                }

            }
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

            switch (event) {
                case EVENT_PICKUP: {

                    // play pickup sound
                    StartSound("snd_acquire", SND_CHANNEL_ITEM, 0, false, null);

                    // hide the model
                    Hide();

                    // remove the highlight shell
                    if (this.itemShellHandle != -1) {
                        gameRenderWorld.FreeEntityDef(this.itemShellHandle);
                        this.itemShellHandle = -1;
                    }
                    return true;
                }
                case EVENT_RESPAWN: {
                    Event_Respawn();
                    return true;
                }
                case EVENT_RESPAWNFX: {
                    Event_RespawnFx();
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//	return false;
        }

        // networking
        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(btoi(IsHidden()), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            if (msg.ReadBits(1) != 0) {
                Hide();
            } else {
                Show();
            }
        }

        @Override
        public boolean UpdateRenderEntity(renderEntity_s renderEntity, final renderView_s renderView) {

            if (this.lastRenderViewTime == renderView.time) {
                return false;
            }

            this.lastRenderViewTime = renderView.time;

            // check for glow highlighting if near the center of the view
            final idVec3 dir = renderEntity.origin.oMinus(renderView.vieworg);
            dir.Normalize();
            final float d = dir.oMultiply(renderView.viewaxis.oGet(0));

            // two second pulse cycle
            float cycle = (renderView.time - this.inViewTime) / 2000.0f;

            if (d > 0.94f) {
                if (!this.inView) {
                    this.inView = true;
                    if (cycle > this.lastCycle) {
                        // restart at the beginning
                        this.inViewTime = renderView.time;
                        cycle = 0.0f;
                    }
                }
            } else {
                if (this.inView) {
                    this.inView = false;
                    this.lastCycle = (int) ceil(cycle);
                }
            }

            // fade down after the last pulse finishes 
            if (!this.inView && (cycle > this.lastCycle)) {
                renderEntity.shaderParms[4] = 0.0f;
            } else {
                // pulse up in 1/4 second
                cycle -= (int) cycle;
                if (cycle < 0.1f) {
                    renderEntity.shaderParms[4] = cycle * 10.0f;
                } else if (cycle < 0.2f) {
                    renderEntity.shaderParms[4] = 1.0f;
                } else if (cycle < 0.3f) {
                    renderEntity.shaderParms[4] = 1.0f - ((cycle - 0.2f) * 10.0f);
                } else {
                    // stay off between pulses
                    renderEntity.shaderParms[4] = 0.0f;
                }
            }

            // update every single time this is in view
            return true;
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                idItem ent;

                // this may be triggered by a model trace or other non-view related source
                if (null == v) {
                    return false;
                }

                ent = (idItem) gameLocal.entities[ e.entityNum];
                if (null == ent) {
                    gameLocal.Error("idItem::ModelCallback: callback with NULL game entity");
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

        private void Event_DropToFloor() {
            final trace_s[] trace = {null};

            // don't drop the floor if bound to another entity
            if ((GetBindMaster() != null) && !GetBindMaster().equals(this)) {
                return;
            }

            gameLocal.clip.TraceBounds(trace, this.renderEntity.origin, this.renderEntity.origin.oMinus(new idVec3(0, 0, 64)), this.renderEntity.bounds, MASK_SOLID | CONTENTS_CORPSE, this);
            SetOrigin(trace[0].endpos);
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            if (!other.IsType(idPlayer.class)) {
                return;
            }

            if (!this.canPickUp) {
                return;
            }

            Pickup((idPlayer) other);
        }

        private void Event_Trigger(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;
            if (!this.canPickUp && this.spawnArgs.GetBool("triggerFirst")) {
                this.canPickUp = true;
                return;
            }

            if ((activator != null) && activator.IsType(idPlayer.class)) {
                Pickup((idPlayer) activator);
            }
        }

        private void Event_Respawn() {
            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_RESPAWN, null, false, -1);
            }
            BecomeActive(TH_THINK);
            Show();
            this.inViewTime = -1000;
            this.lastCycle = -1;
            GetPhysics().SetContents(CONTENTS_TRIGGER);
            SetOrigin(this.orgOrigin);
            StartSound("snd_respawn", SND_CHANNEL_ITEM, 0, false, null);
            CancelEvents(EV_RespawnItem); // don't double respawn
        }

        private void Event_RespawnFx() {
            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_RESPAWNFX, null, false, -1);
            }
            final String sfx = this.spawnArgs.GetString("fxRespawn");
            if ((sfx != null) && !sfx.isEmpty()) {
                idEntityFx.StartFx(sfx, null, null, this, true);
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

     idItemPowerup

     ===============================================================================
     */
    public static class idItemPowerup extends idItem {
// public 	CLASS_PROTOTYPE( idItemPowerup );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final int[] time = {0};
        private final int[] type = {0};
        //
        //

        public idItemPowerup() {
            this.time[0] = 0;
            this.type[0] = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.time[0]);
            savefile.WriteInt(this.type[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadInt(this.time);
            savefile.ReadInt(this.type);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.time[0] = this.spawnArgs.GetInt("time", "30");
            this.type[0] = this.spawnArgs.GetInt("type", "0");
        }

        @Override
        public boolean GiveToPlayer(idPlayer player) {
            if (player.spectating) {
                return false;
            }
            player.GivePowerUp(this.type[0], this.time[0] * 1000);
            return true;
        }

    }

    /*
     ===============================================================================

     idObjective

     ===============================================================================
     */
    public static class idObjective extends idItem {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//public 	CLASS_PROTOTYPE( idObjective );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idItem.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idObjective>) idObjective::Event_Trigger);
            eventCallbacks.put(EV_HideObjective, (eventCallback_t1<idObjective>) idObjective::Event_HideObjective);
            eventCallbacks.put(EV_GetPlayerPos, (eventCallback_t0<idObjective>) idObjective::Event_GetPlayerPos);
            eventCallbacks.put(EV_CamShot, (eventCallback_t0<idObjective>) idObjective::Event_CamShot);
        }

        private idVec3 playerPos;
        //
        //

        public idObjective() {
            this.playerPos = new idVec3();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.playerPos);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.playerPos);
            PostEventMS(EV_CamShot, 250);
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            Hide();
            PostEventMS(EV_CamShot, 250);
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {

                //Pickup( player );
                if (this.spawnArgs.GetString("inv_objective", null) != null) {
                    if ( /*player &&*/player.hud != null) {
                        final idStr shotName = new idStr(gameLocal.GetMapName());
                        shotName.StripFileExtension();
                        shotName.oPluSet("/");
                        shotName.oPluSet(this.spawnArgs.GetString("screenshot"));
                        shotName.SetFileExtension(".tga");
                        player.hud.SetStateString("screenshot", shotName.getData());
                        player.hud.SetStateString("objective", "1");
                        player.hud.SetStateString("objectivetext", this.spawnArgs.GetString("objectivetext"));
                        player.hud.SetStateString("objectivetitle", this.spawnArgs.GetString("objectivetitle"));
                        player.GiveObjective(this.spawnArgs.GetString("objectivetitle"), this.spawnArgs.GetString("objectivetext"), shotName.getData());

                        // a tad slow but keeps from having to update all objectives in all maps with a name ptr
                        for (int i = 0; i < gameLocal.num_entities; i++) {
                            if ((gameLocal.entities[ i] != null) && gameLocal.entities[ i].IsType(idObjectiveComplete.class)) {
                                if (idStr.Icmp(this.spawnArgs.GetString("objectivetitle"), gameLocal.entities[ i].spawnArgs.GetString("objectivetitle")) == 0) {
                                    gameLocal.entities[ i].spawnArgs.SetBool("objEnabled", true);
                                    break;
                                }
                            }
                        }

                        PostEventMS(EV_GetPlayerPos, 2000);
                    }
                }
            }
        }

        private void Event_HideObjective(idEventArg<idEntity> e) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                final idVec3 v = player.GetPhysics().GetOrigin().oMinus(this.playerPos);
                if (v.Length() > 64.0f) {
                    player.HideObjective();
                    PostEventMS(EV_Remove, 0);
                } else {
                    PostEventMS(EV_HideObjective, 100, player);
                }
            }
        }

        private void Event_GetPlayerPos() {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                this.playerPos = player.GetPhysics().GetOrigin();
                PostEventMS(EV_HideObjective, 100, player);
            }
        }

        private void Event_CamShot() {
            final String[] camName = {null};
            final idStr shotName = new idStr(gameLocal.GetMapName());
            shotName.StripFileExtension();
            shotName.oPluSet("/");
            shotName.oPluSet(this.spawnArgs.GetString("screenshot"));
            shotName.SetFileExtension(".tga");
            if (this.spawnArgs.GetString("camShot", "", camName)) {
                final idEntity ent = gameLocal.FindEntity(camName[0]);
                if ((ent != null) && (ent.cameraTarget != null)) {
                    final renderView_s view = ent.cameraTarget.GetRenderView();
                    final renderView_s fullView = view;
                    fullView.width = SCREEN_WIDTH;
                    fullView.height = SCREEN_HEIGHT;
                    // draw a view to a texture
                    renderSystem.CropRenderSize(256, 256, true);
                    gameRenderWorld.RenderScene(fullView);
                    renderSystem.CaptureRenderToFile(shotName.getData());
                    renderSystem.UnCrop();
                }
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

     idVideoCDItem

     ===============================================================================
     */
    public static class idVideoCDItem extends idItem {

//            public 	CLASS_PROTOTYPE( idVideoCDItem );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public boolean GiveToPlayer(idPlayer player) {
            final String str = this.spawnArgs.GetString("video");
            if ((player != null) && (str.length() != 0)) {
                player.GiveVideo(str, this.spawnArgs);
            }
            return true;
        }
    }

    /*
     ===============================================================================

     idPDAItem

     ===============================================================================
     */
    public static class idPDAItem extends idItem {
        //public 	CLASS_PROTOTYPE( idPDAItem );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public boolean GiveToPlayer(idPlayer player) {
            final idStr str = new idStr(this.spawnArgs.GetString("pda_name"));

            if (player != null) {
                player.GivePDA(str, this.spawnArgs);
            }
            return true;
        }
    }

    /*
     ===============================================================================

     idMoveableItem
	
     ===============================================================================
     */
    public static class idMoveableItem extends idItem {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idMoveableItem );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idItem.getEventCallBacks());
            eventCallbacks.put(EV_DropToFloor, (eventCallback_t0<idMoveableItem>) idMoveableItem::Event_DropToFloor);
            eventCallbacks.put(EV_Gib, (eventCallback_t1<idMoveableItem>) idMoveableItem::Event_Gib);
        }

        private final idPhysics_RigidBody physicsObj;
        private idClipModel         trigger;
        private idDeclParticle      smoke;
        private int                 smokeTime;
        //
        //

        public idMoveableItem() {
            this.physicsObj = new idPhysics_RigidBody();
            this.trigger = null;
            this.smoke = null;
            this.smokeTime = 0;
        }

        // virtual					~idMoveableItem();
        @Override
        protected void _deconstructor() {
            if (this.trigger != null) {
                idClipModel.delete(this.trigger);
            }
            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteClipModel(this.trigger);

            savefile.WriteParticle(this.smoke);
            savefile.WriteInt(this.smokeTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            savefile.ReadClipModel(this.trigger);

            savefile.ReadParticle(this.smoke);
            this.smokeTime = savefile.ReadInt();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            final idTraceModel trm = new idTraceModel();
            final float[] density = {0}, friction = {0}, bouncyness = {0}, tsize = {0};
            final idStr clipModelName = new idStr();
//            idBounds bounds = new idBounds();

            // create a trigger for item pickup
            this.spawnArgs.GetFloat("triggersize", "16.0", tsize);
            this.trigger = new idClipModel(new idTraceModel(new idBounds(getVec3_origin()).Expand(tsize[0])));
            this.trigger.Link(gameLocal.clip, this, 0, GetPhysics().GetOrigin(), GetPhysics().GetAxis());
            this.trigger.SetContents(CONTENTS_TRIGGER);

            // check if a clip model is set
            this.spawnArgs.GetString("clipmodel", "", clipModelName);
            if (!isNotNullOrEmpty(clipModelName)) {
                clipModelName.oSet(this.spawnArgs.GetString("model"));		// use the visual model
            }

            // load the trace model
            if (!CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm)) {
                gameLocal.Error("idMoveableItem '%s': cannot load collision model %s", this.name, clipModelName);
                return;
            }

            // if the model should be shrinked
            if (this.spawnArgs.GetBool("clipshrink")) {
                trm.Shrink(CM_CLIP_EPSILON);
            }

            // get rigid body properties
            this.spawnArgs.GetFloat("density", "0.5", density);
            density[0] = idMath.ClampFloat(0.001f, 1000.0f, density[0]);
            this.spawnArgs.GetFloat("friction", "0.05", friction);
            friction[0] = idMath.ClampFloat(0.0f, 1.0f, friction[0]);
            this.spawnArgs.GetFloat("bouncyness", "0.6", bouncyness);
            bouncyness[0] = idMath.ClampFloat(0.0f, 1.0f, bouncyness[0]);

            // setup the physics
            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(trm), density[0]);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetBouncyness(bouncyness[0]);
            this.physicsObj.SetFriction(0.6f, 0.6f, friction[0]);
            this.physicsObj.SetGravity(gameLocal.GetGravity());
            this.physicsObj.SetContents(CONTENTS_RENDERMODEL);
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_MOVEABLECLIP);
            SetPhysics(this.physicsObj);

            this.smoke = null;
            this.smokeTime = 0;
            final String smokeName = this.spawnArgs.GetString("smoke_trail");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                this.smoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                this.smokeTime = gameLocal.time;
                BecomeActive(TH_UPDATEPARTICLES);
            }
        }

        @Override
        public void Think() {

            RunPhysics();

            if ((this.thinkFlags & TH_PHYSICS) != 0) {
                // update trigger position
                this.trigger.Link(gameLocal.clip, this, 0, GetPhysics().GetOrigin(), getMat3_identity());
            }

            if ((this.thinkFlags & TH_UPDATEPARTICLES) != 0) {
                if (!gameLocal.smokeParticles.EmitSmoke(this.smoke, this.smokeTime, gameLocal.random.CRandomFloat(), GetPhysics().GetOrigin(), GetPhysics().GetAxis())) {
                    this.smokeTime = 0;
                    BecomeInactive(TH_UPDATEPARTICLES);
                }
            }

            Present();
        }

        @Override
        public boolean Pickup(idPlayer player) {
            final boolean ret = super.Pickup(player);
            if (ret) {
                this.trigger.SetContents(0);
            }
            return ret;
        }

        /*
         ================
         idMoveableItem::DropItems

         The entity should have the following key/value pairs set:
         "def_drop<type>Item"			"item def"
         "drop<type>ItemJoint"			"joint name"
         "drop<type>ItemRotation"		"pitch yaw roll"
         "drop<type>ItemOffset"			"x y z"
         "skin_drop<type>"				"skin name"
         To drop multiple items the following key/value pairs can be used:
         "def_drop<type>Item<X>"			"item def"
         "drop<type>Item<X>Joint"		"joint name"
         "drop<type>Item<X>Rotation"		"pitch yaw roll"
         "drop<type>Item<X>Offset"		"x y z"
         where <X> is an aribtrary string.
         ================
         */
        public static void DropItems(idAnimatedEntity ent, final String type, idList<idEntity> list) {
            idKeyValue kv;
            String skinName, c, jointName;
            String key, key2;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            final idAngles angles = new idAngles();
            idDeclSkin skin;
            int/*jointHandle_t*/ joint;
            idEntity item;
            int length;

            // drop all items
            kv = ent.spawnArgs.MatchPrefix(va("def_drop%sItem", type), null);
            while (kv != null) {

                c = kv.GetKey().getData();// + kv.GetKey().Length();
                length = kv.GetKey().Length();
                if ((idStr.Icmp(c.substring(length - 5), "Joint") != 0) && (idStr.Icmp(c.substring(length - 8), "Rotation") != 0)) {

                    key = kv.GetKey().getData() + 4;
                    key2 = key;
                    key += "Joint";
                    key2 += "Offset";
                    jointName = ent.spawnArgs.GetString(key);
                    joint = ent.GetAnimator().GetJointHandle(jointName);
                    if (!ent.GetJointWorldTransform(joint, gameLocal.time, origin, axis)) {
                        gameLocal.Warning("%s refers to invalid joint '%s' on entity '%s'\n", key, jointName, ent.name);
                        origin = ent.GetPhysics().GetOrigin();
                        axis = ent.GetPhysics().GetAxis();
                    }
                    if (isNotNullOrEmpty(g_dropItemRotation.GetString())) {
                        angles.Zero();
                        final Scanner sscanf = new Scanner(g_dropItemRotation.GetString());
                        angles.pitch = sscanf.nextFloat();
                        angles.yaw = sscanf.nextFloat();
                        angles.roll = sscanf.nextFloat();
                        sscanf.close();
                    } else {
                        key = kv.GetKey().getData() + 4;
                        key += "Rotation";
                        ent.spawnArgs.GetAngles(key, "0 0 0", angles);
                    }
                    axis = angles.ToMat3().oMultiply(axis);

                    origin.oPluSet(ent.spawnArgs.GetVector(key2, "0 0 0"));

                    item = DropItem(kv.GetValue().getData(), origin, axis, getVec3_origin(), 0, 0);
                    if ((list != null) && (item != null)) {
                        list.Append(item);
                    }
                }

                kv = ent.spawnArgs.MatchPrefix(va("def_drop%sItem", type), kv);
            }

            // change the skin to hide all items
            skinName = ent.spawnArgs.GetString(va("skin_drop%s", type));
            if (!skinName.isEmpty()) {
                skin = declManager.FindSkin(skinName);
                ent.SetSkin(skin);
            }
        }

        public static idEntity DropItem(final String classname, final idVec3 origin, final idMat3 axis, final idVec3 velocity, int activateDelay, int removeDelay) {
            final idDict args = new idDict();
            final idEntity[] item = {null};

            args.Set("classname", classname);
            args.Set("dropped", "1");

            // we sometimes drop idMoveables here, so set 'nodrop' to 1 so that it doesn't get put on the floor
            args.Set("nodrop", "1");

            if (activateDelay != 0) {
                args.SetBool("triggerFirst", true);
            }

            gameLocal.SpawnEntityDef(args, item);
            if (item[0] != null) {
                // set item position
                item[0].GetPhysics().SetOrigin(origin);
                item[0].GetPhysics().SetAxis(axis);
                item[0].GetPhysics().SetLinearVelocity(velocity);
                item[0].UpdateVisuals();
                if (activateDelay != 0) {
                    item[0].PostEventMS(EV_Activate, activateDelay, item[0]);
                }
                if (0 == removeDelay) {
                    removeDelay = 5 * 60 * 1000;
                }
                // always remove a dropped item after 5 minutes in case it dropped to an unreachable location
                item[0].PostEventMS(EV_Remove, removeDelay);
            }
            return item[0];
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

        private void Gib(final idVec3 dir, final String damageDefName) {
            // spawn smoke puff
            final String smokeName = this.spawnArgs.GetString("smoke_gib");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                final idDeclParticle smoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
                gameLocal.smokeParticles.EmitSmoke(smoke, gameLocal.time, gameLocal.random.CRandomFloat(), this.renderEntity.origin, this.renderEntity.axis);
            }
            // remove the entity
            PostEventMS(EV_Remove, 0);
        }

        private void Event_DropToFloor() {
            // the physics will drop the moveable to the floor
        }

        private void Event_Gib(final idEventArg<String> damageDefName) {
            Gib(new idVec3(0, 0, 1), damageDefName.value);
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

     idMoveablePDAItem

     ===============================================================================
     */
    public static class idMoveablePDAItem extends idMoveableItem {
//public 	CLASS_PROTOTYPE( idMoveablePDAItem );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public boolean GiveToPlayer(idPlayer player) {
            final idStr str = new idStr(this.spawnArgs.GetString("pda_name"));
            if (player != null) {
                player.GivePDA(str, this.spawnArgs);
            }
            return true;
        }
    }

    /*
     ===============================================================================

     Item removers.

     ===============================================================================
     */
    /*
     ===============================================================================

     idItemRemover

     ===============================================================================
     */
    public static class idItemRemover extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//public 	CLASS_PROTOTYPE( idItemRemover );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idItemRemover>) idItemRemover::Event_Trigger);
        }

        public void RemoveItem(idPlayer player) {
            final String remove;

            remove = this.spawnArgs.GetString("remove");
            player.RemoveInventoryItem(remove);
        }

        private void Event_Trigger(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;
            if (activator.IsType(idPlayer.class)) {
                RemoveItem((idPlayer) activator);
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

     idObjectiveComplete

     ===============================================================================
     */
    public static class idObjectiveComplete extends idItemRemover {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idObjectiveComplete );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idItemRemover.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idObjectiveComplete>) idObjectiveComplete::Event_Trigger);
            eventCallbacks.put(EV_HideObjective, (eventCallback_t1<idObjectiveComplete>) idObjectiveComplete::Event_HideObjective);
            eventCallbacks.put(EV_GetPlayerPos, (eventCallback_t0<idObjectiveComplete>) idObjectiveComplete::Event_GetPlayerPos);
        }

        private final idVec3 playerPos;
//
//
        public idObjectiveComplete() {
            this.playerPos = new idVec3();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.playerPos);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.playerPos);
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.spawnArgs.SetBool("objEnabled", false);
            Hide();
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            if (!this.spawnArgs.GetBool("objEnabled")) {
                return;
            }
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                RemoveItem(player);

                if (this.spawnArgs.GetString("inv_objective", null) != null) {
                    if (player.hud != null) {
                        player.hud.SetStateString("objective", "2");
                        player.hud.SetStateString("objectivetext", this.spawnArgs.GetString("objectivetext"));
                        player.hud.SetStateString("objectivetitle", this.spawnArgs.GetString("objectivetitle"));
                        player.CompleteObjective(this.spawnArgs.GetString("objectivetitle"));
                        PostEventMS(EV_GetPlayerPos, 2000);
                    }
                }
            }
        }

        private void Event_HideObjective(idEventArg<idEntity> e) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                this.playerPos.oSet(player.GetPhysics().GetOrigin());
                PostEventMS(EV_HideObjective, 100, player);
            }
        }

        private void Event_GetPlayerPos() {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                final idVec3 v = player.GetPhysics().GetOrigin();
                v.oMinSet(this.playerPos);
                if (v.Length() > 64.0f) {
                    player.hud.HandleNamedEvent("closeObjective");
                    PostEventMS(EV_Remove, 0);
                } else {
                    PostEventMS(EV_HideObjective, 100, player);
                }
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
}
