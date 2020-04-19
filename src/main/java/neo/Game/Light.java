package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Hide;
import static neo.Game.Entity.EV_PostSpawn;
import static neo.Game.Entity.EV_Show;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_editEntityMode;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.framework.Common.EDITOR_LIGHT;
import static neo.framework.Common.EDITOR_SOUND;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.idlib.Lib.PackColor;
import static neo.idlib.Lib.UnpackColor;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_zero;

import java.util.HashMap;
import java.util.Map;

import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.eventCallback_t4;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Light {

    /*
     ===============================================================================

     Generic light.

     ===============================================================================
     */
    public static final idEventDef EV_Light_SetShader = new idEventDef("setShader", "s");
    public static final idEventDef EV_Light_GetLightParm = new idEventDef("getLightParm", "d", 'f');
    public static final idEventDef EV_Light_SetLightParm = new idEventDef("setLightParm", "df");
    public static final idEventDef EV_Light_SetLightParms = new idEventDef("setLightParms", "ffff");
    public static final idEventDef EV_Light_SetRadiusXYZ = new idEventDef("setRadiusXYZ", "fff");
    public static final idEventDef EV_Light_SetRadius = new idEventDef("setRadius", "f");
    public static final idEventDef EV_Light_On = new idEventDef("On", null);
    public static final idEventDef EV_Light_Off = new idEventDef("Off", null);
    public static final idEventDef EV_Light_FadeOut = new idEventDef("fadeOutLight", "f");
    public static final idEventDef EV_Light_FadeIn = new idEventDef("fadeInLight", "f");

    public static class idLight extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public 	CLASS_PROTOTYPE( idLight );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static{
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Light_SetShader, (eventCallback_t1<idLight>) idLight::Event_SetShader);
            eventCallbacks.put(EV_Light_GetLightParm, (eventCallback_t1<idLight>) idLight::Event_GetLightParm);
            eventCallbacks.put(EV_Light_SetLightParm, (eventCallback_t2<idLight>) idLight::Event_SetLightParm);
            eventCallbacks.put(EV_Light_SetLightParms, (eventCallback_t4<idLight>) idLight::Event_SetLightParms);
            eventCallbacks.put(EV_Light_SetRadiusXYZ, (eventCallback_t3<idLight>) idLight::Event_SetRadiusXYZ);
            eventCallbacks.put(EV_Light_SetRadius, (eventCallback_t1<idLight>) idLight::Event_SetRadius);
            eventCallbacks.put(EV_Hide, (eventCallback_t0<idLight>) idLight::Event_Hide);
            eventCallbacks.put(EV_Show, (eventCallback_t0<idLight>) idLight::Event_Show);
            eventCallbacks.put(EV_Light_On, (eventCallback_t0<idLight>) idLight::Event_On);
            eventCallbacks.put(EV_Light_Off, (eventCallback_t0<idLight>) idLight::Event_Off);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idLight>) idLight::Event_ToggleOnOff);
            eventCallbacks.put(EV_PostSpawn, (eventCallback_t0<idLight>) idLight::Event_SetSoundHandles);
            eventCallbacks.put(EV_Light_FadeOut, (eventCallback_t1<idLight>) idLight::Event_FadeOut);
            eventCallbacks.put(EV_Light_FadeIn, (eventCallback_t1<idLight>) idLight::Event_FadeIn);
        }


        private final renderLight_s renderLight;          // light presented to the renderer
        private idVec3 localLightOrigin;            // light origin relative to the physics origin
        private idMat3 localLightAxis;              // light axis relative to physics axis
        private int/*qhandle_t*/ lightDefHandle;    // handle to renderer light def
        private final idStr brokenModel;
        private final int[] levels = {0};
        private int currentLevel;
        private idVec3 baseColor;
        private boolean breakOnTrigger;//TODO:give all variables default init values like c++, opposite of lazy init?
        private int count;
        private int triggercount;
        private idEntity lightParent;
        private final idVec4 fadeFrom;
        private idVec4 fadeTo;
        private int fadeStart;
        private int fadeEnd;
        private boolean soundWasPlaying;
        //
        //

        public idLight() {
//	memset( &renderLight, 0, sizeof( renderLight ) );
            this.renderLight = new renderLight_s();
            this.localLightOrigin = getVec3_zero();
            this.localLightAxis = getMat3_identity();
            this.lightDefHandle = -1;
            this.brokenModel = new idStr();
            this.levels[0] = 0;
            this.currentLevel = 0;
            this.baseColor = getVec3_zero();
            this.breakOnTrigger = false;
            this.count = 0;
            this.triggercount = 0;
            this.lightParent = null;
            this.fadeFrom = new idVec4(1, 1, 1, 1);
            this.fadeTo = new idVec4(1, 1, 1, 1);
            this.fadeStart = 0;
            this.fadeEnd = 0;
            this.soundWasPlaying = false;
        }
        // ~idLight();

        @Override
        public void Spawn() {
            super.Spawn();
            
            final boolean[] start_off = {false};
            boolean needBroken;
            final String[] demonic_shader = {null};

            // do the parsing the same way dmap and the editor do
            GameEdit.gameEdit.ParseSpawnArgsToRenderLight(this.spawnArgs, this.renderLight);

            // we need the origin and axis relative to the physics origin/axis
            this.localLightOrigin = this.renderLight.origin.oMinus(GetPhysics().GetOrigin()).oMultiply(GetPhysics().GetAxis().Transpose());
            this.localLightAxis = this.renderLight.axis.oMultiply(GetPhysics().GetAxis().Transpose());

            // set the base color from the shader parms
            this.baseColor.Set(this.renderLight.shaderParms[ SHADERPARM_RED], this.renderLight.shaderParms[ SHADERPARM_GREEN], this.renderLight.shaderParms[ SHADERPARM_BLUE]);

            // set the number of light levels
            this.spawnArgs.GetInt("levels", "1", this.levels);
            this.currentLevel = this.levels[0];
            if (this.levels[0] <= 0) {
                idGameLocal.Error("Invalid light level set on entity #%d(%s)", this.entityNumber, this.name);
            }

            // make sure the demonic shader is cached
            if (this.spawnArgs.GetString("mat_demonic", null, demonic_shader)) {
                declManager.FindType(DECL_MATERIAL, demonic_shader[0]);
            }

            // game specific functionality, not mirrored in
            // editor or dmap light parsing
            // also put the light texture on the model, so light flares
            // can get the current intensity of the light
            this.renderEntity.referenceShader = this.renderLight.shader;

            this.lightDefHandle = -1;		// no static version yet

            // see if an optimized shadow volume exists
            // the renderer will ignore this value after a light has been moved,
            // but there may still be a chance to get it wrong if the game moves
            // a light before the first present, and doesn't clear the prelight
            this.renderLight.prelightModel = null;
            if (this.name.oGet(0) != 0) {
                // this will return 0 if not found
                this.renderLight.prelightModel = renderModelManager.CheckModel(va("_prelight_%s", this.name));
            }

            this.spawnArgs.GetBool("start_off", "0", start_off);
            if (start_off[0]) {
                Off();
            }

            this.health = this.spawnArgs.GetInt("health", "0");
            this.spawnArgs.GetString("broken", "", this.brokenModel);
            this.breakOnTrigger = this.spawnArgs.GetBool("break", "0");
            this.count = this.spawnArgs.GetInt("count", "1");

            this.triggercount = 0;

            this.fadeFrom.Set(1, 1, 1, 1);
            this.fadeTo.Set(1, 1, 1, 1);
            this.fadeStart = 0;
            this.fadeEnd = 0;

            // if we have a health make light breakable
            if (this.health != 0) {
                final idStr model = new idStr(this.spawnArgs.GetString("model"));		// get the visual model
                if (0 == model.Length()) {
                    idGameLocal.Error("Breakable light without a model set on entity #%d(%s)", this.entityNumber, this.name);
                }

                this.fl.takedamage = true;

                // see if we need to create a broken model name
                needBroken = true;
                if ((model.Length() != 0) && NOT(this.brokenModel.Length())) {
                    int pos;

                    needBroken = false;

                    pos = model.Find(".");
                    if (pos < 0) {
                        pos = model.Length();
                    }
                    if (pos > 0) {
                        model.Left(pos, this.brokenModel);
                    }
                    this.brokenModel.oPluSet("_broken");
                    if (pos > 0) {
                        this.brokenModel.oPluSet(model.substring(pos));
                    }
                }

                // make sure the model gets cached
                if (NOT(renderModelManager.CheckModel(this.brokenModel))) {
                    if (needBroken) {
                        idGameLocal.Error("Model '%s' not found for entity %d(%s)", this.brokenModel, this.entityNumber, this.name);
                    } else {
                        this.brokenModel.oSet("");
                    }
                }

                GetPhysics().SetContents(this.spawnArgs.GetBool("nonsolid") ? 0 : CONTENTS_SOLID);

                // make sure the collision model gets cached
                idClipModel.CheckModel(this.brokenModel);
            }

            PostEventMS(EV_PostSpawn, 0);

            UpdateVisuals();
        }

        /*
         ================
         idLight::Save

         archives object for save game file
         ================
         */
        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteRenderLight(this.renderLight);

            savefile.WriteBool(this.renderLight.prelightModel != null);

            savefile.WriteVec3(this.localLightOrigin);
            savefile.WriteMat3(this.localLightAxis);

            savefile.WriteString(this.brokenModel);
            savefile.WriteInt(this.levels[0]);
            savefile.WriteInt(this.currentLevel);

            savefile.WriteVec3(this.baseColor);
            savefile.WriteBool(this.breakOnTrigger);
            savefile.WriteInt(this.count);
            savefile.WriteInt(this.triggercount);
            savefile.WriteObject(this.lightParent);

            savefile.WriteVec4(this.fadeFrom);
            savefile.WriteVec4(this.fadeTo);
            savefile.WriteInt(this.fadeStart);
            savefile.WriteInt(this.fadeEnd);
            savefile.WriteBool(this.soundWasPlaying);
        }

        /*
         ================
         idLight::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] hadPrelightModel = {false};

            savefile.ReadRenderLight(this.renderLight);

            savefile.ReadBool(hadPrelightModel);
            this.renderLight.prelightModel = renderModelManager.CheckModel(va("_prelight_%s", this.name));
            if ((this.renderLight.prelightModel == null) && hadPrelightModel[0]) {
                assert (false);
                if (developer.GetBool()) {
                    // we really want to know if this happens
                    idGameLocal.Error("idLight::Restore: prelightModel '_prelight_%s' not found", this.name);
                } else {
                    // but let it slide after release
                    gameLocal.Warning("idLight::Restore: prelightModel '_prelight_%s' not found", this.name);
                }
            }

            savefile.ReadVec3(this.localLightOrigin);
            savefile.ReadMat3(this.localLightAxis);

            savefile.ReadString(this.brokenModel);
            savefile.ReadInt(this.levels);
            this.currentLevel = savefile.ReadInt();

            savefile.ReadVec3(this.baseColor);
            this.breakOnTrigger = savefile.ReadBool();
            this.count = savefile.ReadInt();
            this.triggercount = savefile.ReadInt();
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/lightParent);

            savefile.ReadVec4(this.fadeFrom);
            savefile.ReadVec4(this.fadeTo);
            this.fadeStart = savefile.ReadInt();
            this.fadeEnd = savefile.ReadInt();
            this.soundWasPlaying = savefile.ReadBool();

            this.lightDefHandle = -1;

            SetLightLevel();
        }

        @Override
        public void UpdateChangeableSpawnArgs(final idDict source) {

            super.UpdateChangeableSpawnArgs(source);

            if (source != null) {
                source.Print();
            }
            FreeSoundEmitter(true);
            GameEdit.gameEdit.ParseSpawnArgsToRefSound(source != null ? source : this.spawnArgs, this.refSound);
            if ((this.refSound.shader != null) && !this.refSound.waitfortrigger) {
                StartSoundShader(this.refSound.shader, SND_CHANNEL_ANY, 0, false, null);
            }

            GameEdit.gameEdit.ParseSpawnArgsToRenderLight(source != null ? source : this.spawnArgs, this.renderLight);

            UpdateVisuals();
        }

        @Override
        public void Think() {
            idVec4 color = new idVec4();

            if ((this.thinkFlags & TH_THINK) != 0) {
                if (this.fadeEnd > 0) {
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

            RunPhysics();
            Present();
        }

        @Override
        public void FreeLightDef() {
            if (this.lightDefHandle != -1) {
                gameRenderWorld.FreeLightDef(this.lightDefHandle);
                this.lightDefHandle = -1;
            }
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            origin = this.localLightOrigin.oPlus(this.renderLight.lightCenter);
            axis = this.localLightAxis.oMultiply(GetPhysics().GetAxis());
            return true;
        }

        @Override
        public void Present() {
            // don't present to the renderer if the entity hasn't changed
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }

            // add the model
            super.Present();

            // current transformation
            this.renderLight.axis.oSet(this.localLightAxis.oMultiply(GetPhysics().GetAxis()));
            this.renderLight.origin.oSet(GetPhysics().GetOrigin().oPlus(GetPhysics().GetAxis().oMultiply(this.localLightOrigin)));

            // reference the sound for shader synced effects
            if (this.lightParent != null) {
                this.renderLight.referenceSound = this.lightParent.GetSoundEmitter();
                this.renderEntity.referenceSound = this.lightParent.GetSoundEmitter();
            } else {
                this.renderLight.referenceSound = this.refSound.referenceSound;
                this.renderEntity.referenceSound = this.refSound.referenceSound;
            }

            // update the renderLight and renderEntity to render the light and flare
            PresentLightDefChange();
            PresentModelDefChange();
        }

        public void SaveState(idDict args) {
            int i;
			final int c = this.spawnArgs.GetNumKeyVals();
            for (i = 0; i < c; i++) {
                final idKeyValue pv = this.spawnArgs.GetKeyVal(i);
                if ((pv.GetKey().Find("editor_", false) >= 0) || (pv.GetKey().Find("parse_", false) >= 0)) {
                    continue;
                }
                args.Set(pv.GetKey(), pv.GetValue());
            }
        }

        @Override
        public void SetColor(float red, float green, float blue) {
            this.baseColor.Set(red, green, blue);
            SetLightLevel();
        }

        @Override
        public void SetColor(final idVec4 color) {
            this.baseColor = color.ToVec3();
            this.renderLight.shaderParms[ SHADERPARM_ALPHA] = color.oGet(3);
            this.renderEntity.shaderParms[ SHADERPARM_ALPHA] = color.oGet(3);
            SetLightLevel();
        }

        @Override
        public void GetColor(idVec3 out) {
            out.oSet(0, this.renderLight.shaderParms[ SHADERPARM_RED]);
            out.oSet(1, this.renderLight.shaderParms[ SHADERPARM_GREEN]);
            out.oSet(2, this.renderLight.shaderParms[ SHADERPARM_BLUE]);
        }

        @Override
        public void GetColor(idVec4 out) {
            out.oSet(0, this.renderLight.shaderParms[ SHADERPARM_RED]);
            out.oSet(1, this.renderLight.shaderParms[ SHADERPARM_GREEN]);
            out.oSet(2, this.renderLight.shaderParms[ SHADERPARM_BLUE]);
            out.oSet(3, this.renderLight.shaderParms[ SHADERPARM_ALPHA]);
        }

        public idVec3 GetBaseColor() {
            return this.baseColor;
        }

        public void SetShader(final String shadername) {
            // allow this to be NULL
            this.renderLight.shader = declManager.FindMaterial(shadername, false);
            PresentLightDefChange();
        }

        public void SetLightParm(int parmnum, float value) {
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                idGameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            this.renderLight.shaderParms[ parmnum] = value;
            PresentLightDefChange();
        }

        public void SetLightParms(float parm0, float parm1, float parm2, float parm3) {
            this.renderLight.shaderParms[ SHADERPARM_RED] = parm0;
            this.renderLight.shaderParms[ SHADERPARM_GREEN] = parm1;
            this.renderLight.shaderParms[ SHADERPARM_BLUE] = parm2;
            this.renderLight.shaderParms[ SHADERPARM_ALPHA] = parm3;
            this.renderEntity.shaderParms[ SHADERPARM_RED] = parm0;
            this.renderEntity.shaderParms[ SHADERPARM_GREEN] = parm1;
            this.renderEntity.shaderParms[ SHADERPARM_BLUE] = parm2;
            this.renderEntity.shaderParms[ SHADERPARM_ALPHA] = parm3;
            PresentLightDefChange();
            PresentModelDefChange();
        }

        public void SetRadiusXYZ(float x, float y, float z) {
            this.renderLight.lightRadius.oSet(0, x);
            this.renderLight.lightRadius.oSet(1, y);
            this.renderLight.lightRadius.oSet(2, z);
            PresentLightDefChange();
        }

        public void SetRadius(float radius) {
            this.renderLight.lightRadius.oSet(0, this.renderLight.lightRadius.oSet(1, this.renderLight.lightRadius.oSet(2, radius)));
            PresentLightDefChange();
        }

        public void On() {
            this.currentLevel = this.levels[0];
            // offset the start time of the shader to sync it to the game time
            this.renderLight.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            if ((this.soundWasPlaying || this.refSound.waitfortrigger) && (this.refSound.shader != null)) {
                StartSoundShader(this.refSound.shader, SND_CHANNEL_ANY, 0, false, null);
                this.soundWasPlaying = false;
            }
            SetLightLevel();
            BecomeActive(TH_UPDATEVISUALS);
        }

        public void Off() {
            this.currentLevel = 0;
            // kill any sound it was making
            if ((this.refSound.referenceSound != null) && this.refSound.referenceSound.CurrentlyPlaying()) {
                StopSound(etoi(SND_CHANNEL_ANY), false);
                this.soundWasPlaying = true;
            }
            SetLightLevel();
            BecomeActive(TH_UPDATEVISUALS);
        }

        public void Fade(final idVec4 to, float fadeTime) {
            GetColor(this.fadeFrom);
            this.fadeTo = to;
            this.fadeStart = gameLocal.time;
            this.fadeEnd = (int) (gameLocal.time + SEC2MS(fadeTime));
            BecomeActive(TH_THINK);
        }

        public void FadeOut(float time) {
            Fade(colorBlack, time);
        }

        public void FadeIn(float time) {
            final idVec3 color = new idVec3();
            final idVec4 color4 = new idVec4();

            this.currentLevel = this.levels[0];
            this.spawnArgs.GetVector("_color", "1 1 1", color);
            color4.Set(color.x, color.y, color.z, 1.0f);
            Fade(color4, time);
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            BecomeBroken(attacker);
        }

        public void BecomeBroken(idEntity activator) {
            final String[] damageDefName = {null};

            this.fl.takedamage = false;

            if (this.brokenModel.Length() != 0) {
                SetModel(this.brokenModel.getData());

                if (!this.spawnArgs.GetBool("nonsolid")) {
                    GetPhysics().SetClipModel(new idClipModel(this.brokenModel.getData()), 1.0f);
                    GetPhysics().SetContents(CONTENTS_SOLID);
                }
            } else if (this.spawnArgs.GetBool("hideModelOnBreak")) {
                SetModel("");
                GetPhysics().SetContents(0);
            }

            if (gameLocal.isServer) {

                ServerSendEvent(EVENT_BECOMEBROKEN, null, true, -1);

                if (this.spawnArgs.GetString("def_damage", "", damageDefName)) {
                    final idVec3 origin = this.renderEntity.origin.oPlus(this.renderEntity.bounds.GetCenter().oMultiply(this.renderEntity.axis));
                    gameLocal.RadiusDamage(origin, activator, activator, this, this, damageDefName[0]);
                }

            }

            ActivateTargets(activator);

            // offset the start time of the shader to sync it to the game time
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            this.renderLight.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            // set the state parm
            this.renderEntity.shaderParms[ SHADERPARM_MODE] = 1;
            this.renderLight.shaderParms[ SHADERPARM_MODE] = 1;

            // if the light has a sound, either start the alternate (broken) sound, or stop the sound
            String parm = this.spawnArgs.GetString("snd_broken");
            if ((this.refSound.shader != null) || ((parm != null) && !parm.isEmpty())) {
                StopSound(etoi(SND_CHANNEL_ANY), false);
                final idSoundShader alternate = this.refSound.shader != null ? this.refSound.shader.GetAltSound() : declManager.FindSound(parm);
                if (alternate != null) {
                    // start it with no diversity, so the leadin break sound plays
                    this.refSound.referenceSound.StartSound(alternate, etoi(SND_CHANNEL_ANY), 0, 0);
                }
            }

            parm = this.spawnArgs.GetString("mtr_broken");
            if ((parm != null) && !parm.isEmpty()) {
                SetShader(parm);
            }

            UpdateVisuals();
        }

        public int/*qhandle_t*/ GetLightDefHandle() {
            return this.lightDefHandle;
        }

        public void SetLightParent(idEntity lparent) {
            this.lightParent = lparent;
        }

        public void SetLightLevel() {
            idVec3 color;
            float intensity;

            intensity = (float) this.currentLevel / (float) this.levels[0];
            color = this.baseColor.oMultiply(intensity);
            this.renderLight.shaderParms[ SHADERPARM_RED] = color.oGet(0);
            this.renderLight.shaderParms[ SHADERPARM_GREEN] = color.oGet(1);
            this.renderLight.shaderParms[ SHADERPARM_BLUE] = color.oGet(2);
            this.renderEntity.shaderParms[ SHADERPARM_RED] = color.oGet(0);
            this.renderEntity.shaderParms[ SHADERPARM_GREEN] = color.oGet(1);
            this.renderEntity.shaderParms[ SHADERPARM_BLUE] = color.oGet(2);
            PresentLightDefChange();
            PresentModelDefChange();
        }

        @Override
        public void ShowEditingDialog() {
            if (g_editEntityMode.GetInteger() == 1) {
                common.InitTool(EDITOR_LIGHT, this.spawnArgs);
            } else {
                common.InitTool(EDITOR_SOUND, this.spawnArgs);
            }
        }
        // enum {
        public static final int EVENT_BECOMEBROKEN = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_MAXEVENTS = EVENT_BECOMEBROKEN + 1;
        // };

        @Override
        public void ClientPredictionThink() {
            Think();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {

            GetPhysics().WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);

            msg.WriteByte(this.currentLevel);
            msg.WriteLong((int) PackColor(this.baseColor));
            // msg.WriteBits( lightParent.GetEntityNum(), GENTITYNUM_BITS );

            /*	// only helps prediction
             msg.WriteLong( PackColor( fadeFrom ) );
             msg.WriteLong( PackColor( fadeTo ) );
             msg.WriteLong( fadeStart );
             msg.WriteLong( fadeEnd );
             */
            // FIXME: send renderLight.shader
            msg.WriteFloat(this.renderLight.lightRadius.oGet(0), 5, 10);
            msg.WriteFloat(this.renderLight.lightRadius.oGet(1), 5, 10);
            msg.WriteFloat(this.renderLight.lightRadius.oGet(2), 5, 10);

            msg.WriteLong((int) PackColor(new idVec4(
                    this.renderLight.shaderParms[SHADERPARM_RED],
                    this.renderLight.shaderParms[SHADERPARM_GREEN],
                    this.renderLight.shaderParms[SHADERPARM_BLUE],
                    this.renderLight.shaderParms[SHADERPARM_ALPHA])));

            msg.WriteFloat(this.renderLight.shaderParms[SHADERPARM_TIMESCALE], 5, 10);
            msg.WriteLong((int) this.renderLight.shaderParms[SHADERPARM_TIMEOFFSET]);
            //msg.WriteByte( renderLight.shaderParms[SHADERPARM_DIVERSITY] );
            msg.WriteShort((int) this.renderLight.shaderParms[SHADERPARM_MODE]);

            WriteColorToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            final idVec4 shaderColor = new idVec4();
            final int oldCurrentLevel = this.currentLevel;
            final idVec3 oldBaseColor = this.baseColor;

            GetPhysics().ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);

            this.currentLevel = msg.ReadByte();
            if (this.currentLevel != oldCurrentLevel) {
                // need to call On/Off for flickering lights to start/stop the sound
                // while doing it this way rather than through events, the flickering is out of sync between clients
                // but at least there is no question about saving the event and having them happening globally in the world
                if (this.currentLevel != 0) {
                    On();
                } else {
                    Off();
                }
            }
            UnpackColor(msg.ReadLong(), this.baseColor);
            // lightParentEntityNum = msg.ReadBits( GENTITYNUM_BITS );	

            /*	// only helps prediction
             UnpackColor( msg.ReadLong(), fadeFrom );
             UnpackColor( msg.ReadLong(), fadeTo );
             fadeStart = msg.ReadLong();
             fadeEnd = msg.ReadLong();
             */
            // FIXME: read renderLight.shader
            this.renderLight.lightRadius.oSet(0, msg.ReadFloat(5, 10));
            this.renderLight.lightRadius.oSet(1, msg.ReadFloat(5, 10));
            this.renderLight.lightRadius.oSet(2, msg.ReadFloat(5, 10));

            UnpackColor(msg.ReadLong(), shaderColor);
            this.renderLight.shaderParms[SHADERPARM_RED] = shaderColor.oGet(0);
            this.renderLight.shaderParms[SHADERPARM_GREEN] = shaderColor.oGet(1);
            this.renderLight.shaderParms[SHADERPARM_BLUE] = shaderColor.oGet(2);
            this.renderLight.shaderParms[SHADERPARM_ALPHA] = shaderColor.oGet(3);

            this.renderLight.shaderParms[SHADERPARM_TIMESCALE] = msg.ReadFloat(5, 10);
            this.renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = msg.ReadLong();
            //renderLight.shaderParms[SHADERPARM_DIVERSITY] = msg.ReadFloat();
            this.renderLight.shaderParms[SHADERPARM_MODE] = msg.ReadShort();

            ReadColorFromSnapshot(msg);

            if (msg.HasChanged()) {
                if ((this.currentLevel != oldCurrentLevel) || (this.baseColor != oldBaseColor)) {
                    SetLightLevel();
                } else {
                    PresentLightDefChange();
                    PresentModelDefChange();
                }
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {

            switch (event) {
                case EVENT_BECOMEBROKEN: {
                    BecomeBroken(null);
                    return true;
                }
                default:
                    return super.ClientReceiveEvent(event, time, msg);

            }
//            return false;
        }

        private void PresentLightDefChange() {
            // let the renderer apply it to the world
            if ((this.lightDefHandle != -1)) {
                gameRenderWorld.UpdateLightDef(this.lightDefHandle, this.renderLight);
            } else {
                this.lightDefHandle = gameRenderWorld.AddLightDef(this.renderLight);
            }
        }

        private void PresentModelDefChange() {

            if ((null == this.renderEntity.hModel) || IsHidden()) {
                return;
            }

            // add to refresh list
            if (this.modelDefHandle == -1) {
                this.modelDefHandle = gameRenderWorld.AddEntityDef(this.renderEntity);
                final int a = 0;
            } else {
                gameRenderWorld.UpdateEntityDef(this.modelDefHandle, this.renderEntity);
            }
        }

        private void Event_SetShader(final idEventArg<String> shadername) {
            SetShader(shadername.value);
        }

        private void Event_GetLightParm(idEventArg<Integer> _parmnum) {
            final int parmnum = _parmnum.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                idGameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            idThread.ReturnFloat(this.renderLight.shaderParms[ parmnum]);
        }

        private void Event_SetLightParm(idEventArg<Integer> parmnum, idEventArg<Float> value) {
            SetLightParm(parmnum.value, value.value);
        }

        private void Event_SetLightParms(idEventArg<Float> parm0, idEventArg<Float> parm1, idEventArg<Float> parm2, idEventArg<Float> parm3) {
            SetLightParms(parm0.value, parm1.value, parm2.value, parm3.value);
        }

        private void Event_SetRadiusXYZ(idEventArg<Float> x, idEventArg<Float> y, idEventArg<Float> z) {
            SetRadiusXYZ(x.value, y.value, z.value);
        }

        private void Event_SetRadius(idEventArg<Float> radius) {
            SetRadius(radius.value);
        }

        private void Event_Hide() {
            Hide();
            PresentModelDefChange();
            Off();
        }

        private void Event_Show() {
            Show();
            PresentModelDefChange();
            On();
        }

        private void Event_On() {
            On();
        }

        private void Event_Off() {
            Off();
        }

        private void Event_ToggleOnOff(idEventArg<idEntity> activator) {
            this.triggercount++;
            if (this.triggercount < this.count) {
                return;
            }

            // reset trigger count
            this.triggercount = 0;

            if (this.breakOnTrigger) {
                BecomeBroken(activator.value);
                this.breakOnTrigger = false;
                return;
            }

            if (0 == this.currentLevel) {
                On();
            } else {
                this.currentLevel--;
                if (0 == this.currentLevel) {
                    Off();
                } else {
                    SetLightLevel();
                }
            }
        }

        /*
         ================
         idLight::Event_SetSoundHandles

         set the same sound def handle on all targeted lights
         ================
         */
        private void Event_SetSoundHandles() {
            int i;
            idEntity targetEnt;

            if (NOT(this.refSound.referenceSound)) {
                return;
            }

            for (i = 0; i < this.targets.Num(); i++) {
                targetEnt = this.targets.oGet(i).GetEntity();
                if ((targetEnt != null) && targetEnt.IsType(idLight.class)) {
                    final idLight light = (idLight) targetEnt;
                    light.lightParent = this;

                    // explicitly delete any sounds on the entity
                    light.FreeSoundEmitter(true);

                    // manually set the refSound to this light's refSound
                    light.renderEntity.referenceSound = this.renderEntity.referenceSound;

                    // update the renderEntity to the renderer
                    light.UpdateVisuals();
                }
            }
        }

        private void Event_FadeOut(idEventArg<Float> time) {
            FadeOut(time.value);
        }

        private void Event_FadeIn(idEventArg<Float> time) {
            FadeIn(time.value);
        }

        @Override
        public idClass CreateInstance() {
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

        @Override
        protected void _deconstructor() {
            if (this.lightDefHandle != -1) {
                gameRenderWorld.FreeLightDef(this.lightDefHandle);
            }

            super._deconstructor();
        }
    }
}
