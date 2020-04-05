package neo.Tools.Compilers.DMap;

import static neo.TempDump.NOT;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import static neo.Tools.Compilers.DMap.gldraw.Draw_ClearWindow;
import static neo.Tools.Compilers.DMap.tritjunction.CountGroupListTris;
import static neo.Tools.Compilers.DMap.tritjunction.FixAreaGroupsTjunctions;
import static neo.Tools.Compilers.DMap.tritjunction.FreeTJunctionHash;
import static neo.Tools.Compilers.DMap.tritools.AllocTri;
import static neo.Tools.Compilers.DMap.tritools.CountTriList;
import static neo.Tools.Compilers.DMap.tritools.FreeTri;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.tritools.PlaneForTri;
import static neo.framework.Common.common;
import static neo.idlib.math.Vector.DotProduct;
import static neo.idlib.math.Vector.VectorMA;
import static neo.idlib.math.Vector.VectorSubtract;
import static neo.opengl.QGL.qglBegin;
import static neo.opengl.QGL.qglBlendFunc;
import static neo.opengl.QGL.qglColor3f;
import static neo.opengl.QGL.qglDisable;
import static neo.opengl.QGL.qglEnable;
import static neo.opengl.QGL.qglEnd;
import static neo.opengl.QGL.qglFlush;
import static neo.opengl.QGL.qglPointSize;
import static neo.opengl.QGL.qglVertex3fv;
import static neo.opengl.QGLConstantsIfc.GL_BLEND;
import static neo.opengl.QGLConstantsIfc.GL_LINES;
import static neo.opengl.QGLConstantsIfc.GL_LINE_LOOP;
import static neo.opengl.QGLConstantsIfc.GL_ONE;
import static neo.opengl.QGLConstantsIfc.GL_POINTS;
import static neo.opengl.QGLConstantsIfc.GL_TRIANGLES;

import java.util.Arrays;

import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class optimize {
    // optimize.cpp -- trianlge mesh reoptimization
    //
    // the shadow volume optimizer call internal optimizer routines, normal triangles
    // will just be done by OptimizeEntity()

    static idBounds optBounds;
    //
    static final int MAX_OPT_VERTEXES = 0x10000;
    static int numOptVerts;
    static       optVertex_s[] optVerts      = new optVertex_s[MAX_OPT_VERTEXES];
    //
    static final int           MAX_OPT_EDGES = 0x40000;
    static int numOptEdges;
    static optEdge_s[] optEdges = new optEdge_s[MAX_OPT_EDGES];
    //
//static bool IsTriangleValid( const optVertex_s *v1, const optVertex_s *v2, const optVertex_s *v3 );
//static bool IsTriangleDegenerate( const optVertex_s *v1, const optVertex_s *v2, const optVertex_s *v3 );
//
    static idRandom orandom;
    //
    static final double COLINEAR_EPSILON = 0.1;

    static class optVertex_s {

        idDrawVert v;
        idVec3 pv;			// projected against planar axis, third value is 0
        optEdge_s edges;
        optVertex_s islandLink;
        boolean addedToIsland;
        boolean emited;			// when regenerating triangles
    }

    static class optEdge_s {

        optVertex_s v1, v2;
        optEdge_s islandLink;
        boolean addedToIsland;
        boolean created;		// not one of the original edges
        boolean combined;		// combined from two or more colinear edges
        optTri_s frontTri, backTri;
        optEdge_s v1link, v2link;
    }

    static class optTri_s {

        optTri_s next;
        idVec3 midpoint;
        optVertex_s[] v = new optVertex_s[3];
        boolean filled;
    }

    static class optIsland_t {

        optimizeGroup_s group;
        optVertex_s verts;
        optEdge_s edges;
        optTri_s tris;
    }

    static class edgeLength_t {

        optVertex_s v1, v2;
        float length;
    }

    static class originalEdges_t {

        optVertex_s v1, v2;
    }

    static class edgeCrossing_s {

        edgeCrossing_s next;
        optVertex_s ov;
    }

    /*

     New vertexes will be created where edges cross.

     optimization requires an accurate t junction fixer.



     */
    /*
     ==============
     ValidateEdgeCounts
     ==============
     */
    static void ValidateEdgeCounts(optIsland_t island) {
        optVertex_s vert;
        optEdge_s e;
        int c;

        for (vert = island.verts; vert != null; vert = vert.islandLink) {
            c = 0;
            for (e = vert.edges; e != null;) {
                c++;
                if (e.v1 == vert) {
                    e = e.v1link;
                } else if (e.v2 == vert) {
                    e = e.v2link;
                } else {
                    common.Error("ValidateEdgeCounts: mislinked");
                }
            }
            if ((c != 2) && (c != 0)) {
                // this can still happen at diamond intersections
//			common.Printf( "ValidateEdgeCounts: %i edges\n", c );
            }
        }
    }


    /*
     ====================
     AllocEdge
     ====================
     */
    static optEdge_s AllocEdge() {
        optEdge_s e;

        if (numOptEdges == MAX_OPT_EDGES) {
            common.Error("MAX_OPT_EDGES");
        }
        e = optEdges[numOptEdges] = new optEdge_s();
        numOptEdges++;
//	memset( e, 0, sizeof( *e ) );

        return e;
    }

    /*
     ====================
     RemoveEdgeFromVert
     ====================
     */
    static void RemoveEdgeFromVert(optEdge_s e1, optVertex_s vert) {
        optEdge_s prev;//TODO:double check these references
        optEdge_s e;

        if (NOT(vert)) {
            return;
        }
        prev = vert.edges;
        while (prev != null) {
            e = prev;
            if (e.equals(e1)) {
                if (e1.v1.equals(vert)) {
                    prev = e1.v1link;
                } else if (e1.v2.equals(vert)) {
                    prev = e1.v2link;
                } else {
                    common.Error("RemoveEdgeFromVert: vert not found");
                }
                return;
            }

            if (e.v1.equals(vert)) {
                prev = e.v1link;
            } else if (e.v2.equals(vert)) {
                prev = e.v2link;
            } else {
                common.Error("RemoveEdgeFromVert: vert not found");
            }
        }
        vert.edges = null;
    }

    /*
     ====================
     UnlinkEdge
     ====================
     */
    static void UnlinkEdge(optEdge_s e, optIsland_t island) {
        optEdge_s prev;

        RemoveEdgeFromVert(e, e.v1);
        RemoveEdgeFromVert(e, e.v2);

        for (prev = island.edges; prev != null; prev = prev.islandLink) {
            if (prev.equals(e)) {
                prev = island.edges = e.islandLink;
                return;
            }
        }

        common.Error("RemoveEdgeFromIsland: couldn't free edge");
    }


    /*
     ====================
     LinkEdge
     ====================
     */
    static void LinkEdge(optEdge_s e) {
        e.v1link = e.v1.edges;
        e.v1.edges = e;

        e.v2link = e.v2.edges;
        e.v2.edges = e;
    }

//#ifdef __linux__
//
//optVertex_s *FindOptVertex( idDrawVert *v, optimizeGroup_s *opt );
//
//#else

    /*
     ================
     FindOptVertex
     ================
     */
    static optVertex_s FindOptVertex(idDrawVert v, optimizeGroup_s opt) {
        int i;
        float x, y;
        optVertex_s vert;

        // deal with everything strictly as 2D
        x = v.xyz.oMultiply(opt.axis[0]);
        y = v.xyz.oMultiply(opt.axis[1]);

        // should we match based on the t-junction fixing hash verts?
        for (i = 0; i < numOptVerts; i++) {
            if ((optVerts[i].pv.oGet(0) == x) && (optVerts[i].pv.oGet(1) == y)) {
                return optVerts[i];
            }
        }

        if (numOptVerts >= MAX_OPT_VERTEXES) {
            common.Error("MAX_OPT_VERTEXES");
            return null;
        }

        numOptVerts++;

        vert = optVerts[i] = new optVertex_s();
//	memset( vert, 0, sizeof( *vert ) );
        vert.v = v;
        vert.pv.oSet(0, x);
        vert.pv.oSet(1, y);
        vert.pv.oSet(2, 0);

        optBounds.AddPoint(vert.pv);

        return vert;
    }

//#endif

    /*
     ================
     DrawAllEdges
     ================
     */
    static void DrawAllEdges() {
        int i;

        if (!dmapGlobals.drawflag) {
            return;
        }

        Draw_ClearWindow();

        qglBegin(GL_LINES);
        for (i = 0; i < numOptEdges; i++) {
            if (optEdges[i].v1 == null) {
                continue;
            }
            qglColor3f(1, 0, 0);
            qglVertex3fv(optEdges[i].v1.pv.ToFloatPtr());
            qglColor3f(0, 0, 0);
            qglVertex3fv(optEdges[i].v2.pv.ToFloatPtr());
        }
        qglEnd();
        qglFlush();

//	GLimp_SwapBuffers();
    }

    /*
     ================
     DrawVerts
     ================
     */
    static void DrawVerts(optIsland_t island) {
        optVertex_s vert;

        if (!dmapGlobals.drawflag) {
            return;
        }

        qglEnable(GL_BLEND);
        qglBlendFunc(GL_ONE, GL_ONE);
        qglColor3f(0.3f, 0.3f, 0.3f);
        qglPointSize(3);
        qglBegin(GL_POINTS);
        for (vert = island.verts; vert != null; vert = vert.islandLink) {
            qglVertex3fv(vert.pv.ToFloatPtr());
        }
        qglEnd();
        qglDisable(GL_BLEND);
        qglFlush();
    }

    /*
     ================
     DrawEdges
     ================
     */
    static void DrawEdges(optIsland_t island) {
        optEdge_s edge;

        if (!dmapGlobals.drawflag) {
            return;
        }

        Draw_ClearWindow();

        qglBegin(GL_LINES);
        for (edge = island.edges; edge != null; edge = edge.islandLink) {
            if (edge.v1 == null) {
                continue;
            }
            qglColor3f(1, 0, 0);
            qglVertex3fv(edge.v1.pv.ToFloatPtr());
            qglColor3f(0, 0, 0);
            qglVertex3fv(edge.v2.pv.ToFloatPtr());
        }
        qglEnd();
        qglFlush();

//	GLimp_SwapBuffers();
    }

//=================================================================

    /*
     =================
     VertexBetween
     =================
     */
    static boolean VertexBetween(final optVertex_s p1, final optVertex_s v1, final optVertex_s v2) {
        idVec3 d1, d2;
        float d;

        d1 = p1.pv.oMinus(v1.pv);
        d2 = p1.pv.oMinus(v2.pv);
        d = d1.oMultiply(d2);
        if (d < 0) {
            return true;
        }
        return false;
    }


    /*
     ====================
     EdgeIntersection

     Creates a new optVertex_s where the line segments cross.
     This should only be called if PointsStraddleLine returned true

     Will return NULL if the lines are colinear
     ====================
     */
    static optVertex_s EdgeIntersection(final optVertex_s p1, final optVertex_s p2,
            final optVertex_s l1, final optVertex_s l2, optimizeGroup_s opt) {
        float f;
        idDrawVert v;
        idVec3 dir1, dir2, cross1, cross2;

        dir1 = p1.pv.oMinus(l1.pv);
        dir2 = p1.pv.oMinus(l2.pv);
        cross1 = dir1.Cross(dir2);

        dir1 = p2.pv.oMinus(l1.pv);
        dir2 = p2.pv.oMinus(l2.pv);
        cross2 = dir1.Cross(dir2);

        if ((cross1.oGet(2) - cross2.oGet(2)) == 0) {
            return null;
        }

        f = cross1.oGet(2) / (cross1.oGet(2) - cross2.oGet(2));

        // FIXME: how are we freeing this, since it doesn't belong to a tri?
        v = new idDrawVert();// Mem_Alloc(sizeof(v));
//	memset( v, 0, sizeof( *v ) );

        v.xyz = p1.v.xyz.oMultiply(1.0f - f).oPlus(p2.v.xyz.oMultiply(f));
        v.normal = p1.v.normal.oMultiply(1.0f - f).oPlus(p2.v.normal.oMultiply(f));
        v.normal.Normalize();
        v.st.oSet(0, (p1.v.st.oGet(0) * (1.0f - f)) + (p2.v.st.oGet(0) * f));
        v.st.oSet(1, (p1.v.st.oGet(1) * (1.0f - f)) + (p2.v.st.oGet(1) * f));

        return FindOptVertex(v, opt);
    }


    /*
     ====================
     PointsStraddleLine

     Colinear is considdered crossing.
     ====================
     */
    static boolean PointsStraddleLine(optVertex_s p1, optVertex_s p2, optVertex_s l1, optVertex_s l2) {
        boolean t1, t2;

        t1 = IsTriangleDegenerate(l1, l2, p1);
        t2 = IsTriangleDegenerate(l1, l2, p2);
        if (t1 && t2) {
            // colinear case
            float s1, s2, s3, s4;
            boolean positive, negative;

            s1 = (p1.pv.oMinus(l1.pv)).oMultiply(l2.pv.oMinus(l1.pv));
            s2 = (p2.pv.oMinus(l1.pv)).oMultiply(l2.pv.oMinus(l1.pv));
            s3 = (p1.pv.oMinus(l2.pv)).oMultiply(l2.pv.oMinus(l1.pv));
            s4 = (p2.pv.oMinus(l2.pv)).oMultiply(l2.pv.oMinus(l1.pv));

            if ((s1 > 0) || (s2 > 0) || (s3 > 0) || (s4 > 0)) {
                positive = true;
            } else {
                positive = false;
            }
            if ((s1 < 0) || (s2 < 0) || (s3 < 0) || (s4 < 0)) {
                negative = true;
            } else {
                negative = false;
            }

            if (positive && negative) {
                return true;
            }
            return false;
        } else if (!p1.equals(l1) && !p1.equals(l2) && !p2.equals(l1) && !p2.equals(l2)) {
            // no shared verts
            t1 = IsTriangleValid(l1, l2, p1);
            t2 = IsTriangleValid(l1, l2, p2);
            if (t1 && t2) {
                return false;
            }

            t1 = IsTriangleValid(l1, p1, l2);
            t2 = IsTriangleValid(l1, p2, l2);
            if (t1 && t2) {
                return false;
            }

            return true;
        } else {
            // a shared vert, not colinear, so not crossing
            return false;
        }
    }


    /*
     ====================
     EdgesCross
     ====================
     */
    static boolean EdgesCross(optVertex_s a1, optVertex_s a2, optVertex_s b1, optVertex_s b2) {
        // if both verts match, consider it to be crossed
        if (a1.equals(b1) && a2.equals(b2)) {
            return true;
        }
        if (a1.equals(b2) && a2.equals(b1)) {
            return true;
        }
        // if only one vert matches, it might still be colinear, which
        // would be considered crossing

        // if both lines' verts are on opposite sides of the other
        // line, it is crossed
        if (!PointsStraddleLine(a1, a2, b1, b2)) {
            return false;
        }
        if (!PointsStraddleLine(b1, b2, a1, a2)) {
            return false;
        }

        return true;
    }

    /*
     ====================
     TryAddNewEdge

     ====================
     */
    static boolean TryAddNewEdge(optVertex_s v1, optVertex_s v2, optIsland_t island) {
        optEdge_s e;

        // if the new edge crosses any other edges, don't add it
        for (e = island.edges; e != null; e = e.islandLink) {
            if (EdgesCross(e.v1, e.v2, v1, v2)) {
                return false;
            }
        }

        if (dmapGlobals.drawflag) {
            qglBegin(GL_LINES);
            qglColor3f(0, (128 + orandom.RandomInt(127)) / 255.0f, 0);
            qglVertex3fv(v1.pv.ToFloatPtr());
            qglVertex3fv(v2.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
        }
        // add it
        e = AllocEdge();

        e.islandLink = island.edges;
        island.edges = e;
        e.v1 = v1;
        e.v2 = v2;

        e.created = true;

        // link the edge to its verts
        LinkEdge(e);

        return true;
    }

    static class LengthSort implements cmp_t<edgeLength_t> {

        @Override
        public int compare(edgeLength_t a, edgeLength_t b) {

            if (a.length < b.length) {
                return -1;
            }
            if (a.length > b.length) {
                return 1;
            }
            return 0;
        }
    }

    /*
     ==================
     AddInteriorEdges

     Add all possible edges between the verts
     ==================
     */
    static void AddInteriorEdges(optIsland_t island) {
        int c_addedEdges;
        optVertex_s vert, vert2;
        int c_verts;
        edgeLength_t[] lengths;
        int numLengths;
        int i;

        DrawVerts(island);

        // count the verts
        c_verts = 0;
        for (vert = island.verts; vert != null; vert = vert.islandLink) {
            if (NOT(vert.edges)) {
                continue;
            }
            c_verts++;
        }

        // allocate space for all the lengths
        lengths = new edgeLength_t[(c_verts * c_verts) / 2];// Mem_Alloc(c_verts * c_verts / 2);
        numLengths = 0;
        for (vert = island.verts; vert != null; vert = vert.islandLink) {
            if (NOT(vert.edges)) {
                continue;
            }
            for (vert2 = vert.islandLink; vert2 != null; vert2 = vert2.islandLink) {
                idVec3 dir;

                if (NOT(vert2.edges)) {
                    continue;
                }
                lengths[numLengths].v1 = vert;
                lengths[numLengths].v2 = vert2;
                dir = (vert.pv.oMinus(vert2.pv));
                lengths[numLengths].length = dir.Length();
                numLengths++;
            }
        }

        // sort by length, shortest first
//        qsort(lengths, numLengths, sizeof(lengths[0]), LengthSort);
        Arrays.sort(lengths, 0, numLengths, new LengthSort());

        // try to create them in that order
        c_addedEdges = 0;
        for (i = 0; i < numLengths; i++) {
            if (TryAddNewEdge(lengths[i].v1, lengths[i].v2, island)) {
                c_addedEdges++;
            }
        }

        if (dmapGlobals.verbose) {
            common.Printf("%6d tested segments\n", numLengths);
            common.Printf("%6d added interior edges\n", c_addedEdges);
        }

        lengths = null;//Mem_Free(lengths);
    }

//==================================================================

    /*
     ====================
     RemoveIfColinear

     ====================
     */
    static void RemoveIfColinear(optVertex_s ov, optIsland_t island) {
        optEdge_s e, e1, e2;
        optVertex_s v1 = new optVertex_s(), v2, v3 = new optVertex_s();
        final idVec3 dir1 = new idVec3(), dir2 = new idVec3();
        float len, dist;
        final idVec3 point = new idVec3();
        final idVec3 offset = new idVec3();
        float off;

        v2 = ov;

        // we must find exactly two edges before testing for colinear
        e1 = null;
        e2 = null;
        for (e = ov.edges; e != null;) {
            if (NOT(e1)) {
                e1 = e;
            } else if (NOT(e2)) {
                e2 = e;
            } else {
                return;		// can't remove a vertex with three edges
            }
            if (e.v1 == v2) {
                e = e.v1link;
            } else if (e.v2 == v2) {
                e = e.v2link;
            } else {
                common.Error("RemoveIfColinear: mislinked edge");
            }
        }

        // can't remove if no edges
        if (NOT(e1)) {
            return;
        }

        if (NOT(e2)) {
            // this may still happen legally when a tiny triangle is
            // the only thing in a group
            common.Printf("WARNING: vertex with only one edge\n");
            return;
        }

        if (e1.v1.equals(v2)) {
            v1 = e1.v2;
        } else if (e1.v2.equals(v2)) {
            v1 = e1.v1;
        } else {
            common.Error("RemoveIfColinear: mislinked edge");
        }
        if (e2.v1.equals(v2)) {
            v3 = e2.v2;
        } else if (e2.v2.equals(v2)) {
            v3 = e2.v1;
        } else {
            common.Error("RemoveIfColinear: mislinked edge");
        }

        if (v1.equals(v3)) {
            common.Error("RemoveIfColinear: mislinked edge");
        }

        // they must point in opposite directions
        dist = (v3.pv.oMinus(v2.pv)).oMultiply(v1.pv.oMinus(v2.pv));
        if (dist >= 0) {
            return;
        }

        // see if they are colinear
        VectorSubtract(v3.v.xyz, v1.v.xyz, dir1);
        len = dir1.Normalize();
        VectorSubtract(v2.v.xyz, v1.v.xyz, dir2);
        dist = DotProduct(dir2, dir1);
        VectorMA(v1.v.xyz, dist, dir1, point);
        VectorSubtract(point, v2.v.xyz, offset);
        off = offset.Length();

        if (off > COLINEAR_EPSILON) {
            return;
        }

        if (dmapGlobals.drawflag) {
            qglBegin(GL_LINES);
            qglColor3f(1, 1, 0);
            qglVertex3fv(v1.pv.ToFloatPtr());
            qglVertex3fv(v2.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
            qglBegin(GL_LINES);
            qglColor3f(0, 1, 1);
            qglVertex3fv(v2.pv.ToFloatPtr());
            qglVertex3fv(v3.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
        }

        // replace the two edges with a single edge
        UnlinkEdge(e1, island);
        UnlinkEdge(e2, island);

        // v2 should have no edges now
        if (v2.edges != null) {
            common.Error("RemoveIfColinear: didn't remove properly");
        }

        // if there is an existing edge that already
        // has these exact verts, we have just collapsed a
        // sliver triangle out of existance, and all the edges
        // can be removed
        for (e = island.edges; e != null; e = e.islandLink) {
            if (((e.v1 == v1) && (e.v2 == v3))
                    || ((e.v1 == v3) && (e.v2 == v1))) {
                UnlinkEdge(e, island);
                RemoveIfColinear(v1, island);
                RemoveIfColinear(v3, island);
                return;
            }
        }

        // if we can't add the combined edge, link
        // the originals back in
        if (!TryAddNewEdge(v1, v3, island)) {
            e1.islandLink = island.edges;
            island.edges = e1;
            LinkEdge(e1);

            e2.islandLink = island.edges;
            island.edges = e2;
            LinkEdge(e2);
            return;
        }

        // recursively try to combine both verts now,
        // because things may have changed since the last combine test
        RemoveIfColinear(v1, island);
        RemoveIfColinear(v3, island);
    }

    /*
     ====================
     CombineColinearEdges
     ====================
     */
    static void CombineColinearEdges(optIsland_t island) {
        int c_edges;
        optVertex_s ov;
        optEdge_s e;

        c_edges = 0;
        for (e = island.edges; e != null; e = e.islandLink) {
            c_edges++;
        }
        if (dmapGlobals.verbose) {
            common.Printf("%6d original exterior edges\n", c_edges);
        }

        for (ov = island.verts; ov != null; ov = ov.islandLink) {
            RemoveIfColinear(ov, island);
        }

        c_edges = 0;
        for (e = island.edges; e != null; e = e.islandLink) {
            c_edges++;
        }
        if (dmapGlobals.verbose) {
            common.Printf("%6d optimized exterior edges\n", c_edges);
        }
    }

//==================================================================

    /*
     ===================
     FreeOptTriangles

     ===================
     */
    static void FreeOptTriangles(optIsland_t island) {
        optTri_s opt, next;

        for (opt = island.tris; opt != null; opt = next) {
            next = opt.next;
            opt = null;//Mem_Free(opt);
        }

        island.tris = null;
    }


    /*
     =================
     IsTriangleValid

     empty area will be considered invalid.
     Due to some truly aweful epsilon issues, a triangle can switch between
     valid and invalid depending on which order you look at the verts, so
     consider it invalid if any one of the possibilities is invalid.
     =================
     */
    static boolean IsTriangleValid(final optVertex_s v1, final optVertex_s v2, final optVertex_s v3) {
        idVec3 d1, d2, normal;

        d1 = v2.pv.oMinus(v1.pv);
        d2 = v3.pv.oMinus(v1.pv);
        normal = d1.Cross(d2);
        if (normal.oGet(2) <= 0) {
            return false;
        }

        d1 = v3.pv.oMinus(v2.pv);
        d2 = v1.pv.oMinus(v2.pv);
        normal = d1.Cross(d2);
        if (normal.oGet(2) <= 0) {
            return false;
        }

        d1 = v1.pv.oMinus(v3.pv);
        d2 = v2.pv.oMinus(v3.pv);
        normal = d1.Cross(d2);
        if (normal.oGet(2) <= 0) {
            return false;
        }

        return true;
    }


    /*
     =================
     IsTriangleDegenerate

     Returns false if it is either front or back facing
     =================
     */
    static boolean IsTriangleDegenerate(final optVertex_s v1, final optVertex_s v2, final optVertex_s v3) {
//#if 1
        idVec3 d1, d2, normal;

        d1 = v2.pv.oMinus(v1.pv);
        d2 = v3.pv.oMinus(v1.pv);
        normal = d1.Cross(d2);
        if (normal.oGet(2) == 0) {
            return true;
        }
        return false;
//#else
//	return (bool)!IsTriangleValid( v1, v2, v3 );
//#endif
    }


    /*
     ==================
     PointInTri

     Tests if a 2D point is inside an original triangle
     ==================
     */
    static boolean PointInTri(final idVec3 p, final mapTri_s tri, optIsland_t island) {
        idVec3 d1, d2, normal;

        // the normal[2] == 0 case is not uncommon when a square is triangulated in
        // the opposite manner to the original
        d1 = tri.optVert[0].pv.oMinus(p);
        d2 = tri.optVert[1].pv.oMinus(p);
        normal = d1.Cross(d2);
        if (normal.oGet(2) < 0) {
            return false;
        }

        d1 = tri.optVert[1].pv.oMinus(p);
        d2 = tri.optVert[2].pv.oMinus(p);
        normal = d1.Cross(d2);
        if (normal.oGet(2) < 0) {
            return false;
        }

        d1 = tri.optVert[2].pv.oMinus(p);
        d2 = tri.optVert[0].pv.oMinus(p);
        normal = d1.Cross(d2);
        if (normal.oGet(2) < 0) {
            return false;
        }

        return true;
    }


    /*
     ====================
     LinkTriToEdge

     ====================
     */
    static void LinkTriToEdge(optTri_s optTri, optEdge_s edge) {
        if ((edge.v1.equals(optTri.v[0]) && edge.v2.equals(optTri.v[1]))
                || (edge.v1.equals(optTri.v[1]) && edge.v2.equals(optTri.v[2]))
                || (edge.v1.equals(optTri.v[2]) && edge.v2.equals(optTri.v[0]))) {
            if (edge.backTri != null) {
                common.Printf("Warning: LinkTriToEdge: already in use\n");
                return;
            }
            edge.backTri = optTri;
            return;
        }
        if ((edge.v1.equals(optTri.v[1]) && edge.v2.equals(optTri.v[0]))
                || (edge.v1.equals(optTri.v[2]) && edge.v2.equals(optTri.v[1]))
                || (edge.v1.equals(optTri.v[0]) && edge.v2.equals(optTri.v[2]))) {
            if (edge.frontTri != null) {
                common.Printf("Warning: LinkTriToEdge: already in use\n");
                return;
            }
            edge.frontTri = optTri;
            return;
        }
        common.Error("LinkTriToEdge: edge not found on tri");
    }

    /*
     ===============
     CreateOptTri
     ===============
     */
    static void CreateOptTri(optVertex_s first, optEdge_s e1, optEdge_s e2, optIsland_t island) {
        optEdge_s opposite;
        optVertex_s second = new optVertex_s(), third = new optVertex_s();
        optTri_s optTri;
        mapTri_s tri;

        if (e1.v1.equals(first)) {
            second = e1.v2;
        } else if (e1.v2.equals(first)) {
            second = e1.v1;
        } else {
            common.Error("CreateOptTri: mislinked edge");
        }

        if (e2.v1.equals(first)) {
            third = e2.v2;
        } else if (e2.v2.equals(first)) {
            third = e2.v1;
        } else {
            common.Error("CreateOptTri: mislinked edge");
        }

        if (!IsTriangleValid(first, second, third)) {
            common.Error("CreateOptTri: invalid");
        }

//DrawEdges( island );
        // identify the third edge
        if (dmapGlobals.drawflag) {
            qglColor3f(1, 1, 0);
            qglBegin(GL_LINES);
            qglVertex3fv(e1.v1.pv.ToFloatPtr());
            qglVertex3fv(e1.v2.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
            qglColor3f(0, 1, 1);
            qglBegin(GL_LINES);
            qglVertex3fv(e2.v1.pv.ToFloatPtr());
            qglVertex3fv(e2.v2.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
        }

        for (opposite = second.edges; opposite != null;) {
            if (!opposite.equals(e1) && (opposite.v1.equals(third) || opposite.v2.equals(third))) {
                break;
            }
            if (opposite.v1.equals(second)) {
                opposite = opposite.v1link;
            } else if (opposite.v2.equals(second)) {
                opposite = opposite.v2link;
            } else {
                common.Error("BuildOptTriangles: mislinked edge");
            }
        }

        if (NOT(opposite)) {
            common.Printf("Warning: BuildOptTriangles: couldn't locate opposite\n");
            return;
        }

        if (dmapGlobals.drawflag) {
            qglColor3f(1, 0, 1);
            qglBegin(GL_LINES);
            qglVertex3fv(opposite.v1.pv.ToFloatPtr());
            qglVertex3fv(opposite.v2.pv.ToFloatPtr());
            qglEnd();
            qglFlush();
        }

        // create new triangle
        optTri = new optTri_s();// Mem_Alloc(sizeof(optTri));
        optTri.v[0] = first;
        optTri.v[1] = second;
        optTri.v[2] = third;
        optTri.midpoint = (optTri.v[0].pv.oPlus(optTri.v[1].pv.oPlus(optTri.v[2].pv))).oMultiply(1.0f / 3.0f);
        optTri.next = island.tris;
        island.tris = optTri;

        if (dmapGlobals.drawflag) {
            qglColor3f(1, 1, 1);
            qglPointSize(4);
            qglBegin(GL_POINTS);
            qglVertex3fv(optTri.midpoint.ToFloatPtr());
            qglEnd();
            qglFlush();
        }

        // find the midpoint, and scan through all the original triangles to
        // see if it is inside any of them
        for (tri = island.group.triList; tri != null; tri = tri.next) {
            if (PointInTri(optTri.midpoint, tri, island)) {
                break;
            }
        }
        if (tri != null) {
            optTri.filled = true;
        } else {
            optTri.filled = false;
        }
        if (dmapGlobals.drawflag) {
            if (optTri.filled) {
                qglColor3f((128 + orandom.RandomInt(127)) / 255.0f, 0, 0);
            } else {
                qglColor3f(0, (128 + orandom.RandomInt(127)) / 255.0f, 0);
            }
            qglBegin(GL_TRIANGLES);
            qglVertex3fv(optTri.v[0].pv.ToFloatPtr());
            qglVertex3fv(optTri.v[1].pv.ToFloatPtr());
            qglVertex3fv(optTri.v[2].pv.ToFloatPtr());
            qglEnd();
            qglColor3f(1, 1, 1);
            qglBegin(GL_LINE_LOOP);
            qglVertex3fv(optTri.v[0].pv.ToFloatPtr());
            qglVertex3fv(optTri.v[1].pv.ToFloatPtr());
            qglVertex3fv(optTri.v[2].pv.ToFloatPtr());
            qglEnd();
            qglFlush();
        }

        // link the triangle to it's edges
        LinkTriToEdge(optTri, e1);
        LinkTriToEdge(optTri, e2);
        LinkTriToEdge(optTri, opposite);
    }

// debugging tool
    static void ReportNearbyVertexes(final optVertex_s v, final optIsland_t island) {
        optVertex_s ov;
        float d;
        idVec3 vec;

        common.Printf("verts near 0x%p (%f, %f)\n", v, v.pv.oGet(0), v.pv.oGet(1));
        for (ov = island.verts; ov != null; ov = ov.islandLink) {
            if (ov == v) {
                continue;
            }

            vec = ov.pv.oMinus(v.pv);

            d = vec.Length();
            if (d < 1) {
                common.Printf("0x%p = (%f, %f)\n", ov, ov.pv.oGet(0), ov.pv.oGet(1));
            }
        }
    }

    /*
     ====================
     BuildOptTriangles

     Generate a new list of triangles from the optEdeges
     ====================
     */
    static void BuildOptTriangles(optIsland_t island) {
        optVertex_s ov, second = new optVertex_s(), third = new optVertex_s(), middle = new optVertex_s();
        optEdge_s e1, e1Next = new optEdge_s(), e2, e2Next = new optEdge_s(), check, checkNext = new optEdge_s();

        // free them
        FreeOptTriangles(island);

        // clear the vertex emitted flags
        for (ov = island.verts; ov != null; ov = ov.islandLink) {
            ov.emited = false;
        }

        // clear the edge triangle links
        for (check = island.edges; check != null; check = check.islandLink) {
            check.frontTri = check.backTri = null;
        }

        // check all possible triangle made up out of the
        // edges coming off the vertex
        for (ov = island.verts; ov != null; ov = ov.islandLink) {
            if (NOT(ov.edges)) {
                continue;
            }

//#if 0
//if ( dmapGlobals.drawflag && ov == (optVertex_s *)0x1845a60 ) {
//for ( e1 = ov.edges ; e1 ; e1 = e1Next ) {
//	qglBegin( GL_LINES );
//	qglColor3f( 0,1,0 );
//	qglVertex3fv( e1.v1.pv.ToFloatPtr() );
//	qglVertex3fv( e1.v2.pv.ToFloatPtr() );
//	qglEnd();
//	qglFlush();
//	if ( e1.v1 == ov ) {
//		e1Next = e1.v1link;
//	} else if ( e1.v2 == ov ) {
//		e1Next = e1.v2link;
//	}
//}
//}
//#endif
            for (e1 = ov.edges; e1 != null; e1 = e1Next) {
                if (e1.v1.equals(ov)) {
                    second = e1.v2;
                    e1Next = e1.v1link;
                } else if (e1.v2.equals(ov)) {
                    second = e1.v1;
                    e1Next = e1.v2link;
                } else {
                    common.Error("BuildOptTriangles: mislinked edge");
                }

                // if the vertex has already been used, it can't be used again
                if (second.emited) {
                    continue;
                }

                for (e2 = ov.edges; e2 != null; e2 = e2Next) {
                    if (e2.v1.equals(ov)) {
                        third = e2.v2;
                        e2Next = e2.v1link;
                    } else if (e2.v2.equals(ov)) {
                        third = e2.v1;
                        e2Next = e2.v2link;
                    } else {
                        common.Error("BuildOptTriangles: mislinked edge");
                    }
                    if (e2.equals(e1)) {
                        continue;
                    }

                    // if the vertex has already been used, it can't be used again
                    if (third.emited) {
                        continue;
                    }

                    // if the triangle is backwards or degenerate, don't use it
                    if (!IsTriangleValid(ov, second, third)) {
                        continue;
                    }

                    // see if any other edge bisects these two, which means
                    // this triangle shouldn't be used
                    for (check = ov.edges; check != null; check = checkNext) {
                        if (check.v1.equals(ov)) {
                            middle = check.v2;
                            checkNext = check.v1link;
                        } else if (check.v2.equals(ov)) {
                            middle = check.v1;
                            checkNext = check.v2link;
                        } else {
                            common.Error("BuildOptTriangles: mislinked edge");
                        }

                        if (check.equals(e1) || check.equals(e2)) {
                            continue;
                        }

                        if (IsTriangleValid(ov, second, middle)
                                && IsTriangleValid(ov, middle, third)) {
                            break;	// should use the subdivided ones
                        }
                    }

                    if (check != null) {
                        continue;	// don't use it
                    }

                    // the triangle is valid
                    CreateOptTri(ov, e1, e2, island);
                }
            }

            // later vertexes will not emit triangles that use an
            // edge that this vert has already used
            ov.emited = true;
        }
    }

    /*
     ====================
     RegenerateTriangles

     Add new triangles to the group's regeneratedTris
     ====================
     */
    static void RegenerateTriangles(optIsland_t island) {
        optTri_s optTri;
        mapTri_s tri;
        int c_out;

        c_out = 0;

        for (optTri = island.tris; optTri != null; optTri = optTri.next) {
            if (!optTri.filled) {
                continue;
            }

            // create a new mapTri_s
            tri = AllocTri();

            tri.material = island.group.material;
            tri.mergeGroup = island.group.mergeGroup;

            tri.v[0] = optTri.v[0].v;
            tri.v[1] = optTri.v[1].v;
            tri.v[2] = optTri.v[2].v;

            final idPlane plane = new idPlane();
            PlaneForTri(tri, plane);
            if (plane.Normal().oMultiply(dmapGlobals.mapPlanes.oGet(island.group.planeNum).Normal()) <= 0) {
                // this can happen reasonably when a triangle is nearly degenerate in
                // optimization planar space, and winds up being degenerate in 3D space
                common.Printf("WARNING: backwards triangle generated!\n");
                // discard it
                FreeTri(tri);
                continue;
            }

            c_out++;
            tri.next = island.group.regeneratedTris;
            island.group.regeneratedTris = tri;
        }

        FreeOptTriangles(island);

        if (dmapGlobals.verbose) {
            common.Printf("%6d tris out\n", c_out);
        }
    }

//===========================================================================

    /*
     ====================
     RemoveInteriorEdges

     Edges that have triangles of the same type (filled / empty)
     on both sides will be removed
     ====================
     */
    static void RemoveInteriorEdges(optIsland_t island) {
        int c_interiorEdges;
        int c_exteriorEdges;
        optEdge_s e, next;
        boolean front, back;

        c_exteriorEdges = 0;
        c_interiorEdges = 0;
        for (e = island.edges; e != null; e = next) {
            // we might remove the edge, so get the next link now
            next = e.islandLink;

            if (NOT(e.frontTri)) {
                front = false;
            } else {
                front = e.frontTri.filled;
            }
            if (NOT(e.backTri)) {
                back = false;
            } else {
                back = e.backTri.filled;
            }

            if (front == back) {
                // free the edge
                UnlinkEdge(e, island);
                c_interiorEdges++;
                continue;
            }

            c_exteriorEdges++;
        }

        if (dmapGlobals.verbose) {
            common.Printf("%6d original interior edges\n", c_interiorEdges);
            common.Printf("%6d original exterior edges\n", c_exteriorEdges);
        }
    }

//==================================================================================

    /*
     =================
     AddEdgeIfNotAlready
     =================
     */
    static void AddEdgeIfNotAlready(optVertex_s v1, optVertex_s v2) {
        optEdge_s e;

        // make sure that there isn't an identical edge already added
        for (e = v1.edges; e != null;) {
            if ((e.v1.equals(v1) && e.v2.equals(v2)) || (e.v1.equals(v2) && e.v2.equals(v1))) {
                return;		// already added
            }
            if (e.v1.equals(v1)) {
                e = e.v1link;
            } else if (e.v2.equals(v1)) {
                e = e.v2link;
            } else {
                common.Error("SplitEdgeByList: bad edge link");
            }
        }

        // this edge is a keeper
        e = AllocEdge();
        e.v1 = v1;
        e.v2 = v2;

        e.islandLink = null;

        // link the edge to its verts
        LinkEdge(e);
    }

    /*
     =================
     DrawOriginalEdges
     =================
     */
    static void DrawOriginalEdges(int numOriginalEdges, originalEdges_t[] originalEdges) {
        int i;

        if (!dmapGlobals.drawflag) {
            return;
        }
        Draw_ClearWindow();

        qglBegin(GL_LINES);
        for (i = 0; i < numOriginalEdges; i++) {
            qglColor3f(1, 0, 0);
            qglVertex3fv(originalEdges[i].v1.pv.ToFloatPtr());
            qglColor3f(0, 0, 0);
            qglVertex3fv(originalEdges[i].v2.pv.ToFloatPtr());
        }
        qglEnd();
        qglFlush();
    }
    static originalEdges_t[] originalEdges;
    static int numOriginalEdges;

    /*
     =================
     AddOriginalTriangle
     =================
     */
    static void AddOriginalTriangle(optVertex_s[] v/*[3]*/) {
        optVertex_s v1, v2;

        // if this triangle is backwards (possible with epsilon issues)
        // ignore it completely
        if (!IsTriangleValid(v[0], v[1], v[2])) {
            common.Printf("WARNING: backwards triangle in input!\n");
            return;
        }

        for (int i = 0; i < 3; i++) {
            v1 = v[i];
            v2 = v[(i + 1) % 3];

            if (v1 == v2) {
                // this probably shouldn't happen, because the
                // tri would be degenerate
                continue;
            }
            int j;
            // see if there is an existing one
            for (j = 0; j < numOriginalEdges; j++) {
                if ((originalEdges[j].v1 == v1) && (originalEdges[j].v2 == v2)) {
                    break;
                }
                if ((originalEdges[j].v2 == v1) && (originalEdges[j].v1 == v2)) {
                    break;
                }
            }

            if (j == numOriginalEdges) {
                // add it
                originalEdges[j].v1 = v1;
                originalEdges[j].v2 = v2;
                numOriginalEdges++;
            }
        }
    }

    /*
     =================
     AddOriginalEdges
     =================
     */
    static void AddOriginalEdges(optimizeGroup_s opt) {
        mapTri_s tri;
        final optVertex_s[] v = new optVertex_s[3];
        int numTris;

        if (dmapGlobals.verbose) {
            common.Printf("----\n");
            common.Printf("%6d original tris\n", CountTriList(opt.triList));
        }

        optBounds.Clear();

        // allocate space for max possible edges
        numTris = CountTriList(opt.triList);
        originalEdges = new originalEdges_t[numTris * 3];// Mem_Alloc(numTris * 3);
        numOriginalEdges = 0;

        // add all unique triangle edges
        numOptVerts = 0;
        numOptEdges = 0;
        for (tri = opt.triList; tri != null; tri = tri.next) {
            v[0] = tri.optVert[0] = FindOptVertex(tri.v[0], opt);
            v[1] = tri.optVert[1] = FindOptVertex(tri.v[1], opt);
            v[2] = tri.optVert[2] = FindOptVertex(tri.v[2], opt);

            AddOriginalTriangle(v);
        }
    }

    /*
     =====================
     SplitOriginalEdgesAtCrossings
     =====================
     */
    static void SplitOriginalEdgesAtCrossings(optimizeGroup_s opt) {
        int i, j, k, l;
        int numOriginalVerts;
        edgeCrossing_s[] crossings;

        numOriginalVerts = numOptVerts;
        // now split any crossing edges and create optEdges
        // linked to the vertexes

        // debug drawing bounds
        dmapGlobals.drawBounds = optBounds;

        dmapGlobals.drawBounds.oGet(0).oMinSet(0, -2);
        dmapGlobals.drawBounds.oGet(0).oMinSet(1, -2);
        dmapGlobals.drawBounds.oGet(1).oPluSet(0, -2);
        dmapGlobals.drawBounds.oGet(1).oPluSet(1, -2);

        // generate crossing points between all the original edges
        crossings = new edgeCrossing_s[numOriginalEdges];// Mem_ClearedAlloc(numOriginalEdges);

        for (i = 0; i < numOriginalEdges; i++) {
            if (dmapGlobals.drawflag) {
                DrawOriginalEdges(numOriginalEdges, originalEdges);
                qglBegin(GL_LINES);
                qglColor3f(0, 1, 0);
                qglVertex3fv(originalEdges[i].v1.pv.ToFloatPtr());
                qglColor3f(0, 0, 1);
                qglVertex3fv(originalEdges[i].v2.pv.ToFloatPtr());
                qglEnd();
                qglFlush();
            }
            for (j = i + 1; j < numOriginalEdges; j++) {
                optVertex_s v1, v2, v3, v4;
                optVertex_s newVert;
                edgeCrossing_s cross;

                v1 = originalEdges[i].v1;
                v2 = originalEdges[i].v2;
                v3 = originalEdges[j].v1;
                v4 = originalEdges[j].v2;

                if (!EdgesCross(v1, v2, v3, v4)) {
                    continue;
                }

                // this is the only point in optimization where
                // completely new points are created, and it only
                // happens if there is overlapping coplanar
                // geometry in the source triangles
                newVert = EdgeIntersection(v1, v2, v3, v4, opt);

                if (NOT(newVert)) {
//common.Printf( "lines %i (%i to %i) and %i (%i to %i) are colinear\n", i, v1 - optVerts, v2 - optVerts, 
//		   j, v3 - optVerts, v4 - optVerts );	// !@#
                    // colinear, so add both verts of each edge to opposite
                    if (VertexBetween(v3, v1, v2)) {
                        cross = new edgeCrossing_s();// Mem_ClearedAlloc(sizeof(cross));
                        cross.ov = v3;
                        cross.next = crossings[i];
                        crossings[i] = cross;
                    }

                    if (VertexBetween(v4, v1, v2)) {
                        cross = new edgeCrossing_s();// Mem_ClearedAlloc(sizeof(cross));
                        cross.ov = v4;
                        cross.next = crossings[i];
                        crossings[i] = cross;
                    }

                    if (VertexBetween(v1, v3, v4)) {
                        cross = new edgeCrossing_s();// Mem_ClearedAlloc(sizeof(cross));
                        cross.ov = v1;
                        cross.next = crossings[j];
                        crossings[j] = cross;
                    }

                    if (VertexBetween(v2, v3, v4)) {
                        cross = new edgeCrossing_s();//) Mem_ClearedAlloc(sizeof(cross));
                        cross.ov = v2;
                        cross.next = crossings[j];
                        crossings[j] = cross;
                    }

                    continue;
                }
//#if 0
//if ( newVert && newVert != v1 && newVert != v2 && newVert != v3 && newVert != v4 ) {
//common.Printf( "lines %i (%i to %i) and %i (%i to %i) cross at new point %i\n", i, v1 - optVerts, v2 - optVerts, 
//		   j, v3 - optVerts, v4 - optVerts, newVert - optVerts );
//} else if ( newVert ) {
//common.Printf( "lines %i (%i to %i) and %i (%i to %i) intersect at old point %i\n", i, v1 - optVerts, v2 - optVerts, 
//		  j, v3 - optVerts, v4 - optVerts, newVert - optVerts );
//}
//#endif
                if (!newVert.equals(v1) && !newVert.equals(v2)) {
                    cross = new edgeCrossing_s();// Mem_ClearedAlloc(sizeof(cross));
                    cross.ov = newVert;
                    cross.next = crossings[i];
                    crossings[i] = cross;
                }

                if (!newVert.equals(v3) && !newVert.equals(v4)) {
                    cross = new edgeCrossing_s();// Mem_ClearedAlloc(sizeof(cross));
                    cross.ov = newVert;
                    cross.next = crossings[j];
                    crossings[j] = cross;
                }

            }
        }

        // now split each edge by its crossing points
        // colinear edges will have duplicated edges added, but it won't hurt anything
        for (i = 0; i < numOriginalEdges; i++) {
            edgeCrossing_s cross, nextCross;
            int numCross;
            optVertex_s[] sorted;

            numCross = 0;
            for (cross = crossings[i]; cross != null; cross = cross.next) {
                numCross++;
            }
            numCross += 2;	// account for originals
            sorted = new optVertex_s[numCross];// Mem_Alloc(numCross);
            sorted[0] = originalEdges[i].v1;
            sorted[1] = originalEdges[i].v2;
            j = 2;
            for (cross = crossings[i]; cross != null; cross = nextCross) {
                nextCross = cross.next;
                sorted[j] = cross.ov;
                cross = null;//Mem_Free(cross);
                j++;
            }

            // add all possible fragment combinations that aren't divided
            // by another point
            for (j = 0; j < numCross; j++) {
                for (k = j + 1; k < numCross; k++) {
                    for (l = 0; l < numCross; l++) {
                        if ((sorted[l] == sorted[j]) || (sorted[l] == sorted[k])) {
                            continue;
                        }
                        if (sorted[j] == sorted[k]) {
                            continue;
                        }
                        if (VertexBetween(sorted[l], sorted[j], sorted[k])) {
                            break;
                        }
                    }
                    if (l == numCross) {
//common.Printf( "line %i fragment from point %i to %i\n", i, sorted[j] - optVerts, sorted[k] - optVerts );
                        AddEdgeIfNotAlready(sorted[j], sorted[k]);
                    }
                }
            }

            sorted = null;//Mem_Free(sorted);
        }

        crossings = null;//Mem_Free(crossings);
        originalEdges = null;//Mem_Free(originalEdges);

        // check for duplicated edges
        for (i = 0; i < numOptEdges; i++) {
            for (j = i + 1; j < numOptEdges; j++) {
                if (((optEdges[i].v1 == optEdges[j].v1) && (optEdges[i].v2 == optEdges[j].v2))
                        || ((optEdges[i].v1 == optEdges[j].v2) && (optEdges[i].v2 == optEdges[j].v1))) {
                    common.Printf("duplicated optEdge\n");
                }
            }
        }

        if (dmapGlobals.verbose) {
            common.Printf("%6d original edges\n", numOriginalEdges);
            common.Printf("%6d edges after splits\n", numOptEdges);
            common.Printf("%6d original vertexes\n", numOriginalVerts);
            common.Printf("%6d vertexes after splits\n", numOptVerts);
        }
    }

//=================================================================
    /*
     ===================
     CullUnusedVerts

     Unlink any verts with no edges, so they
     won't be used in the retriangulation
     ===================
     */
    static void CullUnusedVerts(optIsland_t island) {
        optVertex_s prev, vert;
        int c_keep, c_free;
        optEdge_s edge;

        c_keep = 0;
        c_free = 0;

        for (prev = island.verts; prev != null;) {
            vert = prev;

            if (NOT(vert.edges)) {
                // free it
                prev = vert.islandLink;
                c_free++;
            } else {
                edge = vert.edges;
                if ((edge.v1.equals(vert) && NOT(edge.v1link))
                        || (edge.v2.equals(vert) && NOT(edge.v2link))) {
                    // is is occasionally possible to get a vert
                    // with only a single edge when colinear optimizations
                    // crunch down a complex sliver
                    UnlinkEdge(edge, island);
                    // free it
                    prev = vert.islandLink;
                    c_free++;
                } else {
                    prev = vert.islandLink;
                    c_keep++;
                }
            }
        }

        if (dmapGlobals.verbose) {
            common.Printf("%6d verts kept\n", c_keep);
            common.Printf("%6d verts freed\n", c_free);
        }
    }

    /*
     ====================
     OptimizeIsland

     At this point, all needed vertexes are already in the
     list, including any that were added at crossing points.

     Interior and colinear vertexes will be removed, and
     a new triangulation will be created.
     ====================
     */
    static void OptimizeIsland(optIsland_t island) {
        // add space-filling fake edges so we have a complete
        // triangulation of a convex hull before optimization
        AddInteriorEdges(island);
        DrawEdges(island);

        // determine all the possible triangles, and decide if
        // the are filled or empty
        BuildOptTriangles(island);

        // remove interior vertexes that have filled triangles
        // between all their edges
        RemoveInteriorEdges(island);
        DrawEdges(island);

        ValidateEdgeCounts(island);

        // remove vertexes that only have two colinear edges
        CombineColinearEdges(island);
        CullUnusedVerts(island);
        DrawEdges(island);

        // add new internal edges between the remaining exterior edges
        // to give us a full triangulation again
        AddInteriorEdges(island);
        DrawEdges(island);

        // determine all the possible triangles, and decide if
        // the are filled or empty
        BuildOptTriangles(island);

        // make mapTri_s out of the filled optTri_s
        RegenerateTriangles(island);
    }

    /*
     ================
     AddVertexToIsland_r
     ================
     */
    static void AddVertexToIsland_r(optVertex_s vert, optIsland_t island) {
        optEdge_s e;

        // we can't just check islandLink, because the
        // last vert will have a NULL
        if (vert.addedToIsland) {
            return;
        }
        vert.addedToIsland = true;
        vert.islandLink = island.verts;
        island.verts = vert;

        for (e = vert.edges; e != null;) {
            if (!e.addedToIsland) {
                e.addedToIsland = true;

                e.islandLink = island.edges;
                island.edges = e;
            }

            if (e.v1.equals(vert)) {
                AddVertexToIsland_r(e.v2, island);
                e = e.v1link;
                continue;
            }
            if (e.v2.equals(vert)) {
                AddVertexToIsland_r(e.v1, island);
                e = e.v2link;
                continue;
            }
            common.Error("AddVertexToIsland_r: mislinked vert");
        }

    }

    /*
     ====================
     SeparateIslands

     While the algorithm should theoretically handle any collection
     of triangles, there are speed and stability benefits to making
     it work on as small a list as possible, so separate disconnected
     collections of edges and process separately.

     FIXME: we need to separate the source triangles before
     doing this, because PointInSourceTris() can give a bad answer if
     the source list has triangles not used in the optimization
     ====================
     */
    static void SeparateIslands(optimizeGroup_s opt) {
        int i;
        final optIsland_t island = new optIsland_t();
        int numIslands;

        DrawAllEdges();

        numIslands = 0;
        for (i = 0; i < numOptVerts; i++) {
            if (optVerts[i].addedToIsland) {
                continue;
            }
            numIslands++;
//		memset( &island, 0, sizeof( island ) );

            island.group = opt;
            AddVertexToIsland_r(optVerts[i], island);
            OptimizeIsland(island);
        }
        if (dmapGlobals.verbose) {
            common.Printf("%6d islands\n", numIslands);
        }
    }

    static void DontSeparateIslands(optimizeGroup_s opt) {
        int i;
        optIsland_t island;

        DrawAllEdges();

//	memset( &island, 0, sizeof( island ) );
        island = new optIsland_t();
        island.group = opt;

        // link everything together
        for (i = 0; i < numOptVerts; i++) {
            optVerts[i].islandLink = island.verts;
            island.verts = optVerts[i];
        }

        for (i = 0; i < numOptEdges; i++) {
            optEdges[i].islandLink = island.edges;
            island.edges = optEdges[i];
        }

        OptimizeIsland(island);
    }


    /*
     ====================
     PointInSourceTris

     This is a sloppy bounding box check
     ====================
     */
    static boolean PointInSourceTris(float x, float y, float z, optimizeGroup_s opt) {
        mapTri_s tri;
        final idBounds b = new idBounds();
        idVec3 p;

        if (!opt.material.IsDrawn()) {
            return false;
        }

        p = new idVec3(x, y, z);
        for (tri = opt.triList; tri != null; tri = tri.next) {
            b.Clear();
            b.AddPoint(tri.v[0].xyz);
            b.AddPoint(tri.v[1].xyz);
            b.AddPoint(tri.v[2].xyz);

            if (b.ContainsPoint(p)) {
                return true;
            }
        }
        return false;
    }

    /*
     ====================
     OptimizeOptList
     ====================
     */
    static void OptimizeOptList(optimizeGroup_s opt) {
        optimizeGroup_s oldNext;

        // fix the t junctions among this single list
        // so we can match edges
        // can we avoid doing this if colinear vertexes break edges?
        oldNext = opt.nextGroup;
        opt.nextGroup = null;
        FixAreaGroupsTjunctions(opt);
        opt.nextGroup = oldNext;

        // create the 2D vectors
        dmapGlobals.mapPlanes.oGet(opt.planeNum).Normal().NormalVectors(opt.axis[0], opt.axis[1]);

        AddOriginalEdges(opt);
        SplitOriginalEdgesAtCrossings(opt);

//#if 0
//	// seperate any discontinuous areas for individual optimization
//	// to reduce the scope of the problem
//	SeparateIslands( opt );
//#else
        DontSeparateIslands(opt);
//#endif

        // now free the hash verts
        FreeTJunctionHash();

        // free the original list and use the new one
        FreeTriList(opt.triList);
        opt.triList = opt.regeneratedTris;
        opt.regeneratedTris = null;
    }


    /*
     ==================
     SetGroupTriPlaneNums

     Copies the group planeNum to every triangle in each group
     ==================
     */
    static void SetGroupTriPlaneNums(optimizeGroup_s groups) {
        mapTri_s tri;
        optimizeGroup_s group;

        for (group = groups; group != null; group = group.nextGroup) {
            for (tri = group.triList; tri != null; tri = tri.next) {
                tri.planeNum = group.planeNum;
            }
        }
    }


    /*
     ===================
     OptimizeGroupList

     This will also fix tjunctions

     ===================
     */
    static void OptimizeGroupList(optimizeGroup_s groupList) {
        int c_in, c_edge, c_tjunc2;
        optimizeGroup_s group;

        if (NOT(groupList)) {
            return;
        }

        c_in = CountGroupListTris(groupList);

        // optimize and remove colinear edges, which will
        // re-introduce some t junctions
        for (group = groupList; group != null; group = group.nextGroup) {
            OptimizeOptList(group);
        }
        c_edge = CountGroupListTris(groupList);

        // fix t junctions again
        FixAreaGroupsTjunctions(groupList);
        FreeTJunctionHash();
        c_tjunc2 = CountGroupListTris(groupList);

        SetGroupTriPlaneNums(groupList);

        common.Printf("----- OptimizeAreaGroups Results -----\n");
        common.Printf("%6d tris in\n", c_in);
        common.Printf("%6d tris after edge removal optimization\n", c_edge);
        common.Printf("%6d tris after final t junction fixing\n", c_tjunc2);
    }


    /*
     ==================
     OptimizeEntity
     ==================
     */
    static void OptimizeEntity(uEntity_t e) {
        int i;

        common.Printf("----- OptimizeEntity -----\n");
        for (i = 0; i < e.numAreas; i++) {
            OptimizeGroupList(e.areas[i].groups);
        }
    }
}
