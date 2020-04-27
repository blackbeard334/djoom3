package neo.Tools.Compilers.AAS;

import neo.Tools.Compilers.AAS.AASFile.idAASFile;
import neo.Tools.Compilers.AAS.AASFile_local.idAASFileLocal;
import neo.idlib.Text.Str.idStr;

/**
 *
 */
public class AASFileManager {

    private static idAASFileManagerLocal AASFileManagerLocal = new idAASFileManagerLocal();
    public static  idAASFileManager      AASFileManager      = AASFileManagerLocal;

    /*
     ===============================================================================

     AAS File Manager

     ===============================================================================
     */
    public static abstract class idAASFileManager {

//	virtual						~idAASFileManager( void ) {}
        public abstract idAASFile LoadAAS(final String fileName, long/*unsigned int*/ mapFileCRC);

        public abstract void FreeAAS(idAASFile file);
    };

    /*
     ===============================================================================

     AAS File Manager

     ===============================================================================
     */
    static class idAASFileManagerLocal extends idAASFileManager {

//        virtual						~idAASFileManagerLocal( void ) {}
        @Override
        public idAASFile LoadAAS(String fileName, long mapFileCRC) {
            idAASFileLocal file = new idAASFileLocal();
            if (!file.Load(new idStr(fileName), mapFileCRC)) {
//		delete file;
                return null;
            }
            return file;
        }

        @Override
        public void FreeAAS(idAASFile file) {
//            delete file
        }
    };

    public static void setAASFileManager(idAASFileManager AASFileManager) {
        neo.Tools.Compilers.AAS.AASFileManager.AASFileManager
                = neo.Tools.Compilers.AAS.AASFileManager.AASFileManagerLocal
                = (idAASFileManagerLocal) AASFileManager;
    }
}
