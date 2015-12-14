package neo.Game.Physics;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import static neo.Game.AFEntity.EV_Gib;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idEntity;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Item.idMoveableItem;
import neo.Game.Moveable.idMoveable;
import static neo.Game.Physics.Clip.CLIPMODEL_ID_TO_JOINT_HANDLE;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import neo.Game.Player.idPlayer;
import static neo.Game.Projectile.EV_Explode;
import neo.Game.Projectile.idProjectile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Push {

    /*
     ===============================================================================

     Allows physics objects to be pushed geometrically.

     ===============================================================================
     */
    public static final int PUSHFL_ONLYMOVEABLE     = 1;    // only push moveable entities
    public static final int PUSHFL_NOGROUNDENTITIES = 2;    // don't push entities the clip model rests upon
    public static final int PUSHFL_CLIP             = 4;    // also clip against all non-moveable entities
    public static final int PUSHFL_CRUSH            = 8;    // kill blocking entities
    public static final int PUSHFL_APPLYIMPULSE     = 16;   // apply impulse to pushed entities
    //
    //
//enum {
    public static final int PUSH_NO                 = 0;    // not pushed
    public static final int PUSH_OK                 = 1;    // pushed ok
    public static final int PUSH_BLOCKED            = 2;    // blocked
//};

    //#define NEW_PUSH
    public static class idPush {

        /*
         ============
         idPush::ClipTranslationalPush

         Try to push other entities by moving the given entity.
         ============
         */
        // If results.fraction < 1.0 the move was blocked by results.c.entityNum
        // Returns total mass of all pushed entities.
        public float ClipTranslationalPush(trace_s[] results, idEntity pusher, final int flags, final idVec3 newOrigin, final idVec3 translation) {
            int i, listedEntities, res;
            idEntity check;
            idEntity[] entityList = new idEntity[MAX_GENTITIES];
            idBounds bounds, pushBounds = new idBounds();
            idVec3 clipMove, clipOrigin, oldOrigin, dir, impulse = new idVec3();
            trace_s[] pushResults = {null};
            boolean wasEnabled;
            float totalMass;
            idClipModel clipModel;

            clipModel = pusher.GetPhysics().GetClipModel();

            totalMass = 0.0f;

            results[0].fraction = 1.0f;
            results[0].endpos = newOrigin;
            results[0].endAxis = clipModel.GetAxis();
//	memset( &results.c, 0, sizeof( results.c ) );//TODO:

            if (translation.equals(getVec3_origin())) {
                return totalMass;
            }

            dir = translation;
            dir.Normalize();
            dir.z += 1.0f;
            dir.oMulSet(10.0f);

            // get bounds for the whole movement
            bounds = clipModel.GetBounds();
            if (bounds.oGet(0).x >= bounds.oGet(1).x) {
                return totalMass;
            }
            pushBounds.FromBoundsTranslation(bounds, clipModel.GetOrigin(), clipModel.GetAxis(), translation);

            wasEnabled = clipModel.IsEnabled();

            // make sure we don't get the pushing clip model in the list
            clipModel.Disable();

            listedEntities = gameLocal.clip.EntitiesTouchingBounds(pushBounds, -1, entityList, MAX_GENTITIES);

            // discard entities we cannot or should not push
            listedEntities = DiscardEntities(entityList, listedEntities, flags, pusher);

            if ((flags & PUSHFL_CLIP) != 0) {

                // can only clip movement of a trace model
                assert (clipModel.IsTraceModel());

                // disable to be pushed entities for collision detection
                for (i = 0; i < listedEntities; i++) {
                    entityList[i].GetPhysics().DisableClip();
                }

                gameLocal.clip.Translation(results, clipModel.GetOrigin(), clipModel.GetOrigin().oPlus(translation), clipModel, clipModel.GetAxis(), pusher.GetPhysics().GetClipMask(), null);

                // enable to be pushed entities for collision detection
                for (i = 0; i < listedEntities; i++) {
                    entityList[i].GetPhysics().EnableClip();
                }

                if (results[0].fraction == 0.0f) {
                    if (wasEnabled) {
                        clipModel.Enable();
                    }
                    return totalMass;
                }

                clipMove = results[0].endpos.oMinus(clipModel.GetOrigin());
                clipOrigin = results[0].endpos;

            } else {

                clipMove = translation;
                clipOrigin = newOrigin;
            }

            // we have to enable the clip model because we use it during pushing
            clipModel.Enable();

            // save pusher old position
            oldOrigin = clipModel.GetOrigin();

            // try to push the entities
            for (i = 0; i < listedEntities; i++) {

                check = entityList[ i];

                idPhysics physics = check.GetPhysics();

                // disable the entity for collision detection
                physics.DisableClip();

                res = TryTranslatePushEntity(pushResults, check, clipModel, flags, clipOrigin, clipMove);

                // enable the entity for collision detection
                physics.EnableClip();

                // if the entity is pushed
                if (res == PUSH_OK) {
                    // set the pusher in the translated position
                    clipModel.Link(gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), newOrigin, clipModel.GetAxis());
                    // the entity might be pushed off the ground
                    physics.EvaluateContacts();
                    // put pusher back in old position
                    clipModel.Link(gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), oldOrigin, clipModel.GetAxis());

                    // wake up this object
                    if ((flags & PUSHFL_APPLYIMPULSE) != 0) {
                        impulse = dir.oMultiply(physics.GetMass());
                    } else {
                        impulse.Zero();
                    }
                    check.ApplyImpulse(clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), impulse);

                    // add mass of pushed entity
                    totalMass += physics.GetMass();
                }

                // if the entity is not blocking
                if (res != PUSH_BLOCKED) {
                    continue;
                }

                // if the blocking entity is a projectile
                if (check.IsType(idProjectile.class)) {
                    check.ProcessEvent(EV_Explode);
                    continue;
                }

                // if blocking entities should be crushed
                if ((flags & PUSHFL_CRUSH) != 0) {
                    check.Damage(clipModel.GetEntity(), clipModel.GetEntity(), getVec3_origin(), "damage_crush", 1.0f, CLIPMODEL_ID_TO_JOINT_HANDLE(pushResults[0].c.id));
                    continue;
                }

                // if the entity is an active articulated figure and gibs
                if (check.IsType(idAFEntity_Base.class) && check.spawnArgs.GetBool("gib")) {
                    if (((idAFEntity_Base) check).IsActiveAF()) {
                        check.ProcessEvent(EV_Gib, "damage_Gib");
                    }
                }

                // if the entity is a moveable item and gibs
                if (check.IsType(idMoveableItem.class) && check.spawnArgs.GetBool("gib")) {
                    check.ProcessEvent(EV_Gib, "damage_Gib");
                }

                // blocked
                results[0] = pushResults[0];
                results[0].fraction = 0.0f;
                results[0].endAxis = clipModel.GetAxis();
                results[0].endpos = clipModel.GetOrigin();
                results[0].c.entityNum = check.entityNumber;
                results[0].c.id = 0;

                if (!wasEnabled) {
                    clipModel.Disable();
                }

                return totalMass;
            }

            if (!wasEnabled) {
                clipModel.Disable();
            }

            return totalMass;
        }

        /*
         ============
         idPush::ClipRotationalPush

         Try to push other entities by moving the given entity.
         ============
         */
        public float ClipRotationalPush(trace_s[] results, idEntity pusher, final int flags, final idMat3 newAxis, final idRotation rotation) {
            int i, listedEntities, res;
            idEntity check;
            idEntity[] entityList = new idEntity[MAX_GENTITIES];
            idBounds bounds, pushBounds = new idBounds();
            idRotation clipRotation;
            idMat3 clipAxis, oldAxis;
            trace_s[] pushResults = {null};
            boolean wasEnabled;
            float totalMass;
            idClipModel clipModel;

            clipModel = pusher.GetPhysics().GetClipModel();

            totalMass = 0.0f;

            results[0].fraction = 1.0f;
            results[0].endpos = clipModel.GetOrigin();
            results[0].endAxis = newAxis;
//	memset( &results.c, 0, sizeof( results.c ) );//TODOS:
            results[0].c = new contactInfo_t();

            if (0 == rotation.GetAngle()) {
                return totalMass;
            }

            // get bounds for the whole movement
            bounds = clipModel.GetBounds();
            if (bounds.oGet(0).x >= bounds.oGet(1).x) {
                return totalMass;
            }
            pushBounds.FromBoundsRotation(bounds, clipModel.GetOrigin(), clipModel.GetAxis(), rotation);

            wasEnabled = clipModel.IsEnabled();

            // make sure we don't get the pushing clip model in the list
            clipModel.Disable();

            listedEntities = gameLocal.clip.EntitiesTouchingBounds(pushBounds, -1, entityList, MAX_GENTITIES);

            // discard entities we cannot or should not push
            listedEntities = DiscardEntities(entityList, listedEntities, flags, pusher);

            if ((flags & PUSHFL_CLIP) != 0) {

                // can only clip movement of a trace model
                assert (clipModel.IsTraceModel());

                // disable to be pushed entities for collision detection
                for (i = 0; i < listedEntities; i++) {
                    entityList[i].GetPhysics().DisableClip();
                }

                gameLocal.clip.Rotation(results, clipModel.GetOrigin(), rotation, clipModel, clipModel.GetAxis(), pusher.GetPhysics().GetClipMask(), null);

                // enable to be pushed entities for collision detection
                for (i = 0; i < listedEntities; i++) {
                    entityList[i].GetPhysics().EnableClip();
                }

                if (results[0].fraction == 0.0f) {
                    if (wasEnabled) {
                        clipModel.Enable();
                    }
                    return totalMass;
                }

                clipRotation = rotation.oMultiply(results[0].fraction);
                clipAxis = results[0].endAxis;
            } else {

                clipRotation = rotation;
                clipAxis = newAxis;
            }

            // we have to enable the clip model because we use it during pushing
            clipModel.Enable();

            // save pusher old position
            oldAxis = clipModel.GetAxis();

            // try to push all the entities
            for (i = 0; i < listedEntities; i++) {

                check = entityList[ i];

                idPhysics physics = check.GetPhysics();

                // disable the entity for collision detection
                physics.DisableClip();

                res = TryRotatePushEntity(pushResults, check, clipModel, flags, clipAxis, clipRotation);

                // enable the entity for collision detection
                physics.EnableClip();

                // if the entity is pushed
                if (res == PUSH_OK) {
                    // set the pusher in the rotated position
                    clipModel.Link(gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), newAxis);
                    // the entity might be pushed off the ground
                    physics.EvaluateContacts();
                    // put pusher back in old position
                    clipModel.Link(gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), oldAxis);

                    // wake up this object
                    check.ApplyImpulse(clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), getVec3_origin());

                    // add mass of pushed entity
                    totalMass += physics.GetMass();
                }

                // if the entity is not blocking
                if (res != PUSH_BLOCKED) {
                    continue;
                }

                // if the blocking entity is a projectile
                if (check.IsType(idProjectile.class)) {
                    check.ProcessEvent(EV_Explode);
                    continue;
                }

                // if blocking entities should be crushed
                if ((flags & PUSHFL_CRUSH) != 0) {
                    check.Damage(clipModel.GetEntity(), clipModel.GetEntity(), getVec3_origin(), "damage_crush", 1.0f, CLIPMODEL_ID_TO_JOINT_HANDLE(pushResults[0].c.id));
                    continue;
                }

                // if the entity is an active articulated figure and gibs
                if (check.IsType(idAFEntity_Base.class) && check.spawnArgs.GetBool("gib")) {
                    if (((idAFEntity_Base) check).IsActiveAF()) {
                        check.ProcessEvent(EV_Gib, "damage_Gib");
                    }
                }

                // blocked
                results[0] = pushResults[0];
                results[0].fraction = 0.0f;
                results[0].endAxis = clipModel.GetAxis();
                results[0].endpos = clipModel.GetOrigin();
                results[0].c.entityNum = check.entityNumber;
                results[0].c.id = 0;

                if (!wasEnabled) {
                    clipModel.Disable();
                }

                return totalMass;
            }

            if (!wasEnabled) {
                clipModel.Disable();
            }

            return totalMass;
        }

        /*
         ============
         idPush::ClipPush

         Try to push other entities by moving the given entity.
         ============
         */
        public float ClipPush(trace_s[] results, idEntity pusher, final int flags, final idVec3 oldOrigin, final idMat3 oldAxis, idVec3 newOrigin, idMat3 newAxis) {
            idVec3 translation;
            idRotation rotation;
            float mass;

            mass = 0.0f;

            results[0].fraction = 1.0f;
            results[0].endpos = newOrigin;
            results[0].endAxis = newAxis;
//	memset( &results.c, 0, sizeof( results.c ) );//TODOS:

            // translational push
            translation = newOrigin.oMinus(oldOrigin);

            // if the pusher translates
            if (translation != getVec3_origin()) {

                mass += ClipTranslationalPush(results, pusher, flags, newOrigin, translation);
                if (results[0].fraction < 1.0f) {
                    newOrigin.oSet(oldOrigin);
                    newAxis.oSet(oldAxis);
                    return mass;
                }
            } else {
                newOrigin.oSet(oldOrigin);
            }

            // rotational push
            rotation = (oldAxis.Transpose().oMultiply(newAxis)).ToRotation();
            rotation.SetOrigin(newOrigin);
            rotation.Normalize180();
            rotation.ReCalculateMatrix();		// recalculate the rotation matrix to avoid accumulating rounding errors

            // if the pusher rotates
            if (rotation.GetAngle() != 0.0f) {

                // recalculate new axis to avoid floating point rounding problems
                newAxis.oSet(oldAxis.oMultiply(rotation.ToMat3()));
                newAxis.OrthoNormalizeSelf();
                newAxis.FixDenormals();
                newAxis.FixDegeneracies();

                pusher.GetPhysics().GetClipModel().SetPosition(newOrigin, oldAxis);

                mass += ClipRotationalPush(results, pusher, flags, newAxis, rotation);
                if (results[0].fraction < 1.0f) {
                    newOrigin.oSet(oldOrigin);
                    newAxis.oSet(oldAxis);
                    return mass;
                }
            } else {
                newAxis.oSet(oldAxis);
            }

            return mass;
        }

        // initialize saving the positions of entities being pushed
        public void InitSavingPushedEntityPositions() {
            numPushed = 0;
        }

        // move all pushed entities back to their previous position
        public void RestorePushedEntityPositions() {
            int i;

            for (i = 0; i < numPushed; i++) {

                // if the entity is an actor
                if (pushed[i].ent.IsType(idActor.class)) {
                    // set back the delta view angles
                    ((idActor) pushed[i].ent).SetDeltaViewAngles(pushed[i].deltaViewAngles);
                }

                // restore the physics state
                pushed[i].ent.GetPhysics().RestoreState();
            }
        }

        // returns the number of pushed entities
        public int GetNumPushedEntities() {
            return numPushed;
        }

        // get the ith pushed entity
        public idEntity GetPushedEntity(int i) {
            assert (i >= 0 && i < numPushed);
            return pushed[i].ent;
        }

//
//
        private static class pushed_s {

            idEntity ent;					// pushed entity
            idAngles deltaViewAngles;		// actor delta view angles
        };
        private final pushed_s[] pushed = new pushed_s[MAX_GENTITIES];	// pushed entities
        private int numPushed;				// number of pushed entities
//

        private static class pushedGroup_s {

            idEntity ent;
            float fraction;
            boolean groundContact;
            boolean test;
        };
        private pushedGroup_s[] pushedGroup = new pushedGroup_s[MAX_GENTITIES];
        int pushedGroupSize;
//
//

        private void SaveEntityPosition(idEntity ent) {
            int i;

            // if already saved the physics state for this entity
            for (i = 0; i < numPushed; i++) {
                if (pushed[i].ent == ent) {
                    return;
                }
            }

            // don't overflow
            if (numPushed >= MAX_GENTITIES) {
                gameLocal.Error("more than MAX_GENTITIES pushed entities");
                return;
            }

            pushed[numPushed].ent = ent;

            // if the entity is an actor
            if (ent.IsType(idActor.class)) {
                // save the delta view angles
                pushed[numPushed].deltaViewAngles = ((idActor) ent).GetDeltaViewAngles();
            }

            // save the physics state
            ent.GetPhysics().SaveState();

            numPushed++;
        }

        private boolean RotateEntityToAxial(idEntity ent, idVec3 rotationPoint) {
            int i;
            trace_s[] trace = {null};
            idRotation rotation;
            idMat3 axis;
            idPhysics physics;

            physics = ent.GetPhysics();
            axis = physics.GetAxis();
            if (!axis.IsRotated()) {
                return true;
            }
            // try to rotate the bbox back to axial with at most four rotations
            for (i = 0; i < 4; i++) {
                axis = physics.GetAxis();
                rotation = axis.ToRotation();
                rotation.Scale(-1);
                rotation.SetOrigin(rotationPoint);
                // tiny float numbers in the clip axis, this can get the entity stuck
                if (rotation.GetAngle() == 0.0f) {
                    physics.SetAxis(getMat3_identity());
                    return true;
                }
                //
                ent.GetPhysics().ClipRotation(trace, rotation, null);
                // if the full rotation is possible
                if (trace[0].fraction >= 1.0f) {
                    // set bbox in final axial position
                    physics.SetOrigin(trace[0].endpos);
                    physics.SetAxis(getMat3_identity());
                    return true;
                } // if partial rotation was possible
                else if (trace[0].fraction > 0.0f) {
                    // partial rotation
                    physics.SetOrigin(trace[0].endpos);
                    physics.SetAxis(trace[0].endAxis);
                }
                // next rotate around collision point
                rotationPoint.oSet(trace[0].c.point);
            }
            return false;
        }
// #ifdef NEW_PUSH//TODO:check if alternative methods are better suited for JAVA!@#
        // boolean			CanPushEntity( idEntity *ent, idEntity *pusher, idEntity *initialPusher, final int flags );
        // void			AddEntityToPushedGroup( idEntity *ent, float fraction, boolean groundContact );
        // boolean			IsFullyPushed( idEntity *ent );
        // boolean			ClipTranslationAgainstPusher( trace_s &results, idEntity *ent, idEntity *pusher, final idVec3 &translation );
        // int				GetPushableEntitiesForTranslation( idEntity *pusher, idEntity *initialPusher, final int flags,
        // final idVec3 &translation, idEntity *entityList[], int maxEntities );
        // boolean			ClipRotationAgainstPusher( trace_s &results, idEntity *ent, idEntity *pusher, final idRotation &rotation );
        // int				GetPushableEntitiesForRotation( idEntity *pusher, idEntity *initialPusher, final int flags,
        // final idRotation &rotation, idEntity *entityList[], int maxEntities );
// #else

        private void ClipEntityRotation(trace_s[] trace, final idEntity ent, final idClipModel clipModel, idClipModel skip, final idRotation rotation) {

            if (skip != null) {
                skip.Disable();
//	}

                ent.GetPhysics().ClipRotation(trace, rotation, clipModel);

//	if ( skip !=null) {//TODO:make sure the above function doesn't somehow turn skip into null.
                skip.Enable();
            }
        }

        private void ClipEntityTranslation(trace_s[] trace, final idEntity ent, final idClipModel clipModel, idClipModel skip, final idVec3 translation) {

            if (skip != null) {
                skip.Disable();
//	}

                ent.GetPhysics().ClipTranslation(trace, translation, clipModel);

//	if ( skip !=null) {//TODO:make sure the above function doesn't somehow turn skip into null.
                skip.Enable();
            }
        }

        private int TryTranslatePushEntity(trace_s[] results, idEntity check, idClipModel clipModel, final int flags, final idVec3 newOrigin, final idVec3 move) {
            trace_s[] trace = {null};
            idVec3 checkMove;
//            idVec3 oldOrigin;
            idPhysics physics;

            physics = check.GetPhysics();

// #ifdef TRANSLATIONAL_PUSH_DEBUG
            // bool startsolid = false;
            // if ( physics.ClipContents( clipModel ) ) {
            // startsolid = true;
            // }
// #endif
            results[0].fraction = 1.0f;
            results[0].endpos = newOrigin;
            results[0].endAxis = clipModel.GetAxis();
//	memset( &results.c, 0, sizeof( results.c ) );//TODOS:

            // always pushed when standing on the pusher
            if (physics.IsGroundClipModel(clipModel.GetEntity().entityNumber, clipModel.GetId())) {
                // move the entity colliding with all other entities except the pusher itself
                ClipEntityTranslation(trace, check, null, clipModel, move);
                // if there is a collision
                if (trace[0].fraction < 1.0f) {
                    // vector along which the entity is pushed
                    checkMove = move.oMultiply(trace[0].fraction);
                    // test if the entity can stay at it's partly pushed position by moving the entity in reverse only colliding with pusher
                    ClipEntityTranslation(results, check, clipModel, null, (move.oMinus(checkMove).oNegative()));
                    // if there is a collision
                    if (results[0].fraction < 1.0f) {

                        // FIXME: try to push the blocking entity as well or try to slide along collision plane(s)?
                        results[0].c.normal = results[0].c.normal.oNegative();
                        results[0].c.dist = -results[0].c.dist;

                        // the entity will be crushed between the pusher and some other entity
                        return PUSH_BLOCKED;
                    }
                } else {
                    // vector along which the entity is pushed
                    checkMove = move;
                }
            } else {
                // move entity in reverse only colliding with pusher
                ClipEntityTranslation(results, check, clipModel, null, move.oNegative());
                // if no collision with the pusher then the entity is not pushed by the pusher
                if (results[0].fraction >= 1.0f) {
                    return PUSH_NO;
                }
                // vector along which the entity is pushed
                checkMove = move.oMultiply(1.0f - results[0].fraction);
                // move the entity colliding with all other entities except the pusher itself
                ClipEntityTranslation(trace, check, null, clipModel, checkMove);
                // if there is a collisions
                if (trace[0].fraction < 1.0f) {

                    results[0].c.normal = results[0].c.normal.oNegative();
                    results[0].c.dist = -results[0].c.dist;

                    // FIXME: try to push the blocking entity as well ?
                    // FIXME: handle sliding along more than one collision plane ?
                    // FIXME: this code has issues, player pushing box into corner in "maps/mre/aaron/test.map"

                    /*
                     oldOrigin = physics.GetOrigin();

                     // movement still remaining
                     checkMove *= (1.0f - trace.fraction);

                     // project the movement along the collision plane
                     if ( !checkMove.ProjectAlongPlane( trace.c.normal, 0.1f, 1.001f ) ) {
                     return PUSH_BLOCKED;
                     }
                     checkMove *= 1.001f;

                     // move entity from collision point along the collision plane
                     physics.SetOrigin( trace.endpos );
                     ClipEntityTranslation( trace, check, NULL, NULL, checkMove );

                     if ( trace.fraction < 1.0f ) {
                     physics.SetOrigin( oldOrigin );
                     return PUSH_BLOCKED;
                     }

                     checkMove = trace.endpos - oldOrigin;

                     // move entity in reverse only colliding with pusher
                     physics.SetOrigin( trace.endpos );
                     ClipEntityTranslation( trace, check, clipModel, NULL, -move );

                     physics.SetOrigin( oldOrigin );
                     */
                    if (trace[0].fraction < 1.0f) {
                        return PUSH_BLOCKED;
                    }
                }
            }

            SaveEntityPosition(check);

            // translate the entity
            physics.Translate(checkMove);

// #ifdef TRANSLATIONAL_PUSH_DEBUG
            // // set the pusher in the translated position
            // clipModel.Link( gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), newOrigin, clipModel.GetAxis() );
            // if ( physics.ClipContents( clipModel ) ) {
            // if ( !startsolid ) {
            // int bah = 1;
            // }
            // }
// #endif
            return PUSH_OK;
        }

        private int TryRotatePushEntity(trace_s[] results, idEntity check, idClipModel clipModel, final int flags, final idMat3 newAxis, final idRotation rotation) {
            trace_s[] trace = {null};
            idVec3 rotationPoint;
            idRotation newRotation = new idRotation();
            float checkAngle;
            idPhysics physics;

            physics = check.GetPhysics();

// #ifdef ROTATIONAL_PUSH_DEBUG
            // bool startsolid = false;
            // if ( physics.ClipContents( clipModel ) ) {
            // startsolid = true;
            // }
// #endif
            results[0].fraction = 1.0f;
            results[0].endpos = clipModel.GetOrigin();
            results[0].endAxis = newAxis;
//	memset( &results.c, 0, sizeof( results.c ) );//TODOS:

            // always pushed when standing on the pusher
            if (physics.IsGroundClipModel(clipModel.GetEntity().entityNumber, clipModel.GetId())) {
                // rotate the entity colliding with all other entities except the pusher itself
                ClipEntityRotation(trace, check, null, clipModel, rotation);
                // if there is a collision
                if (trace[0].fraction < 1.0f) {
                    // angle along which the entity is pushed
                    checkAngle = rotation.GetAngle() * trace[0].fraction;
                    // test if the entity can stay at it's partly pushed position by rotating
                    // the entity in reverse only colliding with pusher
                    newRotation.Set(rotation.GetOrigin(), rotation.GetVec(), -(rotation.GetAngle() - checkAngle));
                    ClipEntityRotation(results, check, clipModel, null, newRotation);
                    // if there is a collision
                    if (results[0].fraction < 1.0f) {

                        // FIXME: try to push the blocking entity as well or try to slide along collision plane(s)?
                        results[0].c.normal = results[0].c.normal.oNegative();
                        results[0].c.dist = -results[0].c.dist;

                        // the entity will be crushed between the pusher and some other entity
                        return PUSH_BLOCKED;
                    }
                } else {
                    // angle along which the entity is pushed
                    checkAngle = rotation.GetAngle();
                }
                // point to rotate entity bbox around back to axial
                rotationPoint = physics.GetOrigin();
            } else {
                // rotate entity in reverse only colliding with pusher
                newRotation = rotation;
                newRotation.Scale(-1);
                //
                ClipEntityRotation(results, check, clipModel, null, newRotation);
                // if no collision with the pusher then the entity is not pushed by the pusher
                if (results[0].fraction >= 1.0f) {
// #ifdef ROTATIONAL_PUSH_DEBUG
                    // // set pusher into final position
                    // clipModel.Link( gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), newAxis );
                    // if ( physics.ClipContents( clipModel ) ) {
                    // if ( !startsolid ) {
                    // int bah = 1;
                    // }
                    // }
// #endif
                    return PUSH_NO;
                }
                // get point to rotate bbox around back to axial
                rotationPoint = results[0].c.point;
                // angle along which the entity will be pushed
                checkAngle = rotation.GetAngle() * (1.0f - results[0].fraction);
                // rotate the entity colliding with all other entities except the pusher itself
                newRotation.Set(rotation.GetOrigin(), rotation.GetVec(), checkAngle);
                ClipEntityRotation(trace, check, null, clipModel, newRotation);
                // if there is a collision
                if (trace[0].fraction < 1.0f) {

                    // FIXME: try to push the blocking entity as well or try to slide along collision plane(s)?
                    results[0].c.normal = results[0].c.normal.oNegative();
                    results[0].c.dist = -results[0].c.dist;

                    // the entity will be crushed between the pusher and some other entity
                    return PUSH_BLOCKED;
                }
            }

            SaveEntityPosition(check);

            newRotation.Set(rotation.GetOrigin(), rotation.GetVec(), checkAngle);
            // NOTE:	this code prevents msvc 6.0 & 7.0 from screwing up the above code in
            //			release builds moving less floats than it should
//	static float shit = checkAngle;

            newRotation.RotatePoint(rotationPoint);

            // rotate the entity
            physics.Rotate(newRotation);

            // set pusher into final position
            clipModel.Link(gameLocal.clip, clipModel.GetEntity(), clipModel.GetId(), clipModel.GetOrigin(), newAxis);

// #ifdef ROTATIONAL_PUSH_DEBUG
            // if ( physics.ClipContents( clipModel ) ) {
            // if ( !startsolid ) {
            // int bah = 1;
            // }
            // }
// #endif
            // if the entity uses actor physics
            if (physics.IsType(idPhysics_Actor.class)) {

                // rotate the collision model back to axial
                if (!RotateEntityToAxial(check, rotationPoint)) {
                    // don't allow rotation if the bbox is no longer axial
                    return PUSH_BLOCKED;
                }
            }

// #ifdef ROTATIONAL_PUSH_DEBUG
            // if ( physics.ClipContents( clipModel ) ) {
            // if ( !startsolid ) {
            // int bah = 1;
            // }
            // }
// #endif
            // if the entity is an actor using actor physics
            if (check.IsType(idActor.class) && physics.IsType(idPhysics_Actor.class)) {

                // if the entity is standing ontop of the pusher
                if (physics.IsGroundClipModel(clipModel.GetEntity().entityNumber, clipModel.GetId())) {
                    // rotate actor view
                    idActor actor = (idActor) check;
                    idAngles delta = actor.GetDeltaViewAngles();
                    delta.yaw += newRotation.ToMat3().oGet(0).ToYaw();
                    actor.SetDeltaViewAngles(delta);
                }
            }

            return PUSH_OK;
        }

        private int DiscardEntities(idEntity[] entityList, int numEntities, int flags, idEntity pusher) {
            int i, num;
            idEntity check;

            // remove all entities we cannot or should not push from the list
            for (num = i = 0; i < numEntities; i++) {
                check = entityList[ i];

                // if the physics object is not pushable
                if (!check.GetPhysics().IsPushable()) {
                    continue;
                }

                // if the entity doesn't clip with this pusher
                if (0 == (check.GetPhysics().GetClipMask() & pusher.GetPhysics().GetContents())) {
                    continue;
                }

                // don't push players in noclip mode
                if (check.IsType(idPlayer.class) && ((idPlayer) check).noclip) {
                    continue;
                }

                // if we should only push idMoveable entities
                if (((flags & PUSHFL_ONLYMOVEABLE) != 0) && !check.IsType(idMoveable.class)) {
                    continue;
                }

                // if we shouldn't push entities the clip model rests upon
                if ((flags & PUSHFL_NOGROUNDENTITIES) != 0) {
                    if (pusher.GetPhysics().IsGroundEntity(check.entityNumber)) {
                        continue;
                    }
                }

                // keep entity in list
                entityList[ num++] = entityList[i];
            }

            return num;
        }
// #endif
    };
}
