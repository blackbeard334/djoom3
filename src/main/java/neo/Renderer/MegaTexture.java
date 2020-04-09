package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Image.textureDepth_t.TD_HIGH_QUALITY;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import static neo.Renderer.MegaTexture.megaTextureHeader_t.ReadDdsFileHeader_t;
import static neo.Renderer.MegaTexture.megaTextureHeader_t.WriteDdsFileHeader_t;
import static neo.Renderer.tr_backend.GL_SelectTexture;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_RENDERER;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_CUR;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import static neo.framework.Session.session;
import static neo.open.gl.QGL.qglProgramLocalParameter4fvARB;
import static neo.open.gl.QGL.qglTexSubImage2D;
import static neo.open.gl.QGLConstantsIfc.GL_RGBA;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_2D;
import static neo.open.gl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.open.gl.QGLConstantsIfc.GL_VERTEX_PROGRAM_ARB;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import neo.TempDump.CPP_class.Pointer;
import neo.Renderer.Image.GeneratorFunction;
import neo.Renderer.Image.idImage;
import neo.Renderer.Model.srfTriangles_s;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.File_h.idFile;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Vector.idVec3;
import neo.open.Nio;

/**
 *
 */
public class MegaTexture {

    static class idTextureTile {

        public static final transient int SIZE
                = Integer.SIZE
                + Integer.SIZE;

        public int x;
        public int y;
    }
    static final int TILE_PER_LEVEL = 4;
    static final int MAX_MEGA_CHANNELS = 3;		// normal, diffuse, specular
    static final int MAX_LEVELS = 12;
    static final int MAX_LEVEL_WIDTH = 512;
    static final int TILE_SIZE = MAX_LEVEL_WIDTH / TILE_PER_LEVEL;

    static class idTextureLevel {

        public static final transient int SIZE
                = Pointer.SIZE//idMegaTexture * mega
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Pointer.SIZE//idImage * image
                + (idTextureTile.SIZE * TILE_PER_LEVEL * TILE_PER_LEVEL);

        public idMegaTexture mega;
//
        public int tileOffset;
        public int tilesWide;
        public int tilesHigh;
//
        public idImage image;
        public idTextureTile[][] tileMap = new idTextureTile[TILE_PER_LEVEL][TILE_PER_LEVEL];
//
        private final FloatBuffer parms = Nio.newFloatBuffer(4);
//

        /*
         ====================
         UpdateForCenter

         Center is in the 0.0 to 1.0 range
         ====================
         */
        public void UpdateForCenter(float[] center/*[2]*/) {
            final int[] globalTileCorner = new int[2];
            final int[] localTileOffset = new int[2];

            if ((this.tilesWide <= TILE_PER_LEVEL) && (this.tilesHigh <= TILE_PER_LEVEL)) {
                globalTileCorner[0] = 0;
                globalTileCorner[1] = 0;
                localTileOffset[0] = 0;
                localTileOffset[1] = 0;
                // orient the mask so that it doesn't mask anything at all
                this.parms.put(0, 0.25f);
                this.parms.put(1, 0.25f);
                this.parms.put(3, 0.25f);
            } else {
                for (int i = 0; i < 2; i++) {
                    final float[] global = new float[2];

                    // this value will be outside the 0.0 to 1.0 range unless
                    // we are in the corner of the megaTexture
                    global[i] = ((center[i] * this.parms.get(3)) - 0.5f) * TILE_PER_LEVEL;

                    globalTileCorner[i] = (int) (global[i] + 0.5);

                    localTileOffset[i] = globalTileCorner[i] & (TILE_PER_LEVEL - 1);

                    // scaling for the mask texture to only allow the proper window
                    // of tiles to show through
                    this.parms.put(i, -globalTileCorner[i] / (float) TILE_PER_LEVEL);
                }
            }

            this.image.Bind();

            for (int x = 0; x < TILE_PER_LEVEL; x++) {
                for (int y = 0; y < TILE_PER_LEVEL; y++) {
                    final int[] globalTile = new int[2];

                    globalTile[0] = globalTileCorner[0] + ((x - localTileOffset[0]) & (TILE_PER_LEVEL - 1));
                    globalTile[1] = globalTileCorner[1] + ((y - localTileOffset[1]) & (TILE_PER_LEVEL - 1));

                    UpdateTile(x, y, globalTile[0], globalTile[1]);
                }
            }
        }

        /*
         ====================
         UpdateTile

         A local tile will only be mapped to globalTile[ localTile + X * TILE_PER_LEVEL ] for some x
         ====================
         */
        public void UpdateTile(int localX, int localY, int globalX, int globalY) {
            final idTextureTile tile = this.tileMap[localX][localY];

            if ((tile.x == globalX) && (tile.y == globalY)) {
                return;
            }
            if (((globalX & (TILE_PER_LEVEL - 1)) != localX) || ((globalY & (TILE_PER_LEVEL - 1)) != localY)) {
                common.Error("idTextureLevel::UpdateTile: bad coordinate mod");
            }

            tile.x = globalX;
            tile.y = globalY;

            final ByteBuffer data = ByteBuffer.allocate(TILE_SIZE * TILE_SIZE * 4);

            if ((globalX >= this.tilesWide) || (globalX < 0) || (globalY >= this.tilesHigh) || (globalY < 0)) {
                // off the map
//		memset( data, 0, sizeof( data ) );
            } else {
                // extract the data from the full image (FIXME: background load from disk)
                final int tileNum = this.tileOffset + (tile.y * this.tilesWide) + tile.x;

                final int tileSize = TILE_SIZE * TILE_SIZE * 4;

                this.mega.fileHandle.Seek(tileNum * tileSize, FS_SEEK_SET);
//		memset( data, 128, sizeof( data ) );
                Arrays.fill(data.array(), (byte) 128);
                this.mega.fileHandle.Read(data, tileSize);
            }

            if (idMegaTexture.r_showMegaTextureLabels.GetBool()) {
                // put a color marker in it
                final byte[] color/*[4]*/ = {(byte) ((255 * localX) / TILE_PER_LEVEL), (byte) ((255 * localY) / TILE_PER_LEVEL), 0, 0};
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
//				*(int *)&data[ ( ( y + TILE_SIZE/2 - 4 ) * TILE_SIZE + x + TILE_SIZE/2 - 4 ) * 4 ] = *(int *)color;
                        System.arraycopy(color, 0, data.array(), (((((y + (TILE_SIZE / 2)) - 4) * TILE_SIZE) + x + (TILE_SIZE / 2)) - 4) * 4, 4);
                    }
                }
            }

            // upload all the mip-map levels
            int level = 0;
            int size = TILE_SIZE;
            while (true) {
                qglTexSubImage2D(GL_TEXTURE_2D, level, localX * size, localY * size, size, size, GL_RGBA, GL_UNSIGNED_BYTE, data);
                size >>= 1;
                level++;

                if (size == 0) {
                    break;
                }

                final int byteSize = size * 4;
                // mip-map in place
                for (int y = 0; y < size; y++) {
                    final byte[] in = new byte[data.capacity() - (y * size * 16)];
                    final byte[] in2 = new byte[in.length - (size * 8)];
                    final byte[] out = new byte[data.capacity() - (y * size * 4)];
//			in = data + y * size * 16;
//			in2 = in + size * 8;
//			out = data + y * size * 4;
                    System.arraycopy(data.array(), y * size * 16, in, 0, in.length);
                    System.arraycopy(in, size * 8, in2, 0, in2.length);
                    System.arraycopy(data.array(), y * size * 4, out, 0, out.length);
                    for (int x = 0; x < size; x++) {
                        out[(x * 4) + 0] = (byte) ((in[(x * 8) + 0] + in[(x * 8) + 4 + 0] + in2[(x * 8) + 0] + in2[(x * 8) + 4 + 0]) >> 2);
                        out[(x * 4) + 1] = (byte) ((in[(x * 8) + 1] + in[(x * 8) + 4 + 1] + in2[(x * 8) + 1] + in2[(x * 8) + 4 + 1]) >> 2);
                        out[(x * 4) + 2] = (byte) ((in[(x * 8) + 2] + in[(x * 8) + 4 + 2] + in2[(x * 8) + 2] + in2[(x * 8) + 4 + 2]) >> 2);
                        out[(x * 4) + 3] = (byte) ((in[(x * 8) + 3] + in[(x * 8) + 4 + 3] + in2[(x * 8) + 3] + in2[(x * 8) + 4 + 3]) >> 2);
                    }
                }
            }
        }

        /*
         =====================
         Invalidate

         Forces all tiles to be regenerated
         =====================
         */
        public void Invalidate() {
            for (int x = 0; x < TILE_PER_LEVEL; x++) {
                for (int y = 0; y < TILE_PER_LEVEL; y++) {
                    this.tileMap[x][y].x
                            = this.tileMap[x][y].y = -99999;
                }
            }
        }
    }

    static class megaTextureHeader_t implements Serializable {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final transient int BYTES = Integer.BYTES * 3;

        int tileSize;
        int tilesWide;
        int tilesHigh;

        public static ByteBuffer ReadDdsFileHeader_t() {
            return ByteBuffer.allocate(BYTES);
        }

        public static megaTextureHeader_t ReadDdsFileHeader_t(ByteBuffer buffer) {
            final megaTextureHeader_t t = new megaTextureHeader_t();
            t.tileSize = buffer.getInt();
            t.tilesWide = buffer.getInt();
            t.tilesHigh = buffer.getInt();
            return t;
        }

        public static ByteBuffer WriteDdsFileHeader_t(final megaTextureHeader_t ev) {
            final ByteBuffer buffer = ReadDdsFileHeader_t();
            buffer.putInt(ev.tileSize).putInt(ev.tilesWide).putInt(ev.tilesHigh).flip();

            return buffer;
        }
    }

    public static class idMegaTexture {

        public static final transient int SIZE
                = Pointer.SIZE//idFile fileHandle
                + Pointer.SIZE//srfTriangles_s currentTriMapping
                + idVec3.SIZE
                + (Float.SIZE * 2 * 4)
                + Integer.SIZE
                + (idTextureLevel.SIZE * MAX_LEVELS)
                + megaTextureHeader_t.BYTES;

        private idFile fileHandle;
        //
        private srfTriangles_s currentTriMapping;
        //
        private idVec3 currentViewOrigin;
        //
        private final float[][] localViewToTextureCenter = new float[2][4];
        //
        private int numLevels;
        private final idTextureLevel[] levels = new idTextureLevel[MAX_LEVELS];				// 0 is the highest resolution
        private megaTextureHeader_t header;
        //
        private static final idCVar r_megaTextureLevel = new idCVar("r_megaTextureLevel", "0", CVAR_RENDERER | CVAR_INTEGER, "draw only a specific level");
        private static final idCVar r_showMegaTexture = new idCVar("r_showMegaTexture", "0", CVAR_RENDERER | CVAR_BOOL, "display all the level images");
        private static final idCVar r_showMegaTextureLabels = new idCVar("r_showMegaTextureLabels", "0", CVAR_RENDERER | CVAR_BOOL, "draw colored blocks in each tile");
        private static final idCVar r_skipMegaTexture = new idCVar("r_skipMegaTexture", "0", CVAR_RENDERER | CVAR_INTEGER, "only use the lowest level image");
        private static final idCVar r_terrainScale = new idCVar("r_terrainScale", "3", CVAR_RENDERER | CVAR_INTEGER, "vertically scale USGS data");
        //
        //

        public boolean InitFromMegaFile(final String fileBase) {
            final idStr name = new idStr("megaTextures/" + fileBase);
            name.StripFileExtension();
            name.Append(".mega");

            int width, height;

            this.fileHandle = fileSystem.OpenFileRead(name.getData());
            if (null == this.fileHandle) {
                common.Printf("idMegaTexture: failed to open %s\n", name);
                return false;
            }

            final ByteBuffer headerBuffer = ReadDdsFileHeader_t();
            this.fileHandle.Read(headerBuffer);
            this.header = ReadDdsFileHeader_t(headerBuffer);

            if ((this.header.tileSize < 64) || (this.header.tilesWide < 1) || (this.header.tilesHigh < 1)) {
                common.Printf("idMegaTexture: bad header on %s\n", name);
                return false;
            }

            this.currentTriMapping = null;

            this.numLevels = 0;
            width = this.header.tilesWide;
            height = this.header.tilesHigh;

            int tileOffset = 1;					// just past the header

//	memset( levels, 0, sizeof( levels ) );
            Arrays.fill(this.levels, 0);
            while (true) {
                final idTextureLevel level = this.levels[this.numLevels];

                level.mega = this;
                level.tileOffset = tileOffset;
                level.tilesWide = width;
                level.tilesHigh = height;
                level.parms.put(0, -1);		// initially mask everything
                level.parms.put(1, 0);
                level.parms.put(2, 0);
                level.parms.put(3, (float) width / TILE_PER_LEVEL);
                level.Invalidate();

                tileOffset += level.tilesWide * level.tilesHigh;

                final String str = String.format("MEGA_%s_%d", fileBase, this.numLevels);

                // give each level a default fill color
                for (int i = 0; i < 4; i++) {
                    fillColor.setColor(i, colors[this.numLevels + 1][i]);
                }

                this.levels[this.numLevels].image = globalImages.ImageFromFunction(str, R_EmptyLevelImage.getInstance());
                this.numLevels++;

                if ((width <= TILE_PER_LEVEL) && (height <= TILE_PER_LEVEL)) {
                    break;
                }
                width = (width + 1) >> 1;
                height = (height + 1) >> 1;
            }

            // force first bind to load everything
            this.currentViewOrigin.oSet(0, -99999999.0f);
            this.currentViewOrigin.oSet(1, -99999999.0f);
            this.currentViewOrigin.oSet(2, -99999999.0f);

            return true;
        }

        /*
         ====================
         SetMappingForSurface

         analyzes xyz and st to create a mapping
         This is not very robust, but works for rectangular grids
         ====================
         */
        public void SetMappingForSurface(final srfTriangles_s tri) {	// analyzes xyz and st to create a mapping
            if (tri.equals(this.currentTriMapping)) {
                return;
            }
            this.currentTriMapping = tri;

            if (null == tri.verts) {
                return;
            }

            idDrawVert origin = new idDrawVert();
            final idDrawVert[] axis = new idDrawVert[2];

            origin.st.oSet(0, 1.0f);
            origin.st.oSet(1, 1.0f);

            axis[0].st.oSet(0, 0f);
            axis[0].st.oSet(1, 1f);

            axis[1].st.oSet(0, 1f);
            axis[1].st.oSet(1, 0f);

            for (int i = 0; i < tri.numVerts; i++) {
                final idDrawVert v = tri.verts[i];

                if ((v.st.oGet(0) <= origin.st.oGet(0)) && (v.st.oGet(1) <= origin.st.oGet(1))) {
                    origin = v;
                }
                if ((v.st.oGet(0) >= axis[0].st.oGet(0)) && (v.st.oGet(1) <= axis[0].st.oGet(1))) {
                    axis[0] = v;
                }
                if ((v.st.oGet(0) <= axis[1].st.oGet(0)) && (v.st.oGet(1) >= axis[1].st.oGet(1))) {
                    axis[1] = v;
                }
            }

            for (int i = 0; i < 2; i++) {
                final idVec3 dir = axis[i].xyz.oMinus(origin.xyz);
                final float texLen = axis[i].st.oGet(i) - origin.st.oGet(i);
                final float spaceLen = (axis[i].xyz.oMinus(origin.xyz)).Length();

                final float scale = texLen / (spaceLen * spaceLen);
                dir.oMulSet(scale);

                final float c = origin.xyz.oMultiply(dir) - origin.st.oGet(i);

                this.localViewToTextureCenter[i][0] = dir.oGet(0);
                this.localViewToTextureCenter[i][1] = dir.oGet(1);
                this.localViewToTextureCenter[i][2] = dir.oGet(2);
                this.localViewToTextureCenter[i][3] = -c;
            }
        }
        private static final FloatBuffer parms/*[4]*/ = Nio.newFloatBuffer(4); // no contribution

        static {
            parms.put(new float[]{-2, -2, 0, 1}).rewind();
        }

        public void BindForViewOrigin(final idVec3 viewOrigin) {	// binds images and sets program parameters

            SetViewOrigin(viewOrigin);

            // borderClamp image goes in texture 0
            GL_SelectTexture(0);
            globalImages.borderClampImage.Bind();

            // level images in higher textures, blurriest first
            for (int i = 0; i < 7; i++) {
                GL_SelectTexture(1 + i);

                if (i >= this.numLevels) {
                    globalImages.whiteImage.Bind();

                    qglProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, i, parms);
                } else {
                    final idTextureLevel level = this.levels[ this.numLevels - 1 - i];

                    if (r_showMegaTexture.GetBool()) {
                        if ((i & 1) == 1) {
                            globalImages.blackImage.Bind();
                        } else {
                            globalImages.whiteImage.Bind();
                        }
                    } else {
                        level.image.Bind();
                    }
                    qglProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, i, level.parms);
                }
            }

            final FloatBuffer parms = Nio.newFloatBuffer(4);
            parms.put(0, 0);
            parms.put(1, 0);
            parms.put(2, 0);
            parms.put(3, 1);
            qglProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, 7, parms);

            parms.put(0, 1);
            parms.put(1, 1);
            parms.put(2, r_terrainScale.GetFloat());
            parms.put(3, 1);
            qglProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, 8, parms);
        }

        /*
         ====================
         Unbind

         This can go away once everything uses fragment programs so the enable states don't
         need tracking
         ====================
         */
        public void Unbind() {								// removes texture bindings
            for (int i = 0; i < this.numLevels; i++) {
                GL_SelectTexture(1 + i);
                globalImages.BindNull();
            }
        }

        /*
         ====================
         MakeMegaTexture_f

         Incrementally load a giant tga file and process into the mega texture block format
         ====================
         */
        public static class MakeMegaTexture_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new MakeMegaTexture_f();

            private MakeMegaTexture_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                int columns, rows, fileSize, numBytes;
                int pixbuf;
                int row, column;
                final _TargaHeader targa_header = new _TargaHeader();

                if (args.Argc() != 2) {
                    common.Printf("USAGE: makeMegaTexture <filebase>\n");
                    return;
                }

                final idStr name_s = new idStr("megaTextures/" + args.Argv(1));
                name_s.StripFileExtension();
                name_s.Append(".tga");

                final String name = name_s.getData();

                //
                // open the file
                //
                common.Printf("Opening %s.\n", name);
                fileSize = fileSystem.ReadFile(name, null, null);
                final idFile file = fileSystem.OpenFileRead(name);

                if (null == file) {
                    common.Printf("Couldn't open %s\n", name);
                    return;
                }

                targa_header.id_length = (char) ReadByte(file);
                targa_header.colormap_type = (char) ReadByte(file);
                targa_header.image_type = (char) ReadByte(file);

                targa_header.colormap_index = ReadShort(file);
                targa_header.colormap_length = ReadShort(file);
                targa_header.colormap_size = (char) ReadByte(file);
                targa_header.x_origin = ReadShort(file);
                targa_header.y_origin = ReadShort(file);
                targa_header.width = ReadShort(file);
                targa_header.height = ReadShort(file);
                targa_header.pixel_size = (char) ReadByte(file);
                targa_header.attributes = (char) ReadByte(file);

                if ((targa_header.image_type != 2) && (targa_header.image_type != 10) && (targa_header.image_type != 3)) {
                    common.Error("LoadTGA( %s ): Only type 2 (RGB), 3 (gray), and 10 (RGB) TGA images supported\n", name);
                }

                if (targa_header.colormap_type != 0) {
                    common.Error("LoadTGA( %s ): colormaps not supported\n", name);
                }

                if (((targa_header.pixel_size != 32) && (targa_header.pixel_size != 24)) && (targa_header.image_type != 3)) {
                    common.Error("LoadTGA( %s ): Only 32 or 24 bit images supported (no colormaps)\n", name);
                }

                if ((targa_header.image_type == 2) || (targa_header.image_type == 3)) {
                    numBytes = targa_header.width * targa_header.height * (targa_header.pixel_size >> 3);
                    if (numBytes > (fileSize - 18 - targa_header.id_length)) {
                        common.Error("LoadTGA( %s ): incomplete file\n", name);
                    }
                }

                columns = targa_header.width;
                rows = targa_header.height;

                // skip TARGA image comment
                if (targa_header.id_length != 0) {
                    file.Seek(targa_header.id_length, FS_SEEK_CUR);
                }

                final megaTextureHeader_t mtHeader = new megaTextureHeader_t();

                mtHeader.tileSize = TILE_SIZE;
                mtHeader.tilesWide = RoundDownToPowerOfTwo(targa_header.width) / TILE_SIZE;
                mtHeader.tilesHigh = RoundDownToPowerOfTwo(targa_header.height) / TILE_SIZE;

                final idStr outName = new idStr(name);
                outName.StripFileExtension();
                outName.Append(".mega");

                common.Printf("Writing %d x %d size %d tiles to %s.\n",
                        mtHeader.tilesWide, mtHeader.tilesHigh, mtHeader.tileSize, outName);

                // open the output megatexture file
                final idFile out = fileSystem.OpenFileWrite(outName.getData());

                out.Write(WriteDdsFileHeader_t(mtHeader));
                out.Seek(TILE_SIZE * TILE_SIZE * 4, FS_SEEK_SET);

                // we will process this one row of tiles at a time, since the entire thing
                // won't fit in memory
                final byte[] targa_rgba = new byte[TILE_SIZE * targa_header.width * 4];// R_StaticAlloc(TILE_SIZE * targa_header.width * 4);

                int blockRowsRemaining = mtHeader.tilesHigh;
                while (blockRowsRemaining-- != 0) {
                    common.Printf("%d blockRowsRemaining\n", blockRowsRemaining);
                    session.UpdateScreen();

                    if ((targa_header.image_type == 2) || (targa_header.image_type == 3)) {
                        // Uncompressed RGB or gray scale image
                        for (row = 0; row < TILE_SIZE; row++) {
                            pixbuf = row * columns * 4;
                            for (column = 0; column < columns; column++) {
                                byte red, green, blue, alphabyte;
                                switch (targa_header.pixel_size) {
                                    case 8:
                                        blue = ReadByte(file);
                                        green = blue;
                                        red = blue;
                                        targa_rgba[pixbuf++] = red;
                                        targa_rgba[pixbuf++] = green;
                                        targa_rgba[pixbuf++] = blue;
                                        targa_rgba[pixbuf++] = (byte) 255;
                                        break;

                                    case 24:
                                        blue = ReadByte(file);
                                        green = ReadByte(file);
                                        red = ReadByte(file);
                                        targa_rgba[pixbuf++] = red;
                                        targa_rgba[pixbuf++] = green;
                                        targa_rgba[pixbuf++] = blue;
                                        targa_rgba[pixbuf++] = (byte) 255;
                                        break;
                                    case 32:
                                        blue = ReadByte(file);
                                        green = ReadByte(file);
                                        red = ReadByte(file);
                                        alphabyte = ReadByte(file);
                                        targa_rgba[pixbuf++] = red;
                                        targa_rgba[pixbuf++] = green;
                                        targa_rgba[pixbuf++] = blue;
                                        targa_rgba[pixbuf++] = alphabyte;
                                        break;
                                    default:
                                        common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                                        break;
                                }
                            }
                        }
                    } else if (targa_header.image_type == 10) {   // Runlength encoded RGB images
                        byte red, green, blue, alphabyte, packetHeader, packetSize, j;

                        red = 0;
                        green = 0;
                        blue = 0;
                        alphabyte = (byte) 0xff;

                        for (row = 0; row < TILE_SIZE; row++) {
                            pixbuf = row * columns * 4;
                            breakOut:
                            for (column = 0; column < columns;) {
                                packetHeader = ReadByte(file);
                                packetSize = (byte) (1 + (packetHeader & 0x7f));
                                if ((packetHeader & 0x80) == 0x80) {        // run-length packet
                                    switch (targa_header.pixel_size) {
                                        case 24:
                                            blue = ReadByte(file);
                                            green = ReadByte(file);
                                            red = ReadByte(file);
                                            alphabyte = (byte) 255;
                                            break;
                                        case 32:
                                            blue = ReadByte(file);
                                            green = ReadByte(file);
                                            red = ReadByte(file);
                                            alphabyte = ReadByte(file);
                                            break;
                                        default:
                                            common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                                            break;
                                    }

                                    for (j = 0; j < packetSize; j++) {
                                        targa_rgba[pixbuf++] = red;
                                        targa_rgba[pixbuf++] = green;
                                        targa_rgba[pixbuf++] = blue;
                                        targa_rgba[pixbuf++] = alphabyte;
                                        column++;
                                        if (column == columns) { // run spans across rows
                                            common.Error("TGA had RLE across columns, probably breaks block");
                                            column = 0;
                                            if (row > 0) {
                                                row--;
                                            } else {
                                                break breakOut;
                                            }
                                            pixbuf = row * columns * 4;
                                        }
                                    }
                                } else {                            // non run-length packet
                                    for (j = 0; j < packetSize; j++) {
                                        switch (targa_header.pixel_size) {
                                            case 24:
                                                blue = ReadByte(file);
                                                green = ReadByte(file);
                                                red = ReadByte(file);
                                                targa_rgba[pixbuf++] = red;
                                                targa_rgba[pixbuf++] = green;
                                                targa_rgba[pixbuf++] = blue;
                                                targa_rgba[pixbuf++] = (byte) 255;
                                                break;
                                            case 32:
                                                blue = ReadByte(file);
                                                green = ReadByte(file);
                                                red = ReadByte(file);
                                                alphabyte = ReadByte(file);
                                                targa_rgba[pixbuf++] = red;
                                                targa_rgba[pixbuf++] = green;
                                                targa_rgba[pixbuf++] = blue;
                                                targa_rgba[pixbuf++] = alphabyte;
                                                break;
                                            default:
                                                common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                                                break;
                                        }
                                        column++;
                                        if (column == columns) { // pixel packet run spans across rows
                                            column = 0;
                                            if (row > 0) {
                                                row--;
                                            } else {
                                                break breakOut;
                                            }
                                            pixbuf = row * columns * 4;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //
                    // write out individual blocks from the full row block buffer
                    //
                    for (int rowBlock = 0; rowBlock < mtHeader.tilesWide; rowBlock++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            out.Write(ByteBuffer.wrap(Arrays.copyOfRange(targa_rgba, ((y * targa_header.width) + (rowBlock * TILE_SIZE)) * 4, targa_rgba.length)), TILE_SIZE * 4);
                        }
                    }
                }
//
//                R_StaticFree(targa_rgba);

                GenerateMegaMipMaps(mtHeader, out);

//	delete out;
//	delete file;
                GenerateMegaPreview(outName.getData());
//if (false){
//	if ( (targa_header.attributes & (1<<5)) ) {			// image flp bit
//		R_VerticalFlip( *pic, *width, *height );
//	}
//}
            }
        }
//// private:
//// friend class idTextureLevel;

        private void SetViewOrigin(final idVec3 viewOrigin) {
            if (r_showMegaTextureLabels.IsModified()) {
                r_showMegaTextureLabels.ClearModified();
                this.currentViewOrigin.oSet(0, viewOrigin.oGet(0) + 0.1f);	// force a change
                for (int i = 0; i < this.numLevels; i++) {
                    this.levels[i].Invalidate();
                }
            }

            if (viewOrigin == this.currentViewOrigin) {
                return;
            }
            if (r_skipMegaTexture.GetBool()) {
                return;
            }

            this.currentViewOrigin = viewOrigin;

            final float[] texCenter = new float[2];

            // convert the viewOrigin to a texture center, which will
            // be a different conversion for each megaTexture
            for (int i = 0; i < 2; i++) {
                texCenter[i]
                        = (viewOrigin.oGet(0) * this.localViewToTextureCenter[i][0])
                        + (viewOrigin.oGet(1) * this.localViewToTextureCenter[i][1])
                        + (viewOrigin.oGet(2) * this.localViewToTextureCenter[i][2])
                        + this.localViewToTextureCenter[i][3];
            }

            for (int i = 0; i < this.numLevels; i++) {
                this.levels[i].UpdateForCenter(texCenter);
            }
        }

        private static void GenerateMegaMipMaps(megaTextureHeader_t header, idFile outFile) {
            outFile.Flush();

            // out fileSystem doesn't allow read / write access...
            final idFile inFile = fileSystem.OpenFileRead(outFile.GetName());

            int tileOffset = 1;
            int width = header.tilesWide;
            int height = header.tilesHigh;

            final int tileSize = header.tileSize * header.tileSize * 4;
            final byte[] oldBlock = new byte[tileSize];
            final byte[] newBlock = new byte[tileSize];

            while ((width > 1) || (height > 1)) {
                int newHeight = (height + 1) >> 1;
                if (newHeight < 1) {
                    newHeight = 1;
                }
                final int newWidth = (width + 1) >> 1;
                if (width < 1) {
                    width = 1;
                }
                common.Printf("generating %d x %d block mip level\n", newWidth, newHeight);

                int tileNum;

                for (int y = 0; y < newHeight; y++) {
                    common.Printf("row %d\n", y);
                    session.UpdateScreen();

                    for (int x = 0; x < newWidth; x++) {
                        // mip map four original blocks down into a single new block
                        for (int yy = 0; yy < 2; yy++) {
                            for (int xx = 0; xx < 2; xx++) {
                                final int tx = (x * 2) + xx;
                                final int ty = (y * 2) + yy;

                                if ((tx > width) || (ty > height)) {
                                    // off edge, zero fill
//							memset( newBlock, 0, sizeof( newBlock ) );
                                } else {
                                    tileNum = tileOffset + (ty * width) + tx;
                                    inFile.Seek(tileNum * tileSize, FS_SEEK_SET);
                                    inFile.Read(ByteBuffer.wrap(oldBlock), tileSize);
                                }
                                // mip map the new pixels
                                for (int yyy = 0; yyy < (TILE_SIZE / 2); yyy++) {
                                    for (int xxx = 0; xxx < (TILE_SIZE / 2); xxx++) {
                                        final int in = ((yyy * 2 * TILE_SIZE) + (xxx * 2)) * 4;
                                        final int out = (((((TILE_SIZE / 2) * yy) + yyy) * TILE_SIZE) + ((TILE_SIZE / 2) * xx) + xxx) * 4;
                                        newBlock[out + 0] = (byte) ((oldBlock[in + 0] + oldBlock[in + 4] + oldBlock[in + 0 + (TILE_SIZE * 4)] + oldBlock[in + 4 + (TILE_SIZE * 4)]) >> 2);
                                        newBlock[out + 1] = (byte) ((oldBlock[in + 1] + oldBlock[in + 5] + oldBlock[in + 1 + (TILE_SIZE * 4)] + oldBlock[in + 5 + (TILE_SIZE * 4)]) >> 2);
                                        newBlock[out + 2] = (byte) ((oldBlock[in + 2] + oldBlock[in + 6] + oldBlock[in + 2 + (TILE_SIZE * 4)] + oldBlock[in + 6 + (TILE_SIZE * 4)]) >> 2);
                                        newBlock[out + 3] = (byte) ((oldBlock[in + 3] + oldBlock[in + 7] + oldBlock[in + 3 + (TILE_SIZE * 4)] + oldBlock[in + 7 + (TILE_SIZE * 4)]) >> 2);
                                    }
                                }

                                // write the block out
                                tileNum = tileOffset + (width * height) + (y * newWidth) + x;
                                outFile.Seek(tileNum * tileSize, FS_SEEK_SET);
                                outFile.Write(ByteBuffer.wrap(newBlock), tileSize);

                            }
                        }
                    }
                }
                tileOffset += width * height;
                width = newWidth;
                height = newHeight;
            }

//	delete inFile;
        }

        /*
         ====================
         GenerateMegaPreview

         Make a 2k x 2k preview image for a mega texture that can be used in modeling programs
         ====================
         */
        private static void GenerateMegaPreview(final String fileName) {
            final idFile fileHandle = fileSystem.OpenFileRead(fileName);
            if (null == fileHandle) {
                common.Printf("idMegaTexture: failed to open %s\n", fileName);
                return;
            }

            final idStr outName = new idStr(fileName);
            outName.StripFileExtension();
            outName.oPluSet("_preview.tga");

            common.Printf("Creating %s.\n", outName.getData());

            megaTextureHeader_t header;
            final ByteBuffer headerBuffer = ReadDdsFileHeader_t();

            fileHandle.Read(headerBuffer);
            header = ReadDdsFileHeader_t(headerBuffer);

            if ((header.tileSize < 64) || (header.tilesWide < 1) || (header.tilesHigh < 1)) {
                common.Printf("idMegaTexture: bad header on %s\n", fileName);
                return;
            }

            final int tileSize = header.tileSize;
            int width = header.tilesWide;
            int height = header.tilesHigh;
            int tileOffset = 1;
            final int tileBytes = tileSize * tileSize * 4;
            // find the level that fits
            while (((width * tileSize) > 2048) || ((height * tileSize) > 2048)) {
                tileOffset += width * height;
                width >>= 1;
                if (width < 1) {
                    width = 1;
                }
                height >>= 1;
                if (height < 1) {
                    height = 1;
                }
            }

            final ByteBuffer pic = ByteBuffer.allocate(width * height * tileBytes);// R_StaticAlloc(width * height * tileBytes);
            final ByteBuffer oldBlock = ByteBuffer.allocate(tileBytes);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int tileNum = tileOffset + (y * width) + x;
                    fileHandle.Seek(tileNum * tileBytes, FS_SEEK_SET);
                    fileHandle.Read(oldBlock, tileBytes);

                    for (int yy = 0; yy < tileSize; yy++) {
//				memcpy( pic + ( ( y * tileSize + yy ) * width * tileSize + x * tileSize  ) * 4,
//					oldBlock + yy * tileSize * 4, tileSize * 4 );
                        pic.position(((((y * tileSize) + yy) * width * tileSize) + (x * tileSize)) * 4);
                        pic.put(oldBlock.array(), yy * tileSize * 4, tileSize * 4);
                    }
                }
            }

            R_WriteTGA(outName.getData(), pic, width * tileSize, height * tileSize, false);

//            R_StaticFree(pic);
//	delete fileHandle;
        }
    }

    /*

     allow sparse population of the upper detail tiles

     */
    static int RoundDownToPowerOfTwo(int num) {
        int pot;
        for (pot = 1; (pot * 2) <= num; pot <<= 1) {
        }
        return pot;
    }
    static fillColors fillColor = new fillColors();

    static class fillColors {

        private int intVal;

        public int getIntVal() {
            return this.intVal;
        }

        public void setIntVal(int intVal) {
            this.intVal = intVal;
        }

        public byte getColor(final int index) {
            return (byte) ((this.intVal >> index) & 0xFF);
        }

        public void setColor(final int index, final short color) {
            final int down = 0xFF << index;
            this.intVal &= ~down;
            this.intVal |= ((color & 0xFF) << index);
        }
    }
    static final short[][] colors/*[8][4]*/ = {
                {0, 0, 0, 55},
                {255, 0, 0, 255},
                {0, 255, 0, 255},
                {255, 255, 0, 255},
                {0, 0, 255, 255},
                {255, 0, 255, 255},
                {0, 255, 255, 255},
                {255, 255, 255, 255}
            };

    static class R_EmptyLevelImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_EmptyLevelImage();

        private R_EmptyLevelImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final int c = MAX_LEVEL_WIDTH * MAX_LEVEL_WIDTH;
            final ByteBuffer data = ByteBuffer.allocate(c * 4);

            for (int i = 0; i < c; i++) {
                data.putInt(i, fillColor.intVal);
            }

            // FIXME: this won't live past vid mode changes
            image.GenerateImage(data, MAX_LEVEL_WIDTH, MAX_LEVEL_WIDTH, TF_DEFAULT, false, TR_REPEAT, TD_HIGH_QUALITY);
        }
    }

    //===================================================================================================
    static class _TargaHeader {

        char id_length, colormap_type, image_type;
        short colormap_index, colormap_length;
        char colormap_size;
        short x_origin, y_origin, width, height;
        char pixel_size, attributes;
    }

    static byte ReadByte(idFile f) {
        final ByteBuffer b = ByteBuffer.allocate(1);

        f.Read(b, 1);
        return b.get();
    }

    static short ReadShort(idFile f) {
        final ByteBuffer b = ByteBuffer.allocate(2);

        f.Read(b, 2);

//        return (short) (b[0] + (b[1] << 8));
        return b.getShort();
    }
}
