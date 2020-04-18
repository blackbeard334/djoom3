/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.Game.Script;

import static neo.Game.GameSys.SysCvar.g_disasm;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Script.Script_Compiler.OP_RETURN;
import static neo.Game.Script.Script_Compiler.RESULT_STRING;
import static neo.Game.Script.Script_Program.MAX_FUNCS;
import static neo.Game.Script.Script_Program.MAX_GLOBALS;
import static neo.Game.Script.Script_Program.MAX_STATEMENTS;
import static neo.Game.Script.Script_Program.def_namespace;
import static neo.Game.Script.Script_Program.def_object;
import static neo.Game.Script.Script_Program.ev_field;
import static neo.Game.Script.Script_Program.ev_function;
import static neo.Game.Script.Script_Program.ev_namespace;
import static neo.Game.Script.Script_Program.ev_vector;
import static neo.Game.Script.Script_Program.type_entity;
import static neo.Game.Script.Script_Program.type_float;
import static neo.Game.Script.Script_Program.type_object;
import static neo.Game.Script.Script_Program.type_string;
import static neo.Game.Script.Script_Program.type_vector;
import static neo.Game.Script.Script_Program.type_void;
import static neo.Game.Script.Script_Program.idVarDef.initialized_t.stackVariable;
import static neo.TempDump.indexOf;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.FileSystem_h.fsMode_t.FS_WRITE;
import static neo.idlib.Lib.idLib.fileSystem;
import static neo.idlib.Text.Str.va;
import static neo.idlib.hashing.MD4.MD4_BlockChecksum;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Script.Script_Compiler.idCompiler;
import neo.Game.Script.Script_Compiler.opcode_s;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idCompileError;
import neo.Game.Script.Script_Program.idTypeDef;
import neo.Game.Script.Script_Program.idVarDef;
import neo.Game.Script.Script_Program.idVarDefName;
import neo.Game.Script.Script_Program.statement_s;
import neo.Game.Script.Script_Program.varEval_s;
import neo.Game.Script.Script_Thread.idThread;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StaticList.idStaticList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Vector.idVec3;
import neo.open.Nio;

/* **********************************************************************

 idProgram

 Handles compiling and storage of script data.  Multiple idProgram objects
 would represent seperate programs with no knowledge of each other.  Scripts
 meant to access shared data and functions should all be compiled by a
 single idProgram.

 ***********************************************************************/
public final class idProgram {
    public static final int BYTES = Integer.BYTES * 20;//TODO:

    private final idStrList fileList = new idStrList();
    private final idStr     filename = new idStr();
    private int filenum;
    //
    private int numVariables;
    private byte[]                    variables        = new byte[MAX_GLOBALS];
    private final idStaticList<Byte>        variableDefaults = new idStaticList<>(MAX_GLOBALS);
    private final idStaticList<function_t>  functions        = new idStaticList<>(MAX_FUNCS, function_t.class);
    private final idStaticList<statement_s> statements       = new idStaticList<>(MAX_STATEMENTS, statement_s.class);
    private final idList<idTypeDef>         types            = new idList<>();
    private final idList<idVarDefName>      varDefNames      = new idList<>();
    private final idHashIndex               varDefNameHash   = new idHashIndex();
    private final idList<idVarDef>          varDefs          = new idList<>();
    //
    private idVarDef sysDef;
    //
    private int      top_functions;
    private int      top_statements;
    private int      top_types;
    private int      top_defs;
    private int      top_files;
    //
    public  idVarDef returnDef;
    public  idVarDef returnStringDef;
//
//

    /*
     ==============
     idProgram::CompileStats

     called after all files are compiled to report memory usage.
     ==============
     */
    private void CompileStats() {
        int memused;
        int memallocated;
        int numdefs;
        int stringspace;
        int funcMem;
        int i;

        gameLocal.Printf("---------- Compile stats ----------\n");
        gameLocal.DPrintf("Files loaded:\n");

        stringspace = 0;
        for (i = 0; i < this.fileList.Num(); i++) {
            gameLocal.DPrintf("   %s\n", this.fileList.oGet(i));
            stringspace += this.fileList.oGet(i).Allocated();
        }
        stringspace += this.fileList.Size();

        numdefs = this.varDefs.Num();
        memused = this.varDefs.Num() * idVarDef.BYTES;
        memused += this.types.Num() * idTypeDef.BYTES;
        memused += stringspace;

        for (i = 0; i < this.types.Num(); i++) {
            memused += this.types.oGet(i).Allocated();
        }

        funcMem = this.functions.MemoryUsed();
        for (i = 0; i < this.functions.Num(); i++) {
            funcMem += this.functions.oGet(i).Allocated();
        }

        memallocated = funcMem + memused + idProgram.BYTES;

        memused += this.statements.MemoryUsed();
        memused += this.functions.MemoryUsed();    // name and filename of functions are shared, so no need to include them
        memused += this.variables.length;

        gameLocal.Printf("\nMemory usage:\n");
        gameLocal.Printf("     Strings: %d, %d bytes\n", this.fileList.Num(), stringspace);
        gameLocal.Printf("  Statements: %d, %d bytes\n", this.statements.Num(), this.statements.MemoryUsed());
        gameLocal.Printf("   Functions: %d, %d bytes\n", this.functions.Num(), funcMem);
        gameLocal.Printf("   Variables: %d bytes\n", this.numVariables);
        gameLocal.Printf("    Mem used: %d bytes\n", memused);
        gameLocal.Printf(" Static data: %d bytes\n", idProgram.BYTES);
        gameLocal.Printf("   Allocated: %d bytes\n", memallocated);
        gameLocal.Printf(" Thread size: %d bytes\n\n", idThread.BYTES);
    }

    public idProgram() {
        FreeData();
    }
    // ~idProgram();

    // save games
    public void Save(idSaveGame savefile) {
        int i;
        int currentFileNum = this.top_files;

        savefile.WriteInt((this.fileList.Num() - currentFileNum));
        while (currentFileNum < this.fileList.Num()) {
            savefile.WriteString(this.fileList.oGet(currentFileNum));
            currentFileNum++;
        }

        for (i = 0; i < this.variableDefaults.Num(); i++) {
            if (this.variables[i] != this.variableDefaults.oGet(i)) {
                savefile.WriteInt(i);
                savefile.WriteByte(this.variables[i]);
            }
        }
        // Mark the end of the diff with default variables with -1
        savefile.WriteInt(-1);

        savefile.WriteInt(this.numVariables);
        for (i = this.variableDefaults.Num(); i < this.numVariables; i++) {
            savefile.WriteByte(this.variables[i]);
        }

        final int checksum = CalculateChecksum();
        savefile.WriteInt(checksum);
    }

    public boolean Restore(idRestoreGame savefile) {
        int i;
        final int[] num = {0}, index = {0};
        boolean result = true;
        final idStr scriptname = new idStr();

        savefile.ReadInt(num);
        for (i = 0; i < num[0]; i++) {
            savefile.ReadString(scriptname);
            CompileFile(scriptname.getData());
        }

        savefile.ReadInt(index);
        while (index[0] >= 0) {
            this.variables[index[0]] = savefile.ReadByte();
            savefile.ReadInt(index);
        }

        savefile.ReadInt(num);
        for (i = this.variableDefaults.Num(); i < num[0]; i++) {
            this.variables[i] = savefile.ReadByte();
        }

        final int[] saved_checksum = {0};
        int checksum;

        savefile.ReadInt(saved_checksum);
        checksum = CalculateChecksum();

        if (saved_checksum[0] != checksum) {
            result = false;
        }

        return result;
    }

    // Used to insure program code has not
    public int CalculateChecksum() {
        int i, result;

        class statementBlock_t {

            int/*unsigned short*/ op;
            int a;
            int b;
            int c;
            int lineNumber;
            int file;

            int[] toArray() {
                return new int[]{this.op, this.a, this.b, this.c, this.lineNumber, this.file};
            }
        }

        final statementBlock_t[] statementList = new statementBlock_t[this.statements.Num()];
        final int[] statementIntArray = new int[this.statements.Num() * 6];

//	memset( statementList, 0, ( sizeof(statementBlock_t) * statements.Num() ) );
        // Copy info into new list, using the variable numbers instead of a pointer to the variable
        for (i = 0; i < this.statements.Num(); i++) {
            statementList[i] = new statementBlock_t();
            statementList[i].op = this.statements.oGet(i).op;

            if (this.statements.oGet(i).a != null) {
                statementList[i].a = this.statements.oGet(i).a.num;
            } else {
                statementList[i].a = -1;
            }
            if (this.statements.oGet(i).b != null) {
                statementList[i].b = this.statements.oGet(i).b.num;
            } else {
                statementList[i].b = -1;
            }
            if (this.statements.oGet(i).c != null) {
                statementList[i].c = this.statements.oGet(i).c.num;
            } else {
                statementList[i].c = -1;
            }

            statementList[i].lineNumber = this.statements.oGet(i).linenumber;
            statementList[i].file = this.statements.oGet(i).file;

            Nio.arraycopy(statementList[i].toArray(), 0, statementIntArray, i * 6, 6);
        }

        result = new BigInteger(MD4_BlockChecksum(statementIntArray, /*sizeof(statementBlock_t)*/ this.statements.Num())).intValue();

//	delete [] statementList;
        return result;
    }

    //    changed between savegames
    public void Startup(final String defaultScript) {
        gameLocal.Printf("Initializing scripts\n");

        // make sure all data is freed up
        idThread.Restart();

        // get ready for loading scripts
        BeginCompilation();

        // load the default script
        if (isNotNullOrEmpty(defaultScript)) {
            CompileFile(defaultScript);
        }

        FinishCompilation();
    }

    /*
     ==============
     idProgram::Restart

     Restores all variables to their initial value
     ==============
     */
    public void Restart() {
        int i;

        idThread.Restart();

        //
        // since there may have been a script loaded by the map or the user may
        // have typed "script" from the console, free up any types and vardefs that
        // have been allocated after the initial startup
        //
//	for( i = top_types; i < types.Num(); i++ ) {
//		delete types[ i ];
//	}
        this.types.SetNum(this.top_types, false);

//	for( i = top_defs; i < varDefs.Num(); i++ ) {
//		delete varDefs[ i ];
//	}
        this.varDefs.SetNum(this.top_defs, false);

        for (i = this.top_functions; i < this.functions.Num(); i++) {
            this.functions.oGet(i).Clear();
        }
        this.functions.SetNum(this.top_functions);

        this.statements.SetNum(this.top_statements);
        this.fileList.SetNum(this.top_files, false);
        this.filename.Clear();

        // reset the variables to their default values
        this.numVariables = this.variableDefaults.Num();
        for (i = 0; i < this.numVariables; i++) {
            this.variables[ i] = this.variableDefaults.oGet(i);
        }
    }

    public boolean CompileText(final String source, final String text, boolean console) {
        final idCompiler compiler = new idCompiler();
        int i;
        idVarDef def;
        String ospath;

        // use a full os path for GetFilenum since it calls OSPathToRelativePath to convert filenames from the parser
        ospath = fileSystem.RelativePathToOSPath(source);
        this.filenum = GetFilenum(ospath);

        try {
            compiler.CompileFile(text, this.filename.getData(), console);

            // check to make sure all functions prototyped have code
            for (i = 0; i < this.varDefs.Num(); i++) {
                def = this.varDefs.oGet(i);
                if ((def.Type() == ev_function) && ((def.scope.Type() == ev_namespace) || def.scope.TypeDef().Inherits(type_object))) {
                    if ((null == def.value.functionPtr.eventdef) && (0 == def.value.functionPtr.firstStatement)) {
                        throw new idCompileError(va("function %s was not defined\n", def.GlobalName()));
                    }
                }
            }
        } catch (final idCompileError err) {
            if (console) {
                gameLocal.Printf("%s\n", err.error);
                return false;
            } else {
                idGameLocal.Error("%s\n", err.error);
            }
        }

        if (!console) {
            CompileStats();
        }

        return true;
    }

    public function_t CompileFunction(final String functionName, final String text) {
        boolean result;

        result = CompileText(functionName, text, false);

        if (g_disasm.GetBool()) {
            Disassemble();
        }

        if (!result) {
            idGameLocal.Error("Compile failed.");
        }

        return FindFunction(functionName);
    }

    public void CompileFile(final String filename) {
        final ByteBuffer[] src = {null};
        boolean result;

        if (fileSystem.ReadFile(filename, src, null) < 0) {
            idGameLocal.Error("Couldn't load %s\n", filename);
        }

        result = CompileText(filename, new String(src[0].array()), false);

        fileSystem.FreeFile(src);

        if (g_disasm.GetBool()) {
            Disassemble();
        }

        if (!result) {
            idGameLocal.Error("Compile failed in file %s.", filename);
        }
    }

    /*
     ==============
     idProgram::BeginCompilation

     called before compiling a batch of files, clears the pr struct
     ==============
     */
    public void BeginCompilation() {
        statement_s statement;

        FreeData();

        try {
            // make the first statement a return for a "NULL" function
            statement = AllocStatement();
            statement.linenumber = 0;
            statement.file = 0;
            statement.op = OP_RETURN;
            statement.a = null;
            statement.b = null;
            statement.c = null;

            // define NULL
            //AllocDef( &type_void, "<NULL>", &def_namespace, true );
            // define the return def
            this.returnDef = AllocDef(type_vector, "<RETURN>", def_namespace, false);

            // define the return def for strings
            this.returnStringDef = AllocDef(type_string, "<RETURN>", def_namespace, false);

            // define the sys object
            this.sysDef = AllocDef(type_void, "sys", def_namespace, true);
        } catch (final idCompileError err) {
            idGameLocal.Error("%s", err.error);
        }
    }

    /*
     ==============
     idProgram::FinishCompilation

     Called after all files are compiled to check for errors
     ==============
     */
    public void FinishCompilation() {
        int i;

        this.top_functions = this.functions.Num();
        this.top_statements = this.statements.Num();
        this.top_types = this.types.Num();
        this.top_defs = this.varDefs.Num();
        this.top_files = this.fileList.Num();

        this.variableDefaults.Clear();
        this.variableDefaults.SetNum(this.numVariables);

        for (i = 0; i < this.numVariables; i++) {
            this.variableDefaults.oSet(i, this.variables[i]);
        }
    }

    public void DisassembleStatement(idFile file, int instructionPointer) {
        opcode_s op;
        statement_s statement;

        statement = this.statements.oGet(instructionPointer);
        op = idCompiler.opcodes[ statement.op];
        file.Printf("%20s(%d):\t%6d: %15s\t", this.fileList.oGet(statement.file), statement.linenumber, instructionPointer, op.opname);

        if (statement.a != null) {
            file.Printf("\ta: ");
            statement.a.PrintInfo(file, instructionPointer);
        }

        if (statement.b != null) {
            file.Printf("\tb: ");
            statement.b.PrintInfo(file, instructionPointer);
        }

        if (statement.c != null) {
            file.Printf("\tc: ");
            statement.c.PrintInfo(file, instructionPointer);
        }

        file.Printf("\n");
    }

    public void Disassemble() {
        int i;
        int instructionPointer;
        function_t func;
        idFile file;

        file = fileSystem.OpenFileByMode("script/disasm.txt", FS_WRITE);

        for (i = 0; i < this.functions.Num(); i++) {
            func = this.functions.oGet(i);
            if (func.eventdef != null) {
                // skip eventdefs
                continue;
            }

            file.Printf("\nfunction %s() %d stack used, %d parms, %d locals {\n", func.Name(), func.locals, func.parmTotal, func.locals - func.parmTotal);

            for (instructionPointer = 0; instructionPointer < func.numStatements; instructionPointer++) {
                DisassembleStatement(file, func.firstStatement + instructionPointer);
            }

            file.Printf("}\n");
        }

        fileSystem.CloseFile(file);
    }

    public void FreeData() {
        int i;

        // free the defs
        this.varDefs.DeleteContents(true);
        this.varDefNames.DeleteContents(true);
        this.varDefNameHash.Free();

        this.returnDef = null;
        this.returnStringDef = null;
        this.sysDef = null;

        // free any special types we've created
        this.types.DeleteContents(true);

        this.filenum = 0;

        this.numVariables = 0;
//	memset( variables, 0, sizeof( variables ) );
        this.variables = new byte[this.variables.length];

        // clear all the strings in the functions so that it doesn't look like we're leaking memory.
        for (i = 0; i < this.functions.Num(); i++) {
            this.functions.oGet(i).Clear();
        }

        this.filename.Clear();
        this.fileList.Clear();
        this.statements.Clear();
        this.functions.Clear();

        this.top_functions = 0;
        this.top_statements = 0;
        this.top_types = 0;
        this.top_defs = 0;
        this.top_files = 0;

        this.filename.oSet("");
    }

    public String GetFilename(int num) {
        return this.fileList.oGet(num).getData();
    }

    public int GetFilenum(final String name) {
        if (this.filename.equals(name)) {
            return this.filenum;
        }

        String strippedName;
        strippedName = fileSystem.OSPathToRelativePath(name);
        if (isNotNullOrEmpty(strippedName)) {
            // not off the base path so just use the full path
            this.filenum = this.fileList.AddUnique(name);
        } else {
            this.filenum = this.fileList.AddUnique(strippedName);
        }

        // save the unstripped name so that we don't have to strip the incoming name every time we call GetFilenum
        this.filename.oSet(name);

        return this.filenum;
    }

    public int GetLineNumberForStatement(int index) {
        return this.statements.oGet(index).linenumber;
    }

    public String GetFilenameForStatement(int index) {
        return GetFilename(this.statements.oGet(index).file);
    }

    public idTypeDef AllocType(idTypeDef type) {
        idTypeDef newtype;

        newtype = new idTypeDef(type);
        this.types.Append(newtype);

        return newtype;
    }

    public idTypeDef AllocType(int/*etype_t*/ etype, idVarDef edef, final String ename, int esize, idTypeDef aux) {
        idTypeDef newtype;

        newtype = new idTypeDef(etype, edef, ename, esize, aux);
        this.types.Append(newtype);

        return newtype;
    }

    /*
     ============
     idProgram::GetType

     Returns a preexisting complex type that matches the parm, or allocates
     a new one and copies it out.
     ============
     */
    public idTypeDef GetType(idTypeDef type, boolean allocate) {
        int i;

        //FIXME: linear search == slow
        for (i = this.types.Num() - 1; i >= 0; i--) {
            if (this.types.oGet(i).MatchesType(type) && this.types.oGet(i).Name().equals(type.Name())) {
                return this.types.oGet(i);
            }
        }

        if (!allocate) {
            return null;
        }

        // allocate a new one
        return AllocType(type);
    }

    /*
     ============
     idProgram::FindType

     Returns a preexisting complex type that matches the name, or returns NULL if not found
     ============
     */
    public idTypeDef FindType(final String name) {
        idTypeDef check;
        int i;

        for (i = this.types.Num() - 1; i >= 0; i--) {
            check = this.types.oGet(i);
            if (check.Name().equals(name)) {
                return check;
            }
        }

        return null;
    }

    public idVarDef AllocDef(idTypeDef type, final String name, idVarDef scope, boolean constant) {
        idVarDef def;
        String element;
        idVarDef def_x;
        idVarDef def_y;
        idVarDef def_z;

        // allocate a new def
        def = new idVarDef(type);
        def.scope = scope;
        def.numUsers = 1;
        def.num = this.varDefs.Append(def);
        def.value = new varEval_s();

        // add the def to the list with defs with this name and set the name pointer
        AddDefToNameList(def, name);

        if ((type.Type() == ev_vector) || ((type.Type() == ev_field) && (type.FieldType().Type() == ev_vector))) {
            //
            // vector
            //
            if (RESULT_STRING.equals(name)) {
                // <RESULT> vector defs don't need the _x, _y and _z components
                assert (scope.Type() == ev_function);
                def.value.setStackOffset(scope.value.functionPtr.locals);
                def.initialized = stackVariable;
                scope.value.functionPtr.locals += type.Size();
            } else if (scope.TypeDef().Inherits(type_object)) {
                final idTypeDef newtype = new idTypeDef(ev_field, null, "float field", 0, type_float);
                final idTypeDef type2 = GetType(newtype, true);

                // set the value to the variable's position in the object
                def.value.setPtrOffset(scope.TypeDef().Size());

                // make automatic defs for the vectors elements
                // origin can be accessed as origin_x, origin_y, and origin_z
                element = String.format("%s_x", def.Name());
                def_x = AllocDef(type2, element, scope, constant);

                element = String.format("%s_y", def.Name());
                def_y = AllocDef(type2, element, scope, constant);
                def_y.value.setPtrOffset(def_x.value.getPtrOffset() + type_float.Size());

                element = String.format("%s_z", def.Name());
                def_z = AllocDef(type2, element, scope, constant);
                def_z.value.setPtrOffset(def_y.value.getPtrOffset() + type_float.Size());
            } else {
                // make automatic defs for the vectors elements
                // origin can be accessed as origin_x, origin_y, and origin_z
                element = String.format("%s_x", def.Name());
                def_x = AllocDef(type_float, element, scope, constant);

                element = String.format("%s_y", def.Name());
                def_y = AllocDef(type_float, element, scope, constant);

                element = String.format("%s_z", def.Name());
                def_z = AllocDef(type_float, element, scope, constant);

                // point the vector def to the x coordinate
                def.value = def_x.value;
                def.initialized = def_x.initialized;
            }
        } else if (scope.TypeDef().Inherits(type_object)) {
            //
            // object variable
            //
            // set the value to the variable's position in the object
            def.value.setPtrOffset(scope.TypeDef().Size());
        } else if (scope.Type() == ev_function) {
            //
            // stack variable
            //
            // since we don't know how many local variables there are,
            // we have to have them go backwards on the stack
            def.value.setStackOffset(scope.value.functionPtr.locals);
            def.initialized = stackVariable;

            if (type.Inherits(type_object)) {
                // objects only have their entity number on the stack, not the entire object
                scope.value.functionPtr.locals += type_object.Size();
            } else {
                scope.value.functionPtr.locals += type.Size();
            }
        } else {
            //
            // global variable
            //
            def.value.setBytePtr(this.variables, this.numVariables);
            this.numVariables += def.TypeDef().Size();
//            System.out.println(def.TypeDef().Name());
            if (this.numVariables > this.variables.length) {
                throw new idCompileError(va("Exceeded global memory size (%d bytes)", this.variables.length));
            }

            Arrays.fill(this.variables, this.numVariables, this.variables.length, (byte) 0);
//                memset(def.value.bytePtr, 0, def.TypeDef().Size());
        }

        return def;
    }

    /*
     ============
     idProgram::GetDef

     If type is NULL, it will match any type
     ============
     */
    public idVarDef GetDef(final idTypeDef type, final String name, final idVarDef scope) {
        idVarDef def;
        idVarDef bestDef;
        int bestDepth;
        int depth;

        bestDepth = 0;
        bestDef = null;
        for (def = GetDefList(name); def != null; def = def.Next()) {
            if (def.scope.Type() == ev_namespace) {
                depth = def.DepthOfScope(scope);
                if (0 == depth) {
                    // not in the same namespace
                    continue;
                }
            } else if (def.scope != scope) {
                // in a different function
                continue;
            } else {
                depth = 1;
            }

            if ((null == bestDef) || (depth < bestDepth)) {
                bestDepth = depth;
                bestDef = def;
            }
        }

        // see if the name is already in use for another type
        if ((bestDef != null) && (type != null) && (!bestDef.TypeDef().equals(type))) {
            throw new idCompileError(va("Type mismatch on redeclaration of %s", name));
        }

        return bestDef;
    }

    public void FreeDef(idVarDef def, final idVarDef scope) {
        idVarDef e;
        int i;

        if (def.Type() == ev_vector) {
            String name;

            name = String.format("%s_x", def.Name());
            e = GetDef(null, name, scope);
            if (e != null) {
                FreeDef(e, scope);
            }

            name = String.format("%s_y", def.Name());
            e = GetDef(null, name, scope);
            if (e != null) {
                FreeDef(e, scope);
            }

            name = String.format("%s_z", def.Name());
            e = GetDef(null, name, scope);
            if (e != null) {
                FreeDef(e, scope);
            }
        }

        this.varDefs.RemoveIndex(def.num);
        for (i = def.num; i < this.varDefs.Num(); i++) {
            this.varDefs.oGet(i).num = i;
        }

        def.close();
    }

    public idVarDef FindFreeResultDef(idTypeDef type, final String name, idVarDef scope, final idVarDef a, final idVarDef b) {
        idVarDef def;

        for (def = GetDefList(name); def != null; def = def.Next()) {
            if (def.equals(a) || def.equals(b)) {
                continue;
            }
            if (!def.TypeDef().equals(type)) {
                continue;
            }
            if (!def.scope.equals(scope)) {
                continue;
            }
            if (def.numUsers <= 1) {
                continue;
            }
            return def;
        }

        return AllocDef(type, name, scope, false);
    }

    public idVarDef GetDefList(final String name) {
        int i, hash;

        hash = this.varDefNameHash.GenerateKey(name, true);
        for (i = this.varDefNameHash.First(hash); i != -1; i = this.varDefNameHash.Next(i)) {
            if (idStr.Cmp(this.varDefNames.oGet(i).Name(), name) == 0) {
                return this.varDefNames.oGet(i).GetDefs();
            }
        }
        return null;
    }

    public void AddDefToNameList(idVarDef def, final String name) {
        int i, hash;

        hash = this.varDefNameHash.GenerateKey(name, true);
        for (i = this.varDefNameHash.First(hash); i != -1; i = this.varDefNameHash.Next(i)) {
            if (idStr.Cmp(this.varDefNames.oGet(i).Name(), name) == 0) {
                break;
            }
        }
        if (i == -1) {
            i = this.varDefNames.Append(new idVarDefName(name));
            this.varDefNameHash.Add(hash, i);
        }
        this.varDefNames.oGet(i).AddDef(def);
    }

    /*
     ================
     idProgram::FindFunction

     Searches for the specified function in the currently loaded script.  A full namespace should be
     specified if not in the global namespace.

     Returns 0 if function not found.
     Returns >0 if function found.
     ================
     */
    public function_t FindFunction(final String name) {				// returns NULL if function not found
        int start;
        int pos;
        idVarDef namespaceDef;
        idVarDef def;

        assert (name != null);

        final idStr fullname = new idStr(name);
        start = 0;
        namespaceDef = def_namespace;
        do {
            pos = fullname.Find("::", true, start);
            if (pos < 0) {
                break;
            }

            final String namespaceName = fullname.Mid(start, pos - start).getData();
            def = GetDef(null, namespaceName, namespaceDef);
            if (null == def) {
                // couldn't find namespace
                return null;
            }
            namespaceDef = def;

            // skip past the ::
            start = pos + 2;
        } while (def.Type() == ev_namespace);

        final String funcName = fullname.Right(fullname.Length() - start).getData();
        def = GetDef(null, funcName, namespaceDef);
        if (null == def) {
            // couldn't find function
            return null;
        }

        if ((def.Type() == ev_function) && (def.value.functionPtr.eventdef == null)) {
            return def.value.functionPtr;
        }

        // is not a function, or is an eventdef
        return null;
    }

    public function_t FindFunction(final idStr name) {
        return FindFunction(name.getData());
    }

    /*
     ================
     idProgram::FindFunction

     Searches for the specified object function in the currently loaded script.

     Returns 0 if function not found.
     Returns >0 if function found.
     ================
     */
    public function_t FindFunction(final String name, final idTypeDef type) {	// returns NULL if function not found
        idVarDef tdef;
        idVarDef def;

        // look for the function
//            def = null;
        for (tdef = type.def; tdef != def_object; tdef = tdef.TypeDef().SuperClass().def) {
            def = GetDef(null, name, tdef);
            if (def != null) {
                return def.value.functionPtr;
            }
        }

        return null;
    }

    public function_t AllocFunction(idVarDef def) {
        if (this.functions.Num() >= this.functions.Max()) {
            throw new idCompileError(va("Exceeded maximum allowed number of functions (%d)", this.functions.Max()));
        }

        // fill in the dfunction
        final function_t func = this.functions.Alloc();
        func.eventdef = null;
        func.def = def;
        func.type = def.TypeDef();
        func.firstStatement = 0;
        func.numStatements = 0;
        func.parmTotal = 0;
        func.locals = 0;
        func.filenum = this.filenum;
        func.parmSize.SetGranularity(1);
        func.SetName(def.GlobalName());

        def.SetFunction(func);

        return func;
    }

    public function_t GetFunction(int index) {
        return this.functions.oGet(index);
    }

    public int GetFunctionIndex(final function_t func) {
        return indexOf(func, this.functions.Ptr());
    }

    public void SetEntity(final String name, idEntity ent) {
        idVarDef def;
        String defName = "$";

        defName += name;

        def = GetDef(type_entity, defName, def_namespace);
        if ((def != null) && (def.initialized != stackVariable)) {
            // 0 is reserved for NULL entity
            if (null == ent) {
                def.value.setEntityNumberPtr(0);
            } else {
                def.value.setEntityNumberPtr(ent.entityNumber + 1);
            }
        }
    }

    public statement_s AllocStatement() {
        if (this.statements.Num() == 61960) {
            final int a = 0;
        }
        if (this.statements.Num() >= this.statements.Max()) {
            throw new idCompileError(va("Exceeded maximum allowed number of statements (%d)", this.statements.Max()));
        }
        return this.statements.Alloc();
    }

    public statement_s GetStatement(int index) {
        if (index == 61961) {
            final int a = 0;
        }
        return this.statements.oGet(index);
    }

    public int NumStatements() {
        return this.statements.Num();
    }

    public int GetReturnedInteger() {
        return this.returnDef.value.getIntPtr();
    }

    public void ReturnFloat(float value) {
        this.returnDef.value.setFloatPtr(value);
    }

    public void ReturnInteger(int value) {
        this.returnDef.value.setIntPtr(value);
    }

    public void ReturnVector(idVec3 vec) {
        this.returnDef.value.setVectorPtr(vec);
    }

    public void ReturnString(final String string) {
        this.returnStringDef.value.stringPtr = string;//idStr.Copynz(returnStringDef.value.stringPtr, string, MAX_STRING_LEN);
    }

    public void ReturnEntity(idEntity ent) {
        if (ent != null) {
            this.returnDef.value.setEntityNumberPtr(ent.entityNumber + 1);
        } else {
            this.returnDef.value.setEntityNumberPtr(0);
        }
    }

    public int NumFilenames() {
        return this.fileList.Num();
    }
}
