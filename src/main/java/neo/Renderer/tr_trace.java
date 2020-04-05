package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.RenderSystem_init.r_showTrace;
import static neo.Renderer.tr_backend.GL_State;
import static neo.Renderer.tr_backend.GL_TexEnv;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_ALWAYS;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_render.RB_DrawElementsImmediate;
import static neo.Renderer.tr_rendertools.RB_DrawBounds;
import static neo.Renderer.tr_trisurf.R_DeriveFacePlanes;
import static neo.framework.Common.common;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.opengl.QGL.qglBegin;
import static neo.opengl.QGL.qglColor4f;
import static neo.opengl.QGL.qglDisableClientState;
import static neo.opengl.QGL.qglEnd;
import static neo.opengl.QGL.qglLoadMatrixf;
import static neo.opengl.QGL.qglVertex3f;
import static neo.opengl.QGLConstantsIfc.GL_LINE_LOOP;
import static neo.opengl.QGLConstantsIfc.GL_MODULATE;
import static neo.opengl.QGLConstantsIfc.GL_TEXTURE_COORD_ARRAY;

import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.localTrace_t;
import neo.idlib.Timer.idTimer;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Math_h;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.opengl.Nio;

/**
 *
 */
public class tr_trace {

    private static final boolean TEST_TRACE = false;

    /*
     =================
     R_LocalTrace

     If we resort the vertexes so all silverts come first, we can save some work here.
     =================
     */
    public static localTrace_t R_LocalTrace(final idVec3 start, final idVec3 end, final float radius, final srfTriangles_s tri) {
        int i, j;
        byte[] cullBits;
        final idPlane[] planes = idPlane.generateArray(4);
        final localTrace_t hit = new localTrace_t();
        int c_testEdges, c_testPlanes, c_intersect;
        idVec3 startDir;
        final byte[] totalOr = new byte[1];
        float radiusSqr;
        idTimer trace_timer = null;

        if (TEST_TRACE) {
            trace_timer = new idTimer();
            trace_timer.Start();
        }

        hit.fraction = 1.0f;

        // create two planes orthogonal to each other that intersect along the trace
        startDir = end.oMinus(start);
        startDir.Normalize();
        startDir.NormalVectors(planes[0].Normal(), planes[1].Normal());
        planes[0].oSet(3, -start.oMultiply(planes[0].Normal()));
        planes[1].oSet(3, -start.oMultiply(planes[1].Normal()));

        // create front and end planes so the trace is on the positive sides of both
        planes[2].oSet(startDir);
        planes[2].oSet(3, -start.oMultiply(planes[2].Normal()));
        planes[3].oSet(startDir.oNegative());
        planes[3].oSet(3, -end.oMultiply(planes[3].Normal()));

        // catagorize each point against the four planes
        cullBits = new byte[tri.numVerts];
        SIMDProcessor.TracePointCull(cullBits, totalOr, radius, planes, tri.verts, tri.numVerts);

        // if we don't have points on both sides of both the ray planes, no intersection
        if (((totalOr[0] ^ (totalOr[0] >> 4)) & 3) != 0) {
            //common.Printf( "nothing crossed the trace planes\n" );
            return hit;
        }

        // if we don't have any points between front and end, no intersection
        if (((totalOr[0] ^ (totalOr[0] >> 1)) & 4) != 0) {
            //common.Printf( "trace didn't reach any triangles\n" );
            return hit;
        }

        // scan for triangles that cross both planes
        c_testPlanes = 0;
        c_testEdges = 0;
        c_intersect = 0;

        radiusSqr = Math_h.Square(radius);
        startDir = end.oMinus(start);

        if ((null == tri.facePlanes) || !tri.facePlanesCalculated) {
            R_DeriveFacePlanes(tri);
        }

        for (i = 0, j = 0; i < tri.numIndexes; i += 3, j++) {
            float d1, d2, f, d;
            float edgeLengthSqr;
            idPlane plane;
            idVec3 point;
            final idVec3[] dir = new idVec3[3];
            idVec3 cross;
            idVec3 edge;
            byte triOr;

            // get sidedness info for the triangle
            triOr = cullBits[tri.indexes[i + 0]];
            triOr |= cullBits[tri.indexes[i + 1]];
            triOr |= cullBits[tri.indexes[i + 2]];

            // if we don't have points on both sides of both the ray planes, no intersection
            if (((triOr ^ (triOr >> 4)) & 3) != 0) {
                continue;
            }

            // if we don't have any points between front and end, no intersection
            if (((triOr ^ (triOr >> 1)) & 4) != 0) {
                continue;
            }

            c_testPlanes++;

            plane = tri.facePlanes[j];
            d1 = plane.Distance(start);
            d2 = plane.Distance(end);

            if (d1 <= d2) {
                continue;		// comning at it from behind or parallel
            }

            if (d1 < 0.0f) {
                continue;		// starts past it
            }

            if (d2 > 0.0f) {
                continue;		// finishes in front of it
            }

            f = d1 / (d1 - d2);

            if (f < 0.0f) {
                continue;		// shouldn't happen
            }

            if (f >= hit.fraction) {
                continue;		// have already hit something closer
            }

            c_testEdges++;

            // find the exact point of impact with the plane
            point = start.oPlus(startDir.oMultiply(f));

            // see if the point is within the three edges
            // if radius > 0 the triangle is expanded with a circle in the triangle plane
            dir[0] = tri.verts[tri.indexes[i + 0]].xyz.oMinus(point);
            dir[1] = tri.verts[tri.indexes[i + 1]].xyz.oMinus(point);

            cross = dir[0].Cross(dir[1]);
            d = plane.Normal().oMultiply(cross);
            if (d > 0.0f) {
                if (radiusSqr <= 0.0f) {
                    continue;
                }
                edge = tri.verts[tri.indexes[i + 0]].xyz.oMinus(tri.verts[tri.indexes[i + 1]].xyz);
                edgeLengthSqr = edge.LengthSqr();
                if (cross.LengthSqr() > (edgeLengthSqr * radiusSqr)) {
                    continue;
                }
                d = dir[0].oMultiply(edge);
                if (d < 0.0f) {
                    edge = tri.verts[tri.indexes[i + 0]].xyz.oMinus(tri.verts[tri.indexes[i + 2]].xyz);
                    d = dir[0].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[0].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                } else if (d > edgeLengthSqr) {
                    edge = tri.verts[tri.indexes[i + 1]].xyz.oMinus(tri.verts[tri.indexes[i + 2]].xyz);
                    d = dir[1].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[1].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                }
            }

            dir[2] = tri.verts[tri.indexes[i + 2]].xyz.oMinus(point);

            cross = dir[1].Cross(dir[2]);
            d = plane.Normal().oMultiply(cross);
            if (d > 0.0f) {
                if (radiusSqr <= 0.0f) {
                    continue;
                }
                edge = tri.verts[tri.indexes[i + 1]].xyz.oMinus(tri.verts[tri.indexes[i + 2]].xyz);
                edgeLengthSqr = edge.LengthSqr();
                if (cross.LengthSqr() > (edgeLengthSqr * radiusSqr)) {
                    continue;
                }
                d = dir[1].oMultiply(edge);
                if (d < 0.0f) {
                    edge = tri.verts[tri.indexes[i + 1]].xyz.oMinus(tri.verts[tri.indexes[i + 0]].xyz);
                    d = dir[1].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[1].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                } else if (d > edgeLengthSqr) {
                    edge = tri.verts[tri.indexes[i + 2]].xyz.oMinus(tri.verts[tri.indexes[i + 0]].xyz);
                    d = dir[2].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[2].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                }
            }

            cross = dir[2].Cross(dir[0]);
            d = plane.Normal().oMultiply(cross);
            if (d > 0.0f) {
                if (radiusSqr <= 0.0f) {
                    continue;
                }
                edge = tri.verts[tri.indexes[i + 2]].xyz.oMinus(tri.verts[tri.indexes[i + 0]].xyz);
                edgeLengthSqr = edge.LengthSqr();
                if (cross.LengthSqr() > (edgeLengthSqr * radiusSqr)) {
                    continue;
                }
                d = dir[2].oMultiply(edge);
                if (d < 0.0f) {
                    edge = tri.verts[tri.indexes[i + 2]].xyz.oMinus(tri.verts[tri.indexes[i + 1]].xyz);
                    d = dir[2].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[2].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                } else if (d > edgeLengthSqr) {
                    edge = tri.verts[tri.indexes[i + 0]].xyz.oMinus(tri.verts[tri.indexes[i + 1]].xyz);
                    d = dir[0].oMultiply(edge);
                    if (d < 0.0f) {
                        if (dir[0].LengthSqr() > radiusSqr) {
                            continue;
                        }
                    }
                }
            }

            // we hit it
            c_intersect++;

            hit.fraction = f;
            hit.normal = plane.Normal();
            hit.point = point;
            hit.indexes[0] = tri.indexes[i];
            hit.indexes[1] = tri.indexes[i + 1];
            hit.indexes[2] = tri.indexes[i + 2];
        }

        if (TEST_TRACE) {
            trace_timer.Stop();
            common.Printf("testVerts:%d c_testPlanes:%d c_testEdges:%d c_intersect:%d msec:%1.4f\n",
                    tri.numVerts, c_testPlanes, c_testEdges, c_intersect, trace_timer.Milliseconds());
        }

        return hit;
    }

    /*
     =================
     RB_DrawExpandedTriangles
     =================
     */
    public static void RB_DrawExpandedTriangles(final srfTriangles_s tri, final float radius, final idVec3 vieworg) {
        int i, j, k;
        final idVec3[] dir = new idVec3[6];
        idVec3 normal, point;

        for (i = 0; i < tri.numIndexes; i += 3) {

            final idVec3[] p/*[3]*/ = {
                        tri.verts[tri.indexes[i + 0]].xyz,
                        tri.verts[tri.indexes[i + 1]].xyz,
                        tri.verts[tri.indexes[i + 2]].xyz};

            dir[0] = p[0].oMinus(p[1]);
            dir[1] = p[1].oMinus(p[2]);
            dir[2] = p[2].oMinus(p[0]);

            normal = dir[0].Cross(dir[1]);

            if (normal.oMultiply(p[0]) < normal.oMultiply(vieworg)) {
                continue;
            }

            dir[0] = normal.Cross(dir[0]);
            dir[1] = normal.Cross(dir[1]);
            dir[2] = normal.Cross(dir[2]);

            dir[0].Normalize();
            dir[1].Normalize();
            dir[2].Normalize();

            qglBegin(GL_LINE_LOOP);

            for (j = 0; j < 3; j++) {
                k = (j + 1) % 3;

                dir[4] = (dir[j].oPlus(dir[k])).oMultiply(0.5f);
                dir[4].Normalize();

                dir[3] = (dir[j].oPlus(dir[4])).oMultiply(0.5f);
                dir[3].Normalize();

                dir[5] = (dir[4].oPlus(dir[k])).oMultiply(0.5f);
                dir[5].Normalize();

                point = p[k].oPlus(dir[j].oMultiply(radius));
                qglVertex3f(point.oGet(0), point.oGet(1), point.oGet(2));

                point = p[k].oPlus(dir[3].oMultiply(radius));
                qglVertex3f(point.oGet(0), point.oGet(1), point.oGet(2));

                point = p[k].oPlus(dir[4].oMultiply(radius));
                qglVertex3f(point.oGet(0), point.oGet(1), point.oGet(2));

                point = p[k].oPlus(dir[5].oMultiply(radius));
                qglVertex3f(point.oGet(0), point.oGet(1), point.oGet(2));

                point = p[k].oPlus(dir[k].oMultiply(radius));
                qglVertex3f(point.oGet(0), point.oGet(1), point.oGet(2));
            }

            qglEnd();
        }
    }

    /*
     ================
     RB_ShowTrace

     Debug visualization
     ================
     */
    public static void RB_ShowTrace(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i;
        srfTriangles_s tri;
        drawSurf_s surf;
        idVec3 start, end;
        final idVec3 localStart = new idVec3(), localEnd = new idVec3();
        localTrace_t hit;
        float radius;

        if (r_showTrace.GetInteger() == 0) {
            return;
        }

        if (r_showTrace.GetInteger() == 2) {
            radius = 5.0f;
        } else {
            radius = 0.0f;
        }

        // determine the points of the trace
        start = backEnd.viewDef.renderView.vieworg;
        end = start.oPlus(backEnd.viewDef.renderView.viewaxis.oGet(0).oMultiply(4000));

        // check and draw the surfaces
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        GL_TexEnv(GL_MODULATE);

        globalImages.whiteImage.Bind();

        // find how many are ambient
        for (i = 0; i < numDrawSurfs; i++) {
            surf = drawSurfs[i];
            tri = surf.geo;

            if (i > 211) {
				continue;
			}
            
            if ((tri == null) || (tri.verts == null)) {
                continue;
            }

            // transform the points into local space
            R_GlobalPointToLocal(surf.space.modelMatrix, start, localStart);
            R_GlobalPointToLocal(surf.space.modelMatrix, end, localEnd);

            // check the bounding box
            if (!tri.bounds.Expand(radius).LineIntersection(localStart, localEnd)) {
                continue;
            }

            qglLoadMatrixf(Nio.wrap(surf.space.modelViewMatrix));

            // highlight the surface
            GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);

            qglColor4f(1, 0, 0, 0.25f);
            RB_DrawElementsImmediate(tri);

            // draw the bounding box
            GL_State(GLS_DEPTHFUNC_ALWAYS);

            qglColor4f(1, 1, 1, 1);
            RB_DrawBounds(tri.bounds);

            if (radius != 0.0f) {
                // draw the expanded triangles
                qglColor4f(0.5f, 0.5f, 1.0f, 1.0f);
                RB_DrawExpandedTriangles(tri, radius, localStart);
            }

            // check the exact surfaces
            hit = R_LocalTrace(localStart, localEnd, radius, tri);
            if (hit.fraction < 1.0) {
                qglColor4f(1, 1, 1, 1);
                RB_DrawBounds(new idBounds(hit.point).Expand(1));
            }
        }
    }
}
