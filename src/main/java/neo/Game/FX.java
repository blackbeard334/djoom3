package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_skipFX;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import static neo.TempDump.NOT;
import static neo.framework.DeclFX.fx_enum.FX_ATTACHENTITY;
import static neo.framework.DeclFX.fx_enum.FX_ATTACHLIGHT;
import static neo.framework.DeclFX.fx_enum.FX_LIGHT;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_FX;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.HashMap;
import java.util.Map;

import neo.Game.Entity.idEntity;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idProjectile;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclFX.idDeclFX;
import neo.framework.DeclFX.idFXSingleAction;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class FX {

    /*
     ===============================================================================

     Special effects.

     ===============================================================================
     */
    public static class idFXLocalAction {

        renderLight_s renderLight;          // light presented to the renderer
        int/*qhandle_t*/ lightDefHandle;    // handle to renderer light def
        renderEntity_s renderEntity;        // used to present a model to the renderer
        int            modelDefHandle;      // handle to static renderer model
        float          delay;
        int            particleSystem;
        int            start;
        boolean        soundStarted;
        boolean        shakeStarted;
        boolean        decalDropped;
        boolean        launched;
    };
    /*
     ===============================================================================

     idEntityFx

     ===============================================================================
     */
    public static final idEventDef EV_Fx_KillFx = new idEventDef("_killfx");
    public static final idEventDef EV_Fx_Action = new idEventDef("_fxAction", "e");	// implemented by subclasses

    public static class idEntityFx extends idEntity {

        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idEntityFx>) idEntityFx::Event_Trigger);
            eventCallbacks.put(EV_Fx_KillFx, (eventCallback_t0<idEntityFx>) idEntityFx::Event_ClearFx);
        }


        protected int                     started;
        protected int                     nextTriggerTime;
        protected idDeclFX                fxEffect;                // GetFX() should be called before using fxEffect as a pointer
        protected idList<idFXLocalAction> actions;
        protected idStr                   systemName;
        //
        //

//        public 	CLASS_PROTOTYPE( idEntityFx );
        public idEntityFx() {
            fxEffect = null;
            started = -1;
            nextTriggerTime = -1;
            fl.networkSync = true;
            actions = new idList<>();
            systemName = new idStr();
        }
//	virtual					~idEntityFx();

        @Override
        public void Spawn() {
            super.Spawn();

            if (g_skipFX.GetBool()) {
                return;
            }

            String[] fx = new String[1];
            nextTriggerTime = 0;
            fxEffect = null;
            if (spawnArgs.GetString("fx", "", fx)) {
                systemName.oSet(fx[0]);
            }
            if (!spawnArgs.GetBool("triggered")) {
                Setup(fx[0]);
                if (spawnArgs.GetBool("test") || spawnArgs.GetBool("start") || spawnArgs.GetFloat("restart") != 0) {
                    PostEventMS(EV_Activate, 0, this);
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(started);
            savefile.WriteInt(nextTriggerTime);
            savefile.WriteFX(fxEffect);
            savefile.WriteString(systemName);

            savefile.WriteInt(actions.Num());

            for (i = 0; i < actions.Num(); i++) {

                if (actions.oGet(i).lightDefHandle >= 0) {
                    savefile.WriteBool(true);
                    savefile.WriteRenderLight(actions.oGet(i).renderLight);
                } else {
                    savefile.WriteBool(false);
                }

                if (actions.oGet(i).modelDefHandle >= 0) {
                    savefile.WriteBool(true);
                    savefile.WriteRenderEntity(actions.oGet(i).renderEntity);
                } else {
                    savefile.WriteBool(false);
                }

                savefile.WriteFloat(actions.oGet(i).delay);
                savefile.WriteInt(actions.oGet(i).start);
                savefile.WriteBool(actions.oGet(i).soundStarted);
                savefile.WriteBool(actions.oGet(i).shakeStarted);
                savefile.WriteBool(actions.oGet(i).decalDropped);
                savefile.WriteBool(actions.oGet(i).launched);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = {0};
            boolean[] hasObject = {false};

            started = savefile.ReadInt();
            nextTriggerTime = savefile.ReadInt();
            savefile.ReadFX(fxEffect);
            savefile.ReadString(systemName);

            savefile.ReadInt(num);

            actions.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {

                savefile.ReadBool(hasObject);
                if (hasObject[0]) {
                    savefile.ReadRenderLight(actions.oGet(i).renderLight);
                    actions.oGet(i).lightDefHandle = gameRenderWorld.AddLightDef(actions.oGet(i).renderLight);
                } else {
//			memset( actions.oGet(i).renderLight, 0, sizeof( renderLight_t ) );
                    actions.oGet(i).renderLight = new renderLight_s();
                    actions.oGet(i).lightDefHandle = -1;
                }

                savefile.ReadBool(hasObject);
                if (hasObject[0]) {
                    savefile.ReadRenderEntity(actions.oGet(i).renderEntity);
                    actions.oGet(i).modelDefHandle = gameRenderWorld.AddEntityDef(actions.oGet(i).renderEntity);
                } else {
//			memset( &actions[i].renderEntity, 0, sizeof( renderEntity_t ) );
                    actions.oGet(i).renderEntity = new renderEntity_s();
                    actions.oGet(i).modelDefHandle = -1;
                }

                actions.oGet(i).delay = savefile.ReadFloat();

                // let the FX regenerate the particleSystem
                actions.oGet(i).particleSystem = -1;

                actions.oGet(i).start = savefile.ReadInt();
                actions.oGet(i).soundStarted = savefile.ReadBool();
                actions.oGet(i).shakeStarted = savefile.ReadBool();
                actions.oGet(i).decalDropped = savefile.ReadBool();
                actions.oGet(i).launched = savefile.ReadBool();
            }
        }

        /*
         ================
         idEntityFx::Think

         Clears any visual fx started when {item,mob,player} was spawned
         ================
         */
        @Override
        public void Think() {
            if (g_skipFX.GetBool()) {
                return;
            }

            if ((thinkFlags & TH_THINK) != 0) {
                Run(gameLocal.time);
            }

            RunPhysics();
            Present();
        }

        public void Setup(final String fx) {

            if (started >= 0) {
                return;					// already started
            }

            // early during MP Spawn() with no information. wait till we ReadFromSnapshot for more
            if (gameLocal.isClient && (null == fx || !fx.isEmpty())) {//[0] == '\0' ) ) {
                return;
            }

            systemName.oSet(fx);
            started = 0;

            fxEffect = (idDeclFX) declManager.FindType(DECL_FX, systemName);

            if (fxEffect != null) {
                idFXLocalAction localAction = new idFXLocalAction();

//		memset( &localAction, 0, sizeof( idFXLocalAction ) );
                actions.AssureSize(fxEffect.events.Num(), localAction);

                for (int i = 0; i < fxEffect.events.Num(); i++) {
                    final idFXSingleAction fxaction = fxEffect.events.oGet(i);

                    idFXLocalAction laction = actions.oGet(i);
                    if (fxaction.random1 != 0 || fxaction.random2 != 0) {
                        laction.delay = fxaction.random1 + gameLocal.random.RandomFloat() * (fxaction.random2 - fxaction.random1);
                    } else {
                        laction.delay = fxaction.delay;
                    }
                    laction.start = -1;
                    laction.lightDefHandle = -1;
                    laction.modelDefHandle = -1;
                    laction.particleSystem = -1;
                    laction.shakeStarted = false;
                    laction.decalDropped = false;
                    laction.launched = false;
                }
            }
        }

        public void Run(int time) {
            int ieff, j;
            idEntity[] ent = {null};
            idDict projectileDef;
            idProjectile projectile;

            if (NOT(fxEffect)) {
                return;
            }

            for (ieff = 0; ieff < fxEffect.events.Num(); ieff++) {
                final idFXSingleAction fxaction = fxEffect.events.oGet(ieff);
                idFXLocalAction laction = actions.oGet(ieff);

                //
                // if we're currently done with this one
                //
                if (laction.start == -1) {
                    continue;
                }

                //
                // see if it's delayed
                //
                if (laction.delay != 0) {
                    if (laction.start + (time - laction.start) < laction.start + (laction.delay * 1000)) {
                        continue;
                    }
                }

                //
                // each event can have it's own delay and restart
                //
                int actualStart = laction.delay != 0 ? laction.start + (int) (laction.delay * 1000) : laction.start;
                float pct = (float) (time - actualStart) / (1000 * fxaction.duration);
                if (pct >= 1.0f) {
                    laction.start = -1;
                    float totalDelay;
                    if (fxaction.restart != 0) {
                        if (fxaction.random1 != 0 || fxaction.random2 != 0) {
                            totalDelay = fxaction.random1 + gameLocal.random.RandomFloat() * (fxaction.random2 - fxaction.random1);
                        } else {
                            totalDelay = fxaction.delay;
                        }
                        laction.delay = totalDelay;
                        laction.start = time;
                    }
                    continue;
                }

                if (fxaction.fire.Length() != 0) {
                    for (j = 0; j < fxEffect.events.Num(); j++) {
                        if (fxEffect.events.oGet(j).name.Icmp(fxaction.fire) == 0) {
                            actions.oGet(j).delay = 0;
                        }
                    }
                }

                idFXLocalAction useAction;
                if (fxaction.sibling == -1) {
                    useAction = laction;
                } else {
                    useAction = actions.oGet(fxaction.sibling);
                }
                assert (useAction != null);

                switch (fxaction.type) {
                    case FX_ATTACHLIGHT:
                    case FX_LIGHT: {
                        if (useAction.lightDefHandle == -1) {
                            if (fxaction.type == FX_LIGHT) {
                                useAction.renderLight = new renderLight_s();//memset( &useAction.renderLight, 0, sizeof( renderLight_t ) );
                                useAction.renderLight.origin.oSet(GetPhysics().GetOrigin().oPlus(fxaction.offset));
                                useAction.renderLight.axis.oSet(GetPhysics().GetAxis());
                                useAction.renderLight.lightRadius.oSet(0, fxaction.lightRadius);
                                useAction.renderLight.lightRadius.oSet(1, fxaction.lightRadius);
                                useAction.renderLight.lightRadius.oSet(2, fxaction.lightRadius);
                                useAction.renderLight.shader = declManager.FindMaterial(fxaction.data, false);
                                useAction.renderLight.shaderParms[SHADERPARM_RED] = fxaction.lightColor.x;
                                useAction.renderLight.shaderParms[SHADERPARM_GREEN] = fxaction.lightColor.y;
                                useAction.renderLight.shaderParms[SHADERPARM_BLUE] = fxaction.lightColor.z;
                                useAction.renderLight.shaderParms[SHADERPARM_TIMESCALE] = 1.0f;
                                useAction.renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(time);
                                useAction.renderLight.referenceSound = refSound.referenceSound;
                                useAction.renderLight.pointLight = true;
                                if (fxaction.noshadows) {
                                    useAction.renderLight.noShadows = true;
                                }
                                useAction.lightDefHandle = gameRenderWorld.AddLightDef(useAction.renderLight);
                            }
                            if (fxaction.noshadows) {
                                for (j = 0; j < fxEffect.events.Num(); j++) {
                                    idFXLocalAction laction2 = actions.oGet(j);
                                    if (laction2.modelDefHandle != -1) {
                                        laction2.renderEntity.noShadow = true;
                                    }
                                }
                            }
                        }
                        ApplyFade(fxaction, useAction, time, actualStart);
                        break;
                    }
                    case FX_SOUND: {
                        if (!useAction.soundStarted) {
                            useAction.soundStarted = true;
                            final idSoundShader shader = declManager.FindSound(fxaction.data);
                            StartSoundShader(shader, SND_CHANNEL_ANY, 0, false, null);
                            for (j = 0; j < fxEffect.events.Num(); j++) {
                                idFXLocalAction laction2 = actions.oGet(j);
                                if (laction2.lightDefHandle != -1) {
                                    laction2.renderLight.referenceSound = refSound.referenceSound;
                                    gameRenderWorld.UpdateLightDef(laction2.lightDefHandle, laction2.renderLight);
                                }
                            }
                        }
                        break;
                    }
                    case FX_DECAL: {
                        if (!useAction.decalDropped) {
                            useAction.decalDropped = true;
                            gameLocal.ProjectDecal(GetPhysics().GetOrigin(), GetPhysics().GetGravity(), 8.0f, true, fxaction.size, fxaction.data.toString());
                        }
                        break;
                    }
                    case FX_SHAKE: {
                        if (!useAction.shakeStarted) {
                            idDict args = new idDict();
                            args.Clear();
                            args.SetFloat("kick_time", fxaction.shakeTime);
                            args.SetFloat("kick_amplitude", fxaction.shakeAmplitude);
                            for (j = 0; j < gameLocal.numClients; j++) {
                                idPlayer player = gameLocal.GetClientByNum(j);
                                if (player != null && (player.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin())).LengthSqr() < Square(fxaction.shakeDistance)) {
                                    if (!gameLocal.isMultiplayer || !fxaction.shakeIgnoreMaster || GetBindMaster() != player) {
                                        player.playerView.DamageImpulse(fxaction.offset, args);
                                    }
                                }
                            }
                            if (fxaction.shakeImpulse != 0.0f && fxaction.shakeDistance != 0.0f) {
                                idEntity ignore_ent = null;
                                if (gameLocal.isMultiplayer) {
                                    ignore_ent = this;
                                    if (fxaction.shakeIgnoreMaster) {
                                        ignore_ent = GetBindMaster();
                                    }
                                }
                                // lookup the ent we are bound to?
                                gameLocal.RadiusPush(GetPhysics().GetOrigin(), fxaction.shakeDistance, fxaction.shakeImpulse, this, ignore_ent, 1.0f, true);
                            }
                            useAction.shakeStarted = true;
                        }
                        break;
                    }
                    case FX_ATTACHENTITY:
                    case FX_PARTICLE:
                    case FX_MODEL: {
                        if (useAction.modelDefHandle == -1) {
//					memset( &useAction.renderEntity, 0, sizeof( renderEntity_t ) );
                            useAction.renderEntity = new renderEntity_s();
                            useAction.renderEntity.origin.oSet(GetPhysics().GetOrigin().oPlus(fxaction.offset));
                            useAction.renderEntity.axis.oSet((fxaction.explicitAxis) ? fxaction.axis : GetPhysics().GetAxis());
                            useAction.renderEntity.hModel = renderModelManager.FindModel(fxaction.data.toString());
                            useAction.renderEntity.shaderParms[ SHADERPARM_RED] = 1.0f;
                            useAction.renderEntity.shaderParms[ SHADERPARM_GREEN] = 1.0f;
                            useAction.renderEntity.shaderParms[ SHADERPARM_BLUE] = 1.0f;
                            useAction.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(time);
                            useAction.renderEntity.shaderParms[3] = 1.0f;
                            useAction.renderEntity.shaderParms[5] = 0.0f;
                            if (useAction.renderEntity.hModel != null) {
                                useAction.renderEntity.bounds.oSet(useAction.renderEntity.hModel.Bounds(useAction.renderEntity));
                            }
                            useAction.modelDefHandle = gameRenderWorld.AddEntityDef(useAction.renderEntity);
                        } else if (fxaction.trackOrigin) {
                            useAction.renderEntity.origin.oSet(GetPhysics().GetOrigin().oPlus(fxaction.offset));
                            useAction.renderEntity.axis.oSet(fxaction.explicitAxis ? fxaction.axis : GetPhysics().GetAxis());
                        }
                        ApplyFade(fxaction, useAction, time, actualStart);
                        break;
                    }
                    case FX_LAUNCH: {
                        if (gameLocal.isClient) {
                            // client never spawns entities outside of ClientReadSnapshot
                            useAction.launched = true;
                            break;
                        }
                        if (!useAction.launched) {
                            useAction.launched = true;
                            projectile = null;
                            // FIXME: may need to cache this if it is slow
                            projectileDef = gameLocal.FindEntityDefDict(fxaction.data.toString(), false);
                            if (null == projectileDef) {
                                gameLocal.Warning("projectile \'%s\' not found", fxaction.data);
                            } else {
                                gameLocal.SpawnEntityDef(projectileDef, ent, false);
                                if (ent[0] != null && ent[0].IsType(idProjectile.class)) {
                                    projectile = (idProjectile) ent[0];
                                    projectile.Create(this, GetPhysics().GetOrigin(), GetPhysics().GetAxis().oGet(0));
                                    projectile.Launch(GetPhysics().GetOrigin(), GetPhysics().GetAxis().oGet(0), getVec3_origin());
                                }
                            }
                        }
                        break;
                    }
				default:
					// TODO check unused Enum case labels
					break;
                }
            }
        }

        public void Start(int time) {
            if (NOT(fxEffect)) {
                return;
            }
            started = time;
            for (int i = 0; i < fxEffect.events.Num(); i++) {
                idFXLocalAction laction = actions.oGet(i);
                laction.start = time;
                laction.soundStarted = false;
                laction.shakeStarted = false;
                laction.particleSystem = -1;
                laction.decalDropped = false;
                laction.launched = false;
            }
        }

        public void Stop() {
            CleanUp();
            started = -1;
        }

        public int Duration() {
            int max = 0;

            if (NOT(fxEffect)) {
                return max;
            }
            for (int i = 0; i < fxEffect.events.Num(); i++) {
                final idFXSingleAction fxaction = fxEffect.events.oGet(i);
                int d = (int) ((fxaction.delay + fxaction.duration) * 1000.0f);
                if (d > max) {
                    max = d;
                }
            }

            return max;
        }

        public String EffectName() {
            return (fxEffect != null ? fxEffect.GetName() : null);
        }

        public String Joint() {
            return (fxEffect != null ? fxEffect.joint.toString() : null);
        }

        public boolean Done() {
            if (started > 0 && gameLocal.time > started + Duration()) {
                return true;
            }
            return false;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            GetPhysics().WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
            msg.WriteLong((fxEffect != null) ? gameLocal.ServerRemapDecl(-1, DECL_FX, fxEffect.Index()) : -1);
            msg.WriteLong(started);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int fx_index, start_time;
            int[] max_lapse = new int[1];

            GetPhysics().ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);
            fx_index = gameLocal.ClientRemapDecl(DECL_FX, msg.ReadLong());
            start_time = msg.ReadLong();

            if (fx_index != -1 && start_time > 0 && NOT(fxEffect) && started < 0) {
                spawnArgs.GetInt("effect_lapse", "1000", max_lapse);
                if (gameLocal.time - start_time > max_lapse[0]) {
                    // too late, skip the effect completely
                    started = 0;
                    return;
                }
                final idDeclFX fx = (idDeclFX) declManager.DeclByIndex(DECL_FX, fx_index);
                if (null == fx) {
                    gameLocal.Error("FX at index %d not found", fx_index);
                }
                fxEffect = fx;
                Setup(fx.GetName());
                Start(start_time);
            }
        }

        @Override
        public void ClientPredictionThink() {
            if (gameLocal.isNewFrame) {
                Run(gameLocal.time);
            }
            RunPhysics();
            Present();
        }

        public static idEntityFx StartFx(final String fx, final idVec3 useOrigin, final idMat3 useAxis, idEntity ent, boolean bind) {

            if (g_skipFX.GetBool() || null == fx || fx.isEmpty()) {
                return null;
            }

            idDict args = new idDict();
            args.SetBool("start", true);
            args.Set("fx", fx);
            idEntityFx nfx = (idEntityFx) gameLocal.SpawnEntityType(idEntityFx.class, args);
            if (nfx.Joint() != null && !nfx.Joint().isEmpty()) {
                nfx.BindToJoint(ent, nfx.Joint(), true);
                nfx.SetOrigin(getVec3_origin());
            } else {
                nfx.SetOrigin((useOrigin != null) ? useOrigin : ent.GetPhysics().GetOrigin());
                nfx.SetAxis((useAxis != null) ? useAxis : ent.GetPhysics().GetAxis());
            }

            if (bind) {
                // never bind to world spawn
                if (ent != gameLocal.world) {
                    nfx.Bind(ent, true);
                }
            }
            nfx.Show();
            return nfx;
        }

        public static idEntityFx StartFx(final idStr fx, final idVec3 useOrigin, final idMat3 useAxis, idEntity ent, boolean bind) {
            return StartFx(fx.toString(), useOrigin, useAxis, ent, bind);
        }

        protected void Event_Trigger(idEventArg<idEntity> activator) {

            if (g_skipFX.GetBool()) {
                return;
            }

            float fxActionDelay;
            String[] fx = new String[1];

            if (gameLocal.time < nextTriggerTime) {
                return;
            }

            if (spawnArgs.GetString("fx", "", fx)) {
                Setup(fx[0]);
                Start(gameLocal.time);
                PostEventMS(EV_Fx_KillFx, Duration());
                BecomeActive(TH_THINK);
            }

            fxActionDelay = spawnArgs.GetFloat("fxActionDelay");
            if (fxActionDelay != 0.0f) {
                nextTriggerTime = (int) (gameLocal.time + SEC2MS(fxActionDelay));
            } else {
                // prevent multiple triggers on same frame
                nextTriggerTime = gameLocal.time + 1;
            }
            PostEventSec(EV_Fx_Action, fxActionDelay, activator.value);
        }

        /*
         ================
         idEntityFx::Event_ClearFx

         Clears any visual fx started when item(mob) was spawned
         ================
         */
        protected void Event_ClearFx() {

            if (g_skipFX.GetBool()) {
                return;
            }

            Stop();
            CleanUp();
            BecomeInactive(TH_THINK);

            if (spawnArgs.GetBool("test")) {
                PostEventMS(EV_Activate, 0, this);
            } else {
                if (spawnArgs.GetFloat("restart") != 0 || !spawnArgs.GetBool("triggered")) {
                    float rest = spawnArgs.GetFloat("restart", "0");
                    if (rest == 0.0f) {
                        PostEventSec(EV_Remove, 0.1f);
                    } else {
                        rest *= gameLocal.random.RandomFloat();
                        PostEventSec(EV_Activate, rest, this);
                    }
                }
            }
        }

        protected void CleanUp() {
            if (NOT(fxEffect)) {
                return;
            }
            for (int i = 0; i < fxEffect.events.Num(); i++) {
                final idFXSingleAction fxaction = fxEffect.events.oGet(i);
                idFXLocalAction laction = actions.oGet(i);
                CleanUpSingleAction(fxaction, laction);
            }
        }

        protected void CleanUpSingleAction(final idFXSingleAction fxaction, idFXLocalAction laction) {
            if (laction.lightDefHandle != -1 && fxaction.sibling == -1 && fxaction.type != FX_ATTACHLIGHT) {
                gameRenderWorld.FreeLightDef(laction.lightDefHandle);
                laction.lightDefHandle = -1;
            }
            if (laction.modelDefHandle != -1 && fxaction.sibling == -1 && fxaction.type != FX_ATTACHENTITY) {
                gameRenderWorld.FreeEntityDef(laction.modelDefHandle);
                laction.modelDefHandle = -1;
            }
            laction.start = -1;
        }

        protected void ApplyFade(final idFXSingleAction fxaction, idFXLocalAction laction, final int time, final int actualStart) {
            if (fxaction.fadeInTime != 0 || fxaction.fadeOutTime != 0) {
                float fadePct = (float) (time - actualStart) / (1000.0f * ((fxaction.fadeInTime != 0) ? fxaction.fadeInTime : fxaction.fadeOutTime));
                if (fadePct > 1.0) {
                    fadePct = 1.0f;
                }
                if (laction.modelDefHandle != -1) {
                    laction.renderEntity.shaderParms[SHADERPARM_RED] = (fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct;
                    laction.renderEntity.shaderParms[SHADERPARM_GREEN] = (fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct;
                    laction.renderEntity.shaderParms[SHADERPARM_BLUE] = (fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct;

                    gameRenderWorld.UpdateEntityDef(laction.modelDefHandle, laction.renderEntity);
                }
                if (laction.lightDefHandle != -1) {
                    laction.renderLight.shaderParms[SHADERPARM_RED] = fxaction.lightColor.x * ((fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct);
                    laction.renderLight.shaderParms[SHADERPARM_GREEN] = fxaction.lightColor.y * ((fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct);
                    laction.renderLight.shaderParms[SHADERPARM_BLUE] = fxaction.lightColor.z * ((fxaction.fadeInTime != 0) ? fadePct : 1.0f - fadePct);

                    gameRenderWorld.UpdateLightDef(laction.lightDefHandle, laction.renderLight);
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

    };

    /*
     ===============================================================================

     idTeleporter
	
     ===============================================================================
     */
    public static class idTeleporter extends idEntityFx {
//        public 	CLASS_PROTOTYPE( idTeleporter );

        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTeleporter>) idTeleporter::Event_DoAction);
        }

        // teleporters to this location
        private void Event_DoAction(idEventArg<idEntity> activator) {
            float angle;

            angle = spawnArgs.GetFloat("angle");
            idAngles a = new idAngles(0, spawnArgs.GetFloat("angle"), 0);
            activator.value.Teleport(GetPhysics().GetOrigin(), a, null);
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
