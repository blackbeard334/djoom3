package neo.Tools.Compilers.DMap;

import static neo.TempDump.NOT;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;

import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.tree_s;
import neo.Tools.Compilers.DMap.dmap.uPortal_s;
import neo.framework.File_h.idFile;
import neo.idlib.geometry.Winding.idWinding;

/**
 *
 */
public class glfile {

    static int c_glfaces;

    static int PortalVisibleSides(uPortal_s p) {
        boolean fcon, bcon;

        if (NOT(p.onnode)) {
            return 0;		// outside
        }
        fcon = p.nodes[0].opaque;
        bcon = p.nodes[1].opaque;

        // same contents never create a face
        if (fcon == bcon) {
            return 0;
        }

        if (!fcon) {
            return 1;
        }
        if (!bcon) {
            return 2;
        }
        return 0;
    }
    private static int level = 128;

    static void OutputWinding(idWinding w, idFile glview) {
        float light;
        int i;

        glview.WriteFloatString("%d\n", w.GetNumPoints());
        level += 28;
        light = (level & 255) / 255.0f;
        for (i = 0; i < w.GetNumPoints(); i++) {
            glview.WriteFloatString("%6.3f %6.3f %6.3f %6.3f %6.3f %6.3f\n",
                    w.oGet(i, 0),
                    w.oGet(i, 1),
                    w.oGet(i, 2),
                    light,
                    light,
                    light);
        }
        glview.WriteFloatString("\n");
    }

    /*
     =============
     OutputPortal
     =============
     */
    static void OutputPortal(uPortal_s p, idFile glview) {
        idWinding w;
        int sides;

        sides = PortalVisibleSides(p);
        if (0 == sides) {
            return;
        }

        c_glfaces++;

        w = p.winding;

        if (sides == 2) {		// back side
            w = w.Reverse();
        }

        OutputWinding(w, glview);

//	if ( sides == 2 ) {
//		delete w;
//	}
    }

    /*
     =============
     WriteGLView_r
     =============
     */
    static void WriteGLView_r(node_s node, idFile glview) {
        uPortal_s p, nextp;

        if (node.planenum != PLANENUM_LEAF) {
            WriteGLView_r(node.children[0], glview);
            WriteGLView_r(node.children[1], glview);
            return;
        }

        // write all the portals
        for (p = node.portals; p != null; p = nextp) {
            if (p.nodes[0] == node) {
                OutputPortal(p, glview);
                nextp = p.next[0];
            } else {
                nextp = p.next[1];
            }
        }
    }

    /*
     =============
     WriteGLView
     =============
     */
    static void WriteGLView(tree_s tree, String source) {
        idFile glview;

        c_glfaces = 0;
        common.Printf("Writing %s\n", source);

        glview = fileSystem.OpenExplicitFileWrite(source);
        if (NOT(glview)) {
            common.Error("Couldn't open %s", source);
        }
        WriteGLView_r(tree.headnode, glview);
        fileSystem.CloseFile(glview);

        common.Printf("%5d c_glfaces\n", c_glfaces);
    }
}
