package neo.ui;

import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.framework.Common.common;
import static neo.ui.RegExp.idRegister.REGTYPE.NUMTYPES;
import static neo.ui.RegExp.idRegister.REGTYPE.STRING;

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
        }
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
        public boolean DBG_D3_KEY = false;
        //
        //

        public idRegister() {
        }

        public idRegister(final String p, int t) {
            this.name = new idStr(p);
            this.type = (short) t;
            assert ((t >= 0) && (t < NUMTYPES.ordinal()));
            this.regCount = REGCOUNT[t];
            this.enabled = (this.type != STRING.ordinal());
            this.var = null;
        }

        public void SetToRegs(float[] registers) {
            int i;
            idVec4 v = new idVec4();
            idVec2 v2;
            idVec3 v3;
            idRectangle rect;

            if (!this.enabled || (this.var == null) || ((this.var != null) && ((this.var.GetDict() != null) || !this.var.GetEval()))) {
                return;
            }

            switch (REGTYPE.values()[this.type]) {
                case VEC4: {
                    v = ((idWinVec4) this.var).data;
                    break;
                }
                case RECTANGLE: {
                    rect = ((idWinRectangle) this.var).data;
                    v = rect.ToVec4();
                    break;
                }
                case VEC2: {
                    v2 = ((idWinVec2) this.var).data;
                    v.oSet(0, v2.oGet(0));
                    v.oSet(1, v2.oGet(1));
                    break;
                }
                case VEC3: {
                    v3 = ((idWinVec3) this.var).data;
                    v.oSet(0, v3.oGet(0));
                    v.oSet(1, v3.oGet(1));
                    v.oSet(2, v3.oGet(2));
                    break;
                }
                case FLOAT: {
                    v.oSet(0, ((idWinFloat) this.var).data);
                    break;
                }
                case INT: {
                    v.oSet(0, ((idWinInt) this.var).data);
                    break;
                }
                case BOOL: {
                    v.oSet(0, btoi(((idWinBool) this.var).data));
                    break;
                }
                default: {
                    common.FatalError("idRegister::SetToRegs: bad reg type");
                    break;
                }
            }

            for (i = 0; i < this.regCount; i++) {
                if (Float.isInfinite(registers[this.regs[i]] = v.oGet(i))) {
                    final int bla = 111;
                }
            }
        }

        private static int DBG_GetFromRegs = 0;
        public void GetFromRegs(float[] registers) {DBG_GetFromRegs++;
            final idVec4 v = new idVec4();
            final idRectangle rect = new idRectangle();

            if (!this.enabled || (this.var == null) || ((this.var != null) && ((this.var.GetDict() != null) || !this.var.GetEval()))) {
                return;
            }

            for (int i = 0; i < this.regCount; i++) {
                v.oSet(i, registers[this.regs[i]]);
            }

            switch (REGTYPE.values()[this.type]) {
                case VEC4: {
                    ((idWinVec4) this.var).oSet(v);
                    break;
                }
                case RECTANGLE: {
                    rect.x = v.x;
                    rect.y = v.y;
                    rect.w = v.z;
                    rect.h = v.w;
                    ((idWinRectangle) this.var).oSet(rect);
                    break;
                }
                case VEC2: {
                    ((idWinVec2) this.var).oSet(v.ToVec2());
                    break;
                }
                case VEC3: {
                    ((idWinVec3) this.var).oSet(v.ToVec3());
                    break;
                }
                case FLOAT: {
                    ((idWinFloat) this.var).data = v.oGet(0);
                    break;
                }
                case INT: {
                    ((idWinInt) this.var).data = (int) v.oGet(0);
                    break;
                }
                case BOOL: {
                    ((idWinBool) this.var).data = (v.oGet(0) != 0.0f);
                    break;
                }
                default: {
                    common.FatalError("idRegister::GetFromRegs: bad reg type");
                    break;
                }
            }
        }

        public void CopyRegs(idRegister src) {
            this.regs[0] = src.regs[0];
            this.regs[1] = src.regs[1];
            this.regs[2] = src.regs[2];
            this.regs[3] = src.regs[3];
        }

        public void Enable(boolean b) {
            this.enabled = b;
        }

        public void ReadFromDemoFile(idDemoFile f) {
            this.enabled = f.ReadBool();
            this.type = f.ReadShort();
            this.regCount = f.ReadInt();
            for (int i = 0; i < 4; i++) {
                this.regs[i] = (short) f.ReadUnsignedShort();
            }
            this.name.oSet(f.ReadHashString());
        }

        public void WriteToDemoFile(idDemoFile f) {
            f.WriteBool(this.enabled);
            f.WriteShort(this.type);
            f.WriteInt(this.regCount);
            for (int i = 0; i < 4; i++) {
                f.WriteUnsignedShort(this.regs[i]);
            }
            f.WriteHashString(this.name.getData());
        }

        public void WriteToSaveGame(idFile savefile) {
            int len;

            savefile.WriteBool(this.enabled);
            savefile.WriteShort(this.type);
            savefile.WriteInt(this.regCount);
            savefile.WriteShort(this.regs[0]);

            len = this.name.Length();
            savefile.WriteInt(len);
            savefile.WriteString(this.name);

            this.var.WriteToSaveGame(savefile);
        }

        public void ReadFromSaveGame(idFile savefile) {
            int len;

            this.enabled = savefile.ReadBool();
            this.type = savefile.ReadShort();
            this.regCount = savefile.ReadInt();
            this.regs[0] = savefile.ReadShort();

            len = savefile.ReadInt();
            this.name.Fill(' ', len);
            savefile.ReadString(this.name);

            this.var.ReadFromSaveGame(savefile);
        }
    }

    static class idRegisterList {

        private final idList<idRegister> regs;
        private final idHashIndex regHash;
        //
        //

        public idRegisterList() {
            this.regs = new idList<idRegister>(4);//.SetGranularity(4);
            this.regHash = new idHashIndex(32, 4);//.SetGranularity(4);
//            regHash.Clear(32, 4);
        }
        // ~idRegisterList();

        public void AddReg(final String name, int type, idParser src, idWindow win, idWinVar var) {
            idRegister reg;

            reg = FindReg(name);

            if (null == reg) {
                assert ((type >= 0) && (type < idRegister.REGTYPE.NUMTYPES.ordinal()));
                final int numRegs = idRegister.REGCOUNT[type];
                reg = new idRegister(name, type);
                reg.var = var;
                if (type == idRegister.REGTYPE.STRING.ordinal()) {
                    final idToken token = new idToken();
                    if (src.ReadToken(token)) {
                        if("#str_07184".equals(token.getData())){
                            reg.DBG_D3_KEY = true;
                        }
                        token.oSet(common.GetLanguageDict().GetString(token.getData()));
                        var.Init(token.getData(), win);
                    }
                } else {
                    for (int i = 0; i < numRegs; i++) {
                        reg.regs[i] = (short) win.ParseExpression(src, null);
                        if (i < (numRegs - 1)) {
                            src.ExpectTokenString(",");
                        }
                    }
                }
                final int hash = this.regHash.GenerateKey(name, false);
                this.regHash.Add(hash, this.regs.Append(reg));
            } else {
                final int numRegs = idRegister.REGCOUNT[type];
                reg.var = var;
                if (type == idRegister.REGTYPE.STRING.ordinal()) {
                    final idToken token = new idToken();
                    if (src.ReadToken(token)) {
                        var.Init(token.getData(), win);
                    }
                } else {
                    for (int i = 0; i < numRegs; i++) {
                        reg.regs[i] = (short) win.ParseExpression(src, null);
                        if (i < (numRegs - 1)) {
                            src.ExpectTokenString(",");
                        }
                    }
                }
            }
        }

        public void AddReg(final String name, int type, idVec4 data, idWindow win, idWinVar var) {
            if (FindReg(name) == null) {
                assert ((type >= 0) && (type < idRegister.REGTYPE.NUMTYPES.ordinal()));
                final int numRegs = idRegister.REGCOUNT[type];
                final idRegister reg = new idRegister(name, type);
                reg.var = var;
                for (int i = 0; i < numRegs; i++) {
                    reg.regs[i] = (short) win.ExpressionConstant(data.oGet(i));
                }
                final int hash = this.regHash.GenerateKey(name, false);
                this.regHash.Add(hash, this.regs.Append(reg));
            }
        }

        public idRegister FindReg(final String name) {
            final int hash = this.regHash.GenerateKey(name, false);
            for (int i = this.regHash.First(hash); i != -1; i = this.regHash.Next(i)) {
                if (this.regs.oGet(i).name.Icmp(name) == 0) {
//                    System.out.println(regs.oGet(i));
                    return this.regs.oGet(i);
                }
            }
            return null;
        }

        public void SetToRegs(float[] registers) {
            int i;
            for (i = 0; i < this.regs.Num(); i++) {
                this.regs.oGet(i).SetToRegs(registers);
            }
        }

        public void GetFromRegs(float[] registers) {
            for (int i = 0; i < this.regs.Num(); i++) {
                this.regs.oGet(i).GetFromRegs(registers);
            }
        }

        public void Reset() {
            this.regs.DeleteContents(true);
            this.regHash.Clear();
        }

        public void ReadFromDemoFile(idDemoFile f) {
            final int[] c = new int[1];

            f.ReadInt(c);
            this.regs.DeleteContents(true);
            for (int i = 0; i < c[0]; i++) {
                final idRegister reg = new idRegister();
                reg.ReadFromDemoFile(f);
                this.regs.Append(reg);
            }
        }

        public void WriteToDemoFile(idDemoFile f) {
            final int c = this.regs.Num();

            f.WriteInt(c);
            for (int i = 0; i < c; i++) {
                this.regs.oGet(i).WriteToDemoFile(f);
            }
        }

        public void WriteToSaveGame(idFile savefile) {
            int i, num;

            num = this.regs.Num();
            savefile.WriteInt(num);

            for (i = 0; i < num; i++) {
                this.regs.oGet(i).WriteToSaveGame(savefile);
            }
        }

        public void ReadFromSaveGame(idFile savefile) {
            int i, num;

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                this.regs.oGet(i).ReadFromSaveGame(savefile);
            }
        }

    }
}
