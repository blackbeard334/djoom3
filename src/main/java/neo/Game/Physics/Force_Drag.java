package neo.Game.Physics;

import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.idlib.math.Math_h.MS2SEC;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.RAD2DEG;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_zero;

/**
 *
 */
public class Force_Drag {

    /*
     ===============================================================================

     Drag force

     ===============================================================================
     */
    public static class idForce_Drag extends idForce {
        // CLASS_PROTOTYPE( idForce_Drag );

        // properties
        private float damping;
        //
        // positioning
        private idPhysics physics;	// physics object
        private int id;			// clip model id of physics object
        private idVec3 p;		// position on clip model
        private idVec3 dragPosition;	// drag towards this position
        //
        //

        public idForce_Drag() {
            damping = 0.5f;
            dragPosition = vec3_zero;
            physics = null;
            id = 0;
            p = vec3_zero;
            dragPosition = vec3_zero;
        }
        // virtual				~idForce_Drag( void );

        // initialize the drag force
        public void Init(float damping) {
            if (damping >= 0.0f && damping < 1.0f) {
                this.damping = damping;
            }
        }

        // set physics object being dragged
        public void SetPhysics(idPhysics phys, int id, final idVec3 p) {
            this.physics = phys;
            this.id = id;
            this.p = p;
        }

        // set position to drag towards
        public void SetDragPosition(final idVec3 pos) {
            this.dragPosition = pos;
        }

        // get the position dragged towards
        public idVec3 GetDragPosition() {
            return this.dragPosition;
        }

        // get the position on the dragged physics object
        public idVec3 GetDraggedPosition() {
            return physics.GetOrigin(id).oPlus(p.oMultiply(physics.GetAxis(id)));
        }

        // common force interface
        @Override
        public void Evaluate(int time) {
            float l1, l2;
            float[] mass = new float[1];
            idVec3 dragOrigin, dir1, dir2, velocity, centerOfMass = new idVec3();
            idMat3 inertiaTensor = new idMat3();
            idRotation rotation = new idRotation();
            idClipModel clipModel;

            if (null == physics) {
                return;
            }

            clipModel = physics.GetClipModel(id);
            if (clipModel != null && clipModel.IsTraceModel()) {
                clipModel.GetMassProperties(1.0f, mass, centerOfMass, inertiaTensor);
            } else {
                centerOfMass.Zero();
            }

            centerOfMass = physics.GetOrigin(id).oPlus(centerOfMass.oMultiply(physics.GetAxis(id)));
            dragOrigin = physics.GetOrigin(id).oPlus(p.oMultiply(physics.GetAxis(id)));

            dir1 = dragPosition.oMinus(centerOfMass);
            dir2 = dragOrigin.oMinus(centerOfMass);
            l1 = dir1.Normalize();
            l2 = dir2.Normalize();

            rotation.Set(centerOfMass, dir2.Cross(dir1), RAD2DEG(idMath.ACos(dir1.oMultiply(dir2))));
            physics.SetAngularVelocity(rotation.ToAngularVelocity().oDivide(MS2SEC(USERCMD_MSEC)), id);

            velocity = physics.GetLinearVelocity(id).oMultiply(damping).oPlus(dir1.oMultiply((l1 - l2) * (1.0f - damping) / MS2SEC(USERCMD_MSEC)));
            physics.SetLinearVelocity(velocity, id);
        }

        @Override
        public void RemovePhysics(final idPhysics phys) {
            if (physics.equals(phys)) {
                physics = null;
            }
        }
    };
}
