package neo.Game.Physics;

import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Physics.Force_Field.forceFieldApplyType.FORCEFIELD_APPLY_FORCE;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_EXPLOSION;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_IMPLOSION;
import static neo.Game.Physics.Force_Field.forceFieldType.FORCEFIELD_UNIFORM;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;

import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Monster.idPhysics_Monster;
import neo.Game.Physics.Physics_Player.idPhysics_Player;
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
    }

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
    }

    public static class idForce_Field extends idForce {
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
            savefile.WriteInt(etoi(this.type));
            savefile.WriteInt(this.applyType.ordinal());
            savefile.WriteFloat(this.magnitude);
            savefile.WriteVec3(this.dir);
            savefile.WriteFloat(this.randomTorque);
            savefile.WriteBool(this.playerOnly);
            savefile.WriteBool(this.monsterOnly);
            savefile.WriteClipModel(this.clipModel);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.type = forceFieldType.values()[savefile.ReadInt()];
            this.applyType = forceFieldApplyType.values()[savefile.ReadInt()];
            this.magnitude = savefile.ReadFloat();
            savefile.ReadVec3(this.dir);
            this.randomTorque = savefile.ReadFloat();
            this.playerOnly = savefile.ReadBool();
            this.monsterOnly = savefile.ReadBool();
            savefile.ReadClipModel(this.clipModel);
        }

        public idForce_Field() {
            this.type = FORCEFIELD_UNIFORM;
            this.applyType = FORCEFIELD_APPLY_FORCE;
            this.magnitude = 0.0f;
            this.dir = new idVec3(0, 0, 1);
            this.randomTorque = 0.0f;
            this.playerOnly = false;
            this.monsterOnly = false;
            this.clipModel = null;
        }

//	virtual				~idForce_Field( void );
        // uniform constant force
        public void Uniform(final idVec3 force) {
            this.dir = force;
            this.magnitude = this.dir.Normalize();
            this.type = FORCEFIELD_UNIFORM;
        }

        // explosion from clip model origin	
        public void Explosion(float force) {
            this.magnitude = force;
            this.type = FORCEFIELD_EXPLOSION;
        }

        // implosion towards clip model origin	
        public void Implosion(float force) {
            this.magnitude = force;
            this.type = FORCEFIELD_IMPLOSION;
        }

        // add random torque	
        public void RandomTorque(float force) {
            this.randomTorque = force;
        }

        // should the force field apply a force, velocity or impulse	
        public void SetApplyType(final forceFieldApplyType type) {
            this.applyType = type;
        }

        // make the force field only push players	
        public void SetPlayerOnly(boolean set) {
            this.playerOnly = set;
        }

        // make the force field only push monsters	
        public void SetMonsterOnly(boolean set) {
            this.monsterOnly = set;
        }

        // clip model describing the extents of the force field	
        public void SetClipModel(idClipModel clipModel) {
            if ((this.clipModel != null) && (clipModel != this.clipModel)) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = clipModel;
        }

        // common force interface
        @Override
        public void Evaluate(int time) {
            int numClipModels, i;
            final idBounds bounds = new idBounds();
            idVec3 force = new idVec3();
			final idVec3 torque = new idVec3();
			idVec3 angularVelocity;
            idClipModel cm;
            final idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];

            assert (this.clipModel != null);

            bounds.FromTransformedBounds(this.clipModel.GetBounds(), this.clipModel.GetOrigin(), this.clipModel.GetAxis());
            numClipModels = gameLocal.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            for (i = 0; i < numClipModels; i++) {
                cm = clipModelList[ i];

                if (!cm.IsTraceModel()) {
                    continue;
                }

                final idEntity entity = cm.GetEntity();

                if (null == entity) {
                    continue;
                }

                final idPhysics physics = entity.GetPhysics();

                if (this.playerOnly) {
                    if (!physics.IsType(idPhysics_Player.class)) {
                        continue;
                    }
                } else if (this.monsterOnly) {
                    if (!physics.IsType(idPhysics_Monster.class)) {
                        continue;
                    }
                }

                if (NOT(gameLocal.clip.ContentsModel(cm.GetOrigin(), cm, cm.GetAxis(), -1, this.clipModel.Handle(), this.clipModel.GetOrigin(), this.clipModel.GetAxis()))) {
                    continue;
                }

                switch (this.type) {
                    case FORCEFIELD_UNIFORM: {
                        force = this.dir;
                        break;
                    }
                    case FORCEFIELD_EXPLOSION: {
                        force = cm.GetOrigin().oMinus(this.clipModel.GetOrigin());
                        force.Normalize();
                        break;
                    }
                    case FORCEFIELD_IMPLOSION: {
                        force = this.clipModel.GetOrigin().oMinus(cm.GetOrigin());
                        force.Normalize();
                        break;
                    }
                    default: {
                        gameLocal.Error("idForce_Field: invalid type");
                        break;
                    }
                }

                if (this.randomTorque != 0.0f) {
                    torque.oSet(0, gameLocal.random.CRandomFloat());
                    torque.oSet(1, gameLocal.random.CRandomFloat());
                    torque.oSet(2, gameLocal.random.CRandomFloat());
                    if (torque.Normalize() == 0.0f) {
                        torque.oSet(2, 1.0f);
                    }
                }

                switch (this.applyType) {
                    case FORCEFIELD_APPLY_FORCE: {
                        if (this.randomTorque != 0.0f) {
                            entity.AddForce(gameLocal.world, cm.GetId(), cm.GetOrigin().oPlus(torque.Cross(this.dir).oMultiply(this.randomTorque)), this.dir.oMultiply(this.magnitude));
                        } else {
                            entity.AddForce(gameLocal.world, cm.GetId(), cm.GetOrigin(), force.oMultiply(this.magnitude));
                        }
                        break;
                    }
                    case FORCEFIELD_APPLY_VELOCITY: {
                        physics.SetLinearVelocity(force.oMultiply(this.magnitude), cm.GetId());
                        if (this.randomTorque != 0.0f) {
                            angularVelocity = physics.GetAngularVelocity(cm.GetId());
                            physics.SetAngularVelocity((angularVelocity.oPlus(torque.oMultiply(this.randomTorque))).oMultiply(0.5f), cm.GetId());
                        }
                        break;
                    }
                    case FORCEFIELD_APPLY_IMPULSE: {
                        if (this.randomTorque != 0.0f) {
                            entity.ApplyImpulse(gameLocal.world, cm.GetId(), cm.GetOrigin().oPlus(torque.Cross(this.dir).oMultiply(this.randomTorque)), this.dir.oMultiply(this.magnitude));
                        } else {
                            entity.ApplyImpulse(gameLocal.world, cm.GetId(), cm.GetOrigin(), force.oMultiply(this.magnitude));
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
    }
}
