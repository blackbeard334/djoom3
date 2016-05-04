package neo.Game.GameSys;

import java.nio.ByteBuffer;
import neo.CM.CollisionModel.contactType_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity;
import neo.Game.AI.AI;
import neo.Game.AI.AI_Events;
import neo.Game.AI.AI_Vagary;
import neo.Game.Actor;
import neo.Game.Camera;
import neo.Game.Entity;
import neo.Game.Entity.idEntity;
import neo.Game.FX;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Class.idTypeInfo;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item;
import neo.Game.Light;
import neo.Game.Misc;
import neo.Game.Moveable;
import neo.Game.Mover;
import neo.Game.Player;
import neo.Game.Projectile;
import static neo.Game.Script.Script_Program.MAX_STRING_LEN;
import neo.Game.Script.Script_Thread;
import neo.Game.SecurityCamera;
import neo.Game.Sound;
import neo.Game.Target;
import neo.Game.Trigger;
import neo.Game.Weapon;
import neo.Renderer.Material.idMaterial;
import neo.TempDump.CPP_class;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.sizeof;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.LinkList.idLinkList;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Event {

    public static final int     D_EVENT_MAXARGS     = 8;        // if changed, enable the CREATE_EVENT_CODE define in Event.cpp to generate switch statement for idClass::ProcessEventArgPtr.
    //                                                          // running the game will then generate c:\doom\base\events.txt, the contents of which should be copied into the switch statement.
//
    public static final char    D_EVENT_VOID        = (char) 0;
    public static final char    D_EVENT_INTEGER     = 'd';
    public static final char    D_EVENT_FLOAT       = 'f';
    public static final char    D_EVENT_VECTOR      = 'v';
    public static final char    D_EVENT_STRING      = 's';
    public static final char    D_EVENT_ENTITY      = 'e';
    public static final char    D_EVENT_ENTITY_NULL = 'E';        // event can handle NULL entity pointers
    public static final char    D_EVENT_TRACE       = 't';
    //
    public static final int     MAX_EVENTS          = 4096;
    //
    public static final int     MAX_EVENTSPERFRAME  = 4096;
    //
    static              boolean eventError          = false;
    static String eventErrorMsg;
    //
    static idLinkList<idEvent> FreeEvents = new idLinkList<>();
    static idLinkList<idEvent> EventQueue = new idLinkList<>();
    static idEvent[]           EventPool  = new idEvent[MAX_EVENTS];
//

    static {
        for (int e = 0; e < EventPool.length; e++) {
            EventPool[e] = new idEvent();
        }
        {   //preload AI_Events' idEventDefs(473).
            Actor actor = new Actor();
            AFEntity entity = new AFEntity();
            AI ai = new AI();
            AI_Events events = new AI_Events();
            AI_Vagary vagary = new AI_Vagary();
            Camera camera = new Camera();
            Entity entity1 = new Entity();
            FX fx = new FX();
            Item item = new Item();
            Light light = new Light();
            Misc misc = new Misc();
            Moveable moveable = new Moveable();
            Mover mover = new Mover();
            Player player = new Player();
            Projectile projectile = new Projectile();
            Script_Thread thread = new Script_Thread();
            SecurityCamera securityCamera = new SecurityCamera();
            Sound sound = new Sound();
            Target target = new Target();
            Trigger trigger = new Trigger();
            Weapon weapon = new Weapon();
        }
    }


    /* **********************************************************************

     idEventDef

     ***********************************************************************/
    public static class idEventDef {

        private String               name;
        private String               formatspec;
        private long/*unsigned int*/ formatspecIndex;
        private int                  returnType;
        private int                  numargs;
        private int/*size_t*/        argsize;
        private int[] argOffset = new int[D_EVENT_MAXARGS];
        private int        eventnum;
        private idEventDef next;
        //
        private static idEventDef[] eventDefList = new idEventDef[MAX_EVENTS];
        private static int          numEventDefs = 0;
//
//

        public idEventDef(final String command) {
            this(command, null);
        }

        public idEventDef(final String command, String formatspec /*= NULL*/) {
            this(command, formatspec, (char) 0);
        }

        public idEventDef(final String command, String formatSpec /*= NULL*/, char returnType /*= 0*/) {
            idEventDef ev;
            int i;
            long/*unsigned int*/ bits;

            assert (command != null);
            assert (!idEvent.initialized);

            // Allow NULL to indicate no args, but always store it as ""
            // so we don't have to check for it.
            if (null == formatSpec) {
                formatSpec = "";
            }

            this.name = command;
            this.formatspec = formatSpec;
            this.returnType = returnType;

            numargs = formatSpec.length();
            assert (numargs <= D_EVENT_MAXARGS);
            if (numargs > D_EVENT_MAXARGS) {
                eventError = true;
                eventErrorMsg = String.format("idEventDef::idEventDef : Too many args for '%s' event.", name);
                return;
            }

            // make sure the format for the args is valid, calculate the formatspecindex, and the offsets for each arg
            bits = 0;
            argsize = 0;
            argOffset = new int[D_EVENT_MAXARGS];//memset( argOffset, 0, sizeof( argOffset ) );
            for (i = 0; i < numargs; i++) {
                argOffset[ i] = argsize;
                switch (formatSpec.charAt(i)) {
                    case D_EVENT_FLOAT:
                        bits |= 1 << i;
                        argsize += Float.SIZE / Byte.SIZE;
                        break;

                    case D_EVENT_INTEGER:
                        argsize += Integer.SIZE / Byte.SIZE;
                        break;

                    case D_EVENT_VECTOR:
                        argsize += idVec3.BYTES;
                        break;

                    case D_EVENT_STRING:
                        argsize += MAX_STRING_LEN;
                        break;

                    case D_EVENT_ENTITY:
//                        argsize += sizeof(idEntityPtr.class/*<idEntity>*/);
//                        break;

                    case D_EVENT_ENTITY_NULL:
                        argsize += CPP_class.Pointer.SIZE / Byte.SIZE;
                        break;

                    case D_EVENT_TRACE:
//                        argsize += sizeof(trace_s.class) + MAX_STRING_LEN + sizeof(boolean.class);
                        break;//TODO: re-enable this!

                    default:
                        eventError = true;
                        eventErrorMsg = String.format("idEventDef::idEventDef : Invalid arg format '%s' string for '%s' event.", formatSpec, name);
                        return;
//                        break;
                }
            }

            // calculate the formatspecindex
            formatspecIndex = (1 << (numargs + D_EVENT_MAXARGS)) | bits;

            // go through the list of defined events and check for duplicates
            // and mismatched format strings
            eventnum = numEventDefs;
            for (i = 0; i < eventnum; i++) {
                ev = eventDefList[ i];
                if (command.equals(ev.name)) {
                    if (!formatSpec.equals(ev.formatspec)) {
                        eventError = true;
                        eventErrorMsg = String.format("idEvent '%s' defined twice with same name but differing format strings ('%s'!='%s').",
                                command, formatSpec, ev.formatspec);
                        return;
                    }

                    if (ev.returnType != returnType) {
                        eventError = true;
                        eventErrorMsg = String.format("idEvent '%s' defined twice with same name but differing return types ('%c'!='%c').",
                                command, returnType, ev.returnType);
                        return;
                    }
                    // Don't bother putting the duplicate event in list.
                    eventnum = ev.eventnum;
                    return;
                }
            }

            ev = this;

            if (numEventDefs >= MAX_EVENTS) {
                eventError = true;
                eventErrorMsg = String.format("numEventDefs >= MAX_EVENTS");
                return;
            }
            eventDefList[numEventDefs] = ev;
            numEventDefs++;
        }

        public String GetName() {
            return name;
        }

        public String GetArgFormat() {
            return formatspec;
        }

        public long/*unsigned int*/ GetFormatspecIndex() {
            return formatspecIndex;
        }

        public char GetReturnType() {
            return (char) returnType;
        }

        public int GetEventNum() {
            return eventnum;
        }

        public int GetNumArgs() {
            return numargs;
        }

        public int/*size_t*/ GetArgSize() {
            return argsize;
        }

        public int GetArgOffset(int arg) {
            assert ((arg >= 0) && (arg < D_EVENT_MAXARGS));
            return argOffset[arg];
        }

        public static int NumEventCommands() {
            return numEventDefs;
        }

        public static idEventDef GetEventCommand(int eventnum) {
            return eventDefList[ eventnum];
        }

        public static idEventDef FindEvent(final String name) {
            idEventDef ev;
            int num;
            int i;

            assert (name != null);

            num = numEventDefs;
            for (i = 0; i < num; i++) {
                ev = eventDefList[ i];
                if (name.equals(ev.name)) {
                    return ev;
                }
            }

            return null;
        }
    };

    /* **********************************************************************

     idEvent

     ***********************************************************************/
    public static class idEvent {

        private idEventDef eventdef;
        private Object[]   data;
        private int        time;
        private idClass    object;
        private idTypeInfo typeinfo;
        //
        private       idLinkList<idEvent> eventNode   = new idLinkList<>();
        //
//        private static idDynamicBlockAlloc<Byte> eventDataAllocator = new idDynamicBlockAlloc(16 * 1024, 256);
//
        public static boolean             initialized = false;
//
//

        // ~idEvent();
        public static idEvent Alloc(final idEventDef evdef, int numargs, idEventArg... args) {
            idEvent ev;
            int/*size_t*/ size;
            String format;
//            idEventArg arg;
            int i;
            String materialName;

            if (FreeEvents.IsListEmpty()) {
                gameLocal.Error("idEvent::Alloc : No more free events");
            }

            ev = FreeEvents.Next();
            ev.eventNode.Remove();

            ev.eventdef = evdef;

            if (numargs != evdef.GetNumArgs()) {
                gameLocal.Error("idEvent::Alloc : Wrong number of args for '%s' event.", evdef.GetName());
            }

            size = evdef.GetArgSize();
            if (size != 0) {
//		ev.data = eventDataAllocator.Alloc( size );
//		memset( ev.data, 0, size );
                ev.data = new Object[size];
            } else {
                ev.data = null;
            }

            format = evdef.GetArgFormat();
            for (i = 0; i < numargs; i++) {
                for (idEventArg arg : args) {
//                arg = va_arg(args, idEventArg);
                    if (format.charAt(i) != arg.type) {
                        // when NULL is passed in for an entity, it gets cast as an integer 0, so don't give an error when it happens
                        if (!(((format.charAt(i) == D_EVENT_TRACE) || (format.charAt(i) == D_EVENT_ENTITY)) && (arg.type == 'd') && (arg.value == Integer.valueOf(0)))) {
                            gameLocal.Error("idEvent::Alloc : Wrong type passed in for arg # %d on '%s' event.", i, evdef.GetName());
                        }
                    }

                    switch (format.charAt(i)) {//TODO:S
                        case D_EVENT_FLOAT:
                        case D_EVENT_INTEGER:
                            ev.data[i] = arg.value;
                            break;
                        case D_EVENT_VECTOR:
                            if (arg.value != null) {
                                ev.data[i] = arg.value;
                            }
                            break;
                        case D_EVENT_STRING:
                            if (arg.value != null) {
                                ev.data[i] = (String) arg.value;
                            }
                            break;
                        case D_EVENT_ENTITY:
                        case D_EVENT_ENTITY_NULL:
                            ev.data[i] = new idEntityPtr<idEntity>((idEntity) arg.value);
                            break;
                        case D_EVENT_TRACE:
//			if ( arg.value!=null ) {
//				*reinterpret_cast<bool *>( ev.data[i] ) = true;
//				*reinterpret_cast<trace_t *>( ev.data[i] + sizeof( bool ) ) = *reinterpret_cast<const trace_t *>( arg.value );
//                        final idMaterial material = ((trace_s ) arg.value ).c.material;
//
//				// save off the material as a string since the pointer won't be valid in save games.
//				// since we save off the entire trace_t structure, if the material is NULL here,
//				// it will be NULL when we process it, so we don't need to save off anything in that case.
//				if ( material !=null) {
//					materialName = material.GetName();
//					idStr.Copynz( reinterpret_cast<char *>( ev.data[i] + sizeof( bool ) + sizeof( trace_t ) ), materialName, MAX_STRING_LEN );
//				}
//			} else {
//				*reinterpret_cast<bool *>( ev.data[i] ) = false;
//			}
                            break;
                        default:
                            gameLocal.Error("idEvent::Alloc : Invalid arg format '%s' string for '%s' event.", format, evdef.GetName());
                            break;
                    }
                }
            }

            return ev;
        }

        public static void CopyArgs(final idEventDef evdef, int numargs, idEventArg[] args, int[] data/*[ D_EVENT_MAXARGS ]*/) {
            int i;
            String format;
//            idEventArg arg;

            format = evdef.GetArgFormat();
            if (numargs != evdef.GetNumArgs()) {
                gameLocal.Error("idEvent::CopyArgs : Wrong number of args for '%s' event.", evdef.GetName());
            }

            for (i = 0; i < numargs; i++) {
                for (idEventArg arg : args) {
                    if (format.charAt(i) != arg.type) {
                        // when NULL is passed in for an entity, it gets cast as an integer 0, so don't give an error when it happens
                        if (!(((format.charAt(i) == D_EVENT_TRACE) || (format.charAt(i) == D_EVENT_ENTITY)) && (arg.type == 'd') && (arg.value == Integer.valueOf(0)))) {
                            gameLocal.Error("idEvent::CopyArgs : Wrong type passed in for arg # %d on '%s' event.", i, evdef.GetName());
                        }
                    }

                    data[i] = (int) arg.value;
                }
            }
        }

        public void Free() {
//            if (data != null) {
//                eventDataAllocator.Free(data);
            data = null;
//            }

            eventdef = null;
            time = 0;
            object = null;
            typeinfo = null;

            eventNode.SetOwner(this);
            eventNode.AddToEnd(FreeEvents);
        }

        public void Schedule(idClass obj, final idTypeInfo type, int time) {
            idEvent event;

            assert (initialized);
            if (!initialized) {
                return;
            }

            object = obj;
            typeinfo = type;

            // wraps after 24 days...like I care. ;)
            this.time = gameLocal.time + time;

            eventNode.Remove();

            event = EventQueue.Next();
            while ((event != null) && (this.time >= event.time)) {
                event = event.eventNode.Next();
            }

            if (event != null) {
                eventNode.InsertBefore(event.eventNode);
            } else {
                eventNode.AddToEnd(EventQueue);
            }
        }

        public Object[] GetData() {
            return data;
        }

        public static void CancelEvents(final idClass obj, final idEventDef evdef /*= NULL*/) {
            idEvent event;
            idEvent next;

            if (!initialized) {
                return;
            }

            for (event = EventQueue.Next(); event != null; event = next) {
                next = event.eventNode.Next();
                if (event.object == obj) {
                    if (null == evdef || (evdef.equals(event.eventdef))) {
                        event.Free();
                    }
                }
            }
        }

        public static void ClearEventList() {
            int i;

            //
            // initialize lists
            //
            FreeEvents.Clear();
            EventQueue.Clear();

            // 
            // add the events to the free list
            //
            for (i = 0; i < MAX_EVENTS; i++) {
                EventPool[i].Free();
            }
        }

        public static void ServiceEvents() {
            idEvent event;
            int num;
            int[] args = new int[D_EVENT_MAXARGS];
            int offset;
            int i;
            int numargs;
            String formatspec;
            trace_s[] tracePtr;
            idEventDef ev;
            Object[] data;
            String materialName;

            num = 0;
            while (!EventQueue.IsListEmpty()) {
                event = EventQueue.Next();
                assert (event != null);

                if (event.time > gameLocal.time) {
                    break;
                }

                // copy the data into the local args array and set up pointers
                ev = event.eventdef;
                formatspec = ev.GetArgFormat();
                numargs = ev.GetNumArgs();
                for (i = 0; i < numargs; i++) {
                    offset = ev.GetArgOffset(i);
                    data = event.data;
                    switch (formatspec.charAt(i)) {
                        case D_EVENT_FLOAT:
                        case D_EVENT_INTEGER:
                            args[i] = (int) data[offset];
                            break;

//			case D_EVENT_VECTOR :
//				*reinterpret_cast<idVec3 **>( &args[ i ] ) = reinterpret_cast<idVec3 *>( &data[ offset ] );
//				break;
//
//			case D_EVENT_STRING :
//				*reinterpret_cast<const char **>( &args[ i ] ) = reinterpret_cast<const char *>( &data[ offset ] );
//				break;
//
//			case D_EVENT_ENTITY :
//			case D_EVENT_ENTITY_NULL :
//				*reinterpret_cast<idEntity **>( &args[ i ] ) = reinterpret_cast< idEntityPtr<idEntity> * >( &data[ offset ] ).GetEntity();
//				break;
//
//			case D_EVENT_TRACE :
//				tracePtr = reinterpret_cast<trace_t **>( &args[ i ] );
//				if ( *reinterpret_cast<bool *>( &data[ offset ] ) ) {
//					*tracePtr = reinterpret_cast<trace_t *>( &data[ offset + sizeof( bool ) ] );
//
//					if ( ( *tracePtr ).c.material != NULL ) {
//						// look up the material name to get the material pointer
//						materialName = reinterpret_cast<const char *>( &data[ offset + sizeof( bool ) + sizeof( trace_t ) ] );
//						( *tracePtr ).c.material = declManager.FindMaterial( materialName, true );
//					}
//				} else {
//					*tracePtr = NULL;
//				}
//				break;

                        default:
                            gameLocal.Error("idEvent::ServiceEvents : Invalid arg format '%s' string for '%s' event.", formatspec, ev.GetName());
                    }//TODO:S ^^^^^^^^^^^^^^^^^^^^^
                }

                // the event is removed from its list so that if then object
                // is deleted, the event won't be freed twice
                event.eventNode.Remove();
                assert (event.object != null);
                event.object.ProcessEventArgPtr(ev, args);

// #if 0
                // // event functions may never leave return values on the FPU stack
                // // enable this code to check if any event call left values on the FPU stack
                // if ( !sys.FPU_StackIsEmpty() ) {
                // gameLocal.Error( "idEvent::ServiceEvents %d: %s left a value on the FPU stack\n", num, ev.GetName() );
                // }
// #endif
                // return the event to the free list
                event.Free();

                // Don't allow ourselves to stay in here too long.  An abnormally high number
                // of events being processed is evidence of an infinite loop of events.
                num++;
                if (num > MAX_EVENTSPERFRAME) {
                    gameLocal.Error("Event overflow.  Possible infinite loop in script.");
                }
            }
        }

        public static void Init() {
            gameLocal.Printf("Initializing event system\n");

            if (eventError) {
                gameLocal.Error("%s", eventErrorMsg);
            }

// #ifdef CREATE_EVENT_CODE
            // void CreateEventCallbackHandler();
            // CreateEventCallbackHandler();
            // gameLocal.Error( "Wrote event callback handler" );
// #endif
            if (initialized) {
                gameLocal.Printf("...already initialized\n");
                ClearEventList();
                return;
            }

            ClearEventList();
//
//            eventDataAllocator.Init();
//
            gameLocal.Printf("...%d event definitions\n", idEventDef.NumEventCommands());

            // the event system has started
            initialized = true;
        }

        public static void Shutdown() {
            gameLocal.Printf("Shutdown event system\n");

            if (!initialized) {
                gameLocal.Printf("...not started\n");
                return;
            }

            ClearEventList();
//
//            eventDataAllocator.Shutdown();
//
            // say it is now shutdown
            initialized = false;
        }

        // save games
        public static void Save(idSaveGame savefile) {					// archives object for save game file
            String str;
            int i, size;
            idEvent event;
            byte[] dataPtr;
            boolean validTrace;
            String format;

            savefile.WriteInt(EventQueue.Num());

            event = EventQueue.Next();
            while (event != null) {
                savefile.WriteInt(event.time);
                savefile.WriteString(event.eventdef.GetName());
                savefile.WriteString(event.typeinfo.classname);
                savefile.WriteObject(event.object);
                savefile.WriteInt(event.eventdef.GetArgSize());
                format = event.eventdef.GetArgFormat();
                for (i = 0, size = 0; i < event.eventdef.GetNumArgs(); ++i) {
//			dataPtr = &event.data[ event.eventdef.GetArgOffset( i ) ];//TODOS:
//			switch( format[ i ] ) {
//				case D_EVENT_FLOAT :
//					savefile.WriteFloat( *reinterpret_cast<float *>( dataPtr ) );
//					size += sizeof( float );
//					break;
//				case D_EVENT_INTEGER :
//				case D_EVENT_ENTITY :
//				case D_EVENT_ENTITY_NULL :
//					savefile.WriteInt( *reinterpret_cast<int *>( dataPtr ) );
//					size += sizeof( int );
//					break;
//				case D_EVENT_VECTOR :
//					savefile.WriteVec3( *reinterpret_cast<idVec3 *>( dataPtr ) );
//					size += sizeof( idVec3 );
//					break;
//				case D_EVENT_TRACE :
//					validTrace = *reinterpret_cast<bool *>( dataPtr );
//					savefile.WriteBool( validTrace );
//					size += sizeof( bool );
//					if ( validTrace ) {
//						size += sizeof( trace_t );
//						const trace_t &t = *reinterpret_cast<trace_t *>( dataPtr + sizeof( bool ) );
//						SaveTrace( savefile, t );
//						if ( t.c.material ) {
//							size += MAX_STRING_LEN;
//							str = reinterpret_cast<char *>( dataPtr + sizeof( bool ) + sizeof( trace_t ) );
//							savefile.Write( str, MAX_STRING_LEN );
//						}
//					}
//					break;
//				default:
//					break;
//			}
                }
                assert (size == event.eventdef.GetArgSize());
                event = event.eventNode.Next();
            }
        }

        public static void Restore(idRestoreGame savefile) {				// unarchives object from save game file
            ByteBuffer str = ByteBuffer.allocate(MAX_STRING_LEN);
            int[] num = {0}, argsize = {0};
            int i, j, size;
            idStr name = new idStr();
            idEvent event;
            String format;

            savefile.ReadInt(num);

            for (i = 0; i < num[0]; i++) {
                if (FreeEvents.IsListEmpty()) {
                    gameLocal.Error("idEvent::Restore : No more free events");
                }

                event = FreeEvents.Next();
                event.eventNode.Remove();
                event.eventNode.AddToEnd(EventQueue);

                event.time = savefile.ReadInt();

                // read the event name
                savefile.ReadString(name);
                event.eventdef = idEventDef.FindEvent(name.toString());
                if (null == event.eventdef) {
                    savefile.Error("idEvent::Restore: unknown event '%s'", name.toString());
                }

                // read the classtype
                savefile.ReadString(name);
                event.typeinfo = idClass.GetClass(name.toString());
                if (null == event.typeinfo) {
                    savefile.Error("idEvent::Restore: unknown class '%s' on event '%s'", name.toString(), event.eventdef.GetName());
                }

                savefile.ReadObject(event.object);

                // read the args
                savefile.ReadInt(argsize);
                if (argsize[0] != event.eventdef.GetArgSize()) {
                    savefile.Error("idEvent::Restore: arg size (%d) doesn't match saved arg size(%d) on event '%s'", event.eventdef.GetArgSize(), argsize[0], event.eventdef.GetName());
                }
                if (argsize[0] != 0) {
                    event.data = new Object[argsize[0]];//eventDataAllocator.Alloc(argsize[0]);
                    format = event.eventdef.GetArgFormat();
                    assert (format != null);
                    for (j = 0, size = 0; j < event.eventdef.GetNumArgs(); ++j) {
                        switch (format.charAt(j)) {//TODOS:reint
                            case D_EVENT_FLOAT:
                                event.data[j] = savefile.ReadFloat( /*reinterpret_cast<float *>( dataPtr )*/);
                                size += Float.BYTES;
                                break;
                            case D_EVENT_INTEGER:
                            case D_EVENT_ENTITY:
                            case D_EVENT_ENTITY_NULL:
                                event.data[j] = savefile.ReadInt( /*reinterpret_cast<int *>( dataPtr )*/);
                                size += Integer.BYTES;
                                break;
                            case D_EVENT_VECTOR:
                                idVec3 buffer = new idVec3();
//						savefile.ReadVec3( *reinterpret_cast<idVec3 *>( dataPtr ) );
                                savefile.ReadVec3(buffer);
                                event.data[j] = buffer.Write();
                                size += idVec3.BYTES;
                                break;
                            case D_EVENT_TRACE:
                                boolean bOOl = savefile.ReadBool( /*reinterpret_cast<bool *>( dataPtr )*/);
                                event.data[j] = ((byte) btoi(bOOl));
                                size++;
//						if ( *reinterpret_cast<bool *>( dataPtr ) ) {
                                if (bOOl) {
                                    size += sizeof(trace_s.class);
//							trace_s t = *reinterpret_cast<trace_t *>( dataPtr + sizeof( bool ) );
                                    trace_s t = new trace_s();
                                    RestoreTrace(savefile, t);
                                    event.data[j] = t.Write();
                                    if (t.c.material != null) {
                                        size += MAX_STRING_LEN;
//								str = reinterpret_cast<char *>( dataPtr + sizeof( bool ) + sizeof( trace_t ) );
                                        savefile.Read(str, MAX_STRING_LEN);
                                        event.data[j] = str;
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    assert (size == event.eventdef.GetArgSize());
                } else {
                    event.data = null;
                }
            }
        }

        /*
         ================
         idEvent::WriteTrace

         idSaveGame has a WriteTrace procedure, but unfortunately idEvent wants the material
         string name at the of the data structure rather than in the middle
         ================
         */
        public static void SaveTrace(idSaveGame savefile, final trace_s trace) {
            savefile.WriteFloat(trace.fraction);
            savefile.WriteVec3(trace.endpos);
            savefile.WriteMat3(trace.endAxis);
            savefile.WriteInt(etoi(trace.c.type));
            savefile.WriteVec3(trace.c.point);
            savefile.WriteVec3(trace.c.normal);
            savefile.WriteFloat(trace.c.dist);
            savefile.WriteInt(trace.c.contents);
//            savefile.WriteInt( /*(int&)*/trace.c.material);
            savefile.Write( /*(int&)*/trace.c.material);
            savefile.WriteInt(trace.c.contents);
            savefile.WriteInt(trace.c.modelFeature);
            savefile.WriteInt(trace.c.trmFeature);
            savefile.WriteInt(trace.c.id);
        }

        /*
         ================
         idEvent::ReadTrace
 
         idRestoreGame has a ReadTrace procedure, but unfortunately idEvent wants the material
         string name at the of the data structure rather than in the middle
         ================
         */
        public static void RestoreTrace(idRestoreGame savefile, trace_s trace) {
            trace.fraction = savefile.ReadFloat();
            savefile.ReadVec3(trace.endpos);
            savefile.ReadMat3(trace.endAxis);
            trace.c.type = contactType_t.values()[savefile.ReadInt( /*(int&)*/)];
            savefile.ReadVec3(trace.c.point);
            savefile.ReadVec3(trace.c.normal);
            trace.c.dist = savefile.ReadFloat();
            trace.c.contents = savefile.ReadInt();
//            savefile.ReadInt( /*(int&)*/trace.c.material);
            savefile.Read( /*(int&)*/trace.c.material);
            trace.c.contents = savefile.ReadInt();
            trace.c.modelFeature = savefile.ReadInt();
            trace.c.trmFeature = savefile.ReadInt();
            trace.c.id = savefile.ReadInt();
        }
    };
}
