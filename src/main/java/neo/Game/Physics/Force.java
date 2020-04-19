package neo.Game.Physics;

import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.Physics.Physics.idPhysics;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class Force {

    /*
     ===============================================================================

     Force base class

     A force object applies a force to a physics object.

     ===============================================================================
     */
    public static class idForce extends idClass {
        // CLASS_PROTOTYPE( idForce );

        private static idList<idForce> forceList = new idList<>();
        //
        //

        public idForce() {
            forceList.Append(this);
        }

        // virtual				~idForce( void );
        protected void _deconstructor() {
            forceList.Remove(this);

            super._deconstructor();
        }

        public static void DeletePhysics(final idPhysics phys) {
            int i;

            for (i = 0; i < forceList.Num(); i++) {
                forceList.oGet(i).RemovePhysics(phys);
            }
        }

        public static void ClearForceList() {
            forceList.Clear();
        }

        // common force interface
        // evalulate the force up to the given time
        public void Evaluate(int time) {
        }

        // removes any pointers to the physics object
        public void RemovePhysics(idPhysics phys) {
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return null;
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
