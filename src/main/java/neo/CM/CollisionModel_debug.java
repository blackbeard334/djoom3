package neo.CM;

import static neo.Renderer.Material.CONTENTS_AAS_OBSTACLE;
import static neo.Renderer.Material.CONTENTS_AAS_SOLID;
import static neo.Renderer.Material.CONTENTS_BLOOD;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_FLASHLIGHT_TRIGGER;
import static neo.Renderer.Material.CONTENTS_IKCLIP;
import static neo.Renderer.Material.CONTENTS_MONSTERCLIP;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_OPAQUE;
import static neo.Renderer.Material.CONTENTS_PLAYERCLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Material.CONTENTS_WATER;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_GAME;

import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.idCmdSystem;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class CollisionModel_debug {
    /*
     ===============================================================================

     Visualisation code

     ===============================================================================
     */

    static final String[] cm_contentsNameByIndex = {
            "none",							// 0
            "solid",						// 1
            "opaque",						// 2
            "water",						// 3
            "playerclip",					// 4
            "monsterclip",					// 5
            "moveableclip",					// 6
            "ikclip",						// 7
            "blood",						// 8
            "body",							// 9
            "corpse",						// 10
            "trigger",						// 11
            "aas_solid",					// 12
            "aas_obstacle",					// 13
            "flashlight_trigger",			// 14
            null
    };

    static final int[] cm_contentsFlagByIndex = {
            -1,								// 0
            CONTENTS_SOLID,					// 1
            CONTENTS_OPAQUE,				// 2
            CONTENTS_WATER,					// 3
            CONTENTS_PLAYERCLIP,			// 4
            CONTENTS_MONSTERCLIP,			// 5
            CONTENTS_MOVEABLECLIP,			// 6
            CONTENTS_IKCLIP,				// 7
            CONTENTS_BLOOD,					// 8
            CONTENTS_BODY,					// 9
            CONTENTS_CORPSE,				// 10
            CONTENTS_TRIGGER,				// 11
            CONTENTS_AAS_SOLID,				// 12
            CONTENTS_AAS_OBSTACLE,			// 13
            CONTENTS_FLASHLIGHT_TRIGGER,	// 14
            0
    };
    //
    static final idCVar cm_drawMask       = new idCVar("cm_drawMask", "none", CVAR_GAME, "collision mask", cm_contentsNameByIndex, new idCmdSystem.ArgCompletion_String(cm_contentsNameByIndex));
    static final idCVar cm_drawColor      = new idCVar("cm_drawColor", "1 0 0 .5", CVAR_GAME, "color used to draw the collision models");
    static final idCVar cm_drawFilled     = new idCVar("cm_drawFilled", "0", CVAR_GAME | CVAR_BOOL, "draw filled polygons");
    static final idCVar cm_drawInternal   = new idCVar("cm_drawInternal", "1", CVAR_GAME | CVAR_BOOL, "draw internal edges green");
    static final idCVar cm_drawNormals    = new idCVar("cm_drawNormals", "0", CVAR_GAME | CVAR_BOOL, "draw polygon and edge normals");
    static final idCVar cm_backFaceCull   = new idCVar("cm_backFaceCull", "0", CVAR_GAME | CVAR_BOOL, "cull back facing polygons");
    static final idCVar cm_debugCollision = new idCVar("cm_debugCollision", "0", CVAR_GAME | CVAR_BOOL, "debug the collision detection");
    //
    static idVec4 cm_color;
}
