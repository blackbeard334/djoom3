/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.Renderer;

import neo.Renderer.RenderWorld_local.portal_s;
import neo.Renderer.tr_local.idScreenRect;
import neo.idlib.math.Plane.idPlane;

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

        portal_s p;
        portalStack_s next;
        //
        idScreenRect rect;
        //
        int numPortalPlanes;
        idPlane[] portalPlanes = new idPlane[MAX_PORTAL_PLANES + 1];
        // positive side is outside the visible frustum
    };

}
