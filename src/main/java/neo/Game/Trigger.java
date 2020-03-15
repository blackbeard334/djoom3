package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_ActivateTargets;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Renderer.Material.CONTENTS_FLASHLIGHT_TRIGGER;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.TempDump.NOT;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorOrange;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Player.idPlayer;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Trigger {

    /*
     ===============================================================================

     Trigger base.

     ===============================================================================
     */
    public static final idEventDef EV_Enable        = new idEventDef("enable", null);
    public static final idEventDef EV_Disable       = new idEventDef("disable", null);
    //
    public static final idEventDef EV_TriggerAction = new idEventDef("<triggerAction>", "e");
    //
    public static final idEventDef EV_Timer         = new idEventDef("<timer>", null);

    public static class idTrigger extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTrigger );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Enable, (eventCallback_t0<idTrigger>) idTrigger::Event_Enable);
            eventCallbacks.put(EV_Disable, (eventCallback_t0<idTrigger>) idTrigger::Event_Disable);

        }


        protected function_t scriptFunction;
        //
        //

        public idTrigger() {
            scriptFunction = null;
        }
        
        public static void DrawDebugInfo(){
            idMat3 axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();
            idVec3 up = axis.oGet(2).oMultiply(5.0f);
            idBounds viewTextBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            idBounds viewBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            idBounds box = new idBounds(new idVec3(-4.0f, -4.0f, -4.0f), new idVec3(4.0f, 4.0f, 4.0f));
            idEntity ent;
            idEntity target;
            int i;
            boolean show;
            function_t func;

            viewTextBounds.ExpandSelf(128.0f);
            viewBounds.ExpandSelf(512.0f);
            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if ((ent.GetPhysics().GetContents() & (CONTENTS_TRIGGER | CONTENTS_FLASHLIGHT_TRIGGER)) != 0) {
                    show = viewBounds.IntersectsBounds(ent.GetPhysics().GetAbsBounds());
                    if (!show) {
                        for (i = 0; i < ent.targets.Num(); i++) {
                            target = ent.targets.oGet(i).GetEntity();
                            if (target != null && viewBounds.IntersectsBounds(target.GetPhysics().GetAbsBounds())) {
                                show = true;
                                break;
                            }
                        }
                    }

                    if (!show) {
                        continue;
                    }

                    gameRenderWorld.DebugBounds(colorOrange, ent.GetPhysics().GetAbsBounds());
                    if (viewTextBounds.IntersectsBounds(ent.GetPhysics().GetAbsBounds())) {
                        gameRenderWorld.DrawText(ent.name.toString(), ent.GetPhysics().GetAbsBounds().GetCenter(), 0.1f, colorWhite, axis, 1);
                        gameRenderWorld.DrawText(ent.GetEntityDefName(), ent.GetPhysics().GetAbsBounds().GetCenter().oPlus(up), 0.1f, colorWhite, axis, 1);
                        if (ent.IsType(idTrigger.class)) {
                            func = ((idTrigger) ent).GetScriptFunction();
                        } else {
                            func = null;
                        }

                        if (func != null) {
                            gameRenderWorld.DrawText(va("call script '%s'", func.Name()), ent.GetPhysics().GetAbsBounds().GetCenter().oMinus(up), 0.1f, colorWhite, axis, 1);
                        }
                    }

                    for (i = 0; i < ent.targets.Num(); i++) {
                        target = ent.targets.oGet(i).GetEntity();
                        if (target != null) {
                            gameRenderWorld.DebugArrow(colorYellow, ent.GetPhysics().GetAbsBounds().GetCenter(), target.GetPhysics().GetOrigin(), 10, 0);
                            gameRenderWorld.DebugBounds(colorGreen, box, target.GetPhysics().GetOrigin());
                            if (viewTextBounds.IntersectsBounds(target.GetPhysics().GetAbsBounds())) {
                                gameRenderWorld.DrawText(target.name.toString(), target.GetPhysics().GetAbsBounds().GetCenter(), 0.1f, colorWhite, axis, 1);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            GetPhysics().SetContents(CONTENTS_TRIGGER);

            String funcname = spawnArgs.GetString("call", "");
            if (funcname.length() != 0) {
                scriptFunction = gameLocal.program.FindFunction(funcname);
                if (scriptFunction == null) {
                    gameLocal.Warning("trigger '%s' at (%s) calls unknown function '%s'", name, GetPhysics().GetOrigin().ToString(0), funcname);
                }
            } else {
                scriptFunction = null;
            }
        }

        public function_t GetScriptFunction() {
            return scriptFunction;
        }

        @Override
        public void Save(idSaveGame savefile) {
            if (scriptFunction != null) {
                savefile.WriteString(scriptFunction.Name());
            } else {
                savefile.WriteString("");
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            idStr funcname = new idStr();
            savefile.ReadString(funcname);
            if (!funcname.IsEmpty()) {
                scriptFunction = gameLocal.program.FindFunction(funcname.toString());
                if (scriptFunction == null) {
                    gameLocal.Warning("idTrigger_Multi '%s' at (%s) calls unknown function '%s'", name, GetPhysics().GetOrigin().ToString(0), funcname.toString());
                }
            } else {
                scriptFunction = null;
            }
        }

        public void Enable() {
            GetPhysics().SetContents(CONTENTS_TRIGGER);
            GetPhysics().EnableClip();
        }

        public void Disable() {
            // we may be relinked if we're bound to another object, so clear the contents as well
            GetPhysics().SetContents(0);
            GetPhysics().DisableClip();
        }

        protected void CallScript() {
            idThread thread;

            if (scriptFunction != null) {
                thread = new idThread(scriptFunction);
                thread.DelayedStart(0);
            }
        }

        protected void Event_Enable() {
            Enable();
        }

        protected void Event_Disable() {
            Disable();
        }

        public final void idEntity_Think() {
            super.Think();
        }

        @Override
        public java.lang.Class/*idTypeInfo*/ GetType() {
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
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };

    /*
     ===============================================================================

     Trigger which can be activated multiple times.

     ===============================================================================
     */
    public static class idTrigger_Multi extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTrigger_Multi );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idTrigger_Multi>) idTrigger_Multi::Event_Touch);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Multi>) idTrigger_Multi::Event_Trigger);
            eventCallbacks.put(EV_TriggerAction, (eventCallback_t1<idTrigger_Multi>) idTrigger_Multi::Event_TriggerAction);
        }


        private float   wait;
        private float   random;
        private float   delay;
        private float   random_delay;
        private int     nextTriggerTime;
        private idStr   requires = new idStr();
        private int     removeItem;
        private boolean touchClient;
        private boolean touchOther;
        private boolean triggerFirst;
        private boolean triggerWithSelf;
        //
        //

        public idTrigger_Multi() {
            wait = 0.0f;
            random = 0.0f;
            delay = 0.0f;
            random_delay = 0.0f;
            nextTriggerTime = 0;
            removeItem = 0;
            touchClient = false;
            touchOther = false;
            triggerFirst = false;
            triggerWithSelf = false;
        }

        /*
         ================
         idTrigger_Multi::Spawn

         "wait" : Seconds between triggerings, 0.5 default, -1 = one time only.
         "call" : Script function to call when triggered
         "random"	wait variance, default is 0
         Variable sized repeatable trigger.  Must be targeted at one or more entities.
         so, the basic time between firing is a random time between
         (wait - random) and (wait + random)
         ================
         */
        @Override
        public void Spawn() {
            super.Spawn();
            
            wait = spawnArgs.GetFloat("wait", "0.5");
            random = spawnArgs.GetFloat("random", "0");
            delay = spawnArgs.GetFloat("delay", "0");
            random_delay = spawnArgs.GetFloat("random_delay", "0");

            if (random != 0 && (random >= wait) && (wait >= 0)) {
                random = wait - 1;
                gameLocal.Warning("idTrigger_Multi '%s' at (%s) has random >= wait", name, GetPhysics().GetOrigin().ToString(0));
            }

            if (random_delay != 0 && (random_delay >= delay) && (delay >= 0)) {
                random_delay = delay - 1;
                gameLocal.Warning("idTrigger_Multi '%s' at (%s) has random_delay >= delay", name, GetPhysics().GetOrigin().ToString(0));
            }

            spawnArgs.GetString("requires", "", requires);
            removeItem = spawnArgs.GetInt("removeItem", "0");
            triggerFirst = spawnArgs.GetBool("triggerFirst", "0");
            triggerWithSelf = spawnArgs.GetBool("triggerWithSelf", "0");

            if (spawnArgs.GetBool("anyTouch")) {
                touchClient = true;
                touchOther = true;
            } else if (spawnArgs.GetBool("noTouch")) {
                touchClient = false;
                touchOther = false;
            } else if (spawnArgs.GetBool("noClient")) {
                touchClient = false;
                touchOther = true;
            } else {
                touchClient = true;
                touchOther = false;
            }

            nextTriggerTime = 0;

            if (spawnArgs.GetBool("flashlight_trigger")) {
                GetPhysics().SetContents(CONTENTS_FLASHLIGHT_TRIGGER);
            } else {
                GetPhysics().SetContents(CONTENTS_TRIGGER);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(wait);
            savefile.WriteFloat(random);
            savefile.WriteFloat(delay);
            savefile.WriteFloat(random_delay);
            savefile.WriteInt(nextTriggerTime);
            savefile.WriteString(requires);
            savefile.WriteInt(removeItem);
            savefile.WriteBool(touchClient);
            savefile.WriteBool(touchOther);
            savefile.WriteBool(triggerFirst);
            savefile.WriteBool(triggerWithSelf);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            wait = savefile.ReadFloat();
            random = savefile.ReadFloat();
            delay = savefile.ReadFloat();
            random_delay = savefile.ReadFloat();
            nextTriggerTime = savefile.ReadInt();
            savefile.ReadString(requires);
            removeItem = savefile.ReadInt();
            touchClient = savefile.ReadBool();
            touchOther = savefile.ReadBool();
            triggerFirst = savefile.ReadBool();
            triggerWithSelf = savefile.ReadBool();
        }

        private boolean CheckFacing(idEntity activator) {
            if (spawnArgs.GetBool("facing")) {
                if (!activator.IsType(idPlayer.class)) {
                    return true;
                }
                idPlayer player = (idPlayer) activator;
                float dot = player.viewAngles.ToForward().oMultiply(GetPhysics().GetAxis().oGet(0));
                float angle = RAD2DEG(idMath.ACos(dot));
                if (angle > spawnArgs.GetFloat("angleLimit", "30")) {
                    return false;
                }
            }
            return true;
        }

        private void TriggerAction(idEntity activator) {
            ActivateTargets(triggerWithSelf ? this : activator);
            CallScript();

            if (wait >= 0) {
                nextTriggerTime = (int) (gameLocal.time + SEC2MS(wait + random * gameLocal.random.CRandomFloat()));
            } else {
                // we can't just remove (this) here, because this is a touch function
                // called while looping through area links...
                nextTriggerTime = gameLocal.time + 1;
                PostEventMS(EV_Remove, 0);
            }
        }

        private void Event_TriggerAction(idEventArg<idEntity> activator) {
            TriggerAction(activator.value);
        }

        /*
         ================
         idTrigger_Multi::Event_Trigger

         the trigger was just activated
         activated should be the entity that originated the activation sequence (ie. the original target)
         activator should be set to the activator so it can be held through a delay
         so wait for the delay time before firing
         ================
         */
        private void Event_Trigger(idEventArg<idEntity> _activator) {
            idEntity activator = _activator.value;
            if (nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            // see if this trigger requires an item
            if (!gameLocal.RequirementMet(activator, requires, removeItem)) {
                return;
            }

            if (!CheckFacing(activator)) {
                return;
            }

            if (triggerFirst) {
                triggerFirst = false;
                return;
            }

            // don't allow it to trigger twice in a single frame
            nextTriggerTime = gameLocal.time + 1;

            if (delay > 0) {
                // don't allow it to trigger again until our delay has passed
                nextTriggerTime += SEC2MS(delay + random_delay * gameLocal.random.CRandomFloat());
                PostEventSec(EV_TriggerAction, delay, _activator);
            } else {
                TriggerAction(activator);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            if (triggerFirst) {
                return;
            }

            boolean player = other.IsType(idPlayer.class);
            if (player) {
                if (!touchClient) {
                    return;
                }
                if (((idPlayer) other).spectating) {
                    return;
                }
            } else if (!touchOther) {
                return;
            }

            if (nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            // see if this trigger requires an item
            if (!gameLocal.RequirementMet(other, requires, removeItem)) {
                return;
            }

            if (!CheckFacing(other)) {
                return;
            }

            if (spawnArgs.GetBool("toggleTriggerFirst")) {
                triggerFirst = true;
            }

            nextTriggerTime = gameLocal.time + 1;
            if (delay > 0) {
                // don't allow it to trigger again until our delay has passed
                nextTriggerTime += SEC2MS(delay + random_delay * gameLocal.random.CRandomFloat());
                PostEventSec(EV_TriggerAction, delay, other);
            } else {
                TriggerAction(other);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which can only be activated by an entity with a specific name.

     ===============================================================================
     */
    public static class idTrigger_EntityName extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//CLASS_PROTOTYPE(idTrigger_EntityName );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idTrigger_EntityName>) idTrigger_EntityName::Event_Touch);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_EntityName>) idTrigger_EntityName::Event_Trigger);
            eventCallbacks.put(EV_TriggerAction, (eventCallback_t1<idTrigger_EntityName>) idTrigger_EntityName::Event_TriggerAction);
        }


        private float   wait;
        private float   random;
        private float   delay;
        private float   random_delay;
        private int     nextTriggerTime;
        private boolean triggerFirst;
        private idStr   entityName = new idStr();
        //
        //

        public idTrigger_EntityName() {
            wait = 0.0f;
            random = 0.0f;
            delay = 0.0f;
            random_delay = 0.0f;
            nextTriggerTime = 0;
            triggerFirst = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(wait);
            savefile.WriteFloat(random);
            savefile.WriteFloat(delay);
            savefile.WriteFloat(random_delay);
            savefile.WriteInt(nextTriggerTime);
            savefile.WriteBool(triggerFirst);
            savefile.WriteString(entityName);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            wait = savefile.ReadFloat();
            random = savefile.ReadFloat();
            delay = savefile.ReadFloat();
            random_delay = savefile.ReadFloat();
            nextTriggerTime = savefile.ReadInt();
            triggerFirst = savefile.ReadBool();
            savefile.ReadString(entityName);
        }

        @Override
        public void Spawn() {
            wait = spawnArgs.GetFloat("wait", "0.5");
            random = spawnArgs.GetFloat("random", "0");
            delay = spawnArgs.GetFloat("delay", "0");
            random_delay = spawnArgs.GetFloat("random_delay", "0");

            if (random != 0 && (random >= wait) && (wait >= 0)) {
                random = wait - 1;
                gameLocal.Warning("idTrigger_EntityName '%s' at (%s) has random >= wait", name, GetPhysics().GetOrigin().ToString(0));
            }

            if (random_delay != 0 && (random_delay >= delay) && (delay >= 0)) {
                random_delay = delay - 1;
                gameLocal.Warning("idTrigger_EntityName '%s' at (%s) has random_delay >= delay", name, GetPhysics().GetOrigin().ToString(0));
            }

            triggerFirst = spawnArgs.GetBool("triggerFirst", "0");

            entityName.oSet(spawnArgs.GetString("entityname"));
            if (NOT(entityName.Length())) {
                gameLocal.Error("idTrigger_EntityName '%s' at (%s) doesn't have 'entityname' key specified", name, GetPhysics().GetOrigin().ToString(0));
            }

            nextTriggerTime = 0;

            if (!spawnArgs.GetBool("noTouch")) {
                GetPhysics().SetContents(CONTENTS_TRIGGER);
            }
        }

        private void TriggerAction(idEntity activator) {
            ActivateTargets(activator);
            CallScript();

            if (wait >= 0) {
                nextTriggerTime = (int) (gameLocal.time + SEC2MS(wait + random * gameLocal.random.CRandomFloat()));
            } else {
                // we can't just remove (this) here, because this is a touch function
                // called while looping through area links...
                nextTriggerTime = gameLocal.time + 1;
                PostEventMS(EV_Remove, 0);
            }
        }

        private void Event_TriggerAction(idEventArg<idEntity> activator) {
            TriggerAction(activator.value);
        }


        /*
         ================
         idTrigger_EntityName::Event_Trigger

         the trigger was just activated
         activated should be the entity that originated the activation sequence (ie. the original target)
         activator should be set to the activator so it can be held through a delay
         so wait for the delay time before firing
         ================
         */ private void Event_Trigger(idEventArg<idEntity> _activator) {
            idEntity activator = _activator.value;
            if (nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            if (null == activator || (!activator.name.equals(entityName))) {
                return;
            }

            if (triggerFirst) {
                triggerFirst = false;
                return;
            }

            // don't allow it to trigger twice in a single frame
            nextTriggerTime = gameLocal.time + 1;

            if (delay > 0) {
                // don't allow it to trigger again until our delay has passed
                nextTriggerTime += SEC2MS(delay + random_delay * gameLocal.random.CRandomFloat());
                PostEventSec(EV_TriggerAction, delay, activator);
            } else {
                TriggerAction(activator);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            if (triggerFirst) {
                return;
            }

            if (nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            if (null == other || (other.name != entityName)) {
                return;
            }

            nextTriggerTime = gameLocal.time + 1;
            if (delay > 0) {
                // don't allow it to trigger again until our delay has passed
                nextTriggerTime += SEC2MS(delay + random_delay * gameLocal.random.CRandomFloat());
                PostEventSec(EV_TriggerAction, delay, other);
            } else {
                TriggerAction(other);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which repeatedly fires targets.

     ===============================================================================
     */
    public static class idTrigger_Timer extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE(idTrigger_Timer );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Timer, (eventCallback_t0<idTrigger_Timer>) idTrigger_Timer::Event_Timer);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Timer>) idTrigger_Timer::Event_Use);
        }


        private float   random;
        private float   wait;
        private boolean on;
        private float   delay;
        private idStr   onName  = new idStr();
        private idStr   offName = new idStr();
        //
        //

        public idTrigger_Timer() {
            random = 0.0f;
            wait = 0.0f;
            on = false;
            delay = 0.0f;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(random);
            savefile.WriteFloat(wait);
            savefile.WriteBool(on);
            savefile.WriteFloat(delay);
            savefile.WriteString(onName);
            savefile.WriteString(offName);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            random = savefile.ReadFloat();
            wait = savefile.ReadFloat();
            on = savefile.ReadBool();
            delay = savefile.ReadFloat();
            savefile.ReadString(onName);
            savefile.ReadString(offName);
        }

        /*
         ================
         idTrigger_Timer::Spawn

         Repeatedly fires its targets.
         Can be turned on or off by using.
         ================
         */
        @Override
        public void Spawn() {
            super.Spawn();

            random = spawnArgs.GetFloat("random", "1");
            wait = spawnArgs.GetFloat("wait", "1");
            on = spawnArgs.GetBool("start_on", "0");
            delay = spawnArgs.GetFloat("delay", "0");
            onName.oSet(spawnArgs.GetString("onName"));
            offName.oSet(spawnArgs.GetString("offName"));

            if (random >= wait && wait >= 0) {
                random = wait - 0.001f;
                gameLocal.Warning("idTrigger_Timer '%s' at (%s) has random >= wait", name, GetPhysics().GetOrigin().ToString(0));
            }

            if (on) {
                PostEventSec(EV_Timer, delay);
            }
        }

        @Override
        public void Enable() {
            // if off, turn it on
            if (!on) {
                on = true;
                PostEventSec(EV_Timer, delay);
            }
        }

        @Override
        public void Disable() {
            // if on, turn it off
            if (on) {
                on = false;
                CancelEvents(EV_Timer);
            }
        }

        private void Event_Timer() {
            ActivateTargets(this);

            // set time before next firing
            if (wait >= 0.0f) {
                PostEventSec(EV_Timer, wait + gameLocal.random.CRandomFloat() * random);
            }
        }

        private void Event_Use(idEventArg<idEntity> _activator) {
            idEntity activator = _activator.value;
            // if on, turn it off
            if (on) {
                if (offName.Length() != 0 && offName.Icmp(activator.GetName()) != 0) {
                    return;
                }
                on = false;
                CancelEvents(EV_Timer);
            } else {
                // turn it on
                if (onName.Length() != 0 && onName.Icmp(activator.GetName()) != 0) {
                    return;
                }
                on = true;
                PostEventSec(EV_Timer, delay);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which fires targets after being activated a specific number of times.

     ===============================================================================
     */
    public static class idTrigger_Count extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE(idTrigger_Count );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Count>) idTrigger_Count::Event_Trigger);
            eventCallbacks.put(EV_TriggerAction, (eventCallback_t1<idTrigger_Count>) idTrigger_Count::Event_TriggerAction);
        }


        private int   goal;
        private int   count;
        private float delay;
        //
        //

        public idTrigger_Count() {
            goal = 0;
            count = 0;
            delay = 0.0f;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(goal);
            savefile.WriteInt(count);
            savefile.WriteFloat(delay);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            goal = savefile.ReadInt();
            count = savefile.ReadInt();
            delay = savefile.ReadFloat();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            goal = spawnArgs.GetInt("count", "1");
            delay = spawnArgs.GetFloat("delay", "0");
            count = 0;
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            // goal of -1 means trigger has been exhausted
            if (goal >= 0) {
                count++;
                if (count >= goal) {
                    if (spawnArgs.GetBool("repeat")) {
                        count = 0;
                    } else {
                        goal = -1;
                    }
                    PostEventSec(EV_TriggerAction, delay, activator.value);
                }
            }
        }

        private void Event_TriggerAction(idEventArg<idEntity> activator) {
            ActivateTargets(activator.value);
            CallScript();
            if (goal == -1) {
                PostEventMS(EV_Remove, 0);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which hurts touching entities.

     ===============================================================================
     */
    static class idTrigger_Hurt extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE(idTrigger_Hurt );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idTrigger_Hurt>) idTrigger_Hurt::Event_Touch);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Hurt>) idTrigger_Hurt::Event_Toggle);
        }


        private boolean on;
        private float   delay;
        private int     nextTime;
        //
        //

        public idTrigger_Hurt() {
            on = false;
            delay = 0.0f;
            nextTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(on);
            savefile.WriteFloat(delay);
            savefile.WriteInt(nextTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            on = savefile.ReadBool();
            delay = savefile.ReadFloat();
            nextTime = savefile.ReadInt();
        }

        /*
         ================
         idTrigger_Hurt::Spawn

         Damages activator
         Can be turned on or off by using.
         ================
         */
        @Override
        public void Spawn() {
            super.Spawn();
            
            on = spawnArgs.GetBool("on", "1");
            delay = spawnArgs.GetFloat("delay", "1.0");
            nextTime = gameLocal.time;
            Enable();
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            final String damage;

            if (on && other != null && gameLocal.time >= nextTime) {
                damage = spawnArgs.GetString("def_damage", "damage_painTrigger");
                other.Damage(null, null, getVec3_origin(), damage, 1.0f, INVALID_JOINT);

                ActivateTargets(other);
                CallScript();

                nextTime = (int) (gameLocal.time + SEC2MS(delay));
            }
        }

        private void Event_Toggle(idEventArg<idEntity> activator) {
            on = !on;
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which fades the player view.

     ===============================================================================
     */
    public static class idTrigger_Fade extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTrigger_Fade );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Fade>) idTrigger_Fade::Event_Trigger);
        }


        private void Event_Trigger(idEventArg<idEntity> activator) {
            idVec4 fadeColor;
            int fadeTime;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor = spawnArgs.GetVec4("fadeColor", "0, 0, 0, 1");
                fadeTime = (int) SEC2MS(spawnArgs.GetFloat("fadeTime", "0.5"));
                player.playerView.Fade(fadeColor, fadeTime);
                PostEventMS(EV_ActivateTargets, fadeTime, activator.value);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };


    /*
     ===============================================================================

     Trigger which continuously tests whether other entities are touching it.

     ===============================================================================
     */
    public static class idTrigger_Touch extends idTrigger {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTrigger_Touch );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTrigger.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTrigger_Touch>) idTrigger_Touch::Event_Trigger);
        }


        private idClipModel clipModel;
        //
        //

        public idTrigger_Touch() {
            clipModel = null;
        }

        @Override
        public void Spawn() {
            // get the clip model
            clipModel = new idClipModel(GetPhysics().GetClipModel());

            // remove the collision model from the physics object
            GetPhysics().SetClipModel(null, 1.0f);

            if (spawnArgs.GetBool("start_on")) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Think() {
            if ((thinkFlags & TH_THINK) != 0) {
                TouchEntities();
            }
            idEntity_Think();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteClipModel(clipModel);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadClipModel(clipModel);
        }

        @Override
        public void Enable() {
            BecomeActive(TH_THINK);
        }

        @Override
        public void Disable() {
            BecomeInactive(TH_THINK);
        }

        public void TouchEntities() {
            int numClipModels, i;
            idBounds bounds = new idBounds();
            idClipModel cm;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];

            if (clipModel == null || scriptFunction == null) {
                return;
            }

            bounds.FromTransformedBounds(clipModel.GetBounds(), clipModel.GetOrigin(), clipModel.GetAxis());
            numClipModels = gameLocal.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            for (i = 0; i < numClipModels; i++) {
                cm = clipModelList[i];

                if (!cm.IsTraceModel()) {
                    continue;
                }

                idEntity entity = cm.GetEntity();

                if (null == entity) {
                    continue;
                }

                if (NOT(gameLocal.clip.ContentsModel(cm.GetOrigin(), cm, cm.GetAxis(), -1,
                        clipModel.Handle(), clipModel.GetOrigin(), clipModel.GetAxis()))) {
                    continue;
                }

                ActivateTargets(entity);

                idThread thread = new idThread();
                thread.CallFunction(entity, scriptFunction, false);
                thread.DelayedStart(0);
            }
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            if ((thinkFlags & TH_THINK) != 0) {
                BecomeInactive(TH_THINK);
            } else {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };
}
