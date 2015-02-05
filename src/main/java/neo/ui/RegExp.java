package neo.ui;

import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.framework.Common.common;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.ui.Rectangle.idRectangle;
import neo.ui.RegExp.idRegister;
import static neo.ui.RegExp.idRegister.REGTYPE.BOOL;
import static neo.ui.RegExp.idRegister.REGTYPE.FLOAT;
import static neo.ui.RegExp.idRegister.REGTYPE.INT;
import static neo.ui.RegExp.idRegister.REGTYPE.NUMTYPES;
import static neo.ui.RegExp.idRegister.REGTYPE.RECTANGLE;
import static neo.ui.RegExp.idRegister.REGTYPE.STRING;
import static neo.ui.RegExp.idRegister.REGTYPE.VEC2;
import static neo.ui.RegExp.idRegister.REGTYPE.VEC3;
import static neo.ui.RegExp.idRegister.REGTYPE.VEC4;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinFloat;
import neo.ui.Winvar.idWinInt;
import neo.ui.Winvar.idWinRectangle;
import neo.ui.Winvar.idWinVar;
import neo.ui.Winvar.idWinVec2;
import neo.ui.Winvar.idWinVec3;
import neo.ui.Winvar.idWinVec4;

/**
 *
 */
public class RegExp {

    public static class idRegister {

        public enum REGTYPE {

            VEC4 /*= 0*/, FLOAT, BOOL, INT, STRING, VEC2, VEC3, RECTANGLE, NUMTYPES
        };
        public static final int[] REGCOUNT = new int[etoi(NUMTYPES)];

        static {
            final int[] bv = {4, 1, 1, 1, 0, 2, 3, 4};
            System.arraycopy(bv, 0, REGCOUNT, 0, REGCOUNT.length);
        }
        //
        public boolean enabled;
        public short   type;
        public idStr   name;
        public int     regCount;
        public /*unsigned*/ final short[] regs = new short[4];
        public idWinVar var;
        //
        //

        public idRegister() {
        }

        public idRegister(final String p, int t) {
            name = new idStr(p);
            type = (short) t;
            assert (t >= 0 && t < NUMTYPES.ordinal());
            regCount = REGCOUNT[t];
            enabled = (type != STRING.ordinal());
            var = null;
        }

        public void SetToRegs(float[] registers) {
            int i;
            idVec4 v = new idVec4();
            idVec2 v2;
            idVec3 v3;
            idRectangle rect;

            if (!enabled || var == null || (var != null && (var.GetDict() != null || !var.GetEval()))) {
                return;
            }

            switch (REGTYPE.values()[type]) {
                case VEC4: {
                    v = ((idWinVec4) var).data;
                    break;
                }
                case RECTANGLE: {
                    rect = ((idWinRectangle) var).data;
                    v = rect.ToVec4();
                    break;
                }
                case VEC2: {
                    v2 = ((idWinVec2) var).data;
                    v.oSet(0, v2.oGet(0));
                    v.oSet(1, v2.oGet(1));
                    break;
                }
                case VEC3: {
                    v3 = ((idWinVec3) var).data;
                    v.oSet(0, v3.oGet(0));
                    v.oSet(1, v3.oGet(1));
                    v.oSet(2, v3.oGet(2));
                    break;
                }
                case FLOAT: {
                    v.oSet(0, ((idWinFloat) var).data);
                    break;
                }
                case INT: {
                    v.oSet(0, ((idWinInt) var).data);
                    break;
                }
                case BOOL: {
                    v.oSet(0, btoi(((idWinBool) var).data));
                    break;
                }
                default: {
                    common.FatalError("idRegister::SetToRegs: bad reg type");
                    break;
                }
            }

            for (i = 0; i < regCount; i++) {
                if (Float.isInfinite(registers[regs[i]] = v.oGet(i))) {
                    int bla = 111;
                }
            }
        }

        public void GetFromRegs(float[] registers) {
            idVec4 v = new idVec4();
            idRectangle rect = new idRectangle();

            if (!enabled || var == null || (var != null && (var.GetDict() != null || !var.GetEval()))) {
                return;
            }

            for (int i = 0; i < regCount; i++) {
                v.oSet(i, registers[regs[i]]);
            }

            switch (REGTYPE.values()[type]) {
                case VEC4: {
                    ((idWinVec4) var).oSet(v);
                    break;
                }
                case RECTANGLE: {
                    rect.x = v.x;
                    rect.y = v.y;
                    rect.w = v.z;
                    rect.h = v.w;
                    ((idWinRectangle) var).oSet(rect);
                    break;
                }
                case VEC2: {
                    ((idWinVec2) var).oSet(v.ToVec2());
                    break;
                }
                case VEC3: {
                    ((idWinVec3) var).oSet(v.ToVec3());
                    break;
                }
                case FLOAT: {
                    ((idWinFloat) var).data = v.oGet(0);
                    break;
                }
                case INT: {
                    ((idWinInt) var).data = (int) v.oGet(0);
                    break;
                }
                case BOOL: {
                    ((idWinBool) var).data = (v.oGet(0) != 0.0f);
                    break;
                }
                default: {
                    common.FatalError("idRegister::GetFromRegs: bad reg type");
                    break;
                }
            }
        }

        public void CopyRegs(idRegister src) {
            regs[0] = src.regs[0];
            regs[1] = src.regs[1];
            regs[2] = src.regs[2];
            regs[3] = src.regs[3];
        }

        public void Enable(boolean b) {
            enabled = b;
        }

        public void ReadFromDemoFile(idDemoFile f) {
            enabled = f.ReadBool();
            type = f.ReadShort();
            regCount = f.ReadInt();
            for (int i = 0; i < 4; i++) {
                regs[i] = (short) f.ReadUnsignedShort();
            }
            name.oSet(f.ReadHashString());
        }

        public void WriteToDemoFile(idDemoFile f) {
            f.WriteBool(enabled);
            f.WriteShort(type);
            f.WriteInt(regCount);
            for (int i = 0; i < 4; i++) {
                f.WriteUnsignedShort(regs[i]);
            }
            f.WriteHashString(name.toString());
        }

        public void WriteToSaveGame(idFile savefile) {
            int len;

            savefile.WriteBool(enabled);
            savefile.WriteShort(type);
            savefile.WriteInt(regCount);
            savefile.WriteShort(regs[0]);

            len = name.Length();
            savefile.WriteInt(len);
            savefile.WriteString(name);

            var.WriteToSaveGame(savefile);
        }

        public void ReadFromSaveGame(idFile savefile) {
            int len;

            enabled = savefile.ReadBool();
            type = savefile.ReadShort();
            regCount = savefile.ReadInt();
            regs[0] = savefile.ReadShort();

            len = savefile.ReadInt();
            name.Fill(' ', len);
            savefile.ReadString(name);

            var.ReadFromSaveGame(savefile);
        }
    };

    static class idRegisterList {

        private idList<idRegister> regs;
        private idHashIndex regHash;
        //
        //

        public idRegisterList() {
            regs = new idList<>(4);//.SetGranularity(4);
            regHash = new idHashIndex(32, 4);//.SetGranularity(4);
//            regHash.Clear(32, 4);
        }
        // ~idRegisterList();

        public void AddReg(final String name, int type, idParser src, idWindow win, idWinVar var) {
            idRegister reg;

            reg = FindReg(name);

            if (null == reg) {
                assert (type >= 0 && type < idRegister.REGTYPE.NUMTYPES.ordinal());
                int numRegs = idRegister.REGCOUNT[type];
                reg = new idRegister(name, type);
                reg.var = var;
                if (type == idRegister.REGTYPE.STRING.ordinal()) {
                    idToken tok = new idToken();
                    if (src.ReadToken(tok)) {
                        tok.oSet(common.GetLanguageDict().GetString(tok.toString()));
                        var.Init(tok.toString(), win);
                    }
                } else {
                    for (int i = 0; i < numRegs; i++) {
                        reg.regs[i] = (short) win.ParseExpression(src, null);
                        if (i < numRegs - 1) {
                            src.ExpectTokenString(",");
                        }
                    }
                }
                int hash = regHash.GenerateKey(name, false);
                regHash.Add(hash, regs.Append(reg));
            } else {
                int numRegs = idRegister.REGCOUNT[type];
                reg.var = var;
                if (type == idRegister.REGTYPE.STRING.ordinal()) {
                    idToken tok = new idToken();
                    if (src.ReadToken(tok)) {
                        var.Init(tok.toString(), win);
                    }
                } else {
                    for (int i = 0; i < numRegs; i++) {
                        reg.regs[i] = (short) win.ParseExpression(src, null);
                        if (i < numRegs - 1) {
                            src.ExpectTokenString(",");
                        }
                    }
                }
            }
        }

        public void AddReg(final String name, int type, idVec4 data, idWindow win, idWinVar var) {
            if (FindReg(name) == null) {
                assert (type >= 0 && type < idRegister.REGTYPE.NUMTYPES.ordinal());
                int numRegs = idRegister.REGCOUNT[type];
                idRegister reg = new idRegister(name, type);
                reg.var = var;
                for (int i = 0; i < numRegs; i++) {
                    reg.regs[i] = (short) win.ExpressionConstant(data.oGet(i));
                }
                int hash = regHash.GenerateKey(name, false);
                regHash.Add(hash, regs.Append(reg));
            }
        }
//

        public idRegister FindReg(final String name) {
            int hash = regHash.GenerateKey(name, false);
            for (int i = regHash.First(hash); i != -1; i = regHash.Next(i)) {
                if (regs.oGet(i).name.Icmp(name) == 0) {
                    return regs.oGet(i);
                }
            }
            return null;
        }

        public void SetToRegs(float[] registers) {
            int i;
            for (i = 0; i < regs.Num(); i++) {
                regs.oGet(i).SetToRegs(registers);
            }
        }

        public void GetFromRegs(float[] registers) {
            for (int i = 0; i < regs.Num(); i++) {
                regs.oGet(i).GetFromRegs(registers);
            }
        }

        public void Reset() {
            regs.DeleteContents(true);
            regHash.Clear();
        }

        public void ReadFromDemoFile(idDemoFile f) {
            int[] c = new int[1];

            f.ReadInt(c);
            regs.DeleteContents(true);
            for (int i = 0; i < c[0]; i++) {
                idRegister reg = new idRegister();
                reg.ReadFromDemoFile(f);
                regs.Append(reg);
            }
        }

        public void WriteToDemoFile(idDemoFile f) {
            int c = regs.Num();

            f.WriteInt(c);
            for (int i = 0; i < c; i++) {
                regs.oGet(i).WriteToDemoFile(f);
            }
        }

        public void WriteToSaveGame(idFile savefile) {
            int i, num;

            num = regs.Num();
            savefile.WriteInt(num);

            for (i = 0; i < num; i++) {
                regs.oGet(i).WriteToSaveGame(savefile);
            }
        }

        public void ReadFromSaveGame(idFile savefile) {
            int i, num;

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                regs.oGet(i).ReadFromSaveGame(savefile);
            }
        }

    };
}
