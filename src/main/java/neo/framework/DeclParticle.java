package neo.framework;

import static neo.TempDump.atof;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_TABLE;
import static neo.framework.DeclParticle.prtCustomPth_t.PPATH_FLIES;
import static neo.framework.DeclParticle.prtCustomPth_t.PPATH_HELIX;
import static neo.framework.DeclParticle.prtCustomPth_t.PPATH_ORBIT;
import static neo.framework.DeclParticle.prtCustomPth_t.PPATH_STANDARD;
import static neo.framework.DeclParticle.prtDirection_t.PDIR_CONE;
import static neo.framework.DeclParticle.prtDirection_t.PDIR_OUTWARD;
import static neo.framework.DeclParticle.prtDistribution_t.PDIST_CYLINDER;
import static neo.framework.DeclParticle.prtDistribution_t.PDIST_RECT;
import static neo.framework.DeclParticle.prtDistribution_t.PDIST_SPHERE;
import static neo.framework.DeclParticle.prtOrientation_t.POR_AIMED;
import static neo.framework.DeclParticle.prtOrientation_t.POR_VIEW;
import static neo.framework.DeclParticle.prtOrientation_t.POR_X;
import static neo.framework.DeclParticle.prtOrientation_t.POR_Y;
import static neo.framework.DeclParticle.prtOrientation_t.POR_Z;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;

import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclTable.idDeclTable;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Memory;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
import neo.open.ColorUtil;

/**
 *
 */
public class DeclParticle {

    /*
     ===============================================================================

     idDeclParticle

     ===============================================================================
     */
    static class ParticleParmDesc {
        final String name;
        int count;
        final String desc;

        public ParticleParmDesc(String name, int count, String desc) {
            this.name = name;
            this.count = count;
            this.desc = desc;
        }
    }
    static final ParticleParmDesc ParticleDistributionDesc[] = {
        new ParticleParmDesc("rect", 3, ""),
        new ParticleParmDesc("cylinder", 4, ""),
        new ParticleParmDesc("sphere", 3, "")
    };
    static final ParticleParmDesc ParticleDirectionDesc[] = {
        new ParticleParmDesc("cone", 1, ""),
        new ParticleParmDesc("outward", 1, "")
    };
    static final ParticleParmDesc ParticleOrientationDesc[] = {
        new ParticleParmDesc("view", 0, ""),
        new ParticleParmDesc("aimed", 2, ""),
        new ParticleParmDesc("x", 0, ""),
        new ParticleParmDesc("y", 0, ""),
        new ParticleParmDesc("z", 0, "")
    };
    static final ParticleParmDesc ParticleCustomDesc[] = {
        new ParticleParmDesc("standard", 0, "Standard"),
        new ParticleParmDesc("helix", 5, "sizeX Y Z radialSpeed axialSpeed"),
        new ParticleParmDesc("flies", 3, "radialSpeed axialSpeed size"),
        new ParticleParmDesc("orbit", 2, "radius speed"),
        new ParticleParmDesc("drip", 2, "something something")
    };
//const int CustomParticleCount = sizeof( ParticleCustomDesc ) / sizeof( const ParticleParmDesc );
    static final int CustomParticleCount = ParticleCustomDesc.length;

    /*
     ====================================================================================

     idParticleParm

     ====================================================================================
     */
    static class idParticleParm {
        public idDeclTable table;
        public float       from;
        public float       to;
        //
        //

        public idParticleParm() {
            this.table = null;
            this.from = this.to = 0.0f;
        }

        public float Eval(float frac, idRandom rand) {
            if (this.table != null) {
                return this.table.TableLookup(frac);
            }
            return this.from + (frac * (this.to - this.from));
        }

        public float Integrate(float frac, idRandom rand) throws idException {
            if (this.table != null) {
                common.Printf("idParticleParm::Integrate: can't integrate tables\n");
                return 0;
            }
            return (this.from + (frac * (this.to - this.from) * 0.5f)) * frac;
        }
    }

    enum prtDistribution_t {

        PDIST_RECT,     // ( sizeX sizeY sizeZ )
        PDIST_CYLINDER, // ( sizeX sizeY sizeZ )
        PDIST_SPHERE	// ( sizeX sizeY sizeZ ringFraction )
                        // a ringFraction of zero allows the entire sphere, 0.9 would only
                        // allow the outer 10% of the sphere
    }

    enum prtDirection_t {

        PDIR_CONE,      // parm0 is the solid cone angle
        PDIR_OUTWARD	// direction is relative to offset from origin, parm0 is an upward bias
    }

    enum prtCustomPth_t {

        PPATH_STANDARD,
        PPATH_HELIX, // ( sizeX sizeY sizeZ radialSpeed climbSpeed )
        PPATH_FLIES,
        PPATH_ORBIT,
        PPATH_DRIP
    }

    enum prtOrientation_t {

        POR_VIEW,
        POR_AIMED, // angle and aspect are disregarded
        POR_X,
        POR_Y,
        POR_Z
    }

    public static class particleGen_t {

        public renderEntity_s renderEnt;          // for shaderParms, etc
        public renderView_s   renderView;
        public int            index;              // particle number in the system
        public float          frac;               // 0.0 to 1.0
        public idRandom       random;
        public idVec3         origin;             // dynamic smoke particles can have individual origins and axis
        public idMat3         axis;
        //
        //
        public float          age;                // in seconds, calculated as fraction * stage->particleLife
        public idRandom       originalRandom;     // needed so aimed particles can reset the random for another origin calculation
        public float          animationFrameFrac; // set by ParticleTexCoords, used to make the cross faded version

        public particleGen_t(){
            this.origin = new idVec3();
            this.axis = new idMat3();
        }
    }

    //
    // single particle stage
    //
    public static class idParticleStage {

        public idMaterial        material;
        //
        public int               totalParticles;  // total number of particles, although some may be invisible at a given time
        public float             cycles;          // allows things to oneShot ( 1 cycle ) or run for a set number of cycles
        // on a per stage basis
        //
        public int               cycleMsec;       // ( particleLife + deadTime ) in msec
        //
        public float             spawnBunching;   // 0.0 = all come out at first instant, 1.0 = evenly spaced over cycle time
        public float             particleLife;    // total seconds of life for each particle
        public float             timeOffset;      // time offset from system start for the first particle to spawn
        public float             deadTime;        // time after particleLife before respawning
        //
        //-------------------------------	  // standard path parms
        //		
        public prtDistribution_t distributionType;
        public float[] distributionParms = new float[4];
        //
        public prtDirection_t directionType;
        public float[] directionParms = new float[4];
        //
        public idParticleParm speed;
        public float          gravity;            // can be negative to float up
        public boolean        worldGravity;       // apply gravity in world space
        public boolean        randomDistribution; // randomly orient the quad on emission ( defaults to true )
        public boolean        entityColor;        // force color from render entity ( fadeColor is still valid )
        //
        //------------------------------	  // custom path will completely replace the standard path calculations
        //	
        public prtCustomPth_t customPathType;     // use custom C code routines for determining the origin
        public float[] customPathParms = new float[8];
        //
        //--------------------------------
        //	
        public idVec3           offset;           // offset from origin to spawn all particles, also applies to customPath
        //
        public int              animationFrames;  // if > 1, subdivide the texture S axis into frames and crossfade
        public float            animationRate;    // frames per second
        //
        public float            initialAngle;     // in degrees, random angle is used if zero ( default )
        public idParticleParm   rotationSpeed;    // half the particles will have negative rotation speeds
        //
        public prtOrientation_t orientation;      // view, aimed, or axis fixed
        public float[] orientationParms = new float[4];
        //
        public idParticleParm size;
        public idParticleParm aspect;             // greater than 1 makes the T axis longer
        //
        public idVec4         color;
        public idVec4         fadeColor;          // either 0 0 0 0 for additive, or 1 1 1 0 for blended materials
        public float          fadeInFraction;     // in 0.0 to 1.0 range
        public float          fadeOutFraction;    // in 0.0 to 1.0 range
        public float          fadeIndexFraction;  // in 0.0 to 1.0 range, causes later index smokes to be more faded
        //
        public boolean        hidden;             // for editor use
        //-----------------------------------
        //
        public float          boundsExpansion;    // user tweak to fix poorly calculated bounds
        //
        public idBounds       bounds;             // derived
        //
        //

        public idParticleStage() {
            this.material = null;
            this.totalParticles = 0;
            this.cycles = 0.0f;
            this.cycleMsec = 0;
            this.spawnBunching = 0.0f;
            this.particleLife = 0.0f;
            this.timeOffset = 0.0f;
            this.deadTime = 0.0f;
            this.distributionType = PDIST_RECT;
            this.distributionParms[0] = this.distributionParms[1] = this.distributionParms[2] = this.distributionParms[3] = 0.0f;
            this.directionType = PDIR_CONE;
            this.directionParms[0] = this.directionParms[1] = this.directionParms[2] = this.directionParms[3] = 0.0f;
            this.speed = new idParticleParm();
            this.gravity = 0.0f;
            this.worldGravity = false;
            this.customPathType = PPATH_STANDARD;
            this.customPathParms[0] = this.customPathParms[1] = this.customPathParms[2] = this.customPathParms[3] = 0.0f;
            this.customPathParms[4] = this.customPathParms[5] = this.customPathParms[6] = this.customPathParms[7] = 0.0f;
            this.offset = new idVec3();
            this.animationFrames = 0;
            this.animationRate = 0.0f;
            this.randomDistribution = true;
            this.entityColor = false;
            this.initialAngle = 0.0f;
            this.rotationSpeed = new idParticleParm(); 
            this.orientation = POR_VIEW;
            this.orientationParms[0] = this.orientationParms[1] = this.orientationParms[2] = this.orientationParms[3] = 0.0f;
            this.size = new idParticleParm();
            this.aspect = new idParticleParm();
            this.color = new idVec4();
            this.fadeColor = new idVec4();
            this.fadeInFraction = 0.0f;
            this.fadeOutFraction = 0.0f;
            this.fadeIndexFraction = 0.0f;
            this.hidden = false;
            this.boundsExpansion = 0.0f;
            this.bounds = new idBounds();
            this.bounds.Clear();
        }
//	virtual					~idParticleStage( void ) {}
//

        /*
         ================
         idParticleStage::Default

         Sets the stage to a default state
         ================
         */
        public void Default() throws idException {
            this.material = declManager.FindMaterial("_default");
            this.totalParticles = 100;
            this.spawnBunching = 1.0f;
            this.particleLife = 1.5f;
            this.timeOffset = 0.0f;
            this.deadTime = 0.0f;
            this.distributionType = PDIST_RECT;
            this.distributionParms[0] = 8.0f;
            this.distributionParms[1] = 8.0f;
            this.distributionParms[2] = 8.0f;
            this.distributionParms[3] = 0.0f;
            this.directionType = PDIR_CONE;
            this.directionParms[0] = 90.0f;
            this.directionParms[1] = 0.0f;
            this.directionParms[2] = 0.0f;
            this.directionParms[3] = 0.0f;
            this.orientation = POR_VIEW;
            this.orientationParms[0] = 0.0f;
            this.orientationParms[1] = 0.0f;
            this.orientationParms[2] = 0.0f;
            this.orientationParms[3] = 0.0f;
            this.speed.from = 150.0f;
            this.speed.to = 150.0f;
            this.speed.table = null;
            this.gravity = 1.0f;
            this.worldGravity = false;
            this.customPathType = PPATH_STANDARD;
            this.customPathParms[0] = 0.0f;
            this.customPathParms[1] = 0.0f;
            this.customPathParms[2] = 0.0f;
            this.customPathParms[3] = 0.0f;
            this.customPathParms[4] = 0.0f;
            this.customPathParms[5] = 0.0f;
            this.customPathParms[6] = 0.0f;
            this.customPathParms[7] = 0.0f;
            this.offset.Zero();
            this.animationFrames = 0;
            this.animationRate = 0.0f;
            this.initialAngle = 0.0f;
            this.rotationSpeed.from = 0.0f;
            this.rotationSpeed.to = 0.0f;
            this.rotationSpeed.table = null;
            this.size.from = 4.0f;
            this.size.to = 4.0f;
            this.size.table = null;
            this.aspect.from = 1.0f;
            this.aspect.to = 1.0f;
            this.aspect.table = null;
            this.color.x = 1.0f;
            this.color.y = 1.0f;
            this.color.z = 1.0f;
            this.color.w = 1.0f;
            this.fadeColor.x = 0.0f;
            this.fadeColor.y = 0.0f;
            this.fadeColor.z = 0.0f;
            this.fadeColor.w = 0.0f;
            this.fadeInFraction = 0.1f;
            this.fadeOutFraction = 0.25f;
            this.fadeIndexFraction = 0.0f;
            this.boundsExpansion = 0.0f;
            this.randomDistribution = true;
            this.entityColor = false;
            this.cycleMsec = (int) ((this.particleLife + this.deadTime) * 1000);
        }

        /*
         ================
         idParticleStage::NumQuadsPerParticle

         includes trails and cross faded animations
         ================
         */
        public int NumQuadsPerParticle() {  // includes trails and cross faded animations
            int count = 1;

            if (this.orientation == POR_AIMED) {
                final int trails = idMath.Ftoi(this.orientationParms[0]);
                // each trail stage will add an extra quad
                count *= (1 + trails);
            }

            // if we are doing strip-animation, we need to double the number and cross fade them
            if (this.animationFrames > 1) {
                count *= 2;
            }

            return count;
        }

        /*
         ================
         idParticleStage::CreateParticle

         Returns 0 if no particle is created because it is completely faded out
         Returns 4 if a normal quad is created
         Returns 8 if two cross faded quads are created

         Vertex order is:

         0 1
         2 3
         ================
         */
        // returns the number of verts created, which will range from 0 to 4*NumQuadsPerParticle()
        public int CreateParticle(particleGen_t g, idDrawVert[] verts) throws idException {
            final idVec3 origin = new idVec3();

            verts[0].Clear();
            verts[1].Clear();
            verts[2].Clear();
            verts[3].Clear();

            ParticleColors(g, verts);

            // if we are completely faded out, kill the particle
            {
                ByteBuffer color = verts[0].getColor();
                if ((color.get(0) == 0) && (color.get(1) == 0) && (color.get(2) == 0) && (color.get(3) == 0)) {
                    return 0;
                }
            }

            ParticleOrigin(g, origin);

            ParticleTexCoords(g, verts);

            final int numVerts = ParticleVerts(g, origin, verts);

            if (this.animationFrames <= 1) {
                return numVerts;
            }

            // if we are doing strip-animation, we need to double the quad and cross fade it
            final float width = 1.0f / this.animationFrames;
            final float frac = g.animationFrameFrac;
            final float iFrac = 1.0f - frac;
            for (int i = 0; i < numVerts; i++) {
                verts[numVerts + i].oSet(verts[i]);

                verts[numVerts + i].st.x += width;

                muliplyElementsWith(verts[numVerts + i].getColor(), frac);

                muliplyElementsWith(verts[i].getColor(), iFrac);
            }

            return numVerts * 2;
        }

    	private static void muliplyElementsWith(ByteBuffer color, float faktor) {
    		for (int i = 0; i < 4; i++) {
    			color.put(i, (byte) (color.get(i) * faktor));
    		}
    	}

        public void ParticleOrigin(particleGen_t g, idVec3 origin) throws idException {
            if (this.customPathType == PPATH_STANDARD) {
                //
                // find intial origin distribution
                //
                float radiusSqr, angle1, angle2;

                switch (this.distributionType) {
                    case PDIST_RECT: {	// ( sizeX sizeY sizeZ )
                        origin.oSet(0, (this.randomDistribution ? g.random.CRandomFloat() : 1.0f) * this.distributionParms[0]);
                        origin.oSet(1, (this.randomDistribution ? g.random.CRandomFloat() : 1.0f) * this.distributionParms[1]);
                        origin.oSet(2, (this.randomDistribution ? g.random.CRandomFloat() : 1.0f) * this.distributionParms[2]);
                        break;
                    }
                    case PDIST_CYLINDER: {	// ( sizeX sizeY sizeZ ringFraction )
                        angle1 = ((this.randomDistribution) ? g.random.CRandomFloat() : 1.0f) * idMath.TWO_PI;

                        final float[] origin2 = new float[1];
                        final float[] origin3 = new float[1];
                        idMath.SinCos16(angle1, origin2, origin3);
                        origin.oSet(0, origin2[0]);
                        origin.oSet(1, origin3[0]);
                        origin.oSet(2, (this.randomDistribution ? g.random.CRandomFloat() : 1.0f));

                        // reproject points that are inside the ringFraction to the outer band
                        if (this.distributionParms[3] > 0.0f) {
                            radiusSqr = (origin.oGet(0) * origin.oGet(0)) + (origin.oGet(1) * origin.oGet(1));
                            if (radiusSqr < (this.distributionParms[3] * this.distributionParms[3])) {
                                // if we are inside the inner reject zone, rescale to put it out into the good zone
                                final float f = (float) (Math.sqrt(radiusSqr) / this.distributionParms[3]);
                                final float invf = 1.0f / f;
                                final float newRadius = this.distributionParms[3] + (f * (1.0f - this.distributionParms[3]));
                                final float rescale = invf * newRadius;

                                origin.oMulSet(0, rescale);
                                origin.oMulSet(1, rescale);
                            }
                        }
                        origin.oMulSet(0, this.distributionParms[0]);
                        origin.oMulSet(1, this.distributionParms[1]);
                        origin.oMulSet(2, this.distributionParms[2]);
                        break;
                    }
                    case PDIST_SPHERE: {	// ( sizeX sizeY sizeZ ringFraction )
                        // iterating with rejection is the only way to get an even distribution over a sphere
                        if (this.randomDistribution) {
                            do {
                                origin.oSet(0, g.random.CRandomFloat());
                                origin.oSet(1, g.random.CRandomFloat());
                                origin.oSet(2, g.random.CRandomFloat());
                                radiusSqr = (origin.oGet(0) * origin.oGet(0)) + (origin.oGet(1) * origin.oGet(1)) + (origin.oGet(2) * origin.oGet(2));
                            } while (radiusSqr > 1.0f);
                        } else {
                            origin.Set(1.0f, 1.0f, 1.0f);
                            radiusSqr = 3.0f;
                        }

                        if (this.distributionParms[3] > 0.0f) {
                            // we could iterate until we got something that also satisfied ringFraction,
                            // but for narrow rings that could be a lot of work, so reproject inside points instead
                            if (radiusSqr < (this.distributionParms[3] * this.distributionParms[3])) {
                                // if we are inside the inner reject zone, rescale to put it out into the good zone
                                final float f = (float) (Math.sqrt(radiusSqr) / this.distributionParms[3]);
                                final float invf = 1.0f / f;
                                final float newRadius = this.distributionParms[3] + (f * (1.0f - this.distributionParms[3]));
                                final float rescale = invf * newRadius;

                                origin.oMulSet(rescale);
                            }
                        }
                        origin.oMulSet(0, this.distributionParms[0]);
                        origin.oMulSet(1, this.distributionParms[1]);
                        origin.oMulSet(2, this.distributionParms[2]);
                        break;
                    }
                }

                // offset will effect all particle origin types
                // add this before the velocity and gravity additions
                origin.oPluSet(this.offset);

                //
                // add the velocity over time
                //
                final idVec3 dir = new idVec3();

                switch (this.directionType) {
                    case PDIR_CONE: {
                        // angle is the full angle, so 360 degrees is any spherical direction
                        angle1 = g.random.CRandomFloat() * this.directionParms[0] * idMath.M_DEG2RAD;
                        angle2 = g.random.CRandomFloat() * idMath.PI;

                        final float[] s1 = new float[1], s2 = new float[1];
                        final float[] c1 = new float[1], c2 = new float[1];
                        idMath.SinCos16(angle1, s1, c1);
                        idMath.SinCos16(angle2, s2, c2);

                        dir.oSet(0, s1[0] * c2[0]);
                        dir.oSet(1, s1[0] * s2[0]);
                        dir.oSet(2, c1[0]);
                        break;
                    }
                    case PDIR_OUTWARD: {
                        dir.oSet(origin);
                        dir.Normalize();
                        dir.oPluSet(2, this.directionParms[0]);
                        break;
                    }
                }

                // add speed
                final float iSpeed = this.speed.Integrate(g.frac, g.random);
                origin.oPluSet(dir.oMultiply(iSpeed).oMultiply(this.particleLife));

            } else {
                //
                // custom paths completely override both the origin and velocity calculations, but still
                // use the standard gravity
                //
                float angle1, angle2, speed1, speed2;
                switch (this.customPathType) {
                    case PPATH_HELIX: {		// ( sizeX sizeY sizeZ radialSpeed axialSpeed )
                        speed1 = g.random.CRandomFloat();
                        speed2 = g.random.CRandomFloat();
                        angle1 = (g.random.RandomFloat() * idMath.TWO_PI) + (this.customPathParms[3] * speed1 * g.age);

                        final float[] s1 = new float[1];
                        final float[] c1 = new float[1];
                        idMath.SinCos16(angle1, s1, c1);

                        origin.oSet(0, c1[0] * this.customPathParms[0]);
                        origin.oSet(1, s1[0] * this.customPathParms[1]);
                        origin.oSet(2, (g.random.RandomFloat() * this.customPathParms[2]) + (this.customPathParms[4] * speed2 * g.age));
                        break;
                    }
                    case PPATH_FLIES: {		// ( radialSpeed axialSpeed size )
                        speed1 = idMath.ClampFloat(0.4f, 1.0f, g.random.CRandomFloat());
//				speed2 = idMath.ClampFloat( 0.4f, 1.0f, g.random.CRandomFloat() );
                        angle1 = (g.random.RandomFloat() * idMath.PI * 2) + (this.customPathParms[0] * speed1 * g.age);
                        angle2 = (g.random.RandomFloat() * idMath.PI * 2) + (this.customPathParms[1] * speed1 * g.age);

                        final float[] s1 = new float[1], s2 = new float[1];
                        final float[] c1 = new float[1], c2 = new float[1];
                        idMath.SinCos16(angle1, s1, c1);
                        idMath.SinCos16(angle2, s2, c2);

                        origin.oSet(0, c1[0] * c2[0]);
                        origin.oSet(1, s1[0] * c2[0]);
                        origin.oSet(2, -s2[0]);
                        origin.oMultiply(this.customPathParms[2]);
                        break;
                    }
                    case PPATH_ORBIT: {		// ( radius speed axis )
                        angle1 = (g.random.RandomFloat() * idMath.TWO_PI) + (this.customPathParms[1] * g.age);

                        final float[] s1 = new float[1], c1 = new float[1];
                        idMath.SinCos16(angle1, s1, c1);

                        origin.oSet(0, c1[0] * this.customPathParms[0]);
                        origin.oSet(1, s1[0] * this.customPathParms[0]);
                        origin.ProjectSelfOntoSphere(this.customPathParms[0]);
                        break;
                    }
                    case PPATH_DRIP: {		// ( speed )
                        origin.oSet(0, 0.0f);
                        origin.oSet(1, 0.0f);
                        origin.oSet(2, -(g.age * this.customPathParms[0]));
                        break;
                    }
                    default: {
                        common.Error("idParticleStage.ParticleOrigin: bad customPathType");
                    }
                }

                origin.oPluSet(this.offset);
            }

            // adjust for the per-particle smoke offset
            origin.oMulSet(g.axis);
            origin.oPluSet(g.origin);

            // add gravity after adjusting for axis
            if (this.worldGravity) {
                final idVec3 gra = new idVec3(0, 0, -this.gravity);
                gra.oMulSet(g.renderEnt.axis.Transpose());
                origin.oPluSet(gra.oMultiply(g.age * g.age));
            } else {
                origin.oMinSet(2, this.gravity * g.age * g.age);
            }
        }

        public int ParticleVerts(particleGen_t g, final idVec3 origin, idDrawVert[] verts) throws idException {
            final float psize = this.size.Eval(g.frac, g.random);
            final float paspect = this.aspect.Eval(g.frac, g.random);

            final float width = psize;
            float height = psize * paspect;

            idVec3 left = new idVec3(), up = new idVec3();

            if (this.orientation == POR_AIMED) {
                // reset the values to an earlier time to get a previous origin
                final idRandom currentRandom = new idRandom(g.random);
                final float currentAge = g.age;
                final float currentFrac = g.frac;
//		idDrawVert []verts_p = verts[verts_p;
                int verts_p = 0;
                idVec3 stepOrigin = origin;
                idVec3 stepLeft = new idVec3();
                final int numTrails = idMath.Ftoi(this.orientationParms[0]);
                float trailTime = this.orientationParms[1];

                if (trailTime == 0) {
                    trailTime = 0.5f;
                }

                height = 1.0f / (1 + numTrails);
                float t = 0;

                for (int i = 0; i <= numTrails; i++) {
                    g.random = new idRandom(g.originalRandom);
                    g.age = currentAge - (((i + 1) * trailTime) / (numTrails + 1));	// time to back up
                    g.frac = g.age / this.particleLife;

                    final idVec3 oldOrigin = new idVec3();
                    ParticleOrigin(g, oldOrigin);

                    up = stepOrigin.oMinus(oldOrigin);	// along the direction of travel

                    final idVec3 forwardDir = new idVec3();
                    g.renderEnt.axis.ProjectVector(g.renderView.viewaxis.oGet(0), forwardDir);

                    up.oMinSet(forwardDir.oMultiply(up.oMultiply(forwardDir)));

                    up.Normalize();

                    left = up.Cross(forwardDir);
                    left.oMulSet(psize);

                    verts[verts_p + 0].oSet(verts[0]);
                    verts[verts_p + 1].oSet(verts[1]);
                    verts[verts_p + 2].oSet(verts[2]);
                    verts[verts_p + 3].oSet(verts[3]);

                    if (i == 0) {
                        verts[verts_p + 0].xyz = stepOrigin.oMinus(left);
                        verts[verts_p + 1].xyz = stepOrigin.oPlus(left);
                    } else {
                        verts[verts_p + 0].xyz = stepOrigin.oMinus(stepLeft);
                        verts[verts_p + 1].xyz = stepOrigin.oPlus(stepLeft);
                    }
                    verts[verts_p + 2].xyz = oldOrigin.oMinus(left);
                    verts[verts_p + 3].xyz = oldOrigin.oPlus(left);

                    // modify texcoords
                    verts[verts_p + 0].st.x = verts[0].st.x;
                    verts[verts_p + 0].st.y = t;

                    verts[verts_p + 1].st.x = verts[1].st.x;
                    verts[verts_p + 1].st.y = t;

                    verts[verts_p + 2].st.x = verts[2].st.x;
                    verts[verts_p + 2].st.y = t + height;

                    verts[verts_p + 3].st.x = verts[3].st.x;
                    verts[verts_p + 3].st.y = t + height;

                    t += height;

                    verts_p += 4;

                    stepOrigin = oldOrigin;
                    stepLeft = left;
                }

                g.random = new idRandom(currentRandom);
                g.age = currentAge;
                g.frac = currentFrac;

                return 4 * (numTrails + 1);
            }

            //
            // constant rotation 
            //
            float angle;

            angle = (this.initialAngle != 0) ? this.initialAngle : 360 * g.random.RandomFloat();

            final float angleMove = this.rotationSpeed.Integrate(g.frac, g.random) * this.particleLife;
            // have hald the particles rotate each way
            if ((g.index & 1) != 0) {
                angle += angleMove;
            } else {
                angle -= angleMove;
            }

            angle = (angle / 180) * idMath.PI;
            final float c = idMath.Cos16(angle);
            final float s = idMath.Sin16(angle);

            if (this.orientation == POR_Z) {
                // oriented in entity space
                left.x = s;
                left.y = c;
                left.z = 0;
                up.x = c;
                up.y = -s;
                up.z = 0;
            } else if (this.orientation == POR_X) {
                // oriented in entity space
                left.x = 0;
                left.y = c;
                left.z = s;
                up.x = 0;
                up.y = -s;
                up.z = c;
            } else if (this.orientation == POR_Y) {
                // oriented in entity space
                left.x = c;
                left.y = 0;
                left.z = s;
                up.x = -s;
                up.y = 0;
                up.z = c;
            } else {
                // oriented in viewer space
                final idVec3 entityLeft = new idVec3(), entityUp = new idVec3();

                g.renderEnt.axis.ProjectVector(g.renderView.viewaxis.oGet(1), entityLeft);
                g.renderEnt.axis.ProjectVector(g.renderView.viewaxis.oGet(2), entityUp);

                left = entityLeft.oMultiply(c).oPlus(entityUp.oMultiply(s));
                up = entityUp.oMultiply(c).oMinus(entityLeft.oMultiply(s));
            }

            left.oMulSet(width);
            up.oMulSet(height);

            verts[0].xyz = origin.oMinus(left).oPlus(up);
            verts[1].xyz = origin.oPlus(left).oPlus(up);
            verts[2].xyz = origin.oMinus(left).oMinus(up);
            verts[3].xyz = origin.oPlus(left).oMinus(up);

            return 4;
        }

        public void ParticleTexCoords(particleGen_t g, idDrawVert[] verts) {
            float s, width;
            float t, height;

            if (this.animationFrames > 1) {
                width = 1.0f / this.animationFrames;
                float floatFrame;
                if (this.animationRate != 0.0f) {
                    // explicit, cycling animation
                    floatFrame = g.age * this.animationRate;
                } else {
                    // single animation cycle over the life of the particle
                    floatFrame = g.frac * this.animationFrames;
                }
                final int intFrame = (int) floatFrame;
                g.animationFrameFrac = floatFrame - intFrame;
                s = width * intFrame;
            } else {
                s = 0.0f;
                width = 1.0f;
            }

            t = 0.0f;
            height = 1.0f;

            verts[0].st.oSet(0, s);
            verts[0].st.oSet(1, t);

            verts[1].st.oSet(0, s + width);
            verts[1].st.oSet(1, t);

            verts[2].st.oSet(0, s);
            verts[2].st.oSet(1, t + height);

            verts[3].st.oSet(0, s + width);
            verts[3].st.oSet(1, t + height);
        }

        public void ParticleColors(particleGen_t g, idDrawVert[] verts) {
            float fadeFraction = 1.0f;

            // most particles fade in at the beginning and fade out at the end
            if (g.frac < this.fadeInFraction) {
                fadeFraction *= (g.frac / this.fadeInFraction);
            }
            if ((1.0f - g.frac) < this.fadeOutFraction) {
                fadeFraction *= ((1.0f - g.frac) / this.fadeOutFraction);
            }

            // individual gun smoke particles get more and more faded as the
            // cycle goes on (note that totalParticles won't be correct for a surface-particle deform)
            if (this.fadeIndexFraction != 0.0f) {
                final float indexFrac = (this.totalParticles - g.index) / (float) this.totalParticles;
                if (indexFrac < this.fadeIndexFraction) {
                    fadeFraction *= indexFrac / this.fadeIndexFraction;
                }
            }

            byte bcolor;
            for (int i = 0; i < 4; i++) {
                final float fcolor = ((this.entityColor ? g.renderEnt.shaderParms[i] : this.color.oGet(i)) * fadeFraction) + (this.fadeColor.oGet(i) * (1.0f - fadeFraction));
                int icolor = idMath.FtoiFast(fcolor * 255.0f);
                if (icolor < 0) {
                    icolor = 0;
                } else if (icolor > 255) {
                    icolor = 255;
                }
                bcolor =  (byte) icolor;
                verts[0].getColor().put(i, bcolor); 
                verts[1].getColor().put(i, bcolor); 
                verts[2].getColor().put(i, bcolor); 
                verts[3].getColor().put(i, bcolor); 
            }
        }
//

        public String GetCustomPathName() {
            final int index = (this.customPathType.ordinal() < CustomParticleCount) ? this.customPathType.ordinal() : 0;
            return ParticleCustomDesc[index].name;
        }

        public String GetCustomPathDesc() {
            final int index = (this.customPathType.ordinal() < CustomParticleCount) ? this.customPathType.ordinal() : 0;
            return ParticleCustomDesc[index].desc;
        }

        public int NumCustomPathParms() {
            final int index = (this.customPathType.ordinal() < CustomParticleCount) ? this.customPathType.ordinal() : 0;
            return ParticleCustomDesc[index].count;
        }

        public void SetCustomPathType(final String p) {
            this.customPathType = PPATH_STANDARD;
            final prtCustomPth_t[] values = prtCustomPth_t.values();
            for (int i = 0; (i < CustomParticleCount) && (i < values.length); i++) {
                if (idStr.Icmp(p, ParticleCustomDesc[i].name) == 0) {
                    this.customPathType = /*static_cast<prtCustomPth_t>*/ values[i];
                    break;
                }
            }
        }
//public	void					operator=( const idParticleStage &src );

        public void oSet(final idParticleStage src) {

            this.material = src.material;
            this.totalParticles = src.totalParticles;
            this.cycles = src.cycles;
            this.cycleMsec = src.cycleMsec;
            this.spawnBunching = src.spawnBunching;
            this.particleLife = src.particleLife;
            this.timeOffset = src.timeOffset;
            this.deadTime = src.deadTime;
            this.distributionType = src.distributionType;
            this.distributionParms[0] = src.distributionParms[0];
            this.distributionParms[1] = src.distributionParms[1];
            this.distributionParms[2] = src.distributionParms[2];
            this.distributionParms[3] = src.distributionParms[3];
            this.directionType = src.directionType;
            this.directionParms[0] = src.directionParms[0];
            this.directionParms[1] = src.directionParms[1];
            this.directionParms[2] = src.directionParms[2];
            this.directionParms[3] = src.directionParms[3];
            this.speed = src.speed;
            this.gravity = src.gravity;
            this.worldGravity = src.worldGravity;
            this.randomDistribution = src.randomDistribution;
            this.entityColor = src.entityColor;
            this.customPathType = src.customPathType;
            this.customPathParms[0] = src.customPathParms[0];
            this.customPathParms[1] = src.customPathParms[1];
            this.customPathParms[2] = src.customPathParms[2];
            this.customPathParms[3] = src.customPathParms[3];
            this.customPathParms[4] = src.customPathParms[4];
            this.customPathParms[5] = src.customPathParms[5];
            this.customPathParms[6] = src.customPathParms[6];
            this.customPathParms[7] = src.customPathParms[7];
            this.offset = src.offset;
            this.animationFrames = src.animationFrames;
            this.animationRate = src.animationRate;
            this.initialAngle = src.initialAngle;
            this.rotationSpeed = src.rotationSpeed;
            this.orientation = src.orientation;
            this.orientationParms[0] = src.orientationParms[0];
            this.orientationParms[1] = src.orientationParms[1];
            this.orientationParms[2] = src.orientationParms[2];
            this.orientationParms[3] = src.orientationParms[3];
            this.size = src.size;
            this.aspect = src.aspect;
            this.color = src.color;
            this.fadeColor = src.fadeColor;
            this.fadeInFraction = src.fadeInFraction;
            this.fadeOutFraction = src.fadeOutFraction;
            this.fadeIndexFraction = src.fadeIndexFraction;
            this.hidden = src.hidden;
            this.boundsExpansion = src.boundsExpansion;
            this.bounds = src.bounds;
        }
    }

    //
    // group of particle stages
    //
    public static class idDeclParticle extends idDecl {

        public idList<idParticleStage> stages;
        public idBounds                bounds;
        public float                   depthHack;
        //
        //
        
        public idDeclParticle(){
            this.stages = new idList<idParticleStage>();
            this.bounds = new idBounds();
        }

        @Override
        public long Size() {
//            return sizeof( idDeclParticle );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "{\n"
                    + "\t\t" + "material\t_default\n"
                    + "\t\t" + "count\t20\n"
                    + "\t\t" + "time\t\t1.0\n"
                    + "\t" + "}\n"
                    + "}";
        }

        @Override
        public boolean Parse(String text, int textLength) throws idException {
            final idLexer src = new idLexer();
            final idToken token = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            this.depthHack = 0.0f;

            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (0 == token.Icmp("}")) {
                    break;
                }

                if (0 == token.Icmp("{")) {
                    final idParticleStage stage = ParseParticleStage(src);
                    if (null == stage) {
                        src.Warning("Particle stage parse failed");
                        MakeDefault();
                        return false;
                    }
                    this.stages.Append(stage);
                    continue;
                }

                if (0 == token.Icmp("depthHack")) {
                    this.depthHack = src.ParseFloat();
                    continue;
                }

                src.Warning("bad token %s", token.getData());
                MakeDefault();
                return false;
            }

            //
            // calculate the bounds
            //
            this.bounds.Clear();
            for (int i = 0; i < this.stages.Num(); i++) {
                GetStageBounds(this.stages.oGet(i));
                this.bounds.AddBounds(this.stages.oGet(i).bounds);
            }

            if (this.bounds.GetVolume() <= 0.1f) {
                this.bounds = new idBounds(Vector.getVec3_origin()).Expand(8.0f);
            }

            return true;
        }

        @Override
        public void FreeData() {
            this.stages.DeleteContents(true);
        }

        public boolean Save() throws idException {
            return Save(null);
        }

        public boolean Save(final String fileName) throws idException {
            RebuildTextSource();
            if (fileName != null) {
                declManager.CreateNewDecl(DECL_PARTICLE, GetName(), fileName);
            }
            ReplaceSourceFileText();
            return true;
        }

        private boolean RebuildTextSource() {
            final idFile_Memory f = new idFile_Memory();

            f.WriteFloatString("\n\n/*\n"
                    + "\tGenerated by the Particle Editor.\n"
                    + "\tTo use the particle editor, launch the game and type 'editParticles' on the console.\n"
                    + "*/\n");

            f.WriteFloatString("particle %s {\n", GetName());

            if (this.depthHack != 0.0f) {
                f.WriteFloatString("\tdepthHack\t%f\n", this.depthHack);
            }

            for (int i = 0; i < this.stages.Num(); i++) {
                WriteStage(f, this.stages.oGet(i));
            }

            f.WriteFloatString("}");

            SetText(new String(f.GetDataPtr().array()));

            return true;
        }

        private void GetStageBounds(idParticleStage stage) throws idException {

            stage.bounds.Clear();

            // this isn't absolutely guaranteed, but it should be close
            final particleGen_t g = new particleGen_t();

            final renderEntity_s renderEntity = new renderEntity_s();//memset( &renderEntity, 0, sizeof( renderEntity ) );
            renderEntity.axis.oSet(getMat3_identity());

            final renderView_s renderView = new renderView_s();//memset( &renderView, 0, sizeof( renderView ) );
            renderView.viewaxis.oSet(getMat3_identity());

            g.renderEnt = renderEntity;
            g.renderView = renderView;
            g.origin.oSet(new idVec3());
            g.axis.oSet(getMat3_identity());

            final idRandom steppingRandom = new idRandom();
            steppingRandom.SetSeed(0);

            // just step through a lot of possible particles as a representative sampling
            for (int i = 0; i < 1000; i++) {
                g.random = new idRandom(g.originalRandom = new idRandom(steppingRandom));

                final int maxMsec = (int) (stage.particleLife * 1000);
                for (int inCycleTime = 0; inCycleTime < maxMsec; inCycleTime += 16) {

                    // make sure we get the very last tic, which may make up an extreme edge
                    if ((inCycleTime + 16) > maxMsec) {
                        inCycleTime = maxMsec - 1;
                    }

                    g.frac = inCycleTime / (stage.particleLife * 1000);
                    g.age = inCycleTime * 0.001f;

                    // if the particle doesn't get drawn because it is faded out or beyond a kill region,
                    // don't increment the verts
                    final idVec3 origin = new idVec3();
                    stage.ParticleOrigin(g, origin);
                    stage.bounds.AddPoint(origin);
                }
            }

            // find the max size
            float maxSize = 0;

            for (float f = 0; f <= 1.0f; f += 1.0f / 64) {
                float size = stage.size.Eval(f, steppingRandom);
                final float aspect = stage.aspect.Eval(f, steppingRandom);
                if (aspect > 1) {
                    size *= aspect;
                }
                if (size > maxSize) {
                    maxSize = size;
                }
            }

            maxSize += 8;	// just for good measure
            // users can specify a per-stage bounds expansion to handle odd cases
            stage.bounds.ExpandSelf(maxSize + stage.boundsExpansion);
        }

        private idParticleStage ParseParticleStage(idLexer src) throws idException {
            final idToken token = new idToken();

            final idParticleStage stage = new idParticleStage();
            stage.Default();

            while (true) {
                if (src.HadError()) {
                    break;
                }
                if (!src.ReadToken(token)) {
                    break;
                }
                if (0 == token.Icmp("}")) {
                    break;
                }
                if (0 == token.Icmp("material")) {
                    src.ReadToken(token);
                    stage.material = declManager.FindMaterial(token);
                    continue;
                }
                if (0 == token.Icmp("count")) {
                    stage.totalParticles = src.ParseInt();
                    continue;
                }
                if (0 == token.Icmp("time")) {
                    stage.particleLife = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("cycles")) {
                    stage.cycles = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("timeOffset")) {
                    stage.timeOffset = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("deadTime")) {
                    stage.deadTime = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("randomDistribution")) {
                    stage.randomDistribution = src.ParseBool();
                    continue;
                }
                if (0 == token.Icmp("bunching")) {
                    stage.spawnBunching = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("distribution")) {
                    src.ReadToken(token);
                    if (0 == token.Icmp("rect")) {
                        stage.distributionType = PDIST_RECT;
                    } else if (0 == token.Icmp("cylinder")) {
                        stage.distributionType = PDIST_CYLINDER;
                    } else if (0 == token.Icmp("sphere")) {
                        stage.distributionType = PDIST_SPHERE;
                    } else {
                        src.Error("bad distribution type: %s\n", token.getData());
                    }
                    ParseParms(src, stage.distributionParms, stage.distributionParms.length);
                    continue;
                }

                if (0 == token.Icmp("direction")) {
                    src.ReadToken(token);
                    if (0 == token.Icmp("cone")) {
                        stage.directionType = PDIR_CONE;
                    } else if (0 == token.Icmp("outward")) {
                        stage.directionType = PDIR_OUTWARD;
                    } else {
                        src.Error("bad direction type: %s\n", token.getData());
                    }
                    ParseParms(src, stage.directionParms, stage.directionParms.length);
                    continue;
                }

                if (0 == token.Icmp("orientation")) {
                    src.ReadToken(token);
                    if (0 == token.Icmp("view")) {
                        stage.orientation = POR_VIEW;
                    } else if (0 == token.Icmp("aimed")) {
                        stage.orientation = POR_AIMED;
                    } else if (0 == token.Icmp("x")) {
                        stage.orientation = POR_X;
                    } else if (0 == token.Icmp("y")) {
                        stage.orientation = POR_Y;
                    } else if (0 == token.Icmp("z")) {
                        stage.orientation = POR_Z;
                    } else {
                        src.Error("bad orientation type: %s\n", token.getData());
                    }
                    ParseParms(src, stage.orientationParms, stage.orientationParms.length);
                    continue;
                }

                if (0 == token.Icmp("customPath")) {
                    src.ReadToken(token);
                    if (0 == token.Icmp("standard")) {
                        stage.customPathType = PPATH_STANDARD;
                    } else if (0 == token.Icmp("helix")) {
                        stage.customPathType = PPATH_HELIX;
                    } else if (0 == token.Icmp("flies")) {
                        stage.customPathType = PPATH_FLIES;
                    } else if (0 == token.Icmp("spherical")) {
                        stage.customPathType = PPATH_ORBIT;
                    } else {
                        src.Error("bad path type: %s\n", token.getData());
                    }
                    ParseParms(src, stage.customPathParms, stage.customPathParms.length);
                    continue;
                }

                if (0 == token.Icmp("speed")) {
                    ParseParametric(src, stage.speed);
                    continue;
                }
                if (0 == token.Icmp("rotation")) {
                    ParseParametric(src, stage.rotationSpeed);
                    continue;
                }
                if (0 == token.Icmp("angle")) {
                    stage.initialAngle = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("entityColor")) {
                    stage.entityColor = src.ParseBool();
                    continue;
                }
                if (0 == token.Icmp("size")) {
                    ParseParametric(src, stage.size);
                    continue;
                }
                if (0 == token.Icmp("aspect")) {
                    ParseParametric(src, stage.aspect);
                    continue;
                }
                if (0 == token.Icmp("fadeIn")) {
                    stage.fadeInFraction = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("fadeOut")) {
                    stage.fadeOutFraction = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("fadeIndex")) {
                    stage.fadeIndexFraction = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("color")) {
                    stage.color.oSet(0, src.ParseFloat());
                    stage.color.oSet(1, src.ParseFloat());
                    stage.color.oSet(2, src.ParseFloat());
                    stage.color.oSet(3, src.ParseFloat());
                    continue;
                }
                if (0 == token.Icmp("fadeColor")) {
                    stage.fadeColor.oSet(0, src.ParseFloat());
                    stage.fadeColor.oSet(1, src.ParseFloat());
                    stage.fadeColor.oSet(2, src.ParseFloat());
                    stage.fadeColor.oSet(3, src.ParseFloat());
                    continue;
                }
                if (0 == token.Icmp("offset")) {
                    stage.offset.oSet(0, src.ParseFloat());
                    stage.offset.oSet(1, src.ParseFloat());
                    stage.offset.oSet(2, src.ParseFloat());
                    continue;
                }
                if (0 == token.Icmp("animationFrames")) {
                    stage.animationFrames = src.ParseInt();
                    continue;
                }
                if (0 == token.Icmp("animationRate")) {
                    stage.animationRate = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("boundsExpansion")) {
                    stage.boundsExpansion = src.ParseFloat();
                    continue;
                }
                if (0 == token.Icmp("gravity")) {
                    src.ReadToken(token);
                    if (0 == token.Icmp("world")) {
                        stage.worldGravity = true;
                    } else {
                        src.UnreadToken(token);
                    }
                    stage.gravity = src.ParseFloat();
                    continue;
                }

                src.Error("unknown token %s\n", token.getData());
            }

            // derive values
            stage.cycleMsec = (int) ((stage.particleLife + stage.deadTime) * 1000);

            return stage;
        }

        /*
         ================
         idDeclParticle::ParseParms

         Parses a variable length list of parms on one line
         ================
         */
        private void ParseParms(idLexer src, float[] parms, int maxParms) throws idException {
            final idToken token = new idToken();

            Arrays.fill(parms, 0, maxParms, 0);//memset( parms, 0, maxParms * sizeof( *parms ) );
            int count = 0;
            while (true) {
                if (!src.ReadTokenOnLine(token)) {
                    return;
                }
                if (count == maxParms) {
                    src.Error("too many parms on line");
                    return;
                }
                token.StripQuotes();
                parms[count] = atof(token.getData());
                count++;
            }
        }

        private void ParseParametric(idLexer src, idParticleParm parm) throws idException {
            final idToken token = new idToken();

            parm.table = null;
            parm.from = parm.to = 0.0f;

            if (!src.ReadToken(token)) {
                src.Error("not enough parameters");
                return;
            }

            if (token.IsNumeric()) {
                // can have a to + 2nd parm
                parm.from = parm.to = atof(token.getData());
                if (src.ReadToken(token)) {
                    if (0 == token.Icmp("to")) {
                        if (!src.ReadToken(token)) {
                            src.Error("missing second parameter");
                            return;
                        }
                        parm.to = atof(token.getData());
                    } else {
                        src.UnreadToken(token);
                    }
                }
            } else {
                // table
                parm.table =/*static_cast<const idDeclTable *>*/ (idDeclTable) (declManager.FindType(DECL_TABLE, token, false));
            }

        }

        private void WriteStage(idFile f, idParticleStage stage) {

            int i;

            f.WriteFloatString("\t{\n");
            f.WriteFloatString("\t\tcount\t\t\t\t%d\n", stage.totalParticles);
            f.WriteFloatString("\t\tmaterial\t\t\t%s\n", stage.material.GetName());
            if (stage.animationFrames != 0) {
                f.WriteFloatString("\t\tanimationFrames \t%d\n", stage.animationFrames);
            }
            if (stage.animationRate != 0) {
                f.WriteFloatString("\t\tanimationRate \t\t%.3f\n", stage.animationRate);
            }
            f.WriteFloatString("\t\ttime\t\t\t\t%.3f\n", stage.particleLife);
            f.WriteFloatString("\t\tcycles\t\t\t\t%.3f\n", stage.cycles);
            if (stage.timeOffset != 0) {
                f.WriteFloatString("\t\ttimeOffset\t\t\t%.3f\n", stage.timeOffset);
            }
            if (stage.deadTime != 0) {
                f.WriteFloatString("\t\tdeadTime\t\t\t%.3f\n", stage.deadTime);
            }
            f.WriteFloatString("\t\tbunching\t\t\t%.3f\n", stage.spawnBunching);

            f.WriteFloatString("\t\tdistribution\t\t%s ", ParticleDistributionDesc[stage.distributionType.ordinal()].name);
            for (i = 0; i < ParticleDistributionDesc[stage.distributionType.ordinal()].count; i++) {
                f.WriteFloatString("%.3f ", stage.distributionParms[i]);
            }
            f.WriteFloatString("\n");

            f.WriteFloatString("\t\tdirection\t\t\t%s ", ParticleDirectionDesc[stage.directionType.ordinal()].name);
            for (i = 0; i < ParticleDirectionDesc[stage.directionType.ordinal()].count; i++) {
                f.WriteFloatString("\"%.3f\" ", stage.directionParms[i]);
            }
            f.WriteFloatString("\n");

            f.WriteFloatString("\t\torientation\t\t\t%s ", ParticleOrientationDesc[stage.orientation.ordinal()].name);
            for (i = 0; i < ParticleOrientationDesc[stage.orientation.ordinal()].count; i++) {
                f.WriteFloatString("%.3f ", stage.orientationParms[i]);
            }
            f.WriteFloatString("\n");

            if (stage.customPathType != PPATH_STANDARD) {
                f.WriteFloatString("\t\tcustomPath %s ", ParticleCustomDesc[stage.customPathType.ordinal()].name);
                for (i = 0; i < ParticleCustomDesc[stage.customPathType.ordinal()].count; i++) {
                    f.WriteFloatString("%.3f ", stage.customPathParms[i]);
                }
                f.WriteFloatString("\n");
            }

            if (stage.entityColor) {
                f.WriteFloatString("\t\tentityColor\t\t\t1\n");
            }

            WriteParticleParm(f, stage.speed, "speed");
            WriteParticleParm(f, stage.size, "size");
            WriteParticleParm(f, stage.aspect, "aspect");

            if (stage.rotationSpeed.from != 0) {
                WriteParticleParm(f, stage.rotationSpeed, "rotation");
            }

            if (stage.initialAngle != 0) {
                f.WriteFloatString("\t\tangle\t\t\t\t%.3f\n", stage.initialAngle);
            }

            f.WriteFloatString("\t\trandomDistribution\t\t\t\t%d\n", stage.randomDistribution ? 1 : 0);
            f.WriteFloatString("\t\tboundsExpansion\t\t\t\t%.3f\n", stage.boundsExpansion);

            f.WriteFloatString("\t\tfadeIn\t\t\t\t%.3f\n", stage.fadeInFraction);
            f.WriteFloatString("\t\tfadeOut\t\t\t\t%.3f\n", stage.fadeOutFraction);
            f.WriteFloatString("\t\tfadeIndex\t\t\t\t%.3f\n", stage.fadeIndexFraction);

            f.WriteFloatString("\t\tcolor \t\t\t\t%.3f %.3f %.3f %.3f\n", stage.color.x, stage.color.y, stage.color.z, stage.color.w);
            f.WriteFloatString("\t\tfadeColor \t\t\t%.3f %.3f %.3f %.3f\n", stage.fadeColor.x, stage.fadeColor.y, stage.fadeColor.z, stage.fadeColor.w);

            f.WriteFloatString("\t\toffset \t\t\t\t%.3f %.3f %.3f\n", stage.offset.x, stage.offset.y, stage.offset.z);
            f.WriteFloatString("\t\tgravity \t\t\t");
            if (stage.worldGravity) {
                f.WriteFloatString("world ");
            }
            f.WriteFloatString("%.3f\n", stage.gravity);
            f.WriteFloatString("\t}\n");
        }

        private void WriteParticleParm(idFile f, idParticleParm parm, final String name) {

            f.WriteFloatString("\t\t%s\t\t\t\t ", name);
            if (parm.table != null) {
                f.WriteFloatString("%s\n", parm.table.GetName());
            } else {
                f.WriteFloatString("\"%.3f\" ", parm.from);
                if (parm.from == parm.to) {
                    f.WriteFloatString("\n");
                } else {
                    f.WriteFloatString(" to \"%.3f\"\n", parm.to);
                }
            }
        }

        public void oSet(idDeclParticle idDeclParticle) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
