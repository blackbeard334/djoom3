package neo.Game;

import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.PlayerIcon.playerIconType_t.ICON_CHAT;
import static neo.Game.PlayerIcon.playerIconType_t.ICON_LAG;
import static neo.Game.PlayerIcon.playerIconType_t.ICON_NONE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_SPRITE_HEIGHT;
import static neo.Renderer.RenderWorld.SHADERPARM_SPRITE_WIDTH;
import static neo.TempDump.etoi;
import static neo.framework.DeclManager.declManager;

import neo.Game.Player.idPlayer;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class PlayerIcon {

    public enum playerIconType_t {

        ICON_LAG,
        ICON_CHAT,
        ICON_NONE
    }
    public static final String[] iconKeys/*[ ICON_NONE ]*/ = {
                "mtr_icon_lag",
                "mtr_icon_chat"
            };

    public static class idPlayerIcon {

        public playerIconType_t iconType;
        public renderEntity_s renderEnt;
        public int/*qhandle_t*/ iconHandle;
        //
        //

        public idPlayerIcon() {
            this.iconHandle = -1;
            this.iconType = ICON_NONE;
        }
        // ~idPlayerIcon();

        public void Draw(idPlayer player, int/*jointHandle_t*/ joint) {
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (joint == INVALID_JOINT) {
                FreeIcon();
                return;
            }

            player.GetJointWorldTransform(joint, gameLocal.time, origin, axis);
            origin.z += 16.0f;

            Draw(player, origin);
        }

        public void Draw(idPlayer player, final idVec3 origin) {
            final idPlayer localPlayer = gameLocal.GetLocalPlayer();
            if ((null == localPlayer) || (null == localPlayer.GetRenderView())) {
                FreeIcon();
                return;
            }

            final idMat3 axis = localPlayer.GetRenderView().viewaxis;

            if (player.isLagged) {
                // create the icon if necessary, or update if already created
                if (!CreateIcon(player, ICON_LAG, origin, axis)) {
                    UpdateIcon(player, origin, axis);
                }
            } else if (player.isChatting) {
                if (!CreateIcon(player, ICON_CHAT, origin, axis)) {
                    UpdateIcon(player, origin, axis);
                }
            } else {
                FreeIcon();
            }
        }

        public void FreeIcon() {
            if (this.iconHandle != - 1) {
                gameRenderWorld.FreeEntityDef(this.iconHandle);
                this.iconHandle = -1;
            }
            this.iconType = ICON_NONE;
        }

        public boolean CreateIcon(idPlayer player, playerIconType_t type, final String mtr, final idVec3 origin, final idMat3 axis) {
            assert (type != ICON_NONE);

            if (type == this.iconType) {
                return false;
            }

            FreeIcon();

//	memset( &renderEnt, 0, sizeof( renderEnt ) );
            this.renderEnt = new renderEntity_s();
            this.renderEnt.origin.oSet(origin);
            this.renderEnt.axis.oSet(axis);
            this.renderEnt.shaderParms[SHADERPARM_RED] = 1.0f;
            this.renderEnt.shaderParms[SHADERPARM_GREEN] = 1.0f;
            this.renderEnt.shaderParms[SHADERPARM_BLUE] = 1.0f;
            this.renderEnt.shaderParms[SHADERPARM_ALPHA] = 1.0f;
            this.renderEnt.shaderParms[SHADERPARM_SPRITE_WIDTH] = 16.0f;
            this.renderEnt.shaderParms[SHADERPARM_SPRITE_HEIGHT] = 16.0f;
            this.renderEnt.hModel = renderModelManager.FindModel("_sprite");
            this.renderEnt.callback = null;
            this.renderEnt.numJoints = 0;
            this.renderEnt.joints = null;
            this.renderEnt.customSkin = null;
            this.renderEnt.noShadow = true;
            this.renderEnt.noSelfShadow = true;
            this.renderEnt.customShader = declManager.FindMaterial(mtr);
            this.renderEnt.referenceShader = null;
            this.renderEnt.bounds.oSet(this.renderEnt.hModel.Bounds(this.renderEnt));

            this.iconHandle = gameRenderWorld.AddEntityDef(this.renderEnt);
            this.iconType = type;

            return true;
        }

        public boolean CreateIcon(idPlayer player, playerIconType_t type, final idVec3 origin, final idMat3 axis) {
            assert (type != ICON_NONE);
            final String mtr = player.spawnArgs.GetString(iconKeys[etoi(type)], "_default");
            return CreateIcon(player, type, mtr, origin, axis);
        }

        public void UpdateIcon(idPlayer player, final idVec3 origin, final idMat3 axis) {
            assert (this.iconHandle >= 0);

            this.renderEnt.origin.oSet(origin);
            this.renderEnt.axis.oSet(axis);
            gameRenderWorld.UpdateEntityDef(this.iconHandle, this.renderEnt);
        }
    }
}
