package neo.Tools.Compilers.DMap;

import static neo.framework.Common.common;

import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.optimize.optVertex_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.DrawVert.idDrawVert;

/**
 *
 */
public class optimize_gcc {

    /*
     crazy gcc 3.3.5 optimization bug
     happens even at -O1
     if you remove the 'return NULL;' after Error(), it only happens at -O3 / release
     see dmap.gcc.zip test map and .proc outputs
     */
    public static idBounds optBounds;
    //
    static final int MAX_OPT_VERTEXES = 0x10000;
    public static int numOptVerts;
    public static final optVertex_s[] optVerts = new optVertex_s[MAX_OPT_VERTEXES];

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
            if (optVerts[i].pv.oGet(0) == x && optVerts[i].pv.oGet(1) == y) {
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
}
