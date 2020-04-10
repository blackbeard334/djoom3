package neo.Renderer;

import static neo.framework.Common.common;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorYellow;

import java.util.ArrayList;

import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.viewDef_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat4;

/**
 *
 */
public class tr_shadowbounds {

// Compute conservative shadow bounds as the intersection
// of the object's bounds' shadow volume and the light's bounds.
// 
// --cass
    static class MyArray<T> {

        //private MyArray() {
        //    this.N = -1;
        //}

        public MyArray(final int N) //: s(0) 
        {
        	//this.N = N;
            this.v = new ArrayList<T>();
        }

        public MyArray(final int N, final MyArray<T> cpy) //: s(cpy.s)
        {
            //this.N = N;
            this.v = new ArrayList<T>();
            for (int i = 0; i < cpy.size(); i++) {
            	this.v.add(cpy.v.get(i));
            }
        }
        
        public void push_back(final T i) {
            //this.v[this.s] = i;
        	this.v.add(i);
            //this.s++;
            //if(s > max_size)
            //	max_size = int(s);
        }

        public T oGet(final int index) {
            return this.v.get(index);
        }

        public T oSet(final int index, final T value) {
            return this.v.set(index, value);
        }

//	const T & operator[](int i) const {
//		return v[i];
//	}
        int size() {
            return this.v.size();
        }

        void empty() {
            this.v.clear();
        }
//
       // private final int N;
        ArrayList<T> v;
        //T[] v;// = (T[]) new Object[N];
        //int s;
//	static int max_size;
    }

    static class MyArrayInt extends MyArray<Integer> {

        public MyArrayInt() {
            super(N);
        }
        private static final int N = 4;
    }
//int MyArrayInt::max_size = 0;

    static class MyArrayVec4 extends MyArray<idVec4> {

        public MyArrayVec4() {
            super(N);
        }
        private static final int N = 16;
    }
//int MyArrayVec4::max_size = 0;

    static class poly {

        MyArrayInt vi;
        MyArrayInt ni;
        idVec4 plane;
    }

    static class MyArrayPoly extends MyArray<poly> {

        public MyArrayPoly() {
            super(N);
        }
        private static final int N = 9;
    }
//int MyArrayPoly::max_size = 0;

    static class edge {

        int[] vi = new int[2];
        int[] pi = new int[2];
    }

    static class MyArrayEdge extends MyArray<edge> {

        public MyArrayEdge() {
            super(N);
        }
        private static final int N = 15;
    }
//int MyArrayEdge::max_size = 0;

    public static MyArrayInt four_ints(int a, int b, int c, int d) {
        final MyArrayInt vi = new MyArrayInt();
        vi.push_back(a);
        vi.push_back(b);
        vi.push_back(c);
        vi.push_back(d);
        return vi;
    }

    public static idVec3 homogeneous_difference(idVec4 a, idVec4 b) {
        final idVec3 v = new idVec3();
        v.x = (b.x * a.w) - (a.x * b.w);
        v.y = (b.y * a.w) - (a.y * b.w);
        v.z = (b.z * a.w) - (a.z * b.w);
        return v;
    }

// handles positive w only
    public static idVec4 compute_homogeneous_plane(idVec4 a, idVec4 b, idVec4 c) {
        final idVec4 v = new idVec4();
		idVec4 t;

        if (a.oGet(3) == 0) {
            t = a;
            a = b;
            b = c;
            c = t;
        }
        if (a.oGet(3) == 0) {
            t = a;
            a = b;
            b = c;
            c = t;
        }

        // can't handle 3 infinite points
        if (a.oGet(3) == 0) {
            return v;
        }

        final idVec3 vb = homogeneous_difference(a, b);
        final idVec3 vc = homogeneous_difference(a, c);

        final idVec3 n = vb.Cross(vc);
        n.Normalize();

        v.x = n.x;
        v.y = n.y;
        v.z = n.z;

        v.w = -n.oMultiply(new idVec3(a.x, a.y, a.z)) / a.w;

        return v;
    }

    static class polyhedron {

        MyArrayVec4 v;
        MyArrayPoly p;
        MyArrayEdge e;

        private polyhedron() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private polyhedron(polyhedron p) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        void add_quad(int va, int vb, int vc, int vd) {
            final poly pg = new poly();
            pg.vi = four_ints(va, vb, vc, vd);
            pg.ni = four_ints(-1, -1, -1, -1);
            pg.plane = compute_homogeneous_plane(this.v.oGet(va), this.v.oGet(vb), this.v.oGet(vc));
            this.p.push_back(pg);
        }

        void discard_neighbor_info() {
            for (int i = 0; i < this.p.size(); i++) {
                final MyArrayInt ni = this.p.oGet(i).ni;
                for (int j = 0; j < ni.size(); j++) {
                    ni.oSet(j, -1);
                }
            }
        }

        void compute_neighbors() {
            this.e.empty();

            discard_neighbor_info();

            boolean found;
            final int P = this.p.size();
            // for each polygon
            for (int i = 0; i < (P - 1); i++) {
                final MyArrayInt vi = this.p.oGet(i).vi;
                final MyArrayInt ni = this.p.oGet(i).ni;
                final int Si = vi.size();

                // for each edge of that polygon
                for (int ii = 0; ii < Si; ii++) {
                    final int ii0 = ii;
                    final int ii1 = (ii + 1) % Si;

                    // continue if we've already found this neighbor
                    if (ni.oGet(ii) != -1) {
                        continue;
                    }
                    found = false;
                    // check all remaining polygons
                    for (int j = i + 1; j < P; j++) {
                        final MyArrayInt vj = this.p.oGet(j).vi;
                        //final MyArrayInt nj = this.p.oGet(j).ni;
                        final int Sj = vj.size();

                        for (int jj = 0; jj < Sj; jj++) {
                            final int jj0 = jj;
                            final int jj1 = (jj + 1) % Sj;
                            if ((vi.oGet(ii0) == vj.oGet(jj1)) && (vi.oGet(ii1) == vj.oGet(jj0))) {
                                final edge ed = new edge();
                                ed.vi[0] = vi.oGet(ii0);
                                ed.vi[1] = vi.oGet(ii1);
                                ed.pi[0] = i;
                                ed.pi[1] = j;
                                this.e.push_back(ed);
                                ni.oSet(ii, j);
                                ni.oSet(jj, i);
                                found = true;
                                break;
                            } else if ((vi.oGet(ii0) == vj.oGet(jj0)) && (vi.oGet(ii1) == vj.oGet(jj1))) {
                                System.err.printf("why am I here?\n");
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                }
            }
        }

        void recompute_planes() {
            // for each polygon
            for (int i = 0; i < this.p.size(); i++) {
                this.p.oGet(i).plane = compute_homogeneous_plane(
                        this.v.oGet(this.p.oGet(i).vi.oGet(0)),
                        this.v.oGet(this.p.oGet(i).vi.oGet(1)),
                        this.v.oGet(this.p.oGet(i).vi.oGet(2)));
            }
        }

        void transform(final idMat4 m) {
            for (int i = 0; i < this.v.size(); i++) {
                this.v.oSet(i, m.oMultiply(this.v.oGet(i)));
            }
            recompute_planes();
        }
    }
    private static polyhedron p;

// make a unit cube
    public static polyhedron PolyhedronFromBounds(final idBounds b) {

//       3----------2
//       |\        /|
//       | \      / |
//       |   7--6   |
//       |   |  |   |
//       |   4--5   |
//       |  /    \  |
//       | /      \ |
//       0----------1
//
        if (p.e.size() == 0) {

            p.v.push_back(new idVec4(-1, -1,  1, 1));
            p.v.push_back(new idVec4( 1, -1,  1, 1));
            p.v.push_back(new idVec4( 1,  1,  1, 1));
            p.v.push_back(new idVec4(-1,  1,  1, 1));
            p.v.push_back(new idVec4(-1, -1, -1, 1));
            p.v.push_back(new idVec4( 1, -1, -1, 1));
            p.v.push_back(new idVec4( 1,  1, -1, 1));
            p.v.push_back(new idVec4(-1,  1, -1, 1));

            p.add_quad(0, 1, 2, 3);
            p.add_quad(7, 6, 5, 4);
            p.add_quad(1, 0, 4, 5);
            p.add_quad(2, 1, 5, 6);
            p.add_quad(3, 2, 6, 7);
            p.add_quad(0, 3, 7, 4);

            p.compute_neighbors();
            p.recompute_planes();
            p.v.empty(); // no need to copy this data since it'll be replaced
        }

        final polyhedron p2 = new polyhedron(p);

        final idVec3 min = b.oGet(0);
        final idVec3 max = b.oGet(1);

        p2.v.empty();
        p2.v.push_back(new idVec4(min.x, min.y, max.z, 1));
        p2.v.push_back(new idVec4(max.x, min.y, max.z, 1));
        p2.v.push_back(new idVec4(max.x, max.y, max.z, 1));
        p2.v.push_back(new idVec4(min.x, max.y, max.z, 1));
        p2.v.push_back(new idVec4(min.x, min.y, min.z, 1));
        p2.v.push_back(new idVec4(max.x, min.y, min.z, 1));
        p2.v.push_back(new idVec4(max.x, max.y, min.z, 1));
        p2.v.push_back(new idVec4(min.x, max.y, min.z, 1));

        p2.recompute_planes();
        return p2;
    }
    private static final polyhedron[] lut = new polyhedron[64];

    public static polyhedron make_sv(final polyhedron oc, idVec4 light) {
        int index = 0;

        for (int i = 0; i < 6; i++) {
            if ((oc.p.oGet(i).plane.oMultiply(light)) > 0) {
                index |= 1 << i;
            }
        }

        if (lut[index].e.size() == 0) {
            final polyhedron ph = lut[index] = oc;

            final int V = ph.v.size();
            for (int j = 0; j < V; j++) {
                final idVec3 proj = homogeneous_difference(light, ph.v.oGet(j));
                ph.v.push_back(new idVec4(proj.x, proj.y, proj.z, 0));
            }

            ph.p.empty();

            for (int i = 0; i < oc.p.size(); i++) {
                if ((oc.p.oGet(i).plane.oMultiply(light)) > 0) {
                    ph.p.push_back(oc.p.oGet(i));
                }
            }

            if (ph.p.size() == 0) {
                return lut[index] = new polyhedron();
            }

            ph.compute_neighbors();

            final MyArrayPoly vpg = new MyArrayPoly();
            final int I = ph.p.size();

            for (int i = 0; i < I; i++) {
                final MyArrayInt vi = ph.p.oGet(i).vi;
                final MyArrayInt ni = ph.p.oGet(i).ni;
                final int S = vi.size();

                for (int j = 0; j < S; j++) {
                    if (ni.oGet(j) == -1) {
                        final poly pg = new poly();
                        final int a = vi.oGet((j + 1) % S);
                        final int b = vi.oGet(j);
                        pg.vi = four_ints(a, b, b + V, a + V);
                        pg.ni = four_ints(-1, -1, -1, -1);
                        vpg.push_back(pg);
                    }
                }
            }
            for (int i = 0; i < vpg.size(); i++) {
                ph.p.push_back(vpg.oGet(i));
            }

            ph.compute_neighbors();
            ph.v.empty(); // no need to copy this data since it'll be replaced
        }

        final polyhedron ph2 = lut[index];

        // initalize vertices
        ph2.v = oc.v;
        final int V = ph2.v.size();
        for (int j = 0; j < V; j++) {
            final idVec3 proj = homogeneous_difference(light, ph2.v.oGet(j));
            ph2.v.push_back(new idVec4(proj.x, proj.y, proj.z, 0));
        }

        // need to compute planes for the shadow volume (sv)
        ph2.recompute_planes();

        return ph2;
    }

    static class MySegments extends MyArray<idVec4> {

        public MySegments() {
            super(N);
        }
        private static final int N = 36;
    }
//int MySegments::max_size = 0;

    public static void polyhedron_edges(polyhedron a, MySegments e) {
        e.empty();
        if ((a.e.size() == 0) && (a.p.size() != 0)) {
            a.compute_neighbors();
        }

        for (int i = 0; i < a.e.size(); i++) {
            e.push_back(a.v.oGet(a.e.oGet(i).vi[0]));
            e.push_back(a.v.oGet(a.e.oGet(i).vi[1]));
        }

    }

// clip the segments of e by the planes of polyhedron a.
    public static void clip_segments(final polyhedron ph, MySegments is, MySegments os) {
        final MyArrayPoly p = ph.p;

        for (int i = 0; i < is.size(); i += 2) {
            idVec4 a = is.oGet(i);
            idVec4 b = is.oGet(i + 1);
            idVec4 c;

            boolean discard = false;

            for (int j = 0; j < p.size(); j++) {
                final float da = a.oMultiply(p.oGet(j).plane);
                final float db = b.oMultiply(p.oGet(j).plane);
                final float rdw = 1 / (da - db);

                int code = 0;
                if (da > 0) {
                    code = 2;
                }
                if (db > 0) {
                    code |= 1;
                }

                switch (code) {
                    case 3:
                        discard = true;
                        break;

                    case 2:
                        c = a.oMultiply(db * rdw).oPlus(b.oMultiply(da * rdw)).oNegative();
                        a = c;
                        break;

                    case 1:
                        c = a.oMultiply(db * rdw).oPlus(b.oMultiply(da * rdw)).oNegative();
                        b = c;
                        break;

                    case 0:
                        break;

                    default:
                        common.Printf("bad clip code!\n");
                        break;
                }

                if (discard) {
                    break;
                }
            }

            if (!discard) {
                os.push_back(a);
                os.push_back(b);
            }
        }

    }

    public static idMat4 make_idMat4(final float[] m) {
        return new idMat4(
                m[ 0], m[ 4], m[ 8], m[12],
                m[ 1], m[ 5], m[ 9], m[13],
                m[ 2], m[ 6], m[10], m[14],
                m[ 3], m[ 7], m[11], m[15]);
    }

    public static idVec3 v4to3(final idVec4 v) {
        return new idVec3(v.x / v.w, v.y / v.w, v.z / v.w);
    }

    public static void draw_polyhedron(final viewDef_s viewDef, final polyhedron p, idVec4 color) {
        for (int i = 0; i < p.e.size(); i++) {
            viewDef.renderWorld.DebugLine(color, v4to3(p.v.oGet(p.e.oGet(i).vi[0])), v4to3(p.v.oGet(p.e.oGet(i).vi[1])));
        }
    }

    public static void draw_segments(final viewDef_s viewDef, final MySegments s, idVec4 color) {
        for (int i = 0; i < s.size(); i += 2) {
            viewDef.renderWorld.DebugLine(color, v4to3(s.oGet(i)), v4to3(s.oGet(i + 1)));
        }
    }

    public static void world_to_hclip(final viewDef_s viewDef, final idVec4 global, idVec4 clip) {
        int i;
        final idVec4 view = new idVec4();

        for (i = 0; i < 4; i++) {
            view.oSet(i,
                    (global.oGet(0) * viewDef.worldSpace.modelViewMatrix[ i + (0 * 4)])
                    + (global.oGet(1) * viewDef.worldSpace.modelViewMatrix[ i + (1 * 4)])
                    + (global.oGet(2) * viewDef.worldSpace.modelViewMatrix[ i + (2 * 4)])
                    + (global.oGet(3) * viewDef.worldSpace.modelViewMatrix[ i + (3 * 4)]));
        }

        for (i = 0; i < 4; i++) {
            clip.oSet(i,
                    (view.oGet(0) * viewDef.projectionMatrix[ i + (0 * 4)])
                    + (view.oGet(1) * viewDef.projectionMatrix[ i + (1 * 4)])
                    + (view.oGet(2) * viewDef.projectionMatrix[ i + (2 * 4)])
                    + (view.oGet(3) * viewDef.projectionMatrix[ i + (3 * 4)]));
        }
    }

    public static idScreenRect R_CalcIntersectionScissor(final idRenderLightLocal lightDef, final idRenderEntityLocal entityDef, final viewDef_s viewDef) {

        final idMat4 omodel = make_idMat4(entityDef.modelMatrix);
        final idMat4 lmodel = make_idMat4(lightDef.modelMatrix);

        // compute light polyhedron
        final polyhedron lvol = PolyhedronFromBounds(lightDef.frustumTris.bounds);
        // transform it into world space
        //lvol.transform( lmodel );

        // debug //
        if (RenderSystem_init.r_useInteractionScissors.GetInteger() == -2) {
            draw_polyhedron(viewDef, lvol, colorRed);
        }

        // compute object polyhedron
        final polyhedron vol = PolyhedronFromBounds(entityDef.referenceBounds);

        //viewDef.renderWorld.DebugBounds( colorRed, lightDef.frustumTris.bounds );
        //viewDef.renderWorld.DebugBox( colorBlue, idBox( model.Bounds(), entityDef.parms.origin, entityDef.parms.axis ) );
        // transform it into world space
        vol.transform(omodel);

        // debug //
        if (RenderSystem_init.r_useInteractionScissors.GetInteger() == -2) {
            draw_polyhedron(viewDef, vol, colorBlue);
        }

        // transform light position into world space
        final idVec4 lightpos = new idVec4(
                lightDef.globalLightOrigin.x,
                lightDef.globalLightOrigin.y,
                lightDef.globalLightOrigin.z,
                1.0f);

        // generate shadow volume "polyhedron"
        final polyhedron sv = make_sv(vol, lightpos);

        final MySegments in_segs = new MySegments(), out_segs = new MySegments();

        // get shadow volume edges
        polyhedron_edges(sv, in_segs);
        // clip them against light bounds planes
        clip_segments(lvol, in_segs, out_segs);

        // get light bounds edges
        polyhedron_edges(lvol, in_segs);
        // clip them by the shadow volume
        clip_segments(sv, in_segs, out_segs);

        // debug // 
        if (RenderSystem_init.r_useInteractionScissors.GetInteger() == -2) {
            draw_segments(viewDef, out_segs, colorGreen);
        }

        final idBounds outbounds = new idBounds();
        outbounds.Clear();
        for (int i = 0; i < out_segs.size(); i++) {

            final idVec4 v = new idVec4();
            world_to_hclip(viewDef, out_segs.oGet(i), v);

            if (v.w <= 0.0f) {
                return lightDef.viewLight.scissorRect;
            }

            final idVec3 rv = new idVec3(v.x, v.y, v.z);
            rv.oDivSet(v.w);

            outbounds.AddPoint(rv);
        }

        // limit the bounds to avoid an inside out scissor rectangle due to floating point to short conversion
        if (outbounds.oGet(0).x < -1.0f) {
            outbounds.oGet(0).x = -1.0f;
        }
        if (outbounds.oGet(1).x > 1.0f) {
            outbounds.oGet(1).x = 1.0f;
        }
        if (outbounds.oGet(0).y < -1.0f) {
            outbounds.oGet(0).y = -1.0f;
        }
        if (outbounds.oGet(1).y > 1.0f) {
            outbounds.oGet(1).y = 1.0f;
        }

        final float w2 = ((viewDef.viewport.x2 - viewDef.viewport.x1) + 1) / 2.0f;
        final float x = viewDef.viewport.x1;
        final float h2 = ((viewDef.viewport.y2 - viewDef.viewport.y1) + 1) / 2.0f;
        final float y = viewDef.viewport.y1;

        final idScreenRect rect = new idScreenRect();
        rect.x1 = (int) ((outbounds.oGet(0).x * w2) + w2 + x);
        rect.x2 = (int) ((outbounds.oGet(1).x * w2) + w2 + x);
        rect.y1 = (int) ((outbounds.oGet(0).y * h2) + h2 + y);
        rect.y2 = (int) ((outbounds.oGet(1).y * h2) + h2 + y);
        rect.Expand();

        rect.Intersect(lightDef.viewLight.scissorRect);

        // debug //
        if ((RenderSystem_init.r_useInteractionScissors.GetInteger() == -2) && !rect.IsEmpty()) {
            viewDef.renderWorld.DebugScreenRect(colorYellow, rect, viewDef);
        }

        return rect;
    }
}
