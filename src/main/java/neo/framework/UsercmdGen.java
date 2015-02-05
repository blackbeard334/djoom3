package neo.framework;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static neo.TempDump.SERIAL_SIZE;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.idCmdSystem;
import static neo.framework.Common.com_ticNumber;
import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_LAST_KEY;
import neo.framework.KeyInput.idKeyInput;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_ATTACK;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BACK;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON0;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON1;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON2;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON3;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON4;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON5;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON6;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_BUTTON7;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_DOWN;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_FORWARD;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE0;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE1;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE10;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE11;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE12;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE13;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE14;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE15;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE16;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE17;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE18;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE19;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE2;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE20;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE21;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE22;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE23;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE24;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE25;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE26;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE27;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE28;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE29;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE3;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE30;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE31;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE32;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE33;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE34;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE35;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE36;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE37;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE38;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE39;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE4;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE40;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE41;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE42;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE43;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE44;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE45;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE46;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE47;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE48;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE49;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE5;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE50;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE51;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE52;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE53;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE54;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE55;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE56;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE57;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE58;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE59;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE6;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE60;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE61;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE62;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE63;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE7;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE8;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_IMPULSE9;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_LEFT;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_LOOKDOWN;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_LOOKUP;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_MAX_BUTTONS;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_MLOOK;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_MOVELEFT;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_MOVERIGHT;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_NONE;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_RIGHT;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_SHOWSCORES;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_SPEED;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_STRAFE;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_UP;
import static neo.framework.UsercmdGen.usercmdButton_t.UB_ZOOM;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleShort;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.math.Angles.PITCH;
import static neo.idlib.math.Angles.YAW;
import static neo.idlib.math.Math_h.ANGLE2SHORT;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import static neo.sys.sys_public.joystickAxis_t.AXIS_FORWARD;
import static neo.sys.sys_public.joystickAxis_t.AXIS_SIDE;
import static neo.sys.sys_public.joystickAxis_t.AXIS_UP;
import static neo.sys.sys_public.joystickAxis_t.MAX_JOYSTICK_AXIS;
import static neo.sys.win_input.Sys_EndKeyboardInputEvents;
import static neo.sys.win_input.Sys_PollKeyboardInputEvents;
import static neo.sys.win_input.Sys_ReturnKeyboardInputEvent;
import static neo.sys.win_input.Sys_ReturnMouseInputEvent;
import static neo.sys.win_main.Sys_DebugPrintf;

/**
 *
 */
public class UsercmdGen {

    private static idUsercmdGenLocal localUsercmdGen = new idUsercmdGenLocal();
    public static  idUsercmdGen      usercmdGen      = localUsercmdGen;

    /*
     ===============================================================================

     Samples a set of user commands from player input.

     ===============================================================================
     */
    public static final int USERCMD_HZ           = 60;          // 60 frames per second
    public static final int USERCMD_MSEC         = 1000 / USERCMD_HZ;
    //
    // usercmd_t->button bits
    public static final int BUTTON_ATTACK        = BIT(0);
    public static final int BUTTON_RUN           = BIT(1);
    public static final int BUTTON_ZOOM          = BIT(2);
    public static final int BUTTON_SCORES        = BIT(3);
    public static final int BUTTON_MLOOK         = BIT(4);
    public static final int BUTTON_5             = BIT(5);
    public static final int BUTTON_6             = BIT(6);
    public static final int BUTTON_7             = BIT(7);
    //
    // usercmd_t->impulse commands
    public static final int IMPULSE_0            = 0;           // weap 0
    public static final int IMPULSE_1            = 1;           // weap 1
    public static final int IMPULSE_2            = 2;           // weap 2
    public static final int IMPULSE_3            = 3;           // weap 3
    public static final int IMPULSE_4            = 4;           // weap 4
    public static final int IMPULSE_5            = 5;           // weap 5
    public static final int IMPULSE_6            = 6;           // weap 6
    public static final int IMPULSE_7            = 7;           // weap 7
    public static final int IMPULSE_8            = 8;           // weap 8
    public static final int IMPULSE_9            = 9;           // weap 9
    public static final int IMPULSE_10           = 10;          // weap 10
    public static final int IMPULSE_11           = 11;          // weap 11
    public static final int IMPULSE_12           = 12;          // weap 12
    public static final int IMPULSE_13           = 13;          // weap reload
    public static final int IMPULSE_14           = 14;          // weap next
    public static final int IMPULSE_15           = 15;          // weap prev
    public static final int IMPULSE_16           = 16;          // <unused>
    public static final int IMPULSE_17           = 17;          // ready to play ( toggles ui_ready )
    public static final int IMPULSE_18           = 18;          // center view
    public static final int IMPULSE_19           = 19;          // show PDA/INV/MAP
    public static final int IMPULSE_20           = 20;          // toggle team ( toggles ui_team )
    public static final int IMPULSE_21           = 21;          // <unused>
    public static final int IMPULSE_22           = 22;          // spectate
    public static final int IMPULSE_23           = 23;          // <unused>
    public static final int IMPULSE_24           = 24;          // <unused>
    public static final int IMPULSE_25           = 25;          // <unused>
    public static final int IMPULSE_26           = 26;          // <unused>
    public static final int IMPULSE_27           = 27;          // <unused>
    public static final int IMPULSE_28           = 28;          // vote yes
    public static final int IMPULSE_29           = 29;          // vote no
    public static final int IMPULSE_40           = 40;          // use vehicle
    //
    // usercmd_t->flags
    public static final int UCF_IMPULSE_SEQUENCE = 0x0001;    // toggled every time an impulse command is sent
//

    public static class usercmd_t {

        public static final transient int SIZE = SERIAL_SIZE(new usercmd_t());

        public int  gameFrame;              // frame number
        public int  gameTime;               // game time
        public int  duplicateCount;         // duplication count for networking
        public byte buttons;                // buttons
        public byte forwardmove;            // forward/backward movement
        public byte rightmove;              // left/right movement
        public byte upmove;                 // up/down movement
        public short[] angles = new short[3];// view angles
        public short mx;                    // mouse delta x
        public short my;                    // mouse delta y
        public byte  impulse;               // impulse command
        public byte  flags;                 // additional flags
        public int   sequence;              // just for debugging

        public void ByteSwap() {            // on big endian systems, byte swap the shorts and ints
            angles[0] = LittleShort(angles[0]);
            angles[1] = LittleShort(angles[1]);
            angles[2] = LittleShort(angles[2]);
            sequence = LittleLong(sequence);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 83 * hash + this.buttons;
            hash = 83 * hash + this.forwardmove;
            hash = 83 * hash + this.rightmove;
            hash = 83 * hash + this.upmove;
            hash = 83 * hash + Arrays.hashCode(this.angles);
            hash = 83 * hash + this.mx;
            hash = 83 * hash + this.my;
            hash = 83 * hash + this.impulse;
            hash = 83 * hash + this.flags;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final usercmd_t other = (usercmd_t) obj;
            if (this.buttons != other.buttons) {
                return false;
            }
            if (this.forwardmove != other.forwardmove) {
                return false;
            }
            if (this.rightmove != other.rightmove) {
                return false;
            }
            if (this.upmove != other.upmove) {
                return false;
            }
            if (!Arrays.equals(this.angles, other.angles)) {
                return false;
            }
            if (this.mx != other.mx) {
                return false;
            }
            if (this.my != other.my) {
                return false;
            }
            if (this.impulse != other.impulse) {
                return false;
            }
            if (this.flags != other.flags) {
                return false;
            }
            return true;
        }
    };

    public enum inhibit_t {

        INHIBIT_SESSION,
        INHIBIT_ASYNC
    };
    static final int MAX_BUFFERED_USERCMD = 64;

    public static abstract class idUsercmdGen {

        // virtual 				~idUsercmdGen( void ) {}
        // Sets up all the cvars and console commands.
        public abstract void Init();

        // Prepares for a new map.
        public abstract void InitForNewMap();

        // Shut down.
        public abstract void Shutdown();

        // Clears all key states and face straight.
        public abstract void Clear();

        // Clears view angles.
        public abstract void ClearAngles();

        // When the console is down or the menu is up, only emit default usercmd, so the player isn't moving around.
        // Each subsystem (session and game) may want an inhibit will OR the requests.
        public abstract void InhibitUsercmd(inhibit_t subsystem, boolean inhibit);

        // Returns a buffered command for the given game tic.
        public abstract usercmd_t TicCmd(int ticNumber) throws idException;

        // Called async at regular intervals.
        public abstract void UsercmdInterrupt();

        // Set a value that can safely be referenced by UsercmdInterrupt() for each key binding.
        public abstract int CommandStringUsercmdData(final String cmdString);

        // Returns the number of user commands.
        public abstract int GetNumUserCommands();

        // Returns the name of a user command via index.
        public abstract String GetUserCommandName(int index);

        // Continuously modified, never reset. For full screen guis.
        public abstract void MouseState(int[] x, int[] y, int[] button, boolean[] down);

        // Directly sample a button.
        public abstract int ButtonState(int key);

        // Directly sample a keystate.
        public abstract int KeyState(int key);

        // Directly sample a usercmd.
        public abstract usercmd_t GetDirectUsercmd();
    };
//    
    static final int KEY_MOVESPEED = 127;

    public enum usercmdButton_t {

        UB_NONE,
        //
        UB_UP,
        UB_DOWN,
        UB_LEFT,
        UB_RIGHT,
        UB_FORWARD,
        UB_BACK,
        UB_LOOKUP,
        UB_LOOKDOWN,
        UB_STRAFE,
        UB_MOVELEFT,
        UB_MOVERIGHT,
        //
        UB_BUTTON0,
        UB_BUTTON1,
        UB_BUTTON2,
        UB_BUTTON3,
        UB_BUTTON4,
        UB_BUTTON5,
        UB_BUTTON6,
        UB_BUTTON7,
        //
        UB_ATTACK,
        UB_SPEED,
        UB_ZOOM,
        UB_SHOWSCORES,
        UB_MLOOK,
        //
        UB_IMPULSE0,
        UB_IMPULSE1,
        UB_IMPULSE2,
        UB_IMPULSE3,
        UB_IMPULSE4,
        UB_IMPULSE5,
        UB_IMPULSE6,
        UB_IMPULSE7,
        UB_IMPULSE8,
        UB_IMPULSE9,
        UB_IMPULSE10,
        UB_IMPULSE11,
        UB_IMPULSE12,
        UB_IMPULSE13,
        UB_IMPULSE14,
        UB_IMPULSE15,
        UB_IMPULSE16,
        UB_IMPULSE17,
        UB_IMPULSE18,
        UB_IMPULSE19,
        UB_IMPULSE20,
        UB_IMPULSE21,
        UB_IMPULSE22,
        UB_IMPULSE23,
        UB_IMPULSE24,
        UB_IMPULSE25,
        UB_IMPULSE26,
        UB_IMPULSE27,
        UB_IMPULSE28,
        UB_IMPULSE29,
        UB_IMPULSE30,
        UB_IMPULSE31,
        UB_IMPULSE32,
        UB_IMPULSE33,
        UB_IMPULSE34,
        UB_IMPULSE35,
        UB_IMPULSE36,
        UB_IMPULSE37,
        UB_IMPULSE38,
        UB_IMPULSE39,
        UB_IMPULSE40,
        UB_IMPULSE41,
        UB_IMPULSE42,
        UB_IMPULSE43,
        UB_IMPULSE44,
        UB_IMPULSE45,
        UB_IMPULSE46,
        UB_IMPULSE47,
        UB_IMPULSE48,
        UB_IMPULSE49,
        UB_IMPULSE50,
        UB_IMPULSE51,
        UB_IMPULSE52,
        UB_IMPULSE53,
        UB_IMPULSE54,
        UB_IMPULSE55,
        UB_IMPULSE56,
        UB_IMPULSE57,
        UB_IMPULSE58,
        UB_IMPULSE59,
        UB_IMPULSE60,
        UB_IMPULSE61,
        UB_IMPULSE62,
        UB_IMPULSE63,
        //
        UB_MAX_BUTTONS
    };

    static class userCmdString_t {

        public userCmdString_t(String string, usercmdButton_t button) {
            this.string = string;
            this.button = button;
        }

        String          string;
        usercmdButton_t button;
    };
//
    static final userCmdString_t[] userCmdStrings = {
        new userCmdString_t("_moveUp", UB_UP),
        new userCmdString_t("_moveDown", UB_DOWN),
        new userCmdString_t("_left", UB_LEFT),
        new userCmdString_t("_right", UB_RIGHT),
        new userCmdString_t("_forward", UB_FORWARD),
        new userCmdString_t("_back", UB_BACK),
        new userCmdString_t("_lookUp", UB_LOOKUP),
        new userCmdString_t("_lookDown", UB_LOOKDOWN),
        new userCmdString_t("_strafe", UB_STRAFE),
        new userCmdString_t("_moveLeft", UB_MOVELEFT),
        new userCmdString_t("_moveRight", UB_MOVERIGHT),
        //
        new userCmdString_t("_attack", UB_ATTACK),
        new userCmdString_t("_speed", UB_SPEED),
        new userCmdString_t("_zoom", UB_ZOOM),
        new userCmdString_t("_showScores", UB_SHOWSCORES),
        new userCmdString_t("_mlook", UB_MLOOK),
        //
        new userCmdString_t("_button0", UB_BUTTON0),
        new userCmdString_t("_button1", UB_BUTTON1),
        new userCmdString_t("_button2", UB_BUTTON2),
        new userCmdString_t("_button3", UB_BUTTON3),
        new userCmdString_t("_button4", UB_BUTTON4),
        new userCmdString_t("_button5", UB_BUTTON5),
        new userCmdString_t("_button6", UB_BUTTON6),
        new userCmdString_t("_button7", UB_BUTTON7),
        //
        new userCmdString_t("_impulse0", UB_IMPULSE0),
        new userCmdString_t("_impulse1", UB_IMPULSE1),
        new userCmdString_t("_impulse2", UB_IMPULSE2),
        new userCmdString_t("_impulse3", UB_IMPULSE3),
        new userCmdString_t("_impulse4", UB_IMPULSE4),
        new userCmdString_t("_impulse5", UB_IMPULSE5),
        new userCmdString_t("_impulse6", UB_IMPULSE6),
        new userCmdString_t("_impulse7", UB_IMPULSE7),
        new userCmdString_t("_impulse8", UB_IMPULSE8),
        new userCmdString_t("_impulse9", UB_IMPULSE9),
        new userCmdString_t("_impulse10", UB_IMPULSE10),
        new userCmdString_t("_impulse11", UB_IMPULSE11),
        new userCmdString_t("_impulse12", UB_IMPULSE12),
        new userCmdString_t("_impulse13", UB_IMPULSE13),
        new userCmdString_t("_impulse14", UB_IMPULSE14),
        new userCmdString_t("_impulse15", UB_IMPULSE15),
        new userCmdString_t("_impulse16", UB_IMPULSE16),
        new userCmdString_t("_impulse17", UB_IMPULSE17),
        new userCmdString_t("_impulse18", UB_IMPULSE18),
        new userCmdString_t("_impulse19", UB_IMPULSE19),
        new userCmdString_t("_impulse20", UB_IMPULSE20),
        new userCmdString_t("_impulse21", UB_IMPULSE21),
        new userCmdString_t("_impulse22", UB_IMPULSE22),
        new userCmdString_t("_impulse23", UB_IMPULSE23),
        new userCmdString_t("_impulse24", UB_IMPULSE24),
        new userCmdString_t("_impulse25", UB_IMPULSE25),
        new userCmdString_t("_impulse26", UB_IMPULSE26),
        new userCmdString_t("_impulse27", UB_IMPULSE27),
        new userCmdString_t("_impulse28", UB_IMPULSE28),
        new userCmdString_t("_impulse29", UB_IMPULSE29),
        new userCmdString_t("_impulse30", UB_IMPULSE30),
        new userCmdString_t("_impulse31", UB_IMPULSE31),
        new userCmdString_t("_impulse32", UB_IMPULSE32),
        new userCmdString_t("_impulse33", UB_IMPULSE33),
        new userCmdString_t("_impulse34", UB_IMPULSE34),
        new userCmdString_t("_impulse35", UB_IMPULSE35),
        new userCmdString_t("_impulse36", UB_IMPULSE36),
        new userCmdString_t("_impulse37", UB_IMPULSE37),
        new userCmdString_t("_impulse38", UB_IMPULSE38),
        new userCmdString_t("_impulse39", UB_IMPULSE39),
        new userCmdString_t("_impulse40", UB_IMPULSE40),
        new userCmdString_t("_impulse41", UB_IMPULSE41),
        new userCmdString_t("_impulse42", UB_IMPULSE42),
        new userCmdString_t("_impulse43", UB_IMPULSE43),
        new userCmdString_t("_impulse44", UB_IMPULSE44),
        new userCmdString_t("_impulse45", UB_IMPULSE45),
        new userCmdString_t("_impulse46", UB_IMPULSE46),
        new userCmdString_t("_impulse47", UB_IMPULSE47),
        new userCmdString_t("_impulse48", UB_IMPULSE48),
        new userCmdString_t("_impulse49", UB_IMPULSE49),
        new userCmdString_t("_impulse50", UB_IMPULSE50),
        new userCmdString_t("_impulse51", UB_IMPULSE51),
        new userCmdString_t("_impulse52", UB_IMPULSE52),
        new userCmdString_t("_impulse53", UB_IMPULSE53),
        new userCmdString_t("_impulse54", UB_IMPULSE54),
        new userCmdString_t("_impulse55", UB_IMPULSE55),
        new userCmdString_t("_impulse56", UB_IMPULSE56),
        new userCmdString_t("_impulse57", UB_IMPULSE57),
        new userCmdString_t("_impulse58", UB_IMPULSE58),
        new userCmdString_t("_impulse59", UB_IMPULSE59),
        new userCmdString_t("_impulse60", UB_IMPULSE60),
        new userCmdString_t("_impulse61", UB_IMPULSE61),
        new userCmdString_t("_impulse62", UB_IMPULSE62),
        new userCmdString_t("_impulse63", UB_IMPULSE63),
        //
        new userCmdString_t(null, UB_NONE)
    };

    static class buttonState_t {

        public int on;
        public boolean held;

        public buttonState_t() {
            Clear();
        }

        public void Clear() {
            held = false;
            on = 0;
        }

        public void SetKeyState(int keystate, boolean toggle) {
            if (!toggle) {
                held = false;
                on = keystate;
            } else if (0 == keystate) {
                held = false;
            } else if (!held) {
                held = true;
                on ^= 1;
            }
        }
    };
    //    
    //    
    static final int NUM_USER_COMMANDS = userCmdStrings.length;
    //
    static final int MAX_CHAT_BUFFER = 127;
    //    
    //   

    static class idUsercmdGenLocal extends idUsercmdGen {

        private idVec3        viewangles;
        private int           flags;
        private int           impulse;
        //
        private buttonState_t toggled_crouch;
        private buttonState_t toggled_run;
        private buttonState_t toggled_zoom;
        private int[]     buttonState = new int[etoi(UB_MAX_BUTTONS)];
        private boolean[] keyState    = new boolean[K_LAST_KEY];
        //
        private int       inhibitCommands;                      // true when in console or menu locally
        private int       lastCommandTime;
        //
        private boolean   initialized;
        //
        private usercmd_t cmd;                                  // the current cmd being built
        private usercmd_t[] buffered = new usercmd_t[MAX_BUFFERED_USERCMD];
        //
        private int continuousMouseX, continuousMouseY;         // for gui event generatioin, never zerod
        private int     mouseButton;                            // for gui event generatioin
        private boolean mouseDown;
        //
        private int     mouseDx, mouseDy;                       // added to by mouse events
        private int[]   joystickAxis = new int[etoi(MAX_JOYSTICK_AXIS)];// set by joystick events
        //
        private static final idCVar in_yawSpeed      = new idCVar("in_yawspeed", "140", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "yaw change speed when holding down _left or _right button");
        private static final idCVar in_pitchSpeed    = new idCVar("in_pitchspeed", "140", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "pitch change speed when holding down look _lookUp or _lookDown button");
        private static final idCVar in_angleSpeedKey = new idCVar("in_anglespeedkey", "1.5", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "angle change scale when holding down _speed button");
        private static final idCVar in_freeLook      = new idCVar("in_freeLook", "1", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_BOOL, "look around with mouse  = new idCVar(reverse _mlook button)");
        private static final idCVar in_alwaysRun     = new idCVar("in_alwaysRun", "0", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_BOOL, "always run  = new idCVar(reverse _speed button) - only in MP");
        private static final idCVar in_toggleRun     = new idCVar("in_toggleRun", "0", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_BOOL, "pressing _speed button toggles run on/off - only in MP");
        private static final idCVar in_toggleCrouch  = new idCVar("in_toggleCrouch", "0", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_BOOL, "pressing _movedown button toggles player crouching/standing");
        private static final idCVar in_toggleZoom    = new idCVar("in_toggleZoom", "0", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_BOOL, "pressing _zoom button toggles zoom on/off");
        private static final idCVar sensitivity      = new idCVar("sensitivity", "5", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "mouse view sensitivity");
        private static final idCVar m_pitch          = new idCVar("m_pitch", "0.022", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "mouse pitch scale");
        private static final idCVar m_yaw            = new idCVar("m_yaw", "0.022", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "mouse yaw scale");
        private static final idCVar m_strafeScale    = new idCVar("m_strafeScale", "6.25", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_FLOAT, "mouse strafe movement scale");
        private static final idCVar m_smooth         = new idCVar("m_smooth", "1", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_INTEGER, "number of samples blended for mouse viewing", 1, 8, new idCmdSystem.ArgCompletion_Integer(1, 8));
        private static final idCVar m_strafeSmooth   = new idCVar("m_strafeSmooth", "4", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_INTEGER, "number of samples blended for mouse moving", 1, 8, new idCmdSystem.ArgCompletion_Integer(1, 8));
        private static final idCVar m_showMouseRate  = new idCVar("m_showMouseRate", "0", CVAR_SYSTEM | CVAR_BOOL, "shows mouse movement");
        //
        //

        public idUsercmdGenLocal() {
            lastCommandTime = 0;
            initialized = false;

            flags = 0;
            impulse = 0;

            toggled_crouch = new buttonState_t();
            toggled_run = new buttonState_t();
            toggled_zoom = new buttonState_t();
            toggled_run.on = btoi(in_alwaysRun.GetBool());

            viewangles = new idVec3();//ClearAngles();
            this.cmd = new usercmd_t();
            Clear();
        }

        @Override
        public void Init() {
            initialized = true;
        }

        @Override
        public void InitForNewMap() {
            flags = 0;
            impulse = 0;

            toggled_crouch.Clear();
            toggled_run.Clear();
            toggled_zoom.Clear();
            toggled_run.on = in_alwaysRun.GetBool() ? 1 : 0;

            Clear();
            ClearAngles();
        }

        @Override
        public void Shutdown() {
            initialized = false;
        }

        @Override
        public void Clear() {
            // clears all key states 
//	memset( buttonState, 0, sizeof( buttonState ) );
//	memset( keyState, false, sizeof( keyState ) );
            Arrays.fill(buttonState, 0);
            Arrays.fill(keyState, false);

            inhibitCommands = 0;//false;

            mouseDx = mouseDy = 0;
            mouseButton = 0;
            mouseDown = false;
        }

        @Override
        public void ClearAngles() {
            viewangles.Zero();
        }

        /*
         ================
         idUsercmdGenLocal::TicCmd

         Returns a buffered usercmd
         ================
         */
        @Override
        public usercmd_t TicCmd(int ticNumber) throws idException {

            // the packetClient code can legally ask for com_ticNumber+1, because
            // it is in the async code and com_ticNumber hasn't been updated yet,
            // but all other code should never ask for anything > com_ticNumber
            if (ticNumber > com_ticNumber + 1) {
                common.Error("idUsercmdGenLocal::TicCmd ticNumber > com_ticNumber");
            }

            if (ticNumber <= com_ticNumber - MAX_BUFFERED_USERCMD) {
                // this can happen when something in the game code hitches badly, allowing the
                // async code to overflow the buffers
                //common.Printf( "warning: idUsercmdGenLocal::TicCmd ticNumber <= com_ticNumber - MAX_BUFFERED_USERCMD\n" );
            }

            return buffered[ticNumber & (MAX_BUFFERED_USERCMD - 1)];
        }

        @Override
        public void InhibitUsercmd(inhibit_t subsystem, boolean inhibit) {
            if (inhibit) {
                inhibitCommands |= 1 << subsystem.ordinal();
            } else {
                inhibitCommands &= (0xffffffff ^ (1 << subsystem.ordinal()));
            }
        }


        /*
         ================
         idUsercmdGenLocal::UsercmdInterrupt

         Called asyncronously
         ================
         */
        @Override
        public void UsercmdInterrupt() {
            // dedicated servers won't create usercmds
            if (!initialized) {
                return;
            }

//            Display.processMessages();
            // init the usercmd for com_ticNumber+1
            InitCurrent();

            // process the system mouse events
            Mouse();

            // process the system keyboard events
            Keyboard();

            // process the system joystick events
            Joystick();

            // create the usercmd for com_ticNumber+1
            MakeCurrent();

            // save a number for debugging cmdDemos and networking
            cmd.sequence = com_ticNumber + 1;

            buffered[(com_ticNumber + 1) & (MAX_BUFFERED_USERCMD - 1)] = cmd;
        }

        /*
         ================
         idUsercmdGenLocal::CommandStringUsercmdData

         Returns the button if the command string is used by the async usercmd generator.
         ================
         */
        @Override
        public int CommandStringUsercmdData(final String cmdString) {
            for (userCmdString_t ucs : userCmdStrings) {
                if (idStr.Icmp(cmdString, ucs.string) == 0) {
                    return ucs.button.ordinal();
                }
            }
            return UB_NONE.ordinal();
        }

        @Override
        public int GetNumUserCommands() {
            return NUM_USER_COMMANDS;
        }

        @Override
        public String GetUserCommandName(int index) {
            if (index >= 0 && index < NUM_USER_COMMANDS) {
                return userCmdStrings[index].string;
            }
            return "";
        }

        @Override
        public void MouseState(int[] x, int[] y, int[] button, boolean[] down) {
            x[0] = continuousMouseX;
            y[0] = continuousMouseY;
            button[0] = mouseButton;
            down[0] = mouseDown;
        }


        /*
         ===============
         idUsercmdGenLocal::ButtonState

         Returns (the fraction of the frame) that the key was down
         ===============
         */
        @Override
        public int ButtonState(int key) {
            if (key < 0 || key >= UB_MAX_BUTTONS.ordinal()) {
                return -1;
            }
            return (buttonState[key] > 0) ? 1 : 0;
        }

        public int ButtonState(usercmdButton_t key) {
            return ButtonState(key.ordinal());
        }


        /*
         ===============
         idUsercmdGenLocal::KeyState

         Returns (the fraction of the frame) that the key was down
         bk20060111
         ===============
         */
        @Override
        public int KeyState(int key) {
            if (key < 0 || key >= K_LAST_KEY) {
                return -1;
            }
            return (keyState[key]) ? 1 : 0;
        }

        @Override
        public usercmd_t GetDirectUsercmd() {

//            Display.processMessages();
            // initialize current usercmd
            InitCurrent();

            // process the system mouse events
            Mouse();

            // process the system keyboard events
            Keyboard();

//            // process the system joystick events
//            Joystick();
//TODO:enable our input devices.
            // create the usercmd
            MakeCurrent();

            cmd.duplicateCount = 0;

            return cmd;
        }

        /*
         ================
         idUsercmdGenLocal::MakeCurrent

         creates the current command for this frame
         ================
         */
        private void MakeCurrent() {
            idVec3 oldAngles;
            int i;

            oldAngles = viewangles;

            if (!Inhibited()) {
                // update toggled key states
                toggled_crouch.SetKeyState(ButtonState(UB_DOWN), in_toggleCrouch.GetBool());
                toggled_run.SetKeyState(ButtonState(UB_SPEED), in_toggleRun.GetBool() && idAsyncNetwork.IsActive());
                toggled_zoom.SetKeyState(ButtonState(UB_ZOOM), in_toggleZoom.GetBool());

                // keyboard angle adjustment
                AdjustAngles();

                // set button bits
                CmdButtons();

                // get basic movement from keyboard
                KeyMove();

                // get basic movement from mouse
                MouseMove();

                // get basic movement from joystick
                JoystickMove();

                // check to make sure the angles haven't wrapped
                if (viewangles.oGet(PITCH) - oldAngles.oGet(PITCH) > 90) {
                    viewangles.oSet(PITCH, oldAngles.oGet(PITCH) + 90);
                } else if (oldAngles.oGet(PITCH) - viewangles.oGet(PITCH) > 90) {
                    viewangles.oSet(PITCH, oldAngles.oGet(PITCH) - 90);
                }
            } else {
                mouseDx = 0;
                mouseDy = 0;
            }

            for (i = 0; i < 3; i++) {
                cmd.angles[i] = (short) ANGLE2SHORT(viewangles.oGet(i));
            }

            cmd.mx = (short) continuousMouseX;
            cmd.my = (short) continuousMouseY;

            flags = cmd.flags;
            impulse = cmd.impulse;
        }


        /*
         ================
         idUsercmdGenLocal::InitCurrent

         inits the current command for this frame
         ================
         */
        private void InitCurrent() {
            cmd = new usercmd_t();//memset( &cmd, 0, sizeof( cmd ) );
            cmd.flags = (byte) flags;
            cmd.impulse = (byte) impulse;
            cmd.buttons |= (in_alwaysRun.GetBool() && idAsyncNetwork.IsActive()) ? BUTTON_RUN : 0;
            cmd.buttons |= in_freeLook.GetBool() ? BUTTON_MLOOK : 0;
        }


        /*
         ================
         idUsercmdGenLocal::Inhibited

         is user cmd generation inhibited
         ================
         */
        private boolean Inhibited() {
            return (inhibitCommands != 0);
        }


        /*
         ================
         idUsercmdGenLocal::AdjustAngles

         Moves the local angle positions
         ================
         */
        private void AdjustAngles() {
            float speed;

            if ((toggled_run.on != 0) ^ (in_alwaysRun.GetBool() && idAsyncNetwork.IsActive())) {
                speed = idMath.M_MS2SEC * USERCMD_MSEC * in_angleSpeedKey.GetFloat();
            } else {
                speed = idMath.M_MS2SEC * USERCMD_MSEC;
            }

            if (0 == ButtonState(UB_STRAFE)) {
                viewangles.oMinSet(YAW, speed * in_yawSpeed.GetFloat() * ButtonState(UB_RIGHT));
                viewangles.oPluSet(YAW, speed * in_yawSpeed.GetFloat() * ButtonState(UB_LEFT));
            }

            viewangles.oMinSet(PITCH, speed * in_pitchSpeed.GetFloat() * ButtonState(UB_LOOKUP));
            viewangles.oPluSet(PITCH, speed * in_pitchSpeed.GetFloat() * ButtonState(UB_LOOKDOWN));
        }

        /*
         ================
         idUsercmdGenLocal::KeyMove

         Sets the usercmd_t based on key states
         ================
         */
        private void KeyMove() {
            int forward, side, up;

            forward = 0;
            side = 0;
            up = 0;
            if (ButtonState(UB_STRAFE) != 0) {
                side += KEY_MOVESPEED * ButtonState(UB_RIGHT);
                side -= KEY_MOVESPEED * ButtonState(UB_LEFT);
            }

            side += KEY_MOVESPEED * ButtonState(UB_MOVERIGHT);
            side -= KEY_MOVESPEED * ButtonState(UB_MOVELEFT);

            up -= KEY_MOVESPEED * toggled_crouch.on;
            up += KEY_MOVESPEED * ButtonState(UB_UP);

            forward += KEY_MOVESPEED * ButtonState(UB_FORWARD);
            forward -= KEY_MOVESPEED * ButtonState(UB_BACK);

            cmd.forwardmove = (byte) idMath.ClampChar(forward);
            cmd.rightmove = (byte) idMath.ClampChar(side);
            cmd.upmove = (byte) idMath.ClampChar(up);
        }

        private void JoystickMove() {
            float anglespeed;

            if ((toggled_run.on != 0) ^ (in_alwaysRun.GetBool() && idAsyncNetwork.IsActive())) {
                anglespeed = idMath.M_MS2SEC * USERCMD_MSEC * in_angleSpeedKey.GetFloat();
            } else {
                anglespeed = idMath.M_MS2SEC * USERCMD_MSEC;
            }

            if (0 == ButtonState(UB_STRAFE)) {
                viewangles.oPluSet(YAW, anglespeed * in_yawSpeed.GetFloat() * joystickAxis[AXIS_SIDE.ordinal()]);
                viewangles.oPluSet(PITCH, anglespeed * in_pitchSpeed.GetFloat() * joystickAxis[AXIS_FORWARD.ordinal()]);
            } else {
                cmd.rightmove = (byte) idMath.ClampChar(cmd.rightmove + joystickAxis[AXIS_SIDE.ordinal()]);
                cmd.forwardmove = (byte) idMath.ClampChar(cmd.forwardmove + joystickAxis[AXIS_FORWARD.ordinal()]);
            }

            cmd.upmove = (byte) idMath.ClampChar(cmd.upmove + joystickAxis[AXIS_UP.ordinal()]);
        }

        static int[][] history = new int[8][2];
        static int historyCounter;

        private void MouseMove() {
            float mx, my, strafeMx, strafeMy;
            int i;

            history[historyCounter & 7][0] = mouseDx;
            history[historyCounter & 7][1] = mouseDy;

            // allow mouse movement to be smoothed together
            int smooth = m_smooth.GetInteger();
            if (smooth < 1) {
                smooth = 1;
            }
            if (smooth > 8) {
                smooth = 8;
            }
            mx = 0;
            my = 0;
            for (i = 0; i < smooth; i++) {
                mx += history[(historyCounter - i + 8) & 7][0];
                my += history[(historyCounter - i + 8) & 7][1];
            }
            mx /= smooth;
            my /= smooth;

            // use a larger smoothing for strafing
            smooth = m_strafeSmooth.GetInteger();
            if (smooth < 1) {
                smooth = 1;
            }
            if (smooth > 8) {
                smooth = 8;
            }
            strafeMx = 0;
            strafeMy = 0;
            for (i = 0; i < smooth; i++) {
                strafeMx += history[(historyCounter - i + 8) & 7][0];
                strafeMy += history[(historyCounter - i + 8) & 7][1];
            }
            strafeMx /= smooth;
            strafeMy /= smooth;

            historyCounter++;

            if (idMath.Fabs(mx) > 1000 || idMath.Fabs(my) > 1000) {
                Sys_DebugPrintf("idUsercmdGenLocal.MouseMove: Ignoring ridiculous mouse delta.\n");
                mx = my = 0;
            }

            mx *= sensitivity.GetFloat();
            my *= sensitivity.GetFloat();

            if (m_showMouseRate.GetBool()) {
                Sys_DebugPrintf("[%3i %3i  = %5.1f %5.1f = %5.1f %5.1f] ", mouseDx, mouseDy, mx, my, strafeMx, strafeMy);
            }

            mouseDx = 0;
            mouseDy = 0;

            if (0.0f == strafeMx && 0.0f == strafeMy) {
                return;
            }

            if (ButtonState(UB_STRAFE) != 0 || 0 == (cmd.buttons & BUTTON_MLOOK)) {
                // add mouse X/Y movement to cmd
                strafeMx *= m_strafeScale.GetFloat();
                strafeMy *= m_strafeScale.GetFloat();
                // clamp as a vector, instead of separate floats
                float len = (float) Math.sqrt(strafeMx * strafeMx + strafeMy * strafeMy);
                if (len > 127) {
                    strafeMx = strafeMx * 127 / len;
                    strafeMy = strafeMy * 127 / len;
                }
            }

            if (0 == ButtonState(UB_STRAFE)) {
                viewangles.oMinSet(YAW, m_yaw.GetFloat() * mx);
            } else {
                cmd.rightmove = (byte) idMath.ClampChar((int) (cmd.rightmove + strafeMx));
            }

            if (0 == ButtonState(UB_STRAFE) && (cmd.buttons & BUTTON_MLOOK) != 0) {
                viewangles.oPluSet(PITCH, m_pitch.GetFloat() * my);
            } else {
                cmd.forwardmove = (byte) idMath.ClampChar((int) (cmd.forwardmove - strafeMy));
            }
        }

        private void CmdButtons() {
            int i;

            cmd.buttons = 0;

            // figure button bits
            for (i = 0; i <= 7; i++) {
                if (ButtonState( /*(usercmdButton_t)*/(UB_BUTTON0.ordinal() + i)) != 0) {
                    cmd.buttons |= 1 << i;
                }
            }

            // check the attack button
            if (ButtonState(UB_ATTACK) != 0) {
                cmd.buttons |= BUTTON_ATTACK;
            }

            // check the run button
            if ((toggled_run.on != 0) ^ (in_alwaysRun.GetBool() && idAsyncNetwork.IsActive())) {
                cmd.buttons |= BUTTON_RUN;
            }

            // check the zoom button
            if (toggled_zoom.on != 0) {
                cmd.buttons |= BUTTON_ZOOM;
            }

            // check the scoreboard button
            if (ButtonState(UB_SHOWSCORES) != 0 || ButtonState(UB_IMPULSE19) != 0) {
                // the button is toggled in SP mode as well but without effect
                cmd.buttons |= BUTTON_SCORES;
            }

            // check the mouse look button
            if ((ButtonState(UB_MLOOK) ^ in_freeLook.GetInteger()) != 0) {
                cmd.buttons |= BUTTON_MLOOK;
            }
        }

        private void Mouse() {
            int i, numEvents;
//
//            numEvents = Sys_PollMouseInputEvents();

//            if (numEvents != 0) {
            //
            // Study each of the buffer elements and process them.
            //
//            if (Mouse.next()) {
//                    sys_mEvents[] action = new sys_mEvents[1];;
            int[] action = {0};
            int[] value = {0};
            Sys_ReturnMouseInputEvent(action, value);
//                if (Sys_ReturnMouseInputEvent(action, value)) {
//                    if (action[0] >= etoi(M_ACTION1) && action[0] <= etoi(M_ACTION8)) {
//                        mouseButton = K_MOUSE1 + (action[0] - etoi(M_ACTION1));
//                        mouseDown = (value[0] != 0);
//                        Key(mouseButton, mouseDown);
//                    } else {
//                        switch (sys_mEvents.values()[action[0]]) {
//                            case M_DELTAX:
//                                mouseDx += value[0];
//                                continuousMouseX += value[0];
//                                break;
//                            case M_DELTAY:
//                                mouseDy += value[0];
//                                continuousMouseY += value[0];
//                                break;
//                            case M_DELTAZ:
//                                int key = value[0] < 0 ? K_MWHEELDOWN : K_MWHEELUP;
//                                value[0] = Math.abs(value[0]);
//                                while (value[0]-- > 0) {
//                                    Key(key, true);
//                                    Key(key, false);
//                                    mouseButton = key;
//                                    mouseDown = true;
//                                }
//                                break;
//                        }
//                    }
//                }
//        }
//            }

//            Sys_EndMouseInputEvents();
        }

        private void Keyboard() {

            int numEvents = Sys_PollKeyboardInputEvents();

            if (numEvents != 0) {
                //
                // Study each of the buffer elements and process them.
                //
                int[] key = {0};
                boolean[] state = {false};
                for (int i = 0; i < numEvents; i++) {
                    if (Sys_ReturnKeyboardInputEvent(i, key, state) != 0) {
                        Key(key[0], state[0]);
                    }
                }
            }

            Sys_EndKeyboardInputEvents();
        }

        private void Joystick() {
//	memset( joystickAxis, 0, sizeof( joystickAxis ) );
            Arrays.fill(joystickAxis, 0);
        }

        /*
         ===================
         idUsercmdGenLocal::Key

         Handles async mouse/keyboard button actions
         ===================
         */
        private void Key(int keyNum, boolean down) {

            // Sanity check, sometimes we get double message :(
            if (keyState[keyNum] == down) {
                return;
            }
            keyState[keyNum] = down;

            int action = idKeyInput.GetUsercmdAction(keyNum);

            if (down) {

                buttonState[action]++;

                if (!Inhibited()) {
                    if (action >= UB_IMPULSE0.ordinal() && action <= UB_IMPULSE61.ordinal()) {
                        cmd.impulse = (byte) (action - UB_IMPULSE0.ordinal());
                        cmd.flags ^= UCF_IMPULSE_SEQUENCE;
                    }
                }
            } else {
                buttonState[action]--;
                // we might have one held down across an app active transition
                if (buttonState[action] < 0) {
                    buttonState[action] = 0;
                }
            }
        }
    };

    public static void setUsercmdGen(idUsercmdGen usercmdGen) {
        UsercmdGen.usercmdGen = localUsercmdGen = (idUsercmdGenLocal) usercmdGen;
    }

}
