package neo.Tools.Compilers.RoqVQ;

import static java.lang.Math.abs;
import static neo.TempDump.atoi;
import static neo.TempDump.ctos;
import static neo.TempDump.strLen;
import static neo.framework.Common.common;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Str.va;

import java.util.Scanner;

import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;

/**
 *
 */
public class RoqParam {

    static class roqParam {

        public  idStr     outputFilename;
        public  int       numInputFiles;
        //
        private int[]     range;
        private boolean[] padding, padding2;
        private idStrList file;
        private idStrList file2;
        private idStr     soundfile;
        private idStr     currentPath;
        private idStr     tempFilename;
        private idStr     startPal;
        private idStr     endPal;
        private idStr     currentFile;
        private int[]     skipnum, skipnum2;
        private int[] startnum, startnum2;
        private int[] endnum, endnum2;
        private int[] numpadding, numpadding2;
        private int[] numfiles;
        private byte  keyR, keyG, keyB;
        private int field;
        private int realnum;
        private final int[] onFrame = {0};
        private int     firstframesize;
        private int     normalframesize;
        private int     jpegDefault;
        //
        private boolean scaleDown;
        private boolean twentyFourToThirty;
        private boolean encodeVideo;
        private boolean useTimecodeForRange;
        private boolean addPath;
        private boolean screenShots;
        private boolean startPalette;
        private boolean endPalette;
        private boolean fixedPalette;
        private boolean keyColor;
        private boolean justDelta;
        private boolean make3DO;
        private boolean makeVectors;
        private boolean justDeltaFlag;
        private boolean noAlphaAtAll;
        private boolean fullSearch;
        private boolean hasSound;
        private boolean isScaleable;
        //
        //

        public String RoqFilename() {
            return this.outputFilename.getData();
        }

        public String RoqTempFilename() {
            int i, j, len;

            j = 0;
            len = this.outputFilename.Length();
            for (i = 0; i < len; i++) {
                if (this.outputFilename.oGet(i) == '/') {
                    j = i;
                }
            }

            this.tempFilename = new idStr(String.format("/%s.temp", this.outputFilename.getData().substring(j + 1)));

            return this.tempFilename.getData();
        }

        public String GetNextImageFilename() {
            final idStr tempBuffer = new idStr();
            int i;
            int len;

            this.onFrame[0]++;
            GetNthInputFileName(tempBuffer, this.onFrame);
            if (this.justDeltaFlag == true) {
                this.onFrame[0]--;
                this.justDeltaFlag = false;
            }

            if (this.addPath == true) {
                this.currentFile.oSet(this.currentPath + "/" + tempBuffer);
            } else {
                this.currentFile = tempBuffer;
            }
            len = this.currentFile.Length();
            for (i = 0; i < len; i++) {
                if (this.currentFile.oGet(i) == '^') {
                    this.currentFile.oSet(i, ' ');
                }
            }

            return this.currentFile.getData();
        }

        public String SoundFilename() {
            return this.soundfile.getData();
        }

        public void InitFromFile(final String fileName) {
            idParser src;
            final idToken token = new idToken();
            int i, readarg;

            src = new idParser(fileName, LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
            if (!src.IsLoaded()) {
//		delete src;
                common.Printf("Error: can't open param file %s\n", fileName);
                return;
            }

            common.Printf("initFromFile: %s\n", fileName);

            this.fullSearch = false;
            this.scaleDown = false;
            this.encodeVideo = false;
            this.addPath = false;
            this.screenShots = false;
            this.startPalette = false;
            this.endPalette = false;
            this.fixedPalette = false;
            this.keyColor = false;
            this.justDelta = false;
            this.useTimecodeForRange = false;
            this.onFrame[0] = 0;
            this.numInputFiles = 0;
            this.currentPath = new idStr('\0');
            this.make3DO = false;
            this.makeVectors = false;
            this.justDeltaFlag = false;
            this.noAlphaAtAll = false;
            this.twentyFourToThirty = false;
            this.hasSound = false;
            this.isScaleable = false;
            this.firstframesize = 56 * 1024;
            this.normalframesize = 20000;
            this.jpegDefault = 85;

            this.realnum = 0;
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                readarg = 0;
// input dir
                if (token.Icmp("input_dir") == 0) {
                    src.ReadToken(token);
                    this.addPath = true;
                    this.currentPath = token;
//			common.Printf("  + input directory is %s\n", currentPath );
                    readarg++;
                    continue;
                }
// input dir
                if (token.Icmp("scale_down") == 0) {
                    this.scaleDown = true;
//			common.Printf("  + scaling down input\n" );
                    readarg++;
                    continue;
                }
// full search
                if (token.Icmp("fullsearch") == 0) {
                    this.normalframesize += this.normalframesize / 2;
                    this.fullSearch = true;
                    readarg++;
                    continue;
                }
// scaleable
                if (token.Icmp("scaleable") == 0) {
                    this.isScaleable = true;
                    readarg++;
                    continue;
                }
// input dir
                if (token.Icmp("no_alpha") == 0) {
                    this.noAlphaAtAll = true;
//			common.Printf("  + scaling down input\n" );
                    readarg++;
                    continue;
                }
                if (token.Icmp("24_fps_in_30_fps_out") == 0) {
                    this.twentyFourToThirty = true;
                    readarg++;
                    continue;
                }
// video in
                if (token.Icmp("video_in") == 0) {
                    this.encodeVideo = true;
//			common.Printf("  + Using the video port as input\n");
                    continue;
                }
//timecode range
                if (token.Icmp("timecode") == 0) {
                    this.useTimecodeForRange = true;
                    this.firstframesize = 12 * 1024;
                    this.normalframesize = 4500;
//			common.Printf("  + Using timecode as range\n");
                    continue;
                }
// soundfile for making a .RnR
                if (token.Icmp("sound") == 0) {
                    src.ReadToken(token);
                    this.soundfile = token;
                    this.hasSound = true;
//			common.Printf("  + Using timecode as range\n");
                    continue;
                }
// soundfile for making a .RnR
                if (token.Icmp("has_sound") == 0) {
                    this.hasSound = true;
                    continue;
                }
// outfile	
                if (token.Icmp("filename") == 0) {
                    src.ReadToken(token);
                    this.outputFilename = token;
                    i = this.outputFilename.Length();
//			common.Printf("  + output file is %s\n", outputFilename );
                    readarg++;
                    continue;
                }
// starting palette
                if (token.Icmp("start_palette") == 0) {
                    src.ReadToken(token);
                    this.startPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.getData()));
//			common.Error("  + starting palette is %s\n", startPal );
                    this.startPalette = true;
                    readarg++;
                    continue;
                }
// ending palette
                if (token.Icmp("end_palette") == 0) {
                    src.ReadToken(token);
                    this.endPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.getData()));
//			common.Printf("  + ending palette is %s\n", endPal );
                    this.endPalette = true;
                    readarg++;
                    continue;
                }
// fixed palette
                if (token.Icmp("fixed_palette") == 0) {
                    src.ReadToken(token);
                    this.startPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.getData()));
//			common.Printf("  + fixed palette is %s\n", startPal );
                    this.fixedPalette = true;
                    readarg++;
                    continue;
                }
// these are screen shots
                if (token.Icmp("screenshot") == 0) {
//			common.Printf("  + shooting screen shots\n" );
                    this.screenShots = true;
                    readarg++;
                    continue;
                }
//	key_color	r g b	
                if (token.Icmp("key_color") == 0) {
                    this.keyR = (byte) src.ParseInt();
                    this.keyG = (byte) src.ParseInt();
                    this.keyB = (byte) src.ParseInt();
                    this.keyColor = true;
//			common.Printf("  + key color is %03d %03d %03d\n", keyR, keyG, keyB );
                    readarg++;
                    continue;
                }
// only want deltas
                if (token.Icmp("just_delta") == 0) {
//			common.Printf("  + outputting deltas in the night\n" );
//			justDelta = true;
//			justDeltaFlag = true;
                    readarg++;
                    continue;
                }
// doing 3DO
                if (token.Icmp("3DO") == 0) {
                    this.make3DO = true;
                    readarg++;
                    continue;
                }
// makes codebook vector tables
                if (token.Icmp("codebook") == 0) {
                    this.makeVectors = true;
                    readarg++;
                    continue;
                }
// set first frame size
                if (token.Icmp("firstframesize") == 0) {
                    this.firstframesize = src.ParseInt();
                    readarg++;
                    continue;
                }
// set normal frame size
                if (token.Icmp("normalframesize") == 0) {
                    this.normalframesize = src.ParseInt();
                    readarg++;
                    continue;
                }
// set normal frame size
                if (token.Icmp("stillframequality") == 0) {
                    this.jpegDefault = src.ParseInt();
                    readarg++;
                    continue;
                }
                if (token.Icmp("input") == 0) {
                    final int num_files = 255;

                    this.range = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.padding = new boolean[num_files];// Mem_ClearedAlloc(num_files);
                    this.padding2 = new boolean[num_files];// Mem_ClearedAlloc(num_files);
                    this.skipnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.skipnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.startnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.startnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.endnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.endnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.numpadding = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.numpadding2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    this.numfiles = new int[num_files];// Mem_ClearedAlloc(num_files);
                    final idStr empty = new idStr();
                    this.file.AssureSize(num_files, empty);
//                    file.AssureSize(num_files, empty);//TODO:should this really be called twice?

                    this.field = 0;
                    this.realnum = 0;
                    do {
                        src.ReadToken(token);
                        if (token.Icmp("end_input") != 0) {
                            idStr arg1, arg2, arg3;

                            this.file.oSet(this.field, token);
                            while (src.ReadTokenOnLine(token) && (token.Icmp("[") != 0)) {
                                this.file.oGet(this.field).Append(token);
                            }

                            arg1 = token;
                            while (src.ReadTokenOnLine(token) && (token.Icmp("[") != 0)) {
                                arg1.Append(token);
                            }

                            arg2 = token;
                            while (src.ReadTokenOnLine(token) && (token.Icmp("[") != 0)) {
//						arg2 += token;
                                arg2.Append(token);
                            }

                            arg3 = token;
                            while (src.ReadTokenOnLine(token) && (token.Icmp("[") != 0)) {
//						arg3 += token;
                                arg3.Append(token);
                            }

                            if (arg1.oGet(0) != '[') {
//						common.Printf("  + reading %s\n", file[field] );
                                this.range[this.field] = 0;
                                this.numfiles[this.field] = 1;
                                this.realnum++;
                            } else {
                                if (arg1.oGet(0) == '[') {
                                    this.range[this.field] = 1;
                                    if (this.useTimecodeForRange) {
                                        this.realnum += parseTimecodeRange(arg1.getData(), this.field, this.skipnum, this.startnum, this.endnum, this.numfiles, this.padding, this.numpadding);
//								common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    } else {
                                        this.realnum += parseRange(arg1.getData(), this.field, this.skipnum, this.startnum, this.endnum, this.numfiles, this.padding, this.numpadding);
//								common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    }
                                } else if ((arg1.oGet(0) != '[') && (arg2.oGet(0) == '[') && (arg3.oGet(0) == '[')) {  //a double ranger...
                                    int files1, files2;

                                    this.file2.oSet(this.field, arg1);
                                    this.range[this.field] = 2;
                                    files1 = parseRange(arg2.getData(), this.field, this.skipnum, this.startnum, this.endnum, this.numfiles, this.padding, this.numpadding);
//							common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    files2 = parseRange(arg3.getData(), this.field, this.skipnum2, this.startnum2, this.endnum2, this.numfiles, this.padding2, this.numpadding2);
//							common.Printf("  + reading %s from %d to %d\n", file2[field], startnum2[field], endnum2[field]);
                                    if (files1 != files2) {
                                        common.Error("You had %d files for %s and %d for %s!", files1, arg1, files2, arg2);
                                    } else {
                                        this.realnum += files1;//not both, they are parallel
                                    }
                                } else {
                                    common.Error("Error: invalid range on open (%s %s %s)\n", arg1, arg2, arg3);
                                }
                            }
                            this.field++;
                        }
                    } while (token.Icmp("end_input") != 0);
                }
            }

            if (TwentyFourToThirty()) {
                this.realnum = this.realnum + (this.realnum >> 2);
            }
            this.numInputFiles = this.realnum;
            common.Printf("  + reading a total of %d frames in %s\n", this.numInputFiles, this.currentPath);
//	delete src;
        }

        public void GetNthInputFileName(idStr fileName, final int[] n) {
            int i, myfield, index, hrs, mins, secs, frs;
//	char tempfile[33], left[256], right[256], *strp;
            String tempfile, left, right;
            int strp;
            if (n[0] > this.realnum) {
                n[0] = this.realnum;
            }
// overcome starting at zero by ++ing and then --ing.
            if (TwentyFourToThirty()) {
                n[0]++;
                n[0] = ((n[0] / 5) * 4) + (n[0] % 5);
                n[0]--;
            }

            i = 0;
            myfield = 0;

            while (i <= n[0]) {
                i += this.numfiles[myfield++];
            }
            myfield--;
            i -= this.numfiles[myfield];

            if (this.range[myfield] == 1) {

                left = this.file.oGet(myfield).getData();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (this.startnum[myfield] <= this.endnum[myfield]) {
                    index = this.startnum[myfield] + ((n[0] - i) * this.skipnum[myfield]);
                } else {
                    index = this.startnum[myfield] - ((n[0] - i) * this.skipnum[myfield]);
                }

                if (this.padding[myfield] == true) {
                    if (this.useTimecodeForRange) {
                        hrs = index / (30 * 60 * 60);
                        mins = (index / (30 * 60)) % 60;
                        secs = (index / (30)) % 60;
                        frs = index % 30;
                        fileName.oSet(String.format("%s%.02d%.02d/%.02d%.02d%.02d%.02d%s", left, hrs, mins, hrs, mins, secs, frs, right));
                    } else {
                        tempfile = String.format("%032d", index);
                        fileName.oSet(String.format("%s%s%s", left, tempfile.substring(32 - this.numpadding[myfield]), right));
                    }
                } else {
                    if (this.useTimecodeForRange) {
                        hrs = index / (30 * 60 * 60);
                        mins = (index / (30 * 60)) % 60;
                        secs = (index / (30)) % 60;
                        frs = index % 30;
                        fileName.oSet(String.format("%s%.02d%.02d/%.02d%.02d%.02d%.02d%s", left, hrs, mins, hrs, mins, secs, frs, right));
                    } else {
                        fileName.oSet(String.format("%s%d%s", left, index, right));
                    }
                }
            } else if (this.range[myfield] == 2) {

                left = this.file.oGet(myfield).getData();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (this.startnum[myfield] <= this.endnum[myfield]) {
                    index = this.startnum[myfield] + ((n[0] - i) * this.skipnum[myfield]);
                } else {
                    index = this.startnum[myfield] - ((n[0] - i) * this.skipnum[myfield]);
                }

                if (this.padding[myfield] == true) {
                    tempfile = String.format("%032d", index);
                    fileName.oSet(String.format("%s%s%s", left, tempfile.substring(32 - this.numpadding[myfield]), right));
                } else {
                    fileName.oSet(String.format("%s%d%s", left, index, right));
                }

                left = this.file2.oGet(myfield).getData();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (this.startnum2[myfield] <= this.endnum2[myfield]) {
                    index = this.startnum2[myfield] + ((n[0] - i) * this.skipnum2[myfield]);
                } else {
                    index = this.startnum2[myfield] - ((n[0] - i) * this.skipnum2[myfield]);
                }

                if (this.padding2[myfield] == true) {
                    tempfile = String.format("%032d", index);
                    fileName.oPluSet(va("\n%s%s%s", left, tempfile.substring(32 - this.numpadding2[myfield]), right));
                } else {
                    fileName.oPluSet(va("\n%s%d%s", left, index, right));
                }
            } else {
                fileName.oSet(this.file.oGet(myfield).getData());
            }
        }

        public boolean MoreFrames() {
            if (this.onFrame[0] < this.numInputFiles) {
                return true;
            } else {
                return false;
            }
        }

        public boolean OutputVectors() {
            return this.makeVectors;
        }

        public boolean Timecode() {
            return this.useTimecodeForRange;
        }

        public boolean DeltaFrames() {
            return this.justDelta;
        }

        public boolean NoAlpha() {
            return this.noAlphaAtAll;
        }

        public boolean SearchType() {
            return this.fullSearch;
        }

        public boolean TwentyFourToThirty() {
            return this.twentyFourToThirty;
        }

        public boolean HasSound() {
            return this.hasSound;
        }

        public int NumberOfFrames() {
            return this.numInputFiles;
        }

        public int NormalFrameSize() {
            return this.normalframesize;
        }

        public int FirstFrameSize() {
            return this.firstframesize;
        }

        public int JpegQuality() {
            return this.jpegDefault;
        }

        public boolean IsScaleable() {
            return this.isScaleable;
        }
    }

    static int parseRange(final String rangeStr, int field, int[] skipnum, int[] startnum, int[] endnum, int[] numfiles, boolean[] padding, int[] numpadding) {
        final char[] start = new char[64], end = new char[64], skip = new char[64];
        int stptr, enptr, skptr;
        int i, realnum;

        i = 1;
        realnum = 0;
//	stptr = start;
//	enptr = end;
//	skptr = skip;
        stptr = enptr = skptr = 0;
        do {
            start[stptr++] = rangeStr.charAt(i++);
        } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
        start[stptr] = '\0';
        if (rangeStr.charAt(i++) != '-') {
            common.Error("Error: invalid range on middle \n");
        }
        do {
            end[enptr++] = rangeStr.charAt(i++);
        } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
        end[enptr] = '\0';
        if (rangeStr.charAt(i) != ']') {
            if (rangeStr.charAt(i++) != '+') {
                common.Error("Error: invalid range on close\n");
            }
            do {
                skip[skptr++] = rangeStr.charAt(i++);
            } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
            skip[skptr] = '\0';
            skipnum[field] = atoi(skip);
        } else {
            skipnum[field] = 1;
        }
        startnum[field] = atoi(start);
        endnum[field] = atoi(end);
        numfiles[field] = (abs(startnum[field] - endnum[field]) / skipnum[field]) + 1;
        realnum += numfiles[field];
        if ((start[0] == '0') && (start[1] != '\0')) {
            padding[field] = true;
            numpadding[field] = strLen(start);
        } else {
            padding[field] = false;
        }
        return realnum;
    }

    static int parseTimecodeRange(final String rangeStr, int field, int[] skipnum, int[] startnum, int[] endnum, int[] numfiles, boolean[] padding, int[] numpadding) {
        final char[] start = new char[64], end = new char[64], skip = new char[64];
        int stptr, enptr, skptr;
        int i, realnum;
        final int[] hrs = {0}, mins = {0}, secs = {0}, frs = {0};

        i = 1;//skip the '['
        realnum = 0;
//	stptr = start;
//	enptr = end;
//	skptr = skip;
        stptr = enptr = skptr = 0;
        do {
            start[stptr++] = rangeStr.charAt(i++);
        } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
        start[stptr] = '\0';
        if (rangeStr.charAt(i++) != '-') {
            common.Error("Error: invalid range on middle \n");
        }
        do {
            end[enptr++] = rangeStr.charAt(i++);
        } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
        end[enptr] = '\0';
        if (rangeStr.charAt(i) != ']') {
            if (rangeStr.charAt(i++) != '+') {
                common.Error("Error: invalid range on close\n");
            }
            do {
                skip[skptr++] = rangeStr.charAt(i++);
            } while ((rangeStr.charAt(i) >= '0') && (rangeStr.charAt(i) <= '9'));
            skip[skptr] = '\0';
            skipnum[field] = atoi(skip);
        } else {
            skipnum[field] = 1;
        }
        sscanf(start, "%2d%2d%2d%2d", hrs, mins, secs, frs);
        startnum[field] = (hrs[0] * 30 * 60 * 60) + (mins[0] * 60 * 30) + (secs[0] * 30) + frs[0];
        sscanf(end, "%2d%2d%2d%2d", hrs, mins, secs, frs);
        endnum[field] = (hrs[0] * 30 * 60 * 60) + (mins[0] * 60 * 30) + (secs[0] * 30) + frs[0];
        numfiles[field] = (abs(startnum[field] - endnum[field]) / skipnum[field]) + 1;
        realnum += numfiles[field];
        if ((start[0] == '0') && (start[1] != '\0')) {
            padding[field] = true;
            numpadding[field] = strLen(start);
        } else {
            padding[field] = false;
        }
        return realnum;
    }

    public static void sscanf(final char[] start, String bla, int[]  
        ... args) {

        try (Scanner scanner = new Scanner(ctos(start))) {
            for (final int[] a : args) {
                a[0] = scanner.nextInt();
            }
        }
    }

}
