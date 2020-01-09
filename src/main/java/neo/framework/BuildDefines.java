package neo.framework;

import java.util.Objects;

/**
 *
 */
public class BuildDefines {

    public static final boolean _DEBUG    = false;
    //
    public static final boolean _WIN32    = System.getProperty("os.name").startsWith("Windows");
    public static final boolean WIN32     = _WIN32;
    public static final boolean MACOS_X   = System.getProperty("os.name").equals("Mac OS X");
    public static final boolean __ppc__   = MACOS_X;//TODO:can macosx run on non ppc?
    public static final boolean __linux__ = System.getProperty("os.name").equals("Linux");

    /*
     ===============================================================================

     Preprocessor settings for compiling different versions.

     ===============================================================================
     */
    // memory debugging
    //#define ID_REDIRECT_NEWDELETE
    //#define ID_DEBUG_MEMORY
    //#define ID_DEBUG_UNINITIALIZED_MEMORY
    // if enabled, the console won't toggle upon ~, unless you start the binary with +set com_allowConsole 1
    // Ctrl+Alt+~ will always toggle the console no matter what
    public static final boolean ID_CONSOLE_LOCK;

    static {
        if (_WIN32 || MACOS_X) {
            if (_DEBUG) {
                ID_CONSOLE_LOCK = false;
            } else {

                ID_CONSOLE_LOCK = true;
            }
        } else {
            ID_CONSOLE_LOCK = false;
        }
    }

    // useful for network debugging, turns off 'LAN' checks, all IPs are classified 'internet'
    public static final boolean ID_NOLANADDRESS    = false;

    // let .dds be loaded from FS without altering pure state. only for developement.
    public static final boolean ID_PURE_ALLOWDDS   = false;

    // build an exe with no CVAR_CHEAT controls
    public static final boolean ID_ALLOW_CHEATS    = false;

    public static final boolean ID_ENABLE_CURL     = true;

    // fake a pure client. useful to connect an all-debug client to a server
    public static final boolean ID_FAKE_PURE       = false;

    // verify checksums in clientinfo traffic
    // NOTE: this makes the network protocol incompatible
    public static final boolean ID_CLIENTINFO_TAGS = false;

    // for win32 this is defined in preprocessor settings so that MFC can be
    // compiled out.
    public static final boolean ID_DEDICATED       = false;

    // if this is defined, the executable positively won't work with any paks other
    // than the demo pak, even if productid is present.
    public static final boolean ID_DEMO_BUILD      = Objects.equals(System.getProperty("ID_DEMO_BUILD"), Boolean.TRUE.toString());

    // don't define ID_ALLOW_TOOLS when we don't want tool code in the executable.
    public static final boolean ID_ALLOW_TOOLS;

    static {
        if (_WIN32 && !ID_DEDICATED && !ID_DEMO_BUILD) {
            ID_ALLOW_TOOLS = true;
        } else {
            ID_ALLOW_TOOLS = false;
        }
    }

    // don't do backtraces in release builds.
    // atm, we have no useful way to reconstruct the trace, so let's leave it off
    public static final boolean ID_BT_STUB;

    static {
        if (__linux__) {
            if (_DEBUG) {
                ID_BT_STUB = true;

            } else {
                ID_BT_STUB = false;
            }
        } else {
            ID_BT_STUB = true;
        }
    }

    public static final boolean ID_ENFORCE_KEY;

    static {
        if (!ID_DEDICATED && !ID_DEMO_BUILD) {
            ID_ENFORCE_KEY = true;
        } else {
            ID_ENFORCE_KEY = false;
        }
    }

    public static final boolean ID_OPENAL = true;

//    static {
//        if ((_WIN32 || MACOS_X) && !ID_DEDICATED) {
//            ID_OPENAL = true;
//        } else {
//            ID_OPENAL = false;
//        }
//    }

    public static final boolean ID_ALLOW_D3XP = true;

}
