package neo.Renderer;

import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.framework.Common.common;
import static neo.idlib.math.Plane.ON_EPSILON;

import neo.Renderer.Model.srfTriangles_s;
import neo.idlib.Lib.idException;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;

/**
 *
 */
public class tr_polytope {

    static final int MAX_POLYTOPE_PLANES = 6;

    /*
     =====================
     R_PolytopeSurface

     Generate vertexes and indexes for a polytope, and optionally returns the polygon windings.
     The positive sides of the planes will be visible.
     =====================
     */
    public static srfTriangles_s R_PolytopeSurface(int numPlanes, final idPlane[] planes, idWinding[] windings) throws idException {
        int i, j;
        srfTriangles_s tri;
        final idFixedWinding[] planeWindings = new idFixedWinding[MAX_POLYTOPE_PLANES];
        int numVerts, numIndexes;

        if (numPlanes > MAX_POLYTOPE_PLANES) {
            common.Error("R_PolytopeSurface: more than %d planes", MAX_POLYTOPE_PLANES);
        }

        numVerts = 0;
        numIndexes = 0;
        for (i = 0; i < numPlanes; i++) {
            final idPlane plane = planes[i];
            final idFixedWinding w = planeWindings[i] = new idFixedWinding();

            w.BaseForPlane(plane);
            for (j = 0; j < numPlanes; j++) {
                final idPlane plane2 = planes[j];
                if (j == i) {
                    continue;
                }
                if (!w.ClipInPlace(plane2.oNegative(), ON_EPSILON)) {
                    break;
                }
            }
            if (w.GetNumPoints() <= 2) {
                continue;
            }
            numVerts += w.GetNumPoints();
            numIndexes += (w.GetNumPoints() - 2) * 3;
        }

        // allocate the surface
        tri = new srfTriangles_s();//R_AllocStaticTriSurf();
        R_AllocStaticTriSurfVerts(tri, numVerts);
        R_AllocStaticTriSurfIndexes(tri, numIndexes);

        // copy the data from the windings
        for (i = 0; i < numPlanes; i++) {
            final idFixedWinding w = planeWindings[i];
            if (0 == w.GetNumPoints()) {
                continue;
            }
            for (j = 0; j < w.GetNumPoints(); j++) {
                tri.verts[tri.numVerts + j].Clear();
                tri.verts[tri.numVerts + j].xyz.oSet(w.oGet(j).ToVec3());
            }

            for (j = 1; j < (w.GetNumPoints() - 1); j++) {
                tri.indexes.getIntBuffer().put( tri.numIndexes + 0, tri.numVerts);
                tri.indexes.getIntBuffer().put( tri.numIndexes + 1, tri.numVerts + j);
                tri.indexes.getIntBuffer().put( tri.numIndexes + 2, tri.numVerts + j + 1);
                tri.numIndexes += 3;
            }
            tri.numVerts += w.GetNumPoints();

            // optionally save the winding
            if (windings != null) {
                windings[i] = new idWinding(w);
            }
        }

        R_BoundTriSurf(tri);

        return tri;
    }

}
