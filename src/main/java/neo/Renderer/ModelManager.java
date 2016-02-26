package neo.Renderer;

import static neo.Renderer.Model.MD5_MESH_EXT;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model_beam.idRenderModelBeam;
import neo.Renderer.Model_liquid.idRenderModelLiquid;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.Renderer.Model_md3.idRenderModelMD3;
import neo.Renderer.Model_md5.idRenderModelMD5;
import neo.Renderer.Model_prt.idRenderModelPrt;
import neo.Renderer.Model_sprite.idRenderModelSprite;
import static neo.Renderer.tr_lightrun.R_CheckForEntityDefsUsingModel;
import static neo.Renderer.tr_lightrun.R_FreeDerivedData;
import static neo.Renderer.tr_lightrun.R_ReCreateWorldReferences;
import static neo.Renderer.tr_local.frameData;
import static neo.Renderer.tr_trisurf.R_PurgeTriSurfData;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_RENDERER;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common.MemInfo_t;
import static neo.framework.Common.com_purgeAll;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import static neo.framework.Session.session;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import static neo.sys.win_shared.Sys_Milliseconds;

/**
 *
 */
public class ModelManager {

    private static idRenderModelManagerLocal localModelManager = new idRenderModelManagerLocal();
    public static idRenderModelManager renderModelManager = localModelManager;

    /*
     ===============================================================================

     Model Manager

     Temporarily created models do not need to be added to the model manager.

     ===============================================================================
     */
    public static abstract class idRenderModelManager {

        // public abstract					~idRenderModelManager() {}
        // registers console commands and clears the list
        public abstract void Init() throws idException;

        // frees all the models
        public abstract void Shutdown();

        // called only by renderer::BeginLevelLoad
        public abstract void BeginLevelLoad();

        // called only by renderer::EndLevelLoad
        public abstract void EndLevelLoad();

        // allocates a new empty render model.
        public abstract idRenderModel AllocModel();

        // frees a render model
        public abstract void FreeModel(idRenderModel model);

        // returns NULL if modelName is NULL or an empty string, otherwise
        // it will create a default model if not loadable
        public abstract idRenderModel FindModel(final String modelName);

        public idRenderModel FindModel(final idStr modelName) {
            return FindModel(modelName.toString());
        }

        // returns NULL if not loadable
        public abstract idRenderModel CheckModel(final String modelName);

        public idRenderModel CheckModel(final idStr modelName) {
            return CheckModel(modelName.toString());
        }

        // returns the default cube model
        public abstract idRenderModel DefaultModel();

        // world map parsing will add all the inline models with this call
        public abstract void AddModel(idRenderModel model);

        // when a world map unloads, it removes its internal models from the list
        // before freeing them.
        // There may be an issue with multiple renderWorlds that share data...
        public abstract void RemoveModel(idRenderModel model);

        // the reloadModels console command calls this, but it can
        // also be explicitly invoked
        public abstract void ReloadModels(boolean forceAll /*= false*/);

        public void ReloadModels() {
            ReloadModels(false);
        }

        // write "touchModel <model>" commands for each non-world-map model
        public abstract void WritePrecacheCommands(idFile f);

        // called during vid_restart
        public abstract void FreeModelVertexCaches();

        // print memory info
        public abstract void PrintMemInfo(MemInfo_t mi);
    };

    public static class idRenderModelManagerLocal extends idRenderModelManager {

        private idList<idRenderModel> models;
        private idHashIndex hash;
        private idRenderModel defaultModel;
        private idRenderModel beamModel;
        private idRenderModel spriteModel;
        private idRenderModel trailModel;
        private boolean insideLevelLoad;		// don't actually load now
        //
        //

        public idRenderModelManagerLocal() {
            this.models = new idList<>();
            this.hash = new idHashIndex();
            defaultModel = null;
            beamModel = null;
            spriteModel = null;
            trailModel = null;
            insideLevelLoad = false;
        }
        // virtual					~idRenderModelManagerLocal() {}

        @Override
        public void Init() throws idException {
            cmdSystem.AddCommand("listModels", ListModels_f.getInstance(), CMD_FL_RENDERER, "lists all models");
            cmdSystem.AddCommand("printModel", PrintModel_f.getInstance(), CMD_FL_RENDERER, "prints model info", idCmdSystem.ArgCompletion_ModelName.getInstance());
            cmdSystem.AddCommand("reloadModels", ReloadModels_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "reloads models");
            cmdSystem.AddCommand("touchModel", TouchModel_f.getInstance(), CMD_FL_RENDERER, "touches a model", idCmdSystem.ArgCompletion_ModelName.getInstance());

            insideLevelLoad = false;

            // create a default model
            idRenderModelStatic model = new idRenderModelStatic();
            model.InitEmpty("_DEFAULT");
            model.MakeDefaultModel();
            model.SetLevelLoadReferenced(true);
            defaultModel = model;
            AddModel(model);

            // create the beam model
            idRenderModelStatic beam = new idRenderModelBeam();
            beam.InitEmpty("_BEAM");
            beam.SetLevelLoadReferenced(true);
            beamModel = beam;
            AddModel(beam);

            idRenderModelStatic sprite = new idRenderModelSprite();
            sprite.InitEmpty("_SPRITE");
            sprite.SetLevelLoadReferenced(true);
            spriteModel = sprite;
            AddModel(sprite);
        }

        @Override
        public void Shutdown() {
            models.DeleteContents(true);
            hash.Free();
        }

        @Override
        public idRenderModel AllocModel() {
            return new idRenderModelStatic();
        }

        @Override
        public void FreeModel(idRenderModel model) {
            if (null == model) {
                return;
            }
            if (null == ((idRenderModelStatic) model)) {//TODO:always false?
                common.Error("idRenderModelManager::FreeModel: model '%s' is not a static model", model.Name());
                return;
            }
            if (model == defaultModel) {
                common.Error("idRenderModelManager::FreeModel: can't free the default model");
                return;
            }
            if (model == beamModel) {
                common.Error("idRenderModelManager::FreeModel: can't free the beam model");
                return;
            }
            if (model == spriteModel) {
                common.Error("idRenderModelManager::FreeModel: can't free the sprite model");
                return;
            }

            R_CheckForEntityDefsUsingModel(model);

//	delete model;
        }

        @Override
        public idRenderModel FindModel(String modelName) {
            return GetModel(modelName, true);
        }

        @Override
        public idRenderModel CheckModel(String modelName) {
            return GetModel(modelName, false);
        }

        @Override
        public idRenderModel DefaultModel() {
            return defaultModel;
        }

        @Override
        public void AddModel(idRenderModel model) {
            hash.Add(hash.GenerateKey(model.Name(), false), models.Append(model));
        }

        @Override
        public void RemoveModel(idRenderModel model) {
            int index = models.FindIndex(model);
            hash.RemoveIndex(hash.GenerateKey(model.Name(), false), index);
            models.RemoveIndex(index);
        }

        @Override
        public void ReloadModels(boolean forceAll) {
            if (forceAll) {
                common.Printf("Reloading all model files...\n");
            } else {
                common.Printf("Checking for changed model files...\n");
            }

            R_FreeDerivedData();

            // skip the default model at index 0
            for (int i = 1; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);

                // we may want to allow world model reloading in the future, but we don't now
                if (!model.IsReloadable()) {
                    continue;
                }

                if (!forceAll) {
                    // check timestamp
                    final long[] current = new long[1];

                    fileSystem.ReadFile(model.Name(), null, current);
                    if (current[0] <= model.Timestamp()[0]) {
                        continue;
                    }
                }

                common.DPrintf("reloading %s.\n", model.Name());

                model.LoadModel();
            }

            // we must force the world to regenerate, because models may
            // have changed size, making their references invalid
            R_ReCreateWorldReferences();
        }

        @Override
        public void FreeModelVertexCaches() {
            for (int i = 0; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);
                model.FreeVertexCache();
            }
        }

        @Override
        public void WritePrecacheCommands(idFile f) {
            for (int i = 0; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);

                if (null == model) {
                    continue;
                }
                if (!model.IsReloadable()) {
                    continue;
                }

//		char	str[1024];
                final String str = String.format("touchModel %s\n", model.Name());
                common.Printf("%s", str);
                f.Printf("%s", str);
            }
        }

        @Override
        public void BeginLevelLoad() {
            insideLevelLoad = true;

            for (int i = 0; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);

                if (com_purgeAll.GetBool() && model.IsReloadable()) {
                    R_CheckForEntityDefsUsingModel(model);
                    model.PurgeModel();
                }

                model.SetLevelLoadReferenced(false);
            }

            // purge unused triangle surface memory
            R_PurgeTriSurfData(frameData);
        }

        @Override
        public void EndLevelLoad() {
            common.Printf("----- idRenderModelManagerLocal::EndLevelLoad -----\n");

            int start = Sys_Milliseconds();

            insideLevelLoad = false;
            int purgeCount = 0;
            int keepCount = 0;
            int loadCount = 0;

            // purge any models not touched
            for (int i = 0; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);

                if (!model.IsLevelLoadReferenced() && model.IsLoaded() && model.IsReloadable()) {

//			common.Printf( "purging %s\n", model.Name() );
                    purgeCount++;

                    R_CheckForEntityDefsUsingModel(model);

                    model.PurgeModel();

                } else {

//			common.Printf( "keeping %s\n", model.Name() );
                    keepCount++;
                }
            }

            // purge unused triangle surface memory
            R_PurgeTriSurfData(frameData);

            // load any new ones
            for (int i = 0; i < models.Num(); i++) {
                idRenderModel model = models.oGet(i);

                if (model.IsLevelLoadReferenced() && !model.IsLoaded() && model.IsReloadable()) {

                    loadCount++;
                    model.LoadModel();

                    if ((loadCount & 15) == 0) {
                        session.PacifierUpdate();
                    }
                }
            }

            // _D3XP added this
            int end = Sys_Milliseconds();
            common.Printf("%5d models purged from previous level, ", purgeCount);
            common.Printf("%5d models kept.\n", keepCount);
            if (loadCount != 0) {
                common.Printf("%5d new models loaded in %5.1f seconds\n", loadCount, (end - start) * 0.001);
            }
            common.Printf("---------------------------------------------------\n");
        }

        @Override
        public void PrintMemInfo(MemInfo_t mi) {
            int i, j, totalMem = 0;
            int[] sortIndex;
            idFile f;

            f = fileSystem.OpenFileWrite(mi.filebase + "_models.txt");
            if (null == f) {
                return;
            }

            // sort first
            sortIndex = new int[localModelManager.models.Num()];

            for (i = 0; i < localModelManager.models.Num(); i++) {
                sortIndex[i] = i;
            }

            for (i = 0; i < localModelManager.models.Num() - 1; i++) {
                for (j = i + 1; j < localModelManager.models.Num(); j++) {
                    if (localModelManager.models.oGet(sortIndex[i]).Memory() < localModelManager.models.oGet(sortIndex[j]).Memory()) {
                        final int temp = sortIndex[i];
                        sortIndex[i] = sortIndex[j];
                        sortIndex[j] = temp;
                    }
                }
            }

            // print next
            for (i = 0; i < localModelManager.models.Num(); i++) {
                idRenderModel model = localModelManager.models.oGet(sortIndex[i]);
                int mem;

                if (!model.IsLoaded()) {
                    continue;
                }

                mem = model.Memory();
                totalMem += mem;
                f.Printf("%s %s\n", idStr.FormatNumber(mem).toString(), model.Name());
            }

//	delete sortIndex;
            mi.modelAssetsTotal = totalMem;

            f.Printf("\nTotal model bytes allocated: %s\n", idStr.FormatNumber(totalMem).toString());
            fileSystem.CloseFile(f);
        }

        private idRenderModel GetModel(final String modelName, boolean createIfNotFound) {
            idStr canonical;
            idStr extension = new idStr();

            if (null == modelName || modelName.isEmpty()) {
                return null;
            }

            canonical = new idStr(modelName);
            canonical.ToLower();

            // see if it is already present
            int key = hash.GenerateKey(modelName, false);
            for (int i = hash.First(key); i != -1; i = hash.Next(i)) {
                idRenderModel model = models.oGet(i);

                if (canonical.Icmp(model.Name()) == 0) {
                    if (!model.IsLoaded()) {
                        // reload it if it was purged
                        model.LoadModel();
                    } else if (insideLevelLoad && !model.IsLevelLoadReferenced()) {
                        // we are reusing a model already in memory, but
                        // touch all the materials to make sure they stay
                        // in memory as well
                        model.TouchData();
                    }
                    model.SetLevelLoadReferenced(true);
                    return model;
                }
            }

            // see if we can load it
            // determine which subclass of idRenderModel to initialize
            idRenderModel model;

            canonical.ExtractFileExtension(extension);

            if ((extension.Icmp("ase") == 0) || (extension.Icmp("lwo") == 0) || (extension.Icmp("flt") == 0)) {
                model = new idRenderModelStatic();
                model.InitFromFile(modelName);
            } else if (extension.Icmp("ma") == 0) {
                model = new idRenderModelStatic();
                model.InitFromFile(modelName);
            } else if (extension.Icmp(MD5_MESH_EXT) == 0) {
                model = new idRenderModelMD5();
                model.InitFromFile(modelName);
            } else if (extension.Icmp("md3") == 0) {
                model = new idRenderModelMD3();
                model.InitFromFile(modelName);
            } else if (extension.Icmp("prt") == 0) {
                model = new idRenderModelPrt();
                model.InitFromFile(modelName);
            } else if (extension.Icmp("liquid") == 0) {
                model = new idRenderModelLiquid();
                model.InitFromFile(modelName);
            } else {

                if (extension.Length() != 0) {
                    common.Warning("unknown model type '%s'", canonical);
                }

                if (!createIfNotFound) {
                    return null;
                }

                idRenderModelStatic smodel = new idRenderModelStatic();
                smodel.InitEmpty(modelName);
                smodel.MakeDefaultModel();

                model = smodel;
            }

            model.SetLevelLoadReferenced(true);

            if (!createIfNotFound && model.IsDefaultModel()) {
//		delete model;
                model = null;

                return null;
            }

            AddModel(model);

            return model;
        }


        /*
         ==============
         idRenderModelManagerLocal::PrintModel_f
         ==============
         */
        private static class PrintModel_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new PrintModel_f();

            private PrintModel_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                idRenderModel model;

                if (args.Argc() != 2) {
                    common.Printf("usage: printModel <modelName>\n");
                    return;
                }

                model = renderModelManager.CheckModel(args.Argv(1));
                if (null == model) {
                    common.Printf("model \"%s\" not found\n", args.Argv(1));
                    return;
                }

                model.Print();
            }
        };

        /*
         ==============
         idRenderModelManagerLocal::ListModels_f
         ==============
         */
        private static class ListModels_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListModels_f();

            private ListModels_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                int totalMem = 0;
                int inUse = 0;

                common.Printf(" mem   srf verts tris\n");
                common.Printf(" ---   --- ----- ----\n");

                for (int i = 0; i < localModelManager.models.Num(); i++) {
                    idRenderModel model = localModelManager.models.oGet(i);

                    if (!model.IsLoaded()) {
                        continue;
                    }
                    model.List();
                    totalMem += model.Memory();
                    inUse++;
                }

                common.Printf(" ---   --- ----- ----\n");
                common.Printf(" mem   srf verts tris\n");

                common.Printf("%d loaded models\n", inUse);
                common.Printf("total memory: %4.1fM\n", (float) totalMem / (1024 * 1024));
            }
        };

        /*
         ==============
         idRenderModelManagerLocal::ReloadModels_f
         ==============
         */
        private static class ReloadModels_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ReloadModels_f();

            private ReloadModels_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (idStr.Icmp(args.Argv(1), "all") == 0) {
                    localModelManager.ReloadModels(true);
                } else {
                    localModelManager.ReloadModels(false);
                }
            }
        };

        /*
         ==============
         idRenderModelManagerLocal::TouchModel_f

         Precache a specific model
         ==============
         */
        private static class TouchModel_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new TouchModel_f();

            private TouchModel_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                String model = args.Argv(1);

                if (model.isEmpty()) {
                    common.Printf("usage: touchModel <modelName>\n");
                    return;
                }

                common.Printf("touchModel %s\n", model);
                session.UpdateScreen();
                idRenderModel m = renderModelManager.CheckModel(model);
                if (null == m) {
                    common.Printf("...not found\n");
                }
            }
        };
    };

    public static void setRenderModelManager(idRenderModelManager renderModelManager) {
        ModelManager.renderModelManager = ModelManager.localModelManager = (idRenderModelManagerLocal) renderModelManager;
    }
}
