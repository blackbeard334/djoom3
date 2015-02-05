package neo.Tools.Compilers.RoqVQ;

import static java.lang.Math.abs;
import java.util.Scanner;
import static neo.TempDump.atoi;
import static neo.TempDump.ctos;
import static neo.TempDump.strLen;
import static neo.framework.Common.common;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
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
        private int[] onFrame = {0};
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
            return outputFilename.toString();
        }

        public String RoqTempFilename() {
            int i, j, len;

            j = 0;
            len = outputFilename.Length();
            for (i = 0; i < len; i++) {
                if (outputFilename.oGet(i) == '/') {
                    j = i;
                }
            }

            tempFilename = new idStr(String.format("/%s.temp", outputFilename.toString().substring(j + 1)));

            return tempFilename.toString();
        }

        public String GetNextImageFilename() {
            idStr tempBuffer = new idStr();
            int i;
            int len;

            onFrame[0]++;
            GetNthInputFileName(tempBuffer, onFrame);
            if (justDeltaFlag == true) {
                onFrame[0]--;
                justDeltaFlag = false;
            }

            if (addPath == true) {
                currentFile.oSet(currentPath + "/" + tempBuffer);
            } else {
                currentFile = tempBuffer;
            }
            len = currentFile.Length();
            for (i = 0; i < len; i++) {
                if (currentFile.oGet(i) == '^') {
                    currentFile.oSet(i, ' ');
                }
            }

            return currentFile.toString();
        }

        public String SoundFilename() {
            return soundfile.toString();
        }

        public void InitFromFile(final String fileName) {
            idParser src;
            idToken token = new idToken();
            int i, readarg;

            src = new idParser(fileName, LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
            if (!src.IsLoaded()) {
//		delete src;
                common.Printf("Error: can't open param file %s\n", fileName);
                return;
            }

            common.Printf("initFromFile: %s\n", fileName);

            fullSearch = false;
            scaleDown = false;
            encodeVideo = false;
            addPath = false;
            screenShots = false;
            startPalette = false;
            endPalette = false;
            fixedPalette = false;
            keyColor = false;
            justDelta = false;
            useTimecodeForRange = false;
            onFrame[0] = 0;
            numInputFiles = 0;
            currentPath = new idStr('\0');
            make3DO = false;
            makeVectors = false;
            justDeltaFlag = false;
            noAlphaAtAll = false;
            twentyFourToThirty = false;
            hasSound = false;
            isScaleable = false;
            firstframesize = 56 * 1024;
            normalframesize = 20000;
            jpegDefault = 85;

            realnum = 0;
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                readarg = 0;
// input dir
                if (token.Icmp("input_dir") == 0) {
                    src.ReadToken(token);
                    addPath = true;
                    currentPath = token;
//			common.Printf("  + input directory is %s\n", currentPath );
                    readarg++;
                    continue;
                }
// input dir
                if (token.Icmp("scale_down") == 0) {
                    scaleDown = true;
//			common.Printf("  + scaling down input\n" );
                    readarg++;
                    continue;
                }
// full search
                if (token.Icmp("fullsearch") == 0) {
                    normalframesize += normalframesize / 2;
                    fullSearch = true;
                    readarg++;
                    continue;
                }
// scaleable
                if (token.Icmp("scaleable") == 0) {
                    isScaleable = true;
                    readarg++;
                    continue;
                }
// input dir
                if (token.Icmp("no_alpha") == 0) {
                    noAlphaAtAll = true;
//			common.Printf("  + scaling down input\n" );
                    readarg++;
                    continue;
                }
                if (token.Icmp("24_fps_in_30_fps_out") == 0) {
                    twentyFourToThirty = true;
                    readarg++;
                    continue;
                }
// video in
                if (token.Icmp("video_in") == 0) {
                    encodeVideo = true;
//			common.Printf("  + Using the video port as input\n");
                    continue;
                }
//timecode range
                if (token.Icmp("timecode") == 0) {
                    useTimecodeForRange = true;
                    firstframesize = 12 * 1024;
                    normalframesize = 4500;
//			common.Printf("  + Using timecode as range\n");
                    continue;
                }
// soundfile for making a .RnR
                if (token.Icmp("sound") == 0) {
                    src.ReadToken(token);
                    soundfile = token;
                    hasSound = true;
//			common.Printf("  + Using timecode as range\n");
                    continue;
                }
// soundfile for making a .RnR
                if (token.Icmp("has_sound") == 0) {
                    hasSound = true;
                    continue;
                }
// outfile	
                if (token.Icmp("filename") == 0) {
                    src.ReadToken(token);
                    outputFilename = token;
                    i = outputFilename.Length();
//			common.Printf("  + output file is %s\n", outputFilename );
                    readarg++;
                    continue;
                }
// starting palette
                if (token.Icmp("start_palette") == 0) {
                    src.ReadToken(token);
                    startPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.toString()));
//			common.Error("  + starting palette is %s\n", startPal );
                    startPalette = true;
                    readarg++;
                    continue;
                }
// ending palette
                if (token.Icmp("end_palette") == 0) {
                    src.ReadToken(token);
                    endPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.toString()));
//			common.Printf("  + ending palette is %s\n", endPal );
                    endPalette = true;
                    readarg++;
                    continue;
                }
// fixed palette
                if (token.Icmp("fixed_palette") == 0) {
                    src.ReadToken(token);
                    startPal.oSet(String.format("/LocalLibrary/vdxPalettes/%s", token.toString()));
//			common.Printf("  + fixed palette is %s\n", startPal );
                    fixedPalette = true;
                    readarg++;
                    continue;
                }
// these are screen shots
                if (token.Icmp("screenshot") == 0) {
//			common.Printf("  + shooting screen shots\n" );
                    screenShots = true;
                    readarg++;
                    continue;
                }
//	key_color	r g b	
                if (token.Icmp("key_color") == 0) {
                    keyR = (byte) src.ParseInt();
                    keyG = (byte) src.ParseInt();
                    keyB = (byte) src.ParseInt();
                    keyColor = true;
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
                    make3DO = true;
                    readarg++;
                    continue;
                }
// makes codebook vector tables
                if (token.Icmp("codebook") == 0) {
                    makeVectors = true;
                    readarg++;
                    continue;
                }
// set first frame size
                if (token.Icmp("firstframesize") == 0) {
                    firstframesize = src.ParseInt();
                    readarg++;
                    continue;
                }
// set normal frame size
                if (token.Icmp("normalframesize") == 0) {
                    normalframesize = src.ParseInt();
                    readarg++;
                    continue;
                }
// set normal frame size
                if (token.Icmp("stillframequality") == 0) {
                    jpegDefault = src.ParseInt();
                    readarg++;
                    continue;
                }
                if (token.Icmp("input") == 0) {
                    int num_files = 255;

                    range = new int[num_files];// Mem_ClearedAlloc(num_files);
                    padding = new boolean[num_files];// Mem_ClearedAlloc(num_files);
                    padding2 = new boolean[num_files];// Mem_ClearedAlloc(num_files);
                    skipnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    skipnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    startnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    startnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    endnum = new int[num_files];// Mem_ClearedAlloc(num_files);
                    endnum2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    numpadding = new int[num_files];// Mem_ClearedAlloc(num_files);
                    numpadding2 = new int[num_files];// Mem_ClearedAlloc(num_files);
                    numfiles = new int[num_files];// Mem_ClearedAlloc(num_files);
                    idStr empty = new idStr();
                    file.AssureSize(num_files, empty);
//                    file.AssureSize(num_files, empty);//TODO:should this really be called twice?

                    field = 0;
                    realnum = 0;
                    do {
                        src.ReadToken(token);
                        if (token.Icmp("end_input") != 0) {
                            idStr arg1, arg2, arg3;

                            file.oSet(field, token);
                            while (src.ReadTokenOnLine(token) && token.Icmp("[") != 0) {
                                file.oGet(field).Append(token);
                            }

                            arg1 = token;
                            while (src.ReadTokenOnLine(token) && token.Icmp("[") != 0) {
                                arg1.Append(token);
                            }

                            arg2 = token;
                            while (src.ReadTokenOnLine(token) && token.Icmp("[") != 0) {
//						arg2 += token;
                                arg2.Append(token);
                            }

                            arg3 = token;
                            while (src.ReadTokenOnLine(token) && token.Icmp("[") != 0) {
//						arg3 += token;
                                arg3.Append(token);
                            }

                            if (arg1.oGet(0) != '[') {
//						common.Printf("  + reading %s\n", file[field] );
                                range[field] = 0;
                                numfiles[field] = 1;
                                realnum++;
                            } else {
                                if (arg1.oGet(0) == '[') {
                                    range[field] = 1;
                                    if (useTimecodeForRange) {
                                        realnum += parseTimecodeRange(arg1.toString(), field, skipnum, startnum, endnum, numfiles, padding, numpadding);
//								common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    } else {
                                        realnum += parseRange(arg1.toString(), field, skipnum, startnum, endnum, numfiles, padding, numpadding);
//								common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    }
                                } else if ((arg1.oGet(0) != '[') && (arg2.oGet(0) == '[') && (arg3.oGet(0) == '[')) {  //a double ranger...
                                    int files1, files2;

                                    file2.oSet(field, arg1);
                                    range[field] = 2;
                                    files1 = parseRange(arg2.toString(), field, skipnum, startnum, endnum, numfiles, padding, numpadding);
//							common.Printf("  + reading %s from %d to %d\n", file[field], startnum[field], endnum[field]);
                                    files2 = parseRange(arg3.toString(), field, skipnum2, startnum2, endnum2, numfiles, padding2, numpadding2);
//							common.Printf("  + reading %s from %d to %d\n", file2[field], startnum2[field], endnum2[field]);
                                    if (files1 != files2) {
                                        common.Error("You had %d files for %s and %d for %s!", files1, arg1, files2, arg2);
                                    } else {
                                        realnum += files1;//not both, they are parallel
                                    }
                                } else {
                                    common.Error("Error: invalid range on open (%s %s %s)\n", arg1, arg2, arg3);
                                }
                            }
                            field++;
                        }
                    } while (token.Icmp("end_input") != 0);
                }
            }

            if (TwentyFourToThirty()) {
                realnum = realnum + (realnum >> 2);
            }
            numInputFiles = realnum;
            common.Printf("  + reading a total of %d frames in %s\n", numInputFiles, currentPath);
//	delete src;
        }

        public void GetNthInputFileName(idStr fileName, final int[] n) {
            int i, myfield, index, hrs, mins, secs, frs;
//	char tempfile[33], left[256], right[256], *strp;
            String tempfile, left, right;
            int strp;
            if (n[0] > realnum) {
                n[0] = realnum;
            }
// overcome starting at zero by ++ing and then --ing.
            if (TwentyFourToThirty()) {
                n[0]++;
                n[0] = (n[0] / 5) * 4 + (n[0] % 5);
                n[0]--;
            }

            i = 0;
            myfield = 0;

            while (i <= n[0]) {
                i += numfiles[myfield++];
            }
            myfield--;
            i -= numfiles[myfield];

            if (range[myfield] == 1) {

                left = file.oGet(myfield).toString();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (startnum[myfield] <= endnum[myfield]) {
                    index = startnum[myfield] + ((n[0] - i) * skipnum[myfield]);
                } else {
                    index = startnum[myfield] - ((n[0] - i) * skipnum[myfield]);
                }

                if (padding[myfield] == true) {
                    if (useTimecodeForRange) {
                        hrs = index / (30 * 60 * 60);
                        mins = (index / (30 * 60)) % 60;
                        secs = (index / (30)) % 60;
                        frs = index % 30;
                        fileName.oSet(String.format("%s%.02d%.02d/%.02d%.02d%.02d%.02d%s", left, hrs, mins, hrs, mins, secs, frs, right));
                    } else {
                        tempfile = String.format("%032d", index);
                        fileName.oSet(String.format("%s%s%s", left, tempfile.substring(32 - numpadding[myfield]), right));
                    }
                } else {
                    if (useTimecodeForRange) {
                        hrs = index / (30 * 60 * 60);
                        mins = (index / (30 * 60)) % 60;
                        secs = (index / (30)) % 60;
                        frs = index % 30;
                        fileName.oSet(String.format("%s%.02d%.02d/%.02d%.02d%.02d%.02d%s", left, hrs, mins, hrs, mins, secs, frs, right));
                    } else {
                        fileName.oSet(String.format("%s%d%s", left, index, right));
                    }
                }
            } else if (range[myfield] == 2) {

                left = file.oGet(myfield).toString();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (startnum[myfield] <= endnum[myfield]) {
                    index = startnum[myfield] + ((n[0] - i) * skipnum[myfield]);
                } else {
                    index = startnum[myfield] - ((n[0] - i) * skipnum[myfield]);
                }

                if (padding[myfield] == true) {
                    tempfile = String.format("%032d", index);
                    fileName.oSet(String.format("%s%s%s", left, tempfile.substring(32 - numpadding[myfield]), right));
                } else {
                    fileName.oSet(String.format("%s%d%s", left, index, right));
                }

                left = file2.oGet(myfield).toString();
                strp = left.indexOf("*");
                strp++;
                right = String.format("%s", left.substring(strp));

                if (startnum2[myfield] <= endnum2[myfield]) {
                    index = startnum2[myfield] + ((n[0] - i) * skipnum2[myfield]);
                } else {
                    index = startnum2[myfield] - ((n[0] - i) * skipnum2[myfield]);
                }

                if (padding2[myfield] == true) {
                    tempfile = String.format("%032d", index);
                    fileName.oPluSet(va("\n%s%s%s", left, tempfile.substring(32 - numpadding2[myfield]), right));
                } else {
                    fileName.oPluSet(va("\n%s%d%s", left, index, right));
                }
            } else {
                fileName.oSet(file.oGet(myfield).toString());
            }
        }

        public boolean MoreFrames() {
            if (onFrame[0] < numInputFiles) {
                return true;
            } else {
                return false;
            }
        }

        public boolean OutputVectors() {
            return makeVectors;
        }

        public boolean Timecode() {
            return useTimecodeForRange;
        }

        public boolean DeltaFrames() {
            return justDelta;
        }

        public boolean NoAlpha() {
            return noAlphaAtAll;
        }

        public boolean SearchType() {
            return fullSearch;
        }

        public boolean TwentyFourToThirty() {
            return twentyFourToThirty;
        }

        public boolean HasSound() {
            return hasSound;
        }

        public int NumberOfFrames() {
            return numInputFiles;
        }

        public int NormalFrameSize() {
            return normalframesize;
        }

        public int FirstFrameSize() {
            return firstframesize;
        }

        public int JpegQuality() {
            return jpegDefault;
        }

        public boolean IsScaleable() {
            return isScaleable;
        }
    };

    static int parseRange(final String rangeStr, int field, int[] skipnum, int[] startnum, int[] endnum, int[] numfiles, boolean[] padding, int[] numpadding) {
        char[] start = new char[64], end = new char[64], skip = new char[64];
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
        } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
        start[stptr] = '\0';
        if (rangeStr.charAt(i++) != '-') {
            common.Error("Error: invalid range on middle \n");
        }
        do {
            end[enptr++] = rangeStr.charAt(i++);
        } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
        end[enptr] = '\0';
        if (rangeStr.charAt(i) != ']') {
            if (rangeStr.charAt(i++) != '+') {
                common.Error("Error: invalid range on close\n");
            }
            do {
                skip[skptr++] = rangeStr.charAt(i++);
            } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
            skip[skptr] = '\0';
            skipnum[field] = atoi(skip);
        } else {
            skipnum[field] = 1;
        }
        startnum[field] = atoi(start);
        endnum[field] = atoi(end);
        numfiles[field] = (abs(startnum[field] - endnum[field]) / skipnum[field]) + 1;
        realnum += numfiles[field];
        if (start[0] == '0' && start[1] != '\0') {
            padding[field] = true;
            numpadding[field] = strLen(start);
        } else {
            padding[field] = false;
        }
        return realnum;
    }

    static int parseTimecodeRange(final String rangeStr, int field, int[] skipnum, int[] startnum, int[] endnum, int[] numfiles, boolean[] padding, int[] numpadding) {
        char[] start = new char[64], end = new char[64], skip = new char[64];
        int stptr, enptr, skptr;
        int i, realnum;
        int[] hrs = {0}, mins = {0}, secs = {0}, frs = {0};

        i = 1;//skip the '['
        realnum = 0;
//	stptr = start;
//	enptr = end;
//	skptr = skip;
        stptr = enptr = skptr = 0;
        do {
            start[stptr++] = rangeStr.charAt(i++);
        } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
        start[stptr] = '\0';
        if (rangeStr.charAt(i++) != '-') {
            common.Error("Error: invalid range on middle \n");
        }
        do {
            end[enptr++] = rangeStr.charAt(i++);
        } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
        end[enptr] = '\0';
        if (rangeStr.charAt(i) != ']') {
            if (rangeStr.charAt(i++) != '+') {
                common.Error("Error: invalid range on close\n");
            }
            do {
                skip[skptr++] = rangeStr.charAt(i++);
            } while (rangeStr.charAt(i) >= '0' && rangeStr.charAt(i) <= '9');
            skip[skptr] = '\0';
            skipnum[field] = atoi(skip);
        } else {
            skipnum[field] = 1;
        }
        sscanf(start, "%2d%2d%2d%2d", hrs, mins, secs, frs);
        startnum[field] = hrs[0] * 30 * 60 * 60 + mins[0] * 60 * 30 + secs[0] * 30 + frs[0];
        sscanf(end, "%2d%2d%2d%2d", hrs, mins, secs, frs);
        endnum[field] = hrs[0] * 30 * 60 * 60 + mins[0] * 60 * 30 + secs[0] * 30 + frs[0];
        numfiles[field] = (abs(startnum[field] - endnum[field]) / skipnum[field]) + 1;
        realnum += numfiles[field];
        if (start[0] == '0' && start[1] != '\0') {
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
            for (int[] a : args) {
                a[0] = scanner.nextInt();
            }
        }
    }

}
