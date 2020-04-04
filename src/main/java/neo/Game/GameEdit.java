package neo.Game;

import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.SysCvar.g_dragDamping;
import static neo.Game.GameSys.SysCvar.g_dragShowSelection;
import static neo.Game.GameSys.SysCvar.g_editEntityMode;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Physics.Clip.CLIPMODEL_ID_TO_JOINT_HANDLE;
import static neo.Game.Physics.Clip.JOINT_HANDLE_TO_CLIPMODEL_ID;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declState_t.DS_DEFAULTED;
import static neo.framework.UsercmdGen.BUTTON_ATTACK;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;

import java.util.Scanner;
import java.util.regex.Pattern;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Entity.idEntity;
import neo.Game.Game.idGameEdit;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Light.idLight;
import neo.Game.Misc.idFuncEmitter;
import neo.Game.Player.idPlayer;
import neo.Game.Sound.idSound;
import neo.Game.WorldSpawn.idWorldspawn;
import neo.Game.AI.AI.idAI;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.Physics.Force_Drag.idForce_Drag;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_AF.idPhysics_AF;
import neo.Game.Physics.Physics_Monster.idPhysics_Monster;
import neo.Game.Physics.Physics_RigidBody.idPhysics_RigidBody;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class GameEdit {

    private static final idGameEdit gameEditLocal = new idGameEdit();
    public static final idGameEdit gameEdit = gameEditLocal;

    /*
     ===============================================================================

     Ingame cursor.

     ===============================================================================
     */
    public static class idCursor3D extends idEntity {

/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//    public 	CLASS_PROTOTYPE( idCursor3D );
        public idCursor3D() {
            this.draggedPosition.Zero();
        }
        //~idCursor3D( void );

        @Override
        public void Spawn() {
        }

        @Override
        public void Present() {
            // don't present to the renderer if the entity hasn't changed
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            final idVec3 origin = GetPhysics().GetOrigin();
            final idMat3 axis = GetPhysics().GetAxis();
            gameRenderWorld.DebugArrow(colorYellow, origin.oPlus(axis.oGet(1).oMultiply(-5.0f).oPlus(axis.oGet(2).oMultiply(5.0f))), origin, 2);
            gameRenderWorld.DebugArrow(colorRed, origin, this.draggedPosition, 2);
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                this.drag.Evaluate(gameLocal.time);
            }
            Present();
        }
//        
//        
        public idForce_Drag drag;
        public idVec3 draggedPosition;
    }

    /*
     ===============================================================================

     Allows entities to be dragged through the world with physics.

     ===============================================================================
     */
    public static final float MAX_DRAG_TRACE_DISTANCE = 2048.0f;

    public static class idDragEntity {

        private idEntityPtr<idEntity> dragEnt;          // entity being dragged
        private int/*jointHandle_t*/  joint;            // joint being dragged
        private int                   id;               // id of body being dragged
        private final idVec3                localEntityPoint; // dragged point in entity space
        private final idVec3                localPlayerPoint; // dragged point in player space
        private idStr                 bodyName;         // name of the body being dragged
        private idCursor3D            cursor;           // cursor entity
        private idEntityPtr<idEntity> selected;         // last dragged entity
        //
        //

        public idDragEntity() {
            this.cursor = null;
            this.localEntityPoint = new idVec3();
            this.localPlayerPoint = new idVec3();
            this.bodyName = new idStr();
            Clear();
        }
        // ~idDragEntity( void );

        public void Clear() {
            this.dragEnt = null;
            this.joint = INVALID_JOINT;
            this.id = 0;
            this.localEntityPoint.Zero();
            this.localPlayerPoint.Zero();
            this.bodyName.Clear();
            this.selected = null;
        }

        public void Update(idPlayer player) {
            final idVec3 viewPoint = new idVec3();
			idVec3 origin;
            final idMat3 viewAxis = new idMat3();
			idMat3 axis = new idMat3();
            final trace_s[] trace = {null};
            idEntity newEnt;
            final idAngles angles;
            int/*jointHandle_t*/ newJoint = 0;
            idStr newBodyName = new idStr();

            player.GetViewPos(viewPoint, viewAxis);

            // if no entity selected for dragging
            if (NOT(this.dragEnt.GetEntity())) {

                if ((player.usercmd.buttons & BUTTON_ATTACK) != 0) {

                    gameLocal.clip.TracePoint(trace, viewPoint, viewPoint.oPlus(viewAxis.oGet(0).oMultiply(MAX_DRAG_TRACE_DISTANCE)), (CONTENTS_SOLID | CONTENTS_RENDERMODEL | CONTENTS_BODY), player);
                    if (trace[0].fraction < 1.0f) {

                        newEnt = gameLocal.entities[ trace[0].c.entityNum];
                        if (newEnt != null) {

                            if (newEnt.GetBindMaster() != null) {
                                if (newEnt.GetBindJoint() != 0) {
                                    trace[0].c.id = JOINT_HANDLE_TO_CLIPMODEL_ID(newEnt.GetBindJoint());
                                } else {
                                    trace[0].c.id = newEnt.GetBindBody();
                                }
                                newEnt = newEnt.GetBindMaster();
                            }

                            if (newEnt.IsType(idAFEntity_Base.class) && ((idAFEntity_Base) newEnt).IsActiveAF()) {
                                final idAFEntity_Base af = (idAFEntity_Base) newEnt;

                                // joint being dragged
                                newJoint = CLIPMODEL_ID_TO_JOINT_HANDLE(trace[0].c.id);
                                // get the body id from the trace model id which might be a joint handle
                                trace[0].c.id = af.BodyForClipModelId(trace[0].c.id);
                                // get the name of the body being dragged
                                newBodyName = af.GetAFPhysics().GetBody(trace[0].c.id).GetName();

                            } else if (!newEnt.IsType(idWorldspawn.class)) {

                                if (trace[0].c.id < 0) {
                                    newJoint = CLIPMODEL_ID_TO_JOINT_HANDLE(trace[0].c.id);
                                } else {
                                    newJoint = INVALID_JOINT;
                                }
                                newBodyName = new idStr("");

                            } else {

                                newJoint = INVALID_JOINT;
                                newEnt = null;
                            }
                        }
                        if (newEnt != null) {
                            this.dragEnt.oSet(newEnt);
                            this.selected.oSet(newEnt);
                            this.joint = newJoint;
                            this.id = trace[0].c.id;
                            this.bodyName = newBodyName;

                            if (null == this.cursor) {
                                this.cursor = (idCursor3D) gameLocal.SpawnEntityType(idCursor3D.class);
                            }

                            final idPhysics phys = this.dragEnt.GetEntity().GetPhysics();
                            this.localPlayerPoint.oSet((trace[0].c.point.oMinus(viewPoint)).oMultiply(viewAxis.Transpose()));
                            origin = phys.GetOrigin(this.id);
                            axis = phys.GetAxis(this.id);
                            this.localEntityPoint.oSet((trace[0].c.point.oMinus(origin)).oMultiply(axis.Transpose()));

                            this.cursor.drag.Init(g_dragDamping.GetFloat());
                            this.cursor.drag.SetPhysics(phys, this.id, this.localEntityPoint);
                            this.cursor.Show();

                            if (phys.IsType(idPhysics_AF.class)
                                    || phys.IsType(idPhysics_RigidBody.class)
                                    || phys.IsType(idPhysics_Monster.class)) {
                                this.cursor.BecomeActive(TH_THINK);
                            }
                        }
                    }
                }
            }

            // if there is an entity selected for dragging
            final idEntity drag = this.dragEnt.GetEntity();
            if (drag != null) {

                if (0 == (player.usercmd.buttons & BUTTON_ATTACK)) {
                    StopDrag();
                    return;
                }

                this.cursor.SetOrigin(viewPoint.oPlus(this.localPlayerPoint.oMultiply(viewAxis)));
                this.cursor.SetAxis(viewAxis);

                this.cursor.drag.SetDragPosition(this.cursor.GetPhysics().GetOrigin());

                final renderEntity_s renderEntity = drag.GetRenderEntity();
                final idAnimator dragAnimator = drag.GetAnimator();

                if ((this.joint != INVALID_JOINT) && (renderEntity != null) && (dragAnimator != null)) {
                    dragAnimator.GetJointTransform(this.joint, gameLocal.time, this.cursor.draggedPosition, axis);
                    this.cursor.draggedPosition = renderEntity.origin.oPlus(this.cursor.draggedPosition.oMultiply(renderEntity.axis));
                    gameRenderWorld.DrawText(va("%s\n%s\n%s, %s", drag.GetName(), drag.GetType().getName(), dragAnimator.GetJointName(this.joint), this.bodyName), this.cursor.GetPhysics().GetOrigin(), 0.1f, colorWhite, viewAxis, 1);
                } else {
                    this.cursor.draggedPosition = this.cursor.GetPhysics().GetOrigin();
                    gameRenderWorld.DrawText(va("%s\n%s\n%s", drag.GetName(), drag.GetType().getName(), this.bodyName), this.cursor.GetPhysics().GetOrigin(), 0.1f, colorWhite, viewAxis, 1);
                }
            }

            // if there is a selected entity
            if ((this.selected.GetEntity() != null) && g_dragShowSelection.GetBool()) {
                // draw the bbox of the selected entity
                final renderEntity_s renderEntity = this.selected.GetEntity().GetRenderEntity();
                if (renderEntity != null) {
                    gameRenderWorld.DebugBox(colorYellow, new idBox(renderEntity.bounds, renderEntity.origin, renderEntity.axis));
                }
            }
        }

        public void SetSelected(idEntity ent) {
            this.selected.oSet(ent);
            StopDrag();
        }

        public idEntity GetSelected() {
            return this.selected.GetEntity();
        }

        public void DeleteSelected() {
//	delete selected.GetEntity();
            this.selected = null;
            StopDrag();
        }

        public void BindSelected() {
            int num, largestNum;
            final idLexer lexer = new idLexer();
            final idToken type = new idToken(), bodyName = new idToken();
            final idStr key = new idStr();
			idStr bindBodyName;
            String value;
            idKeyValue kv;
            idAFEntity_Base af;

            af = (idAFEntity_Base) this.dragEnt.GetEntity();

            if ((null == af) || !af.IsType(idAFEntity_Base.class) || !af.IsActiveAF()) {
                return;
            }

            bindBodyName = af.GetAFPhysics().GetBody(this.id).GetName();
            largestNum = 1;

            // parse all the bind constraints
            kv = af.spawnArgs.MatchPrefix("bindConstraint ", null);
            while (kv != null) {
                key.oSet(kv.GetKey());
                key.Strip("bindConstraint ");
                if ((num = sscanf(key, "bind%d")) != -1) {
                    if (num >= largestNum) {
                        largestNum = num + 1;
                    }
                }

                lexer.LoadMemory(kv.GetValue(), kv.GetValue().Length(), kv.GetKey());
                lexer.ReadToken(type);
                lexer.ReadToken(bodyName);
                lexer.FreeSource();

                // if there already exists a bind constraint for this body
                if (bodyName.Icmp(bindBodyName) == 0) {
                    // delete the bind constraint
                    af.spawnArgs.Delete(kv.GetKey());
                    kv = null;
                }

                kv = af.spawnArgs.MatchPrefix("bindConstraint ", kv);
            }

            key.oSet(String.format("bindConstraint bind%d", largestNum));
            value = String.format("ballAndSocket %s %s", bindBodyName.getData(), af.GetAnimator().GetJointName(this.joint));

            af.spawnArgs.Set(key, value);
            af.spawnArgs.Set("bind", "worldspawn");
            af.Bind(gameLocal.world, true);
        }

        public void UnbindSelected() {
            idKeyValue kv;
            idAFEntity_Base af;

            af = (idAFEntity_Base) this.selected.GetEntity();

            if ((null == af) || !af.IsType(idAFEntity_Base.class) || !af.IsActiveAF()) {
                return;
            }

            // unbind the selected entity
            af.Unbind();

            // delete all the bind constraints
            kv = this.selected.GetEntity().spawnArgs.MatchPrefix("bindConstraint ", null);
            while (kv != null) {
                this.selected.GetEntity().spawnArgs.Delete(kv.GetKey());
                kv = this.selected.GetEntity().spawnArgs.MatchPrefix("bindConstraint ", null);
            }

            // delete any bind information
            af.spawnArgs.Delete("bind");
            af.spawnArgs.Delete("bindToJoint");
            af.spawnArgs.Delete("bindToBody");
        }

        private void StopDrag() {
            this.dragEnt = null;
            if (this.cursor != null) {
                this.cursor.BecomeInactive(TH_THINK);
            }
        }

    }

    /*
     ===============================================================================

     Handles ingame entity editing.

     ===============================================================================
     */
    public static class selectedTypeInfo_s {

        Class/*idTypeInfo*/ typeInfo;
        idStr textKey;
    }

    public static class idEditEntities {

        private int nextSelectTime;
        private final idList<selectedTypeInfo_s> selectableEntityClasses;
        private final idList<idEntity> selectedEntities;
        //
        //

        public idEditEntities() {
            this.selectableEntityClasses = new idList<>();
            this.selectedEntities = new idList<>();
            this.nextSelectTime = 0;
        }

        public boolean SelectEntity(final idVec3 origin, final idVec3 dir, final idEntity skip) {
            idVec3 end;
            idEntity ent;

            if ((0 == g_editEntityMode.GetInteger()) || (this.selectableEntityClasses.Num() == 0)) {
                return false;
            }

            if (gameLocal.time < this.nextSelectTime) {
                return true;
            }
            this.nextSelectTime = gameLocal.time + 300;

            end = origin.oPlus(dir.oMultiply(4096.0f));

            ent = null;
            for (int i = 0; i < this.selectableEntityClasses.Num(); i++) {
                ent = gameLocal.FindTraceEntity(origin, end, this.selectableEntityClasses.oGet(i).typeInfo, skip);
                if (ent != null) {
                    break;
                }
            }
            if (ent != null) {
                ClearSelectedEntities();
                if (EntityIsSelectable(ent)) {
                    AddSelectedEntity(ent);
                    gameLocal.Printf("entity #%d: %s '%s'\n", ent.entityNumber, ent.GetClassname(), ent.name);
                    ent.ShowEditingDialog();
                    return true;
                }
            }
            return false;
        }

        public void AddSelectedEntity(idEntity ent) {
            ent.fl.selected = true;
            this.selectedEntities.AddUnique(ent);
        }

        public void RemoveSelectedEntity(idEntity ent) {
            if (this.selectedEntities.Find(ent) != Integer.valueOf(0)) {
                this.selectedEntities.Remove(ent);
            }
        }

        public void ClearSelectedEntities() {
            int i, count;

            count = this.selectedEntities.Num();
            for (i = 0; i < count; i++) {
                this.selectedEntities.oGet(i).fl.selected = false;
            }
            this.selectedEntities.Clear();
        }

        public void DisplayEntities() {
            idEntity ent;

            if (NOT(gameLocal.GetLocalPlayer())) {
                return;
            }

            this.selectableEntityClasses.Clear();
            final selectedTypeInfo_s sit = new selectedTypeInfo_s();

            switch (g_editEntityMode.GetInteger()) {
                case 1:
                    sit.typeInfo = idLight.class;
                    sit.textKey.oSet("texture");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 2:
                    sit.typeInfo = idSound.class;
                    sit.textKey.oSet("s_shader");
                    this.selectableEntityClasses.Append(sit);
                    sit.typeInfo = idLight.class;
                    sit.textKey.oSet("texture");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 3:
                    sit.typeInfo = idAFEntity_Base.class;
                    sit.textKey.oSet("articulatedFigure");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 4:
                    sit.typeInfo = idFuncEmitter.class;
                    sit.textKey.oSet("model");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 5:
                    sit.typeInfo = idAI.class;
                    sit.textKey.oSet("name");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 6:
                    sit.typeInfo = idEntity.class;
                    sit.textKey.oSet("name");
                    this.selectableEntityClasses.Append(sit);
                    break;
                case 7:
                    sit.typeInfo = idEntity.class;
                    sit.textKey.oSet("model");
                    this.selectableEntityClasses.Append(sit);
                    break;
                default:
                    return;
            }

            final idBounds viewBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            final idBounds viewTextBounds = new idBounds(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
            final idMat3 axis = gameLocal.GetLocalPlayer().viewAngles.ToMat3();

            viewBounds.ExpandSelf(512);
            viewTextBounds.ExpandSelf(128);

            idStr textKey;

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {

                final idVec4 color = new idVec4();

                textKey = new idStr("");
                if (!EntityIsSelectable(ent, color, textKey)) {
                    continue;
                }

                boolean drawArrows = false;
                if (ent.GetType() == idAFEntity_Base.class) {
                    if (!((idAFEntity_Base) ent).IsActiveAF()) {
                        continue;
                    }
                } else if (ent.GetType() == idSound.class) {
                    if (ent.fl.selected) {
                        drawArrows = true;
                    }
                    final idSoundShader ss = declManager.FindSound(ent.spawnArgs.GetString(textKey.getData()));
                    if (ss.HasDefaultSound() || (ss.base.GetState() == DS_DEFAULTED)) {
                        color.Set(1.0f, 0.0f, 1.0f, 1.0f);
                    }
                } else if (ent.GetType() == idFuncEmitter.class) {
                    if (ent.fl.selected) {
                        drawArrows = true;
                    }
                }

                if (!viewBounds.ContainsPoint(ent.GetPhysics().GetOrigin())) {
                    continue;
                }

                gameRenderWorld.DebugBounds(color, new idBounds(ent.GetPhysics().GetOrigin()).Expand(8));
                if (drawArrows) {
                    final idVec3 start = ent.GetPhysics().GetOrigin();
                    idVec3 end = start.oPlus(new idVec3(1, 0, 0).oMultiply(20.0f));
                    gameRenderWorld.DebugArrow(colorWhite, start, end, 2);
                    gameRenderWorld.DrawText("x+", end.oPlus(new idVec3(4, 0, 0)), 0.15f, colorWhite, axis);
                    end = start.oPlus(new idVec3(1, 0, 0).oMultiply(-20.0f));
                    gameRenderWorld.DebugArrow(colorWhite, start, end, 2);
                    gameRenderWorld.DrawText("x-", end.oPlus(new idVec3(-4, 0, 0)), 0.15f, colorWhite, axis);
                    end = start.oPlus(new idVec3(0, 1, 0).oMultiply(20.0f));
                    gameRenderWorld.DebugArrow(colorGreen, start, end, 2);
                    gameRenderWorld.DrawText("y+", end.oPlus(new idVec3(0, 4, 0)), 0.15f, colorWhite, axis);
                    end = start.oPlus(new idVec3(0, 1, 0).oMultiply(-20.0f));
                    gameRenderWorld.DebugArrow(colorGreen, start, end, 2);
                    gameRenderWorld.DrawText("y-", end.oPlus(new idVec3(0, -4, 0)), 0.15f, colorWhite, axis);
                    end = start.oPlus(new idVec3(0, 0, 1).oMultiply(20.0f));
                    gameRenderWorld.DebugArrow(colorBlue, start, end, 2);
                    gameRenderWorld.DrawText("z+", end.oPlus(new idVec3(0, 0, 4)), 0.15f, colorWhite, axis);
                    end = start.oPlus(new idVec3(0, 0, 1).oMultiply(-20.0f));
                    gameRenderWorld.DebugArrow(colorBlue, start, end, 2);
                    gameRenderWorld.DrawText("z-", end.oPlus(new idVec3(0, 0, -4)), 0.15f, colorWhite, axis);
                }

                if (textKey.Length() != 0) {
                    final String text = ent.spawnArgs.GetString(textKey.getData());
                    if (viewTextBounds.ContainsPoint(ent.GetPhysics().GetOrigin())) {
                        gameRenderWorld.DrawText(text, ent.GetPhysics().GetOrigin().oPlus(new idVec3(0, 0, 12)), 0.25f, colorWhite, axis, 1);
                    }
                }
            }
        }

        public boolean EntityIsSelectable(idEntity ent, idVec4 color/* = NULL*/, idStr text /*= NULL*/) {
            for (int i = 0; i < this.selectableEntityClasses.Num(); i++) {
                if (ent.GetType() == this.selectableEntityClasses.oGet(i).typeInfo) {
                    if (text != null) {
                        text.oSet(this.selectableEntityClasses.oGet(i).textKey);
                    }
                    if (color != null) {
                        if (ent.fl.selected) {
                            color.oSet(colorRed);
                        } else {
                            switch (i) {
                                case 1:
                                    color.oSet(colorYellow);
                                    break;
                                case 2:
                                    color.oSet(colorBlue);
                                    break;
                                default:
                                    color.oSet(colorGreen);
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        public boolean EntityIsSelectable(idEntity ent, idVec4 color/* = NULL*/) {
            return EntityIsSelectable(ent, color, null);
        }

        public boolean EntityIsSelectable(idEntity ent) {
            return EntityIsSelectable(ent, null);
        }
    }

    private static int sscanf(idStr key, String pattern) {
        int a = -1;
        String result;

        pattern = pattern.replaceAll("%d", "\\d+");

        Scanner scanner = new Scanner(key.getData());

        result = scanner.findInLine(Pattern.compile(pattern));
        if (isNotNullOrEmpty(result)) {
        	scanner.close();
        	scanner = new Scanner(result);
            scanner.findInLine("bind");
            a = scanner.nextInt();
        }
        scanner.close();

        return a;
    }
}
