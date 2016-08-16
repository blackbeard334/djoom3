package neo.Game.Physics;

import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force.idForce;
import static neo.Game.Physics.Force_Field.forceFieldApplyType.FORCEFIELD_APPLY_FORCE;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_EXPLOSION;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_IMPLOSION;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_UNIFORM;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Monster.idPhysics_Monster;
import neo.Game.Physics.Physics_Player.idPhysics_Player;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Force_Field {

    /*
     ===============================================================================

     Force field

     ===============================================================================
     */
    public enum forceFieldType {

        FORCEFIELD_UNIFORM,
        FORCEFIELD_EXPLOSION,
        FORCEFIELD_IMPLOSION;

        public static forceFieldType oGet(final int index) {

            if (index > values().length) {
                return values()[0];
            } else {
                return values()[index];
            }
        }
    };

    public enum forceFieldApplyType {

        FORCEFIELD_APPLY_FORCE,
        FORCEFIELD_APPLY_VELOCITY,
        FORCEFIELD_APPLY_IMPULSE;

        public static forceFieldApplyType oGet(final int index) {

            if (index > values().length) {
                return values()[0];
            } else {
                return values()[index];
            }
        }
    };

    public class idForce_Field extends idForce {
//	CLASS_PROTOTYPE( idForce_Field );

        // force properties
        private forceFieldType type;
        private forceFieldApplyType applyType;
        private float magnitude;
        private idVec3 dir;
        private float randomTorque;
        private boolean playerOnly;
        private boolean monsterOnly;
        private idClipModel clipModel;
        //
        //

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(etoi(type));
            savefile.WriteInt(applyType.ordinal());
            savefile.WriteFloat(magnitude);
            savefile.WriteVec3(dir);
            savefile.WriteFloat(randomTorque);
            savefile.WriteBool(playerOnly);
            savefile.WriteBool(monsterOnly);
            savefile.WriteClipModel(clipModel);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            type = forceFieldType.values()[savefile.ReadInt()];
            applyType = forceFieldApplyType.values()[savefile.ReadInt()];
            magnitude = savefile.ReadFloat();
            savefile.ReadVec3(dir);
            randomTorque = savefile.ReadFloat();
            playerOnly = savefile.ReadBool();
            monsterOnly = savefile.ReadBool();
            savefile.ReadClipModel(clipModel);
        }

        public idForce_Field() {
            type = FORCEFIELD_UNIFORM;
            applyType = FORCEFIELD_APPLY_FORCE;
            magnitude = 0.0f;
            dir.Set(0, 0, 1);
            randomTorque = 0.0f;
            playerOnly = false;
            monsterOnly = false;
            clipModel = null;
        }

//	virtual				~idForce_Field( void );
        // uniform constant force
        public void Uniform(final idVec3 force) {
            dir = force;
            magnitude = dir.Normalize();
            type = FORCEFIELD_UNIFORM;
        }

        // explosion from clip model origin	
        public void Explosion(float force) {
            magnitude = force;
            type = FORCEFIELD_EXPLOSION;
        }

        // implosion towards clip model origin	
        public void Implosion(float force) {
            magnitude = force;
            type = FORCEFIELD_IMPLOSION;
        }

        // add random torque	
        public void RandomTorque(float force) {
            randomTorque = force;
        }

        // should the force field apply a force, velocity or impulse	
        public void SetApplyType(final forceFieldApplyType type) {
            applyType = type;
        }

        // make the force field only push players	
        public void SetPlayerOnly(boolean set) {
            playerOnly = set;
        }

        // make the force field only push monsters	
        public void SetMonsterOnly(boolean set) {
            monsterOnly = set;
        }

        // clip model describing the extents of the force field	
        public void SetClipModel(idClipModel clipModel) {
            if (this.clipModel != null && clipModel != this.clipModel) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = clipModel;
        }

        // common force interface
        @Override
        public void Evaluate(int time) {
            int numClipModels, i;
            idBounds bounds = new idBounds();
            idVec3 force = new idVec3(), torque = new idVec3(), angularVelocity;
            idClipModel cm;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];

            assert (clipModel != null);

            bounds.FromTransformedBounds(clipModel.GetBounds(), clipModel.GetOrigin(), clipModel.GetAxis());
            numClipModels = gameLocal.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            for (i = 0; i < numClipModels; i++) {
                cm = clipModelList[ i];

                if (!cm.IsTraceModel()) {
                    continue;
                }

                idEntity entity = cm.GetEntity();

                if (null == entity) {
                    continue;
                }

                idPhysics physics = entity.GetPhysics();

                if (playerOnly) {
                    if (!physics.IsType(idPhysics_Player.class)) {
                        continue;
                    }
                } else if (monsterOnly) {
                    if (!physics.IsType(idPhysics_Monster.class)) {
                        continue;
                    }
                }

                if (NOT(gameLocal.clip.ContentsModel(cm.GetOrigin(), cm, cm.GetAxis(), -1, clipModel.Handle(), clipModel.GetOrigin(), clipModel.GetAxis()))) {
                    continue;
                }

                switch (type) {
                    case FORCEFIELD_UNIFORM: {
                        force = dir;
                        break;
                    }
                    case FORCEFIELD_EXPLOSION: {
                        force = cm.GetOrigin().oMinus(clipModel.GetOrigin());
                        force.Normalize();
                        break;
                    }
                    case FORCEFIELD_IMPLOSION: {
                        force = clipModel.GetOrigin().oMinus(cm.GetOrigin());
                        force.Normalize();
                        break;
                    }
                    default: {
                        gameLocal.Error("idForce_Field: invalid type");
                        break;
                    }
                }

                if (randomTorque != 0.0f) {
                    torque.oSet(0, gameLocal.random.CRandomFloat());
                    torque.oSet(1, gameLocal.random.CRandomFloat());
                    torque.oSet(2, gameLocal.random.CRandomFloat());
                    if (torque.Normalize() == 0.0f) {
                        torque.oSet(2, 1.0f);
                    }
                }

                switch (applyType) {
                    case FORCEFIELD_APPLY_FORCE: {
                        if (randomTorque != 0.0f) {
                            entity.AddForce(gameLocal.world, cm.GetId(), cm.GetOrigin().oPlus(torque.Cross(dir).oMultiply(randomTorque)), dir.oMultiply(magnitude));
                        } else {
                            entity.AddForce(gameLocal.world, cm.GetId(), cm.GetOrigin(), force.oMultiply(magnitude));
                        }
                        break;
                    }
                    case FORCEFIELD_APPLY_VELOCITY: {
                        physics.SetLinearVelocity(force.oMultiply(magnitude), cm.GetId());
                        if (randomTorque != 0.0f) {
                            angularVelocity = physics.GetAngularVelocity(cm.GetId());
                            physics.SetAngularVelocity((angularVelocity.oPlus(torque.oMultiply(randomTorque))).oMultiply(0.5f), cm.GetId());
                        }
                        break;
                    }
                    case FORCEFIELD_APPLY_IMPULSE: {
                        if (randomTorque != 0.0f) {
                            entity.ApplyImpulse(gameLocal.world, cm.GetId(), cm.GetOrigin().oPlus(torque.Cross(dir).oMultiply(randomTorque)), dir.oMultiply(magnitude));
                        } else {
                            entity.ApplyImpulse(gameLocal.world, cm.GetId(), cm.GetOrigin(), force.oMultiply(magnitude));
                        }
                        break;
                    }
                    default: {
                        gameLocal.Error("idForce_Field: invalid apply type");
                        break;
                    }
                }
            }
        }
    };
}
