package neo.Game.AI;

import java.nio.IntBuffer;

import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AAS_routing {

    static final int CACHETYPE_AREA           = 1;
    static final int CACHETYPE_PORTAL         = 2;
    //
    static final int MAX_ROUTING_CACHE_MEMORY = (2 * 1024 * 1024);
    //
    static final int LEDGE_TRAVELTIME_PANALTY = 250;
//

    static class idRoutingCache {
        // friend class idAASLocal;
        public static final int BYTES = Integer.BYTES * 12;

        int            type;            // portal or area cache
        int            size;            // size of cache
        int            cluster;         // cluster of the cache
        int            areaNum;         // area of the cache
        int            travelFlags;     // combinations of the travel flags
        idRoutingCache next;            // next in list
        idRoutingCache prev;            // previous in list
        idRoutingCache time_next;       // next in time based list
        idRoutingCache time_prev;       // previous in time based list
        int            startTravelTime; // travel time to start with
        byte[]         reachabilities;  // reachabilities used for routing
        int[]          travelTimes;     // travel time for every area
        //
        //

        public idRoutingCache(int size) {
            this.areaNum = 0;
            this.cluster = 0;
            this.next = this.prev = null;
            this.time_next = this.time_prev = null;
            this.travelFlags = 0;
            this.startTravelTime = 0;
            this.type = 0;
            this.size = size;
            this.reachabilities = new byte[size];
//	memset( reachabilities, 0, size * sizeof( reachabilities[0] ) );
            this.travelTimes = new int[size];
//	memset( travelTimes, 0, size * sizeof( travelTimes[0] ) );
        }

        // ~idRoutingCache( void );
        public int Size() {
            return idRoutingCache.BYTES + (this.size * Byte.BYTES) + (this.size * Short.BYTES);//TODO:we use integers for travelTimes, but are using shorts for the sake of consistency...
        }
    }

    static class idRoutingUpdate {
        // friend class idAASLocal;

        int             cluster;         // cluster number of this update
        int             areaNum;         // area number of this update
        int             tmpTravelTime;   // temporary travel time
        IntBuffer       areaTravelTimes; // travel times within the area
        idVec3          start;           // start point into area
        idRoutingUpdate next;            // next in list
        idRoutingUpdate prev;            // prev in list
        boolean         isInList;        // true if the update is in the list
        //
        //
    }

    static class idRoutingObstacle {
        // friend class idAASLocal;

        idBounds        bounds;          // obstacle bounds
        idList<Integer> areas;           // areas the bounds are in
        //
        //

        idRoutingObstacle() {
        }
    }
}
