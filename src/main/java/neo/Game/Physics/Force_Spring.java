package neo.Game.Physics;

import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Force_Spring {

    /*
     ===============================================================================

     Spring force

     ===============================================================================
     */
    public static class idForce_Spring extends idForce {
//	CLASS_PROTOTYPE( idForce_Spring );

        // spring properties
        private float Kstretch;
        private float Kcompress;
        private float damping;
        private float restLength;
        //
        // positioning
        private idPhysics physics1;	// first physics object
        private int id1;		// clip model id of first physics object
        private idVec3 p1;		// position on clip model
        private idPhysics physics2;	// second physics object
        private int id2;		// clip model id of second physics object
        private idVec3 p2;		// position on clip model
        //
        //

        public idForce_Spring() {
            this.Kstretch = 100.0f;
            this.Kcompress = 100.0f;
            this.damping = 0.0f;
            this.restLength = 0.0f;
            this.physics1 = null;
            this.id1 = 0;
            this.p1 = getVec3_zero();
            this.physics2 = null;
            this.id2 = 0;
            this.p2 = getVec3_zero();
        }
//	virtual				~idForce_Spring( void );

        // initialize the spring
        public void InitSpring(float Kstretch, float Kcompress, float damping, float restLength) {
            this.Kstretch = Kstretch;
            this.Kcompress = Kcompress;
            this.damping = damping;
            this.restLength = restLength;
        }

        // set the entities and positions on these entities the spring is attached to
        public void SetPosition(idPhysics physics1, int id1, final idVec3 p1, idPhysics physics2, int id2, final idVec3 p2) {
            this.physics1 = physics1;
            this.id1 = id1;
            this.p1 = p1;
            this.physics2 = physics2;
            this.id2 = id2;
            this.p2 = p2;
        }

        // common force interface
        @Override
        public void Evaluate(int time) {
            float length;
            idMat3 axis;
            idVec3 pos1, pos2, velocity1, velocity2, force, dampingForce;
            impactInfo_s info = new impactInfo_s();

            pos1 = this.p1;
            pos2 = this.p2;
            velocity1 = velocity2 = getVec3_origin();

            if (this.physics1 != null) {
                axis = this.physics1.GetAxis(this.id1);
                pos1 = this.physics1.GetOrigin(this.id1);
                pos1.oPluSet(this.p1.oMultiply(axis));
                if (this.damping > 0.0f) {
                    info = this.physics1.GetImpactInfo(this.id1, pos1);
                    velocity1 = info.velocity;
                }
            }

            if (this.physics2 != null) {
                axis = this.physics2.GetAxis(this.id2);
                pos2 = this.physics2.GetOrigin(this.id2);
                pos2.oPluSet(this.p2.oMultiply(axis));
                if (this.damping > 0.0f) {
                    info = this.physics2.GetImpactInfo(this.id2, pos2);
                    velocity2 = info.velocity;
                }
            }

            force = pos2.oMinus(pos1);
            dampingForce = force.oMultiply(this.damping * (((velocity2.oMinus(velocity1)).oMultiply(force)) / (force.oMultiply(force))));
            length = force.Normalize();

            // if the spring is stretched
            if (length > this.restLength) {
                if (this.Kstretch > 0.0f) {
                    force = force.oMultiply(Square(length - this.restLength) * this.Kstretch).oMinus(dampingForce);
                    if (this.physics1 != null) {
                        this.physics1.AddForce(this.id1, pos1, force);
                    }
                    if (this.physics2 != null) {
                        this.physics2.AddForce(this.id2, pos2, force.oNegative());
                    }
                }
            } else {
                if (this.Kcompress > 0.0f) {
                    force = force.oMultiply(Square(length - this.restLength) * this.Kcompress).oMinSet(dampingForce);
                    if (this.physics1 != null) {
                        this.physics1.AddForce(this.id1, pos1, force.oNegative());
                    }
                    if (this.physics2 != null) {
                        this.physics2.AddForce(this.id2, pos2, force);
                    }
                }
            }
        }

        @Override
        public void RemovePhysics(final idPhysics phys) {
            if (this.physics1.equals(phys)) {
                this.physics1 = null;
            }
            if (this.physics2.equals(phys)) {
                this.physics2 = null;
            }
        }
    }
}
