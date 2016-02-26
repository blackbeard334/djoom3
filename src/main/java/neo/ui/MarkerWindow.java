package neo.ui;

import java.util.Arrays;
import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import static neo.TempDump.itob;
import static neo.TempDump.wrapToNativeBuffer;
import static neo.framework.DeclManager.declManager;
import neo.framework.FileSystem_h.idFileList;
import neo.framework.File_h.idFile;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_SPACE;
import static neo.framework.Session.MAX_LOGGED_STATS;
import neo.framework.Session.logStats_t;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Lib.idLib.fileSystem;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.MarkerWindow.markerData_t;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;

/**
 *
 */
public class MarkerWindow {

    public static class markerData_t {

        int time;
        idMaterial mat;
        idRectangle rect;
    };

    public static class idMarkerWindow extends idWindow {

        private final logStats_t[] loggedStats = new logStats_t[MAX_LOGGED_STATS];
        private idList<markerData_t> markerTimes;
        private idStr                statData;
        private int                  numStats;
        private int/*dword*/[]       imageBuff;
        private idMaterial           markerMat;
        private idMaterial           markerStop;
        private idVec4               markerColor;
        private int                  currentMarker;
        private int                  currentTime;
        private int                  stopTime;
        //
        //

        public idMarkerWindow(idUserInterfaceLocal gui) {
            super();
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }

        public idMarkerWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super();
            this.dc = dc;
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }
//virtual ~idMarkerWindow();

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }
//
//        @Override
//        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/) {
//            return super.GetWinVarByName(_name, winLookup);
//        }
//

        public String HandleEvent(final sysEvent_s event, boolean updateVisuals) {

            if (!(event.evType == SE_KEY && event.evValue2 != 0)) {
                return "";
            }

            int key = event.evValue;
            if (event.evValue2 != 0 && key == K_MOUSE1) {
                gui.GetDesktop().SetChildWinVarVal("markerText", "text", "");
                idRectangle r;
                int c = markerTimes.Num();
                int i;
                for (i = 0; i < c; i++) {
                    markerData_t md = markerTimes.oGet(i);
                    if (md.rect.Contains(gui.CursorX(), gui.CursorY())) {
                        currentMarker = i;
                        gui.SetStateInt("currentMarker", md.time);
                        stopTime = md.time;
                        gui.GetDesktop().SetChildWinVarVal("markerText", "text", va("Marker set at %.2i:%.2i", md.time / 60 / 60, (md.time / 60) % 60));
                        gui.GetDesktop().SetChildWinVarVal("markerText", "visible", "1");
                        gui.GetDesktop().SetChildWinVarVal("markerBackground", "matcolor", "1 1 1 1");
                        gui.GetDesktop().SetChildWinVarVal("markerBackground", "text", "");
                        gui.GetDesktop().SetChildWinVarVal("markerBackground", "background", md.mat.GetName());
                        break;
                    }
                }
                if (i == c) {
                    // no marker selected;
                    currentMarker = -1;
                    gui.SetStateInt("currentMarker", currentTime);
                    stopTime = currentTime;
                    gui.GetDesktop().SetChildWinVarVal("markerText", "text", va("Marker set at %.2i:%.2i", currentTime / 60 / 60, (currentTime / 60) % 60));
                    gui.GetDesktop().SetChildWinVarVal("markerText", "visible", "1");
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "matcolor", "0 0 0 0");
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "text", "No Preview");
                }
                float pct = gui.State().GetFloat("loadPct");
                int len = gui.State().GetInt("loadLength");
                if (stopTime > len * pct) {
                    return "cmdDemoGotoMarker";
                }
            } else if (key == K_MOUSE2) {
                stopTime = -1;
                gui.GetDesktop().SetChildWinVarVal("markerText", "text", "");
                gui.SetStateInt("currentMarker", -1);
                return "cmdDemoGotoMarker";
            } else if (key == K_SPACE) {
                return "cmdDemoPauseFrame";
            }

            return "";
        }

        @Override
        public void PostParse() {
            super.PostParse();
        }

        static final int HEALTH_MAX  = 100;
        static final int COMBAT_MAX  = 100;
        static final int RATE_MAX    = 125;
        static final int STAMINA_MAX = 12;

        @Override
        public void Draw(int time, float x, float y) {
            float pct;
            idRectangle r = clientRect;
            int len = gui.State().GetInt("loadLength");
            if (len == 0) {
                len = 1;
            }
            if (numStats > 1) {
                int c = markerTimes.Num();
                if (c > 0) {
                    for (int i = 0; i < c; i++) {
                        markerData_t md = markerTimes.oGet(i);
                        if (md.rect.w == 0) {
                            md.rect.x = (r.x + r.w * ((float) md.time / len)) - 8;
                            md.rect.y = r.y + r.h - 20;
                            md.rect.w = 16;
                            md.rect.h = 16;
                        }
                        dc.DrawMaterial(md.rect.x, md.rect.y, md.rect.w, md.rect.h, markerMat, markerColor);
                    }
                }
            }

            r.y += 10;
            if (r.w > 0 && r.Contains(gui.CursorX(), gui.CursorY())) {
                pct = (gui.CursorX() - r.x) / r.w;
                currentTime = (int) (len * pct);
                r.x = (gui.CursorX() > r.x + r.w - 40) ? gui.CursorX() - 40 : gui.CursorX();
                r.y = gui.CursorY() - 15;
                r.w = 40;
                r.h = 20;
                dc.DrawText(va("%.2i:%.2i", currentTime / 60 / 60, (currentTime / 60) % 60), 0.25f, 0, idDeviceContext.colorWhite, r, false);
            }

            if (stopTime >= 0 && markerStop != null) {
                r = clientRect;
                r.y += (r.h - 32) / 2;
                pct = (float) stopTime / len;
                r.x += (r.w * pct) - 16;
                idVec4 color = new idVec4(1, 1, 1, 0.65f);
                dc.DrawMaterial(r.x, r.y, 32, 32, markerStop, color);
            }

        }

        @Override
        public String RouteMouseCoords(float xd, float yd) {
            final String ret = super.RouteMouseCoords(xd, yd);
            idRectangle r;
            int i, c = markerTimes.Num();
            int len = gui.State().GetInt("loadLength");
            if (len == 0) {
                len = 1;
            }
            for (i = 0; i < c; i++) {
                markerData_t md = markerTimes.oGet(i);
                if (md.rect.Contains(gui.CursorY(), gui.CursorX())) {
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "background", md.mat.GetName());
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "matcolor", "1 1 1 1");
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "text", "");
                    break;
                }
            }

            if (i >= c) {
                if (currentMarker == -1) {
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "matcolor", "0 0 0 0");
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "text", "No Preview");
                } else {
                    markerData_t md = markerTimes.oGet(currentMarker);
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "background", md.mat.GetName());
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "matcolor", "1 1 1 1");
                    gui.GetDesktop().SetChildWinVarVal("markerBackground", "text", "");
                }
            }
            return ret;
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            if (activate) {
                int i;
                gui.GetDesktop().SetChildWinVarVal("markerText", "text", "");
                imageBuff = new int[512 * 64 * 4];// Mem_Alloc(512 * 64 * 4);
                markerTimes.Clear();
                currentMarker = -1;
                currentTime = -1;
                stopTime = -1;
                statData.oSet(gui.State().GetString("statData"));
                numStats = 0;
                if (statData.Length() != 0) {
                    idFile file = fileSystem.OpenFileRead(statData.toString());
                    if (file != null) {
                        numStats = file.ReadInt();
//                        file->Read(loggedStats, numStats * sizeof(loggedStats[0]));
                        for (i = 0; i < numStats; i++) {
                            file.Read(loggedStats[i]);
                            if (loggedStats[i].health < 0) {
                                loggedStats[i].health = 0;
                            }
                            if (loggedStats[i].stamina < 0) {
                                loggedStats[i].stamina = 0;
                            }
                            if (loggedStats[i].heartRate < 0) {
                                loggedStats[i].heartRate = 0;
                            }
                            if (loggedStats[i].combat < 0) {
                                loggedStats[i].combat = 0;
                            }
                        }
                        fileSystem.CloseFile(file);
                    }
                }

                if (numStats > 1 && background != null) {
                    idStr markerPath = statData;
                    markerPath.StripFilename();
                    idFileList markers;
                    markers = fileSystem.ListFiles(markerPath.toString(), ".tga", false, true);
                    idStr name;
                    for (i = 0; i < markers.GetNumFiles(); i++) {
                        name = new idStr(markers.GetFile(i));
                        markerData_t md = new markerData_t();
                        md.mat = declManager.FindMaterial(name);
                        md.mat.SetSort(SS_GUI);
                        name.StripPath();
                        name.StripFileExtension();
                        md.time = Integer.parseInt(name.toString());
                        markerTimes.Append(md);
                    }
                    fileSystem.FreeFileList(markers);
//                    memset(imageBuff, 0, 512 * 64 * 4);
                    Arrays.fill(imageBuff, 0, 512 * 64 * 4, 0);
                    float step = 511.0f / (numStats - 1);
                    float startX = 0;
                    float x1, y1, x2, y2;
                    x1 = 0 - step;
                    for (i = 0; i < numStats - 1; i++) {
                        x1 += step;
                        x2 = x1 + step;
                        y1 = 63 * ((float) loggedStats[i].health / HEALTH_MAX);
                        y2 = 63 * ((float) loggedStats[i + 1].health / HEALTH_MAX);
                        Line(x1, y1, x2, y2, imageBuff, 0xff0000ff);
                        y1 = 63 * ((float) loggedStats[i].heartRate / RATE_MAX);
                        y2 = 63 * ((float) loggedStats[i + 1].heartRate / RATE_MAX);
                        Line(x1, y1, x2, y2, imageBuff, 0xff00ff00);
                        // stamina not quite as high on graph so health does not get obscured with both at 100%
                        y1 = 62 * ((float) loggedStats[i].stamina / STAMINA_MAX);
                        y2 = 62 * ((float) loggedStats[i + 1].stamina / STAMINA_MAX);
                        Line(x1, y1, x2, y2, imageBuff, 0xffff0000);
                        y1 = 63 * ((float) loggedStats[i].combat / COMBAT_MAX);
                        y2 = 63 * ((float) loggedStats[i + 1].combat / COMBAT_MAX);
                        Line(x1, y1, x2, y2, imageBuff, 0xff00ffff);
                    }
                    final shaderStage_t stage = background.GetStage(0);
                    if (stage != null) {//TODO: check the wrapToNativeBuffer below.
                        stage.texture.image[0].UploadScratch(wrapToNativeBuffer(itob(imageBuff)), 512, 64);
                    }
//                    Mem_Free(imageBuff);
                    imageBuff = null;
                }
            }
        }

        @Override
        public void MouseExit() {
            super.MouseExit();
        }

        @Override
        public void MouseEnter() {
            super.MouseEnter();
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "markerMat") == 0) {
                idStr str = new idStr();
                ParseString(src, str);
                markerMat = declManager.FindMaterial(str);
                markerMat.SetSort(SS_GUI);
                return true;
            }
            if (idStr.Icmp(_name, "markerStop") == 0) {
                idStr str = new idStr();
                ParseString(src, str);
                markerStop = declManager.FindMaterial(str);
                markerStop.SetSort(SS_GUI);
                return true;
            }
            if (idStr.Icmp(_name, "markerColor") == 0) {
                ParseVec4(src, markerColor);
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        @Override
        public void CommonInit() {
            numStats = 0;
            currentTime = -1;
            currentMarker = -1;
            stopTime = -1;
            imageBuff = null;
            markerMat = null;
            markerStop = null;
        }

        private void Line(int x1, int y1, int x2, int y2, int[] out, int color) {
            int deltax = Math.abs(x2 - x1);
            int deltay = Math.abs(y2 - y1);
            int incx = (x1 > x2) ? -1 : 1;
            int incy = (y1 > y2) ? -1 : 1;
            int right, up, dir;
            if (deltax > deltay) {
                right = deltay * 2;
                up = right - deltax * 2;
                dir = right - deltax;
                while (deltax-- >= 0) {
                    Point(x1, y1, out, color);
                    x1 += incx;
                    y1 += (dir > 0) ? incy : 0;
                    dir += (dir > 0) ? up : right;
                }
            } else {
                right = deltax * 2;
                up = right - deltay * 2;
                dir = right - deltay;
                while (deltay-- >= 0) {
                    Point(x1, y1, out, color);
                    x1 += (dir > 0) ? incx : 0;
                    y1 += incy;
                    dir += (dir > 0) ? up : right;
                }
            }
        }

        private void Line(float x1, float y1, float x2, float y2, int[] out, int color) {
            this.Line((int) x1, (int) y1, (int) x2, (int) y2, out, color);
        }

        private void Point(int x, int y, int[] out, int color) {
            int index = (63 - y) * 512 + x;
            if (index >= 0 && index < 512 * 64) {
                out[index] = color;
            } else {
                common.Warning("Out of bounds on point %d : %d", x, y);
            }
        }
    };
}
