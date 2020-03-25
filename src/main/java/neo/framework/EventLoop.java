package neo.framework;

import static neo.TempDump.bbtoa;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.sys.sys_public.sysEventType_t.SE_CONSOLE;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_NONE;
import static neo.sys.win_main.Sys_GetEvent;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.nio.ByteBuffer;

import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.File_h.idFile;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.Lib.idException;
import neo.sys.sys_public.sysEvent_s;

/**
 *
 */
public class EventLoop {
    /*
     ===============================================================================

     The event loop receives events from the system and dispatches them to
     the various parts of the engine. The event loop also handles journaling.
     The file system copies .cfg files to the journaled file.

     ===============================================================================
     */

    static final int MAX_PUSHED_EVENTS = 64;

    static final idCVar com_journalFile = new idCVar("com_journal", "0", CVAR_INIT | CVAR_SYSTEM, "1 = record journal, 2 = play back journal", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));

    public static final idEventLoop eventLoop = new idEventLoop();

    public static class idEventLoop {

        // Journal file.
        public idFile com_journalFile;
        public idFile com_journalDataFile;
        //
        //
        // all events will have this subtracted from their time
        private int initialTimeOffset;
        //
        private int com_pushedEventsHead, com_pushedEventsTail;
        private final sysEvent_s[] com_pushedEvents = new sysEvent_s[MAX_PUSHED_EVENTS];
        //
        private static final idCVar com_journal = new idCVar("com_journal", "0", CVAR_INIT | CVAR_SYSTEM, "1 = record journal, 2 = play back journal", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        //
        //

        public idEventLoop() {
            this.com_journalFile = null;
            this.com_journalDataFile = null;
            this.initialTimeOffset = 0;
        }
//					~idEventLoop( void );

        public void Init() throws idException {

            this.initialTimeOffset = Sys_Milliseconds();

            common.StartupVariable("journal", false);

            if (com_journal.GetInteger() == 1) {
                common.Printf("Journaling events\n");
                this.com_journalFile = fileSystem.OpenFileWrite("journal.dat");
                this.com_journalDataFile = fileSystem.OpenFileWrite("journaldata.dat");
            } else if (com_journal.GetInteger() == 2) {
                common.Printf("Replaying journaled events\n");
                this.com_journalFile = fileSystem.OpenFileRead("journal.dat");
                this.com_journalDataFile = fileSystem.OpenFileRead("journaldata.dat");
            }

            if ((null == this.com_journalFile) || (null == this.com_journalDataFile)) {
                com_journal.SetInteger(0);
//		com_journalFile = 0;
//		com_journalDataFile = 0;
                common.Printf("Couldn't open journal files\n");
            }
        }

        // Closes the journal file if needed.
        public void Shutdown() {
            if (this.com_journalFile != null) {
                fileSystem.CloseFile(this.com_journalFile);
                this.com_journalFile = null;
            }
            if (this.com_journalDataFile != null) {
                fileSystem.CloseFile(this.com_journalDataFile);
                this.com_journalDataFile = null;
            }
        }

        // It is possible to get an event at the beginning of a frame that
        // has a time stamp lower than the last event from the previous frame.
        public sysEvent_s GetEvent() throws idException {
            if (this.com_pushedEventsHead > this.com_pushedEventsTail) {
                this.com_pushedEventsTail++;
                return this.com_pushedEvents[(this.com_pushedEventsTail - 1) & (MAX_PUSHED_EVENTS - 1)];
            }
            return GetRealEvent();
        }

        // Dispatches all pending events and returns the current time.
        public int RunEventLoop() throws idException {
            return RunEventLoop(true);
        }

        public int RunEventLoop(boolean commandExecution /*= true*/) throws idException {
            sysEvent_s ev;

            while (true) {

                if (commandExecution) {
                    // execute any bound commands before processing another event
                    cmdSystem.ExecuteCommandBuffer();
                }

                ev = GetEvent();

                // if no more events are available
                if (ev.evType == SE_NONE) {
                    return 0;
                }
                ProcessEvent(ev);
            }

//	return 0;	// never reached
        }

        /*
         ================
         idEventLoop::Milliseconds

         Can be used for profiling, but will be journaled accurately
         ================
         */
        // Gets the current time in a way that will be journaled properly,
        // as opposed to Sys_Milliseconds(), which always reads a real timer.
        public int Milliseconds() {
//            if (true) {// FIXME!
            return Sys_Milliseconds() - this.initialTimeOffset;
//            } else {
//                sysEvent_s ev;
//
//                // get events and push them until we get a null event with the current time
//                do {
//
//                    ev = Com_GetRealEvent();
//                    if (ev.evType != SE_NONE) {
//                        Com_PushEvent( & ev);
//                    }
//                } while (ev.evType != SE_NONE);
//
//                return ev.evTime;
//        };
        }

        // Returns the journal level, 1 = record, 2 = play back.
        public int JournalLevel() {
            return com_journal.GetInteger();
        }

        private sysEvent_s GetRealEvent() throws idException {
            int r;
            sysEvent_s ev;
            ByteBuffer event;

            // either get an event from the system or the journal file
            if (com_journal.GetInteger() == 2) {
                event = ByteBuffer.allocate(sysEvent_s.BYTES);
                r = this.com_journalFile.Read(event);
                ev = new sysEvent_s(event);
                if (r != ev.BYTES) {
                    common.FatalError("Error reading from journal file");
                }
                if (ev.evPtrLength != 0) {
                    ev.evPtr = ByteBuffer.allocate(ev.evPtrLength);//Mem_ClearedAlloc(ev.evPtrLength);
                    r = this.com_journalFile.Read(ev.evPtr);//, ev.evPtrLength);
                    if (r != ev.evPtrLength) {
                        common.FatalError("Error reading from journal file");
                    }
                }
            } else {
                ev = Sys_GetEvent();

                // write the journal value out if needed
                if (com_journal.GetInteger() == 1) {
                    r = this.com_journalFile.Write(ev.Write());
                    if (r != ev.BYTES) {
                        common.FatalError("Error writing to journal file");
                    }
                    if (ev.evPtrLength != 0) {
                        r = this.com_journalFile.Write(ev.evPtr, ev.evPtrLength);
                        if (r != ev.evPtrLength) {
                            common.FatalError("Error writing to journal file");
                        }
                    }
                }
            }

            return ev;
        }

        private void ProcessEvent(sysEvent_s ev) throws idException {
            // track key up / down states
            if (ev.evType == SE_KEY) {
                idKeyInput.PreliminaryKeyEvent(ev.evValue, (ev.evValue2 != 0));
            }

            if (ev.evType == SE_CONSOLE) {
                // from a text console outside the game window
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, bbtoa(ev.evPtr));
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "\n");
            } else {
                session.ProcessEvent(ev);
            }

            // free any block data
            if (ev.evPtr != null) {//TODO:ptr?
//                Mem_Free(ev.evPtr);
                ev.evPtr = null;
            }
        }
        static boolean printedWarning;

        private void PushEvent(sysEvent_s event) throws idException {
            sysEvent_s ev;

            ev = this.com_pushedEvents[this.com_pushedEventsHead & (MAX_PUSHED_EVENTS - 1)];

            if ((this.com_pushedEventsHead - this.com_pushedEventsTail) >= MAX_PUSHED_EVENTS) {

                // don't print the warning constantly, or it can give time for more...
                if (!printedWarning) {
                    printedWarning = true;
                    common.Printf("WARNING: Com_PushEvent overflow\n");
                }

                if (ev.evPtr != null) {
//                    Mem_Free(ev.evPtr);
                    ev.evPtr = null;
                }
                this.com_pushedEventsTail++;
            } else {
                printedWarning = false;
            }

            this.com_pushedEvents[this.com_pushedEventsHead & (MAX_PUSHED_EVENTS - 1)] = event;
            this.com_pushedEventsHead++;
        }
    }
}
