package neo.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.GLXARBGetProcAddress;
import org.lwjgl.system.linux.XVisualInfo;

/**
 * GLX stuff from Cpp Source 
 */
public class QGLXNotTestedCpp {

	public static void qglXChooseVisual(int display, int screen, IntBuffer attrib_list) {
		QGL.DEBUG_printName("glXChooseVisual");
		GLX.glXChooseVisual(display, screen, attrib_list);
	}

	public static void qglXCreateContext(long display, XVisualInfo visual, long share_list, boolean direct) {
		QGL.DEBUG_printName("glXCreateContext");
		GLX.glXCreateContext(display, visual, share_list, direct);
	}

	public static void qglXDestroyContext(long display, long ctx) {
		QGL.DEBUG_printName("glXDestroyContext");
		GLX.glXDestroyContext(display, ctx);
	}

	public static void qglXGetProcAddressARB(ByteBuffer procName) {
		QGL.DEBUG_printName("glXGetProcAddressARB");
		GLXARBGetProcAddress.glXGetProcAddressARB(procName);
	}

	public static void qglXMakeCurrent(long display, long draw, long ctx) {
		QGL.DEBUG_printName("glXMakeCurrent");
		GLX.glXMakeCurrent(display, draw, ctx);
	}

	public static void qglXSwapBuffers(long display, long draw) {
		QGL.DEBUG_printName("glXSwapBuffers");
		GLX.glXSwapBuffers(display, draw);
	}

}
