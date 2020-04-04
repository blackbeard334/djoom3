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
            this.scriptFunction = null;
        }
        
        public static void DrawDebugInfo(){
            final idMat3 axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();
            final idVec3 up = axis.oGet(2).oMultiply(5.0f);
            final idBounds viewTextBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            final idBounds viewBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            final idBounds box = new idBounds(new idVec3(-4.0f, -4.0f, -4.0f), new idVec3(4.0f, 4.0f, 4.0f));
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
                            if ((target != null) && viewBounds.IntersectsBounds(target.GetPhysics().GetAbsBounds())) {
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
                        gameRenderWorld.DrawText(ent.name.getData(), ent.GetPhysics().GetAbsBounds().GetCenter(), 0.1f, colorWhite, axis, 1);
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
                                gameRenderWorld.DrawText(target.name.getData(), target.GetPhysics().GetAbsBounds().GetCenter(), 0.1f, colorWhite, axis, 1);
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

            final String funcname = this.spawnArgs.GetString("call", "");
            if (funcname.length() != 0) {
                this.scriptFunction = gameLocal.program.FindFunction(funcname);
                if (this.scriptFunction == null) {
                    gameLocal.Warning("trigger '%s' at (%s) calls unknown function '%s'", this.name, GetPhysics().GetOrigin().ToString(0), funcname);
                }
            } else {
                this.scriptFunction = null;
            }
        }

        public function_t GetScriptFunction() {
            return this.scriptFunction;
        }

        @Override
        public void Save(idSaveGame savefile) {
            if (this.scriptFunction != null) {
                savefile.WriteString(this.scriptFunction.Name());
            } else {
                savefile.WriteString("");
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final idStr funcname = new idStr();
            savefile.ReadString(funcname);
            if (!funcname.IsEmpty()) {
                this.scriptFunction = gameLocal.program.FindFunction(funcname.getData());
                if (this.scriptFunction == null) {
                    gameLocal.Warning("idTrigger_Multi '%s' at (%s) calls unknown function '%s'", this.name, GetPhysics().GetOrigin().ToString(0), funcname.getData());
                }
            } else {
                this.scriptFunction = null;
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

            if (this.scriptFunction != null) {
                thread = new idThread(this.scriptFunction);
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

    }

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
        private final idStr   requires = new idStr();
        private int     removeItem;
        private boolean touchClient;
        private boolean touchOther;
        private boolean triggerFirst;
        private boolean triggerWithSelf;
        //
        //

        public idTrigger_Multi() {
            this.wait = 0.0f;
            this.random = 0.0f;
            this.delay = 0.0f;
            this.random_delay = 0.0f;
            this.nextTriggerTime = 0;
            this.removeItem = 0;
            this.touchClient = false;
            this.touchOther = false;
            this.triggerFirst = false;
            this.triggerWithSelf = false;
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
            
            this.wait = this.spawnArgs.GetFloat("wait", "0.5");
            this.random = this.spawnArgs.GetFloat("random", "0");
            this.delay = this.spawnArgs.GetFloat("delay", "0");
            this.random_delay = this.spawnArgs.GetFloat("random_delay", "0");

            if ((this.random != 0) && (this.random >= this.wait) && (this.wait >= 0)) {
                this.random = this.wait - 1;
                gameLocal.Warning("idTrigger_Multi '%s' at (%s) has random >= wait", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            if ((this.random_delay != 0) && (this.random_delay >= this.delay) && (this.delay >= 0)) {
                this.random_delay = this.delay - 1;
                gameLocal.Warning("idTrigger_Multi '%s' at (%s) has random_delay >= delay", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            this.spawnArgs.GetString("requires", "", this.requires);
            this.removeItem = this.spawnArgs.GetInt("removeItem", "0");
            this.triggerFirst = this.spawnArgs.GetBool("triggerFirst", "0");
            this.triggerWithSelf = this.spawnArgs.GetBool("triggerWithSelf", "0");

            if (this.spawnArgs.GetBool("anyTouch")) {
                this.touchClient = true;
                this.touchOther = true;
            } else if (this.spawnArgs.GetBool("noTouch")) {
                this.touchClient = false;
                this.touchOther = false;
            } else if (this.spawnArgs.GetBool("noClient")) {
                this.touchClient = false;
                this.touchOther = true;
            } else {
                this.touchClient = true;
                this.touchOther = false;
            }

            this.nextTriggerTime = 0;

            if (this.spawnArgs.GetBool("flashlight_trigger")) {
                GetPhysics().SetContents(CONTENTS_FLASHLIGHT_TRIGGER);
            } else {
                GetPhysics().SetContents(CONTENTS_TRIGGER);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.wait);
            savefile.WriteFloat(this.random);
            savefile.WriteFloat(this.delay);
            savefile.WriteFloat(this.random_delay);
            savefile.WriteInt(this.nextTriggerTime);
            savefile.WriteString(this.requires);
            savefile.WriteInt(this.removeItem);
            savefile.WriteBool(this.touchClient);
            savefile.WriteBool(this.touchOther);
            savefile.WriteBool(this.triggerFirst);
            savefile.WriteBool(this.triggerWithSelf);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.wait = savefile.ReadFloat();
            this.random = savefile.ReadFloat();
            this.delay = savefile.ReadFloat();
            this.random_delay = savefile.ReadFloat();
            this.nextTriggerTime = savefile.ReadInt();
            savefile.ReadString(this.requires);
            this.removeItem = savefile.ReadInt();
            this.touchClient = savefile.ReadBool();
            this.touchOther = savefile.ReadBool();
            this.triggerFirst = savefile.ReadBool();
            this.triggerWithSelf = savefile.ReadBool();
        }

        private boolean CheckFacing(idEntity activator) {
            if (this.spawnArgs.GetBool("facing")) {
                if (!activator.IsType(idPlayer.class)) {
                    return true;
                }
                final idPlayer player = (idPlayer) activator;
                final float dot = player.viewAngles.ToForward().oMultiply(GetPhysics().GetAxis().oGet(0));
                final float angle = RAD2DEG(idMath.ACos(dot));
                if (angle > this.spawnArgs.GetFloat("angleLimit", "30")) {
                    return false;
                }
            }
            return true;
        }

        private void TriggerAction(idEntity activator) {
            ActivateTargets(this.triggerWithSelf ? this : activator);
            CallScript();

            if (this.wait >= 0) {
                this.nextTriggerTime = (int) (gameLocal.time + SEC2MS(this.wait + (this.random * gameLocal.random.CRandomFloat())));
            } else {
                // we can't just remove (this) here, because this is a touch function
                // called while looping through area links...
                this.nextTriggerTime = gameLocal.time + 1;
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
            final idEntity activator = _activator.value;
            if (this.nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            // see if this trigger requires an item
            if (!gameLocal.RequirementMet(activator, this.requires, this.removeItem)) {
                return;
            }

            if (!CheckFacing(activator)) {
                return;
            }

            if (this.triggerFirst) {
                this.triggerFirst = false;
                return;
            }

            // don't allow it to trigger twice in a single frame
            this.nextTriggerTime = gameLocal.time + 1;

            if (this.delay > 0) {
                // don't allow it to trigger again until our delay has passed
                this.nextTriggerTime += SEC2MS(this.delay + (this.random_delay * gameLocal.random.CRandomFloat()));
                PostEventSec(EV_TriggerAction, this.delay, _activator);
            } else {
                TriggerAction(activator);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            if (this.triggerFirst) {
                return;
            }

            final boolean player = other.IsType(idPlayer.class);
            if (player) {
                if (!this.touchClient) {
                    return;
                }
                if (((idPlayer) other).spectating) {
                    return;
                }
            } else if (!this.touchOther) {
                return;
            }

            if (this.nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            // see if this trigger requires an item
            if (!gameLocal.RequirementMet(other, this.requires, this.removeItem)) {
                return;
            }

            if (!CheckFacing(other)) {
                return;
            }

            if (this.spawnArgs.GetBool("toggleTriggerFirst")) {
                this.triggerFirst = true;
            }

            this.nextTriggerTime = gameLocal.time + 1;
            if (this.delay > 0) {
                // don't allow it to trigger again until our delay has passed
                this.nextTriggerTime += SEC2MS(this.delay + (this.random_delay * gameLocal.random.CRandomFloat()));
                PostEventSec(EV_TriggerAction, this.delay, other);
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

    }


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
        private final idStr   entityName = new idStr();
        //
        //

        public idTrigger_EntityName() {
            this.wait = 0.0f;
            this.random = 0.0f;
            this.delay = 0.0f;
            this.random_delay = 0.0f;
            this.nextTriggerTime = 0;
            this.triggerFirst = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.wait);
            savefile.WriteFloat(this.random);
            savefile.WriteFloat(this.delay);
            savefile.WriteFloat(this.random_delay);
            savefile.WriteInt(this.nextTriggerTime);
            savefile.WriteBool(this.triggerFirst);
            savefile.WriteString(this.entityName);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.wait = savefile.ReadFloat();
            this.random = savefile.ReadFloat();
            this.delay = savefile.ReadFloat();
            this.random_delay = savefile.ReadFloat();
            this.nextTriggerTime = savefile.ReadInt();
            this.triggerFirst = savefile.ReadBool();
            savefile.ReadString(this.entityName);
        }

        @Override
        public void Spawn() {
            this.wait = this.spawnArgs.GetFloat("wait", "0.5");
            this.random = this.spawnArgs.GetFloat("random", "0");
            this.delay = this.spawnArgs.GetFloat("delay", "0");
            this.random_delay = this.spawnArgs.GetFloat("random_delay", "0");

            if ((this.random != 0) && (this.random >= this.wait) && (this.wait >= 0)) {
                this.random = this.wait - 1;
                gameLocal.Warning("idTrigger_EntityName '%s' at (%s) has random >= wait", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            if ((this.random_delay != 0) && (this.random_delay >= this.delay) && (this.delay >= 0)) {
                this.random_delay = this.delay - 1;
                gameLocal.Warning("idTrigger_EntityName '%s' at (%s) has random_delay >= delay", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            this.triggerFirst = this.spawnArgs.GetBool("triggerFirst", "0");

            this.entityName.oSet(this.spawnArgs.GetString("entityname"));
            if (NOT(this.entityName.Length())) {
                gameLocal.Error("idTrigger_EntityName '%s' at (%s) doesn't have 'entityname' key specified", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            this.nextTriggerTime = 0;

            if (!this.spawnArgs.GetBool("noTouch")) {
                GetPhysics().SetContents(CONTENTS_TRIGGER);
            }
        }

        private void TriggerAction(idEntity activator) {
            ActivateTargets(activator);
            CallScript();

            if (this.wait >= 0) {
                this.nextTriggerTime = (int) (gameLocal.time + SEC2MS(this.wait + (this.random * gameLocal.random.CRandomFloat())));
            } else {
                // we can't just remove (this) here, because this is a touch function
                // called while looping through area links...
                this.nextTriggerTime = gameLocal.time + 1;
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
            final idEntity activator = _activator.value;
            if (this.nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            if ((null == activator) || (!activator.name.equals(this.entityName))) {
                return;
            }

            if (this.triggerFirst) {
                this.triggerFirst = false;
                return;
            }

            // don't allow it to trigger twice in a single frame
            this.nextTriggerTime = gameLocal.time + 1;

            if (this.delay > 0) {
                // don't allow it to trigger again until our delay has passed
                this.nextTriggerTime += SEC2MS(this.delay + (this.random_delay * gameLocal.random.CRandomFloat()));
                PostEventSec(EV_TriggerAction, this.delay, activator);
            } else {
                TriggerAction(activator);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            if (this.triggerFirst) {
                return;
            }

            if (this.nextTriggerTime > gameLocal.time) {
                // can't retrigger until the wait is over
                return;
            }

            if ((null == other) || (other.name != this.entityName)) {
                return;
            }

            this.nextTriggerTime = gameLocal.time + 1;
            if (this.delay > 0) {
                // don't allow it to trigger again until our delay has passed
                this.nextTriggerTime += SEC2MS(this.delay + (this.random_delay * gameLocal.random.CRandomFloat()));
                PostEventSec(EV_TriggerAction, this.delay, other);
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

    }


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
        private final idStr   onName  = new idStr();
        private final idStr   offName = new idStr();
        //
        //

        public idTrigger_Timer() {
            this.random = 0.0f;
            this.wait = 0.0f;
            this.on = false;
            this.delay = 0.0f;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.random);
            savefile.WriteFloat(this.wait);
            savefile.WriteBool(this.on);
            savefile.WriteFloat(this.delay);
            savefile.WriteString(this.onName);
            savefile.WriteString(this.offName);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.random = savefile.ReadFloat();
            this.wait = savefile.ReadFloat();
            this.on = savefile.ReadBool();
            this.delay = savefile.ReadFloat();
            savefile.ReadString(this.onName);
            savefile.ReadString(this.offName);
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

            this.random = this.spawnArgs.GetFloat("random", "1");
            this.wait = this.spawnArgs.GetFloat("wait", "1");
            this.on = this.spawnArgs.GetBool("start_on", "0");
            this.delay = this.spawnArgs.GetFloat("delay", "0");
            this.onName.oSet(this.spawnArgs.GetString("onName"));
            this.offName.oSet(this.spawnArgs.GetString("offName"));

            if ((this.random >= this.wait) && (this.wait >= 0)) {
                this.random = this.wait - 0.001f;
                gameLocal.Warning("idTrigger_Timer '%s' at (%s) has random >= wait", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            if (this.on) {
                PostEventSec(EV_Timer, this.delay);
            }
        }

        @Override
        public void Enable() {
            // if off, turn it on
            if (!this.on) {
                this.on = true;
                PostEventSec(EV_Timer, this.delay);
            }
        }

        @Override
        public void Disable() {
            // if on, turn it off
            if (this.on) {
                this.on = false;
                CancelEvents(EV_Timer);
            }
        }

        private void Event_Timer() {
            ActivateTargets(this);

            // set time before next firing
            if (this.wait >= 0.0f) {
                PostEventSec(EV_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
            }
        }

        private void Event_Use(idEventArg<idEntity> _activator) {
            final idEntity activator = _activator.value;
            // if on, turn it off
            if (this.on) {
                if ((this.offName.Length() != 0) && (this.offName.Icmp(activator.GetName()) != 0)) {
                    return;
                }
                this.on = false;
                CancelEvents(EV_Timer);
            } else {
                // turn it on
                if ((this.onName.Length() != 0) && (this.onName.Icmp(activator.GetName()) != 0)) {
                    return;
                }
                this.on = true;
                PostEventSec(EV_Timer, this.delay);
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

    }


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
            this.goal = 0;
            this.count = 0;
            this.delay = 0.0f;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.goal);
            savefile.WriteInt(this.count);
            savefile.WriteFloat(this.delay);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.goal = savefile.ReadInt();
            this.count = savefile.ReadInt();
            this.delay = savefile.ReadFloat();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.goal = this.spawnArgs.GetInt("count", "1");
            this.delay = this.spawnArgs.GetFloat("delay", "0");
            this.count = 0;
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            // goal of -1 means trigger has been exhausted
            if (this.goal >= 0) {
                this.count++;
                if (this.count >= this.goal) {
                    if (this.spawnArgs.GetBool("repeat")) {
                        this.count = 0;
                    } else {
                        this.goal = -1;
                    }
                    PostEventSec(EV_TriggerAction, this.delay, activator.value);
                }
            }
        }

        private void Event_TriggerAction(idEventArg<idEntity> activator) {
            ActivateTargets(activator.value);
            CallScript();
            if (this.goal == -1) {
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

    }


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
            this.on = false;
            this.delay = 0.0f;
            this.nextTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.on);
            savefile.WriteFloat(this.delay);
            savefile.WriteInt(this.nextTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.on = savefile.ReadBool();
            this.delay = savefile.ReadFloat();
            this.nextTime = savefile.ReadInt();
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
            
            this.on = this.spawnArgs.GetBool("on", "1");
            this.delay = this.spawnArgs.GetFloat("delay", "1.0");
            this.nextTime = gameLocal.time;
            Enable();
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            final String damage;

            if (this.on && (other != null) && (gameLocal.time >= this.nextTime)) {
                damage = this.spawnArgs.GetString("def_damage", "damage_painTrigger");
                other.Damage(null, null, getVec3_origin(), damage, 1.0f, INVALID_JOINT);

                ActivateTargets(other);
                CallScript();

                this.nextTime = (int) (gameLocal.time + SEC2MS(this.delay));
            }
        }

        private void Event_Toggle(idEventArg<idEntity> activator) {
            this.on = !this.on;
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

    }


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
                fadeColor = this.spawnArgs.GetVec4("fadeColor", "0, 0, 0, 1");
                fadeTime = (int) SEC2MS(this.spawnArgs.GetFloat("fadeTime", "0.5"));
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

    }


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
            this.clipModel = null;
        }

        @Override
        public void Spawn() {
            // get the clip model
            this.clipModel = new idClipModel(GetPhysics().GetClipModel());

            // remove the collision model from the physics object
            GetPhysics().SetClipModel(null, 1.0f);

            if (this.spawnArgs.GetBool("start_on")) {
                BecomeActive(TH_THINK);
            }
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                TouchEntities();
            }
            idEntity_Think();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteClipModel(this.clipModel);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadClipModel(this.clipModel);
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
            final idBounds bounds = new idBounds();
            idClipModel cm;
            final idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];

            if ((this.clipModel == null) || (this.scriptFunction == null)) {
                return;
            }

            bounds.FromTransformedBounds(this.clipModel.GetBounds(), this.clipModel.GetOrigin(), this.clipModel.GetAxis());
            numClipModels = gameLocal.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            for (i = 0; i < numClipModels; i++) {
                cm = clipModelList[i];

                if (!cm.IsTraceModel()) {
                    continue;
                }

                final idEntity entity = cm.GetEntity();

                if (null == entity) {
                    continue;
                }

                if (NOT(gameLocal.clip.ContentsModel(cm.GetOrigin(), cm, cm.GetAxis(), -1,
                        this.clipModel.Handle(), this.clipModel.GetOrigin(), this.clipModel.GetAxis()))) {
                    continue;
                }

                ActivateTargets(entity);

                final idThread thread = new idThread();
                thread.CallFunction(entity, this.scriptFunction, false);
                thread.DelayedStart(0);
            }
        }

        private void Event_Trigger(idEventArg<idEntity> activator) {
            if ((this.thinkFlags & TH_THINK) != 0) {
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

    }
}
