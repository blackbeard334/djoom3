package neo.Game.Physics;

import static neo.idlib.math.Vector.getVec3_zero;

import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Force_Constant {

    /*
     ===============================================================================

     Constant force

     ===============================================================================
     */
    public static class idForce_Constant extends idForce {
        // CLASS_PROTOTYPE( idForce_Constant );

        // force properties
        private idVec3 force;
        private idPhysics physics;
        private int id;
        private idVec3 point;
        //
        //

        public idForce_Constant() {
            this.force = getVec3_zero();
            this.physics = null;
            this.id = 0;
            this.point = getVec3_zero();
        }
        // virtual				~idForce_Constant( void );

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.force);
            savefile.WriteInt(this.id);
            savefile.WriteVec3(this.point);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            // Owner needs to call SetPhysics!!
            savefile.ReadVec3(this.force);
            this.id = savefile.ReadInt();
            savefile.ReadVec3(this.point);
        }

        // constant force
        public void SetForce(final idVec3 force) {
            this.force = force;
        }
        // set force position

        public void SetPosition(idPhysics physics, int id, final idVec3 point) {
            this.physics = physics;
            this.id = id;
            this.point = point;
        }

        public void SetPhysics(idPhysics physics) {
            this.physics = physics;
        }

        // common force interface
        @Override
        public void Evaluate(int time) {
            idVec3 p;

            if (null == this.physics) {
                return;
            }

            p = this.physics.GetOrigin(this.id).oPlus(this.point.oMultiply(this.physics.GetAxis(this.id)));

            this.physics.AddForce(this.id, p, this.force);
        }

        @Override
        public void RemovePhysics(final idPhysics phys) {
            if (this.physics == phys) {
                this.physics = null;
            }
        }
    }
}
