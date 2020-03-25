package neo.idlib.geometry;

import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;

import java.util.Arrays;
import java.util.stream.Stream;

import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Pluecker.idPluecker;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Surface {

//    @Deprecated
    private static int UpdateVertexIndex(int vertexIndexNum[], int[] vertexRemap, int[] vertexCopyIndex, int vertNum) {
        final int s = INTSIGNBITSET(vertexRemap[vertNum]);
        vertexIndexNum[0] = vertexRemap[vertNum];
        vertexRemap[vertNum] = vertexIndexNum[s];
        vertexIndexNum[1] += s;
        vertexCopyIndex[vertexRemap[vertNum]] = vertNum;
        return vertexRemap[vertNum];
    }

//    private static int UpdateVertexIndex(int vertexIndexNum[], int[] vertexRemap, final int rIndex, int[] vertexCopyIndex, final int cIndex, int vertNum) {
//        int s = INTSIGNBITSET(vertexRemap[rIndex + vertNum]);
//        vertexIndexNum[0] = vertexRemap[rIndex + vertNum];
//        vertexRemap[rIndex + vertNum] = vertexIndexNum[s];
//        vertexIndexNum[1] += s;
//        vertexCopyIndex[cIndex + vertexRemap[rIndex + vertNum]] = vertNum;
//        return vertexRemap[rIndex + vertNum];
//    }

    /*
     ===============================================================================

     Surface base class.

     A surface is tesselated to a triangle mesh with each edge shared by
     at most two triangles.

     ===============================================================================
     */
    static class surfaceEdge_t {

        int[] verts = new int[2];	// edge vertices always with ( verts[0] < verts[1] )
        int[] tris = new int[2];	// edge triangles

        private static surfaceEdge_t[] generateArray(final int length) {
            return Stream.generate(surfaceEdge_t::new).limit(length).toArray(surfaceEdge_t[]::new);
        }
    }

    public static class idSurface {
        protected idList<idDrawVert>    verts;          // vertices
        protected idList<Integer>       indexes;        // 3 references to vertices for each triangle
        protected idList<surfaceEdge_t> edges;          // edges
        protected idList<Integer>       edgeIndexes;    // 3 references to edges for each triangle, may be negative for reversed edge
        //
        //

        public idSurface() {
            this.verts = new idList<>();
            this.indexes = new idList<>();
            this.edges = new idList<>();
            this.edgeIndexes = new idList<>();
        }

        public idSurface(final idSurface surf) {
            this.verts = surf.verts;
            this.indexes = surf.indexes;
            this.edges = surf.edges;
            this.edgeIndexes = surf.edgeIndexes;
        }

        public idSurface(final idDrawVert[] verts, final int numVerts, final int[] indexes, final int numIndexes) {
            assert ((verts != null) && (indexes != null) && (numVerts > 0) && (numIndexes > 0));
            this.verts.SetNum(numVerts);
//	memcpy( this.verts.Ptr(), verts, numVerts * sizeof( verts[0] ) );
            System.arraycopy(verts, 0, this.verts.Ptr(), 0, numVerts);
            this.indexes.SetNum(numIndexes);
//	memcpy( this.indexes.Ptr(), indexes, numIndexes * sizeof( indexes[0] ) );
            System.arraycopy(indexes, 0, this.indexes.Ptr(), 0, numIndexes);
            GenerateEdgeIndexes();
        }
//public							~idSurface( void );
//
//public	const idDrawVert &		operator[]( const int index ) const;

        public idDrawVert oGet(final int index) {
            return this.verts.oGet(index);
        }

        public idSurface oPluSet(final idSurface surf) {
            int i, m, n;
            n = this.verts.Num();
            m = this.indexes.Num();
            this.verts.Append(surf.verts);            // merge verts where possible ?
            this.indexes.Append(surf.indexes);
            for (i = m; i < this.indexes.Num(); i++) {
                this.indexes.oPluSet(i, n);
            }
            GenerateEdgeIndexes();
            return this;
        }

        public int GetNumIndexes() {
            return this.indexes.Num();
        }

        public Integer[] GetIndexes() {
            return this.indexes.Ptr();
        }
//public	int						GetNumVertices( void ) const { return verts.Num(); }
//public	const idDrawVert *		GetVertices( void ) const { return verts.Ptr(); }
//public	const int *				GetEdgeIndexes( void ) const { return edgeIndexes.Ptr(); }
//public	const surfaceEdge_t *	GetEdges( void ) const { return edges.Ptr(); }
//

        public void Clear() {
            this.verts.Clear();
            this.indexes.Clear();
            this.edges.Clear();
            this.edgeIndexes.Clear();
        }

        public void SwapTriangles(idSurface surf) {
            this.verts.Swap(surf.verts);
            this.indexes.Swap(surf.indexes);
            this.edges.Swap(surf.edges);
            this.edgeIndexes.Swap(surf.edgeIndexes);
        }

        public void TranslateSelf(final idVec3 translation) {
            for (int i = 0; i < this.verts.Num(); i++) {
                this.verts.oGet(i).xyz.oPluSet(translation);
            }
        }

        public void RotateSelf(final idMat3 rotation) {
            for (int i = 0; i < this.verts.Num(); i++) {
                this.verts.oGet(i).xyz.oMulSet(rotation);
                this.verts.oGet(i).normal.oMulSet(rotation);
                this.verts.oGet(i).tangents[0].oMulSet(rotation);
                this.verts.oGet(i).tangents[1].oMulSet(rotation);
            }
        }
//
        // splits the surface into a front and back surface, the surface itself stays unchanged
        // frontOnPlaneEdges and backOnPlaneEdges optionally store the indexes to the edges that lay on the split plane
        // returns a SIDE_?

        public int Split(final idPlane plane, final float epsilon, idSurface[][] front, idSurface[][] back) {
            return Split(plane, epsilon, front, back, null, null);
        }

        public int Split(final idPlane plane, final float epsilon, idSurface[][] front, idSurface[][] back, int[] frontOnPlaneEdges) {
            return Split(plane, epsilon, front, back, frontOnPlaneEdges, null);
        }

        public int Split(final idPlane plane, final float epsilon, idSurface[][] front, idSurface[][] back, int[] frontOnPlaneEdges, int[] backOnPlaneEdges) {
            float[] dists;
            float f;
            byte[] sides;
            final int[] counts = new int[3];
            int[] edgeSplitVertex;
            int numEdgeSplitVertexes;
            final int[][] vertexRemap = new int[2][];
            final int[][] vertexIndexNum = new int[2][2];
            final int[][] vertexCopyIndex = new int[2][];
            final Integer[][] indexPtr = new Integer[2][];
            final int[] indexNum = new int[2];
            Integer[] index;
            final int[][] onPlaneEdges = new int[2][];
            final int[] numOnPlaneEdges = new int[2];
            int maxOnPlaneEdges;
            int i;
            final idSurface[] surface = new idSurface[2];
            final idDrawVert v = new idDrawVert();

            dists = new float[this.verts.Num()];
            sides = new byte[this.verts.Num()];

            counts[0] = counts[1] = counts[2] = 0;

            // determine side for each vertex
            for (i = 0; i < this.verts.Num(); i++) {
                dists[i] = f = plane.Distance(this.verts.oGet(i).xyz);
                if (f > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (f < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }

            front[0] = back[0] = null;

            // if coplanar, put on the front side if the normals match
            if ((0 == counts[SIDE_FRONT]) && (0 == counts[SIDE_BACK])) {

                f = this.verts.oGet(this.indexes.oGet(1)).xyz.oMinus(this.verts.oGet(this.indexes.oGet(0)).xyz).Cross(
                        this.verts.oGet(this.indexes.oGet(0)).xyz.oMinus(this.verts.oGet(this.indexes.oGet(2)).xyz)).oMultiply(plane.Normal());
                if (FLOATSIGNBITSET(f) != 0) {
                    back[0][0] = new idSurface(this);//TODO:check deref
                    return SIDE_BACK;
                } else {
                    front[0][0] = new idSurface(this);
                    return SIDE_FRONT;
                }
            }
            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
                back[0][0] = new idSurface(this);
                return SIDE_BACK;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                front[0][0] = new idSurface(this);
                return SIDE_FRONT;
            }

            // allocate front and back surface
            front[0][0] = surface[0] = new idSurface();
            back[0][0] = surface[1] = new idSurface();

            edgeSplitVertex = new int[this.edges.Num()];
            numEdgeSplitVertexes = 0;

            maxOnPlaneEdges = 4 * counts[SIDE_ON];
            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            // split edges
            for (i = 0; i < this.edges.Num(); i++) {
                final int v0 = this.edges.oGet(i).verts[0];
                final int v1 = this.edges.oGet(i).verts[1];
                final int sidesOr = (sides[v0] | sides[v1]);

                // if both vertexes are on the same side or one is on the clipping plane
                if (((sides[v0] ^ sides[v1]) == 0) || ((sidesOr & SIDE_ON) == 0)) {
                    edgeSplitVertex[i] = -1;
                    counts[sidesOr & SIDE_BACK]++;
                    counts[SIDE_ON] += (sidesOr & SIDE_ON) >> 1;
                } else {
                    f = dists[v0] / (dists[v0] - dists[v1]);
                    v.LerpAll(this.verts.oGet(v0), this.verts.oGet(v1), f);
                    edgeSplitVertex[i] = numEdgeSplitVertexes++;
                    surface[0].verts.Append(v);
                    surface[1].verts.Append(v);
                }
            }

            // each edge is shared by at most two triangles, as such there can never be more indexes than twice the number of edges
            surface[0].indexes.Resize(((counts[SIDE_FRONT] + counts[SIDE_ON]) * 2) + (numEdgeSplitVertexes * 4));
            surface[1].indexes.Resize(((counts[SIDE_BACK] + counts[SIDE_ON]) * 2) + (numEdgeSplitVertexes * 4));

            // allocate indexes to construct the triangle indexes for the front and back surface
            vertexRemap[0] = new int[this.verts.Num()];
//	memset( vertexRemap[0], -1, verts.Num() * sizeof( int ) );
            Arrays.fill(vertexRemap[0], -1, 0, this.verts.Num());
            vertexRemap[1] = new int[this.verts.Num()];
//	memset( vertexRemap[1], -1, verts.Num() * sizeof( int ) );
            Arrays.fill(vertexRemap[0], -1, 0, this.verts.Num());

            vertexCopyIndex[0] = new int[(numEdgeSplitVertexes + this.verts.Num())];
            vertexCopyIndex[1] = new int[(numEdgeSplitVertexes + this.verts.Num())];

            vertexIndexNum[0][0] = vertexIndexNum[1][0] = 0;
            vertexIndexNum[0][1] = vertexIndexNum[1][1] = numEdgeSplitVertexes;

            indexPtr[0] = surface[0].indexes.Ptr();
            indexPtr[1] = surface[1].indexes.Ptr();
            indexNum[0] = surface[0].indexes.Num();
            indexNum[1] = surface[1].indexes.Num();

            maxOnPlaneEdges += 4 * numEdgeSplitVertexes;
            // allocate one more in case no triangles are actually split which may happen for a disconnected surface
            onPlaneEdges[0] = new int[(maxOnPlaneEdges + 1)];
            onPlaneEdges[1] = new int[(maxOnPlaneEdges + 1)];
            numOnPlaneEdges[0] = numOnPlaneEdges[1] = 0;

            // split surface triangles
            for (i = 0; i < this.edgeIndexes.Num(); i += 3) {
                int e0, e1, e2, v0, v1, v2, s, n;

                e0 = Math.abs(this.edgeIndexes.oGet(i + 0));
                e1 = Math.abs(this.edgeIndexes.oGet(i + 1));
                e2 = Math.abs(this.edgeIndexes.oGet(i + 2));

                v0 = this.indexes.oGet(i + 0);
                v1 = this.indexes.oGet(i + 1);
                v2 = this.indexes.oGet(i + 2);

                switch ((INTSIGNBITSET(edgeSplitVertex[e0]) | (INTSIGNBITSET(edgeSplitVertex[e1]) << 1) | (INTSIGNBITSET(edgeSplitVertex[e2]) << 2)) ^ 7) {
                    case 0: {	// no edges split
                        if (((sides[v0] & sides[v1] & sides[v2]) & SIDE_ON) != 0) {
                            // coplanar
                            f = this.verts.oGet(v1).xyz.oMinus(this.verts.oGet(v0).xyz).Cross(this.verts.oGet(v0).xyz.oMinus(this.verts.oGet(v2).xyz)).oMultiply(plane.Normal());
                            s = FLOATSIGNBITSET(f);
                        } else {
                            s = (sides[v0] | sides[v1] | sides[v2]) & SIDE_BACK;
                        }
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]] = n;
                        numOnPlaneEdges[s] += (sides[v0] & sides[v1]) >> 1;
                        onPlaneEdges[s][numOnPlaneEdges[s]] = n + 1;
                        numOnPlaneEdges[s] += (sides[v1] & sides[v2]) >> 1;
                        onPlaneEdges[s][numOnPlaneEdges[s]] = n + 2;
                        numOnPlaneEdges[s] += (sides[v2] & sides[v0]) >> 1;
                        index = indexPtr[s];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        indexNum[s] = n;
                        break;
                    }
                    case 1: {	// first edge split
                        s = sides[v0] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        indexNum[s] = n;
                        break;
                    }
                    case 2: {	// second edge split
                        s = sides[v1] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        indexNum[s] = n;
                        break;
                    }
                    case 3: {	// first and second edge split
                        s = sides[v1] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        indexNum[s] = n;
                        break;
                    }
                    case 4: {	// third edge split
                        s = sides[v2] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        indexNum[s] = n;
                        break;
                    }
                    case 5: {	// first and third edge split
                        s = sides[v0] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = edgeSplitVertex[e0];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        index[n++] = edgeSplitVertex[e2];
                        indexNum[s] = n;
                        break;
                    }
                    case 6: {	// second and third edge split
                        s = sides[v2] & SIDE_BACK;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v2);
                        indexNum[s] = n;
                        s ^= 1;
                        n = indexNum[s];
                        onPlaneEdges[s][numOnPlaneEdges[s]++] = n;
                        index = indexPtr[s];
                        index[n++] = edgeSplitVertex[e1];
                        index[n++] = edgeSplitVertex[e2];
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v0);
                        index[n++] = UpdateVertexIndex(vertexIndexNum[s], vertexRemap[s], vertexCopyIndex[s], v1);
                        index[n++] = edgeSplitVertex[e2];
                        indexNum[s] = n;
                        break;
                    }
                }
            }

            surface[0].indexes.SetNum(indexNum[0], false);
            surface[1].indexes.SetNum(indexNum[1], false);

            // copy vertexes
            surface[0].verts.SetNum(vertexIndexNum[0][1], false);
//            index = vertexCopyIndex[0];
            for (i = numEdgeSplitVertexes; i < surface[0].verts.Num(); i++) {
                surface[0].verts.oSet(i, this.verts.oGet(vertexCopyIndex[0][i]));
            }
            surface[1].verts.SetNum(vertexIndexNum[1][1], false);
//            index = vertexCopyIndex[1];
            for (i = numEdgeSplitVertexes; i < surface[1].verts.Num(); i++) {
                surface[1].verts.oSet(i, this.verts.oGet(vertexCopyIndex[1][i]));
            }

            // generate edge indexes
            surface[0].GenerateEdgeIndexes();
            surface[1].GenerateEdgeIndexes();

            if (null != frontOnPlaneEdges) {
//		memcpy( frontOnPlaneEdges, onPlaneEdges[0], numOnPlaneEdges[0] * sizeof( int ) );
                System.arraycopy(onPlaneEdges[0], 0, frontOnPlaneEdges, 0, numOnPlaneEdges[0]);
                frontOnPlaneEdges[numOnPlaneEdges[0]] = -1;
            }

            if (null != backOnPlaneEdges) {
//		memcpy( backOnPlaneEdges, onPlaneEdges[1], numOnPlaneEdges[1] * sizeof( int ) );
                System.arraycopy(onPlaneEdges[1], 0, backOnPlaneEdges, 0, numOnPlaneEdges[1]);
                backOnPlaneEdges[numOnPlaneEdges[1]] = -1;
            }

            return SIDE_CROSS;
        }

        public boolean ClipInPlace(final idPlane plane) {
            return ClipInPlace(plane, Plane.ON_EPSILON, false);
        }

        public boolean ClipInPlace(final idPlane plane, final float epsilon) {
            return ClipInPlace(plane, epsilon, false);
        }

        // cuts off the part at the back side of the plane, returns true if some part was at the front
        // if there is nothing at the front the number of points is set to zero
        public boolean ClipInPlace(final idPlane plane, final float epsilon, final boolean keepOn) {
            float[] dists;
            float f;
            byte[] sides;
            final int[] counts = new int[3];
            int i;
            int[] edgeSplitVertex;
            int[] vertexRemap;
            final int[] vertexIndexNum = new int[2];
            int[] vertexCopyIndex;
            Integer[] indexPtr;
            int indexNum;
            int numEdgeSplitVertexes;
            final idDrawVert v = new idDrawVert();
            final idList<idDrawVert> newVerts = new idList<>();
            final idList<Integer> newIndexes = new idList<>();

            dists = new float[this.verts.Num()];
            sides = new byte[this.verts.Num()];

            counts[0] = counts[1] = counts[2] = 0;

            // determine side for each vertex
            for (i = 0; i < this.verts.Num(); i++) {
                dists[i] = f = plane.Distance(this.verts.oGet(i).xyz);
                if (f > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (f < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }

            // if coplanar, put on the front side if the normals match
            if ((0 == counts[SIDE_FRONT]) && (0 == counts[SIDE_BACK])) {

                f = this.verts.oGet(this.indexes.oGet(1)).xyz.oMinus(this.verts.oGet(this.indexes.oGet(0)).xyz).Cross(this.verts.oGet(this.indexes.oGet(0)).xyz.oMinus(this.verts.oGet(this.indexes.oGet(2)).xyz)).oMultiply(plane.Normal());
                if (FLOATSIGNBITSET(f) != 0) {
                    Clear();
                    return false;
                } else {
                    return true;
                }
            }
            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
                Clear();
                return false;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                return true;
            }

            edgeSplitVertex = new int[this.edges.Num()];
            numEdgeSplitVertexes = 0;

            counts[SIDE_FRONT] = counts[SIDE_BACK] = 0;

            // split edges
            for (i = 0; i < this.edges.Num(); i++) {
                final int v0 = this.edges.oGet(i).verts[0];
                final int v1 = this.edges.oGet(i).verts[1];

                // if both vertexes are on the same side or one is on the clipping plane
                if (((sides[v0] ^ sides[v1]) == 0) || (((sides[v0] | sides[v1]) & SIDE_ON) != 0)) {
                    edgeSplitVertex[i] = -1;
                    counts[(sides[v0] | sides[v1]) & SIDE_BACK]++;
                } else {
                    f = dists[v0] / (dists[v0] - dists[v1]);
                    v.LerpAll(this.verts.oGet(v0), this.verts.oGet(v1), f);
                    edgeSplitVertex[i] = numEdgeSplitVertexes++;
                    newVerts.Append(v);
                }
            }

            // each edge is shared by at most two triangles, as such there can never be
            // more indexes than twice the number of edges
            newIndexes.Resize((counts[SIDE_FRONT] << 1) + (numEdgeSplitVertexes << 2));

            // allocate indexes to construct the triangle indexes for the front and back surface
            vertexRemap = new int[this.verts.Num()];
            Arrays.fill(vertexRemap, -1, 0, this.verts.Num());

            vertexCopyIndex = new int[(numEdgeSplitVertexes + this.verts.Num())];

            vertexIndexNum[0] = 0;
            vertexIndexNum[1] = numEdgeSplitVertexes;

            indexPtr = newIndexes.Ptr();
            indexNum = newIndexes.Num();

            // split surface triangles
            for (i = 0; i < this.edgeIndexes.Num(); i += 3) {
                int e0, e1, e2, v0, v1, v2;

                e0 = Math.abs(this.edgeIndexes.oGet(i + 0));
                e1 = Math.abs(this.edgeIndexes.oGet(i + 1));
                e2 = Math.abs(this.edgeIndexes.oGet(i + 2));

                v0 = this.indexes.oGet(i + 0);
                v1 = this.indexes.oGet(i + 1);
                v2 = this.indexes.oGet(i + 2);

                switch ((INTSIGNBITSET(edgeSplitVertex[e0]) | (INTSIGNBITSET(edgeSplitVertex[e1]) << 1) | (INTSIGNBITSET(edgeSplitVertex[e2]) << 2)) ^ 7) {
                    case 0: {	// no edges split
                        if (((sides[v0] | sides[v1] | sides[v2]) & SIDE_BACK) != 0) {
                            break;
                        }
                        if (((sides[v0] & sides[v1] & sides[v2]) & SIDE_ON) != 0) {
                            // coplanar
                            if (!keepOn) {
                                break;
                            }
                            f = this.verts.oGet(v1).xyz.oMinus(this.verts.oGet(v0).xyz).Cross(this.verts.oGet(v0).xyz.oMinus(this.verts.oGet(v2).xyz)).oMultiply(plane.Normal());
                            if (FLOATSIGNBITSET(f) != 0) {
                                break;
                            }
                        }
                        indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                        indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                        indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                        break;
                    }
                    case 1: {	// first edge split
                        if ((sides[v0] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                        } else {
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                        }
                        break;
                    }
                    case 2: {	// second edge split
                        if ((sides[v1] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                        } else {
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                        }
                        break;
                    }
                    case 3: {	// first and second edge split
                        if ((sides[v1] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                        } else {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                        }
                        break;
                    }
                    case 4: {	// third edge split
                        if ((sides[v2] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                        } else {
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                        }
                        break;
                    }
                    case 5: {	// first and third edge split
                        if ((sides[v0] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                        } else {
                            indexPtr[indexNum++] = edgeSplitVertex[e0];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                        }
                        break;
                    }
                    case 6: {	// second and third edge split
                        if ((sides[v2] & SIDE_BACK) == 0) {
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v2);
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                        } else {
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = edgeSplitVertex[e1];
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v0);
                            indexPtr[indexNum++] = UpdateVertexIndex(vertexIndexNum, vertexRemap, vertexCopyIndex, v1);
                            indexPtr[indexNum++] = edgeSplitVertex[e2];
                        }
                        break;
                    }
                }
            }

            newIndexes.SetNum(indexNum, false);

            // copy vertexes
            newVerts.SetNum(vertexIndexNum[1], false);
            for (i = numEdgeSplitVertexes; i < newVerts.Num(); i++) {
                newVerts.oSet(i, this.verts.oGet(vertexCopyIndex[i]));
            }

            // copy back to this surface
            this.indexes = newIndexes;
            this.verts = newVerts;

            GenerateEdgeIndexes();

            return true;
        }
//
        // returns true if each triangle can be reached from any other triangle by a traversal

        public boolean IsConnected() {
            int i, j, numIslands, numTris;
            int queueStart, queueEnd;
            int[] queue, islandNum;
            int curTri, nextTri, edgeNum;
            int index;

            numIslands = 0;
            numTris = this.indexes.Num() / 3;
            islandNum = new int[numTris];
            Arrays.fill(islandNum, -1, 0, numTris);
            queue = new int[numTris];

            for (i = 0; i < numTris; i++) {

                if (islandNum[i] != -1) {
                    continue;
                }

                queueStart = 0;
                queueEnd = 1;
                queue[0] = i;
                islandNum[i] = numIslands;

                for (curTri = queue[queueStart]; queueStart < queueEnd; curTri = queue[++queueStart]) {

                    index = curTri * 3;

                    for (j = 0; j < 3; j++) {

                        edgeNum = this.edgeIndexes.oGet(index + j);
                        nextTri = this.edges.oGet(Math.abs(edgeNum)).tris[INTSIGNBITNOTSET(edgeNum)];

                        if (nextTri == -1) {
                            continue;
                        }

                        nextTri /= 3;

                        if (islandNum[nextTri] != -1) {
                            continue;
                        }

                        queue[queueEnd++] = nextTri;
                        islandNum[nextTri] = numIslands;
                    }
                }
                numIslands++;
            }

            return (numIslands == 1);
        }

        // returns true if the surface is closed
        public boolean IsClosed() {
            for (int i = 0; i < this.edges.Num(); i++) {
                if ((this.edges.oGet(i).tris[0] < 0) || (this.edges.oGet(i).tris[1] < 0)) {
                    return false;
                }
            }
            return true;
        }

        public boolean IsPolytope() {
            return IsPolytope(0.1f);
        }
        // returns true if the surface is a convex hull

        public boolean IsPolytope(final float epsilon) {
            int i, j;
            final idPlane plane = new idPlane();

            if (!IsClosed()) {
                return false;
            }

            for (i = 0; i < this.indexes.Num(); i += 3) {
                plane.FromPoints(this.verts.oGet(
                        this.indexes.oGet(i + 0)).xyz,
                        this.verts.oGet(this.indexes.oGet(i + 1)).xyz,
                        this.verts.oGet(this.indexes.oGet(i + 2)).xyz);

                for (j = 0; j < this.verts.Num(); j++) {
                    if (plane.Side(this.verts.oGet(j).xyz, epsilon) == SIDE_FRONT) {
                        return false;
                    }
                }
            }
            return true;
        }
//

        public float PlaneDistance(final idPlane plane) {
            int i;
            float d, min, max;

            min = idMath.INFINITY;
            max = -min;
            for (i = 0; i < this.verts.Num(); i++) {
                d = plane.Distance(this.verts.oGet(i).xyz);
                if (d < min) {
                    min = d;
                    if ((FLOATSIGNBITSET(min) & FLOATSIGNBITNOTSET(max)) != 0) {
                        return 0.0f;
                    }
                }
                if (d > max) {
                    max = d;
                    if ((FLOATSIGNBITSET(min) & FLOATSIGNBITNOTSET(max)) != 0) {
                        return 0.0f;
                    }
                }
            }
            if (FLOATSIGNBITNOTSET(min) != 0) {
                return min;
            }
            if (FLOATSIGNBITSET(max) != 0) {
                return max;
            }
            return 0.0f;
        }

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            boolean front, back;
            int i;
            float d;

            front = false;
            back = false;
            for (i = 0; i < this.verts.Num(); i++) {
                d = plane.Distance(this.verts.oGet(i).xyz);
                if (d < -epsilon) {
                    if (front) {
                        return SIDE_CROSS;
                    }
                    back = true;
                    continue;
                } else if (d > epsilon) {
                    if (back) {
                        return SIDE_CROSS;
                    }
                    front = true;
                    continue;
                }
            }

            if (back) {
                return SIDE_BACK;
            }
            if (front) {
                return SIDE_FRONT;
            }
            return SIDE_ON;
        }

//
        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            return LineIntersection(start, end, false);
        }
        // returns true if the line intersects one of the surface triangles

        public boolean LineIntersection(final idVec3 start, final idVec3 end, boolean backFaceCull) {
            final float[] scale = new float[1];//TODO:check bakref

            RayIntersection(start, end.oMinus(start), scale, false);
            return ((scale[0] >= 0.0f) && (scale[0] <= 1.0f));
        }

        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale) {
            return RayIntersection(start, dir, scale, false);
        }
        // intersection point is start + dir * scale

        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale, boolean backFaceCull) {
            int i, i0, i1, i2, s0, s1, s2;
            float d;
            final float[] s = new float[0];
            byte[] sidedness;
            final idPluecker rayPl = new idPluecker(), pl = new idPluecker();
            final idPlane plane = new idPlane();

            sidedness = new byte[this.edges.Num()];
            scale[0] = idMath.INFINITY;

            rayPl.FromRay(start, dir);

            // ray sidedness for edges
            for (i = 0; i < this.edges.Num(); i++) {
                pl.FromLine(this.verts.oGet(this.edges.oGet(i).verts[1]).xyz, this.verts.oGet(this.edges.oGet(i).verts[0]).xyz);
                d = pl.PermutedInnerProduct(rayPl);
                sidedness[i] = (byte) FLOATSIGNBITSET(d);
            }

            // test triangles
            for (i = 0; i < this.edgeIndexes.Num(); i += 3) {
                i0 = this.edgeIndexes.oGet(i + 0);
                i1 = this.edgeIndexes.oGet(i + 1);
                i2 = this.edgeIndexes.oGet(i + 2);
                s0 = sidedness[Math.abs(i0)] ^ INTSIGNBITSET(i0);
                s1 = sidedness[Math.abs(i1)] ^ INTSIGNBITSET(i1);
                s2 = sidedness[Math.abs(i2)] ^ INTSIGNBITSET(i2);

                if ((s0 & s1 & s2) != 0) {
                    plane.FromPoints(
                            this.verts.oGet(this.indexes.oGet(i + 0)).xyz,
                            this.verts.oGet(this.indexes.oGet(i + 1)).xyz,
                            this.verts.oGet(this.indexes.oGet(i + 2)).xyz);
                    plane.RayIntersection(start, dir, s);
                    if (idMath.Fabs(s[0]) < idMath.Fabs(scale[0])) {
                        scale[0] = s[0];
                    }
                } else if (!backFaceCull && (((s0 | s1 | s2)) == 0)) {
                    plane.FromPoints(
                            this.verts.oGet(this.indexes.oGet(i + 0)).xyz,
                            this.verts.oGet(this.indexes.oGet(i + 1)).xyz,
                            this.verts.oGet(this.indexes.oGet(i + 2)).xyz);
                    plane.RayIntersection(start, dir, s);
                    if (idMath.Fabs(s[0]) < idMath.Fabs(scale[0])) {
                        scale[0] = s[0];
                    }
                }
            }

            if (idMath.Fabs(scale[0]) < idMath.INFINITY) {
                return true;
            }
            return false;
        }

        /*
         =================
         idSurface::GenerateEdgeIndexes

         Assumes each edge is shared by at most two triangles.
         =================
         */
        protected void GenerateEdgeIndexes() {
            int i, j, i0, i1, i2, s, v0, v1, edgeNum;
            int[]  vertexEdges, edgeChain;
            Integer[] index;
            final surfaceEdge_t[] e = surfaceEdge_t.generateArray(3);

            vertexEdges = new int[this.verts.Num()];
            Arrays.fill(vertexEdges, 0, this.verts.Num(), -1);
            edgeChain = new int[this.indexes.Num()];

            this.edgeIndexes.SetNum(this.indexes.Num(), true);

            this.edges.Clear();

            // the first edge is a dummy
            e[0].verts[0] = e[0].verts[1] = e[0].tris[0] = e[0].tris[1] = 0;
            this.edges.Append(e[0]);

            for (i = 0; i < this.indexes.Num(); i += 3) {
                index = this.indexes.Ptr();//index = indexes.Ptr() + i;
                // vertex numbers
                i0 = index[i + 0];
                i1 = index[i + 1];
                i2 = index[i + 2];
                // setup edges each with smallest vertex number first
                s = INTSIGNBITSET(i1 - i0);
                e[0].verts[0] = index[i + s];
                e[0].verts[1] = index[(i + s) ^ 1];
                s = INTSIGNBITSET(i2 - i1) + 1;
                e[1].verts[0] = index[i + s];
                e[1].verts[1] = index[(i + s) ^ 3];
                s = INTSIGNBITSET(i2 - i0) << 1;
                e[2].verts[0] = index[i + s];
                e[2].verts[1] = index[(i + s) ^ 2];
                // get edges
                for (j = 0; j < 3; j++) {
                    v0 = e[j].verts[0];
                    v1 = e[j].verts[1];
                    for (edgeNum = vertexEdges[v0]; edgeNum >= 0; edgeNum = edgeChain[edgeNum]) {
                        if (this.edges.oGet(edgeNum).verts[1] == v1) {
                            break;
                        }
                    }
                    // if the edge does not yet exist
                    if (edgeNum < 0) {
                        e[j].tris[0] = e[j].tris[1] = -1;
                        edgeNum = this.edges.Append(e[j]);
                        edgeChain[edgeNum] = vertexEdges[v0];
                        vertexEdges[v0] = edgeNum;
                    }
                    // update edge index and edge tri references
                    if (index[i + j] == v0) {
                        assert (this.edges.oGet(edgeNum).tris[0] == -1); // edge may not be shared by more than two triangles
                        this.edges.oGet(edgeNum).tris[0] = i;
                        this.edgeIndexes.oSet(i + j, edgeNum);
                    } else {
                        assert (this.edges.oGet(edgeNum).tris[1] == -1); // edge may not be shared by more than two triangles
                        this.edges.oGet(edgeNum).tris[1] = i;
                        this.edgeIndexes.oSet(i + j, -edgeNum);
                    }
                }
            }
        }

        protected int FindEdge(int v1, int v2) {
            int i, firstVert, secondVert;

            if (v1 < v2) {
                firstVert = v1;
                secondVert = v2;
            } else {
                firstVert = v2;
                secondVert = v1;
            }
            for (i = 1; i < this.edges.Num(); i++) {
                if (this.edges.oGet(i).verts[0] == firstVert) {
                    if (this.edges.oGet(i).verts[1] == secondVert) {
                        break;
                    }
                }
            }
            if (i < this.edges.Num()) {
                return v1 < v2 ? i : -i;
            }
            return 0;
        }
    }
}
