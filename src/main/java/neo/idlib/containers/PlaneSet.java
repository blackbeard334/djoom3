package neo.idlib.containers;

import static neo.idlib.math.Plane.PLANETYPE_NEGX;
import static neo.idlib.math.Plane.PLANETYPE_TRUEAXIAL;

import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;

/**
 *
 */
public class PlaneSet {

    /*
     ===============================================================================

     Plane Set

     ===============================================================================
     */
    public static class idPlaneSet extends idList<idPlane> {

        private idHashIndex hash;
        //
        //

        public idPlaneSet() {
            hash = new idHashIndex();
        }

        @Override
        public void Clear() {
            super.Clear();
            hash.Free();
        }

//
        public int FindPlane(final idPlane plane, final float normalEps, final float distEps) {
            int i, border, hashKey;

            assert (distEps <= 0.125f);

            hashKey = (int) (idMath.Fabs(plane.Dist()) * 0.125f);
            for (border = -1; border <= 1; border++) {
                for (i = hash.First(hashKey + border); i >= 0; i = hash.Next(i)) {
                    if (this.oGet(i).Compare(plane, normalEps, distEps)) {
                        return i;
                    }
                }
            }

            if (plane.Type() >= PLANETYPE_NEGX && plane.Type() < PLANETYPE_TRUEAXIAL) {
                Append(plane.oNegative());
                hash.Add(hashKey, Num() - 1);
                Append(plane);
                hash.Add(hashKey, Num() - 1);
                return (Num() - 1);
            } else {
                Append(plane);
                hash.Add(hashKey, Num() - 1);
                Append(plane.oNegative());
                hash.Add(hashKey, Num() - 1);
                return (Num() - 2);
            }
        }
    };
}
