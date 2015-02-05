package neo.framework;

/**
 *
 */
public class Licensee {

    /*
     ===============================================================================

     Definitions for information that is related to a licensee's game name and location.

     ===============================================================================
     */
    public static final String GAME_NAME                    = "DOOM 3";        // appears on window titles and errors
    //
    public static final String ENGINE_VERSION               = "DOOM 1.3.1";    // printed in console
    //
    // paths
    public static final String CD_BASEDIR                   = "Doom";
    //#ifdef ID_DEMO_BUILD
    //	public static  final String BASE_GAMEDIR		=			"demo";
    //#else
    public static final String BASE_GAMEDIR                 = "base";
    //#endif
    // filenames
    public static final String CD_EXE                       = "doom.exe";
    public static final String CONFIG_FILE                  = "DoomConfig.cfg";
    //
    // base folder where the source code lives
    public static final String SOURCE_CODE_BASE_FOLDER      = "neo";
    //
    //
    // default idnet host address
    //#ifndef IDNET_HOST
    public static final String IDNET_HOST                   = "idnet.ua-corp.com";
    //#endif
    //
    // default idnet master port
    //#ifndef IDNET_MASTER_PORT
    public static final int    IDNET_MASTER_PORT            = 27650;
    //#endif
    // default network server port
    //#ifndef PORT_SERVER
    public static final int    PORT_SERVER                  = 27666;
    //#endif
    //
    // broadcast scan this many ports after PORT_SERVER so a single machine can run multiple servers
    public static final int    NUM_SERVER_PORTS             = 4;
    //
    // see ASYNC_PROTOCOL_VERSION
    // use a different major for each game
    public static final int    ASYNC_PROTOCOL_MAJOR         = 1;
    //
    // Savegame Version
    // Update when you can no longer maintain compatibility with previous savegames
    // NOTE: a seperate core savegame version and game savegame version could be useful
    // 16: Doom v1.1
    // 17: Doom v1.2 / D3XP. Can still read old v16 with defaults for new data
    public static final int    SAVEGAME_VERSION             = 17;
    //
    // <= Doom v1.1: 1. no DS_VERSION token ( default )
    // Doom v1.2: 2
    public static final int    RENDERDEMO_VERSION           = 2;
    //
    // editor info
    public static final String EDITOR_DEFAULT_PROJECT       = "doom.qe4";
    public static final String EDITOR_REGISTRY_KEY          = "DOOMRadiant";
    public static final String EDITOR_WINDOWTEXT            = "DOOMEdit";
    //
    // win32 info
    public static final String WIN32_CONSOLE_CLASS          = "DOOM 3 WinConsole";
    public static final String WIN32_WINDOW_CLASS_NAME      = "DOOM3";
    public static final String WIN32_FAKE_WINDOW_CLASS_NAME = "DOOM3_WGL_FAKE";
    //
    // Linux info
    //#ifdef ID_DEMO_BUILD
    //	public static  final String LINUX_DEFAULT_PATH		=	"/usr/local/games/doom3-demo";
    //#else
    public static final String LINUX_DEFAULT_PATH           = "/usr/local/games/doom3";
    //#endif
    // CD Key file info
    // goes into BASE_GAMEDIR whatever the fs_game is set to
    // two distinct files for easier win32 installer job
    public static final String CDKEY_FILE                   = "doomkey";
    public static final String XPKEY_FILE                   = "xpkey";
    public static final String CDKEY_TEXT                   = "\n// Do not give this file to ANYONE.\n"
            + "// id Software or Zenimax will NEVER ask you to send this file to them.\n";
    //
    public static final String CONFIG_SPEC                  = "config.spec";
}
