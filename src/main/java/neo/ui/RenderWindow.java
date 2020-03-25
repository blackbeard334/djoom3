package neo.ui;

import static neo.Game.GameEdit.gameEdit;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.Common.common;
import static neo.idlib.math.Vector.getVec3_origin;

import neo.Game.Animation.Anim.idMD5Anim;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;
import neo.ui.Winvar.idWinVec4;

/**
 *
 */
public class RenderWindow {

    public static final class idRenderWindow extends idWindow {

        private renderView_s   refdef;
        private idRenderWorld  world;
        private renderEntity_s worldEntity;
        private final renderLight_s rLight = new renderLight_s();
        private idMD5Anim        modelAnim;
        //
        private int/*qhandle_t*/ worldModelDef;
        private int/*qhandle_t*/ lightDef;
        private int/*qhandle_t*/ modelDef;
        private final idWinStr  modelName   = new idWinStr();
        private final idWinStr  animName    = new idWinStr();
        private final idStr     animClass   = new idStr();
        private final idWinVec4 lightOrigin = new idWinVec4();
        private final idWinVec4 lightColor  = new idWinVec4();
        private final idWinVec4 modelOrigin = new idWinVec4();
        private final idWinVec4 modelRotate = new idWinVec4();
        private final idWinVec4 viewOffset  = new idWinVec4();
        private final idWinBool needsRender = new idWinBool();
        private int     animLength;
        private int     animEndTime;
        private boolean updateAnimation;
        //
        //

        public idRenderWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.dc = null;
            this.gui = gui;
            CommonInit();
        }

        public idRenderWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	// virtual ~idRenderWindow();
//

        @Override
        public void PostParse() {
            super.PostParse();
        }

        @Override
        public void Draw(int time, float x, float y) {
            PreRender();
            Render(time);

//            memset(refdef, 0, sizeof(refdef));
            this.refdef = new renderView_s();
            this.refdef.vieworg = this.viewOffset.ToVec3();
            //refdef.vieworg.Set(-128, 0, 0);

            this.refdef.viewaxis.Identity();
            this.refdef.shaderParms[0] = 1;
            this.refdef.shaderParms[1] = 1;
            this.refdef.shaderParms[2] = 1;
            this.refdef.shaderParms[3] = 1;

            this.refdef.x = (int) this.drawRect.x;
            this.refdef.y = (int) this.drawRect.y;
            this.refdef.width = (int) this.drawRect.w;
            this.refdef.height = (int) this.drawRect.h;
            this.refdef.fov_x = 90;
            this.refdef.fov_y = (float) (2 * Math.atan(this.drawRect.h / this.drawRect.w) * idMath.M_RAD2DEG);

            this.refdef.time = time;
            this.world.RenderScene(this.refdef);
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {

            if (idStr.Icmp(_name, "model") == 0) {
                return this.modelName;
            }
            if (idStr.Icmp(_name, "anim") == 0) {
                return this.animName;
            }
            if (idStr.Icmp(_name, "lightOrigin") == 0) {
                return this.lightOrigin;
            }
            if (idStr.Icmp(_name, "lightColor") == 0) {
                return this.lightColor;
            }
            if (idStr.Icmp(_name, "modelOrigin") == 0) {
                return this.modelOrigin;
            }
            if (idStr.Icmp(_name, "modelRotate") == 0) {
                return this.modelRotate;
            }
            if (idStr.Icmp(_name, "viewOffset") == 0) {
                return this.viewOffset;
            }
            if (idStr.Icmp(_name, "needsRender") == 0) {
                return this.needsRender;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        private void CommonInit() {
            this.world = renderSystem.AllocRenderWorld();
            this.needsRender.data = true;
            this.lightOrigin.oSet(new idVec4(-128.0f, 0.0f, 0.0f, 1.0f));
            this.lightColor.oSet(new idVec4(1.0f, 1.0f, 1.0f, 1.0f));
            this.modelOrigin.Zero();
            this.viewOffset.oSet(new idVec4(-128.0f, 0.0f, 0.0f, 1.0f));
            this.modelAnim = null;
            this.animLength = 0;
            this.animEndTime = -1;
            this.modelDef = -1;
            this.updateAnimation = true;
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "animClass") == 0) {
                ParseString(src, this.animClass);
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        /**
         * This function renders the 3D shit to the screen.
         * @param time 
         */
        private void Render(int time) {
            this.rLight.origin = this.lightOrigin.ToVec3();//TODO:ref?
            this.rLight.shaderParms[SHADERPARM_RED] = this.lightColor.x();
            this.rLight.shaderParms[SHADERPARM_GREEN] = this.lightColor.y();
            this.rLight.shaderParms[SHADERPARM_BLUE] = this.lightColor.z();
            this.world.UpdateLightDef(this.lightDef, this.rLight);
            if (this.worldEntity.hModel != null) {
                if (this.updateAnimation) {
                    BuildAnimation(time);
                }
                if (this.modelAnim != null) {
                    if (time > this.animEndTime) {
                        this.animEndTime = time + this.animLength;
                    }
                    gameEdit.ANIM_CreateAnimFrame(this.worldEntity.hModel, this.modelAnim, this.worldEntity.numJoints, this.worldEntity.joints, this.animLength - (this.animEndTime - time), getVec3_origin(), false);
                }
                this.worldEntity.axis.oSet(new idAngles(this.modelRotate.x(), this.modelRotate.y(), this.modelRotate.z()).ToMat3());
//                System.out.printf("x=%f, y=%f, z=%f\n", modelRotate.x(), modelRotate.y(), modelRotate.z());
                this.world.UpdateEntityDef(this.modelDef, this.worldEntity);
            }
        }

        private void PreRender() {
            if (this.needsRender.oCastBoolean()) {
                this.world.InitFromMap(null);
                final idDict spawnArgs = new idDict();
                spawnArgs.Set("classname", "light");
                spawnArgs.Set("name", "light_1");
                spawnArgs.Set("origin", this.lightOrigin.ToVec3().ToString());
                spawnArgs.Set("_color", this.lightColor.ToVec3().ToString());
                gameEdit.ParseSpawnArgsToRenderLight(spawnArgs, this.rLight);
                this.lightDef = this.world.AddLightDef(this.rLight);
                if (!isNotNullOrEmpty(this.modelName.c_str())) {
                    common.Warning("Window '%s' in gui '%s': no model set", GetName(), GetGui().GetSourceFile());
                }
                this.worldEntity = new renderEntity_s();
                spawnArgs.Clear();
                spawnArgs.Set("classname", "func_static");
                spawnArgs.Set("model", this.modelName.c_str());
                spawnArgs.Set("origin", this.modelOrigin.c_str());
                gameEdit.ParseSpawnArgsToRenderEntity(spawnArgs, this.worldEntity);
                if (this.worldEntity.hModel != null) {
                    final idVec3 v = this.modelRotate.ToVec3();
                    this.worldEntity.axis.oSet(v.ToMat3());
                    this.worldEntity.shaderParms[0] = 1;
                    this.worldEntity.shaderParms[1] = 1;
                    this.worldEntity.shaderParms[2] = 1;
                    this.worldEntity.shaderParms[3] = 1;
                    this.modelDef = this.world.AddEntityDef(this.worldEntity);
                }
                this.needsRender.data = false;
            }
        }

        private void BuildAnimation(int time) {

            if (!this.updateAnimation) {
                return;
            }

            if ((this.animName.Length() != 0) && (this.animClass.Length() != 0)) {
                this.worldEntity.numJoints = this.worldEntity.hModel.NumJoints();
                this.worldEntity.joints = new idJointMat[this.worldEntity.numJoints];
                this.modelAnim = gameEdit.ANIM_GetAnimFromEntityDef(this.animClass.getData(), this.animName.toString());
                if (this.modelAnim != null) {
                    this.animLength = gameEdit.ANIM_GetLength(this.modelAnim);
                    this.animEndTime = time + this.animLength;
                }
            }
            this.updateAnimation = false;
        }
    }
}
