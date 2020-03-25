package neo.Game;

import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.SysCvar.g_showEntityInfo;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;

import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Light.idLight;
import neo.Game.Player.idPlayer;
import neo.Game.Pvs.pvsHandle_t;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Renderer.RenderWorld.renderView_s;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class SecurityCamera {

    /*
     ===================================================================================

     Security camera

     ===================================================================================
     */
    public static final idEventDef EV_SecurityCam_ReverseSweep  = new idEventDef("<reverseSweep>");
    public static final idEventDef EV_SecurityCam_ContinueSweep = new idEventDef("<continueSweep>");
    public static final idEventDef EV_SecurityCam_Pause         = new idEventDef("<pause>");
    public static final idEventDef EV_SecurityCam_Alert         = new idEventDef("<alert>");
    public static final idEventDef EV_SecurityCam_AddLight      = new idEventDef("<addLight>");

    public static class idSecurityCamera extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_SecurityCam_ReverseSweep, (eventCallback_t0<idSecurityCamera>) idSecurityCamera::Event_ReverseSweep);
            eventCallbacks.put(EV_SecurityCam_ContinueSweep, (eventCallback_t0<idSecurityCamera>) idSecurityCamera::Event_ContinueSweep);
            eventCallbacks.put(EV_SecurityCam_Pause, (eventCallback_t0<idSecurityCamera>) idSecurityCamera::Event_Pause);
            eventCallbacks.put(EV_SecurityCam_Alert, (eventCallback_t0<idSecurityCamera>) idSecurityCamera::Event_Alert);
            eventCallbacks.put(EV_SecurityCam_AddLight, (eventCallback_t0<idSecurityCamera>) idSecurityCamera::Event_AddLight);
        }

        private static final int SCANNING       = 0;
        private static final int LOSINGINTEREST = 1;
        private static final int ALERT          = 2;
        private static final int ACTIVATED      = 3;
        // enum { SCANNING, LOSINGINTEREST, ALERT, ACTIVATED };
        private float               angle;
        private float               sweepAngle;
        private int                 modelAxis;
        private boolean             flipAxis;
        private float               scanDist;
        private float               scanFov;
        //							
        private float               sweepStart;
        private float               sweepEnd;
        private boolean             negativeSweep;
        private boolean             sweeping;
        private int                 alertMode;
        private float               stopSweeping;
        private float               scanFovCos;
        //
        private idVec3              viewOffset;
        //							
        private int                 pvsArea;
        private idPhysics_RigidBody physicsObj;
        private idTraceModel        trm;
        //
        //

        // CLASS_PROTOTYPE( idSecurityCamera );
        @Override
        public void Spawn() {
            idStr str;

            this.sweepAngle = this.spawnArgs.GetFloat("sweepAngle", "90");
            this.health = this.spawnArgs.GetInt("health", "100");
            this.scanFov = this.spawnArgs.GetFloat("scanFov", "90");
            this.scanDist = this.spawnArgs.GetFloat("scanDist", "200");
            this.flipAxis = this.spawnArgs.GetBool("flipAxis");

            this.modelAxis = this.spawnArgs.GetInt("modelAxis");
            if ((this.modelAxis < 0) || (this.modelAxis > 2)) {
                this.modelAxis = 0;
            }

            this.spawnArgs.GetVector("viewOffset", "0 0 0", this.viewOffset);

            if (this.spawnArgs.GetBool("spotLight")) {
                PostEventMS(EV_SecurityCam_AddLight, 0);
            }

            this.negativeSweep = (this.sweepAngle < 0);
            this.sweepAngle = Math.abs(this.sweepAngle);

            this.scanFovCos = (float) Math.cos((this.scanFov * idMath.PI) / 360.0f);

            this.angle = GetPhysics().GetAxis().ToAngles().yaw;
            StartSweep();
            SetAlertMode(SCANNING);
            BecomeActive(TH_THINK);

            if (this.health != 0) {
                this.fl.takedamage = true;
            }

            this.pvsArea = gameLocal.pvs.GetPVSArea(GetPhysics().GetOrigin());
            // if no target specified use ourself
            str = new idStr(this.spawnArgs.GetString("cameraTarget"));
            if (str.Length() == 0) {
                this.spawnArgs.Set("cameraTarget", this.spawnArgs.GetString("name"));
            }

            // check if a clip model is set
            this.spawnArgs.GetString("clipmodel", "", str);
            if (!isNotNullOrEmpty(str)) {
                str.oSet(this.spawnArgs.GetString("model"));		// use the visual model
            }

            if (!CollisionModel_local.collisionModelManager.TrmFromModel(str, this.trm)) {
                gameLocal.Error("idSecurityCamera '%s': cannot load collision model %s", this.name, str);
                return;
            }

            GetPhysics().SetContents(CONTENTS_SOLID);
            GetPhysics().SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            // setup the physics
            UpdateChangeableSpawnArgs(null);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.angle);
            savefile.WriteFloat(this.sweepAngle);
            savefile.WriteInt(this.modelAxis);
            savefile.WriteBool(this.flipAxis);
            savefile.WriteFloat(this.scanDist);
            savefile.WriteFloat(this.scanFov);

            savefile.WriteFloat(this.sweepStart);
            savefile.WriteFloat(this.sweepEnd);
            savefile.WriteBool(this.negativeSweep);
            savefile.WriteBool(this.sweeping);
            savefile.WriteInt(this.alertMode);
            savefile.WriteFloat(this.stopSweeping);
            savefile.WriteFloat(this.scanFovCos);

            savefile.WriteVec3(this.viewOffset);

            savefile.WriteInt(this.pvsArea);
            savefile.WriteStaticObject(this.physicsObj);
            savefile.WriteTraceModel(this.trm);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.angle = savefile.ReadFloat();
            this.sweepAngle = savefile.ReadFloat();
            this.modelAxis = savefile.ReadInt();
            this.flipAxis = savefile.ReadBool();
            this.scanDist = savefile.ReadFloat();
            this.scanFov = savefile.ReadFloat();

            this.sweepStart = savefile.ReadFloat();
            this.sweepEnd = savefile.ReadFloat();
            this.negativeSweep = savefile.ReadBool();
            this.sweeping = savefile.ReadBool();
            this.alertMode = savefile.ReadInt();
            this.stopSweeping = savefile.ReadFloat();
            this.scanFovCos = savefile.ReadFloat();

            savefile.ReadVec3(this.viewOffset);

            this.pvsArea = savefile.ReadInt();
            savefile.ReadStaticObject(this.physicsObj);
            savefile.ReadTraceModel(this.trm);
        }

        @Override
        public void Think() {
            float pct;
            float travel;

            if ((this.thinkFlags & TH_THINK) != 0) {
                if (g_showEntityInfo.GetBool()) {
                    DrawFov();
                }

                if (this.health <= 0) {
                    BecomeInactive(TH_THINK);
                    return;
                }
            }

            // run physics
            RunPhysics();

            if ((this.thinkFlags & TH_THINK) != 0) {
                if (CanSeePlayer()) {
                    if (this.alertMode == SCANNING) {
                        float sightTime;

                        SetAlertMode(ALERT);
                        this.stopSweeping = gameLocal.time;
                        if (this.sweeping) {
                            CancelEvents(EV_SecurityCam_Pause);
                        } else {
                            CancelEvents(EV_SecurityCam_ReverseSweep);
                        }
                        this.sweeping = false;
                        StopSound(etoi(SND_CHANNEL_ANY), false);
                        StartSound("snd_sight", SND_CHANNEL_BODY, 0, false, null);

                        sightTime = this.spawnArgs.GetFloat("sightTime", "5");
                        PostEventSec(EV_SecurityCam_Alert, sightTime);
                    }
                } else {
                    if (this.alertMode == ALERT) {
                        float sightResume;

                        SetAlertMode(LOSINGINTEREST);
                        CancelEvents(EV_SecurityCam_Alert);

                        sightResume = this.spawnArgs.GetFloat("sightResume", "1.5");
                        PostEventSec(EV_SecurityCam_ContinueSweep, sightResume);
                    }

                    if (this.sweeping) {
                        final idAngles a = GetPhysics().GetAxis().ToAngles();

                        pct = (gameLocal.time - this.sweepStart) / (this.sweepEnd - this.sweepStart);
                        travel = pct * this.sweepAngle;
                        if (this.negativeSweep) {
                            a.yaw = this.angle + travel;
                        } else {
                            a.yaw = this.angle - travel;
                        }

                        SetAngles(a);
                    }
                }
            }
            Present();
        }

        @Override
        public renderView_s GetRenderView() {
            final renderView_s rv = super.GetRenderView();
            rv.fov_x = this.scanFov;
            rv.fov_y = this.scanFov;
            rv.viewaxis = GetAxis().ToAngles().ToMat3();
            rv.vieworg = GetPhysics().GetOrigin().oPlus(this.viewOffset);
            return rv;
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            this.sweeping = false;
            StopSound(etoi(SND_CHANNEL_ANY), false);
            final String fx = this.spawnArgs.GetString("fx_destroyed");
            if (isNotNullOrEmpty(fx)) {//fx[0] != '\0' ) {
                idEntityFx.StartFx(fx, null, null, this, true);
            }

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(this.trm), 0.02f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetBouncyness(0.2f);
            this.physicsObj.SetFriction(0.6f, 0.6f, 0.2f);
            this.physicsObj.SetGravity(gameLocal.GetGravity());
            this.physicsObj.SetContents(CONTENTS_SOLID);
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            SetPhysics(this.physicsObj);
            this.physicsObj.DropToFloor();
        }

        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            final String fx = this.spawnArgs.GetString("fx_damage");
            if (isNotNullOrEmpty(fx)) {//fx[0] != '\0' ) {
                idEntityFx.StartFx(fx, null, null, this, true);
            }
            return true;
        }

        /*
         ================
         idSecurityCamera::Present

         Present is called to allow entities to generate refEntities, lights, etc for the renderer.
         ================
         */
        @Override
        public void Present() {
            // don't present to the renderer if the entity hasn't changed
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            // camera target for remote render views
            if (this.cameraTarget != null) {
                this.renderEntity.remoteRenderView = this.cameraTarget.GetRenderView();
            }

            // if set to invisible, skip
            if ((null == this.renderEntity.hModel) || IsHidden()) {
                return;
            }

            // add to refresh list
            if (this.modelDefHandle == -1) {
                this.modelDefHandle = gameRenderWorld.AddEntityDef(this.renderEntity);
                final int a = 0;
            } else {
                gameRenderWorld.UpdateEntityDef(this.modelDefHandle, this.renderEntity);
            }
        }

        private void StartSweep() {
            int speed;

            this.sweeping = true;
            this.sweepStart = gameLocal.time;
            speed = (int) SEC2MS(SweepSpeed());
            this.sweepEnd = this.sweepStart + speed;
            PostEventMS(EV_SecurityCam_Pause, speed);
            StartSound("snd_moving", SND_CHANNEL_BODY, 0, false, null);
        }

        private boolean CanSeePlayer() {
            int i;
            float dist;
            idPlayer ent;
            final trace_s[] tr = {null};
            idVec3 dir;
            pvsHandle_t handle;

            handle = gameLocal.pvs.SetupCurrentPVS(this.pvsArea);

            for (i = 0; i < gameLocal.numClients; i++) {
                ent = (idPlayer) gameLocal.entities[i];

                if (NOT(ent) || (ent.fl.notarget)) {
                    continue;
                }

                // if there is no way we can see this player
                if (!gameLocal.pvs.InCurrentPVS(handle, ent.GetPVSAreas(), ent.GetNumPVSAreas())) {
                    continue;
                }

                dir = ent.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
                dist = dir.Normalize();

                if (dist > this.scanDist) {
                    continue;
                }

                if (dir.oMultiply(GetAxis()) < this.scanFovCos) {
                    continue;
                }

                idVec3 eye;

                eye = ent.EyeOffset();

                gameLocal.clip.TracePoint(tr, GetPhysics().GetOrigin(), ent.GetPhysics().GetOrigin().oPlus(eye), MASK_OPAQUE, this);
                if ((tr[0].fraction == 1.0f) || (gameLocal.GetTraceEntity(tr[0]).equals(ent))) {
                    gameLocal.pvs.FreeCurrentPVS(handle);
                    return true;
                }
            }

            gameLocal.pvs.FreeCurrentPVS(handle);

            return false;
        }

        private void SetAlertMode(int alert) {
            if ((alert >= SCANNING) && (alert <= ACTIVATED)) {
                this.alertMode = alert;
            }
            this.renderEntity.shaderParms[SHADERPARM_MODE] = this.alertMode;
            UpdateVisuals();
        }

        private void DrawFov() {
            int i;
            float radius, a, halfRadius;
            final float[] s = new float[1], c = new float[1];
            final idVec3 right = new idVec3(), up = new idVec3();
            final idVec4 color = new idVec4(1, 0, 0, 1), color2 = new idVec4(0, 0, 1, 1);
            idVec3 lastPoint, point, lastHalfPoint, halfPoint, center;

            final idVec3 dir = GetAxis();
            dir.NormalVectors(right, up);

            radius = (float) Math.tan((this.scanFov * idMath.PI) / 360.0f);
            halfRadius = radius * 0.5f;
            lastPoint = dir.oPlus(up.oMultiply(radius));
            lastPoint.Normalize();
            lastPoint = GetPhysics().GetOrigin().oPlus(lastPoint.oMultiply(this.scanDist));
            lastHalfPoint = dir.oPlus(up.oMultiply(halfRadius));
            lastHalfPoint.Normalize();
            lastHalfPoint = GetPhysics().GetOrigin().oPlus(lastHalfPoint.oMultiply(this.scanDist));
            center = GetPhysics().GetOrigin().oPlus(dir.oMultiply(this.scanDist));
            for (i = 1; i < 12; i++) {
                a = (idMath.TWO_PI * i) / 12.0f;
                idMath.SinCos(a, s, c);
                point = dir.oPlus(right.oMultiply(s[0] * radius).oPlus(up.oMultiply(c[0] * radius)));
                point.Normalize();
                point = GetPhysics().GetOrigin().oPlus(point.oMultiply(this.scanDist));
                gameRenderWorld.DebugLine(color, lastPoint, point);
                gameRenderWorld.DebugLine(color, GetPhysics().GetOrigin(), point);
                lastPoint = point;

                halfPoint = dir.oPlus(right.oMultiply(s[0] * halfRadius).oPlus(up.oMultiply(c[0] * halfRadius)));
                halfPoint.Normalize();
                halfPoint = GetPhysics().GetOrigin().oPlus(halfPoint.oMultiply(this.scanDist));
                gameRenderWorld.DebugLine(color2, point, halfPoint);
                gameRenderWorld.DebugLine(color2, lastHalfPoint, halfPoint);
                lastHalfPoint = halfPoint;

                gameRenderWorld.DebugLine(color2, halfPoint, center);
            }
        }

        private idVec3 GetAxis() {
            return (this.flipAxis) ? GetPhysics().GetAxis().oGet(this.modelAxis).oNegative() : GetPhysics().GetAxis().oGet(this.modelAxis);
        }

        private float SweepSpeed() {
            return this.spawnArgs.GetFloat("sweepSpeed", "5");
        }

        private void Event_ReverseSweep() {
            this.angle = GetPhysics().GetAxis().ToAngles().yaw;
            this.negativeSweep = !this.negativeSweep;
            StartSweep();
        }

        private void Event_ContinueSweep() {
            final float pct = (this.stopSweeping - this.sweepStart) / (this.sweepEnd - this.sweepStart);
            final float f = gameLocal.time - ((this.sweepEnd - this.sweepStart) * pct);
            int speed;

            this.sweepStart = f;
            speed = (int) MS2SEC(SweepSpeed());
            this.sweepEnd = this.sweepStart + speed;
            PostEventMS(EV_SecurityCam_Pause, (int) (speed * (1.0f - pct)));
            StartSound("snd_moving", SND_CHANNEL_BODY, 0, false, null);
            SetAlertMode(SCANNING);
            this.sweeping = true;
        }

        private void Event_Pause() {
            float sweepWait;

            sweepWait = this.spawnArgs.GetFloat("sweepWait", "0.5");
            this.sweeping = false;
            StopSound(etoi(SND_CHANNEL_ANY), false);
            StartSound("snd_stop", SND_CHANNEL_BODY, 0, false, null);
            PostEventSec(EV_SecurityCam_ReverseSweep, sweepWait);
        }

        private void Event_Alert() {
            float wait;

            SetAlertMode(ACTIVATED);
            StopSound(etoi(SND_CHANNEL_ANY), false);
            StartSound("snd_activate", SND_CHANNEL_BODY, 0, false, null);
            ActivateTargets(this);
            CancelEvents(EV_SecurityCam_ContinueSweep);

            wait = this.spawnArgs.GetFloat("wait", "20");
            PostEventSec(EV_SecurityCam_ContinueSweep, wait);
        }

        private void Event_AddLight() {
            final idDict args = new idDict();
            idVec3 right = new idVec3(), up = new idVec3(), target;
			final idVec3 temp;
            idVec3 dir;
            float radius;
            final idVec3 lightOffset = new idVec3();
            idLight spotLight;

            dir = GetAxis();
            dir.NormalVectors(right, up);
            target = GetPhysics().GetOrigin().oPlus(dir.oMultiply(this.scanDist));

            radius = (float) Math.tan((this.scanFov * idMath.PI) / 360.0f);
            up = dir.oPlus(up.oMultiply(radius));
            up.Normalize();
            up = GetPhysics().GetOrigin().oPlus(up.oMultiply(this.scanDist));
            up.oMinSet(target);

            right = dir.oPlus(right.oMultiply(radius));
            right.Normalize();
            right = GetPhysics().GetOrigin().oPlus(right.oMultiply(this.scanDist));
            right.oMinSet(target);

            this.spawnArgs.GetVector("lightOffset", "0 0 0", lightOffset);

            args.Set("origin", (GetPhysics().GetOrigin().oPlus(lightOffset)).ToString());
            args.Set("light_target", target.ToString());
            args.Set("light_right", right.ToString());
            args.Set("light_up", up.ToString());
            args.SetFloat("angle", GetPhysics().GetAxis().oGet(0).ToYaw());

            spotLight = (idLight) gameLocal.SpawnEntityType(idLight.class, args);
            spotLight.Bind(this, true);
            spotLight.UpdateVisuals();
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
