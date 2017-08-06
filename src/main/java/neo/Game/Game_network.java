package neo.Game;

import neo.Game.Game_local.entityNetEvent_s;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_network.idEventQueue.outOfOrderBehaviour_t.OUTOFORDER_DROP;
import static neo.Game.Game_network.idEventQueue.outOfOrderBehaviour_t.OUTOFORDER_SORT;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.CVarSystem.CVAR_GAME;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.idCmdSystem;

import static neo.idlib.Lib.idLib.common;

/**
 *
 */
public class Game_network {

    /*
     ===============================================================================

     Client running game code:
     - entity events don't work and should not be issued
     - entities should never be spawned outside idGameLocal::ClientReadSnapshot

     ===============================================================================
     */
// adds tags to the network protocol to detect when things go bad ( internal consistency )
// NOTE: this changes the network protocol
//#ifndef ASYNC_WRITE_TAGS
    public static final boolean ASYNC_WRITE_TAGS = false;
//#endif
//    
    public static final idCVar net_clientShowSnapshot = new idCVar("net_clientShowSnapshot", "0", CVAR_GAME | CVAR_INTEGER, "", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
    public static final idCVar net_clientShowSnapshotRadius = new idCVar("net_clientShowSnapshotRadius", "128", CVAR_GAME | CVAR_FLOAT, "");
    public static final idCVar net_clientSmoothing = new idCVar("net_clientSmoothing", "0.8", CVAR_GAME | CVAR_FLOAT, "smooth other clients angles and position.", 0.0f, 0.95f);
    public static final idCVar net_clientSelfSmoothing = new idCVar("net_clientSelfSmoothing", "0.6", CVAR_GAME | CVAR_FLOAT, "smooth self position if network causes prediction error.", 0.0f, 0.95f);
    public static final idCVar net_clientMaxPrediction = new idCVar("net_clientMaxPrediction", "1000", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "maximum number of milliseconds a client can predict ahead of server.");
    public static final idCVar net_clientLagOMeter = new idCVar("net_clientLagOMeter", "1", CVAR_GAME | CVAR_BOOL | CVAR_NOCHEAT | CVAR_ARCHIVE, "draw prediction graph");
//    

    public static class idEventQueue {

        private entityNetEvent_s start;
        private entityNetEvent_s end;
//        private idBlockAlloc<entityNetEvent_s> eventAllocator = new idBlockAlloc<>(32);
        //
        //

        public enum outOfOrderBehaviour_t {

            OUTOFORDER_IGNORE,
            OUTOFORDER_DROP,
            OUTOFORDER_SORT
        };

        public idEventQueue() {//: start( NULL ), end( NULL ) {}
        }

        public entityNetEvent_s Alloc() {
            entityNetEvent_s event = new entityNetEvent_s();// eventAllocator.Alloc();
            event.prev = null;
            event.next = null;
            return event;
        }

        public void Free(entityNetEvent_s event) {
            // should only be called on an unlinked event!
            assert (null == event.next && null == event.prev);
//            eventAllocator.Free(event);
        }

        public void Shutdown() {
//            eventAllocator.Shutdown();
            this.Init();
        }

        public void Init() {
            start = null;
            end = null;
        }

        public void Enqueue(entityNetEvent_s event, outOfOrderBehaviour_t oooBehaviour) {
            if (oooBehaviour.equals(OUTOFORDER_DROP)) {
                // go backwards through the queue and determine if there are
                // any out-of-order events
                while (end != null && end.time > event.time) {
                    entityNetEvent_s outOfOrder = RemoveLast();
                    common.DPrintf("WARNING: new event with id %d ( time %d ) caused removal of event with id %d ( time %d ), game time = %d.\n", event.event, event.time, outOfOrder.event, outOfOrder.time, gameLocal.time);
                    Free(outOfOrder);
                }
            } else if (oooBehaviour.equals(OUTOFORDER_SORT) && end != null) {
                // NOT TESTED -- sorting out of order packets hasn't been
                //				 tested yet... wasn't strictly necessary for
                //				 the patch fix.
                entityNetEvent_s cur = end;
                // iterate until we find a time < the new event's
                while (cur != null && cur.time > event.time) {
                    cur = cur.prev;
                }
                if (null == cur) {
                    // add to start
                    event.next = start;
                    event.prev = null;
                    start = event;
                } else {
                    // insert
                    event.prev = cur;
                    event.next = cur.next;
                    cur.next = event;
                }
                return;
            }

            // add the new event
            event.next = null;
            event.prev = null;

            if (end != null) {
                end.next = event;
                event.prev = end;
            } else {
                start = event;
            }
            end = event;
        }

        public entityNetEvent_s Dequeue() {
            entityNetEvent_s event = start;
            if (null == event) {
                return null;
            }

            start = start.next;

            if (null == start) {
                end = null;
            } else {
                start.prev = null;
            }

            event.next = null;
            event.prev = null;

            return event;
        }

        public entityNetEvent_s RemoveLast() {
            entityNetEvent_s event = end;
            if (null == event) {
                return null;
            }

            end = event.prev;

            if (null == end) {
                start = null;
            } else {
                end.next = null;
            }

            event.next = null;
            event.prev = null;

            return event;
        }

        public entityNetEvent_s Start() {
            return start;
        }
    };
//============================================================================
}
