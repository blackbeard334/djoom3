package neo.idlib.math;

import neo.idlib.math.Simd_MMX.idSIMD_MMX;

/**
 * TODO:these functions can be converted.
 */
@Deprecated
public class Simd_SSE {

    static class idSIMD_SSE extends idSIMD_MMX {
//        
///*
//============
//SSE_Sin
//============
//*/
//float SSE_Sin( float a ) {
//#if 1
//
//	float t;
//
//	__asm {
//		movss		xmm1, a
//		movss		xmm2, xmm1
//		movss		xmm3, xmm1
//		mulss		xmm2, SIMD_SP_oneOverTwoPI
//		cvttss2si	ecx, xmm2
//		cmpltss		xmm3, SIMD_SP_zero
//		andps		xmm3, SIMD_SP_one
//		cvtsi2ss	xmm2, ecx
//		subss		xmm2, xmm3
//		mulss		xmm2, SIMD_SP_twoPI
//		subss		xmm1, xmm2
//
//		movss		xmm0, SIMD_SP_PI			// xmm0 = PI
//		subss		xmm0, xmm1					// xmm0 = PI - a
//		movss		xmm1, xmm0					// xmm1 = PI - a
//		andps		xmm1, SIMD_SP_signBitMask	// xmm1 = signbit( PI - a )
//		movss		xmm2, xmm0					// xmm2 = PI - a
//		xorps		xmm2, xmm1					// xmm2 = fabs( PI - a )
//		cmpnltss	xmm2, SIMD_SP_halfPI		// xmm2 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? 0xFFFFFFFF : 0x00000000
//		movss		xmm3, SIMD_SP_PI			// xmm3 = PI
//		xorps		xmm3, xmm1					// xmm3 = PI ^ signbit( PI - a )
//		andps		xmm3, xmm2					// xmm3 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? ( PI ^ signbit( PI - a ) ) : 0.0f
//		andps		xmm2, SIMD_SP_signBitMask	// xmm2 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? SIMD_SP_signBitMask : 0.0f
//		xorps		xmm0, xmm2
//		addps		xmm0, xmm3
//
//		movss		xmm1, xmm0
//		mulss		xmm1, xmm1
//		movss		xmm2, SIMD_SP_sin_c0
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_sin_c1
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_sin_c2
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_sin_c3
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_sin_c4
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_one
//		mulss		xmm2, xmm0
//		movss		t, xmm2
//	}
//
//	return t;
//
//#else
//
//	float s, t;
//
//	if ( ( a < 0.0f ) || ( a >= idMath::TWO_PI ) ) {
//		a -= floorf( a / idMath::TWO_PI ) * idMath::TWO_PI;
//	}
//
//	a = idMath::PI - a;
//	if ( fabs( a ) >= idMath::HALF_PI ) {
//		a = ( ( a < 0.0f ) ? -idMath::PI : idMath::PI ) - a;
//	}
//
//	s = a * a;
//	t = -2.39e-08f;
//	t *= s;
//	t += 2.7526e-06f;
//	t *= s;
//	t += -1.98409e-04f;
//	t *= s;
//	t += 8.3333315e-03f;
//	t *= s;
//	t += -1.666666664e-01f;
//	t *= s;
//	t += 1.0f;
//	t *= a;
//
//	return t;
//
//#endif
//}
//
//
///*
//============
//SSE_CosZeroHalfPI
//
//  The angle must be between zero and half PI.
//============
//*/
//float SSE_CosZeroHalfPI( float a ) {
//#if 1
//
//	float t;
//
//	assert( a >= 0.0f && a <= idMath::HALF_PI );
//
//	__asm {
//		movss		xmm0, a
//		mulss		xmm0, xmm0
//		movss		xmm1, SIMD_SP_cos_c0
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c1
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c2
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c3
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c4
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_one
//		movss		t, xmm1
//	}
//
//	return t;
//
//#else
//
//	float s, t;
//
//	assert( a >= 0.0f && a <= idMath::HALF_PI );
//
//	s = a * a;
//	t = -2.605e-07f;
//	t *= s;
//	t += 2.47609e-05f;
//	t *= s;
//	t += -1.3888397e-03f;
//	t *= s;
//	t += 4.16666418e-02f;
//	t *= s;
//	t += -4.999999963e-01f;
//	t *= s;
//	t += 1.0f;
//
//	return t;
//
//#endif
//}
//
//
//
///*
//============
//SSE_Cos
//============
//*/
//float SSE_Cos( float a ) {
//#if 1
//
//	float t;
//
//	__asm {
//		movss		xmm1, a
//		movss		xmm2, xmm1
//		movss		xmm3, xmm1
//		mulss		xmm2, SIMD_SP_oneOverTwoPI
//		cvttss2si	ecx, xmm2
//		cmpltss		xmm3, SIMD_SP_zero
//		andps		xmm3, SIMD_SP_one
//		cvtsi2ss	xmm2, ecx
//		subss		xmm2, xmm3
//		mulss		xmm2, SIMD_SP_twoPI
//		subss		xmm1, xmm2
//
//		movss		xmm0, SIMD_SP_PI			// xmm0 = PI
//		subss		xmm0, xmm1					// xmm0 = PI - a
//		movss		xmm1, xmm0					// xmm1 = PI - a
//		andps		xmm1, SIMD_SP_signBitMask	// xmm1 = signbit( PI - a )
//		movss		xmm2, xmm0					// xmm2 = PI - a
//		xorps		xmm2, xmm1					// xmm2 = fabs( PI - a )
//		cmpnltss	xmm2, SIMD_SP_halfPI		// xmm2 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? 0xFFFFFFFF : 0x00000000
//		movss		xmm3, SIMD_SP_PI			// xmm3 = PI
//		xorps		xmm3, xmm1					// xmm3 = PI ^ signbit( PI - a )
//		andps		xmm3, xmm2					// xmm3 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? ( PI ^ signbit( PI - a ) ) : 0.0f
//		andps		xmm2, SIMD_SP_signBitMask	// xmm2 = ( fabs( PI - a ) >= idMath::HALF_PI ) ? SIMD_SP_signBitMask : 0.0f
//		xorps		xmm0, xmm2
//		addps		xmm0, xmm3
//
//		mulss		xmm0, xmm0
//		movss		xmm1, SIMD_SP_cos_c0
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c1
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c2
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c3
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_cos_c4
//		mulss		xmm1, xmm0
//		addss		xmm1, SIMD_SP_one
//		xorps		xmm2, SIMD_SP_signBitMask
//		xorps		xmm1, xmm2
//		movss		t, xmm1
//	}
//
//	return t;
//
//#else
//
//	float s, t;
//
//	if ( ( a < 0.0f ) || ( a >= idMath::TWO_PI ) ) {
//		a -= floorf( a / idMath::TWO_PI ) * idMath::TWO_PI;
//	}
//
//	a = idMath::PI - a;
//	if ( fabs( a ) >= idMath::HALF_PI ) {
//		a = ( ( a < 0.0f ) ? -idMath::PI : idMath::PI ) - a;
//		d = 1.0f;
//	} else {
//		d = -1.0f;
//	}
//
//	s = a * a;
//	t = -2.605e-07f;
//	t *= s;
//	t += 2.47609e-05f;
//	t *= s;
//	t += -1.3888397e-03f;
//	t *= s;
//	t += 4.16666418e-02f;
//	t *= s;
//	t += -4.999999963e-01f;
//	t *= s;
//	t += 1.0f;
//	t *= d;
//
//	return t;
//
//#endif
//}
//
//
///*
//============
//SSE_ATanPositive
//
//  Both 'x' and 'y' must be positive.
//============
//*/
//float SSE_ATanPositive( float y, float x ) {
//#if 1
//
//	float t;
//
//	assert( y >= 0.0f && x >= 0.0f );
//
//	__asm {
//		movss		xmm0, x
//		movss		xmm3, xmm0
//		movss		xmm1, y
//		minss		xmm0, xmm1
//		maxss		xmm1, xmm3
//		cmpeqss		xmm3, xmm0
//		rcpss		xmm2, xmm1
//		mulss		xmm1, xmm2
//		mulss		xmm1, xmm2
//		addss		xmm2, xmm2
//		subss		xmm2, xmm1				// xmm2 = 1 / y or 1 / x
//		mulss		xmm0, xmm2				// xmm0 = x / y or y / x
//		movss		xmm1, xmm3
//		andps		xmm1, SIMD_SP_signBitMask
//		xorps		xmm0, xmm1				// xmm0 = -x / y or y / x
//		andps		xmm3, SIMD_SP_halfPI	// xmm3 = HALF_PI or 0.0f
//		movss		xmm1, xmm0
//		mulss		xmm1, xmm1				// xmm1 = s
//		movss		xmm2, SIMD_SP_atan_c0
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c1
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c2
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c3
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c4
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c5
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c6
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c7
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_one
//		mulss		xmm2, xmm0
//		addss		xmm2, xmm3
//		movss		t, xmm2
//	}
//
//	return t;
//
//#else
//
//	float a, d, s, t;
//
//	assert( y >= 0.0f && x >= 0.0f );
//
//	if ( y > x ) {
//		a = -x / y;
//		d = idMath::HALF_PI;
//	} else {
//		a = y / x;
//		d = 0.0f;
//	}
//	s = a * a;
//	t = 0.0028662257f;
//	t *= s;
//	t += -0.0161657367f;
//	t *= s;
//	t += 0.0429096138f;
//	t *= s;
//	t += -0.0752896400f;
//	t *= s;
//	t += 0.1065626393f;
//	t *= s;
//	t += -0.1420889944f;
//	t *= s;
//	t += 0.1999355085f;
//	t *= s;
//	t += -0.3333314528f;
//	t *= s;
//	t += 1.0f;
//	t *= a;
//	t += d;
//
//	return t;
//
//#endif
//}
//
//
//
///*
//============
//SSE_ATan
//============
//*/
//float SSE_ATan( float y, float x ) {
//#if 1
//
//	float t;
//
//	__asm {
//		movss		xmm0, x
//		movss		xmm3, xmm0
//		movss		xmm4, xmm0
//		andps		xmm0, SIMD_SP_absMask
//		movss		xmm1, y
//		xorps		xmm4, xmm1
//		andps		xmm1, SIMD_SP_absMask
//		andps		xmm4, SIMD_SP_signBitMask
//		minss		xmm0, xmm1
//		maxss		xmm1, xmm3
//		cmpeqss		xmm3, xmm0
//		rcpss		xmm2, xmm1
//		mulss		xmm1, xmm2
//		mulss		xmm1, xmm2
//		addss		xmm2, xmm2
//		subss		xmm2, xmm1				// xmm2 = 1 / y or 1 / x
//		mulss		xmm0, xmm2				// xmm0 = x / y or y / x
//		xorps		xmm0, xmm4
//		movss		xmm1, xmm3
//		andps		xmm1, SIMD_SP_signBitMask
//		xorps		xmm0, xmm1				// xmm0 = -x / y or y / x
//		orps		xmm4, SIMD_SP_halfPI	// xmm4 = +/- HALF_PI
//		andps		xmm3, xmm4				// xmm3 = +/- HALF_PI or 0.0f
//		movss		xmm1, xmm0
//		mulss		xmm1, xmm1				// xmm1 = s
//		movss		xmm2, SIMD_SP_atan_c0
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c1
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c2
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c3
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c4
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c5
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c6
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_atan_c7
//		mulss		xmm2, xmm1
//		addss		xmm2, SIMD_SP_one
//		mulss		xmm2, xmm0
//		addss		xmm2, xmm3
//		movss		t, xmm2
//	}
//
//	return t;
//
//#else
//
//	float a, d, s, t;
//
//	if ( fabs( y ) > fabs( x ) ) {
//		a = -x / y;
//		d = idMath::HALF_PI;
//		*((unsigned long *)&d) ^= ( *((unsigned long *)&x) ^ *((unsigned long *)&y) ) & (1<<31);
//	} else {
//		a = y / x;
//		d = 0.0f;
//	}
//
//	s = a * a;
//	t = 0.0028662257f;
//	t *= s;
//	t += -0.0161657367f;
//	t *= s;
//	t += 0.0429096138f;
//	t *= s;
//	t += -0.0752896400f;
//	t *= s;
//	t += 0.1065626393f;
//	t *= s;
//	t += -0.1420889944f;
//	t *= s;
//	t += 0.1999355085f;
//	t *= s;
//	t += -0.3333314528f;
//	t *= s;
//	t += 1.0f;
//	t *= a;
//	t += d;
//
//	return t;
//
//#endif
//}
//
//
///*
//============
//SSE_TestTrigonometry
//============
//*/
//void SSE_TestTrigonometry( void ) {
//	int i;
//	float a, s1, s2, c1, c2;
//
//	for ( i = 0; i < 100; i++ ) {
//		a = i * idMath::HALF_PI / 100.0f;
//
//		s1 = sin( a );
//		s2 = SSE_SinZeroHalfPI( a );
//
//		if ( fabs( s1 - s2 ) > 1e-7f ) {
//			assert( 0 );
//		}
//
//		c1 = cos( a );
//		c2 = SSE_CosZeroHalfPI( a );
//
//		if ( fabs( c1 - c2 ) > 1e-7f ) {
//			assert( 0 );
//		}
//	}
//
//	for ( i = -200; i < 200; i++ ) {
//		a = i * idMath::TWO_PI / 100.0f;
//
//		s1 = sin( a );
//		s2 = SSE_Sin( a );
//
//		if ( fabs( s1 - s2 ) > 1e-6f ) {
//			assert( 0 );
//		}
//
//		c1 = cos( a );
//		c2 = SSE_Cos( a );
//
//		if ( fabs( c1 - c2 ) > 1e-6f ) {
//			assert( 0 );
//		}
//
//		SSE_SinCos( a, s2, c2 );
//		if ( fabs( s1 - s2 ) > 1e-6f || fabs( c1 - c2 ) > 1e-6f ) {
//			assert( 0 );
//		}
//	}
//}
//
///*
//============
//idSIMD_SSE::GetName
//============
//*/
//const char * idSIMD_SSE::GetName( void ) const {
//	return "MMX & SSE";
//}
//
///*
//============
//idSIMD_SSE::Add
//
//  dst[i] = constant + src[i];
//============
//*/
//void VPCALL idSIMD_SSE::Add( float *dst, const float constant, const float *src, const int count ) {
//	KFLOAT_CA( add, dst, src, constant, count )
//}
//
///*
//============
//idSIMD_SSE::Add
//
//  dst[i] = src0[i] + src1[i];
//============
//*/
//void VPCALL idSIMD_SSE::Add( float *dst, const float *src0, const float *src1, const int count ) {
//	KFLOAT_AA( add, dst, src0, src1, count )
//}
//
///*
//============
//idSIMD_SSE::Sub
//
//  dst[i] = constant - src[i];
//============
//*/
//void VPCALL idSIMD_SSE::Sub( float *dst, const float constant, const float *src, const int count ) {
//	KFLOAT_CA( sub, dst, src, constant, count )
//}
//
///*
//============
//idSIMD_SSE::Sub
//
//  dst[i] = src0[i] - src1[i];
//============
//*/
//void VPCALL idSIMD_SSE::Sub( float *dst, const float *src0, const float *src1, const int count ) {
//	KFLOAT_AA( sub, dst, src0, src1, count )
//}
//
///*
//============
//idSIMD_SSE::Mul
//
//  dst[i] = constant * src[i];
//============
//*/
//void VPCALL idSIMD_SSE::Mul( float *dst, const float constant, const float *src, const int count ) {
//	KFLOAT_CA( mul, dst, src, constant, count )
//}
//
///*
//============
//idSIMD_SSE::Mul
//
//  dst[i] = src0[i] * src1[i];
//============
//*/
//void VPCALL idSIMD_SSE::Mul( float *dst, const float *src0, const float *src1, const int count ) {
//	KFLOAT_AA( mul, dst, src0, src1, count )
//}
//
//
///*
//============
//idSIMD_SSE::CmpGT
//
//  dst[i] = src0[i] > constant;
//============
//*/
//void VPCALL idSIMD_SSE::CmpGT( byte *dst, const float *src0, const float constant, const int count ) {
//	COMPARECONSTANT( dst, src0, constant, count, >, cmpnleps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpGT
//
//  dst[i] |= ( src0[i] > constant ) << bitNum;
//============
//*/
//void VPCALL idSIMD_SSE::CmpGT( byte *dst, const byte bitNum, const float *src0, const float constant, const int count ) {
//	COMPAREBITCONSTANT( dst, bitNum, src0, constant, count, >, cmpnleps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpGE
//
//  dst[i] = src0[i] >= constant;
//============
//*/
//void VPCALL idSIMD_SSE::CmpGE( byte *dst, const float *src0, const float constant, const int count ) {
//	COMPARECONSTANT( dst, src0, constant, count, >=, cmpnltps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpGE
//
//  dst[i] |= ( src0[i] >= constant ) << bitNum;
//============
//*/
//void VPCALL idSIMD_SSE::CmpGE( byte *dst, const byte bitNum, const float *src0, const float constant, const int count ) {
//	COMPAREBITCONSTANT( dst, bitNum, src0, constant, count, >=, cmpnltps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpLT
//
//  dst[i] = src0[i] < constant;
//============
//*/
//void VPCALL idSIMD_SSE::CmpLT( byte *dst, const float *src0, const float constant, const int count ) {
//	COMPARECONSTANT( dst, src0, constant, count, <, cmpltps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpLT
//
//  dst[i] |= ( src0[i] < constant ) << bitNum;
//============
//*/
//void VPCALL idSIMD_SSE::CmpLT( byte *dst, const byte bitNum, const float *src0, const float constant, const int count ) {
//	COMPAREBITCONSTANT( dst, bitNum, src0, constant, count, <, cmpltps, NOFLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpLE
//
//  dst[i] = src0[i] <= constant;
//============
//*/
//void VPCALL idSIMD_SSE::CmpLE( byte *dst, const float *src0, const float constant, const int count ) {
//	COMPARECONSTANT( dst, src0, constant, count, <=, cmpnleps, FLIP )
//}
//
///*
//============
//idSIMD_SSE::CmpLE
//
//  dst[i] |= ( src0[i] <= constant ) << bitNum;
//============
//*/
//void VPCALL idSIMD_SSE::CmpLE( byte *dst, const byte bitNum, const float *src0, const float constant, const int count ) {
//	COMPAREBITCONSTANT( dst, bitNum, src0, constant, count, <=, cmpnleps, FLIP )
//}
//
//
//
///*
//============
//idSIMD_SSE::MatX_LowerTriangularSolveTranspose
//
//  solves x in L'x = b for the n * n sub-matrix of L
//  L has to be a lower triangular matrix with (implicit) ones on the diagonal
//  x == b is allowed
//============
//*/
//void VPCALL idSIMD_SSE::MatX_LowerTriangularSolveTranspose( const idMatX &L, float *x, const float *b, const int n ) {
//	int nc;
//	const float *lptr;
//
//	lptr = L.ToFloatPtr();
//	nc = L.GetNumColumns();
//
//	// unrolled cases for n < 8
//	if ( n < 8 ) {
//		switch( n ) {
//			case 0:
//				return;
//			case 1:
//				x[0] = b[0];
//				return;
//			case 2:
//				x[1] = b[1];
//				x[0] = b[0] - lptr[1*nc+0] * x[1];
//				return;
//			case 3:
//				x[2] = b[2];
//				x[1] = b[1] - lptr[2*nc+1] * x[2];
//				x[0] = b[0] - lptr[2*nc+0] * x[2] - lptr[1*nc+0] * x[1];
//				return;
//			case 4:
//				x[3] = b[3];
//				x[2] = b[2] - lptr[3*nc+2] * x[3];
//				x[1] = b[1] - lptr[3*nc+1] * x[3] - lptr[2*nc+1] * x[2];
//				x[0] = b[0] - lptr[3*nc+0] * x[3] - lptr[2*nc+0] * x[2] - lptr[1*nc+0] * x[1];
//				return;
//			case 5:
//				x[4] = b[4];
//				x[3] = b[3] - lptr[4*nc+3] * x[4];
//				x[2] = b[2] - lptr[4*nc+2] * x[4] - lptr[3*nc+2] * x[3];
//				x[1] = b[1] - lptr[4*nc+1] * x[4] - lptr[3*nc+1] * x[3] - lptr[2*nc+1] * x[2];
//				x[0] = b[0] - lptr[4*nc+0] * x[4] - lptr[3*nc+0] * x[3] - lptr[2*nc+0] * x[2] - lptr[1*nc+0] * x[1];
//				return;
//			case 6:
//				x[5] = b[5];
//				x[4] = b[4] - lptr[5*nc+4] * x[5];
//				x[3] = b[3] - lptr[5*nc+3] * x[5] - lptr[4*nc+3] * x[4];
//				x[2] = b[2] - lptr[5*nc+2] * x[5] - lptr[4*nc+2] * x[4] - lptr[3*nc+2] * x[3];
//				x[1] = b[1] - lptr[5*nc+1] * x[5] - lptr[4*nc+1] * x[4] - lptr[3*nc+1] * x[3] - lptr[2*nc+1] * x[2];
//				x[0] = b[0] - lptr[5*nc+0] * x[5] - lptr[4*nc+0] * x[4] - lptr[3*nc+0] * x[3] - lptr[2*nc+0] * x[2] - lptr[1*nc+0] * x[1];
//				return;
//			case 7:
//				x[6] = b[6];
//				x[5] = b[5] - lptr[6*nc+5] * x[6];
//				x[4] = b[4] - lptr[6*nc+4] * x[6] - lptr[5*nc+4] * x[5];
//				x[3] = b[3] - lptr[6*nc+3] * x[6] - lptr[5*nc+3] * x[5] - lptr[4*nc+3] * x[4];
//				x[2] = b[2] - lptr[6*nc+2] * x[6] - lptr[5*nc+2] * x[5] - lptr[4*nc+2] * x[4] - lptr[3*nc+2] * x[3];
//				x[1] = b[1] - lptr[6*nc+1] * x[6] - lptr[5*nc+1] * x[5] - lptr[4*nc+1] * x[4] - lptr[3*nc+1] * x[3] - lptr[2*nc+1] * x[2];
//				x[0] = b[0] - lptr[6*nc+0] * x[6] - lptr[5*nc+0] * x[5] - lptr[4*nc+0] * x[4] - lptr[3*nc+0] * x[3] - lptr[2*nc+0] * x[2] - lptr[1*nc+0] * x[1];
//				return;
//		}
//		return;
//	}
//
//#if 1
//
//	int i, j, m;
//	float *xptr;
//	double s0;
//
//	// if the number of columns is not a multiple of 2 we're screwed for alignment.
//	// however, if the number of columns is a multiple of 2 but the number of to be
//	// processed rows is not a multiple of 2 we can still run 8 byte aligned
//	m = n;
//	if ( m & 1 ) {
//
//		m--;
//		x[m] = b[m];
//
//		lptr = L.ToFloatPtr() + m * nc + m - 4;
//		xptr = x + m;
//		__asm {
//			push		ebx
//			mov			eax, m					// eax = i
//			mov			esi, xptr				// esi = xptr
//			mov			edi, lptr				// edi = lptr
//			mov			ebx, b					// ebx = b
//			mov			edx, nc					// edx = nc*sizeof(float)
//			shl			edx, 2
//		process4rows_1:
//			movlps		xmm0, [ebx+eax*4-16]	// load b[i-2], b[i-1]
//			movhps		xmm0, [ebx+eax*4-8]		// load b[i-4], b[i-3]
//			xor			ecx, ecx
//			sub			eax, m
//			neg			eax
//			jz			done4x4_1
//		process4x4_1:	// process 4x4 blocks
//			movlps		xmm2, [edi+0]
//			movhps		xmm2, [edi+8]
//			add			edi, edx
//			movss		xmm1, [esi+4*ecx+0]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm3, [edi+0]
//			movhps		xmm3, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm2
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+4]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm4, [edi+0]
//			movhps		xmm4, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm3
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+8]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm5, [edi+0]
//			movhps		xmm5, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm4
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+12]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			add			ecx, 4
//			cmp			ecx, eax
//			mulps		xmm1, xmm5
//			subps		xmm0, xmm1
//			jl			process4x4_1
//		done4x4_1:		// process left over of the 4 rows
//			movlps		xmm2, [edi+0]
//			movhps		xmm2, [edi+8]
//			movss		xmm1, [esi+4*ecx]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			mulps		xmm1, xmm2
//			subps		xmm0, xmm1
//			imul		ecx, edx
//			sub			edi, ecx
//			neg			eax
//
//			add			eax, m
//			sub			eax, 4
//			movaps		xmm1, xmm0
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 1, 1, 1 )
//			movaps		xmm2, xmm0
//			shufps		xmm2, xmm2, R_SHUFFLEPS( 2, 2, 2, 2 )
//			movaps		xmm3, xmm0
//			shufps		xmm3, xmm3, R_SHUFFLEPS( 3, 3, 3, 3 )
//			sub			edi, edx
//			movss		[esi-4], xmm3			// xptr[-1] = s3
//			movss		xmm4, xmm3
//			movss		xmm5, xmm3
//			mulss		xmm3, [edi+8]			// lptr[-1*nc+2] * s3
//			mulss		xmm4, [edi+4]			// lptr[-1*nc+1] * s3
//			mulss		xmm5, [edi+0]			// lptr[-1*nc+0] * s3
//			subss		xmm2, xmm3
//			movss		[esi-8], xmm2			// xptr[-2] = s2
//			movss		xmm6, xmm2
//			sub			edi, edx
//			subss		xmm0, xmm5
//			subss		xmm1, xmm4
//			mulss		xmm2, [edi+4]			// lptr[-2*nc+1] * s2
//			mulss		xmm6, [edi+0]			// lptr[-2*nc+0] * s2
//			subss		xmm1, xmm2
//			movss		[esi-12], xmm1			// xptr[-3] = s1
//			subss		xmm0, xmm6
//			sub			edi, edx
//			cmp			eax, 4
//			mulss		xmm1, [edi+0]			// lptr[-3*nc+0] * s1
//			subss		xmm0, xmm1
//			movss		[esi-16], xmm0			// xptr[-4] = s0
//			jl			done4rows_1
//			sub			edi, edx
//			sub			edi, 16
//			sub			esi, 16
//			jmp			process4rows_1
//		done4rows_1:
//			pop			ebx
//		}
//
//	} else {
//
//		lptr = L.ToFloatPtr() + m * nc + m - 4;
//		xptr = x + m;
//		__asm {
//			push		ebx
//			mov			eax, m					// eax = i
//			mov			esi, xptr				// esi = xptr
//			mov			edi, lptr				// edi = lptr
//			mov			ebx, b					// ebx = b
//			mov			edx, nc					// edx = nc*sizeof(float)
//			shl			edx, 2
//		process4rows:
//			movlps		xmm0, [ebx+eax*4-16]	// load b[i-2], b[i-1]
//			movhps		xmm0, [ebx+eax*4-8]		// load b[i-4], b[i-3]
//			sub			eax, m
//			jz			done4x4
//			neg			eax
//			xor			ecx, ecx
//		process4x4:		// process 4x4 blocks
//			movlps		xmm2, [edi+0]
//			movhps		xmm2, [edi+8]
//			add			edi, edx
//			movss		xmm1, [esi+4*ecx+0]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm3, [edi+0]
//			movhps		xmm3, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm2
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+4]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm4, [edi+0]
//			movhps		xmm4, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm3
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+8]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movlps		xmm5, [edi+0]
//			movhps		xmm5, [edi+8]
//			add			edi, edx
//			mulps		xmm1, xmm4
//			subps		xmm0, xmm1
//			movss		xmm1, [esi+4*ecx+12]
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//			add			ecx, 4
//			cmp			ecx, eax
//			mulps		xmm1, xmm5
//			subps		xmm0, xmm1
//			jl			process4x4
//			imul		ecx, edx
//			sub			edi, ecx
//			neg			eax
//		done4x4:		// process left over of the 4 rows
//			add			eax, m
//			sub			eax, 4
//			movaps		xmm1, xmm0
//			shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 1, 1, 1 )
//			movaps		xmm2, xmm0
//			shufps		xmm2, xmm2, R_SHUFFLEPS( 2, 2, 2, 2 )
//			movaps		xmm3, xmm0
//			shufps		xmm3, xmm3, R_SHUFFLEPS( 3, 3, 3, 3 )
//			sub			edi, edx
//			movss		[esi-4], xmm3			// xptr[-1] = s3
//			movss		xmm4, xmm3
//			movss		xmm5, xmm3
//			mulss		xmm3, [edi+8]			// lptr[-1*nc+2] * s3
//			mulss		xmm4, [edi+4]			// lptr[-1*nc+1] * s3
//			mulss		xmm5, [edi+0]			// lptr[-1*nc+0] * s3
//			subss		xmm2, xmm3
//			movss		[esi-8], xmm2			// xptr[-2] = s2
//			movss		xmm6, xmm2
//			sub			edi, edx
//			subss		xmm0, xmm5
//			subss		xmm1, xmm4
//			mulss		xmm2, [edi+4]			// lptr[-2*nc+1] * s2
//			mulss		xmm6, [edi+0]			// lptr[-2*nc+0] * s2
//			subss		xmm1, xmm2
//			movss		[esi-12], xmm1			// xptr[-3] = s1
//			subss		xmm0, xmm6
//			sub			edi, edx
//			cmp			eax, 4
//			mulss		xmm1, [edi+0]			// lptr[-3*nc+0] * s1
//			subss		xmm0, xmm1
//			movss		[esi-16], xmm0			// xptr[-4] = s0
//			jl			done4rows
//			sub			edi, edx
//			sub			edi, 16
//			sub			esi, 16
//			jmp			process4rows
//		done4rows:
//			pop			ebx
//		}
//	}
//
//	// process left over rows
//	for ( i = (m&3)-1; i >= 0; i-- ) {
//		s0 = b[i];
//		lptr = L[0] + i;
//		for ( j = i + 1; j < n; j++ ) {
//			s0 -= lptr[j*nc] * x[j];
//		}
//		x[i] = s0;
//	}
//
//#else
//
//	int i, j, m;
//	double s0, s1, s2, s3, t;
//	const float *lptr2;
//	float *xptr, *xptr2;
//
//	m = n;
//	if ( m & 1 ) {
//
//		m--;
//		x[m] = b[m];
//
//		lptr = L.ToFloatPtr() + m * nc + m - 4;
//		xptr = x + m;
//		// process 4 rows at a time
//		for ( i = m; i >= 4; i -= 4 ) {
//			s0 = b[i-4];
//			s1 = b[i-3];
//			s2 = b[i-2];
//			s3 = b[i-1];
//			// process 4x4 blocks
//			xptr2 = xptr;	// x + i;
//			lptr2 = lptr;	// ptr = L[i] + i - 4;
//			for ( j = 0; j < m-i; j += 4 ) {
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//			}
//			t = xptr2[0];
//			s0 -= lptr2[0] * t;
//			s1 -= lptr2[1] * t;
//			s2 -= lptr2[2] * t;
//			s3 -= lptr2[3] * t;
//			// process left over of the 4 rows
//			lptr -= nc;
//			s0 -= lptr[0] * s3;
//			s1 -= lptr[1] * s3;
//			s2 -= lptr[2] * s3;
//			lptr -= nc;
//			s0 -= lptr[0] * s2;
//			s1 -= lptr[1] * s2;
//			lptr -= nc;
//			s0 -= lptr[0] * s1;
//			lptr -= nc;
//			// store result
//			xptr[-4] = s0;
//			xptr[-3] = s1;
//			xptr[-2] = s2;
//			xptr[-1] = s3;
//			// update pointers for next four rows
//			lptr -= 4;
//			xptr -= 4;
//		}
//
//	} else {
//
//		lptr = L.ToFloatPtr() + m * nc + m - 4;
//		xptr = x + m;
//		// process 4 rows at a time
//		for ( i = m; i >= 4; i -= 4 ) {
//			s0 = b[i-4];
//			s1 = b[i-3];
//			s2 = b[i-2];
//			s3 = b[i-1];
//			// process 4x4 blocks
//			xptr2 = xptr;	// x + i;
//			lptr2 = lptr;	// ptr = L[i] + i - 4;
//			for ( j = 0; j < m-i; j += 4 ) {
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//				t = xptr2[0];
//				s0 -= lptr2[0] * t;
//				s1 -= lptr2[1] * t;
//				s2 -= lptr2[2] * t;
//				s3 -= lptr2[3] * t;
//				lptr2 += nc;
//				xptr2++;
//			}
//			// process left over of the 4 rows
//			lptr -= nc;
//			s0 -= lptr[0] * s3;
//			s1 -= lptr[1] * s3;
//			s2 -= lptr[2] * s3;
//			lptr -= nc;
//			s0 -= lptr[0] * s2;
//			s1 -= lptr[1] * s2;
//			lptr -= nc;
//			s0 -= lptr[0] * s1;
//			lptr -= nc;
//			// store result
//			xptr[-4] = s0;
//			xptr[-3] = s1;
//			xptr[-2] = s2;
//			xptr[-1] = s3;
//			// update pointers for next four rows
//			lptr -= 4;
//			xptr -= 4;
//		}
//	}
//	// process left over rows
//	for ( i--; i >= 0; i-- ) {
//		s0 = b[i];
//		lptr = L[0] + i;
//		for ( j = i + 1; j < m; j++ ) {
//			s0 -= lptr[j*nc] * x[j];
//		}
//		x[i] = s0;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::MatX_LDLTFactor
//
//  in-place factorization LDL' of the n * n sub-matrix of mat
//  the reciprocal of the diagonal elements are stored in invDiag
//  currently assumes the number of columns of mat is a multiple of 4
//============
//*/
//bool VPCALL idSIMD_SSE::MatX_LDLTFactor( idMatX &mat, idVecX &invDiag, const int n ) {
//#if 1
//
//	int j, nc;
//	float *v, *diag, *invDiagPtr, *mptr;
//	double s0, s1, s2, sum, d;
//
//	v = (float *) _alloca16( n * sizeof( float ) );
//	diag = (float *) _alloca16( n * sizeof( float ) );
//	invDiagPtr = invDiag.ToFloatPtr();
//
//	nc = mat.GetNumColumns();
//
//	assert( ( nc & 3 ) == 0 );
//
//	if ( n <= 0 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//
//	sum = mptr[0];
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	diag[0] = sum;
//	invDiagPtr[0] = d = 1.0f / sum;
//
//	if ( n <= 1 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 1; j < n; j++ ) {
//		mptr[j*nc+0] = ( mptr[j*nc+0] ) * d;
//	}
//
//	mptr = mat[1];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	sum = mptr[1] - s0;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[1][1] = sum;
//	diag[1] = sum;
//	invDiagPtr[1] = d = 1.0f / sum;
//
//	if ( n <= 2 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 2; j < n; j++ ) {
//		mptr[j*nc+1] = ( mptr[j*nc+1] - v[0] * mptr[j*nc+0] ) * d;
//	}
//
//	mptr = mat[2];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	v[1] = diag[1] * mptr[1]; s1 = v[1] * mptr[1];
//	sum = mptr[2] - s0 - s1;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[2][2] = sum;
//	diag[2] = sum;
//	invDiagPtr[2] = d = 1.0f / sum;
//
//	if ( n <= 3 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 3; j < n; j++ ) {
//		mptr[j*nc+2] = ( mptr[j*nc+2] - v[0] * mptr[j*nc+0] - v[1] * mptr[j*nc+1] ) * d;
//	}
//
//	mptr = mat[3];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	v[1] = diag[1] * mptr[1]; s1 = v[1] * mptr[1];
//	v[2] = diag[2] * mptr[2]; s2 = v[2] * mptr[2];
//	sum = mptr[3] - s0 - s1 - s2;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[3][3] = sum;
//	diag[3] = sum;
//	invDiagPtr[3] = d = 1.0f / sum;
//
//	if ( n <= 4 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 4; j < n; j++ ) {
//		mptr[j*nc+3] = ( mptr[j*nc+3] - v[0] * mptr[j*nc+0] - v[1] * mptr[j*nc+1] - v[2] * mptr[j*nc+2] ) * d;
//	}
//
//	int ncf = nc * sizeof( float );
//	mptr = mat[0];
//
//	__asm {
//		xorps		xmm2, xmm2
//		xorps		xmm3, xmm3
//		xorps		xmm4, xmm4
//
//		push		ebx
//		mov			ebx, 4
//
//	loopRow:
//			cmp			ebx, n
//			jge			done
//
//			mov			ecx, ebx				// esi = i
//			shl			ecx, 2					// esi = i * 4
//			mov			edx, diag				// edx = diag
//			add			edx, ecx				// edx = &diag[i]
//			mov			edi, ebx				// edi = i
//			imul		edi, ncf				// edi = i * nc * sizeof( float )
//			add			edi, mptr				// edi = mat[i]
//			add			edi, ecx				// edi = &mat[i][i]
//			mov			esi, v					// ecx = v
//			add			esi, ecx				// ecx = &v[i]
//			mov			eax, invDiagPtr			// eax = invDiagPtr
//			add			eax, ecx				// eax = &invDiagPtr[i]
//			neg			ecx
//
//			movaps		xmm0, [edx+ecx]
//			mulps		xmm0, [edi+ecx]
//			movaps		[esi+ecx], xmm0
//			mulps		xmm0, [edi+ecx]
//			add			ecx, 12*4
//			jg			doneDot8
//		dot8:
//			movaps		xmm1, [edx+ecx-(8*4)]
//			mulps		xmm1, [edi+ecx-(8*4)]
//			movaps		[esi+ecx-(8*4)], xmm1
//			mulps		xmm1, [edi+ecx-(8*4)]
//			addps		xmm0, xmm1
//			movaps		xmm2, [edx+ecx-(4*4)]
//			mulps		xmm2, [edi+ecx-(4*4)]
//			movaps		[esi+ecx-(4*4)], xmm2
//			mulps		xmm2, [edi+ecx-(4*4)]
//			addps		xmm0, xmm2
//			add			ecx, 8*4
//			jle			dot8
//		doneDot8:
//			sub			ecx, 4*4
//			jg			doneDot4
//			movaps		xmm1, [edx+ecx-(4*4)]
//			mulps		xmm1, [edi+ecx-(4*4)]
//			movaps		[esi+ecx-(4*4)], xmm1
//			mulps		xmm1, [edi+ecx-(4*4)]
//			addps		xmm0, xmm1
//			add			ecx, 4*4
//		doneDot4:
//			sub			ecx, 2*4
//			jg			doneDot2
//			movlps		xmm3, [edx+ecx-(2*4)]
//			movlps		xmm4, [edi+ecx-(2*4)]
//			mulps		xmm3, xmm4
//			movlps		[esi+ecx-(2*4)], xmm3
//			mulps		xmm3, xmm4
//			addps		xmm0, xmm3
//			add			ecx, 2*4
//		doneDot2:
//			sub			ecx, 1*4
//			jg			doneDot1
//			movss		xmm3, [edx+ecx-(1*4)]
//			movss		xmm4, [edi+ecx-(1*4)]
//			mulss		xmm3, xmm4
//			movss		[esi+ecx-(1*4)], xmm3
//			mulss		xmm3, xmm4
//			addss		xmm0, xmm3
//		doneDot1:
//			movhlps		xmm2, xmm0
//			addps		xmm0, xmm2
//			movaps		xmm2, xmm0
//			shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 0, 0, 0 )
//			addss		xmm0, xmm2
//			movss		xmm1, [edi]
//			subss		xmm1, xmm0
//			movss		[edi], xmm1				// mptr[i] = sum;
//			movss		[edx], xmm1				// diag[i] = sum;
//
//			// if ( sum == 0.0f ) return false;
//			movaps		xmm2, xmm1
//			cmpeqss		xmm2, SIMD_SP_zero
//			andps		xmm2, SIMD_SP_tiny
//			orps		xmm1, xmm2
//
//			rcpss		xmm7, xmm1
//			mulss		xmm1, xmm7
//			mulss		xmm1, xmm7
//			addss		xmm7, xmm7
//			subss		xmm7, xmm1
//			movss		[eax], xmm7				// invDiagPtr[i] = 1.0f / sum;
//
//			mov			edx, n					// edx = n
//			sub			edx, ebx				// edx = n - i
//			dec			edx						// edx = n - i - 1
//			jle			doneSubRow				// if ( i + 1 >= n ) return true;
//
//			mov			eax, ebx				// eax = i
//			shl			eax, 2					// eax = i * 4
//			neg			eax
//
//		loopSubRow:
//				add			edi, ncf
//				mov			ecx, eax
//				movaps		xmm0, [esi+ecx]
//				mulps		xmm0, [edi+ecx]
//				add			ecx, 12*4
//				jg			doneSubDot8
//			subDot8:
//				movaps		xmm1, [esi+ecx-(8*4)]
//				mulps		xmm1, [edi+ecx-(8*4)]
//				addps		xmm0, xmm1
//				movaps		xmm2, [esi+ecx-(4*4)]
//				mulps		xmm2, [edi+ecx-(4*4)]
//				addps		xmm0, xmm2
//				add			ecx, 8*4
//				jle			subDot8
//			doneSubDot8:
//				sub			ecx, 4*4
//				jg			doneSubDot4
//				movaps		xmm1, [esi+ecx-(4*4)]
//				mulps		xmm1, [edi+ecx-(4*4)]
//				addps		xmm0, xmm1
//				add			ecx, 4*4
//			doneSubDot4:
//				sub			ecx, 2*4
//				jg			doneSubDot2
//				movlps		xmm3, [esi+ecx-(2*4)]
//				movlps		xmm4, [edi+ecx-(2*4)]
//				mulps		xmm3, xmm4
//				addps		xmm0, xmm3
//				add			ecx, 2*4
//			doneSubDot2:
//				sub			ecx, 1*4
//				jg			doneSubDot1
//				movss		xmm3, [esi+ecx-(1*4)]
//				movss		xmm4, [edi+ecx-(1*4)]
//				mulss		xmm3, xmm4
//				addss		xmm0, xmm3
//			doneSubDot1:
//				movhlps		xmm2, xmm0
//				addps		xmm0, xmm2
//				movaps		xmm2, xmm0
//				shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 0, 0, 0 )
//				addss		xmm0, xmm2
//				movss		xmm1, [edi]
//				subss		xmm1, xmm0
//				mulss		xmm1, xmm7
//				movss		[edi], xmm1
//				dec			edx
//				jg			loopSubRow
//		doneSubRow:
//			inc		ebx
//			jmp		loopRow
//	done:
//		pop		ebx
//	}
//
//	return true;
//
//#else
//
//	int i, j, k, nc;
//	float *v, *diag, *mptr;
//	double s0, s1, s2, s3, sum, d;
//
//	v = (float *) _alloca16( n * sizeof( float ) );
//	diag = (float *) _alloca16( n * sizeof( float ) );
//
//	nc = mat.GetNumColumns();
//
//	if ( n <= 0 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//
//	sum = mptr[0];
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	diag[0] = sum;
//	invDiag[0] = d = 1.0f / sum;
//
//	if ( n <= 1 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 1; j < n; j++ ) {
//		mptr[j*nc+0] = ( mptr[j*nc+0] ) * d;
//	}
//
//	mptr = mat[1];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	sum = mptr[1] - s0;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[1][1] = sum;
//	diag[1] = sum;
//	invDiag[1] = d = 1.0f / sum;
//
//	if ( n <= 2 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 2; j < n; j++ ) {
//		mptr[j*nc+1] = ( mptr[j*nc+1] - v[0] * mptr[j*nc+0] ) * d;
//	}
//
//	mptr = mat[2];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	v[1] = diag[1] * mptr[1]; s1 = v[1] * mptr[1];
//	sum = mptr[2] - s0 - s1;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[2][2] = sum;
//	diag[2] = sum;
//	invDiag[2] = d = 1.0f / sum;
//
//	if ( n <= 3 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 3; j < n; j++ ) {
//		mptr[j*nc+2] = ( mptr[j*nc+2] - v[0] * mptr[j*nc+0] - v[1] * mptr[j*nc+1] ) * d;
//	}
//
//	mptr = mat[3];
//
//	v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//	v[1] = diag[1] * mptr[1]; s1 = v[1] * mptr[1];
//	v[2] = diag[2] * mptr[2]; s2 = v[2] * mptr[2];
//	sum = mptr[3] - s0 - s1 - s2;
//
//	if ( sum == 0.0f ) {
//		return false;
//	}
//
//	mat[3][3] = sum;
//	diag[3] = sum;
//	invDiag[3] = d = 1.0f / sum;
//
//	if ( n <= 4 ) {
//		return true;
//	}
//
//	mptr = mat[0];
//	for ( j = 4; j < n; j++ ) {
//		mptr[j*nc+3] = ( mptr[j*nc+3] - v[0] * mptr[j*nc+0] - v[1] * mptr[j*nc+1] - v[2] * mptr[j*nc+2] ) * d;
//	}
//
//	for ( i = 4; i < n; i++ ) {
//
//		mptr = mat[i];
//
//		v[0] = diag[0] * mptr[0]; s0 = v[0] * mptr[0];
//		v[1] = diag[1] * mptr[1]; s1 = v[1] * mptr[1];
//		v[2] = diag[2] * mptr[2]; s2 = v[2] * mptr[2];
//		v[3] = diag[3] * mptr[3]; s3 = v[3] * mptr[3];
//		for ( k = 4; k < i-3; k += 4 ) {
//			v[k+0] = diag[k+0] * mptr[k+0]; s0 += v[k+0] * mptr[k+0];
//			v[k+1] = diag[k+1] * mptr[k+1]; s1 += v[k+1] * mptr[k+1];
//			v[k+2] = diag[k+2] * mptr[k+2]; s2 += v[k+2] * mptr[k+2];
//			v[k+3] = diag[k+3] * mptr[k+3]; s3 += v[k+3] * mptr[k+3];
//		}
//		switch( i - k ) {
//			case 3: v[k+2] = diag[k+2] * mptr[k+2]; s0 += v[k+2] * mptr[k+2];
//			case 2: v[k+1] = diag[k+1] * mptr[k+1]; s1 += v[k+1] * mptr[k+1];
//			case 1: v[k+0] = diag[k+0] * mptr[k+0]; s2 += v[k+0] * mptr[k+0];
//		}
//		sum = s3;
//		sum += s2;
//		sum += s1;
//		sum += s0;
//		sum = mptr[i] - sum;
//
//		if ( sum == 0.0f ) {
//			return false;
//		}
//
//		mat[i][i] = sum;
//		diag[i] = sum;
//		invDiag[i] = d = 1.0f / sum;
//
//		if ( i + 1 >= n ) {
//			return true;
//		}
//
//		mptr = mat[i+1];
//		for ( j = i+1; j < n; j++ ) {
//			s0 = mptr[0] * v[0];
//			s1 = mptr[1] * v[1];
//			s2 = mptr[2] * v[2];
//			s3 = mptr[3] * v[3];
//			for ( k = 4; k < i-7; k += 8 ) {
//				s0 += mptr[k+0] * v[k+0];
//				s1 += mptr[k+1] * v[k+1];
//				s2 += mptr[k+2] * v[k+2];
//				s3 += mptr[k+3] * v[k+3];
//				s0 += mptr[k+4] * v[k+4];
//				s1 += mptr[k+5] * v[k+5];
//				s2 += mptr[k+6] * v[k+6];
//				s3 += mptr[k+7] * v[k+7];
//			}
//			switch( i - k ) {
//				case 7: s0 += mptr[k+6] * v[k+6];
//				case 6: s1 += mptr[k+5] * v[k+5];
//				case 5: s2 += mptr[k+4] * v[k+4];
//				case 4: s3 += mptr[k+3] * v[k+3];
//				case 3: s0 += mptr[k+2] * v[k+2];
//				case 2: s1 += mptr[k+1] * v[k+1];
//				case 1: s2 += mptr[k+0] * v[k+0];
//			}
//			sum = s3;
//			sum += s2;
//			sum += s1;
//			sum += s0;
//			mptr[i] = ( mptr[i] - sum ) * d;
//			mptr += nc;
//		}
//	}
//
//	return true;
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::BlendJoints
//============
//*/
//#define REFINE_BLENDJOINTS_RECIPROCAL
//
//void VPCALL idSIMD_SSE::BlendJoints( idJointQuat *joints, const idJointQuat *blendJoints, const float lerp, const int *index, const int numJoints ) {
//	int i;
//
//	if ( lerp <= 0.0f ) {
//		return;
//	} else if ( lerp >= 1.0f ) {
//		for ( i = 0; i < numJoints; i++ ) {
//			int j = index[i];
//			joints[j] = blendJoints[j];
//		}
//		return;
//	}
//
//	for ( i = 0; i <= numJoints - 4; i += 4 ) {
//		ALIGN16( float jointVert0[4] );
//		ALIGN16( float jointVert1[4] );
//		ALIGN16( float jointVert2[4] );
//		ALIGN16( float blendVert0[4] );
//		ALIGN16( float blendVert1[4] );
//		ALIGN16( float blendVert2[4] );
//		ALIGN16( float jointQuat0[4] );
//		ALIGN16( float jointQuat1[4] );
//		ALIGN16( float jointQuat2[4] );
//		ALIGN16( float jointQuat3[4] );
//		ALIGN16( float blendQuat0[4] );
//		ALIGN16( float blendQuat1[4] );
//		ALIGN16( float blendQuat2[4] );
//		ALIGN16( float blendQuat3[4] );
//
//		for ( int j = 0; j < 4; j++ ) {
//			int n = index[i+j];
//
//			jointVert0[j] = joints[n].t[0];
//			jointVert1[j] = joints[n].t[1];
//			jointVert2[j] = joints[n].t[2];
//
//			blendVert0[j] = blendJoints[n].t[0];
//			blendVert1[j] = blendJoints[n].t[1];
//			blendVert2[j] = blendJoints[n].t[2];
//
//			jointQuat0[j] = joints[n].q[0];
//			jointQuat1[j] = joints[n].q[1];
//			jointQuat2[j] = joints[n].q[2];
//			jointQuat3[j] = joints[n].q[3];
//
//			blendQuat0[j] = blendJoints[n].q[0];
//			blendQuat1[j] = blendJoints[n].q[1];
//			blendQuat2[j] = blendJoints[n].q[2];
//			blendQuat3[j] = blendJoints[n].q[3];
//		}
//
//#if 1
//		__asm {
//			// lerp translation
//			movss		xmm7, lerp
//			shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//			movaps		xmm0, blendVert0
//			subps		xmm0, jointVert0
//			mulps		xmm0, xmm7
//			addps		xmm0, jointVert0
//			movaps		jointVert0, xmm0
//			movaps		xmm1, blendVert1
//			subps		xmm1, jointVert1
//			mulps		xmm1, xmm7
//			addps		xmm1, jointVert1
//			movaps		jointVert1, xmm1
//			movaps		xmm2, blendVert2
//			subps		xmm2, jointVert2
//			mulps		xmm2, xmm7
//			addps		xmm2, jointVert2
//			movaps		jointVert2, xmm2
//
//			// lerp quaternions
//			movaps		xmm0, jointQuat0
//			mulps		xmm0, blendQuat0
//			movaps		xmm1, jointQuat1
//			mulps		xmm1, blendQuat1
//			addps		xmm0, xmm1
//			movaps		xmm2, jointQuat2
//			mulps		xmm2, blendQuat2
//			addps		xmm0, xmm2
//			movaps		xmm3, jointQuat3
//			mulps		xmm3, blendQuat3
//			addps		xmm0, xmm3					// xmm0 = cosom
//
//			movaps		xmm1, xmm0
//			movaps		xmm2, xmm0
//			andps		xmm1, SIMD_SP_signBitMask	// xmm1 = signBit
//			xorps		xmm0, xmm1
//			mulps		xmm2, xmm2
//
//			xorps		xmm4, xmm4
//			movaps		xmm3, SIMD_SP_one
//			subps		xmm3, xmm2					// xmm3 = scale0
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//
//#ifdef REFINE_BLENDJOINTS_RECIPROCAL
//			movaps		xmm2, xmm3
//			rsqrtps		xmm4, xmm2
//			mulps		xmm2, xmm4
//			mulps		xmm2, xmm4
//			subps		xmm2, SIMD_SP_rsqrt_c0
//			mulps		xmm4, SIMD_SP_rsqrt_c1
//			mulps		xmm2, xmm4
//#else
//			rsqrtps		xmm2, xmm3					// xmm2 = sinom
//#endif
//			mulps		xmm3, xmm2					// xmm3 = sqrt( scale0 )
//
//			// omega0 = atan2( xmm3, xmm0 )
//			movaps		xmm4, xmm0
//			minps		xmm0, xmm3
//			maxps		xmm3, xmm4
//			cmpeqps		xmm4, xmm0
//
//#ifdef REFINE_BLENDJOINTS_RECIPROCAL
//			rcpps		xmm5, xmm3
//			mulps		xmm3, xmm5
//			mulps		xmm3, xmm5
//			addps		xmm5, xmm5
//			subps		xmm5, xmm3					// xmm5 = 1 / y or 1 / x
//			mulps		xmm0, xmm5					// xmm0 = x / y or y / x
//#else
//			rcpps		xmm3, xmm3					// xmm3 = 1 / y or 1 / x
//			mulps		xmm0, xmm3					// xmm0 = x / y or y / x
//#endif
//			movaps		xmm3, xmm4
//			andps		xmm3, SIMD_SP_signBitMask
//			xorps		xmm0, xmm3					// xmm0 = -x / y or y / x
//			andps		xmm4, SIMD_SP_halfPI		// xmm4 = HALF_PI or 0.0f
//			movaps		xmm3, xmm0
//			mulps		xmm3, xmm3					// xmm3 = s
//			movaps		xmm5, SIMD_SP_atan_c0
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c1
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c2
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c3
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c4
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c5
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c6
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_atan_c7
//			mulps		xmm5, xmm3
//			addps		xmm5, SIMD_SP_one
//			mulps		xmm5, xmm0
//			addps		xmm5, xmm4					// xmm5 = omega0
//
//			movaps		xmm6, xmm7					// xmm6 = lerp
//			mulps		xmm6, xmm5					// xmm6 = omega1
//			subps		xmm5, xmm6					// xmm5 = omega0
//
//			// scale0 = sin( xmm5 ) * xmm2
//			// scale1 = sin( xmm6 ) * xmm2
//			movaps		xmm3, xmm5
//			movaps		xmm7, xmm6
//			mulps		xmm3, xmm3
//			mulps		xmm7, xmm7
//			movaps		xmm4, SIMD_SP_sin_c0
//			movaps		xmm0, SIMD_SP_sin_c0
//			mulps		xmm4, xmm3
//			mulps		xmm0, xmm7
//			addps		xmm4, SIMD_SP_sin_c1
//			addps		xmm0, SIMD_SP_sin_c1
//			mulps		xmm4, xmm3
//			mulps		xmm0, xmm7
//			addps		xmm4, SIMD_SP_sin_c2
//			addps		xmm0, SIMD_SP_sin_c2
//			mulps		xmm4, xmm3
//			mulps		xmm0, xmm7
//			addps		xmm4, SIMD_SP_sin_c3
//			addps		xmm0, SIMD_SP_sin_c3
//			mulps		xmm4, xmm3
//			mulps		xmm0, xmm7
//			addps		xmm4, SIMD_SP_sin_c4
//			addps		xmm0, SIMD_SP_sin_c4
//			mulps		xmm4, xmm3
//			mulps		xmm0, xmm7
//			addps		xmm4, SIMD_SP_one
//			addps		xmm0, SIMD_SP_one
//			mulps		xmm5, xmm4
//			mulps		xmm6, xmm0
//			mulps		xmm5, xmm2					// xmm5 = scale0
//			mulps		xmm6, xmm2					// xmm6 = scale1
//
//			xorps		xmm6, xmm1
//
//			movaps		xmm0, jointQuat0
//			mulps		xmm0, xmm5
//			movaps		xmm1, blendQuat0
//			mulps		xmm1, xmm6
//			addps		xmm0, xmm1
//			movaps		jointQuat0, xmm0
//
//			movaps		xmm1, jointQuat1
//			mulps		xmm1, xmm5
//			movaps		xmm2, blendQuat1
//			mulps		xmm2, xmm6
//			addps		xmm1, xmm2
//			movaps		jointQuat1, xmm1
//
//			movaps		xmm2, jointQuat2
//			mulps		xmm2, xmm5
//			movaps		xmm3, blendQuat2
//			mulps		xmm3, xmm6
//			addps		xmm2, xmm3
//			movaps		jointQuat2, xmm2
//
//			movaps		xmm3, jointQuat3
//			mulps		xmm3, xmm5
//			movaps		xmm4, blendQuat3
//			mulps		xmm4, xmm6
//			addps		xmm3, xmm4
//			movaps		jointQuat3, xmm3
//		}
//
//#else
//
//		jointVert0[0] += lerp * ( blendVert0[0] - jointVert0[0] );
//		jointVert0[1] += lerp * ( blendVert0[1] - jointVert0[1] );
//		jointVert0[2] += lerp * ( blendVert0[2] - jointVert0[2] );
//		jointVert0[3] += lerp * ( blendVert0[3] - jointVert0[3] );
//
//		jointVert1[0] += lerp * ( blendVert1[0] - jointVert1[0] );
//		jointVert1[1] += lerp * ( blendVert1[1] - jointVert1[1] );
//		jointVert1[2] += lerp * ( blendVert1[2] - jointVert1[2] );
//		jointVert1[3] += lerp * ( blendVert1[3] - jointVert1[3] );
//
//		jointVert2[0] += lerp * ( blendVert2[0] - jointVert2[0] );
//		jointVert2[1] += lerp * ( blendVert2[1] - jointVert2[1] );
//		jointVert2[2] += lerp * ( blendVert2[2] - jointVert2[2] );
//		jointVert2[3] += lerp * ( blendVert2[3] - jointVert2[3] );
//
//		ALIGN16( float cosom[4] );
//		ALIGN16( float sinom[4] );
//		ALIGN16( float omega0[4] );
//		ALIGN16( float omega1[4] );
//		ALIGN16( float scale0[4] );
//		ALIGN16( float scale1[4] );
//		ALIGN16( unsigned long signBit[4] );
//
//		cosom[0] = jointQuat0[0] * blendQuat0[0];
//		cosom[1] = jointQuat0[1] * blendQuat0[1];
//		cosom[2] = jointQuat0[2] * blendQuat0[2];
//		cosom[3] = jointQuat0[3] * blendQuat0[3];
//
//		cosom[0] += jointQuat1[0] * blendQuat1[0];
//		cosom[1] += jointQuat1[1] * blendQuat1[1];
//		cosom[2] += jointQuat1[2] * blendQuat1[2];
//		cosom[3] += jointQuat1[3] * blendQuat1[3];
//
//		cosom[0] += jointQuat2[0] * blendQuat2[0];
//		cosom[1] += jointQuat2[1] * blendQuat2[1];
//		cosom[2] += jointQuat2[2] * blendQuat2[2];
//		cosom[3] += jointQuat2[3] * blendQuat2[3];
//
//		cosom[0] += jointQuat3[0] * blendQuat3[0];
//		cosom[1] += jointQuat3[1] * blendQuat3[1];
//		cosom[2] += jointQuat3[2] * blendQuat3[2];
//		cosom[3] += jointQuat3[3] * blendQuat3[3];
//
//		signBit[0] = (*(unsigned long *)&cosom[0]) & ( 1 << 31 );
//		signBit[1] = (*(unsigned long *)&cosom[1]) & ( 1 << 31 );
//		signBit[2] = (*(unsigned long *)&cosom[2]) & ( 1 << 31 );
//		signBit[3] = (*(unsigned long *)&cosom[3]) & ( 1 << 31 );
//
//		(*(unsigned long *)&cosom[0]) ^= signBit[0];
//		(*(unsigned long *)&cosom[1]) ^= signBit[1];
//		(*(unsigned long *)&cosom[2]) ^= signBit[2];
//		(*(unsigned long *)&cosom[3]) ^= signBit[3];
//
//		scale0[0] = 1.0f - cosom[0] * cosom[0];
//		scale0[1] = 1.0f - cosom[1] * cosom[1];
//		scale0[2] = 1.0f - cosom[2] * cosom[2];
//		scale0[3] = 1.0f - cosom[3] * cosom[3];
//
//		scale0[0] = ( scale0[0] <= 0.0f ) ? SIMD_SP_tiny[0] : scale0[0];
//		scale0[1] = ( scale0[1] <= 0.0f ) ? SIMD_SP_tiny[1] : scale0[1];
//		scale0[2] = ( scale0[2] <= 0.0f ) ? SIMD_SP_tiny[2] : scale0[2];
//		scale0[3] = ( scale0[3] <= 0.0f ) ? SIMD_SP_tiny[3] : scale0[3];
//
//		sinom[0] = idMath::RSqrt( scale0[0] );
//		sinom[1] = idMath::RSqrt( scale0[1] );
//		sinom[2] = idMath::RSqrt( scale0[2] );
//		sinom[3] = idMath::RSqrt( scale0[3] );
//
//		scale0[0] *= sinom[0];
//		scale0[1] *= sinom[1];
//		scale0[2] *= sinom[2];
//		scale0[3] *= sinom[3];
//
//		omega0[0] = SSE_ATanPositive( scale0[0], cosom[0] );
//		omega0[1] = SSE_ATanPositive( scale0[1], cosom[1] );
//		omega0[2] = SSE_ATanPositive( scale0[2], cosom[2] );
//		omega0[3] = SSE_ATanPositive( scale0[3], cosom[3] );
//
//		omega1[0] = lerp * omega0[0];
//		omega1[1] = lerp * omega0[1];
//		omega1[2] = lerp * omega0[2];
//		omega1[3] = lerp * omega0[3];
//
//		omega0[0] -= omega1[0];
//		omega0[1] -= omega1[1];
//		omega0[2] -= omega1[2];
//		omega0[3] -= omega1[3];
//
//		scale0[0] = SSE_SinZeroHalfPI( omega0[0] ) * sinom[0];
//		scale0[1] = SSE_SinZeroHalfPI( omega0[1] ) * sinom[1];
//		scale0[2] = SSE_SinZeroHalfPI( omega0[2] ) * sinom[2];
//		scale0[3] = SSE_SinZeroHalfPI( omega0[3] ) * sinom[3];
//
//		scale1[0] = SSE_SinZeroHalfPI( omega1[0] ) * sinom[0];
//		scale1[1] = SSE_SinZeroHalfPI( omega1[1] ) * sinom[1];
//		scale1[2] = SSE_SinZeroHalfPI( omega1[2] ) * sinom[2];
//		scale1[3] = SSE_SinZeroHalfPI( omega1[3] ) * sinom[3];
//
//		(*(unsigned long *)&scale1[0]) ^= signBit[0];
//		(*(unsigned long *)&scale1[1]) ^= signBit[1];
//		(*(unsigned long *)&scale1[2]) ^= signBit[2];
//		(*(unsigned long *)&scale1[3]) ^= signBit[3];
//
//		jointQuat0[0] = scale0[0] * jointQuat0[0] + scale1[0] * blendQuat0[0];
//		jointQuat0[1] = scale0[1] * jointQuat0[1] + scale1[1] * blendQuat0[1];
//		jointQuat0[2] = scale0[2] * jointQuat0[2] + scale1[2] * blendQuat0[2];
//		jointQuat0[3] = scale0[3] * jointQuat0[3] + scale1[3] * blendQuat0[3];
//
//		jointQuat1[0] = scale0[0] * jointQuat1[0] + scale1[0] * blendQuat1[0];
//		jointQuat1[1] = scale0[1] * jointQuat1[1] + scale1[1] * blendQuat1[1];
//		jointQuat1[2] = scale0[2] * jointQuat1[2] + scale1[2] * blendQuat1[2];
//		jointQuat1[3] = scale0[3] * jointQuat1[3] + scale1[3] * blendQuat1[3];
//
//		jointQuat2[0] = scale0[0] * jointQuat2[0] + scale1[0] * blendQuat2[0];
//		jointQuat2[1] = scale0[1] * jointQuat2[1] + scale1[1] * blendQuat2[1];
//		jointQuat2[2] = scale0[2] * jointQuat2[2] + scale1[2] * blendQuat2[2];
//		jointQuat2[3] = scale0[3] * jointQuat2[3] + scale1[3] * blendQuat2[3];
//
//		jointQuat3[0] = scale0[0] * jointQuat3[0] + scale1[0] * blendQuat3[0];
//		jointQuat3[1] = scale0[1] * jointQuat3[1] + scale1[1] * blendQuat3[1];
//		jointQuat3[2] = scale0[2] * jointQuat3[2] + scale1[2] * blendQuat3[2];
//		jointQuat3[3] = scale0[3] * jointQuat3[3] + scale1[3] * blendQuat3[3];
//
//#endif
//
//		for ( int j = 0; j < 4; j++ ) {
//			int n = index[i+j];
//
//			joints[n].t[0] = jointVert0[j];
//			joints[n].t[1] = jointVert1[j];
//			joints[n].t[2] = jointVert2[j];
//
//			joints[n].q[0] = jointQuat0[j];
//			joints[n].q[1] = jointQuat1[j];
//			joints[n].q[2] = jointQuat2[j];
//			joints[n].q[3] = jointQuat3[j];
//		}
//	}
//
//	for ( ; i < numJoints; i++ ) {
//		int n = index[i];
//
//		idVec3 &jointVert = joints[n].t;
//		const idVec3 &blendVert = blendJoints[n].t;
//
//		jointVert[0] += lerp * ( blendVert[0] - jointVert[0] );
//		jointVert[1] += lerp * ( blendVert[1] - jointVert[1] );
//		jointVert[2] += lerp * ( blendVert[2] - jointVert[2] );
//
//		idQuat &jointQuat = joints[n].q;
//		const idQuat &blendQuat = blendJoints[n].q;
//
//		float cosom;
//		float sinom;
//		float omega;
//		float scale0;
//		float scale1;
//		unsigned long signBit;
//
//		cosom = jointQuat.x * blendQuat.x + jointQuat.y * blendQuat.y + jointQuat.z * blendQuat.z + jointQuat.w * blendQuat.w;
//
//		signBit = (*(unsigned long *)&cosom) & ( 1 << 31 );
//
//		(*(unsigned long *)&cosom) ^= signBit;
//
//		scale0 = 1.0f - cosom * cosom;
//		scale0 = ( scale0 <= 0.0f ) ? SIMD_SP_tiny[0] : scale0;
//		sinom = idMath::InvSqrt( scale0 );
//		omega = idMath::ATan16( scale0 * sinom, cosom );
//		scale0 = idMath::Sin16( ( 1.0f - lerp ) * omega ) * sinom;
//		scale1 = idMath::Sin16( lerp * omega ) * sinom;
//
//		(*(unsigned long *)&scale1) ^= signBit;
//
//		jointQuat.x = scale0 * jointQuat.x + scale1 * blendQuat.x;
//		jointQuat.y = scale0 * jointQuat.y + scale1 * blendQuat.y;
//		jointQuat.z = scale0 * jointQuat.z + scale1 * blendQuat.z;
//		jointQuat.w = scale0 * jointQuat.w + scale1 * blendQuat.w;
//	}
//}
//
///*
//============
//idSIMD_SSE::ConvertJointQuatsToJointMats
//============
//*/
//void VPCALL idSIMD_SSE::ConvertJointQuatsToJointMats( idJointMat *jointMats, const idJointQuat *jointQuats, const int numJoints ) {
//
//	assert( sizeof( idJointQuat ) == JOINTQUAT_SIZE );
//	assert( sizeof( idJointMat ) == JOINTMAT_SIZE );
//	assert( (int)(&((idJointQuat *)0)->t) == (int)(&((idJointQuat *)0)->q) + (int)sizeof( ((idJointQuat *)0)->q ) );
//
//	for ( int i = 0; i < numJoints; i++ ) {
//
//		const float *q = jointQuats[i].q.ToFloatPtr();
//		float *m = jointMats[i].ToFloatPtr();
//
//		m[0*4+3] = q[4];
//		m[1*4+3] = q[5];
//		m[2*4+3] = q[6];
//
//		float x2 = q[0] + q[0];
//		float y2 = q[1] + q[1];
//		float z2 = q[2] + q[2];
//
//		{
//			float xx = q[0] * x2;
//			float yy = q[1] * y2;
//			float zz = q[2] * z2;
//
//			m[0*4+0] = 1.0f - yy - zz;
//			m[1*4+1] = 1.0f - xx - zz;
//			m[2*4+2] = 1.0f - xx - yy;
//		}
//
//		{
//			float yz = q[1] * z2;
//			float wx = q[3] * x2;
//
//			m[2*4+1] = yz - wx;
//			m[1*4+2] = yz + wx;
//		}
//
//		{
//			float xy = q[0] * y2;
//			float wz = q[3] * z2;
//
//			m[1*4+0] = xy - wz;
//			m[0*4+1] = xy + wz;
//		}
//
//		{
//			float xz = q[0] * z2;
//			float wy = q[3] * y2;
//
//			m[0*4+2] = xz - wy;
//			m[2*4+0] = xz + wy;
//		}
//	}
//}
//
///*
//============
//idSIMD_SSE::ConvertJointMatsToJointQuats
//============
//*/
//void VPCALL idSIMD_SSE::ConvertJointMatsToJointQuats( idJointQuat *jointQuats, const idJointMat *jointMats, const int numJoints ) {
//
//	assert( sizeof( idJointQuat ) == JOINTQUAT_SIZE );
//	assert( sizeof( idJointMat ) == JOINTMAT_SIZE );
//	assert( (int)(&((idJointQuat *)0)->t) == (int)(&((idJointQuat *)0)->q) + (int)sizeof( ((idJointQuat *)0)->q ) );
//
//#if 1
//
//	ALIGN16( byte shuffle[16] );
//
//	__asm {
//		mov			eax, numJoints
//		mov			esi, jointMats
//		mov			edi, jointQuats
//		and			eax, ~3
//		jz			done4
//		imul		eax, JOINTMAT_SIZE
//		add			esi, eax
//		neg			eax
//
//	loopMat4:
//		movss		xmm5, [esi+eax+3*JOINTMAT_SIZE+0*16+0*4]
//		movss		xmm6, [esi+eax+3*JOINTMAT_SIZE+1*16+1*4]
//		movss		xmm7, [esi+eax+3*JOINTMAT_SIZE+2*16+2*4]
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm0, [esi+eax+2*JOINTMAT_SIZE+0*16+0*4]
//		movss		xmm1, [esi+eax+2*JOINTMAT_SIZE+1*16+1*4]
//		movss		xmm2, [esi+eax+2*JOINTMAT_SIZE+2*16+2*4]
//
//		movss		xmm5, xmm0
//		movss		xmm6, xmm1
//		movss		xmm7, xmm2
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm0, [esi+eax+1*JOINTMAT_SIZE+0*16+0*4]
//		movss		xmm1, [esi+eax+1*JOINTMAT_SIZE+1*16+1*4]
//		movss		xmm2, [esi+eax+1*JOINTMAT_SIZE+2*16+2*4]
//
//		movss		xmm5, xmm0
//		movss		xmm6, xmm1
//		movss		xmm7, xmm2
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm0, [esi+eax+0*JOINTMAT_SIZE+0*16+0*4]
//		movss		xmm1, [esi+eax+0*JOINTMAT_SIZE+1*16+1*4]
//		movss		xmm2, [esi+eax+0*JOINTMAT_SIZE+2*16+2*4]
//
//		movss		xmm5, xmm0
//		movss		xmm6, xmm1
//		movss		xmm7, xmm2
//
//		// -------------------
//
//		movaps		xmm0, xmm5
//		addps		xmm0, xmm6
//		addps		xmm0, xmm7
//		cmpnltps	xmm0, SIMD_SP_zero						// xmm0 = m[0 * 4 + 0] + m[1 * 4 + 1] + m[2 * 4 + 2] > 0.0f
//
//		movaps		xmm1, xmm5
//		movaps		xmm2, xmm5
//		cmpnltps	xmm1, xmm6
//		cmpnltps	xmm2, xmm7
//		andps		xmm2, xmm1								// xmm2 = m[0 * 4 + 0] > m[1 * 4 + 1] && m[0 * 4 + 0] > m[2 * 4 + 2]
//
//		movaps		xmm4, xmm6
//		cmpnltps	xmm4, xmm7								// xmm3 = m[1 * 4 + 1] > m[2 * 4 + 2]
//
//		movaps		xmm1, xmm0
//		andnps		xmm1, xmm2
//		orps		xmm2, xmm0
//		movaps		xmm3, xmm2
//		andnps		xmm2, xmm4
//		orps		xmm3, xmm2
//		xorps		xmm3, SIMD_SP_not
//
//		andps		xmm0, SIMD_DW_mat2quatShuffle0
//		movaps		xmm4, xmm1
//		andps		xmm4, SIMD_DW_mat2quatShuffle1
//		orps		xmm0, xmm4
//		movaps		xmm4, xmm2
//		andps		xmm4, SIMD_DW_mat2quatShuffle2
//		orps		xmm0, xmm4
//		movaps		xmm4, xmm3
//		andps		xmm4, SIMD_DW_mat2quatShuffle3
//		orps		xmm4, xmm0
//
//		movaps		shuffle, xmm4
//
//		movaps		xmm0, xmm2
//		orps		xmm0, xmm3								// xmm0 = xmm2 | xmm3	= s0
//		orps		xmm2, xmm1								// xmm2 = xmm1 | xmm2	= s2
//		orps		xmm1, xmm3								// xmm1 = xmm1 | xmm3	= s1
//
//		andps		xmm0, SIMD_SP_signBitMask
//		andps		xmm1, SIMD_SP_signBitMask
//		andps		xmm2, SIMD_SP_signBitMask
//
//		xorps		xmm5, xmm0
//		xorps		xmm6, xmm1
//		xorps		xmm7, xmm2
//		addps		xmm5, xmm6
//		addps		xmm7, SIMD_SP_one
//		addps		xmm5, xmm7								// xmm5 = t
//
//		movaps		xmm7, xmm5								// xmm7 = t
//		rsqrtps		xmm6, xmm5
//		mulps		xmm5, xmm6
//		mulps		xmm5, xmm6
//		subps		xmm5, SIMD_SP_rsqrt_c0
//		mulps		xmm6, SIMD_SP_mat2quat_rsqrt_c1
//		mulps		xmm6, xmm5								// xmm5 = s
//
//		mulps		xmm7, xmm6								// xmm7 = s * t
//		xorps		xmm6, SIMD_SP_signBitMask				// xmm6 = -s
//
//		// -------------------
//
//		add			edi, 4*JOINTQUAT_SIZE
//
//		movzx		ecx, byte ptr shuffle[0*4+0]			// ecx = k0
//		movss		[edi+ecx*4-4*JOINTQUAT_SIZE], xmm7		// q[k0] = s * t;
//
//		movzx		edx, byte ptr shuffle[0*4+1]			// edx = k1
//		movss		xmm4, [esi+eax+0*JOINTMAT_SIZE+1*16+0*4]
//		xorps		xmm4, xmm2
//		subss		xmm4, [esi+eax+0*JOINTMAT_SIZE+0*16+1*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-4*JOINTQUAT_SIZE], xmm4		// q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//
//		movzx		ecx, byte ptr shuffle[0*4+2]			// ecx = k2
//		movss		xmm3, [esi+eax+0*JOINTMAT_SIZE+0*16+2*4]
//		xorps		xmm3, xmm1
//		subss		xmm3, [esi+eax+0*JOINTMAT_SIZE+2*16+0*4]
//		mulss		xmm3, xmm6
//		movss		[edi+ecx*4-4*JOINTQUAT_SIZE], xmm3		// q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//
//		movzx		edx, byte ptr shuffle[0*4+3]			// edx = k3
//		movss		xmm4, [esi+eax+0*JOINTMAT_SIZE+2*16+1*4]
//		xorps		xmm4, xmm0
//		subss		xmm4, [esi+eax+0*JOINTMAT_SIZE+1*16+2*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-4*JOINTQUAT_SIZE], xmm4		// q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		mov			ecx, [esi+eax+0*JOINTMAT_SIZE+0*16+3*4]
//		mov			[edi-4*JOINTQUAT_SIZE+16], ecx			// q[4] = m[0 * 4 + 3];
//		mov			edx, [esi+eax+0*JOINTMAT_SIZE+1*16+3*4]
//		mov			[edi-4*JOINTQUAT_SIZE+20], edx			// q[5] = m[1 * 4 + 3];
//		mov			ecx, [esi+eax+0*JOINTMAT_SIZE+2*16+3*4]
//		mov			[edi-4*JOINTQUAT_SIZE+24], ecx			// q[6] = m[2 * 4 + 3];
//
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movzx		ecx, byte ptr shuffle[1*4+0]			// ecx = k0
//		movss		[edi+ecx*4-3*JOINTQUAT_SIZE], xmm7		// q[k0] = s * t;
//
//		movzx		edx, byte ptr shuffle[1*4+1]			// edx = k1
//		movss		xmm4, [esi+eax+1*JOINTMAT_SIZE+1*16+0*4]
//		xorps		xmm4, xmm2
//		subss		xmm4, [esi+eax+1*JOINTMAT_SIZE+0*16+1*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-3*JOINTQUAT_SIZE], xmm4		// q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//
//		movzx		ecx, byte ptr shuffle[1*4+2]			// ecx = k2
//		movss		xmm3, [esi+eax+1*JOINTMAT_SIZE+0*16+2*4]
//		xorps		xmm3, xmm1
//		subss		xmm3, [esi+eax+1*JOINTMAT_SIZE+2*16+0*4]
//		mulss		xmm3, xmm6
//		movss		[edi+ecx*4-3*JOINTQUAT_SIZE], xmm3		// q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//
//		movzx		edx, byte ptr shuffle[1*4+3]			// edx = k3
//		movss		xmm4, [esi+eax+1*JOINTMAT_SIZE+2*16+1*4]
//		xorps		xmm4, xmm0
//		subss		xmm4, [esi+eax+1*JOINTMAT_SIZE+1*16+2*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-3*JOINTQUAT_SIZE], xmm4		// q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		mov			ecx, [esi+eax+1*JOINTMAT_SIZE+0*16+3*4]
//		mov			[edi-3*JOINTQUAT_SIZE+16], ecx			// q[4] = m[0 * 4 + 3];
//		mov			edx, [esi+eax+1*JOINTMAT_SIZE+1*16+3*4]
//		mov			[edi-3*JOINTQUAT_SIZE+20], edx			// q[5] = m[1 * 4 + 3];
//		mov			ecx, [esi+eax+1*JOINTMAT_SIZE+2*16+3*4]
//		mov			[edi-3*JOINTQUAT_SIZE+24], ecx			// q[6] = m[2 * 4 + 3];
//
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movzx		ecx, byte ptr shuffle[2*4+0]			// ecx = k0
//		movss		[edi+ecx*4-2*JOINTQUAT_SIZE], xmm7		// q[k0] = s * t;
//
//		movzx		edx, byte ptr shuffle[2*4+1]			// edx = k1
//		movss		xmm4, [esi+eax+2*JOINTMAT_SIZE+1*16+0*4]
//		xorps		xmm4, xmm2
//		subss		xmm4, [esi+eax+2*JOINTMAT_SIZE+0*16+1*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-2*JOINTQUAT_SIZE], xmm4		// q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//
//		movzx		ecx, byte ptr shuffle[2*4+2]			// ecx = k2
//		movss		xmm3, [esi+eax+2*JOINTMAT_SIZE+0*16+2*4]
//		xorps		xmm3, xmm1
//		subss		xmm3, [esi+eax+2*JOINTMAT_SIZE+2*16+0*4]
//		mulss		xmm3, xmm6
//		movss		[edi+ecx*4-2*JOINTQUAT_SIZE], xmm3		// q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//
//		movzx		edx, byte ptr shuffle[2*4+3]			// edx = k3
//		movss		xmm4, [esi+eax+2*JOINTMAT_SIZE+2*16+1*4]
//		xorps		xmm4, xmm0
//		subss		xmm4, [esi+eax+2*JOINTMAT_SIZE+1*16+2*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-2*JOINTQUAT_SIZE], xmm4		// q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		mov			ecx, [esi+eax+2*JOINTMAT_SIZE+0*16+3*4]
//		mov			[edi-2*JOINTQUAT_SIZE+16], ecx			// q[4] = m[0 * 4 + 3];
//		mov			edx, [esi+eax+2*JOINTMAT_SIZE+1*16+3*4]
//		mov			[edi-2*JOINTQUAT_SIZE+20], edx			// q[5] = m[1 * 4 + 3];
//		mov			ecx, [esi+eax+2*JOINTMAT_SIZE+2*16+3*4]
//		mov			[edi-2*JOINTQUAT_SIZE+24], ecx			// q[6] = m[2 * 4 + 3];
//
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movzx		ecx, byte ptr shuffle[3*4+0]			// ecx = k0
//		movss		[edi+ecx*4-1*JOINTQUAT_SIZE], xmm7		// q[k0] = s * t;
//
//		movzx		edx, byte ptr shuffle[3*4+1]			// edx = k1
//		movss		xmm4, [esi+eax+3*JOINTMAT_SIZE+1*16+0*4]
//		xorps		xmm4, xmm2
//		subss		xmm4, [esi+eax+3*JOINTMAT_SIZE+0*16+1*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-1*JOINTQUAT_SIZE], xmm4		// q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//
//		movzx		ecx, byte ptr shuffle[3*4+2]			// ecx = k2
//		movss		xmm3, [esi+eax+3*JOINTMAT_SIZE+0*16+2*4]
//		xorps		xmm3, xmm1
//		subss		xmm3, [esi+eax+3*JOINTMAT_SIZE+2*16+0*4]
//		mulss		xmm3, xmm6
//		movss		[edi+ecx*4-1*JOINTQUAT_SIZE], xmm3		// q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//
//		movzx		edx, byte ptr shuffle[3*4+3]			// edx = k3
//		movss		xmm4, [esi+eax+3*JOINTMAT_SIZE+2*16+1*4]
//		xorps		xmm4, xmm0
//		subss		xmm4, [esi+eax+3*JOINTMAT_SIZE+1*16+2*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-1*JOINTQUAT_SIZE], xmm4		// q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		mov			ecx, [esi+eax+3*JOINTMAT_SIZE+0*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+16], ecx			// q[4] = m[0 * 4 + 3];
//		mov			edx, [esi+eax+3*JOINTMAT_SIZE+1*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+20], edx			// q[5] = m[1 * 4 + 3];
//		mov			ecx, [esi+eax+3*JOINTMAT_SIZE+2*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+24], ecx			// q[6] = m[2 * 4 + 3];
//
//		add			eax, 4*JOINTMAT_SIZE
//		jl			loopMat4
//
//	done4:
//		mov			eax, numJoints
//		and			eax, 3
//		jz			done1
//		imul		eax, JOINTMAT_SIZE
//		add			esi, eax
//		neg			eax
//
//	loopMat1:
//		movss		xmm5, [esi+eax+0*JOINTMAT_SIZE+0*16+0*4]
//		movss		xmm6, [esi+eax+0*JOINTMAT_SIZE+1*16+1*4]
//		movss		xmm7, [esi+eax+0*JOINTMAT_SIZE+2*16+2*4]
//
//		// -------------------
//
//		movaps		xmm0, xmm5
//		addss		xmm0, xmm6
//		addss		xmm0, xmm7
//		cmpnltss	xmm0, SIMD_SP_zero						// xmm0 = m[0 * 4 + 0] + m[1 * 4 + 1] + m[2 * 4 + 2] > 0.0f
//
//		movaps		xmm1, xmm5
//		movaps		xmm2, xmm5
//		cmpnltss	xmm1, xmm6
//		cmpnltss	xmm2, xmm7
//		andps		xmm2, xmm1								// xmm2 = m[0 * 4 + 0] > m[1 * 4 + 1] && m[0 * 4 + 0] > m[2 * 4 + 2]
//
//		movaps		xmm4, xmm6
//		cmpnltss	xmm4, xmm7								// xmm3 = m[1 * 4 + 1] > m[2 * 4 + 2]
//
//		movaps		xmm1, xmm0
//		andnps		xmm1, xmm2
//		orps		xmm2, xmm0
//		movaps		xmm3, xmm2
//		andnps		xmm2, xmm4
//		orps		xmm3, xmm2
//		xorps		xmm3, SIMD_SP_not
//
//		andps		xmm0, SIMD_DW_mat2quatShuffle0
//		movaps		xmm4, xmm1
//		andps		xmm4, SIMD_DW_mat2quatShuffle1
//		orps		xmm0, xmm4
//		movaps		xmm4, xmm2
//		andps		xmm4, SIMD_DW_mat2quatShuffle2
//		orps		xmm0, xmm4
//		movaps		xmm4, xmm3
//		andps		xmm4, SIMD_DW_mat2quatShuffle3
//		orps		xmm4, xmm0
//
//		movss		shuffle, xmm4
//
//		movaps		xmm0, xmm2
//		orps		xmm0, xmm3								// xmm0 = xmm2 | xmm3	= s0
//		orps		xmm2, xmm1								// xmm2 = xmm1 | xmm2	= s2
//		orps		xmm1, xmm3								// xmm1 = xmm1 | xmm3	= s1
//
//		andps		xmm0, SIMD_SP_signBitMask
//		andps		xmm1, SIMD_SP_signBitMask
//		andps		xmm2, SIMD_SP_signBitMask
//
//		xorps		xmm5, xmm0
//		xorps		xmm6, xmm1
//		xorps		xmm7, xmm2
//		addss		xmm5, xmm6
//		addss		xmm7, SIMD_SP_one
//		addss		xmm5, xmm7								// xmm5 = t
//
//		movss		xmm7, xmm5								// xmm7 = t
//		rsqrtss		xmm6, xmm5
//		mulss		xmm5, xmm6
//		mulss		xmm5, xmm6
//		subss		xmm5, SIMD_SP_rsqrt_c0
//		mulss		xmm6, SIMD_SP_mat2quat_rsqrt_c1
//		mulss		xmm6, xmm5								// xmm5 = s
//
//		mulss		xmm7, xmm6								// xmm7 = s * t
//		xorps		xmm6, SIMD_SP_signBitMask				// xmm6 = -s
//
//		// -------------------
//
//		movzx		ecx, byte ptr shuffle[0]				// ecx = k0
//		add			edi, JOINTQUAT_SIZE
//		movss		[edi+ecx*4-1*JOINTQUAT_SIZE], xmm7		// q[k0] = s * t;
//
//		movzx		edx, byte ptr shuffle[1]				// edx = k1
//		movss		xmm4, [esi+eax+0*JOINTMAT_SIZE+1*16+0*4]
//		xorps		xmm4, xmm2
//		subss		xmm4, [esi+eax+0*JOINTMAT_SIZE+0*16+1*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-1*JOINTQUAT_SIZE], xmm4		// q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//
//		movzx		ecx, byte ptr shuffle[2]				// ecx = k2
//		movss		xmm3, [esi+eax+0*JOINTMAT_SIZE+0*16+2*4]
//		xorps		xmm3, xmm1
//		subss		xmm3, [esi+eax+0*JOINTMAT_SIZE+2*16+0*4]
//		mulss		xmm3, xmm6
//		movss		[edi+ecx*4-1*JOINTQUAT_SIZE], xmm3		// q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//
//		movzx		edx, byte ptr shuffle[3]				// edx = k3
//		movss		xmm4, [esi+eax+0*JOINTMAT_SIZE+2*16+1*4]
//		xorps		xmm4, xmm0
//		subss		xmm4, [esi+eax+0*JOINTMAT_SIZE+1*16+2*4]
//		mulss		xmm4, xmm6
//		movss		[edi+edx*4-1*JOINTQUAT_SIZE], xmm4		// q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		mov			ecx, [esi+eax+0*JOINTMAT_SIZE+0*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+16], ecx			// q[4] = m[0 * 4 + 3];
//		mov			edx, [esi+eax+0*JOINTMAT_SIZE+1*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+20], edx			// q[5] = m[1 * 4 + 3];
//		mov			ecx, [esi+eax+0*JOINTMAT_SIZE+2*16+3*4]
//		mov			[edi-1*JOINTQUAT_SIZE+24], ecx			// q[6] = m[2 * 4 + 3];
//
//		add			eax, JOINTMAT_SIZE
//		jl			loopMat1
//
//	done1:
//	}
//
//#elif 0
//
//	for ( int i = 0; i < numJoints; i++ ) {
//		float s0, s1, s2;
//		int k0, k1, k2, k3;
//
//		float *q = jointQuats[i].q.ToFloatPtr();
//		const float *m = jointMats[i].ToFloatPtr();
//
//		if ( m[0 * 4 + 0] + m[1 * 4 + 1] + m[2 * 4 + 2] > 0.0f ) {
//
//			k0 = 3;
//			k1 = 2;
//			k2 = 1;
//			k3 = 0;
//			s0 = 1.0f;
//			s1 = 1.0f;
//			s2 = 1.0f;
//
//		} else if ( m[0 * 4 + 0] > m[1 * 4 + 1] && m[0 * 4 + 0] > m[2 * 4 + 2] ) {
//
//			k0 = 0;
//			k1 = 1;
//			k2 = 2;
//			k3 = 3;
//			s0 = 1.0f;
//			s1 = -1.0f;
//			s2 = -1.0f;
//
//		} else if ( m[1 * 4 + 1] > m[2 * 4 + 2] ) {
//
//			k0 = 1;
//			k1 = 0;
//			k2 = 3;
//			k3 = 2;
//			s0 = -1.0f;
//			s1 = 1.0f;
//			s2 = -1.0f;
//
//		} else {
//
//			k0 = 2;
//			k1 = 3;
//			k2 = 0;
//			k3 = 1;
//			s0 = -1.0f;
//			s1 = -1.0f;
//			s2 = 1.0f;
//
//		}
//
//		float t = s0 * m[0 * 4 + 0] + s1 * m[1 * 4 + 1] + s2 * m[2 * 4 + 2] + 1.0f;
//		float s = idMath::InvSqrt( t ) * 0.5f;
//
//		q[k0] = s * t;
//		q[k1] = ( m[0 * 4 + 1] - s2 * m[1 * 4 + 0] ) * s;
//		q[k2] = ( m[2 * 4 + 0] - s1 * m[0 * 4 + 2] ) * s;
//		q[k3] = ( m[1 * 4 + 2] - s0 * m[2 * 4 + 1] ) * s;
//
//		q[4] = m[0 * 4 + 3];
//		q[5] = m[1 * 4 + 3];
//		q[6] = m[2 * 4 + 3];
//	}
//
//#elif 1
//
//	for ( int i = 0; i < numJoints; i++ ) {
//
//		float *q = jointQuats[i].q.ToFloatPtr();
//		const float *m = jointMats[i].ToFloatPtr();
//
//		if ( m[0 * 4 + 0] + m[1 * 4 + 1] + m[2 * 4 + 2] > 0.0f ) {
//
//			float t = + m[0 * 4 + 0] + m[1 * 4 + 1] + m[2 * 4 + 2] + 1.0f;
//			float s = idMath::InvSqrt( t ) * 0.5f;
//
//			q[3] = s * t;
//			q[2] = ( m[0 * 4 + 1] - m[1 * 4 + 0] ) * s;
//			q[1] = ( m[2 * 4 + 0] - m[0 * 4 + 2] ) * s;
//			q[0] = ( m[1 * 4 + 2] - m[2 * 4 + 1] ) * s;
//
//		} else if ( m[0 * 4 + 0] > m[1 * 4 + 1] && m[0 * 4 + 0] > m[2 * 4 + 2] ) {
//
//			float t = + m[0 * 4 + 0] - m[1 * 4 + 1] - m[2 * 4 + 2] + 1.0f;
//			float s = idMath::InvSqrt( t ) * 0.5f;
//
//			q[0] = s * t;
//			q[1] = ( m[0 * 4 + 1] + m[1 * 4 + 0] ) * s;
//			q[2] = ( m[2 * 4 + 0] + m[0 * 4 + 2] ) * s;
//			q[3] = ( m[1 * 4 + 2] - m[2 * 4 + 1] ) * s;
//
//		} else if ( m[1 * 4 + 1] > m[2 * 4 + 2] ) {
//
//			float t = - m[0 * 4 + 0] + m[1 * 4 + 1] - m[2 * 4 + 2] + 1.0f;
//			float s = idMath::InvSqrt( t ) * 0.5f;
//
//			q[1] = s * t;
//			q[0] = ( m[0 * 4 + 1] + m[1 * 4 + 0] ) * s;
//			q[3] = ( m[2 * 4 + 0] - m[0 * 4 + 2] ) * s;
//			q[2] = ( m[1 * 4 + 2] + m[2 * 4 + 1] ) * s;
//
//		} else {
//
//			float t = - m[0 * 4 + 0] - m[1 * 4 + 1] + m[2 * 4 + 2] + 1.0f;
//			float s = idMath::InvSqrt( t ) * 0.5f;
//
//			q[2] = s * t;
//			q[3] = ( m[0 * 4 + 1] - m[1 * 4 + 0] ) * s;
//			q[0] = ( m[2 * 4 + 0] + m[0 * 4 + 2] ) * s;
//			q[1] = ( m[1 * 4 + 2] + m[2 * 4 + 1] ) * s;
//
//		}
//
//		q[4] = m[0 * 4 + 3];
//		q[5] = m[1 * 4 + 3];
//		q[6] = m[2 * 4 + 3];
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::TransformJoints
//============
//*/
//void VPCALL idSIMD_SSE::TransformJoints( idJointMat *jointMats, const int *parents, const int firstJoint, const int lastJoint ) {
//#if 1
//
//	assert( sizeof( idJointMat ) == JOINTMAT_SIZE );
//
//	__asm {
//
//		mov			ecx, firstJoint
//		mov			eax, lastJoint
//		sub			eax, ecx
//		jl			done
//		imul		ecx, 4
//		mov			edi, parents
//		add			edi, ecx
//		imul		ecx, 12
//		mov			esi, jointMats
//		imul		eax, 4
//		add			edi, eax
//		neg			eax
//
//	loopJoint:
//
//		movaps		xmm0, [esi+ecx+ 0]						// xmm0 = m0, m1, m2, t0
//		mov			edx, [edi+eax]
//		movaps		xmm1, [esi+ecx+16]						// xmm1 = m2, m3, m4, t1
//		imul		edx, JOINTMAT_SIZE
//		movaps		xmm2, [esi+ecx+32]						// xmm2 = m5, m6, m7, t2
//
//		movss		xmm4, [esi+edx+ 0]
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm4, xmm0
//
//		movss		xmm5, [esi+edx+ 4]
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm5, xmm1
//		addps		xmm4, xmm5
//		movss		xmm6, [esi+edx+ 8]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm2
//		addps		xmm4, xmm6
//
//		movss		xmm5, [esi+edx+16]
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm5, xmm0
//
//		movss		xmm7, [esi+edx+12]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		addps		xmm4, xmm7
//
//		movaps		[esi+ecx+ 0], xmm4
//
//		movss		xmm6, [esi+edx+20]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm1
//		addps		xmm5, xmm6
//		movss		xmm7, [esi+edx+24]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm2
//		addps		xmm5, xmm7
//
//		movss		xmm6, [esi+edx+32]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm0
//
//		movss		xmm3, [esi+edx+28]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 1, 2, 3, 0 )
//		addps		xmm5, xmm3
//
//		movaps		[esi+ecx+16], xmm5
//
//		movss		xmm7, [esi+edx+36]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm1
//		addps		xmm6, xmm7
//		movss		xmm3, [esi+edx+40]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm3, xmm2
//		addps		xmm6, xmm3
//
//		movss		xmm7, [esi+edx+44]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		addps		xmm6, xmm7
//
//		movaps		[esi+ecx+32], xmm6
//
//		add			ecx, JOINTMAT_SIZE
//		add			eax, 4
//		jle			loopJoint
//	done:
//	}
//
//#else
//
//	int i;
//
//	for( i = firstJoint; i <= lastJoint; i++ ) {
//		assert( parents[i] < i );
//		jointMats[i] *= jointMats[parents[i]];
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::UntransformJoints
//============
//*/
//void VPCALL idSIMD_SSE::UntransformJoints( idJointMat *jointMats, const int *parents, const int firstJoint, const int lastJoint ) {
//#if 1
//
//	assert( sizeof( idJointMat ) == JOINTMAT_SIZE );
//
//	__asm {
//
//		mov			edx, firstJoint
//		mov			eax, lastJoint
//		mov			ecx, eax
//		sub			eax, edx
//		jl			done
//		mov			esi, jointMats
//		imul		ecx, JOINTMAT_SIZE
//		imul		edx, 4
//		mov			edi, parents
//		add			edi, edx
//		imul		eax, 4
//
//	loopJoint:
//
//		movaps		xmm0, [esi+ecx+ 0]						// xmm0 = m0, m1, m2, t0
//		mov			edx, [edi+eax]
//		movaps		xmm1, [esi+ecx+16]						// xmm1 = m2, m3, m4, t1
//		imul		edx, JOINTMAT_SIZE
//		movaps		xmm2, [esi+ecx+32]						// xmm2 = m5, m6, m7, t2
//
//		movss		xmm6, [esi+edx+12]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		subps		xmm0, xmm6
//		movss		xmm7, [esi+edx+28]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 2, 3, 0 )
//		subps		xmm1, xmm7
//		movss		xmm3, [esi+edx+44]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 1, 2, 3, 0 )
//		subps		xmm2, xmm3
//
//		movss		xmm4, [esi+edx+ 0]
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm4, xmm0
//		movss		xmm5, [esi+edx+16]
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm5, xmm1
//		addps		xmm4, xmm5
//		movss		xmm6, [esi+edx+32]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm2
//		addps		xmm4, xmm6
//
//		movaps		[esi+ecx+ 0], xmm4
//
//		movss		xmm5, [esi+edx+ 4]
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm5, xmm0
//		movss		xmm6, [esi+edx+20]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm1
//		addps		xmm5, xmm6
//		movss		xmm7, [esi+edx+36]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm2
//		addps		xmm5, xmm7
//
//		movaps		[esi+ecx+16], xmm5
//
//		movss		xmm6, [esi+edx+ 8]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm0
//		movss		xmm7, [esi+edx+24]
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm1
//		addps		xmm6, xmm7
//		movss		xmm3, [esi+edx+40]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm3, xmm2
//		addps		xmm6, xmm3
//
//		movaps		[esi+ecx+32], xmm6
//
//		sub			ecx, JOINTMAT_SIZE
//		sub			eax, 4
//		jge			loopJoint
//	done:
//	}
//
//#else
//
//	int i;
//
//	for( i = lastJoint; i >= firstJoint; i-- ) {
//		assert( parents[i] < i );
//		jointMats[i] /= jointMats[parents[i]];
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::TransformVerts
//============
//*/
//void VPCALL idSIMD_SSE::TransformVerts( idDrawVert *verts, const int numVerts, const idJointMat *joints, const idVec4 *weights, const int *index, const int numWeights ) {
//#if 1
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//	assert( sizeof( idVec4 ) == JOINTWEIGHT_SIZE );
//	assert( sizeof( idJointMat ) == JOINTMAT_SIZE );
//
//	__asm
//	{
//		mov			eax, numVerts
//		test		eax, eax
//		jz			done
//		imul		eax, DRAWVERT_SIZE
//
//		mov			ecx, verts
//		mov			edx, index
//		mov			esi, weights
//		mov			edi, joints
//
//		add			ecx, eax
//		neg			eax
//
//	loopVert:
//		mov			ebx, [edx]
//		movaps		xmm2, [esi]
//		add			edx, 8
//		movaps		xmm0, xmm2
//		add			esi, JOINTWEIGHT_SIZE
//		movaps		xmm1, xmm2
//
//		mulps		xmm0, [edi+ebx+ 0]						// xmm0 = m0, m1, m2, t0
//		mulps		xmm1, [edi+ebx+16]						// xmm1 = m3, m4, m5, t1
//		mulps		xmm2, [edi+ebx+32]						// xmm2 = m6, m7, m8, t2
//
//		cmp			dword ptr [edx-4], 0
//
//		jne			doneWeight
//
//	loopWeight:
//		mov			ebx, [edx]
//		movaps		xmm5, [esi]
//		add			edx, 8
//		movaps		xmm3, xmm5
//		add			esi, JOINTWEIGHT_SIZE
//		movaps		xmm4, xmm5
//
//		mulps		xmm3, [edi+ebx+ 0]						// xmm3 = m0, m1, m2, t0
//		mulps		xmm4, [edi+ebx+16]						// xmm4 = m3, m4, m5, t1
//		mulps		xmm5, [edi+ebx+32]						// xmm5 = m6, m7, m8, t2
//
//		cmp			dword ptr [edx-4], 0
//
//		addps		xmm0, xmm3
//		addps		xmm1, xmm4
//		addps		xmm2, xmm5
//
//		je			loopWeight
//
//	doneWeight:
//		add			eax, DRAWVERT_SIZE
//
//		movaps		xmm6, xmm0								// xmm6 =    m0,    m1,          m2,          t0
//		unpcklps	xmm6, xmm1								// xmm6 =    m0,    m3,          m1,          m4
//		unpckhps	xmm0, xmm1								// xmm1 =    m2,    m5,          t0,          t1
//		addps		xmm6, xmm0								// xmm6 = m0+m2, m3+m5,       m1+t0,       m4+t1
//
//		movaps		xmm7, xmm2								// xmm7 =    m6,    m7,          m8,          t2
//		movlhps		xmm2, xmm6								// xmm2 =    m6,    m7,       m0+m2,       m3+m5
//		movhlps		xmm6, xmm7								// xmm6 =    m8,    t2,       m1+t0,       m4+t1
//		addps		xmm6, xmm2								// xmm6 = m6+m8, m7+t2, m0+m1+m2+t0, m3+m4+m5+t1
//
//		movhps		[ecx+eax-DRAWVERT_SIZE+0], xmm6
//
//		movaps		xmm5, xmm6								// xmm5 = m6+m8, m7+t2
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 0, 2, 3 )	// xmm5 = m7+t2, m6+m8
//		addss		xmm5, xmm6								// xmm5 = m6+m8+m7+t2
//
//		movss		[ecx+eax-DRAWVERT_SIZE+8], xmm5
//
//		jl			loopVert
//	done:
//	}
//
//#else
//
//	int i, j;
//	const byte *jointsPtr = (byte *)joints;
//
//	for( j = i = 0; i < numVerts; i++ ) {
//		idVec3 v;
//
//		v = ( *(idJointMat *) ( jointsPtr + index[j*2+0] ) ) * weights[j];
//		while( index[j*2+1] == 0 ) {
//			j++;
//			v += ( *(idJointMat *) ( jointsPtr + index[j*2+0] ) ) * weights[j];
//		}
//		j++;
//
//		verts[i].xyz = v;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::TracePointCull
//============
//*/
//void VPCALL idSIMD_SSE::TracePointCull( byte *cullBits, byte &totalOr, const float radius, const idPlane *planes, const idDrawVert *verts, const int numVerts ) {
//#if 1
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//
//	__asm {
//		push		ebx
//		mov			eax, numVerts
//		test		eax, eax
//		jz			done
//
//		mov			edi, planes
//		movlps		xmm1, [edi]								// xmm1 =  0,  1,  X,  X
//		movhps		xmm1, [edi+16]							// xmm1 =  0,  1,  4,  5
//		movlps		xmm3, [edi+8]							// xmm3 =  2,  3,  X,  X
//		movhps		xmm3, [edi+24]							// xmm3 =  2,  3,  6,  7
//		movlps		xmm4, [edi+32]							// xmm4 =  8,  9,  X,  X
//		movhps		xmm4, [edi+48]							// xmm4 =  8,  9, 12, 13
//		movlps		xmm5, [edi+40]							// xmm5 = 10, 11,  X,  X
//		movhps		xmm5, [edi+56]							// xmm5 = 10, 11, 14, 15
//		movaps		xmm0, xmm1								// xmm0 =  0,  1,  4,  5
//		shufps		xmm0, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm0 =  0,  4,  8, 12
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm1 =  1,  5,  9, 13
//		movaps		xmm2, xmm3								// xmm2 =  2,  3,  6,  7
//		shufps		xmm2, xmm5, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm2 =  2,  6, 10, 14
//		shufps		xmm3, xmm5, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm3 =  3,  7, 11, 15
//		movss		xmm7, radius
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//
//		xor			edx, edx
//		mov			esi, verts
//		mov			edi, cullBits
//		imul		eax, DRAWVERT_SIZE
//		add			esi, eax
//		neg			eax
//
//	loopVert:
//		movss		xmm4, [esi+eax+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		movss		xmm5, [esi+eax+DRAWVERT_XYZ_OFFSET+4]
//		mulps		xmm4, xmm0
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		movss		xmm6, [esi+eax+DRAWVERT_XYZ_OFFSET+8]
//		mulps		xmm5, xmm1
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		addps		xmm4, xmm5
//		mulps		xmm6, xmm2
//		addps		xmm4, xmm3
//		addps		xmm4, xmm6
//		movaps		xmm5, xmm4
//		xorps		xmm5, SIMD_SP_signBitMask
//		cmpltps		xmm4, xmm7
//		movmskps	ecx, xmm4
//		cmpltps		xmm5, xmm7
//		movmskps	ebx, xmm5
//		shl			cx, 4
//		or			cl, bl
//		inc			edi
//		or			dl, cl
//		add			eax, DRAWVERT_SIZE
//		mov			byte ptr [edi-1], cl
//		jl			loopVert
//
//	done:
//		mov			esi, totalOr
//        mov			byte ptr [esi], dl
//		pop			ebx
//	}
//
//#else
//
//	int i;
//	byte tOr;
//
//	tOr = 0;
//
//	for ( i = 0; i < numVerts; i++ ) {
//		byte bits;
//		float d0, d1, d2, d3, t;
//		const idVec3 &v = verts[i].xyz;
//
//		d0 = planes[0][0] * v[0] + planes[0][1] * v[1] + planes[0][2] * v[2] + planes[0][3];
//		d1 = planes[1][0] * v[0] + planes[1][1] * v[1] + planes[1][2] * v[2] + planes[1][3];
//		d2 = planes[2][0] * v[0] + planes[2][1] * v[1] + planes[2][2] * v[2] + planes[2][3];
//		d3 = planes[3][0] * v[0] + planes[3][1] * v[1] + planes[3][2] * v[2] + planes[3][3];
//
//		t = d0 + radius;
//		bits  = FLOATSIGNBITSET( t ) << 0;
//		t = d1 + radius;
//		bits |= FLOATSIGNBITSET( t ) << 1;
//		t = d2 + radius;
//		bits |= FLOATSIGNBITSET( t ) << 2;
//		t = d3 + radius;
//		bits |= FLOATSIGNBITSET( t ) << 3;
//
//		t = d0 - radius;
//		bits |= FLOATSIGNBITSET( t ) << 4;
//		t = d1 - radius;
//		bits |= FLOATSIGNBITSET( t ) << 5;
//		t = d2 - radius;
//		bits |= FLOATSIGNBITSET( t ) << 6;
//		t = d3 - radius;
//		bits |= FLOATSIGNBITSET( t ) << 7;
//
//		bits ^= 0x0F;		// flip lower four bits
//
//		tOr |= bits;
//		cullBits[i] = bits;
//	}
//
//	totalOr = tOr;
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::DecalPointCull
//============
//*/
//void VPCALL idSIMD_SSE::DecalPointCull( byte *cullBits, const idPlane *planes, const idDrawVert *verts, const int numVerts ) {
//#if 1
//
//	ALIGN16( float p0[4] );
//	ALIGN16( float p1[4] );
//	ALIGN16( float p2[4] );
//	ALIGN16( float p3[4] );
//	ALIGN16( float p4[4] );
//	ALIGN16( float p5[4] );
//	ALIGN16( float p6[4] );
//	ALIGN16( float p7[4] );
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//
//	__asm {
//		mov			ecx, planes
//		movlps		xmm1, [ecx]								// xmm1 =  0,  1,  X,  X
//		movhps		xmm1, [ecx+16]							// xmm1 =  0,  1,  4,  5
//		movlps		xmm3, [ecx+8]							// xmm3 =  2,  3,  X,  X
//		movhps		xmm3, [ecx+24]							// xmm3 =  2,  3,  6,  7
//		movlps		xmm4, [ecx+32]							// xmm4 =  8,  9,  X,  X
//		movhps		xmm4, [ecx+48]							// xmm4 =  8,  9, 12, 13
//		movlps		xmm5, [ecx+40]							// xmm5 = 10, 11,  X,  X
//		movhps		xmm5, [ecx+56]							// xmm5 = 10, 11, 14, 15
//		movaps		xmm0, xmm1								// xmm0 =  0,  1,  4,  5
//		shufps		xmm0, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm0 =  0,  4,  8, 12
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm1 =  1,  5,  9, 13
//		movaps		xmm2, xmm3								// xmm2 =  2,  3,  6,  7
//		shufps		xmm2, xmm5, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm2 =  2,  6, 10, 14
//		shufps		xmm3, xmm5, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm3 =  3,  7, 11, 15
//
//		movaps		p0, xmm0
//		movaps		p1, xmm1
//		movaps		p2, xmm2
//		movaps		p3, xmm3
//
//		movlps		xmm4, [ecx+64]							// xmm4 = p40, p41,   X,   X
//		movhps		xmm4, [ecx+80]							// xmm4 = p40, p41, p50, p51
//		movaps		xmm5, xmm4								// xmm5 = p40, p41, p50, p51
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm4 = p40, p50, p40, p50
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm5 = p41, p51, p41, p51
//		movlps		xmm6, [ecx+72]							// xmm6 = p42, p43,   X,   X
//		movhps		xmm6, [ecx+88]							// xmm6 = p42, p43, p52, p53
//		movaps		xmm7, xmm6								// xmm7 = p42, p43, p52, p53
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 2, 0, 2 )	// xmm6 = p42, p52, p42, p52
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 1, 3, 1, 3 )	// xmm7 = p43, p53, p43, p53
//
//		movaps		p4, xmm4
//		movaps		p5, xmm5
//		movaps		p6, xmm6
//		movaps		p7, xmm7
//
//		mov			esi, verts
//		mov			edi, cullBits
//		mov			eax, numVerts
//		and			eax, ~1
//		jz			done2
//		imul		eax, DRAWVERT_SIZE
//		add			esi, eax
//		neg			eax
//
//	loopVert2:
//		movaps		xmm6, p0
//		movss		xmm0, [esi+eax+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm0
//		movaps		xmm7, p1
//		movss		xmm1, [esi+eax+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm1
//		addps		xmm6, xmm7
//		movaps		xmm7, p2
//		movss		xmm2, [esi+eax+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm2
//		addps		xmm6, xmm7
//		addps		xmm6, p3
//
//		cmpnltps	xmm6, SIMD_SP_zero
//		movmskps	ecx, xmm6
//			
//		movaps		xmm6, p0
//		movss		xmm3, [esi+eax+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm3
//		movaps		xmm7, p1
//		movss		xmm4, [esi+eax+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm4
//		addps		xmm6, xmm7
//		movaps		xmm7, p2
//		movss		xmm5, [esi+eax+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm5
//		addps		xmm6, xmm7
//		addps		xmm6, p3
//
//		cmpnltps	xmm6, SIMD_SP_zero
//		movmskps	edx, xmm6
//		mov			ch, dl
//
//		shufps		xmm0, xmm3, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm0, p4
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm1, p5
//		addps		xmm0, xmm1
//		shufps		xmm2, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm2, p6
//		addps		xmm0, xmm2
//		addps		xmm0, p7
//
//		cmpnltps	xmm0, SIMD_SP_zero
//		movmskps	edx, xmm0
//
//		add			edi, 2
//
//		mov			dh, dl
//		shl			dl, 4
//		shl			dh, 2
//		and			edx, (3<<4)|(3<<12)
//		or			ecx, edx
//
//		add			eax, 2*DRAWVERT_SIZE
//		mov			word ptr [edi-2], cx
//		jl			loopVert2
//
//	done2:
//
//		mov			eax, numVerts
//		and			eax, 1
//		jz			done
//
//		movaps		xmm6, p0
//		movss		xmm0, [esi+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm6, xmm0
//		movaps		xmm7, p1
//		movss		xmm1, [esi+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm1
//		addps		xmm6, xmm7
//		movaps		xmm7, p2
//		movss		xmm2, [esi+DRAWVERT_XYZ_OFFSET+8]
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm7, xmm2
//		addps		xmm6, xmm7
//		addps		xmm6, p3
//
//		cmpnltps	xmm6, SIMD_SP_zero
//		movmskps	ecx, xmm6
//
//		mulps		xmm0, p4
//		mulps		xmm1, p5
//		addps		xmm0, xmm1
//		mulps		xmm2, p6
//		addps		xmm0, xmm2
//		addps		xmm0, p7
//
//		cmpnltps	xmm0, SIMD_SP_zero
//		movmskps	edx, xmm0
//
//		and			edx, 3
//		shl			edx, 4
//		or			ecx, edx
//
//		mov			byte ptr [edi], cl
//
//	done:
//	}
//
//
//#else
//
//	int i;
//
//	for ( i = 0; i < numVerts; i += 2 ) {
//		unsigned short bits0, bits1;
//		float d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11;
//		const idVec3 &v0 = verts[i+0].xyz;
//		const idVec3 &v1 = verts[i+1].xyz;
//
//		d0  = planes[0][0] * v0[0] + planes[0][1] * v0[1] + planes[0][2] * v0[2] + planes[0][3];
//		d1  = planes[1][0] * v0[0] + planes[1][1] * v0[1] + planes[1][2] * v0[2] + planes[1][3];
//		d2  = planes[2][0] * v0[0] + planes[2][1] * v0[1] + planes[2][2] * v0[2] + planes[2][3];
//		d3  = planes[3][0] * v0[0] + planes[3][1] * v0[1] + planes[3][2] * v0[2] + planes[3][3];
//
//		d4  = planes[4][0] * v0[0] + planes[4][1] * v0[1] + planes[4][2] * v0[2] + planes[4][3];
//		d5  = planes[5][0] * v0[0] + planes[5][1] * v0[1] + planes[5][2] * v0[2] + planes[5][3];
//		d10 = planes[4][0] * v1[0] + planes[4][1] * v1[1] + planes[4][2] * v1[2] + planes[4][3];
//		d11 = planes[5][0] * v1[0] + planes[5][1] * v1[1] + planes[5][2] * v1[2] + planes[5][3];
//
//		d6  = planes[0][0] * v1[0] + planes[0][1] * v1[1] + planes[0][2] * v1[2] + planes[0][3];
//		d7  = planes[1][0] * v1[0] + planes[1][1] * v1[1] + planes[1][2] * v1[2] + planes[1][3];
//		d8  = planes[2][0] * v1[0] + planes[2][1] * v1[1] + planes[2][2] * v1[2] + planes[2][3];
//		d9  = planes[3][0] * v1[0] + planes[3][1] * v1[1] + planes[3][2] * v1[2] + planes[3][3];
//
//		bits0  = FLOATSIGNBITSET( d0  ) << (0+0);
//		bits0 |= FLOATSIGNBITSET( d1  ) << (0+1);
//		bits0 |= FLOATSIGNBITSET( d2  ) << (0+2);
//		bits0 |= FLOATSIGNBITSET( d3  ) << (0+3);
//		bits0 |= FLOATSIGNBITSET( d4  ) << (0+4);
//		bits0 |= FLOATSIGNBITSET( d5  ) << (0+5);
//
//		bits1  = FLOATSIGNBITSET( d6  ) << (8+0);
//		bits1 |= FLOATSIGNBITSET( d7  ) << (8+1);
//		bits1 |= FLOATSIGNBITSET( d8  ) << (8+2);
//		bits1 |= FLOATSIGNBITSET( d9  ) << (8+3);
//		bits1 |= FLOATSIGNBITSET( d10 ) << (8+4);
//		bits1 |= FLOATSIGNBITSET( d11 ) << (8+5);
//
//		*(unsigned short *)(cullBits + i) = ( bits0 | bits1 ) ^ 0x3F3F;
//	}
//
//	if ( numVerts & 1 ) {
//		byte bits;
//		float d0, d1, d2, d3, d4, d5;
//		const idVec3 &v = verts[numVerts - 1].xyz;
//
//		d0 = planes[0][0] * v[0] + planes[0][1] * v[1] + planes[0][2] * v[2] + planes[0][3];
//		d1 = planes[1][0] * v[0] + planes[1][1] * v[1] + planes[1][2] * v[2] + planes[1][3];
//		d2 = planes[2][0] * v[0] + planes[2][1] * v[1] + planes[2][2] * v[2] + planes[2][3];
//		d3 = planes[3][0] * v[0] + planes[3][1] * v[1] + planes[3][2] * v[2] + planes[3][3];
//
//		d4 = planes[4][0] * v[0] + planes[4][1] * v[1] + planes[4][2] * v[2] + planes[4][3];
//		d5 = planes[5][0] * v[0] + planes[5][1] * v[1] + planes[5][2] * v[2] + planes[5][3];
//
//		bits  = FLOATSIGNBITSET( d0 ) << 0;
//		bits |= FLOATSIGNBITSET( d1 ) << 1;
//		bits |= FLOATSIGNBITSET( d2 ) << 2;
//		bits |= FLOATSIGNBITSET( d3 ) << 3;
//
//		bits |= FLOATSIGNBITSET( d4 ) << 4;
//		bits |= FLOATSIGNBITSET( d5 ) << 5;
//
//		cullBits[numVerts - 1] = bits ^ 0x3F;		// flip lower 6 bits
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::OverlayPointCull
//============
//*/
//void VPCALL idSIMD_SSE::OverlayPointCull( byte *cullBits, idVec2 *texCoords, const idPlane *planes, const idDrawVert *verts, const int numVerts ) {
//#if 1
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//
//	__asm {
//		mov			eax, numVerts
//		mov			edx, verts
//		mov			esi, texCoords
//		mov			edi, cullBits
//
//		mov			ecx, planes
//		movss		xmm4, [ecx+ 0]
//		movss		xmm5, [ecx+16]
//		shufps		xmm4, xmm5, R_SHUFFLEPS( 0, 0, 0, 0 )
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )
//		movss		xmm5, [ecx+ 4]
//		movss		xmm6, [ecx+20]
//		shufps		xmm5, xmm6, R_SHUFFLEPS( 0, 0, 0, 0 )
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 2, 0, 2 )
//		movss		xmm6, [ecx+ 8]
//		movss		xmm7, [ecx+24]
//		shufps		xmm6, xmm7, R_SHUFFLEPS( 0, 0, 0, 0 )
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 2, 0, 2 )
//		movss		xmm7, [ecx+12]
//		movss		xmm0, [ecx+28]
//		shufps		xmm7, xmm0, R_SHUFFLEPS( 0, 0, 0, 0 )
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 0, 2, 0, 2 )
//
//		and			eax, ~1
//		jz			done2
//		add			edi, eax
//		neg			eax
//
//	loopVert2:
//		movss		xmm0, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm1, [edx+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm0, xmm4
//		movss		xmm1, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm2, [edx+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm1, xmm2, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm1, xmm5
//		movss		xmm2, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm3, [edx+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		shufps		xmm2, xmm3, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm2, xmm6
//		addps		xmm0, xmm1
//		addps		xmm0, xmm2
//		addps		xmm0, xmm7
//		movaps		[esi], xmm0
//		movaps		xmm1, xmm0
//		movaps		xmm2, SIMD_SP_one
//		subps		xmm2, xmm0
//		shufps		xmm0, xmm2, R_SHUFFLEPS( 0, 1, 0, 1 )
//		shufps		xmm1, xmm2, R_SHUFFLEPS( 2, 3, 2, 3 )
//		add			edx, 2*DRAWVERT_SIZE
//		movmskps	ecx, xmm0
//		mov			byte ptr [edi+eax+0], cl
//		add			esi, 4*4
//		movmskps	ecx, xmm1
//		mov			byte ptr [edi+eax+1], cl
//		add			eax, 2
//		jl			loopVert2
//
//	done2:
//		mov			eax, numVerts
//		and			eax, 1
//		jz			done
//
//		movss		xmm0, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm0, xmm4
//		movss		xmm1, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm1, xmm5
//		movss		xmm2, [edx+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm2, xmm6
//		addps		xmm0, xmm1
//		addps		xmm0, xmm2
//		addps		xmm0, xmm7
//		movlps		[esi], xmm0
//		movaps		xmm1, xmm0
//		movaps		xmm2, SIMD_SP_one
//		subps		xmm2, xmm0
//		shufps		xmm0, xmm2, R_SHUFFLEPS( 0, 1, 0, 1 )
//		movmskps	ecx, xmm0
//		mov			byte ptr [edi], cl
//
//	done:
//	}
//
//#else
//
//	const idPlane &p0 = planes[0];
//	const idPlane &p1 = planes[1];
//
//	for ( int i = 0; i < numVerts - 1; i += 2 ) {
//		unsigned short bits;
//		float d0, d1, d2, d3;
//
//		const idVec3 &v0 = verts[i+0].xyz;
//		const idVec3 &v1 = verts[i+1].xyz;
//
//		d0 = p0[0] * v0[0] + p0[1] * v0[1] + p0[2] * v0[2] + p0[3];
//		d1 = p1[0] * v0[0] + p1[1] * v0[1] + p1[2] * v0[2] + p1[3];
//		d2 = p0[0] * v1[0] + p0[1] * v1[1] + p0[2] * v1[2] + p0[3];
//		d3 = p1[0] * v1[0] + p1[1] * v1[1] + p1[2] * v1[2] + p1[3];
//
//		texCoords[i+0][0] = d0;
//		texCoords[i+0][1] = d1;
//		texCoords[i+1][0] = d2;
//		texCoords[i+1][1] = d3;
//
//		bits  = FLOATSIGNBITSET( d0 ) << 0;
//		bits |= FLOATSIGNBITSET( d1 ) << 1;
//		bits |= FLOATSIGNBITSET( d2 ) << 8;
//		bits |= FLOATSIGNBITSET( d3 ) << 9;
//
//		d0 = 1.0f - d0;
//		d1 = 1.0f - d1;
//		d2 = 1.0f - d2;
//		d3 = 1.0f - d3;
//
//		bits |= FLOATSIGNBITSET( d0 ) << 2;
//		bits |= FLOATSIGNBITSET( d1 ) << 3;
//		bits |= FLOATSIGNBITSET( d2 ) << 10;
//		bits |= FLOATSIGNBITSET( d3 ) << 11;
//
//		*(unsigned short *)(cullBits + i) = bits;
//	}
//
//	if ( numVerts & 1 ) {
//		byte bits;
//		float d0, d1;
//
//		const idPlane &p0 = planes[0];
//		const idPlane &p1 = planes[1];
//		const idVec3 &v0 = verts[numVerts - 1].xyz;
//
//		d0 = p0[0] * v0[0] + p0[1] * v0[1] + p0[2] * v0[2] + p0[3];
//		d1 = p1[0] * v0[0] + p1[1] * v0[1] + p1[2] * v0[2] + p1[3];
//
//		texCoords[i][0] = d0;
//		texCoords[i][1] = d1;
//
//		bits  = FLOATSIGNBITSET( d0 ) << 0;
//		bits |= FLOATSIGNBITSET( d1 ) << 1;
//
//		d0 = 1.0f - d0;
//		d1 = 1.0f - d1;
//
//		bits |= FLOATSIGNBITSET( d0 ) << 2;
//		bits |= FLOATSIGNBITSET( d1 ) << 3;
//
//		cullBits[numVerts - 1] = bits;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::DeriveTriPlanes
//============
//*/
//void VPCALL idSIMD_SSE::DeriveTriPlanes( idPlane *planes, const idDrawVert *verts, const int numVerts, const int *indexes, const int numIndexes ) {
//#if 1
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//
//	__asm {
//		mov			eax, numIndexes
//		shl			eax, 2
//		mov			esi, verts
//		mov			edi, indexes
//		mov			edx, planes
//
//		add			edi, eax
//		neg			eax
//
//		add			eax, 4*12
//		jge			done4
//
//	loopPlane4:
//		mov			ebx, [edi+eax-4*12+4]
//		imul		ebx, DRAWVERT_SIZE
//		mov			ecx, [edi+eax-4*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		movss		xmm0, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//
//		movss		xmm1, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//
//		movss		xmm2, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		mov			ebx, [edi+eax-4*12+8]
//		imul		ebx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm3, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm3, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//
//		movss		xmm4, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm4, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//
//		movss		xmm5, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm5, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		mov			ebx, [edi+eax-3*12+4]
//		imul		ebx, DRAWVERT_SIZE
//		mov			ecx, [edi+eax-3*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm0, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm1, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm2, xmm6
//
//		mov			ebx, [edi+eax-3*12+8]
//		imul		ebx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm3, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm4, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm5, xmm7
//
//		mov			ebx, [edi+eax-2*12+4]
//		imul		ebx, DRAWVERT_SIZE
//		mov			ecx, [edi+eax-2*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm0, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm1, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm2, xmm6
//
//		mov			ebx, [edi+eax-2*12+8]
//		imul		ebx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm3, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm4, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm5, xmm7
//
//		mov			ebx, [edi+eax-1*12+4]
//		imul		ebx, DRAWVERT_SIZE
//		mov			ecx, [edi+eax-1*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 3, 0, 1, 2 )
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 3, 0, 1, 2 )
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm0, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm1, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm2, xmm6
//
//		mov			ebx, [edi+eax-1*12+8]
//		imul		ebx, DRAWVERT_SIZE
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		movss		xmm3, xmm7
//
//		movss		xmm6, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm6, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		movss		xmm4, xmm6
//
//		movss		xmm7, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm7, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//		movss		xmm5, xmm7
//
//		movaps		xmm6, xmm4
//		mulps		xmm6, xmm2
//		movaps		xmm7, xmm5
//		mulps		xmm7, xmm1
//		subps		xmm6, xmm7
//
//		mulps		xmm5, xmm0
//		mulps		xmm2, xmm3
//		subps		xmm5, xmm2
//
//		mulps		xmm3, xmm1
//		mulps		xmm4, xmm0
//		subps		xmm3, xmm4
//
//		movaps		xmm0, xmm6
//		mulps		xmm6, xmm6
//		movaps		xmm1, xmm5
//		mulps		xmm5, xmm5
//		movaps		xmm2, xmm3
//		mulps		xmm3, xmm3
//
//		addps		xmm3, xmm5
//		addps		xmm3, xmm6
//		rsqrtps		xmm3, xmm3
//
//		add			edx, 4*16
//		mov			ecx, [edi+eax-1*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		mulps		xmm0, xmm3
//		mulps		xmm1, xmm3
//		mulps		xmm2, xmm3
//
//		movss		[edx-1*16+0], xmm0
//		movss		[edx-1*16+4], xmm1
//		movss		[edx-1*16+8], xmm2
//
//		mulss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		mulss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		mulss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		xorps		xmm0, SIMD_SP_singleSignBitMask
//		subss		xmm0, xmm1
//		subss		xmm0, xmm2
//		movss		[edx-1*16+12], xmm0
//
//		mov			ecx, [edi+eax-2*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[edx-2*16+0], xmm0
//		movss		[edx-2*16+4], xmm1
//		movss		[edx-2*16+8], xmm2
//
//		mulss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		mulss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		mulss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		xorps		xmm0, SIMD_SP_singleSignBitMask
//		subss		xmm0, xmm1
//		subss		xmm0, xmm2
//		movss		[edx-2*16+12], xmm0
//
//		mov			ecx, [edi+eax-3*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[edx-3*16+0], xmm0
//		movss		[edx-3*16+4], xmm1
//		movss		[edx-3*16+8], xmm2
//
//		mulss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		mulss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		mulss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		xorps		xmm0, SIMD_SP_singleSignBitMask
//		subss		xmm0, xmm1
//		subss		xmm0, xmm2
//		movss		[edx-3*16+12], xmm0
//
//		mov			ecx, [edi+eax-4*12+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[edx-4*16+0], xmm0
//		movss		[edx-4*16+4], xmm1
//		movss		[edx-4*16+8], xmm2
//
//		mulss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		mulss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		mulss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		xorps		xmm0, SIMD_SP_singleSignBitMask
//		subss		xmm0, xmm1
//		subss		xmm0, xmm2
//		movss		[edx-4*16+12], xmm0
//
//		add			eax, 4*12
//		jle			loopPlane4
//
//	done4:
//
//		sub			eax, 4*12
//		jge			done
//
//	loopPlane1:
//		mov			ebx, [edi+eax+4]
//		imul		ebx, DRAWVERT_SIZE
//		mov			ecx, [edi+eax+0]
//		imul		ecx, DRAWVERT_SIZE
//
//		movss		xmm0, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//
//		movss		xmm1, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//
//		movss		xmm2, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		mov			ebx, [edi+eax+8]
//		imul		ebx, DRAWVERT_SIZE
//
//		movss		xmm3, [esi+ebx+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm3, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//
//		movss		xmm4, [esi+ebx+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm4, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//
//		movss		xmm5, [esi+ebx+DRAWVERT_XYZ_OFFSET+8]
//		subss		xmm5, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		movss		xmm6, xmm4
//		mulss		xmm6, xmm2
//		movss		xmm7, xmm5
//		mulss		xmm7, xmm1
//		subss		xmm6, xmm7
//
//		mulss		xmm5, xmm0
//		mulss		xmm2, xmm3
//		subss		xmm5, xmm2
//
//		mulss		xmm3, xmm1
//		mulss		xmm4, xmm0
//		subss		xmm3, xmm4
//
//		movss		xmm0, xmm6
//		mulss		xmm6, xmm6
//		movss		xmm1, xmm5
//		mulss		xmm5, xmm5
//		movss		xmm2, xmm3
//		mulss		xmm3, xmm3
//
//		addss		xmm3, xmm5
//		addss		xmm3, xmm6
//		rsqrtss		xmm3, xmm3
//
//		add			edx, 1*16
//
//		mulss		xmm0, xmm3
//		mulss		xmm1, xmm3
//		mulss		xmm2, xmm3
//
//		movss		[edx-1*16+0], xmm0
//		movss		[edx-1*16+4], xmm1
//		movss		[edx-1*16+8], xmm2
//
//		mulss		xmm0, [esi+ecx+DRAWVERT_XYZ_OFFSET+0]
//		mulss		xmm1, [esi+ecx+DRAWVERT_XYZ_OFFSET+4]
//		mulss		xmm2, [esi+ecx+DRAWVERT_XYZ_OFFSET+8]
//
//		xorps		xmm0, SIMD_SP_singleSignBitMask
//		subss		xmm0, xmm1
//		subss		xmm0, xmm2
//		movss		[edx-1*16+12], xmm0
//
//		add			eax, 1*12
//		jl			loopPlane1
//
//	done:
//	}
//
//#else
//
//	int i, j;
//
//	for ( i = 0; i <= numIndexes - 12; i += 12 ) {
//		ALIGN16( float d0[4] );
//		ALIGN16( float d1[4] );
//		ALIGN16( float d2[4] );
//		ALIGN16( float d3[4] );
//		ALIGN16( float d4[4] );
//		ALIGN16( float d5[4] );
//		ALIGN16( float n0[4] );
//		ALIGN16( float n1[4] );
//		ALIGN16( float n2[4] );
//
//		for ( j = 0; j < 4; j++ ) {
//			const idDrawVert *a, *b, *c;
//
//			a = verts + indexes[i + j * 3 + 0];
//			b = verts + indexes[i + j * 3 + 1];
//			c = verts + indexes[i + j * 3 + 2];
//
//			d0[j] = b->xyz[0] - a->xyz[0];
//			d1[j] = b->xyz[1] - a->xyz[1];
//			d2[j] = b->xyz[2] - a->xyz[2];
//
//			d3[j] = c->xyz[0] - a->xyz[0];
//			d4[j] = c->xyz[1] - a->xyz[1];
//			d5[j] = c->xyz[2] - a->xyz[2];
//		}
//
//		ALIGN16( float tmp[4] );
//
//		n0[0] = d4[0] * d2[0];
//		n0[1] = d4[1] * d2[1];
//		n0[2] = d4[2] * d2[2];
//		n0[3] = d4[3] * d2[3];
//
//		n0[0] -= d5[0] * d1[0];
//		n0[1] -= d5[1] * d1[1];
//		n0[2] -= d5[2] * d1[2];
//		n0[3] -= d5[3] * d1[3];
//
//		n1[0] = d5[0] * d0[0];
//		n1[1] = d5[1] * d0[1];
//		n1[2] = d5[2] * d0[2];
//		n1[3] = d5[3] * d0[3];
//
//		n1[0] -= d3[0] * d2[0];
//		n1[1] -= d3[1] * d2[1];
//		n1[2] -= d3[2] * d2[2];
//		n1[3] -= d3[3] * d2[3];
//
//		n2[0] = d3[0] * d1[0];
//		n2[1] = d3[1] * d1[1];
//		n2[2] = d3[2] * d1[2];
//		n2[3] = d3[3] * d1[3];
//
//		n2[0] -= d4[0] * d0[0];
//		n2[1] -= d4[1] * d0[1];
//		n2[2] -= d4[2] * d0[2];
//		n2[3] -= d4[3] * d0[3];
//
//		tmp[0] = n0[0] * n0[0];
//		tmp[1] = n0[1] * n0[1];
//		tmp[2] = n0[2] * n0[2];
//		tmp[3] = n0[3] * n0[3];
//
//		tmp[0] += n1[0] * n1[0];
//		tmp[1] += n1[1] * n1[1];
//		tmp[2] += n1[2] * n1[2];
//		tmp[3] += n1[3] * n1[3];
//
//		tmp[0] += n2[0] * n2[0];
//		tmp[1] += n2[1] * n2[1];
//		tmp[2] += n2[2] * n2[2];
//		tmp[3] += n2[3] * n2[3];
//
//		tmp[0] = idMath::RSqrt( tmp[0] );
//		tmp[1] = idMath::RSqrt( tmp[1] );
//		tmp[2] = idMath::RSqrt( tmp[2] );
//		tmp[3] = idMath::RSqrt( tmp[3] );
//
//		n0[0] *= tmp[0];
//		n0[1] *= tmp[1];
//		n0[2] *= tmp[2];
//		n0[3] *= tmp[3];
//
//		n1[0] *= tmp[0];
//		n1[1] *= tmp[1];
//		n1[2] *= tmp[2];
//		n1[3] *= tmp[3];
//
//		n2[0] *= tmp[0];
//		n2[1] *= tmp[1];
//		n2[2] *= tmp[2];
//		n2[3] *= tmp[3];
//
//
//		for ( j = 0; j < 4; j++ ) {
//			const idDrawVert *a;
//
//			a = verts + indexes[i + j * 3];
//
//			planes->Normal()[0] = n0[j];
//			planes->Normal()[1] = n1[j];
//			planes->Normal()[2] = n2[j];
//			planes->FitThroughPoint( a->xyz );
//			planes++;
//		}
//	}
//
//	for ( ; i < numIndexes; i += 3 ) {
//		const idDrawVert *a, *b, *c;
//		float d0, d1, d2, d3, d4, d5;
//		float n0, n1, n2;
//
//		a = verts + indexes[i + 0];
//		b = verts + indexes[i + 1];
//		c = verts + indexes[i + 2];
//
//		d0 = b->xyz[0] - a->xyz[0];
//		d1 = b->xyz[1] - a->xyz[1];
//		d2 = b->xyz[2] - a->xyz[2];
//
//		d3 = c->xyz[0] - a->xyz[0];
//		d4 = c->xyz[1] - a->xyz[1];
//		d5 = c->xyz[2] - a->xyz[2];
//
//		float tmp;
//
//		n0 = d4 * d2 - d5 * d1;
//		n1 = d5 * d0 - d3 * d2;
//		n2 = d3 * d1 - d4 * d0;
//
//		tmp = idMath::RSqrt( n0 * n0 + n1 * n1 + n2 * n2 );
//
//		n0 *= tmp;
//		n1 *= tmp;
//		n2 *= tmp;
//
//		planes->Normal()[0] = n0;
//		planes->Normal()[1] = n1;
//		planes->Normal()[2] = n2;
//		planes->FitThroughPoint( a->xyz );
//		planes++;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::DeriveTangents
//============
//*/
////#define REFINE_TANGENT_SQUAREROOT
//#define FIX_DEGENERATE_TANGENT
//
//void VPCALL idSIMD_SSE::DeriveTangents( idPlane *planes, idDrawVert *verts, const int numVerts, const int *indexes, const int numIndexes ) {
//	int i;
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->normal == DRAWVERT_NORMAL_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[0] == DRAWVERT_TANGENT0_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[1] == DRAWVERT_TANGENT1_OFFSET );
//
//	assert( planes != NULL );
//	assert( verts != NULL );
//	assert( numVerts >= 0 );
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//	__asm {
//		movaps		xmm6, SIMD_SP_rsqrt_c0
//		movaps		xmm7, SIMD_SP_rsqrt_c1
//	}
//#endif
//
//	bool *used = (bool *)_alloca16( numVerts * sizeof( used[0] ) );
//	memset( used, 0, numVerts * sizeof( used[0] ) );
//
//	for ( i = 0; i <= numIndexes - 12; i += 12 ) {
//		idDrawVert *a, *b, *c;
//		ALIGN16( unsigned long signBit[4] );
//		ALIGN16( float d0[4] );
//		ALIGN16( float d1[4] );
//		ALIGN16( float d2[4] );
//		ALIGN16( float d3[4] );
//		ALIGN16( float d4[4] );
//		ALIGN16( float d5[4] );
//		ALIGN16( float d6[4] );
//		ALIGN16( float d7[4] );
//		ALIGN16( float d8[4] );
//		ALIGN16( float d9[4] );
//		ALIGN16( float n0[4] );
//		ALIGN16( float n1[4] );
//		ALIGN16( float n2[4] );
//		ALIGN16( float t0[4] );
//		ALIGN16( float t1[4] );
//		ALIGN16( float t2[4] );
//		ALIGN16( float t3[4] );
//		ALIGN16( float t4[4] );
//		ALIGN16( float t5[4] );
//
//		for ( int j = 0; j < 4; j++ ) {
//
//			a = verts + indexes[i + j * 3 + 0];
//			b = verts + indexes[i + j * 3 + 1];
//			c = verts + indexes[i + j * 3 + 2];
//
//			d0[j] = b->xyz[0] - a->xyz[0];
//			d1[j] = b->xyz[1] - a->xyz[1];
//			d2[j] = b->xyz[2] - a->xyz[2];
//			d3[j] = b->st[0] - a->st[0];
//			d4[j] = b->st[1] - a->st[1];
//
//			d5[j] = c->xyz[0] - a->xyz[0];
//			d6[j] = c->xyz[1] - a->xyz[1];
//			d7[j] = c->xyz[2] - a->xyz[2];
//			d8[j] = c->st[0] - a->st[0];
//			d9[j] = c->st[1] - a->st[1];
//		}
//
//#if 1
//
//		__asm {
//			// normal
//			movaps		xmm0, d6
//			mulps		xmm0, d2
//			movaps		xmm1, d7
//			mulps		xmm1, d1
//			subps		xmm0, xmm1
//
//			movaps		xmm1, d7
//			mulps		xmm1, d0
//			movaps		xmm2, d5
//			mulps		xmm2, d2
//			subps		xmm1, xmm2
//
//			movaps		xmm2, d5
//			mulps		xmm2, d1
//			movaps		xmm3, d6
//			mulps		xmm3, d0
//			subps		xmm2, xmm3
//
//			movaps		xmm3, xmm0
//			movaps		xmm4, xmm1
//			movaps		xmm5, xmm2
//
//			mulps		xmm3, xmm3
//			mulps		xmm4, xmm4
//			mulps		xmm5, xmm5
//
//			addps		xmm3, xmm4
//			addps		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtps		xmm4, xmm3
//			mulps		xmm3, xmm4
//			mulps		xmm3, xmm4
//			subps		xmm3, xmm6
//			mulps		xmm4, xmm7
//			mulps		xmm3, xmm4
//#else
//			rsqrtps		xmm3, xmm3
//#endif
//			mulps		xmm0, xmm3
//			movaps		n0, xmm0
//			mulps		xmm1, xmm3
//			movaps		n1, xmm1
//			mulps		xmm2, xmm3
//			movaps		n2, xmm2
//
//			// area sign bit
//			movaps		xmm0, d3
//			mulps		xmm0, d9
//			movaps		xmm1, d4
//			mulps		xmm1, d8
//			subps		xmm0, xmm1
//			andps		xmm0, SIMD_SP_signBitMask
//			movaps		signBit, xmm0
//
//			// first tangent
//			movaps		xmm0, d0
//			mulps		xmm0, d9
//			movaps		xmm1, d4
//			mulps		xmm1, d5
//			subps		xmm0, xmm1
//
//			movaps		xmm1, d1
//			mulps		xmm1, d9
//			movaps		xmm2, d4
//			mulps		xmm2, d6
//			subps		xmm1, xmm2
//
//			movaps		xmm2, d2
//			mulps		xmm2, d9
//			movaps		xmm3, d4
//			mulps		xmm3, d7
//			subps		xmm2, xmm3
//
//			movaps		xmm3, xmm0
//			movaps		xmm4, xmm1
//			movaps		xmm5, xmm2
//
//			mulps		xmm3, xmm3
//			mulps		xmm4, xmm4
//			mulps		xmm5, xmm5
//
//			addps		xmm3, xmm4
//			addps		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtps		xmm4, xmm3
//			mulps		xmm3, xmm4
//			mulps		xmm3, xmm4
//			subps		xmm3, xmm6
//			mulps		xmm4, xmm7
//			mulps		xmm3, xmm4
//#else
//			rsqrtps		xmm3, xmm3
//#endif
//			xorps		xmm3, signBit
//
//			mulps		xmm0, xmm3
//			movaps		t0, xmm0
//			mulps		xmm1, xmm3
//			movaps		t1, xmm1
//			mulps		xmm2, xmm3
//			movaps		t2, xmm2
//
//			// second tangent
//			movaps		xmm0, d3
//			mulps		xmm0, d5
//			movaps		xmm1, d0
//			mulps		xmm1, d8
//			subps		xmm0, xmm1
//
//			movaps		xmm1, d3
//			mulps		xmm1, d6
//			movaps		xmm2, d1
//			mulps		xmm2, d8
//			subps		xmm1, xmm2
//
//			movaps		xmm2, d3
//			mulps		xmm2, d7
//			movaps		xmm3, d2
//			mulps		xmm3, d8
//			subps		xmm2, xmm3
//
//			movaps		xmm3, xmm0
//			movaps		xmm4, xmm1
//			movaps		xmm5, xmm2
//
//			mulps		xmm3, xmm3
//			mulps		xmm4, xmm4
//			mulps		xmm5, xmm5
//
//			addps		xmm3, xmm4
//			addps		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtps		xmm4, xmm3
//			mulps		xmm3, xmm4
//			mulps		xmm3, xmm4
//			subps		xmm3, xmm6
//			mulps		xmm4, xmm7
//			mulps		xmm3, xmm4
//#else
//			rsqrtps		xmm3, xmm3
//#endif
//			xorps		xmm3, signBit
//
//			mulps		xmm0, xmm3
//			movaps		t3, xmm0
//			mulps		xmm1, xmm3
//			movaps		t4, xmm1
//			mulps		xmm2, xmm3
//			movaps		t5, xmm2
//		}
//
//#else
//
//		ALIGN16( float tmp[4] );
//
//		// normal
//		n0[0] = d6[0] * d2[0];
//		n0[1] = d6[1] * d2[1];
//		n0[2] = d6[2] * d2[2];
//		n0[3] = d6[3] * d2[3];
//
//		n0[0] -= d7[0] * d1[0];
//		n0[1] -= d7[1] * d1[1];
//		n0[2] -= d7[2] * d1[2];
//		n0[3] -= d7[3] * d1[3];
//
//		n1[0] = d7[0] * d0[0];
//		n1[1] = d7[1] * d0[1];
//		n1[2] = d7[2] * d0[2];
//		n1[3] = d7[3] * d0[3];
//
//		n1[0] -= d5[0] * d2[0];
//		n1[1] -= d5[1] * d2[1];
//		n1[2] -= d5[2] * d2[2];
//		n1[3] -= d5[3] * d2[3];
//
//		n2[0] = d5[0] * d1[0];
//		n2[1] = d5[1] * d1[1];
//		n2[2] = d5[2] * d1[2];
//		n2[3] = d5[3] * d1[3];
//
//		n2[0] -= d6[0] * d0[0];
//		n2[1] -= d6[1] * d0[1];
//		n2[2] -= d6[2] * d0[2];
//		n2[3] -= d6[3] * d0[3];
//
//		tmp[0] = n0[0] * n0[0];
//		tmp[1] = n0[1] * n0[1];
//		tmp[2] = n0[2] * n0[2];
//		tmp[3] = n0[3] * n0[3];
//
//		tmp[0] += n1[0] * n1[0];
//		tmp[1] += n1[1] * n1[1];
//		tmp[2] += n1[2] * n1[2];
//		tmp[3] += n1[3] * n1[3];
//
//		tmp[0] += n2[0] * n2[0];
//		tmp[1] += n2[1] * n2[1];
//		tmp[2] += n2[2] * n2[2];
//		tmp[3] += n2[3] * n2[3];
//
//		tmp[0] = idMath::RSqrt( tmp[0] );
//		tmp[1] = idMath::RSqrt( tmp[1] );
//		tmp[2] = idMath::RSqrt( tmp[2] );
//		tmp[3] = idMath::RSqrt( tmp[3] );
//
//		n0[0] *= tmp[0];
//		n0[1] *= tmp[1];
//		n0[2] *= tmp[2];
//		n0[3] *= tmp[3];
//
//		n1[0] *= tmp[0];
//		n1[1] *= tmp[1];
//		n1[2] *= tmp[2];
//		n1[3] *= tmp[3];
//
//		n2[0] *= tmp[0];
//		n2[1] *= tmp[1];
//		n2[2] *= tmp[2];
//		n2[3] *= tmp[3];
//
//		// area sign bit
//		tmp[0] = d3[0] * d9[0];
//		tmp[1] = d3[1] * d9[1];
//		tmp[2] = d3[2] * d9[2];
//		tmp[3] = d3[3] * d9[3];
//
//		tmp[0] -= d4[0] * d8[0];
//		tmp[1] -= d4[1] * d8[1];
//		tmp[2] -= d4[2] * d8[2];
//		tmp[3] -= d4[3] * d8[3];
//
//		signBit[0] = ( *(unsigned long *)&tmp[0] ) & ( 1 << 31 );
//		signBit[1] = ( *(unsigned long *)&tmp[1] ) & ( 1 << 31 );
//		signBit[2] = ( *(unsigned long *)&tmp[2] ) & ( 1 << 31 );
//		signBit[3] = ( *(unsigned long *)&tmp[3] ) & ( 1 << 31 );
//
//		// first tangent
//		t0[0] = d0[0] * d9[0];
//		t0[1] = d0[1] * d9[1];
//		t0[2] = d0[2] * d9[2];
//		t0[3] = d0[3] * d9[3];
//
//		t0[0] -= d4[0] * d5[0];
//		t0[1] -= d4[1] * d5[1];
//		t0[2] -= d4[2] * d5[2];
//		t0[3] -= d4[3] * d5[3];
//
//		t1[0] = d1[0] * d9[0];
//		t1[1] = d1[1] * d9[1];
//		t1[2] = d1[2] * d9[2];
//		t1[3] = d1[3] * d9[3];
//
//		t1[0] -= d4[0] * d6[0];
//		t1[1] -= d4[1] * d6[1];
//		t1[2] -= d4[2] * d6[2];
//		t1[3] -= d4[3] * d6[3];
//
//		t2[0] = d2[0] * d9[0];
//		t2[1] = d2[1] * d9[1];
//		t2[2] = d2[2] * d9[2];
//		t2[3] = d2[3] * d9[3];
//
//		t2[0] -= d4[0] * d7[0];
//		t2[1] -= d4[1] * d7[1];
//		t2[2] -= d4[2] * d7[2];
//		t2[3] -= d4[3] * d7[3];
//
//		tmp[0] = t0[0] * t0[0];
//		tmp[1] = t0[1] * t0[1];
//		tmp[2] = t0[2] * t0[2];
//		tmp[3] = t0[3] * t0[3];
//
//		tmp[0] += t1[0] * t1[0];
//		tmp[1] += t1[1] * t1[1];
//		tmp[2] += t1[2] * t1[2];
//		tmp[3] += t1[3] * t1[3];
//
//		tmp[0] += t2[0] * t2[0];
//		tmp[1] += t2[1] * t2[1];
//		tmp[2] += t2[2] * t2[2];
//		tmp[3] += t2[3] * t2[3];
//
//		tmp[0] = idMath::RSqrt( tmp[0] );
//		tmp[1] = idMath::RSqrt( tmp[1] );
//		tmp[2] = idMath::RSqrt( tmp[2] );
//		tmp[3] = idMath::RSqrt( tmp[3] );
//
//		*(unsigned long *)&tmp[0] ^= signBit[0];
//		*(unsigned long *)&tmp[1] ^= signBit[1];
//		*(unsigned long *)&tmp[2] ^= signBit[2];
//		*(unsigned long *)&tmp[3] ^= signBit[3];
//
//		t0[0] *= tmp[0];
//		t0[1] *= tmp[1];
//		t0[2] *= tmp[2];
//		t0[3] *= tmp[3];
//
//		t1[0] *= tmp[0];
//		t1[1] *= tmp[1];
//		t1[2] *= tmp[2];
//		t1[3] *= tmp[3];
//
//		t2[0] *= tmp[0];
//		t2[1] *= tmp[1];
//		t2[2] *= tmp[2];
//		t2[3] *= tmp[3];
//
//		// second tangent
//		t3[0] = d3[0] * d5[0];
//		t3[1] = d3[1] * d5[1];
//		t3[2] = d3[2] * d5[2];
//		t3[3] = d3[3] * d5[3];
//
//		t3[0] -= d0[0] * d8[0];
//		t3[1] -= d0[1] * d8[1];
//		t3[2] -= d0[2] * d8[2];
//		t3[3] -= d0[3] * d8[3];
//
//		t4[0] = d3[0] * d6[0];
//		t4[1] = d3[1] * d6[1];
//		t4[2] = d3[2] * d6[2];
//		t4[3] = d3[3] * d6[3];
//
//		t4[0] -= d1[0] * d8[0];
//		t4[1] -= d1[1] * d8[1];
//		t4[2] -= d1[2] * d8[2];
//		t4[3] -= d1[3] * d8[3];
//
//		t5[0] = d3[0] * d7[0];
//		t5[1] = d3[1] * d7[1];
//		t5[2] = d3[2] * d7[2];
//		t5[3] = d3[3] * d7[3];
//
//		t5[0] -= d2[0] * d8[0];
//		t5[1] -= d2[1] * d8[1];
//		t5[2] -= d2[2] * d8[2];
//		t5[3] -= d2[3] * d8[3];
//
//		tmp[0] = t3[0] * t3[0];
//		tmp[1] = t3[1] * t3[1];
//		tmp[2] = t3[2] * t3[2];
//		tmp[3] = t3[3] * t3[3];
//
//		tmp[0] += t4[0] * t4[0];
//		tmp[1] += t4[1] * t4[1];
//		tmp[2] += t4[2] * t4[2];
//		tmp[3] += t4[3] * t4[3];
//
//		tmp[0] += t5[0] * t5[0];
//		tmp[1] += t5[1] * t5[1];
//		tmp[2] += t5[2] * t5[2];
//		tmp[3] += t5[3] * t5[3];
//
//		tmp[0] = idMath::RSqrt( tmp[0] );
//		tmp[1] = idMath::RSqrt( tmp[1] );
//		tmp[2] = idMath::RSqrt( tmp[2] );
//		tmp[3] = idMath::RSqrt( tmp[3] );
//
//		*(unsigned long *)&tmp[0] ^= signBit[0];
//		*(unsigned long *)&tmp[1] ^= signBit[1];
//		*(unsigned long *)&tmp[2] ^= signBit[2];
//		*(unsigned long *)&tmp[3] ^= signBit[3];
//
//		t3[0] *= tmp[0];
//		t3[1] *= tmp[1];
//		t3[2] *= tmp[2];
//		t3[3] *= tmp[3];
//
//		t4[0] *= tmp[0];
//		t4[1] *= tmp[1];
//		t4[2] *= tmp[2];
//		t4[3] *= tmp[3];
//
//		t5[0] *= tmp[0];
//		t5[1] *= tmp[1];
//		t5[2] *= tmp[2];
//		t5[3] *= tmp[3];
//
//#endif
//
//		for ( int j = 0; j < 4; j++ ) {
//
//			const int v0 = indexes[i + j * 3 + 0];
//			const int v1 = indexes[i + j * 3 + 1];
//			const int v2 = indexes[i + j * 3 + 2];
//
//			a = verts + v0;
//			b = verts + v1;
//			c = verts + v2;
//
//			planes->Normal()[0] = n0[j];
//			planes->Normal()[1] = n1[j];
//			planes->Normal()[2] = n2[j];
//			planes->FitThroughPoint( a->xyz );
//			planes++;
//
//			if ( used[v0] ) {
//				a->normal[0] += n0[j];
//				a->normal[1] += n1[j];
//				a->normal[2] += n2[j];
//
//				a->tangents[0][0] += t0[j];
//				a->tangents[0][1] += t1[j];
//				a->tangents[0][2] += t2[j];
//
//				a->tangents[1][0] += t3[j];
//				a->tangents[1][1] += t4[j];
//				a->tangents[1][2] += t5[j];
//			} else {
//				a->normal[0] = n0[j];
//				a->normal[1] = n1[j];
//				a->normal[2] = n2[j];
//
//				a->tangents[0][0] = t0[j];
//				a->tangents[0][1] = t1[j];
//				a->tangents[0][2] = t2[j];
//
//				a->tangents[1][0] = t3[j];
//				a->tangents[1][1] = t4[j];
//				a->tangents[1][2] = t5[j];
//
//				used[v0] = true;
//			}
//
//			if ( used[v1] ) {
//				b->normal[0] += n0[j];
//				b->normal[1] += n1[j];
//				b->normal[2] += n2[j];
//
//				b->tangents[0][0] += t0[j];
//				b->tangents[0][1] += t1[j];
//				b->tangents[0][2] += t2[j];
//
//				b->tangents[1][0] += t3[j];
//				b->tangents[1][1] += t4[j];
//				b->tangents[1][2] += t5[j];
//			} else {
//				b->normal[0] = n0[j];
//				b->normal[1] = n1[j];
//				b->normal[2] = n2[j];
//
//				b->tangents[0][0] = t0[j];
//				b->tangents[0][1] = t1[j];
//				b->tangents[0][2] = t2[j];
//
//				b->tangents[1][0] = t3[j];
//				b->tangents[1][1] = t4[j];
//				b->tangents[1][2] = t5[j];
//
//				used[v1] = true;
//			}
//
//			if ( used[v2] ) {
//				c->normal[0] += n0[j];
//				c->normal[1] += n1[j];
//				c->normal[2] += n2[j];
//
//				c->tangents[0][0] += t0[j];
//				c->tangents[0][1] += t1[j];
//				c->tangents[0][2] += t2[j];
//
//				c->tangents[1][0] += t3[j];
//				c->tangents[1][1] += t4[j];
//				c->tangents[1][2] += t5[j];
//			} else {
//				c->normal[0] = n0[j];
//				c->normal[1] = n1[j];
//				c->normal[2] = n2[j];
//
//				c->tangents[0][0] = t0[j];
//				c->tangents[0][1] = t1[j];
//				c->tangents[0][2] = t2[j];
//
//				c->tangents[1][0] = t3[j];
//				c->tangents[1][1] = t4[j];
//				c->tangents[1][2] = t5[j];
//
//				used[v2] = true;
//			}
//		}
//	}
//
//	for ( ; i < numIndexes; i += 3 ) {
//		idDrawVert *a, *b, *c;
//		ALIGN16( unsigned long signBit[4] );
//		float d0, d1, d2, d3, d4;
//		float d5, d6, d7, d8, d9;
//		float n0, n1, n2;
//		float t0, t1, t2;
//		float t3, t4, t5;
//
//		const int v0 = indexes[i + 0];
//		const int v1 = indexes[i + 1];
//		const int v2 = indexes[i + 2];
//
//		a = verts + v0;
//		b = verts + v1;
//		c = verts + v2;
//
//		d0 = b->xyz[0] - a->xyz[0];
//		d1 = b->xyz[1] - a->xyz[1];
//		d2 = b->xyz[2] - a->xyz[2];
//		d3 = b->st[0] - a->st[0];
//		d4 = b->st[1] - a->st[1];
//
//		d5 = c->xyz[0] - a->xyz[0];
//		d6 = c->xyz[1] - a->xyz[1];
//		d7 = c->xyz[2] - a->xyz[2];
//		d8 = c->st[0] - a->st[0];
//		d9 = c->st[1] - a->st[1];
//
//#if 1
//
//		__asm {
//			// normal
//			movss		xmm0, d6
//			mulss		xmm0, d2
//			movss		xmm1, d7
//			mulss		xmm1, d1
//			subss		xmm0, xmm1
//
//			movss		xmm1, d7
//			mulss		xmm1, d0
//			movss		xmm2, d5
//			mulss		xmm2, d2
//			subss		xmm1, xmm2
//
//			movss		xmm2, d5
//			mulss		xmm2, d1
//			movss		xmm3, d6
//			mulss		xmm3, d0
//			subss		xmm2, xmm3
//
//			movss		xmm3, xmm0
//			movss		xmm4, xmm1
//			movss		xmm5, xmm2
//
//			mulss		xmm3, xmm3
//			mulss		xmm4, xmm4
//			mulss		xmm5, xmm5
//
//			addss		xmm3, xmm4
//			addss		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtss		xmm4, xmm3
//			mulss		xmm3, xmm4
//			mulss		xmm3, xmm4
//			subss		xmm3, xmm6
//			mulss		xmm4, xmm7
//			mulss		xmm3, xmm4
//#else
//			rsqrtss		xmm3, xmm3
//#endif
//			mulss		xmm0, xmm3
//			movss		n0, xmm0
//			mulss		xmm1, xmm3
//			movss		n1, xmm1
//			mulss		xmm2, xmm3
//			movss		n2, xmm2
//
//			// area sign bit
//			movss		xmm0, d3
//			mulss		xmm0, d9
//			movss		xmm1, d4
//			mulss		xmm1, d8
//			subss		xmm0, xmm1
//			andps		xmm0, SIMD_SP_signBitMask
//			movaps		signBit, xmm0
//
//			// first tangent
//			movss		xmm0, d0
//			mulss		xmm0, d9
//			movss		xmm1, d4
//			mulss		xmm1, d5
//			subss		xmm0, xmm1
//
//			movss		xmm1, d1
//			mulss		xmm1, d9
//			movss		xmm2, d4
//			mulss		xmm2, d6
//			subss		xmm1, xmm2
//
//			movss		xmm2, d2
//			mulss		xmm2, d9
//			movss		xmm3, d4
//			mulss		xmm3, d7
//			subss		xmm2, xmm3
//
//			movss		xmm3, xmm0
//			movss		xmm4, xmm1
//			movss		xmm5, xmm2
//
//			mulss		xmm3, xmm3
//			mulss		xmm4, xmm4
//			mulss		xmm5, xmm5
//
//			addss		xmm3, xmm4
//			addss		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtss		xmm4, xmm3
//			mulss		xmm3, xmm4
//			mulss		xmm3, xmm4
//			subss		xmm3, xmm6
//			mulss		xmm4, xmm7
//			mulss		xmm3, xmm4
//#else
//			rsqrtss		xmm3, xmm3
//#endif
//			xorps		xmm3, signBit
//
//			mulss		xmm0, xmm3
//			movss		t0, xmm0
//			mulss		xmm1, xmm3
//			movss		t1, xmm1
//			mulss		xmm2, xmm3
//			movss		t2, xmm2
//
//			// second tangent
//			movss		xmm0, d3
//			mulss		xmm0, d5
//			movss		xmm1, d0
//			mulss		xmm1, d8
//			subss		xmm0, xmm1
//
//			movss		xmm1, d3
//			mulss		xmm1, d6
//			movss		xmm2, d1
//			mulss		xmm2, d8
//			subss		xmm1, xmm2
//
//			movss		xmm2, d3
//			mulss		xmm2, d7
//			movss		xmm3, d2
//			mulss		xmm3, d8
//			subss		xmm2, xmm3
//
//			movss		xmm3, xmm0
//			movss		xmm4, xmm1
//			movss		xmm5, xmm2
//
//			mulss		xmm3, xmm3
//			mulss		xmm4, xmm4
//			mulss		xmm5, xmm5
//
//			addss		xmm3, xmm4
//			addss		xmm3, xmm5
//
//#ifdef FIX_DEGENERATE_TANGENT
//			xorps		xmm4, xmm4
//			cmpeqps		xmm4, xmm3
//			andps		xmm4, SIMD_SP_tiny			// if values are zero replace them with a tiny number
//			andps		xmm3, SIMD_SP_absMask		// make sure the values are positive
//			orps		xmm3, xmm4
//#endif
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//			rsqrtss		xmm4, xmm3
//			mulss		xmm3, xmm4
//			mulss		xmm3, xmm4
//			subss		xmm3, xmm6
//			mulss		xmm4, xmm7
//			mulss		xmm3, xmm4
//#else
//			rsqrtss		xmm3, xmm3
//#endif
//			xorps		xmm3, signBit
//
//			mulss		xmm0, xmm3
//			movss		t3, xmm0
//			mulss		xmm1, xmm3
//			movss		t4, xmm1
//			mulss		xmm2, xmm3
//			movss		t5, xmm2
//		}
//
//#else
//
//		float tmp;
//
//		// normal
//		n0 = d6 * d2 - d7 * d1;
//		n1 = d7 * d0 - d5 * d2;
//		n2 = d5 * d1 - d6 * d0;
//
//		tmp = idMath::RSqrt( n0 * n0 + n1 * n1 + n2 * n2 );
//
//		n0 *= tmp;
//		n1 *= tmp;
//		n2 *= tmp;
//
//		// area sign bit
//		tmp = d3 * d9 - d4 * d8;
//		signBit[0] = ( *(unsigned long *)&tmp ) & ( 1 << 31 );
//
//		// first tangent
//		t0 = d0 * d9 - d4 * d5;
//		t1 = d1 * d9 - d4 * d6;
//		t2 = d2 * d9 - d4 * d7;
//
//		tmp = idMath::RSqrt( t0 * t0 + t1 * t1 + t2 * t2 );
//		*(unsigned long *)&tmp ^= signBit[0];
//
//		t0 *= tmp;
//		t1 *= tmp;
//		t2 *= tmp;
//
//		// second tangent
//		t3 = d3 * d5 - d0 * d8;
//		t4 = d3 * d6 - d1 * d8;
//		t5 = d3 * d7 - d2 * d8;
//
//		tmp = idMath::RSqrt( t3 * t3 + t4 * t4 + t5 * t5 );
//		*(unsigned long *)&tmp ^= signBit[0];
//
//		t3 *= tmp;
//		t4 *= tmp;
//		t5 *= tmp;
//
//#endif
//
//		planes->Normal()[0] = n0;
//		planes->Normal()[1] = n1;
//		planes->Normal()[2] = n2;
//		planes->FitThroughPoint( a->xyz );
//		planes++;
//
//		if ( used[v0] ) {
//			a->normal[0] += n0;
//			a->normal[1] += n1;
//			a->normal[2] += n2;
//
//			a->tangents[0][0] += t0;
//			a->tangents[0][1] += t1;
//			a->tangents[0][2] += t2;
//
//			a->tangents[1][0] += t3;
//			a->tangents[1][1] += t4;
//			a->tangents[1][2] += t5;
//		} else {
//			a->normal[0] = n0;
//			a->normal[1] = n1;
//			a->normal[2] = n2;
//
//			a->tangents[0][0] = t0;
//			a->tangents[0][1] = t1;
//			a->tangents[0][2] = t2;
//
//			a->tangents[1][0] = t3;
//			a->tangents[1][1] = t4;
//			a->tangents[1][2] = t5;
//
//			used[v0] = true;
//		}
//
//		if ( used[v1] ) {
//			b->normal[0] += n0;
//			b->normal[1] += n1;
//			b->normal[2] += n2;
//
//			b->tangents[0][0] += t0;
//			b->tangents[0][1] += t1;
//			b->tangents[0][2] += t2;
//
//			b->tangents[1][0] += t3;
//			b->tangents[1][1] += t4;
//			b->tangents[1][2] += t5;
//		} else {
//			b->normal[0] = n0;
//			b->normal[1] = n1;
//			b->normal[2] = n2;
//
//			b->tangents[0][0] = t0;
//			b->tangents[0][1] = t1;
//			b->tangents[0][2] = t2;
//
//			b->tangents[1][0] = t3;
//			b->tangents[1][1] = t4;
//			b->tangents[1][2] = t5;
//
//			used[v1] = true;
//		}
//
//		if ( used[v2] ) {
//			c->normal[0] += n0;
//			c->normal[1] += n1;
//			c->normal[2] += n2;
//
//			c->tangents[0][0] += t0;
//			c->tangents[0][1] += t1;
//			c->tangents[0][2] += t2;
//
//			c->tangents[1][0] += t3;
//			c->tangents[1][1] += t4;
//			c->tangents[1][2] += t5;
//		} else {
//			c->normal[0] = n0;
//			c->normal[1] = n1;
//			c->normal[2] = n2;
//
//			c->tangents[0][0] = t0;
//			c->tangents[0][1] = t1;
//			c->tangents[0][2] = t2;
//
//			c->tangents[1][0] = t3;
//			c->tangents[1][1] = t4;
//			c->tangents[1][2] = t5;
//
//			used[v2] = true;
//		}
//	}
//}
//
///*
//============
//idSIMD_SSE::DeriveUnsmoothedTangents
//============
//*/
//#define DERIVE_UNSMOOTHED_BITANGENT
//
//void VPCALL idSIMD_SSE::DeriveUnsmoothedTangents( idDrawVert *verts, const dominantTri_s *dominantTris, const int numVerts ) {
//	int i, j;
//
//	for ( i = 0; i <= numVerts - 4; i += 4 ) {
//		ALIGN16( float s0[4] );
//		ALIGN16( float s1[4] );
//		ALIGN16( float s2[4] );
//		ALIGN16( float d0[4] );
//		ALIGN16( float d1[4] );
//		ALIGN16( float d2[4] );
//		ALIGN16( float d3[4] );
//		ALIGN16( float d4[4] );
//		ALIGN16( float d5[4] );
//		ALIGN16( float d6[4] );
//		ALIGN16( float d7[4] );
//		ALIGN16( float d8[4] );
//		ALIGN16( float d9[4] );
//		ALIGN16( float n0[4] );
//		ALIGN16( float n1[4] );
//		ALIGN16( float n2[4] );
//		ALIGN16( float t0[4] );
//		ALIGN16( float t1[4] );
//		ALIGN16( float t2[4] );
//		ALIGN16( float t3[4] );
//		ALIGN16( float t4[4] );
//		ALIGN16( float t5[4] );
//
//		for ( j = 0; j < 4; j++ ) {
//			const idDrawVert *a, *b, *c;
//
//			const dominantTri_s &dt = dominantTris[i+j];
//
//			s0[j] = dt.normalizationScale[0];
//			s1[j] = dt.normalizationScale[1];
//			s2[j] = dt.normalizationScale[2];
//
//			a = verts + i + j;
//			b = verts + dt.v2;
//			c = verts + dt.v3;
//
//			d0[j] = b->xyz[0] - a->xyz[0];
//			d1[j] = b->xyz[1] - a->xyz[1];
//			d2[j] = b->xyz[2] - a->xyz[2];
//			d3[j] = b->st[0] - a->st[0];
//			d4[j] = b->st[1] - a->st[1];
//
//			d5[j] = c->xyz[0] - a->xyz[0];
//			d6[j] = c->xyz[1] - a->xyz[1];
//			d7[j] = c->xyz[2] - a->xyz[2];
//			d8[j] = c->st[0] - a->st[0];
//			d9[j] = c->st[1] - a->st[1];
//		}
//
//#if 1
//
//		__asm {
//
//			movaps		xmm0, d6
//			mulps		xmm0, d2
//			movaps		xmm1, d7
//			mulps		xmm1, d1
//
//			movaps		xmm2, d7
//			mulps		xmm2, d0
//			movaps		xmm3, d5
//			mulps		xmm3, d2
//
//			movaps		xmm4, d5
//			mulps		xmm4, d1
//			movaps		xmm5, d6
//			mulps		xmm5, d0
//
//			subps		xmm0, xmm1
//			subps		xmm2, xmm3
//			movaps		xmm7, s2
//			subps		xmm4, xmm5
//
//			mulps		xmm0, xmm7
//			movaps		n0, xmm0
//			mulps		xmm2, xmm7
//			movaps		n1, xmm2
//			mulps		xmm4, xmm7
//			movaps		n2, xmm4
//
//			movaps		xmm0, d0
//			mulps		xmm0, d9
//			movaps		xmm1, d4
//			mulps		xmm1, d5
//
//			movaps		xmm2, d1
//			mulps		xmm2, d9
//			movaps		xmm3, d4
//			mulps		xmm3, d6
//
//			movaps		xmm4, d2
//			mulps		xmm4, d9
//			movaps		xmm5, d4
//			mulps		xmm5, d7
//
//			subps		xmm0, xmm1
//			subps		xmm2, xmm3
//			movaps		xmm7, s0
//			subps		xmm4, xmm5
//
//			mulps		xmm0, xmm7
//			movaps		t0, xmm0
//			mulps		xmm2, xmm7
//			movaps		t1, xmm2
//			mulps		xmm4, xmm7
//			movaps		t2, xmm4
//
//#ifndef DERIVE_UNSMOOTHED_BITANGENT
//			movaps		xmm0, d3
//			mulps		xmm0, d5
//			movaps		xmm1, d0
//			mulps		xmm1, d8
//
//			movaps		xmm2, d3
//			mulps		xmm2, d6
//			movaps		xmm3, d1
//			mulps		xmm3, d8
//
//			movaps		xmm4, d3
//			mulps		xmm4, d7
//			movaps		xmm5, d2
//			mulps		xmm5, d8
//#else
//			movaps		xmm0, n2
//			mulps		xmm0, t1
//			movaps		xmm1, n1
//			mulps		xmm1, t2
//
//			movaps		xmm2, n0
//			mulps		xmm2, t2
//			movaps		xmm3, n2
//			mulps		xmm3, t0
//
//			movaps		xmm4, n1
//			mulps		xmm4, t0
//			movaps		xmm5, n0
//			mulps		xmm5, t1
//#endif
//			subps		xmm0, xmm1
//			subps		xmm2, xmm3
//			movaps		xmm7, s1
//			subps		xmm4, xmm5
//
//			mulps		xmm0, xmm7
//			movaps		t3, xmm0
//			mulps		xmm2, xmm7
//			movaps		t4, xmm2
//			mulps		xmm4, xmm7
//			movaps		t5, xmm4
//		}
//
//#else
//
//		n0[0] = d6[0] * d2[0];
//		n0[1] = d6[1] * d2[1];
//		n0[2] = d6[2] * d2[2];
//		n0[3] = d6[3] * d2[3];
//
//		n1[0] = d7[0] * d0[0];
//		n1[1] = d7[1] * d0[1];
//		n1[2] = d7[2] * d0[2];
//		n1[3] = d7[3] * d0[3];
//
//		n2[0] = d5[0] * d1[0];
//		n2[1] = d5[1] * d1[1];
//		n2[2] = d5[2] * d1[2];
//		n2[3] = d5[3] * d1[3];
//
//		n0[0] -= d7[0] * d1[0];
//		n0[1] -= d7[1] * d1[1];
//		n0[2] -= d7[2] * d1[2];
//		n0[3] -= d7[3] * d1[3];
//
//		n1[0] -= d5[0] * d2[0];
//		n1[1] -= d5[1] * d2[1];
//		n1[2] -= d5[2] * d2[2];
//		n1[3] -= d5[3] * d2[3];
//
//		n2[0] -= d6[0] * d0[0];
//		n2[1] -= d6[1] * d0[1];
//		n2[2] -= d6[2] * d0[2];
//		n2[3] -= d6[3] * d0[3];
//
//		n0[0] *= s2[0];
//		n0[1] *= s2[1];
//		n0[2] *= s2[2];
//		n0[3] *= s2[3];
//
//		n1[0] *= s2[0];
//		n1[1] *= s2[1];
//		n1[2] *= s2[2];
//		n1[3] *= s2[3];
//
//		n2[0] *= s2[0];
//		n2[1] *= s2[1];
//		n2[2] *= s2[2];
//		n2[3] *= s2[3];
//
//		t0[0] = d0[0] * d9[0];
//		t0[1] = d0[1] * d9[1];
//		t0[2] = d0[2] * d9[2];
//		t0[3] = d0[3] * d9[3];
//
//		t1[0] = d1[0] * d9[0];
//		t1[1] = d1[1] * d9[1];
//		t1[2] = d1[2] * d9[2];
//		t1[3] = d1[3] * d9[3];
//
//		t2[0] = d2[0] * d9[0];
//		t2[1] = d2[1] * d9[1];
//		t2[2] = d2[2] * d9[2];
//		t2[3] = d2[3] * d9[3];
//
//		t0[0] -= d4[0] * d5[0];
//		t0[1] -= d4[1] * d5[1];
//		t0[2] -= d4[2] * d5[2];
//		t0[3] -= d4[3] * d5[3];
//
//		t1[0] -= d4[0] * d6[0];
//		t1[1] -= d4[1] * d6[1];
//		t1[2] -= d4[2] * d6[2];
//		t1[3] -= d4[3] * d6[3];
//
//		t2[0] -= d4[0] * d7[0];
//		t2[1] -= d4[1] * d7[1];
//		t2[2] -= d4[2] * d7[2];
//		t2[3] -= d4[3] * d7[3];
//
//		t0[0] *= s0[0];
//		t0[1] *= s0[1];
//		t0[2] *= s0[2];
//		t0[3] *= s0[3];
//
//		t1[0] *= s0[0];
//		t1[1] *= s0[1];
//		t1[2] *= s0[2];
//		t1[3] *= s0[3];
//
//		t2[0] *= s0[0];
//		t2[1] *= s0[1];
//		t2[2] *= s0[2];
//		t2[3] *= s0[3];
//
//#ifndef DERIVE_UNSMOOTHED_BITANGENT
//		t3[0] = d3[0] * d5[0];
//		t3[1] = d3[1] * d5[1];
//		t3[2] = d3[2] * d5[2];
//		t3[3] = d3[3] * d5[3];
//
//		t4[0] = d3[0] * d6[0];
//		t4[1] = d3[1] * d6[1];
//		t4[2] = d3[2] * d6[2];
//		t4[3] = d3[3] * d6[3];
//
//		t5[0] = d3[0] * d7[0];
//		t5[1] = d3[1] * d7[1];
//		t5[2] = d3[2] * d7[2];
//		t5[3] = d3[3] * d7[3];
//
//		t3[0] -= d0[0] * d8[0];
//		t3[1] -= d0[1] * d8[1];
//		t3[2] -= d0[2] * d8[2];
//		t3[3] -= d0[3] * d8[3];
//
//		t4[0] -= d1[0] * d8[0];
//		t4[1] -= d1[1] * d8[1];
//		t4[2] -= d1[2] * d8[2];
//		t4[3] -= d1[3] * d8[3];
//
//		t5[0] -= d2[0] * d8[0];
//		t5[1] -= d2[1] * d8[1];
//		t5[2] -= d2[2] * d8[2];
//		t5[3] -= d2[3] * d8[3];
//#else
//		t3[0] = n2[0] * t1[0];
//		t3[1] = n2[1] * t1[1];
//		t3[2] = n2[2] * t1[2];
//		t3[3] = n2[3] * t1[3];
//
//		t4[0] = n0[0] * t2[0];
//		t4[1] = n0[1] * t2[1];
//		t4[2] = n0[2] * t2[2];
//		t4[3] = n0[3] * t2[3];
//
//		t5[0] = n1[0] * t0[0];
//		t5[1] = n1[1] * t0[1];
//		t5[2] = n1[2] * t0[2];
//		t5[3] = n1[3] * t0[3];
//
//		t3[0] -= n1[0] * t2[0];
//		t3[1] -= n1[1] * t2[1];
//		t3[2] -= n1[2] * t2[2];
//		t3[3] -= n1[3] * t2[3];
//
//		t4[0] -= n2[0] * t0[0];
//		t4[1] -= n2[1] * t0[1];
//		t4[2] -= n2[2] * t0[2];
//		t4[3] -= n2[3] * t0[3];
//
//		t5[0] -= n0[0] * t1[0];
//		t5[1] -= n0[1] * t1[1];
//		t5[2] -= n0[2] * t1[2];
//		t5[3] -= n0[3] * t1[3];
//#endif
//		t3[0] *= s1[0];
//		t3[1] *= s1[1];
//		t3[2] *= s1[2];
//		t3[3] *= s1[3];
//
//		t4[0] *= s1[0];
//		t4[1] *= s1[1];
//		t4[2] *= s1[2];
//		t4[3] *= s1[3];
//
//		t5[0] *= s1[0];
//		t5[1] *= s1[1];
//		t5[2] *= s1[2];
//		t5[3] *= s1[3];
//
//#endif
//
//		for ( j = 0; j < 4; j++ ) {
//			idDrawVert *a;
//
//			a = verts + i + j;
//
//			a->normal[0] = n0[j];
//			a->normal[1] = n1[j];
//			a->normal[2] = n2[j];
//
//			a->tangents[0][0] = t0[j];
//			a->tangents[0][1] = t1[j];
//			a->tangents[0][2] = t2[j];
//
//			a->tangents[1][0] = t3[j];
//			a->tangents[1][1] = t4[j];
//			a->tangents[1][2] = t5[j];
//		}
//	}
//
//	for ( ; i < numVerts; i++ ) {
//		idDrawVert *a, *b, *c;
//		float d0, d1, d2, d3, d4;
//		float d5, d6, d7, d8, d9;
//		float s0, s1, s2;
//		float n0, n1, n2;
//		float t0, t1, t2;
//		float t3, t4, t5;
//
//		const dominantTri_s &dt = dominantTris[i];
//
//		s0 = dt.normalizationScale[0];
//		s1 = dt.normalizationScale[1];
//		s2 = dt.normalizationScale[2];
//
//		a = verts + i;
//		b = verts + dt.v2;
//		c = verts + dt.v3;
//
//		d0 = b->xyz[0] - a->xyz[0];
//		d1 = b->xyz[1] - a->xyz[1];
//		d2 = b->xyz[2] - a->xyz[2];
//		d3 = b->st[0] - a->st[0];
//		d4 = b->st[1] - a->st[1];
//
//		d5 = c->xyz[0] - a->xyz[0];
//		d6 = c->xyz[1] - a->xyz[1];
//		d7 = c->xyz[2] - a->xyz[2];
//		d8 = c->st[0] - a->st[0];
//		d9 = c->st[1] - a->st[1];
//
//#if 1
//
//		__asm {
//
//			movss		xmm0, d6
//			mulss		xmm0, d2
//			movss		xmm1, d7
//			mulss		xmm1, d1
//
//			movss		xmm2, d7
//			mulss		xmm2, d0
//			movss		xmm3, d5
//			mulss		xmm3, d2
//
//			movss		xmm4, d5
//			mulss		xmm4, d1
//			movss		xmm5, d6
//			mulss		xmm5, d0
//
//			subss		xmm0, xmm1
//			subss		xmm2, xmm3
//			movss		xmm7, s2
//			subss		xmm4, xmm5
//
//			mulss		xmm0, xmm7
//			movss		n0, xmm0
//			mulss		xmm2, xmm7
//			movss		n1, xmm2
//			mulss		xmm4, xmm7
//			movss		n2, xmm4
//
//			movss		xmm0, d0
//			mulss		xmm0, d9
//			movss		xmm1, d4
//			mulss		xmm1, d5
//
//			movss		xmm2, d1
//			mulss		xmm2, d9
//			movss		xmm3, d4
//			mulss		xmm3, d6
//
//			movss		xmm4, d2
//			mulss		xmm4, d9
//			movss		xmm5, d4
//			mulss		xmm5, d7
//
//			subss		xmm0, xmm1
//			subss		xmm2, xmm3
//			movss		xmm7, s0
//			subss		xmm4, xmm5
//
//			mulss		xmm0, xmm7
//			movss		t0, xmm0
//			mulss		xmm2, xmm7
//			movss		t1, xmm2
//			mulss		xmm4, xmm7
//			movss		t2, xmm4
//
//#ifndef DERIVE_UNSMOOTHED_BITANGENT
//			movss		xmm0, d3
//			mulss		xmm0, d5
//			movss		xmm1, d0
//			mulss		xmm1, d8
//
//			movss		xmm2, d3
//			mulss		xmm2, d6
//			movss		xmm3, d1
//			mulss		xmm3, d8
//
//			movss		xmm4, d3
//			mulss		xmm4, d7
//			movss		xmm5, d2
//			mulss		xmm5, d8
//#else
//			movss		xmm0, n2
//			mulss		xmm0, t1
//			movss		xmm1, n1
//			mulss		xmm1, t2
//
//			movss		xmm2, n0
//			mulss		xmm2, t2
//			movss		xmm3, n2
//			mulss		xmm3, t0
//
//			movss		xmm4, n1
//			mulss		xmm4, t0
//			movss		xmm5, n0
//			mulss		xmm5, t1
//#endif
//			subss		xmm0, xmm1
//			subss		xmm2, xmm3
//			movss		xmm7, s1
//			subss		xmm4, xmm5
//
//			mulss		xmm0, xmm7
//			movss		t3, xmm0
//			mulss		xmm2, xmm7
//			movss		t4, xmm2
//			mulss		xmm4, xmm7
//			movss		t5, xmm4
//		}
//
//#else
//
//		n0 = s2 * ( d6 * d2 - d7 * d1 );
//		n1 = s2 * ( d7 * d0 - d5 * d2 );
//		n2 = s2 * ( d5 * d1 - d6 * d0 );
//
//		t0 = s0 * ( d0 * d9 - d4 * d5 );
//		t1 = s0 * ( d1 * d9 - d4 * d6 );
//		t2 = s0 * ( d2 * d9 - d4 * d7 );
//
//#ifndef DERIVE_UNSMOOTHED_BITANGENT
//		t3 = s1 * ( d3 * d5 - d0 * d8 );
//		t4 = s1 * ( d3 * d6 - d1 * d8 );
//		t5 = s1 * ( d3 * d7 - d2 * d8 );
//#else
//		t3 = s1 * ( n2 * t1 - n1 * t2 );
//		t4 = s1 * ( n0 * t2 - n2 * t0 );
//		t5 = s1 * ( n1 * t0 - n0 * t1 );
//#endif
//
//#endif
//
//		a->normal[0] = n0;
//		a->normal[1] = n1;
//		a->normal[2] = n2;
//
//		a->tangents[0][0] = t0;
//		a->tangents[0][1] = t1;
//		a->tangents[0][2] = t2;
//
//		a->tangents[1][0] = t3;
//		a->tangents[1][1] = t4;
//		a->tangents[1][2] = t5;
//	}
//}
//
///*
//============
//idSIMD_SSE::NormalizeTangents
//============
//*/
//void VPCALL idSIMD_SSE::NormalizeTangents( idDrawVert *verts, const int numVerts ) {
//	ALIGN16( float normal[12] );
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->normal == DRAWVERT_NORMAL_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[0] == DRAWVERT_TANGENT0_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[1] == DRAWVERT_TANGENT1_OFFSET );
//
//	assert( verts != NULL );
//	assert( numVerts >= 0 );
//
//	__asm {
//		mov			eax, numVerts
//		test		eax, eax
//		jz			done
//#ifdef REFINE_TANGENT_SQUAREROOT
//		movaps		xmm6, SIMD_SP_rsqrt_c0
//		movaps		xmm7, SIMD_SP_rsqrt_c1
//#endif
//		mov			esi, verts
//		imul		eax, DRAWVERT_SIZE
//		add			esi, eax
//		neg			eax
//		add			eax, DRAWVERT_SIZE*4
//		jle			loopVert4
//
//		sub			eax, DRAWVERT_SIZE*4
//		jl			loopVert1
//
//	loopVert4:
//
//		sub			eax, DRAWVERT_SIZE*4
//
//		// normalize 4 idDrawVert::normal
//
//		movss		xmm0, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_NORMAL_OFFSET+0]	//  0,  X,  X,  X
//		movhps		xmm0, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_NORMAL_OFFSET+0]	//  0,  X,  3,  4
//		movss		xmm2, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_NORMAL_OFFSET+8]	//  5,  X,  X,  X
//		movhps		xmm2, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_NORMAL_OFFSET+4]	//	5,  X,  1,  2
//		movss		xmm4, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_NORMAL_OFFSET+0]	//  6,  X,  X,  X
//		movhps		xmm4, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_NORMAL_OFFSET+0]	//  6,  X,  9, 10
//		movss		xmm3, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_NORMAL_OFFSET+8]	// 11,  X,  X,  X
//		movhps		xmm3, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_NORMAL_OFFSET+4]	// 11,  X,  7,  8
//
//		movaps		xmm1, xmm0
//		movaps		xmm5, xmm2
//		shufps		xmm0, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )		//  0,  3,  6,  9
//		shufps		xmm2, xmm3, R_SHUFFLEPS( 3, 0, 3, 0 )		//  2,  5,  8, 11
//		shufps		xmm1, xmm5, R_SHUFFLEPS( 3, 3, 2, 2 )		//  4,  4,  1,  1
//		shufps		xmm4, xmm3, R_SHUFFLEPS( 3, 3, 2, 2 )		// 10, 10,  7,  7
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 2, 0, 2, 0 )		//  1,  4,  7, 10
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		movaps		xmm5, xmm2
//
//		mulps		xmm3, xmm3
//		mulps		xmm4, xmm4
//		mulps		xmm5, xmm5
//		addps		xmm3, xmm4
//		addps		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtps		xmm4, xmm3
//		mulps		xmm3, xmm4
//		mulps		xmm3, xmm4
//		subps		xmm3, xmm6
//		mulps		xmm4, xmm7
//		mulps		xmm3, xmm4
//#else
//		rsqrtps		xmm3, xmm3
//#endif
//
//		mulps		xmm0, xmm3
//		mulps		xmm1, xmm3
//		mulps		xmm2, xmm3
//
//		// save the 4 idDrawVert::normal to project the tangents
//
//		movaps		[normal+ 0], xmm0
//		movaps		[normal+16], xmm1
//		movaps		[normal+32], xmm2
//
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_NORMAL_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_NORMAL_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_NORMAL_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_NORMAL_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_NORMAL_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_NORMAL_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_NORMAL_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_NORMAL_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_NORMAL_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_NORMAL_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_NORMAL_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_NORMAL_OFFSET+8], xmm2
//
//		// project and normalize 4 idDrawVert::tangent[0]
//
//		movss		xmm0, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT0_OFFSET+0]	//  0,  X,  X,  X
//		movhps		xmm0, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT0_OFFSET+0]	//  0,  X,  3,  4
//		movss		xmm2, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT0_OFFSET+8]	//  5,  X,  X,  X
//		movhps		xmm2, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT0_OFFSET+4]	//	5,  X,  1,  2
//		movss		xmm4, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT0_OFFSET+0]	//  6,  X,  X,  X
//		movhps		xmm4, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT0_OFFSET+0]	//  6,  X,  9, 10
//		movss		xmm3, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT0_OFFSET+8]	// 11,  X,  X,  X
//		movhps		xmm3, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT0_OFFSET+4]	// 11,  X,  7,  8
//
//		movaps		xmm1, xmm0
//		movaps		xmm5, xmm2
//		shufps		xmm0, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )		//  0,  3,  6,  9
//		shufps		xmm2, xmm3, R_SHUFFLEPS( 3, 0, 3, 0 )		//  2,  5,  8, 11
//		shufps		xmm1, xmm5, R_SHUFFLEPS( 3, 3, 2, 2 )		//  4,  4,  1,  1
//		shufps		xmm4, xmm3, R_SHUFFLEPS( 3, 3, 2, 2 )		// 10, 10,  7,  7
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 2, 0, 2, 0 )		//  1,  4,  7, 10
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		movaps		xmm5, xmm2
//
//		mulps		xmm3, [normal+ 0]
//		mulps		xmm4, [normal+16]
//		mulps		xmm5, [normal+32]
//		addps		xmm3, xmm4
//		addps		xmm3, xmm5
//
//		movaps		xmm4, xmm3
//		movaps		xmm5, xmm3
//		mulps		xmm3, [normal+ 0]
//		mulps		xmm4, [normal+16]
//		mulps		xmm5, [normal+32]
//		subps		xmm0, xmm3
//		subps		xmm1, xmm4
//		subps		xmm2, xmm5
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		movaps		xmm5, xmm2
//
//		mulps		xmm3, xmm3
//		mulps		xmm4, xmm4
//		mulps		xmm5, xmm5
//		addps		xmm3, xmm4
//		addps		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtps		xmm4, xmm3
//		mulps		xmm3, xmm4
//		mulps		xmm3, xmm4
//		subps		xmm3, xmm6
//		mulps		xmm4, xmm7
//		mulps		xmm3, xmm4
//#else
//		rsqrtps		xmm3, xmm3
//#endif
//
//		mulps		xmm0, xmm3
//		mulps		xmm1, xmm3
//		mulps		xmm2, xmm3
//
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT0_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT0_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT0_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT0_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT0_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT0_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT0_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT0_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT0_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT0_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT0_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT0_OFFSET+8], xmm2
//
//		// project and normalize 4 idDrawVert::tangent[1]
//
//		movss		xmm0, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT1_OFFSET+0]	//  0,  X,  X,  X
//		movhps		xmm0, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT1_OFFSET+0]	//  0,  X,  3,  4
//		movss		xmm2, [esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT1_OFFSET+8]	//  5,  X,  X,  X
//		movhps		xmm2, [esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT1_OFFSET+4]	//	5,  X,  1,  2
//		movss		xmm4, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT1_OFFSET+0]	//  6,  X,  X,  X
//		movhps		xmm4, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT1_OFFSET+0]	//  6,  X,  9, 10
//		movss		xmm3, [esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT1_OFFSET+8]	// 11,  X,  X,  X
//		movhps		xmm3, [esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT1_OFFSET+4]	// 11,  X,  7,  8
//
//		movaps		xmm1, xmm0
//		movaps		xmm5, xmm2
//		shufps		xmm0, xmm4, R_SHUFFLEPS( 0, 2, 0, 2 )		//  0,  3,  6,  9
//		shufps		xmm2, xmm3, R_SHUFFLEPS( 3, 0, 3, 0 )		//  2,  5,  8, 11
//		shufps		xmm1, xmm5, R_SHUFFLEPS( 3, 3, 2, 2 )		//  4,  4,  1,  1
//		shufps		xmm4, xmm3, R_SHUFFLEPS( 3, 3, 2, 2 )		// 10, 10,  7,  7
//		shufps		xmm1, xmm4, R_SHUFFLEPS( 2, 0, 2, 0 )		//  1,  4,  7, 10
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		movaps		xmm5, xmm2
//
//		mulps		xmm3, [normal+ 0]
//		mulps		xmm4, [normal+16]
//		mulps		xmm5, [normal+32]
//		addps		xmm3, xmm4
//		addps		xmm3, xmm5
//
//		movaps		xmm4, xmm3
//		movaps		xmm5, xmm3
//		mulps		xmm3, [normal+ 0]
//		mulps		xmm4, [normal+16]
//		mulps		xmm5, [normal+32]
//		subps		xmm0, xmm3
//		subps		xmm1, xmm4
//		subps		xmm2, xmm5
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		movaps		xmm5, xmm2
//
//		mulps		xmm3, xmm3
//		mulps		xmm4, xmm4
//		mulps		xmm5, xmm5
//		addps		xmm3, xmm4
//		addps		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtps		xmm4, xmm3
//		mulps		xmm3, xmm4
//		mulps		xmm3, xmm4
//		subps		xmm3, xmm6
//		mulps		xmm4, xmm7
//		mulps		xmm3, xmm4
//#else
//		rsqrtps		xmm3, xmm3
//#endif
//
//		mulps		xmm0, xmm3
//		mulps		xmm1, xmm3
//		mulps		xmm2, xmm3
//
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT1_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT1_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*0+DRAWVERT_TANGENT1_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT1_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT1_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*1+DRAWVERT_TANGENT1_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT1_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT1_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*2+DRAWVERT_TANGENT1_OFFSET+8], xmm2
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT1_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT1_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_SIZE*3+DRAWVERT_TANGENT1_OFFSET+8], xmm2
//
//		add			eax, DRAWVERT_SIZE*8
//
//		jle			loopVert4
//
//		sub			eax, DRAWVERT_SIZE*4
//		jge			done
//
//	loopVert1:
//
//		// normalize one idDrawVert::normal
//
//		movss		xmm0, [esi+eax+DRAWVERT_NORMAL_OFFSET+0]
//		movss		xmm1, [esi+eax+DRAWVERT_NORMAL_OFFSET+4]
//		movss		xmm2, [esi+eax+DRAWVERT_NORMAL_OFFSET+8]
//		movss		xmm3, xmm0
//		movss		xmm4, xmm1
//		movss		xmm5, xmm2
//
//		mulss		xmm3, xmm3
//		mulss		xmm4, xmm4
//		mulss		xmm5, xmm5
//		addss		xmm3, xmm4
//		addss		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtss		xmm4, xmm3
//		mulss		xmm3, xmm4
//		mulss		xmm3, xmm4
//		subss		xmm3, xmm6
//		mulss		xmm4, xmm7
//		mulss		xmm3, xmm4
//#else
//		rsqrtss		xmm3, xmm3
//#endif
//
//		mulss		xmm0, xmm3
//		mulss		xmm1, xmm3
//		mulss		xmm2, xmm3
//
//		movss		[esi+eax+DRAWVERT_NORMAL_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_NORMAL_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_NORMAL_OFFSET+8], xmm2
//
//		// project and normalize one idDrawVert::tangent[0]
//
//		movss		xmm0, [esi+eax+DRAWVERT_TANGENT0_OFFSET+0]
//		movss		xmm1, [esi+eax+DRAWVERT_TANGENT0_OFFSET+4]
//		movss		xmm2, [esi+eax+DRAWVERT_TANGENT0_OFFSET+8]
//		movss		xmm3, xmm0
//		movss		xmm4, xmm1
//		movss		xmm5, xmm2
//
//		mulss		xmm3, [esi+eax+DRAWVERT_NORMAL_OFFSET+0]
//		mulss		xmm4, [esi+eax+DRAWVERT_NORMAL_OFFSET+4]
//		mulss		xmm5, [esi+eax+DRAWVERT_NORMAL_OFFSET+8]
//		addss		xmm3, xmm4
//		addss		xmm3, xmm5
//
//		movss		xmm4, xmm3
//		movss		xmm5, xmm3
//		mulss		xmm3, [esi+eax+DRAWVERT_NORMAL_OFFSET+0]
//		mulss		xmm4, [esi+eax+DRAWVERT_NORMAL_OFFSET+4]
//		mulss		xmm5, [esi+eax+DRAWVERT_NORMAL_OFFSET+8]
//		subss		xmm0, xmm3
//		subss		xmm1, xmm4
//		subss		xmm2, xmm5
//
//		movss		xmm3, xmm0
//		movss		xmm4, xmm1
//		movss		xmm5, xmm2
//
//		mulss		xmm3, xmm3
//		mulss		xmm4, xmm4
//		mulss		xmm5, xmm5
//		addss		xmm3, xmm4
//		addss		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtss		xmm4, xmm3
//		mulss		xmm3, xmm4
//		mulss		xmm3, xmm4
//		subss		xmm3, xmm6
//		mulss		xmm4, xmm7
//		mulss		xmm3, xmm4
//#else
//		rsqrtss		xmm3, xmm3
//#endif
//
//		mulss		xmm0, xmm3
//		mulss		xmm1, xmm3
//		mulss		xmm2, xmm3
//
//		movss		[esi+eax+DRAWVERT_TANGENT0_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_TANGENT0_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_TANGENT0_OFFSET+8], xmm2
//
//		// project and normalize one idDrawVert::tangent[1]
//
//		movss		xmm0, [esi+eax+DRAWVERT_TANGENT1_OFFSET+0]
//		movss		xmm1, [esi+eax+DRAWVERT_TANGENT1_OFFSET+4]
//		movss		xmm2, [esi+eax+DRAWVERT_TANGENT1_OFFSET+8]
//		movss		xmm3, xmm0
//		movss		xmm4, xmm1
//		movss		xmm5, xmm2
//
//		mulss		xmm3, [esi+eax+DRAWVERT_NORMAL_OFFSET+0]
//		mulss		xmm4, [esi+eax+DRAWVERT_NORMAL_OFFSET+4]
//		mulss		xmm5, [esi+eax+DRAWVERT_NORMAL_OFFSET+8]
//		addss		xmm3, xmm4
//		addss		xmm3, xmm5
//
//		movss		xmm4, xmm3
//		movss		xmm5, xmm3
//		mulss		xmm3, [esi+eax+DRAWVERT_NORMAL_OFFSET+0]
//		mulss		xmm4, [esi+eax+DRAWVERT_NORMAL_OFFSET+4]
//		mulss		xmm5, [esi+eax+DRAWVERT_NORMAL_OFFSET+8]
//		subss		xmm0, xmm3
//		subss		xmm1, xmm4
//		subss		xmm2, xmm5
//
//		movss		xmm3, xmm0
//		movss		xmm4, xmm1
//		movss		xmm5, xmm2
//
//		mulss		xmm3, xmm3
//		mulss		xmm4, xmm4
//		mulss		xmm5, xmm5
//		addss		xmm3, xmm4
//		addss		xmm3, xmm5
//
//#ifdef REFINE_TANGENT_SQUAREROOT
//		rsqrtss		xmm4, xmm3
//		mulss		xmm3, xmm4
//		mulss		xmm3, xmm4
//		subss		xmm3, xmm6
//		mulss		xmm4, xmm7
//		mulss		xmm3, xmm4
//#else
//		rsqrtss		xmm3, xmm3
//#endif
//
//		mulss		xmm0, xmm3
//		mulss		xmm1, xmm3
//		mulss		xmm2, xmm3
//
//		movss		[esi+eax+DRAWVERT_TANGENT1_OFFSET+0], xmm0
//		movss		[esi+eax+DRAWVERT_TANGENT1_OFFSET+4], xmm1
//		movss		[esi+eax+DRAWVERT_TANGENT1_OFFSET+8], xmm2
//
//		add			eax, DRAWVERT_SIZE
//
//		jl			loopVert1
//	done:
//	}
//}
//
///*
//============
//idSIMD_SSE::CreateTextureSpaceLightVectors
//============
//*/
//void VPCALL idSIMD_SSE::CreateTextureSpaceLightVectors( idVec3 *lightVectors, const idVec3 &lightOrigin, const idDrawVert *verts, const int numVerts, const int *indexes, const int numIndexes ) {
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//	assert( (int)&((idDrawVert *)0)->normal == DRAWVERT_NORMAL_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[0] == DRAWVERT_TANGENT0_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[1] == DRAWVERT_TANGENT1_OFFSET );
//
//	bool *used = (bool *)_alloca16( numVerts * sizeof( used[0] ) );
//	memset( used, 0, numVerts * sizeof( used[0] ) );
//
//	for ( int i = numIndexes - 1; i >= 0; i-- ) {
//		used[indexes[i]] = true;
//	}
//
//#if 0
//
//	__asm {
//
//		mov			eax, numVerts
//
//		mov			esi, used
//		add			esi, eax
//
//		mov			edi, verts
//		sub			edi, DRAWVERT_SIZE
//
//		neg			eax
//		dec			eax
//
//		mov			ecx, lightOrigin
//		movss		xmm7, [ecx+0]
//		movhps		xmm7, [ecx+4]
//
//		mov			ecx, lightVectors
//		sub			ecx, 3*4
//
//	loopVert:
//		inc			eax
//		jge			done
//
//		add			edi, DRAWVERT_SIZE
//		add			ecx, 3*4
//
//		cmp			byte ptr [esi+eax], 0
//		je			loopVert
//
//		movaps		xmm0, xmm7
//		movss		xmm1, [edi+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm1, [edi+DRAWVERT_XYZ_OFFSET+4]
//		subps		xmm0, xmm1
//
//		// 0,  X,  1,  2
//		// 3,  X,  4,  5
//		// 6,  X,  7,  8
//
//		movss		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+0]
//		movhps		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+4]
//		mulps		xmm2, xmm0
//
//		movss		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+0]
//		movhps		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+4]
//		mulps		xmm3, xmm0
//
//		movaps		xmm5, xmm2								// xmm5 = 0,  X,  1,  2
//		unpcklps	xmm5, xmm3								// xmm5 = 0,  3,  X,  X
//		unpckhps	xmm2, xmm3								// xmm2 = 1,  4,  2,  5
//
//		movss		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+0]
//		movhps		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+4]
//		mulps		xmm4, xmm0
//
//		movlhps		xmm5, xmm4								// xmm5 = 0,  3,  6,  X
//		movhlps		xmm4, xmm2								// xmm4 = 2,  5,  7,  8
//		shufps		xmm2, xmm4, R_SHUFFLEPS( 0, 1, 3, 2 )	// xmm2 = 2,  5,  8,  7
//
//		addps		xmm5, xmm4
//		addps		xmm5, xmm2
//		movlps		[ecx+0], xmm5
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 2, 3, 0, 1 )
//		movss		[ecx+8], xmm5
//
//		jmp			loopVert
//
//	done:
//	}
//
//#elif 1
//
//	for ( int i = 0; i < numVerts; i++ ) {
//		if ( !used[i] ) {
//			continue;
//		}
//
//		const idDrawVert *v = &verts[i];
//		idVec3 lightDir;
//
//		lightDir[0] = lightOrigin[0] - v->xyz[0];
//		lightDir[1] = lightOrigin[1] - v->xyz[1];
//		lightDir[2] = lightOrigin[2] - v->xyz[2];
//
//		lightVectors[i][0] = lightDir[0] * v->tangents[0][0] + lightDir[1] * v->tangents[0][1] + lightDir[2] * v->tangents[0][2];
//		lightVectors[i][1] = lightDir[0] * v->tangents[1][0] + lightDir[1] * v->tangents[1][1] + lightDir[2] * v->tangents[1][2];
//		lightVectors[i][2] = lightDir[0] * v->normal[0] + lightDir[1] * v->normal[1] + lightDir[2] * v->normal[2];
//	}
//
//#elif 1
//
//	ALIGN16( int usedVertNums[4] );
//	ALIGN16( float lightDir0[4] );
//	ALIGN16( float lightDir1[4] );
//	ALIGN16( float lightDir2[4] );
//	ALIGN16( float normal0[4] );
//	ALIGN16( float normal1[4] );
//	ALIGN16( float normal2[4] );
//	ALIGN16( float tangent0[4] );
//	ALIGN16( float tangent1[4] );
//	ALIGN16( float tangent2[4] );
//	ALIGN16( float tangent3[4] );
//	ALIGN16( float tangent4[4] );
//	ALIGN16( float tangent5[4] );
//	idVec3 localLightOrigin = lightOrigin;
//
//	__asm {
//
//		xor			ecx, ecx
//		mov			eax, numVerts
//
//		mov			esi, used
//		add			esi, eax
//
//		mov			edi, verts
//		sub			edi, DRAWVERT_SIZE
//
//		neg			eax
//		dec			eax
//
//	loopVert4:
//		inc			eax
//		jge			done4
//
//		add			edi, DRAWVERT_SIZE
//
//		cmp			byte ptr [esi+eax], 0
//		je			loopVert4
//
//		mov			usedVertNums[ecx*4], eax
//
//		inc			ecx
//		cmp			ecx, 4
//
//		movss		xmm0, localLightOrigin[0]
//		movss		xmm1, localLightOrigin[4]
//		movss		xmm2, localLightOrigin[8]
//
//		subss		xmm0, [edi+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm1, [edi+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm2, [edi+DRAWVERT_XYZ_OFFSET+8]
//
//		movss		lightDir0[ecx*4-4], xmm0
//		movss		lightDir1[ecx*4-4], xmm1
//		movss		lightDir2[ecx*4-4], xmm2
//
//		movss		xmm3, [edi+DRAWVERT_NORMAL_OFFSET+0]
//		movss		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+4]
//		movss		xmm5, [edi+DRAWVERT_NORMAL_OFFSET+8]
//
//		movss		normal0[ecx*4-4], xmm3
//		movss		normal1[ecx*4-4], xmm4
//		movss		normal2[ecx*4-4], xmm5
//
//		movss		xmm0, [edi+DRAWVERT_TANGENT0_OFFSET+0]
//		movss		xmm1, [edi+DRAWVERT_TANGENT0_OFFSET+4]
//		movss		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+8]
//
//		movss		tangent0[ecx*4-4], xmm0
//		movss		tangent1[ecx*4-4], xmm1
//		movss		tangent2[ecx*4-4], xmm2
//
//		movss		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+0]
//		movss		xmm4, [edi+DRAWVERT_TANGENT1_OFFSET+4]
//		movss		xmm5, [edi+DRAWVERT_TANGENT1_OFFSET+8]
//
//		movss		tangent3[ecx*4-4], xmm3
//		movss		tangent4[ecx*4-4], xmm4
//		movss		tangent5[ecx*4-4], xmm5
//
//		jl			loopVert4
//
//		movaps		xmm0, lightDir0
//		movaps		xmm1, lightDir1
//		movaps		xmm2, lightDir2
//
//		movaps		xmm3, tangent0
//		mulps		xmm3, xmm0
//		movaps		xmm4, tangent1
//		mulps		xmm4, xmm1
//		movaps		xmm5, tangent2
//		mulps		xmm5, xmm2
//
//		addps		xmm3, xmm4
//		addps		xmm5, xmm3
//
//		movaps		xmm3, tangent3
//		mulps		xmm3, xmm0
//		movaps		xmm4, tangent4
//		mulps		xmm4, xmm1
//		movaps		xmm6, tangent5
//		mulps		xmm6, xmm2
//
//		addps		xmm3, xmm4
//		addps		xmm6, xmm3
//
//		mulps		xmm0, normal0
//		mulps		xmm1, normal1
//		mulps		xmm2, normal2
//
//		addps		xmm0, xmm1
//		addps		xmm0, xmm2
//
//		mov			ecx, numVerts
//		imul		ecx, 12
//		mov			edx, usedVertNums[0]
//		add			ecx, lightVectors
//		imul		edx, 12
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[4]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		imul		edx, 12
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[8]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		imul		edx, 12
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[12]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		imul		edx, 12
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//
//		xor			ecx, ecx
//		jmp			loopVert4
//
//	done4:
//		test		ecx, ecx
//		jz			done
//		xor			eax, eax
//		mov			edi, numVerts
//		imul		edi, 12
//		add			edi, lightVectors
//
//	loopVert1:
//		movss		xmm0, lightDir0[eax*4]
//		movss		xmm1, lightDir1[eax*4]
//		movss		xmm2, lightDir2[eax*4]
//
//		mov			edx, usedVertNums[eax*4]
//		imul		edx, 12
//
//		movss		xmm3, tangent0[eax*4]
//		mulss		xmm3, xmm0
//		movss		xmm4, tangent1[eax*4]
//		mulss		xmm4, xmm1
//		movss		xmm5, tangent2[eax*4]
//		mulss		xmm5, xmm2
//
//		addss		xmm3, xmm4
//		addss		xmm5, xmm3
//		movss		[edi+edx+0], xmm5
//
//		movss		xmm3, tangent3[eax*4]
//		mulss		xmm3, xmm0
//		movss		xmm4, tangent4[eax*4]
//		mulss		xmm4, xmm1
//		movss		xmm6, tangent5[eax*4]
//		mulss		xmm6, xmm2
//
//		addss		xmm3, xmm4
//		addss		xmm6, xmm3
//		movss		[edi+edx+4], xmm6
//
//		mulss		xmm0, normal0[eax*4]
//		mulss		xmm1, normal1[eax*4]
//		mulss		xmm2, normal2[eax*4]
//
//		addss		xmm0, xmm1
//		addss		xmm0, xmm2
//		movss		[edi+edx+8], xmm0
//
//		inc			eax
//		dec			ecx
//		jg			loopVert1
//
//	done:
//	}
//
//#else
//
//	ALIGN16( float lightVectors0[4] );
//	ALIGN16( float lightVectors1[4] );
//	ALIGN16( float lightVectors2[4] );
//	int numUsedVerts = 0;
//
//	for ( int i = 0; i < numVerts; i++ ) {
//		if ( !used[i] ) {
//			continue;
//		}
//
//		const idDrawVert *v = &verts[i];
//
//		lightDir0[numUsedVerts] = lightOrigin[0] - v->xyz[0];
//		lightDir1[numUsedVerts] = lightOrigin[1] - v->xyz[1];
//		lightDir2[numUsedVerts] = lightOrigin[2] - v->xyz[2];
//
//		normal0[numUsedVerts] = v->normal[0];
//		normal1[numUsedVerts] = v->normal[1];
//		normal2[numUsedVerts] = v->normal[2];
//
//		tangent0[numUsedVerts] = v->tangents[0][0];
//		tangent1[numUsedVerts] = v->tangents[0][1];
//		tangent2[numUsedVerts] = v->tangents[0][2];
//
//		tangent3[numUsedVerts] = v->tangents[1][0];
//		tangent4[numUsedVerts] = v->tangents[1][1];
//		tangent5[numUsedVerts] = v->tangents[1][2];
//
//		usedVertNums[numUsedVerts++] = i;
//		if ( numUsedVerts < 4 ) {
//			continue;
//		}
//
//		lightVectors0[0] = lightDir0[0] * tangent0[0];
//		lightVectors0[1] = lightDir0[1] * tangent0[1];
//		lightVectors0[2] = lightDir0[2] * tangent0[2];
//		lightVectors0[3] = lightDir0[3] * tangent0[3];
//
//		lightVectors0[0] += lightDir1[0] * tangent1[0];
//		lightVectors0[1] += lightDir1[1] * tangent1[1];
//		lightVectors0[2] += lightDir1[2] * tangent1[2];
//		lightVectors0[3] += lightDir1[3] * tangent1[3];
//
//		lightVectors0[0] += lightDir2[0] * tangent2[0];
//		lightVectors0[1] += lightDir2[1] * tangent2[1];
//		lightVectors0[2] += lightDir2[2] * tangent2[2];
//		lightVectors0[3] += lightDir2[3] * tangent2[3];
//
//		lightVectors1[0] = lightDir0[0] * tangent3[0];
//		lightVectors1[1] = lightDir0[1] * tangent3[1];
//		lightVectors1[2] = lightDir0[2] * tangent3[2];
//		lightVectors1[3] = lightDir0[3] * tangent3[3];
//
//		lightVectors1[0] += lightDir1[0] * tangent4[0];
//		lightVectors1[1] += lightDir1[1] * tangent4[1];
//		lightVectors1[2] += lightDir1[2] * tangent4[2];
//		lightVectors1[3] += lightDir1[3] * tangent4[3];
//
//		lightVectors1[0] += lightDir2[0] * tangent5[0];
//		lightVectors1[1] += lightDir2[1] * tangent5[1];
//		lightVectors1[2] += lightDir2[2] * tangent5[2];
//		lightVectors1[3] += lightDir2[3] * tangent5[3];
//
//		lightVectors2[0] = lightDir0[0] * normal0[0];
//		lightVectors2[1] = lightDir0[1] * normal0[1];
//		lightVectors2[2] = lightDir0[2] * normal0[2];
//		lightVectors2[3] = lightDir0[3] * normal0[3];
//
//		lightVectors2[0] += lightDir1[0] * normal1[0];
//		lightVectors2[1] += lightDir1[1] * normal1[1];
//		lightVectors2[2] += lightDir1[2] * normal1[2];
//		lightVectors2[3] += lightDir1[3] * normal1[3];
//
//		lightVectors2[0] += lightDir2[0] * normal2[0];
//		lightVectors2[1] += lightDir2[1] * normal2[1];
//		lightVectors2[2] += lightDir2[2] * normal2[2];
//		lightVectors2[3] += lightDir2[3] * normal2[3];
//
//
//		for ( int j = 0; j < 4; j++ ) {
//			int n = usedVertNums[j];
//
//			lightVectors[n][0] = lightVectors0[j];
//			lightVectors[n][1] = lightVectors1[j];
//			lightVectors[n][2] = lightVectors2[j];
//		}
//
//		numUsedVerts = 0;
//	}
//
//	for ( int i = 0; i < numUsedVerts; i++ ) {
//
//		lightVectors0[i] = lightDir0[i] * tangent0[i] + lightDir1[i] * tangent1[i] + lightDir2[i] * tangent2[i];
//		lightVectors1[i] = lightDir0[i] * tangent3[i] + lightDir1[i] * tangent4[i] + lightDir2[i] * tangent5[i];
//		lightVectors2[i] = lightDir0[i] * normal0[i] + lightDir1[i] * normal1[i] + lightDir2[i] * normal2[i];
//
//		int n = usedVertNums[i];
//		lightVectors[n][0] = lightVectors0[i];
//		lightVectors[n][1] = lightVectors1[i];
//		lightVectors[n][2] = lightVectors2[i];
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::CreateSpecularTextureCoords
//============
//*/
//void VPCALL idSIMD_SSE::CreateSpecularTextureCoords( idVec4 *texCoords, const idVec3 &lightOrigin, const idVec3 &viewOrigin, const idDrawVert *verts, const int numVerts, const int *indexes, const int numIndexes ) {
//
//	assert( sizeof( idDrawVert ) == DRAWVERT_SIZE );
//	assert( (int)&((idDrawVert *)0)->xyz == DRAWVERT_XYZ_OFFSET );
//	assert( (int)&((idDrawVert *)0)->normal == DRAWVERT_NORMAL_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[0] == DRAWVERT_TANGENT0_OFFSET );
//	assert( (int)&((idDrawVert *)0)->tangents[1] == DRAWVERT_TANGENT1_OFFSET );
//
//	bool *used = (bool *)_alloca16( numVerts * sizeof( used[0] ) );
//	memset( used, 0, numVerts * sizeof( used[0] ) );
//
//	for ( int i = numIndexes - 1; i >= 0; i-- ) {
//		used[indexes[i]] = true;
//	}
//
//#if 0
//
//	__asm {
//
//		mov			eax, numVerts
//
//		mov			esi, used
//		add			esi, eax
//
//		mov			edi, verts
//		sub			edi, DRAWVERT_SIZE
//
//		neg			eax
//		dec			eax
//
//		mov			ecx, viewOrigin
//		movss		xmm6, [ecx+0]
//		movhps		xmm6, [ecx+4]
//
//		mov			ecx, lightOrigin
//		movss		xmm7, [ecx+0]
//		movhps		xmm7, [ecx+4]
//
//		mov			ecx, texCoords
//		sub			ecx, 4*4
//
//	loopVert:
//		inc			eax
//		jge			done
//
//		add			edi, DRAWVERT_SIZE
//		add			ecx, 4*4
//
//		cmp			byte ptr [esi+eax], 0
//		je			loopVert
//
//		movaps		xmm0, xmm7
//		movaps		xmm1, xmm6
//		movss		xmm2, [edi+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm2, [edi+DRAWVERT_XYZ_OFFSET+4]
//		subps		xmm0, xmm2
//		subps		xmm1, xmm2
//
//		movaps		xmm3, xmm0
//		movaps		xmm4, xmm1
//		mulps		xmm3, xmm3
//		mulps		xmm4, xmm4
//
//		// 0,  X,  1,  2
//		// 3,  X,  4,  5
//
//		movaps		xmm5, xmm3								// xmm5 = 0,  X,  1,  2
//		unpcklps	xmm5, xmm4								// xmm5 = 0,  3,  X,  X
//		unpckhps	xmm3, xmm4								// xmm3 = 1,  4,  2,  5
//		movhlps		xmm4, xmm3								// xmm4 = 2,  5,  4,  5
//
//		addps		xmm5, xmm3
//		addps		xmm5, xmm4
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 0, 1, 0, 1 )
//		rsqrtps		xmm5, xmm5
//
//		movaps		xmm4, xmm5
//		shufps		xmm4, xmm4, R_SHUFFLEPS( 0, 0, 0, 0 )
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 1, 1, 1 )
//
//		mulps		xmm0, xmm4
//		mulps		xmm1, xmm5
//		addps		xmm0, xmm1
//
//		movss		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+0]
//		movhps		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+4]
//		mulps		xmm2, xmm0
//
//		movss		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+0]
//		movhps		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+4]
//		mulps		xmm3, xmm0
//
//		movss		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+0]
//		movhps		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+4]
//		mulps		xmm4, xmm0
//
//		movaps		xmm5, xmm2								// xmm5 = 0,  X,  1,  2
//		unpcklps	xmm5, xmm3								// xmm5 = 0,  3,  X,  X
//		unpckhps	xmm2, xmm3								// xmm2 = 1,  4,  2,  5
//
//		movlhps		xmm5, xmm4								// xmm5 = 0,  3,  6,  X
//		movhlps		xmm4, xmm2								// xmm4 = 2,  5,  7,  8
//		shufps		xmm2, xmm4, R_SHUFFLEPS( 0, 1, 3, 2 )	// xmm2 = 2,  5,  8,  7
//
//		movaps		xmm3, SIMD_SP_one
//
//		addps		xmm5, xmm4
//		addps		xmm5, xmm2
//		movaps		[ecx+0], xmm5
//		movss		[ecx+12], xmm3
//
//		jmp			loopVert
//
//	done:
//	}
//
//#elif 0
//
//	for ( int i = 0; i < numVerts; i++ ) {
//		if ( !used[i] ) {
//			continue;
//		}
//
//		const idDrawVert *v = &verts[i];
//
//		idVec3 lightDir = lightOrigin - v->xyz;
//		idVec3 viewDir = viewOrigin - v->xyz;
//
//		float ilength;
//
//		ilength = idMath::RSqrt( lightDir[0] * lightDir[0] + lightDir[1] * lightDir[1] + lightDir[2] * lightDir[2] );
//		lightDir[0] *= ilength;
//		lightDir[1] *= ilength;
//		lightDir[2] *= ilength;
//
//		ilength = idMath::RSqrt( viewDir[0] * viewDir[0] + viewDir[1] * viewDir[1] + viewDir[2] * viewDir[2] );
//		viewDir[0] *= ilength;
//		viewDir[1] *= ilength;
//		viewDir[2] *= ilength;
//
//		lightDir += viewDir;
//
//		texCoords[i][0] = lightDir[0] * v->tangents[0][0] + lightDir[1] * v->tangents[0][1] + lightDir[2] * v->tangents[0][2];
//		texCoords[i][1] = lightDir[0] * v->tangents[1][0] + lightDir[1] * v->tangents[1][1] + lightDir[2] * v->tangents[1][2];
//		texCoords[i][2] = lightDir[0] * v->normal[0] + lightDir[1] * v->normal[1] + lightDir[2] * v->normal[2];
//		texCoords[i][3] = 1.0f;
//	}
//
//
//#elif 1
//
//	ALIGN16( int usedVertNums[4] );
//	ALIGN16( float lightDir0[4] );
//	ALIGN16( float lightDir1[4] );
//	ALIGN16( float lightDir2[4] );
//	ALIGN16( float viewDir0[4] );
//	ALIGN16( float viewDir1[4] );
//	ALIGN16( float viewDir2[4] );
//	ALIGN16( float normal0[4] );
//	ALIGN16( float normal1[4] );
//	ALIGN16( float normal2[4] );
//	ALIGN16( float tangent0[4] );
//	ALIGN16( float tangent1[4] );
//	ALIGN16( float tangent2[4] );
//	ALIGN16( float tangent3[4] );
//	ALIGN16( float tangent4[4] );
//	ALIGN16( float tangent5[4] );
//	idVec3 localLightOrigin = lightOrigin;
//	idVec3 localViewOrigin = viewOrigin;
//
//	__asm {
//
//		xor			ecx, ecx
//		mov			eax, numVerts
//
//		mov			esi, used
//		add			esi, eax
//
//		mov			edi, verts
//		sub			edi, DRAWVERT_SIZE
//
//		neg			eax
//		dec			eax
//
//	loopVert4:
//		inc			eax
//		jge			done4
//
//		add			edi, DRAWVERT_SIZE
//
//		cmp			byte ptr [esi+eax], 0
//		je			loopVert4
//
//		mov			usedVertNums[ecx*4], eax
//
//		inc			ecx
//		cmp			ecx, 4
//
//		movss		xmm3, localLightOrigin[0]
//		movss		xmm4, localLightOrigin[4]
//		movss		xmm5, localLightOrigin[8]
//
//		subss		xmm3, [edi+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm4, [edi+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm5, [edi+DRAWVERT_XYZ_OFFSET+8]
//
//		movss		lightDir0[ecx*4-4], xmm3
//		movss		lightDir1[ecx*4-4], xmm4
//		movss		lightDir2[ecx*4-4], xmm5
//
//		movss		xmm0, localViewOrigin[0]
//		movss		xmm1, localViewOrigin[4]
//		movss		xmm2, localViewOrigin[8]
//
//		subss		xmm0, [edi+DRAWVERT_XYZ_OFFSET+0]
//		subss		xmm1, [edi+DRAWVERT_XYZ_OFFSET+4]
//		subss		xmm2, [edi+DRAWVERT_XYZ_OFFSET+8]
//
//		movss		viewDir0[ecx*4-4], xmm0
//		movss		viewDir1[ecx*4-4], xmm1
//		movss		viewDir2[ecx*4-4], xmm2
//
//		movss		xmm3, [edi+DRAWVERT_NORMAL_OFFSET+0]
//		movss		xmm4, [edi+DRAWVERT_NORMAL_OFFSET+4]
//		movss		xmm5, [edi+DRAWVERT_NORMAL_OFFSET+8]
//
//		movss		normal0[ecx*4-4], xmm3
//		movss		normal1[ecx*4-4], xmm4
//		movss		normal2[ecx*4-4], xmm5
//
//		movss		xmm0, [edi+DRAWVERT_TANGENT0_OFFSET+0]
//		movss		xmm1, [edi+DRAWVERT_TANGENT0_OFFSET+4]
//		movss		xmm2, [edi+DRAWVERT_TANGENT0_OFFSET+8]
//
//		movss		tangent0[ecx*4-4], xmm0
//		movss		tangent1[ecx*4-4], xmm1
//		movss		tangent2[ecx*4-4], xmm2
//
//		movss		xmm3, [edi+DRAWVERT_TANGENT1_OFFSET+0]
//		movss		xmm4, [edi+DRAWVERT_TANGENT1_OFFSET+4]
//		movss		xmm5, [edi+DRAWVERT_TANGENT1_OFFSET+8]
//
//		movss		tangent3[ecx*4-4], xmm3
//		movss		tangent4[ecx*4-4], xmm4
//		movss		tangent5[ecx*4-4], xmm5
//
//		jl			loopVert4
//
//		movaps		xmm6, lightDir0
//		movaps		xmm0, xmm6
//		mulps		xmm6, xmm6
//		movaps		xmm7, lightDir1
//		movaps		xmm1, xmm7
//		mulps		xmm7, xmm7
//		addps		xmm6, xmm7
//		movaps		xmm5, lightDir2
//		movaps		xmm2, xmm5
//		mulps		xmm5, xmm5
//		addps		xmm6, xmm5
//		rsqrtps		xmm6, xmm6
//
//		mulps		xmm0, xmm6
//		mulps		xmm1, xmm6
//		mulps		xmm2, xmm6
//
//		movaps		xmm3, viewDir0
//		movaps		xmm7, xmm3
//		mulps		xmm7, xmm7
//		movaps		xmm4, viewDir1
//		movaps		xmm6, xmm4
//		mulps		xmm6, xmm6
//		addps		xmm7, xmm6
//		movaps		xmm5, viewDir2
//		movaps		xmm6, xmm5
//		mulps		xmm6, xmm6
//		addps		xmm7, xmm6
//		rsqrtps		xmm7, xmm7
//
//		mulps		xmm3, xmm7
//		addps		xmm0, xmm3
//		mulps		xmm4, xmm7
//		addps		xmm1, xmm4
//		mulps		xmm5, xmm7
//		addps		xmm2, xmm5
//
//		movaps		xmm3, tangent0
//		mulps		xmm3, xmm0
//		movaps		xmm4, tangent1
//		mulps		xmm4, xmm1
//		addps		xmm3, xmm4
//		movaps		xmm5, tangent2
//		mulps		xmm5, xmm2
//		addps		xmm5, xmm3
//
//		movaps		xmm3, tangent3
//		mulps		xmm3, xmm0
//		movaps		xmm4, tangent4
//		mulps		xmm4, xmm1
//		addps		xmm3, xmm4
//		movaps		xmm6, tangent5
//		mulps		xmm6, xmm2
//		addps		xmm6, xmm3
//
//		mulps		xmm0, normal0
//		mulps		xmm1, normal1
//		addps		xmm0, xmm1
//		mulps		xmm2, normal2
//		addps		xmm0, xmm2
//
//		mov			ecx, numVerts
//		shl			ecx, 4
//		mov			edx, usedVertNums[0]
//		add			ecx, texCoords
//		shl			edx, 4
//		movss		xmm3, SIMD_SP_one
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//		movss		[ecx+edx+12], xmm3
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[4]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shl			edx, 4
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//		movss		[ecx+edx+12], xmm3
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[8]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shl			edx, 4
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//		movss		[ecx+edx+12], xmm3
//
//		shufps		xmm5, xmm5, R_SHUFFLEPS( 1, 2, 3, 0 )
//		mov			edx, usedVertNums[12]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 1, 2, 3, 0 )
//		shl			edx, 4
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 1, 2, 3, 0 )
//
//		movss		[ecx+edx+0], xmm5
//		movss		[ecx+edx+4], xmm6
//		movss		[ecx+edx+8], xmm0
//		movss		[ecx+edx+12], xmm3
//
//		xor			ecx, ecx
//		jmp			loopVert4
//
//	done4:
//		test		ecx, ecx
//		jz			done
//		xor			eax, eax
//		mov			edi, numVerts
//		shl			edi, 4
//		add			edi, texCoords
//
//	loopVert1:
//		movss		xmm6, lightDir0[eax*4]
//		movss		xmm0, xmm6
//		mulss		xmm6, xmm6
//		movss		xmm7, lightDir1[eax*4]
//		movss		xmm1, xmm7
//		mulss		xmm7, xmm7
//		addss		xmm6, xmm7
//		movss		xmm5, lightDir2[eax*4]
//		movss		xmm2, xmm5
//		mulss		xmm5, xmm5
//		addss		xmm6, xmm5
//		rsqrtss		xmm6, xmm6
//
//		mulss		xmm0, xmm6
//		mulss		xmm1, xmm6
//		mulss		xmm2, xmm6
//
//		movss		xmm3, viewDir0[eax*4]
//		movss		xmm7, xmm3
//		mulss		xmm7, xmm7
//		movss		xmm4, viewDir1[eax*4]
//		movss		xmm6, xmm4
//		mulss		xmm6, xmm6
//		addss		xmm7, xmm6
//		movss		xmm5, viewDir2[eax*4]
//		movss		xmm6, xmm5
//		mulss		xmm6, xmm6
//		addss		xmm7, xmm6
//		rsqrtss		xmm7, xmm7
//
//		mulss		xmm3, xmm7
//		addss		xmm0, xmm3
//		mulss		xmm4, xmm7
//		addss		xmm1, xmm4
//		mulss		xmm5, xmm7
//		addss		xmm2, xmm5
//
//		mov			edx, usedVertNums[eax*4]
//		shl			edx, 4
//
//		movss		xmm3, tangent0[eax*4]
//		mulss		xmm3, xmm0
//		movss		xmm4, tangent1[eax*4]
//		mulss		xmm4, xmm1
//		addss		xmm3, xmm4
//		movss		xmm5, tangent2[eax*4]
//		mulss		xmm5, xmm2
//		addss		xmm5, xmm3
//		movss		[edi+edx+0], xmm5
//
//		movss		xmm3, tangent3[eax*4]
//		mulss		xmm3, xmm0
//		movss		xmm4, tangent4[eax*4]
//		mulss		xmm4, xmm1
//		addss		xmm3, xmm4
//		movss		xmm6, tangent5[eax*4]
//		mulss		xmm6, xmm2
//		addss		xmm6, xmm3
//		movss		[edi+edx+4], xmm6
//
//		mulss		xmm0, normal0[eax*4]
//		mulss		xmm1, normal1[eax*4]
//		addss		xmm0, xmm1
//		mulss		xmm2, normal2[eax*4]
//		addss		xmm0, xmm2
//		movss		[edi+edx+8], xmm0
//
//		movss		xmm3, SIMD_SP_one
//		movss		[edi+edx+12], xmm3
//
//		inc			eax
//		dec			ecx
//		jg			loopVert1
//
//	done:
//	}
//
//#else
//
//	ALIGN16( int usedVertNums[4] );
//	ALIGN16( float lightDir0[4] );
//	ALIGN16( float lightDir1[4] );
//	ALIGN16( float lightDir2[4] );
//	ALIGN16( float viewDir0[4] );
//	ALIGN16( float viewDir1[4] );
//	ALIGN16( float viewDir2[4] );
//	ALIGN16( float normal0[4] );
//	ALIGN16( float normal1[4] );
//	ALIGN16( float normal2[4] );
//	ALIGN16( float tangent0[4] );
//	ALIGN16( float tangent1[4] );
//	ALIGN16( float tangent2[4] );
//	ALIGN16( float tangent3[4] );
//	ALIGN16( float tangent4[4] );
//	ALIGN16( float tangent5[4] );
//	ALIGN16( float texCoords0[4] );
//	ALIGN16( float texCoords1[4] );
//	ALIGN16( float texCoords2[4] );
//	idVec3 localLightOrigin = lightOrigin;
//	idVec3 localViewOrigin = viewOrigin;
//	int numUsedVerts = 0;
//
//	for ( int i = 0; i < numVerts; i++ ) {
//		if ( !used[i] ) {
//			continue;
//		}
//
//		const idDrawVert *v = &verts[i];
//
//		lightDir0[numUsedVerts] = localLightOrigin[0] - v->xyz[0];
//		lightDir1[numUsedVerts] = localLightOrigin[1] - v->xyz[1];
//		lightDir2[numUsedVerts] = localLightOrigin[2] - v->xyz[2];
//
//		viewDir0[numUsedVerts] = localViewOrigin[0] - v->xyz[0];
//		viewDir1[numUsedVerts] = localViewOrigin[1] - v->xyz[1];
//		viewDir2[numUsedVerts] = localViewOrigin[2] - v->xyz[2];
//
//		normal0[numUsedVerts] = v->normal[0];
//		normal1[numUsedVerts] = v->normal[1];
//		normal2[numUsedVerts] = v->normal[2];
//
//		tangent0[numUsedVerts] = v->tangents[0][0];
//		tangent1[numUsedVerts] = v->tangents[0][1];
//		tangent2[numUsedVerts] = v->tangents[0][2];
//
//		tangent3[numUsedVerts] = v->tangents[1][0];
//		tangent4[numUsedVerts] = v->tangents[1][1];
//		tangent5[numUsedVerts] = v->tangents[1][2];
//
//		usedVertNums[numUsedVerts++] = i;
//		if ( numUsedVerts < 4 ) {
//			continue;
//		}
//
//		ALIGN16( float temp[4] );
//
//		temp[0] = lightDir0[0] * lightDir0[0];
//		temp[1] = lightDir0[1] * lightDir0[1];
//		temp[2] = lightDir0[2] * lightDir0[2];
//		temp[3] = lightDir0[3] * lightDir0[3];
//
//		temp[0] += lightDir1[0] * lightDir1[0];
//		temp[1] += lightDir1[1] * lightDir1[1];
//		temp[2] += lightDir1[2] * lightDir1[2];
//		temp[3] += lightDir1[3] * lightDir1[3];
//
//		temp[0] += lightDir2[0] * lightDir2[0];
//		temp[1] += lightDir2[1] * lightDir2[1];
//		temp[2] += lightDir2[2] * lightDir2[2];
//		temp[3] += lightDir2[3] * lightDir2[3];
//
//		temp[0] = idMath::RSqrt( temp[0] );
//		temp[1] = idMath::RSqrt( temp[1] );
//		temp[2] = idMath::RSqrt( temp[2] );
//		temp[3] = idMath::RSqrt( temp[3] );
//
//		lightDir0[0] *= temp[0];
//		lightDir0[1] *= temp[1];
//		lightDir0[2] *= temp[2];
//		lightDir0[3] *= temp[3];
//
//		lightDir1[0] *= temp[0];
//		lightDir1[1] *= temp[1];
//		lightDir1[2] *= temp[2];
//		lightDir1[3] *= temp[3];
//
//		lightDir2[0] *= temp[0];
//		lightDir2[1] *= temp[1];
//		lightDir2[2] *= temp[2];
//		lightDir2[3] *= temp[3];
//
//		temp[0] = viewDir0[0] * viewDir0[0];
//		temp[1] = viewDir0[1] * viewDir0[1];
//		temp[2] = viewDir0[2] * viewDir0[2];
//		temp[3] = viewDir0[3] * viewDir0[3];
//
//		temp[0] += viewDir1[0] * viewDir1[0];
//		temp[1] += viewDir1[1] * viewDir1[1];
//		temp[2] += viewDir1[2] * viewDir1[2];
//		temp[3] += viewDir1[3] * viewDir1[3];
//
//		temp[0] += viewDir2[0] * viewDir2[0];
//		temp[1] += viewDir2[1] * viewDir2[1];
//		temp[2] += viewDir2[2] * viewDir2[2];
//		temp[3] += viewDir2[3] * viewDir2[3];
//
//		temp[0] = idMath::RSqrt( temp[0] );
//		temp[1] = idMath::RSqrt( temp[1] );
//		temp[2] = idMath::RSqrt( temp[2] );
//		temp[3] = idMath::RSqrt( temp[3] );
//
//		viewDir0[0] *= temp[0];
//		viewDir0[1] *= temp[1];
//		viewDir0[2] *= temp[2];
//		viewDir0[3] *= temp[3];
//
//		viewDir1[0] *= temp[0];
//		viewDir1[1] *= temp[1];
//		viewDir1[2] *= temp[2];
//		viewDir1[3] *= temp[3];
//
//		viewDir2[0] *= temp[0];
//		viewDir2[1] *= temp[1];
//		viewDir2[2] *= temp[2];
//		viewDir2[3] *= temp[3];
//
//		lightDir0[0] += viewDir0[0];
//		lightDir0[1] += viewDir0[1];
//		lightDir0[2] += viewDir0[2];
//		lightDir0[3] += viewDir0[3];
//
//		lightDir1[0] += viewDir1[0];
//		lightDir1[1] += viewDir1[1];
//		lightDir1[2] += viewDir1[2];
//		lightDir1[3] += viewDir1[3];
//
//		lightDir2[0] += viewDir2[0];
//		lightDir2[1] += viewDir2[1];
//		lightDir2[2] += viewDir2[2];
//		lightDir2[3] += viewDir2[3];
//
//		texCoords0[0] = lightDir0[0] * tangent0[0];
//		texCoords0[1] = lightDir0[1] * tangent0[1];
//		texCoords0[2] = lightDir0[2] * tangent0[2];
//		texCoords0[3] = lightDir0[3] * tangent0[3];
//
//		texCoords0[0] += lightDir1[0] * tangent1[0];
//		texCoords0[1] += lightDir1[1] * tangent1[1];
//		texCoords0[2] += lightDir1[2] * tangent1[2];
//		texCoords0[3] += lightDir1[3] * tangent1[3];
//
//		texCoords0[0] += lightDir2[0] * tangent2[0];
//		texCoords0[1] += lightDir2[1] * tangent2[1];
//		texCoords0[2] += lightDir2[2] * tangent2[2];
//		texCoords0[3] += lightDir2[3] * tangent2[3];
//
//		texCoords1[0] = lightDir0[0] * tangent3[0];
//		texCoords1[1] = lightDir0[1] * tangent3[1];
//		texCoords1[2] = lightDir0[2] * tangent3[2];
//		texCoords1[3] = lightDir0[3] * tangent3[3];
//
//		texCoords1[0] += lightDir1[0] * tangent4[0];
//		texCoords1[1] += lightDir1[1] * tangent4[1];
//		texCoords1[2] += lightDir1[2] * tangent4[2];
//		texCoords1[3] += lightDir1[3] * tangent4[3];
//
//		texCoords1[0] += lightDir2[0] * tangent5[0];
//		texCoords1[1] += lightDir2[1] * tangent5[1];
//		texCoords1[2] += lightDir2[2] * tangent5[2];
//		texCoords1[3] += lightDir2[3] * tangent5[3];
//
//		texCoords2[0] = lightDir0[0] * normal0[0];
//		texCoords2[1] = lightDir0[1] * normal0[1];
//		texCoords2[2] = lightDir0[2] * normal0[2];
//		texCoords2[3] = lightDir0[3] * normal0[3];
//
//		texCoords2[0] += lightDir1[0] * normal1[0];
//		texCoords2[1] += lightDir1[1] * normal1[1];
//		texCoords2[2] += lightDir1[2] * normal1[2];
//		texCoords2[3] += lightDir1[3] * normal1[3];
//
//		texCoords2[0] += lightDir2[0] * normal2[0];
//		texCoords2[1] += lightDir2[1] * normal2[1];
//		texCoords2[2] += lightDir2[2] * normal2[2];
//		texCoords2[3] += lightDir2[3] * normal2[3];
//
//		for ( int j = 0; j < 4; j++ ) {
//			int n = usedVertNums[j];
//
//			texCoords[n][0] = texCoords0[j];
//			texCoords[n][1] = texCoords1[j];
//			texCoords[n][2] = texCoords2[j];
//			texCoords[n][3] = 1.0f;
//		}
//
//		numUsedVerts = 0;
//	}
//
//	for ( int i = 0; i < numUsedVerts; i++ ) {
//		float temp;
//
//		temp = lightDir0[i] * lightDir0[i] + lightDir1[i] * lightDir1[i] + lightDir2[i] * lightDir2[i];
//		temp = idMath::RSqrt( temp );
//
//		lightDir0[i] *= temp;
//		lightDir1[i] *= temp;
//		lightDir2[i] *= temp;
//
//		temp = viewDir0[i] * viewDir0[i] + viewDir1[i] * viewDir1[i] + viewDir2[i] * viewDir2[i];
//		temp = idMath::RSqrt( temp );
//
//		viewDir0[i] *= temp;
//		viewDir1[i] *= temp;
//		viewDir2[i] *= temp;
//
//		lightDir0[i] += viewDir0[i];
//		lightDir1[i] += viewDir1[i];
//		lightDir2[i] += viewDir2[i];
//
//		texCoords0[i] = lightDir0[i] * tangent0[i] + lightDir1[i] * tangent1[i] + lightDir2[i] * tangent2[i];
//		texCoords1[i] = lightDir0[i] * tangent3[i] + lightDir1[i] * tangent4[i] + lightDir2[i] * tangent5[i];
//		texCoords2[i] = lightDir0[i] * normal0[i] + lightDir1[i] * normal1[i] + lightDir2[i] * normal2[i];
//
//		int n = usedVertNums[i];
//		texCoords[n][0] = texCoords0;
//		texCoords[n][1] = texCoords1;
//		texCoords[n][2] = texCoords2;
//		texCoords[n][3] = 1.0f;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::CreateShadowCache
//============
//*/
//int VPCALL idSIMD_SSE::CreateShadowCache( idVec4 *vertexCache, int *vertRemap, const idVec3 &lightOrigin, const idDrawVert *verts, const int numVerts ) {
//#if 1
//	int outVerts;
//
//	__asm {
//		push		ebx
//
//		mov			esi, lightOrigin
//		movaps		xmm5, SIMD_SP_lastOne
//		movss		xmm6, [esi+0]
//		movhps		xmm6, [esi+4]
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 2, 3, 1 )
//		orps		xmm6, SIMD_SP_lastOne
//		movaps		xmm7, xmm6
//
//		xor			ebx, ebx
//		xor			ecx, ecx
//
//		mov			edx, vertRemap
//		mov			esi, verts
//		mov			edi, vertexCache
//		mov			eax, numVerts
//		and			eax, ~3
//		jz			done4
//		shl			eax, 2
//		add			edx, eax
//		neg			eax
//
//	loop4:
//		prefetchnta	[edx+128]
//		prefetchnta	[esi+4*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET]
//
//		cmp         dword ptr [edx+eax+0], ebx
//		jne         skip1
//
//		mov			dword ptr [edx+eax+0], ecx
//		movss		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		add			ecx, 2
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 2, 3, 0, 1 );
//		orps		xmm0, xmm5
//		movaps		[edi+0*16], xmm0
//		subps		xmm0, xmm6
//		movaps		[edi+1*16], xmm0
//		add			edi, 2*16
//
//	skip1:
//		cmp         dword ptr [edx+eax+4], ebx
//		jne         skip2
//
//		mov			dword ptr [edx+eax+4], ecx
//		movss		xmm1, [esi+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm1, [esi+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		add			ecx, 2
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 2, 3, 1 )
//		orps		xmm1, xmm5
//		movaps		[edi+0*16], xmm1
//		subps		xmm1, xmm7
//		movaps		[edi+1*16], xmm1
//		add			edi, 2*16
//
//	skip2:
//		cmp         dword ptr [edx+eax+8], ebx
//		jne         skip3
//
//		mov			dword ptr [edx+eax+8], ecx
//		movss		xmm2, [esi+2*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm2, [esi+2*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		add			ecx, 2
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 2, 3, 0, 1 );
//		orps		xmm2, xmm5
//		movaps		[edi+0*16], xmm2
//		subps		xmm2, xmm6
//		movaps		[edi+1*16], xmm2
//		add			edi, 2*16
//
//	skip3:
//		cmp         dword ptr [edx+eax+12], ebx
//		jne         skip4
//
//		mov			dword ptr [edx+eax+12], ecx
//		movss		xmm3, [esi+3*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm3, [esi+3*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		add			ecx, 2
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 0, 2, 3, 1 )
//		orps		xmm3, xmm5
//		movaps		[edi+0*16], xmm3
//		subps		xmm3, xmm7
//		movaps		[edi+1*16], xmm3
//		add			edi, 2*16
//
//	skip4:
//		add			esi, 4*DRAWVERT_SIZE
//		add			eax, 4*4
//		jl			loop4
//
//	done4:
//		mov			eax, numVerts
//		and			eax, 3
//		jz			done1
//		shl			eax, 2
//		add			edx, eax
//		neg			eax
//
//	loop1:
//		cmp         dword ptr [edx+eax+0], ebx
//		jne         skip0
//
//		mov			dword ptr [edx+eax+0], ecx
//		movss		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		add			ecx, 2
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 2, 3, 0, 1 )
//		orps		xmm0, xmm5
//		movaps		[edi+0*16], xmm0
//		subps		xmm0, xmm6
//		movaps		[edi+1*16], xmm0
//		add			edi, 2*16
//
//	skip0:
//
//		add			esi, DRAWVERT_SIZE
//		add			eax, 4
//		jl			loop1
//
//	done1:
//		pop			ebx
//		mov			outVerts, ecx
//	}
//	return outVerts;
//
//#else
//
//	int outVerts = 0;
//	for ( int i = 0; i < numVerts; i++ ) {
//		if ( vertRemap[i] ) {
//			continue;
//		}
//		const float *v = verts[i].xyz.ToFloatPtr();
//		vertexCache[outVerts+0][0] = v[0];
//		vertexCache[outVerts+0][1] = v[1];
//		vertexCache[outVerts+0][2] = v[2];
//		vertexCache[outVerts+0][3] = 1.0f;
//
//		// R_SetupProjection() builds the projection matrix with a slight crunch
//		// for depth, which keeps this w=0 division from rasterizing right at the
//		// wrap around point and causing depth fighting with the rear caps
//		vertexCache[outVerts+1][0] = v[0] - lightOrigin[0];
//		vertexCache[outVerts+1][1] = v[1] - lightOrigin[1];
//		vertexCache[outVerts+1][2] = v[2] - lightOrigin[2];
//		vertexCache[outVerts+1][3] = 0.0f;
//		vertRemap[i] = outVerts;
//		outVerts += 2;
//	}
//	return outVerts;
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::CreateVertexProgramShadowCache
//============
//*/
//int VPCALL idSIMD_SSE::CreateVertexProgramShadowCache( idVec4 *vertexCache, const idDrawVert *verts, const int numVerts ) {
//#if 1
//
//	__asm {
//		movaps		xmm4, SIMD_SP_lastOne
//		movaps		xmm5, xmm4
//		movaps		xmm6, xmm4
//		movaps		xmm7, xmm4
//
//		mov			esi, verts
//		mov			edi, vertexCache
//		mov			eax, numVerts
//		and			eax, ~3
//		jz			done4
//		shl			eax, 5
//		add			edi, eax
//		neg			eax
//
//	loop4:
//		prefetchnta	[esi+4*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET]
//
//		movss		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 2, 3, 0, 1 );
//		movaps		[edi+eax+1*16], xmm0
//		orps		xmm0, xmm4
//		movaps		[edi+eax+0*16], xmm0
//
//		movss		xmm1, [esi+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm1, [esi+1*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 2, 3, 1 )
//		movaps		[edi+eax+3*16], xmm1
//		orps		xmm1, xmm5
//		movaps		[edi+eax+2*16], xmm1
//
//		movss		xmm2, [esi+2*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm2, [esi+2*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 2, 3, 0, 1 );
//		movaps		[edi+eax+5*16], xmm2
//		orps		xmm2, xmm6
//		movaps		[edi+eax+4*16], xmm2
//
//		movss		xmm3, [esi+3*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		movhps		xmm3, [esi+3*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+4]
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 0, 2, 3, 1 )
//		movaps		[edi+eax+7*16], xmm3
//		orps		xmm3, xmm7
//		movaps		[edi+eax+6*16], xmm3
//
//		add			esi, 4*DRAWVERT_SIZE
//		add			eax, 4*8*4
//		jl			loop4
//
//	done4:
//		mov			eax, numVerts
//		and			eax, 3
//		jz			done1
//		shl			eax, 5
//		add			edi, eax
//		neg			eax
//
//	loop1:
//		movss		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+8]
//		movhps		xmm0, [esi+0*DRAWVERT_SIZE+DRAWVERT_XYZ_OFFSET+0]
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 2, 3, 0, 1 );
//		movaps		[edi+eax+1*16], xmm0
//		orps		xmm0, xmm4
//		movaps		[edi+eax+0*16], xmm0
//
//		add			esi, DRAWVERT_SIZE
//		add			eax, 8*4
//		jl			loop1
//
//	done1:
//	}
//	return numVerts * 2;
//
//#else
//
//	for ( int i = 0; i < numVerts; i++ ) {
//		const float *v = verts[i].xyz.ToFloatPtr();
//		vertexCache[i*2+0][0] = v[0];
//		vertexCache[i*2+0][1] = v[1];
//		vertexCache[i*2+0][2] = v[2];
//		vertexCache[i*2+0][3] = 1.0f;
//
//		vertexCache[i*2+1][0] = v[0];
//		vertexCache[i*2+1][1] = v[1];
//		vertexCache[i*2+1][2] = v[2];
//		vertexCache[i*2+1][3] = 0.0f;
//	}
//	return numVerts * 2;
//
//#endif
//}
//
//
//
//
//
//
///*
//============
//idSIMD_SSE::UpSamplePCMTo44kHz
//
//  Duplicate samples for 44kHz output.
//============
//*/
//void idSIMD_SSE::UpSamplePCMTo44kHz( float *dest, const short *src, const int numSamples, const int kHz, const int numChannels ) {
//	if ( kHz == 11025 ) {
//		if ( numChannels == 1 ) {
//			SSE_UpSample11kHzMonoPCMTo44kHz( dest, src, numSamples );
//		} else {
//			SSE_UpSample11kHzStereoPCMTo44kHz( dest, src, numSamples );
//		}
//	} else if ( kHz == 22050 ) {
//		if ( numChannels == 1 ) {
//			SSE_UpSample22kHzMonoPCMTo44kHz( dest, src, numSamples );
//		} else {
//			SSE_UpSample22kHzStereoPCMTo44kHz( dest, src, numSamples );
//		}
//	} else if ( kHz == 44100 ) {
//		SSE_UpSample44kHzMonoPCMTo44kHz( dest, src, numSamples );
//	} else {
//		assert( 0 );
//	}
//}
//
//
///*
//============
//SSE_UpSample44kHzMonoOGGTo44kHz
//============
//*/
//static void SSE_UpSample44kHzMonoOGGTo44kHz( float *dest, const float *src, const int numSamples ) {
//	float constant = 32768.0f;
//	KFLOAT_CA( mul, dest, src, constant, numSamples )
//}
//
//
///*
//============
//idSIMD_SSE::UpSampleOGGTo44kHz
//
//  Duplicate samples for 44kHz output.
//============
//*/
//void idSIMD_SSE::UpSampleOGGTo44kHz( float *dest, const float * const *ogg, const int numSamples, const int kHz, const int numChannels ) {
//	if ( kHz == 11025 ) {
//		if ( numChannels == 1 ) {
//			SSE_UpSample11kHzMonoOGGTo44kHz( dest, ogg[0], numSamples );
//		} else {
//			SSE_UpSample11kHzStereoOGGTo44kHz( dest, ogg, numSamples );
//		}
//	} else if ( kHz == 22050 ) {
//		if ( numChannels == 1 ) {
//			SSE_UpSample22kHzMonoOGGTo44kHz( dest, ogg[0], numSamples );
//		} else {
//			SSE_UpSample22kHzStereoOGGTo44kHz( dest, ogg, numSamples );
//		}
//	} else if ( kHz == 44100 ) {
//		if ( numChannels == 1 ) {
//			SSE_UpSample44kHzMonoOGGTo44kHz( dest, ogg[0], numSamples );
//		} else {
//			SSE_UpSample44kHzStereoOGGTo44kHz( dest, ogg, numSamples );
//		}
//	} else {
//		assert( 0 );
//	}
//}
//
///*
//============
//idSIMD_SSE::MixSoundTwoSpeakerMono
//============
//*/
//void VPCALL idSIMD_SSE::MixSoundTwoSpeakerMono( float *mixBuffer, const float *samples, const int numSamples, const float lastV[2], const float currentV[2] ) {
//#if 1
//
//	ALIGN16( float incs[2] );
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incs[0] = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incs[1] = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//
//	__asm {
//		mov			eax, MIXBUFFER_SAMPLES
//		mov			edi, mixBuffer
//		mov			esi, samples
//		shl			eax, 2
//		add			esi, eax
//		neg			eax
//
//		mov			ecx, lastV
//		movlps		xmm6, [ecx]
//		xorps		xmm7, xmm7
//		movhps		xmm7, incs
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 1, 0, 1 )
//		addps		xmm6, xmm7
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 2, 3, 2, 3 )
//		addps		xmm7, xmm7
//
//	loop16:
//		add			edi, 4*4*4
//
//		movaps		xmm0, [esi+eax+0*4*4]
//		movaps		xmm1, xmm0
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 0, 0, 1, 1 )
//		mulps		xmm0, xmm6
//		addps		xmm0, [edi-4*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-4*4*4], xmm0
//
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 2, 2, 3, 3 )
//		mulps		xmm1, xmm6
//		addps		xmm1, [edi-3*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-3*4*4], xmm1
//
//		movaps		xmm2, [esi+eax+1*4*4]
//		movaps		xmm3, xmm2
//		shufps		xmm2, xmm2, R_SHUFFLEPS( 0, 0, 1, 1 )
//		mulps		xmm2, xmm6
//		addps		xmm2, [edi-2*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-2*4*4], xmm2
//
//		shufps		xmm3, xmm3, R_SHUFFLEPS( 2, 2, 3, 3 )
//		mulps		xmm3, xmm6
//		addps		xmm3, [edi-1*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-1*4*4], xmm3
//
//		add			eax, 2*4*4
//
//		jl			loop16
//	}
//
//#else
//
//	int i;
//	float incL;
//	float incR;
//	float sL0, sL1;
//	float sR0, sR1;
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incL = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incR = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//
//	sL0 = lastV[0];
//	sR0 = lastV[1];
//	sL1 = lastV[0] + incL;
//	sR1 = lastV[1] + incR;
//
//	incL *= 2;
//	incR *= 2;
//
//	for( i = 0; i < MIXBUFFER_SAMPLES; i += 2 ) {
//		mixBuffer[i*2+0] += samples[i+0] * sL0;
//		mixBuffer[i*2+1] += samples[i+0] * sR0;
//		mixBuffer[i*2+2] += samples[i+1] * sL1;
//		mixBuffer[i*2+3] += samples[i+1] * sR1;
//		sL0 += incL;
//		sR0 += incR;
//		sL1 += incL;
//		sR1 += incR;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::MixSoundTwoSpeakerStereo
//============
//*/
//void VPCALL idSIMD_SSE::MixSoundTwoSpeakerStereo( float *mixBuffer, const float *samples, const int numSamples, const float lastV[2], const float currentV[2] ) {
//#if 1
//
//	ALIGN16( float incs[2] );
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incs[0] = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incs[1] = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//
//	__asm {
//		mov			eax, MIXBUFFER_SAMPLES
//		mov			edi, mixBuffer
//		mov			esi, samples
//		shl			eax, 3
//		add			esi, eax
//		neg			eax
//
//		mov			ecx, lastV
//		movlps		xmm6, [ecx]
//		xorps		xmm7, xmm7
//		movhps		xmm7, incs
//		shufps		xmm6, xmm6, R_SHUFFLEPS( 0, 1, 0, 1 )
//		addps		xmm6, xmm7
//		shufps		xmm7, xmm7, R_SHUFFLEPS( 2, 3, 2, 3 )
//		addps		xmm7, xmm7
//
//	loop16:
//		add			edi, 4*4*4
//
//		movaps		xmm0, [esi+eax+0*4*4]
//		mulps		xmm0, xmm6
//		addps		xmm0, [edi-4*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-4*4*4], xmm0
//
//		movaps		xmm2, [esi+eax+1*4*4]
//		mulps		xmm2, xmm6
//		addps		xmm2, [edi-3*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-3*4*4], xmm2
//
//		movaps		xmm3, [esi+eax+2*4*4]
//		mulps		xmm3, xmm6
//		addps		xmm3, [edi-2*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-2*4*4], xmm3
//
//		movaps		xmm4, [esi+eax+3*4*4]
//		mulps		xmm4, xmm6
//		addps		xmm4, [edi-1*4*4]
//		addps		xmm6, xmm7
//		movaps		[edi-1*4*4], xmm4
//
//		add			eax, 4*4*4
//
//		jl			loop16
//	}
//
//#else
//
//	int i;
//	float incL;
//	float incR;
//	float sL0, sL1;
//	float sR0, sR1;
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incL = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incR = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//
//	sL0 = lastV[0];
//	sR0 = lastV[1];
//	sL1 = lastV[0] + incL;
//	sR1 = lastV[1] + incR;
//
//	incL *= 2;
//	incR *= 2;
//
//	for( i = 0; i < MIXBUFFER_SAMPLES; i += 2 ) {
//		mixBuffer[i*2+0] += samples[i*2+0] * sL0;
//		mixBuffer[i*2+1] += samples[i*2+1] * sR0;
//		mixBuffer[i*2+2] += samples[i*2+2] * sL1;
//		mixBuffer[i*2+3] += samples[i*2+3] * sR1;
//		sL0 += incL;
//		sR0 += incR;
//		sL1 += incL;
//		sR1 += incR;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::MixSoundSixSpeakerMono
//============
//*/
//void VPCALL idSIMD_SSE::MixSoundSixSpeakerMono( float *mixBuffer, const float *samples, const int numSamples, const float lastV[6], const float currentV[6] ) {
//#if 1
//
//	ALIGN16( float incs[6] );
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incs[0] = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incs[1] = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//	incs[2] = ( currentV[2] - lastV[2] ) / MIXBUFFER_SAMPLES;
//	incs[3] = ( currentV[3] - lastV[3] ) / MIXBUFFER_SAMPLES;
//	incs[4] = ( currentV[4] - lastV[4] ) / MIXBUFFER_SAMPLES;
//	incs[5] = ( currentV[5] - lastV[5] ) / MIXBUFFER_SAMPLES;
//
//	__asm {
//		mov			eax, MIXBUFFER_SAMPLES
//		mov			edi, mixBuffer
//		mov			esi, samples
//		shl			eax, 2
//		add			esi, eax
//		neg			eax
//
//		mov			ecx, lastV
//		movlps		xmm2, [ecx+ 0]
//		movhps		xmm2, [ecx+ 8]
//		movlps		xmm3, [ecx+16]
//		movaps		xmm4, xmm2
//		shufps		xmm3, xmm2, R_SHUFFLEPS( 0, 1, 0, 1 )
//		shufps		xmm4, xmm3, R_SHUFFLEPS( 2, 3, 0, 1 )
//
//		xorps		xmm5, xmm5
//		movhps		xmm5, incs
//		movlps		xmm7, incs+8
//		movhps		xmm7, incs+16
//		addps		xmm3, xmm5
//		addps		xmm4, xmm7
//		shufps		xmm5, xmm7, R_SHUFFLEPS( 2, 3, 0, 1 )
//		movaps		xmm6, xmm7
//		shufps		xmm6, xmm5, R_SHUFFLEPS( 2, 3, 0, 1 )
//		addps		xmm5, xmm5
//		addps		xmm6, xmm6
//		addps		xmm7, xmm7
//
//	loop24:
//		add			edi, 6*16
//
//		movaps		xmm0, [esi+eax]
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 0, 0 )
//		mulps		xmm1, xmm2
//		addps		xmm1, [edi-6*16]
//		addps		xmm2, xmm5
//		movaps		[edi-6*16], xmm1
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 0, 1, 1 )
//		mulps		xmm1, xmm3
//		addps		xmm1, [edi-5*16]
//		addps		xmm3, xmm6
//		movaps		[edi-5*16], xmm1
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 1, 1, 1, 1 )
//		mulps		xmm1, xmm4
//		addps		xmm1, [edi-4*16]
//		addps		xmm4, xmm7
//		movaps		[edi-4*16], xmm1
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 2, 2, 2, 2 )
//		mulps		xmm1, xmm2
//		addps		xmm1, [edi-3*16]
//		addps		xmm2, xmm5
//		movaps		[edi-3*16], xmm1
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 2, 2, 3, 3 )
//		mulps		xmm1, xmm3
//		addps		xmm1, [edi-2*16]
//		addps		xmm3, xmm6
//		movaps		[edi-2*16], xmm1
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 3, 3, 3, 3 )
//		mulps		xmm0, xmm4
//		addps		xmm0, [edi-1*16]
//		addps		xmm4, xmm7
//		movaps		[edi-1*16], xmm0
//
//		add			eax, 4*4
//
//		jl			loop24
//	}
//
//#else
//
//	int i;
//	float sL0, sL1, sL2, sL3, sL4, sL5, sL6, sL7, sL8, sL9, sL10, sL11;
//	float incL0, incL1, incL2, incL3, incL4, incL5;
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//
//	incL0 = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incL1 = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//	incL2 = ( currentV[2] - lastV[2] ) / MIXBUFFER_SAMPLES;
//	incL3 = ( currentV[3] - lastV[3] ) / MIXBUFFER_SAMPLES;
//	incL4 = ( currentV[4] - lastV[4] ) / MIXBUFFER_SAMPLES;
//	incL5 = ( currentV[5] - lastV[5] ) / MIXBUFFER_SAMPLES;
//
//	sL0  = lastV[0];
//	sL1  = lastV[1];
//	sL2  = lastV[2];
//	sL3  = lastV[3];
//	sL4  = lastV[4];
//	sL5  = lastV[5];
//
//	sL6  = lastV[0] + incL0;
//	sL7  = lastV[1] + incL1;
//	sL8  = lastV[2] + incL2;
//	sL9  = lastV[3] + incL3;
//	sL10 = lastV[4] + incL4;
//	sL11 = lastV[5] + incL5;
//
//	incL0 *= 2;
//	incL1 *= 2;
//	incL2 *= 2;
//	incL3 *= 2;
//	incL4 *= 2;
//	incL5 *= 2;
//
//	for( i = 0; i <= MIXBUFFER_SAMPLES - 2; i += 2 ) {
//		mixBuffer[i*6+ 0] += samples[i+0] * sL0;
//		mixBuffer[i*6+ 1] += samples[i+0] * sL1;
//		mixBuffer[i*6+ 2] += samples[i+0] * sL2;
//		mixBuffer[i*6+ 3] += samples[i+0] * sL3;
//
//		mixBuffer[i*6+ 4] += samples[i+0] * sL4;
//		mixBuffer[i*6+ 5] += samples[i+0] * sL5;
//		mixBuffer[i*6+ 6] += samples[i+1] * sL6;
//		mixBuffer[i*6+ 7] += samples[i+1] * sL7;
//
//		mixBuffer[i*6+ 8] += samples[i+1] * sL8;
//		mixBuffer[i*6+ 9] += samples[i+1] * sL9;
//		mixBuffer[i*6+10] += samples[i+1] * sL10;
//		mixBuffer[i*6+11] += samples[i+1] * sL11;
//
//		sL0  += incL0;
//		sL1  += incL1;
//		sL2  += incL2;
//		sL3  += incL3;
//
//		sL4  += incL4;
//		sL5  += incL5;
//		sL6  += incL0;
//		sL7  += incL1;
//
//		sL8  += incL2;
//		sL9  += incL3;
//		sL10 += incL4;
//		sL11 += incL5;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::MixSoundSixSpeakerStereo
//============
//*/
//void VPCALL idSIMD_SSE::MixSoundSixSpeakerStereo( float *mixBuffer, const float *samples, const int numSamples, const float lastV[6], const float currentV[6] ) {
//#if 1
//
//	ALIGN16( float incs[6] );
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//	assert( SPEAKER_RIGHT == 1 );
//	assert( SPEAKER_BACKRIGHT == 5 );
//
//	incs[0] = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incs[1] = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//	incs[2] = ( currentV[2] - lastV[2] ) / MIXBUFFER_SAMPLES;
//	incs[3] = ( currentV[3] - lastV[3] ) / MIXBUFFER_SAMPLES;
//	incs[4] = ( currentV[4] - lastV[4] ) / MIXBUFFER_SAMPLES;
//	incs[5] = ( currentV[5] - lastV[5] ) / MIXBUFFER_SAMPLES;
//
//	__asm {
//		mov			eax, MIXBUFFER_SAMPLES
//		mov			edi, mixBuffer
//		mov			esi, samples
//		shl			eax, 3
//		add			esi, eax
//		neg			eax
//
//		mov			ecx, lastV
//		movlps		xmm2, [ecx+ 0]
//		movhps		xmm2, [ecx+ 8]
//		movlps		xmm3, [ecx+16]
//		movaps		xmm4, xmm2
//		shufps		xmm3, xmm2, R_SHUFFLEPS( 0, 1, 0, 1 )
//		shufps		xmm4, xmm3, R_SHUFFLEPS( 2, 3, 0, 1 )
//
//		xorps		xmm5, xmm5
//		movhps		xmm5, incs
//		movlps		xmm7, incs+ 8
//		movhps		xmm7, incs+16
//		addps		xmm3, xmm5
//		addps		xmm4, xmm7
//		shufps		xmm5, xmm7, R_SHUFFLEPS( 2, 3, 0, 1 )
//		movaps		xmm6, xmm7
//		shufps		xmm6, xmm5, R_SHUFFLEPS( 2, 3, 0, 1 )
//		addps		xmm5, xmm5
//		addps		xmm6, xmm6
//		addps		xmm7, xmm7
//
//	loop12:
//		add			edi, 3*16
//
//		movaps		xmm0, [esi+eax+0]
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 1, 0, 0 )
//		mulps		xmm1, xmm2
//		addps		xmm1, [edi-3*16]
//		addps		xmm2, xmm5
//		movaps		[edi-3*16], xmm1
//
//		movaps		xmm1, xmm0
//		shufps		xmm1, xmm1, R_SHUFFLEPS( 0, 1, 2, 3 )
//		mulps		xmm1, xmm3
//		addps		xmm1, [edi-2*16]
//		addps		xmm3, xmm6
//		movaps		[edi-2*16], xmm1
//
//		add			eax, 4*4
//
//		shufps		xmm0, xmm0, R_SHUFFLEPS( 2, 2, 2, 3 )
//		mulps		xmm0, xmm4
//		addps		xmm0, [edi-1*16]
//		addps		xmm4, xmm7
//		movaps		[edi-1*16], xmm0
//
//		jl			loop12
//
//		emms
//	}
//
//#else
//
//	int i;
//	float sL0, sL1, sL2, sL3, sL4, sL5, sL6, sL7, sL8, sL9, sL10, sL11;
//	float incL0, incL1, incL2, incL3, incL4, incL5;
//
//	assert( numSamples == MIXBUFFER_SAMPLES );
//	assert( SPEAKER_RIGHT == 1 );
//	assert( SPEAKER_BACKRIGHT == 5 );
//
//	incL0 = ( currentV[0] - lastV[0] ) / MIXBUFFER_SAMPLES;
//	incL1 = ( currentV[1] - lastV[1] ) / MIXBUFFER_SAMPLES;
//	incL2 = ( currentV[2] - lastV[2] ) / MIXBUFFER_SAMPLES;
//	incL3 = ( currentV[3] - lastV[3] ) / MIXBUFFER_SAMPLES;
//	incL4 = ( currentV[4] - lastV[4] ) / MIXBUFFER_SAMPLES;
//	incL5 = ( currentV[5] - lastV[5] ) / MIXBUFFER_SAMPLES;
//
//	sL0  = lastV[0];
//	sL1  = lastV[1];
//	sL2  = lastV[2];
//	sL3  = lastV[3];
//	sL4  = lastV[4];
//	sL5  = lastV[5];
//
//	sL6  = lastV[0] + incL0;
//	sL7  = lastV[1] + incL1;
//	sL8  = lastV[2] + incL2;
//	sL9  = lastV[3] + incL3;
//	sL10 = lastV[4] + incL4;
//	sL11 = lastV[5] + incL5;
//
//	incL0 *= 2;
//	incL1 *= 2;
//	incL2 *= 2;
//	incL3 *= 2;
//	incL4 *= 2;
//	incL5 *= 2;
//
//	for( i = 0; i <= MIXBUFFER_SAMPLES - 2; i += 2 ) {
//		mixBuffer[i*6+ 0] += samples[i*2+0+0] * sL0;
//		mixBuffer[i*6+ 1] += samples[i*2+0+1] * sL1;
//		mixBuffer[i*6+ 2] += samples[i*2+0+0] * sL2;
//		mixBuffer[i*6+ 3] += samples[i*2+0+0] * sL3;
//
//		mixBuffer[i*6+ 4] += samples[i*2+0+0] * sL4;
//		mixBuffer[i*6+ 5] += samples[i*2+0+1] * sL5;
//		mixBuffer[i*6+ 6] += samples[i*2+2+0] * sL6;
//		mixBuffer[i*6+ 7] += samples[i*2+2+1] * sL7;
//
//		mixBuffer[i*6+ 8] += samples[i*2+2+0] * sL8;
//		mixBuffer[i*6+ 9] += samples[i*2+2+0] * sL9;
//		mixBuffer[i*6+10] += samples[i*2+2+0] * sL10;
//		mixBuffer[i*6+11] += samples[i*2+2+1] * sL11;
//
//		sL0  += incL0;
//		sL1  += incL1;
//		sL2  += incL2;
//		sL3  += incL3;
//
//		sL4  += incL4;
//		sL5  += incL5;
//		sL6  += incL0;
//		sL7  += incL1;
//
//		sL8  += incL2;
//		sL9  += incL3;
//		sL10 += incL4;
//		sL11 += incL5;
//	}
//
//#endif
//}
//
///*
//============
//idSIMD_SSE::MixedSoundToSamples
//============
//*/
//void VPCALL idSIMD_SSE::MixedSoundToSamples( short *samples, const float *mixBuffer, const int numSamples ) {
//#if 1
//
//	assert( ( numSamples % MIXBUFFER_SAMPLES ) == 0 );
//
//	__asm {
//
//		mov			eax, numSamples
//		mov			edi, mixBuffer
//		mov			esi, samples
//		shl			eax, 2
//		add			edi, eax
//		neg			eax
//
//	loop16:
//
//		movaps		xmm0, [edi+eax+0*16]
//		movaps		xmm2, [edi+eax+1*16]
//		movaps		xmm4, [edi+eax+2*16]
//		movaps		xmm6, [edi+eax+3*16]
//
//		add			esi, 4*4*2
//
//		movhlps		xmm1, xmm0
//		movhlps		xmm3, xmm2
//		movhlps		xmm5, xmm4
//		movhlps		xmm7, xmm6
//
//		prefetchnta	[edi+eax+64]
//
//		cvtps2pi	mm0, xmm0
//		cvtps2pi	mm2, xmm2
//		cvtps2pi	mm4, xmm4
//		cvtps2pi	mm6, xmm6
//
//		prefetchnta	[edi+eax+128]
//
//		cvtps2pi	mm1, xmm1
//		cvtps2pi	mm3, xmm3
//		cvtps2pi	mm5, xmm5
//		cvtps2pi	mm7, xmm7
//
//		add			eax, 4*16
//
//		packssdw	mm0, mm1
//		packssdw	mm2, mm3
//		packssdw	mm4, mm5
//		packssdw	mm6, mm7
//
//		movq		[esi-4*4*2], mm0
//		movq		[esi-3*4*2], mm2
//		movq		[esi-2*4*2], mm4
//		movq		[esi-1*4*2], mm6
//
//		jl			loop16
//
//		emms
//	}
//
//#else
//
//	for ( int i = 0; i < numSamples; i++ ) {
//		if ( mixBuffer[i] <= -32768.0f ) {
//			samples[i] = -32768;
//		} else if ( mixBuffer[i] >= 32767.0f ) {
//			samples[i] = 32767;
//		} else {
//			samples[i] = (short) mixBuffer[i];
//		}
//	}
//
//#endif
//}

    }
}
