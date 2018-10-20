package neo.Tools.Compilers.DMap;

import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Material.materialCoverage_t.MC_OPAQUE;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.DMap.dmap.MAX_GROUP_LIGHTS;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.mapLight_t;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.dmap.primitive_s;
import neo.Tools.Compilers.DMap.dmap.side_s;
import neo.Tools.Compilers.DMap.dmap.textureVectors_t;
import neo.Tools.Compilers.DMap.dmap.uArea_t;
import neo.Tools.Compilers.DMap.dmap.uBrush_t;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import static neo.Tools.Compilers.DMap.map.FindFloatPlane;
import static neo.Tools.Compilers.DMap.map.FreeOptimizeGroupList;
import static neo.Tools.Compilers.DMap.shadowopt3.CreateLightShadow;
import static neo.Tools.Compilers.DMap.tritools.AllocTri;
import static neo.Tools.Compilers.DMap.tritools.CopyMapTri;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.tritools.MapTriArea;
import static neo.Tools.Compilers.DMap.tritools.MergeTriLists;
import static neo.Tools.Compilers.DMap.tritools.PlaneForTri;
import static neo.Tools.Compilers.DMap.tritools.WindingForTri;
import static neo.Tools.Compilers.DMap.tritools.WindingToTriList;
import static neo.framework.Common.common;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Plane.ON_EPSILON;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.DotProduct;
import static neo.idlib.math.Vector.VectorCopy;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec5;
import static neo.sys.win_shared.Sys_Milliseconds;

/**
 *
 */
public class usurface {

    static final double TEXTURE_OFFSET_EQUAL_EPSILON = 0.005;
    static final double TEXTURE_VECTOR_EQUAL_EPSILON = 0.001;
    //
    static final int    SNAP_FLOAT_TO_INT            = 256;
    static final double SNAP_INT_TO_FLOAT            = (1.0 / SNAP_FLOAT_TO_INT);

    /*
     ===============
     AddTriListToArea

     The triList is appended to the apropriate optimzeGroup_t,
     creating a new one if needed.
     The entire list is assumed to come from the same planar primitive
     ===============
     */
    static void AddTriListToArea(uEntity_t e, mapTri_s triList, int planeNum, int areaNum, textureVectors_t texVec) {
        uArea_t area;
        optimizeGroup_s group;
        int i, j;

        if (NOT(triList)) {
            return;
        }

        area = e.areas[areaNum];
        for (group = area.groups; group != null; group = group.nextGroup) {
            if (group.material == triList.material
                    && group.planeNum == planeNum
                    && group.mergeGroup == triList.mergeGroup) {
                // check the texture vectors
                for (i = 0; i < 2; i++) {
                    for (j = 0; j < 3; j++) {
                        if (idMath.Fabs(texVec.v[i].oGet(j) - group.texVec.v[i].oGet(j)) > TEXTURE_VECTOR_EQUAL_EPSILON) {
                            break;
                        }
                    }
                    if (j != 3) {
                        break;
                    }
                    if (idMath.Fabs(texVec.v[i].oGet(3) - group.texVec.v[i].oGet(3)) > TEXTURE_OFFSET_EQUAL_EPSILON) {
                        break;
                    }
                }
                if (i == 2) {
                    break;	// exact match
                } else {
                    // different texture offsets
                    i = 1;	// just for debugger breakpoint
                }
            }
        }

        if (NOT(group)) {
            group = new optimizeGroup_s();// Mem_Alloc(sizeof(group));
            //		memset( group, 0, sizeof( *group ) );
            group.planeNum = planeNum;
            group.mergeGroup = triList.mergeGroup;
            group.material = triList.material;
            group.nextGroup = area.groups;
            group.texVec = texVec;
            area.groups = group;
        }

        group.triList = MergeTriLists(group.triList, triList);
    }

    /*
     ===================
     TexVecForTri
     ===================
     */
    static void TexVecForTri(textureVectors_t texVec, mapTri_s tri) {
        float area, inva;
        idVec3 temp = new idVec3();
        idVec5 d0 = new idVec5(), d1 = new idVec5();
        idDrawVert a, b, c;

        a = tri.v[0];
        b = tri.v[1];
        c = tri.v[2];

        d0.oSet(0, b.xyz.oGet(0) - a.xyz.oGet(0));
        d0.oSet(1, b.xyz.oGet(1) - a.xyz.oGet(1));
        d0.oSet(2, b.xyz.oGet(2) - a.xyz.oGet(2));
        d0.oSet(3, b.st.oGet(0) - a.st.oGet(0));
        d0.oSet(4, b.st.oGet(1) - a.st.oGet(1));

        d1.oSet(0, c.xyz.oGet(0) - a.xyz.oGet(0));
        d1.oSet(1, c.xyz.oGet(1) - a.xyz.oGet(1));
        d1.oSet(2, c.xyz.oGet(2) - a.xyz.oGet(2));
        d1.oSet(3, c.st.oGet(0) - a.st.oGet(0));
        d1.oSet(4, c.st.oGet(1) - a.st.oGet(1));

        area = d0.oGet(3) * d1.oGet(4) - d0.oGet(4) * d1.oGet(3);
        inva = 1.0f / area;

        temp.oSet(0, (d0.oGet(0) * d1.oGet(4) - d0.oGet(4) * d1.oGet(0)) * inva);
        temp.oSet(1, (d0.oGet(1) * d1.oGet(4) - d0.oGet(4) * d1.oGet(1)) * inva);
        temp.oSet(2, (d0.oGet(2) * d1.oGet(4) - d0.oGet(4) * d1.oGet(2)) * inva);
        temp.Normalize();
        texVec.v[0].oSet(temp);
        texVec.v[0].oSet(3, tri.v[0].xyz.oMultiply(texVec.v[0].ToVec3()) - tri.v[0].st.oGet(0));

        temp.oSet(0, (d0.oGet(3) * d1.oGet(0) - d0.oGet(0) * d1.oGet(3)) * inva);
        temp.oSet(1, (d0.oGet(3) * d1.oGet(1) - d0.oGet(1) * d1.oGet(3)) * inva);
        temp.oSet(2, (d0.oGet(3) * d1.oGet(2) - d0.oGet(2) * d1.oGet(3)) * inva);
        temp.Normalize();
        texVec.v[1].oSet(temp);
        texVec.v[1].oSet(3, tri.v[0].xyz.oMultiply(texVec.v[0].ToVec3()) - tri.v[0].st.oGet(1));
    }


    /*
     =================
     TriListForSide
     =================
     */
    //#define	SNAP_FLOAT_TO_INT	8
    static mapTri_s TriListForSide(final side_s s, final idWinding w) {
        int i, j;
        idDrawVert dv;
        mapTri_s tri, triList;
        idVec3 vec;
        idMaterial si;

        si = s.material;

        // skip any generated faces
        if (NOT(si)) {
            return null;
        }

        // don't create faces for non-visible sides
        if (!si.SurfaceCastsShadow() && !si.IsDrawn()) {
            return null;
        }

        //	if ( 1 ) {
        // triangle fan using only the outer verts
        // this gives the minimum triangle count,
        // but may have some very distended triangles
        triList = null;
        for (i = 2; i < w.GetNumPoints(); i++) {
            tri = AllocTri();
            tri.material = si;
            tri.next = triList;
            triList = tri;

            for (j = 0; j < 3; j++) {
                if (j == 0) {
                    vec = w.oGet(0).ToVec3();
                } else if (j == 1) {
                    vec = w.oGet(i - 1).ToVec3();
                } else {
                    vec = w.oGet(i).ToVec3();
                }

                dv = tri.v[j];
                //#if 0
                //				// round the xyz to a given precision
                //				for ( k = 0 ; k < 3 ; k++ ) {
                //					dv.xyz[k] = SNAP_INT_TO_FLOAT * floor( vec[k] * SNAP_FLOAT_TO_INT + 0.5 );
                //				}
                //#else
                VectorCopy(vec, dv.xyz);//TODO:copy range?????
                //#endif

                // calculate texture s/t from brush primitive texture matrix
                dv.st.oSet(0, DotProduct(dv.xyz, s.texVec.v[0]) + s.texVec.v[0].oGet(3));
                dv.st.oSet(1, DotProduct(dv.xyz, s.texVec.v[1]) + s.texVec.v[1].oGet(3));

                // copy normal
                dv.normal = dmapGlobals.mapPlanes.oGet(s.planenum).Normal();
                if (dv.normal.Length() < 0.9 || dv.normal.Length() > 1.1) {
                    common.Error("Bad normal in TriListForSide");
                }
            }
        }
        //	} else {
        //		// triangle fan from central point, more verts and tris, but less distended
        //		// I use this when debugging some tjunction problems
        //		triList = NULL;
        //		for ( i = 0 ; i < w.GetNumPoints() ; i++ ) {
        //			idVec3	midPoint;
        //
        //			tri = AllocTri();
        //			tri.material = si;	
        //			tri.next = triList;
        //			triList = tri;
        //
        //			for ( j = 0 ; j < 3 ; j++ ) {
        //				if ( j == 0 ) {
        //					vec = &midPoint;
        //					midPoint = w.GetCenter();
        //				} else if ( j == 1 ) {
        //					vec = &((*w)[i]).ToVec3();
        //				} else {
        //					vec = &((*w)[(i+1)%w.GetNumPoints()]).ToVec3();
        //				}
        //
        //				dv = tri.v + j;
        //
        //				VectorCopy( *vec, dv.xyz );
        //				
        //				// calculate texture s/t from brush primitive texture matrix
        //				dv.st[0] = DotProduct( dv.xyz, s.texVec.v[0] ) + s.texVec.v[0][3];
        //				dv.st[1] = DotProduct( dv.xyz, s.texVec.v[1] ) + s.texVec.v[1][3];
        //
        //				// copy normal
        //				dv.normal = dmapGlobals.mapPlanes[s.planenum].Normal();
        //				if ( dv.normal.Length() < 0.9f || dv.normal.Length() > 1.1f ) {
        //					common.Error( "Bad normal in TriListForSide" );
        //				}
        //			}
        //		}
        //	}

        // set merge groups if needed, to prevent multiple sides from being
        // merged into a single surface in the case of gui shaders, mirrors, and autosprites
        if (s.material.IsDiscrete()) {
            for (tri = triList; tri != null; tri = tri.next) {
                tri.mergeGroup = s;
            }
        }

        return triList;
    }

    //=================================================================================

    /*
     ====================
     ClipSideByTree_r

     Adds non-opaque leaf fragments to the convex hull
     ====================
     */
    static void ClipSideByTree_r(idWinding w, side_s side, node_s node) {
        idWinding front = new idWinding(), back = new idWinding();

        if (NOT(w)) {
            return;
        }

        if (node.planenum != PLANENUM_LEAF) {
            if (side.planenum == node.planenum) {
                ClipSideByTree_r(w, side, node.children[0]);
                return;
            }
            if (side.planenum == (node.planenum ^ 1)) {
                ClipSideByTree_r(w, side, node.children[1]);
                return;
            }

            w.Split(dmapGlobals.mapPlanes.oGet(node.planenum), ON_EPSILON, front, back);
            //		delete w;

            ClipSideByTree_r(front, side, node.children[0]);
            ClipSideByTree_r(back, side, node.children[1]);

            return;
        }

        // if opaque leaf, don't add
        if (!node.opaque) {
            if (NOT(side.visibleHull)) {
                side.visibleHull = w.Copy();
            } else {
                side.visibleHull.AddToConvexHull(w, dmapGlobals.mapPlanes.oGet(side.planenum).Normal());
            }
        }

        //	delete w;
//        return;
    }


    /*
     =====================
     ClipSidesByTree

     Creates side.visibleHull for all visible sides

     The visible hull for a side will consist of the convex hull of
     all points in non-opaque clusters, which allows overlaps
     to be trimmed off automatically.
     =====================
     */
    static void ClipSidesByTree(uEntity_t e) {
        uBrush_t b;
        int i;
        idWinding w;
        side_s side;
        primitive_s prim;

        common.Printf("----- ClipSidesByTree -----\n");

        for (prim = e.primitives; prim != null; prim = prim.next) {
            b = (uBrush_t) prim.brush;
            if (NOT(b)) {
                // FIXME: other primitives!
                continue;
            }
            for (i = 0; i < b.numsides; i++) {
                side = b.sides[i];
                if (NOT(side.winding)) {
                    continue;
                }
                w = side.winding.Copy();
                side.visibleHull = null;
                ClipSideByTree_r(w, side, e.tree.headnode);
                // for debugging, we can choose to use the entire original side
                // but we skip this if the side was completely clipped away
                if (side.visibleHull != null && dmapGlobals.noClipSides) {
                    //				delete side.visibleHull;
                    side.visibleHull = side.winding.Copy();
                }
            }
        }
    }

    //=================================================================================

    /*
     ====================
     ClipTriIntoTree_r

     This is used for adding curve triangles
     The winding will be freed before it returns
     ====================
     */
    static void ClipTriIntoTree_r(idWinding w, mapTri_s originalTri, uEntity_t e, node_s node) {
        idWinding front = new idWinding(), back = new idWinding();

        if (NOT(w)) {
            return;
        }

        if (node.planenum != PLANENUM_LEAF) {
            w.Split(dmapGlobals.mapPlanes.oGet(node.planenum), ON_EPSILON, front, back);
            //		delete w;

            ClipTriIntoTree_r(front, originalTri, e, node.children[0]);
            ClipTriIntoTree_r(back, originalTri, e, node.children[1]);

            return;
        }

        // if opaque leaf, don't add
        if (!node.opaque && node.area >= 0) {
            mapTri_s list;
            int planeNum;
            idPlane plane = new idPlane();
            textureVectors_t texVec = new textureVectors_t();

            list = WindingToTriList(w, originalTri);

            PlaneForTri(originalTri, plane);
            planeNum = FindFloatPlane(plane);

            TexVecForTri(texVec, originalTri);

            AddTriListToArea(e, list, planeNum, node.area, texVec);
        }

        //	delete w;
//        return;
    }

    //=============================================================

    /*
     ====================
     CheckWindingInAreas_r

     Returns the area number that the winding is in, or
     -2 if it crosses multiple areas.

     ====================
     */
    static int CheckWindingInAreas_r(final idWinding w, node_s node) {
        idWinding front = new idWinding(), back = new idWinding();

        if (NOT(w)) {
            return -1;
        }

        if (node.planenum != PLANENUM_LEAF) {
            int a1, a2;
            //#if 0
            //		if ( side.planenum == node.planenum ) {
            //			return CheckWindingInAreas_r( w, node.children[0] );
            //		}
            //		if ( side.planenum == ( node.planenum ^ 1) ) {
            //			return CheckWindingInAreas_r( w, node.children[1] );
            //		}
            //#endif
            w.Split(dmapGlobals.mapPlanes.oGet(node.planenum), ON_EPSILON, front, back);

            a1 = CheckWindingInAreas_r(front, node.children[0]);
            //		delete front;
            a2 = CheckWindingInAreas_r(back, node.children[1]);
            //		delete back;

            if (a1 == -2 || a2 == -2) {
                return -2;	// different
            }
            if (a1 == -1) {
                return a2;	// one solid
            }
            if (a2 == -1) {
                return a1;	// one solid
            }

            if (a1 != a2) {
                return -2;	// cross areas
            }
            return a1;
        }

        return node.area;
    }

    /*
     ====================
     PutWindingIntoAreas_r

     Clips a winding down into the bsp tree, then converts
     the fragments to triangles and adds them to the area lists
     ====================
     */
    static void PutWindingIntoAreas_r(uEntity_t e, final idWinding w, side_s side, node_s node) {
        idWinding front = new idWinding(), back = new idWinding();
        int area;

        if (NOT(w)) {
            return;
        }

        if (node.planenum != PLANENUM_LEAF) {
            if (side.planenum == node.planenum) {
                PutWindingIntoAreas_r(e, w, side, node.children[0]);
                return;
            }
            if (side.planenum == (node.planenum ^ 1)) {
                PutWindingIntoAreas_r(e, w, side, node.children[1]);
                return;
            }

            // see if we need to split it
            // adding the "noFragment" flag to big surfaces like sky boxes
            // will avoid potentially dicing them up into tons of triangles
            // that take forever to optimize back together
            if (!dmapGlobals.fullCarve || side.material.NoFragment()) {
                area = CheckWindingInAreas_r(w, node);
                if (area >= 0) {
                    mapTri_s tri;

                    // put in single area
                    tri = TriListForSide(side, w);
                    AddTriListToArea(e, tri, side.planenum, area, side.texVec);
                    return;
                }
            }

            w.Split(dmapGlobals.mapPlanes.oGet(node.planenum), ON_EPSILON, front, back);

            PutWindingIntoAreas_r(e, front, side, node.children[0]);
            //		if ( front ) {
            //			delete front;
            //		}

            PutWindingIntoAreas_r(e, back, side, node.children[1]);
            //		if ( back ) {
            //			delete back;
            //		}

            return;
        }

        // if opaque leaf, don't add
        if (node.area >= 0 && !node.opaque) {
            mapTri_s tri;

            tri = TriListForSide(side, w);
            AddTriListToArea(e, tri, side.planenum, node.area, side.texVec);
        }
    }

    /*
     ==================
     AddMapTriToAreas

     Used for curves and inlined models
     ==================
     */
    static void AddMapTriToAreas(mapTri_s tri, uEntity_t e) {
        int area;
        idWinding w;

        // skip degenerate triangles from pinched curves
        if (MapTriArea(tri) <= 0) {
            return;
        }

        if (dmapGlobals.fullCarve) {
            // always fragment into areas
            w = WindingForTri(tri);
            ClipTriIntoTree_r(w, tri, e, e.tree.headnode);
            return;
        }

        w = WindingForTri(tri);
        area = CheckWindingInAreas_r(w, e.tree.headnode);
        //	delete w;
        if (area == -1) {
            return;
        }
        if (area >= 0) {
            mapTri_s newTri;
            idPlane plane = new idPlane();
            int planeNum;
            textureVectors_t texVec = new textureVectors_t();

            // put in single area
            newTri = CopyMapTri(tri);
            newTri.next = null;

            PlaneForTri(tri, plane);
            planeNum = FindFloatPlane(plane);

            TexVecForTri(texVec, newTri);

            AddTriListToArea(e, newTri, planeNum, area, texVec);
        } else {
            // fragment into areas
            w = WindingForTri(tri);
            ClipTriIntoTree_r(w, tri, e, e.tree.headnode);
        }
    }

    /*
     =====================
     PutPrimitivesInAreas

     =====================
     */
    static void PutPrimitivesInAreas(uEntity_t e) {
        uBrush_t b;
        int i;
        side_s side;
        primitive_s prim;
        mapTri_s tri;

        common.Printf("----- PutPrimitivesInAreas -----\n");

        // allocate space for surface chains for each area
        e.areas = new uArea_t[e.numAreas];// Mem_Alloc(e.numAreas);
        //	memset( e.areas, 0, e.numAreas * sizeof( e.areas[0] ) );

        // for each primitive, clip it to the non-solid leafs
        // and divide it into different areas
        for (prim = e.primitives; prim != null; prim = prim.next) {
            b = (uBrush_t) prim.brush;

            if (NOT(b)) {
                // add curve triangles
                for (tri = prim.tris; tri != null; tri = tri.next) {
                    AddMapTriToAreas(tri, e);
                }
                continue;
            }

            // clip in brush sides
            for (i = 0; i < b.numsides; i++) {
                side = b.sides[i];
                if (NOT(side.visibleHull)) {
                    continue;
                }
                PutWindingIntoAreas_r(e, side.visibleHull, side, e.tree.headnode);
            }
        }

        // optionally inline some of the func_static models
        if (dmapGlobals.entityNum == 0) {
            boolean inlineAll = dmapGlobals.uEntities[0].mapEntity.epairs.GetBool("inlineAllStatics");

            for (int eNum = 1; eNum < dmapGlobals.num_entities; eNum++) {
                uEntity_t entity = dmapGlobals.uEntities[eNum];
                final String className = entity.mapEntity.epairs.GetString("classname");
                if (idStr.Icmp(className, "func_static") != 0) {
                    continue;
                }
                if (!entity.mapEntity.epairs.GetBool("inline") && !inlineAll) {
                    continue;
                }
                final String modelName = entity.mapEntity.epairs.GetString("model");
                if (isNotNullOrEmpty(modelName)) {
                    continue;
                }
                idRenderModel model = renderModelManager.FindModel(modelName);

                common.Printf("inlining %s.\n", entity.mapEntity.epairs.GetString("name"));

                idMat3 axis = new idMat3();
                // get the rotation matrix in either full form, or single angle form
                if (!entity.mapEntity.epairs.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", axis)) {
                    float angle = entity.mapEntity.epairs.GetFloat("angle");
                    if (angle != 0.0f) {
                        axis = new idAngles(0.0f, angle, 0.0f).ToMat3();
                    } else {
                        axis.Identity();
                    }
                }

                idVec3 origin = entity.mapEntity.epairs.GetVector("origin");

                for (i = 0; i < model.NumSurfaces(); i++) {
                    final modelSurface_s surface = model.Surface(i);
                    final srfTriangles_s tri2 = surface.geometry;

                    mapTri_s mapTri = new mapTri_s();
                    //				memset( &mapTri, 0, sizeof( mapTri ) );
                    mapTri.material = surface.shader;
                    // don't let discretes (autosprites, etc) merge together
                    if (mapTri.material.IsDiscrete()) {
                        mapTri.mergeGroup = surface;
                    }
                    for (int j = 0; j < tri2.numIndexes; j += 3) {
                        for (int k = 0; k < 3; k++) {
                            idVec3 v = tri2.verts[tri2.indexes[j + k]].xyz;

                            mapTri.v[k].xyz = v.oMultiply(axis).oPlus(origin);

                            mapTri.v[k].normal = tri2.verts[tri2.indexes[j + k]].normal.oMultiply(axis);
                            mapTri.v[k].st = tri2.verts[tri2.indexes[j + k]].st;
                        }
                        AddMapTriToAreas(mapTri, e);
                    }
                }
            }
        }
    }

    //============================================================================

    /*
     =================
     ClipTriByLight

     Carves a triangle by the frustom planes of a light, producing
     a (possibly empty) list of triangles on the inside and outside.

     The original triangle is not modified.

     If no clipping is required, the result will be a copy of the original.

     If clipping was required, the outside fragments will be planar clips, which
     will benefit from re-optimization.
     =================
     */
    static void ClipTriByLight(final mapLight_t light, final mapTri_s tri, mapTri_s in, mapTri_s out) {
        idWinding inside, oldInside;
        idWinding[] outside = new idWinding[6];
        boolean hasOutside;
        int i;

//        in[0] = null;
//        out[0] = null;
        // clip this winding to the light
        inside = WindingForTri(tri);
        hasOutside = false;
        for (i = 0; i < 6; i++) {
            oldInside = inside;
            if (oldInside != null) {
                oldInside.Split(light.def.frustum[i], 0, outside[i], inside);
                //			delete oldInside;
            } else {
                outside[i] = null;
            }
            if (outside[i] != null) {
                hasOutside = true;
            }
        }

        if (NOT(inside)) {
            // the entire winding is outside this light

            // free the clipped fragments
            for (i = 0; i < 6; i++) {
                if (outside[i] != null) {
                    //				delete outside[i];
                    outside[i] = null;
                }
            }

            out.oSet(CopyMapTri(tri));
            out.next = null;

            return;
        }

        if (!hasOutside) {
            // the entire winding is inside this light

            // free the inside copy
            //		delete inside;
            inside = null;

            in.oSet(CopyMapTri(tri));
            in.next = null;

            return;
        }

        // the winding is split
        in.oSet(WindingToTriList(inside, tri));
        //	delete inside;

        // combine all the outside fragments
        for (i = 0; i < 6; i++) {
            if (outside[i] != null) {
                mapTri_s list;

                list = WindingToTriList(outside[i], tri);
                //			delete outside[i];
                out.oSet(MergeTriLists(out, list));
            }
        }
    }

    /*
     =================
     BoundOptimizeGroup
     =================
     */
    static void BoundOptimizeGroup(optimizeGroup_s group) {
        group.bounds.Clear();
        for (mapTri_s tri = group.triList; tri != null; tri = tri.next) {
            group.bounds.AddPoint(tri.v[0].xyz);
            group.bounds.AddPoint(tri.v[1].xyz);
            group.bounds.AddPoint(tri.v[2].xyz);
        }
    }

    /*
     ====================
     BuildLightShadows

     Build the beam tree and shadow volume surface for a light
     ====================
     */
    static void BuildLightShadows(uEntity_t e, mapLight_t light) {
        int i;
        optimizeGroup_s group;
        mapTri_s tri;
        mapTri_s shadowers;
        optimizeGroup_s shadowerGroups;
        idVec3 lightOrigin;
        boolean hasPerforatedSurface = false;

        //
        // build a group list of all the triangles that will contribute to
        // the optimized shadow volume, leaving the original triangles alone
        //
        // shadowers will contain all the triangles that will contribute to the
        // shadow volume
        shadowerGroups = null;
        lightOrigin = new idVec3(light.def.globalLightOrigin);

        // if the light is no-shadows, don't add any surfaces
        // to the beam tree at all
        if (!light.def.parms.noShadows
                && light.def.lightShader.LightCastsShadows()) {
            for (i = 0; i < e.numAreas; i++) {
                for (group = e.areas[i].groups; group != null; group = group.nextGroup) {
                    // if the surface doesn't cast shadows, skip it
                    if (!group.material.SurfaceCastsShadow()) {
                        continue;
                    }

                    // if the group doesn't face away from the light, it
                    // won't contribute to the shadow volume
                    if (dmapGlobals.mapPlanes.oGet(group.planeNum).Distance(lightOrigin) > 0) {
                        continue;
                    }

                    // if the group bounds doesn't intersect the light bounds,
                    // skip it
                    if (!group.bounds.IntersectsBounds(light.def.frustumTris.bounds)) {
                        continue;
                    }

                    // build up a list of the triangle fragments inside the
                    // light frustum
                    shadowers = null;
                    for (tri = group.triList; tri != null; tri = tri.next) {
                        mapTri_s in = new mapTri_s(), out = new mapTri_s();

                        // clip it to the light frustum
                        ClipTriByLight(light, tri, in, out);
                        FreeTriList(out);
                        shadowers = MergeTriLists(shadowers, in);
                    }

                    // if we didn't get any out of this group, we don't
                    // need to create a new group in the shadower list
                    if (NOT(shadowers)) {
                        continue;
                    }

                    // find a group in shadowerGroups to add these to
                    // we will ignore everything but planenum, and we
                    // can merge across areas
                    optimizeGroup_s check;

                    for (check = shadowerGroups; check != null; check = check.nextGroup) {
                        if (check.planeNum == group.planeNum) {
                            break;
                        }
                    }
                    if (NOT(check)) {
//                        check = (optimizeGroup_s) Mem_Alloc(sizeof(check));
                        check = group;
                        check.triList = null;
                        check.nextGroup = shadowerGroups;
                        shadowerGroups = check;
                    }

                    // if any surface is a shadow-casting perforated or translucent surface, we
                    // can't use the face removal optimizations because we can see through
                    // some of the faces
                    if (group.material.Coverage() != MC_OPAQUE) {
                        hasPerforatedSurface = true;
                    }

                    check.triList = MergeTriLists(check.triList, shadowers);
                }
            }
        }

        // take the shadower group list and create a beam tree and shadow volume
        light.shadowTris = CreateLightShadow(shadowerGroups, light);

        if (light.shadowTris != null && hasPerforatedSurface) {
            // can't ever remove front faces, because we can see through some of them
            light.shadowTris.numShadowIndexesNoCaps = light.shadowTris.numShadowIndexesNoFrontCaps
                    = light.shadowTris.numIndexes;
        }

        // we don't need the original shadower triangles for anything else
        FreeOptimizeGroupList(shadowerGroups);
    }


    /*
     ====================
     CarveGroupsByLight

     Divide each group into an inside group and an outside group, based
     on which fragments are illuminated by the light's beam tree
     ====================
     */
    static void CarveGroupsByLight(uEntity_t e, mapLight_t light) {
        int i;
        optimizeGroup_s group, newGroup, carvedGroups, nextGroup;
        mapTri_s tri, inside, outside;
        uArea_t area;

        for (i = 0; i < e.numAreas; i++) {
            area = e.areas[i];
            carvedGroups = null;

            // we will be either freeing or reassigning the groups as we go
            for (group = area.groups; group != null; group = nextGroup) {
                nextGroup = group.nextGroup;
                // if the surface doesn't get lit, don't carve it up
                if ((light.def.lightShader.IsFogLight() && !group.material.ReceivesFog())
                        || (!light.def.lightShader.IsFogLight() && !group.material.ReceivesLighting())
                        || !group.bounds.IntersectsBounds(light.def.frustumTris.bounds)) {

                    group.nextGroup = carvedGroups;
                    carvedGroups = group;
                    continue;
                }

                if (group.numGroupLights == MAX_GROUP_LIGHTS) {
                    common.Error("MAX_GROUP_LIGHTS around %f %f %f",
                            group.triList.v[0].xyz.oGet(0), group.triList.v[0].xyz.oGet(1), group.triList.v[0].xyz.oGet(2));
                }

                // if the group doesn't face the light,
                // it won't get carved at all
                if (!light.def.lightShader.LightEffectsBackSides()
                        && !group.material.ReceivesLightingOnBackSides()
                        && dmapGlobals.mapPlanes.oGet(group.planeNum).Distance(light.def.parms.origin) <= 0) {

                    group.nextGroup = carvedGroups;
                    carvedGroups = group;
                    continue;
                }

                // split into lists for hit-by-light, and not-hit-by-light
                inside = null;
                outside = null;

                for (tri = group.triList; tri != null; tri = tri.next) {
                    mapTri_s in = new mapTri_s(), out = new mapTri_s();

                    ClipTriByLight(light, tri, in, out);
                    inside = MergeTriLists(inside, in);
                    outside = MergeTriLists(outside, out);
                }

                if (inside != null) {
//                    newGroup = (optimizeGroup_s) Mem_Alloc(sizeof(newGroup));
                    newGroup = group;
                    newGroup.groupLights[newGroup.numGroupLights] = light;
                    newGroup.numGroupLights++;
                    newGroup.triList = inside;
                    newGroup.nextGroup = carvedGroups;
                    carvedGroups = newGroup;
                }

                if (outside != null) {
//                    newGroup = (optimizeGroup_s) Mem_Alloc(sizeof(newGroup));
                    newGroup = group;
                    newGroup.triList = outside;
                    newGroup.nextGroup = carvedGroups;
                    carvedGroups = newGroup;
                }

                // free the original
                group.nextGroup = null;
                FreeOptimizeGroupList(group);
            }

            // replace this area's group list with the new one
            area.groups = carvedGroups;
        }
    }

    /*
     =====================
     Prelight

     Break optimize groups up into additional groups at light boundaries, so
     optimization won't cross light bounds
     =====================
     */
    static void Prelight(uEntity_t e) {
        int i;
        int start, end;
        mapLight_t light;

        // don't prelight anything but the world entity
        if (dmapGlobals.entityNum != 0) {
            return;
        }

        if (etoi(dmapGlobals.shadowOptLevel) > 0) {
            common.Printf("----- BuildLightShadows -----\n");
            start = Sys_Milliseconds();

            // calc bounds for all the groups to speed things up
            for (i = 0; i < e.numAreas; i++) {
                uArea_t area = e.areas[i];

                for (optimizeGroup_s group = area.groups; group != null; group = group.nextGroup) {
                    BoundOptimizeGroup(group);
                }
            }

            for (i = 0; i < dmapGlobals.mapLights.Num(); i++) {
                light = dmapGlobals.mapLights.oGet(i);
                BuildLightShadows(e, light);
            }

            end = Sys_Milliseconds();
            common.Printf("%5.1f seconds for BuildLightShadows\n", (end - start) / 1000.0);
        }

        if (!dmapGlobals.noLightCarve) {
            common.Printf("----- CarveGroupsByLight -----\n");
            start = Sys_Milliseconds();
            // now subdivide the optimize groups into additional groups for
            // each light that illuminates them
            for (i = 0; i < dmapGlobals.mapLights.Num(); i++) {
                light = dmapGlobals.mapLights.oGet(i);
                CarveGroupsByLight(e, light);
            }

            end = Sys_Milliseconds();
            common.Printf("%5.1f seconds for CarveGroupsByLight\n", (end - start) / 1000.0);
        }

    }
}
