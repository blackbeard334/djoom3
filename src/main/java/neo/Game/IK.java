package neo.Game;

import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_NONE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import static neo.Game.GameSys.SysCvar.ik_debug;
import static neo.Game.GameSys.SysCvar.ik_enable;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Renderer.Material.CONTENTS_IKCLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.idlib.Lib.Min;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.stream.Stream;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Mover.idPlat;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Renderer.Model.idRenderModel;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class IK /*ea*/ {

    /*
     ===============================================================================

     IK base class with a simple fast two bone solver.

     ===============================================================================
     */
    public static final String IK_ANIM = "ik_pose";

    /*
     ===============================================================================

     idIK

     ===============================================================================
     */
    public static class idIK {

        protected boolean    initialized;
        protected boolean    ik_activate;
        protected idEntity   self;        // entity using the animated model
        protected idAnimator animator;        // animator on entity
        protected int        modifiedAnim;        // animation modified by the IK
        protected idVec3     modelOffset;
        //
        //

        public idIK() {
            this.ik_activate = false;
            this.initialized = false;
            this.self = null;
            this.animator = null;
            this.modifiedAnim = 0;
            this.modelOffset = new idVec3();
        }
        // virtual					~idIK( void );

        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.initialized);
            savefile.WriteBool(this.ik_activate);
            savefile.WriteObject(this.self);
            savefile.WriteString((this.animator != null) && (this.animator.GetAnim(this.modifiedAnim) != null) ? this.animator.GetAnim(this.modifiedAnim).Name() : "");
            savefile.WriteVec3(this.modelOffset);
        }

        public void Restore(idRestoreGame savefile) {
            final idStr anim = new idStr();

            this.initialized = savefile.ReadBool();
            this.ik_activate = savefile.ReadBool();
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/self);
            savefile.ReadString(anim);
            savefile.ReadVec3(this.modelOffset);

            if (this.self != null) {
                this.animator = this.self.GetAnimator();
                if ((this.animator == null) || (this.animator.ModelDef() == null)) {
                    gameLocal.Warning("idIK::Restore: IK for entity '%s' at (%s) has no model set.",
                            this.self.name, this.self.GetPhysics().GetOrigin().ToString(0));
                }
                this.modifiedAnim = this.animator.GetAnim(anim.toString());
                if (this.modifiedAnim == 0) {
                    gameLocal.Warning("idIK::Restore: IK for entity '%s' at (%s) has no modified animation.",
                            this.self.name, this.self.GetPhysics().GetOrigin().ToString(0));
                }
            } else {
                this.animator = null;
                this.modifiedAnim = 0;
            }
        }

        public boolean IsInitialized() {
            return this.initialized && ik_enable.GetBool();
        }

        public boolean Init(idEntity self, final String anim, final idVec3 modelOffset) {
            idRenderModel model;//TODO:finalize objects that can be finalized. hint <- <- <-

            if (self == null) {
                return false;
            }

            this.self = self;

            this.animator = self.GetAnimator();
            if ((this.animator == null) || (this.animator.ModelDef() == null)) {
                gameLocal.Warning("idIK::Init: IK for entity '%s' at (%s) has no model set.",
                        self.name, self.GetPhysics().GetOrigin().ToString(0));
                return false;
            }
            if (this.animator.ModelDef().ModelHandle() == null) {
                gameLocal.Warning("idIK::Init: IK for entity '%s' at (%s) uses default model.",
                        self.name, self.GetPhysics().GetOrigin().ToString(0));
                return false;
            }
            model = this.animator.ModelHandle();
            if (model == null) {
                gameLocal.Warning("idIK::Init: IK for entity '%s' at (%s) has no model set.",
                        self.name, self.GetPhysics().GetOrigin().ToString(0));
                return false;
            }
            this.modifiedAnim = this.animator.GetAnim(anim);
            if (this.modifiedAnim == 0) {
                gameLocal.Warning("idIK::Init: IK for entity '%s' at (%s) has no modified animation.",
                        self.name, self.GetPhysics().GetOrigin().ToString(0));
                return false;
            }

            this.modelOffset = modelOffset;

            return true;
        }

        public void Evaluate() {
        }

        public void ClearJointMods() {
            this.ik_activate = false;
        }

        public boolean SolveTwoBones(final idVec3 startPos, final idVec3 endPos, final idVec3 dir, float len0, float len1, idVec3 jointPos) {
            float length, lengthSqr, lengthInv, x, y;
            idVec3 vec0, vec1;

            vec0 = endPos.oMinus(startPos);
            lengthSqr = vec0.LengthSqr();
            lengthInv = idMath.InvSqrt(lengthSqr);
            length = lengthInv * lengthSqr;

            // if the start and end position are too far out or too close to each other
            if ((length > (len0 + len1)) || (length < idMath.Fabs(len0 - len1))) {
                jointPos.oSet(startPos.oPlus(vec0.oMultiply(0.5f)));
                return false;
            }

            vec0.oMulSet(lengthInv);
            vec1 = dir.oMinus(vec0.oMultiply(dir.oMultiply(vec0)));
            vec1.Normalize();

            x = (((length * length) + (len0 * len0)) - (len1 * len1)) * (0.5f * lengthInv);
            y = idMath.Sqrt((len0 * len0) - (x * x));

            jointPos.oSet(startPos.oPlus(vec0.oMultiply(x).oPlus(vec1.oMultiply(y))));

            return true;
        }

        public float GetBoneAxis(final idVec3 startPos, final idVec3 endPos, final idVec3 dir, idMat3 axis) {
            float length;
            axis.oSet(0, endPos.oMinus(startPos));
            length = axis.oGet(0).Normalize();
            axis.oSet(1, dir.oMinus(axis.oGet(0).oMultiply(dir.oMultiply(axis.oGet(0)))));
            axis.oGet(1).Normalize();
            axis.oGet(2).Cross(axis.oGet(1), axis.oGet(0));

            return length;
        }

    }

    /*
     ===============================================================================

     IK controller for a walking character with an arbitrary number of legs.	

     ===============================================================================
     */
    /*
     ===============================================================================

     idIK_Walk

     ===============================================================================
     */
    public static class idIK_Walk extends idIK {

        private static final int MAX_LEGS = 8;
        //
        private idClipModel footModel;
        //
        private int         numLegs;
        private int         enabledLegs;
        private final int/*jointHandle_t*/[] footJoints  = new int[MAX_LEGS];
        private final int/*jointHandle_t*/[] ankleJoints = new int[MAX_LEGS];
        private final int/*jointHandle_t*/[] kneeJoints  = new int[MAX_LEGS];
        private final int/*jointHandle_t*/[] hipJoints   = new int[MAX_LEGS];
        private final int/*jointHandle_t*/[] dirJoints   = new int[MAX_LEGS];
        private int/*jointHandle_t*/ waistJoint;
        //
        private final idVec3[] hipForward          = new idVec3[MAX_LEGS];
        private final idVec3[] kneeForward         = new idVec3[MAX_LEGS];
        //
        private final float[]  upperLegLength      = new float[MAX_LEGS];
        private final float[]  lowerLegLength      = new float[MAX_LEGS];
        //
        private final idMat3[] upperLegToHipJoint  = new idMat3[MAX_LEGS];
        private final idMat3[] lowerLegToKneeJoint = new idMat3[MAX_LEGS];
        //
        private float   smoothing;
        private float   waistSmoothing;
        private float   footShift;
        private float   waistShift;
        private float   minWaistFloorDist;
        private float   minWaistAnkleDist;
        private float   footUpTrace;
        private float   footDownTrace;
        private boolean tiltWaist;
        private boolean usePivot;
        //
        // state
        private int     pivotFoot;
        private float   pivotYaw;
        private idVec3  pivotPos;
        private boolean oldHeightsValid;
        private float   oldWaistHeight;
        private final float[] oldAnkleHeights = new float[MAX_LEGS];
        private idVec3 waistOffset;
        //
        //

        public idIK_Walk() {
            int i;

            this.initialized = false;
            this.footModel = null;
            this.numLegs = 0;
            this.enabledLegs = 0;
            for (i = 0; i < MAX_LEGS; i++) {
                this.footJoints[i] = INVALID_JOINT;
                this.ankleJoints[i] = INVALID_JOINT;
                this.kneeJoints[i] = INVALID_JOINT;
                this.hipJoints[i] = INVALID_JOINT;
                this.dirJoints[i] = INVALID_JOINT;
                this.hipForward[i] = new idVec3();
                this.kneeForward[i] = new idVec3();
                this.upperLegLength[i] = 0;
                this.lowerLegLength[i] = 0;
                this.upperLegToHipJoint[i] = getMat3_identity();
                this.lowerLegToKneeJoint[i] = getMat3_identity();
                this.oldAnkleHeights[i] = 0;
            }
            this.waistJoint = INVALID_JOINT;

            this.smoothing = 0.75f;
            this.waistSmoothing = 0.5f;
            this.footShift = 0;
            this.waistShift = 0;
            this.minWaistFloorDist = 0;
            this.minWaistAnkleDist = 0;
            this.footUpTrace = 32.0f;
            this.footDownTrace = 32.0f;
            this.tiltWaist = false;
            this.usePivot = false;

            this.pivotFoot = -1;
            this.pivotYaw = 0;
            this.pivotPos = new idVec3();

            this.oldHeightsValid = false;
            this.oldWaistHeight = 0;
            this.waistOffset = new idVec3();
        }
        // virtual					~idIK_Walk( void );

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            super.Save(savefile);

            savefile.WriteClipModel(this.footModel);

            savefile.WriteInt(this.numLegs);
            savefile.WriteInt(this.enabledLegs);
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteInt(this.footJoints[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteInt(this.ankleJoints[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteInt(this.kneeJoints[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteInt(this.hipJoints[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteInt(this.dirJoints[i]);
            }
            savefile.WriteInt(this.waistJoint);

            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteVec3(this.hipForward[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteVec3(this.kneeForward[i]);
            }

            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteFloat(this.upperLegLength[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteFloat(this.lowerLegLength[i]);
            }

            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteMat3(this.upperLegToHipJoint[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteMat3(this.lowerLegToKneeJoint[i]);
            }

            savefile.WriteFloat(this.smoothing);
            savefile.WriteFloat(this.waistSmoothing);
            savefile.WriteFloat(this.footShift);
            savefile.WriteFloat(this.waistShift);
            savefile.WriteFloat(this.minWaistFloorDist);
            savefile.WriteFloat(this.minWaistAnkleDist);
            savefile.WriteFloat(this.footUpTrace);
            savefile.WriteFloat(this.footDownTrace);
            savefile.WriteBool(this.tiltWaist);
            savefile.WriteBool(this.usePivot);

            savefile.WriteInt(this.pivotFoot);
            savefile.WriteFloat(this.pivotYaw);
            savefile.WriteVec3(this.pivotPos);
            savefile.WriteBool(this.oldHeightsValid);
            savefile.WriteFloat(this.oldWaistHeight);
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.WriteFloat(this.oldAnkleHeights[i]);
            }
            savefile.WriteVec3(this.waistOffset);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;

            super.Restore(savefile);

            savefile.ReadClipModel(this.footModel);

            this.numLegs = savefile.ReadInt();
            this.enabledLegs = savefile.ReadInt();
            for (i = 0; i < MAX_LEGS; i++) {
                this.footJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_LEGS; i++) {
                this.ankleJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_LEGS; i++) {
                this.kneeJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_LEGS; i++) {
                this.hipJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_LEGS; i++) {
                this.dirJoints[i] = savefile.ReadInt();
            }
            this.waistJoint = savefile.ReadInt();

            for (i = 0; i < MAX_LEGS; i++) {
                savefile.ReadVec3(this.hipForward[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.ReadVec3(this.kneeForward[i]);
            }

            for (i = 0; i < MAX_LEGS; i++) {
                this.upperLegLength[i] = savefile.ReadFloat();
            }
            for (i = 0; i < MAX_LEGS; i++) {
                this.lowerLegLength[i] = savefile.ReadFloat();
            }

            for (i = 0; i < MAX_LEGS; i++) {
                savefile.ReadMat3(this.upperLegToHipJoint[i]);
            }
            for (i = 0; i < MAX_LEGS; i++) {
                savefile.ReadMat3(this.lowerLegToKneeJoint[i]);
            }

            this.smoothing = savefile.ReadFloat();
            this.waistSmoothing = savefile.ReadFloat();
            this.footShift = savefile.ReadFloat();
            this.waistShift = savefile.ReadFloat();
            this.minWaistFloorDist = savefile.ReadFloat();
            this.minWaistAnkleDist = savefile.ReadFloat();
            this.footUpTrace = savefile.ReadFloat();
            this.footDownTrace = savefile.ReadFloat();
            this.tiltWaist = savefile.ReadBool();
            this.usePivot = savefile.ReadBool();

            this.pivotFoot = savefile.ReadInt();
            this.pivotYaw = savefile.ReadFloat();
            savefile.ReadVec3(this.pivotPos);
            this.oldHeightsValid = savefile.ReadBool();
            this.oldWaistHeight = savefile.ReadFloat();
            for (i = 0; i < MAX_LEGS; i++) {
                this.oldAnkleHeights[i] = savefile.ReadFloat();
            }
            savefile.ReadVec3(this.waistOffset);
        }
        private static final idVec3[] footWinding/*[4]*/ = {
                    new idVec3(1.0f, 1.0f, 0),
                    new idVec3(-1.0f, 1.0f, 0),
                    new idVec3(-1.0f, -1.0f, 0),
                    new idVec3(1.0f, -1.0f, 0)
                };

        @Override
        public boolean Init(idEntity self, final String anim, final idVec3 modelOffset) {
            int i;
            float footSize;
            final idVec3[] verts = new idVec3[4];
            final idTraceModel trm = new idTraceModel();
            String jointName;
            idVec3 dir = new idVec3(), ankleOrigin, kneeOrigin, hipOrigin, dirOrigin;
            final idMat3 axis = new idMat3();
			idMat3 ankleAxis, kneeAxis, hipAxis;

            if (null == self) {
                return false;
            }

            this.numLegs = Min(self.spawnArgs.GetInt("ik_numLegs", "0"), MAX_LEGS);
            if (this.numLegs == 0) {
                return true;
            }

            if (!super.Init(self, anim, modelOffset)) {
                return false;
            }

            final int numJoints = this.animator.NumJoints();
            final idJointMat[] joints = Stream.generate(idJointMat::new).limit(numJoints).toArray(idJointMat[]::new);

            // create the animation frame used to setup the IK
            GameEdit.gameEdit.ANIM_CreateAnimFrame(this.animator.ModelHandle(), this.animator.GetAnim(this.modifiedAnim).MD5Anim(0), numJoints, joints, 1, this.animator.ModelDef().GetVisualOffset().oPlus(modelOffset), this.animator.RemoveOrigin());

            this.enabledLegs = 0;

            // get all the joints
            for (i = 0; i < this.numLegs; i++) {

                jointName = self.spawnArgs.GetString(va("ik_foot%d", i + 1));
                this.footJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.footJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Walk::Init: invalid foot joint '%s'", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_ankle%d", i + 1));
                this.ankleJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.ankleJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Walk::Init: invalid ankle joint '%s'", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_knee%d", i + 1));
                this.kneeJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.kneeJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Walk::Init: invalid knee joint '%s'\n", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_hip%d", i + 1));
                this.hipJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.hipJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Walk::Init: invalid hip joint '%s'\n", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_dir%d", i + 1));
                this.dirJoints[i] = this.animator.GetJointHandle(jointName);

                this.enabledLegs |= 1 << i;
            }

            jointName = self.spawnArgs.GetString("ik_waist");
            this.waistJoint = this.animator.GetJointHandle(jointName);
            if (this.waistJoint == INVALID_JOINT) {
                gameLocal.Error("idIK_Walk::Init: invalid waist joint '%s'\n", jointName);
            }

            // get the leg bone lengths and rotation matrices
            for (i = 0; i < this.numLegs; i++) {
                this.oldAnkleHeights[i] = 0;

                ankleAxis = joints[ this.ankleJoints[ i]].ToMat3();
                ankleOrigin = joints[ this.ankleJoints[ i]].ToVec3();

                kneeAxis = joints[ this.kneeJoints[ i]].ToMat3();
                kneeOrigin = joints[ this.kneeJoints[ i]].ToVec3();

                hipAxis = joints[ this.hipJoints[ i]].ToMat3();
                hipOrigin = joints[ this.hipJoints[ i]].ToVec3();

                // get the IK direction
                if (this.dirJoints[i] != INVALID_JOINT) {
                    dirOrigin = joints[ this.dirJoints[ i]].ToVec3();
                    dir = dirOrigin.oMinus(kneeOrigin);
                } else {
                    dir.Set(1.0f, 0, 0);
                }

                this.hipForward[i] = dir.oMultiply(hipAxis.Transpose());
                this.kneeForward[i] = dir.oMultiply(kneeAxis.Transpose());

                // conversion from upper leg bone axis to hip joint axis
                this.upperLegLength[i] = GetBoneAxis(hipOrigin, kneeOrigin, dir, axis);
                this.upperLegToHipJoint[i] = hipAxis.oMultiply(axis.Transpose());

                // conversion from lower leg bone axis to knee joint axis
                this.lowerLegLength[i] = GetBoneAxis(kneeOrigin, ankleOrigin, dir, axis);
                this.lowerLegToKneeJoint[i] = kneeAxis.oMultiply(axis.Transpose());
            }

            this.smoothing = self.spawnArgs.GetFloat("ik_smoothing", "0.75");
            this.waistSmoothing = self.spawnArgs.GetFloat("ik_waistSmoothing", "0.75");
            this.footShift = self.spawnArgs.GetFloat("ik_footShift", "0");
            this.waistShift = self.spawnArgs.GetFloat("ik_waistShift", "0");
            this.minWaistFloorDist = self.spawnArgs.GetFloat("ik_minWaistFloorDist", "0");
            this.minWaistAnkleDist = self.spawnArgs.GetFloat("ik_minWaistAnkleDist", "0");
            this.footUpTrace = self.spawnArgs.GetFloat("ik_footUpTrace", "32");
            this.footDownTrace = self.spawnArgs.GetFloat("ik_footDownTrace", "32");
            this.tiltWaist = self.spawnArgs.GetBool("ik_tiltWaist", "0");
            this.usePivot = self.spawnArgs.GetBool("ik_usePivot", "0");

            // setup a clip model for the feet
            footSize = self.spawnArgs.GetFloat("ik_footSize", "4") * 0.5f;
            if (footSize > 0) {
                for (i = 0; i < 4; i++) {
                    verts[i] = footWinding[i].oMultiply(footSize);
                }
                trm.SetupPolygon(verts, 4);
                this.footModel = new idClipModel(trm);
            }

            this.initialized = true;

            return true;
        }

        @Override
        public void Evaluate() {
            int i, newPivotFoot = 0;
            float modelHeight, jointHeight, lowestHeight;
            final float[] floorHeights = new float[MAX_LEGS];
            float shift, smallestShift, newHeight, step, newPivotYaw, height, largestAnkleHeight;
            idVec3 modelOrigin, normal, hipDir, kneeDir, start, end;
            final idVec3[] jointOrigins = new idVec3[MAX_LEGS];
            final idVec3 footOrigin = new idVec3(), ankleOrigin = new idVec3(),
                    kneeOrigin = new idVec3();
			idVec3 hipOrigin = new idVec3(), waistOrigin = new idVec3();
            idMat3 modelAxis;
			final idMat3 waistAxis = new idMat3(), axis = new idMat3();
            final idMat3[] hipAxis = new idMat3[MAX_LEGS], kneeAxis = new idMat3[MAX_LEGS], ankleAxis = new idMat3[MAX_LEGS];
            final trace_s[] results = {null};

            if ((null == this.self) || !gameLocal.isNewFrame) {
                return;
            }

            // if no IK enabled on any legs
            if (0 == this.enabledLegs) {//TODO:make booleans out of ints that are boolean anyways. damn you C programmers!!
                return;
            }

            normal = this.self.GetPhysics().GetGravityNormal().oNegative();
            modelOrigin = this.self.GetPhysics().GetOrigin();
            modelAxis = this.self.GetRenderEntity().axis;
            modelHeight = modelOrigin.oMultiply(normal);

            modelOrigin.oPluSet(this.modelOffset.oMultiply(modelAxis));

            // create frame without joint mods
            this.animator.CreateFrame(gameLocal.time, false);

            // get the joint positions for the feet
            lowestHeight = idMath.INFINITY;
            for (i = 0; i < this.numLegs; i++) {
                this.animator.GetJointTransform(this.footJoints[i], gameLocal.time, footOrigin, axis);
                jointOrigins[i] = modelOrigin.oPlus(footOrigin.oMultiply(modelAxis));
                jointHeight = jointOrigins[i].oMultiply(normal);
                if (jointHeight < lowestHeight) {
                    lowestHeight = jointHeight;
                    newPivotFoot = i;
                }
            }

            if (this.usePivot) {

                newPivotYaw = modelAxis.oGet(0).ToYaw();

                // change pivot foot
                if ((newPivotFoot != this.pivotFoot) || (idMath.Fabs(idMath.AngleNormalize180(newPivotYaw - this.pivotYaw)) > 30.0f)) {
                    this.pivotFoot = newPivotFoot;
                    this.pivotYaw = newPivotYaw;
                    this.animator.GetJointTransform(this.footJoints[this.pivotFoot], gameLocal.time, footOrigin, axis);
                    this.pivotPos = modelOrigin.oPlus(footOrigin.oMultiply(modelAxis));
                }

                // keep pivot foot in place
                jointOrigins[this.pivotFoot] = this.pivotPos;
            }

            // get the floor heights for the feet
            for (i = 0; i < this.numLegs; i++) {

                if (0 == (this.enabledLegs & (1 << i))) {
                    continue;
                }

                start = jointOrigins[i].oPlus(normal.oMultiply(this.footUpTrace));
                end = jointOrigins[i].oMinus(normal.oMultiply(this.footDownTrace));
                gameLocal.clip.Translation(results, start, end, this.footModel, getMat3_identity(), CONTENTS_SOLID | CONTENTS_IKCLIP, this.self);
                floorHeights[i] = results[0].endpos.oMultiply(normal);

                if (ik_debug.GetBool() && (this.footModel != null)) {
                    final idFixedWinding w = new idFixedWinding();
                    for (int j = 0; j < this.footModel.GetTraceModel().numVerts; j++) {
                        w.oPluSet(this.footModel.GetTraceModel().verts[j]);
                    }
                    gameRenderWorld.DebugWinding(colorRed, w, results[0].endpos, results[0].endAxis);
                }
            }

            final idPhysics phys = this.self.GetPhysics();

            // test whether or not the character standing on the ground
            final boolean onGround = phys.HasGroundContacts();

            // test whether or not the character is standing on a plat
            boolean onPlat = false;
            for (i = 0; i < phys.GetNumContacts(); i++) {
                final idEntity ent = gameLocal.entities[ phys.GetContact(i).entityNum];
                if ((ent != null) && ent.IsType(idPlat.class)) {
                    onPlat = true;
                    break;
                }
            }

            // adjust heights of the ankles
            smallestShift = idMath.INFINITY;
            largestAnkleHeight = -idMath.INFINITY;
            for (i = 0; i < this.numLegs; i++) {

                if (onGround && ((this.enabledLegs & (1 << i)) != 0)) {
                    shift = (floorHeights[i] - modelHeight) + this.footShift;
                } else {
                    shift = 0;
                }

                if (shift < smallestShift) {
                    smallestShift = shift;
                }

                ankleAxis[i] = new idMat3();
                this.animator.GetJointTransform(this.ankleJoints[i], gameLocal.time, ankleOrigin, ankleAxis[i]);
                jointOrigins[i] = modelOrigin.oPlus(ankleOrigin.oMultiply(modelAxis));

                height = jointOrigins[i].oMultiply(normal);

                if (this.oldHeightsValid && !onPlat) {
                    step = (height + shift) - this.oldAnkleHeights[i];
                    shift -= this.smoothing * step;
                }

                newHeight = height + shift;
                if (newHeight > largestAnkleHeight) {
                    largestAnkleHeight = newHeight;
                }

                this.oldAnkleHeights[i] = newHeight;

                jointOrigins[i].oPluSet(normal.oMultiply(shift));
            }

            this.animator.GetJointTransform(this.waistJoint, gameLocal.time, waistOrigin, waistAxis);
            waistOrigin = modelOrigin.oPlus(waistOrigin.oMultiply(modelAxis));

            // adjust position of the waist
            this.waistOffset = normal.oMultiply(smallestShift + this.waistShift);

            // if the waist should be at least a certain distance above the floor
            if ((this.minWaistFloorDist > 0) && (this.waistOffset.oMultiply(normal) < 0)) {
                start = waistOrigin;
                end = waistOrigin.oPlus(this.waistOffset.oMinus(normal.oMultiply(this.minWaistFloorDist)));
                gameLocal.clip.Translation(results, start, end, this.footModel, modelAxis, CONTENTS_SOLID | CONTENTS_IKCLIP, this.self);
                height = (waistOrigin.oPlus(this.waistOffset.oMinus(results[0].endpos))).oMultiply(normal);
                if (height < this.minWaistFloorDist) {
                    this.waistOffset.oPluSet(normal.oMultiply(this.minWaistFloorDist - height));
                }
            }

            // if the waist should be at least a certain distance above the ankles
            if (this.minWaistAnkleDist > 0) {
                height = (waistOrigin.oPlus(this.waistOffset)).oMultiply(normal);
                if ((height - largestAnkleHeight) < this.minWaistAnkleDist) {
                    this.waistOffset.oPluSet(normal.oMultiply(this.minWaistAnkleDist - (height - largestAnkleHeight)));
                }
            }

            if (this.oldHeightsValid) {
                // smoothly adjust height of waist
                newHeight = (waistOrigin.oPlus(this.waistOffset)).oMultiply(normal);
                step = newHeight - this.oldWaistHeight;
                this.waistOffset.oMinSet(normal.oMultiply(this.waistSmoothing * step));
            }

            // save height of waist for smoothing
            this.oldWaistHeight = (waistOrigin.oPlus(this.waistOffset)).oMultiply(normal);

            if (!this.oldHeightsValid) {
                this.oldHeightsValid = true;
                return;
            }

            // solve IK
            for (i = 0; i < this.numLegs; i++) {

                // get the position of the hip in world space
                this.animator.GetJointTransform(this.hipJoints[i], gameLocal.time, hipOrigin, axis);
                hipOrigin = modelOrigin.oPlus(this.waistOffset.oPlus(hipOrigin.oMultiply(modelAxis)));
                hipDir = this.hipForward[i].oMultiply(axis.oMultiply(modelAxis));

                // get the IK bend direction
                this.animator.GetJointTransform(this.kneeJoints[i], gameLocal.time, kneeOrigin, axis);
                kneeDir = this.kneeForward[i].oMultiply(axis.oMultiply(modelAxis));

                // solve IK and calculate knee position
                SolveTwoBones(hipOrigin, jointOrigins[i], kneeDir, this.upperLegLength[i], this.lowerLegLength[i], kneeOrigin);

                if (ik_debug.GetBool()) {
                    gameRenderWorld.DebugLine(colorCyan, hipOrigin, kneeOrigin);
                    gameRenderWorld.DebugLine(colorRed, kneeOrigin, jointOrigins[i]);
                    gameRenderWorld.DebugLine(colorYellow, kneeOrigin, kneeOrigin.oPlus(hipDir));
                    gameRenderWorld.DebugLine(colorGreen, kneeOrigin, kneeOrigin.oPlus(kneeDir));
                }

                // get the axis for the hip joint
                GetBoneAxis(hipOrigin, kneeOrigin, hipDir, axis);
                hipAxis[i] = this.upperLegToHipJoint[i].oMultiply((axis.oMultiply(modelAxis.Transpose())));

                // get the axis for the knee joint
                GetBoneAxis(kneeOrigin, jointOrigins[i], kneeDir, axis);
                kneeAxis[i] = this.lowerLegToKneeJoint[i].oMultiply((axis.oMultiply(modelAxis.Transpose())));
            }

            // set the joint mods
            this.animator.SetJointAxis(this.waistJoint, JOINTMOD_WORLD_OVERRIDE, waistAxis);
            this.animator.SetJointPos(this.waistJoint, JOINTMOD_WORLD_OVERRIDE, (waistOrigin.oPlus(this.waistOffset.oMinus(modelOrigin))).oMultiply(modelAxis.Transpose()));
            for (i = 0; i < this.numLegs; i++) {
                this.animator.SetJointAxis(this.hipJoints[i], JOINTMOD_WORLD_OVERRIDE, hipAxis[i]);
                this.animator.SetJointAxis(this.kneeJoints[i], JOINTMOD_WORLD_OVERRIDE, kneeAxis[i]);
                this.animator.SetJointAxis(this.ankleJoints[i], JOINTMOD_WORLD_OVERRIDE, ankleAxis[i]);
            }

            this.ik_activate = true;
        }

        @Override
        public void ClearJointMods() {
            int i;

            if ((null == this.self) || !this.ik_activate) {
                return;
            }

            this.animator.SetJointAxis(this.waistJoint, JOINTMOD_NONE, getMat3_identity());
            this.animator.SetJointPos(this.waistJoint, JOINTMOD_NONE, getVec3_origin());
            for (i = 0; i < this.numLegs; i++) {
                this.animator.SetJointAxis(this.hipJoints[i], JOINTMOD_NONE, getMat3_identity());
                this.animator.SetJointAxis(this.kneeJoints[i], JOINTMOD_NONE, getMat3_identity());
                this.animator.SetJointAxis(this.ankleJoints[i], JOINTMOD_NONE, getMat3_identity());
            }

            this.ik_activate = false;
        }

        public void EnableAll() {
            this.enabledLegs = (1 << this.numLegs) - 1;
            this.oldHeightsValid = false;
        }

        public void DisableAll() {
            this.enabledLegs = 0;
            this.oldHeightsValid = false;
        }

        public void EnableLeg(int num) {
            this.enabledLegs |= 1 << num;
        }

        public void DisableLeg(int num) {
            this.enabledLegs &= ~(1 << num);
        }
    }

    /*
     ===============================================================================

     IK controller for reaching a position with an arm or leg.

     ===============================================================================
     */
    /*
     ===============================================================================

     idIK_Reach

     ===============================================================================
     */
    public static class idIK_Reach extends idIK {

        private static final int MAX_ARMS = 2;
        //
        private int numArms;
        private int enabledArms;
        private final int/*jointHandle_t*/[] handJoints = new int[MAX_ARMS];
        private final int/*jointHandle_t*/[] elbowJoints = new int[MAX_ARMS];
        private final int/*jointHandle_t*/[] shoulderJoints = new int[MAX_ARMS];
        private final int/*jointHandle_t*/[] dirJoints = new int[MAX_ARMS];
        //
        private final idVec3[] shoulderForward = new idVec3[MAX_ARMS];
        private final idVec3[] elbowForward = new idVec3[MAX_ARMS];
        //
        private final float[] upperArmLength = new float[MAX_ARMS];
        private final float[] lowerArmLength = new float[MAX_ARMS];
        //
        private final idMat3[] upperArmToShoulderJoint = new idMat3[MAX_ARMS];
        private final idMat3[] lowerArmToElbowJoint = new idMat3[MAX_ARMS];
        //
        //

        public idIK_Reach() {
            int i;

            this.initialized = false;
            this.numArms = 0;
            this.enabledArms = 0;
            for (i = 0; i < MAX_ARMS; i++) {
                this.handJoints[i] = INVALID_JOINT;
                this.elbowJoints[i] = INVALID_JOINT;
                this.shoulderJoints[i] = INVALID_JOINT;
                this.dirJoints[i] = INVALID_JOINT;
                this.shoulderForward[i].Zero();
                this.elbowForward[i].Zero();
                this.upperArmLength[i] = 0;
                this.lowerArmLength[i] = 0;
                this.upperArmToShoulderJoint[i].Identity();
                this.lowerArmToElbowJoint[i].Identity();
            }
        }
        // virtual					~idIK_Reach( void );

        @Override
        public void Save(idSaveGame savefile) {
            int i;
            super.Save(savefile);

            savefile.WriteInt(this.numArms);
            savefile.WriteInt(this.enabledArms);
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteInt(this.handJoints[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteInt(this.elbowJoints[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteInt(this.shoulderJoints[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteInt(this.dirJoints[i]);
            }

            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteVec3(this.shoulderForward[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteVec3(this.elbowForward[i]);
            }

            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteFloat(this.upperArmLength[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteFloat(this.lowerArmLength[i]);
            }

            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteMat3(this.upperArmToShoulderJoint[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.WriteMat3(this.lowerArmToElbowJoint[i]);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            super.Restore(savefile);

            this.numArms = savefile.ReadInt();
            this.enabledArms = savefile.ReadInt();
            for (i = 0; i < MAX_ARMS; i++) {
                this.handJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_ARMS; i++) {
                this.elbowJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_ARMS; i++) {
                this.shoulderJoints[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_ARMS; i++) {
                this.dirJoints[i] = savefile.ReadInt();
            }

            for (i = 0; i < MAX_ARMS; i++) {
                savefile.ReadVec3(this.shoulderForward[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.ReadVec3(this.elbowForward[i]);
            }

            for (i = 0; i < MAX_ARMS; i++) {
                this.upperArmLength[i] = savefile.ReadFloat();
            }
            for (i = 0; i < MAX_ARMS; i++) {
                this.lowerArmLength[i] = savefile.ReadFloat();
            }

            for (i = 0; i < MAX_ARMS; i++) {
                savefile.ReadMat3(this.upperArmToShoulderJoint[i]);
            }
            for (i = 0; i < MAX_ARMS; i++) {
                savefile.ReadMat3(this.lowerArmToElbowJoint[i]);
            }
        }

        @Override
        public boolean Init(idEntity self, final String anim, final idVec3 modelOffset) {
            int i;
            String jointName;
            final idTraceModel trm = new idTraceModel();
            idVec3 dir = new idVec3(), handOrigin, elbowOrigin, shoulderOrigin, dirOrigin;
            final idMat3 axis = new idMat3();
			idMat3 handAxis = new idMat3(), elbowAxis, shoulderAxis;

            if (null == self) {
                return false;
            }

            this.numArms = Min(self.spawnArgs.GetInt("ik_numArms", "0"), MAX_ARMS);
            if (this.numArms == 0) {
                return true;
            }

            if (!super.Init(self, anim, modelOffset)) {
                return false;
            }

            final int numJoints = this.animator.NumJoints();
            final idJointMat[] joints = new idJointMat[numJoints];

            // create the animation frame used to setup the IK
            GameEdit.gameEdit.ANIM_CreateAnimFrame(this.animator.ModelHandle(), this.animator.GetAnim(this.modifiedAnim).MD5Anim(0), numJoints, joints, 1, this.animator.ModelDef().GetVisualOffset().oPlus(modelOffset), this.animator.RemoveOrigin());

            this.enabledArms = 0;

            // get all the joints
            for (i = 0; i < this.numArms; i++) {

                jointName = self.spawnArgs.GetString(va("ik_hand%d", i + 1));
                this.handJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.handJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Reach::Init: invalid hand joint '%s'", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_elbow%d", i + 1));
                this.elbowJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.elbowJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Reach::Init: invalid elbow joint '%s'\n", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_shoulder%d", i + 1));
                this.shoulderJoints[i] = this.animator.GetJointHandle(jointName);
                if (this.shoulderJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idIK_Reach::Init: invalid shoulder joint '%s'\n", jointName);
                }

                jointName = self.spawnArgs.GetString(va("ik_elbowDir%d", i + 1));
                this.dirJoints[i] = this.animator.GetJointHandle(jointName);

                this.enabledArms |= 1 << i;
            }

            // get the arm bone lengths and rotation matrices
            for (i = 0; i < this.numArms; i++) {

                handAxis = joints[ this.handJoints[ i]].ToMat3();
                handOrigin = joints[ this.handJoints[ i]].ToVec3();

                elbowAxis = joints[ this.elbowJoints[ i]].ToMat3();
                elbowOrigin = joints[ this.elbowJoints[ i]].ToVec3();

                shoulderAxis = joints[ this.shoulderJoints[ i]].ToMat3();
                shoulderOrigin = joints[ this.shoulderJoints[ i]].ToVec3();

                // get the IK direction
                if (this.dirJoints[i] != INVALID_JOINT) {
                    dirOrigin = joints[ this.dirJoints[ i]].ToVec3();
                    dir = dirOrigin.oMinus(elbowOrigin);
                } else {
                    dir.Set(-1.0f, 0.0f, 0.0f);
                }

                this.shoulderForward[i] = dir.oMultiply(shoulderAxis.Transpose());
                this.elbowForward[i] = dir.oMultiply(elbowAxis.Transpose());

                // conversion from upper arm bone axis to should joint axis
                this.upperArmLength[i] = GetBoneAxis(shoulderOrigin, elbowOrigin, dir, axis);
                this.upperArmToShoulderJoint[i] = shoulderAxis.oMultiply(axis.Transpose());

                // conversion from lower arm bone axis to elbow joint axis
                this.lowerArmLength[i] = GetBoneAxis(elbowOrigin, handOrigin, dir, axis);
                this.lowerArmToElbowJoint[i] = elbowAxis.oMultiply(axis.Transpose());
            }

            this.initialized = true;

            return true;
        }

        @Override
        public void Evaluate() {
            int i;
            idVec3 modelOrigin, shoulderOrigin = new idVec3();
			final idVec3 elbowOrigin = new idVec3();
			idVec3 handOrigin = new idVec3(), shoulderDir, elbowDir;
            idMat3 modelAxis;
			final idMat3 axis = new idMat3();
            final idMat3[] shoulderAxis = new idMat3[MAX_ARMS], elbowAxis = new idMat3[MAX_ARMS];
            final trace_s[] trace = {null};

            modelOrigin = this.self.GetRenderEntity().origin;
            modelAxis = this.self.GetRenderEntity().axis;

            // solve IK
            for (i = 0; i < this.numArms; i++) {

                // get the position of the shoulder in world space
                this.animator.GetJointTransform(this.shoulderJoints[i], gameLocal.time, shoulderOrigin, axis);
                shoulderOrigin = modelOrigin.oPlus(shoulderOrigin.oMultiply(modelAxis));
                shoulderDir = this.shoulderForward[i].oMultiply(axis.oMultiply(modelAxis));

                // get the position of the hand in world space
                this.animator.GetJointTransform(this.handJoints[i], gameLocal.time, handOrigin, axis);
                handOrigin = modelOrigin.oPlus(handOrigin.oMultiply(modelAxis));

                // get first collision going from shoulder to hand
                gameLocal.clip.TracePoint(trace, shoulderOrigin, handOrigin, CONTENTS_SOLID, this.self);
                handOrigin = trace[0].endpos;

                // get the IK bend direction
                this.animator.GetJointTransform(this.elbowJoints[i], gameLocal.time, elbowOrigin, axis);
                elbowDir = this.elbowForward[i].oMultiply(axis.oMultiply(modelAxis));

                // solve IK and calculate elbow position
                SolveTwoBones(shoulderOrigin, handOrigin, elbowDir, this.upperArmLength[i], this.lowerArmLength[i], elbowOrigin);

                if (ik_debug.GetBool()) {
                    gameRenderWorld.DebugLine(colorCyan, shoulderOrigin, elbowOrigin);
                    gameRenderWorld.DebugLine(colorRed, elbowOrigin, handOrigin);
                    gameRenderWorld.DebugLine(colorYellow, elbowOrigin, elbowOrigin.oPlus(elbowDir));
                    gameRenderWorld.DebugLine(colorGreen, elbowOrigin, elbowOrigin.oPlus(shoulderDir));
                }

                // get the axis for the shoulder joint
                GetBoneAxis(shoulderOrigin, elbowOrigin, shoulderDir, axis);
                shoulderAxis[i] = this.upperArmToShoulderJoint[i].oMultiply(axis.oMultiply(modelAxis.Transpose()));

                // get the axis for the elbow joint
                GetBoneAxis(elbowOrigin, handOrigin, elbowDir, axis);
                elbowAxis[i] = this.lowerArmToElbowJoint[i].oMultiply(axis.oMultiply(modelAxis.Transpose()));
            }

            for (i = 0; i < this.numArms; i++) {
                this.animator.SetJointAxis(this.shoulderJoints[i], JOINTMOD_WORLD_OVERRIDE, shoulderAxis[i]);
                this.animator.SetJointAxis(this.elbowJoints[i], JOINTMOD_WORLD_OVERRIDE, elbowAxis[i]);
            }

            this.ik_activate = true;
        }

        @Override
        public void ClearJointMods() {
            int i;

            if ((null == this.self) || !this.ik_activate) {
                return;
            }

            for (i = 0; i < this.numArms; i++) {
                this.animator.SetJointAxis(this.shoulderJoints[i], JOINTMOD_NONE, getMat3_identity());
                this.animator.SetJointAxis(this.elbowJoints[i], JOINTMOD_NONE, getMat3_identity());
                this.animator.SetJointAxis(this.handJoints[i], JOINTMOD_NONE, getMat3_identity());
            }

            this.ik_activate = false;
        }
    }
}
