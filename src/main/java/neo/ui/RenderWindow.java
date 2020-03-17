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
            refdef = new renderView_s();
            refdef.vieworg = viewOffset.ToVec3();
            //refdef.vieworg.Set(-128, 0, 0);

            refdef.viewaxis.Identity();
            refdef.shaderParms[0] = 1;
            refdef.shaderParms[1] = 1;
            refdef.shaderParms[2] = 1;
            refdef.shaderParms[3] = 1;

            refdef.x = (int) drawRect.x;
            refdef.y = (int) drawRect.y;
            refdef.width = (int) drawRect.w;
            refdef.height = (int) drawRect.h;
            refdef.fov_x = 90;
            refdef.fov_y = (float) (2 * Math.atan((float) drawRect.h / drawRect.w) * idMath.M_RAD2DEG);

            refdef.time = time;
            world.RenderScene(refdef);
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {

            if (idStr.Icmp(_name, "model") == 0) {
                return modelName;
            }
            if (idStr.Icmp(_name, "anim") == 0) {
                return animName;
            }
            if (idStr.Icmp(_name, "lightOrigin") == 0) {
                return lightOrigin;
            }
            if (idStr.Icmp(_name, "lightColor") == 0) {
                return lightColor;
            }
            if (idStr.Icmp(_name, "modelOrigin") == 0) {
                return modelOrigin;
            }
            if (idStr.Icmp(_name, "modelRotate") == 0) {
                return modelRotate;
            }
            if (idStr.Icmp(_name, "viewOffset") == 0) {
                return viewOffset;
            }
            if (idStr.Icmp(_name, "needsRender") == 0) {
                return needsRender;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        private void CommonInit() {
            world = renderSystem.AllocRenderWorld();
            needsRender.data = true;
            lightOrigin.oSet(new idVec4(-128.0f, 0.0f, 0.0f, 1.0f));
            lightColor.oSet(new idVec4(1.0f, 1.0f, 1.0f, 1.0f));
            modelOrigin.Zero();
            viewOffset.oSet(new idVec4(-128.0f, 0.0f, 0.0f, 1.0f));
            modelAnim = null;
            animLength = 0;
            animEndTime = -1;
            modelDef = -1;
            updateAnimation = true;
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "animClass") == 0) {
                ParseString(src, animClass);
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        /**
         * This function renders the 3D shit to the screen.
         * @param time 
         */
        private void Render(int time) {
            rLight.origin = lightOrigin.ToVec3();//TODO:ref?
            rLight.shaderParms[SHADERPARM_RED] = lightColor.x();
            rLight.shaderParms[SHADERPARM_GREEN] = lightColor.y();
            rLight.shaderParms[SHADERPARM_BLUE] = lightColor.z();
            world.UpdateLightDef(lightDef, rLight);
            if (worldEntity.hModel != null) {
                if (updateAnimation) {
                    BuildAnimation(time);
                }
                if (modelAnim != null) {
                    if (time > animEndTime) {
                        animEndTime = time + animLength;
                    }
                    gameEdit.ANIM_CreateAnimFrame(worldEntity.hModel, modelAnim, worldEntity.numJoints, worldEntity.joints, animLength - (animEndTime - time), getVec3_origin(), false);
                }
                worldEntity.axis.oSet(new idAngles(modelRotate.x(), modelRotate.y(), modelRotate.z()).ToMat3());
//                System.out.printf("x=%f, y=%f, z=%f\n", modelRotate.x(), modelRotate.y(), modelRotate.z());
                world.UpdateEntityDef(modelDef, worldEntity);
            }
        }

        private void PreRender() {
            if (needsRender.oCastBoolean()) {
                world.InitFromMap(null);
                idDict spawnArgs = new idDict();
                spawnArgs.Set("classname", "light");
                spawnArgs.Set("name", "light_1");
                spawnArgs.Set("origin", lightOrigin.ToVec3().ToString());
                spawnArgs.Set("_color", lightColor.ToVec3().ToString());
                gameEdit.ParseSpawnArgsToRenderLight(spawnArgs, rLight);
                lightDef = world.AddLightDef(rLight);
                if (!isNotNullOrEmpty(modelName.c_str())) {
                    common.Warning("Window '%s' in gui '%s': no model set", GetName(), GetGui().GetSourceFile());
                }
                worldEntity = new renderEntity_s();
                spawnArgs.Clear();
                spawnArgs.Set("classname", "func_static");
                spawnArgs.Set("model", modelName.c_str());
                spawnArgs.Set("origin", modelOrigin.c_str());
                gameEdit.ParseSpawnArgsToRenderEntity(spawnArgs, worldEntity);
                if (worldEntity.hModel != null) {
                    idVec3 v = modelRotate.ToVec3();
                    worldEntity.axis.oSet(v.ToMat3());
                    worldEntity.shaderParms[0] = 1;
                    worldEntity.shaderParms[1] = 1;
                    worldEntity.shaderParms[2] = 1;
                    worldEntity.shaderParms[3] = 1;
                    modelDef = world.AddEntityDef(worldEntity);
                }
                needsRender.data = false;
            }
        }

        private void BuildAnimation(int time) {

            if (!updateAnimation) {
                return;
            }

            if (animName.Length() != 0 && animClass.Length() != 0) {
                worldEntity.numJoints = worldEntity.hModel.NumJoints();
                worldEntity.joints = new idJointMat[worldEntity.numJoints];
                modelAnim = gameEdit.ANIM_GetAnimFromEntityDef(animClass.getData(), animName.toString());
                if (modelAnim != null) {
                    animLength = gameEdit.ANIM_GetLength(modelAnim);
                    animEndTime = time + animLength;
                }
            }
            updateAnimation = false;
        }
    };
}
