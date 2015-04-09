package neo.idlib.math;

import java.nio.FloatBuffer;
import java.util.Arrays;
import neo.Renderer.Model.dominantTri_s;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMatX;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Simd.idSIMDProcessor;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVecX;

/**
 *
 */
public class Simd_Generic {
//     UNROLL1(Y) { int _IX; for (_IX=0;_IX<count;_IX++) {Y_IX;} }
//#define UNROLL2(Y) { int _IX, _NM = count&0xfffffffe; for (_IX=0;_IX<_NM;_IX+=2){Y(_IX+0);Y(_IX+1);} if (_IX < count) {Y_IX;}}

//    static void UNROLL4(float[] dst, float constant, float[] src, int count) {
//        int _IX, _NM = count & 0xfffffffc;
//        for (_IX = 0; _IX < _NM; _IX += 4) {
//            dst[_IX + 0] = src[_IX + 0] + constant;
//            dst[_IX + 1] = src[_IX + 1] + constant;
//            dst[_IX + 2] = src[_IX + 2] + constant;
//            dst[_IX + 3] = src[_IX + 3] + constant;
//        }
//        for (; _IX < count; _IX++) {
//            dst[_IX] = src[_IX] + constant;
//        }
//    }
//#define UNROLL8(Y) { int _IX, _NM = count&0xfffffff8; for (_IX=0;_IX<_NM;_IX+=8){Y(_IX+0);Y(_IX+1);Y(_IX+2);Y(_IX+3);Y(_IX+4);Y(_IX+5);Y(_IX+6);Y(_IX+7);} _NM = count&0xfffffffe; for(;_IX<_NM;_IX+=2){Y_IX; Y(_IX+1);} if (_IX < count) {Y_IX;} }
    final static int MIXBUFFER_SAMPLES = 4096;
    /*
     ===============================================================================

     Generic implementation of idSIMDProcessor

     ===============================================================================
     */

    static class idSIMD_Generic extends idSIMDProcessor {

        @Override
        public String GetName() {
            return "generic code";
        }

        /*
         ============
         idSIMD_Generic::Add

         dst[i] = constant + src[i];
         ============
         */
        @Override
        public void Add(float[] dst, float constant, float[] src, int count) {
//#define OPER(X) dst[(X)] = src[(X)] + constant;
//	UNROLL4(OPER)
//#undef OPER
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src[_IX + 0] + constant;
                dst[_IX + 1] = src[_IX + 1] + constant;
                dst[_IX + 2] = src[_IX + 2] + constant;
                dst[_IX + 3] = src[_IX + 3] + constant;
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src[_IX] + constant;
            }
        }

        /*
         ============
         idSIMD_Generic::Add

         dst[i] = src0[i] + src1[i];
         ============
         */
        @Override
        public void Add(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] + src1[_IX + 0];
                dst[_IX + 1] = src0[_IX + 1] + src1[_IX + 1];
                dst[_IX + 2] = src0[_IX + 2] + src1[_IX + 2];
                dst[_IX + 3] = src0[_IX + 3] + src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] + src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Sub

         dst[i] = constant - src[i];
         ============
         */
        @Override
        public void Sub(float[] dst, float constant, float[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant - src[_IX + 0];
                dst[_IX + 1] = constant - src[_IX + 1];
                dst[_IX + 2] = constant - src[_IX + 2];
                dst[_IX + 3] = constant - src[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = constant - src[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Sub

         dst[i] = src0[i] - src1[i];
         ============
         */
        @Override
        public void Sub(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] - src1[_IX + 0];
                dst[_IX + 1] = src0[_IX + 1] - src1[_IX + 1];
                dst[_IX + 2] = src0[_IX + 2] - src1[_IX + 2];
                dst[_IX + 3] = src0[_IX + 3] - src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] - src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Mul

         dst[i] = constant * src[i];
         ============
         */
        @Override
        public void Mul(float[] dst, float constant, float[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant * src[_IX + 0];
                dst[_IX + 1] = constant * src[_IX + 1];
                dst[_IX + 2] = constant * src[_IX + 2];
                dst[_IX + 3] = constant * src[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = constant * src[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Mul

         dst[i] = src0[i] * src1[i];
         ============
         */
        @Override
        public void Mul(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] * src1[_IX + 0];
                dst[_IX + 1] = src0[_IX + 1] * src1[_IX + 1];
                dst[_IX + 2] = src0[_IX + 2] * src1[_IX + 2];
                dst[_IX + 3] = src0[_IX + 3] * src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] * src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Div

         dst[i] = constant / divisor[i];
         ============
         */
        @Override
        public void Div(float[] dst, float constant, float[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant / src[_IX + 0];
                dst[_IX + 1] = constant / src[_IX + 1];
                dst[_IX + 2] = constant / src[_IX + 2];
                dst[_IX + 3] = constant / src[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = constant / src[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Div

         dst[i] = src0[i] / src1[i];
         ============
         */
        @Override
        public void Div(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] / src1[_IX + 0];
                dst[_IX + 1] = src0[_IX + 1] / src1[_IX + 1];
                dst[_IX + 2] = src0[_IX + 2] / src1[_IX + 2];
                dst[_IX + 3] = src0[_IX + 3] / src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] / src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::MulAdd

         dst[i] += constant * src[i];
         ============
         */
        @Override
        public void MulAdd(float[] dst, float constant, float[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] += constant * src[_IX + 0];
                dst[_IX + 1] += constant * src[_IX + 1];
                dst[_IX + 2] += constant * src[_IX + 2];
                dst[_IX + 3] += constant * src[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] += constant * src[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::MulAdd

         dst[i] += src0[i] * src1[i];
         ============
         */
        @Override
        public void MulAdd(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] += src0[_IX + 0] * src1[_IX + 0];
                dst[_IX + 1] += src0[_IX + 1] * src1[_IX + 1];
                dst[_IX + 2] += src0[_IX + 2] * src1[_IX + 2];
                dst[_IX + 3] += src0[_IX + 3] * src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] += src0[_IX] * src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::MulSub

         dst[i] -= constant * src[i];
         ============
         */
        @Override
        public void MulSub(float[] dst, float constant, float[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] -= constant * src[_IX + 0];
                dst[_IX + 1] -= constant * src[_IX + 1];
                dst[_IX + 2] -= constant * src[_IX + 2];
                dst[_IX + 3] -= constant * src[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] -= constant * src[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::MulSub

         dst[i] -= src0[i] * src1[i];
         ============
         */
        @Override
        public void MulSub(float[] dst, float[] src0, float[] src1, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] -= src0[_IX + 0] * src1[_IX + 0];
                dst[_IX + 1] -= src0[_IX + 1] * src1[_IX + 1];
                dst[_IX + 2] -= src0[_IX + 2] * src1[_IX + 2];
                dst[_IX + 3] -= src0[_IX + 3] * src1[_IX + 3];
            }
            for (; _IX < count; _IX++) {
                dst[_IX] -= src0[_IX] * src1[_IX];
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant * src[i];
         ============
         */
        @Override
        public void Dot(float[] dst, idVec3 constant, idVec3[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src[_IX + 0].oMultiply(constant);
                dst[_IX + 1] = src[_IX + 1].oMultiply(constant);
                dst[_IX + 2] = src[_IX + 2].oMultiply(constant);
                dst[_IX + 3] = src[_IX + 3].oMultiply(constant);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src[_IX].oMultiply(constant);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant * src[i].Normal() + src[i][3];
         ============
         */
        @Override
        public void Dot(float[] dst, idVec3 constant, idPlane[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src[_IX + 0].Normal().oPlus(src[_IX + 0].oGet(3)).oMultiply(constant);
                dst[_IX + 1] = src[_IX + 1].Normal().oPlus(src[_IX + 1].oGet(3)).oMultiply(constant);
                dst[_IX + 2] = src[_IX + 2].Normal().oPlus(src[_IX + 2].oGet(3)).oMultiply(constant);
                dst[_IX + 3] = src[_IX + 3].Normal().oPlus(src[_IX + 3].oGet(3)).oMultiply(constant);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src[_IX].Normal().oPlus(src[_IX].oGet(3)).oMultiply(constant);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant * src[i].xyz;
         ============
         */
        @Override
        public void Dot(float[] dst, idVec3 constant, idDrawVert[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src[_IX + 0].xyz.oMultiply(constant);
                dst[_IX + 0] = src[_IX + 1].xyz.oMultiply(constant);
                dst[_IX + 0] = src[_IX + 2].xyz.oMultiply(constant);
                dst[_IX + 0] = src[_IX + 3].xyz.oMultiply(constant);
            }
            for (; _IX < count; _IX++) {
                dst[_IX + 0] = src[_IX].xyz.oMultiply(constant);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant.Normal() * src[i] + constant[3];
         ============
         */
        @Override
        public void Dot(float[] dst, idPlane constant, idVec3[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 0]) + constant.oGet(3);
                dst[_IX + 1] = constant.Normal().oMultiply(src[_IX + 1]) + constant.oGet(3);
                dst[_IX + 2] = constant.Normal().oMultiply(src[_IX + 2]) + constant.oGet(3);
                dst[_IX + 3] = constant.Normal().oMultiply(src[_IX + 3]) + constant.oGet(3);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = constant.Normal().oMultiply(src[_IX]) + constant.oGet(3);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant.Normal() * src[i].Normal() + constant[3] * src[i][3];
         ============
         */
        @Override
        public void Dot(float[] dst, idPlane constant, idPlane[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 0].Normal()) + src[_IX + 0].oGet(3) * constant.oGet(3);
                dst[_IX + 1] = constant.Normal().oMultiply(src[_IX + 1].Normal()) + src[_IX + 1].oGet(3) * constant.oGet(3);
                dst[_IX + 2] = constant.Normal().oMultiply(src[_IX + 2].Normal()) + src[_IX + 2].oGet(3) * constant.oGet(3);
                dst[_IX + 3] = constant.Normal().oMultiply(src[_IX + 3].Normal()) + src[_IX + 3].oGet(3) * constant.oGet(3);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = constant.Normal().oMultiply(src[_IX].Normal()) + src[_IX].oGet(3) * constant.oGet(3);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = constant.Normal() * src[i].xyz + constant[3];
         ============
         */
        @Override
        public void Dot(float[] dst, idPlane constant, idDrawVert[] src, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 0].xyz) * constant.oGet(3);
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 1].xyz) * constant.oGet(3);
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 2].xyz) * constant.oGet(3);
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX + 3].xyz) * constant.oGet(3);
            }
            for (; _IX < count; _IX++) {
                dst[_IX + 0] = constant.Normal().oMultiply(src[_IX].xyz) * constant.oGet(3);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dst[i] = src0[i] * src1[i];
         ============
         */
        @Override
        public void Dot(float[] dst, idVec3[] src0, idVec3[] src1, int count) {
            int _IX, _NM = count & ~3;//TODO:check chekc checks
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0].oMultiply(src1[_IX + 0]);
                dst[_IX + 1] = src0[_IX + 1].oMultiply(src1[_IX + 1]);
                dst[_IX + 2] = src0[_IX + 2].oMultiply(src1[_IX + 2]);
                dst[_IX + 3] = src0[_IX + 3].oMultiply(src1[_IX + 3]);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX].oMultiply(src1[_IX]);
            }
        }

        /*
         ============
         idSIMD_Generic::Dot

         dot = src1[0] * src2[0] + src1[1] * src2[1] + src1[2] * src2[2] + ...
         ============
         */
        @Override
        public void Dot(float[] dot, float[] src1, float[] src2, int count) {
//#if 1
//
            switch (count) {
                case 0: {
                    dot[0] = 0.0f;
                    return;
                }
                case 1: {
                    dot[0] = src1[0] * src2[0];
                    return;
                }
                case 2: {
                    dot[0] = src1[0] * src2[0] + src1[1] * src2[1];
                    return;
                }
                case 3: {
                    dot[0] = src1[0] * src2[0] + src1[1] * src2[1] + src1[2] * src2[2];
                    return;
                }
                default: {
                    int i;
                    double s0, s1, s2, s3;
                    s0 = src1[0] * src2[0];
                    s1 = src1[1] * src2[1];
                    s2 = src1[2] * src2[2];
                    s3 = src1[3] * src2[3];
                    for (i = 4; i < count - 7; i += 8) {
                        s0 += src1[i + 0] * src2[i + 0];
                        s1 += src1[i + 1] * src2[i + 1];
                        s2 += src1[i + 2] * src2[i + 2];
                        s3 += src1[i + 3] * src2[i + 3];
                        s0 += src1[i + 4] * src2[i + 4];
                        s1 += src1[i + 5] * src2[i + 5];
                        s2 += src1[i + 6] * src2[i + 6];
                        s3 += src1[i + 7] * src2[i + 7];
                    }
                    switch (count - i) {
//				NODEFAULT;
                        case 7:
                            s0 += src1[i + 6] * src2[i + 6];
                        case 6:
                            s1 += src1[i + 5] * src2[i + 5];
                        case 5:
                            s2 += src1[i + 4] * src2[i + 4];
                        case 4:
                            s3 += src1[i + 3] * src2[i + 3];
                        case 3:
                            s0 += src1[i + 2] * src2[i + 2];
                        case 2:
                            s1 += src1[i + 1] * src2[i + 1];
                        case 1:
                            s2 += src1[i + 0] * src2[i + 0];
                        case 0:
                            break;
                    }
                    double sum;
                    sum = s3;
                    sum += s2;
                    sum += s1;
                    sum += s0;
                    dot[0] = (float) sum;
                }
            }
//
//#else
//
//            dot = 0.0f;
//            for ( i = 0; i < count; i++) {
//                dot += src1[i] * src2[i];
//            }
//
//#endif
        }

        /*
         ============
         idSIMD_Generic::CmpGT

         dst[i] = src0[i] > constant;
         ============
         */
        @Override
        public void CmpGT(boolean[] dst, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] > constant;
                dst[_IX + 1] = src0[_IX + 1] > constant;
                dst[_IX + 2] = src0[_IX + 2] > constant;
                dst[_IX + 3] = src0[_IX + 3] > constant;
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] > constant;
            }
        }

        /*
         ============
         idSIMD_Generic::CmpGT

         dst[i] |= ( src0[i] > constant ) << bitNum;
         ============
         */
        @Override
        public void CmpGT(byte[] dst, byte bitNum, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            final byte _bitNum = (byte) (1 << bitNum);//TODO:check byte signage
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = (byte) (src0[_IX + 0] > constant ? _bitNum : 0);
                dst[_IX + 1] = (byte) (src0[_IX + 1] > constant ? _bitNum : 0);
                dst[_IX + 2] = (byte) (src0[_IX + 2] > constant ? _bitNum : 0);
                dst[_IX + 3] = (byte) (src0[_IX + 3] > constant ? _bitNum : 0);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = (byte) (src0[_IX] > constant ? _bitNum : 0);
            }
        }

        /*
         ============
         idSIMD_Generic::CmpGE

         dst[i] = src0[i] >= constant;
         ============
         */
        @Override
        public void CmpGE(boolean[] dst, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] >= constant;
                dst[_IX + 1] = src0[_IX + 1] >= constant;
                dst[_IX + 2] = src0[_IX + 2] >= constant;
                dst[_IX + 3] = src0[_IX + 3] >= constant;
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] >= constant;
            }
        }

        /*
         ============
         idSIMD_Generic::CmpGE

         dst[i] |= ( src0[i] >= constant ) << bitNum;
         ============
         */
        @Override
        public void CmpGE(byte[] dst, byte bitNum, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            final byte _bitNum = (byte) (1 << bitNum);//TODO:check byte signage
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = (byte) (src0[_IX + 0] >= constant ? _bitNum : 0);
                dst[_IX + 1] = (byte) (src0[_IX + 1] >= constant ? _bitNum : 0);
                dst[_IX + 2] = (byte) (src0[_IX + 2] >= constant ? _bitNum : 0);
                dst[_IX + 3] = (byte) (src0[_IX + 3] >= constant ? _bitNum : 0);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = (byte) (src0[_IX] >= constant ? _bitNum : 0);
            }
        }

        /*
         ============
         idSIMD_Generic::CmpLT

         dst[i] = src0[i] < constant;
         ============
         */
        @Override
        public void CmpLT(boolean[] dst, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] < constant;
                dst[_IX + 1] = src0[_IX + 1] < constant;
                dst[_IX + 2] = src0[_IX + 2] < constant;
                dst[_IX + 3] = src0[_IX + 3] < constant;
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] < constant;
            }
        }

        /*
         ============
         idSIMD_Generic::CmpLT

         dst[i] |= ( src0[i] < constant ) << bitNum;
         ============
         */
        @Override
        public void CmpLT(byte[] dst, byte bitNum, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            final byte _bitNum = (byte) (1 << bitNum);//TODO:check byte signage
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = (byte) (src0[_IX + 0] < constant ? _bitNum : 0);
                dst[_IX + 1] = (byte) (src0[_IX + 1] < constant ? _bitNum : 0);
                dst[_IX + 2] = (byte) (src0[_IX + 2] < constant ? _bitNum : 0);
                dst[_IX + 3] = (byte) (src0[_IX + 3] < constant ? _bitNum : 0);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = (byte) (src0[_IX] < constant ? _bitNum : 0);
            }
        }

        /*
         ============
         idSIMD_Generic::CmpLE

         dst[i] = src0[i] <= constant;
         ============
         */
        @Override
        public void CmpLE(boolean[] dst, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = src0[_IX + 0] <= constant;
                dst[_IX + 1] = src0[_IX + 1] <= constant;
                dst[_IX + 2] = src0[_IX + 2] <= constant;
                dst[_IX + 3] = src0[_IX + 3] <= constant;
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = src0[_IX] <= constant;
            }
        }

        @Override
        public void CmpLE(byte[] dst, byte bitNum, float[] src0, float constant, int count) {
            int _IX, _NM = count & 0xfffffffc;
            final byte _bitNum = (byte) (1 << bitNum);//TODO:check byte signage
            for (_IX = 0; _IX < _NM; _IX += 4) {
                dst[_IX + 0] = (byte) (src0[_IX + 0] <= constant ? _bitNum : 0);
                dst[_IX + 1] = (byte) (src0[_IX + 1] <= constant ? _bitNum : 0);
                dst[_IX + 2] = (byte) (src0[_IX + 2] <= constant ? _bitNum : 0);
                dst[_IX + 3] = (byte) (src0[_IX + 3] <= constant ? _bitNum : 0);
            }
            for (; _IX < count; _IX++) {
                dst[_IX] = (byte) (src0[_IX] <= constant ? _bitNum : 0);
            }
        }

        @Override
        public void MinMax(float[] min, float[] max, float[] src, int count) {
            min[0] = idMath.INFINITY;
            max[0] = -idMath.INFINITY;

            for (int _IX = 0; _IX < count; _IX++) {
                if (src[(_IX)] < min[0]) {
                    min[0] = src[(_IX)];
                } else if (src[(_IX)] > max[0]) {
                    max[0] = src[(_IX)];
                }
            }
        }

        @Override
        public void MinMax(idVec2 min, idVec2 max, idVec2[] src, int count) {
            min.x = min.y = idMath.INFINITY;
            max.x = max.y = -idMath.INFINITY;
            for (int _IX = 0; _IX < count; _IX++) {
                final idVec2 v = src[_IX];
                if (v.x < min.x) {
                    min.x = v.x;
                } else if (v.x > max.x) {
                    max.x = v.x;
                }
                if (v.y < min.y) {
                    min.y = v.y;
                } else if (v.y > max.y) {
                    max.y = v.y;
                }
            }
        }

        @Override
        public void MinMax(idVec3 min, idVec3 max, idVec3[] src, int count) {
            min.x = min.y = min.z = idMath.INFINITY;
            max.x = max.y = max.z = -idMath.INFINITY;
            for (int _IX = 0; _IX < count; _IX++) {
                final idVec3 v = src[_IX];
                if (v.x < min.x) {
                    min.x = v.x;
                } else if (v.x > max.x) {
                    max.x = v.x;
                }
                if (v.y < min.y) {
                    min.y = v.y;
                } else if (v.y > max.y) {
                    max.y = v.y;
                }
                if (v.z < min.z) {
                    min.z = v.z;
                } else if (v.z > max.z) {
                    max.z = v.z;
                }
            }
        }

        @Override
        public void MinMax(idVec3 min, idVec3 max, idDrawVert[] src, int count) {
            min.x = min.y = min.z = idMath.INFINITY;
            max.x = max.y = max.z = -idMath.INFINITY;
            for (int _IX = 0; _IX < count; _IX++) {
                final idDrawVert v = src[_IX];
                if (v.oGet(0) < min.x) {
                    min.x = v.oGet(0);
                } else if (v.oGet(0) > max.x) {
                    max.x = v.oGet(0);
                }
                if (v.oGet(1) < min.y) {
                    min.y = v.oGet(1);
                } else if (v.oGet(1) > max.y) {
                    max.y = v.oGet(1);
                }
                if (v.oGet(2) < min.z) {
                    min.z = v.oGet(2);
                } else if (v.oGet(2) > max.z) {
                    max.z = v.oGet(2);
                }
            }
        }

        @Override
        public void MinMax(idVec3 min, idVec3 max, idDrawVert[] src, int[] indexes, int count) {
            min.x = min.y = min.z = idMath.INFINITY;
            max.x = max.y = max.z = -idMath.INFINITY;
            for (int _IX = 0; _IX < count; _IX++) {
                final idDrawVert v = src[indexes[_IX]];
                if (v.oGet(0) < min.x) {
                    min.x = v.oGet(0);
                } else if (v.oGet(0) > max.x) {
                    max.x = v.oGet(0);
                }
                if (v.oGet(1) < min.y) {
                    min.y = v.oGet(1);
                } else if (v.oGet(1) > max.y) {
                    max.y = v.oGet(1);
                }
                if (v.oGet(2) < min.z) {
                    min.z = v.oGet(2);
                } else if (v.oGet(2) > max.z) {
                    max.z = v.oGet(2);
                }
            }
        }

        @Override
        public void Clamp(float[] dst, float[] src, float min, float max, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src[_IX] < min ? min : src[_IX] > max ? max : src[_IX];
            }
        }

        @Override
        public void ClampMin(float[] dst, float[] src, float min, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src[_IX] < min ? min : src[_IX];
            }
        }

        @Override
        public void ClampMax(float[] dst, float[] src, float max, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src[_IX] > max ? max : src[_IX];
            }
        }

        @Override
        public void Memcpy(Object[] dst, Object[] src, int count) {
//            memcpy( dst, src, count );
            System.arraycopy(src, 0, dst, 0, count);
        }

        @Override
        public void Memset(Object[] dst, int val, int count) {
//            memset( dst, val, count );
            Arrays.fill(dst, 0, count, val);
        }

        @Override
        public void Zero16(float[] dst, int count) {
//            memset( dst, 0, count * sizeof( float ) );
            Arrays.fill(dst, 0, count, 0);
        }

        @Override
        public void Negate16(float[] dst, int count) {
//            unsigned int *ptr = reinterpret_cast<unsigned int *>(dst);
            for (int _IX = 0; _IX < count; _IX++) {
                int _dst = Float.floatToIntBits(dst[_IX]);
                _dst ^= (1 << 31);// IEEE 32 bits float sign bit
                dst[_IX] = Float.intBitsToFloat(_dst);
            }
        }

        @Override
        public void Copy16(float[] dst, float[] src, int count) {
//            for (int _IX = 0; _IX < count; _IX++) {
//                dst[_IX] = src[_IX];
//            }
            System.arraycopy(src, 0, dst, 0, count);
        }

        @Override
        public void Add16(float[] dst, float[] src1, float[] src2, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src1[_IX] + src2[_IX];
            }
        }

        @Override
        public void Sub16(float[] dst, float[] src1, float[] src2, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src1[_IX] - src2[_IX];
            }
        }

        @Override
        public void Mul16(float[] dst, float[] src1, float constant, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] = src1[_IX] * constant;
            }
        }

        @Override
        public void AddAssign16(float[] dst, float[] src, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] += src[_IX];
            }
        }

        @Override
        public void SubAssign16(float[] dst, float[] src, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] -= src[_IX];
            }
        }

        @Override
        public void MulAssign16(float[] dst, float constant, int count) {
            for (int _IX = 0; _IX < count; _IX++) {
                dst[_IX] *= constant;
            }
        }

        @Override
        public void MatX_MultiplyVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, j, numRows;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumColumns());
            assert (dst.GetSize() >= mat.GetNumRows());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numRows = mat.GetNumRows();
            switch (mat.GetNumColumns()) {
                case 1:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1];
                        mIndex += 2;
                    }
                    break;
                case 3:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2];
                        mIndex += 3;
                    }
                    break;
                case 4:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3];
                        mIndex += 4;
                    }
                    break;
                case 5:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4];
                        mIndex += 5;
                    }
                    break;
                case 6:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] = mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4] + mPtr[mIndex + 5] * vPtr[5];
                        mIndex += 6;
                    }
                    break;
                default:
                    int numColumns = mat.GetNumColumns();
                    for (i = 0; i < numRows; i++) {
                        float sum = mPtr[mIndex + 0] * vPtr[0];
                        for (j = 1; j < numColumns; j++) {
                            sum += mPtr[mIndex + j] * vPtr[j];
                        }
                        dstPtr[i] = sum;
                        mIndex += numColumns;
                    }
                    break;
            }
        }

        @Override
        public void MatX_MultiplyAddVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, j, numRows;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumColumns());
            assert (dst.GetSize() >= mat.GetNumRows());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numRows = mat.GetNumRows();
            switch (mat.GetNumColumns()) {
                case 1:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1];
                        mIndex += 2;
                    }
                    break;
                case 3:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2];
                        mIndex += 3;
                    }
                    break;
                case 4:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3];
                        mIndex += 4;
                    }
                    break;
                case 5:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4];
                        mIndex += 5;
                    }
                    break;
                case 6:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] += mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4] + mPtr[mIndex + 5] * vPtr[5];
                        mIndex += 6;
                    }
                    break;
                default:
                    int numColumns = mat.GetNumColumns();
                    for (i = 0; i < numRows; i++) {
                        float sum = mPtr[mIndex + 0] * vPtr[0];
                        for (j = 1; j < numColumns; j++) {
                            sum += mPtr[mIndex + j] * vPtr[j];
                        }
                        dstPtr[i] += sum;
                        mIndex += numColumns;
                    }
                    break;
            }
        }

        @Override
        public void MatX_MultiplySubVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, j, numRows;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumColumns());
            assert (dst.GetSize() >= mat.GetNumRows());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numRows = mat.GetNumRows();
            switch (mat.GetNumColumns()) {
                case 1:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1];
                        mIndex += 2;
                    }
                    break;
                case 3:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2];
                        mIndex += 3;
                    }
                    break;
                case 4:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3];
                        mIndex += 4;
                    }
                    break;
                case 5:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4];
                        mIndex += 5;
                    }
                    break;
                case 6:
                    for (i = 0; i < numRows; i++) {
                        dstPtr[i] -= mPtr[mIndex + 0] * vPtr[0] + mPtr[mIndex + 1] * vPtr[1] + mPtr[mIndex + 2] * vPtr[2]
                                + mPtr[mIndex + 3] * vPtr[3] + mPtr[mIndex + 4] * vPtr[4] + mPtr[mIndex + 5] * vPtr[5];
                        mIndex += 6;
                    }
                    break;
                default:
                    int numColumns = mat.GetNumColumns();
                    for (i = 0; i < numRows; i++) {
                        float sum = mPtr[mIndex + 0] * vPtr[0];
                        for (j = 1; j < numColumns; j++) {
                            sum += mPtr[mIndex + j] * vPtr[j];
                        }
                        dstPtr[i] -= sum;
                        mIndex += numColumns;
                    }
                    break;
            }
        }

        @Override
        public void MatX_TransposeMultiplyVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, j, numColumns;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumRows());
            assert (dst.GetSize() >= mat.GetNumColumns());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numColumns = mat.GetNumColumns();
            switch (mat.GetNumRows()) {
                case 1:
                    for (i = 0; i < numColumns; i++) {//TODO:check pointer to array conversion
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1];
                        mIndex++;
                    }
                    break;
                case 3:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2];
                        mIndex++;
                    }
                    break;
                case 4:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3];
                        mIndex++;
                    }
                    break;
                case 5:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4];
                        mIndex++;
                    }
                    break;
                case 6:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] = (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4] + (mPtr[mIndex] + 5 * numColumns) * vPtr[5];
                        mIndex++;
                    }
                    break;
                default:
                    int numRows = mat.GetNumRows();
                    for (i = 0; i < numColumns; i++) {
                        mIndex = i;
                        float sum = mPtr[0] * vPtr[0];
                        for (j = 1; j < numRows; j++) {
                            mIndex += numColumns;
                            sum += mPtr[0] * vPtr[j];
                        }
                        dstPtr[i] = sum;
                    }
                    break;
            }
        }

        @Override
        public void MatX_TransposeMultiplyAddVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, j, numColumns;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumRows());
            assert (dst.GetSize() >= mat.GetNumColumns());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numColumns = mat.GetNumColumns();
            switch (mat.GetNumRows()) {
                case 1:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1];
                        mIndex++;
                    }
                    break;
                case 3:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2];
                        mIndex++;
                    }
                    break;
                case 4:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3];
                        mIndex++;
                    }
                    break;
                case 5:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4];
                        mIndex++;
                    }
                    break;
                case 6:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] += (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4] + (mPtr[mIndex] + 5 * numColumns) * vPtr[5];
                        mIndex++;
                    }
                    break;
                default:
                    int numRows = mat.GetNumRows();
                    for (i = 0; i < numColumns; i++) {
                        mIndex = i;
                        float sum = mPtr[0] * vPtr[0];
                        for (j = 1; j < numRows; j++) {
                            mIndex += numColumns;
                            sum += mPtr[0] * vPtr[j];
                        }
                        dstPtr[i] += sum;
                    }
                    break;
            }
        }

        @Override
        public void MatX_TransposeMultiplySubVecX(idVecX dst, idMatX mat, idVecX vec) {
            int i, numColumns;
            int mIndex = 0;
            final float[] mPtr, vPtr;
            float[] dstPtr;

            assert (vec.GetSize() >= mat.GetNumRows());
            assert (dst.GetSize() >= mat.GetNumColumns());

            mPtr = mat.ToFloatPtr();
            vPtr = vec.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            numColumns = mat.GetNumColumns();
            switch (mat.GetNumRows()) {
                case 1:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0];
                        mIndex++;
                    }
                    break;
                case 2:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1];
                        mIndex++;
                    }
                    break;
                case 3:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2];
                        mIndex++;
                    }
                    break;
                case 4:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3];
                        mIndex++;
                    }
                    break;
                case 5:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4];
                        mIndex++;
                    }
                    break;
                case 6:
                    for (i = 0; i < numColumns; i++) {
                        dstPtr[i] -= (mPtr[mIndex]) * vPtr[0] + (mPtr[mIndex] + numColumns) * vPtr[1] + (mPtr[mIndex] + 2 * numColumns) * vPtr[2]
                                + (mPtr[mIndex] + 3 * numColumns) * vPtr[3] + (mPtr[mIndex] + 4 * numColumns) * vPtr[4] + (mPtr[mIndex] + 5 * numColumns) * vPtr[5];
                        mIndex++;
                    }
                    break;
                default:
                    int numRows = mat.GetNumRows();
                    for (i = 0; i < numColumns; i++) {
                        mIndex = i;
                        float sum = mPtr[0] * vPtr[0];
                        for (int j = 1; j < numRows; j++) {
                            mIndex += numColumns;
                            sum += mPtr[0] * vPtr[j];
                        }
                        dstPtr[i] -= sum;
                    }
                    break;
            }
        }

        /*
         ============
         idSIMD_Generic::MatX_MultiplyMatX

         optimizes the following matrix multiplications:

         NxN * Nx6
         6xN * Nx6
         Nx6 * 6xN
         6x6 * 6xN

         with N in the range [1-6].
         ============
         */
        @Override
        public void MatX_MultiplyMatX(idMatX dst, idMatX m1, idMatX m2) {
            int i, j, k, l, n;
            int m1Index = 0, m2Index = 0, dIndex = 0;
            float[] dstPtr;
            final float[] m1Ptr, m2Ptr;
            double sum;

            assert (m1.GetNumColumns() == m2.GetNumRows());

            dstPtr = dst.ToFloatPtr();//TODO:check floatptr back reference
            m1Ptr = m1.ToFloatPtr();
            m2Ptr = m2.ToFloatPtr();
            k = m1.GetNumRows();
            l = m2.GetNumColumns();

            switch (m1.GetNumColumns()) {
                case 1: {
                    if (l == 6) {
                        for (i = 0; i < k; i++) {		// Nx1 * 1x6
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 0];
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 1];
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 2];
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 3];
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 4];
                            dstPtr[dIndex++] = m1Ptr[m1Index + i] * m2Ptr[m2Index + 5];
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                }
                case 2: {
                    if (l == 6) {
                        for (i = 0; i < k; i++) {		// Nx2 * 2x6
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 6];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 1] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 7];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 2] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 8];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 3] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 9];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 4] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 10];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 5] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 11];
                            m1Index += 2;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + l];
                            m2Index++;
                        }
                        m1Index += 2;
                    }
                    break;
                }
                case 3: {
                    if (l == 6) {
                        for (i = 0; i < k; i++) {		// Nx3 * 3x6
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 6] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 12];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 1] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 7] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 13];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 2] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 8] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 14];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 3] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 9] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 15];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 4] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 10] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 16];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 5] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 11] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 17];
                            m1Index += 3;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * l];
                            m2Index++;
                        }
                        m1Index += 3;
                    }
                    break;
                }
                case 4: {
                    if (l == 6) {
                        for (i = 0; i < k; i++) {		// Nx4 * 4x6
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 6] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 12] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 18];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 1] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 7] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 13] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 19];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 2] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 8] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 14] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 20];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 3] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 9] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 15] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 21];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 4] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 10] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 16] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 22];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 5] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 11] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 17] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 23];
                            m1Index += 4;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * l];
                            m2Index++;
                        }
                        m1Index += 4;
                    }
                    break;
                }
                case 5: {
                    if (l == 6) {
                        for (i = 0; i < k; i++) {		// Nx5 * 5x6
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 6] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 12] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 18] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 24];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 1] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 7] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 13] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 19] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 25];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 2] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 8] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 14] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 20] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 26];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 3] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 9] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 15] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 21] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 27];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 4] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 10] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 16] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 22] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 28];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 5] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 11] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 17] + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 23] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 29];
                            m1Index += 5;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * l] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * l];
                            m2Index++;
                        }
                        m1Index += 5;
                    }
                    break;
                }
                case 6: {
                    switch (k) {
                        case 1: {
                            if (l == 1) {		// 1x6 * 6x1
                                dstPtr[0] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2]
                                        + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4] + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5];
                                return;
                            }
                            break;
                        }
                        case 2: {
                            if (l == 2) {		// 2x6 * 6x2
                                for (i = 0; i < 2; i++) {
                                    for (j = 0; j < 2; j++) {
                                        dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 2 + j]
                                                + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 2 + j]
                                                + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 2 + j]
                                                + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 2 + j]
                                                + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 2 + j]
                                                + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 2 + j];
                                        dIndex++;
                                    }
                                    m1Index += 6;
                                }
                                return;
                            }
                            break;
                        }
                        case 3: {
                            if (l == 3) {		// 3x6 * 6x3
                                for (i = 0; i < 3; i++) {
                                    for (j = 0; j < 3; j++) {
                                        dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 3 + j]
                                                + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 3 + j]
                                                + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 3 + j]
                                                + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 3 + j]
                                                + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 3 + j]
                                                + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 3 + j];
                                        dIndex++;
                                    }
                                    m1Index += 6;
                                }
                                return;
                            }
                            break;
                        }
                        case 4: {
                            if (l == 4) {		// 4x6 * 6x4
                                for (i = 0; i < 4; i++) {
                                    for (j = 0; j < 4; j++) {
                                        dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 4 + j]
                                                + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 4 + j]
                                                + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 4 + j]
                                                + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 4 + j]
                                                + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 4 + j]
                                                + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 4 + j];
                                        dIndex++;
                                    }
                                    m1Index += 6;
                                }
                                return;
                            }
                        }
                        case 5: {
                            if (l == 5) {		// 5x6 * 6x5
                                for (i = 0; i < 5; i++) {
                                    for (j = 0; j < 5; j++) {
                                        dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 5 + j]
                                                + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 5 + j]
                                                + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 5 + j]
                                                + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 5 + j]
                                                + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 5 + j]
                                                + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 5 + j];
                                        dIndex++;
                                    }
                                    m1Index += 6;
                                }
                                return;
                            }
                        }
                        case 6: {
                            switch (l) {
                                case 1: {		// 6x6 * 6x1
                                    for (i = 0; i < 6; i++) {
                                        dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 1]
                                                + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 1]
                                                + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 1]
                                                + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 1]
                                                + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 1]
                                                + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 1];
                                        dIndex++;
                                        m1Index += 6;
                                    }
                                    return;
                                }
                                case 2: {		// 6x6 * 6x2
                                    for (i = 0; i < 6; i++) {
                                        for (j = 0; j < 2; j++) {
                                            dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 2 + j]
                                                    + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 2 + j]
                                                    + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 2 + j]
                                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 2 + j]
                                                    + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 2 + j]
                                                    + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 2 + j];
                                            dIndex++;
                                        }
                                        m1Index += 6;
                                    }
                                    return;
                                }
                                case 3: {		// 6x6 * 6x3
                                    for (i = 0; i < 6; i++) {
                                        for (j = 0; j < 3; j++) {
                                            dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 3 + j]
                                                    + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 3 + j]
                                                    + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 3 + j]
                                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 3 + j]
                                                    + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 3 + j]
                                                    + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 3 + j];
                                            dIndex++;
                                        }
                                        m1Index += 6;
                                    }
                                    return;
                                }
                                case 4: {		// 6x6 * 6x4
                                    for (i = 0; i < 6; i++) {
                                        for (j = 0; j < 4; j++) {
                                            dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 4 + j]
                                                    + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 4 + j]
                                                    + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 4 + j]
                                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 4 + j]
                                                    + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 4 + j]
                                                    + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 4 + j];
                                            dIndex++;
                                        }
                                        m1Index += 6;
                                    }
                                    return;
                                }
                                case 5: {		// 6x6 * 6x5
                                    for (i = 0; i < 6; i++) {
                                        for (j = 0; j < 5; j++) {
                                            dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 5 + j]
                                                    + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 5 + j]
                                                    + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 5 + j]
                                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 5 + j]
                                                    + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 5 + j]
                                                    + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 5 + j];
                                            dIndex++;
                                        }
                                        m1Index += 6;
                                    }
                                    return;
                                }
                                case 6: {		// 6x6 * 6x6
                                    for (i = 0; i < 6; i++) {
                                        for (j = 0; j < 6; j++) {
                                            dstPtr[dIndex] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0 * 6 + j]
                                                    + m1Ptr[m1Index + 1] * m2Ptr[m2Index + 1 * 6 + j]
                                                    + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * 6 + j]
                                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * 6 + j]
                                                    + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * 6 + j]
                                                    + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * 6 + j];
                                            dIndex++;
                                        }
                                        m1Index += 6;
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + 1] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3] * m2Ptr[m2Index + 3 * l] + m1Ptr[m1Index + 4] * m2Ptr[m2Index + 4 * l] + m1Ptr[m1Index + 5] * m2Ptr[m2Index + 5 * l];
                            m2Index++;
                        }
                        m1Index += 6;
                    }
                    break;
                }
                default: {
                    for (i = 0; i < k; i++) {
                        for (j = 0; j < l; j++) {
                            m2Index = j;
                            sum = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            for (n = 1; n < m1.GetNumColumns(); n++) {
                                m2Index += l;
                                sum += m1Ptr[m1Index + n] * m2Ptr[m2Index + 0];
                            }
                            dstPtr[dIndex++] = (float) sum;
                        }
                        m1Index += m1.GetNumColumns();
                    }
                    break;
                }
            }
        }

        /*
         ============
         idSIMD_Generic::MatX_TransposeMultiplyMatX

         optimizes the following tranpose matrix multiplications:

         Nx6 * NxN
         6xN * 6x6

         with N in the range [1-6].
         ============
         */
        @Override
        public void MatX_TransposeMultiplyMatX(idMatX dst, idMatX m1, idMatX m2) {
            int i, j, k, l, n;
            int m1Index = 0, m2Index = 0, dIndex = 0;
            float[] dstPtr;
            final float[] m1Ptr, m2Ptr;
            double sum;

            assert (m1.GetNumRows() == m2.GetNumRows());

            m1Ptr = m1.ToFloatPtr();
            m2Ptr = m2.ToFloatPtr();
            dstPtr = dst.ToFloatPtr();
            k = m1.GetNumColumns();
            l = m2.GetNumColumns();

            switch (m1.GetNumRows()) {
                case 1:
                    if (k == 6 && l == 1) {			// 1x6 * 1x1
                        for (i = 0; i < 6; i++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            m1Index++;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                case 2:
                    if (k == 6 && l == 2) {			// 2x6 * 2x2
                        for (i = 0; i < 6; i++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 2 + 0] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 2 + 0];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 2 + 1] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 2 + 1];
                            m1Index++;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + k] * m2Ptr[m2Index + l];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                case 3:
                    if (k == 6 && l == 3) {			// 3x6 * 3x3
                        for (i = 0; i < 6; i++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 3 + 0] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 3 + 0] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 3 + 0];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 3 + 1] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 3 + 1] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 3 + 1];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 3 + 2] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 3 + 2] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 3 + 2];
                            m1Index++;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + k] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2 * k] * m2Ptr[m2Index + 2 * l];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                case 4:
                    if (k == 6 && l == 4) {			// 4x6 * 4x4
                        for (i = 0; i < 6; i++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 4 + 0] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 4 + 0] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 4 + 0] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 4 + 0];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 4 + 1] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 4 + 1] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 4 + 1] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 4 + 1];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 4 + 2] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 4 + 2] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 4 + 2] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 4 + 2];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 4 + 3] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 4 + 3] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 4 + 3] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 4 + 3];
                            m1Index++;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + k] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2 * k] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3 * k] * m2Ptr[m2Index + 3 * l];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                case 5:
                    if (k == 6 && l == 5) {			// 5x6 * 5x5
                        for (i = 0; i < 6; i++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 5 + 0] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 5 + 0] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 5 + 0] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 5 + 0] + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 5 + 0];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 5 + 1] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 5 + 1] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 5 + 1] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 5 + 1] + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 5 + 1];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 5 + 2] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 5 + 2] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 5 + 2] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 5 + 2] + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 5 + 2];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 5 + 3] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 5 + 3] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 5 + 3] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 5 + 3] + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 5 + 3];
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 5 + 4] + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 5 + 4] + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 5 + 4] + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 5 + 4] + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 5 + 4];
                            m2Index++;
                        }
                        return;
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + k] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2 * k] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3 * k] * m2Ptr[m2Index + 3 * l] + m1Ptr[m1Index + 4 * k] * m2Ptr[m2Index + 4 * l];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                case 6:
                    if (l == 6) {
                        switch (k) {
                            case 1:						// 6x1 * 6x6
                                m2Index = 0;
                                for (j = 0; j < 6; j++) {
                                    dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 1] * m2Ptr[m2Index + 0 * 6]
                                            + m1Ptr[m1Index + 1 * 1] * m2Ptr[m2Index + 1 * 6]
                                            + m1Ptr[m1Index + 2 * 1] * m2Ptr[m2Index + 2 * 6]
                                            + m1Ptr[m1Index + 3 * 1] * m2Ptr[m2Index + 3 * 6]
                                            + m1Ptr[m1Index + 4 * 1] * m2Ptr[m2Index + 4 * 6]
                                            + m1Ptr[m1Index + 5 * 1] * m2Ptr[m2Index + 5 * 6];
                                    m2Index++;
                                }
                                return;
                            case 2:						// 6x2 * 6x6
                                for (i = 0; i < 2; i++) {
                                    m2Index = 0;
                                    for (j = 0; j < 6; j++) {
                                        dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 2] * m2Ptr[m2Index + 0 * 6]
                                                + m1Ptr[m1Index + 1 * 2] * m2Ptr[m2Index + 1 * 6]
                                                + m1Ptr[m1Index + 2 * 2] * m2Ptr[m2Index + 2 * 6]
                                                + m1Ptr[m1Index + 3 * 2] * m2Ptr[m2Index + 3 * 6]
                                                + m1Ptr[m1Index + 4 * 2] * m2Ptr[m2Index + 4 * 6]
                                                + m1Ptr[m1Index + 5 * 2] * m2Ptr[m2Index + 5 * 6];
                                        m2Index++;
                                    }
                                    m1Index++;
                                }
                                return;
                            case 3:						// 6x3 * 6x6
                                for (i = 0; i < 3; i++) {
                                    m2Index = 0;
                                    for (j = 0; j < 6; j++) {
                                        dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 3] * m2Ptr[m2Index + 0 * 6]
                                                + m1Ptr[m1Index + 1 * 3] * m2Ptr[m2Index + 1 * 6]
                                                + m1Ptr[m1Index + 2 * 3] * m2Ptr[m2Index + 2 * 6]
                                                + m1Ptr[m1Index + 3 * 3] * m2Ptr[m2Index + 3 * 6]
                                                + m1Ptr[m1Index + 4 * 3] * m2Ptr[m2Index + 4 * 6]
                                                + m1Ptr[m1Index + 5 * 3] * m2Ptr[m2Index + 5 * 6];
                                        m2Index++;
                                    }
                                    m1Index++;
                                }
                                return;
                            case 4:						// 6x4 * 6x6
                                for (i = 0; i < 4; i++) {
                                    m2Index = 0;
                                    for (j = 0; j < 6; j++) {
                                        dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 4] * m2Ptr[m2Index + 0 * 6]
                                                + m1Ptr[m1Index + 1 * 4] * m2Ptr[m2Index + 1 * 6]
                                                + m1Ptr[m1Index + 2 * 4] * m2Ptr[m2Index + 2 * 6]
                                                + m1Ptr[m1Index + 3 * 4] * m2Ptr[m2Index + 3 * 6]
                                                + m1Ptr[m1Index + 4 * 4] * m2Ptr[m2Index + 4 * 6]
                                                + m1Ptr[m1Index + 5 * 4] * m2Ptr[m2Index + 5 * 6];
                                        m2Index++;
                                    }
                                    m1Index++;
                                }
                                return;
                            case 5:						// 6x5 * 6x6
                                for (i = 0; i < 5; i++) {
                                    m2Index = 0;
                                    for (j = 0; j < 6; j++) {
                                        dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 5] * m2Ptr[m2Index + 0 * 6]
                                                + m1Ptr[m1Index + 1 * 5] * m2Ptr[m2Index + 1 * 6]
                                                + m1Ptr[m1Index + 2 * 5] * m2Ptr[m2Index + 2 * 6]
                                                + m1Ptr[m1Index + 3 * 5] * m2Ptr[m2Index + 3 * 6]
                                                + m1Ptr[m1Index + 4 * 5] * m2Ptr[m2Index + 4 * 6]
                                                + m1Ptr[m1Index + 5 * 5] * m2Ptr[m2Index + 5 * 6];
                                        m2Index++;
                                    }
                                    m1Index++;
                                }
                                return;
                            case 6:						// 6x6 * 6x6
                                for (i = 0; i < 6; i++) {
                                    m2Index = 0;
                                    for (j = 0; j < 6; j++) {
                                        dstPtr[dIndex++] = m1Ptr[m1Index + 0 * 6] * m2Ptr[m2Index + 0 * 6]
                                                + m1Ptr[m1Index + 1 * 6] * m2Ptr[m2Index + 1 * 6]
                                                + m1Ptr[m1Index + 2 * 6] * m2Ptr[m2Index + 2 * 6]
                                                + m1Ptr[m1Index + 3 * 6] * m2Ptr[m2Index + 3 * 6]
                                                + m1Ptr[m1Index + 4 * 6] * m2Ptr[m2Index + 4 * 6]
                                                + m1Ptr[m1Index + 5 * 6] * m2Ptr[m2Index + 5 * 6];
                                        m2Index++;
                                    }
                                    m1Index++;
                                }
                                return;
                        }
                    }
                    for (i = 0; i < k; i++) {
                        m2Index = 0;
                        for (j = 0; j < l; j++) {
                            dstPtr[dIndex++] = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0] + m1Ptr[m1Index + k] * m2Ptr[m2Index + l] + m1Ptr[m1Index + 2 * k] * m2Ptr[m2Index + 2 * l]
                                    + m1Ptr[m1Index + 3 * k] * m2Ptr[m2Index + 3 * l] + m1Ptr[m1Index + 4 * k] * m2Ptr[m2Index + 4 * l] + m1Ptr[m1Index + 5 * k] * m2Ptr[m2Index + 5 * l];
                            m2Index++;
                        }
                        m1Index++;
                    }
                    break;
                default:
                    for (i = 0; i < k; i++) {
                        for (j = 0; j < l; j++) {
                            m1Index = i;
                            m2Index = j;
                            sum = m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            for (n = 1; n < m1.GetNumRows(); n++) {
                                m1Index += k;
                                m2Index += l;
                                sum += m1Ptr[m1Index + 0] * m2Ptr[m2Index + 0];
                            }
                            dstPtr[dIndex++] = (float) sum;
                        }
                    }
                    break;
            }
        }

        private int NSKIP(final int n, final int s) {
            return ((n << 3) | (s & 7));
        }
        private final int NSKIP1_0 = ((1 << 3) | (0 & 7)),
                NSKIP2_0 = ((2 << 3) | (0 & 7)), NSKIP2_1 = ((2 << 3) | (1 & 7)),
                NSKIP3_0 = ((3 << 3) | (0 & 7)), NSKIP3_1 = ((3 << 3) | (1 & 7)), NSKIP3_2 = ((3 << 3) | (2 & 7)),
                NSKIP4_0 = ((4 << 3) | (0 & 7)), NSKIP4_1 = ((4 << 3) | (1 & 7)), NSKIP4_2 = ((4 << 3) | (2 & 7)), NSKIP4_3 = ((4 << 3) | (3 & 7)),
                NSKIP5_0 = ((5 << 3) | (0 & 7)), NSKIP5_1 = ((5 << 3) | (1 & 7)), NSKIP5_2 = ((5 << 3) | (2 & 7)), NSKIP5_3 = ((5 << 3) | (3 & 7)), NSKIP5_4 = ((5 << 3) | (4 & 7)),
                NSKIP6_0 = ((6 << 3) | (0 & 7)), NSKIP6_1 = ((6 << 3) | (1 & 7)), NSKIP6_2 = ((6 << 3) | (2 & 7)), NSKIP6_3 = ((6 << 3) | (3 & 7)), NSKIP6_4 = ((6 << 3) | (4 & 7)), NSKIP6_5 = ((6 << 3) | (5 & 7)),
                NSKIP7_0 = ((7 << 3) | (0 & 7)), NSKIP7_1 = ((7 << 3) | (1 & 7)), NSKIP7_2 = ((7 << 3) | (2 & 7)), NSKIP7_3 = ((7 << 3) | (3 & 7)), NSKIP7_4 = ((7 << 3) | (4 & 7)), NSKIP7_5 = ((7 << 3) | (5 & 7)), NSKIP7_6 = ((7 << 3) | (6 & 7));

        @Override
        public void MatX_LowerTriangularSolve(idMatX L, float[] x, float[] b, int n) {
            MatX_LowerTriangularSolve(L, x, b, n, 0);
        }

        /*
         ============
         idSIMD_Generic::MatX_LowerTriangularSolve

         solves x in Lx = b for the n * n sub-matrix of L
         if skip > 0 the first skip elements of x are assumed to be valid already
         L has to be a lower triangular matrix with (implicit) ones on the diagonal
         x == b is allowed
         ============
         */
        @Override
        public void MatX_LowerTriangularSolve(idMatX L, float[] x, float[] b, int n, int skip) {
//#if 1

            int nc;
            float[] lptr;
            int lIndex = 0;

            if (skip >= n) {
                return;
            }

            lptr = L.ToFloatPtr();
            nc = L.GetNumColumns();

            // unrolled cases for n < 8
            if (n < 8) {
//		#define NSKIP( n, s )	((n<<3)|(s&7))
                switch (NSKIP(n, skip)) {
                    case NSKIP1_0:
                        x[0] = b[0];
                        return;
                    case NSKIP2_0:
                        x[0] = b[0];
                    case NSKIP2_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                        return;
                    case NSKIP3_0:
                        x[0] = b[0];
                    case NSKIP3_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                    case NSKIP3_2:
                        x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                        return;
                    case NSKIP4_0:
                        x[0] = b[0];
                    case NSKIP4_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                    case NSKIP4_2:
                        x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                    case NSKIP4_3:
                        x[3] = b[3] - lptr[3 * nc + 0] * x[0] - lptr[3 * nc + 1] * x[1] - lptr[3 * nc + 2] * x[2];
                        return;
                    case NSKIP5_0:
                        x[0] = b[0];
                    case NSKIP5_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                    case NSKIP5_2:
                        x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                    case NSKIP5_3:
                        x[3] = b[3] - lptr[3 * nc + 0] * x[0] - lptr[3 * nc + 1] * x[1] - lptr[3 * nc + 2] * x[2];
                    case NSKIP5_4:
                        x[4] = b[4] - lptr[4 * nc + 0] * x[0] - lptr[4 * nc + 1] * x[1] - lptr[4 * nc + 2] * x[2] - lptr[4 * nc + 3] * x[3];
                        return;
                    case NSKIP6_0:
                        x[0] = b[0];
                    case NSKIP6_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                    case NSKIP6_2:
                        x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                    case NSKIP6_3:
                        x[3] = b[3] - lptr[3 * nc + 0] * x[0] - lptr[3 * nc + 1] * x[1] - lptr[3 * nc + 2] * x[2];
                    case NSKIP6_4:
                        x[4] = b[4] - lptr[4 * nc + 0] * x[0] - lptr[4 * nc + 1] * x[1] - lptr[4 * nc + 2] * x[2] - lptr[4 * nc + 3] * x[3];
                    case NSKIP6_5:
                        x[5] = b[5] - lptr[5 * nc + 0] * x[0] - lptr[5 * nc + 1] * x[1] - lptr[5 * nc + 2] * x[2] - lptr[5 * nc + 3] * x[3] - lptr[5 * nc + 4] * x[4];
                        return;
                    case NSKIP7_0:
                        x[0] = b[0];
                    case NSKIP7_1:
                        x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                    case NSKIP7_2:
                        x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                    case NSKIP7_3:
                        x[3] = b[3] - lptr[3 * nc + 0] * x[0] - lptr[3 * nc + 1] * x[1] - lptr[3 * nc + 2] * x[2];
                    case NSKIP7_4:
                        x[4] = b[4] - lptr[4 * nc + 0] * x[0] - lptr[4 * nc + 1] * x[1] - lptr[4 * nc + 2] * x[2] - lptr[4 * nc + 3] * x[3];
                    case NSKIP7_5:
                        x[5] = b[5] - lptr[5 * nc + 0] * x[0] - lptr[5 * nc + 1] * x[1] - lptr[5 * nc + 2] * x[2] - lptr[5 * nc + 3] * x[3] - lptr[5 * nc + 4] * x[4];
                    case NSKIP7_6:
                        x[6] = b[6] - lptr[6 * nc + 0] * x[0] - lptr[6 * nc + 1] * x[1] - lptr[6 * nc + 2] * x[2] - lptr[6 * nc + 3] * x[3] - lptr[6 * nc + 4] * x[4] - lptr[6 * nc + 5] * x[5];
                        return;
                }
                return;
            }

            // process first 4 rows
            switch (skip) {
                case 0:
                    x[0] = b[0];
                case 1:
                    x[1] = b[1] - lptr[1 * nc + 0] * x[0];
                case 2:
                    x[2] = b[2] - lptr[2 * nc + 0] * x[0] - lptr[2 * nc + 1] * x[1];
                case 3:
                    x[3] = b[3] - lptr[3 * nc + 0] * x[0] - lptr[3 * nc + 1] * x[1] - lptr[3 * nc + 2] * x[2];
                    skip = 4;
            }

//	lptr = L[skip];
            lptr = L.oGet(skip);

            int i, j;
            double s0, s1, s2, s3;

            for (i = skip; i < n; i++) {
                s0 = lptr[lIndex + 0] * x[0];
                s1 = lptr[lIndex + 1] * x[1];
                s2 = lptr[lIndex + 2] * x[2];
                s3 = lptr[lIndex + 3] * x[3];
                for (j = 4; j < i - 7; j += 8) {
                    s0 += lptr[lIndex + j + 0] * x[j + 0];
                    s1 += lptr[lIndex + j + 1] * x[j + 1];
                    s2 += lptr[lIndex + j + 2] * x[j + 2];
                    s3 += lptr[lIndex + j + 3] * x[j + 3];
                    s0 += lptr[lIndex + j + 4] * x[j + 4];
                    s1 += lptr[lIndex + j + 5] * x[j + 5];
                    s2 += lptr[lIndex + j + 6] * x[j + 6];
                    s3 += lptr[lIndex + j + 7] * x[j + 7];
                }
                switch (i - j) {
//			NODEFAULT;
                    case 7:
                        s0 += lptr[lIndex + j + 6] * x[j + 6];
                    case 6:
                        s1 += lptr[lIndex + j + 5] * x[j + 5];
                    case 5:
                        s2 += lptr[lIndex + j + 4] * x[j + 4];
                    case 4:
                        s3 += lptr[lIndex + j + 3] * x[j + 3];
                    case 3:
                        s0 += lptr[lIndex + j + 2] * x[j + 2];
                    case 2:
                        s1 += lptr[lIndex + j + 1] * x[j + 1];
                    case 1:
                        s2 += lptr[lIndex + j + 0] * x[j + 0];
                    case 0:
                        break;
                }
                double sum;
                sum = s3;
                sum += s2;
                sum += s1;
                sum += s0;
                sum -= b[i];
                x[i] = (float) -sum;
                lIndex += nc;
            }

//#else
//
//	int i, j;
//	const float *lptr;
//	double sum;
//
//	for ( i = skip; i < n; i++ ) {
//		sum = b[i];
//		lptr = L[i];
//		for ( j = 0; j < i; j++ ) {
//			sum -= lptr[j] * x[j];
//		}
//		x[i] = sum;
//	}
//
//#endif
        }

        /*
         ============
         idSIMD_Generic::MatX_LowerTriangularSolveTranspose

         solves x in L'x = b for the n * n sub-matrix of L
         L has to be a lower triangular matrix with (implicit) ones on the diagonal
         x == b is allowed
         ============
         */
        @Override
        public void MatX_LowerTriangularSolveTranspose(idMatX L, float[] x, float[] b, int n) {
//#if 1

            int nc;
            float[] lptr;

            lptr = L.ToFloatPtr();
            nc = L.GetNumColumns();

            // unrolled cases for n < 8
            if (n < 8) {
                switch (n) {
                    case 0:
                        return;
                    case 1:
                        x[0] = b[0];
                        return;
                    case 2:
                        x[1] = b[1];
                        x[0] = b[0] - lptr[1 * nc + 0] * x[1];
                        return;
                    case 3:
                        x[2] = b[2];
                        x[1] = b[1] - lptr[2 * nc + 1] * x[2];
                        x[0] = b[0] - lptr[2 * nc + 0] * x[2] - lptr[1 * nc + 0] * x[1];
                        return;
                    case 4:
                        x[3] = b[3];
                        x[2] = b[2] - lptr[3 * nc + 2] * x[3];
                        x[1] = b[1] - lptr[3 * nc + 1] * x[3] - lptr[2 * nc + 1] * x[2];
                        x[0] = b[0] - lptr[3 * nc + 0] * x[3] - lptr[2 * nc + 0] * x[2] - lptr[1 * nc + 0] * x[1];
                        return;
                    case 5:
                        x[4] = b[4];
                        x[3] = b[3] - lptr[4 * nc + 3] * x[4];
                        x[2] = b[2] - lptr[4 * nc + 2] * x[4] - lptr[3 * nc + 2] * x[3];
                        x[1] = b[1] - lptr[4 * nc + 1] * x[4] - lptr[3 * nc + 1] * x[3] - lptr[2 * nc + 1] * x[2];
                        x[0] = b[0] - lptr[4 * nc + 0] * x[4] - lptr[3 * nc + 0] * x[3] - lptr[2 * nc + 0] * x[2] - lptr[1 * nc + 0] * x[1];
                        return;
                    case 6:
                        x[5] = b[5];
                        x[4] = b[4] - lptr[5 * nc + 4] * x[5];
                        x[3] = b[3] - lptr[5 * nc + 3] * x[5] - lptr[4 * nc + 3] * x[4];
                        x[2] = b[2] - lptr[5 * nc + 2] * x[5] - lptr[4 * nc + 2] * x[4] - lptr[3 * nc + 2] * x[3];
                        x[1] = b[1] - lptr[5 * nc + 1] * x[5] - lptr[4 * nc + 1] * x[4] - lptr[3 * nc + 1] * x[3] - lptr[2 * nc + 1] * x[2];
                        x[0] = b[0] - lptr[5 * nc + 0] * x[5] - lptr[4 * nc + 0] * x[4] - lptr[3 * nc + 0] * x[3] - lptr[2 * nc + 0] * x[2] - lptr[1 * nc + 0] * x[1];
                        return;
                    case 7:
                        x[6] = b[6];
                        x[5] = b[5] - lptr[6 * nc + 5] * x[6];
                        x[4] = b[4] - lptr[6 * nc + 4] * x[6] - lptr[5 * nc + 4] * x[5];
                        x[3] = b[3] - lptr[6 * nc + 3] * x[6] - lptr[5 * nc + 3] * x[5] - lptr[4 * nc + 3] * x[4];
                        x[2] = b[2] - lptr[6 * nc + 2] * x[6] - lptr[5 * nc + 2] * x[5] - lptr[4 * nc + 2] * x[4] - lptr[3 * nc + 2] * x[3];
                        x[1] = b[1] - lptr[6 * nc + 1] * x[6] - lptr[5 * nc + 1] * x[5] - lptr[4 * nc + 1] * x[4] - lptr[3 * nc + 1] * x[3] - lptr[2 * nc + 1] * x[2];
                        x[0] = b[0] - lptr[6 * nc + 0] * x[6] - lptr[5 * nc + 0] * x[5] - lptr[4 * nc + 0] * x[4] - lptr[3 * nc + 0] * x[3] - lptr[2 * nc + 0] * x[2] - lptr[1 * nc + 0] * x[1];
                        return;
                }
                return;
            }

            int i, j;
            double s0, s1, s2, s3;
            float[] xptr;
            int lIndex, xIndex;

            lptr = L.ToFloatPtr();
            lIndex = n * nc + n - 4;
            xptr = x;
            xIndex = n;

            // process 4 rows at a time
            for (i = n; i >= 4; i -= 4) {
                s0 = b[i - 4];
                s1 = b[i - 3];
                s2 = b[i - 2];
                s3 = b[i - 1];
                // process 4x4 blocks
                for (j = 0; j < n - i; j += 4) {
                    s0 -= lptr[lIndex + (j + 0) * nc + 0] * xptr[xIndex + j + 0];
                    s1 -= lptr[lIndex + (j + 0) * nc + 1] * xptr[xIndex + j + 0];
                    s2 -= lptr[lIndex + (j + 0) * nc + 2] * xptr[xIndex + j + 0];
                    s3 -= lptr[lIndex + (j + 0) * nc + 3] * xptr[xIndex + j + 0];
                    s0 -= lptr[lIndex + (j + 1) * nc + 0] * xptr[xIndex + j + 1];
                    s1 -= lptr[lIndex + (j + 1) * nc + 1] * xptr[xIndex + j + 1];
                    s2 -= lptr[lIndex + (j + 1) * nc + 2] * xptr[xIndex + j + 1];
                    s3 -= lptr[lIndex + (j + 1) * nc + 3] * xptr[xIndex + j + 1];
                    s0 -= lptr[lIndex + (j + 2) * nc + 0] * xptr[xIndex + j + 2];
                    s1 -= lptr[lIndex + (j + 2) * nc + 1] * xptr[xIndex + j + 2];
                    s2 -= lptr[lIndex + (j + 2) * nc + 2] * xptr[xIndex + j + 2];
                    s3 -= lptr[lIndex + (j + 2) * nc + 3] * xptr[xIndex + j + 2];
                    s0 -= lptr[lIndex + (j + 3) * nc + 0] * xptr[xIndex + j + 3];
                    s1 -= lptr[lIndex + (j + 3) * nc + 1] * xptr[xIndex + j + 3];
                    s2 -= lptr[lIndex + (j + 3) * nc + 2] * xptr[xIndex + j + 3];
                    s3 -= lptr[lIndex + (j + 3) * nc + 3] * xptr[xIndex + j + 3];
                }
                // process left over of the 4 rows
                s0 -= lptr[lIndex + 0 - 1 * nc] * s3;
                s1 -= lptr[lIndex + 1 - 1 * nc] * s3;
                s2 -= lptr[lIndex + 2 - 1 * nc] * s3;
                s0 -= lptr[lIndex + 0 - 2 * nc] * s2;
                s1 -= lptr[lIndex + 1 - 2 * nc] * s2;
                s0 -= lptr[lIndex + 0 - 3 * nc] * s1;
                // store result
                xptr[xIndex - 4] = (float) s0;
                xptr[xIndex - 3] = (float) s1;
                xptr[xIndex - 2] = (float) s2;
                xptr[xIndex - 1] = (float) s3;
                // update pointers for next four rows
                lIndex -= 4 + 4 * nc;
                xIndex -= 4;
            }
            // process left over rows
            for (i--; i >= 0; i--) {
                s0 = b[i];
                lptr = L.oGet(0);
                lIndex = i;
                for (j = i + 1; j < n; j++) {
                    s0 -= lptr[lIndex + j * nc] * x[j];
                }
                x[i] = (float) s0;
            }

//#else
//
//	int i, j, nc;
//	const float *ptr;
//	double sum;
//
//	nc = L.GetNumColumns();
//	for ( i = n - 1; i >= 0; i-- ) {
//		sum = b[i];
//		ptr = L[0] + i;
//		for ( j = i + 1; j < n; j++ ) {
//			sum -= ptr[j*nc] * x[j];
//		}
//		x[i] = sum;
//	}
//
//#endif
        }

        /*
         ============
         idSIMD_Generic::MatX_LDLTFactor

         in-place factorization LDL' of the n * n sub-matrix of mat
         the reciprocal of the diagonal elements are stored in invDiag
         ============
         */
        @Override
        public boolean MatX_LDLTFactor(idMatX mat, idVecX invDiag, int n) {
//#if 1

            int i, j, k, nc;
            float[] v, diag, mptr;
            float s0, s1, s2, s3, sum, d;
            int mIndex = 0;

            v = new float[n];
            diag = new float[n];

            nc = mat.GetNumColumns();

            if (n <= 0) {
                return true;
            }

            mptr = mat.oGet(0);

            sum = mptr[0];

            if (sum == 0.0f) {
                return false;
            }

            diag[0] = sum;
            invDiag.p[0] = (d = 1.0f / sum);

            if (n <= 1) {
                return true;
            }

            mptr = mat.oGet(0);
            for (j = 1; j < n; j++) {
                mptr[j * nc + 0] = ((mptr[j * nc + 0]) * d);
            }

            mptr = mat.oGet(1);

            v[0] = diag[0] * mptr[0];
            s0 = v[0] * mptr[0];
            sum = mptr[1] - s0;

            if (sum == 0.0f) {
                return false;
            }

            mat.oSet(1, 1, sum);
            diag[1] = sum;
            invDiag.p[1] = d = 1.0f / sum;

            if (n <= 2) {
                return true;
            }

            mptr = mat.oGet(0);
            for (j = 2; j < n; j++) {
                mptr[j * nc + 1] = (mptr[j * nc + 1] - v[0] * mptr[j * nc + 0]) * d;
            }

            mptr = mat.oGet(2);

            v[0] = diag[0] * mptr[0];
            s0 = v[0] * mptr[0];
            v[1] = diag[1] * mptr[1];
            s1 = v[1] * mptr[1];
            sum = mptr[2] - s0 - s1;

            if (sum == 0.0f) {
                return false;
            }

            mat.oSet(2, 2, sum);
            diag[2] = sum;
            invDiag.p[2] = d = 1.0f / sum;

            if (n <= 3) {
                return true;
            }

            mptr = mat.oGet(0);
            for (j = 3; j < n; j++) {
                mptr[j * nc + 2] = (mptr[j * nc + 2] - v[0] * mptr[j * nc + 0] - v[1] * mptr[j * nc + 1]) * d;
            }

            mptr = mat.oGet(3);

            v[0] = diag[0] * mptr[0];
            s0 = v[0] * mptr[0];
            v[1] = diag[1] * mptr[1];
            s1 = v[1] * mptr[1];
            v[2] = diag[2] * mptr[2];
            s2 = v[2] * mptr[2];
            sum = mptr[3] - s0 - s1 - s2;

            if (sum == 0.0f) {
                return false;
            }

            mat.oSet(3, 3, sum);
            diag[3] = sum;
            invDiag.p[3] = d = 1.0f / sum;

            if (n <= 4) {
                return true;
            }

            mptr = mat.oGet(0);
            for (j = 4; j < n; j++) {
                mptr[j * nc + 3] = (mptr[j * nc + 3] - v[0] * mptr[j * nc + 0] - v[1] * mptr[j * nc + 1] - v[2] * mptr[j * nc + 2]) * d;
            }

            for (i = 4; i < n; i++) {

                mptr = mat.oGet(i);

                v[0] = diag[0] * mptr[0];
                s0 = v[0] * mptr[0];
                v[1] = diag[1] * mptr[1];
                s1 = v[1] * mptr[1];
                v[2] = diag[2] * mptr[2];
                s2 = v[2] * mptr[2];
                v[3] = diag[3] * mptr[3];
                s3 = v[3] * mptr[3];
                for (k = 4; k < i - 3; k += 4) {
                    v[k + 0] = diag[k + 0] * mptr[k + 0];
                    s0 += v[k + 0] * mptr[k + 0];
                    v[k + 1] = diag[k + 1] * mptr[k + 1];
                    s1 += v[k + 1] * mptr[k + 1];
                    v[k + 2] = diag[k + 2] * mptr[k + 2];
                    s2 += v[k + 2] * mptr[k + 2];
                    v[k + 3] = diag[k + 3] * mptr[k + 3];
                    s3 += v[k + 3] * mptr[k + 3];
                }
                switch (i - k) {
//			NODEFAULT;
                    case 3:
                        v[k + 2] = diag[k + 2] * mptr[k + 2];
                        s0 += v[k + 2] * mptr[k + 2];
                    case 2:
                        v[k + 1] = diag[k + 1] * mptr[k + 1];
                        s1 += v[k + 1] * mptr[k + 1];
                    case 1:
                        v[k + 0] = diag[k + 0] * mptr[k + 0];
                        s2 += v[k + 0] * mptr[k + 0];
                    case 0:
                        break;
                }
                sum = s3;
                sum += s2;
                sum += s1;
                sum += s0;
                sum = mptr[i] - sum;

                if (sum == 0.0f) {
                    return false;
                }

                mat.oSet(i, i, sum);
                diag[i] = sum;
                invDiag.p[i] = d = 1.0f / sum;

                if (i + 1 >= n) {
                    return true;
                }

                mptr = mat.oGet(i + 1);
                for (j = i + 1; j < n; j++) {
                    s0 = mptr[mIndex + 0] * v[0];
                    s1 = mptr[mIndex + 1] * v[1];
                    s2 = mptr[mIndex + 2] * v[2];
                    s3 = mptr[mIndex + 3] * v[3];
                    for (k = 4; k < i - 7; k += 8) {
                        s0 += mptr[mIndex + k + 0] * v[k + 0];
                        s1 += mptr[mIndex + k + 1] * v[k + 1];
                        s2 += mptr[mIndex + k + 2] * v[k + 2];
                        s3 += mptr[mIndex + k + 3] * v[k + 3];
                        s0 += mptr[mIndex + k + 4] * v[k + 4];
                        s1 += mptr[mIndex + k + 5] * v[k + 5];
                        s2 += mptr[mIndex + k + 6] * v[k + 6];
                        s3 += mptr[mIndex + k + 7] * v[k + 7];
                    }
                    switch (i - k) {
//				NODEFAULT;
                        case 7:
                            s0 += mptr[mIndex + k + 6] * v[k + 6];
                        case 6:
                            s1 += mptr[mIndex + k + 5] * v[k + 5];
                        case 5:
                            s2 += mptr[mIndex + k + 4] * v[k + 4];
                        case 4:
                            s3 += mptr[mIndex + k + 3] * v[k + 3];
                        case 3:
                            s0 += mptr[mIndex + k + 2] * v[k + 2];
                        case 2:
                            s1 += mptr[mIndex + k + 1] * v[k + 1];
                        case 1:
                            s2 += mptr[mIndex + k + 0] * v[k + 0];
                        case 0:
                            break;
                    }
                    sum = s3;
                    sum += s2;
                    sum += s1;
                    sum += s0;
                    mptr[mIndex + i] = (mptr[mIndex + i] - sum) * d;
                    mIndex += nc;
                }
            }

            return true;

//#else
//
//	int i, j, k, nc;
//	float *v, *ptr, *diagPtr;
//	double d, sum;
//
//	v = (float *) _alloca16( n * sizeof( float ) );
//	nc = mat.GetNumColumns();
//
//	for ( i = 0; i < n; i++ ) {
//
//		ptr = mat[i];
//		diagPtr = mat[0];
//		sum = ptr[i];
//		for ( j = 0; j < i; j++ ) {
//			d = ptr[j];
//		    v[j] = diagPtr[0] * d;
//		    sum -= v[j] * d;
//			diagPtr += nc + 1;
//		}
//
//		if ( sum == 0.0f ) {
//			return false;
//		}
//
//		diagPtr[0] = sum;
//		invDiag[i] = d = 1.0f / sum;
//
//		if ( i + 1 >= n ) {
//			continue;
//		}
//
//		ptr = mat[i+1];
//		for ( j = i + 1; j < n; j++ ) {
//			sum = ptr[i];
//			for ( k = 0; k < i; k++ ) {
//				sum -= ptr[k] * v[k];
//			}
//			ptr[i] = sum * d;
//			ptr += nc;
//		}
//	}
//
//	return true;
//
//#endif
        }

        @Override
        public void BlendJoints(idJointQuat[] joints, idJointQuat[] blendJoints, float lerp, int[] index, int numJoints) {
            int i;

            for (i = 0; i < numJoints; i++) {
                int j = index[i];
                joints[j].q.Slerp(joints[j].q, blendJoints[j].q, lerp);
                joints[j].t.Lerp(joints[j].t, blendJoints[j].t, lerp);
            }
        }

        @Override
        public void ConvertJointQuatsToJointMats(idJointMat[] jointMats, idJointQuat[] jointQuats, int numJoints) {
            int i;

            for (i = 0; i < numJoints; i++) {
                jointMats[i].SetRotation(jointQuats[i].q.ToMat3());
                jointMats[i].SetTranslation(jointQuats[i].t);
            }
        }

        @Override
        public void ConvertJointMatsToJointQuats(idJointQuat[] jointQuats, idJointMat[] jointMats, int numJoints) {
            int i;

            for (i = 0; i < numJoints; i++) {
                jointQuats[i] = jointMats[i].ToJointQuat();
            }
        }

        @Override
        public void TransformJoints(idJointMat[] jointMats, int[] parents, int firstJoint, int lastJoint) {
            int i;

            for (i = firstJoint; i <= lastJoint; i++) {
                assert (parents[i] < i);
                jointMats[i].oMulSet(jointMats[parents[i]]);
            }
        }

        @Override
        public void UntransformJoints(idJointMat[] jointMats, int[] parents, int firstJoint, int lastJoint) {
            int i;

            for (i = lastJoint; i >= firstJoint; i--) {
                assert (parents[i] < i);
                jointMats[i].oDivSet(jointMats[parents[i]]);
            }
        }

        @Override
        public void TransformVerts(idDrawVert[] verts, int numVerts, idJointMat[] joints, idVec4[] weights, int[] index, int numWeights) {
            int i, j;
//	byte jointsPtr = (byte *)joints;

            for (j = i = 0; i < numVerts; i++) {
                idVec3 v;

                v = joints[index[j * 2 + 0]].oMultiply(weights[j]);//TODO:check if this equals to the byte pointer
                while (index[j * 2 + 1] == 0) {
                    j++;
                    v.oPluSet(joints[index[j * 2 + 0]].oMultiply(weights[j]));
                }
                j++;

                verts[i].xyz = v;
            }
        }

        @Override
        public void TracePointCull(byte[] cullBits, byte[] totalOr, float radius, idPlane[] planes, idDrawVert[] verts, int numVerts) {
            int i;
            byte tOr;

            tOr = 0;

            for (i = 0; i < numVerts; i++) {
                int bits;
                float d0, d1, d2, d3, t;
                final idVec3 v = verts[i].xyz;

                d0 = planes[0].Distance(v);
                d1 = planes[1].Distance(v);
                d2 = planes[2].Distance(v);
                d3 = planes[3].Distance(v);

                t = d0 + radius;
                bits = FLOATSIGNBITSET(t) << 0;
                t = d1 + radius;
                bits |= FLOATSIGNBITSET(t) << 1;
                t = d2 + radius;
                bits |= FLOATSIGNBITSET(t) << 2;
                t = d3 + radius;
                bits |= FLOATSIGNBITSET(t) << 3;

                t = d0 - radius;
                bits |= FLOATSIGNBITSET(t) << 4;
                t = d1 - radius;
                bits |= FLOATSIGNBITSET(t) << 5;
                t = d2 - radius;
                bits |= FLOATSIGNBITSET(t) << 6;
                t = d3 - radius;
                bits |= FLOATSIGNBITSET(t) << 7;

                bits ^= 0x0F;		// flip lower four bits

                tOr |= bits;
                cullBits[i] = (byte) bits;
            }

            totalOr[0] = tOr;
        }

        @Override
        public void DecalPointCull(byte[] cullBits, idPlane[] planes, idDrawVert[] verts, int numVerts) {
            int i;

            for (i = 0; i < numVerts; i++) {
                int bits;
                float d0, d1, d2, d3, d4, d5;
                final idVec3 v = verts[i].xyz;

                d0 = planes[0].Distance(v);
                d1 = planes[1].Distance(v);
                d2 = planes[2].Distance(v);
                d3 = planes[3].Distance(v);
                d4 = planes[4].Distance(v);
                d5 = planes[5].Distance(v);

                bits = FLOATSIGNBITSET(d0) << 0;
                bits |= FLOATSIGNBITSET(d1) << 1;
                bits |= FLOATSIGNBITSET(d2) << 2;
                bits |= FLOATSIGNBITSET(d3) << 3;
                bits |= FLOATSIGNBITSET(d4) << 4;
                bits |= FLOATSIGNBITSET(d5) << 5;

                cullBits[i] = (byte) (bits ^ 0x3F);		// flip lower 6 bits
            }
        }

        @Override
        public void OverlayPointCull(byte[] cullBits, idVec2[] texCoords, idPlane[] planes, idDrawVert[] verts, int numVerts) {
            int i;

            for (i = 0; i < numVerts; i++) {
                int bits;
                float d0, d1;
                final idVec3 v = verts[i].xyz;

                texCoords[i].oSet(0, d0 = planes[0].Distance(v));
                texCoords[i].oSet(1, d1 = planes[1].Distance(v));

                bits = FLOATSIGNBITSET(d0) << 0;
                d0 = 1.0f - d0;
                bits |= FLOATSIGNBITSET(d1) << 1;
                d1 = 1.0f - d1;
                bits |= FLOATSIGNBITSET(d0) << 2;
                bits |= FLOATSIGNBITSET(d1) << 3;

                cullBits[i] = (byte) bits;
            }
        }

        /*
         ============
         idSIMD_Generic::DeriveTriPlanes

         Derives a plane equation for each triangle.
         ============
         */
        @Override
        public void DeriveTriPlanes(idPlane[] planes, idDrawVert[] verts, int numVerts, int[] indexes, int numIndexes) {
            int i, planePtr;

            for (i = planePtr = 0; i < numIndexes; i += 3) {
                final idDrawVert a, b, c;
                float[] d0 = new float[3], d1 = new float[3];
                float f;
                idVec3 n;

                a = verts[indexes[i + 0]];
                b = verts[indexes[i + 1]];
                c = verts[indexes[i + 2]];

                d0[0] = b.xyz.oGet(0) - a.xyz.oGet(0);
                d0[1] = b.xyz.oGet(1) - a.xyz.oGet(1);
                d0[2] = b.xyz.oGet(2) - a.xyz.oGet(2);

                d1[0] = c.xyz.oGet(0) - a.xyz.oGet(0);
                d1[1] = c.xyz.oGet(1) - a.xyz.oGet(1);
                d1[2] = c.xyz.oGet(2) - a.xyz.oGet(2);

                n = new idVec3(
                        d1[1] * d0[2] - d1[2] * d0[1],
                        d1[2] * d0[0] - d1[0] * d0[2],
                        d1[0] * d0[1] - d1[1] * d0[0]);

                f = idMath.RSqrt(n.x * n.x + n.y * n.y + n.z * n.z);

                n.x *= f;
                n.y *= f;
                n.z *= f;

                planes[planePtr].SetNormal(n);
                planes[planePtr].FitThroughPoint(a.xyz);
                planePtr++;
            }
        }

        /*
         ============
         idSIMD_Generic::DeriveTangents

         Derives the normal and orthogonal tangent vectors for the triangle vertices.
         For each vertex the normal and tangent vectors are derived from all triangles
         using the vertex which results in smooth tangents across the mesh.
         In the process the triangle planes are calculated as well.
         ============
         */
        @Override
        public void DeriveTangents(idPlane[] planes, idDrawVert[] verts, int numVerts, int[] indexes, int numIndexes) {
            int i, planesPtr;

            boolean[] used = new boolean[numVerts];
//	memset( used, 0, numVerts * sizeof( used[0] ) );

            for (i = planesPtr = 0; i < numIndexes; i += 3) {
                idDrawVert a, b, c;
                int signBit;
                float[] d0 = new float[5], d1 = new float[5];
                float f, area;
                idVec3 n, t0 = new idVec3(), t1 = new idVec3();

                int v0 = indexes[i + 0];
                int v1 = indexes[i + 1];
                int v2 = indexes[i + 2];

                a = verts[v0];
                b = verts[v1];
                c = verts[v2];

                d0[0] = b.xyz.oGet(0) - a.xyz.oGet(0);
                d0[1] = b.xyz.oGet(1) - a.xyz.oGet(1);
                d0[2] = b.xyz.oGet(2) - a.xyz.oGet(2);
                d0[3] = b.st.oGet(0) - a.st.oGet(0);
                d0[4] = b.st.oGet(1) - a.st.oGet(1);

                d1[0] = c.xyz.oGet(0) - a.xyz.oGet(0);
                d1[1] = c.xyz.oGet(1) - a.xyz.oGet(1);
                d1[2] = c.xyz.oGet(2) - a.xyz.oGet(2);
                d1[3] = c.st.oGet(0) - a.st.oGet(0);
                d1[4] = c.st.oGet(1) - a.st.oGet(1);

                // normal
                n = new idVec3(
                        d1[1] * d0[2] - d1[2] * d0[1],
                        d1[2] * d0[0] - d1[0] * d0[2],
                        d1[0] * d0[1] - d1[1] * d0[0]);

                f = idMath.RSqrt(n.x * n.x + n.y * n.y + n.z * n.z);

                n.x *= f;
                n.y *= f;
                n.z *= f;

                planes[planesPtr].SetNormal(n);
                planes[planesPtr].FitThroughPoint(a.xyz);
                planesPtr++;

                // area sign bit
                area = d0[3] * d1[4] - d0[4] * d1[3];
                signBit = Float.floatToIntBits(area) & (1 << 31);

                // first tangent
                t0.oSet(0, d0[0] * d1[4] - d0[4] * d1[0]);
                t0.oSet(1, d0[1] * d1[4] - d0[4] * d1[1]);
                t0.oSet(2, d0[2] * d1[4] - d0[4] * d1[2]);

                f = idMath.RSqrt(t0.x * t0.x + t0.y * t0.y + t0.z * t0.z);
                f = Float.intBitsToFloat(Float.floatToIntBits(f) ^ signBit);

                t0.x *= f;
                t0.y *= f;
                t0.z *= f;

                // second tangent
                t1.oSet(0, d0[3] * d1[0] - d0[0] * d1[3]);
                t1.oSet(1, d0[3] * d1[1] - d0[1] * d1[3]);
                t1.oSet(2, d0[3] * d1[2] - d0[2] * d1[3]);

                f = idMath.RSqrt(t1.x * t1.x + t1.y * t1.y + t1.z * t1.z);
                f = Float.intBitsToFloat(Float.floatToIntBits(f) ^ signBit);

                t1.x *= f;
                t1.y *= f;
                t1.z *= f;

                if (used[v0]) {
                    a.normal.oPluSet(n);
                    a.tangents[0].oPluSet(t0);
                    a.tangents[1].oPluSet(t1);
                } else {
                    a.normal = n;
                    a.tangents[0] = t0;
                    a.tangents[1] = t1;
                    used[v0] = true;
                }

                if (used[v1]) {
                    b.normal.oPluSet(n);
                    b.tangents[0].oPluSet(t0);
                    b.tangents[1].oPluSet(t1);
                } else {
                    b.normal = n;
                    b.tangents[0] = t0;
                    b.tangents[1] = t1;
                    used[v1] = true;
                }

                if (used[v2]) {
                    c.normal.oPluSet(n);
                    c.tangents[0].oPluSet(t0);
                    c.tangents[1].oPluSet(t1);
                } else {
                    c.normal = n;
                    c.tangents[0] = t0;
                    c.tangents[1] = t1;
                    used[v2] = true;
                }
            }
        }

        @Override
        public void DeriveUnsmoothedTangents(idDrawVert[] verts, dominantTri_s[] dominantTris, int numVerts) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /*
         ============
         idSIMD_Generic::NormalizeTangents

         Normalizes each vertex normal and projects and normalizes the
         tangent vectors onto the plane orthogonal to the vertex normal.
         ============
         */
        @Override
        public void NormalizeTangents(idDrawVert[] verts, int numVerts) {

            for (int i = 0; i < numVerts; i++) {
                idVec3 v = verts[i].normal;
                float f;

                f = idMath.RSqrt(v.x * v.x + v.y * v.y + v.z * v.z);
                v.x *= f;
                v.y *= f;
                v.z *= f;

                for (int j = 0; j < 2; j++) {
                    idVec3 t = verts[i].tangents[j];

                    t.oMinSet(v.oMultiply(t.oMultiply(v)));
                    f = idMath.RSqrt(t.x * t.x + t.y * t.y + t.z * t.z);
                    t.x *= f;
                    t.y *= f;
                    t.z *= f;
                }
            }
        }

        /*
         ============
         idSIMD_Generic::CreateTextureSpaceLightVectors

         Calculates light vectors in texture space for the given triangle vertices.
         For each vertex the direction towards the light origin is projected onto texture space.
         The light vectors are only calculated for the vertices referenced by the indexes.
         ============
         */
        @Override
        public void CreateTextureSpaceLightVectors(idVec3[] lightVectors, idVec3 lightOrigin, idDrawVert[] verts, int numVerts, int[] indexes, int numIndexes) {

            boolean[] used = new boolean[numVerts];
//	memset( used, 0, numVerts * sizeof( used[0] ) );

            for (int i = numIndexes - 1; i >= 0; i--) {
                used[indexes[i]] = true;
            }

            for (int i = 0; i < numVerts; i++) {
                if (!used[i]) {
                    continue;
                }

                final idDrawVert v = verts[i];

                idVec3 lightDir = lightOrigin.oMinus(v.xyz);

                lightVectors[i].oSet(0, lightDir.oMultiply(v.tangents[0]));
                lightVectors[i].oSet(1, lightDir.oMultiply(v.tangents[1]));
                lightVectors[i].oSet(2, lightDir.oMultiply(v.normal));
            }
        }

        /*
         ============
         idSIMD_Generic::CreateSpecularTextureCoords

         Calculates specular texture coordinates for the given triangle vertices.
         For each vertex the normalized direction towards the light origin is added to the
         normalized direction towards the view origin and the result is projected onto texture space.
         The texture coordinates are only calculated for the vertices referenced by the indexes.
         ============
         */
        @Override
        public void CreateSpecularTextureCoords(idVec4[] texCoords, idVec3 lightOrigin, idVec3 viewOrigin, idDrawVert[] verts, int numVerts, int[] indexes, int numIndexes) {

            boolean[] used = new boolean[numVerts];
//	memset( used, 0, numVerts * sizeof( used[0] ) );

            for (int i = numIndexes - 1; i >= 0; i--) {
                used[indexes[i]] = true;
            }

            for (int i = 0; i < numVerts; i++) {
                if (!used[i]) {
                    continue;
                }

                final idDrawVert v = verts[i];

                idVec3 lightDir = lightOrigin.oMinus(v.xyz);
                idVec3 viewDir = viewOrigin.oMinus(v.xyz);

                float ilength;

                ilength = idMath.RSqrt(lightDir.oMultiply(lightDir));
                lightDir.oMulSet(ilength);

                ilength = idMath.RSqrt(viewDir.oMultiply(viewDir));
                viewDir.oMulSet(ilength);

                lightDir.oPluSet(viewDir);

                texCoords[i].oSet(0, lightDir.oMultiply(v.tangents[0]));
                texCoords[i].oSet(1, lightDir.oMultiply(v.tangents[1]));
                texCoords[i].oSet(2, lightDir.oMultiply(v.normal));
                texCoords[i].oSet(3, 1.0f);
            }
        }

        @Override
        public int CreateShadowCache(idVec4[] vertexCache, int[] vertRemap, idVec3 lightOrigin, idDrawVert[] verts, int numVerts) {
            int outVerts = 0;

            for (int i = 0; i < numVerts; i++) {
                if (vertRemap[i] != 0) {
                    continue;
                }
                final float[] v = verts[i].xyz.ToFloatPtr();
                vertexCache[outVerts + 0].oSet(0, v[0]);
                vertexCache[outVerts + 0].oSet(1, v[1]);
                vertexCache[outVerts + 0].oSet(2, v[2]);
                vertexCache[outVerts + 0].oSet(3, 1.0f);

                // R_SetupProjection() builds the projection matrix with a slight crunch
                // for depth, which keeps this w=0 division from rasterizing right at the
                // wrap around point and causing depth fighting with the rear caps
                vertexCache[outVerts + 1].oSet(0, v[0] - lightOrigin.oGet(0));
                vertexCache[outVerts + 1].oSet(1, v[1] - lightOrigin.oGet(1));
                vertexCache[outVerts + 1].oSet(2, v[2] - lightOrigin.oGet(2));
                vertexCache[outVerts + 1].oSet(3, 0.0f);
                vertRemap[i] = outVerts;
                outVerts += 2;
            }
            return outVerts;
        }

        @Override
        public int CreateVertexProgramShadowCache(idVec4[] vertexCache, idDrawVert[] verts, int numVerts) {
            for (int i = 0; i < numVerts; i++) {
                final float[] v = verts[i].xyz.ToFloatPtr();
                vertexCache[i * 2 + 0].oSet(0, v[0]);
                vertexCache[i * 2 + 1].oSet(0, v[0]);
                vertexCache[i * 2 + 0].oSet(1, v[1]);
                vertexCache[i * 2 + 1].oSet(1, v[1]);
                vertexCache[i * 2 + 0].oSet(2, v[2]);
                vertexCache[i * 2 + 1].oSet(2, v[2]);
                vertexCache[i * 2 + 0].oSet(3, 1.0f);
                vertexCache[i * 2 + 1].oSet(3, 0.0f);
            }
            return numVerts * 2;
        }

        /*
         ============
         idSIMD_Generic::UpSamplePCMTo44kHz

         Duplicate samples for 44kHz output.
         ============
         */
        @Override
        public void UpSamplePCMTo44kHz(float[] dest, short[] pcm, int numSamples, int kHz, int numChannels) {
            if (kHz == 11025) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest[i * 4 + 0] = dest[i * 4 + 1] = dest[i * 4 + 2] = dest[i * 4 + 3] = (float) pcm[i + 0];
                    }
                } else {
                    for (int i = 0; i < numSamples; i += 2) {
                        dest[i * 4 + 0] = dest[i * 4 + 2] = dest[i * 4 + 4] = dest[i * 4 + 6] = (float) pcm[i + 0];
                        dest[i * 4 + 1] = dest[i * 4 + 3] = dest[i * 4 + 5] = dest[i * 4 + 7] = (float) pcm[i + 1];
                    }
                }
            } else if (kHz == 22050) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest[i * 2 + 0] = dest[i * 2 + 1] = (float) pcm[i + 0];
                    }
                } else {
                    for (int i = 0; i < numSamples; i += 2) {
                        dest[i * 2 + 0] = dest[i * 2 + 2] = (float) pcm[i + 0];
                        dest[i * 2 + 1] = dest[i * 2 + 3] = (float) pcm[i + 1];
                    }
                }
            } else if (kHz == 44100) {
                for (int i = 0; i < numSamples; i++) {
                    dest[i] = (float) pcm[i];
                }
            } else {
//		assert( 0 );
                assert (false);
            }
        }

        /*
         ============
         idSIMD_Generic::UpSampleOGGTo44kHz

         Duplicate samples for 44kHz output.
         ============
         */
        @Override
        public void UpSampleOGGTo44kHz(float[] dest, int offset, float[][] ogg, int numSamples, int kHz, int numChannels) {
            if (kHz == 11025) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest[offset + (i * 4 + 0)] = dest[offset + (i * 4 + 1)] = dest[offset + (i * 4 + 2)] = dest[offset + (i * 4 + 3)] = ogg[0][i] * 32768.0f;
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest[offset + (i * 8 + 0)] = dest[offset + (i * 8 + 2)] = dest[offset + (i * 8 + 4)] = dest[offset + (i * 8 + 6)] = ogg[0][i] * 32768.0f;
                        dest[offset + (i * 8 + 1)] = dest[offset + (i * 8 + 3)] = dest[offset + (i * 8 + 5)] = dest[offset + (i * 8 + 7)] = ogg[1][i] * 32768.0f;
                    }
                }
            } else if (kHz == 22050) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest[offset + (i * 2 + 0)] = dest[offset + (i * 2 + 1)] = ogg[0][i] * 32768.0f;
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest[offset + (i * 4 + 0)] = dest[offset + (i * 4 + 2)] = ogg[0][i] * 32768.0f;
                        dest[offset + (i * 4 + 1)] = dest[offset + (i * 4 + 3)] = ogg[1][i] * 32768.0f;
                    }
                }
            } else if (kHz == 44100) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest[offset + (i * 1 + 0)] = ogg[0][i] * 32768.0f;
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest[offset + (i * 2 + 0)] = ogg[0][i] * 32768.0f;
                        dest[offset + (i * 2 + 1)] = ogg[1][i] * 32768.0f;
                    }
                }
            } else {
                assert (false);
            }
        }
        
        @Override
        public void UpSampleOGGTo44kHz(FloatBuffer dest, int offset, float[][] ogg, int numSamples, int kHz, int numChannels) {
            offset += dest.position();
            if (kHz == 11025) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest.put(offset + (i * 4 + 0), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 4 + 1), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 4 + 2), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 4 + 3), ogg[0][i] * 32768.0f);
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest.put(offset + (i * 8 + 0), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 8 + 2), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 8 + 4), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 8 + 6), ogg[0][i] * 32768.0f);
                        dest.put(offset + (i * 8 + 1), ogg[1][i] * 32768.0f)
                            .put(offset + (i * 8 + 3), ogg[1][i] * 32768.0f)
                            .put(offset + (i * 8 + 5), ogg[1][i] * 32768.0f)
                            .put(offset + (i * 8 + 7), ogg[1][i] * 32768.0f);
                    }
                }
            } else if (kHz == 22050) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest.put(offset + (i * 2 + 0), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 2 + 1), ogg[0][i] * 32768.0f);
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest.put(offset + (i * 4 + 0), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 4 + 2), ogg[0][i] * 32768.0f);
                        dest.put(offset + (i * 4 + 1), ogg[1][i] * 32768.0f)
                            .put(offset + (i * 4 + 3), ogg[1][i] * 32768.0f);
                    }
                }
            } else if (kHz == 44100) {
                if (numChannels == 1) {
                    for (int i = 0; i < numSamples; i++) {
                        dest.put(offset + (i * 1 + 0), ogg[0][i] * 32768.0f);
                    }
                } else {
                    for (int i = 0; i < numSamples >> 1; i++) {
                        dest.put(offset + (i * 2 + 0), ogg[0][i] * 32768.0f)
                            .put(offset + (i * 2 + 1), ogg[1][i] * 32768.0f);
                    }
                }
            } else {
                assert (false);
            }
        }

        @Override
        public void MixSoundTwoSpeakerMono(float[] mixBuffer, float[] samples, int numSamples, float[] lastV, float[] currentV) {
            float sL = lastV[0];
            float sR = lastV[1];
            float incL = (currentV[0] - lastV[0]) / MIXBUFFER_SAMPLES;
            float incR = (currentV[1] - lastV[1]) / MIXBUFFER_SAMPLES;

            assert (numSamples == MIXBUFFER_SAMPLES);

            for (int j = 0; j < MIXBUFFER_SAMPLES; j++) {
                mixBuffer[j * 2 + 0] += samples[j] * sL;
                mixBuffer[j * 2 + 1] += samples[j] * sR;
                sL += incL;
                sR += incR;
            }
        }

        @Override
        public void MixSoundTwoSpeakerStereo(float[] mixBuffer, float[] samples, int numSamples, float[] lastV, float[] currentV) {
            float sL = lastV[0];
            float sR = lastV[1];
            float incL = (currentV[0] - lastV[0]) / MIXBUFFER_SAMPLES;
            float incR = (currentV[1] - lastV[1]) / MIXBUFFER_SAMPLES;

            assert (numSamples == MIXBUFFER_SAMPLES);

            for (int j = 0; j < MIXBUFFER_SAMPLES; j++) {
                mixBuffer[j * 2 + 0] += samples[j * 2 + 0] * sL;
                mixBuffer[j * 2 + 1] += samples[j * 2 + 1] * sR;
                sL += incL;
                sR += incR;
            }
        }

        @Override
        public void MixSoundSixSpeakerMono(float[] mixBuffer, float[] samples, int numSamples, float[] lastV, float[] currentV) {
            float sL0 = lastV[0];
            float sL1 = lastV[1];
            float sL2 = lastV[2];
            float sL3 = lastV[3];
            float sL4 = lastV[4];
            float sL5 = lastV[5];

            float incL0 = (currentV[0] - lastV[0]) / MIXBUFFER_SAMPLES;
            float incL1 = (currentV[1] - lastV[1]) / MIXBUFFER_SAMPLES;
            float incL2 = (currentV[2] - lastV[2]) / MIXBUFFER_SAMPLES;
            float incL3 = (currentV[3] - lastV[3]) / MIXBUFFER_SAMPLES;
            float incL4 = (currentV[4] - lastV[4]) / MIXBUFFER_SAMPLES;
            float incL5 = (currentV[5] - lastV[5]) / MIXBUFFER_SAMPLES;

            assert (numSamples == MIXBUFFER_SAMPLES);

            for (int i = 0; i < MIXBUFFER_SAMPLES; i++) {
                mixBuffer[i * 6 + 0] += samples[i] * sL0;
                mixBuffer[i * 6 + 1] += samples[i] * sL1;
                mixBuffer[i * 6 + 2] += samples[i] * sL2;
                mixBuffer[i * 6 + 3] += samples[i] * sL3;
                mixBuffer[i * 6 + 4] += samples[i] * sL4;
                mixBuffer[i * 6 + 5] += samples[i] * sL5;
                sL0 += incL0;
                sL1 += incL1;
                sL2 += incL2;
                sL3 += incL3;
                sL4 += incL4;
                sL5 += incL5;
            }
        }

        @Override
        public void MixSoundSixSpeakerStereo(float[] mixBuffer, float[] samples, int numSamples, float[] lastV, float[] currentV) {
            float sL0 = lastV[0];
            float sL1 = lastV[1];
            float sL2 = lastV[2];
            float sL3 = lastV[3];
            float sL4 = lastV[4];
            float sL5 = lastV[5];

            float incL0 = (currentV[0] - lastV[0]) / MIXBUFFER_SAMPLES;
            float incL1 = (currentV[1] - lastV[1]) / MIXBUFFER_SAMPLES;
            float incL2 = (currentV[2] - lastV[2]) / MIXBUFFER_SAMPLES;
            float incL3 = (currentV[3] - lastV[3]) / MIXBUFFER_SAMPLES;
            float incL4 = (currentV[4] - lastV[4]) / MIXBUFFER_SAMPLES;
            float incL5 = (currentV[5] - lastV[5]) / MIXBUFFER_SAMPLES;

            assert (numSamples == MIXBUFFER_SAMPLES);

            for (int i = 0; i < MIXBUFFER_SAMPLES; i++) {
                mixBuffer[i * 6 + 0] += samples[i * 2 + 0] * sL0;
                mixBuffer[i * 6 + 1] += samples[i * 2 + 1] * sL1;
                mixBuffer[i * 6 + 2] += samples[i * 2 + 0] * sL2;
                mixBuffer[i * 6 + 3] += samples[i * 2 + 0] * sL3;
                mixBuffer[i * 6 + 4] += samples[i * 2 + 0] * sL4;
                mixBuffer[i * 6 + 5] += samples[i * 2 + 1] * sL5;
                sL0 += incL0;
                sL1 += incL1;
                sL2 += incL2;
                sL3 += incL3;
                sL4 += incL4;
                sL5 += incL5;
            }
        }

        @Override
        public void MixedSoundToSamples(short[] samples, int offset, float[] mixBuffer, int numSamples) {

            for (int i = 0; i < numSamples; i++) {
                if (mixBuffer[i] <= -32768.0f) {
                    samples[offset + i] = -32768;
                } else if (mixBuffer[i] >= 32767.0f) {
                    samples[offset + i] = 32767;
                } else {
                    samples[offset + i] = (short) mixBuffer[i];
                }
            }
        }
    };
}
