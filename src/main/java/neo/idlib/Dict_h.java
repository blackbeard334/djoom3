package neo.idlib;

import java.util.Objects;
import static neo.TempDump.atof;
import static neo.TempDump.atoi;
import static neo.TempDump.btoi;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.Common.common;
import neo.framework.File_h.idFile;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_PUNCTUATION;
import static neo.idlib.Text.Token.TT_STRING;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrPool.idPoolStr;
import neo.idlib.containers.StrPool.idStrPool;
import neo.idlib.hashing.CRC32;
import static neo.idlib.hashing.CRC32.CRC32_FinishChecksum;
import static neo.idlib.hashing.CRC32.CRC32_InitChecksum;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Dict_h {

    /**
     * ===============================================================================
     *
     * Key/value dictionary
     *
     * This is a dictionary class that tracks an arbitrary number of key / value
     * pair combinations. It is used for map entity spawning, GUI state
     * management, and other things.
     *
     * Keys are compared case-insensitive.
     *
     * Does not allocate memory until the first key/value pair is added.
     *
     * ===============================================================================
     */
    public static class idKeyValue extends idDict implements Cloneable {
//	friend class idDict;

        private idPoolStr key;
        private idPoolStr value;
        //
        //

        public final idStr GetKey() {
            return key;
        }

        public final idStr GetValue() {
            return value;
        }

//
        @Override
        public long Allocated() {
            return key.Allocated() + value.Allocated();
        }

        @Override
        public long Size() {
            return /*sizeof( *this ) +*/ key.Size() + value.Size();
        }
//	public boolean				operator==( final idKeyValue &kv ) final { return ( key == kv.key && value == kv.value ); }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.key);
            hash = 71 * hash + Objects.hashCode(this.value);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idKeyValue other = (idKeyValue) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }

            return Objects.equals(this.value, other.value);
        }

        @Override
        public String toString() {
            return "idKeyValue{" + "key=" + key + ", value=" + value + '}';
        }       

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    public static class idDict {

        private idList<idKeyValue> args = new idList<>();
        private idHashIndex argHash = new idHashIndex();
        //        
        private static final idStrPool globalKeys = new idStrPool();
        private static final idStrPool globalValues = new idStrPool();
        //
        //

        public idDict() {
            args.SetGranularity(16);
            argHash.SetGranularity(16);
            argHash.Clear(128, 16);
        }

        // allow declaration with assignment
        public idDict(final idDict other) {
            this.oSet(other);
        }
//public						~idDict( );

        // set the granularity for the index
        public void SetGranularity(int granularity) {
            args.SetGranularity(granularity);
            argHash.SetGranularity(granularity);
        }

        // set hash size
        public void SetHashSize(int hashSize) {
            if (args.Num() == 0) {
                argHash.Clear(hashSize, 16);
            }
        }

        /*
         ================
         idDict::operator=

         clear existing key/value pairs and copy all key/value pairs from other
         ================
         */
        // clear existing key/value pairs and copy all key/value pairs from other
        public idDict oSet(final idDict other) {
            int i;

            // check for assignment to self
            if (this == other) {
                return this;
            }

            Clear();

            args = other.args;
            argHash = other.argHash;

            for (i = 0; i < args.Num(); i++) {
                args.oGet(i).key = globalKeys.CopyString(args.oGet(i).key);
                args.oGet(i).value = globalValues.CopyString(args.oGet(i).value);
            }

            return this;
        }

        /*
         ================
         idDict::Copy

         copy all key value pairs without removing existing key/value pairs not present in the other dict
         ================
         */
        // copy from other while leaving existing key/value pairs in place
        public void Copy(final idDict other) throws idException {
            int i, n;
            int[] found;
            idKeyValue kv = new idKeyValue();

            // check for assignment to self
            if (this == other) {
                return;
            }

            n = other.args.Num();

            if (args.Num() != 0) {
                found = new int[other.args.Num()];
                for (i = 0; i < n; i++) {
                    found[i] = FindKeyIndex(other.args.oGet(i).GetKey() + "");
                }
            } else {
                found = null;
            }

            for (i = 0; i < n; i++) {
                if (found != null && found[i] != -1) {
                    // first set the new value and then free the old value to allow proper self copying
                    idPoolStr oldValue = args.oGet(found[i]).value;
                    args.oGet(found[i]).value = globalValues.CopyString(other.args.oGet(i).value);
                    globalValues.FreeString(oldValue);
                } else {
                    kv.key = globalKeys.CopyString(other.args.oGet(i).key);
                    kv.value = globalValues.CopyString(other.args.oGet(i).value);
                    argHash.Add(argHash.GenerateKey(kv.GetKey() + "", false), args.Append(kv));
                }
            }
        }

        /*
         ================
         idDict::TransferKeyValues

         clear existing key/value pairs and transfer key/value pairs from other
         ================
         */
        // clear existing key/value pairs and transfer key/value pairs from other
        public void TransferKeyValues(idDict other) throws idException {
            int i, n;

            if (this == other) {
                return;
            }

            if (other.args.Num() != 0 && other.args.oGet(0).key.GetPool() != globalKeys) {
                common.FatalError("idDict::TransferKeyValues: can't transfer values across a DLL boundary");
                return;
            }

            Clear();

            n = other.args.Num();
            args.SetNum(n);
            try {
                for (i = 0; i < n; i++) {
                    args.oSet(i, other.args.oGet(i).clone());
                }
            } catch (CloneNotSupportedException ex) {
                throw new idException(ex);
            }
            argHash.oSet(other.argHash);

            other.args.Clear();
            other.argHash.Free();
        }

        // parse dict from parser
        public boolean Parse(idParser parser) throws idException {
            idToken token = new idToken();
            idToken token2 = new idToken();
            boolean errors;

            errors = false;

            parser.ExpectTokenString("{");
            parser.ReadToken(token);
            while ((token.type != TT_PUNCTUATION) || !token.equals("}")) {
                if (token.type != TT_STRING) {
                    parser.Error("Expected quoted string, but found '%s'", token);
                }

                if (!parser.ReadToken(token2)) {
                    parser.Error("Unexpected end of file");
                }

                if (FindKey(token) != null) {
                    parser.Warning("'%s' already defined", token);
                    errors = true;
                }
                Set(token, token2);

                if (!parser.ReadToken(token)) {
                    parser.Error("Unexpected end of file");
                }
            }

            return !errors;
        }

        // copy key/value pairs from other dict not present in this dict
        public void SetDefaults(final idDict dict) throws idException {
            final int n = dict.args.Num();

            for (int i = 0; i < n; i++) {
                idKeyValue def = dict.args.oGet(i);
                idKeyValue kv = FindKey(def.GetKey() + "");//TODO:override toString?
                idKeyValue newkv = new idKeyValue();
                if (null == kv) {
                    newkv.key = globalKeys.CopyString(def.key);
                    newkv.value = globalValues.CopyString(def.value);
                    argHash.Add(argHash.GenerateKey(newkv.GetKey() + "", false), args.Append(newkv));
                }
            }
        }

        // clear dict freeing up memory
        public void Clear() {
            int i;

            for (i = 0; i < args.Num(); i++) {
                globalKeys.FreeString(args.oGet(i).key);
                globalValues.FreeString(args.oGet(i).value);
            }

            args.Clear();
            argHash.Free();
        }

        // print the dict
        public void Print() throws idException {
            int i;
            int n;

            n = args.Num();
            for (i = 0; i < n; i++) {
                idLib.common.Printf("%s = %s\n", args.oGet(i).GetKey().toString(), args.oGet(i).GetValue().toString());
            }
        }

        public long Allocated() {
            int i;
            long size;

            size = args.Allocated() + argHash.Allocated();
            for (i = 0; i < args.Num(); i++) {
                size += args.oGet(i).Size();
            }

            return size;
        }

        public long Size() {
            return /*sizeof( * this) +*/ Allocated();
        }

        /**
         * @deprecated make sure the <b>.toString()</b> methods output the
         * strings we need.
         */
        @Deprecated
        public void Set(final Object key, final Object value) throws idException {
            Set(key.toString(), value.toString());//TODO:check if toString is sufficient instead of checking whether it's an idStr first?
        }

        public void Set(final String key, final String value) throws idException {
            int i;
            idKeyValue kv = new idKeyValue();

            if (key == null || key.length() == 0 || key.charAt(0) == '\0') {
                return;
            }

            i = FindKeyIndex(key);
            if (i != -1) {
                // first set the new value and then free the old value to allow proper self copying
                idPoolStr oldValue = args.oGet(i).value;
                args.oGet(i).value = globalValues.AllocString(value);
                globalValues.FreeString(oldValue);
            } else {
                kv.key = globalKeys.AllocString(key);
                kv.value = globalValues.AllocString(value);
                argHash.Add(argHash.GenerateKey("" + kv.GetKey(), false), args.Append(kv));
            }
        }

        public void SetFloat(final String key, float val) throws idException {
            Set(key, va("%f", val));
        }

        public void SetInt(final String key, int val) throws idException {
            Set(key, va("%d", val));
        }

        public void SetBool(final String key, boolean val) throws idException {
            Set(key, va("%d", btoi(val)));
        }

        public void SetVector(final String key, final idVec3 val) throws idException {
            Set(key, val.ToString());
        }

        public void SetVec2(final String key, final idVec2 val) throws idException {
            Set(key, val.ToString());
        }

        public void SetVec4(final String key, final idVec4 val) throws idException {
            Set(key, val.ToString());
        }

        public void SetAngles(final String key, final idAngles val) throws idException {
            Set(key, val.ToString());
        }

        public void SetMatrix(final String key, final idMat3 val) throws idException {
            Set(key, val.ToString());
        }

        public String GetString(final String key) throws idException {
            return GetString(key, "");
        }

        // these return default values of 0.0, 0 and false
        public String GetString(final String key, final String defaultString) throws idException {
            idKeyValue kv = FindKey(key);
            if (kv != null) {
                return kv.GetValue().toString();
            }
            return defaultString;
        }

        public float GetFloat(final String key) throws idException {
            return GetFloat(key, "0");
        }

        public float GetFloat(final String key, final String defaultString /*= "0"*/) throws idException {
            return atof(GetString(key, defaultString));
        }

        public int GetInt(final String key) throws idException {
            return GetInt(key, "0");
        }

        public int GetInt(final String key, final String defaultString) throws idException {
            return atoi(GetString(key, defaultString));
        }

        public boolean GetBool(final String key) throws idException {
            return GetBool(key, "0");
        }

        public boolean GetBool(final String key, final String defaultString) throws idException {
            return Boolean.parseBoolean(GetString(key, defaultString));
        }

        public idVec3 GetVector(final String key, final String defaultString) throws idException {
            idVec3 out = new idVec3();
            GetVector(key, defaultString, out);
            return out;
        }

        public idVec3 GetVector(final String key) throws idException {
            return GetVector(key, null);
        }

        public idVec2 GetVec2(final String key, final String defaultString) throws idException {
            idVec2 out = new idVec2();
            GetVec2(key, defaultString, out);
            return out;
        }

        public idVec2 GetVec2(final String key) throws idException {
            return GetVec2(key, null);
        }

        public idVec4 GetVec4(final String key, final String defaultString) throws idException {
            idVec4 out = new idVec4();
            GetVec4(key, defaultString, out);
            return out;
        }

        public idVec4 GetVec4(final String key) throws idException {
            return GetVec4(key, null);
        }

        public idAngles GetAngles(final String key, final String defaultString) throws idException {
            idAngles out = new idAngles();
            GetAngles(key, defaultString, out);
            return out;
        }

        public idAngles GetAngles(final String key) throws idException {
            return GetAngles(key, null);
        }

        public idMat3 GetMatrix(final String key, final String defaultString) throws idException {
            idMat3 out = new idMat3();
            GetMatrix(key, defaultString, out);
            return out;
        }

        static void WriteString(final String s, idFile f) throws idException {
            int len = s.length();
            if (len >= MAX_STRING_CHARS - 1) {
                idLib.common.Error("idDict::WriteToFileHandle: bad string");
            }
            f.WriteString(s);//, len + 1);
        }

        public boolean GetString(final String key, final String defaultString, final String[] out) throws idException {
            idKeyValue kv = FindKey(key);
            if (kv != null) {
                out[0] = kv.GetValue().toString();
                return true;
            }
            out[0] = defaultString;
            return false;
        }

        public boolean GetString(final String key, final String defaultString, idStr out) throws idException {
            idKeyValue kv = FindKey(key);
            if (kv != null) {
                out.oSet(kv.GetValue());
                return true;
            }
            out.oSet(defaultString);
            return false;
        }

        public boolean GetFloat(final String key, final String defaultString, float[] out) throws idException {
            String[] s = new String[1];
            boolean found;

            found = GetString(key, defaultString, s);
            out[0] = atof(s[0]);
            return found;
        }

        public boolean GetInt(final String key, final String defaultString, int[] out) throws idException {
            String[] s = new String[1];
            boolean found;

            found = GetString(key, defaultString, s);
            out[0] = atoi(s[0]);
            return found;
        }

        public boolean GetBool(final String key, final String defaultString, boolean[] out) throws idException {
            String[] s = new String[1];
            boolean found;

            found = GetString(key, defaultString, s);
            out[0] = Boolean.parseBoolean(s[0]);
            return found;
        }

        public boolean GetVector(final String key, String defaultString, idVec3 out) throws idException {
            boolean found;
            String[] s = {null};

            if (null == defaultString) {
                defaultString = "0 0 0";
            }

            found = GetString(key, defaultString, s);
            out.Zero();

            String[] sscanf = s[0].split(" ");
            if (sscanf.length > 2) {
                out.x = atof(sscanf[0]);
                out.y = atof(sscanf[1]);
                out.z = atof(sscanf[2]);
            }
//	sscanf( s, "%f %f %f", &out.x, &out.y, &out.z );
            return found;
        }

        public boolean GetVec2(final String key, String defaultString, idVec2 out) throws idException {
            boolean found;
            String[] s = new String[1];

            if (null == defaultString) {
                defaultString = "0 0";
            }

            found = GetString(key, defaultString, s);
            out.Zero();

            String[] sscanf = s[0].split(" ");
            out.x = atof(sscanf[0]);
            out.y = atof(sscanf[1]);
//	sscanf( s, "%f %f", &out.x, &out.y );
            return found;
        }

        public boolean GetVec4(final String key, String defaultString, idVec4 out) throws idException {
            boolean found;
            String[] s = new String[1];

            if (null == defaultString) {
                defaultString = "0 0 0 0";
            }

            found = GetString(key, defaultString, s);
            out.Zero();

            String[] sscanf = s[0].split(" ");
            out.x = atof(sscanf[0]);
            out.y = atof(sscanf[1]);
            out.z = atof(sscanf[2]);
            out.w = atof(sscanf[3]);
//	sscanf( s, "%f %f %f", &out.x, &out.y, &out.z , &out.w);
            return found;
        }

        public boolean GetAngles(final String key, String defaultString, idAngles out) throws idException {
            boolean found;
            String[] s = new String[1];

            if (null == defaultString) {
                defaultString = "0 0 0";
            }

            found = GetString(key, defaultString, s);
            out.Zero();

            String[] sscanf = s[0].split(" ");
            out.pitch = atof(sscanf[0]);
            out.yaw = atof(sscanf[1]);
            out.roll = atof(sscanf[2]);
//	sscanf( s, "%f %f %f", &out.pitch, &out.yaw, &out.roll );
            return found;
        }

        public boolean GetMatrix(final String key, String defaultString, idMat3 out) throws idException {
            boolean found;
            String[] s = new String[1];

            if (null == defaultString) {
                defaultString = "1 0 0 0 1 0 0 0 1";
            }

            found = GetString(key, defaultString, s);
            out.Zero();

            String[] sscanf = s[0].split(" ");
            out.oGet(0).x = atof(sscanf[0]);
            out.oGet(0).y = atof(sscanf[1]);
            out.oGet(0).z = atof(sscanf[2]);
            out.oGet(1).x = atof(sscanf[3]);
            out.oGet(1).y = atof(sscanf[4]);
            out.oGet(1).z = atof(sscanf[5]);
            out.oGet(2).x = atof(sscanf[6]);
            out.oGet(2).y = atof(sscanf[7]);
            out.oGet(2).z = atof(sscanf[8]);
//	sscanf( s, "%f %f %f %f %f %f %f %f %f", &out[0].x, &out[0].y, &out[0].z, &out[1].x, &out[1].y, &out[1].z, &out[2].x, &out[2].y, &out[2].z );
            return found;
        }

        public int GetNumKeyVals() {
            return args.Num();
        }

        public idKeyValue GetKeyVal(int index) {
            if (index >= 0 && index < args.Num()) {
                return args.oGet(index);
            }
            return null;
        }

        // returns the key/value pair with the given key
        // returns NULL if the key/value pair does not exist
        public idKeyValue FindKey(final String key) throws idException {
            int i, hash;

            if (key == null || key.isEmpty()/*[0] == '\0'*/) {
                idLib.common.DWarning("idDict::FindKey: empty key");
                return null;
            }

            hash = argHash.GenerateKey(key, false);
            for (i = argHash.First(hash); i != -1; i = argHash.Next(i)) {
                if (args.oGet(i).GetKey().Icmp(key) == 0) {
                    return args.oGet(i);
                }
            }

            return null;
        }

        public idKeyValue FindKey(final idStr key) throws idException {
            return FindKey(key.toString());
        }

        // returns the index to the key/value pair with the given key
        // returns -1 if the key/value pair does not exist
        public int FindKeyIndex(final String key) throws idException {

            if (key == null || key.length() < 1/*[0] == '\0'*/) {
                idLib.common.DWarning("idDict::FindKeyIndex: empty key");
                return 0;
            }

            int hash = argHash.GenerateKey(key, false);
            for (int i = argHash.First(hash); i != -1; i = argHash.Next(i)) {
                if (args.oGet(i).GetKey().Icmp(key) == 0) {
                    return i;
                }
            }

            return -1;
        }

        // delete the key/value pair with the given key
        public void Delete(final String key) {
            int hash, i;

            hash = argHash.GenerateKey(key, false);
            for (i = argHash.First(hash); i != -1; i = argHash.Next(i)) {
                if (args.oGet(i).GetKey().Icmp(key) == 0) {
                    globalKeys.FreeString(args.oGet(i).key);
                    globalValues.FreeString(args.oGet(i).value);
                    args.RemoveIndex(i);
                    argHash.RemoveIndex(hash, i);
                    break;
                }
            }
//
//#if 0
//	// make sure all keys can still be found in the hash index
//	for ( i = 0; i < args.Num(); i++ ) {
//		assert( FindKey( args[i].GetKey() ) != NULL );
//	}
//#endif
        }

        public void Delete(final idStr key) {
            if (key != null) {
                Delete(key.toString());
            }
        }

        public idKeyValue MatchPrefix(final String prefix) {
            return this.MatchPrefix(prefix, null);
        }
        // finds the next key/value pair with the given key prefix.
        // lastMatch can be used to do additional searches past the first match.

        public idKeyValue MatchPrefix(final String prefix, final idKeyValue lastMatch) {
            int i;
            int len;
            int start;

            assert (prefix != null);
            len = prefix.length();

            start = -1;
            if (lastMatch != null) {
                start = args.FindIndex(lastMatch);
                assert (start >= 0);
                if (start < 1) {
                    start = 0;
                }
            }

            for (i = start + 1; i < args.Num(); i++) {
                if (0 == args.oGet(i).GetKey().Icmpn(prefix, len)) {
                    return args.oGet(i);
                }
            }
            return null;
        }

        // randomly chooses one of the key/value pairs with the given key prefix and returns it's value
        public String RandomPrefix(final String prefix, idRandom random) {
            int count;
            final int MAX_RANDOM_KEYS = 2048;
            final String[] list = new String[MAX_RANDOM_KEYS];
            idKeyValue kv;

//            list[0] = "";
            for (count = 0, kv = MatchPrefix(prefix); kv != null && count < MAX_RANDOM_KEYS; kv = MatchPrefix(prefix, kv)) {
                list[count++] = String.copyValueOf(kv.GetValue().c_str());
            }
            return list[random.RandomInt(count)];
        }

        public void WriteToFileHandle(idFile f) throws idException {
            int c = LittleLong(args.Num());
            f.WriteInt(c);//, sizeof(c));
            for (int i = 0; i < args.Num(); i++) {	// don't loop on the swapped count use the original
                WriteString(args.oGet(i).GetKey().toString(), f);
                WriteString(args.oGet(i).GetValue().toString(), f);
            }
        }

        static idStr ReadString(idFile f) throws idException {
            char[] str = new char[MAX_STRING_CHARS];
            short[] c = {0};
            int len;

            for (len = 0; len < MAX_STRING_CHARS; len++) {
                f.ReadChar(c);//, 1);
                str[len] = (char) c[0];
                if (str[len] == 0) {
                    break;
                }
            }
            if (len == MAX_STRING_CHARS) {
                idLib.common.Error("idDict::ReadFromFileHandle: bad string");
            }

            return new idStr(str);
        }

        public void ReadFromFileHandle(idFile f) throws idException {
            int[] c = new int[1];
            idStr key, val;

            Clear();

//            f.Read(c, sizeof(c));
            f.ReadInt(c);
            c[0] = LittleLong(c[0]);
            for (int i = 0; i < c[0]; i++) {
                key = ReadString(f);
                val = ReadString(f);
                Set(key, val);
            }
        }

        // returns a unique checksum for this dictionary's content
        public long Checksum() {
            long[] ret = new long[1];
            int i, n;

            idList<idKeyValue> sorted = args;
            sorted.Sort(new KeyCompare());
            n = sorted.Num();
            CRC32_InitChecksum(ret);
            for (i = 0; i < n; i++) {
                CRC32.CRC32_UpdateChecksum(ret, sorted.oGet(i).GetKey().c_str(), sorted.oGet(i).GetKey().Length());
                CRC32.CRC32_UpdateChecksum(ret, sorted.oGet(i).GetValue().c_str(), sorted.oGet(i).GetValue().Length());
            }
            CRC32_FinishChecksum(ret);
            return ret[0];
        }

        public static void Init() {
            globalKeys.SetCaseSensitive(false);
            globalValues.SetCaseSensitive(true);
        }

        public static void Shutdown() {
            globalKeys.Clear();
            globalValues.Clear();
        }

        public static class ShowMemoryUsage_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ShowMemoryUsage_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idLib.common.Printf("%5d KB in %d keys\n", globalKeys.Size() >> 10, globalKeys.Num());
                idLib.common.Printf("%5d KB in %d values\n", globalValues.Size() >> 10, globalValues.Num());
            }
        }

        public static class ListKeys_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListKeys_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i;
                idList<idPoolStr> keyStrings = new idList<>();

                for (i = 0; i < globalKeys.Num(); i++) {
                    keyStrings.Append(globalKeys.oGet(i));
                }
                keyStrings.Sort();
                for (i = 0; i < keyStrings.Num(); i++) {
                    idLib.common.Printf("%s\n", keyStrings.oGet(i));
                }
                idLib.common.Printf("%5d keys\n", keyStrings.Num());
            }
        };

        public static class ListValues_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListValues_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i;
                idList<idPoolStr> valueStrings = new idList<>();

                for (i = 0; i < globalValues.Num(); i++) {
                    valueStrings.Append(globalValues.oGet(i));
                }
                valueStrings.Sort();
                for (i = 0; i < valueStrings.Num(); i++) {
                    idLib.common.Printf("%s\n", valueStrings.oGet(i));
                }
                idLib.common.Printf("%5d values\n", valueStrings.Num());
            }
        };
    }

    static class KeyCompare implements cmp_t<idKeyValue> {

        @Override
        public int compare(final idKeyValue a, final idKeyValue b) {
            return idStr.Cmp(a.GetKey(), b.GetKey());
        }
    }
}
