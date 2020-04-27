package neo.idlib.Text;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Token {

    /*
     ===============================================================================

     idToken is a token read from a file or memory with idLexer or idParser

     ===============================================================================
     */
    // token types
    public static final int TT_STRING             = 1;          // string
    public static final int TT_LITERAL            = 2;          // literal
    public static final int TT_NUMBER             = 3;          // number
    public static final int TT_NAME               = 4;          // name
    public static final int TT_PUNCTUATION        = 5;          // punctuation
    //                                        
    // number sub types                     
    public static final int TT_INTEGER            = 0x00001;    // integer
    public static final int TT_DECIMAL            = 0x00002;    // decimal number
    public static final int TT_HEX                = 0x00004;    // hexadecimal number
    public static final int TT_OCTAL              = 0x00008;    // octal number
    public static final int TT_BINARY             = 0x00010;    // binary number
    public static final int TT_LONG               = 0x00020;    // long int
    public static final int TT_UNSIGNED           = 0x00040;    // unsigned int
    public static final int TT_FLOAT              = 0x00080;    // floating point number
    public static final int TT_SINGLE_PRECISION   = 0x00100;    // float
    public static final int TT_DOUBLE_PRECISION   = 0x00200;    // double
    public static final int TT_EXTENDED_PRECISION = 0x00400;    // long double
    public static final int TT_INFINITE           = 0x00800;    // infinite 1.#INF
    public static final int TT_INDEFINITE         = 0x01000;    // indefinite 1.#IND
    public static final int TT_NAN                = 0x02000;    // NaN
    public static final int TT_IPADDRESS          = 0x04000;    // ip address
    public static final int TT_IPPORT             = 0x08000;    // ip port
    public static final int TT_VALUESVALID        = 0x10000;    // set if intvalue and floatvalue are valid

    // string sub type is the length of the string
    // literal sub type is the ASCII code
    // punctuation sub type is the punctuation id
    // name sub type is the length of the name
    public static class idToken extends idStr {
//	friend class idParser;
//	friend class idLexer;

        public    int     type;                   // token type
        public    int     subtype;                // token sub type
        public    int     line;                   // line in script the token was on
        public    int     linesCrossed;           // number of lines crossed in white space before token
        public    int     flags;                  // token flags, used for recursive defines
        //
        protected long    intValue;               // integer value
        protected double  floatValue;             // floating point value
        protected int     whiteSpaceStart_p;      // start of white space before token, only used by idLexer
        protected int     whiteSpaceEnd_p;        // end of white space before token, only used by idLexer
        protected idToken next;                   // next token in chain, only used by idParser
        //
        //

        public idToken() {
        }

        public idToken(final idToken token) {
            this.oSet(token);
        }

        //public					~idToken( void );
//
        @Override
        public void oSet(final idStr text) {
//             * static_cast < idStr * > (this) = text;
            super.oSet(text);
        }

        @Override
        public idStr oSet(final String text) {
//             * static_cast < idStr * > (this) = text;
            return super.oSet(text);
        }

        // double value of TT_NUMBER
        public double GetDoubleValue() {
            if (type != TT_NUMBER) {
                return 0.0;
            }
            if (0 == (subtype & TT_VALUESVALID)) {
                NumberValue();
            }
            return floatValue;
        }

        // float value of TT_NUMBER
        public float GetFloatValue() {
            return (float) GetDoubleValue();
        }

        public long GetUnsignedLongValue() {        // unsigned long value of TT_NUMBER
            if (type != TT_NUMBER) {
                return 0;
            }
            if (0 == (subtype & TT_VALUESVALID)) {
                NumberValue();
            }
            return intValue;
        }

        public int GetIntValue() {                // int value of TT_NUMBER
            return (int) GetUnsignedLongValue();
        }

        public boolean WhiteSpaceBeforeToken() {// returns length of whitespace before token
            return (whiteSpaceEnd_p > whiteSpaceStart_p);
        }

        public void ClearTokenWhiteSpace() {        // forget whitespace before token
            whiteSpaceStart_p = 0;
            whiteSpaceEnd_p = 0;
            linesCrossed = 0;
        }
//

        public void NumberValue() {                // calculate values for a TT_NUMBER
            int i, pow, c;
            boolean div;
            final char[] p;
            int pIndex = 0;
            double m;

            assert (type == TT_NUMBER);
            p = c_str();
            floatValue = 0;
            intValue = 0;
            // floating point number
            if ((subtype & TT_FLOAT) != 0) {
                if ((subtype & (TT_INFINITE | TT_INDEFINITE | TT_NAN)) != 0) {
                    if ((subtype & TT_INFINITE) != 0) {            // 1.#INF
                        int inf = 0x7f800000;
                        floatValue = (double) ((float) inf);//TODO:WHY THE DOUBLE CAST?
                    } else if ((subtype & TT_INDEFINITE) != 0) {    // 1.#IND
                        int ind = 0xffc00000;
                        floatValue = (double) ((float) ind);
                    } else if ((subtype & TT_NAN) != 0) {			// 1.#QNAN
                        int nan = 0x7fc00000;
                        floatValue = (double) ((float) nan);
                    }
                } else {
                    while ( /*p[pIndex]!=null &&*/p[pIndex] != '.' && p[pIndex] != 'e') {
                        floatValue = floatValue * 10.0 + (double) (p[pIndex] - '0');
                        pIndex++;
                    }
                    if (p[pIndex] == '.') {
                        pIndex++;
                        for (m = 0.1; pIndex < p.length && p[pIndex] != 'e'; pIndex++) {
                            floatValue = floatValue + (double) (p[pIndex] - '0') * m;
                            m *= 0.1;
                        }
                    }
                    if (pIndex < p.length && p[pIndex] == 'e') {
                        pIndex++;
                        if (p[pIndex] == '-') {
                            div = true;
                            pIndex++;
                        } else if (p[pIndex] == '+') {
                            div = false;
                            pIndex++;
                        } else {
                            div = false;
                        }

                        for (pow = 0; pIndex < p.length; pIndex++) {
                            pow = pow * 10 + (int) (p[pIndex] - '0');
                        }
                        for (m = 1.0, i = 0; i < pow; i++) {
                            m *= 10.0;
                        }
                        if (div) {
                            floatValue /= m;
                        } else {
                            floatValue *= m;
                        }
                    }
                }
                intValue = idMath.Ftol((float) floatValue);
            } else if ((subtype & TT_DECIMAL) != 0) {
                while (pIndex < p.length) {
                    intValue = intValue * 10 + (p[pIndex] - '0');
                    pIndex++;
                }
                floatValue = intValue;
            } else if ((subtype & TT_IPADDRESS) != 0) {
                c = 0;
                while (/*p[pIndex] &&*/p[pIndex] != ':') {
                    if (p[pIndex] == '.') {
                        while (c != 3) {
                            intValue = intValue * 10;
                            c++;
                        }
                        c = 0;
                    } else {
                        intValue = intValue * 10 + (p[pIndex] - '0');
                        c++;
                    }
                    pIndex++;
                }
                while (c != 3) {
                    intValue = intValue * 10;
                    c++;
                }
                floatValue = intValue;
            } else if ((subtype & TT_OCTAL) != 0) {
                // step over the first zero
                pIndex += 1;
                while (pIndex < p.length) {
                    intValue = (intValue << 3) + (p[pIndex] - '0');
                    pIndex++;
                }
                floatValue = intValue;
            } else if ((subtype & TT_HEX) != 0) {
                // step over the leading 0x or 0X
                pIndex += 2;
                while (pIndex < p.length) {
                    intValue <<= 4;
                    if (p[pIndex] >= 'a' && p[pIndex] <= 'f') {
                        intValue += p[pIndex] - 'a' + 10;
                    } else if (p[pIndex] >= 'A' && p[pIndex] <= 'F') {
                        intValue += p[pIndex] - 'A' + 10;
                    } else {
                        intValue += p[pIndex] - '0';
                    }
                    p[pIndex]++;
                }
                floatValue = intValue;
            } else if ((subtype & TT_BINARY) != 0) {
                // step over the leading 0b or 0B
                pIndex += 2;
                while (pIndex < p.length) {
                    intValue = (intValue << 1) + (p[pIndex] - '0');
                    pIndex++;
                }
                floatValue = intValue;
            }
            subtype |= TT_VALUESVALID;
        }

        // append character without adding trailing zero
        protected void AppendDirty(final char a) {
            EnsureAlloced(len + 2, true);
//	data[len++] = a;
            data += a;
            len++;
        }

        idToken oSet(final idToken token) {
            this.type = token.type;
            this.subtype = token.subtype;
            this.line = token.line;
            this.linesCrossed = token.linesCrossed;
            this.flags = token.flags;
            this.intValue = token.intValue;
            this.floatValue = token.floatValue;
            this.whiteSpaceStart_p = token.whiteSpaceStart_p;
            this.whiteSpaceEnd_p = token.whiteSpaceEnd_p;
            this.next = token.next;
            super.oSet(token);
            return this;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            //idStr
            if (getClass() != obj.getClass()) {
                return super.equals(obj);
            }

            final idToken other = (idToken) obj;
            return this.data.startsWith(other.data);
        }

    };
}
