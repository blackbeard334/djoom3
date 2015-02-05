package neo.Tools.Compilers.DMap;

import static neo.Renderer.qgl.qglVertex3fv;
import static neo.Renderer.tr_backend.RB_SetGL2D;
import neo.TempDump.TODO_Exception;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Vector.idVec3;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POLYGON;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glVertex3f;

/**
 *
 */
public class gldraw {

    static final int WIN_SIZE = 1024;

    static void Draw_ClearWindow() {

        if (!dmapGlobals.drawflag) {
            return;
        }

        glDrawBuffer(GL_FRONT);

        RB_SetGL2D();

        glClearColor(0.5f, 0.5f, 0.5f, 0);
        glClear(GL_COLOR_BUFFER_BIT);

//#if 0
//	int		w, h, g;
//	float	mx, my;
//
//	w = (dmapGlobals.drawBounds.b[1][0] - dmapGlobals.drawBounds.b[0][0]);
//	h = (dmapGlobals.drawBounds.b[1][1] - dmapGlobals.drawBounds.b[0][1]);
//
//	mx = dmapGlobals.drawBounds.b[0][0] + w/2;
//	my = dmapGlobals.drawBounds.b[1][1] + h/2;
//
//	g = w > h ? w : h;
//
//	glLoadIdentity ();
//    gluPerspective (90,  1,  2,  16384);
//	gluLookAt (mx, my, draw_maxs[2] + g/2, mx , my, draw_maxs[2], 0, 1, 0);
//#else
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(dmapGlobals.drawBounds.oGet(0, 0), dmapGlobals.drawBounds.oGet(1, 0),
                dmapGlobals.drawBounds.oGet(0, 1), dmapGlobals.drawBounds.oGet(1, 1),
                -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
//#endif
        glColor3f(0, 0, 0);
//	glPolygonMode (GL_FRONT_AND_BACK, GL_LINE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDisable(GL_DEPTH_TEST);
//	glEnable (GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

//#if 0
////glColor4f (1,0,0,0.5);
////	glBegin( GL_LINE_LOOP );
//	glBegin( GL_QUADS );
//
//	glVertex2f( dmapGlobals.drawBounds.b[0][0] + 20, dmapGlobals.drawBounds.b[0][1] + 20 );
//	glVertex2f( dmapGlobals.drawBounds.b[1][0] - 20, dmapGlobals.drawBounds.b[0][1] + 20 );
//	glVertex2f( dmapGlobals.drawBounds.b[1][0] - 20, dmapGlobals.drawBounds.b[1][1] - 20 );
//	glVertex2f( dmapGlobals.drawBounds.b[0][0] + 20, dmapGlobals.drawBounds.b[1][1] - 20 );
//
//	glEnd ();
//#endif
        glFlush();

    }

    static void Draw_SetRed() {
        if (!dmapGlobals.drawflag) {
            return;
        }

        glColor3f(1, 0, 0);
    }

    static void Draw_SetGrey() {
        if (!dmapGlobals.drawflag) {
            return;
        }

        glColor3f(0.5f, 0.5f, 0.5f);
    }

    static void Draw_SetBlack() {
        if (!dmapGlobals.drawflag) {
            return;
        }

        glColor3f(0.0f, 0.0f, 0.0f);
    }

    static void DrawWinding(final idWinding w) {
        int i;

        if (!dmapGlobals.drawflag) {
            return;
        }

        glColor3f(0.3f, 0.0f, 0.0f);
        glBegin(GL_POLYGON);
        for (i = 0; i < w.GetNumPoints(); i++) {
            glVertex3f(w.oGet(i).oGet(0), w.oGet(i).oGet(1), w.oGet(i).oGet(2));
        }
        glEnd();

        glColor3f(1, 0, 0);
        glBegin(GL_LINE_LOOP);
        for (i = 0; i < w.GetNumPoints(); i++) {
            glVertex3f(w.oGet(i).oGet(0), w.oGet(i).oGet(1), w.oGet(i).oGet(2));
        }
        glEnd();

        glFlush();
    }

    static void DrawAuxWinding(final idWinding w) {
        int i;

        if (!dmapGlobals.drawflag) {
            return;
        }

        glColor3f(0.0f, 0.3f, 0.0f);
        glBegin(GL_POLYGON);
        for (i = 0; i < w.GetNumPoints(); i++) {
            glVertex3f(w.oGet(i).oGet(0), w.oGet(i).oGet(1), w.oGet(i).oGet(2));
        }
        glEnd();

        glColor3f(0.0f, 1.0f, 0.0f);
        glBegin(GL_LINE_LOOP);
        for (i = 0; i < w.GetNumPoints(); i++) {
            glVertex3f(w.oGet(i).oGet(0), w.oGet(i).oGet(1), w.oGet(i).oGet(2));
        }
        glEnd();

        glFlush();
    }

    static void DrawLine(idVec3 v1, idVec3 v2, int color) {
        if (!dmapGlobals.drawflag) {
            return;
        }

        switch (color) {
            case 0:
                glColor3f(0, 0, 0);
                break;
            case 1:
                glColor3f(0, 0, 1);
                break;
            case 2:
                glColor3f(0, 1, 0);
                break;
            case 3:
                glColor3f(0, 1, 1);
                break;
            case 4:
                glColor3f(1, 0, 0);
                break;
            case 5:
                glColor3f(1, 0, 1);
                break;
            case 6:
                glColor3f(1, 1, 0);
                break;
            case 7:
                glColor3f(1, 1, 1);
                break;
        }

        glBegin(GL_LINES);

        qglVertex3fv(v1.ToFloatPtr());
        qglVertex3fv(v2.ToFloatPtr());

        glEnd();
        glFlush();
    }
//============================================================
    static final int GLSERV_PORT = 25001;
//
    static boolean wins_init;
    static int draw_socket;

    static void GLS_BeginScene() {
        throw new TODO_Exception();
//        WSADATA winsockdata;
//        WORD wVersionRequested;
//        sockaddr_in address;
//        int r;
//
//        if (!wins_init) {
//            wins_init = true;
//
//            wVersionRequested = MAKEWORD(1, 1);
//
//            r = WSAStartup(MAKEWORD(1, 1), winsockdata);
//
//            if (r != 0) {
//                common.Error("Winsock initialization failed.");
//            }
//
//        }
//
//        // connect a socket to the server
//        draw_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
//        if (draw_socket == -1) {
//            common.Error("draw_socket failed");
//        }
//
//        address.sin_family = AF_INET;
//        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
//        address.sin_port = GLSERV_PORT;
//        r = connect(draw_socket, (sockaddr) address, sizeof(address));
//        if (r == -1) {
//            closesocket(draw_socket);
//            draw_socket = 0;
//        }
    }

    static void GLS_Winding(final idWinding w, int code) {
        throw new TODO_Exception();
//        byte[] buf = new byte[1024];
//        int i, j;
//
//        if (0 == draw_socket) {
//            return;
//        }
//
//        buf[0] = w.GetNumPoints();//TODO:put int into multiple bytes?
//        buf[1] = code;
//        for (i = 0; i < w.GetNumPoints(); i++) {
//            for (j = 0; j < 3; j++) //			((float *)buf)[2+i*3+j] = (*w)[i][j];
//            {
//                buf[2 + i * 3 + j] = w.oGet(i).oGet(j);//TODO:put float into multiple bytes?
//            }
//        }
//        send(draw_socket, (String) buf, w.GetNumPoints() * 12 + 8, 0);
    }

    static void GLS_Triangle(final mapTri_s tri, int code) {
        throw new TODO_Exception();
//        idWinding w = new idWinding();
//
//        w.SetNumPoints(3);
//        VectorCopy(tri.v[0].xyz, w.oGet(0));
//        VectorCopy(tri.v[1].xyz, w.oGet(1));
//        VectorCopy(tri.v[2].xyz, w.oGet(2));
//        GLS_Winding(w, code);
    }

    static void GLS_EndScene() {
        throw new TODO_Exception();
//        closesocket(draw_socket);
//        draw_socket = 0;
    }
//#else
//void Draw_ClearWindow(  ) {
//}
//
//void DrawWinding( final idWinding w) {
//}
//
//void DrawAuxWinding ( final idWinding w) {
//}
//
//void GLS_Winding( final idWinding w, int code ) {
//}
//
//void GLS_BeginScene () {
//}
//
//void GLS_EndScene ()
//{
//}
//
//#endif
}
