package neo.sys;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import static neo.TempDump.NOT;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.Common.common;
import neo.framework.EditField.idEditField;
import static neo.framework.Licensee.GAME_NAME;
import neo.idlib.Text.Str.idStr;
import static neo.sys.RC.doom_resource.IDI_ICON1;
import static neo.sys.win_local.win32;
import static neo.sys.win_main.Sys_Error;

/**
 *
 */
public class win_syscon {

    static final int COPY_ID      = 1;
    static final int QUIT_ID      = 2;
    static final int CLEAR_ID     = 3;

    static final int ERRORBOX_ID  = 10;
    static final int ERRORTEXT_ID = 11;

    static final int EDIT_ID      = 100;
    static final int INPUT_ID     = 101;

    static final int COMMAND_HISTORY = 64;

    private static final int CONSOLE_BUFFER_SIZE = 16384;

    private static class WinConData {//TODO:refactor names to reflect the types; e.g:hWnd -> jFrame or something.

        JFrame /*HWND*/ hWnd;
        StringBuilder buffer = new StringBuilder(0x7000);
        JTextArea textArea;
        JScrollPane/*HWND*/ hwndBuffer;
        //
        JButton/*HWND*/     hwndButtonClear;
        JButton/*HWND*/     hwndButtonCopy;
        JButton/*HWND*/     hwndButtonQuit;
        //
        JTextField/*HWND*/  hwndErrorBox;
        //	HWND		hwndErrorText;
//
//	HBITMAP		hbmLogo;
//	HBITMAP		hbmClearBitmap;
//
//	HBRUSH		hbrEditBackground;
//	HBRUSH		hbrErrorBackground;
//
        Font/*HFONT*/       hfBufferFont;
        Font/*HFONT*/       hfButtonFont;
        //
        JTextField /*HWND*/ hwndInputLine;
        //
        StringBuilder errorString = new StringBuilder(80);
        //
//	char		consoleText[512], returnedText[512];
        int windowWidth, windowHeight;
        //
//	WNDPROC		SysInputLineWndProc;
//
        idEditField[] historyEditLines = new idEditField[COMMAND_HISTORY];
        //
//	int			nextHistoryLine;// the last line in the history buffer, not masked
//	int			historyLine;	// the line being displayed from history buffer
//								// will be <= nextHistoryLine
//
        idEditField   consoleField     = new idEditField();
        //
        private static final WindowAdapter QUIT_ON_CLOSE = new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                common.Quit();
            }

        };

        void setQuitOnClose(boolean quitOnClose) {
            if (quitOnClose) {
                this.hWnd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                this.hWnd.addWindowListener(QUIT_ON_CLOSE);
            } else {
                this.hWnd.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                this.hWnd.removeWindowListener(QUIT_ON_CLOSE);
            }
        }
    };

    static WinConData s_wcd = new WinConData();

//    static LONG WINAPI    ConWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
//	char *cmdString;
//	static bool s_timePolarity;
//
//	switch (uMsg) {
//		case WM_ACTIVATE:
//			if ( LOWORD( wParam ) != WA_INACTIVE ) {
//				SetFocus( s_wcd.hwndInputLine );
//			}
//		break;
//		case WM_CLOSE:
//			if ( cvarSystem->IsInitialized() && com_skipRenderer.GetBool() ) {
//				cmdString = Mem_CopyString( "quit" );
//				Sys_QueEvent( 0, SE_CONSOLE, 0, 0, strlen( cmdString ) + 1, cmdString );
//			} else if ( s_wcd.quitOnClose ) {
//				PostQuitMessage( 0 );
//			} else {
//				Sys_ShowConsole( 0, false );
//				win32.win_viewlog.SetBool( false );
//			}
//			return 0;
//		case WM_CTLCOLORSTATIC:
//			if ( ( HWND ) lParam == s_wcd.hwndBuffer ) {
//				SetBkColor( ( HDC ) wParam, RGB( 0x00, 0x00, 0x80 ) );
//				SetTextColor( ( HDC ) wParam, RGB( 0xff, 0xff, 0x00 ) );
//				return ( long ) s_wcd.hbrEditBackground;
//			} else if ( ( HWND ) lParam == s_wcd.hwndErrorBox ) {
//				if ( s_timePolarity & 1 ) {
//					SetBkColor( ( HDC ) wParam, RGB( 0x80, 0x80, 0x80 ) );
//					SetTextColor( ( HDC ) wParam, RGB( 0xff, 0x0, 0x00 ) );
//				} else {
//					SetBkColor( ( HDC ) wParam, RGB( 0x80, 0x80, 0x80 ) );
//					SetTextColor( ( HDC ) wParam, RGB( 0x00, 0x0, 0x00 ) );
//				}
//				return ( long ) s_wcd.hbrErrorBackground;
//			}
//			break;
//		case WM_SYSCOMMAND:
//			if ( wParam == SC_CLOSE ) {
//				PostQuitMessage( 0 );
//			}
//			break;
//		case WM_COMMAND:
//			if ( wParam == COPY_ID ) {
//				SendMessage( s_wcd.hwndBuffer, EM_SETSEL, 0, -1 );
//				SendMessage( s_wcd.hwndBuffer, WM_COPY, 0, 0 );
//			} else if ( wParam == QUIT_ID ) {
//				if ( s_wcd.quitOnClose ) {
//					PostQuitMessage( 0 );
//				} else {
//					cmdString = Mem_CopyString( "quit" );
//					Sys_QueEvent( 0, SE_CONSOLE, 0, 0, strlen( cmdString ) + 1, cmdString );
//				}
//			} else if ( wParam == CLEAR_ID ) {
//				SendMessage( s_wcd.hwndBuffer, EM_SETSEL, 0, -1 );
//				SendMessage( s_wcd.hwndBuffer, EM_REPLACESEL, FALSE, ( LPARAM ) "" );
//				UpdateWindow( s_wcd.hwndBuffer );
//			}
//			break;
//		case WM_CREATE:
//			s_wcd.hbrEditBackground = CreateSolidBrush( RGB( 0x00, 0x00, 0x80 ) );
//			s_wcd.hbrErrorBackground = CreateSolidBrush( RGB( 0x80, 0x80, 0x80 ) );
//			SetTimer( hWnd, 1, 1000, NULL );
//			break;
///*
//		case WM_ERASEBKGND:
//			HGDIOBJ oldObject;
//			HDC hdcScaled;
//			hdcScaled = CreateCompatibleDC( ( HDC ) wParam );
//			assert( hdcScaled != 0 );
//			if ( hdcScaled ) {
//				oldObject = SelectObject( ( HDC ) hdcScaled, s_wcd.hbmLogo );
//				assert( oldObject != 0 );
//				if ( oldObject )
//				{
//					StretchBlt( ( HDC ) wParam, 0, 0, s_wcd.windowWidth, s_wcd.windowHeight, 
//						hdcScaled, 0, 0, 512, 384,
//						SRCCOPY );
//				}
//				DeleteDC( hdcScaled );
//				hdcScaled = 0;
//			}
//			return 1;
//*/
//		case WM_TIMER:
//			if ( wParam == 1 ) {
//				s_timePolarity = (bool)!s_timePolarity;
//				if ( s_wcd.hwndErrorBox ) {
//					InvalidateRect( s_wcd.hwndErrorBox, NULL, FALSE );
//				}
//			}
//			break;
//    }
//
//    return DefWindowProc( hWnd, uMsg, wParam, lParam );
//    }
//    LONG WINAPI    InputLineWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
//	int key, cursor;
//	switch ( uMsg ) {
//	case WM_KILLFOCUS:
//		if ( ( HWND ) wParam == s_wcd.hWnd || ( HWND ) wParam == s_wcd.hwndErrorBox ) {
//			SetFocus( hWnd );
//			return 0;
//		}
//		break;
//
//	case WM_KEYDOWN:
//		key = MapKey( lParam );
//
//		// command history
//		if ( ( key == K_UPARROW ) || ( key == K_KP_UPARROW ) ) {
//			if ( s_wcd.nextHistoryLine - s_wcd.historyLine < COMMAND_HISTORY && s_wcd.historyLine > 0 ) {
//				s_wcd.historyLine--;
//			}
//			s_wcd.consoleField = s_wcd.historyEditLines[ s_wcd.historyLine % COMMAND_HISTORY ];
//
//			SetWindowText( s_wcd.hwndInputLine, s_wcd.consoleField.GetBuffer() );
//			SendMessage( s_wcd.hwndInputLine, EM_SETSEL, s_wcd.consoleField.GetCursor(), s_wcd.consoleField.GetCursor() );
//			return 0;
//		}
//
//		if ( ( key == K_DOWNARROW ) || ( key == K_KP_DOWNARROW ) ) {
//			if ( s_wcd.historyLine == s_wcd.nextHistoryLine ) {
//				return 0;
//			}
//			s_wcd.historyLine++;
//			s_wcd.consoleField = s_wcd.historyEditLines[ s_wcd.historyLine % COMMAND_HISTORY ];
//
//			SetWindowText( s_wcd.hwndInputLine, s_wcd.consoleField.GetBuffer() );
//			SendMessage( s_wcd.hwndInputLine, EM_SETSEL, s_wcd.consoleField.GetCursor(), s_wcd.consoleField.GetCursor() );
//			return 0;
//		}
//		break;
//
//	case WM_CHAR:
//		key = MapKey( lParam );
//
//		GetWindowText( s_wcd.hwndInputLine, s_wcd.consoleField.GetBuffer(), MAX_EDIT_LINE );
//		SendMessage( s_wcd.hwndInputLine, EM_GETSEL, (WPARAM) NULL, (LPARAM) &cursor );
//		s_wcd.consoleField.SetCursor( cursor );
//
//		// enter the line
//		if ( key == K_ENTER || key == K_KP_ENTER ) {
//			strncat( s_wcd.consoleText, s_wcd.consoleField.GetBuffer(), sizeof( s_wcd.consoleText ) - strlen( s_wcd.consoleText ) - 5 );
//			strcat( s_wcd.consoleText, "\n" );
//			SetWindowText( s_wcd.hwndInputLine, "" );
//
//			Sys_Printf( "]%s\n", s_wcd.consoleField.GetBuffer() );
//
//			// copy line to history buffer
//			s_wcd.historyEditLines[s_wcd.nextHistoryLine % COMMAND_HISTORY] = s_wcd.consoleField;
//			s_wcd.nextHistoryLine++;
//			s_wcd.historyLine = s_wcd.nextHistoryLine;
//
//			s_wcd.consoleField.Clear();
//
//			return 0;
//		}
//
//		// command completion
//		if ( key == K_TAB ) {
//			s_wcd.consoleField.AutoComplete();
//
//			SetWindowText( s_wcd.hwndInputLine, s_wcd.consoleField.GetBuffer() );
//			//s_wcd.consoleField.SetWidthInChars( strlen( s_wcd.consoleField.GetBuffer() ) );
//			SendMessage( s_wcd.hwndInputLine, EM_SETSEL, s_wcd.consoleField.GetCursor(), s_wcd.consoleField.GetCursor() );
//
//			return 0;
//		}
//
//		// clear autocompletion buffer on normal key input
//		if ( ( key >= K_SPACE && key <= K_BACKSPACE ) || 
//			( key >= K_KP_SLASH && key <= K_KP_PLUS ) || ( key >= K_KP_STAR && key <= K_KP_EQUALS ) ) {
//			s_wcd.consoleField.ClearAutoComplete();
//		}
//		break;
//	}
//
//	return CallWindowProc( s_wcd.SysInputLineWndProc, hWnd, uMsg, wParam, lParam );
//    }

    /*
     ** Sys_CreateConsole
     */
    static void Sys_CreateConsole() {//        throw new TODO_Exception();
//	HDC hDC;
        JFrame/*WNDCLASS*/ wc;
        Rectangle/*RECT*/ rect;
//	const char *DEDCLASS = WIN32_CONSOLE_CLASS;
        final int nHeight;
        final int swidth, sheight;
        final Dimension screen;
//	int DEDSTYLE = WS_POPUPWINDOW | WS_CAPTION | WS_MINIMIZEBOX;
        int i;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(win_syscon.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        wc = new JFrame(GAME_NAME);
//
//        wc.getContentPane().add(jPanel);
//
        rect = new Rectangle(0, 0, 540, 450);
//	AdjustWindowRect( &rect, DEDSTYLE, FALSE );//TODO: check this function.
//
//	hDC = GetDC( GetDesktopWindow() );
        screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        swidth = screen.width;
        sheight = screen.height;
//	ReleaseDC( GetDesktopWindow(), hDC );
//
        s_wcd.windowWidth = rect.width - rect.x + 1;
        s_wcd.windowHeight = rect.height - rect.y + 1;
//
//	//s_wcd.hbmLogo = LoadBitmap( win32.hInstance, MAKEINTRESOURCE( IDB_BITMAP_LOGO) );
//
//        wc.setName(GAME_NAME);
        wc.setIconImage(IDI_ICON1);
        wc.setLayout(new FlowLayout());
        wc.setLocation((swidth - 600) / 2, (sheight - 450) / 2);
        wc.setPreferredSize(new Dimension(540, 450));
        wc.setResizable(false);
//        wc.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        s_wcd.hWnd = wc;

        //
        // create fonts
        //
        nHeight = 12;//-MulDiv( 8, GetDeviceCaps( hDC, LOGPIXELSY ), 72 );

        s_wcd.hfBufferFont = new Font("Courier New", 0, nHeight);//CreateFont(nHeight, 0, 0, 0, FW_LIGHT, 0, 0, 0, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, FF_MODERN | FIXED_PITCH, "Courier New");

        //
        // create the input line
        //
        s_wcd.hwndInputLine = new JTextField("edit");
        s_wcd.hwndInputLine.setEditable(false);
        s_wcd.hwndInputLine.setLocation(6, 400);
        s_wcd.hwndInputLine.setPreferredSize(new Dimension(528, 20));

        //
        // create the buttons
        //
        s_wcd.hwndButtonCopy = new JButton("copy");
        s_wcd.hwndButtonCopy.setBounds(5, 425, 72, 24);
//        s_wcd.hwndButtonCopy.setLocation(5, 425);
//        s_wcd.hwndButtonCopy.setPreferredSize(new Dimension(72, 24));
//        s_wcd.hwndButtonCopy.setAction();
        s_wcd.hwndButtonCopy.addMouseListener(new Click() {

            @Override
            public void mouseClicked(MouseEvent e) {
//                System.out.println("---" + s_wcd.buffer.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s_wcd.textArea.getText()), null);
                System.out.println("--DBUG-- " + s_wcd.buffer.toString());
            }
        });

        s_wcd.hwndButtonClear = new JButton("clear");
        s_wcd.hwndButtonClear.setBounds(82, 425, 72, 24);
//        s_wcd.hwndButtonClear.setLocation(82, 425);
//        s_wcd.hwndButtonClear.setPreferredSize(new Dimension(72, 24));
//        s_wcd.hwndButtonClear.setAction();
        s_wcd.hwndButtonClear.addMouseListener(new Click() {

            @Override
            public void mouseClicked(MouseEvent e) {
                s_wcd.textArea.setText("");
            }
        });

        s_wcd.hwndButtonQuit = new JButton("quit");
        s_wcd.hwndButtonQuit.setBounds(462, 425, 72, 24);
//        s_wcd.hwndButtonQuit.setLocation(462, 425);
//        s_wcd.hwndButtonQuit.setPreferredSize(new Dimension(72, 24));
//        s_wcd.hwndButtonQuit.setAction();
        s_wcd.hwndButtonQuit.addMouseListener(new Click() {

            @Override
            public void mouseClicked(MouseEvent e) {
                common.Quit();
            }
        });

        //
        // create the scrollbuffer text area
        //
        s_wcd.textArea = new JTextArea();
        s_wcd.textArea.setEditable(false);
        s_wcd.textArea.setLineWrap(true);
        s_wcd.textArea.setWrapStyleWord(true);
        s_wcd.textArea.setFont(s_wcd.hfBufferFont);
        s_wcd.textArea.setBackground(Color.BLUE.darker().darker());
        s_wcd.textArea.setForeground(Color.YELLOW.brighter());

        //
        // create the scrollbuffer
        //
        s_wcd.hwndBuffer = new JScrollPane(s_wcd.textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s_wcd.hwndBuffer.setLocation(6, 40);
        s_wcd.hwndBuffer.setPreferredSize(new Dimension(526, 354));
//
//	s_wcd.SysInputLineWndProc = ( WNDPROC ) SetWindowLong( s_wcd.hwndInputLine, GWL_WNDPROC, ( long ) InputLineWndProc );
//	SendMessage( s_wcd.hwndInputLine, WM_SETFONT, ( WPARAM ) s_wcd.hfBufferFont, 0 );
//
        wc.getContentPane().add(s_wcd.hwndInputLine);
        wc.getContentPane().add(s_wcd.hwndBuffer);
        wc.getContentPane().add(s_wcd.hwndButtonCopy);
        wc.getContentPane().add(s_wcd.hwndButtonClear);
        wc.getContentPane().add(s_wcd.hwndButtonQuit);
        wc.pack();
        //TODO: switch off, for testing purposes only.
//        wc.setVisible(true);

        // don't show it now that we have a splash screen up
        if (win32.win_viewlog.GetBool()) {
            wc.setVisible(true);
//		UpdateWindow( s_wcd.hWnd );
//		SetForegroundWindow( s_wcd.hWnd );
            s_wcd.hwndInputLine.setFocusable(true);
        }

        s_wcd.consoleField.Clear();

        for (i = 0; i < COMMAND_HISTORY; i++) {
//            s_wcd.historyEditLines[i].Clear();
            s_wcd.historyEditLines[i] = new idEditField();
        }
    }

    /**
     * <editor-fold defaultstate="collapsed"> ******
     */
    /*
     void Sys_CreateConsole( void ) {
     HDC hDC;
     WNDCLASS wc;
     RECT rect;
     const char *DEDCLASS = WIN32_CONSOLE_CLASS;
     int nHeight;
     int swidth, sheight;
     int DEDSTYLE = WS_POPUPWINDOW | WS_CAPTION | WS_MINIMIZEBOX;
     int i;

     memset( &wc, 0, sizeof( wc ) );

     wc.style         = 0;
     wc.lpfnWndProc   = (WNDPROC) ConWndProc;
     wc.cbClsExtra    = 0;
     wc.cbWndExtra    = 0;
     wc.hInstance     = win32.hInstance;
     wc.hIcon         = LoadIcon( win32.hInstance, MAKEINTRESOURCE(IDI_ICON1));
     wc.hCursor       = LoadCursor (NULL,IDC_ARROW);
     wc.hbrBackground = (struct HBRUSH__ *)COLOR_WINDOW;
     wc.lpszMenuName  = 0;
     wc.lpszClassName = DEDCLASS;

     if ( !RegisterClass (&wc) ) {
     return;
     }

     rect.left = 0;
     rect.right = 540;
     rect.top = 0;
     rect.bottom = 450;
     AdjustWindowRect( &rect, DEDSTYLE, FALSE );

     hDC = GetDC( GetDesktopWindow() );
     swidth = GetDeviceCaps( hDC, HORZRES );
     sheight = GetDeviceCaps( hDC, VERTRES );
     ReleaseDC( GetDesktopWindow(), hDC );

     s_wcd.windowWidth = rect.right - rect.left + 1;
     s_wcd.windowHeight = rect.bottom - rect.top + 1;

     //s_wcd.hbmLogo = LoadBitmap( win32.hInstance, MAKEINTRESOURCE( IDB_BITMAP_LOGO) );

     s_wcd.hWnd = CreateWindowEx( 0,
     DEDCLASS,
     GAME_NAME,
     DEDSTYLE,
     ( swidth - 600 ) / 2, ( sheight - 450 ) / 2 , rect.right - rect.left + 1, rect.bottom - rect.top + 1,
     NULL,
     NULL,
     win32.hInstance,
     NULL );

     if ( s_wcd.hWnd == NULL ) {
     return;
     }

     //
     // create fonts
     //
     hDC = GetDC( s_wcd.hWnd );
     nHeight = -MulDiv( 8, GetDeviceCaps( hDC, LOGPIXELSY ), 72 );

     s_wcd.hfBufferFont = CreateFont( nHeight, 0, 0, 0, FW_LIGHT, 0, 0, 0, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, FF_MODERN | FIXED_PITCH, "Courier New" );

     ReleaseDC( s_wcd.hWnd, hDC );

     //
     // create the input line
     //
     s_wcd.hwndInputLine = CreateWindow( "edit", NULL, WS_CHILD | WS_VISIBLE | WS_BORDER | 
     ES_LEFT | ES_AUTOHSCROLL,
     6, 400, 528, 20,
     s_wcd.hWnd, 
     ( HMENU ) INPUT_ID,	// child window ID
     win32.hInstance, NULL );

     //
     // create the buttons
     //
     s_wcd.hwndButtonCopy = CreateWindow( "button", NULL, BS_PUSHBUTTON | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
     5, 425, 72, 24,
     s_wcd.hWnd, 
     ( HMENU ) COPY_ID,	// child window ID
     win32.hInstance, NULL );
     SendMessage( s_wcd.hwndButtonCopy, WM_SETTEXT, 0, ( LPARAM ) "copy" );

     s_wcd.hwndButtonClear = CreateWindow( "button", NULL, BS_PUSHBUTTON | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
     82, 425, 72, 24,
     s_wcd.hWnd, 
     ( HMENU ) CLEAR_ID,	// child window ID
     win32.hInstance, NULL );
     SendMessage( s_wcd.hwndButtonClear, WM_SETTEXT, 0, ( LPARAM ) "clear" );

     s_wcd.hwndButtonQuit = CreateWindow( "button", NULL, BS_PUSHBUTTON | WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
     462, 425, 72, 24,
     s_wcd.hWnd, 
     ( HMENU ) QUIT_ID,	// child window ID
     win32.hInstance, NULL );
     SendMessage( s_wcd.hwndButtonQuit, WM_SETTEXT, 0, ( LPARAM ) "quit" );


     //
     // create the scrollbuffer
     //
     s_wcd.hwndBuffer = CreateWindow( "edit", NULL, WS_CHILD | WS_VISIBLE | WS_VSCROLL | WS_BORDER | 
     ES_LEFT | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY,
     6, 40, 526, 354,
     s_wcd.hWnd, 
     ( HMENU ) EDIT_ID,	// child window ID
     win32.hInstance, NULL );
     SendMessage( s_wcd.hwndBuffer, WM_SETFONT, ( WPARAM ) s_wcd.hfBufferFont, 0 );

     s_wcd.SysInputLineWndProc = ( WNDPROC ) SetWindowLong( s_wcd.hwndInputLine, GWL_WNDPROC, ( long ) InputLineWndProc );
     SendMessage( s_wcd.hwndInputLine, WM_SETFONT, ( WPARAM ) s_wcd.hfBufferFont, 0 );

     // don't show it now that we have a splash screen up
     if ( win32.win_viewlog.GetBool() ) {
     ShowWindow( s_wcd.hWnd, SW_SHOWDEFAULT);
     UpdateWindow( s_wcd.hWnd );
     SetForegroundWindow( s_wcd.hWnd );
     SetFocus( s_wcd.hwndInputLine );
     }



     s_wcd.consoleField.Clear();

     for ( i = 0 ; i < COMMAND_HISTORY ; i++ ) {
     s_wcd.historyEditLines[i].Clear();
     }
     }
     */

    /**
     * ******
     * </editor-fold>
     */
    /*
     ** Sys_DestroyConsole
     */
    static void Sys_DestroyConsole() {

        if (s_wcd.hWnd != null) {
//		ShowWindow( s_wcd.hWnd, SW_HIDE );
            s_wcd.hWnd.setVisible(false);
            s_wcd.hWnd.dispose();
//		CloseWindow( s_wcd.hWnd );
//		DestroyWindow( s_wcd.hWnd );
            s_wcd.hWnd = null;
        }
    }

    /*
     ** Sys_ShowConsole
     */
    public static void Sys_ShowConsole(int visLevel, boolean quitOnClose) {

        s_wcd.setQuitOnClose(quitOnClose);

        if (NOT(s_wcd.hWnd)) {
            return;
        }

        switch (visLevel) {
            case 0:
                s_wcd.hWnd.setVisible(false);//ShowWindow( s_wcd.hWnd, SW_HIDE );
                break;
            case 1:
                s_wcd.textArea.setText(s_wcd.buffer.toString());
                s_wcd.hWnd.setVisible(true);//ShowWindow( s_wcd.hWnd, SW_SHOWNORMAL );
                s_wcd.hwndBuffer.getVerticalScrollBar().setValue(0xffff);//SendMessage(s_wcd.hwndBuffer, EM_LINESCROLL, 0, 0xffff);
                break;
            case 2:
                s_wcd.textArea.setText(s_wcd.buffer.toString());
                s_wcd.hWnd.setVisible(true);
                s_wcd.hWnd.setState(JFrame.ICONIFIED);//ShowWindow( s_wcd.hWnd, SW_MINIMIZE );
                break;
            default:
                Sys_Error("Invalid visLevel %d sent to Sys_ShowConsole\n", visLevel);
                break;
        }
    }

    /*
     ** Sys_ConsoleInput
     */
    static String Sys_ConsoleInput() {
        return null;
//	
//	if ( s_wcd.consoleText[0] == 0 ) {
//		return NULL;
//	}
//		
//	strcpy( s_wcd.returnedText, s_wcd.consoleText );
//	s_wcd.consoleText[0] = 0;
//	
//	return s_wcd.returnedText;
    }

    private static /*unsigned*/ long s_totalChars;

    /*
     ** Conbuf_AppendText
     */
    static void Conbuf_AppendText(final String pMsg) {

        StringBuilder buffer = new StringBuilder(CONSOLE_BUFFER_SIZE * 2);
        int b = 0;//buffer;
        final String msg;
        int bufLen;
        int i = 0;

        //
        // if the message is REALLY long, use just the last portion of it
        //
        if (isNotNullOrEmpty(pMsg)
                && pMsg.length() > CONSOLE_BUFFER_SIZE - 1) {
            msg = pMsg.substring(pMsg.length() - CONSOLE_BUFFER_SIZE + 1);
        } else {
            msg = pMsg;
        }

        //
        // copy into an intermediate buffer
        //
        while ((i < msg.length())//&& msg.charAt(i) != 0)//TODO: is the character ever '0' or '\0', or are we just wasting our fucking resources?
                && (b < buffer.capacity() - 1)) {
            if (msg.charAt(i) == '\n'
                    && (i + 1 < msg.length()
                    && msg.charAt(i + 1) == '\r')) {
                buffer.insert(b + 0, '\r');
                buffer.insert(b + 1, '\n');
                b += 2;
                i++;
            } else if (msg.charAt(i) == '\r') {
                buffer.insert(b + 0, '\r');
                buffer.insert(b + 1, '\n');
            } else if (msg.charAt(i) == '\n') {
                buffer.insert(b + 0, '\r');
                buffer.insert(b + 1, '\n');
                b += 2;
            } else if (idStr.IsColor(msg.substring(i))) {
                i++;
            } else {
                buffer.insert(b++, msg.charAt(i));
            }
            i++;
        }
//        buffer.insert(b, '\0');
        bufLen = b;//- buffer;

        s_totalChars += bufLen;

        //
        // replace selection instead of appending if we're overflowing
        //
        if (s_totalChars > 0x7000) {
            s_wcd.buffer = buffer;
            s_totalChars = bufLen;
        } else {
            s_wcd.buffer.append(buffer);
        }

//        //
//        // put this text into the windows console
//        //
//        SendMessage(s_wcd.hwndBuffer, EM_LINESCROLL, 0, 0xffff);
//        SendMessage(s_wcd.hwndBuffer, EM_SCROLLCARET, 0, 0);
//        SendMessage(s_wcd.hwndBuffer, EM_REPLACESEL, 0, (LPARAM) buffer);
    }

    /*
     ** Win_SetErrorText
     */
    static void Win_SetErrorText(final String buf) {

        idStr.Copynz(s_wcd.errorString, buf);
        if (NOT(s_wcd.hwndErrorBox)) {
            s_wcd.hwndErrorBox = new JTextField("static");
            s_wcd.hwndErrorBox.setEditable(false);
            s_wcd.hwndErrorBox.setLocation(6, 5);
            s_wcd.hwndErrorBox.setPreferredSize(new Dimension(526, 30));
            s_wcd.hwndErrorBox.setBackground(Color.GRAY);
            s_wcd.hwndErrorBox.setForeground(Color.RED);

            s_wcd.hwndErrorBox.setText(s_wcd.errorString.toString());

            s_wcd.hWnd.getContentPane().add(s_wcd.hwndErrorBox);

            s_wcd.hWnd.getContentPane().remove(s_wcd.hwndInputLine);
            s_wcd.hWnd.pack();
            s_wcd.hwndInputLine = null;
        }
    }

    static abstract class Click implements MouseListener {

        @Override
        public abstract void mouseClicked(MouseEvent e);

        @Override
        public final void mousePressed(MouseEvent e) {
        }

        @Override
        public final void mouseReleased(MouseEvent e) {
        }

        @Override
        public final void mouseEntered(MouseEvent e) {
        }

        @Override
        public final void mouseExited(MouseEvent e) {
        }
    }
}
