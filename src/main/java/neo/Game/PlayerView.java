package neo.Game;

import static java.lang.Math.sin;
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
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;

import neo.Game.Player.idPlayer;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.renderView_s;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
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
    }
    public static final int MAX_SCREEN_BLOBS    = 8;
    public static final int IMPULSE_DELAY       = 150;

    public static class idPlayerView {

        private final screenBlob_t[] screenBlobs = new screenBlob_t[MAX_SCREEN_BLOBS];
        //
        private int          dvFinishTime;      // double vision will be stopped at this time
        private final idMaterial   dvMaterial;        // material to take the double vision screen shot
        //
        private int          kickFinishTime;    // view kick will be stopped at this time
        private final idAngles     kickAngles;
        //
        private boolean      bfgVision;
        //
        private final idMaterial   tunnelMaterial;    // health tunnel vision
        private final idMaterial   armorMaterial;     // armor damage view effect
        private final idMaterial   berserkMaterial;   // berserk effect
        private final idMaterial   irGogglesMaterial; // ir effect
        private final idMaterial   bloodSprayMaterial;// blood spray
        private final idMaterial   bfgMaterial;       // when targeted with BFG
        private final idMaterial   lagoMaterial;      // lagometer drawing
        private float        lastDamageTime;    // accentuate the tunnel effect for a while
        //
        private final idVec4       fadeColor;         // fade color
        private final idVec4       fadeToColor;       // color to fade to
        private final idVec4       fadeFromColor;     // color to fade from
        private float        fadeRate;          // fade rate
        private int          fadeTime;          // fade time
        //
        private final idAngles     shakeAng;          // from the sound sources
        //
        private idPlayer     player;
        private final renderView_s view;
        //
        //

        public idPlayerView() {
//	memset( screenBlobs, 0, sizeof( screenBlobs ) );
//	memset( &view, 0, sizeof( view ) );
            this.view = new renderView_s();
            this.player = null;
            this.dvMaterial = declManager.FindMaterial("_scratch");
            this.tunnelMaterial = declManager.FindMaterial("textures/decals/tunnel");
            this.armorMaterial = declManager.FindMaterial("armorViewEffect");
            this.berserkMaterial = declManager.FindMaterial("textures/decals/berserk");
            this.irGogglesMaterial = declManager.FindMaterial("textures/decals/irblend");
            this.bloodSprayMaterial = declManager.FindMaterial("textures/decals/bloodspray");
            this.bfgMaterial = declManager.FindMaterial("textures/decals/bfgvision");
            this.lagoMaterial = declManager.FindMaterial(LAGO_MATERIAL, false);
            this.bfgVision = false;
            this.dvFinishTime = 0;
            this.kickFinishTime = 0;
            this.kickAngles = new idAngles();
            this.lastDamageTime = 0f;
            this.fadeTime = 0;
            this.fadeRate = 0;
            this.fadeFromColor = new idVec4();
            this.fadeToColor = new idVec4();
            this.fadeColor = new idVec4();
            this.shakeAng = new idAngles();

            ClearEffects();
        }

        public void Save(idSaveGame savefile) {
            int i;
            screenBlob_t blob;

            blob = this.screenBlobs[0];
            for (i = 0; i < MAX_SCREEN_BLOBS; blob = this.screenBlobs[++i]) {
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

            savefile.WriteInt(this.dvFinishTime);
            savefile.WriteMaterial(this.dvMaterial);
            savefile.WriteInt(this.kickFinishTime);
            savefile.WriteAngles(this.kickAngles);
            savefile.WriteBool(this.bfgVision);

            savefile.WriteMaterial(this.tunnelMaterial);
            savefile.WriteMaterial(this.armorMaterial);
            savefile.WriteMaterial(this.berserkMaterial);
            savefile.WriteMaterial(this.irGogglesMaterial);
            savefile.WriteMaterial(this.bloodSprayMaterial);
            savefile.WriteMaterial(this.bfgMaterial);
            savefile.WriteFloat(this.lastDamageTime);

            savefile.WriteVec4(this.fadeColor);
            savefile.WriteVec4(this.fadeToColor);
            savefile.WriteVec4(this.fadeFromColor);
            savefile.WriteFloat(this.fadeRate);
            savefile.WriteInt(this.fadeTime);

            savefile.WriteAngles(this.shakeAng);

            savefile.WriteObject(this.player);
            savefile.WriteRenderView(this.view);
        }

        public void Restore(idRestoreGame savefile) {
            int i;
            screenBlob_t blob;

//            blob = screenBlobs[ 0];
            for (blob = this.screenBlobs[i = 0]; i < MAX_SCREEN_BLOBS; blob = this.screenBlobs[++i]) {
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

            this.dvFinishTime = savefile.ReadInt();
            savefile.ReadMaterial(this.dvMaterial);
            this.kickFinishTime = savefile.ReadInt();
            savefile.ReadAngles(this.kickAngles);
            this.bfgVision = savefile.ReadBool();

            savefile.ReadMaterial(this.tunnelMaterial);
            savefile.ReadMaterial(this.armorMaterial);
            savefile.ReadMaterial(this.berserkMaterial);
            savefile.ReadMaterial(this.irGogglesMaterial);
            savefile.ReadMaterial(this.bloodSprayMaterial);
            savefile.ReadMaterial(this.bfgMaterial);
            this.lastDamageTime = savefile.ReadFloat();

            savefile.ReadVec4(this.fadeColor);
            savefile.ReadVec4(this.fadeToColor);
            savefile.ReadVec4(this.fadeFromColor);
            this.fadeRate = savefile.ReadFloat();
            this.fadeTime = savefile.ReadInt();

            savefile.ReadAngles(this.shakeAng);

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/player);
            savefile.ReadRenderView(this.view);
        }

        public void SetPlayerEntity(idPlayer playerEnt) {
            this.player = playerEnt;
        }

        public void ClearEffects() {
            this.lastDamageTime = MS2SEC(gameLocal.time - 99999);

            this.dvFinishTime = (gameLocal.time - 99999);
            this.kickFinishTime = (gameLocal.time - 99999);

            for (int i = 0; i < MAX_SCREEN_BLOBS; i++) {
                this.screenBlobs[i] = new screenBlob_t();
                this.screenBlobs[i].finishTime = gameLocal.time;
            }

            this.fadeTime = 0;
            this.bfgVision = false;
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
            if ((this.lastDamageTime > 0.0f) && ((SEC2MS(this.lastDamageTime) + IMPULSE_DELAY) > gameLocal.time)) {
                // keep shotgun from obliterating the view
                return;
            }

            final float dvTime = damageDef.GetFloat("dv_time");
            if (dvTime != 0) {
                if (this.dvFinishTime < gameLocal.time) {
                    this.dvFinishTime = gameLocal.time;
                }
                this.dvFinishTime += g_dvTime.GetFloat() * dvTime;
                // don't let it add up too much in god mode
                if (this.dvFinishTime > (gameLocal.time + 5000)) {
                    this.dvFinishTime = gameLocal.time + 5000;
                }
            }

            //
            // head angle kick
            //
            final float kickTime = damageDef.GetFloat("kick_time");
            if (kickTime != 0) {
                this.kickFinishTime = (int) (gameLocal.time + (g_kickTime.GetFloat() * kickTime));

                // forward / back kick will pitch view
                this.kickAngles.oSet(0, localKickDir.oGet(0));

                // side kick will yaw view
                this.kickAngles.oSet(1, localKickDir.oGet(1) * 0.5f);

                // up / down kick will pitch view
                this.kickAngles.oPluSet(0, localKickDir.oGet(2));

                // roll will come from  side
                this.kickAngles.oSet(2, localKickDir.oGet(1));

                final float kickAmplitude = damageDef.GetFloat("kick_amplitude");
                if (kickAmplitude != 0) {
                    this.kickAngles.oMulSet(kickAmplitude);
                }
            }

            //
            // screen blob
            //
            final float blobTime = damageDef.GetFloat("blob_time");
            if (blobTime != 0) {
                final screenBlob_t blob = GetScreenBlob();
                blob.startFadeTime = gameLocal.time;
                blob.finishTime = (int) (gameLocal.time + (blobTime * g_blobTime.GetFloat()));

                final String materialName = damageDef.GetString("mtr_blob");
                blob.material = declManager.FindMaterial(materialName);
                blob.x = damageDef.GetFloat("blob_x");
                blob.x += (gameLocal.random.RandomInt() & 63) - 32;
                blob.y = damageDef.GetFloat("blob_y");
                blob.y += (gameLocal.random.RandomInt() & 63) - 32;

                final float scale = (256 + ((gameLocal.random.RandomInt() & 63) - 32)) / 256.0f;
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
            this.lastDamageTime = MS2SEC(gameLocal.time);

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
            if ((recoilTime != 0) && (this.kickFinishTime < gameLocal.time)) {
                final idAngles angles = new idAngles();
                weaponDef.GetAngles("recoilAngles", "5 0 0", angles);
                this.kickAngles.oSet(angles);
                final int finish = (int) (gameLocal.time + (g_kickTime.GetFloat() * recoilTime));
                this.kickFinishTime = finish;
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

            if (gameLocal.time < this.kickFinishTime) {
                final float offset = this.kickFinishTime - gameLocal.time;

                ang = this.kickAngles.oMultiply(offset * offset * g_kickAmplitude.GetFloat());

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
            return this.shakeAng.ToMat3();
        }

        public void CalculateShake() {
//            idVec3 origin, matrix;

            final float shakeVolume = gameSoundWorld.CurrentShakeAmplitudeForPosition(gameLocal.time, this.player.firstPersonViewOrigin);
            //
            // shakeVolume should somehow be molded into an angle here
            // it should be thought of as being in the range 0.0 . 1.0, although
            // since CurrentShakeAmplitudeForPosition() returns all the shake sounds
            // the player can hear, it can go over 1.0 too.
            //
            this.shakeAng.oSet(0, gameLocal.random.CRandomFloat() * shakeVolume);
            this.shakeAng.oSet(1, gameLocal.random.CRandomFloat() * shakeVolume);
            this.shakeAng.oSet(2, gameLocal.random.CRandomFloat() * shakeVolume);
        }

        // this may involve rendering to a texture and displaying
        // that with a warp model or in double vision mode
        public void RenderPlayerView(idUserInterface hud) {
            final renderView_s view = this.player.GetRenderView();

            if (g_skipViewEffects.GetBool()) {
                SingleView(hud, view);
            } else {
                if ((this.player.GetInfluenceMaterial() != null) || (this.player.GetInfluenceEntity() != null)) {
                    InfluenceVision(hud, view);
                } else if (gameLocal.time < this.dvFinishTime) {
                    DoubleVision(hud, view, this.dvFinishTime - gameLocal.time);
                } else if (this.player.PowerUpActive(BERSERK)) {
                    BerserkVision(hud, view);
                } else {
                    SingleView(hud, view);
                }
                ScreenFade();
            }

            if (net_clientLagOMeter.GetBool() && (this.lagoMaterial != null) && gameLocal.isClient) {
                renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
                renderSystem.DrawStretchPic(10.0f, 380.0f, 64.0f, 64.0f, 0.0f, 0.0f, 1.0f, 1.0f, this.lagoMaterial);
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

            if (0 == this.fadeTime) {
                this.fadeFromColor.Set(0.0f, 0.0f, 0.0f, 1.0f - color.oGet(3));
            } else {
                this.fadeFromColor.oSet(this.fadeColor);
            }
            this.fadeToColor.oSet(color);

            if (time <= 0) {
                this.fadeRate = 0;
                time = 0;
                this.fadeColor.oSet(this.fadeToColor);
            } else {
                this.fadeRate = 1.0f / time;
            }

            if ((gameLocal.realClientTime == 0) && (time == 0)) {
                this.fadeTime = 1;
            } else {
                this.fadeTime = gameLocal.realClientTime + time;
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
            this.fadeFromColor.oSet(colorWhite);
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
            this.bfgVision = b;
        }

        private void SingleView(idUserInterface hud, final renderView_s view) {

            // normal rendering
            if (null == view) {
                return;
            }

            // place the sound origin for the player
            gameSoundWorld.PlaceListener(view.vieworg, view.viewaxis, this.player.entityNumber + 1, gameLocal.time, new idStr(hud != null ? hud.State().GetString("location") : "Undefined"));

            // if the objective system is up, don't do normal drawing
            if (this.player.objectiveSystemOpen) {
                this.player.objectiveSystem.Redraw(gameLocal.time);
                return;
            }

            // hack the shake in at the very last moment, so it can't cause any consistency problems
            final renderView_s hackedView = new renderView_s(view);
            hackedView.viewaxis = hackedView.viewaxis.oMultiply(ShakeAxis());
//            hackedView.viewaxis = idMat3.getMat3_identity();//HACKME::10
//            hackedView.viewaxis = new idMat3(-1.0f, -3.8941437E-7f, -0.0f, 3.8941437E-7f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f);

            gameRenderWorld.RenderScene(hackedView);

            if (this.player.spectating) {
                return;
            }

            // draw screen blobs
            if (!pm_thirdPerson.GetBool() && !g_skipViewEffects.GetBool()) {
                for (int i = 0; i < MAX_SCREEN_BLOBS; i++) {
                    final screenBlob_t blob = this.screenBlobs[i];
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
                this.player.DrawHUD(hud);

                // armor impulse feedback
                final float armorPulse = (gameLocal.time - this.player.lastArmorPulse) / 250.0f;

                if ((armorPulse > 0.0f) && (armorPulse < 1.0f)) {
                    renderSystem.SetColor4(1, 1, 1, 1.0f - armorPulse);
                    renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, this.armorMaterial);
                }

                // tunnel vision
                float health;
                if (g_testHealthVision.GetFloat() != 0.0f) {
                    health = g_testHealthVision.GetFloat();
                } else {
                    health = this.player.health;
                }
                float alpha = health / 100.0f;
                if (alpha < 0.0f) {
                    alpha = 0.0f;
                }
                if (alpha > 1.0f) {
                    alpha = 1.0f;
                }

                if (alpha < 1.0f) {
                    renderSystem.SetColor4((this.player.health <= 0.0f) ? MS2SEC(gameLocal.time) : this.lastDamageTime, 1.0f, 1.0f, (this.player.health <= 0.0f) ? 0.0f : alpha);
                    renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, this.tunnelMaterial);
                }

                if (this.player.PowerUpActive(BERSERK)) {
                    final int berserkTime = this.player.inventory.powerupEndTime[ BERSERK] - gameLocal.time;
                    if (berserkTime > 0) {
                        // start fading if within 10 seconds of going away
                        alpha = (berserkTime < 10000) ? (float) berserkTime / 10000 : 1.0f;
                        renderSystem.SetColor4(1.0f, 1.0f, 1.0f, alpha);
                        renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, this.berserkMaterial);
                    }
                }

                if (this.bfgVision) {
                    renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
                    renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, this.bfgMaterial);
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
            final idVec4 color = new idVec4(1, 1, 1, 1);
            if (gameLocal.time < this.player.inventory.powerupEndTime[ BERSERK]) {
                color.y = 0;
                color.z = 0;
            }

            renderSystem.SetColor4(color.x, color.y, color.z, 1.0f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, shift, 1, 1, 0, this.dvMaterial);
            renderSystem.SetColor4(color.x, color.y, color.z, 0.5f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, 1 - shift, 0, this.dvMaterial);
        }

        private void BerserkVision(idUserInterface hud, final renderView_s view) {
            renderSystem.CropRenderSize(512, 256, true);
            SingleView(hud, view);
            renderSystem.CaptureRenderToImage("_scratch");
            renderSystem.UnCrop();
            renderSystem.SetColor4(1.0f, 1.0f, 1.0f, 1.0f);
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 1, 1, 0, this.dvMaterial);
        }

        private void InfluenceVision(idUserInterface hud, final renderView_s view) {

            float distance;
            float pct = 1.0f;
            if (this.player.GetInfluenceEntity() != null) {
                distance = (this.player.GetInfluenceEntity().GetPhysics().GetOrigin().oMinus(this.player.GetPhysics().GetOrigin())).Length();
                if ((this.player.GetInfluenceRadius() != 0.0f) && (distance < this.player.GetInfluenceRadius())) {
//			pct = distance / player.GetInfluenceRadius();//TODO:wtf?
                    pct = 1.0f - idMath.ClampFloat(0.0f, 1.0f, pct);
                }
            }
            if (this.player.GetInfluenceMaterial() != null) {
                SingleView(hud, view);
                renderSystem.CaptureRenderToImage("_currentRender");
                renderSystem.SetColor4(1.0f, 1.0f, 1.0f, pct);
                renderSystem.DrawStretchPic(0.0f, 0.0f, 640.0f, 480.0f, 0.0f, 0.0f, 1.0f, 1.0f, this.player.GetInfluenceMaterial());
            } else if (this.player.GetInfluenceEntity() == null) {
                SingleView(hud, view);
//		return;
            } else {
                final int offset = (int) (25 + sin(gameLocal.time));
                DoubleVision(hud, view, (int) (pct * offset));
            }
        }

        private void ScreenFade() {
            int msec;
            float t;

            if (0 == this.fadeTime) {
                return;
            }

            msec = this.fadeTime - gameLocal.realClientTime;

            if (msec <= 0) {
                this.fadeColor.oSet(this.fadeToColor);
                if (this.fadeColor.oGet(3) == 0.0f) {
                    this.fadeTime = 0;
                }
            } else {
                t = msec * this.fadeRate;
                this.fadeColor.oSet(this.fadeFromColor.oMultiply(t).oPlus(this.fadeToColor.oMultiply(1.0f - t)));
            }

            if (this.fadeColor.oGet(3) != 0.0f) {
                renderSystem.SetColor4(this.fadeColor.oGet(0), this.fadeColor.oGet(1), this.fadeColor.oGet(2), this.fadeColor.oGet(3));
                renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, declManager.FindMaterial("_white"));
            }
        }

        private screenBlob_t GetScreenBlob() {
            screenBlob_t oldest = this.screenBlobs[0];

            for (int i = 1; i < MAX_SCREEN_BLOBS; i++) {
                if (this.screenBlobs[i].finishTime < oldest.finishTime) {
                    oldest = this.screenBlobs[i];
                }
            }
            return oldest;
        }

    }
}
