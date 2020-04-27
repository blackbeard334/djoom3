package neo.Game;

import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Pvs.pvsType_t.PVS_ALL_PORTALS_OPEN;
import static neo.Game.Pvs.pvsType_t.PVS_CONNECTED_AREAS;
import static neo.Game.Pvs.pvsType_t.PVS_NORMAL;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_VIEW;
import static neo.TempDump.indexOf;
import static neo.TempDump.memcmp;
import static neo.TempDump.reinterpret_cast_long_array;
import static neo.framework.Common.common;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;

import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.Renderer.RenderWorld.exitPortal_t;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Timer.idTimer;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Pvs {

    /*
     ===================================================================================

     PVS

     Note: mirrors and other special view portals are not taken into account

     ===================================================================================
     */
    public static final int MAX_BOUNDS_AREAS    = 16;
    public static final int MAX_CURRENT_PVS     = 8;		// must be a power of 2

    public static class pvsHandle_t {

        int i;			// index to current pvs
	/*unsigned */
        int h;			// handle for current pvs
    };

    public static class pvsCurrent_t {

        pvsHandle_t handle;	// current pvs handle
        byte[] pvs;		    // current pvs bit string

        public pvsCurrent_t() {
            this.handle = new pvsHandle_t();
        }
    };

    public enum pvsType_t {

        PVS_NORMAL,//= 0,               // PVS through portals taking portal states into account
        PVS_ALL_PORTALS_OPEN,//	= 1,	// PVS through portals assuming all portals are open
        PVS_CONNECTED_AREAS,//	= 2	    // PVS considering all topologically connected areas visible
    };

    public static class pvsPassage_t {

        byte[] canSee;		// bit set for all portals that can be seen through this passage
    };

    public static class pvsPortal_t {

        int            areaNum;  // area this portal leads to
        idWinding      w;        // winding goes counter clockwise seen from the area this portal is part of
        idBounds       bounds;   // winding bounds
        idPlane        plane;    // winding plane, normal points towards the area this portal leads to
        pvsPassage_t[] passages; // passages to portals in the area this portal leads to
        boolean        done;     // true if pvs is calculated for this portal
        byte[]         vis;      // PVS for this portal
        byte[]         mightSee; // used during construction
        
        public pvsPortal_t(){
            bounds = new idBounds();
            plane = new idPlane();
        }
    }

    ;

    public static class pvsArea_t {

        int           numPortals;// number of portals in this area
        idBounds      bounds;    // bounds of the whole area
        pvsPortal_t[] portals;   // array with pointers to the portals of this area

        public pvsArea_t() {
            bounds = new idBounds();
        }
    };

    public static class pvsStack_t {

        pvsStack_t next;        // next stack entry
        byte[]     mightSee;    // bit set for all portals that might be visible through this passage/portal stack
    };

    public static class idPVS {

        private int       numAreas;
        private int       numPortals;
        private boolean[] connectedAreas;
        private int[]     areaQueue;
        private byte[]    areaPVS;
        // current PVS for a specific source possibly taking portal states (open/closed) into account
        private pvsCurrent_t[] currentPVS = new pvsCurrent_t[MAX_CURRENT_PVS];
        // used to create PVS
        private int           portalVisBytes;
        private int           portalVisLongs;
        private int           areaVisBytes;
        private int           areaVisLongs;
        private pvsPortal_t[] pvsPortals;
        private pvsArea_t[]   pvsAreas;
        //
        //

        public idPVS() {
            int i;

            numAreas = 0;
            numPortals = 0;

            connectedAreas = null;
            areaQueue = null;
            areaPVS = null;

            for (i = 0; i < MAX_CURRENT_PVS; i++) {
                currentPVS[i] = new pvsCurrent_t();
                currentPVS[i].handle.i = -1;
                currentPVS[i].handle.h = 0;
                currentPVS[i].pvs = null;
            }

            pvsAreas = null;
            pvsPortals = null;
        }
        // ~idPVS( void );

        // setup for the current map
        public void Init() {
            int totalVisibleAreas;

            Shutdown();

            numAreas = gameRenderWorld.NumAreas();
            if (numAreas <= 0) {
                return;
            }

            connectedAreas = new boolean[numAreas];
            areaQueue = new int[numAreas];

            areaVisBytes = (((numAreas + 31) & ~31) >> 3);
            areaVisLongs = areaVisBytes / Long.BYTES;

            areaPVS = new byte[numAreas * areaVisBytes];//	memset( areaPVS, 0xFF, numAreas * areaVisBytes );
            Arrays.fill(areaPVS, 0, numAreas * areaVisBytes, (byte) 0xFF);

            numPortals = GetPortalCount();

            portalVisBytes = (((numPortals + 31) & ~31) >> 3);
            portalVisLongs = portalVisBytes / Long.BYTES;

            for (int i = 0; i < MAX_CURRENT_PVS; i++) {
                currentPVS[i].handle.i = -1;
                currentPVS[i].handle.h = 0;
                currentPVS[i].pvs = new byte[areaVisBytes];//memset( currentPVS[i].pvs, 0, areaVisBytes );
            }

            idTimer timer = new idTimer();
            timer.Start();

            CreatePVSData();

            FrontPortalPVS();

            CopyPortalPVSToMightSee();

            PassagePVS();

            totalVisibleAreas = AreaPVSFromPortalPVS();

            DestroyPVSData();

            timer.Stop();

            gameLocal.Printf("%5.0f msec to calculate PVS\n", timer.Milliseconds());
            gameLocal.Printf("%5d areas\n", numAreas);
            gameLocal.Printf("%5d portals\n", numPortals);
            gameLocal.Printf("%5d areas visible on average\n", totalVisibleAreas / numAreas);
            if (numAreas * areaVisBytes < 1024) {
                gameLocal.Printf("%5d bytes PVS data\n", numAreas * areaVisBytes);
            } else {
                gameLocal.Printf("%5d KB PVS data\n", (numAreas * areaVisBytes) >> 10);
            }
        }

        public void Shutdown() {
            if (connectedAreas != null) {
//		delete connectedAreas;
                connectedAreas = null;
            }
            if (areaQueue != null) {
//		delete areaQueue;
                areaQueue = null;
            }
            if (areaPVS != null) {
//		delete areaPVS;
                areaPVS = null;
            }
            if (currentPVS != null) {
                for (int i = 0; i < MAX_CURRENT_PVS; i++) {
//			delete currentPVS[i].pvs;
                    currentPVS[i].pvs = null;
                }
            }
        }

        // get the area(s) the source is in
        public int GetPVSArea(final idVec3 point) {		// returns the area number
            return gameRenderWorld.PointInArea(point);
        }

        public int GetPVSAreas(final idBounds bounds, int[] areas, int maxAreas) {	// returns number of areas
            return gameRenderWorld.BoundsInAreas(bounds, areas, maxAreas);
        }

        // setup current PVS for the source
        public pvsHandle_t SetupCurrentPVS(final idVec3 source, final pvsType_t type /*= PVS_NORMAL*/) {
            int sourceArea;

            sourceArea = gameRenderWorld.PointInArea(source);

            return SetupCurrentPVS(sourceArea, type);
        }

        public pvsHandle_t SetupCurrentPVS(final idBounds source, final pvsType_t type /*= PVS_NORMAL*/) {
            int numSourceAreas;
            int[] sourceAreas = new int[MAX_BOUNDS_AREAS];

            numSourceAreas = gameRenderWorld.BoundsInAreas(source, sourceAreas, MAX_BOUNDS_AREAS);

            return SetupCurrentPVS(sourceAreas, numSourceAreas, type);
        }

        public pvsHandle_t SetupCurrentPVS(final int sourceArea, final pvsType_t type /*= PVS_NORMAL*/) {
            int i;
            pvsHandle_t handle;

            handle = AllocCurrentPVS( /*reinterpret_cast<const unsigned int *>*/(sourceArea));

            if (sourceArea < 0 || sourceArea >= numAreas) {
//		memset( currentPVS[handle.i].pvs, 0, areaVisBytes );
                Arrays.fill(currentPVS[handle.i].pvs, 0, areaVisBytes, (byte) 0);
                return handle;
            }

            if (type != PVS_CONNECTED_AREAS) {
//		memcpy( currentPVS[handle.i].pvs, areaPVS + sourceArea * areaVisBytes, areaVisBytes );
                System.arraycopy(areaPVS, sourceArea * areaVisBytes, currentPVS[handle.i].pvs, 0, areaVisBytes);
            } else {
//		memset( currentPVS[handle.i].pvs, -1, areaVisBytes );
                Arrays.fill(currentPVS[handle.i].pvs, 0, areaVisBytes, (byte) -1);
            }

            if (type == PVS_ALL_PORTALS_OPEN) {
                return handle;
            }

//	memset( connectedAreas, 0, numAreas * sizeof( *connectedAreas ) );
            Arrays.fill(connectedAreas, 0, numAreas, false);

            GetConnectedAreas(sourceArea, connectedAreas);

            for (i = 0; i < numAreas; i++) {
                if (!connectedAreas[i]) {
                    currentPVS[handle.i].pvs[i >> 3] &= ~(1 << (i & 7));
                }
            }

            return handle;
        }

        public pvsHandle_t SetupCurrentPVS(final int sourceArea) {
            return this.SetupCurrentPVS(sourceArea, PVS_NORMAL);
        }

        public pvsHandle_t SetupCurrentPVS(final int[] sourceAreas, final int numSourceAreas, final pvsType_t type /*= PVS_NORMAL*/) {
            int i, j;
            /*unsigned*/ int h;
            long[] vis, pvs;
            pvsHandle_t handle;

            h = 0;
            for (i = 0; i < numSourceAreas; i++) {
                h
                        ^= /**
                         * reinterpret_cast<const unsigned int *>
                         */
                        (sourceAreas[i]);
            }
            handle = AllocCurrentPVS(h);

            if (0 == numSourceAreas || sourceAreas[0] < 0 || sourceAreas[0] >= numAreas) {
                Arrays.fill(currentPVS[handle.i].pvs, 0, areaVisBytes, (byte) 0);//memset(currentPVS[handle.i].pvs, 0, areaVisBytes);
                return handle;
            }

            if (type != PVS_CONNECTED_AREAS) {
                // merge PVS of all areas the source is in
                System.arraycopy(areaPVS, sourceAreas[0] * areaVisBytes, currentPVS[handle.i].pvs, 0, areaVisBytes);//		memcpy( currentPVS[handle.i].pvs, areaPVS + sourceAreas[0] * areaVisBytes, areaVisBytes );
                for (i = 1; i < numSourceAreas; i++) {

                    assert (sourceAreas[i] >= 0 && sourceAreas[i] < numAreas);

                    final int vOffset = sourceAreas[i] * areaVisBytes / Long.BYTES;
                    vis = reinterpret_cast_long_array(areaPVS);
                    pvs = reinterpret_cast_long_array(currentPVS[handle.i].pvs);
                    for (j = 0; j < areaVisLongs; j++) {
                        pvs[j] |= vis[j + vOffset];
                    }
                }
            } else {
                Arrays.fill(currentPVS[handle.i].pvs, 0, areaVisBytes, (byte) -1);//memset( currentPVS[handle.i].pvs, -1, areaVisBytes );
            }

            if (type == PVS_ALL_PORTALS_OPEN) {
                return handle;
            }

            Arrays.fill(connectedAreas, 0, numAreas, false);//memset( connectedAreas, 0, numAreas * sizeof( *connectedAreas ) );

            // get all areas connected to any of the source areas
            for (i = 0; i < numSourceAreas; i++) {
                if (!connectedAreas[sourceAreas[i]]) {
                    GetConnectedAreas(sourceAreas[i], connectedAreas);
                }
            }

            // remove unconnected areas from the PVS
            for (i = 0; i < numAreas; i++) {
                if (!connectedAreas[i]) {
                    currentPVS[handle.i].pvs[i >> 3] &= ~(1 << (i & 7));
                }
            }

            return handle;
        }

        public pvsHandle_t SetupCurrentPVS(final int[] sourceAreas, final int numSourceAreas) {
            return SetupCurrentPVS(sourceAreas, numSourceAreas, PVS_NORMAL);
        }

        public pvsHandle_t MergeCurrentPVS(pvsHandle_t pvs1, pvsHandle_t pvs2) {
            int i;
            long[] pvs1Ptr, pvs2Ptr, ptr;
            pvsHandle_t handle;

            if (pvs1.i < 0 || pvs1.i >= MAX_CURRENT_PVS || pvs1.h != currentPVS[pvs1.i].handle.h
                    || pvs2.i < 0 || pvs2.i >= MAX_CURRENT_PVS || pvs2.h != currentPVS[pvs2.i].handle.h) {
                gameLocal.Error("idPVS::MergeCurrentPVS: invalid handle");
            }

            handle = AllocCurrentPVS(pvs1.h ^ pvs2.h);

            ptr = reinterpret_cast_long_array(currentPVS[handle.i].pvs);
            pvs1Ptr = reinterpret_cast_long_array(currentPVS[pvs1.i].pvs);
            pvs2Ptr = reinterpret_cast_long_array(currentPVS[pvs2.i].pvs);

            for (i = 0; i < areaVisLongs; i++) {
                ptr[i] = pvs1Ptr[i] | pvs2Ptr[i];
            }

            return handle;
        }

        public void FreeCurrentPVS(pvsHandle_t handle) {
            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::FreeCurrentPVS: invalid handle");
            }
            currentPVS[handle.i].handle.i = -1;
        }

        // returns true if the target is within the current PVS
        public boolean InCurrentPVS(final pvsHandle_t handle, final idVec3 target) {
            int targetArea;

            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS
                    || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::InCurrentPVS: invalid handle");
            }

            targetArea = gameRenderWorld.PointInArea(target);

            if (targetArea == -1) {
                return false;
            }

            return ((currentPVS[handle.i].pvs[targetArea >> 3] & (1 << (targetArea & 7))) != 0);
        }

        public boolean InCurrentPVS(final pvsHandle_t handle, final idBounds target) {
            int i, numTargetAreas;
            int[] targetAreas = new int[MAX_BOUNDS_AREAS];

            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS
                    || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::InCurrentPVS: invalid handle");
            }

            numTargetAreas = gameRenderWorld.BoundsInAreas(target, targetAreas, MAX_BOUNDS_AREAS);

            for (i = 0; i < numTargetAreas; i++) {
                if ((currentPVS[handle.i].pvs[targetAreas[i] >> 3] & (1 << (targetAreas[i] & 7))) != 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean InCurrentPVS(final pvsHandle_t handle, final int targetArea) {

            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS
                    || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::InCurrentPVS: invalid handle");
            }

            if (targetArea < 0 || targetArea >= numAreas) {
                return false;
            }

            return ((currentPVS[handle.i].pvs[targetArea >> 3] & (1 << (targetArea & 7))) != 0);
        }

        public boolean InCurrentPVS(final pvsHandle_t handle, final int[] targetAreas, int numTargetAreas) {
            int i;

            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS
                    || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::InCurrentPVS: invalid handle");
            }

            for (i = 0; i < numTargetAreas; i++) {
                if (targetAreas[i] < 0 || targetAreas[i] >= numAreas) {
                    continue;
                }
                if ((currentPVS[handle.i].pvs[targetAreas[i] >> 3] & (1 << (targetAreas[i] & 7))) != 0) {
                    return true;
                }
            }
            return false;
        }

        // draw all portals that are within the PVS of the source
        public void DrawPVS(final idVec3 source, final pvsType_t type /*= PVS_NORMAL*/) {
            int i, j, k, numPoints, n, sourceArea;
            exitPortal_t portal;
            idPlane plane = new idPlane();
            idVec3 offset;
            idVec4 color;
            pvsHandle_t handle;

            sourceArea = gameRenderWorld.PointInArea(source);

            if (sourceArea == -1) {
                return;
            }

            handle = SetupCurrentPVS(source, type);

            for (j = 0; j < numAreas; j++) {

                if (0 == (currentPVS[handle.i].pvs[j >> 3] & (1 << (j & 7)))) {
                    continue;
                }

                if (j == sourceArea) {
                    color = colorRed;
                } else {
                    color = colorCyan;
                }

                n = gameRenderWorld.NumPortalsInArea(j);

                // draw all the portals of the area
                for (i = 0; i < n; i++) {
                    portal = gameRenderWorld.GetPortal(j, i);

                    numPoints = portal.w.GetNumPoints();

                    portal.w.GetPlane(plane);
                    offset = plane.Normal().oMultiply(4.0f);
                    for (k = 0; k < numPoints; k++) {
                        gameRenderWorld.DebugLine(color, portal.w.oGet(k).ToVec3().oPlus(offset), portal.w.oGet((k + 1) % numPoints).ToVec3().oPlus(offset));
                    }
                }
            }

            FreeCurrentPVS(handle);
        }

        public void DrawPVS(final idBounds source, final pvsType_t type /*= PVS_NORMAL*/) {
            int i, j, k, numPoints, n, num;
            int[] areas = new int[MAX_BOUNDS_AREAS];
            exitPortal_t portal;
            idPlane plane = new idPlane();
            idVec3 offset;
            idVec4 color;
            pvsHandle_t handle;

            num = gameRenderWorld.BoundsInAreas(source, areas, MAX_BOUNDS_AREAS);

            if (0 == num) {
                return;
            }

            handle = SetupCurrentPVS(source, type);

            for (j = 0; j < numAreas; j++) {

                if (0 == (currentPVS[handle.i].pvs[j >> 3] & (1 << (j & 7)))) {
                    continue;
                }

                for (i = 0; i < num; i++) {
                    if (j == areas[i]) {
                        break;
                    }
                }
                if (i < num) {
                    color = colorRed;
                } else {
                    color = colorCyan;
                }

                n = gameRenderWorld.NumPortalsInArea(j);

                // draw all the portals of the area
                for (i = 0; i < n; i++) {
                    portal = gameRenderWorld.GetPortal(j, i);

                    numPoints = portal.w.GetNumPoints();

                    portal.w.GetPlane(plane);
                    offset = plane.Normal().oMultiply(4.0f);
                    for (k = 0; k < numPoints; k++) {
                        gameRenderWorld.DebugLine(color, portal.w.oGet(k).ToVec3().oPlus(offset), portal.w.oGet((k + 1) % numPoints).ToVec3().oPlus(offset));
                    }
                }
            }

            FreeCurrentPVS(handle);
        }

        // visualize the PVS the handle points to
        public void DrawCurrentPVS(final pvsHandle_t handle, final idVec3 source) {
            int i, j, k, numPoints, n, sourceArea;
            exitPortal_t portal;
            idPlane plane = new idPlane();
            idVec3 offset;
            idVec4 color;

            if (handle.i < 0 || handle.i >= MAX_CURRENT_PVS
                    || handle.h != currentPVS[handle.i].handle.h) {
                gameLocal.Error("idPVS::DrawCurrentPVS: invalid handle");
            }

            sourceArea = gameRenderWorld.PointInArea(source);

            if (sourceArea == -1) {
                return;
            }

            for (j = 0; j < numAreas; j++) {

                if (0 == (currentPVS[handle.i].pvs[j >> 3] & (1 << (j & 7)))) {
                    continue;
                }

                if (j == sourceArea) {
                    color = colorRed;
                } else {
                    color = colorCyan;
                }

                n = gameRenderWorld.NumPortalsInArea(j);

                // draw all the portals of the area
                for (i = 0; i < n; i++) {
                    portal = gameRenderWorld.GetPortal(j, i);

                    numPoints = portal.w.GetNumPoints();

                    portal.w.GetPlane(plane);
                    offset = plane.Normal().oMultiply(4.0f);
                    for (k = 0; k < numPoints; k++) {
                        gameRenderWorld.DebugLine(color, portal.w.oGet(k).ToVec3().oPlus(offset), portal.w.oGet((k + 1) % numPoints).ToVec3().oPlus(offset));
                    }
                }
            }
        }

// #if ASYNC_WRITE_PVS
        public void WritePVS(final pvsHandle_t handle, idBitMsg msg) {
            msg.WriteData(ByteBuffer.wrap(currentPVS[handle.i].pvs), areaVisBytes);
        }

        public void ReadPVS(final pvsHandle_t handle, final idBitMsg msg) {
            ByteBuffer l_pvs = ByteBuffer.allocate(256);
            int i;

            assert (areaVisBytes <= 256);
            msg.ReadData(l_pvs, areaVisBytes);
            if (memcmp(l_pvs.array(), currentPVS[handle.i].pvs, areaVisBytes)) {

                common.Printf("PVS not matching ( %d areaVisBytes ) - server then client:\n", areaVisBytes);
                for (i = 0; i < areaVisBytes; i++) {
                    common.Printf("%x ", l_pvs.get(i));
                }
                common.Printf("\n");
                for (i = 0; i < areaVisBytes; i++) {
                    common.Printf("%x ", currentPVS[ handle.i].pvs[ i]);
                }
                common.Printf("\n");
            }
        }
// #endif

        private int GetPortalCount() {
            int i, na, np;

            na = gameRenderWorld.NumAreas();
            np = 0;
            for (i = 0; i < na; i++) {
                np += gameRenderWorld.NumPortalsInArea(i);
            }
            return np;
        }

        private void CreatePVSData() {
            int i, j, n, cp;
            exitPortal_t portal;
            pvsArea_t area;
            pvsPortal_t p;
            pvsPortal_t[] portalPtrs;

            if (0 == numPortals) {
                return;
            }

            pvsPortals = new pvsPortal_t[numPortals];
            pvsAreas = new pvsArea_t[numAreas];
//	memset( pvsAreas, 0, numAreas * sizeof( *pvsAreas ) );

            cp = 0;
            portalPtrs = new pvsPortal_t[numPortals];

            for (i = 0; i < numAreas; i++) {

                area = pvsAreas[i] = new pvsArea_t();
                area.bounds.Clear();
//                area.portals = portalPtrs + cp;

                n = gameRenderWorld.NumPortalsInArea(i);

                for (j = 0; j < n; j++) {

                    portal = gameRenderWorld.GetPortal(i, j);

                    p = pvsPortals[cp++] = new pvsPortal_t();
                    // the winding goes counter clockwise seen from this area
                    p.w = portal.w.Copy();
                    p.areaNum = portal.areas[1];	// area[1] is always the area the portal leads to

                    p.vis = new byte[portalVisBytes];
                    p.mightSee = new byte[portalVisBytes];
                    p.w.GetBounds(p.bounds);
                    p.w.GetPlane(p.plane);
                    // plane normal points to outside the area
                    p.plane = p.plane.oNegative();
                    // no PVS calculated for this portal yet
                    p.done = false;

                    portalPtrs[area.numPortals++] = p;

                    area.bounds.oPluSet(p.bounds);
                }
                area.portals = portalPtrs;
            }
        }

        private void DestroyPVSData() {
//	int i;

            if (null == pvsAreas) {
                return;
            }

            // delete portal pointer array
//	delete[] pvsAreas[0].portals;
            // delete all areas
//	delete[] pvsAreas;
            pvsAreas = null;

            // delete portal data
//	for ( i = 0; i < numPortals; i++ ) {
//		delete[] pvsPortals[i].vis;
//		delete[] pvsPortals[i].mightSee;
//		delete pvsPortals[i].w;
//	}
            // delete portals
//	delete[] pvsPortals;
            pvsPortals = null;
        }

        private void CopyPortalPVSToMightSee() {
            int i;
            pvsPortal_t p;

            for (i = 0; i < numPortals; i++) {
                p = pvsPortals[i];
//		memcpy( p.mightSee, p.vis, portalVisBytes );
                System.arraycopy(p.vis, 0, p.mightSee, 0, portalVisBytes);
            }
        }

        private void FloodFrontPortalPVS_r(pvsPortal_t portal, int areaNum) {
            int i, n;
            pvsArea_t area;
            pvsPortal_t p;

            area = pvsAreas[ areaNum];

            for (i = 0; i < area.numPortals; i++) {
                p = area.portals[i];
                n = indexOf(p, pvsPortals);//TODO:very importante, what does thus do!?
                // don't flood through if this portal is not at the front
                if (0 == (portal.mightSee[ n >> 3] & (1 << (n & 7)))) {
                    continue;
                }
                // don't flood through if already visited this portal
                if ((portal.vis[ n >> 3] & (1 << (n & 7))) != 0) {
                    continue;
                }
                // this portal might be visible
                portal.vis[ n >> 3] |= (1 << (n & 7));
                // flood through the portal
                FloodFrontPortalPVS_r(portal, p.areaNum);
            }
        }

        private void FrontPortalPVS() {
            int i, j, k, n, p, side1, side2, areaSide;
            pvsPortal_t p1, p2;
            pvsArea_t area;

            for (i = 0; i < numPortals; i++) {
                p1 = pvsPortals[i];

                for (j = 0; j < numAreas; j++) {

                    area = pvsAreas[j];

                    areaSide = side1 = area.bounds.PlaneSide(p1.plane);

                    // if the whole area is at the back side of the portal
                    if (areaSide == PLANESIDE_BACK) {
                        continue;
                    }

                    for (p = 0; p < area.numPortals; p++) {

                        p2 = area.portals[p];

                        // if we the whole area is not at the front we need to check
                        if (areaSide != PLANESIDE_FRONT) {
                            // if the second portal is completely at the back side of the first portal
                            side1 = p2.bounds.PlaneSide(p1.plane);
                            if (side1 == PLANESIDE_BACK) {
                                continue;
                            }
                        }

                        // if the first portal is completely at the front of the second portal
                        side2 = p1.bounds.PlaneSide(p2.plane);
                        if (side2 == PLANESIDE_FRONT) {
                            continue;
                        }

                        // if the second portal is not completely at the front of the first portal
                        if (side1 != PLANESIDE_FRONT) {
                            // more accurate check
                            for (k = 0; k < p2.w.GetNumPoints(); k++) {
                                // if more than an epsilon at the front side
                                if (p1.plane.Side(p2.w.oGet(k).ToVec3(), ON_EPSILON) == PLANESIDE_FRONT) {
                                    break;
                                }
                            }
                            if (k >= p2.w.GetNumPoints()) {
                                continue;	// second portal is at the back of the first portal
                            }
                        }

                        // if the first portal is not completely at the back side of the second portal
                        if (side2 != PLANESIDE_BACK) {
                            // more accurate check
                            for (k = 0; k < p1.w.GetNumPoints(); k++) {
                                // if more than an epsilon at the back side
                                if (p2.plane.Side(p1.w.oGet(k).ToVec3(), ON_EPSILON) == PLANESIDE_BACK) {
                                    break;
                                }
                            }
                            if (k >= p1.w.GetNumPoints()) {
                                continue;	// first portal is at the front of the second portal
                            }
                        }

                        // the portal might be visible at the front
                        n = indexOf(p2, pvsPortals);
                        p1.mightSee[ n >> 3] |= 1 << (n & 7);
                    }
                }
            }

            // flood the front portal pvs for all portals
            for (i = 0; i < numPortals; i++) {
                p1 = pvsPortals[i];
                FloodFrontPortalPVS_r(p1, p1.areaNum);
            }
        }

        private pvsStack_t FloodPassagePVS_r(pvsPortal_t source, final pvsPortal_t portal, pvsStack_t prevStack) {
            int i, j, n;
            long m;
            pvsPortal_t p;
            pvsArea_t area;
            pvsStack_t stack;
            pvsPassage_t passage;
            long[] sourceVis, passageVis, portalVis, mightSee, prevMightSee;
            long more;

            area = pvsAreas[portal.areaNum];

            stack = prevStack.next;
            // if no next stack entry allocated
            if (null == stack) {
//		stack = reinterpret_cast<pvsStack_t*>(new byte[sizeof(pvsStack_t) + portalVisBytes]);
                stack = new pvsStack_t();
//		stack.mightSee = (reinterpret_cast<byte *>(stack)) + sizeof(pvsStack_t);TODO:check this..very importante
                stack.mightSee = new byte[portalVisBytes];
                stack.next = null;
                prevStack.next = stack;
            }

            // check all portals for flooding into other areas
            for (i = 0; i < area.numPortals; i++) {

                passage = portal.passages[i];

                // if this passage is completely empty
                if (null == passage.canSee) {
                    continue;
                }

                p = area.portals[i];
                n = indexOf(p, pvsPortals);

                // if this portal cannot be seen through our current portal/passage stack
                if (0 == (prevStack.mightSee[n >> 3] & (1 << (n & 7)))) {
                    continue;
                }

                // mark the portal as visible
                source.vis[n >> 3] |= (1 << (n & 7));

                // get pointers to vis data
                prevMightSee = reinterpret_cast_long_array(prevStack.mightSee);
                passageVis = reinterpret_cast_long_array(passage.canSee);
                sourceVis = reinterpret_cast_long_array(source.vis);
                mightSee = reinterpret_cast_long_array(stack.mightSee);

                more = 0;
                // use the portal PVS if it has been calculated
                if (p.done) {
                    portalVis = reinterpret_cast_long_array(p.vis);
                    for (j = 0; j < portalVisLongs; j++) {
                        // get new PVS which is decreased by going through this passage
                        m = prevMightSee[j] & passageVis[j] & portalVis[j];
                        // check if anything might be visible through this passage that wasn't yet visible
                        more |= (m & ~(sourceVis[j]));
                        // store new PVS
                        mightSee[j] = m;
                    }
                } else {
                    // the p.mightSee is implicitely stored in the passageVis
                    for (j = 0; j < portalVisLongs; j++) {
                        // get new PVS which is decreased by going through this passage
                        m = prevMightSee[j] & passageVis[j];
                        // check if anything might be visible through this passage that wasn't yet visible
                        more |= (m & ~(sourceVis[j]));
                        // store new PVS
                        mightSee[j] = m;
                    }
                }

                // if nothing more can be seen
                if (0 == more) {
                    continue;
                }

                // go through the portal
                stack.next = FloodPassagePVS_r(source, p, stack);
            }

            return stack;
        }

        private void PassagePVS() {
            int i;
            pvsPortal_t source;
            pvsStack_t stack, s;

            // create the passages
            CreatePassages();

            // allocate first stack entry
//	stack = reinterpret_cast<pvsStack_t*>(new byte[sizeof(pvsStack_t) + portalVisBytes]);
            stack = new pvsStack_t();
//	stack.mightSee = (reinterpret_cast<byte *>(stack)) + sizeof(pvsStack_t);
            stack.mightSee = new byte[portalVisBytes];
            stack.next = null;

            // calculate portal PVS by flooding through the passages
            for (i = 0; i < numPortals; i++) {
                source = pvsPortals[i];
                Arrays.fill(source.vis, 0, portalVisBytes, (byte) 0);
//		memcpy( stack.mightSee, source.mightSee, portalVisBytes );
                System.arraycopy(source.mightSee, 0, stack.mightSee, 0, portalVisBytes);
                FloodPassagePVS_r(source, source, stack);
                source.done = true;
            }

            // free the allocated stack
            for (s = stack; s != null; s = stack) {
                stack = stack.next;
//		delete[] s;
            }

            // destroy the passages
            DestroyPassages();
        }

        private void AddPassageBoundaries(final idWinding source, final idWinding pass, boolean flipClip, idPlane[] bounds, int[] numBounds, int maxBounds) {
            int i, j, k, l;
            idVec3 v1, v2, normal;
            float d, dist;
            boolean flipTest, front;
            idPlane plane = new idPlane();

            // check all combinations	
            for (i = 0; i < source.GetNumPoints(); i++) {

                l = (i + 1) % source.GetNumPoints();
                v1 = source.oGet(l).ToVec3().oMinus(source.oGet(i).ToVec3());

                // find a vertex of pass that makes a plane that puts all of the
                // vertices of pass on the front side and all of the vertices of
                // source on the back side
                for (j = 0; j < pass.GetNumPoints(); j++) {

                    v2 = pass.oGet(j).ToVec3().oMinus(source.oGet(i).ToVec3());

                    normal = v1.Cross(v2);
                    if (normal.Normalize() < 0.01f) {
                        continue;
                    }
                    dist = normal.oMultiply(pass.oGet(j).ToVec3());

                    //
                    // find out which side of the generated seperating plane has the
                    // source portal
                    //
                    flipTest = false;
                    for (k = 0; k < source.GetNumPoints(); k++) {
                        if (k == i || k == l) {
                            continue;
                        }
                        d = source.oGet(k).ToVec3().oMultiply(normal) - dist;
                        if (d < -ON_EPSILON) {
                            // source is on the negative side, so we want all
                            // pass and target on the positive side
                            flipTest = false;
                            break;
                        } else if (d > ON_EPSILON) {
                            // source is on the positive side, so we want all
                            // pass and target on the negative side
                            flipTest = true;
                            break;
                        }
                    }
                    if (k == source.GetNumPoints()) {
                        continue;		// planar with source portal
                    }

                    // flip the normal if the source portal is backwards
                    if (flipTest) {
                        normal = normal.oNegative();
                        dist = -dist;
                    }

                    // if all of the pass portal points are now on the positive side,
                    // this is the seperating plane
                    front = false;
                    for (k = 0; k < pass.GetNumPoints(); k++) {
                        if (k == j) {
                            continue;
                        }
                        d = pass.oGet(k).ToVec3().oMultiply(normal) - dist;
                        if (d < -ON_EPSILON) {
                            break;
                        } else if (d > ON_EPSILON) {
                            front = true;
                        }
                    }
                    if (k < pass.GetNumPoints()) {
                        continue;	// points on negative side, not a seperating plane
                    }
                    if (!front) {
                        continue;	// planar with seperating plane
                    }

                    // flip the normal if we want the back side
                    if (flipClip) {
                        plane.SetNormal(normal.oNegative());
                        plane.SetDist(-dist);
                    } else {
                        plane.SetNormal(normal);
                        plane.SetDist(dist);
                    }

                    // check if the plane is already a passage boundary
                    for (k = 0; k < numBounds[0]; k++) {
                        if (plane.Compare(bounds[k], 0.001f, 0.01f)) {
                            break;
                        }
                    }
                    if (k < numBounds[0]) {
                        break;
                    }

                    if (numBounds[0] >= maxBounds) {
                        gameLocal.Warning("max passage boundaries.");
                        break;
                    }
                    bounds[numBounds[0]] = plane;
                    numBounds[0]++;
                    break;
                }
            }
        }
        public static final int MAX_PASSAGE_BOUNDS = 128;

        private void CreatePassages() {
            int i, j, l, n, front, passageMemory, byteNum, bitNum;
            int[] numBounds = new int[1];
            int[] sides = new int[MAX_PASSAGE_BOUNDS];
            idPlane[] passageBounds = new idPlane[MAX_PASSAGE_BOUNDS];
            pvsPortal_t source, target, p;
            pvsArea_t area;
            pvsPassage_t passage;
            idFixedWinding winding = new idFixedWinding();
            byte canSee, mightSee, bit;

            passageMemory = 0;
            for (i = 0; i < numPortals; i++) {
                source = pvsPortals[i];
                area = pvsAreas[source.areaNum];

                source.passages = new pvsPassage_t[area.numPortals];

                for (j = 0; j < area.numPortals; j++) {
                    target = area.portals[j];
                    n = indexOf(target, pvsPortals);

                    passage = source.passages[j] = new pvsPassage_t();

                    // if the source portal cannot see this portal
                    if (0 == (source.mightSee[ n >> 3] & (1 << (n & 7)))) {
                        // not all portals in the area have to be visible because areas are not necesarily convex
                        // also no passage has to be created for the portal which is the opposite of the source
                        passage.canSee = null;
                        continue;
                    }

                    passage.canSee = new byte[portalVisBytes];
                    passageMemory += portalVisBytes;

                    // boundary plane normals point inwards
                    numBounds[0] = 0;
                    AddPassageBoundaries(source.w, target.w, false, passageBounds, numBounds, MAX_PASSAGE_BOUNDS);
                    AddPassageBoundaries(target.w, source.w, true, passageBounds, numBounds, MAX_PASSAGE_BOUNDS);

                    // get all portals visible through this passage
                    for (byteNum = 0; byteNum < portalVisBytes; byteNum++) {

                        canSee = 0;
                        mightSee = (byte) (source.mightSee[byteNum] & target.mightSee[byteNum]);

                        // go through eight portals at a time to speed things up
                        for (bitNum = 0; bitNum < 8; bitNum++) {

                            bit = (byte) (1 << bitNum);

                            if (0 == (mightSee & bit)) {
                                continue;
                            }

                            p = pvsPortals[(byteNum << 3) + bitNum];

                            if (p.areaNum == source.areaNum) {
                                continue;
                            }

                            for (front = 0, l = 0; l < numBounds[0]; l++) {
                                sides[l] = p.bounds.PlaneSide(passageBounds[l]);
                                // if completely at the back of the passage bounding plane
                                if (sides[l] == PLANESIDE_BACK) {
                                    break;
                                }
                                // if completely at the front
                                if (sides[l] == PLANESIDE_FRONT) {
                                    front++;
                                }
                            }
                            // if completely outside the passage
                            if (l < numBounds[0]) {
                                continue;
                            }

                            // if not at the front of all bounding planes and thus not completely inside the passage
                            if (front != numBounds[0]) {

                                winding.oSet(p.w);

                                for (l = 0; l < numBounds[0]; l++) {
                                    // only clip if the winding possibly crosses this plane
                                    if (sides[l] != PLANESIDE_CROSS) {
                                        continue;
                                    }
                                    // clip away the part at the back of the bounding plane
                                    winding.ClipInPlace(passageBounds[l]);
                                    // if completely clipped away
                                    if (0 == winding.GetNumPoints()) {
                                        break;
                                    }
                                }
                                // if completely outside the passage
                                if (l < numBounds[0]) {
                                    continue;
                                }
                            }

                            canSee |= bit;
                        }

                        // store results of all eight portals
                        passage.canSee[byteNum] = canSee;
                    }

                    // can always see the target portal
                    passage.canSee[n >> 3] |= (1 << (n & 7));
                }
            }
            if (passageMemory < 1024) {
                gameLocal.Printf("%5d bytes passage memory used to build PVS\n", passageMemory);
            } else {
                gameLocal.Printf("%5d KB passage memory used to build PVS\n", passageMemory >> 10);
            }
        }

        private void DestroyPassages() {
            int i, j;
            pvsPortal_t p;
            pvsArea_t area;

            for (i = 0; i < numPortals; i++) {
                p = pvsPortals[i];
                area = pvsAreas[p.areaNum];
                for (j = 0; j < area.numPortals; j++) {
                    if (p.passages[j].canSee != null) {
//				delete[] p.passages[j].canSee;
                        p.passages[j].canSee = null;
                    }
                }
//		delete[] p.passages;
                p.passages = null;
            }
        }

        private int AreaPVSFromPortalPVS() {
            int i, j, k, areaNum, totalVisibleAreas;
            long[] p1, p2;
            int pvs;
            byte[] portalPVS;
            pvsArea_t area;

            totalVisibleAreas = 0;

            if (0 == numPortals) {
                return totalVisibleAreas;
            }

            Arrays.fill(areaPVS, 0, numAreas * areaVisBytes, (byte) 0);

            for (i = 0; i < numAreas; i++) {
                area = pvsAreas[i];
//                pvs = areaPVS + i * areaVisBytes;
                pvs = i * areaVisBytes;

                // the area is visible to itself
                areaPVS[pvs + (i >> 3)] |= 1 << (i & 7);

                if (0 == area.numPortals) {
                    continue;
                }

                // store the PVS of all portals in this area at the first portal
                for (j = 1; j < area.numPortals; j++) {
                    p1 = reinterpret_cast_long_array(area.portals[0].vis);
                    p2 = reinterpret_cast_long_array(area.portals[j].vis);
                    for (k = 0; k < portalVisLongs; k++) {
                        p1[k] |= p2[k];
                    }
                }

                // the portals of this area are always visible
                for (j = 0; j < area.numPortals; j++) {
                    k = indexOf(area.portals[j], pvsPortals);
                    area.portals[0].vis[ k >> 3] |= 1 << (k & 7);
                }

                // set all areas to visible that can be seen from the portals of this area
                portalPVS = area.portals[0].vis;
                for (j = 0; j < numPortals; j++) {
                    // if this portal is visible
                    if ((portalPVS[j >> 3] & (1 << (j & 7))) != 0) {
                        areaNum = pvsPortals[j].areaNum;
                        areaPVS[pvs + (areaNum >> 3)] |= 1 << (areaNum & 7);
                    }
                }

                // count the number of visible areas
                for (j = 0; j < numAreas; j++) {
                    if ((areaPVS[pvs + (j >> 3)] & (1 << (j & 7))) != 0) {
                        totalVisibleAreas++;
                    }
                }
            }
            return totalVisibleAreas;
        }

        /*
         ================
         idPVS::GetConnectedAreas

         assumes the 'areas' array is initialized to false
         ================
         */
        private void GetConnectedAreas(int srcArea, boolean[] connectedAreas) {
            int curArea, nextArea;
            int queueStart, queueEnd;
            int i, n;
            exitPortal_t portal;

            queueStart = -1;
            queueEnd = 0;
            connectedAreas[srcArea] = true;

            for (curArea = srcArea; queueStart < queueEnd; curArea = areaQueue[++queueStart]) {

                n = gameRenderWorld.NumPortalsInArea(curArea);

                for (i = 0; i < n; i++) {
                    portal = gameRenderWorld.GetPortal(curArea, i);

                    if ((portal.blockingBits & PS_BLOCK_VIEW.ordinal()) != 0) {
                        continue;
                    }

                    // area[1] is always the area the portal leads to
                    nextArea = portal.areas[1];

                    // if already visited this area
                    if (connectedAreas[nextArea]) {
                        continue;
                    }

                    // add area to queue
                    areaQueue[queueEnd++] = nextArea;
                    connectedAreas[nextArea] = true;
                }
            }
        }

        private pvsHandle_t AllocCurrentPVS( /*unsigned*/int h) {
            int i;
            pvsHandle_t handle = new pvsHandle_t();

            for (i = 0; i < MAX_CURRENT_PVS; i++) {
                if (currentPVS[i].handle.i == -1) {
                    currentPVS[i].handle.i = i;
                    currentPVS[i].handle.h = h;
                    return currentPVS[i].handle;
                }
            }

            gameLocal.Error("idPVS::AllocCurrentPVS: no free PVS left");

            handle.i = -1;
            handle.h = 0;
            return handle;
        }

    };

}
