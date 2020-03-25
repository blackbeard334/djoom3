package neo.sys.RC;

import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 *
 */
public class doom_resource {

    public static final int IDB_BITMAP_LOGO = 4000;
    public static Image IDI_ICON1;

    static {
        try {
            IDI_ICON1 = ImageIO.read(doom_resource.class.getResource("res/doom.bmp"));//TODO: use a transparent png instead yo!
        } catch (final IOException ex) {
            Logger.getLogger(doom_resource.class.getName()).log(Level.SEVERE, null, ex);//TODO: log to doom console.
        }
    }
}
