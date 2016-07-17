package neo.Game.Physics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Arrays;
import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import static neo.Game.Entity.TH_PHYSICS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.af_contactFrictionScale;
import static neo.Game.GameSys.SysCvar.af_forceFriction;
import static neo.Game.GameSys.SysCvar.af_highlightBody;
import static neo.Game.GameSys.SysCvar.af_highlightConstraint;
import static neo.Game.GameSys.SysCvar.af_jointFrictionScale;
import static neo.Game.GameSys.SysCvar.af_maxAngularVelocity;
import static neo.Game.GameSys.SysCvar.af_maxLinearVelocity;
import static neo.Game.GameSys.SysCvar.af_showActive;
import static neo.Game.GameSys.SysCvar.af_showBodies;
import static neo.Game.GameSys.SysCvar.af_showBodyNames;
import static neo.Game.GameSys.SysCvar.af_showConstrainedBodies;
import static neo.Game.GameSys.SysCvar.af_showConstraintNames;
import static neo.Game.GameSys.SysCvar.af_showConstraints;
import static neo.Game.GameSys.SysCvar.af_showInertia;
import static neo.Game.GameSys.SysCvar.af_showLimits;
import static neo.Game.GameSys.SysCvar.af_showMass;
import static neo.Game.GameSys.SysCvar.af_showPrimaryOnly;
import static neo.Game.GameSys.SysCvar.af_showTimings;
import static neo.Game.GameSys.SysCvar.af_showTotalMass;
import static neo.Game.GameSys.SysCvar.af_showTrees;
import static neo.Game.GameSys.SysCvar.af_showVelocity;
import static neo.Game.GameSys.SysCvar.af_skipFriction;
import static neo.Game.GameSys.SysCvar.af_skipLimits;
import static neo.Game.GameSys.SysCvar.af_skipSelfCollision;
import static neo.Game.GameSys.SysCvar.af_timeScale;
import static neo.Game.GameSys.SysCvar.af_useImpulseFriction;
import static neo.Game.GameSys.SysCvar.af_useJointImpulseFriction;
import static neo.Game.GameSys.SysCvar.af_useLinearTime;
import static neo.Game.GameSys.SysCvar.af_useSymmetry;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.impactInfo_s;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_BALLANDSOCKETJOINT;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_CONELIMIT;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_CONTACT;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_FIXED;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_FRICTION;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_HINGE;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_HINGESTEERING;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_INVALID;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_PLANE;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_PYRAMIDLIMIT;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_SLIDER;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_SPRING;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_SUSPENSION;
import static neo.Game.Physics.Physics_AF.constraintType_t.CONSTRAINT_UNIVERSALJOINT;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Lib.idLib.cvarSystem;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.Timer.idTimer;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Lcp.idLCP;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.FLOAT_IS_NAN;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.SkewSymmetric;
import static neo.idlib.math.Matrix.idMat3.TransposeMultiply;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Matrix.idMat3.getMat3_zero;
import neo.idlib.math.Matrix.idMatX;
import static neo.idlib.math.Matrix.idMatX.MATX_ALLOCA;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import static neo.idlib.math.Vector.getVec6_infinity;
import static neo.idlib.math.Vector.getVec6_origin;
import static neo.idlib.math.Vector.getVec6_zero;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Vector.idVecX;
import static neo.idlib.math.Vector.idVecX.VECX_ALLOCA;

/**
 *
 */
public class Physics_AF {

    /*
     ===================================================================================

     Articulated Figure physics

     Employs a constraint force based dynamic simulation using a lagrangian
     multiplier method to solve for the constraint forces.

     ===================================================================================
     */
    public enum constraintType_t {

        CONSTRAINT_INVALID,
        CONSTRAINT_FIXED,
        CONSTRAINT_BALLANDSOCKETJOINT,
        CONSTRAINT_UNIVERSALJOINT,
        CONSTRAINT_HINGE,
        CONSTRAINT_HINGESTEERING,
        CONSTRAINT_SLIDER,
        CONSTRAINT_CYLINDRICALJOINT,
        CONSTRAINT_LINE,
        CONSTRAINT_PLANE,
        CONSTRAINT_SPRING,
        CONSTRAINT_CONTACT,
        CONSTRAINT_FRICTION,
        CONSTRAINT_CONELIMIT,
        CONSTRAINT_PYRAMIDLIMIT,
        CONSTRAINT_SUSPENSION;

        public static constraintType_t oGet(final int index) {

            if (index > values().length) {
                return values()[0];
            } else {
                return values()[index];
            }
        }
    };
    //
    public static final  float   ERROR_REDUCTION               = 0.5f;
    public static final  float   ERROR_REDUCTION_MAX           = 256.0f;
    public static final  float   LIMIT_ERROR_REDUCTION         = 0.3f;
    public static final  float   LCP_EPSILON                   = 1e-7f;
    public static final  float   LIMIT_LCP_EPSILON             = 1e-4f;
    public static final  float   CONTACT_LCP_EPSILON           = 1e-6f;
    public static final  float   CENTER_OF_MASS_EPSILON        = 1e-4f;
    public static final  float   NO_MOVE_TIME                  = 1.0f;
    public static final  float   NO_MOVE_TRANSLATION_TOLERANCE = 10.0f;
    public static final  float   NO_MOVE_ROTATION_TOLERANCE    = 10.0f;
    public static final  float   MIN_MOVE_TIME                 = -1.0f;
    public static final  float   MAX_MOVE_TIME                 = -1.0f;
    public static final  float   IMPULSE_THRESHOLD             = 500.0f;
    public static final  float   SUSPEND_LINEAR_VELOCITY       = 10.0f;
    public static final  float   SUSPEND_ANGULAR_VELOCITY      = 15.0f;
    public static final  float   SUSPEND_LINEAR_ACCELERATION   = 20.0f;
    public static final  float   SUSPEND_ANGULAR_ACCELERATION  = 30.0f;
    private static final idVec6  vec6_lcp_epsilon              = new idVec6(LCP_EPSILON, LCP_EPSILON, LCP_EPSILON, LCP_EPSILON, LCP_EPSILON, LCP_EPSILON);
    //
    public static final  boolean AF_TIMINGS                    = true;
    public static final  boolean TEST_COLLISION_DETECTION      = false;
    // #ifdef AF_TIMINGS
    static               int     lastTimerReset                = 0;
    static               int     numArticulatedFigures         = 0;
    static               idTimer timer_total                   = new idTimer(),
                                 timer_pc                      = new idTimer(),
                                 timer_ac                      = new idTimer(),
                                 timer_collision               = new idTimer(),
                                 timer_lcp                     = new idTimer();
// #endif

    //===============================================================
    //
    //	idAFConstraint
    //
    //===============================================================
    // base class for all constraints
    public static class idAFConstraint {

        protected constraintType_t type;            // constraint type
        protected idStr name = new idStr();         // name of constraint
        protected idAFBody     body1;               // first constrained body
        protected idAFBody     body2;               // second constrained body, NULL for world
        protected idPhysics_AF physics;             // for adding additional constraints like limits
        //
        // simulation variables set by Evaluate
        protected idMatX       J1, J2;              // matrix with left hand side of constraint equations
        protected idVecX c1, c2;                    // right hand side of constraint equations
        protected idVecX lo, hi, e;                 // low and high bounds and lcp epsilon
        protected idAFConstraint boxConstraint;     // constraint the boxIndex refers to
        protected final int[] boxIndex = new int[6];// indexes for special box constrained variables
        //
        // simulation variables used during calculations
        protected idMatX invI;                      // transformed inertia
        protected idMatX J;                         // transformed constraint matrix
        protected idVecX s;                         // temp solution
        protected idVecX lm;                        // lagrange multipliers
        protected int    firstIndex;                // index of the first constraint row in the lcp matrix
//

        protected static final class constraintFlags_s {

            boolean allowPrimary;//: 1;             // true if the constraint can be used as a primary constraint
            boolean frameConstraint;//: 1;	        // true if this constraint is added to the frame constraints
            boolean noCollision;//: 1;              // true if body1 and body2 never collide with each other
            boolean isPrimary;//: 1;                // true if this is a primary constraint
            boolean isZero;//: 1;                   // true if 's' is zero during calculations
        };
        protected constraintFlags_s fl;
//
//

        // friend class idPhysics_AF;
        // friend class idAFTree;
        public idAFConstraint() {
            type = CONSTRAINT_INVALID;
            name = new idStr("noname");
            body1 = null;
            body2 = null;
            physics = null;

            lo = new idVecX(6);
            lo.SubVec6(0).oSet(getVec6_infinity().oNegative());
            hi = new idVecX(6);
            hi.SubVec6(0).oSet(getVec6_infinity());
            e = new idVecX(6);
            e.SubVec6(0).oSet(vec6_lcp_epsilon);

            boxConstraint = null;
            boxIndex[0] = boxIndex[1] = boxIndex[2] = boxIndex[3] = boxIndex[4] = boxIndex[5] = -1;

            firstIndex = 0;

//	memset( &fl, 0, sizeof( fl ) );
            fl = new constraintFlags_s();
        }
        // virtual					~idAFConstraint( void );

        public constraintType_t GetType() {
            return type;
        }

        public idStr GetName() {
            return name;
        }

        public idAFBody GetBody1() {
            return body1;
        }

        public idAFBody GetBody2() {
            return body2;
        }

        public void SetPhysics(idPhysics_AF p) {
            physics = p;
        }

        public idVecX GetMultiplier() {
            return lm;
        }

        public void SetBody1(idAFBody body) {
            if (!body1.equals(body)) {
                body1 = body;
                if (physics != null) {
                    physics.SetChanged();
                }
            }
        }

        public void SetBody2(idAFBody body) {
            if (!body2.equals(body)) {
                body2 = body;
                if (physics != null) {
                    physics.SetChanged();
                }
            }
        }

        public void DebugDraw() {
        }

        public void GetForce(idAFBody body, idVec6 force) {
            idVecX v = new idVecX();

            v.SetData(6, VECX_ALLOCA(6));
            if (body.equals(body1)) {
                J1.TransposeMultiply(v, lm);
            } else if (body.equals(body2)) {
                J2.TransposeMultiply(v, lm);
            } else {
                v.Zero();
            }
            force.p[0] = v.p[0];
            force.p[1] = v.p[1];
            force.p[2] = v.p[2];
            force.p[3] = v.p[3];
            force.p[4] = v.p[4];
            force.p[5] = v.p[5];
        }

        public void Translate(final idVec3 translation) {
            assert (false);
        }

        public void Rotate(final idRotation rotation) {
            assert (false);
        }

        public void GetCenter(idVec3 center) {
            center.Zero();
        }

        public void Save(idSaveGame saveFile) {
            saveFile.WriteInt(type.ordinal());
        }

        public void Restore(idRestoreGame saveFile) {
            int[] t = {0};
            saveFile.ReadInt(t);//TODO:int to booleans
            assert (t[0] == type.ordinal());
        }

        protected void Evaluate(float invTimeStep) {
            assert (false);
        }

        protected void ApplyFriction(float invTimeStep) {
        }

        protected void InitSize(int size) {
            J1 = new idMatX(size, 6);
            J2 = new idMatX(size, 6);
            c1 = new idVecX(size);
            c2 = new idVecX(size);
            s = new idVecX(size);
            lm = new idVecX(size);
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Fixed
    //
    //===============================================================
    // fixed or rigid joint which allows zero degrees of freedom
    // constrains body1 to have a fixed position and orientation relative to body2
    public static class idAFConstraint_Fixed extends idAFConstraint {

        public idAFConstraint_Fixed(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_FIXED;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(6);
            fl.allowPrimary = true;
            fl.noCollision = true;

            InitOffset();
        }

        public void SetRelativeOrigin(final idVec3 origin) {
            this.offset = origin;
        }

        public void SetRelativeAxis(final idMat3 axis) {
            this.relAxis = axis;
        }

        @Override
        public void SetBody1(idAFBody body) {
            if (!body1.equals(body)) {
                body1 = body;
                InitOffset();
                if (physics != null) {
                    physics.SetChanged();
                }
            }
        }

        @Override
        public void SetBody2(idAFBody body) {
            if (!body2.equals(body)) {
                body2 = body;
                InitOffset();
                if (physics != null) {
                    physics.SetChanged();
                }
            }
        }

        @Override
        public void DebugDraw() {
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();
            if (master != null) {
                gameRenderWorld.DebugLine(colorRed, body1.GetWorldOrigin(), master.GetWorldOrigin());
            } else {
                gameRenderWorld.DebugLine(colorRed, body1.GetWorldOrigin(), getVec3_origin());
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                offset.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                offset.oMulSet(rotation);
                relAxis.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center = body1.GetWorldOrigin();
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(offset);
            saveFile.WriteMat3(relAxis);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(offset);
            saveFile.ReadMat3(relAxis);
        }
//
//
        protected idVec3 offset;						// offset of body1 relative to body2 in body2 space
        protected idMat3 relAxis;					// rotation of body1 relative to body2
//
//

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 ofs, a2 = new idVec3();
            idMat3 ax;
            idRotation r;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                a2 = offset.oMultiply(master.GetWorldAxis());
                ofs = a2.oPlus(master.GetWorldOrigin());
                ax = relAxis.oMultiply(master.GetWorldAxis());
            } else {
                a2.Zero();
                ofs = offset;
                ax = relAxis;
            }

            J1.Set(getMat3_identity(), getMat3_zero(), getMat3_zero(), getMat3_identity());

            if (body2 != null) {
                J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(), getMat3_identity().oNegative());
            } else {
                J2.Zero(6, 6);
            }

            c1.SubVec3(0).oSet(ofs.oMinus(body1.GetWorldOrigin())).oMultiply(-(invTimeStep * ERROR_REDUCTION));
            r = (body1.GetWorldAxis().Transpose().oMultiply(ax)).ToRotation();
            c1.SubVec3(1).oSet(r.GetVec().oMultiply(-(float) DEG2RAD(r.GetAngle()))).oMultiply(-(invTimeStep * ERROR_REDUCTION));

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }

        protected void InitOffset() {
            if (body2 != null) {
                offset = (body1.GetWorldOrigin().oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose());
                relAxis = body1.GetWorldAxis().oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                offset = body1.GetWorldOrigin();
                relAxis = body1.GetWorldAxis();
            }
        }
    };

    //===============================================================
    //
    //	idAFConstraint_BallAndSocketJoint
    //
    //===============================================================
    // ball and socket or spherical joint which allows 3 degrees of freedom
    // constrains body1 relative to body2 with a ball and socket joint
    public static class idAFConstraint_BallAndSocketJoint extends idAFConstraint {

        protected idVec3                                    anchor1;        // anchor in body1 space
        protected idVec3                                    anchor2;        // anchor in body2 space
        protected float                                     friction;       // joint friction
        protected idAFConstraint_ConeLimit                  coneLimit;      // cone shaped limit
        protected idAFConstraint_PyramidLimit               pyramidLimit;   // pyramid shaped limit
        protected idAFConstraint_BallAndSocketJointFriction fc;             // friction constraint
        //
        //

        public idAFConstraint_BallAndSocketJoint(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_BALLANDSOCKETJOINT;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(3);
            coneLimit = null;
            pyramidLimit = null;
            friction = 0.0f;
            fc = null;
            fl.allowPrimary = true;
            fl.noCollision = true;
        }
        // ~idAFConstraint_BallAndSocketJoint( void );

        public void SetAnchor(final idVec3 worldPosition) {

            // get anchor relative to center of mass of body1
            anchor1 = (worldPosition.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
            if (body2 != null) {
                // get anchor relative to center of mass of body2
                anchor2 = (worldPosition.oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                anchor2 = worldPosition;
            }

            if (coneLimit != null) {
                coneLimit.SetAnchor(anchor2);
            }
            if (pyramidLimit != null) {
                pyramidLimit.SetAnchor(anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (body2 != null) {
                return body2.GetWorldOrigin().oPlus(body2.GetWorldAxis().oMultiply(anchor2));
            }
            return anchor2;
        }

        public void SetNoLimit() {
            if (coneLimit != null) {
//		delete coneLimit;
                coneLimit = null;
            }
            if (pyramidLimit != null) {
//		delete pyramidLimit;
                pyramidLimit = null;
            }
        }

        public void SetConeLimit(final idVec3 coneAxis, final float coneAngle, final idVec3 body1Axis) {
            if (pyramidLimit != null) {
//		delete pyramidLimit;
                pyramidLimit = null;
            }
            if (null == coneLimit) {
                coneLimit = new idAFConstraint_ConeLimit();
                coneLimit.SetPhysics(physics);
            }
            if (body2 != null) {
                coneLimit.Setup(body1, body2, anchor2, coneAxis.oMultiply(body2.GetWorldAxis().Transpose()), coneAngle, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            } else {
                coneLimit.Setup(body1, body2, anchor2, coneAxis, coneAngle, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetPyramidLimit(final idVec3 pyramidAxis, final idVec3 baseAxis, final float angle1, final float angle2, final idVec3 body1Axis) {
            if (coneLimit != null) {
//		delete coneLimit;
                coneLimit = null;
            }
            if (null == pyramidLimit) {
                pyramidLimit = new idAFConstraint_PyramidLimit();
                pyramidLimit.SetPhysics(physics);
            }
            if (body2 != null) {
                pyramidLimit.Setup(body1, body2, anchor2, pyramidAxis.oMultiply(body2.GetWorldAxis().Transpose()),
                        baseAxis.oMultiply(body2.GetWorldAxis().Transpose()), angle1, angle2, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            } else {
                pyramidLimit.Setup(body1, body2, anchor2, pyramidAxis, baseAxis, angle1, angle2, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (coneLimit != null) {
                coneLimit.SetEpsilon(e);
            }
            if (pyramidLimit != null) {
                pyramidLimit.SetEpsilon(e);
            }
        }

        public void SetFriction(final float f) {
            friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return friction * physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            idVec3 a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(5, 0, 0)), a1.oPlus(new idVec3(5, 0, 0)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(0, 5, 0)), a1.oPlus(new idVec3(0, 5, 0)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(0, 0, 5)), a1.oPlus(new idVec3(0, 0, 5)));

            if (af_showLimits.GetBool()) {
                if (coneLimit != null) {
                    coneLimit.DebugDraw();
                }
                if (pyramidLimit != null) {
                    pyramidLimit.DebugDraw();
                }
            }
        }

        @Override
        public void GetForce(idAFBody body, idVec6 force) {
            super.GetForce(body, force);
            // FIXME: add limit force
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                anchor2.oPluSet(translation);
            }
            if (coneLimit != null) {
                coneLimit.Translate(translation);
            } else if (pyramidLimit != null) {
                pyramidLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                anchor2.oMulSet(rotation);
            }
            if (coneLimit != null) {
                coneLimit.Rotate(rotation);
            } else if (pyramidLimit != null) {
                pyramidLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center.oSet(body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis())));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(anchor1);
            saveFile.WriteVec3(anchor2);
            saveFile.WriteFloat(friction);
            if (coneLimit != null) {
                coneLimit.Save(saveFile);
            }
            if (pyramidLimit != null) {
                pyramidLimit.Save(saveFile);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(anchor1);
            saveFile.ReadVec3(anchor2);
            saveFile.ReadFloat(friction);

            this.friction = friction[0];

            if (coneLimit != null) {
                coneLimit.Restore(saveFile);
            }
            if (pyramidLimit != null) {
                pyramidLimit.Restore(saveFile);
            }
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2 = new idVec3();
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = anchor1.oMultiply(body1.GetWorldAxis());

            if (master != null) {
                a2 = anchor2.oMultiply(master.GetWorldAxis());
                c1.SubVec3(0).oSet((a2.oPlus(master.GetWorldOrigin().oMinus(a1.oPlus(body1.GetWorldOrigin())))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                c1.SubVec3(0).oSet((anchor2.oMinus(a1.oPlus(body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative());

            if (body2 != null) {
                J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2));
            } else {
                J2.Zero(3, 6);
            }

            if (coneLimit != null) {
                coneLimit.Add(physics, invTimeStep);
            } else if (pyramidLimit != null) {
                pyramidLimit.Add(physics, invTimeStep);
            }
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            idVec3 angular;
            float invMass, currentFriction;

            currentFriction = GetFriction();

            if (currentFriction <= 0.0f) {
                return;
            }

            if (af_useImpulseFriction.GetBool() || af_useJointImpulseFriction.GetBool()) {

                angular = body1.GetAngularVelocity();
                invMass = body1.GetInverseMass();
                if (body2 != null) {
                    angular.oMinus(body2.GetAngularVelocity());
                    invMass += body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                body1.SetAngularVelocity(body1.GetAngularVelocity().oMinus(angular.oMultiply(body1.GetInverseMass())));
                if (body2 != null) {
                    body2.SetAngularVelocity(body2.GetAngularVelocity().oPlus(angular.oMultiply(body2.GetInverseMass())));
                }
            } else {
                if (null == fc) {
                    fc = new idAFConstraint_BallAndSocketJointFriction();
                    fc.Setup(this);
                }

                fc.Add(physics, invTimeStep);
            }
        }
    };

    //===============================================================
    //
    //	idAFConstraint_BallAndSocketJointFriction
    //
    //===============================================================
    // ball and socket joint friction
    public static class idAFConstraint_BallAndSocketJointFriction extends idAFConstraint {

        protected idAFConstraint_BallAndSocketJoint joint;
        //
        //

        public idAFConstraint_BallAndSocketJointFriction() {
            type = CONSTRAINT_FRICTION;
            name.oSet("ballAndSocketJointFriction");
            InitSize(3);
            joint = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_BallAndSocketJoint bsj) {
            this.joint = bsj;
            body1 = bsj.GetBody1();
            body2 = bsj.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float f;

            physics = phys;

            f = joint.GetFriction() * joint.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            lo.p[0] = lo.p[1] = lo.p[2] = -f;
            hi.p[0] = hi.p[1] = hi.p[2] = f;

            J1.Zero(3, 6);
            J1.oSet(0, 3, J1.oSet(1, 4, J1.oSet(2, 5, 1.0f)));

            if (body2 != null) {

                J2.Zero(3, 6);
                J2.oSet(0, 3, J2.oSet(1, 4, J2.oSet(2, 5, 1.0f)));
            }

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFConstraint_UniversalJoint
    //
    //===============================================================
    // universal, Cardan or Hooke joint which allows 2 degrees of freedom
    // like a ball and socket joint but also constrains the rotation about the cardan shafts
    public static class idAFConstraint_UniversalJoint extends idAFConstraint {

        protected idVec3                                anchor1;        // anchor in body1 space
        protected idVec3                                anchor2;        // anchor in body2 space
        protected idVec3                                shaft1;         // body1 cardan shaft in body1 space
        protected idVec3                                shaft2;         // body2 cardan shaft in body2 space
        protected idVec3                                axis1;          // cardan axis in body1 space
        protected idVec3                                axis2;          // cardan axis in body2 space
        protected float                                 friction;       // joint friction
        protected idAFConstraint_ConeLimit              coneLimit;      // cone shaped limit
        protected idAFConstraint_PyramidLimit           pyramidLimit;   // pyramid shaped limit
        protected idAFConstraint_UniversalJointFriction fc;             // friction constraint
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idAFConstraint_UniversalJoint(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            this.anchor1 = new idVec3();
            this.anchor2 = new idVec3();
            this.shaft1 = new idVec3();
            this.shaft2 = new idVec3();
            this.axis1 = new idVec3();
            this.axis2 = new idVec3();
            type = CONSTRAINT_UNIVERSALJOINT;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(4);
            coneLimit = null;
            pyramidLimit = null;
            friction = 0.0f;
            fc = null;
            fl.allowPrimary = true;
            fl.noCollision = true;
        }
        // ~idAFConstraint_UniversalJoint();

        private static int DBG_SetAnchor = 0;
        public void SetAnchor(final idVec3 worldPosition) {DBG_SetAnchor++;

            // get anchor relative to center of mass of body1
            anchor1.oSet((worldPosition.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose()));
            if (body2 != null) {
                // get anchor relative to center of mass of body2
                anchor2.oSet((worldPosition.oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose()));
            } else {
                anchor2.oSet(worldPosition);
            }

            if (coneLimit != null) {
                coneLimit.SetAnchor(anchor2);
            }
            if (pyramidLimit != null) {
                pyramidLimit.SetAnchor(anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (body2 != null) {
                return body2.GetWorldOrigin().oPlus(body2.GetWorldAxis().oMultiply(anchor2));
            }
            return anchor2;
        }

        public void SetShafts(final idVec3 cardanShaft1, final idVec3 cardanShaft2) {
            idVec3 cardanAxis;
            float l;

            shaft1.oSet(cardanShaft1);
            l = shaft1.Normalize();
            assert (l != 0.0f);
            shaft2.oSet(cardanShaft2);
            l = shaft2.Normalize();
            assert (l != 0.0f);

            // the cardan axis is a vector orthogonal to both cardan shafts
            cardanAxis = shaft1.Cross(shaft2);
            if (cardanAxis.Normalize() == 0.0f) {
//		idVec3 vecY = new idVec3();
//		shaft1.OrthogonalBasis( cardanAxis, vecY );
                shaft1.OrthogonalBasis(cardanAxis, new idVec3());
                cardanAxis.Normalize();
            }

            shaft1.oMulSet(body1.GetWorldAxis().Transpose());
            axis1.oSet(cardanAxis.oMulSet(body1.GetWorldAxis().Transpose()));
            if (body2 != null) {
                shaft2.oMulSet(body2.GetWorldAxis().Transpose());
                axis2.oSet(cardanAxis.oMultiply(body2.GetWorldAxis().Transpose()));
            } else {
                axis2.oSet(cardanAxis);
            }

            if (coneLimit != null) {
                coneLimit.SetBody1Axis(shaft1);
            }
            if (pyramidLimit != null) {
                pyramidLimit.SetBody1Axis(shaft1);
            }
        }

        public void GetShafts(idVec3 cardanShaft1, idVec3 cardanShaft2) {
            cardanShaft1.oSet(shaft1);
            cardanShaft2.oSet(shaft2);
        }

        public void SetNoLimit() {
            if (coneLimit != null) {
                coneLimit = null;
            }
            if (pyramidLimit != null) {
                pyramidLimit = null;
            }
        }

        public void SetConeLimit(final idVec3 coneAxis, final float coneAngle) {
            if (pyramidLimit != null) {
                pyramidLimit = null;
            }
            if (null == coneLimit) {
                coneLimit = new idAFConstraint_ConeLimit();
                coneLimit.SetPhysics(physics);
            }
            if (body2 != null) {
                coneLimit.Setup(body1, body2, anchor2, coneAxis.oMultiply(body2.GetWorldAxis().Transpose()), coneAngle, shaft1);
            } else {
                coneLimit.Setup(body1, body2, anchor2, coneAxis, coneAngle, shaft1);
            }
        }

        public void SetPyramidLimit(final idVec3 pyramidAxis, final idVec3 baseAxis, final float angle1, final float angle2) {
            if (coneLimit != null) {
                coneLimit = null;
            }
            if (null == pyramidLimit) {
                pyramidLimit = new idAFConstraint_PyramidLimit();
                pyramidLimit.SetPhysics(physics);
            }
            if (body2 != null) {
                pyramidLimit.Setup(body1, body2, anchor2, pyramidAxis.oMultiply(body2.GetWorldAxis().Transpose()), baseAxis.oMultiply(body2.GetWorldAxis().Transpose()), angle1, angle2, shaft1);
            } else {
                pyramidLimit.Setup(body1, body2, anchor2, pyramidAxis, baseAxis, angle1, angle2, shaft1);
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (coneLimit != null) {
                coneLimit.SetEpsilon(e);
            }
            if (pyramidLimit != null) {
                pyramidLimit.SetEpsilon(e);
            }
        }

        public void SetFriction(final float f) {
            friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return friction * physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            idVec3 a1, a2, s1, s2, d1, d2, v;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            s1 = shaft1.oMultiply(body1.GetWorldAxis());
            d1 = axis1.oMultiply(body1.GetWorldAxis());

            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(anchor2.oMultiply(master.GetWorldAxis()));
                s2 = shaft2.oMultiply(master.GetWorldAxis());
                d2 = axis2.oMultiply(master.GetWorldAxis());
            } else {
                a2 = anchor2;
                s2 = shaft2;
                d2 = axis2;
            }

            v = s1.Cross(s2);
            if (v.Normalize() != 0.0f) {
                idMat3 m1, m2;

                m1 = new idMat3(s1, v, v.Cross(s1));

                m2 = new idMat3(s2.oNegative(), v, v.Cross(s2.oNegative()));

                d2.oMulSet(m2.Transpose().oMultiply(m1));
            }

            gameRenderWorld.DebugArrow(colorCyan, a1, a1.oPlus(s1.oMultiply(5.0f)), 1);
            gameRenderWorld.DebugArrow(colorBlue, a2, a2.oPlus(s2.oMultiply(5.0f)), 1);
            gameRenderWorld.DebugLine(colorGreen, a1, a1.oPlus(d1.oMultiply(5.0f)));
            gameRenderWorld.DebugLine(colorGreen, a2, a2.oPlus(d2.oMultiply(5.0f)));

            if (af_showLimits.GetBool()) {
                if (coneLimit != null) {
                    coneLimit.DebugDraw();
                }
                if (pyramidLimit != null) {
                    pyramidLimit.DebugDraw();
                }
            }
        }

        @Override
        public void GetForce(idAFBody body, idVec6 force) {
            super.GetForce(body, force);
            // FIXME: add limit force
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                anchor2.oPluSet(translation);
            }
            if (coneLimit != null) {
                coneLimit.Translate(translation);
            } else if (pyramidLimit != null) {
                pyramidLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                anchor2.oMulSet(rotation);
                shaft2.oMulSet(rotation.ToMat3());
                axis2.oMulSet(rotation.ToMat3());
            }
            if (coneLimit != null) {
                coneLimit.Rotate(rotation);
            } else if (pyramidLimit != null) {
                pyramidLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center.oSet(body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis())));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(anchor1);
            saveFile.WriteVec3(anchor2);
            saveFile.WriteVec3(shaft1);
            saveFile.WriteVec3(shaft2);
            saveFile.WriteVec3(axis1);
            saveFile.WriteVec3(axis2);
            saveFile.WriteFloat(friction);
            if (coneLimit != null) {
                coneLimit.Save(saveFile);
            }
            if (pyramidLimit != null) {
                pyramidLimit.Save(saveFile);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(anchor1);
            saveFile.ReadVec3(anchor2);
            saveFile.ReadVec3(shaft1);
            saveFile.ReadVec3(shaft2);
            saveFile.ReadVec3(axis1);
            saveFile.ReadVec3(axis2);
            saveFile.ReadFloat(friction);

            this.friction = friction[0];

            if (coneLimit != null) {
                coneLimit.Restore(saveFile);
            }
            if (pyramidLimit != null) {
                pyramidLimit.Restore(saveFile);
            }
        }

        /*
         ================
         idAFConstraint_UniversalJoint::Evaluate

         NOTE: this joint is homokinetic
         ================
         */
        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2, s1, s2, d1, d2, v;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = anchor1.oMultiply(body1.GetWorldAxis());
            s1 = shaft1.oMultiply(body1.GetWorldAxis());
            d1 = s1.Cross(axis1.oMultiply(body1.GetWorldAxis()));

            if (master != null) {
                a2 = anchor2.oMultiply(master.GetWorldAxis());
                s2 = shaft2.oMultiply(master.GetWorldAxis());
                d2 = axis2.oMultiply(master.GetWorldAxis());
                c1.SubVec3(0).oSet((a2.oPlus(master.GetWorldOrigin().oMinus(a1.oPlus(body1.GetWorldOrigin())))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                a2 = anchor2;
                s2 = shaft2;
                d2 = axis2;
                c1.SubVec3(0).oSet((a2.oMinus(a1.oPlus(body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative(), getMat3_zero(),
                    new idMat3(s1.oGet(0), s1.oGet(1), s1.oGet(2),
                            0.0f, 0.0f, 0.0f,
                            0.0f, 0.0f, 0.0f));
            J1.SetSize(4, 6);

            if (body2 != null) {
                J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(),
                        new idMat3(s2.oGet(0), s2.oGet(1), s2.oGet(2),
                                0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f));
                J2.SetSize(4, 6);
            } else {
                J2.Zero(4, 6);
            }

            v = s1.Cross(s2);
            if (v.Normalize() != 0.0f) {
                idMat3 m1, m2;

                m1 = new idMat3(s1, v, v.Cross(s1));

                m2 = new idMat3(s2.oNegative(), v, v.Cross(s2.oNegative()));

                d2.oMulSet(m2.Transpose().oMultiply(m1));
            }

            c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (d1.oMultiply(d2));

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            if (coneLimit != null) {
                coneLimit.Add(physics, invTimeStep);
            } else if (pyramidLimit != null) {
                pyramidLimit.Add(physics, invTimeStep);
            }
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            idVec3 angular;
            float invMass, currentFriction;

            currentFriction = GetFriction();

            if (currentFriction <= 0.0f) {
                return;
            }

            if (af_useImpulseFriction.GetBool() || af_useJointImpulseFriction.GetBool()) {

                angular = body1.GetAngularVelocity();
                invMass = body1.GetInverseMass();
                if (body2 != null) {
                    angular.oMinSet(body2.GetAngularVelocity());
                    invMass += body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                body1.SetAngularVelocity(body1.GetAngularVelocity().oMinus(angular.oMultiply(body1.GetInverseMass())));
                if (body2 != null) {
                    body2.SetAngularVelocity(body2.GetAngularVelocity().oPlus(angular.oMultiply(body2.GetInverseMass())));
                }
            } else {
                if (null == fc) {
                    fc = new idAFConstraint_UniversalJointFriction();
                    fc.Setup(this);
                }

                fc.Add(physics, invTimeStep);
            }
        }
    };

    //===============================================================
    //
    //	idAFConstraint_UniversalJointFriction
    //
    //===============================================================
    // universal joint friction
    public static class idAFConstraint_UniversalJointFriction extends idAFConstraint {

        protected idAFConstraint_UniversalJoint joint;			// universal joint
        //
        //

        public idAFConstraint_UniversalJointFriction() {
            type = CONSTRAINT_FRICTION;
            name.oSet("universalJointFriction");
            InitSize(2);
            joint = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_UniversalJoint uj) {
            this.joint = uj;
            body1 = uj.GetBody1();
            body2 = uj.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            idVec3 s1 = new idVec3(), s2 = new idVec3(), dir1 = new idVec3(), dir2 = new idVec3();
            float f;

            physics = phys;

            f = joint.GetFriction() * joint.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            lo.p[0] = lo.p[1] = -f;
            hi.p[0] = hi.p[1] = f;

            joint.GetShafts(s1, s2);

            s1.oMulSet(body1.GetWorldAxis());
            s1.NormalVectors(dir1, dir2);

            J1.SetSize(2, 6);
            J1.SubVec63_Zero(0, 0);
            J1.SubVec63_oSet(0, 1, dir1);
            J1.SubVec63_Zero(1, 0);
            J1.SubVec63_oSet(1, 1, dir2);

            if (body2 != null) {

                J2.SetSize(2, 6);
                J2.SubVec63_Zero(0, 0);
                J2.SubVec63_oSet(0, 1, dir1.oNegative());
                J2.SubVec63_Zero(1, 0);
                J2.SubVec63_oSet(1, 1, dir2.oNegative());
            }

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFConstraint_CylindricalJoint
    //
    //===============================================================
    // cylindrical joint which allows 2 degrees of freedom
    // constrains body1 to lie on a line relative to body2 and allows only translation along and rotation about the line
    public static class idAFConstraint_CylindricalJoint extends idAFConstraint {

        public idAFConstraint_CylindricalJoint(final idStr name, idAFBody body1, idAFBody body2) {
            assert (false);	// FIXME: implement
        }

        @Override
        public void DebugDraw() {
            assert (false);	// FIXME: implement
        }

        @Override
        public void Translate(final idVec3 translation) {
            assert (false);	// FIXME: implement
        }

        @Override
        public void Rotate(final idRotation rotation) {
            assert (false);	// FIXME: implement
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            assert (false);	// FIXME: implement
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            assert (false);	// FIXME: implement
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Hinge
    //
    //===============================================================
    // hinge, revolute or pin joint which allows 1 degree of freedom
    // constrains all motion of body1 relative to body2 except the rotation about the hinge axis
    public static class idAFConstraint_Hinge extends idAFConstraint {

        protected idVec3                       anchor1;     // anchor in body1 space
        protected idVec3                       anchor2;     // anchor in body2 space
        protected idVec3                       axis1;       // axis in body1 space
        protected idVec3                       axis2;       // axis in body2 space
        protected idMat3                       initialAxis; // initial axis of body1 relative to body2
        protected float                        friction;    // hinge friction
        protected idAFConstraint_ConeLimit     coneLimit;   // cone limit
        protected idAFConstraint_HingeSteering steering;    // steering
        protected idAFConstraint_HingeFriction fc;          // friction constraint
        //
        //

        public idAFConstraint_Hinge(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_HINGE;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(5);
            coneLimit = null;
            steering = null;
            friction = 0.0f;
            fc = null;
            fl.allowPrimary = true;
            fl.noCollision = true;
            initialAxis = body1.GetWorldAxis();
            if (body2 != null) {
                initialAxis.oMulSet(body2.GetWorldAxis().Transpose());
            }
        }
        // ~idAFConstraint_Hinge();

        public void SetAnchor(final idVec3 worldPosition) {
            // get anchor relative to center of mass of body1
            anchor1 = (worldPosition.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
            if (body2 != null) {
                // get anchor relative to center of mass of body2
                anchor2 = (worldPosition.oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                anchor2 = worldPosition;
            }

            if (coneLimit != null) {
                coneLimit.SetAnchor(anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (body2 != null) {
                return body2.GetWorldOrigin().oPlus(body2.GetWorldAxis().oMultiply(anchor2));
            }
            return anchor2;
        }

        public void SetAxis(final idVec3 axis) {
            idVec3 normAxis;

            normAxis = axis;
            normAxis.Normalize();

            // get axis relative to body1
            axis1 = normAxis.oMultiply(body1.GetWorldAxis().Transpose());
            if (body2 != null) {
                // get axis relative to body2
                axis2 = normAxis.oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                axis2 = normAxis;
            }
        }

        public void GetAxis(idVec3 a1, idVec3 a2) {
            a1.oSet(axis1);
            a2.oSet(axis2);
        }

        public idVec3 GetAxis() {
            if (body2 != null) {
                return axis2.oMultiply(body2.GetWorldAxis());
            }
            return axis2;
        }

        public void SetNoLimit() {
            if (coneLimit != null) {
//		delete coneLimit;
                coneLimit = null;
            }
        }

        public void SetLimit(final idVec3 axis, final float angle, final idVec3 body1Axis) {
            if (null == coneLimit) {
                coneLimit = new idAFConstraint_ConeLimit();
                coneLimit.SetPhysics(physics);
            }
            if (body2 != null) {
                coneLimit.Setup(body1, body2, anchor2, axis.oMultiply(body2.GetWorldAxis().Transpose()), angle, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            } else {
                coneLimit.Setup(body1, body2, anchor2, axis, angle, body1Axis.oMultiply(body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (coneLimit != null) {
                coneLimit.SetEpsilon(e);
            }
        }

        public float GetAngle() {
            idMat3 axis;
            idRotation rotation;
            float angle;

            axis = body1.GetWorldAxis().oMultiply(body2.GetWorldAxis().Transpose().oMultiply(initialAxis.Transpose()));
            rotation = axis.ToRotation();
            angle = rotation.GetAngle();
            if (rotation.GetVec().oMultiply(axis1) < 0.0f) {
                return -angle;
            }
            return angle;
        }

        public void SetSteerAngle(final float degrees) {
            if (coneLimit != null) {
//		delete coneLimit;
                coneLimit = null;
            }
            if (null == steering) {
                steering = new idAFConstraint_HingeSteering();
                steering.Setup(this);
            }
            steering.SetSteerAngle(degrees);
        }

        public void SetSteerSpeed(final float speed) {
            if (steering != null) {
                steering.SetSteerSpeed(speed);
            }
        }

        public void SetFriction(final float f) {
            friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return friction * physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            idVec3 vecX = new idVec3(), vecY = new idVec3();
            idVec3 a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            idVec3 x1 = axis1.oMultiply(body1.GetWorldAxis());
            x1.OrthogonalBasis(vecX, vecY);

            gameRenderWorld.DebugArrow(colorBlue, a1.oMinus(x1.oMultiply(4.0f)), a1.oPlus(x1.oMultiply(4.0f)), 1);
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(vecX.oMultiply(2.0f)), a1.oPlus(vecX.oMultiply(2.0f)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(vecY.oMultiply(2.0f)), a1.oPlus(vecY.oMultiply(2.0f)));

            if (af_showLimits.GetBool()) {
                if (coneLimit != null) {
                    coneLimit.DebugDraw();
                }
            }
        }

        @Override
        public void GetForce(idAFBody body, idVec6 force) {
            super.GetForce(body, force);
            // FIXME: add limit force
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                anchor2.oPluSet(translation);
            }
            if (coneLimit != null) {
                coneLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                anchor2.oMulSet(rotation);
                axis2.oMulSet(rotation.ToMat3());
            }
            if (coneLimit != null) {
                coneLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(anchor1);
            saveFile.WriteVec3(anchor2);
            saveFile.WriteVec3(axis1);
            saveFile.WriteVec3(axis2);
            saveFile.WriteMat3(initialAxis);
            saveFile.WriteFloat(friction);
            if (coneLimit != null) {
                saveFile.WriteBool(true);
                coneLimit.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
            if (steering != null) {
                saveFile.WriteBool(true);
                steering.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
            if (fc != null) {
                saveFile.WriteBool(true);
                fc.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            boolean[] b = {false};
            float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(anchor1);
            saveFile.ReadVec3(anchor2);
            saveFile.ReadVec3(axis1);
            saveFile.ReadVec3(axis2);
            saveFile.ReadMat3(initialAxis);
            saveFile.ReadFloat(friction);

            saveFile.ReadBool(b);

            this.friction = friction[0];

            if (b[0]) {
                if (null == coneLimit) {
                    coneLimit = new idAFConstraint_ConeLimit();
                }
                coneLimit.SetPhysics(physics);
                coneLimit.Restore(saveFile);
            }
            saveFile.ReadBool(b);
            if (b[0]) {
                if (null == steering) {
                    steering = new idAFConstraint_HingeSteering();
                }
                steering.Setup(this);
                steering.Restore(saveFile);
            }
            saveFile.ReadBool(b);
            if (b[0]) {
                if (null == fc) {
                    fc = new idAFConstraint_HingeFriction();
                }
                fc.Setup(this);
                fc.Restore(saveFile);
            }
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2;
            idVec3 x1, x2, cross;
            idVec3 vecX = new idVec3(), vecY = new idVec3();
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            x1 = axis1.oMultiply(body1.GetWorldAxis());        // axis in body1 space
            x1.OrthogonalBasis(vecX, vecY);                    // basis for axis in body1 space

            a1 = anchor1.oMultiply(body1.GetWorldAxis());      // anchor in body1 space

            if (master != null) {
                a2 = anchor2.oMultiply(master.GetWorldAxis()); // anchor in master space
                x2 = axis2.oMultiply(master.GetWorldAxis());
                c1.SubVec3(0).oSet((a2.oPlus(master.GetWorldOrigin().oMinus(a1.oPlus(body1.GetWorldOrigin())))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                a2 = anchor2;
                x2 = axis2;
                c1.SubVec3(0).oSet(a2.oMinus(a1.oPlus(body1.GetWorldOrigin())).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative(), getMat3_zero(),
                    new idMat3(vecX.oGet(0), vecX.oGet(1), vecX.oGet(2),
                            vecY.oGet(0), vecY.oGet(1), vecY.oGet(2),
                            0.0f, 0.0f, 0.0f));
            J1.SetSize(5, 6);

            if (body2 != null) {
                J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(),
                        new idMat3(-vecX.oGet(0), -vecX.oGet(1), -vecX.oGet(2),
                                -vecY.oGet(0), -vecY.oGet(1), -vecY.oGet(2),
                                0.0f, 0.0f, 0.0f));
                J2.SetSize(5, 6);
            } else {
                J2.Zero(5, 6);
            }

            cross = x1.Cross(x2);

            c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (cross.oMultiply(vecX));
            c1.p[4] = -(invTimeStep * ERROR_REDUCTION) * (cross.oMultiply(vecY));

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            if (steering != null) {
                steering.Add(physics, invTimeStep);
            } else if (coneLimit != null) {
                coneLimit.Add(physics, invTimeStep);
            }
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            idVec3 angular;
            float invMass, currentFriction;

            currentFriction = GetFriction();

            if (currentFriction <= 0.0f) {
                return;
            }

            if (af_useImpulseFriction.GetBool() || af_useJointImpulseFriction.GetBool()) {

                angular = body1.GetAngularVelocity();
                invMass = body1.GetInverseMass();
                if (body2 != null) {
                    angular.oMinSet(body2.GetAngularVelocity());
                    invMass += body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                body1.SetAngularVelocity(body1.GetAngularVelocity().oMinus(angular.oMultiply(body1.GetInverseMass())));
                if (body2 != null) {
                    body2.SetAngularVelocity(body2.GetAngularVelocity().oPlus(angular.oMultiply(body2.GetInverseMass())));
                }
            } else {
                if (null == fc) {
                    fc = new idAFConstraint_HingeFriction();
                    fc.Setup(this);
                }

                fc.Add(physics, invTimeStep);
            }
        }
    };

    //===============================================================
    //
    //	idAFConstraint_HingeFriction
    //
    //===============================================================
    // hinge joint friction
    public static class idAFConstraint_HingeFriction extends idAFConstraint {

        protected idAFConstraint_Hinge hinge;            // hinge
        //
        //

        public idAFConstraint_HingeFriction() {
            type = CONSTRAINT_FRICTION;
            name.oSet("hingeFriction");
            InitSize(1);
            hinge = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_Hinge h) {
            this.hinge = h;
            body1 = h.GetBody1();
            body2 = h.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            idVec3 a1 = new idVec3(), a2 = new idVec3();
            float f;

            physics = phys;

            f = hinge.GetFriction() * hinge.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            lo.p[0] = -f;
            hi.p[0] = f;

            hinge.GetAxis(a1, a2);

            a1.oMulSet(body1.GetWorldAxis());

            J1.SetSize(1, 6);
            J1.SubVec63_Zero(0, 0);
            J1.SubVec63_oSet(0, 1, a1);

            if (body2 != null) {
                a2.oMulSet(body2.GetWorldAxis());

                J2.SetSize(1, 6);
                J2.SubVec63_Zero(0, 0);
                J2.SubVec63_oSet(0, 1, a2.oNegative());
            }

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFConstraint_HingeSteering
    //
    //===============================================================
    // constrains two bodies attached to each other with a hinge to get a specified relative orientation
    public static class idAFConstraint_HingeSteering extends idAFConstraint {

        protected idAFConstraint_Hinge hinge;            // hinge
        protected float                steerAngle;       // desired steer angle in degrees
        protected float                steerSpeed;       // steer speed
        protected float                epsilon;          // lcp epsilon
        //
        //

        public idAFConstraint_HingeSteering() {
            type = CONSTRAINT_HINGESTEERING;
            name.oSet("hingeFriction");
            InitSize(1);
            hinge = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
            steerSpeed = 0.0f;
            epsilon = LCP_EPSILON;
        }

        public void Setup(idAFConstraint_Hinge h) {
            this.hinge = h;
            body1 = h.GetBody1();
            body2 = h.GetBody2();
        }

        public void SetSteerAngle(final float degrees) {
            steerAngle = degrees;
        }

        public void SetSteerSpeed(final float speed) {
            steerSpeed = speed;
        }

        public void SetEpsilon(final float e) {
            epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float angle, speed;
            idVec3 a1 = new idVec3(), a2 = new idVec3();

            physics = phys;

            hinge.GetAxis(a1, a2);
            angle = hinge.GetAngle();

            a1.oMulSet(body1.GetWorldAxis());

            J1.SetSize(1, 6);
            J1.SubVec63_Zero(0, 0);
            J1.SubVec63_oSet(0, 1, a1);

            if (body2 != null) {
                a2.oMulSet(body2.GetWorldAxis());

                J2.SetSize(1, 6);
                J2.SubVec63_Zero(0, 0);
                J2.SubVec63_oSet(0, 1, a2.oNegative());
            }

            speed = steerAngle - angle;
            if (steerSpeed != 0.0f) {
                if (speed > steerSpeed) {
                    speed = steerSpeed;
                } else if (speed < -steerSpeed) {
                    speed = -steerSpeed;
                }
            }

            c1.p[0] = (float) (DEG2RAD(speed) * invTimeStep);

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }

        @Override
        public void Save(idSaveGame saveFile) {
            saveFile.WriteFloat(steerAngle);
            saveFile.WriteFloat(steerSpeed);
            saveFile.WriteFloat(epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[] steerAngle = {0};//TODO:check if these read pointers need to have the original values set instead of zero;
            float[] steerSpeed = {0};
            float[] epsilon = {0};

            saveFile.ReadFloat(steerAngle);
            saveFile.ReadFloat(steerSpeed);
            saveFile.ReadFloat(epsilon);

            this.steerAngle = steerAngle[0];
            this.steerSpeed = steerSpeed[0];
            this.epsilon = epsilon[0];
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Slider
    //
    //===============================================================
    // slider, prismatic or translational constraint which allows 1 degree of freedom
    // constrains body1 to lie on a line relative to body2, the orientation is also fixed relative to body2
    public static class idAFConstraint_Slider extends idAFConstraint {

        protected idVec3 axis;                // axis along which body1 slides in body2 space
        protected idVec3 offset;              // offset of body1 relative to body2
        protected idMat3 relAxis;             // rotation of body1 relative to body2
        //
        //

        public idAFConstraint_Slider(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_SLIDER;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(5);
            fl.allowPrimary = true;
            fl.noCollision = true;

            if (body2 != null) {
                offset = (body1.GetWorldOrigin().oMinus(body2.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
                relAxis = body1.GetWorldAxis().oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                offset = body1.GetWorldOrigin();
                relAxis = body1.GetWorldAxis();
            }
        }

        public void SetAxis(final idVec3 ax) {
            idVec3 normAxis;

            // get normalized axis relative to body1
            normAxis = ax;//TODO:unreferenced clone!?
            normAxis.Normalize();
            if (body2 != null) {
                axis = normAxis.oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                axis = normAxis;
            }
        }

        @Override
        public void DebugDraw() {
            idVec3 ofs;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();
            if (master != null) {
                ofs = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(offset).oMinus(body1.GetWorldOrigin()));
            } else {
                ofs = offset.oMinus(body1.GetWorldOrigin());
            }
            gameRenderWorld.DebugLine(colorGreen, ofs, ofs.oPlus(axis.oMultiply(body1.GetWorldAxis())));
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                offset.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                offset.oMulSet(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();
            if (master != null) {
                center = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(offset).oMinus(body1.GetWorldOrigin()));
            } else {
                center = offset.oMinus(body1.GetWorldOrigin());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(axis);
            saveFile.WriteVec3(offset);
            saveFile.WriteMat3(relAxis);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(axis);
            saveFile.ReadVec3(offset);
            saveFile.ReadMat3(relAxis);
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 vecX = new idVec3(), vecY = new idVec3(), ofs;
            idRotation r;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                (axis.oMultiply(master.GetWorldAxis())).OrthogonalBasis(vecX, vecY);
                ofs = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(offset).oMinus(body1.GetWorldOrigin()));
                r = (body1.GetWorldAxis().Transpose().oMultiply((relAxis.oMultiply(master.GetWorldAxis()))).ToRotation());
            } else {
                axis.OrthogonalBasis(vecX, vecY);
                ofs = offset.oMinus(body1.GetWorldOrigin());
                r = (body1.GetWorldAxis().Transpose().oMultiply(relAxis)).ToRotation();
            }

            J1.Set(getMat3_zero(), getMat3_identity(),
                    new idMat3(vecX, vecY, getVec3_origin()), getMat3_zero());
            J1.SetSize(5, 6);

            if (body2 != null) {

                J2.Set(getMat3_zero(), getMat3_identity().oNegative(),
                        new idMat3(vecX.oNegative(), vecY.oNegative(), getVec3_origin()), getMat3_zero());
                J2.SetSize(5, 6);
            } else {
                J2.Zero(5, 6);
            }

            c1.SubVec3(0).oSet((r.GetVec().oMultiply(-(float) DEG2RAD(r.GetAngle()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));

            c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (vecX.oMultiply(ofs));
            c1.p[4] = -(invTimeStep * ERROR_REDUCTION) * (vecY.oMultiply(ofs));

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Line
    //
    //===============================================================
    // line constraint which allows 4 degrees of freedom
    // constrains body1 to lie on a line relative to body2, does not constrain the orientation.
    public static class idAFConstraint_Line extends idAFConstraint {

        public idAFConstraint_Line(final idStr name, idAFBody body1, idAFBody body2) {
            assert (false);    // FIXME: implement
        }

        @Override
        public void DebugDraw() {
            assert (false);    // FIXME: implement
        }

        @Override
        public void Translate(final idVec3 translation) {
            assert (false);    // FIXME: implement
        }

        @Override
        public void Rotate(final idRotation rotation) {
            assert (false);    // FIXME: implement
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            assert (false);    // FIXME: implement
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            assert (false);    // FIXME: implement
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Plane
    //
    //===============================================================
    // plane constraint which allows 5 degrees of freedom
    // constrains body1 to lie in a plane relative to body2, does not constrain the orientation.
    public static class idAFConstraint_Plane extends idAFConstraint {

        protected idVec3 anchor1;            // anchor in body1 space
        protected idVec3 anchor2;            // anchor in body2 space
        protected idVec3 planeNormal;        // plane normal in body2 space
        //
        //

        public idAFConstraint_Plane(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_PLANE;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(1);
            fl.allowPrimary = true;
            fl.noCollision = true;
        }

        public void SetPlane(final idVec3 normal, final idVec3 anchor) {
            // get anchor relative to center of mass of body1
            anchor1 = (anchor.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
            if (body2 != null) {
                // get anchor relative to center of mass of body2
                anchor2 = (anchor.oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose());
                planeNormal = normal.oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                anchor2 = anchor;
                planeNormal = normal;
            }
        }

        @Override
        public void DebugDraw() {
            idVec3 a1, normal, right = new idVec3(), up = new idVec3();
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            if (master != null) {
                normal = planeNormal.oMultiply(master.GetWorldAxis());
            } else {
                normal = planeNormal;
            }
            normal.NormalVectors(right, up);
            normal.oMulSet(4.0f);
            right.oMulSet(4.0f);
            up.oMulSet(4.0f);

            gameRenderWorld.DebugLine(colorCyan, a1.oMinus(right), a1.oPlus(right));
            gameRenderWorld.DebugLine(colorCyan, a1.oMinus(up), a1.oPlus(up));
            gameRenderWorld.DebugArrow(colorCyan, a1, a1.oPlus(normal), 1);
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                anchor2.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                anchor2.oMulSet(rotation);
                planeNormal.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(anchor1);
            saveFile.WriteVec3(anchor2);
            saveFile.WriteVec3(planeNormal);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(anchor1);
            saveFile.ReadVec3(anchor2);
            saveFile.ReadVec3(planeNormal);
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2, normal, p;
            idVec6 v = new idVec6();
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(anchor2.oMultiply(master.GetWorldAxis()));
                normal = planeNormal.oMultiply(master.GetWorldAxis());
            } else {
                a2 = anchor2;
                normal = planeNormal;
            }

            p = a1.oMinus(body1.GetWorldOrigin());
            v.SubVec3_oSet(0, normal);
            v.SubVec3_oSet(1, p.Cross(normal));
            J1.Set(1, 6, v.ToFloatPtr());

            if (body2 != null) {
                p = a1.oMinus(body2.GetWorldOrigin());
                v.SubVec3_oSet(0, normal.oNegative());
                v.SubVec3_oSet(1, p.Cross(normal.oNegative()));
                J2.Set(1, 6, v.ToFloatPtr());
            }

            c1.p[0] = -(invTimeStep * ERROR_REDUCTION) * (a1.oMultiply(normal) - a2.oMultiply(normal));

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Spring
    //
    //===============================================================
    // spring constraint which allows 6 or 5 degrees of freedom based on the spring limits
    // constrains body1 relative to body2 with a spring
    public static class idAFConstraint_Spring extends idAFConstraint {

        protected idVec3 anchor1;                    // anchor in body1 space
        protected idVec3 anchor2;                    // anchor in body2 space
        protected float  kstretch;                   // spring constant when stretched
        protected float  kcompress;                  // spring constant when compressed
        protected float  damping;                    // spring damping
        protected float  restLength;                 // rest length of spring
        protected float  minLength;                  // minimum spring length
        protected float  maxLength;                  // maximum spring length
        //
        //

        public idAFConstraint_Spring(final idStr name, idAFBody body1, idAFBody body2) {
            assert (body1 != null);
            type = CONSTRAINT_SPRING;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(1);
            fl.allowPrimary = false;
            kstretch = kcompress = damping = 1.0f;
            minLength = maxLength = restLength = 0.0f;
        }

        public void SetAnchor(final idVec3 worldAnchor1, final idVec3 worldAnchor2) {
            // get anchor relative to center of mass of body1
            anchor1 = (worldAnchor1.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
            if (body2 != null) {
                // get anchor relative to center of mass of body2
                anchor2 = (worldAnchor2.oMinus(body2.GetWorldOrigin())).oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                anchor2 = worldAnchor2;
            }
        }

        public void SetSpring(final float stretch, final float compress, final float damping, final float restLength) {
            assert (stretch >= 0.0f && compress >= 0.0f && restLength >= 0.0f);
            this.kstretch = stretch;
            this.kcompress = compress;
            this.damping = damping;
            this.restLength = restLength;
        }

        public void SetLimit(final float minLength, final float maxLength) {
            assert (minLength >= 0.0f && maxLength >= 0.0f && maxLength >= minLength);
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        @Override
        public void DebugDraw() {
            idAFBody master;
            float length;
            idVec3 a1, a2, dir, mid, p;

            master = body2 != null ? body2 : physics.GetMasterBody();
            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(anchor2.oMultiply(master.GetWorldAxis()));
            } else {
                a2 = anchor2;
            }
            dir = a2.oMinus(a1);
            mid = a1.oPlus(dir.oMultiply(0.5f));
            length = dir.Normalize();

            // draw spring
            gameRenderWorld.DebugLine(colorGreen, a1, a2);

            // draw rest length
            p = dir.oMultiply(restLength * 0.5f);
            gameRenderWorld.DebugCircle(colorWhite, mid.oPlus(p), dir, 1.0f, 10);
            gameRenderWorld.DebugCircle(colorWhite, mid.oMinus(p), dir, 1.0f, 10);
            if (restLength > length) {
                gameRenderWorld.DebugLine(colorWhite, a2, mid.oPlus(p));
                gameRenderWorld.DebugLine(colorWhite, a1, mid.oMinus(p));
            }

            if (minLength > 0.0f) {
                // draw min length
                gameRenderWorld.DebugCircle(colorBlue, mid.oPlus(dir.oMultiply(minLength * 0.5f)), dir, 2.0f, 10);
                gameRenderWorld.DebugCircle(colorBlue, mid.oMinus(dir.oMultiply(minLength * 0.5f)), dir, 2.0f, 10);
            }

            if (maxLength > 0.0f) {
                // draw max length
                gameRenderWorld.DebugCircle(colorRed, mid.oPlus(dir.oMultiply(maxLength * 0.5f)), dir, 2.0f, 10);
                gameRenderWorld.DebugCircle(colorRed, mid.oMinus(dir.oMultiply(maxLength * 0.5f)), dir, 2.0f, 10);
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                anchor2.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                anchor2.oMulSet(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            idAFBody master;
            idVec3 a1, a2;

            master = body2 != null ? body2 : physics.GetMasterBody();
            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(anchor2.oMultiply(master.GetWorldAxis()));
            } else {
                a2 = anchor2;
            }
            center.oSet((a1.oPlus(a2)).oMultiply(0.5f));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(anchor1);
            saveFile.WriteVec3(anchor2);
            saveFile.WriteFloat(kstretch);
            saveFile.WriteFloat(kcompress);
            saveFile.WriteFloat(damping);
            saveFile.WriteFloat(restLength);
            saveFile.WriteFloat(minLength);
            saveFile.WriteFloat(maxLength);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[] kstretch = {0};
            float[] kcompress = {0};
            float[] damping = {0};
            float[] restLength = {0};
            float[] minLength = {0};
            float[] maxLength = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(anchor1);
            saveFile.ReadVec3(anchor2);
            saveFile.ReadFloat(kstretch);
            saveFile.ReadFloat(kcompress);
            saveFile.ReadFloat(damping);
            saveFile.ReadFloat(restLength);
            saveFile.ReadFloat(minLength);
            saveFile.ReadFloat(maxLength);

            this.kstretch = kstretch[0];
            this.kcompress = kcompress[0];
            this.damping = damping[0];
            this.restLength = restLength[0];
            this.minLength = minLength[0];
            this.maxLength = maxLength[0];
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2, velocity1, velocity2 = new idVec3(), force;
            idVec6 v1 = new idVec6(), v2 = new idVec6();
            float d, dampingForce, length, error;
            boolean limit;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            a1 = body1.GetWorldOrigin().oPlus(anchor1.oMultiply(body1.GetWorldAxis()));
            velocity1 = body1.GetPointVelocity(a1);

            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(anchor2.oMultiply(master.GetWorldAxis()));
                velocity2 = master.GetPointVelocity(a2);
            } else {
                a2 = anchor2;
                velocity2.Zero();
            }

            force = a2.oMinus(a1);
            d = force.oMultiply(force);
            if (d != 0.0f) {
                dampingForce = damping * idMath.Fabs((velocity2.oMinus(velocity1)).oMultiply(force)) / d;
            } else {
                dampingForce = 0.0f;
            }
            length = force.Normalize();

            if (length > restLength) {
                if (kstretch > 0.0f) {
                    idVec3 springForce = force.oMultiply((float) Square(length - restLength) * kstretch - dampingForce);
                    body1.AddForce(a1, springForce);
                    if (master != null) {
                        master.AddForce(a2, springForce.oNegative());
                    }
                }
            } else {
                if (kcompress > 0.0f) {
                    idVec3 springForce = force.oMultiply(-(float) (Square(restLength - length) * kcompress - dampingForce));
                    body1.AddForce(a1, springForce);
                    if (master != null) {
                        master.AddForce(a2, springForce.oNegative());
                    }
                }
            }

            // check for spring limits
            if (length < minLength) {
                force = force.oNegative();
                error = minLength - length;
                limit = true;
            } else if (maxLength > 0.0f && length > maxLength) {
                error = length - maxLength;
                limit = true;
            } else {
                error = 0.0f;
                limit = false;
            }

            if (limit) {
                a1.oMinSet(body1.GetWorldOrigin());
                v1.SubVec3_oSet(0, force);
                v1.SubVec3_oSet(1, a1.Cross(force));
                J1.Set(1, 6, v1.ToFloatPtr());
                if (body2 != null) {
                    a2.oMinSet(body2.GetWorldOrigin());
                    v2.SubVec3_oSet(0, force.oNegative());
                    v2.SubVec3_oSet(1, a2.Cross(force.oNegative()));
                    J2.Set(1, 6, v2.ToFloatPtr());
                }
                c1.p[0] = -(invTimeStep * ERROR_REDUCTION) * error;
                lo.p[0] = 0.0f;
            } else {
                J1.Zero(0, 0);
                J2.Zero(0, 0);
            }

            c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Contact
    //
    //===============================================================
    // constrains body1 to either be in contact with or move away from body2
    public static class idAFConstraint_Contact extends idAFConstraint {

        public idAFConstraint_Contact() {
            name.oSet("contact");
            type = CONSTRAINT_CONTACT;
            InitSize(1);
            fc = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }
        // ~idAFConstraint_Contact();

        public void Setup(idAFBody b1, idAFBody b2, contactInfo_t c) {
            idVec3 p;
            idVec6 v = new idVec6();
            float vel;
            float minBounceVelocity = 2.0f;

            assert (b1 != null);

            body1 = b1;
            body2 = b2;
            contact = c;

            p = c.point.oMinus(body1.GetWorldOrigin());
            v.SubVec3_oSet(0, c.normal);
            v.SubVec3_oSet(1, p.Cross(c.normal));
            J1.Set(1, 6, v.ToFloatPtr());
            vel = v.SubVec3(0).oMultiply(body1.GetLinearVelocity()) + v.SubVec3(1).oMultiply(body1.GetAngularVelocity());

            if (body2 != null) {
                p = c.point.oMinus(body2.GetWorldOrigin());
                v.SubVec3_oSet(0, c.normal.oNegative());
                v.SubVec3_oSet(1, p.Cross(c.normal.oNegative()));
                J2.Set(1, 6, v.ToFloatPtr());
                vel += v.SubVec3(0).oMultiply(body2.GetLinearVelocity()) + v.SubVec3(1).oMultiply(body2.GetAngularVelocity());
                c2.p[0] = 0.0f;
            }

            if (body1.GetBouncyness() > 0.0f && -vel > minBounceVelocity) {
                c1.p[0] = body1.GetBouncyness() * vel;
            } else {
                c1.p[0] = 0.0f;
            }

            e.p[0] = CONTACT_LCP_EPSILON;
            lo.p[0] = 0.0f;
            hi.p[0] = idMath.INFINITY;
            boxConstraint = null;
            boxIndex[0] = -1;
        }

        public contactInfo_t GetContact() {
            return contact;
        }

        @Override
        public void DebugDraw() {
            idVec3 x = new idVec3(), y = new idVec3();
            contact.normal.NormalVectors(x, y);
            gameRenderWorld.DebugLine(colorWhite, contact.point, contact.point.oPlus(contact.normal.oMultiply(6.0f)));
            gameRenderWorld.DebugLine(colorWhite, contact.point.oMinus(x.oMultiply(2.0f)), contact.point.oPlus(x.oMultiply(2.0f)));
            gameRenderWorld.DebugLine(colorWhite, contact.point.oMinus(y.oMultiply(2.0f)), contact.point.oPlus(y.oMultiply(2.0f)));
        }

        @Override
        public void Translate(final idVec3 translation) {
            assert (false);    // contact should never be translated
        }

        @Override
        public void Rotate(final idRotation rotation) {
            assert (false);    // contact should never be rotated
        }

        @Override
        public void GetCenter(idVec3 center) {
            center.oSet(contact.point);
        }

        //
//
        protected contactInfo_t                  contact;                    // contact information
        protected idAFConstraint_ContactFriction fc;                    // contact friction
//
//

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            idVec3 r, velocity, normal, dir1, dir2;
            float friction, magnitude, forceNumerator, forceDenominator;
            idVecX impulse = new idVecX(), dv = new idVecX();

            friction = body1.GetContactFriction();
            if (body2 != null && body2.GetContactFriction() < friction) {
                friction = body2.GetContactFriction();
            }

            friction *= physics.GetContactFrictionScale();

            if (friction <= 0.0f) {
                return;
            }

            // seperate friction per contact is silly but it's fast and often looks close enough
            if (af_useImpulseFriction.GetBool()) {

                impulse.SetData(6, VECX_ALLOCA(6));
                dv.SetData(6, VECX_ALLOCA(6));

                // calculate velocity in the contact plane
                r = contact.point.oMinus(body1.GetWorldOrigin());
                velocity = body1.GetLinearVelocity().oPlus(body1.GetAngularVelocity().Cross(r));
                velocity.oMinSet(contact.normal.oMultiply(velocity.oMultiply(contact.normal)));

                // get normalized direction of friction and magnitude of velocity
                normal = velocity.oNegative();
                magnitude = normal.Normalize();

                forceNumerator = friction * magnitude;
                forceDenominator = body1.GetInverseMass() + ((body1.GetInverseWorldInertia().oMultiply(r.Cross(normal)).Cross(r)).oMultiply(normal));
                impulse.SubVec3(0).oSet(normal.oMultiply(forceNumerator / forceDenominator));
                impulse.SubVec3(1).oSet(r.Cross(impulse.SubVec3(0)));
                body1.InverseWorldSpatialInertiaMultiply(dv, impulse.ToFloatPtr());

                // modify velocity with friction force
                body1.SetLinearVelocity(body1.GetLinearVelocity().oPlus(dv.SubVec3(0)));
                body1.SetAngularVelocity(body1.GetAngularVelocity().oPlus(dv.SubVec3(1)));
            } else {

                if (null == fc) {
                    fc = new idAFConstraint_ContactFriction();
                }
                // call setup each frame because contact constraints are re-used for different bodies
                fc.Setup(this);
                fc.Add(physics, invTimeStep);
            }
        }
    };

    //===============================================================
    //
    //	idAFConstraint_ContactFriction
    //
    //===============================================================
    // contact friction
    public static class idAFConstraint_ContactFriction extends idAFConstraint {

        public idAFConstraint_ContactFriction() {
            type = CONSTRAINT_FRICTION;
            name.oSet("contactFriction");
            InitSize(2);
            cc = null;
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_Contact cc) {
            this.cc = cc;
            body1 = cc.GetBody1();
            body2 = cc.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            idVec3 r, dir1 = new idVec3(), dir2 = new idVec3();
            float friction;
            int newRow;

            physics = phys;

            friction = body1.GetContactFriction() * physics.GetContactFrictionScale();

            // if the body only has friction in one direction
            if (body1.GetFrictionDirection(dir1)) {
                // project the friction direction into the contact plane
                dir1.oMinSet(dir1.oMultiply(cc.GetContact().normal.oMultiply(dir1)));
                dir1.Normalize();

                r = cc.GetContact().point.oMinus(body1.GetWorldOrigin());

                J1.SetSize(1, 6);
                J1.SubVec63_oSet(0, 0, dir1);
                J1.SubVec63_oSet(0, 1, r.Cross(dir1));
                c1.SetSize(1);
                c1.p[0] = 0.0f;

                if (body2 != null) {
                    r = cc.GetContact().point.oMinus(body2.GetWorldOrigin());

                    J2.SetSize(1, 6);
                    J2.SubVec63_oSet(0, 0, dir1.oNegative());
                    J2.SubVec63_oSet(0, 1, r.Cross(dir1.oNegative()));
                    c2.SetSize(1);
                    c2.p[0] = 0.0f;
                }

                lo.p[0] = -friction;
                hi.p[0] = friction;
                boxConstraint = cc;
                boxIndex[0] = 0;
            } else {
                // get two friction directions orthogonal to contact normal
                cc.GetContact().normal.NormalVectors(dir1, dir2);

                r = cc.GetContact().point.oMinus(body1.GetWorldOrigin());

                J1.SetSize(2, 6);
                J1.SubVec63_oSet(0, 0, dir1);
                J1.SubVec63_oSet(0, 1, r.Cross(dir1));
                J1.SubVec63_oSet(1, 0, dir2);
                J1.SubVec63_oSet(1, 1, r.Cross(dir2));
                c1.SetSize(2);
                c1.p[0] = c1.p[1] = 0.0f;

                if (body2 != null) {
                    r = cc.GetContact().point.oMinus(body2.GetWorldOrigin());

                    J2.SetSize(2, 6);
                    J2.SubVec63_oSet(0, 0, dir1.oNegative());
                    J2.SubVec63_oSet(0, 1, r.Cross(dir1.oNegative()));
                    J2.SubVec63_oSet(1, 0, dir2.oNegative());
                    J2.SubVec63_oSet(1, 1, r.Cross(dir2.oNegative()));
                    c2.SetSize(2);
                    c2.p[0] = c2.p[1] = 0.0f;

                    if (body2.GetContactFriction() < friction) {
                        friction = body2.GetContactFriction();
                    }
                }

                lo.p[0] = -friction;
                hi.p[0] = friction;
                boxConstraint = cc;
                boxIndex[0] = 0;
                lo.p[1] = -friction;
                hi.p[1] = friction;
                boxIndex[1] = 0;
            }

            if (body1.GetContactMotorDirection(dir1) && body1.GetContactMotorForce() > 0.0f) {
                // project the motor force direction into the contact plane
                dir1.oMinSet(dir1.oMultiply(cc.GetContact().normal.oMultiply(dir1)));
                dir1.Normalize();

                r = cc.GetContact().point.oMinus(body1.GetWorldOrigin());

                newRow = J1.GetNumRows();
                J1.ChangeSize(newRow + 1, J1.GetNumColumns());
                J1.SubVec63_oSet(newRow, 0, dir1.oNegative());
                J1.SubVec63_oSet(newRow, 1, r.Cross(dir1.oNegative()));
                c1.ChangeSize(newRow + 1);
                c1.p[newRow] = body1.GetContactMotorVelocity();

                if (body2 != null) {
                    r = cc.GetContact().point.oMinus(body2.GetWorldOrigin());

                    J2.ChangeSize(newRow + 1, J2.GetNumColumns());
                    J2.SubVec63_oSet(newRow, 0, dir1.oNegative());
                    J2.SubVec63_oSet(newRow, 1, r.Cross(dir1.oNegative()));
                    c2.ChangeSize(newRow + 1);
                    c2.p[newRow] = 0.0f;
                }

                lo.p[newRow] = -body1.GetContactMotorForce();
                hi.p[newRow] = body1.GetContactMotorForce();
                boxIndex[newRow] = -1;
            }

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void DebugDraw() {
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }
//
//
        idAFConstraint_Contact cc;							// contact constraint
//
//

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFConstraint_ConeLimit
    //
    //===============================================================
    // constrains an axis attached to body1 to be inside a cone relative to body2
    public static class idAFConstraint_ConeLimit extends idAFConstraint {

        protected idVec3 coneAnchor;             // top of the cone in body2 space
        protected idVec3 coneAxis;               // cone axis in body2 space
        protected idVec3 body1Axis;              // axis in body1 space that should stay within the cone
        protected float  cosAngle;               // cos( coneAngle / 2 )
        protected float  sinHalfAngle;           // sin( coneAngle / 4 )
        protected float  cosHalfAngle;           // cos( coneAngle / 4 )
        protected float  epsilon;                // lcp epsilon
        //
        //

        public idAFConstraint_ConeLimit() {
            coneAnchor = new idVec3();
            coneAxis = new idVec3();
            body1Axis = new idVec3();
            type = CONSTRAINT_CONELIMIT;
            name.oSet("coneLimit");
            InitSize(1);
            fl.allowPrimary = false;
            fl.frameConstraint = true;
        }


        /*
         ================
         idAFConstraint_ConeLimit::Setup

         the coneAnchor is the top of the cone in body2 space
         the coneAxis is the axis of the cone in body2 space
         the coneAngle is the angle the cone hull makes at the top
         the body1Axis is the axis in body1 space that should stay within the cone
         ================
         */
        public void Setup(idAFBody b1, idAFBody b2, final idVec3 coneAnchor, final idVec3 coneAxis,
                final float coneAngle, final idVec3 body1Axis) {
            this.body1 = b1;
            this.body2 = b2;
            this.coneAxis.oSet(coneAxis);
            this.coneAxis.Normalize();
            this.coneAnchor.oSet(coneAnchor);
            this.body1Axis.oSet(body1Axis);
            this.body1Axis.Normalize();
            this.cosAngle = (float) cos(DEG2RAD(coneAngle * 0.5f));
            this.sinHalfAngle = (float) sin(DEG2RAD(coneAngle * 0.25f));
            this.cosHalfAngle = (float) cos(DEG2RAD(coneAngle * 0.25f));
        }

        public void SetAnchor(final idVec3 coneAnchor) {
            this.coneAnchor.oSet(coneAnchor);
        }

        public void SetBody1Axis(final idVec3 body1Axis) {
            this.body1Axis.oSet(body1Axis);
        }

        public void SetEpsilon(final float e) {
            epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float a;
            idVec6 J1row = new idVec6(), J2row = new idVec6();
            idVec3 ax, anchor, body1ax, normal, coneVector, p1, p2;
            idQuat q = new idQuat();
            idAFBody master;

            if (af_skipLimits.GetBool()) {
                lm.Zero();    // constraint exerts no force
                return false;
            }

            physics = phys;

            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                ax = coneAxis.oMultiply(master.GetWorldAxis());
                anchor = master.GetWorldOrigin().oPlus(coneAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                ax = coneAxis;
                anchor = coneAnchor;
            }

            body1ax = body1Axis.oMultiply(body1.GetWorldAxis());

            a = ax.oMultiply(body1ax);

            // if the body1 axis is inside the cone
            if (a > cosAngle) {
                lm.Zero();    // constraint exerts no force
                return false;
            }

            // calculate the inward cone normal for the position the body1 axis went outside the cone
            normal = body1ax.Cross(ax);
            normal.Normalize();
            q.x = normal.x * sinHalfAngle;
            q.y = normal.y * sinHalfAngle;
            q.z = normal.z * sinHalfAngle;
            q.w = cosHalfAngle;
            coneVector = ax.oMultiply(q.ToMat3());
            normal = coneVector.Cross(ax).Cross(coneVector);
            normal.Normalize();

            p1 = anchor.oPlus(coneVector.oMultiply(32.0f).oMinus(body1.GetWorldOrigin()));

            J1row.SubVec3_oSet(0, normal);
            J1row.SubVec3_oSet(1, p1.Cross(normal));
            J1.Set(1, 6, J1row.ToFloatPtr());

            c1.p[0] = (invTimeStep * LIMIT_ERROR_REDUCTION) * (normal.oMultiply(body1ax.oMultiply(32.0f)));

            if (body2 != null) {

                p2 = anchor.oPlus(coneVector.oMultiply(32.0f).oMinus(master.GetWorldOrigin()));

                J2row.SubVec3_oSet(0, normal.oNegative());
                J2row.SubVec3_oSet(1, p2.Cross(normal.oNegative()));
                J2.Set(1, 6, J2row.ToFloatPtr());

                c2.p[0] = 0.0f;
            }

            lo.p[0] = 0.0f;
            e.p[0] = LIMIT_LCP_EPSILON;

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void DebugDraw() {
            idVec3 ax, anchor, x = new idVec3(), y = new idVec3(), z, start, end;
            float sinAngle, a, size = 10.0f;
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                ax = coneAxis.oMultiply(master.GetWorldAxis());
                anchor = master.GetWorldOrigin().oPlus(coneAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                ax = coneAxis;
                anchor = coneAnchor;
            }

            // draw body1 axis
            gameRenderWorld.DebugLine(colorGreen, anchor, anchor.oPlus((body1Axis.oMultiply(body1.GetWorldAxis())).oMultiply(size)));

            // draw cone
            ax.NormalVectors(x, y);
            sinAngle = idMath.Sqrt(1.0f - cosAngle * cosAngle);
            x.oMulSet(size * sinAngle);
            y.oMulSet(size * sinAngle);
            z = anchor.oPlus(ax.oMultiply(size * cosAngle));
            start = x.oPlus(z);
            for (a = 0.0f; a < 360.0f; a += 45.0f) {
                end = x.oMultiply((float) cos(DEG2RAD(a + 45.0f))).oPlus(y.oMultiply((float) sin(DEG2RAD(a + 45.0f))).oPlus(z));
                gameRenderWorld.DebugLine(colorMagenta, anchor, start);
                gameRenderWorld.DebugLine(colorMagenta, start, end);
                start = end;
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                coneAnchor.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                coneAnchor.oMulSet(rotation);
                coneAxis.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(coneAnchor);
            saveFile.WriteVec3(coneAxis);
            saveFile.WriteVec3(body1Axis);
            saveFile.WriteFloat(cosAngle);
            saveFile.WriteFloat(sinHalfAngle);
            saveFile.WriteFloat(cosHalfAngle);
            saveFile.WriteFloat(epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[] cosAngle = {0};
            float[] sinHalfAngle = {0};
            float[] cosHalfAngle = {0};
            float[] epsilon = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(coneAnchor);
            saveFile.ReadVec3(coneAxis);
            saveFile.ReadVec3(body1Axis);
            saveFile.ReadFloat(cosAngle);
            saveFile.ReadFloat(sinHalfAngle);
            saveFile.ReadFloat(cosHalfAngle);
            saveFile.ReadFloat(epsilon);

            this.cosAngle = cosAngle[0];
            this.sinHalfAngle = sinHalfAngle[0];
            this.cosHalfAngle = cosHalfAngle[0];
            this.epsilon = epsilon[0];
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
        }
    };

    //===============================================================
    //
    //	idAFConstraint_PyramidLimit
    //
    //===============================================================
    // constrains an axis attached to body1 to be inside a pyramid relative to body2
    public static class idAFConstraint_PyramidLimit extends idAFConstraint {

        protected idVec3 pyramidAnchor;                 // top of the pyramid in body2 space
        protected idMat3 pyramidBasis;                  // pyramid basis in body2 space with base[2] being the pyramid axis
        protected idVec3 body1Axis;                     // axis in body1 space that should stay within the cone
        protected float[] cosAngle     = new float[2];  // cos( pyramidAngle / 2 )
        protected float[] sinHalfAngle = new float[2];  // sin( pyramidAngle / 4 )
        protected float[] cosHalfAngle = new float[2];  // cos( pyramidAngle / 4 )
        protected float epsilon;                        // lcp epsilon
        //
        //

        public idAFConstraint_PyramidLimit() {
            type = CONSTRAINT_PYRAMIDLIMIT;
            name.oSet("pyramidLimit");
            InitSize(1);
            fl.allowPrimary = false;
            fl.frameConstraint = true;

            pyramidAnchor = new idVec3();
            pyramidBasis = new idMat3();
            body1Axis = new idVec3();
        }

        public void Setup(idAFBody b1, idAFBody b2, final idVec3 pyramidAnchor, final idVec3 pyramidAxis,
                          final idVec3 baseAxis, final float pyramidAngle1, final float pyramidAngle2, final idVec3 body1Axis) {
            body1 = b1;
            body2 = b2;
            // setup the base and make sure the basis is orthonormal
            pyramidBasis.oSet(2, pyramidAxis);
            pyramidBasis.oGet(2).Normalize();
            pyramidBasis.oSet(0, baseAxis);
            pyramidBasis.oGet(0).oMinSet(pyramidBasis.oGet(2).oMultiply(baseAxis.oMultiply(pyramidBasis.oGet(2))));
            pyramidBasis.oGet(0).Normalize();
            pyramidBasis.oSet(0, pyramidBasis.oGet(0).Cross(pyramidBasis.oGet(2)));
            // pyramid top
            pyramidAnchor.oSet(pyramidAnchor);
            // angles
            cosAngle[0] = (float) cos(DEG2RAD(pyramidAngle1 * 0.5f));
            cosAngle[1] = (float) cos(DEG2RAD(pyramidAngle2 * 0.5f));
            sinHalfAngle[0] = (float) sin(DEG2RAD(pyramidAngle1 * 0.25f));
            sinHalfAngle[1] = (float) sin(DEG2RAD(pyramidAngle2 * 0.25f));
            cosHalfAngle[0] = (float) cos(DEG2RAD(pyramidAngle1 * 0.25f));
            cosHalfAngle[1] = (float) cos(DEG2RAD(pyramidAngle2 * 0.25f));

            body1Axis.oSet(body1Axis);
        }

        public void SetAnchor(final idVec3 pyramidAxis) {
            this.pyramidAnchor.oSet(pyramidAnchor);
        }

        public void SetBody1Axis(final idVec3 body1Axis) {
            this.body1Axis.oSet(body1Axis);
        }

        public void SetEpsilon(final float e) {
            epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            int i;
            float[] a = new float[2];
            idVec6 J1row = new idVec6(), J2row = new idVec6();
            idMat3 worldBase = new idMat3();
            idVec3 anchor, body1ax, v, normal, pyramidVector, p1, p2;
            idVec3[] ax = new idVec3[2];
            idQuat q = new idQuat();
            idAFBody master;

            if (af_skipLimits.GetBool()) {
                lm.Zero();    // constraint exerts no force
                return false;
            }

            physics = phys;
            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                worldBase.oSet(0, pyramidBasis.oGet(0).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(1, pyramidBasis.oGet(1).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(2, pyramidBasis.oGet(2).oMultiply(master.GetWorldAxis()));
                anchor = master.GetWorldOrigin().oPlus(pyramidAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                worldBase = pyramidBasis;
                anchor = pyramidAnchor;
            }

            body1ax = body1Axis.oMultiply(body1.GetWorldAxis());

            for (i = 0; i < 2; i++) {
                final int he = (i == 0 ? 1 : 0);
                ax[i] = body1ax.oMinus(worldBase.oGet(he).oMultiply(body1ax.oMultiply(worldBase.oGet(he))));
                ax[i].Normalize();
                a[i] = worldBase.oGet(2).oMultiply(ax[i]);
            }

            // if the body1 axis is inside the pyramid
            if (a[0] > cosAngle[0] && a[1] > cosAngle[1]) {
                lm.Zero();    // constraint exerts no force
                return false;
            }

            // calculate the inward pyramid normal for the position the body1 axis went outside the pyramid
            pyramidVector = worldBase.oGet(2);
            for (i = 0; i < 2; i++) {
                if (a[i] <= cosAngle[i]) {
                    v = ax[i].Cross(worldBase.oGet(2));
                    v.Normalize();
                    q.x = v.x * sinHalfAngle[i];
                    q.y = v.y * sinHalfAngle[i];
                    q.z = v.z * sinHalfAngle[i];
                    q.w = cosHalfAngle[i];
                    pyramidVector.oMulSet(q.ToMat3());
                }
            }
            normal = pyramidVector.Cross(worldBase.oGet(2)).Cross(pyramidVector);
            normal.Normalize();

            p1 = anchor.oPlus(pyramidVector.oMultiply(32.0f).oMinus(body1.GetWorldOrigin()));

            J1row.SubVec3_oSet(0, normal);
            J1row.SubVec3_oSet(1, p1.Cross(normal));
            J1.Set(1, 6, J1row.ToFloatPtr());

            c1.p[0] = (invTimeStep * LIMIT_ERROR_REDUCTION) * (normal.oMultiply(body1ax.oMultiply(32.0f)));

            if (body2 != null) {

                p2 = anchor.oPlus(pyramidVector.oMultiply(32.0f).oMinus(master.GetWorldOrigin()));

                J2row.SubVec3_oSet(0, normal.oNegative());
                J2row.SubVec3_oSet(1, p2.Cross(normal.oNegative()));
                J2.Set(1, 6, J2row.ToFloatPtr());

                c2.p[0] = 0.0f;
            }

            lo.p[0] = 0.0f;
            e.p[0] = LIMIT_LCP_EPSILON;

            physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void DebugDraw() {
            int i;
            float size = 10.0f;
            idVec3 anchor, dir;
            idVec3[] p = new idVec3[4];
            idMat3 worldBase = new idMat3();
            idMat3[] m = new idMat3[2];
            idQuat q = new idQuat();
            idAFBody master;

            master = body2 != null ? body2 : physics.GetMasterBody();

            if (master != null) {
                worldBase.oSet(0, pyramidBasis.oGet(0).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(1, pyramidBasis.oGet(1).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(2, pyramidBasis.oGet(2).oMultiply(master.GetWorldAxis()));
                anchor = master.GetWorldOrigin().oPlus(pyramidAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                worldBase = pyramidBasis;
                anchor = pyramidAnchor;
            }

            // draw body1 axis
            gameRenderWorld.DebugLine(colorGreen, anchor, anchor.oPlus((body1Axis.oMultiply(body1.GetWorldAxis())).oMultiply(size)));

            // draw the pyramid
            for (i = 0; i < 2; i++) {
                final int him = (i == 0 ? 1 : 0);
                q.x = worldBase.oGet(him).x * sinHalfAngle[i];
                q.y = worldBase.oGet(him).y * sinHalfAngle[i];
                q.z = worldBase.oGet(him).z * sinHalfAngle[i];
                q.w = cosHalfAngle[i];
                m[i] = q.ToMat3();
            }

            dir = worldBase.oGet(2).oMultiply(size);
            p[0] = anchor.oPlus(m[0].oMultiply(m[1].oMultiply(dir)));
            p[1] = anchor.oPlus(m[0].oMultiply(m[1].Transpose().oMultiply(dir)));
            p[2] = anchor.oPlus(m[0].Transpose().oMultiply(m[1].Transpose().oMultiply(dir)));
            p[3] = anchor.oPlus(m[0].Transpose().oMultiply(m[1].oMultiply(dir)));

            for (i = 0; i < 4; i++) {
                gameRenderWorld.DebugLine(colorMagenta, anchor, p[i]);
                gameRenderWorld.DebugLine(colorMagenta, p[i], p[(i + 1) & 3]);
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == body2) {
                pyramidAnchor.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == body2) {
                pyramidAnchor.oMulSet(rotation);
                pyramidBasis.oGet(0).oMulSet(rotation.ToMat3());
                pyramidBasis.oGet(1).oMulSet(rotation.ToMat3());
                pyramidBasis.oGet(2).oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(pyramidAnchor);
            saveFile.WriteMat3(pyramidBasis);
            saveFile.WriteVec3(body1Axis);
            saveFile.WriteFloat(cosAngle[0]);
            saveFile.WriteFloat(cosAngle[1]);
            saveFile.WriteFloat(sinHalfAngle[0]);
            saveFile.WriteFloat(sinHalfAngle[1]);
            saveFile.WriteFloat(cosHalfAngle[0]);
            saveFile.WriteFloat(cosHalfAngle[1]);
            saveFile.WriteFloat(epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            float[][] cosAngle = {{0}, {0}};
            float[][] sinHalfAngle = {{0}, {0}};
            float[][] cosHalfAngle = {{0}, {0}};
            float[] epsilon = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(pyramidAnchor);
            saveFile.ReadMat3(pyramidBasis);
            saveFile.ReadVec3(body1Axis);
            saveFile.ReadFloat(cosAngle[0]);
            saveFile.ReadFloat(cosAngle[1]);
            saveFile.ReadFloat(sinHalfAngle[0]);
            saveFile.ReadFloat(sinHalfAngle[1]);
            saveFile.ReadFloat(cosHalfAngle[0]);
            saveFile.ReadFloat(cosHalfAngle[1]);
            saveFile.ReadFloat(epsilon);

            this.cosAngle[0] = cosAngle[0][0];
            this.cosAngle[1] = cosAngle[1][0];
            this.sinHalfAngle[0] = sinHalfAngle[0][0];
            this.sinHalfAngle[1] = sinHalfAngle[1][0];
            this.cosHalfAngle[0] = cosHalfAngle[0][0];
            this.cosHalfAngle[1] = cosHalfAngle[1][0];
            this.epsilon = epsilon[0];
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            // do nothing
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
        }
    };

    //===============================================================
    //
    //	idAFConstraint_Suspension
    //
    //===============================================================
    // vehicle suspension
    public static class idAFConstraint_Suspension extends idAFConstraint {

        protected idVec3      localOrigin;         // position of suspension relative to body1
        protected idMat3      localAxis;           // orientation of suspension relative to body1
        protected float       suspensionUp;        // suspension up movement
        protected float       suspensionDown;      // suspension down movement
        protected float       suspensionKCompress; // spring compress constant
        protected float       suspensionDamping;   // spring damping
        protected float       steerAngle;          // desired steer angle in degrees
        protected float       friction;            // friction
        protected boolean     motorEnabled;        // whether the motor is enabled or not
        protected float       motorForce;          // motor force
        protected float       motorVelocity;       // desired velocity
        protected idClipModel wheelModel;          // wheel model
        protected idVec3      wheelOffset;         // wheel position relative to body1
        protected trace_s     trace;               // contact point with the ground
        protected float       epsilon;             // lcp epsilon
        //
        //

        public idAFConstraint_Suspension() {
            type = CONSTRAINT_SUSPENSION;
            name.oSet("suspension");
            InitSize(3);
            fl.allowPrimary = false;
            fl.frameConstraint = true;

            localOrigin.Zero();
            localAxis.Identity();
            suspensionUp = 0.0f;
            suspensionDown = 0.0f;
            suspensionKCompress = 0.0f;
            suspensionDamping = 0.0f;
            steerAngle = 0.0f;
            friction = 2.0f;
            motorEnabled = false;
            motorForce = 0.0f;
            motorVelocity = 0.0f;
            wheelModel = null;
            trace = new trace_s();//	memset( &trace, 0, sizeof( trace ) );
            epsilon = LCP_EPSILON;
        }

        public void Setup(final String name, idAFBody body, final idVec3 origin, final idMat3 axis, idClipModel clipModel) {
            this.name.oSet(name);
            body1 = body;
            body2 = null;
            localOrigin = (origin.oMinus(body.GetWorldOrigin())).oMultiply(body.GetWorldAxis().Transpose());
            localAxis = axis.oMultiply(body.GetWorldAxis().Transpose());
            wheelModel = clipModel;
        }

        public void SetSuspension(final float up, final float down, final float k, final float d, final float f) {
            suspensionUp = up;
            suspensionDown = down;
            suspensionKCompress = k;
            suspensionDamping = d;
            friction = f;
        }

        public void SetSteerAngle(final float degrees) {
            steerAngle = degrees;
        }

        public void EnableMotor(final boolean enable) {
            motorEnabled = enable;
        }

        public void SetMotorForce(final float force) {
            motorForce = force;
        }

        public void SetMotorVelocity(final float vel) {
            motorVelocity = vel;
        }

        public void SetEpsilon(final float e) {
            epsilon = e;
        }

        public idVec3 GetWheelOrigin() {
            return body1.GetWorldOrigin().oPlus(wheelOffset.oMultiply(body1.GetWorldAxis()));
        }

        @Override
        public void DebugDraw() {
            idVec3 origin;
            idMat3 axis;
            idRotation rotation = new idRotation();

            axis = localAxis.oMultiply(body1.GetWorldAxis());

            rotation.SetVec(axis.oGet(2));
            rotation.SetAngle(steerAngle);

            axis.oMulSet(rotation.ToMat3());

            if (trace.fraction < 1.0f) {
                origin = trace.c.point;

                gameRenderWorld.DebugLine(colorWhite, origin, origin.oPlus(axis.oGet(2).oMultiply(6.0f)));
                gameRenderWorld.DebugLine(colorWhite, origin.oMinus(axis.oGet(0).oMultiply(4.0f)), origin.oPlus(axis.oGet(0).oMultiply(4.0f)));
                gameRenderWorld.DebugLine(colorWhite, origin.oMinus(axis.oGet(1).oMultiply(2.0f)), origin.oPlus(axis.oGet(1).oMultiply(2.0f)));
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            float velocity, suspensionLength, springLength, compression, dampingForce, springForce;
            idVec3 origin, start, end, vel1, vel2 = new idVec3(), springDir, r, frictionDir, motorDir;
            idMat3 axis;
            idRotation rotation = new idRotation();

            axis = localAxis.oMultiply(body1.GetWorldAxis());
            origin = body1.GetWorldOrigin().oPlus(localOrigin.oMultiply(body1.GetWorldAxis()));
            start = origin.oPlus(axis.oGet(2).oMultiply(suspensionUp));
            end = origin.oMinus(axis.oGet(2).oMultiply(suspensionDown));

            rotation.SetVec(axis.oGet(2));
            rotation.SetAngle(steerAngle);

            axis.oMulSet(rotation.ToMat3());

            {
                trace_s[] tracy = {trace};
                gameLocal.clip.Translation(tracy, start, end, wheelModel, axis, MASK_SOLID, null);
                this.trace = tracy[0];
            }

            wheelOffset = (trace.endpos.oMinus(body1.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());

            if (trace.fraction >= 1.0f) {
                J1.SetSize(0, 6);
                if (body2 != null) {
                    J2.SetSize(0, 6);
                }
                return;
            }

            // calculate and add spring force
            vel1 = body1.GetPointVelocity(start);
            if (body2 != null) {
                vel2 = body2.GetPointVelocity(trace.c.point);
            } else {
                vel2.Zero();
            }

            suspensionLength = suspensionUp + suspensionDown;
            springDir = trace.endpos.oMinus(start);
            springLength = trace.fraction * suspensionLength;
            dampingForce = suspensionDamping * idMath.Fabs((vel2.oMinus(vel1)).oMultiply(springDir)) / (1.0f + springLength * springLength);
            compression = suspensionLength - springLength;
            springForce = compression * compression * suspensionKCompress - dampingForce;

            r = trace.c.point.oMinus(body1.GetWorldOrigin());
            J1.SetSize(2, 6);
            J1.SubVec63_oSet(0, 0, trace.c.normal);
            J1.SubVec63_oSet(0, 1, r.Cross(trace.c.normal));
            c1.SetSize(2);
            c1.p[0] = 0.0f;
            velocity = J1.SubVec6(0).SubVec3(0).oMultiply(body1.GetLinearVelocity()) + J1.SubVec6(0).SubVec3(1).oMultiply(body1.GetAngularVelocity());

            if (body2 != null) {
                r = trace.c.point.oMinus(body2.GetWorldOrigin());
                J2.SetSize(2, 6);
                J2.SubVec63_oSet(0, 0, trace.c.normal.oNegative());
                J2.SubVec63_oSet(0, 1, r.Cross(trace.c.normal.oNegative()));
                c2.SetSize(2);
                c2.p[0] = 0.0f;
                velocity += J2.SubVec6(0).SubVec3(0).oMultiply(body2.GetLinearVelocity()) + J2.SubVec6(0).SubVec3(1).oMultiply(body2.GetAngularVelocity());
            }

            c1.p[0] = -compression;        // + 0.5f * -velocity;

            e.p[0] = 1e-4f;
            lo.p[0] = 0.0f;
            hi.p[0] = springForce;
            boxConstraint = null;
            boxIndex[0] = -1;

            // project the friction direction into the contact plane
            frictionDir = axis.oGet(1).oMinus(axis.oGet(1).oMultiply(trace.c.normal.oMultiply(axis.oGet(1))));
            frictionDir.Normalize();

            r = trace.c.point.oMinus(body1.GetWorldOrigin());

            J1.SubVec63_oSet(1, 0, frictionDir);
            J1.SubVec63_oSet(1, 1, r.Cross(frictionDir));
            c1.p[1] = 0.0f;

            if (body2 != null) {
                r = trace.c.point.oMinus(body2.GetWorldOrigin());

                J2.SubVec63_oSet(1, 0, frictionDir.oNegative());
                J2.SubVec63_oSet(1, 1, r.Cross(frictionDir.oNegative()));
                c2.p[1] = 0.0f;
            }

            lo.p[1] = -friction * physics.GetContactFrictionScale();
            hi.p[1] = friction * physics.GetContactFrictionScale();

            boxConstraint = this;
            boxIndex[1] = 0;

            if (motorEnabled) {
                // project the motor force direction into the contact plane
                motorDir = axis.oGet(0).oMinus(axis.oGet(0).oMultiply(trace.c.normal.oMultiply(axis.oGet(0))));
                motorDir.Normalize();

                r = trace.c.point.oMinus(body1.GetWorldOrigin());

                J1.ChangeSize(3, J1.GetNumColumns());
                J1.SubVec63_oSet(2, 0, motorDir.oNegative());
                J1.SubVec63_oSet(2, 1, r.Cross(motorDir.oNegative()));
                c1.ChangeSize(3);
                c1.p[2] = motorVelocity;

                if (body2 != null) {
                    r = trace.c.point.oMinus(body2.GetWorldOrigin());

                    J2.ChangeSize(3, J2.GetNumColumns());
                    J2.SubVec63_oSet(2, 0, motorDir.oNegative());
                    J2.SubVec63_oSet(2, 1, r.Cross(motorDir.oNegative()));
                    c2.ChangeSize(3);
                    c2.p[2] = 0.0f;
                }

                lo.p[2] = -motorForce;
                hi.p[2] = motorForce;
                boxIndex[2] = -1;
            }
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    };

    //===============================================================
    //
    //	idAFBody
    //
    //===============================================================
    public static class AFBodyPState_s {

        idVec3 worldOrigin;              // position in world space
        idMat3 worldAxis;                // axis at worldOrigin
        idVec6 spatialVelocity;          // linear and rotational velocity of body
        idVec6 externalForce;            // external force and torque applied to body

        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;
    };

    public static class idAFBody {

        // properties
        private idStr                  name;                // name of body
        private idAFBody               parent;              // parent of this body
        private idList<idAFBody>       children;            // children of this body
        private idClipModel            clipModel;           // model used for collision detection
        private idAFConstraint         primaryConstraint;   // primary constraint (this.constraint.body1 = this)
        private idList<idAFConstraint> constraints;         // all constraints attached to this body
        private idAFTree               tree;                // tree structure this body is part of
        private float                  linearFriction;      // translational friction
        private float                  angularFriction;     // rotational friction
        private float                  contactFriction;     // friction with contact surfaces
        private float                  bouncyness;          // bounce
        private int                    clipMask;            // contents this body collides with
        private idVec3                 frictionDir;         // specifies a single direction of friction in body space
        private idVec3                 contactMotorDir;     // contact motor direction
        private float                  contactMotorVelocity;// contact motor velocity
        private float                  contactMotorForce;   // maximum force applied to reach the motor velocity
        //
        // derived properties
        private float                  mass;                // mass of body
        private float                  invMass;             // inverse mass
        private idVec3                 centerOfMass;        // center of mass of body
        private idMat3                 inertiaTensor;       // inertia tensor
        private idMat3                 inverseInertiaTensor;// inverse inertia tensor
        //
        // physics state
        private AFBodyPState_s[] state = new AFBodyPState_s[2];
        private AFBodyPState_s current;                     // current physics state
        private AFBodyPState_s next;                        // next physics state
        private AFBodyPState_s saved;                       // saved physics state
        private idVec3         atRestOrigin;                // origin at rest
        private idMat3         atRestAxis;                  // axis at rest
        //
        // simulation variables used during calculations
        private idMatX         inverseWorldSpatialInertia;  // inverse spatial inertia in world space
        private idMatX         I, invI;                     // transformed inertia
        private idMatX  J;                                  // transformed constraint matrix
        private idVecX  s;                                  // temp solution
        private idVecX  totalForce;                         // total force acting on body
        private idVecX  auxForce;                           // force from auxiliary constraints
        private idVecX  acceleration;                       // acceleration
        private float[] response;                           // forces on body in response to auxiliary constraint forces
        private int[]   responseIndex;                      // index to response forces
        private int     numResponses;                       // number of response forces
        private int     maxAuxiliaryIndex;                  // largest index of an auxiliary constraint constraining this body
        private int     maxSubTreeAuxiliaryIndex;           // largest index of an auxiliary constraint constraining this body or one of it's children
//
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        private final class bodyFlags_s {

            boolean clipMaskSet;//: 1;          // true if this body has a clip mask set
            boolean selfCollision;//: 1;	// true if this body can collide with other bodies of this AF
            boolean spatialInertiaSparse;//: 1;	// true if the spatial inertia matrix is sparse
            boolean useFrictionDir;//: 1;	// true if a single friction direction should be used
            boolean useContactMotorDir;//: 1;	// true if a contact motor should be used
            boolean isZero;//: 1;               // true if 's' is zero during calculations
        };
        //
        private bodyFlags_s fl;
        //
        //

        // friend class idPhysics_AF;
        // friend class idAFTree;
        public idAFBody() {
            Init();
        }

        public idAFBody(final idStr name, idClipModel clipModel, float density) {

            assert (clipModel != null);
            assert (clipModel.IsTraceModel());

            Init();

            this.name.oSet(name);
            this.clipModel = null;

            SetClipModel(clipModel);
            SetDensity(density);

            current.worldOrigin.oSet(clipModel.GetOrigin());
            current.worldAxis.oSet(clipModel.GetAxis());
            next = current;

        }

        // ~idAFBody();
        public void Init() {
            name = new idStr("noname");
            parent = null;
            children = new idList<>();
            clipModel = null;
            primaryConstraint = null;
            constraints = new idList<>();
            tree = null;

            linearFriction = -1.0f;
            angularFriction = -1.0f;
            contactFriction = -1.0f;
            bouncyness = -1.0f;
            clipMask = 0;

            frictionDir = getVec3_zero();
            contactMotorDir = getVec3_zero();
            contactMotorVelocity = 0.0f;
            contactMotorForce = 0.0f;

            mass = 1.0f;
            invMass = 1.0f;
            centerOfMass = getVec3_zero();
            inertiaTensor = getMat3_identity();
            inverseInertiaTensor = getMat3_identity();

            current = state[0] = new AFBodyPState_s();
            next = state[1] = new AFBodyPState_s();
            current.worldOrigin = getVec3_zero();
            current.worldAxis = getMat3_identity();
            current.spatialVelocity = getVec6_zero();
            current.externalForce = getVec6_zero();
            next = current;
            saved = current;
            atRestOrigin = getVec3_zero();
            atRestAxis = getMat3_identity();

            inverseWorldSpatialInertia = new idMatX();
            I = new idMatX();
            invI = new idMatX();
            J = new idMatX();
            s = new idVecX(6);
            totalForce = new idVecX(6);
            auxForce = new idVecX(6);
            acceleration = new idVecX(6);

            response = null;
            responseIndex = null;
            numResponses = 0;
            maxAuxiliaryIndex = 0;
            maxSubTreeAuxiliaryIndex = 0;

            fl = new bodyFlags_s();//	memset( &fl, 0, sizeof( fl ) );

            fl.selfCollision = true;
            fl.isZero = true;
        }

        public idStr GetName() {
            return name;
        }

        public idVec3 GetWorldOrigin() {
            return current.worldOrigin;
        }

        public idMat3 GetWorldAxis() {
            return current.worldAxis;
        }

        public idVec3 GetLinearVelocity() {
            return current.spatialVelocity.SubVec3(0);
        }

        public idVec3 GetAngularVelocity() {
            return current.spatialVelocity.SubVec3(1);
        }

        public idVec3 GetPointVelocity(final idVec3 point) {
            idVec3 r = point.oMinus(current.worldOrigin);
            return current.spatialVelocity.SubVec3(0).oPlus(current.spatialVelocity.SubVec3(1).Cross(r));
        }

        public idVec3 GetCenterOfMass() {
            return centerOfMass;
        }

        public void SetClipModel(idClipModel clipModel) {
//	if ( this.clipModel && this.clipModel != clipModel ) {
//		delete this.clipModel;
//	}
            //blessed be the garbage collector
            this.clipModel = clipModel;
        }

        public idClipModel GetClipModel() {
            return clipModel;
        }

        public void SetClipMask(final int mask) {
            clipMask = mask;
            fl.clipMaskSet = true;
        }

        public int GetClipMask() {
            return clipMask;
        }

        public void SetSelfCollision(final boolean enable) {
            fl.selfCollision = enable;
        }

        public void SetWorldOrigin(final idVec3 origin) {
            current.worldOrigin.oSet(origin);
        }

        public void SetWorldAxis(final idMat3 axis) {
            current.worldAxis.oSet(axis);
        }

        public void SetLinearVelocity(final idVec3 linear) {
            current.spatialVelocity.SubVec3_oSet(0, linear);
        }

        public void SetAngularVelocity(final idVec3 angular) {
            current.spatialVelocity.SubVec3_oSet(1, angular);
        }

        public void SetFriction(float linear, float angular, float contact) {
            if (linear < 0.0f || linear > 1.0f
                    || angular < 0.0f || angular > 1.0f
                    || contact < 0.0f) {
                gameLocal.Warning("idAFBody::SetFriction: friction out of range, linear = %.1f, angular = %.1f, contact = %.1f", linear, angular, contact);
                return;
            }
            linearFriction = linear;
            angularFriction = angular;
            contactFriction = contact;
        }

        public float GetContactFriction() {
            return contactFriction;
        }

        public void SetBouncyness(float bounce) {
            if (bounce < 0.0f || bounce > 1.0f) {
                gameLocal.Warning("idAFBody::SetBouncyness: bouncyness out of range, bounce = %.1f", bounce);
                return;
            }
            bouncyness = bounce;
        }

        public float GetBouncyness() {
            return bouncyness;
        }

        public void SetDensity(float density, final idMat3 inertiaScale /*= mat3_identity*/) {

            float[] massTemp = {mass};

            // get the body mass properties
            clipModel.GetMassProperties(density, massTemp, centerOfMass, inertiaTensor);

            mass = massTemp[0];

            // make sure we have a valid mass
            if (mass <= 0.0f || FLOAT_IS_NAN(mass)) {
                gameLocal.Warning("idAFBody::SetDensity: invalid mass for body '%s'", name);
                mass = 1.0f;
                centerOfMass.Zero();
                inertiaTensor.Identity();
            }

            // make sure the center of mass is at the body origin
            if (!centerOfMass.Compare(Vector.getVec3_origin(), CENTER_OF_MASS_EPSILON)) {
                gameLocal.Warning("idAFBody::SetDentity: center of mass not at origin for body '%s'", name);
            }
            centerOfMass.Zero();

            // calculate the inverse mass and inverse inertia tensor
            invMass = 1.0f / mass;
            if (!inertiaScale.equals(getMat3_identity())) {
                inertiaTensor.oMulSet(inertiaScale);
            }
            if (inertiaTensor.IsDiagonal(1e-3f)) {
                inertiaTensor.oSet(0, 1, inertiaTensor.oSet(0, 2, 0.0f));
                inertiaTensor.oSet(1, 0, inertiaTensor.oSet(1, 2, 0.0f));
                inertiaTensor.oSet(2, 0, inertiaTensor.oSet(2, 1, 0.0f));
                inverseInertiaTensor.Identity();
                inverseInertiaTensor.oSet(0, 0, 1.0f / inertiaTensor.oGet(0, 0));
                inverseInertiaTensor.oSet(1, 1, 1.0f / inertiaTensor.oGet(1, 1));
                inverseInertiaTensor.oSet(2, 2, 1.0f / inertiaTensor.oGet(2, 2));
            } else {
                inverseInertiaTensor.oSet(inertiaTensor.Inverse());
            }
        }

        public void SetDensity(float density) {
            SetDensity(density, getMat3_identity());
        }

        public float GetInverseMass() {
            return invMass;
        }

        public idMat3 GetInverseWorldInertia() {
            return current.worldAxis.Transpose().oMultiply(inverseInertiaTensor.oMultiply(current.worldAxis));
        }

        public void SetFrictionDirection(final idVec3 dir) {
            frictionDir.oSet(dir.oMultiply(current.worldAxis.Transpose()));
            fl.useFrictionDir = true;
        }

        public boolean GetFrictionDirection(idVec3 dir) {
            if (fl.useFrictionDir) {
                dir.oSet(frictionDir.oMultiply(current.worldAxis));
                return true;
            }
            return false;
        }

        public void SetContactMotorDirection(final idVec3 dir) {
            contactMotorDir.oSet(dir.oMultiply(current.worldAxis.Transpose()));
            fl.useContactMotorDir = true;
        }

        public boolean GetContactMotorDirection(idVec3 dir) {
            if (fl.useContactMotorDir) {
                dir.oSet(contactMotorDir.oMultiply(current.worldAxis));
                return true;
            }
            return false;
        }

        public void SetContactMotorVelocity(float vel) {
            contactMotorVelocity = vel;
        }

        public float GetContactMotorVelocity() {
            return contactMotorVelocity;
        }

        public void SetContactMotorForce(float force) {
            contactMotorForce = force;
        }

        public float GetContactMotorForce() {
            return contactMotorForce;
        }

        public void AddForce(final idVec3 point, final idVec3 force) {
            current.externalForce.SubVec3_oPluSet(0, force);
            current.externalForce.SubVec3_oPluSet(1, (point.oMinus(current.worldOrigin)).Cross(force));
        }

        /*
         ================
         idAFBody::InverseWorldSpatialInertiaMultiply

         dst = this->inverseWorldSpatialInertia * v;
         ================
         */
        public void InverseWorldSpatialInertiaMultiply(idVecX dst, final float[] v) {
            final float[] mPtr = inverseWorldSpatialInertia.ToFloatPtr();
            final float[] vPtr = v;
            float[] dstPtr = dst.ToFloatPtr();

            if (fl.spatialInertiaSparse) {
                dstPtr[0] = mPtr[0 * 6 + 0] * vPtr[0];
                dstPtr[1] = mPtr[1 * 6 + 1] * vPtr[1];
                dstPtr[2] = mPtr[2 * 6 + 2] * vPtr[2];
                dstPtr[3] = mPtr[3 * 6 + 3] * vPtr[3] + mPtr[3 * 6 + 4] * vPtr[4] + mPtr[3 * 6 + 5] * vPtr[5];
                dstPtr[4] = mPtr[4 * 6 + 3] * vPtr[3] + mPtr[4 * 6 + 4] * vPtr[4] + mPtr[4 * 6 + 5] * vPtr[5];
                dstPtr[5] = mPtr[5 * 6 + 3] * vPtr[3] + mPtr[5 * 6 + 4] * vPtr[4] + mPtr[5 * 6 + 5] * vPtr[5];
            } else {
                gameLocal.Warning("spatial inertia is not sparse for body %s", name);
            }
        }

        public idVec6 GetResponseForce(int index) {
//            return reinterpret_cast < idVec6 > (response[ index * 8]);
            return new idVec6(Arrays.copyOfRange(response, index * 8, (index * 8) + 6));
        }

        public void Save(idSaveGame saveFile) {
            saveFile.WriteFloat(linearFriction);
            saveFile.WriteFloat(angularFriction);
            saveFile.WriteFloat(contactFriction);
            saveFile.WriteFloat(bouncyness);
            saveFile.WriteInt(clipMask);
            saveFile.WriteVec3(frictionDir);
            saveFile.WriteVec3(contactMotorDir);
            saveFile.WriteFloat(contactMotorVelocity);
            saveFile.WriteFloat(contactMotorForce);

            saveFile.WriteFloat(mass);
            saveFile.WriteFloat(invMass);
            saveFile.WriteVec3(centerOfMass);
            saveFile.WriteMat3(inertiaTensor);
            saveFile.WriteMat3(inverseInertiaTensor);

            saveFile.WriteVec3(current.worldOrigin);
            saveFile.WriteMat3(current.worldAxis);
            saveFile.WriteVec6(current.spatialVelocity);
            saveFile.WriteVec6(current.externalForce);
            saveFile.WriteVec3(atRestOrigin);
            saveFile.WriteMat3(atRestAxis);
        }

        public void Restore(idRestoreGame saveFile) {
            linearFriction = saveFile.ReadFloat();
            angularFriction = saveFile.ReadFloat();
            contactFriction = saveFile.ReadFloat();
            bouncyness = saveFile.ReadFloat();
            clipMask = saveFile.ReadInt();
            saveFile.ReadVec3(frictionDir);
            saveFile.ReadVec3(contactMotorDir);
            contactMotorVelocity = saveFile.ReadFloat();
            contactMotorForce = saveFile.ReadFloat();

            mass = saveFile.ReadFloat();
            invMass = saveFile.ReadFloat();
            saveFile.ReadVec3(centerOfMass);
            saveFile.ReadMat3(inertiaTensor);
            saveFile.ReadMat3(inverseInertiaTensor);

            saveFile.ReadVec3(current.worldOrigin);
            saveFile.ReadMat3(current.worldAxis);
            saveFile.ReadVec6(current.spatialVelocity);
            saveFile.ReadVec6(current.externalForce);
            saveFile.ReadVec3(atRestOrigin);
            saveFile.ReadMat3(atRestAxis);
        }
    };

    //===============================================================
    //                                                        M
    //  idAFTree                                             MrE
    //                                                        E
    //===============================================================
    public static class idAFTree {
        // friend class idPhysics_AF;

        private idList<idAFBody> sortedBodies = new idList<>();
        //
        //

        /*
         ================
         idAFTree::Factor

         factor matrix for the primary constraints in the tree
         ================
         */
        public void Factor() {
            int i, j;
            idAFBody body;
            idAFConstraint child = new idAFConstraint();
            idMatX childI = new idMatX();

            childI.SetData(6, 6, MATX_ALLOCA(6 * 6));

            // from the leaves up towards the root
            for (i = sortedBodies.Num() - 1; i >= 0; i--) {
                body = sortedBodies.oGet(i);

                if (body.children.Num() != 0) {

                    for (j = 0; j < body.children.Num(); j++) {

                        child = body.children.oGet(j).primaryConstraint;

                        // child.I = - child.body1.J.Transpose() * child.body1.I * child.body1.J;
                        childI.SetSize(child.J1.GetNumRows(), child.J1.GetNumRows());
                        child.body1.J.TransposeMultiply(child.body1.I).Multiply(childI, child.body1.J);
                        childI.Negate();

                        child.invI = childI;
                        if (!child.invI.InverseFastSelf()) {
                            gameLocal.Warning("idAFTree::Factor: couldn't invert %dx%d matrix for constraint '%s'",
                                    child.invI.GetNumRows(), child.invI.GetNumColumns(), child.GetName());
                        }
                        child.J = child.invI.oMultiply(child.J);

                        body.I.oMinSet(child.J.TransposeMultiply(childI).oMultiply(child.J));
                    }

                    body.invI.oSet(body.I);
                    if (!body.invI.InverseFastSelf()) {
                        gameLocal.Warning("idAFTree::Factor: couldn't invert %dx%d matrix for body %s",
                                child.invI.GetNumRows(), child.invI.GetNumColumns(), body.GetName());
                    }
                    if (body.primaryConstraint != null) {
                        body.J.oSet(body.invI.oMultiply(body.J));
                    }
                } else if (body.primaryConstraint != null) {
                    body.J.oSet(body.inverseWorldSpatialInertia.oMultiply(body.J));
                }
            }
        }

        /*
         ================
         idAFTree::Solve

         solve for primary constraints in the tree
         ================
         */
        public void Solve(int auxiliaryIndex /*= 0*/) {
            int i, j;
            idAFBody body, child;
            idAFConstraint primaryConstraint;

            // from the leaves up towards the root
            for (i = sortedBodies.Num() - 1; i >= 0; i--) {
                body = sortedBodies.oGet(i);

                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j);
                    primaryConstraint = child.primaryConstraint;

                    if (!child.fl.isZero) {
                        child.J.TransposeMultiplySub(primaryConstraint.s, child.s);
                        primaryConstraint.fl.isZero = false;
                    }
                    if (!primaryConstraint.fl.isZero) {
                        primaryConstraint.J.TransposeMultiplySub(body.s, primaryConstraint.s);
                        body.fl.isZero = false;
                    }
                }
            }

            boolean useSymmetry = af_useSymmetry.GetBool();

            // from the root down towards the leaves
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);
                primaryConstraint = body.primaryConstraint;

                if (primaryConstraint != null) {

                    if (useSymmetry && body.parent.maxSubTreeAuxiliaryIndex < auxiliaryIndex) {
                        continue;
                    }

                    if (!primaryConstraint.fl.isZero) {
                        primaryConstraint.s = primaryConstraint.invI.oMultiply(primaryConstraint.s);
                    }
                    primaryConstraint.J.MultiplySub(primaryConstraint.s, primaryConstraint.body2.s);

                    primaryConstraint.lm = primaryConstraint.s;

                    if (useSymmetry && body.maxSubTreeAuxiliaryIndex < auxiliaryIndex) {
                        continue;
                    }

                    if (body.children.Num() != 0) {
                        if (!body.fl.isZero) {
                            body.s.oSet(body.invI.oMultiply(body.s));
                        }
                        body.J.MultiplySub(body.s, primaryConstraint.s);
                    }
                } else if (body.children.Num() != 0) {
                    body.s.oSet(body.invI.oMultiply(body.s));
                }
            }
        }

        public void Solve() {
            Solve(0);
        }

        /*
         ================
         idAFTree::Response

         calculate body forces in the tree in response to a constraint force
         ================
         */
        public void Response(final idAFConstraint constraint, int row, int auxiliaryIndex) {
            int i, j;
            idAFBody body;
            idAFConstraint child, primaryConstraint;
            idVecX v = new idVecX();

            // if a single body don't waste time because there aren't any primary constraints
            if (sortedBodies.Num() == 1) {
                body = constraint.body1;
                if (body.tree == this) {
                    body.GetResponseForce(body.numResponses).oSet(constraint.J1.SubVec6(row));
                    body.responseIndex[body.numResponses++] = auxiliaryIndex;
                } else {
                    body = constraint.body2;
                    body.GetResponseForce(body.numResponses).oSet(constraint.J2.SubVec6(row));
                    body.responseIndex[body.numResponses++] = auxiliaryIndex;
                }
                return;
            }

            v.SetData(6, VECX_ALLOCA(6));

            // initialize right hand side to zero
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.s.Zero();
                    primaryConstraint.fl.isZero = true;
                }
                body.s.Zero();
                body.fl.isZero = true;
                body.GetResponseForce(body.numResponses).Zero();
            }

            // set right hand side for first constrained body
            body = constraint.body1;
            if (body.tree == this) {
                body.InverseWorldSpatialInertiaMultiply(v, constraint.J1.oGet(row));
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.Multiply(primaryConstraint.s, v);
                    primaryConstraint.fl.isZero = false;
                }
                for (i = 0; i < body.children.Num(); i++) {
                    child = body.children.oGet(i).primaryConstraint;
                    child.J2.Multiply(child.s, v);
                    child.fl.isZero = false;
                }
                body.GetResponseForce(body.numResponses).oSet(constraint.J1.SubVec6(row));
            }

            // set right hand side for second constrained body
            body = constraint.body2;
            if (body != null && body.tree.equals(this)) {
                body.InverseWorldSpatialInertiaMultiply(v, constraint.J2.oGet(row));
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.MultiplyAdd(primaryConstraint.s, v);
                    primaryConstraint.fl.isZero = false;
                }
                for (i = 0; i < body.children.Num(); i++) {
                    child = body.children.oGet(i).primaryConstraint;
                    child.J2.MultiplyAdd(child.s, v);
                    child.fl.isZero = false;
                }
                body.GetResponseForce(body.numResponses).oSet(constraint.J2.SubVec6(row));
            }

            // solve for primary constraints
            Solve(auxiliaryIndex);

            boolean useSymmetry = af_useSymmetry.GetBool();

            // store body forces in response to the constraint force
            idVecX force = new idVecX();
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);

                if (useSymmetry && body.maxAuxiliaryIndex < auxiliaryIndex) {
                    continue;
                }

                force.SetData(6, Arrays.copyOfRange(body.response, body.numResponses * 8, body.response.length));

                // add forces of all primary constraints acting on this body
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.TransposeMultiplyAdd(force, primaryConstraint.lm);
                }
                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j).primaryConstraint;
                    child.J2.TransposeMultiplyAdd(force, child.lm);
                }

                body.responseIndex[body.numResponses++] = auxiliaryIndex;
            }
        }

        /*
         ================
         idAFTree::CalculateForces

         calculate forces on the bodies in the tree
         ================
         */
        public void CalculateForces(float timeStep) {
            int i, j;
            float invStep;
            idAFBody body;
            idAFConstraint child, c, primaryConstraint;

            // forces on bodies
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);

                body.totalForce.SubVec6(0).oSet(body.current.externalForce.oPlus(body.auxForce.SubVec6(0)));
            }

            // if a single body don't waste time because there aren't any primary constraints
            if (sortedBodies.Num() == 1) {
                return;
            }

            invStep = 1.0f / timeStep;

            // initialize right hand side
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);

                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.acceleration.SubVec6(0).oPluSet(body.current.spatialVelocity.oMultiply(invStep));
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    // b = ( J * acc + c )
                    c = primaryConstraint;
                    c.s = c.J1.oMultiply(c.body1.acceleration).oPlus(c.J2.oMultiply(c.body2.acceleration)).oPlus((c.c1.oPlus(c.c2)).oMultiply(invStep));
                    c.fl.isZero = false;
                }
                body.s.Zero();
                body.fl.isZero = true;
            }

            // solve for primary constraints
            Solve();

            // calculate forces on bodies after applying primary constraints
            for (i = 0; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);

                // add forces of all primary constraints acting on this body
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.TransposeMultiplyAdd(body.totalForce, primaryConstraint.lm);
                }
                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j).primaryConstraint;
                    child.J2.TransposeMultiplyAdd(body.totalForce, child.lm);
                }
            }
        }

        public void SetMaxSubTreeAuxiliaryIndex() {
            int i, j;
            idAFBody body, child;

            // from the leaves up towards the root
            for (i = sortedBodies.Num() - 1; i >= 0; i--) {
                body = sortedBodies.oGet(i);

                body.maxSubTreeAuxiliaryIndex = body.maxAuxiliaryIndex;
                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j);
                    if (child.maxSubTreeAuxiliaryIndex > body.maxSubTreeAuxiliaryIndex) {
                        body.maxSubTreeAuxiliaryIndex = child.maxSubTreeAuxiliaryIndex;
                    }
                }
            }
        }

        /*
         ================
         idAFTree::SortBodies

         sort body list to make sure parents come first
         ================
         */
        public void SortBodies() {
            int i;
            idAFBody body;

            // find the root
            for (i = 0; i < sortedBodies.Num(); i++) {
                if (null == sortedBodies.oGet(i).parent) {
                    break;
                }
            }

            if (i >= sortedBodies.Num()) {
                gameLocal.Error("Articulated figure tree has no root.");
            }

            body = sortedBodies.oGet(i);
            sortedBodies.Clear();
            sortedBodies.Append(body);
            SortBodies_r(sortedBodies, body);
        }

        public void SortBodies_r(idList<idAFBody> sortedList, idAFBody body) {
            int i;

            for (i = 0; i < body.children.Num(); i++) {
                sortedList.Append(body.children.oGet(i));
            }
            for (i = 0; i < body.children.Num(); i++) {
                SortBodies_r(sortedList, body.children.oGet(i));
            }
        }

        public void DebugDraw(final idVec4 color) {
            int i;
            idAFBody body;

            for (i = 1; i < sortedBodies.Num(); i++) {
                body = sortedBodies.oGet(i);
                gameRenderWorld.DebugArrow(color, body.parent.current.worldOrigin, body.current.worldOrigin, 1);
            }
        }
    };

    //===============================================================
    //                                                        M
    //  idPhysics_AF                                         MrE
    //                                                        E
    //===============================================================
    public static class AFPState_s {

        int    atRest;                    // >= 0 if articulated figure is at rest
        float  noMoveTime;                // time the articulated figure is hardly moving
        float  activateTime;              // time since last activation
        float  lastTimeStep;              // last time step
        idVec6 pushVelocity;              // velocity with which the af is pushed

        public AFPState_s() {
            pushVelocity = new idVec6();
        }
    };

    public static class AFCollision_s {

        trace_s  trace;
        idAFBody body;
    };

    public static class idPhysics_AF extends idPhysics_Base {

        // articulated figure
        private idList<idAFTree>               trees;                     // tree structures
        private idList<idAFBody>               bodies;                    // all bodies
        private idList<idAFConstraint>         constraints;               // all frame independent constraints
        private idList<idAFConstraint>         primaryConstraints;        // list with primary constraints
        private idList<idAFConstraint>         auxiliaryConstraints;      // list with auxiliary constraints
        private idList<idAFConstraint>         frameConstraints;          // constraints that only live one frame
        private idList<idAFConstraint_Contact> contactConstraints;        // contact constraints
        private idList<Integer>                contactBodies;             // body id for each contact
        private idList<AFCollision_s>          collisions;                // collisions
        private boolean                        changedAF;                 // true when the articulated figure just changed
        //
        // properties
        private float                          linearFriction;            // default translational friction
        private float                          angularFriction;           // default rotational friction
        private float                          contactFriction;           // default friction with contact surfaces
        private float                          bouncyness;                // default bouncyness
        private float                          totalMass;                 // total mass of articulated figure
        private float                          forceTotalMass;            // force this total mass
        //
        private idVec2                         suspendVelocity;           // simulation may not be suspended if a body has more velocity
        private idVec2                         suspendAcceleration;       // simulation may not be suspended if a body has more acceleration
        private float                          noMoveTime;                // suspend simulation if hardly any movement for this many seconds
        private float                          noMoveTranslation;         // maximum translation considered no movement
        private float                          noMoveRotation;            // maximum rotation considered no movement
        private float                          minMoveTime;               // if > 0 the simulation is never suspended before running this many seconds
        private float                          maxMoveTime;               // if > 0 the simulation is always suspeded after running this many seconds
        private float                          impulseThreshold;          // threshold below which impulses are ignored to avoid continuous activation
        //
        private float                          timeScale;                 // the time is scaled with this value for slow motion effects
        private float                          timeScaleRampStart;        // start of time scale change
        private float                          timeScaleRampEnd;          // end of time scale change
        //
        private float                          jointFrictionScale;        // joint friction scale
        private float                          jointFrictionDent;         // joint friction dives from 1 to this value and goes up again
        private float                          jointFrictionDentStart;    // start time of joint friction dent
        private float                          jointFrictionDentEnd;      // end time of joint friction dent
        private float                          jointFrictionDentScale;    // dent scale
        //
        private float                          contactFrictionScale;      // contact friction scale
        private float                          contactFrictionDent;       // contact friction dives from 1 to this value and goes up again
        private float                          contactFrictionDentStart;  // start time of contact friction dent
        private float                          contactFrictionDentEnd;    // end time of contact friction dent
        private float                          contactFrictionDentScale;  // dent scale
        //
        private boolean                        enableCollision;           // if true collision detection is enabled
        private boolean                        selfCollision;             // if true the self collision is allowed
        private boolean                        comeToRest;                // if true the figure can come to rest
        private boolean                        linearTime;                // if true use the linear time algorithm
        private boolean                        noImpact;                  // if true do not activate when another object collides
        private boolean                        worldConstraintsLocked;    // if true world constraints cannot be moved
        private boolean                        forcePushable;             // if true can be pushed even when bound to a master
        //
        // physics state
        private AFPState_s                     current;
        private AFPState_s                     saved;
        //
        private idAFBody                       masterBody;                // master body
        private idLCP                          lcp;                       // linear complementarity problem solver
//
//

        // CLASS_PROTOTYPE( idPhysics_AF );
        public idPhysics_AF() {
            trees = new idList<>();
            bodies = new idList<>();
            constraints = new idList<>();
            primaryConstraints = new idList<>();
            auxiliaryConstraints = new idList<>();
            frameConstraints = new idList<>();
            contactConstraints = new idList<>()     ;
            contactBodies = new idList<>();
            contacts = new idList<>();
            collisions = new idList<>();
            changedAF = true;
            masterBody = null;

            lcp = idLCP.AllocSymmetric();

            current = new AFPState_s();//memset( &current, 0, sizeof( current ) );
            current.atRest = -1;
            current.lastTimeStep = USERCMD_MSEC;
            saved = current;

            linearFriction = 0.005f;
            angularFriction = 0.005f;
            contactFriction = 0.8f;
            bouncyness = 0.4f;
            totalMass = 0.0f;
            forceTotalMass = -1.0f;

            suspendVelocity = new idVec2(SUSPEND_LINEAR_VELOCITY, SUSPEND_ANGULAR_VELOCITY);
            suspendAcceleration = new idVec2(SUSPEND_LINEAR_ACCELERATION, SUSPEND_LINEAR_ACCELERATION);
            noMoveTime = NO_MOVE_TIME;
            noMoveTranslation = NO_MOVE_TRANSLATION_TOLERANCE;
            noMoveRotation = NO_MOVE_ROTATION_TOLERANCE;
            minMoveTime = MIN_MOVE_TIME;
            maxMoveTime = MAX_MOVE_TIME;
            impulseThreshold = IMPULSE_THRESHOLD;

            timeScale = 1.0f;
            timeScaleRampStart = 0.0f;
            timeScaleRampEnd = 0.0f;

            jointFrictionScale = 0.0f;
            jointFrictionDent = 0.0f;
            jointFrictionDentStart = 0.0f;
            jointFrictionDentEnd = 0.0f;
            jointFrictionDentScale = 0.0f;

            contactFrictionScale = 0.0f;
            contactFrictionDent = 0.0f;
            contactFrictionDentStart = 0.0f;
            contactFrictionDentEnd = 0.0f;
            contactFrictionDentScale = 0.0f;

            enableCollision = true;
            selfCollision = true;
            comeToRest = true;
            linearTime = true;
            noImpact = false;
            worldConstraintsLocked = false;
            forcePushable = false;

            if (AF_TIMINGS) {
                lastTimerReset = 0;
            }
        }
        // ~idPhysics_AF();

        @Override
        public void Save(idSaveGame saveFile) {
            int i;

            // the articulated figure structure is handled by the owner
            idPhysics_AF_SavePState(saveFile, current);
            idPhysics_AF_SavePState(saveFile, saved);

            saveFile.WriteInt(bodies.Num());
            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).Save(saveFile);
            }
            if (masterBody != null) {
                saveFile.WriteBool(true);
                masterBody.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }

            saveFile.WriteInt(constraints.Num());
            for (i = 0; i < constraints.Num(); i++) {
                constraints.oGet(i).Save(saveFile);
            }

            saveFile.WriteBool(changedAF);

            saveFile.WriteFloat(linearFriction);
            saveFile.WriteFloat(angularFriction);
            saveFile.WriteFloat(contactFriction);
            saveFile.WriteFloat(bouncyness);
            saveFile.WriteFloat(totalMass);
            saveFile.WriteFloat(forceTotalMass);

            saveFile.WriteVec2(suspendVelocity);
            saveFile.WriteVec2(suspendAcceleration);
            saveFile.WriteFloat(noMoveTime);
            saveFile.WriteFloat(noMoveTranslation);
            saveFile.WriteFloat(noMoveRotation);
            saveFile.WriteFloat(minMoveTime);
            saveFile.WriteFloat(maxMoveTime);
            saveFile.WriteFloat(impulseThreshold);

            saveFile.WriteFloat(timeScale);
            saveFile.WriteFloat(timeScaleRampStart);
            saveFile.WriteFloat(timeScaleRampEnd);

            saveFile.WriteFloat(jointFrictionScale);
            saveFile.WriteFloat(jointFrictionDent);
            saveFile.WriteFloat(jointFrictionDentStart);
            saveFile.WriteFloat(jointFrictionDentEnd);
            saveFile.WriteFloat(jointFrictionDentScale);

            saveFile.WriteFloat(contactFrictionScale);
            saveFile.WriteFloat(contactFrictionDent);
            saveFile.WriteFloat(contactFrictionDentStart);
            saveFile.WriteFloat(contactFrictionDentEnd);
            saveFile.WriteFloat(contactFrictionDentScale);

            saveFile.WriteBool(enableCollision);
            saveFile.WriteBool(selfCollision);
            saveFile.WriteBool(comeToRest);
            saveFile.WriteBool(linearTime);
            saveFile.WriteBool(noImpact);
            saveFile.WriteBool(worldConstraintsLocked);
            saveFile.WriteBool(forcePushable);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            int i;
            int[] num = {0};
            boolean[] hasMaster = {false};

            // the articulated figure structure should have already been restored
            idPhysics_AF_RestorePState(saveFile, current);
            idPhysics_AF_RestorePState(saveFile, saved);

            saveFile.ReadInt(num);
            assert (num[0] == bodies.Num());
            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).Restore(saveFile);
            }
            saveFile.ReadBool(hasMaster);
            if (hasMaster[0]) {
                masterBody = new idAFBody();
                masterBody.Restore(saveFile);
            }

            saveFile.ReadInt(num);
            assert (num[0] == constraints.Num());
            for (i = 0; i < constraints.Num(); i++) {
                constraints.oGet(i).Restore(saveFile);
            }

            changedAF = saveFile.ReadBool();

            linearFriction = saveFile.ReadFloat();
            angularFriction = saveFile.ReadFloat();
            contactFriction = saveFile.ReadFloat();
            bouncyness = saveFile.ReadFloat();
            totalMass = saveFile.ReadFloat();
            forceTotalMass = saveFile.ReadFloat();

            saveFile.ReadVec2(suspendVelocity);
            saveFile.ReadVec2(suspendAcceleration);
            noMoveTime = saveFile.ReadFloat();
            noMoveTranslation = saveFile.ReadFloat();
            noMoveRotation = saveFile.ReadFloat();
            minMoveTime = saveFile.ReadFloat();
            maxMoveTime = saveFile.ReadFloat();
            impulseThreshold = saveFile.ReadFloat();

            timeScale = saveFile.ReadFloat();
            timeScaleRampStart = saveFile.ReadFloat();
            timeScaleRampEnd = saveFile.ReadFloat();

            jointFrictionScale = saveFile.ReadFloat();
            jointFrictionDent = saveFile.ReadFloat();
            jointFrictionDentStart = saveFile.ReadFloat();
            jointFrictionDentEnd = saveFile.ReadFloat();
            jointFrictionDentScale = saveFile.ReadFloat();

            contactFrictionScale = saveFile.ReadFloat();
            contactFrictionDent = saveFile.ReadFloat();
            contactFrictionDentStart = saveFile.ReadFloat();
            contactFrictionDentEnd = saveFile.ReadFloat();
            contactFrictionDentScale = saveFile.ReadFloat();

            enableCollision = saveFile.ReadBool();
            selfCollision = saveFile.ReadBool();
            comeToRest = saveFile.ReadBool();
            linearTime = saveFile.ReadBool();
            noImpact = saveFile.ReadBool();
            worldConstraintsLocked = saveFile.ReadBool();
            forcePushable = saveFile.ReadBool();

            changedAF = true;

            UpdateClipModels();
        }

        /*
         ================
         idPhysics_AF::AddBody

         bodies get an id in the order they are added starting at zero
         as such the first body added will get id zero
         ================
         */
        // initialisation
        public int AddBody(idAFBody body) {    // returns body id
            int id = 0;

            if (null == body.clipModel) {
                gameLocal.Error("idPhysics_AF::AddBody: body '%s' has no clip model.", body.name);
            }

            if (bodies.Find(body) != null) {
                gameLocal.Error("idPhysics_AF::AddBody: body '%s' added twice.", body.name);
            }

            if (GetBody(body.name.toString()) != null) {
                gameLocal.Error("idPhysics_AF::AddBody: a body with the name '%s' already exists.", body.name);
            }

            id = bodies.Num();
            body.clipModel.SetId(id);
            if (body.linearFriction < 0.0f) {
                body.linearFriction = linearFriction;
                body.angularFriction = angularFriction;
                body.contactFriction = contactFriction;
            }
            if (body.bouncyness < 0.0f) {
                body.bouncyness = bouncyness;
            }
            if (!body.fl.clipMaskSet) {
                body.clipMask = clipMask;
            }

            bodies.Append(body);

            changedAF = true;

            return id;
        }

        public void AddConstraint(idAFConstraint constraint) {

            if (constraints.Find(constraint) != null) {
                gameLocal.Error("idPhysics_AF::AddConstraint: constraint '%s' added twice.", constraint.name);
            }
            if (GetConstraint(constraint.name.toString()) != null) {
                gameLocal.Error("idPhysics_AF::AddConstraint: a constraint with the name '%s' already exists.", constraint.name);
            }
            if (null == constraint.body1) {
                gameLocal.Error("idPhysics_AF::AddConstraint: body1 == NULL on constraint '%s'.", constraint.name);
            }
            if (null == bodies.Find(constraint.body1)) {
                gameLocal.Error("idPhysics_AF::AddConstraint: body1 of constraint '%s' is not part of the articulated figure.", constraint.name);
            }
            if (constraint.body2 != null && null == bodies.Find(constraint.body2)) {
                gameLocal.Error("idPhysics_AF::AddConstraint: body2 of constraint '%s' is not part of the articulated figure.", constraint.name);
            }
            if (constraint.body1.equals(constraint.body2)) {
                gameLocal.Error("idPhysics_AF::AddConstraint: body1 and body2 of constraint '%s' are the same.", constraint.name);
            }

            constraints.Append(constraint);
            constraint.physics = this;

            changedAF = true;
        }

        public void AddFrameConstraint(idAFConstraint constraint) {
            frameConstraints.Append(constraint);
            constraint.physics = this;
        }

        // force a body to have a certain id
        public void ForceBodyId(idAFBody body, int newId) {
            int id;

            id = bodies.FindIndex(body);
            if (id == -1) {
                gameLocal.Error("ForceBodyId: body '%s' is not part of the articulated figure.\n", body.name);
            }
            if (id != newId) {
                idAFBody b = bodies.oGet(newId);
                bodies.oSet(newId, bodies.oGet(id));
                bodies.oSet(id, b);
                changedAF = true;
            }
        }

        // get body or constraint id
        public int GetBodyId(idAFBody body) {
            int id;

            id = bodies.FindIndex(body);
            if (id == -1 && body != null) {//TODO:can't be null
                gameLocal.Error("GetBodyId: body '%s' is not part of the articulated figure.\n", body.name);
            }
            return id;
        }

        public int GetBodyId(final String bodyName) {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                if (0 == bodies.oGet(i).name.Icmp(bodyName)) {
                    return i;
                }
            }
            gameLocal.Error("GetBodyId: no body with the name '%s' is not part of the articulated figure.\n", bodyName);
            return 0;
        }

        public int GetConstraintId(idAFConstraint constraint) {
            int id;

            id = constraints.FindIndex(constraint);
            if (id == -1 && constraint != null) {//TODO:can't be null
                gameLocal.Error("GetConstraintId: constraint '%s' is not part of the articulated figure.\n", constraint.name);
            }
            return id;
        }

        public int GetConstraintId(final String constraintName) {
            int i;

            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).name.Icmp(constraintName) == 0) {
                    return i;
                }
            }
            gameLocal.Error("GetConstraintId: no constraint with the name '%s' is not part of the articulated figure.\n", constraintName);
            return 0;
        }

        // number of bodies and constraints
        public int GetNumBodies() {
            return bodies.Num();
        }

        public int GetNumConstraints() {
            return constraints.Num();
        }

        // retrieve body or constraint
        public idAFBody GetBody(final String bodyName) {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                if (0 == bodies.oGet(i).name.Icmp(bodyName)) {
                    return bodies.oGet(i);
                }
            }

            return null;
        }

        public idAFBody GetBody(final idStr bodyName) {
            return GetBody(bodyName.toString());
        }

        public idAFBody GetBody(final int id) {
            if (id < 0 || id >= bodies.Num()) {
                gameLocal.Error("GetBody: no body with id %d exists\n", id);
                return null;
            }
            return bodies.oGet(id);
        }

        public idAFBody GetMasterBody() {
            return masterBody;
        }

        public idAFConstraint GetConstraint(final String constraintName) {
            int i;

            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).name.Icmp(constraintName) == 0) {
                    return constraints.oGet(i);
                }
            }

            return null;
        }

        public idAFConstraint GetConstraint(final int id) {
            if (id < 0 || id >= constraints.Num()) {
                gameLocal.Error("GetConstraint: no constraint with id %d exists\n", id);
                return null;
            }
            return constraints.oGet(id);
        }

        // delete body or constraint
        public void DeleteBody(final String bodyName) {
            int i;

            // find the body with the given name
            for (i = 0; i < bodies.Num(); i++) {
                if (0 == bodies.oGet(i).name.Icmp(bodyName)) {
                    break;
                }
            }

            if (i >= bodies.Num()) {
                gameLocal.Warning("DeleteBody: no body found in the articulated figure with the name '%s' for entity '%s' type '%s'.",
                        bodyName, self.name, self.GetType().getName());
                return;
            }

            DeleteBody(i);
        }

        public void DeleteBody(final int id) {
            int j;

            if (id < 0 || id > bodies.Num()) {
                gameLocal.Error("DeleteBody: no body with id %d.", id);
                return;
            }

            // remove any constraints attached to this body
            for (j = 0; j < constraints.Num(); j++) {
                if (constraints.oGet(j).body1.equals(bodies.oGet(id)) || constraints.oGet(j).body2.equals(bodies.oGet(id))) {
//			delete constraints[j];
                    constraints.RemoveIndex(j);
                    j--;
                }
            }

            // remove the body
//	delete bodies[id];
            bodies.RemoveIndex(id);

            // set new body ids
            for (j = 0; j < bodies.Num(); j++) {
                bodies.oGet(j).clipModel.SetId(j);
            }

            changedAF = true;
        }

        public void DeleteConstraint(final String constraintName) {
            int i;

            // find the constraint with the given name
            for (i = 0; i < constraints.Num(); i++) {
                if (NOT(constraints.oGet(i).name.Icmp(constraintName))) {
                    break;
                }
            }

            if (i >= constraints.Num()) {
                gameLocal.Warning("DeleteConstraint: no constriant found in the articulated figure with the name '%s' for entity '%s' type '%s'.",
                        constraintName, self.name, self.GetType().getName());
                return;
            }

            DeleteConstraint(i);
        }

        public void DeleteConstraint(final int id) {

            if (id < 0 || id >= constraints.Num()) {
                gameLocal.Error("DeleteConstraint: no constraint with id %d.", id);
                return;
            }

            // remove the constraint
//	delete constraints[id];
            constraints.RemoveIndex(id);

            changedAF = true;
        }

        // get all the contact constraints acting on the body
        public int GetBodyContactConstraints(final int id, idAFConstraint_Contact[] contacts, int maxContacts) {
            int i, numContacts;
            idAFBody body;
            idAFConstraint_Contact contact;

            if (id < 0 || id >= bodies.Num() || maxContacts <= 0) {
                return 0;
            }

            numContacts = 0;
            body = bodies.oGet(id);
            for (i = 0; i < contactConstraints.Num(); i++) {
                contact = contactConstraints.oGet(i);
                if (contact.body1 == body || contact.body2 == body) {
                    contacts[numContacts++] = contact;
                    if (numContacts >= maxContacts) {
                        return numContacts;
                    }
                }
            }
            return numContacts;
        }

        // set the default friction for bodies
        public void SetDefaultFriction(float linear, float angular, float contact) {
            if (linear < 0.0f || linear > 1.0f
                    || angular < 0.0f || angular > 1.0f
                    || contact < 0.0f || contact > 1.0f) {
                return;
            }
            linearFriction = linear;
            angularFriction = angular;
            contactFriction = contact;
        }

        // suspend settings
        public void SetSuspendSpeed(final idVec2 velocity, final idVec2 acceleration) {
            this.suspendVelocity = velocity;
            this.suspendAcceleration = acceleration;
        }

        // set the time and tolerances used to determine if the simulation can be suspended when the figure hardly moves for a while
        public void SetSuspendTolerance(final float noMoveTime, final float translationTolerance, final float rotationTolerance) {
            this.noMoveTime = noMoveTime;
            this.noMoveTranslation = translationTolerance;
            this.noMoveRotation = rotationTolerance;
        }

        // set minimum and maximum simulation time in seconds
        public void SetSuspendTime(final float minTime, final float maxTime) {
            this.minMoveTime = minTime;
            this.maxMoveTime = maxTime;
        }

        // set the time scale value
        public void SetTimeScale(final float ts) {
            timeScale = ts;
        }

        // set time scale ramp
        public void SetTimeScaleRamp(final float start, final float end) {
            timeScaleRampStart = start;
            timeScaleRampEnd = end;
        }

        // set the joint friction scale
        public void SetJointFrictionScale(final float scale) {
            jointFrictionScale = scale;
        }
        // set joint friction dent

        public void SetJointFrictionDent(final float dent, final float start, final float end) {
            jointFrictionDent = dent;
            jointFrictionDentStart = start;
            jointFrictionDentEnd = end;
        }

        // get the current joint friction scale
        public float GetJointFrictionScale() {
            if (jointFrictionDentScale > 0.0f) {
                return jointFrictionDentScale;
            } else if (jointFrictionScale > 0.0f) {
                return jointFrictionScale;
            } else if (af_jointFrictionScale.GetFloat() > 0.0f) {
                return af_jointFrictionScale.GetFloat();
            }
            return 1.0f;
        }

        // set the contact friction scale
        public void SetContactFrictionScale(final float scale) {
            contactFrictionScale = scale;
        }

        // set contact friction dent
        public void SetContactFrictionDent(final float dent, final float start, final float end) {
            contactFrictionDent = dent;
            contactFrictionDentStart = start;
            contactFrictionDentEnd = end;
        }

        // get the current contact friction scale
        public float GetContactFrictionScale() {
            if (contactFrictionDentScale > 0.0f) {
                return contactFrictionDentScale;
            } else if (contactFrictionScale > 0.0f) {
                return contactFrictionScale;
            } else if (af_contactFrictionScale.GetFloat() > 0.0f) {
                return af_contactFrictionScale.GetFloat();
            }
            return 1.0f;
        }

        // enable or disable collision detection
        public void SetCollision(final boolean enable) {
            enableCollision = enable;
        }

        // enable or disable self collision
        public void SetSelfCollision(final boolean enable) {
            selfCollision = enable;
        }

        // enable or disable coming to a dead stop
        public void SetComeToRest(boolean enable) {
            comeToRest = enable;
        }

        // call when structure of articulated figure changes
        public void SetChanged() {
            changedAF = true;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            noImpact = false;
        }

        public void DisableImpact() {
            noImpact = true;
        }

        // lock of unlock the world constraints
        public void LockWorldConstraints(final boolean lock) {
            worldConstraintsLocked = lock;
        }

        // set force pushable
        public void SetForcePushable(final boolean enable) {
            forcePushable = enable;
        }

        // update the clip model positions
        public void UpdateClipModels() {
            int i;
            idAFBody body;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);
                body.clipModel.Link(gameLocal.clip, self, body.clipModel.GetId(), body.current.worldOrigin, body.current.worldAxis);
            }
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            if (id >= 0 && id < bodies.Num()) {
                return bodies.oGet(id).GetClipModel();
            }
            return null;
        }

        @Override
        public int GetNumClipModels() {
            return bodies.Num();
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
            if (id >= 0 && id < bodies.Num()) {
            } else {
                forceTotalMass = mass;
            }
            SetChanged();
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            if (id >= 0 && id < bodies.Num()) {
                return bodies.oGet(id).mass;
            }
            return totalMass;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            int i;

            if (id >= 0 && id < bodies.Num()) {
                bodies.oGet(id).GetClipModel().SetContents(contents);
            } else {
                for (i = 0; i < bodies.Num(); i++) {
                    bodies.oGet(i).GetClipModel().SetContents(contents);
                }
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            int i, contents;

            if (id >= 0 && id < bodies.Num()) {
                return bodies.oGet(id).GetClipModel().GetContents();
            } else {
                contents = 0;
                for (i = 0; i < bodies.Num(); i++) {
                    contents |= bodies.oGet(i).GetClipModel().GetContents();
                }
                return contents;
            }
        }

        private static idBounds relBounds;

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            int i;

            if (id >= 0 && id < bodies.Num()) {
                return bodies.oGet(id).GetClipModel().GetBounds();
            } else if (0 == bodies.Num()) {
                relBounds.Zero();
                return relBounds;
            } else {
                relBounds = bodies.oGet(0).GetClipModel().GetBounds();
                for (i = 1; i < bodies.Num(); i++) {
                    idBounds bounds = new idBounds();
                    idVec3 origin = (bodies.oGet(i).GetWorldOrigin().oMinus(bodies.oGet(0).GetWorldOrigin())).oMultiply(bodies.oGet(0).GetWorldAxis().Transpose());
                    idMat3 axis = bodies.oGet(i).GetWorldAxis().oMultiply(bodies.oGet(0).GetWorldAxis().Transpose());
                    bounds.FromTransformedBounds(bodies.oGet(i).GetClipModel().GetBounds(), origin, axis);
                    relBounds.oPluSet(bounds);
                }
                return relBounds;
            }
        }

        private static idBounds absBounds;

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            int i;

            if (id >= 0 && id < bodies.Num()) {
                return bodies.oGet(id).GetClipModel().GetAbsBounds();
            } else if (0 == bodies.Num()) {
                absBounds.Zero();
                return absBounds;
            } else {
                absBounds = bodies.oGet(0).GetClipModel().GetAbsBounds();
                for (i = 1; i < bodies.Num(); i++) {
                    absBounds.oPluSet(bodies.oGet(i).GetClipModel().GetAbsBounds());
                }
                return absBounds;
            }
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            float timeStep;

            if (timeScaleRampStart < MS2SEC(endTimeMSec) && timeScaleRampEnd > MS2SEC(endTimeMSec)) {
                timeStep = MS2SEC(timeStepMSec) * (MS2SEC(endTimeMSec) - timeScaleRampStart) / (timeScaleRampEnd - timeScaleRampStart);
            } else if (af_timeScale.GetFloat() != 1.0f) {
                timeStep = MS2SEC(timeStepMSec) * af_timeScale.GetFloat();
            } else {
                timeStep = MS2SEC(timeStepMSec) * timeScale;
            }
            current.lastTimeStep = timeStep;

            // if the articulated figure changed
            if (changedAF || (linearTime != af_useLinearTime.GetBool())) {
                BuildTrees();
                changedAF = false;
                linearTime = af_useLinearTime.GetBool();
            }

            // get the new master position
            if (masterBody != null) {
                idVec3 masterOrigin = new idVec3();
                idMat3 masterAxis = new idMat3();
                self.GetMasterPosition(masterOrigin, masterAxis);
                if (current.atRest >= 0 && (masterBody.current.worldOrigin != masterOrigin || masterBody.current.worldAxis != masterAxis)) {
                    Activate();
                }
                masterBody.current.worldOrigin.oSet(masterOrigin);
                masterBody.current.worldAxis.oSet(masterAxis);
            }

            // if the simulation is suspended because the figure is at rest
            if (current.atRest >= 0 || timeStep <= 0.0f) {
                DebugDraw();
                return false;
            }

            // move the af velocity into the frame of a pusher
            AddPushVelocity(current.pushVelocity.oNegative());

            if (AF_TIMINGS) {
                timer_total.Start();
            }

            if (AF_TIMINGS) {
                timer_collision.Start();
            }

            // evaluate contacts
            EvaluateContacts();

            // setup contact constraints
            SetupContactConstraints();

            if (AF_TIMINGS) {
                timer_collision.Stop();
            }

            // evaluate constraint equations
            EvaluateConstraints(timeStep);

            // apply friction
            ApplyFriction(timeStep, endTimeMSec);

            // add frame constraints
            AddFrameConstraints();

            int i, numPrimary = 0, numAuxiliary = 0;
            if (AF_TIMINGS) {
                for (i = 0; i < primaryConstraints.Num(); i++) {
                    numPrimary += primaryConstraints.oGet(i).J1.GetNumRows();
                }
                for (i = 0; i < auxiliaryConstraints.Num(); i++) {
                    numAuxiliary += auxiliaryConstraints.oGet(i).J1.GetNumRows();
                }
                timer_pc.Start();
            }

            // factor matrices for primary constraints
            PrimaryFactor();

            // calculate forces on bodies after applying primary constraints
            PrimaryForces(timeStep);

            if (AF_TIMINGS) {
                timer_pc.Stop();
                timer_ac.Start();
            }

            // calculate and apply auxiliary constraint forces
            AuxiliaryForces(timeStep);

            if (AF_TIMINGS) {
                timer_ac.Stop();
            }

            // evolve current state to next state
            Evolve(timeStep);

            // debug graphics
            DebugDraw();

            // clear external forces on all bodies
            ClearExternalForce();

            // apply contact force to other entities
            ApplyContactForces();

            // remove all frame constraints
            RemoveFrameConstraints();

            if (AF_TIMINGS) {
                timer_collision.Start();
            }

            // check for collisions between current and next state
            CheckForCollisions(timeStep);

            if (AF_TIMINGS) {
                timer_collision.Stop();
            }

            // swap the current and next state
            SwapStates();

            // make sure all clip models are disabled in case they were enabled for self collision
            if (selfCollision && !af_skipSelfCollision.GetBool()) {
                DisableClip();
            }

            // apply collision impulses
            if (ApplyCollisions(timeStep)) {
                current.atRest = gameLocal.time;
                comeToRest = true;
            }

            // test if the simulation can be suspended because the whole figure is at rest
            if (comeToRest && TestIfAtRest(timeStep)) {
                Rest();
            } else {
                ActivateContactEntities();
            }

            // add gravitational force
            AddGravity();

            // move the af velocity back into the world frame
            AddPushVelocity(current.pushVelocity);
            current.pushVelocity.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("articulated figure moved outside world bounds for entity '%s' type '%s' at (%s)",
                        self.name, self.GetType().getName(), bodies.oGet(0).current.worldOrigin.ToString(0));
                Rest();
            }

            if (AF_TIMINGS) {
                timer_total.Stop();

                if (af_showTimings.GetInteger() == 1) {
                    gameLocal.Printf("%12s: t %1.4f pc %2d, %1.4f ac %2d %1.4f lcp %1.4f cd %1.4f\n",
                            self.name,
                            timer_total.Milliseconds(),
                            numPrimary, timer_pc.Milliseconds(),
                            numAuxiliary, timer_ac.Milliseconds() - timer_lcp.Milliseconds(),
                            timer_lcp.Milliseconds(), timer_collision.Milliseconds());
                } else if (af_showTimings.GetInteger() == 2) {
                    numArticulatedFigures++;
                    if (endTimeMSec > lastTimerReset) {
                        gameLocal.Printf("af %d: t %1.4f pc %2d, %1.4f ac %2d %1.4f lcp %1.4f cd %1.4f\n",
                                numArticulatedFigures,
                                timer_total.Milliseconds(),
                                numPrimary, timer_pc.Milliseconds(),
                                numAuxiliary, timer_ac.Milliseconds() - timer_lcp.Milliseconds(),
                                timer_lcp.Milliseconds(), timer_collision.Milliseconds());
                    }
                }

                if (endTimeMSec > lastTimerReset) {
                    lastTimerReset = endTimeMSec;
                    numArticulatedFigures = 0;
                    timer_total.Clear();
                    timer_pc.Clear();
                    timer_ac.Clear();
                    timer_collision.Clear();
                    timer_lcp.Clear();
                }
            }

            return true;
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
        }

        @Override
        public int GetTime() {
            return gameLocal.time;
        }

        @Override
        public void GetImpactInfo(final int id, final idVec3 point, impactInfo_s info) {
            if (id < 0 || id >= bodies.Num()) {
//                memset(info, 0, sizeof(info));
                info.clear();
                return;
            }
            info.invMass = 1.0f / bodies.oGet(id).mass;
            info.invInertiaTensor = bodies.oGet(id).current.worldAxis.Transpose().oMultiply(bodies.oGet(id).inverseInertiaTensor.oMultiply(bodies.oGet(id).current.worldAxis));
            info.position = point.oMinus(bodies.oGet(id).current.worldOrigin);
            info.velocity = bodies.oGet(id).current.spatialVelocity.SubVec3(0).oPlus(bodies.oGet(id).current.spatialVelocity.SubVec3(1).Cross(info.position));
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (id < 0 || id >= bodies.Num()) {
                return;
            }
            if (noImpact || impulse.LengthSqr() < Square(impulseThreshold)) {
                return;
            }
            idMat3 invWorldInertiaTensor = bodies.oGet(id).current.worldAxis.Transpose().oMultiply(bodies.oGet(id).inverseInertiaTensor.oMultiply(bodies.oGet(id).current.worldAxis));
            bodies.oGet(id).current.spatialVelocity.SubVec3_oPluSet(0, impulse.oMultiply(bodies.oGet(id).invMass));
            bodies.oGet(id).current.spatialVelocity.SubVec3_oPluSet(1, invWorldInertiaTensor.oMultiply((point.oMinus(bodies.oGet(id).current.worldOrigin)).Cross(impulse)));
            Activate();
        }

        @Override
        public void AddForce(final int id, final idVec3 point, final idVec3 force) {
            if (noImpact) {
                return;
            }
            if (id < 0 || id >= bodies.Num()) {
                return;
            }
            bodies.oGet(id).current.externalForce.SubVec3_oPluSet(0, force);
            bodies.oGet(id).current.externalForce.SubVec3_oPluSet(1, (point.oMinus(bodies.oGet(id).current.worldOrigin)).Cross(force));
            Activate();
        }

        @Override
        public boolean IsAtRest() {
            return current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return current.atRest;
        }

        @Override
        public void Activate() {
            // if the articulated figure was at rest
            if (current.atRest >= 0) {
                // normally gravity is added at the end of a simulation frame
                // if the figure was at rest add gravity here so it is applied this simulation frame
                AddGravity();
                // reset the active time for the max move time
                current.activateTime = 0.0f;
            }
            current.atRest = -1;
            current.noMoveTime = 0.0f;
            self.BecomeActive(TH_PHYSICS);
        }


        /*
         ================
         idPhysics_AF::PutToRest

         put to rest untill something collides with this physics object
         ================
         */
        @Override
        public void PutToRest() {
            Rest();
        }

        @Override
        public boolean IsPushable() {
            return (!noImpact && (masterBody == null || forcePushable));
        }

        @Override
        public void SaveState() {
            int i;

            saved = current;

            for (i = 0; i < bodies.Num(); i++) {
//                memcpy(bodies.oGet(i).saved, bodies.oGet(i).current, sizeof(AFBodyPState_t));
                bodies.oGet(i).saved = bodies.oGet(i).current;
            }
        }

        @Override
        public void RestoreState() {
            int i;

            current = saved;

            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).current = bodies.oGet(i).saved;
            }

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            if (masterBody != null) {
                Translate(masterBody.current.worldOrigin.oPlus(masterBody.current.worldAxis.oMultiply(newOrigin).oMinus(bodies.oGet(0).current.worldOrigin)));
            } else {
                Translate(newOrigin.oMinus(bodies.oGet(0).current.worldOrigin));
            }
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idMat3 axis;
            idRotation rotation;

            if (masterBody != null) {
                axis = bodies.oGet(0).current.worldAxis.Transpose().oMultiply(newAxis.oMultiply(masterBody.current.worldAxis));
            } else {
                axis = bodies.oGet(0).current.worldAxis.Transpose().oMultiply(newAxis);
            }
            rotation = axis.ToRotation();
            rotation.SetOrigin(bodies.oGet(0).current.worldOrigin);

            Rotate(rotation);
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
            int i;
            idAFBody body;

            if (!worldConstraintsLocked) {
                // translate constraints attached to the world
                for (i = 0; i < constraints.Num(); i++) {
                    constraints.oGet(i).Translate(translation);
                }
            }

            // translate all the bodies
            for (i = 0; i < bodies.Num(); i++) {

                body = bodies.oGet(i);
                body.current.worldOrigin.oPluSet(translation);
            }

            Activate();

            UpdateClipModels();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            int i;
            idAFBody body;

            if (!worldConstraintsLocked) {
                // rotate constraints attached to the world
                for (i = 0; i < constraints.Num(); i++) {
                    constraints.oGet(i).Rotate(rotation);
                }
            }

            // rotate all the bodies
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                body.current.worldOrigin.oMulSet(rotation);
                body.current.worldAxis.oMulSet(rotation.ToMat3());
            }

            Activate();

            UpdateClipModels();
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return getVec3_origin();
            } else {
                return bodies.oGet(id).current.worldOrigin;
            }
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return getMat3_identity();
            } else {
                return bodies.oGet(id).current.worldAxis;
            }
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return;
            }
            bodies.oGet(id).current.spatialVelocity.SubVec3_oSet(0, newLinearVelocity);
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return;
            }
            bodies.oGet(id).current.spatialVelocity.SubVec3_oSet(1, newAngularVelocity);
            Activate();
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return getVec3_origin();
            } else {
                return bodies.oGet(id).current.spatialVelocity.SubVec3(0);
            }
        }

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            if (id < 0 || id >= bodies.Num()) {
                return getVec3_origin();
            } else {
                return bodies.oGet(id).current.spatialVelocity.SubVec3(1);
            }
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
            int i;
            idAFBody body;
            trace_s[] bodyResults = {null};

            results[0].fraction = 1.0f;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.clipModel.IsTraceModel()) {
                    if (model != null) {
                        gameLocal.clip.TranslationModel(bodyResults, body.current.worldOrigin, body.current.worldOrigin.oPlus(translation),
                                body.clipModel, body.current.worldAxis, body.clipMask,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        gameLocal.clip.Translation(bodyResults, body.current.worldOrigin, body.current.worldOrigin.oPlus(translation),
                                body.clipModel, body.current.worldAxis, body.clipMask, self);
                    }
                    if (bodyResults[0].fraction < results[0].fraction) {
                        results[0].oSet(bodyResults[0]);
                    }
                }
            }

            results[0].endpos.oSet(bodies.oGet(0).current.worldOrigin.oPlus(translation.oMultiply(results[0].fraction)));
            results[0].endAxis.oSet(bodies.oGet(0).current.worldAxis);
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            int i;
            idAFBody body;
            trace_s[] bodyResults = {null};
            idRotation partialRotation;

            results[0].fraction = 1.0f;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.clipModel.IsTraceModel()) {
                    if (model != null) {
                        gameLocal.clip.RotationModel(bodyResults, body.current.worldOrigin, rotation,
                                body.clipModel, body.current.worldAxis, body.clipMask,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        gameLocal.clip.Rotation(bodyResults, body.current.worldOrigin, rotation,
                                body.clipModel, body.current.worldAxis, body.clipMask, self);
                    }
                    if (bodyResults[0].fraction < results[0].fraction) {
                        results[0] = bodyResults[0];
                    }
                }
            }

            partialRotation = rotation.oMultiply(results[0].fraction);
            results[0].endpos.oSet(bodies.oGet(0).current.worldOrigin.oMultiply(partialRotation));
            results[0].endAxis.oSet(bodies.oGet(0).current.worldAxis.oMultiply(partialRotation.ToMat3()));
        }

        @Override
        public int ClipContents(final idClipModel model) {
            int i, contents;
            idAFBody body;

            contents = 0;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.clipModel.IsTraceModel()) {
                    if (model != null) {
                        contents |= gameLocal.clip.ContentsModel(body.current.worldOrigin,
                                body.clipModel, body.current.worldAxis, -1,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        contents |= gameLocal.clip.Contents(body.current.worldOrigin,
                                body.clipModel, body.current.worldAxis, -1, null);
                    }
                }
            }

            return contents;
        }

        @Override
        public void DisableClip() {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            UpdateClipModels();
        }

        @Override
        public boolean EvaluateContacts() {
            int i, j, k, numContacts, numBodyContacts;
            idAFBody body;
            contactInfo_t[] contactInfo = new contactInfo_t[10];
            idEntity passEntity;
            idVecX dir = new idVecX(6, VECX_ALLOCA(6));

            // evaluate bodies
            EvaluateBodies(current.lastTimeStep);

            // remove all existing contacts
            ClearContacts();

            contactBodies.SetNum(0, false);

            if (!enableCollision) {
                return false;
            }

            // find all the contacts
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.clipMask == 0) {
                    continue;
                }

                passEntity = SetupCollisionForBody(body);

                body.InverseWorldSpatialInertiaMultiply(dir, body.current.externalForce.ToFloatPtr());
                dir.SubVec6(0).oSet(body.current.spatialVelocity.oPlus(dir.SubVec6(0).oMultiply(current.lastTimeStep)));
                dir.SubVec3(0).Normalize();
                dir.SubVec3(1).Normalize();

                numContacts = gameLocal.clip.Contacts(contactInfo, 10, body.current.worldOrigin, dir.SubVec6(0), 2.0f, //CONTACT_EPSILON,
                        body.clipModel, body.current.worldAxis, body.clipMask, passEntity);

                if (true) {
                    // merge nearby contacts between the same bodies
                    // and assure there are at most three planar contacts between any pair of bodies
                    for (j = 0; j < numContacts; j++) {

                        numBodyContacts = 0;
                        for (k = 0; k < contacts.Num(); k++) {
                            if (contacts.oGet(k).entityNum == contactInfo[j].entityNum) {
                                if ((contacts.oGet(k).id == i && contactInfo[j].id == contactBodies.oGet(k))
                                        || (contactBodies.oGet(k) == i && contacts.oGet(k).id == contactInfo[j].id)) {

                                    if ((contacts.oGet(k).point.oMinus(contactInfo[j].point)).LengthSqr() < Square(2.0f)) {
                                        break;
                                    }
                                    if (idMath.Fabs(contacts.oGet(k).normal.oMultiply(contactInfo[j].normal)) > 0.9f) {
                                        numBodyContacts++;
                                    }
                                }
                            }
                        }

                        if (k >= contacts.Num() && numBodyContacts < 3) {
                            contacts.Append(contactInfo[j]);
                            contactBodies.Append(i);
                        }
                    }

//}else{
//
//		for ( j = 0; j < numContacts; j++ ) {
//			contacts.Append( contactInfo[j] );
//			contactBodies.Append( i );
//		}
                }

            }

            AddContactEntitiesForContacts();

            return (contacts.Num() != 0);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idAFBody body;
            idRotation rotation;

            if (bodies.Num() != 0) {
                body = bodies.oGet(0);
                rotation = (body.saved.worldAxis.Transpose().oMultiply(body.current.worldAxis).ToRotation());

                // velocity with which the af is pushed
                current.pushVelocity.SubVec3_oPluSet(0, (body.current.worldOrigin.oMinus(body.saved.worldOrigin)).oDivide(deltaTime * idMath.M_MS2SEC));
                current.pushVelocity.SubVec3_oPluSet(1, rotation.GetVec().oMultiply((float) -DEG2RAD(rotation.GetAngle())).oDivide(deltaTime * idMath.M_MS2SEC));
            }
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return current.pushVelocity.SubVec3(0);
        }

        @Override
        public idVec3 GetPushedAngularVelocity(final int id /*= 0*/) {
            return current.pushVelocity.SubVec3(1);
        }

        /*
         ================
         idPhysics_AF::SetMaster

         the binding is orientated based on the constraints being used
         ================
         */
        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            int i;
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();
            idRotation rotation;

            if (master != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                if (null == masterBody) {
                    masterBody = new idAFBody();
                    // translate and rotate all the constraints with body2 == NULL from world space to master space
                    rotation = masterAxis.Transpose().ToRotation();
                    for (i = 0; i < constraints.Num(); i++) {
                        if (constraints.oGet(i).GetBody2() == null) {
                            constraints.oGet(i).Translate(masterOrigin.oNegative());
                            constraints.oGet(i).Rotate(rotation);
                        }
                    }
                    Activate();
                }
                masterBody.current.worldOrigin.oSet(masterOrigin);
                masterBody.current.worldAxis.oSet(masterAxis);
            } else if (masterBody != null) {
                // translate and rotate all the constraints with body2 == NULL from master space to world space
                rotation = masterBody.current.worldAxis.ToRotation();
                for (i = 0; i < constraints.Num(); i++) {
                    if (constraints.oGet(i).GetBody2() == null) {
                        constraints.oGet(i).Rotate(rotation);
                        constraints.oGet(i).Translate(masterBody.current.worldOrigin);
                    }
                }
//			delete masterBody;
                masterBody = null;
                Activate();
            }
        }

        static final float AF_VELOCITY_MAX           = 16000;
        static final int   AF_VELOCITY_TOTAL_BITS    = 16;
        static final int   AF_VELOCITY_EXPONENT_BITS = idMath.BitsForInteger(idMath.BitsForFloat(AF_VELOCITY_MAX)) + 1;
        static final int   AF_VELOCITY_MANTISSA_BITS = AF_VELOCITY_TOTAL_BITS - 1 - AF_VELOCITY_EXPONENT_BITS;
        static final float AF_FORCE_MAX              = 1e20f;
        static final int   AF_FORCE_TOTAL_BITS       = 16;
        static final int   AF_FORCE_EXPONENT_BITS    = idMath.BitsForInteger(idMath.BitsForFloat(AF_FORCE_MAX)) + 1;
        static final int   AF_FORCE_MANTISSA_BITS    = AF_FORCE_TOTAL_BITS - 1 - AF_FORCE_EXPONENT_BITS;

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            int i;
            idCQuat quat;

            msg.WriteLong(current.atRest);
            msg.WriteFloat(current.noMoveTime);
            msg.WriteFloat(current.activateTime);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(0), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(1), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(2), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(3), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(4), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(5), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);

            msg.WriteByte(bodies.Num());

            for (i = 0; i < bodies.Num(); i++) {
                AFBodyPState_s state = bodies.oGet(i).current;
                quat = state.worldAxis.ToCQuat();

                msg.WriteFloat(state.worldOrigin.oGet(0));
                msg.WriteFloat(state.worldOrigin.oGet(1));
                msg.WriteFloat(state.worldOrigin.oGet(2));
                msg.WriteFloat(quat.x);
                msg.WriteFloat(quat.y);
                msg.WriteFloat(quat.z);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(0), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(1), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(2), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(3), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(4), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                msg.WriteDeltaFloat(0.0f, state.spatialVelocity.oGet(5), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
                /*		msg.WriteDeltaFloat( 0.0f, state.externalForce[0], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 msg.WriteDeltaFloat( 0.0f, state.externalForce[1], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 msg.WriteDeltaFloat( 0.0f, state.externalForce[2], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 msg.WriteDeltaFloat( 0.0f, state.externalForce[3], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 msg.WriteDeltaFloat( 0.0f, state.externalForce[4], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 msg.WriteDeltaFloat( 0.0f, state.externalForce[5], AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 */
            }
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int i, num;
            idCQuat quat = new idCQuat();

            current.atRest = msg.ReadLong();
            current.noMoveTime = msg.ReadFloat();
            current.activateTime = msg.ReadFloat();
            current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(3, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(4, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(5, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));

            num = msg.ReadByte();
            assert (num == bodies.Num());

            for (i = 0; i < bodies.Num(); i++) {
                AFBodyPState_s state = bodies.oGet(i).current;

                state.worldOrigin.oSet(0, msg.ReadFloat());
                state.worldOrigin.oSet(1, msg.ReadFloat());
                state.worldOrigin.oSet(2, msg.ReadFloat());
                quat.x = msg.ReadFloat();
                quat.y = msg.ReadFloat();
                quat.z = msg.ReadFloat();
                state.spatialVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                state.spatialVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                state.spatialVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                state.spatialVelocity.oSet(3, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                state.spatialVelocity.oSet(4, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                state.spatialVelocity.oSet(5, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
                /*		state.externalForce[0] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 state.externalForce[1] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 state.externalForce[2] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 state.externalForce[3] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 state.externalForce[4] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 state.externalForce[5] = msg.ReadDeltaFloat( 0.0f, AF_FORCE_EXPONENT_BITS, AF_FORCE_MANTISSA_BITS );
                 */
                state.worldAxis.oSet(quat.ToMat3());
            }

            UpdateClipModels();
        }

        private void BuildTrees() {
            int i;
            float scale;
            idAFBody b;
            idAFConstraint c;
            idAFTree tree;

            primaryConstraints.Clear();
            auxiliaryConstraints.Clear();
            trees.DeleteContents(true);

            totalMass = 0.0f;
            for (i = 0; i < bodies.Num(); i++) {
                b = bodies.oGet(i);
                b.parent = null;
                b.primaryConstraint = null;
                b.constraints.SetNum(0, false);
                b.children.Clear();
                b.tree = null;
                totalMass += b.mass;
            }

            if (forceTotalMass > 0.0f) {
                scale = forceTotalMass / totalMass;
                for (i = 0; i < bodies.Num(); i++) {
                    b = bodies.oGet(i);
                    b.mass *= scale;
                    b.invMass = 1.0f / b.mass;
                    b.inertiaTensor.oMulSet(scale);
                    b.inverseInertiaTensor.oSet(b.inertiaTensor.Inverse());
                }
                totalMass = forceTotalMass;
            }

            if (af_useLinearTime.GetBool()) {

                for (i = 0; i < constraints.Num(); i++) {
                    c = constraints.oGet(i);

                    c.body1.constraints.Append(c);
                    if (c.body2 != null) {
                        c.body2.constraints.Append(c);
                    }

                    // only bilateral constraints between two non-world bodies that do not
                    // create loops can be used as primary constraints
                    if (null == c.body1.primaryConstraint && c.fl.allowPrimary && c.body2 != null && !IsClosedLoop(c.body1, c.body2)) {
                        c.body1.primaryConstraint = c;
                        c.body1.parent = c.body2;
                        c.body2.children.Append(c.body1);
                        c.fl.isPrimary = true;
                        c.firstIndex = 0;
                        primaryConstraints.Append(c);
                    } else {
                        c.fl.isPrimary = false;
                        auxiliaryConstraints.Append(c);
                    }
                }

                // create trees for all parent bodies
                for (i = 0; i < bodies.Num(); i++) {
                    if (null == bodies.oGet(i).parent) {
                        tree = new idAFTree();
                        tree.sortedBodies.Clear();
                        tree.sortedBodies.Append(bodies.oGet(i));
                        bodies.oGet(i).tree = tree;
                        trees.Append(tree);
                    }
                }

                // add each child body to the appropriate tree
                for (i = 0; i < bodies.Num(); i++) {
                    if (bodies.oGet(i).parent != null) {
                        for (b = bodies.oGet(i).parent; null == b.tree; b = b.parent) {
                        }
                        b.tree.sortedBodies.Append(bodies.oGet(i));
                        bodies.oGet(i).tree = b.tree;
                    }
                }

                if (trees.Num() > 1) {
                    gameLocal.Warning("Articulated figure has multiple seperate tree structures for entity '%s' type '%s'.",
                            self.name, self.GetType().getName());
                }

                // sort bodies in each tree to make sure parents come first
                for (i = 0; i < trees.Num(); i++) {
                    trees.oGet(i).SortBodies();
                }

            } else {

                // create a tree for each body
                for (i = 0; i < bodies.Num(); i++) {
                    tree = new idAFTree();
                    tree.sortedBodies.Clear();
                    tree.sortedBodies.Append(bodies.oGet(i));
                    bodies.oGet(i).tree = tree;
                    trees.Append(tree);
                }

                for (i = 0; i < constraints.Num(); i++) {
                    c = constraints.oGet(i);

                    c.body1.constraints.Append(c);
                    if (c.body2 != null) {
                        c.body2.constraints.Append(c);
                    }

                    c.fl.isPrimary = false;
                    auxiliaryConstraints.Append(c);
                }
            }
        }

        private boolean IsClosedLoop(final idAFBody body1, final idAFBody body2) {
            idAFBody b1, b2;

            for (b1 = body1; b1.parent != null; b1 = b1.parent) {
            }
            for (b2 = body2; b2.parent != null; b2 = b2.parent) {
            }
            return (b1.equals(b2));
        }

        private void PrimaryFactor() {
            int i;

            for (i = 0; i < trees.Num(); i++) {
                trees.oGet(i).Factor();
            }
        }

        private void EvaluateBodies(float timeStep) {
            int i;
            idAFBody body;
            idMat3 axis;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                // we transpose the axis before using it because idMat3 is column-major
                axis = body.current.worldAxis.Transpose();

                // if the center of mass is at the body point of reference
                if (body.centerOfMass.Compare(getVec3_origin(), CENTER_OF_MASS_EPSILON)) {

                    // spatial inertia in world space
                    body.I.Set(getMat3_identity().oMultiply(body.mass), getMat3_zero(), getMat3_zero(), axis.oMultiply(body.inertiaTensor).oMultiply(axis.Transpose()));

                    // inverse spatial inertia in world space
                    body.inverseWorldSpatialInertia.Set(getMat3_identity().oMultiply(body.invMass), getMat3_zero(), getMat3_zero(), axis.oMultiply(body.inverseInertiaTensor).oMultiply(axis.Transpose()));

                    body.fl.spatialInertiaSparse = true;
                } else {
                    idMat3 massMoment = SkewSymmetric(body.centerOfMass).oMultiply(body.mass);

                    // spatial inertia in world space
                    body.I.Set(getMat3_identity().oMultiply(body.mass), massMoment,
                            massMoment.Transpose(), axis.oMultiply(body.inertiaTensor).oMultiply(axis.Transpose()));

                    // inverse spatial inertia in world space
                    body.inverseWorldSpatialInertia.oSet(body.I.InverseFast());

                    body.fl.spatialInertiaSparse = false;
                }

                // initialize auxiliary constraint force to zero
                body.auxForce.Zero();
            }
        }

        private void EvaluateConstraints(float timeStep) {
            int i;
            float invTimeStep;
            idAFBody body;
            idAFConstraint c;

            invTimeStep = 1.0f / timeStep;

            // setup the constraint equations for the current position and orientation of the bodies
            for (i = 0; i < primaryConstraints.Num(); i++) {
                c = primaryConstraints.oGet(i);
                c.Evaluate(invTimeStep);
                c.J = c.J2;
            }
            for (i = 0; i < auxiliaryConstraints.Num(); i++) {
                auxiliaryConstraints.oGet(i).Evaluate(invTimeStep);
            }

            // add contact constraints to the list with frame constraints
            for (i = 0; i < contactConstraints.Num(); i++) {
                AddFrameConstraint(contactConstraints.oGet(i));
            }

            // setup body primary constraint matrix
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.primaryConstraint != null) {
                    body.J.oSet(body.primaryConstraint.J1.Transpose());
                }
            }
        }

        private void AddFrameConstraints() {
            int i;

            // add frame constraints to auxiliary constraints
            for (i = 0; i < frameConstraints.Num(); i++) {
                auxiliaryConstraints.Append(frameConstraints.oGet(i));
            }
        }

        private void RemoveFrameConstraints() {
            // remove all the frame constraints from the auxiliary constraints
            auxiliaryConstraints.SetNum(auxiliaryConstraints.Num() - frameConstraints.Num(), false);
            frameConstraints.SetNum(0, false);
        }

        private void ApplyFriction(float timeStep, float endTimeMSec) {
            int i;
            float invTimeStep;

            if (af_skipFriction.GetBool()) {
                return;
            }

            if (jointFrictionDentStart < MS2SEC(endTimeMSec) && jointFrictionDentEnd > MS2SEC(endTimeMSec)) {
                float halfTime = (jointFrictionDentEnd - jointFrictionDentStart) * 0.5f;
                if (jointFrictionDentStart + halfTime > MS2SEC(endTimeMSec)) {
                    jointFrictionDentScale = 1.0f - (1.0f - jointFrictionDent) * (MS2SEC(endTimeMSec) - jointFrictionDentStart) / halfTime;
                } else {
                    jointFrictionDentScale = jointFrictionDent + (1.0f - jointFrictionDent) * (MS2SEC(endTimeMSec) - jointFrictionDentStart - halfTime) / halfTime;
                }
            } else {
                jointFrictionDentScale = 0.0f;
            }

            if (contactFrictionDentStart < MS2SEC(endTimeMSec) && contactFrictionDentEnd > MS2SEC(endTimeMSec)) {
                float halfTime = (contactFrictionDentEnd - contactFrictionDentStart) * 0.5f;
                if (contactFrictionDentStart + halfTime > MS2SEC(endTimeMSec)) {
                    contactFrictionDentScale = 1.0f - (1.0f - contactFrictionDent) * (MS2SEC(endTimeMSec) - contactFrictionDentStart) / halfTime;
                } else {
                    contactFrictionDentScale = contactFrictionDent + (1.0f - contactFrictionDent) * (MS2SEC(endTimeMSec) - contactFrictionDentStart - halfTime) / halfTime;
                }
            } else {
                contactFrictionDentScale = 0.0f;
            }

            invTimeStep = 1.0f / timeStep;

            for (i = 0; i < primaryConstraints.Num(); i++) {
                primaryConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
            for (i = 0; i < auxiliaryConstraints.Num(); i++) {
                auxiliaryConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
            for (i = 0; i < frameConstraints.Num(); i++) {
                frameConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
        }

        private void PrimaryForces(float timeStep) {
            int i;

            for (i = 0; i < trees.Num(); i++) {
                trees.oGet(i).CalculateForces(timeStep);
            }
        }

        private void AuxiliaryForces(float timeStep) {
            int i, j, k, l, n, m, s, numAuxConstraints;
            int[] index, boxIndex;
            float[] ptr, j1, j2, dstPtr, forcePtr;
            float invStep, u;
            idAFBody body;
            idAFConstraint constraint;
            idVecX tmp = new idVecX();
            idMatX jmk = new idMatX();
            idVecX rhs = new idVecX(), w = new idVecX(), lm = new idVecX(), lo = new idVecX(), hi = new idVecX();
            int p_i, d_i;

            // get the number of one dimensional auxiliary constraints
            for (numAuxConstraints = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                numAuxConstraints += auxiliaryConstraints.oGet(i).J1.GetNumRows();
            }

            if (numAuxConstraints == 0) {
                return;
            }

            // allocate memory to store the body response to auxiliary constraint forces
//            forcePtr = new float[bodies.Num() * numAuxConstraints * 8];
//            index = new int[bodies.Num() * numAuxConstraints];
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);
                body.response = new float[numAuxConstraints * 8];
                body.responseIndex = new int[numAuxConstraints];
                body.numResponses = 0;
                body.maxAuxiliaryIndex = 0;
//                forcePtr += numAuxConstraints * 8;
//                index += numAuxConstraints;
            }

            // set on each body the largest index of an auxiliary constraint constraining the body
            if (af_useSymmetry.GetBool()) {
                for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                    constraint = auxiliaryConstraints.oGet(i);
                    for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {
                        if (k > constraint.body1.maxAuxiliaryIndex) {
                            constraint.body1.maxAuxiliaryIndex = k;
                        }
                        if (constraint.body2 != null && k > constraint.body2.maxAuxiliaryIndex) {
                            constraint.body2.maxAuxiliaryIndex = k;
                        }
                    }
                }
                for (i = 0; i < trees.Num(); i++) {
                    trees.oGet(i).SetMaxSubTreeAuxiliaryIndex();
                }
            }

            // calculate forces of primary constraints in response to the auxiliary constraint forces
            for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                constraint = auxiliaryConstraints.oGet(i);

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {

                    // calculate body forces in the tree in response to the constraint force
                    constraint.body1.tree.Response(constraint, j, k);
                    // if there is a second body which is part of a different tree
                    if (constraint.body2 != null && constraint.body2.tree != constraint.body1.tree) {
                        // calculate body forces in the second tree in response to the constraint force
                        constraint.body2.tree.Response(constraint, j, k);
                    }
                }
            }

            // NOTE: the rows are 16 byte padded
            jmk.SetData(numAuxConstraints, ((numAuxConstraints + 3) & ~3), MATX_ALLOCA(numAuxConstraints * ((numAuxConstraints + 3) & ~3)));
            tmp.SetData(6, VECX_ALLOCA(6));

            // create constraint matrix for auxiliary constraints using a mass matrix adjusted for the primary constraints
            for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                constraint = auxiliaryConstraints.oGet(i);

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {
                    constraint.body1.InverseWorldSpatialInertiaMultiply(tmp, constraint.J1.oGet(j));
                    j1 = tmp.ToFloatPtr();
                    ptr = constraint.body1.response;
                    index = constraint.body1.responseIndex;
                    dstPtr = jmk.oGet(k);
                    s = af_useSymmetry.GetBool() ? k + 1 : numAuxConstraints;
                    for (l = n = p_i = 0, m = index[0]; n < constraint.body1.numResponses && m < s; n++) {
                        m = index[n];
                        while (l < m) {
                            dstPtr[l++] = 0.0f;
                        }
                        dstPtr[l++] = j1[0] * ptr[p_i + 0] + j1[1] * ptr[p_i + 1] + j1[2] * ptr[p_i + 2]
                                + j1[3] * ptr[p_i + 3] + j1[4] * ptr[p_i + 4] + j1[5] * ptr[p_i + 5];
                        p_i += 8;
                    }

                    while (l < s) {
                        dstPtr[l++] = 0.0f;
                    }

                    if (constraint.body2 != null) {
                        constraint.body2.InverseWorldSpatialInertiaMultiply(tmp, constraint.J2.oGet(j));
                        j2 = tmp.ToFloatPtr();
                        ptr = constraint.body2.response;
                        index = constraint.body2.responseIndex;
                        for (n = p_i = 0, m = index[0]; n < constraint.body2.numResponses && m < s; n++) {
                            m = index[n];
                            dstPtr[m] += j2[0] * ptr[p_i + 0] + j2[1] * ptr[p_i + 1] + j2[2] * ptr[p_i + 2]
                                    + j2[3] * ptr[p_i + 3] + j2[4] * ptr[p_i + 4] + j2[5] * ptr[p_i + 5];
                            p_i += 8;
                        }
                    }
                }
            }

            if (af_useSymmetry.GetBool()) {
                n = jmk.GetNumColumns();
                for (i = 0; i < numAuxConstraints; i++) {
                    ptr = jmk.ToFloatPtr();
                    p_i = (i + 1) * n + i;
                    dstPtr = jmk.ToFloatPtr();
                    d_i = i * n + i + 1;
                    for (j = i + 1; j < numAuxConstraints; j++) {
                        dstPtr[d_i++] = ptr[p_i];//TODO:
                        p_i += n;
                    }
                }
            }

            invStep = 1.0f / timeStep;

            // calculate body acceleration
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);
                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.acceleration.SubVec6(0).oPluSet(body.current.spatialVelocity.oMultiply(invStep));
            }

            rhs.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            lo.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            hi.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            lm.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            boxIndex = new int[numAuxConstraints];

            // set first index for special box constrained variables
            for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                auxiliaryConstraints.oGet(i).firstIndex = k;
                k += auxiliaryConstraints.oGet(i).J1.GetNumRows();
            }

            // initialize right hand side and low and high bounds for auxiliary constraints
            for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                constraint = auxiliaryConstraints.oGet(i);
                n = k;

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {

                    j1 = constraint.J1.oGet(j);
                    ptr = constraint.body1.acceleration.ToFloatPtr();
                    rhs.p[k] = j1[0] * ptr[0] + j1[1] * ptr[1] + j1[2] * ptr[2] + j1[3] * ptr[3] + j1[4] * ptr[4] + j1[5] * ptr[5];
                    rhs.p[k] += constraint.c1.p[j] * invStep;

                    if (constraint.body2 != null) {
                        j2 = constraint.J2.oGet(j);
                        ptr = constraint.body2.acceleration.ToFloatPtr();
                        rhs.p[k] += j2[0] * ptr[0] + j2[1] * ptr[1] + j2[2] * ptr[2] + j2[3] * ptr[3] + j2[4] * ptr[4] + j2[5] * ptr[5];
                        rhs.p[k] += constraint.c2.p[j] * invStep;
                    }

                    rhs.oSet(k, -rhs.oGet(k));
                    lo.p[k] = constraint.lo.p[j];
                    hi.p[k] = constraint.hi.p[j];

                    if (constraint.boxIndex[j] >= 0) {
                        if (constraint.boxConstraint.fl.isPrimary) {
                            gameLocal.Error("cannot reference primary constraints for the box index");
                        }
                        boxIndex[k] = constraint.boxConstraint.firstIndex + constraint.boxIndex[j];
                    } else {
                        boxIndex[k] = -1;
                    }
                    jmk.oPluSet(k, k, constraint.e.p[j] * invStep);
                }
            }

            if (AF_TIMINGS) {
                timer_lcp.Start();
            }

            // calculate lagrange multipliers for auxiliary constraints
            if (!lcp.Solve(jmk, lm, rhs, lo, hi, boxIndex)) {
                return;        // bad monkey!
            }

            if (AF_TIMINGS) {
                timer_lcp.Stop();
            }

            // calculate auxiliary constraint forces
            for (k = 0, i = 0; i < auxiliaryConstraints.Num(); i++) {
                constraint = auxiliaryConstraints.oGet(i);

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {
                    constraint.lm.p[j] = u = lm.oGet(k);

                    j1 = constraint.J1.oGet(j);
                    ptr = constraint.body1.auxForce.ToFloatPtr();
                    ptr[0] += j1[0] * u;
                    ptr[1] += j1[1] * u;
                    ptr[2] += j1[2] * u;
                    ptr[3] += j1[3] * u;
                    ptr[4] += j1[4] * u;
                    ptr[5] += j1[5] * u;

                    if (constraint.body2 != null) {
                        j2 = constraint.J2.oGet(j);
                        ptr = constraint.body2.auxForce.ToFloatPtr();
                        ptr[0] += j2[0] * u;
                        ptr[1] += j2[1] * u;
                        ptr[2] += j2[2] * u;
                        ptr[3] += j2[3] * u;
                        ptr[4] += j2[4] * u;
                        ptr[5] += j2[5] * u;
                    }
                }
            }

            // recalculate primary constraint forces in response to auxiliary constraint forces
            PrimaryForces(timeStep);

            // clear pointers pointing to stack space so tools don't get confused
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);
                body.response = null;
                body.responseIndex = null;
            }
        }

        private void VerifyContactConstraints() {
// if (false){
            // int i;
            // float impulseNumerator, impulseDenominator;
            // idVec3 r, velocity, normalVelocity, normal, impulse;
            // idAFBody *body;

            // for ( i = 0; i < contactConstraints.Num(); i++ ) {
            // body = contactConstraints[i].body1;
            // const contactInfo_t &contact = contactConstraints[i].GetContact();
            // r = contact.point - body.GetCenterOfMass();
            // // calculate velocity at contact point
            // velocity = body.GetLinearVelocity() + body.GetAngularVelocity().Cross( r );
            // // velocity along normal vector
            // normalVelocity = ( velocity * contact.normal ) * contact.normal;
            // // if moving towards the surface at the contact point
            // if ( normalVelocity * contact.normal < 0.0f ) {
            // // calculate impulse
            // normal = -normalVelocity;
            // impulseNumerator = normal.Normalize();
            // impulseDenominator = body.GetInverseMass() + ( ( body.GetInverseWorldInertia() * r.Cross( normal ) ).Cross( r ) * normal );
            // impulse = (impulseNumerator / impulseDenominator) * normal * 1.0001f;
            // // apply impulse
            // body.SetLinearVelocity( body.GetLinearVelocity() + impulse );
            // body.SetAngularVelocity( body.GetAngularVelocity() + r.Cross( impulse ) );
            // }
            // }
// }else{
            int i;
            idAFBody body;
            idVec3 normal;

            for (i = 0; i < contactConstraints.Num(); i++) {
                body = contactConstraints.oGet(i).body1;
                normal = contactConstraints.oGet(i).GetContact().normal;
                final float v = normal.oMultiply(body.next.spatialVelocity.SubVec3(0));
                if (v <= 0.0f) {
                    body.next.spatialVelocity.SubVec3_oMinSet(0, normal.oMultiply(1.0001f * v));
                }
                body = contactConstraints.oGet(i).body2;
                if (null == body) {
                    continue;
                }
                normal = normal.oNegative();
                if (v <= 0.0f) {
                    body.next.spatialVelocity.SubVec3_oMinSet(0, normal.oMultiply(1.0001f * v));
                }
            }
// }
        }

        private void SetupContactConstraints() {
            int i;

            // make sure enough contact constraints are allocated
            contactConstraints.AssureSizeAlloc(contacts.Num(), idAFConstraint_Contact.class);
            contactConstraints.SetNum(contacts.Num(), false);

            // setup contact constraints
            for (i = 0; i < contacts.Num(); i++) {
                // add contact constraint
                contactConstraints.oGet(i).physics = this;
                if (contacts.oGet(i).entityNum == self.entityNumber) {
                    contactConstraints.oGet(i).Setup(bodies.oGet(contactBodies.oGet(i)), bodies.oGet(contacts.oGet(i).id), contacts.oGet(i));
                } else {
                    contactConstraints.oGet(i).Setup(bodies.oGet(contactBodies.oGet(i)), null, contacts.oGet(i));
                }
            }
        }

        private void ApplyContactForces() {
//#if 0
//	int i;
//	idEntity *ent;
//	idVec3 force;
//
//	for ( i = 0; i < contactConstraints.Num(); i++ ) {
//		if ( contactConstraints[i]->body2 != NULL ) {
//			continue;
//		}
//		const contactInfo_t &contact = contactConstraints[i]->GetContact();
//		ent = gameLocal.entities[contact.entityNum];
//		if ( !ent ) {
//			continue;
//		}
//		force.Zero();
//		ent->AddForce( self, contact.id, contact.point, force );
//	}
//#endif
        }

        private void Evolve(float timeStep) {
            int i;
            float angle;
            idVec3 vec;
            idAFBody body;
            idVec6 force;
            idRotation rotation;
            float vSqr, maxLinearVelocity, maxAngularVelocity;

            maxLinearVelocity = af_maxLinearVelocity.GetFloat() / timeStep;
            maxAngularVelocity = af_maxAngularVelocity.GetFloat() / timeStep;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                // calculate the spatial velocity for the next physics state
                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.next.spatialVelocity.oSet(body.current.spatialVelocity.oPlus(body.acceleration.SubVec6(0).oMultiply(timeStep)));

                if (maxLinearVelocity > 0.0f) {
                    // cap the linear velocity
                    vSqr = body.next.spatialVelocity.SubVec3(0).LengthSqr();
                    if (vSqr > Square(maxLinearVelocity)) {
                        body.next.spatialVelocity.SubVec3_oMulSet(0, idMath.InvSqrt(vSqr) * maxLinearVelocity);
                    }
                }

                if (maxAngularVelocity > 0.0f) {
                    // cap the angular velocity
                    vSqr = body.next.spatialVelocity.SubVec3(1).LengthSqr();
                    if (vSqr > Square(maxAngularVelocity)) {
                        body.next.spatialVelocity.SubVec3_oMulSet(1, idMath.InvSqrt(vSqr) * maxAngularVelocity);
                    }
                }
            }

            // make absolutely sure all contact constraints are satisfied
            VerifyContactConstraints();

            // calculate the position of the bodies for the next physics state
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                // translate world origin
                body.next.worldOrigin.oSet(body.current.worldOrigin.oPlus(body.next.spatialVelocity.SubVec3(0).oMultiply(timeStep)));

                // convert angular velocity to a rotation matrix
                vec = body.next.spatialVelocity.SubVec3(1);
                angle = -timeStep * (float) RAD2DEG(vec.Normalize());
                rotation = new idRotation(getVec3_origin(), vec, angle);
                rotation.Normalize180();

                // rotate world axis
                body.next.worldAxis.oSet(body.current.worldAxis.oMultiply(rotation.ToMat3()));
                body.next.worldAxis.OrthoNormalizeSelf();

                // linear and angular friction
                body.next.spatialVelocity.SubVec3_oMinSet(0, body.next.spatialVelocity.SubVec3(0).oMultiply(body.linearFriction));
                body.next.spatialVelocity.SubVec3_oMinSet(1, body.next.spatialVelocity.SubVec3(1).oMultiply(body.angularFriction));
            }
        }

        private idEntity SetupCollisionForBody(idAFBody body) {
            int i;
            idAFBody b;
            idEntity passEntity;

            passEntity = null;

            if (!selfCollision || !body.fl.selfCollision || af_skipSelfCollision.GetBool()) {

                // disable all bodies
                for (i = 0; i < bodies.Num(); i++) {
                    bodies.oGet(i).clipModel.Disable();
                }

                // don't collide with world collision model if attached to the world
                for (i = 0; i < body.constraints.Num(); i++) {
                    if (!body.constraints.oGet(i).fl.noCollision) {
                        continue;
                    }
                    // if this constraint attaches the body to the world
                    if (body.constraints.oGet(i).body2 == null) {
                        // don't collide with the world collision model
                        passEntity = gameLocal.world;
                    }
                }

            } else {

                // enable all bodies that have self collision
                for (i = 0; i < bodies.Num(); i++) {
                    if (bodies.oGet(i).fl.selfCollision) {
                        bodies.oGet(i).clipModel.Enable();
                    } else {
                        bodies.oGet(i).clipModel.Disable();
                    }
                }

                // don't let the body collide with itself
                body.clipModel.Disable();

                // disable any bodies attached with constraints
                for (i = 0; i < body.constraints.Num(); i++) {
                    if (!body.constraints.oGet(i).fl.noCollision) {
                        continue;
                    }
                    // if this constraint attaches the body to the world
                    if (body.constraints.oGet(i).body2 == null) {
                        // don't collide with the world collision model
                        passEntity = gameLocal.world;
                    } else {
                        if (body.constraints.oGet(i).body1.equals(body)) {
                            b = body.constraints.oGet(i).body2;
                        } else if (body.constraints.oGet(i).body2.equals(body)) {
                            b = body.constraints.oGet(i).body1;
                        } else {
                            continue;
                        }
                        // don't collide with this body
                        b.clipModel.Disable();
                    }
                }
            }

            return passEntity;
        }

        /*
         ================
         idPhysics_AF::CollisionImpulse

         apply impulse to the colliding bodies
         the current state of the body should be set to the moment of impact
         this is silly as it doesn't take the AF structure into account
         ================
         */
        private boolean CollisionImpulse(float timeStep, idAFBody body, trace_s collision) {
            idVec3 r, velocity, impulse;
            idMat3 inverseWorldInertiaTensor;
            float impulseNumerator, impulseDenominator;
            impactInfo_s info = new impactInfo_s();
            idEntity ent;

            ent = gameLocal.entities[collision.c.entityNum];
            if (ent == self) {
                return false;
            }

            // get info from other entity involved
            ent.GetImpactInfo(self, collision.c.id, collision.c.point, info);
            // collision point relative to the body center of mass
            r = collision.c.point.oMinus(body.current.worldOrigin.oPlus(body.centerOfMass.oMultiply(body.current.worldAxis)));
            // the velocity at the collision point
            velocity = body.current.spatialVelocity.SubVec3_oPluSet(0, body.current.spatialVelocity.SubVec3(1).Cross(r));
            // subtract velocity of other entity
            velocity.oMinSet(info.velocity);
            // never stick
            if (velocity.oMultiply(collision.c.normal) > 0.0f) {
                velocity = collision.c.normal;
            }
            inverseWorldInertiaTensor = body.current.worldAxis.Transpose().oMultiply(body.inverseInertiaTensor).oMultiply(body.current.worldAxis);
            impulseNumerator = -(1.0f + body.bouncyness) * (velocity.oMultiply(collision.c.normal));
            impulseDenominator = body.invMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(collision.c.normal)).Cross(r).oMultiply(collision.c.normal)));
            if (info.invMass != 0) {
                impulseDenominator += info.invMass + ((info.invInertiaTensor.oMultiply(info.position.Cross(collision.c.normal)).Cross(info.position).oMultiply(collision.c.normal)));
            }
            impulse = collision.c.normal.oMultiply((impulseNumerator / impulseDenominator));

            // apply impact to other entity
            ent.ApplyImpulse(self, collision.c.id, collision.c.point, impulse.oNegative());

            // callback to self to let the entity know about the impact
            return self.Collide(collision, velocity);
        }

        private boolean ApplyCollisions(float timeStep) {
            int i;

            for (i = 0; i < collisions.Num(); i++) {
                if (CollisionImpulse(timeStep, collisions.oGet(i).body, collisions.oGet(i).trace)) {
                    return true;
                }
            }
            return false;
        }

        /*
         ================
         idPhysics_AF::CheckForCollisions

         check for collisions between the current and next state
         if there is a collision the next state is set to the state at the moment of impact
         assumes all bodies are linked for collision detection and relinks all bodies after moving them
         ================
         */
        private void CheckForCollisions(float timeStep) {
//	#define TEST_COLLISION_DETECTION
            int i, index;
            idAFBody body;
            idMat3 axis = new idMat3();
            idRotation rotation;
            trace_s[] collision = {new trace_s()};
            idEntity passEntity;
            boolean startSolid = false;

            // clear list with collisions
            collisions.SetNum(0, false);

            if (!enableCollision) {
                return;
            }

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.clipMask != 0) {

                    passEntity = SetupCollisionForBody(body);

                    if (TEST_COLLISION_DETECTION) {
                        if (gameLocal.clip.Contents(body.current.worldOrigin, body.clipModel,
                                body.current.worldAxis, body.clipMask, passEntity) != 0) {
                            startSolid = true;
                        }
                    }

                    TransposeMultiply(body.current.worldAxis, body.next.worldAxis, axis);
                    rotation = axis.ToRotation();
                    rotation.SetOrigin(body.current.worldOrigin);

                    // if there was a collision
                    if (gameLocal.clip.Motion(collision, body.current.worldOrigin, body.next.worldOrigin, rotation,
                            body.clipModel, body.current.worldAxis, body.clipMask, passEntity)) {

                        // set the next state to the state at the moment of impact
                        body.next.worldOrigin.oSet(collision[0].endpos);
                        body.next.worldAxis.oSet(collision[0].endAxis);

                        // add collision to the list
                        index = collisions.Num();
                        collisions.SetNum(index + 1, false);
                        collisions.oGet(index).trace = collision[0];
                        collisions.oGet(index).body = body;
                    }

                    if (TEST_COLLISION_DETECTION) {
                        if (gameLocal.clip.Contents(body.next.worldOrigin, body.clipModel,
                                body.next.worldAxis, body.clipMask, passEntity) != 0) {
                            if (!startSolid) {
                                int bah = 1;
                            }
                        }
                    }
                }

                body.clipModel.Link(gameLocal.clip, self, body.clipModel.GetId(), body.next.worldOrigin, body.next.worldAxis);
            }
        }

        private void ClearExternalForce() {
            int i;
            idAFBody body;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                // clear external force
                body.current.externalForce.Zero();
                body.next.externalForce.Zero();
            }
        }

        private void AddGravity() {
            int i;
            idAFBody body;

            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);
                // add gravitational force
                body.current.externalForce.SubVec3_oPluSet(0, gravityVector.oMultiply(body.mass));
            }
        }

        private void SwapStates() {
            int i;
            idAFBody body;
            AFBodyPState_s swap;

            for (i = 0; i < bodies.Num(); i++) {

                body = bodies.oGet(i);

                // swap the current and next state for next simulation step
                swap = body.current;
                body.current = body.next;
                body.next = swap;
            }
        }

        private boolean TestIfAtRest(float timeStep) {
            int i;
            float translationSqr, maxTranslationSqr, rotation, maxRotation;
            idAFBody body;

            if (current.atRest >= 0) {
                return true;
            }

            current.activateTime += timeStep;

            // if the simulation should never be suspended before a certaint amount of time passed
            if (minMoveTime > 0.0f && current.activateTime < minMoveTime) {
                return false;
            }

            // if the simulation should always be suspended after a certain amount time passed
            if (maxMoveTime > 0.0f && current.activateTime > maxMoveTime) {
                return true;
            }

            // test if all bodies hardly moved over a period of time
            if (current.noMoveTime == 0.0f) {
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);
                    body.atRestOrigin.oSet(body.current.worldOrigin);
                    body.atRestAxis.oSet(body.current.worldAxis);
                }
                current.noMoveTime += timeStep;
            } else if (current.noMoveTime > noMoveTime) {
                current.noMoveTime = 0.0f;
                maxTranslationSqr = 0.0f;
                maxRotation = 0.0f;
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);

                    translationSqr = (body.current.worldOrigin.oMinus(body.atRestOrigin)).LengthSqr();
                    if (translationSqr > maxTranslationSqr) {
                        maxTranslationSqr = translationSqr;
                    }
                    rotation = (body.atRestAxis.Transpose().oMultiply(body.current.worldAxis)).ToRotation().GetAngle();
                    if (rotation > maxRotation) {
                        maxRotation = rotation;
                    }
                }

                if (maxTranslationSqr < Square(noMoveTranslation) && maxRotation < noMoveRotation) {
                    // hardly moved over a period of time so the articulated figure may come to rest
                    return true;
                }
            } else {
                current.noMoveTime += timeStep;
            }

            // test if the velocity or acceleration of any body is still too large to come to rest
            for (i = 0; i < bodies.Num(); i++) {
                body = bodies.oGet(i);

                if (body.current.spatialVelocity.SubVec3(0).LengthSqr() > Square(suspendVelocity.oGet(0))) {
                    return false;
                }
                if (body.current.spatialVelocity.SubVec3(1).LengthSqr() > Square(suspendVelocity.oGet(1))) {
                    return false;
                }
                if (body.acceleration.SubVec3(0).LengthSqr() > Square(suspendAcceleration.oGet(0))) {
                    return false;
                }
                if (body.acceleration.SubVec3(1).LengthSqr() > Square(suspendAcceleration.oGet(1))) {
                    return false;
                }
            }

            // all bodies have a velocity and acceleration small enough to come to rest
            return true;
        }

        private void Rest() {
            int i;

            current.atRest = gameLocal.time;

            for (i = 0; i < bodies.Num(); i++) {
                bodies.oGet(i).current.spatialVelocity.Zero();
                bodies.oGet(i).current.externalForce.Zero();
            }

            self.BecomeInactive(TH_PHYSICS);
        }

        private void AddPushVelocity(final idVec6 pushVelocity) {
            int i;

            if (pushVelocity != getVec6_origin()) {
                for (i = 0; i < bodies.Num(); i++) {
                    bodies.oGet(i).current.spatialVelocity.oPluSet(pushVelocity);
                }
            }
        }

        private void DebugDraw() {
            int i;
            idAFBody body, highlightBody = null, constrainedBody1 = null, constrainedBody2 = null;
            idAFConstraint constraint;
            idVec3 center = new idVec3();
            idMat3 axis;

            if (isNotNullOrEmpty(af_highlightConstraint.GetString())) {
                constraint = GetConstraint(af_highlightConstraint.GetString());
                if (constraint != null) {
                    constraint.GetCenter(center);
                    axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();
                    gameRenderWorld.DebugCone(colorYellow, center, (axis.oGet(2).oMinus(axis.oGet(1))).oMultiply(4.0f), 0.0f, 1.0f, 0);

                    if (af_showConstrainedBodies.GetBool()) {
                        cvarSystem.SetCVarString("cm_drawColor", colorCyan.ToString(0));
                        constrainedBody1 = constraint.body1;
                        if (constrainedBody1 != null) {
                            CollisionModel_local.collisionModelManager.DrawModel(constrainedBody1.clipModel.Handle(), constrainedBody1.clipModel.GetOrigin(),
                                    constrainedBody1.clipModel.GetAxis(), getVec3_origin(), 0.0f);
                        }
                        cvarSystem.SetCVarString("cm_drawColor", colorBlue.ToString(0));
                        constrainedBody2 = constraint.body2;
                        if (constrainedBody2 != null) {
                            CollisionModel_local.collisionModelManager.DrawModel(constrainedBody2.clipModel.Handle(), constrainedBody2.clipModel.GetOrigin(),
                                    constrainedBody2.clipModel.GetAxis(), getVec3_origin(), 0.0f);
                        }
                        cvarSystem.SetCVarString("cm_drawColor", colorRed.ToString(0));
                    }
                }
            }

            if (isNotNullOrEmpty(af_highlightBody.GetString())) {
                highlightBody = GetBody(af_highlightBody.GetString());
                if (highlightBody != null) {
                    cvarSystem.SetCVarString("cm_drawColor", colorYellow.ToString(0));
                    CollisionModel_local.collisionModelManager.DrawModel(highlightBody.clipModel.Handle(), highlightBody.clipModel.GetOrigin(),
                            highlightBody.clipModel.GetAxis(), getVec3_origin(), 0.0f);
                    cvarSystem.SetCVarString("cm_drawColor", colorRed.ToString(0));
                }
            }

            if (af_showBodies.GetBool()) {
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);
                    if (body == constrainedBody1 || body == constrainedBody2) {
                        continue;
                    }
                    if (body == highlightBody) {
                        continue;
                    }
                    CollisionModel_local.collisionModelManager.DrawModel(body.clipModel.Handle(), body.clipModel.GetOrigin(),
                            body.clipModel.GetAxis(), getVec3_origin(), 0.0f);
                    //DrawTraceModelSilhouette( gameLocal.GetLocalPlayer().GetEyePosition(), body.clipModel );
                }
            }

            if (af_showBodyNames.GetBool()) {
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);
                    gameRenderWorld.DrawText(body.GetName().toString(), body.GetWorldOrigin(), 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showMass.GetBool()) {
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);
                    gameRenderWorld.DrawText(va("\n%1.2f", 1.0f / body.GetInverseMass()), body.GetWorldOrigin(), 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showTotalMass.GetBool()) {
                axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();
                gameRenderWorld.DrawText(va("\n%1.2f", totalMass), bodies.oGet(0).GetWorldOrigin().oPlus(axis.oGet(2).oMultiply(8.0f)), 0.15f, colorCyan, axis, 1);
            }

            if (af_showInertia.GetBool()) {
                for (i = 0; i < bodies.Num(); i++) {
                    body = bodies.oGet(i);
                    idMat3 I = body.inertiaTensor;
                    gameRenderWorld.DrawText(va("\n\n\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )",
                                    I.oGet(0).x, I.oGet(0).y, I.oGet(0).z,
                                    I.oGet(1).x, I.oGet(1).y, I.oGet(1).z,
                                    I.oGet(2).x, I.oGet(2).y, I.oGet(2).z),
                            body.GetWorldOrigin(), 0.05f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showVelocity.GetBool()) {
                for (i = 0; i < bodies.Num(); i++) {
                    DrawVelocity(bodies.oGet(i).clipModel.GetId(), 0.1f, 4.0f);
                }
            }

            if (af_showConstraints.GetBool()) {
                for (i = 0; i < primaryConstraints.Num(); i++) {
                    constraint = primaryConstraints.oGet(i);
                    constraint.DebugDraw();
                }
                if (!af_showPrimaryOnly.GetBool()) {
                    for (i = 0; i < auxiliaryConstraints.Num(); i++) {
                        constraint = auxiliaryConstraints.oGet(i);
                        constraint.DebugDraw();
                    }
                }
            }

            if (af_showConstraintNames.GetBool()) {
                for (i = 0; i < primaryConstraints.Num(); i++) {
                    constraint = primaryConstraints.oGet(i);
                    constraint.GetCenter(center);
                    gameRenderWorld.DrawText(constraint.GetName().toString(), center, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
                if (!af_showPrimaryOnly.GetBool()) {
                    for (i = 0; i < auxiliaryConstraints.Num(); i++) {
                        constraint = auxiliaryConstraints.oGet(i);
                        constraint.GetCenter(center);
                        gameRenderWorld.DrawText(constraint.GetName().toString(), center, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                    }
                }
            }

            if (af_showTrees.GetBool() || (af_showActive.GetBool() && current.atRest < 0)) {
                for (i = 0; i < trees.Num(); i++) {
                    trees.oGet(i).DebugDraw(idStr.ColorForIndex(i + 3));
                }
            }
        }

        @Override
        public void oSet(idClass oGet) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    /*
     ================
     idPhysics_AF_SavePState
     ================
     */
    static void idPhysics_AF_SavePState(idSaveGame saveFile, final AFPState_s state) {
        saveFile.WriteInt(state.atRest);
        saveFile.WriteFloat(state.noMoveTime);
        saveFile.WriteFloat(state.activateTime);
        saveFile.WriteFloat(state.lastTimeStep);
        saveFile.WriteVec6(state.pushVelocity);
    }

    /*
     ================
     idPhysics_AF_RestorePState
     ================
     */
    static void idPhysics_AF_RestorePState(idRestoreGame saveFile, AFPState_s state) {
        int[] atRest = {0};
        float[] noMoveTime = {0};
        float[] activateTime = {0};
        float[] lastTimeStep = {0};

        saveFile.ReadInt(atRest);
        saveFile.ReadFloat(noMoveTime);
        saveFile.ReadFloat(activateTime);
        saveFile.ReadFloat(lastTimeStep);
        saveFile.ReadVec6(state.pushVelocity);

        state.atRest = atRest[0];
        state.noMoveTime = noMoveTime[0];
        state.activateTime = activateTime[0];
        state.lastTimeStep = lastTimeStep[0];
    }
}
