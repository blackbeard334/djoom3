/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/* JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *  
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 *   
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.jcraft.jorbis;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

public final class VorbisFile implements AutoCloseable {

    /* a shade over 8k; anyone using pages well over 8k gets what they deserve */
    private static final int CHUNKSIZE = 8500;
    private static final int SEEK_SET  = 0;
    private static final int SEEK_CUR  = 1;
    private static final int SEEK_END  = 2;
    
    private static final int NOTOPEN   = 0;
    private static final int PARTOPEN  = 1;
    private static final int OPENED    = 2;
    private static final int STREAMSET = 3;
    private static final int INITSET   = 4;

    private static final int OV_FALSE = -1;
    private static final int OV_EOF   = -2;
    private static final int OV_HOLE  = -3;

    private static final int OV_EREAD      = -128;
    private static final int OV_EFAULT     = -129;
    private static final int OV_EIMPL      = -130;
    public  static final int OV_EINVAL     = -131;
    private static final int OV_ENOTVORBIS = -132;
    private static final int OV_EBADHEADER = -133;
    private static final int OV_EVERSION   = -134;
    private static final int OV_ENOTAUDIO  = -135;
    private static final int OV_EBADPACKET = -136;
    private static final int OV_EBADLINK   = -137;
    private static final int OV_ENOSEEK    = -138;

    private ByteBuffer datasource;
    private static final boolean seekable = true;//TODO:remove
    private long offset;
    private long end;

    private SyncState oy = new SyncState();

    private int       links;
    private long[]    offsets;
    private long[]    dataoffsets;
    private long[]    serialNos;
    private long[]    pcmlengths;
    private Info[]    vi;
    private Comment[] vc;

    // Decoding working state local storage
    private long pcm_offset;
    private int ready_state;

    private long[] current_serialno = {0};
    private int current_link;

    private float bittrack;
    private float samptrack;

    private StreamState os = new StreamState(); // take physical pages, weld into a logical
                                                // stream of packets
    private DspState    vd = new DspState();    // central working state for 
                                                // the packet->PCM decoder
    private Block       vb = new Block(vd);     // local working space for packet->PCM decode

    //ov_callbacks callbacks;
    public VorbisFile(String file) throws JOrbisException {
        super();
        try (FileChannel channel = FileChannel.open(Paths.get(file))) {
            ByteBuffer is = ByteBuffer.allocate((int) channel.size());
//            is = new SeekableInputStream(file);
            channel.read(is);
            int ret = open(is, null, 0);
            if (ret == -1) {
                throw new JOrbisException("VorbisFile: open return -1");
            }
        } catch (IOException | JOrbisException e) {
            throw new JOrbisException("VorbisFile: " + e.toString());
        }
    }

    /**{@link #VorbisFile(java.io.InputStream, byte[], int)} with (is, null, -1, SEEK_TYPE.ONE_WAY_SEEKABLE)*/ 
    public VorbisFile(ByteBuffer is) throws JOrbisException {
        this(is, null, -1);
    }
    
    public VorbisFile(ByteBuffer is, byte[] initial, int ibytes) throws JOrbisException {
        int ret = open(is, initial, ibytes);
        if (ret == -1) {
        }
    }

    /* read a little more data from the file/pipe into the ogg_sync framer */
    private int get_data() {
        if (datasource != null) {
            oy.buffer(CHUNKSIZE);
            byte[] buffer = oy.data;
            final int remainingChunk = datasource.remaining();
            final int chunk;

            if (remainingChunk >= CHUNKSIZE) {
                chunk = CHUNKSIZE;
            } else {
                chunk = remainingChunk;
            }

            datasource.get(buffer, 0, chunk);
            oy.wrote(chunk);
            return chunk;
        }
        return 0;
    }

    /* save a tiny smidge of verbosity to make the code more readable */
    private void seek_helper(long offst) {
        if (datasource != null) {
            fseek(datasource, offst, SEEK_SET);
            this.offset = offst;
            oy.reset();
        } else {
            // shouldn't happen unless someone writes a broken callback
        }
    }

    /* from the head of the stream, get the next page.  boundary specifies
     if the function is allowed to fetch more data from the stream (and
     how much) or only use internally buffered data.

     boundary: -1) unbounded search
     0) read no additional data; use cached only
     n) search for a new page beginning for n bytes

     return:   <0) did not find a page (OV_FALSE, OV_EOF, OV_EREAD)
     n) found a page at absolute offset n */
    private int get_next_page(Page page, long boundary) {
        if (boundary > 0) {
            boundary += offset;
        }
        while (true) {
            int more;
            if (boundary > 0 && offset >= boundary) {
                return OV_FALSE;
            }
            more = oy.pageseek(page);
            if (more < 0) {
                // skipped n bytes
                offset -= more;
            } else {
                if (more == 0) {
                    // send more paramedics
                    if (boundary == 0) {
                        return OV_FALSE;
                    }
                    int ret = get_data();
                    if (ret == 0) {
                        return OV_EOF;
                    }
                    if (ret < 0) {
                        return OV_EREAD;
                    }
                } else {
                    // got a page.  Return the offset at the page beginning,
		    // advance the internal offset past the page end 
                    int ret = (int) offset; //!!!
                    offset += more;
                    return ret;
                }
            }
        }
    }

    /* find the latest page beginning before the current stream cursor
     position. Much dirtier than the above as Ogg doesn't have any
     backward search linkage.  no 'readp' as it will certainly have to
     read. 
     returns offset or OV_EREAD, OV_FAULT */
    private int get_prev_page(Page page) {
        long begin = offset; //!!!
        long end = begin;
        int ret;
        int offst = -1;
        while (offst == -1) {
            begin -= CHUNKSIZE;
            if (begin < 0) {
                begin = 0;
            }
            seek_helper(begin);
            while (offset < end) {
                ret = get_next_page(page, end - offset);
                if (ret == OV_EREAD) {
                    return OV_EREAD;
                }
                if (ret < 0) {
//                    if (offst == -1) {
//                        throw new JOrbisException();
//                    }
                    break;
                } else {
                    offst = ret;
                }
            }
        }
        // we have the offset.  Actually snork and hold the page now
        seek_helper(offst); //!!!
        ret = get_next_page(page, CHUNKSIZE);
        if (ret < 0) {
            // this shouldn't be possible 
            return OV_EFAULT;
        }
        return offst;
    }

    /* finds each bitstream link one at a time using a bisection search
     (has to begin by knowing the offset of the lb's initial page).
     Recurses for each link so it can alloc the link storage after
     finding them all, then unroll and fill the cache at the same time */
    private int bisect_forward_serialno(long begin, long searched, long end, long currentNo, int m) {
        long endsearched = end;
        long next = end;
        Page page = new Page();
        int ret;

        // the below guards against garbage seperating the last and
	// first pages of two links.
        while (searched < endsearched) {
            long bisect;
            if (endsearched - searched < CHUNKSIZE) {
                bisect = searched;
            } else {
                bisect = (searched + endsearched) / 2;
            }

            seek_helper(bisect);
            ret = get_next_page(page, -1);
            if (ret == OV_EREAD) {
                return OV_EREAD;
            }
            if (ret < 0 || page.serialno() != currentNo) {
                endsearched = bisect;
                if (ret >= 0) {
                    next = ret;
                }
            } else {
                searched = ret + page.header_len + page.body_len;
            }
        }
        seek_helper(next);
        ret = get_next_page(page, -1);
        if (ret == OV_EREAD) {
            return OV_EREAD;
        }

        if (searched >= end || ret < 0) {
            links = m + 1;
            offsets = new long[links + 1];
            serialNos = new long[links];
            offsets[m + 1] = searched;
        } else {
            ret = bisect_forward_serialno(next, offset, end, page.serialno(), m + 1);
            if (ret == OV_EREAD) {
                return OV_EREAD;
            }
        }
        offsets[m] = begin;
        serialNos[m] = currentNo;
        return 0;
    }

    /* uses the local ogg_stream storage in vf; this is important for
     non-streaming input sources */
    private int fetch_headers(Info vi, Comment vc, long[] serialno, Page og_ptr) {
        Page og = new Page();
        Packet op = new Packet();
        int ret;

        if (og_ptr == null) {
            ret = get_next_page(og, CHUNKSIZE);
            if (ret == OV_EREAD) {
                return OV_EREAD;
            }
            if (ret < 0) {
                return OV_ENOTVORBIS;
            }
            og_ptr = og;
        }
        
        os.reset_serialno(og_ptr.serialno());
        if (serialno != null) {
            serialno[0] = og_ptr.serialno();
        }
        this.ready_state = STREAMSET;

        // extract the initial header from the first page and verify that the
        // Ogg bitstream is in fact Vorbis data
        vi.init();
        vc.init();

        int i = 0;
        while (i < 3) {
            os.pagein(og_ptr);
            while (i < 3) {
                int result = os.packetout(op);
                if (result == 0) {
                    break;
                }
                if (result == -1) {
                    vi.clear();
                    vc.clear();
//                    os.clear();
                    this.ready_state = OPENED;
                    return OV_EBADHEADER;
                }
                if ((ret = vi.synthesis_headerin(vc, op)) != 0) {
                    vi.clear();
                    vc.clear();
//                    os.clear();
                    this.ready_state = OPENED;
                    return ret;
                }
                i++;
            }
            if (i < 3) {
                if (get_next_page(og_ptr, CHUNKSIZE) < 0) {
                    vi.clear();
                    vc.clear();
//                    os.clear();
                    this.ready_state = OPENED;
                    return OV_EBADHEADER;
                }
            }
        }
        return 0;
    }

    /* last step of the OggVorbis_File initialization; get all the
     vorbis_info structs and PCM positions.  Only called by the seekable
     initialization (local stream storage is hacked slightly; pay
     attention to how that's done) */
    /* this is void and does not propogate errors up because we want to be
     able to open and use damaged bitstreams as well as we can.  Just
     watch out for missing information for links in the OggVorbis_File
     struct */
    private void prefetch_all_headers(long dataoffset)
            throws JOrbisException {
        Page og = new Page();
        int ret;

        vi = ogg_realloc(vi, links);
        vc = ogg_realloc(vc, links);
        dataoffsets = new long[links];
        pcmlengths = new long[links * 2];

        for (int i = 0; i < links; i++) {
            if (i == 0) {
                // we already grabbed the initial header earlier.  Just set the offset
//                vi[i] = first_i;//TODO:???
//                vc[i] = first_c;
                dataoffsets[i] = dataoffset;
                seek_helper(dataoffset);
            } else {
                // seek to the location of the initial header
                seek_helper(offsets[i]); //!!!
                if (fetch_headers(vi[i], vc[i], null, null) < 0) {
                    dataoffsets[i] = -1;
                } else {
                    dataoffsets[i] = offset;
//                    os.clear();
                }
            }

            // fetch beginning PCM offset
            if (this.dataoffsets[i] != -1) {
                int accumulated = 0;
                long lastblock = -1;
                int result;

                this.os.init(this.serialNos[i]);

                while (true) {
                    Packet op = new Packet();

                    ret = get_next_page(og, -1);
                    if (ret < 0) {
                        //this should not be possible unless the file is
                        //truncated/mangled 
                        break;
                    }

                    if (og.serialno() != this.serialNos[i]) {
                        break;
                    }

                    // count blocksizes of all frames in the page
                    this.os.pagein(og);
                    while ((result = this.os.packetout(op)) != 0) {
                        if (result > 0) { // ignore holes

                            long thisblock = this.vi[i].blocksize(op);
                            if (lastblock != -1) {
                                accumulated += (lastblock + thisblock) >> 2;
                            }
                            lastblock = thisblock;
                        }
                    }

                    if (og.granulepos() != -1) {
                        // pcm offset of last packet on the first audio page
                        accumulated = (int) (og.granulepos() - accumulated);
                        break;
                    }
                }

                // less than zero?  This is a stream with samples trimmed off
                // the beginning, a normal occurrence; set the offset to zero
                if (accumulated < 0) {
                    accumulated = 0;
                }

                this.pcmlengths[i * 2] = accumulated;
            }

            // get the PCM length of this link. To do this,
            // get the last page of the stream
            {
                long end = offsets[i + 1]; //!!!
                seek_helper(end);

                while (true) {
                    ret = get_prev_page(og);
                    if (ret < 0) {
                        // this should not be possible
                        vi[i].clear();
                        vc[i].clear();
                        break;
                    }
                    if (og.granulepos() != -1) {
//                        serialNos[i] = og.serialno();
                        pcmlengths[i * 2 + 1] = og.granulepos() - pcmlengths[i * 2];
                        break;
                    }
                }
            }
        }
    }

    private int make_decode_ready() {
        if (ready_state > STREAMSET) {
//            System.exit(1);
            return 0;
        }
        if (ready_state < STREAMSET) {
            return OV_EFAULT;
        }
        if (vd.synthesis_init(vi[0]) != 0) {
            return OV_EBADLINK;
        }
        vb.init(vd);
        ready_state = INITSET;
        this.bittrack = 0;
        this.samptrack = 0;
        return (0);
    }

    private int open_seekable2() throws JOrbisException {
        long serialno = current_serialno[0];
        long dataoffset = offset, end;
        Page og = new Page();

        /* we're partially open and have a first link header state in
         storage in vf */
        /* we can seek, so set out learning all about this file */
        fseek(datasource, 0, SEEK_END);
        offset = this.end = ftell(datasource);

        /* We get the offset for the last page of the physical bitstream.
         Most OggVorbis files will contain a single logical bitstream */
        end = get_prev_page(og);
        if (end < 0) {
            return (int) end;
        }

        // more than one logical bitstream?
        if (og.serialno() != serialno) {
            // Chained bitstream. Bisect-search each logical bitstream
            // section.  Do so based on serial number only
            if (bisect_forward_serialno(0, 0, end + 1, serialno, 0) < 0) {
                return (OV_EREAD);
            }

        } else {
            // Only one logical bitstream
            if (bisect_forward_serialno(0, end, end + 1, serialno, 0) != 0) {
                return (OV_EREAD);
            }

        }

        // the initial header memory is referenced by vf after; don't free it
        prefetch_all_headers(dataoffset);
        return ov_raw_seek(0);
    }

    @Deprecated
    private int open_nonseekable() {
        // we cannot seek. Set up a 'single' (current) logical bitstream entry
        links = 1;
        vi = new Info[links];
        vi[0] = new Info(); // ??
        vc = new Comment[links];
        vc[0] = new Comment(); // ?? bug?

        // Try to fetch the headers, maintaining all the storage
        if (fetch_headers(vi[0], vc[0], current_serialno, null) == -1) {
            return (-1);
        }
        make_decode_ready();
        return 0;
    }

    // clear out the current logical bitstream decoder
    private void decode_clear() {
        os.clear();
        vd.clear();
        vb.clear();
        ready_state = OPENED;
        bittrack = 0.f;
        samptrack = 0.f;
    }

    /* fetch and process a packet.  Handles the case where we're at a
     bitstream boundary and dumps the decoding machine.  If the decoding
     machine is unloaded, it loads it.  It also keeps pcm_offset up to
     date (seek and read both use this.  seek uses a special hack with
     readp). 

     return: <0) error, OV_HOLE (lost packet) or OV_EOF
     0) need more data (only if readp==0)
     1) got a packet 
     */
    private int process_packet(boolean readp, boolean spanp) {
        Page og = new Page();

        // handle one packet.  Try to fetch it from current stream state
        // extract packets from page
        while (true) {
            // process a packet if we can.  If the machine isn't loaded,
            // neither is a page
            if (ready_state == INITSET) {
                while (true) {
                    Packet op = new Packet();
                    int result = os.packetout(op);
                    long granulepos;

                    if (result == -1) {
                        return (OV_HOLE); // hole in the data.
                    }

                    if (result > 0) {
                        // got a packet.  process it
                        granulepos = op.granulepos;
                        if (vb.synthesis(op) == 0) { // lazy check for lazy
                            // header handling.  The
                            // header packets aren't
                            // audio, so if/when we
                            // submit them,
                            // vorbis_synthesis will
                            // reject them

                            // suck in the synthesis data and track bitrate
                            {
                                int oldsamples = vd.synthesis_pcmout(null);

                            // for proper use of libvorbis within libvorbisfile,
                                // oldsamples will always be zero.
                                if (oldsamples != 0) {
                                    return OV_EFAULT;
                                }

                                vd.synthesis_blockin(vb);
                                samptrack += vd.synthesis_pcmout(null) - oldsamples;
                                bittrack += op.bytes * 8;
                            }

                            // update the pcm offset.
                            if (granulepos != -1 && op.e_o_s == 0) {
                                int link = (seekable ? current_link : 0);
                                int samples;
                            // this packet has a pcm_offset on it (the last packet
                                // completed on a page carries the offset) After processing
                                // (above), we know the pcm position of the *last* sample
                                // ready to be returned. Find the offset of the *first*
                                // 
                                // As an aside, this trick is inaccurate if we begin
                                // reading anew right at the last page; the end-of-stream
                                // granulepos declares the last frame in the stream, and the
                                // last packet of the last page may be a partial frame.
                                // So, we need a previous granulepos from an in-sequence page
                                // to have a reference point.  Thus the !op.e_o_s clause above

                                if (seekable && link > 0) {
                                    granulepos -= pcmlengths[link * 2];
                                }
                                if (granulepos < 0) {
                                    granulepos = 0;
                                // actually, this shouldn't be possible
                                    // here unless the stream
                                    // is very broken
                                }

                                samples = vd.synthesis_pcmout(null);
                                granulepos -= samples;
                                for (int i = 0; i < link; i++) {
                                    granulepos += pcmlengths[i * 2 + 1];
                                }
                                pcm_offset = granulepos;
                            }
                            return 1;
                        }
                    } else {
                        break;
                    }
                }
            }

            if (ready_state >= OPENED) {
                if (!readp) {
                    return 0;
                }
                if (get_next_page(og, -1) < 0) {
                    return OV_EOF; // eof. leave unitialized
                }
                // bitrate tracking; add the header's bytes here, the body bytes
                // are done by packet above
                bittrack += og.header_len * 8;

                // has our decoding just traversed a bitstream boundary?
                if (ready_state == INITSET) {
                    if (current_serialno[0] != og.serialno()) {
                        if (!spanp) {
                            return OV_EOF;
                        }
                        decode_clear();
                    }
                }
            }

            // Do we need to load a new machine before submitting the page?
            // This is different in the seekable and non-seekable cases.  
            // 
            // In the seekable case, we already have all the header
            // information loaded and cached; we just initialize the machine
            // with it and continue on our merry way.
            // 
            // In the non-seekable (streaming) case, we'll only be at a
            // boundary if we just left the previous logical bitstream and
            // we're now nominally at the header of the next bitstream
            if (ready_state != INITSET) {
                int i;
                if (ready_state < STREAMSET) {
                    if (seekable) {
                        current_serialno[0] = og.serialno();

                        // match the serialno to bitstream section.  We use this rather than
                        // offset positions to avoid problems near logical bitstream boundaries
                        for (i = 0; i < links; i++) {
                            if (serialNos[i] == current_serialno[0]) {
                                break;
                            }
                        }
                        if (i == links) {
                            return OV_EBADLINK; // sign of a bogus stream.  error out,
                        }                       // leave machine uninitialized
                        current_link = i;

                        os.init(current_serialno[0]);
//                        os.reset();
                        ready_state = STREAMSET;
                    } else {
                        // we're streaming
                        // fetch the three header packets, build the info struct
                        int ret = fetch_headers(vi[0], vc[0], current_serialno, og);
                        if (ret != 0) {
                            return ret;
                        }
                        current_link++;
                    }
                }
                int ret = make_decode_ready();
                if (ret < 0) {
                    return ret;
                }
            }
            os.pagein(og);
        }
    }

    // The helpers are over; it's all toplevel interface from here on out
    // clear out the OggVorbis_File struct
    private int ov_clear() {
        vb.clear();
        vd.clear();
        os.clear();

        if (vi != null && links != 0) {
            for (int i = 0; i < links; i++) {
                vi[i].clear();
                vc[i].clear();
            }
//            vi = null;
//            vc = null;
        }
        dataoffsets = null;
        pcmlengths = null;
        serialNos = null;
        offsets = null;
        oy.clear();
        
        if (datasource != null) {
            datasource.position(0);
        }

        return 0;
    }

    static void fseek(ByteBuffer fis, long off, int origin) {
//        if (fis instanceof SeekableInputStream) {
//            SeekableInputStream sis = (SeekableInputStream) fis;
//            try {
//                if (whence == SEEK_SET) {
//                    sis.seek(off);
//                } else if (whence == SEEK_END) {
//                    sis.seek(sis.getLength() - off);
//                } else {
//                }
//            } catch (Exception e) {
//            }
//            return 0;
//        }
//        try {
//            if (whence == 0) {
//                fis.reset();
//            }
//            fis.skip(off);
//        } catch (Exception e) {
//            return -1;
//        }
//        return 0;
        switch (origin) {
            case SEEK_SET:
                origin = 0;
                break;
            case SEEK_CUR:
                origin = fis.position();
                break;
            case SEEK_END:
                origin = fis.capacity();
        }
        fis.position((int) (off + origin));
    }

    static long ftell(ByteBuffer fis) {
//        try {
//            if (fis instanceof SeekableInputStream) {
//                SeekableInputStream sis = (SeekableInputStream) fis;
//                return (sis.tell());
//            }
//        } catch (Exception e) {
//        }
//        return 0;
        return fis.position();
    }

    // inspects the OggVorbis file and finds/documents all the logical
    // bitstreams contained in it.  Tries to be tolerant of logical
    // bitstream sections that are truncated/woogie. 
    //
    // return: -1) error
    //          0) OK
    int open(ByteBuffer is, byte[] initial, int ibytes) throws JOrbisException {
        return open_callbacks(is, initial, ibytes);
    }
    
    int open_callbacks(ByteBuffer is, byte[] initial, int ibytes/*, callbacks callbacks*/) throws JOrbisException {
        int ret = ov_open1(is, initial, ibytes);
        if (ret != 0) {
            return ret;
        }
        return ov_open2(is, initial, ibytes);
    }

    int ov_open1(ByteBuffer is, byte[] initial, int ibytes/*, callbacks callbacks*/) throws JOrbisException {
        int ret;

        datasource = is;
        
        // init the framing state
        oy.init();

        // perhaps some data was previously read into a buffer for testing
        // against other stream types.  Allow initialization from this
        // previously read data (as we may be reading from a non-seekable
        // stream)
        if (initial != null) {
            int index = oy.buffer(ibytes);
            System.arraycopy(initial, 0, oy.data, index, ibytes);
            oy.wrote(ibytes);
        }
        
        // No seeking yet; Set up a 'single' (current) logical bitstream entry for partial open
        links = 1;
        vi = new Info[links];
        vc = new Comment[links];
        vi[0] = new Info();
        vc[0] = new Comment();
        os.init(-1);
        
         // Try to fetch the headers, maintaining all the storage
        if ((ret = fetch_headers(vi[0], vc[0], current_serialno, null)) < 0) {
            datasource = null;
            ov_clear();
        } else {
            ready_state = PARTOPEN;
        }
        return (ret);
    }
    
    int ov_open2(ByteBuffer is, byte[] initial, int ibytes/*, callbacks callbacks*/) throws JOrbisException{
        if (ready_state != PARTOPEN) {
            return OV_EINVAL;
        }
        ready_state = OPENED;
        if (seekable) {
            int ret = open_seekable2();
            if (ret != 0) {
                datasource = null;
                ov_clear();
            }
            return (ret);
        } else {
            ready_state = STREAMSET;
        }

        return 0;
    }

    // How many logical bitstreams in this physical bitstream?
    public int streams() {
        return links;
    }

    // Is the FILE * associated with vf seekable?
    public boolean seekable() {
        return seekable;
    }

    // returns the bitrate for a given logical bitstream or the entire
    // physical bitstream.  If the file is open for random access, it will
    // find the *actual* average bitrate.  If the file is streaming, it
    // returns the nominal bitrate (if set) else the average of the
    // upper/lower bounds (if set) else -1 (unset).
    // 
    // If you want the actual bitrate field settings, get them from the
    // vorbis_info structs
    public int bitrate(int i) {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        
        if (i >= links) {
            return OV_EINVAL;
        }
        if (!seekable && i != 0) {
            return (bitrate(0));
        }
        if (i < 0) {
            long bits = 0;
            for (int j = 0; j < links; j++) {
                bits += (offsets[j + 1] - dataoffsets[j]) * 8;
            }
            return ((int) Math.rint(bits / time_total(-1)));
        } else {
            if (seekable) {
                // return the actual bitrate
                return ((int) Math.rint((offsets[i + 1] - dataoffsets[i]) * 8 / time_total(i)));
            } else {
                // return nominal if set
                if (vi[i].bitrate_nominal > 0) {
                    return vi[i].bitrate_nominal;
                } else {
                    if (vi[i].bitrate_upper > 0) {
                        if (vi[i].bitrate_lower > 0) {
                            return (vi[i].bitrate_upper + vi[i].bitrate_lower) / 2;
                        } else {
                            return vi[i].bitrate_upper;
                        }
                    }
                    return OV_FALSE;
                }
            }
        }
    }

    /* returns the actual bitrate since last call.  returns -1 if no
     additional data to offer since last call (or at beginning of stream),
     EINVAL if stream is only partially open 
     */
    public int bitrate_instant() {
        int _link = (seekable ? current_link : 0);
        
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        
        if (samptrack == 0) {
            return OV_FALSE;
        }
        int ret = (int) (bittrack / samptrack * vi[_link].rate + .5);
        bittrack = 0.f;
        samptrack = 0.f;
        return (ret);
    }

    /* Guess */
    public long serialnumber(int i) {
        if (i >= links) {
            return (serialnumber(links - 1));
        }
        if (!seekable && i >= 0) {
            return (serialnumber(-1));
        }
        if (i < 0) {
            return (current_serialno[0]);
        } else {
            return (serialNos[i]);
        }
    }

    /* returns: total raw (compressed) length of content if i==-1
     raw (compressed) length of that logical bitstream for i==0 to n
     OV_EINVAL if the stream is not seekable (we can't know the length)
     or if stream is only partially open
     */
    public long raw_total(int i) {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
    
        if (!seekable || i >= links) {
            return OV_EINVAL;
        }
        if (i < 0) {
            long acc = 0; // bug?
            for (int j = 0; j < links; j++) {
                acc += raw_total(j);
            }
            return (acc);
        } else {
            return (offsets[i + 1] - offsets[i]);
        }
    }

    /**
     * @return
     * <p>
     * total PCM length (samples) of content if i==-1 PCM length (samples) of
     * that logical bitstream for i==0 to n -1 if the stream is not seekable (we
     * can't know the length)
     * </p>
     * @see
     * <a href="https://www.xiph.org/vorbis/doc/vorbisfile/ov_pcm_total.html">ov_pcm_total</a>
     */
    public long pcm_total(int i) {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        
        if (!seekable || i >= links) {
            return OV_EINVAL;
        }
        if (i < 0) {
            long acc = 0;
            for (int j = 0; j < links; j++) {
                acc += pcm_total(j);
            }
            return (acc);
        } else {
            return (pcmlengths[i * 2 + 1]);
        }
    }

    /* returns: total seconds of content if i==-1
     seconds in that logical bitstream for i==0 to n
     OV_EINVAL if the stream is not seekable (we can't know the
     length) or only partially open 
     */
    public double time_total(int i) {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }

        if (!seekable || i >= links) {
            return OV_EINVAL;
        }
        if (i < 0) {
            double acc = 0;
            for (int j = 0; j < links; j++) {
                acc += time_total(j);
            }
            return (acc);
        } else {
            return ((pcmlengths[i * 2 + 1]) / vi[i].rate);
        }
    }

    /* seek to an offset relative to the *compressed* data. This also
     scans packets to update the PCM cursor. It will cross a logical
     bitstream boundary, but only if it can't get any packets out of the
     tail of the bitstream we seek to (so no surprises).

     returns zero on success, nonzero on failure */
    public int ov_raw_seek(long pos) {
        StreamState work_os = new StreamState();

        if (ready_state < OPENED) {
            return OV_EINVAL;
        }

        if (!seekable) {
            return OV_ENOSEEK; // don't dump machine if we can't seek
        }

        if (pos < 0 || pos > end) {
            return OV_EINVAL;
        }

        // don't yet clear out decoding machine (if it's initialized), in
        // the case we're in the same link.  Restart the decode lapping, and
        // let _fetch_and_process_packet deal with a potential bitstream boundary
        pcm_offset = -1;
        os.reset_serialno(current_serialno[0]); // must set serialno 
        vd.synthesis_restart();

        seek_helper(pos);

        /* we need to make sure the pcm_offset is set, but we don't want to
         advance the raw cursor past good packets just to get to the first
         with a granulepos.  That's not equivalent behavior to beginning
         decoding as immediately after the seek position as possible.

         So, a hack.  We use two stream states; a local scratch state and
         the shared vf->os stream state.  We use the local state to
         scan, and the shared state as a buffer for later decode. 

         Unfortuantely, on the last page we still advance to last packet
         because the granulepos on the last page is not necessarily on a
         packet boundary, and we need to make sure the granpos is
         correct. 
         */
        {
            Page og = new Page();
            Packet op = new Packet();
            int lastBlock = 0;
            int accBlock = 0;
            int thisBlock;
            int eosFlag = 0;

            work_os.init(current_serialno[0]); // get the memory ready
            work_os.reset(); /* eliminate the spurious OV_HOLE
             return from not necessarily
             starting from the beginning */

            while (true) {

                if (ready_state >= STREAMSET) {
                    /* snarf/scan a packet if we can */
                    int result = work_os.packetout(op);

                    if (result > 0) {

//                        if (vi[current_link].codec_setup) {
                            thisBlock = vi[current_link].blocksize(op);
                            if (thisBlock < 0) {
                                os.packetout(null);
                                thisBlock = 0;
                            } else {

                                if (eosFlag != 0) {
                                    os.packetout(null);
                                } else if (lastBlock != 0) {
                                    accBlock += (lastBlock + thisBlock) >> 2;
                                }
                            }

                            if (op.granulepos != -1) {
                                int i, link = current_link;
                                long granulepos = op.granulepos - pcmlengths[link * 2];
                                if (granulepos < 0) {
                                    granulepos = 0;
                                }

                                for (i = 0; i < link; i++) {
                                    granulepos += pcmlengths[i * 2 + 1];
                                }
                                pcm_offset = granulepos - accBlock;
                                break;
                            }
                            lastBlock = thisBlock;
                            continue;
//                        } else {
//                            os.packetout(null);
//                        }
                    }
                }

                if (lastBlock == 0) {
                    if (get_next_page(og, -1) < 0) {
                        pcm_offset = pcm_total(-1);
                        break;
                    }
                } else {
                    /* huh?  Bogus stream with packets but no granulepos */
                    pcm_offset = -1;
                    break;
                }

                /* has our decoding just traversed a bitstream boundary? */
                if (ready_state >= STREAMSET) {
                    if (current_serialno[0] != og.serialno()) {
                        decode_clear(); /* clear out stream state */

                        work_os.clear();
                    }
                }

                if (ready_state < STREAMSET) {
                    int link;

                    current_serialno[0] = og.serialno();
                    for (link = 0; link < links; link++) {
                        if (serialNos[link] == current_serialno[0]) {
                            break;
                        }
                    }
                    if (link == links) { /* sign of a bogus stream. error out, leave machine uninitialized */
//                     goto seek_error; 
                        // dump the machine so we're in a known state

                        pcm_offset = -1;
                        work_os.clear();
                        decode_clear();
                        return OV_EBADLINK;
                    }
                    current_link = link;

                    os.reset_serialno(current_serialno[0]);
                    work_os.reset_serialno(current_serialno[0]);
                    ready_state = STREAMSET;
                }

                os.pagein(og);
                work_os.pagein(og);
                eosFlag = og.eos();
            }
        }

        work_os.clear();
        bittrack = 0.f;
        samptrack = 0.f;

        return 0;

        // seek_error:
        // dump the machine so we're in a known state
        //pcm_offset=-1;
        //decode_clear();
    }

    /* Page granularity seek (faster than sample granularity because we
     don't do the last bit of decode to find a specific sample).

     Seek to the last [granule marked] page preceeding the specified pos
     location, such that decoding past the returned point will quickly
     arrive at the requested position. */
    public int pcm_seek(long pos) {
        int link = -1;
        long result = 0;
        long total = pcm_total(-1);

        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        if (!seekable) {
            return OV_ENOSEEK;
        }

        if (pos < 0 || pos > total) {
            return OV_EINVAL;
        }

        // which bitstream section does this pcm offset occur in?
        for (link = links - 1; link >= 0; link--) {
            total -= pcmlengths[link * 2 + 1];
            if (pos >= total) {
                break;
            }
        }

        /* search within the logical bitstream for the page with the highest
         pcm_pos preceeding (or equal to) pos.  There is a danger here;
         missing pages or incorrect frame number information in the
         bitstream could make our task impossible.  Account for that (it
         would be an error condition) */

        /* new search algorithm by HB (Nicholas Vinen) */
        {
            long end = offsets[link + 1];
            long begin = offsets[link];
            long begintime = pcmlengths[link * 2];
            long endtime = pcmlengths[link * 2 + 1] + begintime;
            long target = pos - total + begintime;
            long best = begin;

            Page og = new Page();
            while (begin < end) {
                long bisect;

                if (end - begin < CHUNKSIZE) {
                    bisect = begin;
                } else {
                    /* take a (pretty decent) guess. */
                    bisect = begin
                            + (target - begintime) * (end - begin)
                            / (endtime - begintime) - CHUNKSIZE;
                    if (bisect <= begin) {
                        bisect = begin + 1;
                    }
                }

                seek_helper(bisect);

                while (begin < end) {
                    result = get_next_page(og, end - offset);
                    if (result == OV_EREAD) {
                        pcm_seek_error(result);
                    }
                    if (result < 0) {
                        if (bisect <= begin + 1) {
                            end = begin; /* found it */
                        } else {
                            if (bisect == 0) {
                                pcm_seek_error(result);
                            }
                            bisect -= CHUNKSIZE;
                            if (bisect <= begin) {
                                bisect = begin + 1;
                            }
                            seek_helper(bisect);
                        }
                    } else {
                        long granulepos = og.granulepos();
                        if (granulepos == -1) {
                            continue;
                        }
                        if (granulepos < target) {
                            best = result;  // raw offset of packet with granulepos
                            begin = offset; // raw offset of next page

                            begintime = granulepos;

                            if (target - begintime > 44100) {
                                break;
                            }
                            bisect = begin; // *not* begin + 1
                        } else {
                            if (bisect <= begin + 1) {
                                end = begin;  /* found it */
                            } else {
                                if (end == offset) { // we're pretty close - we'd be stuck in 
                                    end = result;
                                    bisect -= CHUNKSIZE; // an endless loop otherwise.
                                    if (bisect <= begin) {
                                        bisect = begin + 1;
                                    }
                                    seek_helper(bisect);
                                } else {
                                    end = result;
                                    endtime = granulepos;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            /* found our page. seek to it, update pcm offset. Easier case than
             raw_seek, don't keep packets preceeding granulepos. */
            {
                Page og2 = new Page();
                Packet op = new Packet();

                // seek
                seek_helper(best);
                pcm_offset = -1;

                if (get_next_page(og2, -1) < 0) {
                    return (OV_EOF); // shouldn't happen
                }

                if (link != current_link) {
                    // Different link; dump entire decode machine
                    decode_clear();

                    current_link = link;
                    current_serialno[0] = og2.serialno();
                    ready_state = STREAMSET;

                } else {
                    vd.synthesis_restart();
                }

                os.reset_serialno(current_serialno[0]);
                os.pagein(og2);

                /* pull out all but last packet; the one with granulepos */
                while (true) {
                    result = os.packetpeek(op);
                    if (result == 0) {
                        /* !!! the packet finishing this page originated on a
                         preceeding page. Keep fetching previous pages until we
                         get one with a granulepos or without the 'continued' flag
                         set.  Then just use raw_seek for simplicity. */
                    
                        seek_helper(best);

                        while (true) {
                            result = get_prev_page(og2);
                            if (result < 0) {
                                pcm_seek_error(result);
                            }
                            if (og2.granulepos() > -1
                                    || og2.continued() == 0) {
                                return ov_raw_seek(result);
                            }
                            offset = result;
                        }
                    }
                    if (result < 0) {
                        result = OV_EBADPACKET;
                        pcm_seek_error(result);
                    }
                    if (op.granulepos != -1) {
                        pcm_offset = op.granulepos - pcmlengths[current_link * 2];
                        if (pcm_offset < 0) {
                            pcm_offset = 0;
                        }
                        pcm_offset += total;
                        break;
                    } else {
                        result = os.packetout(null);
                    }
                }
            }
        }

        /* verify result */
        if (pcm_offset > pos || pos > pcm_total(-1)) {
            result = OV_EFAULT;
            pcm_seek_error(result);
        }
        bittrack = 0.f;
        samptrack = 0.f;
        return (0);

//seek_error:
//	/* dump machine so we're in a known state */
//	pcm_offset=-1;
//	_decode_clear(vf);
//	return (int)result;
    }
    
    private int pcm_seek_error(long result) {
        /* dump machine so we're in a known state */
        pcm_offset = -1;
        decode_clear();
        return (int) result;
    }

    /* seek to a playback time relative to the decompressed pcm stream 
     returns zero on success, nonzero on failure */
    private int time_seek(float seconds) {
        // translate time to PCM position and call pcm_seek

        int link = -1;
        long pcm_total = pcm_total(-1);
        double time_total = time_total(-1);
        
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }

        if (!seekable) {
            return OV_ENOSEEK; // don't dump machine if we can't seek
        }
        if (seconds < 0 || seconds > time_total) {
            //goto seek_error;
//            pcm_offset = -1;
//            decode_clear();
            return OV_EINVAL;
        }

        // which bitstream section does this time offset occur in?
        for (link = links - 1; link >= 0; link--) {
            pcm_total -= pcmlengths[link * 2 + 1];
            time_total -= time_total(link);
            if (seconds >= time_total) {
                break;
            }
        }

        // enough information to convert time offset to pcm offset
        {
            long target = (long) (pcm_total + (seconds - time_total) * vi[link].rate);
            return (pcm_seek(target));
        }

        //seek_error:
        // dump machine so we're in a known state
        //pcm_offset=-1;
        //decode_clear();
        //return -1;
    }

    // tell the current stream offset cursor.  Note that seek followed by
    // tell will likely not give the set offset due to caching
    public long raw_tell() {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        
        return (offset);
    }

    // return PCM offset (sample) of next PCM sample to be read
    public long pcm_tell() {
        if (ready_state < OPENED) {
            return OV_EINVAL;
        }
        
        return (pcm_offset);
    }

    // return time offset (seconds) of next PCM sample to be read
    public double time_tell() {
        // translate time to PCM position and call pcm_seek

        int link = -1;
        long pcm_total = 0;
        double time_total = 0;

        if (ready_state < OPENED) {
            return OV_EINVAL;
        }

        if (seekable) {
            pcm_total = pcm_total(-1);
            time_total = time_total(-1);

            // which bitstream section does this time offset occur in?
            for (link = links - 1; link >= 0; link--) {
                pcm_total -= pcmlengths[link * 2 + 1];
                time_total -= time_total(link);
                if (pcm_offset >= pcm_total) {
                    break;
                }
            }
        }

        return ((double) time_total + (double) (pcm_offset - pcm_total) / vi[link].rate);
    }

    /*  link:   -1) return the vorbis_info struct for the bitstream section
     currently being decoded
     0-n) to request information for a specific bitstream section

     In the case of a non-seekable bitstream, any call returns the
     current bitstream.  NULL in the case that the machine is not
     initialized */
    public Info getInfo(int link) {
        if (seekable) {
            if (link < 0) {
                if (ready_state >= STREAMSET) {
                    return vi[current_link];
                } else {
                    return vi[0];
                }
            } else {
                if (link >= links) {
                    return null;
                } else {
                    return vi[link];
                }
            }
        } else {
            return vi[0];
        }
    }

    /* grr, strong typing, grr, no templates/inheritence, grr */
    public Comment getComment(int link) {
        if (seekable) {
            if (link < 0) {
                if (ready_state >= STREAMSET) {
                    return vc[current_link];
                } else {
                    return vc[0];
                }
            } else {
                if (link >= links) {
                    return null;
                } else {
                    return vc[link];
                }
            }
        } else {
            return vc[0];
        }
    }

    private int host_is_big_endian() {
        return 1;
        //    return ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
        //
        //    short pattern = 0xbabe;
        //    unsigned char *bytewise = (unsigned char *)&pattern;
        //    if (bytewise[0] == 0xba) return 1;
        //    assert(bytewise[0] == 0xbe);
        //    return 0;
    }

    /* up to this point, everything could more or less hide the multiple
     logical bitstream nature of chaining from the toplevel application
     if the toplevel application didn't particularly care.  However, at
     the point that we actually read audio back, the multiple-section
     nature must surface: Multiple bitstream sections do not necessarily
     have to have the same number of channels or sampling rate.

     ov_read returns the sequential logical bitstream number currently
     being decoded along with the PCM data in order that the toplevel
     application can take action on channel/sample rate changes.  This
     number will be incremented even for streamed (non-seekable) streams
     (for seekable streams, it represents the actual logical bitstream
     index within the physical bitstream.  Note that the accessor
     functions above are aware of this dichotomy).

     input values: buffer) a buffer to hold packed PCM data for return
     length) the byte length requested to be placed into buffer
     bigendianp) should the data be packed LSB first (0) or
     MSB first (1)
     word) word size for output.  currently 1 (byte) or 
     2 (16 bit short)

     return values: <0) error/hole in data (OV_HOLE), partial open (OV_EINVAL)
     0) EOF
     n) number of bytes of PCM actually returned.  The
     below works on a packet-by-packet basis, so the
     return length is not related to the 'length' passed
     in, just guaranteed to fit.

     *section) set to the logical bitstream number */
    private long read(byte[] buffer, int length, int bigendianp, int word, int sgned, int[] bitstream) {
        int host_endian = host_is_big_endian();
        int index = 0;
        float[][] pcm;
        long samples = 0;

        if (this.ready_state < OPENED) {
            return OV_EINVAL;
        }

        while (true) {
            if (ready_state == INITSET) {
                float[][][] _pcm = new float[1][][];
                samples = vd.synthesis_pcmout(_pcm);
                pcm = _pcm[0];
                if (samples != 0) {
                    break;
                }

                // suck in another packet
                {
                    int ret = process_packet(true, true);
                    if (ret == OV_EOF) {
                        return (0);
                    }
                    if (ret <= 0) {
                        return (ret);
                    }
                }
            }
        }
        if (samples > 0) {

            /* yay! proceed to pack data into the byte buffer */
            long channels = getInfo(-1).channels;
            long bytespersample = word * channels;
            int fpu;
            if (samples > length / bytespersample) {
                samples = length / bytespersample;
            }

            if (samples <= 0) {
                return OV_EINVAL;
            }

            // a tight loop to pack each size
            {
                int val;
                if (word == 1) {
                    int off = (sgned != 0 ? 0 : 128);
                    for (int j = 0; j < samples; j++) {
                        for (int i = 0; i < channels; i++) {
                            val = (int) (pcm[i][j] * 128. + 0.5);
                            if (val > 127) {
                                val = 127;
                            } else if (val < -128) {
                                val = -128;
                            }
                            buffer[index++] = (byte) (val + off);
                        }
                    }
                } else {
                    int off = (sgned != 0 ? 0 : 32768);

                    if (host_endian == bigendianp) {
                        if (sgned != 0) {
                            for (int i = 0; i < channels; i++) { // It's faster in this order
                                float[] src = pcm[i];
                                int dest = i;
                                for (int j = 0; j < samples; j++) {
                                    val = (int) (src[j] * 32768. + 0.5);
                                    if (val > 32767) {
                                        val = 32767;
                                    } else if (val < -32768) {
                                        val = -32768;
                                    }
                                    buffer[dest] = (byte) (val >>> 8);
                                    buffer[dest + 1] = (byte) (val);
                                    dest += channels * 2;
                                }
                            }
                        } else {
                            for (int i = 0; i < channels; i++) {
                                float[] src = pcm[i];
                                int dest = i;
                                for (int j = 0; j < samples; j++) {
                                    val = (int) (src[j] * 32768. + 0.5);
                                    if (val > 32767) {
                                        val = 32767;
                                    } else if (val < -32768) {
                                        val = -32768;
                                    }
                                    buffer[dest] = (byte) ((val + off) >>> 8);
                                    buffer[dest + 1] = (byte) (val + off);
                                    dest += channels * 2;
                                }
                            }
                        }
                    } else if (bigendianp != 0) {
                        for (int j = 0; j < samples; j++) {
                            for (int i = 0; i < channels; i++) {
                                val = (int) (pcm[i][j] * 32768. + 0.5);
                                if (val > 32767) {
                                    val = 32767;
                                } else if (val < -32768) {
                                    val = -32768;
                                }
                                val += off;
                                buffer[index++] = (byte) (val >>> 8);
                                buffer[index++] = (byte) val;
                            }
                        }
                    } else {
                        //int val;
                        for (int j = 0; j < samples; j++) {
                            for (int i = 0; i < channels; i++) {
                                val = (int) (pcm[i][j] * 32768. + 0.5);
                                if (val > 32767) {
                                    val = 32767;
                                } else if (val < -32768) {
                                    val = -32768;
                                }
                                val += off;
                                buffer[index++] = (byte) val;
                                buffer[index++] = (byte) (val >>> 8);
                            }
                        }
                    }
                }
            }

            vd.synthesis_read((int) samples);
            pcm_offset += samples;
            if (bitstream != null) {
                bitstream[0] = current_link;
            }
            return (samples * bytespersample);
        } else {
            return samples;
        }
    }

    /* input values: pcm_channels) a float vector per channel of output
     length) the sample length being read by the app

     return values: <0) error/hole in data (OV_HOLE), partial open (OV_EINVAL)
     0) EOF
     n) number of samples of PCM actually returned.  The
     below works on a packet-by-packet basis, so the
     return length is not related to the 'length' passed
     in, just guaranteed to fit.

     *section) set to the logical bitstream number
     @see <a href="https://www.xiph.org/vorbis/doc/vorbisfile/ov_read_float.html">ov_read_float</a> */
    public int read_float(float[][][] pcm_channels, int length, int[] bitstream) {
        
        if (this.ready_state < OPENED) {
            return OV_EINVAL;
        }

        while (true) {
            if (this.ready_state == INITSET) {
                float[][][] _pcm = new float[1][][];
                int samples = vd.synthesis_pcmout(_pcm);

                if (samples != 0) {
                    if (pcm_channels != null) {
                            pcm_channels[0] = _pcm[0];
                        }
                    if (samples > length) {
                        samples = length;
                    }
                    vd.synthesis_read(samples);
                    this.pcm_offset += samples;
                    if (bitstream != null) {
                        bitstream[0] = this.current_link;
                    }
                    return samples;
                }
            }

            /* suck in another packet */
            {
                int ret = this.process_packet(true, true);
                if (ret == OV_EOF) {
                    return 0;
                }
                if (ret <= 0) {
                    return ret;
                }
            }
        }
    }
    
    public Info[] getInfo() {
        return vi;
    }

    public Comment[] getComment() {
        return vc;
    }

    @Override
    public void close() throws java.io.IOException {
//        datasource.close();
    }
    
    private static <T> T[] ogg_realloc(T[] array, int size) {
        if (array == null || array.length == size) {
            return array;
        }
        T[] temp = (T[]) Array.newInstance(array[0].getClass(), size);
        System.arraycopy(array, 0, temp, 0, array.length);

        return temp;
    }
}
