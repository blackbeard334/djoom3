package neo.Tools;

import neo.Game.Script.Script_Interpreter.idInterpreter;
import neo.Game.Script.idProgram;
import static neo.framework.Common.common;
import neo.idlib.Dict_h.idDict;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class edit_public {

    /*
     ===============================================================================

     Editors.

     ===============================================================================
     */
    // class	idProgram;
    // class	idInterpreter;
    // Radiant Level Editor
    public static void RadiantInit() {
        common.Printf("The level editor Radiant only runs on Win32\n");
    }

    public static void RadiantShutdown() {
    }

    public static void RadiantRun() {
    }

    public static void RadiantPrint(final String text) {
    }

    public static void RadiantSync(final String mapName, final idVec3 viewOrg, final idAngles viewAngles) {
    }

    // in-game Light Editor
    public static void LightEditorInit(final idDict spawnArgs) {
        common.Printf("The Light Editor only runs on Win32\n");
    }

    public static void LightEditorShutdown() {
    }

    public static void LightEditorRun() {
    }

    // in-game Sound Editor
    public static void SoundEditorInit(final idDict spawnArgs) {
        common.Printf("The Sound Editor only runs on Win32\n");
    }

    public static void SoundEditorShutdown() {
    }

    public static void SoundEditorRun() {
    }

    // in-game Articulated Figure Editor
    public static void AFEditorInit(final idDict spawnArgs) {
        common.Printf("The Articulated Figure Editor only runs on Win32\n");
    }

    public static void AFEditorShutdown() {
    }

    public static void AFEditorRun() {
    }

    // in-game Particle Editor
    public static void ParticleEditorInit(final idDict spawnArgs) {
        common.Printf("The Particle Editor only runs on Win32\n");
    }

    public static void ParticleEditorShutdown() {
    }

    public static void ParticleEditorRun() {
    }

    // in-game PDA Editor
    public static void PDAEditorInit(final idDict spawnArgs) {
        common.Printf("The PDA editor only runs on Win32\n");
    }

    public static void PDAEditorShutdown() {
    }

    public static void PDAEditorRun() {
    }

    // in-game Script Editor
    public static void ScriptEditorInit(final idDict spawnArgs) {
        common.Printf("The Script Editor only runs on Win32\n");
    }

    public static void ScriptEditorShutdown() {
    }

    public static void ScriptEditorRun() {
    }

    // in-game Declaration Browser
    public static void DeclBrowserInit(final idDict spawnArgs) {
        common.Printf("The Declaration Browser only runs on Win32\n");
    }

    public static void DeclBrowserShutdown() {
    }

    public static void DeclBrowserRun() {
    }

    public static void DeclBrowserReloadDeclarations() {
    }

    // GUI Editor
    public static void GUIEditorInit() {
        common.Printf("The GUI Editor only runs on Win32\n");
    }

    public static void GUIEditorShutdown() {
    }

    public static void GUIEditorRun() {
    }

    public static boolean GUIEditorHandleMessage(Object msg) {
        return false;
    }

    // Script Debugger
    public static void DebuggerClientLaunch() {
    }

    public static void DebuggerClientInit(final String cmdline) {
        common.Printf("The Script Debugger Client only runs on Win32\n");
    }

    public static boolean DebuggerServerInit() {
        return false;
    }

    public static void DebuggerServerShutdown() {
    }

    public static void DebuggerServerPrint(final String text) {
    }

    public static void DebuggerServerCheckBreakpoint(idInterpreter interpreter, idProgram program, int instructionPointer) {
    }

    //Material Editor
    public static void MaterialEditorInit() {
        common.Printf("The Material editor only runs on Win32\n");
    }

    public static void MaterialEditorRun() {
    }

    public static void MaterialEditorShutdown() {
    }

    public static void MaterialEditorPrintConsole(final String msg) {
    }
}
