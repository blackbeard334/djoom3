package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.itob;
import static neo.TempDump.sizeof;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.ui.Window.WIN_BORDER;
import static neo.ui.Window.WIN_INVERTRECT;
import static neo.ui.Window.WIN_NATURALMAT;
import static neo.ui.Window.WIN_NOCLIP;
import static neo.ui.Window.WIN_NOWRAP;

import neo.Renderer.Material.idMaterial;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBackground;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinFloat;
import neo.ui.Winvar.idWinRectangle;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;
import neo.ui.Winvar.idWinVec2;
import neo.ui.Winvar.idWinVec4;

/**
 *
 */
public class SimpleWindow {

    static class drawWin_t {

        idWindow win;
        idSimpleWindow simp;
        
        final int DBG_index;
        private static int DBG_counter = 0;

        public drawWin_t() {
            this.DBG_index = DBG_counter++;
        }
    }

    public static class idSimpleWindow {
//	friend class idWindow;

        protected idUserInterfaceLocal gui;
        protected idDeviceContext      dc;
        protected int                  flags;
        protected idRectangle          drawRect;          // overall rect
        protected idRectangle          clientRect;        // client area
        protected idRectangle          textRect;
        protected idVec2               origin;
        protected int                  fontNum;
        protected float                matScalex;
        protected float                matScaley;
        protected float                borderSize;
        protected int                  textAlign;
        protected float                textAlignx;
        protected float                textAligny;
        protected int                  textShadow;
        // 
        protected idWinStr             text           = new idWinStr();
        protected idWinBool            visible        = new idWinBool();
        protected idWinRectangle       rect           = new idWinRectangle();// overall rect
        protected idWinVec4            backColor      = new idWinVec4();
        protected idWinVec4            matColor       = new idWinVec4();
        protected idWinVec4            foreColor      = new idWinVec4();
        protected idWinVec4            borderColor    = new idWinVec4();
        protected idWinFloat           textScale      = new idWinFloat();
        protected idWinFloat           rotate         = new idWinFloat();
        protected idWinVec2            shear          = new idWinVec2();
        protected idWinBackground      backGroundName = new idWinBackground();
        // 
        protected idMaterial           background;
        // 	
        protected idWindow             mParent;
        // 
        protected idWinBool            hideCursor     = new idWinBool();
        //
        private static idMat3     trans = new idMat3();
        private static idVec3     org   = new idVec3();
        private static idRotation rot   = new idRotation();
        private static idVec3     vec   = new idVec3(0, 0, 1);
        private static idMat3     smat  = new idMat3();
        //
        public idStr name;
        //
        //
        private static int DBG_countersOfCreation = 0;
        private final  int DBG_count              = DBG_countersOfCreation++;

        public static int DBG_idSimpleWindow = 0;

        public idSimpleWindow(idWindow win) {
            this.gui = win.GetGui();
            this.dc = win.dc;
            this.drawRect = new idRectangle(win.drawRect);
            this.clientRect = new idRectangle(win.clientRect);
            this.textRect = new idRectangle(win.textRect);
            this.origin = new idVec2(win.origin);
            this.fontNum = win.fontNum;
            this.name = new idStr(win.name);
            this.matScalex = win.matScalex;
            this.matScaley = win.matScaley;
            this.borderSize = win.borderSize;
            this.textAlign = win.textAlign;
            this.textAlignx = win.textAlignx;
            this.textAligny = win.textAligny;
            this.background = win.background;
            this.flags = win.flags;
            this.textShadow = win.textShadow;

            this.text.oSet(win.text);
            this.visible.oSet(win.visible);
            this.rect.oSet(win.rect);
            this.backColor.oSet(win.backColor);
            this.matColor.oSet(win.matColor);
            this.foreColor.oSet(win.foreColor);
            this.borderColor.oSet(win.borderColor);
            this.textScale.oSet(win.textScale);
            this.rotate.oSet(win.rotate);
            this.shear.oSet(win.shear);
            this.backGroundName.oSet(win.backGroundName);
            if (this.backGroundName.Length() != 0) {
                this.background = declManager.FindMaterial(this.backGroundName.data);
                this.background.SetSort(SS_GUI);
                this.background.SetImageClassifications(1);    // just for resource tracking
            }
            this.backGroundName.SetMaterialPtr(this.background);

            // 
            //  added parent
            this.mParent = win.GetParent();
            // 

            this.hideCursor.oSet(win.hideCursor);

            final idWindow parent = win.GetParent();
            if (parent != null) {
                if (this.text.NeedsUpdate()) {
                    DBG_idSimpleWindow++;
//                    if(DBG_idSimpleWindow++==26)
//                    if(DBG_idSimpleWindow++==27)
                    parent.AddUpdateVar(this.text);
//                    System.out.println(">>" + this);
                }
                if (this.visible.NeedsUpdate()) {
                    parent.AddUpdateVar(this.visible);
                }
                if (this.rect.NeedsUpdate()) {
                    parent.AddUpdateVar(this.rect);
                }
                if (this.backColor.NeedsUpdate()) {
                    parent.AddUpdateVar(this.backColor);
                }
                if (this.matColor.NeedsUpdate()) {
                    parent.AddUpdateVar(this.matColor);
                }
                if (this.foreColor.NeedsUpdate()) {
                    parent.AddUpdateVar(this.foreColor);
                }
                if (this.borderColor.NeedsUpdate()) {
                    parent.AddUpdateVar(this.borderColor);
                }
                if (this.textScale.NeedsUpdate()) {
                    parent.AddUpdateVar(this.textScale);
                }
                if (this.rotate.NeedsUpdate()) {
                    parent.AddUpdateVar(this.rotate);
                }
                if (this.shear.NeedsUpdate()) {
                    parent.AddUpdateVar(this.shear);
                }
                if (this.backGroundName.NeedsUpdate()) {
                    parent.AddUpdateVar(this.backGroundName);
                }
            }
        }
//	virtual			~idSimpleWindow();

        public void Redraw(float x, float y) {
            if (!this.visible.data) {
                return;
            }

            CalcClientRect(0, 0);
            this.dc.SetFont(this.fontNum);
            this.drawRect.Offset(x, y);
            this.clientRect.Offset(x, y);
            this.textRect.Offset(x, y);
            SetupTransforms(x, y);
            if ((this.flags & WIN_NOCLIP) != 0) {
                this.dc.EnableClipping(false);
            }
            DrawBackground(this.drawRect);
            DrawBorderAndCaption(this.drawRect);
            if (this.textShadow != 0) {
                final idStr shadowText = this.text.data;
                final idRectangle shadowRect = new idRectangle(this.textRect);

                shadowText.RemoveColors();
                shadowRect.x += this.textShadow;
                shadowRect.y += this.textShadow;

                this.dc.DrawText(shadowText, this.textScale.data, this.textAlign, colorBlack, shadowRect, !itob(this.flags & WIN_NOWRAP), -1);
            }
            this.dc.DrawText(this.text.data, this.textScale.data, this.textAlign, this.foreColor.data, this.textRect, !itob(this.flags & WIN_NOWRAP), -1);
            this.dc.SetTransformInfo(getVec3_origin(), getMat3_identity());
            if ((this.flags & WIN_NOCLIP) != 0) {
                this.dc.EnableClipping(true);
            }
            this.drawRect.Offset(-x, -y);
            this.clientRect.Offset(-x, -y);
            this.textRect.Offset(-x, -y);
        }

        public void StateChanged(boolean redraw) {
            if (redraw && (this.background != null) && (this.background.CinematicLength() != 0)) {
                this.background.UpdateCinematic(this.gui.GetTime());
            }
        }

        public idWinVar GetWinVarByName(final String _name) {
            idWinVar retVar = null;
            if (idStr.Icmp(_name, "background") == 0) {//TODO:should this be a switch?
                retVar = this.backGroundName;
            }
            if (idStr.Icmp(_name, "visible") == 0) {
                retVar = this.visible;
            }
            if (idStr.Icmp(_name, "rect") == 0) {
                retVar = this.rect;
            }
            if (idStr.Icmp(_name, "backColor") == 0) {
                retVar = this.backColor;
            }
            if (idStr.Icmp(_name, "matColor") == 0) {
                retVar = this.matColor;
            }
            if (idStr.Icmp(_name, "foreColor") == 0) {
                retVar = this.foreColor;
            }
            if (idStr.Icmp(_name, "borderColor") == 0) {
                retVar = this.borderColor;
            }
            if (idStr.Icmp(_name, "textScale") == 0) {
                retVar = this.textScale;
            }
            if (idStr.Icmp(_name, "rotate") == 0) {
                retVar = this.rotate;
            }
            if (idStr.Icmp(_name, "shear") == 0) {
                retVar = this.shear;
            }
            if (idStr.Icmp(_name, "text") == 0) {
                retVar = this.text;
            }
            return retVar;
        }

        public int GetWinVarOffset(idWinVar wv, drawWin_t owner) {
            final int ret = -1;

//	if ( wv == &rect ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->rect;
//	}
//
//	if ( wv == &backColor ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->backColor;
//	}
//
//	if ( wv == &matColor ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->matColor;
//	}
//
//	if ( wv == &foreColor ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->foreColor;
//	}
//
//	if ( wv == &borderColor ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->borderColor;
//	}
//
//	if ( wv == &textScale ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->textScale;
//	}
//
//	if ( wv == &rotate ) {
//		ret = (int)&( ( idSimpleWindow * ) 0 )->rotate;
//	}
//
//	if ( ret != -1 ) {
//		owner->simp = this;
//	}
            return ret;
        }

        public int/*size_t*/ Size() {
            int sz = sizeof(this);
            sz += this.name.Size();
            sz += this.text.Size();
            sz += this.backGroundName.Size();
            return sz;
        }

        public idWindow GetParent() {
            return this.mParent;
        }

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteInt(this.flags);
            savefile.Write(this.drawRect);
            savefile.Write(this.clientRect);
            savefile.Write(this.textRect);
            savefile.Write(this.origin);
            savefile.WriteInt(this.fontNum);
            savefile.WriteFloat(this.matScalex);
            savefile.WriteFloat(this.matScaley);
            savefile.WriteFloat(this.borderSize);
            savefile.WriteInt(this.textAlign);
            savefile.WriteFloat(this.textAlignx);
            savefile.WriteFloat(this.textAligny);
            savefile.WriteInt(this.textShadow);

            this.text.WriteToSaveGame(savefile);
            this.visible.WriteToSaveGame(savefile);
            this.rect.WriteToSaveGame(savefile);
            this.backColor.WriteToSaveGame(savefile);
            this.matColor.WriteToSaveGame(savefile);
            this.foreColor.WriteToSaveGame(savefile);
            this.borderColor.WriteToSaveGame(savefile);
            this.textScale.WriteToSaveGame(savefile);
            this.rotate.WriteToSaveGame(savefile);
            this.shear.WriteToSaveGame(savefile);
            this.backGroundName.WriteToSaveGame(savefile);

            int stringLen;

            if (this.background != null) {
                stringLen = this.background.GetName().length();
                savefile.WriteInt(stringLen);
                savefile.WriteString(this.background.GetName());
            } else {
                stringLen = 0;
                savefile.WriteInt(stringLen);
            }

        }

        public void ReadFromSaveGame(idFile savefile) {

            this.flags = savefile.ReadInt();
            savefile.Read(this.drawRect);
            savefile.Read(this.clientRect);
            savefile.Read(this.textRect);
            savefile.Read(this.origin);
            this.fontNum = savefile.ReadInt();
            this.matScalex = savefile.ReadFloat();
            this.matScaley = savefile.ReadFloat();
            this.borderSize = savefile.ReadFloat();
            this.textAlign = savefile.ReadInt();
            this.textAlignx = savefile.ReadFloat();
            this.textAligny = savefile.ReadFloat();
            this.textShadow = savefile.ReadInt();

            this.text.ReadFromSaveGame(savefile);
            this.visible.ReadFromSaveGame(savefile);
            this.rect.ReadFromSaveGame(savefile);
            this.backColor.ReadFromSaveGame(savefile);
            this.matColor.ReadFromSaveGame(savefile);
            this.foreColor.ReadFromSaveGame(savefile);
            this.borderColor.ReadFromSaveGame(savefile);
            this.textScale.ReadFromSaveGame(savefile);
            this.rotate.ReadFromSaveGame(savefile);
            this.shear.ReadFromSaveGame(savefile);
            this.backGroundName.ReadFromSaveGame(savefile);

            int stringLen;

            stringLen = savefile.ReadInt();
            if (stringLen > 0) {
                final idStr backName = new idStr();

                backName.Fill(' ', stringLen);
                savefile.ReadString(backName);

                this.background = declManager.FindMaterial(backName);
                this.background.SetSort(SS_GUI);
            } else {
                this.background = null;
            }

        }

        protected void CalcClientRect(float xofs, float yofs) {

            this.drawRect.oSet(this.rect.data);

            if ((this.flags & WIN_INVERTRECT) != 0) {
                this.drawRect.x = this.rect.x() - this.rect.w();
                this.drawRect.y = this.rect.y() - this.rect.h();
            }

            this.drawRect.x += xofs;
            this.drawRect.y += yofs;

            this.clientRect.oSet(this.drawRect);
            if ((this.rect.h() > 0.0) && (this.rect.w() > 0.0)) {

                if (((this.flags & WIN_BORDER) != 0) && (this.borderSize != 0)) {
                    this.clientRect.x += this.borderSize;
                    this.clientRect.y += this.borderSize;
                    this.clientRect.w -= this.borderSize;
                    this.clientRect.h -= this.borderSize;
                }

                this.textRect.oSet(this.clientRect);
                this.textRect.x += 2.0;
                this.textRect.w -= 2.0;
                this.textRect.y += 2.0;
                this.textRect.h -= 2.0;
                this.textRect.x += this.textAlignx;
                this.textRect.y += this.textAligny;

            }
            this.origin.Set(this.rect.x() + (this.rect.w() / 2), this.rect.y() + (this.rect.h() / 2));
        }

        protected void SetupTransforms(float x, float y) {

            trans.Identity();
            org.Set(this.origin.x + x, this.origin.y + y, 0);
            if ((this.rotate != null) && (this.rotate.data != 0)) {

                rot.Set(org, vec, this.rotate.data);
                trans = rot.ToMat3();
            }

            smat.Identity();
            if ((this.shear.x() != 0) || (this.shear.y() != 0)) {
                smat.oSet(0, 1, this.shear.x());
                smat.oSet(1, 0, this.shear.y());
                trans.oMulSet(smat);
            }

            if (!trans.IsIdentity()) {
                this.dc.SetTransformInfo(org, trans);
            }
        }

        protected void DrawBackground(final idRectangle drawRect) {
            if (this.backColor.w() > 0) {
                this.dc.DrawFilledRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, this.backColor.data);
            }

            if (this.background != null) {
                if (this.matColor.w() > 0) {
                    float scaleX, scaleY;
                    if ((this.flags & WIN_NATURALMAT) != 0) {
                        scaleX = drawRect.w / this.background.GetImageWidth();
                        scaleY = drawRect.h / this.background.GetImageHeight();
                    } else {
                        scaleX = this.matScalex;
                        scaleY = this.matScaley;
                    }
                    this.dc.DrawMaterial(drawRect.x, drawRect.y, drawRect.w, drawRect.h, this.background, this.matColor.data, scaleX, scaleY);
                }
            }
        }

        protected void DrawBorderAndCaption(final idRectangle drawRect) {
            if ((this.flags & WIN_BORDER) != 0) {
                if (this.borderSize != 0) {
                    this.dc.DrawRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, this.borderSize, this.borderColor.data);
                }
            }
        }
    }
}
