package neo.Game.Script;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import neo.Game.Entity.idEntity;
import static neo.Game.GameSys.Event.D_EVENT_ENTITY;
import static neo.Game.GameSys.Event.D_EVENT_ENTITY_NULL;
import static neo.Game.GameSys.Event.D_EVENT_FLOAT;
import static neo.Game.GameSys.Event.D_EVENT_INTEGER;
import static neo.Game.GameSys.Event.D_EVENT_MAXARGS;
import static neo.Game.GameSys.Event.D_EVENT_STRING;
import static neo.Game.GameSys.Event.D_EVENT_TRACE;
import static neo.Game.GameSys.Event.D_EVENT_VECTOR;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Script.Script_Compiler.OP_ADDRESS;
import static neo.Game.Script.Script_Compiler.OP_ADD_F;
import static neo.Game.Script.Script_Compiler.OP_ADD_FS;
import static neo.Game.Script.Script_Compiler.OP_ADD_S;
import static neo.Game.Script.Script_Compiler.OP_ADD_SF;
import static neo.Game.Script.Script_Compiler.OP_ADD_SV;
import static neo.Game.Script.Script_Compiler.OP_ADD_V;
import static neo.Game.Script.Script_Compiler.OP_ADD_VS;
import static neo.Game.Script.Script_Compiler.OP_AND;
import static neo.Game.Script.Script_Compiler.OP_AND_BOOLBOOL;
import static neo.Game.Script.Script_Compiler.OP_AND_BOOLF;
import static neo.Game.Script.Script_Compiler.OP_AND_FBOOL;
import static neo.Game.Script.Script_Compiler.OP_BITAND;
import static neo.Game.Script.Script_Compiler.OP_BITOR;
import static neo.Game.Script.Script_Compiler.OP_BREAK;
import static neo.Game.Script.Script_Compiler.OP_CALL;
import static neo.Game.Script.Script_Compiler.OP_COMP_F;
import static neo.Game.Script.Script_Compiler.OP_CONTINUE;
import static neo.Game.Script.Script_Compiler.OP_DIV_F;
import static neo.Game.Script.Script_Compiler.OP_EQ_E;
import static neo.Game.Script.Script_Compiler.OP_EQ_EO;
import static neo.Game.Script.Script_Compiler.OP_EQ_F;
import static neo.Game.Script.Script_Compiler.OP_EQ_OE;
import static neo.Game.Script.Script_Compiler.OP_EQ_OO;
import static neo.Game.Script.Script_Compiler.OP_EQ_S;
import static neo.Game.Script.Script_Compiler.OP_EQ_V;
import static neo.Game.Script.Script_Compiler.OP_EVENTCALL;
import static neo.Game.Script.Script_Compiler.OP_GE;
import static neo.Game.Script.Script_Compiler.OP_GOTO;
import static neo.Game.Script.Script_Compiler.OP_GT;
import static neo.Game.Script.Script_Compiler.OP_IF;
import static neo.Game.Script.Script_Compiler.OP_IFNOT;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_BOOL;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_ENT;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_F;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_OBJ;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_S;
import static neo.Game.Script.Script_Compiler.OP_INDIRECT_V;
import static neo.Game.Script.Script_Compiler.OP_INT_F;
import static neo.Game.Script.Script_Compiler.OP_LE;
import static neo.Game.Script.Script_Compiler.OP_LT;
import static neo.Game.Script.Script_Compiler.OP_MOD_F;
import static neo.Game.Script.Script_Compiler.OP_MUL_F;
import static neo.Game.Script.Script_Compiler.OP_MUL_FV;
import static neo.Game.Script.Script_Compiler.OP_MUL_V;
import static neo.Game.Script.Script_Compiler.OP_MUL_VF;
import static neo.Game.Script.Script_Compiler.OP_NEG_F;
import static neo.Game.Script.Script_Compiler.OP_NEG_V;
import static neo.Game.Script.Script_Compiler.OP_NE_E;
import static neo.Game.Script.Script_Compiler.OP_NE_EO;
import static neo.Game.Script.Script_Compiler.OP_NE_F;
import static neo.Game.Script.Script_Compiler.OP_NE_OE;
import static neo.Game.Script.Script_Compiler.OP_NE_OO;
import static neo.Game.Script.Script_Compiler.OP_NE_S;
import static neo.Game.Script.Script_Compiler.OP_NE_V;
import static neo.Game.Script.Script_Compiler.OP_NOT_BOOL;
import static neo.Game.Script.Script_Compiler.OP_NOT_ENT;
import static neo.Game.Script.Script_Compiler.OP_NOT_F;
import static neo.Game.Script.Script_Compiler.OP_NOT_S;
import static neo.Game.Script.Script_Compiler.OP_NOT_V;
import static neo.Game.Script.Script_Compiler.OP_OBJECTCALL;
import static neo.Game.Script.Script_Compiler.OP_OBJTHREAD;
import static neo.Game.Script.Script_Compiler.OP_OR;
import static neo.Game.Script.Script_Compiler.OP_OR_BOOLBOOL;
import static neo.Game.Script.Script_Compiler.OP_OR_BOOLF;
import static neo.Game.Script.Script_Compiler.OP_OR_FBOOL;
import static neo.Game.Script.Script_Compiler.OP_PUSH_BTOF;
import static neo.Game.Script.Script_Compiler.OP_PUSH_BTOS;
import static neo.Game.Script.Script_Compiler.OP_PUSH_ENT;
import static neo.Game.Script.Script_Compiler.OP_PUSH_F;
import static neo.Game.Script.Script_Compiler.OP_PUSH_FTOB;
import static neo.Game.Script.Script_Compiler.OP_PUSH_FTOS;
import static neo.Game.Script.Script_Compiler.OP_PUSH_OBJ;
import static neo.Game.Script.Script_Compiler.OP_PUSH_OBJENT;
import static neo.Game.Script.Script_Compiler.OP_PUSH_S;
import static neo.Game.Script.Script_Compiler.OP_PUSH_V;
import static neo.Game.Script.Script_Compiler.OP_PUSH_VTOS;
import static neo.Game.Script.Script_Compiler.OP_RETURN;
import static neo.Game.Script.Script_Compiler.OP_STOREP_BOOL;
import static neo.Game.Script.Script_Compiler.OP_STOREP_BOOLTOF;
import static neo.Game.Script.Script_Compiler.OP_STOREP_BTOS;
import static neo.Game.Script.Script_Compiler.OP_STOREP_ENT;
import static neo.Game.Script.Script_Compiler.OP_STOREP_F;
import static neo.Game.Script.Script_Compiler.OP_STOREP_FLD;
import static neo.Game.Script.Script_Compiler.OP_STOREP_FTOBOOL;
import static neo.Game.Script.Script_Compiler.OP_STOREP_FTOS;
import static neo.Game.Script.Script_Compiler.OP_STOREP_OBJ;
import static neo.Game.Script.Script_Compiler.OP_STOREP_OBJENT;
import static neo.Game.Script.Script_Compiler.OP_STOREP_S;
import static neo.Game.Script.Script_Compiler.OP_STOREP_V;
import static neo.Game.Script.Script_Compiler.OP_STOREP_VTOS;
import static neo.Game.Script.Script_Compiler.OP_STORE_BOOL;
import static neo.Game.Script.Script_Compiler.OP_STORE_BOOLTOF;
import static neo.Game.Script.Script_Compiler.OP_STORE_BTOS;
import static neo.Game.Script.Script_Compiler.OP_STORE_ENT;
import static neo.Game.Script.Script_Compiler.OP_STORE_ENTOBJ;
import static neo.Game.Script.Script_Compiler.OP_STORE_F;
import static neo.Game.Script.Script_Compiler.OP_STORE_FTOBOOL;
import static neo.Game.Script.Script_Compiler.OP_STORE_FTOS;
import static neo.Game.Script.Script_Compiler.OP_STORE_OBJ;
import static neo.Game.Script.Script_Compiler.OP_STORE_OBJENT;
import static neo.Game.Script.Script_Compiler.OP_STORE_S;
import static neo.Game.Script.Script_Compiler.OP_STORE_V;
import static neo.Game.Script.Script_Compiler.OP_STORE_VTOS;
import static neo.Game.Script.Script_Compiler.OP_SUB_F;
import static neo.Game.Script.Script_Compiler.OP_SUB_V;
import static neo.Game.Script.Script_Compiler.OP_SYSCALL;
import static neo.Game.Script.Script_Compiler.OP_THREAD;
import static neo.Game.Script.Script_Compiler.OP_UADD_F;
import static neo.Game.Script.Script_Compiler.OP_UADD_V;
import static neo.Game.Script.Script_Compiler.OP_UAND_F;
import static neo.Game.Script.Script_Compiler.OP_UDECP_F;
import static neo.Game.Script.Script_Compiler.OP_UDEC_F;
import static neo.Game.Script.Script_Compiler.OP_UDIV_F;
import static neo.Game.Script.Script_Compiler.OP_UDIV_V;
import static neo.Game.Script.Script_Compiler.OP_UINCP_F;
import static neo.Game.Script.Script_Compiler.OP_UINC_F;
import static neo.Game.Script.Script_Compiler.OP_UMOD_F;
import static neo.Game.Script.Script_Compiler.OP_UMUL_F;
import static neo.Game.Script.Script_Compiler.OP_UMUL_V;
import static neo.Game.Script.Script_Compiler.OP_UOR_F;
import static neo.Game.Script.Script_Compiler.OP_USUB_F;
import static neo.Game.Script.Script_Compiler.OP_USUB_V;
import static neo.Game.Script.Script_Program.MAX_STRING_LEN;
import static neo.Game.Script.Script_Program.def_namespace;
import static neo.Game.Script.Script_Program.ev_boolean;
import static neo.Game.Script.Script_Program.ev_field;
import static neo.Game.Script.Script_Program.ev_float;
import static neo.Game.Script.Script_Program.ev_string;
import static neo.Game.Script.Script_Program.ev_vector;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idScriptObject;
import neo.Game.Script.Script_Program.idTypeDef;
import neo.Game.Script.Script_Program.idVarDef;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.stackVariable;
import neo.Game.Script.Script_Program.statement_s;
import static neo.Game.Script.Script_Program.type_object;
import neo.Game.Script.Script_Program.varEval_s;
import neo.Game.Script.Script_Thread.idThread;
import static neo.TempDump.NOT;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.btoi;
import static neo.TempDump.btoia;
import static neo.TempDump.btos;
import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.TempDump.sizeof;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Vector.vec3_zero;

/**
 *
 */
public class Script_Interpreter {

    static final int MAX_STACK_DEPTH = 64;
    static final int LOCALSTACK_SIZE = 6144;

    public static class prstack_s {

        int        s;
        function_t f;
        int        stackbase;
    };

    public static class idInterpreter {

        private final prstack_s[] callStack = new prstack_s[MAX_STACK_DEPTH];
        private int callStackDepth;
        private int maxStackDepth;
        //
        private final byte[] localstack = new byte[LOCALSTACK_SIZE];
        private int        localstackUsed;
        private int        localstackBase;
        private int        maxLocalstackUsed;
        //
        private function_t currentFunction;
        private int        instructionPointer;
        //
        private int        popParms;
        private idEventDef multiFrameEvent;
        private idEntity   eventEntity;
        //
        private idThread   thread;
        //
        public  boolean    doneProcessing;
        public  boolean    threadDying;
        public  boolean    terminateOnExit;
        public  boolean    debug;
//
//

        private void PopParms(int numParms) {
            // pop our parms off the stack
            if (localstackUsed < numParms) {
                Error("locals stack underflow\n");
            }

            localstackUsed -= numParms;
        }

        private void PushString(final String string) {
            if (localstackUsed + MAX_STRING_LEN > LOCALSTACK_SIZE) {
                Error("PushString: locals stack overflow\n");
            }
//            idStr.Copynz(localstack[localstackUsed], string, MAX_STRING_LEN);
            System.arraycopy(localstack, localstackUsed, string.getBytes(), 0, MAX_STRING_LEN);
            localstackUsed += MAX_STRING_LEN;
        }

        private void Push(int value) {
            if (localstackUsed /*+ sizeof( int )*/ > LOCALSTACK_SIZE) {
                Error("Push: locals stack overflow\n");
            }
            localstack[localstackUsed] = (byte) value;
            localstackUsed += 4;//sizeof(int);
        }

        static char[] text = new char[32];

        private String FloatToString(float value) {

            if (value == (float) (int) value) {
                text = String.format("%d", (int) value).toCharArray();
            } else {
                text = String.format("%f", value).toCharArray();
            }
            return ctos(text);
        }

        private void AppendString(idVarDef def, final String from) {
            if (def.initialized == stackVariable) {
//                idStr.Append(localstack[localstackBase + def.value.stackOffset], MAX_STRING_LEN, from);
                System.arraycopy(from.getBytes(), 0, localstack, localstackBase + def.value.getStackOffset(), MAX_STRING_LEN);
            } else {
                def.value.stringPtr[0] = idStr.Append(def.value.stringPtr[0], MAX_STRING_LEN, from);
            }
        }

        private void SetString(idVarDef def, final String from) {
            if (def.initialized == stackVariable) {
//                idStr.Copynz(localstack[localstackBase + def.value.stackOffset], from, MAX_STRING_LEN);
                System.arraycopy(from.getBytes(), 0, localstack, localstackBase + def.value.getStackOffset(), MAX_STRING_LEN);
            } else {
                def.value.stringPtr[0] = from;//idStr.Copynz(def.value.stringPtr, from, MAX_STRING_LEN);
            }
        }

        private String GetString(idVarDef def) {
            if (def.initialized == stackVariable) {
                return btos(localstack, localstackBase + def.value.getStackOffset());
            } else {
                return def.value.stringPtr[0];
            }
        }

        private varEval_s GetVariable(idVarDef def) {
            if (def.initialized == stackVariable) {
//                val.intPtr = ( int * )&localstack[ localstackBase + def->value.stackOffset ];
                varEval_s val = new varEval_s(localstack, (localstackBase + def.value.getStackOffset()) * Integer.BYTES);
                return val;
            } else {
                return def.value;
            }
        }

        private idEntity GetEntity(int entnum) {
            assert (entnum <= MAX_GENTITIES);
            if ((entnum > 0) && (entnum <= MAX_GENTITIES)) {
                return gameLocal.entities[entnum - 1];
            }
            return null;
        }

        private idScriptObject GetScriptObject(int entnum) {
            idEntity ent;

            assert (entnum <= MAX_GENTITIES);
            if ((entnum > 0) && (entnum <= MAX_GENTITIES)) {
                ent = gameLocal.entities[entnum - 1];
                if (ent != null && ent.scriptObject.data != null) {
                    return ent.scriptObject;
                }
            }
            return null;
        }

        private void NextInstruction(int position) {
            // Before we execute an instruction, we increment instructionPointer,
            // therefore we need to compensate for that here.
            instructionPointer = position - 1;
        }

        private void LeaveFunction(idVarDef returnDef) {
            prstack_s stack;
            varEval_s ret;

            if (callStackDepth <= 0) {
                Error("prog stack underflow");
            }

            // return value
            if (returnDef != null) {
                switch (returnDef.Type()) {
                    case ev_string:
                        gameLocal.program.ReturnString(GetString(returnDef));
                        break;

                    case ev_vector:
                        ret = GetVariable(returnDef);
                        gameLocal.program.ReturnVector(ret.vectorPtr[0]);
                        break;

                    default:
                        ret = GetVariable(returnDef);
                        gameLocal.program.ReturnInteger(ret.intPtr[0]);
                }
            }

            // remove locals from the stack
            PopParms(currentFunction.locals);
            assert (localstackUsed == localstackBase);

            if (debug) {
                statement_s line = gameLocal.program.GetStatement(instructionPointer);
                gameLocal.Printf("%d: %s(%d): exit %s", gameLocal.time, gameLocal.program.GetFilename(line.file), line.linenumber, currentFunction.Name());
                if (callStackDepth > 1) {
                    gameLocal.Printf(" return to %s(line %d)\n", callStack[callStackDepth - 1].f.Name(), gameLocal.program.GetStatement(callStack[callStackDepth - 1].s).linenumber);
                } else {
                    gameLocal.Printf(" done\n");
                }
            }

            // up stack
            callStackDepth--;
            stack = callStack[callStackDepth];
            currentFunction = stack.f;
            localstackBase = stack.stackbase;
            NextInstruction(stack.s);

            if (0 == callStackDepth) {
                // all done
                doneProcessing = true;
                threadDying = true;
                currentFunction = null;
            }
        }

        private void CallEvent(final function_t func, int argsize) {
            int i;
            int j;
            varEval_s var;
            int pos;
            int start;
            ByteBuffer data = ByteBuffer.allocate(D_EVENT_MAXARGS * Integer.BYTES);
            idEventDef evdef;
            char[] format;

            if (NOT(func)) {
                Error("NULL function");
            }

            assert (func.eventdef != null);
            evdef = func.eventdef;

            start = localstackUsed - argsize;
            var = new varEval_s(localstack, start);
            eventEntity = GetEntity(var.entityNumberPtr[0]);

            if (null == eventEntity || !eventEntity.RespondsTo(evdef)) {
                if (eventEntity != null && developer.GetBool()) {
                    // give a warning in developer mode
                    Warning("Function '%s' not supported on entity '%s'", evdef.GetName(), eventEntity.name.toString());
                }
                // always return a safe value when an object doesn't exist
                switch (evdef.GetReturnType()) {
                    case D_EVENT_INTEGER:
                        gameLocal.program.ReturnInteger(0);
                        break;

                    case D_EVENT_FLOAT:
                        gameLocal.program.ReturnFloat(0);
                        break;

                    case D_EVENT_VECTOR:
                        gameLocal.program.ReturnVector(vec3_zero);
                        break;

                    case D_EVENT_STRING:
                        gameLocal.program.ReturnString("");
                        break;

                    case D_EVENT_ENTITY:
                    case D_EVENT_ENTITY_NULL:
                        gameLocal.program.ReturnEntity(null);
                        break;

                    case D_EVENT_TRACE:
                    default:
                        // unsupported data type
                        break;
                }

                PopParms(argsize);
                eventEntity = null;
                return;
            }

            format = evdef.GetArgFormat().toCharArray();
            for (j = 0, i = 0, pos = type_object.Size(); (pos < argsize) || (format[i] != 0); i++) {
                switch (format[i]) {
                    case D_EVENT_INTEGER:
                        var = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        data.asIntBuffer().put(i, (int) var.bytePtr.getFloat(0));
                        break;

                    case D_EVENT_FLOAT:
                        var = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        data.asFloatBuffer().put(i, var.bytePtr.getFloat(0));
                        break;

                    case D_EVENT_VECTOR:
                        var = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        FloatBuffer fb = data.asFloatBuffer();//( *( idVec3 ** )&data[ i ] ) = var.vectorPtr;
                        fb.put(i + 0, var.vectorPtr[0].x);
                        fb.put(i + 1, var.vectorPtr[0].y);
                        fb.put(i + 2, var.vectorPtr[0].z);
                        break;

                    case D_EVENT_STRING:
//                        ( *( const char ** )&data[ i ] ) = ( char * )&localstack[ start + pos ];
                        System.arraycopy(localstack, start + pos, data.array(), i, -1);//TODO:length? \0?
                        break;

                    case D_EVENT_ENTITY:
                        var = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        idEntity entity = GetEntity(var.entityNumberPtr[0]);
                        if (null == entity) {
                            Warning("Entity not found for event '%s'. Terminating thread.", evdef.GetName());
                            threadDying = true;
                            PopParms(argsize);
                            return;
                        }
                        int length = entity.Write().capacity();
                        System.arraycopy(entity.Write().array(), 0, data.array(), i, length);
                        break;

                    case D_EVENT_ENTITY_NULL:
                        var = new varEval_s(localstack, (start + pos) * Integer.BYTES);
//                        entity = GetEntity(var.entityNumberPtr);
                        entity = GetEntity(var.entityNumberPtr[0]);
//                        ((idEntity) data[ i]) = GetEntity(var.entityNumberPtr);
                        length = entity.Write().capacity();
                        System.arraycopy(entity.Write().array(), 0, data.array(), i, length);
                        break;

                    case D_EVENT_TRACE:
                        Error("trace type not supported from script for '%s' event.", evdef.GetName());
                        break;

                    default:
                        Error("Invalid arg format string for '%s' event.", evdef.GetName());
                        break;
                }

                pos += func.parmSize.oGet(j++);
            }

            popParms = argsize;
            eventEntity.ProcessEventArgPtr(evdef, btoia(data));

            if (null == multiFrameEvent) {
                if (popParms != 0) {
                    PopParms(popParms);
                }
                eventEntity = null;
            } else {
                doneProcessing = true;
            }
            popParms = 0;
        }

        private void CallSysEvent(final function_t func, int argsize) {
            int i;
            int j;
            varEval_s source;
            int pos;
            int start;
            int[] data = new int[D_EVENT_MAXARGS];
            idEventDef evdef;
            char[] format;

            if (NOT(func)) {
                Error("NULL function");
            }

            assert (func.eventdef != null);
            evdef = func.eventdef;

            start = localstackUsed - argsize;

            format = evdef.GetArgFormat().toCharArray();//TODO:is string output necessary?
            for (j = 0, i = 0, pos = 0; (pos < argsize) || (format[ i] != 0); i++) {
                switch (format[ i]) {
                    case D_EVENT_INTEGER:
                        source = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        data[i] = (int) source.floatPtr[0];
                        break;

                    case D_EVENT_FLOAT:
                        source = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        data[i] = Float.floatToIntBits(source.floatPtr[0]);
                        break;

                    case D_EVENT_VECTOR:
                        source = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        final IntBuffer fb = source.bytePtr.asIntBuffer();
                        data[i + 0] = fb.get(0);
                        data[i + 1] = fb.get(1);
                        data[i + 2] = fb.get(2);
                        break;

                    case D_EVENT_STRING:
                        data[ i] = localstack[ start + pos];
                        break;

                    case D_EVENT_ENTITY:
                        source = new varEval_s(localstack, (start + pos) * Integer.BYTES);
                        idEntity entity = GetEntity(source.entityNumberPtr[0]);
                        if (null == entity) {
                            Warning("Entity not found for event '%s'. Terminating thread.", evdef.GetName());
                            threadDying = true;
                            PopParms(argsize);
                            return;
                        }
//                        data[ i] = GetEntity(source.entityNumberPtr);
                        int length = entity.Write().capacity();
                        for (int k = 0; k < length; k++) {
                            data[i + k] = entity.Write().getInt(4 * k);
                        }
                        break;

                    case D_EVENT_ENTITY_NULL:
                        source = new varEval_s(localstack, (start + pos) * Integer.BYTES);
//                        data[ i] = GetEntity(source.entityNumberPtr);
                        entity = GetEntity(source.bytePtr.getInt(0));
                        length = idEntity.BYTES;
                        entity.Write().asIntBuffer().get(data, i, length);
                        break;

                    case D_EVENT_TRACE:
                        Error("trace type not supported from script for '%s' event.", evdef.GetName());
                        break;

                    default:
                        Error("Invalid arg format string for '%s' event.", evdef.GetName());
                        break;
                }

                pos += func.parmSize.oGet(j++);
            }

            popParms = argsize;
            thread.ProcessEventArgPtr(evdef, data);
            if (popParms != 0) {
                PopParms(popParms);
            }
            popParms = 0;
        }

        public idInterpreter() {
            localstackUsed = 0;
            terminateOnExit = true;
            debug = false;
//	memset( localstack, 0, sizeof( localstack ) );
//            Arrays.fill(localstack, 0);
//	memset( callStack, 0, sizeof( callStack ) );
//            Arrays.fill(callStack, 0);
            Reset();
        }

        // save games
        public void Save(idSaveGame savefile) {				// archives object for save game file
            int i;

            savefile.WriteInt(callStackDepth);
            for (i = 0; i < callStackDepth; i++) {
                savefile.WriteInt(callStack[i].s);
                if (callStack[i].f != null) {
                    savefile.WriteInt(gameLocal.program.GetFunctionIndex(callStack[i].f));
                } else {
                    savefile.WriteInt(-1);
                }
                savefile.WriteInt(callStack[i].stackbase);
            }
            savefile.WriteInt(maxStackDepth);

            savefile.WriteInt(localstackUsed);
            savefile.Write(ByteBuffer.wrap(localstack), localstackUsed);

            savefile.WriteInt(localstackBase);
            savefile.WriteInt(maxLocalstackUsed);

            if (currentFunction != null) {
                savefile.WriteInt(gameLocal.program.GetFunctionIndex(currentFunction));
            } else {
                savefile.WriteInt(-1);
            }
            savefile.WriteInt(instructionPointer);

            savefile.WriteInt(popParms);

            if (multiFrameEvent != null) {
                savefile.WriteString(multiFrameEvent.GetName());
            } else {
                savefile.WriteString("");
            }
            savefile.WriteObject(eventEntity);

            savefile.WriteObject(thread);

            savefile.WriteBool(doneProcessing);
            savefile.WriteBool(threadDying);
            savefile.WriteBool(terminateOnExit);
            savefile.WriteBool(debug);
        }

        public void Restore(idRestoreGame savefile) {				// unarchives object from save game file
            int i;
            idStr funcname = new idStr();
            int[] func_index = {0};

            callStackDepth = savefile.ReadInt();
            for (i = 0; i < callStackDepth; i++) {
                callStack[i].s = savefile.ReadInt();

                savefile.ReadInt(func_index);
                if (func_index[0] >= 0) {
                    callStack[i].f = gameLocal.program.GetFunction(func_index[0]);
                } else {
                    callStack[i].f = null;
                }

                callStack[i].stackbase = savefile.ReadInt();
            }
            maxStackDepth = savefile.ReadInt();

            localstackUsed = savefile.ReadInt();
            savefile.Read(ByteBuffer.wrap(localstack), localstackUsed);

            localstackBase = savefile.ReadInt();
            maxLocalstackUsed = savefile.ReadInt();

            savefile.ReadInt(func_index);
            if (func_index[0] >= 0) {
                currentFunction = gameLocal.program.GetFunction(func_index[0]);
            } else {
                currentFunction = null;
            }
            instructionPointer = savefile.ReadInt();

            popParms = savefile.ReadInt();

            savefile.ReadString(funcname);
            if (funcname.Length() != 0) {
                multiFrameEvent = idEventDef.FindEvent(funcname.toString());
            }

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/eventEntity);
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/thread);

            doneProcessing = savefile.ReadBool();
            threadDying = savefile.ReadBool();
            terminateOnExit = savefile.ReadBool();
            debug = savefile.ReadBool();
        }

        public void SetThread(idThread pThread) {
            thread = pThread;
        }

        public void StackTrace() {
            function_t f;
            int i;
            int top;

            if (callStackDepth == 0) {
                gameLocal.Printf("<NO STACK>\n");
                return;
            }

            top = callStackDepth;
            if (top >= MAX_STACK_DEPTH) {
                top = MAX_STACK_DEPTH - 1;
            }

            if (NOT(currentFunction)) {
                gameLocal.Printf("<NO FUNCTION>\n");
            } else {
                gameLocal.Printf("%12s : %s\n", gameLocal.program.GetFilename(currentFunction.filenum), currentFunction.Name());
            }

            for (i = top; i >= 0; i--) {
                f = callStack[ i].f;
                if (NOT(f)) {
                    gameLocal.Printf("<NO FUNCTION>\n");
                } else {
                    gameLocal.Printf("%12s : %s\n", gameLocal.program.GetFilename(f.filenum), f.Name());
                }
            }
        }

        public int CurrentLine() {
            if (instructionPointer < 0) {
                return 0;
            }
            return gameLocal.program.GetLineNumberForStatement(instructionPointer);
        }

        public String CurrentFile() {
            if (instructionPointer < 0) {
                return "";
            }
            return gameLocal.program.GetFilenameForStatement(instructionPointer);
        }

        /*
         ============
         idInterpreter::Error

         Aborts the currently executing function
         ============
         */
        public void Error(String fmt, Object... Objects) {// id_attribute((format(printf,2,3)));
            throw new TODO_Exception();
//            va_list argptr;
//            char[] text = new char[1024];
//
//            va_start(argptr, fmt);
//            vsprintf(text, fmt, argptr);
//            va_end(argptr);
//
//            StackTrace();
//
//            if ((instructionPointer >= 0) && (instructionPointer < gameLocal.program.NumStatements())) {
//                statement_s line = gameLocal.program.GetStatement(instructionPointer);
//                common.Error("%s(%d): Thread '%s': %s\n", gameLocal.program.GetFilename(line.file), line.linenumber, thread.GetThreadName(), text);
//            } else {
//                common.Error("Thread '%s': %s\n", thread.GetThreadName(), text);
//            }
        }

        /*
         ============
         idInterpreter::Warning

         Prints file and line number information with warning.
         ============
         */
        public void Warning(String fmt, Object... Objects) {// id_attribute((format(printf,2,3)));
            throw new TODO_Exception();
//            va_list argptr;
//            char[] text = new char[1024];
//
//            va_start(argptr, fmt);
//            vsprintf(text, fmt, argptr);
//            va_end(argptr);
//
//            if ((instructionPointer >= 0) && (instructionPointer < gameLocal.program.NumStatements())) {
//                statement_s line = gameLocal.program.GetStatement(instructionPointer);
//                common.Warning("%s(%d): Thread '%s': %s", gameLocal.program.GetFilename(line.file), line.linenumber, thread.GetThreadName(), text);
//            } else {
//                common.Warning("Thread '%s' : %s", thread.GetThreadName(), text);
//            }
        }

        public void DisplayInfo() {
            function_t f;
            int i;

            gameLocal.Printf(" Stack depth: %d bytes, %d max\n", localstackUsed, maxLocalstackUsed);
            gameLocal.Printf("  Call depth: %d, %d max\n", callStackDepth, maxStackDepth);
            gameLocal.Printf("  Call Stack: ");

            if (callStackDepth == 0) {
                gameLocal.Printf("<NO STACK>\n");
            } else {
                if (NOT(currentFunction)) {
                    gameLocal.Printf("<NO FUNCTION>\n");
                } else {
                    gameLocal.Printf("%12s : %s\n", gameLocal.program.GetFilename(currentFunction.filenum), currentFunction.Name());
                }

                for (i = callStackDepth; i > 0; i--) {
                    gameLocal.Printf("              ");
                    f = callStack[ i].f;
                    if (NOT(f)) {
                        gameLocal.Printf("<NO FUNCTION>\n");
                    } else {
                        gameLocal.Printf("%12s : %s\n", gameLocal.program.GetFilename(f.filenum), f.Name());
                    }
                }
            }
        }

        public boolean BeginMultiFrameEvent(idEntity ent, final idEventDef event) {
            if (!eventEntity.equals(ent)) {
                Error("idInterpreter::BeginMultiFrameEvent called with wrong entity");
            }
            if (multiFrameEvent != null) {
                if (!multiFrameEvent.equals(event)) {
                    Error("idInterpreter::BeginMultiFrameEvent called with wrong event");
                }
                return false;
            }

            multiFrameEvent = event;
            return true;
        }

        public void EndMultiFrameEvent(idEntity ent, final idEventDef event) {
            if (!multiFrameEvent.equals(event)) {
                Error("idInterpreter::EndMultiFrameEvent called with wrong event");
            }

            multiFrameEvent = null;
        }

        public boolean MultiFrameEventInProgress() {
            return multiFrameEvent != null;
        }

        /*
         ====================
         idInterpreter::ThreadCall

         Copys the args from the calling thread's stack
         ====================
         */
        public void ThreadCall(idInterpreter source, final function_t func, int args) {
            Reset();

//	memcpy( localstack, &source.localstack[ source.localstackUsed - args ], args );
            System.arraycopy(source.localstack, source.localstackUsed - args, localstack, 0, args);

            localstackUsed = args;
            localstackBase = 0;

            maxLocalstackUsed = localstackUsed;
            EnterFunction(func, false);

            thread.SetThreadName(currentFunction.Name());
        }

        /*
         ====================
         idInterpreter::EnterFunction

         Returns the new program statement counter

         NOTE: If this is called from within a event called by this interpreter, the function arguments will be invalid after calling this function.
         ====================
         */
        public void EnterFunction(final function_t func, boolean clearStack) {
            int c;
            prstack_s stack;

            if (clearStack) {
                Reset();
            }
            if (popParms != 0) {
                PopParms(popParms);
                popParms = 0;
            }

            if (callStackDepth >= MAX_STACK_DEPTH) {
                Error("call stack overflow");
            }

            stack = callStack[ callStackDepth];

            stack.s = instructionPointer + 1;	// point to the next instruction to execute
            stack.f = currentFunction;
            stack.stackbase = localstackBase;

            callStackDepth++;
            if (callStackDepth > maxStackDepth) {
                maxStackDepth = callStackDepth;
            }

            if (NOT(func)) {
                Error("NULL function");
            }

            if (debug) {
                if (currentFunction != null) {
                    gameLocal.Printf("%d: call '%s' from '%s'(line %d)%s\n", gameLocal.time, func.Name(), currentFunction.Name(),
                            gameLocal.program.GetStatement(instructionPointer).linenumber, clearStack ? " clear stack" : "");
                } else {
                    gameLocal.Printf("%d: call '%s'%s\n", gameLocal.time, func.Name(), clearStack ? " clear stack" : "");
                }
            }

            currentFunction = func;
            assert (NOT(func.eventdef));
            NextInstruction(func.firstStatement);

            // allocate space on the stack for locals
            // parms are already on stack
            c = func.locals - func.parmTotal;
            assert (c >= 0);

            if (localstackUsed + c > LOCALSTACK_SIZE) {
                Error("EnterFuncton: locals stack overflow\n");
            }

            // initialize local stack variables to zero
//	memset( &localstack[ localstackUsed ], 0, c );
            Arrays.fill(localstack, localstackUsed, localstack.length - localstackUsed, (byte) 0);

            localstackUsed += c;
            localstackBase = localstackUsed - func.locals;

            if (localstackUsed > maxLocalstackUsed) {
                maxLocalstackUsed = localstackUsed;
            }
        }

        /*
         ================
         idInterpreter::EnterObjectFunction

         Calls a function on a script object.

         NOTE: If this is called from within a event called by this interpreter, the function arguments will be invalid after calling this function.
         ================
         */
        public void EnterObjectFunction(idEntity self, final function_t func, boolean clearStack) {
            if (clearStack) {
                Reset();
            }
            if (popParms != 0) {
                PopParms(popParms);
                popParms = 0;
            }
            Push(self.entityNumber + 1);
            EnterFunction(func, false);
        }

        public boolean Execute() {
            varEval_s var_a;
            varEval_s var_b;
            varEval_s var_c;
            varEval_s var;
            statement_s st;
            int runaway;
            idThread newThread;
            float floatVal;
            idScriptObject obj;
            function_t func;

            if (threadDying || NOT(currentFunction)) {
                return true;
            }

            if (multiFrameEvent != null) {
                // move to previous instruction and call it again
                instructionPointer--;
            }

            runaway = 5000000;

            doneProcessing = false;
            while (!doneProcessing && !threadDying) {
                instructionPointer++;

                if (0 == --runaway) {
                    Error("runaway loop error");
                }

                // next statement
                st = gameLocal.program.GetStatement(instructionPointer);

                switch (st.op) {
                    case OP_RETURN:
                        LeaveFunction(st.a);
                        break;

                    case OP_THREAD:
                        newThread = new idThread(this, st.a.value.functionPtr[0], st.b.value.getArgSize());
                        newThread.Start();

                        // return the thread number to the script
                        gameLocal.program.ReturnFloat(newThread.GetThreadNum());
                        PopParms(st.b.value.getArgSize());
                        break;

                    case OP_OBJTHREAD:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            func = obj.GetTypeDef().GetFunction(st.b.value.getVirtualFunction());
                            assert (st.c.value.getArgSize() == func.parmTotal);
                            newThread = new idThread(this, GetEntity(var_a.entityNumberPtr[0]), func, func.parmTotal);
                            newThread.Start();

                            // return the thread number to the script
                            gameLocal.program.ReturnFloat(newThread.GetThreadNum());
                        } else {
                            // return a null thread to the script
                            gameLocal.program.ReturnFloat(0.0f);
                        }
                        PopParms(st.c.value.getArgSize());
                        break;

                    case OP_CALL:
                        EnterFunction(st.a.value.functionPtr[0], false);
                        break;

                    case OP_EVENTCALL:
                        CallEvent(st.a.value.functionPtr[0], st.b.value.getArgSize());
                        break;

                    case OP_OBJECTCALL:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            func = obj.GetTypeDef().GetFunction(st.b.value.getVirtualFunction());
                            EnterFunction(func, false);
                        } else {
                            // return a 'safe' value
                            gameLocal.program.ReturnVector(vec3_zero);
                            gameLocal.program.ReturnString("");
                            PopParms(st.c.value.getArgSize());
                        }
                        break;

                    case OP_SYSCALL:
                        CallSysEvent(st.a.value.functionPtr[0], st.b.value.getArgSize());
                        break;

                    case OP_IFNOT:
                        var_a = GetVariable(st.a);
                        if (var_a.intPtr[0] != 0) {
                            NextInstruction(instructionPointer + st.b.value.getJumpOffset());
                        }
                        break;

                    case OP_IF:
                        var_a = GetVariable(st.a);
                        if (var_a.intPtr[0] != 0) {
                            NextInstruction(instructionPointer + st.b.value.getJumpOffset());
                        }
                        break;

                    case OP_GOTO:
                        NextInstruction(instructionPointer + st.a.value.getJumpOffset());
                        break;

                    case OP_ADD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = var_a.floatPtr[0] + var_b.floatPtr[0];
                        break;

                    case OP_ADD_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.vectorPtr[0] = var_a.vectorPtr[0].oPlus(var_b.vectorPtr[0]);
                        break;

                    case OP_ADD_S:
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_FS:
                        var_a = GetVariable(st.a);
                        SetString(st.c, FloatToString(var_a.floatPtr[0]));
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_SF:
                        var_b = GetVariable(st.b);
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, FloatToString(var_b.floatPtr[0]));
                        break;

                    case OP_ADD_VS:
                        var_a = GetVariable(st.a);
                        SetString(st.c, var_a.vectorPtr[0].ToString());
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_SV:
                        var_b = GetVariable(st.b);
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, var_b.vectorPtr[0].ToString());
                        break;

                    case OP_SUB_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = var_a.floatPtr[0] - var_b.floatPtr[0];
                        break;

                    case OP_SUB_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.vectorPtr[0] = var_a.vectorPtr[0].oMinus(var_b.vectorPtr[0]);
                        break;

                    case OP_MUL_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = var_a.floatPtr[0] * var_b.floatPtr[0];
                        break;

                    case OP_MUL_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = var_a.vectorPtr[0].oMultiply(var_b.vectorPtr[0]);
                        break;

                    case OP_MUL_FV:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.vectorPtr[0] = var_b.vectorPtr[0].oMultiply(var_a.floatPtr[0]);
                        break;

                    case OP_MUL_VF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.vectorPtr[0] = var_a.vectorPtr[0].oMultiply(var_b.floatPtr[0]);
                        break;

                    case OP_DIV_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);

                        if (var_b.floatPtr[0] == 0.0f) {
                            Warning("Divide by zero");
                            var_c.floatPtr[0] = idMath.INFINITY;
                        } else {
                            var_c.floatPtr[0] = var_a.floatPtr[0] / var_b.floatPtr[0];
                        }
                        break;

                    case OP_MOD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);

                        if (var_b.floatPtr[0] == 0.0f) {
                            Warning("Divide by zero");
                            var_c.floatPtr[0] = var_a.floatPtr[0];
                        } else {
                            var_c.floatPtr[0] = ((int) var_a.floatPtr[0]) % ((int) var_b.floatPtr[0]);//TODO:casts!
                        }
                        break;

                    case OP_BITAND:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = ((int) var_a.floatPtr[0]) & ((int) var_b.floatPtr[0]);
                        break;

                    case OP_BITOR:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = ((int) var_a.floatPtr[0]) | ((int) var_b.floatPtr[0]);
                        break;

                    case OP_GE:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] >= var_b.floatPtr[0]);
                        break;

                    case OP_LE:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] <= var_b.floatPtr[0]);
                        break;

                    case OP_GT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] > var_b.floatPtr[0]);
                        break;

                    case OP_LT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] < var_b.floatPtr[0]);
                        break;

                    case OP_AND:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.floatPtr[0] != 0.0f) && (var_b.floatPtr[0] != 0.0f));
                        break;

                    case OP_AND_BOOLF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.intPtr[0] != 0) && (var_b.floatPtr[0] != 0.0f));
                        break;

                    case OP_AND_FBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.floatPtr[0] != 0.0f) && (var_b.intPtr[0] != 0));
                        break;

                    case OP_AND_BOOLBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.intPtr[0] != 0) && (var_b.intPtr[0] != 0));
                        break;

                    case OP_OR:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.floatPtr[0] != 0.0f) || (var_b.floatPtr[0] != 0.0f));
                        break;

                    case OP_OR_BOOLF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.intPtr[0] != 0) || (var_b.floatPtr[0] != 0.0f));
                        break;

                    case OP_OR_FBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.floatPtr[0] != 0.0f) || (var_b.intPtr[0] != 0));
                        break;

                    case OP_OR_BOOLBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi((var_a.intPtr[0] != 0) || (var_b.intPtr[0] != 0));
                        break;

                    case OP_NOT_BOOL:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.intPtr[0] == 0);
                        break;

                    case OP_NOT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] == 0.0f);
                        break;

                    case OP_NOT_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.vectorPtr[0].equals(vec3_zero));
                        break;

                    case OP_NOT_S:
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(!isNotNullOrEmpty(GetString(st.a)));
                        break;

                    case OP_NOT_ENT:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(GetEntity(var_a.entityNumberPtr[0]) == null);
                        break;

                    case OP_NEG_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = -var_a.floatPtr[0];
                        break;

                    case OP_NEG_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.vectorPtr[0] = var_a.vectorPtr[0].oNegative();
                        break;

                    case OP_INT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = var_a.floatPtr[0];
                        break;

                    case OP_EQ_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] == var_b.floatPtr[0]);
                        break;

                    case OP_EQ_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.vectorPtr[0].equals(var_b.vectorPtr));
                        break;

                    case OP_EQ_S:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(idStr.Cmp(GetString(st.a), GetString(st.b)) == 0);
                        break;

                    case OP_EQ_E:
                    case OP_EQ_EO:
                    case OP_EQ_OE:
                    case OP_EQ_OO:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.entityNumberPtr == var_b.entityNumberPtr);
                        break;

                    case OP_NE_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.floatPtr[0] != var_b.floatPtr[0]);
                        break;

                    case OP_NE_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(!var_a.vectorPtr[0].equals(var_b.vectorPtr));
                        break;

                    case OP_NE_S:
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(idStr.Cmp(GetString(st.a), GetString(st.b)) != 0);
                        break;

                    case OP_NE_E:
                    case OP_NE_EO:
                    case OP_NE_OE:
                    case OP_NE_OO:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = btoi(var_a.entityNumberPtr != var_b.entityNumberPtr);
                        break;

                    case OP_UADD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] += var_a.floatPtr[0];
                        break;

                    case OP_UADD_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.vectorPtr[0].oPluSet(var_a.vectorPtr[0]);
                        break;

                    case OP_USUB_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] -= var_a.floatPtr[0];
                        break;

                    case OP_USUB_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.vectorPtr[0].oMinSet(var_a.vectorPtr[0]);
                        break;

                    case OP_UMUL_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] *= var_a.floatPtr[0];
                        break;

                    case OP_UMUL_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.vectorPtr[0].oMulSet(var_a.floatPtr[0]);
                        break;

                    case OP_UDIV_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.floatPtr[0] == 0.0f) {
                            Warning("Divide by zero");
                            var_b.floatPtr[0] = idMath.INFINITY;
                        } else {
                            var_b.floatPtr[0] /= var_a.floatPtr[0];
                        }
                        break;

                    case OP_UDIV_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.floatPtr[0] == 0.0f) {
                            Warning("Divide by zero");
                            var_b.vectorPtr[0].Set(idMath.INFINITY, idMath.INFINITY, idMath.INFINITY);
                        } else {
                            var_b.vectorPtr[0] = var_b.vectorPtr[0].oDivide(var_a.floatPtr[0]);
                        }
                        break;

                    case OP_UMOD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.floatPtr[0] == 0.0f) {
                            Warning("Divide by zero");
                            var_b.floatPtr[0] = var_a.floatPtr[0];
                        } else {
                            var_b.floatPtr[0] = ((int) var_b.floatPtr[0]) % ((int) var_a.floatPtr[0]);
                        }
                        break;

                    case OP_UOR_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] = ((int) var_b.floatPtr[0]) | ((int) var_a.floatPtr[0]);
                        break;

                    case OP_UAND_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] = ((int) var_b.floatPtr[0]) & ((int) var_a.floatPtr[0]);
                        break;

                    case OP_UINC_F:
                        var_a = GetVariable(st.a);
                        var_a.floatPtr[0]++;
                        break;

                    case OP_UINCP_F:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            final int pos = st.b.value.getPtrOffset();
                            obj.data.putFloat(pos, obj.data.getFloat(pos) + 1);
                        }
                        break;

                    case OP_UDEC_F:
                        var_a = GetVariable(st.a);
                        var_a.floatPtr[0]--;
                        break;

                    case OP_UDECP_F:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            final int pos = st.b.value.getPtrOffset();
                            obj.data.putFloat(pos, obj.data.getFloat(pos) - 1);
                        }
                        break;

                    case OP_COMP_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.floatPtr[0] = ~((int) var_a.floatPtr[0]);
                        break;

                    case OP_STORE_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] = var_a.floatPtr[0];
                        break;

                    case OP_STORE_ENT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.entityNumberPtr[0] = var_a.entityNumberPtr[0];
                        break;

                    case OP_STORE_BOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.intPtr[0] = var_a.intPtr[0];
                        break;

                    case OP_STORE_OBJENT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (NOT(obj)) {
                            var_b.entityNumberPtr[0] = 0;
                        } else if (!obj.GetTypeDef().Inherits(st.b.TypeDef())) {
                            //Warning( "object '%s' cannot be converted to '%s'", obj.GetTypeName(), st.b.TypeDef().Name() );
                            var_b.entityNumberPtr[0] = 0;
                        } else {
                            var_b.entityNumberPtr[0] = var_a.entityNumberPtr[0];
                        }
                        break;

                    case OP_STORE_OBJ:
                    case OP_STORE_ENTOBJ:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.entityNumberPtr[0] = var_a.entityNumberPtr[0];
                        break;

                    case OP_STORE_S:
                        SetString(st.b, GetString(st.a));
                        break;

                    case OP_STORE_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.vectorPtr[0] = var_a.vectorPtr[0];
                        break;

                    case OP_STORE_FTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, FloatToString(var_a.floatPtr[0]));
                        break;

                    case OP_STORE_BTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, itob(var_a.intPtr[0]) ? "true" : "false");
                        break;

                    case OP_STORE_VTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, var_a.vectorPtr[0].ToString());
                        break;

                    case OP_STORE_FTOBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        if (var_a.floatPtr[0] != 0.0f) {
                            var_b.intPtr[0] = 1;
                        } else {
                            var_b.intPtr[0] = 0;
                        }
                        break;

                    case OP_STORE_BOOLTOF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.floatPtr[0] = Float.intBitsToFloat(var_a.intPtr[0]);
                        break;

                    case OP_STOREP_F:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].floatPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].floatPtr[0] = var_a.floatPtr[0];
                        }
                        break;

                    case OP_STOREP_ENT:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].entityNumberPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].floatPtr[0] = var_a.entityNumberPtr[0];
                        }
                        break;

                    case OP_STOREP_FLD:
                    case OP_STOREP_BOOL:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].intPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].intPtr[0] = var_a.intPtr[0];
                        }
                        break;
                        
                    case OP_STOREP_S:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].stringPtr != null) {
                            var_b.evalPtr[0].stringPtr[0] = GetString(st.a);//idStr.Copynz(var_b.evalPtr.stringPtr, GetString(st.a), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_V:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].vectorPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].vectorPtr[0] = var_a.vectorPtr[0];
                        }
                        break;

                    case OP_STOREP_FTOS:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].stringPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].stringPtr[0] = FloatToString(var_a.floatPtr[0]);//idStr.Copynz(var_b.evalPtr.stringPtr, FloatToString(var_a.floatPtr.oGet()), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_BTOS:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].stringPtr != null) {
                            var_a = GetVariable(st.a);
                            if (var_a.floatPtr[0] != 0.0f) {
                                var_b.evalPtr[0].stringPtr[0] = "true";//idStr.Copynz(var_b.evalPtr.stringPtr, "true", MAX_STRING_LEN);
                            } else {
                                var_b.evalPtr[0].stringPtr[0] = "false";//idStr.Copynz(var_b.evalPtr.stringPtr, "false", MAX_STRING_LEN);
                            }
                        }
                        break;

                    case OP_STOREP_VTOS:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].stringPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].stringPtr[0] = var_a.vectorPtr[0].ToString();//idStr.Copynz(var_b.evalPtr.stringPtr, var_a.vectorPtr[0].ToString(), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_FTOBOOL:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].intPtr != null) {
                            var_a = GetVariable(st.a);
                            if (var_a.floatPtr[0] != 0.0f) {
                                var_b.evalPtr[0].intPtr[0] = 1;
                            } else {
                                var_b.evalPtr[0].intPtr[0] = 0;
                            }
                        }
                        break;

                    case OP_STOREP_BOOLTOF:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].floatPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].floatPtr[0] = Float.intBitsToFloat(var_a.intPtr[0]);
                        }
                        break;

                    case OP_STOREP_OBJ:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].entityNumberPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr[0].entityNumberPtr[0] = var_a.entityNumberPtr[0];
                        }
                        break;

                    case OP_STOREP_OBJENT:
                        var_b = GetVariable(st.b);
                        if (var_b.evalPtr != null && var_b.evalPtr[0].entityNumberPtr != null) {
                            var_a = GetVariable(st.a);
                            obj = GetScriptObject(var_a.entityNumberPtr[0]);
                            if (NOT(obj)) {
                                var_b.evalPtr[0].entityNumberPtr[0] = 0;

                                // st.b points to type_pointer, which is just a temporary that gets its type reassigned, so we store the real type in st.c
                                // so that we can do a type check during run time since we don't know what type the script object is at compile time because it
                                // comes from an entity
                            } else if (!obj.GetTypeDef().Inherits(st.c.TypeDef())) {
                                //Warning( "object '%s' cannot be converted to '%s'", obj.GetTypeName(), st.c.TypeDef().Name() );
                                var_b.evalPtr[0].entityNumberPtr[0] = 0;
                            } else {
                                var_b.evalPtr[0].entityNumberPtr[0] = var_a.entityNumberPtr[0];
                            }
                        }
                        break;

                    case OP_ADDRESS:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            final varEval_s temp = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c = new varEval_s(temp);
                        } else {
                            var_c = null;
                        }
                        break;

                    case OP_INDIRECT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c.floatPtr[0] = var.getFloatPtr();
                        } else {
                            var_c.floatPtr[0] = 0.0f;
                        }
                        break;

                    case OP_INDIRECT_ENT:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c.entityNumberPtr[0] = var.getEntityNumberPtr();
                        } else {
                            var_c.entityNumberPtr[0] = 0;
                        }
                        break;

                    case OP_INDIRECT_BOOL:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c.intPtr[0] = var.getIntPtr();
                        } else {
                            var_c.intPtr[0] = 0;
                        }
                        break;

                    case OP_INDIRECT_S:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            SetString(st.c, var.getStringPtr());
                        } else {
                            SetString(st.c, "");
                        }
                        break;

                    case OP_INDIRECT_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (obj != null) {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c.vectorPtr[0] = var.getVectorPtr();
                        } else {
                            var_c.vectorPtr[0].Zero();
                        }
                        break;

                    case OP_INDIRECT_OBJ:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.entityNumberPtr[0]);
                        if (NOT(obj)) {
                            var_c.entityNumberPtr[0] = 0;
                        } else {
                            var = new varEval_s(obj.data, st.b.value.getPtrOffset());
                            var_c.entityNumberPtr[0] = var.getEntityNumberPtr();
                        }
                        break;

                    case OP_PUSH_F:
                        var_a = GetVariable(st.a);
                        Push(var_a.intPtr[0]);
                        break;

                    case OP_PUSH_FTOS:
                        var_a = GetVariable(st.a);
                        PushString(FloatToString(var_a.floatPtr[0]));
                        break;

                    case OP_PUSH_BTOF:
                        var_a = GetVariable(st.a);
                        floatVal = var_a.intPtr[0];
                        Push(Float.floatToIntBits(floatVal));
                        break;

                    case OP_PUSH_FTOB:
                        var_a = GetVariable(st.a);
                        if (var_a.floatPtr[0] != 0.0f) {
                            Push(1);
                        } else {
                            Push(0);
                        }
                        break;

                    case OP_PUSH_VTOS:
                        var_a = GetVariable(st.a);
                        PushString(var_a.vectorPtr[0].ToString());
                        break;

                    case OP_PUSH_BTOS:
                        var_a = GetVariable(st.a);
                        PushString(itob(var_a.intPtr[0]) ? "true" : "false");
                        break;

                    case OP_PUSH_ENT:
                        var_a = GetVariable(st.a);
                        Push(var_a.entityNumberPtr[0]);
                        break;

                    case OP_PUSH_S:
                        PushString(GetString(st.a));
                        break;

                    case OP_PUSH_V:
                        var_a = GetVariable(st.a);
                        Push(Float.floatToIntBits(var_a.vectorPtr[0].x));
                        Push(Float.floatToIntBits(var_a.vectorPtr[0].y));
                        Push(Float.floatToIntBits(var_a.vectorPtr[0].z));
                        break;

                    case OP_PUSH_OBJ:
                        var_a = GetVariable(st.a);
                        Push(var_a.entityNumberPtr[0]);
                        break;

                    case OP_PUSH_OBJENT:
                        var_a = GetVariable(st.a);
                        Push(var_a.entityNumberPtr[0]);
                        break;

                    case OP_BREAK:
                    case OP_CONTINUE:
                    default:
                        Error("Bad opcode %d", st.op);
                        break;
                }
            }
            var = var_a = var_b = var_c = null;

            return threadDying;
        }

        public void Reset() {
            callStackDepth = 0;
            localstackUsed = 0;
            localstackBase = 0;

            maxLocalstackUsed = 0;
            maxStackDepth = 0;

            popParms = 0;
            multiFrameEvent = null;
            eventEntity = null;

            currentFunction = null;
            NextInstruction(0);

            threadDying = false;
            doneProcessing = true;
        }

        /*
         ================
         idInterpreter::GetRegisterValue

         Returns a string representation of the value of the register.  This is 
         used primarily for the debugger and debugging

         //FIXME:  This is pretty much wrong.  won't access data in most situations.
         ================
         */
        public boolean GetRegisterValue(final String name, idStr out, int scopeDepth) {
            varEval_s reg;
            idVarDef d;
            String[] funcObject = {null};//new char[1024];
            String funcName;
            idVarDef scope;
            idTypeDef field;
            idScriptObject obj;
            function_t func;
            int funcIndex;

            out.Empty();

            if (scopeDepth == -1) {
                scopeDepth = callStackDepth;
            }

            if (scopeDepth == callStackDepth) {
                func = currentFunction;
            } else {
                func = callStack[ scopeDepth].f;
            }
            if (NOT(func)) {
                return false;
            }

            idStr.Copynz(funcObject, func.Name(), sizeof(funcObject));
            funcIndex = funcObject[0].indexOf("::");
            if (funcIndex != -1) {
//                funcName = "\0";
                scope = gameLocal.program.GetDef(null, funcObject[0], def_namespace);
                funcName = funcObject[0].substring(funcIndex + 2);//TODO:check pointer location
            } else {
                funcName = funcObject[0];
                scope = def_namespace;
            }

            // Get the function from the object
            d = gameLocal.program.GetDef(null, funcName, scope);
            if (NOT(d)) {
                return false;
            }

            // Get the variable itself and check various namespaces
            d = gameLocal.program.GetDef(null, name, d);
            if (NOT(d)) {
                if (scope == def_namespace) {
                    return false;
                }

                d = gameLocal.program.GetDef(null, name, scope);
                if (NOT(d)) {
                    d = gameLocal.program.GetDef(null, name, def_namespace);
                    if (NOT(d)) {
                        return false;
                    }
                }
            }

            reg = GetVariable(d);
            switch (d.Type()) {
                case ev_float:
                    if (reg.floatPtr[0] != 0.0f) {
                        out.oSet(va("%g", reg.floatPtr[0]));
                    } else {
                        out.oSet("0");
                    }
                    return true;
//                    break;

                case ev_vector:
                    if (reg.vectorPtr != null) {
                        out.oSet(va("%g,%g,%g", reg.vectorPtr[0].x, reg.vectorPtr[0].y, reg.vectorPtr[0].z));
                    } else {
                        out.oSet("0,0,0");
                    }
                    return true;
//                    break;

                case ev_boolean:
                    if (reg.intPtr[0] != 0) {
                        out.oSet(va("%d", reg.intPtr[0]));
                    } else {
                        out.oSet("0");
                    }
                    return true;
//                    break;

                case ev_field:
                    if (scope == def_namespace) {
                        // should never happen, but handle it safely anyway
                        return false;
                    }

                    field = scope.TypeDef().GetParmType(reg.getPtrOffset()).FieldType();
                    obj = new idScriptObject();
                    obj.Read(ByteBuffer.wrap(Arrays.copyOf(localstack, callStack[callStackDepth].stackbase)));//TODO: check this range
                    if (NOT(field) || NOT(obj)) {
                        return false;
                    }

                    switch (field.Type()) {
                        case ev_boolean:
                            out.oSet(va("%d", obj.data.getInt(reg.getPtrOffset())));
                            return true;

                        case ev_float:
                            out.oSet(va("%g", obj.data.getFloat(reg.getPtrOffset())));
                            return true;

                        default:
                            return false;
                    }
//                    break;

                case ev_string:
                    if (reg.stringPtr != null) {
                        out.oSet("\"");
                        out.oPluSet(reg.stringPtr[0]);
                        out.oPluSet("\"");
                    } else {
                        out.oSet("\"\"");
                    }
                    return true;

                default:
                    return false;
            }
//            return false;
        }

        public int GetCallstackDepth() {
            return callStackDepth;
        }

        public prstack_s GetCallstack() {
            return callStack[0];
        }

        public function_t GetCurrentFunction() {
            return currentFunction;
        }

        public idThread GetThread() {
            return thread;
        }
    };
}
