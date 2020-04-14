package neo.Game.Physics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static neo.Game.Entity.TH_PHYSICS;
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
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.FLOAT_IS_NAN;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Matrix.idMat3.SkewSymmetric;
import static neo.idlib.math.Matrix.idMat3.TransposeMultiply;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Matrix.idMat3.getMat3_zero;
import static neo.idlib.math.Matrix.idMatX.MATX_ALLOCA;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import static neo.idlib.math.Vector.getVec6_infinity;
import static neo.idlib.math.Vector.getVec6_origin;
import static neo.idlib.math.Vector.getVec6_zero;
import static neo.idlib.math.Vector.idVecX.VECX_ALLOCA;

import java.util.Arrays;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Timer.idTimer;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Lcp.idLCP;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Vector.idVecX;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMatX;

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
    }
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
        }
        protected constraintFlags_s fl;
//
//

        // friend class idPhysics_AF;
        // friend class idAFTree;
        public idAFConstraint() {
            this.type = CONSTRAINT_INVALID;
            this.name = new idStr("noname");
            this.body1 = null;
            this.body2 = null;
            this.physics = null;

            this.lo = new idVecX(6);
            this.lo.SubVec6_oSet(0, getVec6_infinity().oNegative());
            this.hi = new idVecX(6);
            this.hi.SubVec6_oSet(0, getVec6_infinity());
            this.e = new idVecX(6);
            this.e.SubVec6_oSet(0, vec6_lcp_epsilon);

            this.boxConstraint = null;
            this.boxIndex[0] = this.boxIndex[1] = this.boxIndex[2] = this.boxIndex[3] = this.boxIndex[4] = this.boxIndex[5] = -1;

            this.firstIndex = 0;

//	memset( &fl, 0, sizeof( fl ) );
            this.fl = new constraintFlags_s();
        }
        // virtual					~idAFConstraint( void );

        public constraintType_t GetType() {
            return this.type;
        }

        public idStr GetName() {
            return this.name;
        }

        public idAFBody GetBody1() {
            return this.body1;
        }

        public idAFBody GetBody2() {
            return this.body2;
        }

        public void SetPhysics(idPhysics_AF p) {
            this.physics = p;
        }

        public idVecX GetMultiplier() {
            return this.lm;
        }

        public void SetBody1(idAFBody body) {
            if (!this.body1.equals(body)) {
                this.body1 = body;
                if (this.physics != null) {
                    this.physics.SetChanged();
                }
            }
        }

        public void SetBody2(idAFBody body) {
            if (!this.body2.equals(body)) {
                this.body2 = body;
                if (this.physics != null) {
                    this.physics.SetChanged();
                }
            }
        }

        public void DebugDraw() {
        }

        public void GetForce(idAFBody body, idVec6 force) {
            final idVecX v = new idVecX();

            v.SetData(6, VECX_ALLOCA(6));
            if (body.equals(this.body1)) {
                this.J1.TransposeMultiply(v, this.lm);
            } else if (body.equals(this.body2)) {
                this.J2.TransposeMultiply(v, this.lm);
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
            saveFile.WriteInt(this.type.ordinal());
        }

        public void Restore(idRestoreGame saveFile) {
            final int[] t = {0};
            saveFile.ReadInt(t);//TODO:int to booleans
            assert (t[0] == this.type.ordinal());
        }

        protected void Evaluate(float invTimeStep) {
            assert (false);
        }

        protected void ApplyFriction(float invTimeStep) {
        }

        protected void InitSize(int size) {
            this.J1 = new idMatX(size, 6);
            this.J2 = new idMatX(size, 6);
            this.c1 = new idVecX(size);
            this.c2 = new idVecX(size);
            this.s = new idVecX(size);
            this.lm = new idVecX(size);
        }
    }

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
            this.type = CONSTRAINT_FIXED;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(6);
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;

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
            if (!this.body1.equals(body)) {
                this.body1 = body;
                InitOffset();
                if (this.physics != null) {
                    this.physics.SetChanged();
                }
            }
        }

        @Override
        public void SetBody2(idAFBody body) {
            if (!this.body2.equals(body)) {
                this.body2 = body;
                InitOffset();
                if (this.physics != null) {
                    this.physics.SetChanged();
                }
            }
        }

        @Override
        public void DebugDraw() {
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();
            if (master != null) {
                gameRenderWorld.DebugLine(colorRed, this.body1.GetWorldOrigin(), master.GetWorldOrigin());
            } else {
                gameRenderWorld.DebugLine(colorRed, this.body1.GetWorldOrigin(), getVec3_origin());
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == this.body2) {
                this.offset.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.offset.oMulSet(rotation);
                this.relAxis.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center = this.body1.GetWorldOrigin();
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.offset);
            saveFile.WriteMat3(this.relAxis);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(this.offset);
            saveFile.ReadMat3(this.relAxis);
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

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                a2 = this.offset.oMultiply(master.GetWorldAxis());
                ofs = a2.oPlus(master.GetWorldOrigin());
                ax = this.relAxis.oMultiply(master.GetWorldAxis());
            } else {
                a2.Zero();
                ofs = this.offset;
                ax = this.relAxis;
            }

            this.J1.Set(getMat3_identity(), getMat3_zero(), getMat3_zero(), getMat3_identity());

            if (this.body2 != null) {
                this.J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(), getMat3_identity().oNegative());
            } else {
                this.J2.Zero(6, 6);
            }

            this.c1.SubVec3_oSet(0, ofs.oMinus(this.body1.GetWorldOrigin()).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            r = (this.body1.GetWorldAxis().Transpose().oMultiply(ax)).ToRotation();
            this.c1.SubVec3_oSet(1, r.GetVec().oMultiply(-DEG2RAD(r.GetAngle())).oMultiply(-(invTimeStep * ERROR_REDUCTION)));

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
            final int a = 0;
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }

        protected void InitOffset() {
            if (this.body2 != null) {
                this.offset = (this.body1.GetWorldOrigin().oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose());
                this.relAxis = this.body1.GetWorldAxis().oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.offset = this.body1.GetWorldOrigin();
                this.relAxis = this.body1.GetWorldAxis();
            }
        }
    }

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
            this.type = CONSTRAINT_BALLANDSOCKETJOINT;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(3);
            this.coneLimit = null;
            this.pyramidLimit = null;
            this.friction = 0.0f;
            this.fc = null;
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;
        }
        // ~idAFConstraint_BallAndSocketJoint( void );

        public void SetAnchor(final idVec3 worldPosition) {

            // get anchor relative to center of mass of body1
            this.anchor1 = (worldPosition.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose());
            if (this.body2 != null) {
                // get anchor relative to center of mass of body2
                this.anchor2 = (worldPosition.oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.anchor2 = worldPosition;
            }

            if (this.coneLimit != null) {
                this.coneLimit.SetAnchor(this.anchor2);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.SetAnchor(this.anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (this.body2 != null) {
                return this.body2.GetWorldOrigin().oPlus(this.body2.GetWorldAxis().oMultiply(this.anchor2));
            }
            return this.anchor2;
        }

        public void SetNoLimit() {
            if (this.coneLimit != null) {
//		delete coneLimit;
                this.coneLimit = null;
            }
            if (this.pyramidLimit != null) {
//		delete pyramidLimit;
                this.pyramidLimit = null;
            }
        }

        public void SetConeLimit(final idVec3 coneAxis, final float coneAngle, final idVec3 body1Axis) {
            if (this.pyramidLimit != null) {
//		delete pyramidLimit;
                this.pyramidLimit = null;
            }
            if (null == this.coneLimit) {
                this.coneLimit = new idAFConstraint_ConeLimit();
                this.coneLimit.SetPhysics(this.physics);
            }
            if (this.body2 != null) {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, coneAxis.oMultiply(this.body2.GetWorldAxis().Transpose()), coneAngle, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            } else {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, coneAxis, coneAngle, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetPyramidLimit(final idVec3 pyramidAxis, final idVec3 baseAxis, final float angle1, final float angle2, final idVec3 body1Axis) {
            if (this.coneLimit != null) {
//		delete coneLimit;
                this.coneLimit = null;
            }
            if (null == this.pyramidLimit) {
                this.pyramidLimit = new idAFConstraint_PyramidLimit();
                this.pyramidLimit.SetPhysics(this.physics);
            }
            if (this.body2 != null) {
                this.pyramidLimit.Setup(this.body1, this.body2, this.anchor2, pyramidAxis.oMultiply(this.body2.GetWorldAxis().Transpose()),
                        baseAxis.oMultiply(this.body2.GetWorldAxis().Transpose()), angle1, angle2, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            } else {
                this.pyramidLimit.Setup(this.body1, this.body2, this.anchor2, pyramidAxis, baseAxis, angle1, angle2, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (this.coneLimit != null) {
                this.coneLimit.SetEpsilon(e);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.SetEpsilon(e);
            }
        }

        public void SetFriction(final float f) {
            this.friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return this.friction * this.physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            final idVec3 a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(5, 0, 0)), a1.oPlus(new idVec3(5, 0, 0)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(0, 5, 0)), a1.oPlus(new idVec3(0, 5, 0)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(new idVec3(0, 0, 5)), a1.oPlus(new idVec3(0, 0, 5)));

            if (af_showLimits.GetBool()) {
                if (this.coneLimit != null) {
                    this.coneLimit.DebugDraw();
                }
                if (this.pyramidLimit != null) {
                    this.pyramidLimit.DebugDraw();
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
            if (null == this.body2) {
                this.anchor2.oPluSet(translation);
            }
            if (this.coneLimit != null) {
                this.coneLimit.Translate(translation);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.anchor2.oMulSet(rotation);
            }
            if (this.coneLimit != null) {
                this.coneLimit.Rotate(rotation);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center.oSet(this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis())));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.anchor1);
            saveFile.WriteVec3(this.anchor2);
            saveFile.WriteFloat(this.friction);
            if (this.coneLimit != null) {
                this.coneLimit.Save(saveFile);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.Save(saveFile);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.anchor1);
            saveFile.ReadVec3(this.anchor2);
            saveFile.ReadFloat(friction);

            this.friction = friction[0];

            if (this.coneLimit != null) {
                this.coneLimit.Restore(saveFile);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.Restore(saveFile);
            }
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2 = new idVec3();
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.anchor1.oMultiply(this.body1.GetWorldAxis());

            if (master != null) {
                a2 = this.anchor2.oMultiply(master.GetWorldAxis());
                this.c1.SubVec3_oSet(0, (a2.oPlus(master.GetWorldOrigin()).oMinus(a1.oPlus(this.body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                this.c1.SubVec3_oSet(0, (this.anchor2.oMinus(a1.oPlus(this.body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            this.J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative());

            if (this.body2 != null) {
                this.J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2));
            } else {
                this.J2.Zero(3, 6);
            }

            if (this.coneLimit != null) {
                this.coneLimit.Add(this.physics, invTimeStep);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Add(this.physics, invTimeStep);
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

                angular = this.body1.GetAngularVelocity();
                invMass = this.body1.GetInverseMass();
                if (this.body2 != null) {
                    angular.oMinus(this.body2.GetAngularVelocity());
                    invMass += this.body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                this.body1.SetAngularVelocity(this.body1.GetAngularVelocity().oMinus(angular.oMultiply(this.body1.GetInverseMass())));
                if (this.body2 != null) {
                    this.body2.SetAngularVelocity(this.body2.GetAngularVelocity().oPlus(angular.oMultiply(this.body2.GetInverseMass())));
                }
            } else {
                if (null == this.fc) {
                    this.fc = new idAFConstraint_BallAndSocketJointFriction();
                    this.fc.Setup(this);
                }

                this.fc.Add(this.physics, invTimeStep);
            }
        }
    }

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
            this.type = CONSTRAINT_FRICTION;
            this.name.oSet("ballAndSocketJointFriction");
            InitSize(3);
            this.joint = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_BallAndSocketJoint bsj) {
            this.joint = bsj;
            this.body1 = bsj.GetBody1();
            this.body2 = bsj.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float f;

            this.physics = phys;

            f = this.joint.GetFriction() * this.joint.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            this.lo.p[0] = this.lo.p[1] = this.lo.p[2] = -f;
            this.hi.p[0] = this.hi.p[1] = this.hi.p[2] = f;

            this.J1.Zero(3, 6);
            this.J1.oSet(0, 3, this.J1.oSet(1, 4, this.J1.oSet(2, 5, 1.0f)));

            if (this.body2 != null) {

                this.J2.Zero(3, 6);
                this.J2.oSet(0, 3, this.J2.oSet(1, 4, this.J2.oSet(2, 5, 1.0f)));
            }

            this.physics.AddFrameConstraint(this);

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
    }

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
            this.type = CONSTRAINT_UNIVERSALJOINT;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(4);
            this.coneLimit = null;
            this.pyramidLimit = null;
            this.friction = 0.0f;
            this.fc = null;
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;
        }
        // ~idAFConstraint_UniversalJoint();

        private static int DBG_SetAnchor = 0;
        public void SetAnchor(final idVec3 worldPosition) {DBG_SetAnchor++;

            // get anchor relative to center of mass of body1
            this.anchor1.oSet((worldPosition.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose()));
            if (this.body2 != null) {
                // get anchor relative to center of mass of body2
                this.anchor2.oSet((worldPosition.oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose()));
            } else {
                this.anchor2.oSet(worldPosition);
            }

            if (this.coneLimit != null) {
                this.coneLimit.SetAnchor(this.anchor2);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.SetAnchor(this.anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (this.body2 != null) {
                return this.body2.GetWorldOrigin().oPlus(this.body2.GetWorldAxis().oMultiply(this.anchor2));
            }
            return this.anchor2;
        }

        public void SetShafts(final idVec3 cardanShaft1, final idVec3 cardanShaft2) {
            idVec3 cardanAxis;
            float l;

            this.shaft1.oSet(cardanShaft1);
            l = this.shaft1.Normalize();
            assert (l != 0.0f);
            this.shaft2.oSet(cardanShaft2);
            l = this.shaft2.Normalize();
            assert (l != 0.0f);

            // the cardan axis is a vector orthogonal to both cardan shafts
            cardanAxis = this.shaft1.Cross(this.shaft2);
            if (cardanAxis.Normalize() == 0.0f) {
                final idVec3 vecY = new idVec3();
                this.shaft1.OrthogonalBasis(cardanAxis, vecY);
                cardanAxis.Normalize();
            }

            this.shaft1.oMulSet(this.body1.GetWorldAxis().Transpose());
            this.axis1.oSet(cardanAxis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            if (this.body2 != null) {
                this.shaft2.oMulSet(this.body2.GetWorldAxis().Transpose());
                this.axis2.oSet(cardanAxis.oMultiply(this.body2.GetWorldAxis().Transpose()));
            } else {
                this.axis2.oSet(cardanAxis);
            }

            if (this.coneLimit != null) {
                this.coneLimit.SetBody1Axis(this.shaft1);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.SetBody1Axis(this.shaft1);
            }
        }

        public void GetShafts(idVec3 cardanShaft1, idVec3 cardanShaft2) {
            cardanShaft1.oSet(this.shaft1);
            cardanShaft2.oSet(this.shaft2);
        }

        public void SetNoLimit() {
            if (this.coneLimit != null) {
                this.coneLimit = null;
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit = null;
            }
        }

        public void SetConeLimit(final idVec3 coneAxis, final float coneAngle) {
            if (this.pyramidLimit != null) {
                this.pyramidLimit = null;
            }
            if (null == this.coneLimit) {
                this.coneLimit = new idAFConstraint_ConeLimit();
                this.coneLimit.SetPhysics(this.physics);
            }
            if (this.body2 != null) {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, coneAxis.oMultiply(this.body2.GetWorldAxis().Transpose()), coneAngle, this.shaft1);
            } else {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, coneAxis, coneAngle, this.shaft1);
            }
        }

        public void SetPyramidLimit(final idVec3 pyramidAxis, final idVec3 baseAxis, final float angle1, final float angle2) {
            if (this.coneLimit != null) {
                this.coneLimit = null;
            }
            if (null == this.pyramidLimit) {
                this.pyramidLimit = new idAFConstraint_PyramidLimit();
                this.pyramidLimit.SetPhysics(this.physics);
            }
            if (this.body2 != null) {
                this.pyramidLimit.Setup(this.body1, this.body2, this.anchor2, pyramidAxis.oMultiply(this.body2.GetWorldAxis().Transpose()), baseAxis.oMultiply(this.body2.GetWorldAxis().Transpose()), angle1, angle2, this.shaft1);
            } else {
                this.pyramidLimit.Setup(this.body1, this.body2, this.anchor2, pyramidAxis, baseAxis, angle1, angle2, this.shaft1);
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (this.coneLimit != null) {
                this.coneLimit.SetEpsilon(e);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.SetEpsilon(e);
            }
        }

        public void SetFriction(final float f) {
            this.friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return this.friction * this.physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            idVec3 a1, a2, s1, s2, d1, d2, v;
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            s1 = this.shaft1.oMultiply(this.body1.GetWorldAxis());
            d1 = this.axis1.oMultiply(this.body1.GetWorldAxis());

            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(this.anchor2.oMultiply(master.GetWorldAxis()));
                s2 = this.shaft2.oMultiply(master.GetWorldAxis());
                d2 = this.axis2.oMultiply(master.GetWorldAxis());
            } else {
                a2 = new idVec3(this.anchor2);
                s2 = new idVec3(this.shaft2);
                d2 = new idVec3(this.axis2);
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
                if (this.coneLimit != null) {
                    this.coneLimit.DebugDraw();
                }
                if (this.pyramidLimit != null) {
                    this.pyramidLimit.DebugDraw();
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
            if (null == this.body2) {
                this.anchor2.oPluSet(translation);
            }
            if (this.coneLimit != null) {
                this.coneLimit.Translate(translation);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.anchor2.oMulSet(rotation);
                this.shaft2.oMulSet(rotation.ToMat3());
                this.axis2.oMulSet(rotation.ToMat3());
            }
            if (this.coneLimit != null) {
                this.coneLimit.Rotate(rotation);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center.oSet(this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis())));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.anchor1);
            saveFile.WriteVec3(this.anchor2);
            saveFile.WriteVec3(this.shaft1);
            saveFile.WriteVec3(this.shaft2);
            saveFile.WriteVec3(this.axis1);
            saveFile.WriteVec3(this.axis2);
            saveFile.WriteFloat(this.friction);
            if (this.coneLimit != null) {
                this.coneLimit.Save(saveFile);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.Save(saveFile);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.anchor1);
            saveFile.ReadVec3(this.anchor2);
            saveFile.ReadVec3(this.shaft1);
            saveFile.ReadVec3(this.shaft2);
            saveFile.ReadVec3(this.axis1);
            saveFile.ReadVec3(this.axis2);
            saveFile.ReadFloat(friction);

            this.friction = friction[0];

            if (this.coneLimit != null) {
                this.coneLimit.Restore(saveFile);
            }
            if (this.pyramidLimit != null) {
                this.pyramidLimit.Restore(saveFile);
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

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.anchor1.oMultiply(this.body1.GetWorldAxis());
            s1 = this.shaft1.oMultiply(this.body1.GetWorldAxis());
            d1 = s1.Cross(this.axis1.oMultiply(this.body1.GetWorldAxis()));

            if (master != null) {
                a2 = this.anchor2.oMultiply(master.GetWorldAxis());
                s2 = this.shaft2.oMultiply(master.GetWorldAxis());
                d2 = this.axis2.oMultiply(master.GetWorldAxis());
                this.c1.SubVec3_oSet(0, (a2.oPlus(master.GetWorldOrigin()).oMinus(a1.oPlus(this.body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                a2 = new idVec3(this.anchor2);
                s2 = new idVec3(this.shaft2);
                d2 = new idVec3(this.axis2);
                this.c1.SubVec3_oSet(0, (a2.oMinus(a1.oPlus(this.body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            this.J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative(), getMat3_zero(),
                    new idMat3(s1.oGet(0), s1.oGet(1), s1.oGet(2),
                            0.0f, 0.0f, 0.0f,
                            0.0f, 0.0f, 0.0f));
            this.J1.SetSize(4, 6);

            if (this.body2 != null) {
                this.J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(),
                        new idMat3(s2.oGet(0), s2.oGet(1), s2.oGet(2),
                                0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f));
                this.J2.SetSize(4, 6);
            } else {
                this.J2.Zero(4, 6);
            }

            v = s1.Cross(s2);
            if (v.Normalize() != 0.0f) {
                idMat3 m1, m2;

                m1 = new idMat3(s1, v, v.Cross(s1));

                m2 = new idMat3(s2.oNegative(), v, v.Cross(s2.oNegative()));

                d2.oMulSet(m2.Transpose().oMultiply(m1));
            }

            this.c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (d1.oMultiply(d2));

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            if (this.coneLimit != null) {
                this.coneLimit.Add(this.physics, invTimeStep);
            } else if (this.pyramidLimit != null) {
                this.pyramidLimit.Add(this.physics, invTimeStep);
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

                angular = this.body1.GetAngularVelocity();
                invMass = this.body1.GetInverseMass();
                if (this.body2 != null) {
                    angular.oMinSet(this.body2.GetAngularVelocity());
                    invMass += this.body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                this.body1.SetAngularVelocity(this.body1.GetAngularVelocity().oMinus(angular.oMultiply(this.body1.GetInverseMass())));
                if (this.body2 != null) {
                    this.body2.SetAngularVelocity(this.body2.GetAngularVelocity().oPlus(angular.oMultiply(this.body2.GetInverseMass())));
                }
            } else {
                if (null == this.fc) {
                    this.fc = new idAFConstraint_UniversalJointFriction();
                    this.fc.Setup(this);
                }

                this.fc.Add(this.physics, invTimeStep);
            }
        }
    }

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
            this.type = CONSTRAINT_FRICTION;
            this.name.oSet("universalJointFriction");
            InitSize(2);
            this.joint = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_UniversalJoint uj) {
            this.joint = uj;
            this.body1 = uj.GetBody1();
            this.body2 = uj.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            final idVec3 s1 = new idVec3(), s2 = new idVec3(), dir1 = new idVec3(), dir2 = new idVec3();
            float f;

            this.physics = phys;

            f = this.joint.GetFriction() * this.joint.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            this.lo.p[0] = this.lo.p[1] = -f;
            this.hi.p[0] = this.hi.p[1] = f;

            this.joint.GetShafts(s1, s2);

            s1.oMulSet(this.body1.GetWorldAxis());
            s1.NormalVectors(dir1, dir2);

            this.J1.SetSize(2, 6);
            this.J1.SubVec63_Zero(0, 0);
            this.J1.SubVec63_oSet(0, 1, dir1);
            this.J1.SubVec63_Zero(1, 0);
            this.J1.SubVec63_oSet(1, 1, dir2);

            if (this.body2 != null) {

                this.J2.SetSize(2, 6);
                this.J2.SubVec63_Zero(0, 0);
                this.J2.SubVec63_oSet(0, 1, dir1.oNegative());
                this.J2.SubVec63_Zero(1, 0);
                this.J2.SubVec63_oSet(1, 1, dir2.oNegative());
            }

            this.physics.AddFrameConstraint(this);

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
    }

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
    }

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
            this.type = CONSTRAINT_HINGE;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(5);
            this.coneLimit = null;
            this.steering = null;
            this.friction = 0.0f;
            this.fc = null;
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;
            this.initialAxis = body1.GetWorldAxis();
            if (body2 != null) {
                this.initialAxis.oMulSet(body2.GetWorldAxis().Transpose());
            }
        }
        // ~idAFConstraint_Hinge();

        public void SetAnchor(final idVec3 worldPosition) {
            // get anchor relative to center of mass of body1
            this.anchor1 = (worldPosition.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose());
            if (this.body2 != null) {
                // get anchor relative to center of mass of body2
                this.anchor2 = (worldPosition.oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.anchor2 = worldPosition;
            }

            if (this.coneLimit != null) {
                this.coneLimit.SetAnchor(this.anchor2);
            }
        }

        public idVec3 GetAnchor() {
            if (this.body2 != null) {
                return this.body2.GetWorldOrigin().oPlus(this.body2.GetWorldAxis().oMultiply(this.anchor2));
            }
            return this.anchor2;
        }

        public void SetAxis(final idVec3 axis) {
            idVec3 normAxis;

            normAxis = axis;
            normAxis.Normalize();

            // get axis relative to body1
            this.axis1 = normAxis.oMultiply(this.body1.GetWorldAxis().Transpose());
            if (this.body2 != null) {
                // get axis relative to body2
                this.axis2 = normAxis.oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.axis2 = normAxis;
            }
        }

        public void GetAxis(idVec3 a1, idVec3 a2) {
            a1.oSet(this.axis1);
            a2.oSet(this.axis2);
        }

        public idVec3 GetAxis() {
            if (this.body2 != null) {
                return this.axis2.oMultiply(this.body2.GetWorldAxis());
            }
            return this.axis2;
        }

        public void SetNoLimit() {
            if (this.coneLimit != null) {
//		delete coneLimit;
                this.coneLimit = null;
            }
        }

        public void SetLimit(final idVec3 axis, final float angle, final idVec3 body1Axis) {
            if (null == this.coneLimit) {
                this.coneLimit = new idAFConstraint_ConeLimit();
                this.coneLimit.SetPhysics(this.physics);
            }
            if (this.body2 != null) {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, axis.oMultiply(this.body2.GetWorldAxis().Transpose()), angle, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            } else {
                this.coneLimit.Setup(this.body1, this.body2, this.anchor2, axis, angle, body1Axis.oMultiply(this.body1.GetWorldAxis().Transpose()));
            }
        }

        public void SetLimitEpsilon(final float e) {
            if (this.coneLimit != null) {
                this.coneLimit.SetEpsilon(e);
            }
        }

        public float GetAngle() {
            idMat3 axis;
            idRotation rotation;
            float angle;

            axis = this.body1.GetWorldAxis().oMultiply(this.body2.GetWorldAxis().Transpose().oMultiply(this.initialAxis.Transpose()));
            rotation = axis.ToRotation();
            angle = rotation.GetAngle();
            if (rotation.GetVec().oMultiply(this.axis1) < 0.0f) {
                return -angle;
            }
            return angle;
        }

        public void SetSteerAngle(final float degrees) {
            if (this.coneLimit != null) {
//		delete coneLimit;
                this.coneLimit = null;
            }
            if (null == this.steering) {
                this.steering = new idAFConstraint_HingeSteering();
                this.steering.Setup(this);
            }
            this.steering.SetSteerAngle(degrees);
        }

        public void SetSteerSpeed(final float speed) {
            if (this.steering != null) {
                this.steering.SetSteerSpeed(speed);
            }
        }

        public void SetFriction(final float f) {
            this.friction = f;
        }

        public float GetFriction() {
            if (af_forceFriction.GetFloat() > 0.0f) {
                return af_forceFriction.GetFloat();
            }
            return this.friction * this.physics.GetJointFrictionScale();
        }

        @Override
        public void DebugDraw() {
            final idVec3 vecX = new idVec3(), vecY = new idVec3();
            final idVec3 a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            final idVec3 x1 = this.axis1.oMultiply(this.body1.GetWorldAxis());
            x1.OrthogonalBasis(vecX, vecY);

            gameRenderWorld.DebugArrow(colorBlue, a1.oMinus(x1.oMultiply(4.0f)), a1.oPlus(x1.oMultiply(4.0f)), 1);
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(vecX.oMultiply(2.0f)), a1.oPlus(vecX.oMultiply(2.0f)));
            gameRenderWorld.DebugLine(colorBlue, a1.oMinus(vecY.oMultiply(2.0f)), a1.oPlus(vecY.oMultiply(2.0f)));

            if (af_showLimits.GetBool()) {
                if (this.coneLimit != null) {
                    this.coneLimit.DebugDraw();
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
            if (null == this.body2) {
                this.anchor2.oPluSet(translation);
            }
            if (this.coneLimit != null) {
                this.coneLimit.Translate(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.anchor2.oMulSet(rotation);
                this.axis2.oMulSet(rotation.ToMat3());
            }
            if (this.coneLimit != null) {
                this.coneLimit.Rotate(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            center = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.anchor1);
            saveFile.WriteVec3(this.anchor2);
            saveFile.WriteVec3(this.axis1);
            saveFile.WriteVec3(this.axis2);
            saveFile.WriteMat3(this.initialAxis);
            saveFile.WriteFloat(this.friction);
            if (this.coneLimit != null) {
                saveFile.WriteBool(true);
                this.coneLimit.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
            if (this.steering != null) {
                saveFile.WriteBool(true);
                this.steering.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
            if (this.fc != null) {
                saveFile.WriteBool(true);
                this.fc.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final boolean[] b = {false};
            final float[] friction = {this.friction};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.anchor1);
            saveFile.ReadVec3(this.anchor2);
            saveFile.ReadVec3(this.axis1);
            saveFile.ReadVec3(this.axis2);
            saveFile.ReadMat3(this.initialAxis);
            saveFile.ReadFloat(friction);

            saveFile.ReadBool(b);

            this.friction = friction[0];

            if (b[0]) {
                if (null == this.coneLimit) {
                    this.coneLimit = new idAFConstraint_ConeLimit();
                }
                this.coneLimit.SetPhysics(this.physics);
                this.coneLimit.Restore(saveFile);
            }
            saveFile.ReadBool(b);
            if (b[0]) {
                if (null == this.steering) {
                    this.steering = new idAFConstraint_HingeSteering();
                }
                this.steering.Setup(this);
                this.steering.Restore(saveFile);
            }
            saveFile.ReadBool(b);
            if (b[0]) {
                if (null == this.fc) {
                    this.fc = new idAFConstraint_HingeFriction();
                }
                this.fc.Setup(this);
                this.fc.Restore(saveFile);
            }
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2;
            idVec3 x1, x2, cross;
            final idVec3 vecX = new idVec3(), vecY = new idVec3();
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            x1 = this.axis1.oMultiply(this.body1.GetWorldAxis());        // axis in body1 space
            x1.OrthogonalBasis(vecX, vecY);                    // basis for axis in body1 space

            a1 = this.anchor1.oMultiply(this.body1.GetWorldAxis());      // anchor in body1 space

            if (master != null) {
                a2 = this.anchor2.oMultiply(master.GetWorldAxis()); // anchor in master space
                x2 = this.axis2.oMultiply(master.GetWorldAxis());
                this.c1.SubVec3_oSet(0, (a2.oPlus(master.GetWorldOrigin()).oMinus(a1.oPlus(this.body1.GetWorldOrigin()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            } else {
                a2 = this.anchor2;
                x2 = this.axis2;
                this.c1.SubVec3_oSet(0, a2.oMinus(a1.oPlus(this.body1.GetWorldOrigin())).oMultiply(-(invTimeStep * ERROR_REDUCTION)));
            }

            this.J1.Set(getMat3_identity(), SkewSymmetric(a1).oNegative(), getMat3_zero(),
                    new idMat3(vecX.oGet(0), vecX.oGet(1), vecX.oGet(2),
                            vecY.oGet(0), vecY.oGet(1), vecY.oGet(2),
                            0.0f, 0.0f, 0.0f));
            this.J1.SetSize(5, 6);

            if (this.body2 != null) {
                this.J2.Set(getMat3_identity().oNegative(), SkewSymmetric(a2), getMat3_zero(),
                        new idMat3(-vecX.oGet(0), -vecX.oGet(1), -vecX.oGet(2),
                                -vecY.oGet(0), -vecY.oGet(1), -vecY.oGet(2),
                                0.0f, 0.0f, 0.0f));
                this.J2.SetSize(5, 6);
            } else {
                this.J2.Zero(5, 6);
            }

            cross = x1.Cross(x2);

            this.c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (cross.oMultiply(vecX));
            this.c1.p[4] = -(invTimeStep * ERROR_REDUCTION) * (cross.oMultiply(vecY));

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);

            if (this.steering != null) {
                this.steering.Add(this.physics, invTimeStep);
            } else if (this.coneLimit != null) {
                this.coneLimit.Add(this.physics, invTimeStep);
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

                angular = this.body1.GetAngularVelocity();
                invMass = this.body1.GetInverseMass();
                if (this.body2 != null) {
                    angular.oMinSet(this.body2.GetAngularVelocity());
                    invMass += this.body2.GetInverseMass();
                }

                angular.oMulSet(currentFriction / invMass);

                this.body1.SetAngularVelocity(this.body1.GetAngularVelocity().oMinus(angular.oMultiply(this.body1.GetInverseMass())));
                if (this.body2 != null) {
                    this.body2.SetAngularVelocity(this.body2.GetAngularVelocity().oPlus(angular.oMultiply(this.body2.GetInverseMass())));
                }
            } else {
                if (null == this.fc) {
                    this.fc = new idAFConstraint_HingeFriction();
                    this.fc.Setup(this);
                }

                this.fc.Add(this.physics, invTimeStep);
            }
        }
    }

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
            this.type = CONSTRAINT_FRICTION;
            this.name.oSet("hingeFriction");
            InitSize(1);
            this.hinge = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_Hinge h) {
            this.hinge = h;
            this.body1 = h.GetBody1();
            this.body2 = h.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            final idVec3 a1 = new idVec3(), a2 = new idVec3();
            float f;

            this.physics = phys;

            f = this.hinge.GetFriction() * this.hinge.GetMultiplier().Length();
            if (f == 0.0f) {
                return false;
            }

            this.lo.p[0] = -f;
            this.hi.p[0] = f;

            this.hinge.GetAxis(a1, a2);

            a1.oMulSet(this.body1.GetWorldAxis());

            this.J1.SetSize(1, 6);
            this.J1.SubVec63_Zero(0, 0);
            this.J1.SubVec63_oSet(0, 1, a1);

            if (this.body2 != null) {
                a2.oMulSet(this.body2.GetWorldAxis());

                this.J2.SetSize(1, 6);
                this.J2.SubVec63_Zero(0, 0);
                this.J2.SubVec63_oSet(0, 1, a2.oNegative());
            }

            this.physics.AddFrameConstraint(this);

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
    }

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
            this.type = CONSTRAINT_HINGESTEERING;
            this.name.oSet("hingeFriction");
            InitSize(1);
            this.hinge = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
            this.steerSpeed = 0.0f;
            this.epsilon = LCP_EPSILON;
        }

        public void Setup(idAFConstraint_Hinge h) {
            this.hinge = h;
            this.body1 = h.GetBody1();
            this.body2 = h.GetBody2();
        }

        public void SetSteerAngle(final float degrees) {
            this.steerAngle = degrees;
        }

        public void SetSteerSpeed(final float speed) {
            this.steerSpeed = speed;
        }

        public void SetEpsilon(final float e) {
            this.epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float angle, speed;
            final idVec3 a1 = new idVec3(), a2 = new idVec3();

            this.physics = phys;

            this.hinge.GetAxis(a1, a2);
            angle = this.hinge.GetAngle();

            a1.oMulSet(this.body1.GetWorldAxis());

            this.J1.SetSize(1, 6);
            this.J1.SubVec63_Zero(0, 0);
            this.J1.SubVec63_oSet(0, 1, a1);

            if (this.body2 != null) {
                a2.oMulSet(this.body2.GetWorldAxis());

                this.J2.SetSize(1, 6);
                this.J2.SubVec63_Zero(0, 0);
                this.J2.SubVec63_oSet(0, 1, a2.oNegative());
            }

            speed = this.steerAngle - angle;
            if (this.steerSpeed != 0.0f) {
                if (speed > this.steerSpeed) {
                    speed = this.steerSpeed;
                } else if (speed < -this.steerSpeed) {
                    speed = -this.steerSpeed;
                }
            }

            this.c1.p[0] = DEG2RAD(speed) * invTimeStep;

            this.physics.AddFrameConstraint(this);

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
            saveFile.WriteFloat(this.steerAngle);
            saveFile.WriteFloat(this.steerSpeed);
            saveFile.WriteFloat(this.epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[] steerAngle = {0};//TODO:check if these read pointers need to have the original values set instead of zero;
            final float[] steerSpeed = {0};
            final float[] epsilon = {0};

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
    }

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
            this.type = CONSTRAINT_SLIDER;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(5);
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;

            if (body2 != null) {
                this.offset = (body1.GetWorldOrigin().oMinus(body2.GetWorldOrigin())).oMultiply(body1.GetWorldAxis().Transpose());
                this.relAxis = body1.GetWorldAxis().oMultiply(body2.GetWorldAxis().Transpose());
            } else {
                this.offset = body1.GetWorldOrigin();
                this.relAxis = body1.GetWorldAxis();
            }
        }

        public void SetAxis(final idVec3 ax) {
            idVec3 normAxis;

            // get normalized axis relative to body1
            normAxis = ax;//TODO:unreferenced clone!?
            normAxis.Normalize();
            if (this.body2 != null) {
                this.axis = normAxis.oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.axis = normAxis;
            }
        }

        @Override
        public void DebugDraw() {
            idVec3 ofs;
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();
            if (master != null) {
                ofs = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(this.offset).oMinus(this.body1.GetWorldOrigin()));
            } else {
                ofs = this.offset.oMinus(this.body1.GetWorldOrigin());
            }
            gameRenderWorld.DebugLine(colorGreen, ofs, ofs.oPlus(this.axis.oMultiply(this.body1.GetWorldAxis())));
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == this.body2) {
                this.offset.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.offset.oMulSet(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();
            if (master != null) {
                center = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(this.offset).oMinus(this.body1.GetWorldOrigin()));
            } else {
                center = this.offset.oMinus(this.body1.GetWorldOrigin());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.axis);
            saveFile.WriteVec3(this.offset);
            saveFile.WriteMat3(this.relAxis);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(this.axis);
            saveFile.ReadVec3(this.offset);
            saveFile.ReadMat3(this.relAxis);
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            final idVec3 vecX = new idVec3(), vecY = new idVec3();
			idVec3 ofs;
            idRotation r;
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                (this.axis.oMultiply(master.GetWorldAxis())).OrthogonalBasis(vecX, vecY);
                ofs = master.GetWorldOrigin().oPlus(master.GetWorldAxis().oMultiply(this.offset).oMinus(this.body1.GetWorldOrigin()));
                r = (this.body1.GetWorldAxis().Transpose().oMultiply((this.relAxis.oMultiply(master.GetWorldAxis()))).ToRotation());
            } else {
                this.axis.OrthogonalBasis(vecX, vecY);
                ofs = this.offset.oMinus(this.body1.GetWorldOrigin());
                r = (this.body1.GetWorldAxis().Transpose().oMultiply(this.relAxis)).ToRotation();
            }

            this.J1.Set(getMat3_zero(), getMat3_identity(),
                    new idMat3(vecX, vecY, getVec3_origin()), getMat3_zero());
            this.J1.SetSize(5, 6);

            if (this.body2 != null) {

                this.J2.Set(getMat3_zero(), getMat3_identity().oNegative(),
                        new idMat3(vecX.oNegative(), vecY.oNegative(), getVec3_origin()), getMat3_zero());
                this.J2.SetSize(5, 6);
            } else {
                this.J2.Zero(5, 6);
            }

            this.c1.SubVec3_oSet(0, (r.GetVec().oMultiply(-DEG2RAD(r.GetAngle()))).oMultiply(-(invTimeStep * ERROR_REDUCTION)));

            this.c1.p[3] = -(invTimeStep * ERROR_REDUCTION) * (vecX.oMultiply(ofs));
            this.c1.p[4] = -(invTimeStep * ERROR_REDUCTION) * (vecY.oMultiply(ofs));

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
            final int a = 0;
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    }

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
    }

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
            this.type = CONSTRAINT_PLANE;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(1);
            this.fl.allowPrimary = true;
            this.fl.noCollision = true;
        }

        public void SetPlane(final idVec3 normal, final idVec3 anchor) {
            // get anchor relative to center of mass of body1
            this.anchor1 = (anchor.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose());
            if (this.body2 != null) {
                // get anchor relative to center of mass of body2
                this.anchor2 = (anchor.oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose());
                this.planeNormal = normal.oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.anchor2 = anchor;
                this.planeNormal = normal;
            }
        }

        @Override
        public void DebugDraw() {
            idVec3 a1, normal;
			final idVec3 right = new idVec3(), up = new idVec3();
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            if (master != null) {
                normal = this.planeNormal.oMultiply(master.GetWorldAxis());
            } else {
                normal = this.planeNormal;
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
            if (null == this.body2) {
                this.anchor2.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.anchor2.oMulSet(rotation);
                this.planeNormal.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.anchor1);
            saveFile.WriteVec3(this.anchor2);
            saveFile.WriteVec3(this.planeNormal);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            super.Restore(saveFile);
            saveFile.ReadVec3(this.anchor1);
            saveFile.ReadVec3(this.anchor2);
            saveFile.ReadVec3(this.planeNormal);
        }

        @Override
        protected void Evaluate(float invTimeStep) {
            idVec3 a1, a2, normal, p;
            final idVec6 v = new idVec6();
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(this.anchor2.oMultiply(master.GetWorldAxis()));
                normal = this.planeNormal.oMultiply(master.GetWorldAxis());
            } else {
                a2 = this.anchor2;
                normal = this.planeNormal;
            }

            p = a1.oMinus(this.body1.GetWorldOrigin());
            v.SubVec3_oSet(0, normal);
            v.SubVec3_oSet(1, p.Cross(normal));
            this.J1.Set(1, 6, v.ToFloatPtr());

            if (this.body2 != null) {
                p = a1.oMinus(this.body2.GetWorldOrigin());
                v.SubVec3_oSet(0, normal.oNegative());
                v.SubVec3_oSet(1, p.Cross(normal.oNegative()));
                this.J2.Set(1, 6, v.ToFloatPtr());
            }

            this.c1.p[0] = -(invTimeStep * ERROR_REDUCTION) * (a1.oMultiply(normal) - a2.oMultiply(normal));

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
            final int a = 0;
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    }

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
            this.type = CONSTRAINT_SPRING;
            this.name.oSet(name);
            this.body1 = body1;
            this.body2 = body2;
            InitSize(1);
            this.fl.allowPrimary = false;
            this.kstretch = this.kcompress = this.damping = 1.0f;
            this.minLength = this.maxLength = this.restLength = 0.0f;
        }

        public void SetAnchor(final idVec3 worldAnchor1, final idVec3 worldAnchor2) {
            // get anchor relative to center of mass of body1
            this.anchor1 = (worldAnchor1.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose());
            if (this.body2 != null) {
                // get anchor relative to center of mass of body2
                this.anchor2 = (worldAnchor2.oMinus(this.body2.GetWorldOrigin())).oMultiply(this.body2.GetWorldAxis().Transpose());
            } else {
                this.anchor2 = worldAnchor2;
            }
        }

        public void SetSpring(final float stretch, final float compress, final float damping, final float restLength) {
            assert ((stretch >= 0.0f) && (compress >= 0.0f) && (restLength >= 0.0f));
            this.kstretch = stretch;
            this.kcompress = compress;
            this.damping = damping;
            this.restLength = restLength;
        }

        public void SetLimit(final float minLength, final float maxLength) {
            assert ((minLength >= 0.0f) && (maxLength >= 0.0f) && (maxLength >= minLength));
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        @Override
        public void DebugDraw() {
            idAFBody master;
            float length;
            idVec3 a1, a2, dir, mid, p;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();
            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(this.anchor2.oMultiply(master.GetWorldAxis()));
            } else {
                a2 = this.anchor2;
            }
            dir = a2.oMinus(a1);
            mid = a1.oPlus(dir.oMultiply(0.5f));
            length = dir.Normalize();

            // draw spring
            gameRenderWorld.DebugLine(colorGreen, a1, a2);

            // draw rest length
            p = dir.oMultiply(this.restLength * 0.5f);
            gameRenderWorld.DebugCircle(colorWhite, mid.oPlus(p), dir, 1.0f, 10);
            gameRenderWorld.DebugCircle(colorWhite, mid.oMinus(p), dir, 1.0f, 10);
            if (this.restLength > length) {
                gameRenderWorld.DebugLine(colorWhite, a2, mid.oPlus(p));
                gameRenderWorld.DebugLine(colorWhite, a1, mid.oMinus(p));
            }

            if (this.minLength > 0.0f) {
                // draw min length
                gameRenderWorld.DebugCircle(colorBlue, mid.oPlus(dir.oMultiply(this.minLength * 0.5f)), dir, 2.0f, 10);
                gameRenderWorld.DebugCircle(colorBlue, mid.oMinus(dir.oMultiply(this.minLength * 0.5f)), dir, 2.0f, 10);
            }

            if (this.maxLength > 0.0f) {
                // draw max length
                gameRenderWorld.DebugCircle(colorRed, mid.oPlus(dir.oMultiply(this.maxLength * 0.5f)), dir, 2.0f, 10);
                gameRenderWorld.DebugCircle(colorRed, mid.oMinus(dir.oMultiply(this.maxLength * 0.5f)), dir, 2.0f, 10);
            }
        }

        @Override
        public void Translate(final idVec3 translation) {
            if (null == this.body2) {
                this.anchor2.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.anchor2.oMulSet(rotation);
            }
        }

        @Override
        public void GetCenter(idVec3 center) {
            idAFBody master;
            idVec3 a1, a2;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();
            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(this.anchor2.oMultiply(master.GetWorldAxis()));
            } else {
                a2 = this.anchor2;
            }
            center.oSet((a1.oPlus(a2)).oMultiply(0.5f));
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.anchor1);
            saveFile.WriteVec3(this.anchor2);
            saveFile.WriteFloat(this.kstretch);
            saveFile.WriteFloat(this.kcompress);
            saveFile.WriteFloat(this.damping);
            saveFile.WriteFloat(this.restLength);
            saveFile.WriteFloat(this.minLength);
            saveFile.WriteFloat(this.maxLength);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[] kstretch = {0};
            final float[] kcompress = {0};
            final float[] damping = {0};
            final float[] restLength = {0};
            final float[] minLength = {0};
            final float[] maxLength = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.anchor1);
            saveFile.ReadVec3(this.anchor2);
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
            final idVec6 v1 = new idVec6(), v2 = new idVec6();
            float d, dampingForce, length, error;
            boolean limit;
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            a1 = this.body1.GetWorldOrigin().oPlus(this.anchor1.oMultiply(this.body1.GetWorldAxis()));
            velocity1 = this.body1.GetPointVelocity(a1);

            if (master != null) {
                a2 = master.GetWorldOrigin().oPlus(this.anchor2.oMultiply(master.GetWorldAxis()));
                velocity2 = master.GetPointVelocity(a2);
            } else {
                a2 = this.anchor2;
                velocity2.Zero();
            }

            force = a2.oMinus(a1);
            d = force.oMultiply(force);
            if (d != 0.0f) {
                dampingForce = (this.damping * idMath.Fabs((velocity2.oMinus(velocity1)).oMultiply(force))) / d;
            } else {
                dampingForce = 0.0f;
            }
            length = force.Normalize();

            if (length > this.restLength) {
                if (this.kstretch > 0.0f) {
                    final idVec3 springForce = force.oMultiply((Square(length - this.restLength) * this.kstretch) - dampingForce);
                    this.body1.AddForce(a1, springForce);
                    if (master != null) {
                        master.AddForce(a2, springForce.oNegative());
                    }
                }
            } else {
                if (this.kcompress > 0.0f) {
                    final idVec3 springForce = force.oMultiply(-((Square(this.restLength - length) * this.kcompress) - dampingForce));
                    this.body1.AddForce(a1, springForce);
                    if (master != null) {
                        master.AddForce(a2, springForce.oNegative());
                    }
                }
            }

            // check for spring limits
            if (length < this.minLength) {
                force = force.oNegative();
                error = this.minLength - length;
                limit = true;
            } else if ((this.maxLength > 0.0f) && (length > this.maxLength)) {
                error = length - this.maxLength;
                limit = true;
            } else {
                error = 0.0f;
                limit = false;
            }

            if (limit) {
                a1.oMinSet(this.body1.GetWorldOrigin());
                v1.SubVec3_oSet(0, force);
                v1.SubVec3_oSet(1, a1.Cross(force));
                this.J1.Set(1, 6, v1.ToFloatPtr());
                if (this.body2 != null) {
                    a2.oMinSet(this.body2.GetWorldOrigin());
                    v2.SubVec3_oSet(0, force.oNegative());
                    v2.SubVec3_oSet(1, a2.Cross(force.oNegative()));
                    this.J2.Set(1, 6, v2.ToFloatPtr());
                }
                this.c1.p[0] = -(invTimeStep * ERROR_REDUCTION) * error;
                this.lo.p[0] = 0.0f;
            } else {
                this.J1.Zero(0, 0);
                this.J2.Zero(0, 0);
            }

            this.c1.Clamp(-ERROR_REDUCTION_MAX, ERROR_REDUCTION_MAX);
            final int a = 0;
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // no friction
        }
    }

    //===============================================================
    //
    //	idAFConstraint_Contact
    //
    //===============================================================
    // constrains body1 to either be in contact with or move away from body2
    public static class idAFConstraint_Contact extends idAFConstraint {

        public idAFConstraint_Contact() {
            this.name.oSet("contact");
            this.type = CONSTRAINT_CONTACT;
            InitSize(1);
            this.fc = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
        }
        // ~idAFConstraint_Contact();

        public void Setup(idAFBody b1, idAFBody b2, contactInfo_t c) {
            idVec3 p;
            final idVec6 v = new idVec6();
            float vel;
            final float minBounceVelocity = 2.0f;

            assert (b1 != null);

            this.body1 = b1;
            this.body2 = b2;
            this.contact = new contactInfo_t(c);

            p = c.point.oMinus(this.body1.GetWorldOrigin());
            v.SubVec3_oSet(0, c.normal);
            v.SubVec3_oSet(1, p.Cross(c.normal));
            this.J1.Set(1, 6, v.ToFloatPtr());
            vel = v.SubVec3(0).oMultiply(this.body1.GetLinearVelocity()) + v.SubVec3(1).oMultiply(this.body1.GetAngularVelocity());

            if (this.body2 != null) {
                p = c.point.oMinus(this.body2.GetWorldOrigin());
                v.SubVec3_oSet(0, c.normal.oNegative());
                v.SubVec3_oSet(1, p.Cross(c.normal.oNegative()));
                this.J2.Set(1, 6, v.ToFloatPtr());
                vel += v.SubVec3(0).oMultiply(this.body2.GetLinearVelocity()) + v.SubVec3(1).oMultiply(this.body2.GetAngularVelocity());
                this.c2.p[0] = 0.0f;
            }

            if ((this.body1.GetBouncyness() > 0.0f) && (-vel > minBounceVelocity)) {
                this.c1.p[0] = this.body1.GetBouncyness() * vel;
            } else {
                this.c1.p[0] = 0.0f;
            }

            this.e.p[0] = CONTACT_LCP_EPSILON;
            this.lo.p[0] = 0.0f;
            this.hi.p[0] = idMath.INFINITY;
            this.boxConstraint = null;
            this.boxIndex[0] = -1;
        }

        public contactInfo_t GetContact() {
            return this.contact;
        }

        @Override
        public void DebugDraw() {
            final idVec3 x = new idVec3(), y = new idVec3();
            this.contact.normal.NormalVectors(x, y);
            gameRenderWorld.DebugLine(colorWhite, this.contact.point, this.contact.point.oPlus(this.contact.normal.oMultiply(6.0f)));
            gameRenderWorld.DebugLine(colorWhite, this.contact.point.oMinus(x.oMultiply(2.0f)), this.contact.point.oPlus(x.oMultiply(2.0f)));
            gameRenderWorld.DebugLine(colorWhite, this.contact.point.oMinus(y.oMultiply(2.0f)), this.contact.point.oPlus(y.oMultiply(2.0f)));
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
            center.oSet(this.contact.point);
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
            idVec3 r, velocity, normal;
			final idVec3 dir1, dir2;
            float friction, magnitude, forceNumerator, forceDenominator;
            final idVecX impulse = new idVecX(), dv = new idVecX();

            friction = this.body1.GetContactFriction();
            if ((this.body2 != null) && (this.body2.GetContactFriction() < friction)) {
                friction = this.body2.GetContactFriction();
            }

            friction *= this.physics.GetContactFrictionScale();

            if (friction <= 0.0f) {
                return;
            }

            // seperate friction per contact is silly but it's fast and often looks close enough
            if (af_useImpulseFriction.GetBool()) {

                impulse.SetData(6, VECX_ALLOCA(6));
                dv.SetData(6, VECX_ALLOCA(6));

                // calculate velocity in the contact plane
                r = this.contact.point.oMinus(this.body1.GetWorldOrigin());
                velocity = this.body1.GetLinearVelocity().oPlus(this.body1.GetAngularVelocity().Cross(r));
                velocity.oMinSet(this.contact.normal.oMultiply(velocity.oMultiply(this.contact.normal)));

                // get normalized direction of friction and magnitude of velocity
                normal = velocity.oNegative();
                magnitude = normal.Normalize();

                forceNumerator = friction * magnitude;
                forceDenominator = this.body1.GetInverseMass() + ((this.body1.GetInverseWorldInertia().oMultiply(r.Cross(normal)).Cross(r)).oMultiply(normal));
                impulse.SubVec3_oSet(0, normal.oMultiply(forceNumerator / forceDenominator));
                impulse.SubVec3_oSet(1, r.Cross(impulse.SubVec3(0)));
                this.body1.InverseWorldSpatialInertiaMultiply(dv, impulse.ToFloatPtr());

                // modify velocity with friction force
                this.body1.SetLinearVelocity(this.body1.GetLinearVelocity().oPlus(dv.SubVec3(0)));
                this.body1.SetAngularVelocity(this.body1.GetAngularVelocity().oPlus(dv.SubVec3(1)));
            } else {

                if (null == this.fc) {
                    this.fc = new idAFConstraint_ContactFriction();
                }
                // call setup each frame because contact constraints are re-used for different bodies
                this.fc.Setup(this);
                this.fc.Add(this.physics, invTimeStep);
            }
        }
    }

    //===============================================================
    //
    //	idAFConstraint_ContactFriction
    //
    //===============================================================
    // contact friction
    public static class idAFConstraint_ContactFriction extends idAFConstraint {

        public idAFConstraint_ContactFriction() {
            this.type = CONSTRAINT_FRICTION;
            this.name.oSet("contactFriction");
            InitSize(2);
            this.cc = null;
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
        }

        public void Setup(idAFConstraint_Contact cc) {
            this.cc = cc;
            this.body1 = cc.GetBody1();
            this.body2 = cc.GetBody2();
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            idVec3 r;
			final idVec3 dir1 = new idVec3(), dir2 = new idVec3();
            float friction;
            int newRow;

            this.physics = phys;

            friction = this.body1.GetContactFriction() * this.physics.GetContactFrictionScale();

            // if the body only has friction in one direction
            if (this.body1.GetFrictionDirection(dir1)) {
                // project the friction direction into the contact plane
                dir1.oMinSet(dir1.oMultiply(this.cc.GetContact().normal.oMultiply(dir1)));
                dir1.Normalize();

                r = this.cc.GetContact().point.oMinus(this.body1.GetWorldOrigin());

                this.J1.SetSize(1, 6);
                this.J1.SubVec63_oSet(0, 0, dir1);
                this.J1.SubVec63_oSet(0, 1, r.Cross(dir1));
                this.c1.SetSize(1);
                this.c1.p[0] = 0.0f;

                if (this.body2 != null) {
                    r = this.cc.GetContact().point.oMinus(this.body2.GetWorldOrigin());

                    this.J2.SetSize(1, 6);
                    this.J2.SubVec63_oSet(0, 0, dir1.oNegative());
                    this.J2.SubVec63_oSet(0, 1, r.Cross(dir1.oNegative()));
                    this.c2.SetSize(1);
                    this.c2.p[0] = 0.0f;
                }

                this.lo.p[0] = -friction;
                this.hi.p[0] = friction;
                this.boxConstraint = this.cc;
                this.boxIndex[0] = 0;
            } else {
                // get two friction directions orthogonal to contact normal
                this.cc.GetContact().normal.NormalVectors(dir1, dir2);

                r = this.cc.GetContact().point.oMinus(this.body1.GetWorldOrigin());

                this.J1.SetSize(2, 6);
                this.J1.SubVec63_oSet(0, 0, dir1);
                this.J1.SubVec63_oSet(0, 1, r.Cross(dir1));
                this.J1.SubVec63_oSet(1, 0, dir2);
                this.J1.SubVec63_oSet(1, 1, r.Cross(dir2));
                this.c1.SetSize(2);
                this.c1.p[0] = this.c1.p[1] = 0.0f;

                if (this.body2 != null) {
                    r = this.cc.GetContact().point.oMinus(this.body2.GetWorldOrigin());

                    this.J2.SetSize(2, 6);
                    this.J2.SubVec63_oSet(0, 0, dir1.oNegative());
                    this.J2.SubVec63_oSet(0, 1, r.Cross(dir1.oNegative()));
                    this.J2.SubVec63_oSet(1, 0, dir2.oNegative());
                    this.J2.SubVec63_oSet(1, 1, r.Cross(dir2.oNegative()));
                    this.c2.SetSize(2);
                    this.c2.p[0] = this.c2.p[1] = 0.0f;

                    if (this.body2.GetContactFriction() < friction) {
                        friction = this.body2.GetContactFriction();
                    }
                }

                this.lo.p[0] = -friction;
                this.hi.p[0] = friction;
                this.boxConstraint = this.cc;
                this.boxIndex[0] = 0;
                this.lo.p[1] = -friction;
                this.hi.p[1] = friction;
                this.boxIndex[1] = 0;
            }

            if (this.body1.GetContactMotorDirection(dir1) && (this.body1.GetContactMotorForce() > 0.0f)) {
                // project the motor force direction into the contact plane
                dir1.oMinSet(dir1.oMultiply(this.cc.GetContact().normal.oMultiply(dir1)));
                dir1.Normalize();

                r = this.cc.GetContact().point.oMinus(this.body1.GetWorldOrigin());

                newRow = this.J1.GetNumRows();
                this.J1.ChangeSize(newRow + 1, this.J1.GetNumColumns());
                this.J1.SubVec63_oSet(newRow, 0, dir1.oNegative());
                this.J1.SubVec63_oSet(newRow, 1, r.Cross(dir1.oNegative()));
                this.c1.ChangeSize(newRow + 1);
                this.c1.p[newRow] = this.body1.GetContactMotorVelocity();

                if (this.body2 != null) {
                    r = this.cc.GetContact().point.oMinus(this.body2.GetWorldOrigin());

                    this.J2.ChangeSize(newRow + 1, this.J2.GetNumColumns());
                    this.J2.SubVec63_oSet(newRow, 0, dir1.oNegative());
                    this.J2.SubVec63_oSet(newRow, 1, r.Cross(dir1.oNegative()));
                    this.c2.ChangeSize(newRow + 1);
                    this.c2.p[newRow] = 0.0f;
                }

                this.lo.p[newRow] = -this.body1.GetContactMotorForce();
                this.hi.p[newRow] = this.body1.GetContactMotorForce();
                this.boxIndex[newRow] = -1;
            }

            this.physics.AddFrameConstraint(this);

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
    }

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
            this.coneAnchor = new idVec3();
            this.coneAxis = new idVec3();
            this.body1Axis = new idVec3();
            this.type = CONSTRAINT_CONELIMIT;
            this.name.oSet("coneLimit");
            InitSize(1);
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;
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
            this.epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            float a;
            final idVec6 J1row = new idVec6(), J2row = new idVec6();
            idVec3 ax, anchor, body1ax, normal, coneVector, p1, p2;
            final idQuat q = new idQuat();
            idAFBody master;

            if (af_skipLimits.GetBool()) {
                this.lm.Zero();    // constraint exerts no force
                return false;
            }

            this.physics = phys;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                ax = this.coneAxis.oMultiply(master.GetWorldAxis());
                anchor = master.GetWorldOrigin().oPlus(this.coneAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                ax = this.coneAxis;
                anchor = this.coneAnchor;
            }

            body1ax = this.body1Axis.oMultiply(this.body1.GetWorldAxis());

            a = ax.oMultiply(body1ax);

            // if the body1 axis is inside the cone
            if (a > this.cosAngle) {
                this.lm.Zero();    // constraint exerts no force
                return false;
            }

            // calculate the inward cone normal for the position the body1 axis went outside the cone
            normal = body1ax.Cross(ax);
            normal.Normalize();
            q.x = normal.x * this.sinHalfAngle;
            q.y = normal.y * this.sinHalfAngle;
            q.z = normal.z * this.sinHalfAngle;
            q.w = this.cosHalfAngle;
            coneVector = ax.oMultiply(q.ToMat3());
            normal = coneVector.Cross(ax).Cross(coneVector);
            normal.Normalize();

            p1 = anchor.oPlus(coneVector.oMultiply(32.0f)).oMinus(this.body1.GetWorldOrigin());

            J1row.SubVec3_oSet(0, normal);
            J1row.SubVec3_oSet(1, p1.Cross(normal));
            this.J1.Set(1, 6, J1row.ToFloatPtr());

            this.c1.p[0] = (invTimeStep * LIMIT_ERROR_REDUCTION) * (normal.oMultiply(body1ax.oMultiply(32.0f)));

            if (this.body2 != null) {

                p2 = anchor.oPlus(coneVector.oMultiply(32.0f)).oMinus(master.GetWorldOrigin());

                J2row.SubVec3_oSet(0, normal.oNegative());
                J2row.SubVec3_oSet(1, p2.Cross(normal.oNegative()));
                this.J2.Set(1, 6, J2row.ToFloatPtr());

                this.c2.p[0] = 0.0f;
            }

            this.lo.p[0] = 0.0f;
            this.e.p[0] = LIMIT_LCP_EPSILON;

            this.physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void DebugDraw() {
            idVec3 ax, anchor;
			final idVec3 x = new idVec3(), y = new idVec3();
			idVec3 z, start, end;
            float sinAngle, a;
			final float size = 10.0f;
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                ax = this.coneAxis.oMultiply(master.GetWorldAxis());
                anchor = master.GetWorldOrigin().oPlus(this.coneAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                ax = this.coneAxis;
                anchor = this.coneAnchor;
            }

            // draw body1 axis
            gameRenderWorld.DebugLine(colorGreen, anchor, anchor.oPlus((this.body1Axis.oMultiply(this.body1.GetWorldAxis())).oMultiply(size)));

            // draw cone
            ax.NormalVectors(x, y);
            sinAngle = idMath.Sqrt(1.0f - (this.cosAngle * this.cosAngle));
            x.oMulSet(size * sinAngle);
            y.oMulSet(size * sinAngle);
            z = anchor.oPlus(ax.oMultiply(size * this.cosAngle));
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
            if (null == this.body2) {
                this.coneAnchor.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.coneAnchor.oMulSet(rotation);
                this.coneAxis.oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.coneAnchor);
            saveFile.WriteVec3(this.coneAxis);
            saveFile.WriteVec3(this.body1Axis);
            saveFile.WriteFloat(this.cosAngle);
            saveFile.WriteFloat(this.sinHalfAngle);
            saveFile.WriteFloat(this.cosHalfAngle);
            saveFile.WriteFloat(this.epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[] cosAngle = {0};
            final float[] sinHalfAngle = {0};
            final float[] cosHalfAngle = {0};
            final float[] epsilon = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.coneAnchor);
            saveFile.ReadVec3(this.coneAxis);
            saveFile.ReadVec3(this.body1Axis);
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
    }

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
            this.type = CONSTRAINT_PYRAMIDLIMIT;
            this.name.oSet("pyramidLimit");
            InitSize(1);
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;

            this.pyramidAnchor = new idVec3();
            this.pyramidBasis = new idMat3();
            this.body1Axis = new idVec3();
        }

        public void Setup(idAFBody b1, idAFBody b2, final idVec3 pyramidAnchor, final idVec3 pyramidAxis,
                          final idVec3 baseAxis, final float pyramidAngle1, final float pyramidAngle2, final idVec3 body1Axis) {
            this.body1 = b1;
            this.body2 = b2;
            // setup the base and make sure the basis is orthonormal
            this.pyramidBasis.oSet(2, pyramidAxis);
            this.pyramidBasis.oGet(2).Normalize();
            this.pyramidBasis.oSet(0, baseAxis);
            this.pyramidBasis.oGet(0).oMinSet(this.pyramidBasis.oGet(2).oMultiply(baseAxis.oMultiply(this.pyramidBasis.oGet(2))));
            this.pyramidBasis.oGet(0).Normalize();
            this.pyramidBasis.oSet(1, this.pyramidBasis.oGet(0).Cross(this.pyramidBasis.oGet(2)));
            // pyramid top
            pyramidAnchor.oSet(pyramidAnchor);
            // angles
            this.cosAngle[0] = (float) cos(DEG2RAD(pyramidAngle1 * 0.5f));
            this.cosAngle[1] = (float) cos(DEG2RAD(pyramidAngle2 * 0.5f));
            this.sinHalfAngle[0] = (float) sin(DEG2RAD(pyramidAngle1 * 0.25f));
            this.sinHalfAngle[1] = (float) sin(DEG2RAD(pyramidAngle2 * 0.25f));
            this.cosHalfAngle[0] = (float) cos(DEG2RAD(pyramidAngle1 * 0.25f));
            this.cosHalfAngle[1] = (float) cos(DEG2RAD(pyramidAngle2 * 0.25f));

            body1Axis.oSet(body1Axis);
        }

        public void SetAnchor(final idVec3 pyramidAxis) {
            this.pyramidAnchor.oSet(this.pyramidAnchor);
        }

        public void SetBody1Axis(final idVec3 body1Axis) {
            this.body1Axis.oSet(body1Axis);
        }

        public void SetEpsilon(final float e) {
            this.epsilon = e;
        }

        public boolean Add(idPhysics_AF phys, float invTimeStep) {
            int i;
            final float[] a = new float[2];
            final idVec6 J1row = new idVec6(), J2row = new idVec6();
            idMat3 worldBase = new idMat3();
            idVec3 anchor, body1ax, v, normal, pyramidVector, p1, p2;
            final idVec3[] ax = new idVec3[2];
            final idQuat q = new idQuat();
            idAFBody master;

            if (af_skipLimits.GetBool()) {
                this.lm.Zero();    // constraint exerts no force
                return false;
            }

            this.physics = phys;
            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                worldBase.oSet(0, this.pyramidBasis.oGet(0).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(1, this.pyramidBasis.oGet(1).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(2, this.pyramidBasis.oGet(2).oMultiply(master.GetWorldAxis()));
                anchor = master.GetWorldOrigin().oPlus(this.pyramidAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                worldBase = this.pyramidBasis;
                anchor = this.pyramidAnchor;
            }

            body1ax = this.body1Axis.oMultiply(this.body1.GetWorldAxis());

            for (i = 0; i < 2; i++) {
                final int he = (i == 0 ? 1 : 0);
                ax[i] = body1ax.oMinus(worldBase.oGet(he).oMultiply(body1ax.oMultiply(worldBase.oGet(he))));
                ax[i].Normalize();
                a[i] = worldBase.oGet(2).oMultiply(ax[i]);
            }

            // if the body1 axis is inside the pyramid
            if ((a[0] > this.cosAngle[0]) && (a[1] > this.cosAngle[1])) {
                this.lm.Zero();    // constraint exerts no force
                return false;
            }

            // calculate the inward pyramid normal for the position the body1 axis went outside the pyramid
            pyramidVector = worldBase.oGet(2);
            for (i = 0; i < 2; i++) {
                if (a[i] <= this.cosAngle[i]) {
                    v = ax[i].Cross(worldBase.oGet(2));
                    v.Normalize();
                    q.x = v.x * this.sinHalfAngle[i];
                    q.y = v.y * this.sinHalfAngle[i];
                    q.z = v.z * this.sinHalfAngle[i];
                    q.w = this.cosHalfAngle[i];
                    pyramidVector.oMulSet(q.ToMat3());
                }
            }
            normal = pyramidVector.Cross(worldBase.oGet(2)).Cross(pyramidVector);
            normal.Normalize();

            p1 = anchor.oPlus(pyramidVector.oMultiply(32.0f).oMinus(this.body1.GetWorldOrigin()));

            J1row.SubVec3_oSet(0, normal);
            J1row.SubVec3_oSet(1, p1.Cross(normal));
            this.J1.Set(1, 6, J1row.ToFloatPtr());

            this.c1.p[0] = (invTimeStep * LIMIT_ERROR_REDUCTION) * (normal.oMultiply(body1ax.oMultiply(32.0f)));

            if (this.body2 != null) {

                p2 = anchor.oPlus(pyramidVector.oMultiply(32.0f).oMinus(master.GetWorldOrigin()));

                J2row.SubVec3_oSet(0, normal.oNegative());
                J2row.SubVec3_oSet(1, p2.Cross(normal.oNegative()));
                this.J2.Set(1, 6, J2row.ToFloatPtr());

                this.c2.p[0] = 0.0f;
            }

            this.lo.p[0] = 0.0f;
            this.e.p[0] = LIMIT_LCP_EPSILON;

            this.physics.AddFrameConstraint(this);

            return true;
        }

        @Override
        public void DebugDraw() {
            int i;
            final float size = 10.0f;
            idVec3 anchor, dir;
            final idVec3[] p = new idVec3[4];
            idMat3 worldBase = new idMat3();
            final idMat3[] m = new idMat3[2];
            final idQuat q = new idQuat();
            idAFBody master;

            master = this.body2 != null ? this.body2 : this.physics.GetMasterBody();

            if (master != null) {
                worldBase.oSet(0, this.pyramidBasis.oGet(0).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(1, this.pyramidBasis.oGet(1).oMultiply(master.GetWorldAxis()));
                worldBase.oSet(2, this.pyramidBasis.oGet(2).oMultiply(master.GetWorldAxis()));
                anchor = master.GetWorldOrigin().oPlus(this.pyramidAnchor.oMultiply(master.GetWorldAxis()));
            } else {
                worldBase = this.pyramidBasis;
                anchor = this.pyramidAnchor;
            }

            // draw body1 axis
            gameRenderWorld.DebugLine(colorGreen, anchor, anchor.oPlus((this.body1Axis.oMultiply(this.body1.GetWorldAxis())).oMultiply(size)));

            // draw the pyramid
            for (i = 0; i < 2; i++) {
                final int him = (i == 0 ? 1 : 0);
                q.x = worldBase.oGet(him).x * this.sinHalfAngle[i];
                q.y = worldBase.oGet(him).y * this.sinHalfAngle[i];
                q.z = worldBase.oGet(him).z * this.sinHalfAngle[i];
                q.w = this.cosHalfAngle[i];
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
            if (null == this.body2) {
                this.pyramidAnchor.oPluSet(translation);
            }
        }

        @Override
        public void Rotate(final idRotation rotation) {
            if (null == this.body2) {
                this.pyramidAnchor.oMulSet(rotation);
                this.pyramidBasis.oGet(0).oMulSet(rotation.ToMat3());
                this.pyramidBasis.oGet(1).oMulSet(rotation.ToMat3());
                this.pyramidBasis.oGet(2).oMulSet(rotation.ToMat3());
            }
        }

        @Override
        public void Save(idSaveGame saveFile) {
            super.Save(saveFile);
            saveFile.WriteVec3(this.pyramidAnchor);
            saveFile.WriteMat3(this.pyramidBasis);
            saveFile.WriteVec3(this.body1Axis);
            saveFile.WriteFloat(this.cosAngle[0]);
            saveFile.WriteFloat(this.cosAngle[1]);
            saveFile.WriteFloat(this.sinHalfAngle[0]);
            saveFile.WriteFloat(this.sinHalfAngle[1]);
            saveFile.WriteFloat(this.cosHalfAngle[0]);
            saveFile.WriteFloat(this.cosHalfAngle[1]);
            saveFile.WriteFloat(this.epsilon);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            final float[][] cosAngle = {{0}, {0}};
            final float[][] sinHalfAngle = {{0}, {0}};
            final float[][] cosHalfAngle = {{0}, {0}};
            final float[] epsilon = {0};

            super.Restore(saveFile);
            saveFile.ReadVec3(this.pyramidAnchor);
            saveFile.ReadMat3(this.pyramidBasis);
            saveFile.ReadVec3(this.body1Axis);
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
    }

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
            this.type = CONSTRAINT_SUSPENSION;
            this.name.oSet("suspension");
            InitSize(3);
            this.fl.allowPrimary = false;
            this.fl.frameConstraint = true;

            this.localOrigin.Zero();
            this.localAxis.Identity();
            this.suspensionUp = 0.0f;
            this.suspensionDown = 0.0f;
            this.suspensionKCompress = 0.0f;
            this.suspensionDamping = 0.0f;
            this.steerAngle = 0.0f;
            this.friction = 2.0f;
            this.motorEnabled = false;
            this.motorForce = 0.0f;
            this.motorVelocity = 0.0f;
            this.wheelModel = null;
            this.trace = new trace_s();//	memset( &trace, 0, sizeof( trace ) );
            this.epsilon = LCP_EPSILON;
        }

        public void Setup(final String name, idAFBody body, final idVec3 origin, final idMat3 axis, idClipModel clipModel) {
            this.name.oSet(name);
            this.body1 = body;
            this.body2 = null;
            this.localOrigin = (origin.oMinus(body.GetWorldOrigin())).oMultiply(body.GetWorldAxis().Transpose());
            this.localAxis = axis.oMultiply(body.GetWorldAxis().Transpose());
            this.wheelModel = clipModel;
        }

        public void SetSuspension(final float up, final float down, final float k, final float d, final float f) {
            this.suspensionUp = up;
            this.suspensionDown = down;
            this.suspensionKCompress = k;
            this.suspensionDamping = d;
            this.friction = f;
        }

        public void SetSteerAngle(final float degrees) {
            this.steerAngle = degrees;
        }

        public void EnableMotor(final boolean enable) {
            this.motorEnabled = enable;
        }

        public void SetMotorForce(final float force) {
            this.motorForce = force;
        }

        public void SetMotorVelocity(final float vel) {
            this.motorVelocity = vel;
        }

        public void SetEpsilon(final float e) {
            this.epsilon = e;
        }

        public idVec3 GetWheelOrigin() {
            return this.body1.GetWorldOrigin().oPlus(this.wheelOffset.oMultiply(this.body1.GetWorldAxis()));
        }

        @Override
        public void DebugDraw() {
            idVec3 origin;
            idMat3 axis;
            final idRotation rotation = new idRotation();

            axis = this.localAxis.oMultiply(this.body1.GetWorldAxis());

            rotation.SetVec(axis.oGet(2));
            rotation.SetAngle(this.steerAngle);

            axis.oMulSet(rotation.ToMat3());

            if (this.trace.fraction < 1.0f) {
                origin = this.trace.c.point;

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
            final idRotation rotation = new idRotation();

            axis = this.localAxis.oMultiply(this.body1.GetWorldAxis());
            origin = this.body1.GetWorldOrigin().oPlus(this.localOrigin.oMultiply(this.body1.GetWorldAxis()));
            start = origin.oPlus(axis.oGet(2).oMultiply(this.suspensionUp));
            end = origin.oMinus(axis.oGet(2).oMultiply(this.suspensionDown));

            rotation.SetVec(axis.oGet(2));
            rotation.SetAngle(this.steerAngle);

            axis.oMulSet(rotation.ToMat3());

            {
                final trace_s[] tracy = {this.trace};
                gameLocal.clip.Translation(tracy, start, end, this.wheelModel, axis, MASK_SOLID, null);
                this.trace = tracy[0];
            }

            this.wheelOffset = (this.trace.endpos.oMinus(this.body1.GetWorldOrigin())).oMultiply(this.body1.GetWorldAxis().Transpose());

            if (this.trace.fraction >= 1.0f) {
                this.J1.SetSize(0, 6);
                if (this.body2 != null) {
                    this.J2.SetSize(0, 6);
                }
                return;
            }

            // calculate and add spring force
            vel1 = this.body1.GetPointVelocity(start);
            if (this.body2 != null) {
                vel2 = this.body2.GetPointVelocity(this.trace.c.point);
            } else {
                vel2.Zero();
            }

            suspensionLength = this.suspensionUp + this.suspensionDown;
            springDir = this.trace.endpos.oMinus(start);
            springLength = this.trace.fraction * suspensionLength;
            dampingForce = (this.suspensionDamping * idMath.Fabs((vel2.oMinus(vel1)).oMultiply(springDir))) / (1.0f + (springLength * springLength));
            compression = suspensionLength - springLength;
            springForce = (compression * compression * this.suspensionKCompress) - dampingForce;

            r = this.trace.c.point.oMinus(this.body1.GetWorldOrigin());
            this.J1.SetSize(2, 6);
            this.J1.SubVec63_oSet(0, 0, this.trace.c.normal);
            this.J1.SubVec63_oSet(0, 1, r.Cross(this.trace.c.normal));
            this.c1.SetSize(2);
            this.c1.p[0] = 0.0f;
            velocity = this.J1.SubVec6(0).SubVec3(0).oMultiply(this.body1.GetLinearVelocity()) + this.J1.SubVec6(0).SubVec3(1).oMultiply(this.body1.GetAngularVelocity());

            if (this.body2 != null) {
                r = this.trace.c.point.oMinus(this.body2.GetWorldOrigin());
                this.J2.SetSize(2, 6);
                this.J2.SubVec63_oSet(0, 0, this.trace.c.normal.oNegative());
                this.J2.SubVec63_oSet(0, 1, r.Cross(this.trace.c.normal.oNegative()));
                this.c2.SetSize(2);
                this.c2.p[0] = 0.0f;
                velocity += this.J2.SubVec6(0).SubVec3(0).oMultiply(this.body2.GetLinearVelocity()) + this.J2.SubVec6(0).SubVec3(1).oMultiply(this.body2.GetAngularVelocity());
            }

            this.c1.p[0] = -compression;        // + 0.5f * -velocity;

            this.e.p[0] = 1e-4f;
            this.lo.p[0] = 0.0f;
            this.hi.p[0] = springForce;
            this.boxConstraint = null;
            this.boxIndex[0] = -1;

            // project the friction direction into the contact plane
            frictionDir = axis.oGet(1).oMinus(axis.oGet(1).oMultiply(this.trace.c.normal.oMultiply(axis.oGet(1))));
            frictionDir.Normalize();

            r = this.trace.c.point.oMinus(this.body1.GetWorldOrigin());

            this.J1.SubVec63_oSet(1, 0, frictionDir);
            this.J1.SubVec63_oSet(1, 1, r.Cross(frictionDir));
            this.c1.p[1] = 0.0f;

            if (this.body2 != null) {
                r = this.trace.c.point.oMinus(this.body2.GetWorldOrigin());

                this.J2.SubVec63_oSet(1, 0, frictionDir.oNegative());
                this.J2.SubVec63_oSet(1, 1, r.Cross(frictionDir.oNegative()));
                this.c2.p[1] = 0.0f;
            }

            this.lo.p[1] = -this.friction * this.physics.GetContactFrictionScale();
            this.hi.p[1] = this.friction * this.physics.GetContactFrictionScale();

            this.boxConstraint = this;
            this.boxIndex[1] = 0;

            if (this.motorEnabled) {
                // project the motor force direction into the contact plane
                motorDir = axis.oGet(0).oMinus(axis.oGet(0).oMultiply(this.trace.c.normal.oMultiply(axis.oGet(0))));
                motorDir.Normalize();

                r = this.trace.c.point.oMinus(this.body1.GetWorldOrigin());

                this.J1.ChangeSize(3, this.J1.GetNumColumns());
                this.J1.SubVec63_oSet(2, 0, motorDir.oNegative());
                this.J1.SubVec63_oSet(2, 1, r.Cross(motorDir.oNegative()));
                this.c1.ChangeSize(3);
                this.c1.p[2] = this.motorVelocity;

                if (this.body2 != null) {
                    r = this.trace.c.point.oMinus(this.body2.GetWorldOrigin());

                    this.J2.ChangeSize(3, this.J2.GetNumColumns());
                    this.J2.SubVec63_oSet(2, 0, motorDir.oNegative());
                    this.J2.SubVec63_oSet(2, 1, r.Cross(motorDir.oNegative()));
                    this.c2.ChangeSize(3);
                    this.c2.p[2] = 0.0f;
                }

                this.lo.p[2] = -this.motorForce;
                this.hi.p[2] = this.motorForce;
                this.boxIndex[2] = -1;
            }
        }

        @Override
        protected void ApplyFriction(float invTimeStep) {
            // do nothing
        }
    }

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

        public AFBodyPState_s() {
            this.worldOrigin = new idVec3();
            this.worldAxis = new idMat3();
            this.spatialVelocity = new idVec6();
            this.externalForce = new idVec6();
        }

        public AFBodyPState_s(AFBodyPState_s bodyPState_s) {
            this();
            this.oSet(bodyPState_s);
        }

        public void oSet(AFBodyPState_s body) {
            this.worldOrigin.oSet(body.worldOrigin);
            this.worldAxis.oSet(body.worldAxis);
            this.spatialVelocity.oSet(body.spatialVelocity);
            this.externalForce.oSet(body.externalForce);
        }
    }

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
        private final AFBodyPState_s[] state = new AFBodyPState_s[2];
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
        }
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

            this.current.worldOrigin.oSet(clipModel.GetOrigin());
            this.current.worldAxis.oSet(clipModel.GetAxis());
            this.next.oSet(this.current);

        }

        // ~idAFBody();
        protected void _deconstructor(){
            idClipModel.delete(this.clipModel);
        }

        public void Init() {
            this.name = new idStr("noname");
            this.parent = null;
            this.children = new idList<>();
            this.clipModel = null;
            this.primaryConstraint = null;
            this.constraints = new idList<>();
            this.tree = null;

            this.linearFriction = -1.0f;
            this.angularFriction = -1.0f;
            this.contactFriction = -1.0f;
            this.bouncyness = -1.0f;
            this.clipMask = 0;

            this.frictionDir = getVec3_zero();
            this.contactMotorDir = getVec3_zero();
            this.contactMotorVelocity = 0.0f;
            this.contactMotorForce = 0.0f;

            this.mass = 1.0f;
            this.invMass = 1.0f;
            this.centerOfMass = getVec3_zero();
            this.inertiaTensor = getMat3_identity();
            this.inverseInertiaTensor = getMat3_identity();

            this.current = this.state[0] = new AFBodyPState_s();
            this.next = this.state[1] = new AFBodyPState_s();
            this.current.worldOrigin = getVec3_zero();
            this.current.worldAxis = getMat3_identity();
            this.current.spatialVelocity = getVec6_zero();
            this.current.externalForce = getVec6_zero();
            this.next.oSet(this.current);
            this.saved = new AFBodyPState_s(this.current);
            this.atRestOrigin = getVec3_zero();
            this.atRestAxis = getMat3_identity();

            this.inverseWorldSpatialInertia = new idMatX();
            this.I = new idMatX();
            this.invI = new idMatX();
            this.J = new idMatX();
            this.s = new idVecX(6);
            this.totalForce = new idVecX(6);
            this.auxForce = new idVecX(6);
            this.acceleration = new idVecX(6);

            this.response = null;
            this.responseIndex = null;
            this.numResponses = 0;
            this.maxAuxiliaryIndex = 0;
            this.maxSubTreeAuxiliaryIndex = 0;

            this.fl = new bodyFlags_s();//	memset( &fl, 0, sizeof( fl ) );

            this.fl.selfCollision = true;
            this.fl.isZero = true;
        }

        public idStr GetName() {
            return this.name;
        }

        public idVec3 GetWorldOrigin() {
            return new idVec3(this.current.worldOrigin);
        }

        public idMat3 GetWorldAxis() {
            return new idMat3(this.current.worldAxis);
        }

        public idVec3 GetLinearVelocity() {
            return this.current.spatialVelocity.SubVec3(0);
        }

        public idVec3 GetAngularVelocity() {
            return this.current.spatialVelocity.SubVec3(1);
        }

        public idVec3 GetPointVelocity(final idVec3 point) {
            final idVec3 r = point.oMinus(this.current.worldOrigin);
            return this.current.spatialVelocity.SubVec3(0).oPlus(this.current.spatialVelocity.SubVec3(1).Cross(r));
        }

        public idVec3 GetCenterOfMass() {
            return this.centerOfMass;
        }

        public void SetClipModel(idClipModel clipModel) {
//	if ( this.clipModel && this.clipModel != clipModel ) {
//		delete this.clipModel;
//	}
            //blessed be the garbage collector
            this.clipModel = clipModel;
        }

        public idClipModel GetClipModel() {
            return this.clipModel;
        }

        public void SetClipMask(final int mask) {
            this.clipMask = mask;
            this.fl.clipMaskSet = true;
        }

        public int GetClipMask() {
            return this.clipMask;
        }

        public void SetSelfCollision(final boolean enable) {
            this.fl.selfCollision = enable;
        }

        public void SetWorldOrigin(final idVec3 origin) {
            this.current.worldOrigin.oSet(origin);
        }

        public void SetWorldAxis(final idMat3 axis) {
            this.current.worldAxis.oSet(axis);
        }

        public void SetLinearVelocity(final idVec3 linear) {
            this.current.spatialVelocity.SubVec3_oSet(0, linear);
            final int a = 0;
        }

        public void SetAngularVelocity(final idVec3 angular) {
            this.current.spatialVelocity.SubVec3_oSet(1, angular);
            final int a = 0;
        }

        public void SetFriction(float linear, float angular, float contact) {
            if ((linear < 0.0f) || (linear > 1.0f)
                    || (angular < 0.0f) || (angular > 1.0f)
                    || (contact < 0.0f)) {
                gameLocal.Warning("idAFBody::SetFriction: friction out of range, linear = %.1f, angular = %.1f, contact = %.1f", linear, angular, contact);
                return;
            }
            this.linearFriction = linear;
            this.angularFriction = angular;
            this.contactFriction = contact;
        }

        public float GetContactFriction() {
            return this.contactFriction;
        }

        public void SetBouncyness(float bounce) {
            if ((bounce < 0.0f) || (bounce > 1.0f)) {
                gameLocal.Warning("idAFBody::SetBouncyness: bouncyness out of range, bounce = %.1f", bounce);
                return;
            }
            this.bouncyness = bounce;
        }

        public float GetBouncyness() {
            return this.bouncyness;
        }

        private static int DBG_SetDensity = 0;
        public void SetDensity(float density, final idMat3 inertiaScale /*= mat3_identity*/) {DBG_SetDensity++;

            final float[] massTemp = {this.mass};

            // get the body mass properties
            this.clipModel.GetMassProperties(density, massTemp, this.centerOfMass, this.inertiaTensor);

            this.mass = massTemp[0];

            // make sure we have a valid mass
            if ((this.mass <= 0.0f) || FLOAT_IS_NAN(this.mass)) {
                gameLocal.Warning("idAFBody::SetDensity: invalid mass for body '%s'", this.name);
                this.mass = 1.0f;
                this.centerOfMass.Zero();
                this.inertiaTensor.Identity();
            }

            // make sure the center of mass is at the body origin
            if (!this.centerOfMass.Compare(Vector.getVec3_origin(), CENTER_OF_MASS_EPSILON)) {
                gameLocal.Warning("idAFBody::SetDentity: center of mass not at origin for body '%s'", this.name);
            }
            this.centerOfMass.Zero();

            // calculate the inverse mass and inverse inertia tensor
            this.invMass = 1.0f / this.mass;
            if (!inertiaScale.equals(getMat3_identity())) {
                this.inertiaTensor.oMulSet(inertiaScale);
                final int a = 0;
            }
            if (this.inertiaTensor.IsDiagonal(1e-3f)) {
                this.inertiaTensor.oSet(0, 1, this.inertiaTensor.oSet(0, 2, 0.0f));
                this.inertiaTensor.oSet(1, 0, this.inertiaTensor.oSet(1, 2, 0.0f));
                this.inertiaTensor.oSet(2, 0, this.inertiaTensor.oSet(2, 1, 0.0f));
                this.inverseInertiaTensor.Identity();
                this.inverseInertiaTensor.oSet(0, 0, 1.0f / this.inertiaTensor.oGet(0, 0));
                this.inverseInertiaTensor.oSet(1, 1, 1.0f / this.inertiaTensor.oGet(1, 1));
                this.inverseInertiaTensor.oSet(2, 2, 1.0f / this.inertiaTensor.oGet(2, 2));
                final int a = 0;
            } else {
                this.inverseInertiaTensor.oSet(this.inertiaTensor.Inverse());
                final int a = 0;
            }
        }

        public void SetDensity(float density) {
            SetDensity(density, getMat3_identity());
        }

        public float GetInverseMass() {
            return this.invMass;
        }

        public idMat3 GetInverseWorldInertia() {
            return this.current.worldAxis.Transpose().oMultiply(this.inverseInertiaTensor.oMultiply(this.current.worldAxis));
        }

        public void SetFrictionDirection(final idVec3 dir) {
            this.frictionDir.oSet(dir.oMultiply(this.current.worldAxis.Transpose()));
            this.fl.useFrictionDir = true;
        }

        public boolean GetFrictionDirection(idVec3 dir) {
            if (this.fl.useFrictionDir) {
                dir.oSet(this.frictionDir.oMultiply(this.current.worldAxis));
                return true;
            }
            return false;
        }

        public void SetContactMotorDirection(final idVec3 dir) {
            this.contactMotorDir.oSet(dir.oMultiply(this.current.worldAxis.Transpose()));
            this.fl.useContactMotorDir = true;
        }

        public boolean GetContactMotorDirection(idVec3 dir) {
            if (this.fl.useContactMotorDir) {
                dir.oSet(this.contactMotorDir.oMultiply(this.current.worldAxis));
                return true;
            }
            return false;
        }

        public void SetContactMotorVelocity(float vel) {
            this.contactMotorVelocity = vel;
        }

        public float GetContactMotorVelocity() {
            return this.contactMotorVelocity;
        }

        public void SetContactMotorForce(float force) {
            this.contactMotorForce = force;
        }

        public float GetContactMotorForce() {
            return this.contactMotorForce;
        }

        public void AddForce(final idVec3 point, final idVec3 force) {
            this.current.externalForce.SubVec3_oPluSet(0, force);
            this.current.externalForce.SubVec3_oPluSet(1, (point.oMinus(this.current.worldOrigin)).Cross(force));
        }

        /*
         ================
         idAFBody::InverseWorldSpatialInertiaMultiply

         dst = this->inverseWorldSpatialInertia * v;
         ================
         */
        public void InverseWorldSpatialInertiaMultiply(idVecX dst, final float[] v) {
            final float[] mPtr = this.inverseWorldSpatialInertia.ToFloatPtr();
            final float[] vPtr = v;
            final float[] dstPtr = dst.ToFloatPtr();

            if (this.fl.spatialInertiaSparse) {
                dstPtr[0] = mPtr[(0 * 6) + 0] * vPtr[0];
                dstPtr[1] = mPtr[(1 * 6) + 1] * vPtr[1];
                dstPtr[2] = mPtr[(2 * 6) + 2] * vPtr[2];
                dstPtr[3] = (mPtr[(3 * 6) + 3] * vPtr[3]) + (mPtr[(3 * 6) + 4] * vPtr[4]) + (mPtr[(3 * 6) + 5] * vPtr[5]);
                dstPtr[4] = (mPtr[(4 * 6) + 3] * vPtr[3]) + (mPtr[(4 * 6) + 4] * vPtr[4]) + (mPtr[(4 * 6) + 5] * vPtr[5]);
                dstPtr[5] = (mPtr[(5 * 6) + 3] * vPtr[3]) + (mPtr[(5 * 6) + 4] * vPtr[4]) + (mPtr[(5 * 6) + 5] * vPtr[5]);
            } else {
                gameLocal.Warning("spatial inertia is not sparse for body %s", this.name);
            }
        }

        /**@deprecated returns immutable response*/
        @Deprecated
        public idVec6 GetResponseForce(int index) {
//            return reinterpret_cast < idVec6 > (response[ index * 8]);
            return new idVec6(Arrays.copyOfRange(this.response, index * 8, (index * 8) + 6));
        }

        public void SetResponseForce(int index, final idVec6 v) {
            System.arraycopy(v.p, 0, this.response, index * 8, 6);
        }

        public void Save(idSaveGame saveFile) {
            saveFile.WriteFloat(this.linearFriction);
            saveFile.WriteFloat(this.angularFriction);
            saveFile.WriteFloat(this.contactFriction);
            saveFile.WriteFloat(this.bouncyness);
            saveFile.WriteInt(this.clipMask);
            saveFile.WriteVec3(this.frictionDir);
            saveFile.WriteVec3(this.contactMotorDir);
            saveFile.WriteFloat(this.contactMotorVelocity);
            saveFile.WriteFloat(this.contactMotorForce);

            saveFile.WriteFloat(this.mass);
            saveFile.WriteFloat(this.invMass);
            saveFile.WriteVec3(this.centerOfMass);
            saveFile.WriteMat3(this.inertiaTensor);
            saveFile.WriteMat3(this.inverseInertiaTensor);

            saveFile.WriteVec3(this.current.worldOrigin);
            saveFile.WriteMat3(this.current.worldAxis);
            saveFile.WriteVec6(this.current.spatialVelocity);
            saveFile.WriteVec6(this.current.externalForce);
            saveFile.WriteVec3(this.atRestOrigin);
            saveFile.WriteMat3(this.atRestAxis);
        }

        public void Restore(idRestoreGame saveFile) {
            this.linearFriction = saveFile.ReadFloat();
            this.angularFriction = saveFile.ReadFloat();
            this.contactFriction = saveFile.ReadFloat();
            this.bouncyness = saveFile.ReadFloat();
            this.clipMask = saveFile.ReadInt();
            saveFile.ReadVec3(this.frictionDir);
            saveFile.ReadVec3(this.contactMotorDir);
            this.contactMotorVelocity = saveFile.ReadFloat();
            this.contactMotorForce = saveFile.ReadFloat();

            this.mass = saveFile.ReadFloat();
            this.invMass = saveFile.ReadFloat();
            saveFile.ReadVec3(this.centerOfMass);
            saveFile.ReadMat3(this.inertiaTensor);
            saveFile.ReadMat3(this.inverseInertiaTensor);

            saveFile.ReadVec3(this.current.worldOrigin);
            saveFile.ReadMat3(this.current.worldAxis);
            saveFile.ReadVec6(this.current.spatialVelocity);
            saveFile.ReadVec6(this.current.externalForce);
            saveFile.ReadVec3(this.atRestOrigin);
            saveFile.ReadMat3(this.atRestAxis);
        }
    }

    //===============================================================
    //                                                        M
    //  idAFTree                                             MrE
    //                                                        E
    //===============================================================
    public static class idAFTree {
        // friend class idPhysics_AF;

        private final idList<idAFBody> sortedBodies = new idList<>();
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
            final idMatX childI = new idMatX();

            childI.SetData(6, 6, MATX_ALLOCA(6 * 6));

            // from the leaves up towards the root
            for (i = this.sortedBodies.Num() - 1; i >= 0; i--) {
                body = this.sortedBodies.oGet(i);

                if (body.children.Num() != 0) {

                    for (j = 0; j < body.children.Num(); j++) {

                        child = body.children.oGet(j).primaryConstraint;

                        // child.I = - child.body1.J.Transpose() * child.body1.I * child.body1.J;
                        childI.SetSize(child.J1.GetNumRows(), child.J1.GetNumRows());
                        child.body1.J.TransposeMultiply(child.body1.I).Multiply(childI, child.body1.J);
                        childI.Negate();

                        child.invI = new idMatX(childI);
                        if (!child.invI.InverseFastSelf()) {
                            gameLocal.Warning("idAFTree::Factor: couldn't invert %dx%d matrix for constraint '%s'",
                                    child.invI.GetNumRows(), child.invI.GetNumColumns(), child.GetName());
                        }
                        child.J = child.invI.oMultiply(child.J);

                        final float[] bodyI = body.I.ToFloatPtr().clone();
                        body.I.oMinSet(child.J.TransposeMultiply(childI).oMultiply(child.J));
                        final int a = 0;
                    }

                    body.invI.oSet(body.I);
                    if (!body.invI.InverseFastSelf()) {
                        gameLocal.Warning("idAFTree::Factor: couldn't invert %dx%d matrix for body %s",
                                child.invI.GetNumRows(), child.invI.GetNumColumns(), body.GetName());
                    }
                    if (body.primaryConstraint != null) {
                        final float[] J = body.J.ToFloatPtr().clone();
                        body.J.oSet(body.invI.oMultiply(body.J));
                        final int a = 0;
                    }
                } else if (body.primaryConstraint != null) {
                    final float[] J = body.J.ToFloatPtr().clone();
                    body.J.oSet(body.inverseWorldSpatialInertia.oMultiply(body.J));
                    final int a = 0;
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
            for (i = this.sortedBodies.Num() - 1; i >= 0; i--) {
                body = this.sortedBodies.oGet(i);

                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j);
                    primaryConstraint = child.primaryConstraint;

                    final float[] s = primaryConstraint.s.ToFloatPtr().clone();
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

            final boolean useSymmetry = af_useSymmetry.GetBool();

            // from the root down towards the leaves
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);
                primaryConstraint = body.primaryConstraint;

                if (primaryConstraint != null) {

                    if (useSymmetry && (body.parent.maxSubTreeAuxiliaryIndex < auxiliaryIndex)) {
                        continue;
                    }

                    final float[] s = primaryConstraint.s.ToFloatPtr().clone();
                    if (!primaryConstraint.fl.isZero) {
                        primaryConstraint.s = primaryConstraint.invI.oMultiply(primaryConstraint.s);
                    }
                    primaryConstraint.J.MultiplySub(primaryConstraint.s, primaryConstraint.body2.s);

                    primaryConstraint.lm.oSet(primaryConstraint.s);

                    if (useSymmetry && (body.maxSubTreeAuxiliaryIndex < auxiliaryIndex)) {
                        continue;
                    }

                    if (body.children.Num() != 0) {
                        if (!body.fl.isZero) {
                            body.s.oSet(body.invI.oMultiply(body.s));
                        }
                        body.J.MultiplySub(body.s, primaryConstraint.s);
                        final int a = 0;
                    }
                } else if (body.children.Num() != 0) {
                    final float[] s = body.s.p.clone();
                    body.s.oSet(body.invI.oMultiply(body.s));
                    final int a = 0;
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
            final idVecX v = new idVecX();

            // if a single body don't waste time because there aren't any primary constraints
            if (this.sortedBodies.Num() == 1) {
                body = constraint.body1;
                if (body.tree == this) {
                    body.SetResponseForce(body.numResponses, constraint.J1.SubVec6(row));
                    body.responseIndex[body.numResponses++] = auxiliaryIndex;
                } else {
                    body = constraint.body2;
                    body.SetResponseForce(body.numResponses, constraint.J2.SubVec6(row));
                    body.responseIndex[body.numResponses++] = auxiliaryIndex;
                }
                return;
            }

            v.SetData(6, VECX_ALLOCA(6));

            // initialize right hand side to zero
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.s.Zero();
                    primaryConstraint.fl.isZero = true;
                }
                body.s.Zero();
                body.fl.isZero = true;
                body.SetResponseForce(body.numResponses, Vector.getVec6_zero());
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
                body.SetResponseForce(body.numResponses, constraint.J1.SubVec6(row));
            }

            // set right hand side for second constrained body
            body = constraint.body2;
            if ((body != null) && body.tree.equals(this)) {
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
                body.SetResponseForce(body.numResponses, constraint.J2.SubVec6(row));
            }

            // solve for primary constraints
            Solve(auxiliaryIndex);

            final boolean useSymmetry = af_useSymmetry.GetBool();

            // store body forces in response to the constraint force
            final idVecX force = new idVecX();
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);

                if (useSymmetry && (body.maxAuxiliaryIndex < auxiliaryIndex)) {
                    continue;
                }

                final int from = body.numResponses * 8;
                final int to = from + 6;
                force.SetData(6, Arrays.copyOfRange(body.response, from, to));

                // add forces of all primary constraints acting on this body
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.TransposeMultiplyAdd(force, primaryConstraint.lm);
                }
                for (j = 0; j < body.children.Num(); j++) {   DBG_force++;
                    child = body.children.oGet(j).primaryConstraint;
                    child.J2.TransposeMultiplyAdd(force, child.lm);
                }

                System.arraycopy(force.p, 0, body.response, from, 6);
                body.responseIndex[body.numResponses++] = auxiliaryIndex;
            }
        }           private static int DBG_force = 0;

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
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);

                body.totalForce.SubVec6_oSet(0, body.current.externalForce.oPlus(body.auxForce.SubVec6(0)));
                final int a = 0;
            }

            // if a single body don't waste time because there aren't any primary constraints
            if (this.sortedBodies.Num() == 1) {
                return;
            }

            invStep = 1.0f / timeStep;

            // initialize right hand side
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);

                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.acceleration.SubVec6_oPluSet(0, body.current.spatialVelocity.oMultiply(invStep));
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
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);

                // add forces of all primary constraints acting on this body
                primaryConstraint = body.primaryConstraint;
                if (primaryConstraint != null) {
                    primaryConstraint.J1.TransposeMultiplyAdd(body.totalForce, primaryConstraint.lm);
                    final int a = 0;
                }
                for (j = 0; j < body.children.Num(); j++) {
                    child = body.children.oGet(j).primaryConstraint;
                    child.J2.TransposeMultiplyAdd(body.totalForce, child.lm);
                    final int a = 0;
                }
            }
        }

        public void SetMaxSubTreeAuxiliaryIndex() {
            int i, j;
            idAFBody body, child;

            // from the leaves up towards the root
            for (i = this.sortedBodies.Num() - 1; i >= 0; i--) {
                body = this.sortedBodies.oGet(i);

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
            for (i = 0; i < this.sortedBodies.Num(); i++) {
                if (null == this.sortedBodies.oGet(i).parent) {
                    break;
                }
            }

            if (i >= this.sortedBodies.Num()) {
                idGameLocal.Error("Articulated figure tree has no root.");
            }

            body = this.sortedBodies.oGet(i);
            this.sortedBodies.Clear();
            this.sortedBodies.Append(body);
            SortBodies_r(this.sortedBodies, body);
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

            for (i = 1; i < this.sortedBodies.Num(); i++) {
                body = this.sortedBodies.oGet(i);
                gameRenderWorld.DebugArrow(color, body.parent.current.worldOrigin, body.current.worldOrigin, 1);
            }
        }
    }

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

        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;

        public AFPState_s() {
            this.pushVelocity = new idVec6();
        }
    }

    public static class AFCollision_s {

        trace_s  trace;
        idAFBody body;
    }

    public static class idPhysics_AF extends idPhysics_Base {

        // articulated figure
        private final idList<idAFTree>               trees;                     // tree structures
        private final idList<idAFBody>               bodies;                    // all bodies
        private final idList<idAFConstraint>         constraints;               // all frame independent constraints
        private final idList<idAFConstraint>         primaryConstraints;        // list with primary constraints
        private final idList<idAFConstraint>         auxiliaryConstraints;      // list with auxiliary constraints
        private final idList<idAFConstraint>         frameConstraints;          // constraints that only live one frame
        private final idList<idAFConstraint_Contact> contactConstraints;        // contact constraints
        private final idList<Integer>                contactBodies;             // body id for each contact
        private final idList<AFCollision_s>          collisions;                // collisions
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
        private final idLCP                          lcp;                       // linear complementarity problem solver
//
//

        // CLASS_PROTOTYPE( idPhysics_AF );
        public idPhysics_AF() {
            this.trees = new idList<>();
            this.bodies = new idList<>();
            this.constraints = new idList<>();
            this.primaryConstraints = new idList<>();
            this.auxiliaryConstraints = new idList<>();
            this.frameConstraints = new idList<>();
            this.contactConstraints = new idList<>()     ;
            this.contactBodies = new idList<>();
            this.contacts = new idList<>();
            this.collisions = new idList<>();
            this.changedAF = true;
            this.masterBody = null;

            this.lcp = idLCP.AllocSymmetric();

            this.current = new AFPState_s();//memset( &current, 0, sizeof( current ) );
            this.current.atRest = -1;
            this.current.lastTimeStep = USERCMD_MSEC;
            this.saved = this.current;

            this.linearFriction = 0.005f;
            this.angularFriction = 0.005f;
            this.contactFriction = 0.8f;
            this.bouncyness = 0.4f;
            this.totalMass = 0.0f;
            this.forceTotalMass = -1.0f;

            this.suspendVelocity = new idVec2(SUSPEND_LINEAR_VELOCITY, SUSPEND_ANGULAR_VELOCITY);
            this.suspendAcceleration = new idVec2(SUSPEND_LINEAR_ACCELERATION, SUSPEND_LINEAR_ACCELERATION);
            this.noMoveTime = NO_MOVE_TIME;
            this.noMoveTranslation = NO_MOVE_TRANSLATION_TOLERANCE;
            this.noMoveRotation = NO_MOVE_ROTATION_TOLERANCE;
            this.minMoveTime = MIN_MOVE_TIME;
            this.maxMoveTime = MAX_MOVE_TIME;
            this.impulseThreshold = IMPULSE_THRESHOLD;

            this.timeScale = 1.0f;
            this.timeScaleRampStart = 0.0f;
            this.timeScaleRampEnd = 0.0f;

            this.jointFrictionScale = 0.0f;
            this.jointFrictionDent = 0.0f;
            this.jointFrictionDentStart = 0.0f;
            this.jointFrictionDentEnd = 0.0f;
            this.jointFrictionDentScale = 0.0f;

            this.contactFrictionScale = 0.0f;
            this.contactFrictionDent = 0.0f;
            this.contactFrictionDentStart = 0.0f;
            this.contactFrictionDentEnd = 0.0f;
            this.contactFrictionDentScale = 0.0f;

            this.enableCollision = true;
            this.selfCollision = true;
            this.comeToRest = true;
            this.linearTime = true;
            this.noImpact = false;
            this.worldConstraintsLocked = false;
            this.forcePushable = false;

            if (AF_TIMINGS) {
                lastTimerReset = 0;
            }
        }
        // ~idPhysics_AF();

        @Override
        public void Save(idSaveGame saveFile) {
            int i;

            // the articulated figure structure is handled by the owner
            idPhysics_AF_SavePState(saveFile, this.current);
            idPhysics_AF_SavePState(saveFile, this.saved);

            saveFile.WriteInt(this.bodies.Num());
            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).Save(saveFile);
            }
            if (this.masterBody != null) {
                saveFile.WriteBool(true);
                this.masterBody.Save(saveFile);
            } else {
                saveFile.WriteBool(false);
            }

            saveFile.WriteInt(this.constraints.Num());
            for (i = 0; i < this.constraints.Num(); i++) {
                this.constraints.oGet(i).Save(saveFile);
            }

            saveFile.WriteBool(this.changedAF);

            saveFile.WriteFloat(this.linearFriction);
            saveFile.WriteFloat(this.angularFriction);
            saveFile.WriteFloat(this.contactFriction);
            saveFile.WriteFloat(this.bouncyness);
            saveFile.WriteFloat(this.totalMass);
            saveFile.WriteFloat(this.forceTotalMass);

            saveFile.WriteVec2(this.suspendVelocity);
            saveFile.WriteVec2(this.suspendAcceleration);
            saveFile.WriteFloat(this.noMoveTime);
            saveFile.WriteFloat(this.noMoveTranslation);
            saveFile.WriteFloat(this.noMoveRotation);
            saveFile.WriteFloat(this.minMoveTime);
            saveFile.WriteFloat(this.maxMoveTime);
            saveFile.WriteFloat(this.impulseThreshold);

            saveFile.WriteFloat(this.timeScale);
            saveFile.WriteFloat(this.timeScaleRampStart);
            saveFile.WriteFloat(this.timeScaleRampEnd);

            saveFile.WriteFloat(this.jointFrictionScale);
            saveFile.WriteFloat(this.jointFrictionDent);
            saveFile.WriteFloat(this.jointFrictionDentStart);
            saveFile.WriteFloat(this.jointFrictionDentEnd);
            saveFile.WriteFloat(this.jointFrictionDentScale);

            saveFile.WriteFloat(this.contactFrictionScale);
            saveFile.WriteFloat(this.contactFrictionDent);
            saveFile.WriteFloat(this.contactFrictionDentStart);
            saveFile.WriteFloat(this.contactFrictionDentEnd);
            saveFile.WriteFloat(this.contactFrictionDentScale);

            saveFile.WriteBool(this.enableCollision);
            saveFile.WriteBool(this.selfCollision);
            saveFile.WriteBool(this.comeToRest);
            saveFile.WriteBool(this.linearTime);
            saveFile.WriteBool(this.noImpact);
            saveFile.WriteBool(this.worldConstraintsLocked);
            saveFile.WriteBool(this.forcePushable);
        }

        @Override
        public void Restore(idRestoreGame saveFile) {
            int i;
            final int[] num = {0};
            final boolean[] hasMaster = {false};

            // the articulated figure structure should have already been restored
            idPhysics_AF_RestorePState(saveFile, this.current);
            idPhysics_AF_RestorePState(saveFile, this.saved);

            saveFile.ReadInt(num);
            assert (num[0] == this.bodies.Num());
            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).Restore(saveFile);
            }
            saveFile.ReadBool(hasMaster);
            if (hasMaster[0]) {
                this.masterBody = new idAFBody();
                this.masterBody.Restore(saveFile);
            }

            saveFile.ReadInt(num);
            assert (num[0] == this.constraints.Num());
            for (i = 0; i < this.constraints.Num(); i++) {
                this.constraints.oGet(i).Restore(saveFile);
            }

            this.changedAF = saveFile.ReadBool();

            this.linearFriction = saveFile.ReadFloat();
            this.angularFriction = saveFile.ReadFloat();
            this.contactFriction = saveFile.ReadFloat();
            this.bouncyness = saveFile.ReadFloat();
            this.totalMass = saveFile.ReadFloat();
            this.forceTotalMass = saveFile.ReadFloat();

            saveFile.ReadVec2(this.suspendVelocity);
            saveFile.ReadVec2(this.suspendAcceleration);
            this.noMoveTime = saveFile.ReadFloat();
            this.noMoveTranslation = saveFile.ReadFloat();
            this.noMoveRotation = saveFile.ReadFloat();
            this.minMoveTime = saveFile.ReadFloat();
            this.maxMoveTime = saveFile.ReadFloat();
            this.impulseThreshold = saveFile.ReadFloat();

            this.timeScale = saveFile.ReadFloat();
            this.timeScaleRampStart = saveFile.ReadFloat();
            this.timeScaleRampEnd = saveFile.ReadFloat();

            this.jointFrictionScale = saveFile.ReadFloat();
            this.jointFrictionDent = saveFile.ReadFloat();
            this.jointFrictionDentStart = saveFile.ReadFloat();
            this.jointFrictionDentEnd = saveFile.ReadFloat();
            this.jointFrictionDentScale = saveFile.ReadFloat();

            this.contactFrictionScale = saveFile.ReadFloat();
            this.contactFrictionDent = saveFile.ReadFloat();
            this.contactFrictionDentStart = saveFile.ReadFloat();
            this.contactFrictionDentEnd = saveFile.ReadFloat();
            this.contactFrictionDentScale = saveFile.ReadFloat();

            this.enableCollision = saveFile.ReadBool();
            this.selfCollision = saveFile.ReadBool();
            this.comeToRest = saveFile.ReadBool();
            this.linearTime = saveFile.ReadBool();
            this.noImpact = saveFile.ReadBool();
            this.worldConstraintsLocked = saveFile.ReadBool();
            this.forcePushable = saveFile.ReadBool();

            this.changedAF = true;

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
                idGameLocal.Error("idPhysics_AF::AddBody: body '%s' has no clip model.", body.name);
            }

            if (this.bodies.Find(body) != null) {
                idGameLocal.Error("idPhysics_AF::AddBody: body '%s' added twice.", body.name);
            }

            if (GetBody(body.name.getData()) != null) {
                idGameLocal.Error("idPhysics_AF::AddBody: a body with the name '%s' already exists.", body.name);
            }

            id = this.bodies.Num();
            body.clipModel.SetId(id);
            if (body.linearFriction < 0.0f) {
                body.linearFriction = this.linearFriction;
                body.angularFriction = this.angularFriction;
                body.contactFriction = this.contactFriction;
            }
            if (body.bouncyness < 0.0f) {
                body.bouncyness = this.bouncyness;
            }
            if (!body.fl.clipMaskSet) {
                body.clipMask = this.clipMask;
            }

            this.bodies.Append(body);

            this.changedAF = true;

            return id;
        }

        public void AddConstraint(idAFConstraint constraint) {

            if (this.constraints.Find(constraint) != null) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: constraint '%s' added twice.", constraint.name);
            }
            if (GetConstraint(constraint.name.getData()) != null) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: a constraint with the name '%s' already exists.", constraint.name);
            }
            if (null == constraint.body1) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: body1 == NULL on constraint '%s'.", constraint.name);
            }
            if (null == this.bodies.Find(constraint.body1)) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: body1 of constraint '%s' is not part of the articulated figure.", constraint.name);
            }
            if ((constraint.body2 != null) && (null == this.bodies.Find(constraint.body2))) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: body2 of constraint '%s' is not part of the articulated figure.", constraint.name);
            }
            if (constraint.body1.equals(constraint.body2)) {
                idGameLocal.Error("idPhysics_AF::AddConstraint: body1 and body2 of constraint '%s' are the same.", constraint.name);
            }

            this.constraints.Append(constraint);
            constraint.physics = this;

            this.changedAF = true;
        }

        public void AddFrameConstraint(idAFConstraint constraint) {
            this.frameConstraints.Append(constraint);
            constraint.physics = this;
        }

        // force a body to have a certain id
        public void ForceBodyId(idAFBody body, int newId) {
            int id;

            id = this.bodies.FindIndex(body);
            if (id == -1) {
                idGameLocal.Error("ForceBodyId: body '%s' is not part of the articulated figure.\n", body.name);
            }
            if (id != newId) {
                final idAFBody b = this.bodies.oGet(newId);
                this.bodies.oSet(newId, this.bodies.oGet(id));
                this.bodies.oSet(id, b);
                this.changedAF = true;
            }
        }

        // get body or constraint id
        public int GetBodyId(idAFBody body) {
            int id;

            id = this.bodies.FindIndex(body);
            if ((id == -1) && (body != null)) {//TODO:can't be null
                idGameLocal.Error("GetBodyId: body '%s' is not part of the articulated figure.\n", body.name);
            }
            return id;
        }

        public int GetBodyId(final String bodyName) {
            int i;

            for (i = 0; i < this.bodies.Num(); i++) {
                if (0 == this.bodies.oGet(i).name.Icmp(bodyName)) {
                    return i;
                }
            }
            idGameLocal.Error("GetBodyId: no body with the name '%s' is not part of the articulated figure.\n", bodyName);
            return 0;
        }

        public int GetConstraintId(idAFConstraint constraint) {
            int id;

            id = this.constraints.FindIndex(constraint);
            if ((id == -1) && (constraint != null)) {//TODO:can't be null
                idGameLocal.Error("GetConstraintId: constraint '%s' is not part of the articulated figure.\n", constraint.name);
            }
            return id;
        }

        public int GetConstraintId(final String constraintName) {
            int i;

            for (i = 0; i < this.constraints.Num(); i++) {
                if (this.constraints.oGet(i).name.Icmp(constraintName) == 0) {
                    return i;
                }
            }
            idGameLocal.Error("GetConstraintId: no constraint with the name '%s' is not part of the articulated figure.\n", constraintName);
            return 0;
        }

        // number of bodies and constraints
        public int GetNumBodies() {
            return this.bodies.Num();
        }

        public int GetNumConstraints() {
            return this.constraints.Num();
        }

        // retrieve body or constraint
        public idAFBody GetBody(final String bodyName) {
            int i;

            for (i = 0; i < this.bodies.Num(); i++) {
                if (0 == this.bodies.oGet(i).name.Icmp(bodyName)) {
                    return this.bodies.oGet(i);
                }
            }

            return null;
        }

        public idAFBody GetBody(final idStr bodyName) {
            return GetBody(bodyName.getData());
        }

        public idAFBody GetBody(final int id) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                idGameLocal.Error("GetBody: no body with id %d exists\n", id);
                return null;
            }
            return this.bodies.oGet(id);
        }

        public idAFBody GetMasterBody() {
            return this.masterBody;
        }

        public idAFConstraint GetConstraint(final String constraintName) {
            int i;

            for (i = 0; i < this.constraints.Num(); i++) {
                if (this.constraints.oGet(i).name.Icmp(constraintName) == 0) {
                    return this.constraints.oGet(i);
                }
            }

            return null;
        }

        public idAFConstraint GetConstraint(final int id) {
            if ((id < 0) || (id >= this.constraints.Num())) {
                idGameLocal.Error("GetConstraint: no constraint with id %d exists\n", id);
                return null;
            }
            return this.constraints.oGet(id);
        }

        // delete body or constraint
        public void DeleteBody(final String bodyName) {
            int i;

            // find the body with the given name
            for (i = 0; i < this.bodies.Num(); i++) {
                if (0 == this.bodies.oGet(i).name.Icmp(bodyName)) {
                    break;
                }
            }

            if (i >= this.bodies.Num()) {
                gameLocal.Warning("DeleteBody: no body found in the articulated figure with the name '%s' for entity '%s' type '%s'.",
                        bodyName, this.self.name, this.self.GetType().getName());
                return;
            }

            DeleteBody(i);
        }

        public void DeleteBody(final int id) {
            int j;

            if ((id < 0) || (id > this.bodies.Num())) {
                idGameLocal.Error("DeleteBody: no body with id %d.", id);
                return;
            }

            // remove any constraints attached to this body
            for (j = 0; j < this.constraints.Num(); j++) {
                if (this.constraints.oGet(j).body1.equals(this.bodies.oGet(id)) || this.constraints.oGet(j).body2.equals(this.bodies.oGet(id))) {
//			delete constraints[j];
                    this.constraints.RemoveIndex(j);
                    j--;
                }
            }

            // remove the body
//	delete bodies[id];
            this.bodies.RemoveIndex(id);

            // set new body ids
            for (j = 0; j < this.bodies.Num(); j++) {
                this.bodies.oGet(j).clipModel.SetId(j);
            }

            this.changedAF = true;
        }

        public void DeleteConstraint(final String constraintName) {
            int i;

            // find the constraint with the given name
            for (i = 0; i < this.constraints.Num(); i++) {
                if (NOT(this.constraints.oGet(i).name.Icmp(constraintName))) {
                    break;
                }
            }

            if (i >= this.constraints.Num()) {
                gameLocal.Warning("DeleteConstraint: no constriant found in the articulated figure with the name '%s' for entity '%s' type '%s'.",
                        constraintName, this.self.name, this.self.GetType().getName());
                return;
            }

            DeleteConstraint(i);
        }

        public void DeleteConstraint(final int id) {

            if ((id < 0) || (id >= this.constraints.Num())) {
                idGameLocal.Error("DeleteConstraint: no constraint with id %d.", id);
                return;
            }

            // remove the constraint
//	delete constraints[id];
            this.constraints.RemoveIndex(id);

            this.changedAF = true;
        }

        // get all the contact constraints acting on the body
        public int GetBodyContactConstraints(final int id, idAFConstraint_Contact[] contacts, int maxContacts) {
            int i, numContacts;
            idAFBody body;
            idAFConstraint_Contact contact;

            if ((id < 0) || (id >= this.bodies.Num()) || (maxContacts <= 0)) {
                return 0;
            }

            numContacts = 0;
            body = this.bodies.oGet(id);
            for (i = 0; i < this.contactConstraints.Num(); i++) {
                contact = this.contactConstraints.oGet(i);
                if ((contact.body1 == body) || (contact.body2 == body)) {
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
            if ((linear < 0.0f) || (linear > 1.0f)
                    || (angular < 0.0f) || (angular > 1.0f)
                    || (contact < 0.0f) || (contact > 1.0f)) {
                return;
            }
            this.linearFriction = linear;
            this.angularFriction = angular;
            this.contactFriction = contact;
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
            this.timeScale = ts;
        }

        // set time scale ramp
        public void SetTimeScaleRamp(final float start, final float end) {
            this.timeScaleRampStart = start;
            this.timeScaleRampEnd = end;
        }

        // set the joint friction scale
        public void SetJointFrictionScale(final float scale) {
            this.jointFrictionScale = scale;
        }
        // set joint friction dent

        public void SetJointFrictionDent(final float dent, final float start, final float end) {
            this.jointFrictionDent = dent;
            this.jointFrictionDentStart = start;
            this.jointFrictionDentEnd = end;
        }

        // get the current joint friction scale
        public float GetJointFrictionScale() {
            if (this.jointFrictionDentScale > 0.0f) {
                return this.jointFrictionDentScale;
            } else if (this.jointFrictionScale > 0.0f) {
                return this.jointFrictionScale;
            } else if (af_jointFrictionScale.GetFloat() > 0.0f) {
                return af_jointFrictionScale.GetFloat();
            }
            return 1.0f;
        }

        // set the contact friction scale
        public void SetContactFrictionScale(final float scale) {
            this.contactFrictionScale = scale;
        }

        // set contact friction dent
        public void SetContactFrictionDent(final float dent, final float start, final float end) {
            this.contactFrictionDent = dent;
            this.contactFrictionDentStart = start;
            this.contactFrictionDentEnd = end;
        }

        // get the current contact friction scale
        public float GetContactFrictionScale() {
            if (this.contactFrictionDentScale > 0.0f) {
                return this.contactFrictionDentScale;
            } else if (this.contactFrictionScale > 0.0f) {
                return this.contactFrictionScale;
            } else if (af_contactFrictionScale.GetFloat() > 0.0f) {
                return af_contactFrictionScale.GetFloat();
            }
            return 1.0f;
        }

        // enable or disable collision detection
        public void SetCollision(final boolean enable) {
            this.enableCollision = enable;
        }

        // enable or disable self collision
        public void SetSelfCollision(final boolean enable) {
            this.selfCollision = enable;
        }

        // enable or disable coming to a dead stop
        public void SetComeToRest(boolean enable) {
            this.comeToRest = enable;
        }

        // call when structure of articulated figure changes
        public void SetChanged() {
            this.changedAF = true;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            this.noImpact = false;
        }

        public void DisableImpact() {
            this.noImpact = true;
        }

        // lock of unlock the world constraints
        public void LockWorldConstraints(final boolean lock) {
            this.worldConstraintsLocked = lock;
        }

        // set force pushable
        public void SetForcePushable(final boolean enable) {
            this.forcePushable = enable;
        }

        // update the clip model positions
        public void UpdateClipModels() {
            int i;
            idAFBody body;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);
                body.clipModel.Link(gameLocal.clip, this.self, body.clipModel.GetId(), body.current.worldOrigin, body.current.worldAxis);
            }
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            if ((id >= 0) && (id < this.bodies.Num())) {
                return this.bodies.oGet(id).GetClipModel();
            }
            return null;
        }

        @Override
        public int GetNumClipModels() {
            return this.bodies.Num();
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
            if ((id >= 0) && (id < this.bodies.Num())) {
            } else {
                this.forceTotalMass = mass;
            }
            SetChanged();
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            if ((id >= 0) && (id < this.bodies.Num())) {
                return this.bodies.oGet(id).mass;
            }
            return this.totalMass;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            int i;

            if ((id >= 0) && (id < this.bodies.Num())) {
                this.bodies.oGet(id).GetClipModel().SetContents(contents);
            } else {
                for (i = 0; i < this.bodies.Num(); i++) {
                    this.bodies.oGet(i).GetClipModel().SetContents(contents);
                }
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            int i, contents;

            if ((id >= 0) && (id < this.bodies.Num())) {
                return this.bodies.oGet(id).GetClipModel().GetContents();
            } else {
                contents = 0;
                for (i = 0; i < this.bodies.Num(); i++) {
                    contents |= this.bodies.oGet(i).GetClipModel().GetContents();
                }
                return contents;
            }
        }

        private static idBounds relBounds;

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            int i;

            if ((id >= 0) && (id < this.bodies.Num())) {
                return this.bodies.oGet(id).GetClipModel().GetBounds();
            } else if (0 == this.bodies.Num()) {
                relBounds.Zero();
                return relBounds;
            } else {
                relBounds = this.bodies.oGet(0).GetClipModel().GetBounds();
                for (i = 1; i < this.bodies.Num(); i++) {
                    final idBounds bounds = new idBounds();
                    final idVec3 origin = (this.bodies.oGet(i).GetWorldOrigin().oMinus(this.bodies.oGet(0).GetWorldOrigin())).oMultiply(this.bodies.oGet(0).GetWorldAxis().Transpose());
                    final idMat3 axis = this.bodies.oGet(i).GetWorldAxis().oMultiply(this.bodies.oGet(0).GetWorldAxis().Transpose());
                    bounds.FromTransformedBounds(this.bodies.oGet(i).GetClipModel().GetBounds(), origin, axis);
                    relBounds.oPluSet(bounds);
                }
                return relBounds;
            }
        }

        private static idBounds absBounds;

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            int i;

            if ((id >= 0) && (id < this.bodies.Num())) {
                return this.bodies.oGet(id).GetClipModel().GetAbsBounds();
            } else if (0 == this.bodies.Num()) {
                absBounds.Zero();
                return absBounds;
            } else {
                absBounds = this.bodies.oGet(0).GetClipModel().GetAbsBounds();
                for (i = 1; i < this.bodies.Num(); i++) {
                    absBounds.oPluSet(this.bodies.oGet(i).GetClipModel().GetAbsBounds());
                }
                return absBounds;
            }
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            float timeStep;

            if ((this.timeScaleRampStart < MS2SEC(endTimeMSec)) && (this.timeScaleRampEnd > MS2SEC(endTimeMSec))) {
                timeStep = (MS2SEC(timeStepMSec) * (MS2SEC(endTimeMSec) - this.timeScaleRampStart)) / (this.timeScaleRampEnd - this.timeScaleRampStart);
            } else if (af_timeScale.GetFloat() != 1.0f) {
                timeStep = MS2SEC(timeStepMSec) * af_timeScale.GetFloat();
            } else {
                timeStep = MS2SEC(timeStepMSec) * this.timeScale;
            }
            this.current.lastTimeStep = timeStep;

            // if the articulated figure changed
            if (this.changedAF || (this.linearTime != af_useLinearTime.GetBool())) {
                BuildTrees();
                this.changedAF = false;
                this.linearTime = af_useLinearTime.GetBool();
            }

            // get the new master position
            if (this.masterBody != null) {
                final idVec3 masterOrigin = new idVec3();
                final idMat3 masterAxis = new idMat3();
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                if ((this.current.atRest >= 0) && ((this.masterBody.current.worldOrigin != masterOrigin) || (this.masterBody.current.worldAxis != masterAxis))) {
                    Activate();
                }
                this.masterBody.current.worldOrigin.oSet(masterOrigin);
                this.masterBody.current.worldAxis.oSet(masterAxis);
                final int a = 0;
            }

            // if the simulation is suspended because the figure is at rest
            if ((this.current.atRest >= 0) || (timeStep <= 0.0f)) {
                DebugDraw();
                return false;
            }

            // move the af velocity into the frame of a pusher
            AddPushVelocity(this.current.pushVelocity.oNegative());

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
                for (i = 0; i < this.primaryConstraints.Num(); i++) {
                    numPrimary += this.primaryConstraints.oGet(i).J1.GetNumRows();
                }
                for (i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                    numAuxiliary += this.auxiliaryConstraints.oGet(i).J1.GetNumRows();
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
            if (this.selfCollision && !af_skipSelfCollision.GetBool()) {
                DisableClip();
            }

            // apply collision impulses
            if (ApplyCollisions(timeStep)) {
                this.current.atRest = gameLocal.time;
                this.comeToRest = true;
            }

            // test if the simulation can be suspended because the whole figure is at rest
            if (this.comeToRest && TestIfAtRest(timeStep)) {
                Rest();
            } else {
                ActivateContactEntities();
            }

            // add gravitational force
            AddGravity();

            // move the af velocity back into the world frame
            AddPushVelocity(this.current.pushVelocity);
            this.current.pushVelocity.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("articulated figure moved outside world bounds for entity '%s' type '%s' at (%s)",
                        this.self.name, this.self.GetType().getName(), this.bodies.oGet(0).current.worldOrigin.ToString(0));
                Rest();
            }

            if (AF_TIMINGS) {
                timer_total.Stop();

                if (af_showTimings.GetInteger() == 1) {
                    gameLocal.Printf("%12s: t %1.4f pc %2d, %1.4f ac %2d %1.4f lcp %1.4f cd %1.4f\n",
                            this.self.name,
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
        public impactInfo_s GetImpactInfo(final int id, final idVec3 point) {
            final impactInfo_s info = new impactInfo_s();
            if ((id < 0) || (id >= this.bodies.Num())) {
                return info;
            }
            info.invMass = 1.0f / this.bodies.oGet(id).mass;
            info.invInertiaTensor = this.bodies.oGet(id).current.worldAxis.Transpose().oMultiply(this.bodies.oGet(id).inverseInertiaTensor.oMultiply(this.bodies.oGet(id).current.worldAxis));
            info.position = point.oMinus(this.bodies.oGet(id).current.worldOrigin);
            info.velocity = this.bodies.oGet(id).current.spatialVelocity.SubVec3(0).oPlus(this.bodies.oGet(id).current.spatialVelocity.SubVec3(1).Cross(info.position));
            return info;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return;
            }
            if (this.noImpact || (impulse.LengthSqr() < Square(this.impulseThreshold))) {
                return;
            }
            final idMat3 invWorldInertiaTensor = this.bodies.oGet(id).current.worldAxis.Transpose().oMultiply(this.bodies.oGet(id).inverseInertiaTensor.oMultiply(this.bodies.oGet(id).current.worldAxis));
            this.bodies.oGet(id).current.spatialVelocity.SubVec3_oPluSet(0, impulse.oMultiply(this.bodies.oGet(id).invMass));
            this.bodies.oGet(id).current.spatialVelocity.SubVec3_oPluSet(1, invWorldInertiaTensor.oMultiply((point.oMinus(this.bodies.oGet(id).current.worldOrigin)).Cross(impulse)));
            Activate();
        }

        @Override
        public void AddForce(final int id, final idVec3 point, final idVec3 force) {
            if (this.noImpact) {
                return;
            }
            if ((id < 0) || (id >= this.bodies.Num())) {
                return;
            }
            this.bodies.oGet(id).current.externalForce.SubVec3_oPluSet(0, force);
            this.bodies.oGet(id).current.externalForce.SubVec3_oPluSet(1, (point.oMinus(this.bodies.oGet(id).current.worldOrigin)).Cross(force));
            Activate();
        }

        @Override
        public boolean IsAtRest() {
            return this.current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return this.current.atRest;
        }

        @Override
        public void Activate() {
            // if the articulated figure was at rest
            if (this.current.atRest >= 0) {
                // normally gravity is added at the end of a simulation frame
                // if the figure was at rest add gravity here so it is applied this simulation frame
                AddGravity();
                // reset the active time for the max move time
                this.current.activateTime = 0.0f;
            }
            this.current.atRest = -1;
            this.current.noMoveTime = 0.0f;
            this.self.BecomeActive(TH_PHYSICS);
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
            return (!this.noImpact && ((this.masterBody == null) || this.forcePushable));
        }

        @Override
        public void SaveState() {
            int i;

            this.saved = this.current;

            for (i = 0; i < this.bodies.Num(); i++) {
//                memcpy(bodies.oGet(i).saved, bodies.oGet(i).current, sizeof(AFBodyPState_t));
                this.bodies.oGet(i).saved.oSet(this.bodies.oGet(i).current);
            }
        }

        @Override
        public void RestoreState() {
            int i;

            this.current = this.saved;

            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).current.oSet(this.bodies.oGet(i).saved);
            }

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            if (this.masterBody != null) {
                Translate(this.masterBody.current.worldOrigin.oPlus(this.masterBody.current.worldAxis.oMultiply(newOrigin).oMinus(this.bodies.oGet(0).current.worldOrigin)));
            } else {
                Translate(newOrigin.oMinus(this.bodies.oGet(0).current.worldOrigin));
            }
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idMat3 axis;
            idRotation rotation;

            if (this.masterBody != null) {
                axis = this.bodies.oGet(0).current.worldAxis.Transpose().oMultiply(newAxis.oMultiply(this.masterBody.current.worldAxis));
            } else {
                axis = this.bodies.oGet(0).current.worldAxis.Transpose().oMultiply(newAxis);
            }
            rotation = axis.ToRotation();
            rotation.SetOrigin(this.bodies.oGet(0).current.worldOrigin);

            Rotate(rotation);
        }

        private static int DBG_Translate = 0;
        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {  DBG_Translate++;
            int i;
            idAFBody body;

            if (!this.worldConstraintsLocked) {
                // translate constraints attached to the world
                for (i = 0; i < this.constraints.Num(); i++) {
                    this.constraints.oGet(i).Translate(translation);
                }
            }

            // translate all the bodies
            for (i = 0; i < this.bodies.Num(); i++) {

                body = this.bodies.oGet(i);
                body.current.worldOrigin.oPluSet(translation);
                final int a = 0;
            }

            Activate();

            UpdateClipModels();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            int i;
            idAFBody body;

            if (!this.worldConstraintsLocked) {
                // rotate constraints attached to the world
                for (i = 0; i < this.constraints.Num(); i++) {
                    this.constraints.oGet(i).Rotate(rotation);
                }
            }

            // rotate all the bodies
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                final idMat3 old = body.GetWorldAxis();
                body.current.worldOrigin.oMulSet(rotation);
                body.current.worldAxis.oMulSet(rotation.ToMat3());
                final int a = 0;
            }

            Activate();

            UpdateClipModels();
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return getVec3_origin();
            } else {
                return new idVec3(this.bodies.oGet(id).current.worldOrigin);
            }
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return getMat3_identity();
            } else {
                return new idMat3(this.bodies.oGet(id).current.worldAxis);
            }
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return;
            }
            this.bodies.oGet(id).current.spatialVelocity.SubVec3_oSet(0, newLinearVelocity);
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return;
            }
            this.bodies.oGet(id).current.spatialVelocity.SubVec3_oSet(1, newAngularVelocity);
            Activate();
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return getVec3_origin();
            } else {
                return this.bodies.oGet(id).current.spatialVelocity.SubVec3(0);
            }
        }

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            if ((id < 0) || (id >= this.bodies.Num())) {
                return getVec3_origin();
            } else {
                return this.bodies.oGet(id).current.spatialVelocity.SubVec3(1);
            }
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
            int i;
            idAFBody body;
            final trace_s[] bodyResults = {new trace_s()};

            results[0].fraction = 1.0f;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                if (body.clipModel.IsTraceModel()) {
                    if (model != null) {
                        gameLocal.clip.TranslationModel(bodyResults, body.current.worldOrigin, body.current.worldOrigin.oPlus(translation),
                                body.clipModel, body.current.worldAxis, body.clipMask,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        gameLocal.clip.Translation(bodyResults, body.current.worldOrigin, body.current.worldOrigin.oPlus(translation),
                                body.clipModel, body.current.worldAxis, body.clipMask, this.self);
                    }
                    if (bodyResults[0].fraction < results[0].fraction) {
                        results[0].oSet(bodyResults[0]);
                    }
                }
            }

            results[0].endpos.oSet(this.bodies.oGet(0).current.worldOrigin.oPlus(translation.oMultiply(results[0].fraction)));
            results[0].endAxis.oSet(this.bodies.oGet(0).current.worldAxis);
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            int i;
            idAFBody body;
            final trace_s[] bodyResults = {null};
            idRotation partialRotation;

            results[0].fraction = 1.0f;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                if (body.clipModel.IsTraceModel()) {
                    if (model != null) {
                        gameLocal.clip.RotationModel(bodyResults, body.current.worldOrigin, rotation,
                                body.clipModel, body.current.worldAxis, body.clipMask,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        gameLocal.clip.Rotation(bodyResults, body.current.worldOrigin, rotation,
                                body.clipModel, body.current.worldAxis, body.clipMask, this.self);
                    }
                    if (bodyResults[0].fraction < results[0].fraction) {
                        results[0] = bodyResults[0];
                    }
                }
            }

            partialRotation = rotation.oMultiply(results[0].fraction);
            results[0].endpos.oSet(this.bodies.oGet(0).current.worldOrigin.oMultiply(partialRotation));
            results[0].endAxis.oSet(this.bodies.oGet(0).current.worldAxis.oMultiply(partialRotation.ToMat3()));
        }

        @Override
        public int ClipContents(final idClipModel model) {
            int i, contents;
            idAFBody body;

            contents = 0;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

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

            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            int i;

            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            int i;

            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            UpdateClipModels();
        }

        @Override
        public boolean EvaluateContacts() {
            int i, j, k, numContacts, numBodyContacts;
            idEntity passEntity;
            final idVecX dir = new idVecX(6, VECX_ALLOCA(6));

            // evaluate bodies
            EvaluateBodies(this.current.lastTimeStep);

            // remove all existing contacts
            ClearContacts();

            this.contactBodies.SetNum(0, false);

            if (!this.enableCollision) {
                return false;
            }

            // find all the contacts
            for (i = 0; i < this.bodies.Num(); i++) {
                final idAFBody body = this.bodies.oGet(i);
                final contactInfo_t[] contactInfo = new contactInfo_t[10];

                if (body.clipMask == 0) {
                    continue;
                }

                passEntity = SetupCollisionForBody(body);

                body.InverseWorldSpatialInertiaMultiply(dir, body.current.externalForce.ToFloatPtr());
                dir.SubVec6_oSet(0, body.current.spatialVelocity.oPlus(dir.SubVec6(0).oMultiply(this.current.lastTimeStep)));
                dir.SubVec3_Normalize(0);
                dir.SubVec3_Normalize(1);

                numContacts = gameLocal.clip.Contacts(contactInfo, 10, body.current.worldOrigin, dir.SubVec6(0), 2.0f, //CONTACT_EPSILON,
                        body.clipModel, body.current.worldAxis, body.clipMask, passEntity);

                if (true) {
                    // merge nearby contacts between the same bodies
                    // and assure there are at most three planar contacts between any pair of bodies
                    for (j = 0; j < numContacts; j++) {

                        numBodyContacts = 0;
                        for (k = 0; k < this.contacts.Num(); k++) {
                            if (this.contacts.oGet(k).entityNum == contactInfo[j].entityNum) {
                                if (((this.contacts.oGet(k).id == i) && (contactInfo[j].id == this.contactBodies.oGet(k)))
                                        || ((this.contactBodies.oGet(k) == i) && (this.contacts.oGet(k).id == contactInfo[j].id))) {

                                    if ((this.contacts.oGet(k).point.oMinus(contactInfo[j].point)).LengthSqr() < Square(2.0f)) {
                                        break;
                                    }
                                    if (idMath.Fabs(this.contacts.oGet(k).normal.oMultiply(contactInfo[j].normal)) > 0.9f) {
                                        numBodyContacts++;
                                    }
                                }
                            }
                        }

                        if ((k >= this.contacts.Num()) && (numBodyContacts < 3)) {
                            this.contacts.Append(contactInfo[j]);
                            this.contactBodies.Append(i);
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

            return (this.contacts.Num() != 0);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idAFBody body;
            idRotation rotation;

            if (this.bodies.Num() != 0) {
                body = this.bodies.oGet(0);
                rotation = (body.saved.worldAxis.Transpose().oMultiply(body.current.worldAxis).ToRotation());

                // velocity with which the af is pushed
                this.current.pushVelocity.SubVec3_oPluSet(0, (body.current.worldOrigin.oMinus(body.saved.worldOrigin)).oDivide(deltaTime * idMath.M_MS2SEC));
                this.current.pushVelocity.SubVec3_oPluSet(1, rotation.GetVec().oMultiply(-DEG2RAD(rotation.GetAngle())).oDivide(deltaTime * idMath.M_MS2SEC));
            }
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity.SubVec3(0);
        }

        @Override
        public idVec3 GetPushedAngularVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity.SubVec3(1);
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
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();
            idRotation rotation;

            if (master != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                if (null == this.masterBody) {
                    this.masterBody = new idAFBody();
                    // translate and rotate all the constraints with body2 == NULL from world space to master space
                    rotation = masterAxis.Transpose().ToRotation();
                    for (i = 0; i < this.constraints.Num(); i++) {
                        if (this.constraints.oGet(i).GetBody2() == null) {
                            this.constraints.oGet(i).Translate(masterOrigin.oNegative());
                            this.constraints.oGet(i).Rotate(rotation);
                        }
                    }
                    Activate();
                }
                this.masterBody.current.worldOrigin.oSet(masterOrigin);
                this.masterBody.current.worldAxis.oSet(masterAxis);
                final int a = 0;
            } else if (this.masterBody != null) {
                // translate and rotate all the constraints with body2 == NULL from master space to world space
                rotation = this.masterBody.current.worldAxis.ToRotation();
                for (i = 0; i < this.constraints.Num(); i++) {
                    if (this.constraints.oGet(i).GetBody2() == null) {
                        this.constraints.oGet(i).Rotate(rotation);
                        this.constraints.oGet(i).Translate(this.masterBody.current.worldOrigin);
                    }
                }
//			delete masterBody;
                this.masterBody = null;
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

            msg.WriteLong(this.current.atRest);
            msg.WriteFloat(this.current.noMoveTime);
            msg.WriteFloat(this.current.activateTime);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(0), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(1), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(2), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(3), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(4), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(5), AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS);

            msg.WriteByte(this.bodies.Num());

            for (i = 0; i < this.bodies.Num(); i++) {
                final AFBodyPState_s state = this.bodies.oGet(i).current;
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
            final idCQuat quat = new idCQuat();

            this.current.atRest = msg.ReadLong();
            this.current.noMoveTime = msg.ReadFloat();
            this.current.activateTime = msg.ReadFloat();
            this.current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(3, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(4, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(5, msg.ReadDeltaFloat(0.0f, AF_VELOCITY_EXPONENT_BITS, AF_VELOCITY_MANTISSA_BITS));

            num = msg.ReadByte();
            assert (num == this.bodies.Num());

            for (i = 0; i < this.bodies.Num(); i++) {
                final AFBodyPState_s state = this.bodies.oGet(i).current;

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
                final int a = 0;
            }

            UpdateClipModels();
        }

        private void BuildTrees() {
            int i;
            float scale;
            idAFBody b;
            idAFConstraint c;
            idAFTree tree;

            this.primaryConstraints.Clear();
            this.auxiliaryConstraints.Clear();
            this.trees.DeleteContents(true);

            this.totalMass = 0.0f;
            for (i = 0; i < this.bodies.Num(); i++) {
                b = this.bodies.oGet(i);
                b.parent = null;
                b.primaryConstraint = null;
                b.constraints.SetNum(0, false);
                b.children.Clear();
                b.tree = null;
                this.totalMass += b.mass;
            }

            if (this.forceTotalMass > 0.0f) {
                scale = this.forceTotalMass / this.totalMass;
                for (i = 0; i < this.bodies.Num(); i++) {
                    b = this.bodies.oGet(i);
                    b.mass *= scale;
                    b.invMass = 1.0f / b.mass;
                    b.inertiaTensor.oMulSet(scale);
                    b.inverseInertiaTensor.oSet(b.inertiaTensor.Inverse());
                    final int a =0;
                }
                this.totalMass = this.forceTotalMass;
            }

            if (af_useLinearTime.GetBool()) {

                for (i = 0; i < this.constraints.Num(); i++) {
                    c = this.constraints.oGet(i);

                    c.body1.constraints.Append(c);
                    if (c.body2 != null) {
                        c.body2.constraints.Append(c);
                    }

                    // only bilateral constraints between two non-world bodies that do not
                    // create loops can be used as primary constraints
                    if ((null == c.body1.primaryConstraint) && c.fl.allowPrimary && (c.body2 != null) && !IsClosedLoop(c.body1, c.body2)) {
                        c.body1.primaryConstraint = c;
                        c.body1.parent = c.body2;
                        c.body2.children.Append(c.body1);
                        c.fl.isPrimary = true;
                        c.firstIndex = 0;
                        this.primaryConstraints.Append(c);
                    } else {
                        c.fl.isPrimary = false;
                        this.auxiliaryConstraints.Append(c);
                    }
                }

                // create trees for all parent bodies
                for (i = 0; i < this.bodies.Num(); i++) {
                    if (null == this.bodies.oGet(i).parent) {
                        tree = new idAFTree();
                        tree.sortedBodies.Clear();
                        tree.sortedBodies.Append(this.bodies.oGet(i));
                        this.bodies.oGet(i).tree = tree;
                        this.trees.Append(tree);
                    }
                }

                // add each child body to the appropriate tree
                for (i = 0; i < this.bodies.Num(); i++) {
                    if (this.bodies.oGet(i).parent != null) {
                        for (b = this.bodies.oGet(i).parent; null == b.tree; b = b.parent) {
                        }
                        b.tree.sortedBodies.Append(this.bodies.oGet(i));
                        this.bodies.oGet(i).tree = b.tree;
                    }
                }

                if (this.trees.Num() > 1) {
                    gameLocal.Warning("Articulated figure has multiple seperate tree structures for entity '%s' type '%s'.",
                            this.self.name, this.self.GetType().getName());
                }

                // sort bodies in each tree to make sure parents come first
                for (i = 0; i < this.trees.Num(); i++) {
                    this.trees.oGet(i).SortBodies();
                }

            } else {

                // create a tree for each body
                for (i = 0; i < this.bodies.Num(); i++) {
                    tree = new idAFTree();
                    tree.sortedBodies.Clear();
                    tree.sortedBodies.Append(this.bodies.oGet(i));
                    this.bodies.oGet(i).tree = tree;
                    this.trees.Append(tree);
                }

                for (i = 0; i < this.constraints.Num(); i++) {
                    c = this.constraints.oGet(i);

                    c.body1.constraints.Append(c);
                    if (c.body2 != null) {
                        c.body2.constraints.Append(c);
                    }

                    c.fl.isPrimary = false;
                    this.auxiliaryConstraints.Append(c);
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

            for (i = 0; i < this.trees.Num(); i++) {
                this.trees.oGet(i).Factor();
            }
        }

        private void EvaluateBodies(float timeStep) {
            int i;
            idAFBody body;
            idMat3 axis;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

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
                    final idMat3 massMoment = SkewSymmetric(body.centerOfMass).oMultiply(body.mass);

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
            for (i = 0; i < this.primaryConstraints.Num(); i++) {
                c = this.primaryConstraints.oGet(i);
                c.Evaluate(invTimeStep);
                c.J = new idMatX(c.J2);
            }
            for (i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                this.auxiliaryConstraints.oGet(i).Evaluate(invTimeStep);
            }

            // add contact constraints to the list with frame constraints
            for (i = 0; i < this.contactConstraints.Num(); i++) {
                AddFrameConstraint(this.contactConstraints.oGet(i));
            }

            // setup body primary constraint matrix
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                if (body.primaryConstraint != null) {
                    body.J.oSet(body.primaryConstraint.J1.Transpose());
                    final int a = 0;
                }
            }
        }

        private void AddFrameConstraints() {
            int i;

            // add frame constraints to auxiliary constraints
            for (i = 0; i < this.frameConstraints.Num(); i++) {
                this.auxiliaryConstraints.Append(this.frameConstraints.oGet(i));
            }
        }

        private void RemoveFrameConstraints() {
            // remove all the frame constraints from the auxiliary constraints
            this.auxiliaryConstraints.SetNum(this.auxiliaryConstraints.Num() - this.frameConstraints.Num(), false);
            this.frameConstraints.SetNum(0, false);
        }

        private void ApplyFriction(float timeStep, float endTimeMSec) {
            int i;
            float invTimeStep;

            if (af_skipFriction.GetBool()) {
                return;
            }

            if ((this.jointFrictionDentStart < MS2SEC(endTimeMSec)) && (this.jointFrictionDentEnd > MS2SEC(endTimeMSec))) {
                final float halfTime = (this.jointFrictionDentEnd - this.jointFrictionDentStart) * 0.5f;
                if ((this.jointFrictionDentStart + halfTime) > MS2SEC(endTimeMSec)) {
                    this.jointFrictionDentScale = 1.0f - (((1.0f - this.jointFrictionDent) * (MS2SEC(endTimeMSec) - this.jointFrictionDentStart)) / halfTime);
                } else {
                    this.jointFrictionDentScale = this.jointFrictionDent + (((1.0f - this.jointFrictionDent) * (MS2SEC(endTimeMSec) - this.jointFrictionDentStart - halfTime)) / halfTime);
                }
            } else {
                this.jointFrictionDentScale = 0.0f;
            }

            if ((this.contactFrictionDentStart < MS2SEC(endTimeMSec)) && (this.contactFrictionDentEnd > MS2SEC(endTimeMSec))) {
                final float halfTime = (this.contactFrictionDentEnd - this.contactFrictionDentStart) * 0.5f;
                if ((this.contactFrictionDentStart + halfTime) > MS2SEC(endTimeMSec)) {
                    this.contactFrictionDentScale = 1.0f - (((1.0f - this.contactFrictionDent) * (MS2SEC(endTimeMSec) - this.contactFrictionDentStart)) / halfTime);
                } else {
                    this.contactFrictionDentScale = this.contactFrictionDent + (((1.0f - this.contactFrictionDent) * (MS2SEC(endTimeMSec) - this.contactFrictionDentStart - halfTime)) / halfTime);
                }
            } else {
                this.contactFrictionDentScale = 0.0f;
            }

            invTimeStep = 1.0f / timeStep;

            for (i = 0; i < this.primaryConstraints.Num(); i++) {
                this.primaryConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
            for (i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                this.auxiliaryConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
            for (i = 0; i < this.frameConstraints.Num(); i++) {
                this.frameConstraints.oGet(i).ApplyFriction(invTimeStep);
            }
        }

        private void PrimaryForces(float timeStep) {
            int i;

            for (i = 0; i < this.trees.Num(); i++) {
                this.trees.oGet(i).CalculateForces(timeStep);
            }
        }

        private void AuxiliaryForces(float timeStep) {
            int i, j, k, l, n, m, s, numAuxConstraints;
            int[] index, boxIndex;
            float[] ptr, j1, j2, dstPtr;
			final float[] forcePtr;
            float invStep, u;
            idAFBody body;
            idAFConstraint constraint;
            final idVecX tmp = new idVecX();
            final idMatX jmk = new idMatX();
            final idVecX rhs = new idVecX(), w = new idVecX(), lm = new idVecX(), lo = new idVecX(), hi = new idVecX();
            int p_i, d_i;

            // get the number of one dimensional auxiliary constraints
            for (numAuxConstraints = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                numAuxConstraints += this.auxiliaryConstraints.oGet(i).J1.GetNumRows();
            }

            if (numAuxConstraints == 0) {
                return;
            }

            // allocate memory to store the body response to auxiliary constraint forces
//            forcePtr = new float[bodies.Num() * numAuxConstraints * 8];
//            index = new int[bodies.Num() * numAuxConstraints];
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);
                body.response = new float[this.bodies.Num() * numAuxConstraints * 8];
                body.responseIndex = new int[this.bodies.Num() * numAuxConstraints];
                body.numResponses = 0;
                body.maxAuxiliaryIndex = 0;
//                forcePtr += numAuxConstraints * 8;
//                index += numAuxConstraints;
            }

            // set on each body the largest index of an auxiliary constraint constraining the body
            if (af_useSymmetry.GetBool()) {
                for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                    constraint = this.auxiliaryConstraints.oGet(i);
                    for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {
                        if (k > constraint.body1.maxAuxiliaryIndex) {
                            constraint.body1.maxAuxiliaryIndex = k;
                        }
                        if ((constraint.body2 != null) && (k > constraint.body2.maxAuxiliaryIndex)) {
                            constraint.body2.maxAuxiliaryIndex = k;
                        }
                    }
                }
                for (i = 0; i < this.trees.Num(); i++) {
                    this.trees.oGet(i).SetMaxSubTreeAuxiliaryIndex();
                }
            }

            // calculate forces of primary constraints in response to the auxiliary constraint forces
            for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                constraint = this.auxiliaryConstraints.oGet(i);

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {

                    // calculate body forces in the tree in response to the constraint force
                    constraint.body1.tree.Response(constraint, j, k);
                    // if there is a second body which is part of a different tree
                    if ((constraint.body2 != null) && (constraint.body2.tree != constraint.body1.tree)) {
                        // calculate body forces in the second tree in response to the constraint force
                        constraint.body2.tree.Response(constraint, j, k);
                    }
                }
            }

            // NOTE: the rows are 16 byte padded
            jmk.SetData(numAuxConstraints, ((numAuxConstraints + 3) & ~3), MATX_ALLOCA(numAuxConstraints * ((numAuxConstraints + 3) & ~3)));
            tmp.SetData(6, VECX_ALLOCA(6));

            // create constraint matrix for auxiliary constraints using a mass matrix adjusted for the primary constraints
            for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                constraint = this.auxiliaryConstraints.oGet(i);

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {
                    constraint.body1.InverseWorldSpatialInertiaMultiply(tmp, constraint.J1.oGet(j));
                    j1 = tmp.ToFloatPtr();
                    ptr = constraint.body1.response;
                    index = constraint.body1.responseIndex;
                    dstPtr = jmk.ToFloatPtr();
                    s = af_useSymmetry.GetBool() ? k + 1 : numAuxConstraints;
                    final int c = k * jmk.GetNumColumns();
                    for (l = n = p_i = 0, m = index[n]; (n < constraint.body1.numResponses) && (m < s); n++, m = index[n]) {
                        while (l < m) {
                            dstPtr[c + l++] = 0.0f;
                        }
                        dstPtr[c + l++] = (j1[0] * ptr[p_i + 0]) + (j1[1] * ptr[p_i + 1]) +
                                (j1[2] * ptr[p_i + 2]) + (j1[3] * ptr[p_i + 3]) +
                                (j1[4] * ptr[p_i + 4]) + (j1[5] * ptr[p_i + 5]);
                        p_i += 8;
                    }

                    while (l < s) {
                        dstPtr[c + l++] = 0.0f;
                    }

                    if (constraint.body2 != null) {
                        constraint.body2.InverseWorldSpatialInertiaMultiply(tmp, constraint.J2.oGet(j));
                        j2 = tmp.ToFloatPtr();
                        ptr = constraint.body2.response;
                        index = constraint.body2.responseIndex;
                        for (n = p_i = 0, m = index[n]; (n < constraint.body2.numResponses) && (m < s); n++, m = index[n]) {
                            dstPtr[c + m] += (j2[0] * ptr[p_i + 0]) + (j2[1] * ptr[p_i + 1]) +
                                    (j2[2] * ptr[p_i + 2]) + (j2[3] * ptr[p_i + 3]) +
                                    (j2[4] * ptr[p_i + 4]) + (j2[5] * ptr[p_i + 5]);
                            p_i += 8;
                        }
                    }
                }
            }

            if (af_useSymmetry.GetBool()) {
                n = jmk.GetNumColumns();
                for (i = 0; i < numAuxConstraints; i++) {
                    ptr = jmk.ToFloatPtr();
                    p_i = ((i + 1) * n) + i;
                    dstPtr = jmk.ToFloatPtr();
                    d_i = (i * n) + i + 1;
                    for (j = i + 1; j < numAuxConstraints; j++) {
                        dstPtr[d_i++] = ptr[p_i];//TODO:
                        p_i += n;
                    }
                }
            }

            invStep = 1.0f / timeStep;

            // calculate body acceleration
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);
                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.acceleration.SubVec6_oPluSet(0, body.current.spatialVelocity.oMultiply(invStep));
                final int a = 0;
            }

            rhs.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            lo.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            hi.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            lm.SetData(numAuxConstraints, VECX_ALLOCA(numAuxConstraints));
            boxIndex = new int[numAuxConstraints];

            // set first index for special box constrained variables
            for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                this.auxiliaryConstraints.oGet(i).firstIndex = k;
                k += this.auxiliaryConstraints.oGet(i).J1.GetNumRows();
            }

            // initialize right hand side and low and high bounds for auxiliary constraints
            for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                constraint = this.auxiliaryConstraints.oGet(i);
                n = k;

                for (j = 0; j < constraint.J1.GetNumRows(); j++, k++) {

                    j1 = constraint.J1.oGet(j);
                    ptr = constraint.body1.acceleration.ToFloatPtr();
                    rhs.p[k] = (j1[0] * ptr[0]) + (j1[1] * ptr[1]) + (j1[2] * ptr[2]) + (j1[3] * ptr[3]) + (j1[4] * ptr[4]) + (j1[5] * ptr[5]);
                    rhs.p[k] += constraint.c1.p[j] * invStep;

                    if (constraint.body2 != null) {
                        j2 = constraint.J2.oGet(j);
                        ptr = constraint.body2.acceleration.ToFloatPtr();
                        rhs.p[k] += (j2[0] * ptr[0]) + (j2[1] * ptr[1]) + (j2[2] * ptr[2]) + (j2[3] * ptr[3]) + (j2[4] * ptr[4]) + (j2[5] * ptr[5]);
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
                    final float v = jmk.oGet(k)[k];
                    jmk.oPluSet(k, k, constraint.e.p[j] * invStep);
                    final int a = 0;
                }
            }

            if (AF_TIMINGS) {
                timer_lcp.Start();
            }

            // calculate lagrange multipliers for auxiliary constraints
            if (!this.lcp.Solve(jmk, lm, rhs, lo, hi, boxIndex)) {
                return;        // bad monkey!
            }

            if (AF_TIMINGS) {
                timer_lcp.Stop();
            }

            // calculate auxiliary constraint forces
            for (k = 0, i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                constraint = this.auxiliaryConstraints.oGet(i);

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
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);
                body.response = null;
                body.responseIndex = null;
            }
        }

        private static int DBG_VerifyContactConstraints = 0;
        private void VerifyContactConstraints() {      DBG_VerifyContactConstraints++;
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

            for (i = 0; i < this.contactConstraints.Num(); i++) {
                body = this.contactConstraints.oGet(i).body1;
                normal = this.contactConstraints.oGet(i).GetContact().normal;
                final float v = normal.oMultiply(body.next.spatialVelocity.SubVec3(0));
                if (v <= 0.0f) {
                    body.next.spatialVelocity.SubVec3_oMinSet(0, normal.oMultiply(1.0001f * v));
                }
                body = this.contactConstraints.oGet(i).body2;
                if (null == body) {
                    continue;
                }
                normal = normal.oNegative();
                if (v <= 0.0f) {
                    body.next.spatialVelocity.SubVec3_oMinSet(0, normal.oMultiply(1.0001f * v));
                }
                final int a = 0;
            }
// }
        }

        private void SetupContactConstraints() {
            int i;

            // make sure enough contact constraints are allocated
            this.contactConstraints.AssureSizeAlloc(this.contacts.Num(), idAFConstraint_Contact.class);
            this.contactConstraints.SetNum(this.contacts.Num(), false);

            // setup contact constraints
            for (i = 0; i < this.contacts.Num(); i++) {
                // add contact constraint
                this.contactConstraints.oGet(i).physics = this;
                if (this.contacts.oGet(i).entityNum == this.self.entityNumber) {
                    this.contactConstraints.oGet(i).Setup(this.bodies.oGet(this.contactBodies.oGet(i)), this.bodies.oGet(this.contacts.oGet(i).id), this.contacts.oGet(i));
                } else {
                    this.contactConstraints.oGet(i).Setup(this.bodies.oGet(this.contactBodies.oGet(i)), null, this.contacts.oGet(i));
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
            final idVec6 force;
            idRotation rotation;
            float vSqr, maxLinearVelocity, maxAngularVelocity;

            maxLinearVelocity = af_maxLinearVelocity.GetFloat() / timeStep;
            maxAngularVelocity = af_maxAngularVelocity.GetFloat() / timeStep;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                // calculate the spatial velocity for the next physics state
                body.InverseWorldSpatialInertiaMultiply(body.acceleration, body.totalForce.ToFloatPtr());
                body.next.spatialVelocity.oSet(body.current.spatialVelocity.oPlus(body.acceleration.SubVec6(0).oMultiply(timeStep)));

                if (maxLinearVelocity > 0.0f) {
                    // cap the linear velocity
                    vSqr = body.next.spatialVelocity.SubVec3(0).LengthSqr();
                    if (vSqr > Square(maxLinearVelocity)) {
                        body.next.spatialVelocity.SubVec3_oMulSet(0, idMath.InvSqrt(vSqr) * maxLinearVelocity);
                        final int a = 0;
                    }
                }

                if (maxAngularVelocity > 0.0f) {
                    // cap the angular velocity
                    vSqr = body.next.spatialVelocity.SubVec3(1).LengthSqr();
                    if (vSqr > Square(maxAngularVelocity)) {
                        body.next.spatialVelocity.SubVec3_oMulSet(1, idMath.InvSqrt(vSqr) * maxAngularVelocity);
                        final int a = 0;
                    }
                }
            }

            // make absolutely sure all contact constraints are satisfied
            VerifyContactConstraints();

            // calculate the position of the bodies for the next physics state
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                // translate world origin
                body.next.worldOrigin.oSet(body.current.worldOrigin.oPlus(body.next.spatialVelocity.SubVec3(0).oMultiply(timeStep)));

                // convert angular velocity to a rotation matrix
                vec = body.next.spatialVelocity.SubVec3(1);
                angle = -timeStep * RAD2DEG(vec.Normalize());
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

            if (!this.selfCollision || !body.fl.selfCollision || af_skipSelfCollision.GetBool()) {

                // disable all bodies
                for (i = 0; i < this.bodies.Num(); i++) {
                    this.bodies.oGet(i).clipModel.Disable();
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
                for (i = 0; i < this.bodies.Num(); i++) {
                    if (this.bodies.oGet(i).fl.selfCollision) {
                        this.bodies.oGet(i).clipModel.Enable();
                    } else {
                        this.bodies.oGet(i).clipModel.Disable();
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
            impactInfo_s info;
            idEntity ent;

            ent = gameLocal.entities[collision.c.entityNum];
            if (ent == this.self) {
                return false;
            }

            // get info from other entity involved
            info = ent.GetImpactInfo(this.self, collision.c.id, collision.c.point);
            // collision point relative to the body center of mass
            r = collision.c.point.oMinus(body.current.worldOrigin.oPlus(body.centerOfMass.oMultiply(body.current.worldAxis)));
            // the velocity at the collision point
            velocity = body.current.spatialVelocity.SubVec3(0).oPlus(body.current.spatialVelocity.SubVec3(1).Cross(r));
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
            ent.ApplyImpulse(this.self, collision.c.id, collision.c.point, impulse.oNegative());

            // callback to self to let the entity know about the impact
            return this.self.Collide(collision, velocity);
        }

        private boolean ApplyCollisions(float timeStep) {
            int i;

            for (i = 0; i < this.collisions.Num(); i++) {
                if (CollisionImpulse(timeStep, this.collisions.oGet(i).body, this.collisions.oGet(i).trace)) {
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
         */                   private static int DBG_CheckForCollisions = 0;
        private void CheckForCollisions(float timeStep) {              DBG_CheckForCollisions++;
//	#define TEST_COLLISION_DETECTION
            int i, index;
            idAFBody body;
            final idMat3 axis = new idMat3();
            idRotation rotation;
            final trace_s[] collision = {new trace_s()};
            idEntity passEntity;
            boolean startSolid = false;

            // clear list with collisions
            this.collisions.SetNum(0, false);

            if (!this.enableCollision) {
                return;
            }

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

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
                        index = this.collisions.Num();
                        this.collisions.SetNum(index + 1, false);
                        this.collisions.oSet(index, new AFCollision_s());
                        this.collisions.oGet(index).trace = new trace_s(collision[0]);
                        this.collisions.oGet(index).body = body;
                    }

                    if (TEST_COLLISION_DETECTION) {
                        if (gameLocal.clip.Contents(body.next.worldOrigin, body.clipModel,
                                body.next.worldAxis, body.clipMask, passEntity) != 0) {
                            if (!startSolid) {
                                final int bah = 1;
                            }
                        }
                    }
                }

                body.clipModel.Link(gameLocal.clip, this.self, body.clipModel.GetId(), body.next.worldOrigin, body.next.worldAxis);
            }
        }

        private void ClearExternalForce() {
            int i;
            idAFBody body;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                // clear external force
                body.current.externalForce.Zero();
                body.next.externalForce.Zero();
            }
        }

        private void AddGravity() {
            int i;
            idAFBody body;

            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);
                // add gravitational force
                body.current.externalForce.SubVec3_oPluSet(0, this.gravityVector.oMultiply(body.mass));
            }
        }

        private void SwapStates() {
            int i;
            idAFBody body;
            AFBodyPState_s swap;

            for (i = 0; i < this.bodies.Num(); i++) {

                body = this.bodies.oGet(i);

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

            if (this.current.atRest >= 0) {
                return true;
            }

            this.current.activateTime += timeStep;

            // if the simulation should never be suspended before a certaint amount of time passed
            if ((this.minMoveTime > 0.0f) && (this.current.activateTime < this.minMoveTime)) {
                return false;
            }

            // if the simulation should always be suspended after a certain amount time passed
            if ((this.maxMoveTime > 0.0f) && (this.current.activateTime > this.maxMoveTime)) {
                return true;
            }

            // test if all bodies hardly moved over a period of time
            if (this.current.noMoveTime == 0.0f) {
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);
                    body.atRestOrigin.oSet(body.current.worldOrigin);
                    body.atRestAxis.oSet(body.current.worldAxis);
                }
                this.current.noMoveTime += timeStep;
            } else if (this.current.noMoveTime > this.noMoveTime) {
                this.current.noMoveTime = 0.0f;
                maxTranslationSqr = 0.0f;
                maxRotation = 0.0f;
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);

                    translationSqr = (body.current.worldOrigin.oMinus(body.atRestOrigin)).LengthSqr();
                    if (translationSqr > maxTranslationSqr) {
                        maxTranslationSqr = translationSqr;
                    }
                    rotation = (body.atRestAxis.Transpose().oMultiply(body.current.worldAxis)).ToRotation().GetAngle();
                    if (rotation > maxRotation) {
                        maxRotation = rotation;
                    }
                }

                if ((maxTranslationSqr < Square(this.noMoveTranslation)) && (maxRotation < this.noMoveRotation)) {
                    // hardly moved over a period of time so the articulated figure may come to rest
                    return true;
                }
            } else {
                this.current.noMoveTime += timeStep;
            }

            // test if the velocity or acceleration of any body is still too large to come to rest
            for (i = 0; i < this.bodies.Num(); i++) {
                body = this.bodies.oGet(i);

                if (body.current.spatialVelocity.SubVec3(0).LengthSqr() > Square(this.suspendVelocity.oGet(0))) {
                    return false;
                }
                if (body.current.spatialVelocity.SubVec3(1).LengthSqr() > Square(this.suspendVelocity.oGet(1))) {
                    return false;
                }
                if (body.acceleration.SubVec3(0).LengthSqr() > Square(this.suspendAcceleration.oGet(0))) {
                    return false;
                }
                if (body.acceleration.SubVec3(1).LengthSqr() > Square(this.suspendAcceleration.oGet(1))) {
                    return false;
                }
            }

            // all bodies have a velocity and acceleration small enough to come to rest
            return true;
        }

        private void Rest() {
            int i;

            this.current.atRest = gameLocal.time;

            for (i = 0; i < this.bodies.Num(); i++) {
                this.bodies.oGet(i).current.spatialVelocity.Zero();
                this.bodies.oGet(i).current.externalForce.Zero();
            }

            this.self.BecomeInactive(TH_PHYSICS);
        }

        private void AddPushVelocity(final idVec6 pushVelocity) {
            int i;

            if (!pushVelocity.equals(getVec6_origin())) {
                for (i = 0; i < this.bodies.Num(); i++) {
                    this.bodies.oGet(i).current.spatialVelocity.oPluSet(pushVelocity);
                    final int a = 0;
                }
            }
        }

        private void DebugDraw() {
            int i;
            idAFBody body, highlightBody = null, constrainedBody1 = null, constrainedBody2 = null;
            idAFConstraint constraint;
            final idVec3 center = new idVec3();
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
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);
                    if ((body == constrainedBody1) || (body == constrainedBody2)) {
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
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);
                    gameRenderWorld.DrawText(body.GetName().getData(), body.GetWorldOrigin(), 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showMass.GetBool()) {
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);
                    gameRenderWorld.DrawText(va("\n%1.2f", 1.0f / body.GetInverseMass()), body.GetWorldOrigin(), 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showTotalMass.GetBool()) {
                axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();
                gameRenderWorld.DrawText(va("\n%1.2f", this.totalMass), this.bodies.oGet(0).GetWorldOrigin().oPlus(axis.oGet(2).oMultiply(8.0f)), 0.15f, colorCyan, axis, 1);
            }

            if (af_showInertia.GetBool()) {
                for (i = 0; i < this.bodies.Num(); i++) {
                    body = this.bodies.oGet(i);
                    final idMat3 I = body.inertiaTensor;
                    gameRenderWorld.DrawText(va("\n\n\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )",
                                    I.oGet(0).x, I.oGet(0).y, I.oGet(0).z,
                                    I.oGet(1).x, I.oGet(1).y, I.oGet(1).z,
                                    I.oGet(2).x, I.oGet(2).y, I.oGet(2).z),
                            body.GetWorldOrigin(), 0.05f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
            }

            if (af_showVelocity.GetBool()) {
                for (i = 0; i < this.bodies.Num(); i++) {
                    DrawVelocity(this.bodies.oGet(i).clipModel.GetId(), 0.1f, 4.0f);
                }
            }

            if (af_showConstraints.GetBool()) {
                for (i = 0; i < this.primaryConstraints.Num(); i++) {
                    constraint = this.primaryConstraints.oGet(i);
                    constraint.DebugDraw();
                }
                if (!af_showPrimaryOnly.GetBool()) {
                    for (i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                        constraint = this.auxiliaryConstraints.oGet(i);
                        constraint.DebugDraw();
                    }
                }
            }

            if (af_showConstraintNames.GetBool()) {
                for (i = 0; i < this.primaryConstraints.Num(); i++) {
                    constraint = this.primaryConstraints.oGet(i);
                    constraint.GetCenter(center);
                    gameRenderWorld.DrawText(constraint.GetName().getData(), center, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                }
                if (!af_showPrimaryOnly.GetBool()) {
                    for (i = 0; i < this.auxiliaryConstraints.Num(); i++) {
                        constraint = this.auxiliaryConstraints.oGet(i);
                        constraint.GetCenter(center);
                        gameRenderWorld.DrawText(constraint.GetName().getData(), center, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
                    }
                }
            }

            if (af_showTrees.GetBool() || (af_showActive.GetBool() && (this.current.atRest < 0))) {
                for (i = 0; i < this.trees.Num(); i++) {
                    this.trees.oGet(i).DebugDraw(idStr.ColorForIndex(i + 3));
                }
            }
        }

        @Override
        public void oSet(idClass oGet) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

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
        final int[] atRest = {0};
        final float[] noMoveTime = {0};
        final float[] activateTime = {0};
        final float[] lastTimeStep = {0};

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
