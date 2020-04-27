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

/* **********************************************************************

 idProgram

 Handles compiling and storage of script data.  Multiple idProgram objects
 would represent seperate programs with no knowledge of each other.  Scripts
 meant to access shared data and functions should all be compiled by a
 single idProgram.

 ***********************************************************************/
public final class idProgram {
    public static final int BYTES = Integer.BYTES * 20;//TODO:

    private idStrList fileList = new idStrList();
    private idStr     filename = new idStr();
    private int filenum;
    //
    private int numVariables;
    private byte[]                    variables        = new byte[MAX_GLOBALS];
    private idStaticList<Byte>        variableDefaults = new idStaticList<>(MAX_GLOBALS);
    private idStaticList<function_t>  functions        = new idStaticList<>(MAX_FUNCS, function_t.class);
    private idStaticList<statement_s> statements       = new idStaticList<>(MAX_STATEMENTS, statement_s.class);
    private idList<idTypeDef>         types            = new idList<>();
    private idList<idVarDefName>      varDefNames      = new idList<>();
    private idHashIndex               varDefNameHash   = new idHashIndex();
    private idList<idVarDef>          varDefs          = new idList<>();
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
        for (i = 0; i < fileList.Num(); i++) {
            gameLocal.DPrintf("   %s\n", fileList.oGet(i));
            stringspace += fileList.oGet(i).Allocated();
        }
        stringspace += fileList.Size();

        numdefs = varDefs.Num();
        memused = varDefs.Num() * idVarDef.BYTES;
        memused += types.Num() * idTypeDef.BYTES;
        memused += stringspace;

        for (i = 0; i < types.Num(); i++) {
            memused += types.oGet(i).Allocated();
        }

        funcMem = functions.MemoryUsed();
        for (i = 0; i < functions.Num(); i++) {
            funcMem += functions.oGet(i).Allocated();
        }

        memallocated = funcMem + memused + idProgram.BYTES;

        memused += statements.MemoryUsed();
        memused += functions.MemoryUsed();    // name and filename of functions are shared, so no need to include them
        memused += variables.length;

        gameLocal.Printf("\nMemory usage:\n");
        gameLocal.Printf("     Strings: %d, %d bytes\n", fileList.Num(), stringspace);
        gameLocal.Printf("  Statements: %d, %d bytes\n", statements.Num(), statements.MemoryUsed());
        gameLocal.Printf("   Functions: %d, %d bytes\n", functions.Num(), funcMem);
        gameLocal.Printf("   Variables: %d bytes\n", numVariables);
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
        int currentFileNum = top_files;

        savefile.WriteInt((fileList.Num() - currentFileNum));
        while (currentFileNum < fileList.Num()) {
            savefile.WriteString(fileList.oGet(currentFileNum));
            currentFileNum++;
        }

        for (i = 0; i < variableDefaults.Num(); i++) {
            if (variables[i] != variableDefaults.oGet(i)) {
                savefile.WriteInt(i);
                savefile.WriteByte(variables[i]);
            }
        }
        // Mark the end of the diff with default variables with -1
        savefile.WriteInt(-1);

        savefile.WriteInt(numVariables);
        for (i = variableDefaults.Num(); i < numVariables; i++) {
            savefile.WriteByte(variables[i]);
        }

        int checksum = CalculateChecksum();
        savefile.WriteInt(checksum);
    }

    public boolean Restore(idRestoreGame savefile) {
        int i;
        int[] num = {0}, index = {0};
        boolean result = true;
        idStr scriptname = new idStr();

        savefile.ReadInt(num);
        for (i = 0; i < num[0]; i++) {
            savefile.ReadString(scriptname);
            CompileFile(scriptname.toString());
        }

        savefile.ReadInt(index);
        while (index[0] >= 0) {
            variables[index[0]] = savefile.ReadByte();
            savefile.ReadInt(index);
        }

        savefile.ReadInt(num);
        for (i = variableDefaults.Num(); i < num[0]; i++) {
            variables[i] = savefile.ReadByte();
        }

        int[] saved_checksum = {0};
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
                return new int[]{op, a, b, c, lineNumber, file};
            }
        };

        statementBlock_t[] statementList = new statementBlock_t[statements.Num()];
        int[] statementIntArray = new int[statements.Num() * 6];

//	memset( statementList, 0, ( sizeof(statementBlock_t) * statements.Num() ) );
        // Copy info into new list, using the variable numbers instead of a pointer to the variable
        for (i = 0; i < statements.Num(); i++) {
            statementList[i] = new statementBlock_t();
            statementList[i].op = statements.oGet(i).op;

            if (statements.oGet(i).a != null) {
                statementList[i].a = statements.oGet(i).a.num;
            } else {
                statementList[i].a = -1;
            }
            if (statements.oGet(i).b != null) {
                statementList[i].b = statements.oGet(i).b.num;
            } else {
                statementList[i].b = -1;
            }
            if (statements.oGet(i).c != null) {
                statementList[i].c = statements.oGet(i).c.num;
            } else {
                statementList[i].c = -1;
            }

            statementList[i].lineNumber = statements.oGet(i).linenumber;
            statementList[i].file = statements.oGet(i).file;

            System.arraycopy(statementList[i].toArray(), 0, statementIntArray, i * 6, 6);
        }

        result = new BigInteger(MD4_BlockChecksum(statementIntArray, /*sizeof(statementBlock_t)*/ statements.Num())).intValue();

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
        types.SetNum(top_types, false);

//	for( i = top_defs; i < varDefs.Num(); i++ ) {
//		delete varDefs[ i ];
//	}
        varDefs.SetNum(top_defs, false);

        for (i = top_functions; i < functions.Num(); i++) {
            functions.oGet(i).Clear();
        }
        functions.SetNum(top_functions);

        statements.SetNum(top_statements);
        fileList.SetNum(top_files, false);
        filename.Clear();

        // reset the variables to their default values
        numVariables = variableDefaults.Num();
        for (i = 0; i < numVariables; i++) {
            variables[ i] = variableDefaults.oGet(i);
        }
    }

    public boolean CompileText(final String source, final String text, boolean console) {
        idCompiler compiler = new idCompiler();
        int i;
        idVarDef def;
        String ospath;

        // use a full os path for GetFilenum since it calls OSPathToRelativePath to convert filenames from the parser
        ospath = fileSystem.RelativePathToOSPath(source);
        filenum = GetFilenum(ospath);

        try {
            compiler.CompileFile(text, filename.toString(), console);

            // check to make sure all functions prototyped have code
            for (i = 0; i < varDefs.Num(); i++) {
                def = varDefs.oGet(i);
                if ((def.Type() == ev_function) && ((def.scope.Type() == ev_namespace) || def.scope.TypeDef().Inherits(type_object))) {
                    if (null == def.value.functionPtr.eventdef && 0 == def.value.functionPtr.firstStatement) {
                        throw new idCompileError(va("function %s was not defined\n", def.GlobalName()));
                    }
                }
            }
        } catch (idCompileError err) {
            if (console) {
                gameLocal.Printf("%s\n", err.error);
                return false;
            } else {
                gameLocal.Error("%s\n", err.error);
            }
        };

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
            gameLocal.Error("Compile failed.");
        }

        return FindFunction(functionName);
    }

    public void CompileFile(final String filename) {
        ByteBuffer[] src = {null};
        boolean result;

        if (fileSystem.ReadFile(filename, src, null) < 0) {
            gameLocal.Error("Couldn't load %s\n", filename);
        }

        result = CompileText(filename, new String(src[0].array()), false);

        fileSystem.FreeFile(src);

        if (g_disasm.GetBool()) {
            Disassemble();
        }

        if (!result) {
            gameLocal.Error("Compile failed in file %s.", filename);
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
            returnDef = AllocDef(type_vector, "<RETURN>", def_namespace, false);

            // define the return def for strings
            returnStringDef = AllocDef(type_string, "<RETURN>", def_namespace, false);

            // define the sys object
            sysDef = AllocDef(type_void, "sys", def_namespace, true);
        } catch (idCompileError err) {
            gameLocal.Error("%s", err.error);
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

        top_functions = functions.Num();
        top_statements = statements.Num();
        top_types = types.Num();
        top_defs = varDefs.Num();
        top_files = fileList.Num();

        variableDefaults.Clear();
        variableDefaults.SetNum(numVariables);

        for (i = 0; i < numVariables; i++) {
            variableDefaults.oSet(i, variables[i]);
        }
    }

    public void DisassembleStatement(idFile file, int instructionPointer) {
        opcode_s op;
        statement_s statement;

        statement = statements.oGet(instructionPointer);
        op = idCompiler.opcodes[ statement.op];
        file.Printf("%20s(%d):\t%6d: %15s\t", fileList.oGet(statement.file), statement.linenumber, instructionPointer, op.opname);

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

        for (i = 0; i < functions.Num(); i++) {
            func = functions.oGet(i);
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
        varDefs.DeleteContents(true);
        varDefNames.DeleteContents(true);
        varDefNameHash.Free();

        returnDef = null;
        returnStringDef = null;
        sysDef = null;

        // free any special types we've created
        types.DeleteContents(true);

        filenum = 0;

        numVariables = 0;
//	memset( variables, 0, sizeof( variables ) );
        variables = new byte[variables.length];

        // clear all the strings in the functions so that it doesn't look like we're leaking memory.
        for (i = 0; i < functions.Num(); i++) {
            functions.oGet(i).Clear();
        }

        filename.Clear();
        fileList.Clear();
        statements.Clear();
        functions.Clear();

        top_functions = 0;
        top_statements = 0;
        top_types = 0;
        top_defs = 0;
        top_files = 0;

        filename.oSet("");
    }

    public String GetFilename(int num) {
        return fileList.oGet(num).toString();
    }

    public int GetFilenum(final String name) {
        if (filename.equals(name)) {
            return filenum;
        }

        String strippedName;
        strippedName = fileSystem.OSPathToRelativePath(name);
        if (isNotNullOrEmpty(strippedName)) {
            // not off the base path so just use the full path
            filenum = fileList.AddUnique(name);
        } else {
            filenum = fileList.AddUnique(strippedName);
        }

        // save the unstripped name so that we don't have to strip the incoming name every time we call GetFilenum
        filename.oSet(name);

        return filenum;
    }

    public int GetLineNumberForStatement(int index) {
        return statements.oGet(index).linenumber;
    }

    public String GetFilenameForStatement(int index) {
        return GetFilename(statements.oGet(index).file);
    }

    public idTypeDef AllocType(idTypeDef type) {
        idTypeDef newtype;

        newtype = new idTypeDef(type);
        types.Append(newtype);

        return newtype;
    }

    public idTypeDef AllocType(int/*etype_t*/ etype, idVarDef edef, final String ename, int esize, idTypeDef aux) {
        idTypeDef newtype;

        newtype = new idTypeDef(etype, edef, ename, esize, aux);
        types.Append(newtype);

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
        for (i = types.Num() - 1; i >= 0; i--) {
            if (types.oGet(i).MatchesType(type) && types.oGet(i).Name().equals(type.Name())) {
                return types.oGet(i);
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

        for (i = types.Num() - 1; i >= 0; i--) {
            check = types.oGet(i);
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
        def.num = varDefs.Append(def);
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
                idTypeDef newtype = new idTypeDef(ev_field, null, "float field", 0, type_float);
                idTypeDef type2 = GetType(newtype, true);

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
            def.value.setBytePtr(variables, numVariables);
            numVariables += def.TypeDef().Size();
//            System.out.println(def.TypeDef().Name());
            if (numVariables > variables.length) {
                throw new idCompileError(va("Exceeded global memory size (%d bytes)", variables.length));
            }

            Arrays.fill(variables, numVariables, variables.length, (byte) 0);
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

            if (null == bestDef || (depth < bestDepth)) {
                bestDepth = depth;
                bestDef = def;
            }
        }

        // see if the name is already in use for another type
        if (bestDef != null && type != null && (!bestDef.TypeDef().equals(type))) {
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

        varDefs.RemoveIndex(def.num);
        for (i = def.num; i < varDefs.Num(); i++) {
            varDefs.oGet(i).num = i;
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

        hash = varDefNameHash.GenerateKey(name, true);
        for (i = varDefNameHash.First(hash); i != -1; i = varDefNameHash.Next(i)) {
            if (idStr.Cmp(varDefNames.oGet(i).Name(), name) == 0) {
                return varDefNames.oGet(i).GetDefs();
            }
        }
        return null;
    }

    public void AddDefToNameList(idVarDef def, final String name) {
        int i, hash;

        hash = varDefNameHash.GenerateKey(name, true);
        for (i = varDefNameHash.First(hash); i != -1; i = varDefNameHash.Next(i)) {
            if (idStr.Cmp(varDefNames.oGet(i).Name(), name) == 0) {
                break;
            }
        }
        if (i == -1) {
            i = varDefNames.Append(new idVarDefName(name));
            varDefNameHash.Add(hash, i);
        }
        varDefNames.oGet(i).AddDef(def);
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

        idStr fullname = new idStr(name);
        start = 0;
        namespaceDef = def_namespace;
        do {
            pos = fullname.Find("::", true, start);
            if (pos < 0) {
                break;
            }

            String namespaceName = fullname.Mid(start, pos - start).toString();
            def = GetDef(null, namespaceName, namespaceDef);
            if (null == def) {
                // couldn't find namespace
                return null;
            }
            namespaceDef = def;

            // skip past the ::
            start = pos + 2;
        } while (def.Type() == ev_namespace);

        String funcName = fullname.Right(fullname.Length() - start).toString();
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
        return FindFunction(name.toString());
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
        if (functions.Num() >= functions.Max()) {
            throw new idCompileError(va("Exceeded maximum allowed number of functions (%d)", functions.Max()));
        }

        // fill in the dfunction
        function_t func = functions.Alloc();
        func.eventdef = null;
        func.def = def;
        func.type = def.TypeDef();
        func.firstStatement = 0;
        func.numStatements = 0;
        func.parmTotal = 0;
        func.locals = 0;
        func.filenum = filenum;
        func.parmSize.SetGranularity(1);
        func.SetName(def.GlobalName());

        def.SetFunction(func);

        return func;
    }

    public function_t GetFunction(int index) {
        return functions.oGet(index);
    }

    public int GetFunctionIndex(final function_t func) {
        return indexOf(func, functions.Ptr());
    }

    public void SetEntity(final String name, idEntity ent) {
        idVarDef def;
        String defName = "$";

        defName += name;

        def = GetDef(type_entity, defName, def_namespace);
        if (def != null && (def.initialized != stackVariable)) {
            // 0 is reserved for NULL entity
            if (null == ent) {
                def.value.setEntityNumberPtr(0);
            } else {
                def.value.setEntityNumberPtr(ent.entityNumber + 1);
            }
        }
    }

    public statement_s AllocStatement() {
        if (statements.Num() == 61960) {
            int a = 0;
        }
        if (statements.Num() >= statements.Max()) {
            throw new idCompileError(va("Exceeded maximum allowed number of statements (%d)", statements.Max()));
        }
        return statements.Alloc();
    }

    public statement_s GetStatement(int index) {
        if (index == 61961) {
            int a = 0;
        }
        return statements.oGet(index);
    }

    public int NumStatements() {
        return statements.Num();
    }

    public int GetReturnedInteger() {
        return returnDef.value.getIntPtr();
    }

    public void ReturnFloat(float value) {
        returnDef.value.setFloatPtr(value);
    }

    public void ReturnInteger(int value) {
        returnDef.value.setIntPtr(value);
    }

    public void ReturnVector(idVec3 vec) {
        returnDef.value.setVectorPtr(vec);
    }

    public void ReturnString(final String string) {
        returnStringDef.value.stringPtr = string;//idStr.Copynz(returnStringDef.value.stringPtr, string, MAX_STRING_LEN);
    }

    public void ReturnEntity(idEntity ent) {
        if (ent != null) {
            returnDef.value.setEntityNumberPtr(ent.entityNumber + 1);
        } else {
            returnDef.value.setEntityNumberPtr(0);
        }
    }

    public int NumFilenames() {
        return fileList.Num();
    }
};
