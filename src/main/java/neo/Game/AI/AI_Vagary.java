package neo.Game.AI;

import neo.Game.AI.AI.idAI;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Event.idEventDef;
import static neo.Game.GameSys.SysCvar.ai_debugTrajectory;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Moveable.idMoveable;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Script.Script_Thread.idThread;
import static neo.TempDump.NOT;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AI_Vagary {
    /* **********************************************************************

     game/ai/AI_Vagary.cpp

     Vagary specific AI code

     ***********************************************************************/

    static final idEventDef AI_Vagary_ChooseObjectToThrow = new idEventDef("vagary_ChooseObjectToThrow", "vvfff", 'e');
    static final idEventDef AI_Vagary_ThrowObjectAtEnemy  = new idEventDef("vagary_ThrowObjectAtEnemy", "ef");
//

    public static class idAI_Vagary extends idAI {
        //CLASS_PROTOTYPE( idAI_Vagary );

        private void Event_ChooseObjectToThrow(final idVec3 mins, final idVec3 maxs, float speed, float minDist, float offset) {
            idEntity ent;
            idEntity[] entityList = new idEntity[MAX_GENTITIES];
            int numListedEntities;
            int i, index;
            float dist;
            idVec3 vel = new idVec3();
            idVec3 offsetVec = new idVec3(0, 0, offset);
            idEntity enemyEnt = enemy.GetEntity();

            if (null == enemyEnt) {
                idThread.ReturnEntity(null);
            }

            idVec3 enemyEyePos = lastVisibleEnemyPos.oPlus(lastVisibleEnemyEyeOffset);
            final idBounds myBounds = physicsObj.GetAbsBounds();
            idBounds checkBounds = new idBounds(mins, maxs);
            checkBounds.TranslateSelf(physicsObj.GetOrigin());
            numListedEntities = gameLocal.clip.EntitiesTouchingBounds(checkBounds, -1, entityList, MAX_GENTITIES);

            index = gameLocal.random.RandomInt(numListedEntities);
            for (i = 0; i < numListedEntities; i++, index++) {
                if (index >= numListedEntities) {
                    index = 0;
                }
                ent = entityList[ index];
                if (!ent.IsType(idMoveable.class)) {
                    continue;
                }

                if (ent.fl.hidden) {
                    // don't throw hidden objects
                    continue;
                }

                idPhysics entPhys = ent.GetPhysics();
                final idVec3 entOrg = entPhys.GetOrigin();
                dist = (entOrg.oMinus(enemyEyePos)).LengthFast();
                if (dist < minDist) {
                    continue;
                }

                idBounds expandedBounds = myBounds.Expand(entPhys.GetBounds().GetRadius());
                if (expandedBounds.LineIntersection(entOrg, enemyEyePos)) {
                    // ignore objects that are behind us
                    continue;
                }

                if (PredictTrajectory(entPhys.GetOrigin().oPlus(offsetVec), enemyEyePos, speed, entPhys.GetGravity(),
                        entPhys.GetClipModel(), entPhys.GetClipMask(), MAX_WORLD_SIZE, null, enemyEnt, ai_debugTrajectory.GetBool() ? 4000 : 0, vel)) {
                    idThread.ReturnEntity(ent);
                    return;
                }
            }

            idThread.ReturnEntity(null);
        }

        private void Event_ThrowObjectAtEnemy(idEntity ent, float speed) {
            idVec3 vel = new idVec3();
            idEntity enemyEnt;
            idPhysics entPhys;

            entPhys = ent.GetPhysics();
            enemyEnt = enemy.GetEntity();
            if (NOT(enemyEnt)) {
                vel = (viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis())).oMultiply(speed);
            } else {
                PredictTrajectory(entPhys.GetOrigin(), lastVisibleEnemyPos.oPlus(lastVisibleEnemyEyeOffset), speed, entPhys.GetGravity(),
                        entPhys.GetClipModel(), entPhys.GetClipMask(), MAX_WORLD_SIZE, null, enemyEnt, ai_debugTrajectory.GetBool() ? 4000 : 0, vel);
                vel.oMulSet(speed);
            }

            entPhys.SetLinearVelocity(vel);

            if (ent.IsType(idMoveable.class)) {
                idMoveable ment = (idMoveable) ent;
                ment.EnableDamage(true, 2.5f);
            }
        }
    };
}
