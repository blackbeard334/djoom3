package neo.Game.AI;

import neo.Game.GameSys.Event.idEventDef;

/**
 *
 */
public class AI_Events {

    /* **********************************************************************

     AI Events

     ***********************************************************************/
    public static final idEventDef AI_FindEnemy                     = new idEventDef("findEnemy", "d", 'e');
    public static final idEventDef AI_FindEnemyAI                   = new idEventDef("findEnemyAI", "d", 'e');
    public static final idEventDef AI_FindEnemyInCombatNodes        = new idEventDef("findEnemyInCombatNodes", null, 'e');
    public static final idEventDef AI_ClosestReachableEnemyOfEntity = new idEventDef("closestReachableEnemyOfEntity", "E", 'e');
    public static final idEventDef AI_HeardSound                    = new idEventDef("heardSound", "d", 'e');
    public static final idEventDef AI_SetEnemy                      = new idEventDef("setEnemy", "E");
    public static final idEventDef AI_ClearEnemy                    = new idEventDef("clearEnemy");
    public static final idEventDef AI_MuzzleFlash                   = new idEventDef("muzzleFlash", "s");
    public static final idEventDef AI_CreateMissile                 = new idEventDef("createMissile", "s", 'e');
    public static final idEventDef AI_AttackMissile                 = new idEventDef("attackMissile", "s", 'e');
    public static final idEventDef AI_FireMissileAtTarget           = new idEventDef("fireMissileAtTarget", "ss", 'e');
    public static final idEventDef AI_LaunchMissile                 = new idEventDef("launchMissile", "vv", 'e');
    public static final idEventDef AI_AttackMelee                   = new idEventDef("attackMelee", "s", 'd');
    public static final idEventDef AI_DirectDamage                  = new idEventDef("directDamage", "es");
    public static final idEventDef AI_RadiusDamageFromJoint         = new idEventDef("radiusDamageFromJoint", "ss");
    public static final idEventDef AI_BeginAttack                   = new idEventDef("attackBegin", "s");
    public static final idEventDef AI_EndAttack                     = new idEventDef("attackEnd");
    public static final idEventDef AI_MeleeAttackToJoint            = new idEventDef("meleeAttackToJoint", "ss", 'd');
    public static final idEventDef AI_RandomPath                    = new idEventDef("randomPath", null, 'e');
    public static final idEventDef AI_CanBecomeSolid                = new idEventDef("canBecomeSolid", null, 'f');
    public static final idEventDef AI_BecomeSolid                   = new idEventDef("becomeSolid");
    public static final idEventDef AI_BecomeRagdoll                 = new idEventDef("becomeRagdoll", null, 'd');
    public static final idEventDef AI_StopRagdoll                   = new idEventDef("stopRagdoll");
    public static final idEventDef AI_SetHealth                     = new idEventDef("setHealth", "f");
    public static final idEventDef AI_GetHealth                     = new idEventDef("getHealth", null, 'f');
    public static final idEventDef AI_AllowDamage                   = new idEventDef("allowDamage");
    public static final idEventDef AI_IgnoreDamage                  = new idEventDef("ignoreDamage");
    public static final idEventDef AI_GetCurrentYaw                 = new idEventDef("getCurrentYaw", null, 'f');
    public static final idEventDef AI_TurnTo                        = new idEventDef("turnTo", "f");
    public static final idEventDef AI_TurnToPos                     = new idEventDef("turnToPos", "v");
    public static final idEventDef AI_TurnToEntity                  = new idEventDef("turnToEntity", "E");
    public static final idEventDef AI_MoveStatus                    = new idEventDef("moveStatus", null, 'd');
    public static final idEventDef AI_StopMove                      = new idEventDef("stopMove");
    public static final idEventDef AI_MoveToCover                   = new idEventDef("moveToCover");
    public static final idEventDef AI_MoveToEnemy                   = new idEventDef("moveToEnemy");
    public static final idEventDef AI_MoveToEnemyHeight             = new idEventDef("moveToEnemyHeight");
    public static final idEventDef AI_MoveOutOfRange                = new idEventDef("moveOutOfRange", "ef");
    public static final idEventDef AI_MoveToAttackPosition          = new idEventDef("moveToAttackPosition", "es");
    public static final idEventDef AI_Wander                        = new idEventDef("wander");
    public static final idEventDef AI_MoveToEntity                  = new idEventDef("moveToEntity", "e");
    public static final idEventDef AI_MoveToPosition                = new idEventDef("moveToPosition", "v");
    public static final idEventDef AI_SlideTo                       = new idEventDef("slideTo", "vf");
    public static final idEventDef AI_FacingIdeal                   = new idEventDef("facingIdeal", null, 'd');
    public static final idEventDef AI_FaceEnemy                     = new idEventDef("faceEnemy");
    public static final idEventDef AI_FaceEntity                    = new idEventDef("faceEntity", "E");
    public static final idEventDef AI_GetCombatNode                 = new idEventDef("getCombatNode", null, 'e');
    public static final idEventDef AI_EnemyInCombatCone             = new idEventDef("enemyInCombatCone", "Ed", 'd');
    public static final idEventDef AI_WaitMove                      = new idEventDef("waitMove");
    public static final idEventDef AI_GetJumpVelocity               = new idEventDef("getJumpVelocity", "vff", 'v');
    public static final idEventDef AI_EntityInAttackCone            = new idEventDef("entityInAttackCone", "E", 'd');
    public static final idEventDef AI_CanSeeEntity                  = new idEventDef("canSee", "E", 'd');
    public static final idEventDef AI_SetTalkTarget                 = new idEventDef("setTalkTarget", "E");
    public static final idEventDef AI_GetTalkTarget                 = new idEventDef("getTalkTarget", null, 'e');
    public static final idEventDef AI_SetTalkState                  = new idEventDef("setTalkState", "d");
    public static final idEventDef AI_EnemyRange                    = new idEventDef("enemyRange", null, 'f');
    public static final idEventDef AI_EnemyRange2D                  = new idEventDef("enemyRange2D", null, 'f');
    public static final idEventDef AI_GetEnemy                      = new idEventDef("getEnemy", null, 'e');
    public static final idEventDef AI_GetEnemyPos                   = new idEventDef("getEnemyPos", null, 'v');
    public static final idEventDef AI_GetEnemyEyePos                = new idEventDef("getEnemyEyePos", null, 'v');
    public static final idEventDef AI_PredictEnemyPos               = new idEventDef("predictEnemyPos", "f", 'v');
    public static final idEventDef AI_CanHitEnemy                   = new idEventDef("canHitEnemy", null, 'd');
    public static final idEventDef AI_CanHitEnemyFromAnim           = new idEventDef("canHitEnemyFromAnim", "s", 'd');
    public static final idEventDef AI_CanHitEnemyFromJoint          = new idEventDef("canHitEnemyFromJoint", "s", 'd');
    public static final idEventDef AI_EnemyPositionValid            = new idEventDef("enemyPositionValid", null, 'd');
    public static final idEventDef AI_ChargeAttack                  = new idEventDef("chargeAttack", "s");
    public static final idEventDef AI_TestChargeAttack              = new idEventDef("testChargeAttack", null, 'f');
    public static final idEventDef AI_TestMoveToPosition            = new idEventDef("testMoveToPosition", "v", 'd');
    public static final idEventDef AI_TestAnimMoveTowardEnemy       = new idEventDef("testAnimMoveTowardEnemy", "s", 'd');
    public static final idEventDef AI_TestAnimMove                  = new idEventDef("testAnimMove", "s", 'd');
    public static final idEventDef AI_TestMeleeAttack               = new idEventDef("testMeleeAttack", null, 'd');
    public static final idEventDef AI_TestAnimAttack                = new idEventDef("testAnimAttack", "s", 'd');
    public static final idEventDef AI_Shrivel                       = new idEventDef("shrivel", "f");
    public static final idEventDef AI_Burn                          = new idEventDef("burn");
    public static final idEventDef AI_ClearBurn                     = new idEventDef("clearBurn");
    public static final idEventDef AI_PreBurn                       = new idEventDef("preBurn");
    public static final idEventDef AI_SetSmokeVisibility            = new idEventDef("setSmokeVisibility", "dd");
    public static final idEventDef AI_NumSmokeEmitters              = new idEventDef("numSmokeEmitters", null, 'd');
    public static final idEventDef AI_WaitAction                    = new idEventDef("waitAction", "s");
    public static final idEventDef AI_StopThinking                  = new idEventDef("stopThinking");
    public static final idEventDef AI_GetTurnDelta                  = new idEventDef("getTurnDelta", null, 'f');
    public static final idEventDef AI_GetMoveType                   = new idEventDef("getMoveType", null, 'd');
    public static final idEventDef AI_SetMoveType                   = new idEventDef("setMoveType", "d");
    public static final idEventDef AI_SaveMove                      = new idEventDef("saveMove");
    public static final idEventDef AI_RestoreMove                   = new idEventDef("restoreMove");
    public static final idEventDef AI_AllowMovement                 = new idEventDef("allowMovement", "f");
    public static final idEventDef AI_JumpFrame                     = new idEventDef("<jumpframe>");
    public static final idEventDef AI_EnableClip                    = new idEventDef("enableClip");
    public static final idEventDef AI_DisableClip                   = new idEventDef("disableClip");
    public static final idEventDef AI_EnableGravity                 = new idEventDef("enableGravity");
    public static final idEventDef AI_DisableGravity                = new idEventDef("disableGravity");
    public static final idEventDef AI_EnableAFPush                  = new idEventDef("enableAFPush");
    public static final idEventDef AI_DisableAFPush                 = new idEventDef("disableAFPush");
    public static final idEventDef AI_SetFlySpeed                   = new idEventDef("setFlySpeed", "f");
    public static final idEventDef AI_SetFlyOffset                  = new idEventDef("setFlyOffset", "d");
    public static final idEventDef AI_ClearFlyOffset                = new idEventDef("clearFlyOffset");
    public static final idEventDef AI_GetClosestHiddenTarget        = new idEventDef("getClosestHiddenTarget", "s", 'e');
    public static final idEventDef AI_GetRandomTarget               = new idEventDef("getRandomTarget", "s", 'e');
    public static final idEventDef AI_TravelDistanceToPoint         = new idEventDef("travelDistanceToPoint", "v", 'f');
    public static final idEventDef AI_TravelDistanceToEntity        = new idEventDef("travelDistanceToEntity", "e", 'f');
    public static final idEventDef AI_TravelDistanceBetweenPoints   = new idEventDef("travelDistanceBetweenPoints", "vv", 'f');
    public static final idEventDef AI_TravelDistanceBetweenEntities = new idEventDef("travelDistanceBetweenEntities", "ee", 'f');
    public static final idEventDef AI_LookAtEntity                  = new idEventDef("lookAt", "Ef");
    public static final idEventDef AI_LookAtEnemy                   = new idEventDef("lookAtEnemy", "f");
    public static final idEventDef AI_SetJointMod                   = new idEventDef("setBoneMod", "d");
    public static final idEventDef AI_ThrowMoveable                 = new idEventDef("throwMoveable");
    public static final idEventDef AI_ThrowAF                       = new idEventDef("throwAF");
    public static final idEventDef AI_RealKill                      = new idEventDef("<kill>");
    public static final idEventDef AI_Kill                          = new idEventDef("kill");
    public static final idEventDef AI_WakeOnFlashlight              = new idEventDef("wakeOnFlashlight", "d");
    public static final idEventDef AI_LocateEnemy                   = new idEventDef("locateEnemy");
    public static final idEventDef AI_KickObstacles                 = new idEventDef("kickObstacles", "Ef");
    public static final idEventDef AI_GetObstacle                   = new idEventDef("getObstacle", null, 'e');
    public static final idEventDef AI_PushPointIntoAAS              = new idEventDef("pushPointIntoAAS", "v", 'v');
    public static final idEventDef AI_GetTurnRate                   = new idEventDef("getTurnRate", null, 'f');
    public static final idEventDef AI_SetTurnRate                   = new idEventDef("setTurnRate", "f");
    public static final idEventDef AI_AnimTurn                      = new idEventDef("animTurn", "f");
    public static final idEventDef AI_AllowHiddenMovement           = new idEventDef("allowHiddenMovement", "d");
    public static final idEventDef AI_TriggerParticles              = new idEventDef("triggerParticles", "s");
    public static final idEventDef AI_FindActorsInBounds            = new idEventDef("findActorsInBounds", "vv", 'e');
    public static final idEventDef AI_CanReachPosition              = new idEventDef("canReachPosition", "v", 'd');
    public static final idEventDef AI_CanReachEntity                = new idEventDef("canReachEntity", "E", 'd');
    public static final idEventDef AI_CanReachEnemy                 = new idEventDef("canReachEnemy", null, 'd');
    public static final idEventDef AI_GetReachableEntityPosition    = new idEventDef("getReachableEntityPosition", "e", 'v');
}
