package neo.sys;

import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_ALT;
import static neo.framework.KeyInput.K_BACKSPACE;
import static neo.framework.KeyInput.K_CAPSLOCK;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DEL;
import static neo.framework.KeyInput.K_DOWNARROW;
import static neo.framework.KeyInput.K_END;
import static neo.framework.KeyInput.K_ENTER;
import static neo.framework.KeyInput.K_F1;
import static neo.framework.KeyInput.K_F10;
import static neo.framework.KeyInput.K_F11;
import static neo.framework.KeyInput.K_F12;
import static neo.framework.KeyInput.K_F2;
import static neo.framework.KeyInput.K_F3;
import static neo.framework.KeyInput.K_F4;
import static neo.framework.KeyInput.K_F5;
import static neo.framework.KeyInput.K_F6;
import static neo.framework.KeyInput.K_F7;
import static neo.framework.KeyInput.K_F8;
import static neo.framework.KeyInput.K_F9;
import static neo.framework.KeyInput.K_HOME;
import static neo.framework.KeyInput.K_INS;
import static neo.framework.KeyInput.K_KP_5;
import static neo.framework.KeyInput.K_KP_DEL;
import static neo.framework.KeyInput.K_KP_DOWNARROW;
import static neo.framework.KeyInput.K_KP_END;
import static neo.framework.KeyInput.K_KP_ENTER;
import static neo.framework.KeyInput.K_KP_EQUALS;
import static neo.framework.KeyInput.K_KP_HOME;
import static neo.framework.KeyInput.K_KP_INS;
import static neo.framework.KeyInput.K_KP_LEFTARROW;
import static neo.framework.KeyInput.K_KP_MINUS;
import static neo.framework.KeyInput.K_KP_NUMLOCK;
import static neo.framework.KeyInput.K_KP_PGDN;
import static neo.framework.KeyInput.K_KP_PGUP;
import static neo.framework.KeyInput.K_KP_PLUS;
import static neo.framework.KeyInput.K_KP_RIGHTARROW;
import static neo.framework.KeyInput.K_KP_SLASH;
import static neo.framework.KeyInput.K_KP_STAR;
import static neo.framework.KeyInput.K_KP_UPARROW;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_LWIN;
import static neo.framework.KeyInput.K_MENU;
import static neo.framework.KeyInput.K_PAUSE;
import static neo.framework.KeyInput.K_PGDN;
import static neo.framework.KeyInput.K_PGUP;
import static neo.framework.KeyInput.K_PRINT_SCR;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.framework.KeyInput.K_RIGHT_ALT;
import static neo.framework.KeyInput.K_RWIN;
import static neo.framework.KeyInput.K_SCROLL;
import static neo.framework.KeyInput.K_SHIFT;
import static neo.framework.KeyInput.K_UPARROW;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.win_local.win32;
import static neo.sys.win_main.Sys_QueEvent;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.awt.event.InputEvent;

import org.lwjgl.glfw.GLFW;

import neo.TempDump.TODO_Exception;
import neo.idlib.Text.Str.idStr;

/**
 *
 */
public class win_input {

    static final int DINPUT_BUFFERSIZE = 256;

    static final int CHAR_FIRSTREPEAT = 200;
    static final int CHAR_REPEAT = 100;


    // class MYDATA {
	// long  lX;                   // X axis goes here
	// long  lY;                   // Y axis goes here
	// long  lZ;                   // Z axis goes here
	// byte  bButtonA;             // One button goes here
	// byte  bButtonB;             // Another button goes here
	// byte  bButtonC;             // Another button goes here
	// byte  bButtonD;             // Another button goes here
// } ;

// static DIOBJECTDATAFORMAT rgodf[] = {
  // { &GUID_XAxis,    FIELD_OFFSET(MYDATA, lX),       DIDFT_AXIS | DIDFT_ANYINSTANCE,   0,},
  // { &GUID_YAxis,    FIELD_OFFSET(MYDATA, lY),       DIDFT_AXIS | DIDFT_ANYINSTANCE,   0,},
  // { &GUID_ZAxis,    FIELD_OFFSET(MYDATA, lZ),       0x80000000 | DIDFT_AXIS | DIDFT_ANYINSTANCE,   0,},
  // { 0,              FIELD_OFFSET(MYDATA, bButtonA), DIDFT_BUTTON | DIDFT_ANYINSTANCE, 0,},
  // { 0,              FIELD_OFFSET(MYDATA, bButtonB), DIDFT_BUTTON | DIDFT_ANYINSTANCE, 0,},
  // { 0,              FIELD_OFFSET(MYDATA, bButtonC), 0x80000000 | DIDFT_BUTTON | DIDFT_ANYINSTANCE, 0,},
  // { 0,              FIELD_OFFSET(MYDATA, bButtonD), 0x80000000 | DIDFT_BUTTON | DIDFT_ANYINSTANCE, 0,},
// };

//==========================================================================

static final char[] s_scantokey/*[256]*/ = {
    //  0            1       2          3          4       5            6         7
    //  8            9       A          B          C       D            E         F
	0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
	'7',        '8',    '9',       '0',        '-',    '=',          K_BACKSPACE, 9, // 0
	'q',        'w',    'e',       'r',        't',    'y',         'u',      'i',
	'o',        'p',    '[',       ']',        K_ENTER,K_CTRL,      'a',      's',   // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      ';',
	'\'',       '`',    K_SHIFT,   '\\',       'z',    'x',         'c',      'v',   // 2
	'b',        'n',    'm',       ',',        '.',    '/',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           0,        K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 7
    // shifted
    0,           27,    '!',       '@',        '#',    '$',         '%',      '^',
    '&',        '*',    '(',       ')',        '_',    '+',          K_BACKSPACE, 9, // 0
    'Q',        'W',    'E',       'R',        'T',    'Y',         'U',      'I',
    'O',        'P',    '[',       ']',        K_ENTER,K_CTRL,      'A',      'S',   // 1
    'D',        'F',    'G',       'H',        'J',    'K',         'L',      ';',
    '\'',       '~',    K_SHIFT,   '\\',       'Z',    'X',         'C',      'V',   // 2
	'B',        'B',    'M',       ',',        '.',    '/',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           0,        K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0      // 7
};

static final char[] s_scantokey_german/*[256]*/ = {
    //  0            1       2          3          4       5            6         7
    //  8            9       A          B          C       D            E         F
	0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
	'7',        '8',    '9',       '0',        '?',    '\'',        K_BACKSPACE, 9,  // 0
	'q',        'w',    'e',       'r',        't',    'z',         'u',      'i',
	'o',        'p',    '=',       '+',        K_ENTER,K_CTRL,      'a',      's',   // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      '[',
	']',        '`',    K_SHIFT,   '#',        'y',    'x',         'c',      'v',   // 2
	'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,      // 7
    // shifted
	0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
	'7',        '8',    '9',       '0',        '?',    '\'',        K_BACKSPACE, 9,  // 0
	'q',        'w',    'e',       'r',        't',    'z',         'u',      'i',
	'o',        'p',    '=',       '+',        K_ENTER,K_CTRL,      'a',      's',   // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      '[',
	']',        '`',    K_SHIFT,   '#',        'y',    'x',         'c',      'v',   // 2
	'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0      // 7
};

static final char[] s_scantokey_french/*[256]*/ = {
    //  0            1       2          3          4       5            6         7
    //  8            9       A          B          C       D            E         F
	0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
	'7',        '8',    '9',       '0',        ')',    '=',         K_BACKSPACE, 9, // 0
	'a',        'z',    'e',       'r',        't',    'y',         'u',      'i',
	'o',        'p',    '^',       '$',        K_ENTER,K_CTRL,      'q',      's',      // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      'm',
	'ù',        '`',    K_SHIFT,   '*',        'w',    'x',         'c',      'v',      // 2
	'b',        'n',    ',',       ';',        ':',    '!',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,      // 7
    // shifted
	0,           27,    '&',       'é',        '\"',    '\'',         '(',      '-',
	'è',        '_',    'ç',       'à',        '°',    '+',         K_BACKSPACE, 9, // 0
	'a',        'z',    'e',       'r',        't',    'y',         'u',      'i',
	'o',        'p',    '^',       '$',        K_ENTER,K_CTRL,      'q',      's',      // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      'm',
	'ù',        0,    K_SHIFT,   '*',        'w',    'x',         'c',      'v',      // 2
	'b',        'n',    ',',       ';',        ':',    '!',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0      // 7
};

static final char[] s_scantokey_spanish/*[256]*/ = {
    //  0            1       2          3          4       5            6         7
    //  8            9       A          B          C       D            E         F
	0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
	'7',        '8',    '9',       '0',        '\'',   '¡',         K_BACKSPACE, 9,  // 0
	'q',        'w',    'e',       'r',        't',    'y',         'u',      'i',
	'o',        'p',    '`',       '+',        K_ENTER,K_CTRL,      'a',      's',   // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      'ñ',
	'´',        'º',    K_SHIFT,   'ç',        'z',    'x',         'c',      'v',   // 2
	'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,      // 7
    // shifted
	0,           27,    '!',       '\"',        '·',    '$',         '%',      '&',
	'/',        '(',    ')',       '=',        '?',   '¿',         K_BACKSPACE, 9,  // 0
	'q',        'w',    'e',       'r',        't',    'y',         'u',      'i',
	'o',        'p',    '^',       '*',        K_ENTER,K_CTRL,      'a',      's',   // 1
	'd',        'f',    'g',       'h',        'j',    'k',         'l',      'Ñ',
	'¨',        'ª',    K_SHIFT,   'Ç',        'z',    'x',         'c',      'v',   // 2
	'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
	K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
	K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
	K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
	K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
	K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0,     // 6
	0,          0,      0,         0,          0,      0,           0,        0,
	0,          0,      0,         0,          0,      0,           0,        0      // 7
};

static final char[] s_scantokey_italian/*[256]*/ = {
    //  0            1       2          3          4       5            6         7
    //  8            9       A          B          C       D            E         F
        0,           27,    '1',       '2',        '3',    '4',         '5',      '6',
        '7',        '8',    '9',       '0',        '\'',   'ì',         K_BACKSPACE, 9,  // 0
        'q',        'w',    'e',       'r',        't',    'y',         'u',      'i',
        'o',        'p',    'è',       '+',        K_ENTER,K_CTRL,      'a',      's',   // 1
        'd',        'f',    'g',       'h',        'j',    'k',         'l',      'ò',
        'à',        '\\',    K_SHIFT,   'ù',        'z',    'x',         'c',      'v',   // 2
        'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
        K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
        K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
        K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
        K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
        K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
        0,          0,      0,         0,          0,      0,           0,        0,
        0,          0,      0,         0,          0,      0,           0,        0,     // 6
        0,          0,      0,         0,          0,      0,           0,        0,
        0,          0,      0,         0,          0,      0,           0,        0,      // 7
    // shifted
        0,           27,    '!',       '\"',        '£',    '$',         '%',      '&',
        '/',        '(',    ')',       '=',        '?',   '^',         K_BACKSPACE, 9,  // 0
        'q',        'w',    'e',       'r',        't',    'y',         'u',      'i',
        'o',        'p',    'é',       '*',        K_ENTER,K_CTRL,      'a',      's',   // 1
        'd',        'f',    'g',       'h',        'j',    'k',         'l',      'ç',
        '°',        '|',    K_SHIFT,   '§',        'z',    'x',         'c',      'v',   // 2
        'b',        'n',    'm',       ',',        '.',    '-',         K_SHIFT,  K_KP_STAR,
        K_ALT,      ' ',    K_CAPSLOCK,K_F1,       K_F2,   K_F3,        K_F4,     K_F5,  // 3
        K_F6,       K_F7,   K_F8,      K_F9,       K_F10,  K_PAUSE,     K_SCROLL, K_HOME,
        K_UPARROW,  K_PGUP, K_KP_MINUS,K_LEFTARROW,K_KP_5, K_RIGHTARROW,K_KP_PLUS,K_END, // 4
        K_DOWNARROW,K_PGDN, K_INS,     K_DEL,      0,      0,           '<',      K_F11,
        K_F12,      0,      0,         K_LWIN,     K_RWIN, K_MENU,      0,        0,     // 5
        0,          0,      0,         0,          0,      0,           0,        0,
        0,          0,      0,         0,          0,      0,           0,        0,     // 6
        0,          0,      0,         0,          0,      0,           0,        0,
        0,          0,      0,         0,          0,      0,           0,        0		 // 7


};

static char[] keyScanTable = s_scantokey;

// this should be part of the scantables and the scan tables should be 512 bytes
// (256 scan codes, shifted and unshifted).  Changing everything to use 512 byte
// scan tables now might introduce bugs in tested code.  Since we only need to fix
// the right-alt case for non-US keyboards, we're just using a special-case table
// for it.  Eventually, the tables above should be fixed to handle all possible
// scan codes instead of just the first 128.
    static int rightAltKey = K_ALT;
    static final InputEvent[]/*DIDEVICEOBJECTDATA*/ polled_didod = new InputEvent[DINPUT_BUFFERSIZE];// Receives buffered data 

    static int diFetch;
    static byte[][] toggleFetch = new byte[2][256];


    /*
     ============================================================

     DIRECT INPUT KEYBOARD CONTROL

     ============================================================
     */
    public static boolean IN_StartupKeyboard() {

//        try {
//            Keyboard.create();
//
//            if (Keyboard.isCreated()) {
//                common.Printf("keyboard: DirectInput initialized.\n");
//                return true;
//            }
//        } catch (LWJGLException ex) {
//            common.Printf("keyboard: couldn't find a keyboard device\n");
//        }
        return false;
    }

    /*
     =======
     MapKey

     Map from windows to quake keynums

     FIXME: scan code tables should include the upper 128 scan codes instead
     of having to special-case them here.  The current code makes it difficult
     to special-case conversions for non-US keyboards.  Currently the only
     special-case is for right alt.
     =======
     */
    public static int IN_DIMapKey(final int key, final int scancode, final int mods) {

        if ((key >= 260) && (scancode >= 128)) {
            switch (key) {
                case GLFW.GLFW_KEY_HOME:
                    return K_HOME;
                case GLFW.GLFW_KEY_UP:
                    return K_UPARROW;
                case GLFW.GLFW_KEY_PAGE_UP:
                    return K_PGUP;
                case GLFW.GLFW_KEY_LEFT:
                    return K_LEFTARROW;
                case GLFW.GLFW_KEY_RIGHT:
                    return K_RIGHTARROW;
                case GLFW.GLFW_KEY_END:
                    return K_END;
                case GLFW.GLFW_KEY_DOWN:
                    return K_DOWNARROW;
                case GLFW.GLFW_KEY_PAGE_DOWN:
                    return K_PGDN;
                case GLFW.GLFW_KEY_INSERT:
                    return K_INS;
                case GLFW.GLFW_KEY_DELETE:
                    return K_DEL;
                case GLFW.GLFW_KEY_RIGHT_ALT:
                    return rightAltKey;
                case GLFW.GLFW_KEY_RIGHT_CONTROL:
                    return K_CTRL;
                case GLFW.GLFW_KEY_KP_ENTER:
                    return K_KP_ENTER;
                case GLFW.GLFW_KEY_KP_EQUAL:
                    return K_KP_EQUALS;
                case GLFW.GLFW_KEY_PAUSE:
                    return K_PAUSE;
                case GLFW.GLFW_KEY_KP_DIVIDE:
                    return K_KP_SLASH;
                case GLFW.GLFW_KEY_LEFT_SUPER:
                    return K_LWIN;
                case GLFW.GLFW_KEY_RIGHT_SUPER:
                    return K_RWIN;
                case GLFW.GLFW_KEY_MENU:
                    return K_MENU;
                case GLFW.GLFW_KEY_PRINT_SCREEN:
                    return K_PRINT_SCR;

                case GLFW.GLFW_KEY_KP_7:
                    return K_KP_HOME;
                case GLFW.GLFW_KEY_KP_8:
                    return K_KP_UPARROW;
                case GLFW.GLFW_KEY_KP_9:
                    return K_KP_PGUP;
                case GLFW.GLFW_KEY_KP_4:
                    return K_KP_LEFTARROW;
                case GLFW.GLFW_KEY_KP_5:
                    return K_KP_5;
                case GLFW.GLFW_KEY_KP_6:
                    return K_KP_RIGHTARROW;
                case GLFW.GLFW_KEY_KP_1:
                    return K_KP_END;
                case GLFW.GLFW_KEY_KP_2:
                    return K_KP_DOWNARROW;
                case GLFW.GLFW_KEY_KP_3:
                    return K_KP_PGDN;
                case GLFW.GLFW_KEY_KP_0:
                    return K_KP_INS;
                case GLFW.GLFW_KEY_KP_DECIMAL:
                    return K_KP_DEL;
                case GLFW.GLFW_KEY_KP_SUBTRACT:
                    return K_KP_MINUS;
                case GLFW.GLFW_KEY_KP_ADD:
                    return K_KP_PLUS;
                case GLFW.GLFW_KEY_NUM_LOCK:
                    return K_KP_NUMLOCK;
                case GLFW.GLFW_KEY_KP_MULTIPLY:
                    return K_KP_STAR;
                default:
                    return 0;
            }
        }
        if (scancode > 256) {
			return 0;
		}

        return keyScanTable[getShiftedScancode(key, scancode, mods)];
    }

    private static int getShiftedScancode(final int key, final int scancode, final int mods) {
        int shiftedCode = scancode;
        if (isShiftableKey(key)) {
            if (((GLFW.GLFW_MOD_CAPS_LOCK & mods) != 0) && isShiftableLetter(key)) {
				shiftedCode += 128;
			}
            if ((GLFW.GLFW_MOD_SHIFT & mods) != 0) {
				shiftedCode += 128;
			}
        }
        return shiftedCode % 256;
    }

    private static boolean isShiftableKey(final int key) {
        return (key == GLFW.GLFW_KEY_APOSTROPHE) || (key == GLFW.GLFW_KEY_COMMA) || (key == GLFW.GLFW_KEY_MINUS) || (key == GLFW.GLFW_KEY_PERIOD) || (key == GLFW.GLFW_KEY_SLASH) || (key == GLFW.GLFW_KEY_0) || (key == GLFW.GLFW_KEY_1) || (key == GLFW.GLFW_KEY_2) || (key == GLFW.GLFW_KEY_3) || (key == GLFW.GLFW_KEY_4) || (key == GLFW.GLFW_KEY_5) || (key == GLFW.GLFW_KEY_6) || (key == GLFW.GLFW_KEY_7) || (key == GLFW.GLFW_KEY_8) || (key == GLFW.GLFW_KEY_9) || (key == GLFW.GLFW_KEY_SEMICOLON) || (key == GLFW.GLFW_KEY_EQUAL) ||
                isShiftableLetter(key)
                || (key == GLFW.GLFW_KEY_LEFT_BRACKET) || (key == GLFW.GLFW_KEY_BACKSLASH) || (key == GLFW.GLFW_KEY_RIGHT_BRACKET) || (key == GLFW.GLFW_KEY_GRAVE_ACCENT) || (key == GLFW.GLFW_KEY_WORLD_1) || (key == GLFW.GLFW_KEY_WORLD_2);
    }

    private static boolean isShiftableLetter(final int key) {
        return (key >= GLFW.GLFW_KEY_A) && (key <= GLFW.GLFW_KEY_Z);
    }

    /*
     ==========================
     IN_DeactivateKeyboard
     ==========================
     */
    public static void IN_DeactivateKeyboard() {
//        if (Keyboard.isCreated()) {
//            Keyboard.destroy();
//        }
    }

    /*
     ============================================================

     DIRECT INPUT MOUSE CONTROL

     ============================================================
     */

    /*
     ========================
     IN_InitDirectInput
     ========================
     */
    public static void IN_InitDirectInput() {
        throw new TODO_Exception();
//    HRESULT		hr;
//
//	common->Printf( "Initializing DirectInput...\n" );
//
//	if ( win32.g_pdi != NULL ) {
//		win32.g_pdi->Release();			// if the previous window was destroyed we need to do this
//		win32.g_pdi = NULL;
//	}
//
//    // Register with the DirectInput subsystem and get a pointer
//    // to a IDirectInput interface we can use.
//    // Create the base DirectInput object
//	if ( FAILED( hr = DirectInput8Create( GetModuleHandle(NULL), DIRECTINPUT_VERSION, IID_IDirectInput8, (void**)&win32.g_pdi, NULL ) ) ) {
//		common->Printf ("DirectInputCreate failed\n");
//    }
    }

    /*
     ========================
     IN_InitDIMouse
     ========================
     */
    public static boolean IN_InitDIMouse() {
//        try {
//            Mouse.create();
//            Mouse.setClipMouseCoordinatesToWindow(true);
//            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
//
//            if (Mouse.isCreated()) {
//                common.Printf("mouse: DirectInput initialized.\n");
//                return true;
//            }
//        } catch (LWJGLException ex) {//TODO:expand this.
//            common.Printf("mouse: Couldn't open DI mouse device\n");
//        }
        return false;
    }


    /*
     ==========================
     IN_ActivateMouse
     ==========================
     */
    public static void IN_ActivateMouse() {
        throw new TODO_Exception();
//	int i;
//	HRESULT hr;
//
//	if ( !win32.in_mouse.GetBool() || win32.mouseGrabbed || !win32.g_pMouse ) {
//		return;
//	}
//
//	win32.mouseGrabbed = true;
//	for ( i = 0; i < 10; i++ ) {
//		if ( ::ShowCursor( false ) < 0 ) {
//			break;
//		}
//	}
//
//	// we may fail to reacquire if the window has been recreated
//	hr = win32.g_pMouse->Acquire();
//	if (FAILED(hr)) {
//		return;
//	}
//
//	// set the cooperativity level.
//	hr = win32.g_pMouse->SetCooperativeLevel( win32.hWnd, DISCL_EXCLUSIVE | DISCL_FOREGROUND);
    }

    /*
     ==========================
     IN_DeactivateMouse
     ==========================
     */
    public static void IN_DeactivateMouse() {
//        if (Mouse.isCreated()) {
//            Mouse.destroy();
//        }
    }

    /*
     ==========================
     IN_DeactivateMouseIfWindowed
     ==========================
     */
    public static void IN_DeactivateMouseIfWindowed() {
        throw new TODO_Exception();
//	if ( !win32.cdsFullscreen ) {
//		IN_DeactivateMouse();
//	}
    }

    /*
     ============================================================

     MOUSE CONTROL

     ============================================================
     */
    /*
     ===========
     Sys_ShutdownInput
     ===========
     */
    public static void Sys_ShutdownInput() {

        IN_DeactivateMouse();
        IN_DeactivateKeyboard();
        if (win32.g_pKeyboard != null) {
//		win32.g_pKeyboard->Release();
            win32.g_pKeyboard = null;
        }

        if (win32.g_pMouse != null) {
//		win32.g_pMouse->Release();
            win32.g_pMouse = null;
        }

//    if ( win32.g_pdi ) {//TODO:not entirely sure what this is, yet!
//		win32.g_pdi->Release();
//		win32.g_pdi = NULL;
//	}
    }

    /*
     ===========
     Sys_InitInput
     ===========
     */
    public static void Sys_InitInput() {

        common.Printf("\n------- Input Initialization -------\n");
//        IN_InitDirectInput();
        if (win32.in_mouse.GetBool()) {
            IN_InitDIMouse();
            // don't grab the mouse on initialization
            Sys_GrabMouseCursor(false);
        } else {
            common.Printf("Mouse control not active.\n");
        }
        IN_StartupKeyboard();
        common.Printf("------------------------------------\n");
        win32.in_mouse.ClearModified();
    }

    /*
     ===========
     Sys_InitScanTable
     ===========
     */
    public static void Sys_InitScanTable() {

        final idStr lang = new idStr(cvarSystem.GetCVarString("sys_lang"));
        if (lang.Length() == 0) {
            lang.oSet("english");
        }
        if (lang.Icmp("english") == 0) {
            keyScanTable = s_scantokey;
            // the only reason that english right alt binds as K_ALT is so that 
            // users who were using right-alt before the patch don't suddenly find
            // that only left-alt is working.
            rightAltKey = K_ALT;
        } else if (lang.Icmp("spanish") == 0) {
            keyScanTable = s_scantokey_spanish;
            rightAltKey = K_RIGHT_ALT;
        } else if (lang.Icmp("french") == 0) {
            keyScanTable = s_scantokey_french;
            rightAltKey = K_RIGHT_ALT;
        } else if (lang.Icmp("german") == 0) {
            keyScanTable = s_scantokey_german;
            rightAltKey = K_RIGHT_ALT;
        } else if (lang.Icmp("italian") == 0) {
            keyScanTable = s_scantokey_italian;
            rightAltKey = K_RIGHT_ALT;
        }
    }

    /*
     ==================
     Sys_GetScanTable
     ==================
     */
    public static char[] Sys_GetScanTable() {
	return keyScanTable;
    }

    /*
     ===============
     Sys_GetConsoleKey
     ===============
     */
    public static char Sys_GetConsoleKey(boolean shifted) {
        return keyScanTable[41 + (shifted ? 128 : 0)];
    }

    /*
     ==================
     IN_Frame

     Called every frame, even if not generating commands
     ==================
     */
    public static void IN_Frame() {
        throw new TODO_Exception();
//	bool	shouldGrab = true;
//
//	if ( !win32.in_mouse.GetBool() ) {
//		shouldGrab = false;
//	}
//	// if fullscreen, we always want the mouse
//	if ( !win32.cdsFullscreen ) {
//		if ( win32.mouseReleased ) {
//			shouldGrab = false;
//		}
//		if ( win32.movingWindow ) {
//			shouldGrab = false;
//		}
//		if ( !win32.activeApp ) {
//			shouldGrab = false;
//		}
//	}
//
//	if ( shouldGrab != win32.mouseGrabbed ) {
//		if ( win32.mouseGrabbed ) {
//			IN_DeactivateMouse();
//		} else {
//			IN_ActivateMouse();
//
//#if 0	// if we can't reacquire, try reinitializing
//			if ( !IN_InitDIMouse() ) {
//				win32.in_mouse.SetBool( false );
//				return;
//			}
//#endif
//		}
//	}
    }

    public static void Sys_GrabMouseCursor(boolean grabIt) {
//        if (Mouse.isGrabbed() == grabIt) {//otherwise resetMouse in setGrabbed will keep erasing our mouse data.
//            Mouse.setGrabbed(grabIt);
//        }
//#ifndef	ID_DEDICATED
//	win32.mouseReleased = !grabIt;
//	if ( !grabIt ) {
//		// release it right now
//		IN_Frame();
//	}
//#endif
    }

//=====================================================================================
//#if 1
// I tried doing the full-state get to address a keyboard problem on one system,
// but it didn't make any difference

    /*
     ====================
     Sys_PollKeyboardInputEvents
     ====================
     */
    @Deprecated
    public static int Sys_PollKeyboardInputEvents() {
//        return Keyboard.getNumKeyboardEvents();
        return -1;
    }

    /*
     ====================
     Sys_PollKeyboardInputEvents
     ====================
     */
	public static int Sys_ReturnKeyboardInputEvent(int[] ch, final int action, final int key, final int scancode, final int mods) {
		ch[0] = IN_DIMapKey(key, scancode, mods);
//        action[0] = Keyboard.getEventKeyState();//state = (polled_didod[ n ].dwData & 0x80) == 0x80;
		switch (ch[0]) {
			case K_PRINT_SCR:
				if (action == GLFW_RELEASE) {
					// don't queue printscreen keys.  Since windows doesn't send us key
					// down events for this, we handle queueing them with DirectInput
					break;
				}
			case K_CTRL:
			case K_ALT:
			case K_RIGHT_ALT:
				// for windows, add a keydown event for print screen here, since
				// windows doesn't send keydown events to the WndProc for this key.
				// ctrl and alt are handled here to get around windows sending ctrl and
				// alt messages when the right-alt is pressed on non-US 102 keyboards.
				Sys_QueEvent(GetTickCount(), SE_KEY, ch[0], action, 0, null);//TODO:enable this
				break;
			default:// nabbed from MainWndProc.
                if ((action == GLFW_RELEASE) && (ch[0] > 31) && (ch[0] != '~') && (ch[0] != '`') && (ch[0] < 128)) {
					Sys_QueEvent(System.currentTimeMillis(), SE_CHAR, ch[0], action, 0, null);
				} else {
					Sys_QueEvent(System.currentTimeMillis(), SE_KEY, ch[0], action, 0, null);
				}
		}
		return ch[0];
	}

    private static long GetTickCount() {
        return System.currentTimeMillis() - START_TIME;
    }
    private static final long START_TIME = System.currentTimeMillis();

    public static void Sys_EndKeyboardInputEvents() {
    }

    public static void Sys_QueMouseEvents(int dwElements) {
        throw new TODO_Exception();
//	int i, value;
//
//	for( i = 0; i < dwElements; i++ ) {
//		if ( polled_didod[i].dwOfs >= DIMOFS_BUTTON0 && polled_didod[i].dwOfs <= DIMOFS_BUTTON7 ) {
//			value = (polled_didod[i].dwData & 0x80) == 0x80;
//			Sys_QueEvent( polled_didod[i].dwTimeStamp, SE_KEY, K_MOUSE1 + ( polled_didod[i].dwOfs - DIMOFS_BUTTON0 ), value, 0, NULL );
//		} else {
//			switch (polled_didod[i].dwOfs) {
//			case DIMOFS_X:
//				value = polled_didod[i].dwData;
//				Sys_QueEvent( polled_didod[i].dwTimeStamp, SE_MOUSE, value, 0, 0, null );
//				break;
//			case DIMOFS_Y:
//				value = polled_didod[i].dwData;
//				Sys_QueEvent( polled_didod[i].dwTimeStamp, SE_MOUSE, 0, value, 0, null );
//				break;
//			case DIMOFS_Z:
//				value = ( (int) polled_didod[i].dwData ) / WHEEL_DELTA;
//				int key = value < 0 ? K_MWHEELDOWN : K_MWHEELUP;
//				value = abs( value );
//				while( value-- > 0 ) {
//					Sys_QueEvent( polled_didod[i].dwTimeStamp, SE_KEY, key, true, 0, null );
//					Sys_QueEvent( polled_didod[i].dwTimeStamp, SE_KEY, key, false, 0, null );
//				}
//				break;
//			}
//		}
//	}
    }

//=====================================================================================
    public static int Sys_PollMouseInputEvents() {
        throw new TODO_Exception();
//        DWORD				dwElements;
//	HRESULT				hr;
//
//	if ( !Mouse.isCreated() || !Mouse.isGrabbed() ) {
////	if ( !win32.g_pMouse || !win32.mouseGrabbed ) {
//		return 0;
//	}
//
//    dwElements = DINPUT_BUFFERSIZE;
//    hr = win32.g_pMouse.GetDeviceData( sizeof(DIDEVICEOBJECTDATA), polled_didod, &dwElements, 0 );
//
//    if( hr != DI_OK ) {
//        hr = win32.g_pMouse.Acquire();
//		// clear the garbage
//		if (!FAILED(hr)) {
//			win32.g_pMouse.GetDeviceData( sizeof(DIDEVICEOBJECTDATA), polled_didod, &dwElements, 0 );
//		}
//    }
//
//    if( FAILED(hr) ) {
//        return 0;
//	}
//
//	Sys_QueMouseEvents( dwElements );
//
//	return dwElements;
    }

    @Deprecated
    public static void Sys_ReturnMouseInputEvent(int[] action, int[] value) {

//        final long dwTimeStamp = Mouse.getEventNanoseconds();
//
//        while (Mouse.next()) {
//            final int x, y, w;
//            if ((x = Mouse.getDX()) != 0) {
//                value[0] = x;
//                action[0] = etoi(M_DELTAX);
//                Sys_QueEvent(dwTimeStamp, SE_MOUSE, value[0], 0, 0, null);
//            }
//            if ((y = Mouse.getDY()) != 0) {
//                value[0] = -y;//TODO:negative a la ogl?
//                action[0] = etoi(M_DELTAY);
//                Sys_QueEvent(dwTimeStamp, SE_MOUSE, 0, value[0], 0, null);
//            }
//            if ((w = Mouse.getDWheel()) != 0) {
//                // mouse wheel actions are impulses, without a specific up / down
//                int wheelValue = value[0] = w;//(int) polled_didod[n].dwData ) / WHEEL_DELTA;
//                final int key = value[0] < 0 ? K_MWHEELDOWN : K_MWHEELUP;
//                action[0] = etoi(M_DELTAZ);
//
//                while (wheelValue-- > 0) {
//                    Sys_QueEvent(dwTimeStamp, SE_KEY, key, btoi(true), 0, null);
//                    Sys_QueEvent(dwTimeStamp, SE_KEY, key, btoi(false), 0, null);
//                }
//            }
//            if (Mouse.getEventButtonState()) {//TODO:find out what Mouse.next() does exactly.
//                final int diaction = Mouse.getEventButton();
//                value[0] = Mouse.isButtonDown(diaction) ? 0x80 : 0;// (polled_didod[n].dwData & 0x80) == 0x80;
//                action[0] = etoi(M_ACTION1) + diaction;//- DIMOFS_BUTTON0 );
//                Sys_QueEvent(dwTimeStamp, SE_KEY, K_MOUSE1 + diaction, value[0], 0, null);
//                B1 = true;
//            } else if (B1) {
//                Sys_QueEvent(dwTimeStamp, SE_KEY, K_MOUSE1, value[0] = 0, 0, null);
//                B1 = false;
//            }
//        }
    }
    private static boolean B1 = false;

    public static void Sys_EndMouseInputEvents() {
    }

    public static char Sys_MapCharForKey(int key) {
        return (char) (key & 0xFF);
    }

}
