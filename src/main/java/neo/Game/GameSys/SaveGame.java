package neo.Game.GameSys;

import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.TempDump.atobb;
import static neo.framework.BuildVersion.BUILD_NUMBER;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_FX;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.idlib.Lib.LittleRevBytes;
import static neo.idlib.geometry.TraceModel.MAX_TRACEMODEL_EDGES;
import static neo.idlib.geometry.TraceModel.MAX_TRACEMODEL_POLYEDGES;
import static neo.idlib.geometry.TraceModel.MAX_TRACEMODEL_POLYS;
import static neo.idlib.geometry.TraceModel.MAX_TRACEMODEL_VERTS;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.contactType_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Game.refSound_t;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idTypeInfo;
import neo.Game.Physics.Clip.idClipModel;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclFX.idDeclFX;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.framework.File_h.idFile;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.TraceModel.traceModel_t;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVec5;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Matrix.idMat3;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class SaveGame {

    /*
     Save game related helper classes.

     Save games are implemented in two classes, idSaveGame and idRestoreGame, that implement write/read functions for 
     common types.  They're passed in to each entity and object for them to archive themselves.  Each class
     implements save/restore functions for it's own data.  When restoring, all the objects are instantiated,
     then the restore function is called on each, superclass first, then subclasses.

     Pointers are restored by saving out an object index for each unique object pointer and adding them to a list of
     objects that are to be saved.  Restore instantiates all the objects in the list before calling the Restore function
     on each object so that the pointers returned are valid.  No object's restore function should rely on any other objects
     being fully instantiated until after the restore process is complete.  Post restore fixup should be done by posting
     events with 0 delay.

     The savegame header will have the Game Name, Version, Map Name, and Player Persistent Info.

     Changes in version make savegames incompatible, and the game will start from the beginning of the level with
     the player's persistent info.

     Changes to classes that don't need to break compatibilty can use the build number as the savegame version.
     Later versions are responsible for restoring from previous versions by ignoring any unused data and initializing
     variables that weren't in previous versions with safe information.

     At the head of the save game is enough information to restore the player to the beginning of the level should the
     file be unloadable in some way (for example, due to script changes).
     */
    public static final int INITIAL_RELEASE_BUILD_NUMBER = 1262;

    public static class idSaveGame {

        private idFile          file;
        //
        private idList<idClass> objects;
        //
        //

        public idSaveGame(idFile savefile) {

            file = savefile;

            // Put NULL at the start of the list so we can skip over it.
            objects = new idList<>();
            objects.Append((idClass) null);
        }

        // ~idSaveGame();
        public void Close() {
            int i;

            WriteSoundCommands();

            // read trace models
            idClipModel.SaveTraceModels(this);

            for (i = 1; i < objects.Num(); i++) {
                CallSave_r(objects.oGet(i).GetType(), objects.oGet(i));
            }

            objects.Clear();

// #ifdef ID_DEBUG_MEMORY
            // idStr gameState = file.GetName();
            // gameState.StripFileExtension();
            // WriteGameState_f( idCmdArgs( va( "test %s_save", gameState.c_str() ), false ) );
// #endif
        }

        public void AddObject(final idClass obj) {
            objects.AddUnique(obj);
        }

        public void WriteObjectList() {
            int i;

            WriteInt(objects.Num() - 1);
            for (i = 1; i < objects.Num(); i++) {
                WriteString(objects.oGet(i).GetClassname());
            }
        }

        public void Write(final ByteBuffer buffer, int len) {
            file.Write(buffer, len);
        }

        public void Write(final SERiAL buffer) {
            file.Write(buffer);
        }

        public void WriteInt(final int value) {
            file.WriteInt(value);
        }

        public void WriteJoint(final int/*jointHandle_t*/ value) {
            file.WriteInt((int) value);
        }

        public void WriteShort(final short value) {
            file.WriteShort(value);
        }

        public void WriteByte(final byte value) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(value);

            file.Write(buffer, 1);//sizeof(value));
        }

        public void WriteSignedChar(final short/*signed char*/ value) {
            ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
            buffer.putShort(value);

            file.Write(buffer, Short.BYTES);//sizeof(value));
        }

        public void WriteFloat(final float value) {
            file.WriteFloat(value);
        }

        public void WriteBool(final boolean value) {
            file.WriteBool(value);
        }

        public void WriteString(final String string) {
            int len;

            len = string.length();
            WriteInt(len);
            file.Write(atobb(string), len);
        }

        public void WriteString(final idStr string) {
            this.WriteString(string.toString());
        }

        public void WriteVec2(final idVec2 vec) {
            file.WriteVec2(vec);
        }

        public void WriteVec3(final idVec3 vec) {
            file.WriteVec3(vec);
        }

        public void WriteVec4(final idVec4 vec) {
            file.WriteVec4(vec);
        }

        public void WriteVec6(final idVec6 vec) {
            file.WriteVec6(vec);
        }

        public void WriteWinding(final idWinding w) {
            int i, num;
            num = w.GetNumPoints();
            file.WriteInt(num);
            for (i = 0; i < num; i++) {
                idVec5 v = w.oGet(i);
                LittleRevBytes(v/*, sizeof(float), sizeof(v) / sizeof(float)*/);
                file.Write(v/*, sizeof(v)*/);
            }
        }

        public void WriteBounds(final idBounds bounds) {
            idBounds b = bounds;
            LittleRevBytes(b/*, sizeof(float), sizeof(b) / sizeof(float)*/);
            file.Write(b/*, sizeof(b)*/);
        }

        public void WriteMat3(final idMat3 mat) {
            file.WriteMat3(mat);
        }

        public void WriteAngles(final idAngles angles) {
            idAngles v = angles;
            LittleRevBytes(v/*, sizeof(float), sizeof(v) / sizeof(float)*/);
            file.Write(v/*, sizeof(v)*/);
        }

        public void WriteObject(final idClass obj) {
            int index;

            index = objects.FindIndex(obj);
            if (index < 0) {
                gameLocal.DPrintf("idSaveGame::WriteObject - WriteObject FindIndex failed\n");

                // Use the NULL index
                index = 0;
            }

            WriteInt(index);
        }

        public void WriteStaticObject(final idClass obj) {
            CallSave_r(obj.GetType(), obj);
        }

        public void WriteDict(final idDict dict) {
            int num;
            int i;
            idKeyValue kv;

            if (null == dict) {
                WriteInt(-1);
            } else {
                num = dict.GetNumKeyVals();
                WriteInt(num);
                for (i = 0; i < num; i++) {
                    kv = dict.GetKeyVal(i);
                    WriteString(kv.GetKey());
                    WriteString(kv.GetValue());
                }
            }
        }

        public void WriteMaterial(final idMaterial material) {
            if (null == material) {
                WriteString("");
            } else {
                WriteString(material.GetName());
            }
        }

        public void WriteSkin(final idDeclSkin skin) {
            if (null == skin) {
                WriteString("");
            } else {
                WriteString(skin.GetName());
            }
        }

        public void WriteParticle(final idDeclParticle particle) {
            if (null == particle) {
                WriteString("");
            } else {
                WriteString(particle.GetName());
            }
        }

        public void WriteFX(final idDeclFX fx) {
            if (null == fx) {
                WriteString("");
            } else {
                WriteString(fx.GetName());
            }
        }

        public void WriteSoundShader(final idSoundShader shader) {
            String name;

            if (null == shader) {
                WriteString("");
            } else {
                name = shader.GetName();
                WriteString(name);
            }
        }

        public void WriteModelDef(final idDeclModelDef modelDef) {
            if (null == modelDef) {
                WriteString("");
            } else {
                WriteString(modelDef.GetName());
            }
        }

        public void WriteModel(final idRenderModel model) {
            String name;

            if (null == model) {
                WriteString("");
            } else {
                name = model.Name();
                WriteString(name);
            }
        }

        public void WriteUserInterface(final idUserInterface ui, boolean unique) {
            String name;

            if (null == ui) {
                WriteString("");
            } else {
                name = ui.Name();
                WriteString(name);
                WriteBool(unique);
                if (ui.WriteToSaveGame(file) == false) {
                    gameLocal.Error("idSaveGame::WriteUserInterface: ui failed to write properly\n");
                }
            }
        }

        public void WriteRenderEntity(final renderEntity_s renderEntity) {
            int i;

            WriteModel(renderEntity.hModel);

            WriteInt(renderEntity.entityNum);
            WriteInt(renderEntity.bodyId);

            WriteBounds(renderEntity.bounds);

            // callback is set by class's Restore function
            WriteInt(renderEntity.suppressSurfaceInViewID);
            WriteInt(renderEntity.suppressShadowInViewID);
            WriteInt(renderEntity.suppressShadowInLightID);
            WriteInt(renderEntity.allowSurfaceInViewID);

            WriteVec3(renderEntity.origin);
            WriteMat3(renderEntity.axis);

            WriteMaterial(renderEntity.customShader);
            WriteMaterial(renderEntity.referenceShader);
            WriteSkin(renderEntity.customSkin);

            if (renderEntity.referenceSound != null) {
                WriteInt(renderEntity.referenceSound.Index());
            } else {
                WriteInt(0);
            }

            for (i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                WriteFloat(renderEntity.shaderParms[ i]);
            }

            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                WriteUserInterface(renderEntity.gui[ i], renderEntity.gui[ i] != null ? renderEntity.gui[ i].IsUniqued() : false);
            }

            WriteFloat(renderEntity.modelDepthHack);

            WriteBool(renderEntity.noSelfShadow);
            WriteBool(renderEntity.noShadow);
            WriteBool(renderEntity.noDynamicInteractions);
            WriteBool(renderEntity.weaponDepthHack);

            WriteInt(renderEntity.forceUpdate);
        }

        public void WriteRenderLight(final renderLight_s renderLight) {
            int i;

            WriteMat3(renderLight.axis);
            WriteVec3(renderLight.origin);

            WriteInt(renderLight.suppressLightInViewID);
            WriteInt(renderLight.allowLightInViewID);
            WriteBool(renderLight.noShadows);
            WriteBool(renderLight.noSpecular);
            WriteBool(renderLight.pointLight);
            WriteBool(renderLight.parallel);

            WriteVec3(renderLight.lightRadius);
            WriteVec3(renderLight.lightCenter);

            WriteVec3(renderLight.target);
            WriteVec3(renderLight.right);
            WriteVec3(renderLight.up);
            WriteVec3(renderLight.start);
            WriteVec3(renderLight.end);

            // only idLight has a prelightModel and it's always based on the entityname, so we'll restore it there
            // WriteModel( renderLight.prelightModel );
            WriteInt(renderLight.lightId);

            WriteMaterial(renderLight.shader);

            for (i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                WriteFloat(renderLight.shaderParms[ i]);
            }

            if (renderLight.referenceSound != null) {
                WriteInt(renderLight.referenceSound.Index());
            } else {
                WriteInt(0);
            }
        }

        public void WriteRefSound(final refSound_t refSound) {
            if (refSound.referenceSound != null) {
                WriteInt(refSound.referenceSound.Index());
            } else {
                WriteInt(0);
            }
            WriteVec3(refSound.origin);
            WriteInt(refSound.listenerId);
            WriteSoundShader(refSound.shader);
            WriteFloat(refSound.diversity);
            WriteBool(refSound.waitfortrigger);

            WriteFloat(refSound.parms.minDistance);
            WriteFloat(refSound.parms.maxDistance);
            WriteFloat(refSound.parms.volume);
            WriteFloat(refSound.parms.shakes);
            WriteInt(refSound.parms.soundShaderFlags);
            WriteInt(refSound.parms.soundClass);
        }

        public void WriteRenderView(final renderView_s view) {
            int i;

            WriteInt(view.viewID);
            WriteInt(view.x);
            WriteInt(view.y);
            WriteInt(view.width);
            WriteInt(view.height);

            WriteFloat(view.fov_x);
            WriteFloat(view.fov_y);
            WriteVec3(view.vieworg);
            WriteMat3(view.viewaxis);

            WriteBool(view.cramZNear);

            WriteInt(view.time);

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                WriteFloat(view.shaderParms[ i]);
            }
        }

        public void WriteUsercmd(final usercmd_t usercmd) {
            WriteInt(usercmd.gameFrame);
            WriteInt(usercmd.gameTime);
            WriteInt(usercmd.duplicateCount);
            WriteByte(usercmd.buttons);
            WriteSignedChar(usercmd.forwardmove);
            WriteSignedChar(usercmd.rightmove);
            WriteSignedChar(usercmd.upmove);
            WriteShort(usercmd.angles[0]);
            WriteShort(usercmd.angles[1]);
            WriteShort(usercmd.angles[2]);
            WriteShort(usercmd.mx);
            WriteShort(usercmd.my);
            WriteSignedChar(usercmd.impulse);
            WriteByte(usercmd.flags);
            WriteInt(usercmd.sequence);
        }

        public void WriteContactInfo(final contactInfo_t contactInfo) {
            WriteInt(contactInfo.type.ordinal());
            WriteVec3(contactInfo.point);
            WriteVec3(contactInfo.normal);
            WriteFloat(contactInfo.dist);
            WriteInt(contactInfo.contents);
            WriteMaterial(contactInfo.material);
            WriteInt(contactInfo.modelFeature);
            WriteInt(contactInfo.trmFeature);
            WriteInt(contactInfo.entityNum);
            WriteInt(contactInfo.id);
        }

        public void WriteTrace(final trace_s trace) {
            WriteFloat(trace.fraction);
            WriteVec3(trace.endpos);
            WriteMat3(trace.endAxis);
            WriteContactInfo(trace.c);
        }

        public void WriteTraceModel(final idTraceModel trace) {
            int j, k;

            WriteInt(trace.type.ordinal());
            WriteInt(trace.numVerts);
            for (j = 0; j < MAX_TRACEMODEL_VERTS; j++) {
                WriteVec3(trace.verts[j]);
            }
            WriteInt(trace.numEdges);
            for (j = 0; j < (MAX_TRACEMODEL_EDGES + 1); j++) {
                WriteInt(trace.edges[j].v[0]);
                WriteInt(trace.edges[j].v[1]);
                WriteVec3(trace.edges[j].normal);
            }
            WriteInt(trace.numPolys);
            for (j = 0; j < MAX_TRACEMODEL_POLYS; j++) {
                WriteVec3(trace.polys[j].normal);
                WriteFloat(trace.polys[j].dist);
                WriteBounds(trace.polys[j].bounds);
                WriteInt(trace.polys[j].numEdges);
                for (k = 0; k < MAX_TRACEMODEL_POLYEDGES; k++) {
                    WriteInt(trace.polys[j].edges[k]);
                }
            }
            WriteVec3(trace.offset);
            WriteBounds(trace.bounds);
            WriteBool(trace.isConvex);
            // padding win32 native structs
//            char[] tmp = new char[3];
            ByteBuffer tmp = ByteBuffer.allocate(6);
//	memset( tmp, 0, sizeof( tmp ) );
            file.Write(tmp, 3);
        }

        public void WriteClipModel(final idClipModel clipModel) {
            if (clipModel != null) {
                WriteBool(true);
                clipModel.Save(this);
            } else {
                WriteBool(false);
            }
        }

        public void WriteSoundCommands() {
            gameSoundWorld.WriteToSaveGame(file);
        }

        public void WriteBuildNumber(final int value) {
            file.WriteInt(BUILD_NUMBER);
        }

        private void CallSave_r(final idTypeInfo cls, final idClass obj) {
            if (cls.zuper != null) {
                CallSave_r(cls.zuper, obj);
                if (cls.zuper.Save.equals(cls.Save)) {
                    // don't call save on this inheritance level since the function was called in the super class
                    return;
                }
            }
//            (obj.cls.Save) (this);
            cls.Save.run(this);
        }

        private void CallSave_r(final java.lang.Class/*idTypeInfo*/ cls, final idClass obj) {
            throw new TODO_Exception();
        }
    };

    /* **********************************************************************

     idRestoreGame
	
     ***********************************************************************/
    public static class idRestoreGame {

        private int buildNumber;
        //
        private idFile file;
        //
        private idList<idClass> objects;
        //
        //

        public idRestoreGame(idFile savefile) {
            file = savefile;
        }
        // ~idRestoreGame();

        public void CreateObjects() {
            int i;
            int[] num = {0};
            idStr className = new idStr();
            idTypeInfo type;

            ReadInt(num);

            // create all the objects
            objects.SetNum(num[0] + 1);
//            memset(objects.Ptr(), 0, sizeof(objects[ 0]) * objects.Num());
            Arrays.fill(objects.Ptr(), 0, objects.Num(), 0);

            for (i = 1; i < objects.Num(); i++) {
                ReadString(className);
                type = idClass.GetClass(className.toString());
                if (null == type) {
                    Error("idRestoreGame::CreateObjects: Unknown class '%s'", className.toString());
                }
                objects.oSet(i, type.CreateInstance.run());

// #ifdef ID_DEBUG_MEMORY
                // InitTypeVariables( objects[i], type.classname, 0xce );
// #endif
            }
        }

        public void RestoreObjects() {
            int i;

            ReadSoundCommands();

            // read trace models
            idClipModel.RestoreTraceModels(this);

            // restore all the objects
            for (i = 1; i < objects.Num(); i++) {
                CallRestore_r(objects.oGet(i).GetType(), objects.oGet(i));
            }

            // regenerate render entities and render lights because are not saved
            for (i = 1; i < objects.Num(); i++) {
                if (objects.oGet(i).IsType(idEntity.class)) {
                    idEntity ent = ((idEntity) objects.oGet(i));
                    ent.UpdateVisuals();
                    ent.Present();
                }
            }

// #ifdef ID_DEBUG_MEMORY
            // idStr gameState = file.GetName();
            // gameState.StripFileExtension();
            // WriteGameState_f( idCmdArgs( va( "test %s_restore", gameState.c_str() ), false ) );
            // //CompareGameState_f( idCmdArgs( va( "test %s_save", gameState.c_str() ) ) );
            // gameLocal.Error( "dumped game states" );
// #endif
        }

        public void DeleteObjects() {

            // Remove the NULL object before deleting
            objects.RemoveIndex(0);

            objects.DeleteContents(true);
        }

        public void Error(final String fmt, Object... objects) {// id_attribute((format(printf,2,3)));
            throw new TODO_Exception();
//            va_list argptr;
//            char[] text = new char[1024];
//
//            va_start(argptr, fmt);
//            vsprintf(text, fmt, argptr);
//            va_end(argptr);
//
//            objects.DeleteContents(true);
//
//            gameLocal.Error("%s", text);
        }

        public void Read(ByteBuffer buffer, int len) {
            file.Read(buffer, len);
        }

        public void Read(SERiAL buffer) {
            file.Read(buffer);
        }

        public void ReadInt(int[] value) {
            file.ReadInt(value);
        }

        public int ReadInt() {
            int[] value = {0};

            this.ReadInt(value);

            return value[0];
        }

        public void ReadJoint(int[] jointHandle_t) {
            file.ReadInt(jointHandle_t);
        }

        public int ReadJoint() {
            int[] jointHandle_t = {0};
            this.ReadJoint(jointHandle_t);

            return jointHandle_t[0];
        }

        public void ReadShort(short[] value) {
            file.ReadShort(value);
        }

        public short ReadShort() {
            short[] value = {0};

            this.ReadShort(value);

            return value[0];
        }

        public void ReadByte(byte[] value) {
            file.Read(ByteBuffer.wrap(value)/*, sizeof(value)*/);
        }

        public byte ReadByte() {
            byte[] value = {0};

            this.ReadByte(value);

            return value[0];
        }

        public void ReadSignedChar(char[] value) {
            file.ReadUnsignedChar(value/*, sizeof(value)*/);
        }

        public char ReadSignedChar() {
            char[] c = new char[1];

            ReadSignedChar(c);

            return c[0];
        }

        public void ReadFloat(float[] value) {
            file.ReadFloat(value);
        }

        public float ReadFloat() {
            float[] value = {0};

            this.ReadFloat(value);

            return value[0];
        }

        public void ReadBool(boolean[] value) {
            file.ReadBool(value);
        }

        public boolean ReadBool() {
            boolean[] value = {false};
            this.ReadBool(value);

            return value[0];
        }

        public void ReadString(idStr string) {
            int[] len = {0};

            ReadInt(len);
            if (len[0] < 0) {
                Error("idRestoreGame::ReadString: invalid length");
            }

            string.Fill(' ', len[0]);
            file.Read(atobb(string), len[0]);
        }

        public void ReadVec2(idVec2 vec) {
            file.ReadVec2(vec);
        }

        public void ReadVec3(idVec3 vec) {
            file.ReadVec3(vec);
        }

        public void ReadVec4(idVec4 vec) {
            file.ReadVec4(vec);
        }

        public void ReadVec6(idVec6 vec) {
            file.ReadVec6(vec);
        }

        public void ReadWinding(idWinding w) {
            int i;
            int[] num = {0};
            file.ReadInt(num);
            w.SetNumPoints(num[0]);
            for (i = 0; i < num[0]; i++) {
                file.Read(w.oGet(i)/*, sizeof(idVec5)*/);
//                LittleRevBytes(w.oGet(i), sizeof(float), sizeof(idVec5) / sizeof(float));
                LittleRevBytes(w.oGet(i)/*, sizeof(float), sizeof(idVec5) / sizeof(float)*/);
            }
        }

        public void ReadBounds(idBounds bounds) {
            file.Read(bounds/*, sizeof(bounds)*/);
//            LittleRevBytes(bounds, sizeof(float), sizeof(bounds) / sizeof(float));
            LittleRevBytes(bounds/*, sizeof(float), sizeof(bounds) / sizeof(float)*/);
        }

        public void ReadMat3(idMat3 mat) {
            file.ReadMat3(mat);
        }

        public void ReadAngles(idAngles angles) {
            file.Read(angles/*, sizeof(angles)*/);
            LittleRevBytes(angles/*, sizeof(float), sizeof(idAngles) / sizeof(float)*/);
        }

        public void ReadObject(idClass obj) {
            throw new TODO_Exception();//TODO:remove the parameter, and return obj instead
//            int[] index = {0};
//
//            ReadInt(index);
//            if ((index[0] < 0) || (index[0] >= objects.Num())) {
//                Error("idRestoreGame::ReadObject: invalid object index");
//            }
//            obj.oSet(objects.oGet(index[0]));
        }

        public void ReadStaticObject(idClass obj) {
            CallRestore_r(obj.GetType(), obj);
        }

        public void ReadDict(idDict dict) {
            int[] num = {0};
            int i;
            idStr key = new idStr();
            idStr value = new idStr();

            ReadInt(num);

            if (num[0] < 0) {
                dict.oSet(null);
            } else {
                dict.Clear();
                for (i = 0; i < num[0]; i++) {
                    ReadString(key);
                    ReadString(value);
                    dict.Set(key, value);
                }
            }
        }

        public void ReadMaterial(final idMaterial material) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                material.oSet(null);
            } else {
                material.oSet(declManager.FindMaterial(name));
            }
        }

        public void ReadSkin(final idDeclSkin skin) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                skin.oSet(null);
            } else {
                skin.oSet(declManager.FindSkin(name));
            }
        }

        public void ReadParticle(final idDeclParticle particle) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                particle.oSet(null);
            } else {
                particle.oSet((idDeclParticle) declManager.FindType(DECL_PARTICLE, name));
            }
        }

        public void ReadFX(final idDeclFX fx) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                fx.oSet(null);
            } else {
                fx.oSet((idDeclFX) declManager.FindType(DECL_FX, name));
            }
        }

        public void ReadSoundShader(final idSoundShader shader) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                shader.oSet(null);
            } else {
                shader.oSet(declManager.FindSound(name));
            }
        }

        public void ReadModelDef(final idDeclModelDef modelDef) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                modelDef.oSet(null);
            } else {
                modelDef.oSet((idDeclModelDef) declManager.FindType(DECL_MODELDEF, name, false));
            }
        }

        public void ReadModel(idRenderModel model) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                model.oSet(null);
            } else {
                model.oSet(renderModelManager.FindModel(name.toString()));
            }
        }

        public void ReadUserInterface(idUserInterface ui) {
            idStr name = new idStr();

            ReadString(name);
            if (0 == name.Length()) {
                ui.oSet(null);
            } else {
                boolean[] unique = {false};
                ReadBool(unique);
                ui.oSet(uiManager.FindGui(name.toString(), true, unique[0]));
                if (ui != null) {
                    if (ui.ReadFromSaveGame(file) == false) {
                        Error("idSaveGame::ReadUserInterface: ui failed to read properly\n");
                    } else {
                        ui.StateChanged(gameLocal.time);
                    }
                }
            }
        }

        public void ReadRenderEntity(renderEntity_s renderEntity) {
            int i;
            int[] index = {0};

            ReadModel(renderEntity.hModel);

            renderEntity.entityNum = ReadInt();
            renderEntity.bodyId = ReadInt();

            ReadBounds(renderEntity.bounds);

            // callback is set by class's Restore function
            renderEntity.callback = null;
            renderEntity.callbackData = null;

            renderEntity.suppressSurfaceInViewID = ReadInt();
            renderEntity.suppressShadowInViewID = ReadInt();
            renderEntity.suppressShadowInLightID = ReadInt();
            renderEntity.allowSurfaceInViewID = ReadInt();

            ReadVec3(renderEntity.origin);
            ReadMat3(renderEntity.axis);

            ReadMaterial(renderEntity.customShader);
            ReadMaterial(renderEntity.referenceShader);
            ReadSkin(renderEntity.customSkin);

            ReadInt(index);
            renderEntity.referenceSound = gameSoundWorld.EmitterForIndex(index[0]);

            for (i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                renderEntity.shaderParms[i] = ReadFloat();
            }

            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                ReadUserInterface(renderEntity.gui[i]);
            }

            // idEntity will restore "cameraTarget", which will be used in idEntity::Present to restore the remoteRenderView
            renderEntity.remoteRenderView = null;

            renderEntity.joints = null;
            renderEntity.numJoints = 0;

            renderEntity.modelDepthHack = ReadFloat();

            renderEntity.noSelfShadow = ReadBool();
            renderEntity.noShadow = ReadBool();
            renderEntity.noDynamicInteractions = ReadBool();
            renderEntity.weaponDepthHack = ReadBool();

            renderEntity.forceUpdate = ReadInt();
        }

        public void ReadRenderLight(renderLight_s renderLight) {
            int[] index = {0};
            int i;

            ReadMat3(renderLight.axis);
            ReadVec3(renderLight.origin);

            renderLight.suppressLightInViewID = ReadInt();
            renderLight.allowLightInViewID = ReadInt();
            renderLight.noShadows = ReadBool();
            renderLight.noSpecular = ReadBool();
            renderLight.pointLight = ReadBool();
            renderLight.parallel = ReadBool();

            ReadVec3(renderLight.lightRadius);
            ReadVec3(renderLight.lightCenter);

            ReadVec3(renderLight.target);
            ReadVec3(renderLight.right);
            ReadVec3(renderLight.up);
            ReadVec3(renderLight.start);
            ReadVec3(renderLight.end);

            // only idLight has a prelightModel and it's always based on the entityname, so we'll restore it there
            // ReadModel( renderLight.prelightModel );
            renderLight.prelightModel = null;

            renderLight.lightId = ReadInt();

            ReadMaterial(renderLight.shader);

            for (i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                renderLight.shaderParms[i] = ReadFloat();
            }

            ReadInt(index);
            renderLight.referenceSound = gameSoundWorld.EmitterForIndex(index[0]);
        }

        public void ReadRefSound(refSound_t refSound) {
            int[] index = {0};
            ReadInt(index);

            refSound.referenceSound = gameSoundWorld.EmitterForIndex(index[0]);
            ReadVec3(refSound.origin);
            refSound.listenerId = ReadInt();
            ReadSoundShader(refSound.shader);
            refSound.diversity = ReadFloat();
            refSound.waitfortrigger = ReadBool();

            refSound.parms.minDistance = ReadFloat();
            refSound.parms.maxDistance = ReadFloat();
            refSound.parms.volume = ReadFloat();
            refSound.parms.shakes = ReadFloat();
            refSound.parms.soundShaderFlags = ReadInt();
            refSound.parms.soundClass = ReadInt();
        }

        public void ReadRenderView(renderView_s view) {
            int i;

            view.viewID = ReadInt();
            view.x = ReadInt();
            view.y = ReadInt();
            view.width = ReadInt();
            view.height = ReadInt();

            view.fov_x = ReadFloat();
            view.fov_y = ReadFloat();
            ReadVec3(view.vieworg);
            ReadMat3(view.viewaxis);

            view.cramZNear = ReadBool();

            view.time = ReadInt();

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                view.shaderParms[i] = ReadFloat();
            }
        }

        public void ReadUsercmd(usercmd_t usercmd) {
            usercmd.gameFrame = ReadInt();
            usercmd.gameTime = ReadInt();
            usercmd.duplicateCount = ReadInt();
            usercmd.buttons = ReadByte();
            usercmd.forwardmove = (byte) ReadSignedChar();
            usercmd.rightmove = (byte) ReadSignedChar();
            usercmd.upmove = (byte) ReadSignedChar();
            usercmd.angles[0] = ReadShort();
            usercmd.angles[1] = ReadShort();
            usercmd.angles[2] = ReadShort();
            usercmd.mx = ReadShort();
            usercmd.my = ReadShort();
            usercmd.impulse = (byte) ReadSignedChar();
            usercmd.flags = ReadByte();
            usercmd.sequence = ReadInt();
        }

        public void ReadContactInfo(contactInfo_t contactInfo) {
            contactInfo.type = contactType_t.values()[ReadInt()];
            ReadVec3(contactInfo.point);
            ReadVec3(contactInfo.normal);
            contactInfo.dist = ReadFloat();
            contactInfo.contents = ReadInt();
            ReadMaterial(contactInfo.material);
            contactInfo.modelFeature = ReadInt();
            contactInfo.trmFeature = ReadInt();
            contactInfo.entityNum = ReadInt();
            contactInfo.id = ReadInt();
        }

        public void ReadTrace(trace_s trace) {
            trace.fraction = ReadFloat();
            ReadVec3(trace.endpos);
            ReadMat3(trace.endAxis);
            ReadContactInfo(trace.c);
        }

        public void ReadTraceModel(idTraceModel trace) {
            int j, k;

            trace.type = traceModel_t.values()[ReadInt()];
            trace.numVerts = ReadInt();
            for (j = 0; j < MAX_TRACEMODEL_VERTS; j++) {
                ReadVec3(trace.verts[j]);
            }
            trace.numEdges = ReadInt();
            for (j = 0; j < (MAX_TRACEMODEL_EDGES + 1); j++) {
                trace.edges[j].v[0] = ReadInt();
                trace.edges[j].v[1] = ReadInt();
                ReadVec3(trace.edges[j].normal);
            }
            trace.numPolys = ReadInt();
            for (j = 0; j < MAX_TRACEMODEL_POLYS; j++) {
                ReadVec3(trace.polys[j].normal);
                trace.polys[j].dist = ReadFloat();
                ReadBounds(trace.polys[j].bounds);
                trace.polys[j].numEdges = ReadInt();
                for (k = 0; k < MAX_TRACEMODEL_POLYEDGES; k++) {
                    trace.polys[j].edges[k] = ReadInt();
                }
            }
            ReadVec3(trace.offset);
            ReadBounds(trace.bounds);
            trace.isConvex = ReadBool();
            // padding win32 native structs
            ByteBuffer tmp = ByteBuffer.allocate(3 * 2);
            file.Read(tmp, 3);
        }

        public void ReadClipModel(idClipModel clipModel) {
            boolean restoreClipModel;

            restoreClipModel = ReadBool();
            if (restoreClipModel) {
//                clipModel.oSet(new idClipModel());
                clipModel.Restore(this);
            } else {
                clipModel.oSet(null);//TODO:
            }
        }

        public void ReadSoundCommands() {
            gameSoundWorld.StopAllSounds();
            gameSoundWorld.ReadFromSaveGame(file);
        }

        public void ReadBuildNumber() {
            int[] buildNumber = {0};
            file.ReadInt(buildNumber);
            this.buildNumber = buildNumber[0];
        }

        //						Used to retrieve the saved game buildNumber from within class Restore methods
        public int GetBuildNumber() {
            return buildNumber;
        }

        private void CallRestore_r(final idTypeInfo cls, idClass obj) {
            if (cls.zuper != null) {
                CallRestore_r(cls.zuper, obj);
                if (cls.zuper.Restore == cls.Restore) {
                    // don't call save on this inheritance level since the function was called in the super class
                    return;
                }
            }
//            (obj.cls.Restore) (this);
            cls.Restore.run(this);
        }

        private void CallRestore_r(final java.lang.Class/*idTypeInfo*/ cls, idClass obj) {
            throw new TODO_Exception();
        }

    };
}
