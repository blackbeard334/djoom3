package neo.idlib.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import neo.TempDump;
import neo.TempDump.SERiAL;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.open.ColorUtil;
import neo.open.Nio;

/**
 *
 */
public class DrawVert {

    /*
     ===============================================================================

     Draw Vertex.

     ===============================================================================
     */
    public static class idDrawVert implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE
                = idVec3.SIZE
                + idVec2.SIZE
                + idVec3.SIZE
                + (2 * idVec3.SIZE)
                + (4 * Byte.SIZE);//color
        public static final transient int BYTES = SIZE / Byte.SIZE;

        private transient int VBO_OFFSET;

        public idVec3   xyz;
        public idVec2   st;
        public idVec3   normal;
        public idVec3[] tangents;
        //public byte[] color = new byte[4];
        private ByteBuffer color = Nio.newByteBuffer(4);
////#if 0 // was MACOS_X see comments concerning DRAWVERT_PADDED in Simd_Altivec.h 
////	float			padding;
////#endif
//public	float			operator[]( const int index ) const;
//public	float &			operator[]( const int index );
        private static int DBG_counter = 0;
        private final int DBG_count = DBG_counter++;

        public idDrawVert() {
            this.xyz = new idVec3();
            this.st = new idVec2();
            this.normal = new idVec3();
            this.tangents = new idVec3[]{new idVec3(), new idVec3()};
        }

        /**
         * copy constructor
         *
         * @param dv
         */
        public idDrawVert(idDrawVert dv) {
            if (null == dv) {
                this.xyz = new idVec3();
                this.st = new idVec2();
                this.normal = new idVec3();
                this.tangents = new idVec3[]{new idVec3(), new idVec3()};
                return;
            }

            this.xyz = new idVec3(dv.xyz);
            this.st = new idVec2(dv.st);
            this.normal = new idVec3(dv.normal);
            this.tangents = new idVec3[]{new idVec3(dv.tangents[0]), new idVec3(dv.tangents[1])};
        }

        public void oSet(final idDrawVert dv) {
            this.xyz.oSet(dv.xyz);
            this.st.oSet(dv.st);
            this.normal.oSet(dv.normal);
            this.tangents[0].oSet(dv.tangents[0]);
            this.tangents[1].oSet(dv.tangents[1]);
        }

        /**
         * cast constructor
         *
         * @param buffer
         */
        public idDrawVert(ByteBuffer buffer) {
            this();
            Read(buffer);
        }

        public float oGet(final int index) {
            switch (index) {
                case 0:
                case 1:
                case 2:
                    return this.xyz.oGet(index);
                case 3:
                case 4:
                    return this.st.oGet(index - 3);
                case 5:
                case 6:
                case 7:
                    return this.normal.oGet(index - 5);
                case 8:
                case 9:
                case 10:
                    return this.tangents[0].oGet(index - 8);
                case 11:
                case 12:
                case 13:
                    return this.tangents[1].oGet(index - 11);
                case 14:
                case 15:
                case 16:
                case 17:
                    return this.getColor().get(index - 14);
            }
            return -1;
        }

        public void Clear() {
            this.xyz.Zero();
            this.st.Zero();
            this.normal.Zero();
            this.tangents[0].Zero();
            this.tangents[1].Zero();
            ColorUtil.setElementsWith(this.getColor(), (byte) 0);
        }

        public void Lerp(final idDrawVert a, final idDrawVert b, final float f) {
            this.xyz.oSet(a.xyz.oPlus((b.xyz.oMinus(a.xyz)).oMultiply(f)));
            this.st.oSet(a.st.oPlus((b.st.oMinus(a.st)).oMultiply(f)));
        }

        public void LerpAll(final idDrawVert a, final idDrawVert b, final float f) {
            this.xyz.oSet(a.xyz.oPlus((b.xyz.oMinus(a.xyz)).oMultiply(f)));
            this.st.oSet(a.st.oPlus((b.st.oMinus(a.st)).oMultiply(f)));
            this.normal.oSet(a.normal.oPlus((b.normal.oMinus(a.normal)).oMultiply(f)));
            this.tangents[0].oSet(a.tangents[0].oPlus((b.tangents[0].oMinus(a.tangents[0])).oMultiply(f)));
            this.tangents[1].oSet(a.tangents[1].oPlus((b.tangents[1].oMinus(a.tangents[1])).oMultiply(f)));
    		for (int i = 0; i < 4; i++) {
    			this.getColor().put((byte) (a.getColor().get(i) + (f * (b.getColor().get(i) - a.getColor().get(i)))));
    		}
        }

        public void Normalize() {
            this.normal.Normalize();
            this.tangents[1].Cross(this.normal, this.tangents[0]);
            this.tangents[1].Normalize();
            this.tangents[0].Cross(this.tangents[1], this.normal);
            this.tangents[0].Normalize();
        }

        public void SetColor(long color) {
//	*reinterpret_cast<dword *>(this->color) = color;
//            this.color = this.set_reinterpret_cast(color);
            throw new TempDump.TODO_Exception();
        }

        public long GetColor() {
            return this.get_reinterpret_cast();
        }

        private long get_reinterpret_cast() {
            return (this.getColor().get(0) & 0x0000_00FF)
                    | (this.getColor().get(1) & 0x0000_FF00)
                    | (this.getColor().get(2) & 0x00FF_0000)
                    | (this.getColor().get(3) & 0xFF00_0000);
        }

        private short[] set_reinterpret_cast(long color) {
            return new short[]{
                (short) (color & 0x0000_00FF),
                (short) (color & 0x0000_FF00),
                (short) (color & 0x00FF_0000),
                (short) (color & 0xFF00_0000)};
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(final ByteBuffer buffer) {

            if (null == buffer) {
                return;
            }

            if (buffer.capacity() == (Integer.SIZE / Byte.SIZE)) {
                this.VBO_OFFSET = buffer.getInt(0);
                return;
            }

            this.xyz.oSet(0, buffer.getFloat());
            this.xyz.oSet(1, buffer.getFloat());
            this.xyz.oSet(2, buffer.getFloat());

            this.st.oSet(0, buffer.getFloat());
            this.st.oSet(1, buffer.getFloat());

            this.normal.oSet(0, buffer.getFloat());
            this.normal.oSet(1, buffer.getFloat());
            this.normal.oSet(2, buffer.getFloat());

            for (final idVec3 tan : this.tangents) {
                tan.oSet(0, buffer.getFloat());
                tan.oSet(1, buffer.getFloat());
                tan.oSet(2, buffer.getFloat());
            }

            ColorUtil.setElementsWith(this.getColor(), buffer.get());
        }

        @Override
        public ByteBuffer Write() {
            final ByteBuffer data = ByteBuffer.allocate(idDrawVert.BYTES);
            data.order(ByteOrder.LITTLE_ENDIAN);//very importante.

            data.putFloat(this.xyz.oGet(0));
            data.putFloat(this.xyz.oGet(1));
            data.putFloat(this.xyz.oGet(2));

            data.putFloat(this.st.oGet(0));
            data.putFloat(this.st.oGet(1));

            data.putFloat(this.normal.oGet(0));
            data.putFloat(this.normal.oGet(1));
            data.putFloat(this.normal.oGet(2));

            for (final idVec3 tan : this.tangents) {
                data.putFloat(tan.oGet(0));
                data.putFloat(tan.oGet(1));
                data.putFloat(tan.oGet(2));
            }

            ColorUtil.putElementsTo(this.getColor(), data);

            return data;
        }

        public int xyzOffset() {
            return this.VBO_OFFSET;
        }

        public int stOffset() {
            return xyzOffset() + idVec3.BYTES;//+xyz
        }

        public int normalOffset() {
            return stOffset() + idVec2.BYTES;//+xyz+st
        }

        public int tangentsOffset_0() {
            return normalOffset() + idVec3.BYTES;//+xyz+st+normal
        }

        public int tangentsOffset_1() {
            return tangentsOffset_0() + idVec3.BYTES;//+xyz+st+normal
        }

        public int colorOffset() {
            return tangentsOffset_1() + idVec3.BYTES;//+xyz+st+normal+tangents
        }

		public ByteBuffer getColor() {
			return color;
		}

		public void setColor(ByteBuffer color) {
			this.color.clear();
			this.color.put(color);
		}
    }

    public static ByteBuffer toByteBuffer(idDrawVert[] verts) {
        final ByteBuffer data = Nio.newByteBuffer(idDrawVert.BYTES * verts.length);

        for (final idDrawVert vert : verts) {
            data.put((ByteBuffer) vert.Write().rewind());
        }
//        System.out.printf("%d %d %d %d\n", data.get(0) & 0xff, data.get(1) & 0xff, data.get(2) & 0xff, data.get(3) & 0xff);
//        System.out.printf("%d %d %d %d\n", data.get(4) & 0xff, data.get(5) & 0xff, data.get(6) & 0xff, data.get(7) & 0xff);
//        System.out.printf("%f %f %f %f\n", data.getFloat(0), data.getFloat(4), data.getFloat(8), data.getFloat(12));

        return (ByteBuffer) data.flip();
    }
}
