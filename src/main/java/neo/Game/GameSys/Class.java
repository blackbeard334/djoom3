package neo.Game.GameSys;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.GameSys.Class.idEventArg.toArg;
import static neo.Game.GameSys.Event.D_EVENT_ENTITY;
import static neo.Game.GameSys.Event.D_EVENT_FLOAT;
import static neo.Game.GameSys.Event.D_EVENT_INTEGER;
import static neo.Game.GameSys.Event.D_EVENT_MAXARGS;
import static neo.Game.GameSys.Event.D_EVENT_STRING;
import static neo.Game.GameSys.Event.D_EVENT_TRACE;
import static neo.Game.GameSys.Event.D_EVENT_VECTOR;
import static neo.Game.GameSys.SysCvar.g_debugTriggers;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
import static neo.TempDump.NOT;
import static neo.TempDump.sizeof;
import static neo.idlib.math.Math_h.SEC2MS;

import java.util.HashMap;
import java.util.Map;

import neo.TempDump;
import neo.TempDump.Deprecation_Exception;
import neo.TempDump.TODO_Exception;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Projectile.idBFGProjectile;
import neo.Game.Projectile.idProjectile;
import neo.Game.Target.idTarget_Remove;
import neo.Game.Trigger.idTrigger_Multi;
import neo.Game.AI.AI.idAI;
import neo.Game.GameSys.Event.idEvent;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Script.Script_Thread.idThread;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.Hierarchy.idHierarchy;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Class {

    public static final idEventDef EV_Remove     = new idEventDef("<immediateremove>", null);
    public static final idEventDef EV_SafeRemove = new idEventDef("remove", null);

    // this is the head of a singly linked list of all the idTypes
    static idTypeInfo              typelist            = null;
    static idHierarchy<idTypeInfo> classHierarchy      = new idHierarchy<>();
    static int                     eventCallbackMemory = 0;

    @FunctionalInterface
    public interface eventCallback_t<T extends idClass> {
        void accept(T t, idEventArg...args);
    }

    @FunctionalInterface
    public interface eventCallback_t0<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t);
        }

        void accept(T e);
    }

    @FunctionalInterface
    public interface eventCallback_t1<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0]);
        }

        void accept(T t, idEventArg a);
    }

    @FunctionalInterface
    public interface eventCallback_t2<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0], args[1]);
        }

        void accept(T t, idEventArg a, idEventArg b);
    }

    @FunctionalInterface
    public interface eventCallback_t3<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0], args[1], args[2]);
        }

        void accept(T t, idEventArg a, idEventArg b, idEventArg c);
    }

    @FunctionalInterface
    public interface eventCallback_t4<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0], args[1], args[2], args[3]);
        }

        void accept(T t, idEventArg a, idEventArg b, idEventArg c, idEventArg d);
    }

    @FunctionalInterface
    public interface eventCallback_t5<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0], args[1], args[2], args[3], args[4]);
        }

        void accept(T t, idEventArg a, idEventArg b, idEventArg c, idEventArg d, idEventArg e);
    }

    @FunctionalInterface
    public interface eventCallback_t6<T extends idClass> extends eventCallback_t<T> {
        @Override
        default void accept(T t, idEventArg... args) {
            accept(t, args[0], args[1], args[2], args[3], args[4], args[5]);
        }

        void accept(T t, idEventArg a, idEventArg b, idEventArg c, idEventArg d, idEventArg e, idEventArg f);
    }

    public static abstract class classSpawnFunc_t<type> {

        public abstract type run();
    }

    public static abstract class idClass_Save {

        public abstract void run(idSaveGame savefile);
    }

    public static abstract class idClass_Restore {

        public abstract void run(idRestoreGame savefile);
    }

    public static class idEventFunc<type> {

        idEventDef      event;
        eventCallback_t function;
    };

    public static class idEventArg<T> {

        public final int type;
        public final T   value;
//
//

        private idEventArg(T data) {
            if(data instanceof Integer)         type = D_EVENT_INTEGER;
            else if(data instanceof Enum)       type = D_EVENT_INTEGER;
            else if(data instanceof Float)      type = D_EVENT_FLOAT;
            else if(data instanceof idVec3)     type = D_EVENT_VECTOR;
            else if(data instanceof idStr)      type = D_EVENT_STRING;
            else if(data instanceof String)     type = D_EVENT_STRING;
            else if(data instanceof idEntity)   type = D_EVENT_ENTITY;
            else if(data instanceof trace_s)    type = D_EVENT_TRACE;
            else {
//                type = D_EVENT_VOID;
                throw new TempDump.TypeErasure_Expection();
            }
            value = data;
        }

        private idEventArg(int type, T data) {
            this.type = type;
            value = data;
        }

        static <T> idEventArg<T> toArg(T data) {
            return new idEventArg(data);
        }

        public static idEventArg<Integer> toArg(int data) {
            return new idEventArg(D_EVENT_INTEGER, data);
        }

        public static idEventArg<Float> toArg(float data) {
            return new idEventArg(D_EVENT_FLOAT, data);
        }

        public static idEventArg<idVec3> toArg(idVec3 data) {
            return new idEventArg(D_EVENT_VECTOR, data);
        }

        public static idEventArg<idStr> toArg(idStr data) {
            return new idEventArg(D_EVENT_STRING, data);
        }

        public static idEventArg<String> toArg(String data) {
            return new idEventArg(D_EVENT_STRING, data);
        }

        public static idEventArg<idEntity> toArg(idEntity data) {
            return new idEventArg(D_EVENT_ENTITY, data);
        }

        public static idEventArg<trace_s> toArg(trace_s data) {
            return new idEventArg(D_EVENT_TRACE, data);
        }
    };

    public static class idAllocError extends neo.idlib.Lib.idException {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public idAllocError(final String text /*= ""*/) {
            super(text);
        }
    };
//    /*
//================
//ABSTRACT_PROTOTYPE
//
//This macro must be included in the definition of any abstract subclass of idClass.
//It prototypes variables used in class instanciation and type checking.
//Use this on single inheritance abstract classes only.
//================
//*/
//#define ABSTRACT_PROTOTYPE( nameofclass )								\
//public:																	\
//	static	idTypeInfo						Type;						\
//	static	idClass							*CreateInstance( void );	\
//	virtual	idTypeInfo						*GetType( void ) const;		\
//	static	idEventFunc<nameofclass>		eventCallbacks[]
//
///*
//================
//ABSTRACT_DECLARATION
//
//This macro must be included in the code to properly initialize variables
//used in type checking.  It also defines the list of events that the class
//responds to.  Take special care to ensure that the proper superclass is
//indicated or the run-time tyep information will be incorrect.  Use this
//on abstract classes only.
//================
//*/
//#define ABSTRACT_DECLARATION( nameofsuperclass, nameofclass )										\
//	idTypeInfo nameofclass::Type( #nameofclass, #nameofsuperclass,									\
//		( idEventFunc<idClass> * )nameofclass::eventCallbacks, nameofclass::CreateInstance, ( void ( idClass::* )( void ) )&nameofclass::Spawn,	\
//		( void ( idClass::* )( idSaveGame * ) const )&nameofclass::Save, ( void ( idClass::* )( idRestoreGame * ) )&nameofclass::Restore );	\
//	idClass *nameofclass::CreateInstance( void ) {													\
//		gameLocal.Error( "Cannot instanciate abstract class %s.", #nameofclass );					\
//		return NULL;																				\
//	}																								\
//	idTypeInfo *nameofclass::GetType( void ) const {												\
//		return &( nameofclass::Type );																\
//	}																								\
//	idEventFunc<nameofclass> nameofclass::eventCallbacks[] = {
//
//typedef void ( idClass::*classSpawnFunc_t )( void );
//
//class idSaveGame;
//class idRestoreGame;

    public static abstract class idClass/*<nameOfClass>*/ {

        //        public static final idTypeInfo Type = null;
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.put(EV_Remove, (eventCallback_t0<idClass>) idClass::Event_Remove);
            eventCallbacks.put(EV_SafeRemove, (eventCallback_t0<idClass>) idClass::Event_SafeRemove);
        }

        //
        private static boolean            initialized = false;
        // alphabetical order
        private static idList<idTypeInfo> types       = new idList<>();
        // typenum order
        private static idList<idTypeInfo> typenums    = new idList<>();
        private static int                typeNumBits = 0;
        private static int                memused     = 0;
        private static int                numobjects  = 0;
        //
        //

        public abstract idClass CreateInstance();

        public abstract java.lang.Class/*idTypeInfo*/ GetType();

        public abstract eventCallback_t getEventCallBack(idEventDef event);

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

// #ifdef ID_REDIRECT_NEWDELETE
// #undef new
// #endif
//public	Object						operator new( size_t );
//public	Object						operator new( size_t s, int, int, char *, int );
//public	void						operator delete( void * );
//public	void						operator delete( void *, int, int, char *, int );
// #ifdef ID_REDIRECT_NEWDELETE
// #define new ID_DEBUG_NEW
// #endif

        // virtual						~idClass();
        protected void _deconstructor() {
            idEvent.CancelEvents(this);
        }

        public void Spawn() {
        }

        public void CallSpawn() {
            throw new TODO_Exception();
//            java.lang.Class/*idTypeInfo*/ type;
//
//            type = GetType();
//            CallSpawnFunc(type);
        }

        /*
         ================
         idClass::IsType

         Checks if the object's class is a subclass of the class defined by the
         passed in idTypeInfo.
         ================
         */
        public boolean IsType(final java.lang.Class/*idTypeInfo*/ superclass) {
            java.lang.Class/*idTypeInfo*/ subclass;

            subclass = this.getClass();
            return superclass.isAssignableFrom(subclass);
        }

        /*
         ================
         idClass::GetClassname

         Returns the text classname of the object.
         ================
         */
        public String GetClassname() {
            return this.getClass().getSimpleName();
        }

        /*
         ================
         idClass::GetSuperclass

         Returns the text classname of the superclass.
         ================
         */
        public String GetSuperclass() {
            throw new TODO_Exception();
//            java.lang.Class/*idTypeInfo*/ cls;
//
//            cls = GetType();
//            return cls.superclass;
        }

        public void FindUninitializedMemory() {
//#ifdef ID_DEBUG_UNINITIALIZED_MEMORY
//	unsigned long *ptr = ( ( unsigned long * )this ) - 1;
//	int size = *ptr;
//	assert( ( size & 3 ) == 0 );
//	size >>= 2;
//	for ( int i = 0; i < size; i++ ) {
//		if ( ptr[i] == 0xcdcdcdcd ) {
//			const char *varName = GetTypeVariableName( GetClassname(), i << 2 );
//			gameLocal.Warning( "type '%s' has uninitialized variable %s (offset %d)", GetClassname(), varName, i << 2 );
//		}
//	}
//#endif
        }

        public void Save(idSaveGame savefile) {
        }

        public void Restore(idRestoreGame savefile) {
        }

        public boolean RespondsTo(final idEventDef ev) {
            return getEventCallBack(ev) != null;//HACKME::7
//            throw new TODO_Exception();
//            final idTypeInfo c;
//
//            assert (idEvent.initialized);
//            c = GetType();
//            return c.RespondsTo(ev);
        }

        public boolean PostEventMS(final idEventDef ev, int time) {
            return PostEventArgs(ev, time, 0);
        }

        public boolean PostEventMS(final idEventDef ev, float time, Object arg1) {
            return PostEventArgs(ev, (int) time, 1, toArg(arg1));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2) {
            return PostEventArgs(ev, time, 2, toArg(arg1), toArg(arg2));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3) {
            return PostEventArgs(ev, time, 3, toArg(arg1), toArg(arg2), toArg(arg3));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3, Object arg4) {
            return PostEventArgs(ev, time, 4, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            return PostEventArgs(ev, time, 5, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            return PostEventArgs(ev, time, 6, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            return PostEventArgs(ev, time, 7, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7));
        }

        public boolean PostEventMS(final idEventDef ev, int time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
            return PostEventArgs(ev, time, 8, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7), toArg(arg8));
        }

        public boolean PostEventSec(final idEventDef ev, float time) {
            return PostEventArgs(ev, (int) SEC2MS(time), 0);
        }

        public boolean PostEventSec(final idEventDef ev, float time, idEventArg arg1) {
            return PostEventArgs(ev, (int) SEC2MS(time), 1, arg1);
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1) {
            return PostEventArgs(ev, (int) SEC2MS(time), 1, toArg(arg1));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2) {
            return PostEventArgs(ev, (int) SEC2MS(time), 2, toArg(arg1), toArg(arg2));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3) {
            return PostEventArgs(ev, (int) SEC2MS(time), 3, toArg(arg1), toArg(arg2), toArg(arg3));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3, Object arg4) {
            return PostEventArgs(ev, (int) SEC2MS(time), 4, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            return PostEventArgs(ev, (int) SEC2MS(time), 5, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            return PostEventArgs(ev, (int) SEC2MS(time), 6, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            return PostEventArgs(ev, (int) SEC2MS(time), 7, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7));
        }

        public boolean PostEventSec(final idEventDef ev, float time, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
            return PostEventArgs(ev, (int) SEC2MS(time), 8, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7), toArg(arg8));
        }

        public boolean ProcessEvent(final idEventDef ev) {
            return ProcessEventArgs(ev, 0);
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1) {
            return ProcessEventArgs(ev, 1, toArg(arg1));
        }
        public boolean ProcessEvent(final idEventDef ev, idEntity arg1) {
            return ProcessEventArgs(ev, 1, toArg(arg1));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2) {
            return ProcessEventArgs(ev, 2, toArg(arg1), toArg(arg2));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3) {
            return ProcessEventArgs(ev, 3, toArg(arg1), toArg(arg2), toArg(arg3));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3, Object arg4) {
            return ProcessEventArgs(ev, 4, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
            return ProcessEventArgs(ev, 5, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
            return ProcessEventArgs(ev, 6, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
            return ProcessEventArgs(ev, 7, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7));
        }

        public boolean ProcessEvent(final idEventDef ev, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
            return ProcessEventArgs(ev, 8, toArg(arg1), toArg(arg2), toArg(arg3), toArg(arg4), toArg(arg5), toArg(arg6), toArg(arg7), toArg(arg8));
        }

        public boolean ProcessEventArgPtr(final idEventDef ev, idEventArg[] data) {
            int num;
            eventCallback_t callback;

            assert (ev != null);
            assert (idEvent.initialized);

            if (g_debugTriggers.GetBool() && (ev == EV_Activate) && IsType(idEntity.class)) {
                final String name;
                if (data[0] != null && ((idClass) data[0].value).IsType(idEntity.class))
                    name = ((idEntity) data[0].value).GetName();
                else
                    name = "NULL";
                gameLocal.Printf("%d: '%s' activated by '%s'\n", gameLocal.framenum, ((idEntity) this).GetName(), name);
            }

            num = ev.GetEventNum();
            callback = this.getEventCallBack(ev);//callback = c.eventMap[num];
            if (callback == null) {
                // we don't respond to this event, so ignore it
                return false;
            }

////
//// #if !CPU_EASYARGS
//// /*
//// on ppc architecture, floats are passed in a seperate set of registers
//// the function prototypes must have matching float declaration
//// http://developer.apple.com/documentation/DeveloperTools/Conceptual/MachORuntime/2rt_powerpc_abi/chapter_9_section_5.html
//// */
//            // switch( ev.GetFormatspecIndex() ) {
//            // case 1 << D_EVENT_MAXARGS :
//            // ( this.*callback )();
//            // break;
//// // generated file - see CREATE_EVENT_CODE
//// #include "Callbacks.cpp"
//            // default:
//            // gameLocal.Warning( "Invalid formatspec on event '%s'", ev.GetName() );
//            // break;
//            // }
//// #else
            assert (D_EVENT_MAXARGS == 8);

            switch (ev.GetNumArgs()) {
                case 0:
//                    callback.run();
//                    break;
//
                case 1:
////		typedef void ( idClass.*eventCallback_1_t )( const int );
////		( this.*( eventCallback_1_t )callback )( data[ 0 ] );
//                    callback.run(data[0]);
//                    break;
//
                case 2:
////		typedef void ( idClass.*eventCallback_2_t )( const int, const int );
////		( this.*( eventCallback_2_t )callback )( data[ 0 ], data[ 1 ] );
//                    callback.run(data[0], data[1]);
//                    break;
//
                case 3:
////		typedef void ( idClass.*eventCallback_3_t )( const int, const int, const int );
////		( this.*( eventCallback_3_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ] );
//                    callback.run(data[0], data[1], data[2]);
//                    break;
//
                case 4:
////		typedef void ( idClass.*eventCallback_4_t )( const int, const int, const int, const int );
////		( this.*( eventCallback_4_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ], data[ 3 ] );
//                    callback.run(data[0], data[1], data[2], data[3]);
//                    break;
//
                case 5:
////		typedef void ( idClass.*eventCallback_5_t )( const int, const int, const int, const int, const int );
////		( this.*( eventCallback_5_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ], data[ 3 ], data[ 4 ] );
//                    callback.run(data[0], data[1], data[2], data[3], data[4]);
//                    break;
//
                case 6:
////		typedef void ( idClass.*eventCallback_6_t )( const int, const int, const int, const int, const int, const int );
////		( this.*( eventCallback_6_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ], data[ 3 ], data[ 4 ], data[ 5 ] );
//                    break;
//
                case 7:
////		typedef void ( idClass.*eventCallback_7_t )( const int, const int, const int, const int, const int, const int, const int );
////		( this.*( eventCallback_7_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ], data[ 3 ], data[ 4 ], data[ 5 ], data[ 6 ] );
//                    callback.run(data[0], data[1], data[2], data[3], data[4], data[5], data[6]);
//                    break;
//
                case 8:
////		typedef void ( idClass.*eventCallback_8_t )( const int, const int, const int, const int, const int, const int, const int, const int );
////		( this.*( eventCallback_8_t )callback )( data[ 0 ], data[ 1 ], data[ 2 ], data[ 3 ], data[ 4 ], data[ 5 ], data[ 6 ], data[ 7 ] );
//                    callback.run(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
                    callback.accept(this, data);
                    break;
//
                default:
                    gameLocal.Warning("Invalid formatspec on event '%s'", ev.GetName());
                    break;
            }

// #endif
            return true;
        }

        public void CancelEvents(final idEventDef ev) {
            idEvent.CancelEvents(this, ev);
        }

        public void Event_Remove() {
            //	delete this;//if only
            if (this instanceof idBFGProjectile) idBFGProjectile.delete((idBFGProjectile) this);
            else if (this instanceof idProjectile) idProjectile.delete((idProjectile) this);
            else if (this instanceof idTrigger_Multi) idTrigger_Multi.delete((idTrigger_Multi) this);
            else if (this instanceof idTarget_Remove) idTarget_Remove.delete((idTarget_Remove) this);
            else if (this instanceof idAI) idAI.delete((idAI) this);
            else if (this instanceof idEntity) idEntity.delete((idEntity) this);
            else if (this instanceof idThread) idThread.delete((idThread) this);
            else throw new TODO_Exception();
        }

        // Static functions
        /*
         ================
         idClass::Init

         Should be called after all idTypeInfos are initialized, so must be called
         manually upon game code initialization.  Tells all the idTypeInfos to initialize
         their event callback table for the associated class.  This should only be called
         once during the execution of the program or DLL.
         ================
         */
        public void Init() {
            INIT();
        }

        public static void INIT() {
            idTypeInfo c;
            int num;

            gameLocal.Printf("Initializing class hierarchy\n");

            if (initialized) {
                gameLocal.Printf("...already initialized\n");
                return;
            }

            // init the event callback tables for all the classes
            for (c = typelist; c != null; c = c.next) {
                c.Init();
            }

            // number the types according to the class hierarchy so we can quickly determine if a class
            // is a subclass of another
            num = 0;
            for (c = classHierarchy.GetNext(); c != null; c = c.node.GetNext(), num++) {
                c.typeNum = num;
                c.lastChild += num;
            }

            // number of bits needed to send types over network
            typeNumBits = idMath.BitsForInteger(num);

            // create a list of the types so we can do quick lookups
            // one list in alphabetical order, one in typenum order
            types.SetGranularity(1);
            types.SetNum(num);
            typenums.SetGranularity(1);
            typenums.SetNum(num);
            num = 0;
            for (c = typelist; c != null; c = c.next, num++) {
                types.oSet(num, c);
                typenums.oSet(c.typeNum, c);
            }

            initialized = true;

            gameLocal.Printf("...%d classes, %d bytes for event callbacks\n", types.Num(), eventCallbackMemory);
        }

        public static void Shutdown() {
            idTypeInfo c;

            for (c = typelist; c != null; c = c.next) {
                c.Shutdown();
            }
            types.Clear();
            typenums.Clear();

            initialized = false;
        }

        /*
         ================
         idClass::GetClass

         Returns the idTypeInfo for the name of the class passed in.  This is a static function
         so it must be called as idClass::GetClass( classname )
         ================
         */@Deprecated
        public static idTypeInfo GetClass(final String name) {
            switch (name) {
                    case "idWorldspawn":
                }
            throw new Deprecation_Exception();
//            idTypeInfo c;
//            int order;
//            int mid;
//            int min;
//            int max;
//
//            if (!initialized) {
//                // idClass::Init hasn't been called yet, so do a slow lookup
//                for (c = typelist; c != null; c = c.next) {
//                    if (NOT(idStr.Cmp(c.classname, name))) {
//                        return c;
//                    }
//                }
//            } else {
//                // do a binary search through the list of types
//                min = 0;
//                max = types.Num() - 1;
//                while (min <= max) {
//                    mid = (min + max) / 2;
//                    c = types.oGet(mid);
//                    order = idStr.Cmp(c.classname, name);
//                    if (0 == order) {
//                        return c;
//                    } else if (order > 0) {
//                        max = mid - 1;
//                    } else {
//                        min = mid + 1;
//                    }
//                }
//            }
//
//            return null;
        }

        public abstract void oSet(idClass oGet);

        /*
         ================
         idClass::DisplayInfo_f
         ================
         */
        public static class DisplayInfo_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new DisplayInfo_f();

            private DisplayInfo_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                gameLocal.Printf("Class memory status: %d bytes allocated in %d objects\n", memused, numobjects);
            }
        };

        /*
         ================
         idClass::ListClasses_f
         ================
         */
        public static class ListClasses_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListClasses_f();

            private ListClasses_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                int i;
                idTypeInfo type;

                gameLocal.Printf("%-24s %-24s %-6s %-6s\n", "Classname", "Superclass", "Type", "Subclasses");
                gameLocal.Printf("----------------------------------------------------------------------\n");

                for (i = 0; i < types.Num(); i++) {
                    type = types.oGet(i);
                    gameLocal.Printf("%-24s %-24s %6d %6d\n", type.classname, type.superclass, type.typeNum, type.lastChild - type.typeNum);
                }

                gameLocal.Printf("...%d classes", types.Num());
            }
        };

        public static idClass CreateInstance(final String name) {
//            idTypeInfo type;
//            idClass obj;
//
//            type = idClass.GetClass(name);
//            if (NOT(type)) {
//                return null;
//            }
//
//            obj = type.CreateInstance();
//            return obj;

            throw new TODO_Exception();
        }

        public static int GetNumTypes() {
            return types.Num();
        }

        public static int GetTypeNumBits() {
            return typeNumBits;
        }

        public static idTypeInfo GetType(int typeNum) {
            idTypeInfo c;

            if (!initialized) {
                for (c = typelist; c != null; c = c.next) {
                    if (c.typeNum == typeNum) {
                        return c;
                    }
                }
            } else if ((typeNum >= 0) && (typeNum < types.Num())) {
                return typenums.oGet(typeNum);
            }

            return null;
        }

        private classSpawnFunc_t CallSpawnFunc(idTypeInfo cls) {
            classSpawnFunc_t func;

            if (cls.zuper != null) {//TODO:rename super
                func = CallSpawnFunc(cls.zuper);
                if (func == cls.Spawn) {
                    // don't call the same function twice in a row.
                    // this can happen when subclasses don't have their own spawn function.
                    return func;
                }
            }

//	( this.*cls.Spawn )();
            cls.Spawn.run();

            return cls.Spawn;
        }

        private boolean PostEventArgs(final idEventDef ev, int time, int numargs, idEventArg... args) {
            java.lang.Class c;
            idEvent event;
//            va_list args;

            assert (ev != null);

            if (!idEvent.initialized) {
                return false;
            }
                                                            
            //TODO:disabled for medicinal reasons
            c = this.getClass();
            if (NOT(this.getEventCallBack(ev))) {
                // we don't respond to this event, so ignore it
                return false;
            }

            // we service events on the client to avoid any bad code filling up the event pool
            // we don't want them processed usually, unless when the map is (re)loading.
            // we allow threads to run fine, though.
            if (gameLocal.isClient && (gameLocal.GameState() != GAMESTATE_STARTUP) && !IsType(idThread.class)) {
                return true;
            }

//            va_start(args, numargs);
            event = idEvent.Alloc(ev, numargs, args);
//            va_end(args);

            //TODO:same as line #755
            event.Schedule(this, c, time);

            return true;
        }

        private boolean ProcessEventArgs(final idEventDef ev, int numargs, idEventArg... args) {
            idTypeInfo c;
            int num;
            idEventArg[] data = new idEventArg[D_EVENT_MAXARGS];
//            va_list args;

            assert (ev != null);
            assert (idEvent.initialized);

            //TODO:same as PostEventArgs
//            c = GetType();
//            num = ev.GetEventNum();
//            if (NOT(c.eventMap[num])) {
//                // we don't respond to this event, so ignore it
//                return false;
//            }

//            va_start(args, numargs);
            idEvent.CopyArgs(ev, numargs, args, data);
//            va_end(args);

            ProcessEventArgPtr(ev, data);

            return true;
        }

        private void Event_SafeRemove() {
            // Forces the remove to be done at a safe time
            PostEventMS(EV_Remove, 0);
        }

        public static void delete(final idClass clazz){
            if (clazz != null) clazz._deconstructor();
        }
    };

    /**
     * *********************************************************************
     *
     * idTypeInfo
     *
     * @deprecated use the native java classes instead.
     * *********************************************************************
     */
    @Deprecated
    public static class idTypeInfo {

        public String classname;
        public String superclass;
//
        public idEventFunc<idClass>[] eventCallbacks;
        public eventCallback_t[] eventMap;
        public idTypeInfo zuper;
        public idTypeInfo next;
        public boolean freeEventMap;
        public int typeNum;
        public int lastChild;
//
        public idHierarchy<idTypeInfo> node;
//
        public classSpawnFunc_t CreateInstance;
        public classSpawnFunc_t Spawn;
        public idClass_Save Save;
        public idClass_Restore Restore;
//
//

        public idTypeInfo(final String classname, final String superclass,
                idEventFunc<idClass>[] eventCallbacks, classSpawnFunc_t CreateInstance, classSpawnFunc_t Spawn,
                idClass_Save Save, idClass_Restore Restore) {

            idTypeInfo type;
            idTypeInfo insert;

            this.classname = classname;
            this.superclass = superclass;
            this.eventCallbacks = eventCallbacks;
            this.eventMap = null;
            this.Spawn = Spawn;
            this.Save = Save;
            this.Restore = Restore;
            this.CreateInstance = CreateInstance;
            this.zuper = idClass.GetClass(superclass);
            this.freeEventMap = false;
            typeNum = 0;
            lastChild = 0;

            // Check if any subclasses were initialized before their superclass
            for (type = typelist; type != null; type = type.next) {
                if ((type.zuper == null) && NOT(idStr.Cmp(type.superclass, this.classname))
                        && idStr.Cmp(type.classname, "idClass") != 0) {
                    type.zuper = this;
                }
            }

            // Insert sorted
            for (insert = typelist; insert != null; insert = insert.next) {
                assert (idStr.Cmp(classname, insert.classname) != 0);
                if (idStr.Cmp(classname, insert.classname) < 0) {
                    next = insert;
                    insert = this;
                    break;
                }
            }
            if (null == insert) {
                insert = this;
                next = null;
            }
        }
        // ~idTypeInfo();

        /*
         ================
         idTypeInfo::Init

         Initializes the event callback table for the class.  Creates a 
         table for fast lookups of event functions.  Should only be called once.
         ================
         */
        public void Init() {
            idTypeInfo c;
            idEventFunc<idClass>[] def;
            int ev;
            int i;
            boolean[] set;
            int num;

            if (eventMap != null) {
                // we've already been initialized by a subclass
                return;
            }

            // make sure our superclass is initialized first
            if (zuper != null && null == zuper.eventMap) {
                zuper.Init();
            }

            // add to our node hierarchy
            if (zuper != null) {
                node.ParentTo(zuper.node);
            } else {
                node.ParentTo(classHierarchy);
            }
            node.SetOwner(this);

            // keep track of the number of children below each class
            for (c = zuper; c != null; c = c.zuper) {
                c.lastChild++;
            }

            // if we're not adding any new event callbacks, we can just use our superclass's table
            if ((null == eventCallbacks || NOT(eventCallbacks[0].event)) && zuper != null) {
                eventMap = zuper.eventMap;
                return;
            }

            // set a flag so we know to delete the eventMap table
            freeEventMap = true;

            // Allocate our new table.  It has to have as many entries as there
            // are events.  NOTE: could save some space by keeping track of the maximum
            // event that the class responds to and doing range checking.
            num = idEventDef.NumEventCommands();
            eventMap = new eventCallback_t[num];
//	memset( eventMap, 0, sizeof( eventCallback_t ) * num );
            eventCallbackMemory += sizeof(eventCallback_t.class) * num;

            // allocate temporary memory for flags so that the subclass's event callbacks
            // override the superclass's event callback
            set = new boolean[num];
//	memset( set, 0, sizeof( bool ) * num );

            // go through the inheritence order and copies the event callback function into
            // a list indexed by the event number.  This allows fast lookups of
            // event functions.
            for (c = this; c != null; c = c.zuper) {
                def = c.eventCallbacks;
                if (null == def) {
                    continue;
                }

                // go through each entry until we hit the NULL terminator
                for (i = 0; def[ i].event != null; i++) {
                    ev = def[ i].event.GetEventNum();

                    if (set[ ev]) {
                        continue;
                    }
                    set[ ev] = true;
                    eventMap[ ev] = def[ i].function;
                }
            }

//	delete[] set;
        }

        /*
         ================
         idTypeInfo::Shutdown

         Should only be called when DLL or EXE is being shutdown.
         Although it cleans up any allocated memory, it doesn't bother to remove itself 
         from the class list since the program is shutting down.
         ================
         */
        public void Shutdown() {
            // free up the memory used for event lookups
            if (eventMap != null) {
//		if ( freeEventMap ) {
//			delete[] eventMap;
//		}
                eventMap = null;
            }
            typeNum = 0;
            lastChild = 0;
        }

        /*
         ================
         idTypeInfo::IsType

         Checks if the object's class is a subclass of the class defined by the 
         passed in idTypeInfo.
         ================
         */
        public boolean IsType(final idTypeInfo type) {
            return ((typeNum >= type.typeNum) && (typeNum <= type.lastChild));
        }

        public boolean IsType(final java.lang.Class type) {
            throw new TODO_Exception();
        }

        public boolean RespondsTo(final idEventDef ev) {
            assert (idEvent.initialized);
            if (null == eventMap[ ev.GetEventNum()]) {
                // we don't respond to this event
                return false;
            }

            return true;
        }
    };
}
