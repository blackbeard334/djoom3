package neo.Game.Animation;

import static neo.Game.Actor.EV_FootstepLeft;
import static neo.Game.Actor.EV_FootstepRight;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_LOCAL_OVERRIDE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_showTestModelFrame;
import static neo.Game.GameSys.SysCvar.g_testModelAnimate;
import static neo.Game.GameSys.SysCvar.g_testModelBlend;
import static neo.Game.GameSys.SysCvar.g_testModelRotate;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.Model.MD5_MESH_EXT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_PARTICLE_STOPTIME;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_LINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import static neo.idlib.math.Math_h.MS2SEC;

import java.util.HashMap;
import java.util.Map;

import neo.TempDump.void_callback;
import neo.Game.Actor.copyJoints_t;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Player.idPlayer;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.Animation.Anim_Import.idModelExport;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.framework.CmdSystem.argCompletion_t;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/*
 =============================================================================

 MODEL TESTING

 Model viewing can begin with either "testmodel <modelname>"

 The names must be the full pathname after the basedir, like 
 "models/weapons/v_launch/tris.md3" or "players/male/tris.md3"

 Extension will default to ".ase" if not specified.

 Testmodel will create a fake entity 100 units in front of the current view
 position, directly facing the viewer.  It will remain immobile, so you can
 move around it to view it from different angles.

 g_testModelRotate
 g_testModelAnimate
 g_testModelBlend

 =============================================================================
 */
public class Anim_Testmodel {

    /*
     ==============================================================================================

     idTestModel

     ==============================================================================================
     */
    public static class idTestModel extends idAnimatedEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTestModel );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idAnimatedEntity.getEventCallBacks());
            eventCallbacks.put(EV_FootstepLeft, (eventCallback_t0<idTestModel>) idTestModel::Event_Footstep);
            eventCallbacks.put(EV_FootstepRight, (eventCallback_t0<idTestModel>) idTestModel::Event_Footstep);
        }


        private final idEntityPtr<idEntity> head;
        private idAnimator            headAnimator;
        private idAnim                customAnim;
        private idPhysics_Parametric  physicsObj;
        private idStr                 animName;
        private int                   anim;
        private int                   headAnim;
        private int                   mode;
        private int                   frame;
        private int                   startTime;
        private int                   animTime;
        //
        private idList<copyJoints_t>  copyJoints;
        //
        //

        public idTestModel() {
            this.head = null;
            this.headAnimator = null;
            this.anim = 0;
            this.headAnim = 0;
            this.startTime = 0;
            this.animTime = 0;
            this.mode = 0;
            this.frame = 0;
        }
        // ~idTestModel();

        @Override
        public void Save(idSaveGame savefile) {
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            // FIXME: one day we may actually want to save/restore test models, but for now we'll just delete them
//	delete this;
        }

        @Override
        public void Spawn() {
            final idVec3 size = new idVec3();
            final idBounds bounds = new idBounds();
            String headModel;
            int/*jointHandle_t*/ joint;
            idStr jointName = new idStr();
            idVec3 origin = new idVec3();
			final idVec3 modelOffset = new idVec3();
            final idMat3 axis = new idMat3();
            idKeyValue kv;
            final copyJoints_t copyJoint = new copyJoints_t();

            if ((this.renderEntity.hModel != null) && this.renderEntity.hModel.IsDefaultModel() && NOT(this.animator.ModelDef())) {
                gameLocal.Warning("Unable to create testmodel for '%s' : model defaulted", this.spawnArgs.GetString("model"));
                PostEventMS(EV_Remove, 0);
                return;
            }

            this.mode = g_testModelAnimate.GetInteger();
            this.animator.RemoveOriginOffset(g_testModelAnimate.GetInteger() == 1);

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());

            if (this.spawnArgs.GetVector("mins", null, bounds.oGet(0))) {
                this.spawnArgs.GetVector("maxs", null, bounds.oGet(1));
                this.physicsObj.SetClipBox(bounds, 1.0f);
                this.physicsObj.SetContents(0);
            } else if (this.spawnArgs.GetVector("size", null, size)) {
                bounds.oGet(0).Set(size.x * -0.5f, size.y * -0.5f, 0.0f);
                bounds.oGet(1).Set(size.x * 0.5f, size.y * 0.5f, size.z);
                this.physicsObj.SetClipBox(bounds, 1.0f);
                this.physicsObj.SetContents(0);
            }

            this.spawnArgs.GetVector("offsetModel", "0 0 0", modelOffset);

            // add the head model if it has one
            headModel = this.spawnArgs.GetString("def_head", "");
            if (isNotNullOrEmpty(headModel)) {
                jointName.oSet(this.spawnArgs.GetString("head_joint"));
                joint = this.animator.GetJointHandle(jointName.toString());
                if (joint == INVALID_JOINT) {
                    gameLocal.Warning("Joint '%s' not found for 'head_joint'", jointName);
                } else {
                    // copy any sounds in case we have frame commands on the head
                    final idDict args = new idDict();
                    idKeyValue sndKV = this.spawnArgs.MatchPrefix("snd_", null);
                    while (sndKV != null) {
                        args.Set(sndKV.GetKey(), sndKV.GetValue());
                        sndKV = this.spawnArgs.MatchPrefix("snd_", sndKV);
                    }

                    this.head.oSet(gameLocal.SpawnEntityType(idAnimatedEntity.class, args));
                    this.animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                    origin = GetPhysics().GetOrigin().oPlus((origin.oPlus(modelOffset)).oMultiply(GetPhysics().GetAxis()));
                    this.head.GetEntity().SetModel(headModel);
                    this.head.GetEntity().SetOrigin(origin);
                    this.head.GetEntity().SetAxis(GetPhysics().GetAxis());
                    this.head.GetEntity().BindToJoint(this, this.animator.GetJointName(joint), true);

                    this.headAnimator = this.head.GetEntity().GetAnimator();

                    // set up the list of joints to copy to the head
                    for (kv = this.spawnArgs.MatchPrefix("copy_joint", null); kv != null; kv = this.spawnArgs.MatchPrefix("copy_joint", kv)) {
                        jointName = kv.GetKey();

                        if (jointName.StripLeadingOnce("copy_joint_world ")) {
                            copyJoint.mod = JOINTMOD_WORLD_OVERRIDE;
                        } else {
                            jointName.StripLeadingOnce("copy_joint ");
                            copyJoint.mod = JOINTMOD_LOCAL_OVERRIDE;
                        }

                        copyJoint.from[0] = this.animator.GetJointHandle(jointName.toString());
                        if (copyJoint.from[0] == INVALID_JOINT) {
                            gameLocal.Warning("Unknown copy_joint '%s'", jointName);
                            continue;
                        }

                        copyJoint.to[0] = this.headAnimator.GetJointHandle(jointName.toString());
                        if (copyJoint.to[0] == INVALID_JOINT) {
                            gameLocal.Warning("Unknown copy_joint '%s' on head", jointName);
                            continue;
                        }

                        this.copyJoints.Append(copyJoint);
                    }
                }
            }

            // start any shader effects based off of the spawn time
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);

            SetPhysics(this.physicsObj);

            gameLocal.Printf("Added testmodel at origin = '%s',  angles = '%s'\n", GetPhysics().GetOrigin().ToString(), GetPhysics().GetAxis().ToAngles().ToString());
            BecomeActive(TH_THINK);
        }

        /*
         ================
         idTestModel::ShouldConstructScriptObjectAtSpawn

         Called during idEntity::Spawn to see if it should construct the script object or not.
         Overridden by subclasses that need to spawn the script object themselves.
         ================
         */
        public boolean ShouldfinalructScriptObjectAtSpawn() {
            return false;
        }

        public void NextAnim(final idCmdArgs args) {
            if (NOT(this.animator.NumAnims())) {
                return;
            }

            this.anim++;
            if (this.anim >= this.animator.NumAnims()) {
                // anim 0 is no anim
                this.anim = 1;
            }

            this.startTime = gameLocal.time;
            this.animTime = this.animator.AnimLength(this.anim);
            this.animName.oSet(this.animator.AnimFullName(this.anim));
            this.headAnim = 0;
            if (this.headAnimator != null) {
                this.headAnimator.ClearAllAnims(gameLocal.time, 0);
                this.headAnim = this.headAnimator.GetAnim(this.animName.toString());
                if (0 == this.headAnim) {
                    this.headAnim = this.headAnimator.GetAnim("idle");
                }

                if ((this.headAnim != 0) && (this.headAnimator.AnimLength(this.headAnim) > this.animTime)) {
                    this.animTime = this.headAnimator.AnimLength(this.headAnim);
                }
            }

            gameLocal.Printf("anim '%s', %d.%03d seconds, %d frames\n", this.animName, this.animator.AnimLength(this.anim) / 1000, this.animator.AnimLength(this.anim) % 1000, this.animator.NumFrames(this.anim));
            if (this.headAnim != 0) {
                gameLocal.Printf("head '%s', %d.%03d seconds, %d frames\n", this.headAnimator.AnimFullName(this.headAnim), this.headAnimator.AnimLength(this.headAnim) / 1000, this.headAnimator.AnimLength(this.headAnim) % 1000, this.headAnimator.NumFrames(this.headAnim));
            }

            // reset the anim
            this.mode = -1;
            this.frame = 1;
        }

        public void PrevAnim(final idCmdArgs args) {
            if (NOT(this.animator.NumAnims())) {
                return;
            }

            this.headAnim = 0;
            this.anim--;
            if (this.anim < 0) {
                this.anim = this.animator.NumAnims() - 1;
            }

            this.startTime = gameLocal.time;
            this.animTime = this.animator.AnimLength(this.anim);
            this.animName.oSet(this.animator.AnimFullName(this.anim));
            this.headAnim = 0;
            if (this.headAnimator != null) {
                this.headAnimator.ClearAllAnims(gameLocal.time, 0);
                this.headAnim = this.headAnimator.GetAnim(this.animName.toString());
                if (0 == this.headAnim) {
                    this.headAnim = this.headAnimator.GetAnim("idle");
                }

                if ((this.headAnim != 0) && (this.headAnimator.AnimLength(this.headAnim) > this.animTime)) {
                    this.animTime = this.headAnimator.AnimLength(this.headAnim);
                }
            }

            gameLocal.Printf("anim '%s', %d.%03d seconds, %d frames\n", this.animName, this.animator.AnimLength(this.anim) / 1000, this.animator.AnimLength(this.anim) % 1000, this.animator.NumFrames(this.anim));
            if (this.headAnim != 0) {
                gameLocal.Printf("head '%s', %d.%03d seconds, %d frames\n", this.headAnimator.AnimFullName(this.headAnim), this.headAnimator.AnimLength(this.headAnim) / 1000, this.headAnimator.AnimLength(this.headAnim) % 1000, this.headAnimator.NumFrames(this.headAnim));
            }

            // reset the anim
            this.mode = -1;
            this.frame = 1;
        }

        public void NextFrame(final idCmdArgs args) {
            if ((0 == this.anim) || ((g_testModelAnimate.GetInteger() != 3) && (g_testModelAnimate.GetInteger() != 5))) {
                return;
            }

            this.frame++;
            if (this.frame > this.animator.NumFrames(this.anim)) {
                this.frame = 1;
            }

            gameLocal.Printf("^5 Anim: ^7%s\n^5Frame: ^7%d/%d\n\n", this.animator.AnimFullName(this.anim), this.frame, this.animator.NumFrames(this.anim));

            // reset the anim
            this.mode = -1;
        }

        public void PrevFrame(final idCmdArgs args) {
            if ((0 == this.anim) || ((g_testModelAnimate.GetInteger() != 3) && (g_testModelAnimate.GetInteger() != 5))) {
                return;
            }

            this.frame--;
            if (this.frame < 1) {
                this.frame = this.animator.NumFrames(this.anim);
            }

            gameLocal.Printf("^5 Anim: ^7%s\n^5Frame: ^7%d/%d\n\n", this.animator.AnimFullName(this.anim), this.frame, this.animator.NumFrames(this.anim));

            // reset the anim
            this.mode = -1;
        }

        public void TestAnim(final idCmdArgs args) {
            String name;
            int animNum;
            idAnim newanim;

            if (args.Argc() < 2) {
                gameLocal.Printf("usage: testanim <animname>\n");
                return;
            }

            newanim = null;

            name = args.Argv(1);
//if (false){
//	if ( strstr( name, ".ma" ) || strstr( name, ".mb" ) ) {
//		const idMD5Anim	*md5anims[ ANIM_MaxSyncedAnims ];
//		idModelExport exporter;
//		exporter.ExportAnim( name );
//		name.SetFileExtension( MD5_ANIM_EXT );
//		md5anims[ 0 ] = animationLib.GetAnim( name );
//		if ( md5anims[ 0 ] ) {
//			customAnim.SetAnim( animator.ModelDef(), name, name, 1, md5anims );
//			newanim = &customAnim;
//		}
//	} else {
//		animNum = animator.GetAnim( name );
//	}
//        }else{
            animNum = this.animator.GetAnim(name);
//    }

            if (0 == animNum) {
                gameLocal.Printf("Animation '%s' not found.\n", name);
                return;
            }

            this.anim = animNum;
            this.startTime = gameLocal.time;
            this.animTime = this.animator.AnimLength(this.anim);
            this.headAnim = 0;
            if (this.headAnimator != null) {
                this.headAnimator.ClearAllAnims(gameLocal.time, 0);
                this.headAnim = this.headAnimator.GetAnim(this.animName.toString());
                if (0 == this.headAnim) {
                    this.headAnim = this.headAnimator.GetAnim("idle");
                    if (0 == this.headAnim) {
                        gameLocal.Printf("Missing 'idle' anim for head.\n");
                    }
                }

                if ((this.headAnim != 0) && (this.headAnimator.AnimLength(this.headAnim) > this.animTime)) {
                    this.animTime = this.headAnimator.AnimLength(this.headAnim);
                }
            }

            this.animName.oSet(name);
            gameLocal.Printf("anim '%s', %d.%03d seconds, %d frames\n", this.animName.toString(), this.animator.AnimLength(this.anim) / 1000, this.animator.AnimLength(this.anim) % 1000, this.animator.NumFrames(this.anim));

            // reset the anim
            this.mode = -1;
        }

        public void BlendAnim(final idCmdArgs args) {
            int anim1;
            int anim2;

            if (args.Argc() < 4) {
                gameLocal.Printf("usage: testblend <anim1> <anim2> <frames>\n");
                return;
            }

            anim1 = gameLocal.testmodel.animator.GetAnim(args.Argv(1));
            if (0 == anim1) {
                gameLocal.Printf("Animation '%s' not found.\n", args.Argv(1));
                return;
            }

            anim2 = gameLocal.testmodel.animator.GetAnim(args.Argv(2));
            if (0 == anim2) {
                gameLocal.Printf("Animation '%s' not found.\n", args.Argv(2));
                return;
            }

            this.animName.oSet(args.Argv(2));
            this.animator.CycleAnim(ANIMCHANNEL_ALL, anim1, gameLocal.time, 0);
            this.animator.CycleAnim(ANIMCHANNEL_ALL, anim2, gameLocal.time, FRAME2MS(Integer.parseInt(args.Argv(3))));

            this.anim = anim2;
            this.headAnim = 0;
        }

        /* **********************************************************************

         Testmodel console commands

         ***********************************************************************/

        /*
         =================
         idTestModel::KeepTestModel_f

         Makes the current test model permanent, allowing you to place
         multiple test models
         =================
         */
        public static class KeepTestModel_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new KeepTestModel_f();

            private KeepTestModel_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No active testModel.\n");
                    return;
                }

                gameLocal.Printf("modelDef %p kept\n", gameLocal.testmodel.renderEntity.hModel);

                gameLocal.testmodel = null;
            }
        }

        /*
         =================
         idTestModel::TestSkin_f

         Sets a skin on an existing testModel
         =================
         */
        public static class TestSkin_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestSkin_f();

            private TestSkin_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                final idVec3 offset;
                final idStr name = new idStr();
                idPlayer player;
                final idDict dict;

                player = gameLocal.GetLocalPlayer();
                if ((null == player) || !gameLocal.CheatsOk()) {
                    return;
                }

                // delete the testModel if active
                if (NOT(gameLocal.testmodel)) {
                    common.Printf("No active testModel\n");
                    return;
                }

                if (args.Argc() < 2) {
                    common.Printf("removing testSkin.\n");
                    gameLocal.testmodel.SetSkin(null);
                    return;
                }

                name.oSet(args.Argv(1));
                gameLocal.testmodel.SetSkin(declManager.FindSkin(name));
            }
        }

        /*
         =================
         idTestModel::TestShaderParm_f

         Sets a shaderParm on an existing testModel
         =================
         */
        public static class TestShaderParm_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestShaderParm_f();

            private TestShaderParm_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                final idVec3 offset;
                final idStr name;
                idPlayer player;
                final idDict dict;

                player = gameLocal.GetLocalPlayer();
                if ((null == player) || !gameLocal.CheatsOk()) {
                    return;
                }

                // delete the testModel if active
                if (NOT(gameLocal.testmodel)) {
                    common.Printf("No active testModel\n");
                    return;
                }

                if (args.Argc() != 3) {
                    common.Printf("USAGE: testShaderParm <parmNum> <float | \"time\">\n");
                    return;
                }

                final int parm = Integer.parseInt(args.Argv(1));
                if ((parm < 0) || (parm >= MAX_ENTITY_SHADER_PARMS)) {
                    common.Printf("parmNum %d out of range\n", parm);
                    return;
                }

                float value;
                if (NOT(idStr.Icmp(args.Argv(2), "time"))) {
                    value = gameLocal.time * -0.001f;
                } else {
                    value = Float.parseFloat(args.Argv(2));
                }

                gameLocal.testmodel.SetShaderParm(parm, value);
            }
        }

        /*
         =================
         idTestModel::TestModel_f

         Creates a static modelDef in front of the current position, which
         can then be moved around
         =================
         */
        public static class TestModel_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestModel_f();

            private TestModel_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                idVec3 offset;
                final idStr name = new idStr();
                idPlayer player;
                idDict entityDef;
                idDict dict = new idDict();

                player = gameLocal.GetLocalPlayer();
                if ((null == player) || !gameLocal.CheatsOk()) {
                    return;
                }

                // delete the testModel if active
                if (gameLocal.testmodel != null) {
//		delete gameLocal.testmodel;
                    gameLocal.testmodel = null;
                }

                if (args.Argc() < 2) {
                    return;
                }

                name.oSet(args.Argv(1));

                entityDef = gameLocal.FindEntityDefDict(name.toString(), false);
                if (entityDef != null) {
                    dict = entityDef;
                } else {
                    if (declManager.FindType(DECL_MODELDEF, name, false) != null) {
                        dict.Set("model", name);
                    } else {
                        // allow map models with underscore prefixes to be tested during development
                        // without appending an ase
                        if (name.oGet(0) != '_') {
                            name.DefaultFileExtension(".ase");
                        }

                        if (name.toString().contains(".ma") || name.toString().contains(".mb")) {
                            final idModelExport exporter = new idModelExport();
                            exporter.ExportModel(name.toString());
                            name.SetFileExtension(MD5_MESH_EXT);
                        }

                        if (NOT(renderModelManager.CheckModel(name.toString()))) {
                            gameLocal.Printf("Can't register model\n");
                            return;
                        }
                        dict.Set("model", name);
                    }
                }

                offset = player.GetPhysics().GetOrigin().oPlus(player.viewAngles.ToForward().oMultiply(100.0f));

                dict.Set("origin", offset.ToString());
                dict.Set("angle", va("%f", player.viewAngles.yaw + 180.0f));
                gameLocal.testmodel = (idTestModel) gameLocal.SpawnEntityType(idTestModel.class, dict);
                gameLocal.testmodel.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            }
        }

        /*
         =====================
         idTestModel::ArgCompletion_TestModel
         =====================
         */
        public static class ArgCompletion_TestModel extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_TestModel();

            private ArgCompletion_TestModel() {
            }

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) {
                int i, num;

                num = declManager.GetNumDecls(DECL_ENTITYDEF);
                for (i = 0; i < num; i++) {
                    callback.run((new idStr(args.Argv(0)) + " " + declManager.DeclByIndex(DECL_ENTITYDEF, i, false).GetName()));
                }
                num = declManager.GetNumDecls(DECL_MODELDEF);
                for (i = 0; i < num; i++) {
                    callback.run((new idStr(args.Argv(0)) + " " + declManager.DeclByIndex(DECL_MODELDEF, i, false).GetName()));
                }
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "models/", false, ".lwo", ".ase", ".md5mesh", ".ma", ".mb", null);
            }
        }

        /*
         =====================
         idTestModel::TestParticleStopTime_f
         =====================
         */
        public static class TestParticleStopTime_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestParticleStopTime_f();

            private TestParticleStopTime_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] = MS2SEC(gameLocal.time);
                gameLocal.testmodel.UpdateVisuals();
            }
        }

        /*
         =====================
         idTestModel::TestAnim_f
         =====================
         */
        public static class TestAnim_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestAnim_f();

            private TestAnim_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.TestAnim(args);
            }
        }


        /*
         =====================
         idTestModel::ArgCompletion_TestAnim
         =====================
         */
        public static class ArgCompletion_TestAnim extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_TestAnim();

            private ArgCompletion_TestAnim() {
            }

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) {
                if (gameLocal.testmodel != null) {
                    final idAnimator animator = gameLocal.testmodel.GetAnimator();
                    for (int i = 0; i < animator.NumAnims(); i++) {
                        callback.run(va("%s %s", args.Argv(0), animator.AnimFullName(i)));
                    }
                }
            }
        }

        /*
         =====================
         idTestModel::TestBlend_f
         =====================
         */
        public static class TestBlend_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestBlend_f();

            private TestBlend_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.BlendAnim(args);
            }
        }

        /*
         =====================
         idTestModel::TestModelNextAnim_f
         =====================
         */
        public static class TestModelNextAnim_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestModelNextAnim_f();

            private TestModelNextAnim_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.NextAnim(args);
            }
        }

        /*
         =====================
         idTestModel::TestModelPrevAnim_f
         =====================
         */
        public static class TestModelPrevAnim_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestModelPrevAnim_f();

            private TestModelPrevAnim_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.PrevAnim(args);
            }
        }

        /*
         =====================
         idTestModel::TestModelNextFrame_f
         =====================
         */
        public static class TestModelNextFrame_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestModelNextFrame_f();

            private TestModelNextFrame_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.NextFrame(args);
            }
        }

        /*
         =====================
         idTestModel::TestModelPrevFrame_f
         =====================
         */
        public static class TestModelPrevFrame_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TestModelPrevFrame_f();

            private TestModelPrevFrame_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (NOT(gameLocal.testmodel)) {
                    gameLocal.Printf("No testModel active.\n");
                    return;
                }

                gameLocal.testmodel.PrevFrame(args);
            }
        }

        @Override
        public void Think() {
            final idVec3 pos = new idVec3();
            final idMat3 axis = new idMat3();
            final idAngles ang = new idAngles();
            int i;

            if ((this.thinkFlags & TH_THINK) != 0) {
                if ((this.anim != 0) && (gameLocal.testmodel.equals(this)) && (this.mode != g_testModelAnimate.GetInteger())) {
                    StopSound(etoi(SND_CHANNEL_ANY), false);
                    if (this.head.GetEntity() != null) {
                        this.head.GetEntity().StopSound(etoi(SND_CHANNEL_ANY), false);
                    }
                    switch (g_testModelAnimate.GetInteger()) {
                        default:
                        case 0:
                            // cycle anim with origin reset
                            if (this.animator.NumFrames(this.anim) <= 1) {
                                // single frame animations end immediately, so just cycle it since it's the same result
                                this.animator.CycleAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                                if (this.headAnim != 0) {
                                    this.headAnimator.CycleAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                                }
                            } else {
                                this.animator.PlayAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                                if (this.headAnim != 0) {
                                    this.headAnimator.PlayAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                                    if (this.headAnimator.AnimLength(this.headAnim) > this.animator.AnimLength(this.anim)) {
                                        // loop the body anim when the head anim is longer
                                        this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(-1);
                                    }
                                }
                            }
                            this.animator.RemoveOriginOffset(false);
                            break;

                        case 1:
                            // cycle anim with fixed origin
                            this.animator.CycleAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            this.animator.RemoveOriginOffset(true);
                            if (this.headAnim != 0) {
                                this.headAnimator.CycleAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            }
                            break;

                        case 2:
                            // cycle anim with continuous origin
                            this.animator.CycleAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            this.animator.RemoveOriginOffset(false);
                            if (this.headAnim != 0) {
                                this.headAnimator.CycleAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            }
                            break;

                        case 3:
                            // frame by frame with continuous origin
                            this.animator.SetFrame(ANIMCHANNEL_ALL, this.anim, this.frame, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            this.animator.RemoveOriginOffset(false);
                            if (this.headAnim != 0) {
                                this.headAnimator.SetFrame(ANIMCHANNEL_ALL, this.headAnim, this.frame, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            }
                            break;

                        case 4:
                            // play anim once
                            this.animator.PlayAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            this.animator.RemoveOriginOffset(false);
                            if (this.headAnim != 0) {
                                this.headAnimator.PlayAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            }
                            break;

                        case 5:
                            // frame by frame with fixed origin
                            this.animator.SetFrame(ANIMCHANNEL_ALL, this.anim, this.frame, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            this.animator.RemoveOriginOffset(true);
                            if (this.headAnim != 0) {
                                this.headAnimator.SetFrame(ANIMCHANNEL_ALL, this.headAnim, this.frame, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                            }
                            break;
                    }

                    this.mode = g_testModelAnimate.GetInteger();
                }

                if ((this.mode == 0) && (gameLocal.time >= (this.startTime + this.animTime))) {
                    this.startTime = gameLocal.time;
                    StopSound(etoi(SND_CHANNEL_ANY), false);
                    this.animator.PlayAnim(ANIMCHANNEL_ALL, this.anim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                    if (this.headAnim != 0) {
                        this.headAnimator.PlayAnim(ANIMCHANNEL_ALL, this.headAnim, gameLocal.time, FRAME2MS(g_testModelBlend.GetInteger()));
                        if (this.headAnimator.AnimLength(this.headAnim) > this.animator.AnimLength(this.anim)) {
                            // loop the body anim when the head anim is longer
                            this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(-1);
                        }
                    }
                }

                if (this.headAnimator != null) {
                    // copy the animation from the body to the head
                    for (i = 0; i < this.copyJoints.Num(); i++) {
                        if (this.copyJoints.oGet(i).mod == JOINTMOD_WORLD_OVERRIDE) {
                            final idMat3 mat = this.head.GetEntity().GetPhysics().GetAxis().Transpose();
                            GetJointWorldTransform(this.copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                            pos.oMinSet(this.head.GetEntity().GetPhysics().GetOrigin());
                            this.headAnimator.SetJointPos(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, pos.oMultiply(mat));
                            this.headAnimator.SetJointAxis(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, axis.oMultiply(mat));
                        } else {
                            this.animator.GetJointLocalTransform(this.copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                            this.headAnimator.SetJointPos(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, pos);
                            this.headAnimator.SetJointAxis(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, axis);
                        }
                    }
                }

                // update rotation
                RunPhysics();

                this.physicsObj.GetAngles(ang);
                this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, ang, new idAngles(0, (g_testModelRotate.GetFloat() * 360.0f) / 60.0f, 0), getAng_zero());

                final idClipModel clip = this.physicsObj.GetClipModel();
                if ((clip != null) && (this.animator.ModelDef() != null)) {
                    idVec3 neworigin = new idVec3();
//			idMat3 axis;
                    int/*jointHandle_t*/ joint;

                    joint = this.animator.GetJointHandle("origin");
                    this.animator.GetJointTransform(joint, gameLocal.time, neworigin, axis);
                    neworigin = ((neworigin.oMinus(this.animator.ModelDef().GetVisualOffset())).oMultiply(this.physicsObj.GetAxis())).oPlus(GetPhysics().GetOrigin());
                    clip.Link(gameLocal.clip, this, 0, neworigin, clip.GetAxis());
                }
            }

            UpdateAnimation();
            Present();

            if ((gameLocal.testmodel.equals(this))
                    && (g_showTestModelFrame.GetInteger() != 0)
                    && (this.anim != 0)) {
                gameLocal.Printf("^5 Anim: ^7%s  ^5Frame: ^7%d/%d  Time: %.3f\n", this.animator.AnimFullName(this.anim), this.animator.CurrentAnim(ANIMCHANNEL_ALL).GetFrameNumber(gameLocal.time),
                        this.animator.CurrentAnim(ANIMCHANNEL_ALL).NumFrames(), MS2SEC(gameLocal.time - this.animator.CurrentAnim(ANIMCHANNEL_ALL).GetStartTime()));
                if (this.headAnim != 0) {
                    gameLocal.Printf("^5 Head: ^7%s  ^5Frame: ^7%d/%d  Time: %.3f\n\n", this.headAnimator.AnimFullName(this.headAnim), this.headAnimator.CurrentAnim(ANIMCHANNEL_ALL).GetFrameNumber(gameLocal.time),
                            this.headAnimator.CurrentAnim(ANIMCHANNEL_ALL).NumFrames(), MS2SEC(gameLocal.time - this.headAnimator.CurrentAnim(ANIMCHANNEL_ALL).GetStartTime()));
                } else {
                    gameLocal.Printf("\n\n");
                }
            }
        }

        private void Event_Footstep() {
            StartSound("snd_footstep", SND_CHANNEL_BODY, 0, false, null);
        }

        @Override
        public void oSet(Class.idClass oGet) {
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
