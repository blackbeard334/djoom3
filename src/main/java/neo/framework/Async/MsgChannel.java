package neo.framework.Async;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.Common.common;
import neo.framework.Compressor.idCompressor;
import neo.framework.File_h.idFile_BitMsg;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Lib.idException;
import neo.sys.sys_public.idPort;
import neo.sys.sys_public.netadr_t;
import static neo.sys.sys_public.netadrtype_t.NA_BAD;
import static neo.sys.win_net.Sys_NetAdrToString;

/**
 *
 */
public class MsgChannel {

    /*

     packet header
     -------------
     2 bytes		id
     4 bytes		outgoing sequence. high bit will be set if this is a fragmented message.
     2 bytes		optional fragment start byte if fragment bit is set.
     2 bytes		optional fragment length if fragment bit is set. if < FRAGMENT_SIZE, this is the last fragment.

     If the id is -1, the packet should be handled as an out-of-band
     message instead of as part of the message channel.

     All fragments will have the same sequence numbers.

     */
    public static final int    MAX_PACKETLEN                  = 1400;    // max size of a network packet
    public static final int    FRAGMENT_SIZE                  = (MAX_PACKETLEN - 100);
    public static final int    FRAGMENT_BIT                   = (1 << 31);
    //
    static final        idCVar net_channelShowPackets         = new idCVar("net_channelShowPackets", "0", CVAR_SYSTEM | CVAR_BOOL, "show all packets");
    static final        idCVar net_channelShowDrop            = new idCVar("net_channelShowDrop", "0", CVAR_SYSTEM | CVAR_BOOL, "show dropped packets");
    //
//    
    /*
     ===============================================================================

     Network channel.

     Handles message fragmentation and out of order / duplicate suppression.
     Unreliable messages are not garrenteed to arrive but when they do, they
     arrive in order and without duplicates. Reliable messages always arrive,
     and they also arrive in order without duplicates. Reliable messages piggy
     back on unreliable messages. As such an unreliable message stream is
     required for the reliable messages to be delivered.

     ===============================================================================
     */
    public static final int    MAX_MESSAGE_SIZE               = 16384;                   // max length of a message, which may be fragmented into multiple packets
    //
    public static final int    CONNECTIONLESS_MESSAGE_ID      = -1;        // id for connectionless messages
    public static final int    CONNECTIONLESS_MESSAGE_ID_MASK = 0x7FFF;    // value to mask away connectionless message id
    //
    public static final int    MAX_MSG_QUEUE_SIZE             = 16384;                 // must be a power of 2

    class idMsgQueue {

        private byte[] buffer = new byte[MAX_MSG_QUEUE_SIZE];
        private int first;      // sequence number of first message in queue
        private int last;       // sequence number of last message in queue
        private int startIndex; // index pointing to the first byte of the first message
        private int endIndex;   // index pointing to the first byte after the last message
        //
        //

        public idMsgQueue() {
            Init(0);
        }

        public void Init(int sequence) {
            first = last = sequence;
            startIndex = endIndex = 0;
        }

        public boolean Add(final byte[] data, final int size) {
            if (GetSpaceLeft() < size + 8) {
                return false;
            }
            int sequence = last;
            WriteShort(size);
            WriteLong(sequence);
            WriteData(data, size);
            last++;
            return true;
        }

        public boolean Get(byte[] data, int[] size) {
            if (first == last) {
                size[0] = 0;
                return false;
            }
            int sequence;
//	size = ReadShort();
            sequence = ReadLong();
            ReadData(data, size[0]);
            assert (sequence == first);
            first++;
            return true;
        }

        public int GetTotalSize() {
            if (startIndex <= endIndex) {
                return (endIndex - startIndex);
            } else {
                return (buffer.length - startIndex + endIndex);
            }
        }

        public int GetSpaceLeft() {
            if (startIndex <= endIndex) {
                return buffer.length - (endIndex - startIndex) - 1;
            } else {
                return (startIndex - endIndex) - 1;
            }
        }

        public int GetFirst() {
            return first;
        }

        public int GetLast() {
            return last;
        }

        public void CopyToBuffer(byte[] buf) {
            if (startIndex <= endIndex) {
//		memcpy( buf, buffer + startIndex, endIndex - startIndex );
                System.arraycopy(buffer, startIndex, buf, 0, endIndex - startIndex);
            } else {
//		memcpy( buf, buffer + startIndex, sizeof( buffer ) - startIndex );
                System.arraycopy(buffer, startIndex, buf, 0, buffer.length - startIndex);
//		memcpy( buf + sizeof( buffer ) - startIndex, buffer, endIndex );
                System.arraycopy(buffer, 0, buf, buffer.length - startIndex, endIndex);
            }
        }

        private void WriteByte(byte b) {
            buffer[endIndex] = b;
            endIndex = (endIndex + 1) & (MAX_MSG_QUEUE_SIZE - 1);
        }

        private byte ReadByte() {
            byte b = buffer[startIndex];
            startIndex = (startIndex + 1) & (MAX_MSG_QUEUE_SIZE - 1);
            return b;
        }

        private void WriteShort(int s) {
            WriteByte((byte) ((s >> 0) & 255));
            WriteByte((byte) ((s >> 8) & 255));
        }

        private int ReadShort() {
            return ReadByte() | (ReadByte() << 8);
        }

        private void WriteLong(int l) {
            WriteByte((byte) ((l >> 0) & 255));
            WriteByte((byte) ((l >> 8) & 255));
            WriteByte((byte) ((l >> 16) & 255));
            WriteByte((byte) ((l >> 24) & 255));
        }

        private int ReadLong() {
            return ReadByte() | (ReadByte() << 8) | (ReadByte() << 16) | (ReadByte() << 24);
        }

        private void WriteData(final byte[] data, final int size) {
            for (int i = 0; i < size; i++) {
                WriteByte(data[i]);
            }
        }

        private void ReadData(byte[] data, final int size) {
            if (data != null) {
                for (int i = 0; i < size; i++) {
                    data[i] = ReadByte();
                }
            } else {
                for (int i = 0; i < size; i++) {
                    ReadByte();
                }
            }
        }
    };

    static class idMsgChannel {

        private netadr_t     remoteAddress; // address of remote host
        private int          id;            // our identification used instead of port number
        private int          maxRate;       // maximum number of bytes that may go out per second
        private idCompressor compressor;    // compressor used for data compression
        //
        // variables to control the outgoing rate
        private int          lastSendTime;  // last time data was sent out
        private int          lastDataBytes; // bytes left to send at last send time
        //
        // variables to keep track of the rate
        private int          outgoingRateTime;
        private int          outgoingRateBytes;
        private int          incomingRateTime;
        private int          incomingRateBytes;
        //
        // variables to keep track of the compression ratio
        private float        outgoingCompression;
        private float        incomingCompression;
        //
        // variables to keep track of the incoming packet loss
        private float        incomingReceivedPackets;
        private float        incomingDroppedPackets;
        private int          incomingPacketLossTime;
        //
        // sequencing variables
        private int          outgoingSequence;
        private int          incomingSequence;
        //
        // outgoing fragment buffer
        private boolean      unsentFragments;
        private int          unsentFragmentStart;
        private final ByteBuffer unsentBuffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
        private idBitMsg unsentMsg;
        //
        // incoming fragment assembly buffer
        private int      fragmentSequence;
        private int      fragmentLength;
        private final ByteBuffer fragmentBuffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
        //
        // reliable messages
        private idMsgQueue reliableSend;
        private idMsgQueue reliableReceive;
        //
        //

        public idMsgChannel() {
            id = -1;
        }


        /*
         ==============
         idMsgChannel::Init

         Opens a channel to a remote system.
         ==============
         */
        public void Init(final netadr_t adr, final int id) {
            this.remoteAddress = adr;
            this.id = id;
            this.maxRate = 50000;
            this.compressor = idCompressor.AllocRunLength_ZeroBased();

            lastSendTime = 0;
            lastDataBytes = 0;
            outgoingRateTime = 0;
            outgoingRateBytes = 0;
            incomingRateTime = 0;
            incomingRateBytes = 0;
            incomingReceivedPackets = 0.0f;
            incomingDroppedPackets = 0.0f;
            incomingPacketLossTime = 0;
            outgoingCompression = 0.0f;
            incomingCompression = 0.0f;
            outgoingSequence = 1;
            incomingSequence = 0;
            unsentFragments = false;
            unsentFragmentStart = 0;
            fragmentSequence = 0;
            fragmentLength = 0;
            reliableSend.Init(1);
            reliableReceive.Init(0);
        }

        public void Shutdown() {
//	delete compressor;
            compressor = null;
        }

        public void ResetRate() {
            lastSendTime = 0;
            lastDataBytes = 0;
            outgoingRateTime = 0;
            outgoingRateBytes = 0;
            incomingRateTime = 0;
            incomingRateBytes = 0;
        }

        // Sets the maximum outgoing rate.
        public void SetMaxOutgoingRate(int rate) {
            maxRate = rate;
        }

        // Gets the maximum outgoing rate.
        public int GetMaxOutgoingRate() {
            return maxRate;
        }

        // Returns the address of the entity at the other side of the channel.
        public netadr_t GetRemoteAddress() {
            return remoteAddress;
        }

        // Returns the average outgoing rate over the last second.
        public int GetOutgoingRate() {
            return outgoingRateBytes;
        }

        // Returns the average incoming rate over the last second.
        public int GetIncomingRate() {
            return incomingRateBytes;
        }

        // Returns the average outgoing compression ratio over the last second.
        public float GetOutgoingCompression() {
            return outgoingCompression;
        }

        // Returns the average incoming compression ratio over the last second.
        public float GetIncomingCompression() {
            return incomingCompression;
        }

        // Returns the average incoming packet loss over the last 5 seconds.
        public float GetIncomingPacketLoss() {
            if (incomingReceivedPackets == 0.0f && incomingDroppedPackets == 0.0f) {
                return 0.0f;
            }
            return incomingDroppedPackets * 100.0f / (incomingReceivedPackets + incomingDroppedPackets);
        }

//
        // Returns true if the channel is ready to send new data based on the maximum rate.
        public boolean ReadyToSend(final int time) {
            int deltaTime;

            if (0 == maxRate) {
                return true;
            }
            deltaTime = time - lastSendTime;
            if (deltaTime > 1000) {
                return true;
            }
            return ((lastDataBytes - (deltaTime * maxRate) / 1000) <= 0);
        }
//

        /*
         ===============
         idMsgChannel::SendMessage

         Sends a message to a connection, fragmenting if necessary
         A 0 length will still generate a packet.
         ================
         */
        // Sends an unreliable message, in order and without duplicates.
        public int SendMessage(idPort port, final int time, final idBitMsg msg) throws idException {
            int totalLength;

            if (remoteAddress.type == NA_BAD) {
                return -1;
            }

            if (unsentFragments) {
                common.Error("idMsgChannel::SendMessage: called with unsent fragments left");
                return -1;
            }

            totalLength = 4 + reliableSend.GetTotalSize() + 4 + msg.GetSize();

            if (totalLength > MAX_MESSAGE_SIZE) {
                common.Printf("idMsgChannel::SendMessage: message too large, length = %d\n", totalLength);
                return -1;
            }

            unsentMsg.Init(unsentBuffer, unsentBuffer.capacity());
            unsentMsg.BeginWriting();

            // fragment large messages
            if (totalLength >= FRAGMENT_SIZE) {
                unsentFragments = true;
                unsentFragmentStart = 0;

                // write out the message data
                WriteMessageData(unsentMsg, msg);

                // send the first fragment now
                SendNextFragment(port, time);

                return outgoingSequence;
            }

            // write the header
            unsentMsg.WriteShort(id);
            unsentMsg.WriteLong(outgoingSequence);

            // write out the message data
            WriteMessageData(unsentMsg, msg);

            // send the packet
            port.SendPacket(remoteAddress, unsentMsg.GetData(), unsentMsg.GetSize());

            // update rate control variables
            UpdateOutgoingRate(time, unsentMsg.GetSize());

            if (net_channelShowPackets.GetBool()) {
                common.Printf("%d send %4i : s = %d ack = %d\n", id, unsentMsg.GetSize(), outgoingSequence - 1, incomingSequence);
            }

            outgoingSequence++;

            return (outgoingSequence - 1);
        }
//

        /*
         =================
         idMsgChannel::SendNextFragment

         Sends one fragment of the current message.
         =================
         */
        // Sends the next fragment if the last message was too large to send at once.
        public void SendNextFragment(idPort port, final int time) throws idException {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_PACKETLEN);
            int fragLength;

            if (remoteAddress.type == NA_BAD) {
                return;
            }

            if (!unsentFragments) {
                return;
            }

            // write the packet
            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(id);
            msg.WriteLong(outgoingSequence | FRAGMENT_BIT);

            fragLength = FRAGMENT_SIZE;
            if (unsentFragmentStart + fragLength > unsentMsg.GetSize()) {
                fragLength = unsentMsg.GetSize() - unsentFragmentStart;
            }

            msg.WriteShort(unsentFragmentStart);
            msg.WriteShort(fragLength);
            msg.WriteData(unsentMsg.GetData(), unsentFragmentStart, fragLength);

            // send the packet
            port.SendPacket(remoteAddress, msg.GetData(), msg.GetSize());

            // update rate control variables
            UpdateOutgoingRate(time, msg.GetSize());

            if (net_channelShowPackets.GetBool()) {
                common.Printf("%d send %4i : s = %d fragment = %d,%d\n", id, msg.GetSize(), outgoingSequence - 1, unsentFragmentStart, fragLength);
            }

            unsentFragmentStart += fragLength;

            // this exit condition is a little tricky, because a packet
            // that is exactly the fragment length still needs to send
            // a second packet of zero length so that the other side
            // can tell there aren't more to follow
            if (unsentFragmentStart == unsentMsg.GetSize() && fragLength != FRAGMENT_SIZE) {
                outgoingSequence++;
                unsentFragments = false;
            }
        }

        // Returns true if there are unsent fragments left.
        public boolean UnsentFragmentsLeft() {
            return unsentFragments;
        }

        /*
         =================
         idMsgChannel::Process

         Returns false if the message should not be processed due to being out of order or a fragment.

         msg must be large enough to hold MAX_MESSAGE_SIZE, because if this is the final
         fragment of a multi-part message, the entire thing will be copied out.
         =================
         */
        // Processes the incoming message. Returns true when a complete message
        // is ready for further processing. In that case the read pointer of msg
        // points to the first byte ready for reading, and sequence is set to
        // the sequence number of the message.
        public boolean Process(final netadr_t from, int time, idBitMsg msg, int[] sequence) {
            int fragStart, fragLength, dropped;
            boolean fragmented;
            idBitMsg fragMsg = new idBitMsg();

            // the IP port can't be used to differentiate them, because
            // some address translating routers periodically change UDP
            // port assignments
            if (remoteAddress.port != from.port) {
                common.Printf("idMsgChannel::Process: fixing up a translated port\n");
                remoteAddress.port = from.port;
            }

            // update incoming rate
            UpdateIncomingRate(time, msg.GetSize());

            // get sequence numbers
            sequence[0] = msg.ReadLong();

            // check for fragment information
            if ((sequence[0] & FRAGMENT_BIT) != 0) {
                sequence[0] &= ~FRAGMENT_BIT;
                fragmented = true;
            } else {
                fragmented = false;
            }

            // read the fragment information
            if (fragmented) {
                fragStart = msg.ReadShort();
                fragLength = msg.ReadShort();
            } else {
                fragStart = 0;		// stop warning message
                fragLength = 0;
            }

            if (net_channelShowPackets.GetBool()) {
                if (fragmented) {
                    common.Printf("%d recv %4i : s = %d fragment = %d,%d\n", id, msg.GetSize(), sequence[0], fragStart, fragLength);
                } else {
                    common.Printf("%d recv %4i : s = %d\n", id, msg.GetSize(), sequence[0]);
                }
            }

            //
            // discard out of order or duplicated packets
            //
            if (sequence[0] <= incomingSequence) {
                if (net_channelShowDrop.GetBool() || net_channelShowPackets.GetBool()) {
                    common.Printf("%s: out of order packet %d at %d\n", Sys_NetAdrToString(remoteAddress), sequence[0], incomingSequence);
                }
                return false;
            }

            //
            // dropped packets don't keep this message from being used
            //
            dropped = sequence[0] - (incomingSequence + 1);
            if (dropped > 0) {
                if (net_channelShowDrop.GetBool() || net_channelShowPackets.GetBool()) {
                    common.Printf("%s: dropped %d packets at %d\n", Sys_NetAdrToString(remoteAddress), dropped, sequence[0]);
                }
                UpdatePacketLoss(time, 0, dropped);
            }

            //
            // if the message is fragmented
            //
            if (fragmented) {
                // make sure we have the correct sequence number
                if (sequence[0] != fragmentSequence) {
                    fragmentSequence = sequence[0];
                    fragmentLength = 0;
                }

                // if we missed a fragment, dump the message
                if (fragStart != fragmentLength) {
                    if (net_channelShowDrop.GetBool() || net_channelShowPackets.GetBool()) {
                        common.Printf("%s: dropped a message fragment at seq %d\n", Sys_NetAdrToString(remoteAddress), sequence[0]);
                    }
                    // we can still keep the part that we have so far,
                    // so we don't need to clear fragmentLength
                    UpdatePacketLoss(time, 0, 1);
                    return false;
                }

                // copy the fragment to the fragment buffer
                if (fragLength < 0 || fragLength > msg.GetRemaingData() || fragmentLength + fragLength > fragmentBuffer.capacity()) {
                    if (net_channelShowDrop.GetBool() || net_channelShowPackets.GetBool()) {
                        common.Printf("%s: illegal fragment length\n", Sys_NetAdrToString(remoteAddress));
                    }
                    UpdatePacketLoss(time, 0, 1);
                    return false;
                }

//		memcpy( fragmentBuffer + fragmentLength, msg.GetData() + msg.GetReadCount(), fragLength );
                System.arraycopy(msg.GetData().array(), msg.GetReadCount(),
                        fragmentBuffer.array(), fragmentLength, fragLength);

                fragmentLength += fragLength;

                UpdatePacketLoss(time, 1, 0);

                // if this wasn't the last fragment, don't process anything
                if (fragLength == FRAGMENT_SIZE) {
                    return false;
                }

            } else {
//		memcpy( fragmentBuffer, msg.GetData() + msg.GetReadCount(), msg.GetRemaingData() );
                System.arraycopy(msg.GetData().array(), msg.GetReadCount(),
                        fragmentBuffer.array(), 0, msg.GetRemaingData());
                fragmentLength = msg.GetRemaingData();
                UpdatePacketLoss(time, 1, 0);
            }

            fragMsg.Init(fragmentBuffer, fragmentLength);
            fragMsg.SetSize(fragmentLength);
            fragMsg.BeginReading();

            incomingSequence = sequence[0];

            // read the message data
            if (!ReadMessageData(msg, fragMsg)) {
                return false;
            }

            return true;
        }
//
        // Sends a reliable message, in order and without duplicates.

        public boolean SendReliableMessage(final idBitMsg msg) {
            boolean result;

            assert (remoteAddress.type != NA_BAD);
            if (remoteAddress.type == NA_BAD) {
                return false;
            }
            result = reliableSend.Add(msg.GetData().array(), msg.GetSize());
            if (!result) {
                common.Warning("idMsgChannel::SendReliableMessage: overflowed");
                return false;
            }
            return result;
        }
//
        // Returns true if a new reliable message is available and stores the message.

        public boolean GetReliableMessage(idBitMsg msg) {
            int[] size = new int[1];
            boolean result;

            result = reliableReceive.Get(msg.GetData().array(), size);
            msg.SetSize(msg.GetData().capacity());//TODO:phase out size and length fields.
            msg.BeginReading();
            return result;
        }

//
        // Removes any pending outgoing or incoming reliable messages.
        public void ClearReliableMessages() {
            reliableSend.Init(1);
            reliableReceive.Init(0);
        }

        private void WriteMessageData(idBitMsg out, final idBitMsg msg) {
            idBitMsg tmp = new idBitMsg();
            ByteBuffer tmpBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            tmp.Init(tmpBuf, tmpBuf.capacity());

            // write acknowledgement of last received reliable message
            tmp.WriteLong(reliableReceive.GetLast());

            // write reliable messages
            reliableSend.CopyToBuffer(Arrays.copyOfRange(tmp.GetData().array(), tmp.GetSize(), tmp.GetData().capacity() - 1));
            tmp.SetSize(tmp.GetSize() + reliableSend.GetTotalSize());
            tmp.WriteShort(0);

            // write data
            tmp.WriteData(msg.GetData(), msg.GetSize());

            // write message size
            out.WriteShort(tmp.GetSize());

            // compress message
            idFile_BitMsg file = new idFile_BitMsg(out);
            compressor.Init(file, true, 3);
            compressor.Write(tmp.GetData(), tmp.GetSize());
            compressor.FinishCompress();
            outgoingCompression = compressor.GetCompressionRatio();
        }

        private boolean ReadMessageData(idBitMsg out, final idBitMsg msg) {
            int reliableAcknowledge, reliableSequence;
            int[] reliableMessageSize = new int[1];

            // read message size
            out.SetSize(msg.ReadShort());

            // decompress message
            idFile_BitMsg file = new idFile_BitMsg(msg);
            compressor.Init(file, false, 3);
            compressor.Read(out.GetData(), out.GetSize());
            incomingCompression = compressor.GetCompressionRatio();
            out.BeginReading();

            // read acknowledgement of sent reliable messages
            reliableAcknowledge = out.ReadLong();

            // remove acknowledged reliable messages
            while (reliableSend.GetFirst() <= reliableAcknowledge) {
                if (!reliableSend.Get(null, reliableMessageSize)) {
                    break;
                }
            }

            // read reliable messages
            reliableMessageSize[0] = out.ReadShort();
            while (reliableMessageSize[0] != 0) {
                if (reliableMessageSize[0] <= 0 || reliableMessageSize[0] > out.GetSize() - out.GetReadCount()) {
                    common.Printf("%s: bad reliable message\n", Sys_NetAdrToString(remoteAddress));
                    return false;
                }
                reliableSequence = out.ReadLong();
                if (reliableSequence == reliableReceive.GetLast() + 1) {
                    reliableReceive.Add(Arrays.copyOfRange(out.GetData().array(), out.GetReadCount(), out.GetData().capacity() - 1), reliableMessageSize[0]);
                }
                out.ReadData(null, reliableMessageSize[0]);
                reliableMessageSize[0] = out.ReadShort();
            }

            return true;
        }
//

        private void UpdateOutgoingRate(final int time, final int size) {
            // update the outgoing rate control variables
            int deltaTime = time - lastSendTime;
            if (deltaTime > 1000) {
                lastDataBytes = 0;
            } else {
                lastDataBytes -= (deltaTime * maxRate) / 1000;
                if (lastDataBytes < 0) {
                    lastDataBytes = 0;
                }
            }
            lastDataBytes += size;
            lastSendTime = time;

            // update outgoing rate variables
            if (time - outgoingRateTime > 1000) {
                outgoingRateBytes -= outgoingRateBytes * (time - outgoingRateTime - 1000) / 1000;
                if (outgoingRateBytes < 0) {
                    outgoingRateBytes = 0;
                }
            }
            outgoingRateTime = time - 1000;
            outgoingRateBytes += size;
        }

        private void UpdateIncomingRate(final int time, final int size) {
            // update incoming rate variables
            if (time - incomingRateTime > 1000) {
                incomingRateBytes -= incomingRateBytes * (time - incomingRateTime - 1000) / 1000;
                if (incomingRateBytes < 0) {
                    incomingRateBytes = 0;
                }
            }
            incomingRateTime = time - 1000;
            incomingRateBytes += size;
        }

        private void UpdatePacketLoss(final int time, final int numReceived, final int numDropped) {
            // update incoming packet loss variables
            if (time - incomingPacketLossTime > 5000) {
                float scale = (time - incomingPacketLossTime - 5000) * (1.0f / 5000.0f);
                incomingReceivedPackets -= incomingReceivedPackets * scale;
                if (incomingReceivedPackets < 0.0f) {
                    incomingReceivedPackets = 0.0f;
                }
                incomingDroppedPackets -= incomingDroppedPackets * scale;
                if (incomingDroppedPackets < 0.0f) {
                    incomingDroppedPackets = 0.0f;
                }
            }
            incomingPacketLossTime = time - 5000;
            incomingReceivedPackets += numReceived;
            incomingDroppedPackets += numDropped;
        }
    };
}
