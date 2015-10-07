
package neo.sys;

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
import static neo.sys.win_input.Sys_GetScanTable;

/**
 *
 */
public class win_wndproc {
    
    
//==========================================================================

// Keep this in sync with the one in win_input.cpp
// This one is used in the menu, the other one is used in game

    static final int[] s_scantokey/*[128]*/ = {
//  0            1       2          3          4       5            6         7
//  8            9       A          B          C       D            E         F
    0,          27,    '1',       '2',        '3',    '4',         '5',      '6', 
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
    0,          0,      0,         0,          0,      0,           0,        0      // 7
}; 

    static int[] s_scantoshift/*[128]*/ = {
//  0            1       2          3          4       5            6         7
//  8            9       A          B          C       D            E         F
    0,           27,    '!',       '@',        '#',    '$',         '%',      '^', 
    '&',        '*',    '(',       ')',        '_',    '+',         K_BACKSPACE, 9,  // 0 
    'Q',        'W',    'E',       'R',        'T',    'Y',         'U',      'I', 
    'O',        'P',    '{',       '}',        K_ENTER,K_CTRL,      'A',      'S',   // 1 
    'D',        'F',    'G',       'H',        'J',    'K',         'L',      ':', 
    '|' ,       '~',    K_SHIFT,   '\\',       'Z',    'X',         'C',      'V',   // 2 
    'B',        'N',    'M',       '<',        '>',    '?',         K_SHIFT,  K_KP_STAR, 
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


    /*
     =======
     MapKey

     Map from windows to Doom keynums
     =======
     */@Deprecated
    static int MapKey(int key) {
        int result;
        int modified;
        boolean is_extended;

        modified = (key >> 16) & 255;

        if (modified > 127) {
            return 0;
        }

        if ((key & (1 << 24)) != 0) {
            is_extended = true;
        } else {
            is_extended = false;
        }

        //Check for certain extended character codes.
        //The specific case we are testing is the numpad / is not being translated
        //properly for localized builds.
        if (is_extended) {
            switch (modified) {
                case 0x35: //Numpad /
                    return K_KP_SLASH;
            }
        }

        final char[] scanToKey = Sys_GetScanTable();
        result = scanToKey[modified];

        // common->Printf( "Key: 0x%08x Modified: 0x%02x Extended: %s Result: 0x%02x\n", key, modified, (is_extended?"Y":"N"), result);
        if (is_extended) {
            switch (result) {
                case K_PAUSE:
                    return K_KP_NUMLOCK;
                case 0x0D:
                    return K_KP_ENTER;
                case 0x2F:
                    return K_KP_SLASH;
                case 0xAF:
                    return K_KP_PLUS;
                case K_KP_STAR:
                    return K_PRINT_SCR;
                case K_ALT:
                    return K_RIGHT_ALT;
            }
        } else {
            switch (result) {
                case K_HOME:
                    return K_KP_HOME;
                case K_UPARROW:
                    return K_KP_UPARROW;
                case K_PGUP:
                    return K_KP_PGUP;
                case K_LEFTARROW:
                    return K_KP_LEFTARROW;
                case K_RIGHTARROW:
                    return K_KP_RIGHTARROW;
                case K_END:
                    return K_KP_END;
                case K_DOWNARROW:
                    return K_KP_DOWNARROW;
                case K_PGDN:
                    return K_KP_PGDN;
                case K_INS:
                    return K_KP_INS;
                case K_DEL:
                    return K_KP_DEL;
            }
        }

        return result;
    }
}
