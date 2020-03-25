package neo.ui;

import static neo.idlib.math.Math_h.DEG2RAD;

import java.nio.ByteBuffer;

import neo.TempDump.SERiAL;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Rectangle {

//
// simple rectangle
//
//extern void RotateVector(idVec3 &v, idVec3 origin, float a, float c, float s);
    public static class idRectangle implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public float x;    // horiz position
        public float y;    // vert position
        public float w;    // width
        public float h;    // height;
//

        public idRectangle() {
            this.x = this.y = this.w = this.h = 0.0f;
        }

        public idRectangle(float ix, float iy, float iw, float ih) {
            this.x = ix;
            this.y = iy;
            this.w = iw;
            this.h = ih;
        }

        //copy constructor
        public idRectangle(idRectangle rectangle) {
            this(rectangle.x, rectangle.y, rectangle.w, rectangle.h);
        }

        public float Bottom() {
            return this.y + this.h;
        }

        public float Right() {
            return this.x + this.w;
        }

        public void Offset(float x, float y) {
            this.x += x;
            this.y += y;
        }

        public boolean Contains(float xt, float yt) {
            if ((this.w == 0.0) && (this.h == 0.0)) {
                return false;
            }
            if ((xt >= this.x) && (xt <= Right()) && (yt >= this.y) && (yt <= Bottom())) {
                return true;
            }
            return false;
        }

        public void Empty() {
            this.x = this.y = this.w = this.h = 0.0f;
        }

        public void ClipAgainst(idRectangle r, boolean sizeOnly) {
            if (!sizeOnly) {
                if (this.x < r.x) {
                    this.x = r.x;
                }
                if (this.y < r.y) {
                    this.y = r.y;
                }
            }
            if ((this.x + this.w) > (r.x + r.w)) {
                this.w = (r.x + r.w) - this.x;
            }
            if ((this.y + this.h) > (r.y + r.h)) {
                this.h = (r.y + r.h) - this.y;
            }
        }

        public void Rotate(float a, idRectangle out) {
            final idVec3 p1 = new idVec3(), p2 = new idVec3(), p3, p4 = new idVec3(), p5;
            float c, s;
            final idVec3 center = new idVec3((this.x + this.w) / 2.0f, (this.y + this.h) / 2.0f, 0);
            p1.Set(this.x, this.y, 0);
            p2.Set(Right(), this.y, 0);
            p4.Set(this.x, Bottom(), 0);
            if (a != 0) {
                s = (float) Math.sin(DEG2RAD(a));
                c = (float) Math.cos(DEG2RAD(a));
            } else {
                s = c = 0;
            }
            RotateVector(p1, center, a, c, s);//TODO:where is these functions!!
            RotateVector(p2, center, a, c, s);
            RotateVector(p4, center, a, c, s);
            out.x = p1.x;
            out.y = p1.y;
            out.w = (p2.oMinus(p1)).Length();
            out.h = (p4.oMinus(p1)).Length();
        }

        public idRectangle oPluSet(final idRectangle a) {
            this.x += a.x;
            this.y += a.y;
            this.w += a.w;
            this.h += a.h;

            return this;
        }

        public idRectangle oMinSet(final idRectangle a) {
            this.x -= a.x;
            this.y -= a.y;
            this.w -= a.w;
            this.h -= a.h;

            return this;
        }

        public idRectangle oDivSet(final idRectangle a) {
            this.x /= a.x;
            this.y /= a.y;
            this.w /= a.w;
            this.h /= a.h;

            return this;
        }

        public idRectangle oDivSet(final float a) {
            final float inva = 1.0f / a;
            this.x *= inva;
            this.y *= inva;
            this.w *= inva;
            this.h *= inva;

            return this;
        }

        public idRectangle oMulSet(final float a) {
            this.x *= a;
            this.y *= a;
            this.w *= a;
            this.h *= a;

            return this;
        }

        public idRectangle oSet(final idVec4 v) {
            this.x = v.x;
            this.y = v.y;
            this.w = v.z;
            this.h = v.w;

            return this;
        }

        public idRectangle oSet(final idRectangle r) {
            this.x = r.x;
            this.y = r.y;
            this.w = r.w;
            this.h = r.h;

            return this;
        }

//	int operator==(const idRectangle &a) const;
        @Override
        public int hashCode() {
            int hash = 7;
            hash = (19 * hash) + Float.floatToIntBits(this.x);
            hash = (19 * hash) + Float.floatToIntBits(this.y);
            hash = (19 * hash) + Float.floatToIntBits(this.w);
            hash = (19 * hash) + Float.floatToIntBits(this.h);
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
            final idRectangle other = (idRectangle) obj;
            if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            }
            if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
                return false;
            }
            if (Float.floatToIntBits(this.w) != Float.floatToIntBits(other.w)) {
                return false;
            }
            if (Float.floatToIntBits(this.h) != Float.floatToIntBits(other.h)) {
                return false;
            }
            return true;
        }

        public float oGet(final int index) {
            switch (index) {
                default:
                    return this.x;
                case 1:
                    return this.y;
                case 2:
                    return this.w;
                case 3:
                    return this.h;
            }
        }
        private static int index = 0;
        private static final char[][] str = new char[8][48];

        @Override
        public String toString() {
            String s;
            char[] temp;

            // use an array so that multiple toString's won't collide
            s = String.format("%.2f %.2f %.2f %.2f", this.x, this.y, this.w, this.h);
            temp = s.toCharArray();
            System.arraycopy(temp, 0, str[index], 0, temp.length);

            index = (index + 1) & 7;

            return s;
        }

        public idVec4 ToVec4() {
            return new idVec4(this.x, this.y, this.w, this.h);
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
    }

    static class idRegion {

        protected idList<idRectangle> rects;
        //
        //

        public idRegion() {
        }

        public void Empty() {
            this.rects.Clear();
        }

        public boolean Contains(float xt, float yt) {
            final int c = this.rects.Num();
            for (int i = 0; i < c; i++) {
                if (this.rects.oGet(i).Contains(xt, yt)) {
                    return true;
                }
            }
            return false;
        }

        public void AddRect(float x, float y, float w, float h) {
            this.rects.Append(new idRectangle(x, y, w, h));
        }

        int GetRectCount() {
            return this.rects.Num();
        }

        public idRectangle GetRect(int index) {
            if ((index >= 0) && (index < this.rects.Num())) {
                return this.rects.oGet(index);
            }
            return null;
        }
    }

    /*
     ================
     RotateVector
     ================
     */
    static void RotateVector(idVec3 v, idVec3 origin, float a, float c, float s) {
        float x = v.oGet(0);
        float y = v.oGet(1);
        if (a != 0) {
            final float x2 = (((x - origin.oGet(0)) * c) - ((y - origin.oGet(1)) * s)) + origin.oGet(0);
            final float y2 = (((x - origin.oGet(0)) * s) + ((y - origin.oGet(1)) * c)) + origin.oGet(1);
            x = x2;
            y = y2;
        }
        v.oSet(0, x);
        v.oSet(1, y);
    }
}
