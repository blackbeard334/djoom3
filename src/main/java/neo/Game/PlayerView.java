package neo.Game;

import static java.lang.Math.sin;

import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_blobSize;
import static neo.Game.GameSys.SysCvar.g_blobTime;
import static neo.Game.GameSys.SysCvar.g_doubleVision;
import static neo.Game.GameSys.SysCvar.g_dvAmplitude;
import static neo.Game.GameSys.SysCvar.g_dvFrequency;
import static neo.Game.GameSys.SysCvar.g_dvTime;
import static neo.Game.GameSys.SysCvar.g_kickAmplitude;
import static neo.Game.GameSys.SysCvar.g_kickTime;
import static neo.Game.GameSys.SysCvar.g_skipViewEffects;
import static neo.Game.GameSys.SysCvar.g_testHealthVision;
import static neo.Game.GameSys.SysCvar.g_testPostProcess;
import static neo.Game.GameSys.SysCvar.pm_thirdPerson;
import static neo.Game.Game_local.LAGO_MATERIAL;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Game_network.net_clientLagOMeter;
import static neo.Game.Player.BERSERK;
import neo.Game.Player.idPlayer;
import neo.Game.PlayerView.screenBlob_t;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import neo.Renderer.RenderWorld.renderView_s;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import neo.idlib.Dict_h.idDict;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class PlayerView {

    /*
     ===============================================================================

     Player view.

     ===============================================================================
     */
// screenBlob_t is for the on-screen damage claw marks, etc
    public static class screenBlob_t {

        idMaterial material;
        float x, y, w, h;
        float s1, t1, s2, t2;
        int finishTime;
        int startFadeTime;
        float driftAmount;
    };
    public static final int MAX_SCREEN_BLOBS    = 8;
    public static final int IMPULSE_DELAY       = 150;

    public static class idPlayerView {

        private final screenBlob_t[] screenBlobs = new screenBlob_t[MAX_SCREEN_BLOBS];
        //
        private int          dvFinishTime;      // double vision will be stopped at this time
        private idMaterial   dvMaterial;        // material to take the double vision screen shot
        //
        private int          kickFinishTime;    // view kick will be stopped at this time
        private idAngles     kickAngles;
        //
        private boolean      bfgVision;
        //
        private idMaterial   tunnelMaterial;    // health tunnel vision
        private idMaterial   armorMaterial;     // armor damage view effect
        private idMaterial   berserkMaterial;   // berserk effect
        private idMaterial   irGogglesMaterial; // ir effect
        private idMaterial   bloodSprayMaterial;// blood spray
        private idMaterial   bfgMaterial;       // when targeted with BFG
        private idMaterial   lagoMaterial;      // lagometer drawing
        private float        lastDamageTime;    // accentuate the tunnel effect for a while
        //
        private idVec4       fadeColor;         // fade color
        private idVec4       fadeToColor;       // color to fade to
        private idVec4       fadeFromColor;     // color to fade from
        private float        fadeRate;          // fade rate
        private int          fadeTime;          // fade time
        //
        private idAngles     shakeAng;          // from the sound sources
        //
        private idPlayer     player;
        private renderView_s view;
        //
        //

        public idPlayerView() {
//	memset( screenBlobs, 0, sizeof( screenBlobs ) );
//	memset( &view, 0, sizeof( view ) );
            view = new renderView_s();
            player = null;
            dvMaterial = declManager.FindMaterial("_scratch");
            tunnelMaterial = declManager.FindMaterial("textures/decals/tunnel");
            armorMaterial = declManager.FindMaterial("armorViewEffect");
            berserkMaterial = declManager.FindMaterial("textures/decals/berserk");
            irGogglesMaterial = declManager.FindMaterial("textures/decals/irblend");
            bloodSprayMaterial = declManager.FindMaterial("textures/decals/bloodspray");
            bfgMaterial = declManager.FindMaterial("textures/decals/bfgvision");
            lagoMaterial = declManager.FindMaterial(LAGO_MATERIAL, false);
            bfgVision = false;
            dvFinishTime = 0;
            kickFinishTime = 0;
            kickAngles = new idAngles();
            lastDamageTime = 0f;
            fadeTime = 0;
            fadeRate = 0;
            fadeFromColor = new idVec4();
            fadeToColor = new idVec4();
            fadeColor = new idVec4();
            shakeAng = new idAngles();

            ClearEffects();
        }

        public void Save(idSaveGame savefile) {
            int i;
            screenBlob_t blob;

            blob = screenBlobs[0];
            for (i = 0; i < MAX_SCREEN_BLOBS; blob = screenBlobs[++i]) {
                savefile.WriteMaterial(blob.material);
                savefile.WriteFloat(blob.x);
                savefile.WriteFloat(blob.y);
                savefile.WriteFloat(blob.w);
                savefile.WriteFloat(blob.h);
                savefile.WriteFloat(blob.s1);
                savefile.WriteFloat(blob.t1);
                savefile.WriteFloat(blob.s2);
                savefile.WriteFloat(blob.t2);
                savefile.WriteInt(blob.finishTime);
                savefile.WriteInt(blob.startFadeTime);
                savefile.WriteFloat(blob.driftAmount);
            }

            savefile.WriteInt(dvFinishTime);
            savefile.WriteMaterial(dvMaterial);
            savefile.WriteInt(kickFinishTime);
            savefile.WriteAngles(kickAngles);
            savefile.WriteBool(bfgVision);

            savefile.WriteMaterial(tunnelMaterial);
            savefile.WriteMaterial(armorMaterial);
            savefile.WriteMaterial(berserkMaterial);
            savefile.WriteMaterial(irGogglesMaterial);
            savefile.WriteMaterial(bloodSprayMaterial);
            savefile.WriteMaterial(bfgMaterial);
            savefile.WriteFloat(lastDamageTime);

            savefile.WriteVec4(fadeColor);
            savefile.WriteVec4(fadeToColor);
            savefile.WriteVec4(fadeFromColor);
            savefile.WriteFloat(fadeRate);
            savefile.WriteInt(fadeTime);

            savefile.WriteAngles(shakeAng);

            savefile.WriteObject(player);
            savefile.WriteRenderView(view);
        }

        public void Restore(idRestoreGame savefile) {
            int i;
            screenBlob_t blob;

//            blob = screenBlobs[ 0];
            for (blob = screenBlobs[i = 0]; i < MAX_SCREEN_BLOBS; blob = screenBlobs[++i]) {
                savefile.ReadMaterial(blob.material);
                blob.x = savefile.ReadFloat();
                blob.y = savefile.ReadFloat();
                blob.w = savefile.ReadFloat();
                blob.h = savefile.ReadFloat();
                blob.s1 = savefile.ReadFloat();
                blob.t1 = savefile.ReadFloat();
                blob.s2 = savefile.ReadFloat();
                blob.t2 = savefile.ReadFloat();
                blob.finishTime = savefile.ReadInt();
                blob.startFadeTime = savefile.ReadInt();
                blob.driftAmount = savefile.ReadFloat();
            }

            dvFinishTime = savefile.ReadInt();
            savefile.ReadMaterial(dvMaterial);
            kickFinishTime = savefile.ReadInt();
            savefile.ReadAngles(kickAngles);
            bfgVision = savefile.ReadBool();

            savefile.ReadMaterial(tunnelMaterial);
            savefile.ReadMaterial(armorMaterial);
            savefile.ReadMaterial(berserkMaterial);
            savefile.ReadMaterial(irGogglesMaterial);
            savefile.ReadMaterial(bloodSprayMaterial);
            savefile.ReadMaterial(bfgMaterial);
            lastDamageTime = savefile.ReadFloat();

            savefile.ReadVec4(fadeColor);
            savefile.ReadVec4(fadeToColor);
            savefile.ReadVec4(fadeFromColor);
            fadeRate = savefile.ReadFloat();
            fadeTime = savefile.ReadInt();

            savefile.ReadAngles(shakeAng);

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/player);
            savefile.ReadRenderView(view);
        }

        public void SetPlayerEntity(idPlayer playerEnt) {
            player = playerEnt;
        }

        public void ClearEffects() {
            lastDamageTime = MS2SEC(gameLocal.time - 99999);

            dvFinishTime = (gameLocal.time - 99999);
            kickFinishTime = (gameLocal.time - 99999);

            for (int i = 0; i < MAX_SCREEN_BLOBS; i++) {
                screenBlobs[i] = new screenBlob_t();
                screenBlobs[i].finishTime = gameLocal.time;
            }

            fadeTime = 0;
            bfgVision = false;
        }

        /*
         ==============
         idPlayerView::DamageImpulse

         LocalKickDir is the direction of force in the player's coordinate system,
         which will determine the head kick direction
         ==============
         */
        public void DamageImpulse(idVec3 localKickDir, final idDict damageDef) {
            //
            // double vision effect
            //
            if (lastDamageTime > 0.0f && SEC2MS(lastDamageTime) + IMPULSE_DELAY > gameLocal.time) {
                // keep shotgun from obliterating the view
                return;
            }

            float dvTime = damageDef.GetFloat("dv_time");
            if (dvTime != 0) {
                if (dvFinishTime < gameLocal.time) {
                    dvFinishTime = gameLocal.time;
                }
                dvFinishTime += g_dvTime.GetFloat() * dvTime;
                // don't let it add up too much in god mode
                if (dvFinishTime > gameLocal.time + 5000) {
                    dvFinishTime = gameLocal.time + 5000;
                }
            }

            //
            // head angle kick
            //
            float kickTime = damageDef.GetFloat("kick_time");
            if (kickTime != 0) {
                kickFinishTime = (int) (gameLocal.time + g_kickTime.GetFloat() * kickTime);

                // forward / back kick will pitch view
                kickAngles.oSet(0, localKickDir.oGet(0));

                // side kick will yaw view
                kickAngles.oSet(1, localKickDir.oGet(1) * 0.5f);

                // up / down kick will pitch view
                kickAngles.oPluSet(0, localKickDir.oGet(2));

                // roll will come from  side
                kickAngles.oSet(2, localKickDir.oGet(1));

                float kickAmplitude = damageDef.GetFloat("kick_amplitude");
                if (kickAmplitude != 0) {
                    kickAngles.oMulSet(kickAmplitude);
                }
            }

            //
            // screen blob
            //
            float blobTime = damageDef.GetFloat("blob_time");
            if (blobTime != 0) {
                screenBlob_t blob = GetScreenBlob();
                blob.startFadeTime = gameLocal.time;
                blob.finishTime = (int) (gameLocal.time + blobTime * g_blobTime.GetFloat());

                final String materialName = damageDef.GetString("mtr_blob");
                blob.material = declManager.FindMaterial(materialName);
                blob.x = damageDef.GetFloat("blob_x");
                blob.x += (gameLocal.random.RandomInt() & 63) - 32;
                blob.y = damageDef.GetFloat("blob_y");
                blob.y += (gameLocal.random.RandomInt() & 63) - 32;

                float scale = (256 + ((gameLocal.random.RandomInt() & 63) - 32)) / 256.0f;
                blob.w = damageDef.GetFloat("blob_width") * g_blobSize.GetFloat() * scale;
                blob.h = damageDef.GetFloat("blob_height") * g_blobSize.GetFloat() * scale;
                blob.s1 = 0;
                blob.t1 = 0;
                blob.s2 = 1;
                blob.t2 = 1;
            }

            //
            // save lastDamageTime for tunnel vision accentuation
            //
            lastDamageTime = MS2SEC(gameLocal.time);

        }

        /*
         ==================
         idPlayerView::WeaponFireFeedback

         Called when a weapon fires, generates head twitches, etc
         ==================
         */
        public void WeaponFireFeedback(final idDict weaponDef) {
            int recoilTime;

            recoilTime = weaponDef.GetInt("recoilTime");
            // don't shorten a damage kick in progress
            if (recoilTime != 0 && kickFinishTime < gameLocal.time) {
                idAngles angles = new idAngles();
                weaponDef.GetAngles("recoilAngles", "5 0 0", angles);
                kickAngles.oSet(angles);
                int finish = (int) (gameLocal.time + g_kickTime.GetFloat() * recoilTime);
                kickFinishTime = finish;
            }

        }

        /*
         ===================
         idPlayerView::AngleOffset

         kickVector, a world space direction that the attack should 
         ===================
         */
        public idAngles AngleOffset() {			// returns the current kick angle
            idAngles ang = new idAngles();

            ang.Zero();

            if (gameLocal.time < kickFinishTime) {
                float offset = kickFinishTime - gameLocal.time;

                ang = kickAngles.oMultiply(offset * offset * g_kickAmplitude.GetFloat());

                for (int i = 0; i < 3; i++) {
                    if (ang.oGet(i) > 70.0f) {
                        ang.oSet(i, 70.0f);
                    } else if (ang.oGet(i) < -70.0f) {
                        ang.oSet(i, -70.0f);
                    }
                }
            }
            return ang;
        }

        public idMat3 ShakeAxis() {			// returns the current shake angle
            return shakeAng.ToMat3();
        }

        public void CalculateShake() {
//            idVec3 origin, matrix;

            float shakeVolume = gameSoundWorld.CurrentShakeAmplitudeForPosition(gameLocal.time, player.firstPersonViewOrigin);
            //
            // shakeVolume should somehow be molded into an angle here
            // it should be thought of as being in the range 0.0 . 1.0, although
            // since CurrentShakeAmplitudeForPosition() returns all the shake sounds
            // the player can hear, it can go over 1.0 too.
            //
            shakeAng.oSet(0, gameLocal.random.CRandomFloat() * shakeVolume);
            shakeAng.oSet(1, gameLocal.random.CRandomFloat() * shakeVolume);
            shakeAng.oSet(2, gameLocal.random.CRandomFloat() * shakeVolume);
        }

        // this may involve rendering to a texture and displaying
        // that with a warp model or in double vision mode
        public void RenderPlayerView(idUserInterface hud) {
            final renderView_s view = player.GetRenderView();

            if (g_skipViewEffects.GetBool()) {
                SingleView(hud, view);
            } else {
                if (player.GetInfluenceMaterial() != null || player.GetInfluenceEntity() != null) {
                    InfluenceVision(hud, view);
                } else if (gameLocal.time < dvFinishTime) {
                    DoubleVision(hud, view, dvFinishTime - gameLocal.time);
                } else if (player.PowerUpActive(BERSERK)) {
                    BerserkVision(hud, view);
                } else {
                    SingleView(hud, view);
                }
                ScreenFade();
            }

            if (net_clientLagOMeter.GetBool() && lagoMaterial != null && gameLocal.isClient) {
                renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
                renderSystem.DrawStretchPic(10.0f, 380.0f, 64.0f, 64.0f, 0.0f, 0.0f, 1.0f, 1.0f, lagoMaterial);
            }
        }

        /*
         =================
         idPlayerView::Fade

         used for level transition fades
         assumes: color.w is 0 or 1
         =================
         */
        public void Fade(idVec4 color, int time) {

            if (0 == fadeTime) {
                fadeFromColor.Set(0.0f, 0.0f, 0.0f, 1.0f - color.oGet(3));
            } else {
                fadeFromColor.oSet(fadeColor);
            }
            fadeToColor.oSet(color);

            if (time <= 0) {
                fadeRate = 0;
                time = 0;
                fadeColor.oSet(fadeToColor);
            } else {
                fadeRate = 1.0f / (float) time;
            }

            if (gameLocal.realClientTime == 0 && time == 0) {
                fadeTime = 1;
            } else {
                fadeTime = gameLocal.realClientTime + time;
            }
        }

        /*
         =================
         idPlayerView::Flash

         flashes the player view with the given color
         =================
         */
        public void Flash(idVec4 color, int time) {
            Fade(new idVec4(0, 0, 0, 0), time);
            fadeFromColor.oSet(colorWhite);
        }

        /*
         ==================
         idPlayerView::AddBloodSpray

         If we need a more generic way to add blobs then we can do that
         but having it localized here lets the material be pre-looked up etc.
         ==================
         */
        public void AddBloodSpray(float duration) {//TODO:fix?
            /*
             if ( duration <= 0 || bloodSprayMaterial == NULL || g_skipViewEffects.GetBool() ) {
             return;
             }
             // visit this for chainsaw
             screenBlob_t *blob = GetScreenBlob();
             blob->startFadeTime = gameLocal.time;
             blob->finishTime = gameLocal.time + ( duration * 1000 );
             blob->material = bloodSprayMaterial;
             blob->x = ( gameLocal.random.RandomInt() & 63 ) - 32;
             blob->y = ( gameLocal.random.RandomInt() & 63 ) - 32;
             blob->driftAmount = 0.5f + gameLocal.random.CRandomFloat() * 0.5;
             float scale = ( 256 + ( ( gameLocal.random.RandomInt()&63 ) - 32 ) ) / 256.0f;
             blob->w = 600 * g_blobSize.GetFloat() * scale;
             blob->h = 480 * g_blobSize.GetFloat() * scale;
             float s1 = 0.0f;
             float t1 = 0.0f;
             float s2 = 1.0f;
             float t2 = 1.0f;
             if ( blob->driftAmount < 0.6 ) {
             s1 = 1.0f;
             s2 = 0.0f;
             } else if ( blob->driftAmount < 0.75 ) {
             t1 = 1.0f;
             t2 = 0.0f;
             } else if ( blob->driftAmount < 0.85 ) {
             s1 = 1.0f;
             s2 = 0.0f;
             t1 = 1.0f;
             t2 = 0.0f;
             }
             blob->s1 = s1;
             blob->t1 = t1;
             blob->s2 = s2;
             blob->t2 = t2;
             */
        }

        // temp for view testing
        public void EnableBFGVision(boolean b) {
            bfgVision = b;
        }

        private void SingleView(idUserInterface hud, final renderView_s view) {

            // normal rendering
            if (null == view) {
                return;
            }

            // place the sound origin for the player
            gameSoundWorld.PlaceListener(view.vieworg, view.viewaxis, player.entityNumber + 1, gameLocal.time, new idStr(hud != null ? hud.State().GetString("location") : "Undefined"));

            // if the objective system is up, don't do normal drawing
            if (player.objectiveSystemOpen) {
                player.objectiveSystem.Redraw(gameLocal.time);
                return;
            }

            // hack the shake in at the very last moment, so it can't cause any consistency problems
            renderView_s hackedView = view;
            hackedView.viewaxis = hackedView.viewaxis.oMultiply(ShakeAxis());
//            hackedView.viewaxis = idMat3.getMat3_identity();//HACKME::10

            gameRenderWorld.RenderScene(hackedView);

            if (player.spectating) {
                return;
            }

            // draw screen blobs
            if (!pm_thirdPerson.GetBool() && !g_skipViewEffects.GetBool()) {
                for (int i = 0; i < MAX_SCREEN_BLOBS; i++) {
                    screenBlob_t blob = screenBlobs[i];
                    if (blob.finishTime <= gameLocal.time) {
                        continue;
                    }

                    blob.y += blob.driftAmount;

                    float fade = (float) (blob.finishTime - gameLocal.time) / (blob.finishTime - blob.startFadeTime);
                    if (fade > 1.0f) {
                        fade = 1.0f;
                    }
                    if (fade != 0) {
                        renderSystem.SetColor4(1, 1, 1, fade);
                        renderSystem.DrawStretchPic(blob.x, blob.y, blob.w, blob.h, blob.s1, blob.t1, blob.s2, blob.t2, blob.material);
                    }
                }
                player.DrawHUD(hud);

                // armor impulse feedback
                float armorPulse = (gameLocal.time - player.lastArmorPulse) / 250.0f;

                if (armorPulse > 0.0f && armorPulse < 1.0f) {
                    renderSystem.SetColor4(1, 1, 1, 1.0f - armorPulse);
                    renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, armorMaterial);
                }

                // tunnel vision
                float health;
                if (g_testHealthVision.GetFloat() != 0.0f) {
                    health = g_testHealthVision.GetFloat();
                } else {
                    health = player.health;
                }
                float alpha = health / 100.0f;
                if (alpha < 0.0f) {
                    alpha = 0.0f;
                }
                if (alpha > 1.0f) {
                    alpha = 1.0f;
                }

                if (alpha < 1.0f) {
                    renderSystem.SetColor4((player.health <= 0.0f) ? MS2SEC(gameLocal.time) : lastDamageTime, 1.0f, 1.0f, (player.health <= 0.0f) ? 0.0f : alpha);
                    renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, tunnelMaterial);
                }

                if (player.PowerUpActive(BERSERK)) {
                    int berserkTime = player.inventory.powerupEndTime[ BERSERK] - gameLocal.time;
                    if (berserkTime > 0) {
                        // start fading if within 10 seconds of going away
                        alpha = (berserkTime < 10000) ? (float) berserkTime / 10000 : 1.0f;
                        renderSystem.SetColor4(1.0f, 1.0f, 1.0f, alpha);
                        renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, berserkMaterial);
                    }
                }

                if (bfgVision) {
                    renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
                    renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, bfgMaterial);
                }

            }

            // test a single material drawn over everything
            if (isNotNullOrEmpty(g_testPostProcess.GetString())) {
                final idMaterial mtr = declManager.FindMaterial(g_testPostProcess.GetString(), false);
                if (null == mtr) {
                    common.Printf("Material not found.\n");
                    g_testPostProcess.SetString("");
                } else {
                    renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
                    renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, mtr);
                }
            }
        }

        private void DoubleVision(idUserInterface hud, final renderView_s view, int offset) {

            if (!g_doubleVision.GetBool()) {
                SingleView(hud, view);
                return;
            }

            float scale = offset * g_dvAmplitude.GetFloat();
            if (scale > 0.5f) {
                scale = 0.5f;
            }
            float shift = (float) (scale * sin(Math.sqrt(offset) * g_dvFrequency.GetFloat()));
            shift = Math.abs(shift);

            // if double vision, render to a texture
            renderSystem.CropRenderSize(512, 256, true);
            SingleView(hud, view);
            renderSystem.CaptureRenderToImage("_scratch");
            renderSystem.UnCrop();

            // carry red tint if in berserk mode
            idVec4 color = new idVec4(1, 1, 1, 1);
            if (gameLocal.time < player.inventory.powerupEndTime[ BERSERK]) {
                color.y = 0;
                color.z = 0;
            }

            renderSystem.SetColor4(color.x, color.y, color.z, 1.0f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, shift, 1, 1, 0, dvMaterial);
            renderSystem.SetColor4(color.x, color.y, color.z, 0.5f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, 1 - shift, 0, dvMaterial);
        }

        private void BerserkVision(idUserInterface hud, final renderView_s view) {
            renderSystem.CropRenderSize(512, 256, true);
            SingleView(hud, view);
            renderSystem.CaptureRenderToImage("_scratch");
            renderSystem.UnCrop();
            renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, 1, 0, dvMaterial);
        }

        private void InfluenceVision(idUserInterface hud, final renderView_s view) {

            float distance;
            float pct = 1.0f;
            if (player.GetInfluenceEntity() != null) {
                distance = (player.GetInfluenceEntity().GetPhysics().GetOrigin().oMinus(player.GetPhysics().GetOrigin())).Length();
                if (player.GetInfluenceRadius() != 0.0f && distance < player.GetInfluenceRadius()) {
//			pct = distance / player.GetInfluenceRadius();//TODO:wtf?
                    pct = 1.0f - idMath.ClampFloat(0.0f, 1.0f, pct);
                }
            }
            if (player.GetInfluenceMaterial() != null) {
                SingleView(hud, view);
                renderSystem.CaptureRenderToImage("_currentRender");
                renderSystem.SetColor4(1.0f, 1.0f, 1.0f, pct);
                renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, player.GetInfluenceMaterial());
            } else if (player.GetInfluenceEntity() == null) {
                SingleView(hud, view);
//		return;
            } else {
                int offset = (int) (25 + sin(gameLocal.time));
                DoubleVision(hud, view, (int) (pct * offset));
            }
        }

        private void ScreenFade() {
            int msec;
            float t;

            if (0 == fadeTime) {
                return;
            }

            msec = fadeTime - gameLocal.realClientTime;

            if (msec <= 0) {
                fadeColor.oSet(fadeToColor);
                if (fadeColor.oGet(3) == 0.0f) {
                    fadeTime = 0;
                }
            } else {
                t = (float) msec * fadeRate;
                fadeColor.oSet(fadeFromColor.oMultiply(t).oPlus(fadeToColor.oMultiply(1.0f - t)));
            }

            if (fadeColor.oGet(3) != 0.0f) {
                renderSystem.SetColor4(fadeColor.oGet(0), fadeColor.oGet(1), fadeColor.oGet(2), fadeColor.oGet(3));
                renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, declManager.FindMaterial("_white"));
            }
        }

        private screenBlob_t GetScreenBlob() {
            screenBlob_t oldest = screenBlobs[0];

            for (int i = 1; i < MAX_SCREEN_BLOBS; i++) {
                if (screenBlobs[i].finishTime < oldest.finishTime) {
                    oldest = screenBlobs[i];
                }
            }
            return oldest;
        }

    };
}
