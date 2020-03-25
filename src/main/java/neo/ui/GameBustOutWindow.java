package neo.ui;

import static neo.Renderer.Image_files.R_LoadImage;
import static neo.Renderer.Material.SS_GUI;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_DOWN;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_LEFT;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_NONE;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_RIGHT;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_UP;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_BIGPADDLE;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_MULTIBALL;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_NONE;

import java.nio.ByteBuffer;

import neo.Renderer.Material.idMaterial;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
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
public class GameBustOutWindow {

    public static final float BALL_RADIUS = 12.f;
    public static final float BALL_SPEED = 250.f;
    public static final float BALL_MAXSPEED = 450.f;
//
    public static final int S_UNIQUE_CHANNEL = 6;
//

    public enum powerupType_t {

        POWERUP_NONE,//= 0,
        POWERUP_BIGPADDLE,
        POWERUP_MULTIBALL
    }

    static class BOEntity {

        public boolean visible;
//
        public idStr materialName;
        public idMaterial material;
        public float width, height;
        public idVec4 color;
        public idVec2 position;
        public idVec2 velocity;
//
        public powerupType_t powerup;
//
        public boolean removed;
        public boolean fadeOut;
//
        public idGameBustOutWindow game;
        //

        public BOEntity(idGameBustOutWindow _game) {
            this.game = _game;
            this.visible = true;

            this.materialName = new idStr("");
            this.material = null;
            this.width = this.height = 8;
            this.color = colorWhite;
            this.powerup = POWERUP_NONE;

            this.position.Zero();
            this.velocity.Zero();

            this.removed = false;
            this.fadeOut = false;//0;
        }
        // virtual					~BOEntity();

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteBool(this.visible);

            this.game.WriteSaveGameString(this.materialName.toString(), savefile);

            savefile.WriteFloat(this.width);
            savefile.WriteFloat(this.height);

            savefile.Write(this.color);
            savefile.Write(this.position);
            savefile.Write(this.velocity);

            savefile.WriteInt(this.powerup);
            savefile.WriteBool(this.removed);
            savefile.WriteBool(this.fadeOut);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBustOutWindow _game) {
            this.game = _game;

            this.visible = savefile.ReadBool();

            this.game.ReadSaveGameString(this.materialName, savefile);
            SetMaterial(this.materialName.toString());

            this.width = savefile.ReadFloat();
            this.height = savefile.ReadFloat();

            savefile.Read(this.color);
            savefile.Read(this.position);
            savefile.Read(this.velocity);

            this.powerup = powerupType_t.values()[savefile.ReadInt()];
            this.removed = savefile.ReadBool();
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

        public void SetColor(float r, float g, float b, float a) {
            this.color.x = r;
            this.color.y = g;
            this.color.z = b;
            this.color.w = a;
        }

        public void SetVisible(boolean isVisible) {
            this.visible = isVisible;
        }

        public void Update(float timeslice, int guiTime) {

            if (!this.visible) {
                return;
            }

            // Move the entity
            this.position.oPluSet(this.velocity.oMultiply(timeslice));

            // Fade out the ent
            if (this.fadeOut) {
                this.color.w -= timeslice * 2.5;

                if (this.color.w <= 0.f) {
                    this.color.w = 0.f;
                    this.removed = true;
                }
            }
        }

        public void Draw(idDeviceContext dc) {
            if (this.visible) {
                dc.DrawMaterialRotated(this.position.x, this.position.y, this.width, this.height, this.material, this.color, 1.0f, 1.0f, DEG2RAD(0.f));
            }
        }
    }

    public enum collideDir_t {

        COLLIDE_NONE,// = 0,
        COLLIDE_DOWN,
        COLLIDE_UP,
        COLLIDE_LEFT,
        COLLIDE_RIGHT
    }

    /*
     *****************************************************************************
     * BOBrick
     ****************************************************************************
     */
    static class BOBrick {

        public float x;
        public float y;
        public float width;
        public float height;
        public powerupType_t powerup;
//        
        public boolean isBroken;
//        
        public BOEntity ent;
//        

        public BOBrick() {
            this.ent = null;
            this.x = this.y = this.width = this.height = 0;
            this.powerup = POWERUP_NONE;
            this.isBroken = false;
        }

        public BOBrick(BOEntity _ent, float _x, float _y, float _width, float _height) {
            this.ent = _ent;
            this.x = _x;
            this.y = _y;
            this.width = _width;
            this.height = _height;
            this.powerup = POWERUP_NONE;

            this.isBroken = false;

            this.ent.position.x = this.x;
            this.ent.position.y = this.y;
            this.ent.SetSize(this.width, this.height);
            this.ent.SetMaterial("game/bustout/brick");

            this.ent.game.entities.Append(this.ent);
        }
        // ~BOBrick();

        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteFloat(this.x);
            savefile.WriteFloat(this.y);
            savefile.WriteFloat(this.width);
            savefile.WriteFloat(this.height);

            savefile.WriteInt(this.powerup);
            savefile.WriteBool(this.isBroken);

            final int index = this.ent.game.entities.FindIndex(this.ent);
            savefile.WriteInt(index);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBustOutWindow game) {
            this.x = savefile.ReadFloat();
            this.y = savefile.ReadFloat();
            this.width = savefile.ReadFloat();
            this.height = savefile.ReadFloat();

            this.powerup = powerupType_t.values()[savefile.ReadInt()];
            this.isBroken = savefile.ReadBool();

            int index;
            index = savefile.ReadInt();
            this.ent = game.entities.oGet(index);
        }

        public void SetColor(idVec4 bcolor) {
            this.ent.SetColor(bcolor.x, bcolor.y, bcolor.z, bcolor.w);
        }

        public collideDir_t checkCollision(idVec2 pos, idVec2 vel) {
            final idVec2 ptA = new idVec2(), ptB = new idVec2();
            float dist;

            collideDir_t result = COLLIDE_NONE;

            if (this.isBroken) {
                return result;
            }

            // Check for collision with each edge
            idVec2 vec;

            // Bottom
            ptA.x = this.x;
            ptA.y = this.y + this.height;

            ptB.x = this.x + this.width;
            ptB.y = this.y + this.height;

            if ((vel.y < 0) && (pos.y > ptA.y)) {
                if ((pos.x > ptA.x) && (pos.x < ptB.x)) {
                    dist = pos.y - ptA.y;

                    if (dist < BALL_RADIUS) {
                        result = COLLIDE_DOWN;
                    }
                } else {
                    if (pos.x <= ptA.x) {
                        vec = pos.oMinus(ptA);
                    } else {
                        vec = pos.oMinus(ptB);
                    }

                    if ((idMath.Fabs(vec.y) > idMath.Fabs(vec.x)) && (vec.LengthFast() < BALL_RADIUS)) {
                        result = COLLIDE_DOWN;
                    }
                }
            }

            if (result == COLLIDE_NONE) {
                // Top
                ptA.y = this.y;
                ptB.y = this.y;

                if ((vel.y > 0) && (pos.y < ptA.y)) {
                    if ((pos.x > ptA.x) && (pos.x < ptB.x)) {
                        dist = ptA.y - pos.y;

                        if (dist < BALL_RADIUS) {
                            result = COLLIDE_UP;
                        }
                    } else {
                        if (pos.x <= ptA.x) {
                            vec = pos.oMinus(ptA);
                        } else {
                            vec = pos.oMinus(ptB);
                        }

                        if ((idMath.Fabs(vec.y) > idMath.Fabs(vec.x)) && (vec.LengthFast() < BALL_RADIUS)) {
                            result = COLLIDE_UP;
                        }
                    }
                }

                if (result == COLLIDE_NONE) {
                    // Left side
                    ptA.x = this.x;
                    ptA.y = this.y;

                    ptB.x = this.x;
                    ptB.y = this.y + this.height;

                    if ((vel.x > 0) && (pos.x < ptA.x)) {
                        if ((pos.y > ptA.y) && (pos.y < ptB.y)) {
                            dist = ptA.x - pos.x;

                            if (dist < BALL_RADIUS) {
                                result = COLLIDE_LEFT;
                            }
                        } else {
                            if (pos.y <= ptA.y) {
                                vec = pos.oMinus(ptA);
                            } else {
                                vec = pos.oMinus(ptB);
                            }

                            if ((idMath.Fabs(vec.x) >= idMath.Fabs(vec.y)) && (vec.LengthFast() < BALL_RADIUS)) {
                                result = COLLIDE_LEFT;
                            }
                        }
                    }

                    if (result == COLLIDE_NONE) {
                        // Right side
                        ptA.x = this.x + this.width;
                        ptB.x = this.x + this.width;

                        if ((vel.x < 0) && (pos.x > ptA.x)) {
                            if ((pos.y > ptA.y) && (pos.y < ptB.y)) {
                                dist = pos.x - ptA.x;

                                if (dist < BALL_RADIUS) {
                                    result = COLLIDE_LEFT;
                                }
                            } else {
                                if (pos.y <= ptA.y) {
                                    vec = pos.oMinus(ptA);
                                } else {
                                    vec = pos.oMinus(ptB);
                                }

                                if ((idMath.Fabs(vec.x) >= idMath.Fabs(vec.y)) && (vec.LengthFast() < BALL_RADIUS)) {
                                    result = COLLIDE_LEFT;
                                }
                            }
                        }

                    }
                }
            }

            return result;
        }
    }
//    
    public static final int BOARD_ROWS = 12;
//    

    /*
     *****************************************************************************
     * idGameBustOutWindow
     ****************************************************************************
     */
    public static class idGameBustOutWindow extends idWindow {

        private idWinBool gamerunning;
        private idWinBool onFire;
        private idWinBool onContinue;
        private idWinBool onNewGame;
        private idWinBool onNewLevel;
        //
        private float timeSlice;
        private boolean gameOver;
        //
        private int numLevels;
        private byte[] levelBoardData;
        private boolean boardDataLoaded;
        //
        private int numBricks;
        private int currentLevel;
        //
        private boolean updateScore;
        private int gameScore;
        private int nextBallScore;
        //
        private int bigPaddleTime;
        private float paddleVelocity;
        //
        private float ballSpeed;
        private int ballsRemaining;
        private int ballsInPlay;
        private boolean ballHitCeiling;
        //
        private idList<BOEntity> balls;
        private idList<BOEntity> powerUps;
        //
        private BOBrick paddle;
        private final idList<BOBrick>[] board = new idList[BOARD_ROWS];
        //
        //

        public idGameBustOutWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idGameBustOutWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	// ~idGameBustOutWindow();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            this.gamerunning.WriteToSaveGame(savefile);
            this.onFire.WriteToSaveGame(savefile);
            this.onContinue.WriteToSaveGame(savefile);
            this.onNewGame.WriteToSaveGame(savefile);
            this.onNewLevel.WriteToSaveGame(savefile);

            savefile.WriteFloat(this.timeSlice);
            savefile.WriteBool(this.gameOver);
            savefile.WriteInt(this.numLevels);

            // Board Data is loaded when GUI is loaded, don't need to save
            savefile.WriteInt(this.numBricks);
            savefile.WriteInt(this.currentLevel);

            savefile.WriteBool(this.updateScore);
            savefile.WriteInt(this.gameScore);
            savefile.WriteInt(this.nextBallScore);

            savefile.WriteInt(this.bigPaddleTime);
            savefile.WriteFloat(this.paddleVelocity);

            savefile.WriteFloat(this.ballSpeed);
            savefile.WriteInt(this.ballsRemaining);
            savefile.WriteInt(this.ballsInPlay);
            savefile.WriteBool(this.ballHitCeiling);

            // Write Entities
            int i;
            int numberOfEnts = this.entities.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                this.entities.oGet(i).WriteToSaveGame(savefile);
            }

            // Write Balls
            numberOfEnts = this.balls.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                final int ballIndex = this.entities.FindIndex(this.balls.oGet(i));
                savefile.WriteInt(ballIndex);
            }

            // Write Powerups
            numberOfEnts = this.powerUps.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                final int powerIndex = this.entities.FindIndex(this.powerUps.oGet(i));
                savefile.WriteInt(powerIndex);
            }

            // Write paddle
            this.paddle.WriteToSaveGame(savefile);

            // Write Bricks
            int row;
            for (row = 0; row < BOARD_ROWS; row++) {
                numberOfEnts = this.board[row].Num();
                savefile.WriteInt(numberOfEnts);
                for (i = 0; i < numberOfEnts; i++) {
                    this.board[row].oGet(i).WriteToSaveGame(savefile);
                }
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            // Clear out existing paddle and entities from GUI load
//	delete paddle;
            this.entities.DeleteContents(true);

            this.gamerunning.ReadFromSaveGame(savefile);
            this.onFire.ReadFromSaveGame(savefile);
            this.onContinue.ReadFromSaveGame(savefile);
            this.onNewGame.ReadFromSaveGame(savefile);
            this.onNewLevel.ReadFromSaveGame(savefile);

            this.timeSlice = savefile.ReadFloat();
            this.gameOver = savefile.ReadBool();
            this.numLevels = savefile.ReadInt();

            // Board Data is loaded when GUI is loaded, don't need to save
            this.numBricks = savefile.ReadInt();
            this.currentLevel = savefile.ReadInt();

            this.updateScore = savefile.ReadBool();
            this.gameScore = savefile.ReadInt();
            this.nextBallScore = savefile.ReadInt();

            this.bigPaddleTime = savefile.ReadInt();
            this.paddleVelocity = savefile.ReadFloat();

            this.ballSpeed = savefile.ReadFloat();
            this.ballsRemaining = savefile.ReadInt();
            this.ballsInPlay = savefile.ReadInt();
            this.ballHitCeiling = savefile.ReadBool();

            int i;
            int numberOfEnts;

            // Read entities
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                BOEntity ent;

                ent = new BOEntity(this);
                ent.ReadFromSaveGame(savefile, this);
                this.entities.Append(ent);
            }

            // Read balls
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                int ballIndex;
                ballIndex = savefile.ReadInt();
                this.balls.Append(this.entities.oGet(ballIndex));
            }

            // Read powerups
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                int powerIndex;
                powerIndex = savefile.ReadInt();
                this.balls.Append(this.entities.oGet(powerIndex));
            }

            // Read paddle
            this.paddle = new BOBrick();
            this.paddle.ReadFromSaveGame(savefile, this);

            // Read board
            int row;
            for (row = 0; row < BOARD_ROWS; row++) {
                numberOfEnts = savefile.ReadInt();
                for (i = 0; i < numberOfEnts; i++) {
                    final BOBrick brick = new BOBrick();
                    brick.ReadFromSaveGame(savefile, this);
                    this.board[row].Append(brick);
                }
            }
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
                    if (this.ballsInPlay == 0) {
                        final BOEntity ball = CreateNewBall();

                        ball.SetVisible(true);
                        ball.position.x = this.paddle.ent.position.x + 48.f;
                        ball.position.y = 430.f;

                        ball.velocity.x = this.ballSpeed;
                        ball.velocity.y = -this.ballSpeed * 2.f;
                        ball.velocity.NormalizeFast();
                        ball.velocity.oMulSet(this.ballSpeed);
                    }
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
            } else if (idStr.Icmp(_name, "onNewLevel") == 0) {
                retVar = this.onNewLevel;
            }

            if (retVar != null) {
                return retVar;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }
//        
        public idList<BOEntity> entities;
//        

        private void CommonInit() {
            BOEntity ent;

            // Precache images
            declManager.FindMaterial("game/bustout/ball");
            declManager.FindMaterial("game/bustout/doublepaddle");
            declManager.FindMaterial("game/bustout/powerup_bigpaddle");
            declManager.FindMaterial("game/bustout/powerup_multiball");
            declManager.FindMaterial("game/bustout/brick");

            // Precache sounds
            declManager.FindSound("arcade_ballbounce");
            declManager.FindSound("arcade_brickhit");
            declManager.FindSound("arcade_missedball");
            declManager.FindSound("arcade_sadsound");
            declManager.FindSound("arcade_extraball");
            declManager.FindSound("arcade_powerup");

            ResetGameState();

            this.numLevels = 0;
            this.boardDataLoaded = false;
            this.levelBoardData = null;

            // Create Paddle
            ent = new BOEntity(this);
            this.paddle = new BOBrick(ent, 260.f, 440.f, 96.f, 24.f);
            this.paddle.ent.SetMaterial("game/bustout/paddle");
        }

        private void ResetGameState() {
            this.gamerunning.data = false;
            this.gameOver = false;
            this.onFire.data = false;
            this.onContinue.data = false;
            this.onNewGame.data = false;
            this.onNewLevel.data = false;

            // Game moves forward 16 milliseconds every frame
            this.timeSlice = 0.016f;
            this.ballsRemaining = 3;
            this.ballSpeed = BALL_SPEED;
            this.ballsInPlay = 0;
            this.updateScore = false;
            this.numBricks = 0;
            this.currentLevel = 1;
            this.gameScore = 0;
            this.bigPaddleTime = 0;
            this.nextBallScore = this.gameScore + 10000;

            ClearBoard();
        }

        private void ClearBoard() {
            int i, j;

            ClearPowerups();

            this.ballHitCeiling = false;

            for (i = 0; i < BOARD_ROWS; i++) {
                for (j = 0; j < this.board[i].Num(); j++) {

                    final BOBrick brick = this.board[i].oGet(j);
                    brick.ent.removed = true;
                }

                this.board[i].DeleteContents(true);
            }
        }

        private void ClearPowerups() {
            while (this.powerUps.Num() != 0) {
                this.powerUps.oGet(0).removed = true;
                this.powerUps.RemoveIndex(0);
            }
        }

        private void ClearBalls() {
            while (this.balls.Num() != 0) {
                this.balls.oGet(0).removed = true;
                this.balls.RemoveIndex(0);
            }

            this.ballsInPlay = 0;
        }

        private void LoadBoardFiles() {
            int i;
            final int[] w = new int[1], h = new int[1];
            final long[]/*ID_TIME_T*/ time = new long[1];
            int boardSize;
            byte[] currentBoard;
            int boardIndex = 0;

            if (this.boardDataLoaded) {
                return;
            }

            boardSize = 9 * 12 * 4;
            this.levelBoardData = new byte[boardSize * this.numLevels];// Mem_Alloc(boardSize * numLevels);

            currentBoard = this.levelBoardData;

            for (i = 0; i < this.numLevels; i++) {
                String name = "guis/assets/bustout/level";
                name += (i + 1);
                name += ".tga";

                ByteBuffer pic = R_LoadImage(name, w, h, time, false);

                if (pic != null) {
                    if ((w[0] != 9) || (h[0] != 12)) {
                        common.DWarning("Hell Bust-Out level image not correct dimensions! (%d x %d)", w, h);
                    }

//			memcpy( currentBoard, pic, boardSize );
                    System.arraycopy(pic.array(), 0, currentBoard, boardIndex, boardSize);
                    pic = null;//Mem_Free(pic);
                }

                boardIndex += boardSize;
            }

            this.boardDataLoaded = true;
        }

        private void SetCurrentBoard() {
            int i, j;
            final int realLevel = ((this.currentLevel - 1) % this.numLevels);
            final int boardSize;
            final int currentBoard;
            float bx = 11f;
            float by = 24f;
            final float stepx = 619.f / 9.f;
            final float stepy = (256 / 12.f);

            boardSize = 9 * 12 * 4;
            currentBoard = realLevel * boardSize;

            for (j = 0; j < BOARD_ROWS; j++) {
                bx = 11.f;

                for (i = 0; i < 9; i++) {
                    final int pixelindex = (j * 9 * 4) + (i * 4);

                    if (this.levelBoardData[currentBoard + pixelindex + 3] != 0) {
                        final idVec4 bcolor = new idVec4();
                        float pType;//= 0f;

                        final BOEntity bent = new BOEntity(this);
                        final BOBrick brick = new BOBrick(bent, bx, by, stepx, stepy);

                        bcolor.x = this.levelBoardData[currentBoard + pixelindex + 0] / 255.f;
                        bcolor.y = this.levelBoardData[currentBoard + pixelindex + 1] / 255.f;
                        bcolor.z = this.levelBoardData[currentBoard + pixelindex + 2] / 255.f;
                        bcolor.w = 1.f;
                        brick.SetColor(bcolor);

                        pType = this.levelBoardData[pixelindex + 3] / 255.f;
                        if ((pType > 0.f) && (pType < 1.f)) {
                            if (pType < 0.5f) {
                                brick.powerup = POWERUP_BIGPADDLE;
                            } else {
                                brick.powerup = POWERUP_MULTIBALL;
                            }
                        }

                        this.board[j].Append(brick);
                        this.numBricks++;
                    }

                    bx += stepx;
                }

                by += stepy;
            }
        }

        private void UpdateGame() {
            int i;

            if (this.onNewGame.oCastBoolean()) {
                ResetGameState();

                // Create Board
                SetCurrentBoard();

                this.gamerunning.oSet(true);
            }
            if (this.onContinue.oCastBoolean()) {
                this.gameOver = false;
                this.ballsRemaining = 3;

                this.onContinue.oSet(false);
            }
            if (this.onNewLevel.oCastBoolean()) {
                this.currentLevel++;

                ClearBoard();
                SetCurrentBoard();

                this.ballSpeed = BALL_SPEED * (1f + (this.currentLevel / 5f));
                if (this.ballSpeed > BALL_MAXSPEED) {
                    this.ballSpeed = BALL_MAXSPEED;
                }
                this.updateScore = true;
                this.onNewLevel.oSet(false);
            }

            if (this.gamerunning.oCastBoolean() == true) {

                UpdatePaddle();
                UpdateBall();
                UpdatePowerups();

                for (i = 0; i < this.entities.Num(); i++) {
                    this.entities.oGet(i).Update(this.timeSlice, this.gui.GetTime());
                }

                // Delete entities that need to be deleted
                for (i = this.entities.Num() - 1; i >= 0; i--) {
                    if (this.entities.oGet(i).removed) {
                        final BOEntity ent = this.entities.oGet(i);
//				delete ent;
                        this.entities.RemoveIndex(i);
                    }
                }

                if (this.updateScore) {
                    UpdateScore();
                    this.updateScore = false;
                }
            }
        }

        private void UpdatePowerups() {
            final idVec2 pos = new idVec2();

            for (int i = 0; i < this.powerUps.Num(); i++) {
                final BOEntity pUp = this.powerUps.oGet(i);

                // Check for powerup falling below screen
                if (pUp.position.y > 480) {

                    this.powerUps.RemoveIndex(i);
                    pUp.removed = true;
                    continue;
                }

                // Check for the paddle catching a powerup
                pos.x = pUp.position.x + (pUp.width / 2);
                pos.y = pUp.position.y + (pUp.height / 2);

                final collideDir_t collision = this.paddle.checkCollision(pos, pUp.velocity);
                if (collision != COLLIDE_NONE) {
                    BOEntity ball;

                    // Give the powerup to the player
                    switch (pUp.powerup) {
                        case POWERUP_BIGPADDLE:
                            this.bigPaddleTime = this.gui.GetTime() + 15000;
                            break;
                        case POWERUP_MULTIBALL:
                            // Create 2 new balls in the spot of the existing ball
                            for (int b = 0; b < 2; b++) {
                                ball = CreateNewBall();
                                ball.position = this.balls.oGet(0).position;
                                ball.velocity = this.balls.oGet(0).velocity;

                                if (b == 0) {
                                    ball.velocity.x -= 35.f;
                                } else {
                                    ball.velocity.x += 35.f;
                                }
                                ball.velocity.NormalizeFast();
                                ball.velocity.oMulSet(this.ballSpeed);

                                ball.SetVisible(true);
                            }
                            break;
                        default:
                            break;
                    }

                    // Play the sound
                    session.sw.PlayShaderDirectly("arcade_powerup", S_UNIQUE_CHANNEL);

                    // Remove it
                    this.powerUps.RemoveIndex(i);
                    pUp.removed = true;
                }
            }
        }

        private void UpdatePaddle() {
            final idVec2 cursorPos = new idVec2();
            final float oldPos = this.paddle.x;

            cursorPos.x = this.gui.CursorX();
            cursorPos.y = this.gui.CursorY();

            if (this.bigPaddleTime > this.gui.GetTime()) {
                this.paddle.x = cursorPos.x - 80f;
                this.paddle.width = 160;
                this.paddle.ent.width = 160;
                this.paddle.ent.SetMaterial("game/bustout/doublepaddle");
            } else {
                this.paddle.x = cursorPos.x - 48f;
                this.paddle.width = 96;
                this.paddle.ent.width = 96;
                this.paddle.ent.SetMaterial("game/bustout/paddle");
            }
            this.paddle.ent.position.x = this.paddle.x;

            this.paddleVelocity = (this.paddle.x - oldPos);
        }
        private static int bounceChannel = 1;

        private void UpdateBall() {
            int ballnum, i, j;
            boolean playSoundBounce = false;
            boolean playSoundBrick = false;

            if (this.ballsInPlay == 0) {
                return;
            }

            for (ballnum = 0; ballnum < this.balls.Num(); ballnum++) {
                final BOEntity ball = this.balls.oGet(ballnum);

                // Check for ball going below screen, lost ball
                if (ball.position.y > 480f) {
                    ball.removed = true;
                    continue;
                }

                // Check world collision
                if ((ball.position.y < 20) && (ball.velocity.y < 0)) {
                    ball.velocity.y = -ball.velocity.y;

                    // Increase ball speed when it hits ceiling
                    if (!this.ballHitCeiling) {
                        this.ballSpeed *= 1.25f;
                        this.ballHitCeiling = true;
                    }
                    playSoundBounce = true;
                }

                if ((ball.position.x > 608) && (ball.velocity.x > 0)) {
                    ball.velocity.x = -ball.velocity.x;
                    playSoundBounce = true;
                } else if ((ball.position.x < 8) && (ball.velocity.x < 0)) {
                    ball.velocity.x = -ball.velocity.x;
                    playSoundBounce = true;
                }

                // Check for Paddle collision
                final idVec2 ballCenter = ball.position.oPlus(new idVec2(BALL_RADIUS, BALL_RADIUS));
                collideDir_t collision = this.paddle.checkCollision(ballCenter, ball.velocity);

                if (collision == COLLIDE_UP) {
                    if (ball.velocity.y > 0) {
                        final idVec2 paddleVec = new idVec2(this.paddleVelocity * 2, 0);
                        float centerX;

                        if (this.bigPaddleTime > this.gui.GetTime()) {
                            centerX = this.paddle.x + 80f;
                        } else {
                            centerX = this.paddle.x + 48f;
                        }

                        ball.velocity.y = -ball.velocity.y;

                        paddleVec.x += (ball.position.x - centerX) * 2;

                        ball.velocity.oPluSet(paddleVec);
                        ball.velocity.NormalizeFast();
                        ball.velocity.oMulSet(this.ballSpeed);

                        playSoundBounce = true;
                    }
                } else if ((collision == COLLIDE_LEFT) || (collision == COLLIDE_RIGHT)) {
                    if (ball.velocity.y > 0) {
                        ball.velocity.x = -ball.velocity.x;
                        playSoundBounce = true;
                    }
                }

                collision = COLLIDE_NONE;

                // Check for collision with bricks
                for (i = 0; i < BOARD_ROWS; i++) {
                    final int num = this.board[i].Num();

                    for (j = 0; j < num; j++) {
                        final BOBrick brick = (this.board[i]).oGet(j);

                        collision = brick.checkCollision(ballCenter, ball.velocity);
                        if (collision != null) {
                            // Now break the brick if there was a collision
                            brick.isBroken = true;
                            brick.ent.fadeOut = true;

                            if (brick.powerup.ordinal() > POWERUP_NONE.ordinal()) {
                                final BOEntity pUp = CreatePowerup(brick);
                            }

                            this.numBricks--;
                            this.gameScore += 100;
                            this.updateScore = true;

                            // Go ahead an forcibly remove the last brick, no fade
                            if (this.numBricks == 0) {
                                brick.ent.removed = true;
                            }
                            this.board[i].Remove(brick);
                            break;
                        }
                    }

                    if (collision != null) {
                        playSoundBrick = true;
                        break;
                    }
                }

                if ((collision == COLLIDE_DOWN) || (collision == COLLIDE_UP)) {
                    ball.velocity.y *= -1;
                } else if ((collision == COLLIDE_LEFT) || (collision == COLLIDE_RIGHT)) {
                    ball.velocity.x *= -1;
                }

                if (playSoundBounce) {
                    session.sw.PlayShaderDirectly("arcade_ballbounce", bounceChannel);
                } else if (playSoundBrick) {
                    session.sw.PlayShaderDirectly("arcade_brickhit", bounceChannel);
                }

                if (playSoundBounce || playSoundBrick) {
                    bounceChannel++;
                    if (bounceChannel == 4) {
                        bounceChannel = 1;
                    }
                }
            }

            // Check to see if any balls were removed from play
            for (ballnum = 0; ballnum < this.balls.Num(); ballnum++) {
                if (this.balls.oGet(ballnum).removed) {
                    this.ballsInPlay--;
                    this.balls.RemoveIndex(ballnum);
                }
            }

            // If all the balls were removed, update the game accordingly
            if (this.ballsInPlay == 0) {
                if (this.ballsRemaining == 0) {
                    this.gameOver = true;

                    // Game Over sound
                    session.sw.PlayShaderDirectly("arcade_sadsound", S_UNIQUE_CHANNEL);
                } else {
                    this.ballsRemaining--;

                    // Ball was lost, but game is not over
                    session.sw.PlayShaderDirectly("arcade_missedball", S_UNIQUE_CHANNEL);
                }

                ClearPowerups();
                this.updateScore = true;
            }
        }

        private void UpdateScore() {

            if (this.gameOver) {
                this.gui.HandleNamedEvent("GameOver");//TODO:put text in property files for localization.
                return;
            }

            // Check for level progression
            if (this.numBricks == 0) {
                ClearBalls();

                this.gui.HandleNamedEvent("levelComplete");
            }

            // Check for new ball score
            if (this.gameScore >= this.nextBallScore) {
                this.ballsRemaining++;
                this.gui.HandleNamedEvent("extraBall");

                // Play sound
                session.sw.PlayShaderDirectly("arcade_extraball", S_UNIQUE_CHANNEL);

                this.nextBallScore = this.gameScore + 10000;
            }

            this.gui.SetStateString("player_score", va("%d", this.gameScore));
            this.gui.SetStateString("balls_remaining", va("%d", this.ballsRemaining));
            this.gui.SetStateString("current_level", va("%d", this.currentLevel));
            this.gui.SetStateString("next_ball_score", va("%d", this.nextBallScore));
        }

        private BOEntity CreateNewBall() {
            BOEntity ball;

            ball = new BOEntity(this);
            ball.position.x = 300f;
            ball.position.y = 416f;
            ball.SetMaterial("game/bustout/ball");
            ball.SetSize(BALL_RADIUS * 2f, BALL_RADIUS * 2f);
            ball.SetVisible(false);

            this.ballsInPlay++;

            this.balls.Append(ball);
            this.entities.Append(ball);

            return ball;
        }

        private BOEntity CreatePowerup(BOBrick brick) {
            final BOEntity powerEnt = new BOEntity(this);

            powerEnt.position.x = brick.x;
            powerEnt.position.y = brick.y;
            powerEnt.velocity.x = 0f;
            powerEnt.velocity.y = 64f;

            powerEnt.powerup = brick.powerup;

            switch (powerEnt.powerup) {
                case POWERUP_BIGPADDLE:
                    powerEnt.SetMaterial("game/bustout/powerup_bigpaddle");
                    break;
                case POWERUP_MULTIBALL:
                    powerEnt.SetMaterial("game/bustout/powerup_multiball");
                    break;
                default:
                    powerEnt.SetMaterial("textures/common/nodraw");
                    break;
            }

            powerEnt.SetSize(619 / 9, 256 / 12);
            powerEnt.SetVisible(true);

            this.powerUps.Append(powerEnt);
            this.entities.Append(powerEnt);

            return powerEnt;
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
            if (idStr.Icmp(_name, "onNewLevel") == 0) {
                this.onNewLevel.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "numLevels") == 0) {
                this.numLevels = src.ParseInt();

                // Load all the level images
                LoadBoardFiles();
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }
    }
}
