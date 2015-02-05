package neo.Game.Script;

import static neo.Game.Game.SCRIPT_DEFAULTDEFS;
import static neo.Game.GameSys.Event.D_EVENT_ENTITY;
import static neo.Game.GameSys.Event.D_EVENT_ENTITY_NULL;
import static neo.Game.GameSys.Event.D_EVENT_FLOAT;
import static neo.Game.GameSys.Event.D_EVENT_INTEGER;
import static neo.Game.GameSys.Event.D_EVENT_STRING;
import static neo.Game.GameSys.Event.D_EVENT_TRACE;
import static neo.Game.GameSys.Event.D_EVENT_VECTOR;
import static neo.Game.GameSys.Event.D_EVENT_VOID;
import neo.Game.GameSys.Event.idEventDef;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Script.Script_Program.def_argsize;
import static neo.Game.Script.Script_Program.def_boolean;
import static neo.Game.Script.Script_Program.def_entity;
import static neo.Game.Script.Script_Program.def_field;
import static neo.Game.Script.Script_Program.def_float;
import static neo.Game.Script.Script_Program.def_function;
import static neo.Game.Script.Script_Program.def_jumpoffset;
import static neo.Game.Script.Script_Program.def_namespace;
import static neo.Game.Script.Script_Program.def_object;
import static neo.Game.Script.Script_Program.def_pointer;
import static neo.Game.Script.Script_Program.def_string;
import static neo.Game.Script.Script_Program.def_vector;
import static neo.Game.Script.Script_Program.def_void;
import static neo.Game.Script.Script_Program.ev_argsize;
import static neo.Game.Script.Script_Program.ev_boolean;
import static neo.Game.Script.Script_Program.ev_entity;
import static neo.Game.Script.Script_Program.ev_error;
import static neo.Game.Script.Script_Program.ev_field;
import static neo.Game.Script.Script_Program.ev_float;
import static neo.Game.Script.Script_Program.ev_function;
import static neo.Game.Script.Script_Program.ev_jumpoffset;
import static neo.Game.Script.Script_Program.ev_namespace;
import static neo.Game.Script.Script_Program.ev_object;
import static neo.Game.Script.Script_Program.ev_pointer;
import static neo.Game.Script.Script_Program.ev_string;
import static neo.Game.Script.Script_Program.ev_vector;
import static neo.Game.Script.Script_Program.ev_virtualfunction;
import static neo.Game.Script.Script_Program.ev_void;
import neo.Game.Script.Script_Program.eval_s;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idCompileError;
import neo.Game.Script.Script_Program.idTypeDef;
import neo.Game.Script.Script_Program.idVarDef;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.initializedConstant;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.uninitialized;
import neo.Game.Script.Script_Program.statement_s;
import static neo.Game.Script.Script_Program.type_argsize;
import static neo.Game.Script.Script_Program.type_boolean;
import static neo.Game.Script.Script_Program.type_entity;
import static neo.Game.Script.Script_Program.type_float;
import static neo.Game.Script.Script_Program.type_function;
import static neo.Game.Script.Script_Program.type_jumpoffset;
import static neo.Game.Script.Script_Program.type_namespace;
import static neo.Game.Script.Script_Program.type_object;
import static neo.Game.Script.Script_Program.type_pointer;
import static neo.Game.Script.Script_Program.type_scriptevent;
import static neo.Game.Script.Script_Program.type_string;
import static neo.Game.Script.Script_Program.type_vector;
import static neo.Game.Script.Script_Program.type_virtualfunction;
import static neo.Game.Script.Script_Program.type_void;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.indexOf;
import static neo.TempDump.itob;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOERRORS;
import static neo.idlib.Text.Lexer.P_PRECOMP;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_LITERAL;
import static neo.idlib.Text.Token.TT_NAME;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.Text.Token.TT_PUNCTUATION;
import static neo.idlib.Text.Token.TT_STRING;
import neo.idlib.Text.Token.idToken;
import neo.idlib.Timer.idTimer;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Script_Compiler {

    static final String RESULT_STRING = "<RESULT>";

    static class opcode_s {

        String   name;
        String   opname;
        int      priority;
        boolean  rightAssociative;
        idVarDef type_a;
        idVarDef type_b;
        idVarDef type_c;

        public opcode_s(String name, String opname, int priority, boolean rightAssociative, idVarDef type_a, idVarDef type_b, idVarDef type_c) {
            this.name = name;
            this.opname = opname;
            this.priority = priority;
            this.rightAssociative = rightAssociative;
            this.type_a = type_a;
            this.type_b = type_b;
            this.type_c = type_c;
        }
    };
    // These opcodes are no longer necessary:
// OP_PUSH_OBJ:
// OP_PUSH_OBJENT:
//enum {
    static final int OP_RETURN         = 0;
    //
    static final int OP_UINC_F         = 1;
    static final int OP_UINCP_F        = 2;
    static final int OP_UDEC_F         = 3;
    static final int OP_UDECP_F        = 4;
    static final int OP_COMP_F         = 5;
    //
    static final int OP_MUL_F          = 6;
    static final int OP_MUL_V          = 7;
    static final int OP_MUL_FV         = 8;
    static final int OP_MUL_VF         = 9;
    static final int OP_DIV_F          = 10;
    static final int OP_MOD_F          = 11;
    static final int OP_ADD_F          = 12;
    static final int OP_ADD_V          = 13;
    static final int OP_ADD_S          = 14;
    static final int OP_ADD_FS         = 15;
    static final int OP_ADD_SF         = 16;
    static final int OP_ADD_VS         = 17;
    static final int OP_ADD_SV         = 18;
    static final int OP_SUB_F          = 19;
    static final int OP_SUB_V          = 20;
    //
    static final int OP_EQ_F           = 21;
    static final int OP_EQ_V           = 22;
    static final int OP_EQ_S           = 23;
    static final int OP_EQ_E           = 24;
    static final int OP_EQ_EO          = 25;
    static final int OP_EQ_OE          = 26;
    static final int OP_EQ_OO          = 27;
    //
    static final int OP_NE_F           = 28;
    static final int OP_NE_V           = 29;
    static final int OP_NE_S           = 30;
    static final int OP_NE_E           = 31;
    static final int OP_NE_EO          = 32;
    static final int OP_NE_OE          = 33;
    static final int OP_NE_OO          = 34;
    //
    static final int OP_LE             = 35;
    static final int OP_GE             = 36;
    static final int OP_LT             = 37;
    static final int OP_GT             = 38;
    //
    static final int OP_INDIRECT_F     = 39;
    static final int OP_INDIRECT_V     = 40;
    static final int OP_INDIRECT_S     = 41;
    static final int OP_INDIRECT_ENT   = 42;
    static final int OP_INDIRECT_BOOL  = 43;
    static final int OP_INDIRECT_OBJ   = 44;
    //
    static final int OP_ADDRESS        = 45;
    //
    static final int OP_EVENTCALL      = 46;
    static final int OP_OBJECTCALL     = 47;
    static final int OP_SYSCALL        = 48;
    //
    static final int OP_STORE_F        = 49;
    static final int OP_STORE_V        = 50;
    static final int OP_STORE_S        = 51;
    static final int OP_STORE_ENT      = 52;
    static final int OP_STORE_BOOL     = 53;
    static final int OP_STORE_OBJENT   = 54;
    static final int OP_STORE_OBJ      = 55;
    static final int OP_STORE_ENTOBJ   = 56;
    //
    static final int OP_STORE_FTOS     = 57;
    static final int OP_STORE_BTOS     = 58;
    static final int OP_STORE_VTOS     = 59;
    static final int OP_STORE_FTOBOOL  = 60;
    static final int OP_STORE_BOOLTOF  = 61;
    //
    static final int OP_STOREP_F       = 62;
    static final int OP_STOREP_V       = 63;
    static final int OP_STOREP_S       = 64;
    static final int OP_STOREP_ENT     = 65;
    static final int OP_STOREP_FLD     = 66;
    static final int OP_STOREP_BOOL    = 67;
    static final int OP_STOREP_OBJ     = 68;
    static final int OP_STOREP_OBJENT  = 69;
    //
    static final int OP_STOREP_FTOS    = 70;
    static final int OP_STOREP_BTOS    = 71;
    static final int OP_STOREP_VTOS    = 72;
    static final int OP_STOREP_FTOBOOL = 73;
    static final int OP_STOREP_BOOLTOF = 74;
    //
    static final int OP_UMUL_F         = 75;
    static final int OP_UMUL_V         = 76;
    static final int OP_UDIV_F         = 77;
    static final int OP_UDIV_V         = 78;
    static final int OP_UMOD_F         = 79;
    static final int OP_UADD_F         = 80;
    static final int OP_UADD_V         = 81;
    static final int OP_USUB_F         = 82;
    static final int OP_USUB_V         = 83;
    static final int OP_UAND_F         = 84;
    static final int OP_UOR_F          = 85;
    //
    static final int OP_NOT_BOOL       = 86;
    static final int OP_NOT_F          = 87;
    static final int OP_NOT_V          = 88;
    static final int OP_NOT_S          = 89;
    static final int OP_NOT_ENT        = 90;
    //
    static final int OP_NEG_F          = 91;
    static final int OP_NEG_V          = 92;
    //
    static final int OP_INT_F          = 93;
    static final int OP_IF             = 94;
    static final int OP_IFNOT          = 95;
    //
    static final int OP_CALL           = 96;
    static final int OP_THREAD         = 97;
    static final int OP_OBJTHREAD      = 98;
    //
    static final int OP_PUSH_F         = 99;
    static final int OP_PUSH_V         = 100;
    static final int OP_PUSH_S         = 101;
    static final int OP_PUSH_ENT       = 102;
    static final int OP_PUSH_OBJ       = 103;
    static final int OP_PUSH_OBJENT    = 104;
    static final int OP_PUSH_FTOS      = 105;
    static final int OP_PUSH_BTOF      = 106;
    static final int OP_PUSH_FTOB      = 107;
    static final int OP_PUSH_VTOS      = 108;
    static final int OP_PUSH_BTOS      = 109;
    //
    static final int OP_GOTO           = 110;
    //
    static final int OP_AND            = 111;
    static final int OP_AND_BOOLF      = 112;
    static final int OP_AND_FBOOL      = 113;
    static final int OP_AND_BOOLBOOL   = 114;
    static final int OP_OR             = 115;
    static final int OP_OR_BOOLF       = 116;
    static final int OP_OR_FBOOL       = 117;
    static final int OP_OR_BOOLBOOL    = 118;
    //
    static final int OP_BITAND         = 119;
    static final int OP_BITOR          = 120;
    //
    static final int OP_BREAK          = 121;            // placeholder op.  not used in final code
    static final int OP_CONTINUE       = 122;        // placeholder op.  not used in final code
    //
    static final int NUM_OPCODES       = 123;
    //};
//    
//    
    static final int FUNCTION_PRIORITY = 2;
    static final int INT_PRIORITY      = 2;
    static final int NOT_PRIORITY      = 5;
    static final int TILDE_PRIORITY    = 5;
    static final int TOP_PRIORITY      = 7;
//    

    static class idCompiler {

        private static boolean[] punctuationValid = new boolean[256];
        private static final String[] punctuation = {
            "+=", "-=", "*=", "/=", "%=", "&=", "|=", "++", "--",
            "&&", "||", "<=", ">=", "==", "!=", "::", ";", ",",
            "~", "!", "*", "/", "%", "(", ")", "-", "+",
            "=", "[", "]", ".", "<", ">", "&", "|", ":", null
        };
        //
        private final        idParser  parser           = new idParser();
        private idParser parserPtr;
        private idToken token = new idToken();
        //
        private idTypeDef immediateType;
        private eval_s    immediate;
        //
        private boolean   eof;
        private boolean   console;
        private boolean   callthread;
        private int       braceDepth;
        private int       loopDepth;
        private int       currentLineNumber;
        private int       currentFileNumber;
        private int       errorCount;
        //
        private idVarDef  scope;                // the function being parsed, or NULL
        private idVarDef  basetype;            // for accessing fields
        //
        public static final opcode_s[] opcodes = {
            new opcode_s("<RETURN>", "RETURN", -1, false, def_void, def_void, def_void),
            //
            new opcode_s("++", "UINC_F", 1, true, def_float, def_void, def_void),
            new opcode_s("++", "UINCP_F", 1, true, def_object, def_field, def_float),
            new opcode_s("--", "UDEC_F", 1, true, def_float, def_void, def_void),
            new opcode_s("--", "UDECP_F", 1, true, def_object, def_field, def_float),
            //
            new opcode_s("~", "COMP_F", -1, false, def_float, def_void, def_float),
            //
            new opcode_s("*", "MUL_F", 3, false, def_float, def_float, def_float),
            new opcode_s("*", "MUL_V", 3, false, def_vector, def_vector, def_float),
            new opcode_s("*", "MUL_FV", 3, false, def_float, def_vector, def_vector),
            new opcode_s("*", "MUL_VF", 3, false, def_vector, def_float, def_vector),
            //
            new opcode_s("/", "DIV", 3, false, def_float, def_float, def_float),
            new opcode_s("%", "MOD_F", 3, false, def_float, def_float, def_float),
            //
            new opcode_s("+", "ADD_F", 4, false, def_float, def_float, def_float),
            new opcode_s("+", "ADD_V", 4, false, def_vector, def_vector, def_vector),
            new opcode_s("+", "ADD_S", 4, false, def_string, def_string, def_string),
            new opcode_s("+", "ADD_FS", 4, false, def_float, def_string, def_string),
            new opcode_s("+", "ADD_SF", 4, false, def_string, def_float, def_string),
            new opcode_s("+", "ADD_VS", 4, false, def_vector, def_string, def_string),
            new opcode_s("+", "ADD_SV", 4, false, def_string, def_vector, def_string),
            //
            new opcode_s("-", "SUB_F", 4, false, def_float, def_float, def_float),
            new opcode_s("-", "SUB_V", 4, false, def_vector, def_vector, def_vector),
            //
            new opcode_s("==", "EQ_F", 5, false, def_float, def_float, def_float),
            new opcode_s("==", "EQ_V", 5, false, def_vector, def_vector, def_float),
            new opcode_s("==", "EQ_S", 5, false, def_string, def_string, def_float),
            new opcode_s("==", "EQ_E", 5, false, def_entity, def_entity, def_float),
            new opcode_s("==", "EQ_EO", 5, false, def_entity, def_object, def_float),
            new opcode_s("==", "EQ_OE", 5, false, def_object, def_entity, def_float),
            new opcode_s("==", "EQ_OO", 5, false, def_object, def_object, def_float),
            //
            new opcode_s("!=", "NE_F", 5, false, def_float, def_float, def_float),
            new opcode_s("!=", "NE_V", 5, false, def_vector, def_vector, def_float),
            new opcode_s("!=", "NE_S", 5, false, def_string, def_string, def_float),
            new opcode_s("!=", "NE_E", 5, false, def_entity, def_entity, def_float),
            new opcode_s("!=", "NE_EO", 5, false, def_entity, def_object, def_float),
            new opcode_s("!=", "NE_OE", 5, false, def_object, def_entity, def_float),
            new opcode_s("!=", "NE_OO", 5, false, def_object, def_object, def_float),
            //
            new opcode_s("<=", "LE", 5, false, def_float, def_float, def_float),
            new opcode_s(">=", "GE", 5, false, def_float, def_float, def_float),
            new opcode_s("<", "LT", 5, false, def_float, def_float, def_float),
            new opcode_s(">", "GT", 5, false, def_float, def_float, def_float),
            //
            new opcode_s(".", "INDIRECT_F", 1, false, def_object, def_field, def_float),
            new opcode_s(".", "INDIRECT_V", 1, false, def_object, def_field, def_vector),
            new opcode_s(".", "INDIRECT_S", 1, false, def_object, def_field, def_string),
            new opcode_s(".", "INDIRECT_E", 1, false, def_object, def_field, def_entity),
            new opcode_s(".", "INDIRECT_BOOL", 1, false, def_object, def_field, def_boolean),
            new opcode_s(".", "INDIRECT_OBJ", 1, false, def_object, def_field, def_object),
            //
            new opcode_s(".", "ADDRESS", 1, false, def_entity, def_field, def_pointer),
            //
            new opcode_s(".", "EVENTCALL", 2, false, def_entity, def_function, def_void),
            new opcode_s(".", "OBJECTCALL", 2, false, def_object, def_function, def_void),
            new opcode_s(".", "SYSCALL", 2, false, def_void, def_function, def_void),
            //
            new opcode_s("=", "STORE_F", 6, true, def_float, def_float, def_float),
            new opcode_s("=", "STORE_V", 6, true, def_vector, def_vector, def_vector),
            new opcode_s("=", "STORE_S", 6, true, def_string, def_string, def_string),
            new opcode_s("=", "STORE_ENT", 6, true, def_entity, def_entity, def_entity),
            new opcode_s("=", "STORE_BOOL", 6, true, def_boolean, def_boolean, def_boolean),
            new opcode_s("=", "STORE_OBJENT", 6, true, def_object, def_entity, def_object),
            new opcode_s("=", "STORE_OBJ", 6, true, def_object, def_object, def_object),
            new opcode_s("=", "STORE_OBJENT", 6, true, def_entity, def_object, def_object),
            //
            new opcode_s("=", "STORE_FTOS", 6, true, def_string, def_float, def_string),
            new opcode_s("=", "STORE_BTOS", 6, true, def_string, def_boolean, def_string),
            new opcode_s("=", "STORE_VTOS", 6, true, def_string, def_vector, def_string),
            new opcode_s("=", "STORE_FTOBOOL", 6, true, def_boolean, def_float, def_boolean),
            new opcode_s("=", "STORE_BOOLTOF", 6, true, def_float, def_boolean, def_float),
            //
            new opcode_s("=", "STOREP_F", 6, true, def_pointer, def_float, def_float),
            new opcode_s("=", "STOREP_V", 6, true, def_pointer, def_vector, def_vector),
            new opcode_s("=", "STOREP_S", 6, true, def_pointer, def_string, def_string),
            new opcode_s("=", "STOREP_ENT", 6, true, def_pointer, def_entity, def_entity),
            new opcode_s("=", "STOREP_FLD", 6, true, def_pointer, def_field, def_field),
            new opcode_s("=", "STOREP_BOOL", 6, true, def_pointer, def_boolean, def_boolean),
            new opcode_s("=", "STOREP_OBJ", 6, true, def_pointer, def_object, def_object),
            new opcode_s("=", "STOREP_OBJENT", 6, true, def_pointer, def_object, def_object),
            //
            new opcode_s("<=>", "STOREP_FTOS", 6, true, def_pointer, def_float, def_string),
            new opcode_s("<=>", "STOREP_BTOS", 6, true, def_pointer, def_boolean, def_string),
            new opcode_s("<=>", "STOREP_VTOS", 6, true, def_pointer, def_vector, def_string),
            new opcode_s("<=>", "STOREP_FTOBOOL", 6, true, def_pointer, def_float, def_boolean),
            new opcode_s("<=>", "STOREP_BOOLTOF", 6, true, def_pointer, def_boolean, def_float),
            //
            new opcode_s("*=", "UMUL_F", 6, true, def_float, def_float, def_void),
            new opcode_s("*=", "UMUL_V", 6, true, def_vector, def_float, def_void),
            new opcode_s("/=", "UDIV_F", 6, true, def_float, def_float, def_void),
            new opcode_s("/=", "UDIV_V", 6, true, def_vector, def_float, def_void),
            new opcode_s("%=", "UMOD_F", 6, true, def_float, def_float, def_void),
            new opcode_s("+=", "UADD_F", 6, true, def_float, def_float, def_void),
            new opcode_s("+=", "UADD_V", 6, true, def_vector, def_vector, def_void),
            new opcode_s("-=", "USUB_F", 6, true, def_float, def_float, def_void),
            new opcode_s("-=", "USUB_V", 6, true, def_vector, def_vector, def_void),
            new opcode_s("&=", "UAND_F", 6, true, def_float, def_float, def_void),
            new opcode_s("|=", "UOR_F", 6, true, def_float, def_float, def_void),
            //
            new opcode_s("!", "NOT_BOOL", -1, false, def_boolean, def_void, def_float),
            new opcode_s("!", "NOT_F", -1, false, def_float, def_void, def_float),
            new opcode_s("!", "NOT_V", -1, false, def_vector, def_void, def_float),
            new opcode_s("!", "NOT_S", -1, false, def_vector, def_void, def_float),
            new opcode_s("!", "NOT_ENT", -1, false, def_entity, def_void, def_float),
            //
            new opcode_s("<NEG_F>", "NEG_F", -1, false, def_float, def_void, def_float),
            new opcode_s("<NEG_V>", "NEG_V", -1, false, def_vector, def_void, def_vector),
            //
            new opcode_s("int", "INT_F", -1, false, def_float, def_void, def_float),
            //
            new opcode_s("<IF>", "IF", -1, false, def_float, def_jumpoffset, def_void),
            new opcode_s("<IFNOT>", "IFNOT", -1, false, def_float, def_jumpoffset, def_void),
            //
            // calls returns REG_RETURN
            new opcode_s("<CALL>", "CALL", -1, false, def_function, def_argsize, def_void),
            new opcode_s("<THREAD>", "THREAD", -1, false, def_function, def_argsize, def_void),
            new opcode_s("<THREAD>", "OBJTHREAD", -1, false, def_function, def_argsize, def_void),
            //
            new opcode_s("<PUSH>", "PUSH_F", -1, false, def_float, def_float, def_void),
            new opcode_s("<PUSH>", "PUSH_V", -1, false, def_vector, def_vector, def_void),
            new opcode_s("<PUSH>", "PUSH_S", -1, false, def_string, def_string, def_void),
            new opcode_s("<PUSH>", "PUSH_ENT", -1, false, def_entity, def_entity, def_void),
            new opcode_s("<PUSH>", "PUSH_OBJ", -1, false, def_object, def_object, def_void),
            new opcode_s("<PUSH>", "PUSH_OBJENT", -1, false, def_entity, def_object, def_void),
            new opcode_s("<PUSH>", "PUSH_FTOS", -1, false, def_string, def_float, def_void),
            new opcode_s("<PUSH>", "PUSH_BTOF", -1, false, def_float, def_boolean, def_void),
            new opcode_s("<PUSH>", "PUSH_FTOB", -1, false, def_boolean, def_float, def_void),
            new opcode_s("<PUSH>", "PUSH_VTOS", -1, false, def_string, def_vector, def_void),
            new opcode_s("<PUSH>", "PUSH_BTOS", -1, false, def_string, def_boolean, def_void),
            //
            new opcode_s("<GOTO>", "GOTO", -1, false, def_jumpoffset, def_void, def_void),
            //
            new opcode_s("&&", "AND", 7, false, def_float, def_float, def_float),
            new opcode_s("&&", "AND_BOOLF", 7, false, def_boolean, def_float, def_float),
            new opcode_s("&&", "AND_FBOOL", 7, false, def_float, def_boolean, def_float),
            new opcode_s("&&", "AND_BOOLBOOL", 7, false, def_boolean, def_boolean, def_float),
            new opcode_s("||", "OR", 7, false, def_float, def_float, def_float),
            new opcode_s("||", "OR_BOOLF", 7, false, def_boolean, def_float, def_float),
            new opcode_s("||", "OR_FBOOL", 7, false, def_float, def_boolean, def_float),
            new opcode_s("||", "OR_BOOLBOOL", 7, false, def_boolean, def_boolean, def_float),
            //
            new opcode_s("&", "BITAND", 3, false, def_float, def_float, def_float),
            new opcode_s("|", "BITOR", 3, false, def_float, def_float, def_float),
            //
            new opcode_s("<BREAK>", "BREAK", -1, false, def_float, def_void, def_void),
            new opcode_s("<CONTINUE>", "CONTINUE", -1, false, def_float, def_void, def_void),
            //
            null
        };
//
//

        private float Divide(float numerator, float denominator) {
            if (denominator == 0) {
                Error("Divide by zero");
                return 0;
            }

            return numerator / denominator;
        }

        /*
         ============
         idCompiler::Error

         Aborts the current file load
         ============
         */
        private void Error(String fmt, Object... args) {//const id_attribute((format(printf,2,3)));

//            va_list argptr;
//            char[] string = new char[1024];
//
//            va_start(argptr, message);
//            vsprintf(string, message, argptr);
//            va_end(argptr);
//
            throw new idCompileError(String.format(fmt, args));
        }

        /*
         ============
         idCompiler::Warning

         Prints a warning about the current line
         ============
         */
        private void Warning(String fmt, Object... args) {// const id_attribute((format(printf,2,3)));

//            va_list argptr;
//            char[] string = new char[1024];
//
//            va_start(argptr, message);
//            vsprintf(string, message, argptr);
//            va_end(argptr);
//
            parserPtr.Warning("%s", String.format(fmt, args));
        }

        /*
         ============
         idCompiler::OptimizeOpcode

         try to optimize when the operator works on constants only
         ============
         */
        private idVarDef OptimizeOpcode(final opcode_s op, idVarDef var_a, idVarDef var_b) {
            eval_s c = new eval_s();
            idTypeDef type;

//            if (var_a != null && var_a.initialized != initializedConstant) {
            if (var_a == null || var_a.initialized != initializedConstant) {
                return null;
            }
//            if (var_b != null && var_b.initialized != initializedConstant) {
            if (var_b == null || var_b.initialized != initializedConstant) {
                return null;
            }

//            idVec3 &vec_c = *reinterpret_cast<idVec3 *>( &c.vector[ 0 ] );
            idVec3 vec_c = new idVec3(c.vector(0), c.vector(1), c.vector(2));//TODO:why are we using an array that is deleted below?

//	memset( &c, 0, sizeof( c ) );
            c = new eval_s();
            switch (indexOf(op, opcodes)) {
                case OP_ADD_F:
                    c._float(var_a.value.getFloatPtr() + var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_ADD_V:
                    vec_c = var_a.value.getVectorPtr().oPlus(var_b.value.getVectorPtr());
                    type = type_vector;
                    break;
                case OP_SUB_F:
                    c._float(var_a.value.getFloatPtr() - var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_SUB_V:
                    vec_c = var_a.value.getVectorPtr().oMinus(var_b.value.getVectorPtr());
                    type = type_vector;
                    break;
                case OP_MUL_F:
                    c._float(var_a.value.getFloatPtr() * var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_MUL_V:
                    c._float(var_a.value.getVectorPtr().oMultiply(var_b.value.getVectorPtr()));
                    type = type_float;
                    break;
                case OP_MUL_FV:
                    vec_c = var_b.value.getVectorPtr().oMultiply(var_a.value.getFloatPtr());
                    type = type_vector;
                    break;
                case OP_MUL_VF:
                    vec_c = var_a.value.getVectorPtr().oMultiply(var_b.value.getFloatPtr());
                    type = type_vector;
                    break;
                case OP_DIV_F:
                    c._float(Divide(var_a.value.getFloatPtr(), var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_MOD_F:
                    c._float((int) var_a.value.getFloatPtr() % (int) var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_BITAND:
                    c._float((int) var_a.value.getFloatPtr() & (int) var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_BITOR:
                    c._float((int) var_a.value.getFloatPtr() | (int) var_b.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_GE:
                    c._float(btoi(var_a.value.getFloatPtr() >= var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_LE:
                    c._float(btoi(var_a.value.getFloatPtr() <= var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_GT:
                    c._float(btoi(var_a.value.getFloatPtr() > var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_LT:
                    c._float(btoi(var_a.value.getFloatPtr() < var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_AND:
                    c._float(btoi(var_a.value.getFloatPtr() != 0 && var_b.value.getFloatPtr() != 0));
                    type = type_float;
                    break;
                case OP_OR:
                    c._float(btoi(var_a.value.getFloatPtr() != 0 || var_b.value.getFloatPtr() != 0));
                    type = type_float;
                    break;
                case OP_NOT_BOOL:
                    c._int(btoi(!itob(var_a.value.getIntPtr())));
                    type = type_boolean;
                    break;
                case OP_NOT_F:
                    c._float(btoi(!itob((int) var_a.value.getFloatPtr())));
                    type = type_float;
                    break;
                case OP_NOT_V:
                    c._float(btoi(0 == var_a.value.getVectorPtr().x
                            && 0 == var_a.value.getVectorPtr().y
                            && 0 == var_a.value.getVectorPtr().z));
                    type = type_float;
                    break;
                case OP_NEG_F:
                    c._float(-var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_NEG_V:
                    vec_c = var_a.value.getVectorPtr().oNegative();
                    type = type_vector;
                    break;
                case OP_INT_F:
                    c._float((int) var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_EQ_F:
                    c._float(btoi(var_a.value.getFloatPtr() == var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_EQ_V:
                    c._float(btoi(var_a.value.getVectorPtr().Compare(var_b.value.getVectorPtr())));
                    type = type_float;
                    break;
                case OP_EQ_E:
                    c._float(btoi(var_a.value.getIntPtr() == var_b.value.getIntPtr()));
                    type = type_float;
                    break;
                case OP_NE_F:
                    c._float(btoi(var_a.value.getFloatPtr() != var_b.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_NE_V:
                    c._float(btoi(!var_a.value.getVectorPtr().Compare(var_b.value.getVectorPtr())));
                    type = type_float;
                    break;
                case OP_NE_E:
                    c._float(btoi(var_a.value.getIntPtr() != var_b.value.getIntPtr()));
                    type = type_float;
                    break;
                case OP_UADD_F:
                    c._float(var_b.value.getFloatPtr() + var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_USUB_F:
                    c._float(var_b.value.getFloatPtr() - var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_UMUL_F:
                    c._float(var_b.value.getFloatPtr() * var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                case OP_UDIV_F:
                    c._float(Divide(var_b.value.getFloatPtr(), var_a.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_UMOD_F:
                    c._float(((int) var_b.value.getFloatPtr()) % ((int) var_a.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_UOR_F:
                    c._float(((int) var_b.value.getFloatPtr()) | ((int) var_a.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_UAND_F:
                    c._float(((int) var_b.value.getFloatPtr()) & ((int) var_a.value.getFloatPtr()));
                    type = type_float;
                    break;
                case OP_UINC_F:
                    c._float(var_a.value.getFloatPtr() + 1);
                    type = type_float;
                    break;
                case OP_UDEC_F:
                    c._float(var_a.value.getFloatPtr() - 1);
                    type = type_float;
                    break;
                case OP_COMP_F:
                    c._float((float) ~(int) var_a.value.getFloatPtr());
                    type = type_float;
                    break;
                default:
                    type = null;
                    break;
            }

            if (null == type) {
                return null;
            }

            if (var_a != null) {
                var_a.numUsers--;
                if (var_a.numUsers <= 0) {
                    gameLocal.program.FreeDef(var_a, null);
                }
            }
            if (var_b != null) {
                var_b.numUsers--;
                if (var_b.numUsers <= 0) {
                    gameLocal.program.FreeDef(var_b, null);
                }
            }

            return GetImmediate(type, c, "");
        }

        /*
         ============
         idCompiler::EmitOpcode

         Emits a primitive statement, returning the var it places it's value in
         ============
         */
        private idVarDef EmitOpcode(final opcode_s op, idVarDef var_a, idVarDef var_b) {
            statement_s statement;
            idVarDef var_c;

            var_c = OptimizeOpcode(op, var_a, var_b);
            if (var_c != null) {
                return var_c;
            }

            if (var_a != null && var_a.Name().equals(RESULT_STRING)) {
                var_a.numUsers++;
            }
            if (var_b != null && var_b.Name().equals(RESULT_STRING)) {
                var_b.numUsers++;
            }

            statement = gameLocal.program.AllocStatement();
            statement.linenumber = currentLineNumber;
            statement.file = currentFileNumber;

            if ((op.type_c == def_void) || op.rightAssociative) {
                // ifs, gotos, and assignments don't need vars allocated
                var_c = null;
            } else {
                // allocate result space
                // try to reuse result defs as much as possible
                var_c = gameLocal.program.FindFreeResultDef(op.type_c.TypeDef(), RESULT_STRING, scope, var_a, var_b);
                // set user count back to 1, a result def needs to be used twice before it can be reused
                var_c.numUsers = 1;
            }

            statement.op = indexOf(op, opcodes);
            statement.a = var_a;
            statement.b = var_b;
            statement.c = var_c;

            if (op.rightAssociative) {
                return var_a;
            }

            return var_c;
        }

        /*
         ============
         idCompiler::EmitOpcode

         Emits a primitive statement, returning the var it places it's value in
         ============
         */
        private idVarDef EmitOpcode(int op, idVarDef var_a, idVarDef var_b) {
            return EmitOpcode(opcodes[ op], var_a, var_b);
        }

        /*
         ============
         idCompiler::EmitPush

         Emits an opcode to push the variable onto the stack.
         ============
         */
        private boolean EmitPush(idVarDef expression, final idTypeDef funcArg) {
            opcode_s op;
            opcode_s out;
            int op_ptr;

            out = null;
            for (op = opcodes[ op_ptr = OP_PUSH_F]; op.name != null && op.name.equals("<PUSH>"); op = opcodes[ ++op_ptr]) {
                if ((funcArg.Type() == op.type_a.Type()) && (expression.Type() == op.type_b.Type())) {
                    out = op;
                    break;
                }
            }

            if (null == out) {
                if ((expression.TypeDef() != funcArg) && !expression.TypeDef().Inherits(funcArg)) {
                    return false;
                }

                out = opcodes[ OP_PUSH_ENT];
            }

            EmitOpcode(out, expression, null);

            return true;
        }

        /*
         ==============
         idCompiler::NextToken

         Sets token, immediateType, and possibly immediate
         ==============
         */
        static int bla2 = 0;

        private void NextToken() {
            int i;

            // reset our type
            immediateType = null;
//	memset( &immediate, 0, sizeof( immediate ) );
            immediate = new eval_s();

            // Save the token's line number and filename since when we emit opcodes the current 
            // token is always the next one to be read 
            currentLineNumber = token.line;
            currentFileNumber = gameLocal.program.GetFilenum(parserPtr.GetFileName().toString());

            bla2++;
            if (!parserPtr.ReadToken(token)) {
                eof = true;
                return;
            }

            if (currentFileNumber != gameLocal.program.GetFilenum(parserPtr.GetFileName().toString())) {
                if ((braceDepth > 0) && !token.equals("}")) {
                    // missing a closing brace.  try to give as much info as possible.
                    if (scope.Type() == ev_function) {
                        Error("Unexpected end of file inside function '%s'.  Missing closing braces.", scope.Name());
                    } else if (scope.Type() == ev_object) {
                        Error("Unexpected end of file inside object '%s'.  Missing closing braces.", scope.Name());
                    } else if (scope.Type() == ev_namespace) {
                        Error("Unexpected end of file inside namespace '%s'.  Missing closing braces.", scope.Name());
                    } else {
                        Error("Unexpected end of file inside braced section");
                    }
                }
            }

            switch (token.type) {
                case TT_STRING:
                    // handle quoted strings as a unit
                    immediateType = type_string;
                    return;

                case TT_LITERAL: {
                    // handle quoted vectors as a unit
                    immediateType = type_vector;
                    idLexer lex = new idLexer(token.toString(), token.Length(), parserPtr.GetFileName().toString(), LEXFL_NOERRORS);
                    idToken token2 = new idToken();
                    for (i = 0; i < 3; i++) {
                        if (!lex.ReadToken(token2)) {
                            Error("Couldn't read vector. '%s' is not in the form of 'x y z'", token);
                        }
                        if (token2.type == TT_PUNCTUATION && token2.equals("-")) {
                            if (NOT(lex.CheckTokenType(TT_NUMBER, 0, token2))) {
                                Error("expected a number following '-' but found '%s' in vector '%s'", token2, token);
                            }
                            immediate.vector(i, -token2.GetFloatValue());
                        } else if (token2.type == TT_NUMBER) {
                            immediate.vector(i, token2.GetFloatValue());
                        } else {
                            Error("vector '%s' is not in the form of 'x y z'.  expected float value, found '%s'", token, token2);
                        }
                    }
                    return;
                }

                case TT_NUMBER:
                    immediateType = type_float;
                    immediate._float(token.GetFloatValue());
                    return;

                case TT_PUNCTUATION:
                    // entity names
                    if (token.equals("$")) {
                        immediateType = type_entity;
                        parserPtr.ReadToken(token);
                        return;
                    }

                    if (token.equals("{")) {
                        braceDepth++;
                        return;
                    }

                    if (token.equals("}")) {
                        braceDepth--;
                        return;
                    }

                    if (punctuationValid[ token.subtype]) {
                        return;
                    }

                    Error("Unknown punctuation '%s'", token);
                    break;

                case TT_NAME:
                    return;

                default:
                    Error("Unknown token '%s'", token);
            }
        }

        /*
         =============
         idCompiler::ExpectToken

         Issues an Error if the current token isn't equal to string
         Gets the next token
         =============
         */
        private void ExpectToken(final String string) {
            if (!token.equals(string)) {
                Error("expected '%s', found '%s'", string, token);
            }

            NextToken();
        }

        /*
         =============
         idCompiler::CheckToken

         Returns true and gets the next token if the current token equals string
         Returns false and does nothing otherwise
         =============
         */
        private boolean CheckToken(final String string) {
            if (!token.toString().equals(string)) {//TODO:try to use the idStr::Cmp in the overridden token.equals() method.
                return false;
            }

            NextToken();

            return true;
        }

        /*
         ============
         idCompiler::ParseName

         Checks to see if the current token is a valid name
         ============
         */
        private void ParseName(idStr name) {
            if (token.type != TT_NAME) {
                Error("'%s' is not a name", token);
            }

            name.oSet(token);
            NextToken();
        }

        /*
         ============
         idCompiler::SkipOutOfFunction

         For error recovery, pops out of nested braces
         ============
         */
        private void SkipOutOfFunction() {
            while (braceDepth != 0) {
                parserPtr.SkipBracedSection(false);
                braceDepth--;
            }
            NextToken();
        }

        /*
         ============
         idCompiler::SkipToSemicolon

         For error recovery
         ============
         */
        private void SkipToSemicolon() {
            do {
                if (CheckToken(";")) {
                    return;
                }

                NextToken();
            } while (!eof);
        }
        /*
         ============
         idCompiler::CheckType

         Parses a variable type, including functions types
         ============
         */

        private idTypeDef CheckType() {
            idTypeDef type;

            if (token.equals("float")) {
                type = type_float;
            } else if (token.equals("vector")) {
                type = type_vector;
            } else if (token.equals("entity")) {
                type = type_entity;
            } else if (token.equals("string")) {
                type = type_string;
            } else if (token.equals("void")) {
                type = type_void;
            } else if (token.equals("object")) {
                type = type_object;
            } else if (token.equals("boolean")) {
                type = type_boolean;
            } else if (token.equals("namespace")) {
                type = type_namespace;
            } else if (token.equals("scriptEvent")) {
                type = type_scriptevent;
            } else {
                type = gameLocal.program.FindType(token.toString());
                if (type != null && !type.Inherits(type_object)) {
                    type = null;
                }
            }

            return type;
        }

        /*
         ============
         idCompiler::ParseType

         Parses a variable type, including functions types
         ============
         */
        private idTypeDef ParseType() {
            idTypeDef type;

            type = CheckType();
            if (null == type) {
                Error("\"%s\" is not a type", token.toString());
            }

            if ((type == type_scriptevent) && (scope != def_namespace)) {
                Error("scriptEvents can only defined in the global namespace");
            }

            if ((type == type_namespace) && (scope.Type() != ev_namespace)) {
                Error("A namespace may only be defined globally, or within another namespace");
            }

            NextToken();

            return type;
        }

        /*
         ============
         idCompiler::FindImmediate

         tries to find an existing immediate with the same value
         ============
         */
        private idVarDef FindImmediate(final idTypeDef type, final eval_s eval, final String string) {
            idVarDef def;
            int/*ctype_t*/ etype;

            etype = type.Type();

            // check for a constant with the same value
            for (def = gameLocal.program.GetDefList("<IMMEDIATE>"); def != null; def = def.Next()) {
                if (def.TypeDef() != type) {
                    continue;
                }

                switch (etype) {
                    case ev_field:
                        if (def.value.getIntPtr() == eval._int()) {
                            return def;
                        }
                        break;

                    case ev_argsize:
                        if (def.value.getArgSize() == eval._int()) {
                            return def;
                        }
                        break;

                    case ev_jumpoffset:
                        if (def.value.getJumpOffset() == eval._int()) {
                            return def;
                        }
                        break;

                    case ev_entity:
                        if (def.value.getIntPtr() == eval.entity()) {
                            return def;
                        }
                        break;

                    case ev_string:
                        if (idStr.Cmp(def.value.getStringPtr(), string) == 0) {
                            return def;
                        }
                        break;

                    case ev_float:
                        if (def.value.getFloatPtr() == eval._float()) {
                            return def;
                        }
                        break;

                    case ev_virtualfunction:
                        if (def.value.getVirtualFunction() == eval._int()) {
                            return def;
                        }
                        break;

                    case ev_vector:
                        if ((def.value.getVectorPtr().x == eval.vector(0))
                                && (def.value.getVectorPtr().y == eval.vector(1))
                                && (def.value.getVectorPtr().z == eval.vector(2))) {
                            return def;
                        }
                        break;

                    default:
                        Error("weird immediate type");
                        break;
                }
            }

            return null;
        }

        /*
         ============
         idCompiler::GetImmediate

         returns an existing immediate with the same value, or allocates a new one
         ============
         */
        private idVarDef GetImmediate(idTypeDef type, final eval_s eval, final String string) {
            idVarDef def;

            def = FindImmediate(type, eval, string);
            if (def != null) {
                def.numUsers++;
            } else {
                // allocate a new def
                def = gameLocal.program.AllocDef(type, "<IMMEDIATE>", def_namespace, true);
                if (type.Type() == ev_string) {
                    def.SetString(string, true);
                } else {
                    def.SetValue(eval, true);
                }
            }

            return def;
        }

        /*
         ============
         idCompiler::VirtualFunctionConstant

         Creates a def for an index into a virtual function table
         ============
         */
        private idVarDef VirtualFunctionConstant(idVarDef func) {
            eval_s eval;

//	memset( &eval, 0, sizeof( eval ) );
            eval = new eval_s();
            eval._int(func.scope.TypeDef().GetFunctionNumber(func.value.getFunctionPtr()));
            if (eval._int() < 0) {
                Error("Function '%s' not found in scope '%s'", func.Name(), func.scope.Name());
            }

            return GetImmediate(type_virtualfunction, eval, "");
        }

        /*
         ============
         idCompiler::SizeConstant

         Creates a def for a size constant
         ============
         */
        private idVarDef SizeConstant(int size) {
            eval_s eval;

//	memset( &eval, 0, sizeof( eval ) );
            eval = new eval_s();
            eval._int(size);
            return GetImmediate(type_argsize, eval, "");
        }

        /*
         ============
         idCompiler::JumpConstant

         Creates a def for a jump constant
         ============
         */
        private idVarDef JumpConstant(int value) {
            eval_s eval;

//	memset( &eval, 0, sizeof( eval ) );
            eval = new eval_s();
            eval._int(value);
            return GetImmediate(type_jumpoffset, eval, "");
        }

        /*
         ============
         idCompiler::JumpDef

         Creates a def for a relative jump from one code location to another
         ============
         */
        private idVarDef JumpDef(int jumpfrom, int jumpto) {
            return JumpConstant(jumpto - jumpfrom);
        }

        /*
         ============
         idCompiler::JumpTo

         Creates a def for a relative jump from current code location
         ============
         */
        private idVarDef JumpTo(int jumpto) {
            return JumpDef(gameLocal.program.NumStatements(), jumpto);
        }

        /*
         ============
         idCompiler::JumpFrom

         Creates a def for a relative jump from code location to current code location
         ============
         */
        private idVarDef JumpFrom(int jumpfrom) {
            return JumpDef(jumpfrom, gameLocal.program.NumStatements());
        }

        /*
         ============
         idCompiler::ParseImmediate

         Looks for a preexisting constant
         ============
         */
        private idVarDef ParseImmediate() {
            idVarDef def;

            blaaaa++;
            def = GetImmediate(immediateType, immediate, token.toString());
            NextToken();

            return def;
        }
        static int blaaaa = 0;

        private idVarDef EmitFunctionParms(int op, idVarDef func, int startarg, int startsize, idVarDef object) {
            idVarDef e;
            idTypeDef type;
            idTypeDef funcArg;
            idVarDef returnDef;
            idTypeDef returnType;
            int arg;
            int size;
            int resultOp;

            type = func.TypeDef();
            if (func.Type() != ev_function) {
                Error("'%s' is not a function", func.Name());
            }

            // copy the parameters to the global parameter variables
            arg = startarg;
            size = startsize;
            if (!CheckToken(")")) {
                do {
                    if (arg >= type.NumParameters()) {
                        Error("too many parameters");
                    }

                    e = GetExpression(TOP_PRIORITY);

                    funcArg = type.GetParmType(arg);
                    if (!EmitPush(e, funcArg)) {
                        Error("type mismatch on parm %d of call to '%s'", arg + 1, func.Name());
                    }

                    if (funcArg.Type() == ev_object) {
                        size += type_object.Size();
                    } else {
                        size += funcArg.Size();
                    }

                    arg++;
                } while (CheckToken(","));

                ExpectToken(")");
            }

            if (arg < type.NumParameters()) {
                Error("too few parameters for function '%s'", func.Name());
            }

            if (op == OP_CALL) {
                EmitOpcode(op, func, null);
            } else if ((op == OP_OBJECTCALL) || (op == OP_OBJTHREAD)) {
                EmitOpcode(op, object, VirtualFunctionConstant(func));

                // need arg size seperate since script object may be NULL
                statement_s statement = gameLocal.program.GetStatement(gameLocal.program.NumStatements() - 1);
                statement.c = SizeConstant(func.value.getFunctionPtr().parmTotal);
            } else {
                EmitOpcode(op, func, SizeConstant(size));
            }

            // we need to copy off the result into a temporary result location, so figure out the opcode
            returnType = type.ReturnType();
            if (returnType.Type() == ev_string) {
                resultOp = OP_STORE_S;
                returnDef = gameLocal.program.returnStringDef;
            } else {
                gameLocal.program.returnDef.SetTypeDef(returnType);
                returnDef = gameLocal.program.returnDef;

                switch (returnType.Type()) {
                    case ev_void:
                        resultOp = OP_STORE_F;
                        break;

                    case ev_boolean:
                        resultOp = OP_STORE_BOOL;
                        break;

                    case ev_float:
                        resultOp = OP_STORE_F;
                        break;

                    case ev_vector:
                        resultOp = OP_STORE_V;
                        break;

                    case ev_entity:
                        resultOp = OP_STORE_ENT;
                        break;

                    case ev_object:
                        resultOp = OP_STORE_OBJ;
                        break;

                    default:
                        Error("Invalid return type for function '%s'", func.Name());
                        // shut up compiler
                        resultOp = OP_STORE_OBJ;
                        break;
                }
            }

            if (returnType.Type() == ev_void) {
                // don't need result space since there's no result, so just return the normal result def.
                return returnDef;
            }

            // allocate result space
            // try to reuse result defs as much as possible
            statement_s statement = gameLocal.program.GetStatement(gameLocal.program.NumStatements() - 1);
            idVarDef resultDef = gameLocal.program.FindFreeResultDef(returnType, RESULT_STRING, scope, statement.a, statement.b);
            // set user count back to 0, a result def needs to be used twice before it can be reused
            resultDef.numUsers = 0;

            EmitOpcode(resultOp, returnDef, resultDef);

            return resultDef;
        }

        private idVarDef ParseFunctionCall(idVarDef funcDef) {
            assert (funcDef != null);

            if (funcDef.Type() != ev_function) {
                Error("'%s' is not a function", funcDef.Name());
            }

            if (funcDef.initialized == uninitialized) {
                Error("Function '%s' has not been defined yet", funcDef.GlobalName());
            }

            assert (funcDef.value.getFunctionPtr() != null);
            if (callthread) {
                if ((funcDef.initialized != uninitialized) && funcDef.value.getFunctionPtr().eventdef != null) {
                    Error("Built-in functions cannot be called as threads");
                }
                callthread = false;
                return EmitFunctionParms(OP_THREAD, funcDef, 0, 0, null);
            } else {
                if ((funcDef.initialized != uninitialized) && funcDef.value.getFunctionPtr().eventdef != null) {
                    if ((scope.Type() != ev_namespace) && (scope.scope.Type() == ev_object)) {
                        // get the local object pointer
                        idVarDef thisdef = gameLocal.program.GetDef(scope.scope.TypeDef(), "self", scope);
                        if (NOT(thisdef)) {
                            Error("No 'self' within scope");
                        }

                        return ParseEventCall(thisdef, funcDef);
                    } else {
                        Error("Built-in functions cannot be called without an object");
                    }
                }

                return EmitFunctionParms(OP_CALL, funcDef, 0, 0, null);
            }
        }

        private idVarDef ParseObjectCall(idVarDef object, idVarDef func) {
            EmitPush(object, object.TypeDef());
            if (callthread) {
                callthread = false;
                return EmitFunctionParms(OP_OBJTHREAD, func, 1, type_object.Size(), object);
            } else {
                return EmitFunctionParms(OP_OBJECTCALL, func, 1, 0, object);
            }
        }

        private idVarDef ParseEventCall(idVarDef object, idVarDef funcDef) {
            if (callthread) {
                Error("Cannot call built-in functions as a thread");
            }

            if (funcDef.Type() != ev_function) {
                Error("'%s' is not a function", funcDef.Name());
            }

            if (NOT(funcDef.value.getFunctionPtr().eventdef)) {
                Error("\"%s\" cannot be called with object notation", funcDef.Name());
            }

            if (object.Type() == ev_object) {
                EmitPush(object, type_entity);
            } else {
                EmitPush(object, object.TypeDef());
            }

            return EmitFunctionParms(OP_EVENTCALL, funcDef, 0, type_object.Size(), null);
        }

        private idVarDef ParseSysObjectCall(idVarDef funcDef) {
            if (callthread) {
                Error("Cannot call built-in functions as a thread");
            }

            if (funcDef.Type() != ev_function) {
                Error("'%s' is not a function", funcDef.Name());
            }

            if (NOT(funcDef.value.getFunctionPtr().eventdef)) {
                Error("\"%s\" cannot be called with object notation", funcDef.Name());
            }

//            //TODO:fix this.
//            if (!idThread.Type.RespondsTo(funcDef.value.functionPtr.eventdef)) {
//                Error("\"%s\" is not callable as a 'sys' function", funcDef.Name());
//            }
            return EmitFunctionParms(OP_SYSCALL, funcDef, 0, 0, null);
        }

        static int bla = 0;

        private idVarDef LookupDef(final String name, final idVarDef baseobj) {
            idVarDef def;
            idVarDef field;
            int/*ctype_t*/ type_b;
            int/*ctype_t*/ type_c;
            opcode_s op;
            int op_i;

            bla++;

            // check if we're accessing a field
            if (baseobj != null && (baseobj.Type() == ev_object)) {
                idVarDef tdef;

                def = null;
                for (tdef = baseobj; tdef != def_object; tdef = tdef.TypeDef().SuperClass().def) {
                    def = gameLocal.program.GetDef(null, name, tdef);
                    if (def != null) {
                        break;
                    }
                }
            } else {
                // first look through the defs in our scope
                def = gameLocal.program.GetDef(null, name, scope);
                if (NOT(def)) {
                    // if we're in a member function, check types local to the object
                    if ((scope.Type() != ev_namespace) && (scope.scope.Type() == ev_object)) {
                        // get the local object pointer
                        idVarDef thisdef = gameLocal.program.GetDef(scope.scope.TypeDef(), "self", scope);

                        field = LookupDef(name, scope.scope.TypeDef().def);
                        if (NOT(field)) {
                            Error("Unknown value \"%s\"", name);
                        }

                        // type check
                        type_b = field.Type();
                        if (field.Type() == ev_function) {
                            type_c = field.TypeDef().ReturnType().Type();
                        } else {
                            type_c = field.TypeDef().FieldType().Type();	// field access gets type from field
                            if (CheckToken("++")) {
                                if (type_c != ev_float) {
                                    Error("Invalid type for ++");
                                }
                                def = EmitOpcode(OP_UINCP_F, thisdef, field);
                                return def;
                            } else if (CheckToken("--")) {
                                if (type_c != ev_float) {
                                    Error("Invalid type for --");
                                }
                                def = EmitOpcode(OP_UDECP_F, thisdef, field);
                                return def;
                            }
                        }

                        op = opcodes[op_i = OP_INDIRECT_F];
                        while ((op.type_a.Type() != ev_object)
                                || (type_b != op.type_b.Type()) || (type_c != op.type_c.Type())) {
                            if ((op.priority == FUNCTION_PRIORITY) && (op.type_a.Type() == ev_object) && (op.type_c.Type() == ev_void)
                                    && (type_c != op.type_c.Type())) {
                                // catches object calls that return a value
                                break;
                            }
                            op = opcodes[++op_i];
                            if (null == op.name || !op.name.equals(".")) {
                                Error("no valid opcode to access type '%s'", field.TypeDef().SuperClass().Name());
                            }
                        }

//				if ( ( op - opcodes ) == OP_OBJECTCALL ) {
                        if (op_i == OP_OBJECTCALL) {
                            ExpectToken("(");
                            def = ParseObjectCall(thisdef, field);
                        } else {
                            // emit the conversion opcode
                            def = EmitOpcode(op, thisdef, field);

                            // field access gets type from field
                            def.SetTypeDef(field.TypeDef().FieldType());
                        }
                    }
                }
            }

            return def;
        }

        /*
         ============
         idCompiler::ParseValue

         Returns the def for the current token
         ============
         */
        private idVarDef ParseValue() {
            idVarDef def;
            idVarDef namespaceDef;
            idStr name = new idStr();

            if (immediateType == type_entity) {
                // if an immediate entity ($-prefaced name) then create or lookup a def for it.
                // when entities are spawned, they'll lookup the def and point it to them.
                def = gameLocal.program.GetDef(type_entity, "$" + token, def_namespace);
                if (NOT(def)) {
                    def = gameLocal.program.AllocDef(type_entity, "$" + token, def_namespace, true);
                }
                NextToken();
                return def;
            } else if (immediateType != null) {
                // if the token is an immediate, allocate a constant for it
                return ParseImmediate();
            }

            ParseName(name);
            def = LookupDef(name.toString(), basetype);
            if (NOT(def)) {
                if (basetype != null) {
                    Error("%s is not a member of %s", name, basetype.TypeDef().Name());
                } else {
                    Error("Unknown value \"%s\"", name);
                }
                // if namespace, then look up the variable in that namespace
            } else if (def.Type() == ev_namespace) {
                while (def.Type() == ev_namespace) {
                    ExpectToken("::");
                    ParseName(name);
                    namespaceDef = def;
                    def = gameLocal.program.GetDef(null, name.toString(), namespaceDef);
                    if (NOT(def)) {
                        Error("Unknown value \"%s::%s\"", namespaceDef.GlobalName(), name);
                    }
                }
                //def = LookupDef( name, basetype );
            }

            return def;
        }

        private idVarDef GetTerm() {
            idVarDef e;
            int op;

            if (NOT(immediateType) && CheckToken("~")) {
                e = GetExpression(TILDE_PRIORITY);
                switch (e.Type()) {
                    case ev_float:
                        op = OP_COMP_F;
                        break;

                    default:
                        Error("type mismatch for ~");

                        // shut up compiler
                        op = OP_COMP_F;
                        break;
                }

                return EmitOpcode(op, e, null);
            }

            if (NOT(immediateType) && CheckToken("!")) {
                e = GetExpression(NOT_PRIORITY);
                switch (e.Type()) {
                    case ev_boolean:
                        op = OP_NOT_BOOL;
                        break;

                    case ev_float:
                        op = OP_NOT_F;
                        break;

                    case ev_string:
                        op = OP_NOT_S;
                        break;

                    case ev_vector:
                        op = OP_NOT_V;
                        break;

                    case ev_entity:
                        op = OP_NOT_ENT;
                        break;

                    case ev_function:
                        Error("Invalid type for !");

                        // shut up compiler
                        op = OP_NOT_F;
                        break;

                    case ev_object:
                        op = OP_NOT_ENT;
                        break;

                    default:
                        Error("type mismatch for !");

                        // shut up compiler
                        op = OP_NOT_F;
                        break;
                }

                return EmitOpcode(op, e, null);
            }

            // check for negation operator
            if (NOT(immediateType) && CheckToken("-")) {
                // constants are directly negated without an instruction
                if (immediateType == type_float) {
                    immediate._float(-immediate._float());
                    return ParseImmediate();
                } else if (immediateType == type_vector) {
                    immediate.vector(0, -immediate.vector(0));
                    immediate.vector(1, -immediate.vector(1));
                    immediate.vector(2, -immediate.vector(2));
                    return ParseImmediate();
                } else {
                    e = GetExpression(NOT_PRIORITY);
                    switch (e.Type()) {
                        case ev_float:
                            op = OP_NEG_F;
                            break;

                        case ev_vector:
                            op = OP_NEG_V;
                            break;
                        default:
                            Error("type mismatch for -");

                            // shut up compiler
                            op = OP_NEG_F;
                            break;
                    }
                    return EmitOpcode(opcodes[op], e, null);
                }
            }

            if (CheckToken("int")) {
                ExpectToken("(");

                e = GetExpression(INT_PRIORITY);
                if (e.Type() != ev_float) {
                    Error("type mismatch for int()");
                }

                ExpectToken(")");

                return EmitOpcode(OP_INT_F, e, null);
            }

            if (CheckToken("thread")) {
                callthread = true;
                e = GetExpression(FUNCTION_PRIORITY);

                if (callthread) {
                    Error("Invalid thread call");
                }

                // threads return the thread number
                gameLocal.program.returnDef.SetTypeDef(type_float);
                return gameLocal.program.returnDef;
            }

            if (NOT(immediateType) && CheckToken("(")) {
                e = GetExpression(TOP_PRIORITY);
                ExpectToken(")");

                return e;
            }

            return ParseValue();
        }

        private boolean TypeMatches(int/*ctype_t*/ type1, int/*ctype_t*/ type2) {

            //if ( ( type1 == ev_entity ) && ( type2 == ev_object ) ) {
            //	return true;
            //}
            //if ( ( type2 == ev_entity ) && ( type1 == ev_object ) ) {
            //	return true;
            //}
            return type1 == type2;
        }

        private idVarDef GetExpression(int priority) {
            opcode_s op;
            opcode_s oldop;
            idVarDef e;
            idVarDef e2;
            idVarDef oldtype;
            int/*ctype_t*/ type_a;
            int/*ctype_t*/ type_b;
            int/*ctype_t*/ type_c;
            int op_i;

            if (priority == 0) {
                return GetTerm();
            }

            e = GetExpression(priority - 1);
            if (token.equals(";")) {
                // save us from searching through the opcodes unneccesarily
                return e;
            }

            while (true) {
                if ((priority == FUNCTION_PRIORITY) && CheckToken("(")) {
                    return ParseFunctionCall(e);
                }

                // has to be a punctuation
                if (immediateType != null) {
                    break;
                }

                for (op = opcodes[op_i = 0];
                        op_i < opcodes.length && op != null && op.name != null;
                        op = opcodes[++op_i]) {
                    if ((op.priority == priority) && CheckToken(op.name)) {
                        break;
                    }
                }

                if (null == op || null == op.name) {
                    // next token isn't at this priority level
                    break;
                }

                // unary operators act only on the left operand
                if (op.type_b == def_void) {
                    e = EmitOpcode(op, e, null);
                    return e;
                }

                // preserve our base type
                oldtype = basetype;

                // field access needs scope from object
                if ((op.name.charAt(0) == '.') && e.TypeDef().Inherits(type_object)) {
                    // save off what type this field is part of
                    basetype = e.TypeDef().def;
                }

                if (op.rightAssociative) {
                    // if last statement is an indirect, change it to an address of
                    if (gameLocal.program.NumStatements() > 0) {
                        statement_s statement = gameLocal.program.GetStatement(gameLocal.program.NumStatements() - 1);
                        if ((statement.op >= OP_INDIRECT_F) && (statement.op < OP_ADDRESS)) {
                            statement.op = OP_ADDRESS;
                            type_pointer.SetPointerType(e.TypeDef());
                            e.SetTypeDef(type_pointer);
                        }
                    }

                    e2 = GetExpression(priority);
                } else {
                    e2 = GetExpression(priority - 1);
                }

                // restore type
                basetype = oldtype;

                // type check
                type_a = e.Type();
                type_b = e2.Type();

                // field access gets type from field
                if (op.name.charAt(0) == '.') {
                    if ((e2.Type() == ev_function) && e2.TypeDef().ReturnType() != null) {
                        type_c = e2.TypeDef().ReturnType().Type();
                    } else if (e2.TypeDef().FieldType() != null) {
                        type_c = e2.TypeDef().FieldType().Type();
                    } else {
                        // not a field
                        type_c = ev_error;
                    }
                } else {
                    type_c = ev_void;
                }

                oldop = op;
                while (!TypeMatches(type_a, op.type_a.Type()) || !TypeMatches(type_b, op.type_b.Type())
                        || ((type_c != ev_void) && !TypeMatches(type_c, op.type_c.Type()))) {
                    if ((op.priority == FUNCTION_PRIORITY) && TypeMatches(type_a, op.type_a.Type()) && TypeMatches(type_b, op.type_b.Type())) {
                        break;
                    }

                    op = opcodes[++op_i];
                    if (null == op.name || !op.name.equals(oldop.name)) {
                        Error("type mismatch for '%s'", oldop.name);
                    }
                }

//		switch( op - opcodes ) {
                switch (op_i) {
                    case OP_SYSCALL:
                        ExpectToken("(");
                        e = ParseSysObjectCall(e2);
                        break;

                    case OP_OBJECTCALL:
                        ExpectToken("(");
                        if ((e2.initialized != uninitialized) && e2.value.getFunctionPtr().eventdef != null) {
                            e = ParseEventCall(e, e2);
                        } else {
                            e = ParseObjectCall(e, e2);
                        }
                        break;

                    case OP_EVENTCALL:
                        ExpectToken("(");
                        if ((e2.initialized != uninitialized) && e2.value.getFunctionPtr().eventdef != null) {
                            e = ParseEventCall(e, e2);
                        } else {
                            e = ParseObjectCall(e, e2);
                        }
                        break;

                    default:
                        if (callthread) {
                            Error("Expecting function call after 'thread'");
                        }

                        if ((type_a == ev_pointer) && (type_b != e.TypeDef().PointerType().Type())) {
                            // FIXME: need to make a general case for this
//				if ( ( op - opcodes == OP_STOREP_F ) && ( e.TypeDef().PointerType().Type() == ev_boolean ) ) {
                            if ((op_i == OP_STOREP_F) && (e.TypeDef().PointerType().Type() == ev_boolean)) {
                                // copy from float to boolean pointer
                                op = opcodes[op_i = OP_STOREP_FTOBOOL];
                            } else if ((op_i == OP_STOREP_BOOL) && (e.TypeDef().PointerType().Type() == ev_float)) {
                                // copy from boolean to float pointer
                                op = opcodes[op_i = OP_STOREP_BOOLTOF];
                            } else if ((op_i == OP_STOREP_F) && (e.TypeDef().PointerType().Type() == ev_string)) {
                                // copy from float to string pointer
                                op = opcodes[ op_i = OP_STOREP_FTOS];
                            } else if ((op_i == OP_STOREP_BOOL) && (e.TypeDef().PointerType().Type() == ev_string)) {
                                // copy from boolean to string pointer
                                op = opcodes[ op_i = OP_STOREP_BTOS];
                            } else if ((op_i == OP_STOREP_V) && (e.TypeDef().PointerType().Type() == ev_string)) {
                                // copy from vector to string pointer
                                op = opcodes[ op_i = OP_STOREP_VTOS];
                            } else if ((op_i == OP_STOREP_ENT) && (e.TypeDef().PointerType().Type() == ev_object)) {
                                // store an entity into an object pointer
                                op = opcodes[ op_i = OP_STOREP_OBJENT];
                            } else {
                                Error("type mismatch for '%s'", op.name);
                            }
                        }

                        if (op.rightAssociative) {
                            e = EmitOpcode(op, e2, e);
                        } else {
                            e = EmitOpcode(op, e, e2);
                        }

                        if (op_i == OP_STOREP_OBJENT) {
                            // statement.b points to type_pointer, which is just a temporary that gets its type reassigned, so we store the real type in statement.c
                            // so that we can do a type check during run time since we don't know what type the script object is at compile time because it
                            // comes from an entity
                            statement_s statement = gameLocal.program.GetStatement(gameLocal.program.NumStatements() - 1);
                            statement.c = type_pointer.PointerType().def;
                        }

                        // field access gets type from field
                        if (type_c != ev_void) {
                            e.SetTypeDef(e2.TypeDef().FieldType());
                        }
                        break;
                }
            }

            return e;
        }

        private idTypeDef GetTypeForEventArg(char argType) {
            idTypeDef type;

            switch (argType) {
                case D_EVENT_INTEGER:
                    // this will get converted to int by the interpreter
                    type = type_float;
                    break;

                case D_EVENT_FLOAT:
                    type = type_float;
                    break;

                case D_EVENT_VECTOR:
                    type = type_vector;
                    break;

                case D_EVENT_STRING:
                    type = type_string;
                    break;

                case D_EVENT_ENTITY:
                case D_EVENT_ENTITY_NULL:
                    type = type_entity;
                    break;

                case D_EVENT_VOID:
                    type = type_void;
                    break;

                case D_EVENT_TRACE:
                    // This data type isn't available from script
                    type = null;
                    break;

                default:
                    // probably a typo
                    type = null;
                    break;
            }

            return type;
        }

        private void PatchLoop(int start, int continuePos) {
            int i;
            statement_s pos;

            pos = gameLocal.program.GetStatement(start);
            for (i = start; i < gameLocal.program.NumStatements(); pos = gameLocal.program.GetStatement(++i)) {
                if (pos.op == OP_BREAK) {
                    pos.op = OP_GOTO;
                    pos.a = JumpFrom(i);
                } else if (pos.op == OP_CONTINUE) {
                    pos.op = OP_GOTO;
                    pos.a = JumpDef(i, continuePos);
                }
            }
        }

        private void ParseReturnStatement() {
            idVarDef e;
            int/*ctype_t*/ type_a;
            int/*ctype_t*/ type_b;
            opcode_s op;
            int op_i;

            if (CheckToken(";")) {
                if (scope.TypeDef().ReturnType().Type() != ev_void) {
                    Error("expecting return value");
                }

                EmitOpcode(OP_RETURN, null, null);
                return;
            }

            e = GetExpression(TOP_PRIORITY);
            ExpectToken(";");

            type_a = e.Type();
            type_b = scope.TypeDef().ReturnType().Type();

            if (TypeMatches(type_a, type_b)) {
                EmitOpcode(OP_RETURN, e, null);
                return;
            }

            for (op = opcodes[op_i = 0]; op.name != null; op = opcodes[++op_i]) {
                if (op.name.equals("=")) {
                    break;
                }
            }

            assert (op.name != null);

            while (!TypeMatches(type_a, op.type_a.Type()) || !TypeMatches(type_b, op.type_b.Type())) {
                op = opcodes[++op_i];
                if (null == op.name || !op.name.equals("=")) {
                    Error("type mismatch for return value");
                }
            }

            idTypeDef returnType = scope.TypeDef().ReturnType();
            if (returnType.Type() == ev_string) {
                EmitOpcode(op, e, gameLocal.program.returnStringDef);
            } else {
                gameLocal.program.returnDef.SetTypeDef(returnType);
                EmitOpcode(op, e, gameLocal.program.returnDef);
            }
            EmitOpcode(OP_RETURN, null, null);
        }

        private void ParseWhileStatement() {
            idVarDef e;
            int patch1;
            int patch2;

            loopDepth++;

            ExpectToken("(");

            patch2 = gameLocal.program.NumStatements();
            e = GetExpression(TOP_PRIORITY);
            ExpectToken(")");

            if ((e.initialized == initializedConstant) && (e.value.getIntPtr() != 0)) {
//                    && (e.value.getIntPtr() != null && e.value.getIntPtr().oGet() != 0)) {
                //FIXME: we can completely skip generation of this code in the opposite case
                ParseStatement();
                EmitOpcode(OP_GOTO, JumpTo(patch2), null);
            } else {
                patch1 = gameLocal.program.NumStatements();
                EmitOpcode(OP_IFNOT, e, null);
                ParseStatement();
                EmitOpcode(OP_GOTO, JumpTo(patch2), null);
                gameLocal.program.GetStatement(patch1).b = JumpFrom(patch1);
            }

            // fixup breaks and continues
            PatchLoop(patch2, patch2);

            loopDepth--;
        }

        /*
         ================
         idCompiler::ParseForStatement

         Form of for statement with a counter:

         a = 0;
         start:					<< patch4
         if ( !( a < 10 ) ) {
         goto end;		<< patch1
         } else {
         goto process;	<< patch3
         }

         increment:				<< patch2
         a = a + 1;
         goto start;			<< goto patch4

         process:
         statements;
         goto increment;		<< goto patch2

         end:

         Form of for statement without a counter:

         a = 0;
         start:					<< patch2
         if ( !( a < 10 ) ) {
         goto end;		<< patch1
         }

         process:
         statements;
         goto start;			<< goto patch2

         end:
         ================
         */
        private void ParseForStatement() {
            idVarDef e;
            int start;
            int patch1;
            int patch2;
            int patch3;
            int patch4;

            loopDepth++;

            start = gameLocal.program.NumStatements();

            ExpectToken("(");

            // init
            if (!CheckToken(";")) {
                do {
                    GetExpression(TOP_PRIORITY);
                } while (CheckToken(","));

                ExpectToken(";");
            }

            // condition
            patch2 = gameLocal.program.NumStatements();

            e = GetExpression(TOP_PRIORITY);
            ExpectToken(";");

            //FIXME: add check for constant expression
            patch1 = gameLocal.program.NumStatements();
            EmitOpcode(OP_IFNOT, e, null);

            // counter
            if (!CheckToken(")")) {
                patch3 = gameLocal.program.NumStatements();
                EmitOpcode(OP_IF, e, null);

                patch4 = patch2;
                patch2 = gameLocal.program.NumStatements();
                do {
                    GetExpression(TOP_PRIORITY);
                } while (CheckToken(","));

                ExpectToken(")");

                // goto patch4
                EmitOpcode(OP_GOTO, JumpTo(patch4), null);

                // fixup patch3
                gameLocal.program.GetStatement(patch3).b = JumpFrom(patch3);
            }

            ParseStatement();

            // goto patch2
            EmitOpcode(OP_GOTO, JumpTo(patch2), null);

            // fixup patch1
            gameLocal.program.GetStatement(patch1).b = JumpFrom(patch1);

            // fixup breaks and continues
            PatchLoop(start, patch2);

            loopDepth--;
        }

        private void ParseDoWhileStatement() {
            idVarDef e;
            int patch1;

            loopDepth++;

            patch1 = gameLocal.program.NumStatements();
            ParseStatement();
            ExpectToken("while");
            ExpectToken("(");
            e = GetExpression(TOP_PRIORITY);
            ExpectToken(")");
            ExpectToken(";");

            EmitOpcode(OP_IF, e, JumpTo(patch1));

            // fixup breaks and continues
            PatchLoop(patch1, patch1);

            loopDepth--;
        }

        private void ParseIfStatement() {
            idVarDef e;
            int patch1;
            int patch2;

            ExpectToken("(");
            e = GetExpression(TOP_PRIORITY);
            ExpectToken(")");

            //FIXME: add check for constant expression
            patch1 = gameLocal.program.NumStatements();
            EmitOpcode(OP_IFNOT, e, null);

            ParseStatement();

            if (CheckToken("else")) {
                patch2 = gameLocal.program.NumStatements();
                EmitOpcode(OP_GOTO, null, null);
                gameLocal.program.GetStatement(patch1).b = JumpFrom(patch1);
                ParseStatement();
                gameLocal.program.GetStatement(patch2).a = JumpFrom(patch2);
            } else {
                gameLocal.program.GetStatement(patch1).b = JumpFrom(patch1);
            }
        }

        private void ParseStatement() {
            if (CheckToken(";")) {
                // skip semicolons, which are harmless and ok syntax
                return;
            }

            if (CheckToken("{")) {
                do {
                    ParseStatement();
                } while (!CheckToken("}"));

                return;
            }

            if (CheckToken("return")) {
                ParseReturnStatement();
                return;
            }

            if (CheckToken("while")) {
                ParseWhileStatement();
                return;
            }

            if (CheckToken("for")) {
                ParseForStatement();
                return;
            }

            if (CheckToken("do")) {
                ParseDoWhileStatement();
                return;
            }

            if (CheckToken("break")) {
                ExpectToken(";");
                if (0 == loopDepth) {
                    Error("cannot break outside of a loop");
                }
                EmitOpcode(OP_BREAK, null, null);
                return;
            }

            if (CheckToken("continue")) {
                ExpectToken(";");
                if (0 == loopDepth) {
                    Error("cannot contine outside of a loop");
                }
                EmitOpcode(OP_CONTINUE, null, null);
                return;
            }

            if (CheckType() != null) {
                ParseDefs();
                return;
            }

            if (CheckToken("if")) {
                ParseIfStatement();
                return;
            }

            GetExpression(TOP_PRIORITY);
            ExpectToken(";");
        }

        private void ParseObjectDef(final String objname) {
            idTypeDef objtype;
            idTypeDef type;
            idTypeDef parentType;
            idTypeDef fieldtype;
            idStr name = new idStr();
            String fieldname;
            idTypeDef newtype = new idTypeDef(ev_field, null, "", 0, null);
            idVarDef oldscope;
            int num;
            int i;

            oldscope = scope;
            if (scope.Type() != ev_namespace) {
                Error("Objects cannot be defined within functions or other objects");
            }

            // make sure it doesn't exist before we create it
            if (gameLocal.program.FindType(objname) != null) {
                Error("'%s' : redefinition; different basic types", objname);
            }

            // base type
            if (!CheckToken(":")) {
                parentType = type_object;
            } else {
                parentType = ParseType();
                if (!parentType.Inherits(type_object)) {
                    Error("Objects may only inherit from objects.");
                }
            }

            objtype = gameLocal.program.AllocType(ev_object, null, objname, parentType == type_object ? 0 : parentType.Size(), parentType);
            objtype.def = gameLocal.program.AllocDef(objtype, objname, scope, true);
            scope = objtype.def;

            // inherit all the functions
            num = parentType.NumFunctions();
            for (i = 0; i < parentType.NumFunctions(); i++) {
                final function_t func = parentType.GetFunction(i);
                objtype.AddFunction(func);
            }

            ExpectToken("{");

            do {
                if (CheckToken(";")) {
                    // skip semicolons, which are harmless and ok syntax
                    continue;
                }

                fieldtype = ParseType();
                newtype.SetFieldType(fieldtype);

                fieldname = va("%s field", fieldtype.Name());
                newtype.SetName(fieldname);

                ParseName(name);

                // check for a function prototype or declaraction
                if (CheckToken("(")) {
                    ParseFunctionDef(newtype.FieldType(), name.toString());
                } else {
                    type = gameLocal.program.GetType(newtype, true);
                    assert (NOT(type.def));
                    gameLocal.program.AllocDef(type, name.toString(), scope, true);
                    objtype.AddField(type, name.toString());
                    ExpectToken(";");
                }
            } while (!CheckToken("}"));

            scope = oldscope;

            ExpectToken(";");
        }

        /*
         ============
         idCompiler::ParseFunction

         parse a function type
         ============
         */
        private idTypeDef ParseFunction(idTypeDef returnType, final String name) {
            idTypeDef newtype = new idTypeDef(ev_function, null, name, type_function.Size(), returnType);
            idTypeDef type;

            if (scope.Type() != ev_namespace) {
                // create self pointer
                newtype.AddFunctionParm(scope.TypeDef(), "self");
            }

            if (!CheckToken(")")) {
                idStr parmName = new idStr();
                do {
                    type = ParseType();
                    ParseName(parmName);
                    newtype.AddFunctionParm(type, parmName.toString());
                } while (CheckToken(","));

                ExpectToken(")");
            }

            return gameLocal.program.GetType(newtype, true);
        }

        private void ParseFunctionDef(idTypeDef returnType, final String name) {
            idTypeDef type;
            idVarDef def;
            idVarDef parm;
            idVarDef oldscope;
            int i;
            int numParms;
            idTypeDef parmType;
            function_t func;
            statement_s pos;

            if ((scope.Type() != ev_namespace) && !scope.TypeDef().Inherits(type_object)) {
                Error("Functions may not be defined within other functions");
            }

            type = ParseFunction(returnType, name);
            def = gameLocal.program.GetDef(type, name, scope);
            if (NOT(def)) {
                def = gameLocal.program.AllocDef(type, name, scope, true);
                type.def = def;

                func = gameLocal.program.AllocFunction(def);
                if (scope.TypeDef().Inherits(type_object)) {
                    scope.TypeDef().AddFunction(func);
                }
            } else {
                func = def.value.getFunctionPtr();
                assert (func != null);
                if (func.firstStatement != 0) {
                    Error("%s redeclared", def.GlobalName());
                }
            }

            // check if this is a prototype or declaration
            if (!CheckToken("{")) {
                // it's just a prototype, so get the ; and move on
                ExpectToken(";");
                return;
            }

            // calculate stack space used by parms
            numParms = type.NumParameters();
            func.parmSize.SetNum(numParms);
            for (i = 0; i < numParms; i++) {
                parmType = type.GetParmType(i);
                if (parmType.Inherits(type_object)) {
                    func.parmSize.oSet(i, type_object.Size());
                } else {
                    func.parmSize.oSet(i, parmType.Size());
                }
                func.parmTotal += func.parmSize.oGet(i);
            }

            // define the parms
            for (i = 0; i < numParms; i++) {
                if (gameLocal.program.GetDef(type.GetParmType(i), type.GetParmName(i), def) != null) {
                    Error("'%s' defined more than once in function parameters", type.GetParmName(i));
                }
                parm = gameLocal.program.AllocDef(type.GetParmType(i), type.GetParmName(i), def, false);
            }

            oldscope = scope;
            scope = def;

            func.firstStatement = gameLocal.program.NumStatements();

            // check if we should call the super class constructor
            if (oldscope.TypeDef().Inherits(type_object) && NOT(idStr.Icmp(name, "init"))) {
                idTypeDef superClass;
                function_t constructorFunc = null;

                // find the superclass constructor
                for (superClass = oldscope.TypeDef().SuperClass(); superClass != type_object; superClass = superClass.SuperClass()) {
                    constructorFunc = gameLocal.program.FindFunction(va("%s::init", superClass.Name()));
                    if (constructorFunc != null) {
                        break;
                    }
                }

                // emit the call to the constructor
                if (constructorFunc != null) {
                    idVarDef selfDef = gameLocal.program.GetDef(type.GetParmType(0), type.GetParmName(0), def);
                    assert (selfDef != null);
                    EmitPush(selfDef, selfDef.TypeDef());
                    EmitOpcode(opcodes[ OP_CALL], constructorFunc.def, null);
                }
            }

            // parse regular statements
            while (!CheckToken("}")) {
                ParseStatement();
            }

            // check if we should call the super class destructor
            if (oldscope.TypeDef().Inherits(type_object) && NOT(idStr.Icmp(name, "destroy"))) {
                idTypeDef superClass;
                function_t destructorFunc = null;

                // find the superclass destructor
                for (superClass = oldscope.TypeDef().SuperClass(); superClass != type_object; superClass = superClass.SuperClass()) {
                    destructorFunc = gameLocal.program.FindFunction(va("%s::destroy", superClass.Name()));
                    if (destructorFunc != null) {
                        break;
                    }
                }

                if (destructorFunc != null) {
                    if (func.firstStatement < gameLocal.program.NumStatements()) {
                        // change all returns to point to the call to the destructor
                        pos = gameLocal.program.GetStatement(func.firstStatement);
                        for (i = func.firstStatement; i < gameLocal.program.NumStatements(); pos = gameLocal.program.GetStatement(++i)) {
                            if (pos.op == OP_RETURN) {
                                pos.op = OP_GOTO;
                                pos.a = JumpDef(i, gameLocal.program.NumStatements());
                            }
                        }
                    }

                    // emit the call to the destructor
                    idVarDef selfDef = gameLocal.program.GetDef(type.GetParmType(0), type.GetParmName(0), def);
                    assert (selfDef != null);
                    EmitPush(selfDef, selfDef.TypeDef());
                    EmitOpcode(opcodes[OP_CALL], destructorFunc.def, null);
                }
            }

// Disabled code since it caused a function to fall through to the next function when last statement is in the form "if ( x ) { return; }"
// #if 0
            // // don't bother adding a return opcode if the "return" statement was used.
            // if ( ( func.firstStatement == gameLocal.program.NumStatements() ) || ( gameLocal.program.GetStatement( gameLocal.program.NumStatements() - 1 ).op != OP_RETURN ) ) {
            // // emit an end of statements opcode
            // EmitOpcode( OP_RETURN, 0, 0 );
            // }
// #else
            // always emit the return opcode
            EmitOpcode(OP_RETURN, null, null);
// #endif

            // record the number of statements in the function
            func.numStatements = gameLocal.program.NumStatements() - func.firstStatement;

            scope = oldscope;
        }

        private void ParseVariableDef(idTypeDef type, final String name) {
            idVarDef def, def2;
            boolean negate;

            def = gameLocal.program.GetDef(type, name, scope);
            if (def != null) {
                Error("%s redeclared", name);
            }

            def = gameLocal.program.AllocDef(type, name, scope, false);

            // check for an initialization
            if (CheckToken("=")) {
                // if a local variable in a function then write out interpreter code to initialize variable
                if (scope.Type() == ev_function) {
                    def2 = GetExpression(TOP_PRIORITY);
                    if ((type == type_float) && (def2.TypeDef() == type_float)) {
                        EmitOpcode(OP_STORE_F, def2, def);
                    } else if ((type == type_vector) && (def2.TypeDef() == type_vector)) {
                        EmitOpcode(OP_STORE_V, def2, def);
                    } else if ((type == type_string) && (def2.TypeDef() == type_string)) {
                        EmitOpcode(OP_STORE_S, def2, def);
                    } else if ((type == type_entity) && ((def2.TypeDef() == type_entity) || (def2.TypeDef().Inherits(type_object)))) {
                        EmitOpcode(OP_STORE_ENT, def2, def);
                    } else if ((type.Inherits(type_object)) && (def2.TypeDef() == type_entity)) {
                        EmitOpcode(OP_STORE_OBJENT, def2, def);
                    } else if ((type.Inherits(type_object)) && (def2.TypeDef().Inherits(type))) {
                        EmitOpcode(OP_STORE_OBJ, def2, def);
                    } else if ((type == type_boolean) && (def2.TypeDef() == type_boolean)) {
                        EmitOpcode(OP_STORE_BOOL, def2, def);
                    } else if ((type == type_string) && (def2.TypeDef() == type_float)) {
                        EmitOpcode(OP_STORE_FTOS, def2, def);
                    } else if ((type == type_string) && (def2.TypeDef() == type_boolean)) {
                        EmitOpcode(OP_STORE_BTOS, def2, def);
                    } else if ((type == type_string) && (def2.TypeDef() == type_vector)) {
                        EmitOpcode(OP_STORE_VTOS, def2, def);
                    } else if ((type == type_boolean) && (def2.TypeDef() == type_float)) {
                        EmitOpcode(OP_STORE_FTOBOOL, def2, def);
                    } else if ((type == type_float) && (def2.TypeDef() == type_boolean)) {
                        EmitOpcode(OP_STORE_BOOLTOF, def2, def);
                    } else {
                        Error("bad initialization for '%s'", name);
                    }
                } else {
                    // global variables can only be initialized with immediate values
                    negate = false;
                    if (token.type == TT_PUNCTUATION && token.equals("-")) {
                        negate = true;
                        NextToken();
                        if (immediateType != type_float) {
                            Error("wrong immediate type for '-' on variable '%s'", name);
                        }
                    }

                    if (immediateType != type) {
                        Error("wrong immediate type for '%s'", name);
                    }

                    // global variables are initialized at start up
                    if (type == type_string) {
                        def.SetString(token.toString(), false);
                    } else {
                        if (negate) {
                            immediate._float(-immediate._float());
                        }
                        def.SetValue(immediate, false);
                    }
                    NextToken();
                }
            } else if (type == type_string) {
                // local strings on the stack are initialized in the interpreter
                if (scope.Type() != ev_function) {
                    def.SetString("", false);
                }
            } else if (type.Inherits(type_object)) {
                if (scope.Type() != ev_function) {
                    def.SetObject(null);
                }
            }
        }

        private void ParseEventDef(idTypeDef returnType, final String name) {
            idTypeDef expectedType;
            idTypeDef argType;
            idTypeDef type;
            int i;
            int num;
            String format;
            idEventDef ev;
            idStr parmName = new idStr();

            ev = idEventDef.FindEvent(name);
            if (null == ev) {
                Error("Unknown event '%s'", name);
            }

            // set the return type
            expectedType = GetTypeForEventArg(ev.GetReturnType());
            if (NOT(expectedType)) {
                Error("Invalid return type '%c' in definition of '%s' event.", ev.GetReturnType(), name);
            }
            if (returnType != expectedType) {
                Error("Return type doesn't match internal return type '%s'", expectedType.Name());
            }

            idTypeDef newtype = new idTypeDef(ev_function, null, name, type_function.Size(), returnType);

            ExpectToken("(");

            format = ev.GetArgFormat();
            num = format.length();
            for (i = 0; i < num; i++) {
                expectedType = GetTypeForEventArg(format.charAt(i));
                if (null == expectedType || (expectedType == type_void)) {
                    Error("Invalid parameter '%c' in definition of '%s' event.", format.charAt(i), name);
                }

                argType = ParseType();
                ParseName(parmName);
                if (argType != expectedType) {
                    Error("The type of parm %d ('%s') does not match the internal type '%s' in definition of '%s' event.",
                            i + 1, parmName, expectedType.Name(), name);
                }

                newtype.AddFunctionParm(argType, "");

                if (i < num - 1) {
                    if (CheckToken(")")) {
                        Error("Too few parameters for event definition.  Internal definition has %d parameters.", num);
                    }
                    ExpectToken(",");
                }
            }
            if (!CheckToken(")")) {
                Error("Too many parameters for event definition.  Internal definition has %d parameters.", num);
            }
            ExpectToken(";");

            type = gameLocal.program.FindType(name);
            if (type != null) {
                if (!newtype.MatchesType(type) || (type.def.value.getFunctionPtr().eventdef != ev)) {
                    Error("Type mismatch on redefinition of '%s'", name);
                }
            } else {
                type = gameLocal.program.AllocType(newtype);
                type.def = gameLocal.program.AllocDef(type, name, def_namespace, true);

                function_t func = gameLocal.program.AllocFunction(type.def);
                func.eventdef = ev;
                func.parmSize.SetNum(num);
                for (i = 0; i < num; i++) {
                    argType = newtype.GetParmType(i);
                    func.parmTotal += argType.Size();
                    func.parmSize.oSet(i, argType.Size());
                }

                // mark the parms as local
                func.locals = func.parmTotal;
            }
        }

        /*
         ================
         idCompiler::ParseDefs

         Called at the outer layer and when a local statement is hit
         ================
         */
        private void ParseDefs() {
            idStr name = new idStr();
            idTypeDef type;
            idVarDef def;
            idVarDef oldscope;

            if (CheckToken(";")) {
                // skip semicolons, which are harmless and ok syntax
                return;
            }

            type = ParseType();
            if (type == type_scriptevent) {
                type = ParseType();
                ParseName(name);
                ParseEventDef(type, name.toString());
                return;
            }

            ParseName(name);

            if (type == type_namespace) {
                def = gameLocal.program.GetDef(type, name.toString(), scope);
                if (NOT(def)) {
                    def = gameLocal.program.AllocDef(type, name.toString(), scope, true);
                }
                ParseNamespace(def);
            } else if (CheckToken("::")) {
                def = gameLocal.program.GetDef(null, name.toString(), scope);
                if (NOT(def)) {
                    Error("Unknown object name '%s'", name);
                }
                ParseName(name);
                oldscope = scope;
                scope = def;

                ExpectToken("(");
                ParseFunctionDef(type, name.toString());
                scope = oldscope;
            } else if (type == type_object) {
                ParseObjectDef(name.toString());
            } else if (CheckToken("(")) {		// check for a function prototype or declaraction
                ParseFunctionDef(type, name.toString());
            } else {
                ParseVariableDef(type, name.toString());
                while (CheckToken(",")) {
                    ParseName(name);
                    ParseVariableDef(type, name.toString());
                }
                ExpectToken(";");
            }
        }

        /*
         ================
         idCompiler::ParseNamespace

         Parses anything within a namespace definition
         ================
         */
        private void ParseNamespace(idVarDef newScope) {
            idVarDef oldscope;

            oldscope = scope;
            if (newScope != def_namespace) {
                ExpectToken("{");
            }

            while (!eof) {
                scope = newScope;
                callthread = false;

                if ((newScope != def_namespace) && CheckToken("}")) {
                    break;
                }

                ParseDefs();
            }

            scope = oldscope;
        }

        public idCompiler() {
            int ptr;
            int id;

            // make sure we have the right # of opcodes in the table
//	assert( ( sizeof( opcodes ) / sizeof( opcodes[ 0 ] ) ) == ( NUM_OPCODES + 1 ) );
            assert (opcodes.length == (NUM_OPCODES + 1));

            eof = true;
            parserPtr = parser;

            callthread = false;
            loopDepth = 0;
            eof = false;
            braceDepth = 0;
            immediateType = null;
            basetype = null;
            currentLineNumber = 0;
            currentFileNumber = 0;
            errorCount = 0;
            console = false;
            scope = def_namespace;

//	memset( &immediate, 0, sizeof( immediate ) );
            immediate = new eval_s();
//	memset( punctuationValid, 0, sizeof( punctuationValid ) );
            punctuationValid = new boolean[punctuationValid.length];
            for (ptr = 0; punctuation[ptr] != null; ptr++) {
                id = parserPtr.GetPunctuationId(punctuation[ptr]);
                if ((id >= 0) && (id < 256)) {
                    punctuationValid[ id] = true;
                }
            }
        }

        /*
         ============
         idCompiler::CompileFile

         compiles the 0 terminated text, adding definitions to the program structure
         ============
         */
        public void CompileFile(final String text, final String filename, boolean toConsole) {
            idTimer compile_time = new idTimer();
            boolean error;

            compile_time.Start();

            scope = def_namespace;
            basetype = null;
            callthread = false;
            loopDepth = 0;
            eof = false;
            braceDepth = 0;
            immediateType = null;
            currentLineNumber = 0;
            console = toConsole;

//	memset( &immediate, 0, sizeof( immediate ) );
            immediate = new eval_s();

            parser.SetFlags(LEXFL_ALLOWMULTICHARLITERALS);
            parser.LoadMemory(text, text.length(), filename);
            parserPtr = parser;

            // unread tokens to include script defines
            token.oSet(SCRIPT_DEFAULTDEFS);
            token.type = TT_STRING;
            token.subtype = token.Length();
            token.line = token.linesCrossed = 0;
            parser.UnreadToken(token);

            token.oSet("include");
            token.type = TT_NAME;
            token.subtype = token.Length();
            token.line = token.linesCrossed = 0;
            parser.UnreadToken(token);

            token.oSet("#");
            token.type = TT_PUNCTUATION;
            token.subtype = P_PRECOMP;
            token.line = token.linesCrossed = 0;
            parser.UnreadToken(token);

            // init the current token line to be the first line so that currentLineNumber is set correctly in NextToken
            token.line = 1;

            error = false;
            try {
                // read first token
                NextToken();
                while (!eof && !error) {
                    // parse from global namespace
                    ParseNamespace(def_namespace);
                }
            } catch (idCompileError err) {
                String error2;

                if (console) {
                    // don't print line number of an error if were calling script from the console using the "script" command
                    error2 = String.format("Error: %s\n", err.error);
                } else {
                    error2 = String.format("Error: file %s, line %d: %s\n", gameLocal.program.GetFilename(currentFileNumber), currentLineNumber, err.error);
                }

                parser.FreeSource();

                throw new idCompileError(error2);
            }

            parser.FreeSource();

            compile_time.Stop();
            if (!toConsole) {
                gameLocal.Printf("Compiled '%s': %.1f ms\n", filename, compile_time.Milliseconds());
            }
        }
    };
}
