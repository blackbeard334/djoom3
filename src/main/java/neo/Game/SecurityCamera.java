package neo.Game;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_showEntityInfo;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import neo.Game.Light.idLight;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Game.Player.idPlayer;
import neo.Game.Pvs.pvsHandle_t;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import neo.Renderer.RenderWorld.renderView_s;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

import java.util.HashMap;
import java.util.Map;

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

            sweepAngle = spawnArgs.GetFloat("sweepAngle", "90");
            health = spawnArgs.GetInt("health", "100");
            scanFov = spawnArgs.GetFloat("scanFov", "90");
            scanDist = spawnArgs.GetFloat("scanDist", "200");
            flipAxis = spawnArgs.GetBool("flipAxis");

            modelAxis = spawnArgs.GetInt("modelAxis");
            if (modelAxis < 0 || modelAxis > 2) {
                modelAxis = 0;
            }

            spawnArgs.GetVector("viewOffset", "0 0 0", viewOffset);

            if (spawnArgs.GetBool("spotLight")) {
                PostEventMS(EV_SecurityCam_AddLight, 0);
            }

            negativeSweep = (sweepAngle < 0);
            sweepAngle = Math.abs(sweepAngle);

            scanFovCos = (float) Math.cos(scanFov * idMath.PI / 360.0f);

            angle = GetPhysics().GetAxis().ToAngles().yaw;
            StartSweep();
            SetAlertMode(SCANNING);
            BecomeActive(TH_THINK);

            if (health != 0) {
                fl.takedamage = true;
            }

            pvsArea = gameLocal.pvs.GetPVSArea(GetPhysics().GetOrigin());
            // if no target specified use ourself
            str = new idStr(spawnArgs.GetString("cameraTarget"));
            if (str.Length() == 0) {
                spawnArgs.Set("cameraTarget", spawnArgs.GetString("name"));
            }

            // check if a clip model is set
            spawnArgs.GetString("clipmodel", "", str);
            if (!isNotNullOrEmpty(str)) {
                str.oSet(spawnArgs.GetString("model"));		// use the visual model
            }

            if (!CollisionModel_local.collisionModelManager.TrmFromModel(str, trm)) {
                gameLocal.Error("idSecurityCamera '%s': cannot load collision model %s", name, str);
                return;
            }

            GetPhysics().SetContents(CONTENTS_SOLID);
            GetPhysics().SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            // setup the physics
            UpdateChangeableSpawnArgs(null);
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(angle);
            savefile.WriteFloat(sweepAngle);
            savefile.WriteInt(modelAxis);
            savefile.WriteBool(flipAxis);
            savefile.WriteFloat(scanDist);
            savefile.WriteFloat(scanFov);

            savefile.WriteFloat(sweepStart);
            savefile.WriteFloat(sweepEnd);
            savefile.WriteBool(negativeSweep);
            savefile.WriteBool(sweeping);
            savefile.WriteInt(alertMode);
            savefile.WriteFloat(stopSweeping);
            savefile.WriteFloat(scanFovCos);

            savefile.WriteVec3(viewOffset);

            savefile.WriteInt(pvsArea);
            savefile.WriteStaticObject(physicsObj);
            savefile.WriteTraceModel(trm);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            angle = savefile.ReadFloat();
            sweepAngle = savefile.ReadFloat();
            modelAxis = savefile.ReadInt();
            flipAxis = savefile.ReadBool();
            scanDist = savefile.ReadFloat();
            scanFov = savefile.ReadFloat();

            sweepStart = savefile.ReadFloat();
            sweepEnd = savefile.ReadFloat();
            negativeSweep = savefile.ReadBool();
            sweeping = savefile.ReadBool();
            alertMode = savefile.ReadInt();
            stopSweeping = savefile.ReadFloat();
            scanFovCos = savefile.ReadFloat();

            savefile.ReadVec3(viewOffset);

            pvsArea = savefile.ReadInt();
            savefile.ReadStaticObject(physicsObj);
            savefile.ReadTraceModel(trm);
        }

        @Override
        public void Think() {
            float pct;
            float travel;

            if ((thinkFlags & TH_THINK) != 0) {
                if (g_showEntityInfo.GetBool()) {
                    DrawFov();
                }

                if (health <= 0) {
                    BecomeInactive(TH_THINK);
                    return;
                }
            }

            // run physics
            RunPhysics();

            if ((thinkFlags & TH_THINK) != 0) {
                if (CanSeePlayer()) {
                    if (alertMode == SCANNING) {
                        float sightTime;

                        SetAlertMode(ALERT);
                        stopSweeping = gameLocal.time;
                        if (sweeping) {
                            CancelEvents(EV_SecurityCam_Pause);
                        } else {
                            CancelEvents(EV_SecurityCam_ReverseSweep);
                        }
                        sweeping = false;
                        StopSound(etoi(SND_CHANNEL_ANY), false);
                        StartSound("snd_sight", SND_CHANNEL_BODY, 0, false, null);

                        sightTime = spawnArgs.GetFloat("sightTime", "5");
                        PostEventSec(EV_SecurityCam_Alert, sightTime);
                    }
                } else {
                    if (alertMode == ALERT) {
                        float sightResume;

                        SetAlertMode(LOSINGINTEREST);
                        CancelEvents(EV_SecurityCam_Alert);

                        sightResume = spawnArgs.GetFloat("sightResume", "1.5");
                        PostEventSec(EV_SecurityCam_ContinueSweep, sightResume);
                    }

                    if (sweeping) {
                        idAngles a = GetPhysics().GetAxis().ToAngles();

                        pct = (gameLocal.time - sweepStart) / (sweepEnd - sweepStart);
                        travel = pct * sweepAngle;
                        if (negativeSweep) {
                            a.yaw = angle + travel;
                        } else {
                            a.yaw = angle - travel;
                        }

                        SetAngles(a);
                    }
                }
            }
            Present();
        }

        @Override
        public renderView_s GetRenderView() {
            renderView_s rv = super.GetRenderView();
            rv.fov_x = scanFov;
            rv.fov_y = scanFov;
            rv.viewaxis = GetAxis().ToAngles().ToMat3();
            rv.vieworg = GetPhysics().GetOrigin().oPlus(viewOffset);
            return rv;
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            sweeping = false;
            StopSound(etoi(SND_CHANNEL_ANY), false);
            final String fx = spawnArgs.GetString("fx_destroyed");
            if (isNotNullOrEmpty(fx)) {//fx[0] != '\0' ) {
                idEntityFx.StartFx(fx, null, null, this, true);
            }

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(trm), 0.02f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetBouncyness(0.2f);
            physicsObj.SetFriction(0.6f, 0.6f, 0.2f);
            physicsObj.SetGravity(gameLocal.GetGravity());
            physicsObj.SetContents(CONTENTS_SOLID);
            physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY | CONTENTS_CORPSE | CONTENTS_MOVEABLECLIP);
            SetPhysics(physicsObj);
            physicsObj.DropToFloor();
        }

        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            final String fx = spawnArgs.GetString("fx_damage");
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
            if (0 == (thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            // camera target for remote render views
            if (cameraTarget != null) {
                renderEntity.remoteRenderView = cameraTarget.GetRenderView();
            }

            // if set to invisible, skip
            if (null == renderEntity.hModel || IsHidden()) {
                return;
            }

            // add to refresh list
            if (modelDefHandle == -1) {
                modelDefHandle = gameRenderWorld.AddEntityDef(renderEntity);
                int a = 0;
            } else {
                gameRenderWorld.UpdateEntityDef(modelDefHandle, renderEntity);
            }
        }

        private void StartSweep() {
            int speed;

            sweeping = true;
            sweepStart = gameLocal.time;
            speed = (int) SEC2MS(SweepSpeed());
            sweepEnd = sweepStart + speed;
            PostEventMS(EV_SecurityCam_Pause, speed);
            StartSound("snd_moving", SND_CHANNEL_BODY, 0, false, null);
        }

        private boolean CanSeePlayer() {
            int i;
            float dist;
            idPlayer ent;
            trace_s[] tr = {null};
            idVec3 dir;
            pvsHandle_t handle;

            handle = gameLocal.pvs.SetupCurrentPVS(pvsArea);

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

                if (dist > scanDist) {
                    continue;
                }

                if (dir.oMultiply(GetAxis()) < scanFovCos) {
                    continue;
                }

                idVec3 eye;

                eye = ent.EyeOffset();

                gameLocal.clip.TracePoint(tr, GetPhysics().GetOrigin(), ent.GetPhysics().GetOrigin().oPlus(eye), MASK_OPAQUE, this);
                if (tr[0].fraction == 1.0f || (gameLocal.GetTraceEntity(tr[0]).equals(ent))) {
                    gameLocal.pvs.FreeCurrentPVS(handle);
                    return true;
                }
            }

            gameLocal.pvs.FreeCurrentPVS(handle);

            return false;
        }

        private void SetAlertMode(int alert) {
            if (alert >= SCANNING && alert <= ACTIVATED) {
                alertMode = alert;
            }
            renderEntity.shaderParms[SHADERPARM_MODE] = alertMode;
            UpdateVisuals();
        }

        private void DrawFov() {
            int i;
            float radius, a, halfRadius;
            float[] s = new float[1], c = new float[1];
            idVec3 right = new idVec3(), up = new idVec3();
            idVec4 color = new idVec4(1, 0, 0, 1), color2 = new idVec4(0, 0, 1, 1);
            idVec3 lastPoint, point, lastHalfPoint, halfPoint, center;

            idVec3 dir = GetAxis();
            dir.NormalVectors(right, up);

            radius = (float) Math.tan(scanFov * idMath.PI / 360.0f);
            halfRadius = radius * 0.5f;
            lastPoint = dir.oPlus(up.oMultiply(radius));
            lastPoint.Normalize();
            lastPoint = GetPhysics().GetOrigin().oPlus(lastPoint.oMultiply(scanDist));
            lastHalfPoint = dir.oPlus(up.oMultiply(halfRadius));
            lastHalfPoint.Normalize();
            lastHalfPoint = GetPhysics().GetOrigin().oPlus(lastHalfPoint.oMultiply(scanDist));
            center = GetPhysics().GetOrigin().oPlus(dir.oMultiply(scanDist));
            for (i = 1; i < 12; i++) {
                a = idMath.TWO_PI * i / 12.0f;
                idMath.SinCos(a, s, c);
                point = dir.oPlus(right.oMultiply(s[0] * radius).oPlus(up.oMultiply(c[0] * radius)));
                point.Normalize();
                point = GetPhysics().GetOrigin().oPlus(point.oMultiply(scanDist));
                gameRenderWorld.DebugLine(color, lastPoint, point);
                gameRenderWorld.DebugLine(color, GetPhysics().GetOrigin(), point);
                lastPoint = point;

                halfPoint = dir.oPlus(right.oMultiply(s[0] * halfRadius).oPlus(up.oMultiply(c[0] * halfRadius)));
                halfPoint.Normalize();
                halfPoint = GetPhysics().GetOrigin().oPlus(halfPoint.oMultiply(scanDist));
                gameRenderWorld.DebugLine(color2, point, halfPoint);
                gameRenderWorld.DebugLine(color2, lastHalfPoint, halfPoint);
                lastHalfPoint = halfPoint;

                gameRenderWorld.DebugLine(color2, halfPoint, center);
            }
        }

        private idVec3 GetAxis() {
            return (flipAxis) ? GetPhysics().GetAxis().oGet(modelAxis).oNegative() : GetPhysics().GetAxis().oGet(modelAxis);
        }

        private float SweepSpeed() {
            return spawnArgs.GetFloat("sweepSpeed", "5");
        }

        private void Event_ReverseSweep() {
            angle = GetPhysics().GetAxis().ToAngles().yaw;
            negativeSweep = !negativeSweep;
            StartSweep();
        }

        private void Event_ContinueSweep() {
            float pct = (stopSweeping - sweepStart) / (sweepEnd - sweepStart);
            float f = gameLocal.time - (sweepEnd - sweepStart) * pct;
            int speed;

            sweepStart = f;
            speed = (int) MS2SEC(SweepSpeed());
            sweepEnd = sweepStart + speed;
            PostEventMS(EV_SecurityCam_Pause, (int) (speed * (1.0f - pct)));
            StartSound("snd_moving", SND_CHANNEL_BODY, 0, false, null);
            SetAlertMode(SCANNING);
            sweeping = true;
        }

        private void Event_Pause() {
            float sweepWait;

            sweepWait = spawnArgs.GetFloat("sweepWait", "0.5");
            sweeping = false;
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

            wait = spawnArgs.GetFloat("wait", "20");
            PostEventSec(EV_SecurityCam_ContinueSweep, wait);
        }

        private void Event_AddLight() {
            idDict args = new idDict();
            idVec3 right = new idVec3(), up = new idVec3(), target, temp;
            idVec3 dir;
            float radius;
            idVec3 lightOffset = new idVec3();
            idLight spotLight;

            dir = GetAxis();
            dir.NormalVectors(right, up);
            target = GetPhysics().GetOrigin().oPlus(dir.oMultiply(scanDist));

            radius = (float) Math.tan(scanFov * idMath.PI / 360.0f);
            up = dir.oPlus(up.oMultiply(radius));
            up.Normalize();
            up = GetPhysics().GetOrigin().oPlus(up.oMultiply(scanDist));
            up.oMinSet(target);

            right = dir.oPlus(right.oMultiply(radius));
            right.Normalize();
            right = GetPhysics().GetOrigin().oPlus(right.oMultiply(scanDist));
            right.oMinSet(target);

            spawnArgs.GetVector("lightOffset", "0 0 0", lightOffset);

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

    };
}
