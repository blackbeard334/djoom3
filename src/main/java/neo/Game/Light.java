package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Hide;
import static neo.Game.Entity.EV_PostSpawn;
import static neo.Game.Entity.EV_Show;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class;
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
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_editEntityMode;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Script.Script_Thread.idThread;
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
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.framework.Common.EDITOR_LIGHT;
import static neo.framework.Common.EDITOR_SOUND;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.PackColor;
import static neo.idlib.Lib.UnpackColor;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

import java.util.HashMap;
import java.util.Map;

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


        private renderLight_s renderLight;          // light presented to the renderer
        private idVec3 localLightOrigin;            // light origin relative to the physics origin
        private idMat3 localLightAxis;              // light axis relative to physics axis
        private int/*qhandle_t*/ lightDefHandle;    // handle to renderer light def
        private idStr brokenModel;
        private int[] levels = {0};
        private int currentLevel;
        private idVec3 baseColor;
        private boolean breakOnTrigger;//TODO:give all variables default init values like c++, opposite of lazy init?
        private int count;
        private int triggercount;
        private idEntity lightParent;
        private idVec4 fadeFrom;
        private idVec4 fadeTo;
        private int fadeStart;
        private int fadeEnd;
        private boolean soundWasPlaying;
        //
        //

        public idLight() {
//	memset( &renderLight, 0, sizeof( renderLight ) );
            renderLight = new renderLight_s();
            localLightOrigin = getVec3_zero();
            localLightAxis = getMat3_identity();
            lightDefHandle = -1;
            brokenModel = new idStr();
            levels[0] = 0;
            currentLevel = 0;
            baseColor = getVec3_zero();
            breakOnTrigger = false;
            count = 0;
            triggercount = 0;
            lightParent = null;
            fadeFrom = new idVec4(1, 1, 1, 1);
            fadeTo = new idVec4(1, 1, 1, 1);
            fadeStart = 0;
            fadeEnd = 0;
            soundWasPlaying = false;
        }
        // ~idLight();

        @Override
        public void Spawn() {
            super.Spawn();
            
            boolean[] start_off = {false};
            boolean needBroken;
            String[] demonic_shader = {null};

            // do the parsing the same way dmap and the editor do
            GameEdit.gameEdit.ParseSpawnArgsToRenderLight(spawnArgs, renderLight);

            // we need the origin and axis relative to the physics origin/axis
            localLightOrigin = renderLight.origin.oMinus(GetPhysics().GetOrigin()).oMultiply(GetPhysics().GetAxis().Transpose());
            localLightAxis = renderLight.axis.oMultiply(GetPhysics().GetAxis().Transpose());

            // set the base color from the shader parms
            baseColor.Set(renderLight.shaderParms[ SHADERPARM_RED], renderLight.shaderParms[ SHADERPARM_GREEN], renderLight.shaderParms[ SHADERPARM_BLUE]);

            // set the number of light levels
            spawnArgs.GetInt("levels", "1", levels);
            currentLevel = levels[0];
            if (levels[0] <= 0) {
                gameLocal.Error("Invalid light level set on entity #%d(%s)", entityNumber, name);
            }

            // make sure the demonic shader is cached
            if (spawnArgs.GetString("mat_demonic", null, demonic_shader)) {
                declManager.FindType(DECL_MATERIAL, demonic_shader[0]);
            }

            // game specific functionality, not mirrored in
            // editor or dmap light parsing
            // also put the light texture on the model, so light flares
            // can get the current intensity of the light
            renderEntity.referenceShader = renderLight.shader;

            lightDefHandle = -1;		// no static version yet

            // see if an optimized shadow volume exists
            // the renderer will ignore this value after a light has been moved,
            // but there may still be a chance to get it wrong if the game moves
            // a light before the first present, and doesn't clear the prelight
            renderLight.prelightModel = null;
            if (name.oGet(0) != 0) {
                // this will return 0 if not found
                renderLight.prelightModel = renderModelManager.CheckModel(va("_prelight_%s", name));
            }

            spawnArgs.GetBool("start_off", "0", start_off);
            if (start_off[0]) {
                Off();
            }

            health = spawnArgs.GetInt("health", "0");
            spawnArgs.GetString("broken", "", brokenModel);
            breakOnTrigger = spawnArgs.GetBool("break", "0");
            count = spawnArgs.GetInt("count", "1");

            triggercount = 0;

            fadeFrom.Set(1, 1, 1, 1);
            fadeTo.Set(1, 1, 1, 1);
            fadeStart = 0;
            fadeEnd = 0;

            // if we have a health make light breakable
            if (health != 0) {
                idStr model = new idStr(spawnArgs.GetString("model"));		// get the visual model
                if (0 == model.Length()) {
                    gameLocal.Error("Breakable light without a model set on entity #%d(%s)", entityNumber, name);
                }

                fl.takedamage = true;

                // see if we need to create a broken model name
                needBroken = true;
                if (model.Length() != 0 && NOT(brokenModel.Length())) {
                    int pos;

                    needBroken = false;

                    pos = model.Find(".");
                    if (pos < 0) {
                        pos = model.Length();
                    }
                    if (pos > 0) {
                        model.Left(pos, brokenModel);
                    }
                    brokenModel.oPluSet("_broken");
                    if (pos > 0) {
                        brokenModel.oPluSet(model.oGet(pos));
                    }
                }

                // make sure the model gets cached
                if (NOT(renderModelManager.CheckModel(brokenModel))) {
                    if (needBroken) {
                        gameLocal.Error("Model '%s' not found for entity %d(%s)", brokenModel, entityNumber, name);
                    } else {
                        brokenModel.oSet("");
                    }
                }

                GetPhysics().SetContents(spawnArgs.GetBool("nonsolid") ? 0 : CONTENTS_SOLID);

                // make sure the collision model gets cached
                idClipModel.CheckModel(brokenModel);
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
            savefile.WriteRenderLight(renderLight);

            savefile.WriteBool(renderLight.prelightModel != null);

            savefile.WriteVec3(localLightOrigin);
            savefile.WriteMat3(localLightAxis);

            savefile.WriteString(brokenModel);
            savefile.WriteInt(levels[0]);
            savefile.WriteInt(currentLevel);

            savefile.WriteVec3(baseColor);
            savefile.WriteBool(breakOnTrigger);
            savefile.WriteInt(count);
            savefile.WriteInt(triggercount);
            savefile.WriteObject(lightParent);

            savefile.WriteVec4(fadeFrom);
            savefile.WriteVec4(fadeTo);
            savefile.WriteInt(fadeStart);
            savefile.WriteInt(fadeEnd);
            savefile.WriteBool(soundWasPlaying);
        }

        /*
         ================
         idLight::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            boolean[] hadPrelightModel = {false};

            savefile.ReadRenderLight(renderLight);

            savefile.ReadBool(hadPrelightModel);
            renderLight.prelightModel = renderModelManager.CheckModel(va("_prelight_%s", name));
            if ((renderLight.prelightModel == null) && hadPrelightModel[0]) {
                assert (false);
                if (developer.GetBool()) {
                    // we really want to know if this happens
                    gameLocal.Error("idLight::Restore: prelightModel '_prelight_%s' not found", name);
                } else {
                    // but let it slide after release
                    gameLocal.Warning("idLight::Restore: prelightModel '_prelight_%s' not found", name);
                }
            }

            savefile.ReadVec3(localLightOrigin);
            savefile.ReadMat3(localLightAxis);

            savefile.ReadString(brokenModel);
            savefile.ReadInt(levels);
            currentLevel = savefile.ReadInt();

            savefile.ReadVec3(baseColor);
            breakOnTrigger = savefile.ReadBool();
            count = savefile.ReadInt();
            triggercount = savefile.ReadInt();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/lightParent);

            savefile.ReadVec4(fadeFrom);
            savefile.ReadVec4(fadeTo);
            fadeStart = savefile.ReadInt();
            fadeEnd = savefile.ReadInt();
            soundWasPlaying = savefile.ReadBool();

            lightDefHandle = -1;

            SetLightLevel();
        }

        @Override
        public void UpdateChangeableSpawnArgs(final idDict source) {

            super.UpdateChangeableSpawnArgs(source);

            if (source != null) {
                source.Print();
            }
            FreeSoundEmitter(true);
            GameEdit.gameEdit.ParseSpawnArgsToRefSound(source != null ? source : spawnArgs, refSound);
            if (refSound.shader != null && !refSound.waitfortrigger) {
                StartSoundShader(refSound.shader, SND_CHANNEL_ANY, 0, false, null);
            }

            GameEdit.gameEdit.ParseSpawnArgsToRenderLight(source != null ? source : spawnArgs, renderLight);

            UpdateVisuals();
        }

        @Override
        public void Think() {
            idVec4 color = new idVec4();

            if ((thinkFlags & TH_THINK) != 0) {
                if (fadeEnd > 0) {
                    if (gameLocal.time < fadeEnd) {
                        color.Lerp(fadeFrom, fadeTo, (float) (gameLocal.time - fadeStart) / (float) (fadeEnd - fadeStart));
                    } else {
                        color = fadeTo;
                        fadeEnd = 0;
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
            if (lightDefHandle != -1) {
                gameRenderWorld.FreeLightDef(lightDefHandle);
                lightDefHandle = -1;
            }
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            origin = localLightOrigin.oPlus(renderLight.lightCenter);
            axis = localLightAxis.oMultiply(GetPhysics().GetAxis());
            return true;
        }

        @Override
        public void Present() {
            // don't present to the renderer if the entity hasn't changed
            if (0 == (thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }

            // add the model
            super.Present();

            // current transformation
            renderLight.axis.oSet(localLightAxis.oMultiply(GetPhysics().GetAxis()));
            renderLight.origin.oSet(GetPhysics().GetOrigin().oPlus(GetPhysics().GetAxis().oMultiply(localLightOrigin)));

            // reference the sound for shader synced effects
            if (lightParent != null) {
                renderLight.referenceSound = lightParent.GetSoundEmitter();
                renderEntity.referenceSound = lightParent.GetSoundEmitter();
            } else {
                renderLight.referenceSound = refSound.referenceSound;
                renderEntity.referenceSound = refSound.referenceSound;
            }

            // update the renderLight and renderEntity to render the light and flare
            PresentLightDefChange();
            PresentModelDefChange();
        }

        public void SaveState(idDict args) {
            int i, c = spawnArgs.GetNumKeyVals();
            for (i = 0; i < c; i++) {
                final idKeyValue pv = spawnArgs.GetKeyVal(i);
                if (pv.GetKey().Find("editor_", false) >= 0 || pv.GetKey().Find("parse_", false) >= 0) {
                    continue;
                }
                args.Set(pv.GetKey(), pv.GetValue());
            }
        }

        @Override
        public void SetColor(float red, float green, float blue) {
            baseColor.Set(red, green, blue);
            SetLightLevel();
        }

        @Override
        public void SetColor(final idVec4 color) {
            baseColor = color.ToVec3();
            renderLight.shaderParms[ SHADERPARM_ALPHA] = color.oGet(3);
            renderEntity.shaderParms[ SHADERPARM_ALPHA] = color.oGet(3);
            SetLightLevel();
        }

        @Override
        public void GetColor(idVec3 out) {
            out.oSet(0, renderLight.shaderParms[ SHADERPARM_RED]);
            out.oSet(1, renderLight.shaderParms[ SHADERPARM_GREEN]);
            out.oSet(2, renderLight.shaderParms[ SHADERPARM_BLUE]);
        }

        @Override
        public void GetColor(idVec4 out) {
            out.oSet(0, renderLight.shaderParms[ SHADERPARM_RED]);
            out.oSet(1, renderLight.shaderParms[ SHADERPARM_GREEN]);
            out.oSet(2, renderLight.shaderParms[ SHADERPARM_BLUE]);
            out.oSet(3, renderLight.shaderParms[ SHADERPARM_ALPHA]);
        }

        public idVec3 GetBaseColor() {
            return baseColor;
        }

        public void SetShader(final String shadername) {
            // allow this to be NULL
            renderLight.shader = declManager.FindMaterial(shadername, false);
            PresentLightDefChange();
        }

        public void SetLightParm(int parmnum, float value) {
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            renderLight.shaderParms[ parmnum] = value;
            PresentLightDefChange();
        }

        public void SetLightParms(float parm0, float parm1, float parm2, float parm3) {
            renderLight.shaderParms[ SHADERPARM_RED] = parm0;
            renderLight.shaderParms[ SHADERPARM_GREEN] = parm1;
            renderLight.shaderParms[ SHADERPARM_BLUE] = parm2;
            renderLight.shaderParms[ SHADERPARM_ALPHA] = parm3;
            renderEntity.shaderParms[ SHADERPARM_RED] = parm0;
            renderEntity.shaderParms[ SHADERPARM_GREEN] = parm1;
            renderEntity.shaderParms[ SHADERPARM_BLUE] = parm2;
            renderEntity.shaderParms[ SHADERPARM_ALPHA] = parm3;
            PresentLightDefChange();
            PresentModelDefChange();
        }

        public void SetRadiusXYZ(float x, float y, float z) {
            renderLight.lightRadius.oSet(0, x);
            renderLight.lightRadius.oSet(1, y);
            renderLight.lightRadius.oSet(2, z);
            PresentLightDefChange();
        }

        public void SetRadius(float radius) {
            renderLight.lightRadius.oSet(0, renderLight.lightRadius.oSet(1, renderLight.lightRadius.oSet(2, radius)));
            PresentLightDefChange();
        }

        public void On() {
            currentLevel = levels[0];
            // offset the start time of the shader to sync it to the game time
            renderLight.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            if ((soundWasPlaying || refSound.waitfortrigger) && refSound.shader != null) {
                StartSoundShader(refSound.shader, SND_CHANNEL_ANY, 0, false, null);
                soundWasPlaying = false;
            }
            SetLightLevel();
            BecomeActive(TH_UPDATEVISUALS);
        }

        public void Off() {
            currentLevel = 0;
            // kill any sound it was making
            if (refSound.referenceSound != null && refSound.referenceSound.CurrentlyPlaying()) {
                StopSound(etoi(SND_CHANNEL_ANY), false);
                soundWasPlaying = true;
            }
            SetLightLevel();
            BecomeActive(TH_UPDATEVISUALS);
        }

        public void Fade(final idVec4 to, float fadeTime) {
            GetColor(fadeFrom);
            fadeTo = to;
            fadeStart = gameLocal.time;
            fadeEnd = (int) (gameLocal.time + SEC2MS(fadeTime));
            BecomeActive(TH_THINK);
        }

        public void FadeOut(float time) {
            Fade(colorBlack, time);
        }

        public void FadeIn(float time) {
            idVec3 color = new idVec3();
            idVec4 color4 = new idVec4();

            currentLevel = levels[0];
            spawnArgs.GetVector("_color", "1 1 1", color);
            color4.Set(color.x, color.y, color.z, 1.0f);
            Fade(color4, time);
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            BecomeBroken(attacker);
        }

        public void BecomeBroken(idEntity activator) {
            String[] damageDefName = {null};

            fl.takedamage = false;

            if (brokenModel.Length() != 0) {
                SetModel(brokenModel.toString());

                if (!spawnArgs.GetBool("nonsolid")) {
                    GetPhysics().SetClipModel(new idClipModel(brokenModel.toString()), 1.0f);
                    GetPhysics().SetContents(CONTENTS_SOLID);
                }
            } else if (spawnArgs.GetBool("hideModelOnBreak")) {
                SetModel("");
                GetPhysics().SetContents(0);
            }

            if (gameLocal.isServer) {

                ServerSendEvent(EVENT_BECOMEBROKEN, null, true, -1);

                if (spawnArgs.GetString("def_damage", "", damageDefName)) {
                    idVec3 origin = renderEntity.origin.oPlus(renderEntity.bounds.GetCenter().oMultiply(renderEntity.axis));
                    gameLocal.RadiusDamage(origin, activator, activator, this, this, damageDefName[0]);
                }

            }

            ActivateTargets(activator);

            // offset the start time of the shader to sync it to the game time
            renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            renderLight.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            // set the state parm
            renderEntity.shaderParms[ SHADERPARM_MODE] = 1;
            renderLight.shaderParms[ SHADERPARM_MODE] = 1;

            // if the light has a sound, either start the alternate (broken) sound, or stop the sound
            String parm = spawnArgs.GetString("snd_broken");
            if (refSound.shader != null || (parm != null && !parm.isEmpty())) {
                StopSound(etoi(SND_CHANNEL_ANY), false);
                final idSoundShader alternate = refSound.shader != null ? refSound.shader.GetAltSound() : declManager.FindSound(parm);
                if (alternate != null) {
                    // start it with no diversity, so the leadin break sound plays
                    refSound.referenceSound.StartSound(alternate, etoi(SND_CHANNEL_ANY), 0, 0);
                }
            }

            parm = spawnArgs.GetString("mtr_broken");
            if (parm != null && !parm.isEmpty()) {
                SetShader(parm);
            }

            UpdateVisuals();
        }

        public int/*qhandle_t*/ GetLightDefHandle() {
            return lightDefHandle;
        }

        public void SetLightParent(idEntity lparent) {
            lightParent = lparent;
        }

        public void SetLightLevel() {
            idVec3 color;
            float intensity;

            intensity = (float) currentLevel / (float) levels[0];
            color = baseColor.oMultiply(intensity);
            renderLight.shaderParms[ SHADERPARM_RED] = color.oGet(0);
            renderLight.shaderParms[ SHADERPARM_GREEN] = color.oGet(1);
            renderLight.shaderParms[ SHADERPARM_BLUE] = color.oGet(2);
            renderEntity.shaderParms[ SHADERPARM_RED] = color.oGet(0);
            renderEntity.shaderParms[ SHADERPARM_GREEN] = color.oGet(1);
            renderEntity.shaderParms[ SHADERPARM_BLUE] = color.oGet(2);
            PresentLightDefChange();
            PresentModelDefChange();
        }

        @Override
        public void ShowEditingDialog() {
            if (g_editEntityMode.GetInteger() == 1) {
                common.InitTool(EDITOR_LIGHT, spawnArgs);
            } else {
                common.InitTool(EDITOR_SOUND, spawnArgs);
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

            msg.WriteByte(currentLevel);
            msg.WriteLong((int) PackColor(baseColor));
            // msg.WriteBits( lightParent.GetEntityNum(), GENTITYNUM_BITS );

            /*	// only helps prediction
             msg.WriteLong( PackColor( fadeFrom ) );
             msg.WriteLong( PackColor( fadeTo ) );
             msg.WriteLong( fadeStart );
             msg.WriteLong( fadeEnd );
             */
            // FIXME: send renderLight.shader
            msg.WriteFloat(renderLight.lightRadius.oGet(0), 5, 10);
            msg.WriteFloat(renderLight.lightRadius.oGet(1), 5, 10);
            msg.WriteFloat(renderLight.lightRadius.oGet(2), 5, 10);

            msg.WriteLong((int) PackColor(new idVec4(
                    renderLight.shaderParms[SHADERPARM_RED],
                    renderLight.shaderParms[SHADERPARM_GREEN],
                    renderLight.shaderParms[SHADERPARM_BLUE],
                    renderLight.shaderParms[SHADERPARM_ALPHA])));

            msg.WriteFloat(renderLight.shaderParms[SHADERPARM_TIMESCALE], 5, 10);
            msg.WriteLong((int) renderLight.shaderParms[SHADERPARM_TIMEOFFSET]);
            //msg.WriteByte( renderLight.shaderParms[SHADERPARM_DIVERSITY] );
            msg.WriteShort((int) renderLight.shaderParms[SHADERPARM_MODE]);

            WriteColorToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            idVec4 shaderColor = new idVec4();
            int oldCurrentLevel = currentLevel;
            idVec3 oldBaseColor = baseColor;

            GetPhysics().ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);

            currentLevel = msg.ReadByte();
            if (currentLevel != oldCurrentLevel) {
                // need to call On/Off for flickering lights to start/stop the sound
                // while doing it this way rather than through events, the flickering is out of sync between clients
                // but at least there is no question about saving the event and having them happening globally in the world
                if (currentLevel != 0) {
                    On();
                } else {
                    Off();
                }
            }
            UnpackColor(msg.ReadLong(), baseColor);
            // lightParentEntityNum = msg.ReadBits( GENTITYNUM_BITS );	

            /*	// only helps prediction
             UnpackColor( msg.ReadLong(), fadeFrom );
             UnpackColor( msg.ReadLong(), fadeTo );
             fadeStart = msg.ReadLong();
             fadeEnd = msg.ReadLong();
             */
            // FIXME: read renderLight.shader
            renderLight.lightRadius.oSet(0, msg.ReadFloat(5, 10));
            renderLight.lightRadius.oSet(1, msg.ReadFloat(5, 10));
            renderLight.lightRadius.oSet(2, msg.ReadFloat(5, 10));

            UnpackColor(msg.ReadLong(), shaderColor);
            renderLight.shaderParms[SHADERPARM_RED] = shaderColor.oGet(0);
            renderLight.shaderParms[SHADERPARM_GREEN] = shaderColor.oGet(1);
            renderLight.shaderParms[SHADERPARM_BLUE] = shaderColor.oGet(2);
            renderLight.shaderParms[SHADERPARM_ALPHA] = shaderColor.oGet(3);

            renderLight.shaderParms[SHADERPARM_TIMESCALE] = msg.ReadFloat(5, 10);
            renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = msg.ReadLong();
            //renderLight.shaderParms[SHADERPARM_DIVERSITY] = msg.ReadFloat();
            renderLight.shaderParms[SHADERPARM_MODE] = msg.ReadShort();

            ReadColorFromSnapshot(msg);

            if (msg.HasChanged()) {
                if ((currentLevel != oldCurrentLevel) || (baseColor != oldBaseColor)) {
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
            if ((lightDefHandle != -1)) {
                gameRenderWorld.UpdateLightDef(lightDefHandle, renderLight);
            } else {
                lightDefHandle = gameRenderWorld.AddLightDef(renderLight);
            }
        }

        private void PresentModelDefChange() {

            if (null == renderEntity.hModel || IsHidden()) {
                return;
            }

            // add to refresh list
            if (modelDefHandle == -1) {
                modelDefHandle = gameRenderWorld.AddEntityDef(renderEntity);
            } else {
                gameRenderWorld.UpdateEntityDef(modelDefHandle, renderEntity);
            }
        }

        private void Event_SetShader(final idEventArg<String> shadername) {
            SetShader(shadername.value);
        }

        private void Event_GetLightParm(idEventArg<Integer> _parmnum) {
            int parmnum = _parmnum.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            idThread.ReturnFloat(renderLight.shaderParms[ parmnum]);
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
            triggercount++;
            if (triggercount < count) {
                return;
            }

            // reset trigger count
            triggercount = 0;

            if (breakOnTrigger) {
                BecomeBroken(activator.value);
                breakOnTrigger = false;
                return;
            }

            if (0 == currentLevel) {
                On();
            } else {
                currentLevel--;
                if (0 == currentLevel) {
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

            if (NOT(refSound.referenceSound)) {
                return;
            }

            for (i = 0; i < targets.Num(); i++) {
                targetEnt = targets.oGet(i).GetEntity();
                if (targetEnt != null && targetEnt.IsType(idLight.class)) {
                    idLight light = (idLight) targetEnt;
                    light.lightParent = this;

                    // explicitly delete any sounds on the entity
                    light.FreeSoundEmitter(true);

                    // manually set the refSound to this light's refSound
                    light.renderEntity.referenceSound = renderEntity.referenceSound;

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

    };
}
