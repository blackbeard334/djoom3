package neo.Tools.Compilers.DMap;

import static neo.TempDump.NOT;
import static neo.TempDump.fopenOptions;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.tree_s;
import neo.Tools.Compilers.DMap.dmap.uPortal_s;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class leakfile {

    /*
     ==============================================================================

     LEAF FILE GENERATION

     Save out name.line for qe3 to read
     ==============================================================================
     */
    /*
     =============
     LeakFile

     Finds the shortest possible chain of portals
     that leads from the outside leaf to a specifically
     occupied leaf
     =============
     */
    static void LeakFile(tree_s tree) {
        idVec3 mid = new idVec3();
//        FILE linefile;
        String filename;
        String ospath;
        byte[] fprintf;
        node_s node;
        int count;

        if (NOT(tree.outside_node.occupied)) {
            return;
        }

        common.Printf("--- LeakFile ---\n");

        //
        // write the points to the file
        //
        filename = String.format("%s.lin", dmapGlobals.mapFileBase);
        ospath = fileSystem.RelativePathToOSPath(filename);
        try (FileChannel linefile = FileChannel.open(Paths.get(ospath), fopenOptions("w"))) {
//             linefile = fopen(ospath, "w");
            if (NOT(linefile)) {
                common.Error("Couldn't open %s\n", filename);
            }

            count = 0;
            node = tree.outside_node;
            while (node.occupied > 1) {
                int next;
                uPortal_s p, nextportal = new uPortal_s();
                node_s nextnode = new node_s();
                int s;

                // find the best portal exit
                next = node.occupied;
                for (p = node.portals; p != null; p = p.next[/*!s*/1 ^ s]) {
                    s = (p.nodes[0].equals(node)) ? 1 : 0;
                    if ((p.nodes[s].occupied != 0)
                            && (p.nodes[s].occupied < next)) {
                        nextportal = p;
                        nextnode = p.nodes[s];
                        next = nextnode.occupied;
                    }
                }
                node = nextnode;
                mid = nextportal.winding.GetCenter();
                fprintf = String.format("%f %f %f\n", mid.oGet(0), mid.oGet(1), mid.oGet(2)).getBytes();
                linefile.write(ByteBuffer.wrap(fprintf));
                count++;
            }
            // add the occupant center
            node.occupant.mapEntity.epairs.GetVector("origin", "", mid);

            fprintf = String.format("%f %f %f\n", mid.oGet(0), mid.oGet(1), mid.oGet(2)).getBytes();
            linefile.write(ByteBuffer.wrap(fprintf));
            common.Printf("%5d point linefile\n", count + 1);

//             fclose(linefile);
        } catch (final IOException ex) {
            Logger.getLogger(leakfile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
