package neo.ui;

import static neo.TempDump.atoi;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Text.Str.va;

import java.util.Objects;
import java.util.Scanner;

import neo.Renderer.Material.idMaterial;
import neo.framework.File_h.idFile;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.ui.Rectangle.idRectangle;
import neo.ui.Window.idWindow;

/**
 *
 */
public class Winvar {

    public static final String VAR_GUIPREFIX     = "gui::";
    public static final int    VAR_GUIPREFIX_LEN = VAR_GUIPREFIX.length();

    public static final idWinVar MIN_ONE = new idWinInt(-1);
    public static final idWinVar MIN_TWO = new idWinInt(-2);

    static abstract class idWinVar {

        public    int     DEBUG_COUNTER;
        protected idDict  guiDict;
        protected String  name;
        protected boolean eval;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idWinVar() {
            this.guiDict = null;
            this.name = null;
            this.eval = true;
        }
        // public   ~idWinVar();

        public void SetGuiInfo(idDict gd, final String _name) {
            this.guiDict = gd;
            SetName(_name);
        }

        public String GetName() {
            if (this.name != null) {
                if ((this.guiDict != null) && (this.name.charAt(0) == '*')) {
                    return this.guiDict.GetString(this.name.substring(1));
                }
                return this.name;
            }
            return "";
        }

        public void SetName(final String _name) {
            // delete []name; 
            this.name = _name;
//            if (_name != null) {
//                // name = new char[strlen(_name)+1]; 
//                // strcpy(name, _name); 
//                name = _name;
//            }
        }

        // idWinVar &operator=( final idWinVar other );
        public idWinVar oSet(final idWinVar other) {
            this.guiDict = other.guiDict;
            SetName(other.name);
            return this;
        }

        public idDict GetDict() {
            return this.guiDict;
        }

        public boolean NeedsUpdate() {
            return (this.guiDict != null);
        }

        public static int DBG_Init = 0;
        public void Init(final String _name, idWindow win) {
            idStr key = new idStr(_name);
            this.guiDict = null;
            final int len = key.Length();
            if ((len > 5) && _name.startsWith("gui:")) {
                DBG_Init++;
                key = key.Right(len - VAR_GUIPREFIX_LEN);
                SetGuiInfo(win.GetGui().GetStateDict(), key.toString());
                win.AddUpdateVar(this);
            } else {
                Set(_name);
            }
        }

        public abstract void Set(final String val);

        public void Set(final idStr val) {
            Set(val.toString());
        }

        public abstract void Update();

        public abstract String c_str();

        public int /*size_t*/ Size() {
            final int /*size_t*/ sz = (this.name != null) ? this.name.length() : 0;
            return sz;
        }

        public abstract void WriteToSaveGame(idFile savefile);

        public abstract void ReadFromSaveGame(idFile savefile);

        public abstract float x();

        public void SetEval(boolean b) {
            this.eval = b;
        }

        public boolean GetEval() {
            return this.eval;
        }

        /** @deprecated calling this function in idWindow::EmitOp hides the loading bar progress. */
        @Deprecated
        public static idWinVar clone(final idWinVar var) {
            if (var == null) {
				return null;
			}

            if ((var.name != null) && var.name.isEmpty()) {
                final int a = 1;
            }
            if (var instanceof idWinBool) {
                return new idWinBool((idWinBool) var);
            }
            if (var instanceof idWinBackground) {
                return new idWinBackground((idWinBackground) var);
            }
            if (var instanceof idWinFloat) {
                return new idWinFloat((idWinFloat) var);
            }
            if (var instanceof idWinInt) {
                return new idWinInt(((idWinInt) var).data);
            }
            if (var instanceof idWinRectangle) {
                return new idWinRectangle((idWinRectangle) var);
            }
            if (var instanceof idWinStr) {
                return new idWinStr((idWinStr) var);
            }
            if (var instanceof idWinVec2) {
                return new idWinVec2(((idWinVec2) var).data);
            }
            if (var instanceof idWinVec3) {
                return new idWinVec3((idWinVec3) var);
            }
            if (var instanceof idWinVec4) {
                return new idWinVec4((idWinVec4) var);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "idWinVar{" + "guiDict=" + this.guiDict + ", name=" + this.name + '}';
        }
    }

    static class idWinBool extends idWinVar {

        protected boolean data;
        //
        //

        public idWinBool() {
            super();
        }

        public idWinBool(boolean a) {
            this();
            this.data = a;
        }

        //copy constructor
        idWinBool(idWinBool winBool) {
            super.oSet(winBool);
            this.data = winBool.data;
        }

        // ~idWinBool() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = this.guiDict.GetBool(GetName());
            }
        }
//	int	operator==(	const bool &other ) { return (other == data); }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (37 * hash) + (this.data ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != boolean.class) {
                return false;
            }
            final boolean other = (boolean) obj;
            
            return (this.data == other);
        }

        public boolean oSet(final boolean other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.SetBool(GetName(), this.data);
            }
            return this.data;
        }

        public idWinBool oSet(final idWinBool other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        public boolean oCastBoolean() {
            return this.data;
        }

        @Override
        public void Set(final String val) {
            this.data = (atoi(val) != 0);
            if (this.guiDict != null) {
                this.guiDict.SetBool(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data = this.guiDict.GetBool(s);
            }
        }

        @Override
        public String c_str() {
            return va("%d", this.data);
        }

        // SaveGames
        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.WriteBool(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            this.data = savefile.ReadBool();
        }

        @Override
        public float x() {
            return this.data ? 1.0f : 0.0f;
        }
    }

    public static class idWinStr extends idWinVar {

        protected idStr data = new idStr();
        //
        //

        public idWinStr() {
            super();
        }
//	// ~idWinStr() {};

        public idWinStr(String a) {
            this();
            this.data = new idStr(a);
        }

        //copy constructor
        idWinStr(idWinStr other) {
            super.oSet(other);
            this.data = new idStr(other.data);
        }

        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = new idStr(this.guiDict.GetString(GetName()));
            }
        }

//	int	operator==(	const idStr other ) {
//		return (other == data);
//	}
//	int	operator==(	const char *other ) {
//		return (data == other);
//	}
        @Override
        public int hashCode() {
            int hash = 7;
            hash = (11 * hash) + Objects.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            //operator==( const idStr &other )
            if (obj.getClass() == idStr.class) {
                final idStr other = (idStr) obj;

                return Objects.equals(this.data, other);
            }

            //operator==( const char *other )
            if (obj.getClass() == String.class) {
                final String other = (String) obj;

                return Objects.equals(this.data, other);
            }

            return false;
        }

        public idStr oSet(final idStr other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.Set(GetName(), this.data);
            }
            return this.data;
        }

        public idWinStr oSet(final idWinStr other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }
//public	operator const char *() {//TODO:wtF?

        public char[] oCastChar() {//TODO:wtF?
            return this.data.c_str();
        }
//	public operator const idStr &() {

        public int LengthWithoutColors() {
            if ((this.guiDict != null) && (this.name != null) && !this.name.isEmpty()) {
                this.data.oSet(this.guiDict.GetString(GetName()));
            }
            return this.data.LengthWithoutColors();
        }

        public int Length() {
            if ((this.guiDict != null) && (this.name != null) && !this.name.isEmpty()) {
                this.data.oSet(this.guiDict.GetString(GetName()));
            }
            return this.data.Length();
        }

        public void RemoveColors() {
            if ((this.guiDict != null) && (this.name != null) && !this.name.isEmpty()) {
                this.data.oSet(this.guiDict.GetString(GetName()));
            }
            this.data.RemoveColors();
        }

        @Override
        public String c_str() {
            return this.data.toString();
        }

        @Override
        public void Set(final String val) {
            this.data.oSet(val);
            if (this.guiDict != null) {
                this.guiDict.Set(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && !s.isEmpty()) {
                this.data.oSet(this.guiDict.GetString(s));
            }
        }

        @Override
        public int /*size_t*/ Size() {
            final int sz = super.Size();
            return sz + this.data.Allocated();
        }

        // SaveGames
        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);

            final int len = this.data.Length();
            savefile.WriteInt(len);
            if (len > 0) {
                savefile.WriteString(this.data);
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();

            int len;
            len = savefile.ReadInt();
            if (len > 0) {
                this.data.Fill(' ', len);
                savefile.ReadString(this.data);
            }
        }

        // return wether string is emtpy
        @Override
        public float x() {
            return this.data.IsEmpty() ? 0.0f : 1.0f;
        }
    }

    static class idWinInt extends idWinVar {

        protected int data;
        //
        //

        public idWinInt() {
            super();
        }

        public idWinInt(int a) {
            this();
            this.data = a;
        }

//	~idWinInt() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = this.guiDict.GetInt(GetName());
            }
        }

        public int oSet(final int other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.SetInt(GetName(), this.data);
            }
            return this.data;
        }

        public idWinInt oSet(final idWinInt other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        int oCastInt() {
            return this.data;
        }

        @Override
        public void Set(final String val) {
            this.data = Integer.parseInt(val);
            if (this.guiDict != null) {
                this.guiDict.SetInt(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data = this.guiDict.GetInt(s);
            }
        }

        @Override
        public String c_str() {
            return va("%d", this.data);
        }

        // SaveGames
        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.WriteInt(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            this.data = savefile.ReadInt();
        }

        // no suitable conversion
        @Override
        public float x() {
            assert (false);
            return 0.0f;
        }
    }

    static class idWinFloat extends idWinVar {

        protected float data;
        //
        //

        public idWinFloat() {
            super();
        }

        public idWinFloat(int a) {
            this();
            this.data = a;///TODO:to float bits?
        }

        //copy constructor
        idWinFloat(idWinFloat winFloat) {
            super.oSet(winFloat);
            this.data = winFloat.data;
        }

//	~idWinFloat() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = this.guiDict.GetFloat(GetName());
            }
        }

        public idWinFloat oSet(final idWinFloat other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        public float oSet(final float other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.SetFloat(GetName(), this.data);
            }
            return this.data;
        }

        public float oCastFloat() {
            return this.data;
        }

        @Override
        public void Set(final String val) {
            try {
                this.data = Float.parseFloat(val);
            } catch (final NumberFormatException e) {
                this.data = 0;//atof doesn't crash with non numbers.
            }
            if (this.guiDict != null) {
                this.guiDict.SetFloat(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data = this.guiDict.GetFloat(s);
            }
        }

        @Override
        public String c_str() {
            return va("%f", this.data);
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.WriteFloat(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            this.data = savefile.ReadFloat();
        }

        @Override
        public float x() {
            return this.data;
        }

    }

    static class idWinRectangle extends idWinVar {

        protected idRectangle data;
        //
        //

        public idWinRectangle() {
            super();
            this.data = new idRectangle();
        }

        //copy constructor
        idWinRectangle(idWinRectangle rect) {
            super.oSet(rect);
            this.data = new idRectangle(rect.data);
        }

//	~idWinRectangle() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                final idVec4 v = this.guiDict.GetVec4(GetName());
                this.data.x = v.x;
                this.data.y = v.y;
                this.data.w = v.z;
                this.data.h = v.w;
            }
        }

//	int	operator==(	final idRectangle other ) {
//		return (other == data);
//	}//TODO:overrid equals
        public idWinRectangle oSet(final idWinRectangle other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        public idRectangle oSet(final idVec4 other) {
            this.data.oSet(other);
            if (this.guiDict != null) {
                this.guiDict.SetVec4(GetName(), other);
            }
            return this.data;
        }

        public idRectangle oSet(final idRectangle other) {
            this.data = other;
            if (this.guiDict != null) {
                final idVec4 v = this.data.ToVec4();
                this.guiDict.SetVec4(GetName(), v);
            }
            return this.data;
        }

        public idRectangle oCastIdRectangle() {
            return this.data;
        }

        @Override
        public float x() {
            return this.data.x;
        }

        public float y() {
            return this.data.y;
        }

        public float w() {
            return this.data.w;
        }

        public float h() {
            return this.data.h;
        }

        public float Right() {
            return this.data.Right();
        }

        public float Bottom() {
            return this.data.Bottom();
        }
        private static idVec4 ret;

        public idVec4 ToVec4() {
            ret = this.data.ToVec4();
            return ret;
        }

        @Override
        public void Set(final String val) {

            try (final Scanner sscanf = new Scanner(val)) {
                if (val.contains(",")) {
//			sscanf( val, "%f,%f,%f,%f", data.x, data.y, data.w, data.h );
                    if (sscanf.hasNext()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.y = sscanf.skip(",").nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.w = sscanf.skip(",").nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.h = sscanf.skip(",").nextFloat();
                    }
                } else {
//			sscanf( val, "%f %f %f %f", data.x, data.y, data.w, data.h );
                    if (sscanf.hasNextFloat()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.y = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.w = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.h = sscanf.nextFloat();
                    }
                }
            }
            if (this.guiDict != null) {
                final idVec4 v = this.data.ToVec4();
                this.guiDict.SetVec4(GetName(), v);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                final idVec4 v = this.guiDict.GetVec4(s);
                this.data.x = v.x;
                this.data.y = v.y;
                this.data.w = v.z;
                this.data.h = v.w;
            }
        }

        @Override
        public String c_str() {
            return this.data.ToVec4().ToString();
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.Write(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            savefile.Read(this.data);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (97 * hash) + Objects.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != idRectangle.class) {
                return false;
            }
            final idRectangle other = (idRectangle) obj;
            
            return Objects.equals(this.data, other);
        }
    }

    static class idWinVec2 extends idWinVar {

        protected idVec2 data;
        //
        //

        public idWinVec2() {
            super();
        }

        //copy constructor
        idWinVec2(idVec2 vec2) {
            this.data = new idVec2(vec2);
            if (this.guiDict != null) {
                this.guiDict.SetVec2(GetName(), this.data);
            }
        }

//	~idWinVec2() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = this.guiDict.GetVec2(GetName());
            }
        }
//	int	operator==(	const idVec2 other ) {
//		return (other == data);
//	}

        @Override
        public int hashCode() {
            int hash = 3;
            hash = (23 * hash) + Objects.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != idVec2.class) {
                return false;
            }
            final idVec2 other = (idVec2) obj;
            
            return Objects.equals(this.data, other);
        }

        idWinVec2 oSet(final idWinVec2 other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        idVec2 oSet(final idVec2 other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.SetVec2(GetName(), this.data);
            }
            return this.data;
        }

        @Override
        public float x() {
            return this.data.x;
        }

        public float y() {
            return this.data.y;
        }

        @Override
        public void Set(final String val) {

            try (final Scanner sscanf = new Scanner(val)) {
                if (val.contains(",")) {
//			sscanf( val, "%f,%f,%f,%f", data.x, data.y, data.w, data.h );
                    if (sscanf.hasNext()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.y = sscanf.skip(",").nextFloat();
                    }
                } else {
//			sscanf( val, "%f %f %f %f", data.x, data.y, data.w, data.h );
                    if (sscanf.hasNextFloat()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.y = sscanf.nextFloat();
                    }
                }
            }
            if (this.guiDict != null) {
                this.guiDict.SetVec2(GetName(), this.data);
            }
        }

        idVec2 oCastIdVec2() {
            return this.data;
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data = this.guiDict.GetVec2(s);
            }
        }

        @Override
        public String c_str() {
            return this.data.ToString();
        }

        void Zero() {
            this.data.Zero();
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.Write(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            savefile.Read(this.data);
        }
    }

    static class idWinVec4 extends idWinVar {

        protected final idVec4 data;
        //
        //

        public idWinVec4() {
            super();
            this.data = new idVec4();
        }

        public idWinVec4(float x, float y, float z, float w) {//TODO: check whether the int to pointer cast works like this.
            this();
            this.data.oSet(new idVec4(x, y, z, w));
        }

        //copy constructor
        idWinVec4(idWinVec4 winVec4) {
            super.oSet(winVec4);
            this.data = new idVec4(winVec4.data);
        }

//	~idWinVec4() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data.oSet(this.guiDict.GetVec4(GetName()));
            }
        }
//	int	operator==(	final idVec4 other ) {
//		return (other == data);
//	}

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (97 * hash) + Objects.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != idVec4.class) {
                return false;
            }
            final idVec4 other = (idVec4) obj;
            
            return Objects.equals(this.data, other);
        }

        public idWinVec4 oSet(final idWinVec4 other) {
            super.oSet(other);
            this.data.oSet(other.data);
            return this;
        }

        public idVec4 oSet(final idVec4 other) {
            this.data.oSet(other);
            if (this.guiDict != null) {
                this.guiDict.SetVec4(GetName(), this.data);
            }
            return this.data;
        }

        public idVec4 oCastIdVec4() {
            return this.data;
        }

        @Override
        public float x() {
            return this.data.x;
        }

        public float y() {
            return this.data.y;
        }

        public float z() {
            return this.data.z;
        }

        public float w() {
            return this.data.w;
        }

        @Override
        public void Set(final String val) {

            try (final Scanner sscanf = new Scanner(val)) {
                if (val.contains(",")) {
//			sscanf( val, "%f,%f,%f,%f", data.x, data.y, data.z, data.w );
                    if (sscanf.hasNext()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.y = sscanf.skip(",").nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.z = sscanf.skip(",").nextFloat();
                    }
                    if (sscanf.hasNext()) {
                        this.data.w = sscanf.skip(",").nextFloat();
                    }
                } else {
//			sscanf( val, "%f %f %f %f", data.x, data.y, data.z, data.w );
                    if (sscanf.hasNextFloat()) {
                        this.data.x = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.y = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.z = sscanf.nextFloat();
                    }
                    if (sscanf.hasNextFloat()) {
                        this.data.w = sscanf.nextFloat();
                    }
                }
            }
            if (this.guiDict != null) {
                this.guiDict.SetVec4(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data.oSet(this.guiDict.GetVec4(s));
            }
        }

        @Override
        public String c_str() {
            return this.data.ToString();
        }

        @Override
        public String toString() {
            return String.valueOf(this.data);
        }

        public void Zero() {
            this.data.Zero();
            if (this.guiDict != null) {
                this.guiDict.SetVec4(GetName(), this.data);
            }
        }

        public idVec3 ToVec3() {
            return this.data.ToVec3();
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.Write(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            savefile.Read(this.data);
        }
    }

    static class idWinVec3 extends idWinVar {

        protected idVec3 data;
        //
        //

        public idWinVec3() {
            super();
        }

        //copy constructor
        idWinVec3(idWinVec3 winVec3) {
            super.oSet(winVec3);
            this.data = new idVec3(winVec3.data);
        }

//	~idWinVec3() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data = this.guiDict.GetVector(GetName());
            }
        }
//	int	operator==(	const idVec3 other ) {
//		return (other == data);
//	}

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (23 * hash) + Objects.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != idVec3.class) {
                return false;
            }
            final idVec3 other = (idVec3) obj;
            
            return Objects.equals(this.data, other);
        }

        public idWinVec3 oSet(final idWinVec3 other) {
            super.oSet(other);
            this.data = other.data;
            return this;
        }

        public idVec3 oSet(final idVec3 other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.SetVector(GetName(), this.data);
            }
            return this.data;
        }

        public idVec3 oCastIdVec3() {
            return this.data;
        }

        @Override
        public float x() {
            return this.data.x;
        }

        public float y() {
            return this.data.y;
        }

        public float z() {
            return this.data.z;
        }

        @Override
        public void Set(final String val) {
            try (final Scanner sscanf = new Scanner(val)) {
//		sscanf( val, "%f %f %f", data.x, data.y, data.z);
                if (sscanf.hasNextFloat()) {
                    this.data.x = sscanf.nextFloat();
                }
                if (sscanf.hasNextFloat()) {
                    this.data.y = sscanf.nextFloat();
                }
                if (sscanf.hasNextFloat()) {
                    this.data.z = sscanf.nextFloat();
                }
            }
            if (this.guiDict != null) {
                this.guiDict.SetVector(GetName(), this.data);
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data = this.guiDict.GetVector(s);
            }
        }

        @Override
        public String c_str() {
            return this.data.ToString();
        }

        public void Zero() {
            this.data.Zero();
            if (this.guiDict != null) {
                this.guiDict.SetVector(GetName(), this.data);
            }
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);
            savefile.Write(this.data);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();
            savefile.Read(this.data);
        }
    }

    static class idWinBackground extends idWinStr {

        protected final idMaterial[] mat;
        //
        //

        public idWinBackground() {
            super();
            this.mat = new idMaterial[1];
            this.data = new idStr();
        }

        //copy constructor
        idWinBackground(idWinBackground other) {
            super.oSet(other);
            this.data = other.data;
            this.mat = other.mat;
            if (this.mat != null) {
                if (this.data.IsEmpty()) {
                    this.mat[0] = null;
                } else {
                    this.mat[0] = declManager.FindMaterial(this.data);
                }
            }
        }

//	~idWinBackground() {};
        @Override
        public void Init(final String _name, idWindow win) {
            super.Init(_name, win);
            if (this.guiDict != null) {
                this.data.oSet(this.guiDict.GetString(GetName()));
            }
        }
//	int	operator==(	const idStr other ) {
//		return (other == data);
//	}
//	int	operator==(	const char *other ) {
//		return (data == other);
//	}

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public idStr oSet(final idStr other) {
            this.data = other;
            if (this.guiDict != null) {
                this.guiDict.Set(GetName(), this.data);
            }
            if (this.mat[0] != null) {
                if (this.data.IsEmpty()) {
                    this.mat[0] = null;
                } else {
                    this.mat[0] = declManager.FindMaterial(this.data);
                }
            }
            return this.data;
        }

//        public idWinBackground oSet(final idWinBackground other) {
//            super.oSet(other);
//            data = other.data;
//            mat[0] = other.mat[0];
//            if (mat != null) {
//                if (data.IsEmpty()) {
//                    mat[0] = null;
//                } else {
//                    mat[0] = declManager.FindMaterial(data);
//                }
//            }
//            return this;
//        }
        @Override
        public char[] oCastChar() {
            return this.data.c_str();
        }

        @Override
        public int Length() {
            if (this.guiDict != null) {
                this.data.oSet(this.guiDict.GetString(GetName()));
            }
            return this.data.Length();
        }

        @Override
        public String c_str() {
            return this.data.toString();
        }

        @Override
        public void Set(final String val) {
            this.data.oSet(val);
            if (this.guiDict != null) {
                this.guiDict.Set(GetName(), this.data);
            }
            if (this.mat[0] != null) {
                if (this.data.IsEmpty()) {
                    this.mat[0] = null;
                } else {
                    this.mat[0] = declManager.FindMaterial(this.data);
                }
            }
        }

        @Override
        public void Update() {
            final String s = GetName();
            if ((this.guiDict != null) && (s.charAt(0) != '\0')) {
                this.data.oSet(this.guiDict.GetString(s));
                if (this.mat != null) {
                    if (this.data.IsEmpty()) {
                        this.mat[0] = null;
                    } else {
                        this.mat[0] = declManager.FindMaterial(this.data);
                    }
                }
            }
        }

        @Override
        public int/*size_t*/ Size() {
            final int sz = super.Size();
            return sz + this.data.Allocated();
        }

        public void SetMaterialPtr(final idMaterial m) {
            this.mat[0] = m;
        }

        @Override
        public void WriteToSaveGame(idFile savefile) {
            savefile.WriteBool(this.eval);

            final int len = this.data.Length();
            savefile.WriteInt(len);
            if (len > 0) {
                savefile.WriteString(this.data);
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            this.eval = savefile.ReadBool();

            int len;
            len = savefile.ReadInt();
            if (len > 0) {
                this.data.Fill(' ', len);
                savefile.ReadString(this.data);
            }
            if (this.mat[0] != null) {
                if (len > 0) {
                    this.mat[0] = declManager.FindMaterial(this.data);
                } else {
                    this.mat[0] = null;
                }
            }
        }
    }

    /*
     ================
     idMultiWinVar
     multiplexes access to a list if idWinVar*
     ================
     */
    static class idMultiWinVar extends idList<idWinVar> {

        public void Set(final String val) {
            for (int i = 0; i < Num(); i++) {
                this.oGet(i).Set(val);
            }
        }

        public void Update() {
            for (int i = 0; i < Num(); i++) {
                this.oGet(i).Update();
            }
        }

        public void SetGuiInfo(idDict dict) {
            for (int i = 0; i < Num(); i++) {
                this.oGet(i).SetGuiInfo(dict, this.oGet(i).c_str());
            }
        }
    }
}
