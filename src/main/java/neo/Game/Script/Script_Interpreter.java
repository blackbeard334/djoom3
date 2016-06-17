package neo.Game.Script;

import java.nio.ByteBuffer;
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

import neo.Game.GameSys.Class.idEventArg;
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
import static neo.TempDump.btoi;
import static neo.TempDump.btos;
import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.TempDump.sizeof;
import neo.idlib.Text.Str.idStr;

import static neo.TempDump.strLen;
import static neo.framework.Common.common;
import static neo.idlib.Text.Str.va;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;

import static neo.idlib.math.Vector.getVec3_zero;

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

        public static final int NULL_ENTITY = -1;
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

        public idInterpreter() {
            localstackUsed = 0;
            terminateOnExit = true;
            debug = false;
//            memset(localstack, 0, sizeof(localstack));
//            memset(callStack, 0, sizeof(callStack));
            Reset();
        }

        private void PopParms(int numParms) {
            // pop our parms off the stack
            if (localstackUsed < numParms) {
                Error("locals stack underflow\n");
            }

            localstackUsed -= numParms;
        }

        private void PushString(final String string) {
            System.out.println("+++ " + string);
            if (localstackUsed + MAX_STRING_LEN > LOCALSTACK_SIZE) {
                Error("PushString: locals stack overflow\n");
            }
//            idStr.Copynz(localstack[localstackUsed], string, MAX_STRING_LEN);
            final String str = string + '\0';
            final int length = Math.min(str.length(), MAX_STRING_LEN);
            System.arraycopy(str.getBytes(), 0, localstack, localstackUsed, length);
            localstackUsed += MAX_STRING_LEN;
        }

        private void Push(int value) {
            if (localstackUsed + Integer.BYTES > LOCALSTACK_SIZE) {
                Error("Push: locals stack overflow\n");
            }
            localstack[localstackUsed + 0] = (byte) (value >>> 0);
            localstack[localstackUsed + 1] = (byte) (value >>> 8);
            localstack[localstackUsed + 2] = (byte) (value >>> 16);
            localstack[localstackUsed + 3] = (byte) (value >>> 24);
            localstackUsed += Integer.BYTES;
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
                final String str = from + '\0';
                final int length = Math.min(str.length(), MAX_STRING_LEN);
                final int offset = localstackBase + def.value.getStackOffset();
                final int appendOffset = strLen(localstack, offset);
                System.arraycopy(from.getBytes(), 0, localstack, appendOffset, length);
            } else {
                def.value.stringPtr = idStr.Append(def.value.stringPtr, MAX_STRING_LEN, from);
            }
        }

        private void SetString(idVarDef def, final String from) {
            if (def.initialized == stackVariable) {
//                idStr.Copynz(localstack[localstackBase + def.value.stackOffset], from, MAX_STRING_LEN);
                final String str = from + '\0';
                final int length = Math.min(str.length(), MAX_STRING_LEN);
                System.arraycopy(str.getBytes(), 0, localstack, localstackBase + def.value.getStackOffset(), length);
            } else {
                def.value.stringPtr = from;//idStr.Copynz(def.value.stringPtr, from, MAX_STRING_LEN);
            }
        }

        private String GetString(idVarDef def) {
            if (def.initialized == stackVariable) {
                return btos(localstack, localstackBase + def.value.getStackOffset());
            } else {
                return def.value.stringPtr;
            }
        }

        private varEval_s GetVariable(idVarDef def) {
            if (def.initialized == stackVariable) {
                varEval_s val = new varEval_s();
                val.setIntPtr(localstack, localstackBase + def.value.getStackOffset());// = ( int * )&localstack[ localstackBase + def->value.stackOffset ];
                return val;
            } else {
                return def.value;
            }
        }

        private varEval_s GetEvalVariable(idVarDef def) {
            final varEval_s var = GetVariable(def);
            if (var.getEntityNumberPtr() != NULL_ENTITY) {
                final idScriptObject scriptObject = gameLocal.entities[var.getEntityNumberPtr() - 1].scriptObject;
                final ByteBuffer data = scriptObject.data;
                if (data != null) {
                    var.evalPtr = new varEval_s();
                    var.evalPtr.setBytePtr(data, scriptObject.offset);
                }
            }

            return var;
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
                        gameLocal.program.ReturnVector(ret.getVectorPtr());
                        break;

                    default:
                        ret = GetVariable(returnDef);
                        gameLocal.program.ReturnInteger(ret.getIntPtr());
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
            varEval_s var = new varEval_s();
            int pos;
            int start;
            idEventArg[] data = new idEventArg[D_EVENT_MAXARGS];
            idEventDef evdef;
            char[] format;

            if (NOT(func)) {
                Error("NULL function");
            }

            assert (func.eventdef != null);
            evdef = func.eventdef;

            start = localstackUsed - argsize;
            var.setIntPtr(localstack, start);
            eventEntity = GetEntity(var.getEntityNumberPtr());

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
                        gameLocal.program.ReturnVector(getVec3_zero());
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
            for (j = 0, i = 0, pos = type_object.Size(); (pos < argsize) || (i < format.length && format[i] != 0); i++) {
                switch (format[i]) {
                    case D_EVENT_INTEGER:
                        var.setIntPtr(localstack, (start + pos));
                        data[i]= idEventArg.toArg((int) var.getFloatPtr());
                        break;

                    case D_EVENT_FLOAT:
                        var.setIntPtr(localstack, (start + pos));
                        data[i]= idEventArg.toArg(var.getFloatPtr());
                        break;

                    case D_EVENT_VECTOR:
                        var.setIntPtr(localstack, (start + pos));
                        data[i]= idEventArg.toArg(var.getVectorPtr());
                        break;

                    case D_EVENT_STRING:
                        data[i]= idEventArg.toArg(btos(localstack, start + pos));//( *( const char ** )&data[ i ] ) = ( char * )&localstack[ start + pos ];
                        break;

                    case D_EVENT_ENTITY:
                        var.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(GetEntity(var.getEntityNumberPtr()));
                        if (null == data[i]) {
                            Warning("Entity not found for event '%s'. Terminating thread.", evdef.GetName());
                            threadDying = true;
                            PopParms(argsize);
                            return;
                        }
                        break;

                    case D_EVENT_ENTITY_NULL:
                        var.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(GetEntity(var.getEntityNumberPtr()));
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
            eventEntity.ProcessEventArgPtr(evdef, data);

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
            varEval_s source = new varEval_s();
            int pos;
            int start;
            idEventArg[] data = new idEventArg[D_EVENT_MAXARGS];
            final idEventDef evdef;
            final String format;

            if (NOT(func)) {
                Error("NULL function");
            }

            assert (func.eventdef != null);
            evdef = func.eventdef;

            start = localstackUsed - argsize;

            format = evdef.GetArgFormat();
            for (j = 0, i = 0, pos = 0; (pos < argsize) || (i < format.length()); i++) {
                switch (format.charAt(i)) {
                    case D_EVENT_INTEGER:
                        source.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg((int)source.getFloatPtr());
                        break;

                    case D_EVENT_FLOAT:
                        source.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(source.getFloatPtr());
                        break;

                    case D_EVENT_VECTOR:
                        source.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(source.getVectorPtr());
                        break;

                    case D_EVENT_STRING:
                        data[i] = idEventArg.toArg(btos(localstack, start + pos));
                        break;

                    case D_EVENT_ENTITY:
                        source.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(GetEntity(source.getEntityNumberPtr()));
                        if (null == data[i]) {
                            Warning("Entity not found for event '%s'. Terminating thread.", evdef.GetName());
                            threadDying = true;
                            PopParms(argsize);
                            return;
                        }
                        break;

                    case D_EVENT_ENTITY_NULL:
                        source.setIntPtr(localstack, (start + pos));
                        data[i] = idEventArg.toArg(GetEntity(source.getEntityNumberPtr()));
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

//            throw new TODO_Exception();
            popParms = argsize;
            thread.ProcessEventArgPtr(evdef, data);
            if (popParms != 0) {
                PopParms(popParms);
            }
            popParms = 0;
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
                f = callStack[i].f;
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
        public void Error(String fmt, Object... objects) {// id_attribute((format(printf,2,3)));
            String text = String.format(fmt, objects);
            StackTrace();

            if ((instructionPointer >= 0) && (instructionPointer < gameLocal.program.NumStatements())) {
                statement_s line = gameLocal.program.GetStatement(instructionPointer);
                common.Error("%s(%d): Thread '%s': %s\n", gameLocal.program.GetFilename(line.file), line.linenumber, thread.GetThreadName(), text);
            } else {
                common.Error("Thread '%s': %s\n", thread.GetThreadName(), text);
            }
        }

        /*
         ============
         idInterpreter::Warning

         Prints file and line number information with warning.
         ============
         */
        public void Warning(String fmt, Object... objects) {// id_attribute((format(printf,2,3)));
            String text = String.format(fmt, objects);

            if ((instructionPointer >= 0) && (instructionPointer < gameLocal.program.NumStatements())) {
                statement_s line = gameLocal.program.GetStatement(instructionPointer);
                common.Warning("%s(%d): Thread '%s': %s", gameLocal.program.GetFilename(line.file), line.linenumber, thread.GetThreadName(), text);
            } else {
                common.Warning("Thread '%s' : %s", thread.GetThreadName(), text);
            }
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
                    f = callStack[i].f;
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

            stack = callStack[callStackDepth] = new prstack_s();

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
            Arrays.fill(localstack, localstackUsed, localstackUsed + c, (byte) 0);

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

        private static int DBG_Execute = 0;
        public boolean Execute() {DBG_Execute++;
            varEval_s var_a = new varEval_s();
            varEval_s var_b;
            varEval_s var_c;
            varEval_s var = new varEval_s();
            statement_s st;
            int runaway;
            idThread newThread;
            float floatVal;
            idScriptObject obj;
            function_t func;
//            System.out.println(instructionPointer);

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

                final idVec3 vectorPtr = var_a.getVectorPtr();
                switch (st.op) {
                    case OP_RETURN:
                        LeaveFunction(st.a);
                        break;

                    case OP_THREAD:
                        newThread = new idThread(this, st.a.value.functionPtr, st.b.value.getArgSize());
                        newThread.Start();

                        // return the thread number to the script
                        gameLocal.program.ReturnFloat(newThread.GetThreadNum());
                        PopParms(st.b.value.getArgSize());
                        break;

                    case OP_OBJTHREAD:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            func = obj.GetTypeDef().GetFunction(st.b.value.getVirtualFunction());
                            assert (st.c.value.getArgSize() == func.parmTotal);
                            newThread = new idThread(this, GetEntity(var_a.getEntityNumberPtr()), func, func.parmTotal);
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
                        EnterFunction(st.a.value.functionPtr, false);
                        break;

                    case OP_EVENTCALL:
                        CallEvent(st.a.value.functionPtr, st.b.value.getArgSize());
                        break;

                    case OP_OBJECTCALL:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            func = obj.GetTypeDef().GetFunction(st.b.value.getVirtualFunction());
                            EnterFunction(func, false);
                        } else {
                            // return a 'safe' value
                            gameLocal.program.ReturnVector(getVec3_zero());
                            gameLocal.program.ReturnString("");
                            PopParms(st.c.value.getArgSize());
                        }
                        break;

                    case OP_SYSCALL:
                        CallSysEvent(st.a.value.functionPtr, st.b.value.getArgSize());
                        break;

                    case OP_IFNOT:
                        var_a = GetVariable(st.a);
                        if (var_a.getIntPtr() == 0) {
                            NextInstruction(instructionPointer + st.b.value.getJumpOffset());
                        }
                        break;

                    case OP_IF:
                        var_a = GetVariable(st.a);
                        if (var_a.getIntPtr() != 0) {
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
                        var_c.setFloatPtr(var_a.getFloatPtr() + var_b.getFloatPtr());
                        break;

                    case OP_ADD_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setVectorPtr(vectorPtr.oPlus(var_b.getVectorPtr()));
                        break;

                    case OP_ADD_S:
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_FS:
                        var_a = GetVariable(st.a);
                        SetString(st.c, FloatToString(var_a.getFloatPtr()));
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_SF:
                        var_b = GetVariable(st.b);
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, FloatToString(var_b.getFloatPtr()));
                        break;

                    case OP_ADD_VS:
                        var_a = GetVariable(st.a);
                        SetString(st.c, vectorPtr.ToString());
                        AppendString(st.c, GetString(st.b));
                        break;

                    case OP_ADD_SV:
                        var_b = GetVariable(st.b);
                        SetString(st.c, GetString(st.a));
                        AppendString(st.c, var_b.getVectorPtr().ToString());
                        break;

                    case OP_SUB_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(var_a.getFloatPtr() - var_b.getFloatPtr());
                        break;

                    case OP_SUB_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setVectorPtr(vectorPtr.oMinus(var_b.getVectorPtr()));
                        break;

                    case OP_MUL_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(var_a.getFloatPtr() * var_b.getFloatPtr());
                        break;

                    case OP_MUL_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(vectorPtr.oMultiply(var_b.getVectorPtr()));
                        break;

                    case OP_MUL_FV:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setVectorPtr(var_b.getVectorPtr().oMultiply(var_a.getFloatPtr()));
                        break;

                    case OP_MUL_VF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.getVectorPtr().oSet(vectorPtr.oMultiply(var_b.getFloatPtr()));
                        break;

                    case OP_DIV_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);

                        if (var_b.getFloatPtr() == 0.0f) {
                            Warning("Divide by zero");
                            var_c.setFloatPtr(idMath.INFINITY);
                        } else {
                            var_c.setFloatPtr(var_a.getFloatPtr() / var_b.getFloatPtr());
                        }
                        break;

                    case OP_MOD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);

                        if (var_b.getFloatPtr() == 0.0f) {
                            Warning("Divide by zero");
                            var_c.setFloatPtr(var_a.getFloatPtr());
                        } else {
                            var_c.setFloatPtr(((int) var_a.getFloatPtr()) % ((int) var_b.getFloatPtr()));//TODO:casts!
                        }
                        break;

                    case OP_BITAND:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(((int) var_a.getFloatPtr()) & ((int) var_b.getFloatPtr()));
                        break;

                    case OP_BITOR:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(((int) var_a.getFloatPtr()) | ((int) var_b.getFloatPtr()));
                        break;

                    case OP_GE:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() >= var_b.getFloatPtr()));
                        break;

                    case OP_LE:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() <= var_b.getFloatPtr()));
                        break;

                    case OP_GT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() > var_b.getFloatPtr()));
                        break;

                    case OP_LT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() < var_b.getFloatPtr()));
                        break;

                    case OP_AND:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getFloatPtr() != 0.0f) && (var_b.getFloatPtr() != 0.0f)));
                        break;

                    case OP_AND_BOOLF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getIntPtr() != 0) && (var_b.getFloatPtr() != 0.0f)));
                        break;

                    case OP_AND_FBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getFloatPtr() != 0.0f) && (var_b.getIntPtr() != 0)));
                        break;

                    case OP_AND_BOOLBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getIntPtr() != 0) && (var_b.getIntPtr() != 0)));
                        break;

                    case OP_OR:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getFloatPtr() != 0.0f) || (var_b.getFloatPtr() != 0.0f)));
                        break;

                    case OP_OR_BOOLF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getIntPtr() != 0) || (var_b.getFloatPtr() != 0.0f)));
                        break;

                    case OP_OR_FBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getFloatPtr() != 0.0f) || (var_b.getIntPtr() != 0)));
                        break;

                    case OP_OR_BOOLBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi((var_a.getIntPtr() != 0) || (var_b.getIntPtr() != 0)));
                        break;

                    case OP_NOT_BOOL:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getIntPtr() == 0));
                        break;

                    case OP_NOT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() == 0.0f));
                        break;

                    case OP_NOT_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(vectorPtr.equals(getVec3_zero())));
                        break;

                    case OP_NOT_S:
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(!isNotNullOrEmpty(GetString(st.a))));
                        break;

                    case OP_NOT_ENT:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(GetEntity(var_a.getEntityNumberPtr()) == null));
                        break;

                    case OP_NEG_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(-var_a.getFloatPtr());
                        break;

                    case OP_NEG_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setVectorPtr(vectorPtr.oNegative());
                        break;

                    case OP_INT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(var_a.getFloatPtr());
                        break;

                    case OP_EQ_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() == var_b.getFloatPtr()));
                        break;

                    case OP_EQ_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(vectorPtr.equals(var_b.getVectorPtr())));
                        break;

                    case OP_EQ_S:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(idStr.Cmp(GetString(st.a), GetString(st.b)) == 0));
                        break;

                    case OP_EQ_E:
                    case OP_EQ_EO:
                    case OP_EQ_OE:
                    case OP_EQ_OO:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getEntityNumberPtr() == var_b.getEntityNumberPtr()));
                        break;

                    case OP_NE_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getFloatPtr() != var_b.getFloatPtr()));
                        break;

                    case OP_NE_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(!vectorPtr.equals(var_b.getVectorPtr())));
                        break;

                    case OP_NE_S:
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(idStr.Cmp(GetString(st.a), GetString(st.b)) != 0));
                        break;

                    case OP_NE_E:
                    case OP_NE_EO:
                    case OP_NE_OE:
                    case OP_NE_OO:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(btoi(var_a.getEntityNumberPtr() != var_b.getEntityNumberPtr()));
                        break;

                    case OP_UADD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(var_b.getFloatPtr() + var_a.getFloatPtr());
                        break;

                    case OP_UADD_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setVectorPtr(var_b.getVectorPtr().oPlus(vectorPtr));
                        break;

                    case OP_USUB_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(var_b.getFloatPtr() - var_a.getFloatPtr());
                        break;

                    case OP_USUB_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setVectorPtr(var_b.getVectorPtr().oMinus(vectorPtr));
                        break;

                    case OP_UMUL_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(var_b.getFloatPtr() * var_a.getFloatPtr());
                        break;

                    case OP_UMUL_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setVectorPtr(var_b.getVectorPtr().oMultiply(var_a.getFloatPtr()));
                        break;

                    case OP_UDIV_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.getFloatPtr() == 0.0f) {
                            Warning("Divide by zero");
                            var_b.setFloatPtr(idMath.INFINITY);
                        } else {
                            var_b.setFloatPtr(var_b.getFloatPtr() / var_a.getFloatPtr());
                        }
                        break;

                    case OP_UDIV_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.getFloatPtr() == 0.0f) {
                            Warning("Divide by zero");
                            var_b.setVectorPtr(new float[]{idMath.INFINITY, idMath.INFINITY, idMath.INFINITY});
                        } else {
                            var_b.setVectorPtr(var_b.getVectorPtr().oDivide(var_a.getFloatPtr()));
                        }
                        break;

                    case OP_UMOD_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);

                        if (var_a.getFloatPtr() == 0.0f) {
                            Warning("Divide by zero");
                            var_b.setFloatPtr(var_a.getFloatPtr());
                        } else {
                            var_b.setFloatPtr(((int) var_b.getFloatPtr()) % ((int) var_a.getFloatPtr()));
                        }
                        break;

                    case OP_UOR_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(((int) var_b.getFloatPtr()) | ((int) var_a.getFloatPtr()));
                        break;

                    case OP_UAND_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(((int) var_b.getFloatPtr()) & ((int) var_a.getFloatPtr()));
                        break;

                    case OP_UINC_F:
                        var_a = GetVariable(st.a);
                        var_a.setFloatPtr(var_a.getFloatPtr() + 1);
                        break;

                    case OP_UINCP_F:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var.setFloatPtr(var.getFloatPtr() + 1);
                        }
                        break;

                    case OP_UDEC_F:
                        var_a = GetVariable(st.a);
                        var_a.setFloatPtr(var_a.getFloatPtr() - 1);
                        break;

                    case OP_UDECP_F:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var.setFloatPtr(var.getFloatPtr() - 1);
                        }
                        break;

                    case OP_COMP_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        var_c.setFloatPtr(~((int) var_a.getFloatPtr()));
                        break;

                    case OP_STORE_F:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(var_a.getFloatPtr());
                        break;

                    case OP_STORE_ENT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setEntityNumberPtr(var_a.getEntityNumberPtr());
                        break;

                    case OP_STORE_BOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setIntPtr(var_a.getIntPtr());
                        break;

                    case OP_STORE_OBJENT:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (NOT(obj)) {
                            var_b.setEntityNumberPtr(0);
                        } else if (!obj.GetTypeDef().Inherits(st.b.TypeDef())) {
                            //Warning( "object '%s' cannot be converted to '%s'", obj.GetTypeName(), st.b.TypeDef().Name() );
                            var_b.setEntityNumberPtr(0);
                        } else {
                            var_b.setEntityNumberPtr(var_a.getEntityNumberPtr());
                        }
                        break;

                    case OP_STORE_OBJ:
                    case OP_STORE_ENTOBJ:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setEntityNumberPtr(var_a.getEntityNumberPtr());
                        break;

                    case OP_STORE_S:
                        SetString(st.b, GetString(st.a));
                        break;

                    case OP_STORE_V:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setVectorPtr(vectorPtr);
                        break;

                    case OP_STORE_FTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, FloatToString(var_a.getFloatPtr()));
                        break;

                    case OP_STORE_BTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, itob(var_a.getIntPtr()) ? "true" : "false");
                        break;

                    case OP_STORE_VTOS:
                        var_a = GetVariable(st.a);
                        SetString(st.b, vectorPtr.ToString());
                        break;

                    case OP_STORE_FTOBOOL:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        if (var_a.getFloatPtr() != 0.0f) {
                            var_b.setIntPtr(1);
                        } else {
                            var_b.setIntPtr(0);
                        }
                        break;

                    case OP_STORE_BOOLTOF:
                        var_a = GetVariable(st.a);
                        var_b = GetVariable(st.b);
                        var_b.setFloatPtr(Float.intBitsToFloat(var_a.getIntPtr()));
                        break;

                    case OP_STOREP_F:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.setFloatPtr(var_a.getFloatPtr());
                        }
                        break;

                    case OP_STOREP_ENT:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.setEntityNumberPtr(var_a.getEntityNumberPtr());
                        }
                        break;

                    case OP_STOREP_FLD:
                    case OP_STOREP_BOOL:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.setIntPtr(var_a.getIntPtr());
                        }
                        break;
                        
                    case OP_STOREP_S:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_b.evalPtr.stringPtr = GetString(st.a);//idStr.Copynz(var_b.evalPtr.stringPtr, GetString(st.a), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_V:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.setVectorPtr(vectorPtr);
                        }
                        break;

                    case OP_STOREP_FTOS:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.stringPtr = FloatToString(var_a.getFloatPtr());//idStr.Copynz(var_b.evalPtr.stringPtr, FloatToString(var_a.floatPtr.oGet()), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_BTOS:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            if (var_a.getFloatPtr() != 0.0f) {
                                var_b.evalPtr.stringPtr = "true";//idStr.Copynz(var_b.evalPtr.stringPtr, "true", MAX_STRING_LEN);
                            } else {
                                var_b.evalPtr.stringPtr = "false";//idStr.Copynz(var_b.evalPtr.stringPtr, "false", MAX_STRING_LEN);
                            }
                        }
                        break;

                    case OP_STOREP_VTOS:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.stringPtr = vectorPtr.ToString();//idStr.Copynz(var_b.evalPtr.stringPtr, var_a.vectorPtr[0].ToString(), MAX_STRING_LEN);
                        }
                        break;

                    case OP_STOREP_FTOBOOL:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            if (var_a.getFloatPtr() != 0.0f) {
                                var_b.evalPtr.setIntPtr(1);
                            } else {
                                var_b.evalPtr.setIntPtr(0);
                            }
                        }
                        break;

                    case OP_STOREP_BOOLTOF:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.setFloatPtr(Float.intBitsToFloat(var_a.getIntPtr()));
                        }
                        break;

                    case OP_STOREP_OBJ:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            var_b.evalPtr.setEntityNumberPtr(var_a.getEntityNumberPtr());
                        }
                        break;

                    case OP_STOREP_OBJENT:
                        var_b = GetEvalVariable(st.b);
                        if (var_b != null && var_b.evalPtr != null) {
                            var_a = GetVariable(st.a);
                            obj = GetScriptObject(var_a.getEntityNumberPtr());
                            if (NOT(obj)) {
                                var_b.evalPtr.setEntityNumberPtr(0);

                            // st.b points to type_pointer, which is just a temporary that gets its type reassigned, so we store the real type in st.c
                            // so that we can do a type check during run time since we don't know what type the script object is at compile time because it
                            // comes from an entity
                            } else if (!obj.GetTypeDef().Inherits(st.c.TypeDef())) {
                                //Warning( "object '%s' cannot be converted to '%s'", obj.GetTypeName(), st.c.TypeDef().Name() );
                                var_b.evalPtr.setEntityNumberPtr(0);
                            } else {
                                var_b.evalPtr.setEntityNumberPtr(var_a.getEntityNumberPtr());
                            }
                        }
                        break;

                    case OP_ADDRESS:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            obj.offset = st.b.value.getPtrOffset();
                            var_c.setEvalPtr(var_a.getEntityNumberPtr());
                        } else {
                            var_c.setEvalPtr(NULL_ENTITY);;
                        }
                        break;

                    case OP_INDIRECT_F:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var_c.setFloatPtr(var.getFloatPtr());
                        } else {
                            var_c.setFloatPtr(0.0f);
                        }
                        break;

                    case OP_INDIRECT_ENT:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var_c.setEntityNumberPtr(var.getEntityNumberPtr());
                        } else {
                            var_c.setEntityNumberPtr(0);
                        }
                        break;

                    case OP_INDIRECT_BOOL:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var_c.setIntPtr(var.getIntPtr());
                        } else {
                            var_c.setIntPtr(0);
                        }
                        break;

                    case OP_INDIRECT_S:
                        var_a = GetVariable(st.a);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setStringPtr(obj.data, st.b.value.getPtrOffset());
                            SetString(st.c, var.stringPtr);
                        } else {
                            SetString(st.c, "");
                        }
                        break;

                    case OP_INDIRECT_V:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (obj != null) {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var_c.setVectorPtr(var.getVectorPtr());
                        } else {
                            var_c.setVectorPtr(getVec3_zero());
                        }
                        break;

                    case OP_INDIRECT_OBJ:
                        var_a = GetVariable(st.a);
                        var_c = GetVariable(st.c);
                        obj = GetScriptObject(var_a.getEntityNumberPtr());
                        if (NOT(obj)) {
                            var_c.setEntityNumberPtr(0);
                        } else {
                            var.setBytePtr(obj.data, st.b.value.getPtrOffset());
                            var_c.setEntityNumberPtr(var.getEntityNumberPtr());
                        }
                        break;

                    case OP_PUSH_F:
                        var_a = GetVariable(st.a);
                        Push(var_a.getIntPtr());
                        break;

                    case OP_PUSH_FTOS:
                        var_a = GetVariable(st.a);
                        PushString(FloatToString(var_a.getFloatPtr()));
                        break;

                    case OP_PUSH_BTOF:
                        var_a = GetVariable(st.a);
                        floatVal = var_a.getIntPtr();
                        Push(Float.floatToIntBits(floatVal));
                        break;

                    case OP_PUSH_FTOB:
                        var_a = GetVariable(st.a);
                        if (var_a.getFloatPtr() != 0.0f) {
                            Push(1);
                        } else {
                            Push(0);
                        }
                        break;

                    case OP_PUSH_VTOS:
                        var_a = GetVariable(st.a);
                        PushString(vectorPtr.ToString());
                        break;

                    case OP_PUSH_BTOS:
                        var_a = GetVariable(st.a);
                        PushString(itob(var_a.getIntPtr()) ? "true" : "false");
                        break;

                    case OP_PUSH_ENT:
                        var_a = GetVariable(st.a);
                        Push(var_a.getEntityNumberPtr());
                        break;

                    case OP_PUSH_S:
                        PushString(GetString(st.a));
                        break;

                    case OP_PUSH_V:
                        var_a = GetVariable(st.a);
                        Push(Float.floatToIntBits(vectorPtr.x));
                        Push(Float.floatToIntBits(vectorPtr.y));
                        Push(Float.floatToIntBits(vectorPtr.z));
                        break;

                    case OP_PUSH_OBJ:
                        var_a = GetVariable(st.a);
                        Push(var_a.getEntityNumberPtr());
                        break;

                    case OP_PUSH_OBJENT:
                        var_a = GetVariable(st.a);
                        Push(var_a.getEntityNumberPtr());
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
                func = callStack[scopeDepth].f;
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
                    if (reg.getFloatPtr() != 0.0f) {
                        out.oSet(va("%g", reg.getFloatPtr()));
                    } else {
                        out.oSet("0");
                    }
                    return true;
//                    break;

                case ev_vector:
//                    if (reg.vectorPtr != null) {
                        final idVec3 vectorPtr = reg.getVectorPtr();
                        out.oSet(va("%g,%g,%g", vectorPtr.x, vectorPtr.y, vectorPtr.z));
//                    } else {
//                        out.oSet("0,0,0");
//                    }
                    return true;
//                    break;

                case ev_boolean:
                    if (reg.getIntPtr() != 0) {
                        out.oSet(va("%d", reg.getIntPtr()));
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
                        out.oPluSet(reg.stringPtr);
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
