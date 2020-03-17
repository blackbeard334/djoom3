package neo.idlib.Text;

import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.strLen;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Objects;

import neo.TempDump.CPP_class.Char;
import neo.TempDump.CPP_class.Pointer;
import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Token.idToken;
import neo.idlib.math.Math_h;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Str {

    public static final int    FILE_HASH_SIZE  = 1024;
    // color escape character
    public static final int    C_COLOR_ESCAPE  = '^';
    public static final int    C_COLOR_DEFAULT = '0';
    public static final int    C_COLOR_RED     = '1';
    public static final int    C_COLOR_GREEN   = '2';
    public static final int    C_COLOR_YELLOW  = '3';
    public static final int    C_COLOR_BLUE    = '4';
    public static final int    C_COLOR_CYAN    = '5';
    public static final int    C_COLOR_MAGENTA = '6';
    public static final int    C_COLOR_WHITE   = '7';
    public static final int    C_COLOR_GRAY    = '8';
    public static final int    C_COLOR_BLACK   = '9';
    // color escape string
    public static final String S_COLOR_DEFAULT = "^0";
    public static final String S_COLOR_RED     = "^1";
    public static final String S_COLOR_GREEN   = "^2";
    public static final String S_COLOR_YELLOW  = "^3";
    public static final String S_COLOR_BLUE    = "^4";
    public static final String S_COLOR_CYAN    = "^5";
    public static final String S_COLOR_MAGENTA = "^6";
    public static final String S_COLOR_WHITE   = "^7";
    public static final String S_COLOR_GRAY    = "^8";
    public static final String S_COLOR_BLACK   = "^9";
    // make idStr a multiple of 16 bytes long
// don't make too large to keep memory requirements to a minimum
    static final        int    STR_ALLOC_BASE  = 20;
    static final        int    STR_ALLOC_GRAN  = 32;

    public enum Measure_t {

        MEASURE_SIZE,
        MEASURE_BANDWIDTH
    };
//
//    static idDynamicBlockAlloc<Character> stringDataAllocator = new idDynamicBlockAlloc<>(1 << 18, 128);
//    
    static final idVec4[] g_color_table = {
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(1.0f, 0.0f, 0.0f, 1.0f), // S_COLOR_RED
        new idVec4(0.0f, 1.0f, 0.0f, 1.0f), // S_COLOR_GREEN
        new idVec4(1.0f, 1.0f, 0.0f, 1.0f), // S_COLOR_YELLOW
        new idVec4(0.0f, 0.0f, 1.0f, 1.0f), // S_COLOR_BLUE
        new idVec4(0.0f, 1.0f, 1.0f, 1.0f), // S_COLOR_CYAN
        new idVec4(1.0f, 0.0f, 1.0f, 1.0f), // S_COLOR_MAGENTA
        new idVec4(1.0f, 1.0f, 1.0f, 1.0f), // S_COLOR_WHITE
        new idVec4(0.5f, 0.5f, 0.5f, 1.0f), // S_COLOR_GRAY
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f), // S_COLOR_BLACK
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),
        new idVec4(0.0f, 0.0f, 0.0f, 1.0f),};
    static final String[][] units = {
        {"B", "KB", "MB", "GB"},
        {"B/s", "KB/s", "MB/s", "GB/s"}
    };

    public static class idStr implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE
                = Integer.SIZE
                + Pointer.SIZE//Character.SIZE //pointer.//TODO:ascertain a char pointer size. EDIT: done.
                + Integer.SIZE
                + (Char.SIZE * STR_ALLOC_BASE);//TODO:char size
        public static final transient int BYTES = SIZE / Byte.SIZE;

        //private int len;//TODO:data is a pointer in the original class.
		private String data = "";//i·ro·ny: when your program breaks because of two measly double quotes. stu·pid·i·ty: when it takes you 2 days to find said "bug".
		//private int alloced;
        //private final char baseBuffer[] = new char[STR_ALLOC_BASE];
        //
        //

        public static idStr parseStr(final String str) {
            return new idStr(str);
        }

        public idStr() {
            Init();
        }

        public idStr(final idStr text) {
            int l;

            Init();
            l = text.Length();
            EnsureAlloced(l + 1);
//	strcpy( data, text.data );
            setData(text.getData());
            // setLen(l);
        }

        public idStr(final idStr text, int start, int end) {
            int i;
            int l;

            Init();
            if (end > text.Length()) {
                end = text.Length();
            }
            if (start > text.Length()) {
                start = text.Length();
            } else if (start < 0) {
                start = 0;
            }

            l = end - start;
            if (l < 0) {
                l = 0;
            }

            EnsureAlloced(l + 1);

//	for ( i = 0; i < l; i++ ) {
//		data[ i ] = text.data[ start + i ];
//	}
            setData(text.getData().substring(start, end));

//	data+= '\0';
            // setLen(l);
        }

        public idStr(final String text) {
            int l;

            Init();
            if (text != null) {
//		l = strlen( text );
                l = text.length();
                EnsureAlloced(l + 1);
//		strcpy( data, text );
                setData(text);
                // setLen(l);
            }
        }

        public idStr(final char[] text) {
            int l;

            Init();
            if (text != null) {
//		l = strlen( text );
                l = text.length;
                EnsureAlloced(l + 1);
//		strcpy( data, text );
                setData(ctos(text));
                // setLen(l);
            }
        }

        public idStr(final String text, int start, int end) {
            int i;
//	int l = strlen( text );
            int l = text.length();

            Init();
            if (end > l) {
                end = l;
            }
            if (start > l) {
                start = l;
            } else if (start < 0) {
                start = 0;
            }

            l = end - start;
            if (l < 0) {
                l = 0;
            }

            EnsureAlloced(l + 1);

            setData(text.substring(start, end));

//	data += '\0';
            // setLen(l);
        }

        public idStr(final boolean b) {
            Init();
            EnsureAlloced(2);
            setData(b ? "1" : "0");
//	data+= '\0';
            // setLen(1);
        }

        public idStr(final char c) {
            Init();
            EnsureAlloced(2);
            setData("" + c);
//	data+= '\0';
            // setLen(1);
        }

        public idStr(final int i) {
//	char []text=new char[ 64 ];
            String text = Integer.toString(i);
            int l = text.length();

            Init();
//	l = sprintf( text, "%d", i );
//	l = sprintf( text, "%d", i );

            EnsureAlloced(l + 1);
//	strcpy( data, text );
            setData(text);
            // setLen(l);
        }

        public idStr(final long u) {
            String text = Long.toString(u);
            int l = text.length();

            Init();
//	l = sprintf( text, "%u", u );
            EnsureAlloced(l + 1);
            //	strcpy( data, text );
            setData(text);
            // setLen(l);
        }

        public idStr(final float f) {
            String text = Float.toString(f);
            int l = text.length();

            Init();
//	l = idStr.snPrintf( text, sizeof( text ), "%f", f );
//	l = this.snPrintf( text, text.length , "%f", f );
//	while( l > 0 && text[l-1] == '0' ) text[--l] = '\0';
//	while( l > 0 && text[l-1] == '.' ) text[--l] = '\0';
            EnsureAlloced(l + 1);
            //	strcpy( data, text );
            setData(text);
            // setLen(l);
        }
//public						~idStr( void ) {
//	FreeData();
//}
//

        public int/*size_t*/ Size() {
            return /*sizeof( *this ) +*/ Allocated();
        }

        @Deprecated
        public char[] c_str() {
            return getData().toCharArray();
        }
//public	operator			const char *( void ) const;
//public	operator			const char *( void );
//

        public char oGet(int index) {
            assert ((index >= 0) && (index <= Length()));
            return getData().charAt(index);
        }

        public char oSet(int index, final char value) {
            assert ((index >= 0) && (index <= Length()));
            if (index == Length()
                    || 0 == Length()) {//just append if length == 0;
                setData(getData() + value);
            } else {
                setData(getData().substring(0, index) + value + getData().substring(index + 1));
            }
            return value;
        }
//public	char &				operator[]( int index );
//
//public	void				operator=( const idStr &text );

        public void oSet(final idStr text) {
            int l;

            l = text.Length();
            EnsureAlloced(l + 1, false);
//	memcpy( data, text.data, l );
//	data[l] = '\0';
            setData(text.getData());
            // setLen(l);
        }

//public	void				operator=( const char *text );
        public idStr oSet(final String text) {
            int l;

            if (text == null) {
                // safe behaviour if NULL
                EnsureAlloced(1, false);
                // setLen(0);
                return this;
            }

            l = text.length();
            EnsureAlloced(l + 1, false);
            setData(text);
            // setLen(l);
            return this;
        }

        public idStr oSet(final char[] text) {
            return this.oSet(ctos(text));
        }

//public	friend idStr		operator+( const idStr &a, const idStr &b );
        public idStr oPlus(final idStr b) {
            idStr result = new idStr(this.getData());
            result.Append(b.getData());
            return result;
        }

//public	friend idStr		operator+( const idStr &a, const char *b );
        public idStr oPlus(final String b) {
            idStr result = new idStr(this.getData());
            result.Append(b);
            return result;
        }

//public	friend idStr		operator+( const char *a, const idStr &b );
        public static idStr oPlus(final String a, final idStr b) {
            idStr result = new idStr(a);
            result.Append(b.getData());
            return result;
        }

//public	friend idStr		operator+( const idStr &a, const float b );
        public idStr oPlus(final float b) {
            String text;
            idStr result = new idStr(this.getData());

            text = String.format("%f", b);
            result.Append(text);

            return result;
        }

//public	friend idStr		operator+( const idStr &a, const int b );
//public	friend idStr		operator+( const idStr &a, const unsigned b );
        public idStr oPlus(final long b) {
            String text;
            idStr result = new idStr(this.getData());

            text = String.format("%d", b);
            result.Append(text);

            return result;
        }
//public	friend idStr		operator+( const idStr &a, const bool b );

        public idStr oPlus(final boolean b) {
            idStr result = new idStr(this.getData());
            result.Append(b ? "true" : "false");
            return result;
        }

//public	friend idStr		operator+( const idStr &a, const char b );
        public idStr oPlus(final char b) {
            idStr result = new idStr(this.getData());
            result.Append(b);
            return result;
        }

//public	 idStr		plus( final idStr a, final int b ){return plus(a, b);}
//public	idStr &				operator+=( const idStr &a );
        public idStr oPluSet(final idStr a) {
            Append(a);
            return this;
        }

//public	idStr &				operator+=( const char *a );
        public idStr oPluSet(final String a) {
            Append(a);
            return this;
        }

//public	idStr &				operator+=( const float a );
        public idStr oPluSet(final float a) {
            Append("" + a);
            return this;
        }

//public	idStr &				operator+=( const char a );
        public idStr oPluSet(final char a) {
            Append(a);
            return this;
        }
//public	idStr &				operator+=( const int a );
//public	idStr &				operator+=( const unsigned a );

        public idStr oPluSet(final long a) {
            Append("" + a);
            return this;
        }
//public	idStr &				operator+=( const bool a );

        public idStr oPluSet(final boolean a) {
            Append(Boolean.toString(a));
            return this;
        }
//
//						// case sensitive compare
//public	friend bool			operator==( const idStr &a, const idStr &b );
//public	friend bool			operator==( const idStr &a, const char *b );
//public	friend bool			operator==( const char *a, const idStr &b );
//
//public	friend bool			operator!=( const idStr &a, const idStr &b );
//public	friend bool			operator!=( const idStr &a, const char *b );
//public	friend bool			operator!=( const char *a, const idStr &b );

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Objects.hashCode(this.getData());
            return hash;
        }

        /**
         * The idStr equals basically compares to see if the string begins with
         * the other string.
         *
         * @see java.lang.String#startsWith(java.lang.String)
         * @param obj the <b>other</b> string.
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj.getClass() == String.class) {//when comparing pointers it's usually only about what they point to.
                if (!((String) obj).isEmpty()) {
                    return this.getData().startsWith((String) obj);//TODO:should we check first character against first character only?
                }
            }

            if (obj.getClass() == idStr.class) {
                if (!((idStr) obj).IsEmpty()) {
                    return this.getData().startsWith(((idStr) obj).getData());
                }
            }

            if (obj.getClass() == Character.class) {
                return this.getData().startsWith(((Character) obj).toString());
            }

            return false;
        }

        // case sensitive compare
        public int Cmp(final String text) {
            assert (text != null);
            return this.Cmp(getData(), text);
        }

        public int Cmp(final idStr text) {
            return Cmp(text.getData());
        }

        public int Cmpn(final String text, int n) {
            assert (text != null);
            return this.Cmpn(getData(), text, n);
        }

        public int CmpPrefix(final String text) {
            assert (null != text);
            return this.Cmpn(getData(), text, /*strlen( text )*/ text.length());
        }

        // case insensitive compare
        public int Icmp(final String text) {
            assert (text != null);
            return this.Icmp(getData(), text);
        }

        public int Icmp(final idStr text) {
            return this.Icmp(text.getData());
        }

        public int Icmpn(final String text, int n) {
            assert (text != null);
            return this.Icmpn(getData(), text, n);
        }

        public int IcmpPrefix(final String text) {
            assert (text != null);
            return this.Icmpn(getData(), text, text.length());
        }

        // case insensitive compare ignoring color
        public int IcmpNoColor(final String text) {
            assert (text != null);
            return this.IcmpNoColor(getData(), text);
        }

        public int IcmpNoColor(final idStr text) {

            return this.IcmpNoColor(text.getData());
        }

        // compares paths and makes sure folders come first
        public int IcmpPath(final String text) {
            assert (text != null);
            return this.IcmpPath(getData(), text);
        }

        public int IcmpnPath(final String text, int n) {
            assert (text != null);
            return this.IcmpnPath(getData(), text, n);
        }

        public int IcmpPrefixPath(final String text) {
            assert (text != null);
            return this.IcmpnPath(getData(), text, text.length());
        }

        public int Length() {
            return getData().length();
        }

        public int Allocated() {
            if ( /*data != baseBuffer* /true*/ Length() > 0) {
                //return alloced;
            	return Length() + 1;
            } else {
                return 0;
            }
        }

        public void Empty() {
            EnsureAlloced(1);
//	data ="\0";
            setData("");
            // setLen(0);
        }

        public boolean IsEmpty() {
//	return ( this.Cmp( data, "" ) == 0 );
            return getData().isEmpty();
        }

        public void Clear() {
            FreeData();
            Init();
        }

        public void Append(final idStr a) {
            Append(a.getData());
        }

        public void Append(final char a) {
            EnsureAlloced(Length() + 2);
            setData(getData() + a);
            // setLen(Length());//TODO:remove \0
//	data+= '\0';
        }

        public void Append(final String text) {
            int newLen;
            int i;

            newLen = Length() + text.length();
            EnsureAlloced(newLen + 1);
//	for ( i = 0; i < text.length; i++ ) {
//		data[ len + i ] = text[ i ];
//	}
            setData(getData() + text);
            // setLen(newLen);
//	data[ len ] = '\0';
        }

        public void Append(final char[] text) {
            Append(ctos(text));
        }
//public	void				Append( const char *text );

        public void Append(final String text, int l) {
            int newLen;
            int i;

            if (text != null && l > 0) {
                newLen = Length() + l;
                EnsureAlloced(newLen + 1);
//		for ( i = 0; text[ i ] && i < l; i++ ) {
//			data[ len + i ] = text[ i ];
//		}
                setData(getData().substring(0, Length()) + text.substring(0, l));
                // setLen(newLen);
//		data[ len ] = '\0';
            }
        }

        public void Insert(final char a, int index) {
            int i, l;

            if (index < 0) {
                index = 0;
            } else if (index > Length()) {
                index = Length();
            }

            l = 1;
            EnsureAlloced(Length() + l + 1);
//	for ( i = len; i >= index; i-- ) {
//		data[i+l] = data[i];
//	}
//	data[index] = a;
            setData(getData().substring(0, index) + a + getData().substring(index, getData().length()));
            // setLen(getLen() + 1);
        }

        public void Insert(final String text, int index) {
            int i, l;

            if (index < 0) {
                index = 0;
            } else if (index > Length()) {
                index = Length();
            }

//	l = strlen( text );
            l = text.length();
            EnsureAlloced(Length() + l + 1);
//	for ( i = len; i >= index; i-- ) {
//		data[i+l] = data[i];
//	}
//	for ( i = 0; i < l; i++ ) {
//		data[index+i] = text[i];
//	}
            setData(getData().substring(0, index) + text + getData().substring(index, getData().length()));
            // setLen(getLen() + l);
        }

        public void ToLower() {
//	for (int i = 0; data[i]; i++ ) {
//		if ( CharIsUpper( data[i] ) ) {
//			data[i] += ( 'a' - 'A' );
//		}
//	}
            setData(getData().toLowerCase());
        }

        public void ToUpper() {
//	for (int i = 0; data[i]; i++ ) {
//		if ( CharIsLower( data[i] ) ) {
//			data[i] -= ( 'a' - 'A' );
//		}
//	}
            setData(getData().toUpperCase());
        }

        public boolean IsNumeric() {
            return this.IsNumeric(getData());
        }

        public boolean IsColor() {
            return this.IsColor(getData());
        }

        public boolean HasLower() {
            return this.HasLower(getData());
        }

        public boolean HasUpper() {
            return this.HasUpper(getData());
        }

        public int LengthWithoutColors() {
            return this.LengthWithoutColors(getData());
        }

        public idStr RemoveColors() {
            setData(this.RemoveColors(getData()));
//            len = Length( data );
            // setLen(getData().length());
            return this;
        }

        public void CapLength(int newlen) {
            if (Length() <= newlen) {
                return;
            }
            setData(getData().substring(0, newlen));
            // setLen(newlen);
        }

        public void Fill(final char ch, int newlen) {
            EnsureAlloced(newlen + 1);
            // setLen(newlen);
//	memset( data, ch, len );
            setData("");
//        Arrays.fill(data, ch);
            for (int a = 0; a < newlen; a++) {
                setData(getData() + ch);
            }

//	data[ len ] = 0;
        }

        public int Find(final char c) {
            return Find(c, 0, -1);
        }

        public int Find(final char c, int start) {
            return Find(c, start, -1);
        }

        public int Find(final char c, int start, int end) {
            if (end == -1) {
                end = Length();
            }
            return this.FindChar(getData(), c, start, end);
        }

        public int Find(final String text) {
            return Find(text, true);
        }

        public int Find(final String text, boolean casesensitive) {
            return Find(text, casesensitive, 0);
        }

        public int Find(final String text, boolean casesensitive, int start) {
            return Find(text, casesensitive, start, -1);
        }

        public int Find(final String text, boolean casesensitive, int start, int end) {
            if (end == -1) {
                end = Length();
            }
            return this.FindText(getData(), text, casesensitive, start, end);
        }

        public boolean Filter(final String filter, boolean casesensitive) {
            return this.Filter(filter, getData(), casesensitive);
        }
        /*
         ============
         idStr::Last

         returns -1 if not found otherwise the index of the char
         ============
         */

        public int Last(final char c) {// return the index to the last occurance of 'c', returns -1 if not found
//	int i;
//	
//	for( i = Length(); i > 0; i-- ) {
//		if ( data[ i - 1 ] == c ) {
//			return i - 1;
//		}
//	}
//
//	return -1;
            return getData().lastIndexOf(c);
        }

        public idStr Left(int len, idStr result) {// store the leftmost 'len' characters in the result
            return Mid(0, len, result);
        }

        public idStr Right(int len, idStr result) {// store the rightmost 'len' characters in the result
            if (len >= Length()) {
                result.oSet(this);
                return result;
            }
            return Mid(Length() - len, len, result);
        }

        // store 'len' characters starting at 'start' in result
        public idStr Mid(int start, int len, idStr result) {
            int i;

            result.Empty();

            i = Length();
            if (i == 0 || len <= 0 || start >= i) {
                return null;
            }

            if (start + len >= i) {
                len = i - start;
            }

            result.Append(getData().substring(start), len);
            return result;
        }

        public idStr Left(int len) {// return the leftmost 'len' characters
            return Mid(0, len);
        }

        public idStr Right(int len) {// return the rightmost 'len' characters
            if (len >= Length()) {
                return this;
            }
            return Mid(Length() - len, len);
        }

        public idStr Mid(int start, int len) {// return 'len' characters starting at 'start'
            int i;
            idStr result = new idStr();

            i = Length();
            if (i == 0 || len <= 0 || start >= i) {
                return result;
            }

            if (start + len >= i) {
                len = i - start;
            }

//	result.Append( &data[ start ], len );
//	result.Append( &data[ start ], len );
            result.Append(getData().substring(start), len);
            return result;
        }

        public void StripLeading(final char c) {// strip char from front as many times as the char occurs
//	while( data[ 0 ] == c ) {
//		memmove( &data[ 0 ], &data[ 1 ], len );
//		len--;
//	}
            while (c == getData().charAt(0)) {
                // setLen(getLen() - 1);
                if (getData().length() == 1) {
                    setData("");
                    break;
                }
                setData(getData().substring(1));
            }
        }

        public void StripLeading(final String string) {// strip string from front as many times as the string occurs
            int l;

//	l = strlen( string );
            l = string.length();
            if (l > 0) {
                while (getData().startsWith(string)) {
//			memmove( data, data + l, len - l + 1 );
                    if (getData().length() == l) {
                        setData("");
                        break;
                    }
                    setData(getData().substring(l));
                }
            }
        }

        public boolean StripLeadingOnce(final String string) {// strip string from front just once if it occurs
            int l;

//	l = strlen( string );
            l = string.length();
//	if ( ( l > 0 ) && !Cmpn( string, l ) ) {
            if ((l > 0) && getData().startsWith(string)) {
//		memmove( data, data + l, len - l + 1 );
                setData(getData().substring(l));
                // setLen(Length());
                return true;
            }
            return false;
        }

        public void StripTrailing(final char c) {// strip char from end as many times as the char occurs

            for (int i = Length(); i > 0 && getData().charAt(i - 1) == c; i--) {
                // setLen(getLen() - 1);
                setData(getData().substring(0, Length() - 1));
            }
        }

        public void StripTrailing(final String string) {// strip string from end as many times as the string occurs
            int l;

//	l = strlen( string );
            l = string.length();
            if (l > 0) {
                while ((Length() >= l) && getData().endsWith(string)) {
                    setData(getData().substring(0, Length() - l));
                    // setLen(Length());
//			data[len] = '\0';
                }
            }
        }

        public boolean StripTrailingOnce(final String string) {// strip string from end just once if it occurs
            int l;

//	l = strlen( string );
            l = string.length();
            if ((l > 0) && (Length() >= l) && getData().endsWith(string)) {
                // setLen(getLen() - l);
//		data[len] = '\0';
                setData(getData().substring(0, Length() - 1));
                return true;
            }
            return false;
        }

        public void Strip(final char c) {// strip char from front and end as many times as the char occurs
            StripLeading(c);
            StripTrailing(c);
        }

        public void Strip(final String string) {// strip string from front and end as many times as the string occurs
            StripLeading(string);
            StripTrailing(string);
        }

        public void StripTrailingWhitespace() {// strip trailing white space characters
//	int i;

            // cast to unsigned char to prevent stripping off high-ASCII characters
//	for( i = Length(); i > 0 && (data[ i - 1 ]) <= ' '; i-- ) {
//		data[ i - 1 ] = '\0';
//		len--;
//	}
            setData(getData().trim());
            // setLen(getData().length());
        }
        /*
         ============
         idStr::StripQuotes

         Removes the quotes from the beginning and end of the string
         ============
         */

        public idStr StripQuotes() {// strip quotes around string
            if (getData().charAt(0) != '\"') {
                return this;
            }

            // Remove the trailing quote first
            if (getData().charAt(Length() - 1) == '\"') {
//		data[len-1] = '\0';
                setData(getData().substring(0, Length() - 2));
                // setLen(getLen() - 1);
            }

            // Strip the leading quote now
            // setLen(getLen() - 1);
            setData(getData().substring(1));
//	memmove( &data[ 0 ], &data[ 1 ], len );
//	data[len] = '\0';

            return this;
        }

        public void Replace(final String old, final String nw) {
            setData(getData().replaceAll(old, nw));
            // setLen(getData().length());
//	int		oldLen, newLen, i, j, count;
//	idStr	oldString=new idStr( data );
//
////	oldLen = strlen( old );
////	newLen = strlen( nw );
//	oldLen = old.length();
//	newLen = nw.length();
//
//	// Work out how big the new string will be
//	count = 0;
//	for( i = 0; i < oldString.Length(); i++ ) {
//		if( !idStr.Cmpn( oldString[i], old, oldLen ) ) {
//			count++;
//			i += oldLen - 1;
//		}
//	}
//
//	if( count!=0 ) {
//		EnsureAlloced( len + ( ( newLen - oldLen ) * count ) + 2, false );
//
//		// Replace the old data with the new data
//		for( i = 0, j = 0; i < oldString.Length(); i++ ) {
//			if( !idStr.Cmpn( oldString[i], old, oldLen ) ) {
//				memcpy( data + j, nw, newLen );
//				i += oldLen - 1;
//				j += newLen;
//			} else {
//				data[j] = oldString[i];
//				j++;
//			}
//		}
//		data[j] = 0;
//		len = strlen( data );
//	}
        }


        /*
         =====================================================================

         filename methods

         =====================================================================
         */
        // hash key for the filename (skips extension)
        public int FileNameHash() {
            int i;
            long hash;
            char letter;

            hash = 0;
            i = 0;
//	while( data[i] != '\0' ) {
            while (i < getData().length()) {
                letter = this.ToLower(getData().charAt(i));
                if (letter == '.') {
                    break;				// don't include extension
                }
                if (letter == '\\') {
                    letter = '/';
                }
                hash += (letter) * (i + 119);
                i++;
            }
            hash &= (FILE_HASH_SIZE - 1);
            return (int) hash;
        }

        public idStr BackSlashesToSlashes() {// convert slashes
//	int i;
//
//	for ( i = 0; i < len; i++ ) {
//		if ( data[ i ] == '\\' ) {
//			data[ i ] = '/';
//		}
//	}
            setData(getData().replaceAll("\\\\", "/"));
            return this;
        }

        public idStr SetFileExtension(final String extension) {// set the given file extension
            StripFileExtension();
//	if ( *extension != '.' ) {
            if (extension.charAt(0) != '.') {
                Append('.');
            }
            Append(extension);
            return this;
        }

        public idStr SetFileExtension(final idStr extension) {
            return SetFileExtension(extension.getData());
        }

        public idStr StripFileExtension() {// remove any file extension
            final int i;

//            for (i = len - 1; i >= 0; i--) {
//                if (data.charAt(i) == '.') {
////			data[i] = '\0';
//                    len = i;
//                    data = data.substring(0, len);
//                    break;
//                }
//            }
            i = getData().lastIndexOf('.');
            if (i > -1) {
                setData(getData().substring(0, i));
                // setLen(Length());
            }
            return this;
        }

        public idStr StripAbsoluteFileExtension() {// remove any file extension looking from front (useful if there are multiple .'s)
            int i;

            for (i = 0; i < Length(); i++) {
                if (getData().charAt(i) == '.') {
//			data[i] = '\0';
                    // setLen(i);
                    setData(getData().substring(0, Length() - 1));
                    break;
                }
            }

            return this;
        }

        public idStr DefaultFileExtension(final String extension) {// if there's no file extension use the default
            int i;

            // do nothing if the string already has an extension
//            for (i = len - 1; i >= 0; i--) {
            if (getData().contains(".")) {
                return this;
            }
//            }
            if (!extension.startsWith(".")) {
                Append('.');
            }
            Append(extension);
            return this;
        }

        public idStr DefaultPath(final char[] basepath) {// if there's no path use the default
//	if ( ( ( *this )[ 0 ] == '/' ) || ( ( *this )[ 0 ] == '\\' ) ) {
            if ((getData().charAt(0) == '/') || (getData().charAt(0) == '\\')) {
                // absolute path location
                return this;
            }

//	*this = basepath + *this;
            setData(new String(basepath) + getData());//TODO:bad..where to put the extension?
            return this;
        }

        public void AppendPath(final String text) {// append a partial path
            int pos;
            int i = 0;
            char[] dataArray = getData().toCharArray();

            if (text != null && text.length() > 0) {
                pos = Length();
                EnsureAlloced(Length() + text.length() + 2);

                if (pos != 0) {
                    if (dataArray[pos - 1] != '/') {
                        dataArray[pos++] = '/';
                    }
                }
                if (text.charAt(i) == '/') {
                    i++;
                }

                for (; i < text.length(); i++) {
                    if (text.charAt(i) == '\\') {
                        dataArray[pos++] = '/';
                    } else {
                        dataArray[pos++] = text.charAt(i);
                    }
                }
                // setLen(pos);
//		data[ pos ] = '\0';
                setData(ctos(dataArray));
            }
        }

        public void AppendPath(final idStr text) {
            Append(text.getData());
        }

        public idStr StripFilename() {// remove the filename from a path
            int pos;

            pos = Length() - 1;
            while ((pos > 0) && (getData().charAt(pos) != '/') && (getData().charAt(pos) != '\\')) {
                pos--;
            }

            if (pos < 0) {
                pos = 0;
            }

            CapLength(pos);
            return this;
        }

        // remove the path from the filename
        public idStr StripPath() {
            int pos;

            pos = Length();
            while ((pos > 0) && (getData().charAt(pos - 1) != '/') && (getData().charAt(pos - 1) != '\\')) {
                pos--;
            }

            idStr temp = Right(Length() - pos);
            setData(temp.getData());
            // setLen(getData().length());
            return this;
        }

        public void ExtractFilePath(idStr dest) {// copy the file path to another string
            int pos;

            //
            // back up until a \ or the start
            //
            pos = Length();
            while ((pos > 0) && (getData().charAt(pos - 1) != '/') && (getData().charAt(pos - 1) != '\\')) {
                pos--;
            }

            Left(pos, dest);
        }

        public void ExtractFileName(idStr dest) {// copy the filename to another string
            int pos;

            //
            // back up until a \ or the start
            //
            pos = Length() - 1;
            while ((pos > 0) && (getData().charAt(pos - 1) != '/') && (getData().charAt(pos - 1) != '\\')) {
                pos--;
            }

            Right(Length() - pos, dest);
        }

        public void ExtractFileBase(idStr dest) {// copy the filename minus the extension to another string
            int pos;
            int start;

            //
            // back up until a \ or the start
            //
            pos = Length() - 1;
            while ((pos > 0) && (getData().charAt(pos - 1) != '/') && (getData().charAt(pos - 1) != '\\')) {
                pos--;
            }

            start = pos;
            while ((pos < Length()) && (getData().charAt(pos) != '.')) {
                pos++;
            }

            Mid(start, pos - start, dest);
        }

        // copy the file extension to another string
        public void ExtractFileExtension(idStr dest) {
            int pos;

            //
            // back up until a . or the start
            //
            pos = Length() - 1;
            while ((pos > 0) && (getData().charAt(pos - 1) != '.')) {
                pos--;
            }

            if (pos == 0) {
                // no extension
                dest.Empty();
            } else {
                Right(Length() - pos, dest);
            }
        }

        public boolean CheckExtension(final String ext) {
            return this.CheckExtension(getData(), ext);
        }

        // char * methods to replace library functions
        public static int Length(final char[] s) {
            int i;
            for (i = 0; i < s.length && s[i] != 0; i++) ;

            return i;
        }

        public static char[] ToLower(char[] s) {
            for (int i = 0; i < s.length && s[i] != 0; i++) {
                if (CharIsUpper(s[i])) {
                    s[i] += ('a' - 'A');
                }
            }
            return s;
        }

        public static char[] ToUpper(char[] s) {
            for (int i = 0; i < s.length && s[i] != 0; i++) {
                if (CharIsLower(s[i])) {
                    s[i] -= ('a' - 'A');
                }
            }
            return s;
        }

        public static boolean IsNumeric(final String s) {
            try {
                Double.parseDouble(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        static boolean isdigit(char c) {
            if ('0' >= c && c <= '9') {
                return true;
            }
            return false;
        }

        public static boolean IsColor(final String s) {
            char[] sArray = s.toCharArray();
            return (sArray[0] == C_COLOR_ESCAPE && sArray.length > 1 && sArray[1] != ' ');
        }

        public static boolean HasLower(final String s) {
            if (s == null) {
                return false;
            }

            if (s.toUpperCase().equals(s)) {
                return false;
            }
//	while ( *s ) {
//		if ( CharIsLower( *s ) ) {
//			return true;
//		}
//		s++;
//	}

            return true;
        }

        public static boolean HasUpper(final String s) {
            if (s == null) {
                return false;
            }

            if (s.toLowerCase().equals(s)) {
                return false;
            }
//	while ( *s ) {
//		if ( CharIsLower( *s ) ) {
//			return true;
//		}
//		s++;
//	}

            return true;
        }

        public static int LengthWithoutColors(final String s) {
            int len;
            int p = 0;

            if (s == null) {
                return 0;
            }
//char[]sArray=s.toCharArray();	

            len = s.length();
//	p = s;
//	while( sArray[p]!=0 ) {
            if (idStr.IsColor(s)) {
                p += 2;
//			continue;
            }
//		p++;
//		len++;
//	}

            return len - p;
        }

        public static String RemoveColors(String s) {
            String string = "";

            for (int a = 0; a < s.length(); a++) {
                if (idStr.IsColor(s.substring(a))) {
                    a++;
                } else {
                    string += s.charAt(a);
                }
            }
//	*d = '\0';

            return string;
        }

        public static int Cmp(final char[] s1, final char[] s2) {
            return Cmp(ctos(s1), ctos(s2));
        }

        public static int Cmp(final idStr s1, final idStr s2) {
            return Cmp(s1.getData(), s2.getData());
        }

        public static int Cmp(final String s1, final String s2) {
            return ("" + s1).compareTo("" + s2);
        }

        public static int Cmpn(final String s1, final String s2, int n) {//TODO:see if we can return booleans
            if (isNotNullOrEmpty(s1) && isNotNullOrEmpty(s2)) {
                if (s1.length() >= n && s2.length() >= n) {
                    return Cmp(s1.substring(0, n), s2.substring(0, n));
                }
            }
            return 1;//not equal
        }

        public static int Icmp(final idToken t1, final String s2) {
            return Icmp(t1.getData(), s2);
        }

        public static int Icmp(final idStr t1, final String s2) {
            return Icmp(t1.getData(), s2);
        }

        public static int Icmp(final idStr t1, final idStr s2) {
            return Icmp(t1.getData(), s2.getData());
        }

        public static int Icmp(final char[] t1, final char[] s2) {
            return Icmp(ctos(t1), ctos(s2));
        }

        public static int Icmp(final String s1, final String s2) {
            return ("" + s1).compareToIgnoreCase("" + s2);
        }

        public static int Icmpn(final String s1, final String s2, int n) {
            if (isNotNullOrEmpty(s1) && isNotNullOrEmpty(s2)) {
                if (s1.length() >= n && s2.length() >= n) {
                    return Icmp(s1.substring(0, n), s2.substring(0, n));
                }
            }
            return 1;//not equal
        }

        public static int Icmpn(final idStr s1, final idStr s2, int n) {
            return Icmpn(s1.getData(), s2.getData(), n);
        }

        public static int Icmpn(final idStr s1, final String s2, int n) {
            return Icmpn(s1.getData(), s2, n);
        }

        public static int IcmpNoColor(final String s1, final String s2) {
            char[] s1Array = s1.toCharArray();
            char[] s2Array = s2.toCharArray();
            int c1 = 0, c2 = 0, d;

            do {
                while (idStr.IsColor(s1)) {
                    c1 += 2;
                }
                while (idStr.IsColor(s2)) {
                    c2 += 2;
                }
                c1++;
                c2++;

                d = s1Array[c1] - s2Array[c2];
                while (d != 0) {
                    if (c1 <= 'Z' && c1 >= 'A') {
                        d += ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    if (c2 <= 'Z' && c2 >= 'A') {
                        d -= ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    return (Math_h.INTSIGNBITNOTSET(d) << 1) - 1;
                }
            } while (c1 != 0);

            return 0;		// strings are equal
        }

        // compares paths and makes sure folders come first
        public static int IcmpPath(final String s1, final String s2) {
            return Paths.get(s1).compareTo(Paths.get(s2));//TODO: whats the "make sure fodlers come first" all about?

//            char[] s1Array = s1.toCharArray();
//            char[] s2Array = s2.toCharArray();
//            int i1 = 0, i2 = 0, d;
//            char c1, c2;
//
////#if 0
////#if !defined( _WIN32 )
////	idLib.common.Printf( "WARNING: IcmpPath used on a case-sensitive filesystem?\n" );
////#endif
//            do {
//                c1 = s1Array[i1++];
//                c2 = s2Array[i2++];
//
//                d = c1 - c2;
//                while (d != 0) {
//                    if (c1 <= 'Z' && c1 >= 'A') {
//                        d += ('a' - 'A');
//                        if (0 == d) {
//                            break;
//                        }
//                    }
//                    if (c1 == '\\') {
//                        d += ('/' - '\\');
//                        if (0 == d) {
//                            break;
//                        }
//                    }
//                    if (c2 <= 'Z' && c2 >= 'A') {
//                        d -= ('a' - 'A');
//                        if (0 == d) {
//                            break;
//                        }
//                    }
//                    if (c2 == '\\') {
//                        d -= ('/' - '\\');
//                        if (0 == d) {
//                            break;
//                        }
//                    }
//                    // make sure folders come first
//                    while (c1 != 0) {
//                        if (c1 == '/' || c1 == '\\') {
//                            break;
//                        }
//                        c1 = s1Array[i1++];
//                    }
//                    while (c2 != 0) {
//                        if (c2 == '/' || c2 == '\\') {
//                            break;
//                        }
//                        c2 = s2Array[i2++];
//                    }
//                    if (c1 != 0 && c2 == 0) {
//                        return -1;
//                    } else if (c1 == 0 && c2 != 0) {
//                        return 1;
//                    }
//                    // same folder depth so use the regular compare
//                    return (Math_h.INTSIGNBITNOTSET(d) << 1) - 1;
//                }
//            } while (c1 != 0);
//
//            return 0;
        }

        public static int IcmpnPath(final String s1, final String s2, int n) {// compares paths and makes sure folders come first
            char[] s1Array = s1.toCharArray();
            char[] s2Array = s2.toCharArray();
            int c1 = 0, c2 = 0, d;

//#if 0
//#if !defined( _WIN32 )
//	idLib.common.Printf( "WARNING: IcmpPath used on a case-sensitive filesystem?\n" );
//#endif
            assert (n >= 0);

            do {
                c1++;
                c2++;

                if (0 == n--) {
                    return 0;		// strings are equal until end point
                }

                d = s1Array[c1] - s2Array[c2];
                while (d != 0) {
                    if (c1 <= 'Z' && c1 >= 'A') {
                        d += ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    if (c1 == '\\') {
                        d += ('/' - '\\');
                        if (0 == d) {
                            break;
                        }
                    }
                    if (c2 <= 'Z' && c2 >= 'A') {
                        d -= ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    if (c2 == '\\') {
                        d -= ('/' - '\\');
                        if (0 == d) {
                            break;
                        }
                    }
                    // make sure folders come first
                    while (c1 != 0) {
                        if (c1 == '/' || c1 == '\\') {
                            break;
                        }
                        c1++;
                    }
                    while (c2 != 0) {
                        if (c2 == '/' || c2 == '\\') {
                            break;
                        }
                        c2++;
                    }
                    if (c1 != 0 && c2 == 0) {
                        return -1;
                    } else if (c1 == 0 && c2 != 0) {
                        return 1;
                    }
                    // same folder depth so use the regular compare
                    return (Math_h.INTSIGNBITNOTSET(d) << 1) - 1;
                }
            } while (c1 != 0);

            return 0;
        }

        /*
         ================
         idStr::Append

         never goes past bounds or leaves without a terminating 0
         ================
         */
        public static void Append(char[] dest, int size, final String src) {
            int l1;

            l1 = strLen(dest);
            if (l1 >= size) {
                idLib.common.Error("idStr::Append: already overflowed");
            }
            idStr.Copynz(dest, src, size - l1);
        }

        public static String Append(String dest, int size, final String src) {
            int l1, l2;

            l1 = dest.length();
            if (l1 >= size) {
                idLib.common.Error("idStr::Append: already overflowed");
                return null;
            }

            l2 = dest.length() + src.length();
            if (l2 > size) {
                return (dest + src).substring(0, size - l1);
            }

            return dest + src;
        }

        public static char[] Copynz(char[] dest, final String src, int destsize) {
            return Copynz(dest, 0, src, destsize);
        }

        /*
         =============
         idStr::Copynz
 
         Safe strncpy that ensures a trailing zero
         =============
         */
        public static char[] Copynz(char[] dest, int offset, final String src, int destsize) {
            if (null == src) {
                idLib.common.Warning("idStr::Copynz: NULL src");
                return null;
            }
            if (destsize < 1) {
                idLib.common.Warning("idStr::Copynz: destsize < 1");
                return null;
            }

//	strncpy( dest, src, destsize-1 );
            final int len = Math.min(destsize - 1, src.length());
            System.arraycopy(src.toCharArray(), 0, dest, offset, len);
            dest[offset + len] = 0;

            return dest;
        }

        public static void Copynz(char[] dest, final char[] src, int destsize) {
            Copynz(dest, ctos(src), destsize);
        }

//        @Deprecated
//        public static void Copynz(String dest, final String src, int destsize) {
//            if (null == src) {
//                idLib.common.Warning("idStr::Copynz: NULL src");
//                return;
//            }
//            if (destsize < 1) {
//                idLib.common.Warning("idStr::Copynz: destsize < 1");
//                return;
//            }
//
//            idStr.Copynz(dest.toCharArray(), src, destsize);
//        }
        public static void Copynz(String[] dest, final String src, int destsize) {
            if (null == src) {
                idLib.common.Warning("idStr::Copynz: NULL src");
                return;
            }
            if (destsize < 1) {
                idLib.common.Warning("idStr::Copynz: destsize < 1");
                return;
            }

            dest[0] = new String(idStr.Copynz((char[]) null, src, destsize));
        }

        public static void Copynz(StringBuilder dest, final String... src) {
            if (null == src) {
                idLib.common.Warning("idStr::Copynz: NULL src");
                return;
            }
            if (null == dest) {
                idLib.common.Warning("idStr::Copynz: NULL dest");
                return;
            }

            for (String s : src) {
                dest.append(s);
            }
        }
//public	static int			snPrintf( char *dest, int size, const char *fmt, ... ) id_attribute((format(printf,3,4)));

        public static int snPrintf(StringBuilder dest, int size, final String fmt, Object... args) {
            int len;
            final int bufferSize = 32000;
            StringBuilder buffer = new StringBuilder(bufferSize);
//
//	va_start( argptr, fmt );
//	len = vsprintf( buffer, fmt, argptr );
//	va_end( argptr );
            len = buffer.append(String.format(fmt, args)).length();
            if (len >= bufferSize) {
                idLib.common.Error("idStr::snPrintf: overflowed buffer");
            }
            if (len >= size) {
                idLib.common.Warning("idStr::snPrintf: overflow of %d in %d\n", len, size);
                len = size;
            }
//            idStr.Copynz(dest, buffer, size);
            dest.delete(0, dest.capacity());//clear
            dest.append(buffer);//TODO: use replace instead?
            return len;
        }

        public static int snPrintf(String[] dest, int size, final String fmt, Object... args) {
            throw new TODO_Exception();
//	int len;
//	va_list argptr;
//	char buffer[32000];	// big, but small enough to fit in PPC stack
//
//	va_start( argptr, fmt );
//	len = vsprintf( buffer, fmt, argptr );
//	va_end( argptr );
//	if ( len >= sizeof( buffer ) ) {
//		idLib::common->Error( "idStr::snPrintf: overflowed buffer" );
//	}
//	if ( len >= size ) {
//		idLib::common->Warning( "idStr::snPrintf: overflow of %i in %i\n", len, size );
//		len = size;
//	}
//	idStr::Copynz( dest, buffer, size );
//	return len;
        }

        public static int snPrintf(char[] dest, int size, final String fmt, Object... args) {
            return snPrintf(0, dest, size, fmt, args);
        }

        public static int snPrintf(int offset, char[] dest, int size, final String fmt, Object... args) {
            int length;
//            char[] argptr;
            StringBuilder buffer = new StringBuilder(32000);	// big, but small enough to fit in PPC stack

//	va_start( argptr, fmt );
//	len = vsprintf( buffer, fmt, argptr );
//	va_end( argptr );
            length = buffer.append(String.format(fmt, args)).length();
            if (length >= dest.length) {
                idLib.common.Error("idStr::snPrintf: overflowed buffer");
            }
            if (length >= size) {
                idLib.common.Warning("idStr::snPrintf: overflow of %d in %d\n", length, size);
                length = size;
            }
            idStr.Copynz(dest, offset, buffer.toString(), size);
            return length;
        }

        /*
         ============
         idStr::vsnPrintf

         vsnprintf portability:

         C99 standard: vsnprintf returns the number of characters (excluding the trailing
         '\0') which would have been written to the final string if enough space had been available
         snprintf and vsnprintf do not write more than size bytes (including the trailing '\0')

         win32: _vsnprintf returns the number of characters written, not including the terminating null character,
         or a negative value if an output error occurs. If the number of characters to write exceeds count, then count 
         characters are written and -1 is returned and no trailing '\0' is added.

         idStr::vsnPrintf: always appends a trailing '\0', returns number of characters written (not including terminal \0)
         or returns -1 on failure or if the buffer would be overflowed.
         ============
         */
        public static int vsnPrintf(String[] dest, int size, final String fmt, Object... args) {
            int ret = 0;

//#ifdef _WIN32
//#undef _vsnprintf
//	ret = _vsnprintf( dest, size-1, fmt, argptr );
//#define _vsnprintf	use_idStr_vsnPrintf
//#else
//#undef vsnprintf
//	ret = vsnprintf( dest, size, fmt, argptr );
//#define vsnprintf	use_idStr_vsnPrintf
//#endif
//            dest[size - 1] = '\0';
            ret = (dest[0] = String.format(fmt, args)).length();
            if (ret < 0 || ret >= size) {
                dest[0] = null;
                return -1;
            }
            return ret;
        }


        /*
         ============
         idStr::FindChar

         returns -1 if not found otherwise the index of the char
         ============
         */
        public static int FindChar(final String str, final char c) {
            return FindChar(str, c, 0);
        }

        public static int FindChar(final String str, final char c, int start) {
            return FindChar(str, c, start, -1);
        }

        public static int FindChar(final String str, final char c, int start, int end) {
            char[] strArray = str.toCharArray();
            int i;

            if (end == -1) {
//		end = strlen( str ) - 1;
                end = str.length();
            }
            for (i = start; i < end; i++) {
                if (strArray[i] == c) {
                    return i;
                }
            }
            return -1;
        }


        /*
         ============
         idStr::FindText

         returns -1 if not found otherwise the index of the text
         ============
         */
        public static int FindText(final String str, final String text) {
            return FindText(str, text, true);
        }

        public static int FindText(final String str, final String text, boolean casesensitive) {
            return FindText(str, text, casesensitive, 0);
        }

        public static int FindText(final String str, final String text, boolean casesensitive, int start) {
            return FindText(str, text, casesensitive, start, -1);
        }

        public static int FindText(final String str, final String text, boolean casesensitive, int start, int end) {
            if (end == -1) {
                end = str.length();
            }
            if (casesensitive) {
                return str.substring(start, end).indexOf(text);
            } else {
                return str.substring(start, end).toLowerCase().indexOf(text.toLowerCase());
            }
        }

        /*
         ============
         idStr::Filter

         Returns true if the string conforms the given filter.
         Several metacharacter may be used in the filter.

         *          match any string of zero or more characters
         ?          match any single character
         [abc...]   match any of the enclosed characters; a hyphen can
         be used to specify a range (e.g. a-z, A-Z, 0-9)

         ============
         */
        public /*static*/ boolean Filter(final String filter, String name, boolean casesensitive) {
            idStr buf = new idStr();
            int i, index;
            boolean found;
            int filterIndex = 0;

            while (filterIndex < filter.length()) {
                char filterChar = filter.charAt(filterIndex);
                if (filterChar == '*') {
                    filterIndex++;
                    buf.Empty();
                    for (i = 0; filterIndex < filter.length(); i++, filterChar = filter.charAt(filterIndex)) {
                        if (filterChar == '*' || filterChar == '?' || (filterChar == '[' && filter.charAt(filterIndex + 1) != '[')) {
                            break;
                        }
                        buf.oPluSet(filterChar);
                        if (filterChar == '[') {
                            filterIndex++;
                        }
                        filterIndex++;
                    }
                    if (buf.Length() > 0) {
                        index = /*new idStr(name).*/ Find(buf.getData(), casesensitive);//TODO:remove stuff
                        if (index == -1) {
                            return false;
                        }
//				name += index + strlen(buf);
                        name = name.substring(index + buf.Length(), name.length() - 1);
                    }
                } else if (filterChar == '?') {
                    filterIndex++;
//			name++;
                    name = name.substring(1);
                } else if (filterChar == '[') {
                    if (filter.charAt(filterIndex + 1) == '[') {
                        if (name.charAt(0) != '[') {
                            return false;
                        }
                        filterIndex += 2;
                        name = name.substring(1);
                    } else {
                        filterIndex++;
                        found = false;
                        while (filterIndex < filter.length() && !found) {
                            if (filterChar == ']' && filter.charAt(filterIndex + 1) != ']') {
                                break;
                            }
                            if (filter.charAt(filterIndex + 1) == '-' && filter.length() > filterIndex + 2
                                    && (filter.charAt(filterIndex + 2) != ']' || filter.charAt(filterIndex + 3) == ']')) {
                                if (casesensitive) {
                                    if (name.charAt(0) >= filterChar
                                            && name.charAt(0) <= filter.charAt(filterIndex + 2)) {
                                        found = true;
                                    }
                                } else {
//							if ( ::toupper(*name) >= ::toupper(*filterIndex) && ::toupper(*name) <= ::toupper(*(filterIndex+2)) ) {
                                    if (name.charAt(0) >= filterChar && name.charAt(0) <= filter.charAt(filterIndex + 2)) {
                                        found = true;
                                    }
                                }
                                filterIndex += 3;
                            } else {
                                if (casesensitive) {
                                    if (filterChar == name.charAt(0)) {
                                        found = true;
                                    }
                                } else {
//							if ( ::toupper(*filterIndex) == ::toupper(*name) ) {
                                    if (filterChar == name.charAt(0)) {
                                        found = true;
                                    }
                                }
                                filterIndex++;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                        while (filterIndex < filter.length()) {
                            if (filterChar == ']' && filter.charAt(filterIndex + 1) != ']') {
                                break;
                            }
                            filterIndex++;
                        }
                        filterIndex++;
                        name = name.substring(1);
                    }
                } else {
                    if (casesensitive) {
                        if (filterChar != name.charAt(0)) {
                            return false;
                        }
                    } else {
//				if ( ::toupper(*filterIndex) != ::toupper(*name) ) {
                        if (filterChar != name.charAt(0)) {
                            return false;
                        }
                    }
                    filterIndex++;
                    name = name.substring(1);
                }
            }
            return true;
        }


        /*
         =============
         idStr::StripMediaName

         makes the string lower case, replaces backslashes with forward slashes, and removes extension
         =============
         */
        public static void StripMediaName(final String name, idStr mediaName) {
//	char c;

            mediaName.Empty();

            for (char c : name.toCharArray()) {
                // truncate at an extension
                if (c == '.') {
                    break;
                }
                // convert backslashes to forward slashes
                if (c == '\\') {
                    mediaName.Append('/');
                } else {
                    mediaName.Append(idStr.ToLower(c));
                }
            }
        }

        public static boolean CheckExtension(final String name, final String ext) {
            int c1 = name.length() - 1, c2 = ext.length() - 1, d;
//TODO:double check if its working
            do {
                d = name.charAt(c1) - ext.charAt(c2);
                while (d != 0) {
                    if (c1 <= 'Z' && c1 >= 'A') {
                        d += ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    if (c2 <= 'Z' && c2 >= 'A') {
                        d -= ('a' - 'A');
                        if (0 == d) {
                            break;
                        }
                    }
                    return false;
                }
                c1--;
                c2--;
            } while (c1 > 0 && c2 > 0);

            return (c1 >= 0);
        }

        static int index = 0;
        static StringBuilder[] str = new StringBuilder[4];	// in case called by nested functions

        public static String FloatArrayToString(final float[] array, final int length, final int precision) {

            int i, n;
            String format;
            StringBuilder s;

            // use an array of string so that multiple calls won't collide
            s = str[index] = new StringBuilder(16384);
            index = (index + 1) & 3;

            format = String.format("%%.%df", precision);
            n = snPrintf(s, s.capacity(), format, array[0]);
//	if ( precision > 0 ) {
//		while( n > 0 && s[n-1] == '0' ) s[--n] = '\0';
//		while( n > 0 && s[n-1] == '.' ) s[--n] = '\0';
//	}
            format = String.format(" %%.%df", precision);
            for (i = 1; i < length; i++) {
                s.append(String.format(format, array[i]));
//                n += this.snPrintf(s + n, sizeof(str[0]) - n, format, array[i]);
//		if ( precision > 0 ) {
//			while( n > 0 && s[n-1] == '0' ) s[--n] = '\0';
//			while( n > 0 && s[n-1] == '.' ) s[--n] = '\0';
//		}
            }
            return s.toString();
        }

        // hash keys
        public static int Hash(final char[] string) {
            int i, hash = 0;
            for (i = 0; i < string.length && string[i] != '\0'; i++) {
                hash += (string[i]) * (i + 119);
            }
            return hash;
        }

        public static int Hash(final String string) {
            return Hash(string.toCharArray());
        }

        public static int Hash(final char[] string, int length) {
            int i, hash = 0;
            for (i = 0; i < length; i++) {
                hash += (string[i]) * (i + 119);
            }
            return hash;
        }

        // case insensitive
        public static int IHash(final char[] string) {
            int i, hash = 0;
            for (i = 0; i < string.length && string[i] != '\0'; i++) {//TODO:eliminate '\0' from char strings.
                hash += ToLower(string[i]) * (i + 119);
            }
            return hash;
        }

        // case insensitive
        public static int IHash(final char[] string, int length) {
            int i, hash = 0;
            for (i = 0; i < length; i++) {
                hash += ToLower(string[i]) * (i + 119);
            }
            return hash;
        }

        // character methods
        public static char ToLower(char c) {
            if (c <= 'Z' && c >= 'A') {
                return (char) (c + ('a' - 'A'));
            }
            return c;
        }

        public static char ToUpper(char c) {
            if (c >= 'a' && c <= 'z') {
                return (char) (c - ('a' - 'A'));
            }
            return c;
        }

        public static boolean CharIsPrintable(int c) {
            // test for regular ascii and western European high-ascii chars
            return (c >= 0x20 && c <= 0x7E) || (c >= 0xA1 && c <= 0xFF);
        }

        public static boolean CharIsLower(int c) {
            // test for regular ascii and western European high-ascii chars
            return (c >= 'a' && c <= 'z') || (c >= 0xE0 && c <= 0xFF);
        }

        public static boolean CharIsUpper(int c) {
            // test for regular ascii and western European high-ascii chars
            return (c <= 'Z' && c >= 'A') || (c >= 0xC0 && c <= 0xDF);
        }

        public static boolean CharIsAlpha(int c) {
            // test for regular ascii and western European high-ascii chars
            return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= 0xC0 && c <= 0xFF));
        }

        public static boolean CharIsNumeric(int c) {
            return (c <= '9' && c >= '0');
        }

        public static boolean CharIsNewLine(char c) {
            return (c == '\n' || c == '\r' /*|| c == '\v'*/);
        }

        public static boolean CharIsTab(char c) {
            return (c == '\t');
        }

        public static int ColorIndex(int c) {
            return (c & 15);
        }

        public static idVec4 ColorForIndex(int i) {
            return g_color_table[i & 15];
        }


        /*
         ============
         sprintf

         Sets the value of the string using a printf interface.
         ============
         */
//public	friend int			sprintf( idStr &dest, const char *fmt, ... );
//public <T>int sprintf( idStr string, final T...fmt) {
//return sprintf(string.data.toCharArray(), fmt);
//}
//public <T>int sprintf( char[] string, final T...fmt) {
//	int l = 0;
//	char[] argptr;
//	char []buffer=new char[32000];
//	
//	va_start( argptr, fmt );
//	l = idStr.vsnPrintf( buffer, sizeof(buffer)-1, fmt, argptr );
//	va_end( argptr );
//	buffer[sizeof(buffer)-1] = '\0';
////
//	string = buffer;
//	return l;
//}
/*
         ============
         vsprintf

         Sets the value of the string using a vprintf interface.
         ============
         */
//public	friend int			vsprintf( idStr &dest, const char *fmt, va_list ap );
        public int vsprintf(idStr string, final String fmt, Object... args) {//char[] argptr) {
            int l;
            String[] buffer = {null};//new char[32000];

            l = this.vsnPrintf(buffer, 32000, fmt, args);
//	buffer[buffer.length-1] = '\0';

//	string = buffer;
            string.oSet(buffer[0]);
            return l;
        }

        // reallocate string data buffer
        public void ReAllocate(int amount, boolean keepold) {
////            char[] newbuffer;
//            int newsize;
//            int mod;
//
//            //assert( data );
//            assert (amount > 0);
//
//            mod = amount % STR_ALLOC_GRAN;
//            if (0 != mod) {
//                newsize = amount;
//            } else {
//                newsize = amount + STR_ALLOC_GRAN - mod;
//            }
//            alloced = newsize;
//
////#ifdef USE_STRING_DATA_ALLOCATOR
////	newbuffer = stringDataAllocator.Alloc( alloced );
////#else
////            newbuffer = new char[alloced];
////#endif
////            if ( keepold && data ) {
////		data[ len ] = '\0';
////		strcpy( newbuffer, data );
////            }
////
////            if ( data && data != baseBuffer ) {
////#ifdef USE_STRING_DATA_ALLOCATOR
////		stringDataAllocator.Free( data );
////#else
////		delete [] data;
////#endif
////            }
////	data = newbuffer;
        }

        public void FreeData() {// free allocated string memory
            if (getData() != null /*&& data != baseBuffer*/) {
//#ifdef USE_STRING_DATA_ALLOCATOR
//		stringDataAllocator.Free( data );
//#else
//		delete[] data;
//#endif
//		data = baseBuffer;
            }
        }

        // format value in the given measurement with the best unit, returns the best unit
        public int BestUnit(final String format, float value, Measure_t measure) {
            int unit = 1;
            while (unit <= 3 && (1 << (unit * 10) < value)) {
                unit++;
            }
            unit--;
            value /= 1 << (unit * 10);
//	sprintf( *this, format, value );
            setData(String.format(format, value));
            setData(getData() + " ");
            setData(getData() + units[measure.ordinal()][unit]);//TODO:ordinal??
            return unit;
        }
        // format value in the requested unit and measurement

        public void SetUnit(final String format, float value, int unit, Measure_t measure) {
            value /= 1 << (unit * 10);
            //	sprintf( *this, format, value );
            setData(String.format(format, value));
            setData(getData() + " ");
            setData(getData() + units[measure.ordinal()][unit]);
        }

        public static void InitMemory() {
//#ifdef USE_STRING_DATA_ALLOCATOR
//	stringDataAllocator.Init();
//#endif
        }

        public static void ShutdownMemory() {
//#ifdef USE_STRING_DATA_ALLOCATOR
//	stringDataAllocator.Shutdown();
//#endif
        }

        public static void PurgeMemory() {
//#ifdef USE_STRING_DATA_ALLOCATOR
//	stringDataAllocator.FreeEmptyBaseBlocks();
//#endif
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            int len = buffer.getInt();
            buffer.getInt();//skip
//            this.alloced = buffer.getInt();
            final char baseBuffer[] = new char[STR_ALLOC_BASE];
            buffer.asCharBuffer().get(baseBuffer);
            setData(new String(baseBuffer));
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static class ShowMemoryUsage_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ShowMemoryUsage_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
//#ifdef USE_STRING_DATA_ALLOCATOR
                idLib.common.Printf("%6d KB string memory (%d KB free in %d blocks, %d empty base blocks)\n");
//                        stringDataAllocator.GetBaseBlockMemory() >> 10,
//                        stringDataAllocator.GetFreeBlockMemory() >> 10,
//                        stringDataAllocator.GetNumFreeBlocks(),
//                        stringDataAllocator.GetNumEmptyBaseBlocks());
//#endif
            }
        };

        public int DynamicMemoryUsed() {
//	return ( data == baseBuffer ) ? 0 : alloced;
            return Allocated();
        }

        static class formatList_t {

            int gran;
            int count;

            public formatList_t(int gran, int count) {
                this.gran = gran;
                this.count = count;
            }
        }
        // elements of list need to decend in size
        static formatList_t formatList[] = {new formatList_t(1000000000, 0),
            new formatList_t(1000000, 0),
            new formatList_t(1000, 0)};

        //int numFormatList = sizeof(formatList) / sizeof( formatList[0] );
        static int numFormatList = formatList.length;

        public static idStr FormatNumber(int number) {
            idStr string = new idStr();
            boolean hit;

            // reset
            for (int i = 0; i < numFormatList; i++) {
                formatList_t li = formatList[i];
                li.count = 0;
            }

            // main loop
            do {
                hit = false;

                for (int i = 0; i < numFormatList; i++) {
                    formatList_t li = formatList[i];

                    if (number >= li.gran) {
                        li.count++;
                        number -= li.gran;
                        hit = true;
                        break;
                    }
                }
            } while (hit);

            // print out
            boolean found = false;

            for (int i = 0; i < numFormatList; i++) {
                formatList_t li = formatList[i];

                if (li.count != 0) {
                    if (!found) {
                        string.oPluSet(va("%d,", li.count));
                    } else {
//				string += va( "%3.3i,", li.count );
                        string.oPluSet(va("%3.3i,", li.count));
                    }
                    found = true;
                } else if (found) {
//			string += va( "%3.3i,", li->count );
                    string.oPluSet(va("%3.3i,", li.count));
                }
            }

            if (found) {
//		string += va( "%3.3i", number );
                string.oPluSet(va("%3.3i,", number));
            } else {
//		string += va( "%d", number );
                string.oPluSet(va("%d,", number));
            }

            // pad to proper size
            int count = 11 - string.Length();

            for (int i = 0; i < count; i++) {
                string.Insert(" ", 0);
            }

            return string;
        }

        protected void Init() {
            //alloced = STR_ALLOC_BASE;
//	data = baseBuffer;
//	data[ 0 ] = '\0';
            setData("");
            // setLen(Length());
//#ifdef ID_DEBUG_UNINITIALIZED_MEMORY
//	memset( baseBuffer, 0, sizeof( baseBuffer ) );
//#endif
        }									// initialize string using base buffer

        protected void EnsureAlloced(int amount) {
//            EnsureAlloced(amount, true);
        }

        protected void EnsureAlloced(int amount, boolean keepold) {// ensure string data buffer is large anough
//            if (amount > alloced) {
//                ReAllocate(amount, keepold);
//            }
        }

        public String substring(int beginIndex) {
            return getData().substring(beginIndex);
        }

        @Override
        @Deprecated
        public String toString() {
            // direct access replaced
        	// TODO: analyze indirect access as object.toString()
        	return getData();
        }

        public String getData() {
			return data;
		}

		protected void setData(String data) {
			if (data == null) {
				this.data = "";
			} else {
				this.data = data;
			}
		}

//		protected int getLen() {
//			return Length();
//		}
//
//		protected void // setLen(int len) {
//			if (len != Length()) {
//				throw new IllegalStateException("Wrong length");
//			}
//			this.len = len;
//		}
    }

    /*
     ============
     va

     does a varargs printf into a temp buffer
     NOTE: not thread safe
     ============
     */
//    @Deprecated
    public static String va(final String fmt, Object... args) {
//////	va_list argptr;
////        char[] argptr;
////        int index = 0;
////        char[][] string = new char[4][16384];	// in case called by nested functions
////        char[] buf;
////
////        buf = string[index];
////        index = (index + 1) & 3;
////
//////	va_start( argptr, fmt );
//////	vsprintf( buf, fmt, argptr );
//////	va_end( argptr );
////
//        return new String(buf);
        return String.format(fmt, args);
    }
}
