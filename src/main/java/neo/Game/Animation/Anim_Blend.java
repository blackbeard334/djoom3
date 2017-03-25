package neo.Game.Animation;

import static java.lang.Character.isDigit;
import java.util.stream.Stream;
import static neo.Game.AI.AI_Events.AI_AttackMelee;
import static neo.Game.AI.AI_Events.AI_AttackMissile;
import static neo.Game.AI.AI_Events.AI_BeginAttack;
import static neo.Game.AI.AI_Events.AI_CreateMissile;
import static neo.Game.AI.AI_Events.AI_DirectDamage;
import static neo.Game.AI.AI_Events.AI_DisableClip;
import static neo.Game.AI.AI_Events.AI_DisableGravity;
import static neo.Game.AI.AI_Events.AI_EnableClip;
import static neo.Game.AI.AI_Events.AI_EnableGravity;
import static neo.Game.AI.AI_Events.AI_EndAttack;
import static neo.Game.AI.AI_Events.AI_FireMissileAtTarget;
import static neo.Game.AI.AI_Events.AI_JumpFrame;
import static neo.Game.AI.AI_Events.AI_MuzzleFlash;
import static neo.Game.AI.AI_Events.AI_TriggerParticles;
import static neo.Game.Actor.AI_DisableEyeFocus;
import static neo.Game.Actor.AI_EnableEyeFocus;
import static neo.Game.Actor.EV_DisableLegIK;
import static neo.Game.Actor.EV_DisableWalkIK;
import static neo.Game.Actor.EV_EnableLegIK;
import static neo.Game.Actor.EV_EnableWalkIK;
import static neo.Game.Actor.EV_Footstep;
import static neo.Game.Actor.EV_FootstepLeft;
import static neo.Game.Actor.EV_FootstepRight;
import neo.Game.Animation.Anim.AFJointModType_t;
import static neo.Game.Animation.Anim.AFJointModType_t.AF_JOINTMOD_AXIS;
import static neo.Game.Animation.Anim.AFJointModType_t.AF_JOINTMOD_BOTH;
import static neo.Game.Animation.Anim.AFJointModType_t.AF_JOINTMOD_ORIGIN;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.ANIMCHANNEL_EYELIDS;
import static neo.Game.Animation.Anim.ANIM_MaxAnimsPerChannel;
import static neo.Game.Animation.Anim.ANIM_MaxSyncedAnims;
import static neo.Game.Animation.Anim.ANIM_NumAnimChannels;
import static neo.Game.Animation.Anim.FRAME2MS;
import neo.Game.Animation.Anim.animFlags_t;
import neo.Game.Animation.Anim.frameBlend_t;
import neo.Game.Animation.Anim.frameCommandType_t;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_AVIGAME;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_BEGINATTACK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_CREATEMISSILE;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DIRECTDAMAGE;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DISABLE_CLIP;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DISABLE_EYE_FOCUS;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DISABLE_GRAVITY;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DISABLE_LEG_IK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_DISABLE_WALK_IK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENABLE_CLIP;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENABLE_EYE_FOCUS;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENABLE_GRAVITY;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENABLE_LEG_IK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENABLE_WALK_IK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_ENDATTACK;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_EVENTFUNCTION;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_FIREMISSILEATTARGET;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_FOOTSTEP;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_FX;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_JUMP;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_LAUNCHMISSILE;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_LEFTFOOT;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_MELEE;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_MUZZLEFLASH;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_RECORDDEMO;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_RIGHTFOOT;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SCRIPTFUNCTION;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SCRIPTFUNCTIONOBJECT;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SKIN;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_BODY;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_BODY2;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_BODY3;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_CHATTER;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_GLOBAL;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_ITEM;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_VOICE;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_VOICE2;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_SOUND_WEAPON;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_TRIGGER;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_TRIGGER_SMOKE_PARTICLE;
import neo.Game.Animation.Anim.frameCommand_t;
import neo.Game.Animation.Anim.frameLookup_t;
import neo.Game.Animation.Anim.idAFPoseJointMod;
import neo.Game.Animation.Anim.idMD5Anim;
import neo.Game.Animation.Anim.jointInfo_t;
import neo.Game.Animation.Anim.jointModTransform_t;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_LOCAL;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_LOCAL_OVERRIDE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_NONE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import neo.Game.Animation.Anim.jointMod_t;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.TH_ANIMATE;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idEntity;
import static neo.Game.Entity.signalNum_t.SIG_TRIGGER;
import neo.Game.FX.idEntityFx;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_debugAnim;
import static neo.Game.GameSys.SysCvar.g_debugBounds;
import static neo.Game.Game_local.animationLib;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY3;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ITEM;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_WEAPON;
import static neo.Game.Sound.SSF_GLOBAL;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.Model.MD5_MESH_EXT;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.etoi;
import static neo.TempDump.indexOf;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itoi;
import static neo.TempDump.sizeof;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_RENDERER;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declState_t.DS_DEFAULTED;
import static neo.framework.DeclManager.declType_t.DECL_FX;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_FLOAT;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.Text.Token.TT_PUNCTUATION;
import neo.idlib.Text.Token.idToken;
import static neo.idlib.containers.BinSearch.idBinSearch_GreaterEqual;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Quat.idQuat;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Anim_Blend {

    public static final String[] channelNames/*[ ANIM_NumAnimChannels ]*/ = {
                "all", "torso", "legs", "head", "eyelids"
            };

    static final boolean VELOCITY_MOVE = false;

    /*
     ==============================================================================================

     idAnim

     ==============================================================================================
     */
    public static class idAnim {

        private idDeclModelDef modelDef;
        private idMD5Anim[] anims = new idMD5Anim[ANIM_MaxSyncedAnims];
        private int                   numAnims;
        private idStr                 name = new idStr();
        private idStr                 realname = new idStr();
        private idList<frameLookup_t> frameLookup = new idList();
        private idList<frameCommand_t> frameCommands = new idList<>(frameCommand_t.class);
        private animFlags_t flags;
//
//

        public idAnim() {
            modelDef = null;
            numAnims = 0;
//	memset( anims, 0, sizeof( anims ) );
//	memset( &flags, 0, sizeof( flags ) );
            flags = new animFlags_t();
        }

        public idAnim(final idDeclModelDef modelDef, final idAnim anim) {
            int i;

            this.modelDef = modelDef;
            numAnims = anim.numAnims;
            name = anim.name;
            realname = anim.realname;
            flags = anim.flags;

            anims = new idMD5Anim[anims.length];
            for (i = 0; i < numAnims; i++) {
                anims[i] = anim.anims[i];
                anims[i].IncreaseRefs();
            }

            frameLookup.SetNum(anim.frameLookup.Num());
            if (frameLookup.Num() > 0) {
                System.arraycopy(anim.frameLookup.Ptr(), 0, frameLookup.Ptr(), 0, frameLookup.MemoryUsed());
            }

            frameCommands.SetNum(anim.frameCommands.Num());
            for (i = 0; i < frameCommands.Num(); i++) {
                frameCommands.oSet(i, anim.frameCommands.oGet(i));
                if (frameCommands.oGet(i).string != null) {
                    frameCommands.oGet(i).string = new idStr(anim.frameCommands.oGet(i).string);
                }
            }
        }

        // ~idAnim();
        public void SetAnim(final idDeclModelDef modelDef, final String sourceName, final String animName, int num, final idMD5Anim[] md5anims/*[ ANIM_MaxSyncedAnims ]*/) {
            int i;

            this.modelDef = modelDef;

            for (i = 0; i < numAnims; i++) {
                anims[i].DecreaseRefs();
                anims[i] = null;
            }

            assert ((num > 0) && (num <= ANIM_MaxSyncedAnims));
            numAnims = num;
            realname.oSet(sourceName);
            name.oSet(animName);

            for (i = 0; i < num; i++) {
                anims[i] = md5anims[i];
                anims[i].IncreaseRefs();
            }

//	memset( &flags, 0, sizeof( flags ) );
            flags = new animFlags_t();

            for (i = 0; i < frameCommands.Num(); i++) {
//                delete frameCommands[ i ].string;
            }

            frameLookup.Clear();
            frameCommands.Clear();
        }

        public String Name() {
            return name.toString();
        }

        public String FullName() {
            return realname.toString();
        }


        /*
         =====================
         idAnim::MD5Anim

         index 0 will never be NULL.  Any anim >= NumAnims will return NULL.
         =====================
         */
        public idMD5Anim MD5Anim(int num) {
            if (anims == null || anims[0] == null) {
                return null;
            }
            return anims[num];
        }

        public idDeclModelDef modelDef() {
            return modelDef;
        }

        public int Length() {
            if (null == anims[0]) {
                return 0;
            }

            return anims[0].Length();
        }

        public int NumFrames() {
            if (null == anims[0]) {
                return 0;
            }

            return anims[0].NumFrames();
        }

        public int NumAnims() {
            return numAnims;
        }

        public idVec3 TotalMovementDelta() {
            if (null == anims[0]) {
                return getVec3_zero();
            }

            return anims[0].TotalMovementDelta();
        }

        public boolean GetOrigin(idVec3 offset, int animNum, int currentTime, int cyclecount) {
            if (null == anims[animNum]) {
                offset.Zero();
                return false;
            }

            anims[animNum].GetOrigin(offset, currentTime, cyclecount);
            return true;
        }

        public boolean GetOriginRotation(idQuat rotation, int animNum, int currentTime, int cyclecount) {
            if (null == anims[animNum]) {
                rotation.Set(0.0f, 0.0f, 0.0f, 1.0f);
                return false;
            }

            anims[animNum].GetOriginRotation(rotation, currentTime, cyclecount);
            return true;
        }

        public boolean GetBounds(idBounds bounds, int animNum, int currentTime, int cyclecount) {
            if (null == anims[animNum]) {
                return false;
            }

            anims[animNum].GetBounds(bounds, currentTime, cyclecount);
            return true;
        }


        /*
         =====================
         idAnim::AddFrameCommand

         Returns NULL if no error.
         =====================
         */
        public String AddFrameCommand(final idDeclModelDef modelDef, int framenum, idLexer src, final idDict def) throws idException {
            int i;
            int index;
            idStr text;
            idStr funcname;
            frameCommand_t fc;
            idToken token = new idToken();
            jointInfo_t jointInfo;

            // make sure we're within bounds
            if ((framenum < 1) || (framenum > anims[0].NumFrames())) {
                return va("Frame %d out of range", framenum);
            }

            // frame numbers are 1 based in .def files, but 0 based internally
            framenum--;

//	memset( &fc, 0, sizeof( fc ) );
            fc = new frameCommand_t();

            if (!src.ReadTokenOnLine(token)) {
                return "Unexpected end of line";
            }
            if (token.equals("call")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SCRIPTFUNCTION;
                fc.function = gameLocal.program.FindFunction(token.toString());
                if (NOT(fc.function)) {
                    return va("Function '%s' not found", token);
                }
            } else if (token.equals("object_call")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SCRIPTFUNCTIONOBJECT;
                fc.string = new idStr(token);
            } else if (token.equals("event")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_EVENTFUNCTION;
                final idEventDef ev = idEventDef.FindEvent(token.toString());
                if (null == ev) {
                    return va("Event '%s' not found", token);
                }
                if (ev.GetNumArgs() != 0) {
                    return va("Event '%s' has arguments", token);
                }
                fc.string = new idStr(token);
            } else if (token.equals("sound")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_voice")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_VOICE;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_voice2")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_VOICE2;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_body")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_BODY;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_body2")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_BODY2;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_body3")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_BODY3;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_weapon")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_WEAPON;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_global")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_GLOBAL;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_item")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_ITEM;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("sound_chatter")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SOUND_CHATTER;
                if (0 == token.Cmpn("snd_", 4)) {
                    fc.string = new idStr(token);
                } else {
                    fc.soundShader = declManager.FindSound(token);
                    if (fc.soundShader.GetState() == DS_DEFAULTED) {
                        gameLocal.Warning("Sound '%s' not found", token);
                    }
                }
            } else if (token.equals("skin")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_SKIN;
                if (token.equals("none")) {
                    fc.skin = null;
                } else {
                    fc.skin = declManager.FindSkin(token);
                    if (NOT(fc.skin)) {
                        return va("Skin '%s' not found", token);
                    }
                }
            } else if (token.equals("fx")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_FX;
                if (NOT(declManager.FindType(DECL_FX, token))) {
                    return va("fx '%s' not found", token);
                }
                fc.string = new idStr(token);
            } else if (token.equals("trigger")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_TRIGGER;
                fc.string = new idStr(token);
            } else if (token.equals("triggerSmokeParticle")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_TRIGGER_SMOKE_PARTICLE;
                fc.string = new idStr(token);
            } else if (token.equals("melee")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_MELEE;
                if (NOT(gameLocal.FindEntityDef(token.toString(), false))) {
                    return va("Unknown entityDef '%s'", token);
                }
                fc.string = new idStr(token);
            } else if (token.equals("direct_damage")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_DIRECTDAMAGE;
                if (NOT(gameLocal.FindEntityDef(token.toString(), false))) {
                    return va("Unknown entityDef '%s'", token);
                }
                fc.string = new idStr(token);
            } else if (token.equals("attack_begin")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_BEGINATTACK;
                if (NOT(gameLocal.FindEntityDef(token.toString(), false))) {
                    return va("Unknown entityDef '%s'", token);
                }
                fc.string = new idStr(token);
            } else if (token.equals("attack_end")) {
                fc.type = FC_ENDATTACK;
            } else if (token.equals("muzzle_flash")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                if (!token.IsEmpty() && NOT(modelDef.FindJoint(token.toString()))) {
                    return va("Joint '%s' not found", token);
                }
                fc.type = FC_MUZZLEFLASH;
                fc.string = new idStr(token);
            } else if (token.equals("muzzle_flash")) {
                fc.type = FC_MUZZLEFLASH;
                fc.string = new idStr("");
            } else if (token.equals("create_missile")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                if (NOT(modelDef.FindJoint(token.toString()))) {
                    return va("Joint '%s' not found", token);
                }
                fc.type = FC_CREATEMISSILE;
                fc.string = new idStr(token);
            } else if (token.equals("launch_missile")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                if (NOT(modelDef.FindJoint(token.toString()))) {
                    return va("Joint '%s' not found", token);
                }
                fc.type = FC_LAUNCHMISSILE;
                fc.string = new idStr(token);
            } else if (token.equals("fire_missile_at_target")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                jointInfo = modelDef.FindJoint(token.toString());
                if (NOT(jointInfo)) {
                    return va("Joint '%s' not found", token);
                }
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_FIREMISSILEATTARGET;
                fc.string = new idStr(token);
                fc.index = jointInfo.num;
            } else if (token.equals("footstep")) {
                fc.type = FC_FOOTSTEP;
            } else if (token.equals("leftfoot")) {
                fc.type = FC_LEFTFOOT;
            } else if (token.equals("rightfoot")) {
                fc.type = FC_RIGHTFOOT;
            } else if (token.equals("enableEyeFocus")) {
                fc.type = FC_ENABLE_EYE_FOCUS;
            } else if (token.equals("disableEyeFocus")) {
                fc.type = FC_DISABLE_EYE_FOCUS;
            } else if (token.equals("disableGravity")) {
                fc.type = FC_DISABLE_GRAVITY;
            } else if (token.equals("enableGravity")) {
                fc.type = FC_ENABLE_GRAVITY;
            } else if (token.equals("jump")) {
                fc.type = FC_JUMP;
            } else if (token.equals("enableClip")) {
                fc.type = FC_ENABLE_CLIP;
            } else if (token.equals("disableClip")) {
                fc.type = FC_DISABLE_CLIP;
            } else if (token.equals("enableWalkIK")) {
                fc.type = FC_ENABLE_WALK_IK;
            } else if (token.equals("disableWalkIK")) {
                fc.type = FC_DISABLE_WALK_IK;
            } else if (token.equals("enableLegIK")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_ENABLE_LEG_IK;
                fc.index = atoi(token.toString());
            } else if (token.equals("disableLegIK")) {
                if (!src.ReadTokenOnLine(token)) {
                    return "Unexpected end of line";
                }
                fc.type = FC_DISABLE_LEG_IK;
                fc.index = atoi(token.toString());
            } else if (token.equals("recordDemo")) {
                fc.type = FC_RECORDDEMO;
                if (src.ReadTokenOnLine(token)) {
                    fc.string = new idStr(token);
                }
            } else if (token.equals("aviGame")) {
                fc.type = FC_AVIGAME;
                if (src.ReadTokenOnLine(token)) {
                    fc.string = new idStr(token);
                }
            } else {
                return va("Unknown command '%s'", token);
            }

            // check if we've initialized the frame loopup table
            if (0 == frameLookup.Num()) {
                // we haven't, so allocate the table and initialize it
                frameLookup.SetGranularity(1);
                frameLookup.SetNum(anims[ 0].NumFrames());
                for (i = 0; i < frameLookup.Num(); i++) {
                    frameLookup.oSet(i, new frameLookup_t()).num = 0;
                    frameLookup.oGet(i).firstCommand = 0;
                }
            }

            // allocate space for a new command
            frameCommands.Alloc();

            // calculate the index of the new command
            index = frameLookup.oGet(framenum).firstCommand + frameLookup.oGet(framenum).num;

            // move all commands from our index onward up one to give us space for our new command
            for (i = frameCommands.Num() - 1; i > index; i--) {
                frameCommands.oSet(i, frameCommands.oGet(i - 1));
            }

            // fix the indices of any later frames to account for the inserted command
            for (i = framenum + 1; i < frameLookup.Num(); i++) {
                frameLookup.oGet(i).firstCommand++;
            }

            // store the new command 
            frameCommands.oSet(index, fc);

            // increase the number of commands on this frame
            frameLookup.oGet(framenum).num++;

            // return with no error
            return null;
        }

        public void CallFrameCommands(idEntity ent, int from, int to) {
            int index;
            int end;
            int frame;
            int numframes;

            numframes = anims[ 0].NumFrames();

            frame = from;
            while (frame != to) {
                frame++;
                if (frame >= numframes) {
                    frame = 0;
                }

                index = frameLookup.oGet(frame).firstCommand;
                end = index + frameLookup.oGet(frame).num;
                while (index < end) {
                    final frameCommand_t command = frameCommands.oGet(index++);
                    switch (command.type) {
                        case FC_SCRIPTFUNCTION: {
                            gameLocal.CallFrameCommand(ent, command.function);
                            break;
                        }
                        case FC_SCRIPTFUNCTIONOBJECT: {
                            gameLocal.CallObjectFrameCommand(ent, command.string.toString());
                            break;
                        }
                        case FC_EVENTFUNCTION: {
                            final idEventDef ev = idEventDef.FindEvent(command.string.toString());
                            ent.ProcessEvent(ev);
                            break;
                        }
                        case FC_SOUND: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_ANY, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_ANY, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_VOICE: {
                            if (NOT(command.soundShader)) {
                                if (NOT(ent.StartSound(command.string.toString(), SND_CHANNEL_VOICE, 0, false, null))) {
                                    gameLocal.Warning("Framecommand 'sound_voice' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_VOICE, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_VOICE2: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_VOICE2, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_voice2' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_VOICE2, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_BODY: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_BODY, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_body' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_BODY, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_BODY2: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_BODY2, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_body2' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_BODY2, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_BODY3: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_BODY3, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_body3' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_BODY3, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_WEAPON: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_WEAPON, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_weapon' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_WEAPON, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_GLOBAL: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_ANY, SSF_GLOBAL, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_global' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_ITEM: {
                            if (NOT(command.soundShader)) {
                                if (!ent.StartSound(command.string.toString(), SND_CHANNEL_ITEM, 0, false, null)) {
                                    gameLocal.Warning("Framecommand 'sound_item' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                            ent.name, FullName(), frame + 1, command.string);
                                }
                            } else {
                                ent.StartSoundShader(command.soundShader, SND_CHANNEL_ITEM, 0, false, null);
                            }
                            break;
                        }
                        case FC_SOUND_CHATTER: {
                            if (ent.CanPlayChatterSounds()) {
                                if (NOT(command.soundShader)) {
                                    if (!ent.StartSound(command.string.toString(), SND_CHANNEL_VOICE, 0, false, null)) {
                                        gameLocal.Warning("Framecommand 'sound_chatter' on entity '%s', anim '%s', frame %d: Could not find sound '%s'",
                                                ent.name, FullName(), frame + 1, command.string);
                                    }
                                } else {
                                    ent.StartSoundShader(command.soundShader, SND_CHANNEL_VOICE, 0, false, null);
                                }
                            }
                            break;
                        }
                        case FC_FX: {
                            idEntityFx.StartFx(command.string.toString(), null, null, ent, true);
                            break;
                        }
                        case FC_SKIN: {
                            ent.SetSkin(command.skin);
                            break;
                        }
                        case FC_TRIGGER: {
                            idEntity target;

                            target = gameLocal.FindEntity(command.string.toString());
                            if (target != null) {
                                target.Signal(SIG_TRIGGER);
                                target.ProcessEvent(EV_Activate, ent);
                                target.TriggerGuis();
                            } else {
                                gameLocal.Warning("Framecommand 'trigger' on entity '%s', anim '%s', frame %d: Could not find entity '%s'",
                                        ent.name, FullName(), frame + 1, command.string);
                            }
                            break;
                        }
                        case FC_TRIGGER_SMOKE_PARTICLE: {
                            ent.ProcessEvent(AI_TriggerParticles, command.string.c_str());
                            break;
                        }
                        case FC_MELEE: {
                            ent.ProcessEvent(AI_AttackMelee, command.string.c_str());
                            break;
                        }
                        case FC_DIRECTDAMAGE: {
                            ent.ProcessEvent(AI_DirectDamage, command.string.c_str());
                            break;
                        }
                        case FC_BEGINATTACK: {
                            ent.ProcessEvent(AI_BeginAttack, command.string.c_str());
                            break;
                        }
                        case FC_ENDATTACK: {
                            ent.ProcessEvent(AI_EndAttack);
                            break;
                        }
                        case FC_MUZZLEFLASH: {
                            ent.ProcessEvent(AI_MuzzleFlash, command.string.c_str());
                            break;
                        }
                        case FC_CREATEMISSILE: {
                            ent.ProcessEvent(AI_CreateMissile, command.string.c_str());
                            break;
                        }
                        case FC_LAUNCHMISSILE: {
                            ent.ProcessEvent(AI_AttackMissile, command.string.c_str());
                            break;
                        }
                        case FC_FIREMISSILEATTARGET: {
                            ent.ProcessEvent(AI_FireMissileAtTarget, modelDef.GetJointName(command.index), command.string.c_str());
                            break;
                        }
                        case FC_FOOTSTEP: {
                            ent.ProcessEvent(EV_Footstep);
                            break;
                        }
                        case FC_LEFTFOOT: {
                            ent.ProcessEvent(EV_FootstepLeft);
                            break;
                        }
                        case FC_RIGHTFOOT: {
                            ent.ProcessEvent(EV_FootstepRight);
                            break;
                        }
                        case FC_ENABLE_EYE_FOCUS: {
                            ent.ProcessEvent(AI_EnableEyeFocus);
                            break;
                        }
                        case FC_DISABLE_EYE_FOCUS: {
                            ent.ProcessEvent(AI_DisableEyeFocus);
                            break;
                        }
                        case FC_DISABLE_GRAVITY: {
                            ent.ProcessEvent(AI_DisableGravity);
                            break;
                        }
                        case FC_ENABLE_GRAVITY: {
                            ent.ProcessEvent(AI_EnableGravity);
                            break;
                        }
                        case FC_JUMP: {
                            ent.ProcessEvent(AI_JumpFrame);
                            break;
                        }
                        case FC_ENABLE_CLIP: {
                            ent.ProcessEvent(AI_EnableClip);
                            break;
                        }
                        case FC_DISABLE_CLIP: {
                            ent.ProcessEvent(AI_DisableClip);
                            break;
                        }
                        case FC_ENABLE_WALK_IK: {
                            ent.ProcessEvent(EV_EnableWalkIK);
                            break;
                        }
                        case FC_DISABLE_WALK_IK: {
                            ent.ProcessEvent(EV_DisableWalkIK);
                            break;
                        }
                        case FC_ENABLE_LEG_IK: {
                            ent.ProcessEvent(EV_EnableLegIK, command.index);
                            break;
                        }
                        case FC_DISABLE_LEG_IK: {
                            ent.ProcessEvent(EV_DisableLegIK, command.index);
                            break;
                        }
                        case FC_RECORDDEMO: {
                            if (command.string != null) {
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("recordDemo %s", command.string));
                            } else {
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "stoprecording");
                            }
                            break;
                        }
                        case FC_AVIGAME: {
                            if (command.string != null) {
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("aviGame %s", command.string));
                            } else {
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "aviGame");
                            }
                            break;
                        }
                    }
                }
            }
        }

        public boolean HasFrameCommands() {
            return (0 != frameCommands.Num());
        }

        // returns first frame (zero based) that command occurs.  returns -1 if not found.
        public int FindFrameForFrameCommand(frameCommandType_t framecommand, final frameCommand_t[] command) {
            int frame;
            int index;
            int numframes;
            int end;

            if (0 == frameCommands.Num()) {
                return -1;
            }

            numframes = anims[ 0].NumFrames();
            for (frame = 0; frame < numframes; frame++) {
                end = frameLookup.oGet(frame).firstCommand + frameLookup.oGet(frame).num;
                for (index = frameLookup.oGet(frame).firstCommand; index < end; index++) {
                    if (frameCommands.oGet(index).type == framecommand) {
                        if (command != null) {
                            command[0] = frameCommands.oGet(index);
                        }
                        return frame;
                    }
                }
            }

            if (command != null) {
                command[0] = null;
            }

            return -1;
        }

        public void SetAnimFlags(final animFlags_t animflags) {
            flags = animflags;
        }

        public animFlags_t GetAnimFlags() {
            return flags;
        }
    };

    /*
     ==============================================================================================

     idDeclModelDef

     ==============================================================================================
     */
    public static class idDeclModelDef extends idDecl {

        private idVec3 offset;
        private idList<jointInfo_t> joints;
        private idList<Integer> jointParents;
        private idList<Integer>[] channelJoints = new idList[ANIM_NumAnimChannels];
        private idRenderModel modelHandle;
        private idList<idAnim> anims;
        private idDeclSkin skin;
        //
        //

        public idDeclModelDef() {
            offset = new idVec3();
            joints = new idList<>();
            jointParents = new idList<>();
            for (int i = 0; i < ANIM_NumAnimChannels; i++) {
                channelJoints[i] = new idList<>();
            }
            modelHandle = null;
            anims = new idList<>();
            skin = null;
        }
        // ~idDeclModelDef();

        @Override
        public long/*size_t*/ Size() {
            return sizeof(idDeclModelDef.class);
        }

        @Override
        public String DefaultDefinition() {
            return "{ }";
        }

        @Override
        public boolean Parse(final String text, final int textLength) throws idException {
            int i;
            int num;
            idStr filename;
            idStr extension = new idStr();
            int md5joint;
            idMD5Joint[] md5joints;
            idLexer src = new idLexer();
            idToken token = new idToken();
            idToken token2 = new idToken();
            String jointnames;
            int channel;
            int/*jointHandle_t*/ jointnum;
            idList<Integer/*jointHandle_t*/> jointList = new idList<>();
            int numDefaultAnims;

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            numDefaultAnims = 0;
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (0 == token.Icmp("}")) {
                    break;
                }

                if (token.equals("inherit")) {
                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }

                    final idDeclModelDef copy = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, token2, false);
                    if (null == copy) {
                        common.Warning("Unknown model definition '%s'", token2);
                    } else if (copy.GetState() == DS_DEFAULTED) {
                        common.Warning("inherited model definition '%s' defaulted", token2);
                        MakeDefault();
                        return false;
                    } else {
                        CopyDecl(copy);
                        numDefaultAnims = anims.Num();
                    }
                } else if (token.equals("skin")) {
                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }
                    skin = declManager.FindSkin(token2);
                    if (null == skin) {
                        src.Warning("Skin '%s' not found", token2);
                        MakeDefault();
                        return false;
                    }
                } else if (token.equals("mesh")) {
                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }
                    filename = token2;
                    filename.ExtractFileExtension(extension);
                    if (!extension.equals(MD5_MESH_EXT)) {
                        src.Warning("Invalid model for MD5 mesh");
                        MakeDefault();
                        return false;
                    }
                    modelHandle = renderModelManager.FindModel(filename.toString());
                    if (null == modelHandle) {
                        src.Warning("Model '%s' not found", filename);
                        MakeDefault();
                        return false;
                    }

                    if (modelHandle.IsDefaultModel()) {
                        src.Warning("Model '%s' defaulted", filename);
                        MakeDefault();
                        return false;
                    }

                    // get the number of joints
                    num = modelHandle.NumJoints();
                    if (0 == num) {
                        src.Warning("Model '%s' has no joints", filename);
                    }

                    // set up the joint hierarchy
                    joints.SetGranularity(1);
                    joints.SetNum(num);
                    jointParents.SetNum(num);
                    channelJoints[0].SetNum(num);
                    md5joints = modelHandle.GetJoints();
                    md5joint = 0;//md5joints;
                    for (i = 0; i < num; i++, md5joint++) {
                        joints.oSet(i, new jointInfo_t()).channel = ANIMCHANNEL_ALL;
                        joints.oGet(i).num = i;
                        if (md5joints[md5joint].parent != null) {
                            joints.oGet(i).parentNum = indexOf(md5joints[md5joint].parent, md5joints);
                        } else {
                            joints.oGet(i).parentNum = INVALID_JOINT;
                        }
                        jointParents.oSet(i, joints.oGet(i).parentNum);
                        channelJoints[0].oSet(i, i);
                    }
                } else if (token.equals("remove")) {
                    // removes any anims whos name matches
                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }
                    num = 0;
                    for (i = 0; i < anims.Num(); i++) {
                        if ((token2.equals(anims.oGet(i).Name())) || (token2.equals(anims.oGet(i).FullName()))) {
//					delete anims[ i ];
                            anims.RemoveIndex(i);
                            if (i >= numDefaultAnims) {
                                src.Warning("Anim '%s' was not inherited.  Anim should be removed from the model def.", token2);
                                MakeDefault();
                                return false;
                            }
                            i--;
                            numDefaultAnims--;
                            num++;
                            continue;
                        }
                    }
                    if (0 == num) {
                        src.Warning("Couldn't find anim '%s' to remove", token2);
                        MakeDefault();
                        return false;
                    }
                } else if (token.equals("anim")) {
                    if (null == modelHandle) {
                        src.Warning("Must specify mesh before defining anims");
                        MakeDefault();
                        return false;
                    }
                    if (!ParseAnim(src, numDefaultAnims)) {
                        MakeDefault();
                        return false;
                    }
                } else if (token.equals("offset")) {
                    if (!src.Parse1DMatrix(3, offset)) {
                        src.Warning("Expected vector following 'offset'");
                        MakeDefault();
                        return false;
                    }
                } else if (token.equals("channel")) {
                    if (null == modelHandle) {
                        src.Warning("Must specify mesh before defining channels");
                        MakeDefault();
                        return false;
                    }

                    // set the channel for a group of joints
                    if (!src.ReadToken(token2)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }
                    if (!src.CheckTokenString("(")) {
                        src.Warning("Expected { after '%s'\n", token2);
                        MakeDefault();
                        return false;
                    }

                    for (i = ANIMCHANNEL_ALL + 1; i < ANIM_NumAnimChannels; i++) {
                        if (0 == idStr.Icmp(channelNames[ i], token2.toString())) {
                            break;
                        }
                    }

                    if (i >= ANIM_NumAnimChannels) {
                        src.Warning("Unknown channel '%s'", token2);
                        MakeDefault();
                        return false;
                    }

                    channel = i;
                    jointnames = "";

                    while (!src.CheckTokenString(")")) {
                        if (!src.ReadToken(token2)) {
                            src.Warning("Unexpected end of file");
                            MakeDefault();
                            return false;
                        }
                        jointnames += token2;
                        if ((!token2.equals("*")) && (!token2.equals("-"))) {
                            jointnames += " ";
                        }
                    }

                    GetJointList(jointnames, jointList);

                    channelJoints[ channel].SetNum(jointList.Num());
                    for (num = i = 0; i < jointList.Num(); i++) {
                        jointnum = jointList.oGet(i);
                        if (joints.oGet(jointnum).channel != ANIMCHANNEL_ALL) {
                            src.Warning("Joint '%s' assigned to multiple channels", modelHandle.GetJointName(jointnum));
                            continue;
                        }
                        joints.oGet(jointnum).channel = channel;
                        channelJoints[ channel].oSet(num++, jointnum);
                    }
                    channelJoints[ channel].SetNum(num);
                } else {
                    src.Warning("unknown token '%s'", token);
                    MakeDefault();
                    return false;
                }
            }

            // shrink the anim list down to save space
            anims.SetGranularity(1);
            anims.SetNum(anims.Num());

            return true;
        }

        @Override
        public void FreeData() {
            anims.DeleteContents(true);
            joints.Clear();
            jointParents.Clear();
            modelHandle = null;
            skin = null;
            offset.Zero();
            for (int i = 0; i < ANIM_NumAnimChannels; i++) {
                channelJoints[i].Clear();
            }
        }

        public void Touch() {
            if (modelHandle != null) {
                renderModelManager.FindModel(modelHandle.Name());
            }
        }

        public idDeclSkin GetDefaultSkin() {
            return skin;
        }

        public idJointQuat[] GetDefaultPose() {
            return modelHandle.GetDefaultPose();
        }

        public void SetupJoints(int[] numJoints, idJointMat[][] jointList, idBounds frameBounds, boolean removeOriginOffset) {
            int num;
            idJointQuat[] pose;
            idJointMat[] list;

            if (null == modelHandle || modelHandle.IsDefaultModel()) {
//                Mem_Free16(jointList);
                jointList[0] = null;
                frameBounds.Clear();
                return;
            }

            // get the number of joints
            num = modelHandle.NumJoints();

            if (0 == num) {
                gameLocal.Error("model '%s' has no joints", modelHandle.Name());
            }

            // set up initial pose for model (with no pose, model is just a jumbled mess)
            list = Stream.generate(idJointMat::new).limit(num).toArray(idJointMat[]::new);
            pose = GetDefaultPose();

            // convert the joint quaternions to joint matrices
            SIMDProcessor.ConvertJointQuatsToJointMats(list, pose, joints.Num());

            // check if we offset the model by the origin joint
            if (removeOriginOffset) {
                if (VELOCITY_MOVE) {
                    list[0].SetTranslation(new idVec3(offset.x, offset.y + pose[0].t.y, offset.z + pose[0].t.z));
                } else {
                    list[0].SetTranslation(offset);
                }
            } else {
                list[ 0].SetTranslation(pose[0].t.oPlus(offset));
            }

            // transform the joint hierarchy
            SIMDProcessor.TransformJoints(list, itoi(jointParents.Ptr(Integer[].class)), 1, joints.Num() - 1);

            numJoints[0] = num;
            jointList[0] = list;

            // get the bounds of the default pose
            frameBounds.oSet(modelHandle.Bounds(null));
        }

        public idRenderModel ModelHandle() {
            return modelHandle;
        }

        public void GetJointList(final String jointnames, idList<Integer/*jointHandle_t*/> jointList) {
            String jointname;
            jointInfo_t joint;
            jointInfo_t child;
            int child_i;
            int i;
            int num;
            boolean getChildren;
            boolean subtract;

            if (null == modelHandle) {
                return;
            }

            jointList.Clear();

            num = modelHandle.NumJoints();

            // split on and skip whitespaces
            for (final String name : jointnames.split("\\s+")) {
                // copy joint name
                jointname = name;

                if (jointname.startsWith("-")) {
                    subtract = true;
                    jointname = jointname.substring(1);
                } else {
                    subtract = false;
                }

                if (jointname.startsWith("*")) {
                    getChildren = true;
                    jointname = jointname.substring(1);
                } else {
                    getChildren = false;
                }

                joint = FindJoint(jointname);
                if (null == joint) {
                    gameLocal.Warning("Unknown joint '%s' in '%s' for model '%s'", jointname, jointnames, GetName());
                    continue;
                }

                if (!subtract) {
                    jointList.AddUnique(joint.num);
                } else {
                    jointList.Remove(joint.num);
                }

                if (getChildren) {
                    // include all joint's children
                    child_i = joints.Find(joint) + 1;
                    for (i = joint.num + 1; i < num; i++, child_i++) {
                        // all children of the joint should follow it in the list.
                        // once we reach a joint without a parent or with a parent
                        // who is earlier in the list than the specified joint, then
                        // we've gone through all it's children.

                        child = joints.oGet(child_i);
                        if (child.parentNum < joint.num) {
                            break;
                        }

                        if (!subtract) {
                            jointList.AddUnique(child.num);
                        } else {
                            jointList.Remove(child.num);
                        }
                    }
                }
            }
        }

        public jointInfo_t FindJoint(final String name) {
            int i;
            idMD5Joint[] joint;

            if (null == modelHandle) {
                return null;
            }

            joint = modelHandle.GetJoints();
            for (i = 0; i < joints.Num(); i++) {
                if (NOT(joint[i].name.Icmp(name))) {
                    return joints.oGet(i);
                }
            }

            return null;
        }

        public int NumAnims() {
            return anims.Num() + 1;
        }

        public idAnim GetAnim(int index) {
            if ((index < 1) || (index > anims.Num())) {
                return null;
            }

            return anims.oGet(index - 1);
        }


        /*
         =====================
         idDeclModelDef::GetSpecificAnim

         Gets the exact anim for the name, without randomization.
         =====================
         */
        public int GetSpecificAnim(final String name) {
            int i;

            // find a specific animation
            for (i = 0; i < anims.Num(); i++) {
                if (name.startsWith(anims.oGet(i).FullName())) {
                    return i + 1;
                }
            }

            // didn't find it
            return 0;
        }
        private static final int MAX_ANIMS = 64;

        public int GetAnim(final String name) {
            int i;
            int which;
            int[] animList = new int[MAX_ANIMS];
            int numAnims;
            int len;

            len = name.length();
            if (len != 0 && idStr.CharIsNumeric(name.charAt(len - 1))) {
                // find a specific animation
                return GetSpecificAnim(name);
            }

            // find all animations with same name
            numAnims = 0;
            for (i = 0; i < anims.Num(); i++) {
                if (anims.oGet(i).Name().equals(name)) {
                    animList[ numAnims++] = i;
                    if (numAnims >= MAX_ANIMS) {
                        break;
                    }
                }
            }

            if (0 == numAnims) {
                return 0;
            }

            // get a random anim
            //FIXME: don't access gameLocal here?
            which = gameLocal.random.RandomInt(numAnims);
            return animList[ which] + 1;
        }

        public boolean HasAnim(final String name) {
            int i;

            // find any animations with same name
            for (i = 0; i < anims.Num(); i++) {
                if ((anims.oGet(i).Name().equals(name))) {
                    return true;
                }
            }

            return false;
        }

        public idDeclSkin GetSkin() {
            return skin;
        }

        public String GetModelName() {
            if (modelHandle != null) {
                return modelHandle.Name();
            } else {
                return "";
            }
        }

        public idList<jointInfo_t> Joints() {
            return joints;
        }

        public Integer[] JointParents() {
            return jointParents.Ptr(Integer[].class);
        }

        public int NumJoints() {
            return joints.Num();
        }

        public jointInfo_t GetJoint(int jointHandle) {
            if ((jointHandle < 0) || (jointHandle > joints.Num())) {
                gameLocal.Error("idDeclModelDef::GetJoint : joint handle out of range");
            }
            return joints.oGet(jointHandle);
        }

        public String GetJointName(int jointHandle) {
            idMD5Joint[] joint;

            if (null == modelHandle) {
                return null;
            }

            if ((jointHandle < 0) || (jointHandle > joints.Num())) {
                gameLocal.Error("idDeclModelDef::GetJointName : joint handle out of range");
            }

            joint = modelHandle.GetJoints();
            return joint[ jointHandle].name.toString();
        }

        public int NumJointsOnChannel(int channel) {
            if ((channel < 0) || (channel >= ANIM_NumAnimChannels)) {
                gameLocal.Error("idDeclModelDef::NumJointsOnChannel : channel out of range");
            }
            return channelJoints[ channel].Num();
        }

        public Integer[] GetChannelJoints(int channel) {
            if ((channel < 0) || (channel >= ANIM_NumAnimChannels)) {
                gameLocal.Error("idDeclModelDef::GetChannelJoints : channel out of range");
            }
            return channelJoints[channel].Ptr(Integer[].class);
        }

        public idVec3 GetVisualOffset() {
            return offset;
        }

        private void CopyDecl(final idDeclModelDef decl) {
            int i;

            FreeData();

            offset = decl.offset;
            modelHandle = decl.modelHandle;
            skin = decl.skin;

            anims.SetNum(decl.anims.Num());
            for (i = 0; i < anims.Num(); i++) {
                anims.oSet(i, new idAnim(this, decl.anims.oGet(i)));
            }

            joints.SetNum(decl.joints.Num());
//            memcpy(joints.Ptr(), decl.joints.Ptr(), decl.joints.Num() * sizeof(joints[0]));
            System.arraycopy(decl.joints.Ptr(), 0, joints.Ptr(), 0, decl.joints.Num());
            jointParents.SetNum(decl.jointParents.Num());
//            memcpy(jointParents.Ptr(), decl.jointParents.Ptr(), decl.jointParents.Num() * sizeof(jointParents[0]));
            System.arraycopy(decl.jointParents.Ptr(), 0, jointParents.Ptr(), 0, decl.jointParents.Num());
            for (i = 0; i < ANIM_NumAnimChannels; i++) {
                channelJoints[i] = decl.channelJoints[i];
            }
        }

        private boolean ParseAnim(idLexer src, int numDefaultAnims) {
            int i;
            int len;
            idAnim anim;
            final idMD5Anim[] md5anims = new idMD5Anim[ANIM_MaxSyncedAnims];
            idMD5Anim md5anim;
            idStr alias;
            idToken realname = new idToken();
            idToken token = new idToken();
            int numAnims;
            animFlags_t flags;

            numAnims = 0;
//	memset( md5anims, 0, sizeof( md5anims ) );

            if (!src.ReadToken(realname)) {
                src.Warning("Unexpected end of file");
                MakeDefault();
                return false;
            }
            alias = realname;

            for (i = 0; i < anims.Num(); i++) {
                if ((anims.oGet(i).FullName().equals(realname))) {
                    break;
                }
            }

            if ((i < anims.Num()) && (i >= numDefaultAnims)) {
                src.Warning("Duplicate anim '%s'", realname);
                MakeDefault();
                return false;
            }

            if (i < numDefaultAnims) {
                anim = anims.oGet(i);
            } else {
                // create the alias associated with this animation
                anim = new idAnim();
                anims.Append(anim);
            }

            // random anims end with a number.  find the numeric suffix of the animation.
            len = alias.Length();
            for (i = len - 1; i > 0; i--) {
                if (!isDigit(alias.oGet(i))) {
                    break;
                }
            }

            // check for zero length name, or a purely numeric name
            if (i <= 0) {
                src.Warning("Invalid animation name '%s'", alias);
                MakeDefault();
                return false;
            }

            // remove the numeric suffix
            alias.CapLength(i + 1);

            // parse the anims from the string
            do {
                if (!src.ReadToken(token)) {
                    src.Warning("Unexpected end of file");
                    MakeDefault();
                    return false;
                }

                // lookup the animation
                md5anim = animationLib.GetAnim(token.toString());
                if (null == md5anim) {
                    src.Warning("Couldn't load anim '%s'", token);
                    MakeDefault();
                    return false;
                }

                md5anim.CheckModelHierarchy(modelHandle);

                if (numAnims > 0) {
                    // make sure it's the same length as the other anims
                    if (md5anim.Length() != md5anims[ 0].Length()) {
                        src.Warning("Anim '%s' does not match length of anim '%s'", md5anim.Name(), md5anims[0].Name());
                        MakeDefault();
                        return false;
                    }
                }

                if (numAnims >= ANIM_MaxSyncedAnims) {
                    src.Warning("Exceeded max synced anims (%d)", ANIM_MaxSyncedAnims);
                    MakeDefault();
                    return false;
                }

                // add it to our list
                md5anims[ numAnims] = md5anim;
                numAnims++;
            } while (src.CheckTokenString(","));

            if (0 == numAnims) {
                src.Warning("No animation specified");
                MakeDefault();
                return false;
            }

            anim.SetAnim(this, realname.toString(), alias.toString(), numAnims, md5anims);
//	memset( &flags, 0, sizeof( flags ) );
            flags = new animFlags_t();

            // parse any frame commands or animflags
            if (src.CheckTokenString("{")) {
                while (true) {
                    if (!src.ReadToken(token)) {
                        src.Warning("Unexpected end of file");
                        MakeDefault();
                        return false;
                    }
                    if (token.equals("}")) {
                        break;
                    } else if (token.equals("prevent_idle_override")) {
                        flags.prevent_idle_override = true;
                    } else if (token.equals("random_cycle_start")) {
                        flags.random_cycle_start = true;
                    } else if (token.equals("ai_no_turn")) {
                        flags.ai_no_turn = true;
                    } else if (token.equals("anim_turn")) {
                        flags.anim_turn = true;
                    } else if (token.equals("frame")) {
                        // create a frame command
                        int framenum;
                        String err;

                        // make sure we don't have any line breaks while reading the frame command so the error line # will be correct
                        if (!src.ReadTokenOnLine(token)) {
                            src.Warning("Missing frame # after 'frame'");
                            MakeDefault();
                            return false;
                        }
                        if (token.type == TT_PUNCTUATION && token.equals("-")) {
                            src.Warning("Invalid frame # after 'frame'");
                            MakeDefault();
                            return false;
                        } else if (token.type != TT_NUMBER || token.subtype == TT_FLOAT) {
                            src.Error("expected integer value, found '%s'", token);
                        }

                        // get the frame number
                        framenum = token.GetIntValue();

                        // put the command on the specified frame of the animation
                        err = anim.AddFrameCommand(this, framenum, src, null);
                        if (err != null) {
                            src.Warning("%s", err);
                            MakeDefault();
                            return false;
                        }
                    } else {
                        src.Warning("Unknown command '%s'", token);
                        MakeDefault();
                        return false;
                    }
                }
            }

            // set the flags
            anim.SetAnimFlags(flags);
            return true;
        }

        public void oSet(idDeclModelDef idDeclModelDef) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    /*
     ==============================================================================================

     idAnimBlend

     ==============================================================================================
     */
    public static class idAnimBlend {
        // friend class				idAnimator;

        private idDeclModelDef modelDef;
        private int starttime;
        private int endtime;
        private int timeOffset;
        private float rate;
//
        private int blendStartTime;
        private int blendDuration;
        private float blendStartValue;
        private float blendEndValue;
//
        private float[] animWeights = new float[ANIM_MaxSyncedAnims];
        private short cycle;
        private short frame;
        private short animNum;
        private boolean allowMove;
        private boolean allowFrameCommands;
        //
        //
        
        public idAnimBlend() {
            Reset(null);
        }
        
        private void Reset(final idDeclModelDef _modelDef) {
            modelDef = _modelDef;
            cycle = 1;
            starttime = 0;
            endtime = 0;
            timeOffset = 0;
            rate = 1.0f;
            frame = 0;
            allowMove = true;
            allowFrameCommands = true;
            animNum = 0;

//	memset( animWeights, 0, sizeof( animWeights ) );
            animWeights = new float[animWeights.length];

            blendStartValue = 0.0f;
            blendEndValue = 0.0f;
            blendStartTime = 0;
            blendDuration = 0;
        }

        private void CallFrameCommands(idEntity ent, int fromtime, int totime) {
            idMD5Anim md5anim;
            frameBlend_t frame1 = new frameBlend_t();
            frameBlend_t frame2 = new frameBlend_t();
            int fromFrameTime;
            int toFrameTime;

            if (!allowFrameCommands || null == ent || frame != 0 || ((endtime > 0) && (fromtime > endtime))) {
                return;
            }

            idAnim anim = Anim();
            if (null == anim || !anim.HasFrameCommands()) {
                return;
            }

            if (totime <= starttime) {
                // don't play until next frame or we'll play commands twice.
                // this happens on the player sometimes.
                return;
            }

            fromFrameTime = AnimTime(fromtime);
            toFrameTime = AnimTime(totime);
            if (toFrameTime < fromFrameTime) {
                toFrameTime += Length();
            }

            md5anim = anim.MD5Anim(0);
            md5anim.ConvertTimeToFrame(fromFrameTime, cycle, frame1);
            md5anim.ConvertTimeToFrame(toFrameTime, cycle, frame2);

            if (fromFrameTime <= 0) {
                // make sure first frame is called
                CallFrameCommands(ent, -1, frame2.frame1);
            } else {
                CallFrameCommands(ent, frame1.frame1, frame2.frame1);
            }
        }

        private void SetFrame(final idDeclModelDef modelDef, int _animNum, int _frame, int currentTime, int blendTime) {
            Reset(modelDef);
            if (null == modelDef) {
                return;
            }

            final idAnim _anim = modelDef.GetAnim(_animNum);
            if (null == _anim) {
                return;
            }

            final idMD5Anim md5anim = _anim.MD5Anim(0);
            if (modelDef.Joints().Num() != md5anim.NumJoints()) {
                gameLocal.Warning("Model '%s' has different # of joints than anim '%s'", modelDef.GetModelName(), md5anim.Name());
                return;
            }

            animNum = (short) _animNum;
            starttime = currentTime;
            endtime = -1;
            cycle = -1;
            animWeights[ 0] = 1.0f;
            frame = (short) _frame;

            // a frame of 0 means it's not a single frame blend, so we set it to frame + 1
            if (frame <= 0) {
                frame = 1;
            } else if (frame > _anim.NumFrames()) {
                frame = (short) _anim.NumFrames();
            }

            // set up blend
            blendEndValue = 1.0f;
            blendStartTime = currentTime - 1;
            blendDuration = blendTime;
            blendStartValue = 0.0f;
        }

        private void CycleAnim(final idDeclModelDef modelDef, int _animNum, int currentTime, int blendTime) {
            Reset(modelDef);
            if (null == modelDef) {
                return;
            }

            final idAnim _anim = modelDef.GetAnim(_animNum);
            if (null == _anim) {
                return;
            }

            final idMD5Anim md5anim = _anim.MD5Anim(0);
            if (modelDef.Joints().Num() != md5anim.NumJoints()) {
                gameLocal.Warning("Model '%s' has different # of joints than anim '%s'", modelDef.GetModelName(), md5anim.Name());
                return;
            }

            animNum = (short) _animNum;
            animWeights[ 0] = 1.0f;
            endtime = -1;
            cycle = -1;
            if (_anim.GetAnimFlags().random_cycle_start) {
                // start the animation at a random time so that characters don't walk in sync
                starttime = (int) (currentTime - gameLocal.random.RandomFloat() * _anim.Length());
            } else {
                starttime = currentTime;
            }

            // set up blend
            blendEndValue = 1.0f;
            blendStartTime = currentTime - 1;
            blendDuration = blendTime;
            blendStartValue = 0.0f;
        }

        private void PlayAnim(final idDeclModelDef modelDef, int _animNum, int currentTime, int blendTime) {
            Reset(modelDef);
            if (null == modelDef) {
                return;
            }

            final idAnim _anim = modelDef.GetAnim(_animNum);
            if (null == _anim) {
                return;
            }

            final idMD5Anim md5anim = _anim.MD5Anim(0);
            if (modelDef.Joints().Num() != md5anim.NumJoints()) {
                gameLocal.Warning("Model '%s' has different # of joints than anim '%s'", modelDef.GetModelName(), md5anim.Name());
                return;
            }

            animNum = (short) _animNum;
            starttime = currentTime;
            endtime = starttime + _anim.Length();
            cycle = 1;
            animWeights[ 0] = 1.0f;

            // set up blend
            blendEndValue = 1.0f;
            blendStartTime = currentTime - 1;
            blendDuration = blendTime;
            blendStartValue = 0.0f;
        }

        private boolean BlendAnim(int currentTime, int channel, int numJoints, idJointQuat[] blendFrame, float[] blendWeight, boolean removeOriginOffset, boolean overrideBlend, boolean printInfo) {
            int i;
            float lerp;
            float mixWeight;
            idMD5Anim md5anim;
            idJointQuat[] ptr;
            frameBlend_t frametime = new frameBlend_t();
            idJointQuat[] jointFrame;
            idJointQuat[] mixFrame;
            int numAnims;
            int time;

            final idAnim anim = Anim();
            if (null == anim) {
                return false;
            }

            float weight = GetWeight(currentTime);
            if (blendWeight[0] > 0.0f) {
                if ((endtime >= 0) && (currentTime >= endtime)) {
                    return false;
                }
                if (0 == weight) {
                    return false;
                }
                if (overrideBlend) {
                    blendWeight[0] = 1.0f - weight;
                }
            }

            if ((channel == ANIMCHANNEL_ALL) && 0 == blendWeight[0]) {
                // we don't need a temporary buffer, so just store it directly in the blend frame
                jointFrame = blendFrame;
            } else {
                // allocate a temporary buffer to copy the joints from
                jointFrame = new idJointQuat[numJoints];
            }

            time = AnimTime(currentTime);

            numAnims = anim.NumAnims();
            if (numAnims == 1) {
                md5anim = anim.MD5Anim(0);
                if (frame != 0) {
                    md5anim.GetSingleFrame(frame - 1, jointFrame, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
                } else {
                    md5anim.ConvertTimeToFrame(time, cycle, frametime);
                    md5anim.GetInterpolatedFrame(frametime, jointFrame, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
                }
            } else {
                //
                // need to mix the multipoint anim together first
                //
                // allocate a temporary buffer to copy the joints to
                mixFrame = new idJointQuat[numJoints];

                if (0 == frame) {
                    anim.MD5Anim(0).ConvertTimeToFrame(time, cycle, frametime);
                }

                ptr = jointFrame;
                mixWeight = 0.0f;
                for (i = 0; i < numAnims; i++) {
                    if (animWeights[ i] > 0.0f) {
                        mixWeight += animWeights[ i];
                        lerp = animWeights[ i] / mixWeight;
                        md5anim = anim.MD5Anim(i);
                        if (frame != 0) {
                            md5anim.GetSingleFrame(frame - 1, ptr, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
                        } else {
                            md5anim.GetInterpolatedFrame(frametime, ptr, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
                        }

                        // only blend after the first anim is mixed in
                        if (ptr != jointFrame) {
                            SIMDProcessor.BlendJoints(jointFrame, ptr, lerp, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
                        }

                        ptr = mixFrame;
                    }
                }

                if (0 == mixWeight) {
                    return false;
                }
            }

            if (removeOriginOffset) {
                if (allowMove) {
                    if (VELOCITY_MOVE) {
                        jointFrame[ 0].t.x = 0.0f;
                    } else {
                        jointFrame[ 0].t.Zero();
                    }
                }

                if (anim.GetAnimFlags().anim_turn) {
                    jointFrame[ 0].q.Set(-0.70710677f, 0.0f, 0.0f, 0.70710677f);
                }
            }

            if (0 == blendWeight[0]) {
                blendWeight[0] = weight;
                if (channel != ANIMCHANNEL_ALL) {
                    final Integer[] index = modelDef.GetChannelJoints(channel);
                    final int num = modelDef.NumJointsOnChannel(channel);
                    for (i = 0; i < num; i++) {
                        int j = index[i];
                        blendFrame[j].t = jointFrame[j].t;
                        blendFrame[j].q = jointFrame[j].q;
                    }
                }
            } else {
                blendWeight[0] += weight;
                lerp = weight / blendWeight[0];
                SIMDProcessor.BlendJoints(blendFrame, jointFrame, lerp, itoi(modelDef.GetChannelJoints(channel)), modelDef.NumJointsOnChannel(channel));
            }

            if (printInfo) {
                if (frame != 0) {
                    gameLocal.Printf("  %s: '%s', %d, %.2f%%\n", channelNames[ channel], anim.FullName(), frame, weight * 100.0f);
                } else {
                    gameLocal.Printf("  %s: '%s', %.3f, %.2f%%\n", channelNames[ channel], anim.FullName(), (float) frametime.frame1 + frametime.backlerp, weight * 100.0f);
                }
            }

            return true;
        }

        private void BlendOrigin(int currentTime, idVec3 blendPos, float[] blendWeight, boolean removeOriginOffset) {
            float lerp;
            idVec3 animpos = new idVec3();
            idVec3 pos = new idVec3();
            int time;
            int num;
            int i;

            if (frame != 0 || ((endtime > 0) && (currentTime > endtime))) {
                return;
            }

            final idAnim anim = Anim();
            if (null == anim) {
                return;
            }

            if (allowMove && removeOriginOffset) {
                return;
            }

            float weight = GetWeight(currentTime);
            if (0 == weight) {
                return;
            }

            time = AnimTime(currentTime);

            pos.Zero();
            num = anim.NumAnims();
            for (i = 0; i < num; i++) {
                anim.GetOrigin(animpos, i, time, cycle);
                pos.oPluSet(animpos.oMultiply(animWeights[ i]));
            }

            if (0 == blendWeight[0]) {
                blendPos.oSet(pos);
                blendWeight[0] = weight;
            } else {
                lerp = weight / (blendWeight[0] + weight);
                blendPos.oPluSet((pos.oMinus(blendPos)).oMultiply(lerp));
                blendWeight[0] += weight;
            }
        }

        private void BlendDelta(int fromtime, int totime, idVec3 blendDelta, float[] blendWeight) {
            idVec3 pos1 = new idVec3();
            idVec3 pos2 = new idVec3();
            idVec3 animpos = new idVec3();
            idVec3 delta;
            int time1;
            int time2;
            float lerp;
            int num;
            int i;

            if (frame != 0 || !allowMove || ((endtime > 0) && (fromtime > endtime))) {
                return;
            }

            final idAnim anim = Anim();
            if (null == anim) {
                return;
            }

            float weight = GetWeight(totime);
            if (0 == weight) {
                return;
            }

            time1 = AnimTime(fromtime);
            time2 = AnimTime(totime);
            if (time2 < time1) {
                time2 += Length();
            }

            num = anim.NumAnims();

            pos1.Zero();
            pos2.Zero();
            for (i = 0; i < num; i++) {
                anim.GetOrigin(animpos, i, time1, cycle);
                pos1.oPluSet(animpos.oMultiply(animWeights[ i]));

                anim.GetOrigin(animpos, i, time2, cycle);
                pos2.oPluSet(animpos.oMultiply(animWeights[ i]));
            }

            delta = pos2.oMinus(pos1);
            if (0 == blendWeight[0]) {
                blendDelta.oSet(delta);
                blendWeight[0] = weight;
            } else {
                lerp = weight / (blendWeight[0] + weight);
                blendDelta.oPluSet((delta.oMinus(blendDelta)).oMultiply(lerp));
                blendWeight[0] += weight;
            }
        }

        private void BlendDeltaRotation(int fromtime, int totime, idQuat blendDelta, float[] blendWeight) {
            idQuat q1 = new idQuat();
            idQuat q2 = new idQuat();
            idQuat q3 = new idQuat();
            int time1;
            int time2;
            float lerp;
            float mixWeight;
            int num;
            int i;

            if (frame != 0 || !allowMove || ((endtime > 0) && (fromtime > endtime))) {
                return;
            }

            final idAnim anim = Anim();
            if (null == anim || !anim.GetAnimFlags().anim_turn) {
                return;
            }

            float weight = GetWeight(totime);
            if (0 == weight) {
                return;
            }

            time1 = AnimTime(fromtime);
            time2 = AnimTime(totime);
            if (time2 < time1) {
                time2 += Length();
            }

            q1.Set(0.0f, 0.0f, 0.0f, 1.0f);
            q2.Set(0.0f, 0.0f, 0.0f, 1.0f);

            mixWeight = 0.0f;
            num = anim.NumAnims();
            for (i = 0; i < num; i++) {
                if (animWeights[ i] > 0.0f) {
                    mixWeight += animWeights[ i];
                    if (animWeights[ i] == mixWeight) {
                        anim.GetOriginRotation(q1, i, time1, cycle);
                        anim.GetOriginRotation(q2, i, time2, cycle);
                    } else {
                        lerp = animWeights[ i] / mixWeight;
                        anim.GetOriginRotation(q3, i, time1, cycle);
                        q1.Slerp(q1, q3, lerp);

                        anim.GetOriginRotation(q3, i, time2, cycle);
                        q2.Slerp(q1, q3, lerp);
                    }
                }
            }

            q3 = q1.Inverse().oMultiply(q2);
            if (0 == blendWeight[0]) {
                blendDelta.oSet(q3);
                blendWeight[0] = weight;
            } else {
                lerp = weight / (blendWeight[0] + weight);
                blendDelta.Slerp(blendDelta, q3, lerp);
                blendWeight[0] += weight;
            }
        }

        private boolean AddBounds(int currentTime, idBounds bounds, boolean removeOriginOffset) {
            int i;
            int num;
            idBounds b = new idBounds();
            int time;
            idVec3 pos = new idVec3();
            boolean addorigin;

            if ((endtime > 0) && (currentTime > endtime)) {
                return false;
            }

            final idAnim anim = Anim();
            if (null == anim) {
                return false;
            }

            float weight = GetWeight(currentTime);
            if (0 == weight) {
                return false;
            }

            time = AnimTime(currentTime);
            num = anim.NumAnims();

            addorigin = !allowMove || !removeOriginOffset;
            for (i = 0; i < num; i++) {
                if (anim.GetBounds(b, i, time, cycle)) {
                    if (addorigin) {
                        anim.GetOrigin(pos, i, time, cycle);
                        b.TranslateSelf(pos);
                    }
                    bounds.AddBounds(b);
                }
            }

            return true;
        }

        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(starttime);
            savefile.WriteInt(endtime);
            savefile.WriteInt(timeOffset);
            savefile.WriteFloat(rate);

            savefile.WriteInt(blendStartTime);
            savefile.WriteInt(blendDuration);
            savefile.WriteFloat(blendStartValue);
            savefile.WriteFloat(blendEndValue);

            for (i = 0; i < ANIM_MaxSyncedAnims; i++) {
                savefile.WriteFloat(animWeights[ i]);
            }
            savefile.WriteShort(cycle);
            savefile.WriteShort(frame);
            savefile.WriteShort(animNum);
            savefile.WriteBool(allowMove);
            savefile.WriteBool(allowFrameCommands);
        }

        /*
         =====================
         idAnimBlend::Restore

         unarchives object from save game file
         =====================
         */
        public void Restore(idRestoreGame savefile, final idDeclModelDef modelDef) {
            int i;

            this.modelDef = modelDef;

            starttime = savefile.ReadInt();
            endtime = savefile.ReadInt();
            timeOffset = savefile.ReadInt();
            rate = savefile.ReadFloat();

            blendStartTime = savefile.ReadInt();
            blendDuration = savefile.ReadInt();
            blendStartValue = savefile.ReadFloat();
            blendEndValue = savefile.ReadFloat();

            for (i = 0; i < ANIM_MaxSyncedAnims; i++) {
                animWeights[i] = savefile.ReadFloat();
            }
            cycle = savefile.ReadShort();
            frame = savefile.ReadShort();
            animNum = savefile.ReadShort();
            if (null == modelDef) {
                animNum = 0;
            } else if ((animNum < 0) || (animNum > modelDef.NumAnims())) {
                gameLocal.Warning("Anim number %d out of range for model '%s' during save game", animNum, modelDef.GetModelName());
                animNum = 0;
            }
            allowMove = savefile.ReadBool();
            allowFrameCommands = savefile.ReadBool();
        }

        public String AnimName() {
            final idAnim anim = Anim();
            if (null == anim) {
                return "";
            }

            return anim.Name();
        }

        public String AnimFullName() {
            final idAnim anim = Anim();
            if (null == anim) {
                return "";
            }

            return anim.FullName();
        }

        public float GetWeight(int currentTime) {
            int timeDelta;
            float frac;
            float w;

            timeDelta = currentTime - blendStartTime;
            if (timeDelta <= 0) {
                w = blendStartValue;
            } else if (timeDelta >= blendDuration) {
                w = blendEndValue;
            } else {
                frac = (float) timeDelta / (float) blendDuration;
                w = blendStartValue + (blendEndValue - blendStartValue) * frac;
            }

            return w;
        }

        public float GetFinalWeight() {
            return blendEndValue;
        }

        public void SetWeight(float newWeight, int currentTime, int blendTime) {
            blendStartValue = GetWeight(currentTime);
            blendEndValue = newWeight;
            blendStartTime = currentTime - 1;
            blendDuration = blendTime;

            if (0 == newWeight) {
                endtime = currentTime + blendTime;
            }
        }

        public int NumSyncedAnims() {
            final idAnim anim = Anim();
            if (null == anim) {
                return 0;
            }

            return anim.NumAnims();
        }

        public boolean SetSyncedAnimWeight(int num, float weight) {
            final idAnim anim = Anim();
            if (null == anim) {
                return false;
            }

            if ((num < 0) || (num > anim.NumAnims())) {
                return false;
            }

            animWeights[ num] = weight;
            return true;
        }

        public void Clear(int currentTime, int clearTime) {
            if (0 == clearTime) {
                Reset(modelDef);
            } else {
                SetWeight(0.0f, currentTime, clearTime);
            }
        }

        public boolean IsDone(int currentTime) {
            if (0 == frame && (endtime > 0) && (currentTime >= endtime)) {
                return true;
            }

            if ((blendEndValue <= 0.0f) && (currentTime >= (blendStartTime + blendDuration))) {
                return true;
            }

            return false;
        }

        public boolean FrameHasChanged(int currentTime) {
            // if we don't have an anim, no change
            if (0 == animNum) {
                return false;
            }

            // if anim is done playing, no change
            if ((endtime > 0) && (currentTime > endtime)) {
                return false;
            }

            // if our blend weight changes, we need to update
            if ((currentTime < (blendStartTime + blendDuration) && (blendStartValue != blendEndValue))) {
                return true;
            }

            // if we're a single frame anim and this isn't the frame we started on, we don't need to update
            if ((frame != 0 || (NumFrames() == 1)) && (currentTime != starttime)) {
                return false;
            }

            return true;
        }

        public int GetCycleCount() {
            return cycle;
        }

        public void SetCycleCount(int count) {
            final idAnim anim = Anim();

            if (null == anim) {
                cycle = -1;
                endtime = 0;
            } else {
                cycle = (short) count;
                if (cycle < 0) {
                    cycle = -1;
                    endtime = -1;
                } else if (cycle == 0) {
                    cycle = 1;

                    // most of the time we're running at the original frame rate, so avoid the int-to-float-to-int conversion
                    if (rate == 1.0f) {
                        endtime = starttime - timeOffset + Length();
                    } else if (rate != 0.0f) {
                        endtime = (int) (starttime - timeOffset + Length() / rate);
                    } else {
                        endtime = -1;
                    }
                } else {
                    // most of the time we're running at the original frame rate, so avoid the int-to-float-to-int conversion
                    if (rate == 1.0f) {
                        endtime = starttime - timeOffset + Length() * cycle;
                    } else if (rate != 0.0f) {
                        endtime = (int) (starttime - timeOffset + (Length() * cycle) / rate);
                    } else {
                        endtime = -1;
                    }
                }
            }
        }

        public void SetPlaybackRate(int currentTime, float newRate) {
            int animTime;

            if (rate == newRate) {
                return;
            }

            animTime = AnimTime(currentTime);
            if (newRate == 1.0f) {
                timeOffset = animTime - (currentTime - starttime);
            } else {
                timeOffset = (int) (animTime - (currentTime - starttime) * newRate);
            }

            rate = newRate;

            // update the anim endtime
            SetCycleCount(cycle);
        }

        public float GetPlaybackRate() {
            return rate;
        }

        public void SetStartTime(int _startTime) {
            starttime = _startTime;

            // update the anim endtime
            SetCycleCount(cycle);
        }

        public int GetStartTime() {
            if (0 == animNum) {
                return 0;
            }

            return starttime;
        }

        public int GetEndTime() {
            if (0 == animNum) {
                return 0;
            }

            return endtime;
        }

        public int GetFrameNumber(int currentTime) {
            final idMD5Anim md5anim;
            frameBlend_t frameinfo = new frameBlend_t();
            int animTime;

            final idAnim anim = Anim();
            if (null == anim) {
                return 1;
            }

            if (frame != 0) {
                return frame;
            }

            md5anim = anim.MD5Anim(0);
            animTime = AnimTime(currentTime);
            md5anim.ConvertTimeToFrame(animTime, cycle, frameinfo);

            return frameinfo.frame1 + 1;
        }

        public int AnimTime(int currentTime) {
            int time;
            int length;
            final idAnim anim = Anim();

            if (anim != null) {
                if (frame != 0) {
                    return FRAME2MS(frame - 1);
                }

                // most of the time we're running at the original frame rate, so avoid the int-to-float-to-int conversion
                if (rate == 1.0f) {
                    time = currentTime - starttime + timeOffset;
                } else {
                    time = (int) (((currentTime - starttime) * rate) + timeOffset);
                }

                // given enough time, we can easily wrap time around in our frame calculations, so
                // keep cycling animations' time within the length of the 
                length = Length();
                if ((cycle < 0) && (length > 0)) {
                    time %= length;

                    // time will wrap after 24 days (oh no!), resulting in negative results for the %.
                    // adding the length gives us the proper result.
                    if (time < 0) {
                        time += length;
                    }
                }
                return time;
            } else {
                return 0;
            }
        }

        public int NumFrames() {
            final idAnim anim = Anim();
            if (null == anim) {
                return 0;
            }

            return anim.NumFrames();
        }

        public int Length() {
            final idAnim anim = Anim();
            if (null == anim) {
                return 0;
            }

            return anim.Length();
        }

        public int PlayLength() {
            if (0 == animNum) {
                return 0;
            }

            if (endtime < 0) {
                return -1;
            }

            return endtime - starttime + timeOffset;
        }

        public void AllowMovement(boolean allow) {
            allowMove = allow;
        }

        public void AllowFrameCommands(boolean allow) {
            allowFrameCommands = allow;
        }

        public idAnim Anim() {
            if (null == modelDef) {
                return null;
            }

            final idAnim anim = modelDef.GetAnim(animNum);
            return anim;
        }

        public int AnimNum() {
            return animNum;
        }
    };

    /*
     ==============================================================================================

     idAnimator

     ==============================================================================================
     */
    public static class idAnimator {

        private idDeclModelDef modelDef;
        private idEntity entity;
        //
        private idAnimBlend[][] channels = new idAnimBlend[ANIM_NumAnimChannels][ANIM_MaxAnimsPerChannel];
        private idList<jointMod_t> jointMods;
        private int numJoints;
        private idJointMat[] joints;
        //
        private int lastTransformTime;		// mutable because the value is updated in CreateFrame
        private boolean stoppedAnimatingUpdate;
        private boolean removeOriginOffset;
        private boolean forceUpdate;
        //
        private idBounds frameBounds;
        //
        private float AFPoseBlendWeight;
        private idList<Integer> AFPoseJoints;
        private idList<idAFPoseJointMod> AFPoseJointMods;
        private idList<idJointQuat> AFPoseJointFrame;
        private idBounds AFPoseBounds;
        private int AFPoseTime;
        //
        //

        public idAnimator() {
            int i, j;

            modelDef = null;
            entity = null;
            jointMods = new idList<>();
            numJoints = 0;
            joints = null;
            lastTransformTime = -1;
            stoppedAnimatingUpdate = false;
            removeOriginOffset = false;
            forceUpdate = false;

            frameBounds = new idBounds();
            frameBounds.Clear();

            AFPoseJoints = new idList<>(1);
            AFPoseJointMods = new idList<>(1);
            AFPoseJointFrame = new idList<>(1);
            AFPoseBounds = new idBounds();

            ClearAFPose();

            for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                    channels[i][j] = new idAnimBlend();
                }
            }
        }
        // ~idAnimator();

        public int/*size_t*/ Allocated() {
            int/*size_t*/ size;

            size = jointMods.Allocated() + numJoints * sizeof(joints[0].getClass()) + jointMods.Num() * sizeof(jointMods.oGet(0).getClass()) + AFPoseJointMods.Allocated() + AFPoseJointFrame.Allocated() + AFPoseJoints.Allocated();
            return size;
        }

        public int/*size_t*/ Size() {
            return sizeof(this) + Allocated();
        }


        /*
         =====================
         idAnimator::Save

         archives object for save game file
         =====================
         */
        public void Save(idSaveGame savefile) {				// archives object for save game file
            int i;
            int j;

            savefile.WriteModelDef(modelDef);
            savefile.WriteObject(entity);

            savefile.WriteInt(jointMods.Num());
            for (i = 0; i < jointMods.Num(); i++) {
                savefile.WriteInt(jointMods.oGet(i).jointnum);
                savefile.WriteMat3(jointMods.oGet(i).mat);
                savefile.WriteVec3(jointMods.oGet(i).pos);
                savefile.WriteInt(etoi(jointMods.oGet(i).transform_pos));
                savefile.WriteInt(etoi(jointMods.oGet(i).transform_axis));
            }

            savefile.WriteInt(numJoints);
            for (i = 0; i < numJoints; i++) {
                float[] data = joints[i].ToFloatPtr();
                for (j = 0; j < 12; j++) {
                    savefile.WriteFloat(data[j]);
                }
            }

            savefile.WriteInt(lastTransformTime);
            savefile.WriteBool(stoppedAnimatingUpdate);
            savefile.WriteBool(forceUpdate);
            savefile.WriteBounds(frameBounds);

            savefile.WriteFloat(AFPoseBlendWeight);

            savefile.WriteInt(AFPoseJoints.Num());
            for (i = 0; i < AFPoseJoints.Num(); i++) {
                savefile.WriteInt(AFPoseJoints.oGet(i));
            }

            savefile.WriteInt(AFPoseJointMods.Num());
            for (i = 0; i < AFPoseJointMods.Num(); i++) {
                savefile.WriteInt(etoi(AFPoseJointMods.oGet(i).mod));
                savefile.WriteMat3(AFPoseJointMods.oGet(i).axis);
                savefile.WriteVec3(AFPoseJointMods.oGet(i).origin);
            }

            savefile.WriteInt(AFPoseJointFrame.Num());
            for (i = 0; i < AFPoseJointFrame.Num(); i++) {
                savefile.WriteFloat(AFPoseJointFrame.oGet(i).q.x);
                savefile.WriteFloat(AFPoseJointFrame.oGet(i).q.y);
                savefile.WriteFloat(AFPoseJointFrame.oGet(i).q.z);
                savefile.WriteFloat(AFPoseJointFrame.oGet(i).q.w);
                savefile.WriteVec3(AFPoseJointFrame.oGet(i).t);
            }

            savefile.WriteBounds(AFPoseBounds);
            savefile.WriteInt(AFPoseTime);

            savefile.WriteBool(removeOriginOffset);

            for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                    channels[ i][ j].Save(savefile);
                }
            }
        }

        /*
         =====================
         idAnimator::Restore

         unarchives object from save game file
         =====================
         */
        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file
            int i;
            int j;
            int[] num = {0};

            savefile.ReadModelDef(modelDef);
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/entity);

            savefile.ReadInt(num);
            jointMods.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                jointMods.oSet(i, new jointMod_t());
                jointMods.oGet(i).jointnum = savefile.ReadInt();
                savefile.ReadMat3(jointMods.oGet(i).mat);
                savefile.ReadVec3(jointMods.oGet(i).pos);
                jointMods.oGet(i).transform_pos = jointModTransform_t.values()[savefile.ReadInt()];
                jointMods.oGet(i).transform_axis = jointModTransform_t.values()[savefile.ReadInt()];
            }

            numJoints = savefile.ReadInt();
            joints = new idJointMat[numJoints];
            for (i = 0; i < numJoints; i++) {
                float[] data = joints[i].ToFloatPtr();
                for (j = 0; j < 12; j++) {
                    data[j] = savefile.ReadFloat();
                }
            }

            lastTransformTime = savefile.ReadInt();
            stoppedAnimatingUpdate = savefile.ReadBool();
            forceUpdate = savefile.ReadBool();
            savefile.ReadBounds(frameBounds);

            AFPoseBlendWeight = savefile.ReadFloat();

            savefile.ReadInt(num);
            AFPoseJoints.SetGranularity(1);
            AFPoseJoints.SetNum(num[0]);
            for (i = 0; i < AFPoseJoints.Num(); i++) {
                AFPoseJoints.oSet(i, savefile.ReadInt());
            }

            savefile.ReadInt(num);
            AFPoseJointMods.SetGranularity(1);
            AFPoseJointMods.SetNum(num[0]);
            for (i = 0; i < AFPoseJointMods.Num(); i++) {
                AFPoseJointMods.oGet(i).mod = AFJointModType_t.values()[savefile.ReadInt()];
                savefile.ReadMat3(AFPoseJointMods.oGet(i).axis);
                savefile.ReadVec3(AFPoseJointMods.oGet(i).origin);
            }

            savefile.ReadInt(num);
            AFPoseJointFrame.SetGranularity(1);
            AFPoseJointFrame.SetNum(num[0]);
            for (i = 0; i < AFPoseJointFrame.Num(); i++) {
                AFPoseJointFrame.oGet(i).q.x = savefile.ReadFloat();
                AFPoseJointFrame.oGet(i).q.y = savefile.ReadFloat();
                AFPoseJointFrame.oGet(i).q.z = savefile.ReadFloat();
                AFPoseJointFrame.oGet(i).q.w = savefile.ReadFloat();
                savefile.ReadVec3(AFPoseJointFrame.oGet(i).t);
            }

            savefile.ReadBounds(AFPoseBounds);
            AFPoseTime = savefile.ReadInt();

            removeOriginOffset = savefile.ReadBool();

            for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                    channels[ i][ j].Restore(savefile, modelDef);
                }
            }
        }

        public void SetEntity(idEntity ent) {
            entity = ent;
        }

        public idEntity GetEntity() {
            return entity;
        }

        public void RemoveOriginOffset(boolean remove) {
            removeOriginOffset = remove;
        }

        public boolean RemoveOrigin() {
            return removeOriginOffset;
        }

        public void GetJointList(final String jointnames, idList<Integer/*jointHandle_t*/> jointList) {
            if (modelDef != null) {
                modelDef.GetJointList(jointnames, jointList);
            }
        }

        public void GetJointList(final idStr jointnames, idList<Integer/*jointHandle_t*/> jointList) {
            GetJointList(jointnames.toString(), jointList);
        }

        public int NumAnims() {
            if (null == modelDef) {
                return 0;
            }

            return modelDef.NumAnims();
        }

        public idAnim GetAnim(int index) {
            if (null == modelDef) {
                return null;
            }

            return modelDef.GetAnim(index);
        }

        public int GetAnim(final String name) {
            if (null == modelDef) {
                return 0;
            }

            return modelDef.GetAnim(name);
        }

        public boolean HasAnim(final String name) {
            if (null == modelDef) {
                return false;
            }

            return modelDef.HasAnim(name);
        }

        public boolean HasAnim(final idStr name) {
            return HasAnim(name.toString());
        }

        public void ServiceAnims(int fromtime, int totime) {
            int i, j;
            idAnimBlend[][] blend;

            if (null == modelDef) {
                return;
            }

            if (modelDef.ModelHandle() != null) {
                blend = channels;
                for (i = 0; i < ANIM_NumAnimChannels; i++) {
                    for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                        blend[i][j].CallFrameCommands(entity, fromtime, totime);
                    }
                }
            }

            if (!IsAnimating(totime)) {
                stoppedAnimatingUpdate = true;
                if (entity != null) {
                    entity.BecomeInactive(TH_ANIMATE);

                    // present one more time with stopped animations so the renderer can properly recreate interactions
                    entity.BecomeActive(TH_UPDATEVISUALS);
                }
            }
        }

        public boolean IsAnimating(int currentTime) {
            int i, j;
            idAnimBlend[][] blend;

            if (null == modelDef || NOT(modelDef.ModelHandle())) {
                return false;
            }

            // if animating with an articulated figure
            if (AFPoseJoints.Num() != 0 && currentTime <= AFPoseTime) {
                return true;
            }

            blend = channels;
            for (i = 0; i < ANIM_NumAnimChannels; i++) {
                for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                    if (!blend[i][j].IsDone(currentTime)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public void GetJoints(int[] numJoints, idJointMat[][] jointsPtr) {
            numJoints[0] = this.numJoints;
            jointsPtr[0] = this.joints;
        }

        public int GetJoints(idJointMat[][] jointsPtr) {
            jointsPtr[0] = this.joints;
            return this.numJoints;
        }

        public int NumJoints() {
            return numJoints;
        }

        public int/*jointHandle_t*/ GetFirstChild(int/*jointHandle_t*/ jointnum) {
                    int i;
                    int num;
                    jointInfo_t joint;

                    if (null == modelDef) {
                        return INVALID_JOINT;
                    }

                    num = modelDef.NumJoints();
                    if (0 == num) {
                        return jointnum;
                    }
                    joint = modelDef.GetJoint(0);
                    for (i = 0; i < num; joint = modelDef.GetJoint(++i)) {
                        if (joint.parentNum == jointnum) {
                            return joint.num;
                        }
                    }
                    return jointnum;
                }

                public int/*jointHandle_t*/ GetFirstChild(final String name) {
                    return GetFirstChild(GetJointHandle(name));
                }

                public idRenderModel SetModel(final String modelname) {
                    int i, j;
                    int[] numJoints = {0};

                    FreeData();

                    // check if we're just clearing the model
                    if (!isNotNullOrEmpty(modelname)) {
                        return null;
                    }

                    modelDef = (idDeclModelDef) (declManager.FindType(DECL_MODELDEF, modelname, false));
                    if (null == modelDef) {
                        return null;
                    }

                    idRenderModel renderModel = modelDef.ModelHandle();
                    if (null == renderModel) {
                        modelDef = null;
                        return null;
                    }

                    // make sure model hasn't been purged
                    modelDef.Touch();

                    {
                        idJointMat[][] joints = {null};
                        modelDef.SetupJoints(numJoints, joints, frameBounds, removeOriginOffset);
                        this.joints = joints[0];
                        this.numJoints = numJoints[0];
                    }
                    modelDef.ModelHandle().Reset();

                    // set the modelDef on all channels
                    for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                        for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                            channels[ i][ j].Reset(modelDef);
                        }
                    }

                    return modelDef.ModelHandle();
                }

                public idRenderModel ModelHandle() {
                    if (null == modelDef) {
                        return null;
                    }

                    return modelDef.ModelHandle();
                }

                public idDeclModelDef ModelDef() {
                    return modelDef;
                }

                public void ForceUpdate() {
                    lastTransformTime = -1;
                    forceUpdate = true;
                }

                public void ClearForceUpdate() {
                    forceUpdate = false;
                }
                private static final idCVar r_showSkel = new idCVar("r_showSkel", "0", CVAR_RENDERER | CVAR_INTEGER, "", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));

                public boolean CreateFrame(int currentTime, boolean force) {
                    int i, j;
                    int numJoints;
                    int parentNum;
                    boolean hasAnim;
                    boolean debugInfo;
                    float[] baseBlend = {0};
                    float[] blendWeight = {0};
                    idAnimBlend[] blend;
                    Integer[] jointParent;
                    jointMod_t jointMod;
                    idJointQuat[] defaultPose;

                    if (gameLocal.inCinematic && gameLocal.skipCinematic) {
                        return false;
                    }

                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        return false;
                    }

                    if (!force && 0 == r_showSkel.GetInteger()) {
                        if (lastTransformTime == currentTime) {
                            return false;
                        }
                        if (lastTransformTime != -1 && !stoppedAnimatingUpdate && !IsAnimating(currentTime)) {
                            return false;
                        }
                    }

                    lastTransformTime = currentTime;
                    stoppedAnimatingUpdate = false;
                    numJoints = modelDef.Joints().Num();

                    if (entity != null && ((g_debugAnim.GetInteger() == entity.entityNumber) || (g_debugAnim.GetInteger() == -2))) {
                        debugInfo = true;
                        gameLocal.Printf("---------------\n%d: entity '%s':\n", gameLocal.time, entity.GetName());
                        gameLocal.Printf("model '%s':\n", modelDef.GetModelName());
                    } else {
                        debugInfo = false;
                    }

                    // init the joint buffer
                    if (AFPoseJoints.Num() != 0) {
                        // initialize with AF pose anim for the case where there are no other animations and no AF pose joint modifications
                        defaultPose = AFPoseJointFrame.Ptr(idJointQuat[].class);
                    } else {
                        defaultPose = modelDef.GetDefaultPose();
                    }

                    if (null == defaultPose) {
                        //gameLocal.Warning( "idAnimator::CreateFrame: no defaultPose on '%s'", modelDef.Name() );
                        return false;
                    }

                    idJointQuat[] jointFrame = new idJointQuat[numJoints];
                    SIMDProcessor.Memcpy(jointFrame, defaultPose, numJoints /* sizeof( jointFrame[0] )*/);

                    hasAnim = false;

                    // blend the all channel
                    baseBlend[0] = 0.0f;
                    blend = channels[ANIMCHANNEL_ALL];
                    for (j = ANIMCHANNEL_ALL; j < ANIM_MaxAnimsPerChannel; j++) {
                        if (blend[j].BlendAnim(currentTime, ANIMCHANNEL_ALL, numJoints, jointFrame, baseBlend, removeOriginOffset, false, debugInfo)) {
                            hasAnim = true;
                            if (baseBlend[0] >= 1.0f) {
                                break;
                            }
                        }
                    }

                    // only blend other channels if there's enough space to blend into
                    if (baseBlend[0] < 1.0f) {
                        for (i = ANIMCHANNEL_ALL + 1; i < ANIM_NumAnimChannels; i++) {
                            if (0 == modelDef.NumJointsOnChannel(i)) {
                                continue;
                            }
                            if (i == ANIMCHANNEL_EYELIDS) {
                                // eyelids blend over any previous anims, so skip it and blend it later
                                continue;
                            }
                            blendWeight[0] = baseBlend[0];
                            blend = channels[i];
                            for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                                if (blend[j].BlendAnim(currentTime, i, numJoints, jointFrame, blendWeight, removeOriginOffset, false, debugInfo)) {
                                    hasAnim = true;
                                    if (blendWeight[0] >= 1.0f) {
                                        // fully blended
                                        break;
                                    }
                                }
                            }

                            if (debugInfo && 0 == AFPoseJoints.Num() && 0 == blendWeight[0]) {
                                gameLocal.Printf("%d: %s using default pose in model '%s'\n", gameLocal.time, channelNames[ i], modelDef.GetModelName());
                            }
                        }
                    }

                    // blend in the eyelids
                    if (modelDef.NumJointsOnChannel(ANIMCHANNEL_EYELIDS) != 0) {
                        blend = channels[ANIMCHANNEL_EYELIDS];
                        blendWeight[0] = baseBlend[0];
                        for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                            if (blend[j].BlendAnim(currentTime, ANIMCHANNEL_EYELIDS, numJoints, jointFrame, blendWeight, removeOriginOffset, true, debugInfo)) {
                                hasAnim = true;
                                if (blendWeight[0] >= 1.0f) {
                                    // fully blended
                                    break;
                                }
                            }
                        }
                    }

                    // blend the articulated figure pose
                    if (BlendAFPose(jointFrame)) {
                        hasAnim = true;
                    }

                    if (!hasAnim && 0 == jointMods.Num()) {
                        // no animations were updated
                        return false;
                    }

                    // convert the joint quaternions to rotation matrices
                    SIMDProcessor.ConvertJointQuatsToJointMats(joints, jointFrame, numJoints);

                    // check if we need to modify the origin
                    if (jointMods.Num() != 0 && (jointMods.oGet(0).jointnum == 0)) {
                        jointMod = jointMods.oGet(0);

                        switch (jointMod.transform_axis) {
                            case JOINTMOD_NONE:
                                break;

                            case JOINTMOD_LOCAL:
                                joints[0].SetRotation(jointMod.mat.oMultiply(joints[0].ToMat3()));
                                break;

                            case JOINTMOD_WORLD:
                                joints[0].SetRotation(joints[0].ToMat3().oMultiply(jointMod.mat));
                                break;

                            case JOINTMOD_LOCAL_OVERRIDE:
                            case JOINTMOD_WORLD_OVERRIDE:
                                joints[0].SetRotation(jointMod.mat);
                                break;
                        }

                        switch (jointMod.transform_pos) {
                            case JOINTMOD_NONE:
                                break;

                            case JOINTMOD_LOCAL:
                                joints[0].SetTranslation(joints[0].ToVec3().oPlus(jointMod.pos));
                                break;

                            case JOINTMOD_LOCAL_OVERRIDE:
                            case JOINTMOD_WORLD:
                            case JOINTMOD_WORLD_OVERRIDE:
                                joints[0].SetTranslation(jointMod.pos);
                                break;
                        }
                        j = 1;
                    } else {
                        j = 0;
                    }

                    // add in the model offset
                    joints[0].SetTranslation(joints[0].ToVec3().oPlus(modelDef.GetVisualOffset()));

                    // pointer to joint info
                    jointParent = modelDef.JointParents();

                    // add in any joint modifications
                    for (i = 1; j < jointMods.Num(); j++, i++) {
                        jointMod = jointMods.oGet(j);

                        // transform any joints preceding the joint modifier
                        SIMDProcessor.TransformJoints(joints, itoi(jointParent), i, jointMod.jointnum - 1);
                        i = jointMod.jointnum;

                        parentNum = jointParent[i];

                        // modify the axis
                        switch (jointMod.transform_axis) {
                            case JOINTMOD_NONE:
                                joints[i].SetRotation(joints[i].ToMat3().oMultiply(joints[ parentNum].ToMat3()));
                                break;

                            case JOINTMOD_LOCAL:
                                joints[i].SetRotation(jointMod.mat.oMultiply(joints[i].ToMat3().oMultiply(joints[parentNum].ToMat3())));
                                break;

                            case JOINTMOD_LOCAL_OVERRIDE:
                                joints[i].SetRotation(jointMod.mat.oMultiply(joints[parentNum].ToMat3()));
                                break;

                            case JOINTMOD_WORLD:
                                joints[i].SetRotation((joints[i].ToMat3().oMultiply(joints[parentNum].ToMat3())).oMultiply(jointMod.mat));
                                break;

                            case JOINTMOD_WORLD_OVERRIDE:
                                joints[i].SetRotation(jointMod.mat);
                                break;
                        }

                        // modify the position
                        switch (jointMod.transform_pos) {
                            case JOINTMOD_NONE:
                                joints[i].SetTranslation(joints[parentNum].ToVec3().oPlus(joints[i].ToVec3().oMultiply(joints[parentNum].ToMat3())));
                                break;

                            case JOINTMOD_LOCAL:
                                joints[i].SetTranslation(joints[parentNum].ToVec3().oPlus(joints[i].ToVec3().oPlus(jointMod.pos)).oMultiply(joints[parentNum].ToMat3()));
                                break;

                            case JOINTMOD_LOCAL_OVERRIDE:
                                joints[i].SetTranslation(joints[parentNum].ToVec3().oPlus(jointMod.pos.oMultiply(joints[parentNum].ToMat3())));
                                break;

                            case JOINTMOD_WORLD:
                                joints[i].SetTranslation(joints[parentNum].ToVec3().oPlus(joints[i].ToVec3()).oMultiply(joints[parentNum].ToMat3()).oPlus(jointMod.pos));
                                break;

                            case JOINTMOD_WORLD_OVERRIDE:
                                joints[i].SetTranslation(jointMod.pos);
                                break;
                        }
                    }

                    // transform the rest of the hierarchy
                    SIMDProcessor.TransformJoints(joints, itoi(jointParent), i, numJoints - 1);

                    return true;
                }

                public boolean FrameHasChanged(int currentTime) {
                    int i, j;
                    idAnimBlend[][] blend;

                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        return false;
                    }

                    // if animating with an articulated figure
                    if (AFPoseJoints.Num() != 0 && currentTime <= AFPoseTime) {
                        return true;
                    }

                    blend = channels;
                    for (i = 0; i < ANIM_NumAnimChannels; i++) {
                        for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                            if (blend[i][j].FrameHasChanged(currentTime)) {
                                return true;
                            }
                        }
                    }

                    if (forceUpdate && IsAnimating(currentTime)) {
                        return true;
                    }

                    return false;
                }

                public void GetDelta(int fromtime, int totime, idVec3 delta) {
                    int i;
                    idAnimBlend[] blend;
                    float[] blendWeight = {0};

                    if (null == modelDef || null == modelDef.ModelHandle() || (fromtime == totime)) {
                        delta.Zero();
                        return;
                    }

                    delta.Zero();
                    blendWeight[0] = 0.0f;

                    blend = channels[ANIMCHANNEL_ALL];
                    for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                        blend[i].BlendDelta(fromtime, totime, delta, blendWeight);
                    }

                    if (modelDef.Joints().oGet(0).channel != 0) {
                        final int c = modelDef.Joints().oGet(0).channel;
                        blend = channels[c];
                        for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                            blend[i].BlendDelta(fromtime, totime, delta, blendWeight);
                        }
                    }
                }

                public boolean GetDeltaRotation(int fromtime, int totime, idMat3 delta) {
                    int i;
                    idAnimBlend[] blend;
                    float[] blendWeight = {0};
                    idQuat q;

                    if (null == modelDef || null == modelDef.ModelHandle() || (fromtime == totime)) {
                        delta.Identity();
                        return false;
                    }

                    q = new idQuat(0.0f, 0.0f, 0.0f, 1.0f);
                    blendWeight[0] = 0.0f;

                    blend = channels[ANIMCHANNEL_ALL];
                    for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                        blend[i].BlendDeltaRotation(fromtime, totime, q, blendWeight);
                    }

                    if (modelDef.Joints().oGet(0).channel != 0) {
                        final int c = modelDef.Joints().oGet(0).channel;
                        blend = channels[c];
                        for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                            blend[i].BlendDeltaRotation(fromtime, totime, q, blendWeight);
                        }
                    }

                    if (blendWeight[0] > 0.0f) {
                        delta.oSet(q.ToMat3());
                        return true;
                    } else {
                        delta.Identity();
                        return false;
                    }
                }

                public void GetOrigin(int currentTime, idVec3 pos) {
                    int i;
                    idAnimBlend[] blend;
                    float[] blendWeight = {0};

                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        pos.Zero();
                        return;
                    }

                    pos.Zero();
                    blendWeight[0] = 0.0f;

                    blend = channels[ANIMCHANNEL_ALL];
                    for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                        blend[i].BlendOrigin(currentTime, pos, blendWeight, removeOriginOffset);
                    }

                    if (modelDef.Joints().oGet(0).channel != 0) {
                        final int k = modelDef.Joints().oGet(0).channel;
                        blend = channels[ k];
                        for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                            blend[i].BlendOrigin(currentTime, pos, blendWeight, removeOriginOffset);
                        }
                    }

                    pos.oPluSet(modelDef.GetVisualOffset());
                }

                public boolean GetBounds(int currentTime, idBounds bounds) {
                    int i, j;
                    idAnimBlend[][] blend;
                    int count;

                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        return false;
                    }

                    if (AFPoseJoints.Num() != 0) {
                        bounds = AFPoseBounds;
                        count = 1;
                    } else {
                        bounds.Clear();
                        count = 0;
                    }

                    blend = channels;
                    for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                        for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                            if (blend[i][j].AddBounds(currentTime, bounds, removeOriginOffset)) {
                                count++;
                            }
                        }
                    }

                    if (0 == count) {
                        if (!frameBounds.IsCleared()) {
                            bounds.oSet(frameBounds);
                            return true;
                        } else {
                            bounds.Zero();
                            return false;
                        }
                    }

                    bounds.TranslateSelf(modelDef.GetVisualOffset());

                    if (g_debugBounds.GetBool()) {
                        if (bounds.oGet(1, 0) - bounds.oGet(0, 0) > 2048 || bounds.oGet(1, 1) - bounds.oGet(0, 1) > 2048) {
                            if (entity != null) {
                                gameLocal.Warning("big frameBounds on entity '%s' with model '%s': %f,%f", entity.name, modelDef.ModelHandle().Name(), bounds.oGet(1, 0) - bounds.oGet(0, 0), bounds.oGet(1, 1) - bounds.oGet(0, 1));
                            } else {
                                gameLocal.Warning("big frameBounds on model '%s': %f,%f", modelDef.ModelHandle().Name(), bounds.oGet(1, 0) - bounds.oGet(0, 0), bounds.oGet(1, 1) - bounds.oGet(0, 1));
                            }
                        }
                    }

                    frameBounds = bounds;

                    return true;
                }

                public idAnimBlend CurrentAnim(int channelNum) {
                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::CurrentAnim : channel out of range");
                    }

                    return channels[ channelNum][ 0];
                }

                public void Clear(int channelNum, int currentTime, int cleartime) {
                    int i;
                    idAnimBlend[] blend;

                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::Clear : channel out of range");
                    }

                    blend = channels[channelNum];
                    for (i = 0; i < ANIM_MaxAnimsPerChannel; i++) {
                        blend[i].Clear(currentTime, cleartime);
                    }
                    ForceUpdate();
                }

                public void SetFrame(int channelNum, int animNum, int frame, int currentTime, int blendTime) {
                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::SetFrame : channel out of range");
                    }

                    if (null == modelDef || null == modelDef.GetAnim(animNum)) {
                        return;
                    }

                    PushAnims(channelNum, currentTime, blendTime);
                    channels[ channelNum][ 0].SetFrame(modelDef, animNum, frame, currentTime, blendTime);
                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                }

                public void CycleAnim(int channelNum, int animNum, int currentTime, int blendTime) {
                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::CycleAnim : channel out of range");
                    }

                    if (null == modelDef || null == modelDef.GetAnim(animNum)) {
                        return;
                    }

                    PushAnims(channelNum, currentTime, blendTime);
                    channels[ channelNum][ 0].CycleAnim(modelDef, animNum, currentTime, blendTime);
                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                }

                public void PlayAnim(int channelNum, int animNum, int currentTime, int blendTime) {
                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::PlayAnim : channel out of range");
                    }

                    if (null == modelDef || null == modelDef.GetAnim(animNum)) {
                        return;
                    }

                    PushAnims(channelNum, currentTime, blendTime);
                    channels[ channelNum][ 0].PlayAnim(modelDef, animNum, currentTime, blendTime);
                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                }

                // copies the current anim from fromChannelNum to channelNum.
                // the copied anim will have frame commands disabled to avoid executing them twice.
                public void SyncAnimChannels(int channelNum, int fromChannelNum, int currentTime, int blendTime) {
                    if ((channelNum < 0) || (channelNum >= ANIM_NumAnimChannels) || (fromChannelNum < 0) || (fromChannelNum >= ANIM_NumAnimChannels)) {
                        gameLocal.Error("idAnimator::SyncToChannel : channel out of range");
                    }

                    idAnimBlend fromBlend = channels[ fromChannelNum][ 0];
                    idAnimBlend toBlend = channels[ channelNum][ 0];

                    float weight = fromBlend.blendEndValue;
                    if ((fromBlend.Anim() != toBlend.Anim()) || (fromBlend.GetStartTime() != toBlend.GetStartTime()) || (fromBlend.GetEndTime() != toBlend.GetEndTime())) {
                        PushAnims(channelNum, currentTime, blendTime);
                        toBlend = fromBlend;
                        toBlend.blendStartValue = 0.0f;
                        toBlend.blendEndValue = 0.0f;
                    }
                    toBlend.SetWeight(weight, currentTime - 1, blendTime);

                    // disable framecommands on the current channel so that commands aren't called twice
                    toBlend.AllowFrameCommands(false);

                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                }

                public void SetJointPos(int/*jointHandle_t*/ jointnum, jointModTransform_t transform_type, final idVec3 pos) {
                    int i;
                    jointMod_t jointMod;

                    if (null == modelDef || null == modelDef.ModelHandle() || (jointnum < 0) || (jointnum >= numJoints)) {
                        return;
                    }

                    jointMod = null;
                    for (i = 0; i < jointMods.Num(); i++) {
                        if (jointMods.oGet(i).jointnum == jointnum) {
                            jointMod = jointMods.oGet(i);
                            break;
                        } else if (jointMods.oGet(i).jointnum > jointnum) {
                            break;
                        }
                    }

                    if (null == jointMod) {
                        jointMod = new jointMod_t();
                        jointMod.jointnum = jointnum;
                        jointMod.mat.Identity();
                        jointMod.transform_axis = JOINTMOD_NONE;
                        jointMods.Insert(jointMod, i);
                    }

                    jointMod.pos.oSet(pos);
                    jointMod.transform_pos = transform_type;

                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                    ForceUpdate();
                }

                public void SetJointAxis(int/*jointHandle_t*/ jointnum, jointModTransform_t transform_type, final idMat3 mat) {
                    int i;
                    jointMod_t jointMod;

                    if (null == modelDef || null == modelDef.ModelHandle() || (jointnum < 0) || (jointnum >= numJoints)) {
                        return;
                    }

                    jointMod = null;
                    for (i = 0; i < jointMods.Num(); i++) {
                        if (jointMods.oGet(i).jointnum == jointnum) {
                            jointMod = jointMods.oGet(i);
                            break;
                        } else if (jointMods.oGet(i).jointnum > jointnum) {
                            break;
                        }
                    }

                    if (null == jointMod) {
                        jointMod = new jointMod_t();
                        jointMod.jointnum = jointnum;
                        jointMod.pos.Zero();
                        jointMod.transform_pos = JOINTMOD_NONE;
                        jointMods.Insert(jointMod, i);
                    }

                    jointMod.mat.oSet(mat);
                    jointMod.transform_axis = transform_type;

                    if (entity != null) {
                        entity.BecomeActive(TH_ANIMATE);
                    }
                    ForceUpdate();
                }

                public void ClearJoint(int/*jointHandle_t*/ jointnum) {
                    int i;

                    if (null == modelDef || null == modelDef.ModelHandle() || (jointnum < 0) || (jointnum >= numJoints)) {
                        return;
                    }

                    for (i = 0; i < jointMods.Num(); i++) {
                        if (jointMods.oGet(i).jointnum == jointnum) {
//			delete jointMods[ i ];
                            jointMods.RemoveIndex(i);
                            ForceUpdate();
                            break;
                        } else if (jointMods.oGet(i).jointnum > jointnum) {
                            break;
                        }
                    }
                }

                public void ClearAllJoints() {
                    if (jointMods.Num() != 0) {
                        ForceUpdate();
                    }
                    jointMods.DeleteContents(true);
                }

                public void InitAFPose() {

                    if (null == modelDef) {
                        return;
                    }

                    AFPoseJoints.SetNum(modelDef.Joints().Num(), false);
                    AFPoseJoints.SetNum(0, false);
                    AFPoseJointMods.SetNum(modelDef.Joints().Num(), false);
                    AFPoseJointFrame.SetNum(modelDef.Joints().Num(), false);
                }

                public void SetAFPoseJointMod(final int/*jointHandle_t*/ jointNum, final AFJointModType_t mod, final idMat3 axis, final idVec3 origin) {
                    AFPoseJointMods.oSet(jointNum, new idAFPoseJointMod());
                    AFPoseJointMods.oGet(jointNum).mod = mod;
                    AFPoseJointMods.oGet(jointNum).axis = axis;
                    AFPoseJointMods.oGet(jointNum).origin = origin;

                    int index = idBinSearch_GreaterEqual(AFPoseJoints.Ptr(), AFPoseJoints.Num(), jointNum);
                    if (index >= AFPoseJoints.Num() || jointNum != AFPoseJoints.oGet(index)) {
                        AFPoseJoints.Insert(jointNum, index);
                    }
                }

                public void FinishAFPose(int animNum, final idBounds bounds, final int time) {
                    int i, j;
                    int numJoints;
                    int parentNum;
                    int jointMod;
                    int jointNum;
                    Integer[] jointParent;

                    if (null == modelDef) {
                        return;
                    }

                    final idAnim anim = modelDef.GetAnim(animNum);
                    if (null == anim) {
                        return;
                    }

                    numJoints = modelDef.Joints().Num();
                    if (0 == numJoints) {
                        return;
                    }

                    idRenderModel md5 = modelDef.ModelHandle();
                    final idMD5Anim md5anim = anim.MD5Anim(0);

                    if (numJoints != md5anim.NumJoints()) {
                        gameLocal.Warning("Model '%s' has different # of joints than anim '%s'", md5.Name(), md5anim.Name());
                        return;
                    }

                    idJointQuat[] jointFrame = new idJointQuat[numJoints];
                    md5anim.GetSingleFrame(0, jointFrame, itoi(modelDef.GetChannelJoints(ANIMCHANNEL_ALL)), modelDef.NumJointsOnChannel(ANIMCHANNEL_ALL));

                    if (removeOriginOffset) {
                        if (VELOCITY_MOVE) {
                            jointFrame[ 0].t.x = 0.0f;
                        } else {
                            jointFrame[ 0].t.Zero();
                        }
                    }

                    idJointMat[] joints = Stream.generate(idJointMat::new).limit(numJoints).toArray(idJointMat[]::new);

                    // convert the joint quaternions to joint matrices
                    SIMDProcessor.ConvertJointQuatsToJointMats(joints, jointFrame, numJoints);

                    // first joint is always root of entire hierarchy
                    if (AFPoseJoints.Num() != 0 && AFPoseJoints.oGet(0) == 0) {
                        switch (AFPoseJointMods.oGet(0).mod) {
                            case AF_JOINTMOD_AXIS: {
                                joints[0].SetRotation(AFPoseJointMods.oGet(0).axis);
                                break;
                            }
                            case AF_JOINTMOD_ORIGIN: {
                                joints[0].SetTranslation(AFPoseJointMods.oGet(0).origin);
                                break;
                            }
                            case AF_JOINTMOD_BOTH: {
                                joints[0].SetRotation(AFPoseJointMods.oGet(0).axis);
                                joints[0].SetTranslation(AFPoseJointMods.oGet(0).origin);
                                break;
                            }
                        }
                        j = 1;
                    } else {
                        j = 0;
                    }

                    // pointer to joint info
                    jointParent = modelDef.JointParents();

                    // transform the child joints
                    for (i = 1; j < AFPoseJoints.Num(); j++, i++) {
                        jointMod = AFPoseJoints.oGet(j);

                        // transform any joints preceding the joint modifier
                        SIMDProcessor.TransformJoints(joints, itoi(jointParent), i, jointMod - 1);
                        i = jointMod;

                        parentNum = jointParent[i];

                        switch (AFPoseJointMods.oGet(jointMod).mod) {
                            case AF_JOINTMOD_AXIS: {
                                joints[i].SetRotation(AFPoseJointMods.oGet(jointMod).axis);
                                joints[i].SetTranslation(joints[parentNum].ToVec3().oPlus(joints[i].ToVec3().oMultiply(joints[parentNum].ToMat3())));
                                break;
                            }
                            case AF_JOINTMOD_ORIGIN: {
                                joints[i].SetRotation(joints[i].ToMat3().oMultiply(joints[parentNum].ToMat3()));
                                joints[i].SetTranslation(AFPoseJointMods.oGet(jointMod).origin);
                                break;
                            }
                            case AF_JOINTMOD_BOTH: {
                                joints[i].SetRotation(AFPoseJointMods.oGet(jointMod).axis);
                                joints[i].SetTranslation(AFPoseJointMods.oGet(jointMod).origin);
                                break;
                            }
                        }
                    }

                    // transform the rest of the hierarchy
                    SIMDProcessor.TransformJoints(joints, itoi(jointParent), i, numJoints - 1);

                    // untransform hierarchy
                    SIMDProcessor.UntransformJoints(joints, itoi(jointParent), 1, numJoints - 1);

                    // convert joint matrices back to joint quaternions
                    SIMDProcessor.ConvertJointMatsToJointQuats(AFPoseJointFrame, joints, numJoints);

                    // find all modified joints and their parents
                    boolean[] blendJoints = new boolean[numJoints];//memset( blendJoints, 0, numJoints * sizeof( bool ) );

                    // mark all modified joints and their parents
                    for (i = 0; i < AFPoseJoints.Num(); i++) {
                        for (jointNum = AFPoseJoints.oGet(i); jointNum != INVALID_JOINT; jointNum = jointParent[jointNum]) {
                            blendJoints[jointNum] = true;
                        }
                    }

                    // lock all parents of modified joints
                    AFPoseJoints.SetNum(0, false);
                    for (i = 0; i < numJoints; i++) {
                        if (blendJoints[i]) {
                            AFPoseJoints.Append(i);
                        }
                    }

                    AFPoseBounds.oSet(bounds);
                    AFPoseTime = time;

                    ForceUpdate();
                }

                public void SetAFPoseBlendWeight(float blendWeight) {
                    AFPoseBlendWeight = blendWeight;
                }

                public boolean BlendAFPose(idJointQuat[] blendFrame) {

                    if (0 == AFPoseJoints.Num()) {
                        return false;
                    }

                    SIMDProcessor.BlendJoints(blendFrame, AFPoseJointFrame.Ptr(idJointQuat[].class), AFPoseBlendWeight, itoi(AFPoseJoints.Ptr(Integer[].class)), AFPoseJoints.Num());

                    return true;
                }

                public void ClearAFPose() {
                    if (AFPoseJoints.Num() != 0) {
                        ForceUpdate();
                    }
                    AFPoseBlendWeight = 1.0f;
                    AFPoseJoints.SetNum(0, false);
                    AFPoseBounds.Clear();
                    AFPoseTime = 0;
                }

                public void ClearAllAnims(int currentTime, int cleartime) {
                    int i;

                    for (i = 0; i < ANIM_NumAnimChannels; i++) {
                        Clear(i, currentTime, cleartime);
                    }

                    ClearAFPose();
                    ForceUpdate();
                }

                public int/*jointHandle_t*/ GetJointHandle(final String name) {
                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        return INVALID_JOINT;
                    }

                    return modelDef.ModelHandle().GetJointHandle(name);
                }

                public int/*jointHandle_t*/ GetJointHandle(final idStr name) {
                    return GetJointHandle(name.toString());
                }

                public String GetJointName(int/*jointHandle_t*/ handle) {
                    if (null == modelDef || null == modelDef.ModelHandle()) {
                        return "";
                    }

                    return modelDef.ModelHandle().GetJointName(handle);
                }

                public int GetChannelForJoint(int/*jointHandle_t*/ joint) {
                    if (null == modelDef) {
                        gameLocal.Error("idAnimator::GetChannelForJoint: NULL model");
                    }

                    if ((joint < 0) || (joint >= numJoints)) {
                        gameLocal.Error("idAnimator::GetChannelForJoint: invalid joint num (%d)", joint);
                    }

                    return modelDef.GetJoint(joint).channel;
                }

                public boolean GetJointTransform(int/*jointHandle_t*/ jointHandle, int currentTime, idVec3 offset, idMat3 axis) {
                    if (null == modelDef || (jointHandle < 0) || (jointHandle >= modelDef.NumJoints())) {
                        return false;
                    }

                    CreateFrame(currentTime, false);

                    offset.oSet(joints[ jointHandle].ToVec3());
                    axis.oSet(joints[ jointHandle].ToMat3());

                    return true;
                }

                public boolean GetJointLocalTransform(int/*jointHandle_t*/ jointHandle, int currentTime, idVec3 offset, idMat3 axis) {
                    if (null == modelDef) {
                        return false;
                    }

                    final idList<jointInfo_t> modelJoints = modelDef.Joints();

                    if ((jointHandle < 0) || (jointHandle >= modelJoints.Num())) {
                        return false;
                    }

                    // FIXME: overkill
                    CreateFrame(currentTime, false);

                    if (jointHandle > 0) {
                        idJointMat m = joints[ jointHandle];
                        m.oDivSet(joints[ modelJoints.oGet(jointHandle).parentNum]);
                        offset.oSet(m.ToVec3());
                        axis.oSet(m.ToMat3());
                    } else {
                        offset.oSet(joints[ jointHandle].ToVec3());
                        axis.oSet(joints[ jointHandle].ToMat3());
                    }

                    return true;
                }

                public animFlags_t GetAnimFlags(int animNum) {
                    animFlags_t result;

                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.GetAnimFlags();
                    }

//	memset( &result, 0, sizeof( result ) );
                    result = new animFlags_t();
                    return result;
                }

                public int NumFrames(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.NumFrames();
                    } else {
                        return 0;
                    }
                }

                public int NumSyncedAnims(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.NumAnims();
                    } else {
                        return 0;
                    }
                }

                public String AnimName(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.Name();
                    } else {
                        return "";
                    }
                }

                public String AnimFullName(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.FullName();
                    } else {
                        return "";
                    }
                }

                public int AnimLength(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.Length();
                    } else {
                        return 0;
                    }
                }

                public idVec3 TotalMovementDelta(int animNum) {
                    final idAnim anim = GetAnim(animNum);
                    if (anim != null) {
                        return anim.TotalMovementDelta();
                    } else {
                        return getVec3_origin();
                    }
                }

                private void FreeData() {
                    int i, j;

                    if (entity != null) {
                        entity.BecomeInactive(TH_ANIMATE);
                    }

                    for (i = ANIMCHANNEL_ALL; i < ANIM_NumAnimChannels; i++) {
                        for (j = 0; j < ANIM_MaxAnimsPerChannel; j++) {
                            channels[ i][ j].Reset(null);
                        }
                    }

                    jointMods.DeleteContents(true);

//	Mem_Free16( joints );
                    joints = null;
                    numJoints = 0;

                    modelDef = null;

                    ForceUpdate();
                }

                private void PushAnims(int channelNum, int currentTime, int blendTime) {
                    int i;
                    idAnimBlend[] channel;

                    channel = channels[ channelNum];
                    if (0 == channel[ 0].GetWeight(currentTime) || (channel[ 0].starttime == currentTime)) {
                        return;
                    }

                    for (i = ANIM_MaxAnimsPerChannel - 1; i > 0; i--) {
                        channel[ i] = channel[ i - 1];
                    }

                    channel[ 0].Reset(modelDef);
                    channel[ 1].Clear(currentTime, blendTime);
                    ForceUpdate();
                }
    };
    /* **********************************************************************

     Util functions

     ***********************************************************************/
    /*
     =====================
     ANIM_GetModelDefFromEntityDef
     =====================
     */

    public static idDeclModelDef ANIM_GetModelDefFromEntityDef(final idDict args) {
        idDeclModelDef modelDef;

        String name = args.GetString("model");
        modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, name, false);
        if (modelDef != null && modelDef.ModelHandle() != null) {
            return modelDef;
        }

        return null;
    }
}
