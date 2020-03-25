package neo.sys;

import static neo.sys.win_net.NET_IPSocket;
import static neo.sys.win_net.NET_OpenSocks;
import static neo.sys.win_net.Net_WaitForUDPPacket;
import static neo.sys.win_net.net_ip;
import static neo.sys.win_net.net_socksEnabled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;

import neo.TempDump.CPP_class;
import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.sys.sys_local.idSysLocal;
import neo.sys.win_net.idUDPLag;

/**
 *
 */
public class sys_public {

    static final boolean WIN32 = true;

    public static idSys sys = sys_local.sysLocal;

    static {
        if (WIN32) {
            BUILD_STRING = "win-x86";
            BUILD_OS_ID = 0;
            CPUSTRING = "x86";
            CPU_EASYARGS = 1;
            PATHSEPERATOR_STR = "\\";
            PATHSEPERATOR_CHAR = '\\';
        } else {
            BUILD_STRING = "linux-x86";
            BUILD_OS_ID = 1;
            CPUSTRING = "x86";
            CPU_EASYARGS = 1;
            PATHSEPERATOR_STR = "/";
            PATHSEPERATOR_CHAR = '/';
        }
    }

    public static final String BUILD_STRING;
    public static final int    BUILD_OS_ID;
    static final        String CPUSTRING;
    static final        int    CPU_EASYARGS;
    //
    public static final String PATHSEPERATOR_STR;
    public static final char   PATHSEPERATOR_CHAR;
    //
    //
    public static final int CPUID_NONE                         = 0x00000;
    public static final int CPUID_UNSUPPORTED                  = 0x00001;// unsupported (386/486)
    public static final int CPUID_GENERIC                      = 0x00002;// unrecognized processor
    public static final int CPUID_INTEL                        = 0x00004;// Intel
    public static final int CPUID_AMD                          = 0x00008;// AMD
    public static final int CPUID_MMX                          = 0x00010;// Multi Media Extensions
    public static final int CPUID_3DNOW                        = 0x00020;// 3DNow!
    public static final int CPUID_SSE                          = 0x00040;// Streaming SIMD Extensions
    public static final int CPUID_SSE2                         = 0x00080;// Streaming SIMD Extensions 2
    public static final int CPUID_SSE3                         = 0x00100;// Streaming SIMD Extentions 3 aka Prescott's New Instructions
    public static final int CPUID_ALTIVEC                      = 0x00200;// AltiVec
    public static final int CPUID_HTT                          = 0x01000;// Hyper-Threading Technology
    public static final int CPUID_CMOV                         = 0x02000;// Conditional Move (CMOV) and fast floating point comparison (FCOMI) instructions
    public static final int CPUID_FTZ                          = 0x04000;// Flush-To-Zero mode (denormal results are flushed to zero)
    public static final int CPUID_DAZ                          = 0x08000;// Denormals-Are-Zero mode (denormal source operands are set to zero)
    //
//    
    static final        int FPU_EXCEPTION_INVALID_OPERATION    = 1;
    static final        int FPU_EXCEPTION_DENORMALIZED_OPERAND = 2;
    static final        int FPU_EXCEPTION_DIVIDE_BY_ZERO       = 4;
    static final        int FPU_EXCEPTION_NUMERIC_OVERFLOW     = 8;
    static final        int FPU_EXCEPTION_NUMERIC_UNDERFLOW    = 16;
    static final        int FPU_EXCEPTION_INEXACT_RESULT       = 32;
//                

    enum fpuPrecision_t {

        FPU_PRECISION_SINGLE,
        FPU_PRECISION_DOUBLE,
        FPU_PRECISION_DOUBLE_EXTENDED
    }

    enum fpuRounding_t {

        FPU_ROUNDING_TO_NEAREST,
        FPU_ROUNDING_DOWN,
        FPU_ROUNDING_UP,
        FPU_ROUNDING_TO_ZERO
    }

    public enum joystickAxis_t {

        AXIS_SIDE,
        AXIS_FORWARD,
        AXIS_UP,
        AXIS_ROLL,
        AXIS_YAW,
        AXIS_PITCH,
        MAX_JOYSTICK_AXIS
    }

    public enum sysEventType_t {

        SE_NONE, // evTime is still valid
        SE_KEY, // evValue is a key code, evValue2 is the down flag
        SE_CHAR, // evValue is an ascii char
        SE_MOUSE, // evValue and evValue2 are reletive signed x / y moves
        SE_JOYSTICK_AXIS, // evValue is an axis number and evValue2 is the current state (-127 to 127)
        SE_CONSOLE        // evPtr is a char*, from typing something at a non-game console
    }

    public enum sys_mEvents {

        M_ACTION1,
        M_ACTION2,
        M_ACTION3,
        M_ACTION4,
        M_ACTION5,
        M_ACTION6,
        M_ACTION7,
        M_ACTION8,
        M_DELTAX,
        M_DELTAY,
        M_DELTAZ
    }

    public static <T> T __id_attribute__(T input) {
//        DebugPrintf( final String...fmt)id_attribute((format(printf,2,3)));
        return input;
    }

    public static class sysEvent_s implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final transient int SIZE
                = CPP_class.Enum.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + CPP_class.Pointer.SIZE;
        public static final transient int BYTES = SIZE / 8;

        public sysEventType_t evType = sysEventType_t.values()[0];
        public int evValue;
        public int evValue2;
        public int evPtrLength;		// bytes of data pointed to by evPtr, for journaling
        public ByteBuffer evPtr;	// this must be manually freed if not NULL
        //TODO:is a byteBuffer necessary? we seem to always be converting it to a string.

        public sysEvent_s() {
        }

        public sysEvent_s(ByteBuffer event) {
            this.Read(event);
        }

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {

            buffer.order(ByteOrder.LITTLE_ENDIAN).rewind();
            this.evType = sysEventType_t.values()[buffer.getInt()];
            this.evValue = buffer.getInt();
            this.evValue2 = buffer.getInt();
            this.evPtrLength = buffer.getInt();
            buffer.getInt();//death to the pointer
        }

        @Override
        public ByteBuffer Write() {

            final ByteBuffer buffer = AllocBuffer();

            buffer.putInt(this.evType.ordinal());
            buffer.putInt(this.evValue);
            buffer.putInt(this.evValue2);
            buffer.putInt(this.evPtrLength);
            buffer.putInt(0x50);//P for pointer

            return buffer;
        }
    }

    static class sysMemoryStats_s {

        int memoryLoad;
        int totalPhysical;
        int availPhysical;
        int totalPageFile;
        int availPageFile;
        int totalVirtual;
        int availVirtual;
        int availExtendedVirtual;
    }

    // use fs_debug to verbose Sys_ListFiles
    // returns -1 if directory was not found (the list is cleared)
    public static int Sys_ListFiles(final String directory, final String extension, idList<idStr> list) {
        return win_main.Sys_ListFiles(directory, extension, (idStrList) list);
    }

    /*
     ==============================================================

     Networking

     ==============================================================
     */
    public static enum netadrtype_t {

        NA_BAD, // an address lookup failed
        NA_LOOPBACK,
        NA_BROADCAST,
        NA_IP
    }

    public static class netadr_t {

        public netadrtype_t type;
        public final char[] ip = new char[4];
        public short port;

        public void oSet(netadr_t address) {
            this.type = address.type;
            this.ip[0] = address.ip[0];
            this.ip[1] = address.ip[1];
            this.ip[2] = address.ip[2];
            this.ip[3] = address.ip[3];
            this.port = address.port;
        }
    }
    public static final int PORT_ANY = -1;

    static final idUDPLag[] udpPorts = new idUDPLag[65536];

    public static class idPort {

        public int packetsRead;
        public int bytesRead;
        //
        public int packetsWritten;
        public int bytesWritten;
        //
        //
        private netadr_t bound_to;	// interface and port
        private int netSocket;		// OS specific socket
        //
        //

        // this just zeros netSocket and port
        public idPort() {
            this.netSocket = 0;
            this.bound_to = new netadr_t();
        }
        // virtual		~idPort();

        // if the InitForPort fails, the idPort.port field will remain 0
        public boolean InitForPort(int portNumber) {
//            int len = sizeof(struct     sockaddr_in );

            this.netSocket = NET_IPSocket(net_ip.GetString(), portNumber, this.bound_to);
            if (this.netSocket <= 0) {
                this.netSocket = 0;
                this.bound_to = new netadr_t();// memset( &bound_to, 0, sizeof( bound_to ) );
                return false;
            }

            if (false) {
                if (net_socksEnabled.GetBool()) {
                    NET_OpenSocks(portNumber);
                }
            }

            udpPorts[this.bound_to.port] = new idUDPLag();

            return true;
        }

        public int GetPort() {
            return this.bound_to.port;
        }

        public netadr_t GetAdr() {
            return this.bound_to;
        }

        public void Close() {
            if (this.netSocket != 0) {
                if (udpPorts[this.bound_to.port] != null) {
                    udpPorts[this.bound_to.port] = null;// delete udpPorts[bound_to.port ];
                }
//                closesocket(netSocket); //TODO:
                this.netSocket = 0;
                this.bound_to = new netadr_t();// memset(bound_to, 0, sizeof(bound_to));
            }
        }

        public boolean GetPacket(netadr_t[] from, Object data, int[] size, int maxSize) {
            throw new TODO_Exception();
//            udpMsg_s msg;
//            boolean ret;
//
//            while (true) {
//
//                ret = Net_GetUDPPacket(netSocket, from[0], (char[]) data, size, maxSize);
//                if (!ret) {
//                    break;
//                }
//
//                if (net_forceDrop.GetInteger() > 0) {
//                    if (rand() < net_forceDrop.GetInteger() * RAND_MAX / 100) {
//                        continue;
//                    }
//                }
//
//                packetsRead++;
//                bytesRead += size[0];
//
//                if (net_forceLatency.GetInteger() > 0) {
//
//                    assert (size[0] <= MAX_UDP_MSG_SIZE);
//                    msg = udpPorts[ bound_to.port].udpMsgAllocator.Alloc();
//                    memcpy(msg.data, data, size[0]);
//                    msg.size = size[0];
//                    msg.address = from;
//                    msg.time = Sys_Milliseconds();
//                    msg.next = null;
//                    if (udpPorts[ bound_to.port].recieveLast) {
//                        udpPorts[ bound_to.port].recieveLast.next = msg;
//                    } else {
//                        udpPorts[ bound_to.port].recieveFirst = msg;
//                    }
//                    udpPorts[ bound_to.port].recieveLast = msg;
//                } else {
//                    break;
//                }
//            }
//
//            if (net_forceLatency.GetInteger() > 0 || (udpPorts[bound_to.port] != null && udpPorts[bound_to.port].recieveFirst != null)) {
//
//                msg = udpPorts[ bound_to.port].recieveFirst;
//                if (msg != null && msg.time <= Sys_Milliseconds() - net_forceLatency.GetInteger()) {
//                    memcpy(data, msg.data, msg.size);
//                    size[0] = msg.size;
//                    from = msg.address;
//                    udpPorts[ bound_to.port].recieveFirst = udpPorts[ bound_to.port].recieveFirst.next;
//                    if (NOT(udpPorts[ bound_to.port].recieveFirst)) {
//                        udpPorts[ bound_to.port].recieveLast = null;
//                    }
//                    udpPorts[ bound_to.port].udpMsgAllocator.Free(msg);
//                    return true;
//                }
//                return false;
//
//            } else {
//                return ret;
//            }
        }

        public boolean GetPacketBlocking(netadr_t[] from, Object data, int[] size, int maxSize, int timeout) {

            Net_WaitForUDPPacket(this.netSocket, timeout);

            if (GetPacket(from, data, size, maxSize)) {
                return true;
            }

            return false;
        }

        public void SendPacket(final netadr_t to, final Object data, int size) {
            throw new TODO_Exception();
//            udpMsg_s msg;
//
//            if (to.type == NA_BAD) {
//                common.Warning("idPort::SendPacket: bad address type NA_BAD - ignored");
//                return;
//            }
//
//            packetsWritten++;
//            bytesWritten += size;
//
//            if (net_forceDrop.GetInteger() > 0) {
//                if (rand() < net_forceDrop.GetInteger() * RAND_MAX / 100) {
//                    return;
//                }
//            }
//
//            if (net_forceLatency.GetInteger() > 0 || (udpPorts[bound_to.port] != null && udpPorts[bound_to.port].sendFirst != null)) {
//
//                assert (size <= MAX_UDP_MSG_SIZE);
//                msg = udpPorts[ bound_to.port].udpMsgAllocator.Alloc();
//                memcpy(msg.data, data, size);
//                msg.size = size;
//                msg.address = to;
//                msg.time = Sys_Milliseconds();
//                msg.next = null;
//                if (udpPorts[ bound_to.port].sendLast) {
//                    udpPorts[ bound_to.port].sendLast.next = msg;
//                } else {
//                    udpPorts[ bound_to.port].sendFirst = msg;
//                }
//                udpPorts[ bound_to.port].sendLast = msg;
//
//                for (msg = udpPorts[bound_to.port].sendFirst; msg != null && msg.time <= Sys_Milliseconds() - net_forceLatency.GetInteger(); msg = udpPorts[ bound_to.port].sendFirst) {
//                    Net_SendUDPPacket(netSocket, msg.size, msg.data, msg.address);
//                    udpPorts[ bound_to.port].sendFirst = udpPorts[ bound_to.port].sendFirst.next;
//                    if (NOT(udpPorts[bound_to.port].sendFirst)) {
//                        udpPorts[ bound_to.port].sendLast = null;
//                    }
//                    udpPorts[ bound_to.port].udpMsgAllocator.Free(msg);
//                }
//
//            } else {
//                Net_SendUDPPacket(netSocket, size, data, to);
//            }
        }
    }


    /*
     ==============================================================

     Multi-threading

     ==============================================================
     */
    public static abstract class xthread_t extends TimerTask {

        @Override
        public abstract void run();

//        public abstract int run(Object... parms);
    }

    public enum xthreadPriority {

        THREAD_NORMAL,
        THREAD_ABOVE_NORMAL,
        THREAD_HIGHEST
    }

    public static class xthreadInfo {

        public String name;
        public Thread/*int*/ threadHandle;
        public /*unsigned*/ long threadId;
    }
    static final        int           MAX_THREADS            = 10;
    //
    public static final xthreadInfo[] g_threads              = new xthreadInfo[MAX_THREADS];
    public static final int[]         g_thread_count         = {0};
    //
    static final        int           MAX_CRITICAL_SECTIONS  = 4;
    //
// enum {
    public static final int           CRITICAL_SECTION_ZERO  = 0;
    public static final int           CRITICAL_SECTION_ONE   = 1;
    public static final int           CRITICAL_SECTION_TWO   = 2;
    public static final int           CRITICAL_SECTION_THREE = 3;
    // };
    static final        int           MAX_TRIGGER_EVENTS     = 4;
    // enum {
    static final        int           TRIGGER_EVENT_ZERO     = 0;
    public static final int           TRIGGER_EVENT_ONE      = 1;
    static final        int           TRIGGER_EVENT_TWO      = 2;
    static final        int           TRIGGER_EVENT_THREE    = 3;
// };

    /*
     ==============================================================

     idSys

     ==============================================================
     */
    public static abstract class idSys {

        public abstract void DebugPrintf(final String fmt, Object... arg);

        public abstract void DebugVPrintf(final String fmt, Object... arg);

        public abstract double GetClockTicks();

        public abstract double ClockTicksPerSecond();

        public abstract /*cpuid_t*/ int GetProcessorId();

        abstract String GetProcessorString();

        public abstract String FPU_GetState();

        public abstract boolean FPU_StackIsEmpty();

        public abstract void FPU_SetFTZ(boolean enable);

        public abstract void FPU_SetDAZ(boolean enable);

        abstract void FPU_EnableExceptions(int exceptions);

        public abstract boolean LockMemory(/*void **/Object ptr, int bytes);

        public abstract boolean UnlockMemory(/*void **/Object ptr, int bytes);

        abstract void GetCallStack(long callStack, final int callStackSize);

        abstract String GetCallStackStr(final long callStack, final int callStackSize);

        public abstract String GetCallStackCurStr(int depth);

        abstract void ShutdownSymbols();

        abstract int DLL_Load(final String dllName);

        public abstract Object DLL_GetProcAddress(int dllHandle, final String procName);

        public abstract void DLL_Unload(int dllHandle);

        public abstract void DLL_GetFileName(final String baseName, String[] dllName, int maxLength);

        public abstract sysEvent_s GenerateMouseButtonEvent(int button, boolean down);

        public abstract sysEvent_s GenerateMouseMoveEvent(int deltax, int deltay);

        public abstract void OpenURL(final String url, boolean quit);

        public abstract void StartProcess(final String exePath, boolean quit);
    }

    public static void setSys(idSys sys) {
        sys_local.sysLocal = (idSysLocal) (sys_public.sys = sys);
    }
}
