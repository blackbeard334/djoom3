package neo.Renderer;

import static neo.opengl.QGLConstants.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
import static neo.opengl.QGLConstants.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;

/**
 *
 */
public class Image_load {

    /*
     PROBLEM: compressed textures may break the zero clamp rule!
     */
    static boolean FormatIsDXT(int internalFormat) {
        if ((internalFormat < GL_COMPRESSED_RGB_S3TC_DXT1_EXT)
                || (internalFormat > GL_COMPRESSED_RGBA_S3TC_DXT5_EXT)) {
            return false;
        }
        return true;
    }

    static int MakePowerOfTwo(int num) {
        int pot;
        for (pot = 1; pot < num; pot <<= 1) {
        }
        return pot;
    }
}
