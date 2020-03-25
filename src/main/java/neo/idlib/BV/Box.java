package neo.idlib.BV;

import static neo.idlib.math.Math_h.FLOATNOTZERO;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.Min3Index;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;

import java.util.Objects;

import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Sphere.idSphere;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVecX;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMatX;

/**
 *
 */
public class Box {

//                4---{4}---5
//     +         /|        /|
//     Z      {7} {8}   {5} |
//     -     /    |    /    {9}
//          7--{6}----6     |
//          |     |   |     |
//        {11}    0---|-{0}-1
//          |    /    |    /       -
//          | {3}  {10} {1}       Y
//          |/        |/         +
//          3---{2}---2
//
//            - X +
//
//      plane bits:
//      0 = min x
//      1 = max x
//      2 = min y
//      3 = max y
//      4 = min z
//      5 = max z



    /*
     static int boxVertPlanes[8] = {
     ( (1<<0) | (1<<2) | (1<<4) ),
     ( (1<<1) | (1<<2) | (1<<4) ),
     ( (1<<1) | (1<<3) | (1<<4) ),
     ( (1<<0) | (1<<3) | (1<<4) ),
     ( (1<<0) | (1<<2) | (1<<5) ),
     ( (1<<1) | (1<<2) | (1<<5) ),
     ( (1<<1) | (1<<3) | (1<<5) ),
     ( (1<<0) | (1<<3) | (1<<5) )
     };

     static int boxVertEdges[8][3] = {
     // bottom
     { 3, 0, 8 },
     { 0, 1, 9 },
     { 1, 2, 10 },
     { 2, 3, 11 },
     // top
     { 7, 4, 8 },
     { 4, 5, 9 },
     { 5, 6, 10 },
     { 6, 7, 11 }
     };

     static int boxEdgePlanes[12][2] = {
     // bottom
     { 4, 2 },
     { 4, 1 },
     { 4, 3 },
     { 4, 0 },
     // top
     { 5, 2 },
     { 5, 1 },
     { 5, 3 },
     { 5, 0 },
     // sides
     { 0, 2 },
     { 2, 1 },
     { 1, 3 },
     { 3, 0 }
     };

     static int boxEdgeVerts[12][2] = {
     // bottom
     { 0, 1 },
     { 1, 2 },
     { 2, 3 },
     { 3, 0 },
     // top
     { 4, 5 },
     { 5, 6 },
     { 6, 7 },
     { 7, 4 },
     // sides
     { 0, 4 },
     { 1, 5 },
     { 2, 6 },
     { 3, 7 }
     };
     */
    static final int[][] boxPlaneBitsSilVerts = {
        {0, 0, 0, 0, 0, 0, 0}, // 000000 = 0
        {4, 7, 4, 0, 3, 0, 0}, // 000001 = 1
        {4, 5, 6, 2, 1, 0, 0}, // 000010 = 2
        {0, 0, 0, 0, 0, 0, 0}, // 000011 = 3
        {4, 4, 5, 1, 0, 0, 0}, // 000100 = 4
        {6, 3, 7, 4, 5, 1, 0}, // 000101 = 5
        {6, 4, 5, 6, 2, 1, 0}, // 000110 = 6
        {0, 0, 0, 0, 0, 0, 0}, // 000111 = 7
        {4, 6, 7, 3, 2, 0, 0}, // 001000 = 8
        {6, 6, 7, 4, 0, 3, 2}, // 001001 = 9
        {6, 5, 6, 7, 3, 2, 1}, // 001010 = 10
        {0, 0, 0, 0, 0, 0, 0}, // 001011 = 11
        {0, 0, 0, 0, 0, 0, 0}, // 001100 = 12
        {0, 0, 0, 0, 0, 0, 0}, // 001101 = 13
        {0, 0, 0, 0, 0, 0, 0}, // 001110 = 14
        {0, 0, 0, 0, 0, 0, 0}, // 001111 = 15
        {4, 0, 1, 2, 3, 0, 0}, // 010000 = 16
        {6, 0, 1, 2, 3, 7, 4}, // 010001 = 17
        {6, 3, 2, 6, 5, 1, 0}, // 010010 = 18
        {0, 0, 0, 0, 0, 0, 0}, // 010011 = 19
        {6, 1, 2, 3, 0, 4, 5}, // 010100 = 20
        {6, 1, 2, 3, 7, 4, 5}, // 010101 = 21
        {6, 2, 3, 0, 4, 5, 6}, // 010110 = 22
        {0, 0, 0, 0, 0, 0, 0}, // 010111 = 23
        {6, 0, 1, 2, 6, 7, 3}, // 011000 = 24
        {6, 0, 1, 2, 6, 7, 4}, // 011001 = 25
        {6, 0, 1, 5, 6, 7, 3}, // 011010 = 26
        {0, 0, 0, 0, 0, 0, 0}, // 011011 = 27
        {0, 0, 0, 0, 0, 0, 0}, // 011100 = 28
        {0, 0, 0, 0, 0, 0, 0}, // 011101 = 29
        {0, 0, 0, 0, 0, 0, 0}, // 011110 = 30
        {0, 0, 0, 0, 0, 0, 0}, // 011111 = 31
        {4, 7, 6, 5, 4, 0, 0}, // 100000 = 32
        {6, 7, 6, 5, 4, 0, 3}, // 100001 = 33
        {6, 5, 4, 7, 6, 2, 1}, // 100010 = 34
        {0, 0, 0, 0, 0, 0, 0}, // 100011 = 35
        {6, 4, 7, 6, 5, 1, 0}, // 100100 = 36
        {6, 3, 7, 6, 5, 1, 0}, // 100101 = 37
        {6, 4, 7, 6, 2, 1, 0}, // 100110 = 38
        {0, 0, 0, 0, 0, 0, 0}, // 100111 = 39
        {6, 6, 5, 4, 7, 3, 2}, // 101000 = 40
        {6, 6, 5, 4, 0, 3, 2}, // 101001 = 41
        {6, 5, 4, 7, 3, 2, 1}, // 101010 = 42
        {0, 0, 0, 0, 0, 0, 0}, // 101011 = 43
        {0, 0, 0, 0, 0, 0, 0}, // 101100 = 44
        {0, 0, 0, 0, 0, 0, 0}, // 101101 = 45
        {0, 0, 0, 0, 0, 0, 0}, // 101110 = 46
        {0, 0, 0, 0, 0, 0, 0}, // 101111 = 47
        {0, 0, 0, 0, 0, 0, 0}, // 110000 = 48
        {0, 0, 0, 0, 0, 0, 0}, // 110001 = 49
        {0, 0, 0, 0, 0, 0, 0}, // 110010 = 50
        {0, 0, 0, 0, 0, 0, 0}, // 110011 = 51
        {0, 0, 0, 0, 0, 0, 0}, // 110100 = 52
        {0, 0, 0, 0, 0, 0, 0}, // 110101 = 53
        {0, 0, 0, 0, 0, 0, 0}, // 110110 = 54
        {0, 0, 0, 0, 0, 0, 0}, // 110111 = 55
        {0, 0, 0, 0, 0, 0, 0}, // 111000 = 56
        {0, 0, 0, 0, 0, 0, 0}, // 111001 = 57
        {0, 0, 0, 0, 0, 0, 0}, // 111010 = 58
        {0, 0, 0, 0, 0, 0, 0}, // 111011 = 59
        {0, 0, 0, 0, 0, 0, 0}, // 111100 = 60
        {0, 0, 0, 0, 0, 0, 0}, // 111101 = 61
        {0, 0, 0, 0, 0, 0, 0}, // 111110 = 62
        {0, 0, 0, 0, 0, 0, 0}, // 111111 = 63
    };

    /*
     ===============================================================================

     Oriented Bounding Box

     ===============================================================================
     */
    public static class idBox {

        private idVec3 center  = new idVec3();
        private idVec3 extents = new idVec3();
        private idMat3 axis    = new idMat3();
        //
        //

        public idBox() {
        }

        public idBox(final idVec3 center, final idVec3 extents, final idMat3 axis) {
            {
                this.center = new idVec3(center);
                this.extents = new idVec3(extents);
                this.axis = new idMat3(axis);
            }
        }

        public idBox(final idVec3 point) {
            this.center = new idVec3(point);
            this.extents.Zero();
            this.axis.Identity();
        }

        public idBox(final idBounds bounds) {
            this.center = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);
            this.extents = bounds.oGet(1).oMinus(this.center);
            this.axis.Identity();
        }

        public idBox(final idBounds bounds, final idVec3 origin, final idMat3 axis) {
            this.center = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);
            this.extents = bounds.oGet(1).oMinus(this.center);
            this.center = origin.oPlus(this.center.oMultiply(axis));
            this.axis = new idMat3(axis);
        }

        public idBox(final idBox box) {
            this.center = new idVec3(box.center);
            this.extents = new idVec3(box.extents);
            this.axis = new idMat3(box.axis);
        }
//

        public idBox oPlus(final idVec3 t) {                // returns translated box
            return new idBox(this.center.oPlus(t), this.extents, this.axis);
        }

        public idBox oPluSet(final idVec3 t) {                    // translate the box
            this.center.oPluSet(t);
            return this;
        }

        public idBox oMultiply(final idMat3 r) {                // returns rotated box
            return new idBox(this.center.oMultiply(r), this.extents, this.axis.oMultiply(r));
        }

        public idBox oMulSet(final idMat3 r) {                    // rotate the box
            this.center.oMulSet(r);
            this.axis.oMulSet(r);
            return this;
        }

        public idBox oPlus(final idBox a) {
            idBox newBox;
            newBox = new idBox(this);
            newBox.AddBox(a);
            return newBox;
        }

        public idBox oPluSet(final idBox a) {
            this.AddBox(a);
            return this;
        }

        public idBox oMinus(final idBox a) {
            return new idBox(this.center, this.extents.oMinus(a.extents), this.axis);
        }

        public idBox oMinSet(final idBox a) {
            this.extents.oMinSet(a.extents);
            return this;
        }
//

        public boolean Compare(final idBox a) {                        // exact compare, no epsilon
            return (this.center.Compare(a.center) && this.extents.Compare(a.extents) && this.axis.Compare(a.axis));
        }

        public boolean Compare(final idBox a, final float epsilon) {    // compare with epsilon
            return (this.center.Compare(a.center, epsilon) && this.extents.Compare(a.extents, epsilon) && this.axis.Compare(a.axis, epsilon));
        }
//public	boolean			operator==(	final idBox &a ) ;						// exact compare, no epsilon
//public	boolean			operator!=(	final idBox &a ) ;						// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (31 * hash) + Objects.hashCode(this.center);
            hash = (31 * hash) + Objects.hashCode(this.extents);
            hash = (31 * hash) + Objects.hashCode(this.axis);
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
            final idBox other = (idBox) obj;
            if (!Objects.equals(this.center, other.center)) {
                return false;
            }
            if (!Objects.equals(this.extents, other.extents)) {
                return false;
            }
            if (!Objects.equals(this.axis, other.axis)) {
                return false;
            }
            return true;
        }
//

        public void Clear() {                                    // inside out box
            this.center.Zero();
//            extents[0] = extents[1] = extents[2] = -idMath::INFINITY;
            this.extents.oSet(0, this.extents.oSet(1, this.extents.oSet(2, -idMath.INFINITY)));
            this.axis.Identity();
        }

        public void Zero() {                                    // single point at origin
            this.center.Zero();
            this.extents.Zero();
            this.axis.Identity();
        }

        // returns center of the box
        public idVec3 GetCenter() {
            return new idVec3(this.center);
        }

        // returns extents of the box
        public idVec3 GetExtents() {
            return new idVec3(this.extents);
        }

        // returns the axis of the box
        public idMat3 GetAxis() {
            return new idMat3(this.axis);
        }

        public float GetVolume() {                        // returns the volume of the box
            return (this.extents.oMultiply(2.0f)).LengthSqr();
        }

        public boolean IsCleared() {                        // returns true if box are inside out
            return this.extents.oGet(0) < 0.0f;
        }
//

        public boolean AddPoint(final idVec3 v) {                    // add the point, returns true if the box expanded
            final idMat3 axis2 = new idMat3();
            final idBounds bounds1 = new idBounds(), bounds2 = new idBounds();

            if (this.extents.oGet(0) < 0.0f) {
                this.extents.Zero();
                this.center = v;
                this.axis.Identity();
                return true;
            }

            bounds1.oSet(0, 0, bounds1.oSet(1, 0, this.center.oMultiply(this.axis.oGet(0))));
            bounds1.oSet(0, 1, bounds1.oSet(1, 1, this.center.oMultiply(this.axis.oGet(1))));
            bounds1.oSet(0, 2, bounds1.oSet(1, 2, this.center.oMultiply(this.axis.oGet(2))));
            bounds1.oGet(0).oMinSet(this.extents);
            bounds1.oGet(1).oPluSet(this.extents);
            if (!bounds1.AddPoint(new idVec3(v.oMultiply(this.axis.oGet(0)), v.oMultiply(this.axis.oGet(1)), v.oMultiply(this.axis.oGet(2))))) {
                // point is contained in the box
                return false;
            }

            axis2.oSet(0, v.oMinus(this.center));
            axis2.oGet(0).Normalize();
            axis2.oSet(1, this.axis.oGet(Min3Index(axis2.oGet(0).oMultiply(this.axis.oGet(0)), axis2.oGet(0).oMultiply(this.axis.oGet(1)), axis2.oGet(0).oMultiply(this.axis.oGet(2)))));
            axis2.oSet(1, axis2.oGet(1).oMinus(axis2.oGet(0).oMultiply(axis2.oGet(1).oMultiply(axis2.oGet(0)))));
            axis2.oGet(1).Normalize();
            axis2.oGet(2).Cross(axis2.oGet(0), axis2.oGet(1));

            AxisProjection(axis2, bounds2);
            bounds2.AddPoint(new idVec3(v.oMultiply(axis2.oGet(0)), v.oMultiply(axis2.oGet(1)), v.oMultiply(axis2.oGet(2))));

            // create new box based on the smallest bounds
            if (bounds1.GetVolume() < bounds2.GetVolume()) {
                this.center = (bounds1.oGet(0).oPlus(bounds1.oGet(1))).oMultiply(0.5f);
                this.extents = bounds1.oGet(1).oMinus(this.center);
                this.center.oMulSet(this.axis);
            } else {
                this.center = (bounds2.oGet(0).oPlus(bounds2.oGet(1))).oMultiply(0.5f);
                this.extents = bounds2.oGet(1).oMinus(this.center);
                this.center.oMulSet(axis2);
                this.axis = axis2;//TODO: new axis 2?
            }
            return true;
        }

        // add the box, returns true if the box expanded
        public boolean AddBox(final idBox a) {
            int i, besti;
            float v, bestv;
            idVec3 dir;
            final idMat3[] ax = new idMat3[4];
            final idBounds[] bounds = new idBounds[4];
            final idBounds b = new idBounds();

            if (a.extents.oGet(0) < 0.0f) {
                return false;
            }

            if (this.extents.oGet(0) < 0.0f) {
                this.center = new idVec3(a.center);
                this.extents = new idVec3(a.extents);
                this.axis = new idMat3(a.axis);
                return true;
            }

            // test axis of this box
            ax[0] = this.axis;
            bounds[0].oSet(0, 0, bounds[0].oSet(1, 0, this.center.oMultiply(ax[0].oGet(0))));
            bounds[0].oSet(0, 1, bounds[0].oSet(1, 1, this.center.oMultiply(ax[0].oGet(1))));
            bounds[0].oSet(0, 2, bounds[0].oSet(1, 2, this.center.oMultiply(ax[0].oGet(2))));
            bounds[0].oGet(0).oMinSet(this.extents);
            bounds[0].oGet(1).oPluSet(this.extents);
            a.AxisProjection(ax[0], b);
            if (!bounds[0].AddBounds(b)) {
                // the other box is contained in this box
                return false;
            }

            // test axis of other box
            ax[1] = a.axis;
            bounds[0].oSet(0, 0, bounds[0].oSet(1, 0, a.center.oMultiply(ax[0].oGet(0))));
            bounds[0].oSet(0, 1, bounds[0].oSet(1, 1, a.center.oMultiply(ax[0].oGet(1))));
            bounds[0].oSet(0, 2, bounds[0].oSet(1, 2, a.center.oMultiply(ax[0].oGet(2))));
            bounds[0].oGet(0).oMinSet(a.extents);
            bounds[0].oGet(1).oPluSet(a.extents);
            AxisProjection(ax[1], b);
            if (!bounds[1].AddBounds(b)) {
                // this box is contained in the other box
                this.center = new idVec3(a.center);
                this.extents = new idVec3(a.extents);
                this.axis = new idMat3(a.axis);
                return true;
            }

            // test axes aligned with the vector between the box centers and one of the box axis
            dir = a.center.oMinus(this.center);
            dir.Normalize();
            for (i = 2; i < 4; i++) {
                ax[i].oSet(0, dir);
                ax[i].oSet(1, ax[i - 2].oGet(Min3Index(dir.oMultiply(ax[i - 2].oGet(0)), dir.oMultiply(ax[i - 2].oGet(1)), dir.oMultiply(ax[i - 2].oGet(2)))));
                ax[i].oSet(1, ax[i].oGet(1).oMinus(dir.oMultiply(ax[i].oGet(1).oMultiply(dir))));
                ax[i].oGet(1).Normalize();
                ax[i].oGet(2).Cross(dir, ax[i].oGet(1));

                AxisProjection(ax[i], bounds[i]);
                a.AxisProjection(ax[i], b);
                bounds[i].AddBounds(b);
            }

            // get the bounds with the smallest volume
            bestv = idMath.INFINITY;
            besti = 0;
            for (i = 0; i < 4; i++) {
                v = bounds[i].GetVolume();
                if (v < bestv) {
                    bestv = v;
                    besti = i;
                }
            }

            // create a box from the smallest bounds axis pair
            this.center = (bounds[besti].oGet(0).oPlus(bounds[besti].oGet(1))).oMultiply(0.5f);
            this.extents = bounds[besti].oGet(1).oMinus(this.center);
            this.center.oMulSet(ax[besti]);
            this.axis = ax[besti];

            return false;
        }

        public idBox Expand(final float d) {					// return box expanded in all directions with the given value
            return new idBox(this.center, this.extents.oPlus(new idVec3(d, d, d)), this.axis);
        }

        public idBox ExpandSelf(final float d) {					// expand box in all directions with the given value 
            this.extents.oPluSet(0, d);
            this.extents.oPluSet(1, d);
            this.extents.oPluSet(2, d);
            return this;
        }

        public idBox Translate(final idVec3 translation) {	// return translated box
            return new idBox(this.center.oPlus(translation), this.extents, this.axis);
        }

        public idBox TranslateSelf(final idVec3 translation) {		// translate this box
            this.center.oPluSet(translation);
            return this;
        }

        public idBox Rotate(final idMat3 rotation) {			// return rotated box
            return new idBox(this.center.oMultiply(rotation), this.extents, this.axis.oMultiply(rotation));
        }

        public idBox RotateSelf(final idMat3 rotation) {			// rotate this box
            this.center.oMulSet(rotation);
            this.axis.oMulSet(rotation);
            return this;
        }
//

        public float PlaneDistance(final idPlane plane) {
            float d1, d2;

            d1 = plane.Distance(this.center);
            d2 = idMath.Fabs(this.extents.oGet(0) * plane.Normal().oGet(0))
                    + idMath.Fabs(this.extents.oGet(1) * plane.Normal().oGet(1))
                    + idMath.Fabs(this.extents.oGet(2) * plane.Normal().oGet(2));

            if ((d1 - d2) > 0.0f) {
                return d1 - d2;
            }
            if ((d1 + d2) < 0.0f) {
                return d1 + d2;
            }
            return 0.0f;
        }

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            float d1, d2;

            d1 = plane.Distance(this.center);
            d2 = idMath.Fabs(this.extents.oGet(0) * plane.Normal().oGet(0))
                    + idMath.Fabs(this.extents.oGet(1) * plane.Normal().oGet(1))
                    + idMath.Fabs(this.extents.oGet(2) * plane.Normal().oGet(2));

            if ((d1 - d2) > epsilon) {
                return PLANESIDE_FRONT;
            }
            if ((d1 + d2) < -epsilon) {
                return PLANESIDE_BACK;
            }
            return PLANESIDE_CROSS;
        }
//

        public boolean ContainsPoint(final idVec3 p) {			// includes touching
            final idVec3 lp = p.oMinus(this.center);
            if ((idMath.Fabs(lp.oMultiply(this.axis.oGet(0))) > this.extents.oGet(0))
                    || (idMath.Fabs(lp.oMultiply(this.axis.oGet(1))) > this.extents.oGet(1))
                    || (idMath.Fabs(lp.oMultiply(this.axis.oGet(2))) > this.extents.oGet(2))) {
                return false;
            }
            return true;
        }

        public boolean IntersectsBox(final idBox a) {			// includes touching
            idVec3 dir;			// vector between centers
            final float[][] c = new float[3][3];		// matrix c = axis.Transpose() * a.axis
            final float[][] ac = new float[3][3];		// absolute values of c
            final float[] axisdir = new float[3];	// axis[i] * dir
            float d, e0, e1;	// distance between centers and projected extents

            dir = a.center.oMinus(this.center);

            // axis C0 + t * A0
            c[0][0] = this.axis.oGet(0).oMultiply(a.axis.oGet(0));
            c[0][1] = this.axis.oGet(0).oMultiply(a.axis.oGet(1));
            c[0][2] = this.axis.oGet(0).oMultiply(a.axis.oGet(2));
            axisdir[0] = this.axis.oGet(0).oMultiply(dir);
            ac[0][0] = idMath.Fabs(c[0][0]);
            ac[0][1] = idMath.Fabs(c[0][1]);
            ac[0][2] = idMath.Fabs(c[0][2]);

            d = idMath.Fabs(axisdir[0]);
            e0 = this.extents.oGet(0);
            e1 = (a.extents.oGet(0) * ac[0][0]) + (a.extents.oGet(1) * ac[0][1]) + (a.extents.oGet(2) * ac[0][2]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A1
            c[1][0] = this.axis.oGet(1).oMultiply(a.axis.oGet(0));
            c[1][1] = this.axis.oGet(1).oMultiply(a.axis.oGet(1));
            c[1][2] = this.axis.oGet(1).oMultiply(a.axis.oGet(2));
            axisdir[1] = this.axis.oGet(1).oMultiply(dir);
            ac[1][0] = idMath.Fabs(c[1][0]);
            ac[1][1] = idMath.Fabs(c[1][1]);
            ac[1][2] = idMath.Fabs(c[1][2]);

            d = idMath.Fabs(axisdir[1]);
            e0 = this.extents.oGet(1);
            e1 = (a.extents.oGet(0) * ac[1][0]) + (a.extents.oGet(1) * ac[1][1]) + (a.extents.oGet(2) * ac[1][2]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A2
            c[2][0] = this.axis.oGet(2).oMultiply(a.axis.oGet(0));
            c[2][1] = this.axis.oGet(2).oMultiply(a.axis.oGet(1));
            c[2][2] = this.axis.oGet(2).oMultiply(a.axis.oGet(2));
            axisdir[2] = this.axis.oGet(2).oMultiply(dir);
            ac[2][0] = idMath.Fabs(c[2][0]);
            ac[2][1] = idMath.Fabs(c[2][1]);
            ac[2][2] = idMath.Fabs(c[2][2]);

            d = idMath.Fabs(axisdir[2]);
            e0 = this.extents.oGet(2);
            e1 = (a.extents.oGet(0) * ac[2][0]) + (a.extents.oGet(1) * ac[2][1]) + (a.extents.oGet(2) * ac[2][2]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * B0
            d = idMath.Fabs(a.axis.oGet(0).oMultiply(dir));
            e0 = (this.extents.oGet(0) * ac[0][0]) + (this.extents.oGet(1) * ac[1][0]) + (this.extents.oGet(2) * ac[2][0]);
            e1 = a.extents.oGet(0);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * B1
            d = idMath.Fabs(a.axis.oGet(1).oMultiply(dir));
            e0 = (this.extents.oGet(0) * ac[0][1]) + (this.extents.oGet(1) * ac[1][1]) + (this.extents.oGet(2) * ac[2][1]);
            e1 = a.extents.oGet(1);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * B2
            d = idMath.Fabs(a.axis.oGet(2).oMultiply(dir));
            e0 = (this.extents.oGet(0) * ac[0][2]) + (this.extents.oGet(1) * ac[1][2]) + (this.extents.oGet(2) * ac[2][2]);
            e1 = a.extents.oGet(2);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A0xB0
            d = idMath.Fabs((axisdir[2] * c[1][0]) - (axisdir[1] * c[2][0]));
            e0 = (this.extents.oGet(1) * ac[2][0]) + (this.extents.oGet(2) * ac[1][0]);
            e1 = (a.extents.oGet(1) * ac[0][2]) + (a.extents.oGet(2) * ac[0][1]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A0xB1
            d = idMath.Fabs((axisdir[2] * c[1][1]) - (axisdir[1] * c[2][1]));
            e0 = (this.extents.oGet(1) * ac[2][1]) + (this.extents.oGet(2) * ac[1][1]);
            e1 = (a.extents.oGet(0) * ac[0][2]) + (a.extents.oGet(2) * ac[0][0]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A0xB2
            d = idMath.Fabs((axisdir[2] * c[1][2]) - (axisdir[1] * c[2][2]));
            e0 = (this.extents.oGet(1) * ac[2][2]) + (this.extents.oGet(2) * ac[1][2]);
            e1 = (a.extents.oGet(0) * ac[0][1]) + (a.extents.oGet(1) * ac[0][0]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A1xB0
            d = idMath.Fabs((axisdir[0] * c[2][0]) - (axisdir[2] * c[0][0]));
            e0 = (this.extents.oGet(0) * ac[2][0]) + (this.extents.oGet(2) * ac[0][0]);
            e1 = (a.extents.oGet(1) * ac[1][2]) + (a.extents.oGet(2) * ac[1][1]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A1xB1
            d = idMath.Fabs((axisdir[0] * c[2][1]) - (axisdir[2] * c[0][1]));
            e0 = (this.extents.oGet(0) * ac[2][1]) + (this.extents.oGet(2) * ac[0][1]);
            e1 = (a.extents.oGet(0) * ac[1][2]) + (a.extents.oGet(2) * ac[1][0]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A1xB2
            d = idMath.Fabs((axisdir[0] * c[2][2]) - (axisdir[2] * c[0][2]));
            e0 = (this.extents.oGet(0) * ac[2][2]) + (this.extents.oGet(2) * ac[0][2]);
            e1 = (a.extents.oGet(0) * ac[1][1]) + (a.extents.oGet(1) * ac[1][0]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A2xB0
            d = idMath.Fabs((axisdir[1] * c[0][0]) - (axisdir[0] * c[1][0]));
            e0 = (this.extents.oGet(0) * ac[1][0]) + (this.extents.oGet(1) * ac[0][0]);
            e1 = (a.extents.oGet(1) * ac[2][2]) + (a.extents.oGet(2) * ac[2][1]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A2xB1
            d = idMath.Fabs((axisdir[1] * c[0][1]) - (axisdir[0] * c[1][1]));
            e0 = (this.extents.oGet(0) * ac[1][1]) + (this.extents.oGet(1) * ac[0][1]);
            e1 = (a.extents.oGet(0) * ac[2][2]) + (a.extents.oGet(2) * ac[2][0]);
            if (d > (e0 + e1)) {
                return false;
            }

            // axis C0 + t * A2xB2
            d = idMath.Fabs((axisdir[1] * c[0][2]) - (axisdir[0] * c[1][2]));
            e0 = (this.extents.oGet(0) * ac[1][2]) + (this.extents.oGet(1) * ac[0][2]);
            e1 = (a.extents.oGet(0) * ac[2][1]) + (a.extents.oGet(1) * ac[2][0]);
            if (d > (e0 + e1)) {
                return false;
            }
            return true;
        }

        /*
         ============
         idBox::LineIntersection

         Returns true if the line intersects the box between the start and end point.
         ============
         */
        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            final float[] ld = new float[3];
            final idVec3 lineDir = (end.oMinus(start)).oMultiply(0.5f);
            final idVec3 lineCenter = start.oPlus(lineDir);
            final idVec3 dir = lineCenter.oMinus(this.center);

            ld[0] = idMath.Fabs(lineDir.oMultiply(this.axis.oGet(0)));
            if (idMath.Fabs(dir.oMultiply(this.axis.oGet(0))) > (this.extents.oGet(0) + ld[0])) {
                return false;
            }

            ld[1] = idMath.Fabs(lineDir.oMultiply(this.axis.oGet(1)));
            if (idMath.Fabs(dir.oMultiply(this.axis.oGet(1))) > (this.extents.oGet(1) + ld[1])) {
                return false;
            }

            ld[2] = idMath.Fabs(lineDir.oMultiply(this.axis.oGet(2)));
            if (idMath.Fabs(dir.oMultiply(this.axis.oGet(2))) > (this.extents.oGet(2) + ld[2])) {
                return false;
            }

            final idVec3 cross = lineDir.Cross(dir);

            if (idMath.Fabs(cross.oMultiply(this.axis.oGet(0))) > ((this.extents.oGet(1) * ld[2]) + (this.extents.oGet(2) * ld[1]))) {
                return false;
            }

            if (idMath.Fabs(cross.oMultiply(this.axis.oGet(1))) > ((this.extents.oGet(0) * ld[2]) + (this.extents.oGet(2) * ld[0]))) {
                return false;
            }

            if (idMath.Fabs(cross.oMultiply(this.axis.oGet(2))) > ((this.extents.oGet(0) * ld[1]) + (this.extents.oGet(1) * ld[0]))) {
                return false;
            }

            return true;
        }

        /*
         ============
         idBox::RayIntersection

         Returns true if the ray intersects the box.
         The ray can intersect the box in both directions from the start point.
         If start is inside the box then scale1 < 0 and scale2 > 0.
         ============
         */
        // intersection points are (start + dir * scale1) and (start + dir * scale2)
        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale1, float[] scale2) {
            idVec3 localStart, localDir;

            localStart = (start.oMinus(this.center)).oMultiply(this.axis.Transpose());
            localDir = dir.oMultiply(this.axis.Transpose());

            scale1[0] = -idMath.INFINITY;
            scale2[0] = idMath.INFINITY;
            return BoxPlaneClip(localDir.x, -localStart.x - this.extents.oGet(0), scale1, scale2)
                    && BoxPlaneClip(-localDir.x, localStart.x - this.extents.oGet(0), scale1, scale2)
                    && BoxPlaneClip(localDir.y, -localStart.y - this.extents.oGet(1), scale1, scale2)
                    && BoxPlaneClip(-localDir.y, localStart.y - this.extents.oGet(1), scale1, scale2)
                    && BoxPlaneClip(localDir.z, -localStart.z - this.extents.oGet(2), scale1, scale2)
                    && BoxPlaneClip(-localDir.z, localStart.z - this.extents.oGet(2), scale1, scale2);
        }

//

        /*
         ============
         idBox::FromPoints

         Tight box for a collection of points.
         ============
         */
        // tight box for a collection of points
        public void FromPoints(final idVec3[] points, final int numPoints) {
            int i;
            float invNumPoints, sumXX, sumXY, sumXZ, sumYY, sumYZ, sumZZ;
            idVec3 dir;
            final idBounds bounds = new idBounds();
            final idMatX eigenVectors = new idMatX();
            final idVecX eigenValues = new idVecX();

            // compute mean of points
            this.center = points[0];
            for (i = 1; i < numPoints; i++) {
                this.center.oPluSet(points[i]);
            }
            invNumPoints = 1.0f / numPoints;
            this.center.oMulSet(invNumPoints);

            // compute covariances of points
            sumXX = 0.0f;
            sumXY = 0.0f;
            sumXZ = 0.0f;
            sumYY = 0.0f;
            sumYZ = 0.0f;
            sumZZ = 0.0f;
            for (i = 0; i < numPoints; i++) {
                dir = points[i].oMinus(this.center);
                sumXX += dir.x * dir.x;
                sumXY += dir.x * dir.y;
                sumXZ += dir.x * dir.z;
                sumYY += dir.y * dir.y;
                sumYZ += dir.y * dir.z;
                sumZZ += dir.z * dir.z;
            }
            sumXX *= invNumPoints;
            sumXY *= invNumPoints;
            sumXZ *= invNumPoints;
            sumYY *= invNumPoints;
            sumYZ *= invNumPoints;
            sumZZ *= invNumPoints;

            // compute eigenvectors for covariance matrix
            eigenValues.SetData(3, Vector.idVecX.VECX_ALLOCA(3));
            eigenVectors.SetData(3, 3, idMatX.MATX_ALLOCA(3 * 3));

            eigenVectors.oSet(0, 0, sumXX);
            eigenVectors.oSet(0, 1, sumXY);
            eigenVectors.oSet(0, 2, sumXZ);
            eigenVectors.oSet(1, 0, sumXY);
            eigenVectors.oSet(1, 1, sumYY);
            eigenVectors.oSet(1, 2, sumYZ);
            eigenVectors.oSet(2, 0, sumXZ);
            eigenVectors.oSet(2, 1, sumYZ);
            eigenVectors.oSet(2, 2, sumZZ);
            eigenVectors.Eigen_SolveSymmetric(eigenValues);
            eigenVectors.Eigen_SortIncreasing(eigenValues);

            this.axis.oSet(0, 0, eigenVectors.oGet(0)[0]);
            this.axis.oSet(0, 1, eigenVectors.oGet(0)[1]);
            this.axis.oSet(0, 2, eigenVectors.oGet(0)[2]);
            this.axis.oSet(1, 0, eigenVectors.oGet(1)[0]);
            this.axis.oSet(1, 1, eigenVectors.oGet(1)[1]);
            this.axis.oSet(1, 2, eigenVectors.oGet(1)[2]);
            this.axis.oSet(2, 0, eigenVectors.oGet(2)[0]);
            this.axis.oSet(2, 1, eigenVectors.oGet(2)[1]);
            this.axis.oSet(2, 2, eigenVectors.oGet(2)[2]);

            this.extents.oSet(0, eigenValues.p[0]);
            this.extents.oSet(1, eigenValues.p[0]);
            this.extents.oSet(2, eigenValues.p[0]);

            // refine by calculating the bounds of the points projected onto the axis and adjusting the center and extents
            bounds.Clear();
            for (i = 0; i < numPoints; i++) {
                bounds.AddPoint(new idVec3(
                        points[i].oMultiply(this.axis.oGet(0)),
                        points[i].oMultiply(this.axis.oGet(1)),
                        points[i].oMultiply(this.axis.oGet(2))));
            }
            this.center = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);
            this.extents = bounds.oGet(1).oMinus(this.center);
            this.center.oMulSet(this.axis);
        }
//					// most tight box for a translation
//public	void			FromPointTranslation( final idVec3 &point, final idVec3 &translation );
//public	void			FromBoxTranslation( final idBox &box, final idVec3 &translation );
//					// most tight box for a rotation
//public	void			FromPointRotation( final idVec3 &point, final idRotation &rotation );
//public	void			FromBoxRotation( final idBox &box, final idRotation &rotation );
//

        public void ToPoints(idVec3[] points) {
            final idMat3 ax = new idMat3();
            final idVec3[] temp = new idVec3[4];

            ax.oSet(0, this.axis.oGet(0).oMultiply(this.extents.oGet(0)));
            ax.oSet(1, this.axis.oGet(1).oMultiply(this.extents.oGet(1)));
            ax.oSet(2, this.axis.oGet(2).oMultiply(this.extents.oGet(2)));
            temp[0] = this.center.oMinus(ax.oGet(0));
            temp[1] = this.center.oPlus(ax.oGet(0));
            temp[2] = ax.oGet(1).oMinus(ax.oGet(2));
            temp[3] = ax.oGet(1).oPlus(ax.oGet(2));
            points[0] = temp[0].oMinus(temp[3]);
            points[1] = temp[1].oMinus(temp[3]);
            points[2] = temp[1].oPlus(temp[2]);
            points[3] = temp[0].oPlus(temp[2]);
            points[4] = temp[0].oMinus(temp[2]);
            points[5] = temp[1].oMinus(temp[2]);
            points[6] = temp[1].oPlus(temp[3]);
            points[7] = temp[0].oPlus(temp[3]);
        }

        public idSphere ToSphere() {
            return new idSphere(this.center, this.extents.Length());
        }
//
//					// calculates the projection of this box onto the given axis

        public void AxisProjection(final idVec3 dir, float[] min, float[] max) {
            final float d1 = dir.oMultiply(this.center);
            final float d2 = idMath.Fabs(this.extents.oGet(0) * (dir.oMultiply(this.axis.oGet(0))))
                    + idMath.Fabs(this.extents.oGet(1) * (dir.oMultiply(this.axis.oGet(1))))
                    + idMath.Fabs(this.extents.oGet(2) * (dir.oMultiply(this.axis.oGet(2))));
            min[0] = d1 - d2;
            max[0] = d1 + d2;
        }

        public void AxisProjection(final idMat3 ax, idBounds bounds) {
            for (int i = 0; i < 3; i++) {

                final float d1 = ax.oGet(i).oMultiply(this.center);
                final float d2 = idMath.Fabs(this.extents.oGet(0) * (ax.oGet(i).oMultiply(this.axis.oGet(0))))
                        + idMath.Fabs(this.extents.oGet(1) * (ax.oGet(i).oMultiply(this.axis.oGet(1))))
                        + idMath.Fabs(this.extents.oGet(2) * (ax.oGet(i).oMultiply(this.axis.oGet(2))));

                bounds.oSet(0, i, d1 - d2);
                bounds.oSet(1, i, d1 + d2);
            }
        }
//

        // calculates the silhouette of the box
        public int GetProjectionSilhouetteVerts(final idVec3 projectionOrigin, idVec3[] silVerts) {
            float f;
            int i, planeBits;
            int[] index;
            final idVec3[] points = new idVec3[8];
            idVec3 dir1, dir2;

            ToPoints(points);

            dir1 = points[0].oMinus(projectionOrigin);
            dir2 = points[6].oMinus(projectionOrigin);
            f = dir1.oMultiply(this.axis.oGet(0));
            planeBits = FLOATSIGNBITNOTSET(f);
            f = dir2.oMultiply(this.axis.oGet(0));
            planeBits |= FLOATSIGNBITSET(f) << 1;
            f = dir1.oMultiply(this.axis.oGet(1));
            planeBits |= FLOATSIGNBITNOTSET(f) << 2;
            f = dir2.oMultiply(this.axis.oGet(1));
            planeBits |= FLOATSIGNBITSET(f) << 3;
            f = dir1.oMultiply(this.axis.oGet(2));
            planeBits |= FLOATSIGNBITNOTSET(f) << 4;
            f = dir2.oMultiply(this.axis.oGet(2));
            planeBits |= FLOATSIGNBITSET(f) << 5;

            index = boxPlaneBitsSilVerts[planeBits];
            for (i = 0; i < index[0]; i++) {
                silVerts[i] = points[index[i + 1]];
            }

            return index[0];
        }

        public int GetParallelProjectionSilhouetteVerts(final idVec3 projectionDir, idVec3[] silVerts) {
            float f;
            int i, planeBits;
            int[] index;
            final idVec3[] points = new idVec3[8];

            ToPoints(points);

            planeBits = 0;
            f = projectionDir.oMultiply(this.axis.oGet(0));
            if (FLOATNOTZERO(f)) {
                planeBits = 1 << FLOATSIGNBITSET(f);
            }
            f = projectionDir.oMultiply(this.axis.oGet(1));
            if (FLOATNOTZERO(f)) {
                planeBits |= 4 << FLOATSIGNBITSET(f);
            }
            f = projectionDir.oMultiply(this.axis.oGet(2));
            if (FLOATNOTZERO(f)) {
                planeBits |= 16 << FLOATSIGNBITSET(f);
            }

            index = boxPlaneBitsSilVerts[planeBits];
            for (i = 0; i < index[0]; i++) {
                silVerts[i] = points[index[i + 1]];
            }

            return index[0];
        }

    }

    /*
     ============
     BoxPlaneClip
     ============
     */
    static boolean BoxPlaneClip(final float denom, final float numer, float[] scale0, float[] scale1) {
        if (denom > 0.0f) {
            if (numer > (denom * scale1[0])) {
                return false;
            }
            if (numer > (denom * scale0[0])) {
                scale0[0] = numer / denom;
            }
            return true;
        } else if (denom < 0.0f) {
            if (numer > (denom * scale0[0])) {
                return false;
            }
            if (numer > (denom * scale1[0])) {
                scale1[0] = numer / denom;
            }
            return true;
        } else {
            return (numer <= 0.0f);
        }
    }
}
