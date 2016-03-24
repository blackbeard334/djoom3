package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import static neo.TempDump.itob;
import static neo.TempDump.sizeof;
import static neo.framework.DeclManager.declManager;
import neo.framework.File_h.idFile;
import static neo.idlib.Lib.colorBlack;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import static neo.ui.Window.WIN_BORDER;
import static neo.ui.Window.WIN_INVERTRECT;
import static neo.ui.Window.WIN_NATURALMAT;
import static neo.ui.Window.WIN_NOCLIP;
import static neo.ui.Window.WIN_NOWRAP;
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
    };

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
        protected idWinStr             text;
        protected idWinBool            visible;
        protected idWinRectangle       rect;              // overall rect
        protected idWinVec4            backColor;
        protected idWinVec4            matColor;
        protected idWinVec4            foreColor;
        protected idWinVec4            borderColor;
        protected idWinFloat           textScale;
        protected idWinFloat           rotate;
        protected idWinVec2            shear;
        protected idWinBackground      backGroundName;
        // 
        protected idMaterial           background;
        // 	
        protected idWindow             mParent;
        // 
        protected idWinBool            hideCursor;
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
        private        int DBG_count              = 0;

        public static int DBG_idSimpleWindow = 0;
        public idSimpleWindow(idWindow win) {
            DBG_count = DBG_countersOfCreation++;
            gui = win.GetGui();
            dc = win.dc;
            drawRect = new idRectangle(win.drawRect);
            clientRect = new idRectangle(win.clientRect);
            textRect = new idRectangle(win.textRect);
            origin = new idVec2(win.origin);
            fontNum = win.fontNum;
            name = new idStr(win.name);
            matScalex = win.matScalex;
            matScaley = win.matScaley;
            borderSize = win.borderSize;
            textAlign = win.textAlign;
            textAlignx = win.textAlignx;
            textAligny = win.textAligny;
            background = win.background;
            flags = win.flags;
            textShadow = win.textShadow;

            text = new idWinStr(win.text);
            visible = new idWinBool(win.visible);
            rect = new idWinRectangle(win.rect);
            backColor = new idWinVec4(win.backColor);
            matColor = new idWinVec4(win.matColor);
            foreColor = new idWinVec4(win.foreColor);
            borderColor = new idWinVec4(win.borderColor);
            textScale = new idWinFloat(win.textScale);
            rotate = new idWinFloat(win.rotate);
            shear = new idWinVec2(win.shear);
            backGroundName = new idWinBackground(win.backGroundName);
            if (backGroundName.Length() != 0) {
                background = declManager.FindMaterial(backGroundName.data);
                background.SetSort(SS_GUI);
                background.SetImageClassifications(1);	// just for resource tracking
            }
            backGroundName.SetMaterialPtr(background);

            // 
            //  added parent
            mParent = win.GetParent();
            // 

            hideCursor = new idWinBool(win.hideCursor);

            idWindow parent = win.GetParent();
            if (parent != null) {
                if (text.NeedsUpdate()) {
                    DBG_idSimpleWindow++;
//                    if(DBG_idSimpleWindow++==26)
//                    if(DBG_idSimpleWindow++==27)
                    parent.AddUpdateVar(text);
//                    System.out.println(">>" + this);
                }
                if (visible.NeedsUpdate()) {
                    parent.AddUpdateVar(visible);
                }
                if (rect.NeedsUpdate()) {
                    parent.AddUpdateVar(rect);
                }
                if (backColor.NeedsUpdate()) {
                    parent.AddUpdateVar(backColor);
                }
                if (matColor.NeedsUpdate()) {
                    parent.AddUpdateVar(matColor);
                }
                if (foreColor.NeedsUpdate()) {
                    parent.AddUpdateVar(foreColor);
                }
                if (borderColor.NeedsUpdate()) {
                    parent.AddUpdateVar(borderColor);
                }
                if (textScale.NeedsUpdate()) {
                    parent.AddUpdateVar(textScale);
                }
                if (rotate.NeedsUpdate()) {
                    parent.AddUpdateVar(rotate);
                }
                if (shear.NeedsUpdate()) {
                    parent.AddUpdateVar(shear);
                }
                if (backGroundName.NeedsUpdate()) {
                    parent.AddUpdateVar(backGroundName);
                }
            }
        }
//	virtual			~idSimpleWindow();

        public void Redraw(float x, float y) {
            if (!visible.data) {
                return;
            }

            CalcClientRect(0, 0);
            dc.SetFont(fontNum);
            drawRect.Offset(x, y);
            clientRect.Offset(x, y);
            textRect.Offset(x, y);
            SetupTransforms(x, y);
            if ((flags & WIN_NOCLIP) != 0) {
                dc.EnableClipping(false);
            }
            DrawBackground(drawRect);
            DrawBorderAndCaption(drawRect);
            if (textShadow != 0) {
                idStr shadowText = text.data;
                idRectangle shadowRect = new idRectangle(textRect);

                shadowText.RemoveColors();
                shadowRect.x += textShadow;
                shadowRect.y += textShadow;

                dc.DrawText(shadowText, textScale.data, textAlign, colorBlack, shadowRect, !itob(flags & WIN_NOWRAP), -1);
            }
            dc.DrawText(text.data, textScale.data, textAlign, foreColor.data, textRect, !itob(flags & WIN_NOWRAP), -1);
            dc.SetTransformInfo(getVec3_origin(), getMat3_identity());
            if ((flags & WIN_NOCLIP) != 0) {
                dc.EnableClipping(true);
            }
            drawRect.Offset(-x, -y);
            clientRect.Offset(-x, -y);
            textRect.Offset(-x, -y);
        }

        public void StateChanged(boolean redraw) {
            if (redraw && background != null && background.CinematicLength() != 0) {
                background.UpdateCinematic(gui.GetTime());
            }
        }

        public idWinVar GetWinVarByName(final String _name) {
            idWinVar retVar = null;
            if (idStr.Icmp(_name, "background") == 0) {//TODO:should this be a switch?
                retVar = backGroundName;
            }
            if (idStr.Icmp(_name, "visible") == 0) {
                retVar = visible;
            }
            if (idStr.Icmp(_name, "rect") == 0) {
                retVar = rect;
            }
            if (idStr.Icmp(_name, "backColor") == 0) {
                retVar = backColor;
            }
            if (idStr.Icmp(_name, "matColor") == 0) {
                retVar = matColor;
            }
            if (idStr.Icmp(_name, "foreColor") == 0) {
                retVar = foreColor;
            }
            if (idStr.Icmp(_name, "borderColor") == 0) {
                retVar = borderColor;
            }
            if (idStr.Icmp(_name, "textScale") == 0) {
                retVar = textScale;
            }
            if (idStr.Icmp(_name, "rotate") == 0) {
                retVar = rotate;
            }
            if (idStr.Icmp(_name, "shear") == 0) {
                retVar = shear;
            }
            if (idStr.Icmp(_name, "text") == 0) {
                retVar = text;
            }
            return retVar;
        }

        public int GetWinVarOffset(idWinVar wv, drawWin_t owner) {
            int ret = -1;

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
            sz += name.Size();
            sz += text.Size();
            sz += backGroundName.Size();
            return sz;
        }

        public idWindow GetParent() {
            return mParent;
        }

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteInt(flags);
            savefile.Write(drawRect);
            savefile.Write(clientRect);
            savefile.Write(textRect);
            savefile.Write(origin);
            savefile.WriteInt(fontNum);
            savefile.WriteFloat(matScalex);
            savefile.WriteFloat(matScaley);
            savefile.WriteFloat(borderSize);
            savefile.WriteInt(textAlign);
            savefile.WriteFloat(textAlignx);
            savefile.WriteFloat(textAligny);
            savefile.WriteInt(textShadow);

            text.WriteToSaveGame(savefile);
            visible.WriteToSaveGame(savefile);
            rect.WriteToSaveGame(savefile);
            backColor.WriteToSaveGame(savefile);
            matColor.WriteToSaveGame(savefile);
            foreColor.WriteToSaveGame(savefile);
            borderColor.WriteToSaveGame(savefile);
            textScale.WriteToSaveGame(savefile);
            rotate.WriteToSaveGame(savefile);
            shear.WriteToSaveGame(savefile);
            backGroundName.WriteToSaveGame(savefile);

            int stringLen;

            if (background != null) {
                stringLen = background.GetName().length();
                savefile.WriteInt(stringLen);
                savefile.WriteString(background.GetName());
            } else {
                stringLen = 0;
                savefile.WriteInt(stringLen);
            }

        }

        public void ReadFromSaveGame(idFile savefile) {

            flags = savefile.ReadInt();
            savefile.Read(drawRect);
            savefile.Read(clientRect);
            savefile.Read(textRect);
            savefile.Read(origin);
            fontNum = savefile.ReadInt();
            matScalex = savefile.ReadFloat();
            matScaley = savefile.ReadFloat();
            borderSize = savefile.ReadFloat();
            textAlign = savefile.ReadInt();
            textAlignx = savefile.ReadFloat();
            textAligny = savefile.ReadFloat();
            textShadow = savefile.ReadInt();

            text.ReadFromSaveGame(savefile);
            visible.ReadFromSaveGame(savefile);
            rect.ReadFromSaveGame(savefile);
            backColor.ReadFromSaveGame(savefile);
            matColor.ReadFromSaveGame(savefile);
            foreColor.ReadFromSaveGame(savefile);
            borderColor.ReadFromSaveGame(savefile);
            textScale.ReadFromSaveGame(savefile);
            rotate.ReadFromSaveGame(savefile);
            shear.ReadFromSaveGame(savefile);
            backGroundName.ReadFromSaveGame(savefile);

            int stringLen;

            stringLen = savefile.ReadInt();
            if (stringLen > 0) {
                idStr backName = new idStr();

                backName.Fill(' ', stringLen);
                savefile.ReadString(backName);

                background = declManager.FindMaterial(backName);
                background.SetSort(SS_GUI);
            } else {
                background = null;
            }

        }

        protected void CalcClientRect(float xofs, float yofs) {

            drawRect.oSet(rect.data);

            if ((flags & WIN_INVERTRECT) != 0) {
                drawRect.x = rect.x() - rect.w();
                drawRect.y = rect.y() - rect.h();
            }

            drawRect.x += xofs;
            drawRect.y += yofs;

            clientRect.oSet(drawRect);
            if (rect.h() > 0.0 && rect.w() > 0.0) {

                if (((flags & WIN_BORDER) != 0) && borderSize != 0) {
                    clientRect.x += borderSize;
                    clientRect.y += borderSize;
                    clientRect.w -= borderSize;
                    clientRect.h -= borderSize;
                }

                textRect.oSet(clientRect);
                textRect.x += 2.0;
                textRect.w -= 2.0;
                textRect.y += 2.0;
                textRect.h -= 2.0;
                textRect.x += textAlignx;
                textRect.y += textAligny;

            }
            origin.Set(rect.x() + (rect.w() / 2), rect.y() + (rect.h() / 2));
        }

        protected void SetupTransforms(float x, float y) {

            trans.Identity();
            org.Set(origin.x + x, origin.y + y, 0);
            if (rotate != null && rotate.data != 0) {

                rot.Set(org, vec, rotate.data);
                trans = rot.ToMat3();
            }

            smat.Identity();
            if (shear.x() != 0 || shear.y() != 0) {
                smat.oSet(0, 1, shear.x());
                smat.oSet(1, 0, shear.y());
                trans.oMulSet(smat);
            }

            if (!trans.IsIdentity()) {
                dc.SetTransformInfo(org, trans);
            }
        }

        protected void DrawBackground(final idRectangle drawRect) {
            if (backColor.w() > 0) {
                dc.DrawFilledRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, backColor.data);
            }

            if (background != null) {
                if (matColor.w() > 0) {
                    float scaleX, scaleY;
                    if ((flags & WIN_NATURALMAT) != 0) {
                        scaleX = drawRect.w / background.GetImageWidth();
                        scaleY = drawRect.h / background.GetImageHeight();
                    } else {
                        scaleX = matScalex;
                        scaleY = matScaley;
                    }
                    dc.DrawMaterial(drawRect.x, drawRect.y, drawRect.w, drawRect.h, background, matColor.data, scaleX, scaleY);
                }
            }
        }

        protected void DrawBorderAndCaption(final idRectangle drawRect) {
            if ((flags & WIN_BORDER) != 0) {
                if (borderSize != 0) {
                    dc.DrawRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, borderSize, borderColor.data);
                }
            }
        }
    };
}
