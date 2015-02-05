
import neo.TempDump.void_callback;
import neo.framework.Common.idCommon;
import neo.idlib.Dict_h;
import neo.idlib.LangDict;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMatX;

/**
 *
 */
public class Test1 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        idLib.common = new CommonTest();
        idMath.Init();
        idMatX.DISABLE_RANDOM_TEST = false;//turns the randoms of idMatX and idVecX off!!!!
        idMatX.Test();
    }

    private static class CommonTest extends idCommon {

        @Override
        public void Warning(String fmt, Object... args) {
            System.err.println(String.format(fmt, args));
        }

        // the other functions aren't important for our test.
        @Override
        public void Init(int argc, String[] argv, String cmdline) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Quit() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean IsInitialized() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Frame() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void GUIFrame(boolean execCmd, boolean network) throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Async() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void StartupVariable(String match, boolean once) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void InitTool(int toolFlag_t, Dict_h.idDict dict) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ActivateTool(boolean active) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void WriteConfigToFile(String filename) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void WriteFlaggedCVarsToFile(String filename, int flags, String setCmd) throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void BeginRedirect(StringBuilder buffer, int buffersize, void_callback<String> flush) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void EndRedirect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void SetRefreshOnPrint(boolean set) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Printf(String fmt, Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void VPrintf(String fmt, Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void DPrintf(String fmt, Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void DWarning(String fmt, Object... args) throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void PrintWarnings() throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ClearWarnings(String reason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Error(String fmt, Object... args) throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void FatalError(final String fmt, Object... args) throws idException {
            throw new UnsupportedOperationException();
        }

        @Override
        public LangDict.idLangDict GetLanguageDict() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String KeysFromBinding(String bind) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String BindingFromKey(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int ButtonState(int key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int KeyState(int key) {
            throw new UnsupportedOperationException();
        }
    };
}
