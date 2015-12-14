package neo.idlib.math;

import java.nio.ByteBuffer;
import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Angles {

    private static final idAngles ang_zero = new idAngles(0.0f, 0.0f, 0.0f);//TODO:make sure new instances are created everytime.
    
    public static idAngles getAng_zero() {
        return new idAngles(ang_zero);
    }
    
    /*
     ===============================================================================

     Euler angles

     ===============================================================================
     */
//    
// angle indexes
    public static final int PITCH = 0;		// up / down
    public static final int YAW = 1;		// left / right
    public static final int ROLL = 2;		// fall over


    public static class idAngles implements SERiAL {

        public float pitch;
        public float yaw;
        public float roll;

        public idAngles() {
        }

        public idAngles(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }

        public idAngles(final idVec3 v) {
            this.pitch = v.x;
            this.yaw = v.y;
            this.roll = v.z;
        }

        public idAngles(idAngles a) {
            this.pitch = a.pitch;
            this.yaw = a.yaw;
            this.roll = a.roll;
        }

        public void Set(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }

        /**
         *
         * @return @deprecated for post constructor use. seeing as how the
         * constructor sets everything to zero anyways.
         */
        @Deprecated
        public idAngles Zero() {
            pitch = yaw = roll = 0.0f;
            return this;
        }
//
//public	float			operator[]( int index ) final ;

        public float oGet(int index) {
            assert ((index >= 0) && (index < 3));
            switch (index) {
                default:
                    return pitch;
                case 1:
                    return yaw;
                case 2:
                    return roll;
            }
        }

//public	float &			operator[]( int index );
        public void oSet(int index, final float value) {
            switch (index) {
                default:
                    pitch = value;
                    break;
                case 1:
                    yaw = value;
                    break;
                case 2:
                    roll = value;
                    break;
            }
        }

        public float oPluSet(int index, final float value) {
            switch (index) {
                default:
                    return pitch += value;
                case 1:
                    return yaw += value;
                case 2:
                    return roll += value;
            }
        }

        public float oMinSet(int index, final float value) {
            switch (index) {
                default:
                    return pitch -= value;
                case 1:
                    return yaw -= value;
                case 2:
                    return roll -= value;
            }
        }

        public idAngles oNegative() {// negate angles, in general not the inverse rotation
            return new idAngles(-pitch, -yaw, -roll);
        }

//public	idAngles &		operator=( final  idAngles &a );
        public idAngles oSet(idAngles a) {
            pitch = a.pitch;
            yaw = a.yaw;
            roll = a.roll;
            return this;
        }

        public idAngles oPlus(final idAngles a) {
            return new idAngles(pitch + a.pitch, yaw + a.yaw, roll + a.roll);
        }

        public idAngles oPluSet(final idAngles a) {
            pitch += a.pitch;
            yaw += a.yaw;
            roll += a.roll;

            return this;
        }

        public idAngles oMinus(final idAngles a) {
            return new idAngles(pitch - a.pitch, yaw - a.yaw, roll - a.roll);
        }

        public idAngles oMinSet(final idAngles a) {
            pitch -= a.pitch;
            yaw -= a.yaw;
            roll -= a.roll;

            return this;
        }

        public idAngles oMultiply(final float a) {
            return new idAngles(pitch * a, yaw * a, roll * a);
        }

        public idAngles oMulSet(final float a) {
            pitch *= a;
            yaw *= a;
            roll *= a;

            return this;
        }

        public idAngles oDivide(final float a) {
            float inva = 1.0f / a;
            return new idAngles(pitch * inva, yaw * inva, roll * inva);
        }

        public idAngles oDivSet(final float a) {
            float inva = 1.0f / a;
            pitch *= inva;
            yaw *= inva;
            roll *= inva;

            return this;
        }
//

        public static idAngles oMultiply(final float a, final idAngles b) {
            return new idAngles(a * b.pitch, a * b.yaw, a * b.roll);
        }
//

        public boolean Compare(final idAngles a) {// exact compare, no epsilon
            return ((a.pitch == pitch) && (a.yaw == yaw) && (a.roll == roll));
        }

        public boolean Compare(final idAngles a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(pitch - a.pitch) > epsilon) {
                return false;
            }

            if (idMath.Fabs(yaw - a.yaw) > epsilon) {
                return false;
            }

            if (idMath.Fabs(roll - a.roll) > epsilon) {
                return false;
            }

            return true;
        }

//public	boolean 			operator==(	final  idAngles &a ) final ;						// exact compare, no epsilon
//public	boolean 			operator!=(	final  idAngles &a ) final ;						// exact compare, no epsilon
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Float.floatToIntBits(this.pitch);
            hash = 73 * hash + Float.floatToIntBits(this.yaw);
            hash = 73 * hash + Float.floatToIntBits(this.roll);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idAngles other = (idAngles) obj;
            if (Float.floatToIntBits(this.pitch) != Float.floatToIntBits(other.pitch)) {
                return false;
            }
            if (Float.floatToIntBits(this.yaw) != Float.floatToIntBits(other.yaw)) {
                return false;
            }
            if (Float.floatToIntBits(this.roll) != Float.floatToIntBits(other.roll)) {
                return false;
            }
            return true;
        }
//

        /**
         * ================= idAngles::Normalize360
         *
         * returns angles normalized to the range [0 <= angle < 360]
         * =================
         */
        public idAngles Normalize360() {// normalizes 'this'
            int i;

            for (i = 0; i < 3; i++) {
                if ((this.oGet(i) >= 360.0f) || (this.oGet(i) < 0.0f)) {
                    this.oPluSet(i, -(float) Math.floor(this.oGet(i) / 360.0f) * 360.0f);

                    if (this.oGet(i) >= 360.0f) {
                        this.oPluSet(i, -360.0f);
                    }
                    if (this.oGet(i) < 0.0f) {
                        this.oPluSet(i, 360.0f);
                    }
                }
            }

            return this;
        }

        /**
         * ================= idAngles::Normalize180
         *
         * returns angles normalized to the range [-180 < angle <= 180]
         * =================
         */
        public idAngles Normalize180() {// normalizes 'this'
            Normalize360();

            if (pitch > 180.0f) {
                pitch -= 360.0f;
            }

            if (yaw > 180.0f) {
                yaw -= 360.0f;
            }

            if (roll > 180.0f) {
                roll -= 360.0f;
            }
            return this;
        }
//

        public void Clamp(final idAngles min, final idAngles max) {
            if (pitch < min.pitch) {
                pitch = min.pitch;
            } else if (pitch > max.pitch) {
                pitch = max.pitch;
            }
            if (yaw < min.yaw) {
                yaw = min.yaw;
            } else if (yaw > max.yaw) {
                yaw = max.yaw;
            }
            if (roll < min.roll) {
                roll = min.roll;
            } else if (roll > max.roll) {
                roll = max.roll;
            }
        }
//

        public int GetDimension() {
            return 3;
        }
//

        public void ToVectors(idVec3 forward) {
            ToVectors(forward, null);
        }

        public void ToVectors(idVec3 forward, idVec3 right) {
            ToVectors(forward, right, null);
        }

        public void ToVectors(idVec3 forward, idVec3 right, idVec3 up) {
            float[] sr = new float[1], sp = new float[1], sy = new float[1], cr = new float[1], cp = new float[1], cy = new float[1];

            idMath.SinCos((float) Math_h.DEG2RAD(yaw), sy, cy);
            idMath.SinCos((float) Math_h.DEG2RAD(pitch), sp, cp);
            idMath.SinCos((float) Math_h.DEG2RAD(roll), sr, cr);

            if (forward != null) {
                forward.Set(cp[0] * cy[0], cp[0] * sy[0], -sp[0]);
            }

            if (right != null) {
                right.Set(-sr[0] * sp[0] * cy[0] + cr[0] * sy[0], -sr[0] * sp[0] * sy[0] + -cr[0] * cy[0], -sr[0] * cp[0]);
            }

            if (up != null) {
                up.Set(cr[0] * sp[0] * cy[0] + -sr[0] * -sy[0], cr[0] * sp[0] * sy[0] + -sr[0] * cy[0], cr[0] * cp[0]);
            }
        }

        public idVec3 ToForward() {
            float[] sp = new float[1], sy = new float[1], cp = new float[1], cy = new float[1];

            idMath.SinCos((float) Math_h.DEG2RAD(yaw), sy, cy);
            idMath.SinCos((float) Math_h.DEG2RAD(pitch), sp, cp);

            return new idVec3(cp[0] * cy[0], cp[0] * sy[0], -sp[0]);
        }

        public idQuat ToQuat() {
            float[] sx = new float[1], cx = new float[1], sy = new float[1], cy = new float[1], sz = new float[1], cz = new float[1];
            float sxcy, cxcy, sxsy, cxsy;

            idMath.SinCos((float) Math_h.DEG2RAD(yaw) * 0.5f, sz, cz);
            idMath.SinCos((float) Math_h.DEG2RAD(pitch) * 0.5f, sy, cy);
            idMath.SinCos((float) Math_h.DEG2RAD(roll) * 0.5f, sx, cx);

            sxcy = sx[0] * cy[0];
            cxcy = cx[0] * cy[0];
            sxsy = sx[0] * sy[0];
            cxsy = cx[0] * sy[0];

            return new idQuat(cxsy * sz[0] - sxcy * cz[0], -cxsy * cz[0] - sxcy * sz[0], sxsy * cz[0] - cxcy * sz[0], cxcy * cz[0] + sxsy * sz[0]);
        }

        public idRotation ToRotation() {
            idVec3 vec = new idVec3();
            float angle, w;
            float[] sx = new float[1], cx = new float[1], sy = new float[1], cy = new float[1], sz = new float[1], cz = new float[1];
            float sxcy, cxcy, sxsy, cxsy;

            if (pitch == 0.0f) {
                if (yaw == 0.0f) {
                    return new idRotation(Vector.getVec3_origin(), new idVec3(-1.0f, 0.0f, 0.0f), roll);
                }
                if (roll == 0.0f) {
                    return new idRotation(Vector.getVec3_origin(), new idVec3(0.0f, 0.0f, -1.0f), yaw);
                }
            } else if (yaw == 0.0f && roll == 0.0f) {
                return new idRotation(Vector.getVec3_origin(), new idVec3(0.0f, -1.0f, 0.0f), pitch);
            }

            idMath.SinCos((float) Math_h.DEG2RAD(yaw) * 0.5f, sz, cz);
            idMath.SinCos((float) Math_h.DEG2RAD(pitch) * 0.5f, sy, cy);
            idMath.SinCos((float) Math_h.DEG2RAD(roll) * 0.5f, sx, cx);

            sxcy = sx[0] * cy[0];
            cxcy = cx[0] * cy[0];
            sxsy = sx[0] * sy[0];
            cxsy = cx[0] * sy[0];

            vec.x = cxsy * sz[0] - sxcy * cz[0];
            vec.y = -cxsy * cz[0] - sxcy * sz[0];
            vec.z = sxsy * cz[0] - cxcy * sz[0];
            w = cxcy * cz[0] + sxsy * sz[0];
            angle = idMath.ACos(w);
            if (angle == 0.0f) {
                vec.Set(0.0f, 0.0f, 1.0f);
            } else {
                //vec *= (1.0f / sin( angle ));
                vec.Normalize();
                vec.FixDegenerateNormal();
                angle *= 2.0f * idMath.M_RAD2DEG;
            }
            return new idRotation(Vector.getVec3_origin(), vec, angle);
        }

        public idMat3 ToMat3() {
            idMat3 mat = new idMat3();
            float[] sr = new float[1], sp = new float[1], sy = new float[1], cr = new float[1], cp = new float[1], cy = new float[1];

            idMath.SinCos((float) Math_h.DEG2RAD(yaw), sy, cy);
            idMath.SinCos((float) Math_h.DEG2RAD(pitch), sp, cp);
            idMath.SinCos((float) Math_h.DEG2RAD(roll), sr, cr);

            mat.setRow(0, cp[0] * cy[0], cp[0] * sy[0], -sp[0]);
            mat.setRow(1, sr[0] * sp[0] * cy[0] + cr[0] * -sy[0], sr[0] * sp[0] * sy[0] + cr[0] * cy[0], sr[0] * cp[0]);
            mat.setRow(2, cr[0] * sp[0] * cy[0] + -sr[0] * -sy[0], cr[0] * sp[0] * sy[0] + -sr[0] * cy[0], cr[0] * cp[0]);

            return mat;
        }

        public idMat4 ToMat4() {
            return ToMat3().ToMat4();
        }

        public idVec3 ToAngularVelocity() {
            idRotation rotation = this.ToRotation();
            return rotation.GetVec().oMultiply((float) Math_h.DEG2RAD(rotation.GetAngle()));
        }

        public float[] ToFloatPtr() {
            throw new TODO_Exception();
        }
//public	float *			ToFloatPtr( void );

        public String ToString() {
            return ToString(2);
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
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
    };
}
