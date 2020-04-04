package neo.sys;

import static neo.Renderer.RenderSystem_init.r_logFile;
import static neo.Renderer.RenderSystem_init.r_swapInterval;
import static neo.Renderer.tr_local.tr;
import static neo.TempDump.NOT;
import static neo.TempDump.atobb;
import static neo.TempDump.fopenOptions;
import static neo.framework.FileSystem_h.MAX_OSPATH;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.UsercmdGen.usercmdGen;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Lib.idLib.cvarSystem;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetVideoModes;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.DisplayMode;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import neo.TempDump.NeoFixStrings;
import neo.TempDump.TODO_Exception;
import neo.idlib.Text.Str.idStr;

//import org.lwjgl.LWJGLException;
//import org.lwjgl.opengl.Display;
//import org.lwjgl.opengl.DisplayMode;

/**
 *
 */
public class win_glimp {

    static long window;
    static Long monitor;
    static GLFWErrorCallback errorCallback;

    /*
     ====================================================================

     IMPLEMENTATION SPECIFIC FUNCTIONS

     ====================================================================
     */
    public static class glimpParms_t {

        public int     width;
        public int     height;
        public boolean fullScreen;
        public boolean stereo;
        public int     displayHz;
        public int     multiSamples;
    }

    /*
     ===================
     GLW_SetFullScreen
     ===================
     */
    static boolean GLW_SetFullScreen(glimpParms_t parms) {
////#if 0
////	// for some reason, bounds checker claims that windows is
////	// writing past the bounds of dm in the get display frequency call
////	union {
////		DEVMODE dm;
////		byte	filler[1024];
////	} hack;
////#endif
        DisplayMode dm = null;
////	int		cdsRet;
//
////	DisplayMode		devmode;
////	int			modeNum;
        boolean matched;
//
        // first make sure the user is not trying to select a mode that his card/monitor can't handle
        matched = false;
        DisplayMode[] modes = null;
        {
    		if (errorCallback == null) {
    			// Setup an error callback. The default implementation
    			// will print the error message in System.err.
    			errorCallback = GLFWErrorCallback.createPrint(System.err).set();

    			// Initialize GLFW. Most GLFW functions will not work before doing this.
    			if (!glfwInit()) {
    				throw new IllegalStateException("Unable to initialize GLFW");
    			}
    	        glfwSetErrorCallback(errorCallback);
    		}
    		try {
    			if (monitor == null) {
    				monitor = glfwGetPrimaryMonitor();
    			}
    			GLFWVidMode.Buffer videoModes = glfwGetVideoModes(monitor);
    			modes = new DisplayMode[videoModes.limit()];
    			for (int i = 0; i < modes.length; i++) {
    				GLFWVidMode videoMode = videoModes.get(i);
    				modes[i] = new DisplayMode(videoMode.width(), videoMode.height(), videoMode.refreshRate(),
    						videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
    			}
    		} catch (Throwable e) {
    			common.Printf(e.getMessage()+"\n");
    			modes = new DisplayMode[0];
    		}
    		LinkedList<DisplayMode> l = new LinkedList<>();
    		GLFWVidMode videoMode = glfwGetVideoMode(monitor);
    		DisplayMode oldDisplayMode = new DisplayMode(videoMode.width(), videoMode.height(), videoMode.refreshRate(),
    				videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
    		l.add(oldDisplayMode);

    		for (DisplayMode m : modes) {
    			if (m.getBitDepth() != oldDisplayMode.getBitDepth()) {
    				continue;
    			}
    			if (m.getRefreshRate() > oldDisplayMode.getRefreshRate()) {
    				continue;
    			}
    			if ((m.getHeight() < 240) || (m.getWidth() < 320)) {
    				continue;
    			}

    			int j = 0;
    			DisplayMode ml = null;
    			for (j = 0; j < l.size(); j++) {
    				ml = l.get(j);
    				if (ml.getWidth() > m.getWidth()) {
    					break;
    				}
    				if ((ml.getWidth() == m.getWidth()) && (ml.getHeight() >= m.getHeight())) {
    					break;
    				}
    			}
    			if (j == l.size()) {
    				l.addLast(m);
    			} else if ((ml.getWidth() > m.getWidth()) || (ml.getHeight() > m.getHeight())) {
    				l.add(j, m);
    			} else if (m.getRefreshRate() > ml.getRefreshRate()) {
    				l.remove(j);
    				l.add(j, m);
    			}
    		}
    		modes = new DisplayMode[l.size()];
    		l.toArray(modes);
    	}
        DisplayMode[] displayModes = modes; //Display.getAvailableDisplayModes();
        Arrays.sort(displayModes, Comparator.comparingInt(DisplayMode::getWidth));
		// Try to find a best match.
		DisplayMode best_match = null; // looking for request size/bpp followed by exact or highest freq
		int match_freq = -1;
		int match_bpp = -1;
		int colorDepth = 32;
		int frequency = parms.displayHz;
		for (DisplayMode mode : modes) {
			if (mode.getWidth() != parms.width) {
				continue;
			}
			if (mode.getHeight() != parms.height) {
				continue;
			}
			if (best_match == null) {
				best_match = mode;
				match_freq = mode.getRefreshRate();
				match_bpp = mode.getBitDepth();
			} else {
				final int cur_freq = mode.getRefreshRate();
				final int cur_bpp = mode.getBitDepth();
				if ((match_bpp != colorDepth) && // Previous is not a perfect match
						((cur_bpp == colorDepth) || // Current is perfect match
								(match_bpp < cur_bpp))) // or is better match

				{
					best_match = mode;
					match_freq = cur_freq;
					match_bpp = cur_bpp;
				} else if ((match_freq != frequency) && // Previous is not a perfect match
						((cur_freq == frequency) || // Current is perfect match
								(match_freq < cur_freq))) // or is better match
				{
					best_match = mode;
					match_freq = cur_freq;
					match_bpp = cur_bpp;
				}
			}
		}
		if (best_match != null) {
			matched = true;
			dm = best_match;
		}

//        for (DisplayMode devmode : displayModes) {
////		if ( !EnumDisplaySettings( NULL, modeNum, &devmode ) ) {
//            if (matched) {
//                // we got a resolution match, but not a frequency match
//                // so disable the frequency requirement
//                common.Printf("...^3%dhz is unsupported at %dx%d^0\n", parms.displayHz, parms.width, parms.height);
//                parms.displayHz = 0;
//                break;
//            }
////			common.Printf( "...^3%dx%d is unsupported in 32 bit^0\n", parms.width, parms.height );
////			return false;
////		}
//            if (devmode.getWidth() >= parms.width
//                    && devmode.getHeight() >= parms.height
//                    && devmode.getBitsPerPixel() == 32) {
//
//                matched = true;
//
//                if (parms.displayHz == 0 || devmode.getFrequency() == parms.displayHz) {
//                    dm = devmode;
//                    break;
//                }
//            }
//        }
//
////	memset( &dm, 0, sizeof( dm ) );
////	dm.dmSize = sizeof( dm );
////
////	dm.dmPelsWidth  = parms.width;
////	dm.dmPelsHeight = parms.height;
////	dm.dmBitsPerPel = 32;
////	dm.dmFields     = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL;
////
////	if ( parms.displayHz != 0 ) {
////		dm.dmDisplayFrequency = parms.displayHz;
////		dm.dmFields |= DM_DISPLAYFREQUENCY;
////	}
////
//        common.Printf("...calling CDS: ");
//	
        // try setting the exact mode requested, because some drivers don't report
        // the low res modes in EnumDisplaySettings, but still work
//        if (dm != null) {
//            Display.setDisplayModeAndFullscreen(dm);
//            Display.setDisplayModeAndFullscreen(dm);
//            Display.setDisplayMode(dm);
//            Display.setVSyncEnabled(true);
//            Display.setTitle(NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH);

        //glfwInit();
        //glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
//        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
//        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//        window = GLFW.glfwCreateWindow(parms.width, parms.height, NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH, glfwGetPrimaryMonitor(), 0);//HACKME::0 change this back to setDisplayModeAndFullscreen.
        if (!parms.fullScreen) {
            window = GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH, NULL, NULL);
        } else {
            window = GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH, monitor, NULL);
        }
        final GLFWVidMode currentMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (currentMode.width() / 2) - (parms.width / 2), (currentMode.height() / 2) - (parms.height / 2));
        if (window != 0) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
//                win32.cdsFullscreen = true;
            glfwShowWindow(window);
            glfwSetInputMode(window, GLFW.GLFW_LOCK_KEY_MODS, GLFW_TRUE);
            glfwSetKeyCallback(window, usercmdGen.keyboardCallback);
            glfwSetCursorPosCallback(window, usercmdGen.mouseCursorCallback);
            glfwSetScrollCallback(window, usercmdGen.mouseScrollCallback);
            glfwSetMouseButtonCallback(window, usercmdGen.mouseButtonCallback);
            common.Printf("ok\n");
            return true;
        }
//        }
//
//	//
//	// the exact mode failed, so scan EnumDisplaySettings for the next largest mode
//	//
//	common.Printf( "^3failed^0, " );
//	
//	PrintCDSError( cdsRet );
//
//	common.Printf( "...trying next higher resolution:" );
//	
//	// we could do a better matching job here...
//	for ( modeNum = 0 ; ; modeNum++ ) {
//		if ( !EnumDisplaySettings( null, modeNum, &devmode ) ) {
//			break;
//		}
//		if ( (int)devmode.dmPelsWidth >= parms.width
//			&& (int)devmode.dmPelsHeight >= parms.height
//			&& devmode.dmBitsPerPel == 32 ) {
//
//			if ( ( cdsRet = ChangeDisplaySettings( &devmode, CDS_FULLSCREEN ) ) == DISP_CHANGE_SUCCESSFUL ) {
//				common.Printf( "ok\n" );
//				win32.cdsFullscreen = true;
//
//				return true;
//			}
//			break;
//		}
//	}
//        common.Printf("\n...^3no high res mode found^0\n");
        return false;
    }

    /*
     ===================
     GLimp_Init

     This is the platform specific OpenGL initialization function.  It
     is responsible for loading OpenGL, initializing it,
     creating a window of the appropriate size, doing
     fullscreen manipulations, etc.  Its overall responsibility is
     to make sure that a functional OpenGL subsystem is operating
     when it returns to the ref.

     If there is any failure, the renderer will revert back to safe
     parameters and try again.
     ===================
     */
    public static boolean GLimp_Init(glimpParms_t parms) {
////	const char	*driverName;
////	HDC		hDC;
////
//        common.Printf("Initializing OpenGL subsystem\n");
//
//        // check our desktop attributes
////	hDC = GetDC( GetDesktopWindow() );
//        win32.desktopBitsPixel = Display.getDesktopDisplayMode().getBitsPerPixel();
//        win32.desktopWidth = Display.getDesktopDisplayMode().getWidth();
//        win32.desktopHeight = Display.getDesktopDisplayMode().getHeight();
////	ReleaseDC( GetDesktopWindow(), hDC );
//        // we can't run in a window unless it is 32 bpp
//        if (win32.desktopBitsPixel < 32 && !parms.fullScreen) {
//            common.Printf("^3Windowed mode requires 32 bit desktop depth^0\n");
//            return false;
//        }
//
//        // save the hardware gamma so it can be
//        // restored on exit
//        GLimp_SaveGamma();
//
////        // create our window classes if we haven't already
////        GLW_CreateWindowClasses();
////
////	// this will load the dll and set all our qgl* function pointers,
////	// but doesn't create a window
////
////	// r_glDriver is only intended for using instrumented OpenGL
////	// dlls.  Normal users should never have to use it, and it is
////	// not archived.
////	driverName = r_glDriver.GetString()[0] ? r_glDriver.GetString() : "opengl32";
////	if ( !QGL_Init( driverName ) ) {
////		common.Printf( "^3GLimp_Init() could not load r_glDriver \"%s\"^0\n", driverName );
////		return false;
////	}
////
////	// getting the wgl extensions involves creating a fake window to get a context,
////	// which is pretty disgusting, and seems to mess with the AGP VAR allocation
////	GLW_GetWGLExtensionsWithFakeWindow();
////
//        // try to change to fullscreen
////        if (parms.fullScreen) {//TODO:change this back.
//        try {
        if (!GLW_SetFullScreen(parms)) {
            GLimp_Shutdown();
            return false;
        }
//            //TODO: comment
//            Display.create();
////            Display.makeCurrent();
////            Display.create(new PixelFormat(), new ContextAttribs(4, 0));
//            Display.isCreated();
//            GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
////
////            GL11.glMatrixMode(GL11.GL_PROJECTION); // Select The Projection Matrix
////            GL11.glLoadIdentity(); // Reset The Projection Matrix
////            GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix
////            GL11.glLoadIdentity(); // Reset The Modelview Matrix
////
////            GL11.glShadeModel(GL11.GL_SMOOTH); // Enables Smooth Shading
////            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Black Background
////            GL11.glClearDepth(1.0f); // Depth Buffer Setup
////            GL11.glEnable(GL11.GL_DEPTH_TEST); // Enables Depth Testing
////            GL11.glDepthFunc(GL11.GL_LEQUAL); // The Type Of Depth Test To Do
////            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST); // Really Nice Perspective Calculations
//        } catch (LWJGLException ex) {
//            Logger.getLogger(win_glimp.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        }
//
////        // try to create a window with the correct pixel format
////        // and init the renderer context
//////        if (!GLW_CreateWindow(parms)) {
////        if (!glcreate) {
////            GLimp_Shutdown();
////            return false;
////        }
//        // check logging
//        GLimp_EnableLogging(itob(r_logFile.GetInteger()));

        return true;
    }

    // If the desired mode can't be set satisfactorily, false will be returned.
    // The renderer will then reset the glimpParms to "safe mode" of 640x480
    // fullscreen and try again.  If that also fails, the error will be fatal.
    public static boolean GLimp_SetScreenParms(glimpParms_t parms) {
//        final DisplayMode dm;
//        final boolean ret;
//
//        dm = new DisplayMode(parms.width, parms.height);
//
//        win32.cdsFullscreen = parms.fullScreen;
//
////        try {
////            if (parms.fullScreen) {
////                Display.setDisplayModeAndFullscreen(dm);
////                return Display.getDisplayMode().equals(dm) && Display.isFullscreen();
////            } else {
////                final int x = Display.getDesktopDisplayMode().getWidth() / 2;
////                final int y = Display.getDesktopDisplayMode().getHeight() / 2;
////                Display.setDisplayMode(dm);
////                Display.setLocation(x, y);
////                return Display.getDisplayMode().equals(dm);
////            }
////        } catch (LWJGLException e) {
////            e.printStackTrace();
////        }

        return false;

    }

    /*
     ===================
     GLimp_Shutdown

     This routine does all OS specific shutdown procedures for the OpenGL
     subsystem.
     ===================
     */ // will set up gl up with the new parms
    public static void GLimp_Shutdown() {
//        final String[] success = {"failed", "success"};
//        int retVal;
//
//        common.Printf("Shutting down OpenGL subsystem\n");
//
////
////        // set current context to NULL
////        if (qwglMakeCurrent) {
////            retVal = qwglMakeCurrent(null, null) != 0;
////            common.Printf("...wglMakeCurrent( NULL, NULL ): %s\n", success[retVal]);
////        }
////
////        // delete HGLRC
////        if (win32.hGLRC) {
////            retVal = qwglDeleteContext(win32.hGLRC) != 0;
////            common.Printf("...deleting GL context: %s\n", success[retVal]);
////            win32.hGLRC = NULL;
////        }
////
////        // release DC
////        if (win32.hDC) {
////            retVal = ReleaseDC(win32.hWnd, win32.hDC) != 0;
////            common.Printf("...releasing DC: %s\n", success[retVal]);
////            win32.hDC = NULL;
////        }
//        // destroy window
//        if (Display.isCreated()) {
////        if (win32.hWnd) {
//            common.Printf("...destroying window\n");
//            Display.destroy();
////            ShowWindow(win32.hWnd, SW_HIDE);
////            DestroyWindow(win32.hWnd);
////            win32.hWnd = NULL;
//        }
//        // close the thread so the handle doesn't dangle
//        if (win32.renderThreadHandle != null) {
//            common.Printf("...closing smp thread\n");
//            win32.renderThreadHandle.interrupt();
//            win32.renderThreadHandle = null;
//        }
//
////        // restore gamma
////        GLimp_RestoreGamma();//TODO:check if our java opengl requires restoring gamma.
////
////        // shutdown QGL subsystem
////        QGL_Shutdown();//not necessary.
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    // Destroys the rendering context, closes the window, resets the resolution,
    // and resets the gamma ramps.
    public static void GLimp_SwapBuffers() {
        if (r_swapInterval.IsModified()) {
            r_swapInterval.ClearModified();

//        if (wglSwapIntervalEXT) {
//            //
//            // wglSwapinterval is a windows-private extension,
//            // so we must check for it here instead of portably
//            //
//            wglSwapIntervalEXT(r_swapInterval.GetInteger());
//        }
        }
//            Display.swapBuffers();//qwglSwapBuffers(win32.hDC);
        glfwPollEvents();
        glfwSwapBuffers(window);

        //Sys_DebugPrintf( "*** SwapBuffers() ***\n" );
    }

    /*
     ========================
     GLimp_GetOldGammaRamp
     ========================
     */
    @Deprecated
    static void GLimp_SaveGamma() {//TODO:is this function needed?
//	HDC			hDC;
//	BOOL		success;
//
//	hDC = GetDC( GetDesktopWindow() );
//	success = GetDeviceGammaRamp( hDC, win32.oldHardwareGamma );
//	common->DPrintf( "...getting default gamma ramp: %s\n", success ? "success" : "failed" );
//	ReleaseDC( GetDesktopWindow(), hDC );
    }

    /*
     ========================
     GLimp_RestoreGamma
     ========================
     */
    @Deprecated
    static void GLimp_RestoreGamma() {//TODO:is this function needed?
//	HDC hDC;
//	BOOL success;
//
//	// if we never read in a reasonable looking
//	// table, don't write it out
//	if ( win32.oldHardwareGamma[0][255] == 0 ) {
//		return;
//	}
//
//	hDC = GetDC( GetDesktopWindow() );
//	success = SetDeviceGammaRamp( hDC, win32.oldHardwareGamma );
//	common->DPrintf ( "...restoring hardware gamma: %s\n", success ? "success" : "failed" );
//	ReleaseDC( GetDesktopWindow(), hDC );
    }

    // Calls the system specific swapbuffers routine, and may also perform
    // other system specific cvar checks that happen every frame.
    // This will not be called if 'r_drawBuffer GL_FRONT'
    public static void GLimp_SetGamma(float gamma, float brightness, float contrast) {
//        try {
//            //    public static void GLimp_SetGamma(short[] red/*[256]*/, short[] green/*[256]*/, short[] blue/*[256]*/) {
////        Gamma.setDisplayGamma(null, gamma, 0, 0);
//            Display.setDisplayConfiguration(gamma, 0, 0);//TODO:check if GL was started.
//        } catch (LWJGLException ex) {
//            Logger.getLogger(win_glimp.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    //    // Sets the hardware gamma ramps for gamma and brightness adjustment.
//    // These are now taken as 16 bit values, so we can take full advantage
//    // of dacs with >8 bits of precision
//    public static boolean GLimp_SpawnRenderThread(glimpRenderThread function) {
//        throw new TODO_Exception();
//    }
    // Returns false if the system only has a single processor
    public static Object GLimp_BackEndSleep() {
        throw new TODO_Exception();
    }

    public static void GLimp_FrontEndSleep() {
        throw new TODO_Exception();
    }

    public static void GLimp_WakeBackEnd(Object data) {
        throw new TODO_Exception();
    }

    // these functions implement the dual processor syncronization
    public static void GLimp_ActivateContext() {
        throw new TODO_Exception();
    }

    public static void GLimp_DeactivateContext() {
        throw new TODO_Exception();
    }

    private static boolean       isEnabled;
    private static int           initialFrames;
    private static StringBuilder ospath = new StringBuilder(MAX_OSPATH);

    // These are used for managing SMP handoffs of the OpenGL context
    // between threads, and as a performance tunining aid.  Setting
    // 'r_skipRenderContext 1' will call GLimp_DeactivateContext() before
    // the 3D rendering code, and GLimp_ActivateContext() afterwards.  On
    // most OpenGL implementations, this will result in all OpenGL calls
    // being immediate returns, which lets us guage how much time is
    // being spent inside OpenGL.
    public static void GLimp_EnableLogging(boolean enable) {//TODO:activate this function. EDIT:make sure it works.

        try {
            // return if we're already active
            if (isEnabled && enable) {
                // decrement log counter and stop if it has reached 0
                r_logFile.SetInteger(r_logFile.GetInteger() - 1);
                if (r_logFile.GetInteger() != 0) {
                    return;
                }
                common.Printf("closing logfile '%s' after %d frames.\n", ospath, initialFrames);
                enable = false;

                tr.logFile.close();
                tr.logFile = null;

            }

            // return if we're already disabled
            if (!enable && !isEnabled) {
                return;
            }

            isEnabled = enable;

            if (enable) {
                if (NOT(tr.logFile)) {
//			struct tm		*newtime;
//			ID_TIME_T			aclock;
                    String qpath = "";
                    int i;
                    final String path;

                    initialFrames = r_logFile.GetInteger();

                    // scan for an unused filename
                    for (i = 0; i < 9999; i++) {
                        qpath = String.format("renderlog_%d.txt", i);
                        if (fileSystem.ReadFile(qpath, null, null) == -1) {
                            break;        // use this name
                        }
                    }

                    path = fileSystem.RelativePathToOSPath(qpath, "fs_savepath");
                    idStr.Copynz(ospath, path);
                    tr.logFile = FileChannel.open(Paths.get(ospath.toString()), fopenOptions("wt"));

                    // write the time out to the top of the file
//			time( &aclock );
//			newtime = localtime( &aclock );
                    tr.logFile.write(atobb(String.format("// %s", new Date())));
                    tr.logFile.write(atobb(String.format("// %s\n\n", cvarSystem.GetCVarString("si_version"))));
                }
            }
        } catch (final IOException ex) {
            Logger.getLogger(win_glimp.class.getName()).log(Level.SEVERE, null, ex);
            common.Warning("---GLimp_EnableLogging---\n%s\n---", ex.getMessage());
        }
    }
}
