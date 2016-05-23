package neo.Renderer;

import neo.Renderer.RenderWorld_local.portal_s;
import neo.Renderer.tr_local.idScreenRect;
import neo.idlib.math.Plane.idPlane;

import java.util.stream.Stream;

/**
 *
 */
public class RenderWorld_portals {

    /*


     All that is done in these functions is the creation of viewLights
     and viewEntitys for the lightDefs and entityDefs that are visible
     in the portal areas that can be seen from the current viewpoint.

     */
    //
    //
    // if we hit this many planes, we will just stop cropping the
    // view down, which is still correct, just conservative
    static final int MAX_PORTAL_PLANES = 20;

    static class portalStack_s {

        portal_s      p;
        portalStack_s next;
        //
        idScreenRect  rect;
        //
        int           numPortalPlanes;
        idPlane[]     portalPlanes = Stream.generate(idPlane::new).limit(MAX_PORTAL_PLANES + 1).toArray(idPlane[]::new);
        // positive side is outside the visible frustum

        public portalStack_s() {
            p = new portal_s();
            rect = new idScreenRect();
        }

        public portalStack_s(final portalStack_s p) {
            this.p = p.p;
            this.next = p.next;
            this.rect = new idScreenRect(p.rect);
            for (int i = 0; i < portalPlanes.length; i++) {
                this.portalPlanes[i].oSet(p.portalPlanes[i]);
            }
        }
    };

}
