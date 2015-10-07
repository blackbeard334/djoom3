package neo.sys;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.isNotNullOrEmpty;
import neo.idlib.Text.Str.idStr;
import neo.sys.sys_public.sysMemoryStats_s;

/**
 *
 */
public class win_shared {

    private static final long sys_timeBase = System.currentTimeMillis();
//    private static boolean initialized = false;

    /*
     ================
     Sys_Milliseconds
     ================
     */
    public static int Sys_Milliseconds() {
        int sys_curtime;

//        if (!initialized) {
//            sys_timeBase = System.currentTimeMillis();
//            initialized = true;
//        }
        sys_curtime = (int) (System.currentTimeMillis() - sys_timeBase);

        return sys_curtime;
    }

    /*
     ================
     Sys_GetSystemRam

     returns amount of physical memory in MB
     ================
     */
    public static int Sys_GetSystemRam() {
//        if (_WIN32) {
//            try {
//                final int colon, MB;
//                String memory = cmd("cmd /c \"systeminfo | find \"Total\"\"");
//                colon = memory.indexOf(':') + 1;
//                MB = memory.indexOf("MB");
//
//                memory = memory.substring(colon, MB).replace(",", "").trim();
//
//                return atoi(memory);
//            } catch (IOException ex) {
//                Logger.getLogger(win_shared.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        return -1;
        final long ram = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        int megaRam = (int) (ram / 1000000);
        
        return megaRam;
    }


    /*
     ================
     Sys_GetDriveFreeSpace
     returns in megabytes
     ================
     */
    public static int Sys_GetDriveFreeSpace(final String path) {
        throw new TODO_Exception();
//	DWORDLONG lpFreeBytesAvailable;
//	DWORDLONG lpTotalNumberOfBytes;
//	DWORDLONG lpTotalNumberOfFreeBytes;
//	int ret = 26;
//	//FIXME: see why this is failing on some machines
//	if ( ::GetDiskFreeSpaceEx( path, (PULARGE_INTEGER)&lpFreeBytesAvailable, (PULARGE_INTEGER)&lpTotalNumberOfBytes, (PULARGE_INTEGER)&lpTotalNumberOfFreeBytes ) ) {
//		ret = ( double )( lpFreeBytesAvailable ) / ( 1024.0 * 1024.0 );
//	}
//	return ret;
    }


    /*
     ================
     Sys_GetVideoRam
     returns in megabytes
     ================
     */
    public static int Sys_GetVideoRam() {
//#ifdef	ID_DEDICATED
        return 0;
//#else
//	unsigned int retSize = 64;
//
//	CComPtr<IWbemLocator> spLoc = NULL;
//	HRESULT hr = CoCreateInstance( CLSID_WbemLocator, 0, CLSCTX_SERVER, IID_IWbemLocator, ( LPVOID * ) &spLoc );
//	if ( hr != S_OK || spLoc == NULL ) {
//		return retSize;
//	}
//
//	CComBSTR bstrNamespace( _T( "\\\\.\\root\\CIMV2" ) );
//	CComPtr<IWbemServices> spServices;
//
//	// Connect to CIM
//	hr = spLoc->ConnectServer( bstrNamespace, NULL, NULL, 0, NULL, 0, 0, &spServices );
//	if ( hr != WBEM_S_NO_ERROR ) {
//		return retSize;
//	}
//
//	// Switch the security level to IMPERSONATE so that provider will grant access to system-level objects.  
//	hr = CoSetProxyBlanket( spServices, RPC_C_AUTHN_WINNT, RPC_C_AUTHZ_NONE, NULL, RPC_C_AUTHN_LEVEL_CALL, RPC_C_IMP_LEVEL_IMPERSONATE, NULL, EOAC_NONE );
//	if ( hr != S_OK ) {
//		return retSize;
//	}
//
//	// Get the vid controller
//	CComPtr<IEnumWbemClassObject> spEnumInst = NULL;
//	hr = spServices->CreateInstanceEnum( CComBSTR( "Win32_VideoController" ), WBEM_FLAG_SHALLOW, NULL, &spEnumInst ); 
//	if ( hr != WBEM_S_NO_ERROR || spEnumInst == NULL ) {
//		return retSize;
//	}
//
//	ULONG uNumOfInstances = 0;
//	CComPtr<IWbemClassObject> spInstance = NULL;
//	hr = spEnumInst->Next( 10000, 1, &spInstance, &uNumOfInstances );
//
//	if ( hr == S_OK && spInstance ) {
//		// Get properties from the object
//		CComVariant varSize;
//		hr = spInstance->Get( CComBSTR( _T( "AdapterRAM" ) ), 0, &varSize, 0, 0 );
//		if ( hr == S_OK ) {
//			retSize = varSize.intVal / ( 1024 * 1024 );
//			if ( retSize == 0 ) {
//				retSize = 64;
//			}
//		}
//	}
//	return retSize;
//#endif
    }

    /*
     ================
     Sys_GetCurrentMemoryStatus

     returns OS mem info
     all values are in kB except the memoryload
     ================
     */
    public static void Sys_GetCurrentMemoryStatus(sysMemoryStats_s stats) {
        throw new TODO_Exception();
//	MEMORYSTATUSEX statex;
//	unsigned __int64 work;
//
//	memset( &statex, sizeof( statex ), 0 );
//	statex.dwLength = sizeof( statex );
//	GlobalMemoryStatusEx( &statex );
//
//	memset( &stats, 0, sizeof( stats ) );
//
//	stats.memoryLoad = statex.dwMemoryLoad;
//
//	work = statex.ullTotalPhys >> 20;
//	stats.totalPhysical = *(int*)&work;
//
//	work = statex.ullAvailPhys >> 20;
//	stats.availPhysical = *(int*)&work;
//
//	work = statex.ullAvailPageFile >> 20;
//	stats.availPageFile = *(int*)&work;
//
//	work = statex.ullTotalPageFile >> 20;
//	stats.totalPageFile = *(int*)&work;
//
//	work = statex.ullTotalVirtual >> 20;
//	stats.totalVirtual = *(int*)&work;
//
//	work = statex.ullAvailVirtual >> 20;
//	stats.availVirtual = *(int*)&work;
//
//	work = statex.ullAvailExtendedVirtual >> 20;
//	stats.availExtendedVirtual = *(int*)&work;
    }

    /*
     ================
     Sys_LockMemory
     ================
     */
    public static boolean Sys_LockMemory(Object ptr, int bytes) {
        throw new TODO_Exception();
//	return ( VirtualLock( ptr, (SIZE_T)bytes ) != FALSE );
    }

    /*
     ================
     Sys_UnlockMemory
     ================
     */
    public static boolean Sys_UnlockMemory(Object ptr, int bytes) {
        throw new TODO_Exception();
//	return ( VirtualUnlock( ptr, (SIZE_T)bytes ) != FALSE );
    }

    /*
     ================
     Sys_SetPhysicalWorkMemory
     ================
     */
    public static void Sys_SetPhysicalWorkMemory(int minBytes, int maxBytes) {
        throw new TODO_Exception();
//	::SetProcessWorkingSetSize( GetCurrentProcess(), minBytes, maxBytes );
    }

    public static String Sys_GetCurrentUser() {

        String s_userName;
        if (!isNotNullOrEmpty(s_userName = System.getProperty("user.name"))) {
            s_userName = "player";
        }

        return s_userName;
    }


    /*
     ===============================================================================

     Call stack

     ===============================================================================
     */
//    static final int UNDECORATE_FLAGS = UNDNAME_NO_MS_KEYWORDS
//            | UNDNAME_NO_ACCESS_SPECIFIERS
//            | UNDNAME_NO_FUNCTION_RETURNS
//            | UNDNAME_NO_ALLOCATION_MODEL
//            | UNDNAME_NO_ALLOCATION_LANGUAGE
//            | UNDNAME_NO_MEMBER_TYPE;

    /*
     ==================
     Sym_Init
     ==================
     */
    public static void Sym_Init(long addr) {
    }

    /*
     ==================
     Sym_Shutdown
     ==================
     */
    public static void Sym_Shutdown() {
    }

    /*
     ==================
     Sym_GetFuncInfo
     ==================
     */
    public static void Sym_GetFuncInfo(long addr, idStr module, idStr funcName) {
        throw new TODO_Exception();
//	module = "";
//	sprintf( funcName, "0x%08x", addr );
    }


    /*
     ==================
     GetFuncAddr
     ==================
     */
    public static long/*address_t*/ GetFuncAddr(long/*address_t*/ midPtPtr) {
                throw new TODO_Exception();
//	long temp;
//	do {
//		temp = (long)(*(long*)midPtPtr);
//		if ( (temp&0x00FFFFFF) == PROLOGUE_SIGNATURE ) {
//			break;
//		}
//		midPtPtr--;
//	} while(true);
//
//	return midPtPtr;
            }

            /*
             ==================
             GetCallerAddr
             ==================
             */
            public static long/*address_t*/ GetCallerAddr(long _ebp) {
                throw new TODO_Exception();
//	long midPtPtr;
//	long res = 0;
//
//	__asm {
//		mov		eax, _ebp
//		mov		ecx, [eax]		// check for end of stack frames list
//		test	ecx, ecx		// check for zero stack frame
//		jz		label
//		mov		eax, [eax+4]	// get the ret address
//		test	eax, eax		// check for zero return address
//		jz		label
//		mov		midPtPtr, eax
//	}
//	res = GetFuncAddr( midPtPtr );
//label:
//	return res;
            }

            /*
             ==================
             Sys_GetCallStack

             use /Oy option
             ==================
             */
            public static void Sys_GetCallStack(long/*address_t*/ callStack, final int callStackSize) {
                throw new TODO_Exception();
//#if 1 //def _DEBUG
//	int i;
//	long m_ebp;
//
//	__asm {
//		mov eax, ebp
//		mov m_ebp, eax
//	}
//	// skip last two functions
//	m_ebp = *((long*)m_ebp);
//	m_ebp = *((long*)m_ebp);
//	// list functions
//	for ( i = 0; i < callStackSize; i++ ) {
//		callStack[i] = GetCallerAddr( m_ebp );
//		if ( callStack[i] == 0 ) {
//			break;
//		}
//		m_ebp = *((long*)m_ebp);
//	}
//#else
//	int i = 0;
//#endif
//	while( i < callStackSize ) {
//		callStack[i++] = 0;
//	}
            }

            /*
             ==================
             Sys_GetCallStackStr
             ==================
             */
            public static String Sys_GetCallStackStr(final long/*address_t*/ callStack, final int callStackSize) {
                throw new TODO_Exception();
//	static char string[MAX_STRING_CHARS*2];
//	int index, i;
//	idStr module, funcName;
//
//	index = 0;
//	for ( i = callStackSize-1; i >= 0; i-- ) {
//		Sym_GetFuncInfo( callStack[i], module, funcName );
//		index += sprintf( string+index, " -> %s", funcName.c_str() );
//	}
//	return string;
            }

            /*
             ==================
             Sys_GetCallStackCurStr
             ==================
             */
            public static String Sys_GetCallStackCurStr(int depth) {
                throw new TODO_Exception();
//	long/*address_t*/ *callStack;
//
//	callStack = (long/*address_t*/ *) _alloca( depth * sizeof( long/*address_t*/ ) );
//	Sys_GetCallStack( callStack, depth );
//	return Sys_GetCallStackStr( callStack, depth );
            }

            /*
             ==================
             Sys_GetCallStackCurAddressStr
             ==================
             */
            public static String Sys_GetCallStackCurAddressStr(int depth) {
                throw new TODO_Exception();
//	static char string[MAX_STRING_CHARS*2];
//	long/*address_t*/ *callStack;
//	int index, i;
//
//	callStack = (long/*address_t*/ *) _alloca( depth * sizeof( long/*address_t*/ ) );
//	Sys_GetCallStack( callStack, depth );
//
//	index = 0;
//	for ( i = depth-1; i >= 0; i-- ) {
//		index += sprintf( string+index, " -> 0x%08x", callStack[i] );
//	}
//	return string;
            }

            /*
             ==================
             Sys_ShutdownSymbols
             ==================
             */
            public static void Sys_ShutdownSymbols() {
                Sym_Shutdown();
            }

}
