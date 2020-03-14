package neo.Game.Script;

import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.idGameLocal.gameError;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.initializedConstant;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.initializedVariable;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.stackVariable;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.uninitialized;
import static neo.TempDump.btoi;
import static neo.TempDump.btos;
import static neo.TempDump.itob;
import static neo.idlib.Text.Str.va;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import neo.TempDump.CPP_class;
import neo.TempDump.SERiAL;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.framework.File_h.idFile;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Script_Program {

    public static final int MAX_STRING_LEN = 128;
    static final        int MAX_GLOBALS    = 196608;       // in bytes
    static final        int MAX_STRINGS    = 1024;
    static final        int MAX_FUNCS      = 3072;
    static final        int MAX_STATEMENTS = 81920;        // statement_s - 18 bytes last I checked

    //    public enum etype_t {
    static final int       ev_error             = -1;
    static final int       ev_void              = 0;
    static final int       ev_scriptevent       = 1;
    static final int       ev_namespace         = 2;
    static final int       ev_string            = 3;
    static final int       ev_float             = 4;
    static final int       ev_vector            = 5;
    static final int       ev_entity            = 6;
    static final int       ev_field             = 7;
    static final int       ev_function          = 8;
    static final int       ev_virtualfunction   = 9;
    static final int       ev_pointer           = 10;
    static final int       ev_object            = 11;
    static final int       ev_jumpoffset        = 12;
    static final int       ev_argsize           = 13;
    static final int       ev_boolean           = 14;
    //    };
    /* **********************************************************************

     Variable and type defintions

     ***********************************************************************/
    // simple types.  function types are dynamically allocated
    static final idTypeDef type_void            = new idTypeDef(ev_void, "void", 0, null);
    static final idTypeDef type_scriptevent     = new idTypeDef(ev_scriptevent, "scriptevent", 4, null);
    static final idTypeDef type_namespace       = new idTypeDef(ev_namespace, "namespace", 4, null);
    static final idTypeDef type_string          = new idTypeDef(ev_string, "string", MAX_STRING_LEN, null);
    static final idTypeDef type_float           = new idTypeDef(ev_float, "float", 4, null);
    static final idTypeDef type_vector          = new idTypeDef(ev_vector, "vector", 12, null);
    static final idTypeDef type_entity          = new idTypeDef(ev_entity, "entity", 4, null);                    // stored as entity number pointer
    static final idTypeDef type_field           = new idTypeDef(ev_field, "field", 4, null);
    static final idTypeDef type_function        = new idTypeDef(ev_function, "function", 4, type_void);
    static final idTypeDef type_virtualfunction = new idTypeDef(ev_virtualfunction, "virtual function", 4, null);
    static final idTypeDef type_pointer         = new idTypeDef(ev_pointer, "pointer", 4, null);
    static final idTypeDef type_object          = new idTypeDef(ev_object, "object", 4, null);                    // stored as entity number pointer
    static final idTypeDef type_jumpoffset      = new idTypeDef(ev_jumpoffset, "<jump>", 4, null);                // only used for jump opcodes
    static final idTypeDef type_argsize         = new idTypeDef(ev_argsize, "<argsize>", 4, null);                // only used for function call and thread opcodes
    static final idTypeDef type_boolean         = new idTypeDef(ev_boolean, "boolean", 4, null);
    //
    static final idVarDef  def_void             = new idVarDef(type_void);
    static final idVarDef  def_scriptevent      = new idVarDef(type_scriptevent);
    static final idVarDef  def_namespace        = new idVarDef(type_namespace);
    static final idVarDef  def_string           = new idVarDef(type_string);
    static final idVarDef  def_float            = new idVarDef(type_float);
    static final idVarDef  def_vector           = new idVarDef(type_vector);
    static final idVarDef  def_entity           = new idVarDef(type_entity);
    static final idVarDef  def_field            = new idVarDef(type_field);
    static final idVarDef  def_function         = new idVarDef(type_function);
    static final idVarDef  def_virtualfunction  = new idVarDef(type_virtualfunction);
    static final idVarDef  def_pointer          = new idVarDef(type_pointer);
    static final idVarDef  def_object           = new idVarDef(type_object);
    static final idVarDef  def_jumpoffset       = new idVarDef(type_jumpoffset);        // only used for jump opcodes
    static final idVarDef  def_argsize          = new idVarDef(type_argsize);
    static final idVarDef  def_boolean          = new idVarDef(type_boolean);

    static {
        type_void.def = def_void;
        type_scriptevent.def = def_scriptevent;
        type_namespace.def = def_namespace;
        type_string.def = def_string;
        type_float.def = def_float;
        type_vector.def = def_vector;
        type_entity.def = def_entity;
        type_field.def = def_field;
        type_function.def = def_function;
        type_virtualfunction.def = def_virtualfunction;
        type_pointer.def = def_pointer;
        type_object.def = def_object;
        type_jumpoffset.def = def_jumpoffset;
        type_argsize.def = def_argsize;
        type_boolean.def = def_boolean;
    }

    /* **********************************************************************

     function_t

     ***********************************************************************/
    public static class function_t implements SERiAL {

        static final int SIZE
                               = idStr.SIZE
                + CPP_class.Pointer.SIZE//eventdef
                + CPP_class.Pointer.SIZE//def
                + CPP_class.Pointer.SIZE//type
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + idList.SIZE;
        static final int BYTES = SIZE / Byte.SIZE;

        private idStr name = new idStr();
        //
        public idEventDef eventdef;
        public idVarDef   def;
        public idTypeDef  type;
        public int        firstStatement;
        public int        numStatements;//TODO:booleans?
        public int        parmTotal;
        public int        locals;            // total ints of parms + locals
        public int        filenum;           // source file defined in
        public idList<Integer> parmSize = new idList<>();
        //
        //

        public function_t() {
            Clear();
        }

        public int/*size_t*/ Allocated() {
            return name.Allocated() + parmSize.Allocated();
        }

        public void SetName(final String name) {
            this.name.oSet(name);
        }

        public String Name() {
            return name.toString();
        }

        public void Clear() {
            eventdef = null;
            def = null;
            type = null;
            firstStatement = 0;
            numStatements = 0;
            parmTotal = 0;
            locals = 0;
            filenum = 0;
            name.Clear();
            parmSize.Clear();
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            try {
                name.Read(buffer);
                buffer.getInt();//skip
                buffer.getInt();//skip
                buffer.getInt();//skip
                firstStatement = buffer.getInt();
                numStatements = buffer.getInt();
                parmTotal = buffer.getInt();
                locals = buffer.getInt();
                filenum = buffer.getInt();
            } catch (BufferUnderflowException ignore) {
            }
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class /*union*/ eval_s {//TODO:unionize?

        final String[]     stringPtr;
        final float        _float;
        final float[]      vector;
        final function_t[] function;
        final int          _int;
        final int          entity;
        
        eval_s(final String string) {
            this.stringPtr = new String[]{string};
            _float = Float.NaN;
            vector = null;
            function = null;
            _int = entity = Integer.MIN_VALUE;
        }
        eval_s(final float _float) {
            stringPtr = null;
            this._float = _float;
            vector = null;
            function = null;
            _int = entity = Integer.MIN_VALUE;
        }
        eval_s(final float[] vector) {
            stringPtr = null;
            _float = Float.NaN;
            this.vector = vector;
            function = null;
            _int = entity = Integer.MIN_VALUE;
        }
        eval_s(final function_t func) {
            stringPtr = null;
            _float = Float.NaN;
            vector = null;
            this.function = new function_t[]{func};
            _int = entity = Integer.MIN_VALUE;
        }
        eval_s(final int val) {
            stringPtr = null;
            _float = Float.NaN;
            vector = null;
            function = null;
            this._int = this.entity = val;
        }
    }

    /* **********************************************************************

     idTypeDef

     Contains type information for variables and functions.

     ***********************************************************************/
    public static final class idTypeDef {
        public static final int BYTES = Integer.BYTES * 8;//TODO:<-

        private int/*etype_t*/ type;
        private idStr          name;
        private int            size;
        //
        // function types are more complex
        private idTypeDef      auxType;  // return type
        private idList<idTypeDef>  parmTypes = new idList<>();
        private idStrList          parmNames = new idStrList();
        private idList<function_t> functions = new idList<>();
        //
        public idVarDef def;        // a def that points to this type
//
//

        public idTypeDef(final idTypeDef other) {
            this.oSet(other);
        }

        public idTypeDef(int/*etype_t*/ etype, idVarDef edef, final String ename, int esize, idTypeDef aux) {
            name = new idStr(ename);
            type = etype;
            def = edef;
            size = esize;
            auxType = aux;

            parmTypes.SetGranularity(1);
            parmNames.SetGranularity(1);
            functions.SetGranularity(1);
        }

        public idTypeDef(int/*etype_t*/ etype, final String ename, int esize, idTypeDef aux) {
            name = new idStr(ename);
            type = etype;
            def = new idVarDef(this);
            size = esize;
            auxType = aux;

            parmTypes.SetGranularity(1);
            parmNames.SetGranularity(1);
            functions.SetGranularity(1);
        }

        public void oSet(final idTypeDef other) {
            type = other.type;
            def = other.def;
            name = other.name;
            size = other.size;
            auxType = other.auxType;
            parmTypes = other.parmTypes;
            parmNames = other.parmNames;
            functions = other.functions;
        }

        public int/*size_t*/ Allocated() {
            int/*size_t*/ memsize;
            int i;

            memsize = name.Allocated() + parmTypes.Allocated() + parmNames.Allocated() + functions.Allocated();
            for (i = 0; i < parmTypes.Num(); i++) {
                memsize += parmNames.oGet(i).Allocated();
            }

            return memsize;
        }

        /*
         ================
         idTypeDef::Inherits

         Returns true if basetype is an ancestor of this type.
         ================
         */
        public boolean Inherits(final idTypeDef basetype) {
            idTypeDef superType;

            if (type != ev_object) {
                return false;
            }

            if (this == basetype) {
                return true;
            }
            for (superType = auxType; superType != null; superType = superType.auxType) {
                if (superType == basetype) {
                    return true;
                }
            }

            return false;
        }

        /*
         ================
         idTypeDef::MatchesType

         Returns true if both types' base types and parameters match
         ================
         */
        public boolean MatchesType(final idTypeDef matchtype) {
            int i;

            if (this.equals(matchtype)) {
                return true;
            }

            if ((type != matchtype.type) || (auxType != matchtype.auxType)) {
                return false;
            }

            if (parmTypes.Num() != matchtype.parmTypes.Num()) {
                return false;
            }

            for (i = 0; i < matchtype.parmTypes.Num(); i++) {
                if (!parmTypes.oGet(i).equals(matchtype.parmTypes.oGet(i))) {
                    return false;
                }
            }

            return true;
        }

        /*
         ================
         idTypeDef::MatchesVirtualFunction

         Returns true if both functions' base types and parameters match
         ================
         */
        public boolean MatchesVirtualFunction(final idTypeDef matchfunc) {
            int i;

            if (this.equals(matchfunc)) {
                return true;
            }

            if ((type != matchfunc.type) || (auxType != matchfunc.auxType)) {
                return false;
            }

            if (parmTypes.Num() != matchfunc.parmTypes.Num()) {
                return false;
            }

            if (parmTypes.Num() > 0) {
                if (!parmTypes.oGet(0).Inherits(matchfunc.parmTypes.oGet(0))) {
                    return false;
                }
            }

            for (i = 1; i < matchfunc.parmTypes.Num(); i++) {
                if (!parmTypes.oGet(i).equals(matchfunc.parmTypes.oGet(i))) {
                    return false;
                }
            }

            return true;
        }

        /*
         ================
         idTypeDef::AddFunctionParm

         Adds a new parameter for a function type.
         ================
         */
        public void AddFunctionParm(idTypeDef parmtype, final String name) {
            if (type != ev_function) {
                throw new idCompileError("idTypeDef::AddFunctionParm : tried to add parameter on non-function type");
            }

            parmTypes.Append(parmtype);
            idStr parmName = parmNames.Alloc();
            parmName.oSet(name);
        }

        /*
         ================
         idTypeDef::AddField

         Adds a new field to an object type.
         ================
         */
        public void AddField(idTypeDef fieldtype, final String name) {
            if (type != ev_object) {
                throw new idCompileError("idTypeDef::AddField : tried to add field to non-object type");
            }

            parmTypes.Append(fieldtype);
            idStr parmName = parmNames.Alloc();
            parmName.oSet(name);

            if (fieldtype.FieldType().Inherits(type_object)) {
                size += type_object.Size();
            } else {
                size += fieldtype.FieldType().Size();
            }
        }

        public void SetName(final String newname) {
            name.oSet(newname);
        }

        public String Name() {
            return name.toString();
        }

        public int/*etype_t*/ Type() {
            return type;
        }

        public int Size() {
            return size;
        }

        /*
         ================
         idTypeDef::SuperClass

         If type is an object, then returns the object's superclass
         ================
         */
        public idTypeDef SuperClass() {
            if (type != ev_object) {
                throw new idCompileError("idTypeDef::SuperClass : tried to get superclass of a non-object type");
            }

            return auxType;
        }

        /*
         ================
         idTypeDef::ReturnType

         If type is a function, then returns the function's return type
         ================
         */
        public idTypeDef ReturnType() {
            if (type != ev_function) {
                throw new idCompileError("idTypeDef::ReturnType: tried to get return type on non-function type");
            }

            return auxType;
        }

        /*
         ================
         idTypeDef::SetReturnType

         If type is a function, then sets the function's return type
         ================
         */
        public void SetReturnType(idTypeDef returntype) {
            if (type != ev_function) {
                throw new idCompileError("idTypeDef::SetReturnType: tried to set return type on non-function type");
            }

            auxType = returntype;
        }

        /*
         ================
         idTypeDef::FieldType

         If type is a field, then returns it's type
         ================
         */
        public idTypeDef FieldType() {
            if (type != ev_field) {
                throw new idCompileError("idTypeDef::FieldType: tried to get field type on non-field type");
            }

            return auxType;
        }

        /*
         ================
         idTypeDef::SetFieldType

         If type is a field, then sets the function's return type
         ================
         */
        public void SetFieldType(idTypeDef fieldtype) {
            if (type != ev_field) {
                throw new idCompileError("idTypeDef::SetFieldType: tried to set return type on non-function type");
            }

            auxType = fieldtype;
        }

        /*
         ================
         idTypeDef::PointerType

         If type is a pointer, then returns the type it points to
         ================
         */
        public idTypeDef PointerType() {
            if (type != ev_pointer) {
                throw new idCompileError("idTypeDef::PointerType: tried to get pointer type on non-pointer");
            }

            return auxType;
        }

        /*
         ================
         idTypeDef::SetPointerType

         If type is a pointer, then sets the pointer's type
         ================
         */
        public void SetPointerType(idTypeDef pointertype) {
            if (type != ev_pointer) {
                throw new idCompileError("idTypeDef::SetPointerType: tried to set type on non-pointer");
            }

            auxType = pointertype;
        }

        public int NumParameters() {
            return parmTypes.Num();
        }

        public idTypeDef GetParmType(int parmNumber) {
            assert (parmNumber >= 0);
            assert (parmNumber < parmTypes.Num());
            return parmTypes.oGet(parmNumber);
        }

        public String GetParmName(int parmNumber) {
            assert (parmNumber >= 0);
            assert (parmNumber < parmTypes.Num());
            return parmNames.oGet(parmNumber).toString();
        }

        public int NumFunctions() {
            return functions.Num();
        }

        public int GetFunctionNumber(final function_t func) {
            int i;

            for (i = 0; i < functions.Num(); i++) {
                if (functions.oGet(i).equals(func)) {
                    return i;
                }
            }
            return -1;
        }

        public function_t GetFunction(int funcNumber) {
            assert (funcNumber >= 0);
            assert (funcNumber < functions.Num());
            return functions.oGet(funcNumber);
        }

        public void AddFunction(final function_t func) {
            int i;

            for (i = 0; i < functions.Num(); i++) {
                if (functions.oGet(i).def.Name().equals(func.def.Name())) {
                    if (func.def.TypeDef().MatchesVirtualFunction(functions.oGet(i).def.TypeDef())) {
                        functions.oSet(i, func);
                        return;
                    }
                }
            }
            functions.Append(func);
        }
    }

    /* **********************************************************************

     idScriptObject

     In-game representation of objects in scripts.  Use the idScriptVariable template
     (below) to access variables.

     ***********************************************************************/
    public static class idScriptObject implements SERiAL {

        private idTypeDef  type;
        //
        public  ByteBuffer data;
        public  int        offset;
//
//

        public idScriptObject() {
            data = null;
            type = type_object;
        }
        // ~idScriptObject();

        public void Save(idSaveGame savefile) {            // archives object for save game file
            int/*size_t*/ size;

            if (type.equals(type_object) && data == null) {
                // Write empty string for uninitialized object
                savefile.WriteString("");
            } else {
                savefile.WriteString(type.Name());
                size = type.Size();
                savefile.WriteInt(size);
                savefile.Write(data, size);
            }
        }

        public void Restore(idRestoreGame savefile) {            // unarchives object from save game file
            idStr typeName = new idStr();
            int/*size_t*/[] size = {0};

            savefile.ReadString(typeName);

            // Empty string signals uninitialized object
            if (typeName.Length() == 0) {
                return;
            }

            if (!SetType(typeName.toString())) {
                savefile.Error("idScriptObject::Restore: failed to restore object of type '%s'.", typeName.toString());
            }

            savefile.ReadInt(size);
            if (size[0] != type.Size()) {
                savefile.Error("idScriptObject::Restore: size of object '%s' doesn't match size in save game.", typeName);
            }

            savefile.Read(data, size[0]);
        }

        public void Free() {
//            if (data != null) {
//                Mem_Free(data);
//            }

            data = null;
            type = type_object;
        }

        /*
         ============
         idScriptObject::SetType

         Allocates an object and initializes memory.
         ============
         */
        public boolean SetType(final String typeName) {
            int/*size_t*/ size;
            idTypeDef newType;

            // lookup the type
            newType = gameLocal.program.FindType(typeName);

            // only allocate memory if the object type changes
            if (newType != type) {
                Free();
                if (null == newType) {
                    gameLocal.Warning("idScriptObject::SetType: Unknown type '%s'", typeName);
                    return false;
                }

                if (!newType.Inherits(type_object)) {
                    gameLocal.Warning("idScriptObject::SetType: Can't create object of type '%s'.  Must be an object type.", newType.Name());
                    return false;
                }

                // set the type
                type = newType;

                // allocate the memory
                size = type.Size();
                data = ByteBuffer.allocate(size);// Mem_Alloc(size);
            }

            // init object memory
            ClearObject();

            return true;
        }

        /*
         ============
         idScriptObject::ClearObject

         Resets the memory for the script object without changing its type.
         ============
         */
        public void ClearObject() {
            int/*size_t*/ size;

            if (!type.equals(type_object)) {
                // init object memory
                size = type.Size();
//		memset( data, 0, size );
                data.clear();
            }
        }

        public boolean HasObject() {
            return (!type.equals(type_object));
        }

        public idTypeDef GetTypeDef() {
            return type;
        }

        public String GetTypeName() {
            return type.Name();
        }

        public function_t GetConstructor() {
            function_t func;

            func = GetFunction("init");
            return func;
        }

        public function_t GetDestructor() {
            function_t func;

            func = GetFunction("destroy");
            return func;
        }

        public function_t GetFunction(final String name) {
            function_t func;

            if (type.equals(type_object)) {
                return null;
            }

            func = gameLocal.program.FindFunction(name, type);
            return func;
        }

        public ByteBuffer GetVariable(final String name, int/*etype_t*/ etype) {
            int i;
            int pos;
            idTypeDef t;
            idTypeDef parm;

            if (type.equals(type_object)) {
                return null;
            }

            t = type;
            do {
                if (!t.SuperClass().equals(type_object)) {
                    pos = t.SuperClass().Size();
                } else {
                    pos = 0;
                }
                for (i = 0; i < t.NumParameters(); i++) {
                    parm = t.GetParmType(i);
                    if (t.GetParmName(i).equals(name)) {
                        if (etype != parm.FieldType().Type()) {
                            return null;
                        }
                        return ((ByteBuffer) data.position(pos)).slice();
                    }

                    if (parm.FieldType().Inherits(type_object)) {
                        pos += type_object.Size();
                    } else {
                        pos += parm.FieldType().Size();
                    }
                }
                t = t.SuperClass();
            } while (t != null && (!t.equals(type_object)));

            return null;
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /* **********************************************************************

     idScriptVariable

     Helper template that handles looking up script variables stored in objects.
     If the specified variable doesn't exist, or is the wrong data type, idScriptVariable
     will cause an error.

     ***********************************************************************/
    static class idScriptVariable<type, returnType> {

        protected final int/*etype_t*/ etype;
        private         ByteBuffer     data;
//
//

//        public idScriptVariable() {
//            etype = null;
//            data = null;
//        }
//        
        public idScriptVariable(int/*etype_t*/ etype) {
            this.etype = etype;
            data = null;
        }

        public boolean IsLinked() {
            return (data != null);
        }

        public void Unlink() {
            data = null;
        }

        public void LinkTo(idScriptObject obj, final String name) {
            data = obj.GetVariable(name, etype);//TODO:convert bytes to type
            if (null == data) {
                gameError("Missing '%s' field in script object '%s'", name, obj.GetTypeName());
            }
        }

        public idScriptVariable oSet(final returnType value) {
            // check if we attempt to access the object before it's been linked
            assert (data != null);

            // make sure we don't crash if we don't have a pointer
            if (data != null) {
                final int pos = data.position();
                switch (etype) {
                    case ev_boolean:
                        data.put((byte) btoi((Boolean) value));
                        break;
                    case ev_float:
                        data.putFloat((Float) value);
                        break;
                }
                data.position(pos);
            }
            return this;
        }

        public returnType _() {
            // check if we attempt to access the object before it's been linked
            assert (data != null);

            // make sure we don't crash if we don't have a pointer
            if (data != null) {
                final int pos = data.position();
                switch (etype) {
                    case ev_boolean:
                        return (returnType) (Boolean) itob(data.get(pos));
                    case ev_float:
                        return (returnType) (Float) data.getFloat(pos);
                    default:
                        return null;
                }
            } else {
                // reasonably safe value
                return null;
            }
        }

        public void _(returnType bla) {
            this.oSet(bla);
        }
    };

    /* **********************************************************************

     Script object variable access template instantiations

     These objects will automatically handle looking up of the current value
     of a variable in a script object.  They can be stored as part of a class
     for up-to-date values of the variable, or can be used in functions to
     sample the data for non-dynamic values.

     ***********************************************************************/
    public static class idScriptBool extends idScriptVariable<Boolean, Boolean> {
        public idScriptBool() {
            super(ev_boolean);
        }
    };

    public static class idScriptFloat extends idScriptVariable<Float, Float> {
        public idScriptFloat() {
            super(ev_float);
        }
    };

    private static class idScriptInt extends idScriptVariable<Float, Integer> {
        public idScriptInt() {
            super(ev_float);
        }
    };

    private static class idScriptVector extends idScriptVariable<idVec3, idVec3> {
        public idScriptVector() {
            super(ev_vector);
        }
    };

    private static class idScriptString extends idScriptVariable<idStr, String> {
        public idScriptString() {
            super(ev_string);
        }
    };

    /* **********************************************************************

     idCompileError

     Causes the compiler to exit out of compiling the current function and
     display an error message with line and file info.

     ***********************************************************************/
    public static class idCompileError extends idException {

        public idCompileError(final String text) {
            super(text);
        }
    }

    ;

    /* **********************************************************************

     idVarDef

     Define the name, type, and location of variables, functions, and objects
     defined in script.

     ***********************************************************************/
    static class /*union*/ varEval_s {
        static final int BYTES = Float.BYTES;


        idScriptObject objectPtrPtr;
        String       stringPtr;
//        final         float[]        floatPtr;
        idVec3       vectorPtr = new idVec3();
        function_t   functionPtr;
//        final         int[]          intPtr;
//        final         ByteBuffer     bytePtr;
//        private int virtualFunction;
//        private int jumpOffset;
//        private int stackOffset;		// offset in stack for local variables
//        private int argSize;
        varEval_s    evalPtr;
//        private int ptrOffset;
        private ByteBuffer primitive = ByteBuffer.allocate(Float.BYTES * 3).order(ByteOrder.LITTLE_ENDIAN);
        private int offset;

        public int getVirtualFunction() {
            return getPrimitive();
        }

        public int getJumpOffset() {
            return getPrimitive();
        }

        public int getStackOffset() {
            return getPrimitive();
        }

        public int getArgSize() {
            return getPrimitive();
        }

        public int getPtrOffset() {
            return getPrimitive();
        }

        private int getPrimitive() {
            return primitive.getInt(0);
        }

        public void setIntPtr(final int val) {
            setPrimitive(val);
        }

        public void setIntPtr(final byte[] val, int offset) {
            setBytePtr(ByteBuffer.wrap(val), offset);
        }

        public void setEntityNumberPtr(final int val) {
            setPrimitive(val);
        }

        public void setFloatPtr(final float val) {
            primitive.putFloat(0, val);
        }

        public void setVirtualFunction(final int val) {
            setPrimitive(val);
        }

        public void setJumpOffset(final int val) {
            setPrimitive(val);
        }

        public void setStackOffset(final int val) {
            setPrimitive(val);
        }

        public void setArgSize(final int val) {
            setPrimitive(val);
        }

        public void setPtrOffset(final int val) {
            setPrimitive(val);
        }

        private void setPrimitive(final int val) {
            primitive.putInt(0, val);
        }

//        void bytePtr(ByteBuffer data, int ptrOffset) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
        
        void setVectorPtr(idVec3 vector) {
            setVectorPtr(vector.ToFloatPtr());
        }

        void setVectorPtr(float[] vector) {
            vectorPtr = new idVec3(vector);
            primitive.putFloat(0, vector[0]);
            primitive.putFloat(4, vector[1]);
            primitive.putFloat(8, vector[2]);
        }

        idVec3 getVectorPtr() {
            vectorPtr.oSet(0, primitive.getFloat(0));
            vectorPtr.oSet(1, primitive.getFloat(4));
            vectorPtr.oSet(2, primitive.getFloat(8));
            return vectorPtr;
        }

        int getIntPtr() {
            return primitive.getInt(0);
        }

        float getFloatPtr() {
            return primitive.getFloat(0);
        }

        int getEntityNumberPtr() {
            return getIntPtr();
        }

        void setBytePtr(ByteBuffer bytes, int offset) {
            this.offset = offset;
            primitive = (((ByteBuffer) bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN).position(offset)).slice()).order(ByteOrder.LITTLE_ENDIAN);
        }

        void setBytePtr(byte[] bytes, int offset) {
            setBytePtr(ByteBuffer.wrap(bytes), offset);
        }

        public void setStringPtr(ByteBuffer data, int offset) {
            stringPtr = btos(data.array(), offset);
        }

        public void setString(final String string) {//TODO:clean up all these weird string pointers
            primitive.put(string.getBytes()).rewind();
        }

        public void setEvalPtr(final int entityNumberIndex) {
            this.setEntityNumberPtr(entityNumberIndex);
        }
    }

    static class idVarDef {
        // friend class idVarDefName;
        static final int BYTES
                = Integer.BYTES
                + varEval_s.BYTES
                + Integer.BYTES
                + Integer.BYTES;

        public int       num;
        public varEval_s value;
        public idVarDef  scope;        // function, namespace, or object the var was defined in
        public int       numUsers;     // number of users if this is a constant
//

        public enum initialized_t {
            uninitialized, initializedVariable, initializedConstant, stackVariable
        }
        //
        public  initialized_t initialized;
        //
        private idTypeDef     typeDef;
        private idVarDefName  name;    // name of this var
        private idVarDef      next;    // next var with the same name
//
//

        public idVarDef() {
            this(null);
        }

        public idVarDef(idTypeDef typeptr /*= NULL*/) {
            typeDef = typeptr;
            num = 0;
            scope = null;
            numUsers = 0;
            initialized = uninitialized;
//	memset( &value, 0, sizeof( value ) );
            name = null;
            next = null;
        }

        // ~idVarDef();
        public void close() {
            if (name != null) {
                name.RemoveDef(this);
            }
        }

        public String Name() {
            return name.Name();
        }

        public String GlobalName() {
            if (!scope.equals(def_namespace)) {
                return va("%s::%s", scope.GlobalName(), name.Name());
            } else {
                return name.Name();
            }
        }

        public void SetTypeDef(idTypeDef _type) {
            typeDef = _type;
        }

        public idTypeDef TypeDef() {
            return typeDef;
        }

        public int/*etype_t*/ Type() {
            return (typeDef != null) ? typeDef.Type() : ev_void;
        }

        public int DepthOfScope(final idVarDef otherScope) {
            idVarDef def;
            int depth;

            depth = 1;
            for (def = otherScope; def != null; def = def.scope) {
                if (def.equals(scope)) {
                    return depth;
                }
                depth++;
            }

            return 0;
        }

        public void SetFunction(function_t func) {
            assert (typeDef != null);
            initialized = initializedConstant;
            assert (typeDef.Type() == ev_function);
            value = new varEval_s();
            value.functionPtr = func;
        }

        public void SetObject(idScriptObject object) {
            assert (typeDef != null);
            initialized = initialized;
            assert (typeDef.Inherits(type_object));
            value = new varEval_s();
            value.objectPtrPtr = object;
        }

        public void SetValue(final eval_s _value, boolean constant) {
            assert (typeDef != null);
            if (constant) {
                initialized = initializedConstant;
            } else {
                initialized = initializedVariable;
            }

            switch (typeDef.Type()) {
                case ev_pointer:
                case ev_boolean:
                case ev_field:
                    value.setIntPtr(_value._int);
                    break;
                case ev_jumpoffset:
                    value.setJumpOffset(_value._int);
                    break;
                case ev_argsize:
                    value.setArgSize(_value._int);
                    break;
                case ev_entity:
                    value.setEntityNumberPtr(_value.entity);
                    break;

                case ev_string:
                    value.stringPtr = _value.stringPtr[0];//idStr.Copynz(value.stringPtr, _value.stringPtr, MAX_STRING_LEN);
                    break;

                case ev_float:
                    value.setFloatPtr(_value._float);
                    break;

                case ev_vector:
                    value.setVectorPtr(_value.vector);
                    break;

                case ev_function:
                    value.functionPtr = _value.function[0];
                    break;

                case ev_virtualfunction:
                    value.setVirtualFunction(_value._int);
                    break;

                case ev_object:
                    value.setEntityNumberPtr(_value.entity);
                    break;

                default:
                    throw new idCompileError(va("weird type on '%s'", Name()));
            }
        }

        public void SetString(final String string, boolean constant) {
            if (constant) {
                initialized = initializedConstant;
            } else {
                initialized = initializedVariable;
            }

            assert (typeDef != null && (typeDef.Type() == ev_string));
            value.stringPtr = string;
        }

        public idVarDef Next() {
            return next;
        }        // next var def with same name

        public void PrintInfo(idFile file, int instructionPointer) {
            statement_s jumpst;
            int jumpto;
            int/*etype_t*/ etype;

            if (initialized == initializedConstant) {
                file.Printf("const ");
            }

            etype = typeDef.Type();
            switch (etype) {
                case ev_jumpoffset:
                    jumpto = instructionPointer + value.getJumpOffset();
                    jumpst = gameLocal.program.GetStatement(jumpto);
                    file.Printf("address %d [%s(%d)]", jumpto, gameLocal.program.GetFilename(jumpst.file), jumpst.linenumber);
                    break;

                case ev_function:
                    if (value.functionPtr.eventdef != null) {
                        file.Printf("event %s", GlobalName());
                    } else {
                        file.Printf("function %s", GlobalName());
                    }
                    break;

                case ev_field:
                    file.Printf("field %d", value.getPtrOffset());
                    break;

                case ev_argsize:
                    file.Printf("args %d", value.getArgSize());
                    break;

                default:
                    file.Printf("%s ", typeDef.Name());
                    if (initialized == initializedConstant) {
                        switch (etype) {
                            case ev_string:
                                file.Printf("\"");
                                for (char ch : value.stringPtr.toCharArray()) {
                                    if (idStr.CharIsPrintable(ch)) {
                                        file.Printf("%c", ch);
                                    } else if (ch == '\n') {
                                        file.Printf("\\n");
                                    } else {
                                        file.Printf("\\x%.2x", (int) ch);
                                    }
                                }
                                file.Printf("\"");
                                break;

                            case ev_vector:
                                file.Printf("'%s'", value.getVectorPtr().ToString());
                                break;

                            case ev_float:
                                file.Printf("%f", value.getFloatPtr());
                                break;

                            case ev_virtualfunction:
                                file.Printf("vtable[ %d ]", value.getVirtualFunction());
                                break;

                            default:
                                file.Printf("%d", value.getIntPtr());
                                break;
                        }
                    } else if (initialized == stackVariable) {
                        file.Printf("stack[%d]", value.getStackOffset());
                    } else {
                        file.Printf("global[%d]", num);
                    }
                    break;
            }
        }
    }

    /* **********************************************************************

     idVarDefName

     ***********************************************************************/
    static class idVarDefName {

        private idStr name = new idStr();
        private idVarDef defs;
        //
        //

        public idVarDefName() {
            defs = null;
        }

        public idVarDefName(final String n) {
            name.oSet(n);
            defs = null;
        }

        public String Name() {
            return name.toString();
        }

        public idVarDef GetDefs() {
            return defs;
        }

        public void AddDef(idVarDef def) {
            assert (def.next == null);
            def.name = this;
            def.next = defs;
            defs = def;
        }

        public void RemoveDef(idVarDef def) {
            if (defs.equals(def)) {
                defs = def.next;
            } else {
                for (idVarDef d = defs; d.next != null; d = d.next) {
                    if (d.next.equals(def)) {
                        d.next = def.next;
                        break;
                    }
                }
            }
            def.next = null;
            def.name = null;
        }
    }

    public static class statement_s {

        int /*unsigned short*/ op;
        idVarDef               a;
        idVarDef               b;
        idVarDef               c;
        int                    linenumber;
        int                    file;

        public statement_s() {
        }
    }
}
