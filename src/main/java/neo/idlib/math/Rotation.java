package neo.idlib.math;

import static java.lang.Math.floor;
import static neo.idlib.math.Math_h.DEG2RAD;

import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Rotation {

    public static class idRotation {

        //private:
        public idVec3  origin;      // origin of rotation
        public idVec3  vec;         // normalized vector to rotate around
        public float   angle;       // angle of rotation in degrees
        public idMat3  axis;        // rotation axis
        public boolean axisValid;   // true if rotation axis is valid
        //
        //

        //        friend class idAngles;
//	friend class idQuat;
//	friend class idMat3;
//
//public:
        public idRotation() {
            this.origin = new idVec3();
            this.vec = new idVec3();
            this.axis = new idMat3();
        }

        public idRotation(final idVec3 rotationOrigin, final idVec3 rotationVec, final float rotationAngle) {
            this();
            this.origin.oSet(rotationOrigin);
            this.vec.oSet(rotationVec);
            this.angle = rotationAngle;
            this.axisValid = false;
        }

        public idRotation(final idRotation rotation) {
            this();
            this.oSet(rotation);
        }

        public void Set(final idVec3 rotationOrigin, final idVec3 rotationVec, final float rotationAngle) {
            this.origin.oSet(rotationOrigin);
            this.vec.oSet(rotationVec);
            this.angle = rotationAngle;
            this.axisValid = false;
        }

        public void SetOrigin(final idVec3 rotationOrigin) {
            this.origin.oSet(rotationOrigin);
        }

        // has to be normalized	
        public void SetVec(final idVec3 rotationVec) {
            this.vec.oSet(rotationVec);
            this.axisValid = false;
        }

        // has to be normalized
        public void SetVec(final float x, final float y, final float z) {
            this.vec.oSet(0, x);
            this.vec.oSet(1, y);
            this.vec.oSet(2, x);
            this.axisValid = false;
        }

        public void SetAngle(final float rotationAngle) {
            this.angle = rotationAngle;
            this.axisValid = false;
        }

        public void Scale(final float s) {
            this.angle *= s;
            this.axisValid = false;
        }

        public void ReCalculateMatrix() {
            this.axisValid = false;
            ToMat3();
        }

        public idVec3 GetOrigin() {
            return this.origin;
        }

        public idVec3 GetVec() {
            return this.vec;
        }

        public float GetAngle() {
            return this.angle;
        }
//
//	idRotation			operator-() const;										// flips rotation

        public idRotation oMultiply(final float s) {// scale rotation
            return new idRotation(this.origin, this.vec, this.angle * s);
        }
//	idRotation			operator/( const float s ) const;						// scale rotation
//	idRotation &		operator*=( const float s );							// scale rotation
//	idRotation &		operator/=( const float s );							// scale rotation

        public idVec3 oMultiply(final idVec3 v) {// rotate vector
            if (!this.axisValid) {
                ToMat3();
            }
            return (this.axis.oMultiply(v.oMinus(this.origin))).oPlus(this.origin);
        }
//
//	friend idRotation	operator*( const float s, const idRotation &r );		// scale rotation
//	friend idVec3		operator*( const idVec3 &v, const idRotation &r );		// rotate vector
//	friend idVec3 &		operator*=( idVec3 &v, const idRotation &r );			// rotate vector

        public idAngles ToAngles() {
            return ToMat3().ToAngles();
        }

//	idQuat				ToQuat( void ) const;
        public idMat3 ToMat3() {
            float wx, wy, wz;
            float xx, yy, yz;
            float xy, xz, zz;
            float x2, y2, z2;
            float a, x, y, z;
            final float[] c = new float[1], s = new float[1];

            if (this.axisValid) {
                return this.axis;
            }

            a = this.angle * (idMath.M_DEG2RAD * 0.5f);
            idMath.SinCos(a, s, c);

            x = this.vec.oGet(0) * s[0];
            y = this.vec.oGet(1) * s[0];
            z = this.vec.oGet(2) * s[0];

            x2 = x + x;
            y2 = y + y;
            z2 = z + z;

            xx = x * x2;
            xy = x * y2;
            xz = x * z2;

            yy = y * y2;
            yz = y * z2;
            zz = z * z2;

            wx = c[0] * x2;
            wy = c[0] * y2;
            wz = c[0] * z2;

            this.axis.oSet(0, 0, 1.0f - (yy + zz));
            this.axis.oSet(0, 1, xy - wz);
            this.axis.oSet(0, 2, xz + wy);

            this.axis.oSet(1, 0, xy + wz);
            this.axis.oSet(1, 1, 1.0f - (xx + zz));
            this.axis.oSet(1, 2, yz - wx);

            this.axis.oSet(2, 0, xz - wy);
            this.axis.oSet(2, 1, yz + wx);
            this.axis.oSet(2, 2, 1.0f - (xx + yy));

            this.axisValid = true;

            return this.axis;
        }
//	idMat4				ToMat4( void ) const;

        public idVec3 ToAngularVelocity() {
            return this.vec.oMultiply(DEG2RAD(this.angle));
        }

        public void RotatePoint(idVec3 point) {
            if (!this.axisValid) {
                ToMat3();
            }
            point.oSet((point.oMinus(this.origin)).oMultiply(this.axis).oPlus(this.origin));
        }

        public void Normalize180() {
            this.angle -= floor(this.angle / 360.0f) * 360.0f;
            if (this.angle > 180.0f) {
                this.angle -= 360.0f;
            } else if (this.angle < -180.0f) {
                this.angle += 360.0f;
            }
            final int a = 0;
        }
//	void				Normalize360( void );
//

        public void oSet(final idRotation other) {
            this.origin.oSet(other.origin);
            this.vec.oSet(other.vec);
            this.angle = other.angle;
            this.axisValid = other.axisValid;
        }
    }
}
