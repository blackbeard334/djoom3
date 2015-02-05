package neo.Game.Physics;

import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_origin;
import static neo.idlib.math.Vector.vec3_zero;

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
            Kstretch = 100.0f;
            Kcompress = 100.0f;
            damping = 0.0f;
            restLength = 0.0f;
            physics1 = null;
            id1 = 0;
            p1 = vec3_zero;
            physics2 = null;
            id2 = 0;
            p2 = vec3_zero;
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

            pos1 = p1;
            pos2 = p2;
            velocity1 = velocity2 = vec3_origin;

            if (physics1 != null) {
                axis = physics1.GetAxis(id1);
                pos1 = physics1.GetOrigin(id1);
                pos1.oPluSet(p1.oMultiply(axis));
                if (damping > 0.0f) {
                    physics1.GetImpactInfo(id1, pos1, info);
                    velocity1 = info.velocity;
                }
            }

            if (physics2 != null) {
                axis = physics2.GetAxis(id2);
                pos2 = physics2.GetOrigin(id2);
                pos2.oPluSet(p2.oMultiply(axis));
                if (damping > 0.0f) {
                    physics2.GetImpactInfo(id2, pos2, info);
                    velocity2 = info.velocity;
                }
            }

            force = pos2.oMinus(pos1);
            dampingForce = force.oMultiply(damping * (((velocity2.oMinus(velocity1)).oMultiply(force)) / (force.oMultiply(force))));
            length = force.Normalize();

            // if the spring is stretched
            if (length > restLength) {
                if (Kstretch > 0.0f) {
                    force = force.oMultiply((float) (Square(length - restLength) * Kstretch)).oMinus(dampingForce);
                    if (physics1 != null) {
                        physics1.AddForce(id1, pos1, force);
                    }
                    if (physics2 != null) {
                        physics2.AddForce(id2, pos2, force.oNegative());
                    }
                }
            } else {
                if (Kcompress > 0.0f) {
                    force = force.oMultiply((float) (Square(length - restLength) * Kcompress)).oMinSet(dampingForce);
                    if (physics1 != null) {
                        physics1.AddForce(id1, pos1, force.oNegative());
                    }
                    if (physics2 != null) {
                        physics2.AddForce(id2, pos2, force);
                    }
                }
            }
        }

        @Override
        public void RemovePhysics(final idPhysics phys) {
            if (physics1.equals(phys)) {
                physics1 = null;
            }
            if (physics2.equals(phys)) {
                physics2 = null;
            }
        }
    };
}
