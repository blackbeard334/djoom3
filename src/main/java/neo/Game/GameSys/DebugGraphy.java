package neo.Game.GameSys;

import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;

import neo.Game.Game_local.idGameLocal;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class DebugGraphy {

    public static class idDebugGraph {

        private idList<Float> samples;
        private int           index;
        //
        //

        public idDebugGraph() {
            this.index = 0;
        }

        public void SetNumSamples(int num) {
            this.index = 0;
            this.samples.Clear();
            this.samples.SetNum(num);
//            memset(samples.Ptr(), 0, samples.MemoryUsed());
        }

        public void AddValue(float value) {
            this.samples.oSet(this.index++, value);
            if (this.index >= this.samples.Num()) {
                this.index = 0;
            }
        }

        public void Draw(final idVec4 color, float scale) {
            int i;
            float value1;
            float value2;
            idVec3 vec1;
            idVec3 vec2;

            final idMat3 axis = gameLocal.GetLocalPlayer().viewAxis;
            final idVec3 pos = gameLocal.GetLocalPlayer().GetPhysics().GetOrigin().oPlus(axis.oGet(1).oMultiply(this.samples.Num() * 0.5f));

            value1 = this.samples.oGet(this.index) * scale;
            for (i = 1; i < this.samples.Num(); i++) {
                value2 = this.samples.oGet((i + this.index) % this.samples.Num()) * scale;

                vec1 = pos.oPlus(axis.oGet(2).oMultiply(value1).oMinus(axis.oGet(1).oMultiply(i - 1).oPlus(axis.oGet(0).oMultiply(this.samples.Num()))));
                vec2 = pos.oPlus(axis.oGet(2).oMultiply(value2).oMinus(axis.oGet(1).oMultiply(i).oPlus(axis.oGet(0).oMultiply(this.samples.Num()))));

                gameRenderWorld.DebugLine(color, vec1, vec2, idGameLocal.msec, false);
                value1 = value2;
            }
        }

    }
}
