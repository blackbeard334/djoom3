package neo.sys;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import neo.framework.CVarSystem.idCVar;
import static neo.sys.sys_public.MAX_CRITICAL_SECTIONS;

/**
 *
 */
public abstract class win_local {

    /*
     ===========================================================================

     Doom 3 GPL Source Code
     Copyright (C) 1999-2011 id Software LLC, a ZeniMax Media company. 

     This file is part of the Doom 3 GPL Source Code (?Doom 3 Source Code?).  

     Doom 3 Source Code is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     Doom 3 Source Code is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     You should have received a copy of the GNU General Public License
     along with Doom 3 Source Code.  If not, see <http://www.gnu.org/licenses/>.

     In addition, the Doom 3 Source Code is also subject to certain additional terms. You should have received a copy of these additional terms immediately following the terms and conditions of the GNU General Public License which accompanied the Doom 3 Source Code.  If not, please request a copy in writing from id Software at the address below.

     If you have questions concerning this license or the applicable additional terms, you may contact in writing id Software LLC, c/o ZeniMax Media Inc., Suite 120, Rockville, Maryland 20850 USA.

     ===========================================================================
     */
//
//
//// WGL_ARB_extensions_string
//public	PFNWGLGETEXTENSIONSSTRINGARBPROC wglGetExtensionsStringARB;
//
//// WGL_EXT_swap_interval
//public	PFNWGLSWAPINTERVALEXTPROC wglSwapIntervalEXT;
//
//// WGL_ARB_pixel_format
//public	PFNWGLGETPIXELFORMATATTRIBIVARBPROC wglGetPixelFormatAttribivARB;
//public	PFNWGLGETPIXELFORMATATTRIBFVARBPROC wglGetPixelFormatAttribfvARB;
//public	PFNWGLCHOOSEPIXELFORMATARBPROC wglChoosePixelFormatARB;
//
//// WGL_ARB_pbuffer
//public	PFNWGLCREATEPBUFFERARBPROC	wglCreatePbufferARB;
//public	PFNWGLGETPBUFFERDCARBPROC	wglGetPbufferDCARB;
//public	PFNWGLRELEASEPBUFFERDCARBPROC	wglReleasePbufferDCARB;
//public	PFNWGLDESTROYPBUFFERARBPROC	wglDestroyPbufferARB;
//public	PFNWGLQUERYPBUFFERARBPROC	wglQueryPbufferARB;
//
//// WGL_ARB_render_texture 
//public	PFNWGLBINDTEXIMAGEARBPROC		wglBindTexImageARB;
//public	PFNWGLRELEASETEXIMAGEARBPROC	wglReleaseTexImageARB;
//public	PFNWGLSETPBUFFERATTRIBARBPROC	wglSetPbufferAttribARB;
//
//
//static final int	MAX_OSPATH=			256;
//
////#define	WINDOW_STYLE	(WS_OVERLAPPED|WS_BORDER|WS_CAPTION|WS_VISIBLE | WS_THICKFRAME)
//
//void	Sys_QueEvent( int time, sysEventType_t type, int value, int value2, int ptrLength, void *ptr );
//
//void	Sys_CreateConsole( void );
//void	Sys_DestroyConsole( void );
//
//char	*Sys_ConsoleInput (void);
//char	*Sys_GetCurrentUser( void );
//
//void	Win_SetErrorText( const char *text );
//
//cpuid_t	Sys_GetCPUId( void );
//
//int		MapKey (int key);
//
//
//// Input subsystem
//
//void	IN_Init (void);
//void	IN_Shutdown (void);
//// add additional non keyboard / non mouse movement on top of the keyboard move cmd
//
//void	IN_DeactivateMouseIfWindowed( void );
//void	IN_DeactivateMouse( void );
//void	IN_ActivateMouse( void );
//
//void	IN_Frame( void );
//
//int		IN_DIMapKey( int key );
//
//void	DisableTaskKeys( BOOL bDisable, BOOL bBeep, BOOL bTaskMgr );
//
//
//// window procedure
//LONG WINAPI MainWndProc( HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
//
//void Conbuf_AppendText( const char *msg );
    static class Win32Vars_t {
//	HWND			hWnd;
//	HINSTANCE		hInstance;
//

        boolean activeApp;		// changed with WM_ACTIVATE messages
        boolean mouseReleased;		// when the game has the console down or is doing a long operation
        boolean movingWindow;		// inhibit mouse grab when dragging the window
        boolean mouseGrabbed;		// current state of grab and hide
//
//	OSVERSIONINFOEX	osversion;
//
        int/*cpuid_t*/ cpuid;
//
        // when we get a windows message, we store the time off so keyboard processing
        // can know the exact time of an event (not really needed now that we use async direct input)
        int sysMsgTime;
//
        boolean windowClassRegistered;
//
//	WNDPROC			wndproc;
//
//	HDC				hDC;							// handle to device context
//	HGLRC			hGLRC;						// handle to GL rendering context
//	PIXELFORMATDESCRIPTOR pfd;		
        int pixelformat;
//
//	HINSTANCE		hinstOpenGL;	// HINSTANCE for the OpenGL library
//
        int desktopBitsPixel;
        int desktopWidth, desktopHeight;
        //
        boolean cdsFullscreen;
        //
//	FILE			*log_fp;
//
//	unsigned short	oldHardwareGamma[3][256];
        // desktop gamma is saved here for restoration at exit
//
        static final idCVar sys_arch                   = new idCVar("sys_arch", "", CVAR_SYSTEM | CVAR_INIT, "");
        static final idCVar sys_cpustring              = new idCVar("sys_cpustring", "detect", CVAR_SYSTEM | CVAR_INIT, "");
        static final idCVar in_mouse                   = new idCVar("in_mouse", "1", CVAR_SYSTEM | CVAR_BOOL, "enable mouse input");
        static final idCVar win_allowAltTab            = new idCVar("win_allowAltTab", "0", CVAR_SYSTEM | CVAR_BOOL, "allow Alt-Tab when fullscreen");
        static final idCVar win_notaskkeys             = new idCVar("win_notaskkeys", "0", CVAR_SYSTEM | CVAR_INTEGER, "disable windows task keys");
        static final idCVar win_username               = new idCVar("win_username", "", CVAR_SYSTEM | CVAR_INIT, "windows user name");
        static final idCVar win_xpos                   = new idCVar("win_xpos", "3", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_INTEGER, "horizontal position of window");            // archived X coordinate of window position
        static final idCVar win_ypos                   = new idCVar("win_ypos", "22", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_INTEGER, "vertical position of window");            // archived Y coordinate of window position
        static final idCVar win_outputDebugString      = new idCVar("win_outputDebugString", "1", CVAR_SYSTEM | CVAR_BOOL, "");
        static final idCVar win_outputEditString       = new idCVar("win_outputEditString", "1", CVAR_SYSTEM | CVAR_BOOL, "");
        static final idCVar win_viewlog                = new idCVar("win_viewlog", "0", CVAR_SYSTEM | CVAR_INTEGER, "");
        static final idCVar win_timerUpdate            = new idCVar("win_timerUpdate", "0", CVAR_SYSTEM | CVAR_BOOL, "allows the game to be updated while dragging the window");
        static final idCVar win_allowMultipleInstances = new idCVar("win_allowMultipleInstances", "0", CVAR_SYSTEM | CVAR_BOOL, "allow multiple instances running concurrently");
        //
        Lock/*CRITICAL_SECTION*/[] criticalSections = new ReentrantLock[MAX_CRITICAL_SECTIONS];
        //	HANDLE			backgroundDownloadSemaphore;
//
//	HINSTANCE		hInstDI;			// direct input
//
//	LPDIRECTINPUT8			g_pdi;
        @Deprecated
        MouseListener/*LPDIRECTINPUTDEVICE8*/ g_pMouse;
        @Deprecated
        KeyListener/*LPDIRECTINPUTDEVICE8*/   g_pKeyboard;
        //
//	HANDLE			renderCommandsEvent;
//	HANDLE			renderCompletedEvent;
//	HANDLE			renderActiveEvent;
        Thread/*HANDLE*/ renderThreadHandle;
        //	unsigned long	renderThreadId;
//	void			(*glimpRenderThread)( void );
//	void			*smpData;
        int wglErrors;
        // SMP acceleration vars
    }

    ;

    public static Win32Vars_t win32 = new Win32Vars_t();

}
