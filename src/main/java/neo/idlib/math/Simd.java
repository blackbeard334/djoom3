package neo.idlib.math;

import java.nio.FloatBuffer;
import java.util.Arrays;
import neo.Renderer.Model.dominantTri_s;
import neo.TempDump.TODO_Exception;
import neo.framework.CmdSystem;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idLib;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import neo.idlib.math.Matrix.idMatX;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Simd_Generic.idSIMD_Generic;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVecX;

import static neo.TempDump.btoi;
import static neo.sys.sys_public.CPUID_GENERIC;
import static neo.sys.sys_public.CPUID_NONE;

/**
 *
 */
public class Simd {

    static idSIMDProcessor processor = null;            // pointer to SIMD processor
    static idSIMDProcessor generic   = null;            // pointer to generic SIMD implementation
    public static idSIMDProcessor SIMDProcessor;

    /*
     ===============================================================================

     Single Instruction Multiple Data (SIMD)

     For optimal use data should be aligned on a 16 byte boundary.
     All idSIMDProcessor routines are thread safe.

     ===============================================================================
     */
    public static class idSIMD {

        public static void Init() {
            generic = new idSIMD_Generic();
            generic.cpuid = CPUID_GENERIC;
            processor = null;
            SIMDProcessor = generic;
        }

        public static void InitProcessor(final String module, boolean forceGeneric) {
            /*cpuid_t*/ int cpuid;
            idSIMDProcessor newProcessor;

            cpuid = idLib.sys.GetProcessorId();

            newProcessor = generic;//TODO:add useSSE to startup sequence.
//            if (forceGeneric) {
//
//                newProcessor = generic;
//
//            } else {
//
//                if (processor != null) {
//                    if ((cpuid & CPUID_ALTIVEC) != 0) {
//                        processor = new idSIMD_AltiVec();
//                    } else if (((cpuid & CPUID_MMX) & (cpuid & CPUID_SSE) & (cpuid & CPUID_SSE2) & (cpuid & CPUID_SSE3))
//                            != 0) {
//                        processor = new idSIMD_SSE3();
//                    } else if (((cpuid & CPUID_MMX) & (cpuid & CPUID_SSE) & (cpuid & CPUID_SSE2)) != 0) {
//                        processor = new idSIMD_SSE2();
//                    } else if (((cpuid & CPUID_MMX) & (cpuid & CPUID_SSE)) != 0) {
//                        processor = new idSIMD_SSE();
//                    } else if (((cpuid & CPUID_MMX) & (cpuid & CPUID_3DNOW)) != 0) {
//                        processor = new idSIMD_3DNow();
//                    } else
//                    if ((cpuid & CPUID_MMX) != 0) {
//                        processor = new idSIMD_MMX();
//                    } else {
//                        processor = generic;
//                    }
//                    processor.cpuid = cpuid;
//                }
//                newProcessor = processor;
//            }
            if (!newProcessor.equals(SIMDProcessor)) {
                SIMDProcessor = newProcessor;
                idLib.common.Printf("%s using %s for SIMD processing\n", module, SIMDProcessor.GetName());
            }
//            if ((cpuid & CPUID_FTZ) != 0) {
//                idLib.sys.FPU_SetFTZ(true);
//                idLib.common.Printf("enabled Flush-To-Zero mode\n");
//            }
//
//            if ((cpuid & CPUID_DAZ) != 0) {
//                idLib.sys.FPU_SetDAZ(true);
//                idLib.common.Printf("enabled Denormals-Are-Zero mode\n");
//            }
        }

        public static void Shutdown() {
            if (processor != generic) {
//		delete processor;
            }
//	delete generic;
            generic = processor = SIMDProcessor = null;
        }

        /**
         * @deprecated we don't really have simd like this in java, so why
         * pretend?
         */
        @Deprecated
        public static class Test_f extends cmdFunction_t {

            private static final CmdSystem.cmdFunction_t instance = new Test_f();

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
//
//                if (_WIN32) {
////                    SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_TIME_CRITICAL);
//                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//                } /* _WIN32 */
//
//                p_simd = processor;
//                p_generic = generic;
//
//                if (isNotNullOrEmpty(args.Argv(1))) {
//                    int cpuid_t = idLib.sys.GetProcessorId();
//                    idStr argString = new idStr(args.Args());
//
//                    argString.Replace(" ", "");
//
//                    if (idStr.Icmp(argString, "MMX") == 0) {
//                        if (0 == (cpuid_t & CPUID_MMX)) {
//                            common.Printf("CPU does not support MMX\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_MMX();
//                    } else if (idStr.Icmp(argString, "3DNow") == 0) {
//                        if (0 == (cpuid_t & CPUID_MMX) || 0 == (cpuid_t & CPUID_3DNOW)) {
//                            common.Printf("CPU does not support MMX & 3DNow\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_3DNow();
//                    } else if (idStr.Icmp(argString, "SSE") == 0) {
//                        if (0 == (cpuid_t & CPUID_MMX) || 0 == (cpuid_t & CPUID_SSE)) {
//                            common.Printf("CPU does not support MMX & SSE\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_SSE();
//                    } else if (idStr.Icmp(argString, "SSE2") == 0) {
//                        if (0 == (cpuid_t & CPUID_MMX) || 0 == (cpuid_t & CPUID_SSE) || 0 == (cpuid_t & CPUID_SSE2)) {
//                            common.Printf("CPU does not support MMX & SSE & SSE2\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_SSE2();
//                    } else if (idStr.Icmp(argString, "SSE3") == 0) {
//                        if (0 == (cpuid_t & CPUID_MMX) || 0 == (cpuid_t & CPUID_SSE) || 0 == (cpuid_t & CPUID_SSE2) || 0 == (cpuid_t & CPUID_SSE3)) {
//                            common.Printf("CPU does not support MMX & SSE & SSE2 & SSE3\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_SSE3();
//                    } else if (idStr.Icmp(argString, "AltiVec") == 0) {
//                        if (0 == (cpuid_t & CPUID_ALTIVEC)) {
//                            common.Printf("CPU does not support AltiVec\n");
//                            return;
//                        }
//                        p_simd = new idSIMD_AltiVec();
//                    } else {
//                        common.Printf("invalid argument, use: MMX, 3DNow, SSE, SSE2, SSE3, AltiVec\n");
//                        return;
//                    }
//                }
//
//                idLib.common.SetRefreshOnPrint(true);
//
//                idLib.common.Printf("using %s for SIMD processing\n", p_simd.GetName());
//
//                GetBaseClocks();
//
//                TestMath();
//                TestAdd();
//                TestSub();
//                TestMul();
//                TestDiv();
//                TestMulAdd();
//                TestMulSub();
//                TestDot();
//                TestCompare();
//                TestMinMax();
//                TestClamp();
//                TestMemcpy();
//                TestMemset();
//                TestNegate();
//
//                TestMatXMultiplyVecX();
//                TestMatXMultiplyAddVecX();
//                TestMatXTransposeMultiplyVecX();
//                TestMatXTransposeMultiplyAddVecX();
//                TestMatXMultiplyMatX();
//                TestMatXTransposeMultiplyMatX();
//                TestMatXLowerTriangularSolve();
//                TestMatXLowerTriangularSolveTranspose();
//                TestMatXLDLTFactor();
//
//                idLib.common.Printf("====================================\n");
//
//                TestBlendJoints();
//                TestConvertJointQuatsToJointMats();
//                TestConvertJointMatsToJointQuats();
//                TestTransformJoints();
//                TestUntransformJoints();
//                TestTransformVerts();
//                TestTracePointCull();
//                TestDecalPointCull();
//                TestOverlayPointCull();
//                TestDeriveTriPlanes();
//                TestDeriveTangents();
//                TestDeriveUnsmoothedTangents();
//                TestNormalizeTangents();
//                TestGetTextureSpaceLightVectors();
//                TestGetSpecularTextureCoords();
//                TestCreateShadowCache();
//
//                idLib.common.Printf("====================================\n");
//
//                TestSoundUpSampling();
//                TestSoundMixing();
//
//                idLib.common.SetRefreshOnPrint(false);
//
//                if (!p_simd.equals(processor)) {
////                    delete p_simd;
//                }
//                p_simd = null;
//                p_generic = null;
//
//                if (_WIN32) {
//                    SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_NORMAL);
//                }/* _WIN32 */

            }
        };
    };
//    
    static idSIMDProcessor p_simd;
    static idSIMDProcessor p_generic;
    static long baseClocks = 0;
//
//
    public static final int MIXBUFFER_SAMPLES = 4096;

    public enum speakerLabel {

        SPEAKER_LEFT,
        SPEAKER_RIGHT,
        SPEAKER_CENTER,
        SPEAKER_LFE,
        SPEAKER_BACKLEFT,
        SPEAKER_BACKRIGHT
    };

    public static abstract class idSIMDProcessor {

        public idSIMDProcessor() {
            cpuid = CPUID_NONE;
        }
//
        public /*cpuid_t*/ int cpuid;
//

        public abstract String/*char *VPCALL*/ GetName();
//

        public abstract void /*VPCALL*/ Add(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ Add(float[] dst, final float[] src0, final float[] src1, final int count);

        public abstract void /*VPCALL*/ Sub(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ Sub(float[] dst, final float[] src0, final float[] src1, final int count);

        public abstract void /*VPCALL*/ Mul(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ Mul(float[] dst, final float[] src0, final float[] src1, final int count);

        public abstract void /*VPCALL*/ Div(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ Div(float[] dst, final float[] src0, final float[] src1, final int count);

        public abstract void /*VPCALL*/ MulAdd(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ MulAdd(float[] dst, final float[] src0, final float[] src1, final int count);

        public abstract void /*VPCALL*/ MulSub(float[] dst, final float constant, final float[] src, final int count);

        public abstract void /*VPCALL*/ MulSub(float[] dst, final float[] src0, final float[] src1, final int count);
//

        public abstract void /*VPCALL*/ Dot(float[] dst, final idVec3 constant, final idVec3[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idVec3 constant, final idPlane[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idVec3 constant, final idDrawVert[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idPlane constant, final idVec3[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idPlane constant, final idPlane[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idPlane constant, final idDrawVert[] src, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dst, final idVec3[] src0, final idVec3[] src1, final int count);

        public abstract void /*VPCALL*/ Dot(float[] dot, final float[] src1, final float[] src2, final int count);
//

        public abstract void /*VPCALL*/ CmpGT(/*byte*/boolean[] dst, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpGT(byte[] dst, final byte bitNum, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpGE(/*byte*/boolean[] dst, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpGE(byte[] dst, final byte bitNum, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpLT(/*byte*/boolean[] dst, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpLT(byte[] dst, final byte bitNum, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpLE(/*byte*/boolean[] dst, final float[] src0, final float constant, final int count);

        public abstract void /*VPCALL*/ CmpLE(byte[] dst, final byte bitNum, final float[] src0, final float constant, final int count);
//

        public abstract void /*VPCALL*/ MinMax(float[] min, float[] max, final float[] src, final int count);

        public abstract void /*VPCALL*/ MinMax(idVec2 min, idVec2 max, final idVec2[] src, final int count);

        public abstract void /*VPCALL*/ MinMax(idVec3 min, idVec3 max, final idVec3[] src, final int count);

        public abstract void /*VPCALL*/ MinMax(idVec3 min, idVec3 max, final idDrawVert[] src, final int count);

        public abstract void /*VPCALL*/ MinMax(idVec3 min, idVec3 max, final idDrawVert[] src, final int[] indexes, final int count);
//

        public abstract void /*VPCALL*/ Clamp(float[] dst, final float[] src, final float min, final float max, final int count);

        public abstract void /*VPCALL*/ ClampMin(float[] dst, final float[] src, final float min, final int count);

        public abstract void /*VPCALL*/ ClampMax(float[] dst, final float[] src, final float max, final int count);
//

        public abstract void /*VPCALL*/ Memcpy(Object[] dst, final Object[] src, final int count);

        public void /*VPCALL*/ Memcpy(Object dst, final Object src, final int count) {
            throw new TODO_Exception();
        }

        @Deprecated
        public abstract void /*VPCALL*/ Memset(Object[] dst, final int val, final int count);
//
//	// these assume 16 byte aligned and 16 byte padded memory

        public abstract void /*VPCALL*/ Zero16(float[] dst, final int count);

        public abstract void /*VPCALL*/ Negate16(float[] dst, final int count);

        public abstract void /*VPCALL*/ Copy16(float[] dst, final float[] src, final int count);

        public abstract void /*VPCALL*/ Add16(float[] dst, final float[] src1, final float[] src2, final int count);

        public abstract void /*VPCALL*/ Sub16(float[] dst, final float[] src1, final float[] src2, final int count);

        public abstract void /*VPCALL*/ Mul16(float[] dst, final float[] src1, final float constant, final int count);

        public abstract void /*VPCALL*/ AddAssign16(float[] dst, final float[] src, final int count);

        public abstract void /*VPCALL*/ SubAssign16(float[] dst, final float[] src, final int count);

        public abstract void /*VPCALL*/ MulAssign16(float[] dst, final float constant, final int count);
//
//	// idMatX operations

        public abstract void /*VPCALL*/ MatX_MultiplyVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_MultiplyAddVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_MultiplySubVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_TransposeMultiplyVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_TransposeMultiplyAddVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_TransposeMultiplySubVecX(idVecX dst, final idMatX mat, final idVecX vec);

        public abstract void /*VPCALL*/ MatX_MultiplyMatX(idMatX dst, final idMatX m1, final idMatX m2);

        public abstract void /*VPCALL*/ MatX_TransposeMultiplyMatX(idMatX dst, final idMatX m1, final idMatX m2);

        public abstract void /*VPCALL*/ MatX_LowerTriangularSolve(final idMatX L, float[] x, final float[] b, final int n/*, int skip = 0*/);

        public abstract void /*VPCALL*/ MatX_LowerTriangularSolve(final idMatX L, float[] x, final float[] b, final int n, int skip);

        public abstract void /*VPCALL*/ MatX_LowerTriangularSolveTranspose(final idMatX L, float[] x, final float[] b, final int n);

        public abstract boolean /*VPCALL*/ MatX_LDLTFactor(idMatX mat, idVecX invDiag, final int n);
//
//	// rendering

        public abstract void /*VPCALL*/ BlendJoints(idJointQuat[] joints, final idJointQuat[] blendJoints, final float lerp, final int[] index, final int numJoints);

        public abstract void /*VPCALL*/ ConvertJointQuatsToJointMats(idJointMat[] jointMats, final idJointQuat[] jointQuats, final int numJoints);

        public abstract void /*VPCALL*/ ConvertJointMatsToJointQuats(idList<idJointQuat> jointQuats, final idJointMat[] jointMats, final int numJoints);

        public abstract void /*VPCALL*/ TransformJoints(idJointMat[] jointMats, final int[] parents, final int firstJoint, final int lastJoint);

        public abstract void /*VPCALL*/ UntransformJoints(idJointMat[] jointMats, final int[] parents, final int firstJoint, final int lastJoint);

        public abstract void /*VPCALL*/ TransformVerts(idDrawVert[] verts, final int numVerts, final idJointMat[] joints, final idVec4[] weights, final int[] index, final int numWeights);

        public abstract void /*VPCALL*/ TracePointCull(byte[] cullBits, byte[] totalOr, final float radius, final idPlane[] planes, final idDrawVert[] verts, final int numVerts);

        public abstract void /*VPCALL*/ DecalPointCull(byte[] cullBits, final idPlane[] planes, final idDrawVert[] verts, final int numVerts);

        public abstract void /*VPCALL*/ OverlayPointCull(byte[] cullBits, idVec2[] texCoords, final idPlane[] planes, final idDrawVert[] verts, final int numVerts);

        public abstract void /*VPCALL*/ DeriveTriPlanes(idPlane[] planes, final idDrawVert[] verts, final int numVerts, final int[] indexes, final int numIndexes);

        public abstract void /*VPCALL*/ DeriveTangents(idPlane[] planes, idDrawVert[] verts, final int numVerts, final int[] indexes, final int numIndexes);

        public abstract void /*VPCALL*/ DeriveUnsmoothedTangents(idDrawVert[] verts, final dominantTri_s[] dominantTris, final int numVerts);

        public abstract void /*VPCALL*/ NormalizeTangents(idDrawVert[] verts, final int numVerts);

        public abstract void /*VPCALL*/ CreateTextureSpaceLightVectors(idVec3[] lightVectors, final idVec3 lightOrigin, final idDrawVert[] verts, final int numVerts, final int[] indexes, final int numIndexes);

        public void CreateTextureSpaceLightVectors(idVec3 localLightVector, idVec3 localLightOrigin, idDrawVert[] verts, int numVerts, int[] indexes, int numIndexes) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public abstract void /*VPCALL*/ CreateSpecularTextureCoords(idVec4[] texCoords, final idVec3 lightOrigin, final idVec3 viewOrigin, final idDrawVert[] verts, final int numVerts, final int[] indexes, final int numIndexes);

        public abstract int /*VPCALL*/ CreateShadowCache(idVec4[] vertexCache, int[] vertRemap, final idVec3 lightOrigin, final idDrawVert[] verts, final int numVerts);

        public abstract int /*VPCALL*/ CreateVertexProgramShadowCache(idVec4[] vertexCache, final idDrawVert[] verts, final int numVerts);

        public void CreateVertexProgramShadowCache(idVec4 vertexCache, final idDrawVert[] verts, final int numVerts) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
//
//	// sound mixing

        public abstract void /*VPCALL*/ UpSamplePCMTo44kHz(float[] dest, final short[] pcm, final int numSamples, final int kHz, final int numChannels);

        public void /*VPCALL*/ UpSampleOGGTo44kHz(float[] dest, final float[][] ogg, final int numSamples, final int kHz, final int numChannels) {
            this.UpSampleOGGTo44kHz(dest, 0, ogg, numSamples, kHz, numChannels);
        }
        
        public abstract void /*VPCALL*/ UpSampleOGGTo44kHz(float[] dest, int offset,  final float[][] ogg, final int numSamples, final int kHz, final int numChannels);
        
        public abstract void /*VPCALL*/ UpSampleOGGTo44kHz(FloatBuffer dest, int offset,  final float[][] ogg, final int numSamples, final int kHz, final int numChannels);

        public abstract void /*VPCALL*/ MixSoundTwoSpeakerMono(float[] mixBuffer, final float[] samples, final int numSamples, final float lastV[], final float currentV[]);

        public abstract void /*VPCALL*/ MixSoundTwoSpeakerStereo(float[] mixBuffer, final float[] samples, final int numSamples, final float lastV[], final float currentV[]);

        public abstract void /*VPCALL*/ MixSoundSixSpeakerMono(float[] mixBuffer, final float[] samples, final int numSamples, final float lastV[], final float currentV[]);

        public abstract void /*VPCALL*/ MixSoundSixSpeakerStereo(float[] mixBuffer, final float[] samples, final int numSamples, final float lastV[], final float currentV[]);

        public abstract void /*VPCALL*/ MixedSoundToSamples(short[] samples, int offset, final float[] mixBuffer, final int numSamples);

        public void /*VPCALL*/ MixedSoundToSamples(short[] samples, final float[] mixBuffer, final int numSamples) {
            MixedSoundToSamples(samples, 0, mixBuffer, numSamples);
        }

        public void Memset(byte[] cullBits, int i, int numVerts) {
            Arrays.fill(cullBits, 0, numVerts, (byte) i);
        }

        public void Memset(int[] cullBits, int i, int numVerts) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void CmpGE(byte[] facing, float[] planeSide, float f, int numFaces) {
            int i, nm = numFaces & 0xfffffffc;
            for (i = 0; i < nm; i += 4) {
                facing[i + 0] = (byte) btoi(planeSide[i + 0] > f);
                facing[i + 1] = (byte) btoi(planeSide[i + 1] > f);
                facing[i + 2] = (byte) btoi(planeSide[i + 2] > f);
                facing[i + 3] = (byte) btoi(planeSide[i + 3] > f);
            }
            for (; i < numFaces; i++) {
                facing[i + 0] = (byte) btoi(planeSide[i + 0] > f);
            }
        }

        public void Memcpy(int[] indexes, int[] tempIndexes, int numShadowIndexes) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void Memcpy(int[] indexes, int indexOffset, int[] tempIndexes, int tempOffset, int numShadowIndexes) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    };
    //TODO:add tests
}
