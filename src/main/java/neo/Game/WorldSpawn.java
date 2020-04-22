package neo.Game;

import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.Class.EV_SafeRemove;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.GameSys.SysCvar.pm_stamina;
import static neo.Game.Game_local.DEFAULT_GRAVITY;
import static neo.Game.Game_local.gameLocal;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Text.Str.va;

import java.util.HashMap;
import java.util.Map;

import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;

/*
 game_worldspawn.cpp

 Worldspawn class.  Each map has one worldspawn which handles global spawnargs.

 */
public class WorldSpawn {

    /*
     ===============================================================================

     World entity.

     Every map should have exactly one worldspawn.

     ===============================================================================
     */
    public static class idWorldspawn extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE( idWorldspawn );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Remove, (eventCallback_t0<idWorldspawn>) idWorldspawn::Event_Remove);
            eventCallbacks.put(EV_SafeRemove, (eventCallback_t0<idWorldspawn>) idWorldspawn::Event_Remove);
        }



        //					~idWorldspawn();
        @Override
        public void Spawn() {
            super.Spawn();

            idStr scriptname;
            idThread thread;
            function_t func;
            idKeyValue kv;

            assert (gameLocal.world == null);
            gameLocal.world = this;

            g_gravity.SetFloat(this.spawnArgs.GetFloat("gravity", va("%f", DEFAULT_GRAVITY)));

            // disable stamina on hell levels
            if (this.spawnArgs.GetBool("no_stamina")) {
                pm_stamina.SetFloat(0.0f);
            }

            // load script
            scriptname = new idStr(gameLocal.GetMapName());
            scriptname.SetFileExtension(".script");
            if (fileSystem.ReadFile(scriptname.getData(), null, null) > 0) {
                gameLocal.program.CompileFile(scriptname.getData());

                // call the main function by default
                func = gameLocal.program.FindFunction("main");
                if (func != null) {
                    thread = new idThread(func);
                    thread.DelayedStart(0);
                }
            }

            // call any functions specified in worldspawn
            kv = this.spawnArgs.MatchPrefix("call");
            while (kv != null) {
                func = gameLocal.program.FindFunction(kv.GetValue().getData());
                if (func == null) {
                    idGameLocal.Error("Function '%s' not found in script for '%s' key on worldspawn", kv.GetValue(), kv.GetKey());
                }

                thread = new idThread(func);
                thread.DelayedStart(0);
                kv = this.spawnArgs.MatchPrefix("call", kv);
            }
        }

        public void Save(idRestoreGame savefile) {
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            assert (gameLocal.world.equals(this));

            g_gravity.SetFloat(this.spawnArgs.GetFloat("gravity", va("%f", DEFAULT_GRAVITY)));

            // disable stamina on hell levels
            if (this.spawnArgs.GetBool("no_stamina")) {
                pm_stamina.SetFloat(0.0f);
            }
        }

        @Override
        public void Event_Remove() {
            idGameLocal.Error("Tried to remove world");
        }

        @Override
        public java.lang.Class<?>/*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t<?> getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }
}
