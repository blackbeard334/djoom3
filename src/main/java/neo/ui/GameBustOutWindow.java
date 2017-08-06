package neo.ui;

import java.nio.ByteBuffer;
import static neo.Renderer.Image_files.R_LoadImage;
import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import static neo.framework.Common.common;
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
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.GameBustOutWindow.BOBrick;
import neo.ui.GameBustOutWindow.BOEntity;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_DOWN;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_LEFT;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_NONE;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_RIGHT;
import static neo.ui.GameBustOutWindow.collideDir_t.COLLIDE_UP;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_BIGPADDLE;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_MULTIBALL;
import static neo.ui.GameBustOutWindow.powerupType_t.POWERUP_NONE;
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
    };

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
            game = _game;
            visible = true;

            materialName = new idStr("");
            material = null;
            width = height = 8;
            color = colorWhite;
            powerup = POWERUP_NONE;

            position.Zero();
            velocity.Zero();

            removed = false;
            fadeOut = false;//0;
        }
        // virtual					~BOEntity();

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteBool(visible);

            game.WriteSaveGameString(materialName.toString(), savefile);

            savefile.WriteFloat(width);
            savefile.WriteFloat(height);

            savefile.Write(color);
            savefile.Write(position);
            savefile.Write(velocity);

            savefile.WriteInt(powerup);
            savefile.WriteBool(removed);
            savefile.WriteBool(fadeOut);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBustOutWindow _game) {
            game = _game;

            visible = savefile.ReadBool();

            game.ReadSaveGameString(materialName, savefile);
            SetMaterial(materialName.toString());

            width = savefile.ReadFloat();
            height = savefile.ReadFloat();

            savefile.Read(color);
            savefile.Read(position);
            savefile.Read(velocity);

            powerup = powerupType_t.values()[savefile.ReadInt()];
            removed = savefile.ReadBool();
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

        public void SetColor(float r, float g, float b, float a) {
            color.x = r;
            color.y = g;
            color.z = b;
            color.w = a;
        }

        public void SetVisible(boolean isVisible) {
            visible = isVisible;
        }

        public void Update(float timeslice, int guiTime) {

            if (!visible) {
                return;
            }

            // Move the entity
            position.oPluSet(velocity.oMultiply(timeslice));

            // Fade out the ent
            if (fadeOut) {
                color.w -= timeslice * 2.5;

                if (color.w <= 0.f) {
                    color.w = 0.f;
                    removed = true;
                }
            }
        }

        public void Draw(idDeviceContext dc) {
            if (visible) {
                dc.DrawMaterialRotated(position.x, position.y, width, height, material, color, 1.0f, 1.0f, (float) DEG2RAD(0.f));
            }
        }
    };

    public enum collideDir_t {

        COLLIDE_NONE,// = 0,
        COLLIDE_DOWN,
        COLLIDE_UP,
        COLLIDE_LEFT,
        COLLIDE_RIGHT
    };

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
            ent = null;
            x = y = width = height = 0;
            powerup = POWERUP_NONE;
            isBroken = false;
        }

        public BOBrick(BOEntity _ent, float _x, float _y, float _width, float _height) {
            ent = _ent;
            x = _x;
            y = _y;
            width = _width;
            height = _height;
            powerup = POWERUP_NONE;

            isBroken = false;

            ent.position.x = x;
            ent.position.y = y;
            ent.SetSize(width, height);
            ent.SetMaterial("game/bustout/brick");

            ent.game.entities.Append(ent);
        }
        // ~BOBrick();

        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteFloat(x);
            savefile.WriteFloat(y);
            savefile.WriteFloat(width);
            savefile.WriteFloat(height);

            savefile.WriteInt(powerup);
            savefile.WriteBool(isBroken);

            int index = ent.game.entities.FindIndex(ent);
            savefile.WriteInt(index);
        }

        public void ReadFromSaveGame(idFile savefile, idGameBustOutWindow game) {
            x = savefile.ReadFloat();
            y = savefile.ReadFloat();
            width = savefile.ReadFloat();
            height = savefile.ReadFloat();

            powerup = powerupType_t.values()[savefile.ReadInt()];
            isBroken = savefile.ReadBool();

            int index;
            index = savefile.ReadInt();
            ent = game.entities.oGet(index);
        }

        public void SetColor(idVec4 bcolor) {
            ent.SetColor(bcolor.x, bcolor.y, bcolor.z, bcolor.w);
        }

        public collideDir_t checkCollision(idVec2 pos, idVec2 vel) {
            idVec2 ptA = new idVec2(), ptB = new idVec2();
            float dist;

            collideDir_t result = COLLIDE_NONE;

            if (isBroken) {
                return result;
            }

            // Check for collision with each edge
            idVec2 vec;

            // Bottom
            ptA.x = x;
            ptA.y = y + height;

            ptB.x = x + width;
            ptB.y = y + height;

            if (vel.y < 0 && pos.y > ptA.y) {
                if (pos.x > ptA.x && pos.x < ptB.x) {
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
                ptA.y = y;
                ptB.y = y;

                if (vel.y > 0 && pos.y < ptA.y) {
                    if (pos.x > ptA.x && pos.x < ptB.x) {
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
                    ptA.x = x;
                    ptA.y = y;

                    ptB.x = x;
                    ptB.y = y + height;

                    if (vel.x > 0 && pos.x < ptA.x) {
                        if (pos.y > ptA.y && pos.y < ptB.y) {
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
                        ptA.x = x + width;
                        ptB.x = x + width;

                        if (vel.x < 0 && pos.x > ptA.x) {
                            if (pos.y > ptA.y && pos.y < ptB.y) {
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
    };
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
        private idList<BOBrick>[] board = new idList[BOARD_ROWS];
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

            gamerunning.WriteToSaveGame(savefile);
            onFire.WriteToSaveGame(savefile);
            onContinue.WriteToSaveGame(savefile);
            onNewGame.WriteToSaveGame(savefile);
            onNewLevel.WriteToSaveGame(savefile);

            savefile.WriteFloat(timeSlice);
            savefile.WriteBool(gameOver);
            savefile.WriteInt(numLevels);

            // Board Data is loaded when GUI is loaded, don't need to save
            savefile.WriteInt(numBricks);
            savefile.WriteInt(currentLevel);

            savefile.WriteBool(updateScore);
            savefile.WriteInt(gameScore);
            savefile.WriteInt(nextBallScore);

            savefile.WriteInt(bigPaddleTime);
            savefile.WriteFloat(paddleVelocity);

            savefile.WriteFloat(ballSpeed);
            savefile.WriteInt(ballsRemaining);
            savefile.WriteInt(ballsInPlay);
            savefile.WriteBool(ballHitCeiling);

            // Write Entities
            int i;
            int numberOfEnts = entities.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                entities.oGet(i).WriteToSaveGame(savefile);
            }

            // Write Balls
            numberOfEnts = balls.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                int ballIndex = entities.FindIndex(balls.oGet(i));
                savefile.WriteInt(ballIndex);
            }

            // Write Powerups
            numberOfEnts = powerUps.Num();
            savefile.WriteInt(numberOfEnts);
            for (i = 0; i < numberOfEnts; i++) {
                int powerIndex = entities.FindIndex(powerUps.oGet(i));
                savefile.WriteInt(powerIndex);
            }

            // Write paddle
            paddle.WriteToSaveGame(savefile);

            // Write Bricks
            int row;
            for (row = 0; row < BOARD_ROWS; row++) {
                numberOfEnts = board[row].Num();
                savefile.WriteInt(numberOfEnts);
                for (i = 0; i < numberOfEnts; i++) {
                    board[row].oGet(i).WriteToSaveGame(savefile);
                }
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            // Clear out existing paddle and entities from GUI load
//	delete paddle;
            entities.DeleteContents(true);

            gamerunning.ReadFromSaveGame(savefile);
            onFire.ReadFromSaveGame(savefile);
            onContinue.ReadFromSaveGame(savefile);
            onNewGame.ReadFromSaveGame(savefile);
            onNewLevel.ReadFromSaveGame(savefile);

            timeSlice = savefile.ReadFloat();
            gameOver = savefile.ReadBool();
            numLevels = savefile.ReadInt();

            // Board Data is loaded when GUI is loaded, don't need to save
            numBricks = savefile.ReadInt();
            currentLevel = savefile.ReadInt();

            updateScore = savefile.ReadBool();
            gameScore = savefile.ReadInt();
            nextBallScore = savefile.ReadInt();

            bigPaddleTime = savefile.ReadInt();
            paddleVelocity = savefile.ReadFloat();

            ballSpeed = savefile.ReadFloat();
            ballsRemaining = savefile.ReadInt();
            ballsInPlay = savefile.ReadInt();
            ballHitCeiling = savefile.ReadBool();

            int i;
            int numberOfEnts;

            // Read entities
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                BOEntity ent;

                ent = new BOEntity(this);
                ent.ReadFromSaveGame(savefile, this);
                entities.Append(ent);
            }

            // Read balls
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                int ballIndex;
                ballIndex = savefile.ReadInt();
                balls.Append(entities.oGet(ballIndex));
            }

            // Read powerups
            numberOfEnts = savefile.ReadInt();
            for (i = 0; i < numberOfEnts; i++) {
                int powerIndex;
                powerIndex = savefile.ReadInt();
                balls.Append(entities.oGet(powerIndex));
            }

            // Read paddle
            paddle = new BOBrick();
            paddle.ReadFromSaveGame(savefile, this);

            // Read board
            int row;
            for (row = 0; row < BOARD_ROWS; row++) {
                numberOfEnts = savefile.ReadInt();
                for (i = 0; i < numberOfEnts; i++) {
                    BOBrick brick = new BOBrick();
                    brick.ReadFromSaveGame(savefile, this);
                    board[row].Append(brick);
                }
            }
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
                    if (ballsInPlay == 0) {
                        BOEntity ball = CreateNewBall();

                        ball.SetVisible(true);
                        ball.position.x = paddle.ent.position.x + 48.f;
                        ball.position.y = 430.f;

                        ball.velocity.x = ballSpeed;
                        ball.velocity.y = -ballSpeed * 2.f;
                        ball.velocity.NormalizeFast();
                        ball.velocity.oMulSet(ballSpeed);
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
            } else if (idStr.Icmp(_name, "onNewLevel") == 0) {
                retVar = onNewLevel;
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

            numLevels = 0;
            boardDataLoaded = false;
            levelBoardData = null;

            // Create Paddle
            ent = new BOEntity(this);
            paddle = new BOBrick(ent, 260.f, 440.f, 96.f, 24.f);
            paddle.ent.SetMaterial("game/bustout/paddle");
        }

        private void ResetGameState() {
            gamerunning.data = false;
            gameOver = false;
            onFire.data = false;
            onContinue.data = false;
            onNewGame.data = false;
            onNewLevel.data = false;

            // Game moves forward 16 milliseconds every frame
            timeSlice = 0.016f;
            ballsRemaining = 3;
            ballSpeed = BALL_SPEED;
            ballsInPlay = 0;
            updateScore = false;
            numBricks = 0;
            currentLevel = 1;
            gameScore = 0;
            bigPaddleTime = 0;
            nextBallScore = gameScore + 10000;

            ClearBoard();
        }

        private void ClearBoard() {
            int i, j;

            ClearPowerups();

            ballHitCeiling = false;

            for (i = 0; i < BOARD_ROWS; i++) {
                for (j = 0; j < board[i].Num(); j++) {

                    BOBrick brick = board[i].oGet(j);
                    brick.ent.removed = true;
                }

                board[i].DeleteContents(true);
            }
        }

        private void ClearPowerups() {
            while (powerUps.Num() != 0) {
                powerUps.oGet(0).removed = true;
                powerUps.RemoveIndex(0);
            }
        }

        private void ClearBalls() {
            while (balls.Num() != 0) {
                balls.oGet(0).removed = true;
                balls.RemoveIndex(0);
            }

            ballsInPlay = 0;
        }

        private void LoadBoardFiles() {
            int i;
            int[] w = new int[1], h = new int[1];
            long[]/*ID_TIME_T*/ time = new long[1];
            int boardSize;
            byte[] currentBoard;
            int boardIndex = 0;

            if (boardDataLoaded) {
                return;
            }

            boardSize = 9 * 12 * 4;
            levelBoardData = new byte[boardSize * numLevels];// Mem_Alloc(boardSize * numLevels);

            currentBoard = levelBoardData;

            for (i = 0; i < numLevels; i++) {
                String name = "guis/assets/bustout/level";
                name += (i + 1);
                name += ".tga";

                ByteBuffer pic = R_LoadImage(name, w, h, time, false);

                if (pic != null) {
                    if (w[0] != 9 || h[0] != 12) {
                        common.DWarning("Hell Bust-Out level image not correct dimensions! (%d x %d)", w, h);
                    }

//			memcpy( currentBoard, pic, boardSize );
                    System.arraycopy(pic.array(), 0, currentBoard, boardIndex, boardSize);
                    pic = null;//Mem_Free(pic);
                }

                boardIndex += boardSize;
            }

            boardDataLoaded = true;
        }

        private void SetCurrentBoard() {
            int i, j;
            final int realLevel = ((currentLevel - 1) % numLevels);
            final int boardSize;
            final int currentBoard;
            float bx = 11f;
            float by = 24f;
            float stepx = 619.f / 9.f;
            float stepy = (256 / 12.f);

            boardSize = 9 * 12 * 4;
            currentBoard = realLevel * boardSize;

            for (j = 0; j < BOARD_ROWS; j++) {
                bx = 11.f;

                for (i = 0; i < 9; i++) {
                    int pixelindex = (j * 9 * 4) + (i * 4);

                    if (levelBoardData[currentBoard + pixelindex + 3] != 0) {
                        idVec4 bcolor = new idVec4();
                        float pType;//= 0f;

                        BOEntity bent = new BOEntity(this);
                        BOBrick brick = new BOBrick(bent, bx, by, stepx, stepy);

                        bcolor.x = levelBoardData[currentBoard + pixelindex + 0] / 255.f;
                        bcolor.y = levelBoardData[currentBoard + pixelindex + 1] / 255.f;
                        bcolor.z = levelBoardData[currentBoard + pixelindex + 2] / 255.f;
                        bcolor.w = 1.f;
                        brick.SetColor(bcolor);

                        pType = levelBoardData[pixelindex + 3] / 255.f;
                        if (pType > 0.f && pType < 1.f) {
                            if (pType < 0.5f) {
                                brick.powerup = POWERUP_BIGPADDLE;
                            } else {
                                brick.powerup = POWERUP_MULTIBALL;
                            }
                        }

                        board[j].Append(brick);
                        numBricks++;
                    }

                    bx += stepx;
                }

                by += stepy;
            }
        }

        private void UpdateGame() {
            int i;

            if (onNewGame.oCastBoolean()) {
                ResetGameState();

                // Create Board
                SetCurrentBoard();

                gamerunning.oSet(true);
            }
            if (onContinue.oCastBoolean()) {
                gameOver = false;
                ballsRemaining = 3;

                onContinue.oSet(false);
            }
            if (onNewLevel.oCastBoolean()) {
                currentLevel++;

                ClearBoard();
                SetCurrentBoard();

                ballSpeed = BALL_SPEED * (1f + ((float) currentLevel / 5f));
                if (ballSpeed > BALL_MAXSPEED) {
                    ballSpeed = BALL_MAXSPEED;
                }
                updateScore = true;
                onNewLevel.oSet(false);
            }

            if (gamerunning.oCastBoolean() == true) {

                UpdatePaddle();
                UpdateBall();
                UpdatePowerups();

                for (i = 0; i < entities.Num(); i++) {
                    entities.oGet(i).Update(timeSlice, gui.GetTime());
                }

                // Delete entities that need to be deleted
                for (i = entities.Num() - 1; i >= 0; i--) {
                    if (entities.oGet(i).removed) {
                        BOEntity ent = entities.oGet(i);
//				delete ent;
                        entities.RemoveIndex(i);
                    }
                }

                if (updateScore) {
                    UpdateScore();
                    updateScore = false;
                }
            }
        }

        private void UpdatePowerups() {
            idVec2 pos = new idVec2();

            for (int i = 0; i < powerUps.Num(); i++) {
                BOEntity pUp = powerUps.oGet(i);

                // Check for powerup falling below screen
                if (pUp.position.y > 480) {

                    powerUps.RemoveIndex(i);
                    pUp.removed = true;
                    continue;
                }

                // Check for the paddle catching a powerup
                pos.x = pUp.position.x + (pUp.width / 2);
                pos.y = pUp.position.y + (pUp.height / 2);

                collideDir_t collision = paddle.checkCollision(pos, pUp.velocity);
                if (collision != COLLIDE_NONE) {
                    BOEntity ball;

                    // Give the powerup to the player
                    switch (pUp.powerup) {
                        case POWERUP_BIGPADDLE:
                            bigPaddleTime = gui.GetTime() + 15000;
                            break;
                        case POWERUP_MULTIBALL:
                            // Create 2 new balls in the spot of the existing ball
                            for (int b = 0; b < 2; b++) {
                                ball = CreateNewBall();
                                ball.position = balls.oGet(0).position;
                                ball.velocity = balls.oGet(0).velocity;

                                if (b == 0) {
                                    ball.velocity.x -= 35.f;
                                } else {
                                    ball.velocity.x += 35.f;
                                }
                                ball.velocity.NormalizeFast();
                                ball.velocity.oMulSet(ballSpeed);

                                ball.SetVisible(true);
                            }
                            break;
                        default:
                            break;
                    }

                    // Play the sound
                    session.sw.PlayShaderDirectly("arcade_powerup", S_UNIQUE_CHANNEL);

                    // Remove it
                    powerUps.RemoveIndex(i);
                    pUp.removed = true;
                }
            }
        }

        private void UpdatePaddle() {
            idVec2 cursorPos = new idVec2();
            float oldPos = paddle.x;

            cursorPos.x = gui.CursorX();
            cursorPos.y = gui.CursorY();

            if (bigPaddleTime > gui.GetTime()) {
                paddle.x = cursorPos.x - 80f;
                paddle.width = 160;
                paddle.ent.width = 160;
                paddle.ent.SetMaterial("game/bustout/doublepaddle");
            } else {
                paddle.x = cursorPos.x - 48f;
                paddle.width = 96;
                paddle.ent.width = 96;
                paddle.ent.SetMaterial("game/bustout/paddle");
            }
            paddle.ent.position.x = paddle.x;

            paddleVelocity = (paddle.x - oldPos);
        }
        private static int bounceChannel = 1;

        private void UpdateBall() {
            int ballnum, i, j;
            boolean playSoundBounce = false;
            boolean playSoundBrick = false;

            if (ballsInPlay == 0) {
                return;
            }

            for (ballnum = 0; ballnum < balls.Num(); ballnum++) {
                BOEntity ball = balls.oGet(ballnum);

                // Check for ball going below screen, lost ball
                if (ball.position.y > 480f) {
                    ball.removed = true;
                    continue;
                }

                // Check world collision
                if (ball.position.y < 20 && ball.velocity.y < 0) {
                    ball.velocity.y = -ball.velocity.y;

                    // Increase ball speed when it hits ceiling
                    if (!ballHitCeiling) {
                        ballSpeed *= 1.25f;
                        ballHitCeiling = true;
                    }
                    playSoundBounce = true;
                }

                if (ball.position.x > 608 && ball.velocity.x > 0) {
                    ball.velocity.x = -ball.velocity.x;
                    playSoundBounce = true;
                } else if (ball.position.x < 8 && ball.velocity.x < 0) {
                    ball.velocity.x = -ball.velocity.x;
                    playSoundBounce = true;
                }

                // Check for Paddle collision
                idVec2 ballCenter = ball.position.oPlus(new idVec2(BALL_RADIUS, BALL_RADIUS));
                collideDir_t collision = paddle.checkCollision(ballCenter, ball.velocity);

                if (collision == COLLIDE_UP) {
                    if (ball.velocity.y > 0) {
                        idVec2 paddleVec = new idVec2(paddleVelocity * 2, 0);
                        float centerX;

                        if (bigPaddleTime > gui.GetTime()) {
                            centerX = paddle.x + 80f;
                        } else {
                            centerX = paddle.x + 48f;
                        }

                        ball.velocity.y = -ball.velocity.y;

                        paddleVec.x += (ball.position.x - centerX) * 2;

                        ball.velocity.oPluSet(paddleVec);
                        ball.velocity.NormalizeFast();
                        ball.velocity.oMulSet(ballSpeed);

                        playSoundBounce = true;
                    }
                } else if (collision == COLLIDE_LEFT || collision == COLLIDE_RIGHT) {
                    if (ball.velocity.y > 0) {
                        ball.velocity.x = -ball.velocity.x;
                        playSoundBounce = true;
                    }
                }

                collision = COLLIDE_NONE;

                // Check for collision with bricks
                for (i = 0; i < BOARD_ROWS; i++) {
                    int num = board[i].Num();

                    for (j = 0; j < num; j++) {
                        BOBrick brick = (board[i]).oGet(j);

                        collision = brick.checkCollision(ballCenter, ball.velocity);
                        if (collision != null) {
                            // Now break the brick if there was a collision
                            brick.isBroken = true;
                            brick.ent.fadeOut = true;

                            if (brick.powerup.ordinal() > POWERUP_NONE.ordinal()) {
                                BOEntity pUp = CreatePowerup(brick);
                            }

                            numBricks--;
                            gameScore += 100;
                            updateScore = true;

                            // Go ahead an forcibly remove the last brick, no fade
                            if (numBricks == 0) {
                                brick.ent.removed = true;
                            }
                            board[i].Remove(brick);
                            break;
                        }
                    }

                    if (collision != null) {
                        playSoundBrick = true;
                        break;
                    }
                }

                if (collision == COLLIDE_DOWN || collision == COLLIDE_UP) {
                    ball.velocity.y *= -1;
                } else if (collision == COLLIDE_LEFT || collision == COLLIDE_RIGHT) {
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
            for (ballnum = 0; ballnum < balls.Num(); ballnum++) {
                if (balls.oGet(ballnum).removed) {
                    ballsInPlay--;
                    balls.RemoveIndex(ballnum);
                }
            }

            // If all the balls were removed, update the game accordingly
            if (ballsInPlay == 0) {
                if (ballsRemaining == 0) {
                    gameOver = true;

                    // Game Over sound
                    session.sw.PlayShaderDirectly("arcade_sadsound", S_UNIQUE_CHANNEL);
                } else {
                    ballsRemaining--;

                    // Ball was lost, but game is not over
                    session.sw.PlayShaderDirectly("arcade_missedball", S_UNIQUE_CHANNEL);
                }

                ClearPowerups();
                updateScore = true;
            }
        }

        private void UpdateScore() {

            if (gameOver) {
                gui.HandleNamedEvent("GameOver");//TODO:put text in property files for localization.
                return;
            }

            // Check for level progression
            if (numBricks == 0) {
                ClearBalls();

                gui.HandleNamedEvent("levelComplete");
            }

            // Check for new ball score
            if (gameScore >= nextBallScore) {
                ballsRemaining++;
                gui.HandleNamedEvent("extraBall");

                // Play sound
                session.sw.PlayShaderDirectly("arcade_extraball", S_UNIQUE_CHANNEL);

                nextBallScore = gameScore + 10000;
            }

            gui.SetStateString("player_score", va("%d", gameScore));
            gui.SetStateString("balls_remaining", va("%d", ballsRemaining));
            gui.SetStateString("current_level", va("%d", currentLevel));
            gui.SetStateString("next_ball_score", va("%d", nextBallScore));
        }

        private BOEntity CreateNewBall() {
            BOEntity ball;

            ball = new BOEntity(this);
            ball.position.x = 300f;
            ball.position.y = 416f;
            ball.SetMaterial("game/bustout/ball");
            ball.SetSize(BALL_RADIUS * 2f, BALL_RADIUS * 2f);
            ball.SetVisible(false);

            ballsInPlay++;

            balls.Append(ball);
            entities.Append(ball);

            return ball;
        }

        private BOEntity CreatePowerup(BOBrick brick) {
            BOEntity powerEnt = new BOEntity(this);

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

            powerUps.Append(powerEnt);
            entities.Append(powerEnt);

            return powerEnt;
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
            if (idStr.Icmp(_name, "onNewLevel") == 0) {
                onNewLevel.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "numLevels") == 0) {
                numLevels = src.ParseInt();

                // Load all the level images
                LoadBoardFiles();
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }
    };
}
