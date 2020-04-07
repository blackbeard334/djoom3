package neo.opengl;

import static neo.TempDump.btoi;
import static neo.framework.KeyInput.K_ALT;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DEL;
import static neo.framework.KeyInput.K_DOWNARROW;
import static neo.framework.KeyInput.K_END;
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
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MWHEELDOWN;
import static neo.framework.KeyInput.K_MWHEELUP;
import static neo.framework.KeyInput.K_PAUSE;
import static neo.framework.KeyInput.K_PGDN;
import static neo.framework.KeyInput.K_PGUP;
import static neo.framework.KeyInput.K_PRINT_SCR;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.framework.KeyInput.K_RIGHT_ALT;
import static neo.framework.KeyInput.K_RWIN;
import static neo.framework.KeyInput.K_UPARROW;
import static neo.framework.UsercmdGen.usercmdGen;
import static neo.idlib.Lib.idLib.common;
import static neo.opengl.QGL.qcreateCapabilities;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_MOUSE;
import static neo.sys.win_input.Sys_EndKeyboardInputEvents;
import static neo.sys.win_input.Sys_EndMouseInputEvents;
import static neo.sys.win_input.Sys_ReturnKeyboardInputEvent;
import static neo.sys.win_main.Sys_QueEvent;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;

import neo.TempDump.NeoFixStrings;
import neo.framework.UsercmdGen.idUsercmdGenLocal.idUsercmdGenLocalData;
import neo.sys.win_glimp.glimpParms_t;

public class QUser {
	static long window;
	static Long monitor;
	static GLFWErrorCallback errorCallback;

	public static void shutdown() {
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	public static void endFrame() {
		glfwSwapBuffers(window);
	}

	public static void update() {
		glfwPollEvents();
	}

	public static boolean setFullScreen(glimpParms_t parms) {
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
		//boolean matched;
//
		// first make sure the user is not trying to select a mode that his card/monitor
		// can't handle
		//matched = false;
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
				common.Printf(e.getMessage() + "\n");
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
		DisplayMode[] displayModes = modes; // Display.getAvailableDisplayModes();
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
			//matched = true;
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

		// glfwInit();
		// glfwSetErrorCallback(errorCallback =
		// GLFWErrorCallback.createPrint(System.err));

		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
//        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
//        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//        window = GLFW.glfwCreateWindow(parms.width, parms.height, NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH, glfwGetPrimaryMonitor(), 0);//HACKME::0 change this back to setDisplayModeAndFullscreen.
		if (!parms.fullScreen) {
			window = GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH,
					NULL, NULL);
		} else {
			window = GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), NeoFixStrings.BLAAAAAAAAAAAAAAAAAARRRGGGGHH,
					monitor, NULL);
		}
		final GLFWVidMode currentMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (currentMode.width() / 2) - (parms.width / 2),
				(currentMode.height() / 2) - (parms.height / 2));
		if (window != 0) {
			glfwMakeContextCurrent(window);
			qcreateCapabilities();
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

	public static int mapKey(final int key) {
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
			return K_ALT;
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

	public static int processKeyboardInputEvents(final int ch, final int action, final long GetTickCount) {
		switch (ch) {
		case K_PRINT_SCR:
			if (action == GLFW_RELEASE) {
				// don't queue printscreen keys. Since windows doesn't send us key
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
			Sys_QueEvent(GetTickCount, SE_KEY, ch, action, 0, null);// TODO:enable this
			break;
		default:// nabbed from MainWndProc.
			if ((action == GLFW_RELEASE) && (ch > 31) && (ch != '~') && (ch != '`') && (ch < 128)) {
				Sys_QueEvent(System.currentTimeMillis(), SE_CHAR, ch, action, 0, null);
			} else {
				Sys_QueEvent(System.currentTimeMillis(), SE_KEY, ch, action, 0, null);
			}
		}
		return ch;
	}

	public static int getShiftedScancode(final int key, final int scancode, final int mods) {
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
		return (key == GLFW.GLFW_KEY_APOSTROPHE) || (key == GLFW.GLFW_KEY_COMMA) || (key == GLFW.GLFW_KEY_MINUS)
				|| (key == GLFW.GLFW_KEY_PERIOD) || (key == GLFW.GLFW_KEY_SLASH) || (key == GLFW.GLFW_KEY_0)
				|| (key == GLFW.GLFW_KEY_1) || (key == GLFW.GLFW_KEY_2) || (key == GLFW.GLFW_KEY_3)
				|| (key == GLFW.GLFW_KEY_4) || (key == GLFW.GLFW_KEY_5) || (key == GLFW.GLFW_KEY_6)
				|| (key == GLFW.GLFW_KEY_7) || (key == GLFW.GLFW_KEY_8) || (key == GLFW.GLFW_KEY_9)
				|| (key == GLFW.GLFW_KEY_SEMICOLON) || (key == GLFW.GLFW_KEY_EQUAL) || isShiftableLetter(key)
				|| (key == GLFW.GLFW_KEY_LEFT_BRACKET) || (key == GLFW.GLFW_KEY_BACKSLASH)
				|| (key == GLFW.GLFW_KEY_RIGHT_BRACKET) || (key == GLFW.GLFW_KEY_GRAVE_ACCENT)
				|| (key == GLFW.GLFW_KEY_WORLD_1) || (key == GLFW.GLFW_KEY_WORLD_2);
	}

	private static boolean isShiftableLetter(final int key) {
		return (key >= GLFW.GLFW_KEY_A) && (key <= GLFW.GLFW_KEY_Z);
	}

	public static class MouseCursorCallback extends GLFWCursorPosCallback {
    	private idUsercmdGenLocalData me;
    	
        public MouseCursorCallback(idUsercmdGenLocalData me) {
			super();
			this.me = me;
		}

		private double prevX, prevY;

        @Override
        public void invoke(long window, double xpos, double ypos) {
            final long dwTimeStamp = System.nanoTime();
            final double dx = xpos - this.prevX;
            final double dy = ypos - this.prevY;

            if ((dx != 0) || (dy != 0)) {
                me.mouseDx += dx;
                me.continuousMouseX += dx;
                this.prevX = xpos;

                me.mouseDy += dy;
                me.continuousMouseY += dy;
                this.prevY = ypos;
                Sys_QueEvent(dwTimeStamp, SE_MOUSE, (int) dx, (int) dy, 0, null);
            }

            Sys_EndMouseInputEvents();

        }
    }

	public static class MouseScrollCallback extends GLFWScrollCallback {
    	private idUsercmdGenLocalData me;
    	
        public MouseScrollCallback(idUsercmdGenLocalData me) {
			super();
			this.me = me;
		}

        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            final long dwTimeStamp = System.nanoTime();

            // mouse wheel actions are impulses, without a specific up / down
            int wheelValue = (int) yoffset;//(int) polled_didod[n].dwData ) / WHEEL_DELTA;
            final int key = yoffset < 0 ? K_MWHEELDOWN : K_MWHEELUP;

            while (wheelValue-- > 0) {
            	me.Key(key, true);
            	me.Key(key, false);
                me.mouseButton = key;
                me.mouseDown = true;
                Sys_QueEvent(dwTimeStamp, SE_KEY, key, btoi(true), 0, null);
                Sys_QueEvent(dwTimeStamp, SE_KEY, key, btoi(false), 0, null);
            }
        }
    }

	public static class MouseButtonCallback extends GLFWMouseButtonCallback {
    	private idUsercmdGenLocalData me;
    	
        public MouseButtonCallback(idUsercmdGenLocalData me) {
			super();
			this.me = me;
		}

        @Override
        public void invoke(long window, int button, int action, int mods) {
            final long dwTimeStamp = System.nanoTime();
            //
            // Study each of the buffer elements and process them.
            //

            final int diaction = button;
            if (diaction != -1) {
                final int buton = action != GLFW_RELEASE ? 0x80 : 0;// (polled_didod[n].dwData & 0x80) == 0x80;
                me.mouseButton = K_MOUSE1 + diaction;
                me.mouseDown = (buton != 0);
                me.Key(me.mouseButton, me.mouseDown);
                Sys_QueEvent(dwTimeStamp, SE_KEY, me.mouseButton, buton, 0, null);
            }

            Sys_EndMouseInputEvents();
        }
    }

	public static class KeyboardCallback extends GLFWKeyCallback {
    	private idUsercmdGenLocalData me;
    	
        public KeyboardCallback(idUsercmdGenLocalData me) {
			super();
			this.me = me;
		}

        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            final int[] ch = {0};
            //                        //-+
            // Study each of the buffer elements and process them.
            //
            if (Sys_ReturnKeyboardInputEvent(ch, action, key, scancode, mods) != 0) {
                me.Key(ch[0], action != GLFW_RELEASE);
            }

            Sys_EndKeyboardInputEvents();
        }
    }

}
