package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.DeclManager.declManager;
import neo.framework.File_h.idFile;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorWhite;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.RAD2DEG;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class GameBearShootWindow {

    public static final int BEAR_GRAVITY = 240;
    public static final float BEAR_SIZE = 24.f;
    public static final float BEAR_SHRINK_TIME = 2000.f;
//
    public static final float MAX_WINDFORCE = 100.f;
//
    public static final idCVar bearTurretAngle = new idCVar("bearTurretAngle", "0", CVAR_FLOAT, "");
    public static final idCVar bearTurretForce = new idCVar("bearTurretForce", "200", CVAR_FLOAT, "");
//

    /*
     *****************************************************************************
     * BSEntity	
     ****************************************************************************
     */
    public static class BSEntity {

        public idMaterial material;
        public idStr materialName;
        public float width, height;
        public boolean visible;
//
        public idVec4 entColor;
        public idVec2 position;
        public float rotation;
        public float rotationSpeed;
        public idVec2 velocity;
//
        public boolean fadeIn;
        public boolean fadeOut;
//
        public idGameBearShootWindow game;
//

        public BSEntity(idGameBearShootWindow _game) {
            game = _game;
            visible = true;

            entColor = colorWhite;
            materialName = new idStr("");
            material = null;
            width = height = 8;
            rotation = 0.f;
            rotationSpeed = 0.f;
            fadeIn = false;
            fadeOut = false;

            position.Zero();
            velocity.Zero();
        }
//	// virtual				~BSEntity();
//

        public void WriteToSaveGame(idFile savefile) {

            game.WriteSaveGameString(materialName.toString(), savefile);

            savefile.WriteFloat(width);
            savefile.WriteFloat(height);
            savefile.WriteBool(visible);

            savefile.Write(entColor);
            savefile.Write(position);
            savefile.WriteFloat(rotation);
            savefile.WriteFloat(rotationSpeed);
            savefile.Write(velocity);

            savefile.WriteBool(fadeIn);
            savefile.WriteBool(fadeOut);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBearShootWindow _game) {
            game = _game;

            game.ReadSaveGameString(materialName, savefile);
            SetMaterial(materialName.toString());

            width = savefile.ReadFloat();
            height = savefile.ReadFloat();
            visible = savefile.ReadBool();

            savefile.Read(entColor);
            savefile.Read(position);
            rotation = savefile.ReadFloat();
            rotationSpeed = savefile.ReadFloat();
            savefile.Read(velocity);

            fadeIn = savefile.ReadBool();
            fadeOut = savefile.ReadBool();
        }

        public void SetMaterial(final String name) {
            materialName.oSet(name);
            material = declManager.FindMaterial(name);
            material.SetSort(SS_GUI);
        }

        public void SetSize(float _width, float _height) {
            width = _width;
            height = _height;
        }

        public void SetVisible(boolean isVisible) {
            visible = isVisible;
        }

        public void Update(float timeslice) {

            if (!visible) {
                return;
            }

            // Fades
            if (fadeIn && entColor.w < 1.f) {
                entColor.w += 1 * timeslice;
                if (entColor.w >= 1.f) {
                    entColor.w = 1.f;
                    fadeIn = false;
                }
            }
            if (fadeOut && entColor.w > 0.f) {
                entColor.w -= 1 * timeslice;
                if (entColor.w <= 0.f) {
                    entColor.w = 0.f;
                    fadeOut = false;
                }
            }

            // Move the entity
            position.oPluSet(velocity.oMultiply(timeslice));

            // Rotate Entity
            rotation += rotationSpeed * timeslice;
        }

        public void Draw(idDeviceContext dc) {
            if (visible) {
                dc.DrawMaterialRotated(position.x, position.y, width, height, material, entColor, 1.0f, 1.0f, (float) DEG2RAD(rotation));
            }
        }
    };

    /*
     *****************************************************************************
     * idGameBearShootWindow
     ****************************************************************************
     */
    public static class idGameBearShootWindow extends idWindow {

        public idGameBearShootWindow(idUserInterfaceLocal gui) {
            super();
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }

        public idGameBearShootWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super();
            this.dc = dc;
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }
        // ~idGameBearShootWindow();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            gamerunning.WriteToSaveGame(savefile);
            onFire.WriteToSaveGame(savefile);
            onContinue.WriteToSaveGame(savefile);
            onNewGame.WriteToSaveGame(savefile);

            savefile.WriteFloat(timeSlice);
            savefile.WriteFloat(timeRemaining);
            savefile.WriteBool(gameOver);

            savefile.WriteInt(currentLevel);
            savefile.WriteInt(goalsHit);
            savefile.WriteBool(updateScore);
            savefile.WriteBool(bearHitTarget);

            savefile.WriteFloat(bearScale);
            savefile.WriteBool(bearIsShrinking);
            savefile.WriteInt(bearShrinkStartTime);

            savefile.WriteFloat(turretAngle);
            savefile.WriteFloat(turretForce);

            savefile.WriteFloat(windForce);
            savefile.WriteInt(windUpdateTime);

            int numberOfEnts = entities.Num();
            savefile.WriteInt(numberOfEnts);

            for (int i = 0; i < numberOfEnts; i++) {
                entities.oGet(i).WriteToSaveGame(savefile);
            }

            int index;
            index = entities.FindIndex(turret);
            savefile.WriteInt(index);
            index = entities.FindIndex(bear);
            savefile.WriteInt(index);
            index = entities.FindIndex(helicopter);
            savefile.WriteInt(index);
            index = entities.FindIndex(goal);
            savefile.WriteInt(index);
            index = entities.FindIndex(wind);
            savefile.WriteInt(index);
            index = entities.FindIndex(gunblast);
            savefile.WriteInt(index);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            // Remove all existing entities
            entities.DeleteContents(true);

            gamerunning.ReadFromSaveGame(savefile);
            onFire.ReadFromSaveGame(savefile);
            onContinue.ReadFromSaveGame(savefile);
            onNewGame.ReadFromSaveGame(savefile);

            timeSlice = savefile.ReadFloat();
            timeRemaining = savefile.ReadInt();
            gameOver = savefile.ReadBool();

            currentLevel = savefile.ReadInt();
            goalsHit = savefile.ReadInt();
            updateScore = savefile.ReadBool();
            bearHitTarget = savefile.ReadBool();

            bearScale = savefile.ReadFloat();
            bearIsShrinking = savefile.ReadBool();
            bearShrinkStartTime = savefile.ReadInt();

            turretAngle = savefile.ReadFloat();
            turretForce = savefile.ReadFloat();

            windForce = savefile.ReadFloat();
            windUpdateTime = savefile.ReadInt();

            int numberOfEnts;
            numberOfEnts = savefile.ReadInt();

            for (int i = 0; i < numberOfEnts; i++) {
                BSEntity ent;

                ent = new BSEntity(this);
                ent.ReadFromSaveGame(savefile, this);
                entities.Append(ent);
            }

            int index;
            index = savefile.ReadInt();
            turret = entities.oGet(index);
            index = savefile.ReadInt();
            bear = entities.oGet(index);
            index = savefile.ReadInt();
            helicopter = entities.oGet(index);
            index = savefile.ReadInt();
            goal = entities.oGet(index);
            index = savefile.ReadInt();
            wind = entities.oGet(index);
            index = savefile.ReadInt();
            gunblast = entities.oGet(index);
        }

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            int key = event.evValue;

            // need to call this to allow proper focus and capturing on embedded children
            final String ret = super.HandleEvent(event, updateVisuals);

            if (event.evType == SE_KEY) {

                if (0 == event.evValue2) {
                    return ret;
                }
                if (key == K_MOUSE1) {
                    // Mouse was clicked	
                } else {
                    return ret;
                }
            }

            return ret;
        }

        @Override
        public void PostParse() {
            super.PostParse();
        }

        @Override
        public void Draw(int time, float x, float y) {
            int i;

            //Update the game every frame before drawing
            UpdateGame();

            for (i = entities.Num() - 1; i >= 0; i--) {
                entities.oGet(i).Draw(dc);
            }
        }

        public String Activate(boolean activate) {
            return "";
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            idWinVar retVar = null;

            if (idStr.Icmp(_name, "gamerunning") == 0) {
                retVar = gamerunning;
            } else if (idStr.Icmp(_name, "onFire") == 0) {
                retVar = onFire;
            } else if (idStr.Icmp(_name, "onContinue") == 0) {
                retVar = onContinue;
            } else if (idStr.Icmp(_name, "onNewGame") == 0) {
                retVar = onNewGame;
            }

            if (retVar != null) {
                return retVar;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void CommonInit() {
            BSEntity ent;

            // Precache sounds
            declManager.FindSound("arcade_beargroan");
            declManager.FindSound("arcade_sargeshoot");
            declManager.FindSound("arcade_balloonpop");
            declManager.FindSound("arcade_levelcomplete1");

            // Precache dynamically used materials
            declManager.FindMaterial("game/bearshoot/helicopter_broken");
            declManager.FindMaterial("game/bearshoot/goal_dead");
            declManager.FindMaterial("game/bearshoot/gun_blast");

            ResetGameState();

            ent = new BSEntity(this);
            turret = ent;
            ent.SetMaterial("game/bearshoot/turret");
            ent.SetSize(272, 144);
            ent.position.x = -44;
            ent.position.y = 260;
            entities.Append(ent);

            ent = new BSEntity(this);
            ent.SetMaterial("game/bearshoot/turret_base");
            ent.SetSize(144, 160);
            ent.position.x = 16;
            ent.position.y = 280;
            entities.Append(ent);

            ent = new BSEntity(this);
            bear = ent;
            ent.SetMaterial("game/bearshoot/bear");
            ent.SetSize(BEAR_SIZE, BEAR_SIZE);
            ent.SetVisible(false);
            ent.position.x = 0;
            ent.position.y = 0;
            entities.Append(ent);

            ent = new BSEntity(this);
            helicopter = ent;
            ent.SetMaterial("game/bearshoot/helicopter");
            ent.SetSize(64, 64);
            ent.position.x = 550;
            ent.position.y = 100;
            entities.Append(ent);

            ent = new BSEntity(this);
            goal = ent;
            ent.SetMaterial("game/bearshoot/goal");
            ent.SetSize(64, 64);
            ent.position.x = 550;
            ent.position.y = 164;
            entities.Append(ent);

            ent = new BSEntity(this);
            wind = ent;
            ent.SetMaterial("game/bearshoot/wind");
            ent.SetSize(100, 40);
            ent.position.x = 500;
            ent.position.y = 430;
            entities.Append(ent);

            ent = new BSEntity(this);
            gunblast = ent;
            ent.SetMaterial("game/bearshoot/gun_blast");
            ent.SetSize(64, 64);
            ent.SetVisible(false);
            entities.Append(ent);
        }

        private void ResetGameState() {
            gamerunning.data = false;
            gameOver = false;
            onFire.data = false;
            onContinue.data = false;
            onNewGame.data = false;

            // Game moves forward 16 milliseconds every frame
            timeSlice = 0.016f;
            timeRemaining = 60f;
            goalsHit = 0;
            updateScore = false;
            bearHitTarget = false;
            currentLevel = 1;
            turretAngle = 0f;
            turretForce = 200f;
            windForce = 0f;
            windUpdateTime = 0;

            bearIsShrinking = false;
            bearShrinkStartTime = 0;
            bearScale = 1f;
        }

        private void UpdateBear() {
            int time = gui.GetTime();
            boolean startShrink = false;

            // Apply gravity
            bear.velocity.y += BEAR_GRAVITY * timeSlice;

            // Apply wind
            bear.velocity.x += windForce * timeSlice;

            // Check for collisions
            if (!bearHitTarget && !gameOver) {
                idVec2 bearCenter = new idVec2();
                boolean collision = false;

                bearCenter.x = bear.position.x + bear.width / 2;
                bearCenter.y = bear.position.y + bear.height / 2;

                if (bearCenter.x > (helicopter.position.x + 16) && bearCenter.x < (helicopter.position.x + helicopter.width - 29)) {
                    if (bearCenter.y > (helicopter.position.y + 12) && bearCenter.y < (helicopter.position.y + helicopter.height - 7)) {
                        collision = true;
                    }
                }

                if (collision) {
                    // balloons pop and bear tumbles to ground
                    helicopter.SetMaterial("game/bearshoot/helicopter_broken");
                    helicopter.velocity.y = 230.f;
                    goal.velocity.y = 230.f;
                    session.sw.PlayShaderDirectly("arcade_balloonpop");

                    bear.SetVisible(false);
                    if (bear.velocity.x > 0) {
                        bear.velocity.x *= -1.f;
                    }
                    bear.velocity.oMulSet(0.666f);
                    bearHitTarget = true;
                    updateScore = true;
                    startShrink = true;
                }
            }

            // Check for ground collision
            if (bear.position.y > 380) {
                bear.position.y = 380;

                if (bear.velocity.Length() < 25) {
                    bear.velocity.Zero();
                } else {
                    startShrink = true;

                    bear.velocity.y *= -1.f;
                    bear.velocity.oMulSet(0.5f);

                    if (bearScale != 0) {
                        session.sw.PlayShaderDirectly("arcade_balloonpop");
                    }
                }
            }

            // Bear rotation is based on velocity
            float angle;
            idVec2 dir;

            dir = bear.velocity;
            dir.NormalizeFast();

            angle = (float) RAD2DEG(Math.atan2(dir.x, dir.y));
            bear.rotation = angle - 90;

            // Update Bear scale
            if (bear.position.x > 650) {
                startShrink = true;
            }

            if (!bearIsShrinking && bearScale != 0 && startShrink) {
                bearShrinkStartTime = time;
                bearIsShrinking = true;
            }

            if (bearIsShrinking) {
                if (bearHitTarget) {
                    bearScale = 1 - ((float) (time - bearShrinkStartTime) / BEAR_SHRINK_TIME);
                } else {
                    bearScale = 1 - ((float) (time - bearShrinkStartTime) / 750);
                }
                bearScale *= BEAR_SIZE;
                bear.SetSize(bearScale, bearScale);

                if (bearScale < 0) {
                    gui.HandleNamedEvent("EnableFireButton");
                    bearIsShrinking = false;
                    bearScale = 0.f;

                    if (bearHitTarget) {
                        goal.SetMaterial("game/bearshoot/goal");
                        goal.position.x = 550;
                        goal.position.y = 164;
                        goal.velocity.Zero();
                        goal.velocity.y = (currentLevel - 1) * 30;
                        goal.entColor.w = 0.f;
                        goal.fadeIn = true;
                        goal.fadeOut = false;

                        helicopter.SetVisible(true);
                        helicopter.SetMaterial("game/bearshoot/helicopter");
                        helicopter.position.x = 550;
                        helicopter.position.y = 100;
                        helicopter.velocity.Zero();
                        helicopter.velocity.y = goal.velocity.y;
                        helicopter.entColor.w = 0.f;
                        helicopter.fadeIn = true;
                        helicopter.fadeOut = false;
                    }
                }
            }
        }

        private void UpdateHelicopter() {

            if (bearHitTarget && bearIsShrinking) {
                if (helicopter.velocity.y != 0 && helicopter.position.y > 264) {
                    helicopter.velocity.y = 0;
                    goal.velocity.y = 0;

                    helicopter.SetVisible(false);
                    goal.SetMaterial("game/bearshoot/goal_dead");
                    session.sw.PlayShaderDirectly("arcade_beargroan", 1);

                    helicopter.fadeOut = true;
                    goal.fadeOut = true;
                }
            } else if (currentLevel > 1) {
                int height = (int) helicopter.position.y;
                float speed = (currentLevel - 1) * 30;

                if (height > 240) {
                    helicopter.velocity.y = -speed;
                    goal.velocity.y = -speed;
                } else if (height < 30) {
                    helicopter.velocity.y = speed;
                    goal.velocity.y = speed;
                }
            }
        }

        private void UpdateTurret() {
            idVec2 pt = new idVec2();
            idVec2 turretOrig = new idVec2();
            idVec2 right = new idVec2();
            float dot, angle;

            pt.x = gui.CursorX();
            pt.y = gui.CursorY();
            turretOrig.Set(80.f, 348.f);

            pt = pt.oMinus(turretOrig);
            pt.NormalizeFast();

            right.x = 1.f;
            right.y = 0.f;

            dot = pt.oMultiply(right);

            angle = (float) RAD2DEG(Math.acos(dot));

            turretAngle = idMath.ClampFloat(0.f, 90.f, angle);
        }

        private void UpdateButtons() {

            if (onFire.oCastBoolean()) {
                idVec2 vec = new idVec2();

                gui.HandleNamedEvent("DisableFireButton");
                session.sw.PlayShaderDirectly("arcade_sargeshoot");

                bear.SetVisible(true);
                bearScale = 1.f;
                bear.SetSize(BEAR_SIZE, BEAR_SIZE);

                vec.x = idMath.Cos(DEG2RAD(turretAngle));
                vec.x += (1 - vec.x) * 0.18f;
                vec.y = -idMath.Sin(DEG2RAD(turretAngle));

                turretForce = bearTurretForce.GetFloat();

                bear.position.x = 80 + (96 * vec.x);
                bear.position.y = 334 + (96 * vec.y);
                bear.velocity.x = vec.x * turretForce;
                bear.velocity.y = vec.y * turretForce;

                gunblast.position.x = 55 + (96 * vec.x);
                gunblast.position.y = 310 + (100 * vec.y);
                gunblast.SetVisible(true);
                gunblast.entColor.w = 1.f;
                gunblast.rotation = turretAngle;
                gunblast.fadeOut = true;

                bearHitTarget = false;

                onFire.data = false;
            }
        }

        private void UpdateGame() {
            int i;

            if (onNewGame.oCastBoolean()) {
                ResetGameState();

                goal.position.x = 550;
                goal.position.y = 164;
                goal.velocity.Zero();
                helicopter.position.x = 550;
                helicopter.position.y = 100;
                helicopter.velocity.Zero();
                bear.SetVisible(false);

                bearTurretAngle.SetFloat(0.f);
                bearTurretForce.SetFloat(200.f);

                gamerunning.data = true;
            }
            if (onContinue.oCastBoolean()) {
                gameOver = false;
                timeRemaining = 60.f;

                onContinue.data = false;
            }

            if (gamerunning.oCastBoolean() == true) {
                int current_time = gui.GetTime();
                idRandom rnd = new idRandom(current_time);

                // Check for button presses
                UpdateButtons();

                if (bear != null) {
                    UpdateBear();
                }
                if (helicopter != null && goal != null) {
                    UpdateHelicopter();
                }

                // Update Wind
                if (windUpdateTime < current_time) {
                    float scale;
                    int width;

                    windForce = rnd.CRandomFloat() * (MAX_WINDFORCE * 0.75f);
                    if (windForce > 0) {
                        windForce += (MAX_WINDFORCE * 0.25f);
                        wind.rotation = 0;
                    } else {
                        windForce -= (MAX_WINDFORCE * 0.25f);
                        wind.rotation = 180;
                    }

                    scale = 1f - ((MAX_WINDFORCE - idMath.Fabs(windForce)) / MAX_WINDFORCE);
                    width = (int) (100 * scale);

                    if (windForce < 0) {
                        wind.position.x = 500 - width + 1;
                    } else {
                        wind.position.x = 500;
                    }
                    wind.SetSize(width, 40);

                    windUpdateTime = current_time + 7000 + rnd.RandomInt(5000);
                }

                // Update turret rotation angle
                if (turret != null) {
                    turretAngle = bearTurretAngle.GetFloat();
                    turret.rotation = turretAngle;
                }

                for (i = 0; i < entities.Num(); i++) {
                    entities.oGet(i).Update(timeSlice);
                }

                // Update countdown timer
                timeRemaining -= timeSlice;
                timeRemaining = idMath.ClampFloat(0.f, 99999.f, timeRemaining);
                gui.SetStateString("time_remaining", va("%2.1f", timeRemaining));

                if (timeRemaining <= 0.f && !gameOver) {
                    gameOver = true;
                    updateScore = true;
                }

                if (updateScore) {
                    UpdateScore();
                    updateScore = false;
                }
            }
        }

        private void UpdateScore() {

            if (gameOver) {
                gui.HandleNamedEvent("GameOver");
                return;
            }

            goalsHit++;
            gui.SetStateString("player_score", va("%d", goalsHit));

            // Check for level progression
            if (0 == (goalsHit % 5)) {
                currentLevel++;
                gui.SetStateString("current_level", va("%d", currentLevel));
                session.sw.PlayShaderDirectly("arcade_levelcomplete1", 3);

                timeRemaining += 30;
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "gamerunning") == 0) {
                gamerunning.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onFire") == 0) {
                onFire.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onContinue") == 0) {
                onContinue.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onNewGame") == 0) {
                onNewGame.oSet(src.ParseBool());
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }
//
//
        private idWinBool gamerunning;
        private idWinBool onFire;
        private idWinBool onContinue;
        private idWinBool onNewGame;
//
        private float timeSlice;
        private float timeRemaining;
        private boolean gameOver;
//
        private int currentLevel;
        private int goalsHit;
        private boolean updateScore;
        private boolean bearHitTarget;
//
        private float bearScale;
        private boolean bearIsShrinking;
        private int bearShrinkStartTime;
//
        private float turretAngle;
        private float turretForce;
//
        private float windForce;
        private int windUpdateTime;
//
        private idList<BSEntity> entities;
//
        private BSEntity turret;
        private BSEntity bear;
        private BSEntity helicopter;
        private BSEntity goal;
        private BSEntity wind;
        private BSEntity gunblast;
    };
}
