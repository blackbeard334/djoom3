package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.RAD2DEG;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;

import neo.Renderer.Material.idMaterial;
import neo.framework.CVarSystem.idCVar;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec4;
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
            this.game = _game;
            this.visible = true;

            this.entColor = colorWhite;
            this.materialName = new idStr("");
            this.material = null;
            this.width = this.height = 8;
            this.rotation = 0.f;
            this.rotationSpeed = 0.f;
            this.fadeIn = false;
            this.fadeOut = false;

            this.position.Zero();
            this.velocity.Zero();
        }
//	// virtual				~BSEntity();
//

        public void WriteToSaveGame(idFile savefile) {

            this.game.WriteSaveGameString(this.materialName.toString(), savefile);

            savefile.WriteFloat(this.width);
            savefile.WriteFloat(this.height);
            savefile.WriteBool(this.visible);

            savefile.Write(this.entColor);
            savefile.Write(this.position);
            savefile.WriteFloat(this.rotation);
            savefile.WriteFloat(this.rotationSpeed);
            savefile.Write(this.velocity);

            savefile.WriteBool(this.fadeIn);
            savefile.WriteBool(this.fadeOut);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBearShootWindow _game) {
            this.game = _game;

            this.game.ReadSaveGameString(this.materialName, savefile);
            SetMaterial(this.materialName.toString());

            this.width = savefile.ReadFloat();
            this.height = savefile.ReadFloat();
            this.visible = savefile.ReadBool();

            savefile.Read(this.entColor);
            savefile.Read(this.position);
            this.rotation = savefile.ReadFloat();
            this.rotationSpeed = savefile.ReadFloat();
            savefile.Read(this.velocity);

            this.fadeIn = savefile.ReadBool();
            this.fadeOut = savefile.ReadBool();
        }

        public void SetMaterial(final String name) {
            this.materialName.oSet(name);
            this.material = declManager.FindMaterial(name);
            this.material.SetSort(SS_GUI);
        }

        public void SetSize(float _width, float _height) {
            this.width = _width;
            this.height = _height;
        }

        public void SetVisible(boolean isVisible) {
            this.visible = isVisible;
        }

        public void Update(float timeslice) {

            if (!this.visible) {
                return;
            }

            // Fades
            if (this.fadeIn && (this.entColor.w < 1.f)) {
                this.entColor.w += 1 * timeslice;
                if (this.entColor.w >= 1.f) {
                    this.entColor.w = 1.f;
                    this.fadeIn = false;
                }
            }
            if (this.fadeOut && (this.entColor.w > 0.f)) {
                this.entColor.w -= 1 * timeslice;
                if (this.entColor.w <= 0.f) {
                    this.entColor.w = 0.f;
                    this.fadeOut = false;
                }
            }

            // Move the entity
            this.position.oPluSet(this.velocity.oMultiply(timeslice));

            // Rotate Entity
            this.rotation += this.rotationSpeed * timeslice;
        }

        public void Draw(idDeviceContext dc) {
            if (this.visible) {
                dc.DrawMaterialRotated(this.position.x, this.position.y, this.width, this.height, this.material, this.entColor, 1.0f, 1.0f, DEG2RAD(this.rotation));
            }
        }
    }

    /*
     *****************************************************************************
     * idGameBearShootWindow
     ****************************************************************************
     */
    public static class idGameBearShootWindow extends idWindow {

        public idGameBearShootWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idGameBearShootWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
        // ~idGameBearShootWindow();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            this.gamerunning.WriteToSaveGame(savefile);
            this.onFire.WriteToSaveGame(savefile);
            this.onContinue.WriteToSaveGame(savefile);
            this.onNewGame.WriteToSaveGame(savefile);

            savefile.WriteFloat(this.timeSlice);
            savefile.WriteFloat(this.timeRemaining);
            savefile.WriteBool(this.gameOver);

            savefile.WriteInt(this.currentLevel);
            savefile.WriteInt(this.goalsHit);
            savefile.WriteBool(this.updateScore);
            savefile.WriteBool(this.bearHitTarget);

            savefile.WriteFloat(this.bearScale);
            savefile.WriteBool(this.bearIsShrinking);
            savefile.WriteInt(this.bearShrinkStartTime);

            savefile.WriteFloat(this.turretAngle);
            savefile.WriteFloat(this.turretForce);

            savefile.WriteFloat(this.windForce);
            savefile.WriteInt(this.windUpdateTime);

            final int numberOfEnts = this.entities.Num();
            savefile.WriteInt(numberOfEnts);

            for (int i = 0; i < numberOfEnts; i++) {
                this.entities.oGet(i).WriteToSaveGame(savefile);
            }

            int index;
            index = this.entities.FindIndex(this.turret);
            savefile.WriteInt(index);
            index = this.entities.FindIndex(this.bear);
            savefile.WriteInt(index);
            index = this.entities.FindIndex(this.helicopter);
            savefile.WriteInt(index);
            index = this.entities.FindIndex(this.goal);
            savefile.WriteInt(index);
            index = this.entities.FindIndex(this.wind);
            savefile.WriteInt(index);
            index = this.entities.FindIndex(this.gunblast);
            savefile.WriteInt(index);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            // Remove all existing entities
            this.entities.DeleteContents(true);

            this.gamerunning.ReadFromSaveGame(savefile);
            this.onFire.ReadFromSaveGame(savefile);
            this.onContinue.ReadFromSaveGame(savefile);
            this.onNewGame.ReadFromSaveGame(savefile);

            this.timeSlice = savefile.ReadFloat();
            this.timeRemaining = savefile.ReadInt();
            this.gameOver = savefile.ReadBool();

            this.currentLevel = savefile.ReadInt();
            this.goalsHit = savefile.ReadInt();
            this.updateScore = savefile.ReadBool();
            this.bearHitTarget = savefile.ReadBool();

            this.bearScale = savefile.ReadFloat();
            this.bearIsShrinking = savefile.ReadBool();
            this.bearShrinkStartTime = savefile.ReadInt();

            this.turretAngle = savefile.ReadFloat();
            this.turretForce = savefile.ReadFloat();

            this.windForce = savefile.ReadFloat();
            this.windUpdateTime = savefile.ReadInt();

            int numberOfEnts;
            numberOfEnts = savefile.ReadInt();

            for (int i = 0; i < numberOfEnts; i++) {
                BSEntity ent;

                ent = new BSEntity(this);
                ent.ReadFromSaveGame(savefile, this);
                this.entities.Append(ent);
            }

            int index;
            index = savefile.ReadInt();
            this.turret = this.entities.oGet(index);
            index = savefile.ReadInt();
            this.bear = this.entities.oGet(index);
            index = savefile.ReadInt();
            this.helicopter = this.entities.oGet(index);
            index = savefile.ReadInt();
            this.goal = this.entities.oGet(index);
            index = savefile.ReadInt();
            this.wind = this.entities.oGet(index);
            index = savefile.ReadInt();
            this.gunblast = this.entities.oGet(index);
        }

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            final int key = event.evValue;

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

            for (i = this.entities.Num() - 1; i >= 0; i--) {
                this.entities.oGet(i).Draw(this.dc);
            }
        }

        public String Activate(boolean activate) {
            return "";
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            idWinVar retVar = null;

            if (idStr.Icmp(_name, "gamerunning") == 0) {
                retVar = this.gamerunning;
            } else if (idStr.Icmp(_name, "onFire") == 0) {
                retVar = this.onFire;
            } else if (idStr.Icmp(_name, "onContinue") == 0) {
                retVar = this.onContinue;
            } else if (idStr.Icmp(_name, "onNewGame") == 0) {
                retVar = this.onNewGame;
            }

            if (retVar != null) {
                return retVar;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        private void CommonInit() {
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
            this.turret = ent;
            ent.SetMaterial("game/bearshoot/turret");
            ent.SetSize(272, 144);
            ent.position.x = -44;
            ent.position.y = 260;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            ent.SetMaterial("game/bearshoot/turret_base");
            ent.SetSize(144, 160);
            ent.position.x = 16;
            ent.position.y = 280;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            this.bear = ent;
            ent.SetMaterial("game/bearshoot/bear");
            ent.SetSize(BEAR_SIZE, BEAR_SIZE);
            ent.SetVisible(false);
            ent.position.x = 0;
            ent.position.y = 0;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            this.helicopter = ent;
            ent.SetMaterial("game/bearshoot/helicopter");
            ent.SetSize(64, 64);
            ent.position.x = 550;
            ent.position.y = 100;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            this.goal = ent;
            ent.SetMaterial("game/bearshoot/goal");
            ent.SetSize(64, 64);
            ent.position.x = 550;
            ent.position.y = 164;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            this.wind = ent;
            ent.SetMaterial("game/bearshoot/wind");
            ent.SetSize(100, 40);
            ent.position.x = 500;
            ent.position.y = 430;
            this.entities.Append(ent);

            ent = new BSEntity(this);
            this.gunblast = ent;
            ent.SetMaterial("game/bearshoot/gun_blast");
            ent.SetSize(64, 64);
            ent.SetVisible(false);
            this.entities.Append(ent);
        }

        private void ResetGameState() {
            this.gamerunning.data = false;
            this.gameOver = false;
            this.onFire.data = false;
            this.onContinue.data = false;
            this.onNewGame.data = false;

            // Game moves forward 16 milliseconds every frame
            this.timeSlice = 0.016f;
            this.timeRemaining = 60f;
            this.goalsHit = 0;
            this.updateScore = false;
            this.bearHitTarget = false;
            this.currentLevel = 1;
            this.turretAngle = 0f;
            this.turretForce = 200f;
            this.windForce = 0f;
            this.windUpdateTime = 0;

            this.bearIsShrinking = false;
            this.bearShrinkStartTime = 0;
            this.bearScale = 1f;
        }

        private void UpdateBear() {
            final int time = this.gui.GetTime();
            boolean startShrink = false;

            // Apply gravity
            this.bear.velocity.y += BEAR_GRAVITY * this.timeSlice;

            // Apply wind
            this.bear.velocity.x += this.windForce * this.timeSlice;

            // Check for collisions
            if (!this.bearHitTarget && !this.gameOver) {
                final idVec2 bearCenter = new idVec2();
                boolean collision = false;

                bearCenter.x = this.bear.position.x + (this.bear.width / 2);
                bearCenter.y = this.bear.position.y + (this.bear.height / 2);

                if ((bearCenter.x > (this.helicopter.position.x + 16)) && (bearCenter.x < ((this.helicopter.position.x + this.helicopter.width) - 29))) {
                    if ((bearCenter.y > (this.helicopter.position.y + 12)) && (bearCenter.y < ((this.helicopter.position.y + this.helicopter.height) - 7))) {
                        collision = true;
                    }
                }

                if (collision) {
                    // balloons pop and bear tumbles to ground
                    this.helicopter.SetMaterial("game/bearshoot/helicopter_broken");
                    this.helicopter.velocity.y = 230.f;
                    this.goal.velocity.y = 230.f;
                    session.sw.PlayShaderDirectly("arcade_balloonpop");

                    this.bear.SetVisible(false);
                    if (this.bear.velocity.x > 0) {
                        this.bear.velocity.x *= -1.f;
                    }
                    this.bear.velocity.oMulSet(0.666f);
                    this.bearHitTarget = true;
                    this.updateScore = true;
                    startShrink = true;
                }
            }

            // Check for ground collision
            if (this.bear.position.y > 380) {
                this.bear.position.y = 380;

                if (this.bear.velocity.Length() < 25) {
                    this.bear.velocity.Zero();
                } else {
                    startShrink = true;

                    this.bear.velocity.y *= -1.f;
                    this.bear.velocity.oMulSet(0.5f);

                    if (this.bearScale != 0) {
                        session.sw.PlayShaderDirectly("arcade_balloonpop");
                    }
                }
            }

            // Bear rotation is based on velocity
            float angle;
            idVec2 dir;

            dir = this.bear.velocity;
            dir.NormalizeFast();

            angle = RAD2DEG((float) Math.atan2(dir.x, dir.y));
            this.bear.rotation = angle - 90;

            // Update Bear scale
            if (this.bear.position.x > 650) {
                startShrink = true;
            }

            if (!this.bearIsShrinking && (this.bearScale != 0) && startShrink) {
                this.bearShrinkStartTime = time;
                this.bearIsShrinking = true;
            }

            if (this.bearIsShrinking) {
                if (this.bearHitTarget) {
                    this.bearScale = 1 - ((time - this.bearShrinkStartTime) / BEAR_SHRINK_TIME);
                } else {
                    this.bearScale = 1 - ((float) (time - this.bearShrinkStartTime) / 750);
                }
                this.bearScale *= BEAR_SIZE;
                this.bear.SetSize(this.bearScale, this.bearScale);

                if (this.bearScale < 0) {
                    this.gui.HandleNamedEvent("EnableFireButton");
                    this.bearIsShrinking = false;
                    this.bearScale = 0.f;

                    if (this.bearHitTarget) {
                        this.goal.SetMaterial("game/bearshoot/goal");
                        this.goal.position.x = 550;
                        this.goal.position.y = 164;
                        this.goal.velocity.Zero();
                        this.goal.velocity.y = (this.currentLevel - 1) * 30;
                        this.goal.entColor.w = 0.f;
                        this.goal.fadeIn = true;
                        this.goal.fadeOut = false;

                        this.helicopter.SetVisible(true);
                        this.helicopter.SetMaterial("game/bearshoot/helicopter");
                        this.helicopter.position.x = 550;
                        this.helicopter.position.y = 100;
                        this.helicopter.velocity.Zero();
                        this.helicopter.velocity.y = this.goal.velocity.y;
                        this.helicopter.entColor.w = 0.f;
                        this.helicopter.fadeIn = true;
                        this.helicopter.fadeOut = false;
                    }
                }
            }
        }

        private void UpdateHelicopter() {

            if (this.bearHitTarget && this.bearIsShrinking) {
                if ((this.helicopter.velocity.y != 0) && (this.helicopter.position.y > 264)) {
                    this.helicopter.velocity.y = 0;
                    this.goal.velocity.y = 0;

                    this.helicopter.SetVisible(false);
                    this.goal.SetMaterial("game/bearshoot/goal_dead");
                    session.sw.PlayShaderDirectly("arcade_beargroan", 1);

                    this.helicopter.fadeOut = true;
                    this.goal.fadeOut = true;
                }
            } else if (this.currentLevel > 1) {
                final int height = (int) this.helicopter.position.y;
                final float speed = (this.currentLevel - 1) * 30;

                if (height > 240) {
                    this.helicopter.velocity.y = -speed;
                    this.goal.velocity.y = -speed;
                } else if (height < 30) {
                    this.helicopter.velocity.y = speed;
                    this.goal.velocity.y = speed;
                }
            }
        }

        private void UpdateTurret() {
            idVec2 pt = new idVec2();
            final idVec2 turretOrig = new idVec2();
            final idVec2 right = new idVec2();
            float dot, angle;

            pt.x = this.gui.CursorX();
            pt.y = this.gui.CursorY();
            turretOrig.Set(80.f, 348.f);

            pt = pt.oMinus(turretOrig);
            pt.NormalizeFast();

            right.x = 1.f;
            right.y = 0.f;

            dot = pt.oMultiply(right);

            angle = RAD2DEG((float) Math.acos(dot));

            this.turretAngle = idMath.ClampFloat(0.f, 90.f, angle);
        }

        private void UpdateButtons() {

            if (this.onFire.oCastBoolean()) {
                final idVec2 vec = new idVec2();

                this.gui.HandleNamedEvent("DisableFireButton");
                session.sw.PlayShaderDirectly("arcade_sargeshoot");

                this.bear.SetVisible(true);
                this.bearScale = 1.f;
                this.bear.SetSize(BEAR_SIZE, BEAR_SIZE);

                vec.x = idMath.Cos(DEG2RAD(this.turretAngle));
                vec.x += (1 - vec.x) * 0.18f;
                vec.y = -idMath.Sin(DEG2RAD(this.turretAngle));

                this.turretForce = bearTurretForce.GetFloat();

                this.bear.position.x = 80 + (96 * vec.x);
                this.bear.position.y = 334 + (96 * vec.y);
                this.bear.velocity.x = vec.x * this.turretForce;
                this.bear.velocity.y = vec.y * this.turretForce;

                this.gunblast.position.x = 55 + (96 * vec.x);
                this.gunblast.position.y = 310 + (100 * vec.y);
                this.gunblast.SetVisible(true);
                this.gunblast.entColor.w = 1.f;
                this.gunblast.rotation = this.turretAngle;
                this.gunblast.fadeOut = true;

                this.bearHitTarget = false;

                this.onFire.data = false;
            }
        }

        private void UpdateGame() {
            int i;

            if (this.onNewGame.oCastBoolean()) {
                ResetGameState();

                this.goal.position.x = 550;
                this.goal.position.y = 164;
                this.goal.velocity.Zero();
                this.helicopter.position.x = 550;
                this.helicopter.position.y = 100;
                this.helicopter.velocity.Zero();
                this.bear.SetVisible(false);

                bearTurretAngle.SetFloat(0.f);
                bearTurretForce.SetFloat(200.f);

                this.gamerunning.data = true;
            }
            if (this.onContinue.oCastBoolean()) {
                this.gameOver = false;
                this.timeRemaining = 60.f;

                this.onContinue.data = false;
            }

            if (this.gamerunning.oCastBoolean() == true) {
                final int current_time = this.gui.GetTime();
                final idRandom rnd = new idRandom(current_time);

                // Check for button presses
                UpdateButtons();

                if (this.bear != null) {
                    UpdateBear();
                }
                if ((this.helicopter != null) && (this.goal != null)) {
                    UpdateHelicopter();
                }

                // Update Wind
                if (this.windUpdateTime < current_time) {
                    float scale;
                    int width;

                    this.windForce = rnd.CRandomFloat() * (MAX_WINDFORCE * 0.75f);
                    if (this.windForce > 0) {
                        this.windForce += (MAX_WINDFORCE * 0.25f);
                        this.wind.rotation = 0;
                    } else {
                        this.windForce -= (MAX_WINDFORCE * 0.25f);
                        this.wind.rotation = 180;
                    }

                    scale = 1f - ((MAX_WINDFORCE - idMath.Fabs(this.windForce)) / MAX_WINDFORCE);
                    width = (int) (100 * scale);

                    if (this.windForce < 0) {
                        this.wind.position.x = (500 - width) + 1;
                    } else {
                        this.wind.position.x = 500;
                    }
                    this.wind.SetSize(width, 40);

                    this.windUpdateTime = current_time + 7000 + rnd.RandomInt(5000);
                }

                // Update turret rotation angle
                if (this.turret != null) {
                    this.turretAngle = bearTurretAngle.GetFloat();
                    this.turret.rotation = this.turretAngle;
                }

                for (i = 0; i < this.entities.Num(); i++) {
                    this.entities.oGet(i).Update(this.timeSlice);
                }

                // Update countdown timer
                this.timeRemaining -= this.timeSlice;
                this.timeRemaining = idMath.ClampFloat(0.f, 99999.f, this.timeRemaining);
                this.gui.SetStateString("time_remaining", va("%2.1f", this.timeRemaining));

                if ((this.timeRemaining <= 0.f) && !this.gameOver) {
                    this.gameOver = true;
                    this.updateScore = true;
                }

                if (this.updateScore) {
                    UpdateScore();
                    this.updateScore = false;
                }
            }
        }

        private void UpdateScore() {

            if (this.gameOver) {
                this.gui.HandleNamedEvent("GameOver");
                return;
            }

            this.goalsHit++;
            this.gui.SetStateString("player_score", va("%d", this.goalsHit));

            // Check for level progression
            if (0 == (this.goalsHit % 5)) {
                this.currentLevel++;
                this.gui.SetStateString("current_level", va("%d", this.currentLevel));
                session.sw.PlayShaderDirectly("arcade_levelcomplete1", 3);

                this.timeRemaining += 30;
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "gamerunning") == 0) {
                this.gamerunning.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onFire") == 0) {
                this.onFire.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onContinue") == 0) {
                this.onContinue.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "onNewGame") == 0) {
                this.onNewGame.oSet(src.ParseBool());
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
    }
}
